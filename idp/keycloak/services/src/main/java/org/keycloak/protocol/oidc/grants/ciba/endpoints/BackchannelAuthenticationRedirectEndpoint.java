/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.util.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.keycloak.authentication.authenticators.utils.Constants.HOOK_WAY;
import static org.keycloak.models.Constants.CIBA;
import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.updateResult;

public class BackchannelAuthenticationRedirectEndpoint extends AbstractCibaEndpoint {

    private static final String SP_WORKFLOW = "sp@workflow";
    private static final String REASON = "reason";
    private static final String ONBOARDING_FAILED = "Onboarding Failed";

    private static final Logger logger = Logger.getLogger(BackchannelAuthenticationRedirectEndpoint.class);

    @Context
    private HttpRequest httpRequest;

    public BackchannelAuthenticationRedirectEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
        event.event(EventType.EZ_TRANSACTION);
    }

    @Path("/{clientid}/{signature}/{payload}")
    @GET
    @NoCache
    public Response processAuthenticationChannelResult(@PathParam("clientid") String clientId,
                                                       @PathParam("signature") String signature, @PathParam("payload") String payload,
                                                       @QueryParam("code") String authorizationCode) {
        event.client(clientId);
        LoginFormsProvider provider = session.getProvider(LoginFormsProvider.class);
        ClientModel clientModel = session.getContext().getRealm().getClientByClientId(clientId);
        String secret = clientModel.getSecret();
        ClientRepresentation clientRepresentation = ModelToRepresentation.toRepresentation(clientModel, session);
        Map<String, String> result = null;

        String cibaAuthorizer = clientModel.getAuthenticationFlowBindingOverride("ciba_auth");
        CibaAuthProvider cibaAuthProvider = session.getProvider(CibaAuthProvider.class, cibaAuthorizer);
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        String callbackUrl = CibaGrantType.authenticationUrl(backendUriInfo.getBaseUriBuilder()).build(realm.getName())
                .toString();
        CibaStateModel cibaStateModel = session.getContext().getRealm().getCibaStateById(payload.split("_")[0]);
        UserModel userModel = null;

        if (cibaStateModel == null) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "Transaction already completed.");
            event.error("ezto state for the request not found. Possible reuse of already used verification request or" +
                    " QR");

            return provider
                    .setAttribute("cibaState", "verification_failed")
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", "access-denied")
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("ciba-redirect.ftl");
        }

        String actionFlow = session.getContext().getRealm().getTrxValue(cibaStateModel.getRequestId(), SP_WORKFLOW);
        boolean isVerificationFlow = actionFlow == null || (actionFlow.equalsIgnoreCase("verification"));

        if (cibaStateModel != null) {
            userModel = session.users().getUserByUsername(realm, cibaStateModel.getUserId());
            event.detail("requestId", cibaStateModel.getRequestId());

            event.client(clientId).user(userModel).event(EventType.EZ_TRANSACTION_START)
                    .detail("requestId", cibaStateModel.getRequestId()).success();
        }
        UserTransactionModel userTransactionDetails = realm.getUserTransactionDetails(cibaStateModel.getRequestId(), userModel.getUsername());

        final String NO_ACTION_REQUIRED = "no-action-required-error";
        try {
            result = CibaHook.constructEvent(payload, signature, secret, realm.getTrxValue(cibaStateModel.getRequestId(), "linkExp"));
        } catch (SignatureVerificationException e) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "Invalid Signature. Looks like attack");
            event.success();
            e.printStackTrace();
            if (cibaStateModel != null) {
                try {
                    cibaAuthProvider.onAuthFailed(clientModel, userModel, callbackUrl, cibaStateModel, event, null);
                } catch (ErrorResponseException error) {
                    return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                            isVerificationFlow ? "verification_failed" : "onboarding_failed", NO_ACTION_REQUIRED);
                }
            }
            return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                    isVerificationFlow ? "verification_failed" : "onboarding_failed", "access-denied");

        } catch (LinkExpirationVerificationException e) {
            event.event(EventType.EZ_TRANSACTION_EXPIRED);
            event.detail(REASON, "Link Expired");
            if (userTransactionDetails != null && userTransactionDetails.getStatus().equals(UserTransactionStatus.USER_CANCELLED.getDisplayName())) {
                realm.updateTransactionStatus(cibaStateModel.getRequestId(), userModel.getUsername(), UserTransactionStatus.EXPIRED.getDisplayName());
            }
            event.success();
            session.setAttribute(SP_WORKFLOW, realm.getTrxValue(cibaStateModel.getRequestId(), SP_WORKFLOW));
            if (cibaStateModel != null) {
                try {
                    cibaAuthProvider.onAuthFailed(clientModel, userModel, callbackUrl, cibaStateModel, event, null);
                } catch (ErrorResponseException error) {
                    if (isHookWayPush(clientModel)) {
                        updateResult(session, realm, clientModel, userModel, cibaStateModel.getRequestId());
                    }

                    logger.error(String.format("ErrorResponseException: %s", error.getErrorDescription()));
                    return provider
                            .setAttribute("cibaState", "link_expired")
                            .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                            .setAttribute("cibaResponse", NO_ACTION_REQUIRED)
                            .setAttribute("showLanguageSelector", "false")
                            .createForm("ciba-redirect.ftl");
                }
            }

            return provider
                    .setAttribute("cibaState", "link_expired")
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", NO_ACTION_REQUIRED)
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("ciba-redirect.ftl");

        }

        if (result == null) {
            event.event(EventType.EZ_TRANSACTION_EXPIRED);
            event.detail(REASON, "Link Expired");

            if (userTransactionDetails != null && !userTransactionDetails.getStatus().equals(UserTransactionStatus.USER_CANCELLED.getDisplayName())) {
                realm.updateTransactionStatus(cibaStateModel.getRequestId(), userModel.getUsername(), UserTransactionStatus.EXPIRED.getDisplayName());
            }
            event.success();
            return provider
                    .setAttribute("cibaState", "link_expired")
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", NO_ACTION_REQUIRED)
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("ciba-redirect.ftl");
        }

        String authorizerProviderId = result.get("authorizer");

        cibaAuthProvider = session.getProvider(CibaAuthProvider.class, authorizerProviderId);
        String state = result.get("state");
        cibaStateModel = session.getContext().getRealm().getCibaStateById(state);
        if (cibaStateModel == null) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "Authentication Failed");
            event.success();
            return provider
                    .setAttribute("cibaState", "link_expired")
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", NO_ACTION_REQUIRED)
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("ciba-redirect.ftl");
        }

        userModel = session.users().getUserByUsername(realm, cibaStateModel.getUserId());
        if (userModel == null) {
            if (cibaAuthorizer.equals("azure-b2c-ciba-authorizer") || cibaAuthorizer.equals("ciba-default-managed" +
                    "-auth")) {
                CibaAuthConfigModel cibaAuthConfigModel =
                        session.getContext().getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer,
                                clientModel.getClientId(),
                                realm.getId());
                cibaAuthProvider.onAuthFailed(clientModel, null, callbackUrl, cibaStateModel, event, null);
            }

            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "Requested user is not available");
            event.success();
            return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                    isVerificationFlow ? "verification_failed" : "onboarding_failed", NO_ACTION_REQUIRED);
        }
        event.user(userModel);
        String redirectUrl = CibaGrantType.redirectUrl(backendUriInfo.getBaseUriBuilder()).build(realm.getName())
                .toString();
        redirectUrl += "/" + clientId + "/" + URLEncoder.encode(signature, StandardCharsets.UTF_8) + "/" + payload;
        Map<String, Object> response = new HashMap<>();
        response.put("authorization_code", authorizationCode);
        response.put("redirectUrl", redirectUrl);

        logger.info("BACKCHANNEL REDIRECT::");
        logger.info(response);
        if (secret == null) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "No Secret available for the requested application");
            event.success();
            try {
                cibaAuthProvider.onAuthFailed(clientModel, userModel, callbackUrl, cibaStateModel, event, response);
            } catch (ErrorResponseException error) {
                return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                        "Failed", NO_ACTION_REQUIRED);
            }
            return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                    "Failed", NO_ACTION_REQUIRED);
        }

        if (authorizationCode == null || authorizationCode.isEmpty()) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "User is unauthorized. No code received");
            if (userTransactionDetails != null && !userTransactionDetails.getStatus().equals(UserTransactionStatus.USER_CANCELLED.getDisplayName())) {
                realm.updateTransactionStatus(cibaStateModel.getRequestId(), userModel.getUsername(), UserTransactionStatus.FAILURE.getDisplayName());
            }
            event.success();
            session.setAttribute(SP_WORKFLOW, realm.getTrxValue(cibaStateModel.getRequestId(), SP_WORKFLOW));
            try {
                cibaAuthProvider.onAuthFailed(clientModel, userModel, callbackUrl, cibaStateModel, event, response);
            } catch (ErrorResponseException error) {
                return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                        isVerificationFlow ? "verification_failed" : "onboarding_failed", NO_ACTION_REQUIRED);
            }
            return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                    isVerificationFlow ? "verification_failed" : "onboarding_failed", NO_ACTION_REQUIRED);
        }

        if (!clientRepresentation.getAppType().equals(CIBA)) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "Request came from invalid application");
            if (userTransactionDetails != null && !userTransactionDetails.getStatus().equals(UserTransactionStatus.USER_CANCELLED.getDisplayName())) {
                realm.updateTransactionStatus(cibaStateModel.getRequestId(), userModel.getUsername(), UserTransactionStatus.FAILURE.getDisplayName());
            }
            event.success();
            try {
                cibaAuthProvider.onAuthFailed(clientModel, userModel, callbackUrl, cibaStateModel, event, response);
            } catch (ErrorResponseException error) {
                return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                        isVerificationFlow ? "verification_failed" : "onboarding_failed", "Invalid Client");
            }
            return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                    isVerificationFlow ? "verification_failed" : "onboarding_failed", "Invalid Client");
        }
        CibaAuthConfigModel cibaAuthConfigModel = session.getContext().getRealm()
                .getCibaAuthConfigByProviderId(authorizerProviderId, clientModel.getClientId(), realm.getId());
        try {
            cibaAuthProvider.onAuthCompleted(clientModel, userModel, cibaAuthConfigModel, callbackUrl, cibaStateModel, event,
                    response);
        } catch (ErrorResponseException e) {
            return GeneralHelper.failureFtl(realm, cibaStateModel, cibaAuthProvider, provider, clientModel, isVerificationFlow,
                    isVerificationFlow ? "verification_failed" : "onboarding_failed", "Invalid Request");
        }
        event.detail("requestId", cibaStateModel.getRequestId()).event(EventType.EZ_TRANSACTION_COMPLETED).success();
        if (userTransactionDetails != null) {
            realm.updateTransactionStatus(cibaStateModel.getRequestId(), userModel.getUsername(), UserTransactionStatus.SUCCESS.getDisplayName());
        }

        if (realm.hasTrxValueAttribute(cibaStateModel.getRequestId(), "ik@custom_message")) {
            Map<String, String> config = new HashMap<>();
            try {
                config = JsonSerialization.readValue(realm.getTrxValue(cibaStateModel.getRequestId(), "ik@custom_message"), Map.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (isHookWayPush(clientModel)) {
                Map<String, String> attributes = realm.getTrx(cibaStateModel.getRequestId()).getAttributes();
                if (!(attributes != null
                        && ((attributes.containsKey("liveness_completed") &&
                        !Boolean.parseBoolean(attributes.get("liveness_completed")))
                        || (attributes.containsKey("ik@video_kyc_completed") &&
                        !Boolean.parseBoolean(attributes.get("ik@video_kyc_completed")))))) {
                    updateResult(session, realm, clientModel, userModel, cibaStateModel.getRequestId());
                }
            }

            return provider
                    .setAttribute("logo", GeneralHelper.getPresignedObjectUrl(realm.getName(), config.get("custom-message-logo")))
                    .setAttribute("cibaTitle", config.get("custom-message-title"))
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", config.get("custom-message-desc"))
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("custom-message.ftl");
        }

        if (isHookWayPush(clientModel)) {
            Map<String, String> attributes = realm.getTrx(cibaStateModel.getRequestId()).getAttributes();
            if (!(attributes != null
                    && ((attributes.containsKey("liveness_completed") &&
                    !Boolean.parseBoolean(attributes.get("liveness_completed")))
                    || (attributes.containsKey("ik@video_kyc_completed") &&
                    !Boolean.parseBoolean(attributes.get("ik@video_kyc_completed")))))) {
                updateResult(session, realm, clientModel, userModel, cibaStateModel.getRequestId());
            }
        }

        boolean isCustomVerificationMsgEnabled = isVerificationFlow && Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomVerificationMsg"));
        boolean isCustomOnboardingMsgEnabled = !isVerificationFlow && Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomOnboardingMsg"));
        String title = isVerificationFlow ? clientModel.getAttributes().get("customSuccessVerificationMsgTitle") : clientModel.getAttributes().get("customSuccessOnboardingMsgTitle");
        String description = isVerificationFlow ? clientModel.getAttributes().get("customSuccessVerificationMsgDesc") : clientModel.getAttributes().get("customSuccessOnboardingMsgDesc");

        if (((isVerificationFlow && isCustomVerificationMsgEnabled) || (!isVerificationFlow && isCustomOnboardingMsgEnabled)) && title != null) {
            return provider
                    .setAttribute("cibaState", isVerificationFlow ? "verification_success" : "onboarding_success")
                    .setAttribute("cibaTitle", title)
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", description)
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("custom-message.ftl");
        }

        boolean showStatusCard = isVerificationFlow ? Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomVerificationMsg")) : Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomOnboardingMsg"));
        String ftl = (cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop()) && !showStatusCard ? "ciba-empty-fallback.ftl" : "ciba-redirect.ftl";

        return provider
                .setAttribute("cibaState", isVerificationFlow ? "verification_success" : "onboarding_success")
                .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                .setAttribute("cibaResponse",
                        cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop()
                                ? "no-action-required"
                                : "close-window-safely")
                .setAttribute("showStatusCard", showStatusCard)
                .setAttribute("showLanguageSelector", "false")
                .createForm(ftl);
    }


    @Path("/{clientid}/{signature}/{payload}")
    @POST
    @NoCache
    public Response processAuthenticationChannelPostResult(@PathParam("clientid") String clientId,
                                                           @PathParam("signature") String signature, @PathParam("payload") String payload,
                                                           final Map<String, Object> request) {
        event.client(clientId);
        if (clientId == null || signature == null || payload == null) {
            event.error("Invalid Request");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        ClientModel clientModel = session.getContext().getRealm().getClientByClientId(clientId);
        String secret = clientModel.getSecret();

        if (secret == null) {
            event.error("No Secret available for the requested client");
            return Response.status(Response.Status.BAD_REQUEST).entity("No Secret available for the requested client")
                    .build();
        }

        ClientRepresentation clientRepresentation = ModelToRepresentation.toRepresentation(clientModel, session);

        if (!clientRepresentation.getAppType().equals(CIBA)) {
            event.error("Request came from invalid client");
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid client").build();
        }
        String cibaAuthorizer = clientModel.getAuthenticationFlowBindingOverride("ciba_auth");
        CibaAuthProvider cibaAuthProvider = session.getProvider(CibaAuthProvider.class, cibaAuthorizer);

        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        String callbackUrl = CibaGrantType.authenticationUrl(backendUriInfo.getBaseUriBuilder()).build(realm.getName())
                .toString();
        CibaStateModel cibaStateModel = session.getContext().getRealm().getCibaStateById(payload.split("_")[0]);
        UserModel userModel = null;
        if (cibaStateModel != null) {
            userModel = session.users().getUserByUsername(realm, cibaStateModel.getUserId());
            event.detail("requestId", cibaStateModel.getRequestId());
            event.client(clientId).user(userModel).event(EventType.EZ_TRANSACTION_START)
                    .detail("requestId", cibaStateModel.getRequestId()).success();
        }
        Map<String, String> result = null;
        try {
            result = CibaHook.constructEvent(payload, signature, secret, realm.getTrxValue(cibaStateModel.getRequestId(), "linkExp"));

        } catch (SignatureVerificationException e) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "Invalid Signature. Looks like attack");
            event.success();
            if (cibaStateModel != null) {
                try {
                    cibaAuthProvider.onAuthFailed(clientModel, userModel, callbackUrl, cibaStateModel, event, null);
                } catch (ErrorResponseException error) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
                }
            }
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (LinkExpirationVerificationException e) {
            event.event(EventType.EZ_TRANSACTION_EXPIRED);
            event.detail(REASON, "Link Expired");
            event.success();
            session.setAttribute(SP_WORKFLOW, realm.getTrxValue(cibaStateModel.getRequestId(), SP_WORKFLOW));
            if (cibaStateModel != null) {
                try {
                    cibaAuthProvider.onAuthFailed(clientModel, userModel, callbackUrl, cibaStateModel, event, null);
                } catch (ErrorResponseException error) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
                }
            }
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        String authorizerProviderId = result.get("authorizer");
        cibaAuthProvider = session.getProvider(CibaAuthProvider.class, authorizerProviderId);
        String state = result.get("state");
        cibaStateModel = session.getContext().getRealm().getCibaStateById(state);
        if (cibaStateModel == null) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "Invalid State");
            event.success();
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid State").build();
        }
        event.detail("requestId", cibaStateModel.getRequestId());
        event.user(userModel);
        if (userModel == null) {
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail(REASON, "Requested user is not available");
            event.success();
            try {
                cibaAuthProvider.onAuthFailed(clientModel, userModel, callbackUrl, cibaStateModel, event, null);
            } catch (ErrorResponseException error) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Requested user is not found").build();
            }
            return Response.status(Response.Status.BAD_REQUEST).entity("Requested user is not found").build();
        }
        CibaAuthConfigModel cibaAuthConfigModel = session.getContext().getRealm()
                .getCibaAuthConfigByProviderId(authorizerProviderId, clientModel.getClientId(), realm.getId());
        cibaAuthProvider.onAuthCompleted(clientModel, userModel, cibaAuthConfigModel, callbackUrl, cibaStateModel,
                event, request);
        event.detail("requestId", cibaStateModel.getRequestId()).event(EventType.EZ_TRANSACTION_COMPLETED).success();
        return Response.status(Response.Status.OK).entity("Authentication is completed").build();
    }

    protected Theme getTheme() throws IOException {
        return session.theme().getTheme(Theme.Type.LOGIN);
    }

    private boolean isHookWayPush(ClientModel clientModel) {
        CibaConfig cibaPolicy = realm.getCibaPolicy();
        String backchannelTokenDeliveryMode = cibaPolicy.getBackchannelTokenDeliveryMode(clientModel);
        CibaNotificationConfigModel configModel = realm.getCibaNotificationConfigByProviderId(backchannelTokenDeliveryMode, clientModel.getClientId(), realm.getId());
        if (configModel == null || configModel.getConfig() == null)
            return false;
        String hookWay = configModel.getConfig().get(HOOK_WAY);
        if (hookWay == null || hookWay.isEmpty())
            return false;
        return hookWay.equalsIgnoreCase("push");
    }
}
