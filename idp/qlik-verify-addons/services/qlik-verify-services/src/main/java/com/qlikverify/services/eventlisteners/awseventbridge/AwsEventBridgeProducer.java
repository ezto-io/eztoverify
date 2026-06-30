package com.qlikverify.services.eventlisteners.awseventbridge;


import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.services.managers.RealmManager;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AwsEventBridgeProducer {
    protected static final Logger logger = Logger.getLogger(AwsEventBridgeProducer.class);

    public static void publishEvent(KeycloakSession session, Map<String, String> config, String eventType, String payload) {

        resetThreadContext();
        EventBridgeClient eventBrClient = EventBridgeClient.builder()
                .credentialsProvider(new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return new AwsCredentials() {
                            @Override
                            public String accessKeyId() {
                                return config.get("AWS_ACCESS_ID");
                            }

                            @Override
                            public String secretAccessKey() {
                                return config.get("AWS_SECRET_ACCESS_KEY");
                            }
                        };
                    }
                })
                .region(Region.of(config.get("AWS_REGION")))
                .build();

        try {

            PutEventsRequestEntry reqEntry = PutEventsRequestEntry.builder()
                    .source(session.getContext().getRealm().getName() + ".qlikverify.com")
                    .detailType(eventType)
                    .detail(payload)
                    .eventBusName(config.get("EVENT_BUS_NAME"))
                    .build();

            // Add the PutEventsRequestEntry to a list
            List<PutEventsRequestEntry> list = new ArrayList<>();
            list.add(reqEntry);

            PutEventsRequest eventsRequest = PutEventsRequest.builder()
                    .entries(reqEntry)
                    .build();

            PutEventsResponse result = eventBrClient.putEvents(eventsRequest);

            for (PutEventsResultEntry resultEntry : result.entries()) {
                if (resultEntry.eventId() != null) {
                    logger.info("Event Id: " + resultEntry.eventId());
                } else {
                    logger.info("Injection failed with Error Code: " + resultEntry.errorCode());
                }
            }

        } catch (EventBridgeException e) {
            disableEventListener(session);
        } catch (Exception err) {
            disableEventListener(session);
        } finally {
            eventBrClient.close();
        }

    }

    private static void disableEventListener(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        RealmEventsConfigRepresentation rnconfig = ModelToRepresentation.toEventsConfigReprensetation(realm);
        if (rnconfig.getEventsListeners().size() > 0) {
            rnconfig.getEventsListeners().remove(AwsEventBridgeEventListenerProviderFactory.ID);
            new RealmManager(session).updateRealmEventsConfig(rnconfig, realm);
        }
    }

    private static void resetThreadContext() {
        Thread.currentThread().setContextClassLoader(null);
    }

    public static boolean test(RealmModel realm, Map<String, String> config) {
        EventBridgeClient eventBrClient = EventBridgeClient.builder()
                .credentialsProvider(new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return new AwsCredentials() {
                            @Override
                            public String accessKeyId() {
                                return config.get("AWS_ACCESS_ID");
                            }

                            @Override
                            public String secretAccessKey() {
                                return config.get("AWS_SECRET_ACCESS_KEY");
                            }
                        };
                    }
                })
                .region(Region.of(config.get("AWS_REGION")))
                .build();

        try {
            PutEventsRequestEntry reqEntry = PutEventsRequestEntry.builder()
                    .source(realm.getName() + ".qlikverify.com")
                    .detailType("test_event")
                    .detail("{ \"tenant\": "+realm.getName()+", \"type\": \"test\" , \"message\":\"ezto auth Test Event\"}")
                    .eventBusName(config.get("EVENT_BUS_NAME"))
                    .build();

            // Add the PutEventsRequestEntry to a list
            List<PutEventsRequestEntry> list = new ArrayList<>();
            list.add(reqEntry);

            PutEventsRequest eventsRequest = PutEventsRequest.builder()
                    .entries(reqEntry)
                    .build();

            PutEventsResponse result = eventBrClient.putEvents(eventsRequest);

            for (PutEventsResultEntry resultEntry : result.entries()) {
                if (resultEntry.eventId() != null)
                    throw new Exception("Injection failed with Error Code: " + resultEntry.errorCode());
            }
            return true;
        } catch (EventBridgeException e) {
            logger.error(e.awsErrorDetails().errorMessage());
            return false;
        } catch (Exception err) {
            return false;
        } finally {
            eventBrClient.close();
        }
    }
}
