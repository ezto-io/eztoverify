package com.qlikverify.backoffice.services.phoneNumberValidation;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.RealmModel;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface PhoneNumberValidationService {
    Map<String, Object> getPhoneNumberDetails(RequiredActionContext context, String phoneNo) throws IOException;

}
