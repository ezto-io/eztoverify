package com.qlikverify.services.resources;

import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class BlacklistingMetadataRepresentation {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    static {
        ProviderConfigProperty providerConfigProperty;

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bl-enable");
        providerConfigProperty.setLabel("bl-enable-label");
        providerConfigProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("bl-enable-hlp-txt");
        providerConfigProperty.setDefaultValue(false);
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("ip-address");
        providerConfigProperty.setLabel("ip-address-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("ip-address-hlp-txt");
        providerConfigProperty.setDefaultValue("");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("email");
        providerConfigProperty.setLabel("email-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("email-hlp-txt");
        providerConfigProperty.setDefaultValue("");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("phone");
        providerConfigProperty.setLabel("phone-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("phone-hlp-txt");
        providerConfigProperty.setDefaultValue("");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("username");
        providerConfigProperty.setLabel("username-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("username-hlp-txt");
        providerConfigProperty.setDefaultValue("");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bl-country");
        providerConfigProperty.setLabel("bl-country-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("bl-country-hlp-txt");
        providerConfigProperty.setDefaultValue("");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bl-continent");
        providerConfigProperty.setLabel("bl-continent-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("bl-continent-hlp-txt");
        providerConfigProperty.setDefaultValue("");
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bl-location");
        providerConfigProperty.setLabel("bl-location-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("bl-location-hlp-txt");
        providerConfigProperty.setDefaultValue("");
        configProperties.add(providerConfigProperty);

    }

    public List<ProviderConfigProperty> getConfigProperties (){
        return configProperties;
    }
}
