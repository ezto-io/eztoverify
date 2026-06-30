package com.qlikverify.extensions.admin.controllers.ssodashboard.rest;

import com.qlikverify.extensions.admin.misc.models.Constants;
import com.qlikverify.extensions.admin.misc.responses.QlikVerifyResponse;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;

import static org.keycloak.services.managers.AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE;
import static org.keycloak.services.managers.AuthenticationManager.expireIdentityCookie;

@Produces({MediaType.APPLICATION_JSON})
@Consumes(MediaType.APPLICATION_JSON)
public class SSODashboardResource implements RealmResourceProvider {

    //~ Static fields/initializers =========================================================================================================

    private final KeycloakSession session;
    private UserModel userModel = null;
    private static final Logger logger = Logger.getLogger(SSODashboardResource.class);

    //~ Constructor ========================================================================================================================

    public SSODashboardResource(KeycloakSession session) {
        this.session = session;
        this.session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
    }

    //~ Methods ============================================================================================================================

    @Override
    public Object getResource() {
        return this;
    }

    //~ ====================================================================================================================================

    @Override
    public void close() {
        // No implementation required
    }

    //~ API ================================================================================================================================

    @GET
    @NoCache
    @Path("{realm_name}/user/{user_name}")
    public Response getUserSSODashboardData(@Context HttpRequest request, final @PathParam("realm_name") String realmName,
                        final @PathParam("user_name") String userName) {
        Response response = preConditioningCheck(realmName, userName);
        if (response != null)
            return response;
        RealmModel realmModel = session.realms().getRealmByName(realmName);
        UserModel user = session.users().getUserByUsername(realmModel, userName);
        if(!validateToken(request, realmModel, user))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        return new SSODashboardResourceFunctions(session, userModel).getUserSSODashboardData();
    }

    //~ ====================================================================================================================================

    @POST
    @NoCache
    @Path("{realm_name}/user/{user_name}")
    public Response updateUserSSODashboardData(@Context HttpRequest request, final @PathParam("realm_name") String realmName,
                                               final @PathParam("user_name") String userName,
                                               final String detailsJson) {
        Response response = preConditioningCheck(realmName, userName);
        if (response != null)
            return response;
        RealmModel realmModel = session.realms().getRealmByName(realmName);
        UserModel user = session.users().getUserByUsername(realmModel, userName);
        if(!validateToken(request, realmModel, user))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        return new SSODashboardResourceFunctions(session, userModel).updateUserSSODashboardData(detailsJson);
    }

    private boolean validateToken (HttpRequest request, RealmModel realm, UserModel user){
        try {
            HttpHeaders httpHeaders = request.getHttpHeaders();
            if(httpHeaders == null || httpHeaders.getCookies() == null || httpHeaders.getCookies().isEmpty()) {
                expireIdentityCookie(realm, session.getContext().getUri(), session.getContext().getConnection());
                return false;
            }
            Cookie cookie = CookieHelper.getCookie(httpHeaders.getCookies(), KEYCLOAK_IDENTITY_COOKIE);
            if (cookie == null) return false;
            String tokenString = cookie.getValue();

            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()))
                    .checkActive(false)
                    .checkTokenType(false)
                    .withChecks( new TokenVerifier.TokenTypeCheck(TokenUtil.TOKEN_TYPE_KEYCLOAK_ID));

            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();

            SignatureVerifierContext signatureVerifier;
            signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);

            AccessToken token = verifier.verify().getToken();
            UserSessionModel cookieSession = session.sessions().getUserSession(realm, token.getSessionState());
            if(cookieSession == null || session.sessions().getUserSessionsStream(realm, user).noneMatch(x -> x.getId().equals(cookieSession.getId()))) {
                expireIdentityCookie(realm, session.getContext().getUri(), session.getContext().getConnection());
                return false;
            }
            return true;


        } catch (VerificationException e) {
            logger.error("exception occured during verifying token, " + e.getMessage());
            return false;
        }
    }

    //~ ====================================================================================================================================

    private Response preConditioningCheck(String realmName, String userName) {
        if (Validation.isBlank(realmName) && Validation.isBlank(userName)) {
            return Response.status(Response.Status.PRECONDITION_FAILED)
                    .entity(new QlikVerifyResponse<>("RealmId or UserId is missing in the path param",
                            Response.Status.PRECONDITION_FAILED.name())).build();
        }
        RealmModel realmModel = session.realms().getRealmByName(realmName);
        if (realmModel == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new QlikVerifyResponse<>("Realm not Found", Constants.BAD_REQUEST)).build();
        }
        userModel = session.users().getUserByUsername(realmModel, userName);
        if (userModel == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new QlikVerifyResponse<>("User not Found", Constants.BAD_REQUEST)).build();
        }
        session.getContext().setRealm(realmModel);
        return null;
    }
}
