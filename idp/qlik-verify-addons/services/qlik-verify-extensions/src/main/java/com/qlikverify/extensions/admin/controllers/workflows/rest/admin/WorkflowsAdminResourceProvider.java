package com.qlikverify.extensions.admin.controllers.workflows.rest.admin;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class WorkflowsAdminResourceProvider implements AdminRealmResourceProvider, AdminRealmResourceProviderFactory {
    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
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
    public void close() {
        // NO OP
    }

    @Override
    public String getId() {
        return "workflows";
    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new WorkflowsAdminResource(session, realm, auth, adminEvent);
    }
}
