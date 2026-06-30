package com.qlikverify.services.eventlisteners.lastlogin;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;

public class UserLastLoginEventListener implements EventListenerProvider, EventListenerProviderFactory {

    private KeycloakSession session;
    private static final Logger logger = Logger.getLogger(UserLastLoginEventListener.class);

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        if (EventType.LOGIN.equals(event.getType()) && event.getUser() != null && event.getRealm() != null) {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), innerSession -> {
                try {
                    RealmModel realm = innerSession.realms().getRealm(event.getRealm().getId());
                    if (realm == null) return;

                    UserModel userModel = innerSession.users().getUserByUsername(realm, event.getUser().getUsername());
                    if (userModel != null) {
                        userModel.setLastLoginTimestamp(System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    logger.error("Error updating last login timestamp: " + e.getMessage(), e);
                }
            });
        }
    }


    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        // NO OP
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return this;
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NO OP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NO OP
    }

    @Override
    public String getId() {
        return "user-last-login-listener";
    }

    @Override
    public String getDisplayName() {
        return "user-last-login-listener";
    }

    @Override
    public String getLogo() {
        return "";
    }

    @Override
    public String getHowTodo() {
        return "";
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
    public boolean testConfig(KeycloakSession session, Map<String, String> config) {
        return false;
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public String getHelpText() {
        return "";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return List.of();
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"ciam", "wiam", "bo"};
    }

    @Override
    public void close() {
        // NO OP
    }
}
