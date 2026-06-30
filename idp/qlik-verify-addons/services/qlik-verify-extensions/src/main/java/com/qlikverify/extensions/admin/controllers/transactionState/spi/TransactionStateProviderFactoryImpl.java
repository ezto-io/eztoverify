package com.qlikverify.extensions.admin.controllers.transactionState.spi;

import com.qlikverify.transactionState.spi.TransactionStateProvider;
import com.qlikverify.transactionState.spi.TransactionStateProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class TransactionStateProviderFactoryImpl implements TransactionStateProviderFactory {
    @Override
    public TransactionStateProvider create(KeycloakSession session) {
        return new TransactionStateProviderImpl(session);
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
        //NOP
    }

    @Override
    public String getId() {
        return "TransactionStateProviderFactoryImpl";
    }
}
