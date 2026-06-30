package org.keycloak.onboarding;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

public interface SubscriptionPolicyProvider extends Provider {

    void addCheckLicensePolicy(RealmModel realm,boolean isAvailable);

    void addMaxClientsPolicy(RealmModel realm,boolean isAvailable);

    void addMaxUsersPolicy(RealmModel realm,boolean isAvailable);

    void addAuthenticationPolicy(RealmModel realm,boolean isAvailable);

    void addSmsPolicy(RealmModel realm, boolean isAvailable);

    void addEmailPolicy(RealmModel realm,boolean isAvailable);

    void addWhatsappPolicy(RealmModel realm,boolean isAvailable);
}
