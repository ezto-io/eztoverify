
package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.branding.LoginBrandingProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.BrandingRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.utils.GeneralHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.authentication.authenticators.utils.Constants.HTML_INJECTED_IN_FIELDS;
import static org.keycloak.authentication.authenticators.utils.Constants.INVALID_BRAND_NAME;

public class BrandingManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    private LoginBrandingProvider provider;
    private Properties properties;

    protected static final Logger logger = Logger.getLogger(BrandingManagementResource.class);

    public BrandingManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent, LoginBrandingProvider provider) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.provider = provider;
        this.adminEvent = adminEvent.resource(ResourceType.BRANDING);
        this.properties = GeneralHelper.getProperties(session, realm, auth.adminAuth().getUser());
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<BrandingRepresentation> getBrandings(@QueryParam("first") Integer firstResult,
                                                       @QueryParam("max") Integer maxResults,
                                                       @QueryParam("search") @DefaultValue("false") boolean search,
                                                       @QueryParam("brandName") String brandName) {
        auth.realm().requireViewRealm();
        Stream<BrandingRepresentation> s = provider.getAllBrands(realm, firstResult, maxResults)
                .map(c -> {
                    BrandingRepresentation representation = null;
                    representation = ModelToRepresentation.toRepresentation(c);
                    return representation;
                })
                .filter(Objects::nonNull);
        return s;
    }

    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBrand(BrandingRepresentation rep) {
        auth.realm().requireManageRealm();

        // Brand Name Null/Empty Check/Invalid check
        if (rep.getName() == null || rep.getName().isEmpty()) {
            return ErrorResponse.error("branding-with-empty-brand-name", Response.Status.BAD_REQUEST);
        }
        if(GeneralHelper.validationForHtmlInjection(rep.getName())) {
            adminEvent.operation(OperationType.CREATE_FAILED).representation(rep).error(HTML_INJECTED_IN_FIELDS).success();
            return ErrorResponse.error(INVALID_BRAND_NAME, Response.Status.BAD_REQUEST);
        }

        try {
            if (rep.getId() != null) {
                BrandingModel brandingModel = provider.getBrandById(realm, rep.getId());
                if (brandingModel != null) {
                    brandingModel = provider.updateBranding(realm, rep.getId(), rep.getAttributes());
                    adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
                    if (brandingModel == null) return ErrorResponse.exists("branding-update-failed");
                    return Response.ok(true).build();
                }
            }

            if (provider.getBrandByName(realm, rep.getName().toUpperCase()) != null || provider.getBrandByName(realm, rep.getName().toLowerCase()) != null) {
                return ErrorResponse.exists(properties.getProperty("branding-already-exists").replace("{{0}}", rep.getName()));
            }

            if (rep.getAttributes() == null) {
                rep.setAttributes(new HashMap<>());
            }

            if (rep.getAttributes().isEmpty()) {
                rep.setAttributes(RealmManager.getDefaultTheme(session, realm));
            }

            BrandingModel createdModel = provider.createBranding(realm, rep.getName(), rep.isRealmBrand(), rep.getAttributes());
            if (createdModel.getId() == null || createdModel.getId().length() == 0)
                return ErrorResponse.exists("branding-creation-failed");

            rep.setId(createdModel.getId());
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(rep).success();
            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(rep.getId()).build()).entity(rep).build();
        } catch (ModelException e) {
            logger.error("ERROR WHILE CREATING BRANDING :: createBrand :: " + e.getMessage());
            adminEvent.operation(OperationType.CREATE_FAILED).resourcePath(session.getContext().getUri()).representation(rep).error("branding-name-exceeds-limit").success();
            return ErrorResponse.error("create-branding-failed", Response.Status.BAD_REQUEST);
        }
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBranding(final @PathParam("id") String id) {

        auth.realm().requireManageRealm();
        BrandingModel brandingModel = provider.getBrandById(realm, id);
        if (brandingModel == null) {
            // we do this to make sure somebody can't phish ids
            throw new NotFoundException("Could not find brand");
        }

        BrandingRepresentation representation = null;
        representation = ModelToRepresentation.toRepresentation(session, realm, brandingModel);
        representation.setRealmId(realm.getId());
        return Response.ok(representation).build();
    }

    @Path("default")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefaultBranding() {
        auth.realm().requireManageRealm();
        Map<String, String> brandingAttrs = RealmManager.getDefaultTheme(session, realm);
        BrandingRepresentation representation = new BrandingRepresentation();
        representation.setRealmId(realm.getId());
        representation.setName("reset-default");
        representation.setAttributes(brandingAttrs);
        return Response.ok(representation).build();
    }

    @Path("config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBrandingConfig() {

        auth.realm().requireManageRealm();
        List<ConfigPropertyRepresentation> rep = new ArrayList<>();
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(provider.getBrandingConfig(realm)).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = ModelToRepresentation.toRepresentation(prop);
            rep.add(propRep);
        }
        return Response.ok(rep).build();
    }

    @Path("all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<BrandingRepresentation> getBrandings() {

        auth.realm().requireManageRealm();
        return provider.getAllBrands(realm, null, null)
                .map(c -> {
                    BrandingRepresentation representation = null;
                    representation = ModelToRepresentation.toRepresentation(c);
                    return representation;
                })
                .filter(Objects::nonNull);
    }

    @Path("{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBrand(final @PathParam("id") String id) {

        auth.realm().requireManageRealm();

        BrandingModel brandingModel = provider.getBrandById(realm, id);
        String brandName = brandingModel.getName();
        if (brandingModel == null) {
            // we do this to make sure somebody can't phish ids
            throw new NotFoundException("Could not find brand");
        }

        if (brandingModel.isRealmBrand()) {
            throw new NotAllowedException("Could not delete realm default brand");
        }

        Optional<ClientModel> brandingInApps = realm.getClientsStream().filter(clientModel -> brandName.equalsIgnoreCase(clientModel.getAttribute("login_theme"))).findFirst();

        if(brandingInApps.isPresent()) {
            adminEvent.operation(OperationType.DELETE_FAILED).resourcePath(session.getContext().getUri()).representation(new HashMap<String,String>(){{
                put("branding name", brandName);
                put("Branding used in applications", realm.getClientsStream().filter(clientModel -> brandName.equalsIgnoreCase(clientModel.getAttribute("login_theme"))).map(clientModel -> clientModel.getName()).collect(Collectors.toList()).toString());
            }}).success();
            return ErrorResponse.error("branding-used-in-app", Response.Status.BAD_REQUEST);
        }

        boolean result = provider.removeBranding(realm, id);
        if (!result)
            return ErrorResponse.exists("cannot-remove-brand");

        //Remove the Brand Data from MINIO
        AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
        if (!avatarProvider.removeBrandData(realm.getName(), brandName)) {
            logger.error("Something went wrong on deleting Brand Data for the realm " + realm.getName() + " from Minio");
        }

        BrandingRepresentation rep = new BrandingRepresentation();
        rep.setId(brandingModel.getId());
        rep.setName(brandingModel.getName());
        rep.setRealmId(realm.getId());
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(rep).success();
        return Response.ok(true).build();
    }

}
