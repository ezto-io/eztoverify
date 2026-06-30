package com.qlikverify.extensions.admin.controllers.transactionStatus.spi;

import com.qlikverify.userTransactionStatus.spi.UserTransactionStatusProvider;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserTransactionModel;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.UserTransactionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserTransactionStatusProviderImpl implements UserTransactionStatusProvider {

    private final KeycloakSession session;
    private static final Logger logger = Logger.getLogger(UserTransactionStatusProviderImpl.class);

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public UserTransactionStatusProviderImpl(KeycloakSession session) {
        this.session = session;
        if (session.getContext().getRealm()== null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }

        if (!session.getContext().getRealm().getMode().equalsIgnoreCase("bo")) {
            throw new IllegalStateException("User transaction status not supported for WIAM/CIAM");
        }
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public UserTransactionModel addTransactionStatus(String realmId, String trxId, String userId, String status, Long expiresAt) {
        UserTransactionEntity userTransactionEntity = addUserTransactionToEntity(realmId, trxId, userId, status, expiresAt);
        return transactionEntityToModel(userTransactionEntity);
    }

    @Override
    public UserTransactionModel getUserTransactionDetails(String trxId, String userId) {
        return transactionEntityToModel(getUserTransactionEntity(trxId, userId));
    }

    @Override
    public void updateTransactionStatus(String realmId, String trxId, String userId, String status) {
        Query query = getEntityManager().createNamedQuery("updateTrxStatus");
        query.setParameter("trxId", trxId);
        query.setParameter("userId", userId);
        query.setParameter("realmId", realmId);
        query.setParameter("currentStatus", status);

        query.executeUpdate();
    }

    @Override
    public List<UserTransactionModel> getUserTransactionStatus(String realmId, String userId) {
        List<UserTransactionEntity> entity = getUserTransactionEntityByUserIdAndRealmId(realmId, userId);

        return entity.stream().filter(Objects::nonNull)
                .map(userTransactionEntity -> {
                    UserTransactionModel model = new UserTransactionModel();
                    model.setStatus(userTransactionEntity.getStatus());
                    model.setUserId(userTransactionEntity.getUserId());
                    model.setTrxId(userTransactionEntity.getTrxId());
                    model.setCreatedAt(userTransactionEntity.getCreatedTimestamp());
                    model.setExpiresAt(userTransactionEntity.getExpiresAt());
                    return model;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void removeUserTransactionStatus(String realmId, String userId) {
        Query query = getEntityManager().createNamedQuery("removeTrxStatus");
        query.setParameter("userId", userId);
        query.setParameter("realmId", realmId);

        query.executeUpdate();
    }

    private UserTransactionModel transactionEntityToModel(UserTransactionEntity entity) {
        if (entity == null)
            return null;
        UserTransactionModel model = new UserTransactionModel();
        model.setTrxId(entity.getTrxId());
        model.setUserId(entity.getUserId());
        model.setStatus(entity.getStatus());
        model.setCreatedAt(entity.getCreatedTimestamp());
        model.setExpiresAt(entity.getExpiresAt());
        return model;
    }

    private UserTransactionEntity addUserTransactionToEntity(String realmId, String trxId, String userId, String status, Long expiresAt) {
        UserTransactionEntity userTransactionEntity = new UserTransactionEntity();
        userTransactionEntity.setRealmId(realmId);
        userTransactionEntity.setId(KeycloakModelUtils.generateId());
        userTransactionEntity.setTrxId(trxId);
        userTransactionEntity.setStatus(status);
        userTransactionEntity.setUserId(userId);
        userTransactionEntity.setExpiresAt(expiresAt);
        getEntityManager().persist(userTransactionEntity);

        return userTransactionEntity;
    }

    public UserTransactionEntity getUserTransactionEntity(String trxId, String userId) {
        EntityManager em = getEntityManager();
        if (em == null || !em.isOpen()) {
            logger.error("EntityManager is closed or null");
            return null;
        }

        try {
            em.setFlushMode(FlushModeType.COMMIT);
            Query nativeQuery = em.createNativeQuery(
                    "SELECT * FROM USER_TRANSACTIONS WHERE TRX_ID = :trxId AND USER_ID = :userId",
                    UserTransactionEntity.class
            );
            nativeQuery.setParameter("trxId", trxId);
            nativeQuery.setParameter("userId", userId);
            List<UserTransactionEntity> resultList = nativeQuery.getResultList();
            return resultList.isEmpty() ? null : resultList.get(0);
        } catch (Exception e) {
            logger.error("Native query failed", e);
            return null;
        }
    }

    private List<UserTransactionEntity> getUserTransactionEntityByUserIdAndRealmId(String realmId, String userId) {
        TypedQuery<UserTransactionEntity> query = getEntityManager().createNamedQuery("findByUserIdAndRealmId", UserTransactionEntity.class);
        query.setParameter("userId", userId);
        query.setParameter("realmId", realmId);

        return query.getResultList();
    }
}
