package com.qlikverify.backoffice.configDownloads;

import org.keycloak.Config;
import org.keycloak.downloads.ConfigDownloadProvider;
import org.keycloak.downloads.ConfigDownloadProviderFactory;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import static com.qlikverify.backoffice.authorizers.AzureB2CAuthorizer.AZURE_B2C_TENANT_ID;
import static com.qlikverify.backoffice.authorizers.AzureB2CAuthorizer.AZURE_B2C_TENANT_ID_OBJECT;

public class AzureTrustFrameworkXmlConfigDownloadProvider implements ConfigDownloadProvider, ConfigDownloadProviderFactory {
    KeycloakSession session;
    @Override
    public String getDownloadFile(ClientModel clientModel) {
        String text = getText();
        String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, session.getContext().getRealm());
        final CibaAuthConfigModel cibaConfigByClientId =
                getCibaAuthConfig(clientModel);
        text = text
                .replaceAll("@app_id@",clientModel.getClientId())
                .replaceAll("@app_secret@",clientModel.getSecret())
                .replaceAll("@workspace_name@",session.getContext().getRealm().getName())
                .replaceAll("@domain_url@", workspaceUrl)
                .replaceAll("@b2c_tenant_id@",
                        cibaConfigByClientId.getConfig().getOrDefault(AZURE_B2C_TENANT_ID, ""))
                .replaceAll("@b2c_tenant_object_id@",
                        cibaConfigByClientId.getConfig().getOrDefault(AZURE_B2C_TENANT_ID_OBJECT, ""));
        return text;
    }

    @Override
    public String getFileName() {
        return "B2C_1A_TRUSTFRAMEWORKBASE.xml";
    }


    private CibaAuthConfigModel getCibaAuthConfig(ClientModel clientModel) {
        return session.getContext().getRealm().getCibaAuthConfigByClientId(clientModel.getClientId(),
                session.getContext().getRealm().getId());
    }

    @Override
    public ConfigDownloadProvider create(KeycloakSession session) {
        this.session = session;
        return this;
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
        return "azure-b2c-trust-framework-provider";
    }

    private String getText(){
        return "<TrustFrameworkPolicy xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://schemas.microsoft.com/online/cpim/schemas/2013/06\" PolicySchemaVersion=\"0.3.0.0\" TenantId=\"@b2c_tenant_id@\" PolicyId=\"B2C_1A_TrustFrameworkBase\" PublicPolicyUri=\"http://@b2c_tenant_id@/B2C_1A_TrustFrameworkBase\" TenantObjectId=\"@b2c_tenant_object_id@\">\n" +
                "  <BuildingBlocks>\n" +
                "    <ClaimsSchema>\n" +
                "      <!-- The ClaimsSchema is divided into three sections:\n" +
                "           1. Section I lists the minimum claims that are required for the user journeys to work properly.\n" +
                "           2. Section II lists the claims required for query string parameters and other special parameters\n" +
                "              to be passed to other claims providers, esp. login.microsoftonline.com for authentication.\n" +
                "              Please do not modify these claims.\n" +
                "           3. Section III lists any additional (optional) claims that can be collected from the user, stored\n" +
                "              in the directory and sent in tokens during sign in. Add new claims to be collected from the user\n" +
                "              and/or sent in the token in Section III. -->\n" +
                "      <!-- NOTE: The claims schema contains restrictions on certain claims such as passwords and usernames.\n" +
                "           The trust framework policy treats Azure AD as any other claims provider and all its restrictions\n" +
                "           are modelled in the policy. A policy could be modified to add more restrictions, or use another\n" +
                "           claims provider for credential storage which will have its own restrictions. -->\n" +
                "      <!-- SECTION I: Claims required for user journeys to work properly -->\n" +
                "      <!-- EZTO:START:Claims-->\n" +
                "      <ClaimType Id=\"auth_req_id\">\n" +
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
                "      </ClaimType>\n" +
                "      <!-- EZTO:END:Claims-->\n" +
                "      <ClaimType Id=\"url\">\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserInputType>Readonly</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"tenantId\">\n" +
                "        <DisplayName>User's Object's Tenant ID</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OAuth2\" PartnerClaimType=\"tid\" />\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"tid\" />\n" +
                "          <Protocol Name=\"SAML2\" PartnerClaimType=\"http://schemas.microsoft.com/identity/claims/tenantid\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText>Tenant identifier (ID) of the user object in Azure AD.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"objectId\">\n" +
                "        <DisplayName>User's Object ID</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OAuth2\" PartnerClaimType=\"oid\" />\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"oid\" />\n" +
                "          <Protocol Name=\"SAML2\" PartnerClaimType=\"http://schemas.microsoft.com/identity/claims/objectidentifier\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText>Object identifier (ID) of the user object in Azure AD.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <!-- Claims needed for local accounts. -->\n" +
                "      <ClaimType Id=\"signInName\">\n" +
                "        <DisplayName>Sign in name</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserHelpText />\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"signInNames.emailAddress\">\n" +
                "        <DisplayName>Email Address</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserHelpText>Email address to use for signing in.</UserHelpText>\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"accountEnabled\">\n" +
                "        <DisplayName>Account Enabled</DisplayName>\n" +
                "        <DataType>boolean</DataType>\n" +
                "        <AdminHelpText>Specifies whether the user's account is enabled.</AdminHelpText>\n" +
                "        <UserHelpText>Specifies whether your account is enabled.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"password\">\n" +
                "        <DisplayName>Password</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserHelpText>Enter password</UserHelpText>\n" +
                "        <UserInputType>Password</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <!-- The claim types newPassword and reenterPassword are considered special, please do not change the names.\n" +
                "           The UI validates that the user correctly re-entered their password during account creation based on these\n" +
                "           claim types.\t  -->\n" +
                "      <ClaimType Id=\"newPassword\">\n" +
                "        <DisplayName>New Password</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserHelpText>Enter new password</UserHelpText>\n" +
                "        <UserInputType>Password</UserInputType>\n" +
                "        <Restriction>\n" +
                "          <Pattern RegularExpression=\"^((?=.*[a-z])(?=.*[A-Z])(?=.*\\d)|(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9])|(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9])|(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]))([A-Za-z\\d@#$%^&amp;*\\-_+=[\\]{}|\\\\:',?/`~&quot;();!]|\\.(?!@)){8,16}$\" HelpText=\"8-16 characters, containing 3 out of 4 of the following: Lowercase characters, uppercase characters, digits (0-9), and one or more of the following symbols: @ # $ % ^ &amp; * - _ + = [ ] { } | \\ : ' , ? / ` ~ &quot; ( ) ; .\" />\n" +
                "        </Restriction>\n" +
                "      </ClaimType>\n" +
                "      <!-- The password regular expression above is constructed for AAD passwords based on restrictions at https://msdn.microsoft.com/en-us/library/azure/jj943764.aspx\n" +
                "\n" +
                "        ^( # one of the following four combinations must appear in the password\n" +
                "         (?=.*[a-z])(?=.*[A-Z])(?=.*\\d) |            # matches lower case, upper case or digit\n" +
                "         (?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]) |  # matches lower case, upper case or special character (i.e. non-alpha or digit)\n" +
                "         (?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]) |     # matches lower case, digit, or special character\n" +
                "         (?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])       # matches upper case, digit, or special character\n" +
                "        )\n" +
                "        ( # The password must match the following restrictions\n" +
                "         [A-Za-z\\d@#$%^&*\\-_+=[\\]{}|\\\\:',?/`~\"();!] |   # The list of all acceptable characters (without .)\n" +
                "         \\.(?!@)                                        # or . can appear as long as not followed by @\n" +
                "        ) {8,16}$                                       # the length must be between 8 and 16 chars inclusive\n" +
                "\n" +
                "      -->\n" +
                "      <ClaimType Id=\"reenterPassword\">\n" +
                "        <DisplayName>Confirm New Password</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserHelpText>Confirm new password</UserHelpText>\n" +
                "        <UserInputType>Password</UserInputType>\n" +
                "        <Restriction>\n" +
                "          <Pattern RegularExpression=\"^((?=.*[a-z])(?=.*[A-Z])(?=.*\\d)|(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9])|(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9])|(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]))([A-Za-z\\d@#$%^&amp;*\\-_+=[\\]{}|\\\\:',?/`~&quot;();!]|\\.(?!@)){8,16}$\" HelpText=\" \" />\n" +
                "        </Restriction>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"passwordPolicies\">\n" +
                "        <DisplayName>Password Policies</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserHelpText>Password policies used by Azure AD to determine password strength, expiry etc.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"client_id\">\n" +
                "        <DisplayName>client_id</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <AdminHelpText>Special parameter passed to EvoSTS.</AdminHelpText>\n" +
                "        <UserHelpText>Special parameter passed to EvoSTS.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"resource_id\">\n" +
                "        <DisplayName>resource_id</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <AdminHelpText>Special parameter passed to EvoSTS.</AdminHelpText>\n" +
                "        <UserHelpText>Special parameter passed to EvoSTS.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"sub\">\n" +
                "        <DisplayName>Subject</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"sub\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText />\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"identityProvider\">\n" +
                "        <DisplayName>Identity Provider</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OAuth2\" PartnerClaimType=\"idp\" />\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"idp\" />\n" +
                "          <Protocol Name=\"SAML2\" PartnerClaimType=\"http://schemas.microsoft.com/identity/claims/identityprovider\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText />\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"displayName\">\n" +
                "        <DisplayName>Display Name</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OAuth2\" PartnerClaimType=\"unique_name\" />\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"name\" />\n" +
                "          <Protocol Name=\"SAML2\" PartnerClaimType=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText>Your display name.</UserHelpText>\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"email\">\n" +
                "        <DisplayName>Email Address</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"email\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText>Email address that can be used to contact you.</UserHelpText>\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "        <Restriction>\n" +
                "          <Pattern RegularExpression=\"^[a-zA-Z0-9.!#$%&amp;'^_`{}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$\" HelpText=\"Please enter a valid email address.\" />\n" +
                "        </Restriction>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"otherMails\">\n" +
                "        <DisplayName>Alternate Email Addresses</DisplayName>\n" +
                "        <DataType>stringCollection</DataType>\n" +
                "        <UserHelpText>Email addresses that can be used to contact the user.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"userPrincipalName\">\n" +
                "        <DisplayName>UserPrincipalName</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OAuth2\" PartnerClaimType=\"upn\" />\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"upn\" />\n" +
                "          <Protocol Name=\"SAML2\" PartnerClaimType=\"http://schemas.microsoft.com/identity/claims/userprincipalname\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText>Your user name as stored in the Azure Active Directory.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"newUser\">\n" +
                "        <DisplayName>User is new</DisplayName>\n" +
                "        <DataType>boolean</DataType>\n" +
                "        <UserHelpText />\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"executed-SelfAsserted-Input\">\n" +
                "        <DisplayName>Executed-SelfAsserted-Input</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserHelpText>A claim that specifies whether attributes were collected from the user.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"authenticationSource\">\n" +
                "        <DisplayName>AuthenticationSource</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <UserHelpText>Specifies whether the user was authenticated at Social IDP or local account.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <!-- SECTION II: Claims required to pass on special parameters (including some query string parameters) to other claims providers -->\n" +
                "      <ClaimType Id=\"objectIdFromSession\">\n" +
                "        <DisplayName>objectIdFromSession</DisplayName>\n" +
                "        <DataType>boolean</DataType>\n" +
                "        <UserHelpText>Parameter provided by the default session management provider to indicate that the object id has been retrieved from an SSO session.</UserHelpText>\n" +
                "      </ClaimType>\n" +
                "      <!-- SECTION III: Additional claims that can be collected from the users, stored in the directory, and sent in the token. Add additional claims here. -->\n" +
                "      <ClaimType Id=\"givenName\">\n" +
                "        <DisplayName>Given Name</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OAuth2\" PartnerClaimType=\"given_name\" />\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"given_name\" />\n" +
                "          <Protocol Name=\"SAML2\" PartnerClaimType=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText>Your given name (also known as first name).</UserHelpText>\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "      </ClaimType>\n" +
                "      <ClaimType Id=\"surname\">\n" +
                "        <DisplayName>Surname</DisplayName>\n" +
                "        <DataType>string</DataType>\n" +
                "        <DefaultPartnerClaimTypes>\n" +
                "          <Protocol Name=\"OAuth2\" PartnerClaimType=\"family_name\" />\n" +
                "          <Protocol Name=\"OpenIdConnect\" PartnerClaimType=\"family_name\" />\n" +
                "          <Protocol Name=\"SAML2\" PartnerClaimType=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname\" />\n" +
                "        </DefaultPartnerClaimTypes>\n" +
                "        <UserHelpText>Your surname (also known as family name or last name).</UserHelpText>\n" +
                "        <UserInputType>TextBox</UserInputType>\n" +
                "      </ClaimType>\n" +
                "    </ClaimsSchema>\n" +
                "    <ClaimsTransformations>\n" +
                "      <ClaimsTransformation Id=\"AssertAccountEnabledIsTrue\" TransformationMethod=\"AssertBooleanClaimIsEqualToValue\">\n" +
                "        <InputClaims>\n" +
                "          <InputClaim ClaimTypeReferenceId=\"accountEnabled\" TransformationClaimType=\"inputClaim\" />\n" +
                "        </InputClaims>\n" +
                "        <InputParameters>\n" +
                "          <InputParameter Id=\"valueToCompareTo\" DataType=\"boolean\" Value=\"true\" />\n" +
                "        </InputParameters>\n" +
                "      </ClaimsTransformation>\n" +
                "    </ClaimsTransformations>\n" +
                "    <ClientDefinitions>\n" +
                "      <ClientDefinition Id=\"DefaultWeb\">\n" +
                "        <ClientUIFilterFlags>LineMarkers, MetaRefresh</ClientUIFilterFlags>\n" +
                "      </ClientDefinition>\n" +
                "    </ClientDefinitions>\n" +
                "    <ContentDefinitions>\n" +
                "      <ContentDefinition Id=\"api.signuporsignin\">\n" +
                "        <LoadUri>~/tenant/templates/AzureBlue/unified.cshtml</LoadUri>\n" +
                "        <RecoveryUri>~/common/default_page_error.html</RecoveryUri>\n" +
                "        <DataUri>urn:com:microsoft:aad:b2c:elements:contract:unifiedssp:1.0.0</DataUri>\n" +
                "        <Metadata>\n" +
                "          <Item Key=\"DisplayName\">Signin and Signup</Item>\n" +
                "        </Metadata>\n" +
                "        <LocalizedResourcesReferences MergeBehavior=\"ReplaceAll\">\n" +
                "          <LocalizedResourcesReference Language=\"en\" LocalizedResourcesReferenceId=\"api.selfasserted.en\" />\n" +
                "        </LocalizedResourcesReferences>\n" +
                "      </ContentDefinition>\n" +
                "      <ContentDefinition Id=\"api.selfasserted\">\n" +
                "        <LoadUri>~/tenant/templates/AzureBlue/selfAsserted.cshtml</LoadUri>\n" +
                "        <RecoveryUri>~/common/default_page_error.html</RecoveryUri>\n" +
                "        <DataUri>urn:com:microsoft:aad:b2c:elements:contract:selfasserted:1.2.0</DataUri>\n" +
                "        <Metadata>\n" +
                "          <Item Key=\"DisplayName\">Collect information from user page</Item>\n" +
                "        </Metadata>\n" +
                "      </ContentDefinition>\n" +
                "      <ContentDefinition Id=\"api.localaccountsignup\">\n" +
                "        <LoadUri>~/tenant/templates/AzureBlue/selfAsserted.cshtml</LoadUri>\n" +
                "        <RecoveryUri>~/common/default_page_error.html</RecoveryUri>\n" +
                "        <DataUri>urn:com:microsoft:aad:b2c:elements:contract:selfasserted:1.2.0</DataUri>\n" +
                "        <Metadata>\n" +
                "          <Item Key=\"DisplayName\">Local account sign up page</Item>\n" +
                "        </Metadata>\n" +
                "      </ContentDefinition>\n" +
                "      <ContentDefinition Id=\"api.localaccountpasswordreset\">\n" +
                "        <LoadUri>~/tenant/templates/AzureBlue/selfAsserted.cshtml</LoadUri>\n" +
                "        <RecoveryUri>~/common/default_page_error.html</RecoveryUri>\n" +
                "        <DataUri>urn:com:microsoft:aad:b2c:elements:contract:selfasserted:1.2.0</DataUri>\n" +
                "        <Metadata>\n" +
                "          <Item Key=\"DisplayName\">Local account change password page</Item>\n" +
                "        </Metadata>\n" +
                "      </ContentDefinition>\n" +
                "      <ContentDefinition Id=\"api.selfasserted.ezto.verify\">\n" +
                "        <LoadUri>https://cdn.jsdelivr.net/gh/ezto-io/dev-web-push/ezto.html</LoadUri>\n" +
                "        <RecoveryUri>~/common/default_page_error.html</RecoveryUri>\n" +
                "        <DataUri>urn:com:microsoft:aad:b2c:elements:contract:selfasserted:1.2.0</DataUri>\n" +
                "        <Metadata>\n" +
                "          <Item Key=\"DisplayName\">Mobile Authentication</Item>\n" +
                "        </Metadata>\n" +
                "      </ContentDefinition>\n" +
                "    </ContentDefinitions>\n" +
                "    <Localization Enabled=\"true\">\n" +
                "      <SupportedLanguages DefaultLanguage=\"en\" MergeBehavior=\"ReplaceAll\">\n" +
                "        <SupportedLanguage>en</SupportedLanguage>\n" +
                "        <SupportedLanguage>es</SupportedLanguage>\n" +
                "      </SupportedLanguages>\n" +
                "      <LocalizedResources Id=\"api.selfasserted.en\">\n" +
                "        <LocalizedStrings>\n" +
                "          <LocalizedString ElementType=\"UxElement\" StringId=\"social_intro\"></LocalizedString>\n" +
                "        </LocalizedStrings>\n" +
                "      </LocalizedResources>\n" +
                "    </Localization>\n" +
                "  </BuildingBlocks>\n" +
                "  <!--\n" +
                "        A list of all the claim providers that can be used in the technical policies. If a claims provider is not listed\n" +
                "        in this section, then it cannot be used in a technical policy.\n" +
                "    -->\n" +
                "  <ClaimsProviders>\n" +
                "    <ClaimsProvider>\n" +
                "      <DisplayName>Azure Active Directory</DisplayName>\n" +
                "      <TechnicalProfiles>\n" +
                "        <TechnicalProfile Id=\"AAD-Common\">\n" +
                "          <DisplayName>Azure Active Directory</DisplayName>\n" +
                "          <Protocol Name=\"Proprietary\" Handler=\"Web.TPEngine.Providers.AzureActiveDirectoryProvider, Web.TPEngine, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\" />\n" +
                "          <CryptographicKeys>\n" +
                "            <Key Id=\"issuer_secret\" StorageReferenceId=\"B2C_1A_TokenSigningKeyContainer\" />\n" +
                "          </CryptographicKeys>\n" +
                "          <!-- We need this here to suppress the SelfAsserted provider from invoking SSO on validation profiles. -->\n" +
                "          <IncludeInSso>false</IncludeInSso>\n" +
                "          <UseTechnicalProfileForSessionManagement ReferenceId=\"SM-Noop\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <!-- Technical profiles for local accounts -->\n" +
                "        <TechnicalProfile Id=\"AAD-UserWriteUsingLogonEmail\">\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"Operation\">Write</Item>\n" +
                "            <Item Key=\"RaiseErrorIfClaimsPrincipalAlreadyExists\">true</Item>\n" +
                "          </Metadata>\n" +
                "          <IncludeInSso>false</IncludeInSso>\n" +
                "          <InputClaims>\n" +
                "            <InputClaim ClaimTypeReferenceId=\"email\" PartnerClaimType=\"signInNames.emailAddress\" Required=\"true\" />\n" +
                "          </InputClaims>\n" +
                "          <PersistedClaims>\n" +
                "            <!-- Required claims -->\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"email\" PartnerClaimType=\"signInNames.emailAddress\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"newPassword\" PartnerClaimType=\"password\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"displayName\" DefaultValue=\"unknown\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"passwordPolicies\" DefaultValue=\"DisablePasswordExpiration\" />\n" +
                "            <!-- Optional claims. -->\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"givenName\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"surname\" />\n" +
                "          </PersistedClaims>\n" +
                "          <OutputClaims>\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"objectId\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"newUser\" PartnerClaimType=\"newClaimsPrincipalCreated\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"authenticationSource\" DefaultValue=\"localAccountAuthentication\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"userPrincipalName\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"signInNames.emailAddress\" />\n" +
                "          </OutputClaims>\n" +
                "          <IncludeTechnicalProfile ReferenceId=\"AAD-Common\" />\n" +
                "          <UseTechnicalProfileForSessionManagement ReferenceId=\"SM-AAD\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <TechnicalProfile Id=\"AAD-UserReadUsingEmailAddress\">\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"Operation\">Read</Item>\n" +
                "            <Item Key=\"RaiseErrorIfClaimsPrincipalDoesNotExist\">true</Item>\n" +
                "            <Item Key=\"UserMessageIfClaimsPrincipalDoesNotExist\">An account could not be found for the provided user ID.</Item>\n" +
                "          </Metadata>\n" +
                "          <IncludeInSso>false</IncludeInSso>\n" +
                "          <InputClaims>\n" +
                "            <InputClaim ClaimTypeReferenceId=\"email\" PartnerClaimType=\"signInNames.emailAddress\" Required=\"true\" />\n" +
                "          </InputClaims>\n" +
                "          <OutputClaims>\n" +
                "            <!-- Required claims -->\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"objectId\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"authenticationSource\" DefaultValue=\"localAccountAuthentication\" />\n" +
                "            <!-- Optional claims -->\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"userPrincipalName\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"displayName\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"accountEnabled\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"otherMails\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"signInNames.emailAddress\" />\n" +
                "          </OutputClaims>\n" +
                "          <OutputClaimsTransformations>\n" +
                "            <OutputClaimsTransformation ReferenceId=\"AssertAccountEnabledIsTrue\" />\n" +
                "          </OutputClaimsTransformations>\n" +
                "          <IncludeTechnicalProfile ReferenceId=\"AAD-Common\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <!-- Technical profiles for updating user record using objectId -->\n" +
                "        <TechnicalProfile Id=\"AAD-UserWriteProfileUsingObjectId\">\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"Operation\">Write</Item>\n" +
                "            <Item Key=\"RaiseErrorIfClaimsPrincipalAlreadyExists\">false</Item>\n" +
                "            <Item Key=\"RaiseErrorIfClaimsPrincipalDoesNotExist\">true</Item>\n" +
                "          </Metadata>\n" +
                "          <IncludeInSso>false</IncludeInSso>\n" +
                "          <InputClaims>\n" +
                "            <InputClaim ClaimTypeReferenceId=\"objectId\" Required=\"true\" />\n" +
                "          </InputClaims>\n" +
                "          <PersistedClaims>\n" +
                "            <!-- Required claims -->\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"objectId\" />\n" +
                "            <!-- Optional claims -->\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"givenName\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"surname\" />\n" +
                "          </PersistedClaims>\n" +
                "          <IncludeTechnicalProfile ReferenceId=\"AAD-Common\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <!-- The following technical profile is used to read data after user authenticates. -->\n" +
                "        <TechnicalProfile Id=\"AAD-UserReadUsingObjectId\">\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"Operation\">Read</Item>\n" +
                "            <Item Key=\"RaiseErrorIfClaimsPrincipalDoesNotExist\">true</Item>\n" +
                "          </Metadata>\n" +
                "          <IncludeInSso>false</IncludeInSso>\n" +
                "          <InputClaims>\n" +
                "            <InputClaim ClaimTypeReferenceId=\"objectId\" Required=\"true\" />\n" +
                "          </InputClaims>\n" +
                "          <OutputClaims>\n" +
                "            <!-- Optional claims -->\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"signInNames.emailAddress\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"displayName\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"otherMails\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"givenName\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"surname\" />\n" +
                "          </OutputClaims>\n" +
                "          <IncludeTechnicalProfile ReferenceId=\"AAD-Common\" />\n" +
                "        </TechnicalProfile>\n" +
                "      </TechnicalProfiles>\n" +
                "    </ClaimsProvider>\n" +
                "    <ClaimsProvider>\n" +
                "      <DisplayName>Local Account</DisplayName>\n" +
                "      <TechnicalProfiles>\n" +
                "        <TechnicalProfile Id=\"LocalAccountDiscoveryUsingEmailAddress-SignIn\">\n" +
                "          <DisplayName>Passwordless Authentication</DisplayName>\n" +
                "          <IncludeTechnicalProfile ReferenceId=\"LocalAccountDiscoveryUsingEmailAddress-NoValidate\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <TechnicalProfile Id=\"LocalAccountSignUpWithLogonEmail\">\n" +
                "          <DisplayName>Email signup</DisplayName>\n" +
                "          <Protocol Name=\"Proprietary\" Handler=\"Web.TPEngine.Providers.SelfAssertedAttributeProvider, Web.TPEngine, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\" />\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"IpAddressClaimReferenceId\">IpAddress</Item>\n" +
                "            <Item Key=\"ContentDefinitionReferenceId\">api.localaccountsignup</Item>\n" +
                "            <Item Key=\"language.button_continue\">Create</Item>\n" +
                "          </Metadata>\n" +
                "          <CryptographicKeys>\n" +
                "            <Key Id=\"issuer_secret\" StorageReferenceId=\"B2C_1A_TokenSigningKeyContainer\" />\n" +
                "          </CryptographicKeys>\n" +
                "          <InputClaims>\n" +
                "            <InputClaim ClaimTypeReferenceId=\"email\" />\n" +
                "          </InputClaims>\n" +
                "          <OutputClaims>\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"objectId\" />\n" +
                "            <!-- EZTO:START:OutputClaim -->\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"email\" />\n" +
                "            <!-- <OutputClaim ClaimTypeReferenceId=\"email\" PartnerClaimType=\"Verified.Email\" Required=\"true\" /> -->\n" +
                "            <!-- EZTO:END:OutputClaim -->\n" +
                "            <!-- <OutputClaim ClaimTypeReferenceId=\"newPassword\" Required=\"true\" /> -->\n" +
                "            <!-- <OutputClaim ClaimTypeReferenceId=\"reenterPassword\" Required=\"true\" /> -->\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"executed-SelfAsserted-Input\" DefaultValue=\"true\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"authenticationSource\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"newUser\" />\n" +
                "            <!-- Optional claims, to be collected from the user -->\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"displayName\" />\n" +
                "            <!--\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"givenName\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"surName\" />\n" +
                "            -->\n" +
                "          </OutputClaims>\n" +
                "          <ValidationTechnicalProfiles>\n" +
                "            <ValidationTechnicalProfile ReferenceId=\"AAD-UserWriteUsingLogonEmail\" />\n" +
                "          </ValidationTechnicalProfiles>\n" +
                "          <UseTechnicalProfileForSessionManagement ReferenceId=\"SM-AAD\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <!-- This technical profile forces the user to verify the email address that they provide on the UI.\n" +
                "             Only after email is verified, the user account is read from the directory. -->\n" +
                "        <TechnicalProfile Id=\"LocalAccountDiscoveryUsingEmailAddress\">\n" +
                "          <DisplayName>Reset password using email address</DisplayName>\n" +
                "          <Protocol Name=\"Proprietary\" Handler=\"Web.TPEngine.Providers.SelfAssertedAttributeProvider, Web.TPEngine, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\" />\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"IpAddressClaimReferenceId\">IpAddress</Item>\n" +
                "            <Item Key=\"ContentDefinitionReferenceId\">api.localaccountpasswordreset</Item>\n" +
                "            <Item Key=\"UserMessageIfClaimsTransformationBooleanValueIsNotEqual\">Your account has been locked. Contact your support person to unlock it, then try again.</Item>\n" +
                "          </Metadata>\n" +
                "          <CryptographicKeys>\n" +
                "            <Key Id=\"issuer_secret\" StorageReferenceId=\"B2C_1A_TokenSigningKeyContainer\" />\n" +
                "          </CryptographicKeys>\n" +
                "          <IncludeInSso>false</IncludeInSso>\n" +
                "          <OutputClaims>\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"email\" PartnerClaimType=\"Verified.Email\" Required=\"true\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"objectId\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"userPrincipalName\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"authenticationSource\" />\n" +
                "          </OutputClaims>\n" +
                "          <ValidationTechnicalProfiles>\n" +
                "            <ValidationTechnicalProfile ReferenceId=\"AAD-UserReadUsingEmailAddress\" />\n" +
                "          </ValidationTechnicalProfiles>\n" +
                "        </TechnicalProfile>\n" +
                "        <!-- This technical profile forces the user to verify the email address that they provide on the UI.\n" +
                "             No validation is done, the user account is read from the directory. -->\n" +
                "        <TechnicalProfile Id=\"LocalAccountDiscoveryUsingEmailAddress-NoValidate\">\n" +
                "          <DisplayName>Reset password using email address</DisplayName>\n" +
                "          <Protocol Name=\"Proprietary\" Handler=\"Web.TPEngine.Providers.SelfAssertedAttributeProvider, Web.TPEngine, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\" />\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"IpAddressClaimReferenceId\">IpAddress</Item>\n" +
                "            <Item Key=\"ContentDefinitionReferenceId\">api.localaccountpasswordreset</Item>\n" +
                "            <Item Key=\"UserMessageIfClaimsTransformationBooleanValueIsNotEqual\">Your account has been locked. Contact your support person to unlock it, then try again.</Item>\n" +
                "          </Metadata>\n" +
                "          <CryptographicKeys>\n" +
                "            <Key Id=\"issuer_secret\" StorageReferenceId=\"B2C_1A_TokenSigningKeyContainer\" />\n" +
                "          </CryptographicKeys>\n" +
                "          <IncludeInSso>false</IncludeInSso>\n" +
                "          <OutputClaims>\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"email\" Required=\"true\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"objectId\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"userPrincipalName\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"authenticationSource\" />\n" +
                "          </OutputClaims>\n" +
                "          <ValidationTechnicalProfiles>\n" +
                "            <ValidationTechnicalProfile ReferenceId=\"AAD-UserReadUsingEmailAddress\" />\n" +
                "          </ValidationTechnicalProfiles>\n" +
                "        </TechnicalProfile>\n" +
                "      </TechnicalProfiles>\n" +
                "    </ClaimsProvider>\n" +
                "    <ClaimsProvider>\n" +
                "      <DisplayName>Session Management</DisplayName>\n" +
                "      <TechnicalProfiles>\n" +
                "        <TechnicalProfile Id=\"SM-Noop\">\n" +
                "          <DisplayName>Noop Session Management Provider</DisplayName>\n" +
                "          <Protocol Name=\"Proprietary\" Handler=\"Web.TPEngine.SSO.NoopSSOSessionProvider, Web.TPEngine, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <TechnicalProfile Id=\"SM-AAD\">\n" +
                "          <DisplayName>Session Mananagement Provider</DisplayName>\n" +
                "          <Protocol Name=\"Proprietary\" Handler=\"Web.TPEngine.SSO.DefaultSSOSessionProvider, Web.TPEngine, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\" />\n" +
                "          <PersistedClaims>\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"objectId\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"signInName\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"authenticationSource\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"identityProvider\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"newUser\" />\n" +
                "            <PersistedClaim ClaimTypeReferenceId=\"executed-SelfAsserted-Input\" />\n" +
                "          </PersistedClaims>\n" +
                "          <OutputClaims>\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"objectIdFromSession\" DefaultValue=\"true\" />\n" +
                "          </OutputClaims>\n" +
                "        </TechnicalProfile>\n" +
                "      </TechnicalProfiles>\n" +
                "    </ClaimsProvider>\n" +
                "    <ClaimsProvider>\n" +
                "      <DisplayName>Trustframework Policy Engine TechnicalProfiles</DisplayName>\n" +
                "      <TechnicalProfiles>\n" +
                "        <TechnicalProfile Id=\"TpEngine_c3bd4fe2-1775-4013-b91d-35f16d377d13\">\n" +
                "          <DisplayName>Trustframework Policy Engine Default Technical Profile</DisplayName>\n" +
                "          <Protocol Name=\"None\" />\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"url\">{service:te}</Item>\n" +
                "          </Metadata>\n" +
                "        </TechnicalProfile>\n" +
                "      </TechnicalProfiles>\n" +
                "    </ClaimsProvider>\n" +
                "    <ClaimsProvider>\n" +
                "      <DisplayName>Token Issuer</DisplayName>\n" +
                "      <TechnicalProfiles>\n" +
                "        <TechnicalProfile Id=\"JwtIssuer\">\n" +
                "          <DisplayName>JWT Issuer</DisplayName>\n" +
                "          <Protocol Name=\"None\" />\n" +
                "          <OutputTokenFormat>JWT</OutputTokenFormat>\n" +
                "          <Metadata>\n" +
                "            <Item Key=\"client_id\">{service:te}</Item>\n" +
                "            <Item Key=\"issuer_refresh_token_user_identity_claim_type\">objectId</Item>\n" +
                "            <Item Key=\"SendTokenResponseBodyWithJsonNumbers\">true</Item>\n" +
                "          </Metadata>\n" +
                "          <CryptographicKeys>\n" +
                "            <Key Id=\"issuer_secret\" StorageReferenceId=\"B2C_1A_TokenSigningKeyContainer\" />\n" +
                "            <Key Id=\"issuer_refresh_token_key\" StorageReferenceId=\"B2C_1A_TokenEncryptionKeyContainer\" />\n" +
                "          </CryptographicKeys>\n" +
                "          <InputClaims />\n" +
                "          <OutputClaims />\n" +
                "        </TechnicalProfile>\n" +
                "      </TechnicalProfiles>\n" +
                "    </ClaimsProvider>\n" +
                "    <!-- EZTO:START:ClaimsProvider -->\n" +
                "    <ClaimsProvider>\n" +
                "      <DisplayName>EZTO Connect</DisplayName>\n" +
                "      <TechnicalProfiles>\n" +
                "        <TechnicalProfile Id=\"TriggerEztoSignUpRequest\">\n" +
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
                "            <InputClaim ClaimTypeReferenceId=\"ezto_verify_url\" DefaultValue=\"@domain_url@/auth/realms/api/ezto_web_push/@app_id@/@workspace_name@/register\" AlwaysUseDefaultValue=\"true\" />\n" +
                "          </InputClaims>\n" +
                "          <DisplayClaims>\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"auth_req_id\" />\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"signInNames.emailAddress\" />\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"ezto_verify_url\" />\n" +
                "          </DisplayClaims>\n" +
                "          <OutputClaims>\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"auth_req_id\" PartnerClaimType=\"auth_req_id\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"signInNames.emailAddress\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"signInName\" Required=\"true\" />\n" +
                "            <OutputClaim ClaimTypeReferenceId=\"ezto_verify_url\" />\n" +
                "          </OutputClaims>\n" +
                "          <UseTechnicalProfileForSessionManagement ReferenceId=\"SM-AAD\" />\n" +
                "        </TechnicalProfile>\n" +
                "        <TechnicalProfile Id=\"TriggerEztoLoginRequest\">\n" +
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
                "            <InputClaim ClaimTypeReferenceId=\"ezto_verify_url\" DefaultValue=\"@domain_url@/auth/realms/api/ezto_web_push/@app_id@/@workspace_name@/register\" AlwaysUseDefaultValue=\"true\" />\n" +
                "          </InputClaims>\n" +
                "          <DisplayClaims>\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"auth_req_id\" />\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"signInNames.emailAddress\" />\n" +
                "            <DisplayClaim ClaimTypeReferenceId=\"ezto_verify_url\" />\n" +
                "          </DisplayClaims>\n" +
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
                "            <Item Key=\"ServiceUrl\">@domain_url@/auth/realms/@workspace_name@/protocol/openid-connect/transaction/result</Item>\n" +
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
                "            <InputClaim ClaimTypeReferenceId=\"app_id\" DefaultValue=\"@app_id@\" AlwaysUseDefaultValue=\"true\" />\n" +
                "            <InputClaim ClaimTypeReferenceId=\"app_secret\" DefaultValue=\"@app_secret@\" AlwaysUseDefaultValue=\"true\" />\n" +
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
                "    </ClaimsProvider>\n" +
                "    <!-- EZTO:END:ClaimsProvider -->\n" +
                "  </ClaimsProviders>\n" +
                "  <UserJourneys>\n" +
                "    <UserJourney Id=\"SignIn\">\n" +
                "      <OrchestrationSteps>\n" +
                "        <OrchestrationStep Order=\"1\" Type=\"ClaimsExchange\" ContentDefinitionReferenceId=\"api.signuporsignin\">\n" +
                "          <ClaimsExchanges>\n" +
                "            <ClaimsExchange Id=\"SignIn-WithEmail\" TechnicalProfileReferenceId=\"LocalAccountDiscoveryUsingEmailAddress-SignIn\" />\n" +
                "          </ClaimsExchanges>\n" +
                "        </OrchestrationStep>\n" +
                "        <!-- This step reads any user attributes that we may not have received when in the token. -->\n" +
                "        <OrchestrationStep Order=\"2\" Type=\"ClaimsExchange\">\n" +
                "          <ClaimsExchanges>\n" +
                "            <ClaimsExchange Id=\"AADUserReadWithObjectId\" TechnicalProfileReferenceId=\"AAD-UserReadUsingObjectId\" />\n" +
                "          </ClaimsExchanges>\n" +
                "        </OrchestrationStep>\n" +
                "        <!-- EZTO:START:SIGNIN:OrchestrationStep -->\n" +
                "        <OrchestrationStep Order=\"3\" Type=\"ClaimsExchange\">\n" +
                "          <ClaimsExchanges>\n" +
                "            <ClaimsExchange Id=\"TriggerEztoLoginRequest\" TechnicalProfileReferenceId=\"TriggerEztoLoginRequest\" />\n" +
                "          </ClaimsExchanges>\n" +
                "        </OrchestrationStep>\n" +
                "        <OrchestrationStep Order=\"4\" Type=\"ClaimsExchange\">\n" +
                "          <ClaimsExchanges>\n" +
                "            <ClaimsExchange Id=\"EztoVerifyResult\" TechnicalProfileReferenceId=\"EztoVerifyResult\" />\n" +
                "          </ClaimsExchanges>\n" +
                "        </OrchestrationStep>\n" +
                "        <!-- EZTO:END:SIGNIN:OrchestrationStep -->\n" +
                "        <OrchestrationStep Order=\"5\" Type=\"SendClaims\" CpimIssuerTechnicalProfileReferenceId=\"JwtIssuer\" />\n" +
                "      </OrchestrationSteps>\n" +
                "      <ClientDefinition ReferenceId=\"DefaultWeb\" />\n" +
                "    </UserJourney>\n" +
                "  </UserJourneys>\n" +
                "</TrustFrameworkPolicy>\n";
    }
}
