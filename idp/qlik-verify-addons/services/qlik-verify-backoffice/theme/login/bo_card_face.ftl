<#macro boCardFace cardFace totalSteps>
    <div>
        <p class="ciba-template-subtitle">${msg("bo_capture_picture")} (${msg(cardFace)} ${msg('Side')})</p>

        <#--  Stage widget  -->
        <div class="ciba-template-stage-widget">
            <#if cardFace == 'Front' >
                <#assign currentStep = 1 >
            <#elseif cardFace == 'Back' >
                <#assign currentStep = 2 >
            </#if>
            <#if totalSteps?? && totalSteps == 2 >
                <#list 1..totalSteps as step>
                    <#if step < currentStep >
                        <div class="ciba-template-widget-completed mx-1" id="widget-${step}" ></div>
                    <#elseif step == currentStep >
                        <div class="ciba-template-widget-not-completed mx-1">
                            <div class="ciba-template-widget-pending" id="widget-inprogress-${cardFace}" ></div>
                        </div>
                    <#else>
                        <div class="ciba-template-widget-not-completed mx-1" id="widget-${step}"></div>
                    </#if>
                </#list>
            </#if>
        </div>

        <#-- Template Instruction  -->
        <div id="${cardFace}_instruction" class="mt-3 text-center mx-auto">
            <img src="/auth/assets/img/${title}_${cardFace}.svg?version=${serverVersion}" id="template-instruction" alt="${title}-cover-image" width="100%" />
        </div>

        <#--  Image Capturing  -->
        <div id="${cardFace}_vid" class="mt-3 text-center mx-auto display-init-none">
            <video id="video-${cardFace}" style="width: 95%; height: 100%;transform: scaleX(-1);" class="rounded" autoplay playsinline muted></video>
            <canvas id="canvas-${cardFace}" style="display: none;"></canvas>
        </div>

        <#--  Image Captured Output  -->
        <div id="${cardFace}_img" class="mt-3 text-center mx-auto display-init-none">
            <div class="position-relative">
                <div class="ciba-template-retakeIcon p-2" id="reCapture-${cardFace}" onclick="captureImage()">
                    <img src="/auth/assets/img/bo_retry.svg?version=${serverVersion}" id="retake" alt="retake picture btn" />
                    <span>${msg("bo_retake")}<span>
                </div>
            </div>
            <img src="/auth/assets/img/${title}_${cardFace}.svg?version=${serverVersion}" id="template-${cardFace}-face" alt="Preview Image" width="95%" class="rounded" />
        </div>
    </div>
</#macro>