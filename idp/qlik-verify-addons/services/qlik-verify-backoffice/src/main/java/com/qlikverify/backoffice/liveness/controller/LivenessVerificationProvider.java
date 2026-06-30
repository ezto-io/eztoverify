package com.qlikverify.backoffice.liveness.controller;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class LivenessVerificationProvider implements EztoPublicRealmResourceProvider {
    private final KeycloakSession session;

    public LivenessVerificationProvider(KeycloakSession session) {
        this.session = session;
    }
    @Override
    public Object getResource() {
        return new LivenessVerificationResource(session);
    }

    @Override
    public void close() {

    }
}
