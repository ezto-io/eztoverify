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
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MimeTypeUtil;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.Constants;
import org.keycloak.models.EztovrfyMarketplaceCategoryModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.onboarding.OnboardingRepresentation;
import org.keycloak.onboarding.OnboardingStep;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.services.messages.Messages.*;
import static org.keycloak.utils.GeneralHelper.isOnboardingStepRequired;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/")
public class WelcomeResource {

    protected static final Logger logger = Logger.getLogger(WelcomeResource.class);

    private static final String KEYCLOAK_STATE_CHECKER = "WELCOME_STATE_CHECKER";
    private static final String OB_ADMIN_EMAIL_SETUP = "ob-admin-email-setup";
    private static final String OB_ADMIN_SMS_SETUP = "ob-admin-sms-setup";

    @Context
    protected HttpHeaders headers;

    @Context
    private KeycloakSession session;

    public WelcomeResource() {
    }

    public WelcomeResource(KeycloakSession session){
        this.session = session;
    }

    /**
     * Welcome page of Keycloak
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getWelcomePage() throws URISyntaxException {
        checkBootstrap();

        String requestUri = session.getContext().getUri().getRequestUri().toString();
        if (!requestUri.endsWith("/")) {
            return Response.seeOther(new URI(requestUri + "/")).build();
        } else {
            //if onBoarding steps are completed,then redirect to console login
            Response response = checkOnboardingStepsCompleted();
            if (response != null) {
                return response;
            }

            //else show welcomeOnboardingSteps
            return createWelcomePage(null, null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response onWelcomePostOnboarding(final MultivaluedMap<String, String> formData) {
        //checkBootstrap();

        Response response = checkOnboardingStepsCompleted();
        if (response != null) {
            return response;
        }

        if (!isLocal()) {
            ServicesLogger.LOGGER.rejectedNonLocalAttemptToCreateInitialUser(session.getContext().getConnection().getRemoteAddr());
            throw new ErrorPageException(session, Status.UNAUTHORIZED, "initial-setup-access-denied");
        }

        String providerId = formData.getFirst("stepProviderId");
        if(providerId == null || providerId.isEmpty())
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        RealmModel realm = session.realms().getRealmByName(Config.getAdminRealm());
        return session.onBoarding().onWelcomePostOnboarding(realm,formData,headers,providerId);
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
    /**
     * API used for Liveness Check
     * @return
     */
    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getPingPong(){
        return Response.ok().type(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).build();
    }

    public Response createWelcomePage(String successMessage, String errorMessage) {
        try {
            Theme theme = getTheme();

            Map<String, Object> map = new HashMap<>();

            ClientSessionCode accessCode = new ClientSessionCode(session, session.getContext().getRealm(),
                    session.getContext().getAuthenticationSession());
            session.getContext().getAuthenticationSession().getParentSession().setTimestamp(Time.currentTime());

            URI actionUrl = LoginActionsService.requiredActionProcessor(session.getContext().getUri())
                    .queryParam(LoginActionsService.SESSION_CODE, accessCode.getOrGenerateCode())
                    .queryParam(Constants.EXECUTION, RequiredActionEnum.TENANT_ONBOARDING.name())
                    .queryParam(Constants.CLIENT_ID, session.getContext().getClient().getClientId())
                    .queryParam(Constants.TAB_ID, session.getContext().getAuthenticationSession().getTabId())
                    .build(session.getContext().getRealm().getName());

            map.put("productName", Version.NAME);
            map.put("productNameFull", Version.NAME_FULL);
            map.put("resourceUrl", new UrlBean(session.getContext().getRealm(), theme,
                    session.getContext().getUri().getBaseUri(), actionUrl).getLoginAction());
            map.put("baseUri", session.getContext().getUri().getBaseUri());
            Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
            map.put("locale", locale.toLanguageTag());
            map.put("locales", Constants.SUPPORTED_LOCALES);
            RealmModel masterRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
            HashSet<String> availableCategories = new HashSet<>();
            masterRealm.getEztovrfyMarketplaceCategoriesStream().map(EztovrfyMarketplaceCategoryModel::getCategory).forEach(availableCategories::add);
            map.put("availableCategories", availableCategories);
            Properties messages = theme.getMessages(locale);
            map.put("msg", new MessageFormatterMethod(locale, messages));
            map.put("properties", theme.getProperties());
            map.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
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
            map.put("adminUrl", session.getContext().getUri(UrlType.ADMIN).getBaseUriBuilder().path("/admin/").path(
                    session.getContext().getRealm().getName()).path("/console/").build());
            map.put("resourcesPath",
                   session.getContext().getUri(UrlType.FRONTEND).getBaseUriBuilder().build()+ "resources/" + Version.RESOURCES_VERSION +
                           "/" + theme.getType().toString().toLowerCase() +
                           "/" + theme.getName());
            map.put("resourcesCommonPath", "resources/" + Version.RESOURCES_VERSION + "/common/keycloak");

            //setup onBoarding Steps
            OnboardingRepresentation steps = session.onBoarding().welcomeOnboardingSteps(session, null);
            map.put("obSteps", steps);
            map.put("tenantName", session.getContext().getRealm().getDisplayName());
            map.put("onboardingCompleted",session.onBoarding().isOnboardingCompleted());
            map.put("showMultiLanSelect", isOnboardingStepRequired(steps));
            map.put("showPreview", isOnboardingStepRequired(steps));

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
                map.put("errorString", errorMessage);
            }

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);

            ResponseBuilder rb = Response.status(errorMessage == null ? Status.OK : Status.BAD_REQUEST)
                    .entity(result)
                    .cacheControl(CacheControlUtil.noCache());
            return rb.build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private OnboardingRepresentation getStepsBasedOnSubsription(OnboardingRepresentation steps) {
        RealmModel realm = session.getContext().getRealm();
        List<OnboardingStep> onboardingSteps = steps.getSteps();
        if(!realm.isEnabledOwnEmailProvider()){
            onboardingSteps =
                    onboardingSteps.stream().filter(x -> x.stepProviderId != OB_ADMIN_EMAIL_SETUP).collect(Collectors.toList());
        }

        if (!realm.isEnabledOwnSmsProvider() && !realm.isEnabledOwnWhatsappProvider()){
            onboardingSteps =
                    onboardingSteps.stream().filter(x -> x.stepProviderId != OB_ADMIN_SMS_SETUP).collect(Collectors.toList());
        }

        steps.setSteps(onboardingSteps);
        return steps;
    }

    private Theme getTheme() {
        try {
            return session.theme().getTheme(Theme.Type.WELCOME);
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    //Function to check welcomePage is accessible for only set of allowed IPs
    private void checkBootstrap() {
        if (shouldBootstrap())
            KeycloakApplication.BOOTSTRAP_ADMIN_USER.compareAndSet(true, new ApplianceBootstrap(session).isNoMasterUser());
    }

    private boolean shouldBootstrap() {
        return KeycloakApplication.BOOTSTRAP_ADMIN_USER.get();
    }

    private void setBootstrap(boolean value) {
        KeycloakApplication.BOOTSTRAP_ADMIN_USER.set(value);
    }


    private boolean isLocal() {
        if(!Objects.equals(session.getContext().getRealm().getName(), Config.getAdminRealm())) {
            return true;
        }
        ClientConnection clientConnection = session.getContext().getConnection();
        String[] allowedIP = EnvironmentReader.getValue("ROOT_ACCESS_IP").split(",");
        return Arrays.stream(allowedIP).anyMatch(x-> x.equals(clientConnection.getRemoteAddr()));
    }

    private boolean isLocalAddress(InetAddress inetAddress) {
        return inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress();
    }

    private String setCsrfCookie() {
        String stateChecker = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
        String cookiePath = session.getContext().getUri().getPath();
        boolean secureOnly = session.getContext().getUri().getRequestUri().getScheme().equalsIgnoreCase("https");
        CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, stateChecker, cookiePath, null, null, 300, secureOnly, true);
        return stateChecker;
    }

    private Response checkOnboardingStepsCompleted(){
        if (session.onBoarding().isOnboardingCompleted()) {
            return Response.status(302).location(session.getContext().getUri(UrlType.ADMIN).getBaseUriBuilder().path("/admin/").path(
                    session.getContext().getRealm().getName()).path("/console/").build()).build();
        }
        return null;
    }

    private void expireCsrfCookie() {
        String cookiePath = session.getContext().getUri().getPath();
        boolean secureOnly = session.getContext().getUri().getRequestUri().getScheme().equalsIgnoreCase("https");
        CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, "", cookiePath, null, null, 0, secureOnly, true);
    }

    private void csrfCheck(final MultivaluedMap<String, String> formData) {
        String formStateChecker = formData.getFirst("stateChecker");
        Cookie cookie = headers.getCookies().get(KEYCLOAK_STATE_CHECKER);
        if (cookie == null) {
            throw new ForbiddenException();
        }

        String cookieStateChecker = cookie.getValue();

        if (cookieStateChecker == null || !cookieStateChecker.equals(formStateChecker)) {
            throw new ForbiddenException();
        }
    }

}
