<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;word-wrap: break-word;">
        ${msg("removed-from")} <#if group??>${group}</#if> ${msg("group")}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hello")} <b><#if user_name??>${user_name}</#if></b>,
            </p>
            <p style="word-wrap: break-word;">
                ${msg("removed-from-group")} <b>${group}</b> ${msg("by-admin")} <b><#if tenant??>${tenant}</#if></b>.
            </p>
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div> 
</#macro>