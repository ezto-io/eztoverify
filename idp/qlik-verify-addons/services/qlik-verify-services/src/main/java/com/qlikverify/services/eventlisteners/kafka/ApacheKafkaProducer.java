package com.qlikverify.services.eventlisteners.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.services.managers.RealmManager;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ApacheKafkaProducer {

    protected static final Logger logger = Logger.getLogger(ApacheKafkaProducer.class);
    public static void publishEvent(KeycloakSession session, String server, String topic, String value,String userName,String password,String authentication) {
        if (server == null || server.isEmpty() || topic == null || topic.isEmpty()){
            logger.error("Some Kafka configuration values are missing ");
            return;
        }

        if(Boolean.parseBoolean(authentication)){
            if(userName== null || userName.isEmpty()
                    || password ==null || password.isEmpty()){
                logger.error("Some Kafka configuration values are missing ");
                return ;
            }
        }
        //reset thread context
        resetThreadContext();
        // create the producer

        if(!testKakfa(server,topic,userName,password,authentication))
        {
            logger.error("Kafka test is failed so kafka event listener will be disabled");
            disableEventListener(session);
            return;
        }

        KafkaProducer<String, String> producer = new KafkaProducer<>(getProperties(server,userName,password,authentication));
        // create a producer record
        try {
            ProducerRecord<String, String> eventRecord =
                    new ProducerRecord<>(topic, value);
            // send data - asynchronous
            producer.send(eventRecord, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if (e != null) {
                        disableEventListener(session);
                        producer.flush();
                        // flush and close producer
                        producer.close();
                    }
                }
            });
            logger.info("Message added to Kafka topic: "+topic+" successfully");
        }
        catch (Exception err)
        {
            logger.error("Message failed to add in kafka topic: "+topic+", so kafka event listener will be disabled");
            disableEventListener(session);
        }
        finally {
            producer.flush();
            // flush and close producer
            producer.close();
        }
    }

    private static void resetThreadContext() {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);
            Thread.currentThread().setContextClassLoader(KafkaProducer.class.getClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    public static Properties getProperties(String server,String userName,String password,String authentication) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.RETRIES_CONFIG, 0);
        properties.put("default.api.timeout.ms", 10000);
        properties.put("reconnect.backoff.max.ms",0);
        properties.put("reconnect.backoff.ms",0);
        properties.put("retry.backoff.ms",0);
        if(Boolean.parseBoolean(authentication)) {
            properties.put("sasl.jaas.config", createSaslJassConfig(userName, password));
            properties.put("security.protocol", "SASL_PLAINTEXT");
            properties.put("sasl.mechanism", "PLAIN");
        }
        return properties;
    }

    private static void disableEventListener(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession session) {
                RealmEventsConfigRepresentation rnconfig = ModelToRepresentation.toEventsConfigReprensetation(realm);
                if (rnconfig.getEventsListeners().size() > 0) {
                    rnconfig.getEventsListeners().remove(KafkaEventListenerProviderFactory.ID);
                    new RealmManager(session).updateRealmEventsConfig(rnconfig, realm);
                }
            }
        });

    }

    public static boolean testKakfa(String server, String topic,String userName,String password,String authentication) {
        Properties properties = getProperties(server,userName,password,authentication);
        properties.put("connections.max.idle.ms", 10000);
        properties.put("request.timeout.ms", 5000);
        logger.info("testing server is "+server+" topic is "+topic);
        try (AdminClient client = KafkaAdminClient.create(properties)) {
            ListTopicsResult topics = client.listTopics();
            Set<String> names = topics.names().get();
            return !names.isEmpty() && names.contains(topic);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Kafka testing is failed ");
            return false;
        }
        catch (Exception err)
        {
            logger.error("Kafka testing is failed ");
            return false;
        }
    }

    private static String createSaslJassConfig(String username,String password){
        return "org.apache.kafka.common.security.plain.PlainLoginModule required username='"+username+"' password='"+password+"';";
    }
}
