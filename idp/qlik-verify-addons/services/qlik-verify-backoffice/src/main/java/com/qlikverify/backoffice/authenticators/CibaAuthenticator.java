package com.qlikverify.backoffice.authenticators;

import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.services.events.models.response.User;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.ciba.models.UserData;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.protocol.oidc.grants.ciba.util.UserTransactionStatus;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AESHelper;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static org.keycloak.protocol.oidc.grants.ciba.resolvers.DefaultCIBALoginUserResolver.SP_WORKFLOW;

public class CibaAuthenticator implements Authenticator {
    private static String BINDING_MESSAGE = "binding_message";

    KeycloakSession session;
    private static Logger logger = Logger.getLogger(CibaAuthenticator.class);

    public CibaAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        ClientModel clientModel = context.getAuthenticationSession().getClient();


        String username = context.getUriInfo().getQueryParameters().getFirst("login_hint");
        String message = context.getUriInfo().getQueryParameters().getFirst(BINDING_MESSAGE);
        if(username != null && message != null) {
            context.getAuthenticationSession().setAuthNote("login_hint", username);
            context.getAuthenticationSession().setAuthNote(BINDING_MESSAGE, message);
        } else {
            username = context.getAuthenticationSession().getAuthNote("login_hint");
            message = context.getAuthenticationSession().getAuthNote(BINDING_MESSAGE);
        }
        final CibaStateModel cibaStateModel = getCibaStateModel(context);
        if (cibaStateModel == null) {
            context.getEvent().detail("requestId", "Request id is null").error(Errors.INVALID_REQUEST);
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        if (username == null) {
            // return error
            context.getEvent().detail("requestId", cibaStateModel.getRequestId()).error(Errors.USER_NOT_FOUND);
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }
        UserData userData = null;
        try {
            userData = UserData.decode(username, context.getAuthenticationSession().getClient());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // validate username
        UserModel userModel = userData != null ? context.getSession().users().getUserById(context.getRealm(), userData.getUserId()) : null;

        if (userModel == null) {
            context.getEvent().detail("requestId", cibaStateModel.getRequestId()).error(Errors.USER_NOT_FOUND);
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }

        BindingMessage parsedMessage = null;
        try {
            parsedMessage = BindingMessage.decode(message, context.getAuthenticationSession().getClient());
        } catch (Exception e) {
            e.printStackTrace();
            context.getEvent().detail("requestId", cibaStateModel.getRequestId()).error(Errors.USER_NOT_FOUND);
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }

        context.setUser(userModel);
        context.getAuthenticationSession().setAuthNote(BINDING_MESSAGE, message);
        try {
            context.getAuthenticationSession().setAuthNote("message", JsonSerialization.writeValueAsString(parsedMessage));
        } catch (IOException e) {
            logger.error("Error while serializing the binding message " + e.getMessage());
        }
        context.getAuthenticationSession().setAuthNote("user", username);
        context.getAuthenticationSession().setAuthNote("request", cibaStateModel.getRequestId());

        if (clientModel != null && clientModel.getAttribute("gatepass_rule") != null) {
            String policyId = clientModel.getAttribute("gatepass_rule");
            boolean result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(policyId, context.getUser());
            if (!result) {
                context.getEvent().detail("requestId", cibaStateModel.getRequestId());
                UserTransactionModel userTransactionDetails = context.getRealm().getUserTransactionDetails(cibaStateModel.getRequestId(), userModel.getUsername());
                if (userTransactionDetails != null) {
                    context.getRealm().updateTransactionStatus(cibaStateModel.getRequestId(), userModel.getUsername(), UserTransactionStatus.ACCESS_DENIED.getDisplayName());
                }
                GeneralHelper.sendCancelStatusToSocket(context.getAuthenticationSession().getAuthNote("request"), "403");
                context.getEvent().event(EventType.EZ_GATEPASS_ERROR);
                context.getEvent().user(session.getContext().getAuthenticationSession().getAuthenticatedUser());
                context.getEvent().error("Gatepass rule executed and blocked access");
                context.failure(AuthenticationFlowError.GATEPASS_ERROR);
                return;
            }
        }

        if (clientModel != null) {
            boolean isShowConsent = Boolean.parseBoolean(clientModel.getAttribute("ciba.consent"));
            if (!isShowConsent) {
                context.success();
                return;
            }
        }
        final Map<String, Object> connectionDetails = getConnectionDetails(cibaStateModel);

        context.getAuthenticationSession().setAuthNote("cibaStateId", cibaStateModel.getCibaStateId().toString());


        Response challenge = context.form()
                .setAttribute(ATTRIBUTE_MESSAGE_TYPE, parsedMessage.getConsent().getType())
                .setAttribute(ATTRIBUTE_MESSAGE_TITLE, parsedMessage.getConsent().getTitle())
                .setAttribute(Constants.ATTRIBUTE_MESSAGE, parsedMessage.getConsent().getFormattedMessage())
                .setAttribute(CONNECTION_DETAILS, connectionDetails)
                .createForm("ciba-authenticator.ftl");
        context.challenge(challenge);
    }

    private static CibaStateModel getCibaStateModel(AuthenticationFlowContext context) {
        String state = context.getUriInfo().getQueryParameters().getFirst("state");
        if(state != null) {
            context.getAuthenticationSession().setAuthNote("state", state);
        } else {
            state = context.getAuthenticationSession().getAuthNote("state");
        }

        if (state == null) {
            context.getEvent().detail("requestId", "Invalid ID").error(Errors.INVALID_REQUEST);
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return null;
        }

        CibaStateModel cibaStateModel = null;
        try {
            String stateId = AESHelper.decrypt(new String(Base64.getDecoder().decode(state.getBytes(StandardCharsets.UTF_8))), context.getAuthenticationSession().getClient().getSecret().substring(0, 32));
            cibaStateModel = context.getRealm().getCibaStateById(stateId);
        } catch (Exception e) {
            context.getEvent().detail("requestId", "Invalid ID").error(e.getMessage());
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return null;
        }
        return cibaStateModel;
    }

    private Map<String, Object> getConnectionDetails(CibaStateModel cibaStateModel) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        Map<String, Object> connectionDetails = new HashMap<>();
        final String trxId = cibaStateModel.getRequestId();
        if (trxId != null && session.getContext().getRealm().getTrx(trxId) != null) {
            final Map<String, String> trxAttributes =
                    session.getContext().getRealm().getTrx(trxId).getAttributes();
            connectionDetails.put(IP, trxAttributes.getOrDefault(IP,
                    session.getContext().getConnection().getRemoteAddr()));
            connectionDetails.put(DEVICE, trxAttributes.getOrDefault(DEVICE,
                    session.getContext().getConnection().getClientOs()));
            connectionDetails.put(LOCATION, session.getContext().getConnection().getClientLocation().getLocation());
            connectionDetails.put(DATE_AND_TIME, ZonedDateTime.now(
                            ZoneId.of(session.getContext().getConnection().getClientLocation().getBody().getZoneId()))
                    .format(formatter));
        }
        return connectionDetails;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String userAction = context.getHttpRequest().getDecodedFormParameters().getFirst(
                "action");
        processForm(context.getAuthenticationSession().getAuthNote(BINDING_MESSAGE), context.getHttpRequest(), context.getUser(), context.getAuthenticationSession().getClient());
        if (userAction != null && userAction.equals("accepted")) {
            clearTrxAttrs(context);
            context.success();
            return;
        }

        if (userAction != null && userAction.equals("back")) {
            BindingMessage parsedMessage = null;
            Map<String, Object> connectionDetails = new HashMap<>();
            try {
                parsedMessage = JsonSerialization.readValue(context.getAuthenticationSession().getAuthNote("message"), BindingMessage.class);
                connectionDetails = getConnectionDetails(context.getRealm().getCibaStateById(context.getAuthenticationSession().getAuthNote("cibaStateId").toString()));
            } catch (Exception e) {
                context.getEvent().detail("error", "Error while deserializing.").error(Errors.INVALID_REQUEST);
                logger.error("Error while deserializing " + e.getMessage());
            }

            Response challenge = context.form()
                    .setAttribute(ATTRIBUTE_MESSAGE_TYPE, parsedMessage.getConsent().getType())
                    .setAttribute(ATTRIBUTE_MESSAGE_TITLE, parsedMessage.getConsent().getTitle())
                    .setAttribute(Constants.ATTRIBUTE_MESSAGE, parsedMessage.getConsent().getFormattedMessage())
                    .setAttribute(CONNECTION_DETAILS, connectionDetails)
                    .createForm("ciba-authenticator.ftl");
            context.challenge(challenge);
        }

        if (userAction != null && userAction.equals("rejected")) {
            if (context.getAuthenticationSession().getClient().getAttribute("consent_rejected_reason") == null) {
                List<String> defaultReasons = Arrays.asList("Incorrect Information", "Suspicious Activity", "Transaction not triggered by me", "Inappropriate Transaction");
                context.getAuthenticationSession().getClient().setAttribute("consent_rejected_reason", String.join("##", defaultReasons));
            }
            Response challenge = context.form()
                    .setAttribute("consent_rejected_reason", Arrays.asList(context.getAuthenticationSession().getClient().getAttribute("consent_rejected_reason").split("##")))
                    .setAttribute("consent_rejected", "true")
                    .createForm("ciba-authenticator.ftl");
            context.challenge(challenge);
            return;
        }

        if (context.getHttpRequest().getDecodedFormParameters().get("confirm_reject") != null
                && Boolean.parseBoolean(context.getHttpRequest().getDecodedFormParameters().getFirst("confirm_reject").toString())) {
            GeneralHelper.sendCancelStatusToSocket(context.getAuthenticationSession().getAuthNote("request"), "403");
            String rejectedReason = "";
            if (context.getHttpRequest().getDecodedFormParameters().getFirst("rejected_reason") != null) {
                rejectedReason = context.getHttpRequest().getDecodedFormParameters().getFirst("rejected_reason");
                context.getEvent().event(EventType.EZ_CONSENT);
                context.getEvent().user(session.getContext().getAuthenticationSession().getAuthenticatedUser());
                context.getEvent().detail("consent_rejected_reason", rejectedReason);
                if (rejectedReason.equalsIgnoreCase("Suspicious Activity") || rejectedReason.equalsIgnoreCase("Transaction not triggered by me")) {
                    context.getUser().setEnabled(false);
                }
            } else {
                context.getEvent().detail("error", "Rejected reason not found.").error(Errors.INVALID_REQUEST);
                logger.error("Rejected reason not found");
                return;
            }

            session.getContext().getRealm().addAttribute(context.getAuthenticationSession().getAuthNote("request"), "sp@consent_rejected_reason", rejectedReason);
            session.getContext().getRealm().updateTransactionStatus(context.getAuthenticationSession().getAuthNote("request"), context.getUser().getUsername(), UserTransactionStatus.USER_CANCELLED.getDisplayName());
            session.getContext().getRealm().addAttribute(context.getAuthenticationSession().getAuthNote("request"), "access_denied", "User cancelled the process.");
            context.cancelLogin();
            return;
        }

        context.failure(AuthenticationFlowError.ACCESS_DENIED);
    }

    private void clearTrxAttrs(AuthenticationFlowContext context) {
        final String trxId = context.getAuthenticationSession().getAuthNote("request");
        if (trxId == null) return;
        TrxModel trxModel = context.getRealm().getTrx(trxId);
        if (trxModel == null || trxModel.getAttributes() == null) {
            return;
        }

        if (context.getRealm().hasTrxValueAttribute(trxId, IP)) context.getRealm().removeTrxAttribute(trxId, IP);
        if (context.getRealm().hasTrxValueAttribute(trxId, DEVICE))
            context.getRealm().removeTrxAttribute(trxId, DEVICE);
        if (context.getRealm().hasTrxValueAttribute(trxId, LOCATION))
            context.getRealm().removeTrxAttribute(trxId, LOCATION);
        if (context.getRealm().
                hasTrxValueAttribute(trxId, DATE_AND_TIME)) context.getRealm().removeTrxAttribute(trxId, DATE_AND_TIME);
    }

    private void processForm(String message, HttpRequest request, UserModel userModel, ClientModel clientModel) {
        if (message == null || message.isEmpty())
            return;

        try {
            BindingMessage bindingMessage = BindingMessage.decode(message, clientModel);
            if (!bindingMessage.getConsent().getType().equalsIgnoreCase("form"))
                return;

            List<LinkedHashMap> hashData = bindingMessage.getConsent().getFormMessage();
            hashData.forEach(x -> {
                String key = (String) x.get("name");
                if (key != null) {
                    String result = request.getDecodedFormParameters().getFirst(key);
                    if (result != null) {
                        userModel.setAttribute("sp@" + key, Arrays.asList(new String[]{result}));
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }
}
