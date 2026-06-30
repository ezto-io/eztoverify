package com.qlikverify.marketplace.templates.sso.base;

import com.qlikverify.marketplace.GeneralHelper;
import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.MARKETPLACE_PATH;

public abstract class SSOMarketplaceTemplate {

    protected static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    protected List<String> validateAlias(RealmModel realmModel, Map<String, String> rep) {
        List<String> configTestResponses = new ArrayList<>();
        String alias = GeneralHelper.getConfigValue(rep, "name", "");
        String clientId = getSSOClientId(alias);
        // Existing Alias  Check
        if (realmModel.getClientByClientId(clientId) != null) {
            configTestResponses.add("SSO app with Name " + alias + " is already exists. Try with different name");
        }
        return configTestResponses;
    }

    protected String getSSOClientId(String alias) {
        return alias.toLowerCase(Locale.ROOT).trim().replace(" ","_");
    }

    protected List<String> validateSSOAlias(RealmModel realmModel, Map<String, String> rep) {
        List<String> configTestResponses = new ArrayList<>();
        String alias = GeneralHelper.getConfigValue(rep, "name", "");
        boolean isAliasAlreadyPresent  = realmModel.getClientsStream()
                .map(ClientModel::getName)
                .map(String::toLowerCase)
                .anyMatch(name -> name.equalsIgnoreCase(alias.toLowerCase())); // Compare in a case-insensitive manner
        if (realmModel.getClientByClientId(getSSOClientId(alias)) != null || isAliasAlreadyPresent) {
            configTestResponses.add("Template with Name " + alias + " is already exists. Try with different name");
        }
        return configTestResponses;
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
                if ( (dependsOn == null || dependsOn) && (!(settings.containsKey(x.getName())) || settings.get(x.getName()).isEmpty()) ){
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

    protected void removeRolesHelper(RealmModel realm , ClientModel client){
        String roleName = "sso-" + client.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-" + client.getAttribute("sso_type") + "-role";
        RoleModel roleModel = realm.getRole(roleName);
        if (roleModel != null) {
            realm.removeRole(roleModel);
        }
    }

    protected String generateRole(String clientId,String ssoType){
        return  "sso-"+clientId.toLowerCase()+"-"+ssoType+"-role";
    }

    protected String generateOverviewMdUrl(String templateName){
        return DEFAULT_SERVER_URL + MARKETPLACE_PATH + templateName + "/overview.md";
    }
    protected String generateInstallationMdUrl(String templateName, String ssoType){
        return DEFAULT_SERVER_URL + MARKETPLACE_PATH + templateName + "/SSO-Installation-with-"+ssoType+".md";
    }
    protected String generateLogoUrl(String templateName){
        return DEFAULT_SERVER_URL + MARKETPLACE_PATH + templateName + "/logo.svg";
    }
}
