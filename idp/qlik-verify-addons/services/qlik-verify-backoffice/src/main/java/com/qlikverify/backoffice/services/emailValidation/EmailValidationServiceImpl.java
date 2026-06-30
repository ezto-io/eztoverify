package com.qlikverify.backoffice.services.emailValidation;

import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmailValidationServiceImpl implements EmailValidationService {
    private static final Logger logger = Logger.getLogger(EmailValidationServiceImpl.class);

    @Override
    public Map<String, Object> getEmailDetails(RequiredActionContext context, String email) throws IOException {
        SimpleHttp.Response response = null;
        try {
            RealmModel adminRealm = context.getSession().realms().getRealmByName(Config.getAdminRealm());

            logger.info("------------External Request-----------------");
            Map<String, Object> reqBody = new HashMap<>();
            response = SimpleHttp.doGet("https://emailvalidation.abstractapi.com/v1/", HttpClients.createDefault()).param("api_key", adminRealm.getAttribute("abstract-api-key"))
                    .param("email", email)
                    .acceptJson().asResponse();
            logger.info("------------External Response-----------------");
            logger.info(response.getStatus());
            logger.info("------------External Response-----------------");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return JsonSerialization.readValue(response.asJson().toString(), Map.class);
    }

}
