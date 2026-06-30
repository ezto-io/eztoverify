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

package org.keycloak.forms.sso.freemarker.models;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.IDToken;
import org.keycloak.services.Urls;
import org.keycloak.theme.Theme;

import java.net.URI;

public class UrlBean {

    private String realm;
    private Theme theme;
    private URI baseURI;
    private URI baseQueryURI;
    private URI currentURI;

    private String idToken;
    public UrlBean(RealmModel realm, Theme theme, URI baseURI, URI baseQueryURI, URI currentURI) {
        this.realm = realm.getName();
        this.theme = theme;
        this.baseURI = baseURI;
        this.baseQueryURI = baseQueryURI;
        this.currentURI = currentURI;
    }

    public UrlBean(RealmModel realm, Theme theme, URI baseURI, URI baseQueryURI, URI currentURI, String idToken) {
        this.realm = realm.getName();
        this.theme = theme;
        this.baseURI = baseURI;
        this.baseQueryURI = baseQueryURI;
        this.currentURI = currentURI;
        this.idToken = idToken;
    }

    public String getSsoUrl() {
        return Urls.ssoPage(baseQueryURI, realm).toString();
    }

    public String getResourcesPath() {
        URI uri = Urls.themeRoot(baseURI);
        return uri.getPath() + "/" + theme.getType().toString().toLowerCase() +"/" + theme.getName();
    }

    public String getLogoutUrl() {
        if (idToken != null){
            return Urls.ssoLogout(baseQueryURI,currentURI, realm, idToken).toString();
        }
        return Urls.ssoLogout(baseQueryURI,currentURI, realm).toString();
    }

}
