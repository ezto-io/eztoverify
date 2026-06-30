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

package com.qlikverify.services.emails.smtp;

import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.email.*;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qlikverify.services.constants.Constants.ENABLE_AUTH;

public class SmtpEmailSenderProviderFactory implements EmailSenderProviderFactory {

    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new SmtpEmailSenderProvider(session,this);
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
        return "smtp";
    }

    @Override
    public String getDisplayName() {
        return "smtp-title";
    }

    @Override
    public String getProviderClass() {
        return (EmailSenderProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "smtp.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/Email-Provider/SMTP-Provider.md";
    }

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isRequiredTestConfigDetails() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        return GeneralHelper.validateConfig(getConfigProperties(session), config);
    }

    @Override
    public ConfigTestResponse testConfig(KeycloakSession session, UserModel userModel, Map<String, String> config) {
        try {
            session.getProvider(EmailTemplateProvider.class).setRealm(session.getContext().getRealm()).setEmailProviderId(getId()).sendSmtpTestEmail(config, userModel, config.get("emailId"));
            return new ConfigTestResponse(true, null);
        } catch (EmailException e) {
            return new ConfigTestResponse(false, e.getMessage());
        }
        catch (ConfigException e)
        {
            EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(),session,session.getContext().getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return new ConfigTestResponse(false, e.getMessage());
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("HOST");
        configProperty.setLabel("host-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("smtp-host-hlp-txt");
        configProperty.setRequired(true);

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("PORT");
        configProperty.setLabel("port-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("smtp-port-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("FROM_DISPLAY_NAME");
        configProperty.setLabel("from-disp-name-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("from-disp-name-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("FROM");
        configProperty.setLabel("from-label");
        configProperty.setType(ProviderConfigProperty.EMAIL);
        configProperty.setHelpText("smtp-sender-email-addr");
        configProperty.setRequired(true);

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("REPLY_TO_DISPLAY_NAME");
        configProperty.setLabel("reply-to-disp-name-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("reply-to-disp-name-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("REPLY_TO");
        configProperty.setLabel("reply-to-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("reply-to-label-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("ENVELOPE_FROM");
        configProperty.setLabel("envelop-from-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("sender-envelop-email-addr-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("ENABLE_SSL");
        configProperty.setLabel("enable-ssl-label");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("ENABLE_STARTTLS");
        configProperty.setLabel("enable-start-tls-label");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ENABLE_AUTH);
        configProperty.setLabel("enable-auth-label");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("USERNAME");
        configProperty.setLabel("username-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("smtp-login-username-hlp-txt");
        configProperty.setRequired(true);
        Map<String, Object> usernameDependsOn = new HashMap<>();
        usernameDependsOn.put(ENABLE_AUTH,true);
        configProperty.setDependsOn(usernameDependsOn);

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("PASSWORD");
        configProperty.setLabel("password-label");
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setHelpText("smtp-login-password-hlp-txt");
        configProperty.setSecret(true);
        configProperty.setRequired(true);
        Map<String, Object> passwordDependsOn = new HashMap<>();
        passwordDependsOn.put(ENABLE_AUTH,true);
        configProperty.setDependsOn(passwordDependsOn);

        configProperties.add(configProperty);
    }
}
