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

public class YoutrackMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {

    public static final String YOUTRACK_AUDIENCE = "Youtrack Audience";
    public static final String YOUTRACK = "Youtrack";
    public static final String EDIT_YOUTRACK_DOMAIN_URL = "edit_youtrack_domain_url";
    public static final String YOUTRACK_DOMAIN_URL = "youtrack_domain_url";
    KeycloakSession session;

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {

        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getSSOClientId(aliasName);
        String entityId = GeneralHelper.getConfigValue(config, ENTITY_ID, "");

        String youtrackDomainUrl = GeneralHelper.getConfigValue(config,YOUTRACK_DOMAIN_URL, "");
        String acsUrl = GeneralHelper.getConfigValue(config, ACS_URL, "");

        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD, entityId);
        String redirectUrl = youtrackDomainUrl;
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
        clientModel.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, acsUrl);
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE, YOUTRACK);
        clientModel.setAttribute(LOGO_URL, getLogo());
        clientModel.setAttribute(EDIT_YOUTRACK_DOMAIN_URL, youtrackDomainUrl);
        clientModel.setAttribute(EDIT_ACS_URL, acsUrl);
        clientModel.setAttribute(EDIT_ENTITY_ID, entityId, true);
        clientModel.setAttribute(INSTALLATION_URL, getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID, getId());

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
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Username Mapper", USERNAME, USERNAME, "", USERNAME, false, null));
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Lastname Mapper", "lastName", "lastname", "", "lastname", false, null));
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Firstname Mapper", "firstName", "firstname", "", "firstname", false, null));
        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(YOUTRACK_AUDIENCE, entityId));

        RealmModel realmModel = session.getContext().getRealm();

        String roleName = generateRole(clientId,YOUTRACK);
        if(realmModel.getRole(roleName) == null)
        {
            realmModel.addRole(roleName);
        }
        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);
        return new InstallResponse(true, null, REALM_PATH + session.getContext().getRealm().getName() + SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        String entityId = config.get(EDIT_ENTITY_ID);
        String acsUrl = config.get(EDIT_ACS_URL);
        clientModel.setAttribute(AUD, entityId);
        String youtrackDomainUrl = config.get(EDIT_YOUTRACK_DOMAIN_URL);
        rep.getAttributes().put(AUD,entityId);

        clientModel.setAttribute(EDIT_YOUTRACK_DOMAIN_URL, youtrackDomainUrl);
        clientModel.setAttribute(EDIT_ACS_URL, acsUrl, true);
        clientModel.setAttribute(EDIT_ENTITY_ID, entityId, true);
        if( (StringUtils.isNotBlank(entityId)) && (rep.getProtocolMappers()!=null) )
        {
            rep.getProtocolMappers().stream().filter(x->
                    x.getName().equals(YOUTRACK_AUDIENCE)).forEach(x->
                    x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE,entityId));
        }
        return new InstallResponse(true, null, REALM_PATH + session.getContext().getRealm().getName() + SSO_PATH);
    }

    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_YOUTRACK_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.YOUTRACK;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_YOUTRACK_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_YOUTRACK_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_YOUTRACK_TEMP,YOUTRACK);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{YOUTRACK,"SSO","Issue Tracking","Project Management","Agile"};
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
        configProperty.setHelpText("youtrack-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(YOUTRACK_DOMAIN_URL);
        configProperty.setLabel("youtrack-domain-name");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("youtrack-domain-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ACS_URL);
        configProperty.setLabel("acs-url");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("youtrack-acs-url-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ENTITY_ID);
        configProperty.setLabel("youtrack-entityID");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("youtrack-entityID-helptext");
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
        return "c5c8b330-6727-4df1-a0e0-c00173086f39";
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

        String youtrackDomainUrl = config.get(YOUTRACK_DOMAIN_URL);
        String acsUrl = config.get(ACS_URL);
        if(Boolean.TRUE.equals(!GeneralHelper.isValidUrl(acsUrl) || !GeneralHelper.isValidUrl(youtrackDomainUrl))){
            response.add("error-invalid-uri");
        }
        return response;
    }

    @Override
    public ThirdPartyProvider get3rdPartyProvider() {
        return null;
    }


    @Override
    public InstallResponse onCleanUp(RealmModel realm , ClientModel client) {
        removeRolesHelper(realm,client);
        return new InstallResponse(true, null, null);
    }
}
