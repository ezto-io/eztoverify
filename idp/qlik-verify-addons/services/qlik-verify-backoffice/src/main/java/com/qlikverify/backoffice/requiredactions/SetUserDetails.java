package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.helpers.templateConfigParser.ConfigTemplateParser;
import com.qlikverify.backoffice.validators.ValidatorPolicy;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.util.JsonSerialization;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.ATTRIBUTE_PHONE_NUMBER;
import static com.qlikverify.backoffice.Constants.BO_VERIFY_PHONE_NUMBER_FTL;
import static org.keycloak.models.Constants.CURRENT_ACTION_ID;
import static org.keycloak.models.UserModel.MOBILE_NUMBER;
import static org.keycloak.models.UserModel.MOBILE_NUMBER_REGION;

public class SetUserDetails implements RequiredActionProvider, RequiredActionFactory {

    private static final Logger logger = Logger.getLogger(SetUserDetails.class);

    KeycloakSession session;
    @Override
    public String getDisplayText() {
        return "bo-set-user-details";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    static List<ProviderConfigProperty> configProperties = new ArrayList<>();
    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("user-detail-name");
        property.setLabel("user-detail-name");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText("user-detail-name-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("enable-user-details");
        property.setLabel("Enable user details");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName("set-user-details");
        property.setLabel("Set user details");
        property.setType(ProviderConfigProperty.MAP_TYPE);
        property.setHelpText("set-user-details-hlptxt");
        property.setDependsOn(new HashMap<>() {{
            put("enable-user-details", true);
        }});

        configProperties.add(property);
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

        String name = context.getConfig().get("user-detail-name").toLowerCase().replaceAll("\\s+", "_");
        boolean detailsSet = context.getRealm().hasTrxValueAttribute(requestId, "sp@" + name);
        if (result && !detailsSet) {
            addAction(context);
        } else {
            context.getUser().removeRequiredAction(context.getModel().getId());
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
        Map<String, String> config = context.getConfig();

        if (config.containsKey("set-user-details") && config.get("set-user-details") != null && Boolean.parseBoolean(config.get("enable-user-details"))) {
            try {
                List<Map<String, String>> details = JsonSerialization.readValue(parseVariables(context, context.getConfig().get("set-user-details")), new TypeReference<List<Map<String, String>>>() {
                });
                if(details.isEmpty()) {
                    context.success();
                    return;
                }
                details.forEach(x-> setUserDetails(context, x));
//                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_SET_USER_DETAILS, "");
                context.getRealm().addAttribute(context.getRequestId(), "sp@" + context.getConfig().get("user-detail-name").toLowerCase().replaceAll("\\s+", "_"), "true");
            } catch (IllegalArgumentException e) {
                logger.error("SetUserDetails :: exception :: " + e.getMessage());
//                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_SET_USER_DETAILS_ERROR, e.getMessage());
                context.getRealm().addAttribute(context.getRequestId(), "sp@" + context.getConfig().get("user-detail-name").toLowerCase().replaceAll("\\s+", "_"), "false");
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error("SetUserDetails :: exception :: " + e.getMessage());
//                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_SET_USER_DETAILS_ERROR, "Error while updating details to the user");
                context.getRealm().addAttribute(context.getRequestId(), "sp@" + context.getConfig().get("user-detail-name").toLowerCase().replaceAll("\\s+", "_"), "false");
            }
            context.success();
        }
    }

    private void setUserDetails(RequiredActionContext context, Map<String, String> details) {
        switch (details.get("key")) {
            case "phone_no_with_code" :
                setUserPhoneDetails(context, details.get("value"));
                break;
        }
    }

    private void setUserPhoneDetails(RequiredActionContext context, String phoneNumberWithCountryCode) {
        Map<String, String> phoneNoDetails = com.qlikverify.backoffice.GeneralHelper.parseMobileNumber(phoneNumberWithCountryCode);
        String mobileNumber = phoneNoDetails.get("phoneNoReg") + phoneNoDetails.get("phoneNo");

        UserModel user = context.getSession().users().getUserByMobile(mobileNumber, context.getRealm());

        if (user != null && context.getUser() != null && !context.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Mobile number is already registered");
        }

        context.getUser().setMobileNumber(phoneNoDetails.get("phoneNo"));
        context.getUser().setMobileNumberRegion(phoneNoDetails.get("phoneNoReg"));
        context.getUser().setSingleAttribute(MOBILE_NUMBER, phoneNoDetails.get("phoneNo"));
        context.getUser().setSingleAttribute(MOBILE_NUMBER_REGION, phoneNoDetails.get("phoneNoReg"));
    }

    private String parseVariables(RequiredActionContext context, String value) {
        ActionModel actionById = context.getRealm().getActionById(context.getAuthenticationSession().getAuthNote(CURRENT_ACTION_ID));
        TrxModel trx = context.getRealm().getTrx(context.getRequestId());
        return new ConfigTemplateParser(actionById.getActionVariables(), trx == null ? new HashMap<>(): trx.getAttributes()).parseTemplate(value);
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = new LinkedHashMap<>();
        String title = model.getConfig().get("user-detail-name").toLowerCase().replaceAll("\\s+", "_");

        claims.put(title, ValidatorPolicy.ValidationTypes.BOOLEAN.getValue());
        return claims;
    }

    @Override
    public void onUpdate(RequiredActionProviderRepresentation rep) {
        String formTitle = rep.getConfig().get("user-detail-name");
        String regex = "[^a-zA-Z0-9 ]";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(formTitle);

        if(matcher.find()) {
            throw new IllegalArgumentException("Enter valid title without special characters");
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        //NOP
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "bo-set-user-details";
    }

    @Override
    public String[] stripeMappingId() {
        return new String[] {"set_user_details"};
    }

    @Override
    public boolean isGifRequired() { return false; }

    @Override
    public boolean skippable() { return false; }

}