package com.qlikverify.services.sms.twilio;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.qlikverify.services.sms.SMSEventBinder;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import org.jboss.logging.Logger;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsProvider;
import org.keycloak.sms.models.EztoSMSModel;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.util.HashMap;
import java.util.Map;

import static com.qlikverify.services.constants.ErrorMappings.CONFIGURATION_IS_NOT_VALID_PLEASE_CHECK_WITH_ADMIN;

public class TwilioProvider extends SMSEventBinder implements SmsProvider {

    HttpUtility httpUtility;
    private final Logger logger = Logger.getLogger(TwilioProvider.class);
    KeycloakSession keycloakSession;

    private final String BASE_ML = "<Response><SAY></Response>";
    private final String SAY_ML = "<Say><MESSAGE></Say><Pause length='1'/>";

    private static Map<Integer, Boolean> displayNameList = new HashMap<>() {{
        put(44, true);
    }};


    public TwilioProvider(KeycloakSession session) {
        super(session);
        this.keycloakSession = session;
        this.httpUtility = new HttpUtility();
    }

    @Override
    public EztoSMSModel sendSms(Map<String, String> smsConfig, String toPhone, String textBody, Map<String, Object> additionalArguments) throws SmsException {
        String from = smsConfig.get("from");
        String sid = smsConfig.get("accountsid");
        String token = smsConfig.get("authtoken");
        boolean isDisplayNameEnabled = Boolean.parseBoolean(smsConfig.get(TwilioProviderFactory.ENABLEDISPLAYNAME));
        String displayName = smsConfig.get(TwilioProviderFactory.DISPLAYNAME);
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        if (isDisplayNameEnabled) {
            try {
                // phone must begin with '+'
                Phonenumber.PhoneNumber numberProto = phoneUtil.parse(toPhone, "");
                int countryCode = numberProto.getCountryCode();
                if (displayNameList.containsKey(countryCode)) {
                    if (displayNameList.get(countryCode)) {
                        if (displayName != null && !displayName.isEmpty()) {
                            from = displayName;
                        }
                    }
                }
            } catch (NumberParseException e) {
                System.err.println("NumberParseException was thrown: " + e.toString());
            }
        }
        EztoSMSModel eztoSMSModel;

        try {
            Twilio.init(sid, token);
            Message twMessage = Message.creator(
                            new com.twilio.type.PhoneNumber(toPhone),
                            new com.twilio.type.PhoneNumber(from),
                            textBody)
                    .create();
            eztoSMSModel = new EztoSMSModel(true, twMessage.getSid());
            logger.debug(twMessage.getSid());
            sendSuccess(EventType.TWILIO_SMS, JsonSerialization.writeValueAsString(twMessage));
        } catch (ApiException e) {
            if (e.getStatusCode() == 400) {
                sendError(EventType.TWILIO_SMS, e.getMessage());
                throw new SmsException(e.getMessage());
            }
            sendError(EventType.TWILIO_SMS, e.getMessage());
            throw new SmsException(CONFIGURATION_IS_NOT_VALID_PLEASE_CHECK_WITH_ADMIN);
        } catch (Exception err) {
            sendError(EventType.TWILIO_SMS, err.getMessage());
            throw new SmsException(CONFIGURATION_IS_NOT_VALID_PLEASE_CHECK_WITH_ADMIN);
        }

        return eztoSMSModel;
    }

    @Override
    public void sendCall(Map<String, String> smsConfig, String toPhone, String[] text, Map<String, Object> additionalArguments) throws SmsException {
        if (!isCallSupported(smsConfig))
            throw new SmsException("Call option is not configured or supported by the provider");

        String from = smsConfig.get("from");
        String sid = smsConfig.get("accountsid");
        String token = smsConfig.get("authtoken");
        String message = BASE_ML;
        StringBuilder stringBuilder = new StringBuilder();
        for (int x = 0; x < text.length; x++) {
            stringBuilder.append(SAY_ML.replace("<MESSAGE>", text[x]));
        }
        message = message.replaceAll("<SAY>", stringBuilder.toString());

        try {
            Twilio.init(sid, token);
            Call call = Call.creator(
                            new com.twilio.type.PhoneNumber(toPhone),
                            new com.twilio.type.PhoneNumber(from),
                            new com.twilio.type.Twiml(message))
                    .create();
            sendSuccess(EventType.TWILIO_SMS, JsonSerialization.writeValueAsString(call));
        } catch (Exception err) {
            sendError(EventType.TWILIO_SMS, err.getMessage());
            throw new SmsException(CONFIGURATION_IS_NOT_VALID_PLEASE_CHECK_WITH_ADMIN);
        }
    }

    @Override
    public boolean isSmsSupported(Map<String, String> smsConfig) {
        return true;
    }

    @Override
    public boolean isCallSupported(Map<String, String> smsConfig) {
        String callUrl = smsConfig.get("callurl");
        return Boolean.parseBoolean(callUrl);
    }


    @Override
    public void close() {

    }
}
