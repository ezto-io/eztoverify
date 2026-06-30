<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "head">
        <script defer src="/auth/assets/js/otpTemplate.min.js?version=${serverVersion}" type="text/javascript"></script>
    </#if>
    <#if section = "form">
        <form id="otp-verification-form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post">
            <div class="row">
                <#assign title = "aadhaar_otp_ver_title">
                <#assign helperText = "aadhaar_otp_ver_description">
                <#assign VerifyItemTitle = "aadhaar_no" >
                <#assign VerifyItemvalue = "">
                <#assign maxLength = "10">
                <#--  Template Hidden Fields  -->
                <#if hiddenFields?? >
                    <#list hiddenFields as field>
                        <input id="${field.fieldName}" name="${field.fieldName}" type="hidden" value="${field.fieldValue}"  />
                    </#list>
                </#if>

                <#--  Template Header  -->
                <div class="col-12 mt-2" id="template-header">
                    <h2 class="ciba-template-title mb-1">${msg(title)}</h2>
                    <p class="mt-1 ciba-template-description">${msg(helperText)} ${msg(VerifyItemTitle)}  (<b>${VerifyItemvalue}</b>). ${msg('enter-otp-description')}</p>
                </div>

                <#--  Template Input Form Fields  -->
                <div class="col-12 mb-1" id="template-form-fields">
                    <div class="form-group mb-0">
                        <div class="d-flex justify-content-between align-items-center mt-2 mb-1">
                            <label class="ciba-template-input-label">${msg("otp")}<span class="text-danger"> *</span></label>
                            <label class="ciba-template-input-label">
                                <#if expiry??>
                                    <span id="otplive">${msg('otp-expires-in')} <span class="c-blue" id="expirytime"></span></span>
                                </#if>
                                <span id="otpExpired">${msg('otp-expired-click')}</span>
                            </label>                        
                        </div>
                        <input id="otp" name="otp" type="text" class="form-control ciba-template-input-label py-0" maxlength="${maxLength}" oninput="validateOtp()" onpaste="validateOtp()" placeholder="${msg('enter-your-otp')}" autofocus required />
                        <p class="mt-2 ciba-template-description text-dark">${msg('code-not-received')} <span class="c-blue cursor-pointer" onclick="resendOTPFun()">${msg('resend')}</span></p>
                    </div>
                </div>

                <#--  Template Error  -->
                <div class="col-12" id="template-error" style="display:none">
                    <div class="d-flex align-items-center">
                        <img src="/auth/assets/img/error-icon.svg?version=${serverVersion}" alt="i" class="my-2" />
                        <div class="mx-2 pb-1">
                            <#import "error_msg.ftl" as e>
                            <@e.error_msg />
                        </div>
                    </div>
                </div>
            </div>

            
            <div class="row mt-3">
                <#--  Template Buttons  -->
                <div class="col-12 d-flex mt-3" id="template-button">
                    <div id="cancelBtn" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center">${msg("doCancel")}</div>
                    <button id="nextBtn" type="submit" disabled class="w-100 py-3 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
                </div>  
                <div class="col-12 d-flex justify-content-center mt-4">    
                    <p class="ciba-template-description text-dark font-500 mb-0"> ${msg('wrong')} ${msg(VerifyItemTitle)}? <span class="c-blue cursor-pointer" onclick="editFunction()">${msg('update')}</span></p>
                </div>
            </div>
        </form>
        <script>
            $(document).ready(function () {
                <#if expiry??>
                    $('#otpExpired').hide();
                    otpExpiryCounter(${expiry});
                </#if>
                <#if error??>
                    $('#template-error').css('display', 'block');
                </#if>
            })
        </script>
    </#if>
</@layout.registrationLayout>