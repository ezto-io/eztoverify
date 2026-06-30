package com.qlikverify.extensions.admin.misc.base;

import org.jboss.logging.Logger;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AppAuthManager;

public class BaseAuthCheckResource {

    protected final KeycloakSession session;
   // protected final AuthenticationManager.AuthResult auth;
    protected AppAuthManager manager;
    private final Logger logger;

    public BaseAuthCheckResource(KeycloakSession session) {
        this.session = session;
        this.manager = new AppAuthManager();
        //this.auth = manager.authenticateBearerToken(session, session.getContext().getRealm());
        this.logger = Logger.getLogger(BaseAuthCheckResource.class);
        this.session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
    }

//    protected void checkActiveToken(String scope) {
//        logger.info(Constants.UMA_CHECK_START + scope);
//        if (auth == null) {
//            logger.info(Constants.UMA_STATUS + " No Auth");
//            throw new NotAuthorizedException("Bearer");
//        } else if (!auth.getToken().isActive()) {
//            logger.info(Constants.UMA_STATUS + " Not Active");
//            throw new NotAuthorizedException(Constants.UNAUTHORIZED);
//        }
////        try {
////            boolean disableUMA = Boolean.parseBoolean(session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getGeneralSettings().get("disableUMA"));
////            if(disableUMA) {
////                return;
////            }
////        } catch (Exception e){
////            logger.info("Error disable/enable UMA. Please check realm settings.");
////        }
//        String permission = "#"+scope;
//        String headerResourceServer = session.getContext().getRequestHeaders().getHeaderString("resource_id");
//        if(headerResourceServer != null && !headerResourceServer.isEmpty())
//        {
//            permission = headerResourceServer+permission;
//        }
//
//        logger.info("Started Checking Authorization");
//        String tokenUrl = EnvironmentReader.getInstance().getValue(Constants.TOKEN_URL);
//        String baseUrl = EnvironmentReader.getInstance().getValue(Constants.CLOUD_MASTER_DOMAIN);
//        if(tokenUrl != null) {
//            logger.info(Constants.UMA_STATUS + " Received TOKEN URL " + tokenUrl + ".Realm -" + session.getContext().getRealm().getName());
//            tokenUrl = tokenUrl.replace(Constants.REALM_NAME_KEY, session.getContext().getRealm().getName());
//        } else if(baseUrl != null) {
//            tokenUrl = "https://" + baseUrl.replace(Constants.TENENT_PLACEHOLDER, session.getContext().getRealm().getName()) + "/digitanium/v1/token";
//        } else {
//            logger.info(Constants.UMA_STATUS + ": Received TOKEN URL is null");
//            tokenUrl = "http://localhost:8080/auth" + Constants.OPENID_TOKEN_URL.replace(Constants.REALM_NAME_KEY, session.getContext().getRealm().getName());
//        }
//        HttpUtility httpUtility = new HttpUtility();
//        HashMap<String, String> map = new HashMap<>();
//        map.put("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket");
//        String umaClient = EnvironmentReader.getInstance().getValue("UMA_CLIENT");
//        map.put("audience", String.join(",", umaClient == null ? Constants.UMA_CLIENT : umaClient));
//        map.put("permission", permission);
//        map.put("response_mode","decision");
//        String result = httpUtility.handleHttpRequestUrlFormData(tokenUrl, manager.extractAuthorizationHeaderToken(session.getContext().getRequestHeaders()), map);
//        logger.debug(result);
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            HashMap<String, Object> response = objectMapper.readValue(result, HashMap.class);
//            if (response == null) {
//                logger.info(Constants.UMA_STATUS + " UMA result was null");
//                throw new NotAuthorizedException("Bearer");
//            }
//
//            if (response.containsKey("error")) {
//                String error = String.valueOf(response.get("error_description"));
//                logger.info(error);
//                throw new NotAuthorizedException(error);
//            }
//
//            if (response.containsKey("result")) {
//                boolean isValid = Boolean.parseBoolean(String.valueOf(response.get("result")));
//                if (!isValid)
//                    throw new NotAuthorizedException(Constants.UNAUTHORIZED);
//            }
//        }
//        catch (Exception err) {
//            logger.info(Constants.UMA_STATUS + " " + err.getMessage());
//            throw new NotAuthorizedException(Constants.UNAUTHORIZED);
//        }
//    }
}
