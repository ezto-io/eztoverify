<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       ${msg("custom-dns-status")} ${tnt!}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
           <p style="margin: 20px 0;line-height:1.4;">${msg("hi")} ${eml!},</p>
            <#if isDNSSet?? && isDNSSet>
                <p style="margin: 20px 0;line-height:1.4;">${msg("custom-dns-success")}</p>
            <#else>
                <p style="margin: 20px 0;line-height:1.4;">${msg("custom-dns-failure")}</p>
            </#if>
        </div>
        <div style="margin:40px 0px">
            <#if isDNSSet?? && isDNSSet>
                <p style="margin: 20px 0;line-height:1.4;">
                    ${msg("login-with-custom-domain")}<br/>
                    ${msg("redirect-link")} <br />
                    <span>${newDomain!}</span>
                </p>  
                <p style="margin: 20px 0;line-height:1.4;">
                    ${msg("help-text")} <br />
                    ${msg("reach-out")} <span><a href="${websiteContactLink}">${msg("support")}</a></span>.
                </p>
                <p style="margin: 20px 0;line-height:1.4;">${msg("thankyou-msg-1")}<br/>${msg("team")} <strong>${msg("qlik-verify")}</strong></p>
            <#else>
                <p style="margin: 20px 0;line-height:1.4;"> 
                    ${msg("sorry-msg")}<br/>
                    ${msg("contact-support")} <br />
                    <span>${oldDomain!}</span>
                </p>  
                <p style="margin: 20px 0;line-height:1.4;"> 
                    ${msg("reach-out")} <span><a href="${websiteContactLink}">${msg("support")}</a></span>.
                </p>
                <p style="margin: 20px 0;line-height:1.4;">${msg("thankyou-msg-2")}<br/>${msg("team")} <strong>${msg("qlik-verify")}</strong></p>
            </#if>
            
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div> 
</#macro>