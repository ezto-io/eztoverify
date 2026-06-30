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

package com.qlikverify.services.eventlisteners.kafka;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.qlikverify.services.constants.Constants.AUTHENTICATION;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "kafka-streamer";
    protected static final Logger logger = Logger.getLogger(KafkaEventListenerProviderFactory.class);
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new KafkaEventListenerProvider(session,this);
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
        return "apache-kafka-title";
    }

    @Override
    public String getLogo() {
        return "kafka.png";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL+"/Setup-Guide/Audit/Apache-Kafka.md";
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
        String kafkaServer = config.get("KAFKA_SERVER");
        String kafkaTopic = config.get("KAFKA_TOPIC");
        String userName = config.get("USER_NAME");
        String password = config.get("PASSWORD");
        String authentication = config.get(AUTHENTICATION);
        if (kafkaServer == null || kafkaServer.isEmpty() || kafkaTopic == null || kafkaTopic.isEmpty()){
            logger.error("Some Kafka configuration values are missing ");
            return false;
        }
        if(Boolean.parseBoolean(authentication) && (userName== null || userName.isEmpty()
                || password ==null || password.isEmpty())){
                logger.error("Some Kafka configuration values are missing ");
                return false;
        }
        return ApacheKafkaProducer.testKakfa(kafkaServer, kafkaTopic, userName, password, authentication);
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public String getHelpText() {
        return "apache-kafka-hlp-txt";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("KAFKA_SERVER");
        configProperty.setLabel("kafka-bootstrap-server-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("kafka-bootstrap-server-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("KAFKA_TOPIC");
        configProperty.setLabel("kafka-topic-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("kafka-topic-hlp-txt");
        configProperty.setRequired(true);

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SEND_ADMIN_EVENT");
        configProperty.setLabel("send-admin-events-label");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("send-admin-events-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("KAFKA_ADMIN_TOPIC");
        configProperty.setLabel("kafka-admin-event-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("kafka-admin-event-hlp-txt");
        configProperty.setRequired(true);
        Map<String, Object> kafkaAdminTopicDependsOn = new HashMap<>();
        kafkaAdminTopicDependsOn.put("SEND_ADMIN_EVENT",true);
        configProperty.setDependsOn(kafkaAdminTopicDependsOn);
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

        configProperty = new ProviderConfigProperty();
        configProperty.setName(AUTHENTICATION);
        configProperty.setLabel("authentication-label");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("authentication-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("USER_NAME");
        configProperty.setLabel("username-or-api-key-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("username-or-api-key-hlp-txt");
        configProperty.setRequired(true);
        Map<String, Object> usernameDependsOn = new HashMap<>();
        usernameDependsOn.put(AUTHENTICATION,true);
        configProperty.setDependsOn(usernameDependsOn);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("PASSWORD");
        configProperty.setLabel("password-or-api-secret-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("password-or-api-secret-hlp-txt");
        configProperty.setRequired(true);
        Map<String, Object> passwordDependsOn = new HashMap<>();
        passwordDependsOn.put(AUTHENTICATION,true);
        configProperty.setDependsOn(passwordDependsOn);
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
