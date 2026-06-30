<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       ${msg("hardDeleteSuccessTitle")}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="margin:10px;color: #636363; font-size: 14px;">
            <#if emlForAccCreator?? && emlForAccCreator>
                ${kcSanitize(msg("hardDeleteSuccessForAcBody",acc_creator_email,tenant,user_name,link))?no_esc}
            <#else>
                ${kcSanitize(msg("hardDeleteSuccessBody",user_name,tenant))?no_esc}
            </#if>
        </div> 
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>