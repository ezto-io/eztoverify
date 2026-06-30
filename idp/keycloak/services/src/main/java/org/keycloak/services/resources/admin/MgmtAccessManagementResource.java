package org.keycloak.services.resources.admin;

import com.qlikverify.models.ManagementKeysPermissions;
import com.qlikverify.requests.UpdateAccessKeyRequest;
import com.qlikverify.responses.PagedResponse;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.MgmtAccessModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.MgmtAccessRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.models.utils.KeycloakModelUtils.generateRandomId;

public class MgmtAccessManagementResource {
    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(MgmtAccessManagementResource.class);

    private static final String INVALID_KEY="Invalid access key ID";

    public MgmtAccessManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.MGMT_ACCESS);
    }

    @GET
    @NoCache
    public PagedResponse<MgmtAccessRepresentation> getKeys(@QueryParam("first") Integer firstResult,
                                                           @QueryParam("max") Integer maxResults,
                                                            @QueryParam("search") @DefaultValue("") String search) {
        auth.realm().canManageRealm();

        if (!auth.adminAuth().getUser().isAccountCreator())
            throw new ForbiddenException();

        long totalCount = realm.getMgmtAccessKeys().count();

        List<MgmtAccessRepresentation> listOfManagementKeys = realm.getMgmtAccessKeys()
                .map(x -> ModelToRepresentation.toRepresentation(session, realm, x))
                .filter(x -> x.getName().contains(search))
                .sorted(Comparator.comparing(MgmtAccessRepresentation::getLastUpdatedDate).reversed()).collect(Collectors.toList());

        List<MgmtAccessRepresentation> reps = listOfManagementKeys.stream()
                .skip(firstResult)
                .limit(maxResults)
                .collect(Collectors.toList());

        return new PagedResponse<>(reps,  !search.isEmpty() ? listOfManagementKeys.size() : totalCount);
    }

    @GET
    @NoCache
    @Path("{id}")
    public Response getKey(final @PathParam("id") String keyId) throws Exception{
        auth.realm().canManageRealm();

        if (!auth.adminAuth().getUser().isAccountCreator())
            throw new ForbiddenException();

        MgmtAccessModel accessKey = realm.getMgmtAccessById(keyId);
        if (accessKey == null) {
            throw new NotFoundException(INVALID_KEY);
        }

        MgmtAccessRepresentation accessRepresentation = ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), accessKey);
        return Response.status(Response.Status.OK).entity(accessRepresentation).build();
    }

    @Path("generate")
    @POST
    @NoCache
    public Response generate(final MgmtAccessRepresentation representation) throws Exception {
        auth.realm().canManageRealm();

        if (!auth.adminAuth().getUser().isAccountCreator())
            throw new ForbiddenException();

        if (!realm.getId().equals(auth.adminAuth().getRealm().getId()))
            throw new ForbiddenException("Not Authorized to generate Management keys. Only specific workspace owners are allowed");

        Optional<MgmtAccessModel> conflictInAccessKeyName = realm.getMgmtAccessKeys().filter(x -> x.getName().equalsIgnoreCase(representation.getName())).findAny();
        if (conflictInAccessKeyName.isPresent()) {
            throw new BadRequestException("Key with the same name already exists");
        }

        if (representation.getName() == null || representation.getName().isEmpty()) {
            throw new BadRequestException("Invalid access key name");
        }

        if (representation.getName().length() < 3) {
            throw new BadRequestException("The length of the key name must be a minimum of 3 characters");
        }

        MgmtAccessModel accessModel = new MgmtAccessModel();
        accessModel.setName(representation.getName());
        Map<String, Boolean> permissions = new HashMap<>();
        Arrays.stream(ManagementKeysPermissions.values()).forEach(value -> permissions.put("Devices@"+value.getName(), false));
        accessModel.setPermissions(permissions);
        accessModel.setRealmId(realm.getId());
        accessModel.setRequestedBy(auth.adminAuth().getUser().getId());
        accessModel.setSandboxMode(realm.isSandboxMode());
        accessModel = realm.addMgmtAccess(accessModel);

        MgmtAccessRepresentation accessRepresentation = ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), accessModel);
        MgmtAccessRepresentation clone = (MgmtAccessRepresentation) accessRepresentation.clone();
        clone.setSecret(null);
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.MGMT_ACCESS).representation(clone).authUser(auth.adminAuth().getUser())
                .authClient(auth.adminAuth().getClient())
                .authRealm(auth.adminAuth().getRealm()).success();
        return Response.status(Response.Status.CREATED).entity(accessRepresentation).build();

    }


    @Path("rotate")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response rotateKey(@QueryParam("id") String keyId) throws Exception {
        auth.realm().canManageRealm();

        if (!auth.adminAuth().getUser().isAccountCreator())
            throw new ForbiddenException();

        if (!realm.getId().equals(auth.adminAuth().getRealm().getId()))
            throw new ForbiddenException("Not Authorized to rotate Management keys. Only specific workspace owners are allowed");

        MgmtAccessModel model = realm.getMgmtAccessById(keyId);
        if (model == null)
            throw new NotFoundException("No access key available");

        model = realm.rotateMgmtAccess(model);

        MgmtAccessRepresentation accessRepresentation = ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), model);
        MgmtAccessRepresentation clone = (MgmtAccessRepresentation) accessRepresentation.clone();
        clone.setSecret(null);

        adminEvent.operation(OperationType.ROTATE).resource(ResourceType.MGMT_ACCESS).representation(clone).authUser(auth.adminAuth().getUser())
                .authClient(auth.adminAuth().getClient())
                .authRealm(auth.adminAuth().getRealm()).success();
        return Response.status(Response.Status.CREATED).entity(accessRepresentation).build();
    }


    @PUT
    @NoCache
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccess(@PathParam("id") String keyId, @Valid UpdateAccessKeyRequest representation) throws Exception {
        auth.realm().canManageRealm();

        if (!auth.adminAuth().getUser().isAccountCreator())
            throw new ForbiddenException();

        if (!realm.getId().equals(auth.adminAuth().getRealm().getId()))
            throw new ForbiddenException("Not Authorized to rotate Management keys. Only specific workspace owners are allowed");

        MgmtAccessModel accessKey = realm.getMgmtAccessById(keyId);
        if (accessKey == null) {
            throw new NotFoundException(INVALID_KEY);
        }

        if(accessKey.getSandboxSecret() == null){
            String clientSecret =  generateRandomId(64);
            UserCredentialModel userSecret = UserCredentialModel.generateSecret(clientSecret);
            accessKey.setSandboxSecret(userSecret.getChallengeResponse());
        }

        Optional<MgmtAccessModel> conflictInAccessKeyName = realm.getMgmtAccessKeys()
                .filter(key -> !Objects.equals(key.getId(), accessKey.getId()))
                .filter(key -> key.getName().equalsIgnoreCase(representation.getName()))
                .findAny();

        if (conflictInAccessKeyName.isPresent()) {
            throw new BadRequestException("Key with the same name already exists");
        }

        if (representation.getName() == null || representation.getName().isEmpty()) {
            throw new BadRequestException("Invalid access key name");
        }

        if (representation.getName().length() < 2) {
            throw new BadRequestException("The length of the key name must be a minimum of 3 characters");
        }

        accessKey.setName(representation.getName());
        accessKey.setEnabled(representation.isEnabled());
        accessKey.setPermissions(representation.getPermissions());
        accessKey.setExpiryDate(representation.getExpiryDate());
        accessKey.setSandboxMode(representation.isSandboxMode());


        realm.updateMgmtAccess(accessKey);

        MgmtAccessRepresentation accessRepresentation = ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), accessKey);
        MgmtAccessRepresentation clone = (MgmtAccessRepresentation) accessRepresentation.clone();
        clone.setSecret(null);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.MGMT_ACCESS).representation(clone).authUser(auth.adminAuth().getUser())
                .authClient(auth.adminAuth().getClient())
                .authRealm(auth.adminAuth().getRealm()).success();
        return Response.status(Response.Status.OK).entity(accessRepresentation).build();
    }

    @DELETE
    @NoCache
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeAccessKey(final @PathParam("id") String keyId) throws NotFoundException,ForbiddenException, CloneNotSupportedException {
        auth.realm().canManageRealm();

        if (!auth.adminAuth().getUser().isAccountCreator())
            throw new ForbiddenException();

        if (!realm.getId().equals(auth.adminAuth().getRealm().getId()))
            throw new ForbiddenException("Not Authorized to generate Management keys. Only specific workspace owners are allowed");

        MgmtAccessModel accessKey = realm.getMgmtAccessById(keyId);
        if (accessKey == null) {
            throw new NotFoundException(INVALID_KEY);
        }

        MgmtAccessRepresentation accessRepresentation = ModelToRepresentation.toRepresentation(session, session.getContext().getRealm(), accessKey);
        MgmtAccessRepresentation clone = (MgmtAccessRepresentation) accessRepresentation.clone();
        clone.setSecret(null);

        realm.removeMgmtAccess(keyId);



        adminEvent.operation(OperationType.DELETE).resource(ResourceType.MGMT_ACCESS).representation(clone).authUser(auth.adminAuth().getUser())
                .authClient(auth.adminAuth().getClient())
                .authRealm(auth.adminAuth().getRealm()).success();

        return Response.status(Response.Status.OK).entity(Map.of("message", "Access key deleted successfully")).build();
    }
}
