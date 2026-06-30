package org.keycloak.mfa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.WebAuthnRegistrationManager;
import com.webauthn4j.anchor.KeyStoreTrustAnchorsProvider;
import com.webauthn4j.anchor.TrustAnchorsResolverImpl;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.*;
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
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.UriUtils;
import org.keycloak.credential.*;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.WebAuthnAuthenticatorsBean;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.utils.GeneralHelper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.services.messages.Messages.*;
import static org.keycloak.utils.ServicesUtils.toWebAuthPolicy;

public class WebAuthnMFAProvider extends MFARecovery implements MFAProviderFactory, MFAProvider {

    private static final Logger logger = Logger.getLogger(WebAuthnMFAProvider.class);
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);
    private static final String WEB_AUTHN_TITLE_ATTR = "webAuthnTitle";
    private static final String WEBAUTHN_MFA_HELP_TEXT="webAuthnMfaHelpText";

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WebAuthn:PreLogin");
        LoginFormsProvider form = context.form();

        Challenge challenge = new DefaultChallenge();
        String challengeValue = Base64Url.encode(challenge.getValue());
        context.getAuthenticationSession().setAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE, challengeValue);
        form.setAttribute(WebAuthnConstants.CHALLENGE, challengeValue);

        WebAuthnPolicy policy = getWebAuthnPolicy(configModel,context);
        String rpId = getRpID(configModel,context);
        form.setAttribute(WebAuthnConstants.RP_ID, rpId);

        UserModel user = context.getUser();
        boolean isUserIdentified = false;
        if (user != null) {
            // in 2 Factor Scenario where the user has already been identified
            WebAuthnAuthenticatorsBean authenticators = new WebAuthnAuthenticatorsBean(context.getSession(), context.getRealm(), user, getCredentialType());
            if (authenticators.getAuthenticators().isEmpty()) {
                // require the user to register webauthn authenticator
                return;
            }
            isUserIdentified = true;
            form.setAttribute(WebAuthnConstants.ALLOWED_AUTHENTICATORS, authenticators);
        } else {
            // in ID-less & Password-less Scenario
            // NOP
        }
        form.setAttribute(WebAuthnConstants.IS_USER_IDENTIFIED, Boolean.toString(isUserIdentified));

        form.setAttribute("showTroubleShoot", isMFARecoveryEnabled(context, configModel));

        // read options from policy
        String userVerificationRequirement = policy.getUserVerificationRequirement();
        form.setAttribute(WebAuthnConstants.USER_VERIFICATION, userVerificationRequirement);
        form.setAttribute("pageType", "mfa");
        context.challenge(form.createLoginWebAuthn());
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WebAuthn:PostLogin");
        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        context.getEvent().detail(Details.CREDENTIAL_TYPE, getCredentialType());

        // receive error from navigator.credentials.get()
        String errorMsgFromWebAuthnApi = params.getFirst(WebAuthnConstants.ERROR);
        if (errorMsgFromWebAuthnApi != null && !errorMsgFromWebAuthnApi.isEmpty()) {
            setErrorResponse(context, WEBAUTHN_ERROR_API_GET, errorMsgFromWebAuthnApi,isMFARecoveryEnabled(context, configModel));
            return false;
        }

        String baseUrl = UriUtils.getOrigin(context.getUriInfo().getBaseUri());
        String rpId = getRpID(configModel,context);

        Origin origin = new Origin(baseUrl);
        Challenge challenge = new DefaultChallenge(context.getAuthenticationSession().getAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE));
        ServerProperty server = new ServerProperty(origin, rpId, challenge, null);

        byte[] credentialId = Base64Url.decode(params.getFirst(WebAuthnConstants.CREDENTIAL_ID));
        byte[] clientDataJSON = Base64Url.decode(params.getFirst(WebAuthnConstants.CLIENT_DATA_JSON));
        byte[] authenticatorData = Base64Url.decode(params.getFirst(WebAuthnConstants.AUTHENTICATOR_DATA));
        byte[] signature = Base64Url.decode(params.getFirst(WebAuthnConstants.SIGNATURE));

        final String userHandle = params.getFirst(WebAuthnConstants.USER_HANDLE);
        final String userId;
        // existing User Handle means that the authenticator used Resident Key supported public key credential
        if (userHandle == null || userHandle.isEmpty()) {
            // Resident Key not supported public key credential was used
            // so rely on the user that has already been authenticated
            userId = context.getUser().getId();
        } else {
            // decode using the same charset as it has been encoded (see: WebAuthnRegister.java)
            userId = new String(Base64Url.decode(userHandle), StandardCharsets.UTF_8);
            if (context.getUser() != null) {
                // Resident Key supported public key credential was used,
                // so need to confirm whether the already authenticated user is equals to one authenticated by the webauthn authenticator
                String firstAuthenticatedUserId = context.getUser().getId();
                if (firstAuthenticatedUserId != null && !firstAuthenticatedUserId.equals(userId)) {
                    context.getEvent()
                            .detail("first_authenticated_user_id", firstAuthenticatedUserId)
                            .detail("web_authn_authenticator_authenticated_user_id", userId);
                    setErrorResponse(context, WEBAUTHN_ERROR_DIFFERENT_USER, null,isMFARecoveryEnabled(context, configModel));
                    return false;
                }
            } else {
                // Resident Key supported public key credential was used,
                // and the user has not yet been identified
                // so rely on the user authenticated by the webauthn authenticator
                // NOP
            }
        }

        boolean isUVFlagChecked = false;
        String userVerificationRequirement = getWebAuthnPolicy(configModel,context).getUserVerificationRequirement();
        if (WebAuthnConstants.OPTION_REQUIRED.equals(userVerificationRequirement)) isUVFlagChecked = true;

        UserModel user = context.getSession().users().getUserById(userId, context.getRealm());

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                credentialId,
                authenticatorData,
                clientDataJSON,
                signature
        );

        WebAuthnCredentialModelInput.KeycloakWebAuthnAuthenticationParameters authenticationParameters = new WebAuthnCredentialModelInput.KeycloakWebAuthnAuthenticationParameters(
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
            setErrorResponse(context, WEBAUTHN_ERROR_AUTH_VERIFICATION, wae.getMessage(),isMFARecoveryEnabled(context, configModel));
            return false;
        }
        String encodedCredentialID = Base64Url.encode(credentialId);
        if (result) {
            String isUVChecked = Boolean.toString(isUVFlagChecked);
            logger.debugv("WebAuthn Authentication successed. isUserVerificationChecked = {0}, PublicKeyCredentialID = {1}", isUVChecked, encodedCredentialID);
            //context.setUser(user);
            context.getEvent()
                    .detail("web_authn_authenticator_user_verification_checked", isUVChecked)
                    .detail("public_key_credential_id", encodedCredentialID);
            context.success();
            return true;
        } else {
            context.getEvent()
                    .detail("web_authn_authenticated_user_id", userId)
                    .detail("public_key_credential_id", encodedCredentialID);
            setErrorResponse(context, WEBAUTHN_ERROR_USER_NOT_FOUND, null,isMFARecoveryEnabled(context, configModel));
            return false;
        }
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WebAuthn:PostRegistration");
        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        String isSetRetry = params.getFirst(WebAuthnConstants.IS_SET_RETRY);
        if (isSetRetry != null && !isSetRetry.isEmpty()) {
            onPreRegistration(configModel, context, false);
            return false;
        }

        context.getEvent().detail(Details.CREDENTIAL_TYPE, getCredentialType());

        // receive error from navigator.credentials.create()
        String errorMsgFromWebAuthnApi = params.getFirst(WebAuthnConstants.ERROR);
        if (errorMsgFromWebAuthnApi != null && !errorMsgFromWebAuthnApi.isEmpty()) {
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTER_VERIFICATION, errorMsgFromWebAuthnApi,false);
            return false;
        }

        WebAuthnPolicy policy = getWebAuthnPolicy(configModel,context);
        String rpId = policy.getRpId();
        if (rpId == null || rpId.isEmpty()) rpId = context.getUriInfo().getBaseUri().getHost();
        String label = params.getFirst(WebAuthnConstants.AUTHENTICATOR_LABEL);
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(params.getFirst(WebAuthnConstants.CLIENT_DATA_JSON));
        byte[] attestationObject = Base64.getUrlDecoder().decode(params.getFirst(WebAuthnConstants.ATTESTATION_OBJECT));

        String publicKeyCredentialId = params.getFirst(WebAuthnConstants.PUBLIC_KEY_CREDENTIAL_ID);

        Origin origin = new Origin(UriUtils.getOrigin(context.getUriInfo().getBaseUri()));
        Challenge challenge = new DefaultChallenge(context.getAuthenticationSession().getAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE));
        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, null);
        // check User Verification by considering a malicious user might modify the result of calling WebAuthn API
        boolean isUserVerificationRequired = policy.getUserVerificationRequirement().equals(WebAuthnConstants.OPTION_REQUIRED);

        RegistrationRequest registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON);
        RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, isUserVerificationRequired);

        WebAuthnRegistrationManager webAuthnRegistrationManager = createWebAuthnRegistrationManager(context.getSession());
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

            // Save new webAuthn credential
            WebAuthnCredentialProvider webAuthnCredProvider = (WebAuthnCredentialProvider) context.getSession().getProvider(CredentialProvider.class, WebAuthnCredentialProviderFactory.PROVIDER_ID);
            WebAuthnCredentialModel newCredentialModel = webAuthnCredProvider.getCredentialModelFromCredentialInput(credential, label);
            newCredentialModel.setProviderId(getId());
            newCredentialModel.setClientId(context.getSession().getContext().getClient().getClientId());
            webAuthnCredProvider.createCredential(context.getRealm(), context.getUser(), newCredentialModel);

            String aaguid = newCredentialModel.getWebAuthnCredentialData().getAaguid();
            logger.debugv("WebAuthn credential registration success for user {0}. credentialType = {1}, publicKeyCredentialId = {2}, publicKeyCredentialLabel = {3}, publicKeyCredentialAAGUID = {4}",
                    context.getUser().getUsername(), getCredentialType(), publicKeyCredentialId, label, aaguid);
            webAuthnCredProvider.dumpCredentialModel(newCredentialModel, credential);

            context.getEvent()
                    .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, publicKeyCredentialId)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, label)
                    .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, aaguid);

            /*OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), "test");
            otpCredentialModel.setProviderId(getId());
            OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
            otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), otpCredentialModel);*/

            return true;
        } catch (WebAuthnException wae) {
            if (logger.isDebugEnabled()) logger.debug(wae.getMessage(), wae);
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTRATION, wae.getMessage(),false);
            return false;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
            setErrorResponse(context, WEBAUTHN_ERROR_REGISTRATION, e.getMessage(),false);
            return false;
        }
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:WebAuthn:PreRegistration");
        KeycloakSession session = context.getSession();
        UserModel userModel = context.getUser();
        // Use standard UTF-8 charset to get bytes from string.
        // Otherwise the platform's default charset is used and it might cause problems later when
        // decoded on different system.
        String userId = Base64Url.encode(userModel.getId().getBytes(StandardCharsets.UTF_8));
        String username = userModel.getUsername();
        Challenge challenge = new DefaultChallenge();
        String challengeValue = Base64Url.encode(challenge.getValue());
        context.getAuthenticationSession().setAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE, challengeValue);

        // construct parameters for calling WebAuthn API navigator.credential.create()

        // mandatory
        WebAuthnPolicy policy = getWebAuthnPolicy(configModel,context);
        if(policy == null){
            Response challengeResponse = context.form()
                    .setAttribute("errorMessage", "webAuthnPolicyRequired")
                    .setAttribute("pageType", "mfa")
                    .createForm("error.ftl");
            context.challenge(challengeResponse);
            return;
        }
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
            excludeCredentialIds = session.userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), userModel, getCredentialType())
                    .map(credentialModel -> {
                        WebAuthnCredentialModel credModel = WebAuthnCredentialModel.createFromCredentialModel(credentialModel);
                        return Base64Url.encodeBase64ToBase64Url(credModel.getWebAuthnCredentialData().getCredentialId());
                    }).collect(Collectors.joining(","));
        }

        String isSetRetry = context.getHttpRequest().getDecodedFormParameters().getFirst(WebAuthnConstants.IS_SET_RETRY);

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
                .setAttribute("mfaProviderId",getId())
                .setAttribute("pageType","mfa")
                .createForm("webauthn-register.ftl");
        context.challenge(form);
    }

    protected String getCredentialType() {
        return WebAuthnCredentialModel.TYPE_TWOFACTOR;
    }

    private String stringifySignatureAlgorithms(List<String> signatureAlgorithmsList) {
        if (signatureAlgorithmsList == null || signatureAlgorithmsList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : signatureAlgorithmsList) {
            switch (s) {
                case Algorithm.ES256:
                    sb.append(COSEAlgorithmIdentifier.ES256.getValue()).append(",");
                    break;
                case Algorithm.RS256:
                    sb.append(COSEAlgorithmIdentifier.RS256.getValue()).append(",");
                    break;
                case Algorithm.ES384:
                    sb.append(COSEAlgorithmIdentifier.ES384.getValue()).append(",");
                    break;
                case Algorithm.RS384:
                    sb.append(COSEAlgorithmIdentifier.RS384.getValue()).append(",");
                    break;
                case Algorithm.ES512:
                    sb.append(COSEAlgorithmIdentifier.ES512.getValue()).append(",");
                    break;
                case Algorithm.RS512:
                    sb.append(COSEAlgorithmIdentifier.RS512.getValue()).append(",");
                    break;
                case "RS1":
                    sb.append(COSEAlgorithmIdentifier.RS1.getValue()).append(",");
                    break;
                default:
                    // NOP
            }
        }
        if (sb.lastIndexOf(",") > -1) sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }

    protected String getRpID(MFAConfigModel configModel, RequiredActionContext context) {
        WebAuthnPolicy policy = getWebAuthnPolicy(configModel,context);
        String rpId = policy.getRpId();
        if (rpId == null || rpId.isEmpty()) rpId = context.getUriInfo().getBaseUri().getHost();
        return rpId;
    }

    protected WebAuthnPolicy getWebAuthnPolicy(MFAConfigModel configModel, RequiredActionContext context) {
        if (configModel == null)
            return null;

        String policyName = configModel.getConfig().get("WEBAUTHN_POLICY");
        RealmPolicyModel realmPolicyModel = context.getSession().policies().getPolicyByName(context.getRealm(), policyName);

        if (realmPolicyModel == null)
            return null;

        String policyValue = realmPolicyModel.getPolicyValue();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String,Object> objectMap = objectMapper.readValue(policyValue,Map.class);
            WebAuthnPolicy webAuthnPolicy = toWebAuthPolicy(objectMap);
            return webAuthnPolicy;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final String ERR_LABEL = "web_authn_registration_error";
    private static final String ERR_DETAIL_LABEL = "web_authn_registration_error_detail";

    private void setErrorResponse(RequiredActionContext context, final String errorCase, final String errorMessage,final boolean showTroubleShoot) {
        Response errorResponse = null;
        LoginFormsProvider formsProvider = context.form();
        if(showTroubleShoot)
        {
            formsProvider.setAttribute("showTroubleShoot",showTroubleShoot);
        }
        switch (errorCase) {
            case WEBAUTHN_ERROR_REGISTER_VERIFICATION:
                logger.warnv("WebAuthn API .create() response validation failure. {0}", errorMessage);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .detail(ERR_DETAIL_LABEL, errorMessage)
                        .error(Errors.INVALID_USER_CREDENTIALS);
                errorResponse = formsProvider
                        .setError(errorCase, errorMessage)
                        .setAttribute(WEB_AUTHN_TITLE_ATTR, WEBAUTHN_REGISTER_TITLE)
                        .createWebAuthnErrorPage();
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_REGISTRATION:
                logger.warn(errorCase);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .detail(ERR_DETAIL_LABEL, errorMessage)
                        .error(Errors.INVALID_REGISTRATION);
                errorResponse = formsProvider
                        .setError(errorCase, errorMessage)
                        .setAttribute(WEB_AUTHN_TITLE_ATTR, WEBAUTHN_REGISTER_TITLE)
                        .createWebAuthnErrorPage();
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_API_GET:
                logger.warnv("error returned from navigator.credentials.get(). {0}", errorMessage);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .detail(ERR_DETAIL_LABEL, errorMessage)
                        .error(Errors.NOT_ALLOWED);
                errorResponse = createErrorResponse(context, errorCase,showTroubleShoot);
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_DIFFERENT_USER:
                logger.warn(errorCase);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .error(Errors.DIFFERENT_USER_AUTHENTICATED);
                errorResponse = createErrorResponse(context, errorCase,showTroubleShoot);
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_AUTH_VERIFICATION:
                logger.warnv("WebAuthn API .get() response validation failure. {0}", errorMessage);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .detail(ERR_DETAIL_LABEL, errorMessage)
                        .error(Errors.INVALID_USER_CREDENTIALS);
                errorResponse = createErrorResponse(context, errorCase,showTroubleShoot);
                context.challenge(errorResponse);
                break;
            case WEBAUTHN_ERROR_USER_NOT_FOUND:
                logger.warn(errorCase);
                context.getEvent()
                        .detail(ERR_LABEL, errorCase)
                        .error(Errors.USER_NOT_FOUND);
                errorResponse = createErrorResponse(context, errorCase,showTroubleShoot);
                context.challenge(errorResponse);
                break;
            default:
                // NOP
        }
    }

    private Response createErrorResponse(RequiredActionContext context, final String errorCase, final  boolean showTroubleShoot) {
        LoginFormsProvider provider = context.form().setError(errorCase);
        if(showTroubleShoot)
        {
            provider.setAttribute("showTroubleShoot",showTroubleShoot);
        }
        UserModel user = context.getUser();
        if (user != null) {
            WebAuthnAuthenticatorsBean authenticators = new WebAuthnAuthenticatorsBean(context.getSession(), context.getRealm(), user, getCredentialType());
            if (authenticators.getAuthenticators() != null) {
                provider.setAttribute(WebAuthnConstants.ALLOWED_AUTHENTICATORS, authenticators);
            }
        }
        return provider.createWebAuthnErrorPage();
    }


    private WebAuthnRegistrationManager createWebAuthnRegistrationManager(KeycloakSession session) {
        return new WebAuthnRegistrationManager(
                Arrays.asList(
                        new NoneAttestationStatementValidator(),
                        new PackedAttestationStatementValidator(),
                        new TPMAttestationStatementValidator(),
                        new AndroidKeyAttestationStatementValidator(),
                        new AndroidSafetyNetAttestationStatementValidator(),
                        new FIDOU2FAttestationStatementValidator()
                ), this.getCertPathtrustValidator(session),
                new DefaultSelfAttestationTrustworthinessValidator(),
                Collections.emptyList(), // Custom Registration Validator is not supported
                new ObjectConverter()
        );
    }

    private CertPathTrustworthinessValidator getCertPathtrustValidator(KeycloakSession session) {
        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
            return new NullCertPathTrustworthinessValidator();
        } else {
            KeyStoreTrustAnchorsProvider trustAnchorsProvider = new KeyStoreTrustAnchorsProvider();
            trustAnchorsProvider.setKeyStore(truststoreProvider.getTruststore());
            TrustAnchorsResolverImpl resolverImpl = new TrustAnchorsResolverImpl(trustAnchorsProvider);
            TrustAnchorCertPathTrustworthinessValidator trustValidator = new TrustAnchorCertPathTrustworthinessValidator(resolverImpl);
            return trustValidator;
        }
    }

    private void showInfoAfterWebAuthnApiCreate(RegistrationData response) {
        AttestedCredentialData attestedCredentialData = response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
        AttestationStatement attestationStatement = response.getAttestationObject().getAttestationStatement();
        logger.debugv("createad key's algorithm = {0}", String.valueOf(attestedCredentialData.getCOSEKey().getAlgorithm().getValue()));
        logger.debugv("aaguid = {0}", attestedCredentialData.getAaguid().toString());
        logger.debugv("attestation format = {0}", attestationStatement.getFormat());
    }

    private void checkAcceptedAuthenticator(RegistrationData response, WebAuthnPolicy policy) throws Exception {
        String aaguid = response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getAaguid().toString();
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
    public String getDisplayName() {
        return "WebAuthn";
    }

    @Override
    public String getProviderClass() {
        return (MFAProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "webauthn.png";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/MFA/WebAuthn-Factor.md";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        return null;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("enable_mfa_recovery");
        property.setLabel("mfa-webauthn-recovery");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("mfa-webauthn-recovery-helptext");
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        RealmPolicyProvider realmPolicyProvider = session.policies();

        List<String> providerList = new ArrayList<>();
        if (realmPolicyProvider != null) {
            List<RealmPolicyModel> policyModels = realmPolicyProvider.getPoliciesByType(session.getContext().getRealm(), "webauthn").collect(Collectors.toList());

            if (!policyModels.isEmpty()) {
                providerList.addAll(policyModels.stream().map(x -> x.getPolicyName()).collect(Collectors.toList()));
            }

        }

        ProviderConfigProperty provider;
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName().equals("WEBAUTHN_POLICY")).findFirst();
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("WEBAUTHN_POLICY");
        provider.setLabel("webauthn-policy");
        if(providerList.isEmpty())
        {
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("No WebAuth policy available. Click <a href=' #/realms/"+session.getContext().getRealm().getName()+"/policies'> here </a> to create one");
        }
        else {
            provider.setType(ProviderConfigProperty.LIST_TYPE);
            provider.setOptions(providerList);
            provider.setHelpText("webauthn-policy-hlptxt");
        }
        provider.setRequired(true);
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(0,provider);
        }
        return configProperties;
    }

    @Override
    public OTPPolicy getOtpPolicy(RealmModel realmModel) {
        return null;
    }

    @Override
    public MFAProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "webauthn";
    }

    @Override
    public String getHelpText() {
        return WEBAUTHN_MFA_HELP_TEXT;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

}
