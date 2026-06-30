package com.qlikverify.marketplace.onboarding;

import com.qlikverify.marketplace.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

public class WhatsappQrRegister implements MarketplaceProvider, MarketplaceProviderFactory {

    KeycloakSession session;
    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+"_onboarding";

        RealmModel realm = session.getContext().getRealm();
        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setAlias(aliasName);
        flow.setDescription("user-reg-via-whatsapp-qr");
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
        execution.setAuthenticator("phone-registration-authenticator");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        //Setting Auth Configs
        AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
        Map<String, String> config = new HashMap<>();
        config.put("sms_ver_otp_txt", "Your " + GeneralHelper.getTenantName(realm) +" OTP is {code}");
        config.put("verify-expiry-time", "1m");
        config.put("sms_ver_otp_len","6");
        config.put("skipUserCreation", "true");
        configModel.setConfig(config);
        configModel.setAlias(execution.getId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setId(KeycloakModelUtils.generateId());
        execution.setParentFlow(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("whatsapp-registration-authenticator");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        AuthenticatorConfigModel authConfigModel = new AuthenticatorConfigModel();
        Map<String, String> configdata = new HashMap<>();
        authConfigModel.setConfig(configdata);
        authConfigModel.setAlias(execution.getId());
        Map<String, String> configOne = new HashMap<>();
        configOne.put("whatsapp_verify_options", "QR");
        configOne.put("SESSION_EXPIRY","1m");
        authConfigModel.setConfig(configOne);
        authConfigModel = realm.addAuthenticatorConfig(authConfigModel);
        execution.setAuthenticatorConfig(authConfigModel.getId());
        realm.addAuthenticatorExecution(execution);

        return true;
    }

    @Override
    public String getTemplateName() {
        return "whatsapp-reg-with-qr";
    }

    @Override
    public String getTemplateDescription() {
        return "whatsapp-reg-with-qr-helptext";
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
                        , new BlockMap("phone-onboarding-flow",false,false,"start-flow")
                        , new BlockMap("whatsapp-onboarding-flow",false,false,"phone-onboarding-flow")
                        , new BlockMap( "end-flow",true,false,"whatsapp-onboarding-flow"));
            }
        };
    }

    @Override
    public List<String> getKeywords() {
        return new ArrayList<>(Arrays.asList("whatsapp","whatsapp qr","qr"));
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
        return "2692d15f-1285-4bae-b0dd-210e5c9fbc44";
    }
}
