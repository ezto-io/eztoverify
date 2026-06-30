package com.qlikverify.backoffice.realmAttributes.videoSDK;

import org.keycloak.Config.Scope;
import org.keycloak.attributes.RealmAttributeProvider;
import org.keycloak.attributes.RealmAttributeProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import static com.qlikverify.backoffice.Constants.VIDEO_SDK_SECRET_KEY;

public class VideoSDKAPISecretKeyAttributeProvider implements RealmAttributeProvider, RealmAttributeProviderFactory {

    @Override
    public void close() {
        //NOP
    }

    @Override
    public RealmAttributeProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public String getId() {
        return VIDEO_SDK_SECRET_KEY;
    }

    @Override
    public String getDisplayName() {
        return VIDEO_SDK_SECRET_KEY;
    }

    @Override
    public String getAttributeName() {
        return VIDEO_SDK_SECRET_KEY;
    }

    @Override
    public String getValue(KeycloakSession session, RealmModel realmModel) {
        return realmModel.getAttribute(getAttributeName());
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
        return true;
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
        return "Video SDK";
    }

    @Override
    public String getHelpText() {
        return VIDEO_SDK_SECRET_KEY + "-hlp-txt";
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
    public boolean rotateSecret(KeycloakSession session, RealmModel realmModel) {
        return false;
    }

    @Override
    public boolean updateAttribute(KeycloakSession session, RealmModel model, Object value) {
        model.setAttribute(getAttributeName(), String.valueOf(value));
        return true;
    }

    @Override
    public boolean showInSubTenant(RealmModel realmModel) {
        return false;
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
