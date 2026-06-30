package com.qlikverify.services.authenticators.misc.whatsapp;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qlikverify.services.constants.Constants.*;
import static com.qlikverify.services.helpers.GeneralHelper.getExpiryDuration;

public class WhatsappOTP implements AuthProvider {

    private String registationTitle = "whatsapp-otp-reg";
    private String loginTitle = "whatsapp-otp-login-title";
    private static final Logger logger = Logger.getLogger(WhatsappOTP.class);

    @Override
    public void onPreLogin(AuthenticationFlowContext context) {
        showView(context, null, true, false);
    }

    @Override
    public void onPostLogin(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst(RESEND_OTP) != null && Boolean.parseBoolean(formData.getFirst(RESEND_OTP))) {
            showView(context, null, true, false);
            return;
        }

        processData(context, false);
    }

    @Override
    public void onPostRegistration(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst(RESEND_OTP) != null && Boolean.parseBoolean(formData.getFirst(RESEND_OTP))) {
            showView(context, null, true, true);
            return;
        }

        processData(context, true);
    }

    @Override
    public void onPreRegistration(AuthenticationFlowContext context) {
        showView(context, null, true, true);
    }

    private void showView(AuthenticationFlowContext context, String error, boolean triggerNewOTP, boolean fromReg) {
        if (triggerNewOTP) {
            String expiryTime = GeneralHelper.getConfigValue(context, Constants.VERIFICATION_EXPIRY_CONFIG, "5m");
            if (expiryTime == null || expiryTime.isEmpty()) {
                expiryTime = "5m";
            }
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryTime)));
            String otp = GeneralHelper.getOtp(Integer.parseInt(GeneralHelper.getConfigValue(context, PHONE_VER_OTP_LENGTH, "6")));
            context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, otp);
            SmsSenderProvider smsSenderProvider = getSmsProvider(context);
            if (context.getRealm().isEnabledOwnWhatsappProvider()) {
                smsSenderProvider.setProviderId("whatsapp");
            } else {
                smsSenderProvider.setProviderId(GeneralHelper.getDefaultWhatsappSender(context.getSession()).getSmsProviderId());
            }


            Map<String, Object> smsPayload = new HashMap<>();
            List<String> datas = new ArrayList<>();
            datas.add(context.getRealm().getDisplayName() + " One Time");
            datas.add(otp);
            smsPayload.put("templateType", "OTP");
            smsPayload.put("messageParts", datas.toArray());

            try {
                smsSenderProvider.sendSMS(getPhoneNumber(context), null, smsPayload);
            } catch (SmsException e) {
                logger.error("Error Occurred on sendSMS " + e.getMessage());
                redirectToError(context, e.getMessage());
                return;
            }
        }

        LoginFormsProvider formsProvider = context.form();
        formsProvider.setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY));
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }

        Response challenge = formsProvider
                .setAttribute("title", fromReg ? registationTitle : loginTitle)
                .setAttribute(OTP_LENGTH_ATTR, context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") != null ? context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") : 6)
                .createForm("whatsapp-mfa-otp.ftl");

        context.challenge(challenge);
    }

    private SmsSenderProvider getSmsProvider(AuthenticationFlowContext context) {
        return context.getSession().getProvider(SmsSenderProvider.class, "default");
    }

    private void redirectToError(AuthenticationFlowContext context, String message) {
        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, message)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }

    private void processData(AuthenticationFlowContext context, boolean isFromRegistration) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String otp = context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP);
        String otpExpiryTime = context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY);

        if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
            showView(context, ErrorMappings.OTP_EXPIRED, false, isFromRegistration);
            return;
        }

        if (formData.getFirst(Constants.FORM_OTP) == null || formData.getFirst(Constants.FORM_OTP).length() == 0) {
            showView(context, ErrorMappings.ENTER_OTP, false, isFromRegistration);
        } else if (formData.getFirst(Constants.FORM_OTP).equals(otp)) {

            if (isFromRegistration) {
                String phoneNumber = context.getAuthenticationSession().getAuthNote(UserModel.MOBILE_NUMBER);
                String phoneNumberRegion =
                        context.getAuthenticationSession().getAuthNote(UserModel.MOBILE_NUMBER_REGION);
                UserModel user = null;
                if(context.getUser() == null){
                    user = context.getSession().users().addUser(context.getRealm(), phoneNumber);
                    user.setEnabled(true);
                    user.setSingleAttribute(UserModel.MOBILE_NUMBER_REGION,phoneNumberRegion.toLowerCase());
                    user.setSingleAttribute(UserModel.MOBILE_NUMBER, phoneNumber);
                    user.setSingleAttribute(ATTRIBUTE_PHONE_VERIFIED, "TRUE");
                    context.setUser(user);
                }
                else{
                    user = context.getSession().users().getUserByMobile(phoneNumber, context.getRealm());
                }
            } else {
                UserModel user = context.getUser();
                user.setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
            }
            context.success();
        } else {
            showView(context, ErrorMappings.WRONG_OTP_ERROR, false, isFromRegistration);
        }
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
