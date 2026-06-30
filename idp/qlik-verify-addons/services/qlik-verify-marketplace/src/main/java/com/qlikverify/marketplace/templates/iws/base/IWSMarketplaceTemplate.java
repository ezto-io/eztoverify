package com.qlikverify.marketplace.templates.iws.base;

import com.qlikverify.marketplace.GeneralHelper;
import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.marketplace.InstallResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.vault.DefaultVaultRawSecret;
import org.keycloak.vault.VaultProvider;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.MARKETPLACE_PATH;

public abstract class IWSMarketplaceTemplate {
    protected static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    public static final String USER_SETS_USERNAME_AND_PASSWORD = "iws-user-sets-credentials";

    public static final String USER_SHARE_A_SINGLE_USERNAME_AND_PASSWORD_SET_BY_ADMIN = "iws-user-shares-credentials";

    protected List<String> validateAlias(RealmModel realmModel, Map<String, String> rep) {
        List<String> configTestResponses = new ArrayList<>();
        String alias = GeneralHelper.getConfigValue(rep, "name", "");
        // Existing Alias  Check
        if (realmModel.getClientByClientId(getIWSClientId(alias)) != null) {
            configTestResponses.add("Template with Name " + alias + " is already exists. Try with different name");
        }
        return configTestResponses;
    }

    protected String getIWSClientId(String alias)
    {
        return alias.toLowerCase(Locale.ROOT).trim().replace(" ","_");
    }

    protected List<String> validateConfig(List<ProviderConfigProperty> configList, Map<String, String> settings, boolean skipConfigCheck) {
        List<String> configTestResponses = new ArrayList<>();
        configList.forEach(x -> {

            if(skipConfigCheck && x.isSkippableOnUpdate())
                return;

            if (x.isRequired()) {
                Boolean dependsOn = null;
                if (x.getDependsOn() != null && !x.getDependsOn().isEmpty()) {
                    dependsOn = getDependsOnConfig(x.getDependsOn(), settings);
                }
                if ( (dependsOn == null || dependsOn) && (!(settings.containsKey(x.getName())) || settings.get(x.getName()).isEmpty()) ) {
                        configTestResponses.add(x.getLabel() + " is a required field");
                }
            }
        });

        return configTestResponses;
    }

    protected boolean getDependsOnConfig(Map<String, Object> dependsOnList, Map<String, String> configuredSettings) {
        for (Map.Entry<String, Object> x : dependsOnList.entrySet()) {
            if (String.valueOf(configuredSettings.get(x.getKey())).equalsIgnoreCase(String.valueOf(x.getValue()))) {
                return true;
            }
        }
        return false;
    }

    protected VaultProvider getVaultProvider(KeycloakSession kcSession, ClientModel clientModel) {
        VaultProvider vaultProvider = kcSession.getProvider(VaultProvider.class, "winlogon-vault");
        vaultProvider.setClientModel(clientModel);
        return vaultProvider;

    }

    protected String generateRole(String clientId){
        return  "iws-" + clientId + "-role";
    }

    public ClientModel processIWS(KeycloakSession session, Map<String, String> config, String appLoginUrl,String logourl,String script) throws InstallException {
        String aliasName = GeneralHelper.getConfigValue(config, "name", "");
        String clientId = getIWSClientId(aliasName);
        ClientModel clientModel = KeycloakModelUtils.createClient(session.getContext().getRealm(), aliasName);
        clientModel.setName(aliasName);
        clientModel.setProtocol("openid-connect");
        clientModel.setClientId(clientId);

        clientModel.setImplicitFlowEnabled(true);
        clientModel.setStandardFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setBearerOnly(false);
        clientModel.setPublicClient(true);

        Set<String> redirectUris = new HashSet<>();
        redirectUris.add(appLoginUrl);
        clientModel.setRedirectUris(redirectUris);
        clientModel.setAttribute("sso.logourl",logourl);
        clientModel.setAttribute("iws.enable","true");
        clientModel.setAttribute("sso.show", "true");
        clientModel.setAttribute("iws.installed","true");
        clientModel.setAttribute("show.app.login.url","false");
        String credWay = config.get("cred_sharing_way");
        clientModel.setAttribute("vault", "true");
        if(credWay.equals(USER_SETS_USERNAME_AND_PASSWORD))
        {
            clientModel.setAttribute("iws.credtype","iws_user_set_pass");
            clientModel.setAttribute("vault", "true");
        }
        else
        {
            String adminUsername = config.get("admin_username");
            String adminPass = config.get("admin_password");
            clientModel.setAttribute("iws.admin.user",adminUsername);
            clientModel.setAttribute("iws.credtype","iws_admin_set_pass");
            VaultProvider vaultProvider = getVaultProvider(session, clientModel);

            boolean isCreated = vaultProvider.writeUserSecret(adminUsername, DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(String.valueOf(adminPass).getBytes(StandardCharsets.UTF_8)))),false);
            if (!isCreated) {
                throw new InstallException(new InstallResponse(false, "Cannot update credentials for the IWS app", null));
            }
            try {
                clientModel.setAttribute("iws.admin.pass", org.keycloak.authentication.authenticators.utils.GeneralHelper.maskString(adminPass, 0, adminPass.length(), '*'));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        clientModel.setAttribute("iws.script",script);
        clientModel.addProtocolMapper(HardcodedClaim.create("App Name Mapper","app_name",aliasName, ProviderConfigProperty.STRING_TYPE,true,false));

        RealmModel realmModel = session.getContext().getRealm();
        String roleName = generateRole(clientId);

        if (realmModel.getRole(roleName) == null) {
            realmModel.addRole(roleName);
        }

        session.onBoarding().onApplicationOnboarding(clientModel, realmModel);
        return clientModel;
    }

    protected String generateOverviewMdUrl(String templateName){
        return DEFAULT_SERVER_URL + MARKETPLACE_PATH + templateName + "/overview.md";
    }
    protected String generateInstallationMdUrl(String templateName, String iwsType){
        return DEFAULT_SERVER_URL + MARKETPLACE_PATH + templateName + "/IWS-Installation-with-" + iwsType + ".md";
    }
}
