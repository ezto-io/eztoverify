package com.qlikverify.extensions.admin.controllers.notification;

import com.qlikverify.extensions.admin.controllers.workflows.misc.helper.WorkflowHelper;
import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.userSync.helpers.SyncRunner;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static com.qlikverify.extensions.admin.misc.models.Constants.*;
import static org.keycloak.events.EventType.WORKFLOW_ERROR;
import static org.keycloak.events.EventType.WORKFLOW_SUCCEEDED;

public class NotificationAuthCheckResource extends BaseAuthCheckResource{
    public NotificationAuthCheckResource(KeycloakSession session) {
        super(session);
    }
    protected static final Logger logger = Logger.getLogger(NotificationAuthCheckResource.class);

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNotification(@Context HttpRequest req){

        UserRepresentation rep = new UserRepresentation();
        try {
            String body = getBody(req);
            Map<String, String> map = JsonSerialization.readValue(body, Map.class);
            logger.info(JsonSerialization.writeValueAsPrettyString(map));
            if(!(map.get(NOTIFICATION_TYPE).equals("WORKFLOW_SUCCEEDS") || map.get(NOTIFICATION_TYPE).equals(WORKFLOW_ERROR.name()))) {
                BackgroundJobActionTokenVerifier tokenVerifier = new BackgroundJobActionTokenVerifier(session, session.getContext().getRealm());
                String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
                if (actionToken == null) {
                    return  Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
                }
                JsonWebToken jsonWebToken = tokenVerifier.validateToken(actionToken.split(" ")[1], map.get("realmId"), false);
                if (jsonWebToken == null || jsonWebToken.isExpired()) {
                    return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
                }
            }
            RealmModel realm = null;
            if (map.containsKey("realmId")) {
                 realm = session.realms().getRealm(map.get("realmId"));
            }

            if (map.containsKey("realmName")) {
                realm = session.realms().getRealmByName(map.get("realmName"));
            }
        
            
            session.getContext().setRealm(realm);

            if (map.get(NOTIFICATION_TYPE) != null) {
                RealmModel realmModel = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
                String notificationType = map.get(NOTIFICATION_TYPE);
                String statusCode = String.valueOf(map.get("statusCode"));
                switch (notificationType) {
                    case "HARD_DELETE":
                        map.put("Body", "Workspace was deleted successfully");
                        if(Integer.parseInt(statusCode) !=200) {
                            map.put("Body", "Failed to delete Workspace");
                        }
                        triggerHardDeleteAuditLogs(realmModel, map);
                        break;
                    case "SOFT_DELETE":
                        map.put("Body", "Workspace is scheduled for deletion");
                        if(Integer.parseInt(statusCode) !=200) {
                            map.put("Body", "Failed to schedule deletion of Workspace");
                        }
                        triggerHardDeleteAuditLogs(realmModel, map);
                        break;
                    case "SOFT_DELETE_SUCCESS":
                        map.put("Body", "Workspace was deleted successfully as per schedule");
                        if(Integer.parseInt(statusCode) !=200) {
                            map.put("Body", "Failed to delete Workspace");
                        }
                        triggerHardDeleteAuditLogs(realmModel, map);
                        break;
                    case "REVOKE_SOFT_DELETE":
                        map.put("Body", "Revoke soft delete - success");
                        if(Integer.parseInt(statusCode) !=200) {
                            map.put("Body", "Revoke soft delete - failed");
                        }
                        triggerHardDeleteAuditLogs(realmModel, map);
                        break;
                    case "WORKFLOW_SUCCEEDS":
                    case "WORKFLOW_ERROR":
                        triggerWorkflowsAuditLogs(realm, map, notificationType);
                        break;
                    default:
                        break;
                }
            }

            if (map.get(NOTIFICATION_TYPE) != null && map.get(NOTIFICATION_TYPE).equals(EventType.NOTIFICATION_ERROR.toString()) && map.get(IS_ADMIN_EVENT).equals("false")){
                EventBuilder eventBuilder = new EventBuilder(realm, session, null);
                eventBuilder.user(session.users().getUserByEmail(realm, map.get(EMAIL)).getId());
                if(map.get(CLIENT_NAME) != null){
                    Optional<String> clientId = realm.getClientsStream().filter(clientModel -> clientModel.getName().equals(map.get(CLIENT_NAME))).map(ClientModel::getClientId).findFirst();
                    if (clientId.isPresent()) {
                        eventBuilder.client(clientId.get());
                    } else {
                        eventBuilder.client(map.get(CLIENT_NAME));
                    }
                }
                eventBuilder.event(EventType.NOTIFICATION_ERROR);
                eventBuilder.error(map.get(ERROR_MESSAGE) == null ? "": map.get(ERROR_MESSAGE));
            }
            else if(map.get(NOTIFICATION_TYPE) != null && map.get(NOTIFICATION_TYPE).equals(EventType.NOTIFICATION_ERROR.toString()) && map.get(IS_ADMIN_EVENT).equals("true")){
                UserModel user = session.users().getUserByEmail(realm, map.get(EMAIL));
                if(user != null){
                    rep.setUsername(user.getUsername());
                }else{
                    rep.setUsername(String.valueOf(map.get(EMAIL)));
                    user = session.users().getUserById(realm, String.valueOf(map.get(EMAIL)));
                }
                rep.setId(user.getId());
                rep.setUsername(user.getUsername());
                List<String> att = new ArrayList<>();
                att.add(map.get(ERROR_MESSAGE));

                Map<String, List<String>> attributes = new HashMap<>();
                attributes.put("details", att);

                rep.setAttributes(attributes);
                ClientModel client  = session.clients().getClientByClientId(realm, DASHBOARD);

                AdminEventBuilder adEvt = new AdminEventBuilder(session.getContext().getRealm(),  new AdminAuth(session.getContext().getRealm(),
                        null,user, client), session,
                        session.getContext().getConnection());
                        adEvt.realm(realm);
                        adEvt.operation(OperationType.NOTIFICATION_ERROR)
                        .resource(ResourceType.NOTIFICATION)
                        .resourcePath(session.getContext().getUri())
                        .representation(rep)
                        .authUser(session.users().getUserByEmail(realm, map.get(EMAIL)))
                        .success();
            }

            if (map.get(NOTIFICATION_TYPE) != null && map.get(NOTIFICATION_TYPE).equals(EventType.NOTIFICATION.toString()) && map.get(IS_ADMIN_EVENT).equals("false")){
                EventBuilder eventBuilder = new EventBuilder(realm, session, null);
                eventBuilder.user(session.users().getUserByEmail(realm, map.get(EMAIL)).getId());
                if(map.get(CLIENT_NAME) != null){
                    Optional<String> clientId = realm.getClientsStream().filter(clientModel -> clientModel.getName().equals(map.get(CLIENT_NAME))).map(ClientModel::getClientId).findFirst();
                    if (clientId.isPresent()) {
                        eventBuilder.client(clientId.get());
                    } else {
                        eventBuilder.client(map.get(CLIENT_NAME));
                    }
                }
                eventBuilder.event(EventType.NOTIFICATION);
                eventBuilder.detail("Status", "Mail sent successfully");
                eventBuilder.realm(realm);
                eventBuilder.success();
            }else if(map.get(NOTIFICATION_TYPE) != null && map.get(NOTIFICATION_TYPE).equals(EventType.NOTIFICATION.toString()) && map.get(IS_ADMIN_EVENT).equals("true")){
                UserModel user = session.users().getUserByEmail(realm, map.get(EMAIL));
                if(user != null){
                    rep.setUsername(user.getUsername());
                }else{
                    rep.setUsername(String.valueOf(map.get(EMAIL)));
                    user = session.users().getUserById(realm, String.valueOf(map.get(EMAIL)));
                }
                List<String> att = new ArrayList<>();
                att.add(map.get(ERROR_MESSAGE));

                Map<String, List<String>> attributes = new HashMap<>();
                attributes.put("details", att);

                rep.setAttributes(attributes);
                ClientModel client  = session.clients().getClientByClientId(realm, DASHBOARD);

                AdminEventBuilder adEvt = new AdminEventBuilder(session.getContext().getRealm(),  new AdminAuth(session.getContext().getRealm(),
                        null,user, client), session,
                        session.getContext().getConnection());
                adEvt.realm(realm);
                adEvt.operation(OperationType.NOTIFICATION)
                        .resource(ResourceType.NOTIFICATION)
                        .resourcePath(session.getContext().getUri())
                        .representation(rep)
                        .authUser(session.users().getUserByEmail(realm, map.get(EMAIL)))
                        .success();
            }
        } catch (IOException e) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
            throw new RuntimeException(e);
        }

        return null;
    }

    @POST
    @Path("/delete-tenant")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeTenant(Map<String, String> request){
        BackgroundJobActionTokenVerifier tokenVerifier = new BackgroundJobActionTokenVerifier(session, session.getContext().getRealm());
        if(!request.containsKey("actionToken")){
            return  Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        if(!request.containsKey("tenant_id")){
            return ErrorResponse.error("Tenant ID is missing", Response.Status.BAD_REQUEST);
        }
        String actionToken = request.get("actionToken");
        if (actionToken == null){
            return  Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        JsonWebToken jsonWebToken = tokenVerifier.validateToken(actionToken, request.get("tenant_id"), false);
        if (jsonWebToken == null || jsonWebToken.isExpired()) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        if (!new RealmManager(session).removeRealm(session.realms().getRealm(request.get("tenant_id")))) {
            return ErrorResponse.error("Realm doesn't exist", Response.Status.BAD_REQUEST);
        }
        return Response.ok().build();
    }

    public static String getBody(HttpRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        return body;
    }

    public void triggerHardDeleteAuditLogs(RealmModel realm, Map<String, String> map){
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Body", map.get("Body"));
        attributes.put("Error Message", map.get(ERROR_MESSAGE));
        attributes.put("Workspace Id", map.get("realmId"));

        ClientModel client = session.clients().getClientByClientId(realm, DASHBOARD);

        AdminEventBuilder adEvt = new AdminEventBuilder(realm, new AdminAuth(realm,
                null, null, client), session,
                session.getContext().getConnection());
        adEvt.realm(realm);
        adEvt.operation(OperationType.NOTIFICATION)
                .resource(ResourceType.NOTIFICATION)
                .resourcePath(session.getContext().getUri())
                .representation(attributes)
                .authUser(null)
                .success();

    }

    @POST
    @Path("/sync/result")
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncResult(@Context HttpRequest req) {
        BackgroundJobActionTokenVerifier tokenVerifier = new BackgroundJobActionTokenVerifier(session, session.getContext().getRealm());
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if (actionToken == null){
            return  Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        try {
            String body = getBody(req);
            Map<String, Object> syncResult = JsonSerialization.readValue(body, Map.class);
            JsonWebToken jsonWebToken = tokenVerifier.validateToken(actionToken.split(" ")[1], (String) syncResult.get("realmId"), false);
            if (jsonWebToken == null || jsonWebToken.isExpired()) {
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }
            SyncRunner.syncRunner(session, ((String) syncResult.get("realmId")), (String) syncResult.get("representation"), syncResult.get("syncResult"), (String) syncResult.get("providerId"), (String) syncResult.get("userId"));
        } catch (Exception e){
            EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, null);
            eventBuilder.event(EventType.NOTIFICATION_ERROR);
            eventBuilder.realm(session.getContext().getRealm());
            eventBuilder.error(e.getMessage());
            Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
            throw new RuntimeException(e);
        }
        return null;
    }

    private void triggerWorkflowsAuditLogs(RealmModel realm, Map<String, String> map, String notificationType) throws IOException {
        logger.info("WORKFLOWS::NOTIFICATION:RESPONSE" + JsonSerialization.writeValueAsPrettyString(map));
        AdminEventBuilder adminEventBuilder = new AdminEventBuilder(realm, new AuthDetails(), session, session.getContext().getConnection());
        if (notificationType.equals("WORKFLOW_SUCCEEDS"))
            adminEventBuilder.operation(OperationType.NOTIFICATION).resource(ResourceType.WORKFLOW_SUCCEEDED);
        else
            adminEventBuilder.operation(OperationType.NOTIFICATION).resource(ResourceType.WORKFLOW_ERROR);

        adminEventBuilder
                .representation(map)
                .realm(realm)
                .authUser(WorkflowHelper.getInstance().getAccountCreatorByRealm(session, realm).orElse(null))
                .success();
    }

}
