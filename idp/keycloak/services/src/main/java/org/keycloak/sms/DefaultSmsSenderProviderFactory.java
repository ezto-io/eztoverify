package org.keycloak.sms;

import org.keycloak.Config;
import org.keycloak.email.ConfigTestResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultSmsSenderProviderFactory implements SmsSenderProviderFactory {
    @Override
    public SmsSenderProvider create(KeycloakSession session) {
        return new DefaultSmsSenderProvider(session);
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
        return "default";
    }

    @Override
    public ConfigTestResponse testConfig(KeycloakSession session, String providerId, Map<String, String> config) {
        try {
            List<String> datas = new ArrayList<>();
            datas.add("123456");
            Map<String, Object> additionalArguments = new HashMap<>();
            additionalArguments.put("messageParts", datas.toArray());
            additionalArguments.put("templateType", "OTP");
            String twilioOtpTemplateSid = session.getContext().getRealm().isEnabledOwnWhatsappProvider() ? session.getContext().getRealm().getAttribute("twilio-otp-template-sid") : session.realms().getRealmByName(Config.getAdminRealm()).getAttribute("twilio-otp-template-sid");
            additionalArguments.put("messageContentSid", twilioOtpTemplateSid);
            session.getProvider(SmsProvider.class, providerId).sendSms(config, config.get("phoneNumber"), "If you see this message, then sms configuration works fine", additionalArguments);
            return new ConfigTestResponse(true, null);
        } catch (SmsException e) {
            e.printStackTrace();
            return new ConfigTestResponse(false, e.getMessage());
        }
    }
}
