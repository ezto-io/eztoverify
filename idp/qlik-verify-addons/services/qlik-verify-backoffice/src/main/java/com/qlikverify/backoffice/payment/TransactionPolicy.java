package com.qlikverify.backoffice.payment;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TransactionPolicy implements ClientRegistrationPolicy {

    private final KeycloakSession session;
    private final ComponentModel componentModel;
    private static final Executor executor = Executors.newCachedThreadPool();
    private static final Logger logger = Logger.getLogger(TransactionPolicy.class);

    public TransactionPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel, Map<String, Object> attributes) {
        // Usage billing is completely bypassed in this free/open version
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
