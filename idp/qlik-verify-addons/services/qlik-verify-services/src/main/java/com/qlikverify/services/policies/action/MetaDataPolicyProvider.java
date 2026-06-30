package com.qlikverify.services.policies.action;

import com.google.api.client.util.Strings;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.keycloak.Config;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;
import org.keycloak.policy.ciba.CibaPolicy;
import org.keycloak.policy.ciba.CibaPolicyCollection;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.policy.ciba.CibaPolicy.parseCibaPolicy;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

public class MetaDataPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {
    private static final  String ID = "metadata";
    private static final String METADATA = "metadata";
    private static final String REQUEST = "request";
    private static final String NULL_STRING = "null";
    private static final Logger logger = Logger.getLogger(MetaDataPolicyProvider.class);
    @Override
    public String getDisplayName() {
        return "meta-data-policy";
    }

    @Override
    public String getConfigType() {
        return null;
    }

    @Override
    public String getDefaultConfigValue() {
        return null;
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        String bindingMessage = null;
        if(session.getContext().getAuthenticationSession() == null) {
            bindingMessage = session.getAttribute("message") != null ? (String) session.getAttribute("message"): "";
        } else {
            bindingMessage = getBindingMessageFromTransactionAttributes(session, policy, bindingMessage);
        }

        if(Strings.isNullOrEmpty(bindingMessage) || NULL_STRING.equalsIgnoreCase(bindingMessage))
            bindingMessage = session.getContext().getAuthenticationSession().getAuthNote(BINDING_MESSAGE);

        if(bindingMessage == null || bindingMessage.isEmpty())
            return false;
        BindingMessage bindingMessageMap = null;
        try {
            bindingMessageMap = JsonSerialization.readValue(bindingMessage, BindingMessage.class);
        }catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }

        List<Boolean> decisions = new ArrayList<>();
        BindingMessage finalBindingMessageMap = bindingMessageMap;
        try{
            // if policy validation has multiple metadata validation
            if (policy.getPolicies().stream().anyMatch(x -> x.startsWith("metadata"))){
                policy.getPolicies().stream().filter(po -> po.startsWith("metadata")).forEach(polId -> {
                    CibaPolicyCollection metadata = (CibaPolicyCollection) policy.getPolicyConfig(polId);
                    if (metadata != null ){
                        metadata.getPolicyList().forEach(x -> {
                            Object result = finalBindingMessageMap.getMetadata().get(x.getPropKey());
                            if(result != null) {
                                decisions.add(x.evaluate(result, x));
                            } else {
                                decisions.add(false);
                            }
                        });
                    }
                });
            }


            if (decisions.isEmpty()) return false;
            if(policy.getDecisionStrategy().equals(ActionPolicy.UNANIMOUS_STRATEGY)){
                return decisions.stream().filter(x-> !x).count() == 0;
            }else{
                return decisions.stream().filter(x-> x).count() != 0;
            }

        } catch (Exception e){
            logger.error(e.getMessage());
            return false;

        }
    }

    @Override
    public Object parseConfig(String value) {
        CibaPolicyCollection cibaPolicyCollection = new CibaPolicyCollection();
        List<CibaPolicy> policies = new ArrayList<>();

        if (value != null && !value.trim().isEmpty()) {
            for (String policy : value.split(" and ")) {
                CibaPolicy policy1 = parseCibaPolicy(policy);
                if(policy1.getPropKey().equalsIgnoreCase("decision"))
                    cibaPolicyCollection.setDecision(policy1.getPropValue() == null ? "Unanimous" : String.valueOf(policy1.getPropValue()));
                else {
                    policies.add(parseCibaPolicy(policy));
                }
            }
        }
        cibaPolicyCollection.setPolicyList(policies);
        return cibaPolicyCollection;
    }

    @Override
    public ActionPolicyProvider create(KeycloakSession session) {
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
        return ID;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"bo"};
    }

    @Nullable
    private static String getBindingMessageFromTransactionAttributes(KeycloakSession session, ActionPolicy policy, String bindingMessage) {
        if(Strings.isNullOrEmpty(bindingMessage) || NULL_STRING.equalsIgnoreCase(bindingMessage) && (policy.toBuilder().get(METADATA) != null)){
            String requestId = session.getContext().getAuthenticationSession().getAuthNote(REQUEST);

            HashMap<String, Map<String, String>> bindingMessageMap = new HashMap<>();
            HashMap<String, String> bindingDetails = new HashMap<>();
            List<CibaPolicy> policyCollection = new ArrayList<>();
            policy.getPolicies().stream()
                    .filter(currentPolicy->currentPolicy.startsWith(METADATA))
                    .forEach(filteredPolicy->
                            policyCollection.addAll(((CibaPolicyCollection)policy.getPolicyConfig(filteredPolicy)).getPolicyList()));

            policyCollection.forEach(pol -> {
                String key = pol.getPropKey();
                String valueFromTransaction = session.getContext().getRealm().getTrx(requestId).getAttributes().get("sp@" + key);
                if(!Strings.isNullOrEmpty(valueFromTransaction))
                    bindingDetails.put(key,valueFromTransaction);
            });
            if(!bindingDetails.isEmpty()) {
                bindingMessageMap.put(METADATA, bindingDetails);
                try {
                    bindingMessage = JsonSerialization.writeValueAsString(bindingMessageMap);
                } catch (IOException e) {
                    logger.error("Error while serialising JSON");
                    return NULL_STRING;
                }
            }
        }
        return bindingMessage;
    }
}
