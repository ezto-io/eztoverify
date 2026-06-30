<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "head">
        <script defer src="/auth/assets/js/otpTemplate.min.js?version=${serverVersion}" type="text/javascript"></script>
    </#if>
    <#if section = "form">
        <form id="otp-verification-form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post">
            <div class="row">
                <#assign title = "gst_otp_ver_title">
                <#assign helperText = "gst_otp_ver_description">
                <#assign updateHelperText = "update-gst-number-description">
                <#assign VerifyItemTitle = "gst_no" >
                <#assign VerifyItemvalue = gst_no >
                <#assign maxLength = "6">
                <#--  Template Hidden Fields  -->
                <input name="gst_verification" type="hidden" value="true">
                <input id="gstNo" name="gst_no" type="hidden" value="${gst_no}">
                <input id="edit" name="gst_edit" type="hidden" value="false">
                <input id="resendOTP" name="gst_resend" type="hidden" value="false">
                <input name="cancel" type="hidden" id="cancel-flow" value="false" />

                <#--  Template Header  -->
                <div class="col-12 mt-2" id="template-header">
                    <h2 class="ciba-template-title mb-1">${msg(title)}</h2>
                    <p class="mt-1 ciba-template-description">${msg(helperText)} ${msg(VerifyItemTitle)}  (<b>${VerifyItemvalue}</b>). ${msg(updateHelperText)} <span class="c-blue cursor-pointer font-w-500" onclick="editFunction()">${msg('here')}</span></p>
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
                        <img src="${url.resourcesPath}/img/error-icon.svg" alt="i" class="my-2" />
                        <div class="mx-2 pb-1">
                            <#import "error_msg.ftl" as e>
                            <@e.error_msg />
                        </div>
                    </div>
                </div>
            </div>

            <#--  Template Buttons  -->
            <div class="row">
                <#--  Mobile View  -->
                <div class="col-12 mt-3 display-mobile-only" id="template-button-mobile">
                    <div id="cancelBtn-mobile" onclick="transactionCancel()" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                    <button id="nextBtn-mobile" type="submit" disabled class="w-100 py-3 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
                </div>
                <#--  Web View  -->
                <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                    <div id="empty-skip-fallback"></div>
                    <div class="d-flex justify-content-end align-items-center">
                        <div id="cancelBtn" onclick="transactionCancel()" class="w-100 px-3 py-2 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                        <button id="nextBtn" type="submit" disabled class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
                    </div>
                </div>
            </div>
        </form>
        <script>
            function resendOTPFun() {
                $("#resendOTP").val("true");
                $("#edit").val("false");
                $("#gstNo").val("${gst_no}");
                $("#otp-verification-form").submit();
            }
            $( document ).ready(function() {
                <#if expiry??>
                    $('#otpExpired').hide();
                    otpExpiryCounter(${expiry});
                </#if>
            })

            function transactionCancel() {
                $("#cancel-flow").val("true");
                $('form').submit();
            }

            <#if error??  >
                $('#template-error').css('display', 'block');
            </#if>
        </script>
    </#if>
</@layout.registrationLayout>