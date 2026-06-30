<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "head">
        <script defer src="/auth/assets/js/selfieRegister.min.js?version=${serverVersion}" type="module"></script>
        <#if geo_mobile?? && geo_mobile>
            <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
            <script defer type="text/javascript" src="/auth/assets/js/base64url.min.js?version=${serverVersion}"></script>
            <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
        </#if>
    </#if>
    <#if section="form">
        <form id="bo_selfie_verification_form" action="${url.loginAction}" class="form-body" method="post">
            <input name="liveliness_image" type="hidden" id="selfie_image" value="true">
            <input name="liveliness_image_details" type="hidden" id="selfie_image_details">
            <input name="skipped" type="hidden" id="skip-flow" value="false" />

            <input type="hidden" id="latitude" name="latitude"/>
            <input type="hidden" id="longitude" name="longitude"/>
            <input type="hidden" id="accuracy" name="accuracy"/>
            <input type="hidden" id="errorMsg" name="errorMsg" />
            <input type="hidden" id="geo_mobile" name="geo_mobile" value="{geo_mobile?c}"/>
            <input type="hidden" id="location" name="location"/>
            <input type="hidden" id="success" name="success" value="true"/>
            <input type="hidden" id="time" name="time"/>

            <#--  Intro Component  -->
            <div class="col-12 d-flex flex-column justify-content-between form-body pb-4 px-4" id="template-content">
                <#--  Intro Header  -->
                <div class="row" id="template-header-wrapper">
                    <div class="col-12 d-flex justify-content-between align-items-center mt-3">
                        <h2 class="ciba-template-title">
                            <#if title??>
                                ${msg("${title}")}
                            <#else>
                                ${msg("liveness-verification")}
                            </#if>
                        </h2>
                        <#if skippable?? && skippable>
                            <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
                        </#if>
                    </div>
                </div>
                <#--  Intro Content  -->
                <div class="row" id="template-intro-content-wrapper">
                    <div class="col-12 px-4 py-1">
                        <p class="liveness-intro-title" >${msg("liveness-intro-title")}</p>
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
                <#--  Retry and Preview  -->
                <div class="row d-none" id="template-preview-content-wrapper">
                    <div class="col-12 p-4 text-center mx-auto">
                        <div class="position-relative">
                            <button type="button" class="ciba-template-retakeIcon p-2" id="retake-selfie-btn">
                                <img src="/auth/assets/img/bo_retry.svg?version=${serverVersion}" id="retake" alt="retake picture btn" />
                                <span>${msg("bo_retake")}<span>
                            </button>
                        </div>
                        <img src="/auth/assets/img/bo_liveliness_dos.png?version=${serverVersion}" id="selfie-image-preview" alt="Preview Image" width="100%" height="100%" class="rounded selfie-preview-image" />
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
                    <div class="col-12 fade-in-out px-2" id="modal-loader">
                        <h6 class="py-4 px-2 rounded modal-loading-card text-center" id="modal-loader-text">${msg("liveness-modal-loader-msg1")}</h6>
                    </div>
                </div>
            </div>
            <#-- Video Component  -->
            <div class="liveness-video-containter form-body d-none"  id="template-video-component-wrapper">
                <div class="video-inner-element video-element">
                    <video id="video-element" class="rounded" autoplay playsinline muted></video>
                    <canvas id="canvas-element" class="d-none"></canvas>
                </div>
                <div class="video-inner-element shadow-overlay face-failure-border" id="video-shadow-element"></div>
                <div class="video-inner-element info-element pt-3">
                    <p id="template-flow-instruction" class="text-center liveness-instruction-text px-2">${msg("liveness-retry-description")}</p>
                </div>
            </div>
            <#--  Template Loader  -->
            <div class="col-12 d-none justify-content-center align-items-center form-body" id="template-loader">
                <#if isMobile?? && !isMobile>
                    <div class="spinner-border text-primary" role="status">
                        <span class="sr-only">Loading...</span>
                    </div>
                <#else>
                    <p>${msg("image-uploading")}</p>
                </#if>
            </div>
        </form>

        <script type="text/javascript">
            $(document).ready(async function () {
                console.log("Ready");
                var geo_mobile = <#if geo_mobile?? && geo_mobile>true<#else>false</#if>;
                var geoTaggingEnabled = <#if geo_tagging_enabled?? && geo_tagging_enabled>true<#else>false</#if>;

                $("#geo_mobile").val(geo_mobile);
                console.log("Executing geolocation first...");

                if (geoTaggingEnabled) {
                    try {
                        if (geo_mobile) {
                            await getMobileGeoLocation();
                        } else {
                            await getWebGeoLocation();
                        }
                        console.log("Geolocation successfully retrieved. Proceeding with other functions.");
                    } catch (err) {
                        console.error("Geolocation error:", err);
                        $("#errorMsg").val(err.message);
                    }
                }
                initializeOtherFeatures();
            });

            function getMobileGeoLocation() {
                return new Promise((resolve, reject) => {
                    readDeviceDetails().then(function (result) {
                        $("#errorMsg").val(result.message);
                        $("#success").val(result.success);
                        $("#location").val(JSON.stringify(result.location));
                        $("#time").val(JSON.stringify(result.time));

                        if (result.success) {
                            resolve();
                        } else {
                            reject(new Error(result.message || "Failed to fetch mobile geolocation"));
                        }
                    }).catch(err => reject(err));
                });
            }

            function getWebGeoLocation() {
                return new Promise((resolve, reject) => {
                    const options = {
                        enableHighAccuracy: true,
                        timeout: 30000,
                        maximumAge: 0,
                    };
                    if (navigator.geolocation) {
                        navigator.geolocation.getCurrentPosition(pos => {
                            const coords = pos.coords;
                            $("#latitude").val(coords.latitude);
                            $("#longitude").val(coords.longitude);
                            $("#accuracy").val(coords.accuracy);
                            $("#errorMsg").val("");
                            resolve();
                        }, err => {
                            $("#errorMsg").val(err.message);
                            reject(err);
                        }, options);
                    } else {
                        reject(new Error("Geolocation not supported"));
                    }
                });
            }

            function initializeOtherFeatures() {
                console.log("Initializing other features like camera...");
                setTimeout(() => {
                    // Initialize camera or other scripts here
                    $("#start-liveness-btn").prop("disabled", false); // Enable the button after geolocation
                }, 1000);
            }

            function success(pos){
                const coords = pos.coords;
                $("#latitude").val(coords.latitude);
                $("#longitude").val(coords.longitude);
                $("#accuracy").val(coords.accuracy);
                $("#errorMsg").val("");
            }

            function error(err){
                $("#errorMsg").val(err.message);
            }

        </script>

        <script type="module" defer>
            import {setConfig} from "/auth/assets/js/selfieRegister.min.js?version=${serverVersion}"
            $(document).ready(function() {
                <#noautoesc>
                const selfieHelpers = {
                    "submitText": "${msg('submit')}",
                    "modalLoaderMsg": "${msg('liveness-modal-loader-msg2')}",
                    "modalLoaderResetMsg": "${msg('liveness-modal-loader-msg1')}",
                    "retryText": "${msg('retry')}"
                }
                </#noautoesc>
                setConfig({
                    "selfie": {
                        "enabled": true
                    }
                }, "/auth/assets", selfieHelpers);

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