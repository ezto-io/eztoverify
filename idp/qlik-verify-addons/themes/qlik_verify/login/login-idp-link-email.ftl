<!-- SL Link Email Sent FTL -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("emailLinkIdpTitle", idpAlias)}
    <#elseif section = "form">
        <form id="idp-link-email-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="nextStep" id="nextStep" val = "" />
            <input type="hidden" name="resendMail" id="resendMail" val = "" />
            <div class="title_sign text-center">
                <h2>${msg("emailLinkIdpTitle", idpAlias)}</h2>
            </div>
            <p id="instruction1" class="instruction text-center">
                ${msg("emailLinkIdp1", idpAlias, brokerContext.username, realm.displayName)}
            </p>
            <p id="instruction3" class="instruction text-center">
                ${msg("emailLinkIdp4")} ${msg("clickNextStep")} ${msg("emailLinkIdp5")}
            </p>
            <div class="${properties.kcFormGroupClass!}">
                <button id="instruction3" onclick="checkEmailVerified()" type="button" class="btn w-100 margin-t-3 btn_account bg-orange-red   cursor-unset" >
                ${msg("doNextStep")}
                </button>
            </div>
            <p id="instruction2" class="col-12 font-s-13 mb-1 margin-t-3 text-center">
                ${msg("email_not_received_msg")}
                <a onclick="resendMail()" class="action-link-clr cursor-pointer">
                    ${msg("otp_resend_btn")}
                </a>    
            </p>
        </form>
    </#if>
    <script defer type="text/javascript">
        function checkEmailVerified() {
            $("#nextStep").val("true");
            $("#idp-link-email-form").submit();
        }
        function resendMail() {
            $("#resendMail").val("true");
            $("#idp-link-email-form").submit();
        }

    </script>
</@layout.registrationLayout>