package com.qlikverify.marketplace.onboarding;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import com.webauthn4j.util.exception.WebAuthnException;
import org.keycloak.Config;
import org.keycloak.marketplace.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resources.admin.MarketplaceManagementResource;

import java.util.*;

public class EmailWebAuthnRegister implements MarketplaceProvider, MarketplaceProviderFactory {
    private static final String ONBOARDING = "_onboarding";

    KeycloakSession session;

    @Override
    public boolean install(Map<String,String> configData) {

        String aliasName = GeneralHelper.getConfigValue(configData,"alias","")+ ONBOARDING;
        RealmModel realm = session.getContext().getRealm();

        try {
            MarketplaceManagementResource.checkPolicyExists(session,realm,aliasName.split(ONBOARDING)[0]+"_webauthn_policy");
        }catch (RuntimeException e){
            throw new WebAuthnException(e.getMessage());
        }

        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setAlias(aliasName);
        flow.setDescription(TemplateConstants.EMAIL_WEBUTHN_ONBOARDING_DESC);
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
        config.put("email-option", "otp");
        configModel.setConfig(config);
        configModel.setAlias(execution.getId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);



        String policyValue = "{\"webAuthnPolicySignatureAlgorithms\":\"RS256\",\"webAuthnPolicyRpEntityName\":\"{{alias_name}}\"}";


        PolicyModel policyModel = new PolicyModel();
        policyModel.setRealmPolicy(false);
        policyModel.setPolicyType("webauthn");
        policyModel.setPolicyValue(policyValue.replace("{{alias_name}}",aliasName.split(ONBOARDING)[0]));
        policyModel.setInBuilt(false);
        policyModel.setPolicyName(aliasName.split(ONBOARDING)[0]+"_webauthn_policy");


        policyModel = session.policies().addPolicy(realm, policyModel);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("webauthn-reg-auth-passwordless");
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        configModel = new AuthenticatorConfigModel();
        config = new HashMap<>();
        config.put("WEBAUTHN_POLICY", policyModel.getPolicyName());
        configModel.setConfig(config);
        configModel.setAlias(KeycloakModelUtils.generateId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);



        return true;
    }

    @Override
    public String getTemplateName() {
        return TemplateConstants.EMAIL_WEBAUTHN_PASSWORDLESS_REG_TEMP_TITLE;
    }

    @Override
    public String getTemplateDescription() {
        return TemplateConstants.EMAIL_WEBAUTHN_PASSWORDLESS_REG_TEMP_DESC;
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
                        , new BlockMap("webauthn-passwordless-onboarding-flow",false,false,"email-onboarding-flow")
                        , new BlockMap( "end-flow",true,false,"webauthn-passwordless-onboarding-flow"));
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
        return "13190269-76d6-4a4d-bfc5-1a4ad5ddf675";
    }
}
