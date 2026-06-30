package com.qlikverify.services.theme.onboarding;

import org.keycloak.common.Version;
import org.keycloak.provider.ProviderConfigProperty;

public class Constants {

    private Constants() {
        // This constructor is private to prevent instantiation of the class
    }

    public static final String OB_COMPLETED_STEPS = "onboarding-steps-completed";
    public static final String OB_ADMIN_EMAIL_SETUP = "ob-admin-email-setup";
    public static final String OB_ADMIN_SECURITY_DEFENSES_SETUP = "ob-admin-security-defenses-setup";
    public static final String OB_ADMIN_SETUP = "ob-admin-setup";
    public static final String OB_ADMIN_SMS_SETUP = "ob-admin-sms-setup";
    public static final String OB_ADMIN_WHATSAPP_SETUP = "ob-admin-whatsapp-setup";
    public static final String OB_ADMIN_WORKSPACE_SETUP = "ob-admin-workspace-setup";
    public static final String OB_ADMIN_AUDIT_SETUP = "ob-admin-audit-setup";
    public static final String OB_ADMIN_AUDIT_STREAMER_SETUP = "ob-admin-audit-streamer-setup";

    public static final String OB_ADMIN_MASTER_SMS_DESC = "ob-admin-master-sms-desc";
    public static final String OB_ADMIN_SMS_DESC = "ob-admin-sms-desc";
    public static final String OB_ADMIN_MASTER_EMAIL_DESC = "ob-admin-master-email-desc";
    public static final String OB_ADMIN_EMAIL_DESC = "ob-admin-email-desc";
    public static final String OB_ADMIN_MASTER_WHATSAPP_DESC = "ob-admin-master-whatsapp-desc";
    public static final String OB_ADMIN_WHATSAPP_DESC = "ob-admin-whatsapp-desc";

    public static final String UNABLE_TO_SEND_EMAIL = "unable-to-send-email";
    public static final String VERIFICATION_EMAIL_SENT = "verification-email-sent";

    public static final String OK = "ok";
    public static final String GENERAL_ERROR = "general-error";
    public static final String ENV = "ENV";
    public static final String PROD_ENV = "prod";
    public  static  final  String QV_ANAMOLOY_DETECTION = "qv-anamoly-detection";
    public static final String ENABLE_ANOMALY_DETECTION = "enable_anamoly_detection";
    public static final String DIFF_OS_TRIGGER = "DIFF_OS_TRIGGER";
    public static final String DIFF_IP_TRIGGER = "DIFF_IP_TRIGGER";
    public static final String DIFF_BROWSER_TRIGGER = "DIFF_BROWSER_TRIGGER";
    public  static  final  String QV_BRUTEFORCE_DETECTION = "qv-bruteforce-detection";
    public static  final String BRUTE_FORCE_PROTECTED = "bruteForceProtected";
    public static  final String PERMANENT_LOCKOUT = "permanentLockout";
    public static  final String FAILURE_FACTOR = "failureFactor";
    public static  final String WAIT_INCREMENT = "waitIncrementSeconds";
    public static  final String QUICK_LOGIN_MILLI_CHECK = "quickLoginCheckMilliSeconds";
    public static  final String QUICK_LOGIN_WAIT = "minimumQuickLoginWaitSeconds";
    public static  final String MAX_WAIT = "maxFailureWaitSeconds";

    public static  final String FAILURE_RESET_TIME = "maxDeltaTimeSeconds";
    public static final String ADMIN_EMAIL_PROVIDER = "admin_email_provider";
    public static final String TEST_NUMBER = "test_mobile_number";
    public static final String ADMIN_SMS_PROVIDER = "admin_sms_provider";
    public static final String WHATSAPP = "whatsapp";
    public static final String STREAMER = "streamer";
    public static final String ADMIN_WHATSAPP_PROVIDER = "admin_whatsapp_provider";
    public static final String DISPLAY_NAME = "display_name";
    public static final String DEFAULT_REGION = "defaultRegion";
    public static final String IMAGE_UPLOAD = "image_upload";
    public static final String USER_INVITE_WAY = "user_invite_way";
    public static final String INVITE_LINK_EXPIRY = "invite_link_expiry";
    public static final String INVITE_ADMIN_USERS = "invite_admin_users";
    public static final String INVITE_LINK_EXPIRY_ATTRIBUTE = "actionTokenGeneratedByUserLifespan.user-invite-link";
    public static final String VIA_LINK = "Via Link";
    public static final String QLIK_VERIFY = "qlik_verify";
    public static final String PRIMARY = "primary";
    public static final String ON_PREMISE = "onpremise";
    public static final String BTN_PRIMARY = "btnPrimary";
    public static final String TEXT_PRIMARY = "textPrimary";
    public static final String BTN_PRIMARY_HOVER = "btnPrimaryHover";
    public static final String BTN_PRIMARY_VALUE_BO = "#1c64eb";
    public static final String BTN_PRIMARY_VALUE_CIAM = "#323232";
    public static final String BTN_PRIMARY_VALUE_WIAM = "#4CA355";
    public static final String PRIMARY_OPAQUE = "primaryOpaque";


    public static final String BTN_SECONDARY = "btnSecondary";
    public static final String CLOUD = "cloud";
    public static final String BTN_SECONDARY_HOVER = "btnSecondaryHover";
    public static final String BUTTON_HOVER_VALUE = "#171818";
    public static final String BUTTON_SECONDARY_VALUE = "#292b2c";
    public static final String KEYCLOAK_STATE_CHECKER = "WELCOME_STATE_CHECKER";
    public static final String ACTION = "action";
    public static final String CUSTOM_SUBSCRIPTION = "custom_subscription";
    public static final String MAX_REALMS = "max-realms";
    public static final ProviderConfigProperty MAX_REALMS_PROPERTY = new ProviderConfigProperty();

    public static final int DEFAULT_MAX_REALMS = Version.TENANT_COUNT;
    public static final String MAX_APP_COUNT_REACHED= "max-app-count-reached-contact-sales";
    public static final String HTTPS = "https://";
    public static final String ONBOARDING_SUBSCRIPTION_CHANGE = "obSubscriptionChange";
    public static final String PR_ID = "pr_id";
    public static final String MAX_USER_COUNT_REACHED = "max-user-count-reached";
    public static final String USERS = "users";

}
