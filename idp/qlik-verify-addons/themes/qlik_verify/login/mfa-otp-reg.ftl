<!-- MFA Authenticator Reg --> 
<#import "template.ftl" as layout>
<@layout.registrationLayout displayRequiredFields=false displayMessage=!messagesPerField.existsError('totp','userLabel'); section>

    <#if section = "header">
        ${msg("loginTotpTitle")}
    <#elseif section = "form">
        <div class="title_sign">
            <h2>${msg("loginTotpHeader")}</h2>
            <div>
                <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
            </div>
        </div>
        <ol class="col-xs-12 col-sm-12 col-md-12 col-lg-12 mfa-steps-continer pr-1 ml-0" id="kc-totp-settings">
            <li>
                <p>${msg("loginTotpStep1",applicationName)}</p>
                    <div class=" d-flex justify-content-center mb-3 ml-n-md">
                        <#if appStore??>
                            <a href="${appStore}" target="_blank"> <img class="mb-2 error-img" width="145px" alt="apple-store" src="${url.resourcesPath}/img/apple-store.svg"> </a>
                        </#if>
                        <#if playStore??>
                            <a href="${playStore}" target="_blank"><img class="mb-2 error-img ml-3" width="145px" alt="play-store"  src="${url.resourcesPath}/img/play-store.svg"> </a>
                        </#if>
                    </div>
            </li>

            <#if totpMode?? && totpMode == "manual">
                <li>
                    <p>${msg("loginTotpManualStep2")}</p>
                    <#if totp?? && totp.totpSecretEncoded??>
                        <p><span id="kc-totp-secret-key">${totp.totpSecretEncoded}</span></p>
                    <#else>
                        <p>${msg("failedToGenerateCode")}</p>
                    </#if>
                    <#if totp?? && totp.qrUrl??>
                        <p><a href="${totp.qrUrl}" id="mode-barcode">${msg("loginTotpScanBarcode")}</a></p>
                    <#else>
                        <p>${msg("failedToGenerateCode")}</p>
                    </#if>
                </li>
                <li>
                    <p>${msg("loginTotpManualStep3")}</p>
                    <p>
                    <ul>
                        <#if totp?? && totp.policy??>
                            <li id="kc-totp-type">${msg("loginTotpType")}: ${msg("loginTotp." + totp.policy.type)}</li>
                            <li id="kc-totp-algorithm">${msg("loginTotpAlgorithm")}: ${totp.policy.getAlgorithmKey()}</li>
                            <li id="kc-totp-digits">${msg("loginTotpDigits")}: ${totp.policy.digits}</li>
                            <#if totp.policy.type = "totp">
                                <li id="kc-totp-period">${msg("loginTotpInterval")}: ${totp.policy.period}</li>
                            <#elseif totp.policy.type = "hotp">
                                <li id="kc-totp-counter">${msg("loginTotpCounter")}: ${totp.policy.initialCounter}</li>
                            </#if>
                        </#if>
                    </ul>
                    </p>
                </li>
            <#else>
                <li>
                    <p>${msg("loginTotpStep2")}</p>
                    <div class="ml-n-md">
                        <div class="qr-container mb-4 mx-auto">
                            <img id="kc-totp-secret-qr-code" src="data:image/png;base64, ${totp.totpSecretQrCode}" alt="Figure: Barcode"><br/>
                        </div>
                    </div>
                    <p class="text-center ml-n-md">${msg("loginTotpUnableToScan")}<a href="${totp.manualUrl}" class="action-link-clr" id="mode-manual">${msg("doClickHere")}</a></p>
                </li>
            </#if>
            <li>
                <p>${msg("loginTotpStep3")}</p>
                <p>${msg("loginTotpStep3DeviceName")}</p>
            </li>
        </ol>

        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-totp-settings-form" method="post">
                <div class="form-group">
                    <label for="totp" class="control-label">${msg("authenticatorCode")}</label> <span class="text-danger"> *</span>
                        <input oninput="disableSubmit()" class="form-control" placeholder="${msg("enter-code")}" type="text" id="totp" name="totp" autocomplete="off"
                           aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>"
                        />

                        <#if messagesPerField.existsError('totp')>
                            <span id="input-error-otp-code" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('totp'))?no_esc}
                            </span>
                        </#if>
                </div>
                <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
                <input type="hidden" id="mfaProviderId" name="mfaProviderId" value="${mfaProviderId}" />
                <#if totpMode??><input type="hidden" id="mode" name="mode" value="${totpMode}"/></#if>
            <div class="form-group">
                    <label for="userLabel" class="control-label">${msg("loginTotpDeviceName")}</label> <span class="text-danger"> *</span>

                    <input oninput="disableSubmit()" class="form-control" placeholder="${msg("enter-device-name")} " type="text" class="${properties.kcInputClass!}" id="userLabel" name="userLabel" autocomplete="off"
                           aria-invalid="<#if messagesPerField.existsError('userLabel')>true</#if>"
                    />

                    <#if messagesPerField.existsError('userLabel')>
                        <span id="input-error-otp-label" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('userLabel'))?no_esc}
                        </span>
                    </#if>
            </div>

            <#if isAppInitiatedAction??>
                <button type="submit"
                        class="btn w-100 margin-t-3 btn_account bg-orange-red  "
                        id="cancelTOTPBtn" name="cancel-aia" value="true" />${msg("doCancel")}
                </button>
            <#else>
                <input type="submit"
                       class="btn w-100 margin-t-3 btn_account bg-orange-red  "
                       id="saveTOTPBtn" value="${msg("doSubmit")}" disabled
                />
            </#if>
        </form>
        <script>
        function disableSubmit(){
            $('#input-error-otp-code').hide();
            const enable = $("#totp").val() && $('#userLabel').val();
            if( enable ){
                $('#saveTOTPBtn').removeClass('disabled');
                $('#saveTOTPBtn').removeAttr('disabled');
            }
            else{
                $('#saveTOTPBtn').addClass('disabled');
                $('#saveTOTPBtn').attr('disabled', 'disabled');
            }
        }
        </script>
    </#if>
</@layout.registrationLayout>