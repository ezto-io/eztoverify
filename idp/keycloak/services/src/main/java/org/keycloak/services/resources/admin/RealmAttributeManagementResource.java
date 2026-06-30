
package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.attributes.RealmAttributeProvider;
import org.keycloak.attributes.RealmAttributeProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GeneralResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.RealmAttributeCollection;
import org.keycloak.representations.RealmAttributeRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

public class RealmAttributeManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;


    protected static final Logger logger = Logger.getLogger(RealmAttributeManagementResource.class);

    public RealmAttributeManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
    }

    @POST
    @Path("update/{tab}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse updateParam(@PathParam("tab") String tabName, Map<String, String> requestBody) {
        List<RealmAttributeProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactoriesStream(RealmAttributeProvider.class).map(x -> (RealmAttributeProviderFactory) x).filter(x -> x.tabName().equals(tabName)).collect(Collectors.toList());
        //RealmAttributeProviderFactory providerFactory = (RealmAttributeProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(RealmAttributeProvider.class, providerId);
        List<Boolean> results = new ArrayList<>();
            factories.forEach(x -> {
                if (x.canUpdate() && !x.isReadOnly()) {
                    RealmAttributeProvider realmAttributeProvider = session.getProvider(RealmAttributeProvider.class, x.getId());
                    try {
                        if(realmAttributeProvider.updateAttribute(session, realm, requestBody.get(x.getAttributeName()))) {
                            results.add(true);
                        }
                    } catch (IllegalArgumentException e) {
                        results.add(false);
                        requestBody.put("error", e.getMessage());
                    }
                }
            });

        boolean isSuccess = results.stream().noneMatch(x -> x == false);
        adminEvent.operation(isSuccess ? OperationType.UPDATE : OperationType.UPDATE_FAILED).resource(ResourceType.WORKSPACE).resourcePath(session.getContext().getUri()).representation(requestBody).success();

        Map<String, String> body = new HashMap<>();
        body.put("message", isSuccess ? "update-succesfully" : "failed-to-update-attribute");
        GeneralResponse response = new GeneralResponse();
        response.setStatus(isSuccess ? 200 : 400);
        response.setBody(body);
        return response;
    }

    @POST
    @Path("rotate/{providerid}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse rotateParam(@PathParam("providerid") String providerId) {
        RealmAttributeProviderFactory providerFactory = (RealmAttributeProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(RealmAttributeProvider.class, providerId);
        RealmAttributeProvider realmAttributeProvider = session.getProvider(RealmAttributeProvider.class, providerId);
        if (!providerFactory.shouldRotate()) {
            Map<String, String> body = new HashMap<>();
            body.put("message", "Request operation cannot be performed");
            GeneralResponse response = new GeneralResponse();
            response.setStatus(400);
            response.setBody(body);
            return response;
        }

        if (providerFactory.isReadOnly()) {
            Map<String, String> body = new HashMap<>();
            body.put("message", "Request operation cannot be performed on read only property");
            GeneralResponse response = new GeneralResponse();
            response.setStatus(400);
            response.setBody(body);
            return response;
        }

        boolean result = realmAttributeProvider.rotateSecret(session, realm);

        Map<String, String> body = new HashMap<>();
        body.put("message", result ? "Rotated Successfully" : "Failed to rotate attribute");
        GeneralResponse response = new GeneralResponse();
        response.setStatus(result ? 200 : 400);
        response.setBody(body);
        return response;
    }

    @POST
    @Path("generate/{providerid}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse generateParam(@PathParam("providerid") String providerId) {
        RealmAttributeProviderFactory providerFactory = (RealmAttributeProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(RealmAttributeProvider.class, providerId);
        RealmAttributeProvider realmAttributeProvider = session.getProvider(RealmAttributeProvider.class, providerId);
        if (!providerFactory.canGenerate(session.getContext().getRealm())) {
            Map<String, String> body = new HashMap<>();
            body.put("message", "Request operation cannot be performed");
            GeneralResponse response = new GeneralResponse();
            response.setStatus(400);
            response.setBody(body);
            return response;
        }

        boolean result = realmAttributeProvider.generateSecret(session, realm);

        Map<String, String> body = new HashMap<>();
        body.put("message", result ? "Generated Successfully" : "Failed to generate attribute");
        GeneralResponse response = new GeneralResponse();
        response.setStatus(result ? 200 : 400);
        response.setBody(body);
        return response;
    }

    @GET
    @Path("available")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse hasAttributes() {
        List<ProviderFactory> attributeProviders = session.getKeycloakSessionFactory().getProviderFactoriesStream(RealmAttributeProvider.class).collect(Collectors.toList());
        GeneralResponse response = new GeneralResponse();
        response.setStatus(attributeProviders.size() > 0 ? 200 : 404);
        return response;
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<RealmAttributeCollection> listAttributes() {
        List<ProviderFactory> attributeProviders = session.getKeycloakSessionFactory().getProviderFactoriesStream(RealmAttributeProvider.class).collect(Collectors.toList());
        if (attributeProviders.size() == 0)
            return null;
        List<RealmAttributeCollection> collections = new ArrayList<>();
        Map<String, List<RealmAttributeRepresentation>> collectionMap = new HashMap<>();
        Map<String, Boolean> undertrial = new HashMap<>();
        Map<String, String> trialLink = new HashMap<>();

        attributeProviders.stream().map(x -> (RealmAttributeProviderFactory) x)
                .filter(x -> {
                    x.create(session);
                    return !x.hidden(auth.adminAuth().getUser());
                })
                .filter(specialAttribute -> Arrays.stream(specialAttribute.supportedRegions()).anyMatch(x -> x.equalsIgnoreCase("all") || x.equalsIgnoreCase(realm.getDefaultRegion())))
                .forEach(x -> {
                    if (!collectionMap.containsKey(x.tabName())) {
                        collectionMap.put(x.tabName(), new ArrayList<>());
                    }
                    if (!undertrial.containsKey(x.tabName())) {
                        undertrial.put(x.tabName(), x.isUnderTrial(session, realm));
                    }

                    if (undertrial.containsKey(x.tabName()) && !trialLink.containsKey(x.tabName())) {
                        trialLink.put(x.tabName(), x.subscribeLink(session, realm));
                    }
                    collectionMap.get(x.tabName()).add(new RealmAttributeRepresentation() {{
                        setProviderId(x.getId());
                        setAttributeName(x.getAttributeName());
                        setLabel(x.getDisplayName());
                        setAttributeValue(x.isSecret() ? "******************" : x.getValue(session, realm));
                        setCanBeRotated(x.shouldRotate());
                        setSecret(x.isSecret());
                        setCanBeUpdated(x.canUpdate());
                        setCanBeViewed(x.canBeViewed());
                        setReadOnly(x.isReadOnly());
                        setCanGenerate(x.canGenerate(realm));
                        setHelpText(x.getHelpText());
                        setJson(x.isJson());
                        setJavascript(x.isScript());
                        setToggle(x.isToggle());
                        setNew(x.isNew());
                        if (realm.getName().equals(org.keycloak.Config.getAdminRealm())) {
                            setShowInSubTenant(true);
                        } else {
                            setShowInSubTenant(x.showInSubTenant(realm));
                        }
                        setPriority(x.priority());
                    }});
                });

        collectionMap.forEach((k, v) -> {
            collections.add(new RealmAttributeCollection() {{
                setTabName(k);
                setAttributes(v.stream().sorted(Comparator.comparing(RealmAttributeRepresentation::getPriority)).collect(Collectors.toList()));
                setSubscribeLink(trialLink.get(k));
                setUnderTrial(undertrial.get(k));
            }});
        });
        return collections;
    }
}
