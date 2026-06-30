package com.qlikverify.services.realmattributes;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class SupportBaseAttributeProvider {
    public boolean isUnderTrial(KeycloakSession session, RealmModel realmModel) {
        return false;
    }

    public String subscribeLink(KeycloakSession session, RealmModel realmModel) {
        return null;
    }
}
