package com.qlikverify.marketplace;

import org.keycloak.EnvironmentReader;

public class TemplateConstants {
    private TemplateConstants(){

    }

    public static final String QV_AUTHOR = EnvironmentReader.getValue("VARIANT", "ezto verify");

    //TemplateNames

    //1.Authentication
    public static final String EMAIL_ML_TEMP = "EmailMagicLinkTemplate";
    public static final String EMAIL_OTP_TEMP = "EmailOtpTemplate";
    public static final String EMAIL_WEBAUTHN_TEMP = "EmailWebAuthnTemplate";
    public static final String MOB_OTP_TEMP = "PhoneOtpTemplate";
    public static final String BASIC_EMAIL_MOB_TEMP = "EmailPasswordTemplate";
    public static final String USERNAME_PASS = "UsernamePasswordTemplate";
    public static final String WHATSAPP_TEMP = "WhatsappTemplate";

    //2.MFA
    public static final String MFA_DUO_SEC_TEMP = "MFADuoSecTemplate";
    public static final String MFA_AUTHY_SEC_TEMP = "MFAAuthyTemplate";
    public static final String MFA_CYLOCK_TEMP = "MFACylockTemplate";

    //3.SSO
    public static final String SSO_SALESFORCE_TEMP = "SSOSalesforceTemplate";
    public static final String SSO_SLACK_TEMP = "SSOSlackTemplate";
    public static final String SSO_YOUTRACK_TEMP = "SSOYoutrackTemplate";
    public static final String SSO_ASSET_SONAR_TEMP = "SSOAssetSonarTemplate";
    public static final String SSO_BREEZY_HR_TEMP = "SSOBreezyHRTemplate";
    public static final String SSO_DROPBOX_TEMP = "SSODropboxTemplate";
    public static final String SSO_GOOGLE_WORKSPACE_TEMP = "SSOGoogleWorkspaceTemplate";
    public static final String SSO_AWS_TEMP = "SSOAWSWorkspaceTemplate";
    public static final String SSO_GITLAB_TEMP = "SSOGitlabWorkspaceTemplate";
    public static final String SSO_GITHUB_ENT_CLOUD_TEMP = "SSOGithubEntCloudWorkspaceTemplate";
    public static final String SSO_ZOHO_TEMP = "SSOZohoTemplate";
    public static final String SSO_OFFICE365_TEMP = "SSOOffice365Template";
    public static final String SSO_OKTA_TEMP = "SSOOktaTemplate";
    public static final String SSO_SENDGRID_TEMP = "SSOSendGridWorkspaceTemplate";
    public static final String SSO_ZOOM_TEMP = "SSOZoomWorkspaceTemplate";
    public static final String SSO_ZENDESK_TEMP = "SSOZendeskWorkspaceTemplate";
    public static final String SSO_ATLASSIAN_TEMP = "SSOAtlassianWorkspaceTemplate";
    public static final String SSO_AHA_TEMP = "SSOAhaWorkspaceTemplate";
    public static final String SSO_NEWRELIC_TEMP = "SSONewRelicWorkspaceTemplate";
    public static final String SSO_JENKINS_TEMP = "SSOJenkinsWorkspaceTemplate";
    public static final String SSO_HEROKU_TEMP = "SSOHerokuWorkspaceTemplate";
    public static final String SSO_CISCOWEBEX_TEMP = "SSOCiscoWebexWorkspaceTemplate";
    public static final String SSO_FRESHDESK_TEMP = "SSOFreshdeskWorkspaceTemplate";
    public static final String SSO_ONE_DESK_TEMP = "SSOOneDeskWorkspaceTemplate";
    public static final String SSO_JIRA_TEMP = "SSOJiraWorkspaceTemplate";
    public static final String SSO_CONFLUENCE_TEMP = "SSOConfluenceWorkspaceTemplate";
    public static final String SSO_APACHEGUACAMOLE_TEMP = "SSOApacheGuacamoleWorkspaceTemplate";
    public static final String SSO_DATADOG_TEMP = "SSODataDogWorkspaceTemplate";
    public static final String SSO_LUCIDCHART_TEMP = "SSOLucidChartWorkspaceTemplate";
    public static final String SSO_NUCLINO_TEMP = "SSONuclinoWorkspaceTemplate";
    public static final String SSO_PLURALSIGHT_TEMP = "SSOPluralSightWorkspaceTemplate";
    public static final String SSO_SENTRY_TEMP = "SSOSentryWorkspaceTemplate";
    public static final String SSO_SONARQUBE_TEMP = "SSOSonarQubeWorkspaceTemplate";
    public static final String SSO_BOX_TEMP = "SSOBoxWorkspaceTemplate";
    public static final String SSO_GOTOMEETING_TEMP = "SSOGotoMeetingWorkspaceTemplate";
    public static final String SSO_SPLUNK_TEMP = "SSOSplunkWorkspaceTemplate";
    public static final String SSO_SNOWFLAKE_TEMP = "SSOSnowFlakeWorkspaceTemplate";
    public static final String SSO_EKARDA_TEMP = "SSOEkardaTemplate";
    public static final String SSO_BLOCKSEDIT_TEMP ="SSOBlocksEditTemplate";
    public static final String SSO_THOUSANDEYES_TEMP = "SSOThousandEyesTemplate";
    public static final String SSO_CLOUDAMQP_TEMP = "SSOCloudAMQPTemplate";
    public static final String SSO_BLOGIN_TEMP = "SSOBlogInWorkspaceTemplate";
    public static final String SSO_CORALOGIX_TEMP = "SSOCoralogixWorkspaceTemplate";
    public static final String SSO_KINTONE_TEMP = "SSOKintoneWorkspaceTemplate";
    public static final String SSO_STRIPE_TEMP = "SSOStripeWorkspaceTemplate";
    public static final String SSO_ASANA_TEMP = "SSOAsanaWorkspaceTemplate";
    public static final String SSO_JAMFPRO_TEMP = "SSOJamfProWorkspaceTemplate";
    public static final String SSO_WORKTEAM_TEMP = "SSOWorkteamWorkspaceTemplate";
    public  static final String SSO_LEARNWORDS_TEMP = "SSOLearnwordsWorkspaceTemplate";
    public static final String SSO_JITBIT_TEMP = "SSOJitbitWorkspaceTemplate";
    public static final String SSO_PAGER_DUTY_TEMP = "SSOPagerDutyWorkspaceTemplate";
    public static final String SSO_PIPEDRIVE_TEMP = "SSOPipedriveWorkspaceTemplate";
    public static final String SSO_BAMBOOHR_TEMP = "SSOBambooHRWorkspaceTemplate";
    public static final String SSO_SCREENSTEPS_TEMP = "SSOScreenstepsWorkspaceTemplate";
    public static final String SSO_SUMO_LOGIC_TEMP = "SSOSumoLogicWorkspaceTemplate";
    public static final String SSO_DELL_BOOMI_TEMP = "SSODellBoomiWorkspaceTemplate";
    public static final String SSO_DESKPRO_TEMP = "SSODeskproWorkspaceTemplate";
    public static final String SSO_WORKPLACE_TEMP = "SSOWorkplaceWorkspaceTemplate";
    public static final String SSO_TABLEAU_TEMP = "SSOTableauWorkspaceTemplate";
    public static final String SSO_MONGO_DB_TEMP = "SSOMongoDBWorkspaceTemplate";
    public static final String SSO_ANY_POINT_TEMP = "SSOAnypointWorkspaceTemplate";
    public static final String SSO_SPLASHTOP_TEMP = "SSOSplashtopWorkspaceTemplate";
    public static final String SSO_KANBAN_TOOL_TEMP = "SSOKanbanToolWorkspaceTemplate";

    //4.IWS
    public static final String IWS_CANVA_TEMP = "IWSCanvaTemplate";
    public static final String IWS_STRAPI_TEMP = "IWSStrapiTemplate";
    public static final String IWS_ODOO_TEMP = "IWSOdooTemplate";
    public static final String IWS_ASSET_SONAR_TEMP = "IWSAssetSonarTemplate";
    public static final String IWS_THOUSAND_EYES_TEMP = "IWSThousandEyesTemplate";
    public static final String IWS_APOLLO_IO_TEMP = "IWSApolloIOTemplate";
    public static final String IWS_WORKTEAM_TEMP = "IWSWorkteamTemplate";
    public static final String IWS_UDEMYBUSINESS_TEMP = "IWSUdemyBusinessTemplate";
    public static final String IWS_PLURALSIGHT_TEMP = "IWSPluralSightTemplate";
    public static final String IWS_FIGMA_TEMP="IWSFigmaTemplate";
    public static final String IWS_LOOM_TEMP="IWSLoomTemplate";
    public static final String IWS_SALESFORCE_TEMP = "IWSSalesforceTemplate";
    public static final String IWS_WOODPECKER_TEMP = "IWSWoodpeckerTemplate";
    public static final String IWS_CLOUDAMQP_TEMP = "IWSCloudAMQPTemplate";
    public static final String IWS_WORKETC_TEMP = "IWSWORKetcTemplate";
    public static final String IWS_BLOGIN_TEMP = "IWSBlogInTemplate";
    public static final String IWS_BLOCKSEDIT_TEMP = "IWSBlocksEditTemplate";
    public static final String IWS_AXOMO_TEMP = "IWSAxomoTemplate";
    public static final String IWS_ATATUS_TEMP = "IWSAtatusTemplate";
    public static final String IWS_MOOSEND_TEMP = "IWSMoosendTemplate";
    public static final String IWS_AUTOMOX_TEMP = "IWSAutomoxTemplate";
    public static final String IWS_PIKTOCHART_TEMP = "IWSPiktochartTemplate";
    public static final String IWS_PIPEDRIVE_TEMP = "IWSPipedriveTemplate";
    public static final String IWS_BRIGHTFUNDS_TEMP = "IWSBrightFundsTemplate";
    public static final String IWS_ICONSCOUT_TEMP="IWSIconScoutTemplate";
    public static final String IWS_HUB_SPOT_TEMP="IWSHubSpotTemplate";
    public static final String IWS_JITBIT_TEMP="IWSJitbitTemplate";
    public static final String IWS_DISCORD_TEMP="IWSDiscordTemplate";
    public static final String IWS_ASANA_TEMP="IWSAsanaTemplate";


    //3.1 SSO CONFIGURATIONS HELPERS
    public static final String IS_SSO = "isSSO";
    public static final String HAS_IN_BUILT_ROLES = "hasInBuiltRoles";
    public static final String SSO_TYPE = "sso_type";
    public static final String LOGO_URL = "logoUrl";
    public static final String INSTALLATION_URL = "installation_url";
    public static final String TEMPLATE_ID = "templateId";
    public static final String USERNAME = "username";
    public static final String COMPANY_ID = "company_id";
    public static final String EDIT_COMPANY_ID = "edit_company_id";
    public static final String EMAIL = "email";
    public static final String ENTITY_ID = "entity_id";
    public static final String EDIT_ENTITY_ID = "edit_entity_id";
    public static final String EDIT_ACS_URL = "edit_acs_url";
    public static final String ORGANIZATION = "organization";
    public static final String EDIT_ORGANIZATION = "edit_organization";
    public static final String EDIT_SIGNOUT_URL = "edit_signout_url";
    public static final String ACS_URL = "acs_url";
    public static final String SUPPORTED_APPS = "supported_apps";
    public static final String EDIT_SUPPORTED_APPS = "edit_supported_apps";
    public static final String APP_LIST = "app_list";
    public static final String SIGNOUT_URL = "signout_url";
    public static final String AUD = "aud";
    public static final String SUB_DOMAIN_NAME = "sub_domain_name";
    public static final String TENANT_NAME = "tenant_name";
    public static final String EDIT_TENANT_NAME = "edit_tenant_name";
    public static final String HTTPS = "https://";
    public static final String HTTP = "http://";
    public static final String EDIT_SERVICE_PROVIDER_ISSUER = "edit_service_provider_issuer";
    public static final String EDIT_SUB_DOMAIN_NAME = "edit_sub_domain_name";
    public static final String SKIP_DEFAULT_AUDIENCE = "skipDefaultAudience";
    public static final String PERSISTENT_NAME_ID = "persistent";
    public static final String SSO_SIGN_IN_URL = "sso_sign_in_url";
    public static final String EDIT_SSO_SIGN_IN_URL = "edit_sso_sign_in_url";
    public static final String DOMAIN_URL = "domain_url";
    public static final String DOMAIN_NAME = "domain_name";
    public static final String EDIT_DOMAIN_NAME = "edit_domain_name";
    public static final String EDIT_DOMAIN = "edit_domain";
    public static final String EDIT_DOMAIN_URL = "edit_domain_url";
    public static final String HAS_SSO_SUB_APPS = "hasSSOSubApps";
    public static final String SERVICE_PROVIDER = "service_provider";
    public static final String EDIT_SERVICE_PROVIDER = "edit_service_provider";
    public static final String HAS_SCRIPT = "hasScript";
    public static final String MARKETPLACE_PATH = "/marketplace/";
    public static final String SCRIPT_URL = "script_url";
    public static final String REALM_PATH = "realms/";
    public static final String SSO_PATH = "/sso";
    public static final String CLIENTS_PATH = "/clients";
    public static final String TEAM_NAME = "team_name";
    public static final String EDIT_TEAM_NAME = "edit_team_name";

    public static final String LOGOUT_URL = "logout_url";
    public static final String EDIT_LOGOUT_URL = "edit_logout_url";



    //4.APPS
    public static final String APPS_WIN_LOGON_TEMP = "AppsWinLogonTemplate";
    public static final String APPS_SSO_DESKTOP_TEMP = "AppsSsoDesktopTemplate";

    //EMAIL OTP TEMPLATE
    public static final String EMAIL_OTP_TEMP_TITLE = "email-authentication-with-otp";
    public static final String EMAIL_OTP_TEMP_DESC = "email-otp-temp-desc";
    public static final String EMAIL_OTP_REG_TEMP_TITLE="email-otp-reg-temp-title";
    public static final String EMAIL_OTP_REG_TEMP_DESC="email-otp-reg-temp-desc";
    public static final String EMAIL_OTP_LOGIN_TEMP_TITLE= "email-login-with-otp-title";
    public static final String EMAIL_OTP_LOGIN_TEMP_DESC="email-login-with-otp-desc";

    //EMAIL WEBAUTH PASSWORDLESS TEMPLATE
    public static final String EMAIL_WEBAUTHN_PASSWORDLESS_TEMP_TITLE = "email-authentication-with-webauth";
    public static final String EMAIL_WEBAUTHN_PASSWORDLESS_TEMP_DESC = "email-webauthn-passwordless-temp-desc";
    public static final String EMAIL_WEBAUTHN_PASSWORDLESS_REG_TEMP_TITLE="email-webauthn-passwordless-reg-temp-title";
    public static final String EMAIL_WEBAUTHN_PASSWORDLESS_REG_TEMP_DESC="email-webauthn-passwordless-reg-temp-desc";
    public static final String EMAIL_WEBAUTHN_PASSWORDLESS_LOGIN_TEMP_TITLE= "email-webauthn-passwordless-login-temp-title";
    public static final String EMAIL_WEBAUTHN_PASSWORDLESS_LOGIN_TEMP_DESC="email-webauthn-passwordless-login-temp-desc";


    //EMAIL MAGIC_LINK TEMPLATE
    public static final String EMAIL_MAGIC_LINK_TEMP_TITLE = "email-authentication-with-magic-link";
    public static final String EMAIL_MAGIC_LINK_TEMP_DESC = "email-magic-link-temp-desc";
    public static final String EMAIL_ML_REG_TEMP_TITLE="email-ml-reg-temp-title";
    public static final String EMAIL_ML_REG_TEMP_DESC="email-ml-reg-temp-desc";
    public static final String EMAIL_ML_LOGIN_TEMP_TITLE="email-login-with-magic-link-title";
    public static final String EMAIL_ML_LOGIN_TEMP_DESC="email-login-with-magic-link-desc";

    //MOBILE OTP TEMPLATE
    public static final String MOB_OTP_REG_TEMP_TITLE="phone-registration-with-otp-title";
    public static final String MOB_OTP_REG_TEMP_DESC="phone-registration-with-otp-desc";
    public static final String MOB_OTP_TEMP_TITLE = "mob-otp-temp-title";
    public static final String MOB_OTP_TEMP_DESC = "mob-otp-temp-desc";
    public static final String MOB_OTP_LOGIN_TEMP_TITLE= "mob-otp-login-temp-title";
    public static final String MOB_OTP_LOGIN_TEMP_DESC="mob-otp-login-temp-desc";

    //Basic Template
    public static final String BASIC_EMAIL_MOB_TEMP_TITLE = "email-authentication-with-password";
    public static final String BASIC_EMAIL_MOB_TEMP_DESC = "email-password-temp-desc";
    public static final String BASIC_EMAIL_MOB_REG_TEMP_TITLE="email-password-onboarding";
    public static final String BASIC_EMAIL_MOB_REG_TEMP_DESC="basic-email-mob-reg-temp-desc";
    public static final String BASIC_EMAIL_MOB_LOGIN_TEMP_TITLE= "email-password-login-authentication";
    public static final String BASIC_EMAIL_MOB_LOGIN_TEMP_DESC="basic-email-mob-login-temp-desc";

    //Authy MFA Template
    public static final String MFA_AUTHY_TITLE="Authy";
    public static final String MFA_AUTHY_DESC="mfa-authy-desc";

    //DUO SEC MFA Template
    public static final String MFA_DUO_SEC_TITLE="Duo Security";
    public static final String MFA_DUO_SEC_DESC="mfa-duo-sec-desc";

    //CYLOCK MFA Template
    public static final String MFA_CYLOCK_TITLE="Cylock";
    public static final String MFA_CYLOCK_DESC="mfa-cylock-desc";

    //WINDOWS LOGON APPS
    public static final String APPS_WIN_LOGON_TITLE = "Windows LogOn";
    public static final String APPS_WIN_LOGON_DESC = "win-logon-desc";
    public static final String APPS_SSO_DESKTOP_DESC = "sso-desktop-desc";

    //Password Recovery

    //Install Type
    public static final String MULTIPLE = "multiple";
    public static final String ONCE = "once";

    public static final String SSO_SALESFORCE_DESC = "sso-salesforce-desc";
    public static final String SALESFORCE = "Salesforce";

    public static final String SSO_SLACK_DESC = "sso-slack-desc";
    public static final String SLACK = "Slack";
    public static final String ZENDESK = "Zendesk";
    public static final String SSO_ZENDESK_DESC = "sso-zendesk-desc";
    public static final String BAMBOOHR = "BambooHR";
    public static final String SSO_BAMBOOHR_DESC = "sso-bamboohr-desc";
    public static final String SCREENSTEPS = "Screensteps";
    public static final String SSO_SCREENSTEPS_DESC = "sso-screensteps-desc";
    public static final String SSO_AHA_DESC = "sso-aha-desc";
    public static final String AHA = "Aha";

    public static final String SSO_YOUTRACK_DESC = "sso-youtrack-desc";
    public static final String SSO_WORKTEAM_DESC = "sso-workteam-desc";
    public static final String YOUTRACK = "YouTrack";
    public static final String SSO_ASSET_SONAR_DESC = "sso-asset-sonar-desc";
    public static final String ASSET_SONAR = "AssetSonar";
    public static final String APOLLO_IO = "Apollo.io";

    public static final String SSO_DROPBOX_DESC = "sso-dropbox-desc";
    public static final String DROPBOX = "Dropbox";
    public static final String SSO_BREEZY_HR_DESC = "sso-breezy-hr-desc";
    public static final String BREEZY_HR = "BreezyHR";

    public static final String SSO_GOOGLE_WORKSPACE_DESC = "sso-google-workspace-desc";
    public static final String GOOGLE_WORKSPACE = "Google Workspace";

    public static final String SSO_AWS_DESC = "sso-aws-desc";
    public static final String AWS = "AWS";

    public static final String SSO_GITLAB_DESC = "sso-gitlab-desc";
    public static final String GITLAB = "GitLab";

    public static final String SSO_GITHUB_ENT_CLOUD_DESC = "sso-github-ent-cloud-desc";
    public static final String GITHUB_ENT_CLOUD = "Github Enterprise Cloud";

    public static final String SSO_ZOHO_DESC = "sso-zoho-desc";
    public static final String ZOHO = "Zoho";

    public static final String SSO_OFFICE365_DESC = "sso-office365-desc";
    public static final String OFFICE365 = "Office365";

    public static final String SSO_OKTA_DESC = "sso-okta-desc";
    public static final String OKTA = "Okta";
    public static final String SSO_FRESHDESK_DESC = "sso-freshdesk-desc";
    public static final String SSO_ONE_DESK_DESC = "sso-one-desk-desc";
    public static final String FRESHDESK = "Freshdesk";
    public static final String ONE_DESK = "OneDesk";
    public static final String ADMIN_CONFIGURATION_MISSING = "invalid-cred-contact-admin";
    public static final String SSO_SPLUNK_DESC = "sso-splunk-desc";
    public static final String SPLUNK = "Splunk";
    public static final String SSO_SNOWFLAKE_DESC = "sso-snowflake-desc";
    public static final String SSO_EKARDA_DESC = "sso-ekarda-desc";
    public static final String SSO_CLOUDAMQP_DESC = "sso-cloudamqp-desc";
    public static final String SSO_BLOCKSEDIT_DESC = "sso-blocksedit-desc";
    public static final String SSO_THOUSANDEYES_DESC = "sso-thousandeyes-desc";
    public static final String SSO_BLOGIN_DESC = "sso-blogin-desc";
    public static final String SSO_CORALOGIX_DESC = "sso-coralogix-desc";
    public static final String SSO_KINTONE_DESC = "sso-kintone-desc";
    public static final String SNOWFLAKE = "Snowflake";
    public static final String EKARDA = "Ekarda";
    public static final String CLOUDAMQP = "CloudAMQP";
    public static final String BLOCKSEDIT = "Blocks Edit";
    public static final String THOUSANDEYES = "ThousandEyes";
    public static final String BLOGIN = "BlogIn";
    public static final String SSO_CORALOGIX = "Coralogix";
    public static final String KINTONE = "Kintone";
    public static final String SSO_LEARNWORLDS_DESC = "sso-learworlds-desc";
    public static final String LEARNWORLDS = "Learnworlds";
    public static final String SSO_JITBIT_DESC = "sso-jitbit-desc";
    public static final String JITBIT ="Jitbit";

    public static final String SSO_JAMFPRO_DESC = "sso-jamfpro-desc";
    public static final String JAMFPRO ="JamfPro";
    public static final String WORKTEAM ="Workteam";
    public static final String DATADOG ="Datadog";
    public static final String SSO_DATADOG_DESC = "sso-datadog-desc";
    public static final String JENKINS ="Jenkins";
    public static final String SSO_JENKINS_DESC = "sso-jenkins-desc";
    public static final String SSO_ATLASSIAN_DESC = "sso-atlassian-desc";

    public static final String PAGER_DUTY ="PagerDuty";
    public static final String PIPEDRIVE ="Pipedrive";
    public static final String SSO_PAGER_DUTY_DESC = "sso-pager-duty-desc";
    public static final String SSO_PIPEDRIVE_DESC = "sso-pipedrive-desc";

    public static final String SUMO_LOGIC ="Sumo Logic";
    public static final String SSO_SUMO_LOGIC_DESC = "sso-sumo-login-desc";
    public static final String DELL_BOOMI ="Dell Boomi";
    public static final String SSO_DELL_BOOMI_DESC = "sso-dellboomi-desc";
    public static final String WORKPLACE ="Workplace";
    public static final String SSO_WORKPLACE_DESC = "sso-workspace-desc";
    public static final String DESKPRO ="Deskpro";
    public static final String SSO_DESKPRO_DESC = "sso-deskpro-desc";

    public static final String TABLEAU ="Tableau";
    public static final String SSO_TABLEAU_DESC = "sso-tableau-desc";
    public static final String MONGO_DB ="MongoDB cloud";
    public static final String SSO_MONGO_DB_DESC = "sso-mongo-db-desc";
    public static final String ANYPOINT ="Anypoint";
    public static final String SSO_ANYPOINT_DESC = "sso-anypoint-desc";
    public static final String SPLASHTOP ="Splashtop";
    public static final String SSO_SPLASHTOP_DESC = "sso-splashtop-desc";
    public static final String SSO_KANBAN_TOOL ="Kanban Tool";
    public static final String SSO_KANBAN_TOOL_DESC = "sso-kanban-tool-desc";

    public static final String CANVA = "Canva";
    public static final String DISCORD = "Discord";
    public static final String ASANA = "Asana";


    public static final String IWS_UDEMY_DESC = "iws-udemy-desc";
    public static final String UDEMY_BUSINESS = "Udemy Business";

    public static final String IWS_PLURALSIGHT_DESC = "iws-pluralsight-desc";
    public static final String PLURALSIGHT = "PluralSight";

    public static final String IWS_FIGMA_DESC = "iws-figma-desc";
    public static final String FIGMA = "Figma";

    public static final String IWS_STRAPI_DESC = "iws-strapi-desc";
    public static final String IWS_ASSET_SONAR_DESC = "iws-asset-sonar-desc";
    public static final String IWS_THOUSAND_EYES_DESC = "iws-thousand-eyes-desc";
    public static final String IWS_APOLLO_IO_DESC = "iws-apollo-io-desc";
    public static final String IWS_WORKTEAM_DESC = "iws-workteam-desc";
    public static final String WORKTEAM_IWS = "Workteam";
    public static final String IWS_JITBIT_DESC = "iws-jitbit-desc";
    public static final String STRAPI = "Strapi";

    public static final String IWS_THOUSAND_EYES =  "ThousandEyes";
    public static final String IWS_LOOM_DESC = "iws-loom-desc";
    public static final String IWS_SALESFORCE_DESC = "iws-salesforce-desc";
    public static final String IWS_WOODPECKER_DESC = "iws-woodpecker-desc";
    public static final String IWS_CLOUDAMQP_DESC = "iws-cloudamqp-desc";
    public static final String IWS_WORKETC_DESC = "iws-worketc-desc";
    public static final String IWS_BLOGIN_DESC = "iws-blogin-desc";
    public static final String IWS_BLOCKSEDIT_DESC = "iws-blocksedit-desc";
    public static final String IWS_AXOMO_DESC = "iws-axomo-desc";
    public static final String IWS_ATATUS_DESC = "iws-atatus-desc";
    public static final String IWS_MOOSEND_DESC = "iws-moosend-desc";
    public static final String IWS_AUTOMOX_DESC = "iws-automox-desc";
    public static final String IWS_PIKTOCHART_DESC = "iws-piktochart-desc";
    public static final String IWS_PIPEDRIVE_DESC = "iws-pipedrive-desc";
    public static final String IWS_BRIGHTFUNDS_DESC = "iws-brightfunds-desc";
    public static final String LOOM = "Loom";
    public static final String IWS_HUB_SPOT_DESC = "iws-hubspot-desc";
    public static final String HUBSPOT = "HubSpot";
    public static final String IWS_SALESFORCE = "Salesforce";
    public static final String IWS_WOODPECKER = "Woodpecker";
    public static final String IWS_CLOUDAMQP = "CloudAMQP";
    public static final String IWS_WORKETC = "WORKetc";
    public static final String IWS_BLOGIN = "BlogIn";
    public static final String IWS_ODOO = "Odoo";
    public static final String IWS_ODOO_DESC = "iws-odoo-desc";
    public static final String IWS_AXOMO = "Axomo";
    public static final String IWS_ATATUS = "Atatus";
    public static final String IWS_MOOSEND = "Moosend";
    public static final String IWS_AUTOMOX = "Automox";
    public static final String IWS_PIKTOCHART = "Piktochart";
    public static final String IWS_PIPEDRIVE = "Pipedrive";
    public static final String IWS_BRIGHTFUNDS = "Bright Funds";
    public static final String IWS_BLOCKSEDIT = "Blocks Edit";
    public static final String IWS_ICONSCOUT_DESC = "iws-iconscout-desc";
    public static final String ICONSCOUT = "IconScout";
    public static final String SSO_DESKTOP_TITLE = "SSO Desktop";
    public static final String EMAIL_PASSWORD_LOGIN_DESC = "email-password-login-authentication";
    public static final String PHONE_OTP_LOGIN_DESC = "phone-otp-login-authentication";
    public static final String EMAIL_PASSWORD_ONBOARDING_DESC = "email-password-onboarding";
    public static final String USERNAME_PASSWORD_TEMP_TITLE = "username-password-temp-title";
    public static final String USERNAME_PASSWORD_TEMP_DESC = "username-password-temp-desc";
    public static final String WHATSAPP_WITH_OTP_DESC = "whatsapp-with-otp-helptext";
    public static final String WHATSAPP_WITH_PUSH_DESC = "whatsapp-with-push-helptext";
    public static final String WHATSAPP_WITH_QR_DESC = "whatsapp-with-qr-helptext";
    public static final String EMAIL_ML_TEMP_CONDN = "email-ml-temp-condn";
    public static final String EMAIL_OTP_ONBOARDING_DESC = "email-otp-onboarding";
    public static final String EMAIL_MAGIC_LINK_ONBOARDING_DESC = "email-magic-link-onboarding";
    public static final String USERANME_PASSWORD_ONBOARDING_DESC = "username-password-onboarding";
    public static final String USERNAME_PASSWORD_ONBOARDING_TITLE = "username-password-onboarding-title";
    public static final String USERNAME_PASSWORD_ONBOARDING_DESCRIPTION = "username-password-onboarding-description";
    public static final String EMAIL_PASSWORD_RECOVERY_DESC = "email-password-recovery";
    public static final String EMAIL_PASSWORD_RECOVERY_TITLE = "email-password-recovery-title";
    public static final String EMAIL_PASSWORD_RECOVERY_DESCRIPTION = "email-password-recovery-description";
    public static final String PHONE_OTP_TEMP_CONDN = "phone-otp-temp-condn";
    public static final String EMAIL_OTP_LOGIN_DESC = "email-otp-login-authentication";
    public static final String EMAIL_MAGICLINK_LOGIN_DESC = "email-magiclink-login-authentication";
    public static final String USERNAME_PASSWORD_LOGIN_DESC = "username-password-login-authentication";
    public static final String USERNAME_PASSWORD_LOGIN_TEMP_TITLE = "username-password-login-temp-title";
    public static final String USERNAME_PASSWORD_LOGIN_TEMP_DESC = "username-password-login-temp-desc";
    public static final String EMAIL_WEBUTHN_ONBOARDING_DESC = "email-webauthn-onboarding";
    public static final String EMAIL_WEBAUTHN_LOGIN_DESC = "email-webauthn-login-authentication";
    public static final String IWS_CANVA_DESC = "iws-canva-desc";
    public static final String IWS_DISCORD_DESC = "iws-discord-desc";
    public static final String IWS_ASANA_DESC = "iws-asana-desc";
    public static final String PHONE_OTP_ONBOARDING_DESC = "phone-otp-onboarding";
    public static final String CRED_SHARING_WAY = "cred_sharing_way";
    public static final String JITBIT_URL = "jitbit_url";
    public static final String MARKETPLACE = "/marketplace/";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String SLACK_URL = "https://slack.com";
}
