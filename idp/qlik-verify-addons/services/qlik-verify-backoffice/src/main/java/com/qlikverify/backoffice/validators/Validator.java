package com.qlikverify.backoffice.validators;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Validator implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final Logger logger = Logger.getLogger(Validator.class);

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final List<ProviderConfigProperty> validatorConfigProperties = new ArrayList<>();

    private static final String VALIDATION_POLICY_ID = "validation-policy-id";
    private static final String VALIDATOR_CLAIM = "sp@validator_";
    private static final String VALIDATOR_STATUS = "validator.last.execution.result";
    private static final String VALIDATION_CONDITION_PROMPT = "validation-condition-prompt";

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("validator-name");
        property.setLabel("validator-name");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("Validator");
        property.setRequired(true);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(VALIDATION_POLICY_ID);
        property.setLabel(VALIDATION_POLICY_ID);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(VALIDATION_CONDITION_PROMPT);
        property.setLabel(VALIDATION_CONDITION_PROMPT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("validator-supported-types");
        property.setLabel("validator-supported-types");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(ValidatorPolicy.types);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("validator-value-non-supported-condition");
        property.setLabel("validator-value-non-supported-condition");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(ValidatorPolicy.valueNonSupportedConditions);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("validator-string");
        property.setLabel("validator-string");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(ValidatorPolicy.stringConditions);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("validator-number");
        property.setLabel("validator-number");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(ValidatorPolicy.numberConditions);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("validator-dt");
        property.setLabel("validator-dt");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(ValidatorPolicy.dtConditions);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("validator-boolean");
        property.setLabel("validator-boolean");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(ValidatorPolicy.booleanConditions);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("validator-array");
        property.setLabel("validator-array");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(ValidatorPolicy.arrayConditions);
        validatorConfigProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("validator-object");
        property.setLabel("validator-object");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(ValidatorPolicy.objectConditions);
        validatorConfigProperties.add(property);
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session, String actionId, String validatorStepId) {
        logger.debugf("Called {0}", validatorStepId);
        RealmModel realm = session.getContext().getRealm();
        List<RequiredActionProviderModel> models = realm.getRequiredActionProvidersStream(actionId).collect(Collectors.toList());
        if (models.isEmpty())
            return Collections.emptyList();

        // gets relevant action claims by validator action id
        List<RequiredActionProviderModel> relevantModels = getRelevantActionsModels(models, validatorStepId);
        Map<String, Object> allClaimKeys;

        if (relevantModels.isEmpty()) {
            /*
             * Assume the validator as the first action in the flow, which is responsible for fetching claims from the HTTP request.
             * Currently, no operation is needed. The initial if-statement checks if further action is necessary.
             * Any required metadata-claims processing will be handled in the `evaluateFromRequestMetadata()` method.
             */
            allClaimKeys = Collections.emptyMap();
        } else {
            // Retrieve on-demand claim keys and default from factories
            allClaimKeys = relevantModels.stream()
                .flatMap(model -> {
                    Stream<Map.Entry<String, String>> actionClaims = Stream.empty();
                    try {
                        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory()
                                .getProviderFactory(RequiredActionProvider.class, model.getAlias());
                        actionClaims = factory.getActionClaims(session, model).entrySet().stream()
                                .map(entry -> new AbstractMap.SimpleEntry<>(stripPrefix(entry.getKey()), entry.getValue()));
                    } catch (Exception e) {
                        logger.errorv("Exception in getting Default claims key's {0}", e.getCause());
                    }
                    return actionClaims;
                }) // In case of duplicate keys, keep the first value :- (v1, v2) -> v1 and maintain the order
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));
        }
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream()
                .filter(x -> x.getName() != null && x.getName().equals("claim-selector"))
                .findFirst();
        ProviderConfigProperty provider;
        provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);
        provider.setName("claim-selector");
        provider.setRequired(true);
        provider.setDependsOn(allClaimKeys);
        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    public static List<RequiredActionProviderModel> getRelevantActionsModels(List<RequiredActionProviderModel> allActions, String validatorId) {
        Map<String, RequiredActionProviderModel> actionMap = allActions.stream()
                .collect(Collectors.toMap(RequiredActionProviderModel::getId, action -> action));

        return Stream.concat(
                allActions.stream()
                .filter(action -> (action.getDirection() == null || action.getDirection().equals("none")) && !action.isValidator()),
                        Stream.iterate(actionMap.get(validatorId), Objects::nonNull, action -> actionMap.get(action.getParentId()))
                            .skip(1) // Skip the initial validator
                            .filter(action -> !action.isValidator()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Deprecated(since = "8.1.0")
    private List<RequiredActionProviderModel> getRelevantActionClaims(List<RequiredActionProviderModel> models, String validatorStepId) {
        return models.stream()
            .filter(m -> m.getId().equals(validatorStepId))
            .findFirst()
            .map(validator -> {
                if (validator.getDirection() == null || "none".equalsIgnoreCase(validator.getDirection())) {
                    // First validator case
                    return models.stream()
                            .takeWhile(m -> !m.getId().equals(validator.getId()))
                            .filter(m -> !m.isValidator())
                            .sorted(Comparator.comparingInt(RequiredActionProviderModel::getPriority))
                            .collect(Collectors.toList());
                } else {
                    // Subsequent validator case
                    String previousValidatorId = validator.getValidatorStepId();
                    String direction = validator.getDirection();

                    return Stream.iterate(validator,
                                    current -> current != null && !current.getId().equals(previousValidatorId),
                                    current -> models.stream()
                                            .filter(m -> m.getId().equals(current.getParentId()))
                                            .findFirst()
                                            .orElse(null))
                            .skip(1) // Skip the current validator
                            .filter(m -> m != null && !m.isValidator() &&
                                    m.getValidatorStepId().equals(previousValidatorId) &&
                                    (direction == null || m.getDirection().equals(direction)))
                            .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                                Collections.reverse(list);
                                return list;
                            }));
                }
            })
            .orElse(Collections.emptyList());
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        addAction(context);
    }

    private void addAction(RequiredActionContext context) {
        RequiredActionProviderModel model = context.getModel();
        if (context.getUser().getRequiredActionsStream().noneMatch(action -> action.equals(model.getId()))) {
            context.getUser().addRequiredAction(model.getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Map<String, String> config = context.getConfig();

        Map<String, String> validatorAttributes = context.getRealm().getTrx(context.getRequestId()).getAttributes();
        String validatorId = validatorAttributes.get(VALIDATOR_CLAIM + config.get(VALIDATION_POLICY_ID));
        if(validatorId != null){
            context.getAuthenticationSession().setAuthNote(VALIDATOR_STATUS, validatorId);
            context.success();
            return;
        }
        if (context.getPolicyId() != null) {
            boolean result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
            if(!result) {
                logger.error("Pre-condition's failed :: rule failed for validator");
                context.getRealm().addAttribute(context.getRequestId(),VALIDATOR_CLAIM + config.get(VALIDATION_POLICY_ID), "validator_rule_failed");
                context.getAuthenticationSession().setAuthNote(VALIDATOR_STATUS,  "validator_rule_failed");
                context.ignore();
                return;
            }
        }

        if (config.isEmpty()) {
            context.getRealm().addAttribute(context.getRequestId(),VALIDATOR_CLAIM + config.get(VALIDATION_POLICY_ID), "true");
            context.getAuthenticationSession().setAuthNote(VALIDATOR_STATUS,  "true");
            logger.error("Pre-condition's failed :: validation claims is empty nor null for validator, Decision default to false");
            context.ignore();
            return;
        }

        String validationCondition = config.get(VALIDATION_CONDITION_PROMPT);
        String validatorConfigPolicyId = config.get(VALIDATION_POLICY_ID);

        if (Validation.isBlank(validationCondition) && Validation.isBlank(validatorConfigPolicyId)) {
            context.getRealm().addAttribute(context.getRequestId(),VALIDATOR_CLAIM + config.get(VALIDATION_POLICY_ID), "false");
            context.getAuthenticationSession().setAuthNote(VALIDATOR_STATUS, "false");
            logger.error("Pre-condition's failed :: Required config's are missing");
            context.ignore();
            return;
        }

        boolean isValid = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, validatorConfigPolicyId, context.getUser());

        context.getRealm().addAttribute(context.getRequestId(),VALIDATOR_CLAIM + config.get(VALIDATION_POLICY_ID), isValid ? "true" : "false");
        context.getAuthenticationSession().setAuthNote(VALIDATOR_STATUS, isValid ? "true" : "false");
        context.success();
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // No Operation Required
    }

    private String stripPrefix(String key) {
        return key.startsWith("sp@") ? key.substring(3) : key;
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
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
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return validatorConfigProperties;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"bo"};
    }

    @Override
    public boolean isValidator() {
        return true;
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public boolean isNew() { return true; }

    @Override
    public String getId() {
        return "validator";
    }

    @Override
    public String getDisplayText() {
        return "Validator";
    }

    @Override
    public void close() {
        // No Operation Required
    }
    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public boolean isClaimsCustomizable() {
        return false;
    }
}
