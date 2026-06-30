package com.qlikverify.helpers;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.keycloak.Config.getAdminRealm;

public class GeneralHelper {
    private static final Logger logger = Logger.getLogger(GeneralHelper.class);
    private static final String LOG_TAG = "EZTO_PRIVATE_HELPER:";
    private static GeneralHelper generalHelper;
    private static boolean isTestMode = false;

    public static GeneralHelper getInstance() {
        if (generalHelper == null) {
            generalHelper = new GeneralHelper();
        }
        return generalHelper;
    }

    public static boolean isEmailConfigured(KeycloakSession session, RealmModel realm) {
        if (!realm.getName().equalsIgnoreCase(getAdminRealm())
                && realm.isEnabledOwnEmailProvider()) {
            Optional<String> emailProviders = realm.getEmailSenderStream().findFirst();
            return emailProviders.isEmpty();
        } else {
            Optional<EmailConfigModel> optEmailConfig =
                    session.realms().getRealmByName(getAdminRealm()).getEmailConfigsStream().findFirst();
            return optEmailConfig.isEmpty();
        }
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

    public static List<String> getClaimsForCurrentClient(RealmModel realm, ClientModel clientModel) {
        List<String> claims = new ArrayList<>();
        String actionId = clientModel.getAuthenticationFlowBindingOverrides().get("action");
        Stream<RequiredActionProviderModel> requiredActionProviderModelList = realm.getRequiredActionProvidersStream(actionId);
        requiredActionProviderModelList.forEach(requiredActionProviderModel -> claims.addAll(getVerificationProcessClaimsById(requiredActionProviderModel.getConfig())));
        return claims;
    }

    public static List<String> getVerificationProcessClaimsById(Map<String, String> config) {
        List<String> claims = new ArrayList<>();
        if (config.keySet().stream().noneMatch(x -> x.contains("claims"))) {
            return claims;
        }
        for (Map.Entry<String, String> entry : config.entrySet()) {
            claims.addAll(Arrays.asList(entry.getValue().split("##")));
        }
        return claims;
    }

    public static HashMap<String, String> getValuesFromUserMetadate(List<String> claims, TrxModel user) {
        HashMap<String, String> claimsToBeSent = new HashMap<>();
        for (String claim : claims) {
            if (user.getAttributes().containsKey("sp@" + claim) && !claim.contains("_error_code")) {
                claimsToBeSent.put(claim, user.getAttributes().get("sp@" + claim));
            }
            if (claim.equals("liveness_completed")) {
                claimsToBeSent.put("liveness_completed", user.getAttributes().get("liveness_completed"));
            }
        }
        return claimsToBeSent;
    }

    public static HashMap<String, String> getValuesFromUserMetadata(List<String> claims, TrxModel user) {
        HashMap<String, String> claimsToBeSent = new HashMap<>();
        for (String claim : claims) {
            if ((claim.startsWith("sp@") || (claim.startsWith("spm@"))) && user.getAttributes().containsKey(claim)) {
                claimsToBeSent.put(claim, user.getAttributes().get(claim));
            }
        }
        return claimsToBeSent;
    }

    public static List<String> getRequiredActionProvidersForClient(RealmModel realm, ClientModel clientModel) {
        List<String> claims = new ArrayList<>();
        String actionId = clientModel.getAuthenticationFlowBindingOverrides().get("action");
        Stream<RequiredActionProviderModel> requiredActionProviderModelList = realm.getRequiredActionProvidersStream(actionId);
        requiredActionProviderModelList.forEach(requiredActionProviderModel -> claims.add(requiredActionProviderModel.getName()));
        return claims;
    }

    public static boolean isMaximumHookFailureReached(KeycloakSession session, RealmModel realm, ClientModel client) {
        final String failedAttempts = client.getAttribute("failed_hook_attempts");
        final CibaNotificationConfigModel configModel = getCibaNotificationConfigModel(session, realm, client);
        final String failureThreshold = configModel != null ? configModel.getConfig().getOrDefault("hook-failure-threshold", "5") : "5";
        return !failureThreshold.isEmpty() && failedAttempts != null && !failedAttempts.isEmpty()
                && Integer.parseInt(failedAttempts) >= Integer.parseInt(failureThreshold);
    }

    public static boolean isMaximumDeadLetterHookFailureReached(KeycloakSession session, RealmModel realm, ClientModel client) {
        final String failedAttempts = client.getAttribute("failed_hook_attempts_for_dead_letter");
        final CibaDeadLetterHookConfigModel configModel = getCibaDeadLetterConfigModel(session, realm, client);
        final String failureThreshold = configModel != null ? configModel.getConfig().getOrDefault("hook-failure-threshold", "5") : "5";
        return !failureThreshold.isEmpty() && failedAttempts != null && !failedAttempts.isEmpty()
                && Integer.parseInt(failedAttempts) >= Integer.parseInt(failureThreshold);
    }

    public static CibaNotificationConfigModel getCibaNotificationConfigModel(KeycloakSession session, RealmModel realm,
                                                                             ClientModel client) {
        CibaConfig cibaConfig = realm.getCibaPolicy();
        String deliveryMode = cibaConfig.getBackchannelTokenDeliveryMode(client);
        CibaNotificationConfigModel configModel =
                session.getContext().getRealm().getCibaNotificationConfigByProviderId(deliveryMode,
                        client.getClientId(), realm.getId());
        return configModel;
    }

    public static CibaDeadLetterHookConfigModel getCibaDeadLetterConfigModel(KeycloakSession session, RealmModel realm,
                                                                             ClientModel client) {
        CibaDeadLetterHookConfigModel configModel =
                session.getContext().getRealm().getDeadLetterHookNotificationConfigByClientId(client.getClientId(), realm.getId());
        return configModel;
    }

    public static boolean checkWalletBalance(RealmModel realm) {
        return false;
    }

    public static String handleBrowserCookie(String browser) {

        if (browser.toLowerCase().contains("safari")) {
            return Messages.SAFARI_CROSS_SITE_TRACKING;
        } else if (browser.toLowerCase().contains("chrome")) {
            return Messages.CHROME_CROSS_SITE_TRACKING;
        } else if (browser.toLowerCase().contains("edge")) {
            return Messages.EDGE_CROSS_SITE_TRACKING;
        } else if (browser.toLowerCase().contains("opera")) {
            return Messages.OPERA_CROSS_SITE_TRACKING;
        } else if (browser.toLowerCase().contains("firefox")) {
            return Messages.FIREFOX_CROSS_SITE_TRACKING;
        } else if (browser.toLowerCase().contains("brave")) {
            return Messages.BRAVE_CROSS_SITE_TRACKING;
        } else {
            return Messages.DEFAULT_CROSS_SITE_TRACKING;
        }
    }

    public static Locale getLocale(UserModel userModel) {
        if (userModel != null) {
            String userPreferredLocale = userModel.getFirstAttribute("preferred_locale");
            if (userPreferredLocale != null && !userPreferredLocale.isEmpty()) {
                boolean isSupported = org.keycloak.models.Constants.SUPPORTED_LOCALES.stream().anyMatch(supportedLocale -> supportedLocale.equalsIgnoreCase(userPreferredLocale));
                if (isSupported) {
                    return new Locale.Builder().setLanguage(userPreferredLocale).build();
                }
            }
        }
        return Locale.ENGLISH;
    }

    public static boolean isV0Supported(RealmModel realm) {
        Optional<ClientModel> isV0Supported = realm.getClientsStream().filter(clientModel -> clientModel.getAttribute("hidden_ua_multi_version") != null && Boolean.parseBoolean(clientModel.getAttribute("hidden_ua_multi_version"))).findAny();
        return isV0Supported.isPresent();
    }
}
