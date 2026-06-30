package com.qlikverify.extensions.admin.controllers.workflows.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class WorkflowsEntityProvider implements JpaEntityProvider {
    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(WorkflowsEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/workflows-master-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return WorkflowsEntityProviderFactory.PROVIDER_FACTORY_ID;
    }

    @Override
    public void close() {
        //NOP
    }
}
