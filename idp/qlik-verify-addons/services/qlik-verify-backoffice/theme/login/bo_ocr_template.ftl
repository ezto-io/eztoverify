<#import "template.ftl" as layout>
    <@layout.registrationLayout; section>
        <#if section = "head">
            <script defer src="/auth/assets/js/socket.min.js?version=${serverVersion}" type="text/javascript"></script>
            <script defer src="/auth/assets/js/ocrTemplate.min.js?version=${serverVersion}" type="text/javascript"></script>
            <script defer src="/auth/assets/js/common.min.js?version=${serverVersion}" type="text/javascript"></script>
            <link rel="preload stylesheet" as="style" href="/auth/assets/css/backoffice.min.css?version=${serverVersion}"/>
        </#if>
        <#include "bo_card_face.ftl" />
        <#if isBothSide?? && isBothSide == true >
            <#assign totalSteps = 2 >
        <#else>
            <#assign totalSteps = 1 >
        </#if>
        <#if section="form">
            <form id="bo_image_verification_form" action="${url.loginAction}" class="d-flex flex-column justify-content-between ciba-template-height" method="post" >
                <#--  Template Hidden Fields  -->
                <div class="row">
                    <#if hiddenFields?? >
                        <#list hiddenFields as field>
                            <input id="${field.fieldName}" name="${field.fieldName}" type="hidden" value="${field.fieldValue}"  />
                        </#list>
                    </#if>

                    <#--  Template Header  -->
                    <div class="col-12 d-flex justify-content-between align-items-center mt-2" id="template-header">
                        <h2 class="ciba-template-title mb-0">${msg(title)}</h2>
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
                          <p class="ciba-template-subtitler mt-3">${msg("enable_camera_desc")}</p>
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
                    <div class="col-12 d-flex mt-3">
                        <button id="cancelBtn" name="cancel" onclick="setValue('#cancel-flow')" value="cancel" type="submit" class="w-100 py-3 mr-2 ciba-template-cancelbtn">
                            ${msg("doCancel")}
                        </button>
                        <div id="capture" onclick="captureImage()" class="w-100 py-3 ml-2 ciba-template-submitbtn text-center">
                            ${msg("bo_capture_picture")}
                        </div>
                        <div id="next" onclick="changeCardFace('')" class="w-100 py-3 ml-2 ciba-template-submitbtn text-center display-init-none">
                            ${msg("bo_next")}
                        </div>
                        <div id="allowCameraBtn" onclick="openCamera()" class="w-100 py-3 ml-2 ciba-template-submitbtn text-center display-init-none">
                            ${msg("bo_allow")}
                        </div>
                        <button name="doSubmit" value="doSubmit" id="doSubmit" type="submit" disabled onclick="submitForm()" class="w-100 py-3 ml-2 ciba-template-submitbtn display-init-none">
                            ${msg("submit")}
                        </button>
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