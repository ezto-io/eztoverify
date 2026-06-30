package com.qlikverify.extensions.admin.controllers.billingAddress.spi;

import com.google.api.client.util.Strings;
import com.qlikverify.billingAddress.spi.BillingAddressRepresentation;
import com.qlikverify.billingAddress.spi.BillingAddressService;
import com.qlikverify.extensions.admin.controllers.billingAddress.jpa.BillingAddressEntity;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

public class BillingAddressServiceImpl implements BillingAddressService {
    private final KeycloakSession session;
    public BillingAddressServiceImpl(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }
    }
    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected RealmModel getRealm() {
        return session.getContext().getRealm();
    }
    @Override
    public void addBillingAddress(RealmModel realm, BillingAddressRepresentation billingAddress) {
        BillingAddressEntity billingAddressEntity = new BillingAddressEntity();
        billingAddressEntity.setRealmId(realm.getId());
        billingAddressEntity.setLine1(billingAddress.getLine1());
        billingAddressEntity.setLine2(billingAddress.getLine2());
        billingAddressEntity.setCity(billingAddress.getCity());
        billingAddressEntity.setState(billingAddress.getState());
        billingAddressEntity.setCountry(billingAddress.getCountry());
        billingAddressEntity.setPostalCode(billingAddress.getPostal_code());

        getEntityManager().persist(billingAddressEntity);
    }

    @Override
    public BillingAddressRepresentation getBillingAddress(RealmModel realmModel) {
        BillingAddressEntity entity = getEntityManager().getReference(BillingAddressEntity.class, realmModel.getId());

        BillingAddressRepresentation billingAddressRepresentation = new BillingAddressRepresentation();
        try {
            billingAddressRepresentation.setLine1(Strings.isNullOrEmpty(entity.getLine1()) ? null : entity.getLine1());
            billingAddressRepresentation.setLine2(Strings.isNullOrEmpty(entity.getLine2()) ? null : entity.getLine2());
            billingAddressRepresentation.setCity(Strings.isNullOrEmpty(entity.getCity()) ? null : entity.getCity());
            billingAddressRepresentation.setState(Strings.isNullOrEmpty(entity.getState()) ? null : entity.getState());
            billingAddressRepresentation.setCountry(Strings.isNullOrEmpty(entity.getCountry()) ? null : entity.getCountry());
            billingAddressRepresentation.setPostal_code(Strings.isNullOrEmpty(entity.getPostalCode()) ? null : entity.getPostalCode());
        } catch (PersistenceException e){
            return new BillingAddressRepresentation();
        }
        return billingAddressRepresentation;
    }

    @Override
    public void close() {
        // NOP
    }
}
