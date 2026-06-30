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

package org.keycloak.models;

import org.keycloak.EnvironmentReader;
import org.keycloak.OAuth2Constants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.EventType;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public final class Constants {
    public static final String ADMIN_CONSOLE_CLIENT_ID = "dashboard";
    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";

    public static final String ACCOUNT_MANAGEMENT_CLIENT_ID = "profile";
    public static final String SSO_MANAGEMENT_CLIENT_ID = "sso";
    public static final String SSO_DESKTOP_CLIENT_ID = "sso-desktop";
    public static final String ACCOUNT_CONSOLE_CLIENT_ID = "account-console";
    public static final String BROKER_SERVICE_CLIENT_ID = "broker";
    public static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management";
    public static final String CREATE_REALM_CLIENT_ID = "create-realm-app";
    public static final String REALM_AS_CLIENT = "-realm";

    // Info page
    public static final String MESSAGE_HEADER = "messageHeader";


    public static final String ADMIN_CONSOLE_CLIENT_NAME = "${client_security-admin-console}";
    public static final String ADMIN_CLI_CLIENT_NAME = "${client_admin-cli}";
    public static final String ACCOUNT_MANAGEMENT_CLIENT_NAME = "${client_account}";
    public static final String ACCOUNT_CONSOLE_CLIENT_NAME = "${client_account-console}";
    public static final String BROKER_SERVICE_CLIENT_NAME = "${client_broker}";
    public static final String REALM_MANAGEMENT_CLIENT_NAME = "${client_realm-management}";
    public static final String CREATE_REALM_CLIENT_NAME = "${client_create-realm-app}";

    public static final String AUTH_BASE_URL_PROP = "${authBaseUrl}";
    public static final String AUTH_ADMIN_URL_PROP = "${authAdminUrl}";

    public static final Collection<String> inBuildClients = Arrays.asList(ADMIN_CONSOLE_CLIENT_ID,ACCOUNT_MANAGEMENT_CLIENT_ID,SSO_MANAGEMENT_CLIENT_ID,SSO_DESKTOP_CLIENT_ID);

    public static final Collection<String> defaultClients = Arrays.asList(CREATE_REALM_CLIENT_ID,ACCOUNT_MANAGEMENT_CLIENT_ID, ADMIN_CLI_CLIENT_ID, BROKER_SERVICE_CLIENT_ID, REALM_MANAGEMENT_CLIENT_ID, ADMIN_CONSOLE_CLIENT_ID, ACCOUNT_CONSOLE_CLIENT_ID,SSO_MANAGEMENT_CLIENT_ID);
    public static final Collection<String> defaultClientNames= Arrays.asList(CREATE_REALM_CLIENT_NAME,ADMIN_CLI_CLIENT_NAME,ACCOUNT_MANAGEMENT_CLIENT_NAME,ADMIN_CONSOLE_CLIENT_NAME,REALM_MANAGEMENT_CLIENT_NAME,BROKER_SERVICE_CLIENT_NAME,ACCOUNT_CONSOLE_CLIENT_NAME);

    public static final String LOGIN_EVT = EventType.LOGIN.name();
    public static final String LOGIN_ERROR_EVT = EventType.LOGIN_ERROR.name();
    public static final String REGISTER_EVT = EventType.REGISTER.name();
    public static final String CREATE_REALM_EVT = "CREATE_REALM";
    public static final String CREATE_USER_EVT = "CREATE_USER";
    public static final String CREATE_ADMIN_USER_EVT = "CREATE_ADMIN_USER";

    public static final Collection<String> defaultEvts = Arrays.asList(LOGIN_EVT,LOGIN_ERROR_EVT,REGISTER_EVT);
    public static final Collection<String> defaultAdminEvts = Arrays.asList(CREATE_REALM_EVT,CREATE_USER_EVT,CREATE_ADMIN_USER_EVT);

    public static final String INSTALLED_APP_URN = "urn:ietf:wg:oauth:2.0:oob";
    public static final String INSTALLED_APP_URL = "http://localhost";
    public static final String INSTALLED_APP_LOOPBACK = "http://127.0.0.1";

    public static final String READ_TOKEN_ROLE = "read-token";
    public static final String[] BROKER_SERVICE_ROLES = {READ_TOKEN_ROLE};
    public static final String OFFLINE_ACCESS_ROLE = OAuth2Constants.OFFLINE_ACCESS;
    public static final String DEFAULT_ROLES_ROLE_PREFIX = "default-roles";

    public static final String AUTHZ_UMA_PROTECTION = "uma_protection";
    public static final String AUTHZ_UMA_AUTHORIZATION = "uma_authorization";
    public static final String[] AUTHZ_DEFAULT_AUTHORIZATION_ROLES = {AUTHZ_UMA_AUTHORIZATION};

    //App Type Constants
    public static final String HIGH_SECURE = "high_secure";
    public static final String BROWSER_BASED = "browser_based";
    public static final String REST_API = "rest_api";
    public static final String M2M = "machine_to_machine";
    public static final String SAML="saml";
    public static final String CIBA="ciba";
    public static final String IWS = "iws";

    // 15 minutes
    public static final int DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT = 900;
    // 30 days
    public static final int DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT = 2592000;
    // KEYCLOAK-7688 Offline Session Max for Offline Token
    // 60 days
    public static final int DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN = 5184000;
    public static final String DEFAULT_SIGNATURE_ALGORITHM = Algorithm.RS256;

    public static final String DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS = Algorithm.ES256;
    public static final String DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME = "keycloak";
    // it stands for optional parameter not specified in WebAuthn
    public static final String DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED = "not specified";

    // Prefix used for the realm attributes and other places
    public static final String WEBAUTHN_PASSWORDLESS_PREFIX = "Passwordless";

    public static final String VERIFY_EMAIL_KEY = "VERIFY_EMAIL_KEY";
    public static final String VERIFY_EMAIL_CODE = "VERIFY_EMAIL_CODE";
    public static final String EXECUTION = "execution";
    public static final String CLIENT_ID = "client_id";
    public static final String TAB_ID = "tab_id";
    public static final String KEY = "key";
    public static final String CODE = "code";
    public static final String STATE = "state";

    public static final String KC_ACTION = "kc_action";
    public static final String KC_ACTION_PARAMETER = "kc_action_parameter";
    public static final String KC_ACTION_STATUS = "kc_action_status";
    public static final String KC_ACTION_EXECUTING = "kc_action_executing";
    public static final int KC_ACTION_MAX_AGE = 300;

    public static final String IS_AIA_REQUEST = "IS_AIA_REQUEST";
    public static final String AIA_SILENT_CANCEL = "silent_cancel";
    public static final String AUTHENTICATION_EXECUTION = "authenticationExecution";
    public static final String CREDENTIAL_ID = "credentialId";

    public static final String SKIP_LINK = "skipLink";
    public static final String TEMPLATE_ATTR_ACTION_URI = "actionUri";
    public static final String TEMPLATE_ATTR_REQUIRED_ACTIONS = "requiredActions";

    // Prefix for user attributes used in various "context"data maps
    public static final String USER_ATTRIBUTES_PREFIX = "user.attributes.";

    // Roles already granted by a mapper when updating brokered users.
    public static final String MAPPER_GRANTED_ROLES = "MAPPER_GRANTED_ROLES";

    // Groups already assigned by a mapper when updating brokered users.
    public static final String MAPPER_GRANTED_GROUPS = "MAPPER_GRANTED_GROUPS";

    // Indication to admin-rest-endpoint that realm keys should be re-generated
    public static final String GENERATE = "GENERATE";

    public static final int DEFAULT_MAX_RESULTS = 100;

    // Delimiter to be used in the configuration of authenticators (and some other components) in case that we need to save
    // multiple values into single string
    public static final String CFG_DELIMITER = "##";

    // Better performance to use this instead of String.split
    public static final Pattern CFG_DELIMITER_PATTERN = Pattern.compile("\\s*" + CFG_DELIMITER + "\\s*");

    public static final String OFFLINE_ACCESS_SCOPE_CONSENT_TEXT = "${offlineAccessScopeConsentText}";

    public static final String MINIO_PUBLIC_SERVER_URL = "MINIO_PUBLIC_URL";
    public static final String MINIO_SERVER_URL = "MINIO_SERVER_URL";
    public static final String LICENSE_SERVER_URL = "LICENSE_URL";
    public static final String DEFAULT_LICENSE_SERVER_URL = "https://license.dev.qlikverify.com"; 
    // Empty by default: a self-hosted instance points the admin-console "Docs" link at its own
    // documentation via the DOCS_URL env var. When DOCS_URL is unset, the console links to a
    // bundled "documentation not configured" page (admin/resources/docs-not-configured.html)
    // instead of any ezto-hosted docs.
    public static final String DEFAULT_DOCS_URL = "";
    public static final String SOCIAL_SETUP_MD_PATH="/social/setup-guides/";


    //List Of Frame SRC to be included in Browser Header
    public static final String SELF_FRAME_SRC="'self'";
    public static final String FRESHDESK_FRAME_SRC="https://wchat.in.freshchat.com/";
    public static final String GOOGLE_FRAME_SRC="https://www.google.com/";
    public static final String TANGO_FRAME_SRC="https://app.tango.us/";
    public static final String YOUTUBE_FRAME_SRC = "https://www.youtube.com/";
    public static final String VIDEO_KYC_FRAME_SRC = EnvironmentReader.getValue("EZTO_VERIFY_VIDEO_SDK_URL", "https://video-kyc.eztovrfy.com");
    public static Collection<String> listOfDefaultFrameSrc = Arrays.asList(SELF_FRAME_SRC,FRESHDESK_FRAME_SRC,GOOGLE_FRAME_SRC,TANGO_FRAME_SRC, YOUTUBE_FRAME_SRC, VIDEO_KYC_FRAME_SRC);

    //Email Templates List
    public static final String WELCOME_EMAIL="welcome-email.ftl";
    public static final String VERIFICATION_EMAIL_CODE = "email-verification-with-code.ftl";
    public static final String VERIFICATION_EMAIL_LINK_FOR_REG = "magic-link-registration.ftl";
    public static final String VERIFICATION_EMAIL_LINK_FOR_LOGIN = "magic-link-login.ftl";
    public static final String ANOMALY_DETECTION ="anomaly-detection.ftl";

    //Array consists of emailTemplates needs to uploadInMinio
    public static final Collection<String> emailTemplates = Arrays.asList(WELCOME_EMAIL,VERIFICATION_EMAIL_CODE,VERIFICATION_EMAIL_LINK_FOR_REG,VERIFICATION_EMAIL_LINK_FOR_LOGIN,ANOMALY_DETECTION);
    public static final Collection<String> boEmailTemplates = Arrays.asList(VERIFICATION_EMAIL_CODE);

    //TEMPLATE TYPE
    public static final String AUTHENTICATION_FLOW="Authentication";
    public static final String MFA="MFA";
    public static final String SSO="SSO-Integrations";
    public static final String PASSWORDLESS = "Passwordless";
    public static final String RECOMMENDED = "Recommended";
    public static final String APPS = "Apps";
    public static final String PARTNERS = "Partners";
    public static final String IWS_MARKETPLACE="IWS";

    //Marketplace Categories
    public static final List<String> marketplaceCategories = Arrays.asList(RECOMMENDED,AUTHENTICATION_FLOW,MFA,PASSWORDLESS,SSO,APPS,PARTNERS,IWS_MARKETPLACE);

    //Available User Syncs
    public static final String AZURE_AD="azure_ad";
    public static final String DUO_WS="duo_ws";
    public static final String GOOGLE_WS="google_ws";
    public static final String OKTA_WS="okta_ws";
    public static final String SYNCED_FROM="syncedFrom";
    public static final String SYNCED_ROLES="syncedRoles";
    public static final Collection<String> availableSyncOptions = Arrays.asList(AZURE_AD,DUO_WS,GOOGLE_WS,OKTA_WS);


    // Client Polices Realm Attributes Keys
    public static final String CLIENT_PROFILES = "client-policies.profiles";
    public static final String CLIENT_POLICIES = "client-policies.policies";


    // Authentication session note, which contains loa of current authentication in progress
    public static final String LEVEL_OF_AUTHENTICATION = "level-of-authentication";

    // Authentication session (and user session) note, which contains map with authenticated levels and the times of their authentications,
    // so it is possible to check when particular level expires and needs to be re-authenticated
    public static final String LOA_MAP = "loa-map";

    public static final String REQUESTED_LEVEL_OF_AUTHENTICATION = "requested-level-of-authentication";
    public static final String FORCE_LEVEL_OF_AUTHENTICATION = "force-level-of-authentication";
    public static final String ACR_LOA_MAP = "acr.loa.map";
    public static final String DEFAULT_ACR_VALUES = "default.acr.values";
    public static final int MINIMUM_LOA = 0;
    public static final int NO_LOA = -1;

    /**
     * If set as an attribute in the {@link KeycloakSession}, indicates that the storage should batch write operations.
     */
    public static final String STORAGE_BATCH_ENABLED = "org.keycloak.storage.batch_enabled";

    /**
     * If {@code #STORAGE_BATCH_ENABLED} is set, indicates the batch size.
     */
    public static final String STORAGE_BATCH_SIZE = "org.keycloak.storage.batch_size";

    public static final String OFFICE_365_SCRIPT= ""
            +"# Check if MSOnline Module is installed"
            +"if (!(Get-Module -ListAvailable -Name MSOnline)) {"
            +"    Write-Host \"ERROR!!! MSOnline does not exist!`n`nPlease install MSOnline module using following steps:`n\""
            +"    Write-Host \"Step 1: Click on start, search for PowerShell and run it as Administrator.`n\""
            +"    Write-Host \"Step 2: Run the command given below to install MSOnline module:`n`n`tInstall-Module MSOnline`n\""
            +"    Write-Host \"`tNOTE: If prompted about installing a module from an untrusted repository, type Y and press ENTER.`n\""
            +"    Write-Host \"`tNOTE: If prompted to install the NuGet provider, type Y and press ENTER.`n\""
            +"    Write-Host \"`tNOTE: If prompted to install the module from PSGallery, type Y and press ENTER.`n\""
            +"    Write-Host \"`nStep 3: Run this script file again after installing MSOnline module.\""
            +"} else {"
            +"    # Pre-configured details required to federate your domain"
            +"    $domainName = \"@domainName\""
            +"    $entityId = \"@entityId\""
            +"    $logoutEndpoint = \"@logoutEndpoint\""
            +"    $loginEndpoint = \"@loginEndpoint\""
            +"    $certificate = \"@x509certificate\""
            +""
            +"    Write-Host \"`nConnect to your Office365 administrator account:\""
            +"    # Connect to Microsoft Online Service"
            +"    Connect-MsolService -ErrorAction SilentlyContinue"
            +""
            +"    if ($?) {"
            +"        Write-Host \"`nUsing the following details:\""
            +"        Write-Host \"Domain: \" $domainName"
            +"        Write-Host \"Entity ID: \" $entityId"
            +"        Write-Host \"Logout URL: \" $logoutEndpoint"
            +"        Write-Host \"Login URL: \" $loginEndpoint"
            +""
            +"        # Check if domain is under the above Office 365 account"
            +"        if (!(Get-MsolDomain -DomainName $domainName -ErrorAction SilentlyContinue)) {"
            +"            Write-Host $domainName \"is not under this account. Please follow below steps to resolve the issue:`n\""
            +"            Write-Host \"Step 1: Make sure you logged in with correct Office 365 admin account.`n\""
            +"            Write-Host \"Step 2: If the domain is correct, then go to your Office365 admin console and add this domain.`n\""
            +"            Write-Host \"Step 3: Run this script file again after resolving the issue\""
            +"        } else {"
            +"            Write-Host \"Federating $domainName\""
            +"            Write-Host -NoNewLine \"`nPlease wait.\""
            +"            # 10 seconds wait"
            +"            $counter = 0;"
            +"            while($counter -lt 10) {"
            +"               Start-Sleep -s 1"
            +"               Write-Host -NoNewLine \".\""
            +"               $counter += 1"
            +"            }"
            +""
            +"            Set-MsolDomainAuthentication -Authentication Managed -DomainName $domainName -IssuerUri $entityId -LogOffUri $logoutEndpoint -PassiveLogOnUri $loginEndpoint -SigningCertificate $certificate -PreferredAuthenticationProtocol SAMLP"
            +"            # 10 seconds wait"
            +"            while($counter -gt 0) {"
            +"               Start-Sleep -s 1"
            +"               Write-Host -NoNewLine \".\""
            +"               $counter -= 1"
            +"            }"
            +"            Write-Host \"`n\""
            +"            # Federate the domain using given account credentials"
            +"            Set-MsolDomainAuthentication -Authentication Federated -DomainName $domainName -IssuerUri $entityId -LogOffUri $logoutEndpoint -PassiveLogOnUri $loginEndpoint -SigningCertificate $certificate -PreferredAuthenticationProtocol SAMLP"
            +"            Write-Host $domainName \" is now federated!\""
            +"        }"
            +"    } else {"
            +"        Write-Host \"Invalid Credentials! Try running this script again!\""
            +"    }"
            +"}"
            +"Write-Host \"`nPress any key to continue...\""
            +"$null = $Host.UI.RawUI.ReadKey(\"NoEcho,IncludeKeyDown\")"
            +"";

    public static final String ONBOARDING_COMPLETED_STEPS = "onboarding-steps-completed" ;
    public static final String DEFAULT_PASSWORD_POLICY = "default_password_policy";
    public static final String CURRENT_ACTION_ID = "currentActionId";
    public static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(MINIO_PUBLIC_SERVER_URL);
    public static final String TEMPLATE_PATH = "/templates/";
    public static HashSet<String> SUPPORTED_LOCALES = new HashSet<>();

    static {
        LinkedHashSet<String> locales = new LinkedHashSet<>();
        locales.add("en");
        locales.add("ar");

        SUPPORTED_LOCALES.addAll(locales);
    }
    public static final String EZTO_ANAMOLOY_DETECTION = "qv-anamoly-detection";

    public static final String REQUEST_ID = "requestId";
    // validator
    public static final String VALIDATOR_PASSED = "has been Passed";
    public static final String VALIDATOR_FAILED = "has been Failed";
}
