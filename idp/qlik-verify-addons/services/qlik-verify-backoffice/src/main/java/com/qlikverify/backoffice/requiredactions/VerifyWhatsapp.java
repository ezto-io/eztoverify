package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.ciba.models.UserData;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;
import static com.qlikverify.services.helpers.GeneralHelper.getExpiryDuration;

public class VerifyWhatsapp implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }
        String requestId = context.getRequestId();
        boolean whatsapp_no = context.getRealm().hasTrxValueAttribute(requestId,WHATSAPP_NUMBER);
        boolean whatsapp_no_verified = context.getRealm().hasTrxValueAttribute(requestId,WHATSAPP_NUMBER_VERIFIED);
        boolean whatsapp_no_verified_at = context.getRealm().hasTrxValueAttribute(requestId,WHATSAPP_NUMBER_VERIFIED_AT);
        boolean region = context.getRealm().hasTrxValueAttribute(requestId,WHATSAPP_NUMBER_REGION);
        boolean hasSkipped = Boolean.parseBoolean(context.getRealm().getTrxValue(requestId,  VERIFY_WHATSAPP_SKIPPED));
        if(result && !hasSkipped && (!whatsapp_no || !whatsapp_no_verified  || !whatsapp_no_verified_at || !region)) {
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
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());

        if(context.getUser().getMobileNumber() != null && context.getUser().getMobileNumberRegion() != null) {
            formIsPhoneReg(context, null, false);
        } else {
            loginFormsProvider.setAttribute(DEFREG, context.getRealm().getDefaultRegion())
                    .setAttribute(PHONE_NUMBER_REGION, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION))
                    .setAttribute(ATTRIBUTE_OLD_PHONE_NUMBER, context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER))
                    .setAttribute(WHATSAPP,"true");

            context.challenge(loginFormsProvider.createForm(BO_VERIFY_PHONE_NUMBER_FTL));
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {

        if(context.getUser() == null){
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.USER_NOT_ONBOARDED);
            showView(context, "user_not_onboarded", BACKOFFICE_ERROR_FTL);
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if(formData.containsKey("cancel") && Boolean.parseBoolean(formData.getFirst("cancel"))) {
            com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR);
            return;
        }

        if(formData.containsKey("skipped") && Boolean.parseBoolean(formData.getFirst("skipped"))) {
            context.getRealm().addAttribute(context.getRequestId(),  VERIFY_WHATSAPP_SKIPPED, "true");
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION, "", skipDetails);
            context.success();
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_EDIT))) {
            Response challenge = context.form()
                    .setAttribute(DEFREG, context.getRealm().getDefaultRegion())
                    .setAttribute(PHONE_NUMBER_REGION, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION))
                    .setAttribute(ATTRIBUTE_OLD_PHONE_NUMBER, context.getAuthenticationSession().getAuthNote(ATTRIBUTE_PHONE_NUMBER))
                    .setAttribute(WHATSAPP,"true")
                    .setAttribute(SKIPPABLE, context.getModel().isSkippable())
                    .createForm(BO_VERIFY_PHONE_NUMBER_FTL);
            context.challenge(challenge);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
            formResend(context, formData.getFirst("editUserDetail"));
            return;
        }

        if(Boolean.parseBoolean(formData.getFirst(FORM_IS_OTP_VERIFICATION))){
            boolean otpVerification = processOTP(context, formData);

            if(otpVerification){
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION, "");
                setDefaultClaims(context);
                context.success();
            }
        }
        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_PHONE_REGISTRATION))) {
            formIsPhoneReg(context, formData, true);
        }
    }

    private void formIsPhoneReg(RequiredActionContext context, MultivaluedMap<String, String> formData, boolean editUserDetail){
        String region = null;
        String phoneNum = null;
        String countryCode = null;
        if(formData != null) {
            region = formData.getFirst(PHONE_NUMBER_REGION);
            phoneNum = formData.get(FORM_PHONE_NUMBER).get(1);
            countryCode = formData.getFirst("countryCode");

        } else {
            region = context.getUser().getMobileNumberRegion();
            phoneNum = context.getUser().getMobileNumber();
        }

        NumberFormat numberFormat = com.qlikverify.backoffice.GeneralHelper.parseMobileNumber(context, region.toUpperCase(), phoneNum);

//        context.getAuthenticationSession().setAuthNote("countryCode", formData.getFirst("countryCode"));
        context.getAuthenticationSession().setAuthNote(PHONE_NUMBER_REGION, region);
        context.getAuthenticationSession().setAuthNote(PHONE_NUMBER, phoneNum);
        context.getAuthenticationSession().setAuthNote(WHATSAPP_NUM_WITH_COUNTRY_CODE, numberFormat.getE16Format());


        if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.PHONE_NUMBER_INVALID);
            showView(context, ErrorMappings.PHONE_NUMBER_INVALID, BO_VERIFY_PHONE_NUMBER_FTL);
            return;
        }

        String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length() + 1);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.PHONE_NUMBER_INVALID);
            showView(context, ErrorMappings.PHONE_NUMBER_INVALID, BO_VERIFY_PHONE_NUMBER_FTL);
            return;
        }

        String mobNum = region + phoneNumber;
        UserModel user = context.getSession().users().getUserByMobile(mobNum, context.getRealm());

        if(user != null && context.getUser() != null && !context.getUser().getId().equals(user.getId())){
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.PHONE_ALREADY_REGISTERED);
            showView(context, ErrorMappings.PHONE_ALREADY_REGISTERED, BO_VERIFY_PHONE_NUMBER_FTL);
            return;
        }

//        String userDataRaw = context.getAuthenticationSession().getAuthNote("user");
//
//        if(!checkForUser(userDataRaw, context, numberFormat, phoneNumber)){
//            return;
//        }

        boolean smsStatus = sendSms(context);
        String otp_len = GeneralHelper.getConfigValue(context, WHATSAPP_VERIFY_OTP_LENGTH);

        if(smsStatus){
            String phoneNo = context.getUser().getMobileNumber() != null ? org.keycloak.authentication.authenticators.utils.GeneralHelper.maskPhoneNumber(context, numberFormat.getE16Format(), region) : numberFormat.getE16Format();

            Response challenge = context.form()
                    .setAttribute(WHATSAPP_NO, phoneNo)
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(WHATSAPP_OTP_EXP_TIME))
                    .setAttribute("editUserDetail", String.valueOf(editUserDetail))
                    .setAttribute("mode", context.getRealm().getMode())
                    .setAttribute(OTP_LENGTH_ATTR, otp_len)
                    .createForm(OTP_VERIFICATION_FTL);

            context.challenge(challenge);
        }
        else{
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.FAILED_TO_SEND_WHATSAPP_OTP);
            showView(context, ErrorMappings.FAILED_TO_SEND_WHATSAPP_OTP, BO_VERIFY_PHONE_NUMBER_FTL);
        }
    }

    private boolean checkForUser(String userDataRaw, RequiredActionContext context, NumberFormat numberFormat, String phoneNumber){
        UserData userData = null;
        try {
            userData = UserData.decode(userDataRaw, context.getAuthenticationSession().getClient());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(userData.getPhone() != null && userData.getPhoneRegion() != null && !userData.getPhone().isEmpty() && !userData.getPhoneRegion().isEmpty()) {
            NumberFormat userProvidedNumberFormat = com.qlikverify.backoffice.GeneralHelper.parseMobileNumber(context, userData.getPhoneRegion().toUpperCase(), userData.getPhone());
            if (userProvidedNumberFormat.getE16Format() != null && !userProvidedNumberFormat.getE16Format().equalsIgnoreCase("invalid")) {
                String userProvidedNumber = userProvidedNumberFormat.getE16Format().substring(numberFormat.getCountryCode().length() + 1);
                if(!userProvidedNumber.equals(phoneNumber))
                {
                    com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.PHONE_NUMBER_INVALID);
                    showView(context, ErrorMappings.PHONE_NUMBER_INVALID, BO_VERIFY_PHONE_NUMBER_FTL);
                    return false;
                }
            }
        }
        return true;
    }


    private void formResend(RequiredActionContext context, String editUserDetail){
        boolean smsStatus = sendSms(context);
        String otp_len = GeneralHelper.getConfigValue(context, WHATSAPP_VERIFY_OTP_LENGTH);

        if(smsStatus){
            Response challenge = context.form()
                    .setAttribute(WHATSAPP_NO, context.getAuthenticationSession().getAuthNote(WHATSAPP_NUM_WITH_COUNTRY_CODE))
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(WHATSAPP_OTP_EXP_TIME))
                    .setAttribute("editUserDetail", String.valueOf(editUserDetail))
                    .setAttribute("mode", context.getRealm().getMode())
                    .setAttribute(OTP_LENGTH_ATTR, otp_len)
                    .createForm(OTP_VERIFICATION_FTL);

            context.challenge(challenge);
        }
        else{
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.FAILED_TO_SEND_WHATSAPP_OTP);
            showView(context, ErrorMappings.FAILED_TO_SEND_WHATSAPP_OTP, BO_VERIFY_PHONE_NUMBER_FTL);
        }
    }

    private boolean sendSms(RequiredActionContext context){
        // Set whatsapp as smsprovider
        SmsSenderProvider smsSenderProvider = getSmsProvider(context);
        if (context.getRealm().isEnabledOwnWhatsappProvider()) {
            smsSenderProvider.setProviderId(WHATSAPP);
        } else {
            SmsConfigModel whatsappProvider = GeneralHelper.getDefaultWhatsappSender(context.getSession());
            if(whatsappProvider != null){
                smsSenderProvider.setProviderId(whatsappProvider.getSmsProviderId());
            } else {
                return false;
            }
        }

        String otp_len = GeneralHelper.getConfigValue(context, WHATSAPP_VERIFY_OTP_LENGTH);
        if(otp_len == null || otp_len.isEmpty()){
            otp_len = "6";
        }
        String otp = com.qlikverify.services.helpers.GeneralHelper.getOtp(Integer.parseInt(otp_len));
        String expiryTime = GeneralHelper.getConfigValue(context, WHATSAPP_VERIFY_EXPIRY_TIME);
        if (expiryTime == null || expiryTime.isEmpty()) {
            expiryTime = "5m";
        }

        context.getAuthenticationSession().setAuthNote(WHATSAPP_OTP_EXP_TIME, String.valueOf(getExpiryDuration(expiryTime)));

        context.getAuthenticationSession().setAuthNote(WHATSAPP_OTP, otp);


        Map<String, Object> smsPayload = new HashMap<>();
        List<String> datas = new ArrayList<>();
        datas.add(otp);
        smsPayload.put("templateType", "OTP");
        smsPayload.put("messageParts", datas.toArray());
        String twilioOtpTemplateSid = context.getRealm().isEnabledOwnWhatsappProvider() ? context.getRealm().getAttribute("twilio-otp-template-sid") : context.getSession().realms().getRealmByName(Config.getAdminRealm()).getAttribute("twilio-otp-template-sid");
        smsPayload.put("messageContentSid", twilioOtpTemplateSid);


        try {
            smsSenderProvider.sendSMS(getPhoneNumber(context), null, smsPayload);
            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(),VERIFY_WHATSAPP);
        } catch (SmsException e) {
            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(),VERIFY_WHATSAPP);
            showView(context,"",BO_VERIFY_PHONE_NUMBER_FTL);
            return false;
        }
        return true;
    }

    private void setDefaultClaims(RequiredActionContext context){
        String requestId = context.getRequestId();
        context.getRealm().addAttribute(requestId,WHATSAPP_NUMBER,context.getAuthenticationSession().getAuthNote("phoneNumber"));
        context.getRealm().addAttribute(requestId,WHATSAPP_NUMBER_VERIFIED, "true");
        context.getRealm().addAttribute(requestId,WHATSAPP_NUMBER_VERIFIED_AT, String.valueOf(Time.currentTime()));
        context.getRealm().addAttribute(requestId,WHATSAPP_NUMBER_REGION, context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION));
    }

    private boolean processOTP(RequiredActionContext context, MultivaluedMap<String, String> formData){
        String otpExpiryTime = context.getAuthenticationSession().getAuthNote(WHATSAPP_OTP_EXP_TIME);
        String otp_len = GeneralHelper.getConfigValue(context, WHATSAPP_VERIFY_OTP_LENGTH);
        context.form().setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime);

        if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.OTP_EXPIRED);
            Response challenge = context.form()
                    .setAttribute(ERROR, ErrorMappings.OTP_EXPIRED)
                    .addError(new FormMessage(ErrorMappings.OTP_EXPIRED))
                    .setAttribute(WHATSAPP_NO, context.getAuthenticationSession().getAuthNote(WHATSAPP_NUM_WITH_COUNTRY_CODE))
                    .setAttribute("mode", context.getRealm().getMode())
                    .setAttribute(OTP_LENGTH_ATTR, otp_len)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return false;
        }

        if (formData.getFirst(FORM_OTP) == null || formData.getFirst(FORM_OTP).isEmpty()) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.ENTER_OTP);
            Response challenge = context.form()
                    .setAttribute(ERROR, ErrorMappings.ENTER_OTP)
                    .addError(new FormMessage(ErrorMappings.ENTER_OTP))
                    .setAttribute(WHATSAPP_NO, context.getAuthenticationSession().getAuthNote(WHATSAPP_NUM_WITH_COUNTRY_CODE))
                    .setAttribute("mode", context.getRealm().getMode())
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                    .setAttribute(OTP_LENGTH_ATTR, otp_len)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return false;
        }

        String otp = context.getAuthenticationSession().getAuthNote(WHATSAPP_OTP);

        if (!formData.getFirst(FORM_OTP).equals(otp)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR, ErrorMappings.WRONG_OTP_ERROR);
            Response challenge = context.form()
                    .setAttribute(ERROR, ErrorMappings.WRONG_OTP_ERROR)
                    .addError(new FormMessage(ErrorMappings.WRONG_OTP_ERROR))
                    .setAttribute(WHATSAPP_NO, context.getAuthenticationSession().getAuthNote(WHATSAPP_NUM_WITH_COUNTRY_CODE))
                    .setAttribute("mode", context.getRealm().getMode())
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                    .setAttribute(OTP_LENGTH_ATTR, otp_len)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return false;
        }

        return true;
    }

    @Override
    public String getDisplayText() {
        return "bo-verify-whatsapp";
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
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
        return "bo-verify-whatsapp";
    }

    private void showView(RequiredActionContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        formsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
        if (error != null) {
            formsProvider.setAttribute(ERROR, error);
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        formsProvider.setAttribute(DEFREG, context.getRealm().getDefaultRegion());
        formsProvider.setAttribute(WHATSAPP, "true");
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(com.qlikverify.services.constants.Constants.WHATSAPP_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(com.qlikverify.services.constants.Constants.WHATSAPP_SENDER);
        provider.setLabel(com.qlikverify.services.constants.Constants.WHATSAPP_SENDER_LABEL);

        if(session.getContext().getRealm().isEnabledOwnWhatsappProvider()){
            Optional<SmsConfigModel> hasWhatsAppProvider = session.getContext().getRealm().getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();

            if (hasWhatsAppProvider.isPresent()) {
                configProperties.removeIf(x -> x.getName() != null && x.getName().equals(com.qlikverify.services.constants.Constants.WHATSAPP_SENDER));
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("");
                provider.setHelperLinkText("no-whatsapp-reg");
                provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/sms/providers/create/whatsapp");
                if (!providerConfigProperty.isPresent()) {
                    configProperties.add(provider);
                }
            }
        } else {
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("");
            provider.setHelperLinkText("all-whatsapp-msg-routed-root-workspace");
            provider.setHelperLink("/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription");
            if (!providerConfigProperty.isPresent()) {
                configProperties.add(provider);
            }
        }
        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {
        ProviderConfigProperty configProperty;
        configProperty = new ProviderConfigProperty();
        configProperty.setName(WHATSAPP_VERIFY_EXPIRY_TIME);
        configProperty.setLabel("whatsapp-no-expiry-duration");
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        configProperty.setHelpText("whatsapp-no-expiry-duration-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(WHATSAPP_VERIFY_OTP_LENGTH);
        configProperty.setLabel("whatsapp-ver-otp-len");
        configProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        configProperty.setHelpText("whatsapp-ver-otp-length-helptext");
        configProperty.setMinVal(4);
        configProperty.setMaxVal(8);
        configProperties.add(configProperty);

    }

    private SmsSenderProvider getSmsProvider(RequiredActionContext context) {
        return context.getSession().getProvider(SmsSenderProvider.class, "default");
    }

    private String getPhoneNumber(RequiredActionContext context) throws SmsException {
        NumberFormat numberFormat;

        numberFormat =
                    getSmsProvider(context).parseNumber(context.getAuthenticationSession().getAuthNote("phoneNumber"),context.getAuthenticationSession().getAuthNote(PHONE_NUMBER_REGION).toUpperCase());
        return numberFormat.getE16Format();
    }

    @Override
    public String[] stripeMappingId() {
        return new String[] {"verify_whatsapp"};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(WHATSAPP_NUMBER, ValidationTypes.NUMBER.getValue());
        defaultClaims.put(WHATSAPP_NUMBER_REGION, ValidationTypes.STRING.getValue());
        defaultClaims.put(WHATSAPP_NUMBER_VERIFIED, ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(WHATSAPP_NUMBER_VERIFIED_AT, ValidationTypes.DATE_AND_TIME.getValue());
        defaultClaims.put(VERIFY_WHATSAPP_SKIPPED, ValidationTypes.BOOLEAN.getValue());

        return defaultClaims;
    }
}
