package com.qlikverify.services.theme.migration.qvmigrators;

import org.jboss.logging.Logger;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.keycloak.models.Constants.DEFAULT_PASSWORD_POLICY;
import static org.keycloak.models.utils.DefaultAuthenticationFlows.REGISTRATION_FLOW;

public class MigrateTo3_9_1 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("3.9.1");
    private static Logger logger = Logger.getLogger(MigrateTo3_9_1.class);
    private static Executor executor = Executors.newCachedThreadPool();
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :" + x.getName());
            addDefautPasswordPolicy(session, x);
            removeUnusedListeners(session,x);
            System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }

    private void removeUnusedListeners(KeycloakSession session, RealmModel realmModel) {
        List<String> collect = realmModel.getEventsListenersStream().collect(Collectors.toList());
        collect.removeIf(e -> e.equalsIgnoreCase("totm-event-listener"));
        try{
            realmModel.setEventsListeners(new HashSet<>(collect));
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private void addDefautPasswordPolicy(KeycloakSession session, RealmModel realmModel){
       try {
           //Create new realmPolicy
           PolicyModel policyModel = new PolicyModel();
           policyModel.setPolicyName(DEFAULT_PASSWORD_POLICY);
           policyModel.setPolicyValue("passwordHistory(3) and length(8) and notUsername(undefined) and notEmail(undefined) and upperCase(1) and specialChars(1) and digits(1)");
           policyModel.setPolicyType("password");
           policyModel.setRealmPolicy(true);
           policyModel.setInBuilt(false);

           RealmPolicyModel policyByName = session.policies().getPolicyByName(realmModel, DEFAULT_PASSWORD_POLICY);
           if (policyByName == null) {
               policyModel = session.policies().addPolicy(realmModel, policyModel);
           }

           Optional<AuthenticationFlowModel> defaultRegistraionFlow = realmModel.getAuthenticationFlowsStream().filter(x -> x.getAlias().equals(REGISTRATION_FLOW)).findFirst();
           if (defaultRegistraionFlow.isPresent()){
               String authenticatorId = defaultRegistraionFlow.get().getId();
               List<AuthenticationExecutionModel> authenticationExecutionModels = realmModel.getAuthenticationExecutionsStream(authenticatorId).collect(Collectors.toList());
               Optional<AuthenticationExecutionModel> authenticationExecutionModel = authenticationExecutionModels.stream().filter(x -> x.getAuthenticator().equals("password-register-authenticator")).findAny();
               if(authenticationExecutionModel.isPresent()){
                   String authenticatorConfig = authenticationExecutionModel.get().getAuthenticatorConfig();
                   if(authenticatorConfig == null){
                       AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
                       configModel = new AuthenticatorConfigModel();
                       Map<String, String> config = new HashMap<>();
                       config = new HashMap<>();
                       config.put("passwordPolicy", policyModel.getPolicyName());
                       configModel.setConfig(config);
                       configModel.setAlias(KeycloakModelUtils.generateId());
                       configModel = realmModel.addAuthenticatorConfig(configModel);
                       authenticationExecutionModel.get().setAuthenticatorConfig(configModel.getId());
                       realmModel.updateAuthenticatorExecution(authenticationExecutionModel.get());
                   }
               }
           }
       } catch (Exception e){
           e.printStackTrace();
           throw new RuntimeException(e);
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
