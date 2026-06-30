<#-- OTP verification in Registration Form  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <h2 class="ciba-template-title text-break text-center mt-2">${msg(title)}</h2>
        <p class="ciba-template-subtitle text-dark text-break mt-4 text-center" > ${msg(title)} ${msg("verification-loading-hlp-txt")} </p>
        <form id="details-found-form" action="${url.loginAction}" class="${properties.kcFormClass!}" method="post">
            <input name="details_found" type="hidden" value="true">
             <#if driving_license_number?? && driving_license_dob??>
                <input name="driving_license_no" type="hidden" value="${driving_license_number}">
                <input name="driving_license_dob" type="hidden" value="${driving_license_dob}">
            </#if>

            <#if pan_number??>
                <input name="pan_no" type="hidden" value="${pan_number}">
            </#if>

            <#if epic_number??>
                <input name="voterID_no" type="hidden" value="${epic_number}">
            </#if>

             <#if aadhaar_ocr_number??>
                <input name="aadhaar_no" type="hidden" value="${aadhaar_ocr_number}">
            </#if>

            <#if aadhaar_number_otp??>
                <input name="aadhaar_no" type="hidden" value="${aadhaar_number_otp}">
            </#if>
        </form>
        <script defer type="text/javascript">
            $(document).ready(function () {
                setTimeout(() => $("#details-found-form").submit() , 3000);
            });
        </script>
        <div class="d-flex justify-content-center align-items-center py-5">
            <div class="spinner-border branding-primary-color" role="status">
                <span class="sr-only">${msg("loading")}...</span>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>