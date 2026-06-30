package org.keycloak.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.onboarding.OnboardingRepresentation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.*;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.authentication.authenticators.utils.Constants;

import javax.imageio.ImageIO;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MultivaluedMap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.stream.Stream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.keycloak.authentication.authenticators.utils.Constants.*;
import static org.keycloak.authentication.authenticators.utils.GeneralHelper.getExpiryDuration;
import static org.keycloak.models.Constants.HIGH_SECURE;
import static org.keycloak.models.Constants.VERIFICATION_EMAIL_CODE;
import static org.keycloak.models.ParConfig.REQUIRE_PUSHED_AUTHORIZATION_REQUESTS;
import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.getDefaultSmsSender;
import static org.keycloak.services.messages.Messages.SUPPORT_CUSTOM_MESSAGE;
import static org.keycloak.services.messages.Messages.SUPPORT_CUSTOM_TOGGLE;
import static org.keycloak.services.resources.admin.RealmsAdminResource.getEmailTemplate;

public class GeneralHelper {

    private static final Logger log = Logger.getLogger(GeneralHelper.class);
    private static int year = Calendar.getInstance().get(Calendar.YEAR);

    public static final String MINIO_SERVER_URL = "MINIO_SERVER_URL";
    public static final String MINIO_ACCESS_KEY = "MINIO_ACCESS_KEY";
    public static final String MINIO_SECRET_KEY = "MINIO_SECRET_KEY";
    public static final String MINIO_SERVER_API_URL = "MINIO_SERVER_API_URL";
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(MINIO_SERVER_URL);
    private static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(MINIO_ACCESS_KEY);
    private static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(MINIO_SECRET_KEY);
    private static final String DEFAULT_MINIO_SERVER_API_URL = EnvironmentReader.getValue(MINIO_SERVER_API_URL);
    private static final String HYPEN = Character.toString(45);

    protected static final Logger logger = Logger.getLogger(GeneralHelper.class);

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

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVW1234567890";

    public static String getRandomValue(int len) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = CHARS.charAt(r.nextInt(CHARS.length()));
            sb.append(c);
        }
        return sb.toString();
    }

    //get Random OTP with given length
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

    // get exp duration in epoch
    public static Long getEpochDuration(String ttl){
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

    public static Date getReverseDate(String ttl){
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            ttl = ttl.substring(0, ttl.length() - 1);
        }
        long duration = Long.parseLong(ttl);
        Instant expiry = LocalDate.now().minusDays(duration).atStartOfDay(ZoneId.of("UTC")).toInstant();
        return Date.from(expiry);
    }

    // get exp duration in unix
    public static String getUnixDuration(String ttl) {
        boolean validate = validateTTL(ttl);
        if (!validate)
            ttl = "1h";
        String timeUnit = "h";
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            timeUnit = ttl.substring(ttl.length() - 1);
            ttl = ttl.substring(0, ttl.length() - 1);
        }
        long duration = Long.parseLong(ttl);

        if (timeUnit.equals("d")) {
            if (duration == 1)
                return duration + " Day";
            return duration + " Days";
        } else if (timeUnit.equals("h")) {
            if (duration == 1)
                return duration + " Hour";
            return duration + " Hours";
        } else if (timeUnit.equals("m")) {
            if (duration == 1)
                return duration + " Minute";
            return duration + " Minutes";
        } else if (timeUnit.equals("s")) {
            if (duration == 1)
                return duration + " Second";
            return duration + " Seconds";
        } else {
            if (duration == 1)
                return duration + " Hour";
            return duration + " Hours";
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
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    public static Map<String, String> showSecret(List<ProviderConfigProperty> configList, Map<String, String> settings) {
        Map<String, String> scrt = new HashMap<>();

        if (configList == null) {
            if (settings.containsKey("clientSecret")) {
                scrt.put("clientSecret", settings.get("clientSecret"));
            }
            return scrt;
        }

        //if properties are not null,then we loop
        configList.forEach(x -> {
            if (x.isSecret()) {
                scrt.put(x.getName(), settings.get(x.getName()));
            }
        });

        return scrt;
    }

    public static Map<String, String> removeInvalidSecrets(List<ProviderConfigProperty> configList, Map<String, String> fromConfig, Map<String, String> settings) {
        configList.forEach(x -> {
            if (x.isSecret() && !checkIsValidSecret(x.getName(), settings)) {
                if (fromConfig != null && fromConfig.containsKey(x.getName())) {
                    settings.put(x.getName(), fromConfig.get(x.getName()));
                }
            }
        });
        return settings;
    }

    public static boolean checkIsValidSecret(String propName, Map<String, String> configuredSettings) {
        if (configuredSettings.containsKey(propName) && configuredSettings.get(propName).equals(ComponentRepresentation.SECRET_VALUE)) {
            return false;
        }
        return true;
    }

    public static boolean checkIsEmailProviderSet(KeycloakSession session, RealmModel realm) {
        if (session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getEmailConfigsStream().count() > 0) {
            return true;
        }
        if (realm.getEmailConfigsStream().count() > 0) {
            return true;
        }
        return false;
    }

    public static boolean isValidTenantName(String realmId) {
//        String regex = "^(?!-.)[a-z0-9-]+.[a-z0-9]$";
        String regex = "^(?!-.)[a-z0-9A-Z- ]{3,55}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(realmId);
        return matcher.matches();
    }


    public static boolean checkUserIsAdmin(KeycloakSession session, RealmModel realm, UserModel user) {
        RoleModel roleModel = getAdminRole(session, realm);
        return user.hasRole(roleModel);
    }

    public static RoleModel getRealmAdminRole(KeycloakSession session, RealmModel realm) {
        return session.roles().getRealmRole(realm, "admin");
    }

    public static RoleModel getAdminRole(KeycloakSession session, RealmModel realm) {
        ClientModel clientModel = realm.getClientByClientId("realm-management");
        if (clientModel == null)
            return getRealmAdminRole(session, realm);

        RoleModel clientRoleModel = session.roles().getClientRole(clientModel, "workspace-admin");

        if (clientRoleModel == null)
            return getRealmAdminRole(session, realm);

        return clientRoleModel;
    }


    public static boolean hasScripts(String html) {
        String scriptRegex = "<\\s?\\/?script\\s*?>";
        Pattern pattern = Pattern.compile(scriptRegex, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(html).find();
    }

    //fetches the  uploaded filename
    public static String getFileName(MultivaluedMap<String, String> header) {

        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {

                String[] name = filename.split("=");

                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }

    //checks for extension of the image was correct
    public static boolean checkForCorrectExtension(String extension) {
        boolean isCorrect = false;
        String[] extTypeArray = new String[]{"jpg", "svg", "png", "jpeg"};
        if (extension != null) {
            isCorrect = Arrays.stream(extTypeArray).anyMatch(ext -> ext.equalsIgnoreCase(extension));
        }
        return isCorrect;
    }


    public static NumberFormat parseMobileNumber(RequiredActionContext context, String region, String number) {
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(number, region.toUpperCase());
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
        return numberFormat;
    }

    public static boolean sendSmsOtp(RequiredActionContext context, String phoneNumber, String expiryDuration, String smsSenderProviderId) {
        String verify_code_size = getConfigValue(context, Constants.PHONE_VER_OTP_LENGTH) == "" ?
                "6" : getConfigValue(context, Constants.PHONE_VER_OTP_LENGTH);


        String message = getSmsText(context, Constants.PHONE_VER_OTP_TXT);

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
            log.error(e.getMessage());
            return false;
        }
        context.getAuthenticationSession().setAuthNote(Constants.SESSION_MOBILE_OTP, generatedOTP);
        context.getAuthenticationSession().setAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static String getSmsText(RequiredActionContext context, String configName) {

        if (context.getConfig() == null)
            return Constants.DEFAULT_MESSAGE_TXT;
        String configValue = context.getConfig().get(configName);
        if (configValue == null || configValue.length() == 0)
            return Constants.DEFAULT_MESSAGE_TXT;

        return configValue;
    }

    public static boolean sendEmailOTP(RequiredActionContext context, String email, String expiryDuration) {
        String verify_code_size = getConfigValue(context, Constants.EMAIL_VER_OTP_LENGTH) == "" ?
                "6" : getConfigValue(context, Constants.EMAIL_VER_OTP_LENGTH);
        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String code = GeneralHelper.getOtp(verifyCodeSize);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));
        attributes.put("year", String.valueOf(year));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, Constants.EMAIL_SENDER);
            if (emailProviderId == null || emailProviderId.isEmpty()) {
                log.error("Email Provider is not set for the authenticator");
                return false;
            }
        }
        try {
            String htmlBody = getEmailTemplate(context.getSession(), context.getRealm().getName(), removeExtension(VERIFICATION_EMAIL_CODE));
            attributes.put("tenant", context.getRealm().getName());
            if (htmlBody != null && !htmlBody.isEmpty())
                attributes.put("htmlBody", htmlBody);
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setRealm(context.getRealm())
                    .setEmailProviderId(emailProviderId)
                    .send(email, "emailVerificationSubject", "email-verification-with-code.ftl", attributes);
        } catch (EmailException e) {
            log.error(e.getMessage());
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return false;
        }

        context.getAuthenticationSession().setAuthNote(Constants.SESSION_EMAIL_OTP, code);
        context.getAuthenticationSession().setAuthNote(Constants.SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static String checkAppName(String name) {
        String finalAppName = "";
        if (name.contains(" ")) {
            finalAppName = name.replaceAll("\\s+", " ");
            return finalAppName.trim();
        }
        return name.trim();
    }

    public static Map<String, Object> getTTLMessage(String ttl) {
        boolean validate = validateTTL(ttl);
        Map<String, Object> timeDuration = new HashMap<>();
        if (!validate)
            ttl = "1h";
        String timeUnit = "h";
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            timeUnit = ttl.substring(ttl.length() - 1);
            ttl = ttl.substring(0, ttl.length() - 1);
        }
        long duration = Long.parseLong(ttl);

        if (timeUnit.equals("d")) {
            if (duration == 1) {
                timeDuration.put("time", 1);
                timeDuration.put("unit", "day");
            } else {
                timeDuration.put("time", duration);
                timeDuration.put("unit", "days");
            }
            return timeDuration;
        } else if (timeUnit.equals("h")) {
            if (duration == 1) {
                timeDuration.put("time", 1);
                timeDuration.put("unit", "hour");
            } else {
                timeDuration.put("time", duration);
                timeDuration.put("unit", "hours");
            }
            return timeDuration;
        } else if (timeUnit.equals("m")) {
            if (duration == 1) {
                timeDuration.put("time", 1);
                timeDuration.put("unit", "minute");
            } else {
                timeDuration.put("time", duration);
                timeDuration.put("unit", "minutes");
            }
            return timeDuration;
        } else if (timeUnit.equals("s")) {
            if (duration == 1) {
                timeDuration.put("time", 1);
                timeDuration.put("unit", "second");
            } else {
                timeDuration.put("time", duration);
                timeDuration.put("unit", "seconds");
            }
            return timeDuration;
        } else {
            if (duration == 1) {
                timeDuration.put("time", 1);
                timeDuration.put("unit", "hour");
            } else {
                timeDuration.put("time", duration);
                timeDuration.put("unit", "hours");
            }
            return timeDuration;
        }
    }

    public static boolean isValidEmail(String email) {
        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static boolean isValidPhoneNumber(UserModel user, RealmModel realm) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber parsed;
        try {
            boolean hasCode = phoneNumberHasCode(user);
            if (hasCode) {
                return false;
            }
            if (user.getMobileNumberRegion() == null || user.getMobileNumberRegion().isEmpty()) {
                parsed = phoneUtil.parse(user.getMobileNumber(), realm.getDefaultRegion().toUpperCase());
            } else {
                parsed = phoneUtil.parse(user.getMobileNumber(), user.getMobileNumberRegion().toUpperCase());
            }

            if (!phoneUtil.isValidNumber(parsed)) {
                return false;
            }
            return true;

        } catch (NumberParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean phoneNumberHasCode(UserModel user) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            if (phoneUtil.parse(user.getMobileNumber(), "").hasCountryCode()) {
                return true;
            }
        } catch (NumberParseException e) {
            return false;
        }
        return false;
    }

    public static void adminEvent(KeycloakSession session, OperationType operationType, ResourceType resourceType) {
        UserRepresentation rep = new UserRepresentation();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, String.valueOf(session.getAttribute("userId")));
        rep.setUsername(String.valueOf(session.getAttribute("userId")));

        ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), "dashboard");

        AdminEventBuilder adEvt = new AdminEventBuilder(realm, new AdminAuth(session.getContext().getRealm(),
                null, user, client), session,
                session.getContext().getConnection());
        adEvt.realm(realm);
        adEvt.operation(operationType)
                .resource(resourceType)
                .resourcePath(session.getContext().getUri())
                .representation(rep)
                .authUser(session.users().getUserByEmail(realm, user.getEmail()))
                .success();
    }

    public static boolean validationForHtmlInjection(String inputValue) {
        if (inputValue != null) {
            Pattern pattern = Pattern.compile("<.*?>");
            Matcher matcher = pattern.matcher(inputValue);
            String output = StringEscapeUtils.escapeHtml(inputValue);
            return matcher.find() && !output.equals(inputValue);
        }
        return false;
    }

    public static boolean validationForHtmlInjection(String modelAlias, String inputValue) {
        if ("bo-api-request".equalsIgnoreCase(modelAlias))
            return false;
        return validationForHtmlInjection(inputValue);
    }

    public static boolean isValidSvgFile(InputPart file) {
        String svgContent = null;
        String sanitizedSvgContent = null;
        File santizedFile = null;
        try {
            santizedFile = new File(IOUtils.toString(file.getBody(InputStream.class, null), StandardCharsets.UTF_8));

            svgContent = santizedFile.toString();
            sanitizedSvgContent = svgContent.replace("<script>", "");
            sanitizedSvgContent = svgContent.replace("</script>", "");

            return svgContent.equals(sanitizedSvgContent);
        } catch (IOException e) {
            log.info("IO Exception occurred while parsing svg file at keycloak-services :: GeneralHelper :: validateSvgFile()");
            throw new RuntimeException(e);
        }
    }

    public static boolean isValidSvgFile(String file) {
        String svgContent = null;
        String sanitizedSvgContent = null;
        svgContent = file;
        sanitizedSvgContent = svgContent.replace("<script>", "");
        sanitizedSvgContent = svgContent.replace("</script>", "");

        return svgContent.equals(sanitizedSvgContent);
    }

    public static Map<String, Object> parseCurl(String curlCommand) {
        Map<String, Object> request = new HashMap<>();
        curlCommand = curlCommand.replaceAll("@@[^@]+@@", "");

        String regexForMethod = "(?i)(?:-X|--request)\\s+([A-Z]+)";
        Pattern patternForMethod = Pattern.compile(regexForMethod);
        Matcher matcherForMethod = patternForMethod.matcher(curlCommand);
        if (matcherForMethod.find()) {
            request.put("method", matcherForMethod.group(1));
        }

        Pattern patternForUrl = Pattern.compile("'(.*?)'");
        Matcher matcherForUrl = patternForUrl.matcher(curlCommand);
        if (matcherForUrl.find()) {
            request.put("url", matcherForUrl.group(1));
        }

        Pattern patternForHeader = Pattern.compile("--header '(.*?)'");
        Matcher matcherForHeader = patternForHeader.matcher(curlCommand);

        Map<String, String> headers = new HashMap<>();
        while (matcherForHeader.find()) {
            String[] header = matcherForHeader.group(1).split(":");
            headers.put(header[0], header[1]);
        }
        request.put("header", headers);

        Pattern formPattern = Pattern.compile("--form\\s+'([^=]+)=\"?([^\"']+)\"?'");
        Matcher formMatcher = formPattern.matcher(curlCommand);
        Map<String, String> formData = new HashMap<>();
        while (formMatcher.find()) {
            formData.put(formMatcher.group(1).trim(), formMatcher.group(2).trim());
        }
        if (!formData.isEmpty()) {
            request.put("form", formData);
        }

        Pattern rawDataPattern = Pattern.compile("--data(?:-raw|-urlencode)?\\s+'([\\s\\S]*?)'");
        Matcher rawDataMatcher = rawDataPattern.matcher(curlCommand);
        StringBuilder rawData = new StringBuilder();
        while (rawDataMatcher.find()) {
            if (rawData.length() > 0) rawData.append("&");
            rawData.append(rawDataMatcher.group(1).replaceAll("\\s+", " ").trim());
        }
        if (rawData.length() > 0) {
            request.put("body", rawData.toString());
        }

//        Pattern patternForBody = Pattern.compile("--data(?:-raw|-urlencode)?\\s+'([\\s\\S]*?)'");
//        Matcher matcherForBody = patternForBody.matcher(curlCommand);
//        String bodyContent = "";
//        while(matcherForBody.find()) {
//            if(!bodyContent.isEmpty()) {
//                bodyContent = bodyContent + "&" + matcherForBody.group(1);
//            } else {
//                bodyContent = matcherForBody.group(1);
//            }
//        }
//        request.put("body", bodyContent.replace("\n",""));

        return request;
    }

    public static List<String> getRequiredActionsInFlow(RealmModel realm, String actionId) {
        return realm.getRequiredActionProvidersStream(actionId)
                .map(RequiredActionProviderModel::getProviderId)
                .collect(Collectors.toList());
    }

    public static List<Map<String, Object>> getGifUrlMap(KeycloakSession session, RealmModel realm, String actionId, String locale) {

        List<Map<String, Object>> gifList = new ArrayList<>();

        List<String> requiredActionsInFlow = getRequiredActionsInFlow(realm, actionId);

        requiredActionsInFlow.forEach(templateId -> {
            session.getKeycloakSessionFactory().getProviderFactoriesStream(RequiredActionProvider.class)
                    .filter(factory -> factory.getId().equals(templateId))
                    .map(factory -> (RequiredActionFactory) factory)
                    .forEach(factory -> {
                        if (factory.gifUrl(locale) != null) {
                            gifList.add(new HashMap<String, Object>() {{
                                put(GIF_ID, factory.getDisplayText());
                                if (!factory.getId().equals(WEB_TO_DEVICE)) {
                                    put(GIF_URL, factory.gifUrl(locale));
                                }
                            }});
                        }
                    });
        });

        return gifList;
    }

    public static String encryptReqBody(RealmModel realm, ClientModel client, String curlCommand) throws IOException {

        Pattern patternForBody = Pattern.compile("--data-urlencode 'request=([^']*)'");
        Matcher matcherForBody = patternForBody.matcher(curlCommand);
        String bodyContent = "";
        while (matcherForBody.find()) {
            bodyContent = matcherForBody.group(1);

            ComponentModel componentModel = realm.getComponent(client.getAttribute("ciba_encryption_id"));
            if (componentModel == null)
                throw new IOException("Encryption key is not set");
            String encrypted = "";
            try {
                encrypted = AESHelper.encrypt(bodyContent.replace("\n", "").replace(" ", ""), componentModel.get("secret"));
                encrypted = URLEncoder.encode(encrypted, "UTF-8");
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
            curlCommand = curlCommand.replace(matcherForBody.group(1), encrypted);
        }
        return curlCommand;
    }

    public static void validationsForClientCreation(RealmModel realm, AdminEventBuilder adminEvent, String appName) {
        if (appName == null || appName.isEmpty()) {
            throw new IllegalArgumentException("Failed to create application with empty application name");
        } else if (!appName.matches("^[\\p{L}\\p{M}\\p{Z}\\-]+$")) {
            throw new IllegalArgumentException("invalid-client-name");
        }

        if (GeneralHelper.validationForHtmlInjection(appName)) {
            adminEvent.operation(OperationType.CREATE_FAILED).representation(appName).error(HTML_INJECTED_IN_FIELDS).success();
            throw new IllegalArgumentException("Failed to create application with invalid name. Check Audit > Admin Logs for further information");
        }

        if (appName.trim().length() > 64) {
            adminEvent.operation(OperationType.CREATE_FAILED).representation(appName).error("Application name exceeded 64 characters.").success();
            throw new IllegalArgumentException("Enter a valid application name");
        }

        if (realm.getClientsStream().map(ClientModel::getName).anyMatch(client -> client.equalsIgnoreCase(appName.trim()))) {
            throw new IllegalArgumentException("application-already-exist");
        }
    }

    public static void validationsForClientUpdation(RealmModel realm, AdminEventBuilder adminEvent, String clientId, String updatedAppName) {
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Application not found");
        }

        Optional<ClientModel> clientToBeUpdated = realm.getClientsStream().filter(x -> x.getName().equalsIgnoreCase(updatedAppName)).findAny();

        if (clientToBeUpdated.isPresent() && clientToBeUpdated.get().getId().equalsIgnoreCase(clientId)) {
            // NO OP - Ignore
            return;
        } else if (clientToBeUpdated.isPresent() && clientToBeUpdated.get().getName().equalsIgnoreCase(client.getName())) {
            // NO OP - Ignore
            return;
        } else {
            validationsForClientCreation(realm, adminEvent, updatedAppName);
        }
    }

    public static String getAppType(ClientModel clientModel) {
        if (clientModel.isImplicitFlowEnabled()) {
            if (clientModel.getAttribute("iws.enable") != null && clientModel.getAttribute("iws.enable").equals("true")) {
                return org.keycloak.models.Constants.IWS;
            }
            return org.keycloak.models.Constants.BROWSER_BASED;
        }
        if (clientModel.isStandardFlowEnabled()) {
            if (clientModel.getProtocol().equalsIgnoreCase("saml")) {
                return org.keycloak.models.Constants.SAML;
            } else if (clientModel.getAttribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED) == null) {
                return org.keycloak.models.Constants.HIGH_SECURE;
            } else {
                return org.keycloak.models.Constants.CIBA;
            }
        }
        if (clientModel.isServiceAccountsEnabled()) {
            return org.keycloak.models.Constants.M2M;
        }
        if (clientModel.isDirectAccessGrantsEnabled()) {
            return org.keycloak.models.Constants.REST_API;
        }
        throw new IllegalArgumentException("Invalid app type to duplicate");
    }

    // to set CIBA configs to duplicated app
    public static void setUpConfigs(RealmModel realm, ClientModel appToBeDuplicated, ClientModel newApp) {
        CibaNotificationConfigModel cibaNotifyConfigOdAppToDuplicate = realm.getCibaNotificationConfigByClientId(appToBeDuplicated.getClientId(), realm.getId());

        CibaAuthConfigModel cibaAuthConfigModel = new CibaAuthConfigModel();
        cibaAuthConfigModel.setId(KeycloakModelUtils.generateId());
        cibaAuthConfigModel.setCibaAuthProviderId(appToBeDuplicated.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER));
        cibaAuthConfigModel.setConfig(realm.getCibaAuthConfigByClientId(appToBeDuplicated.getClientId(), realm.getId()).getConfig());
        cibaAuthConfigModel.setClientId(newApp.getClientId());
        cibaAuthConfigModel.setRealmId(realm.getId());
        realm.addCibaAuthConfig(cibaAuthConfigModel);

        if (cibaNotifyConfigOdAppToDuplicate != null) {
            CibaNotificationConfigModel cibaNotificationConfigModel = new CibaNotificationConfigModel();
            cibaNotificationConfigModel.setId(KeycloakModelUtils.generateId());
            cibaNotificationConfigModel.setCibaNotificationProviderId(cibaNotifyConfigOdAppToDuplicate.getCibaNotificationProviderId());
            cibaNotificationConfigModel.setConfig(cibaNotifyConfigOdAppToDuplicate.getConfig());
            cibaNotificationConfigModel.setClientId(newApp.getClientId());
            cibaNotificationConfigModel.setRealmId(realm.getId());
            realm.addCibaNotificationConfig(cibaNotificationConfigModel);
        }

        if (newApp.getAttributes().get("ciba.backchannel.dead.letter.hook.delivery.mode") != null) {
            Map<String, String> config = realm.getDeadLetterHookNotificationConfigByProviderId(newApp.getAttributes().get("ciba.backchannel.dead.letter.hook.delivery.mode"), appToBeDuplicated.getClientId(), realm.getId()).getConfig();
            String providerId = newApp.getAttributes().get("ciba.backchannel.dead.letter.hook.delivery.mode");
            if (config != null && realm.getDeadLetterHookNotificationConfigByProviderId(providerId, newApp.getClientId(), realm.getId()) == null) {
                CibaDeadLetterHookConfigModel cibaDeadLetterHookConfigModel = new CibaDeadLetterHookConfigModel();
                cibaDeadLetterHookConfigModel.setCibaResultHookProviderId(providerId);
                cibaDeadLetterHookConfigModel.setConfig(config);
                cibaDeadLetterHookConfigModel.setClientId(newApp.getClientId());
                cibaDeadLetterHookConfigModel.setRealmId(realm.getId());
                realm.addCibaResultHookNotificationConfig(cibaDeadLetterHookConfigModel);
            }
        }
    }

    public static void setUpAppTypes(ClientModel resource, ClientRepresentation rep) {
        if (rep.getAppType() != null) {
            switch (rep.getAppType()) {
                case HIGH_SECURE:
                    resource.setStandardFlowEnabled(true);
                    resource.setImplicitFlowEnabled(false);
                    resource.setDirectAccessGrantsEnabled(false);
                    resource.setServiceAccountsEnabled(false);
                    resource.setBearerOnly(false);
                    break;
                case org.keycloak.models.Constants.REST_API:
                    resource.setDirectAccessGrantsEnabled(true);
                    resource.setStandardFlowEnabled(false);
                    resource.setImplicitFlowEnabled(false);
                    resource.setServiceAccountsEnabled(false);
                    resource.setBearerOnly(false);
                    break;
                case org.keycloak.models.Constants.M2M:
                    resource.setServiceAccountsEnabled(true);
                    resource.setDirectAccessGrantsEnabled(false);
                    resource.setStandardFlowEnabled(false);
                    resource.setImplicitFlowEnabled(false);
                    resource.setPublicClient(false);
                    resource.setBearerOnly(false);
                    break;
                case org.keycloak.models.Constants.BROWSER_BASED:
                case org.keycloak.models.Constants.IWS:
                    resource.setImplicitFlowEnabled(true);
                    resource.setStandardFlowEnabled(false);
                    resource.setDirectAccessGrantsEnabled(false);
                    resource.setServiceAccountsEnabled(false);
                    resource.setBearerOnly(false);
                    break;
                case org.keycloak.models.Constants.CIBA:
                    resource.setStandardFlowEnabled(true);
                    resource.setImplicitFlowEnabled(false);
                    resource.setDirectAccessGrantsEnabled(false);
                    resource.setServiceAccountsEnabled(false);
                    resource.setBearerOnly(false);
                    resource.setAttribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, "true");
                    resource.setAttribute(REQUIRE_PUSHED_AUTHORIZATION_REQUESTS, "false");
                    resource.setPublicClient(false);
                    break;
            }
        }
    }

    public static Properties getProperties(KeycloakSession session, RealmModel realm, UserModel user) {
        return AdminRoot.getMessages(session, realm, session.getContext().resolveLocale(user, LocaleType.ADMIN).toLanguageTag(), "admin-messages");
    }

    public static void updateTermsAndConditions(KeycloakSession session, RealmModel realm, RequiredActionProviderRepresentation rep, String alias, AdminEventBuilder adminEvent) {

        if (rep.getConfig().containsKey("terms_and_condition_text") && rep.getConfig().get("terms_and_condition_text").length() > 3000) {
            throw new BadRequestException("terms-and-condition-text-too-large-error");
        }
        List<TermsAndConditionsModel> termsAndConditions = realm.getTermsAndConditionsVersions(alias);
        Long updatedAt = System.currentTimeMillis();
        String version = rep.getConfig().get(TERMS_AND_CONDITONS_VERSION);
        String id = version + updatedAt;
        String content = "";
        List<String> availableVersions = new ArrayList<>();
        termsAndConditions.stream().forEach(availableVersion -> {
            availableVersions.add(availableVersion.getVersion());
        });
        if (availableVersions.stream().filter(x -> x.equals(rep.getConfig().get(TERMS_AND_CONDITONS_VERSION))).count() == 0) {
            if (rep.getConfig().get(TERMS_AND_CONDITIONS_TYPE).equals("PDF")) {
                isValidContent(rep.getConfig().get("terms_and_condition_pdf"), "PDF", adminEvent, rep);

                String[] pdf = rep.getConfig().get("terms_and_condition_pdf").split(":");
                String pdfName = pdf[0];
                byte[] decodedBytes = Base64.getDecoder().decode(pdf[1]);
                try {
                    File tempFile = createMediaTempFile(new ByteArrayInputStream(decodedBytes));
                    content = putObjectToMinioForTermsAndConditions(tempFile, "pdf", pdfName + "+" + updatedAt, version, session.getContext(), alias);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                }
            } else if (rep.getConfig().get(TERMS_AND_CONDITIONS_TYPE).equals("Image")) {

                isValidContent(rep.getConfig().get("terms_and_condition_image"), "Image", adminEvent, rep);

                try {
                    PDDocument document = new PDDocument();

                    List<String> imageList = Arrays.asList(rep.getConfig().get("terms_and_condition_image").split(","));

                    // Define initial coordinates for the first image
                    float x = 30; // Initial x-coordinate
                    float y = PDRectangle.A4.getHeight() - 30; // Initial y-coordinate

                    PDPage currentPage = null;

                    for (String encodedImageString : imageList) {
                        // Decode the base64 string into bytes
                        byte[] decodedBytes = Base64.getDecoder().decode(encodedImageString);

                        // Load the decoded bytes into a BufferedImage
                        ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes);
                        BufferedImage bufferedImage = ImageIO.read(bais);

                        // Ensure BufferedImage is not null
                        if (bufferedImage == null) {
                            throw new IOException("BufferedImage is null. Unable to decode image.");
                        }

                        // Convert BufferedImage to PDImageXObject
                        PDImageXObject pdImageXObject = PDImageXObject.createFromByteArray(document, decodedBytes, null);

                        float originalWidth = pdImageXObject.getWidth();
                        float originalHeight = pdImageXObject.getHeight();
                        float maxWidth = PDRectangle.A4.getWidth() - 60; // 30 margin on each side
                        float maxHeight = PDRectangle.A4.getHeight() - 60; // 30 margin on top and bottom

                        float scaledWidth = originalWidth;
                        float scaledHeight = originalHeight;

                        // Scale image if it exceeds the maximum dimensions
                        if (originalWidth > maxWidth || originalHeight > maxHeight) {
                            float scale = Math.min(maxWidth / originalWidth, maxHeight / originalHeight);
                            scaledWidth = originalWidth * scale;
                            scaledHeight = originalHeight * scale;
                        }

                        // Add a new page for each image
                        currentPage = new PDPage(PDRectangle.A4);
                        document.addPage(currentPage);

                        // Calculate position to center the image on the page
                        x = (PDRectangle.A4.getWidth() - scaledWidth) / 2;
                        y = (PDRectangle.A4.getHeight() - scaledHeight) / 2;

                        // Add the image to the PDF document
                        try (PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true)) {
                            contentStream.drawImage(pdImageXObject, x, y, scaledWidth, scaledHeight);
                        }

                    }

                    File tempFile = File.createTempFile("terms_and_conditions", ".pdf");
                    document.save(tempFile);
                    document.close();
                    content = putObjectToMinioForTermsAndConditions(tempFile, "pdf", version + "+" + updatedAt, version, session.getContext(), alias);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                    adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).representation(rep).error("terms-and-condition-image-process-error-desc").success();
                    throw new BadRequestException("terms-and-condition-image-process-error");
                }

            } else {
                if (rep.getConfig().get(TERMS_AND_CONDITIONS_TYPE).equals("Link")) {
                    isValidContent(rep.getConfig().get("terms_and_condition_link"), "Link", adminEvent, rep);

                    content = rep.getConfig().get("terms_and_condition_link");
                    if (!content.startsWith("https://")) {
                        adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).representation(rep).error("Invalid Link").success();
                        throw new BadRequestException(INVALID_CONFIG_INPUT);
                    }

                    String host = rep.getConfig().get("terms_and_condition_link");
                    Map<String, String> browserHeaders = session.getContext().getRealm().getBrowserSecurityHeaders();
                    Map<String, String> headers = new HashMap<>();
                    headers.putAll(browserHeaders);
                    String csp = headers.get("contentSecurityPolicy");
                    List<String> cspSplit = Arrays.stream(csp.split(";")).collect(Collectors.toList());
                    for (int x = 0; x < cspSplit.size(); x++) {
                        if (cspSplit.get(x).trim().startsWith("frame-src")) {
                            String frame = cspSplit.get(x);
                            if (!frame.contains(host)) {
                                frame += " " + host;
                            }
                            cspSplit.set(x, frame);
                        }
                    }
                    csp = String.join(";", cspSplit);
                    headers.put("contentSecurityPolicy", csp);
                    session.getContext().getRealm().setBrowserSecurityHeaders(headers);

                } else {
                    isValidContent(rep.getConfig().get("terms_and_condition_text"), "Text", adminEvent, rep);
                    content = rep.getConfig().get("terms_and_condition_text");
                }
            }

            if (version != null && !version.isEmpty() && version.split("\\.").length != 3 || !isLatestVersion(version, availableVersions)) {
                adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).representation(rep).error("terms-and-condition-invalid-version").success();
                throw new BadRequestException("terms-and-condition-invalid-version");
            }

            if (availableVersions.stream().filter(x -> x.equals(rep.getConfig().get(TERMS_AND_CONDITONS_VERSION))).count() == 0) {
                realm.addTermsAndConditions(rep.getId(), session.getContext().getRealm().getId(), id, version, rep.getConfig().get(TERMS_AND_CONDITIONS_TYPE), content);
            }
        }
    }

    public static String updateCustomMessageLogo(KeycloakSession session, RequiredActionProviderRepresentation rep, String alias) {
        try {
            File tempFile = createTempFileForCustomMessage(rep.getConfig().get("custom-message-logo"));
            return putObjectToMinioForCustomMessage(tempFile, session.getContext(), alias);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File createTempFileForCustomMessage(String fileContent) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(fileContent.split(":")[1]);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        File tempFile = File.createTempFile("temp", ".png");
        ImageIO.write(image, "png", tempFile);
        return tempFile;
    }

    public static String putObjectToMinioForCustomMessage(File file, KeycloakContext context, String id) {
        String bucketName = context.getRealm().getName();
        String objectName = "custom_message" + "/" + id + "/" + id + "." + "png";

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();

        try {
            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new FileInputStream(file), file.length(), -1)
                            .contentType("application/octet-stream")
                            .build());

            return objectName;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            log.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static void isValidContent(String content, String fieldName, AdminEventBuilder adminEvent, RequiredActionProviderRepresentation rep) {
        if (content == null || content.isEmpty()) {
            adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).representation(rep).error(fieldName + " must not be empty").success();
            throw new BadRequestException("terms-condn-content-not-found");
        }
    }

    public static File createMediaTempFile(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("temp", ".pdf");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.write(inputStream.readAllBytes(), out);
        }
        return tempFile;
    }

    public static boolean isLatestVersion(String availableVersion, List<String> versionsList) {
        for (String version : versionsList) {
            if (compareVersions(availableVersion, version) < 0) {
                return false; // The given version is not the latest
            }
        }
        return true; // The given version is the latest
    }


    public static int compareVersions(String version1, String version2) {
        // Remove any non-digit characters from the beginning of the version strings
        version1 = version1.replaceAll("[^\\d.]", "");
        version2 = version2.replaceAll("[^\\d.]", "");

        String[] v1 = version1.split("\\.");
        String[] v2 = version2.split("\\.");

        for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
            int num1 = Integer.parseInt(v1[i]);
            int num2 = Integer.parseInt(v2[i]);
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return Integer.compare(v1.length, v2.length);
    }

    public static String extractBase64String(String base64Url) {
        // Split the input string by the "base64," delimiter
        String[] parts = base64Url.split("base64,");

        // Check if the split was successful and return the Base64 part
        if (parts.length == 2) {
            return parts[1];
        } else {
            throw new IllegalArgumentException("Invalid Base64 URL format");
        }
    }

    public static String extractFileNameFromBase64String(String base64Url) {
        String[] parts = base64Url.split(";");
        for (String part : parts) {
            if (part.startsWith("name=")) {
                // Extract the file name by removing the 'name=' prefix
                return part.substring(5);  // "name=" has 5 characters
            }
        }
        return null; // Return null if no file name found
    }

    public static String getDefaultLogoUrl() {
        return EnvironmentReader.getValue("MINIO_PUBLIC_URL") + "/marketplace/ezto-vrfy/workflow-template/deafult-template-logo.svg";
    }


    public static String detectFileType(byte[] fileBytes) {
        String content = new String(fileBytes, StandardCharsets.UTF_8);

        if (content.trim().contains("<svg")) {
            return ".svg";
        }

        InputStream is = new ByteArrayInputStream(fileBytes);
        BufferedImage image = null;
        try {
            image = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (image != null) {
            return ".png";
        }

        throw new IllegalArgumentException("Unsupported image format");
    }

    public static String uploadToMinIO(String bucketName, String objectName, byte[] fileData) {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(DEFAULT_SERVER_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY)
                .build();

        String contentType = "application/octet-stream";
        if (objectName.endsWith("svg")) {
            contentType = "image/svg+xml";
        } else if (objectName.endsWith("png")) {
            contentType = "image/png";
        }

        try {
            // Convert byte array to InputStream
            InputStream stream = new ByteArrayInputStream(fileData);

            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Upload the file
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, fileData.length, -1)  // Stream length and content type
                            .contentType(contentType)
                            .build()
            );

            // Return the file URL or path
            return DEFAULT_MINIO_SERVER_API_URL + "/" + bucketName + "/" + objectName;

        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            log.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static String putObjectToMinioForTermsAndConditions(File file, String media, String fileName, String version, KeycloakContext context, String id) {
        String bucketName = context.getRealm().getName();
        String objectName = "terms_and_conditions" + "/" + id + "/" + version + "/" + fileName + "." + media;

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_MINIO_SERVER_API_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();

        try {
            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new FileInputStream(file), file.length(), -1)
                            .contentType("application/pdf")
                            .build());

            return objectName;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            log.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static String getPresignedObjectUrl(KeycloakContext context, String objectName) {
        String bucketName = context.getRealm().getName();

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_MINIO_SERVER_API_URL)
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
            log.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static void removeSecurityHeader(RealmModel realm, KeycloakSession session, TermsAndConditionsModel termsAndConditionsVersion) {
        if (termsAndConditionsVersion.getType().equals("Link")) {
            List<TermsAndConditionsModel> termsAndConditions = realm.getTermsAndConditions(realm.getId());
            if (termsAndConditions.stream().filter(x -> x.getType().equals("Link"))
                    .filter(x -> x.getContent().equals(termsAndConditionsVersion.getContent())).count() == 1) {
                String host = termsAndConditionsVersion.getContent();
                Map<String, String> browserHeaders = session.getContext().getRealm().getBrowserSecurityHeaders();
                Map<String, String> headers = new HashMap<>();
                headers.putAll(browserHeaders);
                String csp = headers.get("contentSecurityPolicy");
                List<String> cspSplit = Arrays.stream(csp.split(";")).collect(Collectors.toList());
                for (int x = 0; x < cspSplit.size(); x++) {
                    if (cspSplit.get(x).trim().startsWith("frame-src")) {
                        String frame = cspSplit.get(x);
                        if (frame.contains(host)) {
                            frame = frame.replace(host, "");
                        }
                        cspSplit.set(x, frame);
                    }
                }
                csp = String.join(";", cspSplit);
                headers.put("contentSecurityPolicy", csp);
                session.getContext().getRealm().setBrowserSecurityHeaders(headers);
            }
        }
    }


    public static boolean updateApplicationRoleName(KeycloakSession session, RealmModel realm, ClientModel client, ClientRepresentation rep) {
        if (rep.getName().equals(client.getName())) {
            return true;
        }

        AtomicBoolean hasError = new AtomicBoolean(false);
        String currentClientName = client.getName().replace(" ", "_").toLowerCase();
        String newClientId = rep.getName().replace(" ", "_").toLowerCase();

        realm.getRolesStream()
                .filter(role -> role.getName().contains(HYPEN + currentClientName + HYPEN) || role.getName().contains(HYPEN + currentClientName.toLowerCase() + HYPEN))
                .forEach(x -> {
                    RoleModel role = x;
                    String roleName = role.getName();
                    String appName;
                    String roleType = roleName.split("-")[0] + "-";
                    String partialRolename = roleName.replaceFirst(roleType, "");
                    try {
                        appName = partialRolename.split(client.getName().toLowerCase().replace(" ", "_"))[1];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        log.error("Role name cannot be updated - ArrayIndexOutOfBoundException");
                        hasError.set(true);
                        return;
                    }
                    role.setName(roleType + newClientId.toLowerCase() + appName);
                });
        return !hasError.get();
    }

    public static Stream<ClientModel> getAppBySandboxMode(KeycloakSession session, Stream<ClientModel> clientModels, String sandboxMode) {
        boolean isSandboxMode = Boolean.parseBoolean(sandboxMode);
        return clientModels.filter(clientModel -> ModelToRepresentation.toRepresentation(clientModel, session).getSandboxMode() == isSandboxMode);
    }

    public static boolean isOnboardingStepRequired(OnboardingRepresentation steps) {
        return steps.getSteps().size() == 1 && steps.getCurrentStep() >= 0 &&
                "ob-admin-workspace-setup".equals(steps.getSteps().get(steps.getCurrentStep()).getStepProviderId());
    }

    public static String uploadImageToMinio(String encodedImage, String productName, String bucketName) {
        try {
            File tempFile = createTempFile(encodedImage);
            return putObjectToMinioForProducts(tempFile, "photo", productName, bucketName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static File createTempFile(String fileContent) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(fileContent);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        File tempFile = File.createTempFile("temp", ".png");
        ImageIO.write(image, "png", tempFile);
        return tempFile;
    }

    public static String putObjectToMinioForProducts(File file, String media, String productName, String bucketName) {
        String objectName = "products" + "/" + productName + "/" + productName;

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();

        try {
            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new FileInputStream(file), file.length(), -1)
                            .contentType("application/octet-stream")
                            .build());

            return objectName;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            logger.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static List<String> getStripeIdByActionId(RealmModel realm, KeycloakSession session, String actionId) {
        List<String> requiredMappingIds = new ArrayList<>();
        realm.getRequiredActionProvidersStream(actionId).forEach(x -> {
            RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, x.getProviderId());
            Collections.addAll(requiredMappingIds, factory.stripeMappingId());
        });
        return requiredMappingIds;
    }

    public static String formatCostDetailsForTemplate(String cost, String walletCurrency) {
        Currency currency = Currency.getInstance(walletCurrency);
        String currencySymbol = currency.getSymbol(Locale.ENGLISH);
        return currencySymbol + cost;
    }

    public static void deleteTemplateAction(RealmModel realm, String actionId) {
        ActionModel actionModel = realm.getActionById(actionId);
        if (actionModel == null)
            return;
        realm.removeAction(actionModel);
    }

    public static Map<String, Object> addCustomSupportMessageDetails(RequiredActionContext context, RealmModel realm, Map<String, Object> attributes) {
        if (realm == null && context == null) {
            return attributes;
        }
        if (realm == null && context != null) {
            realm = context.getRealm();
        }
        if (Boolean.parseBoolean(realm.getAttribute(SUPPORT_CUSTOM_TOGGLE)) && StringUtil.isNotBlank(realm.getAttribute(SUPPORT_CUSTOM_MESSAGE))) {
            String customMessage = realm.getAttribute(SUPPORT_CUSTOM_MESSAGE);
            if ((customMessage.split("\\{").length) > 2)
                return attributes;

            int startIndex = customMessage.indexOf("{");
            int endIndex = customMessage.indexOf("}");
            if (endIndex < startIndex)
                return attributes;

            if (startIndex != -1 && endIndex != -1) {
                String supportCustomMessageLink = customMessage.substring(startIndex + 1, endIndex);
                customMessage = customMessage.replace("{" + supportCustomMessageLink + "}", "<a href=\"{0}\"> {1} </a>");
                attributes.put("supportCustomMessage", customMessage);
                attributes.put("supportCustomMessageLink", supportCustomMessageLink);
            } else {
                attributes.put("supportCustomMessage", customMessage);
            }
        }
        return attributes;
    }

    public static String getProviderName(KeycloakSession session, String verifierName) throws AccessControlException {
        String realmId = session.getContext().getRealm().getId();
        APIPriorityModel verifierAPIDetails;

        if (!session.getContext().getRealm().getVerifierProviderDetails(realmId, null, null, "", null, "ASC").isEmpty()) {
            verifierAPIDetails = session.getContext().getRealm().getVerifierAPIDetails(verifierName, realmId);
            if (verifierAPIDetails == null || !verifierAPIDetails.getIsEnabled()) {
                throw new AccessControlException("Permission Denied");
            }
            return verifierAPIDetails.getSelectedProvider();
        }
        return "";
    }

    public static Map<String, Object> getPricingDetails(KeycloakSession session, String providerName, String stripeId) {
        Map<String, Object> pricingDetails = new HashMap<>();
        pricingDetails.put("currency", "USD");
        pricingDetails.put("amount", "0.00");
        return pricingDetails;
    }

    public static String getStripeIdWithProvider(KeycloakSession session, RealmModel realm, Optional<String> stripeId, String[] apiMappingName) {
        if (apiMappingName == null) {
            return "surepass##" + stripeId.get();
        }
        APIPriorityModel apiPriorityModel = realm.getVerifierAPIDetails(apiMappingName[0], realm.getId());
        if (apiPriorityModel == null) {
            RealmModel masterRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
            apiPriorityModel = realm.getVerifierAPIDetails(apiMappingName[0], masterRealm.getId());
            if (apiPriorityModel == null)
                return "";
        }
        String selectedProvider = apiPriorityModel.getSelectedProvider();
        return selectedProvider + "##" + stripeId.get();
    }

    public static File createTempFileWithType(InputStream inputStream, String ext) {
        try {
            File file = File.createTempFile("temp_" + UUID.randomUUID(), "." + ext);
            OutputStream outputStream = new FileOutputStream(file);
            org.apache.pdfbox.io.IOUtils.copy(inputStream, outputStream);
            return file;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
