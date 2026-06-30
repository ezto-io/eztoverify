package com.qlikverify.extensions.admin.controllers.trxAttribute.spi.listener;

import com.qlikverify.extensions.admin.controllers.trxAttribute.spi.TransactionAttributeProviderImpl;
import com.qlikverify.transactionAttribute.spi.infinispan.TransactionAttributeEntity;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.event.CacheEntryExpiredEvent;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resources.KeycloakApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener
public class TransactionAttributeCacheListener {
    private static final Logger log = LoggerFactory.getLogger(TransactionAttributeCacheListener.class);
    private final KeycloakSession session;

    public TransactionAttributeCacheListener(KeycloakSession session) {
        this.session = session;
    }

//    @CacheEntryExpired
//    public void onEntryExpired(CacheEntryExpiredEvent<String, TransactionAttributeEntity> event) {
//        String trxId = event.getKey();
//        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
//            @Override
//            public void run(KeycloakSession innerSession) {
//                KeycloakSession keycloakSession = KeycloakApplication.getSessionFactory().create();
//                new TransactionAttributeProviderImpl(keycloakSession, keycloakSession.getProvider(InfinispanConnectionProvider.class).getCache("transactionAttributes")).removeTransaction(trxId);
//            }
//        });
//    }
}
