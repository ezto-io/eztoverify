let stream;
let isCaptured = {};
isCaptured['Front'] = false;
isCaptured["Back"] = false;
let isCameraOpened = false;
let isRetake = false;
let isBothSides = false;
let cardFace = "Front";
const captureBtn = document.querySelector("#capture");
const captureBtnMobile = document.querySelector("#capture-mobile");
const submitBtn = document.querySelector("#doSubmit");
const submitBtnMobile = document.querySelector("#doSubmit-mobile");
const nextBtn = document.querySelector("#next");
const nextBtnMobile = document.querySelector("#next-mobile");
const cardFront = document.querySelector("#card-face-Front");
const cardBack = document.querySelector("#card-face-Back");
const errorElement = document.querySelector("#cameraError");

const captureImage = () => {
    if (isCameraOpened) {
        takePicture();
    } else {
        openCamera();
    }
}

// capture picture
const takePicture = () => {
    const canvas = $("#canvas-" + cardFace)[0];
    const video = $("#video-" + cardFace)[0]
    const context = canvas.getContext("2d");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0);
    const image = canvas.toDataURL("image/png");
    $("#template-" + cardFace + "-face").prop('src', image);
    $("#" + cardFace + "_vid").css('display', 'none');
    $("#" + cardFace + "_img").css('display', 'block');
    isCaptured[cardFace] = true;
    isCameraOpened = false;
    captureBtn.style.display = "none";
    captureBtnMobile.style.display = "none";
    if(isBothSides) {
        nextBtn.style.display = "block";
        nextBtnMobile.style.display = "block";
    } else {
        submitBtn.style.display = "block";
        submitBtnMobile.style.display = "block";
    }
    enableButton("#doSubmit");
    enableButton("#doSubmit-mobile");
    $("#widget-inprogress-" + cardFace).css("width", "100%");
    stopStream();
};

// open video to capture picture
const openCamera = async () => {
    try {
        disableButton("#capture");
        isCaptured[cardFace] = false;
        isCameraOpened = true;
        stopStream();
        stream = await getCameraAcess();
        stream.getVideoTracks().forEach(track => {
            track.addEventListener("ended", () => {
                if (!isCaptured[cardFace]) {
                    stopStream();
                    $("#" + cardFace + "_instruction").css('display', 'none');
                    $("#" + cardFace + "_vid").css('display', 'none');
                    $("#camera-disabled-instruction").css('display', 'block');
                    $("#" + cardFace + "_instruction").css('display', 'none');
                    $("#" + cardFace + "_vid").css('display', 'none');
                    $("#camera-disabled-instruction").css('display', 'block');
                    captureBtn.style.display = "none";
                    captureBtnMobile.style.display = "none";
                    $("#allowCameraBtn").css('display', 'block');
                    $("#allowCameraBtn-mobile").css('display', 'block');
                    if (isCaptured[cardFace]) {
                        $("#widget-inprogress-" + cardFace).css("width", "100%");
                    } else {
                        $("#widget-inprogress-" + cardFace).css("width", "30%");
                    }
                }
            });
        });
        await showVideo(stream);
        $("#widget-inprogress-" + cardFace).css("width", "80%");
        nextBtn.style.display = "none";
        nextBtnMobile.style.display = "none";
        submitBtn.style.display = "none";
        submitBtnMobile.style.display = "none";
        $("#allowCameraBtn").css('display', 'none');
        $("#allowCameraBtn-mobile").css('display', 'none');
        captureBtn.style.display = "block";
        captureBtnMobile.style.display = "block";
        errorElement.innerHTML = "";
    } catch (e) {
        isCameraOpened = false;
        $("#" + cardFace + "_instruction").css('display', 'none');
        $("#" + cardFace + "_vid").css('display', 'none');
        $("#camera-disabled-instruction").css('display', 'block');
        captureBtn.style.display = "none";
        captureBtnMobile.style.display = "none";
        $("#allowCameraBtn").css('display', 'block');
        $("#allowCameraBtn-mobile").css('display', 'block');
        if (isCaptured[cardFace]) {
            $("#widget-inprogress-" + cardFace).css("width", "100%");
        } else {
            $("#widget-inprogress-" + cardFace).css("width", "30%");
        }
    }
};

// change camera type
const changeCardFace = (face) => {
    if (cardFace == 'Front' && isCaptured[cardFace] && isBothSides && !face) {
        if (!isRetake) {
            cardFace = 'Back';
            $("#" + cardFace + "_vid").css("display", "none");
            $("#" + cardFace + "_img").css("display", "none");
            captureBtn.style.display = "block";
            captureBtnMobile.style.display = "block";
            cardBack.style.display = "block"
        } else {
            cardFace = 'Preview';
            $('#preview_img').css('display', 'block');
            $("#template-Front-face-preview").prop('src', $('#template-Front-face').attr('src'));
            submitBtn.style.display = "block";
            submitBtnMobile.style.display = "block";
            isRetake = false;
        }
        nextBtn.style.display = "none";
        nextBtnMobile.style.display = "none";
        cardFront.style.display = "none";
    } else if (cardFace == 'Back' && isCaptured[cardFace] && isBothSides && !face) {
        $('#preview_img').css('display', 'block');
        $("#template-Front-face-preview").prop('src', $('#template-Front-face').attr('src'));
        $("#template-Back-face-preview").prop('src', $('#template-Back-face').attr('src'));
        cardFace = 'Preview';
        nextBtn.style.display = "none";
        nextBtnMobile.style.display = "none";
        submitBtn.style.display = "block";
        submitBtnMobile.style.display = "block";
        cardBack.style.display = "none";
    } else if (cardFace == 'Preview' && isCaptured[face] && isBothSides) {
        $('#preview_img').css('display', 'none');
        submitBtn.style.display = "none";
        submitBtnMobile.style.display = "none";
        captureBtn.style.display = "block";
        captureBtnMobile.style.display = "block";
        if (face == 'Front') {
            cardFront.style.display = "block";
            cardFace = "Front";
            isRetake = true;
        } else if (face == 'Back') {
            cardBack.style.display = "block";
            cardFace = "Back";
        }
        $("#" + cardFace + "_img").css("display", "none");
        $("#" + cardFace + "_instruction").css("display", "block");
        captureImage();
    }
}

// Get camera access from browser
const getCameraAcess = () => {
    const constraints = {
        video: {
            facingMode: "environment",
        },
    };
    if (window.innerWidth <= 768 && window.innerHeight <= 1024) {
        $("#video-" + cardFace).css('transform', 'none');
    }
    return navigator.mediaDevices.getUserMedia(constraints);
};

// display video to take picture
const showVideo = async(stream) => {
    $('#template-error').css('display', 'none');
    $("#" + cardFace + "_instruction").css('display', 'none');
    $("#" + cardFace + "_img").css('display', 'none');
    $("#camera-disabled-instruction").css('display', 'none');
    $("#" + cardFace + "_vid").css('display', 'block');
    const video = $("#video-" + cardFace)[0];
    video.srcObject = null;
    video.srcObject = stream;
    await video.play();
    disableButton("#doSubmit");
    disableButton("#doSubmit-mobile");
    await enableButton("#capture");
    await enableButton("#capture-mobile");
};

// stop streaming video
const stopStream = () => {
    if (stream != null) {
        stream.getTracks().forEach(function (track) {
            track.stop();
        });
    }
};

const setTemplateFields = (bothSides) => {
    isBothSides = bothSides;
}

// submit form
function submitForm() {
    let event = {};
    event['front'] = $('#template-Front-face').attr('src');
    if(isBothSides){
        event['back'] = $('#template-Back-face').attr('src');
    }
    $("#image_details").val(JSON.stringify(event));
    $('#doSubmit').attr('disabled', 'disabled');
    $('#doSubmit-mobile').attr('disabled', 'disabled');
    $('form').submit();
}

//  document onload 
$(document).ready(function () {
    $("#video-" + cardFace).removeAttr('controls'); 
    $('#Front_vid').css('display', 'none');
    $('#Front_img').css('display', 'none');
    $('#doSubmit').css('display', 'none');
    $('#doSubmit-mobile').css('display', 'none');
    $('#next').css('display', 'none');
    $('#next-mobile').css('display', 'none');
    $('#card-face-Back').css('display', 'none');
    $('#preview_img').css('display', 'none');
    $('#camera-disabled-instruction').css('display', 'none');
    $('#allowCameraBtn').css('display', 'none');
    $('#allowCameraBtn-mobile').css('display', 'none');
    $(document).keypress(function (event) {
        if (event.which === 13 && isCaptured) {
            $("#doSubmit").click();
        }
    });
});