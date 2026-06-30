<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="aadhaar-pan-link-check-form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post">
            <div class="row">
                <#--  Template Hidden Fields  -->
                <input name="pan_aadhaar_link_status" type="hidden" value="true"/>
                <input name="cancel" type="hidden" id="cancel-flow" value="false" />
                <input name="skipped" type="hidden" id="skip-flow" value="false" />


                <#--  Template Header  -->
                <div class="col-12 d-flex justify-content-between align-items-center mt-2" id="template-header">
                    <h2 class="ciba-template-title mb-0">${msg("pan-aadhaar-link-status")}</h2>
                    <#if skippable?? && skippable>
                        <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
                    </#if>
                </div>

                <#--  Template Input Form Fields  -->
                 <div class="col-12 mb-1" id="template-form-fields">
                    <div class="form-group mt-1 mb-0">
                        <label class="ciba-template-input-label my-2">${msg("pan-number")}<span class="text-danger"> *</span></label>
                        <div class="d-flex position-relative">
                        <#if pan_number??>
                           <input id="panNo" name="pan_number" value="${pan_number}" type="text" class="form-control ciba-template-input-label py-0 pr-4-2rem" placeholder="${msg('pan-number-hlp-txt')}" maxlength="10" oninput="this.value = this.value.toUpperCase(); disableSubmit();" onpaste="disableSubmit()"  autofocus required />
                        <#else>
                            <input id="panNo" name="pan_number" type="text" class="form-control ciba-template-input-label py-0 pr-4-2rem" placeholder="${msg('pan-number-hlp-txt')}" maxlength="10" oninput="this.value = this.value.toUpperCase(); disableSubmit();" onpaste="disableSubmit()"  autofocus required />
                        </#if>
                            <div class="d-flex justify-content-between align-items-center cursor-pointer template-help-section" onclick="showHelper('pan')">
                                <img src="/auth/assets/img/bo_template_help.svg?version=${serverVersion}" id="bo-template-help" alt="?" width="100%" />
                                <p class="ciba-template-help-text px-1 my-0">${msg("help")}</p>
                            </div>
                        </div>

                        <label class="ciba-template-input-label my-2">${msg("aadhaar-number")}<span class="text-danger"> *</span></label>
                        <div class="d-flex position-relative">
                            <#if aadhaar_number??>
                                <input id="aadhaarNo" name="aadhaar_number" value="${aadhaar_number}" type="text" class="form-control ciba-template-input-label py-0 pr-4-2rem" placeholder="${msg('aadhaar-number-hlp-txt')}" maxlength="12" oninput="disableSubmit()" onpaste="disableSubmit()"  autofocus required />
                            <#else>
                                <input id="aadhaarNo" name="aadhaar_number" type="text" class="form-control ciba-template-input-label py-0 pr-4-2rem" placeholder="${msg('aadhaar-number-hlp-txt')}" maxlength="12" oninput="disableSubmit()" onpaste="disableSubmit()"  autofocus required />
                            </#if>
                            <div class="d-flex justify-content-between align-items-center cursor-pointer template-help-section" onclick="showHelper('aadhaar')">
                                <img src="/auth/assets/img/bo_template_help.svg?version=${serverVersion}" id="bo-template-help" alt="?" width="100%" />
                                <p class="ciba-template-help-text px-1 my-0">${msg("help")}</p>
                            </div>
                        </div>
                    </div>
                </div>

                    <div class="col-12 d-none justify-content-center flex-column align-items-center submit-form-loader-height py-5" id="spinner-submit">
                        <#if isMobile?? && !isMobile>
                            <div class="d-flex justify-content-center align-items-center py-5">
                                <div class="spinner-border branding-primary-color" role="status">
                                    <span class="sr-only">Loading...</span>
                                </div>
                            </div>
                        </#if>
                        <p class="ciba-template-subtitle text-dark text-break mt-3 text-center">${msg("pan-aadhaar-link-status-loading")}</p>
                    </div>

                <#--  Template Error  -->
                <div class="col-12" id="template-error" style="display:none">
                    <div class="d-flex align-items-center">
                        <img src="/auth/assets/img/error-icon.svg?version=${serverVersion}" alt="i" class="my-2" />
                        <div class="mx-2 pb-1">
                            <#import "error_msg.ftl" as e>
                            <@e.error_msg />
                        </div>
                    </div>
                </div>


                <#--  Template Help -->
                <div class="col-12" id="template-helper">
                    <div>
                        <div class="my-4 " id="tenant-logo">
                            <#if tenantLogo?contains("http")>
                                <div class="d-flex ${logoPosition}"> <img alt="logo" class="form-logo form-logo-parent"  src=" ${tenantLogo} " /> </div>
                            <#elseif tenantLogo == "default-verify-logo">
                                <div class="d-flex ${logoPosition}"> <img alt="logo" class="form-logo form-logo-parent" src="${url.resourcesPath}/img/eztovrfy-logo.svg?version=${serverVersion}"" /> </div>
                            <#else>
                                <div class="d-flex ${logoPosition}"> <img alt="logo" class="form-logo form-logo-parent" src="${url.resourcesPath}/img/logo.svg?version=${serverVersion}"" /> </div>
                            </#if>
                        </div>
                        <h2 class="ciba-template-title mb-0">${msg("pan-aadhaar-link-status")}</h2>
                        <h6 class="ciba-template-subtitle text-dark my-2">${msg("bo_input_field_helper_text")}</h6>
                        <div class="text-center">
                            <img src="/auth/assets/img/aadhar_Front.png?version=${serverVersion}" id="pan-aadhaar-link-status-helper-image" class="rounded my-3 mw-100" alt="business-verification-helper-image" />
                        </div>
                    </div>
                    <div class="w-100 py-3 mt-3 ciba-template-submitbtn text-center workflow-template-helper-back-btn" onclick="showHelper()">${msg("bo_template_back")}</div>
                </div>
            </div>

            <#--  Template Buttons  -->
            <div class="row mt-3" id="template-buttons-wrapper">
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
        </form>

        <script>
            var isShowingHelp = false;
            var isTransactionFlowSkipped = false;
            var panValue;
            var aadhaarValue;
            var spinner;
            var form;
            var templateFields;

            function showHelper(type) {

                let helpText = "";
                let helpImage = "";
                if(type === 'pan') {
                    helpText = "${msg('pan-help-text')}";
                    helpImage = "/auth/assets/img/pan_verification.svg";
                } else if (type === "aadhaar") {
                    helpText = "${msg('aadhaar-help-text')}";
                    helpImage = "/auth/assets/img/aadhar_verification.svg";
                }

                $('#helper-text').text(helpText);
                $('#pan-aadhaar-link-status-helper-image').attr('src', helpImage);

                if (window.innerWidth <= 768 && window.innerHeight <= 1024) {
                    if (!isShowingHelp) {
                        showHelperToView()
                        isShowingHelp = true;
                    } else {
                        hideHelperToBottom()
                        isShowingHelp = false;
                    }
                }else{
                    if (!isShowingHelp) {
                        $('#template-error').css('display', 'none');
                        $('#template-helper').css('display', 'block');
                        $('#template-header').css('display', 'none').removeClass('d-flex');
                        $('#template-form-fields').css('display', 'none');
                        $('#template-button-mobile').css('display', 'none');
                        $('#template-button-web').css('display', 'none');
                        isShowingHelp = true;
                    } else {
                        $('#template-helper').css('display', 'none');
                        $('#template-header').css('display', 'flex').addClass('d-flex');
                        $('#template-form-fields').css('display', 'block');
                        if (window.innerWidth <= 576) {
                            $('#template-button-mobile').css('display', 'flex');
                            $('#template-button-web').css('display', 'none');
                        } else {
                            $('#template-button-mobile').css('display', 'none');
                            $('#template-button-web').css('display', 'flex');
                        }
                        isShowingHelp = false;
                    }
                }
            }

            function hideHelperToBottom() {
                $('#tenant-logo').css('display', 'none');
                $('#template-helper').css('top', '+150%');
            }

            function showHelperToView() {
                $('#tenant-logo').css('display', 'block');
                $('#template-helper').css('top', '0');
            }

            function validateAadhaar(aadhaar) {
                    return /^\d{12}$/.test(aadhaar); // Exactly 12 digits
            }

            function validatePAN(pan) {
                return /^[A-Z]{5}\d{4}[A-Z]$/.test(pan); // 5 letters, 4 digits, 1 letter
            }

            function disableSubmit(){
                 $('#template-error').css('display', 'none');

                let panInput = document.getElementById("panNo");
                let aadhaarInput = document.getElementById("aadhaarNo");

                let isAadhaarValid = validateAadhaar(aadhaarInput.value);
                let isPanValid = validatePAN(panInput.value);

                if( isAadhaarValid && isPanValid){
                    $("#nextBtn-mobile").removeAttr('disabled');
                    $("#nextBtn").removeAttr('disabled');
                }else{
                    disableSubmitButton();
                }
            }

            $('#aadhaar-pan-link-check-form').submit(function(e){
                disableSubmitButton();
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

            document.addEventListener("DOMContentLoaded", function() {
                panValue = document.getElementById("panNo")?.value;
                aadhaarValue = document.getElementById("aadhaarNo")?.value;
                spinner = document.getElementById("spinner-submit");
                form = document.getElementById("aadhaar-pan-link-check-form");
                templateFields = document.getElementById("template-form-fields");

                if (validateAadhaar(aadhaarValue) && validatePAN(panValue)) {
                    templateFields.classList.add("d-none");
                    $('#template-buttons-wrapper').addClass('d-none');
                    spinner.classList.remove("d-none");
                    spinner.classList.add("d-flex");
                    document.getElementById("aadhaar-pan-link-check-form").submit();
                }
            });

            <#if error??  >
                $('#template-error').css('display', 'block');
            </#if>
        </script>

    </#if>
</@layout.registrationLayout>