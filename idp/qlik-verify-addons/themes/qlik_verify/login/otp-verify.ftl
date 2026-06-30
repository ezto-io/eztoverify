<#-- OTP verification in Registration Form  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
         <#if mode?? && mode == "bo">
            <form onsubmit="handleSubmit()" id="otp-verification-form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post">
                <div class="row">
                    <#--  Template Hidden Fields  -->
                    <input name="isOtpVerification" type="hidden" value="true">
                    <input id="edit" name="edit" type="hidden" value="false">
                    <input id="resendOTP" name="resendOTP" type="hidden" value="false">
                    <input name="cancel" type="hidden" id="cancel-flow" value="false" />
                    <#assign isEditUserDetail ='' />
                    <#if editUserDetail??>
                        <#assign isEditUserDetail = editUserDetail>
                    <#else>
                        <#assign isEditUserDetail = 'true'>
                    </#if>
                    <input id="editUserDetail" name=editUserDetail type="hidden" value=isEditUserDetail>
                    <#if phoneNumberRegion??>
                        <input id="phoneNumberRegion" name="phoneNumberRegion" type="hidden" value="${phoneNumberRegion}">
                    </#if>
                    <#if phoneNumber??>
                        <input id="phoneNumber" name="phoneNumber" type="hidden" value="${phoneNumber}">
                    </#if>

                    <#--  Template Header  -->
                    <div class="col-12 mt-2" id="template-header">
                        <h2 class="ciba-template-title mb-1">${msg("otp_ver_title")}</h2>
                        <#if email??>
                            <p class="mt-1 ciba-template-description">${msg('otp-triggered-to-email')} (<b>${email}</b>). <#if isEditUserDetail == 'true'>${msg('update-email-description')} <span class="c-blue cursor-pointer font-w-500" onclick="editFunction()">${msg('here')}</span></#if></p>
                        </#if>
                        <#if phoneNumberWithCountryCode??>
                            <p class="mt-1 ciba-template-description numbers">${msg('otp-triggered-to-phone-number')} (<b>${phoneNumberWithCountryCode}</b>). <#if isEditUserDetail == 'true'>${msg('update-phone-number-description')} <span class="c-blue cursor-pointer" onclick="editFunction()">${msg('here')}</span></#if></p>
                        </#if>
                        <#if whatsappNumber??>
                            <p class="mt-1 ciba-template-description numbers">${msg('otp-triggered-to-whatsapp-number')} (<b>${whatsappNumber}</b>). <#if isEditUserDetail == 'true'>${msg('update-whatsapp-number-description')} <span class="c-blue cursor-pointer" onclick="editFunction()">${msg('here')}</span></#if></p>
                        </#if>
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
                                </label>
                            </div>
                            <input id="inputOTP" name="otp" type="text" class="form-control ciba-template-input-label py-0"
                                oninput="preventText(this); 
                                    <#if otp_len?? && otp_len?trim?has_content >
                                        disableOTPSubmitBtnIncludingOtpLength(${expiry}, ${otp_len});
                                    <#elseif otp_len?? && !otp_len?trim?has_content >
                                        disableOTPSubmitBtnIncludingOtpLength(${expiry}, '6');
                                    <#else>
                                        disableOTPSubmitBtn(${expiry});
                                    </#if>
                                    $('#template-error').css('display', 'none');" 
                                onpaste="preventText(this); 
                                    <#if otp_len?? && otp_len?trim?has_content >
                                        disableOTPSubmitBtnIncludingOtpLength(${expiry}, ${otp_len});
                                    <#elseif otp_len?? && !otp_len?trim?has_content >
                                        disableOTPSubmitBtnIncludingOtpLength(${expiry}, '6');
                                    <#else>
                                        disableOTPSubmitBtn(${expiry});
                                    </#if>
                                    $('#template-error').css('display', 'none');" 
                                placeholder="${msg('enter-your-otp')}" 
                                maxlength="<#if otp_len?? && otp_len?trim?has_content>${otp_len}<#else>6</#if>" 
                                autofocus 
                                required />
                            <p class="mt-2 ciba-template-description text-dark">${msg('code-not-received')} <span class="c-blue cursor-pointer" onclick="resendOTPFun()">${msg('resend')}</span></p>
                        </div>
                    </div>

                    <#--  Template Error  -->
                        <div class="col-12" id="template-error" style="display:none">
                        <div class="d-flex align-items-center">
                            <img src="/auth/assets/img/error-icon.svg?version=${serverVersion}" alt="i" class="my-2" />
                            <div class="mx-2 pb-1">
                                <label class="mb-0 input-alert-text-error font-w-400" id="otpExpired" >
                                    ${msg("otp_expired_msg")}
                                </label>
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
                    const handleSubmit = () => {
                        $('#nextBtn').addClass('disabled');
                        $('#nextBtn-mobile').addClass('disabled');
                        $('#nextBtn').attr('disabled', 'disabled');
                        $('#nextBtn-mobile').attr('disabled', 'disabled');
                        return true;
                    }
                    function editFunction() {
                        $("#edit").val("true");
                        $("#otp-verification-form").submit();
                    }

                    function resendOTPFun() {
                        $("#resendOTP").val("true");
                        $("#editUserDetail").val(${isEditUserDetail});
                        $("#otp-verification-form").submit();
                    }

                    function transactionCancel() {
                        $("#cancel-flow").val("true");
                        $('form').submit();
                    }
                    $( document ).ready(function() {
                        <#if expiry??>
                            $('#otpExpired').hide();
                            otpExpiryCounter(${expiry});
                        </#if>
                    });

                    <#if error??  >
                        $('#template-error').css('display', 'block');
                    </#if>
            </script>
         <#else>
            <form id="otp-verification-form" action="${url.loginAction}" class="row" method="post">
                <input name="isOtpVerification" type="hidden" value="true">
                <input id="edit" name="edit" type="hidden" value="false">
                <input id="resendOTP" name="resendOTP" type="hidden" value="false">
                <#assign isEditUserDetail ='' />
                <#if editUserDetail??>
                    <#assign isEditUserDetail = editUserDetail>
                <#else>
                    <#assign isEditUserDetail = 'true'>
                </#if>
                <input id="editUserDetail" name=editUserDetail type="hidden" value=isEditUserDetail>
                <#if phoneNumberRegion??>
                    <input id="phoneNumberRegion" name="phoneNumberRegion" type="hidden" value="${phoneNumberRegion}">
                </#if>
                <#if phoneNumber??>
                    <input id="phoneNumber" name="phoneNumber" type="hidden" value="${phoneNumber}">
                </#if>
                <div class="col-12">
                    <div class="title_sign">
                        <h2>${msg("otp_ver_title")}</h2>
                    </div>
                </div>
                <#if email??>
                    <input type="hidden" name="emailOtpVerify" id="emailOtpVerify" value="emailOtpVerify" />
                    <input id="email" name="email" value="${email}" type="hidden">
                    <div class="col-12">
                        <div class="form-group">
                            <label>${msg("otp_ver_email_title")}</label>
                            <div style="position: relative">
                                <input name="email" type="email" class="form-control" value="${email}" disabled>
                                <#if isEditUserDetail == 'true'>
                                <a onclick="editFunction()">
                                <i class="tio editBtn cursor-pointer">${msg("edit")}</i>
                                </a>
                                </#if>

                            </div>
                        </div>
                    </div>
                </#if>
                <#if phoneNumberWithCountryCode??>
                    <input type="hidden" name="phoneOtpVerify" id="phoneOtpVerify" value="phoneOtpVerify" />
                    <div class="col-12">
                        <div class="form-group">
                            <label>${msg("otp_ver_mob_num_title")}</label><br>
                            <div style="position: relative">
                                <input name="phoneNumberWithCountryCode" type="tel" class="form-control"
                                    value="${phoneNumberWithCountryCode}" disabled>
                                <#if isEditUserDetail == 'true'>
                                    <a onclick="editFunction()">
                                        <i class="tio editBtn cursor-pointer">${msg("edit")}</i>
                                    </a>
                                </#if>
                            </div>
                        </div>
                    </div>
                </#if>
                <#if username??>
                    <div class="col-12">
                        <div id="divUsername" class="form-group">
                            <label>${msg("otp_ver_username_title")}</label><br>
                            <div style="position: relative">
                                <input name="username" type="tel" class="form-control" value="${username}" disabled>
                                <a onclick="editFunction()">
                                    <i class="tio editBtn cursor-pointer">${msg("edit")}</i>
                                </a>
                            </div>
                        </div>
                        <#if telegramLogin??>
                        <div class="mb-3" style="background-color: #D2FFD2">
                            <p class="mb-0" style="padding: 5px 15px 5px 15px; font-size: 15px">
                                ${msg("otp_sent_telgram_success")}
                            </p>
                        </div>
                        <style>
                            #divUsername {
                                margin-bottom: 0.5rem !important;
                            }
                        </style>
                        </#if>
                    </div>
                </#if>
                <#if whatsappNumber??>
                    <div id="whatsappnumber" class="col-12">
                        <div class="form-group">
                            <label>${msg("otp_ver_wp_num_title")}</label><br>
                            <div style="position: relative">
                                <input name="username" type="tel" class="form-control" value="${whatsappNumber}" disabled>
                                <a onclick="editFunction()">
                                    <i class="tio editBtn cursor-pointer">${msg("edit")}</i>
                                </a>
                            </div>
                        </div>
                            <div class="mb-3" style="background-color: #D2FFD2">
                                <p class="mb-0" style="padding: 5px 15px 5px 15px; font-size: 15px">
                                ${msg("otp_sent_wp_success")}</p>
                            </div>
                            <style>
                                #whatsappnumber {
                                    margin-bottom: 0.5rem !important;
                                }
                            </style>
                    </div>
                </#if>
                <div class="col-12">
                    <div class="form-group">
                        <label>${msg("otp")}</label> <span class="text-danger"> *</span>
                        <input oninput="preventText(this);disableOTPSubmitBtn(${expiry});" id="inputOTP" name="otp" type="text" class="form-control" autofocus placeholder="${msg("enter-your-otp")}"
                            required maxlength="${otp_len!''}">
                        <#import "error_msg.ftl" as e>
                        <@e.error_msg />
                    </div>
                    <#if expiry??>
                        <div class="form-group mb-0">
                            <label class="mb-0" id="otplive">
                                ${msg("otp_expires_in_msg")} <span class="font-w-500 c-primary" id="expirytime"></span>
                            </label>
                            <label class="mb-0 input-alert-text-error font-w-400" id="otpExpired" >
                            ${msg("otp_expired_msg")}
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
                    ${msg("otp_dint_received_msg")}
                    <a onclick="resendOTPFun()" target="_blank" class="action-link-clr cursor-pointer">
                    ${msg("otp_resend_btn")}
                    </a>
                </p>
            </form>
            <script>

                function editFunction() {
                    $("#edit").val("true");
                    $("#otp-verification-form").submit();
                }

                function resendOTPFun() {
                    $("#resendOTP").val("true");
                    $("#editUserDetail").val(${isEditUserDetail});
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
    </#if>
</@layout.registrationLayout>