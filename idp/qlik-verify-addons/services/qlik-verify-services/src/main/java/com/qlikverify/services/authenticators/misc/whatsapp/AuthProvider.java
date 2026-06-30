package com.qlikverify.services.authenticators.misc.whatsapp;

import org.keycloak.authentication.AuthenticationFlowContext;

public interface AuthProvider {

    void onPreLogin(AuthenticationFlowContext context);

    /**
     * Its triggered after the user submitted the authentication for MFA
     * @param context
     */
    void onPostLogin(AuthenticationFlowContext context);

    /**
     * Its triggered after user submitted form to register for MFA
     * @param context
     */
    void onPostRegistration(AuthenticationFlowContext context);

    /**
     * Its triggered to show registration screen for MFA
     * @param context
     */
    void onPreRegistration(AuthenticationFlowContext context);
}
