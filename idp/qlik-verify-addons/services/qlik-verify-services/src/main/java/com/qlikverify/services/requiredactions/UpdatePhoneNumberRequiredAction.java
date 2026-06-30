package com.qlikverify.services.requiredactions;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sms.models.NumberFormat;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.qlikverify.services.constants.Constants.*;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

public class UpdatePhoneNumberRequiredAction implements RequiredActionProvider, RequiredActionFactory {
    @Override
    public String getDisplayText() {
        return "update-phone-number-ra";
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
        if (context.getPolicyId() != null) {
            boolean result;
            String policyType = context.getPolicyType();
            if (policyType.equals("action")) {
                result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
            } else {
                result = context.getSession().getProvider(CibaPolicyProvider.class).validate(context.getSession(), context.getAuthenticationSession().getAuthNote(BINDING_MESSAGE), context.getPolicyId());
            }
            if (result) {
                addAction(context);
            } else {
                context.getUser().removeRequiredAction(getId());
            }
        }
    }
    private void addAction(RequiredActionContext context) {
        if (!context.getUser().getRequiredActionsStream().filter(x -> x.equalsIgnoreCase(getId())).findFirst().isPresent()) {
            context.getUser().addRequiredAction(getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        showView(context, null, UPDATE_PHONE_NUMBER_FTL);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (Boolean.parseBoolean(formData.getFirst(FORM_EDIT))) {
            Response challenge = context.form()
                    .setAttribute("defReg", context.getRealm().getDefaultRegion())
                    .setAttribute(PHONE_NUMBER_REGION,context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION))
                    .setAttribute(ATTRIBUTE_OLD_PHONE_NUMBER, context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER))
                    .createForm(UPDATE_PHONE_NUMBER_FTL);
            context.challenge(challenge);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
            String expiryTime = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
            if(!GeneralHelper.sendSmsOtp(context, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE), expiryTime,GeneralHelper.getConfigValue(context,SMS_SENDER))){
                showView(context,ErrorMappings.FAILED_TO_SEND_SMS_OTP,OTP_VERIFICATION_FTL);
                return;
            }
            Response challenge = context.form()
                    .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE))
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY))
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_PHONE_REGISTRATION))) {
            String region     = formData.getFirst(PHONE_NUMBER_REGION);
            String phoneNum   = formData.get(FORM_PHONE_NUMBER).get(1);
            NumberFormat numberFormat = GeneralHelper.parseMobileNumber(context,region.toUpperCase(),phoneNum);
            if(numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")){
                showView(context, ErrorMappings.PHONE_NUMBER_INVALID, UPDATE_PHONE_NUMBER_FTL);
                return;
            }

            String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length()+1);
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                showView(context, ErrorMappings.PHONE_NUMBER_INVALID, UPDATE_PHONE_NUMBER_FTL);
                return;
            }
            String mobNum = region + phoneNumber;
            UserModel existingUserWithMobile = context.getSession().users().getUserByMobile(mobNum, context.getRealm());

            if(!context.getRealm().isDuplicateMobNumAllowed() && existingUserWithMobile != null){
                    showView(context, ErrorMappings.PHONE_ALREADY_REGISTERED, UPDATE_PHONE_NUMBER_FTL);
                    return;
            }

            String expiryTime = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
            if(!GeneralHelper.sendSmsOtp(context, numberFormat.getE16Format(),  expiryTime, GeneralHelper.getConfigValue(context, SMS_SENDER))){
                showView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP, UPDATE_PHONE_NUMBER_FTL);
                return;
            }

            context.getAuthenticationSession().setAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE, numberFormat.getE16Format());
            context.getAuthenticationSession().setAuthNote(PHONE_NUMBER_REGION, formData.getFirst(PHONE_NUMBER_REGION));
            context.getAuthenticationSession().setAuthNote(ATTRIBUTE_PHONE_NUMBER, phoneNumber);

            Response challenge = context.form()
                    .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, numberFormat.getE16Format())
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY))
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_OTP_VERIFICATION))) {
            String mobNum = context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION) + context.getAuthenticationSession().getAuthNote(FORM_PHONE_NUMBER);
            UserModel existingUserWithMobile = context.getSession().users().getUserByMobile(mobNum, context.getRealm());

            if(!context.getRealm().isDuplicateMobNumAllowed() && existingUserWithMobile != null){
                    showView(context, ErrorMappings.PHONE_ALREADY_REGISTERED, PHONE_NUMBER_REGISTRATION_FTL);
                    return;
            }

            String otpExpiryTime = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY);

            if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
                Response challenge = context.form()
                        .addError(new FormMessage(ErrorMappings.OTP_EXPIRED))
                        .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE))
                        .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                        .createForm(OTP_VERIFICATION_FTL);
                context.challenge(challenge);
                return;
            }

            if (formData.getFirst(FORM_OTP) == null || formData.getFirst(FORM_OTP).length() == 0) {
                Response challenge = context.form()
                        .addError(new FormMessage(ErrorMappings.ENTER_OTP))
                        .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE))
                        .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                        .createForm(OTP_VERIFICATION_FTL);
                context.challenge(challenge);
                return;
            }

            String otp = context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP);

            if (!formData.getFirst(FORM_OTP).equals(otp)) {
                Response challenge = context.form()
                        .addError(new FormMessage(ErrorMappings.WRONG_OTP_ERROR))
                        .setAttribute(PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_WITH_COUNTRY_CODE))
                        .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                        .createForm(OTP_VERIFICATION_FTL);
                context.challenge(challenge);
                return;
            }
            if(context.getUser() == null) {
                UserModel user = context.getSession().users().addUser(context.getRealm(), context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER));
                user.setEnabled(true);

            }
            context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER_REGION,context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION));
            context.getUser().setSingleAttribute(UserModel.MOBILE_NUMBER,context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER));
            context.getUser().setSingleAttribute(ATTRIBUTE_PHONE_VERIFIED, "TRUE");
            context.success();
        }
    }

    private void showView(RequiredActionContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        formsProvider.setAttribute("defReg", context.getRealm().getDefaultRegion());
        formsProvider.setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_MOBILE_OTP_EXPIRY));
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
        return "update-phone-number";
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.SMS_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.SMS_SENDER);
        provider.setLabel(Constants.SMS_SENDER_LABEL);

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
                provider.setHelpText("No SMS Sender are registered. Click <a href=' #/realms/" + session.getContext().getRealm().getName() + "/sms/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All SMSes are routed through default SMS Provider from "+ org.keycloak.Config.getAdminRealm() + ".If you want to use your SMS providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
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

    public String[] supportedModes() {
        return new String[] {"ciam","wiam"};
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {
        ProviderConfigProperty configProperty;
        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.VERIFICATION_EXPIRY_CONFIG);
        configProperty.setLabel("phone-no-expiry-duration");
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        configProperty.setHelpText("phone-no-expiry-duration-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.PHONE_VER_OTP_LENGTH);
        configProperty.setLabel(Constants.PHONE_VER_OTP_LENGTH_LABEL);
        configProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        configProperty.setHelpText("phone-ver-otp-length-helptext");
        configProperty.setMinVal(4);
        configProperty.setMaxVal(8);
        configProperty.setDefaultValue(6);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.PHONE_VER_OTP_TXT);
        configProperty.setLabel("phone-num-template-text");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("phone-num-template-text-helptext");
        configProperties.add(configProperty);
    }


}
