package com.qlikverify.services.authenticators.onboarding.userinvite;

import com.qlikverify.services.constants.Constants;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.EmailConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserInviteAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "user-invite-authenticator";

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.ONBOARDING;
    }

    @Override
    public String getArticleId() {
        return null;
    }

    @Override
    public String getDisplayType() {
        return "user-invite-onboarding-title";
    }

    @Override
    public String getReferenceCategory() {
        return "user-invite-onboarding-title";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }
    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "user-invite-onboarding-title";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        setupEmailProviders(session);

        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("next_authenticator")).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        List<ProviderFactory> providers = session.getKeycloakSessionFactory().getProviderFactories(Authenticator.class);
        String type = "ONBOARDING";
        if (type != null) {
            providers = providers.stream().filter(x -> ((AuthenticatorFactory) x).authenticatorType() != null && (((AuthenticatorFactory) x).authenticatorType().toString().equals(type.toUpperCase(Locale.ROOT)) || ((AuthenticatorFactory) x).authenticatorType().toString().equals(AuthenticationType.ALL.toString().toUpperCase(Locale.ROOT)))).collect(Collectors.toList());
        }
        providers = providers.stream().filter(x -> !((AuthenticatorFactory) x).isInBuilt()).collect(Collectors.toList());

        List<String> providerList = new ArrayList<>();
        if (!providers.isEmpty()) {
            providers.forEach(x -> providerList.add(x.getId()));
        }
        provider.setName("next_authenticator");
        provider.setLabel("select-nxt-auth-label");
        provider.setType(ProviderConfigProperty.LIST_TYPE);
        provider.setOptions(providerList);
        provider.setRequired(true);
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    private void setupEmailProviders(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.EMAIL_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.EMAIL_SENDER);
        provider.setLabel(Constants.EMAIL_SENDER_LABEL);

        if(session.getContext().getRealm().isEnabledOwnEmailProvider()){
            List<EmailConfigModel> providers = session.getContext().getRealm().getEmailConfigsStream().collect(Collectors.toList());

            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getEmailProviderId()));
            }
            provider.setRequired(true);
            if (!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("email-sender-hlp-txt");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No Email Sender are registered. Click <a href=' #/realms/" + session.getContext().getRealm().getName() + "/email/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All Emails are routed through default Email Provider from "+org.keycloak.Config.getAdminRealm()+".If you want to use your email providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/email/providers/create'> here </a>");
        }

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new UserInviteAuthenticator();
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
}
