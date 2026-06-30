<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="multiple-reset-options-form" action="${url.loginAction}" class="row" method="post">
          <div class="col-12">
            <div class="title_sign mb-2">
              <h2 class="mb-0" >${msg("rp_opt_title")}</h2>
            </div>
          </div>
          <div class="col-12">
            <div class="form-group mb-0">
                <div>
                    <#if emailOtpReset??>
                        <div class="mt-3 radio-wrap d-flex flex-row  align-items-center">
                            <input id="emailOtpReset" type="radio" name="chosenType" value="emailOtpReset" checked>
                            <label for="emailOtpReset"></label>
                            <span> ${msg("rp_opt_email_otp")}</span>
                        </div>
                    </#if>
                    <#if emailLinkReset??>
                        <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                            <input id="emailLinkReset" type="radio" name="chosenType" value="emailLinkReset">
                            <label for="emailLinkReset"></label>
                            <span> ${msg("rp_opt_email_link")}</span>
                        </div>
                    </#if>
                    <#if mobOtpReset??>
                        <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                            <input id="mobOtpReset" type="radio" name="chosenType" value="mobOtpReset">
                            <label for="mobOtpReset"></label>
                            <span> ${msg("rp_opt_mob_otp")}</span>
                        </div>
                    </#if> 
                </div>
                <button id="Submit" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
                    ${msg("rp_opt_proceed_btn")}
                </button>
            </div>
          </div>
        </form>
        <script>
            $("input:radio[name='chosenType'][disabled=false]:first").attr('checked', true);
        </script>
    </#if>
</@layout.registrationLayout>