<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg('role-remvoed')}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hi")} <b><#if eml??>${eml}</#if></b>,
            </p>
            <p>
                ${msg('you-dont-have-access')}
            </p>
        </div>
        <div style="border: 2px solid #4B72FA; padding: 20px; margin: 40px 0px;">
            <table style="width: 100%;text-align:center">
                <tr>
                    <td>
                    <h2 style="word-break:break-all;"> ${msg('admin-role-removed')}<h2>
                    <h2 style="word-break:break-all;"><b><#if tnt??>${tnt}</#if></b><h2>
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
                            <a href="${workspace}" style="padding: 8px 20px; background-color: #4B72FA; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("take-to-dashboard")}</a>
                        </#if>
                    </td>
                </tr>
            </table>  
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>