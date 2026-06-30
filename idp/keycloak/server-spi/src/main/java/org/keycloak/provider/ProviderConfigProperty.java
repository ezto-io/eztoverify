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

package org.keycloak.provider;

import org.keycloak.models.KeycloakSession;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuration property metadata.  Used to render generic configuration pages for Keycloak extensions in the admin console.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProviderConfigProperty {
    public static final String BOOLEAN_TYPE = "boolean";
    public static final String STRING_TYPE = "String";

    /**
     * Possibility to configure multiple String values of any value (something like "redirect_uris" for clients)
     */
    public static final String MULTIVALUED_STRING_TYPE = "MultivaluedString";
    public static final String MULTIVALUE_GROUP_SELECT = "MultiValuedGroupSelect";
    public static final String MULTIVALUE_OBJECT_LIST = "MultivaluedObjectList";
    public static final String MULTIVALUE_CLAIM_LIST = "MultivaluedClaimList";

    public static final String SCRIPT_TYPE = "Script";
    public static final String FILE_TYPE = "File";
    public static final String FONT_FILE_TYPE = "FontFile";
    public static final String ROLE_TYPE = "Role";
    public static final String GROUP_TYPE = "Group";
    public static final String CONFIG_TYPE = "Config";
    public static final String DOWNLOAD_TYPE = "Download";

    /**
     * This Config Type is useful for handling zero and negative values in Expiry Time.
     */
    public static final String EXPIRY_TIME_TYPE = "ExpiryTime";

    public static final String NUMBER_TYPE = "Number";
    public static final String ALPHANUMERIC_TYPE = "AlphaNumeric";

    /**
     * Possibility to configure single String value, which needs to be chosen from the list of predefined values (HTML select)
     */
    public static final String LIST_TYPE = "List";
    public static final String TIME = "time";

    /**
     * Possibility to configure multiple String values, which needs to be chosen from the list of predefined values (HTML select with multiple)
     */
    public static final String MULTIVALUED_LIST_TYPE = "MultivaluedList";
    public static final String CLIENT_LIST_TYPE = "ClientList";
    public static final String POLICY_LIST_TYPE = "PoliciesList";
    public static final String PASSWORD = "Password";
    public static final String SECRET = "Secret";
    public static final String EMAIL = "Email";
    public static final String URL = "Url";
    public static final String FROM_MOBILE_NUMBER = "from_mobile_number";
    public static final String IMAGE_FILE = "ImageFile";
    public static final String TEXT_EDITOR = "TextEditor";
    public static final String VERSION = "Version";
    public static final String PDF_FILE = "PDFFile";

    /**
     * Color field
     */
    public static final String COLOR_TYPE = "Color";
    /**
     * Color field
     */
    public static final String DECIMAL_TYPE = "Decimal";

    /**
     * Image field
     */
    public static final String IMAGE_TYPE = "Image";

    /**
     * textarea field
     */
    public static final String TEXT_TYPE = "Text";
    public static final String PHONE_REGION = "phoneregion";

    /**
     * InputGroup Field
     */
    public static final String INPUT_GROUP = "InputGroup";

    /**
     * Configure multiple (key, value) pairs
     */
    public static final String MAP_TYPE = "Map";
    public static final String MAP_LIST_TYPE = "MapList";
    public static final String MAP_LIST_EXCEPT_CURRENT_CONFIG_TYPE = "MapListExceptCurrentConfig";
    public static final String NOTIFICATION_CODE = "Notification_Code";
    public static final String NOTIFICATION_WARN = "Notification_Warn";
    public static final String NOTIFICATION_SUCCESS = "Notification_Success";
    public static final String NOTIFICATION_DANGER = "Notification_Danger";

    public static final String VALIDATION_ALL_TRUE = "all_true";
    public static final String VALIDATION_ANY_ONE_TRUE = "anyone_true";
    public static final String TEST_MOBILE_NUMBER = "mobile_number";
    public static final String IMAGE_UPLOAD = "image_upload";
    public static final String EMAIL_TABLE = "email_table";
    public static final String MULTI_SELECT = "multiselect";
    public static final String CAUTION = "caution";

    protected String name;
    protected String label;
    protected String helpText;
    protected String type = STRING_TYPE;
    protected Object defaultValue;
    protected List<String> options;
    protected List<List<Map<String, String>>> groupOptions;
    protected List<List<Map<String, String>>> configDetails;
    protected List<Map> mapOptions;
    protected List<ProviderConfigProperty> configProperties;
    protected boolean secret;
    protected boolean isRequired;
    protected int maxVal;
    protected int minVal;
    protected Map<String, Object> dependsOn;
    protected String validationType;
    protected String groupBy;
    protected String groupLabel;
    private boolean readOnly;
    private boolean skippableOnUpdate;
    private String onDemand;
    private String downloadUrl;
    private String helperLinkText;
    private String helperLink;
    private String onDemandComponent;
    private boolean isRawFormat;


    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    private String logo;

    public ProviderConfigProperty() {
    }

    public ProviderConfigProperty(ProviderConfigProperty property) {
        this.name = property.getName();
        this.label = property.getLabel();
        this.helpText = property.getHelpText();
        this.type = property.getType();
        this.defaultValue = property.getDefaultValue();
        this.options = property.getOptions();
        this.mapOptions = property.getMapOptions();
        this.configProperties = property.getConfigProperties();
        this.secret = property.isSecret();
        this.isRequired = property.isRequired();
        this.maxVal = property.getMaxVal();
        this.minVal = property.getMinVal();
        this.dependsOn = property.getDependsOn();
        this.validationType = property.getValidationType();
        this.groupBy = property.getGroupBy();
        this.groupLabel = property.getGroupLabel();
        this.readOnly = property.isReadOnly();
        this.skippableOnUpdate = property.isSkippableOnUpdate();
        this.onDemand = property.getOnDemand();
        this.downloadUrl = property.getDownloadUrl();
        this.helperLinkText = property.getHelperLinkText();
        this.helperLink = property.getHelperLink();
        this.onDemandComponent = property.getOnDemandComponent();
        this.logo = property.getLogo();
        this.isRawFormat = property.isRawFormat();
    }

    public ProviderConfigProperty(String name, String label, String helpText, String type, Object defaultValue) {
        this.name = name;
        this.label = label;
        this.helpText = helpText;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public ProviderConfigProperty(String name, String label, String helpText, String type, Object defaultValue,
                                  String logo) {
        this.name = name;
        this.label = label;
        this.helpText = helpText;
        this.type = type;
        this.defaultValue = defaultValue;
        this.logo = logo;
    }

    public ProviderConfigProperty(String name, String label, String helpText, String type, Object defaultValue, String... options) {
        this.name = name;
        this.label = label;
        this.helpText = helpText;
        this.type = type;
        this.defaultValue = defaultValue;
        this.options = Arrays.asList(options);
    }

    public ProviderConfigProperty(String name, String label, String helpText, String type, Object defaultValue, boolean secret) {
        this(name, label, helpText, type, defaultValue);
        this.secret = secret;
    }

    /**
     * Name of the config variable stored in the database
     *
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Label shown in the admin console when configuring the variable
     *
     * @return
     */
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Type of the variable.  i.e. boolean, string etc.  See the constants declared in this class for what your choices
     * are.
     *
     * @return
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Default value for the variable
     *
     * @return
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * For list types, this is a list of choices to choose from.
     *
     * @return
     */
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

    /**
     * Help text that will be displayed in the admin console tooltip
     *
     * @return
     */
    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    /**
     * If true, this variable is only writeable.  It will never be viewable.  This is important for things like
     * passwords in which you never want to display them on the screen.
     *
     * @return
     */
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

    public Map<String, Object> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(Map<String, Object> dependsOn) {
        this.dependsOn = dependsOn;
        this.validationType = VALIDATION_ALL_TRUE;
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

    public void setMaxVal(int val) {
        this.maxVal = val;
    }

    public void setMinVal(int val) {
        this.minVal = val;
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    public void setConfigProperties(List<ProviderConfigProperty> configProperties) {
        this.configProperties = configProperties;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isSkippableOnUpdate() {
        return skippableOnUpdate;
    }

    public void setSkippableOnUpdate(boolean skippableOnUpdate) {
        this.skippableOnUpdate = skippableOnUpdate;
    }

    /**
     * This applies only for providerconfigtype list, here set url will be called ondemand from frontend to get the dynamic providerConfigProperty
     *
     * @return
     */
    public String getOnDemand() {
        return onDemand;
    }

    public void setOnDemand(String onDemand) {
        this.onDemand = onDemand;
    }

    public List<Map> getMapOptions() {
        return mapOptions;
    }

    public void setMapOptions(List<Map> mapOptions) {
        this.mapOptions = mapOptions;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
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

    public void setOnDemandComponent(String onDemandComponent) {
        this.onDemandComponent = onDemandComponent;
    }

    public String getOnDemandComponent() {
        return onDemandComponent;
    }

    public boolean isRawFormat() {
        return isRawFormat;
    }

    public void setRawFormat(boolean rawFormat) {
        isRawFormat = rawFormat;
    }
}
