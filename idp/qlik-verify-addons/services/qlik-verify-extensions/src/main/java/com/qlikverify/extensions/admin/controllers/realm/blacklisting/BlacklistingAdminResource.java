package com.qlikverify.extensions.admin.controllers.realm.blacklisting;

import com.qlikverify.services.resources.BlacklistingResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Path;

public class BlacklistingAdminResource {

    //~ Static fields/initializers =========================================================================================================

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    //~ Constructor ========================================================================================================================

    public BlacklistingAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    //~ API ================================================================================================================================
    @Path("/")
    public BlacklistingResource getBlacklistingDetails(){
        auth.realm().requireManageRealm();
        return new BlacklistingResource(session, realm, adminEvent);
    }
}
