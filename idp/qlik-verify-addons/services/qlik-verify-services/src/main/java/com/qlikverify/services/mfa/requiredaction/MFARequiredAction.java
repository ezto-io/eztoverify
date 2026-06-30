package com.qlikverify.services.mfa.requiredaction;

import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.*;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.MFAProviderRepresentation;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

public class MFARequiredAction extends MFARecovery implements RequiredActionProvider, RequiredActionFactory {
    @Override
    public String getDisplayText() {
        return "MFA Action";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.MFA;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"ciam","wiam"};
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getSession().getContext().getClient().isOverrideAuthentication()
                && context.getSession().getContext().getClient().getMFAConnections().size() == 0) {
            context.getUser().removeRequiredAction(getId());
            return;
        }

        Stream<UserSessionModel> userSessionsStream = context.getSession().sessions().getUserSessionsStream(context.getRealm(), context.getUser());

        Optional<UserSessionModel> userSessionModel =
                userSessionsStream.filter(x -> x.getId().equalsIgnoreCase(context.getAuthenticationSession().getParentSession().getId())).findAny();
        if (userSessionModel.isPresent() && context.getAuthenticationSession().getClient().getName().equalsIgnoreCase(Constants.ADMIN_CONSOLE_CLIENT_ID)) {
            int started = userSessionModel.get().getStarted();

            int validAccessTokenTime = started + context.getRealm().getAccessTokenLifespanForImplicitFlow();

            if ((Time.currentTime() < validAccessTokenTime) && userSessionModel.get().getNote("mfa-action") != null && userSessionModel.get().getNote("mfa-action") .equalsIgnoreCase("completed")) {
                context.getUser().removeRequiredAction(getId());
                return;
            }
        }


        if (!context.getSession().getContext().getClient().isOverrideAuthentication() &&  context.getRealm().getMFAConnections().size() == 0) {
            context.getUser().removeRequiredAction(getId());
            return;
        }

        String policyId = null;
        String policyType = null;
        RealmPolicyModel policyById = null;

        if (context.getSession().getContext().getClient().isOverrideAuthentication()) {
            if (context.getSession() != null && context.getSession().getContext() != null
                    && context.getSession().getContext().getClient().getMfaPolicyId() != null
                    && !context.getSession().getContext().getClient().getMfaPolicyId().isEmpty()) {
                policyId = context.getSession().getContext().getClient().getMfaPolicyId();
                policyType = context.getSession().getContext().getClient().getMfaPolicyType();
            }
        } else {
            policyId = context.getRealm().getAttribute("mfaPolicy");
        }
        if (policyId != null) {
            policyById = context.getSession().policies().getPolicyById(context.getRealm(), policyId);
            if (policyById.getPolicyType() != null) {
                policyType = policyById.getPolicyType();
            }
        }
        boolean result = false;
        if (policyId == null  || policyType == null){
            return;
        }
        if (policyType != null) {
            if (policyType.equals("action")) {
                result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, policyId, context.getUser());
            } else {
                result = context.getSession().getProvider(CibaPolicyProvider.class).validate(context.getSession(), context.getAuthenticationSession().getAuthNote(BINDING_MESSAGE), policyId);
            }
        }

        if (!result
                && context.getSession().getContext().getClient().isOverrideAuthentication()
                && !context.getAuthenticationSession().getClient().getMFAConnections().containsKey("is_mandatory")) {
            context.getUser().removeRequiredAction(getId());
        }

        if (!result
                && (context.getRealm().getAttribute("mfaMandatory", false))) {
            context.getUser().removeRequiredAction(getId());
        }

    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        Map<String, Boolean> connections =
                context.getAuthenticationSession().getClient().isOverrideAuthentication() ?
                        context.getAuthenticationSession().getClient().getMFAConnections():
                        context.getRealm().getMFAConnections();
        boolean isMandatory = false;
        if (context.getAuthenticationSession().getClient().isOverrideAuthentication() && connections.containsKey(
                "is_mandatory")) {
            isMandatory = connections.get("is_mandatory");
            connections.remove("is_mandatory");
        } else {
            isMandatory = context.getRealm().getAttribute("is_mandatory", false);
        }

        if (connections.size() == 0) {
            context.success();
            return;
        }

        List<String> connectionIds = connections.entrySet().stream().filter(x -> x.getValue()).map(x -> x.getKey()).collect(Collectors.toList());
        if (connectionIds.isEmpty()) {
            context.success();
            return;
        }

        try {
            CredentialModel credentialModel = getCredentialModel(context);

            if (credentialModel != null) {
                if(!connectionIds.contains(credentialModel.getProviderId())){
                    context.success();
                    return;
                }
                MFAProvider provider = context.getSession().getProvider(MFAProvider.class, credentialModel.getProviderId());
                MFAConfigModel configModel = context.getRealm().getMFAConfigByProviderId(credentialModel.getProviderId());
                provider.onPreLogin(configModel, context);
            } else {
                if (connections.size() == 1 && isMandatory) {
                    String mfaProviderId = connectionIds.get(0);
                    MFAProvider provider = context.getSession().getProvider(MFAProvider.class, mfaProviderId);
                    MFAConfigModel configModel = context.getRealm().getMFAConfigByProviderId(mfaProviderId);
                    provider.onPreRegistration(configModel, context, isMandatory);
                    return;
                }

                List<MFAProviderRepresentation> mfaProviderRepresentations = connectionIds.stream()
                        .map(x -> ModelToRepresentation.toRepresentation(context.getSession(), x))
                        .collect(Collectors.toList());

                Response response = context.form().setAttribute("client_mfa_providers", mfaProviderRepresentations)
                        .setAttribute("mfa_selection", true)
                        .setAttribute("mfa_is_mandatory", isMandatory)
                        .setAttribute("variant", EnvironmentReader.getValue("VARIANT"))
                        .createForm("mfa-section.ftl");

                context.challenge(response);
            }
        } catch (NullPointerException e){
            Response response = context.form().setError(Messages.ERROR_OCCERRED_CONTACT_ADMIN).createErrorPage(Response.Status.UNAUTHORIZED);
            context.challenge(response);
        }
    }

    private CredentialModel getCredentialModel(RequiredActionContext context) {
        UserModel userModel = context.getAuthenticationSession().getAuthenticatedUser();
        if(userModel == null){
            throw new NullPointerException();
        }

        boolean isConfigured = isConfiguredForMFA(context, userModel);

        CredentialModel credentialModel = null;

        if (isConfigured) {
            credentialModel = getConfiguredMFAProvider(context, userModel);
        }
        return credentialModel;
    }

    private CredentialModel getConfiguredMFAProvider(RequiredActionContext context, UserModel userModel) {
        List<CredentialModel> credentialModels = context.getSession().userCredentialManager().getStoredCredentialsStream(context.getRealm(), userModel)
                .filter(credentialModel -> credentialModel.getClientId() != null && credentialModel.getClientId().equals(context.getAuthenticationSession().getClient().getClientId()))
                .filter(credentialModel -> !(credentialModel.getProviderId() == null || credentialModel.getType().equalsIgnoreCase("password")))
                .collect(Collectors.toList());
        List<String> realmMFAProvider = context.getRealm().getMFAProvidersStream().collect(Collectors.toList());

        if(!credentialModels.isEmpty() && !realmMFAProvider.contains(credentialModels.get(0).getProviderId())) {
                context.getSession().userCredentialManager().removeStoredCredential(context.getRealm(), userModel, credentialModels.get(0).getId());
                credentialModels.remove(0);
        }

        if (credentialModels.isEmpty())
            return null;

        return credentialModels.get(0);
    }

    private OTPCredentialModel getOtpCredentialModel(CredentialModel credential) {
        return OTPCredentialModel.createFromCredentialModel(credential);
    }

    private WebAuthnCredentialModel getWebAuthnCredentialModel(CredentialModel credentialModel) {
        return WebAuthnCredentialModel.createFromCredentialModel(credentialModel);
    }

    private boolean isConfiguredForMFA(RequiredActionContext context, UserModel userModel) {
        boolean isOtpType = context.getSession().userCredentialManager().isConfiguredFor(context.getRealm(), userModel, OTPCredentialModel.TYPE);

        if (isOtpType)
            return isOtpType;

        boolean isWebAuthnType = context.getSession().userCredentialManager().isConfiguredFor(context.getRealm(), userModel, WebAuthnCredentialModel.TYPE_TWOFACTOR);

        if (isWebAuthnType)
            return isWebAuthnType;

        return false;
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        String rcCopied = params.getFirst("rc_copied");
        String rcSkipped = params.getFirst("rc_skip");

        if (rcCopied != null || rcSkipped != null) {
            if (rcCopied != null) {
                createRecovery(context);
            }
            context.success();
            return;
        }

        String isTroubleShoot = params.getFirst("troubleShootMFA");
        if (Boolean.parseBoolean(isTroubleShoot)) {
            try{
                CredentialModel credentialModel = getCredentialModel(context);

                String mfaProviderId = credentialModel.getProviderId();

                String verifyTroubleShoot = params.getFirst("verify_troubleshoot");
                String cancelTroubleShoot = params.getFirst("cancel_troubleshoot");

                if (cancelTroubleShoot != null && Boolean.parseBoolean(cancelTroubleShoot)) {
                    requiredActionChallenge(context);
                    return;
                }

                if (verifyTroubleShoot != null && Boolean.parseBoolean(verifyTroubleShoot)) {
                    String recoveryCode = params.getFirst("recovery_code");
                    if (recoveryCode == null || recoveryCode.isEmpty()) {
                        showRecovery(context, mfaProviderId, "recovery-code-required");
                        return;
                    }

                    boolean isSucces = isValid(context, recoveryCode);

                    if (!isSucces) {
                        showRecovery(context, mfaProviderId, "invalid-recovery-code");
                        return;
                    }

                    if (isSucces) {
                        boolean isremoved = context.getSession().userCredentialManager().removeStoredCredential(context.getRealm(), context.getUser(), credentialModel.getId());
                        if (!isremoved) {
                            showRecovery(context, mfaProviderId, "invalid-recovery-code");
                            return;
                        }

                        requiredActionChallenge(context);
                        return;
                    }
                }


                showRecovery(context, mfaProviderId, null);
                return;

            } catch (NullPointerException e){
                Response response = context.form().setError(Messages.ERROR_OCCERRED_CONTACT_ADMIN).createErrorPage(Response.Status.UNAUTHORIZED);
                context.challenge(response);
                return;
            }

        }

        String skip = params.getFirst("skipMFA");

        if (skip != null && Boolean.parseBoolean(skip)) {
            context.success();
            return;
        }

        String isMFASelection = params.getFirst("mfa_selection");
        if (isMFASelection != null && Boolean.parseBoolean(isMFASelection)) {
            String mfaProviderId = params.getFirst("mfa_selected_id");
            if (mfaProviderId == null) {
                requiredActionChallenge(context);
                return;
            }
            MFAProvider provider = context.getSession().getProvider(MFAProvider.class, mfaProviderId);
            MFAConfigModel configModel = context.getRealm().getMFAConfigByProviderId(mfaProviderId);
            provider.onPreRegistration(configModel, context, !Boolean.parseBoolean(skip));
            return;
        }

        try{
            CredentialModel credentialModel = getCredentialModel(context);
            if (credentialModel != null) {
                mfaValidation(context, credentialModel);
            } else {
                mfaRegistration(context, params);
            }
        }catch (NullPointerException exception){
            Response response = context.form().setError(Messages.ERROR_OCCERRED_CONTACT_ADMIN).createErrorPage(Response.Status.UNAUTHORIZED);
            context.challenge(response);
        }
    }

    private void mfaValidation(RequiredActionContext context, CredentialModel credentialModel) {
        MFAProvider provider = context.getSession().getProvider(MFAProvider.class, credentialModel.getProviderId());
        MFAConfigModel configModel = context.getRealm().getMFAConfigByProviderId(credentialModel.getProviderId());
        boolean isSuccess = provider.onPostLogin(configModel, context);
        if (isSuccess) {
            if(!isMFARecoveryConfigured(context) && Boolean.parseBoolean(GeneralHelper.getConfigValue(configModel, "enable_mfa_recovery")))
            {
                MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
                List<String> codes = getRecovery(context);
                LoginFormsProvider form = context.form()
                        .setFormData(formData)
                        .setAttribute("mfaProviderId", credentialModel.getProviderId())
                        .setAttribute("recoveryCode", codes);
                Response challenge = form.createForm("mfa-recovery-show.ftl");
                context.challenge(challenge);
                return;
            }
            context.success();
        }
    }

    private void mfaRegistration(RequiredActionContext context, MultivaluedMap<String, String> params) {
        String providerId = params.getFirst("mfaProviderId");
        // Ugly way to identify duo, but still this is better way for now
        if (providerId == null && params.getFirst("sig_response") != null) {
                providerId = "duo-mfa";
        }
        MFAProvider provider = context.getSession().getProvider(MFAProvider.class, providerId);
        MFAConfigModel configModel = context.getRealm().getMFAConfigByProviderId(providerId);
        boolean isSuccess = provider.onPostRegistration(configModel, context);
        if (isSuccess) {
            if (Boolean.parseBoolean(GeneralHelper.getConfigValue(configModel, "enable_mfa_recovery"))) {
                MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
                List<String> codes = getRecovery(context);
                LoginFormsProvider form = context.form()
                        .setFormData(formData)
                        .setAttribute("mfaProviderId", providerId)
                        .setAttribute("recoveryCode", codes);
                Response challenge = form.createForm("mfa-recovery-show.ftl");
                context.challenge(challenge);
                return;
            }
            context.success();
        }
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
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
        return "mfa-action";
    }

    private OTPCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (OTPCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-otp");
    }
}
