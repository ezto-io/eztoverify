<!--  QV Error Page   -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <#if errorMessage??>
            <p>${msg("${errorMessage}")}</p>
            <#else>
            <p>${kcSanitize(msg("general_error_instruction"))?no_esc} </p>
        </#if>
    </#if>
</@layout.registrationLayout>