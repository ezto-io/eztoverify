package com.qlikverify.extensions.admin.misc.helpers;

import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.TrxModel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GeneralHelper {
    private static final Logger logger = Logger.getLogger(GeneralHelper.class);

    private static GeneralHelper generalHelper;
    public static final String GENERIC_ERROR = "Our services failed to process this request, if error persists please contact support.";
    public static String DECENTRO = "decentro";
    public static String SIGNZY = "signzy";
    public static String SUREPASS = "surepass";

    public static GeneralHelper getInstance() {
        if (generalHelper == null) {
            generalHelper = new GeneralHelper();
        }
        return generalHelper;
    }

    public static boolean isValidEmail(String email) {
        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,63}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static boolean isValidTenantName(String realmId){
        String regex = "^(?!-.)[a-z0-9A-Z- ]{3,55}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(realmId);
        return matcher.matches();
    }
    public static boolean isReservedTenantName(String tenantName){
        String env = EnvironmentReader.getValue("RESERVED_TENANT_NAMES");
        if(env == null || env.isEmpty()){
            logger.info("Environment variable is Empty.Ignoring Reserved Tenant Name Check");
            return false;
        }
        String[] allowedNames = env.split(",");
        return  Arrays.stream(allowedNames).anyMatch(x-> x.equals(tenantName));
    }

    public static boolean isV0Supported(RealmModel realm) {
        Optional<ClientModel> isV0Supported = realm.getClientsStream().filter(clientModel -> clientModel.getAttribute("hidden_ua_multi_version") != null && Boolean.parseBoolean(clientModel.getAttribute("hidden_ua_multi_version"))).findAny();
        return isV0Supported.isPresent();
    }

    public static boolean checkForAvailableMetadatas(String requestId,RealmModel realmModel, Map<String, String> response) {

        List<String> keyValues = new ArrayList<>();
        keyValues.addAll(response.keySet());

        boolean result;
        result = keyValues.stream().map(key -> {
            String value = String.valueOf(response.get(key));
            TrxModel trxModel = realmModel.getTrx(requestId);
            return !trxModel.getAttributes().containsKey("sp@" + key) || (trxModel.getAttributes().get("sp@" + key).isBlank() || value.equals("")) || (trxModel.getAttributes().get("sp@" + key).equals(value));
        }).collect(Collectors.toList()).contains(false);
        return !result;
    }

    public String sanitizeErrorMessage(String error){
        if(error == null)
            return null;
        error = (error.contains(DECENTRO) || error.contains(DECENTRO.toUpperCase())) ? GENERIC_ERROR : error;
        error = (error.contains(SUREPASS) || error.contains(SUREPASS.toUpperCase())) ? GENERIC_ERROR : error;
        error = (error.contains(SIGNZY) || error.contains(SIGNZY.toUpperCase())) ? GENERIC_ERROR : error;
        return error;
    }
}
