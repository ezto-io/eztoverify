
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
<#if section = "head">
    <#if ismobile?? && ismobile>
        <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
        <script defer type="text/javascript" src="/auth/assets/js/base64url.min.js?version=${serverVersion}"></script>
        <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
    </#if>
</#if>
<#if section = "title">
    title
<#elseif section = "header">
    <span class="${properties.kcWebAuthnKeyIcon}"></span>
    ${kcSanitize(msg("proximity-title"))?no_esc}
<#elseif section = "form">
    <#if ismobile?? && ismobile>
        <div class="badge badge-success text-white h-25 ml-1 mt-2_4"
        style="float: right; width: 50px; padding: 1px; line-height: 20px;">Beta</div>
        <div class="title_sign mt-4 text-center">
            <h2>${msg("proximity-title")}</h2>
        </div>
        <#if error??>
            <span class="text-danger text-center">${error}</span>
        </#if>
        <p class="text-center" > ${msg("proximity-message")} </p>
        <form id="check" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <input type="hidden" name="from" value="proximity-check"/>
                <input type="hidden" id="location" name="location"/>
                <input type="hidden" id="error" name="error"/>
                <input type="hidden" id="success" name="success" value="true"/>
                <input type="hidden" id="time" name="time"/>
                <input type="hidden" id="retry" name="retry" value="false"/>
                <input type="hidden" id="cancel" name="cancel" value="false"/>
            </div>
            <button class="btn w-100 mt-2 btn_account bg-orange-red" id="retry" onclick="onRetry()">${msg("retry")}</button>
            <button class="btn w-100 mt-2 btn_account bg-orange-red" id="cancel" onclick="onCancel()">Cancel</button>
        </form>
        <script defer type="text/javascript">
            $(document).ready(function () {
                <#if !error??>
                    getDetails();
                </#if>

                function getDetails(){
                    readDeviceDetails().then(function(result){
                        $("#error").val(result.message);
                        $("#success").val(result.success);
                        $("#location").val(JSON.stringify(result.location));
                        $("#time").val(JSON.stringify(result.time));
                        $("#check").submit();
                    });
                }

                function onRetry(){
                    $("#retry").val("true");
                    $("#submit").submit();
                }

                function onCancel(){
                    $("#cancel").val("true");
                    $("#submit").submit();
                }
            });
        </script>
    <#else>
        <div class="col-12 text-center">
        <svg class="mb-2 error-img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><g fill="var(--form-accept-btn-clr)" data-name="26 - error"><path d="M18.87,3.77a3.21,3.21,0,0,0-5.74,0L2.34,25.35A3.21,3.21,0,0,0,5.21,30H26.79a3.21,3.21,0,0,0,2.87-4.65Zm9,23.66a1.2,1.2,0,0,1-1,.57H5.21a1.2,1.2,0,0,1-1.08-1.75L14.92,4.67a1.21,1.21,0,0,1,2.16,0L27.87,26.25A1.21,1.21,0,0,1,27.82,27.43Z"/><path d="M16 11a1 1 0 0 0-1 1v8a1 1 0 0 0 2 0V12A1 1 0 0 0 16 11zM16 23a1 1 0 1 0 1 1A1 1 0 0 0 16 23zm0 1z"/></g></svg>
        </div>
        <div>
            <p class="text-center">${msg("proximity-not-supported")}</p>
        </div>
    </#if>

</#if>
</@layout.registrationLayout>
