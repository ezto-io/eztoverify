<#--  MFA Reg Options Page  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="otp-login-form" action="${url.loginAction}" class="row" method="post">
            <input name="mfa_selection" value="true" type="hidden">
            <input id="skip_mfa" name="skipMFA" value="false" type="hidden">
            <div class="col-12">
                <div class="title_sign">
                    <h4>${msg("mfa-primary-header")}</h4>
                    <h5>${msg("mfa-secondary-header", variant)}</h5>
                </div>
                <p>${msg("mfa-desc")}</p>
            </div>

            <div class="col-12">
                <#assign checkedVal = 0 >
                <#list client_mfa_providers as prov>
                    <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                        <input type="radio" name="mfa_selected_id" id="${prov.id}" value="${prov.id}">
                        <label for="${prov.id}" ></label>
                        <span>${msg("${prov.displayName}")}</span>
                    </div>
                 </#list>
            </div>

            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red   cursor-unset">
                    ${msg("next")}
                </button>
                <#if !mfa_is_mandatory>
                    <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                        ${msg("skip-link")}
                        <a onclick="skipMFAFunc()" class="action-link-clr cursor-pointer">
                        ${msg("doClickHere")}
                        </a>
                    </p>
                </#if>
            </div>
        </form>
        <script>
            function skipMFAFunc(){
               $("#skip_mfa").val("true");
               $("#otp-login-form").submit();
            }
            $(document).ready(
                function(){
                    $("input:radio[name=mfa_selected_id]:first").attr('checked', true);
                }
            );
        </script>
    </#if>
</@layout.registrationLayout>