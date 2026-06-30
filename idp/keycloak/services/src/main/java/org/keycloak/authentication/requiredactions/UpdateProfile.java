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

package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.util.UpdateProfileContext;
import org.keycloak.authentication.requiredactions.util.UserUpdateProfileContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.IdpReviewProfileBean;
import org.keycloak.forms.login.freemarker.model.ProfileBean;
import org.keycloak.models.*;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.EventAuditingAttributeChangeListener;
import org.keycloak.utils.GeneralHelper;
import org.keycloak.authentication.authenticators.utils.Constants;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.forms.login.LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UpdateProfile implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getPolicyId() != null) {
            boolean result;
            String policyType = context.getPolicyType();
            if (policyType.equals("action")) {
                result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
            } else {
                result = context.getSession().getProvider(CibaPolicyProvider.class).validate(context.getSession(), context.getAuthenticationSession().getAuthNote(BINDING_MESSAGE), context.getPolicyId());
            }if (result) {
                addAction(context);
            } else {
                context.getUser().removeRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
            }
        }
    }

    private void addAction(RequiredActionContext context) {
        if (!context.getUser().getRequiredActionsStream().filter(x -> x.equalsIgnoreCase(UserModel.RequiredAction.UPDATE_PROFILE.name())).findFirst().isPresent()) {
            context.getUser().addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
        }
    }


    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Map<String,Object> attrsToSent = new HashMap<>();
        attrsToSent.put("updateMail", Boolean.parseBoolean(GeneralHelper.getConfigValue(context,"add_email_property")));
        attrsToSent.put("updateMob", Boolean.parseBoolean(GeneralHelper.getConfigValue(context,"add_mob_property")));
        attrsToSent.put("defReg", context.getRealm().getDefaultRegion());
        context.challenge(createResponse(context, attrsToSent, null,null));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        event.event(EventType.UPDATE_PROFILE).detail(Details.CONTEXT, UserProfileContext.UPDATE_PROFILE.name());
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        MultivaluedMap<String,String> formDataToSend = formData;
        UserModel user = context.getUser();
        boolean isEmailUpdated = false;
        boolean isMobNumUpdated = false;
        if(formData.containsKey("email") && !formData.getFirst("email").equals(user.getEmail())){
            isEmailUpdated = true;
        }
        if(formData.containsKey("phoneNumber") &&
                !(formData.getFirst(Constants.PHONE_NUMBER_REGION)+formData.get(Constants.FORM_PHONE_NUMBER).get(1))
                        .equals(user.getMobileNumberRegion()+user.getMobileNumber())){
            isMobNumUpdated = true;
        }

        if(!formData.containsKey("email")){
            formDataToSend.put("email", Collections.singletonList(user.getEmail()));
        }
        if(!formData.containsKey("username")){
            formDataToSend.put("username", Collections.singletonList(user.getUsername()));
        }
        if(!formData.containsKey("firstName")){
            formDataToSend.put("firstName", Collections.singletonList(user.getFirstName()));
        }
        if(!formData.containsKey("lastName")){
            formDataToSend.put("lastName", Collections.singletonList(user.getLastName()));
        }
        if (Boolean.parseBoolean(formData.getFirst(Constants.FORM_EDIT))) {
            showView(context,null,Constants.UPDATE_PROFILE_FTL);
            return;
        }
        if (Boolean.parseBoolean(formData.getFirst(Constants.FORM_RESEND))) {
            //for resend email OTP
            if(formData.getFirst("emailOtpVerify") != null) {
                showEmailOtpVerifyView(context);
                return;
            }

            //for resent phone OTP
            if(formData.getFirst("phoneOtpVerify") != null){
                showPhoneOtpVerifyView(context);
                return;
            }
        }

        UserProfileProvider provider = context.getSession().getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, formDataToSend, user);

        try {
            // backward compatibility with old account console where attributes are not removed if missing
            profile.update(false, new EventAuditingAttributeChangeListener(profile, event));
        } catch (IllegalStateException e){
            showView(context, Constants.PHONE_ALREADY_REGISTERED, Constants.UPDATE_PROFILE_FTL);
        } catch (ValidationException pve) {
            List<FormMessage> errors = Validation.getFormErrorsFromValidation(pve.getErrors());
            context.challenge(createResponse(context,null, formData, errors));
        }

        if(Boolean.parseBoolean(formData.getFirst("isUpdateProfile"))){
            //update email
            if(formData.containsKey("updateMail") && Boolean.parseBoolean(formData.getFirst("updateMail")) && isEmailUpdated){
                String email = formData.getFirst("email");
                UserModel userModel = context.getSession().users().getUserByEmail(context.getRealm(),email);
                if(userModel != null && context.getUser() != null && !context.getUser().getId().equals(userModel.getId())) {
                    showView(context, Constants.USER_EXISTS_SAME_EMAIL, Constants.UPDATE_PROFILE_FTL);
                    return;
                }

                if(Boolean.parseBoolean(GeneralHelper.getConfigValue(context,"enable_email_verification"))){
                    context.getAuthenticationSession().setAuthNote("email", email);
                    context.getAuthenticationSession().setAuthNote("verifyEmail", "true");
                } else {
                    user.setEmail(email);
                }
            }

            //update mobileNumber
            if(formData.containsKey("updateMob") && Boolean.parseBoolean(formData.getFirst("updateMob")) && isMobNumUpdated){
                String region     = formData.getFirst(Constants.PHONE_NUMBER_REGION);
                String phoneNum   = formData.get(Constants.FORM_PHONE_NUMBER).get(1);
                NumberFormat numberFormat = GeneralHelper.parseMobileNumber(context,region.toUpperCase(),phoneNum);
                if(numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")){
                    showView(context, Constants.PHONE_NUMBER_INVALID, Constants.UPDATE_PROFILE_FTL);
                    return;
                }

                String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length()+1);
                if (phoneNumber == null || !(phoneNumber.length() > 0)) {
                    showView(context, Constants.PHONE_NUMBER_INVALID, Constants.UPDATE_PROFILE_FTL);
                    return;
                }
                String mobNum = region + phoneNumber;
                UserModel existingUserWithMobile = context.getSession().users().getUserByMobile(mobNum, context.getRealm());

                if (existingUserWithMobile != null && !user.getId().equals(existingUserWithMobile.getId())) {
                    showView(context, Constants.PHONE_ALREADY_REGISTERED, Constants.UPDATE_PROFILE_FTL);
                    return;
                }

                if(Boolean.parseBoolean(GeneralHelper.getConfigValue(context,"enable_phone_verification"))){
                    context.getAuthenticationSession().setAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, numberFormat.getE16Format());
                    context.getAuthenticationSession().setAuthNote(Constants.FORM_PHONE_NUMBER, phoneNumber);
                    context.getAuthenticationSession().setAuthNote(Constants.PHONE_NUMBER_REGION, region);
                    context.getAuthenticationSession().setAuthNote("verifyPhone", "true");
                } else {
                    user.setSingleAttribute(UserModel.MOBILE_NUMBER_REGION,region);
                    user.setSingleAttribute(UserModel.MOBILE_NUMBER,phoneNum);
                    user.setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
                }
            }
        }

        if(Boolean.parseBoolean(formData.getFirst("isOtpVerification"))) {
            if(formData.getFirst("emailOtpVerify") != null){
                String sessEmail = context.getAuthenticationSession().getAuthNote("email");
                if (sessEmail == null && sessEmail.length() == 0) {
                    showView(context, Constants.EMAIL_REQUIRED, Constants.UPDATE_PROFILE_FTL);
                    return;
                }
                String otp          = context.getAuthenticationSession().getAuthNote(Constants.SESSION_EMAIL_OTP);
                String otpExpiry    = context.getAuthenticationSession().getAuthNote(Constants.SESSION_EMAIL_OTP_EXPIRY);
                context.form().setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, otpExpiry);


                if (System.currentTimeMillis() > Long.parseLong(otpExpiry)) {
                    Response challenge = context.form()
                            .addError(new FormMessage(Constants.OTP_EXPIRED))
                            .setAttribute("email", context.getAuthenticationSession().getAuthNote("email"))
                            .createForm(Constants.OTP_VERIFICATION_FTL);
                    context.challenge(challenge);
                    return;
                }

                if (!formData.getFirst(Constants.FORM_OTP).equals(otp)) {
                    Response challenge = context.form()
                            .addError(new FormMessage(Constants.WRONG_OTP_ERROR))
                            .setAttribute("email", context.getAuthenticationSession().getAuthNote("email"))
                            .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, otpExpiry)
                            .createForm(Constants.OTP_VERIFICATION_FTL);
                    context.challenge(challenge);
                    return;
                }
                context.getUser().setEmail(sessEmail);
                context.getUser().setEmailVerified(true);
                context.getAuthenticationSession().removeAuthNote("verifyEmail");
            }
            if(formData.getFirst("phoneOtpVerify") != null){
                String mobNum = context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_REGION) + context.getAuthenticationSession().getAuthNote(Constants.FORM_PHONE_NUMBER);
                UserModel existingUserWithMobile = context.getSession().users().getUserByMobile(mobNum, context.getRealm());

                if (existingUserWithMobile != null) {
                    showView(context, Constants.PHONE_ALREADY_REGISTERED, Constants.UPDATE_PROFILE_FTL);
                    return;
                }

                String otpExpiryTime = context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY);

                if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
                    Response challenge = context.form()
                            .addError(new FormMessage(Constants.OTP_EXPIRED))
                            .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE))
                            .createForm(Constants.OTP_VERIFICATION_FTL);
                    context.challenge(challenge);
                    return;
                }

                if (formData.getFirst(Constants.FORM_OTP) == null || formData.getFirst(Constants.FORM_OTP).length() == 0) {
                    Response challenge = context.form()
                            .addError(new FormMessage(Constants.INVALID_OTP))
                            .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE))
                            .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                            .createForm(Constants.OTP_VERIFICATION_FTL);
                    context.challenge(challenge);
                    return;
                }

                String otp = context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP);

                if (!formData.getFirst(Constants.FORM_OTP).equals(otp)) {
                    Response challenge = context.form()
                            .addError(new FormMessage(Constants.WRONG_OTP_ERROR))
                            .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE))
                            .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                            .createForm(Constants.OTP_VERIFICATION_FTL);
                    context.challenge(challenge);
                    return;
                }
                if(context.getUser() == null) {
                    UserModel newUser = context.getSession().users().addUser(context.getRealm(), context.getAuthenticationSession().getAuthNote(Constants.ATTRIBUTE_PHONE_NUMBER));
                    newUser.setEnabled(true);
                }
                context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER_REGION,context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_REGION));
                context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER,context.getAuthenticationSession().getAuthNote(Constants.FORM_PHONE_NUMBER));
                context.getUser().setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
                context.getAuthenticationSession().removeAuthNote("verifyPhone");
            }
        }
        if(context.getAuthenticationSession().getAuthNote("verifyPhone") != null || context.getAuthenticationSession().getAuthNote("verifyEmail") != null){
            checkVerWays(context);
            return;
        }
        context.success();
    }

    private void checkVerWays(RequiredActionContext context){
        if(context.getAuthenticationSession().getAuthNote("verifyEmail") != null && Boolean.parseBoolean(context.getAuthenticationSession().getAuthNote("verifyEmail"))){
            showEmailOtpVerifyView(context);
            return;
        }
        if(context.getAuthenticationSession().getAuthNote("verifyPhone") != null && Boolean.parseBoolean(context.getAuthenticationSession().getAuthNote("verifyPhone"))){
            showPhoneOtpVerifyView(context);
            return;
        }
    }

    private void showEmailOtpVerifyView(RequiredActionContext context){
        String email = context.getAuthenticationSession().getAuthNote("email");
        String expiryTime   = GeneralHelper.getConfigValue(context, "mail-verify-expiry-time");
        if (!GeneralHelper.sendEmailOTP(context, email, expiryTime)) {
            showView(context, Constants.FAILED_TO_SEND_EMAIL_OTP, Constants.UPDATE_PROFILE_FTL);
            return;
        }
        Response challenge = context.form()
                .setAttribute("email", email)
                .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(Constants.SESSION_EMAIL_OTP_EXPIRY))
                .createForm(Constants.OTP_VERIFICATION_FTL);
        context.challenge(challenge);
        return;
    }

    private void showPhoneOtpVerifyView(RequiredActionContext context){
        String phoneNumber =  context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE);
        String expiryTime = GeneralHelper.getConfigValue(context, "mob-verify-expiry-time");
        if(!GeneralHelper.sendSmsOtp(context, phoneNumber,  expiryTime, GeneralHelper.getConfigValue(context, "sms-sender"))){
            showView(context, Constants.FAILED_TO_SEND_SMS_OTP,  Constants.UPDATE_PROFILE_FTL);
            return;
        }
        Response challenge = context.form()
                .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, phoneNumber)
                .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY))
                .createForm(Constants.OTP_VERIFICATION_FTL);
        context.challenge(challenge);
        return;
    }

    protected UserModel.RequiredAction getResponseAction(){
        return UserModel.RequiredAction.UPDATE_PROFILE;
    }

    protected Response createResponse(RequiredActionContext context, Map<String,Object> attributes, MultivaluedMap<String, String> formData, List<FormMessage> errors) {
        LoginFormsProvider form = context.form();

        if(attributes != null && !attributes.isEmpty()){
            attributes.forEach(form::setAttribute);
        }

        if (errors != null && !errors.isEmpty()) {
            form.setErrors(errors);
        }

        if(formData != null) {
            form = form.setFormData(formData);
        }

        return form.createResponse(getResponseAction());
    }


    @Override
    public void close() {

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }


    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleUpdateProfile.SINGLETON;
    }



    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getDisplayText() {
        return "update-profile";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.UPDATE_PROFILE.name();
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        setupSmsSenders(session);
        setupEmailSenders(session);

        return configProperties;
    }

    private void showView(RequiredActionContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.setAttribute("error", error);
        }
        UpdateProfileContext userBasedContext = new UserUpdateProfileContext(context.getRealm(), context.getUser());
        formsProvider.setAttribute("user",  new ProfileBean(userBasedContext, null));
        formsProvider.setAttribute("defReg", context.getRealm().getDefaultRegion());
        formsProvider.setAttribute("updateMail", Boolean.parseBoolean(GeneralHelper.getConfigValue(context,"add_email_property")));
        formsProvider.setAttribute("updateMob", Boolean.parseBoolean(GeneralHelper.getConfigValue(context,"add_mob_property")));
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"ciam","wiam"};
    }

    private void setupEmailSenders(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName().equals("email-sender")).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("email-sender");
        provider.setLabel("email-sender-label");
        provider.setDependsOn(new HashMap<String,Object>() {{
            put("enable_email_verification",true);
            put("add_email_property",true);
        }});
        provider.setValidationType(ProviderConfigProperty.VALIDATION_ALL_TRUE);

        if(session.getContext().getRealm().isEnabledOwnEmailProvider()){
            List<EmailConfigModel> providers = session.getContext().getRealm().getEmailConfigsStream().collect(Collectors.toList());
            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getEmailProviderId()));
            }

            provider.setRequired(true);
            if(!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("email-config-set-sender");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No Email Sender are registered. Click <a href=' #/realms/"+session.getContext().getRealm().getName()+"/email/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All Emails are routed through default Email Provider from "+org.keycloak.Config.getAdminRealm()+".If you want to use your email providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
        }
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    private void setupSmsSenders(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("sms-sender")).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("sms-sender");
        provider.setLabel("sms-sender-label");
        provider.setDependsOn(new HashMap<String,Object>() {{
            put("enable_phone_verification",true);
            put("add_mob_property", true);
        }});
        provider.setValidationType(ProviderConfigProperty.VALIDATION_ALL_TRUE);

        if(session.getContext().getRealm().isEnabledOwnSmsProvider()){
            List<SmsConfigModel> providers = session.getContext().getRealm().getSmsConfigsStream().filter(x-> x.getSmsProviderId() !=null && !x.getSmsProviderId().equalsIgnoreCase("whatsapp")).collect(Collectors.toList());
            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getSmsProviderId()));
            }

            provider.setRequired(true);
            if(!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("sms-sender-hlp-txt");
            }  else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No SMS Sender are registered. Click <a href=' #/realms/"+session.getContext().getRealm().getName()+"/sms/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All SMSes are routed through default SMS Provider from "+org.keycloak.Config.getAdminRealm()+".If you want to use your SMS providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
        }

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty updateEmailConfig = new ProviderConfigProperty();
        updateEmailConfig.setName("add_email_property");
        updateEmailConfig.setLabel("add-email-form-title");
        updateEmailConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        updateEmailConfig.setHelpText("enable-user-update-email");
        configProperties.add(updateEmailConfig);

        ProviderConfigProperty emailVerification = new ProviderConfigProperty();
        emailVerification.setName("enable_email_verification");
        emailVerification.setLabel("enable-email-veriication");
        emailVerification.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        emailVerification.setHelpText("verify-email-using-otp-update");
        emailVerification.setDependsOn(new HashMap<String,Object>() {{
            put("add_email_property",true);
        }});
        configProperties.add(emailVerification);

        ProviderConfigProperty updateMobConfig = new ProviderConfigProperty();
        updateMobConfig.setName("add_mob_property");
        updateMobConfig.setLabel("add-phone-number");
        updateMobConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        updateMobConfig.setHelpText("enable-user-update-phone-number");
        configProperties.add(updateMobConfig);

        ProviderConfigProperty phoneVerification = new ProviderConfigProperty();
        phoneVerification.setName("enable_phone_verification");
        phoneVerification.setLabel("enable-phone-number-verification");
        phoneVerification.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        phoneVerification.setHelpText("verify-entered-phone-number");
        phoneVerification.setDependsOn(new HashMap<String,Object>() {{
            put("add_mob_property",true);
        }});
        configProperties.add(phoneVerification);


        ProviderConfigProperty emailExpiryTime = new ProviderConfigProperty();
        emailExpiryTime.setName("mail-verify-expiry-time");
        emailExpiryTime.setLabel("email-config-expiry-duration");
        emailExpiryTime.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        emailExpiryTime.setHelpText("email-config-expiry-duration-helptext");
        emailExpiryTime.setDependsOn(new HashMap<String,Object>() {{
            put("enable_email_verification",true);
            put("add_email_property", true);
        }});
        emailExpiryTime.setValidationType(ProviderConfigProperty.VALIDATION_ALL_TRUE);
        configProperties.add(emailExpiryTime);


        ProviderConfigProperty emailOtplenConfig = new ProviderConfigProperty();
        emailOtplenConfig.setName("email_ver_otp_len");
        emailOtplenConfig.setLabel("email-ver-otp-length-label");
        emailOtplenConfig.setType(ProviderConfigProperty.NUMBER_TYPE);
        emailOtplenConfig.setHelpText("email-ver-otp-length-helptext");
        emailOtplenConfig.setDependsOn(new HashMap<String,Object>() {{
            put("enable_email_verification",true);
            put("add_email_property", true);
        }});
        emailOtplenConfig.setValidationType(ProviderConfigProperty.VALIDATION_ALL_TRUE);
        emailOtplenConfig.setMinVal(4);
        emailOtplenConfig.setMaxVal(8);
        emailOtplenConfig.setDefaultValue(6);
        configProperties.add(emailOtplenConfig);


        ProviderConfigProperty mobExpiryTime = new ProviderConfigProperty();
        mobExpiryTime.setName("mob-verify-expiry-time");
        mobExpiryTime.setLabel("phone-no-expiry-duration");
        mobExpiryTime.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        mobExpiryTime.setHelpText("phone-no-expiry-duration-helptext");
        mobExpiryTime.setDependsOn(new HashMap<String,Object>() {{
            put("enable_phone_verification",true);
            put("add_mob_property", true);
        }});
        mobExpiryTime.setValidationType(ProviderConfigProperty.VALIDATION_ALL_TRUE);
        configProperties.add(mobExpiryTime);


        ProviderConfigProperty smsOtplenConfig = new ProviderConfigProperty();
        smsOtplenConfig.setName("sms_ver_otp_len");
        smsOtplenConfig.setLabel("phone-ver-otp-length-label");
        smsOtplenConfig.setType(ProviderConfigProperty.NUMBER_TYPE);
        smsOtplenConfig.setHelpText("phone-ver-otp-length-helptext");
        smsOtplenConfig.setDependsOn(new HashMap<String,Object>() {{
            put("enable_phone_verification",true);
            put("add_mob_property", true);
        }});
        smsOtplenConfig.setMinVal(4);
        smsOtplenConfig.setMaxVal(8);
        smsOtplenConfig.setDefaultValue(6);
        configProperties.add(smsOtplenConfig);

        ProviderConfigProperty smstxtConfig = new ProviderConfigProperty();
        smstxtConfig.setName("sms_ver_otp_txt");
        smstxtConfig.setLabel("phone-num-template-text");
        smstxtConfig.setType(ProviderConfigProperty.STRING_TYPE);
        smstxtConfig.setHelpText("phone-num-template-text-helptext");
        smstxtConfig.setDependsOn(new HashMap<String,Object>() {{
            put("enable_phone_verification",true);
            put("add_mob_property", true);
        }});
        configProperties.add(smstxtConfig);

    }

}
