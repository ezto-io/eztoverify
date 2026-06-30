package com.qlikverify.marketplace.templates.sso;

import com.qlikverify.marketplace.GeneralHelper;
import com.qlikverify.marketplace.MessageBundler;
import com.qlikverify.marketplace.TemplateConstants;
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

public class OneDeskMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {
    public static final String ONE_DESK = "OneDesk";
    public static final String ONE_DESK_AUDIENCE = "OneDesk Audience";
    KeycloakSession session;

    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel, String baseRedirectUrl) {
        return new ArrayList<>();
    }

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
        clientModel.setAttribute(SSO_TYPE, ONE_DESK);
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(LOGO_URL, getLogo());
        clientModel.setAttribute(EDIT_ENTITY_ID, entityId);
        clientModel.setAttribute(EDIT_ACS_URL, acsUrl);
        clientModel.setAttribute(INSTALLATION_URL, getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID, getId());

        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        client.setNameIDFormat("username");
        client.setIncludeAuthnStatement(true);
        client.setForceNameIDFormat(false);
        client.setRequiresRealmSignature(true);
        client.setForcePostBinding(false);
        client.setRequiresClientSignature(false);

        if (client.requiresClientSignature() && client.getClientSigningCertificate() == null) {
            CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(clientModel.getClientId());
            client.setClientSigningCertificate(info.getCertificate());
            client.setClientSigningPrivateKey(info.getPrivateKey());
        }

        clientModel.setFrontchannelLogout(false);
        Optional<ClientScopeModel> clientScopeModel = session
                                                        .getContext()
                                                        .getRealm()
                                                        .getClientScopesStream()
                                                        .filter(x -> x.getName()
                                                                .equals("role_list"))
                                                        .findFirst();

        clientScopeModel.ifPresent(clientModel::removeClientScope);

        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(ONE_DESK_AUDIENCE, entityId));
        RealmModel realmModel = session.getContext().getRealm();
        String roleName = generateRole(clientId, ONE_DESK);

        if (realmModel.getRole(roleName) == null) {
            realmModel.addRole(roleName);
        }

        session.onBoarding().onApplicationOnboarding(clientModel, realmModel);
        return new InstallResponse(true, null, REALM_PATH + session.getContext().getRealm().getName() + SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {

        String entityId = config.get(EDIT_ENTITY_ID);
        String acsUrl = config.get(EDIT_ACS_URL);

        checkConfigIsValid(true,"",entityId,acsUrl);

        clientModel.setAttribute(AUD, entityId);
        rep.getAttributes().put(AUD, entityId);
        clientModel.setAttribute(EDIT_ENTITY_ID, entityId);
        clientModel.setAttribute(EDIT_ACS_URL, acsUrl);

        rep.getAttributes().put(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, acsUrl);
        if ( (StringUtils.isNotBlank(entityId)) && (rep.getProtocolMappers() != null) ) {
            rep.getProtocolMappers().stream().filter(x ->
                    x.getName().equals(ONE_DESK_AUDIENCE)).forEach(x ->
                    x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE, entityId));
        }
        return null;
    }

    @Override
    public boolean isImplemented() {
        return true;
    }

    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_ONE_DESK_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.ONE_DESK;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_ONE_DESK_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_ONE_DESK_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_ONE_DESK_TEMP, ONE_DESK);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{ONE_DESK, "SSO", "Helpdesk Software", "Customer Support"};
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
        configProperty.setHelpText("one-desk-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ENTITY_ID);
        configProperty.setLabel("one-desk-entityID");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("one-desk-entityID-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ACS_URL);
        configProperty.setLabel("acs-url");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("one-desk-acs-url-helptext");
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
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
        return "651a2631-5cac-4630-a2d1-49fab645dc9e";
    }

    @Override
    public InstallResponse onCleanUp(RealmModel realmModel, ClientModel clientModel) {
        removeRolesHelper(realmModel, clientModel);
        return new InstallResponse(true, null, null);
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
}
