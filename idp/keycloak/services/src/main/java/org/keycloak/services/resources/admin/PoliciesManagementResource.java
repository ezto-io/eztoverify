
package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.policy.PolicyError;
import org.keycloak.policy.password.PasswordPolicyConfigException;
import org.keycloak.policy.password.PasswordPolicyProvider;
import org.keycloak.representations.idm.RealmPolicyRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.utils.GeneralHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.authentication.authenticators.utils.Constants.HTML_INJECTED_IN_FIELDS;
import static org.keycloak.authentication.authenticators.utils.Constants.INVALID_POLICY_NAME;

public class PoliciesManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    private RealmPolicyProvider provider;
    private Properties properties;

    protected static final Logger logger = Logger.getLogger(PoliciesManagementResource.class);

    public PoliciesManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent, RealmPolicyProvider provider) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.provider = provider;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
        this.properties = GeneralHelper.getProperties(session, realm, auth.adminAuth().getUser());
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<RealmPolicyRepresentation> getPolicies(@QueryParam("first") Integer firstResult,
                                                         @QueryParam("max") Integer maxResults,
                                                         @QueryParam("search") @DefaultValue("false") boolean search,
                                                         @QueryParam("policyName") String policyName,
                                                         @QueryParam("policytype") String policyType) {
        auth.realm().requireViewRealm();
        Stream<RealmPolicyModel> policyModels;

        if (realm.getMode().equalsIgnoreCase("bo")) {
            policyModels = provider.getCibaPoliciesStream(realm, firstResult, maxResults, policyName);
        } else {
            //search param is true and policyName is the searchString
            if (search) {
                if (policyName != null && policyName.length() > 0) {
                    policyModels = provider.searchPoliciesByPolicyNameStream(policyName, realm, firstResult, maxResults);
                } else {
                    policyModels = provider.getPoliciesStream(realm, firstResult, maxResults);
                }
            } else { //search param is false
                if (policyType != null) {
                    return getRealmPolicyRepresentationStream(policyType);
                }
                policyModels = provider.getPoliciesStream(realm, firstResult, maxResults);
            }
        }

        Stream<RealmPolicyRepresentation> s = policyModels
                .map(c -> {
                    RealmPolicyRepresentation representation = null;
                    representation = ModelToRepresentation.toRepresentation(c);
                    representation.setRealmId(realm.getId());
                    return representation;
                })
                .filter(Objects::nonNull);
        return s;
    }

    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPolicy(RealmPolicyRepresentation rep) {
        auth.realm().requireManageRealm();

        if(rep.getId() != null) {
            RealmPolicyModel policyModel = provider.getPolicyById(realm, rep.getId());
            if (policyModel != null) {
                if(rep.getPolicyType().equalsIgnoreCase("password")){
                    //Do validation before save
                    PolicyError err = validatePasswordPolicy(rep.getPolicyValue());
                    if(err != null){
                        adminEvent.operation(OperationType.UPDATE_FAILED).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).error(err.getMessage()).success();
                        return ErrorResponse.exists(err.getMessage());
                    }
                }
                boolean res = provider.updatePolicy(realm,rep.getId(),rep.getPolicyName(), rep.getPolicyValue(), rep.isRealmPolicy(), rep.getPolicyType(), false);
                if (rep.isRealmPolicy() && policyModel.getPolicyValue() != null){
                    realm.setPasswordPolicy(PasswordPolicy.parse(session, policyModel.getPolicyValue()));
                }
                if (!res) {
                    if(realm.getMode().equals("bo")){
                        adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.RULE).error("rule-update-failed").success();
                        return ErrorResponse.exists("rule-update-failed");
                    } else{
                        adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.POLICY).error("policy-update-failed").success();
                        return ErrorResponse.exists("policy-update-failed");
                    }
                }
                adminEvent.operation(OperationType.UPDATE).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).representation(rep).success();
                return Response.ok(true).build();
            }
        }

        // PolicyName Null/Empty Check
        if (rep.getPolicyName() == null || rep.getPolicyName().isEmpty()) {
            if(realm.getMode().equals("bo")){
                adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.RULE).error("rule-with-empty-name-failed").success();
                return ErrorResponse.error("rule-with-empty-name-failed", Response.Status.BAD_REQUEST);
            } else {
                adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.POLICY).error("policy-with-empty-name-failed").success();
                return ErrorResponse.error("policy-with-empty-name-failed", Response.Status.BAD_REQUEST);
            }
        }
        if(GeneralHelper.validationForHtmlInjection(rep.getPolicyName())){
            adminEvent.operation(OperationType.CREATE_FAILED).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).error(HTML_INJECTED_IN_FIELDS).success();
            return ErrorResponse.error(INVALID_POLICY_NAME, Response.Status.BAD_REQUEST);
        }

        // Existing PolicyName  Check
        if (provider.getPolicyByName(realm,rep.getPolicyName()) != null) {
            adminEvent.operation(OperationType.CREATE_FAILED).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).error(properties.getProperty((realm.getMode().equals("bo") ? "rule" : "policy")  + "-already-exists").replace("{{0}}", rep.getPolicyName())).success();
            return ErrorResponse.exists(properties.getProperty((realm.getMode().equals("bo") ? "rule" : "policy")  + "-already-exists").replace("{{0}}", rep.getPolicyName()));
        }

        PolicyModel createdModel = createPol(rep);
        if (createdModel.getId() == null ||  createdModel.getId().length() == 0 ) {
            adminEvent.operation(OperationType.CREATE_FAILED).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).error((realm.getMode().equals("bo") ? "Rule" :  "Policy") + "creation failed for unknown reason").success();
            return ErrorResponse.exists((realm.getMode().equals("bo") ? "" : "Policy") +  "creation failed");
        }

        rep.setId(createdModel.getId());
        adminEvent.operation(OperationType.CREATE).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).representation(createdModel).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(rep.getId()).build()).entity(rep).build();
    }

    private PolicyModel createPol(RealmPolicyRepresentation rep) {
        return provider.addPolicy(realm, RepresentationToModel.toModel(rep));
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPolicy(final @PathParam("id") String id) {

        RealmPolicyModel policyModel = provider.getPolicyById(realm,id);
        if (policyModel == null) {
            // we do this to make sure somebody can't phish ids
            throw new NotFoundException("Could not find policy");
        }

        RealmPolicyRepresentation representation = null;
        representation = ModelToRepresentation.toRepresentation(policyModel);
        representation.setRealmId(realm.getId());
        return Response.ok(representation).build();
    }

    private Stream<RealmPolicyRepresentation> getRealmPolicyRepresentationStream(String policyType) {
        Stream<RealmPolicyModel> policyModels = provider.getPoliciesByType(realm, policyType);
        Stream<RealmPolicyRepresentation> s = policyModels
                .map(c -> {
                    RealmPolicyRepresentation representation = null;
                    representation = ModelToRepresentation.toRepresentation(c);
                    representation.setRealmId(realm.getId());
                    return representation;
                })
                .filter(Objects::nonNull);

        return s;
    }

    private static boolean checkIfPolicyIsLive(RealmModel realm, String flowId, RealmPolicyModel policyModel, AdminEventBuilder adminEvent) {
        Stream <String> actionIdStream = realm.getClientsStream()
                .map(ClientModel::getAuthenticationFlowBindingOverrides)
                .filter(client -> client.containsKey("action"))
                .map(x-> x.get("action"));

        Stream<String> authStream = realm.getClientsStream()
                .map(ClientModel::getAuthenticationFlowBindingOverrides)
                .filter(client -> client.containsKey("onboarding") || client.containsKey("recovery"))
                .flatMap(client -> Stream.of(client.get("onboarding"), client.get("recovery")))
                .filter(Objects::nonNull);

        List<String> isRuleUsedInApp = realm.getClientsStream().filter(clientModel -> !clientModel.getAttributes().isEmpty() && clientModel.getAttributes().containsKey("gatepass_rule") && clientModel.getAttribute("gatepass_rule").equalsIgnoreCase(flowId))
                .map(x-> x.getName()).collect(Collectors.toList());

        List<String> policyInAuth = authStream.filter(x ->
                realm.getAuthenticationExecutionsStream(x).filter(y ->
                                y.getAuthenticatorConfig() != null && realm.getAuthenticatorConfigById(y.getAuthenticatorConfig()).getConfig().containsKey("passwordPolicy") && realm.getAuthenticatorConfigById(y.getAuthenticatorConfig()).getConfig().get("passwordPolicy").equalsIgnoreCase(policyModel.getPolicyName()))
                        .count() > 0).collect(Collectors.toList());

        List<String> policyInAction = actionIdStream.filter(actionId ->
                        realm.getRequiredActionProvidersStream(actionId)
                                .filter(requiredActionProvider -> requiredActionProvider.getPolicyId() != null
                                        && (requiredActionProvider.getPolicyId().equals(flowId) || (requiredActionProvider.getPasswordPolicy() != null && requiredActionProvider.getPasswordPolicy().equals(flowId)))).count() > 0)
                .collect(Collectors.toList());

        if(!policyInAuth.isEmpty() || !policyInAction.isEmpty() || !isRuleUsedInApp.isEmpty()){
            adminEvent.operation(OperationType.DELETE_FAILED).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).representation(new HashMap<String,String>(){{
                put("PolicyId", flowId);
                if(!policyInAction.isEmpty()) {
                    put(realm.getMode().equalsIgnoreCase("bo") ? "Templates" : "Actions", policyInAction.stream().map(x-> realm.getActionById(x)).map(x->x.getAlias()).collect(Collectors.toList()).toString());
                }
                if(!isRuleUsedInApp.isEmpty()) {
                    put("Applications", isRuleUsedInApp.toString());
                }
            }}).error(realm.getMode().equalsIgnoreCase("bo") ? "Rule deletion failed as it is used in the mentioned applications or in live templates": "Policy deletion failed as it is used in the flows").success();
        }

        return !policyInAuth.isEmpty() || !policyInAction.isEmpty() || !isRuleUsedInApp.isEmpty();
    }

    @Path("{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePolicy(final @PathParam("id") String id) {

        RealmPolicyModel policyModel = provider.getPolicyById(realm,id);
        if (policyModel == null) {
            // we do this to make sure somebody can't phish ids
            if(realm.getMode().equals("bo")){
                adminEvent.operation(OperationType.DELETE_FAILED).resource(ResourceType.RULE).representation(new HashMap<String,String>(){{
                    put("ruleId", id);
                }}).error( "Could not find rule" ).success();
                throw new NotFoundException("Could not find rule");
            } else{
                adminEvent.operation(OperationType.DELETE_FAILED).resource(ResourceType.POLICY).representation(new HashMap<String,String>(){{
                    put("PolicyId", id);
                }}).error( "Could not find policy" ).success();
                throw new NotFoundException("Could not find policy");
            }
        }

        // we check if the policy is set as live in action, if so we restrict from deleting the policy
        if (checkIfPolicyIsLive(realm, id, policyModel, adminEvent)) {
            return ErrorResponse.exists(realm.getMode().equalsIgnoreCase("bo") ? "Rule couldn't be deleted. Check Audit > Admin Logs for further information" : "Policy couldn't be deleted. Check Audit > Admin Logs for further information");
        }

        if (policyModel.isRealmPolicy()){
            adminEvent.operation(OperationType.DELETE_FAILED).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).representation(new HashMap<String,String>(){{
                put("PolicyId", id);
            }}).error( "Failed to delete default realm password policy" ).success();
           return ErrorResponse.error("Failed to delete default realm password policy", Response.Status.BAD_REQUEST);
        }

        boolean result = provider.removePolicy(realm,id);
        if(!result) {
            if(realm.getMode().equals("bo")){
                adminEvent.operation(OperationType.DELETE_FAILED).resource(ResourceType.RULE).representation(policyModel).error( "Cannot remove the rule" ).success();
                return ErrorResponse.exists("Cannot remove the rule");
            } else{
                adminEvent.operation(OperationType.DELETE_FAILED).resource(ResourceType.POLICY).representation(policyModel).error( "Cannot remove the policy" ).success();
                return ErrorResponse.exists("Cannot remove the policy");
            }
        }


        adminEvent.operation(OperationType.DELETE).resource(realm.getMode().equals("bo") ? ResourceType.RULE : ResourceType.POLICY).representation(policyModel).success();
        return Response.ok(true).build();
    }

    // Validate the Password Policy from Representation before storing
    private PolicyError validatePasswordPolicy(String policyVal){
        try{
           PasswordPolicy policy = PasswordPolicy.parse(session, policyVal);

           List<String>  errors = validatePolicies(session,policy.getPolicyConfigs());
           if (errors != null && !errors.isEmpty()) {
                return new PolicyError(String.join("\n", errors));
           }

        } catch (ModelException e){
            return new PolicyError(e.getMessage());
        }
        return null;
    }

    public List<String> validatePolicies(KeycloakSession session,Map<String,Object> config) {
        List<String> validations = new ArrayList<>();

        config.forEach( (key,val) -> {
            PasswordPolicyProvider provider = session.getProvider(PasswordPolicyProvider.class, key);
            if (provider == null) {
                throw new PasswordPolicyConfigException("Password policy not found");
            }

            String error = provider.isValidConfig(val);
            if(error != null && error.length() > 0){
                validations.add(error);
            }

        });

        return validations;
    }

}
