<#-- Mobile MFA Otp Verification  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="otp-verification-form" action="${url.loginAction}" class="row" method="post">
            <input id="resendOTP" name="resendOTP" type="hidden" value="false">
            <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
            <#if mfaProviderId??>
                <input id="mfaProviderId" name="mfaProviderId" type="hidden" value="${mfaProviderId}">
            </#if>
            <div class="col-12">
                <div class="title_sign mb-2">
                    <#if pageType??>
                        <h2>${msg("loginTotpHeader")}</h2>
                    <#else>
                        <h2>${msg('${title}')}</h2>
                    </#if>
                <div>
                    <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
                </div>
                    <label class="mt-2">${msg("wp_otp_sent")}</label>
                </div>
            </div>
            <div class="col-12">
                <div class="form-group">
                    <label>${msg("wp_otp")}</label>
                    <input oninput="preventText(this);disableOTPSubmitBtn(${expiry});" id="inputOTP" name="otp" type="text" class="form-control" placeholder="${msg("enter-your-otp")}"
                           required maxlength="${otp_len!''}">
                    <#import "error_msg.ftl" as e>
                    <@e.error_msg />
                </div>
                <#if expiry??>
                    <div class="form-group mt-4">
                        <label id="otplive">
                            ${msg("mfa_mobile_otp_expires_in_msg")}  <span class="font-w-500 c-primary" id="expirytime"></span>
                        </label>
                        <label class="input-alert-text-error font-w-400" id="otpExpired" >
                            ${msg("mfa_mobile_otp_expired_msg")}
                        </label>
                    </div>
                </#if>
            </div>

            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 btn_account bg-orange-red" disabled="disabled" >
                    ${msg("otp_ver_btn")}
                </button>
            </div>
            <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                ${msg("mfa_mobile_otp_dint_received_msg")}
                <a onclick="resendOTPFun()" target="_blank" class="action-link-clr cursor-pointer">
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
            <div class="w-100 text-center mt-3">
                <img src="${url.resourcesPath}/img/twilio_powered.png" class="form-provider-img" alt="twilio-powered" >
            </div>
            <#if isFromReg??>
                <hr/>
                <#if realm.password && registrationEnabled && !registrationDisabled?? && !isFromReg>
                    <#import "create-account-link.ftl" as c>
                    <@c.create_account_link />
                    <#import "social-login.ftl" as s>
                    <@s.social_login />
                </#if >
                <#if isFromReg>
                    <#import "login-account-link.ftl" as l>
                    <@l.login_account_link />
                </#if>
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