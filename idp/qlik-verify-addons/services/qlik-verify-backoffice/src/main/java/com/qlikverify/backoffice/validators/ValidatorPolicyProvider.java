package com.qlikverify.backoffice.validators;

import com.qlikverify.backoffice.helpers.templateConfigParser.ConfigTemplateParser;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.qlikverify.backoffice.validators.ValidatorPolicy.DOES_NOT_EXISTS;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.EXISTS;
import static org.keycloak.models.Constants.CURRENT_ACTION_ID;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

public class ValidatorPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    private static final String ID = "claimValidator";

    private static final Logger logger = Logger.getLogger(ValidatorPolicyProvider.class);

    /**
     * Validates a user based on the provided action policy.
     * <p>
     * The method evaluates the policies associated with the given {@link ActionPolicy}. It uses two strategies:
     * <ul>
     *   <li>{@code ActionPolicy.UNANIMOUS_STRATEGY} - Requires all policies to be valid (AND operation).</li>
     *   <li>{@code ActionPolicy.AFFIRMATIVE_STRATEGY} - Requires at least one policy to be valid (OR operation).</li>
     * </ul>
     * </p>
     *
     * @param session the current Keycloak session
     * @param policy the action policy to evaluate
     * @param user the user being validated
     * @return {@code true} if the validation is successful based on the strategy; {@code false} otherwise
     */
    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        try {
            List<Boolean> decisions = new ArrayList<>();
            policy.getPolicies().forEach(p -> {
                if (!Validation.isBlank(p) && !p.startsWith("decision")) {
                    ValidatorPolicyCollection collection = policy.getPolicyConfig(p);
                    if (collection == null || collection.getPolicyList() == null || collection.getPolicyList().isEmpty()) {
                        decisions.add(false);
                        return;
                    }
                    checkPolicyList(session, collection, decisions);
                }
            });
            return policy.getDecisionStrategy().equals(ActionPolicy.UNANIMOUS_STRATEGY)
                    ? decisions.stream().allMatch(x -> x)
                    : decisions.stream().anyMatch(x -> x);
        } catch (Exception e) {
            logger.error("ValidatorPolicyProvider::validate()::" + e.getMessage());
            return false;
        }
    }

    /**
     * Evaluates a list of policies and adds the results to the decisions list.
     * <p>
     * This method iterates through the provided {@link ValidatorPolicyCollection}, checking if the policies are present in the
     * realm model's transaction value attributes. If a policy is present, it evaluates the policy using its value. Otherwise,
     * it marks the condition as false. The results of these evaluations are added to the {@code decisions} list.
     * </p>
     *
     * @param session the current Keycloak session
     * @param collection the collection of policies to evaluate
     * @param decisions the list to store the results of the policy evaluations
     */
    private static void checkPolicyList(KeycloakSession session, ValidatorPolicyCollection collection, List<Boolean> decisions) {
        collection.getPolicyList().forEach(pol -> {
            RealmModel realmModel = session.getContext().getRealm();
            AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();
            if (authenticationSession == null) {
                logger.error("checkPolicyList() -> Authentication session seems to be invalid");
                return;
            }

            String requestId = authenticationSession.getAuthNote("request");
            if (requestId == null) {
                logger.error("Request ID could not be retrieved");
                throw new ValidatorPolicyException("Request ID could not be retrieved");
            }

            evaluateExistingValue(session, realmModel, pol, requestId, pol.getPropKey(), decisions);
        });
    }

    /**
     *  Evaluates from the realmModel.getTrxValue() data
     */
    private static void evaluateExistingValue(KeycloakSession session, RealmModel realmModel, ValidatorPolicy pol, String requestId, String claimKey, List<Boolean> decisions) {
        String propComparator = pol.getPropComparator();
        boolean isExistsComparator = EXISTS.equalsIgnoreCase(propComparator);
        boolean isDoesNotExistsComparator = DOES_NOT_EXISTS.equalsIgnoreCase(propComparator);
        ActionModel actionById = realmModel.getActionById(session.getContext().getAuthenticationSession().getAuthNote(CURRENT_ACTION_ID));

        ConfigTemplateParser configTemplateParser = new ConfigTemplateParser(actionById.getActionVariables(), realmModel.getTrx(requestId).getAttributes());
            String resultValue = configTemplateParser.parseTemplate(claimKey);
            if(claimKey.equals(resultValue))
                resultValue = configTemplateParser.getValue(claimKey);

        String toCompareValue;

        if (String.valueOf(pol.getPropValue()).startsWith("${") && String.valueOf(pol.getPropValue()).endsWith("}")) {
            toCompareValue = configTemplateParser.parseTemplate(String.valueOf(pol.getPropValue()));
            pol.setPropValue(toCompareValue);
        }

        if (isExistsComparator) {
            decisions.add(!claimKey.contains(resultValue) || !resultValue.isEmpty());
        } else if(isDoesNotExistsComparator) {
            decisions.add(claimKey.contains(resultValue) || resultValue.isEmpty());
        } else {
            decisions.add(pol.evaluate(pol, resultValue));
        }
    }

    /**
     *  Evaluates from the http-request metadata attribute data
     */
    private static void evaluateFromRequestMetadata(RealmModel realmModel, ValidatorPolicy pol, String requestId, List<Boolean> decisions) {
        Map<String, String> requestAttributes = realmModel.getTrx(requestId).getAttributes();
        if (requestAttributes != null) {
            String fromRequestMetadata = requestAttributes.get("hidden_ua_" + BINDING_MESSAGE);
            if (!Validation.isBlank(fromRequestMetadata)) {
                decisions.add(pol.evaluateTypesFromRequestMetadata(pol, fromRequestMetadata));
            } else {
                switch (pol.getPropComparator()) {
                    case EXISTS:
                    case "is_equal_to":
                    case "is_not_equal_to":
                        decisions.add(false);
                        break;
                    case DOES_NOT_EXISTS:
                        decisions.add(true);
                        break;
                }
            }
        }
    }

    /**
     * Parses a configuration string to create a {@link ValidatorPolicyCollection}.
     * <p>
     * The method splits the provided string by the delimiter " and " to parse individual policies. Each policy is converted into a
     * {@link ValidatorPolicy} object. If a policy has the key "decision", it sets the decision strategy in the
     * {@link ValidatorPolicyCollection}. The method then collects these policies and sets them in the collection.
     * </p>
     *
     * @param value the configuration string to parse, containing policies separated by " and "
     * @return a {@link ValidatorPolicyCollection} containing the parsed policies and the decision strategy
     */
    @Override
    public Object parseConfig(String value) {
        ValidatorPolicyCollection collection = new ValidatorPolicyCollection();
        List<ValidatorPolicy> policies = new ArrayList<>();

        if (value != null && !value.trim().isEmpty()) {
            for (String policy : value.split(" and ")) {
                ValidatorPolicy validatorPolicy = ValidatorPolicy.parseValidatorPolicy(policy);
                if (validatorPolicy.getPropKey().equalsIgnoreCase("decision")) {
                    collection.setDecision(validatorPolicy.getPropValue() == null ? "Unanimous" : String.valueOf(validatorPolicy.getPropValue()));
                }
                policies.add(validatorPolicy);
            }
        }

        collection.setPolicyList(policies);
        return collection;
    }

    @Override
    public ActionPolicyProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // No Operation Required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No Operation Required
    }

    @Override
    public void close() {
        // No Operation Required
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "validator-policy-provider";
    }

    @Override
    public String getConfigType() {
        return null;
    }

    @Override
    public String getDefaultConfigValue() {
        return null;
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }
}
