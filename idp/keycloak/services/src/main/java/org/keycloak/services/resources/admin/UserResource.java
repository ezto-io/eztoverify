/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.ActionTokenConstants;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.authentication.requiredactions.util.RequiredActionsValidator;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.util.UserTransactionStatus;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.*;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.account.AccountFormService;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.utils.GeneralHelper;
import org.keycloak.utils.ProfileHelper;
import org.keycloak.utils.StringUtil;

import javax.imageio.ImageIO;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.userprofile.UserProfileContext.USER_API;
import static org.keycloak.utils.GeneralHelper.*;

/**
 * Base resource for managing users
 *
 * @resource Users
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserResource {
    private static final Logger logger = Logger.getLogger(UserResource.class);

    protected RealmModel realm;

    private AdminPermissionEvaluator auth;

    private final String LICENSE_HOST_URL = EnvironmentReader.getValue(Constants.LICENSE_SERVER_URL, Constants.DEFAULT_LICENSE_SERVER_URL);
    private AdminEventBuilder adminEvent;
    private UserModel user;
    private static AvatarProvider avatarProvider;

    private final String HIDDEN_KEY = "hidden_ua_add_account_creator_";
    private final String OTP_KEY = "_req_otp";
    private final String EXP_KEY = "_req_otp_exp";
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_URL);
    private static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_ACCESS_KEY);
    private static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_SECRET_KEY);

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public UserResource(RealmModel realm, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.user = user;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    /**
     * Update the user
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(final UserRepresentation rep) {

        boolean userEmailModified = false;

        auth.users().requireManage(user);
        try {
            boolean wasPermanentlyLockedOut = false;
            if (rep.isEnabled() != null && rep.isEnabled()) {
                UserLoginFailureModel failureModel = session.sessions().getUserLoginFailure(realm, user.getId());
                if (failureModel != null) {
                    failureModel.clearFailures();
                }
                wasPermanentlyLockedOut = session.getProvider(BruteForceProtector.class).isPermanentlyLockedOut(session, realm, user);
            }

            UserModel userByEmail= rep.getEmail() != null ? KeycloakModelUtils.findUserByNameOrEmail(session, realm, rep.getEmail()) : null;
            if (userByEmail != null && !Objects.equals(userByEmail.getId(), user.getId())) {
                return ErrorResponse.exists("User exists with same username or email");
            }

            if(StringUtil.isNotBlank(rep.getMobileNumber()) && StringUtil.isNotBlank(rep.getMobileNumberRegion()) && !realm.isDuplicateMobNumAllowed()){
                UserModel userByMobNum = KeycloakModelUtils.findUserByMobile(session, realm, rep.getMobileNumber(),rep.getMobileNumberRegion());
                if (userByMobNum != null && !Objects.equals(userByMobNum.getId(), user.getId())) {
                    return ErrorResponse.exists("User exists with same mobile number");
                }
            }

            List<CredentialModel> userCredentialsList = session.userCredentialManager().getStoredCredentialsByTypeStream(realm,user,"password").filter(x -> !x.isTemporary()).collect(Collectors.toList());

            if ((user.getEmail() == null && rep.getEmail() != null && !rep.getEmail().isEmpty()) ||
                    (user.getEmail() != null && !user.getEmail().equals(rep.getEmail()) && userCredentialsList.isEmpty())) {
                if (rep.getCredentials() == null || rep.getCredentials().isEmpty()) {
                    if (realm.getUserInviteWay().equalsIgnoreCase("otp")) {
                        List<CredentialRepresentation> credentials = new ArrayList<>();
                        CredentialRepresentation pass = new CredentialRepresentation();
                        pass.setType(PASSWORD);
                        pass.setValue(HmacOTP.generateSecret(10));
                        pass.setTemporary(true);
                        pass.setUserLabel("userInvited");
                        credentials.add(pass);
                        rep.setCredentials(credentials);
                    }

                    rep.getAttributes().remove(UserModel.USER_INVITE_STATUS);
                    userEmailModified=true;
                    user.setSingleAttribute(UserModel.USER_INVITE_CLIENT, rep.getInviteClientId());
                    adminEvent.operation(OperationType.CREATE).resource(ResourceType.USER).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();
                }
            }

            UserProfile profile = session.getProvider(UserProfileProvider.class).create(USER_API, rep.toAttributes(), user);

            Response response = validateUserProfile(profile, user, session);
            if (response != null) {
                return response;
            }

            profile.update(rep.getAttributes() != null);
            updateUserFromRep(profile,user, rep, session, true);
            RepresentationToModel.createCredentials(rep, session, realm, user, true);

            if(userEmailModified)
                user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");

            // we need to do it here as the attributes would be overwritten by what is in the rep
            if (wasPermanentlyLockedOut) {
                session.getProvider(BruteForceProtector.class).cleanUpPermanentLockout(session, realm, user);
            }

            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("User exists with same username or email");
        } catch (ReadOnlyException re) {
            return ErrorResponse.exists("User is read only!");
        } catch (ModelException me) {
            logger.warn("Could not update user!", me);
            return ErrorResponse.error("Could not update user!", Status.BAD_REQUEST);
        } catch (ForbiddenException fe) {
            throw fe;
        } catch (Exception me) { // JPA
            logger.warn("Could not update user!", me);//
            me.printStackTrace();// may be committed by JTA which can't
            return ErrorResponse.error(me.getCause().getMessage().toString(), Status.BAD_REQUEST);
        }
    }

    public static Response validateUserProfile(UserProfile profile, UserModel user, KeycloakSession session) {
        try {
            profile.validate();
        } catch (ValidationException pve) {
            List<ErrorRepresentation> errors = new ArrayList<>();
            for (ValidationException.Error error : pve.getErrors()) {
                errors.add(new ErrorRepresentation(error.getFormattedMessage(new AdminMessageFormatter(session, user))));
            }
            return ErrorResponse.errors(errors, Response.Status.BAD_REQUEST);
        }
        return null;
    }

    public static void updateUserFromRep(UserProfile userProfile, UserModel user, UserRepresentation rep, KeycloakSession session, boolean isUpdateExistingUser) {
        boolean removeMissingRequiredActions = isUpdateExistingUser;

        if(getAccountCreatorOfTenant(session,session.getContext().getRealm()) != null && rep.getAttributes().containsKey("qv_is_account_creator")){
            rep.getAttributes().remove("qv_is_account_creator");
        }

        if (rep.isEnabled() != null) user.setEnabled(rep.isEnabled());

        if (rep.isEmailVerified() != null) user.setEmailVerified(rep.isEmailVerified());

        if(rep.isMfaEnabled() != null) user.setMfaEnabled(rep.isMfaEnabled());

        if (rep.getFederationLink() != null) user.setFederationLink(rep.getFederationLink());

        if(rep.getProfPic() != null) user.setSingleAttribute(UserModel.USER_PICTURE, rep.getProfPic());

        List<String> reqActions = rep.getRequiredActions();

        if (reqActions != null) {
            Set<String> allActions = new HashSet<>();
            for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(RequiredActionProvider.class)) {
                allActions.add(factory.getId());
            }
            for (String action : allActions) {
                if (reqActions.contains(action)) {
                    user.addRequiredAction(action);
                } else if (removeMissingRequiredActions) {
                    user.removeRequiredAction(action);
                }
            };
        }

        List<CredentialRepresentation> credentials = rep.getCredentials();
        if (credentials != null) {
            for (CredentialRepresentation credential : credentials) {
                if (CredentialRepresentation.PASSWORD.equals(credential.getType()) && credential.isTemporary() != null
                        && credential.isTemporary()) {
                    user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                }
            }
        }
    }

    /**
     * Get representation of the user
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUser() {
        auth.users().requireView(user);

        UserRepresentation rep = ModelToRepresentation.toRepresentation(session, realm, user);

        if (realm.isIdentityFederationEnabled()) {
            List<FederatedIdentityRepresentation> reps = getFederatedIdentities(user).collect(Collectors.toList());
            rep.setFederatedIdentities(reps);
        }

        if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
            rep.setEnabled(false);
        }
        rep.setAccess(auth.users().getAccess(user));

        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(USER_API, user);
        Map<String, List<String>> readableAttributes = profile.getAttributes().getReadable(false);

        if (rep.getAttributes() != null) {
            rep.setAttributes(readableAttributes);
        }

        return rep;
    }


    @POST
    @Path("resend-invite")
    public Response resendInvite(){
        UserRepresentation rep = new UserRepresentation();
        rep.setEmail(user.getEmail());
        rep.setSkipSendingEmail(false);
        if (realm.getUserInviteWay().equalsIgnoreCase("otp")) {
            List<CredentialRepresentation> credentials = new ArrayList<>();
            CredentialRepresentation pass = new CredentialRepresentation();
            pass.setType(PASSWORD);
            pass.setValue(HmacOTP.generateSecret(10));
            pass.setTemporary(true);
            pass.setUserLabel("userInvited");
            credentials.add(pass);
            rep.setCredentials(credentials);
            RepresentationToModel.createCredentials(rep, session, realm, user, true);
        }
        user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");
        user.setSingleAttribute(UserModel.USER_INVITE_CLIENT, rep.getInviteClientId());
        if (GeneralHelper.checkUserIsAdmin(session, realm, user)) {
            adminEvent.operation(OperationType.RESEND_INVITE).resource(ResourceType.ADMIN_USER).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();
        } else {
            adminEvent.operation(OperationType.RESEND_INVITE).resource(ResourceType.USER).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();
        }

        return Response.noContent().build();
    }


    @POST
    @Path("upload-userpicture")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveImageAndReturnUrl(final MultipartFormDataInput input) throws IOException {
        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();

        InputPart file;
        String fileName = user.getId();
        if(formDataMap.containsKey(fileName)){
            file = formDataMap.get(user.getId()).get(0);
        } else {
            return ErrorResponse.error("Missing Parameters", Status.BAD_REQUEST);
        }

        //checking file extension
        String extension = FilenameUtils.getExtension(getFileName(file.getHeaders()));
        if (!checkForCorrectExtension(extension))
            return ErrorResponse.error("Unsupported File Type", Status.BAD_REQUEST);

        fileName = fileName + "." + extension;
        String fileContentType = file.getHeaders().get("Content-Type").get(0);

        if(extension.equals("svg") && (!GeneralHelper.isValidSvgFile(file))){
            return ErrorResponse.error("Upload a proper image file", Status.BAD_REQUEST);
        }

        //converting file into InputStream
        InputStream inputStream = file.getBody(InputStream.class, null);

        InputStream imageStream = file.getBody(InputStream.class, null);
        BufferedImage image = ImageIO.read(imageStream);

        if (image == null && !extension.equals("svg")) {
            return ErrorResponse.error("Upload a proper image file", Status.BAD_REQUEST);
        }

        //Form a ObjectPath to store object in specified path
        String objectPath = "public/user-logos/";
        String url = session.getProvider(AvatarProvider.class, "avatar-storage-minio")
                .saveImgAndReturnUrl(realm.getName(), objectPath, fileName, fileContentType, inputStream);
        String returningUrl = "{\"url\": \"" + url + "\"}";
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().build()).entity(returningUrl).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("assign-groups")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addGroupsToUser(AssignGroupsRepresentation rep){
        this.auth.realm().canManageRealm();

        List<String> grpIds = rep.getGrpIds();
        if(grpIds.isEmpty()){
            return ErrorResponse.error("Group List is Empty", Response.Status.BAD_REQUEST);
        }

        if(grpIds.size() > 10){
            return ErrorResponse.error("Only 10 groups added at one time", Response.Status.BAD_REQUEST);
        }

        grpIds.forEach(gId -> {
            GroupModel grp = session.groups().getGroupById(realm,gId);
            if(grp == null){
                logger.error("Group not found with this id " + gId + " in the realm " + realm.getName());
            } else {
                if (!user.isMemberOf(grp)) {
                    user.joinGroup(grp);
                    triggerNotification(OperationType.CREATE,ResourceType.GROUP_MEMBERSHIP,grp,user);
                }
            }

        });
        return Response.noContent().build();
    }


    /**
     * Impersonate the user
     *
     * @return
     */
    @Path("impersonation")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> impersonate() {
        ProfileHelper.requireFeature(Profile.Feature.IMPERSONATION);

        auth.users().requireImpersonate(user);
        RealmModel authenticatedRealm = auth.adminAuth().getRealm();
        // if same realm logout before impersonation
        boolean sameRealm = false;
        if (authenticatedRealm.getId().equals(realm.getId())) {
            sameRealm = true;
            UserSessionModel userSession = session.sessions().getUserSession(authenticatedRealm, auth.adminAuth().getToken().getSessionState());
            AuthenticationManager.expireIdentityCookie(realm, session.getContext().getUri(), clientConnection);
            AuthenticationManager.expireRememberMeCookie(realm, session.getContext().getUri(), clientConnection);
            AuthenticationManager.backchannelLogout(session, authenticatedRealm, userSession, session.getContext().getUri(), clientConnection, headers, true);
        }
        EventBuilder event = new EventBuilder(realm, session, clientConnection);

        UserSessionModel userSession = session.sessions().createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteAddr(), "impersonate", false, null, null);

        UserModel adminUser = auth.adminAuth().getUser();
        String impersonatorId = adminUser.getId();
        String impersonator = adminUser.getUsername();
        userSession.setNote(IMPERSONATOR_ID.toString(), impersonatorId);
        userSession.setNote(IMPERSONATOR_USERNAME.toString(), impersonator);

        AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession, session.getContext().getUri(), clientConnection);
        URI redirect = AccountFormService.accountServiceBaseUrl(session.getContext().getUri()).build(realm.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("sameRealm", sameRealm);
        result.put("redirect", redirect.toString());
        event.event(EventType.IMPERSONATE)
                .session(userSession)
                .user(user)
                .detail(Details.IMPERSONATOR_REALM, authenticatedRealm.getName())
                .detail(Details.IMPERSONATOR, impersonator).success();

        return result;
    }

    /**
     * Get sessions associated with the user
     *
     * @return
     */
    @Path("sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserSessionRepresentation> getSessions() {
        auth.users().requireView(user);
        return session.sessions().getUserSessionsStream(realm, user).map(ModelToRepresentation::toRepresentation);
    }

    /**
     * Get offline sessions associated with the user and client
     *
     * @return
     */
    @Path("offline-sessions/{clientUuid}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserSessionRepresentation> getOfflineSessions(final @PathParam("clientUuid") String clientUuid) {
        auth.users().requireView(user);
        ClientModel client = realm.getClientById(clientUuid);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        return new UserSessionManager(session).findOfflineSessionsStream(realm, user)
                .map(session -> toUserSessionRepresentation(session, clientUuid))
                .filter(Objects::nonNull);
    }

    /**
     * Get social logins associated with the user
     *
     * @return a non-null {@code Stream} of social logins (federated identities).
     */
    @Path("federated-identity")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<FederatedIdentityRepresentation> getFederatedIdentity() {
        auth.users().requireView(user);
        return getFederatedIdentities(user);
    }

    private Stream<FederatedIdentityRepresentation> getFederatedIdentities(UserModel user) {
        Set<String> idps = realm.getIdentityProvidersStream().map(IdentityProviderModel::getAlias).collect(Collectors.toSet());
        return session.users().getFederatedIdentitiesStream(realm,user)
                .filter(identity -> idps.contains(identity.getIdentityProvider()))
                .map(ModelToRepresentation::toRepresentation);
    }

    /**
     * Add a social login provider to the user
     *
     * @param provider Social login provider id
     * @param rep
     * @return
     */
    @Path("federated-identity/{provider}")
    @POST
    @NoCache
    public Response addFederatedIdentity(final @PathParam("provider") String provider, FederatedIdentityRepresentation rep) {
        auth.users().requireManage(user);
        if (session.users().getFederatedIdentity(user, provider, realm) != null) {
            return ErrorResponse.exists("User is already linked with provider");
        }

        FederatedIdentityModel socialLink = new FederatedIdentityModel(provider, rep.getUserId(), rep.getUserName());
        session.users().addFederatedIdentity(realm, user, socialLink);
        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(rep).success();
        return Response.noContent().build();
    }

    /**
     * Remove a social login provider from user
     *
     * @param provider Social login provider id
     */
    @Path("federated-identity/{provider}")
    @DELETE
    @NoCache
    public void removeFederatedIdentity(final @PathParam("provider") String provider) {
        auth.users().requireManage(user);
        if (!session.users().removeFederatedIdentity(realm, user, provider)) {
            throw new NotFoundException("Link not found");
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Get consents granted by the user
     *
     * @return
     */
    @Path("consents")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<Map<String, Object>> getConsents() {
        auth.users().requireView(user);

        Set<ClientModel> offlineClients = new UserSessionManager(session).findClientsWithOfflineToken(realm, user);

        return realm.getClientsStream()
                .map(client -> toConsent(client, offlineClients))
                .filter(Objects::nonNull);
    }

    private Map<String, Object> toConsent(ClientModel client, Set<ClientModel> offlineClients) {
        UserConsentModel consent = session.users().getConsentByClient(realm, user.getId(), client.getId());
        boolean hasOfflineToken = offlineClients.contains(client);

        if (consent == null && !hasOfflineToken) {
            return null;
        }

        UserConsentRepresentation rep = (consent == null) ? null : ModelToRepresentation.toRepresentation(consent);


        Map<String, Object> currentRep = new HashMap<>();
        currentRep.put("clientId", client.getClientId());
        currentRep.put("grantedClientScopes", (rep == null ? Collections.emptyList() : rep.getGrantedClientScopes()));
        currentRep.put("createdDate", (rep == null ? null : rep.getCreatedDate()));
        currentRep.put("lastUpdatedDate", (rep == null ? null : rep.getLastUpdatedDate()));

        List<Map<String, String>> additionalGrants = new LinkedList<>();
        if (hasOfflineToken) {
            Map<String, String> offlineTokens = new HashMap<>();
            offlineTokens.put("client", client.getId());
            offlineTokens.put("key", "Offline Token");
            additionalGrants.add(offlineTokens);
        }
        currentRep.put("additionalGrants", additionalGrants);
        return currentRep;
    }

    /**
     * Revoke consent and offline tokens for particular client from user
     *
     * @param clientId Client id
     */
    @Path("consents/{client}")
    @DELETE
    @NoCache
    public void revokeConsent(final @PathParam("client") String clientId) {
        auth.users().requireManage(user);

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }
        boolean revokedConsent = session.users().revokeConsentForClient(realm, user.getId(), client.getId());
        boolean revokedOfflineToken = new UserSessionManager(session).revokeOfflineToken(user, client);

        if (revokedConsent) {
            // Logout clientSessions for this user and client
            AuthenticationManager.backchannelLogoutUserFromClient(session, realm, user, client, session.getContext().getUri(), headers);
        }

        if (!revokedConsent && !revokedOfflineToken) {
            throw new NotFoundException("Consent nor offline token not found");
        }
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Remove all user sessions associated with the user
     *
     * Also send notification to all clients that have an admin URL to invalidate
     * the sessions for the particular user.
     *
     */
    @Path("logout")
    @POST
    public void logout() {
        auth.users().requireManage(user);

        session.users().setNotBeforeForUser(realm, user, Time.currentTime());

        session.sessions().getUserSessionsStream(realm, user)
                .collect(Collectors.toList()) // collect to avoid concurrent modification as backchannelLogout removes
                // the user sessions.
                .forEach(userSession -> AuthenticationManager.backchannelLogout(session, realm, userSession,
                        session.getContext().getUri(), clientConnection, headers, true));
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Delete the user
     */
    @DELETE
    @NoCache
    public Response deleteUser() {
        auth.users().requireManage(user);
        // for keyless
        session.setAttribute("email",user.getUsername());
        // for pindrop
        session.setAttribute("user",user);
        // for workflow
        session.setAttribute("deletedUserRoleMappings", user.getRealmRoleMappingsStream().collect(Collectors.toList()));

        session.getContext().setRealm(realm);
        boolean removed = new UserManager(session).removeUser(realm, user);
        // Remove the Realm Logo from MINIO
        AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
        if( ! avatarProvider.removeAvatarImage(session.getContext().getRealm().getName(),user.getId())){
            logger.error("Something went wrong on deleting User Avatar for the user " + user.getId() +" from Minio");
        }
        if (removed) {
            ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "max-users");
            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setId(user.getId());
            userRepresentation.setUsername(user.getUsername());
            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(userRepresentation).success();
            return Response.noContent().build();
        } else {
            return ErrorResponse.error("User couldn't be deleted", Status.BAD_REQUEST);
        }
    }

    //Send OTP to current account creator for make other co admin as account creator
    @GET
    @Path("/send-otp-to-account-creator")
    public Response sendOtpToAccountCreator(){

        UserModel oldAccountCreator = auth.adminAuth().getUser();

        //Only current account creator can access this api so verifying that
        if(!checkIsAccountCreatorOrNot(oldAccountCreator)){
            return ErrorResponse.error("Only account creator can make other co admin as a Account creator",Status.FORBIDDEN);
        }

        //checking is newAccountCreator is already an account creator
        if(checkIsAccountCreatorOrNot(user)){
            return ErrorResponse.error("This user is already an account creator",Status.BAD_REQUEST);
        }

        if(!user.isEmailVerified())
            return ErrorResponse.error("email-not-verified",Status.BAD_REQUEST);

        //To check user is approved
        if(!user.isMfaEnabled()){
            return ErrorResponse.error("This user is not approved by admin",Status.BAD_REQUEST);
        }

        //To check user is verified
        if(!checkIsUserVerified(user)){
            return ErrorResponse.error("This user is not verified",Status.BAD_REQUEST);
        }



        try {
            if (checkEmailEmptyForUser(oldAccountCreator)) {
                return ErrorResponse.error("User does not have the email to send OTP for change account creator request", Response.Status.BAD_REQUEST);
            }

            Map<String, Object> data = generateOtpAndSetAttrs(realm, oldAccountCreator,user);
            data.put("realm", realm.getId());
            adminLogs(data, realm, OperationType.ADD_ACCOUNT_CREATOR_TRIGGER, adminEvent);

        } catch(Exception e){
            logger.error(e.getMessage());
            return ErrorResponse.error(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent().build();
    }

    //Verify OTP and make co admin as account creator
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/add-account-creator")
    public Response addAccountCreator(Map<String, String> otpConfig) throws IOException {

        UserModel authUser = auth.adminAuth().getUser();
        UserModel realmAccountCreator = session.users().searchForUserByUserAttributeStream(realm,"qv_is_account_creator", "true",true).findFirst().orElse(null);
        if(realmAccountCreator == null){
            return ErrorResponse.error("Account creator is not available", Status.EXPECTATION_FAILED);
        }
        //Only current account creator can access this api so verifying that
        if(!checkIsAccountCreatorOrNot(authUser)){
            return ErrorResponse.error("Only account creator can make other co admin as a Account creator", Status.FORBIDDEN);
        }

        //checking is newAccountCreator is already an account creator
        if(checkIsAccountCreatorOrNot(user)){
            return ErrorResponse.error("This user already was an account creator", Status.BAD_REQUEST);
        }

        String requestOtp = otpConfig.get("changeAccountCreatorReqCode");

        if(checkEmailEmptyForUser(realmAccountCreator) || checkEmailEmptyForUser(user)) {
            return ErrorResponse.error("Email is required for validation", Status.BAD_REQUEST);
        }

        if(requestOtp == null || requestOtp.isEmpty()) {
            return ErrorResponse.error("OTP is required for validation", Status.BAD_REQUEST);
        }

        String otp = authUser.getFirstAttribute(getAccountCreatorOtpKey());
        String otpExp = authUser.getFirstAttribute(getAccountCreatorExpKey());

        if(otp != null && otpExp != null) {
            if (System.currentTimeMillis() > Long.parseLong(otpExp)) {
                return ErrorResponse.error("The OTP you entered is expired", Response.Status.BAD_REQUEST);
            }

            if(!requestOtp.equals(otp)) {
                return ErrorResponse.error("The OTP you entered is incorrect.Please enter valid OTP", Status.BAD_REQUEST);
            }

            //Trying  to update account creator user in strapi
            boolean isCustomerUpdated = updateStripe(realmAccountCreator.getEmail(), user.getEmail());
            if(isCustomerUpdated){
                Map<String, Object> data = setupAttrsForNotify(realm, user,realmAccountCreator);

                authUser.removeAttribute(getAccountCreatorOtpKey());
                authUser.removeAttribute(getAccountCreatorExpKey());

                //setting newAccountCreator as account creator and removing currentAccountCreator
                user.setSingleAttribute("qv_is_account_creator","true");
                realmAccountCreator.setSingleAttribute("qv_is_account_creator","false");

                //Clearing session user cache
                session.userCache().evict(realm, realmAccountCreator);
                data.put("realm", realm.getId());
                adminLogs(data, realm, OperationType.ADD_ACCOUNT_CREATOR, adminEvent);
            }
            else {
                return ErrorResponse.error("Account creator not updated in stripe", Status.EXPECTATION_FAILED);
            }


            return Response.noContent().build();
        } else {
            return ErrorResponse.error("invalid-request", Status.BAD_REQUEST);
        }
    }

    @Path("role-mappings")
    public RoleMapperResource getRoleMappings() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.users().requireMapRoles(user);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.users().requireView(user);
        RoleMapperResource resource = new RoleMapperResource(realm, auth, user, adminEvent, manageCheck, viewCheck);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;

    }

    @Path("winlogon")
    public WinLogonManagementResource getWinLogon() {
        WinLogonManagementResource resource = new WinLogonManagementResource(realm, user, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Disable all credentials for a user of a specific type
     *
     * @param credentialTypes
     */
    @Path("disable-credential-types")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void disableCredentialType(List<String> credentialTypes) {
        auth.users().requireManage(user);
        if (credentialTypes == null)
            return;
        for (String type : credentialTypes) {
            session.userCredentialManager().disableCredentialType(realm, user, type);

        }
    }

    /**
     * Set up a new password for the user.
     *
     * @param cred The representation must contain a rawPassword with the plain-text
     *             password
     */
    @Path("reset-password")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void resetPassword(@QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId,CredentialRepresentation cred) {
        auth.users().requireManage(user);
        if(clientId.isEmpty() || cred == null ){
            throw new BadRequestException("Reset Password mail not sent.");
        }
        if(user == null || user.getEmail() == null){
            throw new BadRequestException("No email is associated with the user.");
        }
        try {
            String passValue  = cred.getValue();
            if(passValue == null)
            {
                cred.setValue(HmacOTP.generateSecret(10));
            }
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password(cred.getValue(), cred.isTemporary()));
        } catch (IllegalStateException ise) {
            throw new BadRequestException("Resetting to N old passwords is not allowed.");
        } catch (ReadOnlyException mre) {
            throw new BadRequestException("Can't reset password as account is read only");
        } catch (ModelException e) {
            logger.warn("Could not update user password.", e);
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(e.getMessage(), MessageFormat.format(messages.getProperty(e.getMessage(), e.getMessage()), e.getParameters()),
                    Status.BAD_REQUEST);
        }
        if (cred.isTemporary() != null && cred.isTemporary()) {
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        } else {
            // Remove a potentially existing UPDATE_PASSWORD action when explicitly assigning a non-temporary password.
            user.removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }

        session.sessions().getUserSessionsStream(realm, user).forEach(x-> {session.sessions().removeUserSession(realm, x);});

        if(cred.isTemporary()) {
            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setEmail(user.getEmail());
            List<CredentialRepresentation> credentials = new ArrayList<>();
            credentials.add(cred);
            userRepresentation.setCredentials(credentials);
            if(clientId == null || realm.getClientByClientId(clientId) == null) {
                throw new BadRequestException("Client not found with given ClientID");
            } else {
                userRepresentation.singleAttribute(Constants.CLIENT_ID,clientId);
            }
            userRepresentation.setId(user.getId());
            adminEvent.operation(OperationType.ACTION).resource(ResourceType.RESET_PASSWORD).resourcePath(session.getContext().getUri()).representation(userRepresentation).success();
        } else {
            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
        }

    }

    @Path("revoke-adminRole")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response revokeAdminRole(@QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        auth.users().requireManage(user);
        if(!user.isEnabled())
            return ErrorResponse.error("user-is-disabled", Status.BAD_REQUEST);

        if(clientId == null)
            return ErrorResponse.error("invalid-request", Status.BAD_REQUEST);

        if(user.getAttributes().containsKey("qv_is_account_creator") && user.getFirstAttribute("qv_is_account_creator").equals("true"))
            return ErrorResponse.error("account-creator-not-demoted", Status.BAD_REQUEST);

        if (auth.adminAuth().getUser().getUsername().equals(user.getUsername()))
            return ErrorResponse.error("cannot-demote-yourself", Status.BAD_REQUEST);

        if(user.getEmail() == null)
            return ErrorResponse.error("email-not-available", Status.BAD_REQUEST);

        if( !user.isEmailVerified())
            return ErrorResponse.error("email-not-verified", Status.BAD_REQUEST);

        if(user.getAttributes().containsKey("userInviteStatus") && user.getFirstAttribute("userInviteStatus").equals("Invited"))
            return ErrorResponse.error("the-invite-not-acceted", Status.BAD_REQUEST);

        if(user.getAttributes().containsKey("qv_is_account_creator") && user.getFirstAttribute("qv_is_account_creator").equals("false"))
            user.removeAttribute("qv_is_account_creator");

        adminEvent.detail("email",user.getEmail());
        if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            RoleModel role = realm.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT).getRole(ActionTokenConstants.ADMIN_USER_ROLE);
            user.deleteRoleMapping(role);
            adminEvent.resource(ResourceType.WORKSPACE_ROLE).operation(OperationType.ADMIN_ROLE_REMOVED).authUser(user).resourcePath(session.getContext().getUri()).success();
            session.sessions().removeUserSessions(realm,user);
            user.setMfaEnabled(false);
            return Response.status(Status.OK).type(MediaType.TEXT_PLAIN).build();
        } else {
            user.deleteRoleMapping(realm.getRole("admin"));
            adminEvent.resource(ResourceType.WORKSPACE_ROLE).operation(OperationType.ADMIN_ROLE_REMOVED).authUser(user).resourcePath(session.getContext().getUri()).success();
            session.sessions().removeUserSessions(realm,user);
            user.setMfaEnabled(false);
            return Response.status(Status.OK).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @Path("add-adminRole")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addAdminRole(@QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        auth.users().requireManage(user);
        if(!user.isEnabled())
            return ErrorResponse.error("user-is-disabled", Status.BAD_REQUEST);

        if(clientId == null)
            return ErrorResponse.error("invalid-request", Status.BAD_REQUEST);

        if(user.getAttributes().containsKey("qv_is_account_creator") && user.getFirstAttribute("qv_is_account_creator").equals("true"))
            return ErrorResponse.error("account-cretor-role-cant-change", Status.BAD_REQUEST);

        if(user.getEmail() == null)
            return ErrorResponse.error("email-not-available", Status.BAD_REQUEST);

        if(!user.isEmailVerified())
            return ErrorResponse.error("email-not-verified", Status.BAD_REQUEST);

        if(user.getAttributes().containsKey("userInviteStatus") && user.getFirstAttribute("userInviteStatus").equals("Invited"))
            return ErrorResponse.error("the-invite-not-acceted", Status.BAD_REQUEST);

        adminEvent.detail("email",user.getEmail());
        if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            RoleModel role = realm.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT).getRole(ActionTokenConstants.ADMIN_USER_ROLE);
            user.grantRole(role);
            adminEvent.resource(ResourceType.WORKSPACE_ROLE).operation(OperationType.ADMIN_ROLE_ADDED).authUser(user).resourcePath(session.getContext().getUri()).success();
            return Response.status(Status.OK).type(MediaType.TEXT_PLAIN).build();
        } else {
            user.grantRole(realm.getRole("admin"));
            adminEvent.resource(ResourceType.WORKSPACE_ROLE).operation(OperationType.ADMIN_ROLE_ADDED).authUser(user).resourcePath(session.getContext().getUri()).success();
            return Response.status(Status.OK).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @GET
    @Path("credentials")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<CredentialRepresentation> credentials() {
        auth.users().requireManage(user);
        return session.userCredentialManager().getStoredCredentialsStream(realm, user)
                .filter(x->!x.isTemporary())
                //.peek(model -> model.setSecretData(null))
                .map(x-> ModelToRepresentation.toRepresentation(x, session));
    }

    @GET
    @Path("transactions")
    @NoCache
    public Map<String, Object> transactions(@QueryParam("first") Integer firstResult,
                                                  @QueryParam("max") Integer maxResults,@QueryParam("request") String request, @QueryParam("status") String status) {
        auth.users().requireManage(user);

        List<UserTransactionModel> userTransactions = realm.getUserTransactionStatus(user.getUsername());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy  hh:mm:ss a");
        ZoneId zoneId = ZoneId.of(session.getContext().getConnection().getClientLocation().getBody().getZoneId());

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        userTransactions.sort(Comparator.comparing(UserTransactionModel::getCreatedAt).reversed());

        List<Map<String, Object>> result = userTransactions.stream().filter(Objects::nonNull)
                .map(transaction -> {
                    Map<String, Object> transactionMap = new HashMap<>();
                    Instant instant = Instant.ofEpochMilli(transaction.getCreatedAt());
                    if(!transaction.getStatus().equals(UserTransactionStatus.USER_CANCELLED.getDisplayName()) && transaction.getStatus().equals(UserTransactionStatus.PENDING.getDisplayName()) && transaction.getExpiresAt() != null && ZonedDateTime.now(zoneId).toInstant().getEpochSecond() > transaction.getExpiresAt()) {
                        transaction.setStatus("EXPIRED");

                        realm.updateTransactionStatus(transaction.getTrxId(), transaction.getUserId(), UserTransactionStatus.EXPIRED.getDisplayName());
                    }
                    transactionMap.put("trxId", transaction.getTrxId());
                    transactionMap.put("timestamp", formatter.format(ZonedDateTime.ofInstant(instant, zoneId)));
                    transactionMap.put("status", transaction.getStatus());
                    return transactionMap;
                })
                .filter(x -> request != null && StringUtil.isNotBlank(request) && !request.equalsIgnoreCase("null") ? ((String)x.get("trxId")).contains(request) : !x.isEmpty())
                .filter(x -> status != null && StringUtil.isNotBlank(status) && !status.equalsIgnoreCase("null") ? status.equals(x.get("status")) : !x.isEmpty())
                .collect(Collectors.toList());

        List<Map<String, Object>> finalResult = result.stream().skip(firstResult)
                .limit(maxResults).collect(Collectors.toList());

        Map<String, Object> userTransactionDetails = new HashMap<>();
        userTransactionDetails.put("result", finalResult);
        userTransactionDetails.put("count", result.size());

        return userTransactionDetails;
    }

    /**
     * Return credential types, which are provided by the user storage where user is
     * stored. Returned values can contain for example "password", "otp" etc.
     * This will always return empty list for "local" users, which are not backed by
     * any user storage
     *
     * @return
     */
    @GET
    @Path("configured-user-storage-credential-types")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<String> getConfiguredUserStorageCredentialTypes() {
        // This has "requireManage" due the compatibility with "credentials()" endpoint.
        // Strictly said, it is reading endpoint, not writing,
        // so may be revisited if to rather use "requireView" here in the future.
        auth.users().requireManage(user);
        return session.userCredentialManager().getConfiguredUserStorageCredentialTypesStream(realm, user);
    }

    /**
     * Remove a credential for a user
     *
     */
    @Path("credentials/{credentialId}")
    @DELETE
    @NoCache
    public void removeCredential(final @PathParam("credentialId") String credentialId) {
        auth.users().requireManage(user);
        CredentialModel credential = session.userCredentialManager().getStoredCredentialById(realm, user, credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery())
                throw new NotFoundException("Credential not found");
            else
                throw new ForbiddenException();
        }
        session.userCredentialManager().removeStoredCredential(realm, user, credentialId);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", realm.getMode().equalsIgnoreCase("bo") ? user.getUsername() : user.getId());
        attrs.put("message", "Device removed successfully");
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(attrs).success();
    }

    /**
     * Update a credential label for a user
     */
    @PUT
    @Consumes(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    @Path("credentials/{credentialId}/userLabel")
    public void setCredentialUserLabel(final @PathParam("credentialId") String credentialId, String userLabel) {
        auth.users().requireManage(user);
        CredentialModel credential = session.userCredentialManager().getStoredCredentialById(realm, user, credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery())
                throw new NotFoundException("Credential not found");
            else
                throw new ForbiddenException();
        }
        session.userCredentialManager().updateCredentialLabel(realm, user, credentialId, userLabel);
    }

    @PUT
    @Consumes(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    @Path("credentials/{credentialId}/enable")
    public void enableCredential(final @PathParam("credentialId") String credentialId) {
        auth.users().requireManage(user);
        CredentialModel credential = session.userCredentialManager().getStoredCredentialById(realm, user, credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery())
                throw new NotFoundException("Credential not found");
            else
                throw new ForbiddenException();
        }
        credential.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, user, credential);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", user.getUsername());
        attrs.put("message", "Device enabled successfully");
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(attrs).success();
    }

    @PUT
    @Consumes(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    @Path("credentials/{credentialId}/disable")
    public void disableCredential(final @PathParam("credentialId") String credentialId) {
        auth.users().requireManage(user);
        CredentialModel credential = session.userCredentialManager().getStoredCredentialById(realm, user, credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery())
                throw new NotFoundException("Credential not found");
            else
                throw new ForbiddenException();
        }
        credential.setEnabled(false);
        session.userCredentialManager().updateCredential(realm, user, credential);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", user.getUsername());
        attrs.put("message", "Device disabled successfully");
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(attrs).success();
    }

    /**
    *  This method is exclusive for ezto verify
    *  This is used to mark the device_token type credential
    *  as the last used one from the dashboard.
    *
    *  @param credentialId The credential to mark as last used
    */
    @PUT
    @Consumes(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    @Path("credentials/{credentialId}/mark-as-last-used")
    public void markAsLastUsed(final @PathParam("credentialId") String credentialId) {
        auth.users().requireManage(user);
        CredentialModel credential = session.userCredentialManager().getStoredCredentialById(realm, user, credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery())
                throw new NotFoundException("Credential not found");
            else
                throw new ForbiddenException();
        }
        credential.setLastUsed(Time.currentTimeMillis());
        session.userCredentialManager().updateCredential(realm, user, credential);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", user.getUsername());
        attrs.put("message", "Marked as last used device successfully");
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(attrs).success();
    }

    /**
     * Move a credential to a first position in the credentials list of the user
     *
     * @param credentialId The credential to move
     */
    @Path("credentials/{credentialId}/moveToFirst")
    @POST
    public void moveCredentialToFirst(final @PathParam("credentialId") String credentialId) {
        moveCredentialAfter(credentialId, null);
    }

    /**
     * Move a credential to a position behind another credential
     *
     * @param credentialId            The credential to move
     * @param newPreviousCredentialId The credential that will be the previous
     *                                element in the list. If set to null, the moved
     *                                credential will be the first element in the
     *                                list.
     */
    @Path("credentials/{credentialId}/moveAfter/{newPreviousCredentialId}")
    @POST
    public void moveCredentialAfter(final @PathParam("credentialId") String credentialId,
                                    final @PathParam("newPreviousCredentialId") String newPreviousCredentialId) {
        auth.users().requireManage(user);
        CredentialModel credential = session.userCredentialManager().getStoredCredentialById(realm, user, credentialId);
        if (credential == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery())
                throw new NotFoundException("Credential not found");
            else
                throw new ForbiddenException();
        }
        session.userCredentialManager().moveCredentialTo(realm, user, credentialId, newPreviousCredentialId);
    }

    /**
     * Send an email to the user with a link they can click to reset their password.
     * The redirectUri and clientId parameters are optional. The default for the
     * redirect is the account client.
     *
     * This endpoint has been deprecated. Please use the execute-actions-email
     * passing a list with
     * UPDATE_PASSWORD within it.
     *
     * @param redirectUri redirect uri
     * @param clientId    client id
     * @return
     */
    @Deprecated
    @Path("reset-password-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPasswordEmail(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri,
                                       @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        return executeActionsEmail(redirectUri, clientId, null, actions);
    }

    /**
     * Send an email to the user with a link they can click to execute particular actions.
     *
     * An email contains a link the user can click to perform a set of required
     * actions.
     * The redirectUri and clientId parameters are optional. If no redirect is
     * given, then there will
     * be no link back to click after actions have completed. Redirect uri must be a
     * valid uri for the
     * particular clientId.
     *
     * @param redirectUri Redirect uri
     * @param clientId    Client id
     * @param lifespan    Number of seconds after which the generated token expires
     * @param actions     Required actions the user needs to complete
     * @return
     */
    @Path("execute-actions-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeActionsEmail(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri,
                                        @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId,
                                        @QueryParam("lifespan") Integer lifespan,
                                        List<String> actions) {
        auth.users().requireManage(user);

        if (user.getEmail() == null) {
            return ErrorResponse.error("user-email-missing", Status.BAD_REQUEST);
        }

        if (!user.isEnabled()) {
            throw new WebApplicationException(
                    ErrorResponse.error("user-is-disabled", Status.BAD_REQUEST));
        }

        if (redirectUri != null && clientId == null) {
            throw new WebApplicationException(
                    ErrorResponse.error("client-id-missing", Status.BAD_REQUEST));
        }

        if (clientId == null) {
            clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        }

        if (CollectionUtil.isNotEmpty(actions) && !RequiredActionsValidator.validRequiredActions(session, actions)) {
            throw new WebApplicationException(
                    ErrorResponse.error("Provided invalid required actions", Status.BAD_REQUEST));
        }

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            logger.debugf("Client %s doesn't exist", clientId);
            throw new WebApplicationException(
                    ErrorResponse.error("client-not-exist", Status.BAD_REQUEST));
        }
        if (!client.isEnabled()) {
            logger.debugf("Client %s is not enabled", clientId);
            throw new WebApplicationException(
                    ErrorResponse.error("client-is-not-enabled", Status.BAD_REQUEST));
        }

        String redirect;
        if (redirectUri != null) {
            redirect = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
            if (redirect == null) {
                throw new WebApplicationException(
                        ErrorResponse.error("invalid-redirect-uri", Status.BAD_REQUEST));
            }
        }

        if (lifespan == null) {
            lifespan = realm.getActionTokenGeneratedByAdminLifespan();
        }
        int expiration = Time.currentTime() + lifespan;
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), user.getEmail(), expiration,
                actions, redirectUri, clientId);

        try {
            UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
            builder.queryParam("key", token.serialize(session, realm, session.getContext().getUri()));

            String link = builder.build(realm.getName()).toString();

            this.session.getProvider(EmailTemplateProvider.class)
                    .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
                    .setRealm(realm)
                    .setUser(user)
                    .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(lifespan));

            // audit.user(user).detail(Details.EMAIL,
            // user.getEmail()).detail(Details.CODE_ID, accessCode.getCodeId()).success();

            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();

            return Response.noContent().build();
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendActionsEmail(e);
            return ErrorResponse.error("failed-to-sent-action-email", Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send an email-verification email to the user
     *
     * An email contains a link the user can click to verify their email address.
     * The redirectUri and clientId parameters are optional. The default for the
     * redirect is the account client.
     *
     * @param redirectUri Redirect uri
     * @param clientId    Client id
     * @return
     */
    @Path("send-verify-email")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendVerifyEmail(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri,
                                    @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId) {
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.VERIFY_EMAIL.name());
        return executeActionsEmail(redirectUri, clientId, null, actions);
    }

    @GET
    @Path("groups")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<GroupRepresentation> groupMembership(@QueryParam("search") String search,
                                                       @QueryParam("first") Integer firstResult,
                                                       @QueryParam("max") Integer maxResults,
                                                       @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        auth.users().requireView(user);

        if (Objects.nonNull(search) && Objects.nonNull(firstResult) && Objects.nonNull(maxResults)) {
            return ModelToRepresentation.searchForGroupByName(user, !briefRepresentation, search.trim(), firstResult,
                    maxResults);
        } else if (Objects.nonNull(firstResult) && Objects.nonNull(maxResults)) {
            return ModelToRepresentation.toGroupHierarchy(user, !briefRepresentation, firstResult, maxResults);
        } else {
            return ModelToRepresentation.toGroupHierarchy(user, !briefRepresentation);
        }
    }

    @GET
    @NoCache
    @Path("groups/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getGroupMembershipCount(@QueryParam("search") String search) {
        auth.users().requireView(user);
        Long results;

        if (Objects.nonNull(search)) {
            results = user.getGroupsCountByNameContaining(search);
        } else {
            results = user.getGroupsCount();
        }
        Map<String, Long> map = new HashMap<>();
        map.put("count", results);
        return map;
    }

    @DELETE
    @Path("groups/{groupId}")
    @NoCache
    public void removeMembership(@PathParam("groupId") String groupId) {
        auth.users().requireManageGroupMembership(user);

        GroupModel group = session.groups().getGroupById(realm, groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        auth.groups().requireManageMembership(group);
        session.setAttribute("groupUsers", user.getEmail());

        try {
            if (user.isMemberOf(group)) {
                user.leaveGroup(group);
                if(user.getAttributes().containsKey("roleViaGroup"))
                    user.removeAttribute("roleViaGroup");

                List<RoleModel> roleModelList = group.getRealmRoleMappingsStream().collect(Collectors.toList());
                triggerNotification(OperationType.DELETE, ResourceType.WORKSPACE_ROLE_MAPPING, user, roleModelList);
                adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP_MEMBERSHIP)
                        .representation(ModelToRepresentation.toRepresentation(group, true))
                        .resourcePath(session.getContext().getUri()).detail("assigned_user",user.getId()).success();
            }
        } catch (ModelException me) {
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(me.getMessage(),
                    MessageFormat.format(messages.getProperty(me.getMessage(), me.getMessage()), me.getParameters()),
                    Status.BAD_REQUEST);
        }
    }

    private void triggerNotification(OperationType opType, ResourceType rType, UserModel user, List<RoleModel> roleModelList){
        AdminEventBuilder adminEventBuilder = new AdminEventBuilder(realm, new AuthDetails(), session, session.getContext().getConnection());

        adminEventBuilder.operation(opType)
                .resource(rType)
                .resourcePath(session.getContext().getUri())
                .representation(roleModelList.stream().map(x->ModelToRepresentation.toRepresentation(x)).collect(Collectors.toList()))
                .detail("assigned_user", user.getId())
                .authUser(user)
                .success();

    }

    @PUT
    @Path("groups/{groupId}")
    @NoCache
    public void joinGroup(@PathParam("groupId") String groupId) {
        auth.users().requireManageGroupMembership(user);
        GroupModel group = session.groups().getGroupById(realm, groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        auth.groups().requireManageMembership(group);
        if (!user.isMemberOf(group)) {
            user.joinGroup(group);
            adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP_MEMBERSHIP)
                    .representation(ModelToRepresentation.toRepresentation(group, true))
                    .resourcePath(session.getContext().getUri()).detail("assigned_user",user.getId()).success();
        }
    }

    @GET
    @Path("bo-claims")
    @NoCache
    public HashMap<String,Object> getVerificationClaimsFromMinio(@QueryParam("verificationId") String verificationId){
        if(!realm.getMode().equals("bo")){
            throw new ErrorResponseException("Access Denied", "Access Denied", Status.BAD_REQUEST);
        }

        if(StringUtil.isBlank(verificationId)){
            throw new ErrorResponseException("Verification ID required", "Verification ID required", Status.BAD_REQUEST);
        }

        String userId = user.getId();
        ObjectMapper objectMapper = new ObjectMapper();

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL).credentials(DEFAULT_ACCESS_KEY,DEFAULT_SECRET_KEY).build();
        try {
            minioClient.ignoreCertCheck();
            InputStream object = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(realm.getName()).object(userId + "/" + verificationId + ".json").build());

            String minioResult = new String(object.readAllBytes(), StandardCharsets.UTF_8);
            if(StringUtil.isBlank(minioResult)){
                throw new ErrorResponseException("No data available", "No data available", Status.BAD_REQUEST);
            }
            return objectMapper.readValue(minioResult, HashMap.class);
        } catch (io.minio.errors.ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException |
                 ServerException | XmlParserException | KeyManagementException e) {
            if (e instanceof KeyManagementException) {
                logger.error("Failed with mino: " + ((KeyManagementException) e).getMessage());
            }
            throw new RuntimeException(e.getMessage());
        }

    }

    /**
     * Converts the specified {@link UserSessionModel} into a
     * {@link UserSessionRepresentation}.
     *
     * @param userSession the model to be converted.
     * @param clientUuid  the client's UUID.
     * @return a reference to the constructed representation or {@code null} if the
     *         session is not associated with the specified
     *         client.
     */
    private UserSessionRepresentation toUserSessionRepresentation(final UserSessionModel userSession,
                                                                  final String clientUuid) {
        UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(userSession);
        // Update lastSessionRefresh with the timestamp from clientSession
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientUuid);
        if (clientSession == null) {
            return null;
        }
        rep.setLastAccess(clientSession.getTimestamp());
        return rep;
    }

    private static UserModel getAccountCreatorOfTenant(KeycloakSession session, RealmModel realm){
            return  session.users().searchForUserByUserAttributeStream(realm,"qv_is_account_creator","true", true )
                    .findFirst()
                    .orElse(null);

    }
    private void triggerNotification(OperationType opType, ResourceType rType,GroupModel group , UserModel user){
        adminEvent.operation(opType)
                .resource(rType)
                .resourcePath(session.getContext().getUri())
                .representation(ModelToRepresentation.toRepresentation(group, true))
                .authUser(user)
                .success();
    }

    private void adminLogs(Map<String, Object> data, RealmModel realm, OperationType operationType, AdminEventBuilder adminEvent){
        data.put("sendMail", "true");
        adminEvent.resource(ResourceType.USER).operation(operationType).authRealm(realm).realm(realm).resourcePath(session.getContext().getUri()).representation(data).success();
    }

    private Map<String, Object> generateOtpAndSetAttrs(RealmModel realm, UserModel oldAccountCreator, UserModel newAccountCreator) {
        Map<String, Object> attributes = new HashMap<>();

        //generate OTP and set expiry
        String code = getOtp(6);
        String codeExpiry = String.valueOf(getEpochDuration("3m"));

        //set Attributes
        if (!checkEmailEmptyForUser(oldAccountCreator) && !checkEmailEmptyForUser(newAccountCreator)) {
            attributes.put("to", oldAccountCreator.getEmail());
            attributes.put("new_acc_creator_email", newAccountCreator.getEmail());
            attributes.put("user_name", newAccountCreator.getUsername());
            attributes.put("code", code);
            attributes.put("expiry", getUnixDuration("3m"));

            //save the OTP as hidden user attribute
            oldAccountCreator.setSingleAttribute(HIDDEN_KEY + realm.getName() + OTP_KEY, code);
            oldAccountCreator.setSingleAttribute(HIDDEN_KEY + realm.getName() + EXP_KEY, codeExpiry);
        }
        return attributes;
    }

    String getAccountCreatorOtpKey() {
        return HIDDEN_KEY + realm.getName() + OTP_KEY;
    }

    String getAccountCreatorExpKey() {
        return HIDDEN_KEY + realm.getName()+ EXP_KEY;
    }

    private static Map<String, Object> setupAttrsForNotify(RealmModel realm, UserModel newAccountCreatorUserModel, UserModel user) {
        Map<String, Object> attributes = new HashMap<>();
        //store data to notify user
        attributes.put("to", newAccountCreatorUserModel.getEmail());
        attributes.put("tenant", realm.getName());
        attributes.put("old_acc_creator_email", user.getEmail());
        return attributes;
    }

    private boolean checkEmailEmptyForUser(UserModel user){
        return user.getEmail().isEmpty() || user.getEmail() == null;
    }

    private boolean checkIsAccountCreatorOrNot(UserModel user){
        return Boolean.parseBoolean(user.getFirstAttribute("qv_is_account_creator"));
    }

    //To check user was verified
    private boolean checkIsUserVerified(UserModel user){
        //If this user is created realm then userInviteStatus Attribute will always null for that user, for other Admin users user attribute will have userInviteStatus
        //so if userInviteStatus was null always going to return true
        return  user.getFirstAttribute("userInviteStatus") == null || user.getFirstAttribute("userInviteStatus").equals("Accepted");
    }

    private boolean updateStripe(String oldACEmail, String newACEmail) throws IOException {

        try{
            Map<String,String> objBodyMap = new HashMap<>();

            objBodyMap.put("oldAccountCreatorEmail", oldACEmail);
            objBodyMap.put("newAccountCreatorEmail", newACEmail);
            objBodyMap.put("tenantId", realm.getId());

            ObjectMapper objectMapper = new ObjectMapper();
            String objBody = objectMapper.writeValueAsString(objBodyMap);

            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, objBody);
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(LICENSE_HOST_URL + "/stripe/customer" )
                    .method("PUT", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            okhttp3.Response response = okHttpClient.newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e){
            throw new RuntimeException(e);
        }

    }
}
