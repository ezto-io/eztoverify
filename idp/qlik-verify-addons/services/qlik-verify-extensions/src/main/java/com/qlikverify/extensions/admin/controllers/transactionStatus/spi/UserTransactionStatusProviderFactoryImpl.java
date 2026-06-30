package com.qlikverify.extensions.admin.controllers.transactionStatus.spi;

import com.qlikverify.userTransactionStatus.spi.UserTransactionStatusProvider;
import com.qlikverify.userTransactionStatus.spi.UserTransactionStatusProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserTransactionStatusProviderFactoryImpl implements UserTransactionStatusProviderFactory {
    @Override
    public UserTransactionStatusProvider create(KeycloakSession session) {
        return new UserTransactionStatusProviderImpl(session);
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
        return "UserTransactionStatusProviderFactoryImpl";
    }
}
