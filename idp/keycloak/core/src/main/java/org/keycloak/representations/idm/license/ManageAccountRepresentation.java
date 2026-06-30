
package org.keycloak.representations.idm.license;


public class ManageAccountRepresentation {

    private String field;

    private AccountBodyRepresentation arguments;

    public AccountBodyRepresentation getArguments() {
        return arguments;
    }

    public void setArguments(AccountBodyRepresentation arguments) {
        this.arguments = arguments;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
