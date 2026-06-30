<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       ${msg("accountSuccessTitle")}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">  
            <#if emlForOldAccCreator?? && emlForOldAccCreator>
                ${kcSanitize(msg("accountSuccessForOACBody",oldAccCreatorEml,tnt,eml))?no_esc}
            <#else>
                ${kcSanitize(msg("accountSuccessBody",eml,tnt))?no_esc}
            </#if>
        </div> 
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>