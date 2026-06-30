package org.keycloak.representations.idm;

import java.util.Map;

public class CibaTutorialRepresentation {
    private String displayName;
    private String description;
    private Map<String,Object> command;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String,Object> getCommand() {
        return command;
    }

    public void setCommand(Map<String,Object> command) {
        this.command = command;
    }
}
