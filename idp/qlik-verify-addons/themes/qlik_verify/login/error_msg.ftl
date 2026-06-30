<#macro error_msg displayMessage=true>
    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
        <div id="be-error" class="input-alert-text-error">
            <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
            <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
            <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
            <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
            <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
        </div>
    </#if>
</#macro>