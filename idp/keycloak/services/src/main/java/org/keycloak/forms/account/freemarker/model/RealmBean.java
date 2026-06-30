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
package org.keycloak.forms.account.freemarker.model;

import org.keycloak.avatar.MinioAvatarStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 */
public class RealmBean {

    private RealmModel realm;
    private KeycloakSession session;

    public RealmBean(KeycloakSession session,RealmModel realmModel) {
        this.session = session;
        realm = realmModel;
    }

    public String getName() {
        return realm.getName();
    }

    public String getDisplayName() {
        String displayName = realm.getDisplayName();
        if (displayName != null && displayName.length() > 0) {
            return displayName;
        } else {
            return getName();
        }
    }

    public String getDisplayNameHtml() {
        String displayNameHtml = realm.getDisplayNameHtml();
        if (displayNameHtml != null && displayNameHtml.length() > 0) {
            return displayNameHtml;
        } else {
            return getDisplayName();
        }
    }

    public String getRealmLogo(){
        String realmLogo = realm.getRealmLogo();
        if(realmLogo != null && realmLogo.length() > 0)
            return ModelToRepresentation.getLogoAbsURL(realmLogo);
        else
            return "";
    }

    public String getDefaultRegion(){
        String defaultRegion = realm.getDefaultRegion();
        if(defaultRegion != null && defaultRegion.length() > 0){
            return defaultRegion;
        } else {
            return "in";
        }
    }

    public String getMode(){
        String mode = realm.getMode();
        if(mode != null) {
            return mode;
        }
        return "bo";
    }

    public boolean isInternationalizationEnabled() {
        return realm.isInternationalizationEnabled();
    }

    public Set<String> getSupportedLocales(){
        return realm.getSupportedLocalesStream().collect(Collectors.toSet());
    }

    public boolean isEditUsernameAllowed() {
        return realm.isEditUsernameAllowed();
    }

    public boolean isRegistrationEmailAsUsername() {
        return realm.isRegistrationEmailAsUsername();
    }

    public boolean isUserManagedAccessAllowed() {
        return realm.isUserManagedAccessAllowed();
    }
}
