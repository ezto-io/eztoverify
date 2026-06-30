<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;word-wrap: break-word;">
        ${msg("role")} <#if role??>" ${role}" </#if> ${msg("revoked")}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hello")} <b><#if user_name??>${user_name}</#if></b>,
            </p>
            <p style="word-wrap: break-word;">
                ${msg("admin-of")} <b><#if tenant??>${tenant}</#if></b> ${msg("revoked-role")}
            </p>
        </div>
        <div style="border: 2px solid ${bodyColor}; padding: 20px; margin: 40px 0px;">
            <table style="width: 100%;text-align:center">
                <tr>
                    <td>
                    <h2 style="word-break:break-all;">${role}<h2>
                    </td>
                </tr>
                <tr>
                    <td style="font-size: 14px;">
                        ${msg("click-to-login-dashboard")}
                    </td>
                </tr>
                <tr>
                    <td>
                     <#if workspace??>
                            <a href="${workspace}" style="padding: 8px 20px; background-color: ${bodyColor}; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("take-to-dashboard")}</a>
                        </#if>
                    </td>
                </tr>
            </table>  
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>