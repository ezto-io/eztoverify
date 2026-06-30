<!-- Recaptcha Entering Page -->

<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <div class="title_sign">
            <h2>${msg("recaptcha-title")}</h2>
        </div>
        <form class="row" action="${url.loginAction}" method="post">
            <#if recaptchaVersion == "V3">
                <input type="hidden" id="g-recaptcha-response" name="g-recaptcha-response">
            </#if>
            <div class="form-group">
                 <div class="${properties.kcInputWrapperClass!}">
                      <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                 </div>
            </div>
            <#import "error_msg.ftl" as e>
                            <@e.error_msg />
                            <#if error??>
                                <div>
                                    <p id="errorMsg">${error}</p>
                                    <style>
                                        #password, #confirmpassword{
                                            border-color: red;
                                        }
                                    </style>
                                </div>
            </#if>
            <div class="col-12">
                            <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
                                                            ${msg("pwd_form_submit_btn")}
                                                        </button>
            </div>
            <#if recaptchaVersion == "V3">
                <script>
                     var onRecaptchaLoaded = function() {
                          grecaptcha.execute('${recaptchaSiteKey}', { action:'${recaptchaActionName}' }).then(function(token) {
                               document.getElementById('g-recaptcha-response').value = token;
                          });
                     };
                </script>
            </#if>
        </form>
    </#if>
</@layout.registrationLayout>