package com.qlikverify.extensions.admin.misc.models;

import liquibase.pro.packaged.S;

public class Constants {

    //Network - Responses
    public static final String CONNECTION_ERROR = "unable-to-connect-to-url";
    public static final String CONNECTION_SUCCESSFUL = "connected-to-server";
    public static final String NETWORK_ERROR_CODE = "400";
    public static final String UNABLE_TO_RETRIEVE_DATA = "unable-to-retrieve-data";



    //General Constants
    public static final String INITIATED = "initiated";
    public static final String RESOLVED = "resolved";
    public static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final String NUMERIC_STRING = "0123456789";

    //Keycloak Admin endpoints
    public static final String OPENID_CONFIG_URL = "/realms/{realm}/.well-known/openid-configuration";
    public static final String ISSUER_URL = "/realms/{realm}";
    public static final String OPENID_TOKEN_URL = "/realms/{realm}/protocol/openid-connect/token";
    public static final String MAIL_LOGIN_ACTION_URL_LOCAL = "{protocol}://{hostName}:{port}/auth/realms/{realm}/login-actions/action-token?";

    public static final String OPENID_AUTH_URL = "/realms/{realm}/protocol/openid-connect/auth";
    public static final String OPENID_TOKEN_INTROSPECT_CONFIG_URL = "/realms/{realm}/protocol/openid-connect/token/introspect";
    public static final String OPENID_USER_INFO_URL = "/realms/{realm}/protocol/openid-connect/userinfo";
    public static final String OPENID_LOGOUT_URL = "/realms/{realm}/protocol/openid-connect/logout";
    public static final String OPENID_CERTS_URL = "/realms/{realm}/protocol/openid-connect/certs";
    public static final String OPENID_LOGIN_STATUS_IFRAME_URL = "/realms/{realm}/protocol/openid-connect/login-status-iframe.html";
    public static final String OPENID_CLIENT_REGISTRATION_URL = "/realms/{realm}/clients-registrations/openid-connect";

    //APPLICATION CONSTANTS
    public static final String CREATE_REALM_CLIENT_ID = "create-realm-app";

    //HTTP verb
    public static final String GET_REQUEST = "GET";
    public static final String POST_REQUEST = "POST";
    public static final String PUT_REQUEST = "PUT";
    public static final String DELETE_REQUEST = "DELETE";

    //ERROR CODES
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String GENERAL_ERROR = "GENERAL ERROR";
    public static final String HTTP_UTILITY_ERROR = "HTTP_UTILITY_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String OK = "OK";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String CONFLICT = "CONFLICT";

    public static final String USER_NOT_FOUND_STATUS = "USER_NOT_FOUND";
    public static final String UMA_GRANTED = "UMA_GRANTED";
    public static final String UMA_CHECK_START = "UMA_CHECK_START";
    public static final String UMA_STATUS = "UMA_STATUS";
    public static final String HTTP_CONNECTION_EXCEPTION = "Exception in Connection : ";
    public static final String HTTP_PROCESSING_REQ = "PROCESSING REQUEST: ";
    public static final String HTTP_CONNECTION_ERROR = "HTTP ERROR: ";
    public static final String HTTP_CONNECTION_SUCESS = "HTTP SUCCESS: ";
    public static final String HTTP_INPUT_PROCESSED = "HTTP INPUT PROCESSED: ";


    public static final String REST_CONNECTOR_URL = "REST_CONNECTOR_URL";
    public static final String KEYCLOAK_ADMIN_URL = "KEYCLOAK_ADMIN_URL";
    public static final String KEYCLOAK_USER = "KEYCLOAK_USER";
    public static final String CLOUD_MASTER_DOMAIN = "CLOUD_MASTER_DOMAIN";
    public static final String TOKEN_URL = "TOKEN_URL";
    public static final String KEYCLOAK_ADMIN_USER = "KEYCLOAK_USER";
    public static final String KEYCLOAK_ADMIN_PASSWORD = "KEYCLOAK_PASSWORD";
    public static final String DUPLICATE_USER_CHECK = "DUPLICATE_USER_CHECK";
    public static final String PROXY_USERNAME = "PROXY_USERNAME";



    //User - Responses
    public static final String USER_NULL = "userid-should-not-be-empty";
    public static final String PROFILE_UPDATE_FAIL = "failed-to-update-profile";
    public static final String INVALID_USER = "user-does-not-exist";
    public static final String USER_UPDATE_FAILED = "user-update-failed";
    public static final String USER_NOT_FOUND = "user-does-not-exist";
    public static final String USER_NOT_AUTHORIZED_TO_ACCESS_PORTAL="not-authorized-to-access-payment-portal";
    public static final String ACC_CREATOR_NOT_FOUND = "account-creator-dont-exist";
    public static final String USER_ACCESS_REVOKED = "admin-access-revoked";
    public static final String USER_ROLE_REVOKED = "user-role-revoked";
    public static final String USER_ROLE_REVOKE_FAILED = "failed-to-revoke-user-role";

    public static final String USER_REQ_ACTION_REVOKED = "user-required-actions-revoked";
    public static final String USER_REQ_ACTION_REVOKE_FAILED = "failed-to-revoke-user-required-actions";
    public static final String USER_REQ_ACTION_INVALID_DATA = "required-actions-should-not-be-empty";

    public static final String USER_UPDATE_SUCCESS = "user-added-successfully";
    public static final String USER_VERIFY_SUCCESS = "user-verified-successfully";
    public static final String USER_PROMOTE_SUCCESS = "user-promoted-successfully";
    public static final String USER_CREATE_SUCCESS = "user-created-successfully";
    public static final String USER_DELETE_SUCCESS = "user-deleted-successfully";
    public static final String USER_UPDATE_FAIL = "failed-to-update-user";
    public static final String USER_CREATE_FAIL = "failed-to-create-user";
    public static final String USER_ID_NULL = "username-should-not-be-empty";
    public static final String USER_DELETE_FAIL = "failed-to-delete-user";
    public static final String USER_FETCH_FAIL = "failed-to-get-user";
    public static final String SEND_SIGNATURE_FAILED = "failed-to-send-signature";
    public static final String GET_SIGNATURE_RESULT_FAILED = "failed-to-get-sign-result";
    public static final String GET_CREDENTIALS_FAILED = "failed-to-fetch-credentials";
    public static final String GET_PROPERTY_FAIL = "failed-to-get-user-property";
    public static final String USER_LOGOUT_SUCCESS = "user-logged-out-successfully";
    public static final String USER_LOGOUT_FAILURE = "failed-to-log-out-user";


    //Realm - Responses
    public static final String TENANT_ALREADY_EXISTS="company-name-already-exists";
    public static final String REALM_NULL = "company-name-should-not-be-empty";
    public static final String TENAT_NAME_MIN_LENGTH_REQ = "company-name-must-have-3-char";
    public static final String TENANT_NAME_INVALID= "company-name-constrains";
    public static final String TENANT_NAME_RESERVED= "reserved-name-error";

    public static final String REALM_NOT_FOUND = "tenant-not-found";
    public static final String REALM_DELETE_FAIL = "failed-to-delete-tenant";
    public static final String REALM_NOT_EXIST = "tenant-does-not-exist";
    public static final String REALM_UPDATE_FAIL = "failed-to-update-tenant";
    public static final String REALM_FETCH_FAIL = "failed-to-get-tenant";
    public static final String REALM_CREATE_FAIL = "failed-to-create-tenant";
    public static final String REALM_EXISTS_ERROR = "tenant-already-exists";
    public static final String REALM_MANDATORY_ERROR = "admin-username-and-password-mandatory";

    public static final String ROLE_EXISTS_ERROR = "role-already-exists";
    public static final String ROLE_NOT_FOUND_ERROR = "role-not-found";

    //Email - Responses
    public static final String EMAIL_CONFIG_MISSING = "email-configuration-not-available";
    public static final String UNABLE_TO_SEND_EMAIL = "unable-to-send-email";
    public static final String VERIFICATION_EMAIL_SENT = "verification-email-sent";
    public static final String INVALID_EMAIL_ADDRESS="invalid-email-address";
    public static final String REALM_SMTP_UPDATE_SUCCESS = "successfully-updated-smtp-details";
    public static final String REALM_SMTP_UPDATE_FAILED = "failed-to-update-smtp-details";
    public static final String EMAIL_TEMPLATE_FAILED = "failed-to-template-email";
    public static final String MISSING_MANDATORY_SMTP_DATA = "mandatory-fields-for-email-config";
    public static final String MAIL_SEND_FAILED = "failed-to-send-mail";
    public static final String MAIL_TYPE_OR_SUBJECT_MANDATORY_ERROR = "mail-type-required";
    public static final String MAIL_SEND_SUCCESS = "mail-sent-to-the-user";
    public static final String INVALID_MAIL_TYPE = "invalid-mail-type";
    public static final String INVALID_REDIRECT_URI = "invalid-redirect-uri";
    public static final String DELETE_AUTH_FAILED = "failed-to-delete-authentication";
    public static final String GET_AUTH_FAILED = "failed-to-get-authentication";
    public static final String CREATE_AUTH_SUCCESS = "deleted-old-created-new-auth-flow";
    public static final String USER_MAILID_NOT_SET = "user-has-no-email";
    public static final String USER_DISABLED = "user-is-disabled";
    public static final String MAIL_CONFIG_EMPTY_ERROR = "smtp-config-missing";
    public static final String PORTAL_URL_EMPTY_ERROR = "portal-url-missing";
    public static final String MISSING_TEMP_PWD = "temp-password-missing";
    public static final String IOS_LINK_EMPTY_ERROR = "ios-link-missing";
    public static final String TENANT_NAME_EMPTY_ERROR = "tenant-name-missing";
    public static final String USER_NAME_EMPTY_ERROR = "username-missing";
    public static final String ANDROID_LINK_EMPTY_ERROR = "android-link-missing";
    public static final String ACTIVATION_CODE_EMPTY_ERROR = "activation-code-missing";
    public static final String QR_CODE_EMPTY_ERROR = "qr-code-missing";
    public static final String DEFAULT_TEST_MAIL_SUBJECT = "test-message-kc";
    public static final String TEST_MAIL_SUBJECT = "emailTestSubject";
    public static final String TEST_MAIL_TEXT_BODY = "Test message";
    public static final String TEST_MAIL_HTML_BODY = "email-test.ftl";
    public static final String EMAIL_VERIFICATION_SUBJECT = "emailVerificationSubject";
    public static final String EMAIL_VERIFICATION_TEMPLATE = "email-verification.ftl";
    public static final String FORGOT_PASSWORD_SUBJECT = "passwordResetSubject";
    public static final String FORGOT_PASSWORD_TEMPLATE = "password-reset.ftl";
    public static final String WELCOME_EMAIL_SUBJECT = "welcomeEmailSubject";
    public static final String WELCOME_EMAIL_TEMPLATE = "welcome-email.ftl";
    public static final String ONBOARD_TEMPLATE = "onboard-email.ftl";
    public static final String TENANT_INFO_EMAIL_SUBJECT = "tenantInfoEmailSubject";
    public static final String TENANT_INFO_EMAIL_TEMPLATE = "tenant-info-mail.ftl";
    public static final String MAIL_TYPE_TEST = "TEST";
    public static final String MAIL_TYPE_WELCOME = "WELCOME";
    public static final String MAIL_TYPE_ONBOARD = "ONBOARD";
    public static final String MAIL_TYPE_VERIFY = "VERIFY";
    public static final String MAIL_TYPE_FORGOT_PASSWORD = "FORGOT_PASSWORD";
    public static final String MAIL_TYPE_TENANT_INFO = "TENANT_INFO";
    public static final String TENANT_PORTAL_URL = "https://{realm}.api.midentity.one/";
    public static final String EMAIL = "email";
    public static final String ACTIVATION_EMAIL = "ACTIVATION";
    public static final String ACTIVATION_EMAIL_SUBJECT = "userActivationEmailSubject";
    public static final String ACTIVATION_EMAIL_TEMPLATE = "activation-info.ftl";

    //User Info messages
    public static final String USER_INFO_SUCCESS = "User info fetched successfully";
    public static final String USER_LIST_SUCCESS = "Users fetched successfully";
    public static final String USER_PROVISION_SUCCESS = "User status fetched successfully";
    public static final String ADMIN_LIST_SUCCESS = "Admin users fetched successfully";
    public static final String USER_EMPTY = "Users not available";

    //Realm Info messages
    public static final String REALM_SETTINGS_SUCCESS = "Tenant settings updated successfully";
    public static final String REALM_SETTINGS_FAILED = "Tenant settings update failed";
    public static final String REALM_SUCCESS = "Tenant created successfully";
    public static final String REALM_DELETE_SUCCESS = "Tenant deleted successfully";
    public static final String REALM_UPDATE_SUCCESS = "Tenant updated successfully";
    public static final String REALM_INFO_SUCCESS = "Tenant info fetched successfully";
    public static final String REALM_LIST_SUCCESS = "Tenant list fetched successfully";
    public static final String REALM_CRED_SUCCESS = "Tenant credentials fetched successfully";
    public static final String REALM_EMPTY = "No tenants found";

    public static final String ROLE_SUCCESS = "Role created successfully";
    public static final String ROLE_DELETE_SUCCESS = "Role deleted successfully";

    public static final String TEMPLATE_ID_NOT_FOUND = "Template ID Not found";
    public static final String EXCEPTION_FETCHING_TEMPLATES = "Something went wrong in fetching Templates";
    public static final String AUTHORIZATION = "Authorization";
    public static final String NOTIFICATION_TYPE = "notificationType";
    public static final String DASHBOARD = "dashboard";
    public static final String IS_ADMIN_EVENT = "isAdminEvent";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String CLIENT_NAME = "clientName";
    public static final String STATE = "state";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String AUTHENTICATION_FAILED = "Authentication Failed";



}