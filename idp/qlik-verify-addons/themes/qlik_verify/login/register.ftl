<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>
    <#if section = "header">
        ${msg("registerTitle")}
    <#elseif section = "form">
        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">
            <div class="title_sign">
                <h2>${msg("SignUpToQlikVerify", msg("qlik-verify-caps"))}</h2>
            </div>
            <div  class="form-group">
                    <label for="firstName">${msg("firstName")}</label>
                    <input oninput="disableButton()"  type="text" id="firstName" class="${properties.formInputClass!}" name="firstName" placeholder="${msg("firstname-placeholder")}"
                           value="${(register.formData.firstName!'')}"
                    />
                    <#if messagesPerField.existsError('firstName')>
                        <span id="input-error-firstname" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('firstName'))?no_esc}
                        </span>
                    </#if>
            </div>

            <div  class="form-group">
                    <label for="lastName">${msg("lastName")}</label>
                    <input oninput="disableButton()"  type="text" id="lastName" class="${properties.formInputClass!}" name="lastName" placeholder="${msg("lastname-placeholder")}"
                           value="${(register.formData.lastName!'')}"
                    />

                    <#if messagesPerField.existsError('lastName')>
                        <span id="input-error-lastname" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('lastName'))?no_esc}
                        </span>
                    </#if>
            </div>
            <div class="form-group">
                    <label for="email">${msg("email")}</label>
                    <input oninput="disableButton()"  type="text" id="email" class="${properties.formInputClass!}" name="email" placeholder="${msg("email-address-placeholder")}"
                           value="${(register.formData.email!'')}" autocomplete="email"
                    />

                    <#if messagesPerField.existsError('email')>
                        <span id="input-error-email" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('email'))?no_esc}
                        </span>
                    </#if>
            </div>

            <#if !realm.registrationEmailAsUsername>
                <div class="form-group">
                        <label for="username">${msg("username")}</label>
                        <input oninput="disableButton()"  type="text" id="username" class="${properties.formInputClass!}" name="username" placeholder="${msg("username-placeholder")}"
                               value="${(register.formData.username!'')}" autocomplete="username"
                        />

                        <#if messagesPerField.existsError('username')>
                            <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('username'))?no_esc}
                            </span>
                        </#if>
                </div>
            </#if>

            <#if passwordRequired??>
                <div class="form-group --password" id="show_hide_password">
                        <label for="password">${msg("password")}</label>
                    <div class="input-group">
                        <input oninput="disableButton()"  type="password" id="password" class="${properties.formInputClass!}" name="password" placeholder="${msg("password")}"
                               autocomplete="new-password"
                        />
                        <div class="input-group-prepend hide_show">
                         <a href=""><span class="input-group-text tio hidden_outlined"></span></a>
                        </div>
                    </div>
                        <#if messagesPerField.existsError('password')>
                            <span id="input-error-password" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('password'))?no_esc}
                            </span>
                        </#if>
                </div>
                <div class="form-group --password" id="show_hide_password2">
                        <label for="password-confirm">${msg("passwordConfirm")}</label>
                    <div class="input-group">
                        <input oninput="disableButton()" type="password" id="password-confirm" class="${properties.formInputClass!}" placeholder="${msg("passwordConfirm")}"
                               name="password-confirm"
                        />
                        <div class="input-group-prepend hide_show">
                         <a href=""><span class="input-group-text tio hidden_outlined"></span></a>
                        </div>
                    </div>
                        <#if messagesPerField.existsError('password-confirm')>
                            <span id="input-error-password-confirm" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                            </span>
                        </#if>
                </div>
            </#if>

            <#if recaptchaRequired??>
                <div class="form-group">
                        <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                </div>
            </#if>

            <div class="terms"><p>${msg("sign-up")} <a href="#">${msg("terms-conditions-link")}</a></p></div>
                <div id="kc-form-buttons" class="form-group">
                    <button id="regBtn" class="btn w-100 margin-t-3 btn_account bg-orange-red  " type="submit" value="${msg("doRegister")}" disabled="disabled" >
                    ${msg("doRegister")}
                    </button>
                </div>
            <#import "login-account-link.ftl" as l>
            <@l.login_account_link />
        </form>
    </#if>
    <script>
        $("#show_hide_password2").on('click', 'a', function (event) {
        event.preventDefault();
        if ($('#show_hide_password2 input').attr("type") == "text") {
            $('#show_hide_password2 input').attr('type', 'password');
            $('#show_hide_password2 .hide_show span').addClass("hidden_outlined");
            $('#show_hide_password2 .hide_show span').removeClass("visible_outlined");
        } else if ($('#show_hide_password2 input').attr("type") == "password") {
            $('#show_hide_password2 input').attr('type', 'text');
            $('#show_hide_password2 .hide_show span').removeClass("hidden_outlined");
            $('#show_hide_password2 .hide_show span').addClass("visible_outlined");
        }
        });
        const Configuration = {
            EmailRegex: /^(([^<>!%&*#$()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,63}))$/,
            Spacing: /\s/,
          };
        function validateEmail (email){
            if( email && email.match(Configuration.EmailRegex)  ){
              return true;
            } else {
              return false;
            }
          }
        function checkNull(){
            let fn = $("#firstName").val();
            let ln = $("#lastName").val();
            let email = $("#email").val();
            let un = $("#username").val();
            let pwd = $("#password").val();
            let cnpwd = $("#password-confirm").val();
            let emailValidation = validateEmail(email.trim())
            if( fn && ln && email && un && pwd && cnpwd && emailValidation){
                return true;
            }
            else{
                return false;
            }
        }
        function checkPasswordEqual(){
            let pwd = $("#password").val();
            let cnpwd = $("#password-confirm").val();
            if( pwd === cnpwd ){
                return true;
            }
            else{
                return false;
            }
        }

        function disableButton(){
            $("span.pf-m-error").css("display","none");
            let passwordEqual;
            $("#be-error").remove();
            let notNull = checkNull();
            if(notNull){
                passwordEqual = checkPasswordEqual();
            }
            if (!passwordEqual) {
                $('#regBtn').addClass('disabled cursor-unset');
                $('#regBtn').attr('disabled', 'disabled');
            } else {
                $('#regBtn').removeClass('disabled cursor-unset');
                $('#regBtn').removeAttr('disabled');
            }
        }
    </script>
</@layout.registrationLayout>