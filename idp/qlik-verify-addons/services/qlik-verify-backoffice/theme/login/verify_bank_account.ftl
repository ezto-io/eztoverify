<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="otp-verification-form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post">
            <div class="row">
                <#--  Template Hidden Fields  -->
                <input name="bank_account_verification" type="hidden" value="true"/>
                <input name="cancel" type="hidden" id="cancel-flow" value="false" />
                <input name="skipped" type="hidden" id="skip-flow" value="false" />

                <#--  Template Header  -->
                <div class="col-12 d-flex justify-content-between align-items-center mt-2" id="template-header">
                    <h2 class="ciba-template-title mb-0">${msg("bank-account-verification")}</h2>
                    <#if skippable?? && skippable>
                        <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
                    </#if>
                </div>

                <#--  Template Input Form Fields  -->
                    <div class="col-12 mb-1" id="template-form-fields">
                    <div class="form-group mt-1 mb-0">
                        <label class="ciba-template-input-label my-2">${msg("bank-account-number")}<span class="text-danger"> *</span></label>
                        <input id="accountNo" name="bank_account_no" type="text" class="form-control ciba-template-input-label py-0" placeholder="${msg('bank-account-number-hlp-txt')}" oninput="disableSubmit()" onpaste="disableSubmit()" autofocus required />
                    </div>
                    <div class="form-group mt-1 mb-0">
                        <label class="ciba-template-input-label my-2">${msg("ifsc-code")}<span class="text-danger"> *</span></label>
                        <input id="ifscCode" name="ifsc_code" type="text" class="form-control ciba-template-input-label py-0" placeholder="${msg('ifsc-code-hlp-txt')}" oninput="disableSubmit()" onpaste="disableSubmit()" autofocus required />
                    </div>
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
            </div>
            
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
        </form>
        <script>
            var isTransactionFlowSkipped = false;
            function validateBankAccountNumberLength(){
                bankAccountNumberFieldValue = document.getElementById("accountNo").value.replace(/[^0-9]/g, '').replace(/(\..*)\./g, '$1');
                const bankAccountNumberFieldLength = bankAccountNumberFieldValue.length;
                document.getElementById("accountNo").value = bankAccountNumberFieldValue;
                return Boolean(bankAccountNumberFieldLength >= 6);
            }

            function validateIFSCCode(){
                ifscCodeValue = document.getElementById("ifscCode").value.replace(/[^0-9a-zA-Z]/g, '').replace(/(\..*)\./g, '$1').toUpperCase();
                const ifscCodeLength = ifscCodeValue.length;
                document.getElementById("ifscCode").value = ifscCodeValue;
                return Boolean(ifscCodeLength >= 6);
            }

            function disableSubmit(){
                $('#template-error').css('display', 'none');
                let accountNo = $("#accountNo").val();
                let accountNoValid = validateBankAccountNumberLength();
                let ifscCode = $("#ifscCode").val();
                let ifscCodeValid = validateIFSCCode();
                if( accountNo && ifscCode && accountNoValid && ifscCodeValid){
                    $("#nextBtn-mobile").removeAttr('disabled');
                    $("#nextBtn").removeAttr('disabled');
                }else{
                    disableSubmitButton();
                }
            }
                
            $('#otp-verification-form').submit(function(e){
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

            <#if error??  >
                $('#template-error').css('display', 'block');
            </#if>
        </script>
    </#if>
</@layout.registrationLayout>