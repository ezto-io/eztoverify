<#import "template.ftl" as layout>
    <@layout.registrationLayout; section>
        <#if section = "head">
            <script defer src="/auth/assets/js/socket.min.js?version=${serverVersion}" type="text/javascript"></script>
            <script defer src="/auth/assets/js/ocrTemplate.min.js?version=${serverVersion}" type="text/javascript"></script>
            <script defer src="/auth/assets/js/common.min.js?version=${serverVersion}" type="text/javascript"></script>
        </#if>
        <#include "bo_card_face.ftl" />
        <#assign title = 'bo_pan_image_upload' >
        <#assign isBothSide = false >
        <#if isBothSide?? && isBothSide == true >
            <#assign totalSteps = 2 >
        <#else>
            <#assign totalSteps = 1 >
        </#if>
        <#if section="form">
            <form id="bo_image_verification_form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post" >
                <#--  Template Hidden Fields  -->
                <div class="row mb-3">
                    <input name="pan_image" type="hidden" id="pan_image" value="true">
                    <input name="image_details" type="hidden" id="image_details">
                    <input name="cancel" type="hidden" id="cancel-flow" value="false" />
                    <input name="skipped" type="hidden" id="skip-flow" value="false" />

                    <#--  Template Header  -->
                    <div class="col-12 d-flex justify-content-between align-items-center mt-2" id="template-header">
                        <h2 class="ciba-template-title mb-0">${msg(title)}</h2>
                        <#if skippable?? && skippable>
                            <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
                        </#if>
                    </div>

                    <#--  Capture Picture Block  -->
                    <div class="col-12">
                        <div class="form-group mt-2 mb-0">
                            <div id="card-face-Front">
                                <@boCardFace cardFace="Front" totalSteps=totalSteps />
                            </div>
                            <#if isBothSide == true >
                                <div id="card-face-Back" class="display-init-none">
                                    <@boCardFace cardFace="Back" totalSteps=totalSteps />
                                </div>
                            </#if>
                        </div>
                    </div>

                    <#--  Preview Page  -->
                    <div id="preview_img" class="col-12 display-init-none">
                        <p class="ciba-template-subtitle text-dark mt-2 mb-2">${msg("preview-card-front")}</p>
                        <div class="text-center mx-auto">
                            <div class="position-relative">
                                <div class="ciba-template-retakeIcon p-2" id="reCapture-Front-preview" onclick="changeCardFace('Front')">
                                    <img src="/auth/assets/img/bo_retry.svg?version=${serverVersion}" id="retake-front" alt="retake picture btn" />
                                    <span>${msg("bo_retake")}<span>
                                </div>
                            </div>
                            <img src="/auth/assets/img/${title}_Front.svg?version=${serverVersion}" id="template-Front-face-preview" alt="Template Front Preview" width="100%" class="rounded" />
                        </div>
                        <p class="ciba-template-subtitle text-dark mt-3 mb-2">${msg("preview-card-back")}</p>
                        <div class="text-center mx-auto">
                            <div class="position-relative">
                                <div class="ciba-template-retakeIcon p-2" id="reCapture-Back-preview" onclick="changeCardFace('Back')">
                                    <img src="/auth/assets/img/bo_retry.svg?version=${serverVersion}" id="retake-back" alt="retake picture btn" />
                                    <span>${msg("bo_retake")}<span>
                                </div>
                            </div>
                            <img src="/auth/assets/img/${title}_Back.svg?version=${serverVersion}" id="template-Back-face-preview" alt="Template Front Preview" width="100%" class="rounded" />
                        </div>
                    </div>

                    <#--  Allow Camera   -->
                    <div class="col-12 mt-3 text-center mx-auto display-init-none" id="camera-disabled-instruction">
                          <img src="/auth/assets/img/camera.svg?version=${serverVersion}" id="template-Back-face-preview" alt="Template Front Preview" width="15%" class="rounded" />
                          <h2 class="ciba-template-title mt-3">${msg("enable_camera")}</h2>
                           <p class="ciba-template-subtitler mt-3">${msg("enable_camera_desc")}
                                <#if (ismobile?? && isdesktop??) && (ismobile || isdesktop)>
                                    <span id="platform-specific-error">${msg("enable_camera_app")}</span>
                                <#else>
                                    <span id="platform-specific-error">${msg("enable_camera_browser")}</span>
                                </#if>
                            </p>                   
                     </div>

                    <#--  Error and retake instruction -->
                    <div class="col-12 mt-3 display-init-none" id="template-error">
                        <div class="d-flex align-items-center">
                            <img src="/auth/assets/img/error-icon.svg?version=${serverVersion}" alt="i" class="my-2" />
                            <div class="mx-2 pb-1">
                                <#import "error_msg.ftl" as e>
                                <@e.error_msg />
                                <p id="cameraError" class="input-alert-text-error mt-0 mb-2"></p>
                            </div>
                        </div>
                    </div>
                </div>

                <#--  Template Buttons  -->
                <div class="row">
                    <#--  Mobile View  -->
                    <div class="col-12 display-mobile-only" id="template-button-mobile">
                        <button id="cancelBtn-mobile" name="cancel" onclick="setValue('#cancel-flow')" value="cancel" type="submit" class="w-100 py-3 mr-2 ciba-template-cancelbtn">
                            ${msg("doCancel")}
                        </button>
                        <div id="capture-mobile" onclick="captureImage()" class="w-100 py-3 ml-2 ciba-template-submitbtn text-center">
                            ${msg("bo_capture_picture")}
                        </div>
                        <div id="next-mobile" onclick="changeCardFace('')" class="w-100 py-3 ml-2 ciba-template-submitbtn text-center display-init-none">
                            ${msg("bo_next")}
                        </div>
                        <div id="allowCameraBtn-mobile" onclick="openCamera()" class="w-100 py-3 ml-2 ciba-template-submitbtn text-center display-init-none">
                            ${msg("bo_allow")}
                        </div>
                        <button name="doSubmit" value="doSubmit" id="doSubmit-mobile" type="submit" disabled onclick="submitForm()" class="w-100 py-3 ml-2 ciba-template-submitbtn display-init-none">
                            ${msg("submit")}
                        </button>
                    </div>
                    <#--  Web View  -->
                    <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                        <#if skippable?? && skippable>
                            <div id="skipBtn-web" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer">${msg("skip")}</div>
                        <#else>
                            <div id="empty-skip-fallback"></div>
                        </#if>
                        <div class="d-flex justify-content-end align-items-center">
                            <button id="cancelBtn" name="cancel" onclick="setValue('#cancel-flow')" value="cancel" type="submit" class="w-100 px-3 py-2 mr-2 ciba-template-cancelbtn">
                                ${msg("doCancel")}
                            </button>
                            <div id="capture" onclick="captureImage()" class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn text-center">
                                ${msg("bo_capture")}
                            </div>
                            <div id="next" onclick="changeCardFace('')" class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn text-center display-init-none">
                                ${msg("bo_next")}
                            </div>
                            <div id="allowCameraBtn" onclick="openCamera()" class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn text-center display-init-none">
                                ${msg("bo_allow")}
                            </div>
                            <button name="doSubmit" value="doSubmit" id="doSubmit" type="submit" disabled onclick="submitForm()" class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn display-init-none">
                                ${msg("submit")}
                            </button>
                        </div>
                    </div>
                </div>
            </form>
            <script defer>
                $(document).ready(() => {
                    setTemplateFields(${isBothSide?string});
                    <#if error??  >
                        $('#template-error').css('display', 'block');
                    </#if>
                })
            </script>
        </#if>    
    </@layout.registrationLayout>