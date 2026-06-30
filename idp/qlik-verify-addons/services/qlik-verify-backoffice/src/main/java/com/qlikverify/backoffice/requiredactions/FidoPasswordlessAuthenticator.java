/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.exception.WebAuthnException;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.*;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.credential.*;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.WebAuthnAuthenticatorsBean;
import org.keycloak.models.*;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.ServicesUtils;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.authorizers.WebAuthorizer.OPEN_IN_NEW_WINDOW;
import static com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel;
import static com.qlikverify.backoffice.helpers.GeneralHelper.showView;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;
import static org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer.CIBA_SDK;
import static org.keycloak.services.messages.Messages.*;

/**
 * Authenticator for WebAuthn authentication with passwordless credential. This class is temporary and will be likely
 * removed in the future during future improvements in authentication SPI
 */
public class FidoPasswordlessAuthenticator implements RequiredActionProvider,
        CredentialValidator<WebAuthnCredentialProvider>, RequiredActionFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(FidoPasswordlessAuthenticator.class);
    KeycloakSession session;

    public void authenticate(RequiredActionContext context) {
        if (context.getRealm().getMode().equalsIgnoreCase("bo")) {
            showChallengeView(context);
            return;
        }
        fidoVerificationChallenge(context);
    }

    private void showChallengeView(RequiredActionContext context) {
        final String authorizerId = context.getAuthenticationSession()
                .getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        CibaAuthProvider cibaAuthProvider = context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
        if (!cibaAuthProvider.isMobile() && !cibaAuthProvider.isDesktop()) {
            fidoVerificationChallenge(context);
            return;
        }

        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute("isMobile", cibaAuthProvider.isMobile());
        loginFormsProvider.setAttribute("isDesktop", cibaAuthProvider.isDesktop());
        context.challenge(loginFormsProvider.createForm("fido-check.ftl"));
    }

    protected WebAuthnPolicy getWebAuthnPolicy(RequiredActionContext context) {
        Map<String, String> config = context.getConfig();
        if (config == null)
            return null;

        Map<String, Object> configConversion = new HashMap<>();
        config.forEach((k, v) -> {
            if (v != null && !v.equals(NOT_SET)) {
                if (k.equals("webAuthnPolicySignatureAlgorithms")) {
                    List<String> options = new ArrayList<>();
                    configConversion.put(k, options.add(v));
                } else {
                    configConversion.put(k, v);
                }
            }
        });

        return ServicesUtils.toWebAuthPolicy(configConversion);
    }

    protected String getRpID(RequiredActionContext context) {
        WebAuthnPolicy policy = getWebAuthnPolicy(context);
        String rpId = policy.getRpId();
        if (rpId == null || rpId.isEmpty()) rpId = context.getUriInfo().getBaseUri().getHost();
        return rpId;
    }

    protected String getCredentialType() {
        return WebAuthnCredentialModel.TYPE_PASSWORDLESS;
    }

    protected boolean shouldDisplayAuthenticators(RequiredActionContext context) {
        return context.getUser() != null;
    }

    public void action(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel") && Boolean.parseBoolean(formData.getFirst("cancel"))) {
            handleCancel(context, EventType.EZ_FIDO_PASSWORDLESS_VERIFICATION_ERROR);
            return;
        }
        String from = formData.getFirst("from");
        if (from != null && from.equalsIgnoreCase("fido-check")) {
            deviceCheck(context, formData);
            return;
        }

        if (context.getUser() == null) {
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_VERIFICATION_ERROR,
                    ErrorMappings.USER_NOT_ONBOARDED);
            showView(context, "user_not_onboarded", BACKOFFICE_ERROR_FTL, null);
            return;
        }

        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        context.getEvent().detail(Details.CREDENTIAL_TYPE, getCredentialType());

        // receive error from navigator.credentials.get()
        String errorMsgFromWebAuthnApi = params.getFirst(WebAuthnConstants.ERROR);
        if (errorMsgFromWebAuthnApi != null && !errorMsgFromWebAuthnApi.isEmpty()) {
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_VERIFICATION_ERROR,
                    errorMsgFromWebAuthnApi);
            setErrorResponse(context, WEBAUTHN_ERROR_API_GET, errorMsgFromWebAuthnApi);
            return;
        }

        String rpId = getRpID(context);
        Set<Origin> origin = null;
        if (params.containsKey(PLATFORM) && params.getFirst(PLATFORM).equalsIgnoreCase("android")) {
            origin = com.qlikverify.backoffice.GeneralHelper.getOriginForAndroid(context.getRealm());
        } else {
            origin = new HashSet<>();
            String registeredUrl = "";
            String cibaAuthorizer =
                    context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
            if (cibaAuthorizer.equals("azure-b2c-ciba-authorizer") || cibaAuthorizer.equals("ciba-default-managed" +
                    "-auth")) {
                CibaAuthConfigModel cibaAuthConfigModel =
                        context.getAuthenticationSession().getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer,
                                context.getAuthenticationSession().getClient().getClientId(),
                                context.getAuthenticationSession().getRealm().getId());
                String notifyWay = cibaAuthConfigModel.getConfig().get("ciba-notify");
                if (cibaAuthorizer.equals("azure-b2c-ciba-authorizer") || (notifyWay != null && notifyWay.equalsIgnoreCase(CIBA_SDK))) {
                    List<String> whiteListHostUrls = Optional.ofNullable(cibaAuthConfigModel.getConfig().get("whitelist_host"))
                            .map(hosts -> Arrays.stream(hosts.split("##"))
                                    .filter(x -> !x.isEmpty())
                                    .collect(Collectors.toList()))
                            .orElse(Collections.emptyList());
                    if (whiteListHostUrls.stream().anyMatch(x -> x.equals(params.getFirst("eventOrigin")))) {
                        final String validDomain = com.qlikverify.backoffice.GeneralHelper.getValidDomain(params.getFirst("eventOrigin"));
                        if (validDomain != null) rpId = validDomain;
                        registeredUrl = params.getFirst("eventOrigin");
                        origin.add(new Origin(registeredUrl));
                    }
                }
            }
            Origin baseOrgin = new Origin(UriUtils.getOrigin(context.getUriInfo().getBaseUri()));
            origin.add(baseOrgin);

            origin.add(new Origin(UriUtils.getOrigin("https://" + rpId)));
        }

        Challenge challenge =
                new DefaultChallenge(context.getAuthenticationSession().getAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE));
        ServerProperty server = new ServerProperty(origin, rpId, challenge, null);

        byte[] credentialId = Base64Url.decode(params.getFirst(WebAuthnConstants.CREDENTIAL_ID));
        byte[] clientDataJSON = Base64Url.decode(params.getFirst(WebAuthnConstants.CLIENT_DATA_JSON));
        byte[] authenticatorData = Base64Url.decode(params.getFirst(WebAuthnConstants.AUTHENTICATOR_DATA));
        byte[] signature = Base64Url.decode(params.getFirst(WebAuthnConstants.SIGNATURE));

        String userHandle = params.getFirst(WebAuthnConstants.USER_HANDLE);
        // existing User Handle means that the authenticator used Resident Key supported public key credential
        if (userHandle == null || userHandle.isEmpty()) {
            // Resident Key not supported public key credential was used
            // so rely on the user set in a previous step (if available)
            if (context.getUser() != null) {
                userHandle = context.getUser().getId();
            } else {
                GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_VERIFICATION_ERROR, "Webauthn " +
                        "credential provided doesn't include user id and user id wasn't provided in a previous step");
                setErrorResponse(context, WEBAUTHN_ERROR_USER_NOT_FOUND,
                        "Webauthn credential provided doesn't include user id and user id wasn't provided in a " +
                                "previous step");
                return;
            }
        } else {
            if (context.getUser() != null) {
                // Resident Key supported public key credential was used,
                // so need to confirm whether the already authenticated user is equals to one authenticated by the
                // webauthn authenticator
                String firstAuthenticatedUserId = context.getUser().getId();
                if (firstAuthenticatedUserId != null && !firstAuthenticatedUserId.equals(userHandle)) {
                    context.getEvent()
                            .detail(WebAuthnConstants.FIRST_AUTHENTICATED_USER_ID, firstAuthenticatedUserId)
                            .detail(WebAuthnConstants.AUTHENTICATED_USER_ID, userHandle);
                    setErrorResponse(context, WEBAUTHN_ERROR_DIFFERENT_USER, null);
                    return;
                }
            } else {
                // Resident Key supported public key credential was used,
                // and the user has not yet been identified
                // so rely on the user authenticated by the webauthn authenticator
                // NOP
            }
        }

        boolean isUVFlagChecked = false;
        String userVerificationRequirement = getWebAuthnPolicy(context).getUserVerificationRequirement();
        if (WebAuthnConstants.OPTION_REQUIRED.equals(userVerificationRequirement)) isUVFlagChecked = true;

        UserModel user = context.getUser();

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                credentialId,
                authenticatorData,
                clientDataJSON,
                signature
        );
        WebAuthnCredentialModelInput.KeycloakWebAuthnAuthenticationParameters authenticationParameters =
                new WebAuthnCredentialModelInput.KeycloakWebAuthnAuthenticationParameters(
                        server,
                        isUVFlagChecked
                );

        WebAuthnCredentialModelInput cred = new WebAuthnCredentialModelInput(getCredentialType());

        cred.setAuthenticationRequest(authenticationRequest);
        cred.setAuthenticationParameters(authenticationParameters);

        boolean result = false;
        try {
            result = context.getSession().userCredentialManager().isValid(context.getRealm(), user, cred);
        } catch (WebAuthnException wae) {
            setErrorResponse(context, WEBAUTHN_ERROR_AUTH_VERIFICATION, wae.getMessage());
            return;
        }
        String encodedCredentialID = Base64Url.encode(credentialId);
        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), "fido_verification");
        if (result) {
            String isUVChecked = Boolean.toString(isUVFlagChecked);
            logger.debugv("WebAuthn Authentication successed. isUserVerificationChecked = {0}, PublicKeyCredentialID " +
                    "= {1}", isUVChecked, encodedCredentialID);
            context.getEvent()
                    .detail("web_authn_authenticator_user_verification_checked", isUVChecked)
                    .detail("public_key_credential_id", encodedCredentialID);
            String requestId = context.getRequestId();
            context.getRealm().addAttribute(requestId, FIDO_VERIFIED, "true");
            context.getRealm().addAttribute(requestId, FIDO_VERIFIED_AT, String.valueOf(Time.currentTime()));
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_VERIFICATION, "");
            context.success();
        } else {
            context.getEvent()
                    .detail("web_authn_authenticated_user_id", userHandle)
                    .detail("public_key_credential_id", encodedCredentialID);
            setErrorResponse(context, WEBAUTHN_ERROR_USER_NOT_FOUND, null);
        }
    }

    private void deviceCheck(RequiredActionContext context, MultivaluedMap<String, String> formData) {
        final Optional<CredentialModel> optionalCredentialModel = context.getSession().userCredentialManager()
                .getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(),
                        WebAuthnCredentialModel.TYPE_PASSWORDLESS).filter(
                        credentialModel -> {
                            try {
                                final HashMap<String, String> deserializedSecretData = JsonSerialization
                                        .readValue(credentialModel.getSecretData(),
                                                new TypeReference<HashMap<String, String>>() {
                                                });
                                return !credentialModel.isEnabled() && deserializedSecretData.get("id")
                                        .equals(formData.getFirst("deviceId"));
                            } catch (Exception e) {
                                return false;
                            }
                        }
                ).findAny();
        if (optionalCredentialModel.isPresent()) {
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_VERIFICATION_ERROR,
                    "The requested device is disabled.");
            context.failure();
            return;
        }

        fidoVerificationChallenge(context);
    }

    @Override
    public WebAuthnCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (WebAuthnCredentialProvider) session.getProvider(CredentialProvider.class,
                WebAuthnCredentialProviderFactory.PROVIDER_ID);
    }

    private static final String ERR_LABEL = "web_authn_authentication_error";
    private static final String ERR_DETAIL_LABEL = "web_authn_authentication_error_detail";

    private void setErrorResponse(RequiredActionContext context, final String errorCase, final String errorMessage) {
        Response errorResponse = null;
        switch (errorCase) {
            case WEBAUTHN_ERROR_REGISTRATION:
                logger.warn(errorCase);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .error(Errors.INVALID_USER_CREDENTIALS);
                errorResponse = createErrorResponse(context, errorCase);
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_API_GET:
                logger.warnv("error returned from navigator.credentials.get(). {0}", errorMessage);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .detail(ERR_DETAIL_LABEL, errorMessage)
                        .error(Errors.NOT_ALLOWED);
                errorResponse = createErrorResponse(context, errorCase);
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_DIFFERENT_USER:
                logger.warn(errorCase);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .error(Errors.DIFFERENT_USER_AUTHENTICATED);
                errorResponse = createErrorResponse(context, errorCase);
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_AUTH_VERIFICATION:
                logger.warnv("WebAuthn API .get() response validation failure. {0}", errorMessage);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .detail(ERR_DETAIL_LABEL, errorMessage)
                        .error(Errors.INVALID_USER_CREDENTIALS);
                errorResponse = createErrorResponse(context, errorCase);
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_USER_NOT_FOUND:
                logger.warn(errorCase);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .error(Errors.USER_NOT_FOUND);
                errorResponse = createErrorResponse(context, errorCase);
                context.challenge(errorResponse);
                break;
            default:
                // NOP
        }
    }

    private Response createErrorResponse(RequiredActionContext context, final String errorCase) {
        LoginFormsProvider provider = context.form().setError(errorCase, "");
        UserModel user = context.getUser();
        if (user != null) {
            WebAuthnAuthenticatorsBean authenticators = new WebAuthnAuthenticatorsBean(context.getSession(),
                    context.getRealm(), user, getCredentialType());
            if (authenticators.getAuthenticators() != null) {
                provider.setAttribute(WebAuthnConstants.ALLOWED_AUTHENTICATORS, authenticators);
            }
        }
        return provider.createFidoErrorPage();
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context,
                    context.getPolicyId(), context.getUser());
        }
        String requestId = context.getRequestId();
        boolean fidoVerified = context.getRealm().hasTrxValueAttribute(requestId, FIDO_VERIFIED);
        boolean fidoVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId, FIDO_VERIFIED_AT);
        WebAuthnAuthenticatorsBean authenticators = new WebAuthnAuthenticatorsBean(context.getSession(),
                context.getRealm(), context.getUser(), getCredentialType());
        if (result && !authenticators.getAuthenticators().isEmpty() && (!fidoVerified || !fidoVerifiedAt)) {
            addAction(context);
        } else {
            context.getUser().removeRequiredAction(context.getModel().getId());
        }
    }

    private void addAction(RequiredActionContext context) {
        RequiredActionProviderModel model = context.getModel();
        if (context.getUser().getRequiredActionsStream().noneMatch(action -> action.equals(model.getId()))) {
            context.getUser().addRequiredAction(model.getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        authenticate(context);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        action(context);
    }

    public static final String PROVIDER_ID = "fido-authenticator-passwordless";


    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"bo"};
    }


    @Override
    public FidoPasswordlessAuthenticator create(KeycloakSession session) {
        this.session = session;
        return new FidoPasswordlessAuthenticator();
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
        return PROVIDER_ID;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public String getDisplayText() {
        return PROVIDER_ID;
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyRpEntityName");
        property.setLabel("relying-party-name");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("relying-party-name-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicySignatureAlgorithms");
        property.setLabel("signature-algorithms");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        List<String> webAuthnPolicySignatureAlgorithmsOptions = new ArrayList<>();
        webAuthnPolicySignatureAlgorithmsOptions.add(NOT_SET);
        webAuthnPolicySignatureAlgorithmsOptions.add("ES384");
        webAuthnPolicySignatureAlgorithmsOptions.add("ES512");
        webAuthnPolicySignatureAlgorithmsOptions.add("RS256");
        webAuthnPolicySignatureAlgorithmsOptions.add("RS384");
        webAuthnPolicySignatureAlgorithmsOptions.add("RS512");
        webAuthnPolicySignatureAlgorithmsOptions.add("RS1");
        property.setOptions(webAuthnPolicySignatureAlgorithmsOptions);
        property.setDefaultValue(NOT_SET);
        property.setHelpText("signature-algorithms-hltxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyRpId");
        property.setLabel("relying-party-id");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("relying-party-id-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyAttestationConveyancePreference");
        property.setLabel("attestation-conveyance-preferences");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        List<String> webAuthnPolicyAttestationConveyancePreferenceOptions = new ArrayList<>();
        webAuthnPolicyAttestationConveyancePreferenceOptions.add(NOT_SET);
        webAuthnPolicyAttestationConveyancePreferenceOptions.add("None");
        webAuthnPolicyAttestationConveyancePreferenceOptions.add("InDirect");
        webAuthnPolicyAttestationConveyancePreferenceOptions.add("Direct");
        property.setOptions(webAuthnPolicyAttestationConveyancePreferenceOptions);
        property.setDefaultValue(NOT_SET);
        property.setHelpText("attestation-conveyance-preferences-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyAuthenticatorAttachment");
        property.setLabel("authenticator-attachment");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        List<String> webAuthnPolicyAuthenticatorAttachmentOptions = new ArrayList<>();
        webAuthnPolicyAuthenticatorAttachmentOptions.add(NOT_SET);
        webAuthnPolicyAuthenticatorAttachmentOptions.add("Platform");
        webAuthnPolicyAuthenticatorAttachmentOptions.add("Cross-Platform");
        property.setOptions(webAuthnPolicyAuthenticatorAttachmentOptions);
        property.setDefaultValue(NOT_SET);
        property.setHelpText("authenticator-attachment-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyRequireResidentKey");
        property.setLabel("require-resident-key");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        List<String> webAuthnPolicyRequireResidentKeyOptions = new ArrayList<>();
        webAuthnPolicyRequireResidentKeyOptions.add(NOT_SET);
        webAuthnPolicyRequireResidentKeyOptions.add("Yes");
        webAuthnPolicyRequireResidentKeyOptions.add("No");
        property.setOptions(webAuthnPolicyRequireResidentKeyOptions);
        property.setDefaultValue(NOT_SET);
        property.setHelpText("require-resident-key-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyUserVerificationRequirement");
        property.setLabel("user-verification-requirement");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        List<String> webAuthnPolicyUserVerificationRequirementOptions = new ArrayList<>();
        webAuthnPolicyUserVerificationRequirementOptions.add(NOT_SET);
        webAuthnPolicyUserVerificationRequirementOptions.add("Required");
        webAuthnPolicyUserVerificationRequirementOptions.add("Preferred");
        webAuthnPolicyUserVerificationRequirementOptions.add("Discouraged");
        property.setOptions(webAuthnPolicyUserVerificationRequirementOptions);
        property.setDefaultValue(NOT_SET);
        property.setHelpText("user-verification-requirement-hlptxt");
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyCreateTimeout");
        property.setLabel("timeout");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("timeout-hlp-txt");
        property.setDefaultValue("0");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyAvoidSameAuthenticatorRegister");
        property.setLabel("avoid-same-authenticator-reg");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("avoid-same-authenticator-reg-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyAcceptableAaguids");
        property.setLabel("acceptable-aaguids");
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        property.setMaxVal(1000);
        property.setHelpText("acceptable-aaguids-hlptxt");
        configProperties.add(property);

    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{"fido_verification"};
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId =
                context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    private void fidoVerificationChallenge(RequiredActionContext context) {
        LoginFormsProvider form = context.form();

        Challenge challenge = new DefaultChallenge();
        String challengeValue = Base64Url.encode(challenge.getValue());
        context.getAuthenticationSession().setAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE, challengeValue);
        form.setAttribute(WebAuthnConstants.CHALLENGE, challengeValue);

        WebAuthnPolicy policy = getWebAuthnPolicy(context);
        String rpId = getRpID(context);
        form.setAttribute(WebAuthnConstants.CREATE_TIMEOUT, policy.getCreateTimeout());

        UserModel user = context.getUser();
        boolean isUserIdentified = false;
        if (user != null) {
            // in 2 Factor Scenario where the user has already been identified
            WebAuthnAuthenticatorsBean authenticators = new WebAuthnAuthenticatorsBean(context.getSession(),
                    context.getRealm(), user, getCredentialType());
            if (authenticators.getAuthenticators().isEmpty()) {
                // require the user to register webauthn authenticator
                GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_VERIFICATION_ERROR, "Webauthn " +
                        "authenticator not registered. Try to register with Webauthn");
                form.setAttribute("errorMessage", "Webauthn authenticator not registered. Try to register with " +
                        "Webauthn");
                return;
            }
            isUserIdentified = true;
            form.setAttribute(WebAuthnConstants.ALLOWED_AUTHENTICATORS, authenticators);
        }
        form.setAttribute(WebAuthnConstants.IS_USER_IDENTIFIED, Boolean.toString(isUserIdentified));
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        boolean isWebSDK = false;
        String registeredUrl = "";
        String cibaAuthorizer =
                context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        CibaAuthConfigModel cibaAuthConfigModel = null;
        if (cibaAuthorizer.equals("azure-b2c-ciba-authorizer") || cibaAuthorizer.equals("ciba-default-managed-auth")) {
            cibaAuthConfigModel =
                    context.getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer,
                            context.getAuthenticationSession().getClient().getClientId(),
                            context.getRealm().getId());
            if (cibaAuthorizer.equals("azure-b2c-ciba-authorizer")) {
                isWebSDK = true;
            } else {
                String notifyWay = cibaAuthConfigModel.getConfig().get(CIBA_NOTIFY);
                isWebSDK = notifyWay.equals(CIBA_SDK);

            }
            final String whitelistHost = cibaAuthConfigModel.getConfig().get("whitelist_host");
            final String validDomain = com.qlikverify.backoffice.GeneralHelper.getValidDomain(whitelistHost);
            if (isWebSDK && validDomain != null) rpId = validDomain;
            registeredUrl = cibaAuthConfigModel.getConfig().get("whitelist_host");
        }

        String userVerificationRequirement = policy.getUserVerificationRequirement();
        form.setAttribute(WebAuthnConstants.RP_ID, rpId);
        form.setAttribute(WebAuthnConstants.USER_VERIFICATION, userVerificationRequirement);
        form.setAttribute(WebAuthnConstants.SHOULD_DISPLAY_AUTHENTICATORS, shouldDisplayAuthenticators(context));
        form.setAttribute("isMobile", cibaAuthProvider.isMobile());
        form.setAttribute("isDesktop", cibaAuthProvider.isDesktop());
        form.setAttribute("openInNewTab", cibaAuthConfigModel != null && Boolean.parseBoolean(cibaAuthConfigModel.getConfig().getOrDefault(OPEN_IN_NEW_WINDOW, "false")));
        form.setAttribute("isWebSDK", isWebSDK);
        form.setAttribute("webRegisteredOrigin", registeredUrl);

        context.challenge(form.createLoginFido());
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(FIDO_VERIFIED, ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(FIDO_VERIFIED_AT, ValidationTypes.DATE_AND_TIME.getValue());

        return defaultClaims;
    }

    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public boolean isClaimsCustomizable() {
        return false;
    }
}
