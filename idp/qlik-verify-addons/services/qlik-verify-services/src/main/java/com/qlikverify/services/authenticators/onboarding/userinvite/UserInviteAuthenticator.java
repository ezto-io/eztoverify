package com.qlikverify.services.authenticators.onboarding.userinvite;

import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.actiontoken.userinvite.UserInviteLinkActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.qlikverify.services.constants.Constants.*;

public class UserInviteAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(UserInviteAuthenticator.class);
    private static int year = Calendar.getInstance().get(Calendar.YEAR);
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String nextAuth = context.getAuthenticationSession().getAuthNote("next_auth");
        if(nextAuth != null && !nextAuth.equalsIgnoreCase(UserInviteAuthenticatorFactory.PROVIDER_ID))
        {
            context.success();
            return;
        }

        String emailProviderId = null;
        if (context.getRealm().isEnabledOwnEmailProvider()) {
            emailProviderId = GeneralHelper.getConfigValue(context, EMAIL_SENDER);
            if (emailProviderId == null || emailProviderId.isEmpty()){
                logger.error("Email Provider is not set for the authenticator");
                redirectToError(context,"email-not-config-error");
                return;
            }
        }
        context.getAuthenticationSession().removeAuthNote("next_auth");

        UserModel user = context.getUser();
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        UriInfo uriInfo = session.getContext().getUri();
        int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(UserInviteLinkActionToken.TOKEN_TYPE);
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
        ClientModel clientModel = session.getContext().getClient();
        UserInviteLinkActionToken token = new UserInviteLinkActionToken(user.getId(), user.getEmail(), absoluteExpirationInSecs, clientModel.getClientId());
        token.setNextAuthenticator(GeneralHelper.getConfigValue(context,"next_authenticator"));
        UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                clientModel.getClientId(), "");
        String link = builder.build(realm.getName()).toString();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenant", getRealmName(realm));
        attributes.put("user_name", user.getUsername());
        attributes.put("year", String.valueOf(year));
        attributes.put("link", link);
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        try {
            EmailTemplateProvider emailTemplateProvider = session.getProvider(EmailTemplateProvider.class);
            emailTemplateProvider
                    .setRealm(realm)
                    .setEmailProviderId(emailProviderId)
                    .send(user.getEmail(), "welcomeUserSubject", "welcome-user-email-link.ftl", attributes);
            String accessCode = context.generateAccessCode();

            Response challenge = context.form().createForm("user-invite.ftl");
            context.forceChallenge(challenge);
        } catch (EmailException e) {
            e.printStackTrace();
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // NOP
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

    private static String getRealmName(RealmModel realm){
        return !TextUtils.isEmpty(realm.getDisplayName()) ? realm.getDisplayName() : realm.getName();
    }

    private void redirectToError(AuthenticationFlowContext context, String message) {
        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, message)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }
}
