package io.ezto.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class JsonHelper {

    private static JsonHelper jsonHelper;
    private ObjectMapper objectMapper;

    private JsonHelper(){
        objectMapper = new ObjectMapper();
    }

    public static JsonHelper getInstance(){
        if(jsonHelper == null)
        {
            jsonHelper = new JsonHelper();
        }
        return jsonHelper;
    }

    public String getStringified(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JsonNode getNode(Object data)
    {
        return objectMapper.convertValue(data,JsonNode.class);
    }

    public Map<String,Object> getData(String node)
    {
        try {
            return objectMapper.readValue(node,new TypeReference<Map<String, Object>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    public ObjectMapper getObjectMapper(){
        return objectMapper;
    }
}
