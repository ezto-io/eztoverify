package org.keycloak.policy.ciba;

import com.fasterxml.jackson.databind.node.DoubleNode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.UsernamePolicy;
import org.keycloak.policy.username.UsernamePolicyConfigException;
import org.keycloak.policy.username.UsernamePolicyProvider;

import java.io.Serializable;
import java.util.*;

/**
 * @QlikVerify
 */
public class CibaPolicy implements Serializable {

    static List<String> stringconditions = new ArrayList<String>() {{
        add("equals");
        add("not equal");
        add("contains");
        add("startswith");
        add("endswith");
    }};

    static List<String> intconditions = new ArrayList<String>() {{
        add("equals");
        add("not equal");
        add("greater than");
        add("greater than equal");
        add("lower than");
        add("lower than equal");
    }};

    static List<String> booleanconditions = new ArrayList<String>() {{
        add("equals");
        add("not equal");
    }};

    static List<String> types = new ArrayList<String>() {{
        add("string");
        add("int");
        add("boolean");
        add("double");
    }};

    public static List<String> getStringConditions() {
        return stringconditions;
    }

    public static List<String> getBooleanConditions() {
        return booleanconditions;
    }

    /**
     * Returns Int conditions, same for double
     *
     * @return
     */
    public static List<String> getIntconditions() {
        return intconditions;
    }

    public static List<String> getTypes() {
        return types;
    }

    public static Map<String, List<String>> getComparators() {
        Map<String, List<String>> comparators = new HashMap<>();
        comparators.put("string", getStringConditions());
        comparators.put("int", getIntconditions());
        comparators.put("boolean", getBooleanConditions());
        comparators.put("double", getIntconditions());
        return comparators;
    }

    private String propKey;
    private String propType;
    private String propComparator;
    private Object propValue;

    public String getPropKey() {
        return propKey;
    }

    public void setPropKey(String propKey) {
        this.propKey = propKey;
    }

    public String getPropType() {
        return propType;
    }

    public void setPropType(String propType) {
        this.propType = propType;
    }

    public String getPropComparator() {
        return propComparator;
    }

    public void setPropComparator(String propComparator) {
        this.propComparator = propComparator;
    }

    public Object getPropValue() {
        return propValue;
    }

    public void setPropValue(Object propValue) {
        this.propValue = propValue;
    }

    private static String parseSubString(String polSubString)
    {
        if(polSubString.startsWith("@"))
            return polSubString.substring(1,polSubString.length());

        if(polSubString.endsWith("@"))
            return polSubString.substring(0,polSubString.length() - 1);

        return polSubString;
    }

    // (propKey) (propType) (propComparator) (propValue)
    public static CibaPolicy parseCibaPolicy(String policy) {

        CibaPolicy cibaPolicy = new CibaPolicy();
        String modifiedPolicy = policy.replace("(","@").replace(")","@");
        String[] propSplit = modifiedPolicy.split("@ @");

        String prKey = parseSubString(propSplit[0]);

        if(prKey.equalsIgnoreCase("decision"))
        {
            cibaPolicy.setPropKey(parseSubString(propSplit[0]));
            cibaPolicy.setPropValue(parseSubString(propSplit[1]));
            return cibaPolicy;
        }

        if (propSplit.length != 4)
            throw new CibaPolicyParseException("Invalid plicy. It didn't match the order of execution");

        cibaPolicy.setPropKey(prKey);
        cibaPolicy.setPropType(parseSubString(propSplit[1]));
        cibaPolicy.setPropComparator(parseSubString(propSplit[2]));
        cibaPolicy.setPropValue(parseSubString(propSplit[3]));

        return cibaPolicy;
    }

    public static CibaPolicyCollection parse(String policyString) {
        CibaPolicyCollection cibaPolicyCollection = new CibaPolicyCollection();
        List<CibaPolicy> policies = new ArrayList<>();

        if (policyString != null && !policyString.trim().isEmpty()) {
            for (String policy : policyString.split(" and ")) {
                CibaPolicy policy1 = parseCibaPolicy(policy);
                if(policy1.getPropKey().equalsIgnoreCase("decision"))
                    cibaPolicyCollection.setDecision(policy1.getPropValue() == null ? "Unanimous" : String.valueOf(policy1.getPropValue()));
                else {
                    policies.add(parseCibaPolicy(policy));
                }
            }
        }
        cibaPolicyCollection.setPolicyList(policies);
        return cibaPolicyCollection;
    }

    public static String convert(List<CibaPolicy> cibaPolicy) {
        boolean first = true;

        StringBuilder stringBuilder = new StringBuilder();
        for (CibaPolicy c :
                cibaPolicy) {
            if (first) {
                first = false;
            }
            {
                stringBuilder.append(" and ");
            }
            stringBuilder.append("(" + c.getPropKey() + ") ");
            stringBuilder.append("(" + c.getPropType() + ") ");
            stringBuilder.append("(" + c.getPropComparator() + ") ");
            stringBuilder.append("(" + c.getPropValue() + ")");

        }
        return stringBuilder.toString();
    }

    private static String extractContent(String policy) {
        int i = policy.trim().indexOf(')');
        return policy.substring(1, i - 1).trim();
    }

    public boolean evaluate(Object keyValue, CibaPolicy cibaPolicy) {

        switch (cibaPolicy.getPropType()) {
            case "string":
                return validateString(String.valueOf(keyValue), String.valueOf(cibaPolicy.getPropValue()), cibaPolicy.getPropComparator());
            case "boolean":
                return validateBoolean(Boolean.parseBoolean(String.valueOf(keyValue)), Boolean.parseBoolean(String.valueOf(cibaPolicy.getPropValue())), cibaPolicy.getPropComparator());
            case "int":
                return validateInt(Integer.parseInt(String.valueOf(keyValue)), Integer.parseInt(String.valueOf(cibaPolicy.getPropValue())), cibaPolicy.getPropComparator());
            case "double":
                return validateDouble(Double.parseDouble(String.valueOf(keyValue)), Double.parseDouble(String.valueOf(cibaPolicy.getPropValue())), cibaPolicy.getPropComparator());

        }

        throw new CibaPolicyValidationException("Invalid object type");
    }

    private boolean validateDouble(double leftValue, double rightValue, String comparator) {
        switch (comparator) {
            case "equals":
                return leftValue == rightValue;
            case "not equal":
                return leftValue != rightValue;
            case "greater than":
                return leftValue > rightValue;
            case "greater than equal":
                return leftValue >= rightValue;
            case "lower than":
                return leftValue < rightValue;
            case "lower than equal":
                return leftValue <= rightValue;
        }

        throw new CibaPolicyValidationException("Invalid comparator");
    }


    private boolean validateInt(int leftValue, int rightValue, String comparator) {
        switch (comparator) {
            case "equals":
                return leftValue == rightValue;
            case "not equal":
                return leftValue != rightValue;
            case "greater than":
                return leftValue > rightValue;
            case "greater than equal":
                return leftValue >= rightValue;
            case "lower than":
                return leftValue < rightValue;
            case "lower than equal":
                return leftValue <= rightValue;
        }

        throw new CibaPolicyValidationException("Invalid comparator");
    }

    private boolean validateString(String leftValue, String rightValue, String comparator) {
        switch (comparator) {
            case "equals":
                return leftValue.equals(rightValue);
            case "not equal":
                return !leftValue.equals(rightValue);
            case "contains":
                return leftValue.contains(rightValue);
            case "startswith":
                return leftValue.startsWith(rightValue);
            case "endswith":
                return leftValue.endsWith(rightValue);
        }

        throw new CibaPolicyValidationException("Invalid comparator");
    }

    private boolean validateBoolean(boolean leftValue, boolean rightValue, String comparator) {
        switch (comparator) {
            case "equals":
                return leftValue == rightValue;
            case "not equal":
                return leftValue != rightValue;
        }

        throw new CibaPolicyValidationException("Invalid comparator");
    }

}
