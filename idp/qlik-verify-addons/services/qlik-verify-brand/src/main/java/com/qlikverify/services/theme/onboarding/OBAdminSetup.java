package com.qlikverify.services.theme.onboarding;

import com.qlikverify.services.theme.helpers.GeneralHelpers;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.onboarding.OnboardingStepProvider;
import org.keycloak.onboarding.OnboardingStepProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.SuccessResponse;
import org.keycloak.services.managers.ApplianceBootstrap;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.keycloak.authentication.authenticators.utils.GeneralHelper.isValidEmail;

public class OBAdminSetup implements OnboardingStepProvider, OnboardingStepProviderFactory {
    KeycloakSession session;

    @Override
    public List<ProviderConfigProperty> getConfig() {
        // List of form fields required for onboarding.
        List<ProviderConfigProperty> stepFields1 = new ArrayList<>();

        ProviderConfigProperty emailField = new ProviderConfigProperty();
        emailField.setType(ProviderConfigProperty.EMAIL);
        emailField.setName("admin_email");
        emailField.setHelpText("master-admin-email-helptext");
        emailField.setLabel("master-admin-email");
        emailField.setRequired(true);
        stepFields1.add(emailField);

        ProviderConfigProperty passwordField = new ProviderConfigProperty();
        passwordField.setType(ProviderConfigProperty.PASSWORD);
        passwordField.setName("admin_password");
        passwordField.setHelpText("master-admin-password-helptext");
        passwordField.setLabel("master-admin-password");
        passwordField.setRequired(true);
        stepFields1.add(passwordField);

        return stepFields1;
    }

    @Override
    public Response process(MultivaluedMap<String, String> params, RealmModel realmModel) {
        String adminEmailId = params.getFirst("admin_email");
        if (adminEmailId == null || adminEmailId.isEmpty())
            return ErrorResponse.error("emailid-required", Response.Status.BAD_REQUEST);

        String password = params.getFirst("admin_password");
        if (password == null || password.isEmpty())
            return ErrorResponse.error("password-required", Response.Status.BAD_REQUEST);

        if (!isValidEmail(adminEmailId))
            return ErrorResponse.error("invalid-email-address", Response.Status.BAD_REQUEST);

        if (!isAdminPresents(adminEmailId, realmModel)) {
            ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
            applianceBootstrap.createMasterRealmUser(adminEmailId, password);
        }

        GeneralHelpers.addToCompletedStep(realmModel,
                this.getProviderId());
        return SuccessResponse.success("admin-acc-created", Response.Status.OK);
    }

    @Override
    public boolean validate(MultivaluedMap<String, String> params, RealmModel realmModel) {
        return false;
    }

    @Override
    public boolean isCompleted(RealmModel realmModel) {
        ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
        return !applianceBootstrap.isNoMasterUser() && GeneralHelpers.checkForCurrentStep(realmModel, this.getProviderId());
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public String getTitle() {
        return ("admin-details");
    }

    @Override
    public String getDescription() {
        return "admin-details-desc";
    }

    @Override
    public OnboardingStepProvider create(KeycloakSession session) {
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
    public String getProviderId() {
        return Constants.OB_ADMIN_SETUP;
    }

    @Override
    public boolean isSkippable() {
        return false;
    }

    @Override
    public String getId() {
        return Constants.OB_ADMIN_SETUP;
    }

    private boolean isAdminPresents(String email, RealmModel realm) {
        return (session.users().getUserByEmail(realm, email) != null);
    }
}
