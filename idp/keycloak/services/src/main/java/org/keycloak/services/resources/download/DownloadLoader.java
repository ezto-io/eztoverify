/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.download;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.account.CorsPreflightService;
import org.keycloak.services.resources.sso.SSOConsole;
import org.keycloak.theme.Theme;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DownloadLoader {

    private static final Logger logger = Logger.getLogger(DownloadLoader.class);

    KeycloakSession session;
    EventBuilder eventBuilder;

    public DownloadLoader(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.eventBuilder =event;
    }

    @Path("/")
    public Object getSSOService() {
        RealmModel realm = session.getContext().getRealm();

        ClientModel client = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (client == null || !client.isEnabled()) {
            logger.debug("Dashboard not enabled");
            throw new NotFoundException("Dashboard not enabled");
        }

        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        HttpHeaders headers = session.getContext().getRequestHeaders();
        MediaType content = headers.getMediaType();
        List<MediaType> accepts = headers.getAcceptableMediaTypes();

        Theme theme = getTheme(session);
        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new CorsPreflightService(request);
        }

        if ((accepts.contains(MediaType.APPLICATION_JSON_TYPE) || MediaType.APPLICATION_JSON_TYPE.equals(content)) && !request.getUri().getPath().endsWith("keycloak.json")) {
            throw new NotFoundException("Dashboard API Service not available");
        }

        DownloadConsole console = new DownloadConsole(realm, client, theme);
        ResteasyProviderFactory.getInstance().injectProperties(console);
        console.init();
        return console;
    }

    private Theme getTheme(KeycloakSession session) {
        try {
            return session.theme().getTheme(Theme.Type.SSO);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

}
