<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${kcSanitize(msg("webauthn-error-title"))?no_esc}
    <#elseif section = "form">
        <#--  Template Header  -->
        <h2 class="ciba-template-title mt-2">${msg("fido-auth")}</h2>
        <p class="ciba-template-subtitle text-dark mt-4 text-center" > ${msg("webauthn-error-desc")} ${msg("fido-try-again")}  </p> 
        <div class="col-12 text-center my-auto">
            <img class="error-img" alt="mail" src="/auth/assets/img/youbikey.svg?version=${serverVersion}" >
        </div> 
        <script defer type="text/javascript">
            refreshPage = () => {
                location.reload(true);
            }
        </script>

        <form id="kc-error-credential-form" class="${properties.kcFormClass!} mt-auto" action="${url.loginAction}" 
              method="post">
            <input type="hidden" id="executionValue" name="authenticationExecution"/>
            <input type="hidden" id="isSetRetry" name="isSetRetry"/>
           <input type="hidden" id="cancel" name="cancel" value="false"/>

        </form>

        <input tabindex="4" onclick="refreshPage()" type="button" 
               class="btn w-100 margin-t-3 btn_account bg-orange-red  "
               name="try-again" id="kc-try-again" value="${kcSanitize(msg("doTryAgain"))?no_esc}"
        />

        <input tabindex="4" onclick="cancel()" type="button"
               class="btn w-100 margin-t-3 btn_account bg-orange-red  "
               name="cancel" id="cancel" value="${kcSanitize(msg("doCancel"))?no_esc}"
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

        <script defer type="text/javascript">
            function troubleShootMFAFunc() {
                $("#troubleShootMFA").val("true");
                $("#kc-webauthn-form").submit();
            }

            function cancel() {
                $("#cancel").val("true");
                $("#kc-error-credential-form").submit();
            }
        </script>

    </#if>
</@layout.registrationLayout>