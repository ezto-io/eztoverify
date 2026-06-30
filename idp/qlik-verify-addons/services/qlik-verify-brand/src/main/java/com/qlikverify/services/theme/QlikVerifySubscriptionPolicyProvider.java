package com.qlikverify.services.theme;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.onboarding.SubscriptionPolicyProvider;
import org.keycloak.onboarding.SubscriptionPolicyProviderFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;

public class QlikVerifySubscriptionPolicyProvider implements SubscriptionPolicyProvider, SubscriptionPolicyProviderFactory {
    KeycloakSession session;
    protected static final Logger logger = Logger.getLogger(QlikVerifySubscriptionPolicyProvider.class);

    @Override
    public void addCheckLicensePolicy(RealmModel realm, boolean isAvailable) {
        QlikVerifyOnboardingProvider.addCheckLicensePolicy(realm, ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED));
    }

    @Override
    public void addMaxClientsPolicy(RealmModel realm, boolean isAvailable) {
        QlikVerifyOnboardingProvider.addMaxClientsPolicy(realm,ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED));
    }

    @Override
    public void addMaxUsersPolicy(RealmModel realm, boolean isAvailable) {
        // NOP
    }

    @Override
    public void addAuthenticationPolicy(RealmModel realm, boolean isAvailable) {
        QlikVerifyOnboardingProvider.addAuthenticationPolicy(realm);
    }

    @Override
    public void addSmsPolicy(RealmModel realm, boolean isAvailable) {
        QlikVerifyOnboardingProvider.addSmsPolicy(realm);
    }

    @Override
    public void addEmailPolicy(RealmModel realm, boolean isAvailable) {
        QlikVerifyOnboardingProvider.addEmailPolicy(realm,isAvailable);
    }

    @Override
    public void addWhatsappPolicy(RealmModel realm, boolean isAvailable) {
        QlikVerifyOnboardingProvider.addWhatsappPolicy(realm);
    }

    @Override
    public SubscriptionPolicyProvider create(KeycloakSession session) {
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
    public String getId() {
        return "qv-subscription-policies";
    }
}
