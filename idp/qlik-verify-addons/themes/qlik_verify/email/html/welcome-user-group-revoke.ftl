<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg("removed-from")} <#if group??>${group}</#if> ${msg("group")}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hi")} <b><#if eml??>${eml}</#if></b>,
            </p>
            <p>
                ${msg("removed-from-group")} <b>${group}</b> ${msg("by-admin")} <b><#if tnt??>${tnt}</#if></b>.
            </p>
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>