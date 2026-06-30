<#import "template.ftl" as layout>
    <@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
        <#if section="header">
            ${msg("doLogIn")}
            <#elseif section="form">
                <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
                    method="post">

                    <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
                    <div class="title_sign">
                        <#if clientName??>
                             <h2>${msg("loginTotpHeader")}</h2>
                             <div>
                                <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
                            </div>
                        <#else>
                             <h2>${msg("otp_ver_title")}</h2>
                        </#if>
                    </div>

                    <div class="${properties.kcFormGroupClass!}">
                        <#if mfaProviderId??>
                            <#if mfaProviderId == msg("google-authenticator")>
                                <label class="mb-3">${msg("googleAuthenticatorMessage")} ${msg("to-authenticate-application")} <b>${clientName}</b></label>
                            <#elseif mfaProviderId == msg("authy")>
                                 <label class="mb-3">${msg("authyMessage")} ${msg("to-authenticate-application")} <b>${clientName}</b></label>
                            <#elseif mfaProviderId == msg("free-otp")>
                                 <label class="mb-3">${msg("freeOTPMessage")} ${msg("to-authenticate-application")} <b>${clientName}</b></label>
                            <#else>
                                 <label class="mb-3">${msg("defaultMFAMessage")} ${msg("to-authenticate-application")} <b>${clientName}</b></label>
                            </#if>
                        </#if>
                        <label>
                            ${msg("otp")}
                            <span class="text-danger"> *</span>
                        </label>
                        <input id="otp" name="otp" autocomplete="off" type="text" class="form-control"
                               autofocus aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>" placeholder="${msg("enter-your-otp")}" oninput="validateOtp()" onpaste="validateOtp()" />

                        <#if messagesPerField.existsError('totp')>
                            <span id="input-error-otp-code" class="${properties.kcInputErrorMessageClass!}"
                                  aria-live="polite">
                                ${kcSanitize(messagesPerField.get('totp'))?no_esc}
                            </span>
                        </#if>
                        <#import "error_msg.ftl" as e>
                        <@e.error_msg />
                </div>

                    <div>
                        <div id="kc-form-options">
                            <div class="${properties.kcFormOptionsWrapperClass!}">
                            </div>
                        </div>

                        <div id="kc-form-buttons">
                            <input
                                class="btn w-100 margin-t-3 btn_account bg-orange-red cursor-unset disabled"
                                name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>

                            <#if showTroubleShoot?? && showTroubleShoot>
                                    <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                                        <a onclick="troubleShootMFAFunc()" class="action-link-clr cursor-pointer">
                                        ${msg("troubleshoot_mfa")}
                                        </a>
                                    </p>
                            </#if>
                        </div>
                    </div>
                </form>
            <script defer type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
            <script defer type="text/javascript">
                function troubleShootMFAFunc(){
                    $("#troubleShootMFA").val("true");
                    $("#kc-otp-login-form").submit();
                }

                function validateOtp() {
                    const otp = document.getElementById("otp").value.trim();
                    if (otp) {
                        $("#kc-login").removeClass("disabled");
                    } else {
                        $("#kc-login").addClass("disabled");
                    }
                }
            </script>
        </#if>
        </@layout.registrationLayout>