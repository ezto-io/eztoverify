package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.UUID;

import static org.keycloak.models.CibaConfig.CIBA_EXPIRES_IN;
import static org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer.CIBA_LINK;
import static org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer.CIBA_NOTIFY;

public class MigrateTo9_0_5 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("9.0.5");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream()
                .filter(realm -> realm.getMode().equals("bo"))
                .forEach(x -> {
                    System.out.println("-------------------------------");
                    System.out.println("MIGRATION STARTED FOR :" + x.getName());
                    addRealmAttributes(x);
                    createDefaultApiApplication(x);
                    System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
                    System.out.println("-------------------------------");
                });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }

    public void addRealmAttributes(RealmModel realm){
        realm.setAttribute("isPlatformWorkspace", "true");
        realm.setAttribute("isApiWorkspace", "true");
    }

    public void createDefaultApiApplication(RealmModel realm){
        if(realm.getClientByClientId("default" + "_API") != null)
            return;
        ClientModel clientModel = realm.addClient();
        clientModel.setSandboxMode(false);
        clientModel.setAuthenticationFlowBindingOverride("ciba_auth", "api-authorizer");
        clientModel.setName("default" + "_API");
        clientModel.setClientId(clientModel.getName());
        clientModel.setSandboxMode(false);
        clientModel.setAuthenticationFlowBindingOverride("ciba_auth", "api-authorizer");
        clientModel.setProtocol("openid-connect");
        clientModel.setClientAuthenticatorType(KeycloakModelUtils.getDefaultClientAuthenticatorType());
        KeycloakModelUtils.generateSecret(clientModel);
        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setAttribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, "true");
        clientModel.setPublicClient(false);
        clientModel.setSandboxMode(false);
        clientModel.setAttribute("login_theme", "default");
        clientModel.setAttribute("isInBuilt", "true");
        clientModel.setAttribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, "true");
        CibaAuthConfigModel configModel = new CibaAuthConfigModel();
        configModel.setClientId(clientModel.getClientId());
        configModel.setId(UUID.randomUUID().toString());
        configModel.setRealmId(realm.getId());
        configModel.setCibaAuthProviderId("ciba-default-managed-auth");
        configModel.setConfig(new HashMap<String,String>(){{
            put("verify-expiry-time", "300s");
            put(CIBA_NOTIFY,CIBA_LINK);
        }});
        realm.addCibaAuthConfig(configModel);
        clientModel.setAttribute(CIBA_EXPIRES_IN, "300s");
    }
}
