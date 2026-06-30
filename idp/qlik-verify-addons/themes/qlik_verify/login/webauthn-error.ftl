<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${kcSanitize(msg("webauthn-error-title"))?no_esc}
    <#elseif section = "form">
        <div class="title_sign text-center">
             <h2>${msg("webauthn-auth-mfa")}</h2>
        </div>
        <p class="text-center" > ${msg("webauthn-error-desc")}  </p> 
        <div class="col-12 text-center mb-2">
            <img class="mb-2 error-img" alt="mail" src="${url.resourcesPath}/img/youbikey.svg" >
        </div> 
        <p class="text-center" > ${msg("webauthn-try-again")} </p> 
        <script type="text/javascript">
            refreshPage = () => {
                location.reload(true);
            }
        </script>

        <form id="kc-error-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
              method="post">
            <input type="hidden" id="executionValue" name="authenticationExecution"/>
            <input type="hidden" id="isSetRetry" name="isSetRetry"/>
        </form>

        <input tabindex="4" onclick="refreshPage()" type="button"
               class="btn w-100 margin-t-3 btn_account bg-orange-red  "
               name="try-again" id="kc-try-again" value="${kcSanitize(msg("doTryAgain"))?no_esc}"
        />


        <#if isAppInitiatedAction??>
            <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-webauthn-settings-form" method="post">
                <button type="submit"
                        class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                        id="cancelWebAuthnAIA" name="cancel-aia" value="true"/>${msg("doCancel")}
                </button>
            </form>
        </#if>
        <#if showTroubleShoot?? && showTroubleShoot>
            <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                <a onclick="troubleShootMFAFunc()" class="action-link-clr cursor-pointer">${msg("troubleshoot_mfa")}</a>
            </p>
        </#if>

        <#if showTroubleShoot??>
            <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-webauthn-form" method="post">
                <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
            </form>
        </#if>

        <script type="text/javascript">
            function troubleShootMFAFunc(){
                $("#troubleShootMFA").val("true");
                $("#kc-webauthn-form").submit();
            }
        </script>

    </#if>
</@layout.registrationLayout>