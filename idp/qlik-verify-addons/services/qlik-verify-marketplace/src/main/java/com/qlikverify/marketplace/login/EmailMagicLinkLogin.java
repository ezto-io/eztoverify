package com.qlikverify.marketplace.login;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

public class EmailMagicLinkLogin implements MarketplaceProvider, MarketplaceProviderFactory {

    KeycloakSession session;
    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+"_login";

        RealmModel realm = session.getContext().getRealm();
        AuthenticationFlowModel topFlow = new AuthenticationFlowModel();
        topFlow.setAlias(aliasName);
        topFlow.setDescription(TemplateConstants.EMAIL_MAGICLINK_LOGIN_DESC);
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
        flow.setAlias("sub_"+aliasName);
        flow.setDescription(TemplateConstants.EMAIL_MAGICLINK_LOGIN_DESC);
        flow.setProviderId("basic-flow");
        flow.setTopLevel(false);
        flow.setBuiltIn(false);
        flow.setHidden(true);
        flow.setTemplateId(getId());
        flow.setTemplateType(getTemplateType().toString());
        flow.setTemplateAlias(getTemplateName());
        flow.setInstalled(true);
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
        execution.setAuthenticator("email-form-authenticator");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
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
        configOne.put("magic_link","true");
        configOne.put("verify-expiry-time","5m");
        configModelOne.setConfig(configOne);
        configModelOne.setAlias(execution.getId());
        configModelOne = realm.addAuthenticatorConfig(configModelOne);
        execution.setAuthenticatorConfig(configModelOne.getId());
        realm.addAuthenticatorExecution(execution);

        return true;
    }

    @Override
    public String getTemplateName() {
        return TemplateConstants.EMAIL_ML_LOGIN_TEMP_TITLE;
    }

    @Override
    public String getTemplateDescription() {
        return TemplateConstants.EMAIL_ML_LOGIN_TEMP_DESC;
    }

    

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
                return Arrays.asList(new BlockMap("start-flow",true,false,"null")
                        , new BlockMap("email-validation-flow", false,false,"start-flow")
                        , new BlockMap("email-magic-link-validation-flow",false,false,"email-validation-flow")
                        , new BlockMap( "end-flow",true,false,"email-magic-link-validation-flow"));
            }
        };
    }

    @Override
    public List<String> getKeywords() {
        return new ArrayList<>();
    }

    @Override
    public boolean isNew() {
        return false;
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
        return "86a06e74-f99b-4fa9-9983-cda3f22805c9";
    }
}
