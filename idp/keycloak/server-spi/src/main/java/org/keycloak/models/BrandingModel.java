package org.keycloak.models;

import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

public interface BrandingModel {

    String getId();

    String getName();

    void setName(String name);

    boolean isRealmBrand();

    void setRealmBrand(boolean isRealmBrand);

    Map<String, String> getAttributes();

    void setAttribute(String name, String value);

    void removeAttribute(String name);

    String getAttribute(String name);

    List<ProviderConfigProperty> getConfig();

    void setCreatedTimeStamp(long timeStamp);

    long getCreatedTimeStamp();

}
