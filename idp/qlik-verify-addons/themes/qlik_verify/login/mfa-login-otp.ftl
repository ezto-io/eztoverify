<#-- OTP verification in Login Form  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="otp-login-form" action="${url.loginAction}" class="row" method="post">
            <input name="isVerifyOtpForm" value="true" type="hidden">
            <input name="editEmail" id="editEmail" value="false" type="hidden">
            <input name="editableEmail" id="editableEmail" type="hidden" value="">
            <input id="resendOTP" name="resendOTP" value="false" type="hidden">
            <#if chosenType??>
                <input id="chosenType" name="chosenType" value="${chosenType}" type="hidden">
            </#if>
            <div class="col-12">
                <div class="title_sign">
                    <h2>${msg("otp_ver_title")}</h2>
                </div>
            </div>
            <#if toAddEmail??>
                 <div class="col-12">
                    <div id="divEmail" class="form-group">
                        <label>${msg("otp_ver_email_title")}</label>
                        <div style="position: relative">
                            <input name="isEmailVerifyOtpForm" value="true" type="hidden">
                            <input name="formEmail" id="formEmail" value="${toAddEmail}" type="hidden">
                            <input name="email" type="email" class="form-control" value="${toAddEmail}" disabled>
                            <a id="editInsideInput" onclick="editEmailAddress()" target="_blank" class="action-link-clr">
                                ${msg("email-edit-title")}
                            </a>
                        </div>
                    </div>
                </div>
            </#if>
            <#if maskedEmail??>
                <div class="col-12">
                    <div id="divEmail" class="form-group">
                        <div style="position: relative">
                            <div class="mb-3" style="background-color: #D2FFD2">
                                <p class="mb-0" style="padding: 5px 15px 5px 15px; font-size: 15px">${msg("verification-email-otp-sent",maskedEmail)}</p>
                            </div>
                            <style>
                                #divEmail {
                                    margin-bottom: 0.5rem !important;
                                }
                            </style>
                        </div>
                    </div>
                </div>
            </#if>
            <#if maskedMobile??>
                <div class="col-12">
                    <div id="divMobile" class="form-group">
                        <div style="position: relative">
                            <div class="mb-3" style="background-color: #D2FFD2">
                                <p class="mb-0" style="padding: 5px 15px 5px 15px; font-size: 15px">${msg("verification-mobile-otp-sent",maskedMobile)}</p>
                            </div>
                            <style>
                                #divMobile {
                                    margin-bottom: 0.5rem !important;
                                }
                            </style>
                        </div>
                    </div>
                </div>
            </#if>
            <div class="col-12">
                <div class="form-group">
                    <label>${msg("otp")}</label>
                    <input id="inputOTP" name="otp" type="text" class="form-control" placeholder="${msg("enter-your-otp")}" oninput="disableOTPSubmitBtn()">
                    <#import "error_msg.ftl" as e>
                    <@e.error_msg />
                </div>
                <#if expiry??>
                    <div class="form-group mb-0" >
                        <label class="mb-0" id="otplive">
                            ${msg("otp_expires_in_msg")}  <span class="font-w-500 c-primary" id="expirytime"></span>
                        </label>
                        <label class="mb-0 input-alert-text-error font-w-400" id="otpExpired" >
                            ${msg("otp_expired_msg")} 
                        </label>
                    </div>
                </#if>
            </div>

            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red   cursor-unset" disabled="disabled">
                    ${msg("otp_ver_btn")}
                </button>
            </div>
            <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                ${msg("otp-not-received-msg")} 
                <a onclick="resendOTPFun()" target="_blank" class="action-link-clr cursor-pointer">
                  ${msg("otp_resend_btn")}
                </a>
            </p>
        </form>
        <script>
            function resendOTPFun(){
                $("#resendOTP").val("true"); 
                $("#otp-login-form").submit();
            }
            function disableOTPSubmitBtn(){
                $("#be-error").remove();
                let conf = $("#inputOTP").val();
                if (!conf) {
                    $('#nextBtn').addClass('disabled cursor-unset');
                    $('#nextBtn').attr('disabled', 'disabled');
                } else {
                    $('#nextBtn').removeClass('disabled cursor-unset');
                    $('#nextBtn').removeAttr('disabled');
                }
            }
            function editEmailAddress(){
                $("#editEmail").val("true");
                <#if toAddEmail??>
                    $("#editableEmail").val("${toAddEmail}");
                </#if>
                $("#otp-login-form").submit();
            }
            $( document ).ready(function() {
                <#if expiry??>
                    $('#otpExpired').hide();
                    otpExpiryCounter(${expiry});
                </#if>
            })
        </script>
    </#if>
</@layout.registrationLayout>