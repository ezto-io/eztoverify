package com.qlikverify.backoffice.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.ArrayList;
import java.util.List;

public class EztoVerifyEntityProvider implements JpaEntityProvider {

    private List<Class<?>> entityClasses = new ArrayList<>();


    @Override
    public List<Class<?>> getEntities() {
        entityClasses.add(ApplicationDetailsEntity.class);
        entityClasses.add(DeepLinkEntity.class);
        return entityClasses;
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/eztoverify-master-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return EztoVerifyEntityProviderFactory.ID;
    }

    @Override
    public void close() {
        // NOP
    }
}
