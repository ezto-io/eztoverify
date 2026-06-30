package com.qlikverify.extensions.admin.controllers.workflows.eventlistener;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;

import static com.qlikverify.extensions.admin.controllers.workflows.misc.Constants.*;

public class WorkflowsEventListenerProvider implements EventListenerProvider, EventListenerProviderFactory {

    private KeycloakSession session;

    @Override
    public void onEvent(AdminEvent adminEvent, Map<String, String> config, boolean includeRepresentation) {
        RealmModel realmModel = Objects.requireNonNull(session.realms().getRealm(adminEvent.getRealmId()));
        if (realmModel.getAttribute(WORKFLOWS_CONNECTED_APPS) == null) return;
        switch (adminEvent.getResourceType()) {
            case WORKFLOW_ROLE: // Against workspace
                new WorkflowsEventListenerResource(session).triggerRoleActivityWorkflows(adminEvent);
                break;
            case WORKSPACE_ROLE_MAPPING: // Against User
                new WorkflowsEventListenerResource(session).triggerUserRoleMappingWorkflows(adminEvent);
                break;
            case USER:
                if (OperationType.DELETE.equals(adminEvent.getOperationType()))
                    new WorkflowsEventListenerResource(session).triggerUserDeletionToConnectedApps(adminEvent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        // NOP
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        this.session = session;
        return this;
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
    public EventListenerProviderFactory getFactory() {
        return this;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return Collections.emptyList();
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"ciam", "wiam"};
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getLogo() {
        return null;
    }

    @Override
    public String getHowTodo() {
        return null;
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
        return null;
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "workflows-event-listener";
    }
}
