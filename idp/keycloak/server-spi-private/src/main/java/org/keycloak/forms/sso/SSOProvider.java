package org.keycloak.forms.sso;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

public interface SSOProvider extends Provider {

    SSOProvider setUriInfo(UriInfo uriInfo);

    SSOProvider setHttpHeaders(HttpHeaders httpHeaders);

    Response createResponse();

    SSOProvider setError(Response.Status status, String message, Object ... parameters);

    SSOProvider setErrors(Response.Status status, List<FormMessage> messages);

    SSOProvider setSuccess(String message, Object ... parameters);

    SSOProvider setWarning(String message, Object ... parameters);

    SSOProvider setUser(UserModel user);

    SSOProvider setRealm(RealmModel realm);

    SSOProvider setReferrer(String[] referrer);

    SSOProvider setStateChecker(String stateChecker);

    SSOProvider setAttribute(String key, String value);
}
