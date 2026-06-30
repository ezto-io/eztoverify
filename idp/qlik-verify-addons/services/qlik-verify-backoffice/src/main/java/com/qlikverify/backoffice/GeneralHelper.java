package com.qlikverify.backoffice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.qlikverify.backoffice.models.AndroidAssetLink;
import com.webauthn4j.data.client.Origin;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.ActionTokenConstants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.component.ComponentModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.*;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.*;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AESHelper;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.services.constants.Constants.SOCKET_URL;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.keycloak.Config.getAdminRealm;
import static org.keycloak.events.Errors.EMAIL_PROVIDER_NOT_FOUND;
import static org.keycloak.events.EventType.EZ_TRANSACTION_REMOVE_USER;
import static org.keycloak.models.Constants.VERIFICATION_EMAIL_CODE;
import static org.keycloak.protocol.oidc.grants.ciba.resolvers.DefaultCIBALoginUserResolver.WORKFLOW;
import static org.keycloak.services.resources.admin.RealmsAdminResource.getEmailTemplate;


public class GeneralHelper {
    private static final Logger logger = Logger.getLogger(GeneralHelper.class);
    private static boolean isTestMode = false;
    private static int year = Calendar.getInstance().get(Calendar.YEAR);
    private static final String REASON = "reason";

    private static final String ONBOARDING_FAILED = "Onboarding Failed";

    private GeneralHelper() {
        throw new UnsupportedOperationException("GeneralHelper cannot be instantiated");
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

    public static String getOtp(int len) {

        // Using numeric values
        String numbers = "0123456789";

        // Using random method
        SecureRandom randomMethod = new SecureRandom();

        char[] otp = new char[len];

        for (int i = 0; i < len; i++) {
            // Use of charAt() method : to get character value
            // Use of nextInt() as it is scanning the value as int
            otp[i] = numbers.charAt(randomMethod.nextInt(numbers.length()));
        }
        return String.valueOf(otp);
    }

    public static String getSmsText(RequiredActionContext context, String configName) {

        if (context.getConfig() == null)
            return DEFAULT_MESSAGE_TXT;
        String configValue = context.getConfig().get(configName);
        if (configValue == null || configValue.length() == 0)
            return DEFAULT_MESSAGE_TXT;

        return configValue;
    }

    public static NumberFormat parseMobileNumber(RequiredActionContext context, String region, String number) {
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(number, region.toUpperCase());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
        return numberFormat;
    }

    public static boolean sendEmailOTP(RequiredActionContext context, String email, String expiryDuration) {
        String codeSize = getConfigValue(context, EMAIL_VER_OTP_LENGTH).equals("") ?
                "6" : getConfigValue(context, EMAIL_VER_OTP_LENGTH);
        int verifyCodeSize = Integer.parseInt(codeSize);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String code = GeneralHelper.getOtp(verifyCodeSize);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("tenant", context.getRealm().getName());
        attributes.put("expirationTime", getTTLMessage(expiryDuration).get("time"));
        attributes.put("expirationUnit", getTTLMessage(expiryDuration).get("unit"));
        attributes.put("year", String.valueOf(year));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP, "123456");
            context.getAuthenticationSession().setAuthNote(SESSION_EMAIL_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }

        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
            if (emailProviderId.isEmpty()) {
                logger.error("Email Provider is not set for the authenticator");
                auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, EMAIL_PROVIDER_NOT_FOUND);
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

    public static boolean isValidEmail(String email) {
        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,63}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static boolean sendSmsOtp(RequiredActionContext context, String phoneNumber, String expiryDuration, String smsSenderProviderId) {
        String codeSize = getConfigValue(context, PHONE_VER_OTP_LENGTH).equals("") ?
                "6" : getConfigValue(context, PHONE_VER_OTP_LENGTH);


        String message = getSmsText(context, PHONE_VER_OTP_TXT);

        if (isTestMode) {
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, "1234");
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryDuration)));
            return true;
        }
        int verifyCodeSize = Integer.parseInt(codeSize);

        //if length is equal to zero,we reset to default value.
        if (verifyCodeSize <= 0) {
            verifyCodeSize = 6; // setting default value
        }
        String generatedOTP = getOtp(verifyCodeSize);
        message = String.format(message.replace("{code}", generatedOTP).replace("\\n", "%n"));

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

    public static Map<String, Object> getTTLMessage(String ttl) {
        boolean validate = validateTTL(ttl);
        Map<String, Object> timeDuration = new HashMap<>();
        ttl = !validate ? "1h" : ttl;
        String timeUnit = "h";
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            timeUnit = ttl.substring(ttl.length() - 1);
            ttl = ttl.substring(0, ttl.length() - 1);
        }
        long duration = Long.parseLong(ttl);

        timeDuration.put("time", duration == 1 ? 1 : duration);
        switch (timeUnit) {
            case "d":
                timeDuration.put("unit", duration == 1 ? "day" : "days");
                break;
            case "h":
                timeDuration.put("unit", duration == 1 ? "hour" : "hours");
                break;
            case "m":
                timeDuration.put("unit", duration == 1 ? "minute" : "minutes");
                break;
            case "s":
                timeDuration.put("unit", duration == 1 ? "second" : "seconds");
                break;
            default:
                timeDuration.put("unit", "hour");
                break;
        }
        return timeDuration;
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

            return phoneUtil.isValidNumber(parsed);

        } catch (NumberParseException e) {
            return false;
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

    public static String phoneNoWithCountryCode(UserModel user) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber parsed;
        if (phoneNumberHasCode(user)) {
            return user.getMobileNumber();
        } else {
            try {
                parsed = phoneUtil.parse(user.getMobileNumber(), user.getMobileNumberRegion().toUpperCase());
            } catch (NumberParseException e) {
                throw new RuntimeException(e);
            }
        }
        return parsed.getCountryCode() + user.getMobileNumber();
    }

    public static Map<String, String> parseMobileNumber(String phoneNumberWithCountryCode) {
        Map<String, String> phoneNoDetails = new HashMap<>();
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phoneNumber;
        phoneNumberWithCountryCode = phoneNumberWithCountryCode.contains("+") ? phoneNumberWithCountryCode : "+" + phoneNumberWithCountryCode;
        try {
            phoneNumber = phoneNumberUtil.parse(phoneNumberWithCountryCode, null);
        } catch (NumberParseException e) {
            throw new RuntimeException(e);
        }
        phoneNoDetails.put("phoneNo", String.valueOf(phoneNumber.getNationalNumber()));
        phoneNoDetails.put("phoneNoReg", phoneNumberUtil.getRegionCodeForNumber(phoneNumber).toLowerCase());
        return phoneNoDetails;
    }

    public static EventBuilder getEvent(RequiredActionContext context, EventType eventType) {
        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.client(context.getAuthenticationSession().getClient().getClientId());
        if (context.getUser() != null) {
            eventBuilder.user(context.getUser());
        } else {
            eventBuilder.user("UNKNOWN_USER");
        }
        eventBuilder.event(eventType);
        return eventBuilder;
    }

    public static void auditEvent(RequiredActionContext context, EventType eventType, String error) {
        EventBuilder eventBuilder = getEvent(context, eventType);
        eventBuilder.detail("requestId", context.getSession().getContext().getAuthenticationSession().getAuthNote("request"));
        if (error == null || error.isEmpty()) {
            eventBuilder.success();
        } else {
            eventBuilder.error(error);
        }
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
                .success();
    }

    public static boolean isValidUrl(List<String> hostUrls) {
        return hostUrls.stream().filter(url -> {
            try {
                new URL(url).toURI();
                return false;
            } catch (URISyntaxException | MalformedURLException e) {
                return true;
            }
        }).count() > 0;

    }

    public static Set<Origin> getOriginForAndroid(RealmModel realmModel) {
        Set<Origin> origins = new HashSet<>(Collections.emptySet());
        String attribute = realmModel.getAttribute("fido-asset-links-android");
        if (attribute != null) {
            try {
                List<AndroidAssetLink> assetsList = Arrays.asList(JsonSerialization.mapper.readValue(attribute,
                        AndroidAssetLink[].class));
                List<AndroidAssetLink> androidAssets = assetsList.stream().filter(x -> x.getTarget().getNamespace().equalsIgnoreCase("android_app")).collect(Collectors.toList());
                androidAssets.forEach(asset ->
                        asset.getTarget().getSha256CERTFingerprints().forEach(hash -> {
                            logger.info("hash: " + hash);
                            String calculatedHash = "android:apk-key-hash:" + calculateAndroidHash(hash);
                            logger.info("calculatedHash: " + calculatedHash);
                            origins.add(new Origin(calculatedHash));
                        }));
            } catch (IOException e) {
                logger.error(e.getMessage());
                return origins;
            }
        }
        return origins;
    }

    public static String calculateAndroidHash(String hash) {
        final String sha256 = hash.replace(":", "");
        byte[] byteArrayFromHex = getByteArrayFromHex(sha256);
        String androidHash = Base64Url.encode(byteArrayFromHex).replace("=", "");
        logger.info(androidHash);
        return androidHash;
    }

    public static byte[] getByteArrayFromHex(String hexString) {
        byte[] byteArray = new byte[hexString.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hexString.substring(index, index + 2), 16);
            byteArray[i] = (byte) j;
        }
        return byteArray;
    }

    public static void sendCancelStatusToSocket(String chatCode, String code) {
        HttpUtility httpUtility = new HttpUtility();
        Map<String, String> body = new HashMap<>();
        body.put("status", code);

        Map<String, String> headers = new HashMap<>();
        headers.put("chatcode", chatCode);
        try (okhttp3.Response response = httpUtility.callRequest(
                "application/json",
                "POST",
                EnvironmentReader.getValue(SOCKET_URL) + "/send?key=" + chatCode,
                new ObjectMapper().writeValueAsString(body),
                null, headers, 1)) {
            if (response.code() != 201) {
                logger.error("Failed to send transaction cancel event to chat id: " + chatCode);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public static String getValidDomain(String whitelistUrl) {
        if (whitelistUrl == null) return null;
        final String urlString = whitelistUrl.trim().toLowerCase();
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            return null;
        }
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isWhiteListHostUrlConfiguredInOtherApps(KeycloakSession session, String hostUrl, ClientModel clientModel) {
        return session.getContext().getRealm().getClientsStream()
                .map(x -> session.getContext().getRealm().getCibaAuthConfigByClientId(x.getClientId(), session.getContext().getRealm().getId()))
                .filter(Objects::nonNull)
                .filter(x -> !x.getClientId().equalsIgnoreCase(clientModel.getClientId()))
                .filter(Objects::nonNull)
                .filter(x -> x.getConfig() != null && x.getConfig().containsKey("whitelist_host"))
                .filter(x -> {
                    String whitelistHosts = x.getConfig().get("whitelist_host");
                    return Arrays.stream(whitelistHosts.trim().replaceAll("#", " ").split("\\s+"))
                            .anyMatch(host -> host.equals(hostUrl));
                }).count() > 0;
    }

    public static void removeUser(KeycloakSession session, RealmModel realm, UserModel userModel, boolean isVerificationFlow, boolean forceRemoveUser, EventBuilder event) {
        if (!isVerificationFlow
                && forceRemoveUser) {
            if (Config.getAdminRealm().equals(realm.getName())) {
                if (!userModel.hasRole(realm.getRole("admin"))) {
                    session.users().removeUser(session.getContext().getRealm(), userModel);
                    event.event(EZ_TRANSACTION_REMOVE_USER).detail(REASON, ONBOARDING_FAILED).success();
                }
            } else {
                if (!userModel.hasRole(realm.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT)
                        .getRole(ActionTokenConstants.ADMIN_USER_ROLE))) {
                    session.users().removeUser(session.getContext().getRealm(), userModel);
                    event.event(EZ_TRANSACTION_REMOVE_USER).detail(REASON, ONBOARDING_FAILED).success();
                }
            }
        }
    }

    public static String updateUserMetadata(KeycloakSession session, ClientModel clientModel, String data, RealmModel realm, String workFlow) {
        try {
            JSONObject inputData = new JSONObject(data);
            JSONObject userObject = inputData.getJSONObject("user");

            Map<String, Object> userMetadataObject = new HashMap<>();
            Map<String, Object> messageMetadataObject = new HashMap<>();

            userMetadataObject.put(WORKFLOW, workFlow);

            if (userObject.optJSONObject("metadata") != null) { // if user metadata already exists need to update user metadata
                JSONObject userMetadata = userObject.getJSONObject("metadata");
                Iterator<String> keys = userMetadata.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = userMetadata.getString(key);
                    userMetadataObject.put(key, value);
                }
            }
            if (inputData.optJSONObject("message") != null) { // if message metadata already exists need to update message metadata
                if (inputData.getJSONObject("message").optJSONObject("metadata") != null) {
                    Iterator<String> keys = inputData.getJSONObject("metadata").keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = inputData.getJSONObject("metadata").getString(key);
                        messageMetadataObject.put(key, value);
                    }
                }
            }

            if (inputData != null) {
                if (clientModel.getAuthenticationFlowBindingOverrides().containsKey(workFlow + "-action") && realm.getRequiredActionProvidersStream(clientModel.getAuthenticationFlowBindingOverrides().get(workFlow + "-action")).count() > 0) {
                    realm.getRequiredActionProvidersStream(clientModel.getAuthenticationFlowBindingOverrides().get(workFlow + "-action")).map(x -> {
                        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, x.getProviderId());
                        if (factory.getMetadata() != null) {
                            if (factory.getMetadata().containsKey("update_in_user_metadata")) {
                                messageMetadataObject.putAll(factory.getMetadata());
                            } else {
                                userMetadataObject.putAll(factory.getMetadata());
                            }
                        }
                        return userMetadataObject;
                    }).collect(Collectors.toList());
                }

                if (!userMetadataObject.isEmpty()) {
                    userObject.put("metadata", new JSONObject(userMetadataObject));
                    inputData.put("user", userObject);
                    data = inputData.toString(4);
                }

                if (!messageMetadataObject.isEmpty()) {
                    messageMetadataObject.remove("update_in_user_metadata");
                    Map<String, Object> messageMetadata = new HashMap<>();
                    if (inputData.optJSONObject("message") != null && inputData.optJSONObject("message").optJSONObject("consent") != null) {
                        messageMetadata.putAll(messageMetadataObject);
                        inputData.getJSONObject("message").put("metadata", messageMetadata);
                    } else {
                        messageMetadata.put("metadata", messageMetadataObject);
                        inputData.put("message", messageMetadata);
                    }
                    data = inputData.toString(4);
                }
                return data;
            }
        } catch (JSONException e) {
            logger.error(e);
        }
        return data;
    }

    public static String encryptUserReqBody(RealmModel realm, ClientModel client, String request) throws IOException {
        ComponentModel componentModel = realm.getComponent(client.getAttribute(CIBA_ENCRYPTION_ID));
        if (componentModel == null)
            throw new IOException("Encryption key is not set");
        String encrypted;
        try {
            encrypted = AESHelper.encrypt(request, componentModel.get("secret"));
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return encrypted;
    }

    public static MultivaluedMap<String, FormPartValue> getMultiPartFormParameters(RequiredActionContext context, MediaType mediaType, HttpHeaders headers, InputStream inputStream) throws IOException {
        try {

            if (!new MediaType("multipart", "form-data").isCompatible(mediaType) || !mediaType.getParameters().containsKey("boundary")) {
                return new MultivaluedHashMap<>();
            }

            Providers providers = ResteasyContext.getContextData(Providers.class);
            MessageBodyReader<MultipartFormDataInput> multiPartProvider = providers.getMessageBodyReader(
                    MultipartFormDataInput.class, null, null, MULTIPART_FORM_DATA_TYPE);
            MultipartFormDataInput inputs = multiPartProvider
                    .readFrom(null, null, null, mediaType, headers.getRequestHeaders(),
                            inputStream);
            MultivaluedHashMap<String, FormPartValue> parts = new MultivaluedHashMap<>();

            for (Map.Entry<String, List<InputPart>> entry : inputs.getFormDataMap().entrySet()) {
                for (InputPart value : entry.getValue()) {
                    javax.ws.rs.core.MediaType valueMediaType = value.getMediaType();

                    if (TEXT_PLAIN_TYPE.isCompatible(valueMediaType)) {
                        parts.add(entry.getKey(), new FormPartValue(value.getBodyAsString()));
                    } else {
                        parts.add(entry.getKey(), new FormPartValue(value.getBody(InputStream.class, null), valueMediaType));
                    }
                }
            }

            return parts;
        } catch (IOException cause) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION_ERROR, ErrorMappings.CONTACT_ADMIN);
            throw new IOException("Failed to parse multi part request", cause);
        }
    }

    public static String getPresignedObjectUrl(RequiredActionContext context, String objectName) {
        String bucketName = context.getRealm().getName();

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

    public static String putObjectToMinio(File file, RequiredActionContext context, String name) {
        String bucketName = context.getRealm().getName();
        String objectName = context.getUser().getId() + "/" + context.getRequestId() + "/" + name + "/" + file.getName();

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

    public static String getFileExtension(MediaType mediaType) {
        MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
        if (mediaType == null) {
            return null;
        }
        String extension = null;
        try {
            extension = allTypes.forName(mediaType.toString()).getExtension();
        } catch (MimeTypeException e) {
            return mediaType.getSubtype();
        }
        return extension.substring(1);
    }

    public static File createMediaTempFile(InputStream inputStream, MediaType mediaType) throws IOException {
        File tempFile = File.createTempFile("temp", "." + getFileExtension(mediaType));
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(inputStream, out);
        }
        return tempFile;
    }

    public static String getSecuredUrl(String url, CibaAuthConfigModel cibaAuthConfigModel) {
        HttpUtility httpUtility = new HttpUtility();
        HashMap<String, String> headers = new HashMap<>();
        String key = EnvironmentReader.getValue("SHORTENER_KEY");
        if (key == null) {
            return null;
        }
        headers.put("x-api-key", EnvironmentReader.getValue("SHORTENER_KEY"));

        HashMap<String, Object> body = new HashMap<>();
        body.put("url", url);
        body.put("ttl", TimeUnit.MILLISECONDS.toSeconds(getExpiryDuration(cibaAuthConfigModel != null ? cibaAuthConfigModel.getConfig().getOrDefault("verify-expiry-time", "120s") : "120s")));

        String shortnedUrl = null;
        try {
            okhttp3.Response response = httpUtility.callRequest("application/json", "POST", EnvironmentReader.getValue("ENV") == null ? EnvironmentReader.getValue("SHORTENER_HTTPS_URL") :
                    EnvironmentReader.getValue("SHORTENER_HTTP_URL"), JsonSerialization.writeValueAsString(body), null, headers, 1);

            if (response.isSuccessful()) {
                Map<String, String> parsed = JsonSerialization.readValue(response.body().string(), Map.class);
                if (parsed.get("id") != null) {
                    shortnedUrl =
                            EnvironmentReader.getValue("SHORTENER_HTTPS_URL") + "/" + parsed.get("id");
                    logger.info("----------------------");
                    logger.info("SHORTEND OIDC URL : " + shortnedUrl);
                    logger.info("----------------------");
                }
                if (shortnedUrl == null) {
                    return null;
                }
                return shortnedUrl;
            }
        } catch (Exception e) {

        }
        return null;
    }

    public static int getRandomTwoDigitNumber() {
        return 10 + new Random().nextInt(90);
    }


    public static Set<Integer> generateUniqueRandomNumbers(int promptNumber) {
        Set<Integer> uniqueNumbers = new HashSet<>();
        Random random = new Random();

        uniqueNumbers.add(promptNumber);

        while (uniqueNumbers.size() < 3) {
            int number = 10 + random.nextInt(90);
            uniqueNumbers.add(number);
        }

        return uniqueNumbers;
    }


    /**
     * Sets error attributes for a given required action context.
     * This method handles the addition of error information to the realm's attributes,
     * either creating a new error entry or appending to an existing one.
     *
     * @param context      The RequiredActionContext containing information about the current action.
     * @param errorCode    The Errors object representing the error code to be set.
     * @param templateName The name of the template associated with the error, 'bo-' prefix will be removed if present.
     */
    public static void setErrorAttribute(RequiredActionContext context, Errors errorCode, String templateName) {
        setErrorAttribute(context.getRealm(), context.getRequestId(), errorCode, templateName);
    }

    public static void setErrorAttribute(RealmModel realm, String requestId, Errors errorCode, String templateName) {
        String error = realm.getTrxValue(requestId, SP_ERROR_CODE);
        errorCode.setTemplateName(templateName.replace("bo-", "").replace("-", "_"));
        try {
            List<String> formattedErrors = new ArrayList<>();
            if (error == null) {
                formattedErrors.add(errorCode.toString());
                realm.addAttribute(requestId, SP_ERROR_CODE, String.valueOf(formattedErrors));
            } else {
                List<Map<String, Object>> errors = JsonSerialization.readValue(error, List.class);
                List<Errors> e = errors.stream().map(x -> Errors.valueOf(x.get("error_code").toString())).collect(Collectors.toList());
                e.add(errorCode);
                e.forEach(x -> formattedErrors.add(x.toString()));
                realm.addAttribute(requestId, SP_ERROR_CODE, String.valueOf(formattedErrors));
            }
            realm.addAttribute(requestId, "sp@" + errorCode.getTemplateName() + "_error_code", errorCode.getErrorCode());
        } catch (Exception e) {
            logger.error("GeneralHelper :: backoffice :: " + e.getMessage(), e);
        }
    }

}