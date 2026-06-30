package com.qlikverify.backoffice.realmAttributes;

import org.keycloak.Config;
import org.keycloak.attributes.RealmAttributeProvider;
import org.keycloak.attributes.RealmAttributeProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

public class NumVerifyApiKeyAttributeProvider implements RealmAttributeProvider, RealmAttributeProviderFactory {

    KeycloakSession session;
    @Override
    public boolean rotateSecret(KeycloakSession session, RealmModel realmModel) {
        return false;
    }

    @Override
    public boolean updateAttribute(KeycloakSession session, RealmModel model, Object value) {
        model.setAttribute(getAttributeName(), String.valueOf(value));
        return true;
    }

    @Override
    public String getDisplayName() {
        return "numverify-phone-validation-api-key-attribute";
    }

    @Override
    public String getAttributeName() {
        return "numverify-phone-validation";
    }

    @Override
    public String getValue(KeycloakSession session, RealmModel realmModel) {
        return  realmModel.getAttribute(getAttributeName());
    }

    @Override
    public boolean isSecret() {
        return false;
    }

    @Override
    public boolean shouldRotate() {
        return false;
    }

    @Override
    public boolean canBeViewed() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public String tabName() {
        return "NUMVERIFY";
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public boolean isUnderTrial(KeycloakSession session, RealmModel realmModel) {
        return false;
    }

    @Override
    public String subscribeLink(KeycloakSession session, RealmModel realmModel) {
        return null;
    }

    @Override
    public RealmAttributeProvider create(KeycloakSession session) {
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
        return "numverify-phone-validation";
    }
}
