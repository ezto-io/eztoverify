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

package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.helpers.GeneralHelper;
import org.apache.commons.validator.routines.UrlValidator;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;

import javax.ws.rs.core.Response;
import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.helpers.GeneralHelper.showView;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TermsAndConditions implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    public static final String PROVIDER_ID = "terms-and-conditions";
    public static final String BO_TERMS_AND_CONDITIONS_ACCEPTED_AT = "terms_and_conditions_accepted_at";
    public static final String BO_TERMS_AND_CONDITIONS_VERSION = "terms_and_conditions_version";
    public static final String TERMS_AND_CONDITIONS_ACCEPTED_VERSION = "terms_and_conditions_accepted_version";

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }
        String requestId = context.getRequestId();
        boolean tcVersion = context.getRealm().hasTrxValueAttribute(requestId, BO_TERMS_AND_CONDITIONS_VERSION);
        boolean tcVersionVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId, BO_TERMS_AND_CONDITIONS_ACCEPTED_AT);
        boolean hasSkipped = Boolean.parseBoolean(context.getRealm().getTrxValue(requestId,  TERMS_AND_CONDITIONS_SKIPPED));

        if(context.getUser().getAttributes().get(TERMS_AND_CONDITIONS_ACCEPTED_VERSION) != null && (context.getConfig().get(TERMS_AND_CONDITION_VER) != null && !context.getConfig().get(TERMS_AND_CONDITION_VER).isEmpty())){
            List<String> versions = context.getUser().getAttributes().get(TERMS_AND_CONDITIONS_ACCEPTED_VERSION);
            List<TermsAndConditionsModel> termsAndConditionVer = context.getRealm().getTermsAndConditionsByVersion(((RequiredActionContextResult) context).getModel().getId(), context.getConfig().get(TERMS_AND_CONDITION_VER));
            if(result && !hasSkipped && !versions.contains(termsAndConditionVer.get(0).getId())){
                addAction(context);
            }
        }else{
            if(result && !hasSkipped)
                addAction(context);
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

        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);

        if(context.getConfig().get(TERMS_AND_CONDITION_VER) == null || context.getConfig().get(TERMS_AND_CONDITION_VER).isEmpty()){
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_TERMS_AND_CONDITIONS_ERROR, "Terms and Conditions version is missing in config.");
            showView(context, "An error occurred. Please contact admin", BACKOFFICE_ERROR_FTL, null);
            return;
        }

        String content = "";

        List<TermsAndConditionsModel> termsAndConditionVer = context.getRealm().getTermsAndConditionsByVersion(((RequiredActionContextResult) context).getModel().getId(), context.getConfig().get(TERMS_AND_CONDITION_VER));

        TermsAndConditionsModel model = termsAndConditionVer.get(0);
        String type = model.getType();
        if(type.equals("Link") || type.equals("Text")){
            content = model.getContent();
        } else{
            String bucketName = context.getRealm().getName();
            String objectName = model.getContent();

            content = GeneralHelper.getPresignedObjectUrl(context, objectName);
        }

        Response challenge = context.form()
                .setAttribute("tcContent", content)
                .setAttribute("tcVer",context.getConfig().get(TERMS_AND_CONDITION_VER))
                .setAttribute("type", type)
                .setAttribute(SKIPPABLE, context.getModel().isSkippable())
                .setAttribute("isWeb", !(cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop()))
                .createForm("bo-terms.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        if(context.getUser() == null){
            showView(context, "user_not_onboarded", BACKOFFICE_ERROR_FTL, null);
            return;
        }

        if (context.getHttpRequest().getDecodedFormParameters().containsKey("cancel")) {
            context.getUser().removeAttribute(BO_TERMS_AND_CONDITIONS_ACCEPTED_AT);
            context.getUser().removeAttribute(BO_TERMS_AND_CONDITIONS_VERSION);
            GeneralHelper.handleCancel(context, EventType.EZ_TERMS_AND_CONDITIONS_ERROR);
            return;
        }
        if(context.getHttpRequest().getFormParameters().containsKey("skipped") && Boolean.parseBoolean(context.getHttpRequest().getFormParameters().getFirst("skipped"))) {
            context.getRealm().addAttribute(context.getRequestId(),  TERMS_AND_CONDITIONS_SKIPPED, "true");
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_TERMS_AND_CONDITIONS, "", skipDetails);
            context.success();
            return;
        }
        String requestId = context.getRequestId();
        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), "terms_and_conditions");

        List<String> termsAndConditionsAcceptedVersion = context.getUser().getAttributes().get(TERMS_AND_CONDITIONS_ACCEPTED_VERSION) != null ?
                context.getUser().getAttributes().get(TERMS_AND_CONDITIONS_ACCEPTED_VERSION) : new ArrayList<>();
        List<TermsAndConditionsModel> termsAndConditionVer = context.getRealm().getTermsAndConditionsByVersion(((RequiredActionContextResult) context).getModel().getId(), context.getConfig().get(TERMS_AND_CONDITION_VER));
        termsAndConditionsAcceptedVersion.add(termsAndConditionVer.get(0).getId());

        context.getUser().setAttribute(TERMS_AND_CONDITIONS_ACCEPTED_VERSION, termsAndConditionsAcceptedVersion);
        context.getRealm().addAttribute(requestId, BO_TERMS_AND_CONDITIONS_ACCEPTED_AT, Integer.toString(Time.currentTime()));
        context.getRealm().addAttribute(requestId, BO_TERMS_AND_CONDITIONS_VERSION, getConfigValue(context,TERMS_AND_CONDITION_VER));
        GeneralHelper.auditEvent(context, EventType.EZ_TERMS_AND_CONDITIONS, "");
        context.success();
    }

    @Override
    public String getDisplayText() {
        return PROVIDER_ID;
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
        return new String[] {"bo"};
    }

    @Override
    public void close() {
        // NOP
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {
        ProviderConfigProperty property;

        List<String> types = new ArrayList<>();
        types.add("Link");
        types.add("PDF");
        types.add("Image");
        types.add("Text");

        property = new ProviderConfigProperty();
        property.setName(TERMS_AND_CONDITION_VER);
        property.setLabel("terms-condn-ver");
        property.setType(ProviderConfigProperty.VERSION);
        property.setHelpText("terms-condn-ver-hlp-txt");
        property.setRequired(true);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("terms_and_condition_type");
        property.setLabel("terms-condn-type");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(types);
        property.setRequired(true);
        property.setHelpText("terms-condn-type-hlp-txt");
        configProperties.add(property);

        Map<String, Object> pdfDependsOn = new HashMap<>();
        pdfDependsOn.put("terms_and_condition_type", "PDF");
        property = new ProviderConfigProperty();
        property.setName("terms_and_condition_pdf");
        property.setLabel("terms-condn-pdf");
        property.setType(ProviderConfigProperty.PDF_FILE);
        property.setDependsOn(pdfDependsOn);
        property.setHelpText("terms-condn-file-limit-hlp-txt");
        configProperties.add(property);

        Map<String, Object> linkDependsOn = new HashMap<>();
        linkDependsOn.put("terms_and_condition_type", "Link");

        property = new ProviderConfigProperty();
        property.setName("terms_and_condition_link");
        property.setLabel("terms-condn-link");
        property.setHelpText("terms-condn-link-hlp-txt");
        property.setType(ProviderConfigProperty.URL);
        property.setDependsOn(linkDependsOn);
        configProperties.add(property);

        Map<String, Object> imageDependsOn = new HashMap<>();
        imageDependsOn.put("terms_and_condition_type", "Image");
        property = new ProviderConfigProperty();
        property.setName("terms_and_condition_image");
        property.setLabel("terms-condn-image");
        property.setType(ProviderConfigProperty.IMAGE_FILE);
        property.setDependsOn(imageDependsOn);
        property.setHelpText("terms-and-condition-image-hlp-txt");
        configProperties.add(property);

        Map<String, Object> textDependsOn = new HashMap<>();
        textDependsOn.put("terms_and_condition_type", "Text");

        property = new ProviderConfigProperty();
        property.setName("terms_and_condition_text");
        property.setLabel("terms-condn-text");
        property.setType(ProviderConfigProperty.TEXT_EDITOR);
        property.setHelpText("terms-and-condition-text-hlp-txt");
        property.setDependsOn(textDependsOn);
        configProperties.add(property);
    }


    public static String getConfigValue(RequiredActionContext context, String configValue) {
        if (context.getConfig() == null) {
            return "";
        }
        String data = context.getConfig().get(configValue);
        if (data == null || data.isEmpty()) {
            return "";
        } else {
            return data;
        }
    }

    @Override
    public String[] stripeMappingId() {
        return new String[] {"terms_and_conditions"};
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId =
                context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(BO_TERMS_AND_CONDITIONS_ACCEPTED_AT, ValidationTypes.DATE_AND_TIME.getValue());
        defaultClaims.put(BO_TERMS_AND_CONDITIONS_VERSION, ValidationTypes.STRING.getValue());
        defaultClaims.put(TERMS_AND_CONDITIONS_SKIPPED, ValidationTypes.BOOLEAN.getValue());

        return defaultClaims;
    }

    @Override
    public boolean isClaimsCustomizable() {
        return false;
    }
    
    @Override
    public void onUpdate(RequiredActionProviderRepresentation rep) {
        if("Link".equals(rep.getConfig().get("terms_and_condition_type"))){
            String termsAndConditionLink = rep.getConfig().get("terms_and_condition_link");
            if (termsAndConditionLink == null || termsAndConditionLink.isEmpty()) {
                throw new IllegalArgumentException("The terms and condition link is empty.");
            }
            UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});
            if (!urlValidator.isValid(termsAndConditionLink)) {
                throw new IllegalArgumentException("Invalid terms and condition link: " + termsAndConditionLink);
            }
        }
    }
}
