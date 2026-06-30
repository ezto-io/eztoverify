package com.qlikverify.marketplace.onboarding;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

import static org.keycloak.models.Constants.DEFAULT_PASSWORD_POLICY;

public class UsernamePasswordRegister implements MarketplaceProvider, MarketplaceProviderFactory {

    KeycloakSession session;
    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+"_onboarding";
        RealmModel realm = session.getContext().getRealm();
        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setAlias(aliasName);
        flow.setDescription(TemplateConstants.USERANME_PASSWORD_ONBOARDING_DESC);
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);
        flow.setTemplateId(getId());
        flow.setTemplateType(getTemplateType().toString());
        flow.setTemplateAlias(getTemplateName());
        flow.setInstalled(true);
        flow = realm.addAuthenticationFlow(flow);


        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setId(KeycloakModelUtils.generateId());
        execution.setParentFlow(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("username-reg-authenticator");
        execution.setPriority(10);
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
        execution.setAuthenticator("email-reg-authenticator");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        //Setting Auth Configs
        configModel = new AuthenticatorConfigModel();
        config = new HashMap<>();
        config.put("email-option", "otp");
        config.put("verify-expiry-time", "1m");
        config.put("email_ver_otp_len","6");
        configModel.setConfig(config);
        configModel.setAlias(execution.getId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setId(KeycloakModelUtils.generateId());
        execution.setParentFlow(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("password-register-authenticator");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        configModel = new AuthenticatorConfigModel();
        config = new HashMap<>();
        config.put("passwordPolicy", DEFAULT_PASSWORD_POLICY);
        configModel.setConfig(config);
        configModel.setAlias(execution.getId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);

        return true;
    }

    @Override
    public String getTemplateName() {
        return TemplateConstants.USERNAME_PASSWORD_ONBOARDING_TITLE;
    }

    @Override
    public String getTemplateDescription() {
        return TemplateConstants.USERNAME_PASSWORD_ONBOARDING_DESCRIPTION;
    }

    

    @Override
    public String getHowItWorks() {
        return null;
    }

    @Override
    public TemplateType getTemplateType() {
        return TemplateType.ONBOARDING;
    }

    @Override
    public FlowMaps getFlow() {
        return new FlowMaps() {
            @Override
            public List<BlockMap> getBlocks() {
                return Arrays.asList(new BlockMap("start-flow",true,false,"null")
                        , new BlockMap("username-onboarding-flow",false,false,"start-flow")
                        , new BlockMap("email-onboarding-flow", false,false,"username-onboarding-flow")
                        , new BlockMap("password-onboarding-flow",false,false,"email-onboarding-flow")
                        , new BlockMap( "end-flow",true,false,"password-onboarding-flow"));
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
        return "ef96ebfc-a302-4f27-8108-a54793e12953";
    }
}
