<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("logoutConfirmTitle")}
    <#elseif section = "form">
        <div id="kc-logout-confirm" class="content-area">
            <form class="form-actions" action="${url.logoutConfirmAction}" method="POST">
                <input type="hidden" name="session_code" value="${logoutConfirm.code}">
                <div class="title_sign">
                    <h2>${msg("confirmLogoutTittle")}</h2>
                </div>
                <p class="instruction">${msg("confirmLogoutDesc")}</p>
                <#if (client.baseUrl)?has_content>
                <div class="form-actions d-flex justify-content-around margin-t-3 tc-action action-parent">
                    <input class="btn w-30 btn_account bg-orange-red" name="confirmLogout" id="kc-accept" type="submit" value="${msg("doLogout")}"/>
                    <input class="btn w-60 btn_account  btn-orange-border" onclick="skip()" value="${kcSanitize(msg("backToApplication"))?no_esc}"/>
                </div>
                <#else>
                    <input name="confirmLogout" id="kc-logout" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red" value="${msg("doLogout")}" />
                </#if>

            <div class="clearfix"></div>
            </form>
        </div>
    </#if>
    <script>
        <#if (client.baseUrl)?has_content>
            function skip() {
                window.location.href = "${client.baseUrl}";
            }
        </#if>
    </script>
</@layout.registrationLayout>