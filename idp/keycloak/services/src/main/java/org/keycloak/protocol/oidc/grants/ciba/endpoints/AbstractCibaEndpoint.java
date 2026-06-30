/*
 * Copyright 2021  Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import javax.ws.rs.core.Response;

import com.qlikverify.helpers.GeneralHelper;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import java.util.HashMap;
import java.util.Map;

import static com.qlikverify.helpers.GeneralHelper.getCibaNotificationConfigModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractCibaEndpoint {

    protected final KeycloakSession session;
    protected final EventBuilder event;
    protected final RealmModel realm;

    public AbstractCibaEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
        realm = session.getContext().getRealm();
    }

    protected ClientModel authenticateClient() {
        checkSsl();
        checkRealm();

        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, null);
        ClientModel client = clientAuth.getClient();

        if (GeneralHelper.isMaximumHookFailureReached(session, realm, client)) {
            if (client.isEnabled()) {
                client.setEnabled(false);
            }
            if (!Boolean.parseBoolean(client.getAttributes().getOrDefault("hook_disabled_mail_sent", "false"))) {
                Map<String, Object> attrs = new HashMap<>();
                attrs.put("applicationName", client.getName());
                attrs.put("numberOfFailures", client.getAttribute("failed_hook_attempts"));
                attrs.put("hookUrl",
                        getCibaNotificationConfigModel(session, realm, client).getConfig().get("notification" +
                                "-endpoint"));
                attrs.put("workspaceName", realm.getName());
                AdminEventBuilder adEvt = new AdminEventBuilder(realm, new AdminAuth(session.getContext().getRealm(),
                        null, null, realm.getClientByClientId("dashboard")), session,
                        session.getContext().getConnection());
                adEvt.realm(realm);
                adEvt.operation(OperationType.HOOK_FAILURE_NOTIFICATION)
                        .resource(ResourceType.NOTIFICATION)
                        .resourcePath(session.getContext().getUri(),
                                client.getId())
                        .representation(attrs).success();
            }

            final String errorMessage = "Application is disabled due to maximum hook delivery failures. Please check" +
                    " the hook associated " +
                    "with the application";

            if (GeneralHelper.isMaximumHookFailureReached(session, realm, client)) {
                event.event(EventType.EZ_TRANSACTION_ERROR);
                event.error(errorMessage);
                throw new ErrorResponseException("Transaction Error",
                        errorMessage,
                        Response.Status.BAD_REQUEST);
            }
        }

        if (client.isBearerOnly()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed",
                    Response.Status.BAD_REQUEST);
        }

        if (!realm.getCibaPolicy().isOIDCCIBAGrantEnabled(client)) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT,
                    "Client not allowed OIDC CIBA Grant", Response.Status.BAD_REQUEST);
        }

        event.client(client.getClientId());

        return client;
    }

    protected void checkSsl() {
        ClientConnection clientConnection = session.getContext().getContextObject(ClientConnection.class);
        RealmModel realm = session.getContext().getRealm();

        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https")
                && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "HTTPS required",
                    Response.Status.FORBIDDEN);
        }
    }

    protected void checkRealm() {
        RealmModel realm = session.getContext().getRealm();

        if (!realm.isEnabled()) {
            throw new ErrorResponseException("access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }
}
