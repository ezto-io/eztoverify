package com.qlikverify.backoffice.extensions;

import com.qlikverify.backoffice.controllers.EVAPIResource;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class EVAPIResourceProvider implements AdminRealmResourceProvider {


    private final KeycloakSession session;
    public EVAPIResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {

    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new EVAPIResource(session, realm, auth, adminEvent);
    }
}
