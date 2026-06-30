package com.qlikverify.marketplace.templates.sso;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import com.qlikverify.marketplace.ZohoSupportedApps;
import com.qlikverify.marketplace.templates.sso.base.SSOMarketplaceTemplate;
import com.qlikverify.marketplace.MessageBundler;
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
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE;
import static org.keycloak.protocol.saml.mappers.SAMLAudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE;

public class ZohoMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {

    public static final String ZOHO_AUDIENCE = "Zoho Audience";
    public static final String ZOHO = "Zoho";

    public static final String ZOHO_ENTITY_ID = "zoho.in";
    KeycloakSession session;

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {

        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getSSOClientId(aliasName);
        String ssoSignInUrl = GeneralHelper.getConfigValue(config, ACS_URL, "");
        String ssoSignoutUrl = GeneralHelper.getConfigValue(config, SIGNOUT_URL, "");
        String entityId = GeneralHelper.getConfigValue(config,ENTITY_ID,ZOHO_ENTITY_ID);
        String zohoSubAppslist = GeneralHelper.getConfigValue(config,SUPPORTED_APPS,"");
        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD, entityId);
        String redirectUrl = ssoSignInUrl;
        if (!redirectUrl.endsWith("/")) {
            redirectUrl = redirectUrl + "/*";
        } else {
            redirectUrl = redirectUrl + "*";
        }
        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(redirectUrl);
        clientModel.setRedirectUris(redirectUris);
        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);
        clientModel.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, ssoSignInUrl);
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE, ZOHO);
        clientModel.setAttribute(LOGO_URL, getLogo());
        clientModel.setAttribute(INSTALLATION_URL, getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID,getId());
        clientModel.setAttribute(HAS_SSO_SUB_APPS, "true");
        clientModel.setAttribute(SUPPORTED_APPS, zohoSubAppslist);
        clientModel.setAttribute(EDIT_SUPPORTED_APPS, zohoSubAppslist);
        clientModel.setAttribute(EDIT_ENTITY_ID,entityId);
        clientModel.setAttribute(EDIT_ACS_URL,ssoSignInUrl);
        clientModel.setAttribute(EDIT_SIGNOUT_URL,ssoSignoutUrl);
        clientModel.setAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,ssoSignoutUrl);
        clientModel.setAttribute(APP_LIST, String.join(",", ZohoSupportedApps.getAllNames()));

        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        client.setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer.NONE);
        client.setNameIDFormat("email");
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

        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Email Mapper", "email", "emailaddress", "", "emailaddress", false, null));
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Username Mapper", "username", "name", "", "name", false, null));
        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(ZOHO_AUDIENCE,entityId));
        RealmModel realmModel =  session.getContext().getRealm();

        Arrays.stream(zohoSubAppslist.split(",")).forEach(x -> {
            String realmRole = GeneralHelper.createRealmRole(clientModel, x);
            realmModel.addRole(realmRole);
        });

        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);
        return new InstallResponse(true, null, REALM_PATH+session.getContext().getRealm().getName()+SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        String ssoSignInUrl = config.get(EDIT_ACS_URL);
        if(StringUtils.isNotBlank(ssoSignInUrl)) {
            clientModel.setAttribute(EDIT_ACS_URL, ssoSignInUrl);
        }

        String entityId = config.get(EDIT_ENTITY_ID);
        if(StringUtils.isNotBlank(entityId))
        {
            clientModel.setAttribute(EDIT_ENTITY_ID, entityId);
            clientModel.setAttribute(AUD, entityId);
            rep.getAttributes().put(AUD,entityId);
            if(rep.getProtocolMappers()!=null){
                rep.getProtocolMappers().stream().filter(x->
                        x.getName().equals(ZOHO_AUDIENCE)).forEach(x->
                        x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE,entityId));
            }
        }

        String ssoSignoutUrl = config.get(EDIT_SIGNOUT_URL);
        if(ssoSignoutUrl !=null && !ssoSignoutUrl.isEmpty())
        {
            clientModel.setAttribute(EDIT_SIGNOUT_URL, ssoSignoutUrl);
        }
        String subapp = config.get(EDIT_SUPPORTED_APPS);
        if(subapp !=null && !subapp.isEmpty())
        {
            GeneralHelper.updateRolesBasedOnSubApps(session, clientModel, rep, subapp);
            clientModel.setAttribute(EDIT_SUPPORTED_APPS, subapp);


        }

        return new InstallResponse(true,"",REALM_PATH+session.getContext().getRealm().getName()+SSO_PATH);
    }


    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_ZOHO_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.ZOHO;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_ZOHO_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_ZOHO_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_ZOHO_TEMP,ZOHO);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{ZOHO,"SSO","CRM","Accounting and Invoicing","Project Management"};
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

    public static final String I_HAVE_A_DOMAIN = "I have a domain";

    public static final String I_DONT_HAVE_DOMAIN = "I dont have domain";

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("name");
        configProperty.setLabel("sso-install-name");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("zoho-name-helptext");
        configProperty.setSkippableOnUpdate(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ENTITY_ID);
        configProperty.setLabel("zoho-entityID");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("zoho-entityID-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ACS_URL);
        configProperty.setLabel("acs-url");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("zoho-acs-url-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(SIGNOUT_URL);
        configProperty.setLabel("zoho-signout-url");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("zoho-signout-url-helptext");
        configProperties.add(configProperty);

        List<String> allNames = ZohoSupportedApps.getAllNames();
        configProperty = new ProviderConfigProperty();
        configProperty.setName(SUPPORTED_APPS);
        configProperty.setLabel("zoho-subapps-list");
        configProperty.setType(ProviderConfigProperty.MULTIVALUED_LIST_TYPE);
        configProperty.setOptions(allNames);
        configProperty.setRequired(true);
        configProperty.setHelpText("zoho-subapps-list-helptext");
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
        return "c799a3ca-9ad9-4d89-a20f-e4ca26aef5e4";
    }

    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel,String baseClientRedirectUri) {

        List<SSOBean> ssoBeans = new ArrayList<>();
        List<String> finalToInstall = GeneralHelper.getListOfSSOAppsTobeInstalled(session, clientModel);
        finalToInstall.forEach(subApp -> {
            List<ZohoSupportedApps> apps = ZohoSupportedApps.getUrlByName(subApp).stream().collect(Collectors.toList());
            SSOBean ssoBean = new SSOBean();
            apps.forEach(app -> {
                ssoBean.setDisplayName(app.getName());
                ssoBean.setLogoUrl(DEFAULT_SERVER_URL + MARKETPLACE_PATH + SSO_ZOHO_TEMP + "/" +app.getLogoUrl());
                ssoBean.setRedirectUrl(baseClientRedirectUri+"?RelayState=" + app.getRelayStateLink());
                ssoBean.setAppType(clientModel.getName());
                ssoBeans.add(ssoBean);
            });
        });

        return ssoBeans;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {
        List<String> response = validateConfig(configProperties, config,isUpdate);
        if (!response.isEmpty())
            return response;

        response = validateSSOAlias(session.getContext().getRealm(),config);
        if(!response.isEmpty())
            return response;

        String acsUrl = config.get(ACS_URL);
        String signoutUrl = config.get(SIGNOUT_URL);
        if(Boolean.TRUE.equals(!GeneralHelper.isValidUrl(acsUrl) || !GeneralHelper.isValidUrl(signoutUrl))){
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
