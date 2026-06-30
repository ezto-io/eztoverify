<#include "index.ftl">
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
   <div style="color: #636363; font-size: 14px;">
      <p> ${msg("hello")}there, </p>
      <p>${msg("email-verify-desc")}</p>
   </div>
   <div style="border: 2px solid ${bodyColor}; padding: 40px; margin: 40px 0px;">
      <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">${msg("ignore-msg-btn")} </h4>
      <div style="text-align: center"> <a href="${link}" style="padding: 8px 20px; background-color: ${bodyColor}; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("verify-email")} </a> </div>
      <div style="text-align: center;margin-top:10px"> ${msg("verify-using-link")}</div>
      <p style="word-break: break-all;">${link}</p>
      <#if link_expiration??>
         <table style="width: 100%;">
            <tr>
               <td style="font-size: 14px"> ${msg("link-expires-in")} <b>${linkExpirationFormatter(link_expiration)}.</b> </td>
            </tr>
         </table>
      </#if>
   </div>
    <#import "email-footer.ftl" as s>
    <@s.email_footer />
</div>
</#macro> 