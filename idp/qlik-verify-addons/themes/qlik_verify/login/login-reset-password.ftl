<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="d-flex justify-content-start title_sign margin-b-2 ">
                <p><a class="text-decoration-none" href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></p>
            </div>
            <div class="title_sign">
              <h2>${msg("reset-pwd-choose-email-title")}</h2>
              <p>${msg("reset-pwd-choose-email-desc")}</p>
            </div>
            <div class="form-group">
                    <label for="username"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                    <#if auth?has_content && auth.showUsername()>
                        <input oninput="disableButton()" type="text" id="username" name="username" placeholder="<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>" class="form-control" autofocus value="${auth.attemptedUsername}" aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>
                    <#else>
                        <input oninput="disableButton()" type="text" id="username" name="username" placeholder="<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>" class="form-control" autofocus aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>
                    </#if>

                    <#if messagesPerField.existsError('username')>
                        <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('username'))?no_esc}
                        </span>
                    </#if>
            </div>
            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                <div id="kc-form-buttons">
                    <button id="submitBtn" class="btn w-100 margin-t-3 btn_account bg-orange-red  " type="submit" value="${msg("doSubmit")}" disabled="disabled">
                        ${msg("doProceed")}
                    </button>
                </div>
            </div>
            <#if realm.password && registrationEnabled && !registrationDisabled??>
                <div class="other_login margin-b-2" >
                    <div class="line-or margin-b-2">
                        <span class="or forgot-password-container">${msg("or-login")}</span>
                    </div>
                </div>
                <#import "create-account-link.ftl" as c>
                <@c.create_account_link />
            </#if>
        </form>
    <#elseif section = "info" >
        ${msg("emailInstruction")}
    </#if>
    <script>
        $( document ).ready(function() {
            disableButton();
        });
        function disableButton(){
        $('#input-error').hide();
        let userName = $("#username").val();
        if( userName){
            $('#submitBtn').removeClass('disabled');
            $('#submitBtn').removeAttr('disabled');
        }
        else{
            $('#submitBtn').addClass('disabled');
            $('#submitBtn').attr('disabled', 'disabled');
        }
    }    
    </script>
</@layout.registrationLayout>
