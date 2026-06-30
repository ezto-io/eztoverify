package com.qlikverify.extensions.admin.misc.helpers;

public class EnvironmentReader {
    private static EnvironmentReader environmentReader;

    public static EnvironmentReader getInstance(){
        if(environmentReader == null)
        {
            environmentReader = new EnvironmentReader();
        }

        return environmentReader;
    }

    public String getValue(String key) {
        return System.getenv(key);
    }
}
