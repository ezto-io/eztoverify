<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg('invite-sso-access')} <#if sso_app??>" ${sso_app} "</#if>
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg('hi')} <b><#if user_name??>${user_name}</#if></b>,
            </p>
            <p>
                ${msg('admin-of')} <b><#if tenant??>${tenant}</#if></b> ${msg('sso-invite')}
            </p>
        </div>
        <div style="border: 2px solid #4B72FA; padding: 20px; margin: 40px 0px;">
            <table style="width: 100%;text-align:center">
                <#if sso_logo??>
                <tr>
                    <td>
                    <img src="${sso_logo}" alt="${sso_app}" title="${sso_app}" width=75px height=75px  />
                    </td>
                </tr>
                </#if>
                <tr>
                    <td>
                    <h2 style="word-break:break-all;">${sso_app}<h2>
                    </td>
                </tr>
                 <#if sso_sub_app??>
                <tr>
                    <td>
                    <h2 style="word-break:break-all;">${sso_sub_app}<h2>
                    </td>
                </tr>
                </#if>
                <tr>
                    <td style="font-size: 14px;">
                        ${msg('click-sso-login')}
                    </td>
                </tr>
                <tr>
                    <td>
                        <#if workspace??>
                            <a href="${workspace}" style="padding: 8px 20px; background-color: #005581; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("take-to-dashboard")}</a>
                        </#if>
                    </td>
                </tr>
            </table>  
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>