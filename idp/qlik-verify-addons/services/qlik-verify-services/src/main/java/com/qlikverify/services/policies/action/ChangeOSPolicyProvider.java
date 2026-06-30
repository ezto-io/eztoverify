package com.qlikverify.services.policies.action;

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

public class ChangeOSPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "oschange";

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        String browser = session.getContext().getConnection().getClientOs();
        if (browser == null || browser.isEmpty())
            return false;

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(session.getContext().getRealm().getId());

        query.user(user.getId());
        query.type(EventType.LOGIN);
        query.maxResults(1);

        List<String> eventRepresentations = query.getResultStream().map(ModelToRepresentation::toRepresentation).map(x -> x.getDetails().get("os")).collect(Collectors.toList());

        if (eventRepresentations.isEmpty())
            return false;

        return !eventRepresentations.contains(browser);
    }

    @Override
    public Object parseConfig(String value) {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "change-os";
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


}
