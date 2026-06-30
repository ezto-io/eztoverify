<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="multiple-logion-options-form" action="${url.loginAction}" class="row" method="post">
          <div class="col-12">
            <div class="title_sign mb-2">
              <h2 class="mb-0" >${msg("mfa_opt_title")}</h2>
            </div>
          </div>
          <div class="col-12">
            <div class="form-group mb-0">
                <div>
                    <#if email??> 
                        <div class="mt-3 radio-wrap d-flex flex-row  align-items-center">
                            <input id="email" type="radio" name="chosenType" value="emailOtpOption" checked>
                            <label for="email"></label>
                            <span class="pl-3"> ${msg("mfa_opt_email_otp",email)}</span>
                        </div>
                    </#if>
                    <#if mobNum??>
                        <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                            <input id="mobNum" type="radio" name="chosenType" value="mobileOtpOption">
                            <label for="mobNum"></label>
                            <span class="pl-3"> ${msg("mfa_opt_mob_otp",mobNum)}</span>
                        </div>
                    </#if>
                </div>
                <button id="Submit" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
                    ${msg("mfa_opt_proceed_btn")}
                </button>
            </div>
          </div>
        </form>
        <script>
            $("input:radio[name='chosenType'][disabled=false]:first").attr('checked', true);
        </script>
    </#if>
</@layout.registrationLayout>