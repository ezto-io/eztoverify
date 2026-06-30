package com.qlikverify.marketplace.templates.sso;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.MessageBundler;
import com.qlikverify.marketplace.TemplateConstants;
import com.qlikverify.marketplace.templates.sso.base.SSOMarketplaceTemplate;
import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
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

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.*;
import static com.qlikverify.marketplace.TemplateConstants.ENTITY_ID;
import static org.keycloak.models.Constants.SAML;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.protocol.saml.mappers.SAMLAudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE;

public class AssetSonarMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {

    public static final String ASSET_SONAR_AUDIENCE = "AssetSonarAudience";
    public static final String ASSET_SONAR = "AssetSonar";
    KeycloakSession session;
    private static final Logger LOGGER = Logger.getLogger(AssetSonarMarketplaceTemplate.class);
    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {

        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String entityId = GeneralHelper.getConfigValue(config, ENTITY_ID, "");
        String acsUrl = GeneralHelper.getConfigValue(config, ACS_URL, "");

        //checking Is the value empty or not? If so, an error will be thrown.
        checkConfigIsValid(false,aliasName,entityId,acsUrl);

        String clientId = getSSOClientId(aliasName);

        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD, entityId);

        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(acsUrl);
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
        clientModel.setAttribute(SSO_TYPE, ASSET_SONAR);
        clientModel.setAttribute(LOGO_URL, getLogo());
        clientModel.setAttribute(EDIT_ACS_URL, acsUrl);
        clientModel.setAttribute(EDIT_ENTITY_ID, entityId, true);
        clientModel.setAttribute(INSTALLATION_URL, getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID, getId());

        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
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

        RealmModel realmModel = session.getContext().getRealm();

        Optional<ClientScopeModel> clientScopeModel = realmModel
                                                    .getClientScopesStream()
                                                    .filter(x -> x.getName().equals("role_list"))
                                                    .findFirst();
        clientScopeModel.ifPresent(clientModel::removeClientScope);

        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Email Mapper", EMAIL, EMAIL, "", EMAIL, false, null));
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Lastname Mapper", "lastName", "last_name", "", "lastname", false, null));
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Firstname Mapper", "firstName", "first_name", "", "firstname", false, null));
        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(ASSET_SONAR_AUDIENCE, entityId));

        String roleName = generateRole(clientId, ASSET_SONAR);
        if(realmModel.getRole(roleName) == null)
        {
            realmModel.addRole(roleName);
        }

        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);

        LOGGER.info("Asset Sonar SSO installed successfully");

        return new InstallResponse(true, null, REALM_PATH + session.getContext().getRealm().getName() + SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {

        String entityId = GeneralHelper.getConfigValue(config, EDIT_ENTITY_ID, "");
        String acsUrl = GeneralHelper.getConfigValue(config, EDIT_ACS_URL, "");

        //checking Is the value empty or not? If so, an error will be thrown.
        checkConfigIsValid(true,"",entityId,acsUrl);

        clientModel.setAttribute(AUD, entityId);
        rep.getAttributes().put(AUD,entityId);
        clientModel.setAttribute(EDIT_ACS_URL, acsUrl, true);
        clientModel.setAttribute(EDIT_ENTITY_ID, entityId, true);

        if(rep.getProtocolMappers()!=null){
            rep.getProtocolMappers().stream().filter(x->
                    x.getName().equals(ASSET_SONAR_AUDIENCE)).forEach(x->
                    x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE,entityId));
        }

        LOGGER.info("Asset Sonar SSO updated successfully");

        return new InstallResponse(true, null, REALM_PATH + session.getContext().getRealm().getName() + SSO_PATH);
    }

    private void checkConfigIsValid(Boolean isUpdate, String aliasName, String entityId, String acsUrl) throws MarketplaceException {
        if((!isUpdate && StringUtils.isBlank(aliasName))){
            throw new MarketplaceException("Mandatory fields are required");
        }
        if(StringUtils.isBlank(entityId)){
            throw new MarketplaceException("Entity ID cannot be empty");
        }
        if (StringUtils.isBlank(acsUrl)|| !acsUrl.startsWith(HTTPS)) {
            throw new MarketplaceException("ACS url can't be in empty and needs to start's with https");
        }
    }
        @Override
    public String getLogo() {
        return generateLogoUrl(SSO_ASSET_SONAR_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.ASSET_SONAR;
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
        return MessageBundler.get(locale, SSO_ASSET_SONAR_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_ASSET_SONAR_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_ASSET_SONAR_TEMP, ASSET_SONAR);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{ASSET_SONAR,"SSO","IT management","Asset management","Analytics"};
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
        configProperty.setHelpText("asset-sonar-sso-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ENTITY_ID);
        configProperty.setLabel("asset-sonar-entityID");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("asset-sonar-entityID-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ACS_URL);
        configProperty.setLabel("acs-url");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("asset-sonar-acs-url-helptext");
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
        return "79ca1f8d-3f3a-4426-a69b-f43d8e7d59b1";
    }


    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel, String baseClientRedirectUri) {
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

        String acsUrl = config.get(ACS_URL);
        if(Boolean.TRUE.equals(!GeneralHelper.isValidUrl(acsUrl))){
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

