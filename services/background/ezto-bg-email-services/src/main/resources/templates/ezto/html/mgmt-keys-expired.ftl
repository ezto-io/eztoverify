<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       ${msg("mgmt-keys-expired-title", applicationName)}
    </h1>
</#macro>
<#macro page_body>
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hello")} <b><#if user_name??>${user_name}</#if></b>,
            </p>
            <p>${kcSanitize(msg("mgmt-expired-description",workspace))?no_esc}</p>
            <ul>
                <#list keys as key>
                    <b>
                        <li>${key.name}</li>
                    </b>
                </#list>
            </ul>
            <p>${kcSanitize(msg("mgmt-expired-suggestion"))?no_esc}</p>
            <p>${msg("mgmt-greeting")}</p>
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