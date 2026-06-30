<#-- OTP verification in Mul Login Form  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="otp-login-form" action="${url.loginAction}" class="row" method="post">
            <input name="isLoginOtpForm" value="true" type="hidden">
            <input id="edit" name="edit" value="false" type="hidden">
            <input id="resendOTP" name="resendOTP" value="false" type="hidden">
            <input id="chosenType" name="chosenType" value="${chosenType}" type="hidden">
            <div class="col-12">
                <div class="title_sign">
                    <h2>${msg("otp_ver_title")}</h2>
                </div>
            </div>
            <#if email??>
                <div class="col-12">
                    <#if !message??>
                        <#if additionalText??>
                            <div class="mb-3" style="background-color: #D2FFD2">
                                <p class="mb-0" style="padding: 5px 15px 5px 15px; font-size: 15px">${msg('${msg(additionalText)}')} ${maskedPhoneNumber}</p>
                            </div>
                            <style>
                                #divEmail {
                                    margin-bottom: 0.5rem !important;
                                }
                            </style>
                        </#if>
                    </#if>
                    <div id="divEmail" class="form-group">
                        <label>${msg("otp_ver_email_title")}</label>
                        <div style="position: relative">
                            <input name="email" type="email" class="form-control" value="${email}" disabled>
                            <a href="${url.loginRestartFlowUrl}">
                                <i class="tio editBtn cursor-pointer">edit</i>
                            </a>
                        </div>
                    </div>
                </div>
            </#if>
            <#if phoneNumber??>
                <div class="col-12">
                    <#if !message??>
                        <#if additionalText??>
                            <div class="mb-3" style="background-color: #D2FFD2">
                                <p class="mb-0" style="padding: 5px 15px 5px 15px; font-size: 15px">${msg('${additionalText}')} ${maskedPhoneNumber}</p>
                            </div>
                            <style>
                                #divEmail {
                                    margin-bottom: 0.5rem !important;
                                }
                            </style>
                        </#if>
                    </#if>
                    <div class="form-group">
                        <label>${msg("otp_ver_mob_num_title")}</label>
                        <div style="position: relative">
                            <input name="phoneNumber" type="tel" class="form-control" value="${phoneNumber}" disabled>
                            <a href="${url.loginRestartFlowUrl}">
                                <i class="tio editBtn cursor-pointer">edit</i>
                            </a>
                        </div>
                    </div>
                </div>
            </#if>
            <#if username??>
                <div class="col-12">
                    <#if !message??>
                        <#if additionalText??>
                            <div class="mb-3" style="background-color: #D2FFD2">
                                <p class="mb-0" style="padding: 5px 15px 5px 15px; font-size: 15px">${msg('${additionalText}')} ${maskedPhoneNumber}</p>
                            </div>
                            <style>
                                #divEmail {
                                    margin-bottom: 0.5rem !important;
                                }
                            </style>
                        </#if>
                    </#if>
                    <div class="form-group">
                        <label>${msg("otp_ver_username_title")}</label>
                        <div style="position: relative">
                            <input name="username" type="text" class="form-control" value="${username}" disabled>
                            <a href="${url.loginRestartFlowUrl}">
                                <i class="tio editBtn cursor-pointer">edit</i>
                            </a>
                        </div>
                    </div> 
                </div>
            </#if>
            <div class="col-12">
                <div class="form-group">
                    <#if difOTP?? && expiry??>
                        <label>${msg('email-otp')}</label> <span class="text-danger"> *</span>
                        <input id="inputEmailOTP" name="emailotp" type="text" class="form-control otp-control" placeholder="${msg('enter-email-otp')}" oninput="disableOTPSubmitBtn(${expiry})">
                        <label>${msg('mobile-otp')}</label> <span class="text-danger"> *</span>
                        <input id="inputMobileOTP" name="mobileotp" type="text" class="form-control otp-control" placeholder="${msg('enter-mobile-otp')}" oninput="disableOTPSubmitBtn(${expiry})">
                        <#else>
                            <#if expiry??>
                                <label>${msg("otp")}</label> <span class="text-danger"> *</span>
                                <input id="inputOTP" name="otp" type="text" class="form-control otp-control" placeholder="${msg('enter-your-otp')}" oninput=" preventText(this);disableOTPSubmitBtn(${expiry});" required maxlength="${otp_len!''}">
                            </#if>
                    </#if>
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
                ${msg("otp_dint_received_msg")}
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
            $( document ).ready(function() {
                <#if expiry??>
                    $('#otpExpired').hide();
                    otpExpiryCounter(${expiry});
                </#if>
            })
        </script>
    </#if>
</@layout.registrationLayout>