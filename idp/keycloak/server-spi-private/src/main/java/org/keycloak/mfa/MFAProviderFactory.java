package org.keycloak.mfa;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.List;
import java.util.Map;

public interface MFAProviderFactory extends ProviderFactory<MFAProvider> {
    String getDisplayName();

    String getProviderClass();

    String getLogo();

    String getHowTodo();

    boolean isConfigurable();

    List<String> validateConfig(KeycloakSession session, Map<String, String> config);

    List<ProviderConfigProperty> getConfigProperties(KeycloakSession session);

    OTPPolicy getOtpPolicy(RealmModel realmModel);

    String getHelpText();

    boolean isNew();

    boolean isDefault();
}
