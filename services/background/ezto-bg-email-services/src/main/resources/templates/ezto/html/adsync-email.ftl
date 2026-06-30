<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg("syncHeader",syncProvider!'')} ${tenant!}
    </h1>
</#macro>
<#macro page_body>  
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
           <p style="margin: 20px 0;line-height:1.4;">${msg("hello")} ${user_name!},</p>
           <p style="margin: 20px 0;line-height:1.4;">${msg("syncDescription",syncProvider!,connectionId)} </p>
        </div>
        <div style="margin:40px 0px">
            <p style="margin: 20px 0;line-height:1.4;">${msg("syncTableDescription")}</p>
            <table style="color:#333333;font-size:13px;border-collapse:collapse;line-height:1.4;width:100%;" width="100%" align="center" cellpadding="0"
            cellspacing="0">
                <tbody width="100%">
                    <tr style="color:#333333;font-size:13px;line-height:1.4;" width="100%">
                        <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid none none solid;border-top-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">${msg("total-synced-users")}<br aria-hidden="true"></td>
                        <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid none none solid;border-top-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">${msg("users-added")}<br aria-hidden="true"></td>
                        <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid none none solid;border-top-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">${msg("users-updated")}<br aria-hidden="true"></td>
                        <#--  <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid none none solid;border-top-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">Contacts skipped<br aria-hidden="true"></td>  -->
                        <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid solid none solid;border-top-width:1px;border-right-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-right-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">${msg("failed-import")}<br aria-hidden="true"></td>
                    </tr>
                    <tr style="color:#333333;font-size:13px;line-height:1.4;">
                        <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid none solid solid;border-top-width:1px;border-bottom-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-bottom-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">${total_synced!}<br aria-hidden="true"></td>
                        <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid none solid solid;border-top-width:1px;border-bottom-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-bottom-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">${added_users!}<br aria-hidden="true"></td>
                        <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid none solid solid;border-top-width:1px;border-bottom-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-bottom-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">${updated_users!}<br aria-hidden="true"></td>
                        <#--  <td width="20%" style="color:#333333;font-size:13px;padding:15px 7.5px;border-style:solid none solid solid;border-top-width:1px;border-bottom-width:1px;border-left-width:1px;border-top-color:${bodyColor};border-bottom-color:${bodyColor};border-left-color:${bodyColor};line-height:1.4;">3<br aria-hidden="true"></td>  -->
                        <td width="20%" style="color:#D72D30;font-size:13px;font-weight:700;padding:15px 7.5px;border:1px solid ${bodyColor};line-height:1.4;">${failed_users!}<br aria-hidden="true"></td>
                    </tr>
                </tbody>
            </table>  
            <p style="margin: 20px 0;line-height:1.4;">${msg("adsyncThanks")}<br/>${msg("adsyncTeam")} <strong>${variant}</strong></p>
        </div>
       <#if variant == "ezto auth">
            <p>${msg("happy-authenticating")}</p>
         </#if>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>