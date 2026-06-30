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
import static org.keycloak.protocol.saml.SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE;
import static org.keycloak.protocol.saml.mappers.SAMLAudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE;

public class SlackMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {

    public static final String SLACK_AUDIENCE = "Slack Audience";
    public static final String SLACK = "Slack";
    KeycloakSession session;

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {

        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getSSOClientId(aliasName);
        String entityId = GeneralHelper.getConfigValue(config, ENTITY_ID, SLACK_URL);
        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD, entityId);
        String teamName = GeneralHelper.getConfigValue(config, TEAM_NAME, "");
        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(HTTPS + teamName + "world.slack.com/");
        redirectUris.add(HTTPS + teamName + "world.slack.com/*");

        clientModel.setDescription(teamName);
        clientModel.setRedirectUris(redirectUris);
        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);
        clientModel.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, HTTPS + teamName + "world.slack.com/sso/saml");
        clientModel.setAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, HTTPS + teamName + "world.slack.com/sso/saml/logout");
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE, SLACK);
        clientModel.setAttribute(LOGO_URL, getLogo());
        clientModel.setAttribute(EDIT_SERVICE_PROVIDER_ISSUER, GeneralHelper.getConfigValue(config, ENTITY_ID, SLACK_URL));
        clientModel.setAttribute(EDIT_TEAM_NAME, teamName);
        clientModel.setAttribute(INSTALLATION_URL,getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID, getId());
        Optional<ClientScopeModel> clientScopeModel = session.getContext().getRealm().getClientScopesStream().filter(x -> x.getName().equals("role_list")).findFirst();
        clientScopeModel.ifPresent(clientModel::removeClientScope);
        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA1);
        client.setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer.NONE);
        client.setNameIDFormat("persistent");
        client.setIncludeAuthnStatement(true);
        client.setForceNameIDFormat(true);
        client.setRequiresRealmSignature(true);
        client.setForcePostBinding(true);
        client.setRequiresClientSignature(false);
        client.setRequiresAssertionSignature(true);

        if (client.requiresClientSignature() && client.getClientSigningCertificate() == null) {
            CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(clientModel.getClientId());
            client.setClientSigningCertificate(info.getCertificate());
            client.setClientSigningPrivateKey(info.getPrivateKey());

        }

        RealmModel realmModel = session.getContext().getRealm();

        String roleName = generateRole(clientId,SLACK);
        if(realmModel.getRole(roleName) == null)
        {
            realmModel.addRole(roleName);
        }

        clientModel.setFrontchannelLogout(false);

        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Email Mapper", "email", "User.Email", "", "email", false, null));
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Username Mapper", "username", "User.Username", "", "Username", false, null));
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Lastname Mapper", "lastName", "last_name", "", "surname", false, null));
        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Firstname Mapper", "firstName", "first_name", "", "givenName", false, null));
        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(SLACK_AUDIENCE, entityId));
        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);
        return new InstallResponse(true, null, REALM_PATH+session.getContext().getRealm().getName()+SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        String entityId = config.get(EDIT_SERVICE_PROVIDER_ISSUER);
        clientModel.setAttribute(AUD, entityId);
        String teamName = config.get(EDIT_TEAM_NAME);
        rep.getAttributes().put(AUD,entityId);

        clientModel.setAttribute(EDIT_SERVICE_PROVIDER_ISSUER, config.get(EDIT_SERVICE_PROVIDER_ISSUER));
        clientModel.setAttribute(EDIT_TEAM_NAME, teamName);
        if( (StringUtils.isNotBlank(entityId)) && (rep.getProtocolMappers()!=null) )
        {
            rep.getProtocolMappers().stream().filter(x->
                    x.getName().equals(SLACK_AUDIENCE)).forEach(x->
                    x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE,entityId));
        }

        return new InstallResponse(true, null, null);
    }

    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_SLACK_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.SLACK;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_SLACK_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_SLACK_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_SLACK_TEMP,SLACK);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{SLACK,"SSO","Team Collaboration","Messaging Platform"};
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
        configProperty.setHelpText("slack-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(TEAM_NAME);
        configProperty.setLabel("slack-team-name");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("slack-team-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ENTITY_ID);
        configProperty.setLabel("slack-service-provider");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("slack-service-provider-helptext");
        configProperty.setDefaultValue(SLACK_URL);
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
        return "62a47396-fcc2-4b95-ba89-e6e2ac3e6ce2";
    }

    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel,String baseClientRedirectUri) {
        return new ArrayList<>();
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {
        List<String> response = validateConfig(configProperties,config,isUpdate);
        if(!response.isEmpty())
            return response;

        return validateSSOAlias(session.getContext().getRealm(),config);
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
