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

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.authentication.actiontoken.ActionTokenConstants;
import org.keycloak.authentication.authenticators.utils.GeneralHelper;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.policy.password.PasswordPolicyNotMetException;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.SearchQueryUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.authentication.authenticators.utils.Constants.HTML_INJECTED_IN_FIELDS;
import static org.keycloak.models.Constants.ADMIN_CONSOLE_CLIENT_ID;
import static org.keycloak.models.utils.KeycloakModelUtils.findGroupByPath;
import static org.keycloak.userprofile.UserProfileContext.USER_API;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 * @resource Users
 */
public class UsersResource {

    private static final Logger logger = Logger.getLogger(UsersResource.class);
    private static final String SEARCH_ID_PARAMETER = "id:";

    protected RealmModel realm;

    private AdminPermissionEvaluator auth;

    private AdminEventBuilder adminEvent;

    private AvatarProvider avatarProvider;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public UsersResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    /**
     * Create a new user
     * <p>
     * Username must be unique.
     *
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(final UserRepresentation rep) {
        // Check the license
       /* try {
            ClientRegistrationPolicyManager.triggerBeforeRegister(session, RegistrationAuth.AUTHENTICATED, "max-users");
        } catch (ClientRegistrationPolicyException e) {
            throw new ErrorResponseException(e.getPolicyModel().getName(), e.getMessage(), Response.Status.BAD_REQUEST);
        }*/
        // first check if user has manage rights
        try {
            auth.users().requireManage();
        } catch (ForbiddenException exception) {
            // if user does not have manage rights, fallback to fine grain admin permissions per group
            if (rep.getGroups() != null) {
                // if groups is part of the user rep, check if admin has manage_members and manage_membership on each group
                for (String groupPath : rep.getGroups()) {
                    GroupModel group = KeycloakModelUtils.findGroupByPath(realm, groupPath);
                    if (group != null) {
                        auth.groups().requireManageMembers(group);
                        auth.groups().requireManageMembership(group);
                    } else {
                        return ErrorResponse.error(String.format("Group %s not found", groupPath), Response.Status.BAD_REQUEST);
                    }
                }
            } else {
                // propagate exception if no group specified
                throw exception;
            }
        }

        if (session.getContext().getRealm().getEmailConfigsStream().count() == 0) {
            if (session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getEmailConfigsStream().count() == 0) {
                return ErrorResponse.error("email-provider-not-configured", Response.Status.BAD_REQUEST);
            }
        }

        if (rep.getEmail() == null) {
            return ErrorResponse.error("email-is-manditory", Response.Status.BAD_REQUEST);
        }

        if (!GeneralHelper.isValidEmail(rep.getEmail())) {
            return ErrorResponse.error("invalid-email-error", Response.Status.BAD_REQUEST);
        }


        String username = rep.getUsername();
        if (realm.isRegistrationEmailAsUsername()) {
            username = rep.getEmail();
        }
        if (ObjectUtil.isBlank(username)) {
            return ErrorResponse.error("user-name-is-missing", Response.Status.BAD_REQUEST);
        }

        // Double-check duplicated username and email here due to federation
        if (session.users().getUserByUsername(realm, username) != null) {
            return ErrorResponse.exists("user-exists-with-same-username");
        }
        if (rep.getEmail() != null && !realm.isDuplicateEmailsAllowed()) {
            try {
                if (session.users().getUserByEmail(realm, rep.getEmail()) != null) {
                    return ErrorResponse.exists("user-exists-with-same-email");
                }
            } catch (ModelDuplicateException e) {
                return ErrorResponse.exists("user-exists-with-same-email");
            }
        }

        if(rep.getFirstName() != null && rep.getFirstName().length() >= 40){
            return ErrorResponse.exists("First name limit exceeds");
        }

        if(rep.getLastName() != null && rep.getLastName().length() >= 40){
            return ErrorResponse.exists("Last name limit exceeds");
        }

        if(rep.getMobileNumber() != null && rep.getMobileNumberRegion() != null && !realm.isDuplicateMobNumAllowed()){
            try {
                if (session.users().getUserByMobile((rep.getMobileNumberRegion() + rep.getMobileNumber()),realm) != null) {
                    return ErrorResponse.exists("user-exists-with-same-mobile-number");
                }
            } catch (ModelDuplicateException e) {
                return ErrorResponse.exists("user-exists-with-same-mobile-number");
            }
        }

        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = profileProvider.create(USER_API, rep.toAttributes());

        try {
            Response response = UserResource.validateUserProfile(profile, null, session);
            if (response != null) {
                return response;
            }

            UserModel user = profile.create();

            UserResource.updateUserFromRep(profile, user, rep, session, false);
            RepresentationToModel.createFederatedIdentities(rep, session, realm, user);
            RepresentationToModel.createGroups(rep, realm, user);
            RoleModel realmModel = realm.getRole("user");
            if (!user.hasRole(realmModel)) {
                user.grantRole(realmModel);
            }
            boolean isAdminRequest = false;
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
                isAdminRequest = true;

                user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");
                user.setSingleAttribute(UserModel.USER_INVITE_CLIENT, rep.getInviteClientId());
            }

            RepresentationToModel.createCredentials(rep, session, realm, user, isAdminRequest);

            if (isAdminRequest) {
                adminEvent.operation(OperationType.CREATE).resource(ResourceType.USER).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();
            } else {
                adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();
            }
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.exists("user-exists-with-same-username-or-email");
        } catch (PasswordPolicyNotMetException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.error("password-policy-not-met", Response.Status.BAD_REQUEST);
        } catch (ModelException me) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            logger.warn("could-not-create-user", me);
            return ErrorResponse.error("could-not-create-user", Response.Status.BAD_REQUEST);
        }
        catch (ClientRegistrationPolicyException e) {
            throw new ErrorResponseException(e.getPolicyModel().getName(), e.getDetailMessage(), Response.Status.BAD_REQUEST);
        } catch (Exception me) { // JPA
            logger.warn("Could not create user!", me);
            return ErrorResponse.error(me.getCause().getMessage().toString(), Response.Status.BAD_REQUEST);
        }
    }


    private boolean canCreateGroupMembers(UserRepresentation rep) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            return false;
        }

        List<GroupModel> groups = Optional.ofNullable(rep.getGroups())
                .orElse(Collections.emptyList())
                .stream().map(path -> findGroupByPath(realm, path))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (groups.isEmpty()) {
            return false;
        }

        // if groups is part of the user rep, check if admin has manage_members and manage_membership on each group
        // an exception is thrown in case the current user does not have permissions to manage any of the groups
        for (GroupModel group : groups) {
            auth.groups().requireManageMembers(group);
            auth.groups().requireManageMembership(group);
        }

        return true;
    }

    /**
     * Get representation of the user
     *
     * @param id User id
     * @return
     */
    @Path("{id}")
    public UserResource user(final @PathParam("id") String id) {
        UserModel user = session.users().getUserById(realm, id);
        if (user == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("user-not-found");
            else throw new ForbiddenException();
        }
        UserResource resource = new UserResource(realm, user, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(users);
        return resource;
    }

    /**
     * Get users
     * <p>
     * Returns a stream of users, filtered according to query parameters.
     *
     * @param search              A String contained in username, first or last name, or email
     * @param last                A String contained in lastName, or the complete lastName, if param "exact" is true
     * @param first               A String contained in firstName, or the complete firstName, if param "exact" is true
     * @param email               A String contained in email, or the complete email, if param "exact" is true
     * @param username            A String contained in username, or the complete username, if param "exact" is true
     * @param emailVerified       whether the email has been verified
     * @param idpAlias            The alias of an Identity Provider linked to the user
     * @param idpUserId           The userId at an Identity Provider linked to the user
     * @param firstResult         Pagination offset
     * @param maxResults          Maximum results size (defaults to 100)
     * @param enabled             Boolean representing if user is enabled or not
     * @param briefRepresentation Boolean which defines whether brief representations are returned (default: false)
     * @param exact               Boolean which defines whether the params "last", "first", "email" and "username" must match exactly
     * @param searchQuery         A query to search for custom attributes, in the format 'key1:value2 key2:value2'
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserRepresentation> getUsers(@QueryParam("search") String search,
                                               @QueryParam("lastName") String last,
                                               @QueryParam("firstName") String first,
                                               @QueryParam("email") String email,
                                               @QueryParam("username") String username,
                                               @QueryParam("emailVerified") Boolean emailVerified,
                                               @QueryParam("idpAlias") String idpAlias,
                                               @QueryParam("idpUserId") String idpUserId,
                                               @QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults,
                                               @QueryParam("enabled") Boolean enabled,
                                               @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                               @QueryParam("exact") Boolean exact,
                                               @QueryParam("q") String searchQuery,
                                               @QueryParam("orderBy") String orderBy) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();

        userPermissionEvaluator.requireQuery();

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);

        Stream<UserModel> userModels = Stream.empty();
        if (search != null) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                UserModel userModel =
                        session.users().getUserById(realm, search.substring(SEARCH_ID_PARAMETER.length()).trim());
                if (userModel != null) {
                    userModels = Stream.of(userModel);
                }
            } else {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.SEARCH, search.trim());
                if (enabled != null) {
                    attributes.put(UserModel.ENABLED, enabled.toString());
                }
                if (orderBy != null) {
                    attributes.put(UserModel.ORDER_BY, orderBy);
                }
                return searchForUser(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                        maxResults, false);
            }
        } else if (orderBy != null || last != null || first != null || email != null || username != null || emailVerified != null
                || idpAlias != null || idpUserId != null || enabled != null || exact != null || !searchAttributes.isEmpty()) {
            Map<String, String> attributes = new HashMap<>();
            if (last != null) {
                attributes.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                attributes.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                attributes.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                attributes.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                attributes.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (idpAlias != null) {
                attributes.put(UserModel.IDP_ALIAS, idpAlias);
            }
            if (idpUserId != null) {
                attributes.put(UserModel.IDP_USER_ID, idpUserId);
            }
            if (enabled != null) {
                attributes.put(UserModel.ENABLED, enabled.toString());
            }
            if (exact != null) {
                attributes.put(UserModel.EXACT, exact.toString());
            }
            if (orderBy != null) {
                attributes.put(UserModel.ORDER_BY, orderBy);
            }

            attributes.putAll(searchAttributes);

            return searchForUser(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                    maxResults, true);
        } else {
            return searchForUser(new HashMap<>(), realm, userPermissionEvaluator, briefRepresentation,
                    firstResult, maxResults, false);
        }

        return toRepresentation(realm, userPermissionEvaluator, briefRepresentation, userModels);
    }

    /**
     * Create a new Admin user [ User with Admin Role ]
     * <p>
     * Username must be unique.
     *
     * @param rep
     * @return
     */
    @Path("/add-admin")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAdminUser(final UserRepresentation rep) {

        // first check if user has manage rights
        try {
            auth.users().requireManage();
        } catch (ForbiddenException exception) {
            // if user does not have manage rights, fallback to fine grain admin permissions per group
            if (rep.getGroups() != null) {
                // if groups is part of the user rep, check if admin has manage_members and manage_membership on each group
                for (String groupPath : rep.getGroups()) {
                    GroupModel group = KeycloakModelUtils.findGroupByPath(realm, groupPath);
                    if (group != null) {
                        auth.groups().requireManageMembers(group);
                        auth.groups().requireManageMembership(group);
                    } else {
                        return ErrorResponse.error(String.format("Group %s not found", groupPath), Response.Status.BAD_REQUEST);
                    }
                }
            } else {
                // propagate exception if no group specified
                throw exception;
            }
        }

        if (session.getContext().getRealm().getEmailConfigsStream().count() == 0) {
            if (session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getEmailConfigsStream().count() == 0) {
                return ErrorResponse.error("email-provider-not-configured", Response.Status.BAD_REQUEST);
            }
        }

        if (rep.getEmail() == null) {
            return ErrorResponse.error("email-is-manditory", Response.Status.BAD_REQUEST);
        }

        String username = rep.getEmail();
        if (ObjectUtil.isBlank(username)) {
            return ErrorResponse.error("user-name-is-missing", Response.Status.BAD_REQUEST);
        }

        if(org.keycloak.utils.GeneralHelper.validationForHtmlInjection(username)) {
            adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.ADMIN_USER).representation(rep).error(HTML_INJECTED_IN_FIELDS).success();
            return ErrorResponse.error("invalid-email-address", Response.Status.BAD_REQUEST);
        }

        // Double-check duplicated username and email here due to federation
        if (session.users().getUserByUsername(username, realm) != null) {
            return ErrorResponse.exists("user-exists-with-same-username");
        }
        if (rep.getEmail() != null && !realm.isDuplicateEmailsAllowed()) {
            try {
                if (session.users().getUserByEmail(rep.getEmail(), realm) != null) {
                    return ErrorResponse.exists("user-exists-with-same-email");
                }
            } catch (ModelDuplicateException e) {
                return ErrorResponse.exists("user-exists-with-same-email");
            }
        }
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = profileProvider.create(USER_API, rep.toAttributes());

        try {
            Response response = UserResource.validateUserProfile(profile, null, session);
            if (response != null) {
                return response;
            }

            UserModel user = profile.create();

            //Set RealmAdmin Role for other than master tenants
            if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
                RoleModel role = realm.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT).getRole(ActionTokenConstants.ADMIN_USER_ROLE);
                user.grantRole(role);
                setMfaEnabled(session, realm, role, user);
            } else {
                //getting adminRole from RealmRoles
                RoleModel adminRole = realm.getRole("admin");

                //If adminRole does not exists
                if (adminRole == null) {
                    logger.error("Could not create Admin user,because Realm Does not have the Admin Role");
                    return ErrorResponse.error("could-not-create-admin-user", Response.Status.INTERNAL_SERVER_ERROR);
                }

                //else, we assign the admin role to the user
                user.grantRole(realm.getRole("admin"));
                setMfaEnabled(session, realm, adminRole, user);
            }

            UserResource.updateUserFromRep(profile, user, rep, session, false);
            RepresentationToModel.createFederatedIdentities(rep, session, realm, user);
            RepresentationToModel.createGroups(rep, realm, user);
            if (realm.getUserInviteWay().equalsIgnoreCase("otp")) {
                List<CredentialRepresentation> credentials = new ArrayList<>();
                CredentialRepresentation pass = new CredentialRepresentation();
                pass.setType(PASSWORD);
                pass.setValue(HmacOTP.generateSecret(10));
                if (!realm.getName().equalsIgnoreCase(Config.getAdminRealm()) && realm.getMode() != null && realm.getMode().equalsIgnoreCase(
                        "bo")) {
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
            user.setSingleAttribute(UserModel.USER_INVITE_CLIENT, ADMIN_CONSOLE_CLIENT_ID);
            adminEvent.operation(OperationType.CREATE).resource(ResourceType.ADMIN_USER).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();
            
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().commit();
            }

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
        } catch (ModelDuplicateException e) {

            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.exists("user-exists-with-same-username-or-email");
        } catch (PasswordPolicyNotMetException e) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            return ErrorResponse.error("password-policy-not-met", Response.Status.BAD_REQUEST);
        } catch (ModelException me) {
            if (session.getTransactionManager().isActive()) {
                session.getTransactionManager().setRollbackOnly();
            }
            logger.warn("could-not-create-user", me);
            return ErrorResponse.error("could-not-create-user", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Get Admin users
     * <p>
     * Returns a stream of users, filtered according to query parameters
     *
     * @param search              A String contained in username, first or last name, or email
     * @param last                A String contained in lastName, or the complete lastName, if param "exact" is true
     * @param first               A String contained in firstName, or the complete firstName, if param "exact" is true
     * @param email               A String contained in email, or the complete email, if param "exact" is true
     * @param username            A String contained in username, or the complete username, if param "exact" is true
     * @param emailVerified       whether the email has been verified
     * @param idpAlias            The alias of an Identity Provider linked to the user
     * @param idpUserId           The userId at an Identity Provider linked to the user
     * @param firstResult         Pagination offset
     * @param maxResults          Maximum results size (defaults to 100)
     * @param enabled             Boolean representing if user is enabled or not
     * @param briefRepresentation Boolean which defines whether brief representations are returned (default: false)
     * @param exact               Boolean which defines whether the params "last", "first", "email" and "username" must match exactly
     * @return a non-null {@code Stream} of users
     */
    @Path("/admins")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserRepresentation> getAdminUsers(@QueryParam("search") String search,
                                                    @QueryParam("lastName") String last,
                                                    @QueryParam("firstName") String first,
                                                    @QueryParam("email") String email,
                                                    @QueryParam("username") String username,
                                                    @QueryParam("emailVerified") Boolean emailVerified,
                                                    @QueryParam("idpAlias") String idpAlias,
                                                    @QueryParam("idpUserId") String idpUserId,
                                                    @QueryParam("first") Integer firstResult,
                                                    @QueryParam("max") Integer maxResults,
                                                    @QueryParam("enabled") Boolean enabled,
                                                    @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                                    @QueryParam("exact") Boolean exact,
                                                    @QueryParam("orderBy") String orderBy) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();

        userPermissionEvaluator.requireQuery();

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        Stream<UserModel> userModels = Stream.empty();
        if (search != null) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put(UserModel.SEARCH, search.trim());
            if (enabled != null) {
                attributes.put(UserModel.ENABLED, enabled.toString());
            }
            if (orderBy != null) {
                attributes.put(UserModel.ORDER_BY, orderBy);
            }
            return searchForAdminUser(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                    maxResults, false);
        } else if (orderBy != null || last != null || first != null || email != null || username != null || emailVerified != null
                || idpAlias != null || idpUserId != null || enabled != null || exact != null) {
            Map<String, String> attributes = new HashMap<>();
            if (last != null) {
                attributes.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                attributes.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                attributes.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                attributes.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                attributes.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (idpAlias != null) {
                attributes.put(UserModel.IDP_ALIAS, idpAlias);
            }
            if (idpUserId != null) {
                attributes.put(UserModel.IDP_USER_ID, idpUserId);
            }
            if (enabled != null) {
                attributes.put(UserModel.ENABLED, enabled.toString());
            }
            if (exact != null) {
                attributes.put(UserModel.EXACT, exact.toString());
            }
            if (orderBy != null) {
                attributes.put(UserModel.ORDER_BY, orderBy);
            }
            return searchForAdminUser(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                    maxResults, true);
        } else {
            return searchForAdminUser(new HashMap<>(), realm, userPermissionEvaluator, briefRepresentation,
                    firstResult, maxResults, false);
        }
    }

    @Path("/admins/count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public int getAdminUsersCount(@QueryParam("search") String search,
                                  @QueryParam("lastName") String last,
                                  @QueryParam("firstName") String first,
                                  @QueryParam("email") String email,
                                  @QueryParam("username") String username,
                                  @QueryParam("emailVerified") Boolean emailVerified,
                                  @QueryParam("idpAlias") String idpAlias,
                                  @QueryParam("idpUserId") String idpUserId,
                                  @QueryParam("enabled") Boolean enabled,
                                  @QueryParam("exact") Boolean exact) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();

        userPermissionEvaluator.requireQuery();

        if (search != null) {
            //TODO: Here check the admin user count
            return session.users().getUsersCount(realm, search.trim(), true);
        } else if (last != null || first != null || email != null || username != null || emailVerified != null
                || idpAlias != null || idpUserId != null || enabled != null || exact != null) {
            Map<String, String> attributes = new HashMap<>();
            if (last != null) {
                attributes.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                attributes.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                attributes.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                attributes.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                attributes.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (idpAlias != null) {
                attributes.put(UserModel.IDP_ALIAS, idpAlias);
            }
            if (idpUserId != null) {
                attributes.put(UserModel.IDP_USER_ID, idpUserId);
            }
            if (enabled != null) {
                attributes.put(UserModel.ENABLED, enabled.toString());
            }
            if (exact != null) {
                attributes.put(UserModel.EXACT, exact.toString());
            }
            //TODO: Here check the admin user count
            return session.users().getUsersCount(realm, attributes, true);
        } else {
            return session.users().getUsersCount(realm, false, true);
        }
    }

    /**
     * Returns the number of users that match the given criteria.
     * It can be called in three different ways.
     * 1. Don't specify any criteria and pass {@code null}. The number of all
     * users within that realm will be returned.
     * <p>
     * 2. If {@code search} is specified other criteria such as {@code last} will
     * be ignored even though you set them. The {@code search} string will be
     * matched against the first and last name, the username and the email of a
     * user.
     * <p>
     * 3. If {@code search} is unspecified but any of {@code last}, {@code first},
     * {@code email} or {@code username} those criteria are matched against their
     * respective fields on a user entity. Combined with a logical and.
     *
     * @param search   arbitrary search string for all the fields below
     * @param last     last name filter
     * @param first    first name filter
     * @param email    email filter
     * @param username username filter
     * @param enabled  Boolean representing if user is enabled or not
     * @return the number of users that match the given criteria
     */
    @Path("count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getUsersCount(@QueryParam("search") String search,
                                 @QueryParam("lastName") String last,
                                 @QueryParam("firstName") String first,
                                 @QueryParam("email") String email,
                                 @QueryParam("emailVerified") Boolean emailVerified,
                                 @QueryParam("username") String username,
                                 @QueryParam("enabled") Boolean enabled) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        if (search != null && !search.isEmpty()) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                UserModel userModel = session.users().getUserById(realm, search.substring(SEARCH_ID_PARAMETER.length()).trim());
                return userModel != null && userPermissionEvaluator.canView(userModel) ? 1 : 0;
            } else {
                return session.users().getUsersCount(realm, search.trim(), false);
            }
        } else if (last != null || first != null || email != null || username != null || emailVerified != null || enabled != null) {
            Map<String, String> parameters = new HashMap<>();
            if (last != null) {
                parameters.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                parameters.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                parameters.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                parameters.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                parameters.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (enabled != null) {
                parameters.put(UserModel.ENABLED, enabled.toString());
            }
            return session.users().getUsersCount(realm, parameters, false);
        } else {
            return session.users().getUsersCount(realm, false, false);
        }
    }

    /**
     * Get Admin users
     * <p>
     * Returns a stream of users, filtered according to query parameters
     *
     * @param search              A String contained in username, first or last name, or email
     * @param last                A String contained in lastName, or the complete lastName, if param "exact" is true
     * @param first               A String contained in firstName, or the complete firstName, if param "exact" is true
     * @param email               A String contained in email, or the complete email, if param "exact" is true
     * @param username            A String contained in username, or the complete username, if param "exact" is true
     * @param emailVerified       whether the email has been verified
     * @param idpAlias            The alias of an Identity Provider linked to the user
     * @param idpUserId           The userId at an Identity Provider linked to the user
     * @param firstResult         Pagination offset
     * @param maxResults          Maximum results size (defaults to 100)
     * @param enabled             Boolean representing if user is enabled or not
     * @param briefRepresentation Boolean which defines whether brief representations are returned (default: false)
     * @param exact               Boolean which defines whether the params "last", "first", "email" and "username" must match exactly
     * @return a non-null {@code Stream} of users
     */
    @Path("/all")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserRepresentation> getAllUsers(@QueryParam("search") String search,
                                                    @QueryParam("lastName") String last,
                                                    @QueryParam("firstName") String first,
                                                    @QueryParam("email") String email,
                                                    @QueryParam("username") String username,
                                                    @QueryParam("emailVerified") Boolean emailVerified,
                                                    @QueryParam("idpAlias") String idpAlias,
                                                    @QueryParam("idpUserId") String idpUserId,
                                                    @QueryParam("first") Integer firstResult,
                                                    @QueryParam("max") Integer maxResults,
                                                    @QueryParam("enabled") Boolean enabled,
                                                    @QueryParam("briefRepresentation") Boolean briefRepresentation,
                                                    @QueryParam("exact") Boolean exact) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        Stream<UserModel> userModels = Stream.empty();
        if (search != null) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put(UserModel.SEARCH, search.trim());
            if (enabled != null) {
                attributes.put(UserModel.ENABLED, enabled.toString());
            }
            return searchForAllUsers(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                    maxResults, false);
        } else if (last != null || first != null || email != null || username != null || emailVerified != null
                || idpAlias != null || idpUserId != null || enabled != null || exact != null) {
            Map<String, String> attributes = new HashMap<>();
            if (last != null) {
                attributes.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                attributes.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                attributes.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                attributes.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                attributes.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (idpAlias != null) {
                attributes.put(UserModel.IDP_ALIAS, idpAlias);
            }
            if (idpUserId != null) {
                attributes.put(UserModel.IDP_USER_ID, idpUserId);
            }
            if (enabled != null) {
                attributes.put(UserModel.ENABLED, enabled.toString());
            }
            if (exact != null) {
                attributes.put(UserModel.EXACT, exact.toString());
            }
            return searchForAllUsers(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                    maxResults, true);
        } else {
            return searchForAllUsers(new HashMap<>(), realm, userPermissionEvaluator, briefRepresentation,
                    firstResult, maxResults, false);
        }
    }

    @GET
    @NoCache
    @Path("count/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> getAllUsersCount(@QueryParam("search") String search,
                                                 @QueryParam("lastName") String last,
                                                 @QueryParam("firstName") String first,
                                                 @QueryParam("email") String email,
                                                 @QueryParam("emailVerified") Boolean emailVerified,
                                                 @QueryParam("username") String username,
                                                 @QueryParam("enabled") Boolean enabled) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();


        Integer totalCnt = 0;
        if (search != null || last != null || first != null || email != null || username != null || emailVerified != null || enabled != null) {
            Map<String, String> parameters = new HashMap<>();
            if(search != null){
                parameters.put(UserModel.SEARCH, search);
            }

            if (last != null) {
                parameters.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                parameters.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                parameters.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                parameters.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                parameters.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (enabled != null) {
                parameters.put(UserModel.ENABLED, enabled.toString());
            }
            totalCnt = getAllUsersCountBySearch(realm,parameters);
        } else {
            totalCnt = session.users().getAllUsersCount(realm);
        }

        Map<String, Integer> map = new HashMap<>();
        map.put("count", totalCnt);
        return map;
    }

    /**
     * Get representation of the user
     *
     * @return
     */
    @Path("profile")
    public UserProfileResource userProfile() {
        UserProfileResource resource = new UserProfileResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    private Stream<UserRepresentation> searchForUser(Map<String, String> attributes, RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Integer firstResult, Integer maxResults, Boolean includeServiceAccounts) {
        session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, includeServiceAccounts);

        if (!auth.users().canView()) {
            Set<String> groupModels = auth.groups().getGroupsWithViewPermission();

            if (!groupModels.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupModels);
            }
        }

        Stream<UserModel> userModels = session.users().searchForUserStream(realm, attributes, firstResult, maxResults, false);
        return toRepresentation(realm, usersEvaluator, briefRepresentation, userModels);
    }

    private Stream<UserRepresentation> searchForAdminUser(Map<String, String> attributes, RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Integer firstResult, Integer maxResults, Boolean includeServiceAccounts) {
        firstResult = firstResult < 0 ? 0 : firstResult;
        maxResults = maxResults < 0 ? Constants.DEFAULT_MAX_RESULTS : maxResults;
        session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, includeServiceAccounts);

        if (!auth.users().canView()) {
            Set<String> groupModels = auth.groups().getGroupsWithViewPermission();

            if (!groupModels.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupModels);
            }
        }

        //TODO: here we have to see the options for onlyadmin
        Stream<UserModel> userModels = session.users().searchForUserStream(realm, attributes, firstResult, maxResults, true);
        return toRepresentation(realm, usersEvaluator, briefRepresentation, userModels);
    }

    private int getAllUsersCountBySearch(RealmModel realm, Map<String,String> params){
        return (int) session.users().searchForUsersStream(realm, params, -1, -1).count();
    }

    private Stream<UserRepresentation> searchForAllUsers(Map<String, String> attributes, RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Integer firstResult, Integer maxResults, Boolean includeServiceAccounts) {
        session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, includeServiceAccounts);

        if (!auth.users().canView()) {
            Set<String> groupModels = auth.groups().getGroupsWithViewPermission();

            if (!groupModels.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupModels);
            }
        }

        Stream<UserModel> userModels = session.users().searchForUsersStream(realm, attributes, firstResult, maxResults);
        return toRepresentation(realm, usersEvaluator, briefRepresentation, userModels);
    }

    private Stream<UserRepresentation> toRepresentation(RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Stream<UserModel> userModels) {
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;
        boolean canViewGlobal = usersEvaluator.canView();

        usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);
        return userModels.filter(user -> canViewGlobal || usersEvaluator.canView(user))
                .map(user -> {
                    UserRepresentation userRep = briefRepresentationB
                            ? ModelToRepresentation.toBriefRepresentation(user)
                            : ModelToRepresentation.toRepresentation(session, realm, user);
                    userRep.setAccess(usersEvaluator.getAccess(user));
                    return userRep;
                });
    }

    private void setMfaEnabled(KeycloakSession session, RealmModel realm, RoleModel role, UserModel userModel) {
        if (hasAdminUser(session, realm, role)) {
            userModel.setMfaEnabled(false);
        } else {
            userModel.setMfaEnabled(true);
        }
    }

    private boolean hasAdminUser(KeycloakSession session, RealmModel realm, RoleModel role) {
        return session.users().getRoleMembersStream(realm, role).count() > 1;
    }
}
