package com.qlikverify.marketplace.templates.sso;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.TemplateConstants;
import com.qlikverify.marketplace.templates.sso.base.SSOMarketplaceTemplate;
import com.qlikverify.marketplace.MessageBundler;
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

public class AhaMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {
    public static final String AHA_AUDIENCE = "Aha Audience";
    public static final String AHA = "Aha";
    public static final String AHA_DOMAIN_ENDPOINT = ".aha.io/";
    public static final String AHA_SUBDOMAIN_CALLBACK_ENDPOINT = ".aha.io/auth/saml/callback";

    KeycloakSession session;

    @Override
    public MarketplaceTemplateProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel, String baseRedirectUrl) {
        return new ArrayList<>();
    }

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {
        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getSSOClientId(aliasName);
        String subDomain = GeneralHelper.getConfigValue(config, SUB_DOMAIN_NAME, "");
        String entityId = GeneralHelper.getConfigValue(config, ENTITY_ID, HTTPS + subDomain +AHA_DOMAIN_ENDPOINT);
        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD, entityId);
        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(HTTPS + subDomain + AHA_DOMAIN_ENDPOINT+"*");

        clientModel.setDescription(subDomain);
        clientModel.setRedirectUris(redirectUris);
        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);
        clientModel.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, HTTPS + subDomain +AHA_SUBDOMAIN_CALLBACK_ENDPOINT);
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE, AHA);
        clientModel.setAttribute(LOGO_URL, getLogo());
        clientModel.setAttribute(EDIT_SERVICE_PROVIDER_ISSUER, GeneralHelper.getConfigValue(config, ENTITY_ID, HTTPS + subDomain + AHA_DOMAIN_ENDPOINT));
        clientModel.setAttribute(EDIT_SUB_DOMAIN_NAME, subDomain);
        clientModel.setAttribute(INSTALLATION_URL,getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID, getId());
        clientModel.setAttribute(SKIP_DEFAULT_AUDIENCE,"true");
        Optional<ClientScopeModel> clientScopeModel = session.getContext().getRealm().getClientScopesStream().filter(x -> x.getName().equals("role_list")).findFirst();
        clientScopeModel.ifPresent(clientModel::removeClientScope);
        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA1);
        client.setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer.NONE);
        client.setNameIDFormat(PERSISTENT_NAME_ID);
        client.setIncludeAuthnStatement(true);
        client.setForceNameIDFormat(false);
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

        String roleName = generateRole(clientId,AHA);
        if(realmModel.getRole(roleName) == null)
        {
            realmModel.addRole(roleName);
        }

        clientModel.setFrontchannelLogout(false);

        clientModel.addProtocolMapper(UserPropertyAttributeStatementMapper.createAttributeMapper("Email Mapper", "email", "User.email", "", "email", false, null));
        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(AHA_AUDIENCE, entityId));
        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);
        return new InstallResponse(true, null, REALM_PATH+session.getContext().getRealm().getName()+SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        String subDomain = config.get(EDIT_SUB_DOMAIN_NAME);
        String entityId = config.get(EDIT_SERVICE_PROVIDER_ISSUER);
        rep.getAttributes().put(EDIT_SERVICE_PROVIDER_ISSUER,entityId);
        rep.getAttributes().put(AUD,entityId);

        clientModel.setAttribute(AUD, entityId);

        clientModel.setAttribute(EDIT_SERVICE_PROVIDER_ISSUER, entityId);
        clientModel.setAttribute(EDIT_SUB_DOMAIN_NAME, subDomain);
        if(rep.getProtocolMappers()!=null) {
            rep.getProtocolMappers().stream().filter(x->
                    x.getName().equals(AHA_AUDIENCE)).forEach(x->
                    x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE, entityId));
        }

        return new InstallResponse(true, null, null);
    }

    @Override
    public InstallResponse onCleanUp(RealmModel realmModel, ClientModel clientModel) {
        removeRolesHelper(realmModel,clientModel);
        return new InstallResponse(true, null, null);
    }

    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_AHA_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.AHA;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_AHA_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_AHA_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_AHA_TEMP,AHA);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{"aha","SSO", "aha.io", "roadmaps", "aha_products", "aha_roadmaps"};
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
//        key is sent to front-end through setLabel for text-translation
        configProperty.setLabel("sso-install-name");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setSkippableOnUpdate(true);
//        key is sent to front-end through setHelpText for text-translation
        configProperty.setHelpText("aha-name-helpText");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(SUB_DOMAIN_NAME);
        configProperty.setLabel("aha-sub-domain");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("aha-subdomain-helptext");
        configProperties.add(configProperty);

    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
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
        return "62a47496-fac1-4b45-ba89-e6e2ec3c6ce2";
    }
}
