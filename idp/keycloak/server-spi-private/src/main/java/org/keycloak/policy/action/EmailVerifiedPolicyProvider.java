package org.keycloak.policy.action;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.util.List;

public class EmailVerifiedPolicyProvider implements ActionPolicyProvider {

    private static final String ERROR_MESSAGE = "notEmailVerified";

    private KeycloakSession session;

    public EmailVerifiedPolicyProvider(KeycloakSession session) {
        this.session = session;
    }


    @Override
    public boolean validate(KeycloakSession session,ActionPolicy policy, UserModel user) {
        Object policyConfig = policy.getPolicyConfig(EmailVerifiedPolicyProviderFactory.ID);

        if(policyConfig == null)
            return false;

        boolean userEmailVerified = user.isEmailVerified();

        if(!(policyConfig instanceof Boolean))
        {
            return false;
        }

        if(userEmailVerified != (Boolean)policyConfig)
            return false;

        return true;
    }

    @Override
    public Object parseConfig(String value) {
        return Boolean.parseBoolean(value);
    }

    @Override
    public void close() {

    }
}
