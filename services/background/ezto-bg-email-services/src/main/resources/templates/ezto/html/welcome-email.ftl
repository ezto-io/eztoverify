<#include "index.ftl">
<#macro page_body>
<div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p> ${msg("hello")} there,</p>
            <p>
               ${msg("welcome-email-thank-msg")}
            </p>
        </div>
        <div style="border: 2px solid ${bodyColor}; padding: 40px; margin: 40px 0px;">
            <h4 style="text-align:center;margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;width:100%;color:#000000;">
             ${msg("your-otp")}
            </h4>
            <h2 style="text-align:center;color:#000000;"> <#if otp??>${otp}<#else>{{otp}}</#if> </h2>
            <table style="width: 100%;">
                <tr>
                    <td style="font-size: 14px;">
                       <p style="margin-top: 20px; text-align: center;word-break:break-all">${msg('workspaceLoginLater')}</p>
                    </td>
                </tr>
                <tr>
                    <td align="center">
                      <a href="${workspace}" style="color: ${bodyColor}; font-weight: bolder; font-size: 16px; display: inline-block; margin: 0px 0px 35px; word-break: break-all;text-align:center;"><#if workspace??>${workspace}<#else>{{workspace}}</#if></a>
                    </td>
                </tr>
            </table>
        </div>
       <#if variant == "ezto auth">
            <p>${msg("happy-authenticating")}</p>
         </#if>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>