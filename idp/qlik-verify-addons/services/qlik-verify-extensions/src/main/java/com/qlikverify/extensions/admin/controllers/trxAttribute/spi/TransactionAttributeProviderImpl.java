package com.qlikverify.extensions.admin.controllers.trxAttribute.spi;

import com.qlikverify.extensions.admin.controllers.trxAttribute.spi.listener.TransactionAttributeCacheListener;
import com.qlikverify.extensions.admin.misc.helpers.GeneralHelper;
import com.qlikverify.transactionAttribute.spi.TransactionAttributeProvider;
import com.qlikverify.transactionAttribute.spi.infinispan.TransactionAttributeEntity;
import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.TrxModel;
import org.keycloak.models.jpa.entities.TrxAttributeEntity;
import org.keycloak.models.sessions.infinispan.InfinispanKeycloakTransaction;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TransactionAttributeProviderImpl implements TransactionAttributeProvider {

    private final KeycloakSession session;
    private final Cache<String, TransactionAttributeEntity> transactionAttributeCache;
    private final InfinispanKeycloakTransaction tx;
    private static final Logger logger = Logger.getLogger(TransactionAttributeProviderImpl.class);

    public TransactionAttributeProviderImpl(KeycloakSession session, Cache<String, TransactionAttributeEntity> transactionAttributeCache) {
        this.session = session;
        this.transactionAttributeCache = transactionAttributeCache;
        this.tx = new InfinispanKeycloakTransaction();

        session.getTransactionManager().enlistAfterCompletion(tx);
        addTransactionAttributeCacheListener();
    }

    private void addTransactionAttributeCacheListener(){
        if(transactionAttributeCache.getListeners().isEmpty()) {
            transactionAttributeCache.addListener(new TransactionAttributeCacheListener(session));
        }
    }

    private EntityManager getEntityManager() {
        EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        if(entityManager != null) {
            return entityManager;
        }
        return null;
    }

    @Override
    public TrxModel addAttribute(String trxId, String name, String value) {
        value = GeneralHelper.getInstance().sanitizeErrorMessage(value);
        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);

        if (transactionAttributeEntity == null) {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put(name, value);
            transactionAttributeEntity = new TransactionAttributeEntity(attributes, System.currentTimeMillis());
            this.tx.put(transactionAttributeCache, trxId, transactionAttributeEntity, getExpiryInSeconds(transactionAttributeEntity),TimeUnit.SECONDS);
        } else {
            transactionAttributeEntity.getTrxAttributes().put(name, value);
            this.tx.replace(transactionAttributeCache, trxId, transactionAttributeEntity, getExpiryInSeconds(transactionAttributeEntity), TimeUnit.SECONDS);
        }

        persistAttributeValueIfNecessary(name, value, trxId, transactionAttributeEntity);
        return entityToModel(trxId, transactionAttributeEntity);
    }

    private void persistAttributeValueIfNecessary(String name, String value, String trxId, TransactionAttributeEntity cachedEntity) {
        String currentValue = cachedEntity.getTrxAttributes().get(name);

        if (currentValue != null && currentValue.equals(value)) {
            return;
        }

        int updated = getEntityManager()
                .createQuery("UPDATE TrxAttributeEntity t SET t.value = :value WHERE t.trxId = :trxId AND t.name = :name")
                .setParameter("value", value)
                .setParameter("trxId", trxId)
                .setParameter("name", name)
                .executeUpdate();

        if (updated == 0) {
            TrxAttributeEntity attr = new TrxAttributeEntity();
            attr.setId(KeycloakModelUtils.generateId());
            attr.setName(name);
            attr.setValue(value);
            attr.setTrxId(trxId);
            getEntityManager().persist(attr);
        }
    }


    @Override
    public void removeTrxAttribute(String trxId, String name) {
        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);
        if (transactionAttributeEntity != null) {
            Map<String, String> attributes = transactionAttributeEntity.getTrxAttributes();
            attributes.remove(name);
            transactionAttributeEntity.setTrxAttributes(attributes);
            this.tx.replace(transactionAttributeCache, trxId, transactionAttributeEntity, getExpiryInSeconds(transactionAttributeEntity), TimeUnit.SECONDS);
            removeTransactionAttribute(trxId, name);
        }
    }

    @Override
    public void addAttributes(String trxId, Map<String, String> attributes) {

        if(attributes == null || attributes.isEmpty()) {
            return;
        }

        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);

        if(transactionAttributeEntity != null) {
            Map<String, String> a = transactionAttributeEntity.getTrxAttributes();
            a.putAll(attributes);
            this.tx.replace(transactionAttributeCache, trxId, transactionAttributeEntity, getExpiryInSeconds(transactionAttributeEntity), TimeUnit.SECONDS);
        } else {
            transactionAttributeEntity = new TransactionAttributeEntity(attributes, System.currentTimeMillis());
            this.tx.put(transactionAttributeCache, trxId, transactionAttributeEntity, getExpiryInSeconds(transactionAttributeEntity),TimeUnit.SECONDS);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO trx_attribute (id, trx_id, name, value, created_at) VALUES ");

        int i = 0;
        long now = System.currentTimeMillis();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String id = KeycloakModelUtils.generateId();
            String name = entry.getKey().replace("'", "''");
            String value = entry.getValue() != null ? entry.getValue().replace("'", "''") : "";

            sql.append(String.format("('%s', '%s', '%s', '%s', %d)",
                    id, trxId, name, value, now));

            if (i < attributes.size() - 1) {
                sql.append(", ");
            }
            i++;
        }

        sql.append(" ON CONFLICT (trx_id, name) DO UPDATE SET ")
                .append("value = EXCLUDED.value, ")
                .append("created_at = EXCLUDED.created_at");

        getEntityManager().createNativeQuery(sql.toString()).executeUpdate();
    }

    @Override
    public void removeAttributes(String trxId, List<String> claimsToBeRemoved) {
        if (claimsToBeRemoved == null || claimsToBeRemoved.isEmpty()) return;

        getEntityManager().clear();

        StringBuilder sql = new StringBuilder("DELETE FROM trx_attribute WHERE (trx_id, name) IN (");

        for (int i = 0; i < claimsToBeRemoved.size(); i++) {
            sql.append("(:trxId").append(i).append(", :name").append(i).append(")");
            if (i < claimsToBeRemoved.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");

        Query query = getEntityManager().createNativeQuery(sql.toString());

        for (int i = 0; i < claimsToBeRemoved.size(); i++) {
            query.setParameter("trxId" + i, trxId);
            query.setParameter("name" + i, claimsToBeRemoved.get(i));
        }

        query.executeUpdate();
        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);
        Map<String, String> claims = transactionAttributeEntity.getTrxAttributes();
        claims.keySet().removeAll(claimsToBeRemoved);
        transactionAttributeEntity.setTrxAttributes(claims);
        this.tx.replace(transactionAttributeCache, trxId, transactionAttributeEntity, getExpiryInSeconds(transactionAttributeEntity), TimeUnit.SECONDS);
    }

    @Override
    public void removeTrx(String trxId) {
        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);
        if (transactionAttributeEntity != null) {
            this.tx.remove(transactionAttributeCache, trxId);
        }
        removeTransaction(trxId);
    }

    @Override
    public TrxModel getTrx(String trxId) {
        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);
        return entityToModel(trxId, transactionAttributeEntity);
    }

    @Override
    public String getTrxValue(String trxId, String name) {
        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);
        if (transactionAttributeEntity != null) {
            return transactionAttributeEntity.getTrxAttributes().get(name);
        }
        return null;
    }

    @Override
    public boolean hasTrxValueAttribute(String trxId, String name) {
        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);
        if (transactionAttributeEntity != null) {
            return transactionAttributeEntity.getTrxAttributes().containsKey(name);
        }
        return false;
    }

    @Override
    public boolean hasTrxValueAttribute(String trxId, String name, String expectedValue) {
        TransactionAttributeEntity transactionAttributeEntity = getTransactionAttributeEntity(trxId);
        if (transactionAttributeEntity != null) {
            return transactionAttributeEntity.getTrxAttributes() != null
                    && transactionAttributeEntity.getTrxAttributes().containsKey(name)
                    && transactionAttributeEntity.getTrxAttributes().get(name).equalsIgnoreCase(expectedValue);
        }
        return false;
    }

    @Override
    public void close() {
        //NOP
    }

    public void persistAttributeValue(String name, String value, String trxId) {
        TypedQuery<TrxAttributeEntity> query = getEntityManager().createNamedQuery("findByTrxIdAndName", TrxAttributeEntity.class);
        query.setParameter("name", name);
        query.setParameter("trxId", trxId);

        List<TrxAttributeEntity> existingAttributes = query.getResultList();

        if (!existingAttributes.isEmpty()) {
            TrxAttributeEntity existingAttribute = existingAttributes.get(0);
            existingAttribute.setValue(value);
            getEntityManager().merge(existingAttribute);
        } else {
            TrxAttributeEntity attr = new TrxAttributeEntity();
            attr.setId(KeycloakModelUtils.generateId());
            attr.setName(name);
            attr.setValue(value);
            attr.setTrxId(trxId);
            getEntityManager().persist(attr);
        }
    }

    public void removeTransactionAttribute(String trxId, String name) {
        String trxValue = getTrxValue(trxId, name);
        if(trxValue != null) {
            Query query = getEntityManager().createNativeQuery(
                    "DELETE FROM TRX_ATTRIBUTE WHERE TRX_ID = :trxId AND NAME = :name"
            );
            query.setParameter("trxId", trxId);
            query.setParameter("name", name);
            query.executeUpdate();
        }
    }

    public void removeTransaction(String trxId) {
        Query query = getEntityManager().createNativeQuery("DELETE FROM TRX_ATTRIBUTE WHERE TRX_ID = :trxId");
        query.setParameter("trxId", trxId);
        query.executeUpdate();
    }

    private TrxModel entityToModel(String trxId, TransactionAttributeEntity entity) {
        if (entity == null)
            return null;
        TrxModel model = new TrxModel();
        model.setTrxId(trxId);
        model.setAttributes(entity.getTrxAttributes());
        model.setCreatedAt(entity.getCreatedAt());

        return model;
    }

    private TransactionAttributeEntity getTransactionAttributeEntity(String trxId) {
        TransactionAttributeEntity cached = this.tx.get(transactionAttributeCache, trxId);
        if (cached != null) {
            return cached;
        }

        List<Object[]> rows = getEntityManager().createQuery(
                        "SELECT t.name, t.value FROM TrxAttributeEntity t WHERE t.trxId = :trxId", Object[].class)
                .setParameter("trxId", trxId)
                .getResultList();

        Map<String, String> map = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            String name  = (String) row[0];
            String value = (String) row[1];
            map.put(name, value);
        }

        TransactionAttributeEntity txEntity =
                new TransactionAttributeEntity(map, System.currentTimeMillis());
        try {
            logger.info(String.format("Caching trxId={} with attribute entity: {}", trxId, txEntity));
            this.tx.put(transactionAttributeCache, trxId, txEntity);
        } catch (IllegalStateException e) {
            logger.warn(String.format("Skipped caching for trxId={} due to active session task", trxId));
        }

        return txEntity;
    }



    private Long getExpiryInSeconds(TransactionAttributeEntity transactionAttributeEntity) {
        boolean linkExp = transactionAttributeEntity != null
                && transactionAttributeEntity.getTrxAttributes() != null
                && transactionAttributeEntity.getTrxAttributes().containsKey("linkExp");

        if (linkExp ) {
            long currentTimeInSeconds = System.currentTimeMillis() / 1000;
            long expiryTimeInSeconds = Long.parseLong(transactionAttributeEntity.getTrxAttributes().get("linkExp"));
            if (expiryTimeInSeconds > currentTimeInSeconds) {
                return expiryTimeInSeconds - currentTimeInSeconds;
            }

            return -1L;
        }

        return -1L;
    }


}
