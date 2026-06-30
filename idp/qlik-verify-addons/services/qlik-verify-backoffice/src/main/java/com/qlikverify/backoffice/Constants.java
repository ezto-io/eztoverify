package com.qlikverify.backoffice;

import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;

import java.util.HashMap;
import java.util.Map;

public class Constants {


    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    public static final String ATTRIBUTE_OLD_PHONE_NUMBER = "oldPhone";
    public static final String ATTRIBUTE_PHONE_NUMBER = "phoneNumber";

    public static final String FORM_EDIT = "edit";
    public static final String FORM_RESEND = "resendOTP";
    public static final String FORM_IS_PHONE_REGISTRATION = "isPhoneNumberRegistration";
    public static final String FORM_IS_OTP_VERIFICATION = "isOtpVerification";
    public static final String FORM_IS_OTP_LESS_VERIFICATION = "otp-less-verification";
    public static final String FORM_IS_EMAIL_UPDATE = "isEmailUpdate";
    public static final String FORM_OTP = "otp";
    public static final String OTP_LENGTH_ATTR = "otp_len";
    public static final String FORM_PHONE_NUMBER = "phoneNumber";
    public static final String PHONE_NUMBER_REGION = "phoneNumberRegion";
    public static final String PHONE_NUMBER_WITH_COUNTRY_CODE = "phoneNumberWithCountryCode";
    public static final String WHATSAPP_NO = "whatsappNumber";

    public static final String ERROR = "error";

    public static final String EMAIL = "email";
    public static final String PHONE_NUMBER = "phoneNumber";

    public static final String SESSION_MOBILE_OTP = "SESSION_MOBILE_OTP";
    public static final String SESSION_EMAIL_OTP = "SESSION_EMAIL_OTP";
    public static final String SESSION_MOBILE_OTP_EXPIRY = "SESSION_MOBILE_OTP_EXPIRY";
    public static final String SESSION_EMAIL_OTP_EXPIRY = "SESSION_EMAIL_OTP_EXPIRY";
    public static final String VERIFICATION_EXPIRY_CONFIG = "verify-expiry-time";
    public static final String SKIPPABLE = "skippable";
    public static final String VERIFICATION_TYPE = "verification-type";
    public static final String VERIFICATION_TYPE_OTP = "OTP";
    public static final String VERIFICATION_TYPE_OTP_LESS = "OTP_LESS";

    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String EXPIRY_TIME_ATTRIBUTE = "expiry";
    public static final String DEFAULT_MESSAGE_TXT = "Your Verification Code is: {code}";


    //Authenticator FTLS
    public static final String OTP_VERIFICATION_FTL = "otp-verify.ftl";
    public static final String BO_VERIFY_EMAIL_FTL = "bo-verify-email.ftl";
    public static final String BO_VERIFY_PHONE_NUMBER_FTL = "bo-verify-phone.ftl";

    public static final String DETAILS_FOUND_FTL = "details-found.ftl";

    //Config Names & Labels
    public static final String EMAIL_SENDER = "email-sender";
    public static final String SMS_SENDER = "sms-sender";
    public static final String EMAIL_SENDER_LABEL = "email-sender-label";
    public static final String SMS_SENDER_LABEL = "sms-sender-label";
    public static final String EMAIL_VER_OTP_LENGTH = "email_ver_otp_len";
    public static final String EMAIL_VER_OTP_LENGTH_LABEL = "email-verify-otp-label";
    public static final String PHONE_VER_OTP_LENGTH = "sms_ver_otp_len";
    public static final String PHONE_VER_OTP_LENGTH_LABEL = "phone-verify-otp-label";
    public static final String PHONE_VER_OTP_TXT = "sms_ver_otp_txt";


    //Help Texts Constants

    public static final String CIBA_NOTIFY = "ciba-notify";
    public static final String CIBA_EMAIL = "Email";
    public static final String CIBA_WHATSAPP = "Whatsapp";

    public static final String CIBA_PUSH = "Push";
    public static final String CIBA_QR = "QR";

    public static final String FIREBASE_JSON = "firebase-json";
    public static final String HUAWEI_CLIENT_ID = "huawei-client-id";
    public static final String HUAWEI_CLIENT_SECRET = "huawei-client-secret";

    public static final String PUSH_WAY = "push-way";
    public static final String AUTOMATIC_FALLBACK = "automatic-fallback";
    public static final String DEEP_LINKING = "deep-linking";
    public static final String PLAY_STORE_LINK = "play-store-link";
    public static final String APP_STORE_LINK = "app-store-link";

    public static final String IVR_PROVIDER = "ivr-provider";
    public static final String CIBA_TWILIO = "Twilio";
    public static final String TWILIO_PROVIDER = "twilio-provider";
    public static final String TWILIO_DEFAULT = "Default";
    public static final String TWILIO_CUSTOM = "Custom";

    public static final String ATTRIBUTE_MESSAGE = "attrMessage";
    public static final String ATTRIBUTE_MESSAGE_TYPE = "messageType";
    public static final String ATTRIBUTE_MESSAGE_TITLE = "messageTitle";

    public static final String CONNECTION_DETAILS = "connectionDetails";

    //driving license verification
    public static final String DRIVING_LICENSE_PREFILL = "driving_license_number";
    public static final String VERIFY_DL_NUMBER_VERIFIED_CLAIM = "verify_driving_license@driving_license_verified";
    public static final String VERIFY_DL_NUMBER_VERIFIED_AT_CLAIM = "verify_driving_license@driving_license_verified_at";

    public static final String VERIFY_OCR_DRIVING_LICENSE_VERIFIED_CLAIM = "ocr_driving_license@ocr_driving_license_verified";
    public static final String VERIFY_OCR_DRIVING_LICENSE_VERIFIED_AT_CLAIM = "ocr_driving_license@ocr_driving_license_verified_at";
    public static final String VERIFY_DRIVING_LICENSE_OCR_CLAIM = "ocr_driving_license@license_number";

    public static final String DRIVING_LICENSE_PHOTO_FTL = "verify_driving_license_photo.ftl";
    public static final String DRIVING_LICENSE_NUMBER_FTL = "verify_driving_license_no.ftl";


    // Bank Verification
    public static final String BANK_DETAILS_VERIFICATION_FTL = "verify_bank_account.ftl";
    public static final String VERIFY_BANK_ACCOUNT_NUMBER_VERIFIED_CLAIM = "verify_bank@bank_account_number_verified";
    public static final String VERIFY_BANK_ACCOUNT_NUMBER_VERIFIED_AT_CLAIM = "verify_bank@bank_account_number_verified_at";
    public static final String VERIFY_BANK_PENNY_DROP_VERIFIED_CLAIM = "verify_bank@bank_account_penny_drop_verified";
    public static final String VERIFY_BANK_PENNY_DROP_VERIFIED_AT_CLAIM = "verify_bank@bank_account_penny_drop_verified_at";
    public static final String PENNY_DROP_BANK_VALID_CLAIM = "verify_bank@penny_drop_bank_account_valid";
    public static final String BANK_ACCOUNT_NO_VERIFICATION = "bank_account_verification";
    public static final String BANK_ACCOUNT_VERIFICATION_PENNY_DROP = "bank_account_verification_penny_drop";
    public static final String BANK_ACCOUNT_NUMBER = "bank_account_no";
    public static final String IFSC_CODE = "ifsc_code";

    //Voter ID
    public static final String STATUS_CODE = "status_code";
    public static final String MESSAGE = "message";
    public static final String RESPONSE_CODE = "response_code";
    public static final String VOTER_ID_CLAIMS = "voter-id-claims";
    public static final String VOTER_ID_OCR_CLAIMS = "voter-id-ocr-claims";
    public static final String VOTER_ID_CLAIMS_HELPTXT = "voter-id-hlp-txt";
    public static final String VOTER_ID_IMAGE = "voter_id_image";
    public static final String VOTER_ID_OB = "voter_id_ob";
    public static final String VOTER_ID_IMAGE_DETAILS = "image_details";


    public static final String VERIFY_TAN_VERIFIED = "verify_tan@tan_number_verified";
    public static final String VERIFY_TAN_VERIFIED_AT = "verify_tan@tan_number_verified_at";
    public static final String TAN_NUMBER_FTL = "verify_tan_no.ftl";


    public static final String BUSINESS_VERIFICATION_FTL = "verify_business.ftl";
    public static final String BUSINESS_VERIFICATION = "business_verification";
    public static final String BUSINESS_VERIFICATION_CIN_NUMBER_VERIFIED_CLAIM = "verify_business@cin_number_verified";
    public static final String BUSINESS_VERIFICATION_CIN_NUMBER_VERIFIED_AT_CLAIM = "verify_business@cin_number_verified_at";

    public static final String VERIFY_GSTIN_VERIFIED_CLAIM = "verify_gstin@gstin_verified";
    public static final String VERIFY_GSTIN_VERIFIED_AT_CLAIM = "verify_gstin@gstin_verified_at";
    public static final String VERIFY_GSTIN_OTP_VERIFIED_CLAIM = "verify_gstwithotp@gstinotp_verified";
    public static final String VERIFY_GSTIN_OTP_VERIFIED_AT_CLAIM = "verify_gstwithotp@gstinotp_verified_at";
    public static final String GSTIN_NUMBER_FTL = "verify_gstin_no.ftl";
    public static final String GST_OTP_FTL = "verify_gst_otp.ftl";

    //fssai
    public static final String VERIFY_FSSAI_VERIFIED_CLAIM = "verify_fssai@fssai_verified";
    public static final String VERIFY_FSSAI_VERIFIED_AT_CLAIM = "verify_fssai@fssai_verified_at";

    public static final String FSSAI_NUMBER_FTL = "verify_fssai_no.ftl";

    public static final String VERIFY_AADHAAR_CARD_AADHAAR_NUMBER_CLAIM = "verify_aadhaar@aadhaar_number";
    public static final String VERIFY_AADHAAR_CARD_AADHAAR_VERIFIED_CLAIM = "verify_aadhaar@aadhaar_verified";

    public static final String VERIFY_AADHAAR_CARD_AADHAAR_VERIFIED_AT_CLAIM = "verify_aadhaar@aadhaar_verified_at";
    public static final String VERIFY_AADHAR_VERIFIED_AT_OTP_CLAIM = "verify_otp_aadhaar@aadhaar_otp_verified_at";
    public static final String AADHAR_OTP_CLIENT_ID = "aadhaar_client_id";
    public static final String AADHAR_OTP_FTL = "verify_aadhaar_otp.ftl";
    public static final String AADHAR_NUMBER_FTL = "verify_aadhaar_no.ftl";
    public static final String AADHAR_OTP = "aadhaar_number_otp";
    public static final String VERIFY_AADHAR_OTP_CLAIM = "verify_otp_aadhaar@aadhaar_number_otp";
    public static final String VERIFY_AADHAAR_OTP_FAILURE_REASON = "aadhaar_otp_failure_reason";
    public static final String VERIFY_PAN_FAILURE_REASON = "verify_pan_failure_reason";
    public static final String VERIFY_BANK_FAILURE_REASON = "verify_bank_failure_reason";
    public static final String VERIFY_BANK_PENNY_DROP_FAILURE_REASON = "verify_bank_penny_drop_failure_reason";
    public static final String VERIFY_AADHAR_VERIFIED_OTP_CLAIM = "verify_otp_aadhaar@aadhaar_otp_verified";
    public static final String AADHAAR_OCR_NUMBER_PREFILL = "aadhaar_ocr_number";
    public static final String VERIFY_AADHAAR_OCR_VERIFIED_CLAIM = "verify_ocr_aadhaar@aadhaar_ocr_verified";
    public static final String VERIFY_AADHAAR_OCR_NUMBER_CLAIM = "verify_ocr_aadhaar@aadhaar_ocr_number";
    public static final String VERIFY_AADHAAR_OCR_VERIFIED_AT_CLAIM = "verify_ocr_aadhaar@aadhaar_ocr_verified_at";
    public static final String AADHAAR_PHOTO_FTL = "verify_aadhaar_photo.ftl";
    public static final String AADHAAR_NUMBER = "aadhaar_number";
    public static final String AADHAAR_NO = "aadhaar_no";
    public static final String AADHAAR_NUMBER_OCR_FRONT_IMAGE = "ocr_aadhaar_front_image";
    public static final String AADHAAR_NUMBER_OCR_BACK_IMAGE = "ocr_aadhaar_back_image";


    //ITR Compliance
    public static final String VERIFY_TDS206_COMPLIANCE_VERIFIED_CLAIM = "verify-tds206-compliance@tds206_compliance_verified";

    public static final String VERIFY_TDS206_COMPLIANCE_VERIFIED_AT_CLAIM = "verify-tds206-compliance@tds206_compliance_verified_at";

    public static final String TDS206_COMPLIANCE_FTL = "verify_tds206_compliance.ftl";
    public static final String TDS206_COMPLIANCE_VERIFICATION = "tds206-compliance-verification";
    public static final String PAN_NUMBER_PREFILL = "pan_number";


    // Finema Verification
    public static final String THAI_ID_NUMBER_VERIFIED = "thai_id_number_verified";
    public static final String THAI_ID_NUMBER_VERIFIED_AT = "thai_id_number_verified_at";
    public static final String FINEMA_ONBOARDING_FTL = "finema-onboarding.ftl";
    public static final String FINEMA_BACK_CARD_FTL = "finema-back-card-details.ftl";
    public static final String F_ID_NUMBER = "sp@id_number";
    public static final String FINEMA_EXPIRY_TIME = "finema_expiry_time";
    public static final String F_LASER_ID = "laser_id";
    public static final String F_FIRST_NAME = "sp@first_name_th";
    public static final String F_LAST_NAME = "sp@last_name_th";
    public static final String F_BIRTH_DATE = "sp@date_of_birth_en";
    public static final String THAI_ID_NUMBER = "thai_id_number";

    public static final String VERIFY_EPFO_VERIFIED_CLAIM = "verify_epfo@epfo_verified";
    public static final String VERIFY_EPFO_VERIFIED_AT_CLAIM = "verify_epfo@epfo_verified_at";
    public static final String EPFO_UAN_NUMBER_FTL = "verify_epfo.ftl";
    public static final String EPFO_VERIFICATION = "epfo_verification";
    public static final String EPFO_OTP_VERIFICATION = "epfo_otp_verification";
    public static final String UAN_NUMBER_CLAIM = "uan_number";
    public static final String UAN_NUMBER_VERIFIED_CLAIM = "epfo_verified";
    public static final String UAN_NUMBER_VERIFIED_AT_CLAIM = "epfo_verified_at";


    public static final String VERIFY_FORM_26AS_VERIFIED = "verify_form_26as@form_26as_verified";
    public static final String VERIFY_FORM_26AS_VERIFIED_AT = "verify_form_26as@form_26as_verified_at";
    public static final String FORM_26AS_NUMBER_FTL = "verify_form_26as.ftl";
    public static final String FORM_26AS_VERIFIED_CLAIM = "form_26as_verified";
    public static final String FORM_26AS_VERIFIED_AT_CLAIM = "form_26as_verified_at";
    public static final String FORM_26AS_STRIPE_ID = "form_26as";


    // Whatsapp-otp
    public static final String WHATSAPP_NUMBER = "sp@whatsapp_number";
    public static final String WHATSAPP_NUMBER_VERIFIED = "sp@whatsapp_number_verified";
    public static final String WHATSAPP_NUMBER_VERIFIED_AT = "sp@whatsapp_number_verified_at";
    public static final String WHATSAPP_NUMBER_REGION = "sp@whatsapp_number_region";
    public static final String WHATSAPP_VERIFY_EXPIRY_TIME = "whatsapp-verify-expiry-time";
    public static final String WHATSAPP_VERIFY_OTP_LENGTH = "whatsapp-verify-otp-length";
    public static final String WHATSAPP_NUM_WITH_COUNTRY_CODE = "whatsapp-num-with-country-code";
    public static final String WHATSAPP_OTP_EXP_TIME = "whatsapp-otp-exp-time";
    public static final String WHATSAPP_OTP = "whatsapp-otp";


    //ITR Verification
    public static final String ITR_VERIFIED = "sp@itr_verified";
    public static final String VERIFY_ITR_VERIFIED = "verify_itr@itr_verified";
    public static final String VERIFY_ITR_VERIFIED_AT = "verify_itr@itr_verified_at";
    public static final String ITR_NUMBER_FTL = "verify_itr.ftl";


    //Web2Device
    public static final String WEB_TO_DEVICE = "web_to_device";
    public static final String T1_REQUEST_ID = "ik@t1_request_id";

    public static final String SECRET_KEY = "secret-keys";
    public static final String RESULT = "result";
    public static final String MOBILE_SDK = "mobile_sdk";
    public static final String DESKTOP_APP = "desktop_app";
    public static final String KAFKA = "kafka";
    public static final String POLL_INTERVAL = "poll-interval";
    public static final String AUTH_REQ_ID = "auth_req_id";
    public static final String WEB_SDK = "web_sdk";
    public static final String NOTIFICATION_WAY = "notification_way";
    public static final String SEND_REQUEST_FAILED = "Failed to send request to client notification endpoint";
    public static final String NOTIFICATION_ENDPOINT_AUTH = "notification-endpoint-auth";
    public static final String NOTIFICATION_ENDPOINT = "notification-endpoint";
    public static final String OCR_VERIFICATION = "ocr-verification";
    public static final String SELFIE_COMPARE = "selfie-compare";
    public static final String SELFIE_INSTRUCTIONS = "selfie-instructions";
    public static final String PAN_INSTRUCTIONS = "pan-instructions";
    public static final String OCR_PAN_INSTRUCTIONS = "pan-ocr-instructions";
    public static final String SELFIE_COMPARE_INSTRUCTIONS = "selfie-compare-instructions";
    public static final String CAN_USER_TOGGLE_CAMERA_AND_MIC = "can-user-toggle-camera-and-mic";
    public static final String REQUEST_ID = "requestId";
    public static final String HOOK_WAY = "hook-way";
    public static final String QUEUE_WAY = "queue-way";
    public static final String ENCRYPTED = "encrypted";
    public static final String NOTIFICATION_RESPONSE_WAY = "notification-response-way";
    public static final String MAX_DEVICE_COUNT = "max_device_count";
    public static final String ALLOW_MULTI_DEVICE = "allow_multi_device";
    public static final String CRED_ID = "credid";
    public static final String CHALLENGE = "challenge";
    public static final String BIOMETRIC_VERIFIED = "biometric_verified";
    public static final String BIOMETRIC_VERIFIED_AT = "biometric_verified_at";
    public static final String BIOMETRIC_DEVICE = "biometric_device";
    public static final String BIOMETRIC_FAILED_REASON = "biometric_failed_reason";
    public static final String IS_MOBILE = "ismobile";
    public static final String IS_DESKTOP = "isdesktop";
    public static final String PUBLIC_KEY = "publickey";
    public static final String PRIVATE_KEY = "privatekey";
    public static final String DEVICE = "device";
    public static final String NO_DEVICE_RECEIVED = "No device received";
    public static final String BIOMETRIC_MAX_DEVICE_REACHED = "The user has already reached the maximum number of devices registered for biometrics";
    public static final String BUSINESS_VERIFICATION_CLAIMS = "business-verification-claims";
    public static final String DEVICE_REGISTERED = "device_registered";
    public static final String DEVICE_REGISTERED_AT = "device_registered_at";
    public static final String DEVICE_REGISTER_FAILED_REASON = "device_register_failed_reason";
    public static final String DEVICE_VERIFICATION = "device_verification";
    public static final String FIDO_VERIFIED = "fido_verified";
    public static final String FIDO_VERIFIED_AT = "fido_verified_at";
    public static final String NOT_SET = "not-set";
    public static final String FIDO_REGISTERED = "sp@fido_registered";
    public static final String FIDO_REGISTERED_AT = "sp@fido_registered_at";
    public static final String WEB_AUTHN_TITLE_ATTR = "webAuthnTitle";
    public static final String PLATFORM = "platform";
    public static final String VERIFY_IEC_VERIFIED_CLAIM = "iec_verification@iec_verified";
    public static final String VERIFY_IEC_VERIFIED_AT_CLAIM = "iec_verification@iec_verified_at";
    public static final String IEC_VERIFICATION_FTL = "verify_iec_number.ftl";
    public static final String IEC_VERIFIED_CLAIM = "iec_verified";
    public static final String IEC_VERIFIED_AT_CLAIM = "iec_verified_at";
    public static final String MODE = "bo";
    public static final String IEC_NO = "iec_no";
    public static final String IEC_VERIFICATION = "iec_verification";
    public static final String FIDO_ASSET_LINKS_ANDROID = "fido-asset-links-android";
    public static final String FIDO_ASSET_LINKS_IOS = "fido-asset-links-ios";
    public static final String ABSTRACT_API_KEY = "abstract-api-key";
    public static final String TRANSACTION = "transaction";
    public static final String BO_TRAN = "bo_tran";
    public static final String UNKNOWN_USER = "UNKNOWN_USER";
    public static final String USER_LIVELINESS = "user_liveness";
    public static final String SELFIE_REGISTRATION = "selfie_registration";
    public static final String SELFIE_VERIFICATION = "selfie_verification";
    public static final String LIVELINESS_IMAGE_DETAILS = "liveliness_image_details";
    public static final String TITLE = "title";
    public static final String CANCEL = "cancel";
    public static final String RETRY = "retry";
    public static final String LIVELINESS_IMAGE = "liveliness_image";
    public static final String VERIFY_UDAY_VERIFIED_CLAIM = "verify_udyam@udyam_verified";
    public static final String VERIFY_UDAY_VERIFIED_AT_CLAIM = "verify_udyam@udyam_verified_at";
    public static final String UDYAM_NO = "udyam_no";
    public static final String PAN_NUMBER_FTL = "verify_pan_no.ftl";
    public static final String DEFREG = "defReg";
    public static final String WHATSAPP = "whatsapp";
    public static final String VERIFY_WHATSAPP = "verify_whatsapp";
    public static final String VOTER_ID_VERIFICATION = "voter_id_verification";
    public static final String VOTERID_NO = "voterID_no";
    public static final String UDYAM_VERIFICATION = "udyam_verification";
    public static final String STATUS = "status";
    public static final String ERR_MSG = "errorMsg";
    public static final String UPLOAD_A_PROPER_IMAGE = "Upload a proper image";
    public static final String EXPIRY = "EXPIRY";
    public static final String LASER_CODE = "laser_code";
    public static final String TAN_VERIFICATION = "tan_verification";
    public static final String NAME_SIMILARITY_CHECK = "name_similarity_check";
    public static final String STRIPE_VIDEO_KYC = "video_kyc";
    public static final String NAME_SIMILARITY_CHECK_TITLE = "name_similarity_check_title";
    public static final String NAME_SIMILARITY_CHECK_TITLE_HLP_TXT = "name_similarity_check_title_hlp_txt";
    public static final String NAME_SIMILARITY_CHECK_DESC = "name_similarity_check_desc";
    public static final String NAME_SIMILARITY_CHECK_DESC_HLP_TXT = "name_similarity_check_desc_hlp_txt";
    public static final String FIRST_NAME_TO_BE_COMPARED = "first_name_to_be_compared";
    public static final String FIRST_NAME_TO_BE_COMPARED_HLP_TXT = "first_name_to_be_compared_hlp_txt";
    public static final String SECOND_NAME_TO_BE_COMPARED = "second_name_to_be_compared";
    public static final String SECOND_NAME_TO_BE_COMPARED_HLP_TXT = "second_name_to_be_compared_hlp_txt";
    public static final String NAME_SIMILARITY_CHECK_WARN_HLP_TXT = "name_similarity_check_warn_hlp_txt";
    public static final String TAN_CLAIMS = "tan-claims";
    public static final String UDYAM_ADDITIONAL_DETAILS = "udyam-additional-details";
    public static final String VOTER_ID_ADDITIONAL_DEAILS = "voter-id-additional-details";
    public static final String GST_ADDITIONAL_DETAILS = "gst-in-additional-details";
    public static final String FSSAI_ADDITIONAL_DETAILS = "fssai-additional-details";
    public static final String BANK_ADDITIONAL_DETAILS = "bank-additional-details";
    public static final String DRIVING_LICENSE_ADDITIONAL_DETAILS = "driving-license-additional-details";
    public static final String AADHAAR_ADDITIONAL_DETAILS = "aadhaar-additional-details";
    public static final String OCR_AADHAAR_ADDITIONAL_DETAILS = "ocr-aadhaar-additional-details";
    public static final String OCR_PAN_ADDITIONAL_DETAILS = "ocr-pan-additional-details";
    public static final String OCR_DRIVING_LICENSE_ADDITIONAL_DETAILS = "ocr-driving-license-additional-details";
    public static final String OCR_VOTER_ID_ADDITIONAL_DETAILS = "ocr-voter-id-additional-details";
    public static final String BUSINESS_VERIFICATION_ADDITIONAL_DETAILS = "business-verification-additional-details";
    public static final String ITR_VERIFICATION_ADDITIONAL_DETAILS = "itr-verification-additional-details";
    public static final String EPFO_VERIFICATION_ADDITIONAL_DETAILS = "epfo-verification-additional-details";
    public static final String TDS_206_VERIFICATION_ADDITIONAL_DETAILS = "tds-206-verification-additional-details";
    public static final String GST_WITH_OTP_VERIFICATION_ADDITIONAL_DETAILS = "gst-with-otp-verification-additional-details";


    public static final String VERIFY_PHONE_REGION_CLAIM = "verify_phone@phone_region";
    public static final String PHONE_WITH_REGION_CODE = "verify_phone@phone_with_region_code";
    public static final String VERIFY_PHONE_CLAIM = "verify_phone@phone";
    public static final String VERIFY_PHONE_NUMBER_CLAIM = "verify_phone@phone_number";
    public static final String VERIFY_PHONE_DEVICE_MATCH_CLAIM = "verify_phone@device_match";
    public static final String VERIFY_PHONE_REMOTE_ATTR_CLAIM = "verify_phone@remote_addr";

    public static final String VERIFY_PHONE_VERIFIED_CLAIM = "verify_phone@phone_verified";
    public static final String VERIFY_PHONE_VERIFIED_AT_CLAIM = "verify_phone@phone_verified_at";
    public static final String VALIDATE_PHONE_NUMBER = "validate_phone_number";
    public static final String MISSED_CALL_VALIDATION = "missed_call_validation";
    public static final String VERIFY_SMS = "verify_sms";
    public static final String VERIFY_PHONE_NUMBER_OTP_LESS = "verify_phone_number_otp_less";
    public static final String PHONE_NO_VALIDATION_CLAIMS = "phone-no-validation-claims";
    public static final String PAN_NO_VALIDATION = "pan_no_validation";
    public static final String PAN_NO = "pan_no";
    public static final String GSTIN_CLAIMS = "gstin-claims";
    public static final String GST_OTP_VERIFICATION = "gst_otp_verification";
    public static final String TERMS_AND_CONDITION_VER = "terms_and_condition_ver";
    public static final String GST_VERIFICATION = "gst_verification";
    public static final String FSSAI_CLAIMS = "fssai-claims";
    public static final String FSSAI_VERIFICATION = "fssai_verification";
    public static final String CLIENT_ID = "client_id";
    public static final String ASSESSMENT_YEAR = "assessment_year";
    public static final String FORM_26AS_CLAIMS = "form-26as-claims";
    public static final String SELECT_FORM_26AS_YEARS = "select-form-26as-years";
    public static final String CUSTOMIZE_YEARS = "customize-years";
    public static final String KYC_DETAILS = "kyc_details";
    public static final String OTP_VALIDATED = "otp_validated";
    public static final String EPFO_PASSBOOK = "epfo_passbook";
    public static final String EPFO_VERIFICATION_CLAIMS = "epfo-verification-claims";
    public static final String GET_PASSBOOK_DETAILS = "get-passbook-details";
    public static final String GENERATE_OTP = "generate_otp";
    public static final String VERIFY_EMAIL_CLAIM = "verify_email@email";
    public static final String VERIFY_EMAIL_VERIFIED_CLAIM = "verify_email@email_verified";
    public static final String VERIFY_EMAIL_VERIFIED_AT_CLAIM = "verify_email@email_verified_at";
    public static final String EMAIL_VALIDATION_CLAIM = "valid_email";
    public static final String EMAIL_VALIDATION_VERIFIED_CLAIM = "email_valid_verified";
    public static final String EMAIL_VALIDATION_VERIFIED_AT_CLAIM = "email_valid_verified_at";
    public static final String VERIFY_EMAIL = "verify_email";
    public static final String FAILED_HOOK_ATTEMPTS = "failed_hook_attempts";
    public static final String FAILED_HOOK_ATTEMPTS_FOR_DEAD_LETTER = "failed_hook_attempts_for_dead_letter";
    public static final String VALIDATE_EMAIL = "validate_email";

    // Phone Validation
    public static final String PHONE_NO_VALIDATION_CLAIM = "number";
    public static final String PHONE_NO_VALIDATION_VERIFIED_CLAIM = "phone_no_valid_verified";
    public static final String PHONE_NO_VALIDATION_VERIFIED_AT_CLAIM = "phone_no_valid_verified_at";

    //AZURE B2C
    public static final String AZURE_B2C_NEW = "New";
    public static final String AZURE_B2C_ALREADY = "Already have";
    public static final String AZURE_B2C_SETUP = "azure-setup";

    public static final String DEVICE_VERIFIED = "device_verified";
    public static final String DEVICE_VERIFIED_AT = "device_verified_at";

    public static final String MOBILE_ENC_KEY = "mobile-enc-key";
    public static final String DESKTOP_ENC_KEY = "desktop-enc-key";

    // Device connection details
    public static final String IP = "ip";
    public static final String LOCATION = "location";
    public static final String DATE_AND_TIME = "dateAndTime";

    //iProov
    public static final String IPROOV_REGISTERED = "face_registered";
    public static final String IPROOV_REGISTERED_AT = "face_registered_at";
    public static final String IPROOV_VERIFIED = "sp@face_verified";
    public static final String IPROOV_VERIFIED_AT = "sp@face_verified_at";

    public static final String SECRET = "secret";
    public static final String REGISTER_NEW_DEVICE = "register_new_device";

    public static final String OTPLESS_FALLBACK = "otpless-fallback";
    // APPLICATION DETAILS

    public static final String APP_NAME = "appName";
    public static final String APP_ID = "appId";
    public static final String APP_VERSION = "appVersion";
    public static final String APP_URL = "appUrl";
    public static final String APP_TYPE = "appType";
    public static final String APP_PLATFORM = "appPlatform";
    public static final String REALM_ID = "realmId";
    public static final String UPDATE_URL = "updateUrl";

    //iProov
    public static final String IPROOV_REG_ID = "face_register";
    public static final String IPROOV_VER_ID = "face_verification";

    public static final String WHITELIST_HOST_URL = "whitelist_host";
    public static final String CONTENT_SECURITY_POLICY = "contentSecurityPolicy";
    public static final String FRAME_ANCESTORS = "frame-ancestors";

    //Secure Messaging
    public static final String SECURE_MESSAGING = "secure_messaging";

    //PROXIMITY
    public static final String PROXIMITY_VERIFICATION = "proximity_verification";
    public static final String ENABLE_CIBA_REQUEST_ENCRYPTION = "enable_ciba_request_encryption";
    public static final String CIBA_ENCRYPTION_ID = "ciba_encryption_id";

    //MINIO CONSTANTS
    public static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_URL);
    public static final String MINIO_SERVER_API_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_API_URL);
    public static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_ACCESS_KEY);
    public static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_SECRET_KEY);

    public static final String IS_LIVENESS = "isLiveness";
    public static final String SELFIE_FTL = "selfie.ftl";
    public static final String BO_VERIFY_PHONE = "bo-verify-phone";
    public static final String BO_VALIDATE_PHONE = "validate-phone-no";
    public static final String BO_WEB_2_DEVICE = "bo-web-to-device";
    public static final String WEB2DEVICE_APP_ID = "app-id";
    public static final String MISSED_CALL = "MISSED_CALL";
    public static final String RETRY_MISSED_CALL_VERIFICATION = "retryMissedCallVerification";
    public static final String ACCOUNT_SID = "account_sid";
    public static final String AUTH_TOKEN = "auth_token";
    public static final String FROM = "from";
    public static final String NUMBER_VALIDATION = "NUMBER_VALIDATION";
    public static final String DYNAMIC_FORM_VERIFICATION = "dynamic_form_verification";
    public static final String CUSTOM_MESSAGE = "custom_message";
    public static final String TWILIO_ACCOUNT_SID = "accountsid";
    public static final String TWILIO_AUTH_TOKEN = "authtoken";
    public static final String VALIDATOR = "validator";

    public static final String PROMPT_SUCCESS = "prompt_success";
    public static final String PROMPT_COMPLETED_AT = "prompt_completed_at";
    public static final String PROMPT_TITLE = "prompt_title";
    public static final String PROMPT_DESCRIPTION = "prompt_description";
    public static final String PROMPT_OPTIONS = "prompt_options";

    public static final String CUSTOM_MESSAGE_LOGO = "custom-message-logo";
    public static final String CUSTOM_MESSAGE_TITLE = "custom-message-title";
    public static final String CUSTOM_MESSAGE_DESC = "custom-message-desc";

    //bo-verify-pan
    public static final String VERIFY_PAN_NUMBER_CLAIM = "verify_pan@pan_number";
    public static final String VERIFY_PAN_VERIFIED_CLAIM = "verify_pan@pan_verified";
    public static final String VERIFY_PAN_NUMBER_VERIFIED_AT_CLAIM = "verify_pan@pan_verified_at";

    //bo-verify-pan-ocr
    public static final String VERIFY_PAN_OCR_PAN_NUMBER_CLAIM = "verify_pan_ocr@pan_ocr_number";
    public static final String VERIFY_PAN_OCR_PAN_NUMBER_VERIFIED_CLAIM = "verify_pan_ocr@pan_ocr_verified";
    public static final String VERIFY_PAN_OCR_PAN_NUMBER_VERIFIED_AT_CLAIM = "verify_pan_ocr@pan_ocr_verified_at";

    // validate email
    public static final String VALIDATE_EMAIL_EMAIL_CLAIM = "validate_email@valid_email";
    public static final String VALIDATE_EMAIL_EMAIL_VERIFIED_CLAIM = "validate_email@email_valid_verified";
    public static final String VALIDATE_EMAIL_EMAIL_VERIFIED_AT_CLAIM = "validate_email@email_valid_verified_at";

    // validate phone
    public static final String VALIDATE_PHONE_PHONE_CLAIM = "validate_phone_no@valid_phone_no";
    public static final String VALIDATE_PHONE_PHONE_VALID_VERIFIED_CLAIM = "validate_phone_no@phone_no_valid_verified";
    public static final String VALIDATE_PHONE_PHONE_VALID_VERIFIED_AT_CLAIM = "validate_email@phone_no_valid_verified_at";

    // video kyc
    public static final String VIDEO_KYC = "bo-video-kyc";
    public static final String VIDEO_KYC_COMPLETED = "ik@video_kyc_completed";
    public static final String VIDEO_KYC_MEETING_DURATION = "ik@meeting_duration";
    public static final String VIDEO_KYC_VERIFIED_CLAIM = "video_kyc@vkyc_verified";
    public static final String VIDEO_KYC_VERIFIED_AT_CLAIM = "video_kyc@vkyc_verified_at";
    public static final String VIDEO_KYC_SKIPPED_CLAIM = "video_kyc_skipped";
    public static final String VIDEO_KYC_RECORDED_VIDEO_CLAIM = "video_kyc@vkyc_recorded_video";
    public static final String VIDEO_KYC_RECORDED_VIDEO_STATUS_CLAIM = "video_kyc@vkyc_recorded_video_status";
    public static final String VIDEO_KYC_ID_NUMBER = "video_kyc@vkyc_id_number";
    public static final String VIDEO_KYC_FATHER_NAME = "video_kyc@vkyc_father_name";
    public static final String VIDEO_KYC_DOB = "video_kyc@vkyc_dob";
    public static final String VIDEO_KYC_FULL_NAME = "video_kyc@vkyc_full_name";
    public static final String VIDEO_KYC_GENDER = "video_kyc@vkyc_gender";
    public static final String VIDEO_KYC_ID_TYPE = "video_kyc@vkyc_id_type";
    public static final String VIDEO_KYC_FRONT_OCR_IMAGE = "video_kyc@vkyc_front_ocr_image";
    public static final String VIDEO_KYC_BACK_OCR_IMAGE = "video_kyc@vkyc_back_ocr_image";
    public static final String VIDEO_KYC_SELFIE_IMAGE = "video_kyc@vkyc_selfie_image";
    public static final String VIDEO_KYC_SELFIE_VERIFICATION_STATUS = "video_kyc@vkyc_selfie_verification_status";
    public static final String VIDEO_KYC_PAN_VERIFICATION_STATUS = "video_kyc@vkyc_pan_verification_status";

    public static final String VIDEO_KYC_VERIFICATION_STATUS = "video_kyc@vkyc_verification_status";


    // REALM ATTRIBUTES
    public static final String VIDEO_SDK_ENDPOINT = "video-sdk-endpoint";
    public static final String VIDEO_SDK_API_KEY = "video-sdk-api-key";
    public static final String VIDEO_SDK_SECRET_KEY = "video-sdk-secret-key";
    public static final String VIDEO_SDK_CONCURRENCY = "video-sdk-concurrency";
    public static final String VIDEO_SDK_CURRENT_MEETINGS = "video-sdk-current-meetings";

    public static final Map<String, String> skipDetails = new HashMap<>() {{
        put("User Action", "user_skipped");
    }};

    // skip variables
    public static final String VERIFY_BUSINESS_SKIPPED = "verify_business_skipped";
    public static final String PAN_AADHAAR_LINK_STATUS_SKIPPED = "pan_aadhaar_link_status_skipped";
    public static final String IEC_VERIFICATION_SKIPPED = "iec_verification_skipped";
    public static final String FACE_REGISTER_SKIPPED = "face_register_skipped";
    public static final String FACE_VERIFY_SKIPPED = "face_verify_skipped";
    public static final String VERIFY_ITR_SKIPPED = "verify_itr_skipped";
    public static final String VERIFY_OCR_AADHAAR_SKIPPED = "verify_ocr_aadhaar_skipped";
    public static final String OCR_DRIVING_LICENSE_SKIPPED = "ocr_driving_license_skipped";
    public static final String VERIFY_PAN_OCR_SKIPPED = "verify_pan_ocr_skipped";
    public static final String VERIFY_OCR_VOTERID_SKIPPED = "verify_ocr_voterID_skipped";
    public static final String SELFIE_REGISTRATION_SKIPPED = "selfie_registration_skipped";
    public static final String SELFIE_VERIFICATION_SKIPPED = "selfie_verification_skipped";
    public static final String TERMS_AND_CONDITIONS_SKIPPED = "terms_and_conditions_skipped";
    public static final String VALIDATE_EMAIL_SKIPPED = "validate_email_skipped";
    public static final String VALIDATE_PHONE_NO_SKIPPED = "validate_phone_no_skipped";
    public static final String VERIFY_AADHAAR_SKIPPED = "verify_aadhaar_skipped";
    public static final String VERIFY_OTP_AADHAAR_SKIPPED = "verify_otp_aadhaar_skipped";
    public static final String VERIFY_BANK_SKIPPED = "verify_bank_skipped";
    public static final String VERIFY_DRIVING_LICENSE_SKIPPED = "verify_driving_license_skipped";
    public static final String VERIFY_EMAIL_SKIPPED = "verify_email_skipped";
    public static final String VERIFY_EPFO_SKIPPED = "verify_epfo_skipped";
    public static final String VERIFY_FORM_26AS_SKIPPED = "verify_form_26as_skipped";
    public static final String VERIFY_FSSAI_SKIPPED = "verify_fssai_skipped";
    public static final String VERIFY_GSTIN_SKIPPED = "verify_gstin_skipped";
    public static final String VERIFY_GSTWITHOTP_SKIPPED = "verify_gstwithotp_skipped";
    public static final String VERIFY_PAN_SKIPPED = "verify_pan_skipped";
    public static final String VERIFY_PHONE_SKIPPED = "verify_phone_skipped";
    public static final String VERIFY_TAN_SKIPPED = "verify_tan_skipped";
    public static final String VERIFY_TDS206_COMPLIANCE_SKIPPED = "verify_tds206_compliance_skipped";
    public static final String VERIFY_UDYAM_SKIPPED = "verify_udyam_skipped";
    public static final String VERIFY_VOTERID_SKIPPED = "verify_voterID_skipped";
    public static final String VERIFY_WHATSAPP_SKIPPED = "verify_whatsapp_skipped";
    public static final String PAN_AADHAAR_LINK_STATUS_VERIFIED = "pan_aadhaar_link_status@pan_aadhaar_link_status_verified";
    public static final String PAN_AADHAAR_LINK_STATUS_VERIFIED_AT = "pan_aadhaar_link_status@pan_aadhaar_link_status_verified_at";
    public static final String PAN_AADHAAR_LINK_STATUS_DESC = "pan_aadhaar_link_status@desc";
    public static final String SP_ERROR_CODE = "sp@error_code";

    // GEO TAGGING

    public static final String GEO_TAGGING = "geo_tagging";
    public static final String SELFIE_REG_LATITUDE = "selfie_registration_latitude";
    public static final String SELFIE_REG_LONGITUDE = "selfie_registration_longitude";
    public static final String SELFIE_REG_ACCURACY = "selfie_registration_accuracy";
    public static final String SELFIE_REG_DEVICE_TYPE = "selfie_registration_device_type";
    public static final String SELFIE_VER_LATITUDE = "selfie_verification_latitude";
    public static final String SELFIE_VER_LONGITUDE = "selfie_verification_longitude";
    public static final String SELFIE_VER_ACCURACY = "selfie_verification_accuracy";
    public static final String SELFIE_VER_DEVICE_TYPE = "selfie_verification_device_type";
    public static final String GEO_TAGGING_VERIFIED_AT = "geo_tagging_verified_at";
    public static final String GEO_TAGGING_VERIFIED = "geo_tagging_verified";

    public static final String GEO_TAGGING_FTL = "geo-tagging.ftl";
    public static final String GEO_TAGGING_LATITUDE = "geo_tagging_latitude";
    public static final String GEO_TAGGING_RETRY = "geo_tagging_retry";

    public static final String GEO_TAGGING_LONGITUDE = "geo_tagging_longitude";
    public static final String GEO_TAGGING_ACCURACY = "geo_tagging_accuracy";
    public static final String GEO_TAGGING_DEVICE_TYPE = "geo_tagging_device_type";
    public static final String GEO_TAGGING_LATITUDE_HLP_TXT = "geo-tagging-latitude-hlptxt";
    public static final String GEO_TAGGING_LONGITUDE_HLP_TXT = "geo-tagging-longitude-hlptxt";
    public static final String GEO_TAGGING_ACCURACY_HLP_TXT = "geo-tagging-accuracy-hlptxt";
    public static final String GEO_TAGGING_DEVICE_TYPE_HLP_TXT = "geo-tagging-device-type-hlptxt";
    public static final String SELFIE_DEVICE_TYPE_HLP_TXT = "selfie-device-type-hlptxt";
    public static final String ENABLE_SELFIE_REG_GEO_TAGGING = "enable_selfie_reg_geo_tagging";
    public static final String SELFIE_LATITUDE = "selfie_latitude";
    public static final String SELFIE_LONGITUDE = "selfie_longitude";
    public static final String SELFIE_ACCURACY = "selfie_accuracy";
    public static final String SELFIE_DEVICE_TYPE = "selfie_device_type";
    public static final String ENABLE_SELFIE_VER_GEO_TAGGING = "enable_selfie_ver_geo_tagging";
    public static final String DYNAMIC_FORM_JSON_SCRIPT = "json-script";
    public static final String DYNAMIC_FORM_TITLE = "form-title";
    public static final String TWILIO_SID = "twilio-sid";
    public static final String TWILIO_TRANSACTION_TEMPLATE_SID = "twilio-transaction-template-sid";
    public static final String TWILIO_TRANSACTION_TEMPLATE_SID_HLP_TXT = "twilio-transaction-template-sid-hlp-txt";
    public static final String TWILIO_OTP_TEMPLATE_SID = "twilio-otp-template-sid";
    public static final String TWILIO_OTP_TEMPLATE_SID_HLP_TXT = "twilio-otp-template-sid-hlp-txt";
    public static final String API_ERROR_MESSAGE = "error_message";
    public static final String API_ERROR_CODE = "error_code";
    public static final String BACKOFFICE_ERROR_FTL = "backoffice-error.ftl";
    public static final String IS_API_WORKSPACE = "isApiWorkspace";
    public static final String IS_PLATFORM_WORKSPACE = "isPlatformWorkspace";
    public static final String WORKSPACE_NOT_FOUND = "Workspace not found";
}
