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
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConfigPropertyRepresentation {
    protected String name;
    protected String label;
    protected String helpText;
    protected String type;
    protected Map<String, Object> dependsOn;
    protected Object defaultValue;
    protected List<String> options;
    protected List<Map> mapOptions;
    protected List<List<Map<String, String>>> groupOptions;
    protected List<List<Map<String, String>>> configDetails;
    protected boolean secret;
    private boolean readOnly;
    protected boolean isRequired;
    protected String validationType;
    protected String groupBy;
    protected String groupLabel;
    protected int maxVal;
    protected int minVal;
    protected String onDemand;
    protected List<ConfigPropertyRepresentation> configProperties;
    protected String downloadUrl;
    protected String helperLink;
    protected String helperLinkText;

    protected boolean isRawFormat;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(Map<String, Object> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<List<Map<String, String>>> getGroupOptions() {
        return groupOptions;
    }

    public void setGroupOptions(List<List<Map<String, String>>> groupOptions) {
        this.groupOptions = groupOptions;
    }

    public List<List<Map<String, String>>> getConfigDetails() {
        return configDetails;
    }

    public void setConfigDetails(List<List<Map<String, String>>> configDetails) {
        this.configDetails = configDetails;
    }

    public List<Map> getMapOptions() {
        return mapOptions;
    }

    public void setMapOptions(List<Map> mapOptions) {
        this.mapOptions = mapOptions;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
    }

    public int getMaxVal() {
        return maxVal;
    }

    public int getMinVal() {
        return minVal;
    }

    public void setMaxVal(int maxVal) {
        this.maxVal = maxVal;
    }

    public void setMinVal(int minVal) {
        this.minVal = minVal;
    }

    public String getOnDemand() {
        return onDemand;
    }

    public void setOnDemand(String onDemand) {
        this.onDemand = onDemand;
    }

    public String getHelperLink() {
        return helperLink;
    }

    public void setHelperLink(String helperLink) { 
        this.helperLink = helperLink; 
    }

    public String getHelperLinkText() {
        return helperLinkText;
    }

    public void setHelperLinkText(String helperLinkText) {
        this.helperLinkText = helperLinkText;
    }

    public List<ConfigPropertyRepresentation> getConfigProperties() {
        return configProperties;
    }

    public void setConfigProperties(List<ConfigPropertyRepresentation> configProperties) {
        this.configProperties = configProperties;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isRawFormat() {
        return isRawFormat;
    }

    public void setRawFormat(boolean rawFormat) {
        this.isRawFormat = rawFormat;
    }
}
