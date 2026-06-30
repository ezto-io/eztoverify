package com.qlikverify.services.authenticators.login.phonenumber;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
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

public class PhoneNumberLoginAuthenticator implements Authenticator{
    private static final Logger logger = Logger.getLogger(PhoneNumberLoginAuthenticator.class);
    private SmsSenderProvider smsSenderProvider;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:PhoneNumberFormAuthenticator:Authenticate");

        showView(context, null);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:PhoneNumberFormAuthenticator:Action");

        //Check user is disabled by brute force attack
        if(context.getUser() != null && GeneralHelper.isTemporarilyDisabledByBruteForce(context, context.getUser(),null))
            return;

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String region = formData.getFirst(PHONE_NUMBER_REGION);
        String mobNumber = formData.get(FORM_PHONE_NUMBER).get(1);
        if(mobNumber == null || region == null){
            showView(context, ErrorMappings.PHONE_NUMBER_REQUIRED);
        }

        NumberFormat numberFormat = GeneralHelper.parseMobileNumber(context,region.toUpperCase(),mobNumber);
        if(numberFormat == null || numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")){
            showView(context, ErrorMappings.PHONE_NUMBER_INVALID);
            return;
        }

        String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length()+1);
        if(phoneNumber==null || phoneNumber.length() == 0) {
            showView(context, ErrorMappings.PHONE_NUMBER_REQUIRED);
        }
        String mobNum = region + phoneNumber;

        //Here we check if there is user in context that should match the provided mobile number
        if(context.getUser() != null) {
            String sessionUserMobile = context.getUser().getMobileNumberRegion() + context.getUser().getMobileNumber();
            if(!sessionUserMobile.equals(mobNum)) {
                Response challenge = context.form()
                        .addError(new FormMessage(ErrorMappings.PHONE_NUMBER_INVALID))
                        .createForm(PHONE_NUMBER_FORM_FTL);
                context.challenge(challenge);
                return;
            }
        }

        UserModel userModel = context.getSession().users().getUserByMobile(mobNum, context.getRealm());
        if (userModel == null) {
            showView(context, ErrorMappings.PHONE_NUMBER_NOT_REGISTERED);
            return;
        }

        String checkUserState = validateUser(context, userModel);
        if (!checkUserState.equals(Constants.OK)) {
            GeneralHelper.redirectToErrorPage(context,checkUserState);
            return;
        }

        context.setUser(userModel);
        context.getAuthenticationSession().setAuthNote(LOGIN_WITH, PHONE_NUMBER);
        context.getAuthenticationSession().setAuthNote(LOGIN_VALUE, formData.getFirst(ATTRIBUTE_PHONE_NUMBER));
        context.success();
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

    private void showView(AuthenticationFlowContext context, String error) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }
        //set def region attribute
        formsProvider.setAttribute("defReg", context.getRealm().getDefaultRegion());
        Response challenge = formsProvider.createForm(PHONE_NUMBER_FORM_FTL);
        context.challenge(challenge);
    }
    private String validateUser(AuthenticationFlowContext context, UserModel user) {
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(Constants.USER_DISABLED);
            return Constants.USER_DISABLED;
        }
        if (context.getRealm().isBruteForceProtected() && context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
            context.getEvent().user(user);
            context.getEvent().error(Constants.USER_DISABLED_BRUTEFORCE);
            return Constants.USER_DISABLED_BRUTEFORCE;
        }
        return Constants.OK;
    }
}
