import {Detector, loadModel} from "./detector.min.js";

let detector = null;
let selfieConfig;
let selfieHelperTxt;
let resourcesPath;
let selfieModel;
let usrId;
let reqId;
let isImageCaptured = false;
let videoStream = null;
const frameRate = 12;
const startButton = document.getElementById("start-liveness-btn");
const startButtonMobile = document.getElementById("start-liveness-btn-mobile");
const retakeButton = document.getElementById("retake-selfie-btn");


// Liveness Check function
const startSelfieRegistration = async () => {

    const faceCenterHandler = async (e) => {
        // isCenter return whether face is centered or not(Boolean)
        if(e.detail.isCenter){
            $('#video-shadow-element').removeClass('face-failure-border');
            $('#video-shadow-element').addClass('face-success-border');
        }else{
            $('#video-shadow-element').removeClass('face-success-border');
            $('#video-shadow-element').addClass('face-failure-border');
        }
        // takeSelfie return true when face is center for 4 seconds continuously
        if(e.detail.takeSelfie){
            detector.destroy();
            detector = null;
            detector = new Detector(selfieConfig, 256, 320, selfieModel);
            $('#template-video-component-wrapper').removeClass('d-flex');
            $('#template-video-component-wrapper').addClass('d-none');
            $("#template-content").removeClass("d-none");
            $("#template-content").addClass("d-flex");
            $('#template-intro-content-wrapper').addClass('d-none');
            $('#template-preview-content-wrapper').removeClass('d-none');
            $('#template-camera-content-wrapper').addClass('d-none');
            $('#start-liveness-btn').text(selfieHelperTxt.submitText);
            $('#start-liveness-btn-mobile').text(selfieHelperTxt.submitText);
            window.removeEventListener("faceCenterUpdate", faceCenterHandler);
            capturePicture();
        }
    }
    // event listener for face to be center of recording
    window.addEventListener("faceCenterUpdate", faceCenterHandler)
     
    // Video streaming and recording
    startVideoStreaming();
}


// Streams video and record it
async function startVideoStreaming(){
    try {
        videoStream = await getCameraAccess("user");
        videoStream.getVideoTracks().forEach(track => {
            track.addEventListener("ended", () => {
                if (!isImageCaptured) {
                    stopVideoStreaming();
                    $('#template-video-component-wrapper').removeClass('d-flex');
                    $('#template-preview-content-wrapper').addClass('d-none');
                    $('#template-video-component-wrapper').addClass('d-none');
                    $('#template-camera-content-wrapper').removeClass('d-none');
                    $('#template-intro-content-wrapper').addClass('d-none');
                    $("#template-content").removeClass("d-none");
                    $("#template-content").addClass("d-flex");
                    $('#template-intro-content-wrapper').addClass('d-none');
                    $('#start-liveness-btn').text(selfieHelperTxt.retryText);
                    $('#start-liveness-btn-mobile').text(selfieHelperTxt.retryText);
                }
            });
        });
    } catch (e) {
        $('#template-intro-content-wrapper').addClass('d-none');
        $('#template-preview-content-wrapper').addClass('d-none');
        $('#template-camera-content-wrapper').removeClass('d-none');
        $('#start-liveness-btn').removeAttr('disabled');
        $('#start-liveness-btn-mobile').removeAttr('disabled');
        $('#start-liveness-btn').text(selfieHelperTxt.retryText);
        $('#start-liveness-btn-mobile').text(selfieHelperTxt.retryText);

    }
    isImageCaptured = false;
    $("#video-element").prop("srcObject", videoStream);
    await $("#video-element")[0].play();
    $("#template-content").removeClass("d-flex");
    $("#template-content").addClass("d-none");
    $("#template-video-component-wrapper").removeClass("d-none");
    $("#template-video-component-wrapper").addClass("d-flex");
    $('#retake-selfie-btn').removeAttr('disabled');
    $('#start-liveness-btn').removeAttr('disabled');
    $('#start-liveness-btn-mobile').removeAttr('disabled');
    detector.processFromVideo($("#video-element")[0], frameRate);
}

// stop streaming the video
function stopVideoStreaming(){
    if (videoStream != null) {
        videoStream.getTracks().forEach(function (track) {
            track.stop();
            videoStream = null;
        });
    }
}

// Save frame in canvas and show in it img tag
const capturePicture = () => {
    if (isImageCaptured) {
        return;
    }
    const canvas = $("#canvas-element")[0]; 
    const context = canvas.getContext("2d"); 
    const video = $("#video-element")[0];
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0);
    const image = canvas.toDataURL("image/png"); 
    $('#selfie-image-preview').attr('src', image);
    isImageCaptured = true;
    stopVideoStreaming();
};

// Save image in hidden tag and submits form
function submitForm() {
    let event = {};
    event['front'] = $('#selfie-image-preview').attr('src');
    $("#selfie_image_details").val(JSON.stringify(event));
    $('#start-liveness-btn').attr('disabled', 'disabled');
    $('#start-liveness-btn-mobile').attr('disabled', 'disabled');
    $("#template-content").removeClass("d-flex");
    $("#template-content").addClass("d-none");
    $("#template-loader").removeClass("d-none");
    $("#template-loader").addClass("d-flex");
    $('form').submit();
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
    if(isImageCaptured){
        submitForm();
    }else{
        $('#start-liveness-btn').attr('disabled', 'disabled');
        $('#start-liveness-btn-mobile').attr('disabled', 'disabled');
        startSelfieRegistration();
    }
}

// Retake Button Lintener
retakeButton.addEventListener('click', function() {
    $('#retake-selfie-btn').attr('disabled', 'disabled');
    $('#video-shadow-element').removeClass('face-success-border');
    $('#video-shadow-element').addClass('face-failure-border');
    startSelfieRegistration();
});

// Initial Modal Loader
function init() {
    async function initialize() {       
        loadModel(selfieConfig)
            .then((model) => {
                detector = new Detector(selfieConfig, 256, 320, model);
                selfieModel = model;
                $("#modal-loader").addClass("d-none");
                $("#template-button-mobile").removeClass("d-none");
                $("#template-button-web").removeClass("d-none");
                $("#template-button-mobile").addClass("display-mobile-only");
                $("#template-button-web").addClass("display-web-only");
                if (isMobileSafari()) {
                    var formBodies = document.querySelectorAll('.form-body');
                    formBodies.forEach(function(formBody) {
                        formBody.classList.add('height-80vh');
                    });
                }
            }).catch(() => {
                $("#modal-loader-text").text(livenessHelperTxt.modalLoaderResetMsg);
                initialize();
            });
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
setInterval(() => $("#modal-loader-text").text(selfieHelperTxt.modalLoaderMsg), 10000);

// Setter function called from FTL
export function setConfig(config, path, selfieHelpers, requestId, userId) {
    selfieConfig = config;
    resourcesPath = path;
    selfieHelperTxt = selfieHelpers;
    reqId = requestId;
    usrId = userId;
    init();
}