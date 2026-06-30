package com.qlikverify.marketplace.templates.sso;

import com.qlikverify.marketplace.*;
import com.qlikverify.marketplace.templates.sso.base.SSOMarketplaceTemplate;
import org.apache.commons.lang.StringUtils;
import org.keycloak.Config;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.locale.LocaleType;
import org.keycloak.marketplace.InstallResponse;
import org.keycloak.marketplace.MarketplaceException;
import org.keycloak.marketplace.ThirdPartyProvider;
import org.keycloak.marketplace.template.MarketplaceSSOTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProviderFactory;
import org.keycloak.marketplace.template.SSOBean;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.mappers.SAMLAudienceProtocolMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.marketplace.TemplateConstants.*;
import static org.keycloak.models.Constants.SAML;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE;

public class GoogleWorkspaceMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {
    public static final String GOOGLE_AUDIENCE = "Google Audience";
    public static final String AUD_GOOGLE_COM = "google.com";
    public static final String GOOGLE = "Google";
    public static final String GOOGLE_A_ENDPOINT = "https://www.google.com/a/";
    public static final String MAIL_A_ENDPOINT = "https://mail.google.com/a/";
    KeycloakSession session;
    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {

        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getSSOClientId(aliasName);
        String domainUrl = config.get(DOMAIN_URL);
        String acsrUrl = GOOGLE_A_ENDPOINT + domainUrl + "/acs";
        String googleSubAppslist = GeneralHelper.getConfigValue(config, SUPPORTED_APPS,"");
        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD, AUD_GOOGLE_COM);
        clientModel.setDescription(domainUrl);

        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(MAIL_A_ENDPOINT + domainUrl);
        redirectUris.add(acsrUrl);
        clientModel.setRedirectUris(redirectUris);
        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);
        clientModel.setAttribute(SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, acsrUrl);
        clientModel.setAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "");
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE, GOOGLE);
        clientModel.setAttribute(LOGO_URL, getLogo());
        clientModel.setAttribute(EDIT_DOMAIN_URL, domainUrl);
        clientModel.setAttribute(INSTALLATION_URL, getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID, getId());
        clientModel.setAttribute(HAS_SSO_SUB_APPS, "true");
        clientModel.setAttribute(SUPPORTED_APPS, googleSubAppslist);
        clientModel.setAttribute(EDIT_SUPPORTED_APPS, googleSubAppslist);
        clientModel.setAttribute(APP_LIST, String.join(",", GoogleWorkspaceSupportedApps.getAllNames()));

        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        client.setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer.NONE);
        client.setNameIDFormat(EMAIL);
        client.setIncludeAuthnStatement(true);
        client.setForceNameIDFormat(false);
        client.setRequiresRealmSignature(true);
        client.setForcePostBinding(true);
        client.setRequiresClientSignature(false);
        client.setRequiresAssertionSignature(false);

        if (client.requiresClientSignature() && client.getClientSigningCertificate() == null) {
            CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(clientModel.getClientId());
            client.setClientSigningCertificate(info.getCertificate());
            client.setClientSigningPrivateKey(info.getPrivateKey());

        }
        clientModel.setFrontchannelLogout(false);

        Optional<ClientScopeModel> clientScopeModel = session.getContext().getRealm().getClientScopesStream().filter(x -> x.getName().equals("role_list")).findFirst();
        clientScopeModel.ifPresent(clientModel::removeClientScope);

        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Email Mapper", EMAIL, EMAIL, "", EMAIL, false, null));
        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(GOOGLE_AUDIENCE, AUD_GOOGLE_COM));

        RealmModel realmModel =  session.getContext().getRealm();

        Arrays.stream(googleSubAppslist.split(",")).forEach(x -> {
            String realmRole = GeneralHelper.createRealmRole(clientModel, x);
            realmModel.addRole(realmRole);
        });
        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);
        return new InstallResponse(true, null, REALM_PATH+session.getContext().getRealm().getName()+SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        String domain = config.get(EDIT_DOMAIN_URL);
        if(StringUtils.isNotBlank(domain)) {
            clientModel.setAttribute(EDIT_DOMAIN_URL, domain);
        }

        String subapp = config.get(EDIT_SUPPORTED_APPS);
        if(subapp !=null && !subapp.isEmpty())
        {
            GeneralHelper.updateRolesBasedOnSubApps(session, clientModel, rep, subapp);
            clientModel.setAttribute(EDIT_SUPPORTED_APPS, subapp);
        }
        return new InstallResponse(true, null, null);
    }


    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel,String baseClientRedirectUri) {

        String domain = clientModel.getAttribute(EDIT_DOMAIN_URL);
        List<SSOBean> ssoBeans = new ArrayList<>();
        List<String> finalToInstall = GeneralHelper.getListOfSSOAppsTobeInstalled(session, clientModel);
        List<String> commonApps = new ArrayList<>(Arrays.asList("Google Meet", "Google Account", "Google Analytics", "Google Search",
                                                                "Google Maps", "Google Cloud", "Google Forms", "Google Chat","Google Slides",
                                                                "Google Sheets", "Google Admin Console","Youtube Studio", "Firebase"));

        finalToInstall.forEach(subApp -> {
            List<GoogleWorkspaceSupportedApps> apps = new ArrayList<>(GoogleWorkspaceSupportedApps.getUrlByName(subApp));
            SSOBean ssoBean = new SSOBean();
            apps.forEach(app -> {
                ssoBean.setDisplayName(app.getName());
                ssoBean.setLogoUrl(DEFAULT_SERVER_URL + MARKETPLACE_PATH + TemplateConstants.SSO_GOOGLE_WORKSPACE_TEMP + "/" + app.getLogoUrl());

                String appName = app.getName();
                String baseUrl = baseClientRedirectUri + "?RelayState=continue?continue=" + app.getRelayStateLink();

                if (commonApps.contains(appName)) {
                    ssoBean.setRedirectUrl(baseUrl + "&next=%2F&action_handle_signin=true");
                } else if ("Youtube".equals(appName)) {
                    ssoBean.setRedirectUrl(baseUrl + "signin&next=%2F&action_handle_signin=true");
                } else {
                    ssoBean.setRedirectUrl(baseUrl + domain + "&next=%2F&action_handle_signin=true");
                }

                ssoBean.setAppType(clientModel.getName());
                ssoBeans.add(ssoBean);
            });
        });

        return ssoBeans;
    }

    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_GOOGLE_WORKSPACE_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.GOOGLE_WORKSPACE;
    }

    @Override
    public String getAuthor(KeycloakSession session) {
        return session == null ? TemplateConstants.QV_AUTHOR : session.rootBrand().getMasterDisplayName();
    }

    @Override
    public String getCategory() {
        return Constants.SSO;
    }

    @Override
    public String getDescription(KeycloakSession session) {
        Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
        return MessageBundler.get(locale, TemplateConstants.SSO_GOOGLE_WORKSPACE_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_GOOGLE_WORKSPACE_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_GOOGLE_WORKSPACE_TEMP,GOOGLE);
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{GOOGLE,"SSO","Gmail","Google Admin Console","Google Calendar","Google Sites","Google Docs",
                "Google Drive","Google Meet","Google Keep","Google Account","Google Forms","Google Maps","Google Cloud",
                 "Google Chat","Google Slides","Google Sheets","Youtube","Youtube Studio","Firebase"};
    }
    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public Map<String, Object> getMarketProviders(KeycloakSession session) {
        return new HashMap<>();
    }

    @Override
    public String installType() {
        return TemplateConstants.MULTIPLE;
    }

    @Override
    public boolean isInstalled(KeycloakSession session) {
        return false;
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("name");
        configProperty.setLabel("sso-install-name");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setSkippableOnUpdate(true);
        configProperty.setHelpText("google-workspace-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(DOMAIN_URL);
        configProperty.setLabel("google-workspace-domain");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("google-workspace-domain-helptext");
        configProperties.add(configProperty);

        List<String> allNames = GoogleWorkspaceSupportedApps.getAllNames();
        configProperty = new ProviderConfigProperty();
        configProperty.setName(SUPPORTED_APPS);
        configProperty.setLabel("google-subapps-list");
        configProperty.setType(ProviderConfigProperty.MULTIVALUED_LIST_TYPE);
        configProperty.setOptions(allNames);
        configProperty.setRequired(true);
        configProperty.setHelpText("google-subapps-list-helptext");
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public MarketplaceTemplateProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "4ce24f5e-13e9-4d4a-a2a9-21d23542833d";
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {

        List<String> response = validateConfig(configProperties,config,isUpdate);
        if(!response.isEmpty())
            return response;

        response = validateSSOAlias(session.getContext().getRealm(),config);
        if(!response.isEmpty())
            return response;

        String domainUrl = config.get(DOMAIN_URL);
        if(Boolean.TRUE.equals(GeneralHelper.isValidUrl(domainUrl))){
            response.add("error-invalid-uri");
        }
        return response;
    }

    @Override
    public ThirdPartyProvider get3rdPartyProvider() {
        return null;
    }


    @Override
    public InstallResponse onCleanUp(RealmModel realmModel , ClientModel clientModel) {
        removeRolesHelper(realmModel,clientModel);
        String subApps = clientModel.getAttribute(EDIT_SUPPORTED_APPS);
        if (subApps != null && !subApps.isEmpty()){
            Arrays.stream(subApps.split(",")).forEach(x -> {
                String roleName = GeneralHelper.createRealmRole(clientModel, x);
                RoleModel role = session.getContext().getRealm().getRole(roleName);
                if (realmModel.getRole(roleName) != null){
                    session.roles().removeRole(role);
                }
            });
        }
        return new InstallResponse(true, null, null);
    }
}
