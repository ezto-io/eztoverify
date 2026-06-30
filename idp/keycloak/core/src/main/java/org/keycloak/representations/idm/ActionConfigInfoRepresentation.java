package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

public class ActionConfigInfoRepresentation {

    protected String name;
    protected String providerId;
    protected String helpText;
    protected boolean hasTestConfig;
    protected boolean isConfigurable;
    protected Map<String, Object> constraints;
    protected boolean hasPolicy;

    protected List<ConfigPropertyRepresentation> properties;
    protected Map<String, String> info;
    protected boolean shouldShowSkipOption;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHelpText() {
        return helpText;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public boolean isHasTestConfig() {
        return hasTestConfig;
    }

    public void setHasTestConfig(boolean hasTestConfig) {
        this.hasTestConfig = hasTestConfig;
    }

    public boolean isConfigurable() {
        return isConfigurable;
    }

    public void setConfigurable(boolean configurable) {
        isConfigurable = configurable;
    }

    public List<ConfigPropertyRepresentation> getProperties() {
        return properties;
    }

    public void setProperties(List<ConfigPropertyRepresentation> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }

    public boolean isHasPolicy() {
        return hasPolicy;
    }

    public void setHasPolicy(boolean hasPolicy) {
        this.hasPolicy = hasPolicy;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public void setInfo(Map<String, String> info) {
        this.info = info;
    }

    public boolean isShouldShowSkipOption() {
        return shouldShowSkipOption;
    }

    public void setShouldShowSkipOption(boolean shouldShowSkipOption) {
        this.shouldShowSkipOption = shouldShowSkipOption;
    }
}


