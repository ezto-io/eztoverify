package com.qlikverify.marketplace.login;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

public class UsernamePasswordLogin implements MarketplaceProvider, MarketplaceProviderFactory {

    KeycloakSession session;

    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+"_login";
        RealmModel realm = session.getContext().getRealm();
        AuthenticationFlowModel topFlow = new AuthenticationFlowModel();
        topFlow.setAlias(aliasName);
        topFlow.setDescription(TemplateConstants.USERNAME_PASSWORD_LOGIN_DESC);
        topFlow.setProviderId("basic-flow");
        topFlow.setTopLevel(true);
        topFlow.setBuiltIn(false);
        topFlow.setTemplateId(getId());
        topFlow.setTemplateType(getTemplateType().toString());
        topFlow.setTemplateAlias(getTemplateName());
        topFlow.setInstalled(true);
        topFlow = realm.addAuthenticationFlow(topFlow);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(topFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("auth-cookie");
        execution.setPriority(5);
        execution.setHidden(true);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(topFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setAuthenticator("offline-authenticator");
        execution.setPriority(8);
        execution.setHidden(true);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setAlias("sub_" + aliasName);
        flow.setDescription(TemplateConstants.USERNAME_PASSWORD_LOGIN_DESC);
        flow.setProviderId("basic-flow");
        flow.setTopLevel(false);
        flow.setBuiltIn(false);
        flow.setTemplateId(getId());
        flow.setTemplateType(getTemplateType().toString());
        flow.setTemplateAlias(getTemplateName());
        flow.setInstalled(true);
        flow.setHidden(true);
        flow = realm.addAuthenticationFlow(flow);
        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(topFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        execution.setFlowId(flow.getId());
        execution.setPriority(10);
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);


        execution = new AuthenticationExecutionModel();
        execution.setId(KeycloakModelUtils.generateId());
        execution.setParentFlow(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("username-authenticator");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        //Setting Auth Configs
        AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
        Map<String, String> config = new HashMap<>();
        configModel.setConfig(config);
        configModel.setAlias(execution.getId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setId(KeycloakModelUtils.generateId());
        execution.setParentFlow(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("multiple-login-options-authenticator");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        //Setting Auth Configs
        AuthenticatorConfigModel configModelOne = new AuthenticatorConfigModel();
        Map<String, String> configOne = new HashMap<>();
        configOne.put("password", "true");
        configModelOne.setConfig(configOne);
        configModelOne.setAlias(execution.getId());
        configModelOne = realm.addAuthenticatorConfig(configModelOne);
        execution.setAuthenticatorConfig(configModelOne.getId());
        realm.addAuthenticatorExecution(execution);

        return true;
    }

    @Override
    public String getTemplateName() {
        return TemplateConstants.USERNAME_PASSWORD_LOGIN_TEMP_TITLE;
    }

    @Override
    public String getTemplateDescription() { return TemplateConstants.USERNAME_PASSWORD_LOGIN_TEMP_DESC; }


    

    @Override
    public String getHowItWorks() {
        return null;
    }

    @Override
    public TemplateType getTemplateType() {
        return TemplateType.LOGIN;
    }

    @Override
    public FlowMaps getFlow() {
        return new FlowMaps() {
            @Override
            public List<BlockMap> getBlocks() {
                return Arrays.asList(new BlockMap("start-flow", true,false,"null")
                        , new BlockMap("username-validation-flow",false,false,"start-flow" )
                        , new BlockMap("password-validation-flow",false,false,"username-validation-flow")
                        , new BlockMap("end-flow", true,false,"password-validation-flow"));
            }
        };
    }

    @Override
    public List<String> getKeywords() {
        return new ArrayList<>();
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public MarketplaceProvider create(KeycloakSession session) {
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
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "5f7dc2d7-ad94-4f22-b97b-87ddad5cc2fd";
    }
}
