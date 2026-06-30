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

package com.qlikverify.services.eventlisteners.splunk;

import com.qlikverify.services.eventlisteners.splunk.models.SplunkTestEvent;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SplunkEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "splunk-streamer";
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);
    private static final Logger logger = Logger.getLogger(SplunkEventListenerProviderFactory.class);

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new SplunkEventListenerProvider(session, this);
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
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "splunk-title";
    }

    @Override
    public String getLogo() {
        return "splunk.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL+"/Setup-Guide/Audit/Splunk.md";
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
    public boolean testConfig(KeycloakSession session, Map<String, String> config) {
        try {
            if (config.get("SPLUNK_DOMAIN") == null || config.get("SPLUNK_TOKEN") == null)
                return false;

            HttpUtility httpUtility = new HttpUtility();
            SplunkTestEvent splunkTestEvent = new SplunkTestEvent( session.getContext().getRealm().getName(),"ezto auth Test Event");

            return SplunkProducer.test(session.getContext().getRealm(), httpUtility, config, JsonSerialization.writeValueAsString(splunkTestEvent));
        } catch (IOException e) {
            logger.error(e);
            return false;
        }
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public String getHelpText() {
        return "splunk-hlp-txt";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SPLUNK_DOMAIN");
        configProperty.setLabel("splunk-domain-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("splunk-domain-hlp-txt");
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SPLUNK_TOKEN");
        configProperty.setLabel("splunk-token-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("splunk-token-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SPLUNK_PORT");
        configProperty.setLabel("splunk-port-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("splunk-port-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SEND_ADMIN_EVENT");
        configProperty.setLabel("send-admin-events-label");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("send-admin-events-hlp-txt");

        configProperties.add(configProperty);

        String variant = EnvironmentReader.getValue("VARIANT");
        boolean isEztoVrfy = ("ezto verify").equalsIgnoreCase(variant);
        List<String> enumNames = Stream.of(EventType.values())
                .filter(x -> isEztoVrfy ?
                        (x.name().startsWith("EZ_") || x.name().startsWith("NOTIFICATION")) :
                        !x.name().startsWith("EZ_"))
                .map(Enum::name)
                .collect(Collectors.toList());
        configProperty = new ProviderConfigProperty();
        configProperty.setName("SET_EVENTS");
        configProperty.setLabel("events-types-label");
        configProperty.setOptions(enumNames);
        configProperty.setType(ProviderConfigProperty.MULTIVALUED_LIST_TYPE);
        configProperty.setHelpText("event-types-splunk-hlp-txt");

        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"ciam", "wiam"};
    }
}
