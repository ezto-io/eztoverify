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

package com.qlikverify.services.eventlisteners.awseventbridge;

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

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AwsEventBridgeEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "aws-eventbridge-streamer";

    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new AwsEventBridgeEventListenerProvider(session, this);
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
        return "aws-eventbridge-title";
    }

    @Override
    public String getLogo() {
        return "aws.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL+ "/Setup-Guide/Audit/AWS-EventBridge.md";
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
    public boolean testConfig(KeycloakSession session,Map<String, String> config) {
        if (config.get("AWS_ACCESS_ID") == null || config.get("AWS_SECRET_ACCESS_KEY") == null || config.get("EVENT_BUS_NAME") == null || config.get("AWS_REGION") == null)
            return false;

        return AwsEventBridgeProducer.test(session.getContext().getRealm(),config);
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public String getHelpText() {
        return "aws-eventbridge-hlp-txt";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("AWS_ACCESS_ID");
        configProperty.setLabel("aws-access-key-id-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("aws-access-key-id-hlp-txt");
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("AWS_SECRET_ACCESS_KEY");
        configProperty.setLabel("aws-secret-access-key-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("aws-secret-access-key-hlp-txt");
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("EVENT_BUS_NAME");
        configProperty.setLabel("event-bus-name-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("event-bus-name-hlp-txt");
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        List<String> regions = new ArrayList<>();
        regions.add("us-east-2");
        regions.add("us-east-1");
        regions.add("us-west-1");
        regions.add("us-west-2");
        regions.add("af-south-1");
        regions.add("ap-east-1");
        regions.add("ap-south-1");
        regions.add("ap-northeast-3");
        regions.add("ap-northeast-2");
        regions.add("ap-southeast-1");
        regions.add("ap-southeast-2");
        regions.add("ap-northeast-1");
        regions.add("ca-central-1");
        regions.add("cn-north-1");
        regions.add("cn-northwest-1");
        regions.add("eu-central-1");
        regions.add("eu-west-1");
        regions.add("eu-west-2");
        regions.add("eu-south-1");
        regions.add("eu-west-3");
        regions.add("eu-north-1");
        regions.add("me-south-1");
        regions.add("sa-east-1");
        regions.add("us-gov-east-1");
        regions.add("us-gov-west-1");
        configProperty = new ProviderConfigProperty();
        configProperty.setName("AWS_REGION");
        configProperty.setLabel("aws-region-label");
        configProperty.setOptions(regions);
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("aws-region-hlp-txt");



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
        return new String[]{"bo", "ciam", "wiam"};
    }
}
