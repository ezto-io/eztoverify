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
package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MimeTypeUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/")
public class UnAuthorizedResource {

    protected static final Logger logger = Logger.getLogger(UnAuthorizedResource.class);

    @Context
    protected HttpHeaders headers;

    @Context
    private KeycloakSession session;

    public UnAuthorizedResource() {
    }

    /**
     * Welcome page of Keycloak
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    public Response getLoginPage() throws URISyntaxException {
        RealmModel master = new RealmManager(session).getKeycloakAdminstrationRealm();
        return Response.status(302).location(
                session.getContext().getUri(UrlType.ADMIN).getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getSSOService").path("/").build(master.getName())
        ).build();
    }

}
