/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MimeTypeUtil;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.EztovrfyMarketplaceCategoryModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.onboarding.OnboardingRepresentation;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;
import org.keycloak.models.Constants;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.keycloak.services.messages.Messages.*;
import static org.keycloak.utils.GeneralHelper.isOnboardingStepRequired;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/")
public class QuarkusWelcomeResource {

    private static final Logger logger = Logger.getLogger(QuarkusWelcomeResource.class);
    private static final String KEYCLOAK_STATE_CHECKER = "WELCOME_STATE_CHECKER";

    private AtomicBoolean shouldBootstrap;

    @Context
    HttpHeaders headers;

    @Context
    HttpRequest request;

    @Context
    KeycloakSession session;

    /**
     * Welcome page of Keycloak
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getWelcomePage() throws URISyntaxException {
        String requestUri = session.getContext().getUri().getRequestUri().toString();
        if (!requestUri.endsWith("/")) {
            return Response.seeOther(new URI(requestUri + "/")).build();
        } else {
            //if onBoarding steps are completed,then redirect to console login
            Response response = checkOnboardingStepsCompleted();
            if (response != null)
                return response;

            //else show welcomeOnboardingSteps
            return createWelcomePage(null, null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response createUser() {
        MultivaluedMap<String, String> formData = request.getDecodedFormParameters();

        Response response = checkOnboardingStepsCompleted();
        if (response != null)
            return response;

        if (!isLocal()) {
            ServicesLogger.LOGGER.rejectedNonLocalAttemptToCreateInitialUser(session.getContext().getConnection().getRemoteAddr());
            throw new ErrorPageException(session, Status.UNAUTHORIZED, "initial-setup-access-denied");
        }

        String providerId = formData.getFirst("stepProviderId");
        if (providerId == null || providerId.isEmpty())
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        RealmModel realm = session.realms().getRealmByName(Config.getAdminRealm());
        return session.onBoarding().onWelcomePostOnboarding(realm, formData, headers, providerId);
    }

    /**
     * Resources for welcome page
     *
     * @param path
     * @return
     */
    @GET
    @Path("/welcome-content/{path}")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getResource(@PathParam("path") String path) {
        try {
            InputStream resource = getTheme().getResourceAsStream(path);
            if (resource != null) {
                String contentType = MimeTypeUtil.getContentType(path);
                Response.ResponseBuilder builder = Response.ok(resource).type(contentType).cacheControl(CacheControlUtil.getDefaultCacheControl());
                return builder.build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Response createWelcomePage(String successMessage, String errorMessage) {
        try {
            Theme theme = getTheme();

            Map<String, Object> map = new HashMap<>();
            Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
            map.put("locale", locale.toLanguageTag());
            Properties messages = theme.getMessages(locale);
            //messages.putAll(realm.getRealmLocalizationTextsByLocale(locale.toLanguageTag()));
            map.put("msg", new MessageFormatterMethod(locale, messages));
            //setup common attrs
            map.put("productName", Version.NAME);
            map.put("productNameFull", Version.NAME_FULL);
            map.put("properties", theme.getProperties());
            map.put("resourceUrl", session.getContext().getUri(UrlType.FRONTEND).getBaseUriBuilder().build());
            map.put("adminUrl", session.getContext().getUri(UrlType.ADMIN).getBaseUriBuilder().path("/admin/").path(
                    session.getContext().getRealm().getName()).path("/console/").build());
            map.put("baseUri", session.getContext().getUri().getBaseUri());
            map.put("resourcesPath", "resources/" + Version.RESOURCES_VERSION + "/" + theme.getType().toString().toLowerCase() + "/" + theme.getName());
            map.put("resourcesCommonPath", "resources/" + Version.RESOURCES_VERSION + "/common/keycloak");
            map.put("variant",EnvironmentReader.getValue("VARIANT", "ezto verify"));
            map.put("mode", session.getContext().getRealm().getMode());
            String websiteContactLink =  EnvironmentReader.getValue(WEBSITE_CONTACT_LINK, EZTO_CONTACT_LINK);
            String supportUrl = session.getContext().getRealm().getAttribute(SUPPORT_URL);
            if (Boolean.parseBoolean(session.getContext().getRealm().getAttribute(SUPPORT_CUSTOM_TOGGLE)) && StringUtil.isNotBlank(supportUrl)) {
                websiteContactLink = supportUrl;
            }
            map.put("websiteContactLink", websiteContactLink);
            map.put("privacyPolicyLink", EnvironmentReader.getValue("PRIVACY_POLICY_LINK", "https://app.termly.io/document/privacy-policy/5c0d5314-9eb1-4498-948a-d4f6751d060c"));
            map.put("termsAndConditionLink", EnvironmentReader.getValue("TERMS_AND_CONDITION_LINK", "https://app.termly.io/document/terms-of-service/0e94947b-0d93-415e-acbb-34a03f2f3283"));
            map.put("docsLink", EnvironmentReader.getValue("DOCS_LINK", ""));
            map.put("websiteLink", EnvironmentReader.getValue("WEBSITE_LINK", "https://ezto.io/"));
            map.put("marketplaceLink", EnvironmentReader.getValue("MARKETPLACE_LINK", "https://eztoauth.com/marketplace"));
            map.put("authenticationLink", EnvironmentReader.getValue("AUTHENTICATION_LINK", "https://eztoauth.com/platform/authentication"));
            map.put("applicationLink", EnvironmentReader.getValue("APPLICATION_LINK", "https://eztoauth.com/platform"));
            map.put("locales", Constants.SUPPORTED_LOCALES);
            RealmModel masterRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
            HashSet<String> availableCategories = new HashSet<>();
            masterRealm.getEztovrfyMarketplaceCategoriesStream().map(EztovrfyMarketplaceCategoryModel::getCategory).forEach(availableCategories::add);
            map.put("availableCategories", availableCategories);

            //setup onBoarding Steps
            OnboardingRepresentation steps = session.onBoarding().welcomeOnboardingSteps(session, null);
            map.put("obSteps", steps);
            map.put("showMultiLanSelect", isOnboardingStepRequired(steps));
            map.put("showPreview", isOnboardingStepRequired(steps));
            map.put("tenantName", session.getContext().getRealm().getDisplayName());
            map.put("onboardingCompleted", session.onBoarding().isOnboardingCompleted());

            /*boolean bootstrap = shouldBootstrap();
            map.put("bootstrap", bootstrap);*/
            boolean isLocal = isLocal();
            map.put("localUser", isLocal);

            if (isLocal) {
                String stateChecker = setCsrfCookie();
                map.put("stateChecker", stateChecker);
            }

            if (successMessage != null) {
                map.put("successMessage", successMessage);
            }
            if (errorMessage != null) {
                map.put("errorMessage", errorMessage);
            }

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);

            ResponseBuilder rb = Response.status(errorMessage == null ? Status.OK : Status.BAD_REQUEST)
                    .entity(result)
                    .cacheControl(CacheControlUtil.noCache());
            return rb.build();
        } catch (Exception e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private String getLocalAdminUrl() {
        boolean isHttpEnabled = Boolean.parseBoolean(Configuration.getConfigValue("kc.http-enabled").getValue());
        String configPath = Configuration.getConfigValue("kc.http-relative-path").getValue();

        if (!configPath.startsWith("/")) {
            configPath = "/" + configPath;
        }

        String configPort = isHttpEnabled ? Configuration.getConfigValue("kc.http-port").getValue() : Configuration.getConfigValue("kc.https-port").getValue() ;

        String scheme = isHttpEnabled ? "http://" : "https://";

        return scheme + "localhost:" + configPort + configPath;
    }

    private Theme getTheme() {
        try {
            return session.theme().getTheme(Theme.Type.WELCOME);
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean shouldBootstrap() {
        if (shouldBootstrap == null) {
            synchronized (this) {
                if (shouldBootstrap == null) {
                    shouldBootstrap = new AtomicBoolean(new ApplianceBootstrap(session).isNoMasterUser());
                }
            }
        }
        return shouldBootstrap.get();
    }

    private boolean isLocal() {
        ClientConnection clientConnection = session.getContext().getConnection();
        String[] allowedIP = EnvironmentReader.getValue("ROOT_ACCESS_IP").split(",");
        return Arrays.stream(allowedIP).anyMatch(x -> x.equals(clientConnection.getRemoteAddr()));
    }

    private String setCsrfCookie() {
        String stateChecker = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
        String cookiePath = session.getContext().getUri().getPath();
        boolean secureOnly = session.getContext().getUri().getRequestUri().getScheme().equalsIgnoreCase("https");
        CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, stateChecker, cookiePath, null, null, 300, secureOnly, true);
        return stateChecker;
    }

    private Response checkOnboardingStepsCompleted() {
        if (session.onBoarding().isOnboardingCompleted()) {
            return Response.status(302).location(session.getContext().getUri(UrlType.ADMIN).getBaseUriBuilder().path("/admin/").path(
                    session.getContext().getRealm().getName()).path("/console/").build()).build();
        }
        return null;
    }

}
