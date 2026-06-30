package org.keycloak.policy.ciba;

import org.keycloak.models.ModelException;

/**
 * @QlikVerify
 */
public class CibaPolicyValidationException extends ModelException {
    public CibaPolicyValidationException(String e) {
        super(e);
    }
}
