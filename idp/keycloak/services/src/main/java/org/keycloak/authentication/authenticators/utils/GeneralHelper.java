package org.keycloak.authentication.authenticators.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.*;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.keycloak.Config.getAdminRealm;
import static org.keycloak.models.Constants.VERIFICATION_EMAIL_CODE;
import static org.keycloak.services.resources.admin.RealmsAdminResource.getEmailTemplate;

public class GeneralHelper {
    private static final Logger logger = Logger.getLogger(GeneralHelper.class);
    private static final Log log = LogFactory.getLog(GeneralHelper.class);
    private static GeneralHelper generalHelper;
    private static int year = Calendar.getInstance().get(Calendar.YEAR);

    public static GeneralHelper getInstance() {
        if (generalHelper == null) {
            generalHelper = new GeneralHelper();
        }
        return generalHelper;
    }

    //Function used to validate email address
    public static boolean isValidEmail(String email) {
        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,63}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    //Function used to mask email address
    public static String maskEmailAddress(String strEmail) throws Exception {

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

    public static String maskString(String strText, int start, int end, char maskChar) throws Exception {

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

    //Function used to get last n digits of phone number
    public static String getLastDigitsOfNumber(String number, int digits) {
        String slicedString = "";

        int digitsToSlice = 0;
        if (digits < 1) {
            digitsToSlice = 3;
        } else {
            digitsToSlice = digits;
        }

        if (number != null && number.length() > 0) {
            slicedString = "\u2022\u2022\u2022\u2022" + number.substring(number.length() - digitsToSlice);
        }

        return slicedString;
    }

    //Function used to get last n digits of email
    public static String getLastDigitsOfEmail(String email, int digits) {
        String[] parts = email.split("@");
        String slicedString = "";

        int digitsToSlice = 0;
        if (digits < 1) {
            digitsToSlice = 3;
        } else {
            digitsToSlice = digits;
        }

        if (parts[0] != null && parts[0].length() > 0) {
            slicedString = "\u2022\u2022\u2022\u2022" + (parts[0].substring(parts[0].length() - digitsToSlice)) + "@" + parts[1];
        }

        return slicedString;
    }

    //To send the Email OTP
    public static boolean sendEmailOTP(AuthenticationFlowContext context, String email, String emailProviderId) {
        String verify_code_size = "6";
        String expiryDuration = "5m";

        if (emailProviderId == null) {
            emailProviderId = "default";
        }

        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String code = GeneralHelper.getOtp(verifyCodeSize);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("expiration", getTTLMessage(expiryDuration));
        attributes.put("year", String.valueOf(year));
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
            logger.error(e.getMessage());
            return false;
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
        }

        context.getAuthenticationSession().setAuthNote("email_otp", code);
        context.getAuthenticationSession().setAuthNote("email_otp_expiry", String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    public static String getPhoneNumber(UserModel model, SmsSenderProvider smsProvider) throws SmsException {

        if (model.getMobileNumber() == null || model.getMobileNumberRegion() == null)
            return "";

        if (smsProvider == null)
            return "";

        NumberFormat numberFormat = smsProvider.parseNumber(model.getMobileNumber(), model.getMobileNumberRegion().toUpperCase());
        return numberFormat.getE16Format();
    }

    //To Send the Mobile OTP
    public static boolean sendSmsOtp(AuthenticationFlowContext context, String smsSenderProviderId) {
        String verify_code_size = "6";
        String expiryDuration = "5m";
        String message = "Your OTP is {code}";

        int verifyCodeSize = Integer.parseInt(verify_code_size);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String generatedOTP = getOtp(verifyCodeSize);
        message = message.replace("{code}", generatedOTP);
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(context.getUser().getMobileNumber(), context.getUser().getMobileNumberRegion().toUpperCase());
            smsSenderProvider.sendSMS(numberFormat.getE16Format(), message, null);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            return false;
        }
        context.getAuthenticationSession().setAuthNote("mobile_otp", generatedOTP);
        context.getAuthenticationSession().setAuthNote("mobile_otp_expiry", String.valueOf(getExpiryDuration(expiryDuration)));
        return true;
    }

    //Function used to mask given Phone Number
    public static String maskPhoneNumber(AuthenticationFlowContext context, String phoneNumber, String region) throws NullPointerException {
        SmsSenderProvider smsSenderProvider;
        NumberFormat numberFormat = null;
        String phoneNumberRaw = null;
        String countryCode = null;
        smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");

        try {
            numberFormat = smsSenderProvider.parseNumber(phoneNumber, region);
        } catch (SmsException e) {
            e.printStackTrace();
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

    public static String maskPhoneNumber(RequiredActionContext context, String phoneNumber, String region) throws NullPointerException {
        SmsSenderProvider smsSenderProvider;
        NumberFormat numberFormat = null;
        String phoneNumberRaw = null;
        String countryCode = null;
        smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");

        try {
            numberFormat = smsSenderProvider.parseNumber(phoneNumber, region);
        } catch (SmsException e) {
            e.printStackTrace();
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

    //Generating the OTP of given length
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
        return new String(otp);
    }


    public static Long getExpiryDurationInSec(String ttl) {
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
        return expiry / 1000;
    }

    public static Long getExpiryDurationInRawSec(String ttl) {
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
        long expiry = 120;

        if (timeUnit.equals("d")) {
            expiry = TimeUnit.DAYS.toSeconds(duration);
        } else if (timeUnit.equals("h")) {
            expiry = TimeUnit.HOURS.toSeconds(duration);
        } else if (timeUnit.equals("s")) {
            expiry = duration;
        }
        return expiry;
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

    public static String getTTLMessage(String ttl) {
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
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    //Error Page Redirect Method
    public static void redirectToErrorPage(AuthenticationFlowContext context, String message) {
        Response challenge = context.form()
                .setAttribute("errorMessage", message)
                .createForm("error.ftl");
        context.challenge(challenge);
    }

    //Error Page Redirect Method
    public static void redirectToErrorPage(RequiredActionContext context, String message) {
        Response challenge = context.form()
                .setAttribute("errorMessage", message)
                .createForm("error.ftl");
        context.challenge(challenge);
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

    public static void auditEvent(RequiredActionContext context, EventType eventType, String error) {
        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.client(context.getAuthenticationSession().getClient().getClientId());
        if (context.getUser() != null) {
            eventBuilder.user(context.getUser());
        } else {
            eventBuilder.user("UNKNOWN_USER");
        }
        eventBuilder.event(eventType);
        if (error.equals("")) {
            eventBuilder.success();
        } else {
            eventBuilder.error(error);
        }
    }

    public static boolean isMultipleLoginOptionPassword(AuthenticationExecutionModel model, AuthenticationFlowContext context) {
        return model.getAuthenticator().equalsIgnoreCase("multiple-login-options-authenticator") &&
                context.getHttpRequest() != null &&
                context.getHttpRequest().getDecodedFormParameters() != null &&
                context.getHttpRequest().getDecodedFormParameters().getFirst("chosenType") != null &&
                context.getHttpRequest().getDecodedFormParameters().getFirst("chosenType").equalsIgnoreCase("passwordLoginOption");
    }

    public static boolean isAdminUser(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        UserModel user = null;

        MultivaluedMap<String, String> formParameters = context.getHttpRequest().getDecodedFormParameters();
        if (formParameters != null) {
            if (formParameters.containsKey("email")) {
                user = session.users().getUserByEmail(context.getRealm(), formParameters.getFirst("email"));
            } else if (formParameters.containsKey("phoneNumber")) {
                String phone = formParameters.getFirst("phoneNumber");
                if (phone != null && phone.length() >= 10) {
                    phone = phone.substring(phone.length() - 10);
                    String phoneNumberRegion = formParameters.getFirst("phoneNumberRegion");
                    if (phoneNumberRegion != null) {
                        user = session.users().getUserByMobile(phoneNumberRegion + phone, context.getRealm());
                    }
                }
            }
        }

        return user != null &&
                user.getRoleMappingsStream().anyMatch(x ->
                        x.getName().equalsIgnoreCase(AdminRoles.REALM_ADMIN) || x.getName().equalsIgnoreCase(AdminRoles.ADMIN));
    }

    public static boolean isWalletBalanceAvailable(AuthenticationFlowContext context) {
        return true;
    }

    public static void checkBlacklistedConfig(AuthenticationFlowContext context) {
        EventBuilder event = context.getEvent();
        Map<String, String> eventConfig = context.getEvent().getEvent().getDetails();

        MultivaluedMap<String, String> formParameters = context.getHttpRequest().getDecodedFormParameters();
        if (formParameters != null) {
            if (formParameters.containsKey("email")) {
                eventConfig.put("bl_email", formParameters.getFirst("email"));
            } else if (formParameters.containsKey("phoneNumber")) {
                String phone = formParameters.getFirst("phoneNumber");
                if (phone != null && phone.length() >= 10) {
                    phone = phone.substring(phone.length() - 10);
                    eventConfig.put("bl_phone_region", formParameters.getFirst("phoneNumberRegion"));
                    eventConfig.put("bl_phone", phone);
                }
            } else if (formParameters.containsKey("username")) {
                eventConfig.put("bl_username", formParameters.getFirst("username"));
            }
        }
        event.getEvent().setDetails(eventConfig);
        event.success();
    }

}
