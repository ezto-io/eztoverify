package org.keycloak.events.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.utils.AccessKeyManager;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.actiontoken.userinvite.UserInviteLinkActionToken;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.exceptions.LicenseException;
import org.keycloak.models.*;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.util.PkceUtils;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.keycloak.models.Constants.ADMIN_CONSOLE_CLIENT_ID;
import static org.keycloak.protocol.oidc.OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

public class EmailProducer {

    @Context
    protected static HttpHeaders headers;

    private static final Logger log = Logger.getLogger(EmailEventListenerProvider.class);

    private static final Executor executor = Executors.newCachedThreadPool();

    private static final int year = Calendar.getInstance().get(Calendar.YEAR);

    public static void sendAdminWelcomeEmail(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        Resteasy.pushContext(HttpHeaders.class, session.getContext().getRequestHeaders());
                        Resteasy.pushContext(ClientConnection.class, session.getContext().getConnection());
                        Resteasy.pushContext(KeycloakSession.class, session);
                        Resteasy.pushContext(UriInfo.class, session.getContext().getUri());
                        ObjectMapper objectMapper = new ObjectMapper();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        UriInfo uriInfo = session.getContext().getUri();
                        innerSession.getContext().setRealm(realm, session);
                        session.getContext().setRealm(realm, session);
                        UserRepresentation userRepresentation = null;
                        try {
                            userRepresentation = objectMapper.readValue(event.getRepresentation(),
                                    UserRepresentation.class);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        if (userRepresentation == null)
                            return;

                        if (userRepresentation.isSkipSendingEmail()) {
                            log.debug("Email Sending Skipped");
                            return;
                        }

                        UserModel user = innerSession.users().getUserByEmail(realm, userRepresentation.getEmail());
                        if (user != null && user.getEmail() != null) {
                            if (realm.getUserInviteWay().equalsIgnoreCase("otp")) {
                                // get oneTimePassword from representation
                                CredentialRepresentation representation = getPasswordCredentialOf(userRepresentation);
                                // setup OIDC & Workspace URLs
                                ClientModel clientModel = realm.getClientByClientId(
                                        realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                : ADMIN_CONSOLE_CLIENT_ID);
                                String url = ClientResource.getOIDCUrl(clientModel, session, realm, null);
                                String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                                // String url = Urls.ssoPage(session.getContext().getUri().getBaseUri(),
                                // realm.getName
                                // ()).toString();
                                Map<String, Object> attributes = new HashMap<>();
                                attributes.put("tenant", getRealmName(realm));
                                attributes.put("user_name", user.getUsername());
                                attributes.put("to", user.getEmail());
                                attributes.put("isAccCreator",
                                        innerSession.users().getUsersStream(realm).count() == 1 ? true : false);
                                attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                                if ((realm.getName().equalsIgnoreCase(Config.getAdminRealm())
                                        || !realm.getMode().equalsIgnoreCase("bo")) && representation != null) {
                                    attributes.put("temporary_password",
                                             representation.getValue());
                                    attributes.put("temppass",
                                             representation.getValue());
                                }
                                attributes.put("year", String.valueOf(year));
                                attributes.put("link", url);
                                attributes.put("workspace", workspaceUrl);

                                if (innerSession.getContext().getClient() == null) {
                                    innerSession.getContext().setClient(
                                            session.getContext().getRealm().getClientByClientId("dashboard"));
                                }

                                try {
                                    innerSession.getProvider(EmailTemplateProvider.class)
                                            .setRealm(realm)
                                            .send(user.getEmail(), "welcomeAdminSubject", "welcome-admin-email.ftl",
                                                    attributes);
                                } catch (EmailException e) {
                                    e.printStackTrace();
                                } catch (ConfigException e) {
                                    EventBuilder eventBuilder = new EventBuilder(realm, session,
                                            session.getContext().getConnection());
                                    eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                    eventBuilder.error(e.getMessage());
                                }
                            } else {
                                session.getContext().setRealm(realm);
                                int validityInSecs = realm
                                        .getActionTokenGeneratedByUserLifespan(UserInviteLinkActionToken.TOKEN_TYPE);
                                int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
                                URI uriInf = UriBuilder.fromUri(ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth")
                                        .build();
                                UserInviteLinkActionToken token = new UserInviteLinkActionToken(user.getId(),
                                        user.getEmail(), absoluteExpirationInSecs,
                                        realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                : ADMIN_CONSOLE_CLIENT_ID);
                                UriBuilder builder = Urls.actionTokenBuilder(uriInf, token.serialize(session, realm,
                                        ""),
                                        realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                : ADMIN_CONSOLE_CLIENT_ID,
                                        "");
                                String link = builder.build(realm.getName()).toString();
                                String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                                // String url = Urls.ssoPage(session.getContext().getUri().getBaseUri(),
                                // realm.getName
                                // ()).toString();
                                Map<String, Object> attributes = new HashMap<>();
                                attributes.put("tenant", getRealmName(realm));
                                attributes.put("user_name", user.getUsername());
                                attributes.put("to", user.getEmail());
                                attributes.put("year", String.valueOf(year));
                                attributes.put("link", link);
                                attributes.put("link_expiry", formatDuration(validityInSecs));
                                attributes.put("workspace", workspaceUrl);
                                attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                                try {
                                    innerSession.getProvider(EmailTemplateProvider.class)
                                            .setRealm(realm)
                                            .send(user.getEmail(), "welcomeAdminSubject", "welcome-admin-email-link" +
                                                    ".ftl", attributes);
                                } catch (EmailException e) {
                                    log.error(e);
                                } catch (ConfigException e) {
                                    EventBuilder eventBuilder = new EventBuilder(realm, session,
                                            session.getContext().getConnection());
                                    eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                    eventBuilder.error(e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        }, executor);
    }

    public static void sendUserEnrollmentEmail(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        innerSession.getContext().setRealm(realm);
                        UserRepresentation userRepresentation = null;
                        try {
                            userRepresentation = objectMapper.readValue(event.getRepresentation(),
                                    UserRepresentation.class);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        if (userRepresentation == null)
                            return;

                        if (userRepresentation.isSkipSendingEmail()) {
                            log.debug("Email Sending Skipped");
                            return;
                        }

                        UserModel user = innerSession.users().getUserByEmail(realm, userRepresentation.getEmail());
                        if (user != null && user.getEmail() != null) {
                            URI uriInfo = UriBuilder.fromUri(ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth")
                                    .build();
                            int validityInSecs = realm
                                    .getActionTokenGeneratedByUserLifespan(UserInviteLinkActionToken.TOKEN_TYPE);
                            int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
                            UserInviteLinkActionToken token = new UserInviteLinkActionToken(user.getId(),
                                    user.getEmail(), absoluteExpirationInSecs,
                                    realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                            : ADMIN_CONSOLE_CLIENT_ID);
                            UriBuilder builder = Urls.actionTokenBuilder(uriInfo,
                                    token.serialize(innerSession, realm, ""),
                                    realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                            : ADMIN_CONSOLE_CLIENT_ID,
                                    "");
                            String link = builder.build(realm.getName()).toString();
                            String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                            // String url = Urls.ssoPage(session.getContext().getUri().getBaseUri(),
                            // realm.getName())
                            // .toString();
                            Map<String, Object> attributes = new HashMap<>();
                            attributes.put("tenant", getRealmName(realm));
                            attributes.put("user_name", user.getUsername());
                            attributes.put("to", user.getEmail());
                            attributes.put("year", String.valueOf(year));
                            attributes.put("link", link);
                            attributes.put("link_expiry", formatDuration(validityInSecs));
                            attributes.put("workspace", workspaceUrl);
                            attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                            try {
                                innerSession.getProvider(EmailTemplateProvider.class)
                                        .setRealm(realm)
                                        .send(user.getEmail(), "welcomeUserSubject", "welcome-user-email-link.ftl",
                                                attributes);
                            } catch (EmailException e) {
                                log.error(e);
                            } catch (ConfigException e) {
                                EventBuilder eventBuilder = new EventBuilder(realm, session,
                                        session.getContext().getConnection());
                                eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                eventBuilder.error(e.getMessage());
                            }
                        }
                    }
                });
            }
        }, executor);

    }

    public static void sendUserWelcomeEmail(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        Resteasy.pushContext(HttpHeaders.class, session.getContext().getRequestHeaders());
                        Resteasy.pushContext(ClientConnection.class, session.getContext().getConnection());
                        Resteasy.pushContext(KeycloakSession.class, session);
                        Resteasy.pushContext(UriInfo.class, session.getContext().getUri());
                        ObjectMapper objectMapper = new ObjectMapper();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        innerSession.getContext().setRealm(realm);
                        UserRepresentation userRepresentation = null;
                        try {
                            userRepresentation = objectMapper.readValue(event.getRepresentation(),
                                    UserRepresentation.class);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        if (userRepresentation == null)
                            return;

                        if (userRepresentation.isSkipSendingEmail()) {
                            log.debug("Email Sending Skipped");
                            return;
                        }

                        UserModel user = innerSession.users().getUserByEmail(userRepresentation.getEmail(), realm);
                        if (user != null && user.getEmail() != null) {

                            if (realm.getUserInviteWay().equalsIgnoreCase("otp")) {
                                CredentialRepresentation representation = getPasswordCredentialOf(userRepresentation);
                                ClientModel clientModel = realm.getClientByClientId(
                                        realm.getUserInviteClientId() == null
                                                ? realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                        : ADMIN_CONSOLE_CLIENT_ID
                                                : realm.getUserInviteClientId());
                                String url = ClientResource.getOIDCUrl(clientModel, session, realm, null);
                                String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                                Map<String, Object> attributes = new HashMap<>();
                                attributes.put("tenant", getRealmName(realm));
                                attributes.put("user_name", user.getUsername());
                                attributes.put("to", user.getEmail());
                                attributes.put("temporary_password", representation.getValue());
                                attributes.put("temppass", representation.getValue());
                                attributes.put("year", String.valueOf(year));
                                attributes.put("link", url);
                                attributes.put("workspace", workspaceUrl);
                                attributes.put("adminEvent", "true");
                                attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                                try {
                                    innerSession.getProvider(EmailTemplateProvider.class)
                                            .setRealm(realm)
                                            .send(user.getEmail(), "welcomeUserSubject", "welcome-user-email-otp.ftl",
                                                    attributes);
                                } catch (EmailException e) {
                                    log.error(e);
                                } catch (ConfigException e) {
                                    EventBuilder eventBuilder = new EventBuilder(realm, session,
                                            session.getContext().getConnection());
                                    eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                    eventBuilder.error(e.getMessage());
                                }
                            } else {
                                UriInfo uriInfo = session.getContext().getUri();
                                int validityInSecs = realm
                                        .getActionTokenGeneratedByUserLifespan(UserInviteLinkActionToken.TOKEN_TYPE);
                                int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
                                UserInviteLinkActionToken token = new UserInviteLinkActionToken(user.getId(),
                                        user.getEmail(), absoluteExpirationInSecs,
                                        realm.getUserInviteClientId() == null
                                                ? realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                        : ADMIN_CONSOLE_CLIENT_ID
                                                : realm.getUserInviteClientId());
                                String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                                ClientModel clientModel = realm.getClientByClientId(
                                        realm.getUserInviteClientId() == null
                                                ? realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                        : ADMIN_CONSOLE_CLIENT_ID
                                                : realm.getUserInviteClientId());
                                String link = ClientResource.getOIDCUrl(clientModel, session, realm, null);
                                // String url = Urls.ssoPage(session.getContext().getUri().getBaseUri(),
                                // realm.getName
                                // ()).toString();
                                Map<String, Object> attributes = new HashMap<>();
                                attributes.put("tenant", getRealmName(realm));
                                attributes.put("user_name", user.getUsername());
                                attributes.put("to", user.getEmail());
                                attributes.put("year", String.valueOf(year));
                                attributes.put("link", link);
                                attributes.put("link_expiry", formatDuration(validityInSecs));
                                attributes.put("workspace", workspaceUrl);
                                attributes.put("adminEvent", "true");
                                attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                                try {
                                    innerSession.getProvider(EmailTemplateProvider.class)
                                            .setRealm(realm)
                                            .send(user.getEmail(), "welcomeUserSubject", "welcome-user-email-link" +
                                                    ".ftl", attributes);
                                } catch (EmailException e) {
                                    log.error(e);
                                } catch (ConfigException e) {
                                    EventBuilder eventBuilder = new EventBuilder(realm, session,
                                            session.getContext().getConnection());
                                    eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                    eventBuilder.error(e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        }, executor);

    }

    public static void sendTempPassword(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation, EmailTemplateProvider emailTemplateProvider) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        UserRepresentation userRepresentation = null;
                        try {
                            userRepresentation = objectMapper.readValue(event.getRepresentation(),
                                    UserRepresentation.class);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        if (userRepresentation == null)
                            return;

                        if (userRepresentation.isSkipSendingEmail()) {
                            log.debug("Email Sending Skipped");
                            return;
                        }

                        UserModel user = innerSession.users().getUserByEmail(userRepresentation.getEmail(), realm);
                        if (user != null && user.getEmail() != null) {

                            CredentialRepresentation representation = getPasswordCredentialOf(userRepresentation);

                            String url = formOIDCUrl(session, realm,
                                    session.getContext().getUri().getBaseUri().toString(),
                                    userRepresentation.firstAttribute(Constants.CLIENT_ID));
                            Map<String, Object> attributes = new HashMap<>();
                            attributes.put("tenant", getRealmName(realm));
                            attributes.put("user_name", user.getUsername());
                            attributes.put("to", user.getEmail());
                            attributes.put("link", url);
                            attributes.put("temporary_password", representation.getValue());
                            attributes.put("temppass", representation.getValue());
                            attributes.put("year", String.valueOf(year));
                            attributes.put("adminEvent", "true");
                            attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

                            if (innerSession.getContext().getClient() == null) {
                                innerSession.getContext()
                                        .setClient(session.getContext().getRealm().getClientByClientId("dashboard"));
                            }

                            try {
                                emailTemplateProvider
                                        .setRealm(realm)
                                        .send(user.getEmail(), "resetPasswordSubject", "reset-password-email.ftl",
                                                attributes);
                            } catch (EmailException e) {
                                log.error(e);
                            } catch (ConfigException e) {
                                EventBuilder eventBuilder = new EventBuilder(realm, session,
                                        session.getContext().getConnection());
                                eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                eventBuilder.error(e.getMessage());
                            }
                        }
                    }
                });
            }
        }, executor);

    }

    public static void sendToManageAccountLambda(KeycloakSession session, AdminEvent event,
            Map<String, String> config, boolean includeRepresentation,
            EmailTemplateProvider emailTemplateProvider) {
        // Bypassed license management actions
    }

    public static void sendHardDeleteOtpEmail(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation,
            EmailTemplateProvider emailTemplateProvider) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            RealmModel realm = innerSession.realms().getRealmByName(Config.getAdminRealm());
                            Map<String, Object> rep = objectMapper.readValue(event.getRepresentation(), Map.class);
                            rep.put("year", String.valueOf(year));
                            rep.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                            rep.put("supportLink", EnvironmentReader.getValue("SUPPORT_LINK", "https://support.ezto.io/support/home"));
                            if (rep.get("user_name") == null) {
                                rep.put("user_name", String.valueOf(rep.get("to")));
                            }
                            try {
                                emailTemplateProvider
                                        .setRealm(realm)
                                        .send(String.valueOf(rep.get("to")), "hardDeleteReqSubject", "hard-delete-req" +
                                                ".ftl", rep);
                            } catch (EmailException e) {
                                log.error(e);
                            } catch (ConfigException e) {
                                EventBuilder eventBuilder = new EventBuilder(realm, session,
                                        session.getContext().getConnection());
                                eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                eventBuilder.error(e.getMessage());
                            }
                        } catch (JsonProcessingException e) {
                            log.error(e);
                        }
                    }
                });
            }
        }, executor);
    }

    public static void sendHardDeleteSuccessEmail(KeycloakSession session, AdminEvent event,
            Map<String, String> config, boolean includeRepresentation,
            EmailTemplateProvider emailTemplateProvider) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            RealmModel realm = innerSession.realms()
                                    .getRealm(event.getAuthDetails().getRealm().getId());
                            Map<String, Object> rep = objectMapper.readValue(event.getRepresentation(), Map.class);
                            rep.put("year", String.valueOf(year));
                            rep.put("supportLink", EnvironmentReader.getValue("SUPPORT_LINK", "https://support.ezto.io/support/home"));
                            rep.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                            if (rep.get("user_name") == null) {
                                rep.put("user_name", String.valueOf(rep.get("to")));
                            }
                            if (rep != null && rep.get("to") != null) {
                                try {
                                    session.getProvider(EmailTemplateProvider.class)
                                            .setRealm(realm)
                                            .send(String.valueOf(rep.get("to")), "hardDeleteSuccessSubject", "hard" +
                                                    "-delete-success.ftl", rep);
                                } catch (EmailException e) {
                                    log.error(e);
                                } catch (ConfigException e) {
                                    EventBuilder eventBuilder = new EventBuilder(realm, session,
                                            session.getContext().getConnection());
                                    eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                    eventBuilder.error(e.getMessage());
                                }
                                // if the loggedInUser is not a accountCreator,then we notify accountCreator
                                if (!Boolean.parseBoolean(String.valueOf(rep.get("isAccCreator")))) {
                                    String acc_creator_email = String.valueOf(rep.get("acc_creator_email"));
                                    if (!acc_creator_email.equals("null")) {
                                        rep.put("emlForAccCreator", true);
                                        try {
                                            session.getProvider(EmailTemplateProvider.class)
                                                    .setRealm(realm)
                                                    .send(acc_creator_email, "hardDeleteSuccessSubject", "hard-delete" +
                                                            "-success.ftl", rep);
                                        } catch (EmailException e) {
                                            log.error(e);
                                        } catch (ConfigException e) {
                                            EventBuilder eventBuilder = new EventBuilder(realm, session,
                                                    session.getContext().getConnection());
                                            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                            eventBuilder.error(e.getMessage());
                                        }
                                    }
                                }
                            }
                        } catch (JsonProcessingException e) {
                            log.error(e);
                        }

                    }
                });
            }
        }, executor);
    }

    public static void sendDNSProcessedEmail(KeycloakSession session, Event event, Map<String, String> config) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        String mailSubject = "";
                        Map<String, String> eventConfig = event.getDetails();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealm().getId());
                        innerSession.getContext().setRealm(realm);

                        // getting user from eventDetails whom triggered custom domain
                        UserModel triggeredUser = innerSession.users().getUserByEmail(realm, eventConfig.get(
                                "triggererEmail"));

                        // setup common attributes for email
                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("tenant", getRealmName(realm));
                        attributes.put("to", triggeredUser.getEmail());
                        attributes.put("user_name", triggeredUser.getUsername());
//                        attributes.put("old_domain", ModelToRepresentation.getWorkspaceUrl(session, realm));
                        attributes.put("new_domain", "https://" + eventConfig.get("newDomain"));
                        attributes.put("year", String.valueOf(year));
                        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

                        if (triggeredUser != null && triggeredUser.getEmail() != null) {
                            if (event.getType() == EventType.CUSTOM_DOMAIN_SUCCESS) {
                                mailSubject = "customDomainSuccessSubject";
                                attributes.put("isDNSSet", true);
                            }

                            if (event.getType() == EventType.CUSTOM_DOMAIN_FAILURE) {
                                mailSubject = "customDomainFailureSubject";
                                attributes.put("isDNSSet", false);
                            }
                        }

                        try {
                            innerSession.getProvider(EmailTemplateProvider.class)
                                    .setRealm(realm)
                                    .setUser(triggeredUser)
                                    .send(triggeredUser.getEmail(), mailSubject, "custom-domain-status.ftl",
                                            attributes);
                        } catch (EmailException e) {
                            log.error(e);
                        } catch (ConfigException e) {
                            EventBuilder eventBuilder = new EventBuilder(realm, session,
                                    session.getContext().getConnection());
                            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                            eventBuilder.error(e.getMessage());
                        }
                    }
                });
            }
        }, executor);

    }

    public static void sendSyncSuccessEMail(KeycloakSession session, Event event, Map<String, String> config) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        Map<String, String> eventConfig = event.getDetails();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealm().getId());
                        innerSession.getContext().setRealm(realm);
                        UserModel triggeredUser = innerSession.users().getUserById(eventConfig.get("triggeredUserId"),
                                realm);

                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("tenant", getRealmName(realm));
                        attributes.put("connectionId",event.getClient().getId());
                        attributes.put("user_name", triggeredUser.getUsername());
                        attributes.put("to", triggeredUser.getEmail());
                        attributes.put("year", String.valueOf(year));
                        attributes.put("syncProvider", eventConfig.get("syncProvider"));
                        attributes.put("total_synced", eventConfig.get("totalSync"));
                        attributes.put("added_users", eventConfig.get("importedCnt"));
                        attributes.put("updated_users", eventConfig.get("updatedCnt"));
                        attributes.put("failed_users", eventConfig.get("failed"));
                        attributes.put("adminEvent", "true");
                        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

                        try {
                            innerSession.getProvider(EmailTemplateProvider.class)
                                    .setRealm(realm)
                                    .setUser(triggeredUser)
                                    .send(triggeredUser.getEmail(), "syncSuccessSubject", "adsync-email.ftl",
                                            attributes);
                        } catch (EmailException e) {
                            log.error(e);
                        } catch (ConfigException e) {
                            EventBuilder eventBuilder = new EventBuilder(realm, session,
                                    session.getContext().getConnection());
                            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                            eventBuilder.error(e.getMessage());
                        }
                    }
                });
            }
        }, executor);

    }

    public static void sendGroupInvites(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        HashMap<String, String> configData = new HashMap<>();
                        List<UserModel> uninvitedUsers = new ArrayList<>();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        innerSession.getContext().setRealm(realm);
                        session.getContext().setRealm(realm, session);
                        try {
                            configData = objectMapper.readValue(event.getRepresentation(), HashMap.class);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        String grpId = configData.get("grpId");
                        GroupModel grp = innerSession.groups().getGroupById(realm, grpId);
                        innerSession.users().getGroupMembersStream(realm, grp, -1, -1).forEach(x -> {
                            if ((x.getAttributes().get(UserModel.USER_INVITE_STATUS) == null)
                                    || (x.getAttributes().get(UserModel.USER_INVITE_STATUS) != null
                                            && !(x.getAttributes().get(UserModel.USER_INVITE_STATUS).get(0)
                                                    .equals("Accepted")))) {
                                uninvitedUsers.add(x);
                            }
                        });
                        if (uninvitedUsers != null) {
                            uninvitedUsers
                                    .forEach(user -> {
                                        if (user != null && user.getEmail() != null) {
                                            if (realm.getUserInviteWay().equalsIgnoreCase("otp")) {
                                                // Set oneTimePassword for User
                                                String tempPass = createTempPassword(innerSession, realm, user,
                                                        realm.getMode().equals("wiam")
                                                                ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                                : ADMIN_CONSOLE_CLIENT_ID);
                                                // setup OIDC & Workspace URLs
                                                String url = formOIDCUrl(session, realm,
                                                        ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth/",
                                                        realm.getMode().equals("wiam")
                                                                ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                                : ADMIN_CONSOLE_CLIENT_ID);
                                                String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                                                // String url = Urls.ssoPage(session.getContext().getUri().getBaseUri
                                                // (), realm.getName()).toString();
                                                Map<String, Object> attributes = new HashMap<>();
                                                attributes.put("tenant", getRealmName(realm));
                                                attributes.put("user_name", user.getUsername());
                                                attributes.put("to", user.getEmail());
                                                attributes.put("temporary_password", tempPass);
                                                attributes.put("temppass", tempPass);
                                                attributes.put("year", String.valueOf(year));
                                                attributes.put("link", url);
                                                attributes.put("workspace", workspaceUrl);
                                                attributes.put("adminEvent", "true");
                                                attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto" +
                                                        " verify"));

                                                try {
                                                    innerSession.getProvider(EmailTemplateProvider.class)
                                                            .setRealm(realm)
                                                            .send(user.getEmail(), "welcomeUserSubject", "welcome" +
                                                                    "-user-email-otp.ftl", attributes);
                                                } catch (EmailException e) {
                                                    e.printStackTrace();
                                                } catch (ConfigException e) {
                                                    EventBuilder eventBuilder = new EventBuilder(realm, session,
                                                            session.getContext().getConnection());
                                                    eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                                    eventBuilder.error(e.getMessage());
                                                }
                                            } else {
                                                URI uriInfo = UriBuilder
                                                        .fromUri(ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth")
                                                        .build();
                                                int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(
                                                        UserInviteLinkActionToken.TOKEN_TYPE);
                                                int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
                                                UserInviteLinkActionToken token = new UserInviteLinkActionToken(
                                                        user.getId(), user.getEmail(),
                                                        absoluteExpirationInSecs, realm.getMode().equals(
                                                                "wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                                        : ADMIN_CONSOLE_CLIENT_ID);
                                                UriBuilder builder = Urls.actionTokenBuilder(uriInfo,
                                                        token.serialize(innerSession, realm, ""),
                                                        realm.getMode().equals("wiam")
                                                                ? Constants.SSO_MANAGEMENT_CLIENT_ID
                                                                : ADMIN_CONSOLE_CLIENT_ID,
                                                        "");
                                                String link = builder.build(realm.getName()).toString();
                                                String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                                                // String url = Urls.ssoPage(session.getContext().getUri().getBaseUri
                                                // (), realm.getName()).toString();
                                                Map<String, Object> attributes = new HashMap<>();
                                                attributes.put("tenant", getRealmName(realm));
                                                attributes.put("user_name", user.getUsername());
                                                attributes.put("to", user.getEmail());
                                                attributes.put("year", String.valueOf(year));
                                                attributes.put("link", link);
                                                attributes.put("link_expiry", formatDuration(validityInSecs));
                                                attributes.put("workspace", workspaceUrl);
                                                attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto" +
                                                        " verify"));
                                                try {
                                                    innerSession.getProvider(EmailTemplateProvider.class)
                                                            .setRealm(realm)
                                                            .send(user.getEmail(), "welcomeUserSubject", "welcome" +
                                                                    "-user-email-link.ftl", attributes);
                                                } catch (EmailException e) {
                                                    log.error(e);
                                                    grp.removeAttribute("hidden_ga_grp_invite_started");
                                                } catch (ConfigException e) {
                                                    EventBuilder eventBuilder = new EventBuilder(realm, session,
                                                            session.getContext().getConnection());
                                                    eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                                    eventBuilder.error(e.getMessage());
                                                    grp.removeAttribute("hidden_ga_grp_invite_started");
                                                }
                                            }
                                        }
                                    });
                        }
                        grp.removeAttribute("hidden_ga_grp_invite_started");
                    }
                });
            }
        }, executor);
    }

    public static void sendGroupAssigned(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        ObjectMapper objectMapper = new ObjectMapper();
                        HashMap<String, String> configData = new HashMap<>();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        innerSession.getContext().setRealm(realm);
                        session.getContext().setRealm(realm, session);
                        try {
                            configData = objectMapper.readValue(event.getRepresentation(), HashMap.class);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        UserModel userModel = null;
                        Map<String, String> eventConfig = event.getDetails();
                        boolean isUserAssignedRole = eventConfig == null ? false
                                : eventConfig.get("assigned_user") != null;
                        if (isUserAssignedRole) {
                            userModel = innerSession.users().getUserById(realm, eventConfig.get("assigned_user"));
                        } else {
                            userModel = innerSession.users().getUserById(realm,
                                    event.getAuthDetails().getUser().getId());
                        }
                        String group = configData.get("name");
                        String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("tenant", getRealmName(realm));
                        attributes.put("user_name", userModel.getUsername());
                        attributes.put("to", userModel.getEmail());
                        attributes.put("year", String.valueOf(year));
                        attributes.put("group", group);
                        attributes.put("workspace", workspaceUrl);
                        attributes.put("adminEvent", "true");
                        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

                        try {
                            innerSession.getProvider(EmailTemplateProvider.class)
                                    .setRealm(realm)
                                    .send(userModel.getEmail(), "welcomeUserGroupSubject", "welcome-user-group.ftl",
                                            attributes);
                        } catch (EmailException e) {
                            EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                            eventBuilder.user(userModel);
                            eventBuilder.event(EventType.NOTIFICATION_ERROR);
                            eventBuilder.error("User cannot be notified via email on adding to group " + group + ". " +
                                    "Check if the email is set for the user or email provider is setup for your " +
                                    "tenant");
                        } catch (ConfigException e) {
                            EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                            eventBuilder.error(e.getMessage());
                        }
                    }
                });
            }
        }, executor);
    }

    public static void sendGroupRemoved(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        HashMap<String, String> configData = new HashMap<>();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        innerSession.getContext().setRealm(realm);
                        session.getContext().setRealm(realm, session);
                        try {
                            configData = objectMapper.readValue(event.getRepresentation(), HashMap.class);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        UserModel userModel = null;
                        Map<String, String> eventConfig = event.getDetails();
                        boolean isUserAssignedRole = eventConfig.get("assigned_user") != null;
                        if (isUserAssignedRole) {
                            userModel = innerSession.users().getUserById(realm, eventConfig.get("assigned_user"));
                        } else {
                            userModel = innerSession.users().getUserById(realm,
                                    event.getAuthDetails().getUser().getId());
                        }
                        String group = configData.get("name");
                        String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("tenant", getRealmName(realm));
                        attributes.put("user_name", userModel.getUsername());
                        attributes.put("to", userModel.getEmail());
                        attributes.put("year", String.valueOf(year));
                        attributes.put("group", group);
                        attributes.put("workspace", workspaceUrl);
                        attributes.put("adminEvent", "true");
                        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                        List<String> email = new ArrayList<>();
                        if (session.getAttribute("groupUsers") != null) {
                            email = Arrays.asList(session.getAttribute("groupUsers").toString().split(","));
                        }
                        email.forEach(x -> {
                            try {
                                innerSession.getProvider(EmailTemplateProvider.class)
                                        .setRealm(realm)
                                        .send(x, "welcomeUserGroupRevokeSubject", "welcome-user-group-revoke.ftl",
                                                attributes);
                            } catch (EmailException e) {
                                EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                                eventBuilder.user(x);
                                eventBuilder.event(EventType.NOTIFICATION_ERROR);
                                eventBuilder.error("User cannot be notified via email on removing from group " + group
                                        + ". Check if the email is set for the user or email provider is setup for your tenant");
                            } catch (ConfigException e) {
                                EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                                eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                eventBuilder.error(e.getMessage());
                            }
                        });
                        if (session.getAttribute("groupUsers") != null) {
                            session.removeAttribute("groupUsers");
                        }
                    }
                });
            }
        }, executor);
    }

    public static void sendReportNotification(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ArrayList<HashMap<String, String>> configData = new ArrayList<>();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                        innerSession.getContext().setRealm(realm);
                        session.getContext().setRealm(realm, session);
                        String downloadUrl = event.getDetails().get("downloadUrl");

                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("user_name", event.getAuthDetails().getUser().getUsername());
                        attributes.put("tenant", getRealmName(realm));
                        attributes.put("to", event.getAuthDetails().getUser().getEmail());
                        attributes.put("workspace", workspaceUrl + downloadUrl);
                        attributes.put("year", String.valueOf(year));
                        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

                        try {
                            innerSession.getProvider(EmailTemplateProvider.class)
                                    .setRealm(realm)
                                    .send(event.getAuthDetails().getUser().getEmail(), "report-notification", "report" +
                                            "-notification.ftl", attributes);
                        } catch (EmailException e) {
                            EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                            eventBuilder.user(event.getAuthDetails().getUser().getId());
                            eventBuilder.event(EventType.NOTIFICATION_ERROR);
                            eventBuilder.error("User cannot be notified via email on report generation . Check if the" +
                                    " email is set for the user or email provider is setup for your tenant");
                        } catch (ConfigException e) {
                            EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                            eventBuilder.error(e.getMessage());
                        }
                    }
                });
            }
        }, executor);
    }

    public static void sendRoleAssigned(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ArrayList<HashMap<String, String>> configData = new ArrayList<>();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        innerSession.getContext().setRealm(realm);
                        session.getContext().setRealm(realm, session);
                        try {
                            configData = JsonSerialization.readValue(event.getRepresentation(), ArrayList.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Map<String, String> eventConfig = event.getDetails();
                        UserModel assignedUserModel = null;
                        boolean isUserAssignedRole = eventConfig == null ? false
                                : eventConfig.get("assigned_user") != null;
                        if (isUserAssignedRole) {
                            assignedUserModel = innerSession.users().getUserById(realm, eventConfig.get(
                                    "assigned_user"));
                            sendRoleUserAssignment(innerSession, configData, realm, session, assignedUserModel);
                        }
                        boolean isGroupAssignedRole = eventConfig == null ? false
                                : eventConfig.get("assigned_group") != null;
                        if (isGroupAssignedRole) {
                            GroupModel groupModel = innerSession.groups().getGroupById(realm, eventConfig.get(
                                    "assigned_group"));
                            ArrayList<HashMap<String, String>> finalConfigData = configData;
                            innerSession.users().getGroupMembersStream(realm, groupModel).filter(x -> x != null)
                                    .forEach(x -> sendRoleUserAssignment(innerSession, finalConfigData, realm, session,
                                            x));
                        }
                    }
                });
            }
        }, executor);
    }

    private static void sendRoleUserAssignment(KeycloakSession innerSession,
            ArrayList<HashMap<String, String>> configData, RealmModel realm,
            KeycloakSession session, UserModel assignedUserModel) {

        String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
        configData.forEach(x -> {
            boolean isInBuilt = Boolean.parseBoolean(String.valueOf(x.get("inBuilt")));
            if (!isInBuilt) {
                String roleName = x.get("name");
                if (roleName.startsWith("sso-")) {
                    String[] split = roleName.split("-");
                    String sso_app = split[1].indexOf("_") < 0 ? split[1] : split[1].replace("_", " ");
                    ClientModel clientModel = innerSession.clients().getClientByClientId(realm, split[1]);
                    // here we trigger sso assigned email
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("tenant", getRealmName(realm));
                    attributes.put("user_name", assignedUserModel.getUsername());
                    attributes.put("to", assignedUserModel.getEmail());
                    attributes.put("year", String.valueOf(year));
                    attributes.put("sso_app", sso_app);
                    attributes.put("adminEvent", "true");
                    attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                    if (clientModel != null && clientModel.getAttribute("sso_type") != null
                            && clientModel.getAttribute("templateId") != null) {
                        attributes.put("sso_sub_app", split[2].replace("_", " "));
                    }
                    if (clientModel != null && clientModel.getAttribute("logoUrl") != null) {
                        attributes.put("sso_logo", clientModel.getAttribute("logoUrl").replace(".svg", ".png"));
                    }
                    attributes.put("workspace", workspaceUrl);

                    try {
                        innerSession.getProvider(EmailTemplateProvider.class)
                                .setRealm(realm)
                                .send(assignedUserModel.getEmail(), "welcomeUserSSOSubject", "welcome-user-sso.ftl",
                                        attributes);
                    } catch (EmailException e) {
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        eventBuilder.user(assignedUserModel);
                        eventBuilder.event(EventType.NOTIFICATION_ERROR);
                        eventBuilder.error("User cannot be notified via email on " + sso_app + " assignment . Check " +
                                "if the email is set for the user or email provider is setup for your tenant");
                    } catch (ConfigException e) {
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                        eventBuilder.error(e.getMessage());
                    }
                } else {
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("tenant", getRealmName(realm));
                    attributes.put("user_name", assignedUserModel.getUsername());
                    attributes.put("to", assignedUserModel.getEmail());
                    attributes.put("year", String.valueOf(year));
                    attributes.put("role", roleName);
                    attributes.put("workspace", workspaceUrl);
                    attributes.put("adminEvent", "true");
                    attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

                    try {
                        innerSession.getProvider(EmailTemplateProvider.class)
                                .setRealm(realm)
                                .send(assignedUserModel.getEmail(), "welcomeUserRoleSubject", "welcome-user-role.ftl",
                                        attributes);
                    } catch (EmailException e) {
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        eventBuilder.user(assignedUserModel);
                        eventBuilder.event(EventType.NOTIFICATION_ERROR);
                        eventBuilder.error("User cannot be notified via email on " + roleName + " assignment. Check " +
                                "if the email is set for the user or email provider is setup for your tenant");
                    } catch (ConfigException e) {
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                        eventBuilder.error(e.getMessage());
                    }
                }

            }
        });
    }

    public static void sendRoleRemoved(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ArrayList<HashMap<String, String>> configData = new ArrayList<>();
                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        innerSession.getContext().setRealm(realm);
                        session.getContext().setRealm(realm, session);
                        try {
                            configData = JsonSerialization.readValue(event.getRepresentation(), ArrayList.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Map<String, String> eventConfig = event.getDetails();
                        boolean isUserAssignedRole = eventConfig.get("assigned_user") != null;
                        if (isUserAssignedRole) {
                            UserModel assignedUserModel = innerSession.users().getUserById(realm, eventConfig.get(
                                    "assigned_user"));
                            sendRoleUserRemovement(innerSession, configData, realm, assignedUserModel, session);
                        }

                        boolean isGroupAssignedRole = eventConfig.get("assigned_group") != null;
                        if (isGroupAssignedRole) {
                            GroupModel groupModel = innerSession.groups().getGroupById(realm, eventConfig.get(
                                    "assigned_group"));
                            ArrayList<HashMap<String, String>> finalConfigData = configData;
                            innerSession.users().getGroupMembersStream(realm, groupModel).filter(x -> x != null)
                                    .forEach(x -> sendRoleUserRemovement(innerSession, finalConfigData, realm, x,
                                            session));
                        }
                    }
                });
            }
        }, executor);
    }

    public static void sendAdminRoleRemoved(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {
        sendAdminRoleChangeEmail(session, event, config, "admin-role-removed-sub", "admin-role-removed.ftl");
    }

    public static void sendAdminRoleAdded(KeycloakSession session, AdminEvent event, Map<String, String> config,
            boolean includeRepresentation) {
        sendAdminRoleChangeEmail(session, event, config, "admin-role-added-sub", "admin-role-added.ftl");
    }

    private static void sendAdminRoleChangeEmail(KeycloakSession session, AdminEvent event,
            Map<String, String> config, String emailSubjectKey, String emailFtl) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {

                        RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                        innerSession.getContext().setRealm(realm);

                        Map<String, String> eventConfig = event.getDetails();
                        UserModel user = innerSession.users().getUserByEmail(realm, eventConfig.get("email"));

                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("tenant", getRealmName(realm));
                        attributes.put("user_name", user.getUsername());
                        attributes.put("to", user.getEmail());
                        attributes.put("year", String.valueOf(year));
                        attributes.put("workspace", ModelToRepresentation.getWorkspaceUrl(session, realm));
                        attributes.put("adminEvent", "true");
                        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

                        try {
                            innerSession.getProvider(EmailTemplateProvider.class)
                                    .setRealm(realm)
                                    .send(user.getEmail(), emailSubjectKey, emailFtl, attributes);
                        } catch (EmailException e) {
                            e.printStackTrace();
                        } catch (ConfigException e) {
                            EventBuilder eventBuilder = new EventBuilder(realm, session,
                                    session.getContext().getConnection());
                            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                            eventBuilder.error(e.getMessage());
                        }
                    }
                });
            }
        }, executor);
    }

    private static void sendRoleUserRemovement(KeycloakSession innerSession,
            ArrayList<HashMap<String, String>> configData, RealmModel realm,
            UserModel assignedUserModel, KeycloakSession session) {
        String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
        configData.forEach(x -> {
            boolean isInBuilt = Boolean.parseBoolean(String.valueOf(x.get("inBuilt")));
            if (!isInBuilt) {
                String roleName = x.get("name");
                if (roleName.startsWith("sso-")) {
                    String[] split = roleName.split("-");
                    String sso_app = split[1].indexOf("_") < 0 ? split[1] : split[1].replace("_", " ");
                    ClientModel clientModel = innerSession.clients().getClientByClientId(realm, split[1]);
                    // here we trigger sso assigned email
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("tenant", getRealmName(realm));
                    attributes.put("user_name", assignedUserModel.getUsername());
                    attributes.put("to", assignedUserModel.getEmail());
                    attributes.put("year", String.valueOf(year));
                    attributes.put("sso_app", sso_app);
                    attributes.put("adminEvent", "true");
                    attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                    if (clientModel != null && clientModel.getAttribute("sso_type") != null
                            && clientModel.getAttribute("templateId") != null) {
                        attributes.put("sso_sub_app", split[2].replace("_", " "));
                    }
                    if (clientModel != null && clientModel.getAttribute("logoUrl") != null) {
                        attributes.put("sso_logo", clientModel.getAttribute("logoUrl").replace(".svg", ".png"));
                    }
                    attributes.put("workspace", workspaceUrl);

                    try {
                        innerSession.getProvider(EmailTemplateProvider.class)
                                .setRealm(realm)
                                .send(assignedUserModel.getEmail(), "welcomeUserSSORevokeSubject", "welcome-user-sso" +
                                        "-revoke.ftl", attributes);
                    } catch (EmailException e) {
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        eventBuilder.user(assignedUserModel);
                        eventBuilder.event(EventType.NOTIFICATION_ERROR);
                        eventBuilder.error("User cannot be notified via email on revoking access to " + sso_app + ". " +
                                "Check if the email is set for the user or email provider is setup for your tenant");
                    } catch (ConfigException e) {
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                        eventBuilder.error(e.getMessage());
                    }
                } else {
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("tenant", getRealmName(realm));
                    attributes.put("to", assignedUserModel.getEmail());
                    attributes.put("user_name", assignedUserModel.getUsername());
                    attributes.put("year", String.valueOf(year));
                    attributes.put("role", roleName);
                    attributes.put("workspace", workspaceUrl);
                    attributes.put("adminEvent", "true");
                    attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

                    try {
                        innerSession.getProvider(EmailTemplateProvider.class)
                                .setRealm(realm)
                                .send(assignedUserModel.getEmail(), "welcomeUserRoleRevokeSubject", "welcome-user" +
                                        "-role-revoke.ftl", attributes);
                    } catch (EmailException e) {
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        eventBuilder.user(assignedUserModel);
                        eventBuilder.event(EventType.NOTIFICATION_ERROR);
                        eventBuilder.error("User cannot be notified via email on revoking role " + roleName + ". " +
                                "Check if the email is set for the user or email provider is setup for your tenant");
                    } catch (ConfigException e) {
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                        eventBuilder.error(e.getMessage());
                    }
                }

            }
        });
    }

    public static void sendAccountCreatorOtpEmail(KeycloakSession session, AdminEvent event,
            EmailTemplateProvider emailTemplateProvider) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            RealmModel realm = innerSession.realms().getRealm(event.getRealmId());
                            Map<String, Object> rep = objectMapper.readValue(event.getRepresentation(), Map.class);
                            rep.put("year", String.valueOf(year));
                            rep.put("supportLink", EnvironmentReader.getValue("SUPPORT_LINK", "https://support.ezto.io/support/home"));
                            rep.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                            try {
                                emailTemplateProvider
                                        .setRealm(realm)
                                        .send(String.valueOf(rep.get("to")), "accountCreatorReqSubject", "account" +
                                                "-creator-req.ftl", rep);
                            } catch (EmailException e) {
                                log.error(e);
                            } catch (ConfigException e) {
                                EventBuilder eventBuilder = new EventBuilder(realm, session,
                                        session.getContext().getConnection());
                                eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                eventBuilder.error(e.getMessage());
                            }
                        } catch (JsonProcessingException e) {
                            log.error(e);
                        }
                    }
                });
            }
        }, executor);
    }

    public static void sendAccountCreatorSuccessEmail(KeycloakSession session, AdminEvent event) {
        // CompletableFuture.runAsync(new Runnable() {
        // @Override
        // public void run() {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession innerSession) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    RealmModel realm = innerSession.realms().getRealm(event.getAuthDetails().getRealm().getId());
                    Map<String, Object> rep = objectMapper.readValue(event.getRepresentation(), Map.class);
                    rep.put("year", String.valueOf(year));
                    rep.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                    if (rep != null && rep.get("user_name") != null) {
                        // Send notification to new account creator.
                        try {
                            session.getProvider(EmailTemplateProvider.class)
                                    .setRealm(realm)
                                    .send(String.valueOf(rep.get("user_name")), "accountSuccessSubject", "account" +
                                            "-creator-success.ftl", rep);
                        } catch (EmailException e) {
                            log.error(e);
                        } catch (ConfigException e) {
                            EventBuilder eventBuilder = new EventBuilder(realm, session,
                                    session.getContext().getConnection());
                            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                            eventBuilder.error(e.getMessage());
                        }
                        // Send notification to old account creator, here after that they are not
                        // account creator
                        String acc_creator_email = String.valueOf(rep.get("old_acc_creator_email"));
                        if (!acc_creator_email.equals("null")) {
                            rep.put("emlForOldAccCreator", true);
                            try {
                                session.getProvider(EmailTemplateProvider.class)
                                        .setRealm(realm)
                                        .send(acc_creator_email, "accountSuccessSubject", "account-creator-success" +
                                                ".ftl", rep);
                            } catch (EmailException e) {
                                log.error(e);
                            } catch (ConfigException e) {
                                EventBuilder eventBuilder = new EventBuilder(realm, session,
                                        session.getContext().getConnection());
                                eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                eventBuilder.error(e.getMessage());
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    log.error(e);
                }

            }
        });
        // }
        // }, executor);
    }

    public static void sendHookFailedEmail(KeycloakSession session, AdminEvent event) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(),
                new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) throws JsonProcessingException {
                        try {
                            RealmModel realm = innerSession.realms()
                                    .getRealm(event.getAuthDetails().getRealm().getId());
                            final ClientModel client = innerSession.clients().getClientByClientId(realm,
                                    session.getContext().getClient().getClientId());
                            Map<String, Object> rep = JsonSerialization.readValue(event.getRepresentation(),
                                    Map.class);
                            final boolean isPolicy = rep.get("isPolicy") != null && (boolean) rep.get("isPolicy");
                            String subject;
                            String template;
                            if(isPolicy) {
                                subject = "hookFailedSubject";
                                template = "hook-failed.ftl";
                            } else {
                                subject = "hookDisabledSubject";
                                template = "hook-disabled.ftl";
                            }
                            rep.put("year", String.valueOf(year));
                            rep.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                            innerSession.users()
                                    .searchForUserStream(realm, "", true).forEach(adminUser -> {
                                        if (adminUser.isEmailVerified()) {
                                            try {
                                                rep.put("user_name", adminUser.getUsername());
                                                session.getProvider(EmailTemplateProvider.class)
                                                        .setRealm(realm)
                                                        .send(adminUser.getEmail(), subject, template, rep);
                                            } catch (EmailException e) {
                                                throw new RuntimeException(e);
                                            } catch (ConfigException e) {
                                                EventBuilder eventBuilder = new EventBuilder(realm, session,
                                                        session.getContext().getConnection());
                                                eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                                                eventBuilder.error(e.getMessage());
                                            }
                                        }
                                    });
                            client.setAttribute("hook_disabled_mail_sent", "true");
                        } catch (Exception e) {
                            log.error(e.getMessage());
                        }
                    }
                });
    }

    public static void sendWarningEmailForAccessKeys(KeycloakSession session, RealmModel realm, Map<String, Object> rep, AccessKeyManager.ExpirationStatus expirationStatus){
        try {
            String emailAddress = (String) rep.get("toEmail");

            String template = "";
            String subject = "";

            if(Objects.equals(expirationStatus, AccessKeyManager.ExpirationStatus.EXPIRED)) {
                template = "mgmt-keys-expired.ftl";
                subject = "mgmtKeysExpiredSubject";
            } else {
                template = "mgmt-keys-will-expire.ftl";
                subject = "mgmtKeysWillExpireSubject";
            }

            session.getContext().setRealm(realm);
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .send(emailAddress, subject, template, rep);
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * helper functions starts
     **/
    private static String getRealmName(RealmModel realm) {
        return !TextUtils.isEmpty(realm.getDisplayName()) ? realm.getDisplayName() : realm.getName();
    }

    private static CredentialRepresentation getPasswordCredentialOf(UserRepresentation user) {
        CredentialRepresentation password = null;
        if (user.getCredentials() != null) {
            for (CredentialRepresentation c : user.getCredentials()) {
                if (CredentialRepresentation.PASSWORD.equals(c.getType())) {
                    password = c;
                }
            }
        }
        return password;
    }

    private static String createTempPassword(KeycloakSession session, RealmModel realm, UserModel user,
            String clientId) {
        String autoGeneratedPass = HmacOTP.generateSecret(10);
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(user.getUsername());
        List<CredentialRepresentation> credentials = new ArrayList<>();
        CredentialRepresentation pass = new CredentialRepresentation();
        pass.setType(PASSWORD);
        pass.setValue(autoGeneratedPass);
        pass.setTemporary(true);
        pass.setUserLabel("userInvited");
        credentials.add(pass);
        rep.setCredentials(credentials);
        RepresentationToModel.createCredentials(rep, session, realm, user, true);
        user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");
        user.setSingleAttribute(UserModel.USER_INVITE_CLIENT, clientId);
        return autoGeneratedPass;
    }

    private static String formOIDCUrl(KeycloakSession session, RealmModel realm, String baseUrl, String clientId) {
        String url = baseUrl + "realms/" + realm.getName() + "/protocol/openid-connect/auth?";
        ClientModel client = realm.getClientByClientId(clientId);
        if (client != null) {
            String URL = url
                    + "client_id=" + clientId
                    + "&redirect_uri=" + getFormattedRedirectURI(session, realm, baseUrl,
                            client.getRedirectUris().stream().findFirst().get())
                    + getResponseParam(client);
            if (client.getAttribute(PKCE_CODE_CHALLENGE_METHOD) != null
                    && client.getAttribute(PKCE_CODE_CHALLENGE_METHOD).equalsIgnoreCase("S256")) {
                PkceUtils pkceUtils = PkceUtils.generatePkce();
                String codeChallenge = pkceUtils.getCodeChallenge();
                URL += "&code_challenge=" + codeChallenge;
                URL += "&code_challenge_method=S256";
            }

            if (client.getName().equalsIgnoreCase(Constants.ADMIN_CONSOLE_CLIENT_ID)) {
                URL += "&try_url=true";
            }

            return URL;
        }
        return url;
    }

    private static String getFormattedRedirectURI(KeycloakSession session, RealmModel realm, String baseUrl,
            String url) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (url.endsWith("*")) {
            url = url.replace("*", "");
        }
        if (url.length() == 0) {
            url = getProfileAppUrl(session, realm);
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return baseUrl + url;
    }

    private static String getProfileAppUrl(KeycloakSession session, RealmModel realm) {
        ClientModel client = session.clients().getClientByClientId(realm, "profile");
        String profileRedirectUri = "";
        if (client != null) {
            profileRedirectUri = client.getRedirectUris().stream().findFirst().toString();
        }
        return profileRedirectUri;
    }

    private static String getResponseParam(ClientModel client) {
        String response = "";
        if (client.isImplicitFlowEnabled()) {
            response = "&response_type=token&response_mode=fragment";
        } else if (client.isStandardFlowEnabled()) {
            response = "&response_type=code&response_mode=query";
        }
        return response;
    }

    private static Map<String, Object> formatDuration(long valueInSeconds) {
        Map<String, Object> timeDuration = new HashMap<>();
        String unit_key = "seconds";
        long value = valueInSeconds;

        if (value > 0 && value % 60 == 0) {
            value = value / 60;
            unit_key = value == 1 ? "minute" : "minutes";
            if (value % 60 == 0) {
                value = value / 60;
                unit_key = value == 1 ? "hour" : "hours";
                if (value % 24 == 0) {
                    value = value / 24;
                    unit_key = value == 1 ? "day" : "days";
                }
            }
        }
        timeDuration.put("value", value);
        timeDuration.put("unit_key", unit_key);
        // return value + " " + getUnitTextFromMessages(unit_key, value);
        return timeDuration;
    }

    private static String getUnitTextFromMessages(String unit_key, long value) {
        if (value == 1) {
            unit_key = unit_key.substring(0, unit_key.length() - 1);
        }
        return unit_key;
    }

    /** helper functions ends **/

}
