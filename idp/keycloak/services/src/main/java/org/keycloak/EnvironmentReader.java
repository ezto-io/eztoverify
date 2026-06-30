package org.keycloak;

public class EnvironmentReader {
    public static String getValue(String key) {
        return System.getenv(key);
    }

    public static String getValue(String key, String defaultValue) {
        String value = getValue(key);
        if(value == null || value.isEmpty())
            return defaultValue;

        return value;
    }
}

