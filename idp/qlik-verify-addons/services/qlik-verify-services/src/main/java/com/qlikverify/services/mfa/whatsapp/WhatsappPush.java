package com.qlikverify.services.mfa.whatsapp;

import com.qlikverify.hook.spi.HookRepresentation;
import com.qlikverify.hook.spi.HookService;
import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.MFAConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static com.qlikverify.services.constants.Constants.*;
import static com.qlikverify.services.helpers.GeneralHelper.getExpiryDuration;
import static org.keycloak.authentication.authenticators.utils.GeneralHelper.redirectToErrorPage;

public class WhatsappPush extends MFARecovery implements MFAProvider {
    private String id;
    private static final Logger logger = Logger.getLogger(WhatsappPush.class);
    public WhatsappPush(String id) {
        this.id = id;
    }

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappPush:PreLogin");
        showView(configModel, context, null, true,false);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappPush:PostLogin");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.failure();
            return false;
        }

        if (formData.getFirst("retry") != null) {
            showView(configModel, context, null, true,false);
            return false;
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                String phoneNumber = null;
                try {
                    phoneNumber = getPhoneNumber(context);
                } catch (SmsException e) {
                    logger.error(e.getMessage());
                    redirectToErrorPage(context, e.getMessage());
                    return false;
                }

                HookRepresentation hookRepresentation = context.getSession().getProvider(HookService.class).findHook("whatsapp:" + phoneNumber);
                Map<String, List<String>> data = JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class);
                return processData(context, data, false);

            } catch (IOException e) {
                logger.error(e.getMessage());
                showView(configModel, context, "Data Processing error", false,false);
                return false;
            }
        }

        showView(configModel, context, null, true,false);
        return false;
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappPush:PostRegistration");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.failure();
            return false;
        }

        if (formData.getFirst("retry") != null) {
            showView(configModel, context, null, true,true);
            return false;
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                String phoneNumber = null;
                try {
                    phoneNumber = getPhoneNumber(context);
                } catch (SmsException e) {
                    logger.error(e.getMessage());
                    redirectToErrorPage(context, e.getMessage());
                    return false;
                }

                HookRepresentation hookRepresentation = context.getSession().getProvider(HookService.class).findHook("whatsapp:" + phoneNumber);
                Map<String, List<String>> data = JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class);
                return processData(context, data, true);
            } catch (IOException e) {
                logger.error(e.getMessage());
                showView(configModel, context, "Data Processing error", false,true);
                return false;
            }
        }

        showView(configModel, context, null, true,true);
        return false;
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:WhatsappPush:PreRegistration");
        showView(configModel, context, null, true,true);
    }

    private void showView(MFAConfigModel mfaConfigModel, RequiredActionContext context, String error, boolean retry, boolean isReg) {
        try {


            if (retry) {
                String expiryTime = GeneralHelper.getConfigValue(mfaConfigModel, "SESSION_EXPIRY_PUSH", "5m");
                if (expiryTime == null || expiryTime.isEmpty()) {
                    expiryTime = "5m";
                }

                context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryTime)));

                String chatCode = UUID.randomUUID().toString();
                context.getAuthenticationSession().setAuthNote("CHATCODE", chatCode);
                String phoneNumber = null;
                try {
                    UserModel userModel = context.getUser();
                    if(userModel.getMobileNumber() == null){
                        logger.error("No mobile number is associated with this user. Please contact admin.");
                        redirectToError(context, "no-mobile-number");
                        return;
                    }
                    phoneNumber = getPhoneNumber(context);
                } catch (SmsException e) {
                    logger.error(e.getMessage());
                    redirectToError(context, e.getMessage());
                    return;
                }
                String finalPhoneNumber = phoneNumber;
                HookRepresentation hookRepresentation = new HookRepresentation();
                hookRepresentation.setRealmId(context.getRealm().getId());
                hookRepresentation.setKey("whatsapp:" + finalPhoneNumber);
                hookRepresentation.setChatCode(chatCode);
                context.getSession().getProvider(HookService.class).addHook(hookRepresentation);

                SmsSenderProvider smsSenderProvider = getSmsProvider(context);
                if (context.getRealm().isEnabledOwnSmsProvider()) {
                    smsSenderProvider.setProviderId("whatsapp");
                } else {
                    smsSenderProvider.setProviderId(GeneralHelper.getDefaultWhatsappSender(context.getSession()).getSmsProviderId());
                }

                Map<String, Object> smsPayload = new HashMap<>();
                List<String> datas = new ArrayList<>();
                datas.add(getPushName(context));
                datas.add(" Browser : " + context.getConnection().getClientBrowser());
                smsPayload.put("templateType", "PUSH");
                smsPayload.put("messageParts", datas.toArray());

                try {
                    smsSenderProvider.sendSMS(getPhoneNumber(context), null, smsPayload);
                } catch (SmsException e) {
                    logger.error(e.getMessage());
                    redirectToError(context, e.getMessage());
                    return;
                }
            }

            LoginFormsProvider loginFormsProvider = context.form();
            loginFormsProvider
                    .setAttribute("mfaProviderId", id);
            loginFormsProvider.setAttribute("chatcode", context.getAuthenticationSession().getAuthNote("CHATCODE"));
            loginFormsProvider.setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY));
            if (error != null) {
                loginFormsProvider.addError(new FormMessage(error));
            }

            if (!isReg) {
                loginFormsProvider
                        .setAttribute("showTroubleShoot", isMFARecoveryEnabled(context, mfaConfigModel));
            }

            Response challenge = loginFormsProvider
                    .setAttribute("socketUrl", EnvironmentReader.getValue("SOCKET_URL"))
                    .setAttribute("title", isReg? "whatsapp-push-two-factor" : "whatsapp-push-two-factor-verify" )
                    .setAttribute("pageType","mfa")
                    .createForm("whatsapp-mfa-push.ftl");
            context.challenge(challenge);
        }
        catch (Exception err)
        {
            err.printStackTrace();
            redirectToError(context,"something-went-wrong-check-admin");
        }
    }

    private SmsSenderProvider getSmsProvider(RequiredActionContext context) {
        return context.getSession().getProvider(SmsSenderProvider.class, "default");
    }

    @Override
    public void close() {
        // NOP
    }

    private void redirectToError(RequiredActionContext context, String message) {
        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, message)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }

    private boolean processData(RequiredActionContext context, Map<String, List<String>> data, boolean isFromRegistration) {
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        String userPhone = data.get("From").get(0);
        String payload = data.get("ButtonPayload").get(0);

        if (payload.equalsIgnoreCase("Reject-Payload")) {
            context.failure();
            return false;
        }

        if (userPhone == null) {
            redirectToError(context, "phone-num-missing");
            return false;
        }

        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(userPhone.split(":")[1], null);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            redirectToError(context, ErrorMappings.PHONE_NUMBER_INVALID);
            return false;
        }

        if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
            redirectToError(context, ErrorMappings.PHONE_NUMBER_INVALID);
            return false;
        }

        String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length() + 1);

        if (!context.getUser().getMobileNumber().equalsIgnoreCase(phoneNumber)) {
            redirectToError(context, ErrorMappings.PHONE_NUMBER_NOT_REGISTERED);
            return false;
        }
        if (System.currentTimeMillis() > Long.parseLong(context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY))) {
            redirectToError(context, ErrorMappings.WP_PUSH_NOTIFICATION_EXPIRED);
            return false;
        }
        context.getUser().setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
        if (isFromRegistration) {
            OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), "test",context.getAuthenticationSession().getClient().getClientId());
            otpCredentialModel.setProviderId(id);
            otpCredentialModel.setClientId(context.getAuthenticationSession().getClient().getClientId());
            OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
            otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), otpCredentialModel);
        }
        return true;
    }

    private String getPhoneNumber(RequiredActionContext context) throws SmsException {
        UserModel userModel = context.getUser();
        NumberFormat numberFormat = getSmsProvider(context).parseNumber(userModel.getMobileNumber(), userModel.getMobileNumberRegion().toUpperCase());
        return numberFormat.getE16Format();
    }

    private String getPushName(RequiredActionContext context){
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
        }
        catch (Exception err)
        {

        }
        return name;
    }
}
