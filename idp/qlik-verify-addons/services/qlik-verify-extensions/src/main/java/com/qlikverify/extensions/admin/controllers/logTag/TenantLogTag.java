package com.qlikverify.extensions.admin.controllers.logTag;

import org.keycloak.utils.StringUtil;
import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class TenantLogTag implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getUriInfo().getPathParameters().containsKey("realm")
                && !StringUtil.isBlank(requestContext.getUriInfo().getPathParameters().getFirst("realm"))) {
            String realm = requestContext.getUriInfo().getPathParameters().getFirst("realm");
            MDC.put("workspace-name", "["+realm+"]");

            requestContext.getHeaders().add("X-WORKSPACE-NAME", realm);
        } else {
            MDC.put("workspace-name", "[-]");
        }
    }
}
