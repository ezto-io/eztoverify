package com.qlikverify.marketplace.recovery;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

public class EmailPasswordRecovery implements MarketplaceProvider, MarketplaceProviderFactory {

    KeycloakSession session;

    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+"_recovery";
        RealmModel realm = session.getContext().getRealm();
        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setAlias(aliasName);
        flow.setDescription(TemplateConstants.EMAIL_PASSWORD_RECOVERY_DESC);
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
        execution.setAuthenticator("reset-password-authenticator");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        //Setting Auth Configs
        AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
        Map<String, String> config = new HashMap<>();
        config.put("use-email-otp", "true");
        config.put("verify-expiry-time","1m");
        config.put("email_ver_otp_len","6");
        configModel.setConfig(config);
        configModel.setAlias(execution.getId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);

        return true;
    }

    @Override
    public String getTemplateName() {
        return TemplateConstants.EMAIL_PASSWORD_RECOVERY_TITLE;
    }

    @Override
    public String getTemplateDescription() {
        return TemplateConstants.EMAIL_PASSWORD_RECOVERY_DESCRIPTION;
    }

    

    @Override
    public String getHowItWorks() {
        return null;
    }

    @Override
    public TemplateType getTemplateType() {
        return TemplateType.RECOVERY;
    }

    @Override
    public FlowMaps getFlow() {
        return new FlowMaps() {
            @Override
            public List<BlockMap> getBlocks() {
                return Arrays.asList(new BlockMap("start-flow",true,false,"null")
                        , new BlockMap("password-recovery-flow", false,false,"start-flow")
                        , new BlockMap( "end-flow",true,false,"password-recovery-flow"));
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
        return "1390fa8f-d84c-4dca-8132-dc966560effa";
    }
}
