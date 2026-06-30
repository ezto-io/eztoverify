<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       ${msg("hook-failed-title", policyName)}
    </h1>
</#macro>
<#macro page_body>
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
               ${msg("hello")} <b><#if user_name??>${user_name}</#if></b>,
            </p>
            <p>
                ${msg("webhook-failed-trouble-sending-response", workspaceName)}
            </p>
            <p>
                ${msg("url-failing-webhook")} <a href=${hookUrl} target="_blank">${hookUrl}</a>.
            </p>
            <p>
                ${msg("webhook-endpoint-send-result")}
            </p>
            <p>
                ${msg("thank-you-for-your-patience")}
            </p>
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