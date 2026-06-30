<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "head">
        <script defer src="/auth/assets/js/liveness.min.js?version=${serverVersion}" type="module"></script>
    </#if>
    <#if section="form">
        <form enctype="multipart/form-data" id="bo_liveness_verification_form" action="${url.loginAction}" class="form-body" method="post">
            <input name="livenessVideo" type="file" id="livenessVideo" class="d-none">
            <input name="livenessMeta" type="text" id="livenessMeta" class="d-none">
            <input name="retry" type="text" id="retryFlow" class="d-none">
            <input name="skipped" type="hidden" id="skip-flow" value="false" />
            <#--  Intro Component  -->
            <div class="col-12 flex-column justify-content-between d-flex form-body pb-4 px-4" id="template-content">
                <#--  Intro Header  -->
                <div class="row" id="template-header-wrapper">
                    <div class="col-12 d-flex justify-content-between align-items-center mt-3 mb-4">
                        <h2 class="ciba-template-title">${msg("liveness-verification")}</h2>
                        <#if skippable?? && skippable>
                            <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
                        </#if>
                    </div>
                </div>
                <#--  Intro Content  -->
                <div class="row" id="template-intro-content-wrapper">
                    <div class="col-12 px-4 py-1">
                        <p class="liveness-intro-title" >${msg("liveness-intro-description")}</p>
                        <h6 class="liveness-intro-sub-title text-center mt-4">${msg("instructions")}</h6>
                        <ul>
                            <li class="py-1">${msg("liveness-intro-instruction1")}</li>
                            <li class="py-1">${msg("liveness-intro-instruction2")}</li>
                            <li class="py-1">${msg("liveness-intro-instruction3")}</li>
                        </ul>
                    </div>
                </div>
                <#--  Camera Instruction  -->
                <div class="row d-none" id="template-camera-content-wrapper">
                    <div class="col-12 p-4 text-center mx-auto">
                        <img src="/auth/assets/img/camera.svg?version=${serverVersion}" id="camera-icon" alt="camera icon" width="15%" class="rounded" />
                        <p class="liveness-intro-title mt-3">${msg("enable_camera")}</p>
                        <p class="mt-3">${msg("enable_camera_desc")}
                            <#if (ismobile?? && isdesktop??) && (ismobile || isdesktop)>
                                <span id="platform-specific-error">${msg("enable_camera_app")}</span>
                            <#else>
                                <span id="platform-specific-error">${msg("enable_camera_browser")}</span>
                            </#if>
                        </p>
                    </div>
                </div>
                <#--  Retry Instruction  -->
                <div class="row d-none" id="template-retry-content-wrapper">
                    <div class="col-12 p-4 text-center mx-auto">
                        <img src="/auth/assets/img/retry.svg?version=${serverVersion}" id="camera-icon" alt="camera icon" width="15%" class="rounded" />
                        <p class="liveness-intro-title mt-3" >${msg("retry")}</p>
                        <p class="text-center px-4"> ${msg("liveness-retry-description")}</p>
                    </div>
                </div>
                <#--  Intro Buttons  -->
                <div class="row" id="template-button-wrapper">
                    <#--  Mobile View  -->
                    <div class="col-12 d-none mb-4" id="template-button-mobile">
                        <button id="cancelBtn-mobile" name="cancel" value="cancel" type="submit" class="w-100 py-3 mr-2 ciba-template-cancelbtn">${msg("doCancel")}</button>
                        <button id="start-liveness-btn-mobile" type="button" class="w-100 py-3 ml-2 ciba-template-submitbtn">${msg("start")}</button>
                    </div>
                    <#--  Web View  -->
                    <div class="justify-content-between align-items-center mt-3 d-none w-100 px-3" id="template-button-web">
                        <#if skippable?? && skippable>
                            <div id="skipBtn-web" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer">${msg("skip")}</div>
                        <#else>
                            <div id="empty-skip-fallback"></div>
                        </#if>
                        <div class="d-flex justify-content-end align-items-center">
                            <button id="cancelBtn" name="cancel" value="cancel" type="submit" class="w-100 px-3 py-2 mr-2 ciba-template-cancelbtn">${msg("doCancel")}</button>
                            <button id="start-liveness-btn" type="button" class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn">${msg("start")}</button>
                        </div>
                    </div>
                    <#--  modal loader  -->
                    <div class="col-12 fade-in-out px-2 mb-3" id="modal-loader">
                        <h6 class="py-4 px-2 rounded modal-loading-card text-center" id="modal-loader-text">${msg("liveness-modal-loader-msg1")}</h6>
                    </div>
                </div>
            </div>
            <#-- Video Component  -->
            <div class="liveness-video-containter form-body d-none"  id="template-video-component-wrapper">
                <div class="video-inner-element video-element">
                    <video id="video-element" class="rounded " autoplay playsinline muted></video>
                </div>
                <div class="video-inner-element shadow-overlay face-failure-border" id="video-shadow-element"></div>
                <div class="video-inner-element info-element pt-3">
                    <p id="template-flow-instruction" class="text-center liveness-instruction-text px-2"></p>
                    <div class="d-flex justify-content-center position-relative align-items-center" >
                        <ul id="template-flows" class="d-flex flex-wrap justify-content-center px-2 mb-5" style="list-style-type: none;"></ul>
                    </div>
                </div>
            </div>
            <#--  Template Loader  -->
            <div class="col-12 d-none justify-content-center align-items-center form-body" id="template-loader">
                <#if isMobile?? && !isMobile>
                    <div class="spinner-border text-primary" role="status">
                        <span class="sr-only">Loading...</span>
                    </div>
                <#else>
                    <p>${msg("video-uploading")}</p>
                </#if>
            </div>
        </form>
        <script type="module" defer>
            import {setLivenessConfig} from "/auth/assets/js/liveness.min.js?version=${serverVersion}"
            $(document).ready(function() {      
                <#noautoesc>  
                const livenessHelpers = {
                    "alignFaceTxt" : "${msg('liveness-align-face-center-helper-text')}",
                    "movementTxt" : "${msg('liveness-movement-helper-text')}",
                    "speechTxtStart" : "${msg('liveness-speech-helper-text-start')}",
                    "speechTxtEnd" : "${msg('liveness-speech-helper-text-end')}",
                    "blinkTxtStart" : "${msg('liveness-blink-helper-text-start')}",
                    "blinkTxtEnd" : "${msg('liveness-blink-helper-text-end')}",
                    "fingerCountTextStart" : "${msg('liveness-fingercount-helper-text-start')}",
                    "fingerCountTextEnd" : "${msg('liveness-fingercount-helper-text-end')}",
                    "retryText": "${msg('retry')}",
                    "modalLoaderMsg": "${msg('liveness-modal-loader-msg2')}",
                    "modalLoaderResetMsg": "${msg('liveness-modal-loader-msg1')}",
                    "fingercount" : "${msg('fingercount')}",
                    "movement" : "${msg('movement')}",
                    "speech" : "${msg('speech')}",
                    "blink" : "${msg('blink')}",
                    "retryText": "${msg('retry')}"
                } 
                </#noautoesc>
                setLivenessConfig({
                    <#list config?keys as key>
                        "${key}": {
                            <#list config[key]?keys as subKey>
                                "${subKey}": 
                                    <#if config[key][subKey]?is_string>"${config[key][subKey]}"
                                    <#elseif config[key][subKey]?is_number> ${config[key][subKey]}
                                    <#else>
                                        <#if config[key][subKey]>true<#else>false</#if>
                                    </#if>,
                            </#list>
                        },
                    </#list>
                }, "/auth/assets", livenessHelpers, "${requestId}", "${userId}");
            });
        </script>
        <script defer>
            var isTransactionFlowSkipped = false;
            function transactionSkip() {
                if(!isTransactionFlowSkipped){
                    isTransactionFlowSkipped = true;
                    $("#skip-flow").val("true");
                    $('form').submit();
                }
            }
        </script>
    </#if>
</@layout.registrationLayout>