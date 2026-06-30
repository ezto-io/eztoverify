package com.qlikverify.services.theme.migration.qvmigrators;

import org.jboss.logging.Logger;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MigrateTo3_9_9 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("3.9.9");
    private static Logger logger = Logger.getLogger(MigrateTo3_9_9.class);
    private static Executor executor = Executors.newCachedThreadPool();

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :" + x.getName());
            realmToWorkspace(session,x);
            System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");

    }

    public void realmToWorkspace(KeycloakSession session,RealmModel realmModel){
        // realm to workspace name change for realm roles
        realmModel.getRolesStream()
                .filter(role->role.getName().contains("realm"))
                .forEach(role->role.setName(role.getName().replace("realm","workspace")));

        // realm to workspace name change for admin realm name
        realmModel.getMasterAdminClient().getRolesStream()
                .filter(role->role.getName().contains("realm"))
                .forEach(role->{
                    role.setName(role.getName().replace("realm","workspace"));
                });

        // realm to workspace name change for realm user roles
        session.users().getUsersStream(realmModel,false)
                .forEach(user ->{
                            user.getRoleMappingsStream().filter(role -> role.getName().contains("realm"))
                                    .forEach(role -> role.setName(role.getName().replace("realm","workspace")));
                });

        // realm to workspace name change for realm client roles
        realmModel.getClientsStream().forEach(clientModel -> {
            clientModel.getRolesStream().filter(roleModel -> roleModel.getName().contains("realm"))
                    .forEach(roleModel -> {
                        roleModel.setName(roleModel.getName().replace("realm","workspace"));
                    });
        });

        // realm to workspace name change for realm name
        if(realmModel.getMasterAdminClient().getName().contains("Realm")) {
            realmModel.getMasterAdminClient()
                    .setName(realmModel.getMasterAdminClient().getName()
                            .replace("Realm", "Workspace"));
        }

        MigrationProvider provider = session.getProvider(MigrationProvider.class);
        ProtocolMapperModel protocolMapper = provider.getBuiltinMappers("openid-connect").get("realm roles");
        if(protocolMapper != null){
           protocolMapper.setName(protocolMapper.getName().replace("realm","workspace"));
        }

        // realm to workspace name changes for mappers
        realmModel.getClientScopesStream().forEach(clientScope->{
                    clientScope.getProtocolMappersStream().filter(protocolMapperModel->protocolMapperModel.getName().equalsIgnoreCase("realm roles"))
                            .collect(Collectors.toList())
                            .forEach(mapperModel -> {
                                mapperModel.setName(mapperModel.getName().replace("realm","workspace"));
                                if(mapperModel.getConfig().get("claim.name").equalsIgnoreCase("realm_access.roles")){
                                    mapperModel.getConfig().remove("claim.name");
                                    mapperModel.getConfig().put("claim.name","workspace_access.roles");
                                }
                            });
                }
        );


    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}
