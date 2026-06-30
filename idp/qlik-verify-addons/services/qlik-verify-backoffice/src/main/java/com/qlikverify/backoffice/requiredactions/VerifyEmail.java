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

import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VerifyEmail implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(VerifyEmail.class);

    @Override
    public String getDisplayText() {
        return "bo-verify-email";
    }


    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }
        String requestId = context.getRequestId();
        boolean email = context.getRealm().hasTrxValueAttribute(requestId, VERIFY_EMAIL_CLAIM);
        boolean emailVerified = context.getRealm().hasTrxValueAttribute(requestId, VERIFY_EMAIL_VERIFIED_CLAIM);
        boolean emailVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId, VERIFY_EMAIL_VERIFIED_AT_CLAIM);
        boolean hasSkipped = Boolean.parseBoolean(context.getRealm().getTrxValue(requestId,  VERIFY_EMAIL_SKIPPED));
        if (result && !hasSkipped && (!emailVerifiedAt || !email || !emailVerified)) {
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
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());

        if (context.getUser().getEmail() != null) {
            String expiryTime = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
            emailUpdate(context, context.getUser().getEmail(), expiryTime, false);
        } else if (context.getRealm().getTrx(context.getRequestId()).getAttributes().entrySet().stream().anyMatch(x -> x.getKey().endsWith("email_prefill"))) {
            TrxModel trx = context.getRealm().getTrx(context.getRequestId());
            String email = null;
            if (trx != null) {
                Map<String, String> attributes = trx.getAttributes();
                if (attributes != null) {
                    Optional<Map.Entry<String, String>> entry = attributes.entrySet().stream().filter(x -> x.getKey().endsWith("email_prefill")).findFirst();
                    if (entry.isPresent()) {
                        email = entry.get().getValue();
                    }
                }
            }
            String expiryTime = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
            emailUpdate(context, email, expiryTime, false);
        } else {
            context.challenge(loginFormsProvider.createForm(BO_VERIFY_EMAIL_FTL));
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        if (context.getUser() == null) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, ErrorMappings.USER_NOT_ONBOARDED);
            showView(context, "user_not_onboarded", BACKOFFICE_ERROR_FTL);
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("cancel") && Boolean.parseBoolean(formData.getFirst("cancel"))) {
            com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel(context, EventType.EZ_EMAIL_VERIFICATION_ERROR);
            return;
        }

        if (formData.containsKey("skipped") && Boolean.parseBoolean(formData.getFirst("skipped"))) {
            context.getRealm().addAttribute(context.getRequestId(),  VERIFY_EMAIL_SKIPPED, "true");
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION, "", skipDetails);
            context.success();
            return;
        }

        String expiryTime = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
        if (Boolean.parseBoolean(formData.getFirst(FORM_EDIT))) {
            Response challenge = context.form()
                    .setAttribute(SKIPPABLE, context.getModel().isSkippable())
                    .createForm(BO_VERIFY_EMAIL_FTL);
            context.challenge(challenge);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
            formResend(context, formData, expiryTime, formData.getFirst("editUserDetail"));
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_EMAIL_UPDATE))) {
            String email = formData.getFirst(EMAIL);
            emailUpdate(context, email, expiryTime, true);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_OTP_VERIFICATION))) {
            otpVerification(context, formData);
        }
    }

    private void emailUpdate(RequiredActionContext context, String email, String expiryTime, boolean editUserDetail) {
        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), VERIFY_EMAIL);

        if (TextUtils.isEmpty(email)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, ErrorMappings.EMAIL_REQUIRED);
            showView(context, ErrorMappings.EMAIL_REQUIRED, BO_VERIFY_EMAIL_FTL);
            return;
        }
        if (!GeneralHelper.isValidEmail(email)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.EMAIL_INVALID))
                    .setAttribute(ERROR, ErrorMappings.EMAIL_INVALID)
                    .setAttribute(EMAIL, email)
                    .setAttribute(SKIPPABLE, context.getModel().isSkippable())
                    .createForm(BO_VERIFY_EMAIL_FTL);
            context.challenge(challenge);
            return;
        }

        if (!isAllowedDomain(context, email)) {
            return;
        }

        UserModel user = context.getSession().users().getUserByEmail(context.getRealm(), email);
        if (user != null && context.getUser() != null && !context.getUser().getId().equals(user.getId())) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, ErrorMappings.USER_EXISTS_SAME_EMAIL);
            showView(context, ErrorMappings.USER_EXISTS_SAME_EMAIL, BO_VERIFY_EMAIL_FTL);
            return;
        }

        context.getAuthenticationSession().setAuthNote(EMAIL, email);

        if (!GeneralHelper.sendEmailOTP(context, email, expiryTime)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, ErrorMappings.FAILED_TO_SEND_EMAIL);
            showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, BO_VERIFY_EMAIL_FTL);
            return;
        }

        try {
            email = context.getUser().getEmail() != null ? org.keycloak.authentication.authenticators.utils.GeneralHelper.maskEmailAddress(email) : email;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        String otp_len = GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH);

        Response challenge = context.form()
                .setAttribute(EMAIL, email)
                .setAttribute("editUserDetail", String.valueOf(editUserDetail))
                .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY))
                .setAttribute("mode", context.getRealm().getMode())
                .setAttribute(OTP_LENGTH_ATTR, otp_len)
                .createForm(OTP_VERIFICATION_FTL);
        context.challenge(challenge);
    }

    private void formResend(RequiredActionContext context, MultivaluedMap<String, String> formData, String expiryTime, String editUserDetail) {
        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), VERIFY_EMAIL);
        String email = context.getAuthenticationSession().getAuthNote(EMAIL);
        String otp_len = GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH);
        if (!GeneralHelper.sendEmailOTP(context, email, expiryTime)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, ErrorMappings.FAILED_TO_SEND_EMAIL);
            showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, BO_VERIFY_EMAIL_FTL);
        } else {
            try {
                email = context.getUser().getEmail() != null ? org.keycloak.authentication.authenticators.utils.GeneralHelper.maskEmailAddress(email) : email;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Response challenge = context.form()
                    .setAttribute(EMAIL, email)
                    .setAttribute("editUserDetail", editUserDetail)
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY))
                    .setAttribute("mode", context.getRealm().getMode())
                    .setAttribute(OTP_LENGTH_ATTR, otp_len)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
        }
    }

    private void otpVerification(RequiredActionContext context, MultivaluedMap<String, String> formData) {
        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), VERIFY_EMAIL);
        String email = context.getAuthenticationSession().getAuthNote(EMAIL);
        String otp = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP);
        String otpExpiry = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY);
        String otp_len = GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH);


        if (System.currentTimeMillis() > Long.parseLong(otpExpiry)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, ErrorMappings.OTP_EXPIRED);
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.OTP_EXPIRED))
                    .setAttribute(ERROR, ErrorMappings.OTP_EXPIRED)
                    .setAttribute(EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiry)
                    .setAttribute("mode", context.getRealm().getMode())
                    .setAttribute(OTP_LENGTH_ATTR, otp_len)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;
        }

        if (!formData.getFirst(FORM_OTP).equals(otp)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, ErrorMappings.WRONG_OTP_ERROR);
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.WRONG_OTP_ERROR))
                    .setAttribute(EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                    .setAttribute(ERROR, ErrorMappings.WRONG_OTP_ERROR)
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiry)
                    .setAttribute("mode", context.getRealm().getMode())
                    .setAttribute(OTP_LENGTH_ATTR, otp_len)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;
        }
        String requestId = context.getRequestId();
        context.getUser().setEmail(email);
        context.getRealm().addAttribute(requestId, VERIFY_EMAIL_CLAIM,email);
        context.getRealm().addAttribute(requestId, VERIFY_EMAIL_VERIFIED_CLAIM,"true");
        context.getRealm().addAttribute(requestId, VERIFY_EMAIL_VERIFIED_AT_CLAIM, String.valueOf(Time.currentTime()));
        com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION, "");

        context.success();
    }

    private boolean isAllowedDomain(RequiredActionContext context, String email) {
        String allowedDmsConfig = context.getConfig().get("allowed_domains");
        if (allowedDmsConfig != null && !allowedDmsConfig.isEmpty()) {
            List<String> allowedDomains = Arrays.asList(allowedDmsConfig.split("##"));
            if (!allowedDomains.isEmpty()) {
                String[] splitted = email.split("@");
                Optional<String> allowedDms = allowedDomains.stream().filter(x -> x.equalsIgnoreCase(splitted[1])).findAny();
                if (!allowedDms.isPresent() && allowedDms.isEmpty()) {
                    com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VERIFICATION_ERROR, ErrorMappings.RESTRICTED_DOMAIN_EMAIL);
                    showView(context, ErrorMappings.RESTRICTED_DOMAIN_EMAIL, BO_VERIFY_EMAIL_FTL);
                    return false;
                }
            }
        }
        return true;
    }

    private void showView(RequiredActionContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        formsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
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
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "bo-verify-email";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(Constants.VERIFICATION_EXPIRY_CONFIG);
        property.setLabel("email-config-expiry-duration");
        property.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        property.setHelpText("email-config-expiry-duration-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(Constants.EMAIL_VER_OTP_LENGTH);
        property.setLabel(Constants.EMAIL_VER_OTP_LENGTH_LABEL);
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("email-ver-otp-length-helptext");
        property.setDefaultValue(6);
        property.setMinVal(4);
        property.setMaxVal(8);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("allowed_domains");
        property.setLabel("allowed_domains_label");
        property.setMaxVal(2000);
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        property.setHelpText("allowed_domains-helptext");
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.EMAIL_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.EMAIL_SENDER);
        provider.setLabel(Constants.EMAIL_SENDER_LABEL);

        if (session.getContext().getRealm().isEnabledOwnEmailProvider()) {
            List<EmailConfigModel> providers = session.getContext().getRealm().getEmailConfigsStream().collect(Collectors.toList());

            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getEmailProviderId()));
            }
            provider.setRequired(true);
            if (!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("email-config-set-sender");
                provider.setHelperLink("");
                provider.setHelperLinkText("");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("");
                provider.setHelperLinkText("no-email-reg");
                provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/email/providers/create");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("");
            provider.setHelperLinkText("all-emails-routed-root-workspace");
            provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/realm-subscription");
        }

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
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
    public String[] stripeMappingId() {
        return new String[]{VERIFY_EMAIL};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(VERIFY_EMAIL_CLAIM, ValidationTypes.STRING.getValue());
        defaultClaims.put(VERIFY_EMAIL_VERIFIED_CLAIM, ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(VERIFY_EMAIL_VERIFIED_AT_CLAIM, ValidationTypes.DATE_AND_TIME.getValue());
        defaultClaims.put(VERIFY_EMAIL_SKIPPED, ValidationTypes.BOOLEAN.getValue());

        return defaultClaims;
    }
}
