package com.qlikverify.services.theme.subscriptionpolicies;

import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.common.Version;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
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

public class AuthenticationPolicy implements ClientRegistrationPolicy {
    private final KeycloakSession session;
    private final ComponentModel componentModel;
    private static final Logger logger = Logger.getLogger(AuthenticationPolicy.class);
    private static final String LOG_TAG = "EZTO:AUTHENTICATION_POLICY: ";

    public AuthenticationPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel, Map<String, Object> attributes) {
        // Bypassed: no billing charge
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
