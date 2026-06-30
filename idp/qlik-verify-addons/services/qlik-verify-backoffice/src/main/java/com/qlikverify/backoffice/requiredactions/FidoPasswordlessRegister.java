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
import com.qlikverify.backoffice.creds.biometric.BiometricCredentialProvider;
import com.qlikverify.backoffice.creds.biometric.BiometricCredentialProviderFactory;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.webauthn4j.WebAuthnRegistrationManager;
import com.webauthn4j.anchor.KeyStoreTrustAnchorsProvider;
import com.webauthn4j.anchor.TrustAnchorsResolverImpl;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.exception.WebAuthnException;
import com.webauthn4j.validator.attestation.statement.androidkey.AndroidKeyAttestationStatementValidator;
import com.webauthn4j.validator.attestation.statement.androidsafetynet.AndroidSafetyNetAttestationStatementValidator;
import com.webauthn4j.validator.attestation.statement.none.NoneAttestationStatementValidator;
import com.webauthn4j.validator.attestation.statement.packed.PackedAttestationStatementValidator;
import com.webauthn4j.validator.attestation.statement.tpm.TPMAttestationStatementValidator;
import com.webauthn4j.validator.attestation.statement.u2f.FIDOU2FAttestationStatementValidator;
import com.webauthn4j.validator.attestation.trustworthiness.certpath.CertPathTrustworthinessValidator;
import com.webauthn4j.validator.attestation.trustworthiness.certpath.NullCertPathTrustworthinessValidator;
import com.webauthn4j.validator.attestation.trustworthiness.certpath.TrustAnchorCertPathTrustworthinessValidator;
import com.webauthn4j.validator.attestation.trustworthiness.self.DefaultSelfAttestationTrustworthinessValidator;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.*;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.WebAuthnCredentialModelInput;
import org.keycloak.credential.WebAuthnCredentialProvider;
import org.keycloak.credential.WebAuthnPasswordlessCredentialProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.model.WebAuthnAuthenticatorsBean;
import org.keycloak.models.*;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.WebAuthnMobileSecretData;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.ServicesUtils;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.core.MediaType;
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
 * Required action for register WebAuthn passwordless credential for the user. This class is temporary and will be
 * likely
 * removed in the future during future improvements in authentication SPI
 */
public class FidoPasswordlessRegister implements RequiredActionProvider, CredentialRegistrator, RequiredActionFactory
        , DisplayTypeRequiredActionFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(FidoPasswordlessRegister.class);
    private KeycloakSession session;
    private CertPathTrustworthinessValidator certPathtrustValidator;

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserModel userModel = context.getUser();
        // Use standard UTF-8 charset to get bytes from string.
        // Otherwise the platform's default charset is used and it might cause problems later when
        // decoded on different system.
        String userId = userModel.getId();
        String username;
        if (context.getRealm().getMode().equalsIgnoreCase("bo")) {
            username = userModel.getEmail();
            if (username == null)
                username = userModel.getMobileNumber();
            if (username == null)
                username = userModel.getUsername();
        } else {
            username = userModel.getEmail();
            if (username == null)
                username = userModel.getMobileNumber();

            if (username == null)
                username = userModel.getId();
        }
        Challenge challenge = new DefaultChallenge();
        String challengeValue = Base64Url.encode(challenge.getValue());
        context.getAuthenticationSession().setAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE, challengeValue);

        // construct parameters for calling WebAuthn API navigator.credential.create()

        // mandatory
        WebAuthnPolicy policy = getWebAuthnPolicy(context);
        List<String> signatureAlgorithmsList = policy.getSignatureAlgorithm();
        String signatureAlgorithms = stringifySignatureAlgorithms(signatureAlgorithmsList);
        String rpEntityName = policy.getRpEntityName();

        // optional
        String rpId = policy.getRpId();
        if (rpId == null || rpId.isEmpty()) rpId = context.getUriInfo().getBaseUri().getHost();
        String attestationConveyancePreference = policy.getAttestationConveyancePreference();
        String authenticatorAttachment = policy.getAuthenticatorAttachment();
        String requireResidentKey = policy.getRequireResidentKey();
        String userVerificationRequirement = policy.getUserVerificationRequirement();
        long createTimeout = policy.getCreateTimeout();
        boolean avoidSameAuthenticatorRegister = policy.isAvoidSameAuthenticatorRegister();

        String excludeCredentialIds = "";
        if (avoidSameAuthenticatorRegister) {
            excludeCredentialIds =
                    session.userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), userModel,
                                    getCredentialType())
                            .filter(x -> x.isEnabled())
                            .map(credentialModel -> {
                                WebAuthnCredentialModel credModel =
                                        WebAuthnCredentialModel.createFromCredentialModel(credentialModel);
                                return Base64Url.encodeBase64ToBase64Url(credModel.getWebAuthnCredentialData().getCredentialId());
                            }).collect(Collectors.joining(","));
        }

        String isSetRetry = null;

        if (isFormDataRequest(context.getHttpRequest())) {
            isSetRetry = context.getHttpRequest().getDecodedFormParameters().getFirst(WebAuthnConstants.IS_SET_RETRY);
        }
        boolean isWebSDK = false;
        String registeredUrls = "";
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        String cibaAuthorizer =
                session.getContext().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        CibaAuthConfigModel cibaAuthConfigModel = null;
        if (cibaAuthorizer.equals("azure-b2c-ciba-authorizer") || cibaAuthorizer.equals("ciba-default-managed-auth")) {
            cibaAuthConfigModel =
                    session.getContext().getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer,
                            session.getContext().getClient().getClientId(), session.getContext().getRealm().getId());
            if (cibaAuthorizer.equals("azure-b2c-ciba-authorizer")) {
                isWebSDK = true;
            } else {
                String notifyWay = cibaAuthConfigModel.getConfig().get(CIBA_NOTIFY);
                isWebSDK = notifyWay.equals(CIBA_SDK);
            }
            final String whitelistHost = cibaAuthConfigModel.getConfig().get("whitelist_host");
            registeredUrls = whitelistHost;
        }

        Response form = context.form()
                .setAttribute(WebAuthnConstants.CHALLENGE, challengeValue)
                .setAttribute(WebAuthnConstants.USER_ID, userId)
                .setAttribute(WebAuthnConstants.USER_NAME, username)
                .setAttribute(WebAuthnConstants.RP_ENTITY_NAME, rpEntityName)
                .setAttribute(WebAuthnConstants.SIGNATURE_ALGORITHMS, signatureAlgorithms)
                .setAttribute(WebAuthnConstants.RP_ID, rpId)
                .setAttribute(WebAuthnConstants.ATTESTATION_CONVEYANCE_PREFERENCE, attestationConveyancePreference)
                .setAttribute(WebAuthnConstants.AUTHENTICATOR_ATTACHMENT, authenticatorAttachment)
                .setAttribute(WebAuthnConstants.REQUIRE_RESIDENT_KEY, requireResidentKey)
                .setAttribute(WebAuthnConstants.USER_VERIFICATION_REQUIREMENT, userVerificationRequirement)
                .setAttribute(WebAuthnConstants.CREATE_TIMEOUT, createTimeout)
                .setAttribute(WebAuthnConstants.EXCLUDE_CREDENTIAL_IDS, excludeCredentialIds)
                .setAttribute(WebAuthnConstants.IS_SET_RETRY, isSetRetry)
                .setAttribute("isDesktop", cibaAuthProvider.isDesktop())
                .setAttribute("isMobile", cibaAuthProvider.isMobile())
                .setAttribute("isWebSDK", isWebSDK)
                .setAttribute("openInNewTab", cibaAuthConfigModel != null && Boolean.parseBoolean(cibaAuthConfigModel.getConfig().getOrDefault(OPEN_IN_NEW_WINDOW, "false")))
                .setAttribute("webRegisteredOrigin", registeredUrls)

                .createForm("fido-register.ftl");
        context.challenge(form);
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

    public BiometricCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (BiometricCredentialProvider) session.getProvider(CredentialProvider.class,
                BiometricCredentialProviderFactory.PROVIDER_ID);
    }

    protected String getCredentialType() {
        return WebAuthnCredentialModel.TYPE_PASSWORDLESS;
    }

    protected String getCredentialProviderId() {
        return WebAuthnPasswordlessCredentialProviderFactory.PROVIDER_ID;
    }

    @Override
    public void processAction(RequiredActionContext context) {
        if (context.getUser() == null) {
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_REGISTER_ERROR,
                    ErrorMappings.USER_NOT_ONBOARDED);
            showView(context, "user_not_onboarded", BACKOFFICE_ERROR_FTL, null);
            return;
        }

        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        if (params.containsKey("cancel") && Boolean.parseBoolean(params.getFirst("cancel"))) {
            handleCancel(context, EventType.EZ_FIDO_PASSWORDLESS_REGISTER_ERROR);
            return;
        }

        String isSetRetry = params.getFirst(WebAuthnConstants.IS_SET_RETRY);
        if (isSetRetry != null && !isSetRetry.isEmpty()) {
            requiredActionChallenge(context);
            return;
        }

        context.getEvent().detail(Details.CREDENTIAL_TYPE, getCredentialType());

        // receive error from navigator.credentials.create()
        String errorMsgFromWebAuthnApi = params.getFirst(WebAuthnConstants.ERROR);
        if (errorMsgFromWebAuthnApi != null && !errorMsgFromWebAuthnApi.isEmpty()) {
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_REGISTER_ERROR, errorMsgFromWebAuthnApi);
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTER_VERIFICATION, errorMsgFromWebAuthnApi);
            return;
        }

        WebAuthnPolicy policy = getWebAuthnPolicy(context);
        String rpId = policy.getRpId();
        if (rpId == null || rpId.isEmpty()) rpId = context.getUriInfo().getBaseUri().getHost();
        String label = params.getFirst(WebAuthnConstants.AUTHENTICATOR_LABEL);
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(params.getFirst(WebAuthnConstants.CLIENT_DATA_JSON));
        byte[] attestationObject = Base64.getUrlDecoder().decode(params.getFirst(WebAuthnConstants.ATTESTATION_OBJECT));

        String publicKeyCredentialId = params.getFirst(WebAuthnConstants.PUBLIC_KEY_CREDENTIAL_ID);
        Set<Origin> origin = null;
        if (params.containsKey(PLATFORM) && params.getFirst(PLATFORM).equalsIgnoreCase("android")) {
            origin = com.qlikverify.backoffice.GeneralHelper.getOriginForAndroid(context.getRealm());
        } else {
            origin = new HashSet<>();
            String registeredUrl = "";
            String cibaAuthorizer =
                    session.getContext().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
            if (cibaAuthorizer.equals("azure-b2c-ciba-authorizer") || cibaAuthorizer.equals("ciba-default-managed" +
                    "-auth")) {
                CibaAuthConfigModel cibaAuthConfigModel =
                        session.getContext().getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer,
                                session.getContext().getClient().getClientId(),
                                session.getContext().getRealm().getId());
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

        if (params.containsKey(PLATFORM) && params.getFirst(PLATFORM).equalsIgnoreCase("ios")) {
            logger.info("context.getUriInfo().getBaseUri(): ," + context.getUriInfo().getBaseUri());
            logger.info("UriUtils.getOrigin(context.getUriInfo().getBaseUri()): ," + UriUtils.getOrigin(context.getUriInfo().getBaseUri()));
        }
        Challenge challenge =
                new DefaultChallenge(context.getAuthenticationSession().getAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE));
        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, null);
        // check User Verification by considering a malicious user might modify the result of calling WebAuthn API
        boolean isUserVerificationRequired =
                policy.getUserVerificationRequirement().equals(WebAuthnConstants.OPTION_REQUIRED);

        final String transportsParam = params.getFirst(WebAuthnConstants.TRANSPORTS);

        RegistrationRequest registrationRequest;

        if (StringUtil.isNotBlank(transportsParam)) {
            final Set<String> transports = new HashSet<>(Arrays.asList(transportsParam.split(",")));
            registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON, transports);
        } else {
            registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON);
        }

        RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty,
                isUserVerificationRequired);

        webAuthnRegistration(context, registrationRequest, registrationParameters, policy, params,
                publicKeyCredentialId, label);
    }

    private void webAuthnRegistration(RequiredActionContext context, RegistrationRequest registrationRequest,
                                      RegistrationParameters registrationParameters, WebAuthnPolicy policy,
                                      MultivaluedMap<String, String> params, String publicKeyCredentialId,
                                      String label) {
        WebAuthnRegistrationManager webAuthnRegistrationManager = createWebAuthnRegistrationManager();
        try {
            // parse
            RegistrationData registrationData = webAuthnRegistrationManager.parse(registrationRequest);
            // validate
            webAuthnRegistrationManager.validate(registrationData, registrationParameters);

            showInfoAfterWebAuthnApiCreate(registrationData);

            checkAcceptedAuthenticator(registrationData, policy);

            WebAuthnCredentialModelInput credential = new WebAuthnCredentialModelInput(getCredentialType());

            credential.setAttestedCredentialData(registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData());
            credential.setCount(registrationData.getAttestationObject().getAuthenticatorData().getSignCount());
            credential.setAttestationStatementFormat(registrationData.getAttestationObject().getFormat());
            credential.setTransports(registrationData.getTransports());

            // Save new webAuthn credential
            WebAuthnCredentialProvider webAuthnCredProvider =
                    (WebAuthnCredentialProvider) this.session.getProvider(CredentialProvider.class,
                            getCredentialProviderId());
            WebAuthnCredentialModel newCredentialModel =
                    webAuthnCredProvider.getCredentialModelFromCredentialInput(credential, label);
            String device = params.getFirst(DEVICE);
            if (device != null && !device.isEmpty()) {
                HashMap<String, String> deviceDetails = null;
                deviceDetails = JsonSerialization.readValue(device, new TypeReference<HashMap<String, String>>() {
                });
                String deviceDetailsString = JsonSerialization.writeValueAsString(deviceDetails);

                if (params.containsKey(DEVICE) && params.getFirst(DEVICE) != null) {
                    newCredentialModel.setSecretData(JsonSerialization.readValue(deviceDetailsString,
                            WebAuthnMobileSecretData.class).toString());
                }

            }
            webAuthnCredProvider.createCredential(context.getRealm(), context.getUser(), newCredentialModel);

            String aaguid = newCredentialModel.getWebAuthnCredentialData().getAaguid();
            logger.debugv("WebAuthn credential registration success for user {0}. credentialType = {1}, " +
                            "publicKeyCredentialId = {2}, publicKeyCredentialLabel = {3}, publicKeyCredentialAAGUID =" +
                            " {4}",
                    context.getUser().getUsername(), getCredentialType(), publicKeyCredentialId, label, aaguid);
            webAuthnCredProvider.dumpCredentialModel(newCredentialModel, credential);

            context.getEvent()
                    .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, publicKeyCredentialId)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, label)
                    .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, aaguid);
            String requestId = context.getRequestId();
            context.getRealm().addAttribute(requestId, FIDO_REGISTERED, "true");
            context.getRealm().addAttribute(requestId, FIDO_REGISTERED_AT, String.valueOf(Time.currentTime()));
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_REGISTER, "");
            context.success();
            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), "fido_register");
        } catch (WebAuthnException wae) {
            if (logger.isDebugEnabled()) logger.debug(wae.getMessage(), wae);
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_REGISTER_ERROR, wae.getMessage());
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTRATION, wae.getMessage());
        } catch (Exception e) {
            if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
            GeneralHelper.auditEvent(context, EventType.EZ_FIDO_PASSWORDLESS_REGISTER_ERROR, e.getMessage());
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTRATION, e.getMessage());
        }
    }

    private WebAuthnRegistrationManager createWebAuthnRegistrationManager() {
        return new WebAuthnRegistrationManager(
                Arrays.asList(
                        new NoneAttestationStatementValidator(),
                        new PackedAttestationStatementValidator(),
                        new TPMAttestationStatementValidator(),
                        new AndroidKeyAttestationStatementValidator(),
                        new AndroidSafetyNetAttestationStatementValidator(),
                        new FIDOU2FAttestationStatementValidator()
                ), this.certPathtrustValidator,
                new DefaultSelfAttestationTrustworthinessValidator(),
                Collections.emptyList(), // Custom Registration Validator is not supported
                new ObjectConverter()
        );
    }

    private String stringifySignatureAlgorithms(List<String> signatureAlgorithmsList) {
        if (signatureAlgorithmsList == null || signatureAlgorithmsList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();

        // here we add in all the supported algorithms from our application, so the supported versions from the
        // authenticator (ubikey, fingerprint, etc) wont stop the flow
        sb.append(COSEAlgorithmIdentifier.ES256.getValue()).append(",");
        sb.append(COSEAlgorithmIdentifier.RS256.getValue()).append(",");
        sb.append(COSEAlgorithmIdentifier.ES384.getValue()).append(",");
        sb.append(COSEAlgorithmIdentifier.RS384.getValue()).append(",");
        sb.append(COSEAlgorithmIdentifier.ES512.getValue()).append(",");
        sb.append(COSEAlgorithmIdentifier.RS512.getValue()).append(",");
        sb.append(COSEAlgorithmIdentifier.RS1.getValue()).append(",");

        // default implementation -> commented because there is no fallback to handle different basic signature
        // algorithm of ubikeys
//        for (String s : signatureAlgorithmsList) {
//            switch (s) {
//                case Algorithm.ES256 :
//                    sb.append(COSEAlgorithmIdentifier.ES256.getValue()).append(",");
//                    break;
//                case Algorithm.RS256 :
//                    sb.append(COSEAlgorithmIdentifier.RS256.getValue()).append(",");
//                    break;
//                case Algorithm.ES384 :
//                    sb.append(COSEAlgorithmIdentifier.ES384.getValue()).append(",");
//                    break;
//                case Algorithm.RS384 :
//                    sb.append(COSEAlgorithmIdentifier.RS384.getValue()).append(",");
//                    break;
//                case Algorithm.ES512 :
//                    sb.append(COSEAlgorithmIdentifier.ES512.getValue()).append(",");
//                    break;
//                case Algorithm.RS512 :
//                    sb.append(COSEAlgorithmIdentifier.RS512.getValue()).append(",");
//                    break;
//                case "RS1" :
//                    sb.append(COSEAlgorithmIdentifier.RS1.getValue()).append(",");
//                    break;
//                default:
//                    // NOP
//            }
//        }

        if (sb.lastIndexOf(",") > -1) sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }

    private void showInfoAfterWebAuthnApiCreate(RegistrationData response) {
        AttestedCredentialData attestedCredentialData =
                response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
        AttestationStatement attestationStatement = response.getAttestationObject().getAttestationStatement();
        Set<AuthenticatorTransport> transports = response.getTransports();

        logger.debugv("createad key's algorithm = {0}",
                String.valueOf(attestedCredentialData.getCOSEKey().getAlgorithm().getValue()));
        logger.debugv("aaguid = {0}", attestedCredentialData.getAaguid().toString());
        logger.debugv("attestation format = {0}", attestationStatement.getFormat());

        if (CollectionUtil.isNotEmpty(transports)) {
            logger.debugv("transports = [{0}]", transports.stream()
                    .map(AuthenticatorTransport::getValue)
                    .collect(Collectors.joining(",")));
        }
    }

    private void checkAcceptedAuthenticator(RegistrationData response, WebAuthnPolicy policy) throws Exception {
        String aaguid =
                response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getAaguid().toString();
        List<String> acceptableAaguids = policy.getAcceptableAaguids();
        boolean isAcceptedAuthenticator = false;
        if (acceptableAaguids != null && !acceptableAaguids.isEmpty()) {
            for (String acceptableAaguid : acceptableAaguids) {
                if (aaguid.equals(acceptableAaguid)) {
                    isAcceptedAuthenticator = true;
                    break;
                }
            }
        } else {
            // no accepted authenticators means accepting any kind of authenticator
            isAcceptedAuthenticator = true;
        }
        if (!isAcceptedAuthenticator) {
            throw new WebAuthnException("not acceptable aaguid = " + aaguid);
        }
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context,
                    context.getPolicyId(), context.getUser());
        }

        WebAuthnAuthenticatorsBean authenticators = new WebAuthnAuthenticatorsBean(context.getSession(),
                context.getRealm(), context.getUser(), getCredentialType());
        if (result && authenticators.getAuthenticators().isEmpty()) {
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

    private static final String ERR_LABEL = "web_authn_registration_error";
    private static final String ERR_DETAIL_LABEL = "web_authn_registration_error_detail";

    private void setErrorResponse(RequiredActionContext context, final String errorCase, final String errorMessage) {
        Response errorResponse = null;
        switch (errorCase) {
            case WEBAUTHN_ERROR_REGISTER_VERIFICATION:
                logger.warnv("WebAuthn API .create() response validation failure. {0}", errorMessage);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .detail(ERR_DETAIL_LABEL, errorMessage)
                        .error(Errors.INVALID_USER_CREDENTIALS);
                errorResponse = context.form()
                        .setError(errorCase, errorMessage)
                        .setAttribute(WEB_AUTHN_TITLE_ATTR, WEBAUTHN_REGISTER_TITLE)
                        .createFidoErrorPage();
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_REGISTRATION:
                logger.warn(errorCase);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .detail(ERR_DETAIL_LABEL, errorMessage)
                        .error(Errors.INVALID_REGISTRATION);
                errorResponse = context.form()
                        .setError(errorCase, errorMessage)
                        .setAttribute(WEB_AUTHN_TITLE_ATTR, WEBAUTHN_REGISTER_TITLE)
                        .createFidoErrorPage();
                context.challenge(errorResponse);
                break;
            default:
                // NOP
        }
    }

    private boolean isFormDataRequest(HttpRequest request) {
        MediaType mediaType = request.getHttpHeaders().getMediaType();
        return mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }

    public static final String PROVIDER_ID = "fido-register";

    @Override
    public RequiredActionProvider create(KeycloakSession session) {

        this.session = session;
        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
            this.certPathtrustValidator = new NullCertPathTrustworthinessValidator();
        } else {
            KeyStoreTrustAnchorsProvider trustAnchorsProvider = new KeyStoreTrustAnchorsProvider();
            trustAnchorsProvider.setKeyStore(truststoreProvider.getTruststore());
            TrustAnchorsResolverImpl resolverImpl = new TrustAnchorsResolverImpl(trustAnchorsProvider);
            TrustAnchorCertPathTrustworthinessValidator trustValidator =
                    new TrustAnchorCertPathTrustworthinessValidator(resolverImpl);
            this.certPathtrustValidator = trustValidator;
        }
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
        return PROVIDER_ID;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return create(session);
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        // TODO : write console typed provider?
        return null;
    }

    @Override
    public String getDisplayText() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN);
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

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
        List<String> webAuthnPolicySignatureAlgorithms = new ArrayList<>();
        webAuthnPolicySignatureAlgorithms.add(NOT_SET);
        webAuthnPolicySignatureAlgorithms.add("ES384");
        webAuthnPolicySignatureAlgorithms.add("ES512");
        webAuthnPolicySignatureAlgorithms.add("RS256");
        webAuthnPolicySignatureAlgorithms.add("RS384");
        webAuthnPolicySignatureAlgorithms.add("RS512");
        webAuthnPolicySignatureAlgorithms.add("RS1");
        property.setOptions(webAuthnPolicySignatureAlgorithms);
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
        List<String> webAuthnPolicyAuthenticatorAttachment = new ArrayList<>();
        webAuthnPolicyAuthenticatorAttachment.add(NOT_SET);
        webAuthnPolicyAuthenticatorAttachment.add("Platform");
        webAuthnPolicyAuthenticatorAttachment.add("Cross-Platform");
        property.setOptions(webAuthnPolicyAuthenticatorAttachment);
        property.setDefaultValue(NOT_SET);
        property.setHelpText("authenticator-attachment-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("webAuthnPolicyRequireResidentKey");
        property.setLabel("require-resident-key");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        List<String> webAuthnPolicyRequireResidentKey = new ArrayList<>();
        webAuthnPolicyRequireResidentKey.add(NOT_SET);
        webAuthnPolicyRequireResidentKey.add("Yes");
        webAuthnPolicyRequireResidentKey.add("No");
        property.setOptions(webAuthnPolicyRequireResidentKey);
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
        property.setHelpText("acceptable-aaguids-hlptxt");
        property.setMaxVal(1000);
        configProperties.add(property);

    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"bo"};
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{"fido_register"};
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId =
                context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession) {
        return getCredentialType();
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(FIDO_REGISTERED, ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(FIDO_REGISTERED_AT, ValidationTypes.DATE_AND_TIME.getValue());

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
