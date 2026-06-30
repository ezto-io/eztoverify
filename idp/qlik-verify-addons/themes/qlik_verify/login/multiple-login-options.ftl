<!-- Multi Login Option Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="multiple-logion-options-form" action="${url.loginAction}" class="row" method="post">
          <div class="col-12">
            <div class="title_sign mb-2">
              <h2 class="mb-0" >${msg("mul_opt_title")}</h2>
            </div>
          </div>
          <div class="col-12">
            <div class="form-group mb-0">
                <div>
                    <#if emailOtpLoginOption??>
                        <div class="mt-3 radio-wrap d-flex flex-row  align-items-center">
                            <input id="emailOtpLoginOption" type="radio" name="chosenType" value="emailOtpLoginOption" checked>
                            <label for="emailOtpLoginOption"></label>
                            <span> ${msg("mul_opt_email_otp")}</span>
                        </div>
                    </#if>
                    <#if emailMagicLinkLoginOption??>
                        <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                            <input id="emailMagicLinkLoginOption" type="radio" name="chosenType" value="emailMagicLinkLoginOption">
                            <label for="emailMagicLinkLoginOption"></label>
                            <span> ${msg("mul_opt_email_magic_link")}</span>
                        </div>
                    </#if>
                    <#if mobileOtpLoginOption??>
                        <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                            <input id="mobileOtpLoginOption" type="radio" name="chosenType" value="mobileOtpLoginOption">
                            <label for="mobileOtpLoginOption"></label>
                            <span> ${msg("mul_opt_mob_otp")}</span>
                        </div>
                    </#if>
                    <#if passwordLoginOption??>
                        <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                            <input id="passwordLoginOption" type="radio" name="chosenType" value="passwordLoginOption">
                            <label for="passwordLoginOption"></label>
                            <span>${msg("mul_opt_password")}</span>
                        </div>
                    </#if>
                    <#if emailPhoneLoginOption??>
                        <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                            <input id="emailPhoneLoginOption" type="radio" name="chosenType" value="emailPhoneLoginOption">
                            <label for="emailPhoneLoginOption"></label>
                            <span>${msg('mul-otp-email-mob-otp')}</span>
                        </div>
                    </#if>
                </div>
                <button id="Submit" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
                    ${msg("mul_opt_proceed_btn")}
                </button>
            </div>
          </div>
        </form>
        <script>
            $("input:radio[name='chosenType'][disabled=false]:first").attr('checked', true);
        </script>
    </#if>
</@layout.registrationLayout>