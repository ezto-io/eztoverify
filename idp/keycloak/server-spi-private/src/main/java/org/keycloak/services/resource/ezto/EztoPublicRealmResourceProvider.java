package org.keycloak.services.resource.ezto;

import org.keycloak.provider.Provider;

/**
 * <p>A {@link EztoPublicRealmResourceProvider} creates JAX-RS <emphasis>sub-resource</emphasis> instances for paths relative
 * to access public realm's RESTful API that could not be resolved by the server.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface EztoPublicRealmResourceProvider extends Provider {

    /**
     * <p>Returns a JAX-RS resource instance.
     *
     * @return a JAX-RS sub-resource instance
     */
    Object getResource();
}
