package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.FormPartValue;
import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.backoffice.helpers.templateConfigParser.ConfigTemplateParser;
import org.apache.http.util.TextUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.jboss.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;
import static org.keycloak.models.Constants.CURRENT_ACTION_ID;

public class JSONBasedForm implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final Logger logger = Logger.getLogger(JSONBasedForm.class);
    private static final String LOG_TAG = "DYNAMIC_FORM:: ";
    KeycloakSession session;


    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-json-based-form";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setLabel(DYNAMIC_FORM_TITLE);
        property.setName(DYNAMIC_FORM_TITLE);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("form-title-hlp-text");
        property.setMaxVal(100);
        property.setRequired(true);

        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(DYNAMIC_FORM_JSON_SCRIPT);
        property.setLabel(DYNAMIC_FORM_JSON_SCRIPT);
        property.setType(ProviderConfigProperty.TEXT_TYPE);
        property.setHelpText("json-script-helptext");
        property.setRequired(true);

        configProperties.add(property);

        ProviderConfigProperty jsonFormat = new ProviderConfigProperty();
        jsonFormat.setName("json-format");
        jsonFormat.setLabel("");
        jsonFormat.setType(ProviderConfigProperty.CONFIG_TYPE);

        List<ProviderConfigProperty> subConfigs = new ArrayList<>();
        ProviderConfigProperty sampleJsonFormat = new ProviderConfigProperty();
        sampleJsonFormat.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        sampleJsonFormat.setHelpText("follow-the-specified-format");
        subConfigs.add(sampleJsonFormat);

        sampleJsonFormat = new ProviderConfigProperty();
        sampleJsonFormat.setType(ProviderConfigProperty.NOTIFICATION_CODE);
        sampleJsonFormat.setLabel("json-format");
        sampleJsonFormat.setHelpText("\n[{\n" +
                "    \"name\": \"\",    // required field\n" +
                "    \"label\": \"\",    // required field\n" +
                "    \"type\": \"text/date/option/checkbox/file/radio/button/capture/textarea/phonenumber\",    // required field\n" +
                "    \"options\": \"\",    // required if type is (option, checkbox, radio)\n" +
                "    \"placeHolder\": \"\",    // optional if type is text\n" +
                "    \"helperText\": \"\",    // optional \n" +
                "    \"dependsOn\": [{" + "\"field\": \"\", \"value\": \"\"}],    // optional (field is required, value is optional)\n" +
                "    \"validationTypeForDependsOn\": \"all_true/any_true\",    // optional if dependsOn presents\n" +
                "    \"defaultValue\": \"${attribute_name}\",    // optional if type is text and needs default value\n" +
                "    \"editable\": true/false,    // optional\n" +
//                "    \"page\": \"1/2/3\",    // optional if elements to be placed in different page\n" +
                "    \"buttonColor\": \"\",    // optional if type is button\n" +
                "    \"textColor\": \"\",    // optional if type is button\n" +
                "    \"isRequired\": true/false,    // optional for all types\n" +
                "    \"regex\": \"\",    // optional if type is text\n" +
                "    \"regexError\": \"\",    // optional and applicable only for regex\n" +
                "    \"multiple\": true/false,    // optional if type is (option, file)\n" +
                "    \"onlyAccept\": \"jpeg,jpg,gif,svg,png,pdf,doc,docx,xls,xlsx,txt,mp4,mov,ppt,pptx,mp3\",    // optional if type is (file)\n" +
                "    \"includeMetadata\": true/false    //applicable only for type capture\n" +
                "    \"facingMode\": \"front/back\"    //applicable only for type capture\n" +
                "}]"
        );
        subConfigs.add(sampleJsonFormat);
        jsonFormat.setConfigProperties(subConfigs);

        configProperties.add(jsonFormat);
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
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }

        boolean detailsFromJsonForm = context.getRealm().hasTrxValueAttribute(context.getRequestId(), "details_from_" + context.getConfig().get(DYNAMIC_FORM_TITLE).replaceAll("\\s+", "_").toLowerCase());

        if(detailsFromJsonForm && context.getRealm().getTrxValue(context.getRequestId(), "ik@completed_dynamic_form") != null && !context.getRealm().getTrxValue(context.getRequestId(), "ik@completed_dynamic_form").contains(context.getModel().getId())) {
            detailsFromJsonForm = false;
        }

        boolean hasSkipped = Boolean.parseBoolean(context.getRealm().getTrxValue(context.getRequestId(), context.getConfig().get(DYNAMIC_FORM_TITLE).toLowerCase().replaceAll("\\s+","_") + "_skipped"));

        if (result && !hasSkipped && !detailsFromJsonForm) {
            addAction(context);
        } else {
            context.getUser().removeRequiredAction(context.getModel().getId());
        }
    }

    private void addAction(RequiredActionContext context) {
        RequiredActionProviderModel model = context.getModel();
        if (context.getUser().getRequiredActionsStream().noneMatch(action -> action.equals(model.getId()))) {
            logger.info(LOG_TAG + "dynamic form required action is being added to the flow");
            context.getUser().addRequiredAction(model.getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        showDynamicFormFtl(context, null);
    }

    private void showDynamicFormFtl(RequiredActionContext context, String error) {
        String inputJSON = context.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT);

        ActionModel actionById = context.getRealm().getActionById(session.getContext().getAuthenticationSession().getAuthNote(CURRENT_ACTION_ID));

        ConfigTemplateParser configTemplateParser = new ConfigTemplateParser(actionById.getActionVariables(), context.getRealm().getTrx(context.getRequestId()).getAttributes());
        inputJSON = configTemplateParser.parseTemplate(inputJSON);

        List<Map<String, Object>> jsonAsList = validateJson(context, inputJSON);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
        loginFormsProvider.setAttribute("isMobile", getCibaAuthProvider(context).isMobile());

        if (error != null) {
            loginFormsProvider.addError(new FormMessage(error));
            loginFormsProvider.setAttribute(Constants.ERROR, error);
        }

        if (jsonAsList != null && !jsonAsList.isEmpty()) {
            loginFormsProvider.setAttribute("inputJson", jsonAsList);
            loginFormsProvider.setAttribute("formTitle", context.getConfig().get(DYNAMIC_FORM_TITLE));
            context.challenge(loginFormsProvider.createForm("bo-json-based-form.ftl"));
        } else {
            showView(context, loginFormsProvider, ErrorMappings.CONTACT_ADMIN, BACKOFFICE_ERROR_FTL, null);
        }
    }

    private List<Map<String, Object>> validateJson(RequiredActionContext context, String inputJSON) {

        // Checks whether JSON is given in the config
        if (TextUtils.isBlank(inputJSON) || TextUtils.isEmpty(inputJSON)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_DYNAMIC_FORM_ERROR, "No JSON Script is available");
            logger.error(LOG_TAG + "json validation failed in generating dynamic form ftl");
            return Collections.emptyList();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> dataList;

        try {
            dataList = Arrays.asList(objectMapper.readValue(inputJSON, Map[].class));
        } catch (JsonProcessingException e) {
            logger.error(LOG_TAG + "Error while parsing the given JSON :: " + e.getMessage());
            logger.error(LOG_TAG + "json validation failed in generating dynamic form ftl");
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_DYNAMIC_FORM_ERROR, "Couldn't parse the given JSON.");
            return Collections.emptyList();
        }

        return dataList;
    }

    @Override
    public void processAction(RequiredActionContext context) {
        logger.info(LOG_TAG + "processAction");
        if (context.getUser() == null) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_DYNAMIC_FORM_ERROR, ErrorMappings.USER_NOT_ONBOARDED);
            showView(context, context.form(),"user_not_onboarded", BACKOFFICE_ERROR_FTL, null);
            logger.error(LOG_TAG + "user not available");
            return;
        }

        try {

            MultivaluedMap<String, FormPartValue> multiPartFormParameters = GeneralHelper.getMultiPartFormParameters(context, context.getHttpRequest().getHttpHeaders().getMediaType(), context.getHttpRequest().getHttpHeaders(), context.getHttpRequest().getInputStream());

            if (multiPartFormParameters.containsKey("cancel") && Boolean.parseBoolean(multiPartFormParameters.getFirst("cancel").asString())) {
                handleCancel(context, EventType.EZ_DYNAMIC_FORM_ERROR);
                logger.error(LOG_TAG + "user clicked cancel");
                return;
            }

            if(multiPartFormParameters.containsKey("skipped") && Boolean.parseBoolean(multiPartFormParameters.getFirst("skipped").asString())) {
                String title = context.getConfig().get(DYNAMIC_FORM_TITLE).toLowerCase().replaceAll("\\s+","_");
                context.getRealm().addAttribute(context.getRequestId(), title + "_skipped", "true");
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_DYNAMIC_FORM, "", skipDetails);
                context.success();
                logger.error(LOG_TAG + "user skipped the requiredAction");
                return;
            }
            multiPartFormParameters.remove("skipped");
            Map<String, String> claims = new HashMap<>();

            String requestId = context.getRequestId();
            CibaAuthConfigModel cibaAuthConfigModel = session.getContext().getRealm().getCibaAuthConfigByClientId(session.getContext().getClient().getClientId(), context.getRealm().getId());
            incrementModuleCounter(requestId, context.getRealm(), context.getUser(), DYNAMIC_FORM_VERIFICATION);
            String formTitle = context.getConfig().get(DYNAMIC_FORM_TITLE).toLowerCase().replaceAll("\\s+","_");
            System.out.println(JsonSerialization.writeValueAsPrettyString(multiPartFormParameters));
            multiPartFormParameters.keySet().stream().filter(keys -> !keys.equalsIgnoreCase("cancel") && !keys.equalsIgnoreCase("doSubmit") && !keys.contains("accept_types")).forEach(key -> {
                try {
                    if (!(multiPartFormParameters.getFirst(key).asInputStream().available() > 0) && multiPartFormParameters.getFirst(key).asMediaType() == null && multiPartFormParameters.getFirst(key).asString() != null) {
                        List<String> values = new ArrayList<>();
                        multiPartFormParameters.get(key).stream().filter(x->!x.asString().isEmpty()).forEach(x -> {
                            values.add(x.asString());
                        });
                        if(!values.isEmpty()) {
//                            context.getRealm().addAttribute(requestId, "sp@" + formTitle + "@" +  key, String.join(", ", values));
                            claims.put(formTitle + "@" + key, String.join(", ", values));
//                            context.getRealm().addAttribute(requestId, " " + formTitle + "@" +  key, String.join(", ", values));

                        }
                    } else {
                        List<String> values = new ArrayList<>();
                        multiPartFormParameters.get(key).stream().filter(x-> {
                            try {
                                return x.asInputStream().available() > 0;
                            } catch (IOException e) {
                                logger.error(LOG_TAG + "JSONBasedForm :: Error while creating temp file :: " + e.getMessage());
                                return false;
                            }
                        }).forEach(x -> {
                            File mediaTempFile = null;
                            try {
                                if(!checkForFileExtension(GeneralHelper.getFileExtension(x.asMediaType()), multiPartFormParameters, key)) {
                                    throw new IllegalArgumentException("invalid-file-format");
                                }
                                mediaTempFile = GeneralHelper.createMediaTempFile(x.asInputStream(), x.asMediaType());
                            } catch (IOException e) {
                                logger.error(LOG_TAG + "JSONBasedForm :: Error while creating temp file :: " + e.getMessage());
                            }
                            String presignedObjectUrl = GeneralHelper.getPresignedObjectUrl(context, GeneralHelper.putObjectToMinio(mediaTempFile, context, "dynamic-form"));
                            values.add(GeneralHelper.getSecuredUrl(presignedObjectUrl, cibaAuthConfigModel));
                        });
                        if(!values.isEmpty()) {
                            claims.put(key, String.join(", ", values));
//                            context.getRealm().addAttribute(requestId, "sp@" + key, String.join(", ", values));

//                            context.getRealm().addAttribute(requestId, "sp@" + key, String.join(", ", values));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    logger.error(LOG_TAG + e.getMessage());
                    showDynamicFormFtl(context, e.getMessage());
                } catch (IllegalStateException | IOException e) {
                    logger.error(LOG_TAG + "JSONBasedForm :: Error while parsing key :: " + key + " :: " + e.getMessage());
                }
            });
            claims.put("details_from_" + context.getConfig().get(DYNAMIC_FORM_TITLE).toLowerCase().replaceAll("\\s+", "_").toLowerCase(), "true" );
//            context.getRealm().addAttribute(requestId, "sp@details_from_" + context.getConfig().get(DYNAMIC_FORM_TITLE).toLowerCase().replaceAll("\\s+", "_").toLowerCase(), "true");
//            context.getRealm().addAttribute(requestId, "sp@details_from_" + context.getConfig().get(DYNAMIC_FORM_TITLE).toLowerCase().replaceAll("\\s+", "_").toLowerCase(), "true");
            if(context.getRealm().getTrxValue(requestId, "ik@completed_dynamic_form") != null) {
//                context.getRealm().addAttribute(requestId, "sp@completed_dynamic_form", context.getRealm().getTrxValue(requestId, "sp@completed_dynamic_form") + "," + context.getModel().getId());
                claims.put("ik@completed_dynamic_form", context.getRealm().getTrxValue(requestId, "ik@completed_dynamic_form") + "," + context.getModel().getId());
//                context.getRealm().addAttribute(requestId, "sp@completed_dynamic_form", context.getRealm().getTrxValue(requestId, "sp@completed_dynamic_form") + "," + context.getModel().getId());
            } else{
                claims.put("ik@completed_dynamic_form", context.getModel().getId());
//                context.getRealm().addAttribute(requestId, "sp@completed_dynamic_form", context.getModel().getId());
//                context.getRealm().addAttribute(requestId, "sp@completed_dynamic_form", context.getModel().getId());
            }

            context.getRealm().addTrxAttributes(context.getRequestId(), claims);

            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_DYNAMIC_FORM, "");
            logger.info(LOG_TAG + "requiredAction completed");
            context.success();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(LOG_TAG + "Exception Occureed :: " + e.getMessage());
        }
    }

    private static boolean checkForFileExtension(String mediaType, MultivaluedMap<String, FormPartValue> multiPartFormParameters, String key) {
        FormPartValue first = multiPartFormParameters.getFirst(key + "_accept_types");
        return first.asString().contains(mediaType);
    }


    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return "bo-json-based-form";
    }

    @Override
    public void onUpdate(RequiredActionProviderRepresentation rep) {
        validateConfigForJsonForm(rep);
    }

    private static void validateConfigForJsonForm(RequiredActionProviderRepresentation rep) {
        try {
            String formTitle = rep.getConfig().get(DYNAMIC_FORM_TITLE);
            String regex = "[^a-zA-Z0-9 ]";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(formTitle);

            if(matcher.find()) {
                logger.error(LOG_TAG + "json validation failed - Enter valid title without special characters");
                throw new IllegalArgumentException("Enter valid title without special characters");
            }

            if(rep.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT) != null && !rep.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT).isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.readValue(rep.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT), Map[].class);


                InputStream inputStream = JSONBasedForm.class.getResourceAsStream("/META-INF/resources/jsonSchemaForDynamicForm.json");
                if (inputStream == null) {
                    logger.error(LOG_TAG + "json validation failed - Schema file not found!");
                    throw new IllegalArgumentException("Schema file not found!");
                }

                JSONObject jsonSchema = new JSONObject(new JSONTokener(inputStream));
                JSONArray jsonArray = new JSONArray(new JSONTokener(rep.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT)));

                Schema schema = SchemaLoader.load(jsonSchema);
                schema.validate(jsonArray);
                checkForUniqueLabel(jsonArray);
                logger.info(LOG_TAG + "JSONBasedForm :: validateConfigForJsonForm :: JSON array is valid.");
            }

        } catch (ValidationException e) {
            if(checkForConfigTemplateParserFormat(rep.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT))) {
                logger.error(LOG_TAG + "JSONBasedForm :: validateConfigForJsonForm :: JSON array is invalid ::  ValidationException");
                Set<String> errors = new HashSet<>();
                printValidationException(e, errors);
                throw new IllegalArgumentException(String.valueOf(errors));
            }
        } catch (JsonProcessingException e) {
            if(checkForConfigTemplateParserFormat(rep.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT))) {
                logger.error(LOG_TAG + "JSONBasedForm :: validateConfigForJsonForm :: Error while parsing the given JSON :: JsonProcessingException :: " + e.getMessage());
                throw new IllegalArgumentException("Error while processing the JSON");
            }
        } catch (IllegalArgumentException e) {
            if(checkForConfigTemplateParserFormat(rep.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT))) {
                logger.error(LOG_TAG + "JSONBasedForm :: validateConfigForJsonForm  :: " + e.getMessage());
                throw new IllegalArgumentException(e.getMessage());
            }
        } catch (Exception e) {
            if(checkForConfigTemplateParserFormat(rep.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT))) {
                e.printStackTrace();
                logger.error(LOG_TAG + "Error while processing the JSON");
                throw new IllegalArgumentException("Error while processing the JSON");
            }
        }
    }

    private static boolean checkForConfigTemplateParserFormat(String template) {
        ConfigTemplateParser configTemplateParser = new ConfigTemplateParser();
        return configTemplateParser.parseTemplate(template).equals(template);
    }

    private static void checkForUniqueLabel(JSONArray jsonArray) {
        Set<String> labels = new HashSet<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                String name = jsonArray.getJSONObject(i).getString("name");
                if (!labels.add(name)) {
                    throw new IllegalArgumentException("Duplicate name found: " + name);
                }
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException("Error while parsing the JSON");
        }
    }

    private static void printValidationException(ValidationException e, Set<String> errors) {
        if (e.getCausingExceptions().isEmpty()) {
            String errorMessage = "Error at: " + e.getPointerToViolation() + " - " + e.getErrorMessage();
            errors.add(errorMessage);
            logger.error(LOG_TAG + "JSON Schema Validation :: Error :: " + errorMessage);
        } else {
            for (ValidationException ve : e.getCausingExceptions()) {
                printValidationException(ve, errors);
            }
        }
    }

    public static void showView(RequiredActionContext context, LoginFormsProvider formsProvider,String error, String ftl, Map<String, Object> attributes) {
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        if (attributes != null) {
            attributes.forEach(formsProvider::setAttribute);
        }
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{DYNAMIC_FORM_VERIFICATION};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = new LinkedHashMap<>();
        String jsonScript = model.getConfig().get(DYNAMIC_FORM_JSON_SCRIPT);
        String dynamicFormTitle = model.getConfig().get(DYNAMIC_FORM_TITLE).toLowerCase().replaceAll("\\s+","_");
        if (jsonScript == null || jsonScript.isEmpty()) {
            return claims;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> jsonConfig = mapper.readValue(jsonScript, new TypeReference<>(){});

            LinkedHashMap<String, String> finalClaims = jsonConfig.stream().filter(field -> !"file".equals(field.get("type")))
                    .collect(Collectors.toMap(
                            field -> dynamicFormTitle + "@" + ((String) field.get("name")).replaceAll("\\s+", "_"),
                            field ->
                            {
                                String type = (String) field.get("type");
                                Boolean multiple = (Boolean) field.get("multiple");
                                switch (type) {
                                    case "date":
                                        return ValidationTypes.DATE_AND_TIME.getValue();
                                    case "option":
                                    case "radio":
                                        return (multiple != null && multiple)
                                                ? ValidationTypes.ARRAY.getValue()
                                                : ValidationTypes.STRING.getValue();
                                    case "checkbox":
                                        return ValidationTypes.ARRAY.getValue();
                                    default:
                                        return ValidationTypes.STRING.getValue();
                                }
                            }, (v1, v2) -> v1, LinkedHashMap::new
                    ));

            finalClaims.put(dynamicFormTitle + "_skipped", ValidationTypes.BOOLEAN.getValue());
            finalClaims.put("details_from_" + dynamicFormTitle, ValidationTypes.BOOLEAN.getValue());
            return finalClaims;
        } catch (Exception e) {
            logger.errorv(LOG_TAG + "Error parsing JSON configuration: {0}", e.getMessage());
        }
        return claims;
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId =
                context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }
}
