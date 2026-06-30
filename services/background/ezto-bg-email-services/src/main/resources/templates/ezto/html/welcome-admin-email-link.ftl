<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg("welcome-to")} <#if tenant??>${tenant}</#if>!
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("hello")} <b><#if user_name??>${user_name}</#if></b>,
            </p>
            <p>
                ${kcSanitize(msg("co-admin-invite", tenant))?no_esc}
            </p>
        </div>
        <div style="border: 2px solid ${bodyColor}; padding: 20px; margin: 40px 0px;">
            <table style="width: 100%;text-align:center">
                <tr>
                    <td style="font-size: 14px;">
                        ${msg("click-btn-to-get-started")}
                    </td>
                </tr>
                <tr>
                    <td>
                        <#if link??>
                            <a href="${link}" style="padding: 8px 20px; background-color: ${bodyColor}; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("click-to-login")}</a>
                            <#if link_expiry??>
                                <br>
                                <p> ${msg("link-expires-in")} <b>${link_expiry.value} ${msg("${link_expiry.unit_key}")}.</b>
                            </#if>
                        <#else>
                            <p style="color: #636363; font-size: 16px; margin: 10px 0px;">${msg("workspaceLinkInvalid")}</p>
                        </#if>
                    </td>
                </tr>
            </table>  
        </div>
        <#if workspace??>
            <div style="border: 2px solid ${bodyColor}; padding: 20px; margin: 40px 0px;">
                <table style="width: 100%;text-align:center">
                    <tr>
                        <td style="font-size: 14px;">
                            ${msg("workspaceLoginLater")}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <a href="${workspace}" style="color: ${bodyColor}; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; word-break: break-all;">${workspace}</a>
                        </td>
                    </tr>
                </table>
            </div>
        </#if>
        <#if variant == "ezto auth">
            <p>${msg("happy-authenticating")}</p>
        <#elseif variant == "ezto verify">
            <p>${msg("happy-verifying")}</p>
        </#if>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>