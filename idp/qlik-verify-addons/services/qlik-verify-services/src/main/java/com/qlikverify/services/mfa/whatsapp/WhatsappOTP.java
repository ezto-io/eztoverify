package com.qlikverify.services.mfa.whatsapp;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.MFAConfigModel;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
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
import static com.qlikverify.services.helpers.GeneralHelper.getProtector;

public class WhatsappOTP extends MFARecovery implements MFAProvider {

    private String id;
    private static final Logger logger = Logger.getLogger(WhatsappOTP.class);


    public WhatsappOTP(String id) {
        this.id = id;
    }

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappOTP:PreLogin");

        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return;

        showView(configModel, context, null, true,false);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappOTP:PostLogin");

        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return false;

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst(RESEND_OTP) != null && Boolean.parseBoolean(formData.getFirst(RESEND_OTP))) {
            showView(configModel, context, null, true,false);
            return false;
        }

        return processData(configModel, context, false);
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappOTP:PostRegistration");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return false;
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst(RESEND_OTP) != null && Boolean.parseBoolean(formData.getFirst(RESEND_OTP))) {
            showView(configModel, context, null, true,true);
            return false;
        }

        return processData(configModel, context, true);
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:WhatsappOTP:PreRegistration");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return;
        showView(configModel, context, null, true,true);
    }

    private void showView(MFAConfigModel configModel, RequiredActionContext context, String error, boolean triggerNewOTP, boolean isReg) {
        try {
            if (triggerNewOTP) {
                String expiryTime = GeneralHelper.getConfigValue(configModel, Constants.VERIFICATION_EXPIRY_CONFIG, "5m");
                if (expiryTime == null || expiryTime.isEmpty()) {
                    expiryTime = "5m";
                }
                context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP_EXPIRY, String.valueOf(getExpiryDuration(expiryTime)));
                String otp = GeneralHelper.getOtp(Integer.parseInt(GeneralHelper.getConfigValue(configModel, PHONE_VER_OTP_LENGTH, "6")));
                context.getAuthenticationSession().setAuthNote(SESSION_MOBILE_OTP, otp);
                SmsSenderProvider smsSenderProvider = getSmsProvider(context);
                if (context.getRealm().isEnabledOwnSmsProvider()) {
                    smsSenderProvider.setProviderId("whatsapp");
                } else {
                    SmsConfigModel smsConfigModel = GeneralHelper.getDefaultWhatsappSender(context.getSession());
                    if (smsConfigModel == null) {
                        redirectToError(context, "whatsapp-not-config");
                        return;
                    }
                    smsSenderProvider.setProviderId(GeneralHelper.getDefaultWhatsappSender(context.getSession()).getSmsProviderId());
                }

                Map<String, Object> smsPayload = new HashMap<>();
                List<String> datas = new ArrayList<>();
                datas.add(GeneralHelper.getCompanyName(context) + " MFA ");
                datas.add(otp);
                smsPayload.put("templateType", "OTP");
                smsPayload.put("messageParts", datas.toArray());

                try {
                    UserModel userModel = context.getUser();
                    if(userModel.getMobileNumber() == null){
                        logger.error("No mobile number is associated with this user. Please contact admin.");
                        redirectToError(context, "no-mobile-num-user");
                        return;
                    }
                    smsSenderProvider.sendSMS(getPhoneNumber(context), null, smsPayload);
                } catch (SmsException e) {
                    e.printStackTrace();
                    redirectToError(context, e.getMessage());
                    return;
                }
            }

            LoginFormsProvider formsProvider = context.form();

            formsProvider.setAttribute("mfaProviderId", id);
            formsProvider.setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY));
            if (error != null) {
                formsProvider.addError(new FormMessage(error));
            }

            if (!isReg) {
                formsProvider
                        .setAttribute("showTroubleShoot", isMFARecoveryEnabled(context, configModel));
            }

            Response challenge = formsProvider
                    .setAttribute("title", isReg? "whatspp-two-factor-reg" : "whatspp-two-factor-verify")
                    .setAttribute("pageType","mfa")
                    .createForm("whatsapp-mfa-otp.ftl");
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

    private boolean processData(MFAConfigModel configModel, RequiredActionContext context, boolean isFromRegistration) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String otp = context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP);
        String otpExpiryTime = context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY);

        if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            showView(configModel, context, ErrorMappings.OTP_EXPIRED, false,isFromRegistration);
            return false;
        }

        if (formData.getFirst(Constants.FORM_OTP) == null || formData.getFirst(Constants.FORM_OTP).length() == 0) {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            showView(configModel, context, ErrorMappings.ENTER_OTP, false,isFromRegistration);
            return false;
        } else if (formData.getFirst(Constants.FORM_OTP).equals(otp)) {
            UserModel user = context.getUser();
            user.setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
            if (isFromRegistration) {
                OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), "test",context.getAuthenticationSession().getClient().getClientId());
                otpCredentialModel.setProviderId(id);
                otpCredentialModel.setClientId(context.getAuthenticationSession().getClient().getClientId());
                OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
                otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), otpCredentialModel);
            }
            getProtector(context).successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
            return true;
        } else {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            showView(configModel, context, ErrorMappings.WRONG_OTP_ERROR, false,isFromRegistration);
            return false;
        }
    }

    private String getPhoneNumber(RequiredActionContext context) throws SmsException {
        UserModel userModel = context.getUser();
        NumberFormat numberFormat = getSmsProvider(context).parseNumber(userModel.getMobileNumber(), userModel.getMobileNumberRegion().toUpperCase());
        return numberFormat.getE16Format();
    }

    protected boolean isTemporarilyDisabledByBruteForce(RequiredActionContext context, UserModel user) {
        if (context.getRealm().isBruteForceProtected()) {
            if (getProtector(context).isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
                Response challengeResponse = context.form()
                        .setAttribute(ERROR_MESSAGE, ErrorMappings.MAXIMUM_ATTEMPTS_REACHED_USER_TEMP_DISABLED)
                        .createForm("error.ftl");
                context.challenge(challengeResponse);
                return true;
            }
        }
        return false;
    }
}
