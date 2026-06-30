<!-- Page Expired FTL -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("pageExpiredTitle")}
    <#elseif section = "form">
        <p id="instruction1" class="instruction">
            ${msg("pageExpiredMessage")} 
        </p>
    </#if>
</@layout.registrationLayout>
