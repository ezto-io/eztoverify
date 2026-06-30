package org.keycloak.protocol.oidc.grants.ciba.util;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import io.minio.http.Method;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.actiontoken.ActionTokenConstants;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.DefaultClientRegistrationPolicies;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.events.EventType.EZ_TRANSACTION_REMOVE_USER;
import static org.keycloak.protocol.oidc.grants.ciba.resolvers.DefaultCIBALoginUserResolver.SP_WORKFLOW;

public class GeneralHelper {
    private static final Logger logger = Logger.getLogger(GeneralHelper.class);
    private static GeneralHelper generalHelper;
    private static boolean isTestMode = false;

    private static final String REASON = "reason";

    private static final String ONBOARDING_FAILED = "Onboarding Failed";
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_URL);
    private static final String MINIO_SERVER_API_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_API_URL);
    private static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_ACCESS_KEY);
    private static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_SECRET_KEY);

    public static GeneralHelper getInstance() {
        if (generalHelper == null) {
            generalHelper = new GeneralHelper();
        }
        return generalHelper;
    }
    public static SmsConfigModel getDefaultWhatsappSender(KeycloakSession session) {
        Optional<SmsConfigModel> optSmsConfig = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
        if (optSmsConfig.isPresent())
            return optSmsConfig.get();
        return null;
    }

    public static SmsConfigModel getDefaultSmsSender(KeycloakSession session) {
        Optional<SmsConfigModel> optSmsConfig = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && !x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
        if (optSmsConfig.isPresent())
            return optSmsConfig.get();
        return null;
    }

    public static void updateResult(KeycloakSession session, RealmModel realm, ClientModel clientModel, UserModel user,String trxId){
        if(user == null){
            logger.info("userModel is null :: updateResult()");
            return;
        }
        Optional<ComponentModel> optionalComponentModel = realm.getComponentsStream(realm.getId()).filter(x -> x.getProviderId().equalsIgnoreCase("result-policy-checker")).findFirst();
        DefaultClientRegistrationPolicies.addResultVerificationPolicy(realm, optionalComponentModel.isPresent());
        ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "result-policy-checker", new HashMap<String, Object>() {{
            put("userid", user.getId());
            put("clientid", clientModel.getClientId());
            put("requestId",trxId);
            put("requiredActions", realm.getRequiredActionProvidersStream(clientModel.getAuthenticationFlowBindingOverrides().get("action")).collect(Collectors.toList()));
        }});
    }

    public static EventBuilder getEvent(KeycloakSession session, UserModel user, EventType eventType)
    {
        EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
        eventBuilder.client(session.getContext().getAuthenticationSession() != null ?
                session.getContext().getAuthenticationSession().getClient().getClientId() :
                session.getContext().getClient().getClientId() != null ? session.getContext().getClient().getClientId()
                        :"No Data Available");
        if (user != null) {
            eventBuilder.user(user);
        } else {
            eventBuilder.user("UNKNOWN_USER");
        }
        eventBuilder.event(eventType);

        return eventBuilder;
    }

    public static EventBuilder getEvent(RequiredActionContext context, EventType eventType) {
        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.detail("requestId", context.getAuthenticationSession().getAuthNote("request"));
        eventBuilder.client(context.getAuthenticationSession().getClient().getClientId());
        if (context.getUser() != null) {
            eventBuilder.user(context.getUser());
        } else {
            eventBuilder.user("UNKNOWN_USER");
        }
        eventBuilder.event(eventType);
        return eventBuilder;
    }

    public static void auditEvent(KeycloakSession session, EventType eventType, String error) {
        UserModel user = session.getContext().getAuthenticationSession() != null ? session.getContext().getAuthenticationSession().getAuthenticatedUser() : null;
        EventBuilder eventBuilder = getEvent(session, user ,eventType);
        if (StringUtil.isBlank(error)) {
            eventBuilder.success();
        } else {
            eventBuilder.error(error);
        }

        return;
    }

    public static Map<String, Object> getTTLMessage(String ttl) {
        boolean validate = validateTTL(ttl);
        Map<String,Object> timeDuration = new HashMap<>();
        if (!validate)
            ttl = "1h";
        String timeUnit = "h";
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            timeUnit = ttl.substring(ttl.length() - 1);
            ttl = ttl.substring(0, ttl.length() - 1);
        }
        long duration = Long.parseLong(ttl);

        if (timeUnit.equals("d")) {
            if (duration == 1){
                timeDuration.put("time",1);
                timeDuration.put("unit", "day");
            }
            else{
                timeDuration.put("time",duration);
                timeDuration.put("unit", "days");
            }
            return timeDuration;
        } else if (timeUnit.equals("h")) {
            if (duration == 1) {
                timeDuration.put("time",1);
                timeDuration.put("unit", "hour");
            }
            else{
                timeDuration.put("time",duration);
                timeDuration.put("unit", "hours");
            }
            return timeDuration;
        } else if (timeUnit.equals("m")) {
            if (duration == 1){
                timeDuration.put("time",1);
                timeDuration.put("unit", "minute");
            }
            else{
                timeDuration.put("time",duration);
                timeDuration.put("unit", "minutes");
            }
            return timeDuration;
        } else if (timeUnit.equals("s")) {
            if (duration == 1){
                timeDuration.put("time",1);
                timeDuration.put("unit", "second");
            }
            else{
                timeDuration.put("time",duration);
                timeDuration.put("unit", "seconds");
            }
            return timeDuration;
        } else {
            if (duration == 1){
                timeDuration.put("time",1);
                timeDuration.put("unit", "hour");
            }
            else{
                timeDuration.put("time",duration);
                timeDuration.put("unit", "hours");
            }
            return timeDuration;
        }
    }

    private static boolean validateTTL(String ttl) {
        if (ttl == null || ttl.length() == 0) {
            return false;
        }
        try {
            ttl = ttl.substring(0, ttl.length() - 1);
            Long.parseLong(ttl);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    public static void removeUser(KeycloakSession session, RealmModel realmModel, UserModel userModel, String requestId, EventBuilder event) {
        String actionFlow = "";
        if(realmModel.getTrx(requestId) == null){
            actionFlow = session.getAttribute(SP_WORKFLOW) == null ? "verification" : String.valueOf(session.getAttribute(SP_WORKFLOW));
        } else{
            actionFlow = realmModel.getTrxValue(requestId, SP_WORKFLOW);
        }
        boolean isVerificationFlow = actionFlow == null || (actionFlow.equalsIgnoreCase("verification"));
        long transactionCount = session.getContext().getRealm().getUserTransactionStatus(userModel.getUsername()).size();

        if(!isVerificationFlow
                && (transactionCount <= 1)){
            if (Config.getAdminRealm().equals(realmModel.getName())) {
                if (!userModel.hasRole(realmModel.getRole("admin"))) {
                    session.users().removeUser(session.getContext().getRealm(), userModel);
                    event.event(EZ_TRANSACTION_REMOVE_USER).detail(REASON, ONBOARDING_FAILED).success();
                }
            }  else {
                if ( !userModel.hasRole(realmModel.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT)
                        .getRole(ActionTokenConstants.ADMIN_USER_ROLE))) {
                    session.users().removeUser(session.getContext().getRealm(), userModel);
                    event.event(EZ_TRANSACTION_REMOVE_USER).detail(REASON, ONBOARDING_FAILED).success();
                }
            }
        }
    }

    public static String getPresignedObjectUrl (String bucketName, String objectName) {
        MinioClient minioClient = MinioClient.builder().endpoint(MINIO_SERVER_API_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();
        try {
            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            logger.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static Response failureFtl(RealmModel realm, CibaStateModel cibaStateModel, CibaAuthProvider cibaAuthProvider, LoginFormsProvider provider, ClientModel clientModel, boolean isVerificationFlow, String cibaState, String cibaResponse) {
        if (realm.hasTrxValueAttribute(cibaStateModel.getRequestId(), "ik@custom_message")) {
            Map<String, String> config = new HashMap<>();
            try {
                config = JsonSerialization.readValue(realm.getTrxValue(cibaStateModel.getRequestId(), "ik@custom_message"), Map.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return provider
                    .setAttribute("logo", GeneralHelper.getPresignedObjectUrl(realm.getName(), config.get("custom-message-logo")))
                    .setAttribute("cibaTitle", config.get("custom-message-title"))
                    .setAttribute("cibaResponse", config.get("custom-message-desc"))
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("custom-message.ftl");
        }

        boolean isCustomVerificationMsgEnabled = isVerificationFlow && Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomVerificationMsg"));
        boolean isCustomOnboardingMsgEnabled = !isVerificationFlow && Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomOnboardingMsg"));
        String title = isVerificationFlow ? clientModel.getAttributes().get("customFailureVerificationMsgTitle") : clientModel.getAttributes().get("customFailureOnboardingMsgTitle");
        String description = isVerificationFlow ? clientModel.getAttributes().get("customFailureVerificationMsgDesc") : clientModel.getAttributes().get("customFailureOnboardingMsgDesc");

        if(title == null) {
            title = isVerificationFlow ? "verification_failed" : "onboarding_failed";
        }
        if(description == null) {
            description = cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop()
                    ? "no-action-required"
                    : "close-window-safely";
        }

        if (isCustomOnboardingMsgEnabled) {
            return provider
                    .setAttribute("cibaState", isVerificationFlow ? "verification_failed" : "onboarding_failed")
                    .setAttribute("cibaTitle", title)
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", description)
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("custom-message.ftl");
        }

        if (isCustomVerificationMsgEnabled) {
            return provider
                    .setAttribute("cibaState", isVerificationFlow ? "verification_failed" : "onboarding_failed")
                    .setAttribute("cibaTitle", title)
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", description)
                    .setAttribute("showLanguageSelector", "false")
                    .createForm("custom-message.ftl");
        }

        else{
            boolean showStatusCard = isVerificationFlow ? Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomVerificationMsg")) : Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomOnboardingMsg"));
            String ftl = (cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop()) && !showStatusCard ? "ciba-empty-fallback.ftl" : "ciba-redirect.ftl";
            return provider
                    .setAttribute("cibaState", cibaState)
                    .setAttribute("isMobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop())
                    .setAttribute("cibaResponse", cibaResponse)
                    .setAttribute("showStatusCard", isVerificationFlow ? Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomVerificationMsg")) : Boolean.parseBoolean(clientModel.getAttributes().get("enableCustomOnboardingMsg")))
                    .setAttribute("showLanguageSelector", "false")
                    .createForm(ftl);
        }

    }
}