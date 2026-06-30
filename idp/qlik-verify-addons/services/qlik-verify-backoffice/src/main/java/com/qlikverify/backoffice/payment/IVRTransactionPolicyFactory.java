package com.qlikverify.backoffice.payment;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.policy.AbstractClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;

import java.util.Collections;
import java.util.List;

public class IVRTransactionPolicyFactory extends AbstractClientRegistrationPolicyFactory {

    public static final String PROVIDER_ID = "ivr-transaction-policy-checker";

    @Override
    public ClientRegistrationPolicy create(KeycloakSession session, ComponentModel model) {
        return new IVRTransactionPolicy(session, model);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return Collections.emptyList();
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
