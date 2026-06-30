<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("loginTitle",realm.name)}
    <#elseif section = "header">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        ${msg("loginTitleHtml",realm.name)}
    <#elseif section = "form">
        <div class="title_sign">
        <#if clientName??>
            <h2>${msg("loginTotpHeader")}</h2>
            <div>
                <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
            </div>
        <#else>
            <h2>${msg("loginTotpHeader")}</h2>
        </#if>
        </div>
        <iframe class="duo_mfa" id="duo_iframe" data-host="${apihost}" data-sig-request="${sig_request}" data-post-action="${url.loginAction}">
        </iframe>
        <form id="kc-totp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" id="mfaProviderId" name="mfaProviderId" value="${mfaProviderId}" />
            <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
            <div class="form-group">
                <div id="kc-form-buttons">
                    <button type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red c-white rounded-8 cursor-unset" name="cancel" value="Cancel">${msg("doCancel")}</button>
                </div>
            </div>
            <#if showTroubleShoot?? && showTroubleShoot>
              <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                 <a onclick="troubleShootMFAFunc()" class="action-link-clr cursor-pointer">
                 ${msg("troubleshoot_mfa")}
                 </a>
              </p>
            </#if>
        </form>
        <script>
             function troubleShootMFAFunc(){
                  $("#troubleShootMFA").val("true");
                  $("#kc-totp-login-form").submit();
            }
        </script>
    </#if>
</@layout.registrationLayout>