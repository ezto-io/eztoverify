package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.models.BrowserSecurityHeaders.convertListValToWholeString;
import static org.keycloak.models.Constants.*;

public class MigrateTo3_8_1 implements QVMigration{

    public static final ModelVersion VERSION = new ModelVersion("3.8.1");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+x.getName());
            updateBrowserSecurityHeaders(x);
            System.out.println("MIGRATION COMPLETED FOR :"+x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void updateBrowserSecurityHeaders(RealmModel realm) {
        if (realm == null) throw new RuntimeException("Realm is null. " +VERSION+" Migration failed");
        Map<String, String> browserSecurityHeaders = new HashMap<>(realm.getBrowserSecurityHeaders());
        if(browserSecurityHeaders == null){
           throw new RuntimeException("Browser security headers is null. " +VERSION+" Migration failed");
        }
       try {
           browserSecurityHeaders.replace("contentSecurityPolicy", ContentSecurityPolicyBuilder.create().frameSrc(convertListValToWholeString(Constants.listOfDefaultFrameSrc)).build());
           realm.setBrowserSecurityHeaders(browserSecurityHeaders);
       } catch (Exception e){
           throw new RuntimeException(e.getMessage()+ "." +VERSION+ " Migration failed");
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
