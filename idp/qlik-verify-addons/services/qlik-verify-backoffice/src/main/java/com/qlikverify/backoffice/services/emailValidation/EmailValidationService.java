package com.qlikverify.backoffice.services.emailValidation;

import org.keycloak.authentication.RequiredActionContext;

import java.io.IOException;
import java.util.Map;

public interface EmailValidationService {
    public Map<String, Object> getEmailDetails(RequiredActionContext context, String email) throws IOException;
}
