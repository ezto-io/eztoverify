<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg("welcome-to")} ${realmName}!
    </h1>
</#macro>
<#macro page_body>  
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hi")} ${user.getUsername()},<br><br>${msg("password-reset-otp")}
            </p>
        </div>
        <div style="border: 2px solid #4B72FA; padding: 40px; margin: 40px 0px;">
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg("verification-otp")} <#if code??>${code}<#else>{{code}}</#if>
            </h4>
            <table style="width: 100%;">
                <tr>
                    <td style="font-size: 14px">
                        ${msg("otp-expires-in")} <b><#if expirationTime??>${expirationTime} ${msg("${expirationUnit}")}<#else>{{expirationTime}} {{expirationUnit}}</#if>.</b>
                    </td>
                </tr>
            </table>  
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>