package org.keycloak.sms;

import org.keycloak.provider.Provider;
import org.keycloak.sms.models.EztoSMSModel;

import java.util.Map;

public interface SmsProvider extends Provider {

    EztoSMSModel sendSms(Map<String, String> smsConfig, String toPhone, String textBody, Map<String, Object> additionalArguments) throws SmsException;

    void sendCall(Map<String, String> smsConfig, String toPhone, String[] text, Map<String, Object> additionalArguments) throws SmsException;

    boolean isSmsSupported(Map<String, String> smsConfig);

    boolean isCallSupported(Map<String, String> smsConfig);

}
