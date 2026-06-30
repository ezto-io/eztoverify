<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
<#if section = "head">
    <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
    <script defer type="text/javascript" src="/auth/assets/js/base64url.min.js?version=${serverVersion}"></script>
    <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
</#if>
<#if section = "title">
        title
<#elseif section = "header">
    <span class="${properties.kcWebAuthnKeyIcon}"></span>
    ${kcSanitize(msg("iproov"))?no_esc}
<#elseif section = "form">
    <#if ismobile?? && ismobile>
        <div class="d-flex justify-content-between align-items-center mt-2" id="template-header">
            <h2 class="ciba-template-title mb-0">${msg(title)}</h2>
            <#if skippable?? && skippable>
                <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
            </#if>
        </div>
        <#if isReg>
                <p class="ciba-template-subtitle text-dark mt-4 mb-3 " > ${msg("face-reg-desc")} </p>
        <#else>
                <p class="ciba-template-subtitle text-dark mt-4 mb-3 " > ${msg("face-ver-desc")} </p>
        </#if>
        <div class="row mt-auto">
            <#--  Template Buttons  -->
            <div class="col-12 d-flex mt-3" id="template-button">
                <div id="cancelBtn" onclick="transactionCancel()" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                <button id="nextBtn" onclick="iproovRegOrVer()" class="w-100 py-3 ml-2 ciba-template-submitbtn">${msg('start')}</button>
            </div>
        </div>
        <form id="iproov" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <input type="hidden" id="device" name="device"/>
                <input type="hidden" id="success" name="success"/>
                <input type="hidden" id="message" name="message"/>
                <input type="hidden" id="error" name="error"/>
                <input type="hidden" id="cancel" name="cancel"/>
                <input name="skipped" type="hidden" id="skip-flow" value="false" />
            </div>
        </form>

        <script defer type="text/javascript">
            var isTransactionFlowSkipped = false;
            function iproovRegOrVer() {
                $("#nextBtn").prop("disabled", true);
                <#if isReg>
                    registerIproov("${streamingUrl}","${token}").then(function(result){
                        $("#success").val(result.success);
                        $("#message").val(result.message);
                        $("#device").val(JSON.stringify(result.device));
                        $("#error").val(result.error);
                        $("#iproov").submit();
                    });
                <#else>
                    verifyIproov("${streamingUrl}","${token}").then(function(result){
                        $("#success").val(result.success);
                        $("#message").val(result.message);
                        $("#error").val(result.error);
                        $("#device").val(JSON.stringify(result.device));
                        $("#iproov").submit();
                    });
                </#if>
            }

            function transactionCancel() {
                    $("#cancel").val("true");
                    $('form').submit();
            }

            function transactionSkip() {
                if(!isTransactionFlowSkipped){
                    isTransactionFlowSkipped = true;
                    $("#skip-flow").val("true");
                    $('form').submit();
                }
            }

        </script>
    <#else>
        <div class="col-12 text-center">
            <svg class="mb-2 error-img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><g fill="var(--form-accept-btn-clr)" data-name="26 - error"><path d="M18.87,3.77a3.21,3.21,0,0,0-5.74,0L2.34,25.35A3.21,3.21,0,0,0,5.21,30H26.79a3.21,3.21,0,0,0,2.87-4.65Zm9,23.66a1.2,1.2,0,0,1-1,.57H5.21a1.2,1.2,0,0,1-1.08-1.75L14.92,4.67a1.21,1.21,0,0,1,2.16,0L27.87,26.25A1.21,1.21,0,0,1,27.82,27.43Z"/><path d="M16 11a1 1 0 0 0-1 1v8a1 1 0 0 0 2 0V12A1 1 0 0 0 16 11zM16 23a1 1 0 1 0 1 1A1 1 0 0 0 16 23zm0 1z"/></g></svg>
        </div>
        <div>
            <p class="text-center">${msg("iproov-not-supported")}</p>
        </div>
    </#if>
</#if>
</@layout.registrationLayout>
