package com.qlikverify.marketplace.onboarding;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

import static org.keycloak.models.Constants.DEFAULT_PASSWORD_POLICY;

public class EmailPasswordRegister implements MarketplaceProvider, MarketplaceProviderFactory {
    KeycloakSession session;

    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+"_onboarding";
        RealmModel realm = session.getContext().getRealm();
        AuthenticationFlowModel registrationFlow = new AuthenticationFlowModel();
        registrationFlow.setAlias(aliasName);
        registrationFlow.setDescription(TemplateConstants.EMAIL_PASSWORD_ONBOARDING_DESC);
        registrationFlow.setProviderId("basic-flow");
        registrationFlow.setTopLevel(true);
        registrationFlow.setBuiltIn(false);
        registrationFlow.setInstalled(true);
        registrationFlow.setTemplateId(getId());
        registrationFlow.setTemplateAlias(getTemplateName());
        registrationFlow.setTemplateType(getTemplateType().toString());
        registrationFlow = realm.addAuthenticationFlow(registrationFlow);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setId(KeycloakModelUtils.generateId());
        execution.setParentFlow(registrationFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("email-reg-authenticator");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
        Map<String, String> config = new HashMap<>();
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
        execution.setParentFlow(registrationFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("password-register-authenticator");
        execution.setPriority(20);
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
        return TemplateConstants.BASIC_EMAIL_MOB_REG_TEMP_TITLE;
    }

    @Override
    public String getTemplateDescription() {
        return TemplateConstants.BASIC_EMAIL_MOB_REG_TEMP_DESC;
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
                        , new BlockMap("email-onboarding-flow", false,false,"start-flow")
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

    /**
     * This value is hardcoded and cannot be changed
     *
     * @return
     */
    @Override
    public String getId() {
        return "8fc88460-4632-44b2-a87c-51ccafdd0420";
    }
}
