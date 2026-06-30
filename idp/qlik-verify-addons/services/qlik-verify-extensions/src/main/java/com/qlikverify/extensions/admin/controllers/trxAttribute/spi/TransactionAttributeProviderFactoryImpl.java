package com.qlikverify.extensions.admin.controllers.trxAttribute.spi;

import com.qlikverify.transactionAttribute.spi.TransactionAttributeProvider;
import com.qlikverify.transactionAttribute.spi.TransactionAttributeProviderFactory;
import com.qlikverify.transactionAttribute.spi.infinispan.TransactionAttributeEntity;
import org.infinispan.Cache;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class TransactionAttributeProviderFactoryImpl implements TransactionAttributeProviderFactory {

    private volatile Cache<String, TransactionAttributeEntity> transactionAttributeCache;

    @Override
    public TransactionAttributeProvider create(KeycloakSession session) {
        return new TransactionAttributeProviderImpl(session, transactionAttributeCache);
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        Cache<String, TransactionAttributeEntity> cache = this.transactionAttributeCache;
        if (cache == null) {
            synchronized (this) {
                cache = this.transactionAttributeCache;
                if (cache == null) {
                    this.transactionAttributeCache = initTransactionAttributeCache(factory.create());
                }
            }
        }
    }

    private Cache<String, TransactionAttributeEntity> initTransactionAttributeCache(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        return connections.getCache("transactionAttributes");
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return "transaction-attribute-provider";
    }

    @Override
    public int order() {
        return 1;
    }
}
