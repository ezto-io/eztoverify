package com.qlikverify.extensions.admin.controllers.ssodashboard.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class SSODashboardEntityProvider implements JpaEntityProvider {

    //~ Methods ============================================================================================================================

    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(SSODashboardEntity.class);
    }

    //~ ====================================================================================================================================

    @Override
    public String getChangelogLocation() {
        return "META-INF/sso-dashboard-changelog.xml";
    }

    //~ ====================================================================================================================================

    @Override
    public String getFactoryId() {
        return SSODashboardEntityProviderFactory.PROVIDER_FACTORY_ID;
    }

    //~ ====================================================================================================================================

    @Override
    public void close() {
        // No implementation required
    }
}
