<#ftl output_format="plainText">
<#if emlForAccCreator?? && emlForAccCreator>
    ${msg("hardDeleteSuccessForAcBody",accCreatorEml,tenant,eml)}
<#else>
    ${msg("hardDeleteSuccessBody",user_name,tenant)}
</#if>