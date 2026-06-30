package com.qlikverify.marketplace.onboarding;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

public class EmailMagicLinkRegister implements MarketplaceProvider, MarketplaceProviderFactory {

    KeycloakSession session;

    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+"_onboarding";
        RealmModel realm = session.getContext().getRealm();
        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setAlias(aliasName);
        flow.setDescription(TemplateConstants.EMAIL_MAGIC_LINK_ONBOARDING_DESC);
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
        execution.setAuthenticator("email-reg-authenticator");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        //Setting Auth Configs
        AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
        Map<String, String> config = new HashMap<>();
        config.put("email-option", "magic_link");
        config.put("verify-expiry-time","5m");
        configModel.setConfig(config);
        configModel.setAlias(execution.getId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);

        return true;
    }

    @Override
    public String getTemplateName() {
        return TemplateConstants.EMAIL_ML_REG_TEMP_TITLE;
    }

    @Override
    public String getTemplateDescription() {
        return TemplateConstants.EMAIL_ML_REG_TEMP_DESC;
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
                        , new BlockMap( "end-flow",true,false,"email-onboarding-flow"));
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
        return "ffad4c80-5956-407b-8658-ec56f8f5f001";
    }
}
