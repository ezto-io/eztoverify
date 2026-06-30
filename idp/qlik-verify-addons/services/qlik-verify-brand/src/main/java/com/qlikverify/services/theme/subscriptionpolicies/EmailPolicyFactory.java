package com.qlikverify.services.theme.subscriptionpolicies;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.policy.AbstractClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;

import java.util.ArrayList;
import java.util.List;

public class EmailPolicyFactory extends AbstractClientRegistrationPolicyFactory {

    public static final String PROVIDER_ID = "email-policy-checker";

    @Override
    public ClientRegistrationPolicy create(KeycloakSession session, ComponentModel model) {
        return new EmailPolicy(session,model);
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return new ArrayList<>();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
