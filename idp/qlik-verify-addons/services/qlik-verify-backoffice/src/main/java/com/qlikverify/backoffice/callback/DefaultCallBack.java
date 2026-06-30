package com.qlikverify.backoffice.callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qlikverify.transaction.CallBackService;
import com.qlikverify.transaction.CallBackServiceProviderFactory;
import com.qlikverify.transaction.TransactionStatus;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.ciba.CibaNotificationProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.qlikverify.transaction.TransactionStatus.CANCELLED;

public class DefaultCallBack implements CallBackService, CallBackServiceProviderFactory {
    KeycloakSession session;

    EventBuilder event;
    private static final String ERROR = "error";
    private static final String INVALID_TOKEN = "Invalid token";
    private static final Logger logger = Logger.getLogger(DefaultCallBack.class);

    @Override
    public CallBackService setEventBuilder(EventBuilder eventBuilder)
    {
        this.event = eventBuilder;
        return this;
    }

    @Override
    public void verifyResult(String token, TransactionStatus status,Map<String, String> params) {
        final String realmId = session.getContext().getRealm().getId();
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), innerSession -> {
            try {
                Resteasy.pushContext(KeycloakSession.class, session);
                Resteasy.pushContext(RealmModel.class, session.getContext().getRealm());
                Resteasy.pushContext(KeycloakContext.class, session.getContext());
                innerSession.getContext().setRealm(session.getContext().getRealm());
                Resteasy.pushContext(ClientModel.class, session.getContext().getClient());
                Resteasy.pushContext(KeycloakUriInfo.class, session.getContext().getUri());
                Resteasy.pushContext(URI.class, session.getContext().getUri().getBaseUri());

                RealmModel realm = innerSession.realms().getRealm(realmId);
                BackchannelAuthCallbackContext ctx = verifyAuthenticationRequest(innerSession, token, realm);
                AccessToken bearerToken = ctx.bearerToken;
                OAuth2DeviceCodeModel deviceModel = ctx.deviceModel;

                if (status == null) {
                    event.error(Errors.INVALID_REQUEST);
                    throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid authentication status",
                            Response.Status.BAD_REQUEST);
                }
                boolean isSuccess = false;
                switch (status) {
                    case SUCCEED:
                        approveRequest(bearerToken, params);
                        isSuccess = true;
                        break;
                    case CANCELLED:
                    case UNAUTHORIZED:
                        denyRequest(bearerToken, status, params);
                        break;
                }
                // Call the notification endpoint
                ClientModel client = realm.getClientByClientId(bearerToken.getIssuedFor());
                event.client(client.getId());
                CibaConfig cibaConfig = realm.getCibaPolicy();
                String notProviderId = cibaConfig.getBackchannelTokenDeliveryMode(client);
                CibaNotificationProvider cibaNotificationProvider = innerSession.getProvider(CibaNotificationProvider.class, notProviderId);
                CibaNotificationConfigModel configModel = realm.getCibaNotificationConfigByProviderId(notProviderId, client.getClientId(), realm.getId());
                if (isSuccess)
                    cibaNotificationProvider.setEventBuilder(event).sendNotification(innerSession, realm, client, cibaConfig, deviceModel, configModel, null,
                            params);
                else {
                    if(realm.getTrx(deviceModel.getClientNotificationToken()).getAttributes().containsKey("sp@web_to_device")
                            && Boolean.parseBoolean(realm.getTrx(deviceModel.getClientNotificationToken()).getAttributes().get("sp@web_to_device"))
                            && realm.getCibaAuthConfigByClientId(client.getClientId(), realm.getId()).getCibaAuthProviderId().equals("ciba-mobile-sdk")){
                        realm.addAttribute(deviceModel.getClientNotificationToken(), "sp@web_to_device_error", "access_denied");
                    }
                    cibaNotificationProvider.setEventBuilder(event).sendError(innerSession, realm, client, cibaConfig, deviceModel, configModel, null, params);
                }
            } catch (Exception e) {
                logger.error("DefaultCallBack::verifyResult::" + e.getMessage());
                e.printStackTrace();
                logger.error("DefaultCallBack::verifyResult::" + e.getMessage());
                innerSession.getTransactionManager().rollback();
            }
        });
    }

    @Override
    public CallBackService create(KeycloakSession session) {
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
    public String getId() {
        return "default-callback";
    }


    private BackchannelAuthCallbackContext verifyAuthenticationRequest(KeycloakSession innerSession, String rawBearerToken, RealmModel realm) {
        final AtomicReference<BackchannelAuthCallbackContext> result = new AtomicReference<>();
        if (rawBearerToken == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, INVALID_TOKEN, Response.Status.UNAUTHORIZED);
        }

        AccessToken bearerToken;

        try {
            bearerToken = TokenVerifier.createWithoutSignature(innerSession.tokens().decode(rawBearerToken, AccessToken.class))
                    .withDefaultChecks()
                    .realmUrl(Urls.realmIssuer(innerSession.getContext().getUri().getBaseUri(), realm.getName()))
                    .checkActive(true)
                    .audience(Urls.realmIssuer(innerSession.getContext().getUri().getBaseUri(), realm.getName()))
                    .verify().getToken();
        } catch (Exception e) {
            logger.error(String.format("DefaultCallBack::verifyAuthenticationRequest::%s::%s", INVALID_TOKEN, e.getMessage()));
            // authentication channel id format is invalid or it has already been used
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, INVALID_TOKEN, Response.Status.FORBIDDEN);
        }

        OAuth2DeviceTokenStoreProvider store = innerSession.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel deviceCode = store.getByUserCode(realm, bearerToken.getId());

        if (deviceCode == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, INVALID_TOKEN, Response.Status.FORBIDDEN);
        }

        if (!deviceCode.isPending()) {
            cancelRequest(bearerToken.getId());
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, INVALID_TOKEN, Response.Status.FORBIDDEN);
        }

        ClientModel issuedFor = realm.getClientByClientId(bearerToken.getIssuedFor());
        innerSession.getContext().setClient(issuedFor);

        if (issuedFor == null || !issuedFor.isEnabled()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid token recipient",
                    Response.Status.BAD_REQUEST);
        }

        if (!deviceCode.getClientId().equals(issuedFor.getClientId())) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Token recipient mismatch",
                    Response.Status.BAD_REQUEST);
        }

        innerSession.getContext().setClient(issuedFor);
        event.client(issuedFor.getClientId());

        result.set(new BackchannelAuthCallbackContext(bearerToken, deviceCode));

        return result.get();
    }

    private void cancelRequest(String authResultId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel userCode = store.getByUserCode(session.getContext().getRealm(), authResultId);
        store.removeDeviceCode(session.getContext().getRealm(), userCode.getDeviceCode());
        store.removeUserCode(session.getContext().getRealm(), authResultId);
    }

    private void approveRequest(AccessToken authReqId, Map<String, String> additionalParams) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.approve(session.getContext().getRealm(), authReqId.getId(), "fake", additionalParams);
    }

    private void denyRequest(AccessToken authReqId, TransactionStatus status, Map<String,String> params) {
        if(params != null) {
            if (params.containsKey(ERROR)) {
                logger.error("Request denied:: " + params.get(ERROR));
            } else {
                if (CANCELLED.equals(status)) {
                    logger.error("Request denied:: " + Errors.NOT_ALLOWED);
                } else {
                    logger.error("Request denied:: " + Errors.CONSENT_DENIED);
                }
            }
        }

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);

        store.deny(session.getContext().getRealm(), authReqId.getId());
    }

    private class BackchannelAuthCallbackContext {

        private final AccessToken bearerToken;
        private final OAuth2DeviceCodeModel deviceModel;

        private BackchannelAuthCallbackContext(AccessToken bearerToken, OAuth2DeviceCodeModel deviceModel) {
            this.bearerToken = bearerToken;
            this.deviceModel = deviceModel;
        }

    }
}
