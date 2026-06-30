package org.keycloak.authentication.actiontoken.userinvite;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHander;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.AuthenticationFlowResolver;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.Objects;
import java.util.Optional;

import static org.keycloak.services.managers.AuthenticationManager.END_AFTER_REQUIRED_ACTIONS;
import static org.keycloak.services.resources.LoginActionsService.AUTHENTICATE_PATH;
import static org.keycloak.services.resources.LoginActionsService.REGISTRATION_PATH;

public class UserInviteLinkTokenHandler extends AbstractActionTokenHander<UserInviteLinkActionToken> {
    public UserInviteLinkTokenHandler() {
        super(
                UserInviteLinkActionToken.TOKEN_TYPE,
                UserInviteLinkActionToken.class,
                Messages.USER_INVITE_NOT_ALLOWED,
                EventType.USER_INVITE_LINK,
                Errors.NOT_ALLOWED
        );
    }

    @Override
    public TokenVerifier.Predicate<? super UserInviteLinkActionToken>[] getVerifiers(ActionTokenContext<UserInviteLinkActionToken> tokenContext) {
        return TokenUtils.predicates(
                TokenUtils.checkThat(
                        t -> Objects.equals(t.getEmail(), tokenContext.getAuthenticationSession().getAuthenticatedUser().getEmail()),
                        Errors.INVALID_EMAIL, getDefaultErrorMessage()
                )
        );
    }

    @Override
    public Response handleToken(UserInviteLinkActionToken token, ActionTokenContext<UserInviteLinkActionToken> tokenContext) {


        final RealmModel realm = tokenContext.getRealm();
        final KeycloakSession session = tokenContext.getSession();
        ClientModel clientModel = tokenContext.getAuthenticationSession().getClient();
        Optional<String> redirectUri = clientModel.getRedirectUris().stream().findFirst();
        if (redirectUri.isPresent()) {
            String redirectUrl = RedirectUtils.getFirstValidRedirectUri(session, clientModel.getRootUrl(), clientModel.getRedirectUris());
            tokenContext.getAuthenticationSession().setRedirectUri(redirectUrl);
        }
        AuthenticationFlowModel flow = getAuthenticationFlow(tokenContext.getAuthenticationSession());

        String flowId = flow.getId();
        AuthenticationProcessor processor = createProcessor(tokenContext.getAuthenticationSession(), flowId, REGISTRATION_PATH, session, realm, tokenContext.getEvent(), tokenContext.getRequest());

        if(processor.getAuthenticationSession().getAuthNote(END_AFTER_REQUIRED_ACTIONS) != null)
        {
            processor.getAuthenticationSession().removeAuthNote(END_AFTER_REQUIRED_ACTIONS);
        }
        processor.getAuthenticationSession().setAuthNote("next_auth", token.getNextAuthenticator());
        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        user.setEmailVerified(true);
        user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Accepted");

        processor.getAuthenticationSession().setAuthNote("user_invite", "true");

        return tokenContext.processFlow(
                false,
                REGISTRATION_PATH,
                flow,
                null,
                processor
        );
    }

    protected AuthenticationFlowModel getAuthenticationFlow(AuthenticationSessionModel authSession) {
        return AuthenticationFlowResolver.resolveBrowserFlow(authSession);
    }

    protected AuthenticationProcessor createProcessor(AuthenticationSessionModel authSession, String flowId, String flowPath, KeycloakSession session, RealmModel realmModel, EventBuilder event, HttpRequest httpRequest) {
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setAuthenticationSession(authSession)
                .setFlowPath(flowPath)
                .setFlowId(flowId)
                .setBrowserFlow(true)
                .setConnection(session.getContext().getConnection())
                .setEventBuilder(event)
                .setRealm(realmModel)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(httpRequest);

        authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, flowPath);

        return processor;
    }

    private Response sendError(KeycloakSession session, AuthenticationSessionModel authSession, String error) {
        return session.getProvider(LoginFormsProvider.class)
                .setAuthenticationSession(authSession)
                .setError(error)
                .setAttribute("instructionMessage", error)
                .createForm("error.ftl");
    }
}
