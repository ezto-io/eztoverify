package com.qlikverify.exceptions;

import com.qlikverify.constants.APIErrorCodes;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class APIException extends Exception {
    private String errorCode;
    private String errorDescription;
    private int statusCode;
    private String apiVersion;

    public APIException(int statusCode, String errorCode, String errorDescription,String apiVersion) {
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.apiVersion = apiVersion;
    }

    /**
     * Returns the HttpResponse
     * @return
     */
    public Response getResponse() {
        OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(errorCode, errorDescription);
        return Response.status(statusCode).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
