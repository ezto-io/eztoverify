
package com.qlikverify.extensions.admin.controllers.hook.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class HookJpaEntityProvider implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return Collections.<Class<?>>singletonList(Hook.class);
    }

    @Override
    public String getChangelogLocation() {
    	return "META-INF/hook-changelog.xml";
    }
    
    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getFactoryId() {
        return HookJpaEntityProviderFactory.ID;
    }
}
