package com.qlikverify.services.mfa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qlikverify.hook.spi.HookRepresentation;
import com.qlikverify.hook.spi.HookService;
import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.common.util.Resteasy;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.MFAProviderFactory;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.qlikverify.services.constants.Constants.*;

public class CyLockMFAProvider extends MFARecovery implements MFAProvider, MFAProviderFactory {

    private String[] PASS_RESULTS = {"AUTH_PASS_CROTP_BOTH", "AUTH_PASS", "AUTH_PASS_POTP_BOTH"};
    private static final Logger logger = Logger.getLogger(CyLockMFAProvider.class);
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:Cylock:PreLogin");
        showView(configModel, context, null, true, false);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:Cylock:PostLogin");
        String verifyWay = GeneralHelper.getConfigValue(configModel, "cylock_verify_way");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.failure();
            return false;
        }

        if (formData.getFirst("retry") != null) {
            showView(configModel, context, null, true, false);
            return false;
        }

        String expiry = context.getAuthenticationSession().getAuthNote("EXPIRY");
        if (System.currentTimeMillis() > Long.parseLong(expiry)) {
            redirectToError(context, "Session Expired, try again");
            return false;
        }

        if (verifyWay.equals("CROTP") || verifyWay.equals("POTP")) {
            String otp = formData.getFirst("otp");
            if (otp == null || otp.isEmpty()) {
                showView(configModel, context, "OTP is a required field", false, false);
                return false;
            }
            return processOfflineData(configModel, context, otp, false);
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                Map<String, String> data = JsonSerialization.readValue(formData.getFirst("hookdata"), Map.class);
                if(!data.containsKey("result"))
                    throw new IOException("Invalid Result");

                HookRepresentation hookRepresentation =
                        context.getSession().getProvider(HookService.class).findHook("cylock:"+context.getAuthenticationSession().getTabId());
                if(hookRepresentation.getHookResult() == null){
                    return  false;
                }
                return processData(context, JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class),
                        false);
            } catch (IOException e) {
                e.printStackTrace();
                showView(configModel, context, "Data Processing error", false, false);
                return false;
            }
        }

        showView(configModel, context, null, true, false);
        return false;
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:Cylock:PostRegistration");

        String verifyWay = GeneralHelper.getConfigValue(configModel, "cylock_verify_way");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.failure();
            return false;
        }

        if (formData.getFirst("retry") != null) {
            showView(configModel, context, null, true, true);
            return false;
        }

        String expiry = context.getAuthenticationSession().getAuthNote("EXPIRY");
        if (System.currentTimeMillis() > Long.parseLong(expiry)) {
            redirectToError(context, "Session Expired, try again");
            return false;
        }

        if (verifyWay.equals("CROTP") || verifyWay.equals("POTP")) {
            String otp = formData.getFirst("otp");
            if (otp == null || otp.isEmpty()) {
                showView(configModel, context, "OTP is a required field", false, true);
                return false;
            }
            return processOfflineData(configModel, context, otp, true);
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                Map<String, String> data = JsonSerialization.readValue(formData.getFirst("hookdata"), Map.class);
                if(!data.containsKey("result"))
                    throw new IOException("Invalid Result");

//                Map<String,String> resultData = JsonSerialization.readValue(context.getAuthenticationSession().getAuthNote("HOOK_DATA"),Map.class);
                HookRepresentation hookRepresentation =
                        context.getSession().getProvider(HookService.class).findHook("cylock:"+context.getAuthenticationSession().getTabId());
                if(hookRepresentation == null  || hookRepresentation.getHookResult() == null){
                    return  false;
                }
                return processData(context, JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class),
                        false);
            } catch (IOException e) {
                e.printStackTrace();
                showView(configModel, context, "Data Processing error", false, true);
                return false;
            }
        }

        showView(configModel, context, null, true, true);
        return false;
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:Cylock:PreRegistration");
        showView(configModel, context, null, true, true);
    }

    @Override
    public String getDisplayName() {
        return "CyLock";
    }

    @Override
    public String getProviderClass() {
        return (MFAProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "cylock.png";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/SMS-Provider/CyLock.md";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        return GeneralHelper.validateConfig(getConfigProperties(session), config);
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName(CYLOCK_SERVER);
        configProperty.setLabel("mfa-cylock-server");
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setRequired(true);
        List<String> cylockServerOptions = new ArrayList<>();
        cylockServerOptions.add("UAT");
        cylockServerOptions.add("LIVE");
        configProperty.setOptions(cylockServerOptions);
        configProperty.setHelpText("mfa-cylock-server-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("cylock_verify_way");
        configProperty.setLabel("mfa-cylock-verify-way");
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setRequired(true);
        List<String> cylockVerifyWay = new ArrayList<>();
        cylockVerifyWay.add("PUSH");
        cylockVerifyWay.add("CROTP");
        cylockVerifyWay.add("POTP");
        configProperty.setOptions(cylockVerifyWay);
        configProperty.setHelpText("mfa-cylock-verify-way-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("cylock_api_key");
        configProperty.setLabel("mfa-cylock-api-key");
        configProperty.setRequired(true);
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setHelpText("mfa-cylock-api-key-helptext");
        configProperty.setSecret(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("cylock_customer_id");
        configProperty.setLabel("mfa-cylock-customer-id");
        configProperty.setRequired(true);
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setHelpText("mfa-cylock-customer-id-helptext");
        configProperty.setSecret(true);
        configProperties.add(configProperty);

        List<String> otpLength = new ArrayList<>();
        otpLength.add("Username");
        otpLength.add("Email");
        otpLength.add("Phone");
        configProperty = new ProviderConfigProperty();
        configProperty.setName("user_select");
        configProperty.setLabel("mfa-cylock-user-mapping");
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setOptions(otpLength);
        configProperty.setRequired(true);
        configProperty.setHelpText("mfa-cylock-user-mapping-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SESSION_EXPIRY_PUSH");
        configProperty.setLabel("mfa-cylock-waiting-for-response");
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        configProperty.setHelpText("mfa-cylock-waiting-for-response-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("enable_mfa_recovery");
        configProperty.setLabel("mfa-cylock-recovery");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("mfa-cylock-recovery-helptext");
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public OTPPolicy getOtpPolicy(RealmModel realmModel) {
        return null;
    }

    @Override
    public String getHelpText() {
        return "mfa-cylock-desc";
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public MFAProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "cylock-mfa";
    }


    private void showView(MFAConfigModel configModel, RequiredActionContext context, String error, boolean retry, boolean isReg) {
        String chatCode = getChatCode(context);
        context.getAuthenticationSession().setAuthNote("CHATCODE", chatCode);

        String verificationExpiryDuration = GeneralHelper.getConfigValue(configModel, "SESSION_EXPIRY_PUSH", "5m");
        if (verificationExpiryDuration == null || verificationExpiryDuration.isEmpty()) {
            verificationExpiryDuration = "5m";
        }

        String expiry = context.getAuthenticationSession().getAuthNote("EXPIRY");
        if (expiry == null || retry) {
            expiry = String.valueOf(
                    GeneralHelper.getExpiryDuration(verificationExpiryDuration));
        }


        String verifyWay = GeneralHelper.getConfigValue(configModel, "cylock_verify_way");

        showView(configModel, context, error, verifyWay.equals("PUSH") ? "cylock-mfa.ftl" : "cylock-offline-mfa.ftl", expiry, chatCode, retry, isReg);
    }

    private void showView(MFAConfigModel configModel, RequiredActionContext context, String error, String ftl, String expiry, String chatCode, boolean retry, boolean isReg) {

        context.getAuthenticationSession().setAuthNote("EXPIRY", expiry);
        String browser = context.getConnection().getClientBrowser();
        String ip = context.getConnection().getRemoteAddr();

        String verifyWay = GeneralHelper.getConfigValue(configModel, "cylock_verify_way");

        String randomValue = GeneralHelper.getOtp(5);

        context.getAuthenticationSession().setAuthNote("RANDOM_STR", randomValue);
        if (retry) {
            HookRepresentation hookRepresentation = new HookRepresentation();
            hookRepresentation.setRealmId(context.getRealm().getId());
            hookRepresentation.setKey("cylock:" + context.getAuthenticationSession().getTabId());
            hookRepresentation.setChatCode(chatCode);
            context.getSession().getProvider(HookService.class).addHook(hookRepresentation);

            Map<String, String> payload = new HashMap<>();
            payload.put(CYLOCK_SERVER, configModel.getConfig().get(CYLOCK_SERVER));
            payload.put("cylock_customer_id", configModel.getConfig().get("cylock_customer_id"));
            payload.put("cylock_username", getUser(configModel, context.getUser()));
            payload.put("cylock_api_key", configModel.getConfig().get("cylock_api_key"));
            payload.put("randomstring", randomValue);
            payload.put("chatCode", chatCode);
            payload.put("verify_way", verifyWay);
            payload.put("message", "Confirm MFA Request From Browser : " + browser + " IP :" + ip);
            processRequest(payload,context);
        }

        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        formsProvider.setAttribute("expiry", String.valueOf(expiry));

        if (!isReg) {
            formsProvider.setAttribute("showTroubleShoot", isMFARecoveryEnabled(context, configModel));
        }

        if (verifyWay.equals("PUSH")) {
            formsProvider.setAttribute("chatcode", chatCode);
            logger.info("MINIO_URL: "+DEFAULT_SERVER_URL);
            logger.info("SOCKET_URL : "+EnvironmentReader.getValue(SOCKET_URL));
            formsProvider.setAttribute("socketUrl",EnvironmentReader.getValue(SOCKET_URL));
        }

        if (verifyWay.equals("CROTP")) {
            formsProvider.setAttribute("isCROTP", true);
        }

        if (verifyWay.equals("POTP")) {
            formsProvider.setAttribute("isPOTP", true);
        }

        Response challenge = formsProvider
                .setAttribute("title", getTitle(verifyWay))
                .setAttribute("mfaProviderId", getId())
                .setAuthenticationSession(context.getAuthenticationSession())
                .createForm(ftl);
        context.challenge(challenge);
    }

    private String getUser(MFAConfigModel configModel, UserModel userModel) {

        String userSelectProperty = GeneralHelper.getConfigValue(configModel, "user_select", "Email");

        String username = null;

        switch (userSelectProperty) {
            case "Username":
                username = userModel.getUsername();
                break;
            case "Email":
                username = userModel.getEmail();
                break;
            case "Phone":
                username = userModel.getMobileNumber();
                break;
        }

        if (username == null || username.isEmpty()) {
            username = userModel.getUsername();
        }
        return username;
    }

    private String getTitle(String verifyWay) {
        return "Two-Factor Authentication";
    }

    private boolean processOfflineData(MFAConfigModel configModel, RequiredActionContext context, String otp, boolean isFromRegistration) {
        Map<String, String> payload = new HashMap<>();
        String cyServer = configModel.getConfig().get(CYLOCK_SERVER);
        String server = "uatauth";
        if (cyServer.equals("LIVE")) {
            server = "auth";
        }
        payload.put("id_sp", configModel.getConfig().get("cylock_customer_id"));
        payload.put("uid_sp", getUser(configModel, context.getUser()));
        payload.put("trans_id_sp", context.getAuthenticationSession().getAuthNote("RANDOM_STR"));
        payload.put("cypher_text", otp);
        payload.put("trans_str", "Confirm MFA Request");
        HttpUtility httpUtility = new HttpUtility();
        okhttp3.Response response = null;
        response = httpUtility.callRequest(MediaType.APPLICATION_FORM_URLENCODED, "POST", "https://" + server + ".cybernexa.com/api/v1/srv/validatofflineotp", getFormEncoded(payload), configModel.getConfig().get("cylock_api_key"));

        try {
            Map<String, String> data = JsonSerialization.readValue(response.body().string(), Map.class);
            response.close();
            return processData(context, data, isFromRegistration);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean processData(RequiredActionContext context, Map<String, String> data, boolean isFromRegistration) {

        String failure = data.get("failure");
        String result = data.get("result");

        if (failure.equalsIgnoreCase("yes")) {
            redirectToError(context, result);
            return false;
        }

        if (!Arrays.asList(PASS_RESULTS).contains(result)) {
            redirectToError(context, result);
            return false;
        }

        if (isFromRegistration) {
            OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), "test",context.getAuthenticationSession().getClient().getClientId());
            otpCredentialModel.setProviderId(getId());
            OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
            otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), otpCredentialModel);
        }
        return true;
    }

    private void processRequest(Map<String, String> request, RequiredActionContext context) {
        Map<String, String> payload = new HashMap<>();
        String verifyWay = request.get("verify_way");
        String cyServer = request.get(CYLOCK_SERVER);
        String server = "uatauth";
        if (cyServer.equals("LIVE")) {
            server = "auth";
        }
        String url = "https://" + server + ".cybernexa.com/api/v1/srv/";
        if (verifyWay.equals("PUSH")) {
            url += "doauth";
            payload.put("id_sp", request.get("cylock_customer_id"));
            payload.put("uid_sp", request.get("cylock_username"));
            payload.put("trans_id_sp", org.keycloak.utils.GeneralHelper.getOtp(5));
            payload.put("real_time", "True");
            payload.put("trans_str", request.get("message"));
        } else if (verifyWay.equals("CROTP")) {
            url += "doofflineauth";
            payload.put("id_sp", request.get("cylock_customer_id"));
            payload.put("uid_sp", request.get("cylock_username"));
            payload.put("trans_id_sp", request.get("randomstring"));
            payload.put("trans_str", request.get("message"));
            payload.put("otpType", "CROTP");
            payload.put("otpMode", "BOTH");
        } else if (verifyWay.equals("POTP")) {
            url += "doofflineauth";
            payload.put("id_sp", request.get("cylock_customer_id"));
            payload.put("uid_sp", request.get("cylock_username"));
            payload.put("trans_id_sp", request.get("randomstring"));
            payload.put("trans_str", request.get("message"));
            payload.put("otpType", "POTP");
            payload.put("otpMode", "BOTH");
        }
        HttpUtility httpUtility = new HttpUtility();
        String finalUrl = url;
        RequiredActionContext ctx = context;
        KeycloakModelUtils.runJobInTransaction(context.getSession().getKeycloakSessionFactory(),
                new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        CompletableFuture.runAsync(new Runnable() {
                            @Override
                            public void run() {
                                okhttp3.Response response = null;
                                Resteasy.pushContext(KeycloakSession.class, innerSession);
                                innerSession.getContext().setRealm(ctx.getRealm());
                                KeycloakTransactionManager transactionManager =
                                        innerSession.getTransactionManager();
                                try {
                                    response = httpUtility.callRequest(MediaType.APPLICATION_FORM_URLENCODED,
                                            "POST",
                                            finalUrl, getFormEncoded(payload), request.get("cylock_api_key"));
                                        transactionManager.begin();
                                        HookService hookProvider = innerSession.getProvider(HookService.class);
                                        hookProvider.updateHook(request.get("chatCode"), response.body().string());
                                        transactionManager.commit();
                                } catch (Exception e) {
                                    logger.error(e.getMessage());
                                    transactionManager.rollback();
                                    throw new RuntimeException(e);
                                } finally {
                                    if (response != null) response.close();
                                }
                                if (verifyWay.equals("PUSH")) {
                                    Map<String, String> pushResultAttr = new HashMap<>();
                                    pushResultAttr.put("result", "true");
                                    sendResult(request.get("chatCode"), pushResultAttr);
                                }
                            }
                        });
                    }
                });
    }

    private void sendResult(String chatCode, Map<String, String> payload) {
        HttpUtility httpUtility = new HttpUtility();
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                okhttp3.Response response = null;
                try {
                   response = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST", EnvironmentReader.getValue(SOCKET_URL)+"/send?key=" + chatCode, JsonSerialization.writeValueAsString(payload), null);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.close();
            }
        });
    }

    private void redirectToError(RequiredActionContext context, String message) {
        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, message)
                .setAuthenticationSession(context.getAuthenticationSession())
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }

    private String getFormEncoded(Map<String, String> payload) {
        final String[] load = {""};
        payload.forEach((k, v) -> {
            if (load[0].length() > 0) {
                load[0] += "&";
            }
            load[0] += k + "=" + v;
        });
        return load[0];
    }

    private String getChatCode(RequiredActionContext ctx) {
        String ctxChatCode = ctx.getAuthenticationSession().getAuthNote("CHATCODE");
        if (ctxChatCode == null) return UUID.randomUUID().toString();
        if (ctxChatCode.isEmpty()) return UUID.randomUUID().toString();
        return ctxChatCode;
    }
}
