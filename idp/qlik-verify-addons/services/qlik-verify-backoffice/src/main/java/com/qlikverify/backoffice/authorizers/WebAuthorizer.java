package com.qlikverify.backoffice.authorizers;

import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.ciba.AuthenticationChannelRequest;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.CibaAuthProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.qlikverify.backoffice.Constants.*;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.updateResult;

public class WebAuthorizer extends AbstractCibaAuthorizer implements CibaAuthProvider, CibaAuthProviderFactory {
    private static final Logger logger = Logger.getLogger(WebAuthorizer.class);
    public static final String SHORTENED_URL_ID = "id";
    public static final String TAG = "WebAuthorizer: ";
    private static final String CIBA_CONSENT = "ciba.consent";
    public static final String SP_DEVICE_ID = "sp@device_id";
    public static final String OPEN_IN_NEW_WINDOW = "open-in-new-window";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WebAuthorizer.class);

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "default-authorizer-title";
    }

    @Override
    public CibaAuthProvider setEventBuilder(EventBuilder eventBuilder) {
        this.event = eventBuilder;
        return this;
    }

    @Override
    public boolean onAuthorize(ClientModel clientModel, RealmModel realmModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel,
                               AuthenticationChannelRequest request, CibaStateModel state) {
        try {
            if (request.getBindingMessage() != null) {
                realmModel.addAttribute(state.getRequestId(), "hidden_ua_" + BINDING_MESSAGE,
                        JsonSerialization.writeValueAsString(request.getBindingMessage()));
            }
        } catch (IOException e) {
            logger.error("Failed to parse binding message");
            throw new RuntimeException(e);
        }

        String oidcUrl = getSecuredUrl(session, clientModel, cibaAuthConfigModel, state.getCibaStateId(), getId(), request);
        if (oidcUrl == null)
            return false;

        RealmModel realm = session.getContext().getRealm();
        String notifyWay = cibaAuthConfigModel.getConfig().get(CIBA_NOTIFY);
        if (notifyWay.equals(Constants.CIBA_EMAIL)) {
            return sendEmail(cibaAuthConfigModel, request, realm, userModel, oidcUrl);
        }

        if (notifyWay.equals(CIBA_SMS)) {
            return sendSMS(cibaAuthConfigModel, request, realm, userModel, oidcUrl, configProperties);
        }

        if (notifyWay.equals(Constants.CIBA_WHATSAPP)) {
            return sendWhatsapp(request, realm, userModel, oidcUrl);
        }

//        if(notifyWay.equals(Constants.CIBA_API)) {
//            realmModel.addAttribute(state.getRequestId(),"transaction_is_api", String.valueOf(true));
//            return true;
//        }

        if (notifyWay.equals(CIBA_LINK)) {
            realmModel.addAttribute(state.getRequestId(), "transaction_link", oidcUrl);
            return true;
        }

        if (notifyWay.equals(CIBA_SDK)) {
            realmModel.addAttribute(state.getRequestId(), "transaction_link", oidcUrl);
            realmModel.addAttribute(state.getRequestId(), "open_in_new_tab", cibaAuthConfigModel.getConfig().getOrDefault(OPEN_IN_NEW_WINDOW, "false"));
            return true;
        }

        return false;
    }


    @Override
    public void onAuthCompleted(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel
            , String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {
        logger.debug("Default ciba authorizer:: onAuthCompleted ");
        session.getContext().setClient(clientModel);
        RealmModel realm = session.getContext().getRealm();
        updateTransaction(cibaState.getRequestId(), clientModel, userModel, realm);
        try {
            String authCode = (String) response.get("authorization_code");
            logger.debug("Default ciba authorizer:: authCode:: " + authCode);
            if (authCode == null || authCode.isEmpty()) {
                throw new Exception("Web Authorizer:: No Authorization code received");
            }
            // In-process authorization-code validation (was a synchronous loopback HTTP POST to this
            // server's own /token endpoint, 120s*retries — under load it let the JTA reaper close
            // the request EntityManager before event.success()). Minted token was discarded.
            OAuth2CodeParser.ParseResult parseResult = OAuth2CodeParser.parseCode(session, authCode, realm, event);
            if (parseResult.isIllegalCode() || parseResult.isExpiredCode()) {
                throw new Exception("Web Authorizer:: Invalid or expired authorization code");
            }
            if (parseResult.getClientSession() == null
                    || !clientModel.getId().equals(parseResult.getClientSession().getClient().getId())) {
                throw new Exception("Web Authorizer:: Authorization code does not match client");
            }
            realm.removeCibaState(cibaState);

            HashMap<String, String> params = new HashMap<>();
            params.put("user_id", userModel.getId());

            sendSuccess(callbackUrl, cibaState, event, params);
        } catch (ErrorResponseException e) {
            logger.error("ErrorResponseException::" + e.getMessage());
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail("reason", e.getErrorDescription());
            event.success();
            sendUnAuthorized(callbackUrl, cibaState, event, null);
        } catch (Exception err) {
            logger.error("Exception::" + err.getMessage());
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail("reason", err.getMessage());
            event.success();
            sendUnAuthorized(callbackUrl, cibaState, event, null);
        }

    }

    @Override
    public void onAuthFailed(ClientModel clientModel, UserModel userModel, String callbackUrl,
                             CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {
        RealmModel realm = session.getContext().getRealm();
        realm.removeCibaState(cibaState);
        HashMap<String, String> params = new HashMap<>();
        if (userModel != null) {
            params.put("user_id", userModel.getId());
            updateTransaction(cibaState.getRequestId(), clientModel, userModel, realm);
        }
        if (response == null && userModel != null) {
            updateResult(session, realm, clientModel, userModel, cibaState.getRequestId());
        }
        sendUnAuthorized(callbackUrl, cibaState, event, params);
    }

    @Override
    public boolean validateUserDetails(UserModel user, RealmModel realm, CibaAuthConfigModel cibaAuthConfigModel, String requestId) {
        if (cibaAuthConfigModel != null) {
            String cibaAuthProviderId = cibaAuthConfigModel.getCibaAuthProviderId();

            if (cibaAuthProviderId.equals(getId())) {
                String notifyWay = cibaAuthConfigModel.getConfig().get("ciba-notify");
                if (notifyWay.equals("Email")) {
                    return validateUserEmailDetails(user);
                }

                if (notifyWay.equals("Sms") || notifyWay.equals("Whatsapp")) {
                    return validateUserPhoneDetails(user, realm);
                }

            }
        }
        return true;
    }

    private boolean validateUserEmailDetails(UserModel user) {
        if (user.getEmail() == null || user.getEmail().isEmpty() || !GeneralHelper.isValidEmail(user.getEmail())) {
            event.error("Invalid email details for email notification");
            return false;
        }
        return true;
    }

    private boolean validateUserPhoneDetails(UserModel user, RealmModel realm) {
        if (user.getMobileNumber() == null || user.getMobileNumber().isEmpty() || !GeneralHelper.isValidPhoneNumber(user, realm)) {
            event.error("Enter a valid phone number without country code");
            return false;
        }
        return true;
    }

    @Override
    public String getStep1(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String actionFlow) {
        String notifyWay = cibaAuthConfigModel.getConfig().get(CIBA_NOTIFY);
        if (notifyWay.equals(CIBA_EMAIL)) {
            return emailOrLinkStepConfiguration(userModel, clientModel, actionFlow);
        }

        if (notifyWay.equals(CIBA_SMS) || notifyWay.equals(CIBA_WHATSAPP)) {
            return smsOrWhatsappStepConfiguration(userModel, clientModel, actionFlow);
        }

        if (notifyWay.equals(CIBA_LINK)) {
            return emailOrLinkStepConfiguration(userModel, clientModel, actionFlow);
        }


        if (notifyWay.equals(CIBA_SDK)) {
            String data = "{\n" +
                    "    \"user\": {\n" +
                    "       \"email\":\"" + userModel.getEmail() + "\",\n" +
                    "       \"metadata\": {\n" +
                    "           \"device_id\": \"@@device id received at your request@@\"\n" +
                    "        }\n" +
                    "    }\n";
            boolean isShowConsent = Boolean.parseBoolean(clientModel.getAttribute("ciba.consent"));
            if (isShowConsent) {
                data += "  ,  \"message\": {\n" +
                        "        \"consent\":{\n" +
                        "           \"type\":\"string\",\n" +
                        "           \"title\":\"Verification Request\",\n" +
                        "           \"message\": \"Test Verification from " + clientModel.getName() + "\"\n" +
                        "         }\n" +
                        "    }\n" +
                        "}";
            } else {
                data += "}";
            }
            return GeneralHelper.updateUserMetadata(session, clientModel, data, session.getContext().getRealm(), actionFlow);
        }

        return "Invalid Settings";
    }

    private String emailOrLinkStepConfiguration(UserModel userModel, ClientModel clientModel, String actionFlow) {
        String data = "{\n" +
                "    \"user\" : {\n" +
                "        \"email\":\"" + userModel.getEmail() + "\"\n" +
                "    }\n";
        boolean isShowConsent = Boolean.parseBoolean(clientModel.getAttribute(CIBA_CONSENT));
        if (isShowConsent) {
            data += "  ,  \"message\": {\n" +
                    "        \"consent\":{\n" +
                    "           \"type\":\"string\",\n" +
                    "           \"title\":\"Verification Request\",\n" +
                    "           \"message\": \"Test Verification from " + clientModel.getName() + "\"\n" +
                    "         }\n" +
                    "    }\n" +
                    "}";
        } else {
            data += "}";
        }

        return GeneralHelper.updateUserMetadata(session, clientModel, data, session.getContext().getRealm(), actionFlow);
    }

    private String smsOrWhatsappStepConfiguration(UserModel userModel, ClientModel clientModel, String actionFlow) {
        String mobileNumber = userModel.getMobileNumber() == null ? "No Phone available for the user" :
                userModel.getMobileNumber();
        String mobileRegion = userModel.getMobileNumberRegion() == null ? "No Phone region available for the user" :
                userModel.getMobileNumberRegion();
        String data = "{\n" +
                "    \"user\": {\n" +
                "            \"phone\": \"" + mobileNumber + "\",\n" +
                "            \"phoneRegion\": \"" + mobileRegion + "\"\n" +
                "    }\n";
        boolean isShowConsent = Boolean.parseBoolean(clientModel.getAttribute(CIBA_CONSENT));
        if (isShowConsent) {
            data += "  ,  \"message\": {\n" +
                    "        \"consent\":{\n" +
                    "           \"type\":\"string\",\n" +
                    "           \"title\":\"Verification Request\",\n" +
                    "           \"message\": \"Test Verification from " + clientModel.getName() + "\"\n" +
                    "         }\n" +
                    "    }\n" +
                    "}";
        } else {
            data += "}";
        }
        return GeneralHelper.updateUserMetadata(session, clientModel, data, session.getContext().getRealm(), actionFlow);
    }


    @Override
    public String getStep2(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel) {
        String notifyWay = cibaAuthConfigModel.getConfig().get(CIBA_NOTIFY);
        if (notifyWay.equals(CIBA_EMAIL)) {
            return "Check your email for link to proceed";
        }

        if (notifyWay.equals(CIBA_SMS)) {
            return "Check your messages for link to proceed";
        }
        if (notifyWay.equals(CIBA_WHATSAPP)) {
            return "Check your whatsapp for link to proceed";
        }

        if (notifyWay.equals(CIBA_LINK)) {
            return "Click the link provided under 'transaction_link' key";
        }

        if (notifyWay.equals(CIBA_SDK)) {
            return "Check your web browser to proceed";
        }
        return "Invalid Settings";
    }


    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(VERIFICATION_EXPIRY_CONFIG);
        property.setLabel("ciba-link-expiry-duration");
        property.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        property.setHelpText("ciba-link-expiry-duration-helptext");
        property.setDefaultValue("120s");
        configProperties.add(property);

        List<String> verificationTypes = new ArrayList<>();
        verificationTypes.add(Constants.CIBA_EMAIL);
        verificationTypes.add(CIBA_SMS);
        verificationTypes.add(Constants.CIBA_WHATSAPP);
        verificationTypes.add(CIBA_LINK);
        verificationTypes.add(CIBA_SDK);
        property = new ProviderConfigProperty();
        property.setName(CIBA_NOTIFY);
        property.setLabel("notification-type-label");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(verificationTypes);
        property.setDefaultValue(CIBA_LINK);
        property.setHelpText("notification-type-hlp-txt");
        property.setRequired(true);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(SMS_TEMPLATE_NAME);
        property.setLabel("sms-template-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText("sms-template-hlp-txt");
        Map<String, Object> dependsOn = new HashMap<>();
        dependsOn.put(CIBA_NOTIFY, CIBA_SMS);
        property.setDependsOn(dependsOn);
        property.setDefaultValue("sms-default-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(OPEN_IN_NEW_WINDOW);
        property.setLabel(OPEN_IN_NEW_WINDOW + "-label");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        property.setHelpText(OPEN_IN_NEW_WINDOW + "-hlp-txt");
        property.setDependsOn(new HashMap<String, Object>() {{
            put(CIBA_NOTIFY, CIBA_SDK);
        }});
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("whitelist_host");
        property.setLabel("whitelist-host-label");
        property.setDependsOn(new HashMap<String, Object>() {{
            put(CIBA_NOTIFY, CIBA_SDK);
        }});
        property.setMaxVal(2000);
        property.setRequired(true);
        property.setHelpText("whitelist-host-hlp-txt");
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("whitelist_host_restrict_https");
        property.setLabel("whitelist-host-restrict-https-label");
        property.setDependsOn(new HashMap<String, Object>() {{
            put(CIBA_NOTIFY, CIBA_SDK);
        }});
        property.setHelpText("whitelist-host-restrict-https-hlp-txt");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("whitelist_host_pub_key");
        property.setLabel("whitelist-host-pub-key-label");
        property.setDependsOn(new HashMap<String, Object>() {{
            put(CIBA_NOTIFY, CIBA_SDK);
            put("whitelist_host_restrict_https", "true");
        }});
        property.setRequired(true);
        property.setHelpText("whitelist-host-pub-key-hlp-txt");
        property.setType(ProviderConfigProperty.TEXT_TYPE);
        configProperties.add(property);
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        setupSmsSenders(session, configProperties);
        setupWhatsappSenders(session, configProperties);
        setupEmailSenders(session, configProperties);
        setupWebSDK(session);
        return configProperties;
    }

    private void setupWebSDK(KeycloakSession session) {
        CibaAuthConfigModel config = session.getContext().getRealm().getCibaAuthConfigByProviderId(getId(),
                session.getContext().getClient().getClientId(), session.getContext().getRealm().getId());
        if (config != null) {

            Optional<ProviderConfigProperty> providerConfigProperty =
                    configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("web_script_desc")).findFirst();
            ProviderConfigProperty provider;
            if (!providerConfigProperty.isPresent()) {
                provider = new ProviderConfigProperty();
            } else {
                provider = providerConfigProperty.get();
            }
            provider.setName("web_script_desc");
            provider.setLabel("integeration-steps");
            provider.setDependsOn(new HashMap<String, Object>() {{
                put(CIBA_NOTIFY, CIBA_SDK);
            }});
            provider.setType(ProviderConfigProperty.CONFIG_TYPE);
            List<ProviderConfigProperty> subConfigs = new ArrayList<>();
            ProviderConfigProperty subprovider = new ProviderConfigProperty();
            subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            subprovider.setHelpText("web-sdk-step-1-helper");
            subConfigs.add(subprovider);
            subprovider = new ProviderConfigProperty();
            subprovider.setType(ProviderConfigProperty.NOTIFICATION_CODE);
            subprovider.setLabel("step-1-script");
            String env = EnvironmentReader.getValue("ENV");
            String gh = "dev-web-push";
            if (env != null && env.equals("prod")) {
                gh = "web-push";
            }
            subprovider.setHelpText("\n    <script type=\"text/javascript\">\n" +
                    "        (function () {\n" +
                    "            d = document;\n" +
                    "            link = d.createElement('link');\n" +
                    "            link.rel = 'stylesheet';\n" +
                    "            link.type = 'text/css';\n" +
                    "            link.href = \"https://cdn.jsdelivr.net/gh/ezto-io/" + gh + "/eztoverify.min.css\";" +
                    "\n" +
                    "            d.getElementsByTagName(\"head\")[0].appendChild(link);\n" +
                    "\n" +
                    "            s = d.createElement(\"script\");\n" +
                    "            s.src = \"https://cdn.socket.io/4.7.2/socket.io.min.js\";\n" +
                    "            d.getElementsByTagName(\"head\")[0].appendChild(s);\n" +
                    "\n" +
                    "            s = d.createElement(\"script\");\n" +
                    "            s.src = \"https://cdn.jsdelivr.net/gh/ezto-io/" + gh + "/eztoverify.min.js\";\n" +
                    "            d.getElementsByTagName(\"head\")[0].appendChild(s);\n" +
                    "        })();\n" +
                    "    </script>");
            subConfigs.add(subprovider);
            subprovider = new ProviderConfigProperty();
            subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            subprovider.setHelpText("web-sdk-step-2-helper");
            subConfigs.add(subprovider);

            subprovider = new ProviderConfigProperty();
            subprovider.setType(ProviderConfigProperty.NOTIFICATION_CODE);
            String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm());
            String pushUrl =
                    workspaceUrl + "/auth/realms/api/ezto_web_push/" + session.getContext().getClient().getClientId() + "/" + session.getContext().getRealm().getName() + "/register";
            subprovider.setHelpText("\n    let ez = new eztoverify();\n" +
                    "    ez.request({request: {}},{api:\"" + pushUrl + "\", apiVersion:\"" + 1 + "\"},function(res){\n" +
                    "        //Handle the result here;\n" +
                    "    });");
            subprovider.setLabel("step-2-script");
            subConfigs.add(subprovider);
            provider.setConfigProperties(subConfigs);

            if (!providerConfigProperty.isPresent()) {
                configProperties.add(provider);
            }

        }
    }


    @Override
    public CibaAuthProvider create(KeycloakSession session) {
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
    public boolean onSave(KeycloakSession session, HashMap<String, String> config) {
        if (!config.get(CIBA_NOTIFY).equals(CIBA_SDK))
            return true;

        List<String> hostUrls = Optional.ofNullable(config.get(WHITELIST_HOST_URL))
                .map(urls -> Arrays.stream(urls.split("##")))
                .orElseGet(Stream::empty)
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (GeneralHelper.isValidUrl(hostUrls)) {
            GeneralHelper.adminEvent(session, OperationType.CONFIG_CHECK_FAILED, ResourceType.BACKOFFICE_AUTH_CONFIG);
            throw new IllegalArgumentException("invalid-config-details");
        }

        Map<String, String> headers = new HashMap<>();
        headers.putAll(session.getContext().getRealm().getBrowserSecurityHeaders());

        String csp = headers.getOrDefault(CONTENT_SECURITY_POLICY, "");
        List<String> cspDirectives = Arrays.stream(csp.split(";")).map(String::trim).collect(Collectors.toList());

        for (int x = 0; x < cspDirectives.size(); x++) {
            String directive = cspDirectives.get(x);

            if (directive.startsWith(FRAME_ANCESTORS)) {
                List<String> urls = Arrays.stream(directive.split("\\s+"))
                        .filter(y -> !y.isEmpty())
                        .collect(Collectors.toList());

                // Add only missing hostUrls
                List<String> updatedUrls = Stream.concat(urls.stream(), hostUrls.stream()
                                .filter(y -> !urls.contains(y))) // Ensure exact match check
                        .distinct()
                        .collect(Collectors.toList());

                cspDirectives.set(x, String.join(" ", updatedUrls));
            }
        }


        csp = String.join(";", cspDirectives);
        headers.put(CONTENT_SECURITY_POLICY, csp);
        session.getContext().getRealm().setBrowserSecurityHeaders(headers);
        return true;
    }

    @Override
    public boolean onDelete(KeycloakSession session, HashMap<String, String> config) {
        String notificationWay = config.get(CIBA_NOTIFY);
        if (!notificationWay.equals(CIBA_SDK))
            return true;

        List<String> hostUrls = Arrays.stream(config.get(WHITELIST_HOST_URL).split("##")).collect(Collectors.toList());
        Map<String, String> browserHeaders = session.getContext().getRealm().getBrowserSecurityHeaders();
        Map<String, String> headers = new HashMap<>();
        headers.putAll(browserHeaders);
        String csp = headers.get(CONTENT_SECURITY_POLICY);
        List<String> cspSplit = Arrays.stream(csp.split(";")).collect(Collectors.toList());
        for (int x = 0; x < cspSplit.size(); x++) {
            if (cspSplit.get(x).trim().startsWith(FRAME_ANCESTORS)) {
                final String[] frame = {cspSplit.get(x)};
                List<String> urls = Arrays.stream(cspSplit.get(x).trim().split("\\s+")).collect(Collectors.toList());
                hostUrls.stream().filter(y -> !GeneralHelper.isWhiteListHostUrlConfiguredInOtherApps(session, y, session.getContext().getClient())).forEach(y -> {
                    urls.stream().filter(string -> string.equals(y))
                            .forEach(string -> {
                                frame[0] = frame[0].replace(string, "");
                            });
                });
                cspSplit.set(x, frame[0]);
            }
        }
        csp = String.join(";", cspSplit);
        headers.put(CONTENT_SECURITY_POLICY, csp);
        session.getContext().getRealm().setBrowserSecurityHeaders(headers);
        return true;
    }

    @Override
    public boolean onUpdate(KeycloakSession session, HashMap<String, String> oldConfig,
                            HashMap<String, String> newConfig) {
        String notificationWay = newConfig.get(CIBA_NOTIFY);
        if (!notificationWay.equals(CIBA_SDK) && !oldConfig.get(CIBA_NOTIFY).equals(CIBA_SDK))
            return true;

        if (oldConfig.get(CIBA_NOTIFY).equals(CIBA_SDK) && !notificationWay.equals(CIBA_SDK))
            return onDelete(session, oldConfig);

        if (!oldConfig.get(CIBA_NOTIFY).equals(CIBA_SDK) && notificationWay.equals(CIBA_SDK))
            return onSave(session, newConfig);

        List<String> oldUrls = Optional.ofNullable(oldConfig.get(WHITELIST_HOST_URL))
                .map(urls -> Arrays.stream(urls.split("##")))
                .orElseGet(Stream::empty)
                .map(String::trim)
                .filter(y -> !y.isEmpty())
                .collect(Collectors.toList());

        List<String> newUrls = Arrays.stream(newConfig.get((WHITELIST_HOST_URL))
                        .split("##")).map(String::trim)
                .filter(y -> !y.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (GeneralHelper.isValidUrl(newUrls)) {
            GeneralHelper.adminEvent(session, OperationType.CONFIG_CHECK_FAILED, ResourceType.BACKOFFICE_AUTH_CONFIG);
            throw new IllegalArgumentException("invalid-config-details");
        }
        Map<String, String> headers = new HashMap<>();
        headers.putAll(session.getContext().getRealm().getBrowserSecurityHeaders());
        String csp = headers.get(CONTENT_SECURITY_POLICY);
        List<String> cspSplit = Arrays.stream(csp.split(";")).collect(Collectors.toList());

        for (int x = 0; x < cspSplit.size(); x++) {
            if (cspSplit.get(x).trim().startsWith(FRAME_ANCESTORS)) {
                String frame = cspSplit.get(x).trim();

                List<String> urls = Arrays.stream(frame.split("\\s+"))
                        .filter(y -> !y.isEmpty())
                        .collect(Collectors.toList());

                List<String> updatedUrls = urls.stream()
                        .filter(url -> oldUrls.stream()
                                .noneMatch(oldUrl -> oldUrl.equals(url) &&
                                        !GeneralHelper.isWhiteListHostUrlConfiguredInOtherApps(session, oldUrl, session.getContext().getClient())))
                        .collect(Collectors.toList());

                newUrls.stream()
                        .distinct()
                        .filter(url -> !updatedUrls.contains(url))
                        .forEach(updatedUrls::add);

                cspSplit.set(x, String.join(" ", updatedUrls));
            }
        }

        csp = String.join(";", cspSplit);
        headers.put(CONTENT_SECURITY_POLICY, csp);
        session.getContext().getRealm().setBrowserSecurityHeaders(headers);
        return true;
    }

    @Override
    public void onCleanUp(KeycloakSession session, RealmModel realm, ClientModel client) {
        CibaAuthConfigModel authenticationConfig = realm.getCibaAuthConfigByClientId(client.getClientId(), realm.getId());
        CibaAuthProviderFactory providerFactory = (CibaAuthProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaAuthProvider.class, authenticationConfig.getCibaAuthProviderId());

        if (providerFactory != null) {
            providerFactory.onDelete(session, (HashMap<String, String>) authenticationConfig.getConfig());
        }
        if (authenticationConfig != null) {
            realm.removeCibaAuthConfig(authenticationConfig);
        }
    }

    @Override
    public String getId() {
        return "ciba-default-managed-auth";
    }

}
