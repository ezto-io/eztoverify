package com.qlikverify.extensions.admin.controllers.transactionState.spi;

import com.qlikverify.transactionState.spi.TransactionStateProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.CibaStateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.jpa.entities.CibaStateEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.Optional;

@Transactional
public class TransactionStateProviderImpl implements TransactionStateProvider {
    private final KeycloakSession session;

    public TransactionStateProviderImpl(KeycloakSession session) {
        this.session = session;
        if (session.getContext().getRealm()== null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }

        if (!session.getContext().getRealm().getMode().equalsIgnoreCase("bo")) {
            throw new IllegalStateException("Transaction state not supported for WIAM/CIAM");
        }
    }

    private EntityManager getEntityManager() {
        KeycloakSessionFactory keycloakSessionFactory = session.getKeycloakSessionFactory();
        KeycloakSession keycloakSession = keycloakSessionFactory.create();
        return keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public CibaStateModel addCibaState(CibaStateModel model) {
        CibaStateEntity cibaState = new CibaStateEntity();
        String id = (model.getCibaStateId() == null) ? KeycloakModelUtils.generateId() : model.getCibaStateId();
        cibaState.setCibaStateId(id);
        cibaState.setCibaToken(model.getCibaToken());
        cibaState.setUserId(model.getUserId());
        cibaState.setRequestId(model.getRequestId());
        cibaState.setCibaStateId(id);

        getEntityManager().persist(cibaState);
        model.setCibaStateId(id);
        return model;
    }

    @Override
    public void removeCibaState(CibaStateModel model) {
        Query removeByCibaState = getEntityManager().createNamedQuery("removeByCibaState");
        removeByCibaState.setParameter("cibaStateId", model.getCibaStateId());
        removeByCibaState.executeUpdate();
    }

    @Override
    public CibaStateModel getCibaStateById(String cibaStateId) {
        CibaStateEntity entity = getCibaStateEntity(cibaStateId);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public void close() {
        //NOP
    }

    private CibaStateEntity getCibaStateEntity(String id) {
        TypedQuery<CibaStateEntity> query = getEntityManager().createNamedQuery("findByCibaState", CibaStateEntity.class);
        query.setParameter("cibaStateId", id);

        Optional<CibaStateEntity> entity = query.getResultList().stream().findFirst();
        return entity.orElse(null);
    }

    private CibaStateModel entityToModel(CibaStateEntity entity) {
        CibaStateModel model = new CibaStateModel();
        model.setCibaStateId(entity.getCibaStateId());
        model.setCibaToken(entity.getCibaToken());
        model.setUserId(entity.getUserId());
        model.setRequestId(entity.getRequestId());
        return model;
    }
}
