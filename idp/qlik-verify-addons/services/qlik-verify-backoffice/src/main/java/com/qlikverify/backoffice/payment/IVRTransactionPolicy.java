package com.qlikverify.backoffice.payment;

import org.keycloak.common.Version;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;

import java.io.IOException;
import java.util.Map;

public class IVRTransactionPolicy implements ClientRegistrationPolicy {

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public IVRTransactionPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel, Map<String, Object> attributes) {
        if (Version.OFFERING.equalsIgnoreCase("onpremise"))
            return;

        RealmModel realm = session.getContext().getRealm();
        if (componentModel.getSubType().equalsIgnoreCase(RegistrationAuth.AUTHENTICATED.toString()) && Version.OFFERING.equalsIgnoreCase("cloud")) {
            // Usage sending bypassed
        }
    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {
        // NOP
    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }
}
