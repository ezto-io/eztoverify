<#macro group_list items>
  <#if items?size == 0><#return></#if>
  <#local sortedItems = items?sort_by(['period', 'start'])>
  <#local groupStart = 0>
  <#list sortedItems as item>
    <#if !item?is_first && ((item.period.start)?replace("," , "")?number * 1000)?number_to_date?string.short != ((lastItem.period.start)?replace("," , "")?number * 1000)?number_to_date?string.short>
      <#local groupEnd = item?index>
      <#nested lastItem.period.start, lastItem.period.end, sortedItems[groupStart ..< groupEnd]>
      <#local groupStart = groupEnd>
    </#if>
    <#local lastItem = item>
  </#list>
  <#local groupEnd = sortedItems?size>
  <#nested lastItem.period.start, lastItem.period.end, sortedItems[groupStart ..< groupEnd]>
</#macro>