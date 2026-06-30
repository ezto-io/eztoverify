package com.qlikverify.services.policies.action;

import org.keycloak.Config;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class NoLoginPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "nologin";

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        Integer policyConfig = policy.getPolicyConfig(ID);
        if (policyConfig == null)
            return false;

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(session.getContext().getRealm().getId());

        query.user(user.getId());
        query.maxResults(1);

        List<Long> eventRepresentations = query.getResultStream().map(ModelToRepresentation::toRepresentation).map(x -> x.getTime()).collect(Collectors.toList());

        if (eventRepresentations.isEmpty())
            return false;

        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(eventRepresentations.get(0)), ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS);

        LocalDateTime stop = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS);

        Duration duration = Duration.between(start, stop);

        long days = duration.toDays();

        return days >= policyConfig;
    }

    @Override
    public Integer parseConfig(String value) {
        return Integer.parseInt(value);
    }

    @Override
    public String getDisplayName() {
        return "no-login-check";
    }

    @Override
    public String getConfigType() {
        return ActionPolicyProvider.INT_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return "10";
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
