package com.qlikverify.backoffice.services.phoneNumberValidation;

import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Map;

public class PhoneNumberValidationServiceImpl implements PhoneNumberValidationService {

    private static final Logger logger = Logger.getLogger(PhoneNumberValidationServiceImpl.class);

    @Override
    public Map<String, Object> getPhoneNumberDetails(RequiredActionContext context, String phoneNo) throws IOException {
        SimpleHttp.Response response = null;
        try {
            RealmModel adminRealm = context.getSession().realms().getRealmByName(Config.getAdminRealm());

            logger.info("------------External Request-----------------");
            response = SimpleHttp.doGet("http://apilayer.net/api/validate", HttpClients.createDefault()).param("access_key", adminRealm.getAttribute("numverify-phone-validation"))
                    .param("number", phoneNo)
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

