<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <div class="col-12 text-center">
        <svg fill="var(--form-accept-btn-clr)" height="92px" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><g data-name="Layer 2"><path d="M8 2a6 6 0 1 0 6 6 6 6 0 0 0-6-6Zm0 11a5 5 0 1 1 5-5 5 5 0 0 1-5 5Z"/><path d="M8 6.85a.5.5 0 0 0-.5.5v3.4a.5.5 0 0 0 1 0v-3.4a.5.5 0 0 0-.5-.5zM8 4.8a.53.53 0 0 0-.51.52v.08a.47.47 0 0 0 .51.47.52.52 0 0 0 .5-.5v-.12A.45.45 0 0 0 8 4.8z"/></g></svg>
    </div>
    <#if section = "header">
        <h4 class="text-center">${msg("emailVerifyTitle")}</h4>
    <#elseif section = "form">
        <p class="text-center">${msg("emailVerifyInstruction1")}</p>
    <#elseif section = "info">
        <p class="text-center">
            ${msg("emailVerifyInstruction2")}
            <br/>
            <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailVerifyInstruction3")}
        </p>
    </#if>
</@layout.registrationLayout>