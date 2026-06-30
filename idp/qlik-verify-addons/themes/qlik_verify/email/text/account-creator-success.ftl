<#ftl output_format="plainText">
<#if emlForOldAccCreator?? && emlForOldAccCreator>
    ${msg("accountSuccessForOACBody",oldAccCreatorEml,tnt,eml)}
<#else>
    ${msg("accountSuccessBody",eml,tnt)}
</#if>
