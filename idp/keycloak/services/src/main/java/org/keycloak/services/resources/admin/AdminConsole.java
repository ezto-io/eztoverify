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
package org.keycloak.services.resources.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import javax.ws.rs.NotFoundException;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Version;
import org.keycloak.common.util.UriUtils;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Providers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.services.messages.Messages.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdminConsole {
    protected static final Logger logger = Logger.getLogger(AdminConsole.class);

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    @Context
    protected KeycloakSession session;

    @Context
    protected Providers providers;

    protected RealmModel realm;

    public AdminConsole(RealmModel realm) {
        this.realm = realm;
    }

    public static class WhoAmI {
        protected String userId;
        protected String realm;
        protected String displayName;
        protected String profPic;
        protected Locale locale;
        protected Boolean isAccountCreator;

        @JsonProperty("createRealm")
        protected boolean createRealm;
        @JsonProperty("realm_access")
        protected Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();

        public WhoAmI() {
        }

        public WhoAmI(String userId, String realm, String displayName,String profPic, boolean createRealm, Map<String, Set<String>> realmAccess, Locale locale,Boolean isAccountCreator) {
            this.userId = userId;
            this.realm = realm;
            this.displayName = displayName;
            this.profPic = profPic;
            this.createRealm = createRealm;
            this.realmAccess = realmAccess;
            this.locale = locale;
            this.isAccountCreator = isAccountCreator;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
        public Boolean getIsAccountCreator() {
            return isAccountCreator;
        }

        public void setIsAccountCreator(Boolean isAccountCreator) {
            this.isAccountCreator = isAccountCreator;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getProfPic() {
            return profPic;
        }

        public void setProfPic(String profPic) {
            this.profPic = profPic;
        }

        public boolean isCreateRealm() {
            return createRealm;
        }

        public void setCreateRealm(boolean createRealm) {
            this.createRealm = createRealm;
        }

        public Map<String, Set<String>> getRealmAccess() {
            return realmAccess;
        }

        public void setRealmAccess(Map<String, Set<String>> realmAccess) {
            this.realmAccess = realmAccess;
        }

        public Locale getLocale() {
            return locale;
        }

        public void setLocale(Locale locale) {
            this.locale = locale;
        }

        //  TODO: Locale setting
        @JsonProperty(value = "locale")
        public String getLocaleLanguageTag() {
            // locale = Locale.SIMPLIFIED_CHINESE;
            // return "zh-CN";
           return locale != null ? locale.toLanguageTag() : null;
        }
    }

    public static  class WorkspaceSetupStatus{
        protected boolean isEnabledOwnEmailProvider;
        protected boolean isEnabledOwnSmsProvider;
        protected boolean isEnabledOwnWhatsappProvider;
        protected boolean isOwnEmailProviderConfigured;
        protected boolean isOwnSmsProviderConfigured;
        protected boolean isOwnWhatsappProviderConfigured;

        protected int adminUserCount;

        public boolean isEnabledOwnEmailProvider() {
            return isEnabledOwnEmailProvider;
        }

        public void setEnabledOwnEmailProvider(boolean enabledOwnEmailProvider) {
            isEnabledOwnEmailProvider = enabledOwnEmailProvider;
        }

        public boolean isEnabledOwnSmsProvider() {
            return isEnabledOwnSmsProvider;
        }

        public void setEnabledOwnSmsProvider(boolean enabledOwnSmsProvider) {
            isEnabledOwnSmsProvider = enabledOwnSmsProvider;
        }

        public boolean isEnabledOwnWhatsappProvider() {
            return isEnabledOwnWhatsappProvider;
        }

        public void setEnabledOwnWhatsappProvider(boolean enabledOwnWhatsappProvider) {
            isEnabledOwnWhatsappProvider = enabledOwnWhatsappProvider;
        }

        public boolean isOwnEmailProviderConfigured() {
            return isOwnEmailProviderConfigured;
        }

        public void setOwnEmailProviderConfigured(boolean ownEmailProviderConfigured) {
            isOwnEmailProviderConfigured = ownEmailProviderConfigured;
        }

        public boolean isOwnSmsProviderConfigured() {
            return isOwnSmsProviderConfigured;
        }

        public void setOwnSmsProviderConfigured(boolean ownSmsProviderConfigured) {
            isOwnSmsProviderConfigured = ownSmsProviderConfigured;
        }

        public boolean isOwnWhatsappProviderConfigured() {
            return isOwnWhatsappProviderConfigured;
        }

        public void setOwnWhatsappProviderConfigured(boolean ownWhatsappProviderConfigured) {
            isOwnWhatsappProviderConfigured = ownWhatsappProviderConfigured;
        }

        public int getAdminUserCount() {
            return adminUserCount;
        }

        public void setAdminUserCount(int adminUserCount) {
            this.adminUserCount = adminUserCount;
        }
    }
    /**
     * Adapter configuration for the admin console for this realm
     *
     * @return
     */
    @Path("config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ClientManager.InstallationAdapterConfig config() {
        ClientModel consoleApp = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (consoleApp == null) {
            throw new NotFoundException("Could not find admin console client");
        }
        return new ClientManager(new RealmManager(session)).toInstallationRepresentation(realm, consoleApp, session.getContext().getUri().getBaseUri());
    }

    @Path("whoami")
    @OPTIONS
    public Response whoAmIPreFlight() {
        return new AdminCorsPreflightService(request).preflight();
    }

    /**
     * Permission information
     *
     * @param headers
     * @return
     */
    @Path("whoami")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response whoAmI(final @Context HttpHeaders headers, @QueryParam("realm") String realmName) {
        RealmManager realmManager = new RealmManager(session);
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            return Response.status(401).build();
        }
        UserModel user= authResult.getUser();
        String displayName;
        if ((user.getFirstName() != null && !user.getFirstName().trim().equals("")) || (user.getLastName() != null && !user.getLastName().trim().equals(""))) {
            displayName = user.getFirstName();
            if (user.getLastName() != null) {
                displayName = displayName != null ? displayName + " " + user.getLastName() : user.getLastName();
            }
        } else {
            displayName = user.getUsername();
        }
        String profilePic = user.getFirstAttribute(UserModel.USER_PICTURE);
        if(profilePic == null){
            profilePic = "";
        } else {
            profilePic = ModelToRepresentation.getLogoAbsURL(profilePic);
        }

        RealmModel queryRealm = null;
        if(realmName != null && realmName.length() > 0)
        {
            queryRealm = session.realms().getRealmByName(realmName);
        }


        Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();
        boolean createRealm = false;
        RealmModel masterRealm = getAdminstrationRealm(realmManager);
        if(queryRealm != null)
        {
            logger.debug("setting up realm access for a realm user");
            if (realm.equals(masterRealm)) {
                ClientModel realmAdminApp = queryRealm.getMasterAdminClient();
                createRealm = user.hasRole(masterRealm.getRole(AdminRoles.CREATE_REALM));
                getRealmAdminAccess(queryRealm, realmAdminApp, user, realmAccess);
            }
            else
            {
                addRealmAccess(queryRealm, user, realmAccess);
            }
        }
        else {
            if (masterRealm == null)
                throw new NotFoundException("No realm found");
            if (realm.equals(masterRealm)) {
                logger.debug("setting up realm access for a master realm user");
                createRealm = user.hasRole(masterRealm.getRole(AdminRoles.CREATE_REALM));
                addMasterRealmAccess(user, realmAccess);
            } else {
                logger.debug("setting up realm access for a realm user");
                addRealmAccess(realm, user, realmAccess);
            }
        }

        Locale locale = session.getContext().resolveLocale(user, LocaleType.ADMIN);

        Cors.add(request).allowedOrigins(authResult.getToken()).allowedMethods("GET").auth()
                .build(response);

        Boolean isAccountCreator = user.getFirstAttribute("qv_is_account_creator") != null &&  Boolean.parseBoolean(user.getFirstAttribute("qv_is_account_creator"));

        return Response.ok(new WhoAmI(user.getId(), realm.getName(), displayName,profilePic, createRealm, realmAccess, locale,isAccountCreator)).header("Set-Cookie", "KEYCLOAK_LOCALE=" +locale.getLanguage() + "; path=/").build();
    }

    @Path("workspace-setup-status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getCompletionStatus(final @Context HttpHeaders headers, @QueryParam("realm") String realmName){
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            return Response.status(401).build();
        }

        WorkspaceSetupStatus status = new WorkspaceSetupStatus();
        status.setEnabledOwnEmailProvider(realm.isEnabledOwnEmailProvider());
        status.setEnabledOwnSmsProvider(realm.isEnabledOwnSmsProvider());
        status.setEnabledOwnWhatsappProvider(realm.isEnabledOwnWhatsappProvider());
        status.setOwnEmailProviderConfigured(realm.getEmailConfigsStream().findAny().isPresent());
        status.setOwnSmsProviderConfigured(realm.getSmsConfigsStream().anyMatch(x -> !x.getSmsProviderId().equals("whatsapp")));
        status.setOwnWhatsappProviderConfigured(realm.getSmsConfigsStream().anyMatch(x -> x.getSmsProviderId().equals("whatsapp")));
        status.setAdminUserCount(session.users().getUsersCount(realm, false,true));
        return Response.ok(status).build();
    }

    private void addRealmAccess(RealmModel realm, UserModel user, Map<String, Set<String>> realmAdminAccess) {
        RealmManager realmManager = new RealmManager(session);
        ClientModel realmAdminApp = realm.getClientByClientId(realmManager.getRealmAdminClientId(realm));
        getRealmAdminAccess(realm, realmAdminApp, user, realmAdminAccess);
    }

    private void addMasterRealmAccess(UserModel user, Map<String, Set<String>> realmAdminAccess) {
        ClientModel realmAdminApp = realm.getMasterAdminClient();
        getRealmAdminAccess(realm, realmAdminApp, user, realmAdminAccess);
    }

    private static <T> HashSet<T> union(Set<T> set1, Set<T> set2) {
        if (set1 == null && set2 == null) {
            return null;
        }
        HashSet<T> res;
        if (set1 instanceof HashSet) {
            res = (HashSet <T>) set1;
        } else {
            res = set1 == null ? new HashSet<>() : new HashSet<>(set1);
        }
        if (set2 != null) {
            res.addAll(set2);
        }
        return res;
    }

    private void getRealmAdminAccess(RealmModel realm, ClientModel client, UserModel user, Map<String, Set<String>> realmAdminAccess) {
        Set<String> realmRoles = client.getRolesStream()
                .filter(user::hasRole)
                .map(RoleModel::getName)
                .collect(Collectors.toSet());

        realmAdminAccess.merge(realm.getName(), realmRoles, AdminConsole::union);
    }

    private static String getProfPicture(KeycloakSession session,UserModel user){
        AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class,"avatar-storage-minio");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {

            InputStream res = avatarProvider.loadAvatarImage(session.getContext().getRealm().getName(), user.getId());
            if(res !=null) {

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = res.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    /**
     * Logout from the admin console
     *
     * @return
     */
    @Path("logout")
    @GET
    @NoCache
    public Response logout() {
        URI redirect = AdminRoot.adminConsoleUrl(session.getContext().getUri(UrlType.ADMIN)).build(realm.getName());

        return Response.status(302).location(
                OIDCLoginProtocolService.logoutUrl(session.getContext().getUri(UrlType.ADMIN)).queryParam("post_logout_redirect_uri", redirect.toString()).build(realm.getName())
        ).build();
    }

    protected RealmModel getAdminstrationRealm(RealmManager realmManager) {
        return realmManager.getKeycloakAdminstrationRealm();
    }

    /**
     * Main page of this realm's admin console
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @NoCache
    public Response getMainPage() throws IOException, FreeMarkerException {
        if (!session.getContext().getUri(UrlType.ADMIN).getRequestUri().getPath().endsWith("/")) {
            UriBuilder uriBuilder = session.getContext().getUri(UrlType.ADMIN).getRequestUriBuilder().path("/");
            MultivaluedMap<String, String> params = session.getContext().getUri().getQueryParameters();
            if(params.containsKey("sso_off_token"))
            {
                uriBuilder.queryParam("sso_off_token",params.get("sso_off_token"));
            }
            return Response.status(302).location(uriBuilder.build()).build();
        } else {
            Theme theme = AdminRoot.getTheme(session, realm);

            Map<String, Object> map = new HashMap<>();

            URI adminBaseUri = session.getContext().getUri(UrlType.ADMIN).getBaseUri();
            String adminBaseUrl = adminBaseUri.toString();
            if (adminBaseUrl.endsWith("/")) {
                adminBaseUrl = adminBaseUrl.substring(0, adminBaseUrl.length() - 1);
            }

            URI authServerBaseUri = session.getContext().getUri(UrlType.FRONTEND).getBaseUri();
            String authServerBaseUrl = authServerBaseUri.toString();
            if (authServerBaseUrl.endsWith("/")) {
                authServerBaseUrl = authServerBaseUrl.substring(0, authServerBaseUrl.length() - 1);
            }

            map.put("authServerUrl", authServerBaseUrl);
            map.put("authUrl", adminBaseUrl);
            map.put("consoleBaseUrl", Urls.adminConsoleRoot(adminBaseUri, realm.getName()).getPath());
            map.put("resourceUrl", Urls.themeRoot(adminBaseUri).getPath() + "/admin/" + theme.getName());
            map.put("resourceCommonUrl", Urls.themeRoot(adminBaseUri).getPath() + "/common/keycloak");
            map.put("keycloakJsUrl", adminBaseUri.getPath() + "js/keycloak.js?version=" + Version.RESOURCES_VERSION);
            map.put("masterRealm", Config.getAdminRealm());
            map.put("resourceVersion", Version.RESOURCES_VERSION);
            map.put("properties", theme.getProperties());
            map.put("serverVersion", Version.QV_VERSION);
            map.put("licenseUrl", EnvironmentReader.getValue(Constants.LICENSE_SERVER_URL));
            map.put("docsUrl", EnvironmentReader.getValue("DOCS_URL",Constants.DEFAULT_DOCS_URL));
            map.put("dashboardUrl", ModelToRepresentation.getDashboardUrl());
            map.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
            map.put("mode", session.getContext().getRealm().getMode());
            String websiteContactLink =  EnvironmentReader.getValue(WEBSITE_CONTACT_LINK, EZTO_CONTACT_LINK);
            String supportUrl = realm.getAttribute(SUPPORT_URL);
            if (Boolean.parseBoolean(realm.getAttribute(SUPPORT_CUSTOM_TOGGLE)) && StringUtil.isNotBlank(supportUrl)) {
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

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);
            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML_UTF_8).language(Locale.ENGLISH).entity(result);

            // Replace CSP if admin is hosted on different URL
            if (!adminBaseUri.equals(authServerBaseUri)) {
                session.getProvider(SecurityHeadersProvider.class).options().allowFrameSrc(UriUtils.getOrigin(authServerBaseUri));
            }
            return builder.build();
        }
    }

    @GET
    @Path("{indexhtml: index.html}") // this expression is a hack to get around jaxdoclet generation bug.  Doesn't like index.html
    public Response getIndexHtmlRedirect() {
        return Response.status(302).location(session.getContext().getUri(UrlType.ADMIN).getRequestUriBuilder().path("../").build()).build();
    }

    @GET
    @Path("messages.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Properties getMessages(@QueryParam("lang") String lang) {
        if(lang == null || lang.isEmpty()){
            lang = realm.getAttributes().get("dashboardLocale") != null ? realm.getAttributes().get("dashboardLocale") : "en";
        }
        return AdminRoot.getMessages(session, realm, lang, "admin-messages");
    }

}
