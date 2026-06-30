package com.qlikverify.backoffice.helpers.phoneValidationHelper;

import com.qlikverify.backoffice.services.phoneNumberValidation.PhoneNumberValidationServiceImpl;
import org.keycloak.authentication.RequiredActionContext;

import java.io.IOException;
import java.util.Map;

public class PhoneValidationHelper {

    public static final PhoneNumberValidationServiceImpl phoneValidationImpl = new PhoneNumberValidationServiceImpl();

    public static Map<String, Object> getPhoneNoDetails(RequiredActionContext context, String phoneNo) throws IOException {
        return phoneValidationImpl.getPhoneNumberDetails(context, phoneNo);
    }
}
