<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg('welcome-to')} ${realmName}!
    </h1>
</#macro>
<#macro page_body>  
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg('hi')} ${user.username},<br><br> ${msg('password-reset-link')}
            </p>
        </div>
        <div style="border: 2px solid #005581; padding: 40px; margin: 40px 0px;">
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg('password-reset-btn')}
            </h4>
            <div style="text-align: center">
                <a href="<#if link??>${link}<#else>{{link}}</#if>" style="padding: 8px 20px; background-color: #005581; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg('reset-password')}</a> 
            </div>
            <div style="text-align: center"> ${msg('reset-using-link')}</div>
            <p style="word-break: break-all;" ><#if link??>${link}<#else>{{link}}</#if></p>
            <table style="width: 100%;">
                <tr>
                    <td style="font-size: 14px">
                        ${msg('link-expires-in')} <b><#if linkExpiration??>${linkExpirationFormatter(linkExpiration)}<#else>{{expiration}}</#if>.</b>
                    </td>
                </tr>
            </table>  
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>