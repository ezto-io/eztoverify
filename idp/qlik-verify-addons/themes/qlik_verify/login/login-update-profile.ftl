<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','email','firstName','lastName'); section>
      <#if section = "header">
        <!-- Phone Number Library Scripts  -->
        <script defer src="${url.resourcesPath}/js/tele-input/tele-input.min.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${url.resourcesPath}/js/tele-input/utils.js?version=${serverVersion}" type="text/javascript"></script>
        <!-- Phone Number Library CSS -->
        <link rel="stylesheet" href="${url.resourcesPath}/js/tele-input/tele-input.min.css?version=${serverVersion}" type="text/css"/>
        
    </#if>
    <#elseif section = "form">
        <form id="kc-update-profile-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <input name="isUpdateProfile" value="true" type="hidden">
            <div class="title_sign">
                <h2>${msg("upd_prof_title")}</h2>
            </div>
            <#if user.editUsernameAllowed>
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
            <#--  EMAIL  -->
            <#if updateMail?? && updateMail>
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
             <#--  MOB NUM  -->
            <#if updateMob?? && updateMob>
                <input type="hidden" id="updateMob" name="updateMob" value="true" />
                <input name="phoneNumber" type="hidden" id="phone"/>
                <input name="countryCode" type="hidden" id="countryCode"/>
                <input name="phoneNumberRegion" type="hidden" id="phoneNumberRegion"/>
                <div class="form-group">
                    <label class="phonenumber-label">${msg("mobile_num_input")}<span class="text-danger"> *</span></label> 
                    <div class="w-100">
                        <#if oldPhone??>
                            <input oninput="disableButton()" id="phoneNumber" name="phoneNumber" type="tel" class="form-control" value="${oldPhone}" maxlength="20" oninput="checkPhoneNumberValid(this)">
                        <#else>
                            <input oninput="disableButton()" id="phoneNumber" name="phoneNumber" type="tel" class="form-control" maxlength="20" oninput="checkPhoneNumberValid(this)">
                        </#if>
                    </div>
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
          <#if updateMob?? && updateMob>
             var input = document.querySelector("#phoneNumber");
            <#if user.attributes.mobileNumber?? && user.attributes.mobileNumberRegion??>
                $("#phoneNumber").val("${user.attributes.mobileNumber}");
            </#if> 
            var selected_flag= document.querySelector(".iti__selected-flag");
            var iti = window.intlTelInput(input, {
                <#if user.attributes.mobileNumberRegion??>
                    initialCountry: "${user.attributes.mobileNumberRegion}",
                <#elseif defReg??>
                    initialCountry: "${defReg}",
                <#else>
                    initialCountry: "in",
                </#if>
                utilsScript: "${url.resourcesPath}/js/tele-input/utils.js",
                separateDialCode: false
            });
            function validateMobileNumber(){
                if(iti.isValidNumber()){ 
                    $("#phone").val(iti.getNumber());
                    $("#countryCode").val("+"+iti.getSelectedCountryData().dialCode)
                    console.log("region", iti.getSelectedCountryData().iso2)
                    $("#phoneNumberRegion").val(iti.getSelectedCountryData().iso2)
                    return true;
                } 
                return false;
            }
            input.addEventListener("countrychange", function() {
                disableButton();
            });
         </#if>
        disableButton();
        function validateEmail(email){
            if( email && email.match(Configuration.EmailRegex)  ){
              return true;
            } else {
              return false;
            }
        }
    function disableButton(){
        let name = $("#username").val();
        let emailId = $("#email").val();
        let firstNameValue = $("#firstName").val();
        let lastNameValue = $("#lastName").val();
        <#assign userDetail = user>
        <#if userDetail.editUsernameAllowed >
            if(name && firstNameValue && lastNameValue  <#if updateMail?? && updateMail> && emailId && validateEmail(emailId.trim())</#if><#if updateMob?? && updateMob>&& validateMobileNumber()</#if>){
                $('#submitBtn').removeClass('disabled');
                $('#submitBtn').removeAttr('disabled');
            }
            else{
                $('#submitBtn').addClass('disabled');
                $('#submitBtn').attr('disabled', 'disabled');
            }
        <#else>
            if(firstNameValue && lastNameValue <#if updateMail?? && updateMail> && emailId && validateEmail(emailId.trim())</#if><#if updateMob?? && updateMob>&& validateMobileNumber()</#if>){
                $('#submitBtn').removeClass('disabled');
                $('#submitBtn').removeAttr('disabled');
            }
            else{
                $('#submitBtn').addClass('disabled');
                $('#submitBtn').attr('disabled', 'disabled');
            }
        </#if>
        
      }    
    </script>
</@layout.registrationLayout>
