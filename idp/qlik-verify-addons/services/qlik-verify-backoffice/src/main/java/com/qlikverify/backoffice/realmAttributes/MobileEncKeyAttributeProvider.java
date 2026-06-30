package com.qlikverify.backoffice.realmAttributes;

import org.keycloak.Config;
import org.keycloak.attributes.RealmAttributeProvider;
import org.keycloak.attributes.RealmAttributeProviderFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ComponentRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.qlikverify.backoffice.Constants.MOBILE_ENC_KEY;

public class MobileEncKeyAttributeProvider implements RealmAttributeProvider, RealmAttributeProviderFactory {


    KeycloakSession session;
    @Override
    public boolean rotateSecret(KeycloakSession session, RealmModel realmModel) {
        ComponentModel model = generateKey(session, realmModel);
        realmModel.setAttribute(getAttributeName(), String.valueOf(model.getConfig().get("secret").get(0)));
        return true;
    }

    @Override
    public boolean generateSecret(KeycloakSession session, RealmModel realmModel) {
        ComponentModel model = generateKey(session, realmModel);
        realmModel.setAttribute(getAttributeName(), String.valueOf(model.getConfig().get("secret").get(0)));
        return true;
    }

    private ComponentModel generateKey(KeycloakSession session, RealmModel realmModel) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("gb_" + UUID.randomUUID().toString());
        rep.setProviderId("aes-generated");
        rep.setProviderType("org.keycloak.keys.KeyProvider");
        rep.setParentId(realmModel.getId());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        List<String> subList = new ArrayList<>();
        subList.add("true");
        config.put("active", subList);
        subList = new ArrayList<>();
        subList.add("32");
        config.put("secretSize", subList);
        subList = new ArrayList<>();
        subList.add("0");
        config.put("priority", subList);
        subList = new ArrayList<>();
        subList.add("true");
        config.put("enabled", subList);
        rep.setConfig(config);
        ComponentModel model = RepresentationToModel.toModel(session, rep);
        if (model.getParentId() == null) model.setParentId(realmModel.getId());

        realmModel.addComponentModel(model);
        return model;
    }

    @Override
    public boolean updateAttribute(KeycloakSession session, RealmModel model, Object value) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "mobile-enc-key-attribute";
    }

    @Override
    public String getAttributeName() {
        return MOBILE_ENC_KEY;
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
        return true;
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
        return false;
    }

    @Override
    public boolean canGenerate(RealmModel realmModel) {
        return realmModel.getAttribute(getAttributeName()) == null || realmModel.getAttribute(getAttributeName()).trim().equalsIgnoreCase("");
    }


    @Override
    public String tabName() {
        return "mobile-apps";
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
        return MOBILE_ENC_KEY;
    }

    @Override
    public boolean showInSubTenant(RealmModel realmModel) {
        return true;
    }
}
