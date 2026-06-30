<#ftl output_format="plainText">
<#if emlForAccCreator?? && emlForAccCreator>
    ${msg("hardDeleteSuccessForAcBody",accCreatorEml,tnt,eml)}
<#else>
    ${msg("hardDeleteSuccessBody",eml,tnt)}
</#if>