package com.qlikverify.backoffice.authorizers;

import com.qlikverify.backoffice.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.ciba.AuthenticationChannelRequest;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.CibaAuthProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
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

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.Constants.VERIFICATION_EXPIRY_CONFIG;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.updateResult;

public class AzureB2CAuthorizer extends AbstractCibaAuthorizer implements CibaAuthProvider, CibaAuthProviderFactory {
    private static final Logger logger = Logger.getLogger(AzureB2CAuthorizer.class);
    private static final String CIBA_CONSENT = "ciba.consent";
    public static final String AZURE_B2C_TENANT_ID = "azure-b2c-tenant-id";
    public static final String AZURE_B2C_TENANT_ID_OBJECT = "azure-b2c-tenant-id-object";
    private static final String AZURE_B2C_POLICY_DOWNLOAD = "azure-b2c-policy-download";
    private static final String AZURE_B2C_POLICY_DOWNLOAD_STEP_1 = "azure-b2c-policy-download-step-1";
    private static final String AZURE_B2C_POLICY_DOWNLOAD_STEP_2 = "azure-b2c-policy-download-step-2";

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "azure-b2c-authorizer-title";
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

        realmModel.addAttribute(state.getRequestId(), "transaction_link", oidcUrl);

        return true;
    }


    @Override
    public void onAuthCompleted(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel
            , String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {
        logger.debug("Default ciba authorizer:: onAuthCompleted ");

        RealmModel realm = session.getContext().getRealm();
        updateTransaction(cibaState.getRequestId(),clientModel, userModel, realm);
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
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail("reason", e.getErrorDescription());
            event.success();
            sendUnAuthorized(callbackUrl, cibaState, event, null);
        } catch (Exception err) {
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
        updateTransaction(cibaState.getRequestId(), clientModel, userModel, realm);
        if (response == null) {
            updateResult(session, realm, clientModel, userModel, cibaState.getRequestId());
        }
        sendUnAuthorized(callbackUrl, cibaState, event, null);
    }

    @Override
    public boolean validateUserDetails(UserModel user, RealmModel realm, CibaAuthConfigModel cibaAuthConfigModel, String requestId) {
        if (cibaAuthConfigModel != null) {
            String cibaAuthProviderId = cibaAuthConfigModel.getCibaAuthProviderId();

            if (!cibaAuthProviderId.equals(getId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getStep1(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String actionFlow) {
        return emailOrLinkStepConfiguration(userModel, clientModel);
    }

    private String emailOrLinkStepConfiguration(UserModel userModel, ClientModel clientModel) {
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
        return data;
    }

    @Override
    public String getStep2(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel) {
        return "Check your web browser to proceed";
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

        /*List<String> verificationTypes = new ArrayList<>();
        verificationTypes.add(AZURE_B2C_NEW);
        verificationTypes.add(AZURE_B2C_ALREADY);
        property = new ProviderConfigProperty();
        property.setName(AZURE_B2C_SETUP);
        property.setLabel(AZURE_B2C_SETUP);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(verificationTypes);
        property.setDefaultValue(AZURE_B2C_NEW);
        property.setHelpText("azure-setup-hlp-txt");
        configProperties.add(property);*/

    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        /*setupAzureB2CNewSetup(session);
        setupAzureB2CAlreadySetup(session);*/
        setupWebPart(session);
//        setupWebSDK(session);
        setupAzureB2CTenantId();
        setupAzureB2CTenantIdObject();
        setupAzureB2CPoliciesDownload(session);
        return configProperties;
    }

    private void setupAzureB2CPoliciesDownload(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(AZURE_B2C_POLICY_DOWNLOAD)).findFirst();
        ProviderConfigProperty provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);

        provider.setName(AZURE_B2C_POLICY_DOWNLOAD);
        provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        provider.setHelpText("follow-the-below-steps");

        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }

        providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName()
                .equals(AZURE_B2C_POLICY_DOWNLOAD_STEP_1)).findFirst();
        provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);
        provider.setName(AZURE_B2C_POLICY_DOWNLOAD_STEP_1);
        provider.setLabel(AZURE_B2C_POLICY_DOWNLOAD_STEP_1);
        provider.setHelpText("download-B2C_1A_TRUSTFRAMEWORKBASE.xml-file");
        provider.setType(ProviderConfigProperty.DOWNLOAD_TYPE);
        provider.setDownloadUrl(ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm())
                +"/auth/realms/"+session.getContext().getRealm().getName()
                +"/download/provconf/azure-b2c-trust-framework-provider/"
                +session.getContext().getClient().getClientId());

        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }

        providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName()
                .equals(AZURE_B2C_POLICY_DOWNLOAD_STEP_2)).findFirst();
        provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);
        provider.setName(AZURE_B2C_POLICY_DOWNLOAD_STEP_2);
        provider.setLabel(AZURE_B2C_POLICY_DOWNLOAD_STEP_2);
        provider.setHelpText("download-B2C_1A_SIGNIN.xml-file");
        provider.setType(ProviderConfigProperty.DOWNLOAD_TYPE);
        provider.setDownloadUrl(ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm())
                +"/auth/realms/"+session.getContext().getRealm().getName()
                +"/download/provconf/azure-b2c-signin-provider/"
                +session.getContext().getClient().getClientId());

        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }
    }

    private void setupAzureB2CTenantIdObject() {
        Optional<ProviderConfigProperty> providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(AZURE_B2C_TENANT_ID_OBJECT)).findFirst();
        ProviderConfigProperty provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);

        provider.setName(AZURE_B2C_TENANT_ID_OBJECT);
        provider.setLabel(AZURE_B2C_TENANT_ID_OBJECT);
        provider.setRequired(true);
        provider.setType(ProviderConfigProperty.STRING_TYPE);
        provider.setHelpText("azure-b2c-tenant-id-object-hlp-txt");

        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }
    }

    private void setupAzureB2CTenantId() {
        Optional<ProviderConfigProperty> providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(AZURE_B2C_TENANT_ID)).findFirst();
        ProviderConfigProperty provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);

        provider.setName(AZURE_B2C_TENANT_ID);
        provider.setLabel(AZURE_B2C_TENANT_ID);
        provider.setRequired(true);
        provider.setType(ProviderConfigProperty.STRING_TYPE);
        provider.setHelpText("azure-b2c-tenant-id-hlp-txt");

        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }
    }

    private void setupWebPart(KeycloakSession session)
    {

        Optional<ProviderConfigProperty> providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("azure_b2c_web_desc")).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("azure_b2c_web_desc");
        provider.setLabel("web-setup-instructions-label");
        provider.setType(ProviderConfigProperty.CONFIG_TYPE);
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }

        providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("whitelist_host")).findFirst();
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(WHITELIST_HOST_URL);
        provider.setLabel("whitelist-host-label");
        provider.setRequired(true);
        provider.setHelpText("whitelist-host-hlp-txt");
        provider.setType(ProviderConfigProperty.STRING_TYPE);
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }

        providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("whitelist_host_restrict_https")).findFirst();

        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("whitelist_host_restrict_https");
        provider.setLabel("whitelist-host-restrict-https-label");
        provider.setHelpText("whitelist-host-restrict-https-hlp-txt");
        provider.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }

        providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("whitelist_host_pub_key")).findFirst();

        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }

        provider.setName("whitelist_host_pub_key");
        provider.setLabel("whitelist-host-pub-key-label");
        provider.setDependsOn(new HashMap<String, Object>() {{
            put("whitelist_host_restrict_https", "true");
        }});
        provider.setRequired(true);
        provider.setHelpText("whitelist-host-pub-key-hlp-txt");
        provider.setType(ProviderConfigProperty.TEXT_TYPE);
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    private void setupAzureB2CNewSetup(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("azure_script_desc")).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("azure_script_desc");
        provider.setLabel("Azure Setup");
        provider.setType(ProviderConfigProperty.CONFIG_TYPE);
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
        providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("azure_b2c_desc")).findFirst();
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("azure_b2c_desc");
        provider.setLabel("Step 1");
        provider.setDependsOn(new HashMap<String, Object>() {{
            put(AZURE_B2C_SETUP, AZURE_B2C_NEW);
        }});
        provider.setHelpText("Download B2C_1A_TRUSTFRAMEWORKBASE.xml file");
        provider.setType(ProviderConfigProperty.DOWNLOAD_TYPE);
        provider.setDownloadUrl(ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm())+"/auth/realms/"+session.getContext().getRealm().getName()+"/download/provconf/azure-b2c-download-provider/"+session.getContext().getClient().getClientId());

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    private void setupAzureB2CAlreadySetup(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("azure_b2c_already_desc")).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("azure_b2c_already_desc");
        provider.setLabel("Instructions");
        provider.setDependsOn(new HashMap<String, Object>() {{
            put(AZURE_B2C_SETUP, AZURE_B2C_ALREADY);
        }});
        provider.setType(ProviderConfigProperty.CONFIG_TYPE);
        List<ProviderConfigProperty> subConfigs = new ArrayList<>();
        ProviderConfigProperty subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        subprovider.setHelpText("<b>Step 1</b><br/> Open B2C_1A_TRUSTFRAMEWORKBASE.xml in edit mode which shall be taken from Azure B2C portal");
        subConfigs.add(subprovider);
        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        subprovider.setHelpText("<b>Step 2</b><br/> Copy following xml block and paste as first under 'ClaimsSchema' block");
        subConfigs.add(subprovider);
        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_CODE);
        subprovider.setHelpText("<ClaimType Id=\"auth_req_id\">\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"ezto_user_id\">\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"app_id\">\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserInputType>Readonly</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"ezto_verify_url\">\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"app_secret\">\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserInputType>Readonly</UserInputType>\n" +
                "      </ClaimType>");
        subConfigs.add(subprovider);
        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        subprovider.setHelpText("<b>Step 3</b><br/> Copy following xml block and paste under 'OutputClaims' block ");
        subConfigs.add(subprovider);

        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_CODE);
        subprovider.setHelpText("<OutputClaim ClaimTypeReferenceId=\"email\" />");
        subConfigs.add(subprovider);

        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        subprovider.setHelpText("<b>Step 4</b>><br/> Copy following xml block and paste under 'ClaimsProviders' block ");
        subConfigs.add(subprovider);

        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_CODE);
        subprovider.setHelpText("<ClaimsProvider>\n" +
                "      <DisplayName>"+session.getContext().getClient().getName()+"</DisplayName>\n" +
                "      <TechnicalProfiles>\n" +
                "        <TechnicalProfile Id=\"TriggerEztoVerifyRequest\">\n" +
                "          <DisplayName>Mobile Authentication GUI</DisplayName>\n" +
                "          <Protocol Name=\"Proprietary\" Handler=\"Web.TPEngine.Providers.SelfAssertedAttributeProvider, Web.TPEngine, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\" />\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"ContentDefinitionReferenceId\">api.selfasserted.ezto.verify</Item>\n" +
                "          </Metadata>\n" +
                "          <CryptographicKeys>\n" +
                "            <Key Id=\"issuer_secret\" StorageReferenceId=\"B2C_1A_TokenSigningKeyContainer\" />\n" +
                "          </CryptographicKeys>\n" +
                "          <InputClaims>\n" +
                "            <InputClaim ClaimTypeReferenceId=\"email\" PartnerClaimType=\"signInNames.emailAddress\" />\n" +
                "            <InputClaim ClaimTypeReferenceId=\"auth_req_id\" PartnerClaimType=\"auth_req_id\" />\n" +
                "            <InputClaim ClaimTypeReferenceId=\"ezto_verify_url\" DefaultValue=\""+ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm())+"/auth/realms/api/ezto_web_push/"+session.getContext().getClient().getClientId()+"/"+session.getContext().getRealm().getName()+"/register\" AlwaysUseDefaultValue=\"true\" />\n" +
                "          </InputClaims>\n" +
                "          <DisplayClaims>\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"auth_req_id\" />\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"signInNames.emailAddress\" />\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"ezto_verify_url\" />\n" +
                "          </DisplayClaims>\n" +
                "          <!-- <PersistedClaims>\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"auth_req_id\" PartnerClaimType=\"auth_req_id\"/>\n" +
                "          </PersistedClaims> -->\n" +
                "          <!-- OutputClaims are required -->\n" +
                "          <OutputClaims>\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"auth_req_id\" PartnerClaimType=\"auth_req_id\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"signInNames.emailAddress\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"signInName\" Required=\"true\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"ezto_verify_url\" />\n" +
                "          </OutputClaims>\n" +
                "          <UseTechnicalProfileForSessionManagement ReferenceId=\"SM-AAD\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <TechnicalProfile Id=\"EztoVerifyResult\">\n" +
                "          <DisplayName>Get Ezto verify result</DisplayName>\n" +
                "          <Protocol Name=\"Proprietary\" Handler=\"Web.TPEngine.Providers.RestfulProvider, Web.TPEngine, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\" />\n" +
                "          <Metadata>\n" +
                "            <!-- TODO: adapt URL -->\n" +
                "            <Item Key=\"ServiceUrl\">"+ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm())+"/auth/realms/"+session.getContext().getRealm().getName()+"/protocol/openid-connect/transaction/result</Item>\n" +
                "            <Item Key=\"SendClaimsIn\">Form</Item>\n" +
                "            <Item Key=\"AuthenticationType\">Bearer</Item>\n" +
                "            <Item Key=\"ResolveJsonPathsInJsonTokens\">true</Item>\n" +
                "          </Metadata>\n" +
                "          <CryptographicKeys>\n" +
                "            <!-- create a policy key with Options manual and type encryption -->\n" +
                "            <Key Id=\"BearerAuthenticationToken\" StorageReferenceId=\"B2C_1A_AuthCloudAccessToken\" />\n" +
                "          </CryptographicKeys>\n" +
                "          <InputClaims>\n" +
                "            <InputClaim ClaimTypeReferenceId=\"auth_req_id\" PartnerClaimType=\"auth_req_id\" />\n" +
                "            <InputClaim ClaimTypeReferenceId=\"app_id\" DefaultValue=\""+session.getContext().getClient().getClientId()+"\" AlwaysUseDefaultValue=\"true\" />\n" +
                "            <InputClaim ClaimTypeReferenceId=\"app_secret\" DefaultValue=\""+session.getContext().getClient().getSecret()+"\" AlwaysUseDefaultValue=\"true\" />\n" +
                "          </InputClaims>\n" +
                "          <PersistedClaims>\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"ezto_user_id\" PartnerClaimType=\"userId\" />\n" +
                "          </PersistedClaims>\n" +
                "          <!-- OutputClaims are required  -->\n" +
                "          <OutputClaims>\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"ezto_user_id\" PartnerClaimType=\"userId\" />\n" +
                "          </OutputClaims>\n" +
                "          <UseTechnicalProfileForSessionManagement ReferenceId=\"SM-AAD\" />\n" +
                "        </TechnicalProfile>\n" +
                "      </TechnicalProfiles>\n" +
                "    </ClaimsProvider>");
        subConfigs.add(subprovider);

        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        subprovider.setHelpText("<b>Step 5</b><br/> Copy following xml block and paste under 'OrchestrationSteps' block of 'SignUp UserJourney' and 'SignIn UserJourney'");
        subConfigs.add(subprovider);

        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_CODE);
        subprovider.setHelpText("<OrchestrationStep Order=\"3\" Type=\"ClaimsExchange\">\n" +
                "          <ClaimsExchanges>\n" +
                "            <ClaimsExchange Id=\"TriggerEztoVerifyRequest\" TechnicalProfileReferenceId=\"TriggerEztoVerifyRequest\" />\n" +
                "          </ClaimsExchanges>\n" +
                "        </OrchestrationStep>\n" +
                "        <OrchestrationStep Order=\"4\" Type=\"ClaimsExchange\">\n" +
                "          <ClaimsExchanges>\n" +
                "            <ClaimsExchange Id=\"EztoVerifyResult\" TechnicalProfileReferenceId=\"EztoVerifyResult\" />\n" +
                "          </ClaimsExchanges>\n" +
                "        </OrchestrationStep>");
        subConfigs.add(subprovider);
        provider.setConfigProperties(subConfigs);
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    private void setupWebSDK(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty =
                configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("web_script_desc")).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("web_script_desc");
        provider.setLabel("WebApp Integeration Steps");
        provider.setType(ProviderConfigProperty.CONFIG_TYPE);
        List<ProviderConfigProperty> subConfigs = new ArrayList<>();
        ProviderConfigProperty subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        subprovider.setHelpText("azureb2c-step-1-helper");
        subConfigs.add(subprovider);
        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_CODE);
        subprovider.setLabel("step-1-script");
        String env = EnvironmentReader.getValue("ENV");
        String gh = "dev-web-push";
        if(env != null && env.equals("prod"))
        {
            gh = "web-push";
        }
        subprovider.setHelpText("\n    <script type=\"text/javascript\">\n" +
                "        (function () {\n" +
                "            d = document;\n" +
                "            link = d.createElement('link');\n" +
                "            link.rel = 'stylesheet';\n" +
                "            link.type = 'text/css';\n" +
                "            link.href = \"https://cdn.jsdelivr.net/gh/ezto-io/"+gh+"/eztoverify.min.css\";" +
                "\n" +
                "            d.getElementsByTagName(\"head\")[0].appendChild(link);\n" +
                "\n" +
                "            s = d.createElement(\"script\");\n" +
                "            s.src = \"https://cdn.socket.io/4.7.2/socket.io.min.js\";\n" +
                "            d.getElementsByTagName(\"head\")[0].appendChild(s);\n" +
                "\n" +
                "            s = d.createElement(\"script\");\n" +
                "            s.src = \"https://cdn.jsdelivr.net/gh/ezto-io/"+gh+"/eztoverify.min.js\";\n" +
                "            d.getElementsByTagName(\"head\")[0].appendChild(s);\n" +
                "        })();\n" +
                "    </script>");
        subConfigs.add(subprovider);
        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        subprovider.setHelpText("azureb2c-step-2-helper");
        subConfigs.add(subprovider);

        subprovider = new ProviderConfigProperty();
        subprovider.setType(ProviderConfigProperty.NOTIFICATION_CODE);
        String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm());
        String pushUrl =
                workspaceUrl + "/auth/realms/api/ezto_web_push/" + session.getContext().getClient().getClientId() + "/" + session.getContext().getRealm().getName() + "/register";
        subprovider.setHelpText("\n    let ez = new eztoverify();\n" +
                "    ez.request({request: {}},{api:\"" + pushUrl + "\", apiVersion: \""+ 1 +"\"},function(res){\n" +
                "        //Handle the result here;\n" +
                "    });");
        subprovider.setLabel("step-2-script");
        subConfigs.add(subprovider);
        provider.setConfigProperties(subConfigs);

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    @Override
    public void onCleanUp(KeycloakSession session, RealmModel realm, ClientModel client) {
        CibaAuthConfigModel authenticationConfig = realm.getCibaAuthConfigByClientId(client.getClientId(), realm.getId());
        CibaAuthProviderFactory providerFactory = (CibaAuthProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaAuthProvider.class, authenticationConfig.getCibaAuthProviderId());
        if(authenticationConfig != null) {
            realm.removeCibaAuthConfig(authenticationConfig);
        }
        if(providerFactory != null) {
            providerFactory.onDelete(session, (HashMap<String, String>) authenticationConfig.getConfig());
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

        String host = config.get(WHITELIST_HOST_URL);
        Map<String, String> browserHeaders = session.getContext().getRealm().getBrowserSecurityHeaders();
        Map<String, String> headers = new HashMap<>();
        headers.putAll(browserHeaders);
        String csp = headers.get(CONTENT_SECURITY_POLICY);
        List<String> cspSplit = Arrays.stream(csp.split(";")).collect(Collectors.toList());
        for (int x = 0; x < cspSplit.size(); x++) {
            if (cspSplit.get(x).trim().startsWith(FRAME_ANCESTORS)) {
                String frame = cspSplit.get(x);
                if (!frame.contains(host)) {
                    frame += " " + host;
                }
                cspSplit.set(x, frame);
            }
        }
        csp = String.join(";", cspSplit);
        headers.put(CONTENT_SECURITY_POLICY, csp);
        session.getContext().getRealm().setBrowserSecurityHeaders(headers);
        return true;
    }

    @Override
    public boolean onDelete(KeycloakSession session, HashMap<String, String> config) {

        String host = config.get(WHITELIST_HOST_URL);
        if (GeneralHelper.isWhiteListHostUrlConfiguredInOtherApps(session, host, session.getContext().getClient())) {
            return true;
        }
        Map<String, String> browserHeaders = session.getContext().getRealm().getBrowserSecurityHeaders();
        Map<String, String> headers = new HashMap<>();
        headers.putAll(browserHeaders);
        String csp = headers.get(CONTENT_SECURITY_POLICY);
        List<String> cspSplit = Arrays.stream(csp.split(";")).collect(Collectors.toList());
        for (int x = 0; x < cspSplit.size(); x++) {
            if (cspSplit.get(x).trim().startsWith(FRAME_ANCESTORS)) {
                String frame = cspSplit.get(x);
                if (frame.contains(host)) {
                    frame = frame.replace(host, "");
                }
                cspSplit.set(x, frame);
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

        String host = oldConfig.get(WHITELIST_HOST_URL);
        Map<String, String> browserHeaders = session.getContext().getRealm().getBrowserSecurityHeaders();
        Map<String, String> headers = new HashMap<>();
        headers.putAll(browserHeaders);
        String csp = headers.get(CONTENT_SECURITY_POLICY);
        List<String> cspSplit = Arrays.stream(csp.split(";")).collect(Collectors.toList());
        for (int x = 0; x < cspSplit.size(); x++) {
            if (cspSplit.get(x).trim().startsWith(FRAME_ANCESTORS)) {
                String frame = cspSplit.get(x);
                if (frame.contains(host) && !GeneralHelper.isWhiteListHostUrlConfiguredInOtherApps(session, host, session.getContext().getClient())) {
                    if (!Arrays.stream(frame.split("\\s+")).anyMatch(part -> part.equals(newConfig.get(WHITELIST_HOST_URL)))) {
                        frame = frame.replace(host, newConfig.get(WHITELIST_HOST_URL));
                    }
                } else {
                        frame += " " + newConfig.get(WHITELIST_HOST_URL);
                    }
                    cspSplit.set(x, frame);
                }
            }
            csp = String.join(";", cspSplit);
            headers.put(CONTENT_SECURITY_POLICY, csp);
            session.getContext().getRealm().setBrowserSecurityHeaders(headers);
            return true;
        }

    @Override
    public String getId() {
        return "azure-b2c-ciba-authorizer";
    }

}
