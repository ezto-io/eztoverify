package com.qlikverify.marketplace.templates.sso;

import com.qlikverify.marketplace.MessageBundler;
import com.qlikverify.marketplace.TemplateConstants;
import com.qlikverify.marketplace.templates.sso.base.SSOMarketplaceTemplate;
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
import org.keycloak.utils.StringUtil;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.*;
import static org.keycloak.models.Constants.SAML;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE;

public class CoralogixMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {

    KeycloakSession session;
    private static final String COROLOGIX = "Corologix";
    private static final String COROLOGIX_ENTITY_ID = "https://webapi.app.coralogix.in/saml/metadata.xml";
    private static final String COROLOGIX_ACS_URL = "https://webapi.app.coralogix.in/saml/assertion";
    private static final String AUDIENCE = "Audience";

    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel, String baseRedirectUrl) {
        return new ArrayList<>();
    }

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {
        String aliasName = config.get("name");

        if(StringUtil.isBlank(aliasName)){
            throw new MarketplaceException("Alias name cannot be empty");
        }

        String clientId = getSSOClientId(aliasName);

        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD, COROLOGIX_ENTITY_ID);
        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);
        clientModel.setAttribute(SKIP_DEFAULT_AUDIENCE,"true");

        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(COROLOGIX_ACS_URL);
        clientModel.setRedirectUris(redirectUris);
        clientModel.setAttribute(SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, COROLOGIX_ACS_URL + "/services/auth/idp/saml2/logout");
        clientModel.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, COROLOGIX_ACS_URL);
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE, COROLOGIX);
        clientModel.setAttribute(LOGO_URL, getLogo());
        clientModel.setAttribute(INSTALLATION_URL, getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID, getId());

        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        client.setNameIDFormat("email");
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

        clientModel.setFrontchannelLogout(false);
        Optional<ClientScopeModel> clientScopeModel = session.getContext().getRealm().getClientScopesStream().filter(x -> x.getName().equals("role_list")).findFirst();
        clientScopeModel.ifPresent(clientModel::removeClientScope);

        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(AUDIENCE, COROLOGIX_ENTITY_ID));
        RealmModel realmModel = session.getContext().getRealm();

        String roleName = generateRole(getSSOClientId(aliasName), COROLOGIX);

        if (realmModel.getRole(roleName) == null) {
            realmModel.addRole(roleName);
        }
        session.onBoarding().onApplicationOnboarding(clientModel, realmModel);

        return new InstallResponse(true, null, REALM_PATH + session.getContext().getRealm().getName() + SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {

        return null;
    }

    @Override
    public boolean isImplemented() {
        return true;
    }

    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_CORALOGIX_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return SSO_CORALOGIX;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_CORALOGIX_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_CORALOGIX_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_CORALOGIX_TEMP, COROLOGIX);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{COROLOGIX,"SSO","Monitoring tool"};
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
        configProperty.setHelpText("coralogix-name-helptext");
        configProperties.add(configProperty);

    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {
        List<String> response = validateConfig(configProperties, config, isUpdate);
        if (!response.isEmpty())
            return response;
        return validateSSOAlias(session.getContext().getRealm(), config);
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
        return "9dc7285e-80a5-455e-959d-30cf55534944";
    }

    @Override
    public InstallResponse onCleanUp(RealmModel realmModel, ClientModel clientModel) {
        removeRolesHelper(realmModel, clientModel);
        return new InstallResponse(true, null, null);
    }
}
