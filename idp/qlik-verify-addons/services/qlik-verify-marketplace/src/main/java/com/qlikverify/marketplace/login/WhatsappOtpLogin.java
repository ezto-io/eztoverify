package com.qlikverify.marketplace.login;

import com.qlikverify.marketplace.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

public class WhatsappOtpLogin implements MarketplaceProvider, MarketplaceProviderFactory {

    KeycloakSession session;
    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+"_login";

        RealmModel realm = session.getContext().getRealm();

        AuthenticationFlowModel topFlow = new AuthenticationFlowModel();
        topFlow.setAlias(aliasName);
        topFlow.setDescription("user-login-via-whatsapp-otp");
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
        flow.setDescription("user-login-via-whatsapp-otp");
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
        execution.setAuthenticator("phonenumber-form-authenticator");
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
        execution.setAuthenticator("whatsapp-login-authenticator");
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);
        AuthenticatorConfigModel configModel1 = new AuthenticatorConfigModel();
        Map<String, String> config1 = new HashMap<>();
        configModel1.setConfig(config1);
        configModel1.setAlias(execution.getId());
        Map<String, String> configOne = new HashMap<>();
        configOne.put("whatsapp_verify_options", "OTP");
        configOne.put("SESSION_EXPIRY","1m");
        configModel1.setConfig(configOne);
        configModel1 = realm.addAuthenticatorConfig(configModel1);
        execution.setAuthenticatorConfig(configModel1.getId());
        realm.addAuthenticatorExecution(execution);

        return true;
    }

    @Override
    public String getTemplateName() {
        return "whatsapp-otp-login-title";
    }

    @Override
    public String getTemplateDescription() {
        return "whatsapp-login-with-otp";
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
                        , new BlockMap("phone-login-title", false,false,"start-flow")
                        , new BlockMap("whatsapp-login-title", false,false,"phone-login-title")
                        , new BlockMap( "end-flow",true,false,"whatsapp-login-title"));
            }
        };
    }

    @Override
    public List<String> getKeywords() {
        return new ArrayList<>(Arrays.asList("whatsapp","whatsapp otp","otp"));
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
        return "da4eaf9c-119a-499d-a98b-791a0a86cdf5";
    }
}
