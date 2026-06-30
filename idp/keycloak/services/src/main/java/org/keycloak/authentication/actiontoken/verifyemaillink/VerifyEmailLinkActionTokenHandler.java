package org.keycloak.authentication.actiontoken.verifyemaillink;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHander;
import org.keycloak.authentication.actiontoken.ActionTokenConstants;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.messages.Messages;

import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import java.util.*;

public class VerifyEmailLinkActionTokenHandler extends AbstractActionTokenHander<VerifyEmailLinkActionToken> {


    public VerifyEmailLinkActionTokenHandler(){
        super(
                VerifyEmailLinkActionToken.TOKEN_TYPE,
                VerifyEmailLinkActionToken.class,
                Messages.STALE_VERIFY_EMAIL_LINK,
                EventType.VERIFY_EMAIL,
                Errors.INVALID_TOKEN
        );
    }


    @Override
    public Response handleToken(VerifyEmailLinkActionToken token, ActionTokenContext<VerifyEmailLinkActionToken> tokenContext) {

        String realmId = token.getComp_name();
        String adminEmailId = token.getEmail();

        RealmManager realmManager = new RealmManager(tokenContext.getSession());

        //checking if tenantID is not null
        if (realmId.trim().isEmpty()) {
            return sendError(tokenContext.getSession(), tokenContext.getAuthenticationSession(), "Company name should not be null");
        }

        //checking if tenant is already present or not
        if(realmManager.getRealm(realmId) != null || realmManager.getRealmByName(realmId) != null) {
            return sendError(tokenContext.getSession(), tokenContext.getAuthenticationSession(), "Company name Already Present");
        } else {
            AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
            if (tokenContext.isAuthenticationSessionFresh()) {
                // Update the authentication session in the token
                String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
                token.setCompoundAuthenticationSessionId(authSessionEncodedId);
            }

           //create Realm with company Name
            Response action  = createRealmwithTenantAdmin(token,tokenContext,tokenContext.getSession(),realmId,adminEmailId);

            return action;
        }
    }
    private Response createRealmwithTenantAdmin(VerifyEmailLinkActionToken token,ActionTokenContext<VerifyEmailLinkActionToken> tokenContext, KeycloakSession sess, String realmId, String adminEmailId){

        KeycloakSession session = sess.getKeycloakSessionFactory().create();
        KeycloakTransactionManager tx =  session.getTransactionManager();
        tx.begin();
        try {
            //creating realm
            RealmManager realmManager = new RealmManager(session);
            RealmModel createdRealm = realmManager.createRealm(realmId);

            //setUp Realm Defaults
            createdRealm.setEnabled(true);
            createdRealm.setResetPasswordAllowed(true);

            RealmRepresentation realmRep= new RealmRepresentation();
            realmRep.setRealm(realmId);
            //create Admin User for that Realm
            RealmModel realm = session.realms().getRealmByName(realmId);

            if (session.users().getUsersCount(realm) > 0) {
                throw new IllegalStateException("Can't create initial user as users already exists");
            }

            UserModel adminUser = session.users().addUser(realm, adminEmailId);
            adminUser.setEmail(adminEmailId);
            adminUser.setEnabled(true);


            //Set RealmAdmin Role
            RoleModel role = createdRealm.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT).getRole(ActionTokenConstants.ADMIN_USER_ROLE);
            adminUser.grantRole(role);

            RepresentationToModel.importRealm(session, realmRep, realm, false);

            adminUser.setEmail(adminEmailId);
            adminUser.setEmailVerified(true);

            //Creating Action
            ActionModel am = new ActionModel();
            am.setId(UUID.randomUUID().toString());
            am.setAlias(UUID.randomUUID().toString());
            am.setRealmId(realmId);
            RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
            requiredAction.setAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            requiredAction.setName(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            requiredAction.setProviderId(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            requiredAction.setDefaultAction(false);
            requiredAction.setPriority(1);
            requiredAction.setEnabled(true);
            requiredAction.setActionId(am.getId());
            realm.addRequiredActionProvider(requiredAction);


            //Client Setup
            ClientModel cliModel = realm.getClientByClientId(Constants.CREATE_REALM_CLIENT_ID);
            cliModel.setAuthenticationFlowBindingOverride("action", am.getId());
            cliModel.setRedirectUris(Collections.singleton("*"));

            AuthenticationSessionModel authSession;
            authSession = createAuthenticationSessionForClient(session, realm, cliModel.getClientId());
            authSession.setRedirectUri(session.getContext().getUri().getBaseUri() + "admin/" + realmId + "/console");
            String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
            token.setCompoundAuthenticationSessionId(authSessionEncodedId);
            tokenContext.setAuthenticationSession(authSession, true);

            session.getContext().setRealm(realm);
            session.getContext().setAuthenticationSession(authSession);
            tokenContext.getAuthenticationSession().setAuthenticatedUser(adminUser);
            session.getContext().getAuthenticationSession().setAuthenticatedUser(adminUser);
            session.getContext().getAuthenticationSession().setAuthNote("requestFromWebsite", "true");
            authSession.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            String nextAction = AuthenticationManager.nextRequiredAction(session, authSession, tokenContext.getRequest(), tokenContext.getEvent());

            return AuthenticationManager.redirectToRequiredActions(
                    session,
                    session.getContext().getRealm(),
                    authSession,
                    session.getContext().getUri(),
                    nextAction);
        }
        catch (Exception e) {
            tx.setRollbackOnly();
            throw new RuntimeException(e);
        } finally {
            close(session);
        }

    }

    protected void close(KeycloakSession session) {
        KeycloakTransactionManager tx = session.getTransactionManager();
        if (tx.isActive()) {
            if (tx.getRollbackOnly()) {
                tx.rollback();
            } else {
                tx.commit();
            }
        }

        session.close();
    }


    private Response sendError(KeycloakSession session, AuthenticationSessionModel authSession, String error){
        return  session.getProvider(LoginFormsProvider.class)
                .setAuthenticationSession(authSession)
                .setError(error)
                .setAttribute("instructionMessage", error)
                .createForm("error.ftl");
    }

   private AuthenticationSessionModel createAuthenticationSessionForClient(KeycloakSession session,RealmModel realm,String clientID)
            throws UriBuilderException, IllegalArgumentException {
        AuthenticationSessionModel authSession;

        ClientModel client = session.clients().getClientByClientId(realm, clientID);
        String redirectUri;

        if (client == null) {
            client = SystemClientUtil.getSystemClient(realm);
            redirectUri = Urls.accountBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName()).toString();
        } else {
            redirectUri = RedirectUtils.getFirstValidRedirectUri(session, client.getRootUrl(), client.getRedirectUris());
        }

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, true);
        authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        //authSession.setNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setRedirectUri(redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
        authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

        return authSession;
    }

}

