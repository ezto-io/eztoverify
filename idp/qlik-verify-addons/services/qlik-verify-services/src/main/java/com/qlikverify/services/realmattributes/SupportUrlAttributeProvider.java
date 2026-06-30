package com.qlikverify.services.realmattributes;

import com.qlikverify.services.constants.Constants;
import org.keycloak.Config;
import org.keycloak.attributes.RealmAttributeProvider;
import org.keycloak.attributes.RealmAttributeProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

public class SupportUrlAttributeProvider extends SupportBaseAttributeProvider implements RealmAttributeProvider, RealmAttributeProviderFactory {

    KeycloakSession session;

    @Override
    public int order() {
        return RealmAttributeProviderFactory.super.order();
    }

    @Override
    public boolean rotateSecret(KeycloakSession session, RealmModel realmModel) {
        return false;
    }

    @Override
    public boolean updateAttribute(KeycloakSession session, RealmModel model, Object value) {
        String valueToBeUpdated = String.valueOf(value);

        // Allowed prefixes
        String[] prefixes = {"https://", "mailto:", "tel:"};
        int count = 0;

        for (String prefix : prefixes) {
            int firstIndex = valueToBeUpdated.indexOf(prefix);
            int lastIndex = valueToBeUpdated.lastIndexOf(prefix);

            // Check if the prefix appears exactly once
            if (firstIndex != -1) { // Found at least once
                if (firstIndex == lastIndex) { // Ensures it's not duplicated
                    count++;
                } else {
                    throw new IllegalArgumentException("Support URL must contain only one occurrence of '" + prefix + "'.");
                }
            }
        }

        if (count != 1) {
            throw new IllegalArgumentException("Support URL must contain exactly one occurrence of 'https://', 'mailto:', or 'tel:'.");
        }

        model.setAttribute(getAttributeName(), String.valueOf(value));
        return true;
    }

    @Override
    public String getDisplayName() {
        return Constants.SUPPORT_URL;
    }

    @Override
    public String getAttributeName() {
        return Constants.SUPPORT_URL;
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
        return Constants.SUPPORT;
    }

    @Override
    public String getHelpText() {
        return Constants.SUPPORT_URL_HELPTEXT;
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
        return Constants.SUPPORT_URL;
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
