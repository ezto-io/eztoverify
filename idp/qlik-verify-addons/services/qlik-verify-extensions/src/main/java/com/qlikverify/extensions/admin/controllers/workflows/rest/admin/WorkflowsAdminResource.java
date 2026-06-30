package com.qlikverify.extensions.admin.controllers.workflows.rest.admin;

import com.qlikverify.extensions.admin.controllers.workflows.jpa.WorkflowsEntity;
import com.qlikverify.extensions.admin.controllers.workflows.jpa.WorkflowsEntityResource;
import com.qlikverify.extensions.admin.controllers.workflows.rest.WorkflowsResourceFunction;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.extensions.admin.controllers.workflows.misc.Constants.*;

public class WorkflowsAdminResource {

    private static final Logger logger = Logger.getLogger(WorkflowsAdminResource.class);
    private static final String LOG_TAG = "WORKFLOW::ADMIN_RESOURCE: ";
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    public final AdminEventBuilder adminEvent;
    private final EventBuilder eventBuilder;
    private final WorkflowsEntityResource workflowsEntityResource;

    public WorkflowsAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.eventBuilder = new EventBuilder(realm, session, null);
        this.workflowsEntityResource = new WorkflowsResourceFunction(session);
    }

    @Path("/")
    @GET
    public Map<String, String> getWorkflowUrl() {
        auth.realm().requireManageRealm();

        Map<String, String> result = new HashMap<>();
        String workflowsUrl = EnvironmentReader.getValue(WORKFLOWS_URL);
        if (Validation.isBlank(workflowsUrl)) {
            logger.error(LOG_TAG + "precondition failed, Missing WORKFLOWS_URL");
            result.put("error","precondition failed, Missing WORKFLOWS_URL");
            return result;
        }
        if(realm.getAttribute(WORKFLOWS_CONFIGURED) == null || realm.getAttribute(WORKFLOWS_CONFIGURED).equals("false")) {
            HashMap<String, String> requestData = getWorkflowLoginRequestData();
            if (requestData.isEmpty()) {
                logger.error(LOG_TAG + "request body cannot be empty to trigger Workflows endpoint");
                result.put("error","URL not found");
                return result;
            }
            HttpUtility httpUtility = new HttpUtility();
            try(okhttp3.Response response = httpUtility.callRequest(APPLICATION_JSON, POST, workflowsUrl + "/saml", JsonSerialization.writeValueAsString(requestData), null)) {
                if (!response.isSuccessful()) {
                    logger.error(LOG_TAG + "failed to authenticate to Workflows service. /saml endpoint returned response code " + response.code());
                }
                logger.info(LOG_TAG + "authenticated to Workflows service");
            } catch (IOException e) {
                logger.error(LOG_TAG + "failed to parse json");
            }
            realm.getAttribute(WORKFLOWS_CONFIGURED,"true");
        }
        result.put("url", workflowsUrl + "/login/saml/" + realm.getName());
        return result;
    }

    @Path("/getAllConfiguredWorkflows")
    @GET
    public Map<String, Object> getAllConfiguredWorkflows() {
        Map<String, Object> workflowsMap = new LinkedHashMap<>();
        List<WorkflowsEntity> workflowsEntities = workflowsEntityResource.getAllWorkflowEntries(realm);
        List<Object> workflowsList = new ArrayList<>();
        workflowsEntities.forEach(x-> {
            Map<String, String> availableStreams = new LinkedHashMap<>();
            availableStreams.put(FLOW_ID, x.getFlowId());
            availableStreams.put(FLOW_NAME, x.getFlowName());
            availableStreams.put(ICON_URL, x.getIconUrl());
            availableStreams.put(CONNECTED_APP, x.getConnectedApp());
            availableStreams.put(EVENT_TYPE, x.getEventType());
            availableStreams.put(WORKFLOWS_SUBSCRIBED, String.valueOf(x.isSubscribed()));
            workflowsList.add(availableStreams);
        });
        workflowsMap.put(CONFIGURED_WORKFLOWS_PROVIDERS,workflowsList);
        return workflowsMap;
    }

    @Path("/updateWorkflowConfig")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkflowConfig(Map<String, Object> data) {
        data.put(WORKFLOWS_SUBSCRIBED, Boolean.parseBoolean(data.get(WORKFLOWS_SUBSCRIBED).toString()));
        return workflowsEntityResource.updateWorkflowEntry(session.getContext().getRealm(), data);
    }

    private HashMap<String, String> getWorkflowLoginRequestData() {
        HashMap<String, String> requestData = new HashMap<>();
        requestData.put("name", realm.getName());
        requestData.put("certificate", getCertificate());
        requestData.put("signatureAlgorithm", "sha256");
        requestData.put("issuer",realm.getName());
        requestData.put("firstnameAttributeName", "email");
        requestData.put("surnameAttributeName", realm.getName());
        requestData.put("emailAttributeName", "email");
        requestData.put("roleAttributeName", "Role");
        requestData.put("entryPoint", session.getContext().getAuthServerUrl() +  "realms/" + realm.getName() + "/protocol/saml/" + realm.getName());
        return requestData;
    }

    private String getCertificate() {
        List<String> signingKeys = session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                .sorted(SamlService::compareKeys)
                .map(key -> PemUtils.encodeCertificate(key.getCertificate()))
                .collect(Collectors.toList());
        if(signingKeys.isEmpty()) {
            logger.error(LOG_TAG + "signingKeys not available");
            return null;
        }
        return signingKeys.get(0);
    }
}
