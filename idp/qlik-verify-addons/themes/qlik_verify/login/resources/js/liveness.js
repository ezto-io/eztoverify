import {Detector, loadModel} from "./detector.min.js";

let detector = null;
let livenessConfig;
let livenessHelperTxt;
let resourcesPath;
let reqId;
let usrId;
let videoBlobsRecorded = [];
let videoRecorder = null;
let completedFlows = [];
let videoStream = null;
let isRetryFlow = false;
const startButton = document.getElementById("start-liveness-btn");
const startButtonMobile = document.getElementById("start-liveness-btn-mobile");
const TemplateFlows = document.getElementById("template-flows");
const frameRate = 12;


// Liveness Check function
const startLivenessCheck = async () => {
    // event listener for flow completion
    window.addEventListener("onLivenessUpdate", (e) => { 
        if(e.detail.is_completed){
            const livenessMeta = document.getElementById("livenessMeta");
            livenessMeta.value = JSON.stringify(e.detail.metadata);
            videoRecorder.stop();
        }
        completedFlows = e.detail.completed_flows;
        // Flow helper-text setter.
        flowHelperTextSetter(e.detail.in_progress);
        // Flow status setter
        if(e.detail.in_progress != 'face_align'){
            renderFlowsUI(e.detail.in_progress);
        }
    });

    // event listener for face to be center of recording
    window.addEventListener("faceCenterUpdate", async (e) => {
        // isCenter return whether face is centered or not(Boolean)
        if(e.detail.isCenter){
            $('#video-shadow-element').removeClass('face-failure-border');
            $('#video-shadow-element').addClass('face-success-border');
        }else{
            $('#video-shadow-element').removeClass('face-success-border');
            $('#video-shadow-element').addClass('face-failure-border');
        }
        // retry return true when face is not center for 4 seconds continuously
        if(e.detail.retry){
            stopVideoRecord();
            $('#template-intro-content-wrapper').addClass('d-none');
            $('#template-retry-content-wrapper').removeClass('d-none');
            $('#template-camera-content-wrapper').addClass('d-none');
            $('#start-liveness-btn').text(livenessHelperTxt.retryText);
            $('#start-liveness-btn-mobile').text(livenessHelperTxt.retryText);
            isRetryFlow = true;
            detector.destroy();
            detector = null;
        }
    })
     
    // Video streaming and recording
    startVideoRecord();
}

// Renders helper text for current flow
function flowHelperTextSetter(currentFlow){
    if(currentFlow == 'face_align'){
        $('#template-flow-instruction').text(livenessHelperTxt.alignFaceTxt);
    }else if(currentFlow == 'fingercount'){
        $('#template-flow-instruction').text(livenessHelperTxt.fingerCountTextStart + " "+ livenessConfig.fingercount.expected + " " + livenessHelperTxt.fingerCountTextEnd);
    }else if(currentFlow == 'blink'){
        $('#template-flow-instruction').text(livenessHelperTxt.blinkTxtStart + " " + livenessConfig.blink.expected + " " + livenessHelperTxt.blinkTxtEnd);
    }else if(currentFlow == 'speech'){
        $('#template-flow-instruction').text(livenessHelperTxt.speechTxtStart + livenessConfig.speech.expected + livenessHelperTxt.speechTxtEnd);
    }else if(currentFlow == 'movement'){
        $('#template-flow-instruction').text(livenessHelperTxt.movementTxt);
    }
}

// Renders status of flows 
function renderFlowsUI(currentFlow) {
    TemplateFlows.innerHTML = "";
    for (var i = 0; i < completedFlows.length; i++) {
        var listItem = document.createElement("li");
        var img = document.createElement('img');
        img.classList.add('pr-2'); 
        img.src = `${resourcesPath}/img/completed-tick.svg`; 
        var span = document.createElement('span');
        span.classList.add('text-white'); 
        span.textContent = livenessHelperTxt[completedFlows[i]];
        listItem.appendChild(img);
        listItem.appendChild(span);
        listItem.classList.add('flow-status-card');
        TemplateFlows.appendChild(listItem);
    }
    if(currentFlow.length > 0){
        var listItem = document.createElement("li");
        var img = document.createElement('img');
        img.classList.add('pr-2'); 
        img.style.height = "18px";
        img.src = `${resourcesPath}/img/circular-loader.gif`; 
        var span = document.createElement('span');
        span.classList.add('text-white'); 
        span.textContent = livenessHelperTxt[currentFlow];
        listItem.appendChild(img);
        listItem.appendChild(span);
        listItem.classList.add('flow-status-card');
        TemplateFlows.appendChild(listItem);
    }
}

// Streams video and record it
async function startVideoRecord(){
    try {
        videoStream = await getCameraAccess("user");
        videoStream.getVideoTracks().forEach(track => {
            track.addEventListener("ended", () => {
                stopVideoRecord();
                $('#template-video-component-wrapper').removeClass('d-flex');
                $('#template-video-component-wrapper').addClass('d-none');
                $('#template-camera-content-wrapper').removeClass('d-none');
                $('#template-intro-content-wrapper').addClass('d-none');
                $("#template-content").removeClass("d-none");
                $("#template-content").addClass("d-flex");
                $('#template-intro-content-wrapper').addClass('d-none');
                $('#start-liveness-btn').text(livenessHelperTxt.retryText);
                $('#start-liveness-btn-mobile').text(livenessHelperTxt.retryText);
            });
        });
    } catch (e) {
        $('#template-intro-content-wrapper').addClass('d-none');
        $('#template-retry-content-wrapper').addClass('d-none');
        $('#template-camera-content-wrapper').removeClass('d-none');
        $('#start-liveness-btn').removeAttr('disabled');
        $('#start-liveness-btn-mobile').removeAttr('disabled');
        $('#start-liveness-btn').text(livenessHelperTxt.retryText);
        $('#start-liveness-btn-mobile').text(livenessHelperTxt.retryText);
    }
    $("#video-element").prop("srcObject", videoStream);
    await $("#video-element")[0].play();
    videoBlobsRecorded = [];
    const options = {
        audioBitsPerSecond: 128000,
        videoBitsPerSecond: 2500000,
    };
    videoRecorder = new MediaRecorder(videoStream, options);
    videoRecorder.addEventListener("dataavailable", function (e) {
        videoBlobsRecorded.push(e.data);
    });
    videoRecorder.start(1000);
    $("#template-content").removeClass("d-flex");
    $("#template-content").addClass("d-none");
    $("#template-video-component-wrapper").removeClass("d-none");
    $("#template-video-component-wrapper").addClass("d-flex");
    $('#start-liveness-btn').removeAttr('disabled');
    $('#start-liveness-btn-mobile').removeAttr('disabled');
    detector.processFromVideo($("#video-element")[0], frameRate)
    const dataTransfer = new DataTransfer();
    videoRecorder.addEventListener("stop", async function () {
        if(videoStream == null){
                $("#template-video-component-wrapper").removeClass("d-flex");
                $("#template-video-component-wrapper").addClass("d-none");
                $("#template-content").removeClass("d-none");
                $("#template-content").addClass("d-flex");
                return;
        }
        const videoBlob = new Blob(videoBlobsRecorded, { type: "video/mp4" });
        const videoFile = new File([videoBlob], "liveliness", { type: "video/mp4" });
        const livenessVideo = document.getElementById("livenessVideo");
        dataTransfer.items.add(videoFile);
        livenessVideo.files = dataTransfer.files;
        const reader = new FileReader();
        reader.addEventListener("load", () => {
            if (videoStream != null) {
                videoStream.getTracks().forEach(function (track) {
                    track.stop();
                });
            }
            $("#template-video-component-wrapper").removeClass("d-flex");
            $("#template-video-component-wrapper").addClass("d-none");
            $("#template-loader").removeClass("d-none");
            $("#template-loader").addClass("d-flex");
            $('#bo_liveness_verification_form').submit();
        },false);
        reader.readAsDataURL(videoBlob);
    });
}

// stop streaming and recording;
function stopVideoRecord(){
    if (videoStream != null) {
        videoStream.getTracks().forEach(function (track) {
            track.stop();
            videoStream = null;
        });
    }
    completedFlows = [];
    videoRecorder.stop();
}

// Camera Stream Function
const getCameraAccess = async (facingMode) => {
    const constraints = {
        video: {
            facingMode,
        },
        audio: true,
    };
    return navigator.mediaDevices.getUserMedia(constraints);
};

// Start Button Listener
startButton.addEventListener('click', function() {
    startOrSubmitFlow();
});
startButtonMobile.addEventListener('click', function() {
    startOrSubmitFlow();
});

function startOrSubmitFlow() {
    if(isRetryFlow){
        const retryFlow = document.getElementById("retryFlow");
        retryFlow.value = "true";
        $('#bo_liveness_verification_form').submit();
    }else{
        $('#start-liveness-btn').attr('disabled', 'disabled');
        $('#start-liveness-btn-mobile').attr('disabled', 'disabled');
        startLivenessCheck();
    }
}

// Initial Modal Loader
function init() {
    async function initialize() {
            loadModel(livenessConfig)
            .then((model) => {
                detector = new Detector(livenessConfig, 256, 320, model);
                $("#modal-loader").addClass("d-none");
                $("#template-button-mobile").removeClass("d-none");
                $("#template-button-web").removeClass("d-none");
                $("#template-button-mobile").addClass("display-mobile-only");
                $("#template-button-web").addClass("display-web-only");
                if (isMobileSafari()) {
                    var formBodies = document.querySelectorAll('.form-body');
                    formBodies.forEach(function(formBody) {
                        formBody.setAttribute("style", "height: 80vh !important;");
                    });
                }
            }).catch(() => {
                $("#modal-loader-text").text(livenessHelperTxt.modalLoaderResetMsg);
                initialize();
            } );
    }
    initialize();
};

// Check for Safari Mobile
function isMobileSafari() {
    return /iP(ad|hone|od).*Version\/[\d.]+.*Safari/i.test(navigator.userAgent) && 
            !window.navigator.userAgent.includes('CriOS') && 
            !window.navigator.userAgent.includes('FxiOS');
}

// Change modal loader text
setInterval(() => $("#modal-loader-text").text(livenessHelperTxt.modalLoaderMsg), 15000);

// Setter function called from FTL
export function setLivenessConfig(config, path, livenessHelpers, requestId, userId) {
    livenessConfig = config;
    resourcesPath = path;
    livenessHelperTxt = livenessHelpers;
    reqId = requestId;
    usrId = userId;
    init();
}