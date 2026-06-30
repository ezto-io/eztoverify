<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','email'); section>
    <#if section = "header">
        ${msg("loginProfileTitle")}
    <#elseif section = "form">
        <form id="kc-update-profile-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="title_sign">
                <h2>${msg("upd_prof_title")}</h2>
            </div>
            <#if user.username??>
                <div class="form-group">
                        <label for="username" >${msg("username")}</label> <span class="text-danger"> *</span>
                        <input oninput="disableButton()" type="text" id="username" name="username" value="${(user.username!'')}"
                               class="form-control disabled"
                               aria-invalid="<#if messagesPerField.existsError('username')>true</#if>" 
                               readonly
                        />
                        <#if messagesPerField.existsError('username')>
                            <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('username'))?no_esc}
                            </span>
                        </#if>
                </div>
            <#else>
                <div class="form-group">
                        <label for="username" >${msg("username")}</label> <span class="text-danger"> *</span>
                        <input oninput="disableButton()" type="text" id="username" name="username" value="${(user.username!'')}"
                               class="form-control"
                               aria-invalid="<#if messagesPerField.existsError('username')>true</#if>" 
                        />
                        <#if messagesPerField.existsError('username')>
                            <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('username'))?no_esc}
                            </span>
                        </#if>
                </div>
            </#if>

            <#if user.email??>
                <div class="form-group">
                        <label for="email">${msg("email")}</label> <span class="text-danger"> *</span>
                        <input oninput="disableButton()" type="text" id="email" name="email" value="${(user.email!'')}"
                            class="form-control disabled"
                            aria-invalid="<#if messagesPerField.existsError('email')>true</#if>" 
                            readonly
                        />

                        <#if messagesPerField.existsError('email')>
                            <span id="input-error-email" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('email'))?no_esc}
                            </span>
                        </#if>
                </div>
            <#else>
                <div class="form-group">
                        <label for="email">${msg("email")}</label> <span class="text-danger"> *</span>
                        <input oninput="disableButton()" type="text" id="email" name="email" value="${(user.email!'')}"
                            class="form-control"
                            aria-invalid="<#if messagesPerField.existsError('email')>true</#if>"
                        />

                        <#if messagesPerField.existsError('email')>
                            <span id="input-error-email" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('email'))?no_esc}
                            </span>
                        </#if>
                </div>
            </#if>

            <#if user.firstName??>
                <div class="form-group">
                        <label for="firstName">${msg("firstName")}</label> <span class="text-danger"> *</span>
                        <input oninput="disableButton()" type="text" id="firstName" name="firstName" value="${(user.firstName!'')}"
                            class="form-control disabled"
                            aria-invalid="<#if messagesPerField.existsError('firstName')>true</#if>" 
                            readonly
                        />

                        <#if messagesPerField.existsError('firstName')>
                            <span id="input-error-firstname" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('firstName'))?no_esc}
                            </span>
                        </#if>
                </div>
            <#else>
                <div class="form-group">
                        <label for="firstName">${msg("firstName")}</label> <span class="text-danger"> *</span>
                        <input oninput="disableButton()" type="text" id="firstName" name="firstName" value="${(user.firstName!'')}"
                            class="form-control"
                            aria-invalid="<#if messagesPerField.existsError('firstName')>true</#if>"
                        />

                        <#if messagesPerField.existsError('firstName')>
                            <span id="input-error-firstname" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('firstName'))?no_esc}
                            </span>
                        </#if>
                </div>
            </#if>

            <#if user.lastName??>
                <div class="form-group">
                        <label for="lastName">${msg("lastName")}</label> <span class="text-danger"> *</span>
                        <input oninput="disableButton()" type="text" id="lastName" name="lastName" value="${(user.lastName!'')}"
                            class="form-control disabled"
                            aria-invalid="<#if messagesPerField.existsError('lastName')>true</#if>"
                            readonly
                        />
                        <#if messagesPerField.existsError('lastName')>
                            <span id="input-error-lastname" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('lastName'))?no_esc}
                            </span>
                        </#if>
                </div>
            <#else>
                <div class="form-group">
                    <label for="lastName">${msg("lastName")}</label> <span class="text-danger"> *</span>
                    <input oninput="disableButton()" type="text" id="lastName" name="lastName" value="${(user.lastName!'')}"
                        class="form-control"
                        aria-invalid="<#if messagesPerField.existsError('lastName')>true</#if>"
                    />
                    <#if messagesPerField.existsError('lastName')>
                        <span id="input-error-lastname" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('lastName'))?no_esc}
                        </span>
                    </#if>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                 <div id="kc-form-buttons">
                    <#if isAppInitiatedAction??>
                        <button id="submitBtn" class="btn w-100 margin-t-3 btn_account bg-orange-red   " type="submit" >${msg("doSubmit")}</button>
                        <button class="btn w-100 margin-t-3 btn_account bg-orange-red  " type="submit" name="cancel-aia" value="true" >${msg("doCancel")}</button>
                    <#else>
                        <button id="submitBtn" class="btn w-100 margin-t-3 btn_account bg-orange-red   " type="submit"  >${msg("doSubmit")}</button>
                    </#if>
                </div>
            </div>
        </form>
    </#if>
    <script>
        const Configuration = {
            EmailRegex: /^(([^<>!%&*#$()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,63}))$/,
            Spacing: /\s/,
          };
        disableButton();
        function validateEmail(email){
            if( email && email.match(Configuration.EmailRegex)){
              return true;
            } else {
              return false;
            }
        }
        function disableButton(){
            let name = $("#username").val();
            let emailId = $("#email").val();
            let firstName = $("#firstName").val();
            let lastName = $("#lastName").val();

            <#assign userDetail = user>
            if(name && emailId && validateEmail(emailId.trim()) && firstName && lastName ){
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
