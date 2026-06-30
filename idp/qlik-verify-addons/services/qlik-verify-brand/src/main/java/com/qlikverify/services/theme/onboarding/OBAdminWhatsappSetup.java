package com.qlikverify.services.theme.onboarding;

import com.qlikverify.services.theme.helpers.GeneralHelpers;
import static com.qlikverify.services.theme.onboarding.Constants.*;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.common.Version;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.onboarding.OnboardingStepProvider;
import org.keycloak.onboarding.OnboardingStepProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.SuccessResponse;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsProvider;
import org.keycloak.sms.SmsProviderFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.services.theme.onboarding.OBAdminSmsSetup.testMobileNumberConfig;

public class OBAdminWhatsappSetup implements OnboardingStepProvider, OnboardingStepProviderFactory {

    KeycloakSession session;
    @Override
    public List<ProviderConfigProperty> getConfig() {
        List<ProviderConfigProperty> configs = new ArrayList<>();
        List<ProviderFactory> whatsappProviders =
                session.getKeycloakSessionFactory().getProviderFactoriesStream(SmsProvider.class).filter(x -> x.getId().contains(WHATSAPP)).collect(Collectors.toList());

        whatsappProviders.forEach(x -> {
            SmsProviderFactory factory = (SmsProviderFactory) x;
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setType(ProviderConfigProperty.CONFIG_TYPE);
            property.setName(factory.getId());
            property.setHelpText("choose-default-whatsapp-provider");
            property.setLabel(factory.getDisplayName());
            property.setLogo(factory.getLogo());
            List<ProviderConfigProperty> properties = new ArrayList<>();
            properties.addAll(factory.getConfigProperties(session));
            if (!properties.contains(testMobileNumberConfig)){
                properties.add(testMobileNumberConfig);
            }
            property.setConfigProperties(properties);
            configs.add(property);
        });

        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.CONFIG_TYPE);
        property.setName(ADMIN_WHATSAPP_PROVIDER);
        property.setHelpText("choose-default-whatsapp-provider");
        property.setLabel("whatsapp-provider");
        property.setConfigProperties(configs);
        Optional<SmsConfigModel> whatsappSender =
                session.getContext().getRealm().getSmsConfigsStream().filter(x -> Objects.equals(x.getSmsProviderId()
                        , WHATSAPP)).findFirst();
        if (whatsappSender.isPresent()){
            Map<String, Object> defaultValue = new HashMap<>();
            defaultValue.put(ADMIN_WHATSAPP_PROVIDER, whatsappSender.get().getSmsProviderId());
            defaultValue.put("config", whatsappSender.get().getConfig());
            property.setDefaultValue(defaultValue);
        }

        return Collections.singletonList(property);
    }

    @Override
    public Response process(MultivaluedMap<String, String> params, RealmModel realmModel) {
        String smsProviderId = params.getFirst(ADMIN_WHATSAPP_PROVIDER);
        if (smsProviderId == null){
            return ErrorResponse.error("whatsapp-provider-required-txt", Response.Status.BAD_REQUEST);
        }

        SmsProviderFactory whatsappSenderProvider =
                (SmsProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SmsProvider.class, smsProviderId);
        if (whatsappSenderProvider == null){
            return ErrorResponse.error("whatsapp-provider-not-found", Response.Status.BAD_REQUEST);
        }

        String testMobileNumber = params.getFirst(TEST_NUMBER);
        if (testMobileNumber == null || testMobileNumber.isEmpty()) {
            return ErrorResponse.error("test-mob-num-required", Response.Status.BAD_REQUEST);
        }


        List<ProviderConfigProperty> providerConfigProperties = whatsappSenderProvider.getConfigProperties(session);
        Map<String, String> receivedConfig = GeneralHelpers.getMatchedResult(providerConfigProperties,
                GeneralHelpers.convertMultiToRegularMap(params));

        receivedConfig.put(TEST_NUMBER, params.getFirst(TEST_NUMBER));

        List<String> errors = whatsappSenderProvider.validateConfig(session, receivedConfig);

        if (!errors.isEmpty()){
            return ErrorResponse.error(String.join(",", errors), Response.Status.BAD_REQUEST);
        }

        session.getContext().setRealm(realmModel);

        String env = EnvironmentReader.getValue(Constants.ENV, "");
        if (env.equalsIgnoreCase(Constants.PROD_ENV)){
            try{
                session.getProvider(SmsProvider.class, smsProviderId).sendSms(receivedConfig, receivedConfig.get(TEST_NUMBER), "If you see " +
                        "this " +
                        "message, then Whatsapp configuration works fine", null);
            } catch (SmsException exception){
                return ErrorResponse.error(exception.getMessage(), Response.Status.BAD_REQUEST);
            }
        }

        // Remove already available sms settings
        List<SmsConfigModel> availableSmsConfig = realmModel.getSmsConfigsStream().collect(Collectors.toList());
        availableSmsConfig.forEach(x -> {
            if(x.getSmsProviderId().contains(WHATSAPP)){
                realmModel.removeSmsConfig(realmModel.getSmsConfigByProviderId(x.getSmsProviderId()));
            }
        });

        SmsConfigModel smsConfigModel = new SmsConfigModel();
        smsConfigModel.setSmsProviderId(smsProviderId);
        Map<String, String> config = new HashMap<>();
        receivedConfig.remove(TEST_NUMBER);
        config.putAll(receivedConfig);
        smsConfigModel.setConfig(config);
        realmModel.addSmsConfig(smsConfigModel);

        if (!realmModel.getSmsSenderStream().collect(Collectors.toList()).contains(smsProviderId)){
            Set<String> smsSender = new HashSet<>();
            smsSender.add(smsProviderId);
            smsSender. addAll(realmModel.getSmsSenderStream().collect(Collectors.toList()));
            realmModel.setSmsSenders(smsSender);
        }

        GeneralHelpers.resetObUpdateBasedOnSubscription(session);
        GeneralHelpers.addToCompletedStep(realmModel,
                this.getProviderId());
        return SuccessResponse.success("whatsapp-setup-completed", Response.Status.OK);
    }

    @Override
    public boolean validate(MultivaluedMap<String, String> params, RealmModel realmModel) {
        return false;
    }

    @Override
    public boolean isCompleted(RealmModel realmModel) {
        return  GeneralHelpers.checkForCurrentStep(realmModel,
                this.getProviderId());
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public String getTitle() {
        return "whatsapp-provider";
    }

    @Override
    public String getDescription() {
        return Objects.equals(session.getContext().getRealm().getName(), Config.getAdminRealm()) ?
                Constants.OB_ADMIN_MASTER_WHATSAPP_DESC : Constants.OB_ADMIN_WHATSAPP_DESC;
    }

    @Override
    public String getProviderId() {
        return Constants.OB_ADMIN_WHATSAPP_SETUP;
    }

    @Override
    public boolean isSkippable() {
        RealmModel realmModel = session.getContext().getRealm();
        if(realmModel == null){
            return  true;
        }
        if (Objects.equals(session.getContext().getRealm().getName(), Config.getAdminRealm())){
            return  true;
        }
        // On-prem: WhatsApp (Twilio) is optional — allow skipping.
        if (Version.OFFERING.equalsIgnoreCase(ON_PREMISE)){
            return  true;
        }
        return !session.getContext().getRealm().isEnabledOwnWhatsappProvider();
    }

    @Override
    public boolean shouldIncludeBasedOnMode() {
        // On-prem has no WhatsApp/Twilio provider — hide this onboarding step entirely.
        return !Version.OFFERING.equalsIgnoreCase(ON_PREMISE);
    }

    @Override
    public OnboardingStepProvider create(KeycloakSession session) {
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
        return Constants.OB_ADMIN_WHATSAPP_SETUP;
    }
}
