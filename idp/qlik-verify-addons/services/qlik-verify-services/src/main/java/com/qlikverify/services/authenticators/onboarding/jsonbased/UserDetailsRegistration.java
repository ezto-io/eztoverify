package com.qlikverify.services.authenticators.onboarding.jsonbased;

import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.qlikverify.services.constants.Constants.CONFIG_JSON;
import static org.apache.http.util.TextUtils.isEmpty;

public class UserDetailsRegistration implements Authenticator {
    private static final Logger logger = Logger.getLogger(UserDetailsRegistration.class);
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String CONFIRM_PASSWORD = "confirmPassword";
    private static final String PHONE_NUMBER = "phoneNumber";
    private static final String PHONE_NUMBER_REGION = "phoneNumberRegion";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String COUNTRY_CODE = "countryCode";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("qlikverify-services : onboarding : jsonbased : UserDetailsRegistration : authenticate");
        LoginFormsProvider form = context.form();
        String configJson = GeneralHelper.getConfigValue(context,CONFIG_JSON);

        if(TextUtils.isBlank(configJson)){
            GeneralHelper.redirectToErrorPage(context, ErrorMappings.JSON_CONFIG_NOTVALID_ERROR);
        }


        /** sets configJson in AuthSession if presents */
        if(!isEmpty(configJson)) {
            form.setAttribute(CONFIG_JSON, configJson);
            GeneralHelper.setFormAttrs(context, form , GeneralHelper.jsonResponseRepToModel(configJson));
            context.getAuthenticationSession().setAuthNote(CONFIG_JSON,configJson);
        }

        Response challenge = form
                .createForm("user-details-reg.ftl");
        context.challenge(challenge);
    }
    public UserModel validateEmailCreateUser(AuthenticationFlowContext context, MultivaluedMap<String, String> formData, LoginFormsProvider form){
        String email = formData.getFirst(EMAIL);
        if(TextUtils.isEmpty(email)) {
            form.addError(new FormMessage(ErrorMappings.EMAIL_INVALID));
            redirectToPage(context,form);
            return null;
        }

        if(context.getSession().users().getUserByEmail(context.getRealm(),email) != null ||
                context.getSession().users().getUserByUsername(context.getRealm(),email) != null){
            form.addError(new FormMessage(ErrorMappings.EMAIL_REGISTERED));
            redirectToPage(context,form);
            return null;
        }

        formData.remove(EMAIL);
        String password = "";
        if(formData.containsKey(PASSWORD) && formData.containsKey(CONFIRM_PASSWORD)){
            password = formData.getFirst(PASSWORD);
            String confirmPassword = formData.getFirst(CONFIRM_PASSWORD);

            if(!password.equals(confirmPassword)){
                form.addError(new FormMessage(ErrorMappings.PASSWORD_MISMATCH_ERROR));
                redirectToPage(context,form);
                return null;
            }
        }

        UserModel user = context.getSession().users().addUser(context.getRealm(), email);
        user.setEmail(email);
        user.setEnabled(true);

        if(!TextUtils.isBlank(password)){
            context.getSession().userCredentialManager()
                    .updateCredential(context.getRealm(), user, UserCredentialModel.password(password));
        }
        return user;
    }

    public boolean validateMobile(AuthenticationFlowContext context, MultivaluedMap<String, String> formData, LoginFormsProvider form){
        String phone = formData.get(PHONE_NUMBER).get(1);
        String phoneRegion = formData.getFirst(PHONE_NUMBER_REGION);
        if(TextUtils.isEmpty(phone)) {
            form.addError(new FormMessage(ErrorMappings.PHONE_NUMBER_INVALID));
            redirectToPage(context,form);
            return false;
        }

        String mobNum = phoneRegion+phone;
        if(context.getSession().users().getUserByMobile(mobNum, context.getRealm()) != null ||
                context.getSession().users().getUserByUsername(context.getRealm(), mobNum) != null){
            form.addError(new FormMessage(ErrorMappings.PHONE_ALREADY_REGISTERED));
            redirectToPage(context,form);
            return false;
        }

        String password = "";
        if(formData.containsKey(PASSWORD) && formData.containsKey(CONFIRM_PASSWORD)){
            password = formData.getFirst(PASSWORD);
            String confirmPassword = formData.getFirst(CONFIRM_PASSWORD);

            if(!password.equals(confirmPassword)){
                form.addError(new FormMessage(ErrorMappings.PASSWORD_MISMATCH_ERROR));
                redirectToPage(context,form);
                return false;
            }
        }
        return  true;
    }

    public UserModel createUserWithMobile(AuthenticationFlowContext context, MultivaluedMap<String, String> formData, LoginFormsProvider form){
        if(!validateMobile(context, formData, form))
            return null;
        String phone = formData.get(PHONE_NUMBER).get(1);
        String password = formData.getFirst(PASSWORD);

        UserModel user = context.getSession().users().addUser(context.getRealm(), phone);
        user.setEnabled(true);

        if (!TextUtils.isBlank(password)) {
            context.getSession().userCredentialManager()
                    .updateCredential(context.getRealm(), user, UserCredentialModel.password(password));
        }

        //set  the user in context
        context.setUser(user);
        context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER_REGION, formData.getFirst(PHONE_NUMBER_REGION));
        context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER, formData.get(PHONE_NUMBER).get(1));
        formData.remove(COUNTRY_CODE);
        formData.remove(PHONE_NUMBER_REGION);
        formData.remove(PHONE_NUMBER);
        return user;
    }

    public boolean createUserWithEmailPhone(AuthenticationFlowContext context, MultivaluedMap<String, String> formData, LoginFormsProvider form){
        UserModel user = validateEmailCreateUser(context, formData, form);
        if(user == null)
            return false;
        if(user != null && validateMobile(context, formData, form)) {
            context.setUser(user);
            context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER_REGION,formData.getFirst(PHONE_NUMBER_REGION));
            context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER,formData.get(PHONE_NUMBER).get(1));
            formData.remove(COUNTRY_CODE);
            formData.remove(PHONE_NUMBER_REGION);
            formData.remove(PHONE_NUMBER);
        }
        else {
            context.getSession().users().removeUser(context.getRealm(), user);
            return false;
        }
        return true;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("qlikverify-services : onboarding : jsonbased : UserDetailsRegistration : action");

        LoginFormsProvider form = context.form();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if(!formData.containsKey(EMAIL) && !formData.containsKey(PHONE_NUMBER)){
            form.addError(new FormMessage(ErrorMappings.EMAIL_AND_PHONE_MISSING_ERROR));
            redirectToPage(context,form);
            return;
        }

        if(formData.containsKey(EMAIL) && formData.containsKey(PHONE_NUMBER)){
            logger.debug("UserDetailsRegistration : action : email and phone registration");
            if(!createUserWithEmailPhone(context,formData,form))
                return;
        } else if (formData.containsKey(EMAIL)) {
            logger.debug("UserDetailsRegistration : action : email registration");
            UserModel user = validateEmailCreateUser(context, formData, form);
            if(user != null)
                context.setUser(user);
            else
                return;
        } else {
            logger.debug("UserDetailsRegistration : action : phone registration");
            if(createUserWithMobile(context, formData, form) == null)
                return;
        }

        if(context.getUser() == null){
            form.addError(new FormMessage(ErrorMappings.USER_REQUIRED));
            redirectToPage(context,form);
            return;
        }

        if(formData.containsKey(FIRST_NAME)){
            context.getUser().setFirstName(formData.getFirst(FIRST_NAME));
            formData.remove(FIRST_NAME);
        }

        if(formData.containsKey(LAST_NAME)){
            context.getUser().setLastName(formData.getFirst(LAST_NAME));
            formData.remove(LAST_NAME);
        }
        formData.remove(PASSWORD);
        formData.remove(CONFIRM_PASSWORD);

        formData.forEach( (k,v) -> context.getUser().setAttribute(k,v));

        context.success();
    }

    public void redirectToPage(AuthenticationFlowContext context, LoginFormsProvider form){
        GeneralHelper.setFormAttrs(context, form , GeneralHelper.jsonResponseRepToModel(
                context.getAuthenticationSession().getAuthNote(CONFIG_JSON)));

        Response challenge = form
                .setAttribute("defReg", context.getRealm().getDefaultRegion())
                .createForm("user-details-reg.ftl");
        context.challenge(challenge);
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
}
