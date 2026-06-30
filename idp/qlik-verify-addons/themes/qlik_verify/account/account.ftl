<#import "template.ftl" as layout>
<@layout.mainLayout active='account' bodyClass='user'; section> 
    <div class="element-wrapper">
        <h1 class="element-header mb-0">${msg("editAccountHtmlTitle")}</h1>
    </div>  
    <div class="element-box">
        <form id="profile-page" action="${url.accountUrl}" class="form-horizontal" method="post">
            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
            <#if !realm.registrationEmailAsUsername>
                <div class="form-group row ${messagesPerField.printIfExists('username','has-error')}">
                    <div class="col-sm-2 col-md-2">
                        <label for="username" class="control-label">${msg("username")}</label> <#if realm.editUsernameAllowed><span class="required">*</span></#if>
                    </div>
                    <div class="col-sm-10 col-md-10">
                        <input type="text" class="form-control" id="username" name="username" required <#if !realm.editUsernameAllowed>disabled="disabled"</#if> value="${(account.username!'')}"/>
                    </div>
                </div>
            </#if>

            <div class="form-group row ${messagesPerField.printIfExists('email','has-error')}">
                <div class="col-sm-2 col-md-2">
                <label for="email" class="control-label">${msg("email")}</label> <span class="required">*</span>
                </div>

                <div class="col-sm-10 col-md-10">
                    <input type="text" class="form-control" id="email" name="email" autofocus value="${(account.email!'')}" required/>
                </div>
            </div>

            <div class="form-group row ${messagesPerField.printIfExists('firstName','has-error')}">
                <div class="col-sm-2 col-md-2">
                    <label for="firstName" class="control-label">${msg("firstName")}</label> <span class="required">*</span>
                </div>

                <div class="col-sm-10 col-md-10">
                    <input type="text" class="form-control" id="firstName" name="firstName" value="${(account.firstName!'')}" maxlength="40" required/>
                </div>
            </div>

            <div class="form-group row ${messagesPerField.printIfExists('lastName','has-error')}">
                <div class="col-sm-2 col-md-2">
                    <label for="lastName" class="control-label">${msg("lastName")}</label> <span class="required">*</span>
                </div>

                <div class="col-sm-10 col-md-10">
                    <input type="text" class="form-control" id="lastName" name="lastName" value="${(account.lastName!'')}" maxlength="40" required/>
                </div>
            </div> 
            <div class="form-group row ${messagesPerField.printIfExists('phoneNumber','has-error')}">
                <div class="col-sm-2 col-md-2">
                    <label for="mobileNumber" class="control-label">${msg("phoneNumber")}</label> <span class="required">*</span>
                </div>
                
                <div class="col-sm-10 col-md-10">
                    <input type="tel" class="form-control" id="mobileNumber" name="mobileNumber" maxlength="20" oninput="phoneNumberSanitation(event)" value="${(account.attributes.mobileNumber!'')}" required oninput/>
                    <input name="mobileNumberRegion" type="hidden" id="mobileNumberRegion" value="${account.attributes.mobileNumberRegion!realm.defaultRegion}"/>
                </div>
            </div> 

            <div class="form-group row mb-4">
                <div id="kc-form-buttons">
                    <div>
                        <#if url.referrerURI??><a href="${url.referrerURI}">${kcSanitize(msg("backToApplication")?no_esc)}</a></#if>
                        <button type="submit" id="nextBtn" class="btn btn-primary" name="submitAction" value="Save" disabled>${msg("doSave")}</button>
                        <button onClick="refreshPage(event)" class="btn btn-secondary disabled" name="submitAction" value="Cancel" disabled>${msg("doCancel")}</button>
                    </div>
                </div>
            </div>
        </form>
    </div>

    <script>
        const phoneNumberForm = document.querySelector("#profile-page");
        phoneNumberForm.addEventListener("submit",function(event){
            if (iti.isValidNumber()) {
                $("#mobileNumber").val(iti.getNumber());
                $("#countryCode").val("+"+iti.getSelectedCountryData().dialCode)
                $("#mobileNumberRegion").val(iti.getSelectedCountryData().iso2)
                $('#nextBtn').addClass('disabled');
                $('#nextBtn').attr('disabled', 'disabled');
                $("#profile-page").submit();
            } else {
                event.preventDefault();
                $('#nextBtn').removeClass('disabled');
                $('#nextBtn').removeAttr('disabled');
            }
        })
        const input = document.querySelector("#mobileNumber");
        const selected_flag= document.querySelector(".iti__selected-flag");
        const iti = window.intlTelInput(input, {
            <#if account.attributes.mobileNumberRegion??>
                initialCountry: "${account.attributes.mobileNumberRegion}",
            <#else>
                initialCountry: "${realm.defaultRegion}",
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
        
        function removeErrorByElementId(elementName) {
            $(elementName).removeClass("error-box-outline");
        }

        function addErrorByElementName(elementName) {
            $(elementName).addClass("error-box-outline");
        }

        function phoneNumberSanitation(event){
            var mobileNumberRegex = new RegExp('^[0-9]+$');
            if(!mobileNumberRegex.test(event.target.value)){
                event.target.value=event.target.value.replace(/\D/g,'');
            }

        }

        <#if oldPhoneNumber??>
        $("#mobileNumber").val("${oldPhoneNumber}");
        </#if>

        <#if error??>
            document.getElementById("error-div").style.display = 'block';
            $("#error-div p").text("${msg('${error}')}");
        </#if>

        const reset = function () {
            removeErrorByElementId("#mobileNumber");
            $("#error-div p").text("");
        };
        input.addEventListener("input",function(event){ 
            if(event.target.value){
                reset();
            }
        })
        input.addEventListener('blur', function () {
            reset();
            if (input.value.trim()) {
                if (iti.isValidNumber()) {
                    $("#error-div p").text("");
                } else {
                    addErrorByElementName("#mobileNumber");
                    $("#error-div p").text("Phone Number Invalid");
                }
            }
        });
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
            $('#phoneNumber').on('keypress', function(e) {
                if (e.which == 32){
                    return false;
                }
            });
        });
    </script>
</@layout.mainLayout>
