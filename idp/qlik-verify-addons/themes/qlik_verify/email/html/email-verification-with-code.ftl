<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       ${msg("welcome-to")} {{tenant}}! 
    </h1>
</#macro>
<#macro page_body>
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p> ${msg("hello-there")},</p>
            <p>
               ${msg("email-verification-with-code-verification")}
            </p>
        </div>
        <div style="border: 2px solid ${bodyColor}; padding: 40px; margin: 40px 0px;">
            <h4 style="text-align:center; width:100%; margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg("verification_code-txt")} <#if code??>${code}<#else>{{code}}</#if>
            </h4>
            <table style="width: 100%;">
                <tr>
                    <td style="text-align:center; width:100%; font-size: 14px">
                        ${msg("code-expires-in")} <b><#if expirationTime??>${expirationTime} ${msg("${expirationUnit}")}<#else>{{expirationTime}} {{expirationUnit}}</#if>.</b>
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