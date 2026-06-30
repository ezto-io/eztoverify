<!--  Info Page   -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        <div style="margin-bottom: 0;"></div>
        <#if messageHeader??>
            <h1>${messageHeader}</h1>
        <#else>
            <h2>${message.summary}</h2>
        </#if>
    <#elseif section = "form">
        <div id="kc-info-message" class="text-center">
            <#if messageHeader??>
                <h4>  ${msg("${messageHeader}")} </h4>
            </#if>
            <p class="instruction text-center">${message.summary}<#if requiredActions??><#list requiredActions>: <b><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#items></b></#list><#else></#if></p>
            <#if skipLink??>
            <#else>
                <#if pageRedirectUri?has_content>
                    <p><a class="btn w-100 margin-t-3 btn_account bg-orange-red" href="${pageRedirectUri}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
                <#elseif actionUri?has_content>
                    <div class="${properties.kcFormGroupClass!}">
                        <button id="instruction3" onclick="location.href=`${actionUri}`" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red cursor-unset" >
                            ${msg("confirm-button")}
                        </button>
                    </div>
                <#elseif (client.baseUrl)?has_content>
                    <p><a class="btn w-100 margin-t-3 btn_account bg-orange-red" href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
                </#if>
            </#if>
        </div>
    </#if>  
</@layout.registrationLayout>