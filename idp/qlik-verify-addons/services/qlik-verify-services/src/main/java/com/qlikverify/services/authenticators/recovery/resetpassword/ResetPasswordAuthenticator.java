package com.qlikverify.services.authenticators.recovery.resetpassword;


import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sms.models.NumberFormat;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.qlikverify.services.constants.Constants.*;

public class ResetPasswordAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(ResetPasswordAuthenticator.class);


    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:ResetPasswordAuthenticator:Authenticate");

        String chosenType = context.getAuthenticationSession().getUserSessionNotes().get(CHOSEN_TYPE);
        if (chosenType != null) {
            logger.info("Resetting flow");
            context.resetFlow();
            return;
        }

        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null){
            throw new RuntimeException("Authenticator config is null");
        }
        Map<String, String> config = authenticatorConfig.getConfig();
        if (config == null) throw new RuntimeException(" Reset ways are null");

        Set<String> availableAuthentications = getAvailableAuthentications(context);
        if(availableAuthentications.isEmpty()) {
            GeneralHelper.redirectToError(context, ErrorMappings.NO_AVAILABLE_WAYS_TO_RESET);
            return;
        }

        LoginFormsProvider formsProvider = context.form();
        String ftl = RESET_PASSWORD_CHOOSE_WAY_FTL;
        if(availableAuthentications.size() == 1){
            formsProvider.setAttribute(availableAuthentications.stream().findFirst().get(), "true");
            String availableAuthentication = availableAuthentications.stream().findFirst().get();
            if (availableAuthentication.equalsIgnoreCase(RP_WITH_MOB_NUM)) {
                context.getAuthenticationSession().setUserSessionNote(CHOSEN_TYPE, RP_WITH_MOB_NUM);
                formsProvider.setAttribute("defReg", context.getRealm().getDefaultRegion());
                ftl = RESET_PHONE_NUMBER_FTL;
            } else if (availableAuthentication.equalsIgnoreCase(RP_WITH_EMAIL_LINK)){
                context.getAuthenticationSession().setUserSessionNote(CHOSEN_TYPE, RP_WITH_EMAIL_LINK);
                ftl = RESET_PASSWORD_FIND_USER_FTL;
            } else {
                context.getAuthenticationSession().setUserSessionNote(CHOSEN_TYPE, RP_WITH_EMAIL_OTP);
                ftl = RESET_PASSWORD_FIND_USER_FTL;
            }
        } else {
            availableAuthentications.forEach(x -> formsProvider.setAttribute(x, "true"));
        }
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:ResetPasswordAuthenticator:Action");

        //Check user is disabled by brute force attack
        if(context.getUser() != null && GeneralHelper.isTemporarilyDisabledByBruteForce(context, context.getUser(),null)){
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst(CHOSEN_TYPE) != null && formData.size() == 1){
            context.clearUser();
        }

        if (context.getUser() == null ){
            if(!noUser(context, formData))
                return;
        }

        String chosenType = context.getAuthenticationSession().getUserSessionNotes().get(CHOSEN_TYPE);
        if(chosenType != null ){
            if (formData.containsKey(EMAIL) && formData.get(EMAIL).stream().count() > 0){
                UserModel user = context.getSession().users().getUserByEmail(context.getRealm(), formData.get("email").get(0));
                if (user == null){
                    showView(context, ErrorMappings.EMAIL_NOT_REGISTERED,null,RESET_PASSWORD_FIND_USER_FTL);
                    return;
                }
            }

            if (formData.containsKey(PHONE_NUMBER) && formData.get(PHONE_NUMBER).stream().count() > 0){
                List<String> phoneNumber = formData.get(PHONE_NUMBER);
                List<String> phoneNumberRegion = formData.get(PHONE_NUMBER_REGION);
                UserModel user = context.getSession().users().getUserByMobile(phoneNumberRegion.get(0)+phoneNumber.get(1), context.getRealm());
                if (user == null){
                    showView(context, ErrorMappings.PHONE_NUMBER_NOT_REGISTERED,null,RESET_PHONE_NUMBER_FTL);
                    return;
                }
                context.setUser(user);
            }

            executeAction(context,formData, chosenType);
        } else {
            String email = formData.getFirst(EMAIL);

            if (email == null || email.length() == 0) {
                showView(context, ErrorMappings.EMAIL_REQUIRED, null,RESET_PASSWORD_FIND_USER_FTL);
                return;
            } else if (!GeneralHelper.isValidEmail(email)) {
                Response challenge = context.form()
                        .addError(new FormMessage(ErrorMappings.EMAIL_INVALID))
                        .setAuthenticationSession(context.getAuthenticationSession())
                        .createForm(RESET_PASSWORD_FIND_USER_FTL);
                context.challenge(challenge);
                return;
            }

            UserModel user = context.getSession().users().getUserByEmail(context.getRealm(), email);
            if (user == null){
                showView(context, null,null,RESET_PASSWORD_FIND_USER_FTL);
                return;
            }

            context.setUser(user);
            Set<String> availableWays = getAvailableAuthentications(context);
            if (availableWays.isEmpty()) {
                GeneralHelper.redirectToError(context, ErrorMappings.NO_AVAILABLE_WAYS_TO_RESET);
            }else {
                LoginFormsProvider formsProvider = context.form();
                formsProvider.setAuthenticationSession(context.getAuthenticationSession());
                availableWays.forEach(x -> formsProvider.setAttribute(x, "true"));
                Response challenge = formsProvider.createForm(RESET_PASSWORD_CHOOSE_WAY_FTL);
                context.challenge(challenge);
            }
        }
    }

    private boolean noUser(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        if ((formData.getFirst(CHOSEN_TYPE) != null  && formData.getFirst(CHOSEN_TYPE).equalsIgnoreCase(RP_WITH_MOB_NUM)) || formData.containsKey("phoneNumber")) {
            context.getAuthenticationSession().setUserSessionNote(CHOSEN_TYPE, RP_WITH_MOB_NUM);
            if (!formData.containsKey("phoneNumber"))
            {
                showView(context, null,null,RESET_PHONE_NUMBER_FTL);
                return false;
            }
        } else if (formData.getFirst(CHOSEN_TYPE) != null){
            context.getAuthenticationSession().setUserSessionNote(CHOSEN_TYPE, formData.getFirst(CHOSEN_TYPE));
            showView(context, null,null,RESET_PASSWORD_FIND_USER_FTL);
            return false;
        }
        return true;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    private void executeAction(AuthenticationFlowContext context,MultivaluedMap<String, String> formData,String option){
        String verificationExpiryDuration = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
        if(context.getUser() == null && formData.containsKey("email")) {
            UserModel userModel = context.getSession().users().getUserByEmail(context.getRealm(), formData.getFirst("email"));
            context.setUser(userModel);
        }
        switch(option){
            case RP_WITH_EMAIL_OTP:
                if (Boolean.parseBoolean(formData.getFirst(FORM_IS_RESET_OTP))){
                    if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
                        if (!GeneralHelper.sendResetPasswordEmailOTP(context, context.getUser(), verificationExpiryDuration)) {
                            showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL,null, RESET_PASSWORD_OTP_FTL);
                            return;
                        }
                        showView(context, null, RP_WITH_EMAIL_OTP, RESET_PASSWORD_OTP_FTL);
                    } else {
                        String otp = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP);
                        String otpExpiryTime = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY);
                        if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
                            context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                            showView(context, ErrorMappings.OTP_EXPIRED, RP_WITH_EMAIL_OTP, RESET_PASSWORD_OTP_FTL);
                            return;
                        }
                        if (formData.getFirst(FORM_OTP) == null || formData.getFirst(FORM_OTP).length() == 0) {
                            showView(context, ErrorMappings.ENTER_OTP, RP_WITH_EMAIL_OTP, RESET_PASSWORD_OTP_FTL);
                            return;
                        } else if (formData.getFirst(FORM_OTP).equals(otp)) {
                            context.getProtector().successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
                            addUpdatePasswordRA(context);
                            context.success();
                        } else {
                            context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                            showView(context, ErrorMappings.WRONG_OTP_ERROR,RP_WITH_EMAIL_OTP, RESET_PASSWORD_OTP_FTL);
                        }
                    }
                } else {
                    if (!GeneralHelper.sendResetPasswordEmailOTP(context, context.getUser(), verificationExpiryDuration)) {
                        showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, null,RESET_PASSWORD_FIND_USER_FTL);
                        return;
                    }
                    showView(context, null,RP_WITH_EMAIL_OTP,RESET_PASSWORD_OTP_FTL);
                }
                break;
            case RP_WITH_EMAIL_LINK:
                if (!sendResetPasswordEmail(context)) {
                    GeneralHelper.redirectToError(context, ErrorMappings.FAILED_TO_SEND_EMAIL);
                    return;
                }
                context.forceChallenge(context.form()
                                .setAttribute("email", formData.getFirst(EMAIL))
                        .createForm(RESET_PASSWORD_EMAIL_LINK_SENT_FTL));
                break;

            case RP_WITH_MOB_NUM:
                if (Boolean.parseBoolean(formData.getFirst(FORM_IS_RESET_OTP))) {
                    if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
                        if (!GeneralHelper.sendSmsOtp(context, verificationExpiryDuration,GeneralHelper.getConfigValue(context, SMS_SENDER))) {
                            showView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP, null, RESET_PASSWORD_OTP_FTL);
                            return;
                        }
                        showView(context, null, RP_WITH_MOB_NUM, RESET_PASSWORD_OTP_FTL);
                    } else {
                        String otp = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP);
                        String otpExpiryTime = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY);
                        if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
                            context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                            showView(context, ErrorMappings.OTP_EXPIRED,RP_WITH_MOB_NUM,RESET_PASSWORD_OTP_FTL);
                            return;
                        }
                        if (formData.getFirst(FORM_OTP) == null || formData.getFirst(FORM_OTP).length() == 0) {
                            showView(context, ErrorMappings.ENTER_OTP, RP_WITH_MOB_NUM,RESET_PASSWORD_OTP_FTL);
                            return;
                        } else if (formData.getFirst(FORM_OTP).equals(otp)) {
                            context.getProtector().successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
                            addUpdatePasswordRA(context);
                            context.success();
                        } else {
                            context.getProtector().failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                            showView(context, ErrorMappings.WRONG_OTP_ERROR, RP_WITH_MOB_NUM, RESET_PASSWORD_OTP_FTL);
                        }
                    }
                } else {
                    if (!GeneralHelper.sendSmsOtp(context, verificationExpiryDuration,GeneralHelper.getConfigValue(context, SMS_SENDER))) {
                        showView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP,RP_WITH_MOB_NUM, RESET_PHONE_NUMBER_FTL);
                        return;
                    }
                    showView(context, null,RP_WITH_MOB_NUM, RESET_PASSWORD_OTP_FTL);
                }
                break;
            default:
                context.resetFlow();
                break;
        }
    }

    private void showView(AuthenticationFlowContext context, String error,String chosenType,String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }
        if (chosenType != null && chosenType.length() > 0) {
            formsProvider.setAttribute(CHOSEN_TYPE, chosenType);
            if (chosenType.equals(RP_WITH_EMAIL_OTP)) {
                formsProvider.setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY));

                String maskedEmail = context.getUser().getEmail();
                try {
                    maskedEmail = GeneralHelper.maskEmailAddress(maskedEmail);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                formsProvider.setAttribute(ADDITIONAL_INFO_TEXT, ADDITIONAL_INFO_TEXT_EMAIL_OTP);
                formsProvider.setAttribute("maskedEmail",maskedEmail);
            } else if(chosenType.equals(RP_WITH_MOB_NUM)){
                formsProvider.setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY));

                String phoneNumber = context.getUser().getMobileNumber();
                String region = context.getUser().getMobileNumberRegion();

                NumberFormat numberFormat = GeneralHelper.parseMobileNumber(context,region.toUpperCase(),phoneNumber);
                if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
                    GeneralHelper.redirectToError(context, ErrorMappings.PHONE_NUMBER_INVALID);
                    return;
                }

                String maskedPhoneNumber = GeneralHelper.maskPhoneNumber(context, phoneNumber, region.toUpperCase());
                if (maskedPhoneNumber == null || maskedPhoneNumber.length() == 0) {
                    GeneralHelper.redirectToError(context, ErrorMappings.PHONE_NUMBER_INVALID);
                    return;
                }
                formsProvider.setAttribute(ADDITIONAL_INFO_TEXT, ADDITIONAL_INFO_TEXT_MOBILE_OTP);
                formsProvider.setAttribute("maskedPhoneNumber",maskedPhoneNumber);

            }
        }

        if (ftl.equalsIgnoreCase(RESET_PHONE_NUMBER_FTL)){
            formsProvider.setAttribute("defReg", context.getRealm().getDefaultRegion());
        }
        formsProvider.setAuthenticationSession(context.getAuthenticationSession());
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }



    protected boolean isTemporarilyDisabledByBruteForce(AuthenticationFlowContext context, UserModel user) {
        if (context.getRealm().isBruteForceProtected() && context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
                Response challengeResponse = context.form()
                        .setAttribute(ERROR_MESSAGE, ErrorMappings.MAXIMUM_ATTEMPTS_REACHED_USER_TEMP_DISABLED)
                        .createForm(ERROR_FTL);
                context.forceChallenge(challengeResponse);
                return true;
        }
        return false;
    }

    private boolean sendResetPasswordEmail(AuthenticationFlowContext context){
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();

        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
            if (emailProviderId == null || emailProviderId.isEmpty()){
                logger.error("Email Provider is not set for the authenticator");
                return false;
            }
        }

        /*EmailConfigModel emailConfigModel = null;

        if(context.getRealm().isEnabledOwnEmailProvider()){
            emailConfigModel = context.getRealm().getEmailConfigByProviderId(emailProvider);
        }*/

        String username = authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);

        // we don't want people guessing usernames, so if there was a problem obtaining the user, the user will be null.
        // just reset login for with a success message
        if (user == null) {
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return false;
        }


        EventBuilder event = context.getEvent();
        // we don't want people guessing usernames, so if there is a problem, just continuously challenge
        if (user.getEmail() == null || user.getEmail().trim().length() == 0) {
            event.user(user)
                    .detail(Details.USERNAME, username)
                    .error(Errors.INVALID_EMAIL);

            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return false;
        }

        int validityInSecs = context.getRealm().getActionTokenGeneratedByUserLifespan(ResetCredentialsActionToken.TOKEN_TYPE);
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        // We send the secret in the email in a link as a query param.
        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authenticationSession).getEncodedId();
        ResetCredentialsActionToken token = new ResetCredentialsActionToken(user.getId(),user.getEmail(), absoluteExpirationInSecs, authSessionEncodedId, authenticationSession.getClient().getClientId());
        String link = UriBuilder
                .fromUri(context.getActionTokenUrl(token.serialize(context.getSession(), context.getRealm(), context.getUriInfo())))
                .build()
                .toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);
        try {
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setRealm(context.getRealm()).setUser(user)
                    .setAuthenticationSession(authenticationSession)
                    .setEmailProviderId(emailProviderId)
                    .sendPasswordReset(link, expirationInMinutes);

            event.clone().event(EventType.SEND_RESET_PASSWORD)
                    .user(user)
                    .detail(Details.USERNAME, username)
                    .detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, authenticationSession.getParentSession().getId()).success();
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return true;
        } catch (EmailException e) {
            event.clone().event(EventType.SEND_RESET_PASSWORD)
                    .detail(Details.USERNAME, username)
                    .user(user)
                    .error(Errors.EMAIL_SEND_FAILED);
            ServicesLogger.LOGGER.failedToSendPwdResetEmail(e);
            Response challenge = context.form()
                    .setError(Messages.EMAIL_SENT_ERROR)
                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
            return false;
        }
    }

    //method used to get the ways to perform reset password action
    private Set<String> getAvailableAuthentications(AuthenticationFlowContext context) {
        Set<String> authTypes = new HashSet<>();

            if (Boolean.parseBoolean(GeneralHelper.getConfigValue(context, RP_USING_EMAIL_OTP)))
                authTypes.add(RP_WITH_EMAIL_OTP);
            if (Boolean.parseBoolean(GeneralHelper.getConfigValue(context, RP_USING_EMAIL_LINK)))
                authTypes.add(RP_WITH_EMAIL_LINK);

        if (Boolean.parseBoolean(GeneralHelper.getConfigValue(context, RP_USING_MOBILE_NUM))) {
            authTypes.add(RP_WITH_MOB_NUM);
        }

        return authTypes;
    }

    private boolean checkUserHasValidEmail(UserModel user){
        if(user == null)
            return false;

        return user.getEmail() != null && user.getEmail().length() > 0 && user.isEmailVerified();
    }

    private boolean checkUserHasValidMobNum(UserModel user){
        if(user == null)
            return false;

        return user.getMobileNumber() != null && user.getMobileNumber().length() > 0 && Boolean.parseBoolean(user.getFirstAttribute(ATTRIBUTE_PHONE_VERIFIED));
    }

    private void addUpdatePasswordRA(AuthenticationFlowContext context){
        //Creating Action
        ActionModel am = new ActionModel();
        am.setId(UUID.randomUUID().toString());
        am.setAlias(UUID.randomUUID().toString());
        am.setRealmId(context.getRealm().getId());
        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setName(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setProviderId(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setDefaultAction(false);
        requiredAction.setPriority(1);
        requiredAction.setEnabled(true);
        requiredAction.setActionId(am.getId());
        if(context.getAuthenticatorConfig() != null && context.getAuthenticatorConfig().getConfig().get("passwordPolicy") != null && context.getSession().policies().getPolicyByName(context.getRealm(), context.getAuthenticatorConfig().getConfig().get("passwordPolicy")) != null) {
            requiredAction.setPolicyId(context.getSession().policies().getPolicyByName(context.getRealm(), context.getAuthenticatorConfig().getConfig().get("passwordPolicy")).getId());
            context.getAuthenticationSession().setAuthNote("recovery_password_policy_id", context.getSession().policies().getPolicyByName(context.getRealm(), context.getAuthenticatorConfig().getConfig().get("passwordPolicy")).getId());
        }
        requiredAction = context.getRealm().addRequiredActionProvider(requiredAction);

        context.getAuthenticationSession().addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.name());
    }

}
