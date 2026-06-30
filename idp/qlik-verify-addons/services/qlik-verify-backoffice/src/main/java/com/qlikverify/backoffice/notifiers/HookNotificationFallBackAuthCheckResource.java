package com.qlikverify.backoffice.notifiers;

import com.qlikverify.backoffice.models.hookResponseModel.HookResponseModel;
import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.JsonWebToken;
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

import static com.qlikverify.backoffice.Constants.FAILED_HOOK_ATTEMPTS;
import static com.qlikverify.backoffice.Constants.FAILED_HOOK_ATTEMPTS_FOR_DEAD_LETTER;
import static com.qlikverify.backoffice.helpers.GeneralHelper.triggerHookFailedEmailNotification;
import static com.qlikverify.helpers.GeneralHelper.isMaximumDeadLetterHookFailureReached;
import static com.qlikverify.helpers.GeneralHelper.isMaximumHookFailureReached;

public class HookNotificationFallBackAuthCheckResource extends BaseAuthCheckResource {
    public HookNotificationFallBackAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @POST
    @Path("/result")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNotification(@Context HttpRequest req) {
        BackgroundJobActionTokenVerifier tokenVerifier = new BackgroundJobActionTokenVerifier(session,
                session.getContext().getRealm());
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if (actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        try {
            String body = getBody(req);
            HookResponseModel hookResponseModel = JsonSerialization.readValue(body, HookResponseModel.class);
            JsonWebToken jsonWebToken = tokenVerifier.validateToken(actionToken.split(" ")[1],
                    hookResponseModel.getRealmId(),
                    false);
            if (jsonWebToken == null || jsonWebToken.isExpired()) {
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }
            RealmModel realm = session.realms().getRealm(hookResponseModel.getRealmId());
            session.getContext().setRealm(realm);
            final ClientModel client = session.clients().getClientByClientId(realm, hookResponseModel.getClientId());
            session.getContext().setClient(client);
            if (client == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Client not found").build();
            }

            String hookKey = hookResponseModel.getMessage().equalsIgnoreCase("deadLetterHookFailure") ? FAILED_HOOK_ATTEMPTS_FOR_DEAD_LETTER : FAILED_HOOK_ATTEMPTS;

            if (hookResponseModel.isSuccess()) {
                if (!client.isEnabled()) {
                    client.setEnabled(true);
                }
                if (client.getAttributes().containsKey(hookKey)) {
                    client.removeAttribute(hookKey);
                }
                return Response.ok().entity("ok").build();
            } else {
                final String attempts = client.getAttributes().get(hookKey);
                int numberOfAttempts = attempts == null? 0 : Integer.parseInt(attempts);
                client.setAttribute(hookKey, String.valueOf(numberOfAttempts + 1));
            }

            if(!hookKey.equalsIgnoreCase(FAILED_HOOK_ATTEMPTS_FOR_DEAD_LETTER)) {
                if (isMaximumHookFailureReached(session, realm, client)) {
                    client.setEnabled(false);
                    triggerHookFailedEmailNotification(session, realm, client, hookKey);
                }
            } else {
                if (isMaximumDeadLetterHookFailureReached(session, realm, client)) {
                    triggerHookFailedEmailNotification(session, realm, client, hookKey);
                }
            }

        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok().entity("ok").build();
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
}
