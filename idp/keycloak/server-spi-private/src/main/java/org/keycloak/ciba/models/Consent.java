
package org.keycloak.ciba.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Consent {

    private String type;
    private String title;
    private Object message;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonIgnore
    public String getStringMessage() {
        return (String) message;
    }

    @JsonIgnore
    public Map<String, Object> getJsonMessage() {
        return JsonSerialization.convertValue(message, Map.class);
    }

    @JsonIgnore
    public List<LinkedHashMap> getFormMessage() {
        return JsonSerialization.convertValue(message, List.class);
    }

    @JsonIgnore
    public Object getFormattedMessage() {
        if (type.equals("string"))
            return getStringMessage();

        if (type.equals("json"))
            return getJsonMessage();

        if (type.equals("form"))
            return getFormMessage();

        return null;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }
}
