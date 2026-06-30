package com.qlikverify.extensions.admin.controllers.billingAddress.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class BillingAddressEntityProvider implements JpaEntityProvider {
    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(BillingAddressEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/billing-address-master-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return BillingAddressEntityProviderFactory.ID;
    }

    @Override
    public void close() {
        // NOP
    }
}
