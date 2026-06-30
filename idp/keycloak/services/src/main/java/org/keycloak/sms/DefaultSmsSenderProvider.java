package org.keycloak.sms;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.qlikverify.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.keycloak.component.ComponentModel;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.saml.common.exceptions.NotImplementedException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.DefaultClientRegistrationPolicies;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.sms.models.EztoSMSModel;
import org.keycloak.sms.models.NumberFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultSmsSenderProvider implements SmsSenderProvider {

    private KeycloakSession session;
    protected UserModel user;
    protected String smsProviderId;
    private static final String CONTACT_ADMIN = "An error occurred. Please contact admin";

    private static final Logger logger = Logger.getLogger(DefaultSmsSenderProvider.class);

    public DefaultSmsSenderProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public SmsSenderProvider setUser(UserModel userModel) {
        this.user = userModel;
        return this;
    }

    @Override
    public SmsSenderProvider setProviderId(String providerId) {
        this.smsProviderId = providerId;
        return this;
    }

    /**
     * Method to trigger sms, this shall be executed only if the provider supports
     *
     * @param toPhone
     * @param textBody
     * @param additionalArguments
     * @throws SmsException
     */
    @Override
    public void sendSMS(String toPhone, String textBody, Map<String, Object> additionalArguments) throws SmsException {
        if (GeneralHelper.checkWalletBalance(session.getContext().getRealm())) {
            throw new SmsException(CONTACT_ADMIN);
        }
        SmsConfigModel smsConfig = getConfig();
        if (smsConfig == null || smsConfig.getConfig().isEmpty())
            throw new SmsException(CONTACT_ADMIN);

        SmsProvider smsProvider = getSmsProvider();

        session.getProvider(EmailSenderProvider.class, "");

        if (smsProvider == null) throw new SmsException("SmsProvider is not available");

        RealmModel realmModel = session.getContext().getRealm();

        EztoSMSModel eztoSMSModel = smsProvider.sendSms(smsConfig.getConfig(), toPhone, textBody, additionalArguments);

        HashMap<String, Object> attributes = new HashMap<>();
        if(additionalArguments != null && additionalArguments.containsKey("requestId")) {
            attributes.put("requestId", additionalArguments.get("requestId"));
        }
        attributes.put("to_phone", toPhone);
        attributes.put("providerId", smsProviderId);
        if (eztoSMSModel.isMessageSent() && !eztoSMSModel.getMessageId().isEmpty()) {
            attributes.put("messageId", eztoSMSModel.getMessageId());
        }
        if (smsProviderId.equalsIgnoreCase("whatsapp")) {
            Optional<ComponentModel> optionalComponentModel = realmModel.getComponentsStream(realmModel.getId()).filter(x -> x.getProviderId().equalsIgnoreCase("whatsapp-policy-checker")).findFirst();
            DefaultClientRegistrationPolicies.addWhatsappPolicy(realmModel, optionalComponentModel.isPresent());
            attributes.put("type", "whatsapp");
            ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "whatsapp-policy-checker", attributes);
        } else {
            Optional<ComponentModel> optionalComponentModel = realmModel.getComponentsStream(realmModel.getId()).filter(x -> x.getProviderId().equalsIgnoreCase("sms-policy-checker")).findFirst();
            DefaultClientRegistrationPolicies.addSmsPolicy(realmModel, optionalComponentModel.isPresent());
            attributes.put("type", "sms");
            ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "sms-policy-checker", attributes);
        }
    }

    /**
     * Method to trigger call, this shall be executed only if the provider supports
     *
     * @param toPhone
     * @param additionalArguments
     * @param text
     * @throws SmsException
     */
    @Override
    public void sendCall(String toPhone, String[] text, Map<String, Object> additionalArguments) throws SmsException {
        SmsConfigModel smsConfig = getConfig();
        if (smsConfig == null || smsConfig.getConfig().isEmpty())
            throw new SmsException(CONTACT_ADMIN);

        SmsProvider smsProvider = getSmsProvider();

        if (smsProvider == null) throw new SmsException("SmsProvider is not available");

        RealmModel realmModel = session.getContext().getRealm();

        try {
            Map<String, Object> attrs = new HashMap<>();
            String policyChecker = smsProviderId.equalsIgnoreCase("whatsapp") ? "whatsapp-policy-checker"
                    : "sms-policy-checker";

            Optional<ComponentModel> optionalComponentModel =
                    realmModel.getComponentsStream(realmModel.getId()).filter(x -> x.getProviderId().equalsIgnoreCase(policyChecker)).findFirst();
            if (smsProviderId.equalsIgnoreCase("whatsapp")) {
                DefaultClientRegistrationPolicies.addWhatsappPolicy(realmModel, optionalComponentModel.isPresent());
            } else {
                DefaultClientRegistrationPolicies.addSmsPolicy(realmModel, optionalComponentModel.isPresent());
            }

            ClientRegistrationPolicyManager.triggerBeforeRegister(session, RegistrationAuth.AUTHENTICATED,
                    policyChecker);
            smsProvider.sendCall(smsConfig.getConfig(), toPhone, text, additionalArguments);
            attrs.put("to_phone", toPhone);
            attrs.put("type", "call");
            attrs.put("providerId", smsProviderId);

            ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED,
                    policyChecker, attrs);

        } catch (SmsException | ClientRegistrationPolicyException err) {
            logger.error(err);
            throw err;
        }
    }

    @Override
    public Map<String, Object> getStatus(Map<String, Object> arguments) {
        throw new NotImplementedException("Not Implemented");
    }

    /**
     * Method to parse phone number, in case the region is not provided it will be set to default DE
     *
     * @param toPhone
     * @param region
     * @return NumberFormat
     * @throws SmsException
     */
    @Override
    public NumberFormat parseNumber(String toPhone, String region) throws SmsException {

        NumberFormat numberFormat = null;
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(toPhone, region);
            if (!phoneUtil.isValidNumber(parsed)) {
                throw new SmsException("Invalid phoneNumber");
            }

            numberFormat = new NumberFormat();
            numberFormat.setRegion(phoneUtil.getRegionCodeForNumber(parsed));
            numberFormat.setCountryCode(String.valueOf(parsed.getCountryCode()));
            numberFormat.setNationalFormat(phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
            numberFormat.setType(phoneUtil.getNumberType(parsed).name());
            numberFormat.setE16Format(phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164));
            numberFormat.setInternationalFormat(phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
        } catch (NumberParseException e) {
            throw new SmsException(e.getCause());
        }
        return numberFormat;
    }

    /**
     * Method to check if sms is configured
     *
     * @return
     */
    @Override
    public boolean isConfigured() {
        SmsConfigModel smsConfig = null;
        try {
            smsConfig = getConfig();
            return smsConfig != null && !smsConfig.getConfig().isEmpty();
        } catch (SmsException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Method to check if call option is supported
     *
     * @return
     */
    @Override
    public boolean isCallSupported() {
        try {
            SmsConfigModel smsConfig = getConfig();
            if (smsConfig == null || smsConfig.getConfig().isEmpty()) return false;

            SmsProvider smsProvider = getSmsProvider();

            if (smsProvider == null) return false;

            return smsProvider.isCallSupported(smsConfig.getConfig());
        } catch (Exception err) {
            return false;
        }
    }

    /**
     * Method to check if sms option is supported
     *
     * @return
     */
    @Override
    public boolean isSmsSupported() {
        try {
            SmsConfigModel smsConfig = getConfig();
            if (smsConfig == null || smsConfig.getConfig().isEmpty()) return false;

            SmsProvider smsProvider = getSmsProvider();

            if (smsProvider == null) return false;

            return smsProvider.isSmsSupported(smsConfig.getConfig());
        } catch (Exception err) {
            return false;
        }
    }

    private SmsConfigModel getConfig() throws SmsException {
        if (smsProviderId == null || smsProviderId.isEmpty()) throw new SmsException("Provider is not set");

        SmsConfigModel config = session.getContext().getRealm().getSmsConfigByProviderId(smsProviderId);
        if (config != null && !config.getConfig().isEmpty()) {
            return config;
        }

        RealmModel adminRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
        if (adminRealm != null) {
            Optional<SmsConfigModel> optSmsConfig = adminRealm.getSmsConfigsStream().filter(x -> {
                if (smsProviderId.equalsIgnoreCase("whatsapp")) {
                    return x.getSmsProviderId().equalsIgnoreCase("whatsapp");
                }
                return !x.getSmsProviderId().equalsIgnoreCase("whatsapp");
            }).findFirst();
            if (optSmsConfig.isPresent()) {
                return optSmsConfig.get();
            }
        }
        return config;
    }

    private SmsProvider getSmsProvider() throws SmsException {
        if (smsProviderId == null || smsProviderId.isEmpty()) throw new SmsException("Provider is not set");

        return session.getProvider(SmsProvider.class, smsProviderId.toLowerCase());
    }
}