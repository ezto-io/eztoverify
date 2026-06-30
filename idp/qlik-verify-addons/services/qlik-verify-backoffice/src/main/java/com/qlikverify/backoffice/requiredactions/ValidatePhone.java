package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.helpers.phoneValidationHelper.PhoneValidationHelper;
import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.IncomingPhoneNumber;
import com.twilio.type.PhoneNumber;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.sms.utility.RetryStartegyHelper;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.ErrorMappings.INVALID_PHONE_NUMBER;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;
import static org.keycloak.provider.ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE;

public class ValidatePhone implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final Logger logger = Logger.getLogger(ValidatePhone.class);
    KeycloakSession session;
    private static final OkHttpClient client = new OkHttpClient();

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "validate-phone-no";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        String prefix = getId().replace("bo-", "").replace("-", "_") + "@";

        configProperties.stream().filter(x -> x.getName().contains("claims"))
                .forEach(providerConfigProperty -> {
                            providerConfigProperty.setDefaultValue(getDefaultClaims().keySet().stream().map(y->prefix + y).collect(Collectors.joining("##")));
                            providerConfigProperty.setOptions(getPhoneNoValidationPotentialClaims().keySet().stream().map(y->prefix + y).collect(Collectors.toList()));
                        }
                );

        Optional<ProviderConfigProperty> providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("call_sender")).findFirst();
        ProviderConfigProperty provider;
        provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);
        provider.setName("call_sender");
        provider.setLabel("call-sender");
        provider.setDependsOn(verificationTypeMissedCallValidationDepMap);
        provider.setValidationType(VALIDATION_ANY_ONE_TRUE);

        if (session.getContext().getRealm().isEnabledOwnSmsProvider() && session.getContext().getRealm().getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("twilio")).count() > 0) {
            SmsConfigModel twilioConfig = session.getContext().getRealm().getSmsConfigByProviderId("twilio");
            List<String> options = new ArrayList<>();
            options.add(twilioConfig.getSmsProviderId());
            if (isCallSupported(twilioConfig)) {
                provider.setRequired(false);
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(options);
                provider.setDefaultValue("twilio");
                provider.setHelpText("call-sender-hlp-txt");
                provider.setHelperLinkText("");
                provider.setHelperLink("");
                if (providerConfigProperty.isEmpty()) {
                    configProperties.add(provider);
                }
                return configProperties;
            }
        }
        provider.setRequired(false);
        provider.setValidationType(VALIDATION_ANY_ONE_TRUE);
        provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        provider.setDefaultValue("");
        provider.setHelpText("");
        provider.setHelperLinkText("all-calls-routed-root-workspace");
        provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/realm-subscription");
        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    private static HashMap<String, Object> verificationTypePhoneNoValidationDepMap = new HashMap<>();
    private static HashMap<String, Object> verificationTypeMissedCallValidationDepMap = new HashMap<>();

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty property;

        ArrayList<String> verificationOptions = new ArrayList<String>();
        verificationOptions.add(NUMBER_VALIDATION);
        verificationOptions.add(MISSED_CALL);

        verificationTypePhoneNoValidationDepMap.put(VERIFICATION_TYPE, NUMBER_VALIDATION);
        verificationTypeMissedCallValidationDepMap.put(VERIFICATION_TYPE, MISSED_CALL);

        property = new ProviderConfigProperty();
        property.setName(Constants.VERIFICATION_TYPE);
        property.setLabel(Constants.VERIFICATION_TYPE);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(verificationOptions);
        property.setDefaultValue(NUMBER_VALIDATION);
        property.setRequired(false);
        property.setHelpText("verification-type-help-text");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("phone-no-valid-claims");
        property.setLabel(PHONE_NO_VALIDATION_CLAIMS);
        property.setType(ProviderConfigProperty.MULTIVALUED_LIST_TYPE);
        property.setHelpText("claims-hlp-txt");
//        property.setDefaultValue(getDefaultClaims().keySet().stream().collect(Collectors.joining("##")));
//        property.setOptions(new ArrayList<>(getPhoneNoValidationPotentialClaims().keySet()));
        property.setDependsOn(verificationTypePhoneNoValidationDepMap);
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
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context,
                    context.getPolicyId(), context.getUser());
        }

        String requestId = context.getRequestId();
        boolean phone = context.getRealm().hasTrxValueAttribute(requestId, VALIDATE_PHONE_PHONE_CLAIM);
        boolean phoneVerified = context.getRealm().hasTrxValueAttribute(requestId,
                 VALIDATE_PHONE_PHONE_VALID_VERIFIED_CLAIM);
        boolean phoneVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId,
                 VALIDATE_PHONE_PHONE_VALID_VERIFIED_AT_CLAIM);
        boolean hasSkipped = Boolean.parseBoolean(context.getRealm().getTrxValue(requestId,
                 VALIDATE_PHONE_NO_SKIPPED));
        if (result && !hasSkipped && (!phone || !phoneVerified || !phoneVerifiedAt)) {
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
        String verificationWay = context.getConfig() != null && !context.getConfig().isEmpty() ?
                context.getConfig().get(Constants.VERIFICATION_TYPE) : NUMBER_VALIDATION;
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        context.getAuthenticationSession().setAuthNote(RETRY_MISSED_CALL_VERIFICATION, "1");

        try {
            if (context.getUser().getMobileNumber() != null && context.getUser().getMobileNumberRegion() != null) {
                if (verificationWay.equalsIgnoreCase(MISSED_CALL)) {
                    String phoneNoWithCountryCode =
                            com.qlikverify.backoffice.GeneralHelper.phoneNoWithCountryCode(context.getUser());
                    startMissedCallVerification(context, phoneNoWithCountryCode, false);
                } else {
                    processPhoneDetails(context,
                            com.qlikverify.backoffice.GeneralHelper.phoneNoWithCountryCode(context.getUser()));
                }
            } else {
                loginFormsProvider.setAttribute("title", "validate-phone");
                loginFormsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
                context.challenge(loginFormsProvider.createForm(BO_VERIFY_PHONE_NUMBER_FTL));
            }
        } catch (TwilioException e) {
            logger.error(e.getMessage());
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context,
                    EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, e.getMessage());
            context.failure();
        }
    }

    private void showMissedCallVerificationftl(RequiredActionContext context, String callSid,
                                               String phoneNoWithCountryCode, boolean isRetry) {
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider
                .setAuthenticationSession(context.getAuthenticationSession());
        Map<String, String> phoneNoDetails =
                com.qlikverify.backoffice.GeneralHelper.parseMobileNumber(phoneNoWithCountryCode);
        loginFormsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
        loginFormsProvider.setAttribute("title", "missed-call");
        loginFormsProvider.setAttribute("verification_type", "missed_call");
        loginFormsProvider.setAttribute("isPhoneReg", false);
        loginFormsProvider.setAttribute("callSid", callSid);
        loginFormsProvider.setAttribute("phoneNoWithCountryCode", phoneNoWithCountryCode);
        loginFormsProvider.setAttribute("phoneNumber", phoneNoDetails.get("phoneNo"));
        loginFormsProvider.setAttribute("phoneNumberRegion", phoneNoDetails.get("phoneNoReg"));
        loginFormsProvider.setAttribute("isRetry", isRetry);
        context.challenge(loginFormsProvider.createForm(BO_VERIFY_PHONE_NUMBER_FTL));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("cancel") && Boolean.parseBoolean(formData.getFirst("cancel"))) {
            GeneralHelper.handleCancel(context, EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR);
            return;
        }

        if (formData.containsKey("skipped") && Boolean.parseBoolean(formData.getFirst("skipped"))) {
            context.getRealm().addAttribute(context.getRequestId(),  VALIDATE_PHONE_NO_SKIPPED, "true");
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_PHONE_NUMBER_VALIDATION,
                    "", skipDetails);
            context.success();
            return;
        }

        String verificationType = context.getConfig() != null && !context.getConfig().isEmpty() ?
                context.getConfig().get(VERIFICATION_TYPE) : NUMBER_VALIDATION;

        String phoneNo = formData.getFirst("phoneNumber");
        String phoneNumberRegion = formData.getFirst("phoneNumberRegion");

        NumberFormat numberFormat = com.qlikverify.backoffice.GeneralHelper.parseMobileNumber(context,
                phoneNumberRegion, phoneNo);

        if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context,
                    EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, ErrorMappings.PHONE_NUMBER_INVALID);
            Map<String, String> validatePhoneNumberAttributes = new HashMap<>();
            validatePhoneNumberAttributes.put("title", "validate-phone");
            showView(context, ErrorMappings.PHONE_NUMBER_INVALID, BO_VERIFY_PHONE_NUMBER_FTL,
                    validatePhoneNumberAttributes);
            return;
        }
        UserModel user =
                context.getSession().users().getUserByMobile(numberFormat.getRegion().toLowerCase() + numberFormat.getE16Format().substring(numberFormat.getCountryCode().length() + 1), context.getRealm());
        if (user != null && context.getUser() != null && !context.getUser().getId().equals(user.getId())) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context,
                    EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, ErrorMappings.PHONE_ALREADY_REGISTERED);
            Map<String, String> validatePhoneNumberAttributes = new HashMap<>();
            validatePhoneNumberAttributes.put("title", "validate-phone");
            showView(context, ErrorMappings.PHONE_ALREADY_REGISTERED, BO_VERIFY_PHONE_NUMBER_FTL,
                    validatePhoneNumberAttributes);
            return;
        }

        try {
            if (verificationType.equalsIgnoreCase(MISSED_CALL)) {
                if (formData.containsKey("callSid")) {
                    proccessFormissedCallVerification(context, formData);
                } else {
                    startMissedCallVerification(context, phoneNo, false);
                }
            } else {
                processPhoneDetails(context, phoneNo);
            }
        } catch (IllegalArgumentException e) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context,
                    EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, e.getMessage());
            context.failure();
        } catch (TwilioException e) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context,
                    EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, e.getMessage());
            context.failure();
        } catch (Exception e) {
            e.printStackTrace();
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context,
                    EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, e.getMessage());
            context.failure();
        }
    }

    private void proccessFormissedCallVerification(RequiredActionContext context,
                                                   MultivaluedMap<String, String> formData) {
        String phoneNumberWithCountryCode = formData.getFirst("phoneNumberWithCountryCode");
        if (formData.containsKey("retry") && Boolean.parseBoolean(formData.getFirst("retry"))) {
            startMissedCallVerification(context, phoneNumberWithCountryCode, false);
            return;
        }
        List<String> callStatus = retry(context, formData.getFirst("callSid"));
        logger.info(String.format("ValidatePhone :: processAction :: callStatus :: %s :: %s :: %s",
                context.getRequestId(), phoneNumberWithCountryCode, callStatus));

        if (callStatus.contains("failed")) {
            GeneralHelper.auditEvent(context, EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, "Phone number validation " +
                    "failed " + fetchCallDetails(context, formData.getFirst("callSid")));
            context.failure();
        } else if (callStatus.stream().filter(x -> x.equalsIgnoreCase("queued")).count() == callStatus.size()) {
            Integer retryMissedCallVerification =
                    Integer.valueOf(context.getAuthenticationSession().getAuthNote(RETRY_MISSED_CALL_VERIFICATION));
            context.getAuthenticationSession().setAuthNote(RETRY_MISSED_CALL_VERIFICATION,
                    String.valueOf(retryMissedCallVerification + 1));
            showMissedCallVerificationftl(context, "", phoneNumberWithCountryCode, true);
        } else {
            setPhoneNoToUser(context, phoneNumberWithCountryCode);
            context.getRealm().addAttribute(context.getRequestId(),  VALIDATE_PHONE_PHONE_CLAIM,
                    context.getUser().getMobileNumber());
            context.getRealm().addAttribute(context.getRequestId(),  VALIDATE_PHONE_PHONE_VALID_VERIFIED_CLAIM
                    , "true");
            context.getRealm().addAttribute(context.getRequestId(),
                     VALIDATE_PHONE_PHONE_VALID_VERIFIED_AT_CLAIM, String.valueOf(Time.currentTime()));
            GeneralHelper.auditEvent(context, EventType.EZ_PHONE_NUMBER_VALIDATION, "");
            context.success();
        }
    }

    private void processPhoneDetails(RequiredActionContext context, String phoneNo) {
        try {
            Map<String, Object> response = PhoneValidationHelper.getPhoneNoDetails(context, phoneNo);
            logger.info(response);

            if (response == null) {
                GeneralHelper.auditEvent(context, EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR,
                        ErrorMappings.PHONE_VALIDATION_REACH_ERROR);
                Map<String, String> validatePhoneNumberAttributes = new HashMap<>();
                validatePhoneNumberAttributes.put("title", "validate-phone");
                showView(context, ErrorMappings.PHONE_VALIDATION_REACH_ERROR, BO_VERIFY_PHONE_NUMBER_FTL,
                        validatePhoneNumberAttributes);
                return;
            }

            if (response.containsKey("error")) {
                GeneralHelper.auditEvent(context, EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR,
                        response.get("error").toString());
                showView(context, ErrorMappings.CONTACT_ADMIN, BACKOFFICE_ERROR_FTL, null);
                return;
            }

            if (response.containsKey("valid") && Boolean.parseBoolean(response.get("valid").toString())) {
                incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(),
                        VALIDATE_PHONE_NUMBER);
                if (response.get("country_code") != null) {
                    context.getUser().setMobileNumberRegion(response.get("country_code").toString().toLowerCase());
                }

                NumberFormat numberFormat = com.qlikverify.backoffice.GeneralHelper.parseMobileNumber(context,
                        response.get("country_code").toString().toUpperCase(),
                        response.get("international_format").toString());

                if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
                    com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context,
                            EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, ErrorMappings.PHONE_NUMBER_INVALID);
                    Map<String, String> validatePhoneNumberAttributes = new HashMap<>();
                    validatePhoneNumberAttributes.put("title", "validate-phone");
                    showView(context, ErrorMappings.PHONE_NUMBER_INVALID, BO_VERIFY_PHONE_NUMBER_FTL,
                            validatePhoneNumberAttributes);
                    return;
                }
                String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length() + 1);

                context.getUser().setMobileNumber(phoneNumber);
                GeneralHelper.auditEvent(context, EventType.EZ_PHONE_NUMBER_VALIDATION, "");
                setClaims(context, response, phoneNo);
                context.success();
            } else {
                GeneralHelper.auditEvent(context, EventType.EZ_PHONE_NUMBER_VALIDATION_ERROR, INVALID_PHONE_NUMBER);
                incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(),
                        VALIDATE_PHONE_NUMBER);
                Map<String, String> validatePhoneNumberAttributes = new HashMap<>();
                validatePhoneNumberAttributes.put("title", "validate-phone");
                showView(context, ErrorMappings.INVALID_PHONE_NUMBER, BO_VERIFY_PHONE_NUMBER_FTL,
                        validatePhoneNumberAttributes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> retry(RequiredActionContext context, String callSid) {
        Integer retryMissedCallVerification =
                Integer.valueOf(context.getAuthenticationSession().getAuthNote(RETRY_MISSED_CALL_VERIFICATION));
        RetryStartegyHelper retryStartegyHelper = new RetryStartegyHelper(retryMissedCallVerification * 5, 500);

        List<String> callStatus = new ArrayList<>();
        Map<String, String> twilioCredentials = getTwilioCredentials(context);

        while (retryStartegyHelper.shouldRetry()) {
            callStatus.add(getCallStatus(context, callSid, twilioCredentials.get(ACCOUNT_SID),
                    twilioCredentials.get(AUTH_TOKEN)));
            try {
                retryStartegyHelper.errorOccured();
            } catch (Exception e) {
                retryStartegyHelper.stopRetry();
            }
        }
        Call.updater(callSid).setStatus(Call.UpdateStatus.COMPLETED).update();
        return callStatus;
    }

    private String startCall(RequiredActionContext context, String phoneNoWithCountryCode) {
        Map<String, String> twilioCredentials = getTwilioCredentials(context);

        Twilio.init(twilioCredentials.get(ACCOUNT_SID), twilioCredentials.get(AUTH_TOKEN));
        Call call = Call.creator(
                        new com.twilio.type.PhoneNumber(phoneNoWithCountryCode),
                        new com.twilio.type.PhoneNumber(twilioCredentials.get("from")),
                        new com.twilio.type.Twiml(
                                "<Response></Response>"))
                .create();
        return call.getSid();
    }

    private String getCallStatus(RequiredActionContext context, String callSid, String accountSid, String authToken) {
        Call call = fetchCallDetails(context, callSid);

        Request request = new Request.Builder()
                .url("https://api.twilio.com" + call.getUri())
                .addHeader("Authorization", Credentials.basic(accountSid, authToken))
                .build();

        HashMap<String, Object> twilioResponseMap = new HashMap<>();

        try {
            Response twilioResponse = client.newCall(request).execute();
            ObjectMapper objectMapper = new ObjectMapper();
            twilioResponseMap = objectMapper.readValue(twilioResponse.body().string(), HashMap.class);
            twilioResponse.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return twilioResponseMap.get("status").toString();
    }

    private Call fetchCallDetails(RequiredActionContext context, String callSid) {
        Map<String, String> twilioConfig = getTwilioCredentials(context);
        Twilio.init(twilioConfig.get(ACCOUNT_SID), twilioConfig.get(AUTH_TOKEN));
        return Call.fetcher(callSid).fetch();
    }

    private void startMissedCallVerification(RequiredActionContext context, String phoneNoWithCountryCode,
                                             boolean isRetry) {
        String callSid = startCall(context, phoneNoWithCountryCode);
        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), MISSED_CALL_VALIDATION);
        showMissedCallVerificationftl(context, callSid, phoneNoWithCountryCode, isRetry);
    }

    private Map<String, String> getTwilioCredentials(RequiredActionContext context) {
        Map<String, String> twilioCredentials = new HashMap<>();
        if (session.getContext().getRealm().isEnabledOwnSmsProvider() && session.getContext().getRealm().getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("twilio")).count() > 0) {
            SmsConfigModel twilioConfig = context.getRealm().getSmsConfigByProviderId("twilio");
            if (twilioConfig.getConfig() == null || twilioConfig.getConfig().isEmpty()) {
                throw new IllegalArgumentException("Twilio Configuration missing in the workspace.");
            }
            if (isCallSupported(twilioConfig)) {
                twilioCredentials.put(ACCOUNT_SID, twilioConfig.getConfig().get(TWILIO_ACCOUNT_SID));
                twilioCredentials.put(AUTH_TOKEN, twilioConfig.getConfig().get(TWILIO_AUTH_TOKEN));
                twilioCredentials.put(FROM, twilioConfig.getConfig().get(FROM));
                return twilioCredentials;
            }
        }
        RealmModel adminRealm = session.realms().getRealmByName(Config.getAdminRealm());
        Optional<SmsConfigModel> twilio =
                adminRealm.getSmsConfigsStream().filter(x -> x.getSmsProviderId().equalsIgnoreCase("twilio")).findFirst();
        if (!twilio.isPresent()) {
            throw new IllegalArgumentException("Twilio Configuration missing in root workspace.");
        }
        twilioCredentials.put(ACCOUNT_SID, twilio.get().getConfig().get(TWILIO_ACCOUNT_SID));
        twilioCredentials.put(AUTH_TOKEN, twilio.get().getConfig().get(TWILIO_AUTH_TOKEN));
        twilioCredentials.put(FROM, twilio.get().getConfig().get(FROM));
        return twilioCredentials;
    }

    private void setClaims(RequiredActionContext context, Map<String, Object> phoneData, String phoneNo) {
        RealmModel realmModel = context.getRealm();

        //default-claims
        realmModel.addAttribute(context.getRequestId(),  VALIDATE_PHONE_PHONE_CLAIM, phoneNo);
        realmModel.addAttribute(context.getRequestId(),  VALIDATE_PHONE_PHONE_VALID_VERIFIED_CLAIM, "true");
        realmModel.addAttribute(context.getRequestId(),  VALIDATE_PHONE_PHONE_VALID_VERIFIED_AT_CLAIM,
                String.valueOf(Time.currentTime()));

        if (context.getConfig().size() == 0) {
            ArrayList<String> claims = new ArrayList<>();
            claims.add(PHONE_NO_VALIDATION_CLAIM);
            claims.add(PHONE_NO_VALIDATION_VERIFIED_CLAIM);
            claims.add(PHONE_NO_VALIDATION_VERIFIED_AT_CLAIM);

            GeneralHelper.handleSettingClaims(context.getRealm(), phoneData, claims, "validate_phone_no",
                    context.getRequestId());
        } else {
            GeneralHelper.handleSettingClaims(context.getRealm(), phoneData, Arrays.asList(context.getConfig().get(
                    "phone-no-valid-claims").split("##")), "validate_phone_no", context.getRequestId());
        }
    }

    private void setPhoneNoToUser(RequiredActionContext context, String phoneNumberWithCountryCode) {
        Map<String, String> phoneNoDetails =
                com.qlikverify.backoffice.GeneralHelper.parseMobileNumber(phoneNumberWithCountryCode);
        context.getUser().setMobileNumber(phoneNoDetails.get("phoneNo"));
        context.getUser().setMobileNumberRegion(phoneNoDetails.get("phoneNoReg"));
    }

    private boolean isCallSupported(SmsConfigModel twilioConfig) {
        try {
            Twilio.init(twilioConfig.getConfig().get(TWILIO_ACCOUNT_SID),
                    twilioConfig.getConfig().get(TWILIO_AUTH_TOKEN));
            PhoneNumber phoneNumber = new PhoneNumber(twilioConfig.getConfig().get(FROM));
            boolean isCallSupported = false;

            ResourceSet<IncomingPhoneNumber> numbers = IncomingPhoneNumber.reader().read();
            for (IncomingPhoneNumber number : numbers) {
                if (number.getPhoneNumber().equals(phoneNumber)) {
                    isCallSupported = number.getCapabilities().getVoice() && number.getStatus().equalsIgnoreCase("in" +
                            "-use");
                }
            }
            twilioConfig.getConfig().put("isCallSupported", String.valueOf(isCallSupported));
            return isCallSupported;
        } catch (TwilioException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    public static void showView(RequiredActionContext context, String error, String ftl,
                                Map<String, String> attributes) {
        LoginFormsProvider formsProvider = context.form();
        formsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
        if (error != null) {
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        if (attributes != null) {
            attributes.forEach(formsProvider::setAttribute);
        }
        javax.ws.rs.core.Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        this.session = session;
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
        return "validate-phone-no";
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{VALIDATE_PHONE_NUMBER, MISSED_CALL_VALIDATION};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = getDefaultClaims();
        // Define additional claims that may be added in the runtime to the model's config's
        Map<String, String> potentialClaims = getPhoneNoValidationPotentialClaims();

        // Process claims from model config
        Set<String> configClaimKeys = model.getConfig().entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().contains("claims"))
                .flatMap(entry -> Arrays.stream(entry.getValue().split("##")))
                .collect(Collectors.toSet());

        // Add matching claims from potentialClaims
        configClaimKeys.forEach(key -> {
            if (potentialClaims.containsKey(key)) {
                claims.put(key, potentialClaims.get(key));
            }
        });

        //Modifying claims as per template specific
        Map<String, String> finalClaims = claims.entrySet().stream()
                .map(x -> Map.entry(getId().replace("bo-", "").replace("-", "_") + "@" + x.getKey(), x.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        finalClaims.put(VALIDATE_PHONE_NO_SKIPPED, ValidationTypes.BOOLEAN.getValue());

        return finalClaims;
    }

    private static Map<String, String> getDefaultClaims() {
        Map<String, String> defaultClaims = new LinkedHashMap<>();
        defaultClaims.put(PHONE_NO_VALIDATION_CLAIM, ValidationTypes.NUMBER.getValue());
        defaultClaims.put(PHONE_NO_VALIDATION_VERIFIED_CLAIM, ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(PHONE_NO_VALIDATION_VERIFIED_AT_CLAIM, ValidationTypes.DATE_AND_TIME.getValue());

        return defaultClaims;
    }

    private static Map<String, String> getPhoneNoValidationPotentialClaims() {
        Map<String, String> potentialClaims = new LinkedHashMap<>();
        potentialClaims.put(PHONE_NO_VALIDATION_CLAIM, ValidationTypes.NUMBER.getValue());
        potentialClaims.put(PHONE_NO_VALIDATION_VERIFIED_CLAIM, ValidationTypes.BOOLEAN.getValue());
        potentialClaims.put(PHONE_NO_VALIDATION_VERIFIED_AT_CLAIM, ValidationTypes.DATE_AND_TIME.getValue());
        potentialClaims.put("valid", ValidationTypes.STRING.getValue());
        potentialClaims.put("local_format", ValidationTypes.STRING.getValue());
        potentialClaims.put("international_format", ValidationTypes.STRING.getValue());
        potentialClaims.put("country_prefix", ValidationTypes.STRING.getValue());
        potentialClaims.put("country_code", ValidationTypes.STRING.getValue());
        potentialClaims.put("country_name", ValidationTypes.STRING.getValue());
        potentialClaims.put("location", ValidationTypes.STRING.getValue());
        potentialClaims.put("carrier", ValidationTypes.STRING.getValue());
        potentialClaims.put("line_type", ValidationTypes.STRING.getValue());

        return potentialClaims;
    }
}
