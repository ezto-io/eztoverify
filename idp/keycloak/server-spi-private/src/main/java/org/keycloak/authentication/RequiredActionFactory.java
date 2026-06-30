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

package org.keycloak.authentication;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.models.Constants.DEFAULT_SERVER_URL;
import static org.keycloak.models.Constants.TEMPLATE_PATH;

/**
 * You must specify a file
 * META-INF/services/org.keycloak.authentication.RequiredActionFactory in the jar that this class is contained in
 * This file must have the fully qualified class name of all your RequiredActionFactory classes
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionFactory extends ProviderFactory<RequiredActionProvider> {

    /**
     * Display text used in admin console to reference this required action
     *
     * @return
     */
    String getDisplayText();

    /**
     * Flag indicating whether the execution of the required action by the same circumstances
     * (e.g. by one and the same action token) should only be permitted once.
     *
     * @return
     */
    default boolean isOneTimeAction() {
        return false;
    }

    default boolean isPolicyRequired() {
        return false;
    }

    RequiredActionEnum requiredActionType();

    List<ProviderConfigProperty> getConfigProperties(KeycloakSession session);

    boolean isConfigurable();

    default boolean isValidator() {
        return false;
    }

    default List<ProviderConfigProperty> getConfigProperties(KeycloakSession session, String actionId, String stepId) {
        return Collections.emptyList();
    }

    default List<ProviderConfigProperty> getOnDemandConfig(KeycloakSession session, String actionId, String stepId, HashMap<String, Object> config) {
        return Collections.emptyList();
    }

    String[] supportedModes();

    default boolean hasPolicy() {
        return true;
    }

    default String[] supportedRegions() {
        return new String[]{"all"};
    }

    default int isTransactionSuccess(RealmModel realm, String trxId, String mappingId) {
        String counter = realm.getTrxValue(trxId, "ik@" + mappingId + "_counter");

        if (counter == null)
            return 0;
        return Integer.parseInt(counter);
    }

    default void incrementModuleCounter(String trxId, RealmModel realmModel, UserModel userModel, String mappingId) {
        int counter = 0;
        String counterObj = userModel.getFirstAttribute("ik@" + mappingId + "_counter");
        if (counterObj != null)
            counter = Integer.parseInt(counterObj);

        counter += 1;
        realmModel.addAttribute(trxId, "ik@" + mappingId + "_counter", String.valueOf(counter));
    }

    default String[] stripeMappingId() {
        return null;
    }

    default String[] getApiMappingName() {
        return null;
    }

    default String[] getApiMappingPrice() {
        return null;
    }

    default Map<String, Object> getMetadata() {
        return null;
    }

    default String gifUrl(String locale) {
        return DEFAULT_SERVER_URL + TEMPLATE_PATH + locale + "/" + getId() + "/" + getId() + ".gif";
    }

    default boolean isBeta() {
        return false;
    }

    default boolean isNew() {
        return false;
    }

    default boolean shouldIncludeBaseTransactionPrice(String stripeId) {
        return true;
    }

    default String getDescription() {
        return this.getId() + "-desc";
    }

    default void onUpdate(RequiredActionProviderRepresentation rep) {
    }

    default Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        return Collections.emptyMap();
    }

    default boolean skippable() {
        return true;
    }

    default boolean isClaimsCustomizable() {
        return true;
    }

    default boolean isGifRequired() {
        return true;
    }
}
