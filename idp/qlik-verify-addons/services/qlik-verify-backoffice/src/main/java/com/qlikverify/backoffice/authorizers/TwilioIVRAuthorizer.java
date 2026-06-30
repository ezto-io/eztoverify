package com.qlikverify.backoffice.authorizers;

import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.models.ivr.FlowExecution;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.ciba.AuthenticationChannelRequest;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.CibaAuthProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer;
import org.keycloak.protocol.oidc.grants.ciba.util.UserTransactionStatus;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.sms.SmsException;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.GeneralHelper.getExpiryDurationInRawSec;
import static org.keycloak.models.AuthenticationFlowBindings.CIBA_AUTHORIZER;

public class TwilioIVRAuthorizer extends AbstractCibaAuthorizer implements CibaAuthProvider, CibaAuthProviderFactory {

    private static final Logger logger = Logger.getLogger(TwilioIVRAuthorizer.class);
    private static List<String> allowedMetadata;
    private static final String REQUEST_ID = "requestId";
    private static final String IVR_VERIFIED = "ivr_verified";
    private static final String IVR_VERIFIED_AT = "ivr_verified_at";
    private static final String IVR_REASON = "ivr_reason";

    static {
        allowedMetadata = new ArrayList<>();
        allowedMetadata.add("ivr_sender");
        allowedMetadata.add("ivr_purpose_message");
        allowedMetadata.add("ivr_validation_message");
    }

    @Override
    public CibaAuthProvider setEventBuilder(EventBuilder eventBuilder) {
        this.event = eventBuilder;
        return this;
    }

    @Override
    public boolean onAuthorize(ClientModel clientModel, RealmModel realmModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, CibaStateModel state) {
        if (session.getContext().getAuthenticationSession() == null) {
            try {
                session.setAttribute("message", JsonSerialization.writeValueAsString(request.getBindingMessage()));
            } catch (IOException e) {
                return false;
            }
        }

        if (clientModel != null && clientModel.getAttribute("gatepass_rule") != null) {
            String policyId = clientModel.getAttribute("gatepass_rule");
            boolean result = session.getProvider(ActionPolicyManagerProvider.class).validate(policyId, userModel);
            if (!result) {
                event.detail("requestId", state.getRequestId());
                event.event(EventType.EZ_GATEPASS_ERROR);
                event.user(userModel);
                event.detail("reason", "Gatepass rule executed and blocked access");
                event.error("Gatepass rule executed and blocked access");
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                        "Gatepass rule executed and blocked access",
                        javax.ws.rs.core.Response.Status.BAD_REQUEST);
            }
        }
        RealmModel realm = session.getContext().getRealm();
        RealmModel adminRealm = session.realms().getRealmByName(Config.getAdminRealm());
        long tolerance = getExpiryDurationInRawSec(cibaAuthConfigModel.getConfig().get(VERIFICATION_EXPIRY_CONFIG));
        assert clientModel != null;
        String redirectUrl = getRedirectUrl(clientModel, state.getCibaStateId(), realm, getId(), tolerance, false);
        if (redirectUrl.startsWith("http://")) {
            redirectUrl = redirectUrl.replace("http://", "https://");
        }
        Map<String, String> configModel = cibaAuthConfigModel.getConfig();
        Response response = null;
        try {

            final String[] params = {"&Parameters={\"username\":\"" + userModel.getUsername() + "\",\"ivr_redirect_url\":\"" + redirectUrl + "\""};
            Map<String, String> metaData = request.getBindingMessage().getMetadata();
            if (!(metaData.containsKey("ivr_sender") && metaData.containsKey("ivr_purpose_message") && metaData.containsKey("ivr_validation_message"))) {
                event.event(EventType.EZ_IVR).detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).error("Required fields are missing in request. Check IVR documentation");
                return false;
            }
            metaData.forEach((k, v) -> {
                if (allowedMetadata.contains(k)) {
                    params[0] += ",\"" + k + "\":\"" + v + "\"";
                }
            });
            params[0] += "}";

            logger.debug(params[0]);

            String toPhoneNumber = getPhoneNumber(userModel, null);
//            toPhoneNumber = toPhoneNumber.replace("+", "%2B");
            boolean isCustom = configModel.get(TWILIO_PROVIDER).equals(TWILIO_CUSTOM);
            String fromNumber = isCustom ? configModel.get(TWILIO_FROM_NO) : adminRealm.getAttribute("TWILIO_FROM_NO");
//            fromNumber = fromNumber.replace("+", "%2B");

            String body = "To=" + toPhoneNumber + "&From=" + fromNumber + params[0];
            String twilioRestApi = isCustom ? configModel.get(TWILIO_REST_API) : adminRealm.getAttribute("TWILIO_FLOW_URL");
            response = httpUtility.callRequest(MediaType.APPLICATION_FORM_URLENCODED, "POST", twilioRestApi, body, getAuthString(configModel, isCustom, adminRealm));
            if (response.code() == 409) {
                session.setAttribute(REQUEST_ID, String.valueOf(request.getAdditionalParameters().get(REQUEST_ID)));
                event.event(EventType.EZ_IVR).detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).detail("userId", userModel.getId()).error("Previous transaction for this user is pending in Twilio, please try again after some time.");
                return false;
            }

            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                event.event(EventType.EZ_IVR).detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).error(response.body() != null ? responseBody : "Failed to trigger request");
                return false;
            }
            FlowExecution flowResponse = JsonSerialization.readValue(responseBody, FlowExecution.class);
            if (flowResponse != null) {
                session.getContext().getRealm().addAttribute(state.getRequestId(), "ik@flow.response.execution_context", flowResponse.getLinks().getExecution_context());
            }
            return true;
        } catch (SmsException | IOException e) {
            event.event(EventType.EZ_IVR).detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).error(e.getMessage());
            return false;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public void onAuthCompleted(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {
        logger.info("-----------TwilioIVRAuthorizer---------");
        try {
            logger.info(JsonSerialization.writeValueAsString(response));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("-----------TwilioIVRAuthorizer---------");
        Map<String, String> configModel = cibaAuthConfigModel.getConfig();
        RealmModel realm = session.getContext().getRealm();
        updateTransaction(cibaState.getRequestId(), clientModel, userModel, realm);
        try {
            String status = (String) response.get("status");
            if (status == null) {
                throw new IllegalArgumentException("No Status available");
            }

            if (!status.equals("OK")) {
                if (realm.getUserTransactionDetails(cibaState.getRequestId(), userModel.getUsername()) != null) {
                    realm.updateTransactionStatus(cibaState.getRequestId(), userModel.getUsername(), UserTransactionStatus.FAILURE.getDisplayName());
                }
                throw new IllegalArgumentException("Not a Valid status. Received : " + status);
            }

            String code = String.valueOf(response.get("code"));
            if (code == null) {
                if (realm.getUserTransactionDetails(cibaState.getRequestId(), userModel.getUsername()) != null) {
                    realm.updateTransactionStatus(cibaState.getRequestId(), userModel.getUsername(), UserTransactionStatus.FAILURE.getDisplayName());
                }
                throw new IllegalArgumentException("Code is not received");
            }

            String userPin = realm.getTrx(cibaState.getRequestId()).getAttributes().get(cibaAuthConfigModel.getConfig().get(PIN_USER_ATTRIBUTE));
            if (userPin == null) {
                if (realm.getUserTransactionDetails(cibaState.getRequestId(), userModel.getUsername()) != null) {
                    realm.updateTransactionStatus(cibaState.getRequestId(), userModel.getUsername(), UserTransactionStatus.FAILURE.getDisplayName());
                }
                throw new IllegalArgumentException("Updated attribute is not available at user metadata");
            }

            if (!userPin.equalsIgnoreCase(code)) {
                if (realm.getUserTransactionDetails(cibaState.getRequestId(), userModel.getUsername()) != null) {
                    realm.updateTransactionStatus(cibaState.getRequestId(), userModel.getUsername(), UserTransactionStatus.FAILURE.getDisplayName());
                }
                throw new IllegalArgumentException("Pin didn't match");
            }

            logger.info("-----------Sending Success---------");

            realm.addAttribute(cibaState.getRequestId(), IVR_VERIFIED, "true");
            realm.addAttribute(cibaState.getRequestId(), IVR_VERIFIED_AT, String.valueOf(Time.currentTime()));
            realm.removeTrxAttribute(cibaState.getRequestId(), IVR_REASON);
            if (realm.getUserTransactionDetails(cibaState.getRequestId(), userModel.getUsername()) != null) {
                realm.updateTransactionStatus(cibaState.getRequestId(), userModel.getUsername(), UserTransactionStatus.SUCCESS.getDisplayName());
            }
            userModel.getRequiredActionsStream().forEach(userModel::removeRequiredAction);
            sendSuccess(callbackUrl, cibaState, event, null);
        } catch (ErrorResponseException e) {
            realm.addAttribute(cibaState.getRequestId(), IVR_VERIFIED, "false");
            realm.addAttribute(cibaState.getRequestId(), IVR_VERIFIED_AT, String.valueOf(Time.currentTime()));
            realm.addAttribute(cibaState.getRequestId(), IVR_REASON, e.getErrorDescription());
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail("reason", e.getErrorDescription());
            event.success();
            sendUnAuthorized(callbackUrl, cibaState, event, null);
        } catch (IllegalArgumentException err) {
            realm.addAttribute(cibaState.getRequestId(), IVR_VERIFIED, "false");
            realm.addAttribute(cibaState.getRequestId(), IVR_VERIFIED_AT, String.valueOf(Time.currentTime()));
            realm.addAttribute(cibaState.getRequestId(), IVR_REASON, err.getMessage());
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail("reason", err.getMessage());
            event.success();
            sendUnAuthorized(callbackUrl, cibaState, event, null);
        }

        boolean isCustom = configModel.get(TWILIO_PROVIDER).equals(TWILIO_CUSTOM);
        if (!isCustom) {
            String toPhoneNumber = null;
            try {
                toPhoneNumber = getPhoneNumber(userModel, null);
                updateIVRTransaction(clientModel,
                        userModel,
                        realm,
                        toPhoneNumber,
                        cibaState,
                        getAuthString(configModel, false, session.realms().getRealmByName(Config.getAdminRealm()))
                );
            } catch (SmsException e) {
                e.printStackTrace();
            }
        }
        realm.removeCibaState(cibaState);
    }

    @Override
    public void onAuthFailed(ClientModel clientModel, UserModel userModel, String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {
        RealmModel realm = session.getContext().getRealm();
        realm.addAttribute(cibaState.getRequestId(), IVR_VERIFIED, "false");
        realm.addAttribute(cibaState.getRequestId(), IVR_VERIFIED_AT, String.valueOf(Time.currentTime()));
        realm.addAttribute(cibaState.getRequestId(), IVR_REASON, "User denied");
        realm.removeCibaState(cibaState);
        updateTransaction(cibaState.getRequestId(), clientModel, userModel, realm);
        try {
            String toPhoneNumber = getPhoneNumber(userModel, null);
            String cibaAuthorizer = clientModel.getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER);
            if (cibaAuthorizer != null) {
                CibaAuthConfigModel cibaAuthConfigByProviderId = session.getContext().getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer, clientModel.getClientId(), realm.getId());
                boolean isCustom = cibaAuthConfigByProviderId.getConfig().get(TWILIO_PROVIDER).equals(TWILIO_CUSTOM);

                if (!isCustom) {
                    updateIVRTransaction(clientModel,
                            userModel,
                            realm,
                            toPhoneNumber,
                            cibaState,
                            getAuthString(cibaAuthConfigByProviderId.getConfig(), false, session.realms().getRealmByName(Config.getAdminRealm()))
                    );
                }
            }
        } catch (SmsException e) {
            e.printStackTrace();
        }
        sendUnAuthorized(callbackUrl, cibaState, event, null);
    }

    @Override
    public boolean validateUserDetails(UserModel user, RealmModel realm, CibaAuthConfigModel cibaAuthConfigModel, String requestId) {
        if (cibaAuthConfigModel != null) {
            String cibaAuthProviderId = cibaAuthConfigModel.getCibaAuthProviderId();
            if ((cibaAuthProviderId.equals(getId())) && (user.getMobileNumber() == null || user.getMobileNumber().isEmpty() || !GeneralHelper.isValidPhoneNumber(user, realm))) {
                event.error("Enter a valid phone number without country code");
                return false;
            }
        }
        return true;
    }

    @Override
    public String getStep1(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String actionFlow) {
        String mobileNumber = userModel.getMobileNumber() == null ? "No Phone available for the user" : userModel.getMobileNumber();
        String mobileRegion = userModel.getMobileNumberRegion() == null ? "No Phone region available for the user" : userModel.getMobileNumberRegion();
        String data = "{\n" +
                "    \"user\": {\n" +
                "        \"phone\": \"" + mobileNumber + "\",\n" +
                "            \"phoneRegion\": \"" + mobileRegion + "\",\n" +
                "            \"metadata\": {\n" +
                "                \"" + cibaAuthConfigModel.getConfig().get(PIN_USER_ATTRIBUTE) + "\": \"1234\"\n" +
                "            }\n" +
                "    },\n" +
                "    \"message\": {\n" +
                "        \"metadata\": {\n" +
                "            \"ivr_sender\": \"" + clientModel.getName() + "\",\n" +
                "            \"ivr_purpose_message\": \"this is a test message\",\n" +
                "            \"ivr_validation_message\": \"please enter value provided for testing under metadata " + cibaAuthConfigModel.getConfig().get(PIN_USER_ATTRIBUTE) + "\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        return GeneralHelper.updateUserMetadata(session, clientModel, data, session.getContext().getRealm(), actionFlow);
    }

    @Override
    public String getStep2(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel) {
        return "Check your phone to complete the call";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "twilio-authorizer-title";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String TWILIO_REST_API = "twilio_rest_api";

    public static final String TWILIO_FROM_NO = "twilio_from_no";

    public static final String TWILIO_ACCOUNT_SID = "twilio_account_sid";

    public static final String TWILIO_AUTH_TOKEN = "twilio_auth_token";

    public static final String PIN_USER_ATTRIBUTE = "pin_user_attribute";

    static {

        ProviderConfigProperty property;

        Map<String, Object> twilioConfigDependsOn = new HashMap<>();
        twilioConfigDependsOn.put(IVR_PROVIDER, CIBA_TWILIO);
        twilioConfigDependsOn.put(TWILIO_PROVIDER, TWILIO_CUSTOM);

        property = new ProviderConfigProperty();
        property.setName(VERIFICATION_EXPIRY_CONFIG);
        property.setLabel("ciba-link-expiry-duration");
        property.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        property.setHelpText("ciba-link-expiry-duration-helptext");
        property.setDefaultValue("120s");
        configProperties.add(property);

        List<String> pushTypes = new ArrayList<>();
        pushTypes.add(CIBA_TWILIO);
        property = new ProviderConfigProperty();
        property.setName(IVR_PROVIDER);
        property.setLabel("ivr-provider-label");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(pushTypes);
        property.setDefaultValue(CIBA_TWILIO);
        property.setHelpText("ivr-provider-hlp-txt");
        configProperties.add(property);

        List<String> twilioTypes = new ArrayList<>();
        twilioTypes.add(TWILIO_DEFAULT);
        twilioTypes.add(TWILIO_CUSTOM);
        property = new ProviderConfigProperty();
        property.setName(TWILIO_PROVIDER);
        property.setLabel("twilio-provider-label");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(twilioTypes);
        property.setDefaultValue(TWILIO_DEFAULT);
        property.setHelpText("twilio-provider-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TWILIO_REST_API);
        property.setLabel("twilio-flow-rest-api-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setDependsOn(twilioConfigDependsOn);
        property.setHelpText("twilio-flow-rest-api-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TWILIO_FROM_NO);
        property.setLabel("from-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setDependsOn(twilioConfigDependsOn);
        property.setHelpText("twilio-from-number-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TWILIO_ACCOUNT_SID);
        property.setLabel("account-sid-label");
        property.setRequired(true);
        property.setDependsOn(twilioConfigDependsOn);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("twilio-account-sid-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TWILIO_AUTH_TOKEN);
        property.setLabel("auth-token-label");
        property.setType(ProviderConfigProperty.SECRET);
        property.setRequired(true);
        property.setSecret(true);

        property.setDependsOn(twilioConfigDependsOn);
        property.setHelpText("auth-token-hlp-txt");
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName(PIN_USER_ATTRIBUTE);
        property.setLabel("pin-map-user-attribute-label");
        property.setRequired(true);
        Map<String, Object> pinUserAttributeDependsOn = new HashMap<>();
        pinUserAttributeDependsOn.put(IVR_PROVIDER, CIBA_TWILIO);
        property.setDependsOn(pinUserAttributeDependsOn);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("pin-map-user-attribute-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setHelpText("ivr-price-warn");
        configProperties.add(property);

    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public CibaAuthProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public boolean isTemplateSupported() {
        return false;
    }

    @Override
    public String getId() {
        return "twilio-ivr-ciba-authorizer";
    }

    private String getAuthString(Map<String, String> configModel, boolean isCustom, RealmModel adminRealm) {
        String twilioAccountSID = isCustom ? configModel.get(TWILIO_ACCOUNT_SID) : adminRealm.getAttribute("TWILIO_ACCOUNT_SID");
        String twilioAuthToken = isCustom ? configModel.get(TWILIO_AUTH_TOKEN) : adminRealm.getAttribute("TWILIO_AUTH_TOKEN");
        String credentials = twilioAccountSID + ":" + twilioAuthToken;
        String encoded = DatatypeConverter.printBase64Binary(credentials.getBytes(StandardCharsets.US_ASCII));
        return "Basic " + encoded;
    }

    @Override
    public void onCleanUp(KeycloakSession session, RealmModel realm, ClientModel client) {
        CibaAuthConfigModel authenticationConfig = realm.getCibaAuthConfigByClientId(client.getClientId(), realm.getId());
        if (authenticationConfig != null) {
            realm.removeCibaAuthConfig(authenticationConfig);
        }
    }
}
