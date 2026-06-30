package com.qlikverify.services.authenticators.login.multiloginoptions;

import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sms.models.NumberFormat;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static com.qlikverify.services.constants.Constants.*;

public class MultipleLoginOptionsAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(MultipleLoginOptionsAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:MultipleLoginOptionsAuthenticator:Authenticate");

        if(context.getAuthenticationSession().getClient().getName().equals("dashboard") && context.getRealm().getMode().equals("bo") &&
                context.getUser().getRoleMappingsStream().filter(role -> role.getName().equals("workspace-admin") || role.getName().equals("admin")).count() == 0){
             context.failure(AuthenticationFlowError.ACCESS_DENIED);
             return;
        }

        //Check user is disabled by brute force attack
        if (GeneralHelper.isTemporarilyDisabledByBruteForce(context, context.getUser(),null))
            return;

        String loginWith = context.getAuthenticationSession().getAuthNote(LOGIN_WITH);
        String loginValue = context.getAuthenticationSession().getAuthNote(LOGIN_VALUE);
        String isMagicLinkLogin = context.getAuthenticationSession().getAuthNote(MAGIC_LINK_ACTION);

        if (loginWith == null || loginWith.length() == 0 || loginValue == null || loginValue.length() == 0) {
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.INVALID_REQUEST);
            return;
        }

        if (Boolean.parseBoolean(isMagicLinkLogin)) {
            magicLinkAuthentication(context);
            return;
        }
        if(context.getUser() == null) {
            context.resetFlow();
            return;
        }

        //get available authentications set in config
        List<String> availableAuthentications = getAvailableAuthentications(context);

        if (availableAuthentications.isEmpty()) {
            logger.error(ErrorMappings.AUTHENTICATORS_NULL);
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.AUTHENTICATORS_NULL);
            return;
        }

        if (availableAuthentications.size() == 1) {
            availableAuthentication(availableAuthentications, context, loginWith, loginValue);
        } else {
            logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Authenticate:Multiple Login Options available");
            LoginFormsProvider formsProvider = context.form();
            formsProvider.setAttribute(loginWith, loginValue);
            availableAuthentications.forEach(x -> formsProvider.setAttribute(x, "true"));
            Response challenge = formsProvider.createForm(MULTIPLE_LOGIN_OPTIONS_FTL);
            context.challenge(challenge);
        }
    }

    private void availableAuthentication(List<String> availableAuthentications, AuthenticationFlowContext context, String loginWith, String loginValue) {
        String availableAuthenticator =  availableAuthentications.get(0);
        switch (availableAuthenticator) {
            case IS_EMAIL_OTP_LOGIN:
                isEmailOtpLogin(context, loginWith, loginValue);
                break;
            case IS_EMAIL_MAGIC_LINK_LOGIN:
                isEmailMagicLinkLogin(context);
                break;
            case IS_MOBILE_OTP_LOGIN:
                isMobileOtpLogin(context, loginWith, loginValue);
                break;
            case IS_EMAIL_MOBILE_OTP:
                isEmailMobileOtp(context, loginWith, loginValue);
                break;
            case IS_PASSWORD_LOGIN:
                isPasswordLogin(context, loginWith, loginValue);
                break;
            default:
                GeneralHelper.redirectToErrorPage(context, ErrorMappings.INVALID_REQUEST);
                break;
        }
    }

    private void isPasswordLogin(AuthenticationFlowContext context, String loginWith, String loginValue) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Authenticate:Password Login");
        String errorInPasswordLogin = GeneralHelper.checkPasswordLoginRestrictions(context,loginWith);
        if(errorInPasswordLogin != null){
            GeneralHelper.redirectToErrorPage(context, errorInPasswordLogin);
            return;
        }
        showView(context, null, loginWith, loginValue, IS_PASSWORD_LOGIN, LOGIN_PASSWORD_FTL);
    }

    private void isEmailMobileOtp(AuthenticationFlowContext context, String loginWith, String loginValue) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Authenticate:Otp login via email and mobile");
        if(!GeneralHelper.checkEmailConfigured(context, false)){
            GeneralHelper.handleConfigurationError(context, EventType.EMAIL_LOGIN_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
            return;
        }
        if(!GeneralHelper.checkSMSConfigured(context)){
            GeneralHelper.handleConfigurationError(context, EventType.PHONE_LOGIN_ERROR, ERROR_FTL, SMS_CONFIG_MISSING_ERROR);
            return;
        }
        if(!GeneralHelper.sendOtpToEmailAndPhone(context)) {
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.AUTHENTICATORS_NULL);
            return;
        }
        context.form()
                .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_MOBILE_OTP_EXPIRY));
        showView(context, null, loginWith, loginValue, IS_EMAIL_MOBILE_OTP, MULTI_LOGIN_OTP_FTL);
    }

    private void isEmailOtpLogin(AuthenticationFlowContext context, String loginWith, String loginValue) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Authenticate:Email Otp Login");
        if(!GeneralHelper.checkEmailConfigured(context, false)){
            GeneralHelper.handleConfigurationError(context, EventType.EMAIL_LOGIN_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
            return;
        }
        if (!GeneralHelper.sendEmailOTP(context, context.getUser().getEmail(), GeneralHelper.getConfigValue(context, EMAIL_VER_EXPIRY_CONFIG))) {
            showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, loginWith, loginValue, IS_EMAIL_OTP_LOGIN, EMAIL_FORM_FTL);
            return;
        }
        showView(context, null, loginWith, loginValue, IS_EMAIL_OTP_LOGIN, MULTI_LOGIN_OTP_FTL);
    }

    private void isEmailMagicLinkLogin(AuthenticationFlowContext context){
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Authenticate:Email Magic Link Login");
        if(!GeneralHelper.checkEmailConfigured(context, false)){
            GeneralHelper.handleConfigurationError(context, EventType.EMAIL_LOGIN_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
            return;
        }
        context.getAuthenticationSession().setAuthNote(MAGIC_LINK_ACTION, "true");
        if (!GeneralHelper.sendEmailMagicLink(context, context.getUser(), GeneralHelper.getConfigValue(context, EMAIL_VER_EXPIRY_CONFIG))) {
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.FAILED_TO_SEND_EMAIL);
            return;
        }
        context.forceChallenge(context.form()
                .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_MAGIC_LINK_EXPIRY))
                .createForm(VIEW_EMAIL_FTL));
    }

    private void isMobileOtpLogin(AuthenticationFlowContext context, String loginWith, String loginValue) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Authenticate:Mobile Otp Login");
        if(!GeneralHelper.checkSMSConfigured(context)){
            GeneralHelper.handleConfigurationError(context, EventType.PHONE_LOGIN_ERROR, ERROR_FTL, SMS_CONFIG_MISSING_ERROR);
            return;
        }
        if (!GeneralHelper.sendSmsOtp(context, GeneralHelper.getConfigValue(context, MOB_VER_EXPIRY_CONFIG),GeneralHelper.getConfigValue(context, SMS_SENDER))) {
            showView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP, loginWith, loginValue, IS_MOBILE_OTP_LOGIN, PHONE_NUMBER_FORM_FTL);
            return;
        }
        showView(context, null, loginWith, loginValue, IS_MOBILE_OTP_LOGIN, MULTI_LOGIN_OTP_FTL);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:MultipleLoginOptionsAuthenticator:Action");


        //Check user is disabled by brute force attack
        if (GeneralHelper.isTemporarilyDisabledByBruteForce(context, context.getUser(),null))
            return;

        UserModel user      = context.getUser();
        String loginWith    = context.getAuthenticationSession().getAuthNote(LOGIN_WITH);
        String loginValue   = context.getAuthenticationSession().getAuthNote(LOGIN_VALUE);

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String chosenType = formData.getFirst("chosenType");

        if (chosenType == null) {
            context.getRefreshExecutionUrl();
            return;
        }

        switch (chosenType) {
            case IS_PASSWORD_LOGIN:
                isPassowrdLoginAction(context, loginWith, loginValue, formData, chosenType);
                break;
            case IS_EMAIL_OTP_LOGIN:
                isEmailOtpLoginAction(context, loginWith, loginValue, chosenType, formData, user);
                break;
            case IS_EMAIL_MAGIC_LINK_LOGIN:
                isEmailMagicLinkLoginAction(context, user);
                break;
            case IS_MOBILE_OTP_LOGIN:
                isMobileOtpLoginAction(context, loginWith, loginValue, chosenType, formData);
                break;
            case IS_EMAIL_MOBILE_OTP:
                isEmailMobileOtpAction(context, loginWith, loginValue, formData, user, chosenType);
                break;
            default:
                context.resetFlow();
                break;
        }
    }

    private void isEmailMobileOtpAction(AuthenticationFlowContext context, String loginWith, String loginValue, MultivaluedMap<String, String> formData, UserModel user, String chosenType) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Action:Otp login via email and mobile");
        if(!GeneralHelper.checkEmailConfigured(context, false)){
            GeneralHelper.handleConfigurationError(context, EventType.EMAIL_LOGIN_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
            return;
        }
        if(!GeneralHelper.checkSMSConfigured(context)){
            GeneralHelper.handleConfigurationError(context, EventType.PHONE_LOGIN_ERROR, ERROR_FTL, SMS_CONFIG_MISSING_ERROR);
            return;
        }

        validateEmailAndSmsOtp(context, formData, user, loginWith, loginValue, chosenType);
    }

    private void isMobileOtpLoginAction(AuthenticationFlowContext context, String loginWith, String loginValue, String chosenType, MultivaluedMap<String, String> formData) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Action:Mobile Otp Login");
        if(!GeneralHelper.checkSMSConfigured(context)){
            GeneralHelper.handleConfigurationError(context, EventType.PHONE_LOGIN_ERROR, ERROR_FTL, "sms-config-missing-error");
            return;
        }

        if(validateSmsOtp(formData,context,loginWith,loginValue,chosenType, formData.getFirst(FORM_OTP)).equals("continue")){
            context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            showView(context, ErrorMappings.WRONG_OTP_ERROR, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
        }
    }

    private void isEmailMagicLinkLoginAction(AuthenticationFlowContext context, UserModel user) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Action:Email Magic Link Login");
        if(!GeneralHelper.checkEmailConfigured(context, false)){
            GeneralHelper.handleConfigurationError(context, EventType.EMAIL_LOGIN_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
            return;
        }
        context.getAuthenticationSession().setAuthNote(MAGIC_LINK_ACTION, "true");
        if (!GeneralHelper.sendEmailMagicLink(context, user, GeneralHelper.getConfigValue(context, EMAIL_VER_EXPIRY_CONFIG))) {
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.FAILED_TO_SEND_EMAIL);
            return;
        }
        context.forceChallenge(context.form()
                .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_MAGIC_LINK_EXPIRY))
                .createForm(VIEW_EMAIL_FTL));
    }

    private void isEmailOtpLoginAction(AuthenticationFlowContext context, String loginWith, String loginValue, String chosenType, MultivaluedMap<String, String> formData, UserModel user) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Action:Email Otp Login");
        if(!GeneralHelper.checkEmailConfigured(context, false)){
            GeneralHelper.handleConfigurationError(context, EventType.EMAIL_LOGIN_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
            return;
        }
        if(validateEmailOtp(formData,context,loginWith,loginValue,chosenType,user, formData.getFirst(FORM_OTP)).equals(CONTINUE)){
            context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            showView(context, ErrorMappings.WRONG_OTP_ERROR, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
        }
    }

    private void isPassowrdLoginAction(AuthenticationFlowContext context, String loginWith, String loginValue, MultivaluedMap<String, String> formData, String chosenType) {
        logger.debug("QlikVerify:MultipleLoginOptionsAuthenticator:Action:Password Login");
        String errorInPasswordLogin = GeneralHelper.checkPasswordLoginRestrictions(context,loginWith);
        if(errorInPasswordLogin != null){
            GeneralHelper.redirectToErrorPage(context, errorInPasswordLogin);
            return;
        }
        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_LOGIN_PASSWORD))) {
            String password = formData.getFirst(FORM_PASSWORD);
            if (TextUtils.isEmpty(password)) {
                showView(context, ErrorMappings.PASSWORD_REQUIRED, loginWith, loginValue, chosenType, LOGIN_PASSWORD_FTL);
                return;
            }
            if(!GeneralHelper.isPasswordValid(context, context.getUser(), password)) {
                context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                showView(context, ErrorMappings.PASSWORD_INCORRECT, loginWith, loginValue, chosenType, LOGIN_PASSWORD_FTL);
                return;
            }
            context.getProtector().successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
            context.success();
        } else {
            showView(context, null, loginWith, loginValue, chosenType, LOGIN_PASSWORD_FTL);
        }
    }

    private void validateEmailAndSmsOtp(AuthenticationFlowContext context, MultivaluedMap<String, String> formData, UserModel user, String loginWith, String loginValue, String chosenType){
        String expiry = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_MOBILE_OTP_EXPIRY);
        String emailOtp = formData.getFirst(FORM_OTP);
        String mobileOtp = formData.getFirst(FORM_OTP);

        if(!GeneralHelper.getConfigValue(context,"sameOtp").equals("true")) {
            emailOtp = formData.getFirst("emailotp");
            mobileOtp = formData.getFirst("mobileotp");
        }
        if(Boolean.parseBoolean(formData.getFirst(FORM_IS_LOGIN_OTP))) {
            if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
                if (!GeneralHelper.sendOtpToEmailAndPhone(context)) {
                    showView(context, ErrorMappings.FAILED_TO_SEND_OTP, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
                }
                showView(context, null, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
            }
            else{
                if (!validateEmailOtp(formData, context, loginWith, loginValue, chosenType, user, expiry, emailOtp).equals("true") || !validateSmsOtp(formData, context, loginWith, loginValue, chosenType, expiry, mobileOtp).equals("true")) {
                    context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                    showView(context, ErrorMappings.WRONG_OTP_ERROR, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
                }
            }
        }else {
            if (!GeneralHelper.sendOtpToEmailAndPhone(context)) {
                showView(context, ErrorMappings.FAILED_TO_SEND_OTP, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
            }
            showView(context, null, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
        }
    }

    private String validateEmailOtp(MultivaluedMap<String, String> formData, AuthenticationFlowContext context, String loginWith, String loginValue, String chosenType, UserModel user, String formOTP){
        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_LOGIN_OTP))) {
            if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
                if (!GeneralHelper.sendEmailOTP(context, context.getUser().getEmail(), GeneralHelper.getConfigValue(context, EMAIL_VER_EXPIRY_CONFIG))) {
                    showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
                }
                showView(context, null, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
            } else {
                String otp = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP);
                String otpExpiryTime = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY);
                if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
                    context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                    showView(context, ErrorMappings.OTP_EXPIRED, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
                    return FALSE;
                }
                if (formOTP == null || formOTP.length() == 0) {
                    showView(context, ErrorMappings.ENTER_OTP, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
                } else if (formOTP.equals(otp)) {
                    if (context.getRealm().getMode() != null && !user.isEmailVerified()) {
                        user.setEmailVerified(true);
                        user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Accepted");
                    }
                    if (user.isEmailVerified() && user.getFirstAttribute(UserModel.USER_INVITE_STATUS) != null
                            && !user.getFirstAttribute(UserModel.USER_INVITE_STATUS).equals(
                            "Accepted")) {
                        user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Accepted");
                    }
                    context.getProtector().successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
                    context.success();
                } else {
                    return CONTINUE;
                }
            }
        } else {
            if (!GeneralHelper.sendEmailOTP(context, user.getEmail(), GeneralHelper.getConfigValue(context, EMAIL_VER_EXPIRY_CONFIG))) {
                showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
            }
            showView(context, null, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
        }
        return "true";
    }

    private String validateEmailOtp(MultivaluedMap<String, String> formData, AuthenticationFlowContext context, String loginWith, String loginValue, String chosenType, UserModel user, String otpExpiryTime, String formOTP){
        String otp = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP);
        if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
            context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            showView(context, ErrorMappings.OTP_EXPIRED, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
            return FALSE;
        }
        if (formOTP == null || formOTP.length() == 0) {
            showView(context, ErrorMappings.ENTER_OTP, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
        } else if (formOTP.equals(otp)) {
            context.getProtector().successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
            context.success();
        } else {
            return CONTINUE;
        }
        return "true";
    }

    private String validateSmsOtp(MultivaluedMap<String, String> formData, AuthenticationFlowContext context, String loginWith, String loginValue, String chosenType, String formOtp){
        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_LOGIN_OTP))) {
            if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
                if (!GeneralHelper.sendSmsOtp(context, GeneralHelper.getConfigValue(context, MOB_VER_EXPIRY_CONFIG),GeneralHelper.getConfigValue(context, SMS_SENDER))) {
                    showView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
                }
                showView(context, null, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
            } else {
                String otp = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP);
                String otpExpiryTime = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY);
                if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
                    context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                    showView(context, ErrorMappings.OTP_EXPIRED, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
                    return FALSE;
                }
                if (formOtp == null || formOtp.length() == 0) {
                    showView(context, ErrorMappings.ENTER_OTP, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
                } else if (formOtp.equals(otp)) {
                    context.getProtector().successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
                    context.success();
                } else {
                    return CONTINUE;
                }
            }
        } else {
            if (!GeneralHelper.sendSmsOtp(context, GeneralHelper.getConfigValue(context, MOB_VER_EXPIRY_CONFIG),GeneralHelper.getConfigValue(context, SMS_SENDER))) {
                showView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
            }
            showView(context, null, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
        }
        return "true";
    }

    private String validateSmsOtp(MultivaluedMap<String, String> formData, AuthenticationFlowContext context, String loginWith, String loginValue, String chosenType, String otpExpiryTime, String formOtp){
        String otp = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP);
        if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
            context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            showView(context, ErrorMappings.OTP_EXPIRED, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
            return FALSE;
        }
        if (formOtp == null || formOtp.length() == 0) {
            showView(context, ErrorMappings.ENTER_OTP, loginWith, loginValue, chosenType, MULTI_LOGIN_OTP_FTL);
        } else if (formOtp.equals(otp)) {
            context.getProtector().successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
            context.success();
        } else {
            return CONTINUE;
        }
        return "true";
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

    private void showView(AuthenticationFlowContext context, String error, String loginWith, String loginValue,
                          String chosenType, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }

        if ( !TextUtils.isEmpty(loginWith) && !TextUtils.isEmpty(loginValue)) {
            formsProvider.setAttribute(loginWith, loginValue);

            if (chosenType != null && chosenType.length() > 0) {
                formsProvider.setAttribute("chosenType", chosenType);
                if (chosenType.equals(IS_EMAIL_OTP_LOGIN)) {
                    if (error == null || !error.equals(ErrorMappings.OTP_EXPIRED))
                        formsProvider.setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY));
                    if (!loginWith.equals(EMAIL)) {
                        String maskedEmail = context.getUser().getEmail();
                        try {
                            maskedEmail = GeneralHelper.maskEmailAddress(maskedEmail);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                        formsProvider.setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY));
                        formsProvider.setAttribute(ADDITIONAL_INFO_TEXT, ADDITIONAL_INFO_TEXT_EMAIL_OTP);
                        formsProvider.setAttribute("maskedEmail",maskedEmail);
                    }
                    formsProvider.setAttribute(OTP_LENGTH_ATTR, GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH) != null ? GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH) : 6);
                } else if (chosenType.equals(IS_MOBILE_OTP_LOGIN)) {
                    if (error == null || !error.equals(ErrorMappings.OTP_EXPIRED))
                        formsProvider.setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY));
                    if (!loginWith.equals(PHONE_NUMBER)) {
                        String region = context.getUser().getMobileNumberRegion();
                        String phoneNumber = context.getUser().getMobileNumber();

                        NumberFormat numberFormat = GeneralHelper.parseMobileNumber(context,region.toUpperCase(),phoneNumber);
                        if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
                            GeneralHelper.redirectToErrorPage(context, ErrorMappings.PHONE_NUMBER_INVALID);
                            return;
                        }

                        String maskedPhoneNumber = GeneralHelper.maskPhoneNumber(context, phoneNumber, region.toUpperCase());
                        if (maskedPhoneNumber == null || maskedPhoneNumber.length() == 0) {
                            GeneralHelper.redirectToErrorPage(context, ErrorMappings.PHONE_NUMBER_INVALID);
                            return;
                        }
                        formsProvider.setAttribute(ADDITIONAL_INFO_TEXT, ADDITIONAL_INFO_TEXT_MOBILE_OTP);
                        formsProvider.setAttribute("maskedPhoneNumber",maskedPhoneNumber);
                    }
                    formsProvider.setAttribute(OTP_LENGTH_ATTR, GeneralHelper.getConfigValue(context, PHONE_VER_OTP_LENGTH) != null ? GeneralHelper.getConfigValue(context, PHONE_VER_OTP_LENGTH) : 6);
                }
                else if(chosenType.equals(IS_EMAIL_MOBILE_OTP)){
                    if (error == null || !error.equals(ErrorMappings.OTP_EXPIRED))
                        formsProvider.setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_MOBILE_OTP_EXPIRY));
                    if(!Boolean.parseBoolean(GeneralHelper.getConfigValue(context, "sameOtp"))) {
                        formsProvider
                                .setAttribute("difOTP", "enabled");
                    }
                    formsProvider.setAttribute(ADDITIONAL_INFO_TEXT, "otp-sent-to-email-phone");
                }
            }
        }

        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }


    private List<String> getAvailableAuthentications(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        List<String> authTypes = new ArrayList<>();

        boolean userHasEmail = user.getEmail() != null && user.getEmail().length() > 0;
        boolean userHasMobNo = user.getMobileNumber() != null && user.getMobileNumber().length() > 0;

        boolean isUserEmailVerified = GeneralHelper.isUserEmailVerified(user, context.getRealm());

        if (Boolean.parseBoolean(GeneralHelper.getConfigValue(context, EMAIL_OTP)) && userHasEmail && isUserEmailVerified)
            authTypes.add(IS_EMAIL_OTP_LOGIN);

        if (Boolean.parseBoolean(GeneralHelper.getConfigValue(context, EMAIL_ML)) && userHasEmail && isUserEmailVerified)
            authTypes.add(IS_EMAIL_MAGIC_LINK_LOGIN);

        if (Boolean.parseBoolean(GeneralHelper.getConfigValue(context, MOB_OTP)) && userHasMobNo) {
            authTypes.add(IS_MOBILE_OTP_LOGIN);
        }

        if(Boolean.parseBoolean(GeneralHelper.getConfigValue(context, EMAIL_PHONE_OTP)) && userHasEmail && userHasMobNo && isUserEmailVerified){
            authTypes.add(IS_EMAIL_MOBILE_OTP);
        }

        boolean isPasswordSet = context.getSession().userCredentialManager().isConfiguredFor(context.getRealm(), user, "password");
        if (isPasswordSet && Boolean.parseBoolean(GeneralHelper.getConfigValue(context, PASSWORD)) && isUserEmailVerified)
            authTypes.add(IS_PASSWORD_LOGIN);

        return authTypes;
    }

    private void magicLinkAuthentication(AuthenticationFlowContext context) {
        logger.info("QlikVerify:MultipleLoginOptionsAuthenticator:MagicLinkAuthentication");

        String sessionKey = context.getAuthenticationSession().getAuthNote("email-key");
        if (sessionKey != null) {
            String requestKey = context.getUriInfo().getQueryParameters().getFirst("key");
            if (requestKey != null) {
                if (requestKey.equals(sessionKey)) {
                    String linkExpiry = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_MAGIC_LINK_EXPIRY);
                    if (System.currentTimeMillis() > Long.parseLong(linkExpiry)) {
                        GeneralHelper.redirectToError(context, ErrorMappings.MAGIC_LINK_EXPIRED);
                    } else {
                        context.getProtector().successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
                        context.success();
                    }
                } else {
                    context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                    GeneralHelper.redirectToError(context, ErrorMappings.MAGIC_LINK_NOT_VALID);
                }
            } else {
                GeneralHelper.redirectToError(context, ErrorMappings.MAGIC_LINK_NOT_VALID);
            }
        } else {
            GeneralHelper.redirectToError(context, ErrorMappings.SOMETHING_WENT_WRONG);
        }
    }
}

