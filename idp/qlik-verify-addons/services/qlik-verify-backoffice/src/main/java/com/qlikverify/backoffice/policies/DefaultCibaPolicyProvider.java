package com.qlikverify.backoffice.policies;

import org.keycloak.Config;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmPolicyModel;
import org.keycloak.policy.ciba.CibaPolicy;
import org.keycloak.policy.ciba.CibaPolicyCollection;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.policy.ciba.CibaPolicyProviderFactory;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DefaultCibaPolicyProvider implements CibaPolicyProvider, CibaPolicyProviderFactory {
    @Override
    public boolean validate(KeycloakSession session, String bindingMessage, String cibaPolicyName) {
        RealmPolicyModel realmPolicyModel = session.policies().getPolicyById(session.getContext().getRealm(),cibaPolicyName);
        if(realmPolicyModel == null)
            return false;


        if(bindingMessage == null || bindingMessage.isEmpty())
            return false;

        BindingMessage bindingMessageMap = null;
        try {
            bindingMessageMap = JsonSerialization.readValue(new String(Base64.getDecoder().decode(bindingMessage.getBytes(StandardCharsets.UTF_8))), BindingMessage.class);
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        CibaPolicyCollection cibaPolicyCollection = CibaPolicy.parse(realmPolicyModel.getPolicyValue());
        List<Boolean> decisions = new ArrayList<>();
        BindingMessage finalBindingMessageMap = bindingMessageMap;
        cibaPolicyCollection.getPolicyList().forEach(x-> {
            Object result = finalBindingMessageMap.getMetadata().get(x.getPropKey());
            if(result != null) {
                decisions.add(x.evaluate(result, x));
            }
        });

        if(decisions.isEmpty())
            return false;

        if(cibaPolicyCollection.getDecision().equalsIgnoreCase("unanimous"))
            return decisions.stream().filter(x->!x).count() == 0;

        return decisions.stream().filter(x-> x).count() > 0;
    }

    @Override
    public CibaPolicyProvider create(KeycloakSession session) {
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
        return "default-ciba-policy";
    }

    @Override
    public String getDisplayName() {
        return null;
    }
}
