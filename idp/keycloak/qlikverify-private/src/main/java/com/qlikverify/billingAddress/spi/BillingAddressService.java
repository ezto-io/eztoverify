package com.qlikverify.billingAddress.spi;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

public interface BillingAddressService extends Provider {
    void addBillingAddress(RealmModel realm, BillingAddressRepresentation billingAddress);
    BillingAddressRepresentation getBillingAddress(RealmModel realm);
}
