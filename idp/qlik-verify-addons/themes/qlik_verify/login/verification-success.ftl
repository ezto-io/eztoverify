<!--  Verification Success Page   -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
    <div class="col-12 text-center">
        <img class="mb-2 error-img" alt="success-img.png" src="${url.resourcesPath}/img/success-img.png" />
        <#if verSuccessTitle??>
            <h4>${verSuccessTitle}</h4>
        </#if>
    </div> 
    <#if verSuccessMsg??>
        <p class="text-center">${verSuccessMsg}</p>
    </#if>
    <#if showLoginUrl??>
        <p class="text-center">${msg("loginUrlMsg")} <a id="loginRestartLink" href="${url.loginRestartFlowUrl}">${msg("doClickHere")}</a> </p>.<br/>
    </#if>

    </#if>
</@layout.registrationLayout>