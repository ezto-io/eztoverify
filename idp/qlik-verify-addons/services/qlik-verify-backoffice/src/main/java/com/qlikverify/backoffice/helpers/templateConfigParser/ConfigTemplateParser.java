package com.qlikverify.backoffice.helpers.templateConfigParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.jboss.logging.Logger;
import org.keycloak.services.validation.Validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigTemplateParser {
    private static final Logger logger = Logger.getLogger(ConfigTemplateParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern KEY_PATTERN = Pattern.compile("\\[(?<index>\\d+)\\]|\\.");

    private final String actionVariables;
    private final Map<String, String> trxAttributes;
    private final Map<String, String> cache;

    private final List<String> staticPlaceHolders = List.of("Date", "GenerateUUID", "Replace", "ToUpperCase", "ToLowerCase", "Split");


    public ConfigTemplateParser(String actionVariables, Map<String, String> trxAttributes) {
        this.actionVariables = actionVariables;
        this.trxAttributes = new ConcurrentHashMap<>(trxAttributes);
        this.cache = new ConcurrentHashMap<>();
    }

    public ConfigTemplateParser() {
        this.actionVariables = "";
        this.trxAttributes = new ConcurrentHashMap<>();
        this.cache = new ConcurrentHashMap<>();
    }


    private static JsonNode parseCustomJson(String input) throws Exception {
        String validJson = input
                .replaceAll("([\\w\\.]+)", "\"$1\"") // Wrap keys in quotes
                .replaceAll("(?<=\\{|,)(\\s*):", "\"\":") // Replace empty values with ""
                .replaceAll(":\\s*,", ":\"\",") // Handle cases where values are empty
                .replaceAll(":\\s*}$", ":\"\"}");
        return objectMapper.readTree(validJson);
    }

    public String getValue(String key) {
        return cache.computeIfAbsent(key, this::computeValue);
    }

    private String computeValue(String key) {
        key = cleanKey(key);
        List<String> keys = parseKeys(key);
        boolean isFromActionVariables = keys.remove("env");

        JsonNode currentNode = isFromActionVariables ? parseActionVariables() : parseTrxAttributes();
        String valueFromJsonNode = getValueFromJsonNode(keys, currentNode);
        if(valueFromJsonNode == null && staticPlaceHolders.stream()
                .anyMatch(key::contains)) {
            return key;
        }
        return (valueFromJsonNode == null || Validation.isBlank(valueFromJsonNode)) ? "" : valueFromJsonNode;
    }

    private String cleanKey(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            return key.substring(2, key.length() - 1);
        }
        return key.contains("${") ? parseTemplate(key) : key;
    }

    private JsonNode parseActionVariables() {
        try {
            return objectMapper.readTree(actionVariables);
        } catch (JsonProcessingException e) {
            logger.error("Unable to parse action variables: " + e.getMessage());
            return null;
        }
    }

    private JsonNode parseTrxAttributes() {
        Map<String, String> updatedAttributes = trxAttributes.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().startsWith("sp@") ? e.getKey().substring(3) : e.getKey(),
                        Map.Entry::getValue,
                        (existingValue, newValue) -> newValue
                ));
        return objectMapper.valueToTree(updatedAttributes);
    }

    private List<String> parseKeys(String key) {
        List<String> keys = new ArrayList<>();
        Matcher matcher = KEY_PATTERN.matcher(key);
        int lastMatch = 0;

        while (matcher.find()) {
            if (lastMatch != matcher.start()) {
                keys.add(key.substring(lastMatch, matcher.start()));
            }
            String index = matcher.group("index");
            if (index != null) {
                keys.add(index);
            }
            lastMatch = matcher.end();
        }

        if (lastMatch != key.length()) {
            keys.add(key.substring(lastMatch));
        }

        return keys;
    }

    private String getValueFromJsonNode(List<String> keys, JsonNode node) {
        for (String key : keys) {
            node = navigateJsonNode(node, key);
            if (node == null) return getValueFromTrxAttribute(key);
        }

        return extractValueFromNode(node);
    }

    private JsonNode navigateJsonNode(JsonNode node, String key) {
        if (node == null) return null;

        if (node.isTextual()) {
            JsonNode jsonNode = parseTextualNode(node);
            if (jsonNode != null && jsonNode.getNodeType().equals(JsonNodeType.OBJECT)) {
                node = jsonNode.get(key);
                return node;
            } else {
                return jsonNode;
            }
        }

        if (node.isArray()) {
            return parseArrayNode(node, key);
        }

        return node.isObject() ? node.get(key) : null;
    }

    private JsonNode parseTextualNode(JsonNode node) {
        try {
            return objectMapper.readTree(node.asText());
        } catch (JsonProcessingException e) {
            logger.debug("Failed to parse JSON string: " + e.getMessage());
            return null;
        }
    }

    private JsonNode parseArrayNode(JsonNode node, String key) {
        try {
            int index = Integer.parseInt(key);
            return (index >= 0 && index < node.size()) ? node.get(index) : null;
        } catch (NumberFormatException e) {
            return node.get(key);
        }
    }

    private String extractValueFromNode(JsonNode node) {
        if (node == null) return "";

        if (node.isTextual()) return node.asText();
        if (node.isObject()) return convertNodeToString(node);

        return node.asText();
    }

    private String convertNodeToString(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "";
        }
    }

    private String getValueFromTrxAttribute(String key) {
        String trxValue = trxAttributes.get("sp@" + key);
        if (trxValue == null || !Validation.isBlank(trxValue))
            return trxValue;

        trxValue = trxAttributes.get(key);
        return trxValue == null || Validation.isBlank(trxValue) ? "" : trxValue;  // need to return empty value instead of key
    }

    public String parseTemplate(String template) {
        StringBuffer result = new StringBuffer(template);
        int startIndex = 0;

        while (startIndex < result.length()) {
            int placeholderStart = result.indexOf("${", startIndex);
            int placeholderEnd = findMatchingClosingBrace(result, placeholderStart);
            if (placeholderStart == -1 || placeholderEnd == -1) {
                break;
            }

            String placeholder = result.substring(placeholderStart + 2, placeholderEnd);
            String value = computeValue(placeholder);
            value = processPlaceholderValue(placeholder, value);
            result.replace(placeholderStart, placeholderEnd + 1, value);
            startIndex = placeholderStart + value.length();
        }

        return result.toString();
    }

    private String processPlaceholderValue(String placeholder, String value) {
        if (value == null) return "";

        if ("GenerateUUID".equalsIgnoreCase(placeholder)) {
            return UUID.randomUUID().toString();
        } else if (value.startsWith("Date")) {
            return formatDate(value);
        } else if (placeholder.startsWith("Replace")) {
            return handleReplacement(value);
        } else if (value.startsWith("ToUpperCase")) {
            return convertToUpperCase(value);
        } else if (value.startsWith("ToLowerCase")) {
            return convertToLowerCase(value);
        } else if (value.startsWith("Split")) {
            return splitValues(value);
        } else if (value.equalsIgnoreCase(placeholder)) {
            return "";
        }
        return value.trim();
    }

    private String convertToUpperCase(String placeholder) {
        //${ToUpperCase(${env.name})}
        String substring = placeholder.substring(12, placeholder.length() - 1);
        return substring.toUpperCase();
    }

    private String convertToLowerCase(String placeholder) {
        //${ToLowerCase(${env.name})}
        String substring = placeholder.substring(12, placeholder.length() - 1);
        return substring.toLowerCase();
    }

    private String splitValues (String value) {
        if(value == null || value.length() < 0) {
            return "";
        }

        String[] values = value.substring(6, value.length()-1).split(",");
        if(values.length != 3) {
            return "";
        }

        String textToBeSplitted = values[0].trim();
        if(textToBeSplitted.isBlank()) {
            return "";
        }

        int startingPosition, endingPosition;
        try {
            startingPosition = Integer.parseInt(values[1].trim());
            endingPosition = Integer.parseInt(values[2].trim());
        } catch (NumberFormatException e) {
            return "";
        }

        if(startingPosition < 0 || startingPosition > textToBeSplitted.length() || endingPosition > textToBeSplitted.length() || startingPosition >= endingPosition) {
            return "";
        }
        return textToBeSplitted.substring(startingPosition, endingPosition);
    }

    private String handleReplacement(String placeholder) {
        //${Replace(${env.gender}, {m: 1, f: 2})}
        String substring = placeholder.substring(8, placeholder.length() - 1);
        try {
            String[] split = substring.split(",", 2);
            JsonNode jsonNode = parseCustomJson(split[1].trim());
            if (jsonNode.get(split[0]) != null) {
                return jsonNode.get(split[0].trim()).asText();
            }
            return split[0];
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDate(String value) {
        //${Date(${env.dob}, dd-MM-yyyy)}
        String[] dates = value.substring(5, value.length() - 1).split(",");

        try {
            if (dates.length == 2 && !dates[0].isEmpty() && !dates[1].isEmpty()) {
                if (dates[0].contains("/")) {
                    dates[0] = dates[0].trim().replaceAll("/", "-");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dates[1].trim());
                    LocalDate date = LocalDate.parse(dates[0].trim(), formatter);
                    return date.format(formatter);
                }
                LocalDate date = LocalDate.parse(dates[0].trim());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dates[1].trim());
                return date.format(formatter);
            }
        } catch (Exception e) {
            logger.error("formatDate :: Error while parsing date:: " + e.getMessage());
        }
        return dates[0];
    }

    private int findMatchingClosingBrace(StringBuffer sb, int start) {
        int openBraces = 1;
        for (int i = start + 2; i < sb.length(); i++) {
            if (sb.charAt(i) == '{') {
                openBraces++;
            } else if (sb.charAt(i) == '}') {
                openBraces--;
                if (openBraces == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}