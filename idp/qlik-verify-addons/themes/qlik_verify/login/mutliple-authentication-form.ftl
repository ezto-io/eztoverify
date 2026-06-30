<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="firstname-lastname-form" action="${url.loginAction}" class="row" method="post">
          <input type="hidden" name="isAuthOptionSelectionForm" value="true">
          <div class="col-12">
            <div class="title_sign">
              <h2>${msg("choose-authentication")}</h2>
              <p>${msg("mfa-selection")}</p>
            </div>
          </div>
          <#if containsEmail?? || containsPhoneNumber??>
              <div class="col-12">
                <div class="form-check">
                  <input class="form-check-input" type="checkbox" value="" id="flexCheckDefault" name="otp">
                  <label class="form-check-label" >
                    ${msg("otp")}
                  </label>
                </div>
                <div class="form-check">
                  <input class="form-check-input" type="checkbox" value="" id="flexCheckChecked" name="magicLink" checked>
                  <label class="form-check-label" >
                    ${msg("magic-link")}
                  </label>
                </div>
          </#if>
            <div class="form-check">
              <input class="form-check-input" type="checkbox" value="" name="yubikey" id="flexCheckChecked">
              <label class="form-check-label" >
                ${("yubikey")}
              </label>
            </div>
          </div>

          <div class="col-12">
            <button type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
              ${msg("next")}
            </button>
          </div>
        </form>
    </#if>
</@layout.registrationLayout>