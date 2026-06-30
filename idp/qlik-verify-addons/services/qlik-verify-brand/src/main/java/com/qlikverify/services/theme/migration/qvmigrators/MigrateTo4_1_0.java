package com.qlikverify.services.theme.migration.qvmigrators;

import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.stream.Collectors;

public class MigrateTo4_1_0 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("4.1.0");
    private static Logger logger = Logger.getLogger(MigrateTo4_1_0.class);
    @Override
    public void migrate(KeycloakSession session) {
        String variant = EnvironmentReader.getValue("VARIANT", " ");
        if (variant.equalsIgnoreCase("ezto auth")) {
            System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
            session.realms().getRealmsStream().forEach(x -> {
                System.out.println("-------------------------------");
                System.out.println("MIGRATION STARTED FOR :" + x.getName());
                toChangeDebuggerUrl(session, x);
                brandingChanges(session, x);
                System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
                System.out.println("-------------------------------");
            });
            System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
        }
    }

    private static void toChangeDebuggerUrl(KeycloakSession session, RealmModel realm) {
        realm.getClientsStream().collect(Collectors.toList()).forEach(x-> {
            if(x.getRedirectUris()!=null && x.getRedirectUris().contains("https://debugger.qlikverify.com/debug")) {
                x.removeRedirectUri("https://debugger.qlikverify.com/debug");
                x.addRedirectUri(EnvironmentReader.getValue("DEBUGGER_URL"));
            }
        });
    }

    private static void brandingChanges(KeycloakSession session, RealmModel realm) {
        if(realm.getMode().equalsIgnoreCase("wiam")) {
            session.loginBranding().getAllBrands(realm,-1,-1).forEach(x-> {
                x.removeAttribute("buttonBgColor");
                x.removeAttribute("textActiveColor");
                x.setAttribute("buttonBgColor", "#4CA355");
                x.setAttribute("textActiveColor", "#418b4a");
            });
        } else if (realm.getMode().equalsIgnoreCase("ciam")) {
            session.loginBranding().getAllBrands(realm,-1,-1).forEach(x-> {
                x.removeAttribute("buttonBgColor");
                x.removeAttribute("textActiveColor");
                x.setAttribute("buttonBgColor", "#323232");
                x.setAttribute("textActiveColor", "#000000");
            });
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}
