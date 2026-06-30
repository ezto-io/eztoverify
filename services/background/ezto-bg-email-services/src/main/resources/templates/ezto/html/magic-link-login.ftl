<#include "index.ftl">
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p> ${msg("hello")}there,</p>
            <p>
                ${msg("magic-link-sent-content")}
            </p>
        </div>
        <div style="border: 2px solid ${bodyColor}; padding: 40px; margin: 40px 0px;">
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg("magic-link")}
            </h4>
            <div style="text-align: center">
                <a href="<#if link??>${link}<#else>{{link}}</#if>" style="padding: 8px 20px; background-color: ${bodyColor}; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("magicLinkRegistrationSubject")} </a>
            </div>
            <div style="text-align: center"> ${msg("login-link")}</div>
            <div style="margin:5px">
                <a href="<#if link??>${link}<#else>{{link}}</#if>" style="color: #6abe45; display: inline-block; padding: 10px 0;word-break:break-all">${link}</a>
             </div>
            <table style="width: 100%;">
                <tr>
                    <td style="font-size: 14px">
                        ${msg("link-expires-in")} <b><#if expirationTime??>${expirationTime} ${msg("${expirationUnit}")}<#else>{{expirationTime}} {{expirationUnit}}</#if>.</b>
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