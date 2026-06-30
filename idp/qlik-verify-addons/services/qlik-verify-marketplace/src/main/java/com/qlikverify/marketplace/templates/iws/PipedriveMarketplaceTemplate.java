package com.qlikverify.marketplace.templates.iws;

import com.qlikverify.marketplace.MessageBundler;
import com.qlikverify.marketplace.TemplateConstants;
import com.qlikverify.marketplace.templates.iws.base.IWSMarketplaceTemplate;
import com.qlikverify.marketplace.templates.iws.base.InstallException;
import org.keycloak.Config;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.locale.LocaleType;
import org.keycloak.marketplace.InstallResponse;
import org.keycloak.marketplace.MarketplaceException;
import org.keycloak.marketplace.ThirdPartyProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.*;

public class PipedriveMarketplaceTemplate extends IWSMarketplaceTemplate implements MarketplaceTemplateProvider, MarketplaceTemplateProviderFactory {


    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("name");
        configProperty.setLabel("iws-install-name");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setSkippableOnUpdate(true);
        configProperty.setHelpText("iws-name-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(CRED_SHARING_WAY);
        configProperty.setLabel("iws-who-sets-the-credentials");
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setRequired(true);
        List<String> options = new ArrayList<>();
        options.add(USER_SETS_USERNAME_AND_PASSWORD);
        options.add(USER_SHARE_A_SINGLE_USERNAME_AND_PASSWORD_SET_BY_ADMIN);
        configProperty.setOptions(options);
        configProperty.setDefaultValue(USER_SHARE_A_SINGLE_USERNAME_AND_PASSWORD_SET_BY_ADMIN);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("admin_username");
        configProperty.setLabel("iws-admin-username");
        configProperty.setRequired(true);
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        Map<String, Object> dependsOn = new HashMap<>();
        dependsOn.put(CRED_SHARING_WAY,new String[]{USER_SHARE_A_SINGLE_USERNAME_AND_PASSWORD_SET_BY_ADMIN});
        configProperty.setDependsOn(dependsOn);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("admin_password");
        configProperty.setLabel("iws-admin-password");
        configProperty.setRequired(true);
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        Map<String, Object> credSharingWayDependsOn = new HashMap<>();
        credSharingWayDependsOn.put(CRED_SHARING_WAY,new String[]{USER_SHARE_A_SINGLE_USERNAME_AND_PASSWORD_SET_BY_ADMIN});
        configProperty.setDependsOn(credSharingWayDependsOn);
        configProperties.add(configProperty);

    }

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {
        ClientModel clientModel =null;
        try {
            clientModel = processIWS(session, config, "https://app.pipedrive.com/auth/login", getLogo(), "[\n" +
                    "    {\n" +
                    "        \"value\": 1,\n" +
                    "        \"type\": \"sleep\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"element\": \"input[type=email]\",\n" +
                    "        \"type\": \"input\",\n" +
                    "        \"mapper\": \"username\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"value\": 1,\n" +
                    "        \"type\": \"sleep\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"element\": \"input[type=Password]\",\n" +
                    "        \"type\": \"input\",\n" +
                    "        \"mapper\": \"password\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"value\": 1,\n" +
                    "        \"type\": \"sleep\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"element\": \"button[class='puco-button puco-button--submit puco-button--width-auto submit-button bt']\",\n" +
                    "        \"type\": \"button\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"timeout\": 10,\n" +
                    "        \"type\": \"timeout\"\n" +
                    "    }\n" +
                    "]");
        } catch (InstallException e) {
            return e.getInstallResponse();
        }
        return new InstallResponse(true, null, REALM_PATH + session.getContext().getRealm().getName() + CLIENTS_PATH+"/"+clientModel.getId());
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        return null;
    }

    @Override
    public String getLogo() {
        return DEFAULT_SERVER_URL + MARKETPLACE_PATH + IWS_PIPEDRIVE_TEMP + "/logo.png";
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return IWS_PIPEDRIVE;
    }

    @Override
    public String getAuthor(KeycloakSession session) {
        return session == null ? TemplateConstants.QV_AUTHOR : session.rootBrand().getMasterDisplayName();
    }

    @Override
    public String getCategory() {
        return Constants.IWS_MARKETPLACE;
    }

    @Override
    public String getDescription(KeycloakSession session) {
        Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
        return MessageBundler.get(locale,TemplateConstants.IWS_PIPEDRIVE_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return generateOverviewMdUrl(IWS_PIPEDRIVE_TEMP);
    }

    @Override
    public String getInstallationMdUrl() {
        return generateInstallationMdUrl(IWS_PIPEDRIVE_TEMP, IWS_PIPEDRIVE);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{"pipedrive", "intelligent web secure", "login"};
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

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {
        List<String> response = validateConfig(configProperties, config, isUpdate);
        if (!response.isEmpty())
            return response;

        return validateAlias(session.getContext().getRealm(), config);
    }

    @Override
    public ThirdPartyProvider get3rdPartyProvider() {
        return null;
    }

    @Override
    public MarketplaceTemplateProvider create(KeycloakSession session) {
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
        return "a59a6983-a106-4b7d-8d66-50f9aa841ab9";
    }
}
