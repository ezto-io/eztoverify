<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg("report-title")}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hello")} <b><#if user_name??>${user_name}</#if></b>,
            </p>
        </div>
        <div style="border: 2px solid ${bodyColor}; padding: 20px; margin: 40px 0px;">
            <table style="width: 100%;text-align:center">
                <tr>
                    <td>
                    <p style="word-break:break-word;">${msg("report-body-1")} <#if tenant??>${tenant}.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <#if workspace??>
                            <p  style="padding: 10px 0px;"></#if></b> ${msg("report-body-3")} </p>
                            <b></b>
                            <a href="${workspace}" style="padding: 5px 20px; background-color: ${bodyColor}; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("take-to-reports")}</a>
                        </#if>
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