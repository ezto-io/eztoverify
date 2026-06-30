package org.keycloak.sms;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.List;
import java.util.Map;

public interface SmsProviderFactory extends ProviderFactory<SmsProvider> {

    String getDisplayName();

    String getProviderClass();

    String getLogo();

    String getHowTodo();

    boolean isConfigurable();

    boolean isRequiredTestConfigDetails();

    List<String> validateConfig(KeycloakSession session, Map<String, String> config);

    List<ProviderConfigProperty> getConfigProperties(KeycloakSession session);

    String getHelpText();

    boolean isNew();

    SmsType getType();
}
