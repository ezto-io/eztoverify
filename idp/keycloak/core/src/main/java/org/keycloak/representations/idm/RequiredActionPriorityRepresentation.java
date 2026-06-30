package org.keycloak.representations.idm;

public class RequiredActionPriorityRepresentation {
    private String actionId;
    private int priority;
    private String validatorStepId;
    private String direction;
    private String parentId;

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getValidatorStepId() {
        return validatorStepId;
    }

    public void setValidatorStepId(String validatorStepId) {
        this.validatorStepId = validatorStepId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parent) {
        this.parentId = parent;
    }
}

