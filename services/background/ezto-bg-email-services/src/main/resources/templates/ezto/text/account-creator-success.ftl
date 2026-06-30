<#ftl output_format="plainText">
<#if emlForOldAccCreator?? && emlForOldAccCreator>
    ${msg("accountSuccessForOACBody",oldAccCreatorEml,tenant,user_name)}
<#else>
    ${msg("accountSuccessBody",user_name,tenant)}
</#if>
