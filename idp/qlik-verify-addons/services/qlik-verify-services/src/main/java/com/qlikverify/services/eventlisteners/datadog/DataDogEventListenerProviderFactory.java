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

package com.qlikverify.services.eventlisteners.datadog;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.qlikverify.services.constants.Constants.AUTHORIZATION;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DataDogEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "datadog-streamer";
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new DataDogEventListenerProvider(session, this);
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
        return "datadog-title";
    }

    @Override
    public String getLogo() {
        return "datadog.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/Audit/DataDog.md";
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
        if (config.get(AUTHORIZATION) == null)
            return false;

        return DataDogProducer.test(session.getContext().getRealm(), config.get("REGION"), config.get(AUTHORIZATION));
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public String getHelpText() {
        return "datadog-hlp-txt";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty configProperty;


        configProperty = new ProviderConfigProperty();
        configProperty.setName("AUTHORIZATION");
        configProperty.setLabel("api-key-label");
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setHelpText("api-key-datadog-hlp-txt");
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        List<String> regions = new ArrayList<>();
        regions.add("EU");
        regions.add("US");
        regions.add("US3");
        regions.add("US1-FED");
        configProperty = new ProviderConfigProperty();
        configProperty.setName("REGION");
        configProperty.setLabel("region-label");
        configProperty.setOptions(regions);
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("datadog-region-hlp-txt");

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
        configProperty.setHelpText("events-types-hlp-txt");

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
