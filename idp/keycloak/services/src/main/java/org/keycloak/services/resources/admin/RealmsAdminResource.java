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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Streams;
import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.actiontoken.ActionTokenConstants;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.marketplace.MarketplaceProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.policy.password.PasswordPolicyNotMetException;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.idm.*;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.clientregistration.policy.impl.CanMultiTenantRegistrationPolicyFactory;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.GeneralHelper;

import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.keycloak.models.Constants.ADMIN_CONSOLE_CLIENT_ID;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.userprofile.UserProfileContext.USER_API;
import static org.keycloak.utils.StreamsUtil.throwIfEmpty;

/**
 * Top level resource for Admin REST API
 *
 * @resource Realms Admin
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmsAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmsAdminResource.class);
    protected AdminAuth auth;
    protected TokenManager tokenManager;

    @Context
    protected KeycloakSession session;

    @Context
    protected ClientConnection clientConnection;

    public RealmsAdminResource(AdminAuth auth, TokenManager tokenManager) {
        this.auth = auth;
        this.tokenManager = tokenManager;
    }

    public static final CacheControl noCache = new CacheControl();

    static {
        noCache.setNoCache(true);
    }

    /**
     * Get accessible realms
     *
     * Returns a list of accessible realms. The list is filtered based on what realms the caller is allowed to view.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<RealmRepresentation> getRealms(@DefaultValue("false") @QueryParam("briefRepresentation") boolean briefRepresentation) {
        Stream<RealmRepresentation> realms = session.realms().getRealmsStream()
                .map(realm -> toRealmRep(realm))
                .filter(Objects::nonNull);
        return throwIfEmpty(realms, new ForbiddenException());
    }

    protected RealmRepresentation toRealmRep(RealmModel realm) {
        if (AdminPermissions.realms(session, auth).isAdmin(realm) || AdminPermissions.realms(session, auth).canView(realm)) {
            boolean isLive = isLiveRealm(realm);
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());
            rep.setLive(isLive);
            rep.setMode(realm.getMode());
            return rep;
        }
        return null;
    }

    protected RealmRepresentation toRealmRep(RealmModel realm, boolean briefRep) {
        if (AdminPermissions.realms(session, auth).canView(realm)) {
            return briefRep ? ModelToRepresentation.toBriefRepresentation(session,realm,false) : ModelToRepresentation.toRepresentation(session, realm, false);
        } else if (AdminPermissions.realms(session, auth).isAdmin(realm)) {
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());
            return rep;
        }
        return null;
    }

    @GET
    @Path("getAllRealms")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<RealmRepresentation> getAllRealms(@QueryParam("realmName") String realmName,
                                                    @QueryParam("search") @DefaultValue("false") boolean search,
                                                    @QueryParam("first") Integer firstResult,
                                                    @QueryParam("max") Integer maxResults,
                                                    @QueryParam("sandboxMode") String sandboxMode) {

        Stream<RealmModel> realmModels;
        firstResult = (firstResult == null || firstResult < 0) ? 0 : firstResult;
        maxResults = (maxResults == null || maxResults < 0) ? Constants.DEFAULT_MAX_RESULTS : maxResults;

        if (search && realmName != null && !realmName.isEmpty() && sandboxMode != null) {
            realmModels = session.realms().searchRealmsByRealmName(realmName, firstResult, maxResults);
            boolean isSandbox = Boolean.parseBoolean(sandboxMode);
            realmModels = realmModels.filter(realm -> realm.isSandboxMode() == isSandbox);
        } else if (search && realmName != null && !realmName.isEmpty()) {
            realmModels = session.realms().searchRealmsByRealmName(realmName, firstResult, maxResults);
        } else if(sandboxMode != null && !sandboxMode.isEmpty()){
            realmModels = getRealmsByMode(sandboxMode, firstResult, maxResults);
        } else if ("ezto verify".equals(EnvironmentReader.getValue("VARIANT", "ezto verify"))) {
            realmModels = getRealmsByMode("false", firstResult, maxResults);
            int realmCount = (int) getRealmsByMode("false", firstResult, maxResults).count();
            if(realmCount < maxResults){
                firstResult = realmCount > 0 ? (realmCount - 1) : 0;
                maxResults = maxResults - realmCount;
                realmModels = Streams.concat(realmModels, getRealmsByMode("true", firstResult, maxResults));
            }
        } else {
            realmModels = session.realms().getRealmsStream(firstResult,maxResults);
        }

        return realmModels.map(this::toBriefRealmRep)
                .filter(Objects::nonNull);
    }

    @GET
    @Path("count")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public int getRealmCount(@QueryParam("realmName") String realmName,
                             @QueryParam("search") @DefaultValue("false") boolean search,
                             @QueryParam("sandboxMode") String sandboxMode) {
        if(!AdminPermissions.realms(session, auth).isAdmin(session.realms().getRealmByName(org.keycloak.Config.getAdminRealm())))
            throw new ForbiddenException();

        if (search && realmName != null && realmName.length() > 0) {
            return session.realms().searchRealmsByRealmNameCount(realmName);
        }

        if(sandboxMode != null && !sandboxMode.isEmpty()){
            return (int) getRealmsBySandboxMode(sandboxMode).count();
        }
        return session.realms().getRealmsCount();
    }

    protected RealmRepresentation toBriefRealmRep(RealmModel realm) {
        if (AdminPermissions.realms(session, auth).canView(realm)) {
            return ModelToRepresentation.toBriefRepresentation(session, realm, false);
        } else if (AdminPermissions.realms(session, auth).isAdmin(realm)) {
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());
            rep.setLive(isLiveRealm(realm));
            rep.setMode(realm.getMode());
            return rep;
        }
        return null;
    }

    private Stream<RealmModel> getRealmsBySandboxMode(String sandboxMode) {
        boolean isSandboxMode = Boolean.parseBoolean(sandboxMode);
        return session.realms().getRealmsStream()
                .filter(realmModel -> realmModel.isSandboxMode() == isSandboxMode);
    }


    private Stream<RealmModel> getRealmsByMode(String sandboxMode, int firstResult, int maxResults){
        boolean isSandbox = Boolean.parseBoolean(sandboxMode);
        return session.realms().getRealmsStream()
                .filter(realmModel -> realmModel.isSandboxMode() == isSandbox)
                .skip(firstResult)
                .limit(maxResults);
    }

    private boolean isLiveRealm(RealmModel realm) {
        return true;
    }

    /**
     * Import a realm
     *
     * Imports a realm from a full representation of that realm.  Realm name must be unique.
     *
     * @param rep JSON representation of the realm
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importRealm(final RealmRepresentation rep) {
        if(isReservedTenantName(rep.getRealm())) {
            return ErrorResponse.exists("Tenant name was Reserved, Please try other name");
        }
        if (!GeneralHelper.isValidTenantName(rep.getRealm())) {
            return ErrorResponse.error("Tenant name can only contain alphanumeric characters, lowercase letters and -" +
                    ". But it can't start nor end with -.", Response.Status.BAD_REQUEST);
        }
        try {
            ClientRegistrationPolicyManager.triggerBeforeRegister(session, RegistrationAuth.AUTHENTICATED, CanMultiTenantRegistrationPolicyFactory.PROVIDER_ID);
        }
        catch (ClientRegistrationPolicyException e) {
            throw new ErrorResponseException(e.getPolicyModel().getName(), e.getDetailMessage(), Response.Status.BAD_REQUEST);
        }
        RealmManager realmManager = new RealmManager(session);
        AdminPermissions.realms(session, auth).requireCreateRealm();
        RealmModel adminRealm = realmManager.getKeycloakAdminstrationRealm();
        logger.debugv("importRealm: {0}", rep.getRealm());

        try {
            RealmModel realm = realmManager.importRealm(rep);
            grantPermissionsToRealmCreator(realm);

            // setting up email templates
            realmManager.uploadEmailTemplatesToMinio(realm);

            // Trigger adminEvent on rootTenant
            AdminEventBuilder adEvt = new AdminEventBuilder(adminRealm, auth, session, clientConnection);
            adEvt.realm(adminRealm.getId());
            adEvt.operation(OperationType.CREATE).resource(ResourceType.WORKSPACE).representation(rep).success();

            setupFlowOverrideForBo(realm);

            if(rep.getAccountCreator() != null) {
                Response response = createAdminUser(realm,rep.getAccountCreator());
                if(response.getStatus() != 200) {
                    return response;
                }
            }

            setUpDefaultAccessKey(realm);


            URI location = AdminRoot.realmsUrl(session.getContext().getUri()).path(realm.getName()).build();
            logger.debugv("imported realm success, sending back: {0}", location.toString());
            return Response.created(location).build();
        } catch (ModelDuplicateException e) {
            logger.error("Conflict detected", e);
            return ErrorResponse.exists("Workspace with same name already exists");
        } catch (PasswordPolicyNotMetException e) {
            logger.error("Password policy not met for user " + e.getUsername(), e);
            if (session.getTransactionManager().isActive()) session.getTransactionManager().setRollbackOnly();
            return ErrorResponse.error("Password policy not met. See logs for details", Response.Status.BAD_REQUEST);
        }
    }

    private void setUpDefaultAccessKey(RealmModel realm) {
        if (realm.getMode().equalsIgnoreCase("bo")) {

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession innerSession) throws JsonProcessingException {
                    MgmtAccessModel accessModel = new MgmtAccessModel();
                    accessModel.setName("default");
                    Map<String,Boolean> permissions = new HashMap<>();
                    permissions.put("Devices@list", true);
                    permissions.put("Devices@remove", true);
                    permissions.put("Devices@enable",true);
                    permissions.put("Devices@disable",true);
                    permissions.put("Devices@mark-as-last-used",true);
                    accessModel.setPermissions(permissions);
                    accessModel.setRealmId(realm.getId());
                    accessModel.setEnabled(true);
                    Optional<UserModel> accountCreator = innerSession.users().getUsersStream(realm).collect(Collectors.toList()).size() == 1 ?
                            innerSession.users().getUsersStream(realm).findFirst():
                            innerSession.users().getUsersStream(realm,false).filter(UserModel::isAccountCreator).findFirst();
                    accountCreator.ifPresent(userModel -> accessModel.setRequestedBy(userModel.getId()));
                    realm.addMgmtAccess(accessModel);
                }
            });
        }
    }

    private void setupFlowOverrideForBo(RealmModel realm) {
        if (realm.getMode() != null && realm.getMode().equalsIgnoreCase("bo")) {
            HashMap<String, String> conf = new HashMap<>();
            conf.put("alias", realm.getMode() + "_" + realm.getName() + "_" + ADMIN_CONSOLE_CLIENT_ID);
            String registerFlowId = KeycloakModelUtils.generateId();
            conf.put("id", registerFlowId);
            conf.put("realm" , realm.getId());

            MarketplaceProvider registrationProvider = session.getProvider(MarketplaceProvider.class,
                    "3655c9eb-5912-4f72-9245-dc548edb5741");
            registrationProvider.install(conf);

            MarketplaceProvider loginProvider = session.getProvider(MarketplaceProvider.class,
                    "40ea930e-ab01-4d53-b2a9-354b1ddd2623");
            String loginFlowId = KeycloakModelUtils.generateId();
            conf.put("id", loginFlowId);
            loginProvider.install(conf);

            MFAConfigRepresentation mfaConfigRepresentation = new MFAConfigRepresentation();
            mfaConfigRepresentation.setMfaProviderId("google-authenticator");
            HashMap<String, String> mfaConf = new HashMap<String, String>();
            mfaConf.put("enable_mfa_recovery", "true");
            mfaConf.put("look_ahead_window", "1");
            mfaConfigRepresentation.setConfig(mfaConf);
            realm.addMFAConfig(RepresentationToModel.toModel(mfaConfigRepresentation));

            Set<String> mfaProviders = new HashSet<String>();
            mfaProviders.add("google-authenticator");
            realm.setMFAProviders(mfaProviders);

            ClientModel dashboardClient = realm.getClientByClientId(ADMIN_CONSOLE_CLIENT_ID);
            dashboardClient.setOverrideAuthentication(true);
            dashboardClient.setAttribute("inviteOnly", "true");
            dashboardClient.setAuthenticationFlowBindingOverride("onboarding", registerFlowId);
            dashboardClient.setAuthenticationFlowBindingOverride("login", loginFlowId);
            dashboardClient.setMFAConnections("is_mandatory", true);
            dashboardClient.setMFAConnections("google-authenticator", true);
        }
    }

    private Response createAdminUser(RealmModel realm, UserRepresentation rep){
        if (session.getContext().getRealm().getEmailConfigsStream().count() == 0) {
            if (session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getEmailConfigsStream().count() == 0) {
                return ErrorResponse.error("Email Provider is not Configured. It requires atleast one email provider to be created and enabled for the tenant", Response.Status.BAD_REQUEST);
            }
        }

        if (rep.getEmail() == null) {
            return ErrorResponse.error("Email is mandatory", Response.Status.BAD_REQUEST);
        }

        String username = rep.getEmail();
        if (ObjectUtil.isBlank(username)) {
            return ErrorResponse.error("User name is missing", Response.Status.BAD_REQUEST);
        }

        // Double-check duplicated username and email here due to federation
        if (session.users().getUserByUsername(username, realm) != null) {
            return ErrorResponse.exists("User exists with same username");
        }
        if (rep.getEmail() != null && !realm.isDuplicateEmailsAllowed()) {
            try {
                if (session.users().getUserByEmail(rep.getEmail(), realm) != null) {
                    return ErrorResponse.exists("User exists with same email");
                }
            } catch (ModelDuplicateException e) {
                return ErrorResponse.exists("User exists with same email");
            }
        }
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = profileProvider.create(USER_API, rep.toAttributes());

        try {
            Response response = UserResource.validateUserProfile(profile, null, session);
            if (response != null) {
                return response;
            }

            //Here we switch to requesting realm and after creating user will switch to root admin realm
//            RealmModel origRealm = session.getContext().getRealm();
            session.getContext().setRealm(realm);
            UserModel user = profile.create();
//            session.getContext().setRealm(origRealm);

            //Set RealmAdmin Role for other than master tenants
            if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
                RoleModel role = realm.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT).getRole(ActionTokenConstants.ADMIN_USER_ROLE);
                user.grantRole(role);
                setMfaEnabled(session,realm,role,user);
            } else {
                //getting adminRole from RealmRoles
                RoleModel adminRole = realm.getRole("admin");

                //If adminRole does not exists
                if (adminRole == null) {
                    logger.error("Could not create Admin user,because Realm Does not have the Admin Role");
                    return ErrorResponse.error("Could not create Admin user", Response.Status.INTERNAL_SERVER_ERROR);
                }

                //else, we assign the admin role to the user
                user.grantRole(realm.getRole("admin"));
                setMfaEnabled(session,realm,adminRole,user);
            }

            UserResource.updateUserFromRep(profile,user, rep, session, false);
            RepresentationToModel.createFederatedIdentities(rep, session, realm, user);
            RepresentationToModel.createGroups(rep, realm, user);
            if (realm.getUserInviteWay().equalsIgnoreCase("otp")) {
                List<CredentialRepresentation> credentials = new ArrayList<>();
                CredentialRepresentation pass = new CredentialRepresentation();
                pass.setType(PASSWORD);
                pass.setValue(HmacOTP.generateSecret(10));
                if (realm.getMode().equalsIgnoreCase("bo")) {
                    pass.setTemporary(false);
                } else {
                    pass.setTemporary(true);
                }
                pass.setUserLabel("userInvited");
                credentials.add(pass);
                rep.setCredentials(credentials);
            }

            RepresentationToModel.createCredentials(rep, session, realm, user, true);
            user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");
            user.setSingleAttribute(UserModel.USER_INVITE_CLIENT,ADMIN_CONSOLE_CLIENT_ID);
            user.setEnabled(true);

            ClientModel clientModel = realm.getMode().equals("wiam") ? realm.getClientByClientId(Constants.SSO_MANAGEMENT_CLIENT_ID) : realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
            String url = ClientResource.getOIDCUrl(clientModel, session, realm, null);
            rep.setAccessUrl(url);

            AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, clientConnection);
            adminEvent.resource(ResourceType.USER);
            adminEvent.realm(realm.getId());
            adminEvent.operation(OperationType.CREATE).resource(ResourceType.ADMIN_USER).representation(rep).success();

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }
            return Response.ok().build();
        } catch (ModelDuplicateException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.exists("User exists with same username or email");
        } catch (PasswordPolicyNotMetException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.error("Password policy not met", Response.Status.BAD_REQUEST);
        } catch (ModelException me) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            logger.warn("Could not create user", me);
            return ErrorResponse.error("Could not create user", Response.Status.BAD_REQUEST);
        }
    }

    private void setMfaEnabled(KeycloakSession session, RealmModel realm, RoleModel role, UserModel userModel) {
        if (hasAdminUser(session, realm, role)) {
            userModel.setMfaEnabled(false);
        } else {
            userModel.setMfaEnabled(true);
        }
    }

    private boolean hasAdminUser(KeycloakSession session, RealmModel realm, RoleModel role) {
        return session.users().getRoleMembersStream(realm,role).count() > 1;
    }

    private boolean isReservedTenantName(String tenantName){
        String env = EnvironmentReader.getValue("RESERVED_TENANT_NAMES");
        if(env == null || env.isEmpty()){
            logger.info("Environment variable is Empty.Ignoring Reserved Tenant Name Check");
            return false;
        }
        String[] allowedNames = env.split(",");
        return  Arrays.stream(allowedNames).anyMatch(x-> x.equals(tenantName));
    }

    private void grantPermissionsToRealmCreator(RealmModel realm) {
        if (auth.hasRealmRole(AdminRoles.ADMIN)) {
            return;
        }

        ClientModel realmAdminApp = realm.getMasterAdminClient();
        Arrays.stream(AdminRoles.ALL_REALM_ROLES)
                .map(realmAdminApp::getRole)
                .forEach(auth.getUser()::grantRole);
    }

    /**
     * Base path for the admin REST API for one particular realm.
     *
     * @param headers
     * @param name realm name (not id!)
     * @return
     */
    @Path("{realm}")
    public RealmAdminResource getRealmAdmin(@Context final HttpHeaders headers,
                                            @PathParam("realm") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) throw new NotFoundException("Realm not found.");

        if (!auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm())
                && !auth.getRealm().equals(realm)) {
            throw new ForbiddenException();
        }
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);

        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, auth, session, clientConnection);
        session.getContext().setRealm(realm);

        RealmAdminResource adminResource = new RealmAdminResource(realmAuth, realm, tokenManager, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(adminResource);
        //resourceContext.initResource(adminResource);
        return adminResource;
    }



    /**
     * Accepts the Multipart Form Data(IMG) and returns the URL of Minio
     */
    @POST
    @Path("{realm}/uploadRealmLogo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveLogoUrl(@PathParam("realm") final String name, final MultipartFormDataInput input) throws IOException {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);

        // if realm is null, then throw notFoundException
        if (realm == null) throw new NotFoundException("Realm not found");

        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();

        if(formDataMap.containsKey("realmLogo")){
            InputPart file = formDataMap.get("realmLogo").get(0);

            //checking file extension
            String extension = FilenameUtils.getExtension(GeneralHelper.getFileName(file.getHeaders()));
            if (!GeneralHelper.checkForCorrectExtension(extension))
                return ErrorResponse.error("Unsupported File Type", Response.Status.OK);

            //setting fileName and fileContentType for storing image in MINIO
            String fileName = realm.getName() + "-logo." + extension;
            String fileContentType = file.getHeaders().get("Content-Type").get(0);

            if(extension.equals("svg") && (!GeneralHelper.isValidSvgFile(file))){
                return ErrorResponse.error("Upload a proper image file", Response.Status.BAD_REQUEST);
            }

            //converting file into InputStream
            InputStream inputStream = file.getBody(InputStream.class, null);

            InputStream imageStream = file.getBody(InputStream.class, null);
            BufferedImage image = ImageIO.read(imageStream);
            if (image == null && !extension.equals("svg")) {
                return ErrorResponse.error("Upload a proper image file", Response.Status.BAD_REQUEST);
            }

            String url = session.getProvider(AvatarProvider.class, "avatar-storage-minio")
                    .saveImgAndReturnUrl(realm.getName(), "public/logos/", fileName, fileContentType, inputStream);

            String returningUrl = "{\"url\": \"" + url + "\"}";

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().build()).entity(returningUrl).type(MediaType.APPLICATION_JSON).build();
        }
        return ErrorResponse.error("Missing Parameters", Response.Status.BAD_REQUEST);
    }



    /**
     * Accepts the Multipart Form Data(IMG) and returns the URL of Minio
     */
    @POST
    @Path("{realm}/uploadBrandImg")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveImageAndReturnUrl(@PathParam("realm") final String name, final MultipartFormDataInput input) throws IOException {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);

        // if realm is null, then throw notFoundException
        if (realm == null) throw new NotFoundException("Realm not found");

        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();

        //Checking Branding Name Presents or not in formData
        if (!formDataMap.containsKey("brandName"))
            return ErrorResponse.error("Missing Branding Name", Response.Status.BAD_REQUEST);

        String brandName = formDataMap.get("brandName").get(0).getBodyAsString();

        if (brandName == null || brandName.length() == 0)
            return ErrorResponse.error("Invalid Branding Name", Response.Status.BAD_REQUEST);

        //Checking BrandingModel matching BrandName exists or not
        BrandingModel brand = session.loginBranding().getBrandByName(realm, brandName);

        if (brand == null)
            return ErrorResponse.error("No Branding Available for the given Branding Name", Response.Status.BAD_REQUEST);

        InputPart file;
        String fileName = "bodyBgImg";
        if (formDataMap.containsKey("bodyBgImg")) {
            file = formDataMap.get("bodyBgImg").get(0);
        } else if (formDataMap.containsKey("sectionBgImg")) {
            file = formDataMap.get("sectionBgImg").get(0);
            fileName = "sectionBgImg";
        } else {
            return ErrorResponse.error("Missing Parameters", Response.Status.BAD_REQUEST);
        }


        //checking file extension
        String extension = FilenameUtils.getExtension(GeneralHelper.getFileName(file.getHeaders()));

        if (!GeneralHelper.checkForCorrectExtension(extension))
            return ErrorResponse.error("Unsupported File Type", Response.Status.OK);

        fileName = fileName + "." + extension;
        String fileContentType = file.getHeaders().get("Content-Type").get(0);

        if(extension.equals("svg") && (!GeneralHelper.isValidSvgFile(file))){
            return ErrorResponse.error("Upload a proper image file", Response.Status.BAD_REQUEST);
        }

        //converting file into InputStream
        InputStream inputStream = file.getBody(InputStream.class, null);

        //Form a ObjectPath to store object in specified path
        String objectPath = "public/" + brandName.replace(' ', '-') + "/";

        String url = session.getProvider(AvatarProvider.class, "avatar-storage-minio")
                .saveImgAndReturnUrl(realm.getName(), objectPath, fileName, fileContentType, inputStream);
        String returningUrl = "{\"url\": \"" + url + "\"}";

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().build()).entity(returningUrl).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * Get All Configured Email Providers
     * <p>
     * Returns a list of configured email providers in the given realm.
     *
     * @return
     */
    @GET
    @Path("{realm}/getConfiguredEmailProviders")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getConfiguredEmailProviders(@PathParam("realm") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);

        // if realm is null, then throw notFoundException
        if (realm == null) throw new NotFoundException("Realm not found.");

        //getting configured email providers of the specified realm.
        List<EmailConfigModel> providers = realm.getEmailConfigsStream().collect(Collectors.toList());

        List<String> providerList = new ArrayList<>();
        if (!providers.isEmpty()) {
            providers.forEach(x -> providerList.add(x.getEmailProviderId()));
        }
        return providerList;
    }

    @GET
    @Path("{realm}/getAllEmailTemplates")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<EmailTemplateRepresentation> getAllEmailTemplates(@PathParam("realm") final String name) {
        List<EmailTemplateRepresentation> emailTemplates = new ArrayList<>();
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);

        // if realm is null, then throw notFoundException
        if (realm == null) throw new NotFoundException("Realm not found.");

        Collection<String> templates;
        //get All Email Templates for this realm
        if(realm.getMode().equalsIgnoreCase("bo")){
            templates = Constants.boEmailTemplates;
        } else {
            templates = Constants.emailTemplates;
        }
        for (String template : templates) {

            //Retrive the Email Template from Minio
            String htmlBody = getEmailTemplate(session, realm.getName(), removeExtension(template));

            emailTemplates.add(ModelToRepresentation.toRepresentation(template, removeExtension(template), htmlBody, htmlBody));
        }
        return emailTemplates;
    }

    @GET
    @Path("{realm}/getEmailTemplate/{templatename}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public EmailTemplateRepresentation getEmailTemplate(@PathParam("realm") final String name, @PathParam("templatename") final String templatename) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);

        // if realm is null, then throw notFoundException
        if (realm == null) throw new NotFoundException("Realm not found.");

        //Retrive the Email Template from Minio
        String htmlBody = getEmailTemplate(session, realm.getName(), removeExtension(templatename));

        return ModelToRepresentation.toRepresentation(templatename, removeExtension(templatename), htmlBody, htmlBody);
    }

    @POST
    @Path("{realm}/saveEmailTemplate")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveEmailTemplate(@PathParam("realm") final String name, final EmailTemplateRepresentation rep) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);

        // if realm is null, then throw notFoundException
        if (realm == null) throw new NotFoundException("Realm not found.");

        //Check for Scripts tag in email Html body.We dont allow due to security purposes.
        if (GeneralHelper.hasScripts(rep.getHtmlBody())) {
            return ErrorResponse.exists("Scripts are not allowed in the Email Templates");
        }
        //Save the template to Minio
        InputStream emailHtmlTemp = new ByteArrayInputStream(rep.getHtmlBody().getBytes());
        boolean isSaved = saveEmailTemplate(session, realm.getName(), rep.getTemplateName(), emailHtmlTemp);
        if (!isSaved) {
            return ErrorResponse.exists("Error Occured on Saving Email Template with the name of " + rep.getTemplateName());
        }
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(rep.getTemplateName()).build()).build();
    }

    @POST
    @Path("{realm}/testTemplate")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testEmailTemplate(@PathParam("realm") final String name, Map<String, String> testConfig) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);

        // if realm is null, then throw notFoundException
        if (realm == null) throw new NotFoundException("Realm not found.");

        //getting EmailAddress  from testConfig Json
        String toEmail = testConfig.get("emailId");
        if (toEmail == null || toEmail.trim().equals("") || toEmail.length() == 0) {
            return ErrorResponse.error("No e-mail is detected for testing connection or e-mail is Empty", Response.Status.INTERNAL_SERVER_ERROR);
        }
        //getting templateName from testConfig Json
        String templateName = testConfig.get("templateName");
        if (templateName == null || templateName.trim().equals("") || templateName.length() == 0) {
            return ErrorResponse.error("No Template is detected for testing connection", Response.Status.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> bodyAttributes = new HashMap<>();
        bodyAttributes.put("htmlBody", getEmailTemplate(session, realm.getName(), removeExtension(templateName)));
        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .send(toEmail, "Test Email Template ", templateName, bodyAttributes);
        } catch (EmailException e) {
            logger.error(e);
            return ErrorResponse.error("failed-to-send-email-check-with-admin", Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return ErrorResponse.error("Config Error :" + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);

        }
        return Response.noContent().build();
    }

    @POST
    @Path("{realm}/resetToDefault/{templatename}")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetEmailTemplate(@PathParam("realm") final String name, @PathParam("templatename") final String templateName) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);

        // if realm is null, then throw notFoundException
        if (realm == null) throw new NotFoundException("Realm not found.");

        //Reset the template in Minio to Default
        try {
            realmManager.resetEmailTemplateInMinio(realm, templateName);
            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(templateName).build()).build();
        } catch (RuntimeException e) {
            return ErrorResponse.exists("Error Occured on Saving Email Template with the name of " + templateName);
        }

    }

    public static String getEmailTemplate(KeycloakSession session, String realmId, String templateName) {
        AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {

            InputStream res = avatarProvider.loadEmailTemplate(realmId, templateName);
            if (res != null) {

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = res.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
            }

        } catch (IOException e) {
            logger.error(e);
        }
        return buffer.toString();
    }

    private static boolean saveEmailTemplate(KeycloakSession session, String realmId, String templateName, InputStream emailHtmlTemp) {
        AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
        try {
            avatarProvider.saveEmailTemplate(realmId, removeExtension(templateName), emailHtmlTemp);
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
        return true;
    }


}
