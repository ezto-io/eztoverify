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
                ${msg("hi")} <b><#if eml??>${eml}</#if></b>,
            </p>
            <p>
                ${msg("password-has-been-reset")} <b><#if tnt??>${tnt}</#if></b>. ${msg("otp-to-access-account")}
            </p>
        </div>
        <div style="border: 2px solid #4B72FA; padding: 40px; margin: 40px 0px;">
            <table style="width: 100%;text-align:center">
                <tr>
                    <td style="font-size: 14px;">
                    ${msg("your-otp")}
                    </td>
                </tr>
                <tr>
                    <td>
                    <h2><#if temppass??>${temppass}</#if><h2>
                    </td>
                </tr>
                <tr>
                    <td style="font-size: 14px;">
                    ${msg("click-btn-to-get-started")}
                    </td>
                </tr>
                <tr>
                    <td>
                    <a href="<#if link??>${link}<#else>{{link}}</#if>" style="padding: 8px 20px; background-color: #4B72FA; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("take-to-dashboard")}</a>
                    </td>
                </tr>
            </table>  
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>