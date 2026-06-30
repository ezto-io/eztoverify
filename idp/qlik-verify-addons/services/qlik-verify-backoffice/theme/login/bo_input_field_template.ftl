<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "head">
        <script defer src="/auth/assets/js/inputFieldTemplate.min.js?version=${serverVersion}" type="text/javascript"></script>
    </#if>
    <#if section = "form">
        <form id="${title}-form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post">
            <div class="row">
                <#--  Template Hidden Fields  -->
                <input name="cancel" type="hidden" id="cancel-flow" value="false" />
                <#if hiddenFields?? >
                    <#list hiddenFields as field>
                        <input id="${field.fieldName}" name="${field.fieldName}" type="hidden" value="${field.fieldValue}"  />
                    </#list>
                </#if>

                <#--  Template Header  -->
                <div class="col-12 d-flex justify-content-between align-items-center mt-2" id="template-header">
                    <h2 class="ciba-template-title mb-0">${msg(title)}</h2>
                    <#if isHelp?? > 
                    <div class="d-flex justify-content-between align-items-center cursor-pointer" onclick="showHelper()">
                        <img src="/auth/assets/img/bo_template_help.svg?version=${serverVersion}" id="bo-template-help" alt="?" width="100%" />
                        <p class="ciba-template-help-text px-1 my-0">${msg("help")}</p>
                    </div>
                    </#if>
                </div>

                <#--  Template Input Form Fields  -->
                <div class="col-12 mb-1" id="template-form-fields">
                    <#if fields?? >
                        <#list fields as field>
                            <#if field.fieldType == 'password' >
                                <div class="form-group mt-1 mb-0 --password" id="show_hide_password">
                                    <label class="ciba-template-input-label my-2">${msg(field.fieldName)}<#if field.isRequired?? && field.isRequired == true ><span class="text-danger"> *</span></#if></label>
                                    <div class="input-group"  >
                                    <input id="${field.fieldName}" name="${field.fieldName}" type="${field.fieldType}" class="form-control ciba-template-input-label py-0" placeholder="${field.placeholder}" maxlength="${field.maxLength}" oninput="checkValidation('${field.fieldName?js_string}')" onpaste="checkValidation('${field.fieldName?js_string}')" autofocus />
                                        <div class="input-group-prepend hide_show" onclick="togglePasswordView()" >
                                            <span class="input-group-text tio hidden_outlined"></span>
                                        </div>
                                    </div>
                                </div>                            
                            <#else>
                                <div class="form-group mt-1 mb-0">
                                    <label class="ciba-template-input-label my-2">${msg(field.fieldName)}<#if field.isRequired?? && field.isRequired == true ><span class="text-danger"> *</span></#if></label>
                                    <input id="${field.fieldName}" name="${field.fieldName}" type="${field.fieldType}" class="form-control ciba-template-input-label py-0" placeholder="${field.placeholder}" maxlength="${field.maxLength}" oninput="checkValidation('${field.fieldName?js_string}')" onpaste="checkValidation('${field.fieldName?js_string}')" autofocus />
                                </div>
                            </#if>

                        </#list>
                    </#if>
                </div>

                <#--  Template Error  -->
                <div class="col-12 display-init-none" id="template-error">
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
                        <h2 class="ciba-template-title mb-0">${msg(title)}</h2>
                        <h6 class="ciba-template-subtitle text-dark my-2">${msg("bo_input_field_helper_text")}</h6>
                        <div class="text-center">
                            <img src="/auth/assets/img/${title}.svg?version=${serverVersion}" id="${title}-helper-image" class="rounded my-3 mw-100" alt="${title}-helper-image" />
                        </div>
                    </div>
                    <div class="w-100 py-3 mt-3 ciba-template-submitbtn text-center" style="margin-bottom: 5rem !important" onclick="showHelper()">${msg("bo_template_back")}</div>
                </div>
            </div>
            
            <div class="row mt-3">
                <#--  Template Buttons  -->
                <div class="col-12 d-flex mt-3" id="template-button">
                    <div id="cancelBtn" onclick="transactionCancel()" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                    <button id="nextBtn" type="submit" disabled class="w-100 py-3 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
                </div>
            </div>
        </form>
        <script>
            setFields([
                <#list fields as item>
                    { 
                        "fieldName": "${item.fieldName}", 
                        "fieldType": "${item.fieldType}", 
                        "maxLength": "${item.maxLength}", 
                        "placeholder": "${item.placeholder}", 
                        "isRequired": ${item.isRequired?string}, 
                        "isError": ${item.isError?string}, 
                        "errorMsg": "${item.errorMsg}", 
                        "validation": {
                            <#if item.validation.fieldLength?? >
                            "fieldLength": "${item.validation.fieldLength}", 
                            </#if>
                            <#if item.validation.regex?? >
                            "regex": "${item.validation.regex}",
                            </#if>
                            <#if item.validation.characterType?? >
                            "characterType": "${item.validation.characterType}",
                            </#if>
                            <#if item.validation.characterCase?? >
                            "characterCase": "${item.validation.characterCase}",
                            </#if>
                        }
                    }<#if item_has_next>,</#if>
                </#list>
            ], 
            <#if phoneNumberRegion??>
                "${phoneNumberRegion}"
            <#elseif defReg??>
                "${defReg}"
            <#else>
                "in"
            </#if>,
                "/auth/assets",
            <#if title??>
                "${title}"
            </#if>
            )
            
            <#if error??>
                $('#template-error').css('display', 'block');
            </#if>
        </script>
    </#if>
</@layout.registrationLayout>