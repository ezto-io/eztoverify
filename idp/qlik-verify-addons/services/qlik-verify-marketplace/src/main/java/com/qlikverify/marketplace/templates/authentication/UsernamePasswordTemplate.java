package com.qlikverify.marketplace.templates.authentication;

import com.qlikverify.marketplace.TemplateConstants;
import com.qlikverify.marketplace.templates.authentication.base.AuthenticationMarketplaceTemplate;
import com.qlikverify.marketplace.MessageBundler;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.events.models.ConstraintData;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.locale.LocaleType;
import org.keycloak.marketplace.*;
import org.keycloak.marketplace.template.MarketplaceTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.*;
import java.util.stream.Collectors;

public class UsernamePasswordTemplate extends AuthenticationMarketplaceTemplate implements MarketplaceTemplateProvider, MarketplaceTemplateProviderFactory {

    KeycloakSession session;

    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);
    private static final String MARKETPLACE = "/marketplace/";
    @Override
    public InstallResponse install(KeycloakSession session, Map<String,String> config) throws MarketplaceException {

        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactoriesStream(MarketplaceProvider.class).collect(Collectors.toList());
        if(this.dependentOnboardingTemplateId() != null)
        {
            Optional<ProviderFactory> onboarding = factories.stream().filter(x -> (x.getId().equals(this.dependentOnboardingTemplateId()))).findFirst();
            if(onboarding.isPresent())
            {
                ((MarketplaceProviderFactory)onboarding.get()).create(session).install(config);
            }
        }

        if(this.dependentLoginTemplateId() != null)
        {
            Optional<ProviderFactory> login = factories.stream().filter(x -> (x.getId().equals(this.dependentLoginTemplateId()))).findFirst();
            if(login.isPresent())
            {
                ((MarketplaceProviderFactory)login.get()).create(session).install(config);
            }
        }

        if(this.dependentRecoveryTemplateId() != null)
        {
            Optional<ProviderFactory> recovery = factories.stream().filter(x -> (x.getId().equals(this.dependentRecoveryTemplateId()))).findFirst();
            if(recovery.isPresent())
            {
                ((MarketplaceProviderFactory)recovery.get()).create(session).install(config);
            }
        }
        return new InstallResponse(true,null,"realms/"+session.getContext().getRealm().getName()+"/authentication/installed/onboarding");
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        return null;
    }

    @Override
    public String getLogo() {
        return  DEFAULT_SERVER_URL+ MARKETPLACE + TemplateConstants.USERNAME_PASS + "/logo.svg";
    }

    @Override
    public String getTitle(KeycloakSession session) {
        Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
        return MessageBundler.get(locale,TemplateConstants.USERNAME_PASSWORD_TEMP_TITLE);
    }

    @Override
    public String getAuthor(KeycloakSession session) {
        return session == null ? TemplateConstants.QV_AUTHOR : session.rootBrand().getMasterDisplayName();
    }

    @Override
    public String getCategory() {
        return Constants.AUTHENTICATION_FLOW;
    }

    @Override
    public String getDescription(KeycloakSession session) {
        Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
        return MessageBundler.get(locale,TemplateConstants.USERNAME_PASSWORD_TEMP_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return  DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.USERNAME_PASS +"/overview.md";
    }

    @Override
    public String getInstallationMdUrl() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.USERNAME_PASS +"/Username-with-Password-Authentication.md";
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{ "username","password"};
    }

    public String dependentLoginTemplateId() {
        return "5f7dc2d7-ad94-4f22-b97b-87ddad5cc2fd";
    }

    public String dependentRecoveryTemplateId() {
        return "1390fa8f-d84c-4dca-8132-dc966560effa";
    }

    public String dependentOnboardingTemplateId() {
        return "ef96ebfc-a302-4f27-8108-a54793e12953";
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        List<ConstraintData> constraintData = new ArrayList<>();
        if(session.getContext().getRealm().isEnabledOwnEmailProvider()){
            if(session.getContext().getRealm().getEmailConfigsStream().count() == 0)
                constraintData.add(new ConstraintData(TemplateConstants.EMAIL_ML_TEMP_CONDN,"#/realms/" + session.getContext().getRealm().getName() + "/email/providers"));
        }
        else if(session.realms().getRealmByName(Config.getAdminRealm()).getEmailConfigsStream().count() == 0)
            constraintData.add(new ConstraintData(TemplateConstants.ADMIN_CONFIGURATION_MISSING,null));
        return constraintData.isEmpty() ? null : new ConstraintResponse(false, constraintData);
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
        return "523c27b2-8c7f-49be-bc0d-c7f5fae330c6";
    }

    @Override
    public Map<String, Object> getMarketProviders(KeycloakSession session) {
        return super.getMarketplace(session);
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
        configProperty.setName("alias");
        configProperty.setLabel("alias");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("alias-name-hlp-txt");
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {
        return validateAlias(session.getContext().getRealm(),config);
    }

    @Override
    public ThirdPartyProvider get3rdPartyProvider() {
        return null;
    }

}
