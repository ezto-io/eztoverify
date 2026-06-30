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

import com.openshift.restclient.authorization.UnauthorizedException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.ws.rs.*;

import org.keycloak.Config;
import org.keycloak.common.ClientConnection;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.component.SubComponentFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.MgmtPermissions;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @resource Component
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ComponentResource {
    protected static final Logger logger = Logger.getLogger(ComponentResource.class);

    protected RealmModel realm;

    private AdminPermissionEvaluator auth;

    private AdminEventBuilder adminEvent;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public ComponentResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.COMPONENT);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<ComponentRepresentation> getComponents(@QueryParam("parent") String parent,
                                                       @QueryParam("type") String type,
                                                       @QueryParam("name") String name) {
        auth.realm().requireViewRealm();
        Stream<ComponentModel> components;
        if (parent == null && type == null) {
            components = realm.getComponentsStream();

        } else if (type == null) {
            components = realm.getComponentsStream(parent);
        } else if (parent == null) {
            components = realm.getComponentsStream(realm.getId(), type);
        } else {
            components = realm.getComponentsStream(parent, type);
        }
        /*AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            throw new ForbiddenException();
        }
        UserModel user= authResult.getUser();
        Boolean isAccountCreator = user.getFirstAttribute("qv_is_account_creator") != null &&  Boolean.parseBoolean(user.getFirstAttribute("qv_is_account_creator"));
*/
        return components
                .filter(component -> Objects.isNull(name) || Objects.equals(component.getName(), name))
                .map(component -> {
                    try {
                        return ModelToRepresentation.toRepresentation(session, component, false);
                    } catch (Exception e) {
                        logger.error("Failed to get component list for component model" + component.getName() + "of realm " + realm.getName());
                        return ModelToRepresentation.toRepresentationWithoutConfig(component);
                    }
                });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(ComponentRepresentation rep) {
        auth.realm().requireManageRealm();
        try {
            ComponentModel model = RepresentationToModel.toModel(session, rep);
            if (model.getParentId() == null) model.setParentId(realm.getId());

            if(realm.getComponentsStream().anyMatch(x-> rep.getName().equals(x.getName()))) {
                throw new BadRequestException("secret-key-already-exists");
            }

            model = realm.addComponentModel(model);

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), model.getId()).representation(StripSecretsUtils.strip(session, rep)).success();
            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
        } catch (ComponentValidationException e) {
            return localizedErrorResponse(e, auth);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ComponentRepresentation getComponent(@PathParam("id") String id) {
        auth.realm().requireViewRealm();
        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();
        UserModel user;
        if (authResult == null) {
            user = auth.adminAuth().getUser();
        } else {
            user = authResult.getUser();
        }
        Boolean isAccountCreator = user.getFirstAttribute("qv_is_account_creator") != null &&  Boolean.parseBoolean(user.getFirstAttribute("qv_is_account_creator"));
        ComponentRepresentation rep = ModelToRepresentation.toRepresentation(session, model, Config.getAdminRealm().equals(auth.adminAuth().getRealm().getName()) || isAccountCreator);
        return rep;
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateComponent(@PathParam("id") String id, ComponentRepresentation rep) {
        auth.realm().requireManageRealm();
        try {
            ComponentModel model = realm.getComponent(id);
            if (model == null) {
                throw new NotFoundException("Could not find component");
            }
            RepresentationToModel.updateComponent(session, rep, model, false, componentInUse(id));
            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(StripSecretsUtils.strip(session, rep)).success();
            realm.updateComponent(model);
            return Response.noContent().build();
        } catch (ComponentValidationException e) {
            return localizedErrorResponse(e, auth);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException();
        }
    }
    @DELETE
    @Path("{id}")
    public void removeComponent(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        if(componentInUse(id)){
            throw new BadRequestException("key-used-message");
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        realm.removeComponent(model);

    }

    private boolean componentInUse(String id){
        // key used in developer section
        List<ClientModel> keyInDevelopers = realm.getClientsStream()
                .filter(clientModel -> clientModel.getAttributes().containsKey("enable_ciba_request_encryption") && clientModel.getAttribute("enable_ciba_request_encryption").equalsIgnoreCase("true"))
                .filter(client -> client.getAttributes().containsKey("ciba_encryption_id") && client.getAttribute("ciba_encryption_id").equalsIgnoreCase(id))
                .collect(Collectors.toList());

        // key used in client authentication way
        List<CibaAuthConfigModel> keyInConfiguration = realm.getClientsStream()
                .map(clientModel -> realm.getCibaAuthConfigByClientId(clientModel.getClientId(), realm.getId()))
                .filter(Objects::nonNull)
                .filter(x -> x.getConfig().containsKey("encryption-enable") && x.getConfig().get("encryption-enable").equalsIgnoreCase("true"))
                .filter(x -> x.getConfig().containsKey("secret-keys") && x.getConfig().get("secret-keys").equalsIgnoreCase(id))
                .collect(Collectors.toList());

        // key used in client notification way
        List<CibaNotificationConfigModel> keyInClientNotification = realm.getClientsStream()
                .map(clientModel -> realm.getCibaNotificationConfigByClientId(clientModel.getClientId(), realm.getId()))
                .filter(Objects::nonNull)
                .filter(x -> x.getConfig().containsKey("notification-response-way") && x.getConfig().get("notification-response-way").equalsIgnoreCase("encrypted"))
                .filter(x -> x.getConfig().containsKey("secret-keys") && x.getConfig().get("secret-keys").equalsIgnoreCase(id))
                .collect(Collectors.toList());

        if(keyInDevelopers.size() > 0 || keyInConfiguration.size() > 0 || keyInClientNotification.size() > 0){
            return true;
        }
        return false;
    }

    private Response localizedErrorResponse(ComponentValidationException cve, AdminPermissionEvaluator auth) {
        String locale = null;
        if(((MgmtPermissions) auth).admin().getAttributes().containsKey("preferred_locale")) {
            locale = ((MgmtPermissions) auth).admin().getFirstAttribute("preferred_locale");
        }
        if(locale == null)
            return ErrorResponse.error("locale not available", Response.Status.BAD_REQUEST);
        Properties messages = AdminRoot.getMessages(session, realm, locale, "admin-messages", "messages");

        Object[] localizedParameters = cve.getParameters()==null ? null : Arrays.asList(cve.getParameters()).stream().map((Object parameter) -> {

            if (parameter instanceof String) {
                String paramStr = (String) parameter;
                return messages.getProperty(paramStr, paramStr);
            } else {
                return parameter;
            }

        }).toArray();

        String message = MessageFormat.format(messages.getProperty(cve.getMessage(), cve.getMessage()), localizedParameters);
        return ErrorResponse.error(message, Response.Status.BAD_REQUEST);
    }

    /**
     * List of subcomponent types that are available to configure for a particular parent component.
     *
     * @param parentId
     * @param subtype
     * @return
     */
    @GET
    @Path("{id}/sub-component-types")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<ComponentTypeRepresentation> getSubcomponentConfig(@PathParam("id") String parentId, @QueryParam("type") String subtype) {
        auth.realm().requireViewRealm();
        ComponentModel parent = realm.getComponent(parentId);
        if (parent == null) {
            throw new NotFoundException("Could not find parent component");
        }
        if (subtype == null) {
            throw new BadRequestException("must specify a subtype");
        }
        Class<? extends Provider> providerClass;
        try {
            providerClass = (Class<? extends Provider>)Class.forName(subtype);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


        return session.getKeycloakSessionFactory().getProviderFactoriesStream(providerClass)
            .filter(ComponentFactory.class::isInstance)
            .map(factory -> toComponentTypeRepresentation(factory, parent));
    }

    private ComponentTypeRepresentation toComponentTypeRepresentation(ProviderFactory factory, ComponentModel parent) {
        ComponentTypeRepresentation rep = new ComponentTypeRepresentation();
        rep.setId(factory.getId());

        ComponentFactory componentFactory = (ComponentFactory)factory;

        rep.setHelpText(componentFactory.getHelpText());
        List<ProviderConfigProperty> props;
        Map<String, Object> metadata;
        if (factory instanceof SubComponentFactory) {
            props = ((SubComponentFactory)factory).getConfigProperties(session,realm, parent);
            metadata = ((SubComponentFactory)factory).getTypeMetadata(realm, parent);

        } else {
            props = componentFactory.getConfigProperties(session);
            metadata = componentFactory.getTypeMetadata();
        }

        List<ConfigPropertyRepresentation> propReps =  ModelToRepresentation.toRepresentation(props);
        rep.setProperties(propReps);
        rep.setMetadata(metadata);
        return rep;
    }
}
