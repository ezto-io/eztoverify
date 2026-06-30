package com.qlikverify.backoffice.helpers.emailValidationHelper;

import com.qlikverify.backoffice.services.emailValidation.EmailValidationServiceImpl;
import org.keycloak.authentication.RequiredActionContext;

import java.io.IOException;
import java.util.Map;

public class EmailValidationHelper {
    public static final EmailValidationServiceImpl emailValidationImpl = new EmailValidationServiceImpl();

    public static Map<String, Object> getEmailDetails(RequiredActionContext context, String email) throws IOException {
        return emailValidationImpl.getEmailDetails(context, email);
    }
}
