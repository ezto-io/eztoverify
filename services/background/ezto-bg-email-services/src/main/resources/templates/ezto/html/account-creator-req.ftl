<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       ${msg("accountCreatorReqTitle")}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            ${kcSanitize(msg("accountCreatorReqBody",to,new_acc_creator_email,bodyColor,code,expiry,supportLink))?no_esc}
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
