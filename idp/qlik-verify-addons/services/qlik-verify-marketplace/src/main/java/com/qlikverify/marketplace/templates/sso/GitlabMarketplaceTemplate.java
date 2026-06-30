package com.qlikverify.marketplace.templates.sso;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
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

import static com.qlikverify.marketplace.TemplateConstants.*;
import static org.keycloak.models.Constants.SAML;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.protocol.saml.mappers.SAMLAudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE;

public class GitlabMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {

    public static final String GITLAB = "Gitlab";
    public static final String GITLAB_AUDIENCE = "Gitlab Audience";
    public static final String GITLAB_BASE_URL = "gitlab_base_url";
    public static final String EDIT_GITLAB_BASE_URL = "edit_gitlab_base_url";
    public static final String SAML_CALLBACK_ENDPOINT = "/users/auth/saml/callback";
    KeycloakSession session;
    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {


        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getSSOClientId(aliasName);
        String issuerUrl = GeneralHelper.getConfigValue(config, GITLAB_BASE_URL, "");
        String acsUrl = issuerUrl+SAML_CALLBACK_ENDPOINT;
        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD,issuerUrl);
        String redirectUrl  = issuerUrl;
        if(!redirectUrl.endsWith("/"))
        {
            redirectUrl = redirectUrl+"/*";
        }
        else
        {
            redirectUrl = redirectUrl+"*";
        }
        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(redirectUrl);
        redirectUris.add(acsUrl);
        clientModel.setRedirectUris(redirectUris);
        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);
        clientModel.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,acsUrl);
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE, GITLAB);
        clientModel.setAttribute(LOGO_URL,getLogo());
        clientModel.setAttribute(EDIT_GITLAB_BASE_URL,issuerUrl);
        clientModel.setAttribute(INSTALLATION_URL,getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID,getId());

        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        client.setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer.NONE);
        client.setNameIDFormat("transient");
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

        Optional<ClientScopeModel> clientScopeModel = session.getContext().getRealm().getClientScopesStream().filter(x->x.getName().equals("role_list")).findFirst();
        clientScopeModel.ifPresent(clientModel::removeClientScope);

        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Email Mapper",EMAIL,EMAIL,"",EMAIL,false,null));
        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(GITLAB_AUDIENCE,issuerUrl));

        RealmModel realmModel =  session.getContext().getRealm();

        String roleName = generateRole(clientId,GITLAB);
        if(realmModel.getRole(roleName) == null)
        {
            realmModel.addRole(roleName);
        }
        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);
        return new InstallResponse(true, null, REALM_PATH+session.getContext().getRealm().getName()+SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        String issuerUrl = config.get(EDIT_GITLAB_BASE_URL);
        rep.getAttributes().put(AUD,issuerUrl);

        clientModel.setAttribute(EDIT_GITLAB_BASE_URL,issuerUrl);
        if(StringUtils.isNotBlank(issuerUrl) && (rep.getProtocolMappers()!=null) )
        {
            rep.getProtocolMappers().stream().filter(x->
                    x.getName().equals(GITLAB_AUDIENCE)).forEach(x->
                    x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE,issuerUrl));
        }
        return null;
    }

    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_GITLAB_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.GITLAB;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_GITLAB_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_GITLAB_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_GITLAB_TEMP,GITLAB);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{GITLAB,"SSO","Source Control","Code Collaboration",
                "Version Control System", "Git Repository Hosting","Project Management"};
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
        configProperty.setHelpText("gitlab-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(GITLAB_BASE_URL);
        configProperty.setLabel("gitlab-base-url");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("gitlab-base-url-helptext");
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
        return "c7243f08-6b70-4124-a41f-c4e8fa40c3b8";
    }

    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel,String baseClientRedirectUri) {
        return new ArrayList<>();
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {
        List<String> response = validateConfig(configProperties, config,isUpdate);
        if (!response.isEmpty())
            return response;

        response = validateSSOAlias(session.getContext().getRealm(),config);
        if(!response.isEmpty())
            return response;

        String gitlabBaseUrl = config.get(GITLAB_BASE_URL);
        if(Boolean.TRUE.equals(!GeneralHelper.isValidUrl(gitlabBaseUrl))){
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
        return new InstallResponse(true, null, null);
    }
}
