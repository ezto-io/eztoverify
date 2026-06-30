/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.DisplayTypeAuthenticatorFactory;
import org.keycloak.authentication.authenticators.AttemptedAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderAuthenticatorFactory implements AuthenticatorFactory, DisplayTypeAuthenticatorFactory {

    protected static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.ALTERNATIVE, AuthenticationExecutionModel.Requirement.DISABLED
    };

    public static final String PROVIDER_ID = "identity-provider-redirector";
    public static final String DEFAULT_PROVIDER = "defaultProvider";
    protected static final String PROMPT_REQUEST_MODE = "promptRequestMode";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public String getDisplayType() {
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Redirects to default Identity Provider or Identity Provider specified with kc_idp_hint query parameter";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        List<String> idpProviders = session.getContext().getRealm().getIdentityProvidersStream()
                .filter(IdentityProviderModel::isEnabled)
                .map(IdentityProviderModel::getProviderId).collect(Collectors.toList());

        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream()
                .filter(property -> property.getName() != null && property.getName().equals(DEFAULT_PROVIDER))
                .findFirst();

        ProviderConfigProperty provider;
        provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);
        provider.setName(DEFAULT_PROVIDER);
        provider.setLabel("default-identity-provider");
        provider.setRequired(true);
        if(!idpProviders.isEmpty()) {
            provider.setType(ProviderConfigProperty.LIST_TYPE);
            provider.setOptions(idpProviders);
            provider.setHelpText("redirect-idp");
            if (!providerConfigProperty.isPresent())
                configProperties.add(provider);
            if (configProperties.stream().noneMatch(prop -> prop.getName().equals(PROMPT_REQUEST_MODE)))
                configProperties.add(getPromptConfigProperty());
        }  else {
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("No IDP are registered. Click <a href=' #/realms/"+session.getContext().getRealm().getName()+"/connections/social/create'> here </a> to create one");
            // also remove promptProvider from configProperties if present
            configProperties.removeIf(prop -> prop.getName().equals(PROMPT_REQUEST_MODE));
            if (!providerConfigProperty.isPresent())
                configProperties.add(provider);
        }
        return configProperties;
    }

    private ProviderConfigProperty getPromptConfigProperty() {
        ProviderConfigProperty promptConfigProviderProperty = new ProviderConfigProperty();
        promptConfigProviderProperty.setName(PROMPT_REQUEST_MODE);
        promptConfigProviderProperty.setLabel("oauth-prompt-parameter-for-identity-provider");
        promptConfigProviderProperty.setType(ProviderConfigProperty.LIST_TYPE);
        promptConfigProviderProperty.setOptions(new ArrayList<>(Arrays.asList(OIDCLoginProtocol.PROMPT_VALUE_LOGIN, OIDCLoginProtocol.PROMPT_VALUE_SELECT_ACCOUNT)));
        promptConfigProviderProperty.setHelpText("oauth-prompt-parameter-hlp-txt");
        return promptConfigProviderProperty;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new IdentityProviderAuthenticator();
    }

    @Override
    public Authenticator createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return new IdentityProviderAuthenticator();
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return AttemptedAuthenticator.SINGLETON;  // ignore this authenticator
    }

    @Override
    public void init(Config.Scope config) {
        // NO OP

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NO OP

    }

    @Override
    public void close() {
        // NO OP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.ALL;
    }

    @Override
    public String getArticleId() {
        return null;
    }

}
