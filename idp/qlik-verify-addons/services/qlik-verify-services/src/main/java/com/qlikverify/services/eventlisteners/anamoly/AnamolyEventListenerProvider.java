package com.qlikverify.services.eventlisteners.anamoly;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.*;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.Urls;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.qlikverify.services.constants.Constants.EMAIL_SENDER;

public class AnamolyEventListenerProvider implements EventListenerProvider {
    KeycloakSession session;
    AnamolyEventListenerProviderFactory factory;
    private static final Logger logger = Logger.getLogger(AnamolyEventListenerProvider.class);
    private static Executor executor = Executors.newCachedThreadPool();

    private static int year = Calendar.getInstance().get(Calendar.YEAR);

    public AnamolyEventListenerProvider(KeycloakSession session, AnamolyEventListenerProviderFactory factory) {
        this.session = session;
        this.factory = factory;
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        boolean anaomalyDetected = false;
        Map<String, String> args = new HashMap<>();
        args.put("ip", event.getIpAddress());
        args.put("ip_loc", session.getContext().getConnection().getClientLocation().getLocation());
        args.put("browser", event.getDetails().get("browser"));
        args.put("os", event.getDetails().get("os"));

        if (event.getType() == EventType.LOGIN && event.getUser() != null) {
            String configValue = config.get("DIFF_IP_TRIGGER");
            if (configValue != null) {
                boolean isIpTriggerEnabled = Boolean.parseBoolean(configValue);
                if (isIpTriggerEnabled) {
                    String ip = checkIp(session, event, config);
                    if (ip != null) anaomalyDetected = true;
                }
            }

            configValue = config.get("DIFF_BROWSER_TRIGGER");
            if (configValue != null) {
                boolean isBrowserTriggerEnabled = Boolean.parseBoolean(configValue);
                if (isBrowserTriggerEnabled) {
                    String browser = checkBrowser(session, event, config);
                    if (browser != null) anaomalyDetected = true;
                }
            }

            configValue = config.get("DIFF_OS_TRIGGER");
            if (configValue != null) {
                boolean isOSTriggerEnabled = Boolean.parseBoolean(configValue);
                if (isOSTriggerEnabled) {
                    String os = checkOS(session, event, config);
                    if (os != null) anaomalyDetected = true;
                }
            }
        }

        if (anaomalyDetected) {
            logger.info("Sending anomaly detection mail");
            sendEmail(session, "unusual-activity", event, config, args);
        }
    }

    private String checkOS(KeycloakSession session, Event event, Map<String, String> config) {
        String os = event.getDetails().get("os");
        if (os == null || os.isEmpty())
            return null;

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(session.getContext().getRealm().getId());
        int maxResults = config.get("ANOMALY_TRIIGER_COUNT") != null ? Integer.valueOf(config.get("ANOMALY_TRIIGER_COUNT")) : 2;

        query.user(event.getUser().getId());
        query.type(EventType.LOGIN);
        query.maxResults(maxResults);

        List<String> eventRepresentations = query.getResultStream().map(ModelToRepresentation::toRepresentation).map(x -> x.getDetails().get("os")).filter(x -> x.equals(os)).collect(Collectors.toList());

        if (eventRepresentations.contains(os)) {
            return null;
        }

        if (!eventRepresentations.isEmpty())
            return null;

        logger.info("Anamoly : OS : triggered");

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("os", os);
        return os;
    }

    private String checkBrowser(KeycloakSession session, Event event, Map<String, String> config) {
        String browser = event.getDetails().get("browser");
        if (browser == null || browser.isEmpty()) {
            return null;
        }

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(session.getContext().getRealm().getId());
        int maxResults = config.get("ANOMALY_TRIIGER_COUNT") != null ? Integer.valueOf(config.get("ANOMALY_TRIIGER_COUNT")) : 2;

        query.user(event.getUser().getId());
        query.type(EventType.LOGIN);
        query.maxResults(maxResults);

        List<String> eventRepresentations = query.getResultStream().map(ModelToRepresentation::toRepresentation).map(x -> x.getDetails().get("browser")).filter(x -> x.equals(browser)).collect(Collectors.toList());

        if (eventRepresentations.contains(browser))
            return null;

        if (!eventRepresentations.isEmpty())
            return null;

        logger.info("Anamoly : Browser : triggered");

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("browser", browser);
        return browser;
    }

    private String checkIp(KeycloakSession session, Event event, Map<String, String> config) {
        String ipAddress = event.getIpAddress();
        if (ipAddress == null || ipAddress.isEmpty()) {
            logger.error("No IP Address defined");
            return null;
        }

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(session.getContext().getRealm().getId());
        int maxResults = config.get("ANOMALY_TRIIGER_COUNT") != null ? Integer.valueOf(config.get("ANOMALY_TRIIGER_COUNT")) : 2;

        query.user(event.getUser().getId());
        query.type(EventType.LOGIN);
        query.maxResults(maxResults);

        List<String> eventRepresentations = query.getResultStream().map(ModelToRepresentation::toRepresentation).map(x -> x.getIpAddress()).filter(x -> x.equals(ipAddress)).collect(Collectors.toList());

        if (eventRepresentations.contains(ipAddress))
            return null;

        if (!eventRepresentations.isEmpty()) {
            return null;
        }

        logger.info("Anamoly : IP : triggered");
        return event.getIpAddress();
    }

    private void sendEmail(KeycloakSession session, String loginMessage, Event event, Map<String, String> config,
                           Map<String, String> args) {

        String emailProviderId = config.get(EMAIL_SENDER);
        EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
        eventBuilder.client(session.getContext().getAuthenticationSession().getClient().getClientId()).event(EventType.ANAMOLY_DETECTION).success();

        ClientConnection clientConnection = session.getContext().getConnection();
        KeycloakUriInfo keycloakUriInfo = session.getContext().getUri();

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        Resteasy.pushContext(ClientConnection.class, clientConnection);
                        Resteasy.pushContext(KeycloakSession.class, innerSession);
                        Resteasy.pushContext(UriInfo.class, keycloakUriInfo);
                        Resteasy.pushContext(RealmModel.class, session.getContext().getRealm());
                        RealmModel realmModel = session.realms().getRealm(event.getRealm().getId());
                        UserModel userModel = session.users().getUserById(event.getUser().getId(), session.getContext().getRealm());
                        innerSession.getContext().setRealm(session.getContext().getRealm());
                        innerSession.getContext().setAuthenticationSession(session.getContext().getAuthenticationSession());

                        try {
                            String resetUrl = "#";
                            try {
                                if (session.getContext().getUri() != null) {
                                    resetUrl = Urls.loginResetCredentials(addQueryParams(keycloakUriInfo.getBaseUri(), event), realmModel.getName()).toString();
                                }
                            } catch (Exception err) {
                                logger.error(err);
                            }

                            // Creating date from milliseconds
                            // using Date() constructor
                            LocalDateTime localDateTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(System.currentTimeMillis()),
                                    ZoneId.of(clientConnection != null ? clientConnection.getClientLocation().getBody().getZoneId() : ZoneId.systemDefault().getId())
                            );
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy hh.mm a");
                            boolean isAdminRealm = session.realms().getRealmByName(Config.getAdminRealm()).getName().equals(realmModel.getName());

                            Map<String, Object> attributes = new HashMap<>();
                            attributes.put("login_message", loginMessage);
                            attributes.put("user_name", userModel.getUsername());
                            attributes.put("date_and_time", localDateTime.format(formatter));
                            attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                            attributes.put("workspaceName", session.getContext().getRealm().getName());
                            if ((realmModel.getMode().equals("bo") && isAdminRealm) || (!realmModel.getMode().equals("bo"))) {
                                attributes.put("reset_password", resetUrl);
                            }
                            attributes.put("year", String.valueOf(year));
                            if (args != null) {
                                attributes.putAll(args);
                            }
                            if (userModel == null)
                                return;

                            if (!userModel.isEnabled())
                                return;

                            if (userModel.getEmail() == null)
                                return;

                            if (!userModel.isEmailVerified())
                                return;

                            innerSession.getProvider(EmailTemplateProvider.class)
                                    .setRealm(session.getContext().getRealm())
                                    .setEmailProviderId(emailProviderId)
                                    .send(userModel.getEmail(), "anomalySubject", "anomaly-detection.ftl", attributes);
                            logger.info("Anamonly Email triggered for the user " + userModel.getId());
                        } catch (EmailException e) {
                            logger.error(e);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                });

            }
        }, executor);

    }

    private URI addQueryParams(URI requestURI, Event event) {
        UriBuilder uriBuilder = UriBuilder.fromUri(requestURI);

        if (event.getClient().getClientId() != null) {
            uriBuilder.queryParam(Constants.CLIENT_ID, event.getClient().getClientId());
        }

        return uriBuilder.build();
    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        //Here we dont react to the admin events
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }

    @Override
    public void close() {
        // NOP
    }
}
