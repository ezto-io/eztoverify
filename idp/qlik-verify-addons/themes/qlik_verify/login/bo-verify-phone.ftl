<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "head">
        <!-- Phone Number Library Scripts  -->
        <script src="/auth/assets/js/tele-input/tele-input.min.js?version=${serverVersion}" type="text/javascript"></script>
        <script src="/auth/assets/js/tele-input/utils.min.js?version=${serverVersion}" type="text/javascript"></script>
        <!-- Phone Number Library CSS -->
        <link rel="stylesheet" href="/auth/assets/js/tele-input/tele-input.min.css?version=${serverVersion}" type="text/css"/>

        <#if otp_less?? && otp_less?boolean>
            <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
        </#if>
        <style>
            .iti__country-list{
                position: fixed !important;
                max-width: 24rem !important;
            }
       </style>
    </#if>
    <#if section = "form">
        <form id="phone-registration-form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post">
            <div class="row">
                <#--  Template Hidden Fields  -->
                <input name="phoneNumber" type="hidden" id="phone" value="<#if phoneNumber??>${phoneNumber}<#else>''</#if>"/>
                <input name="countryCode" type="hidden" id="countryCode"/>
                <input name="phoneNumberRegion" type="hidden" id="phoneNumberRegion" value="<#if phoneNumberRegion??>${phoneNumberRegion}<#else>''</#if>"/>
                <input name="isPhoneNumberRegistration" value="true" type="hidden">
                <input name="phoneNumberWithCountryCode" type="hidden" id="phoneNumberWithCountryCode" value="<#if phoneNoWithCountryCode??>${phoneNoWithCountryCode}<#else>''</#if>"/>
                <input name="cancel" type="hidden" id="cancel-flow" value="false" />
                <input name="skipped" type="hidden" id="skip-flow" value="false" />
                <#if otp_less?? && otp_less?boolean>
                    <input name="networkType" type="hidden" id="networkType" value=""/>
                    <input name="success" type="hidden" id="success" value="false"/>
                </#if>
                 <#if verification_type?? && verification_type == "missed_call">
                    <input name="callSid" type="hidden" id="callSid" value="${callSid!}"/>
                 </#if>



                <#--  Template Header  -->
                <div class="col-12 d-flex justify-content-between align-items-center mt-2" id="template-header">
                    <#if whatsapp?? && whatsapp=='true'>
                        <h2 class="ciba-template-title mb-0">${msg("bo_whatsapp_ver_title")}</h2>
                    <#else>
                        <#if title??>
                            <h2 class="ciba-template-title mb-0">${msg("${title}")}</h2>
                        <#else>
                            <h2 class="ciba-template-title mb-0">${msg("bo_phone_ver_title")}</h2>
                        </#if>
                    </#if>
                    <#if skippable?? && skippable>
                        <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
                    </#if>
                </div>

                <#if !isPhoneReg??>
                <#--  Template Input Form Fields  -->
                    <div class="col-12 mb-1" id="template-form-fields">
                    <div class="form-group mt-1 mb-0">
                        <label class="ciba-template-input-label my-2">${msg("bo_phone_num_input")}<span class="text-danger"> *</span></label>
                        <input id="phoneNumber" name="phoneNumber" type="tel" class="form-control ciba-template-input-label py-0" maxlength="20" oninput="checkPhoneNumberValid(this)" onpaste="checkPhoneNumberValid(this)" autofocus required />
                    </div>
                </div>
                </#if>

                <#--  Template Error  -->
                <div class="col-12" id="template-error" style="display:none">
                    <div class="d-flex align-items-center">
                        <img src="/auth/assets/img/error-icon.svg?version=${serverVersion}" alt="i" class="my-2" />
                        <div class="mx-2 pb-1">
                            <#import "error_msg.ftl" as e>
                            <@e.error_msg />
                            <div id="error-div">
                                <p id="error-p" class="input-alert-text-error mb-0"></p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <#if !isPhoneReg??>
            <#--  Template Buttons  -->
            <div class="row mt-3">
                <#--  Mobile View  -->
                <div class="col-12 mt-3 display-mobile-only" id="template-button-mobile">
                    <div id="cancelBtn-mobile" onclick="transactionCancel()" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                    <button id="nextBtn-mobile" type="submit" disabled class="w-100 py-3 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
                </div>
                <#--  Web View  -->
                <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                    <#if skippable?? && skippable>
                        <div id="skipBtn-web" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer">${msg("skip")}</div>
                    <#else>
                        <div id="empty-skip-fallback"></div>
                    </#if>
                    <div class="d-flex justify-content-end align-items-center">
                        <div id="cancelBtn" onclick="transactionCancel()" class="w-100 px-3 py-2 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                        <button id="nextBtn" type="submit" disabled class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
                    </div>
                </div>
            </div>
            </#if>

        <#if verification_type?? && verification_type == "missed_call">
          <#if isRetry ?? && !isRetry>
            <div class="d-flex justify-content-center align-items-center">
                <p class="mt-3">${msg("bo_missed_call_ver_desc")}</p>
            </div>
            <div class="d-flex justify-content-center align-items-center mt-3">
                <div class="spinner-border text-primary" role="status">
                   <span class="sr-only">Loading...</span>
                </div>
            </div>
          </#if>
         <#if isRetry ?? && isRetry>
         <input name="retry" type="hidden" id="retry-flow" value="true" />
         <div class="d-flex justify-content-center align-items-center">
              <p class="mt-3">${msg("bo_missed_call_ver_retry_desc")}</p>
            <#--  Template Buttons  -->
            <div class="row mt-3">
                <#--  Mobile View  -->
                <div class="col-12 mt-3 display-mobile-only" id="template-button-mobile">
                    <div id="cancelBtn-mobile" onclick="transactionCancel()" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                    <button id="nextBtn-mobile" type="submit" disabled class="w-100 py-3 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
                </div>
                <#--  Web View  -->
                <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                    <#if skippable?? && skippable>
                        <div id="skipBtn-web" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer">${msg("skip")}</div>
                    <#else>
                        <div id="empty-skip-fallback"></div>
                    </#if>
                    <div class="d-flex justify-content-end align-items-center">
                        <div id="cancelBtn" onclick="transactionCancel()" class="w-100 px-3 py-2 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                        <button id="nextBtn" type="submit" disabled class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
                    </div>
                </div>
            </div>
         </#if>
        </#if>
        </form>

        <script defer>
            var isTransactionFlowSkipped = false;
            $( document ).ready(function() {
                <#if verification_type?? && verification_type == "missed_call" && (!isRetry?? || !isRetry)>
                     $("#isPhoneNumberRegistration").val(false);
                     $('form').submit();
                </#if>
                if("${otp_less!''}" === "true") {
                     getCurrentNetwork()
                        .then(function(result) {
                            $("#success").val(result.success);
                            $("#networkType").val(result.connectivity);
                        });
                }
            });

            function transactionCancel() {
                $("#cancel-flow").val("true");
                $('form').submit();
            }

            function disableSubmitButton() {
                $("#nextBtn-mobile").attr('disabled', 'disabled');
                $("#nextBtn").attr('disabled', 'disabled');
            }

            function transactionSkip() {
                if(!isTransactionFlowSkipped){
                    isTransactionFlowSkipped = true;
                    $("#skip-flow").val("true");
                    $('form').submit();
                }
            }
            
            const phoneNumberForm = document.querySelector("#phone-registration-form")
            phoneNumberForm.addEventListener("submit", function(event){
                removeErrorByElementName();
                if (iti.isValidNumber()) {
                    $("#phone").val(iti.getNumber());
                    $("#countryCode").val("+"+iti.getSelectedCountryData().dialCode)
                    $("#phoneNumberRegion").val(iti.getSelectedCountryData().iso2)
                    $('#nextBtn').addClass('disabled');
                    disableSubmitButton();
                    $("#phone-registration-form").submit();
                } else {
                    event.preventDefault();
                    $('#nextBtn').removeClass('disabled');
                    $('#nextBtn').removeAttr('disabled');
                    $("#nextBtn-mobile").removeAttr('disabled');
                }
                });
            var input = document.querySelector("#phoneNumber");
            var selected_flag= document.querySelector(".iti__selected-flag");
            var iti = window.intlTelInput(input, {
               <#if phoneNumberRegion??>
                    initialCountry: "${phoneNumberRegion}",
                <#elseif defReg??>
                    initialCountry: "${defReg}",
                <#else>
                    initialCountry: "in",
                </#if>
                utilsScript: "${url.resourcesPath}/js/tele-input/utils.js",
                separateDialCode: false
            });
            
            function phoneNumValidCheck(){
                if(!iti.isValidNumber()) {
                    disableSubmitButton();
                    $('#nextBtn').addClass('disabled');
                }else {
                    $('#nextBtn').removeClass('disabled');
                    $('#nextBtn').removeAttr('disabled');
                    $("#nextBtn-mobile").removeAttr('disabled');
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
                    $('#nextBtn').removeClass('disabled');
                    $('#nextBtn').removeAttr('disabled');
                    $("#nextBtn-mobile").removeAttr('disabled');
                }
                else {
                    if($("#phoneNumber").val()) {
                        errorDiv.style.display = 'block';
                        $("#error-div p").text("Phone Number Invalid");
                        document.getElementById('error-p').textContent = "Phone Number Invalid";
                        addErrorByElementName("#phoneNumber");
                        $('#nextBtn').addClass('disabled');
                        disableSubmitButton();
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

            var reset = function () {
                $('#template-error').css('display', 'none');
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
                var empty = false;
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
            setTimeout(() => {
                const targetNode = document.getElementsByClassName('iti__selected-flag');
                const config = { attributes: true, attributeFilter: ['title'] };
                observer.observe(targetNode[0], config);
            }, 1000);

            <#if error?? >
                document.getElementById("error-div").style.display = 'block';
                $("#error-div p").text("${msg('${error}')}");
                $('#template-error').css('display', 'block');
            </#if>
        </script>
        <style>
            .iti.iti--allow-dropdown {
                display: block;
            }
        </style>
    </#if>
</@layout.registrationLayout>
