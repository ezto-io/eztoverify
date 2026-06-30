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

package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class AuthenticatorConfigInfoRepresentation {

    protected String name;
    protected String providerId;
    protected String helpText;
    protected boolean hasTestConfig;
    protected boolean isTemplateSupported;
    protected Map<String,Object> constraints;

    protected List<ConfigPropertyRepresentation> properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHelpText() {
        return helpText;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public boolean isHasTestConfig() {
        return hasTestConfig;
    }

    public void setHasTestConfig(boolean hasTestConfig) {
        this.hasTestConfig = hasTestConfig;
    }

    public List<ConfigPropertyRepresentation> getProperties() {
        return properties;
    }

    public void setProperties(List<ConfigPropertyRepresentation> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }

    public boolean isTemplateSupported() {
        return isTemplateSupported;
    }

    public void setTemplateSupported(boolean templateSupported) {
        isTemplateSupported = templateSupported;
    }
}

