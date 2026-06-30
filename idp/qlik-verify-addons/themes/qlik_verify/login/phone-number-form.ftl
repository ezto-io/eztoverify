<!-- MobNum Entering Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        <!-- Phone Number Library Scripts  -->
        <script defer src="${url.resourcesPath}/js/tele-input/tele-input.min.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${url.resourcesPath}/js/tele-input/utils.js?version=${serverVersion}" type="text/javascript"></script>
        <!-- Phone Number Library CSS -->
        <link rel="stylesheet" href="${url.resourcesPath}/js/tele-input/tele-input.min.css?version=${serverVersion}" type="text/css"/>
    </#if>
    <#if section = "form">
        <form id="phone-number-form" action="${url.loginAction}" method="post">
            <input name="phoneNumber" type="hidden" id="phone"/>
            <input name="countryCode" type="hidden" id="countryCode"/>
            <input name="phoneNumberRegion" type="hidden" id="phoneNumberRegion"/>
            <div class="title_sign">
              <h2>${msg("mob_num_form_title")}</h2>
            </div>
            <div class="form-group">
              <label>${msg("mob_num_form_input")}</label>
                <div class="w-100">
                    <#if oldPhone??>
                        <input id="phoneNumber" name="phoneNumber" type="tel" class="form-control" value="${oldPhone}" maxlength="20" oninput="checkPhoneNumberValid(this)">
                    <#else>
                        <input id="phoneNumber" name="phoneNumber" type="tel" class="form-control" maxlength="20" oninput="checkPhoneNumberValid(this)">
                    </#if>
                </div>
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
                <div id="error-div">
                    <p id="error-p" class="input-alert-text-error"></p>
                </div>
            </div>

            <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  " disabled="disabled" >
              ${msg("mob_num_form_submit_btn")}
            </button>
          <#if realm.password && registrationEnabled && !registrationDisabled??>
          <#import "create-account-link.ftl" as c>
          <@c.create_account_link />
          <#import "social-login.ftl" as s>
          <@s.social_login />
           </#if>
        </form>
        <script>
            const phoneNumberForm = document.querySelector("#phone-number-form");
            phoneNumberForm.addEventListener("submit",function(event){
                removeErrorByElementName();
                if (iti.isValidNumber()) {
                    $("#phone").val(iti.getNumber());
                    $("#countryCode").val("+"+iti.getSelectedCountryData().dialCode)
                    $("#phoneNumberRegion").val(iti.getSelectedCountryData().iso2)
                    $('#nextBtn').addClass('disabled');
                    $('#nextBtn').attr('disabled', 'disabled');
                    $("#phone-number-form").submit();
                } else {
                    event.preventDefault();
                    $('#nextBtn').removeClass('disabled');
                    $('#nextBtn').removeAttr('disabled');
                }
            })
            const input = document.querySelector("#phoneNumber");
            const backendError = document.querySelector("#be-error");
            if(backendError){
                addErrorByElementName("#phoneNumber");
            }
            const selected_flag= document.querySelector(".iti__selected-flag");
            const iti = window.intlTelInput(input, {
               <#if defReg??>
                    initialCountry: "${defReg}",
                <#else>
                    initialCountry: "in",
                </#if>
                utilsScript: "${url.resourcesPath}/js/tele-input/utils.js",
                separateDialCode: false
            });

            function phoneNumValidCheck(){
                if(!iti.isValidNumber()) {
                    $('#nextBtn').attr('disabled', 'disabled');
                    $('#nextBtn').addClass('disabled');
                }else {
                    $('#nextBtn').removeClass('disabled');
                    $('#nextBtn').removeAttr('disabled');
                }
            }
            
            function removeErrorByElementName() {
                var flagName = JSON.stringify(iti.getSelectedCountryData().iso2);
                flagName = flagName.toString().replace(/"/g, "");
                sessionStorage.setItem('flagName', flagName);
                var isValid = iti.isValidNumber();
                var errorDiv = document.getElementById("error-div");
                if(isValid) {
                    errorDiv.style.display = 'none';
                    $("#error-div p").text("");
                }
                else {
                    if($("#phoneNumber").val()) {
                        errorDiv.style.display = 'block';
                        $("#error-div p").text("Phone Number Invalid");
                        document.getElementById('error-p').textContent = "Phone Number Invalid";
                        addErrorByElementName("#phoneNumber");
                    }
                }
            }
            function removeErrorByElementId(elementName) {
                $(elementName).removeClass("error-box-outline");
            }

            function addErrorByElementName(elementName) {
                $(elementName).addClass("error-box-outline");
            }

            <#if oldPhoneNumber??>
            $("#phoneNumber").val("${oldPhoneNumber}");
            </#if>

            <#if error??>
                document.getElementById("error-div").style.display = 'block';
                $("#error-div p").text("${error}");
            </#if>

            const reset = function () {
                removeErrorByElementId("#phoneNumber");
                $("#error-div p").text("");
                if(backendError){
                    backendError.style.display = "none";
                }
            };
            input.addEventListener("input",function(event){ 
                if( event.target.value ){
                    reset();
                }
            })
            input.addEventListener('change', reset);
            input.addEventListener('countrychange',function(){
                reset();
                phoneNumValidCheck();
            });
            $("input[type=tel]").on("input",function() {
                let empty = false;
                $("input[type=tel]").each(function() {
                    if ($(this).val() == '') {
                        empty = true;
                    }
                });
            });
            $(function() {
                $('#phoneNumber ').on('keypress', function(e) {
                    if (e.which == 32){
                        return false;
                    }
                });
            });
        </script>
        <style>
            .iti.iti--allow-dropdown {
                display: block;
            }
        </style>
    </#if>
</@layout.registrationLayout>