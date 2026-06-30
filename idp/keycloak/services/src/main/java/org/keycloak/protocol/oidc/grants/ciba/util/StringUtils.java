package org.keycloak.protocol.oidc.grants.ciba.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.regex.Pattern;

public final class StringUtils {
    private static Pattern whitespacePattern = Pattern.compile("\\s");

    public StringUtils() {
    }

    public static boolean containsWhitespace(String str) {
        Objects.requireNonNull(str);
        return whitespacePattern.matcher(str).find();
    }

    public static boolean secureCompare(String a, String b) {
        byte[] digesta = a.getBytes(StandardCharsets.UTF_8);
        byte[] digestb = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(digesta, digestb);
    }

    public static String toSnakeCase(String str) {
        return str.replaceAll("(.)([A-Z][a-z]+)", "$1_$2").replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}