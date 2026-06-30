package com.qlikverify.extensions.admin.controllers.workflows.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.extensions.admin.controllers.workflows.jpa.WorkflowsEntity;
import com.qlikverify.extensions.admin.controllers.workflows.jpa.WorkflowsEntityResource;
import com.qlikverify.extensions.admin.controllers.workflows.misc.WorkflowsFlowModel;
import com.qlikverify.extensions.admin.misc.helpers.JsonUtil;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static com.qlikverify.extensions.admin.controllers.workflows.misc.Constants.*;

public class WorkflowsResourceFunction implements WorkflowsEntityResource {

    //~ Static fields/initializers =========================================================================================================

    private static final Logger logger = Logger.getLogger(WorkflowsResourceFunction.class);
    private static final String LOG_TAG = "WORKFLOWS::RESOURCE_FUNCTIONS: ";
    private final KeycloakSession session;
    private final EntityManager em;
    private final CriteriaBuilder builder;
    private final RealmModel realm;
    private final EventBuilder eventBuilder;

    //~ Constructor =======================================================================================================================

    public WorkflowsResourceFunction(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.builder = em.getCriteriaBuilder();
        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, null);
    }

    //~ Methods ============================================================================================================================

    @Override
    public String getConnectedApplicationsMetadata(String type) {
        CriteriaQuery<Object[]> criteriaQuery = builder.createQuery(Object[].class);
        Root<WorkflowsEntity> root = criteriaQuery.from(WorkflowsEntity.class);

        Predicate eventTypePredicate = builder.equal(root.get(EVENT_TYPE), type.equals("role") ? CREATE_ROLE : CREATE_USER);
        Predicate connectedAppPredicate = builder.notEqual(root.get(CONNECTED_APP), GITLAB); // since gitlab not supported for roles manipulation
        Predicate realmIdPredicate = builder.equal(root.get(REALM_ID), realm.getId());

        Predicate finalPredicate = builder.and(eventTypePredicate, connectedAppPredicate, realmIdPredicate);

        criteriaQuery.select(builder.array(
                root.get(FLOW_ID),
                root.get(FLOW_NAME),
                root.get(CONNECTED_APP),
                root.get(METADATA)
        )).where(finalPredicate);

        criteriaQuery.where(finalPredicate);

        try {
            List<Map<String, Object>> jsonList = new ArrayList<>();
            em.createQuery(criteriaQuery).getResultList().forEach(workflowEntity -> {
                Map<String, Object> jsonMap = new LinkedHashMap<>();
                jsonMap.put(FLOW_ID, workflowEntity[0]);
                jsonMap.put(FLOW_NAME, workflowEntity[1]);
                jsonMap.put(CONNECTED_APP, workflowEntity[2]);
                String metaDataString = (String) workflowEntity[3];
                if (metaDataString != null) {
                    try {
                        Map<String, Object> metaDataMap = JsonSerialization.readValue(metaDataString, new TypeReference<Map<String,Object>>() {});
                        jsonMap.put(METADATA, metaDataMap);
                    } catch (IOException e) {
                        logger.error("Failed to parse metaData as JSON: " + e.getMessage());
                    }
                } else {
                    jsonMap.put(METADATA, "");
                }
                jsonList.add(jsonMap);
            });
            return JsonUtil.getAsJsonPrettyString(jsonList);
        } catch (NoResultException e) {
            return "";
        }
    }

    //~ ====================================================================================================================================

    @Override
    public List<WorkflowsEntity> getAllWorkflowEntries(RealmModel realm) {
        CriteriaQuery<WorkflowsEntity> criteriaQuery = builder.createQuery(WorkflowsEntity.class);
        Root<WorkflowsEntity> root = criteriaQuery.from(WorkflowsEntity.class);

        Predicate realmIdPredicate = builder.equal(root.get(REALM_ID), realm.getId());

        criteriaQuery.where(realmIdPredicate);
        criteriaQuery.orderBy(builder.asc(root.get("flowId")), builder.asc(root.get("id"))); // to ensure the order is same for every fetch

        return em.createQuery(criteriaQuery).getResultList();
    }

    //~ ====================================================================================================================================

    @Override
    public List<WorkflowsEntity> getAllWorkflowEntries(RealmModel realm, String connectedApp, String eventType) {
        if(Validation.isBlank(eventType)) {
            logger.error(LOG_TAG + "Event type cannot be empty");
            return new ArrayList<>();
        }
        CriteriaQuery<WorkflowsEntity> criteriaQuery = builder.createQuery(WorkflowsEntity.class);
        Root<WorkflowsEntity> root = criteriaQuery.from(WorkflowsEntity.class);

        Predicate realmIdPredicate = builder.equal(root.get(REALM_ID), realm.getId());
        Predicate flowNamePredicate = builder.equal(root.get(CONNECTED_APP), connectedApp);
        Predicate eventTypePredicate = builder.equal(root.get(EVENT_TYPE), eventType);

        criteriaQuery.where(builder.and(realmIdPredicate, flowNamePredicate, eventTypePredicate));

        return em.createQuery(criteriaQuery).getResultList();
    }

    //~ ====================================================================================================================================

    @Override
    public Response addNewWorkflowEntry(WorkflowsFlowModel workflowsFlowModel) {
        try {
            logger.info(LOG_TAG + "Data Received : " + JsonUtil.getAsJsonPrettyString(workflowsFlowModel));
            List<String> connectedApps = new ArrayList<>();
            if(realm.getAttribute(WORKFLOWS_CONNECTED_APPS) != null) {
                connectedApps = JsonSerialization.readValue(realm.getAttribute(WORKFLOWS_CONNECTED_APPS), new TypeReference<List<String>>(){});
                if (connectedApps.isEmpty() || !connectedApps.contains(workflowsFlowModel.getConnectedApp())) {
                    connectedApps.add(workflowsFlowModel.getConnectedApp());
                }
            } else {
                connectedApps.add(workflowsFlowModel.getConnectedApp());
            }
            realm.setAttribute(WORKFLOWS_CONNECTED_APPS, JsonSerialization.writeValueAsString(connectedApps));

            CriteriaQuery<WorkflowsEntity> criteriaQuery = builder.createQuery(WorkflowsEntity.class);
            Root<WorkflowsEntity> root = criteriaQuery.from(WorkflowsEntity.class);

            Predicate realmIdPredicate = builder.equal(root.get(FLOW_ID), workflowsFlowModel.getFlowId());

            criteriaQuery.where(builder.and(realmIdPredicate));

            WorkflowsEntity workflowsEntity = em.createQuery(criteriaQuery).getResultStream().findFirst().orElse(null);

            if (workflowsEntity == null) {
                workflowsEntity = new WorkflowsEntity();
                workflowsEntity.setRealmId(realm.getId());
            }
            workflowsEntity.setFlowId(workflowsFlowModel.getFlowId());
            workflowsEntity.setWebhookUrl(workflowsFlowModel.getWebhookUrl());
            workflowsEntity.setEventType(workflowsFlowModel.getEventType());
            workflowsEntity.setFlowName(workflowsFlowModel.getFlowName());
            workflowsEntity.setIconUrl(workflowsFlowModel.getIconUrl());
            workflowsEntity.setConnectedApp(workflowsFlowModel.getConnectedApp());
            workflowsEntity.setCreatedTime(String.valueOf(System.currentTimeMillis()));
            workflowsEntity.setRealmId(realm.getId());
            workflowsEntity.setMetaData(JsonSerialization.writeValueAsString(workflowsFlowModel.getMetaData()));

            if (workflowsEntity.getId() == null) {
                em.persist(workflowsEntity);
            } else {
                em.merge(workflowsEntity);
            }
            em.flush();
            logger.info(LOG_TAG + "Workflows entry added for - " + workflowsFlowModel.getConnectedApp());
            return Response.accepted().type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            logger.error(LOG_TAG + "An error occurred while adding a new Workflows entry: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity("An error occurred while adding a new Workflows entry. Please try again later.").build();
        }
    }

    //~ ====================================================================================================================================

    @Override
    public Response updateWorkflowEntry(RealmModel realm, Map<String, Object> postData) {
        try {
            CriteriaQuery<WorkflowsEntity> criteriaQuery = builder.createQuery(WorkflowsEntity.class);
            Root<WorkflowsEntity> root = criteriaQuery.from(WorkflowsEntity.class);

            Predicate realmIdPredicate = builder.equal(root.get(REALM_ID), realm.getId());
            Predicate flowIdPredicate = builder.equal(root.get(FLOW_ID), postData.get(FLOW_ID).toString());

            criteriaQuery.where(builder.and(realmIdPredicate, flowIdPredicate));

            WorkflowsEntity workflowsEntity = em.createQuery(criteriaQuery).getSingleResult();

            if (workflowsEntity != null) {
                workflowsEntity.setSubscribed((boolean) postData.get(WORKFLOWS_SUBSCRIBED));

                em.merge(workflowsEntity);
                em.flush();
                logger.info(LOG_TAG + "Workflow entry updated - " + postData.get(FLOW_ID).toString());
                return Response.ok().type(MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("WorkflowEntity not found").type(MediaType.APPLICATION_JSON).build();
            }
        } catch(Exception e) {
            logger.error(LOG_TAG + "An error occurred while updating Workflows entry: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity("An error occurred while updating Workflows entry. Please try again later.").build();
        }
    }

    //~ ====================================================================================================================================

    @Override
    public Response deleteWorkflowEntry(WorkflowsFlowModel deleteData) {
        logger.infov(LOG_TAG + "workflow is unpublished for {0}, {1} - ", deleteData.getConnectedApp(), JsonUtil.getAsJsonPrettyString(deleteData));
        String flowId = deleteData.getFlowId();
        try {
            CriteriaQuery<WorkflowsEntity> criteriaQuery = builder.createQuery(WorkflowsEntity.class);
            Root<WorkflowsEntity> root = criteriaQuery.from(WorkflowsEntity.class);

            Predicate realmIdPredicate = builder.equal(root.get(REALM_ID), realm.getId());
            Predicate flowIdPredicate = builder.equal(root.get(FLOW_ID), flowId);

            criteriaQuery.where(builder.and(realmIdPredicate, flowIdPredicate));

            WorkflowsEntity workflowsEntity = em.createQuery(criteriaQuery).getSingleResult();

            if (workflowsEntity != null) {
                em.remove(workflowsEntity);
                em.flush();
                return Response.ok().type(MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity(WORK_FLOWS_ENTITY_NOT_FOUND).type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            logger.error(LOG_TAG + "An error occurred while deleting Workflows entry: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity("An error occurred while deleting Workflows entry. Please try again later.").build();
        }
    }

    //~ ====================================================================================================================================

    @Override
    public String getWorkflowRoleDetails(String flowName) {
        CriteriaQuery<WorkflowsEntity> criteriaQuery = builder.createQuery(WorkflowsEntity.class);
        Root<WorkflowsEntity> root = criteriaQuery.from(WorkflowsEntity.class);

        Predicate realmIdPredicate = builder.equal(root.get(FLOW_NAME), flowName);

        criteriaQuery.where(realmIdPredicate);

        WorkflowsEntity workflowsEntity = em.createQuery(criteriaQuery).getSingleResult();
        return workflowsEntity.getMetaData();
    }

    //~ ====================================================================================================================================

    @Override
    public Response postWorkflowRoleDetails(Map<String, Object> roleDetails, String userId) {
        UserModel user = session.users().getUserById(realm, userId);
        if(user == null)
            return Response.status(400).build();
        if(roleDetails.containsKey("organizations"))
            user.setAttribute("organizations", (ArrayList)roleDetails.get("organizations"));
        if(roleDetails.containsKey("projects"))
            user.setAttribute("projects", (ArrayList)roleDetails.get("projects"));
        return Response.ok().build();
    }
}
