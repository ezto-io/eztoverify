package com.qlikverify.backoffice.validators;

import org.keycloak.models.ModelException;

public class ValidatorPolicyException extends ModelException {
    public ValidatorPolicyException(String e) {
        super(e);
    }
}
