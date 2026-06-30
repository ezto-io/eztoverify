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

package org.keycloak.events.log;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.LDAPConstants;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProvider;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JBossLoggingEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "jboss-logging";

    private static final Logger logger = Logger.getLogger("org.keycloak.events");

    private Logger.Level successLevel;
    private Logger.Level errorLevel;
    private boolean sanitize;
    private Character quotes;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new JBossLoggingEventListenerProvider(session, logger, successLevel, errorLevel,this, quotes, sanitize);
    }

    @Override
    public void init(Config.Scope config) {
        successLevel = Logger.Level.valueOf(config.get("success-level", "debug").toUpperCase());
        errorLevel = Logger.Level.valueOf(config.get("error-level", "warn").toUpperCase());
        sanitize = config.getBoolean("sanitize", true);
        String quotesString = config.get("quotes", "\"");
        if (!quotesString.equals("none") && quotesString.length() > 1) {
            logger.warn("Invalid quotes configuration, it should be none or one character to use as quotes. Using default \" quotes");
            quotesString = "\"";
        }
        quotes = quotesString.equals("none")? null : quotesString.charAt(0);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Console Logging";
    }

    @Override
    public String getLogo() {
        return null;
    }

    @Override
    public String getHowTodo() {
        return null;
    }

    @Override
    public boolean isInBuilt() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean isRequiredTestConfigDetails() {
        return false;
    }

    @Override
    public boolean testConfig(KeycloakSession session,Map<String, String> config) {
        return false;
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public String getHelpText() {
        return null;
    }

    protected static final List<ProviderConfigProperty> configProperties;

    static {
        configProperties = getConfigProps();
    }

    private static List<ProviderConfigProperty> getConfigProps() {
        return ProviderConfigurationBuilder.create()
                .property().name(KerberosConstants.KERBEROS_REALM)
                .label("kerberos-realm")
                .helpText("kerberos-realm.tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(KerberosConstants.SERVER_PRINCIPAL)
                .label("server-principal")
                .helpText("server-principal.tooltip")
                .defaultValue("hello")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(KerberosConstants.KEYTAB)
                .label("keytab")
                .helpText("keytab.tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(KerberosConstants.DEBUG)
                .label("debug")
                .helpText("debug.tooltip")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false")
                .add()
                .property().name(KerberosConstants.ALLOW_PASSWORD_AUTHENTICATION)
                .label("allow-password-authentication")
                .helpText("allow-password-authentication.tooltip")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false")
                .add()
                .property().name(LDAPConstants.EDIT_MODE)
                .label("edit-mode")
                .helpText("edit-mode.tooltip")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(UserStorageProvider.EditMode.READ_ONLY.toString(), UserStorageProvider.EditMode.UNSYNCED.toString())
                .add()
                .property().name(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN)
                .label("update-profile-first-login")
                .helpText("update-profile-first-login.tooltip")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("false")
                .add()
                .property()
                .name("sanitize")
                .type("boolean")
                .helpText("If true the log messages are sanitized to avoid line breaks. If false messages are not sanitized.")
                .defaultValue("true")
                .add()
                .property()
                .name("quotes")
                .type("string")
                .helpText("The quotes to use for values, it should be one character like \" or '. Use \"none\" if quotes are not needed.")
                .defaultValue("\"")
                .add()
                .build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        String[] logLevels = Arrays.stream(Logger.Level.values())
                .map(Logger.Level::name)
                .map(String::toLowerCase)
                .sorted(Comparator.naturalOrder())
                .toArray(String[]::new);
        return ProviderConfigurationBuilder.create()
                .property()
                .name("success-level")
                .type("string")
                .helpText("The log level for success messages.")
                .options(logLevels)
                .defaultValue("debug")
                .add()
                .property()
                .name("error-level")
                .type("string")
                .helpText("The log level for error messages.")
                .options(logLevels)
                .defaultValue("warn")
                .add()
                .build();
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"bo", "ciam", "wiam"};
    }
}
