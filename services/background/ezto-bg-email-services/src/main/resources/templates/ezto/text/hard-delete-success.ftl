<#ftl output_format="plainText">
<#if emlForAccCreator?? && emlForAccCreator>
    ${msg("hardDeleteSuccessForAcBody",acc_creator_email,tenant,user_name)}
<#else>
    ${msg("hardDeleteSuccessBody",user_name,tenant)}
</#if>