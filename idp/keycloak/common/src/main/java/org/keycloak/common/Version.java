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

package org.keycloak.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Version {
    public static final String UNKNOWN = "UNKNOWN";
    public static boolean IS_MULTITENANT;
    public static int USER_COUNT;
    public static int APP_COUNT;
    public static int TENANT_COUNT;
    public static List<String> DISABLED_FEATURES;
    public static List<String> WHITELIST_MARKETPLACE;
    public static List<String> MODE;
    public static String OFFERING;
    public static String NAME;
    public static String NAME_FULL;
    public static String NAME_HTML;
    public static String DISPLAYNAME;
    public static String DISPLAYNAME_FULL;
    public static String DISPLAYNAME_HTML;
    public static String VERSION;
    public static String QV_VERSION;
    public static String VERSION_KEYCLOAK;
    public static String RESOURCES_VERSION;
    public static String QV_RESOURCES_VERSION;
    public static String BUILD_TIME;
    public static String DEFAULT_PROFILE;

    static {
        try (InputStream is = Version.class.getResourceAsStream("/keycloak-version.properties")) {
            Properties props = new Properties();
            props.load(is);
            String mode = props.getProperty("mode");
            Version.MODE = mode == null || mode.isEmpty() ? new ArrayList<String>() : Arrays.asList(mode.split(","));;
            Version.OFFERING = props.getProperty("offering");
            String disFeaProp = props.getProperty("features-disable");
            Version.DISABLED_FEATURES = disFeaProp == null || disFeaProp.isEmpty() ? new ArrayList<String>() : Arrays.asList(disFeaProp.split(","));
            String wMarketplaceProp = props.getProperty("marketplace-whitelist");
            Version.WHITELIST_MARKETPLACE = wMarketplaceProp == null || wMarketplaceProp.isEmpty() ? new ArrayList<String>() : Arrays.asList(wMarketplaceProp.split(","));
            Version.IS_MULTITENANT = props.getProperty("multitenant") == null ? true : Boolean.parseBoolean(props.getProperty("multitenant"));
            Version.USER_COUNT = props.getProperty("user-count") == null ? -1 : Integer.parseInt(props.getProperty("user-count"));
            Version.APP_COUNT = props.getProperty("app-count") == null ? -1 : Integer.parseInt(props.getProperty("app-count"));
            Version.TENANT_COUNT = props.getProperty("tenant-count") == null ? -1 : Integer.parseInt(props.getProperty("tenant-count"));
            Version.NAME = props.getProperty("name");
            Version.NAME_FULL = props.getProperty("name-full");
            Version.NAME_HTML = props.getProperty("name-html");
            Version.DISPLAYNAME = props.getProperty("displayname");
            Version.DISPLAYNAME_FULL = props.getProperty("displayname-full");
            Version.DISPLAYNAME_HTML = props.getProperty("displayname-html");
            Version.DEFAULT_PROFILE = props.getProperty("default-profile");
            Version.VERSION = props.getProperty("version");
            Version.QV_VERSION = props.getProperty("qv-version");
            Version.VERSION_KEYCLOAK = props.getProperty("version-keycloak");
            Version.BUILD_TIME = props.getProperty("build-time");
            Version.RESOURCES_VERSION = Version.VERSION.toLowerCase();
            Version.QV_RESOURCES_VERSION = Version.QV_VERSION.toLowerCase();

            if (Version.RESOURCES_VERSION.endsWith("-snapshot")) {
                Version.RESOURCES_VERSION = Version.RESOURCES_VERSION.replace("-snapshot", "-" + Version.BUILD_TIME.replace(" ", "").replace(":", "").replace("-", ""));
            }
        } catch (IOException e) {
            Version.VERSION = Version.UNKNOWN;
            Version.BUILD_TIME = Version.UNKNOWN;
        }
    }

}
