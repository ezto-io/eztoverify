package com.qlikverify.services.theme.helpers;

import static com.qlikverify.services.theme.onboarding.Constants.*;
import com.qlikverify.services.theme.onboarding.Constants;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.Urls;

import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

public class GeneralHelpers {

    private GeneralHelpers() {
        // This constructor is private to prevent instantiation of the class
    }

    private static final Logger logger = Logger.getLogger(GeneralHelpers.class);

    public static Map<String, String> convertMultiToRegularMap(MultivaluedMap<String, String> m) {
        Map<String, String> map = new HashMap<>();
        if (m == null) {
            return map;
        }
        for (Map.Entry<String, List<String>> entry : m.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (String s : entry.getValue()) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(s);
            }
            map.put(entry.getKey(), sb.toString());
        }
        return map;
    }

    public static Map<String,String> getMatchedResult(List<ProviderConfigProperty> configProperties, Map<String,String> receivedParams)
    {
        Map<String,String> result = new HashMap<>();
        configProperties.forEach(x-> {
            if(receivedParams.containsKey(x.getName()))
            {
                result.put(x.getName(),receivedParams.get(x.getName()));
            }
        });
        return result;
    }

    public static String generateConsoleUrl(String tenantName) {
        String domainName = EnvironmentReader.getValue("ROOT_DNS_NAME","qlikverify.com");
        String env = EnvironmentReader.getValue("ENV");
        if (env == null || env.isEmpty())
            return "http://localhost:8080/auth/admin/" + tenantName + "/console/";

        if (tenantName != null && tenantName.equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            tenantName = "dashboard";
        }

        if (env.equalsIgnoreCase("prod")) {
            return HTTPS + tenantName + "." + domainName + "/auth/admin/" + tenantName + "/console";
        }
        return HTTPS + tenantName + "." + env  + "." +domainName + "/auth/admin/" + tenantName + "/console";
    }

    public static  String getSSOUrl(KeycloakSession session, String tenantName){
        return  Urls.ssoPage(session.getContext().getUri().getBaseUri(), tenantName).toString();
    }

    public static String generateWorkspaceUrl(String tenantName) {
        String domainName = EnvironmentReader.getValue("ROOT_DNS_NAME","qlikverify.com");
        String env = EnvironmentReader.getValue("ENV");
        if (env == null || env.isEmpty())
            return "http://localhost:8080/auth/admin/" + tenantName + "/console/";

        if (tenantName != null && tenantName.equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            tenantName = "dashboard";
        }
        if (env.equalsIgnoreCase("prod")) {
            return HTTPS + tenantName + "." + domainName;
        }
        return HTTPS + tenantName + "." + env  + "." +domainName;
    }

    public static boolean sendVerificationEmail(KeycloakSession session, RealmModel masterRealm, String link, String otp, RealmModel createdRealm, String adminEmailId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("compName", getRealmName(createdRealm));
        attributes.put("to", adminEmailId);
        attributes.put("otp", otp);
        attributes.put("link", link);
        attributes.put("year", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        attributes.put("workspace", link);
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        attributes.put("application_link", EnvironmentReader.getValue("AUTH_DOCS_APPLICATION_LINK", "https://docs.ezto.io/auth/docs/wiam/applications/Overview/"));
        attributes.put("authentication_link", EnvironmentReader.getValue("AUTH_DOCS_AUTHENTICATION_LINK", "https://docs.ezto.io/auth/docs/wiam/authentication/Overview/"));
        attributes.put("marketplace_link", EnvironmentReader.getValue("AUTH_DOCS_MARKETPLACE_LINK", "https://docs.ezto.io/auth/docs/wiam/marketplace/Overview/"));

        String subject = "emailVerificationSubject";
        String ftlName = "welcome-email.ftl";
        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(masterRealm)
                    .send(adminEmailId, subject, ftlName, attributes);
        } catch (EmailException e) {
            logger.error(e);
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return false;
        }
        return true;
    }

    private static String getRealmName(RealmModel realm){
        return !TextUtils.isEmpty(realm.getDisplayName()) ? realm.getDisplayName() : realm.getName();
    }


    public static  boolean checkForCurrentStep(RealmModel realmModel,String stepName){
        List<String> steps = getCompletedStepList(realmModel);
        return  steps.contains(stepName);
    }

    public static void addToCompletedStep(RealmModel realmModel, String stepName){
        // If current step is already completed, Remove following steps from the list to start them again.
        List<String> steps = getCompletedStepList(realmModel);
        if (steps.contains(stepName)){
            final String obSubscriptionChange = realmModel.getAttribute(ONBOARDING_SUBSCRIPTION_CHANGE);
            boolean sublist = true;
            if(obSubscriptionChange != null && !obSubscriptionChange.isEmpty()) {
                sublist =
                        Boolean.getBoolean(obSubscriptionChange);
            }
            if(sublist){
                int index = steps.indexOf(stepName);
                if (index != (steps.size() - 1)){
                    steps.subList(index + 1, steps.size()).clear();
                }
            }
        } else {
            steps.add(stepName);
        }
        realmModel.setAttribute(Constants.OB_COMPLETED_STEPS, String.join(",", steps));
    }


    public static List<String> getCompletedStepList(RealmModel realmModel){
        String completedSteps = realmModel.getAttribute(Constants.OB_COMPLETED_STEPS);
        if (completedSteps == null) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(completedSteps.split(",")));
    }

    public static void resetObUpdateBasedOnSubscription(KeycloakSession session){
        if (session.getContext().getRealm().getAttribute(ONBOARDING_SUBSCRIPTION_CHANGE, false).equals(true)){
            session.getContext().getRealm().setAttribute(ONBOARDING_SUBSCRIPTION_CHANGE, false);
        }
    }

    public static void markAllStepsAsCompleted(KeycloakSession session){
        session.onBoarding()
                .welcomeOnboardingSteps(session, null)
                .getSteps()
                .stream()
                .map(x -> x.stepProviderId)
                .filter(stepProviderId -> {
                    List<String> completedStepList = getCompletedStepList(session.getContext().getRealm());
                    return !completedStepList.contains(stepProviderId);
                })
                .forEach(stepProviderId -> addToCompletedStep(session.getContext().getRealm(), stepProviderId));
    }

    public static String getFileName(String fileContent) {
        String[] fileType = fileContent.split("/");
        String[]  extension = fileType[1].split("\\+");

        return extension[0];
    }
}
