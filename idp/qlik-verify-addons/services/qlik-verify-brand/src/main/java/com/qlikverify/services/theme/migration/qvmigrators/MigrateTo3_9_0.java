package com.qlikverify.services.theme.migration.qvmigrators;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.xbill.DNS.dnssec.R;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MigrateTo3_9_0 implements QVMigration {

    public static final ModelVersion VERSION = new ModelVersion("3.9.0");
    private static Logger logger = Logger.getLogger(MigrateTo3_9_0.class);
    private static Executor executor = Executors.newCachedThreadPool();


    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :" + x.getName());
            iwsRoleFormatUpdate(session, x);
            System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }

    private void iwsRoleFormatUpdate(KeycloakSession session, RealmModel realm) {
        if (realm == null) {
            throw new RuntimeException("Realm is null. " + VERSION + " Migration failed");
        } else {

           try{
               List<ClientModel> iwsEnabledClients = realm.getClientsStream().filter
                       (x -> x.getAttribute("iws.enable")!=null && x.getAttribute("iws.enable").equalsIgnoreCase("true") ).collect(Collectors.toList());

               iwsEnabledClients.forEach(x -> {
                   String oldRole = "sso-" + x.getName().toLowerCase().replace(" ", "_") + "-role";
                   RoleModel oldRoleModel = realm.getRole(oldRole);
                   if (oldRoleModel == null){
                       return;
                   }
                   String newRole = oldRole.replace("sso", "iws");
                   RoleModel newRoleModel = realm.getRole(newRole);
                   if (newRoleModel == null){
                       realm.addRole(newRole);
                   } else {
                       return;
                   }

                   newRoleModel = realm.getRole(newRole);
                   RoleModel finalNewRoleModel = newRoleModel;
                   session.users().getUsersStream(realm, null, null).forEach(user -> {
                       if (user.hasRole(oldRoleModel)){
                           user.grantRole(finalNewRoleModel);
                           user.deleteRoleMapping(oldRoleModel);
                       }
                   });

                   realm.removeRole(oldRoleModel);
               });
           }catch (Exception e){
               e.printStackTrace();
               throw new RuntimeException(e.getMessage());
           }
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
