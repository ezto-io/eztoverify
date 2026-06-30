package org.keycloak.models.jpa;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmPolicyModel;
import org.keycloak.models.jpa.entities.RealmPolicyEntity;

import javax.persistence.EntityManager;

public class RealmPolicyAdapter implements RealmPolicyModel, JpaModel<RealmPolicyEntity> {

    protected KeycloakSession session;
    protected RealmModel realm;
    protected EntityManager em;
    protected RealmPolicyEntity entity;

    public RealmPolicyAdapter(RealmModel realm, EntityManager em, KeycloakSession session, RealmPolicyEntity entity) {
        this.session = session;
        this.realm = realm;
        this.em = em;
        this.entity = entity;
    }

    @Override
    public RealmPolicyEntity getEntity() {
        return entity;
    }


    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public void setPolicyName(String policyName) {
        entity.setPolicyName(policyName);
    }

    @Override
    public String getPolicyName() {
        return entity.getPolicyName();
    }

    @Override
    public void setPolicyType(String policyType) {
        entity.setPolicyType(policyType);
    }

    @Override
    public String getPolicyType() {
        return entity.getPolicyType();
    }

    @Override
    public boolean isRealmPolicy() {
        return entity.isRealmPolicy();
    }

    @Override
    public void setAsRealmPolicy(boolean value) {
        entity.setRealmPolicy(value);
    }

    @Override
    public String getPolicyValue() {
        return entity.getPolicyValue();
    }

    @Override
    public void setPolicyValue(String policyValue) {
        entity.setPolicyValue(policyValue);
    }

    @Override
    public void setCreatedTimeStamp(long timeStamp) {
        entity.setCreatedTimestamp(timeStamp);
    }

    @Override
    public long getCreatedTimeStamp() {
        return entity.getCreatedTimestamp();
    }

    @Override
    public boolean inBuilt() {
        return entity.isInBuilt();
    }

    @Override
    public void setInBuilt(boolean inBuilt) {
        entity.setInBuilt(inBuilt);
    }
}
