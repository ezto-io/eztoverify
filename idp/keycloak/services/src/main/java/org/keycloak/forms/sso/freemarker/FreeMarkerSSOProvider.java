package org.keycloak.forms.sso.freemarker;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.keycloak.EnvironmentReader;
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64;
import org.keycloak.forms.account.freemarker.model.ReferrerBean;
import org.keycloak.forms.account.freemarker.model.RealmBean;
import org.keycloak.forms.sso.SSOProvider;
import org.keycloak.locale.LocaleType;
import org.keycloak.marketplace.template.MarketplaceSSOTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProvider;
import org.keycloak.marketplace.template.SSOAccessState;
import org.keycloak.marketplace.template.SSOBean;
import org.keycloak.forms.sso.freemarker.models.SSOBeans;
import org.keycloak.forms.sso.freemarker.models.UrlBean;
import org.keycloak.forms.sso.freemarker.models.UserBean;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.sso.SSOResponse;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.*;

import javax.ws.rs.core.Response.Status;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import static org.keycloak.models.Constants.*;

public class FreeMarkerSSOProvider implements SSOProvider {

    private static final Logger logger = Logger.getLogger(FreeMarkerSSOProvider.class);

    protected Auth auth;
    protected AdminPermissionEvaluator  adminPermissionEvaluator;
    protected UriInfo uriInfo;
    protected HttpHeaders httpHeaders;

    protected Map<String, Object> attributes;

    protected RealmModel realm;
    protected UserModel user;
    protected KeycloakSession session;
    protected FreeMarkerUtil freeMarker;
    protected Status status = Status.OK;

    protected String[] referrer;
    protected String stateChecker;

    protected List<FormMessage> messages = null;
    protected MessageType messageType = MessageType.ERROR;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    protected String idToken;

    public FreeMarkerSSOProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    public SSOProvider setAuth(Auth auth) {
        this.auth = auth;
        return this;
    }

    public SSOProvider setAdminPermissionEvaluator(AdminPermissionEvaluator auth) {
        this.adminPermissionEvaluator = auth;
        return this;
    }

    @Override
    public SSOProvider setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public SSOProvider setHttpHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
        return this;
    }

    @Override
    public Response createResponse() {
        if (user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }
        Map<String, Object> attributes = new HashMap<>();
        Theme theme;
        try {
            theme = getTheme();
        } catch (IOException e) {
            logger.error("Failed to create theme", e);
            return Response.serverError().build();
        }
        // TODO: Locale setting
        Locale locale = session.getContext().resolveLocale(user, LocaleType.LOGIN);;
        Properties messagesBundle = handleThemeResources(theme, locale, attributes);
        URI baseUri = uriInfo.getBaseUri();
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
            baseUriBuilder.queryParam(e.getKey(), e.getValue().toArray());
        }
        URI baseQueryUri = baseUriBuilder.build();

        if (stateChecker != null) {
            attributes.put("stateChecker", stateChecker);
        }

        handleMessages(locale, messagesBundle, attributes);

        if (referrer != null) {
            attributes.put("referrer", new ReferrerBean(referrer));
        }

        if (realm != null) {
            attributes.put("realm", new RealmBean(session, realm));
        }

        attributes.put("ssouser", new UserBean(user, null));
        attributes.put("user_preferred_lang",  session.getContext().resolveLocale(user, LocaleType.ADMIN));
        attributes.put("locales", realm.getPreferredLoginLocalesStream().collect(Collectors.toSet()));
        attributes.put("showLanguageSelector", Boolean.toString(realm.getPreferredLoginLocalesStream().count() > 1));
        attributes.put("url", new UrlBean(realm,theme, baseUri, baseQueryUri, uriInfo.getRequestUri(), idToken));
        attributes.put("baseURI", baseUri);

        if (realm != null && realm.isInternationalizationEnabled()) {
            UriBuilder b = UriBuilder.fromUri(baseQueryUri).path(uriInfo.getPath());
            attributes.put("locale", new LocaleBean(realm, locale, b, messagesBundle));
        }
        URI adminBaseUri = session.getContext().getUri(UrlType.ADMIN).getBaseUri();
        attributes.put("resourceCommonUrl", Urls.themeRoot(adminBaseUri).getPath() + "/common/keycloak");
        attributes.put("serverVersion", Version.QV_VERSION);
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        SSOResponse ssoResponse = new SSOResponse(){{
            setSsoApps(getSSO());
            setUser(ModelToRepresentation.toBriefRepresentation(user));
            setRealm(ModelToRepresentation.toBriefRepresentation(realm,false, session));
        }};
        boolean isDesktopAppSupported = session.rootBrand().whitelistedMarketplace(realm) != null && !session.rootBrand().whitelistedMarketplace(realm).contains("204a8ae7-8d55-4188-92eb-705f24a47ff1");
        if(isDesktopAppSupported) {
            ClientModel clientModel = session.getContext().getRealm().getClientByClientId(SSO_DESKTOP_CLIENT_ID);
            if(clientModel != null) {
                String url = ModelToRepresentation.getWorkspaceUrl(session, realm)+"/auth/realms/"+realm.getName()+"/sso/downloads/download/";
                ssoResponse.setDesktopAppInstalled(true);
                ssoResponse.setAppDetails(new HashMap<String,String>(){{
                    put("desktop_app_link_mac",url+"sso-mac");
                    put("desktop_app_link_win",url+"sso-win");
                    put("app_store_link",url+"sso-ios");
                    put("play_store_link",url+"sso-android");
                    put("qr",getQr());
                    try {
                        put("workspace",JsonSerialization.writeValueAsString(getQrData()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }});
            }
        }

        try {
            attributes.put("ssoApps", JsonSerialization.writeValueAsString(ssoResponse.getSsoApps()));
        } catch (IOException e) {
            logger.error("failed to parse object as json");
        }
        // Browser plugin urls
        attributes.put("chromePluginURL", EnvironmentReader.getValue("CHROME_BROWSER_PLUGIN","https://chromewebstore.google.com/detail/ezto-auth-browser-plugin/fhfhpfjmphjihapgjpobghchlapajhpa"));
        attributes.put("firefoxPluginURL", EnvironmentReader.getValue("FIREFOX_BROWSER_PLUGIN", "https://addons.mozilla.org/en-GB/firefox/addon/ezto-auth-browser-plugin/"));
        attributes.put("applePluginURL", EnvironmentReader.getValue("APPLE_BROWSER_PLUGIN", "https://apps.apple.com/in/app/ezto-auth-browser-plugin/id6475013798?mt=12"));


        return processTemplate(theme, attributes, locale);
    }

    private HashMap<String,String> getQrData(){
        HashMap<String,String> qrData = new HashMap<>();
        qrData.put("type","sso-app");
        qrData.put("workspaceUrl", ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm()));
        qrData.put("workspaceName",session.getContext().getRealm().getName());
        return qrData;
    }

    private String getQr(){

        try {
            return qrCode(JsonSerialization.writeValueAsString(getQrData()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String qrCode(String text) {
        try {
            int width = 512;
            int height = 512;

            QRCodeWriter writer = new QRCodeWriter();
            final BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
            bos.close();

            return Base64.encodeBytes(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<SSOBean> getSSO() {

        List<SSOBean> ssoBeans = new ArrayList<>();

        List<ClientModel> clientModels = realm.getClientsStream().collect(Collectors.toList());

        Optional<ClientModel> dashboardClient = clientModels.stream().filter(x -> x.getClientId().equalsIgnoreCase(ADMIN_CONSOLE_CLIENT_ID)).findFirst();
        if (dashboardClient.isPresent()) {
            Optional<ClientModel> realmManagementClient = clientModels.stream().filter(x -> x.getClientId().equalsIgnoreCase(REALM_MANAGEMENT_CLIENT_ID)).findFirst();
            if ((auth != null && auth.hasRealmRole(AdminRoles.ADMIN)) || (adminPermissionEvaluator != null && adminPermissionEvaluator.adminAuth().hasRealmRole(AdminRoles.ADMIN))) {
                ClientModel clientModel = dashboardClient.get();
                addToSSO(ssoBeans, clientModel, false,true);
            } else if (realmManagementClient.isPresent()) {
                if ((auth != null && auth.hasClientRole(realmManagementClient.get(), AdminRoles.REALM_ADMIN)) || (adminPermissionEvaluator != null && adminPermissionEvaluator.adminAuth().hasAppRole(realmManagementClient.get(), AdminRoles.REALM_ADMIN))) {
                    ClientModel clientModel = dashboardClient.get();
                    addToSSO(ssoBeans, clientModel, false,true);
                }
            }

        }

        Optional<ClientModel> profileClient = clientModels.stream().filter(x -> x.getClientId().equalsIgnoreCase(ACCOUNT_MANAGEMENT_CLIENT_ID)).findFirst();
        if (profileClient.isPresent()) {
            ClientModel clientModel = profileClient.get();
            addToSSO(ssoBeans, clientModel, false,true);
        }

        clientModels.stream().filter(x -> x.getAttribute("isSSO") != null && x.getAttribute("isSSO").equalsIgnoreCase("true") && x.isEnabled()).forEach(x -> addToSSO(ssoBeans, x, false,false));
        clientModels.stream().filter(x -> x.getAttribute("sso.show") != null && x.getAttribute("sso.show").equalsIgnoreCase("true")  && x.isEnabled()).forEach(x -> addToSSO(ssoBeans, x, true,false));
        return ssoBeans;
    }

    private void addToSSO(List<SSOBean> ssoBeans, ClientModel clientModel, boolean isNormalApp, boolean isInBuildApp) {
        String hasSSOApps = clientModel.getAttribute("hasSSOSubApps");
        ClientRepresentation representation = ModelToRepresentation.toRepresentation(clientModel,session);
        boolean isIWSApp = clientModel.getAttribute("iws.enable") != null && clientModel.getAttribute("iws.enable").equalsIgnoreCase("true");
        if (isNormalApp && clientModel.getProtocol().equalsIgnoreCase("openid-connect")) {
            if(clientModel.getRedirectUris().size() > 0) {
                String redirectUrl = clientModel.getRedirectUris().stream().findFirst().get();
                if(!redirectUrl.startsWith("http") && !redirectUrl.startsWith("https"))
                {
                    logger.info("Trying to take "+ redirectUrl);
                    logger.info("Redirect URL for SSO is not valid");
                    return;
                }
                boolean hasAccess;
                if(isIWSApp){
                        hasAccess = hasAccess("iws-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-role");
                }
                else{
                    hasAccess = hasAccess("sso-" +clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-role");
                }

                if (hasAccess) {
                    ssoBeans.add(new SSOBean() {{
                        setDisplayName(clientModel.getName());
                        setAlias(clientModel.getClientId());
                        setAppType(representation.getAppType());
                        setAppLoginUrl(isIWSApp? redirectUrl : null);
                        if(clientModel.getAttribute("spurl") != null){
                            setRedirectUrl(clientModel.getAttribute("spurl"));
                        }
                        else{
                            setRedirectUrl(ClientResource.getOIDCUrl(clientModel, session, realm, redirectUrl));
                        }
                        setLogoUrl(clientModel.getAttribute("sso.logourl") != null ? clientModel.getAttribute("sso.logourl") : null);
                    }});
                }
            }
        }
        else if(isInBuildApp)
        {
            String oidcUrl =  ClientResource.getOIDCUrl(clientModel,session,realm,null);
            if(clientModel.getClientId().equalsIgnoreCase(ADMIN_CONSOLE_CLIENT_ID)) {
                String redirectUrl = clientModel.getBaseUrl();
                if (!redirectUrl.startsWith("http")) {
                    URI adminBaseUri = session.getContext().getUri(UrlType.ADMIN).getBaseUri();
                    String adminBaseUrl = adminBaseUri.toString();
                    if (adminBaseUrl.endsWith("/")) {
                        adminBaseUrl = adminBaseUrl.substring(0, adminBaseUrl.length() - 1);
                    }
                    redirectUrl = adminBaseUrl + redirectUrl;
                }
                oidcUrl = redirectUrl;
            }
            String finalOidcUrl = oidcUrl;
            ssoBeans.add(new SSOBean() {{
                setDisplayName(clientModel.getName());
                setAlias(clientModel.getClientId());
                setRedirectUrl(finalOidcUrl);
                setLogoUrl(clientModel.getAttribute("logoUrl") != null ? clientModel.getAttribute("logoUrl") : null);
            }});
        }
        else if (hasSSOApps == null) {
            String adminBaseUrl = getAdminUrl();
            String finalRedirectUrl = adminBaseUrl+"/realms/"+realm.getName()+"/protocol/saml/clients/"+clientModel.getClientId();
            String roleName = "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-" + clientModel.getAttribute("sso_type") + "-role";
            boolean hasAccess = hasAccess(roleName);
            if(hasAccess) {
                ssoBeans.add(new SSOBean() {{
                    setDisplayName(clientModel.getName());
                    setAlias(clientModel.getClientId());
                    setRedirectUrl(finalRedirectUrl);
                    if(clientModel.getAttribute("sso.logourl") != null){
                        setLogoUrl(clientModel.getAttribute("sso.logourl"));
                    }
                    else if(clientModel.getAttribute("logoUrl") != null){
                        setLogoUrl(clientModel.getAttribute("logoUrl"));
                    }
                    else{
                        setLogoUrl(null);
                    }

                }});
            }
        } else {
            String adminBaseUrl = getAdminUrl();
            String finalRedirectUrl = adminBaseUrl+"/realms/"+realm.getName()+"/protocol/saml/clients/"+clientModel.getClientId();
            String templateId = clientModel.getAttribute("templateId");
            MarketplaceSSOTemplateProvider marketplaceProvider = (MarketplaceSSOTemplateProvider) session.getProvider(MarketplaceTemplateProvider.class, templateId);
            if (marketplaceProvider != null && clientModel.getAttribute("edit_supported_apps") == null) {
                String roleName = "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-" + clientModel.getAttribute("sso_type") + "-role";
                boolean hasAccess = hasAccess(roleName);
                if(hasAccess) {
                    List<SSOBean> ssoBeanList = marketplaceProvider.getSSOSubApps(clientModel, finalRedirectUrl);
                    ssoBeans.addAll(ssoBeanList);
                }
            } else {
                // Client with subapps
                List<SSOBean> ssoBeanList = marketplaceProvider.getSSOSubApps(clientModel, finalRedirectUrl);
                ssoBeans.addAll(ssoBeanList);
            }
        }
    }

    protected  boolean hasAccess(String role)
    {
        if ((auth != null && auth.hasRealmRole(role)) || (adminPermissionEvaluator != null && adminPermissionEvaluator.adminAuth().hasRealmRole(role))) {
            return true;
        }
        return false;
    }

    private String getAdminUrl() {
        URI adminBaseUri = session.getContext().getUri(UrlType.ADMIN).getBaseUri();
        String adminBaseUrl = adminBaseUri.toString();
        if (adminBaseUrl.endsWith("/")) {
            adminBaseUrl = adminBaseUrl.substring(0, adminBaseUrl.length() - 1);
        }
        return adminBaseUrl;
    }

    @Override
    public SSOProvider setError(Response.Status status, String message, Object... parameters) {
        this.status = status;
        setMessage(MessageType.ERROR, message, parameters);
        return this;
    }

    @Override
    public SSOProvider setErrors(Response.Status status, List<FormMessage> messages) {
        this.status = status;
        this.messageType = MessageType.ERROR;
        this.messages = new ArrayList<>(messages);
        return this;
    }

    @Override
    public SSOProvider setSuccess(String message, Object... parameters) {
        setMessage(MessageType.SUCCESS, message, parameters);
        return this;
    }

    @Override
    public SSOProvider setWarning(String message, Object... parameters) {
        setMessage(MessageType.WARNING, message, parameters);
        return this;
    }

    @Override
    public SSOProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public SSOProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public SSOProvider setReferrer(String[] referrer) {
        this.referrer = referrer;
        return this;
    }

    @Override
    public SSOProvider setStateChecker(String stateChecker) {
        this.stateChecker = stateChecker;
        return this;
    }


    @Override
    public SSOProvider setAttribute(String key, String value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
        return this;
    }


    @Override
    public void close() {

    }

    protected Theme getTheme() throws IOException {
        return session.theme().getTheme(Theme.Type.SSO);
    }

    protected void setMessage(MessageType type, String message, Object... parameters) {
        messageType = type;
        messages = new ArrayList<>();
        messages.add(new FormMessage(null, message, parameters));
    }

    protected Properties handleThemeResources(Theme theme, Locale locale, Map<String, Object> attributes) {
        Properties messagesBundle;
        try {
            messagesBundle = theme.getMessages(locale);
            attributes.put("msg", new MessageFormatterMethod(locale, messagesBundle));
        } catch (IOException e) {
            logger.warn("Failed to load messages", e);
            messagesBundle = new Properties();
        }
        try {
            attributes.put("properties", theme.getProperties());
        } catch (IOException e) {
            logger.warn("Failed to load properties", e);
        }
        return messagesBundle;
    }

    protected Response processTemplate(Theme theme, Map<String, Object> attributes, Locale locale) {
        try {
            String result = freeMarker.processTemplate(attributes, "index.ftl", theme);
            Response.ResponseBuilder builder = Response.status(status).type(MediaType.TEXT_HTML_UTF_8_TYPE).language(locale).entity(result);
            builder.cacheControl(CacheControlUtil.noCache());
            return builder.build();
        } catch (FreeMarkerException e) {
            logger.error("Failed to process template", e);
            return Response.serverError().build();
        }
    }

    protected void handleMessages(Locale locale, Properties messagesBundle, Map<String, Object> attributes) {
        MessagesPerFieldBean messagesPerField = new MessagesPerFieldBean();
        if (messages != null) {
            MessageBean wholeMessage = new MessageBean(null, messageType);
            for (FormMessage message : this.messages) {
                String formattedMessageText = formatMessage(message, messagesBundle, locale);
                if (formattedMessageText != null) {
                    wholeMessage.appendSummaryLine(formattedMessageText);
                    messagesPerField.addMessage(message.getField(), formattedMessageText, messageType);
                }
            }
            attributes.put("message", wholeMessage);
        }
        attributes.put("messagesPerField", messagesPerField);
    }

    protected String formatMessage(FormMessage message, Properties messagesBundle, Locale locale) {
        if (message == null)
            return null;
        if (messagesBundle.containsKey(message.getMessage())) {
            return new MessageFormat(messagesBundle.getProperty(message.getMessage()), locale).format(message.getParameters());
        } else {
            return message.getMessage();
        }
    }
}
