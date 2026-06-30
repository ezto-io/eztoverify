package com.qlikverify.services.authenticators.misc.whatsapp;

import com.qlikverify.hook.spi.HookRepresentation;
import com.qlikverify.hook.spi.HookService;
import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static com.qlikverify.services.constants.Constants.SESSION_MOBILE_OTP_EXPIRY;
import static com.qlikverify.services.constants.Constants.USER_DISABLED_BRUTEFORCE;
import static com.qlikverify.services.helpers.GeneralHelper.getCompanyName;
import static com.qlikverify.services.helpers.GeneralHelper.getExpiryDuration;

public class WhatsappPush implements AuthProvider {

    private String registationTitle = "whatsapp-push-reg-title";
    private String loginTitle = "whatsapp-push-login-title";

    private static final Logger logger = Logger.getLogger(WhatsappPush.class);

    @Override
    public void onPreLogin(AuthenticationFlowContext context) {
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        showView(configModel, context, null, true, false);
    }

    @Override
    public void onPostLogin(AuthenticationFlowContext context) {
        AuthenticatorConfigModel configModel    = context.getAuthenticatorConfig();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.cancelLogin();
            return;
        }

        if (formData.getFirst("retry") != null) {
            showView(configModel, context, null, true, false);
            return;
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                String phoneNumber = null;
                try {
                    phoneNumber = getPhoneNumber(context);
                } catch (SmsException e) {
                    logger.error(e.getMessage());
                    GeneralHelper.redirectToErrorPage(context, e.getMessage());
                    return;
                }

                HookRepresentation hookRepresentation = context.getSession().getProvider(HookService.class).findHook("whatsapp:" + phoneNumber);
                Map<String, List<String>> data = JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class);
                processData(context, data, false);
                context.getSession().getProvider(HookService.class).deleteHook(hookRepresentation.getChatCode(),context.getRealm().getId());
                return;
            } catch (IOException e) {
                logger.error(e.getMessage());
                showView(configModel, context, "data-processing-error", false, false);
            }
        }

        showView(configModel, context, null, true, false);
    }

    @Override
    public void onPostRegistration(AuthenticationFlowContext context) {
        AuthenticatorConfigModel configModel    = context.getAuthenticatorConfig();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.cancelLogin();
            return;
        }

        if (formData.getFirst("retry") != null) {
            showView(configModel, context, null, true,true);
            return;
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                String phoneNumber = null;
                try {
                    phoneNumber = getPhoneNumber(context);
                } catch (SmsException e) {
                    logger.error(e.getMessage());
                    GeneralHelper.redirectToErrorPage(context, e.getMessage());
                    return;
                }

                HookRepresentation hookRepresentation = context.getSession().getProvider(HookService.class).findHook("whatsapp:" + phoneNumber);
                Map<String, List<String>> data = JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class);
                processData(context, data, true);
                context.getSession().getProvider(HookService.class).deleteHook(hookRepresentation.getChatCode(),context.getRealm().getId());
                return;
            } catch (IOException e) {
                logger.error(e.getMessage());
                showView(configModel, context, "data-processing-error", false, true);
            }
        }

        showView(configModel, context, null, true, true);
    }

    @Override
    public void onPreRegistration(AuthenticationFlowContext context) {
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        showView(configModel, context, null, true,true);
    }

    private void showView(AuthenticatorConfigModel configModel, AuthenticationFlowContext context, String error, boolean retry, boolean fromReg) {
        if (retry) {
            String expiryTime = GeneralHelper.getConfigValue(context, "SESSION_EXPIRY_PUSH", "5m");
            if(expiryTime == null || expiryTime.isEmpty()) {
                expiryTime = "5m";
            }
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryTime)));

            String chatCode = UUID.randomUUID().toString();
            context.getAuthenticationSession().setAuthNote("CHATCODE", chatCode);
            String phoneNumber = null;
            try {
                phoneNumber = getPhoneNumber(context);
            } catch (SmsException e) {
                logger.error(e.getMessage());
                GeneralHelper.redirectToErrorPage(context, e.getMessage());
                return;
            }


            String finalPhoneNumber = phoneNumber;
            HookRepresentation hookRepresentation = new HookRepresentation();
            hookRepresentation.setRealmId(context.getRealm().getId());
            hookRepresentation.setKey("whatsapp:" + finalPhoneNumber);
            hookRepresentation.setChatCode(chatCode);
            context.getSession().getProvider(HookService.class).addHook(hookRepresentation);

            SmsSenderProvider smsSenderProvider = getSmsProvider(context);
            smsSenderProvider.setProviderId("whatsapp");

            Map<String, Object> smsPayload = new HashMap<>();
            List<String> datas = new ArrayList<>();
            datas.add(getCompanyName(context));
            datas.add(" Browser : " + context.getConnection().getClientBrowser());
            smsPayload.put("templateType", "PUSH");
            smsPayload.put("messageParts", datas.toArray());

            try {
                smsSenderProvider.sendSMS(getPhoneNumber(context), null, smsPayload);
            } catch (SmsException e) {
                e.printStackTrace();
                GeneralHelper.redirectToErrorPage(context, e.getMessage());
                return;
            }
        }

        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAttribute("chatcode", context.getAuthenticationSession().getAuthNote("CHATCODE"));
        loginFormsProvider.setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY));
        if (error != null) {
            loginFormsProvider.addError(new FormMessage(error));
        }

        Response challenge = loginFormsProvider
                .setAttribute("title", fromReg ? registationTitle : loginTitle)
                .setAttribute("socketUrl", EnvironmentReader.getValue("SOCKET_URL"))
                .createForm("whatsapp-mfa-push.ftl");
        context.challenge(challenge);
    }

    private SmsSenderProvider getSmsProvider(AuthenticationFlowContext context) {
        return context.getSession().getProvider(SmsSenderProvider.class, "default");
    }

    private void processData(AuthenticationFlowContext context, Map<String, List<String>> data, boolean isFromRegistration) {
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        String userPhone    = data.get("From").get(0);
        String userProfile  = data.get("ProfileName").get(0);

        if (data.get("ButtonPayload") == null ){
            context.getSession().getProvider(HookService.class).deleteHook(context.getSession().getProvider(HookService.class).findHook(userPhone).getChatCode(),context.getRealm().getId());
            GeneralHelper.redirectToErrorPage(context, "invalid-response-choose-from-request");
            return;
        }
        String payload      = data.get("ButtonPayload").get(0);

        if (payload.equalsIgnoreCase("Reject-Payload")) {
            context.getSession().getProvider(HookService.class).deleteHook(context.getSession().getProvider(HookService.class).findHook(userPhone).getChatCode(),context.getRealm().getId());
            context.cancelLogin();
            return;
        }

        if (userPhone == null) {
            GeneralHelper.redirectToErrorPage(context, "phone-num-missing");
            return;
        }

        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(userPhone.split(":")[1], null);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.PHONE_NUMBER_INVALID);
            return;
        }

        if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.PHONE_NUMBER_INVALID);
            return;
        }

        String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length() + 1);

        if ( context.getUser() != null && !context.getUser().getMobileNumber().equalsIgnoreCase(phoneNumber)) {
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.PHONE_NUMBER_NOT_REGISTERED);
            return;
        }

        //check user was locked by bruteforce
        if(context.getUser() != null && GeneralHelper.isTemporarilyDisabledByBruteForce(context,context.getUser(),USER_DISABLED_BRUTEFORCE)){
            context.getSession().getProvider(HookService.class).deleteHook(context.getSession().getProvider(HookService.class).findHook(userPhone).getChatCode(),context.getRealm().getId());
            return;
        }

        context.getSession().getProvider(HookService.class).deleteHook(context.getSession().getProvider(HookService.class).findHook(userPhone).getChatCode(),context.getRealm().getId());
        if (isFromRegistration) {
            UserModel user = context.getSession().users().addUser(context.getRealm(), phoneNumber);
            user.setFirstName(userProfile);
            user.setEnabled(true);
            user.setSingleAttribute(UserModel.MOBILE_NUMBER_REGION,numberFormat.getRegion().toLowerCase());
            user.setSingleAttribute(UserModel.MOBILE_NUMBER, phoneNumber);
            context.setUser(user);
        }
        context.getUser().setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
        context.success();
    }

    private String getPhoneNumber(AuthenticationFlowContext context) throws SmsException {
        UserModel userModel = context.getUser();
        NumberFormat numberFormat;

        if (userModel == null) {
            numberFormat =
                    getSmsProvider(context).parseNumber(context.getAuthenticationSession().getAuthNote(UserModel.MOBILE_NUMBER),context.getAuthenticationSession().getAuthNote(UserModel.MOBILE_NUMBER_REGION).toUpperCase());
        } else {
            numberFormat = getSmsProvider(context).parseNumber(userModel.getMobileNumber(),
                    userModel.getMobileNumberRegion().toUpperCase());
        }

        return numberFormat.getE16Format();
    }
}
