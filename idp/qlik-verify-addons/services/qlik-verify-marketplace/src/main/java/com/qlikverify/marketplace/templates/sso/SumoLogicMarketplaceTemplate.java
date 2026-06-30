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
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SignatureAlgorithm;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.*;
import static org.keycloak.models.Constants.SAML;
import static org.keycloak.protocol.saml.mappers.SAMLAudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE;

public class SumoLogicMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {

    KeycloakSession session;
    public static final String SUMO_LOGIC = "SumoLogic";
    public static final String SUMO_LOGIC_AUDIENCE = "Sumo Logic Audience";


    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel, String baseRedirectUrl) {
        return new ArrayList<>();
    }

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {
        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getSSOClientId(aliasName);
        String entityId = KeycloakModelUtils.generateId();
        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(SAML);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD,entityId);

        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE, SUMO_LOGIC);
        clientModel.setAttribute(LOGO_URL,getLogo());
        clientModel.setAttribute(EDIT_ENTITY_ID,entityId);
        clientModel.setAttribute(INSTALLATION_URL,getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID,getId());

        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        client.setNameIDFormat("username");
        client.setIncludeAuthnStatement(true);
        client.setForceNameIDFormat(false);
        client.setRequiresRealmSignature(true);
        client.setForcePostBinding(false);
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

        RealmModel realmModel =  session.getContext().getRealm();

        String roleName =generateRole(clientId, SUMO_LOGIC);
        if(realmModel.getRole(roleName) == null)
        {
            realmModel.addRole(roleName);
        }
        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);
        return new InstallResponse(true, null, "realms/"+session.getContext().getRealm().getName()+SSO_PATH);

    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        String entityId = config.get(EDIT_ENTITY_ID);
        clientModel.setAttribute(AUD,entityId);
        rep.getAttributes().put(AUD,entityId);
        clientModel.setAttribute(EDIT_ENTITY_ID,entityId);
        if( (StringUtils.isNotBlank(entityId)) && (rep.getProtocolMappers()!=null) )
        {
            rep.getProtocolMappers().stream().filter(x->
                    x.getName().equals(SUMO_LOGIC_AUDIENCE)).forEach(x->
                    x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE,entityId));
        }
        return null;
    }


    @Override
    public String getLogo() {
        return generateLogoUrl(TemplateConstants.SSO_SUMO_LOGIC_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.SUMO_LOGIC;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_SUMO_LOGIC_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(TemplateConstants.SSO_SUMO_LOGIC_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(TemplateConstants.SSO_SUMO_LOGIC_TEMP,SUMO_LOGIC);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{SUMO_LOGIC,"SSO","Data Analytics","Log Management","Performance Monitoring"};
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
        configProperty.setHelpText("sumologic-name-helptext");
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
        return "61cb8f53-924e-4a78-95aa-453124ec0697";
    }

    @Override
    public InstallResponse onCleanUp(RealmModel realmModel , ClientModel clientModel) {
        removeRolesHelper(realmModel,clientModel);
        return new InstallResponse(true, null, null);
    }
}
