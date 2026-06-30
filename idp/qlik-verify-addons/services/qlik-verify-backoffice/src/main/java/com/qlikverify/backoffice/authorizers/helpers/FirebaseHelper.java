package com.qlikverify.backoffice.authorizers.helpers;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FirebaseHelper {

    static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private FirebaseHelper(){
        throw new UnsupportedOperationException("Firebase helper cannot be instantiated");
    }

    public static GoogleCredentials getCredentials(String data) throws IOException {
        JsonFactory jsonFactory = JSON_FACTORY;
        JsonObjectParser parser = new JsonObjectParser(jsonFactory);
        InputStream inputStream=new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        GenericJson fileContents =
                parser.parseAndClose(inputStream, StandardCharsets.UTF_8, GenericJson.class);
        String fileType = (String)fileContents.get("type");
        if (fileType == null) {
            throw new IOException("Error reading credentials from stream, 'type' field not specified.");
        } else if ("authorized_user".equals(fileType)) {
            return UserCredentials.fromStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        } else if ("service_account".equals(fileType)) {
            return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        } else if ("gdch_service_account".equals(fileType)) {
            return GdchCredentials.fromStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        } else if ("external_account".equals(fileType)) {
            return ExternalAccountCredentials.fromStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        } else if ("external_account_authorized_user".equals(fileType)) {
            return ExternalAccountAuthorizedUserCredentials.fromStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        } else if ("impersonated_service_account".equals(fileType)) {
            return ImpersonatedCredentials.fromStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        } else {
            throw new IOException(String.format("Error reading credentials from stream, 'type' value '%s' not recognized. Expecting '%s' or '%s'.", fileType, "authorized_user", "service_account"));
        }
    }
}
