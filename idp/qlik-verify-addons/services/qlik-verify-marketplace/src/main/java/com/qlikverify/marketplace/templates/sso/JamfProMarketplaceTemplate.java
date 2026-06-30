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
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SignatureAlgorithm;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.*;
import static org.keycloak.protocol.saml.SamlProtocol.*;
import static org.keycloak.protocol.saml.mappers.SAMLAudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE;

public class JamfProMarketplaceTemplate extends SSOMarketplaceTemplate implements MarketplaceSSOTemplateProvider, MarketplaceTemplateProviderFactory {

    KeycloakSession session;
    public static final String JAMFPRO = "JamfPro";
    public static final String JAMFPRO_AUDIENCE = "JamfPro Audience";

    public static final String JAMFPRO_ACS_URL = "JamfPro_acs_url";
    public static final String EDIT_JAMFPRO_ACS_URL = "edit_JamfPro_acs_url";

    @Override
    public List<SSOBean> getSSOSubApps(ClientModel clientModel, String baseRedirectUrl) {
        return new ArrayList<>();
    }

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {
        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getSSOClientId(aliasName);
        String entityId = GeneralHelper.getConfigValue(config, ENTITY_ID, "");
        String jamfProAcsUrl = GeneralHelper.getConfigValue(config, JAMFPRO_ACS_URL, "");
        if(!(jamfProAcsUrl.startsWith(HTTPS) || jamfProAcsUrl.startsWith(HTTP)))
            jamfProAcsUrl = HTTPS+jamfProAcsUrl;
        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol(LOGIN_PROTOCOL);
        clientModel.setClientId(clientId);
        clientModel.setAttribute(AUD,entityId);

        clientModel.setStandardFlowEnabled(true);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);
        clientModel.setAttribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,jamfProAcsUrl);
        clientModel.setAttribute(IS_SSO, "true");
        clientModel.setAttribute(HAS_IN_BUILT_ROLES, "true");
        clientModel.setAttribute(SSO_TYPE , JAMFPRO);
        clientModel.setAttribute(LOGO_URL,getLogo());
        clientModel.setAttribute(EDIT_JAMFPRO_ACS_URL,jamfProAcsUrl);
        clientModel.setAttribute(EDIT_ENTITY_ID,entityId);
        clientModel.setAttribute(INSTALLATION_URL,getInstallationMdUrl());
        clientModel.setAttribute(TEMPLATE_ID,getId());

        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(jamfProAcsUrl);
        clientModel.setRedirectUris(redirectUris);

        SamlClient client = new SamlClient(clientModel);
        client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        client.setNameIDFormat(USERNAME);
        client.setIncludeAuthnStatement(true);
        client.setForceNameIDFormat(false);
        client.setRequiresRealmSignature(true);
        client.setForcePostBinding(false);
        client.setRequiresClientSignature(false);
        client.setRequiresAssertionSignature(true);

        if (client.requiresClientSignature() && client.getClientSigningCertificate() == null) {
            CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(clientModel.getClientId());
            client.setClientSigningCertificate(info.getCertificate());
            client.setClientSigningPrivateKey(info.getPrivateKey());

        }
        clientModel.setFrontchannelLogout(false);
        Optional<ClientScopeModel> clientScopeModel = session.getContext().getRealm().getClientScopesStream().filter(x->x.getName().equals("role_list")).findFirst();
        clientScopeModel.ifPresent(clientModel::removeClientScope);
        clientModel.addProtocolMapper(SAMLAudienceProtocolMapper.createClaimMapper(JAMFPRO_AUDIENCE,entityId));

        RealmModel realmModel =  session.getContext().getRealm();

        String roleName = generateRole(clientId,JAMFPRO);
        if(realmModel.getRole(roleName) == null)
        {
            realmModel.addRole(roleName);
        }
        session.onBoarding().onApplicationOnboarding(clientModel,realmModel);
        return new InstallResponse(true, null, REALM_PATH+session.getContext().getRealm().getName()+SSO_PATH);
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        String entityId = config.get(EDIT_ENTITY_ID);
        String jamfProAcsUrl = config.get(EDIT_JAMFPRO_ACS_URL);
        if(StringUtils.isNotBlank(jamfProAcsUrl)) {
            clientModel.setAttribute(EDIT_JAMFPRO_ACS_URL, jamfProAcsUrl);
        }
        if(StringUtils.isNotBlank(entityId))
        {
            clientModel.setAttribute(AUD,entityId);
            rep.getAttributes().put(AUD,entityId);
            clientModel.setAttribute(EDIT_ENTITY_ID,entityId);
            if(rep.getProtocolMappers()!=null){
                rep.getProtocolMappers().stream().filter(x->
                        x.getName().equals(JAMFPRO_AUDIENCE)).forEach(x->
                        x.getConfig().put(INCLUDED_CUSTOM_AUDIENCE,entityId));
            }
        }
        return null;
    }

    @Override
    public boolean isImplemented(){
        return true;
    }

    @Override
    public String getLogo() {
        return generateLogoUrl(SSO_JAMFPRO_TEMP);
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.JAMFPRO;
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
        return MessageBundler.get(locale, TemplateConstants.SSO_JAMFPRO_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(SSO_JAMFPRO_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(SSO_JAMFPRO_TEMP,JAMFPRO);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{JAMFPRO,"Apple Device Management","SSO","Mobile Device Management","Endpoint Management"};
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
        configProperty.setHelpText("jamfpro-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ENTITY_ID);
        configProperty.setLabel("jamfpro-entityID");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("jamfpro-entityID-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(JAMFPRO_ACS_URL);
        configProperty.setLabel("acs-url");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("jamfpro-acs-url-helptext");
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

        String jamfproAcsUrl = config.get(JAMFPRO_ACS_URL);
        if(Boolean.TRUE.equals(!GeneralHelper.isValidUrl(jamfproAcsUrl))){
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
        return "114a171f-7006-4b8f-870c-375ff19b1f80";
    }

    @Override
    public InstallResponse onCleanUp(RealmModel realmModel , ClientModel clientModel) {
        removeRolesHelper(realmModel,clientModel);
        return new InstallResponse(true, null, null);
    }
}
