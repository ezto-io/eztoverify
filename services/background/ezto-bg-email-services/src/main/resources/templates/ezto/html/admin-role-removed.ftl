<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg('role-removed')}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hello")} <b><#if user_name??>${user_name}</#if></b>,
            </p>
            <p>
                ${msg('you-dont-have-access',tenant)}
            </p>
        </div>
        </div>
        <#if variant == "ezto auth">
            <p>${msg("happy-authenticating")}</p>
        <#elseif variant == "ezto verify">
            <p>${msg("happy-verifying")}</p>
        </#if>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>