package org.keycloak.representations.idm.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaDataRepresentation {
    private String aadhaar_e_signature;
    private String aadhaar_card_verification;
    private String aadhaar_no_validation;
    private String aadhaar_no_verification;
    private String bank_account_verification;
    private String bank_account_verification_penny_drop;
    private String business_verification;
    private String biometric_register;
    private String biometric_verify;

    private String consider;
    private String dl_card_verification;
    private String dl_verification;
//    private String epfo_passbook;
    private String fido_register;
    private String fido_verification;
    private String form_26as;
    private String fssai_verification;
    private String gst_otp_verification;
    private String gst_verification;
    private String iec_verification;
    private String invoice_verification;
    private String itr_verification;
    private String pan_card_validation;
    private String pan_no_validation;
    private String passport_verification;
    private String stage;
    private String tan_verification;
    private String tds_206_compliance;
    private String terms_and_conditions;
    private String transaction;
    private String udyam_verification;
    private String udyog_verification;
    private String user_liveness;
    private String validate_email;
    private String validate_phone_number;
    private String verify_email;
    private String verify_sms;
    private String verify_whatsapp;
    private String volume;
    private String voter_card_verification;
    private String voter_id_verification;
    private String face_register;
    private String face_verification;
    private String device_register;
    private String device_verification;
    private String proximity_verification;
    private String secure_messaging;
    private String selfie_registration;
    private String selfie_verification;
    private String uae_pass_verification;
    private String dynamic_form_verification;
    private String ezto_pass_verification;
    private String missed_call_validation;
    private String api_request;
    private String custom_response;
    private String custom_message;
    private String sub_workflow;
    private String number_prompt;
    private String digilocker;
    private String pan_aadhaar_link_status;
    private String name_similarity_check;
    private String geo_tagging;
    private String video_kyc;
}
