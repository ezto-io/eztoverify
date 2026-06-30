<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg("reset-password")}
    </h1>
</#macro>
<#macro page_body>  
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hello")} ${user.username},<br><br> ${msg("password-reset-link",realmName)}
            </p>
        </div>
        <div style="border: 2px solid ${bodyColor}; padding: 40px; margin: 40px 0px;">
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg("password-reset-btn")}
            </h4>
            <div style="text-align: center">
                <a href="<#if link??>${link}<#else>{{link}}</#if>" style="padding: 8px 20px; background-color: ${bodyColor}; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("reset-password")}</a>
            </div>
            <div style="text-align: center"> ${msg("reset-using-link")}</div>
            <a style="word-break: break-all; color: ${bodyColor};" href="<#if link??>${link}<#else>{{link}}</#if>" ><#if link??>${link}<#else>{{link}}</#if></a>
            <table style="width: 100%;">
                <tr>
                    <td style="font-size: 14px">
                        ${msg("link-expires-in")} <b><#if link_expiration??>${linkExpirationFormatter(link_expiration)}<#else>{{expiration}}</#if>.</b>
                    </td>
                </tr>
            </table>  
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