package com.qlikverify.services.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.events.models.response.User;
import com.qlikverify.services.models.JsonInputModel.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.qlikverify.services.constants.Constants.*;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.keycloak.Config.getAdminRealm;
import static org.keycloak.locale.LocaleType.LOGIN;
import static org.keycloak.models.Constants.VERIFICATION_EMAIL_CODE;
import static org.keycloak.services.resources.admin.RealmsAdminResource.getEmailTemplate;


public class GeneralHelper {
    private static final Logger logger = Logger.getLogger(GeneralHelper.class);
    private static GeneralHelper generalHelper;
    private static boolean isTestMode = false;

    private static int year = Calendar.getInstance().get(Calendar.YEAR);

    public static GeneralHelper getInstance() {
        if (generalHelper == null) {
            generalHelper = new GeneralHelper();
        }
        return generalHelper;
    }

    public long ipToLongInt (InetAddress ipAddress) {
        long resultIP = 0;
        byte[] ipAddressOctets = ipAddress.getAddress();

        for (byte octet : ipAddressOctets) {
            resultIP <<= 8;
            resultIP |= octet & 0xFF;
        }
        return resultIP;
    }

    public String getRemoteAddress(KeycloakSession session) {
        return session.getContext().getConnection().getRemoteAddr();
    }

    public static String getConfigValue(RequiredActionContext context, String configValue) {
        if (context.getConfig() == null) {
            return "";
        }
        String data = context.getConfig().get(configValue);
        if (data == null || data.isEmpty()) {
            return "";
        } else {
            return data;
        }
    }

    public static String getConfigValue(AuthenticationFlowContext context, String configValue) {
        return getConfigValue(context, configValue, null);
    }

    public static String getConfigValue(MFAConfigModel context, String configValue) {
        return getConfigValue(context, configValue, null);
    }

    public static String getConfigValue(AuthenticationFlowContext context, String configValue, String defaultValue) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config == null) {
            return "";
        }
        String data = config.getConfig().get(configValue);
        if (data == null || data.isEmpty()) {
            if (defaultValue == null) {
                return "";
            }
            return defaultValue;
        } else {
            return data;
        }
    }

    public static String getConfigValue(MFAConfigModel configModel, String configValue, String defaultValue) {
        if (configModel == null) {
            return "";
        }
        String data = configModel.getConfig().get(configValue);
        if (data == null || data.isEmpty()) {
            if (defaultValue == null) {
                return "";
            }
            return defaultValue;
        } else {
            return data;
        }
    }

    public static String getOtp(int len) {

        // Using numeric values
        String numbers = "0123456789";

        // Using random method
        SecureRandom rndm_method = new SecureRandom();

        char[] otp = new char[len];

        for (int i = 0; i < len; i++) {
            // Use of charAt() method : to get character value
            // Use of nextInt() as it is scanning the value as int
            otp[i] = numbers.charAt(rndm_method.nextInt(numbers.length()));
        }
        return String.valueOf(otp);
    }

    public static String getOtpConfigValue(AuthenticationFlowContext context, String configName) {
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null)
            return "6";
        String configValue = configModel.getConfig().get(configName);
        if (configValue == null || configValue.length() == 0)
            return "6";

        return configValue;
    }

    public static String getOtpConfigValue(MFAConfigModel configModel, String configName) {
        if (configModel == null)
            return "6";
        String configValue = configModel.getConfig().get(configName);
        if (configValue == null || configValue.length() == 0)
            return "6";

        return configValue;
    }

    public static String getSmsText(AuthenticationFlowContext context, String configName) {
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null)
            return DEFAULT_MESSAGE_TXT;
        String configValue = configModel.getConfig().get(configName);
        if (configValue == null || configValue.length() == 0)
            return DEFAULT_MESSAGE_TXT;

        return configValue;
    }

    public static String getSmsText(RequiredActionContext context, String configName) {

        if (context.getConfig() == null)
            return DEFAULT_MESSAGE_TXT;
        String configValue = context.getConfig().get(configName);
        if (configValue == null || configValue.length() == 0)
            return DEFAULT_MESSAGE_TXT;

        return configValue;
    }


    public static String getSmsText(MFAConfigModel configModel, String configName) {
        if (configModel == null)
            return DEFAULT_MESSAGE_TXT;
        String configValue = configModel.getConfig().get(configName);
        if (configValue == null || configValue.length() == 0)
            return DEFAULT_MESSAGE_TXT;

        return configValue;
    }

    public static NumberFormat parseMobileNumber(AuthenticationFlowContext context,String region,String number){
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class,"default");
        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(number,region.toUpperCase());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
        return numberFormat;
    }

    public static NumberFormat parseMobileNumber(RequiredActionContext context,String region,String number){
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class,"default");
        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(number,region.toUpperCase());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
        return numberFormat;
    }

    public static boolean sendEmailOTP(MFAConfigModel configModel, RequiredActionContext context, String email, String expiryDuration) {
        String verify_code_size = getOtpConfigValue(configModel, EMAIL_VER_OTP_LENGTH);
        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(configModel, EMAIL_SENDER);
            if (emailProviderId == null || emailProviderId.isEmpty()){
                logger.error("Email Provider is not set for the authenticator");
                return false;
            }
        }

        /*EmailConfigModel emailConfigModel = null;

            emailConfigModel = context.getRealm().getEmailConfigByProviderId(emailProviderId);
        }*/

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, "123456");
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }
        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String code = GeneralHelper.getOtp(verifyCodeSize);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("year", String.valueOf(year));
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        try {
            String htmlBody = getEmailTemplate(context.getSession(), context.getRealm().getName(), removeExtension(VERIFICATION_EMAIL_CODE));
            attributes.put("tenant", context.getRealm().getName());
            if(htmlBody != null && !htmlBody.isEmpty())
                attributes.put("htmlBody", htmlBody);
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setRealm(context.getRealm())
                    .setEmailProviderId(emailProviderId)
                    .send(email, "emailVerificationSubject", "email-verification-with-code.ftl", attributes);
        } catch (EmailException e) {
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return false;
        }

        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, code);
        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVW1234567890";


    public static boolean sendEmailOTP(AuthenticationFlowContext context, String email, String expiryDuration) {

        String verify_code_size;
        if(context.getAuthenticatorConfig() != null && context.getAuthenticatorConfig().getConfig().containsKey(EMAIL_PHONE_OTP_LEN))
            verify_code_size = getOtpConfigValue(context, EMAIL_PHONE_OTP_LEN);
        else
            verify_code_size = getOtpConfigValue(context, EMAIL_VER_OTP_LENGTH);

        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String code = GeneralHelper.getOtp(verifyCodeSize);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        attributes.put("code", code);
        attributes.put("year", String.valueOf(year));
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, "123456");
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }

        String emailProviderId = null;
        if (!context.getRealm().getName().equalsIgnoreCase(getAdminRealm())
                && context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
            if (StringUtil.isBlank(emailProviderId)) {
                Optional<EmailConfigModel> optionalEmailConfigModel = Optional.empty();

                if (!context.getRealm().getMode().equalsIgnoreCase("bo")) {
                    if (context.getAuthenticationSession().getClient() != null &&
                            context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverrides() != null &&
                             (context.getRealm().getAuthenticationFlowById(context.getExecution().getParentFlow().toString()) != null
                                     && context.getRealm().getAuthenticationFlowById(context.getExecution().getParentFlow().toString()).getAlias() != null
                                     && context.getRealm().getAuthenticationFlowById(context.getExecution().getParentFlow().toString()).getAlias().equals("registration")) &&
                            context.getExecution().getAuthenticator() != null &&
                            context.getExecution().getAuthenticator().equals("email-reg-authenticator")) {
                        optionalEmailConfigModel = context.getRealm().getEmailConfigsStream().findFirst();
                    }
                } else {
                    optionalEmailConfigModel = context.getRealm().getEmailConfigsStream().findFirst();
                }

                if (optionalEmailConfigModel.isEmpty()) {
                    logger.error("Email Provider is not set for the authenticator");
                    return false;
                }

                emailProviderId = optionalEmailConfigModel.get().getEmailProviderId();
            }
        }

       /* EmailConfigModel emailConfigModel = null;
        if(context.getRealm().isEnabledOwnEmailProvider()){
            emailConfigModel = context.getRealm().getEmailConfigByProviderId(emailProviderId);
        }*/

        try {
            String htmlBody = getEmailTemplate(context.getSession(), context.getRealm().getName(), removeExtension(VERIFICATION_EMAIL_CODE));
            attributes.put("tenant", getRealmName(context.getRealm()));
            if(htmlBody != null && !htmlBody.isEmpty())
                attributes.put("htmlBody", htmlBody);
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setRealm(context.getRealm())
                    .setEmailProviderId(emailProviderId)
                    .send(email, "emailVerificationSubject", "email-verification-with-code.ftl", attributes);
        } catch (EmailException e) {
            logger.error(e.getMessage());
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return false;
        }

        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, code);
        if(context.getAuthenticatorConfig() != null && context.getAuthenticatorConfig().getConfig().containsKey(EMAIL_PHONE_EXP_TIME))
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        else
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static boolean sendEmailOTP(AuthenticationFlowContext context, String email, String code, String expiryDuration) {

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, "123456");
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("year", String.valueOf(year));
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        String emailProviderId = null;
        if (!context.getRealm().getName().equalsIgnoreCase(getAdminRealm())
                && context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
            if (emailProviderId == null || emailProviderId.isEmpty()) {
                Optional<EmailConfigModel> emailProvider = context.getRealm().getEmailConfigsStream().findFirst();
                if (emailProvider.isEmpty()){
                    logger.error("No email Provider is configured");
                    return false;
                }
                emailProviderId = emailProvider.get().getEmailProviderId();
            }
        }

        try {
            String htmlBody = getEmailTemplate(context.getSession(), context.getRealm().getName(), removeExtension(VERIFICATION_EMAIL_CODE));
            attributes.put("tenant", context.getRealm().getName());
            if(htmlBody != null && !htmlBody.isEmpty())
                attributes.put("htmlBody", htmlBody);
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setRealm(context.getRealm())
                    .setEmailProviderId(emailProviderId)
                    .send(email, "emailVerificationSubject", "email-verification-with-code.ftl", attributes);
        } catch (EmailException e) {
            logger.error(e.getMessage());
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return false;
        }

        return true;
    }

    public static boolean sendEmailOTP(RequiredActionContext context, String email, String expiryDuration) {
        String verify_code_size = getConfigValue(context, EMAIL_VER_OTP_LENGTH) == "" ?
                                    "6" : getConfigValue(context, EMAIL_VER_OTP_LENGTH);
        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String code = GeneralHelper.getOtp(verifyCodeSize);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("year", String.valueOf(year));
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, "123456");
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }

        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
            if (emailProviderId == null || emailProviderId.isEmpty()) {
//                logger.error("Email Provider is not set for the authenticator");
//                return false;

                Optional<EmailConfigModel> emailProvider = context.getRealm().getEmailConfigsStream().findFirst();
                if (emailProvider.isEmpty()){
                    logger.error("No email Provider is configured");
                    return false;
                }
                emailProviderId = emailProvider.get().getEmailProviderId();
            }
        }

       /* EmailConfigModel emailConfigModel = null;
        if(context.getRealm().isEnabledOwnEmailProvider()){
            emailConfigModel = context.getRealm().getEmailConfigByProviderId(emailProviderId);
        }*/

        try {
            String htmlBody = getEmailTemplate(context.getSession(), context.getRealm().getName(), removeExtension(VERIFICATION_EMAIL_CODE));
            attributes.put("tenant", context.getRealm().getName());
            if(htmlBody != null && !htmlBody.isEmpty())
                attributes.put("htmlBody", htmlBody);
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setRealm(context.getRealm())
                    .setEmailProviderId(emailProviderId)
                    .send(email, "emailVerificationSubject", "email-verification-with-code.ftl", attributes);
        } catch (EmailException e) {
            logger.error(e.getMessage());
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return false;
        }

        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, code);
        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static boolean sendResetPasswordEmailOTP(AuthenticationFlowContext context, UserModel user, String expiryDuration) {
        String email = user.getEmail();
        if (email == null || email.length() == 0)
            return false;

        String verify_code_size = getOtpConfigValue(context, EMAIL_VER_OTP_LENGTH);

        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
           if (emailProviderId == null || emailProviderId.isEmpty()){
//                logger.error("Email Provider is not set for the authenticator");
//                return false;

               Optional<EmailConfigModel> emailProvider = context.getRealm().getEmailConfigsStream().findFirst();
               if (emailProvider.isEmpty()){
                   logger.error("No email Provider is configured");
                   return false;
               }
               emailProviderId = emailProvider.get().getEmailProviderId();
            }
        }

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, "123456");
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }

        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String code = GeneralHelper.getOtp(verifyCodeSize);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("year", String.valueOf(year));
        attributes.put("realmName", getRealmName(context.getRealm()));
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));
        attributes.put("user", new ProfileBean(user));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        try {
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setRealm(context.getRealm())
                    .setUser(user)
                    .setEmailProviderId(emailProviderId)
                    .send(email, "resetPasswordOtpSubject", "password-reset-otp.ftl", attributes);
        } catch (EmailException e) {
            logger.error(e.getMessage());
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return false;
        }

        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, code);
        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    private static WebAuthnPolicy getWebAuthnPolicyTwoFactor(RealmRepresentation rep) {
        WebAuthnPolicy webAuthnPolicy = new WebAuthnPolicy();

        String webAuthnPolicyRpEntityName = rep.getWebAuthnPolicyRpEntityName();
        if (webAuthnPolicyRpEntityName == null || webAuthnPolicyRpEntityName.isEmpty())
            webAuthnPolicyRpEntityName = org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME;
        webAuthnPolicy.setRpEntityName(webAuthnPolicyRpEntityName);

        List<String> webAuthnPolicySignatureAlgorithms = rep.getWebAuthnPolicySignatureAlgorithms();
        if (webAuthnPolicySignatureAlgorithms == null || webAuthnPolicySignatureAlgorithms.isEmpty())
            webAuthnPolicySignatureAlgorithms = Arrays.asList(org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS.split(","));
        webAuthnPolicy.setSignatureAlgorithm(webAuthnPolicySignatureAlgorithms);

        String webAuthnPolicyRpId = rep.getWebAuthnPolicyRpId();
        if (webAuthnPolicyRpId == null || webAuthnPolicyRpId.isEmpty())
            webAuthnPolicyRpId = "";
        webAuthnPolicy.setRpId(webAuthnPolicyRpId);

        String webAuthnPolicyAttestationConveyancePreference = rep.getWebAuthnPolicyAttestationConveyancePreference();
        if (webAuthnPolicyAttestationConveyancePreference == null || webAuthnPolicyAttestationConveyancePreference.isEmpty())
            webAuthnPolicyAttestationConveyancePreference = org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);

        String webAuthnPolicyAuthenticatorAttachment = rep.getWebAuthnPolicyAuthenticatorAttachment();
        if (webAuthnPolicyAuthenticatorAttachment == null || webAuthnPolicyAuthenticatorAttachment.isEmpty())
            webAuthnPolicyAuthenticatorAttachment = org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);

        String webAuthnPolicyRequireResidentKey = rep.getWebAuthnPolicyRequireResidentKey();
        if (webAuthnPolicyRequireResidentKey == null || webAuthnPolicyRequireResidentKey.isEmpty())
            webAuthnPolicyRequireResidentKey = org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setRequireResidentKey(webAuthnPolicyRequireResidentKey);

        String webAuthnPolicyUserVerificationRequirement = rep.getWebAuthnPolicyUserVerificationRequirement();
        if (webAuthnPolicyUserVerificationRequirement == null || webAuthnPolicyUserVerificationRequirement.isEmpty())
            webAuthnPolicyUserVerificationRequirement = org.keycloak.models.Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);

        Integer webAuthnPolicyCreateTimeout = rep.getWebAuthnPolicyCreateTimeout();
        if (webAuthnPolicyCreateTimeout != null) webAuthnPolicy.setCreateTimeout(webAuthnPolicyCreateTimeout);
        else webAuthnPolicy.setCreateTimeout(0);

        Boolean webAuthnPolicyAvoidSameAuthenticatorRegister = rep.isWebAuthnPolicyAvoidSameAuthenticatorRegister();
        if (webAuthnPolicyAvoidSameAuthenticatorRegister != null)
            webAuthnPolicy.setAvoidSameAuthenticatorRegister(webAuthnPolicyAvoidSameAuthenticatorRegister);

        List<String> webAuthnPolicyAcceptableAaguids = rep.getWebAuthnPolicyAcceptableAaguids();
        if (webAuthnPolicyAcceptableAaguids != null)
            webAuthnPolicy.setAcceptableAaguids(webAuthnPolicyAcceptableAaguids);

        return webAuthnPolicy;
    }

    private static List<?> convertObjectToList(Object obj) {
        List<?> list = new ArrayList<>();
        if (obj.getClass().isArray()) {
            list = Arrays.asList((Object[]) obj);
        } else if (obj instanceof Collection) {
            list = new ArrayList<>((Collection<?>) obj);
        }
        return list;
    }

    public static List<String> validateConfig(List<ProviderConfigProperty> configList, Map<String, String> settings) {
        List<String> configTestResponses = new ArrayList<>();
        configList.forEach(x -> {
            if (x.isRequired()) {
                Boolean dependsOn = null;
                if (x.getDependsOn() != null && !x.getDependsOn().isEmpty()) {
                    dependsOn = getDependsOnConfig(x.getDependsOn(), settings);
                }
                if ((dependsOn == null || dependsOn) && (!(settings.containsKey(x.getName())) || settings.get(x.getName()).isEmpty())) {
                        configTestResponses.add(x.getLabel() + " is a required field");
                }
            }
            if (settings.containsKey(x.getName()) && org.keycloak.utils.GeneralHelper.validationForHtmlInjection(settings.get(x.getName()))){
                configTestResponses.add("Invalid input in the field " + x.getName());
            }
        });

        return configTestResponses;
    }

    public static List<String> validateMFAConfig(List<ProviderConfigProperty> props, Map<String, String> configToSet){
        List<String> configTestResponses = new ArrayList<>();
        props.forEach(p ->{
            //required field check
            if (p.isRequired()) {
                Boolean dependsOn = null;
                if (p.getDependsOn() != null && !p.getDependsOn().isEmpty()) {
                    dependsOn = getDependsOnConfig(p.getDependsOn(), configToSet);
                }
                if ((dependsOn == null || dependsOn) && (!(configToSet.containsKey(p.getName())) || configToSet.get(p.getName()).isEmpty())) {
                        configTestResponses.add(p.getLabel() + " is a required field");
                }
            }
            //Min Value of field check
            String configVal = configToSet.get(p.getName());
            if(configVal != null && isParsableInt(configVal)
                    && Integer.parseInt(configVal) < p.getMinVal()){
                configTestResponses.add("Minimum Value for " + p.getLabel() + " is " + p.getMinVal());
            }
            //Max Value of field check
            configVal = configToSet.get(p.getName());
            if (configVal != null && isParsableInt(configVal)
                    && Integer.parseInt(configVal) > p.getMaxVal()) {
                configTestResponses.add("Maximum Value for " + p.getLabel() + " is " + p.getMaxVal());
            }
        });
        return configTestResponses;
    }


    private static boolean isParsableInt(String val){
        try {
            Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean getDependsOnConfig(Map<String, Object> dependsOnList, Map<String, String> configuredSettings) {
        for (Map.Entry<String, Object> x : dependsOnList.entrySet()) {
            if (String.valueOf(configuredSettings.get(x.getKey())).equalsIgnoreCase(String.valueOf(x.getValue()))) {
                return true;
            }
        }
        return false;
    }

    public static boolean sendEmailMagicLink(AuthenticationFlowContext context, UserModel userModel, String expiryDuration) {

        //generating randomUUID and storing in AuthSession
        String key = KeycloakModelUtils.generateId();
        context.getAuthenticationSession().setAuthNote("email-key", key);

        //generating link with UUID as query params
        String link = KeycloakUriBuilder.fromUri(context.getRefreshExecutionUrl()).queryParam("key", key).build().toString();

        //sending Email
        String mailSubject = "magicLinkLoginSubject";
        String ftlName = "magic-link-login.ftl";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("link", link);
        attributes.put("year", String.valueOf(year));
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
           if (emailProviderId == null || emailProviderId.isEmpty()){
                logger.error("Email Provider is not set for the authenticator");
                return false;
            }
        }

        try {
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setRealm(context.getRealm())
                    .setUser(userModel)
                    .setEmailProviderId(emailProviderId)
                    .send(mailSubject, ftlName, attributes);
        } catch (EmailException e) {
            logger.error(e.getMessage());
            return false;
        }
        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_MAGIC_LINK_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static boolean sendEmailMagicLink(AuthenticationFlowContext context, String email, String expiryDuration) {

        //generating randomUUID and storing in AuthSession
        String key = KeycloakModelUtils.generateId();
        context.getAuthenticationSession().setAuthNote("email-key", key);

        //generating link with UUID as query params
        String link = KeycloakUriBuilder.fromUri(context.getRefreshExecutionUrl()).queryParam("key", key).build().toString();

        //sending Email
        String mailSubject = "magicLinkRegistrationSubject";
        String ftlName = "magic-link-registration.ftl";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("link", link);
        attributes.put("year", String.valueOf(year));
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
           if (emailProviderId == null || emailProviderId.isEmpty()){
//                logger.error("Email Provider is not set for the authenticator");
//                return false;

               Optional<EmailConfigModel> emailProvider = context.getRealm().getEmailConfigsStream().findFirst();
               if (emailProvider.isEmpty()){
                   logger.error("No email Provider is configured");
                   return false;
               }
               emailProviderId = emailProvider.get().getEmailProviderId();
            }
        }

        try {
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setRealm(context.getRealm())
                    .setEmailProviderId(emailProviderId)
                    .send(email, mailSubject, ftlName, attributes);
        } catch (EmailException e) {
            logger.error(e.getMessage());
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return false;
        }
        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_MAGIC_LINK_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static boolean isValidEmail(String email) {
        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,63}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static boolean isPasswordValid(AuthenticationFlowContext context, UserModel user, String password) {
        return context.getSession().userCredentialManager().isValid(context.getRealm(), user, UserCredentialModel.password(password));
    }

    public static boolean sendSmsOtp(AuthenticationFlowContext context, String expiryDuration, String smsSenderProviderId) {
        String verify_code_size;
        verify_code_size = getOtpConfigValue(context, PHONE_VER_OTP_LENGTH);

        String message = getSmsText(context, PHONE_VER_OTP_TXT);

        if(expiryDuration.isBlank())
            expiryDuration = "5m";

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, "1234");
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }
        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String generatedOTP = getOtp(verifyCodeSize);
        message = message.replace("{code}", generatedOTP);

        //getting smsSenderProvider
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        if (context.getRealm().isEnabledOwnSmsProvider()) {
            smsSenderProvider.setProviderId(smsSenderProviderId);
        } else {
            SmsConfigModel smsConfig = getDefaultSmsSender(context.getSession());
            if (smsConfig != null) {
                smsSenderProvider.setProviderId(smsConfig.getSmsProviderId());
            }
        }

        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(context.getUser().getMobileNumber(), context.getUser().getMobileNumberRegion().toUpperCase());
            smsSenderProvider.sendSMS(numberFormat.getE16Format(), message, null);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            return false;
        }
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, generatedOTP);
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static boolean sendSmsOtp(AuthenticationFlowContext context, String code, String expiryDuration, String message, String smsSenderProviderId) {

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, "1234");
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }

        message = message.replace("{code}", code);

        //getting smsSenderProvider
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        if (context.getRealm().isEnabledOwnSmsProvider()) {
            smsSenderProvider.setProviderId(smsSenderProviderId);
        } else {
            SmsConfigModel smsConfig = getDefaultSmsSender(context.getSession());
            if (smsConfig != null) {
                smsSenderProvider.setProviderId(smsConfig.getSmsProviderId());
            }
        }

        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(context.getUser().getMobileNumber(), context.getUser().getMobileNumberRegion().toUpperCase());
            smsSenderProvider.sendSMS(numberFormat.getE16Format(), message, null);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean sendSmsOtp(RequiredActionContext context,String phoneNumber, String expiryDuration, String smsSenderProviderId) {
        String verify_code_size = getConfigValue(context, PHONE_VER_OTP_LENGTH) == "" ?
                "6" : getConfigValue(context, PHONE_VER_OTP_LENGTH);


        String message = getSmsText(context, PHONE_VER_OTP_TXT);

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, "1234");
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }
        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String generatedOTP = getOtp(verifyCodeSize);
        message = message.replace("{code}", generatedOTP);

        //getting smsSenderProvider
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        if (context.getRealm().isEnabledOwnSmsProvider()) {
            smsSenderProvider.setProviderId(smsSenderProviderId);
        } else {
            SmsConfigModel smsConfig = getDefaultSmsSender(context.getSession());
            if (smsConfig != null) {
                smsSenderProvider.setProviderId(smsConfig.getSmsProviderId());
            }
        }

        try {
            smsSenderProvider.sendSMS(phoneNumber, message, null);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            return false;
        }
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, generatedOTP);
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static boolean sendSmsOtp(MFAConfigModel configModel, RequiredActionContext context, String expiryDuration, String smsSenderProviderId) {
        String verify_code_size = getOtpConfigValue(configModel, PHONE_VER_OTP_LENGTH);

        String message = getSmsText(configModel, PHONE_VER_OTP_TXT);

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, "1234");
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }
        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String generatedOTP = getOtp(verifyCodeSize);
        message = message.replace("{code}", generatedOTP);
        //getting smsSenderProvider
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        if (context.getRealm().isEnabledOwnSmsProvider()) {
            smsSenderProvider.setProviderId(smsSenderProviderId);
        } else {
            SmsConfigModel smsConfig = getDefaultSmsSender(context.getSession());
            if (smsConfig != null) {
                smsSenderProvider.setProviderId(smsConfig.getSmsProviderId());
            }
        }

        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(context.getUser().getMobileNumber(), context.getUser().getMobileNumberRegion().toUpperCase());
            smsSenderProvider.sendSMS(numberFormat.getE16Format(), message, null);
        } catch (SmsException e) {
            ;
            return false;
        }
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, generatedOTP);
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static boolean sendSmsOtp(AuthenticationFlowContext context, String phoneNumber, String expiryDuration, String smsSenderProviderId) {
        String verify_code_size = getOtpConfigValue(context, PHONE_VER_OTP_LENGTH);

        String message = getSmsText(context, PHONE_VER_OTP_TXT);

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, "1234");
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }
        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String generatedOTP = getOtp(verifyCodeSize);
        message = message.replace("{code}", generatedOTP);

        //getting smsSenderProvider
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        if (context.getRealm().isEnabledOwnSmsProvider()) {
            smsSenderProvider.setProviderId(smsSenderProviderId);
        } else {
            SmsConfigModel smsConfig = getDefaultSmsSender(context.getSession());
            if (smsConfig != null) {
                smsSenderProvider.setProviderId(smsConfig.getSmsProviderId());
            }
        }

        try {
            smsSenderProvider.sendSMS(phoneNumber, message, null);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            return false;
        }
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, generatedOTP);
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static SmsConfigModel getDefaultSmsSender(KeycloakSession session) {
        Optional<SmsConfigModel> optSmsConfig = session.realms().getRealmByName(getAdminRealm()).getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && !x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
        if (optSmsConfig.isPresent())
            return optSmsConfig.get();
        return null;
    }

    // Get whatsappSender from AdminRealm
    public static SmsConfigModel getDefaultWhatsappSender(KeycloakSession session) {
        Optional<SmsConfigModel> optSmsConfig = session.realms().getRealmByName(getAdminRealm()).getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
        if (optSmsConfig.isPresent())
            return optSmsConfig.get();
        return null;
    }

    public static SmsConfigModel getWhatsappSender(AuthenticationFlowContext context){
        return getSmsConfig(context.getRealm(), context.getSession());
    }
    public static SmsConfigModel getWhatsappSender(RequiredActionContext context){
        return getSmsConfig(context.getRealm(), context.getSession());
    }

    private static SmsConfigModel getSmsConfig(RealmModel realm, KeycloakSession session){
        if (realm.isEnabledOwnWhatsappProvider()){
            Optional<SmsConfigModel> ownWpConfig =
                    realm.getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
            return ownWpConfig.isPresent() ? ownWpConfig.get() : null;
        } else {
            return GeneralHelper.getDefaultWhatsappSender(session);
        }
    }

    public static String maskEmailAddress(String strEmail)
            throws Exception {

        String[] parts = strEmail.split("@");

        //mask first part
        String strId = "";
        if (parts[0].length() < 4)
            strId = maskString(parts[0], 0, parts[0].length(), '\u2022');
        else
            strId = maskString(parts[0], 3, parts[0].length(), '\u2022');

        //now append the domain part to the masked id part
        return strId + "@" + parts[1];
    }

    public static String maskString(String strText, int start, int end, char maskChar)
            throws Exception {

        if (strText == null || strText.equals(""))
            return "";

        if (start < 0)
            start = 0;

        if (end > strText.length())
            end = strText.length();

        if (start > end)
            throw new Exception("End index cannot be greater than start index");

        int maskLength = end - start;

        if (maskLength == 0)
            return strText;

        StringBuilder sbMaskString = new StringBuilder(maskLength);

        for (int i = 0; i < maskLength; i++) {
            sbMaskString.append(maskChar);
        }

        return strText.substring(0, start)
                + sbMaskString.toString()
                + strText.substring(start + maskLength);
    }

    public static String maskPhoneNumber(AuthenticationFlowContext context, String phoneNumber, String region) throws NullPointerException {
        SmsSenderProvider smsSenderProvider;
        NumberFormat numberFormat = null;
        String phoneNumberRaw = null;
        String countryCode = null;
        smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");

        if (smsSenderProvider == null) {
            redirectToError(context, Constants.NO_SMS_CONFIG);
            return "";
        }

        try {
            numberFormat = smsSenderProvider.parseNumber(phoneNumber, region);
        } catch (SmsException e) {
            ;
        }
        String[] phoneRaw = numberFormat.getInternationalFormat() != null ? numberFormat.getInternationalFormat().split(" ") : new String[]{};

        if (phoneRaw.length > 0) {
            countryCode = phoneRaw[0];
            phoneNumberRaw = StringUtils.join(phoneRaw, "", 1, phoneRaw.length);
        }
        //if international format has dashes in it
        phoneNumberRaw = phoneNumberRaw.replaceAll("[\\-]", "");

        String formattedPhonenumberWithoutCountryCode = phoneNumberRaw.replaceAll("(?<=\\d\\d\\d)\\d(?=\\d\\d)", "\u2022");

        //return Masked PhoneNumber
        return countryCode + formattedPhonenumberWithoutCountryCode;
    }

    public static Long getExpiryDuration(String ttl) {
        logger.info("Expiry Duration: " + getTTLMessage(ttl));
        boolean validate = validateTTL(ttl);
        if (!validate)
            ttl = "1h";
        String timeUnit = "";
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            timeUnit = ttl.substring(ttl.length() - 1);
            ttl = ttl.substring(0, ttl.length() - 1);
        } else {
            timeUnit = "h";
        }
        long duration = Long.parseLong(ttl);
        long expiry;

        if (timeUnit.equals("d")) {
            expiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(duration);
        } else if (timeUnit.equals("h")) {
            expiry = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(duration);
        } else if (timeUnit.equals("s")) {
            expiry = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(duration);
        } else if (timeUnit.equals("m")) {
            expiry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(duration);
        } else {
            expiry = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(duration);
        }
        return expiry;
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

    public static void redirectToError(AuthenticationFlowContext context, String message) {
        if (message == null || message.equals("")) {
            message = ErrorMappings.GENERAL_INSTRUCTION_MESSAGE;
        }
        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, message)
                .createForm(Constants.QV_ERROR_FTL);
        context.challenge(challenge);
    }

    public static void redirectToErrorPage(AuthenticationFlowContext context, String message) {
        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, message)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }

    public static void handleConfigurationError(AuthenticationFlowContext context, EventType eventType, String ftl,
                                                String error){
        logger.warn(error);
        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.client(context.getAuthenticationSession().getClient().getClientId());
        if (context.getUser() != null ) {
            eventBuilder.user(context.getUser());
        } else {
            eventBuilder.user("UNKNOWN_USER");
        }
        eventBuilder.event(eventType);
        eventBuilder.error(error);

        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, CONTACT_ADMIN)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }

    public static void auditEvent(AuthenticationFlowContext context, EventType eventType, String error){
        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.client(context.getAuthenticationSession().getClient().getClientId());

        if (context.getUser() != null ) {
            eventBuilder.user(context.getUser());
        } else {
            eventBuilder.user("UNKNOWN_USER");
        }
        eventBuilder.event(eventType);
        eventBuilder.error(error);

        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, CONTACT_ADMIN)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }

    public static void handleConfigurationError(RequiredActionContext context, EventType eventType, String ftl,
                                                String error){
        logger.warn(error);
        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.client(context.getAuthenticationSession().getClient().getClientId());
        if (context.getUser() != null ) {
            eventBuilder.user(context.getUser());
        } else {
            eventBuilder.user("UNKNOWN_USER");
        }
        eventBuilder.event(eventType);
        eventBuilder.error(error);

        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, CONTACT_ADMIN)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
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

    //method used to get Realm Name to map in Email Template
    private static String getRealmName(RealmModel realm) {
        if (realm.getDisplayName() != null) {
            return realm.getDisplayName();
        } else {
            return ObjectUtil.capitalize(realm.getName());
        }
    }


    //method used to get companyName by hierarchy from RAContext;if all are not set,default is QlikVerify
    public static String getCompanyName(AuthenticationFlowContext context) {
        String name = "ezto verify";
        try {
            if (context.getAuthenticationSession().getClient().getName() != null) {
                name = context.getAuthenticationSession().getClient().getName();
            } else if (context.getRealm().getDisplayName() != null) {
                name = context.getRealm().getDisplayName();
            } else if (context.getRealm().getName() != null) {
                name = context.getRealm().getName();
            }
            return name;
        } catch (Exception err) {
        }
        return name;
    }

    //method used to get companyName by hierarchy from RAContext;if all are not set,default is QlikVerify
    public static String getCompanyName(RequiredActionContext context) {
        String name = "ezto verify";
        try {
            if (context.getAuthenticationSession().getClient().getName() != null) {
                name = context.getAuthenticationSession().getClient().getName();
            } else if (context.getRealm().getDisplayName() != null) {
                name = context.getRealm().getDisplayName();
            } else if (context.getRealm().getName() != null) {
                name = context.getRealm().getName();
            }
            return name;
        } catch (Exception err) {
        }
        return name;
    }

    //method to get the specific configProperty  from list
    public static ProviderConfigProperty getConfigProp(List<ProviderConfigProperty> props, String propName) {
        Optional<ProviderConfigProperty> configProperty = props.stream()
                .filter(x -> x.getName() != null && x.getName().equals(propName))
                .findFirst();
        return configProperty.isPresent() ? configProperty.get() : null;
    }

    //to get BruteForceProtector Provider
    public static BruteForceProtector getProtector(RequiredActionContext context){
        return context.getSession().getProvider(BruteForceProtector.class);
    }


    public static boolean isTemporarilyDisabledByBruteForce(AuthenticationFlowContext context, UserModel user,String errorMsg) {
        if(TextUtils.isEmpty(errorMsg)){
            errorMsg = ErrorMappings.MAXIMUM_ATTEMPTS_REACHED_USER_TEMP_DISABLED;
        }
        if (context.getRealm().isBruteForceProtected() && context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
                Response challengeResponse = context.form()
                        .setAttribute(ERROR_MESSAGE, errorMsg)
                        .createForm(ERROR_FTL);
                context.challenge(challengeResponse);
                return true;
        }
        return false;
    }

    public static boolean isTemporarilyDisabledByBruteForce(RequiredActionContext context, UserModel user,String errorMsg) {
        if(TextUtils.isEmpty(errorMsg)){
            errorMsg = ErrorMappings.MAXIMUM_ATTEMPTS_REACHED_USER_TEMP_DISABLED;
        }
        if (context.getRealm().isBruteForceProtected() && getProtector(context).isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
                Response challengeResponse = context.form()
                        .setAttribute(ERROR_MESSAGE, errorMsg)
                        .createForm(ERROR_FTL);
                context.challenge(challengeResponse);
                return true;
        }
        return false;
    }



    public static boolean checkEmailConfigured(AuthenticationFlowContext context, boolean isReg){
        if (!context.getRealm().getName().equalsIgnoreCase(getAdminRealm())
                && context.getRealm().isEnabledOwnEmailProvider()) {
            Optional<String> emailProviders = context.getRealm().getEmailSenderStream().findFirst();

            if(!emailProviders.isPresent()){
                return false;
            }

            Map<String, String> authenticationFlowBindingOverrides =
                    context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverrides();
            if (isReg && (authenticationFlowBindingOverrides.getOrDefault("onboarding", null) == null) && !emailProviders.isEmpty()){
               // Client has default onboarding flow
                return true;
            }

            if (!isReg && (authenticationFlowBindingOverrides.getOrDefault("login", null) == null) && !emailProviders.isEmpty() ){
                // Client has default login flow
                return true;
            }

            if (emailProviders.isEmpty() && (context.getAuthenticatorConfig() == null || context.getAuthenticatorConfig().getConfig().get(EMAIL_SENDER) == null)) {
                return false;
            }
        } else {
            Optional<EmailConfigModel> optEmailConfig =
                    context.getSession().realms().getRealmByName(getAdminRealm()).getEmailConfigsStream().findFirst();
            if (optEmailConfig.isEmpty()){
                return false;
            }
        }
        return true;
    }

    public static boolean checkSMSConfigured(AuthenticationFlowContext context){
        if (context.getRealm().isEnabledOwnSmsProvider()) {
            final Optional<String> smsProviderId = context.getRealm().getSmsSenderStream().filter(x -> !Objects.equals(x,
                    "whatsapp")).findFirst();
            if (smsProviderId.isEmpty() || context.getAuthenticatorConfig() == null || context.getAuthenticatorConfig().getConfig().get(SMS_SENDER) == null) {
                return false;
            }
        } else {
            Optional<SmsConfigModel> optSmsConfig =
                    context.getSession().realms().getRealmByName(getAdminRealm()).getSmsConfigsStream().findFirst();
            if (optSmsConfig.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static String checkPasswordLoginRestrictions(AuthenticationFlowContext context,String loginWay){
        if(loginWay.equalsIgnoreCase(EMAIL)){
            return context.getUser().isEmailVerified() ? null : ErrorMappings.EMAIL_NOT_VERIFIED;
        } else if(loginWay.equalsIgnoreCase(PHONE_NUMBER)){
            return Boolean.parseBoolean(context.getUser().getFirstAttribute(ATTRIBUTE_PHONE_VERIFIED)) ? null :ErrorMappings.PHONE_NUMBER_NOT_VERIFIED;
        }
        return null;
    }


    public static JSONResponseModel jsonResponseRepToModel(String jsonInput) {
        try {
            if (StringUtils.isBlank(jsonInput))
                return null;
            return new ObjectMapper().readValue(jsonInput, JSONResponseModel.class);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static void setFormAttrs(AuthenticationFlowContext context, LoginFormsProvider form, JSONResponseModel model){
        logger.info("qlikverify-services : helpers : GeneralHelper : setFormAttrs");
        if(model != null){

            // form Title
            if(model.getFormDisplayTitle() != null){
                form.setAttribute("formDisplayTitle", getFormText(context,model.getFormDisplayTitle()));
            }

            if(model.getFormDisplayTitleAlign() != null){
                form.setAttribute("formDisplayTitleAlign", model.getFormDisplayTitleAlign());
            }
            if(model.getFormDisplayTitleClr() != null){
                form.setAttribute("formDisplayTitleClr", model.getFormDisplayTitleClr());
            }

            //form description
            if(model.getFormDescription() != null){
                form.setAttribute("formDescription", getFormText(context,model.getFormDescription()));
            }
            if(model.getFormDescriptionClr() != null){
                form.setAttribute("formDescriptionClr",model.getFormDescriptionClr());
            }
            if(model.getFormDisplayTitleClr() != null){
                form.setAttribute("formDescriptionAlign", model.getFormDescriptionAlign());
            }


            //form inputs
            if(model.getFormInputs() != null) {
                List<JsonFormInputRep> formInputs = new ArrayList<>();
                List<JSONFormInputModel> inputs = model.getFormInputs();

                inputs.forEach( ip -> {
                    JsonFormInputRep ipDetails = new JsonFormInputRep();
                    ipDetails.setType(ip.getType());
                    ipDetails.setInputId(ip.getInputId());
                    ipDetails.setLabelTxt(getFormText(context, ip.getLabelTxt()).toString());
                    ipDetails.setPlaceholderTxt(getFormText(context,ip.getPlaceholderTxt()).toString());
                    ipDetails.setPattern(ip.getPattern());
                    ipDetails.setLabel(ip.getLabel());
                    if(getFormText(context,ip.getOptions()) == "")
                        ipDetails.setOptions(null);
                    else
                        ipDetails.setOptions((ArrayList<String>) getFormText(context,ip.getOptions()));
                    formInputs.add(ipDetails);
                });
                form.setAttribute("formInputs", formInputs);
            }

            //form buttons
            if(model.getFormDisplayButtons() != null) {
                List<JSONButtonRep> formButtons = new ArrayList<>();
                List<JSONButton> btns = model.getFormDisplayButtons();

                btns.forEach( btn -> {
                    JSONButtonRep btnDetails = new JSONButtonRep();
                    btnDetails.setBtnId(btn.getBtnId());
                    btnDetails.setBtnBgClr(btn.getBtnBgClr());
                    btnDetails.setBtnText(getFormText(context,btn.getBtnText()).toString());
                    btnDetails.setBtnTextClr(btn.getBtnTextClr());
                    formButtons.add(btnDetails);
                });

                form.setAttribute("formButtons", formButtons);
            }
        }
    }

    public static Object getFormText(AuthenticationFlowContext context,Map<String, Object> formTexts){
        if (formTexts == null)
            return "";

        Object localizedText = null;
        Map<String, Object> formTextWithLocales = null;
        ArrayList<String> formArrayTextWithLocales = null;

        /** Get Locale to Pick from FormTexts */
        String locale = getLocale(context);

        /** Get Localized formText Value */
        try {
            formTextWithLocales = new ObjectMapper().convertValue(formTexts, new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception e) {
            logger.warn("Locales not found for " + localizedText);
            return String.valueOf(localizedText);
        }

        if (formTextWithLocales != null && !formTextWithLocales.isEmpty()) {
            return formTextWithLocales.get(locale);
        }

        return null;
    }



    public static String getLocale(AuthenticationFlowContext context) {
        String currentLang = context.getAuthenticationSession().getAuthNote("current_locale");
        if(StringUtils.isNotBlank(currentLang))
            return currentLang;
        return context.getSession().getContext().resolveLocale(context.getUser(),LOGIN).getLanguage();
    }

    public static boolean sendOtpToEmailAndPhone(AuthenticationFlowContext context) {

        String verify_code_size = getOtpConfigValue(context, EMAIL_PHONE_OTP_LEN);
        int verifyCodeSize = Integer.parseInt(verify_code_size);
        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }

        String expiry = getConfigValue(context, EMAIL_PHONE_EXP_TIME);
        if(expiry.isBlank()){
            expiry = "5m";
        }

        String emailCode  = GeneralHelper.getOtp(verifyCodeSize);
        String mobileCode = emailCode;

        if (!getConfigValue(context, "sameOtp").equals("true")) {
            mobileCode = GeneralHelper.getOtp(verifyCodeSize);
        }

        if(!sendEmailOTP(context, context.getUser().getEmail(), emailCode, expiry) || !sendSmsOtp(context, mobileCode, expiry, getSmsText(context, PHONE_VER_OTP_TXT), getConfigValue(context, SMS_SENDER)))
            return false;

        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, emailCode);
        context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, mobileCode);
        context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiry)));

        return true;
    }

    public static boolean isUserEmailVerified(UserModel user, RealmModel realm){
        if(realm.isVerifyEmail()){
            return user.isEmailVerified();
        }
        return true;
    }

    public static void createAndAddRole(KeycloakSession innerSession, RealmModel realm, AdminEventBuilder adminEvent, UserModel userModel, String userRole){
        if (Validation.isBlank(userRole)) {
            logger.error("role name not available");
            return;
        }
        if(userModel == null){
            logger.error("user model cannot be null");
            return;
        }
        RoleRepresentation rep = new RoleRepresentation();
        rep.setName(userRole);

        try {
            RoleModel role;
            if(innerSession.roles().getRealmRole(realm, rep.getName().toUpperCase()) != null || innerSession.roles().getRealmRole(realm, rep.getName().toLowerCase()) != null){
                logger.error("Role with name " + rep.getName() + " already exists");
                role = realm.getRole(rep.getName());
                if(role == null){
                    logger.error("Role not available in realm");
                    return;
                }
            } else {
                role = realm.addRole(rep.getName());
                role.setDescription(rep.getDescription());

                Map<String, List<String>> attributes = rep.getAttributes();
                if (attributes != null) {
                    attributes.forEach(role::setAttribute);
                }
                rep.setId(role.getName());

                userModel.grantRole(realm.getRole(userRole));
                adminEvent.resource(role.isClientRole() ? ResourceType.CLIENT_ROLE : ResourceType.WORKSPACE_ROLE);
                adminEvent.operation(OperationType.CREATE).representation(rep).success();
            }
            logger.info(userModel.getUsername() + " is assigned with the role - " + userRole + " created through userSync");
        } catch (ModelDuplicateException e) {
            logger.debug("Role with name " + rep.getName() + " already exists");
        }
    }

    public static Locale getLocale(KeycloakSession session, UserModel userModel) {
        if (userModel != null) {
            String userPreferredLocale = userModel.getFirstAttribute("preferred_locale");
            String cookieLocale = session.getContext().getRequestHeaders() != null && session.getContext().getRequestHeaders().getCookies().containsKey("KEYCLOAK_LOCALE") ? session.getContext().getRequestHeaders().getCookies().get("KEYCLOAK_LOCALE").getValue() : "";
            if(cookieLocale != null && !cookieLocale.isEmpty() && session.getContext().getRealm().getPreferredLoginLocalesStream().filter(x->x.equals(cookieLocale)).count() == 1) {
                boolean isSupported = org.keycloak.models.Constants.SUPPORTED_LOCALES.stream().anyMatch(supportedLocale -> supportedLocale.equalsIgnoreCase(cookieLocale));
                if (isSupported) {
                    return new Locale.Builder().setLanguage(cookieLocale).build();
                }
            }
            if (userPreferredLocale != null && !userPreferredLocale.isEmpty()) {
                boolean isSupported = org.keycloak.models.Constants.SUPPORTED_LOCALES.stream().anyMatch(supportedLocale -> supportedLocale.equalsIgnoreCase(userPreferredLocale));
                if (isSupported) {
                    return new Locale.Builder().setLanguage(userPreferredLocale).build();
                }
            }
        }
        return Locale.ENGLISH;
    }
}
