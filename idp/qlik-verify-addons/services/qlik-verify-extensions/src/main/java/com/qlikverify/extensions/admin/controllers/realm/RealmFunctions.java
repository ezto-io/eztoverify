package com.qlikverify.extensions.admin.controllers.realm;

import com.qlikverify.extensions.admin.misc.helpers.GeneralHelper;
import com.qlikverify.extensions.admin.misc.helpers.LogHelper;
import com.qlikverify.extensions.admin.misc.models.Constants;
import com.qlikverify.extensions.admin.misc.requests.CheckTenantExistsRequest;
import com.qlikverify.extensions.admin.misc.requests.CreateRealmRequest;
import com.qlikverify.extensions.admin.misc.responses.GetRealmResponse;
import com.qlikverify.extensions.admin.misc.responses.QlikVerifyResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.EmailConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSyncConfigModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.clientregistration.policy.impl.CanMultiTenantRegistrationPolicyFactory;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import javax.json.Json;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.extensions.admin.misc.models.Constants.*;

public class RealmFunctions {

    private KeycloakSession session;
    private RealmModel realm;
    private static final Logger logger = Logger.getLogger(RealmFunctions.class);


    public RealmFunctions(KeycloakSession session, AuthenticationManager.AuthResult auth) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    public Response getRealms() {
        try {
            List<RealmModel> realmsList = session.realms().getRealmsStream().collect(Collectors.toList());
            List<GetRealmResponse> responseRealmList = new ArrayList<>();
            for (RealmModel realmData : realmsList) {
                responseRealmList.add(new GetRealmResponse(
                        realmData.getName(),
                        realmData.isEnabled(),
                        realmData.isRememberMe(),
                        realmData.isResetPasswordAllowed(),
                        realmData.isRegistrationAllowed()
                ));
            }
            return Response.status(Response.Status.OK).entity(responseRealmList).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    public Response checkTenantExists(CreateRealmRequest request) throws Exception {

        ClientRegistrationPolicyManager.triggerBeforeRegister(session, RegistrationAuth.AUTHENTICATED, CanMultiTenantRegistrationPolicyFactory.PROVIDER_ID);

        String realmId = request.getTenantName().trim().toLowerCase().replace(" ","-").replace(",","-");
        String adminEmailId = request.getAdminEmail();

        RealmManager realmManager = new RealmManager(session);

        //checking adminEmail is valid or not
        if (adminEmailId == null || adminEmailId.trim().isEmpty()) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.REALM_NULL, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }
        if (!GeneralHelper.isValidEmail(adminEmailId)) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.INVALID_EMAIL_ADDRESS, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }

        //checking if tenantID is not null
        if (realmId.trim().isEmpty()) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.REALM_NULL, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }

        //checking if tenant name matches bucket naming standards
        if (!(realmId.length() >= 3 && realmId.length() <= 63)) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.TENAT_NAME_MIN_LENGTH_REQ, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }
        if (!GeneralHelper.isValidTenantName(realmId)) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.TENANT_NAME_INVALID, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }

        if (GeneralHelper.isReservedTenantName(realmId)) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.TENANT_NAME_RESERVED, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }

        //checking if tenantID is unique or not
        if (realmManager.getRealm(realmId) != null || realmManager.getRealmByName(realmId) != null) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.TENANT_ALREADY_EXISTS, Constants.CONFLICT);
            return Response.status(Response.Status.CONFLICT).entity(qvResponse).build();
        }


        //checking Email Config isSet or not from Master Tenant
        RealmModel masterRealm = realmManager.getRealmByName(org.keycloak.Config.getAdminRealm());
        Optional<EmailConfigModel> emailConfigModel = masterRealm.getEmailConfigsStream().findFirst();

        if (!emailConfigModel.isPresent() || emailConfigModel.get().getConfig() == null || emailConfigModel.get().getConfig().size() == 0) {
            LogHelper.getInstance().error(Constants.EMAIL_CONFIG_MISSING);
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.UNABLE_TO_SEND_EMAIL, Constants.GENERAL_ERROR);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(qvResponse).build();
        }
        session.getContext().setRealm(masterRealm);
        RealmModel createdRealm = realmManager.createRealm(KeycloakModelUtils.generateId(),request.getTenantName(),request.getMode(), request.getSubscriptionType());

        //trigger adminEvt on
        AdminEventBuilder adEvt = new AdminEventBuilder(masterRealm,  new AdminAuth(session.getContext().getRealm(),
                null,null,createdRealm.getClientByClientId("create-realm-app")), session,
                session.getContext().getConnection());
        adEvt.realm(masterRealm.getId());
        adEvt.authRealm(createdRealm);
        adEvt.authUser(null);
        adEvt.authClient(createdRealm.getClientByClientId("create-realm-app"));
        adEvt.operation(OperationType.CREATE).resource(ResourceType.WORKSPACE).success();

       return session.onBoarding().onTenantPostOnboarding(session,createdRealm,adminEmailId);
    }

    public Response isTenantExists(CheckTenantExistsRequest request) throws Exception {
        String realmId = request.getTenantName().trim().toLowerCase().replace(" ","-");

        RealmManager realmManager = new RealmManager(session);
        session.getContext().setRealm(realmManager.getRealmByName(org.keycloak.Config.getAdminRealm()));

        //checking if tenantID is not null
        if (realmId.trim().isEmpty()) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.REALM_NULL, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }

        //checking if tenant name is reserved or not
        if (GeneralHelper.isReservedTenantName(realmId)) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.TENANT_NAME_RESERVED, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }

        //checking if tenant is present or not
        if (realmManager.getRealm(realmId) != null || realmManager.getRealmByName(realmId) != null) {
            String URL = generateConsoleUrl(realmId);
            QlikVerifyResponse qvResponse = new QlikVerifyResponse("true", Constants.OK, URL);
            return Response.status(Response.Status.OK).entity(qvResponse).build();
        } else {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse("false", Constants.NOT_FOUND);
            return Response.status(Response.Status.NOT_FOUND).entity(qvResponse).build();
        }
    }

    private String generateConsoleUrl(String tenantName) {
        String domainName = EnvironmentReader.getValue("ROOT_DNS_NAME", "qlikverify.com");
        String env = EnvironmentReader.getValue("ENV");
        if (env == null || env.isEmpty())
            return "http://localhost:8080/auth/admin/" + tenantName + "/console/";

        if (tenantName != null && tenantName.equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            tenantName = "dashboard";
        }

        if (env.equalsIgnoreCase("prod")) {
            return "https://" + tenantName + "." + domainName;
        }
        return "https://" + tenantName + "." + env  + "." +domainName;
    }

    public Response storeADTokenInDB(String code, String realmId,String connId,String redirectUrl){
        String clientId= EnvironmentReader.getValue("AAD_CLIENT_ID");
        String clientSecret = EnvironmentReader.getValue("AAD_CLIENT_SECRET");
        String finalRedirectUrl = "";
        RealmModel realm = session.realms().getRealm(realmId);
        if(realm == null) {
          QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.REALM_NULL, Constants.BAD_REQUEST);
          return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }
        URI uriInfo = UriBuilder.fromUri( ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth/admin/" + realm.getName() + "/console/#/realms/" + realm.getName() + "/users/directory-sync/connections/azure_ad/" + connId).build();
        try {
            finalRedirectUrl = URLDecoder.decode(uriInfo.toString(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }

        //if code from microsoft was null, then we just redirect
        if(code.equalsIgnoreCase("null")){
            return Response.status(302).location(
                            UriBuilder.fromUri(finalRedirectUrl).build())
                    .entity(AUTHENTICATION_FAILED)
                    .build();
        }

        // else we store the token from Azure
        UserSyncConfigModel model = realm.getUserSyncConfigByProviderId(connId);

        Map<String, String> config = new HashMap<>();
        config.putAll(model.getConfig());

        //code to token
        OkHttpClient client = HttpUtility.getUnsafeOkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String content = "code=" + code + "&grant_type=authorization_code&redirect_uri=" + redirectUrl +"&client_id=" + clientId +"&client_secret="+clientSecret;
        RequestBody body = RequestBody.create(mediaType, content.getBytes());
        Request request = new Request.Builder()
                .url("https://login.microsoftonline.com/organizations/oauth2/v2.0/token")
                .addHeader("Content-Type","application/x-www-form-urlencoded")
                .method("POST", body)
                .build();

        okhttp3.Response response = null;
        try{
            response = client.newCall(request).execute();
            logger.info("AZUREAD :: CODETOTOKEN :: RESPONSE CODE" + response.code());
            if(response.isSuccessful()){
                Map<String,String> resObj = JsonSerialization.readValue(response.body().string(),Map.class);
                response.close();
                config.put(ACCESS_TOKEN, resObj.get(ACCESS_TOKEN));
                config.put(REFRESH_TOKEN,resObj.get(REFRESH_TOKEN));
                model.setConfig(config);
                realm.updateUserSyncConfigModel(model);
            }
        } catch (IOException e){
            logger.error(e.getMessage());
        }

        return Response.status(302).location(
                 UriBuilder.fromUri(finalRedirectUrl).build())
                .build();
    }

    public Response storeGoogleWSTokenInDB(String code, String realmId,String connId,String redirectUrl){
        RealmModel realm = session.realms().getRealm(realmId);
        UserSyncConfigModel model = realm.getUserSyncConfigByProviderId(connId);
        
        String clientId= model.getConfig().get("GWS_CLIENT_ID");
        String clientSecret = model.getConfig().get("GWS_CLIENT_SECRET");
        String finalRedirectUrl = "";

        if(realm == null) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.REALM_NULL, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }
        URI uriInfo = UriBuilder.fromUri( ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth/admin/" + realm.getName() + "/console/#/realms/" + realm.getName() + "/users/directory-sync/connections/google_ws/" + connId).build();
        try {
            finalRedirectUrl = URLDecoder.decode(uriInfo.toString(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }

        //if code from microsoft was null, then we just redirect
        if(code.equalsIgnoreCase("null")){
            return Response.status(302).location(
                            UriBuilder.fromUri(finalRedirectUrl).build())
                    .entity(AUTHENTICATION_FAILED)
                    .build();
        }

        // else we store the token from GOOGLE_WS

        Map<String, String> config = new HashMap<>();
        config.putAll(model.getConfig());

        //code to token
        OkHttpClient client = HttpUtility.getUnsafeOkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String content = "code=" + code + "&grant_type=authorization_code&redirect_uri=" + redirectUrl +"&client_id=" + clientId +"&client_secret="+clientSecret;
        RequestBody body = RequestBody.create(mediaType, content.getBytes());
        Request request = new Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .addHeader("Content-Type","application/x-www-form-urlencoded")
                .method("POST", body)
                .build();

        okhttp3.Response response = null;
        try{
            response = client.newCall(request).execute();
            logger.info("GOOGLE WS :: CODETOTOKEN :: RESPONSE CODE" + response.code());
            if(response.isSuccessful()){
                Map<String,String> resObj = JsonSerialization.readValue(response.body().string(),Map.class);
                response.close();
                config.put(ACCESS_TOKEN, resObj.get(ACCESS_TOKEN));
                config.put(REFRESH_TOKEN,resObj.get(REFRESH_TOKEN));
                model.setConfig(config);
                realm.updateUserSyncConfigModel(model);
            }
        } catch (IOException e){
            logger.error(e.getMessage());
        }

        return Response.status(302).location(
                        UriBuilder.fromUri(finalRedirectUrl).build())
                .build();
    }

    public Response storeOktaWSTokenInDB(String code, String realmId,String connId,String redirectUrl){

        RealmModel realm = session.realms().getRealm(realmId);

        UserSyncConfigModel userSyncConfigModel = realm.getUserSyncConfigByProviderId(connId);

        String clientId= userSyncConfigModel.getConfig().get("clientId");
        String clientSecret = userSyncConfigModel.getConfig().get("clientSecret");

        String finalRedirectUrl = "";
        if(realm == null) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.REALM_NULL, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }
        URI uriInfo = UriBuilder.fromUri( ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth/admin/" + realm.getName() + "/console/#/realms/" + realm.getName() + "/users/directory-sync/connections/okta_ws/" + connId).build();
        try {
            finalRedirectUrl = URLDecoder.decode(uriInfo.toString(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }

        //if code from Okta was null, then we just redirect
        if(code.equalsIgnoreCase("null")){
            return Response.status(302).location(
                            UriBuilder.fromUri(finalRedirectUrl).build())
                    .entity(AUTHENTICATION_FAILED)
                    .build();
        }

        // else we store the token from Okta
        UserSyncConfigModel model = realm.getUserSyncConfigByProviderId(connId);

        Map<String, String> config = new HashMap<>();
        config.putAll(model.getConfig());

        String domainName = model.getConfig().get("domainName");
        String requestUrl = "https://"+domainName+".okta.com/oauth2/default/v1/token";

        //code to token
        OkHttpClient client = HttpUtility.getUnsafeOkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String content = "code=" + code + "&grant_type=authorization_code&redirect_uri=" + redirectUrl +"&client_id=" + clientId +"&client_secret="+clientSecret;
        RequestBody body = RequestBody.create(mediaType, content.getBytes());
        Request request = new Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type","application/x-www-form-urlencoded")
                .method("POST", body)
                .build();

        okhttp3.Response response = null;
        try{
            response = client.newCall(request).execute();
            logger.info("OKTA WS :: CODETOTOKEN :: RESPONSE CODE" + response.code());
            if(response.isSuccessful()){
                Map<String,String> resObj = JsonSerialization.readValue(response.body().string(),Map.class);
                response.close();
                config.put(ACCESS_TOKEN, resObj.get(ACCESS_TOKEN));
                config.put(REFRESH_TOKEN,resObj.get(REFRESH_TOKEN));
                model.setConfig(config);
                realm.updateUserSyncConfigModel(model);
            }
        } catch (IOException e){
            logger.error(e.getMessage());
        }

        return Response.status(302).location(
                        UriBuilder.fromUri(finalRedirectUrl).build())
                .build();
    }
    public Response getWorkspaceURL(String tenantName){
        //checking if tenantName is not null
        if (tenantName.trim().isEmpty()) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.REALM_NULL, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }
        RealmModel realm = session.realms().getRealmByName(tenantName);
        if (realm == null) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(Constants.REALM_NULL, Constants.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity(qvResponse).build();
        }
        String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
        QlikVerifyResponse qvResponse = new QlikVerifyResponse("true", Constants.OK, workspaceUrl);
        return Response.status(Response.Status.OK).entity(qvResponse).build();
    }
}

