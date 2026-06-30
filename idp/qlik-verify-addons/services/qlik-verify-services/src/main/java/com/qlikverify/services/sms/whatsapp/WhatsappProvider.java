package com.qlikverify.services.sms.whatsapp;

import com.qlikverify.services.sms.SMSEventBinder;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsProvider;
import org.keycloak.sms.models.EztoSMSModel;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.NotSupportedException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.qlikverify.services.constants.ErrorMappings.CONFIGURATION_IS_NOT_VALID_PLEASE_CHECK_WITH_ADMIN;

public class WhatsappProvider extends SMSEventBinder implements SmsProvider {

    HttpUtility httpUtility;

    private final Logger logger = Logger.getLogger(WhatsappProvider.class);

    public WhatsappProvider(KeycloakSession session) {
        super(session);
        this.httpUtility = new HttpUtility();
    }

    @Override
    public EztoSMSModel sendSms(Map<String, String> smsConfig, String toPhone, String textBody, Map<String, Object> additionalArguments) throws SmsException {

        String from = smsConfig.get("from");
        String messageContentSid = "";
        Map<String, Object> contentVariables = new HashMap<>();
        if (!additionalArguments.isEmpty()) {
            messageContentSid = String.valueOf(additionalArguments.get("messageContentSid"));
            additionalArguments.remove("messageContentSid");
            if(!additionalArguments.isEmpty()) {
                contentVariables = getTextMessage(additionalArguments);
            }
        }
        EztoSMSModel eztoSMSModel;
        try {
            String sid = smsConfig.get("accountsid");
            String token = smsConfig.get("authtoken");

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("To", "whatsapp:" + toPhone);
            reqBody.put("From", "whatsapp:" + from);
            reqBody.put("ContentSid", messageContentSid);
            if(!contentVariables.isEmpty()) {
                reqBody.put("ContentVariables", JsonSerialization.writeValueAsString(contentVariables));
            }

            SimpleHttp.Response response = SimpleHttp.doPost("https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json", HttpClients.createDefault()).acceptJson().json(reqBody).authBasic(sid, token).asResponse();
            Map<String, Object> responseBody = JsonSerialization.readValue(response.asJson().toString(), Map.class);

            eztoSMSModel = new EztoSMSModel(true, responseBody.get("sid").toString());
            sendSuccess(EventType.WHATSAPP, JsonSerialization.writeValueAsString(responseBody));
        } catch (Exception err) {
            sendError(EventType.WHATSAPP, err.getMessage());
            throw new SmsException(CONFIGURATION_IS_NOT_VALID_PLEASE_CHECK_WITH_ADMIN);
        }

        return eztoSMSModel;
    }

    @Override
    public void sendCall(Map<String, String> smsConfig, String toPhone, String[] text, Map<String, Object> additionalArguments) throws SmsException {
        throw new SmsException(new NotSupportedException().getCause());
    }

    @Override
    public boolean isSmsSupported(Map<String, String> smsConfig) {
        return true;
    }

    @Override
    public boolean isCallSupported(Map<String, String> smsConfig) {
        return false;
    }

    @Override
    public void close() {

    }

    private Map<String, Object> getTextMessage(Map<String, Object> additionalArguments) {

        if (!additionalArguments.containsKey("templateType"))
            return Collections.emptyMap();

        if (!additionalArguments.containsKey("messageParts"))
            return Collections.emptyMap();

        String templateType = String.valueOf(additionalArguments.get("templateType"));
        Object[] msgParts = (Object[]) additionalArguments.get("messageParts");
        switch (templateType) {
            case "OTP":
                Map<String, Object> OtpContentVariables = new HashMap<>();
                for (int x = 0; x < msgParts.length; x++) {
                    OtpContentVariables.put(String.valueOf(x + 1), msgParts[x]);
                }
                return OtpContentVariables;
            case "PUSH":
                Map<String, Object> pushContentVariables = new HashMap<>();
                for (int x = 0; x < msgParts.length; x++) {
                    pushContentVariables.put(String.valueOf(x + 1), msgParts[x]);
                }
                return pushContentVariables;

            case "CIBA":
                Map<String, Object> cibaContentVariables = new HashMap<>();
                for (int x = 0; x < msgParts.length; x++) {
                    cibaContentVariables.put(String.valueOf(x + 1), msgParts[x]);
                }
                return cibaContentVariables;
        }

        return null;
    }
}
