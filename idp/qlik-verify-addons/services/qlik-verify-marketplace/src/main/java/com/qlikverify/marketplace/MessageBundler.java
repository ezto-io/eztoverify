package com.qlikverify.marketplace;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class MessageBundler {
    private MessageBundler(){

    }

    public static String get(Locale locale, String key) {
        ResourceBundle resourceBundle;
        try {
            resourceBundle = ResourceBundle.getBundle("messages", locale);
        }
        catch (MissingResourceException e){
            resourceBundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
        return resourceBundle.containsKey(key) ? resourceBundle.getString(key) : key;
    }
}
