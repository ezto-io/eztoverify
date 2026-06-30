package com.qlikverify.services.theme.onboarding;

import com.qlikverify.services.theme.helpers.GeneralHelpers;
import org.keycloak.Config;
import org.keycloak.email.ConfigTestResponse;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.*;
import org.keycloak.onboarding.OnboardingStepProvider;
import org.keycloak.onboarding.OnboardingStepProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.SuccessResponse;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.services.theme.onboarding.Constants.*;

public class OBAdminEmailSetup implements OnboardingStepProvider, OnboardingStepProviderFactory {
    KeycloakSession session;

    @Override
    public List<ProviderConfigProperty> getConfig() {
        // List of form fields required for onboarding.
        List<ProviderConfigProperty> configs = new ArrayList<>();

        List<ProviderFactory> providerFactories =
                session.getKeycloakSessionFactory().getProviderFactoriesStream(EmailSenderProvider.class).collect(Collectors.toList());
        providerFactories.forEach(x -> {
            EmailSenderProviderFactory factory = (EmailSenderProviderFactory) x;
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setType(ProviderConfigProperty.CONFIG_TYPE);
            property.setName(factory.getId());
            property.setHelpText("choose-default-email-provider");
            property.setLogo(factory.getLogo());
            property.setLabel(factory.getDisplayName());
            property.setConfigProperties(factory.getConfigProperties(session));
            configs.add(property);
        });

        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.CONFIG_TYPE);
        property.setName(ADMIN_EMAIL_PROVIDER);
        property.setHelpText("choose-default-email-provider");
        property.setLabel("intro-add-email-provider-title");
        property.setConfigProperties(configs);
        property.setRequired(true);
        Optional<EmailConfigModel>  emailSender = session.getContext().getRealm().getEmailConfigsStream().findFirst();
        if (emailSender.isPresent()){
            Map<String, Object> dependsOn = new HashMap<>();
            dependsOn.put(ADMIN_EMAIL_PROVIDER, emailSender.get().getEmailProviderId());
            dependsOn.put("config", emailSender.get().getConfig());
            property.setDefaultValue(dependsOn);
        }

        return Arrays.asList(property);
    }

    @Override
    public Response process(MultivaluedMap<String, String> params, RealmModel realmModel) {
        String emailProviderId = params.getFirst(ADMIN_EMAIL_PROVIDER);
        if (emailProviderId == null)
            return ErrorResponse.error("email-provider-required", Response.Status.BAD_REQUEST);

        EmailSenderProviderFactory emailSenderProvider = (EmailSenderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EmailSenderProvider.class, emailProviderId);
        if (emailSenderProvider == null)
            return ErrorResponse.error("email-provider-not-found", Response.Status.BAD_REQUEST);

        List<ProviderConfigProperty> providerConfigProperties = emailSenderProvider.getConfigProperties(session);
        Map<String, String> receivedConfig = GeneralHelpers.getMatchedResult(providerConfigProperties,
                GeneralHelpers.convertMultiToRegularMap(params));

        List<String> errors = emailSenderProvider.validateConfig(session, receivedConfig);
        if (!errors.isEmpty()){
            return ErrorResponse.error(String.join(",", errors), Response.Status.BAD_REQUEST);
        }

        session.getContext().setRealm(realmModel);

        Map<String, String> testConfig = receivedConfig;
        UserModel user = isAdminPresents(realmModel);
        if(user != null){
            testConfig.put("emailId", user.getEmail());
        }

        ConfigTestResponse configTestResponse = emailSenderProvider.testConfig(session, user, testConfig);
        if (!configTestResponse.isSuccess())
            return ErrorResponse.error(configTestResponse.getErrorMessage(), Response.Status.BAD_REQUEST);

//        Remove email provider if any existing
        Optional<EmailConfigModel> emailProvider = realmModel.getEmailConfigsStream().findFirst();
            if (emailProvider.isPresent()){
                realmModel.removeEmailConfig(emailProvider.get());
            }

        EmailConfigModel emailConfigModel = new EmailConfigModel();
        emailConfigModel.setEmailProviderId(emailProviderId);
        emailConfigModel.setConfig(receivedConfig);
        realmModel.addEmailConfig(emailConfigModel);

        Set<String> emailSenders = new HashSet<>();
        emailSenders.add(emailProviderId);
        realmModel.setEmailSenders(emailSenders);

        GeneralHelpers.resetObUpdateBasedOnSubscription(session);
        GeneralHelpers.addToCompletedStep(realmModel,
                this.getProviderId());
        return SuccessResponse.success("email-setup-completed", Response.Status.OK);
    }


    private UserModel isAdminPresents(RealmModel realm){
        if (realm.getName().equals(Config.getAdminRealm())){
            RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);
            // Prefer an admin with a syntactically valid email. The bootstrap admin created from
            // KEYCLOAK_ADMIN has an unusable email (e.g. "admin" or null); using it as the SMTP
            // test recipient makes the server reject it ("501 Bad recipient address syntax").
            Optional<UserModel> users = session.users().getRoleMembersStream(realm, adminRole)
                    .filter(u -> u.getEmail() != null
                            && u.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                    .findFirst();
            if (!users.isPresent()) {
                users = session.users().getRoleMembersStream(realm, adminRole).findFirst();
            }
            if(users.isPresent()){
                return users.get();
            }
        } else {
            final List<UserModel> users =
                    session.users().searchForUserStream(realm, new HashMap<>(), -1, -1, true).collect(Collectors.toList());
            if (users !=null){
                return  users.get(0);
            }
        }

        return null;
    }
    @Override
    public boolean validate(MultivaluedMap<String, String> params, RealmModel realmModel) {
        return false;
    }

    @Override
    public boolean isCompleted(RealmModel realmModel) {
        return realmModel.getEmailConfigsStream().findAny().isPresent() && GeneralHelpers.checkForCurrentStep(realmModel, this.getProviderId());
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public String getTitle() {
        return "into-email-providers-title";
    }

    @Override
    public String getDescription() {
        return Objects.equals(session.getContext().getRealm().getName(), Config.getAdminRealm()) ? Constants.OB_ADMIN_MASTER_EMAIL_DESC : Constants.OB_ADMIN_EMAIL_DESC;
    }

    @Override
    public String getProviderId() {
        return Constants.OB_ADMIN_EMAIL_SETUP;
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
    public boolean isSkippable() {
        return false;
    }

    @Override
    public String getId() {
        return Constants.OB_ADMIN_EMAIL_SETUP;
    }
}
