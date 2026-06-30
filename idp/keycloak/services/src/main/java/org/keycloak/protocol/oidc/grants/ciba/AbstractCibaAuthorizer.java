package org.keycloak.protocol.oidc.grants.ciba;

import com.qlikverify.helpers.TwilioHelper;
import com.qlikverify.transaction.CallBackService;
import com.qlikverify.transaction.TransactionStatus;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.ciba.AuthenticationChannelRequest;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.ciba.models.UserData;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.component.ComponentModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.grants.ciba.util.CibaHook;
import org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.DefaultClientRegistrationPolicies;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AESHelper;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Year;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.keycloak.authentication.authenticators.utils.GeneralHelper.getExpiryDuration;
import static org.keycloak.authentication.authenticators.utils.GeneralHelper.getExpiryDurationInRawSec;
import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.getTTLMessage;
import static org.keycloak.provider.ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE;


public abstract class AbstractCibaAuthorizer {

    private static final Logger logger = Logger.getLogger(AbstractCibaAuthorizer.class);
    protected KeycloakSession session;
    protected EventBuilder event;
    public static final String EMAIL_SENDER = "email-sender";
    public static final String EMAIL_TITLE = "email-title";
    public static final String EMAIL_TITLE_LABEL = "email-title-label";
    public static final String EMAIL_BODY = "email-body";
    public static final String EMAIL_BODY_LABEL = "email-body-label";
    public static final String EMAIL_SUBJECT = "email-subject";
    public static final String EMAIL_SUBJECT_LABEL = "email-subject-label";
    public static final String SMS_SENDER = "sms-sender";
    public static final String SMS_SENDER_LABEL = "sms-sender-label";
    public static final String CIBA_NOTIFY = "ciba-notify";
    public static final String CIBA_EMAIL = "Email";
    public static final String CIBA_SDK = "SDK";
    public static final String CIBA_LINK = "Link";
    public static final String CIBA_WHATSAPP = "Whatsapp";
    public static final String CIBA_SMS = "Sms";
    public static final String EMAIL_SENDER_LABEL = "email-sender-label";
    public static final String WHATSAPP_SENDER = "whatsapp-sender";
    public static final String WHATSAPP_SENDER_LABEL = "whatsapp-sender-label";
    public static final String SHORTENED_URL_ID = "id";
    public static final String DIVIDER = "----------------------";
    public static final String SMS_TEMPLATE_NAME = "sms-template";
    protected HttpUtility httpUtility = new HttpUtility();

    protected String getSecuredUrl(KeycloakSession session, ClientModel clientModel, CibaAuthConfigModel cibaAuthConfigModel, String state, String authorizerId, AuthenticationChannelRequest request) {
        RealmModel realm = session.getContext().getRealm();
        boolean showConsent = Boolean.parseBoolean(clientModel.getAttributes().get("ciba.consent"));
        long tolerance = getExpiryDurationInRawSec(cibaAuthConfigModel.getConfig().get("verify-expiry-time"));
        String redirectUrl = getRedirectUrl(clientModel, state, realm, authorizerId, tolerance, showConsent);
        if (redirectUrl == null) return null;
        Map<String, String> attributes = new HashMap<>();
        try {
            attributes.put("binding_message", BindingMessage.encode(request.getBindingMessage(), clientModel));
            attributes.put("login_hint", UserData.encode(request.getLoginHint(), clientModel));
            attributes.put("state", Base64.getEncoder().encodeToString(AESHelper.encrypt(state, clientModel.getSecret().substring(0, 32)).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            event.detail("requestId", (String) request.getAdditionalParameters().get("requestId")).error(authorizerId + e.getMessage());
        }
        String oidcUrl = ClientResource.getOIDCUrl(clientModel, session, realm, redirectUrl, attributes);
        logger.debug(DIVIDER);
        logger.debug("TRANSACTION OIDC URL : " + oidcUrl);
        logger.debug(DIVIDER);
        HttpUtility httpUtility = new HttpUtility();
        HashMap<String, String> headers = new HashMap<>();
        String key = EnvironmentReader.getValue("SHORTENER_KEY");
        if (key == null) {
            event.detail("requestId", (String) request.getAdditionalParameters().get("requestId")).error("MobileAuthorizer: No apikey defined for URL Shortener");
            return null;
        }
        headers.put("x-api-key", EnvironmentReader.getValue("SHORTENER_KEY"));

        HashMap<String, Object> body = new HashMap<>();
        body.put("url", oidcUrl);
        body.put("ttl", TimeUnit.MILLISECONDS.toSeconds(getExpiryDuration(cibaAuthConfigModel.getConfig().get("verify-expiry-time"))));
        try (okhttp3.Response response = httpUtility.callRequest("application/json", "POST", EnvironmentReader.getValue("ENV") == null ? EnvironmentReader.getValue("SHORTENER_HTTPS_URL") :
                EnvironmentReader.getValue("SHORTENER_HTTP_URL"), JsonSerialization.writeValueAsString(body), null, headers, 1)) {

            if (response.isSuccessful()) {
                Map<String, String> parsed = JsonSerialization.readValue(response.body().string(), Map.class);
                if (parsed.get(SHORTENED_URL_ID) != null) {
                    oidcUrl =
                            EnvironmentReader.getValue("SHORTENER_HTTPS_URL") + "/" + parsed.get(SHORTENED_URL_ID);
                    logger.info("----------------------");
                    logger.info("SHORTEND OIDC URL : " + oidcUrl);
                    logger.info("----------------------");
                }
                if (oidcUrl == null) {
                    return null;
                }
                return oidcUrl;
            }
        } catch (IOException e) {
            event.detail("requestId", (String) request.getAdditionalParameters().get("requestId")).error(authorizerId + e.getMessage());
            return null;
        }

        return null;
    }

    protected void updateTransaction(String trxId, ClientModel clientModel, UserModel userModel, RealmModel realmModel) {
        if (userModel == null) {
            logger.info("userModel is null :: updateTransaction()");
            return;
        }
        Optional<ComponentModel> optionalComponentModel = realmModel.getComponentsStream(realmModel.getId()).filter(x -> x.getProviderId().equalsIgnoreCase("transaction-policy-checker")).findFirst();
        DefaultClientRegistrationPolicies.addTransactionPolicy(realmModel, optionalComponentModel.isPresent());
        ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "transaction-policy-checker", new HashMap<String, Object>() {{
            put("userId", userModel.getId());
            put("clientId", clientModel.getClientId());
            put("trxId", trxId);
        }});
    }

    protected void updateIVRTransaction(ClientModel clientModel, UserModel userModel, RealmModel realmModel, String toPhone, CibaStateModel cibaStateModel, String basicAuth) {
        Optional<ComponentModel> IvrPolicy = realmModel.getComponentsStream(realmModel.getId()).filter(x -> x.getProviderId().equalsIgnoreCase("ivr-transaction-policy-checker")).findFirst();

        DefaultClientRegistrationPolicies.addIvrTransactionPolicyChecker(realmModel, IvrPolicy.isPresent());

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                        @Override
                        public void run(KeycloakSession innerSession) {
                            Resteasy.pushContext(HttpHeaders.class, session.getContext().getRequestHeaders());
                            Resteasy.pushContext(ClientConnection.class, session.getContext().getConnection());
                            Resteasy.pushContext(KeycloakSession.class, session);
                            Resteasy.pushContext(UriInfo.class, session.getContext().getUri());
                            Resteasy.pushContext(UserModel.class, userModel);
                            Resteasy.pushContext(ClientModel.class, clientModel);
                            Resteasy.pushContext(CibaStateModel.class, cibaStateModel);
                            Resteasy.pushContext(RealmModel.class, realmModel);
                            RealmModel realm = innerSession.realms().getRealm(realmModel.getId());
                            UserModel user = innerSession.users().getUserById(realm, userModel.getId());
                            innerSession.getContext().setRealm(realm);
                            HashMap<String, Object> attributes = new HashMap<>();
                            TrxModel trx = realm.getTrx(cibaStateModel.getRequestId());
                            logger.info("updateIVRTransaction initiated");
                            if (trx != null && trx.getAttributes() != null && trx.getAttributes().containsKey("ik@flow.response.execution_context")) {
                                logger.info("updateIVRTransaction price calculations started");
                                String executionContext = trx.getAttributes().get("ik@flow.response.execution_context");
                                attributes.put("price", TwilioHelper.getCallPricing(innerSession, executionContext, basicAuth));
                            }
                            attributes.put("userId", user.getId());
                            attributes.put("clientId", clientModel.getClientId());
                            attributes.put("to_phone", toPhone);
                            attributes.put("trxId", cibaStateModel.getCibaStateId());

                            ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "ivr-transaction-policy-checker", attributes);
                        }
                });
            }
        });
    }

    protected boolean sendWhatsapp(AuthenticationChannelRequest request, RealmModel realm, UserModel userModel, String oidcUrl) {
        SmsSenderProvider smsSenderProvider = getSmsProvider();
        if (smsSenderProvider == null) {
            event.error("DefaultCibaAuthorizer: Whatsapp provider is not set");
            return false;
        }

        SmsConfigModel smsConfigModel = getWhatsappProvider(realm);
        if (smsConfigModel == null) {
            String workspaceName =
                    realm.isEnabledOwnWhatsappProvider() && !realm.getName().equalsIgnoreCase(Config.getAdminRealm()) ? realm.getName() : "master";
            event.error("DefaultCibaAuthorizer: Whatsapp provider is not configured in " + workspaceName + " workspace");
            return false;
        }

        smsSenderProvider.setProviderId(smsConfigModel.getSmsProviderId());
        Map<String, Object> smsPayload = new HashMap<>();
        List<String> datas = new ArrayList<>();
        Map<String, String> metadata = request.getBindingMessage() != null ? request.getBindingMessage().getMetadata() : null;
        if (metadata != null && !metadata.isEmpty()) {
            String fromMessage = metadata.get("transaction_from");
            datas.add(fromMessage == null || fromMessage.isEmpty() ? realm.getDisplayName() : fromMessage);
        } else {
            datas.add(realm.getDisplayName());
        }
        datas.add(oidcUrl);
        smsPayload.put("templateType", "CIBA");
        smsPayload.put("messageParts", datas.toArray());
        String twilioTransactionTemplateSid = session.getContext().getRealm().isEnabledOwnWhatsappProvider() ? session.getContext().getRealm().getAttribute("twilio-transaction-template-sid") : session.realms().getRealmByName(Config.getAdminRealm()).getAttribute("twilio-transaction-template-sid");
        smsPayload.put("messageContentSid", twilioTransactionTemplateSid);
        if(request != null && request.getAdditionalParameters().containsKey("requestId")) {
            smsPayload.put("requestId", request.getAdditionalParameters().get("requestId"));
        }
        try {
            if (userModel.getMobileNumber() == null || userModel.getMobileNumber().isEmpty())
                throw new SmsException("User doesnt have mobile number");

            smsSenderProvider.sendSMS(getPhoneNumber(userModel, smsSenderProvider), null, smsPayload);
        } catch (SmsException e) {
            String workspaceName = realm.isEnabledOwnWhatsappProvider() && !realm.getName().equalsIgnoreCase(Config.getAdminRealm()) ? realm.getName() : "master";
            event.error("DefaultCibaAuthorizer: Whatsapp provider is not configured in " + workspaceName + " workspace");
            return false;
        }
        return true;
    }

    protected boolean sendSMS(CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, RealmModel realm, UserModel userModel, String oidcUrl, List<ProviderConfigProperty> configProperties) {
        SmsSenderProvider smsSenderProvider = getSmsProvider();
        if (realm.isEnabledOwnSmsProvider()) {
            smsSenderProvider.setProviderId(cibaAuthConfigModel.getConfig().get(SMS_SENDER));
        } else {
            SmsConfigModel smsConfigModel = GeneralHelper.getDefaultSmsSender(session);
            if (smsConfigModel != null) {
                smsSenderProvider.setProviderId(smsConfigModel.getSmsProviderId());
            }
        }
        if (smsSenderProvider == null || !smsSenderProvider.isConfigured() || !smsSenderProvider.isSmsSupported()) {
            event.error("DefaultCibaAuthorizer: Sms provider is not configured");
            return false;
        }

        String expiry = cibaAuthConfigModel.getConfig().get("verify-expiry-time");
        if (expiry == null) {
            expiry = "120s";
        }

        final String[] template = {cibaAuthConfigModel.getConfig().get(SMS_TEMPLATE_NAME)};
        Map<String, String> metadata = request.getBindingMessage() != null ? request.getBindingMessage().getMetadata() : null;
        if (metadata != null) {
            if (template[0] == null) {
                template[0] = (String) configProperties.stream().filter(x -> x.getName().equals(SMS_TEMPLATE_NAME)).map(x -> x.getDefaultValue()).findFirst().get();

                String fromMessage = metadata.get("transaction_from");
                template[0] = template[0].replace("{{transaction_from}}", fromMessage.isEmpty() ? realm.getDisplayName() : fromMessage);
            } else {
                metadata.forEach((k, v) -> {
                    template[0] = template[0].contains("{{" + k + "}}") ? template[0].replace("{{" + k + "}}", v) : template[0];
                });
                template[0] = template[0].contains("{{link}}") ? template[0] : template[0] + " {{link}}";
            }
        } else {
            if (template[0] == null) {
                template[0] = (String) configProperties.stream().filter(x -> x.getName().equals(SMS_TEMPLATE_NAME)).map(x -> x.getDefaultValue()).findFirst().get();

                String fromMessage = realm.getDisplayName();
                template[0] = template[0].replace("{{transaction_from}}", fromMessage.isEmpty() ? realm.getDisplayName() : fromMessage);
            } else {
                template[0] = template[0].contains("{{link}}") ? template[0] : template[0] + " {{link}}";
            }
        }

        template[0] = template[0].replace("{{link}}", oidcUrl);
        if (template[0].contains("{{transaction_from}}")) {
            template[0] = template[0].replace("{{transaction_from}}", realm.getDisplayName());
        }
        if (template[0].contains("{{expirationTime}}")) {
            template[0] = template[0].replace("{{expirationTime}}", String.valueOf(getTTLMessage(expiry).get("time")));
        }
        if (template[0].contains("{{expirationUnit}}")) {
            template[0] = template[0].replace("{{expirationUnit}}", String.valueOf(getTTLMessage(expiry).get("unit")));
        }


        try {
            if (userModel.getMobileNumber() == null || userModel.getMobileNumber().isEmpty())
                throw new SmsException("User doesnt have mobile number");

            Map<String, Object> params = new HashMap<>();
            if(request != null && request.getAdditionalParameters().containsKey("requestId")) {
                params.put("requestId", request.getAdditionalParameters().get("requestId"));
            }

            smsSenderProvider.sendSMS(getPhoneNumber(userModel, smsSenderProvider), template[0], params);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            event.error("DefaultCibaAuthorizer: " + e.getMessage());
            return false;
        }
        return true;
    }

    protected String getPhoneNumber(UserModel model, SmsSenderProvider smsProvider) throws SmsException {
        if (smsProvider == null)
            smsProvider = getSmsProvider();

        NumberFormat numberFormat = smsProvider.parseNumber(model.getMobileNumber(), model.getMobileNumberRegion().toUpperCase());
        return numberFormat.getE16Format();
    }

    protected SmsSenderProvider getSmsProvider() {
        return session.getProvider(SmsSenderProvider.class, "default");
    }

    protected boolean sendEmail(CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, RealmModel realm, UserModel userModel, String oidcUrl) {
        String emailProviderId = null;
        if (realm.isEnabledOwnEmailProvider()) {
            emailProviderId = cibaAuthConfigModel.getConfig().get(EMAIL_SENDER);
            if (emailProviderId == null || emailProviderId.isEmpty()) {
                event.error("DefaultCibaAuthorizer: Email Provider is not set for the authorizer");
                return false;
            }
        }

        Map<String, String> metadata = request.getBindingMessage() != null ? request.getBindingMessage().getMetadata() : null;
        String emSubject = cibaAuthConfigModel.getConfig().get(EMAIL_SUBJECT);
        emSubject = emSubject != null && !emSubject.isEmpty() ? emSubject : "Verify Transaction";
        String emTitle = cibaAuthConfigModel.getConfig().get(EMAIL_TITLE);
        emTitle = emTitle != null && !emTitle.isEmpty() ? emTitle : "Welcome to " + realm.getName();
        String emBody = cibaAuthConfigModel.getConfig().get(EMAIL_BODY);
        emBody = emBody != null && !emBody.isEmpty() ? emBody : "Click below link to proceed further";
        Map<String, Object> emAttrs = new HashMap<>();
        emAttrs.put("link", oidcUrl);
        emAttrs.put("year", String.valueOf(Year.now().getValue()));
        emAttrs.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        String expiry = cibaAuthConfigModel.getConfig().get("verify-expiry-time");
        if (expiry == null) {
            expiry = "120s";
        }

        if (metadata != null) {
            if (emTitle != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    emTitle = emTitle.replaceAll("@" + entry.getKey() + "@", entry.getValue());
                }
            }

            if (emBody != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    emBody = emBody.replaceAll("@" + entry.getKey() + "@", entry.getValue());
                }
            }

            if (emSubject != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    emSubject = emSubject.replaceAll("@" + entry.getKey() + "@", entry.getValue());
                }
            }
            emAttrs.put("emTitle", emTitle);
            emAttrs.put("emBody", String.format(emBody.replace("\\n", "%n")));
            emAttrs.put("emSubject", emSubject);
            emAttrs.put("expirationTime", getTTLMessage(expiry).get("time"));
            emAttrs.put("expirationUnit", getTTLMessage(expiry).get("unit"));
            emAttrs.putAll(metadata);
        } else {
            emAttrs.put("emTitle", emTitle);
            emAttrs.put("emSubject", emSubject);
            emAttrs.put("emBody", String.format(emBody.replace("\\n", "%n")));
            emAttrs.put("expirationTime", getTTLMessage(expiry).get("time"));
            emAttrs.put("expirationUnit", getTTLMessage(expiry).get("unit"));
        }

        try {
            if (userModel.getEmail() == null || userModel.getEmail().isEmpty())
                throw new EmailException("User doesnt have valid email");

            session.getProvider(EmailTemplateProvider.class)
                    .setUser(userModel)
                    .setRealm(realm)
                    .setCustomSubject(true)
                    .setEmailProviderId(emailProviderId)
                    .send(userModel.getEmail(), emSubject, "ciba-authentication.ftl", emAttrs);
        } catch (EmailException e) {
            event.error("DefaultCibaAuthorizer: " + e.getMessage());
            return false;
        } catch (ConfigException e) {
            event.error("DefaultCibaAuthorizer: " + e.getMessage());
            return false;
        }
        return true;
    }

    protected SmsConfigModel getWhatsappProvider(RealmModel realmModel) {
        if (realmModel.isEnabledOwnWhatsappProvider()) {
            Optional<SmsConfigModel> ownWpConfig = realmModel.getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
            return ownWpConfig.isPresent() ? ownWpConfig.get() : null;
        } else {
            return GeneralHelper.getDefaultWhatsappSender(session);
        }
    }


    protected String getRedirectUrl(ClientModel clientModel, String state, RealmModel realm, String id, long tolerance, boolean showConfirmation) {
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        String redirectUrl = CibaGrantType.redirectUrl(backendUriInfo.getBaseUriBuilder()).build(realm.getName()).toString();
        String clientId = clientModel.getClientId();
        String payload = state + "_" + id + "_" + String.valueOf(tolerance);
        clientModel.setAttribute("ciba.consent", String.valueOf(showConfirmation), true);
        String signature = null;
        long timeStamp = CibaHook.Util.getTimeNow();
        String signedPayload = String.format("%d.%s", timeStamp, payload);
        try {
            signature = Base64.getEncoder().encodeToString(("t=" + String.valueOf(timeStamp) + ",v1=" + CibaHook.Util.computeHmacSha256(clientModel.getSecret(), signedPayload)).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            event.error("DefaultCibaAuthorizer: " + e.getMessage());
            return null;
        }

        redirectUrl += "/" + clientId + "/" + signature + "/" + payload;
        return redirectUrl;
    }

    protected void sendUnAuthorized(String callbackUrl, CibaStateModel cibaState, EventBuilder event, HashMap<String, String> params) {
        CallBackService callBackService = session.getProvider(CallBackService.class, "default-callback");
        event.detail("requestId", cibaState.getRequestId());
        callBackService.setEventBuilder(event).verifyResult(cibaState.getCibaToken(), TransactionStatus.UNAUTHORIZED, params);
    }

    protected void sendSuccess(String callbackUrl, CibaStateModel cibaState, EventBuilder event, HashMap<String, String> params) {
        CallBackService callBackService = session.getProvider(CallBackService.class, "default-callback");
        event.detail("requestId", cibaState.getRequestId());
        callBackService.setEventBuilder(event).verifyResult(cibaState.getCibaToken(), TransactionStatus.SUCCEED, params);
    }


    protected void setupSmsSenders(KeycloakSession session, List<ProviderConfigProperty> configProperties) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(SMS_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(SMS_SENDER);
        provider.setLabel(SMS_SENDER_LABEL);
        provider.setDependsOn(new HashMap<String, Object>() {{
            put(CIBA_NOTIFY, CIBA_SMS);
        }});

        if (session.getContext().getRealm().isEnabledOwnSmsProvider()) {
            List<SmsConfigModel> providers = session.getContext().getRealm().getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && !x.getSmsProviderId().equalsIgnoreCase("whatsapp")).collect(Collectors.toList());
            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getSmsProviderId()));
            }

            provider.setRequired(true);
            if (!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("sms-sender-hlp-txt");
                provider.setHelperLinkText("");
                provider.setHelperLink("");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("");
                provider.setHelperLinkText("no-sms-reg");
                provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/sms/providers/create");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("");
            provider.setHelperLinkText("all-sms-routed-root-workspace");
            provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/realm-subscription");
        }

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    protected void setupWhatsappSenders(KeycloakSession session, List<ProviderConfigProperty> configProperties) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(WHATSAPP_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(WHATSAPP_SENDER);
        provider.setLabel(WHATSAPP_SENDER_LABEL);
        provider.setDependsOn(new HashMap<String, Object>() {{
            put(CIBA_NOTIFY, CIBA_WHATSAPP);
        }});

        if (session.getContext().getRealm().isEnabledOwnWhatsappProvider()) {
            Optional<SmsConfigModel> hasWhatsAppProvider = session.getContext().getRealm().getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();

            if (hasWhatsAppProvider.isPresent()) {
                configProperties.removeIf(x -> x.getName() != null && x.getName().equals(WHATSAPP_SENDER));
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
            provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/realm-subscription");
            if (!providerConfigProperty.isPresent()) {
                configProperties.add(provider);
            }
        }
    }

    protected void setupEmailSenders(KeycloakSession session, List<ProviderConfigProperty> configProperties) {

        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName().equals(EMAIL_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(EMAIL_SENDER);
        provider.setLabel(EMAIL_SENDER_LABEL);
        provider.setDependsOn(
                new HashMap<String, Object>() {{
                    put(CIBA_NOTIFY, CIBA_EMAIL);
                }});
        provider.setValidationType(VALIDATION_ANY_ONE_TRUE);

        if (session.getContext().getRealm().isEnabledOwnEmailProvider()) {
            List<EmailConfigModel> providers = session.getContext().getRealm().getEmailConfigsStream().collect(Collectors.toList());
            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getEmailProviderId()));
            }

            provider.setRequired(true);
            if (!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("email-config-set-sender");
                provider.setHelperLink("");
                provider.setHelperLinkText("");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("");
                provider.setHelperLinkText("no-email-reg");
                provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/email/providers/create");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("");
            provider.setHelperLinkText("all-emails-routed-root-workspace");
            provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/realm-subscription");
        }
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }

        Optional<ProviderConfigProperty> emailSubProviderOpt = configProperties.stream().filter(x -> x.getName().equals(EMAIL_SUBJECT)).findFirst();
        ProviderConfigProperty emailSubProvider;
        if (!emailSubProviderOpt.isPresent()) {
            emailSubProvider = new ProviderConfigProperty();
        } else {
            emailSubProvider = emailSubProviderOpt.get();
        }

        emailSubProvider.setLabel(EMAIL_SUBJECT_LABEL);
        emailSubProvider.setName(EMAIL_SUBJECT);
        emailSubProvider.setDependsOn(
                new HashMap<String, Object>() {{
                    put(CIBA_NOTIFY, CIBA_EMAIL);
                }});
        emailSubProvider.setValidationType(VALIDATION_ANY_ONE_TRUE);
        emailSubProvider.setRequired(true);
        emailSubProvider.setType(ProviderConfigProperty.STRING_TYPE);

        if (!emailSubProviderOpt.isPresent()) {
            configProperties.add(emailSubProvider);
        }

        Optional<ProviderConfigProperty> emailTitleProviderOpt = configProperties.stream().filter(x -> x.getName().equals(EMAIL_TITLE)).findFirst();
        ProviderConfigProperty emailTitleProvider;
        if (!emailTitleProviderOpt.isPresent()) {
            emailTitleProvider = new ProviderConfigProperty();
        } else {
            emailTitleProvider = emailTitleProviderOpt.get();
        }

        emailTitleProvider.setLabel(EMAIL_TITLE_LABEL);
        emailTitleProvider.setName(EMAIL_TITLE);
        emailTitleProvider.setDependsOn(
                new HashMap<String, Object>() {{
                    put(CIBA_NOTIFY, CIBA_EMAIL);
                }});
        emailTitleProvider.setValidationType(VALIDATION_ANY_ONE_TRUE);
        emailTitleProvider.setRequired(true);
        emailTitleProvider.setType(ProviderConfigProperty.STRING_TYPE);

        if (!emailTitleProviderOpt.isPresent()) {
            configProperties.add(emailTitleProvider);
        }

        Optional<ProviderConfigProperty> emailBodyProviderOpt = configProperties.stream().filter(x -> x.getName().equals(EMAIL_BODY)).findFirst();
        ProviderConfigProperty emailBodyProvider;
        if (!emailBodyProviderOpt.isPresent()) {
            emailBodyProvider = new ProviderConfigProperty();
        } else {
            emailBodyProvider = emailBodyProviderOpt.get();
        }

        emailBodyProvider.setLabel(EMAIL_BODY_LABEL);
        emailBodyProvider.setName(EMAIL_BODY);
        emailBodyProvider.setDependsOn(
                new HashMap<String, Object>() {{
                    put(CIBA_NOTIFY, CIBA_EMAIL);
                }});
        emailBodyProvider.setValidationType(VALIDATION_ANY_ONE_TRUE);
        emailBodyProvider.setRequired(true);
        emailBodyProvider.setType(ProviderConfigProperty.TEXT_TYPE);

        if (!emailBodyProviderOpt.isPresent()) {
            configProperties.add(emailBodyProvider);
        }
    }
}
