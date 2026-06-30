/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.services;

import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.SuccessRepresentation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public class SuccessResponse {

    public static Response success(String message, Response.Status status) {
        return SuccessResponse.success(message, null, status);
    }
    
    public static Response success(String message, Object[] params, Response.Status status) {
        SuccessRepresentation sucess = new SuccessRepresentation();
        sucess.setMessage(message);
        sucess.setParams(params);
        return Response.status(status).entity(sucess).type(MediaType.APPLICATION_JSON).build();
    }

}
