    <#import "template.ftl" as layout>
    <@layout.registrationLayout; section>
     <#if (isMobile?? && isMobile) || (isDesktop?? && isDesktop)>
        <#if section = "head">
            <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
            <script defer type="text/javascript" src="/auth/assets/js/base64url.min.js?version=${serverVersion}"></script>
            <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
        </#if>
    </#if>
    <#if section = "title">
        title
    <#elseif section = "header">
        <span class="${properties.kcWebAuthnKeyIcon}"></span>
    <#elseif section = "form">
        <#if (isMobile?? && isMobile) || (isDesktop?? && isDesktop)>
            <div class="title_sign mt-4 text-center">
                 <h2>${msg("fido-auth")}</h2>
            </div>
            <p class="text-center" > ${msg("fido-check-desc")} </p>
            <form id="check" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <input type="hidden" name="from" value="fido-check"/>
                    <input type="hidden" id="deviceId" name="deviceId"/>
                </div>
            </form>

            <script defer type="text/javascript">
                $(document).ready(function () {
                    getFcmToken().then(function(result){
                        $("#deviceId").val(result.device.id);
                        $("#check").submit();
                    });
                });
            </script>
        <#else>
        </#if>

    </#if>
    </@layout.registrationLayout>
