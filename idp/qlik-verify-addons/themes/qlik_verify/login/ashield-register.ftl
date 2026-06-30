    <#import "template.ftl" as layout>
    <@layout.registrationLayout; section>
    <#if section = "title">
        title
    <#elseif section = "header">
        <span class="${properties.kcWebAuthnKeyIcon}"></span>
        ${kcSanitize(msg("ashield-registration-title"))?no_esc}
    <#elseif section = "form">
        <#if ismobile?? && ismobile>
        <div class="title_sign mt-4 text-center">
        <#if pageType??>
            <h2>${msg("loginTotpHeader")}</h2>
            <div>
                <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
            </div>
        <#else>
             <h2>${msg("ashield-setup")}</h2>
        </#if>
        </div>
        <p class="text-center" > ${msg("ashield-setup-desc")} </p>
        <form id="register" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <input type="hidden" id="success" name="success"/>
                <input type="hidden" id="message" name="message"/>
                <input type="hidden" id="msisdn" name="msisdn"/>
                <input type="hidden" id="transaction_id" name="reg_transaction_id" value="${reg_transaction_id}"/>
                <input type="hidden" id="transaction_id" name="auth_transaction_id" value="${auth_transaction_id}"/>
                <input type="hidden" id="device" name="device"/>
                <input type="hidden" id="errorCodes" name="errorCodes"/>
            </div>
        </form>

        <script defer type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
        <script defer type="text/javascript" src="${url.resourcesPath}/js/base64url.js"></script>
        <script defer type="text/javascript" src="${url.resourcesPath}/js/ezto.js"></script>
        <script defer type="text/javascript">
            $(document).ready(function () {
                    registerAshield("${mid}","${reg_num}","${reg_transaction_id}","${reg_signature}","${auth_transaction_id}","${auth_signature}").then(function(result){
                        $("#success").val(result.success);
                        $("#message").val(result.message);
                        $("#msisdn").val(result.msisdn);
                        $("#device").val(JSON.stringify(result.device));
                        $("#errorCodes").val(result.errorCodes);
                        $("#register").submit();
                    });
            });
        </script>
        <#else>
            <div class="col-12 text-center">
            <svg class="mb-2 error-img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><g fill="var(--form-accept-btn-clr)" data-name="26 - error"><path d="M18.87,3.77a3.21,3.21,0,0,0-5.74,0L2.34,25.35A3.21,3.21,0,0,0,5.21,30H26.79a3.21,3.21,0,0,0,2.87-4.65Zm9,23.66a1.2,1.2,0,0,1-1,.57H5.21a1.2,1.2,0,0,1-1.08-1.75L14.92,4.67a1.21,1.21,0,0,1,2.16,0L27.87,26.25A1.21,1.21,0,0,1,27.82,27.43Z"/><path d="M16 11a1 1 0 0 0-1 1v8a1 1 0 0 0 2 0V12A1 1 0 0 0 16 11zM16 23a1 1 0 1 0 1 1A1 1 0 0 0 16 23zm0 1z"/></g></svg>
            </div>
            <div>
                <p class="text-center">${msg("ashield-not-supported")}</p>
            </div>
        </#if>

    </#if>
    </@layout.registrationLayout>
