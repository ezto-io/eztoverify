package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.Errors;
import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.backoffice.helpers.templateConfigParser.ConfigTemplateParser;
import com.qlikverify.extensions.admin.misc.helpers.JsonUtil;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;

import static com.qlikverify.backoffice.Constants.BACKOFFICE_ERROR_FTL;
import static com.qlikverify.backoffice.Constants.ERROR_MESSAGE;
import static com.qlikverify.backoffice.helpers.GeneralHelper.showView;
import static org.keycloak.models.Constants.CURRENT_ACTION_ID;

public class APIRequest implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(APIRequest.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("api-request-name");
        property.setLabel("api-request-name");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setMaxVal(48);
        property.setHelpText("api-request-name-helptext");
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName("api-request-title");
        property.setLabel("api-request-title");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setMaxVal(48);
        property.setHelpText("api-request-title-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("api-request-desc");
        property.setLabel("api-request-desc");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setMaxVal(300);
        property.setHelpText("api-request-desc-helptext");
        configProperties.add(property);

        List<String> httpMethods = new ArrayList<>();
        httpMethods.add("GET");
        httpMethods.add("POST");
        httpMethods.add("PUT");
        httpMethods.add("PATCH");
        httpMethods.add("DELETE");

        property = new ProviderConfigProperty();
        property.setName("api-request-method");
        property.setLabel("api-request-method");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setRequired(true);
        property.setOptions(httpMethods);
        property.setHelpText("api-request-method-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("api-request-url");
        property.setLabel("api-request-url");
        property.setType(ProviderConfigProperty.URL);
        property.setRequired(true);
        property.setHelpText("api-request-url-helptext");
        configProperties.add(property);

        List<String> contentType = new ArrayList<>();
        contentType.add("application/json");
        contentType.add("application/x-www-form-urlencoded");
        contentType.add("application/xml");
        contentType.add("text/javascript");
        contentType.add("text/plain");
        contentType.add("text/html");


        property = new ProviderConfigProperty();
        property.setName("api-request-content-type");
        property.setLabel("api-request-content-type");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setRequired(true);
        property.setRawFormat(true);
        property.setOptions(contentType);
        property.setHelpText("api-request-content-type-helptext");
        configProperties.add(property);

        List<String> body = new ArrayList<>();
        body.add("none");
        body.add("required");

        property = new ProviderConfigProperty();
        property.setName("api-request-body-type");
        property.setLabel("api-request-body-type");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setRequired(true);
        property.setOptions(body);
        property.setHelpText("api-request-body-type-helptext");
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName("api-request-body-form");
        property.setLabel("api-request-body-form");
        property.setType(ProviderConfigProperty.MAP_TYPE);
        property.setDependsOn(new HashMap<>() {{
            put("api-request-content-type", "application/x-www-form-urlencoded");
            put("api-request-body-type", "required");
        }});
        property.setHelpText("api-request-body-form-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("api-request-headers");
        property.setLabel("api-request-headers");
        property.setType(ProviderConfigProperty.MAP_TYPE);
        property.setHelpText("api-request-headers-helptext");
        Set<String> validMethods = new HashSet<>(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE"));
        property.setDependsOn(Collections.singletonMap("api-request-method", validMethods));
        property.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("api-request-body-other");
        property.setLabel("api-request-body-other");
        property.setType(ProviderConfigProperty.TEXT_TYPE);
        property.setDependsOn(new HashMap<>() {{
            put("api-request-content-type", new String[]{"application/json", "application/xml", "text/javascript", "text/plain", "text/html"});
            put("api-request-body-type", "required");
        }});
        property.setHelpText("api-request-body-other-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("api-request-is-auth");
        property.setLabel("api-request-is-auth");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("api-request-is-auth-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("api-request-bearer-token");
        property.setLabel("api-request-bearer-token");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDependsOn(new HashMap<>() {{
            put("api-request-is-auth", true);
        }});
        property.setHelpText("api-request-bearer-token-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("clear-data");
        property.setLabel("clear-data");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("clear-data-hlptext");
        configProperties.add(property);
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return this;
    }

    @Override
    public String getDisplayText() {
        return "bo-api-request";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
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
        String requestId = context.getRequestId();
        String name = context.getConfig().get("api-request-name");

        boolean isOnboardingFlow = com.qlikverify.backoffice.helpers.GeneralHelper.isOnboardingFlow(context);
        if (isOnboardingFlow && context.getAuthenticationSession().getAuthNote(context.getModel().getId()) == null) {
            cleanUpForResume(context);
        }

        boolean requestDone = context.getRealm().hasTrxValueAttribute(requestId, getSanitizedName(name));
        boolean requestDoneAt = context.getRealm().hasTrxValueAttribute(requestId, getSanitizedName(name) + "_completed_at");
        if (result && (!requestDone || !requestDoneAt)) {
            addAction(context);
        } else {
            context.getUser().removeRequiredAction(context.getModel().getId());
        }
    }

    private void cleanUpForResume(RequiredActionContext context) {
        String shouldCleanUp = context.getConfig().getOrDefault("clear-data", "false");
        if (Boolean.parseBoolean(shouldCleanUp) && Validation.isBlank(context.getAuthenticationSession().getAuthNote(context.getModel().getId()))) {
            String apiRequestName = context.getConfig().get("api-request-name");

            if (context.getRealm().hasTrxValueAttribute(context.getRequestId(), getSanitizedName(apiRequestName))) {
                context.getRealm().removeTrxAttribute(context.getRequestId(), getSanitizedName(apiRequestName));
                context.getRealm().removeTrxAttribute(context.getRequestId(), getSanitizedName(apiRequestName) + "_completed_at");
                context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
            }
        }
    }

    private void addAction(RequiredActionContext context) {
        RequiredActionProviderModel model = context.getModel();
        if (context.getUser().getRequiredActionsStream().noneMatch(action -> action.equals(model.getId()))) {
            context.getUser().addRequiredAction(model.getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        LoginFormsProvider loginFormsProvider = context.form();
        if (!context.getConfig().isEmpty()) {
            loginFormsProvider.setAttribute("apiRequestTitle", context.getConfig().get("api-request-title"));
            loginFormsProvider.setAttribute("apiRequestDesc", context.getConfig().get("api-request-desc"));
            loginFormsProvider.setAttribute("isMobile", getCibaAuthProvider(context).isMobile());
            context.challenge(loginFormsProvider.createForm("api-request.ftl"));
        } else {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_API_REQUEST_ERROR, "Config(s) missing");
            Map<String, String> contactAdminErrMsg = new HashMap<>();
            contactAdminErrMsg.put(ERROR_MESSAGE, ErrorMappings.CONTACT_ADMIN);
            showView(context, null, BACKOFFICE_ERROR_FTL, contactAdminErrMsg);
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        String name = context.getConfig().get("api-request-name");
        HttpUtility httpUtility = new HttpUtility();
        boolean isBodyRequired = isBodyRequired(context.getConfig().get("api-request-body-type"));
        String body = "{}";

        if (isBodyRequired) {
            String mediaType = context.getConfig().get("api-request-content-type");
            body = mediaType.equals("application/x-www-form-urlencoded") ? context.getConfig().get("api-request-body-form") : context.getConfig().get("api-request-body-other");
            body = getRequestBody(context, body);
        }

        boolean isAuth = Boolean.parseBoolean(context.getConfig().get("api-request-is-auth"));
        Response response = null;
        try {
            response = httpUtility.callRequest(context.getConfig().get("api-request-content-type"), context.getConfig().get("api-request-method"), getRequestUrl(context), body, isAuth ? getRequestAuth(context) : null, getRequestHeaders(context), 3, false);
            context.getRealm().addAttribute(context.getRequestId(), getSanitizedName(name), response.isSuccessful() ? "success" : "failed");
            context.getRealm().addAttribute(context.getRequestId(), getSanitizedName(name) + "_completed_at", String.valueOf(Time.currentTime()));

            if (!response.isSuccessful()) {
                if (response.code() == 401) {
                    GeneralHelper.setErrorAttribute(context, Errors.EVAPI0002, getId());
                } else if (response.code() == 403) {
                    GeneralHelper.setErrorAttribute(context, Errors.EVAPI0003, getId());
                } else if (response.code() == 429) {
                    GeneralHelper.setErrorAttribute(context, Errors.EVAPI0004, getId());
                } else if (response.code() == 404) {
                    GeneralHelper.setErrorAttribute(context, Errors.EVAPI0005, getId());
                } else if (response.code() == 409) {
                    GeneralHelper.setErrorAttribute(context, Errors.EVAPI0006, getId());
                } else if (response.code() >= 500 && response.code() <= 599) {
                    GeneralHelper.setErrorAttribute(context, Errors.EVAPI0007, getId());
                }
                GeneralHelper.auditEvent(context, EventType.EZ_API_REQUEST_ERROR, getAuditRepresentation(name, response));
                context.failure();
                return;
            }

            if (response.body() != null) {
                String res = response.body().string();
                if (!Validation.isBlank(res)) {
                    addResponse(context, name, res);
                }
            } else {
                addResponse(context, name, !Validation.isBlank(response.message()) ? response.message() : "Error" + ": " + response.code());
            }

        } catch (Exception e) {
            GeneralHelper.auditEvent(context, EventType.EZ_API_REQUEST_ERROR, "Please check the target API service: " + e.getMessage());
            GeneralHelper.setErrorAttribute(context, Errors.EVAPI0001, getId());
            logger.error("APIRequest::" + e.getMessage());
            context.getRealm().addAttribute(context.getRequestId(), getSanitizedName(name), "failed");
            context.getRealm().addAttribute(context.getRequestId(), getSanitizedName(name) + "_completed_at", String.valueOf(Time.currentTime()));
            context.failure();
            return;
        } finally {
            if (Objects.nonNull(response)) {
                response.close();
            }
        }

        GeneralHelper.auditEvent(context, EventType.EZ_API_REQUEST, "");
        context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
        context.success();
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
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
        return "bo-api-request";
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    private void addResponse(RequiredActionContext context, String requestName, String value) {
        context.getRealm().addAttribute(context.getRequestId(), getSanitizedName(requestName) + "_response", value);
    }

    private String getRequestUrl(RequiredActionContext context) {
        return parseVariables(context, context.getConfig().get("api-request-url"));
    }

    private String getRequestAuth(RequiredActionContext context) {
        return parseVariables(context, context.getConfig().get("api-request-bearer-token"));
    }

    private String getRequestBody(RequiredActionContext context, String body) {
        return parseVariables(context, body);
    }

    private Map<String, String> getRequestHeaders(RequiredActionContext context) {
        try {
            List<Map<String, String>> headerList = JsonSerialization.readValue(parseVariables(context, context.getConfig().get("api-request-headers")), new TypeReference<List<Map<String, String>>>() {
            });
            Map<String, String> headers = new HashMap<>();
            headerList.forEach(x -> headers.put(x.get("key"), x.get("value")));
            return headers;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String parseVariables(RequiredActionContext context, String value) {
        ActionModel actionById = context.getRealm().getActionById(context.getAuthenticationSession().getAuthNote(CURRENT_ACTION_ID));
        TrxModel trx = context.getRealm().getTrx(context.getRequestId());
        return new ConfigTemplateParser(actionById.getActionVariables(), trx == null ? new HashMap<>() : trx.getAttributes()).parseTemplate(value);
    }

    private boolean isBodyRequired(String bodyType) {
        return bodyType != null && !bodyType.equals("none");
    }

    private String getSanitizedName(String name) {
        if (Validation.isBlank(name)) return "";

        return name.toLowerCase().replace(" ", "_");
    }

    @Override
    public void onUpdate(RequiredActionProviderRepresentation rep) {
        boolean bodyRequired = isBodyRequired(rep.getConfig().get("api-request-body-type"));
        String contentType = rep.getConfig().get("api-request-content-type");
        if (bodyRequired && contentType != null && Objects.equals(contentType, "application/json")) {
            try {
                JsonSerialization.readValue(rep.getConfig().get("api-request-body-other"), new TypeReference<HashMap<String, Object>>() {
                });
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid json body");
            }
        }
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{"api_request"};
    }

    @Override
    public boolean skippable() {
        return false;
    }

    private String getAuditRepresentation(String name, Response response) throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        map.put("request_name", name);
        map.put("status_code", response.code());
        if (response.body() != null && response.body().contentLength() > 0) {
            map.put("response_body", response.body().string());
        }
        response.close();
        return JsonUtil.getAsJsonPrettyString(map);
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId =
                context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public boolean isClaimsCustomizable() {
        return false;
    }
}
