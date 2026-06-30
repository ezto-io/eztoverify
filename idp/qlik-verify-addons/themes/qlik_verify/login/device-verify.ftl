    <#import "template.ftl" as layout>
    <@layout.registrationLayout showAnotherWayIfPresent=false; section>
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
        ${kcSanitize(msg("device-login-title"))?no_esc}
    <#elseif section = "form">
        <#if ismobile?? && ismobile>
            <div class="title_sign text-center">
                <h2>${msg("device-login-auth")}</h2>
            </div>
            <p class="text-center" > ${msg("device-login-desc")} </p>
            <form id="device_login" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <input type="hidden" name="from" value="device-verify"/>
                    <input type="hidden" id="success" name="success"/>
                    <input type="hidden" id="message" name="message"/>
                    <input type="hidden" id="challengeHash" name="challengeHash"/>
                    <input type="hidden" id="keyHashFirst6" name="keyHashFirst6"/>
                    <input type="hidden" id="device" name="device"/>
                </div>
            </form>

            <script defer type="text/javascript">
                $(document).ready(function () {
                    verifySilentBinding("${challenge}").then(function(result){
                        $("#success").val(result.success);
                        $("#message").val(result.message);
                        $("#challengeHash").val(result.challengeHash);
                        $("#keyHashFirst6").val(result.keyHashFirst6);
                        $("#device").val(JSON.stringify(result.device));
                        $("#device_login").submit();
                    });
                });
            </script>
        <#else>
            <div class="col-12 text-center">
                <svg class="mb-2 error-img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><g fill="var(--form-accept-btn-clr)" data-name="26 - error"><path d="M18.87,3.77a3.21,3.21,0,0,0-5.74,0L2.34,25.35A3.21,3.21,0,0,0,5.21,30H26.79a3.21,3.21,0,0,0,2.87-4.65Zm9,23.66a1.2,1.2,0,0,1-1,.57H5.21a1.2,1.2,0,0,1-1.08-1.75L14.92,4.67a1.21,1.21,0,0,1,2.16,0L27.87,26.25A1.21,1.21,0,0,1,27.82,27.43Z"/><path d="M16 11a1 1 0 0 0-1 1v8a1 1 0 0 0 2 0V12A1 1 0 0 0 16 11zM16 23a1 1 0 1 0 1 1A1 1 0 0 0 16 23zm0 1z"/></g></svg>
            </div>
            <div>
                <p class="text-center">${msg("device-not-supported")}</p>
            </div>
        </#if>
    </#if>
    </@layout.registrationLayout>
