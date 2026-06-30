<!--  Info Page   -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if isMobile ?? && isMobile>
        <#if section = "head">
            <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
        </#if>
    </#if>
    <#if section = "form">
        <div class="d-flex justify-content-center align-items-center py-5 my-5">
            <div class="spinner-border text-primary" role="status">
            <span class="sr-only">Loading...</span>
            </div>
        </div>
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
