package com.qlikverify.services.eventlisteners.reports;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

public class ReportEventListenerProviderFactory implements EventListenerProviderFactory {
    @Override
    public String getDisplayName() {
        return "Report Listener";
    }

    @Override
    public String getLogo() {
        return null;
    }

    @Override
    public String getHowTodo() {
        return null;
    }

    @Override
    public boolean isInBuilt() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean isRequiredTestConfigDetails() {
        return false;
    }

    @Override
    public boolean testConfig(KeycloakSession session, Map<String, String> config) {
        return false;
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new ReportEventListenerProvider(session,this);
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
        return "report-listener";
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"bo", "ciam", "wiam"};
    }
}
