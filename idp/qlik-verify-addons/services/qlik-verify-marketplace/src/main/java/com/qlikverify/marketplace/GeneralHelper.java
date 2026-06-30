package com.qlikverify.marketplace;

import org.apache.http.util.TextUtils;
import org.keycloak.TokenVerifier;
import org.keycloak.models.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.HttpHeaders;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.marketplace.TemplateConstants.EDIT_SUPPORTED_APPS;
import static com.qlikverify.marketplace.TemplateConstants.SUPPORTED_APPS;
import static org.keycloak.Config.getAdminRealm;

public class GeneralHelper {
    private static String whatsapp = "whatsapp";
    private GeneralHelper(){

    }

    public static String getConfigValue(Map<String, String> configModel, String configKey, String defaultValue) {
        if (configModel == null) {
            return "";
        }
        String data = configModel.get(configKey);
        if (data == null || data.isEmpty()) {
            if (defaultValue == null) {
                return "";
            }
            return defaultValue;
        } else {
            return data;
        }
    }

    public static String getTenantName(RealmModel realm){
        if(realm != null){
            if(!TextUtils.isBlank(realm.getDisplayName())){
                return realm.getDisplayName();
            } else if(!TextUtils.isBlank(realm.getName())){
                return realm.getName();
            }
        }
        return getAdminRealm();
    }

    public static Boolean isProviderConfigured(RealmModel realm, String providerType){
        if(providerType.equals("sms"))
            return realm.getSmsConfigsStream()
                    .filter(x-> x.getSmsProviderId() !=null && !x.getSmsProviderId().equalsIgnoreCase(whatsapp))
                    .collect(Collectors.toList())
                    .isEmpty();
        if(providerType.equals(whatsapp))
            return realm.getSmsConfigsStream()
                .anyMatch(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase(whatsapp));
        return false;
    }

    public static Boolean isValidUrl(String url){
        return !(!url.startsWith("http") && !url.startsWith("https"));
    }

    public static String createRealmRole(ClientModel client, String subApp) {
        return  "sso-"+ client.getName().replace(" ","_").toLowerCase()+ "-" + subApp.replace(" ", "_")+ "-role";
    }

    public static String createRealmRole(String clientNameFromRep, String subApp) {
        return  "sso-"+ clientNameFromRep.replace(" ","_").toLowerCase()+ "-" + subApp.replace(" ", "_")+ "-role";
    }

    public static List<String> getNewSubApps(String  subapp){
        return new LinkedList<>(Arrays.asList(subapp.split(",")));
    }

    public static List<String> getOldSubApps(ClientModel clientModel){
        return new LinkedList<>(Arrays.asList(clientModel.getAttribute(EDIT_SUPPORTED_APPS).split(",")));
    }

    public static void updateRolesBasedOnSubApps(KeycloakSession session, ClientModel clientModel, ClientRepresentation rep, String subapp) {
        if (clientModel.getAttribute(EDIT_SUPPORTED_APPS) != null && !clientModel.getAttribute(EDIT_SUPPORTED_APPS).isEmpty()){

            List<String> newSubApps = getNewSubApps(subapp);
            List<String> oldSubApps = getOldSubApps(clientModel);

            newSubApps.removeAll(oldSubApps);
            if (newSubApps.stream().count() > 0){
                // Add new roles to realm.
                newSubApps.forEach(newSubApp -> {
                    String newRole = GeneralHelper.createRealmRole(rep.getName(), newSubApp);
                    session.getContext().getRealm().addRole(newRole);
                });
            }
            newSubApps = getNewSubApps(subapp);
            oldSubApps = getOldSubApps(clientModel);
            oldSubApps.removeAll(newSubApps);
            if (oldSubApps.stream().count() > 0){
                // Some sub apps were removed. Remove those roles from realm and assigned users.
                oldSubApps.forEach(removeSubApp -> {
                    String realmRole = GeneralHelper.createRealmRole(clientModel, removeSubApp);
                    RoleModel oldAppRole = session.roles().getRealmRole(session.getContext().getRealm(), realmRole);
                    if(oldAppRole != null){
                        session.roles().removeRole(oldAppRole);
                    }

                });
            }

        }
    }

    private static boolean isTokenValid(List<String> tokens) {
        return !tokens.isEmpty() && tokens.get(0) != null && tokens.get(0).split(" ").length >= 2;
    }

    public static List<String> getListOfSSOAppsTobeInstalled(KeycloakSession session, ClientModel clientModel){
        AuthenticationManager authenticationManager = new AuthenticationManager();
        List<String> tokens = session.getContext().getRequestHeaders().getRequestHeader(HttpHeaders.AUTHORIZATION);
        AuthenticationManager.AuthResult authResult = null;
        if (!tokens.isEmpty() && isTokenValid(tokens)) {
            authResult =  AppAuthManager.verifyIdentityToken(session, session.getContext().getRealm(),
                    session.getContext().getUri(),
                    session.getContext().getConnection(), true, false, null, false,
                    tokens.get(0).split(" ")[1],
                    session.getContext().getRequestHeaders(),
                    new TokenVerifier.TokenTypeCheck(TokenUtil.TOKEN_TYPE_BEARER));

        } else {
            authResult = authenticationManager.authenticateIdentityCookie(session, session.getContext().getRealm());
        }

        if(authResult == null) return new ArrayList<>();
        UserModel user = authResult.getUser();
        // assigned roles
        List<String> mappedRoles = user.getRoleMappingsStream().map(x -> x.getName()).collect(Collectors.toList());
        // assigned group roles
        user.getGroupsStream().forEach(groupModel -> {
            List<String> groupRoles = groupModel.getRealmRoleMappingsStream().map(RoleModel :: getName).collect(Collectors.toList());
            mappedRoles.addAll(groupRoles);
        });

        List<String>toInstall = Arrays.asList(clientModel.getAttribute(SUPPORTED_APPS).split(","));
        if(clientModel.getAttribute(EDIT_SUPPORTED_APPS) != null && !clientModel.getAttribute(EDIT_SUPPORTED_APPS).isEmpty()){
            toInstall = Arrays.asList(clientModel.getAttribute(EDIT_SUPPORTED_APPS).split(","));
        }

        List<String> finalToInstall = new ArrayList<>();
        toInstall.forEach(zohoSubApp -> {
            String expectedRole = GeneralHelper.createRealmRole(clientModel, zohoSubApp);
            for(String mappedRole : mappedRoles) {
                if (mappedRole.equalsIgnoreCase(expectedRole) && !finalToInstall.contains(zohoSubApp)) {
                        finalToInstall.add(zohoSubApp);
                }
            }
        });
        return finalToInstall;
    }

}
