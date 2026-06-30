package com.qlikverify.backoffice.configDownloads;

import org.keycloak.Config;
import org.keycloak.downloads.ConfigDownloadProvider;
import org.keycloak.downloads.ConfigDownloadProviderFactory;
import org.keycloak.models.CibaAuthConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import static com.qlikverify.backoffice.authorizers.AzureB2CAuthorizer.AZURE_B2C_TENANT_ID;
import static com.qlikverify.backoffice.authorizers.AzureB2CAuthorizer.AZURE_B2C_TENANT_ID_OBJECT;

public class AzureSigninXmlConfigDownloadProvider implements ConfigDownloadProvider, ConfigDownloadProviderFactory {
    KeycloakSession session;
    @Override
    public String getDownloadFile(ClientModel clientModel) {
        String text = getText();
        final CibaAuthConfigModel cibaConfigByClientId =
                getCibaAuthConfig(clientModel);

        text = text
                .replaceAll("@b2c_tenant_id@",
                        cibaConfigByClientId.getConfig().getOrDefault(AZURE_B2C_TENANT_ID, ""))
                .replaceAll("@b2c_tenant_object_id@",
                        cibaConfigByClientId.getConfig().getOrDefault(AZURE_B2C_TENANT_ID_OBJECT, ""));
        return text;
    }

    @Override
    public String getFileName() {
        return "B2C_1A_SIGNIN.xml";
    }


    @Override
    public ConfigDownloadProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return "azure-b2c-signin-provider";
    }

    private String getText(){
        return "<TrustFrameworkPolicy xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://schemas.microsoft.com/online/cpim/schemas/2013/06\" PolicySchemaVersion=\"0.3.0.0\" TenantId=\"@b2c_tenant_id@\" PolicyId=\"B2C_1A_Signin\" PublicPolicyUri=\"http://@b2c_tenant_id@/B2C_1A_Signin\" TenantObjectId=\"@b2c_tenant_object_id@\">\n" +
                "  <BasePolicy>\n" +
                "    <TenantId>@b2c_tenant_id@</TenantId>\n" +
                "    <PolicyId>B2C_1A_TrustFrameworkBase</PolicyId>\n" +
                "  </BasePolicy>\n" +
                "  <RelyingParty>\n" +
                "    <DefaultUserJourney ReferenceId=\"SignIn\" />\n" +
                "    <!-- debugging using application insights -->\n" +
                "    <UserJourneyBehaviors>\n" +
                "      <ScriptExecution>Allow</ScriptExecution>\n" +
                "    </UserJourneyBehaviors>\n" +
                "    <TechnicalProfile Id=\"PolicyProfile\">\n" +
                "      <DisplayName>PolicyProfile</DisplayName>\n" +
                "      <Protocol Name=\"OpenIdConnect\" />\n" +
                "      <OutputClaims>\n" +
                "        <OutputClaim ClaimTypeReferenceId=\"displayName\" />\n" +
                "        <OutputClaim ClaimTypeReferenceId=\"email\" />\n" +
                "        <OutputClaim ClaimTypeReferenceId=\"objectId\" PartnerClaimType=\"sub\" />\n" +
                "        <OutputClaim ClaimTypeReferenceId=\"tenantId\" AlwaysUseDefaultValue=\"true\" DefaultValue=\"{Policy:TenantObjectId}\" />\n" +
                "      </OutputClaims>\n" +
                "      <SubjectNamingInfo ClaimType=\"sub\" />\n" +
                "    </TechnicalProfile>\n" +
                "  </RelyingParty>\n" +
                "</TrustFrameworkPolicy>";
    }

    private CibaAuthConfigModel getCibaAuthConfig(ClientModel clientModel) {
        return session.getContext().getRealm().getCibaAuthConfigByClientId(clientModel.getClientId(),
                session.getContext().getRealm().getId());
    }
}
