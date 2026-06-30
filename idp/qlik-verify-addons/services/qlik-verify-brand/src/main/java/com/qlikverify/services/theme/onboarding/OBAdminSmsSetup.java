package com.qlikverify.services.theme.onboarding;

import com.qlikverify.services.theme.helpers.GeneralHelpers;
import static com.qlikverify.services.theme.onboarding.Constants.*;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.common.Version;
import org.keycloak.models.*;
import org.keycloak.onboarding.OnboardingStepProvider;
import org.keycloak.onboarding.OnboardingStepProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.SuccessResponse;
import org.keycloak.sms.*;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

public class OBAdminSmsSetup implements OnboardingStepProvider, OnboardingStepProviderFactory {

    static  final ProviderConfigProperty testMobileNumberConfig = new ProviderConfigProperty(TEST_NUMBER,
            "test-mobile-num",
            "test-mobile-num-helptext",
            ProviderConfigProperty.TEST_MOBILE_NUMBER,
            ""
    );
    KeycloakSession session;

    @Override
    public List<ProviderConfigProperty> getConfig() {
        List<ProviderConfigProperty> configs = new ArrayList<>();
        List<ProviderFactory> smsProviders =
                session.getKeycloakSessionFactory().getProviderFactoriesStream(SmsProvider.class).filter(x -> !x.getId().contains(WHATSAPP)).collect(Collectors.toList());
        // Test mobile number
        testMobileNumberConfig.setRequired(true);

        smsProviders.forEach(x -> {
            SmsProviderFactory factory = (SmsProviderFactory) x;
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setType(ProviderConfigProperty.CONFIG_TYPE);
            property.setName(factory.getId());
            property.setHelpText("choose-default-sms-provider");
            property.setLabel(factory.getDisplayName());
            property.setLogo(factory.getLogo());

            List<ProviderConfigProperty> properties = new ArrayList<>();
            properties.addAll(factory.getConfigProperties(session));
            if (!properties.contains(testMobileNumberConfig)){
                testMobileNumberConfig.setDefaultValue(session.getContext().getRealm().getAttribute("defaultRegion"));
                properties.add(testMobileNumberConfig);
            }

            property.setConfigProperties(properties);
            configs.add(property);
        });

        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.CONFIG_TYPE);
        property.setName(ADMIN_SMS_PROVIDER);
        property.setRequired(true);
        property.setHelpText("choose-default-sms-provider");
        property.setLabel("intro-add-sms-provider-title");
        property.setConfigProperties(configs);
        Optional<SmsConfigModel> smsSender=
                session.getContext().getRealm().getSmsConfigsStream().filter(x -> !Objects.equals(x.getSmsProviderId(), WHATSAPP)).findFirst();
        if (smsSender.isPresent()){
            Map<String, Object> smsSenderDefaultValue = new HashMap<>();
            smsSenderDefaultValue.put(ADMIN_SMS_PROVIDER, smsSender.get().getSmsProviderId());
            smsSenderDefaultValue.put("config", smsSender.get().getConfig());
            property.setDefaultValue(smsSenderDefaultValue);
        }
        return Collections.singletonList(property);
    }

    @Override
    public Response process(MultivaluedMap<String, String> params, RealmModel realmModel) {
        String smsProviderId = params.getFirst(ADMIN_SMS_PROVIDER);
        if (smsProviderId == null){
            return ErrorResponse.error("sms-provider-required", Response.Status.BAD_REQUEST);
        }

        SmsProviderFactory smsSenderProvider =
                (SmsProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SmsProvider.class, smsProviderId);
        if (smsSenderProvider == null){
            return ErrorResponse.error("sms-provider-not-found", Response.Status.BAD_REQUEST);
        }

        String testMobileNumber = params.getFirst(TEST_NUMBER);
        if (testMobileNumber == null || testMobileNumber.isEmpty()) {
            return ErrorResponse.error("test-mob-num-required", Response.Status.BAD_REQUEST);
        }


        List<ProviderConfigProperty> providerConfigProperties = smsSenderProvider.getConfigProperties(session);
        Map<String, String> receivedConfig = GeneralHelpers.getMatchedResult(providerConfigProperties,
                GeneralHelpers.convertMultiToRegularMap(params));

        receivedConfig.put(TEST_NUMBER, params.getFirst(TEST_NUMBER));

        List<String> errors = smsSenderProvider.validateConfig(session, receivedConfig);

        if (!errors.isEmpty()){
            return ErrorResponse.error(String.join(",", errors), Response.Status.BAD_REQUEST);
        }

        session.getContext().setRealm(realmModel);

        String env = EnvironmentReader.getValue(Constants.ENV, "");
        if (env.equalsIgnoreCase(Constants.PROD_ENV)){
            try{
                session.getProvider(SmsProvider.class, smsProviderId).sendSms(receivedConfig, receivedConfig.get(TEST_NUMBER), "If you see " +
                        "this " +
                        "message, then sms configuration works fine", null);
            } catch (SmsException exception){
                return ErrorResponse.error(exception.getMessage(), Response.Status.BAD_REQUEST);
            }
        }

        //Remove SMS config if already available
        List<SmsConfigModel> availableSmsConfig = realmModel.getSmsConfigsStream().collect(Collectors.toList());
        availableSmsConfig.forEach(x -> {
            if(!x.getSmsProviderId().contains(WHATSAPP)){
                realmModel.removeSmsConfig(realmModel.getSmsConfigByProviderId(x.getSmsProviderId()));
            }
        });

        SmsConfigModel smsConfigModel = new SmsConfigModel();
        smsConfigModel.setSmsProviderId(smsProviderId);
        receivedConfig.remove(TEST_NUMBER);
        Map<String, String> config = new HashMap<>(receivedConfig);
        smsConfigModel.setConfig(config);
        realmModel.addSmsConfig(smsConfigModel);

        Set<String> smsSender = new HashSet<>();
        smsSender.add(smsProviderId);
        smsSender.addAll(realmModel.getSmsSenderStream().collect(Collectors.toList()));
        realmModel.setSmsSenders(smsSender);

        GeneralHelpers.resetObUpdateBasedOnSubscription(session);
        GeneralHelpers.addToCompletedStep(realmModel,
                this.getProviderId());
        return SuccessResponse.success("sms-setup-completed", Response.Status.OK);
    }

    @Override
    public boolean validate(MultivaluedMap<String, String> params, RealmModel realmModel) {
        return false;
    }

    @Override
    public boolean isCompleted(RealmModel realmModel) {
        boolean markedComplete = GeneralHelpers.checkForCurrentStep(realmModel, this.getProviderId());
        boolean hasSmsConfig = realmModel.getSmsConfigsStream().anyMatch(x -> !x.getSmsProviderId().equals(WHATSAPP));
        // A skippable step (on-prem) that the admin marked complete by skipping
        // counts as complete even without an SMS provider configured — otherwise
        // onboarding loops back to this step forever. Cloud (non-skippable) still
        // requires an actual SMS config.
        return markedComplete && (hasSmsConfig || isSkippable());
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String getTitle() {
        return "intro-add-sms-provider-title";
    }

    @Override
    public String getDescription() {
        return Objects.equals(session.getContext().getRealm().getName(), Config.getAdminRealm()) ? Constants.OB_ADMIN_MASTER_SMS_DESC : Constants.OB_ADMIN_SMS_DESC;
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
    public String getProviderId() {
        return Constants.OB_ADMIN_SMS_SETUP;
    }

    @Override
    public boolean isSkippable() {
        // On-prem: SMS provider (Twilio) is optional — let the admin skip it so
        // onboarding isn't walled by a mandatory paid-vendor config. Cloud keeps
        // it mandatory.
        return Version.OFFERING.equalsIgnoreCase(ON_PREMISE);
    }

    @Override
    public boolean shouldIncludeBasedOnMode() {
        // On-prem has no SMS/Twilio provider — hide this onboarding step entirely.
        return !Version.OFFERING.equalsIgnoreCase(ON_PREMISE);
    }

    @Override
    public String getId() {
        return Constants.OB_ADMIN_SMS_SETUP;
    }

}
