package org.keycloak.onboarding;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public interface OnboardingProvider extends Provider {
    void onApplicationOnboarding(ClientModel clientModel, RealmModel realmModel);
    void onTenantOnboarding(RealmModel realmModel, RealmRepresentation realmRepresentation);
    Response onTenantPostOnboarding(KeycloakSession session,RealmModel realmModel,String adminEmailId);

    void onMasterOnboarding(RealmModel realmModel);
    void onWelcomePreOnboarding(RealmModel realmModel);
    Response onWelcomePostOnboarding(RealmModel realmModel, MultivaluedMap<String, String> formData, HttpHeaders headers, String stepProviderId);
    void onMigrate(KeycloakSession session);
    void onLazyUserMigrate(KeycloakSession session, RealmModel realmModel, UserModel userModel);
    OnboardingRepresentation welcomeOnboardingSteps(KeycloakSession session, String switchStep);
    boolean isOnboardingCompleted();
    void onDNSCompleted(RealmModel realmModel,String dnsName);
    void onDNSRemoved(RealmModel realmModel, String dnsName);
}
