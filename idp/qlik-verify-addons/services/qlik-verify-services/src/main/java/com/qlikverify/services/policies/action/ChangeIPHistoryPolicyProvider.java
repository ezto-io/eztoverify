package com.qlikverify.services.policies.action;

import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ChangeIPHistoryPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "ipchangehistory";

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        Integer policyConfig = policy.getPolicyConfig(ID);
        if (policyConfig == null)
            return false;

        String ipAddress = GeneralHelper.getInstance().getRemoteAddress(session);
        if (ipAddress == null || ipAddress.isEmpty())
            return false;

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(session.getContext().getRealm().getId());

        query.user(user.getId());
        query.type(EventType.LOGIN);
        query.maxResults(policyConfig.intValue());

        List<String> eventRepresentations = query.getResultStream().map(ModelToRepresentation::toRepresentation).map(x -> x.getIpAddress()).collect(Collectors.toList());

        if (eventRepresentations.isEmpty())
            return true;

        return !eventRepresentations.contains(ipAddress);
    }

    @Override
    public Integer parseConfig(String value) {
        return Integer.parseInt(value);
    }

    @Override
    public String getDisplayName() {
        return "change-ip-history";
    }

    @Override
    public String getConfigType() {
        return ActionPolicyProvider.INT_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return "5";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
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
        return new String[] {"wiam", "ciam", "bo"};
    }


}
