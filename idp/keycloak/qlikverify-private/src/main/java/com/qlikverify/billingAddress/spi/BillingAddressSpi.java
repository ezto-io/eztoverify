package com.qlikverify.billingAddress.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class BillingAddressSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "billingAddress";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return BillingAddressService.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return BillingAddressServiceProviderFactory.class;
    }
}
