package com.qlikverify.backoffice.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class ValidatorPolicy implements Serializable {

    private String propKey;

    private String propType;

    private String propComparator;

    private transient Object propValue;

    protected static final String EXISTS = "exists";

    protected static final String DOES_NOT_EXISTS = "doesn't exist";

    private static final String IS_EQUAL_TO = "is equal to";

    private static final String IS_NOT_EQUAL_TO = "isn't equal to";

    private static final String CONTAINS = "contains";

    private static final String DOES_NOT_CONTAINS = "doesn't contain";

    private static final String IS_TRUE = "is true";

    private static final String IS_FALSE = "is false";

    @Getter
    public enum ValidationTypes {
        STRING("string"),
        NUMBER("number"),
        DATE_AND_TIME("dt"),
        BOOLEAN("boolean"),
        ARRAY("array"),
        OBJECT("object");

        ValidationTypes(String value) {
            this.value = value;
        }

        private final String value;
    }

    static List<String> types = Arrays.stream(ValidationTypes.values()).map(ValidationTypes::getValue)
            .collect(Collectors.toList());

    static List<String> stringConditions = List.of(
            IS_EQUAL_TO,
            IS_NOT_EQUAL_TO,
            CONTAINS,
            DOES_NOT_CONTAINS,
            "starts with",
            "doesn't start with",
            "ends with",
            "doesn't end with",
            "matches regex",
            "doesn't match regex"
    );

    static List<String> numberConditions = List.of(
            IS_EQUAL_TO,
            IS_NOT_EQUAL_TO,
            "is greater than",
            "is less than",
            "is greater than or equal",
            "is less than or equal"
    );

    static List<String> dtConditions = List.of(
            "is after",
            "is before",
            "is after or equal",
            "is before or equal"
    );

    static List<String> booleanConditions = List.of(
            EXISTS,
            DOES_NOT_EXISTS,
            IS_TRUE,
            IS_FALSE
    );

    static List<String> arrayConditions = List.of(
            CONTAINS,
            DOES_NOT_CONTAINS,
            "length equal to",
            "length not equal to",
            "length greater than",
            "length greater than or equal",
            "length less than",
            "length less than or equal"
    );

    static List<String> objectConditions = List.of(
            EXISTS,
            DOES_NOT_EXISTS,
            IS_EQUAL_TO,
            IS_NOT_EQUAL_TO
    );

    static List<String> valueNonSupportedConditions = List.of(
            EXISTS,
            DOES_NOT_EXISTS,
            IS_TRUE,
            IS_FALSE
    );

    //~ ====================================================================================================================================
    // Starts validator parsing helper methods

    /**
     * Parses a policy string into a {@link ValidatorPolicy} object.
     * <p>
     * The expected syntax for the policy string is "(propKey) (propType) (propComparator) (propValue)". The method replaces
     * parentheses with '@' and splits the string into parts. If the policy has a "decision" key, it sets only the key and value.
     * For other keys, it sets the key, type, comparator, and value in the {@link ValidatorPolicy}.
     * </p>
     *
     * @param policy the policy string to parse
     * @return a {@link ValidatorPolicy} object with properties set according to the parsed values
     * @throws ValidatorPolicyException if the policy string does not match the expected syntax
     */
    public static ValidatorPolicy parseValidatorPolicy(String policy) {
        ValidatorPolicy validatorPolicy = new ValidatorPolicy();
        String modifiedPolicy = policy.replace("(","@").replace(")","@");
        String[] propSplit = modifiedPolicy.split("@ @");

        if (propSplit.length < 3 || propSplit.length > 4)
            throw new ValidatorPolicyException("Invalid policy. It didn't match the order of execution");

        String prKey = parseSubString(propSplit[0]);

        if(prKey.equalsIgnoreCase("decision")) {
            validatorPolicy.setPropKey(parseSubString(propSplit[0]));
            validatorPolicy.setPropValue(parseSubString(propSplit[1]));
            return validatorPolicy;
        }

        validatorPolicy.setPropKey(prKey);
        validatorPolicy.setPropType(parseSubString(propSplit[1]));
        validatorPolicy.setPropComparator(parseSubString(propSplit[2]));
        if (propSplit.length == 4)
            validatorPolicy.setPropValue(parseSubString(propSplit[3]));

        return validatorPolicy;
    }

    private static String parseSubString(String polSubString) {
        if(polSubString.startsWith("@"))
            return polSubString.substring(1);

        if(polSubString.endsWith("@"))
            return polSubString.substring(0, polSubString.length() - 1);

        return polSubString;
    }

    //~ ====================================================================================================================================
    // Starts validating methods

    public boolean evaluate(ValidatorPolicy validatorPolicy, Object resultValue) {
        switch (validatorPolicy.getPropType()) {
            case "string":
                return validateString(validatorPolicy.getPropComparator(), String.valueOf(resultValue), String.valueOf(validatorPolicy.getPropValue()));
            case "boolean":
                return validateBoolean(validatorPolicy.getPropComparator(), Boolean.valueOf(String.valueOf(resultValue)), Boolean.valueOf(String.valueOf(validatorPolicy.getPropValue())));
            case "number":
                return validateNumber(validatorPolicy.getPropComparator(), resultValue, validatorPolicy.getPropValue());
            case "dt":
                return validateDateTime(validatorPolicy.getPropComparator(), resultValue, validatorPolicy.getPropValue());
            case "object":
                return validateObject(validatorPolicy.getPropComparator(), validatorPolicy, resultValue);
            case "array":
                return validateArray(validatorPolicy.getPropComparator(), toObjectArray(resultValue), toObjectArray(validatorPolicy.getPropValue()));
            default:
                throw new ValidatorPolicyException("Invalid property type: " + validatorPolicy.getPropType());
        }
    }

    public boolean evaluateTypesFromRequestMetadata(ValidatorPolicy validatorPolicy, Object resultValue) {
        switch (validatorPolicy.getPropType()) {
            case "object":
                return validateObjectFromMetadata(validatorPolicy.getPropComparator(), validatorPolicy, resultValue);
            default:
                throw new ValidatorPolicyException("Invalid property type: " + validatorPolicy.getPropType());
        }
    }

    private boolean validateString(String comparator, String resultValue, String configValue) {
        switch (comparator) {
            case IS_EQUAL_TO:
                return resultValue.equals(configValue);
            case IS_NOT_EQUAL_TO:
                return !resultValue.equals(configValue);
            case CONTAINS:
                return resultValue.contains(configValue);
            case DOES_NOT_CONTAINS:
                return !resultValue.contains(configValue);
            case "starts with":
                return resultValue.startsWith(configValue);
            case "doesn't start with":
                return !resultValue.startsWith(configValue);
            case "ends with":
                return resultValue.endsWith(configValue);
            case "doesn't end with":
                return !resultValue.endsWith(configValue);
            case "matches regex":
                return resultValue.matches(configValue);
            case "doesn't match regex":
                return !resultValue.matches(configValue);
            default:
                throw new ValidatorPolicyException("Invalid comparator for string: " + comparator);
        }
    }

    private boolean validateNumber(String comparator, Object resultValue, Object configValue) {
        try {
            double result = parseToDouble(resultValue);
            double config = parseToDouble(configValue);

            switch (comparator) {
                case IS_EQUAL_TO:
                    return Math.abs(result - config) < 1e-9; // Using epsilon for floating-point comparison
                case IS_NOT_EQUAL_TO:
                    return Math.abs(result - config) >= 1e-9;
                case "is greater than":
                    return result > config;
                case "is less than":
                    return result < config;
                case "is greater than or equal":
                    return result >= config;
                case "is less than or equal":
                    return result <= config;
                default:
                    throw new ValidatorPolicyException("Invalid comparator for number: " + comparator);
            }
        } catch (NumberFormatException e) {
            throw new ValidatorPolicyException("Invalid number format: " + e.getMessage());
        }
    }

    private boolean validateDateTime(String comparator, Object resultValue, Object configValue) {
        LocalDateTime resultDateTime = parseToLocalDateTime(resultValue);
        LocalDateTime configDateTime = parseToLocalDateTime(configValue);

        switch (comparator) {
            case "is after":
                return resultDateTime.isAfter(configDateTime);
            case "is before":
                return resultDateTime.isBefore(configDateTime);
            case "is after or equal":
                return !resultDateTime.isBefore(configDateTime);
            case "is before or equal":
                return !resultDateTime.isAfter(configDateTime);
            default:
                throw new ValidatorPolicyException("Invalid comparator for date and time: " + comparator);
        }
    }

    private boolean validateBoolean(String comparator, Boolean resultValue, Boolean configValue) {
        switch (comparator) {
            case IS_EQUAL_TO:
                return resultValue.equals(configValue);
            case IS_NOT_EQUAL_TO:
                return !resultValue.equals(configValue);
            case IS_TRUE:
                return Boolean.TRUE.equals(resultValue);
            case IS_FALSE:
                return Boolean.FALSE.equals(resultValue);
            default:
                throw new ValidatorPolicyException("Invalid comparator for boolean: " + comparator);
        }
    }

    private boolean validateArray(String comparator, Object[] resultValue, Object[] configValue) {
        switch (comparator) {
            case CONTAINS:
                return new HashSet<>(Arrays.asList(resultValue)).containsAll(Arrays.asList(configValue));
            case DOES_NOT_CONTAINS:
                return !new HashSet<>(Arrays.asList(resultValue)).containsAll(Arrays.asList(configValue));
            case "length equal to":
                return resultValue.length == Integer.parseInt(configValue[0].toString());
            case "length not equal to":
                return resultValue.length != Integer.parseInt(configValue[0].toString());
            case "length greater than":
                return resultValue.length > Integer.parseInt(configValue[0].toString());
            case "length greater than or equal":
                return resultValue.length >= Integer.parseInt(configValue[0].toString());
            case "length less than":
                return resultValue.length < Integer.parseInt(configValue[0].toString());
            case "length less than or equal":
                return resultValue.length <= Integer.parseInt(configValue[0].toString());
            default:
                throw new ValidatorPolicyException("Invalid comparator for array: " + comparator);
        }
    }

    private boolean validateObject(String comparator, ValidatorPolicy validatorPolicy, Object resultValue) {
        try {
            String propertyValue = String.valueOf(validatorPolicy.getPropValue());

            switch (comparator) {
                case IS_EQUAL_TO:
                    return resultValue.equals(propertyValue);
                case IS_NOT_EQUAL_TO:
                    return !resultValue.equals(propertyValue);
                default:
                    throw new ValidatorPolicyException("Invalid comparator for object: " + comparator);
            }
        } catch (Exception e) {
            throw new ValidatorPolicyException("Error validating object: " + e.getMessage());
        }
    }

    private boolean validateObjectFromMetadata(String comparator, ValidatorPolicy validatorPolicy, Object resultValue) {
        final String METADATA = "metadata";
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode resultNode;
            if (resultValue instanceof String)
                resultNode = mapper.readTree((String) resultValue);
            else
                resultNode = mapper.readTree(String.valueOf(resultValue));

            String propertyKey = validatorPolicy.getPropKey();
            String propertyValue = String.valueOf(validatorPolicy.getPropValue());

            boolean keyExists = resultNode.has(METADATA) && resultNode.get(METADATA).has(propertyKey);
            JsonNode valueNode = resultNode.get(METADATA).get(propertyKey);

            switch (comparator) {
                case EXISTS:
                    return keyExists;
                case DOES_NOT_EXISTS:
                    return !keyExists;
                case IS_EQUAL_TO:
                    return objectEqualsCheck(valueNode, propertyValue);
                case IS_NOT_EQUAL_TO:
                    return !objectEqualsCheck(valueNode, propertyValue);
                default:
                    throw new ValidatorPolicyException("Invalid comparator for object: " + comparator);
            }
        } catch (Exception e) {
            throw new ValidatorPolicyException("Error validating object: " + e.getMessage());
        }
    }

    public boolean objectEqualsCheck(JsonNode valueNode, String propertyValue) {
        boolean isEqual;
        if(valueNode == null) {
            isEqual = false;
        } else if (valueNode.isTextual()) {
            String value = valueNode.asText().replaceAll("^\"|\"$", "");
            isEqual = value.equals(propertyValue);
        } else if (valueNode.isBoolean()) {
            isEqual = valueNode.asBoolean() == Boolean.parseBoolean(propertyValue);
        } else if (valueNode.isNumber()) {
            isEqual = valueNode.asDouble() == Double.parseDouble(propertyValue);
        } else {
            isEqual = valueNode.toString().equals(propertyValue);
        }
        return isEqual;
    }

    //~ ====================================================================================================================================
    // Helper methods

    /**
     * Parses an object to LocalDateTime. The object can be a date string, a timestamp (long), or already a LocalDateTime.
     *
     * @param value The value to parse.
     * @return The parsed LocalDateTime.
     * @throws ValidatorPolicyException if the value cannot be parsed to a valid date/time.
     */
    private LocalDateTime parseToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof String) {
            String strValue = (String) value;
            List<DateTimeFormatter> formatters = Arrays.asList(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,      // "2017-11-23T10:15:30"
                    DateTimeFormatter.ISO_LOCAL_DATE,           // "2017-11-23"
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),  // "23-11-2017"
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),  // "11/23/2017"
                    DateTimeFormatter.ofPattern("yyyy.MM.dd"),  // "2017.11.23"
                    DateTimeFormatter.ofPattern("dd.MM.yyyy"),  // "23.11.2017"
                    DateTimeFormatter.ofPattern("yyyy/MM/dd")   // "2017/11/23"
            );
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(strValue, formatter);
                } catch (DateTimeParseException e) {
                    try {
                        return LocalDate.parse(strValue, formatter).atStartOfDay();
                    } catch (DateTimeParseException e2) {
                        // Continue to next formatter
                    }
                }
            }
            // Try parsing as epoch timestamp (milliseconds or seconds)
            try {
                long timestamp = Long.parseLong(strValue);
                if (timestamp > 100000000000L) {  // Assume milliseconds if > 100000000000
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                } else {  // Assume seconds
                    return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                }
            } catch (NumberFormatException e) {
                // Not a number, continue
            }
            throw new ValidatorPolicyException("Invalid date/time format: " + strValue);
        } else if (value instanceof Number) {
            long timestamp = ((Number) value).longValue();
            if (timestamp > 100000000000L) {  // Assume milliseconds if > 100000000000
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            } else {  // Assume seconds
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
            }
        } else {
            throw new ValidatorPolicyException("Unsupported date/time format: " + value);
        }
    }

    /**
     * Parses an object to Double. The object can be a number or a string representation of a number.
     *
     * @param value The value to parse.
     * @return The parsed Double.
     * @throws NumberFormatException if the value cannot be parsed to a valid number.
     */
    private Double parseToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else {
            throw new NumberFormatException("Cannot parse to number: " + value);
        }
    }

    /**
     * Converts various types of input into an Object array.
     *
     * This method handles the following cases:
     * - Null input: Returns an empty array.
     * - Object array: Returns the input array as is.
     * - String: Splits the string by commas, trimming whitespace, and returns the resulting array.
     * - Any other object: Returns a single-element array containing the input object.
     *
     * @param value The input object to be converted into an array. Can be null, an array, a string, or any other object.
     * @return An Object array representation of the input value.
     *         If the input is null, returns an empty array.
     *         If the input is already an Object array, returns it unchanged.
     *         If the input is a String, splits it by commas and returns the resulting array.
     *         For any other input type, returns a single-element array containing the input object.
     * Example:
     *   toObjectArray(null)                 returns []
     *   toObjectArray(new Object[]{1, 2, 3}) returns [1, 2, 3]
     *   toObjectArray("a, b, c")            returns ["a", "b", "c"]
     *   toObjectArray("single")             returns ["single"]
     *   toObjectArray(42)                   returns [42]
     *   toObjectArray([{key1=value1}, {key2=value2}]) returns [{key1=value1}, {key2=value2}]
     */
    private Object[] toObjectArray(Object value) {
        if (value == null) {
            return new Object[0];
        }
        if (value instanceof Object[]) {
            return (Object[]) value;
        }
        if (value instanceof List) {
            return ((List<?>) value).toArray();
        }
        if (value instanceof String) {
            return ((String) value).split("\\s*,\\s*");
        }
        return new Object[]{value};
    }
}
