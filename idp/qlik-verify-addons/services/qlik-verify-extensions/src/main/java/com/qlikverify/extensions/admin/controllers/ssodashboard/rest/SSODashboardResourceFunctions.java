package com.qlikverify.extensions.admin.controllers.ssodashboard.rest;

import com.qlikverify.extensions.admin.controllers.ssodashboard.jpa.SSODashboardEntity;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.Response;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;

/**
 * DOCUMENT ME!
 */
public class SSODashboardResourceFunctions {

    //~ Static fields/initializers =========================================================================================================

    private final EntityManager em;
    private final CriteriaBuilder builder;

    //~ Instance fields ====================================================================================================================

    private final RealmModel realm;
    private final UserModel userModel;

    //~ Constructor =======================================================================================================================

    public SSODashboardResourceFunctions(KeycloakSession session, UserModel userModel) {
        this.realm = session.getContext().getRealm();
        this.userModel = userModel;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.builder = em.getCriteriaBuilder();
    }

    //~ Methods ============================================================================================================================

    public Response getUserSSODashboardData() {
        CriteriaQuery<SSODashboardEntity> query = builder.createQuery(SSODashboardEntity.class);

        Root<SSODashboardEntity> root = query.from(SSODashboardEntity.class);
        query.select(root.get("representationJson"));
        query.where(builder.equal(root.get("realmId"), realm.getId())).where(builder.equal(root.get("userId"), userModel.getId()));

        String detailsJson = String.valueOf(em.createQuery(query).getResultList().stream().findFirst().orElse(null));
        if (detailsJson == null || detailsJson.equalsIgnoreCase("null") || detailsJson.isEmpty()) {
            detailsJson = "{}";
        }
        return Response.ok().entity(detailsJson).build();
    }

    //~ ====================================================================================================================================

    public Response updateUserSSODashboardData(String detailsJson) {
        CriteriaQuery<SSODashboardEntity> criteriaQuery = builder.createQuery(SSODashboardEntity.class);
        Root<SSODashboardEntity> root = criteriaQuery.from(SSODashboardEntity.class);

        Predicate realmIdPredicate = builder.equal(root.get("realmId"), realm.getId());
        Predicate userIdPredicate = builder.equal(root.get("userId"), userModel.getId());

        criteriaQuery.where(builder.and(realmIdPredicate, userIdPredicate));

        SSODashboardEntity ssoDashboardEntity = em.createQuery(criteriaQuery).getResultStream().findFirst().orElse(null);

        if (ssoDashboardEntity == null) {
            ssoDashboardEntity = new SSODashboardEntity();
            ssoDashboardEntity.setRealmId(realm.getId());
            ssoDashboardEntity.setUserId(userModel.getId());
        }
        ssoDashboardEntity.setRepresentationJson(detailsJson);

        if (ssoDashboardEntity.getId() == null) {
            em.persist(ssoDashboardEntity);
        } else {
            em.merge(ssoDashboardEntity);
        }
        em.flush();
        return Response.accepted().build();
    }
}
