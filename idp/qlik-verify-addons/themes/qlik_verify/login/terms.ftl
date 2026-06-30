<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("termsTitle")}
    <#elseif section = "form">
    <div>
        <h2 class="title_sign"> ${msg("terms-and-conditions")} </h2>
    </div>
    <div id="kc-terms-text" class="tc-body">
        <#if tcContent?? &&  tcContent != "">
            ${kcSanitize(tcContent)?no_esc}
        <#else>
            ${kcSanitize(msg("termsText"))?no_esc}
        </#if>
    </div>
    <form class="form-actions d-flex justify-content-around margin-t-3 tc-action" action="${url.loginAction}" method="POST">
        <input class="btn w-30 btn_account bg-orange-red  " name="accept" id="kc-accept" type="submit" value="${msg("doAccept")}"/>
        <input class="btn w-30 btn_account  btn-orange-border" name="cancel" id="kc-decline" type="submit" value="${msg("doDecline")}"/>
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
