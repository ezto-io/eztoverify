<#-- Email MFA Verification  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="otp-verification-form" action="${url.loginAction}" class="row" method="post">
            <input name="isOtpVerification" type="hidden" value="true">
            <input id="edit" name="edit" type="hidden" value="false">
            <input id="resendOTP" name="resendOTP" type="hidden" value="false">
            <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
            <#if mfaProviderId??>
                <input id="mfaProviderId" name="mfaProviderId" type="hidden" value="${mfaProviderId}">
            </#if>
            <div class="col-12">
                <div class="title_sign">
                    <#if clientName??>
                        <h2>${msg("loginTotpHeader")}</h2>
                        <div>
                            <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
                        </div>
                    <#else>
                        <h2>${msg("otp_ver_title")}</h2>
                    </#if>
                    <label class="mt-2">${msg("otp_mail_ver_desc")}</label>
                </div>
            </div>
            <div class="col-12">
                <div class="form-group">
                    <label>${msg("otp")}</label>
                    <input oninput="disableOTPSubmitBtn(${expiry})" id="inputOTP" name="otp" type="text" class="form-control" placeholder="${msg('enter-your-otp')}"
                           required>
                    <#import "error_msg.ftl" as e>
                    <@e.error_msg />
                </div>
                <#if expiry??>
                    <div class="form-group mb-0">
                        <label class="mb-0" id="otplive">
                            ${msg("mfa_email_otp_expires_in_msg")}<span class="font-w-500 c-primary" id="expirytime"></span>
                        </label>
                        <label class="mb-0 input-alert-text-error font-w-400" id="otpExpired" >
                           ${msg("mfa_email_otp_expired_msg")}
                        </label>
                    </div>
                </#if>
            </div>

            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  " disabled="disabled" >
                    ${msg("otp_ver_btn")}
                </button>
            </div>
            <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                ${msg("mfa_email_otp_dint_received_msg")}
                <a onclick="resendOTPFun()" class="action-link-clr cursor-pointer">
                  ${msg("otp_resend_btn")}
                </a>
            </p>
            <#if showTroubleShoot?? && showTroubleShoot>
                                                                        <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                                                                            <a onclick="troubleShootMFAFunc()" class="action-link-clr cursor-pointer">
                                                                            ${msg("troubleshoot_mfa")}
                                                                            </a>
                                                                        </p>
                                                                </#if>
        </form>
        <script>
            function troubleShootMFAFunc(){                                                             
                 $("#resendOTP").val("false");   
                 $("#troubleShootMFA").val("true");
                 $("#otp-verification-form").submit();
            }
            function resendOTPFun() {
                $("#troubleShootMFA").val("false");
                $("#resendOTP").val("true");
                $("#otp-verification-form").submit();
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