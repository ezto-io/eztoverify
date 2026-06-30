package org.keycloak.representations.idm;

public class AuthenticationExecutionPriorityRepresentation {
    private String executionId;
    private int priority;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
