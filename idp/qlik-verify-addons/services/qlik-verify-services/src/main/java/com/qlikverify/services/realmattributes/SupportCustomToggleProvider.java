package com.qlikverify.services.realmattributes;

import com.qlikverify.services.constants.Constants;
import org.keycloak.Config;
import org.keycloak.attributes.RealmAttributeProvider;
import org.keycloak.attributes.RealmAttributeProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class SupportCustomToggleProvider extends SupportBaseAttributeProvider implements RealmAttributeProvider, RealmAttributeProviderFactory {
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
        return Constants.SUPPORT_CUSTOM_TOGGLE;
    }

    @Override
    public String getAttributeName() {
        return Constants.SUPPORT_CUSTOM_TOGGLE;
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
    public boolean isToggle(){
        return true;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public String tabName() {
        return Constants.SUPPORT;
    }

    @Override
    public String getHelpText() {
        return Constants.SUPPORT_CUSTOM_HELPTEXT;
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

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return Constants.SUPPORT_CUSTOM_TOGGLE;
    }

    @Override
    public boolean showInSubTenant(RealmModel realmModel) {
        return true;
    }

//    @Override
//    public boolean hidden(UserModel userModel) {
//        return session.getContext().getRealm().getMode().equals("bo");
//    }
}
