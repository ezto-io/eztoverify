package com.qlikverify.extensions.admin.misc.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public class JsonUtil {

    //~ Static fields/initializers =========================================================================================================

    private static final String LOG_TAG = "JSON:UTIL: ";

    //~ Constructor =======================================================================================================================

    protected JsonUtil() {}

    //~ Methods ============================================================================================================================

    /**
     * Retrieves the singleton instance of the FeaturesModuleGeneralHelper class.
     *
     * @return The singleton instance of the FeaturesModuleGeneralHelper.
     */
    public static ObjectMapper getObjectMapperInstance() {
        return JsonSerialization.mapper;
    }

    //~ ===================================================================================================================================

    /**
     * Converts a generic class object into JSON representation
     *
     * @param requestData The data to be converted to JSON.
     * @param <T>         The type of the data.
     * @return The JSON representation of the provided data as a string.
     * @throws RuntimeException If there is an issue with the JSON conversion or an invalid request body.
     */
    public static <T> String getAsJsonString(final T requestData) {
        String jsonInput;
        try {
            jsonInput = JsonSerialization.writeValueAsString(requestData);
        } catch (IOException e) {
            throw new RuntimeException(LOG_TAG + "Exception:: On getting json string");
        }
        return jsonInput;
    }

    //~ ===================================================================================================================================

    /**
     * Converts a generic class object into JSON representation as pretty string to print into the logs
     *
     * @param requestData The data to be converted to JSON.
     * @param <T>         The type of the data.
     * @return The JSON representation of the provided data as a string.
     * @throws RuntimeException If there is an issue with the JSON conversion or an invalid request body.
     */
    public static <T> String getAsJsonPrettyString(final T requestData) {
        String jsonInput;
        try {
            jsonInput = JsonSerialization.writeValueAsPrettyString(requestData);
        } catch (IOException e) {
            throw new RuntimeException(LOG_TAG + "Exception:: On getting json string");
        }
        return jsonInput;
    }
}
