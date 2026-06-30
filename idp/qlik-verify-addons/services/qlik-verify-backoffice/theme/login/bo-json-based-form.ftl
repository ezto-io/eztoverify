<!--JSON Based Form Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "head">
        <!-- FlatPickr Library -->
        <link rel="preload stylesheet" as="style" href="/auth/assets/css/flatpickr.min.css?version=${serverVersion}"/>
        <!-- Select2 CSS -->
        <link rel="preload stylesheet" as="style" href="/auth/assets/css/select2.min.css?version=${serverVersion}"/>
        <link rel="preload stylesheet" as="style" href="/auth/assets/css/dynamic-form.min.css?version=${serverVersion}"/>
        <!-- Select2 JS -->
        <script defer src="/auth/assets/js/select2.min.js?version=${serverVersion}"></script>
        <script defer src="/auth/assets/js/flatpickr.min.js?version=${serverVersion}" type="text/javascript"></script>
        <!-- Dynamic Form -->
        <script defer  src="/auth/assets/js/dynamicForm.min.js?version=${serverVersion}" type="text/javascript"></script>
        <!-- Phone Number Library Scripts  -->
        <script src="/auth/assets/js/tele-input/tele-input.min.js?version=${serverVersion}" type="text/javascript"></script>
        <script src="/auth/assets/js/tele-input/utils.min.js?version=${serverVersion}" type="text/javascript"></script>
        <!-- Phone Number Library CSS -->
        <link rel="stylesheet" href="/auth/assets/js/tele-input/tele-input.min.css?version=${serverVersion}" type="text/css"/>
        <style>
            .iti__country-list{
                position: fixed !important;
                max-width: 24rem !important;
            }
       </style>
    </#if>
    
    <#if section = "form">
        <form id="json-based-form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post" enctype="multipart/form-data">

            <input type="hidden" name="cancel" id="cancel-input" value="false" />
            <input name="skipped" type="hidden" id="skip-flow" value="false" />

            <div>
                <#--  Template Title  -->
                <div class="row" id="template-header-wrapper">
                    <div class="col-12 d-flex justify-content-between align-items-center mt-3 mb-4">
                        <h2 class="ciba-template-title json-form-label">${formTitle!default("")}</h2>
                        <#if skippable?? && skippable>
                            <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
                        </#if>
                    </div>
                </div>
                <#--  Template Body  -->
                <div class="scrollable-element-body d-flex align-items-center flex-column">
                <#list inputJson as field>

                    <#--  Common Form Values  -->
                    <#assign fieldLabel = field.label!" " />
                    <#assign fieldName = field.name?replace(" ", "-") />
                    <#assign fieldId = fieldName />
                    <#assign fieldType = field.type?default("text") />
                    <#assign fieldRequired = field.isRequired?default(false) />
                    <#assign fieldHelperText = field.helperText?default(' ') />
                    <#assign labelClass = "json-form-label" />
                    <#assign placeHolder = field.placeHolder?default("") />
                    <#assign defaultValue = field.defaultValue?default("") />
                    <#if field.editable?? && !field.editable>
                        <#assign readonly = true />
                    <#else>
                        <#assign readonly = false />
                    </#if>
                    <#assign includeMetadata = field.includeMetadata?default(false)/>
                    <#if field.dependsOn??>
                        <#assign jsonEntries = [] />
                        <#list field.dependsOn as dependOn>
                            <#assign key = dependOn.field?trim?split(" ")?filter(word -> word?has_content)?join("_")?lower_case>
                            <#assign entry = "\"${key}\": \"${dependOn.value?default('')}\"" />
                            <#assign jsonEntries += [entry] />
                        </#list>
                        <#assign dependsOnJsonString = "{" + jsonEntries?join(", ") + "}" />
                    <#else>
                        <#assign dependsOnJsonString = "" />
                    </#if>
                    <#assign validationTypeForDependsOn = field.validationTypeForDependsOn!"all_true" />

                    <#-- Form Value For File extension and onlyAccept for file type -->
                    <#assign acceptableFileExtensions = "jpeg, jpg, gif, svg, png, pdf, doc, docx, xls, xlsx, txt, mp4, mov, ppt, pptx, mp3" />
                    <#if field.onlyAccept?? && field.onlyAccept?has_content>
                        <#assign onlyAccept = field.onlyAccept?split(',')?map(ext -> '.' + ext)?join(',') />
                    </#if>
                    <#if fieldType == "checkbox">
                        <#assign labelClass = labelClass + " form-check pl-0 d-flex align-items-center" />
                        <#if fieldRequired>
                            <#assign labelClass = labelClass + " required-checkbox" />
                        </#if>
                    </#if>

                    <#-- Form Value For Button Color -->
                    <#assign primaryColor = field.primaryColor?default("") />
                    <#assign secondaryColor = field.secondaryColor?default("") />

                    <#if defaultValue != "null">
                    <script>
                        $(document).ready(function() {
                            page = Number("${field.page!''}")
                            if ( page > 0) {
                                totalPage = totalPage < page ? page : totalPage;
                                isPaginationPresent = true;
                            }
                            initFields({type:"${fieldType}", name:"${fieldName}", "label":"${fieldLabel}", isRequired:${fieldRequired?string('true', 'false')}, page: ${field.page!'0'}});
                        })
                    </script>

                    <div data-name=${fieldName}  class="form-group d-none <#if field.type == 'capture'>camera-form-group</#if>" data-page="${field.page!'0'}" <#if dependsOnJsonString?? && dependsOnJsonString != ""> dependOn="${dependsOnJsonString}" validationTypeForDependsOn="${validationTypeForDependsOn}" hidden </#if>>

                        <#-- Common label for all types except type button and capture -->
                        <#if fieldType != "button">
                            <label for="${fieldId}" <#if fieldType == "checkbox">data-fieldId="${fieldId}"</#if> class="${labelClass}">
                                ${fieldLabel}<#if fieldLabel!=" " && fieldRequired> <span class="text-danger"> *</span> </#if>
                            </label>
                        </#if>

                        <#if fieldType == "option" && field.options?has_content>
                            <select id="${fieldId}" class="form-control " name="${fieldName}" <#if readonly>disabled="${readonly?c}"</#if> <#if field.multiple?? && field.multiple>multiple="${field.multiple?c}"</#if>  <#if fieldRequired>required</#if>>
                                <#if !field.multiple?? || !field.multiple>
                                    <option value="" selected disabled>${msg("select-option")}</option>
                                </#if>
                                <#list field.options?split(",") as option>
                                    <option class="json-form-label" value="${option}" <#if defaultValue != "" && option == defaultValue> selected </#if>>${option}</option>
                                </#list>
                            </select>
                            <#if field.multiple?? && field.multiple>
                                <script defer type="text/javascript">
                                    $(document).ready(function() {
                                        $("#${fieldId}").select2({
                                            placeholder: "${msg('select-option')}",
                                            allowClear: true,
                                            minimumResultsForSearch: Infinity
                                        });
                                        <#if fieldRequired>
                                            $("#${fieldId}").on('change', function () {
                                                if($(this).val().length == 0){
                                                    <#list inputJson as field>
                                                        <#if field.type == "button">
                                                             $('#'+'${field.label}'+'-btn').attr('disabled', true);
                                                        </#if>
                                                    </#list>    
                                                    $('#submitBtn').attr('disabled', true);
                                                }else{
                                                    form.dispatchEvent(new Event('input'));
                                                }
                                            });
                                        </#if>
                                    });
                                </script>
                            </#if>

                        <#elseif (fieldType == "checkbox" || fieldType = "radio") && field.options?has_content>
                            <#if field.options?has_content>
                                <div class="d-flex w-100 justify-content-between flex-wrap">
                                    <#list field.options?split(",") as option>
                                        <label class="form-checkbox d-flex align-items-center" for="${fieldId}_${option}">
                                            <input class="height-12px line-height-1 ${fieldId}_options" type="${fieldType}" id="${fieldId}_${option}" name="${fieldId}" value="${option}" <#if defaultValue != "" && option == defaultValue> checked </#if> <#if readonly>onclick="preventChange(event)"</#if> />
                                            <span>${option}</span>
                                        </label>
                                    </#list>
                                </div>
                            </#if>

                        <#elseif fieldType == "file">
                            <ul id="${fieldId}-selected-files" class="px-2 file-name-text"></ul>
                            <div class="file-upload-wrapper">
                                <div class="file-upload-button">
                                    <img class="mr-2" width="30" height="30" alt="mail" src="/auth/assets/img/upload.svg?version=${serverVersion}" alt="upload-icon">
                                    <#if field.multiple?? && field.multiple>
                                        <span class="text-nowrap">${msg('upload-files')}</span>
                                    <#else>
                                        <span class="text-nowrap">${msg('upload-file')}</span>
                                    </#if>
                                </div>
                                <input type="file" class="file-upload-input" id="${fieldId}" name="${fieldName}" <#if onlyAccept??>accept="${onlyAccept}"</#if> <#if field.multiple?? && field.multiple>multiple="${field.multiple?c}"</#if> <#if fieldRequired>required</#if>>
                                <input type="hidden" name="${fieldName}_accept_types" id="${fieldId}_accept_types" value=<#if onlyAccept??>"${onlyAccept}"<#else>"${acceptableFileExtensions}"</#if>>
                                <p class="text-danger font-size-14px pt-2 d-none" id="${fieldId}-error-txt" >${msg('file-size-greater-than-20mb-error')}</p>
                            </div>

                            <script>
                                $(document).ready(function() {
                                    <#if onlyAccept?? && onlyAccept?has_content>
                                        var acceptableFileType = '${onlyAccept}';
                                    <#else>
                                        var acceptableFileType = '${acceptableFileExtensions}';
                                    </#if>
                                    listenFileInput('${fieldId}', acceptableFileType, '${msg('invalid-file-format')}');
                                });
                            </script>
                        <#elseif fieldType == "phonenumber">
                            <input id="${fieldId}_dialcode" name="${fieldName}_dialcode" type="hidden" value="+91" />
                            <input data-page="${field.page!'0'}" id="${fieldId}" name="${fieldName}" value="${defaultValue}" data-value="${defaultValue}" type="tel" class="form-control" <#if readonly>readonly="${readonly?string('readonly', '')}"</#if> <#if fieldRequired>required</#if> oninput="allowOnlyNumbers('${fieldId}')" onpaste="allowOnlyNumbers('${fieldId}')" />
                            <div class="align-items-center" id="${fieldId}-error-txt" style="display:none;">
                                <img src="${url.resourcesPath}/img/error-icon.svg" alt="i" class="my-2" />
                                <p class="text-danger m-2 font-size-14px">${msg('invalid-phone-number')}</p>
                            </div>
                            <script>
                                $(document).ready(function() {
                                    initTelInput("${fieldId}", "${url.resourcesPath}");
                                })
                            </script>
                        <#elseif fieldType == "capture">
                            <input type="text" class="d-none" id="${fieldName}" name="${fieldName}" <#if fieldRequired>required</#if>>
                            <#if field.placeHolder??>
                                <img id="${fieldName}-placeholder-image" width="100%" height="100%" class="rounded" src="${field.placeHolder}" alt="placholder-image"/>
                            <#else>
                                <div id="${fieldName}-placeholder-image" class="rounded justify-content-center align-items-center w-100 dynamic-form-capture-placholder">
                                    <img width="50" height="50" src="/auth/assets/img/CaptureCamera.svg?version=${serverVersion}" alt="camera-icon"/>
                                </div>
                            </#if>
                            <#if isMobile?? && isMobile>
                                <div class="position-relative" style="display:none;" id="${fieldName}-switch-camera-wrapper">
                                    <button type="button" class="ciba-template-retakeIcon p-1" style="width: inherit;" id="${fieldName}-switch-camera-btn">
                                        <img src="/auth/assets/img/switch-camera.svg?version=${serverVersion}" width="38px" id="switch" alt="switch camera btn" alt="switch-camera-icon"/>
                                    </button>
                                </div>
                            </#if>
                            <video id="${fieldName}-video" width="100%" height="100%" class="rounded" autoplay playsinline muted style="display:none; transform: scaleX(-1);"></video>
                            <div class="position-relative" style="display:none;" id="${fieldName}-retake-selfie-wrapper">
                                <button type="button" class="ciba-template-retakeIcon p-2" id="${fieldName}-retake-selfie-btn" onclick="captureImage('${fieldName}', ${includeMetadata?c})">
                                    <img src="/auth/assets/img/bo_retry.svg?version=${serverVersion}" id="retake" alt="retake picture btn" alt="retry-icon"/>
                                    <span>${msg("bo_retake")}<span>
                                </button>
                            </div>
                            <img id="${fieldName}-imagePreview" width="100%" height="100%" class="rounded" style="display:none; transform: scaleX(-1);" alt="Captured Image Preview">
                            <canvas id="${fieldName}-canvas" width="100%" height="100%" style="display:none;"></canvas>
                            <div id="${fieldName}-camera-error" class="px-2 py-5 text-center" style="display:none;">
                                <p class="text-center liveness-instruction-text">${msg("enable_camera_desc")} ${msg("enable_camera_app_browser")}</p>
                                <button type="button" class="ciba-template-submitbtn mt-3 px-3 py-2" onclick="captureImage('${fieldName}', ${includeMetadata?c})">${msg("doTryAgain")}</button>
                            </div>
                            <div class="d-flex justify-content-center">
                                <button type="button" class="btn position-relative px-3 py-2 rounded bg-white cursor-pointer" id="${fieldName}-capture-text" style="bottom: 3.5rem; font-size: 14px; font-weight: 500" onclick="captureImage('${fieldName}', ${includeMetadata?c})">
                                    <span>${msg("tap-to-capture-image")}</span>
                                </button>
                                <button type="button" class="btn position-relative p-2 rounded bg-white cursor-pointer" id="${fieldName}-capture-image" style="bottom: 3.5rem; display: none" onclick="captureImage('${fieldName}', ${includeMetadata?c})"><img src="/auth/assets/img/camera.svg?version=${serverVersion}" width="28px" id="capture-image-icon" alt="capture image btn" /></button>
                            </div>
                            <script>
                                $(document).ready(function() {
                                    loadCaptureElements('${fieldName}', ${isMobile?c}, ${includeMetadata?c}, <#if field.facingMode??>'${field.facingMode}'<#else>'back'</#if>);
                                });
                            </script>
                        <#elseif fieldType == "textarea">
                            <textarea data-page="${field.page!'0'}" class="form-control" id="${fieldId}" name="${fieldName}" placeholder="${placeHolder}" <#if readonly>readonly="${readonly?string('readonly', '')}"</#if> <#if fieldRequired>required</#if> >${defaultValue}</textarea>
                        <#elseif fieldType == "date" || fieldType == "text">
                            <input data-page="${field.page!'0'}" type="${fieldType}" class="form-control" id="${fieldId}" name="${fieldName}" value="${defaultValue}" data-value="${defaultValue}"  <#if readonly>readonly="${readonly?string('readonly', '')}"</#if>
                            <#if fieldType == 'date'>placeholder="dd/mm/yyyy"</#if>
                                placeholder="${placeHolder}"
                                <#if fieldRequired>required</#if>
                                <#if field.regex?? && field.regex?has_content>pattern="${field.regex}"</#if>
                                <#if field.regexError?? && field.regexError?has_content>regexerror="${field.regexError}"</#if> />
                        </#if>
                        <#if fieldHelperText??>
                            <p class="helper-text ng-binding" style="font-size: 13px">${fieldHelperText}</p>
                        </#if>

                    </div>
                    </#if>
                </#list>
                </div>

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

            <#assign buttonPresent = false>

            <#--  Custom Template Buttons  -->
            <#--  Mobile  -->
            <div class="mt-3 flex-wrap display-mobile-only" style="gap:0.5rem" id="template-button">
                <#list inputJson as field>
                    <#if field.type == "button">
                        <#assign buttonPresent = true>
                        <input type="hidden" value="false" id="${field.name}" name="${field.name}"/>
                        <button <#if dependsOnJsonString?? && dependsOnJsonString != ""> dependOn="${dependsOnJsonString}" validationTypeForDependsOn="${validationTypeForDependsOn}" customButton </#if> id="${field.name}-btn-mobile" class="py-3 my-2 ciba-template-submitbtn <#if field.borderColor??>ciba-template-cancelbtn</#if>" type="button" onclick="buttonAction('${field.name}')" style=" background-color:${field.buttonColor} !important;color:${field.textColor} !important;  flex-grow: 1; min-width: 45%<#if field.borderColor??>border-color:${field.borderColor} !important;</#if>">${field.label}</button>
                    </#if>
                </#list>
            </div>
            <#--  Web  -->
            <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                <#if skippable?? && skippable && buttonPresent>
                    <div id="skipBtn-web" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer mr-5">${msg("skip")}</div>
                </#if>
                <div class="d-flex mt-3 flex-wrap" style="gap:0.5rem" id="template-button">
                    <#list inputJson as field>
                        <#if field.type == "button">
                            <#assign buttonPresent = true>
                            <button <#if dependsOnJsonString?? && dependsOnJsonString != ""> dependOn="${dependsOnJsonString}" validationTypeForDependsOn="${validationTypeForDependsOn}" customButton </#if> id="${field.name}-btn" class="py-2 px-3 my-2 ciba-template-submitbtn <#if field.borderColor??>ciba-template-cancelbtn</#if>" type="button" onclick="buttonAction('${field.name}')" style=" background-color:${field.buttonColor} !important;color:${field.textColor} !important;  flex-grow: 1; min-width: 45%<#if field.borderColor??>border-color:${field.borderColor} !important;</#if>">${field.label}</button>
                        </#if>
                    </#list>
                </div>
            </div>

            <#--  Default Template Buttons  -->
            <#if !buttonPresent>
                <div class="row mt-3">
                    <#--  Mobile View  -->
                    <div class="col-12 mt-3 display-mobile-only" id="template-button-mobile">
                        <button onclick="cancelForm()" id="cancelBtn-mobile" type="button" value="cancel" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</button>
                        <button id="submitBtn-mobile" type="button" onclick="nextPage()" class="w-100 py-3 ml-2 ciba-template-submitbtn" disabled="disabled">${msg("doSubmit")}</button>
                    </div>
                    <#--  Web View  -->
                    <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                        <#if skippable?? && skippable>
                            <div id="skipBtn-web" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer">${msg("skip")}</div>
                        <#else>
                            <div id="empty-skip-fallback"></div>
                        </#if>
                        <div class="d-flex justify-content-end align-items-center">
                            <div onclick="cancelForm()" id="cancelBtn" type="button" value="cancel" class="w-100 px-3 py-2 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
                            <button id="submitBtn" type="button" onclick="nextPage()" class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn" disabled="disabled">${msg("doSubmit")}</button>
                        </div>
                    </div>
                </div>
            </#if>

        </form>
    </#if>

</@layout.registrationLayout>