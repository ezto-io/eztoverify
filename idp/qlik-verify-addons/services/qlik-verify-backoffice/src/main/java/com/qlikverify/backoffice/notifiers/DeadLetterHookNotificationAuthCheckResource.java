package com.qlikverify.backoffice.notifiers;

import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundHookNotifierService.BackgroundHookNotifierRequestBody;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.DefaultClientRegistrationPolicies;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AESHelper;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.qlikverify.backoffice.Constants.*;

public class DeadLetterHookNotificationAuthCheckResource extends BaseAuthCheckResource {
    public DeadLetterHookNotificationAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @POST
    @Path("/send-result")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendResultToHook(@Context HttpRequest req) {
        BackgroundJobActionTokenVerifier tokenVerifier = new BackgroundJobActionTokenVerifier(session,
                session.getContext().getRealm());
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);

        if (actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        try {
            String body = getBody(req);
            Map<String, String> hookResponseModel = JsonSerialization.readValue(body, Map.class);
            JsonWebToken jsonWebToken = tokenVerifier.validateToken(actionToken,
                    hookResponseModel.get("realmId"),
                    false);

            if (jsonWebToken == null || jsonWebToken.isExpired()) {
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }

            RealmModel realm = session.realms().getRealm(hookResponseModel.get("realmId"));
            session.getContext().setRealm(realm);
            final ClientModel client = session.clients().getClientByClientId(realm, hookResponseModel.get("clientId"));
            session.getContext().setClient(client);
            UserModel userModel = session.users().getUserByUsername(realm, hookResponseModel.get("userId"));
            String reqId = hookResponseModel.get("requestId");
            String userId = hookResponseModel.get("userId");


            if (client == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Client not found").build();
            }

            CibaDeadLetterHookConfigModel deadLetterHookNotificationConfigByClientId = realm.getDeadLetterHookNotificationConfigByClientId(client.getClientId(), realm.getId());
            if(deadLetterHookNotificationConfigByClientId == null || deadLetterHookNotificationConfigByClientId.getConfig() == null || deadLetterHookNotificationConfigByClientId.getConfig().isEmpty()) {
                return Response.ok().entity("ok").build();
            }

            TrxModel trxModel = realm.getTrx(reqId);
            if(trxModel == null || trxModel.getAttributes() == null || trxModel.getAttributes().isEmpty()) {
                return Response.ok().entity("ok").build();
            }

            if(deadLetterHookNotificationConfigByClientId.getConfig().get("enable").equalsIgnoreCase("false")) {
                removeTrxAttributes(realm,trxModel);
                updateTransaction(reqId, client, userModel, realm);
                return Response.ok().entity("ok").build();
            }

            String deadLetterHookUrl = deadLetterHookNotificationConfigByClientId.getConfig().get("notification-endpoint");
            String token = deadLetterHookNotificationConfigByClientId.getConfig().get(NOTIFICATION_ENDPOINT_AUTH);
            boolean isEncrypted = ENCRYPTED.equalsIgnoreCase(deadLetterHookNotificationConfigByClientId.getConfig().get("notification-response-way"));

            HashMap<String, String> map = new HashMap<>();
            trxModel.getAttributes().entrySet().forEach(x-> {
                if(!x.getKey().equalsIgnoreCase("linkExp") && !x.getKey().endsWith("_counter"))
                    map.put(x.getKey(), x.getValue());
            });

            String transactionStatus = "";
            UserModel user = session.users().getUserByUsername(realm, userId);
            if (user == null) {
                transactionStatus = "failure";
            } else {
                UserTransactionModel userTransactionDetails = realm.getUserTransactionDetails(reqId, userId);
                transactionStatus = userTransactionDetails.getStatus().toLowerCase();
            }


            map.put("sp@request_id", trxModel.getTrxId());
            map.put("sp@user_id", hookResponseModel.get("userId"));
            map.put("sp@transaction_status", transactionStatus);


            String response = "";
            if(isEncrypted) {
                response = formEncryptedResponse(deadLetterHookNotificationConfigByClientId, map);
            }

            try {
                Map<String, String> fallBackDetails = new HashMap<>();
                fallBackDetails.put("clientId", client.getClientId());
                fallBackDetails.put("tenantId", realm.getId());
                fallBackDetails.put("actionToken", BackgroundJobActionTokenVerifier.
                        generateToken(session, Long.parseLong(String.valueOf(GeneralHelper.getExpiryDuration("1h"))), ""));

                BackgroundHookNotifierRequestBody requestBody = new BackgroundHookNotifierRequestBody(
                        "push",
                        deadLetterHookUrl,
                        token != null && !token.isEmpty() ? token : null,
                        isEncrypted ? JsonSerialization.writeValueAsString(response) : JsonSerialization.writeValueAsString(map),
                        JsonSerialization.writeValueAsString(fallBackDetails)
                );
                requestBody.setTriggeredFor("deadLetter");
                BackgroundServiceImpl.getInstance(session).createBackgroundHookNotifierJob(requestBody);

                updateTransaction(reqId, client, userModel, realm);
                removeTrxAttributes(realm,trxModel);
//                event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, hookWay).event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER).success();
            } catch (Exception ioe) {
//                logger.errorf(ioe, "Failed to send request to client notification endpoint '%s' of client '%s'", clientNotificationEndpoint, client.getClientId());
//                event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, hookWay).event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER).error(ioe.getMessage());
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, SEND_REQUEST_FAILED,
                        Response.Status.BAD_REQUEST);
            }
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok().entity("ok").build();
    }

    private String formEncryptedResponse(CibaDeadLetterHookConfigModel deadLetterHookNotificationConfigByClientId, HashMap<String, String> trxModel) {
        String secretKey = null;
        String response = "";
        HashMap<String, Object> config = new HashMap<>();
        config.put("secret-keys", deadLetterHookNotificationConfigByClientId.getConfig().get("secret-keys"));
        ComponentRepresentation componentRepresentation = getEncryptionComponent(session, config);
        if (componentRepresentation != null) {
            secretKey = componentRepresentation.getConfig().getFirst("secret");
        }
        try {
            response = JsonSerialization.writeValueAsString(trxModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            response = AESHelper.encrypt(response, secretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    private void removeTrxAttributes(RealmModel realmModel, TrxModel trxModel) {
        realmModel.removeTrx(trxModel.getTrxId());
    }

    private ComponentRepresentation getEncryptionComponent(KeycloakSession session, HashMap<String, Object> config) {
        ComponentModel optionalComponentModel = session.getContext().getRealm().getComponent(String.valueOf(config.get("secret-keys")));
        if (optionalComponentModel == null) {
            return null;
        }

        return ModelToRepresentation.toRepresentation(session, optionalComponentModel, true);
    }

    private void updateTransaction(String trxId,ClientModel clientModel, UserModel userModel, RealmModel realmModel) {
        if (userModel == null) {
            return;
        }
        Optional<ComponentModel> optionalComponentModel = realmModel.getComponentsStream(realmModel.getId()).filter(x -> x.getProviderId().equalsIgnoreCase("transaction-policy-checker")).findFirst();
        DefaultClientRegistrationPolicies.addTransactionPolicy(realmModel, optionalComponentModel.isPresent());
        ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "transaction-policy-checker", new HashMap<String, Object>() {{
            put("userId", userModel.getId());
            put("clientId", clientModel.getClientId());
            put("trxId", trxId);
        }});
    }

    public static String getBody(HttpRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        return body;
    }
}
