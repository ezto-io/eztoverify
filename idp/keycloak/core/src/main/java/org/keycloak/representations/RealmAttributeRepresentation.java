package org.keycloak.representations;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class RealmAttributeRepresentation {

    private String providerId;
    private String label;
    private String attributeName;
    private String attributeValue;
    private boolean isSecret;
    private boolean canBeViewed;
    private boolean canBeRotated;
    private boolean canBeUpdated;
    private boolean canGenerate;
    private boolean isReadOnly;
    private String helpText;
    @JsonProperty("json")
    private boolean isJson;
    @JsonProperty("javascript")
    private boolean isJavascript;
    @JsonProperty("showInSubTenant")
    private boolean isShowInSubTenant;
    @JsonProperty("toggle")
    private boolean isToggle;
    private boolean hidden;
    private int priority;
    private boolean isNew;
}
