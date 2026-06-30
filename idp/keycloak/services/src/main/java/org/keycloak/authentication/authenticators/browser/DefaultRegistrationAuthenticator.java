package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.utils.Constants;
import org.keycloak.authentication.authenticators.utils.GeneralHelper;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.exceptions.LicenseException;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

public class DefaultRegistrationAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(DefaultRegistrationAuthenticator.class);


    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:DefaultRegistrationAuthenticator:Authenticate");

        showView(context, null, Constants.ONBOARD_EMAIL_FORM_FTL);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:DefaultRegistrationAuthenticator:Action");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (Boolean.parseBoolean(formData.getFirst("isEmailRegistration"))) {
            String email = formData.getFirst("email");

            //check email isEmpty
            if (email == null || email.length() == 0) {
                showView(context, Constants.EMAIL_REQUIRED, Constants.ONBOARD_EMAIL_FORM_FTL);
                return;
            }

            //check email isValid
            if (!GeneralHelper.isValidEmail(email)) {
                Response challenge = context.form()
                        .addError(new FormMessage(Constants.EMAIL_INVALID))
                        .createForm(Constants.ONBOARD_EMAIL_FORM_FTL);
                context.challenge(challenge);
                return;
            }

            //check user present with given emailID
            UserModel userByEmail = context.getSession().users().getUserByEmail(email, context.getRealm());

            if (userByEmail != null) {
                showView(context, Constants.EMAIL_REGISTERED, Constants.ONBOARD_EMAIL_FORM_FTL);
                return;
            }

            if (context.getRealm().getEmailConfigsStream().count() == 0) {
                if (context.getSession().realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getEmailConfigsStream().count() == 0) {
                    GeneralHelper.redirectToErrorPage(context,Constants.EMAIL_PROVIDER_NOT_CONFIGURED);
                }
            }

            if(!addUser(context,email)){
                GeneralHelper.redirectToErrorPage(context,Constants.USER_ONBOARD_FAILED);
            }
            context.success();
        }
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

    }

    @Override
    public void close() {

    }


    private void showView(AuthenticationFlowContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }

        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    private boolean addUser(AuthenticationFlowContext context,String email){
        //check userwith Same Name present to avoid PrimaryKeyViolation
        UserModel isUserPresent = context.getSession().users().getUserByUsername(email, context.getRealm());
        if (isUserPresent != null) {
            logger.error(Constants.USER_EXISTS_SAME_EMAIL);
            showView(context,Constants.USER_EXISTS_SAME_EMAIL,Constants.ONBOARD_EMAIL_FORM_FTL);
            return false;
        }
        UserModel user = context.getSession().users().addUser(context.getRealm(), email);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);

        if(!createCredAndTriggerEmail(context.getSession(), context.getRealm(), user))
            return false;

        context.setUser(user);
        return true;
    }


    private boolean createCredAndTriggerEmail(KeycloakSession session, RealmModel realm, UserModel user) {
        //Making userModel into userRep
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(user.getUsername());
        userRepresentation.setId(user.getId());
        userRepresentation.setFirstName(user.getFirstName());
        userRepresentation.setLastName(user.getLastName());
        userRepresentation.setEmail(user.getEmail());

        //Settings oneTimePassword into userRep
        List<CredentialRepresentation> credentials = new ArrayList<>();
        CredentialRepresentation pass = new CredentialRepresentation();
        pass.setType(PASSWORD);
        pass.setValue(HmacOTP.generateSecret(10));
        pass.setTemporary(true);
        credentials.add(pass);
        userRepresentation.setCredentials(credentials);

        //creating Credentials using userRep
        RepresentationToModel.createCredentials(userRepresentation, session, realm, user, true);

        //sending Email
        if(!sendEmail(session,realm,user,pass.getValue())){
            return false;
        }

        return true;
    }

    private boolean sendEmail(KeycloakSession session,RealmModel realm,UserModel user,String passValue){

        //setting up attributes to pass into emailTemplate
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenant", realm.getName());
        attributes.put("user_name", user.getUsername());
        attributes.put("temporary_password", passValue);

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .send(user.getEmail(), "welcomeUserSubject", "welcome-user-email.ftl", attributes);
        } catch (EmailException e) {
            logger.error(e);
            return false;
        }
        catch (ConfigException e)
        {
            EventBuilder eventBuilder = new EventBuilder(realm,session,session.getContext().getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
        }

        return true;
    }
}
