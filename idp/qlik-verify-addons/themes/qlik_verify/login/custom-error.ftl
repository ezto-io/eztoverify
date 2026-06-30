<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="nect-verify-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <h1>${msg("error")}</h1>
        </form>
    </#if>
</@layout.registrationLayout>
