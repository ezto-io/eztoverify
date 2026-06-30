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
       <form id="json-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">
            <div class="title_sign">
                <h2 style="
                        <#if formDisplayTitleAlign??> text-align:${formDisplayTitleAlign};</#if>
                        <#if formDisplayTitleClr??> color:${formDisplayTitleClr}; </#if> ">
                        ${(formDisplayTitle!"formDisplayTitle")?replace('\n','<br/>')?no_esc}
                </h2>
            </div>
            <p style="
                    <#if formDescriptionAlign??> text-align:${formDescriptionAlign};</#if>
                    <#if formDescriptionClr??> color:${formDescriptionClr}; </#if> ">
                 ${(formDescription!"formDescription")?replace('\n','<br/>')?no_esc}
            </p>
            <#if formInputs??>
                <#list formInputs as input> 
                    <#if input.getType()=='radio'>
                        <label >${input.getLabel()}</label>
                        <div class="form-group">  
                            <#if input.getOptions()??>
                                <#list input.getOptions() as option>
                                    <div class="flex-align-center">
                                        <input id="${input.getInputId()}"                         
                                            name="${input.getLabel()}" 
                                            type="${input.getType()}"
                                            value="${option}"
                                            class="form-check-input-1"
                                            placeholder="${input.getPlaceholderTxt() ! 'placeHolderTxt'}">
                                            <label class="mb-0 ml-2">${option}</label>
                                            <br>
                                    </div>   
                                </#list>
                            </#if>
                            <label >${(input.getLabelTxt())}</label>
                        </div>
                        <#else>
                            <div class="form-group">  
                                <label for="${input.getInputId()}">${(input.getLabelTxt())!'labelTxt'}</label>
                                <div class="w-100">
                                <#if input.getType()=='phoneNumber'>
                                    <input name="phoneNumber" type="hidden" id="phone"/>
                                    <input name="countryCode" type="hidden" id="countryCode"/>
                                    <input name="phoneNumberRegion" type="hidden" id="phoneNumberRegion"/>
                                    <input id="${input.getInputId()}" 
                                        name="${input.getInputId()}"
                                        type="tel" 
                                        pattern="${input.getPattern()!'.*'}"
                                        class="form-control" 
                                        value="" 
                                        placeholder="${input.getPlaceholderTxt() ! 'placeHolderTxt'}"
                                        oninput="checkPhoneNumberValid(this)">
                                <#else>
                                    <input id="${input.getInputId()}" 
                                            name="${input.getInputId()}"
                                            type="${input.getType()!'text'}" 
                                            pattern="${input.getPattern()!'.*'}"
                                            class="form-control" 
                                            value="" 
                                            placeholder="${input.getPlaceholderTxt() ! 'placeHolderTxt'}">
                                </#if>
                                </div>
                            </div>
                    </#if>
                </#list>
            </#if>
            <#import "error_msg.ftl" as e>
            <@e.error_msg />
            <div id="error-div">
                <p id="error-p" class="input-alert-text-error"></p>
            </div>
            <div id="kc-form-buttons" class="form-group">
                <#if formButtons??>
                    <#list formButtons as btn>
                        <button 
                            id="${btn.getBtnId()}" 
                            type="<#if btn.getBtnId()?? && btn.getBtnId() == 'nextBtn'>submit</#if>!'button'" 
                            <#if btn.getBtnId()?? && btn.getBtnId() == 'nextBtn'>
                            disabled="disabled"
                            </#if>
                            class="btn w-100 mb-2"
                            style="
                                <#if btn.getBtnTextClr()??> color:${btn.getBtnTextClr()};</#if>
                                <#if btn.getBtnBgClr()??> background-color:${btn.getBtnBgClr()}; </#if> "
                            >
                            ${(btn.getBtnText()!"btnTxt")?replace('\n','<br/>')?no_esc}
                        </button>
                    </#list>
                </#if>
            </div>
            <#import "login-account-link.ftl" as l>
            <@l.login_account_link />
        </form>
        <script>
            const phoneNumberForm = document.querySelector("#json-register-form");
            phoneNumberForm.addEventListener("submit",function(event){
                removeErrorByElementName();
                if (iti.isValidNumber()) {
                    $("#phone").val(iti.getNumber());
                    $("#countryCode").val("+"+iti.getSelectedCountryData().dialCode)
                    $("#phoneNumberRegion").val(iti.getSelectedCountryData().iso2)
                    $('#nextBtn').addClass('disabled');
                    $('#nextBtn').attr('disabled', 'disabled');
                    $("#json-register-form").submit();
                } else {
                    event.preventDefault();
                    $('#nextBtn').removeClass('disabled');
                    $('#nextBtn').removeAttr('disabled');
                }
            })
            const input = document.querySelector("#phoneNumber");
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
                $("#error-div p").text("${msg('${error}')}");
            </#if>

            const reset = function () {
                removeErrorByElementId("#phoneNumber");
                $("#error-div p").text("");
            };
            input.addEventListener("input",function(event){ 
                if( event.target.value ){
                    reset();
                }
            })
            input.addEventListener('blur', function () {
                reset();
                if (input.value.trim()) {
                    if (iti.isValidNumber()) {
                        $("#error-div p").text("");
                    } else {
                        addErrorByElementName("#phoneNumber");
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
        <style>
            .iti.iti--allow-dropdown {
                display: block;
            }
        </style>
    </#if>
</@layout.registrationLayout>