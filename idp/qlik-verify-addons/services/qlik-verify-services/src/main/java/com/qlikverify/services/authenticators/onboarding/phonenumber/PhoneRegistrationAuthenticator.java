package com.qlikverify.services.authenticators.onboarding.phonenumber;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.qlikverify.services.constants.Constants.*;

public class PhoneRegistrationAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(PhoneRegistrationAuthenticator.class);
    private SmsSenderProvider smsSenderProvider;

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        String nextAuth = context.getAuthenticationSession().getAuthNote("next_auth");
        if(nextAuth != null && !nextAuth.equalsIgnoreCase(PhoneRegistrationAuthenticatorFactory.PROVIDER_ID)) {
            context.success();
            return;
        }
        context.getAuthenticationSession().removeAuthNote("next_auth");

        logger.info("QlikVerify:PhoneRegistrationAuthenticator:Authenticate");
        showView(context, null, PHONE_NUMBER_REGISTRATION_FTL);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:PhoneRegistrationAuthenticator:Action");
        if(!GeneralHelper.checkSMSConfigured(context)){
            GeneralHelper.handleConfigurationError(context, EventType.PHONE_ONBOARDING_ERROR, ERROR_FTL, "sms-config-missing-error");
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (Boolean.parseBoolean(formData.getFirst(FORM_EDIT))) {
            Response challenge = context.form()
                    .setAttribute("defReg", context.getRealm().getDefaultRegion())
                    .setAttribute(PHONE_NUMBER_REGION,context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION))
                    .setAttribute(ATTRIBUTE_OLD_PHONE_NUMBER, context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER))
                    .setAttribute(OTP_LENGTH_ATTR, context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") != null ? context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") : 6)
                    .createForm(PHONE_NUMBER_REGISTRATION_FTL);
            context.challenge(challenge);
            return;
        }


        if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
            formResend(context);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_PHONE_REGISTRATION))) {
            formRegistration(context, formData);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_OTP_VERIFICATION))) {
            formOtpVerification(context, formData);
        }

    }

    private void formOtpVerification(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        String mobNum = context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION) + context.getAuthenticationSession().getAuthNote(FORM_PHONE_NUMBER);
        UserModel existingUserWithMobile = context.getSession().users().getUserByMobile(mobNum, context.getRealm());

        if (existingUserWithMobile != null) {
            showView(context, ErrorMappings.PHONE_ALREADY_REGISTERED, PHONE_NUMBER_REGISTRATION_FTL);
            return;
        }

        String otpExpiryTime = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY);
        context.form().setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime);

        if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.OTP_EXPIRED))
                    .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE))
                    .setAttribute(OTP_LENGTH_ATTR, context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") != null ? context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") : 6)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;
        }

        if (formData.getFirst(FORM_OTP) == null || formData.getFirst(FORM_OTP).length() == 0) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.ENTER_OTP))
                    .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE))
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                    .setAttribute(OTP_LENGTH_ATTR, context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") != null ? context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") : 6)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;
        }

        String otp = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP);

        if (!formData.getFirst(FORM_OTP).equals(otp)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.WRONG_OTP_ERROR))
                    .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE))
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                    .setAttribute(OTP_LENGTH_ATTR, context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len"))
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;
        }
        context.getAuthenticationSession().setAuthNote(UserModel.MOBILE_NUMBER_REGION,context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION));
        context.getAuthenticationSession().setAuthNote(UserModel.MOBILE_NUMBER,context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER));
        if ("true".equalsIgnoreCase(GeneralHelper.getConfigValue(context, SKIP_USER_CREATION, "false"))){
            context.success();
            return;
        }
        if(context.getUser() == null) {
            UserModel user = context.getSession().users().addUser(context.getRealm(), context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER));
            user.setEnabled(true);
            context.setUser(user);
        }
        context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER_REGION,context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION));
        context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER,context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER));
        context.getUser().setSingleAttribute(ATTRIBUTE_PHONE_VERIFIED, "TRUE");
        context.success();
    }

    private void formRegistration(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        NumberFormat numberFormat = GeneralHelper.parseMobileNumber(context,formData.getFirst(PHONE_NUMBER_REGION).toUpperCase(),formData.get(FORM_PHONE_NUMBER).get(1));
        if(numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")){
            showView(context, ErrorMappings.PHONE_NUMBER_INVALID, PHONE_NUMBER_REGISTRATION_FTL);
            return;
        }

        String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length()+1);
        if (phoneNumber != null && phoneNumber.length() > 0) {
            String mobNum = formData.getFirst(PHONE_NUMBER_REGION) + phoneNumber;
            UserModel existingUserWithMobile = context.getSession().users().getUserByMobile(mobNum, context.getRealm());
            UserModel existingUserWithUsername = context.getSession().users().getUserByUsername(context.getRealm(),phoneNumber);

            if (existingUserWithMobile != null) {
                showView(context, ErrorMappings.PHONE_ALREADY_REGISTERED, PHONE_NUMBER_REGISTRATION_FTL);
                return;
            }
            if(existingUserWithUsername != null) {
                GeneralHelper.auditEvent(context, EventType.PHONE_ONBOARDING_ERROR, "Conflict in user phone number and username");
                return;
            }

            String expiryTime = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
            if(!GeneralHelper.sendSmsOtp(context, numberFormat.getE16Format(),  expiryTime, GeneralHelper.getConfigValue(context, SMS_SENDER))){
                showView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP, PHONE_NUMBER_REGISTRATION_FTL);
                return;
            }

            context.getAuthenticationSession().setAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE, numberFormat.getE16Format());
            context.getAuthenticationSession().setAuthNote(PHONE_NUMBER_REGION, formData.getFirst(PHONE_NUMBER_REGION));
            context.getAuthenticationSession().setAuthNote(ATTRIBUTE_PHONE_NUMBER, phoneNumber);

            Response challenge = context.form()
                    .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, numberFormat.getE16Format())
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY))
                    .setAttribute(OTP_LENGTH_ATTR, context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") != null ? context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") : 6)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
        } else {
            showView(context, ErrorMappings.PHONE_NUMBER_INVALID, PHONE_NUMBER_REGISTRATION_FTL);
        }
    }

    private void formResend(AuthenticationFlowContext context) {
        String expiryTime = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
        if(!GeneralHelper.sendSmsOtp(context, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE), expiryTime,GeneralHelper.getConfigValue(context,SMS_SENDER))){
            showView(context,ErrorMappings.FAILED_TO_SEND_SMS_OTP,OTP_VERIFICATION_FTL);
            return;
        }
        Response challenge = context.form()
                .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE))
                .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY))
                .setAttribute(OTP_LENGTH_ATTR, context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") != null ? context.getAuthenticatorConfig().getConfig().get("sms_ver_otp_len") : 6)
                .createForm(OTP_VERIFICATION_FTL);
        context.challenge(challenge);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    private void showView(AuthenticationFlowContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        formsProvider.setAttribute("defReg", context.getRealm().getDefaultRegion());
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    private void redirectToError(AuthenticationFlowContext context, String message) {
        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, message)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }

}
