<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
<#if section = "head">
    <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
    <script defer type="text/javascript" src="/auth/assets/js/base64url.min.js?version=${serverVersion}"></script>
</#if>
<#if section = "title">
        title
<#elseif section = "form">
    <h2 class="ciba-template-title text-break text-center mt-2">${apiRequestTitle}</h2>
    <p class="ciba-template-subtitle text-dark text-break mt-4 text-center" > ${apiRequestDesc} </p>
    <form id="check" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <input type="hidden" name="from" value="http-request"/>
        </div>
    </form>
    <#if isMobile?? && !isMobile>
        <div class="d-flex justify-content-center align-items-center py-5">
            <div class="spinner-border branding-primary-color" role="status">
                <span class="sr-only">Loading...</span>
            </div>
        </div>
    </#if>
    <script defer type="text/javascript">
        $(document).ready(function () {
            setTimeout(() => $("#check").submit() , 3000);
        });
    </script>
</#if>
</@layout.registrationLayout>
