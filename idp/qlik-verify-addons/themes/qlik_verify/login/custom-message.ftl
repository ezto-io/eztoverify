<!--  Info Page   -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "head">
        <#if isMobile?? && isMobile>
            <script defer src="/auth/assets/js/ezto.min.js?version=${serverVersion}" type="text/javascript"></script>
        </#if>
    </#if>
    <#if section = "form">
        <div class="col-12 text-center">
            <#if cibaState?? && (cibaState?lower_case == 'verification_failed' || cibaState?lower_case == 'onboarding_failed' || cibaState?lower_case == 'link_expired')>
                <img class="mb-2 error-img" alt="error img" src="/auth/assets/img/error.png?version=${serverVersion}" />
            <#elseif logo??>
                <img class="mb-2 error-img" alt="logo" src="${logo}" />
            <#else>
                <img class="mb-2 success-img" alt="success img" src="/auth/assets/img/success-img.png?version=${serverVersion}" />
            </#if>
            <#if cibaTitle??>
                <div class="title_sign mb-2">
                    <h2 class="json-form-label">${msg('${cibaTitle}')}</h2>
                </div>
            </#if>
        </div>
        <#if cibaResponse??>
            <p class="text-center word-break json-form-label">${msg('${cibaResponse}')}</p>
        </#if>
        <#if isMobile ?? && isMobile>
            <script defer>
                $(document).ready(function () {
                    callTimeout();
                    function callTimeout(){
                        <#if cibaState?? && (cibaState?lower_case == 'verification_failed' || cibaState?lower_case == 'onboarding_failed' || cibaState?lower_case == 'link_expired')>
                        setTimeout(function(){
                            failure(`${cibaState}`);
                        },5000);
                        <#else>
                        setTimeout(function(){
                            success();
                        },5000);
                        </#if>
                    }
                });
            </script>
        </#if>
    </#if>
</@layout.registrationLayout>
