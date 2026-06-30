<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("confirmLinkIdpTitle")}
    <#elseif section = "form">
        <form id="kc-register-form" action="${url.loginAction}" method="post">
            <div class="col-12">
                <div class="title_sign text-center">
                    <h2>${msg("welcomeBackGreet")}</h2>
                </div>
            </div>
            <div class="col-12 text-center">
                <p>${msg("upd_prof_hlp_txt")}</p> 
            </div>
            <div class="${properties.kcFormGroupClass!}">
                <button type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red cursor-unset" name="submitAction" id="updateProfile" value="updateProfile" >
                    ${msg("upd_prof_btn")}
                </button>
            </div>
            <div class="social-login-seperator margin-b-2">
                <p class="text-center font-s-13 c-gray">${msg("or")}</p>
            </div>
            <input type="hidden" id="linkAccount"/>
            <div class="col-12 mt-4 title_sign mb-0 text-center">
                <p class="continue-login-btn ${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" >
                    <span class="c-gray" >${msg("continueLogin")}</span>
                    <a class="font-w-500 cursor-pointer action-link-clr" onclick="skipUpdate()">  ${msg("doClickHere")} </a>
                </p>
            </div>
        </form>
        <script>
            function skipUpdate(){
               $('#linkAccount').attr('name', 'submitAction');
               $('#linkAccount').attr('value', 'linkAccount');
               $("#kc-register-form").submit();
            }
        </script>
    </#if>
</@layout.registrationLayout>
