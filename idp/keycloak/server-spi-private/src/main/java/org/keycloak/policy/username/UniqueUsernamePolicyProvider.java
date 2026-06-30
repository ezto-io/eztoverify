package org.keycloak.policy.username;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UsernamePolicy;
import org.keycloak.policy.PolicyError;
import org.keycloak.policy.action.EmailVerifiedPolicyProviderFactory;

public class UniqueUsernamePolicyProvider implements UsernamePolicyProvider {

    private static final String ERROR_MESSAGE = "notUniqueUsernameMessage";

    private KeycloakSession session;

    public UniqueUsernamePolicyProvider(KeycloakSession session) {
        this.session = session;
    }


    @Override
    public PolicyError validate(UsernamePolicy policy, UserModel user) {
        return validate(policy,user.getUsername());
    }

    @Override
    public PolicyError validate(UsernamePolicy policy, String user) {
        Object policyConfig = policy.getPolicyConfig(EmailVerifiedPolicyProviderFactory.ID);

        if(policyConfig == null)
            return null;

        UserModel avaUser = session.users().getUserByUsername(user,session.getContext().getRealm());

        if(avaUser == null)
            new PolicyError(ERROR_MESSAGE);

        return null;
    }

    @Override
    public Object parseConfig(String value) {
        return null;
    }

    @Override
    public void close() {

    }
}
