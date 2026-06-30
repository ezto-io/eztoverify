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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.KeyPairVerifier;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.client.ClientAuthUtil;
import org.keycloak.branding.LoginBrandingProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.Version;
import org.keycloak.common.util.PemUtils;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.*;
import org.keycloak.events.admin.*;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.exportimport.ClientDescriptionConverterFactory;
import org.keycloak.exportimport.util.ExportOptions;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.models.*;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.partialimport.PartialImportManager;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.*;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.*;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.utils.GeneralHelper;
import org.keycloak.utils.ProfileHelper;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.ServicesUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.utils.StripSecretsUtils.stripForExport;
import static org.keycloak.util.JsonSerialization.readValue;
import static org.keycloak.utils.GeneralHelper.*;

/**
 * Base resource class for the admin REST api of one realm
 *
 * @resource Realms Admin
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected AdminPermissionEvaluator auth;
    protected RealmModel realm;
    private TokenManager tokenManager;
    private AdminEventBuilder adminEvent;
    private final String HIDDEN_KEY = "hidden_ua_delete_realm_";
    private final String OTP_KEY = "_req_otp";
    private final String EXP_KEY = "_req_otp_exp";

    @Context
    protected KeycloakSession session;

    @Context
    protected ClientConnection connection;

    @Context
    protected HttpHeaders headers;

    public RealmAdminResource(AdminPermissionEvaluator auth, RealmModel realm, TokenManager tokenManager, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.tokenManager = tokenManager;
        this.adminEvent = adminEvent.realm(realm).resource(ResourceType.WORKSPACE);
    }

    /**
     * Base path for importing clients under this realm.
     *
     * @return
     */
    @Path("client-description-converter")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation convertClientDescription(String description) {
        auth.clients().requireManage();

        if (realm == null) {
            throw new NotFoundException("Realm not found.");
        }

        return session.getKeycloakSessionFactory().getProviderFactoriesStream(ClientDescriptionConverter.class)
                .map(ClientDescriptionConverterFactory.class::cast)
                .filter(factory -> factory.isSupported(description))
                .map(factory -> factory.create(session).convertToInternal(description))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported format"));
    }

    /**
     * Base path for managing attack detection.
     *
     * @return
     */
    @Path("attack-detection")
    public AttackDetectionResource getAttackDetection() {
        AttackDetectionResource resource = new AttackDetectionResource(auth, realm, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Base path for managing Activity details
     *
     * @return
     */
    @Path("activity")
    public ActivityManagementResource getActivityDetails() {
        ActivityManagementResource resource = new ActivityManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("activity/bo")
    public BackofficeActivityManagementResource getBackOfficeActivityDetails() {
        BackofficeActivityManagementResource resource = new BackofficeActivityManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("reports")
    public ReportsManagementResource getReports() {
        ReportsManagementResource resource = new ReportsManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("mgmt_access_keys")
    public MgmtAccessManagementResource getMgmtAccessKeys() {
        MgmtAccessManagementResource resource = new MgmtAccessManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }


    /**
     * Base path for managing clients under this realm.
     *
     * @return
     */
    @Path("clients")
    public ClientsResource getClients() {
        ClientsResource clientsResource = new ClientsResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientsResource);
        return clientsResource;
    }

    /**
     * This endpoint is deprecated. It's here just because of backwards compatibility. Use {@link #getClientScopes()} instead
     *
     * @return
     */
    @Deprecated
    @Path("client-templates")
    public ClientScopesResource getClientTemplates() {
        return getClientScopes();
    }

    /**
     * Base path for managing client scopes under this realm.
     *
     * @return
     */
    @Path("client-scopes")
    public ClientScopesResource getClientScopes() {
        ClientScopesResource clientScopesResource = new ClientScopesResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientScopesResource);
        return clientScopesResource;
    }

    /**
     * Base path for managing localization under this realm.
     */
    @Path("localization")
    public RealmLocalizationResource getLocalization() {
        RealmLocalizationResource resource = new RealmLocalizationResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Get realm default client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-default-client-scopes")
    public Stream<ClientScopeRepresentation> getDefaultDefaultClientScopes() {
        return getDefaultClientScopes(true);
    }

    private Stream<ClientScopeRepresentation> getDefaultClientScopes(boolean defaultScope) {
        auth.clients().requireViewClientScopes();

        return realm.getDefaultClientScopesStream(defaultScope).map(clientScope -> {
            ClientScopeRepresentation rep = new ClientScopeRepresentation();
            rep.setId(clientScope.getId());
            rep.setName(clientScope.getName());
            rep.setProtocol(clientScope.getProtocol());
            return rep;
        });
    }


    @PUT
    @NoCache
    @Path("default-default-client-scopes/{clientScopeId}")
    public void addDefaultDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId,true);
    }

    private void addDefaultClientScope(String clientScopeId, boolean defaultScope) {
        auth.clients().requireManageClientScopes();

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new NotFoundException("Client scope not found");
        }
        realm.addDefaultClientScope(clientScope, defaultScope);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.CLIENT_SCOPE).resourcePath(session.getContext().getUri()).success();
    }


    @DELETE
    @NoCache
    @Path("default-default-client-scopes/{clientScopeId}")
    public void removeDefaultDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        auth.clients().requireManageClientScopes();

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new NotFoundException("Client scope not found");
        }
        realm.removeDefaultClientScope(clientScope);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.CLIENT_SCOPE).resourcePath(session.getContext().getUri()).success();
    }


    /**
     * Get realm optional client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-optional-client-scopes")
    public Stream<ClientScopeRepresentation> getDefaultOptionalClientScopes() {
        return getDefaultClientScopes(false);
    }

    @PUT
    @NoCache
    @Path("default-optional-client-scopes/{clientScopeId}")
    public void addDefaultOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId, false);
    }

    @DELETE
    @NoCache
    @Path("default-optional-client-scopes/{clientScopeId}")
    public void removeDefaultOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        removeDefaultDefaultClientScope(clientScopeId);
    }

    /**
     * Base path for managing client initial access tokens
     *
     * @return
     */
    @Path("clients-initial-access")
    public ClientInitialAccessResource getClientInitialAccess() {
        ClientInitialAccessResource resource = new ClientInitialAccessResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("client-registration-policy")
    public ClientRegistrationPolicyResource getClientRegistrationPolicy() {
        ClientRegistrationPolicyResource resource = new ClientRegistrationPolicyResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Base path for managing components under this realm.
     *
     * @return
     */
    @Path("components")
    public ComponentResource getComponents() {
        ComponentResource resource = new ComponentResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * base path for managing realm-level roles of this realm
     *
     * @return
     */
    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(session, session.getContext().getUri(), realm, auth, realm, adminEvent);
    }

    /**
     * Get the top-level representation of the realm
     *
     * It will not include nested information like User and Client representations.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public RealmRepresentation getRealm() {
        if (auth.realm().canViewRealm()) {
            RealmRepresentation realmRepresentation = ModelToRepresentation.toRepresentation(session, realm, false);
            if(realm.getMode().equals("bo") && !Config.getAdminRealm().equals(realm.getName()) && Config.getAdminRealm().equals(auth.adminAuth().getRealm().getName())){
                realmRepresentation.setAdminRealm(true);
            }
            return realmRepresentation;
        } else {
            auth.realm().requireViewRealmNameList();

            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());

            if (auth.realm().canViewIdentityProviders()) {
                RealmRepresentation r = ModelToRepresentation.toRepresentation(session, realm, false);
                rep.setIdentityProviders(r.getIdentityProviders());
                rep.setIdentityProviderMappers(r.getIdentityProviderMappers());
            }

            return rep;
        }
    }

    /**
     * Update the top-level information of the realm
     *
     * Any user, roles or client information in the representation
     * will be ignored.  This will only update top-level attributes of the realm.
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRealm(final RealmRepresentation rep) {
        auth.realm().requireManageRealm();
        String displayName = rep.getDisplayName();

        logger.debug("updating realm: " + realm.getName());

        if (rep.getRealm() != null && !realm.getName().equalsIgnoreCase(rep.getRealm())) {
            return ErrorResponse.error("Can't rename tenant name", Status.BAD_REQUEST);
        }

        if(displayName == null || displayName.isEmpty()){
            return ErrorResponse.error("displayname-required", Response.Status.BAD_REQUEST);
        }else if (!displayName.trim().matches("^[\\p{L}\\p{M}\\p{Z}\\p{N}\\-]+$")){
            return ErrorResponse.error("displayname-should-be-alpha-numeric", Response.Status.BAD_REQUEST);
        }else if(displayName.trim().length() > 55) {
            return ErrorResponse.error("display-name-should-be-under-55-char", Response.Status.BAD_REQUEST);
        }
        // ReservedCharValidator.validate(rep.getRealm());
        ReservedCharValidator.validateLocales(rep.getSupportedLocales());

        try {
            if (!Constants.GENERATE.equals(rep.getPublicKey()) && (rep.getPrivateKey() != null && rep.getPublicKey() != null)) {
                try {
                    KeyPairVerifier.verify(rep.getPrivateKey(), rep.getPublicKey());
                } catch (VerificationException e) {
                    return ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
                }
            }

            if (!Constants.GENERATE.equals(rep.getPublicKey()) && (rep.getCertificate() != null)) {
                try {
                    X509Certificate cert = PemUtils.decodeCertificate(rep.getCertificate());
                    if (cert == null) {
                        return ErrorResponse.error("Failed to decode certificate", Status.BAD_REQUEST);
                    }
                } catch (Exception e)  {
                    return ErrorResponse.error("Failed to decode certificate", Status.BAD_REQUEST);
                }
            }

            boolean wasDuplicateEmailsAllowed = realm.isDuplicateEmailsAllowed();
            RepresentationToModel.updateRealm(rep, realm, session);

            // Refresh periodic sync tasks for configured federationProviders
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();
            realm.getUserStorageProvidersStream().forEachOrdered(fedProvider ->
                    usersSyncManager.notifyToRefreshPeriodicSync(session, realm, fedProvider, false));

            // This populates the map in DefaultKeycloakContext to be used when treating the event
            session.getContext().getUri();

            adminEvent.operation(OperationType.UPDATE).representation(StripSecretsUtils.strip(rep)).success();
            
            if (rep.isDuplicateEmailsAllowed() != null && rep.isDuplicateEmailsAllowed() != wasDuplicateEmailsAllowed) {
                UserCache cache = session.getProvider(UserCache.class);
                if (cache != null) cache.clear();
            }
            
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Realm with same name exists");
        } catch (ModelException e) {
            return ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ErrorResponse.error("Failed to update realm", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete the realm
     *
     */
    @DELETE
    public Response deleteRealm(@QueryParam("forceDelete") @DefaultValue("false") boolean forceDelete) {
        try{
            auth.realm().requireManageRealm();

            if (Version.OFFERING.equalsIgnoreCase("cloud")) {
                if (forceDelete) {
                    //fetching authenticatedUser from auth token
                    UserModel authenticatedUser = auth.adminAuth().getUser();
                    if (authenticatedUser.getEmail() == null || authenticatedUser.getEmail().length() == 0) {
                        return ErrorResponse.error("User does not have the email to send OTP for delete tenant request", Response.Status.BAD_REQUEST);
                    }

                    Map<String, Object> data = generateOtpAndSetAttrs(realm, authenticatedUser);
                    data.put("realm", realm.getId());
                    adminLogs(data, realm, OperationType.HARD_DELETE_TRIGGER, adminEvent, session.getContext().getRealm());

                } else {
                    Response licenseResp = checkLicenseIsValid();
                    if (licenseResp != null)
                        return licenseResp;

                    boolean result = !realm.isEnabled();
                    realm.setEnabled(result);
                    Map<String, Object> data = new HashMap<>();
                    data.put("isEnabled", result);

                    RealmRepresentation realmRepresentation = new RealmRepresentation();
                    realmRepresentation.setRealm(realm.getId());
                    Map<String, String> attributes= new HashMap<>();
                    attributes.put("isEnabled", data.get("isEnabled").toString());
                    realmRepresentation.setAttributes(attributes);

                    adminLogs(data, realm, OperationType.SOFT_DELETE, adminEvent, session.getContext().getRealm());

                }
            } else {
                if (!new RealmManager(session).removeRealm(realm)) {
                    return ErrorResponse.error("Realm doesn't exist", Response.Status.BAD_REQUEST);
                }
            }
        } catch (Exception e){
            logger.error(e.getMessage());
            return ErrorResponse.error(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
        }
        return Response.noContent().build();
    }

    private Response checkLicenseIsValid() {
        //we need to check license only if mode is cloud
        if (Version.OFFERING.equalsIgnoreCase("cloud")) {
            try {
                ClientRegistrationPolicyManager.triggerBeforeRegister(session, RegistrationAuth.AUTHENTICATED, "license-checker");
            } catch (ClientRegistrationPolicyException crpe) {
                return ErrorResponse.error(crpe.getDetailMessage(), Status.BAD_REQUEST);
            }
        }
        return null;
    }


    private void adminLogs(Map<String, Object> data, RealmModel realm, OperationType operationType, AdminEventBuilder adminEvent, RealmModel oldRealm){
        RealmModel masterRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());

        switch (operationType){
            case HARD_DELETE_TRIGGER:
                adminEvent.resource(ResourceType.WORKSPACE).operation(operationType).authRealm(realm).realm(realm).resourcePath(session.getContext().getUri()).representation(data).success();
                session.getContext().setRealm(masterRealm);
                data.put("sendMail", "true");
                adminEvent.resource(ResourceType.WORKSPACE).operation(operationType).authRealm(masterRealm).realm(masterRealm).resourcePath(session.getContext().getUri()).representation(data).success();
                session.getContext().setRealm(oldRealm);
                break;
            case SOFT_DELETE:
                adminEvent.resource(ResourceType.WORKSPACE).operation(operationType).authRealm(realm).realm(realm).resourcePath(session.getContext().getUri()).representation(data).success();
                session.getContext().setRealm(masterRealm);
                adminEvent.resource(ResourceType.WORKSPACE).operation(operationType).authRealm(masterRealm).realm(masterRealm).resourcePath(session.getContext().getUri()).representation(data).success();
                session.getContext().setRealm(oldRealm);
                break;

            case HARD_DELETE:
                session.getContext().setRealm(masterRealm);
                adminEvent.resource(ResourceType.WORKSPACE).operation(operationType).authRealm(masterRealm).realm(masterRealm).resourcePath(session.getContext().getUri()).representation(data).success();
                session.getContext().setRealm(oldRealm);
                break;

        }
    }

    private Map<String, Object> generateOtpAndSetAttrs(RealmModel realm, UserModel user) {
        Map<String, Object> attributes = new HashMap<>();

        //generate OTP and set expiry
        String code = getOtp(6);
        String codeExpiry = String.valueOf(getEpochDuration("3m"));

        //set Attributes
        if (user != null && user.getEmail() != null) {
            attributes.put("to", user.getEmail());
            attributes.put("tenant", getRealmName(realm));
            attributes.put("code", code);
            attributes.put("expiry", getUnixDuration("3m"));

            //save the OTP as hidden user attribute
            user.setSingleAttribute(HIDDEN_KEY + realm.getName() + OTP_KEY, code);
            user.setSingleAttribute(HIDDEN_KEY + realm.getName() + EXP_KEY, codeExpiry);
        }
        return attributes;
    }

    // Validate Force remove of tenant
    @POST
    @Path("validate-force-delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateForceDelete(Map<String, String> otpConfig) {
        UserModel adminUser = auth.adminAuth().getUser();
        String requestOtp = otpConfig.get("deleteReqCode");

        if(adminUser.getEmail().isEmpty() || adminUser.getEmail() == null) {
            return ErrorResponse.error("Email is required for validation", Status.BAD_REQUEST);
        }

        if(requestOtp == null || requestOtp.isEmpty()) {
            return ErrorResponse.error("OTP is required for validation", Status.BAD_REQUEST);
        }

        UserModel accountCreator = session.users().searchForUserByUserAttributeStream(realm, "qv_is_account_creator",
                        "true", true)
                .findFirst()
                .orElse(null);

        boolean triggeredByCreator = accountCreator!=null && accountCreator.getEmail().equals(adminUser.getEmail());

        String otp = adminUser.getFirstAttribute(getRealmDeleteOtpKey());
        String otpExp = adminUser.getFirstAttribute(getRealmDeleteOtpExpKey());

        if(otp != null && otpExp != null) {
            if (System.currentTimeMillis() > Long.parseLong(otpExp)) {
                return ErrorResponse.error("The OTP you entered is expired", Response.Status.BAD_REQUEST);
            }

            if(!requestOtp.equals(otp)) {
                return ErrorResponse.error("The OTP you entered is incorrect.Please enter valid OTP", Status.BAD_REQUEST);
            }

            Map<String, Object> data = setupAttrsForNotify(realm, adminUser, triggeredByCreator, accountCreator);

            adminUser.removeAttribute(getRealmDeleteOtpKey());
            adminUser.removeAttribute(getRealmDeleteOtpExpKey());

            if(!new RealmManager(session).removeRealm(realm)) {
                return ClientAuthUtil.errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "workspace_not_exist", "Workspace does not exist");
            }

            data.put("realm", realm.getId());
            adminLogs(data, realm, OperationType.HARD_DELETE, adminEvent, session.getContext().getRealm());

            // License delete account skipped

            return Response.noContent().build();
        } else {
            return ErrorResponse.error("Invalid Request", Status.BAD_REQUEST);
        }
    }

    String getRealmDeleteOtpKey() {
        return HIDDEN_KEY + realm.getName() + OTP_KEY;
    }

    String getRealmDeleteOtpExpKey() {
        return HIDDEN_KEY + realm.getName() + EXP_KEY;
    }

    private static Map<String, Object> setupAttrsForNotify(RealmModel realm, UserModel user, boolean triggererIsAccountCreater, UserModel accCreator) {
        Map<String, Object> attributes = new HashMap<>();
        //store data to notify user
        attributes.put("to", user.getEmail());
        attributes.put("tenant", getRealmName(realm));
        attributes.put("isAccCreator", triggererIsAccountCreater);
        if (!triggererIsAccountCreater && accCreator != null) {
            attributes.put("accCreatorEml", accCreator.getEmail());
        }
        return attributes;
    }

    /**
     * Base path for managing users in this realm.
     *
     * @return
     */
    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        //resourceContext.initResource(users);
        return users;
    }

    @NoCache
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users-management-permissions")
    public ManagementPermissionReference getUserMgmtPermissions() {
        auth.realm().requireViewRealm();

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (permissions.users().isPermissionsEnabled()) {
            return toUsersMgmtRef(permissions);
        } else {
            return new ManagementPermissionReference();
        }

    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Path("users-management-permissions")
    public ManagementPermissionReference setUsersManagementPermissionsEnabled(ManagementPermissionReference ref) {
        auth.realm().requireManageRealm();

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.users().setPermissionsEnabled(ref.isEnabled());
        if (ref.isEnabled()) {
            return toUsersMgmtRef(permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }


    public static ManagementPermissionReference toUsersMgmtRef(AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.users().resource().getId());
        Map<String, String> scopes = permissions.users().getPermissions();
        ref.setScopePermissions(scopes);
        return ref;
    }


    @Path("user-storage")
    public UserStorageProviderResource userStorage() {
        UserStorageProviderResource fed = new UserStorageProviderResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(fed);
        //resourceContext.initResource(fed);
        return fed;
    }

    @Path("{extension}")
    public Object extension(@PathParam("extension") String extension) {
        AdminRealmResourceProvider provider = session.getProvider(AdminRealmResourceProvider.class, extension);
        if (provider != null) {
            Object resource = provider.getResource(session, realm, auth, adminEvent);
            if (resource != null) {
                return resource;
            }
        }

        throw new NotFoundException();
    }

    @Path("authentication")
    public AuthenticationManagementResource flows() {
        AuthenticationManagementResource resource = new AuthenticationManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;

    }

//    @Path("license")
//    public LicenseManagementResource licenseFlows() {
//        LicenseManagementResource resource = new LicenseManagementResource(realm, session, auth, adminEvent);
//        ResteasyProviderFactory.getInstance().injectProperties(resource);
//        //resourceContext.initResource(resource);
//        return resource;
//
//    }

    @Path("users-sync")
    public UserSyncManagementResource syncUsers(){
        UserSyncManagementResource resource = new UserSyncManagementResource(realm,session,auth,adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }


    @Path("azureadsync")
    public AzureAdUserSyncManagementResource syncADUsers() {
        AzureAdUserSyncManagementResource resource = new AzureAdUserSyncManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }


    @Path("special-attributes")
    public RealmAttributeManagementResource specialAttrs() {
        RealmAttributeManagementResource resource = new RealmAttributeManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("dns")
    public DnsManagementResource dns() {
        DnsManagementResource resource = new DnsManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("downloads")
    public DownloadManagementResource downloads() {
        if(realm.getMode().equalsIgnoreCase("ciam"))
            throw new ForbiddenException();
        DownloadManagementResource resource = new DownloadManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("marketplace")
    public MarketplaceManagementResource marketplaceFlows() {
        MarketplaceManagementResource resource = new MarketplaceManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;

    }

    @Path("policies")
    public PoliciesManagementResource policies() {
        RealmPolicyProvider provider = session.policies();
        PoliciesManagementResource resource = new PoliciesManagementResource(realm, session, auth, adminEvent, provider);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;

    }

    @Path("brandings")
    public BrandingManagementResource loginBranding() {
        LoginBrandingProvider provider = session.loginBranding();
        BrandingManagementResource resource = new BrandingManagementResource(realm, session, auth, adminEvent, provider);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;

    }

    /**
     * Path for managing all realm-level or client-level roles defined in this realm by its id.
     *
     * @return
     */
    @Path("roles-by-id")
    public RoleByIdResource rolesById() {
         RoleByIdResource resource = new RoleByIdResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;
    }

    /**
     * Push the realm's revocation policy to any client that has an admin url associated with it.
     *
     */
    @Path("push-revocation")
    @POST
    public GlobalRequestResult pushRevocation() {
        auth.realm().requireManageRealm();

        GlobalRequestResult result = new ResourceAdminManager(session).pushRealmRevocationPolicy(realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    /**
     * Removes all user sessions.  Any client that has an admin url will also be told to invalidate any sessions
     * they have.
     *
     */
    @Path("logout-all")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public GlobalRequestResult logoutAll() {
        auth.users().requireManage();

        session.sessions().removeUserSessions(realm);
        GlobalRequestResult result = new ResourceAdminManager(session).logoutAll(realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    /**
     * Remove a specific user session. Any client that has an admin url will also be told to invalidate this
     * particular session.
     *
     * @param sessionId
     */
    @Path("sessions/{session}")
    @DELETE
    public void deleteSession(@PathParam("session") String sessionId) {
        auth.users().requireManage();

        UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
        if (userSession == null) throw new NotFoundException("Sesssion not found");
        AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), connection, headers, true);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.USER_SESSION).resourcePath(session.getContext().getUri()).success();

    }

    /**
     * Get client session stats
     *
     * Returns a JSON map.  The key is the client id, the value is the number of sessions that currently are active
     * with that client.  Only clients that actually have a session associated with them will be in this map.
     *
     * @return
     */
    @Path("client-session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<Map<String, String>> getClientSessionStats() {
        auth.realm().requireViewRealm();

        Map<String, Map<String, String>> data = new HashMap<>();
        {
            Map<String, Long> activeCount = session.sessions().getActiveClientSessionStats(realm, false);
            for (Map.Entry<String, Long> entry : activeCount.entrySet()) {
                Map<String, String> map = new HashMap<>();
                ClientModel client = realm.getClientById(entry.getKey());
                if (client == null)
                    continue;
                map.put("id", client.getId());
                map.put("clientId", client.getClientId());
                map.put("active", entry.getValue().toString());
                map.put("offline", "0");
                data.put(client.getId(), map);
            }
        }
        {
            Map<String, Long> offlineCount = session.sessions().getActiveClientSessionStats(realm, true);
            for (Map.Entry<String, Long> entry : offlineCount.entrySet()) {
                Map<String, String> map = data.get(entry.getKey());
                if (map == null) {
                    map = new HashMap<>();
                    ClientModel client = realm.getClientById(entry.getKey());
                    if (client == null)
                        continue;
                    map.put("id", client.getId());
                    map.put("clientId", client.getClientId());
                    map.put("active", "0");
                    data.put(client.getId(), map);
                }
                map.put("offline", entry.getValue().toString());
            }
        }

        return data.values().stream();
    }

    /**
     * Get the events provider configuration
     *
     * Returns JSON object with events provider configuration
     *
     * @return
     */
    @GET
    @NoCache
    @Path("events/config")
    @Produces(MediaType.APPLICATION_JSON)
    public RealmEventsConfigRepresentation getRealmEventsConfig() {
        auth.realm().requireViewEvents();

        RealmEventsConfigRepresentation config = ModelToRepresentation.toEventsConfigReprensetation(realm);
        if (config.getEnabledEventTypes() == null || config.getEnabledEventTypes().isEmpty()) {
            List<String> eventTypes = Arrays.stream(EventType.values())
                    .filter(EventType::isSaveByDefault)
                    .map(EventType::name)
                    .collect(Collectors.toList());
            config.setEnabledEventTypes(eventTypes);
        }
        return config;
    }

    /**
     * Update the events provider
     *
     * Change the events provider and/or its configuration
     *
     * @param rep
     */
    @PUT
    @Path("events/config")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRealmEventsConfig(final RealmEventsConfigRepresentation rep) {
        auth.realm().requireManageEvents();

        logger.debug("updating realm events config: " + realm.getName());
        new RealmManager(session).updateRealmEventsConfig(rep, realm);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.WORKSPACE).realm(realm)
                .resourcePath(session.getContext().getUri()).representation(rep)
                // refresh the builder to consider old and new config
                .refreshRealmEventsConfig(session)
                .success();
    }

    /**
     * Get the email provider configuration
     * <p>
     * Returns JSON object with email provider configuration
     *
     * @return
     */
    @GET
    @NoCache
    @Path("email/config")
    @Produces(MediaType.APPLICATION_JSON)
    public RealmEmailConfigRepresentation getRealmEmailConfig() {
        auth.realm().requireViewEvents();

        RealmEmailConfigRepresentation config = ModelToRepresentation.toEmailConfigRepresentation(realm);
        return config;
    }

    /**
     * Update the email provider
     * <p>
     * Change the email provider and/or its configuration
     *
     * @param rep
     */
    @PUT
    @Path("email/config")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRealmEmailConfig(final RealmEmailConfigRepresentation rep) {
        auth.realm().requireManageEvents();

        logger.debug("updating realm email config: " + realm.getName());
        new RealmManager(session).updateRealmEmailConfig(rep, realm);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.WORKSPACE).realm(realm)
                .resourcePath(session.getContext().getUri()).representation(rep)
                // refresh the builder to consider old and new config
                .refreshRealmEventsConfig(session)
                .success();
    }

    /**
     * Get the SMS provider configuration
     * <p>
     * Returns JSON object with SMS provider configuration
     *
     * @return
     */
    @GET
    @NoCache
    @Path("sms/config")
    @Produces(MediaType.APPLICATION_JSON)
    public RealmSmsConfigRepresentation getRealmSmsConfig() {
        auth.realm().requireViewEvents();

        RealmSmsConfigRepresentation config = ModelToRepresentation.toSmsConfigRepresentation(realm);
        return config;
    }

    /**
     * Update the SMS provider
     * <p>
     * Change the SMS provider and/or its configuration
     *
     * @param rep
     */
    @PUT
    @Path("sms/config")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRealmSmsConfig(final RealmSmsConfigRepresentation rep) {
        auth.realm().requireManageEvents();

        logger.debug("updating realm sms config: " + realm.getName());
        new RealmManager(session).updateRealmSmsConfig(rep, realm);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.WORKSPACE).realm(realm)
                .resourcePath(session.getContext().getUri()).representation(rep)
                // refresh the builder to consider old and new config
                .refreshRealmEventsConfig(session)
                .success();
    }

    /**
     * Get the MFA provider configuration
     * <p>
     * Returns JSON object with MFA provider configuration
     *
     * @return
     */
    @GET
    @NoCache
    @Path("mfa/config")
    @Produces(MediaType.APPLICATION_JSON)
    public RealmMFAConfigRepresentation getRealmMFAConfig() {
        auth.realm().requireViewEvents();

        RealmMFAConfigRepresentation config = ModelToRepresentation.toMFAConfigRepresentation(realm);
        return config;
    }

    /**
     * Update the MFA provider
     * <p>
     * Change the MFA provider and/or its configuration
     *
     * @param rep
     */
    @PUT
    @Path("mfa/config")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRealmMFAConfig(final RealmMFAConfigRepresentation rep) {
        auth.realm().requireManageEvents();

        logger.debug("updating realm mfa config: " + realm.getName());
        new RealmManager(session).updateRealmMFAConfig(rep, realm);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.WORKSPACE).realm(realm)
                .resourcePath(session.getContext().getUri()).representation(rep)
                // refresh the builder to consider old and new config
                .refreshRealmEventsConfig(session)
                .success();
    }

    /**
     * Test LDAP connection
     *
     * @param action
     * @param connectionUrl
     * @param bindDn
     * @param bindCredential
     * @return
     */
    @Path("testLDAPConnection")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Deprecated
    public Response testLDAPConnection(@FormParam("action") String action, @FormParam("connectionUrl") String connectionUrl,
                                       @FormParam("bindDn") String bindDn, @FormParam("bindCredential") String bindCredential,
                                       @FormParam("useTruststoreSpi") String useTruststoreSpi, @FormParam("connectionTimeout") String connectionTimeout,
                                       @FormParam("componentId") String componentId, @FormParam("startTls") String startTls) {
        auth.realm().requireManageRealm();

        TestLdapConnectionRepresentation config = new TestLdapConnectionRepresentation(action, connectionUrl, bindDn, bindCredential, useTruststoreSpi, connectionTimeout, startTls, LDAPConstants.AUTH_TYPE_SIMPLE);
        config.setComponentId(componentId);
        boolean result = LDAPServerCapabilitiesManager.testLDAP(config, session, realm);
        return result ? Response.noContent().build() : ErrorResponse.error("LDAP test error", Response.Status.BAD_REQUEST);
    }

    /**
     * Test LDAP connection
     * @return
     */
    @Path("testLDAPConnection")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testLDAPConnection(TestLdapConnectionRepresentation config) {
        auth.realm().requireManageRealm();

        boolean result = LDAPServerCapabilitiesManager.testLDAP(config, session, realm);
        return result ? Response.noContent().build() : ErrorResponse.error("LDAP test error", Response.Status.BAD_REQUEST);
    }

    /**
     * Get LDAP supported extensions.
     * @param config LDAP configuration
     * @return
     */
    @POST
    @Path("ldap-server-capabilities")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response ldapServerCapabilities(TestLdapConnectionRepresentation config) {
        auth.realm().requireManageRealm();
        try {
            Set<LDAPCapabilityRepresentation> ldapCapabilities = LDAPServerCapabilitiesManager.queryServerCapabilities(config, session, realm);
            return Response.ok().entity(ldapCapabilities).build();
        } catch (Exception e) {
            return ErrorResponse.error("ldapServerCapabilities error", Status.BAD_REQUEST);
        }
    }

    /**
     * Test SMTP connection with current logged in user
     *
     * @param config SMTP server configuration
     * @return
     * @throws Exception
     */
    @Path("testSMTPConnection")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Deprecated
    public Response testSMTPConnection(final @FormParam("config") String config) throws Exception {
        Map<String, String> settings = readValue(config, new TypeReference<Map<String, String>>() {
        });
        return testSMTPConnection(settings);
    }

    @Path("testSMTPConnection")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testSMTPConnection(Map<String, String> settings) throws Exception {
        try {
            UserModel user = auth.adminAuth().getUser();
            if (user.getEmail() == null) {
                return ErrorResponse.error("Logged in user does not have an e-mail.", Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (ComponentRepresentation.SECRET_VALUE.equals(settings.get("password"))) {
                settings.put("password", realm.getSmtpConfig().get("password"));
            }
            adminEvent.operation(OperationType.TRIGGER).resource(ResourceType.EMAIL_TEST).representation(settings).success();
            session.getProvider(EmailTemplateProvider.class).sendSmtpTestEmail(settings, user);
        } catch (Exception e) {
            e.printStackTrace();
            logger.errorf("Failed to send email \n %s", e.getCause());
            adminEvent.operation(OperationType.FAILED).resource(ResourceType.EMAIL_TEST).representation(settings).error(e.getMessage()).success();
            return ErrorResponse.error("Failed to send email", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent().build();
    }

    /**
     * Test SMS connection with Given Phone Number.
     *
     * @param config MobileNumber Data
     * @return
     * @throws Exception
     */
    @Path("testSMSConnection")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Deprecated
    public Response testSMSConnection(final @FormParam("config") String config) throws Exception {
        Map<String, String> mobileNumData = readValue(config, new TypeReference<Map<String, String>>() {
        });
        return testSMSConnection(mobileNumData);
    }

    @Path("testSMSConnection")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testSMSConnection(Map<String, String> mobileNumData) throws Exception {
        try {
            //getting phoneNumber from mobileNumData Json
            String toPhone = mobileNumData.get("phoneNumber");

            if (toPhone == null) {
                adminEvent.operation(OperationType.FAILED).resource(ResourceType.SMS_TEST).representation(mobileNumData).error("No PhoneNumber is detected for testing connection or Phone Number is Empty").success();

                return ErrorResponse.error("No PhoneNumber is detected for testing connection or Phone Number is Empty", Response.Status.INTERNAL_SERVER_ERROR);
            }

            //Default Message for Testing Connection
            String message = "This is the test message for testing SMS Connection";

            //getting SMSProvider and Sending SMS
            SmsSenderProvider smsSenderProvider = session.getProvider(SmsSenderProvider.class, "default");

            //checking provider has support for SMS
            if (!smsSenderProvider.isSmsSupported()) {
                adminEvent.operation(OperationType.FAILED).resource(ResourceType.SMS_TEST).representation(mobileNumData).error("Your Provider does not have the support for sending SMS").success();
                return ErrorResponse.error("Your Provider does not have the support for sending SMS", Response.Status.INTERNAL_SERVER_ERROR);
            }
            adminEvent.operation(OperationType.TRIGGER).resource(ResourceType.SMS_TEST).representation(mobileNumData).success();
            smsSenderProvider.sendSMS(toPhone, message, null);
        } catch (SmsException e) {
            e.printStackTrace();
            String errorMessage = "Failed to send SMS (Reason: Unknown)";
            if (e.getCause() != null) {
                errorMessage = String.format("Failed to send SMS (Reason: %s)", e.getCause());
            } else if (e.getMessage() != null) {
                errorMessage = String.format("Failed to send SMS (Reason: %s)", e.getMessage());
            }
            adminEvent.operation(OperationType.FAILED).resource(ResourceType.SMS_TEST).representation(mobileNumData).error(errorMessage).success();

            return ErrorResponse.error(errorMessage, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent().build();
    }

    @Path("identity-provider")
    public IdentityProvidersResource getIdentityProviderResource() {
        return new IdentityProvidersResource(realm, session, this.auth, adminEvent);
    }

    /**
     * Get group hierarchy.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-groups")
    public Stream<GroupRepresentation> getDefaultGroups() {
        auth.realm().requireViewRealm();

        return realm.getDefaultGroupsStream().map(ServicesUtils::groupToBriefRepresentation);
    }
    @PUT
    @NoCache
    @Path("default-groups/{groupId}")
    public void addDefaultGroup(@PathParam("groupId") String groupId) {
        auth.realm().requireManageRealm();

        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        realm.addDefaultGroup(group);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP).resourcePath(session.getContext().getUri()).success();
    }

    @DELETE
    @NoCache
    @Path("default-groups/{groupId}")
    public void removeDefaultGroup(@PathParam("groupId") String groupId) {
        auth.realm().requireManageRealm();

        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        realm.removeDefaultGroup(group);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP).resourcePath(session.getContext().getUri()).success();
    }


    @Path("groups")
    public GroupsResource getGroups() {
        GroupsResource resource =  new GroupsResource(realm, session, this.auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }


    @GET
    @Path("group-by-path/{path: .*}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroupByPath(@PathParam("path") String path) {
        GroupModel found = KeycloakModelUtils.findGroupByPath(realm, path);
        if (found == null) {
            throw new NotFoundException("Group path does not exist");

        }
        auth.groups().requireView(found);
        return ModelToRepresentation.toGroupHierarchy(found, true);
    }

    /**
     * Partial import from a JSON file to an existing realm.
     *
     * @param rep
     * @return
     */
    @Path("partialImport")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response partialImport(PartialImportRepresentation rep) {
        auth.realm().requireManageRealm();

        PartialImportManager partialImport = new PartialImportManager(rep, session, realm, adminEvent);
        return partialImport.saveResources();
    }

    /**
     * Partial export of existing realm into a JSON file.
     *
     * @param exportGroupsAndRoles
     * @param exportClients
     * @return
     */
    @Path("partial-export")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public RealmRepresentation partialExport(@QueryParam("exportGroupsAndRoles") Boolean exportGroupsAndRoles,
                                                     @QueryParam("exportClients") Boolean exportClients) {
        auth.realm().requireViewRealm();

        boolean groupsAndRolesExported = exportGroupsAndRoles != null && exportGroupsAndRoles;
        boolean clientsExported = exportClients != null && exportClients;

        if (groupsAndRolesExported) {
            auth.groups().requireList();
        }
        if (clientsExported) {
            auth.clients().requireView();
        }

        // service accounts are exported if the clients are exported
        // this means that if clients is true but groups/roles is false the service account is exported without roles
        // the other option is just include service accounts if clientsExported && groupsAndRolesExported
        ExportOptions options = new ExportOptions(false, clientsExported, groupsAndRolesExported, clientsExported);
        RealmRepresentation rep = ExportUtils.exportRealm(session, realm, options, false);
        return stripForExport(session, rep);
    }

    /**
     * Clear realm cache
     *
     */
    @Path("clear-realm-cache")
    @POST
    public void clearRealmCache() {
        auth.realm().requireManageRealm();

        CacheRealmProvider cache = session.getProvider(CacheRealmProvider.class);
        if (cache != null) {
            cache.clear();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Clear user cache
     *
     */
    @Path("clear-user-cache")
    @POST
    public void clearUserCache() {
        auth.realm().requireManageRealm();

        UserCache cache = session.getProvider(UserCache.class);
        if (cache != null) {
            cache.clear();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Clear cache of external public keys (Public keys of clients or Identity providers)
     *
     */
    @Path("clear-keys-cache")
    @POST
    public void clearKeysCache() {
        auth.realm().requireManageRealm();

        PublicKeyStorageProvider cache = session.getProvider(PublicKeyStorageProvider.class);
        if (cache != null) {
            cache.clearCache();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
    }

    @Path("clear-report-queue")
    @POST
    public void clearReportQueur() {
        auth.realm().requireManageRealm();

        realm.removeAttribute("isReportProgress");

        adminEvent.operation(OperationType.CLEAR).resource(ResourceType.REPORT).authUser(auth.adminAuth().getUser()).success();
    }

    @Path("keys")
    public KeyResource keys() {
        KeyResource resource =  new KeyResource(realm, session, this.auth);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @GET
    @Path("credential-registrators/{actionid}")
    @NoCache
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Stream<String> getCredentialRegistrators(@PathParam("actionid") String actionId) {
        auth.realm().requireViewRealm();
        return session.getContext().getRealm().getRequiredActionProvidersStream(actionId)
                .filter(ra -> ra.isEnabled())
                .map(RequiredActionProviderModel::getProviderId)
                .filter(providerId -> session.getProvider(RequiredActionProvider.class, providerId) instanceof CredentialRegistrator);
    }

    @Path("client-policies/policies")
    public ClientPoliciesResource getClientPoliciesResource() {
        ProfileHelper.requireFeature(Profile.Feature.CLIENT_POLICIES);
        ClientPoliciesResource resource = new ClientPoliciesResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("client-policies/profiles")
    public ClientProfilesResource getClientProfilesResource() {
        ProfileHelper.requireFeature(Profile.Feature.CLIENT_POLICIES);
        ClientProfilesResource resource = new ClientProfilesResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("wallet")
    public WalletManagementResource getWalletManagementResource(){
        WalletManagementResource resource = new WalletManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("products")
    public ProductManagementResource getProductManagementResource() {
        ProductManagementResource resource = new ProductManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("subscription")
    public SubscriptionManagementResource getSubscriptionManagementResource() {
        SubscriptionManagementResource resource = new SubscriptionManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    private static String getRealmName(RealmModel realm){
        return !TextUtils.isEmpty(realm.getDisplayName()) ? realm.getDisplayName() : realm.getName();
    }
}
