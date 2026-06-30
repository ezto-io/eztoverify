<#include "general-index.ftl">
<#macro page_title>
<h1 style="margin-top: 0px;">
    ${msg('welcomeUserSubject')}
</h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg('hi')} <b><#if user_name??>${user_name}</#if></b>,
            </p>
            <p>
                ${msg('acc-created-with')} <b><#if tenant??>${tenant}</#if></b>. ${msg("find-otp")}
            </p>
        </div>
        <div style="border: 2px solid #005581; padding: 20px; margin: 40px 0px;">
            <table style="width: 100%;text-align:center">
                <tr>
                    <td style="font-size: 14px;">
                    ${msg('one-time-password')}
                    </td>
                </tr>
                <tr>
                    <td>
                    <h2 style="word-break:break-all;"><#if temporary_password??>${temporary_password}</#if><h2>
                    </td>
                </tr>
                <tr>
                    <td>
                        <#if link??>
                            <a href="${link}" style="padding: 8px 20px; background-color: #005581; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("click-to-login")}</a>
                        <#else>
                            <p style="color: #636363; font-size: 16px; margin: 10px 0px;">${msg("workspaceLinkInvalid")}</p>
                        </#if>
                    </td>
                </tr>
            </table>  
        </div>
        <#if workspace??>
            <div style="border: 2px solid #005581; padding: 20px; margin: 40px 0px;">
                <table style="width: 100%;text-align:center">
                    <tr>
                        <td style="font-size: 14px;">
                            ${msg("workspaceLoginLater")}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <a href="${workspace}" style="color: #005581;text-decoration-color:#005581; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; word-break: break-all;">${workspace}</a>
                        </td>
                    </tr>
                </table>
            </div>
        </#if>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>