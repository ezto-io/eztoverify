/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.client;

import org.keycloak.models.*;
import org.keycloak.models.RoleContainerModel.RoleRemovedEvent;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author hmlnarik
 */
public class MapClientProviderFactory extends AbstractMapProviderFactory<ClientProvider, MapClientEntity, ClientModel> implements ClientProviderFactory, ProviderEventListener {

    private final ConcurrentHashMap<String, ConcurrentMap<String, Long>> REGISTERED_NODES_STORE = new ConcurrentHashMap<>();

    private Runnable onClose;

    public MapClientProviderFactory() {
        super(ClientModel.class);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this);
        onClose = () -> factory.unregister(this);
    }

    @Override
    public MapClientProvider create(KeycloakSession session) {
        return new MapClientProvider(session, getStorage(session), REGISTERED_NODES_STORE);
    }

    @Override
    public void close() {
        super.close();
        onClose.run();
    }

    @Override
    public String getHelpText() {
        return "Client provider";
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof RoleContainerModel.RoleRemovedEvent) {
            RoleRemovedEvent e = (RoleContainerModel.RoleRemovedEvent) event;
            RoleModel role = e.getRole();
            RoleContainerModel container = role.getContainer();
            RealmModel realm;
            if (container instanceof RealmModel) {
                realm = (RealmModel) container;
            } else if (container instanceof ClientModel) {
                realm = ((ClientModel) container).getRealm();
            } else {
                return;
            }
            ((MapClientProvider) e.getKeycloakSession().getProvider(ClientProvider.class)).preRemove(realm, role);
        }
    }
}
