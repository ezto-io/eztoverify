<!--  Info Page   -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if isMobile ?? && isMobile>
        <#if section = "head">
            <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
        </#if>
    </#if>
    <#if section = "form">
        <div class="col-12 text-center">
            <#if cibaState?? && (cibaState?lower_case == 'verification_failed' || cibaState?lower_case == 'onboarding_failed' || cibaState?lower_case == 'link_expired')>
                <img class="mb-2 error-img" alt="error img" src="/auth/assets/img/error.png?version=${serverVersion}" />
            <#else>
                <img class="mb-2 error-img" alt="success img" src="/auth/assets/img/success-img.png?version=${serverVersion}" />
            </#if>
            <#if cibaState??>
                <div class="title_sign mb-2">
                    <h2>${msg('${cibaState}')}</h2>
                </div>
            </#if>
        </div>

        <#if cibaResponse??>
            <p class="text-center">${msg('${cibaResponse}')}</p>
        </#if>
        <#if isMobile ?? && isMobile>
            <script>
                $(document).ready(function () {
                    <#if showStatusCard ?? && !showStatusCard>
                        failure();
                    <#else>
                        callTimeout();
                    </#if>
                });
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
            </script>
        </#if>
    </#if>
</@layout.registrationLayout>
