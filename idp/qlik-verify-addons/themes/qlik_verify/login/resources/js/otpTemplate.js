function validateOtp() {
    $('#template-error').css('display', 'none');
    $("#otp").val($("#otp").val().replace(/[^0-9]/g,''))
    const otpLength =  $("#otp").val().length;
    if (otpLength){
        $("#nextBtn").removeAttr('disabled');
        $("#nextBtn-mobile").removeAttr('disabled');
    } else {
        $("#nextBtn").attr('disabled', 'disabled');
        $("#nextBtn-mobile").attr('disabled', 'disabled');
    }
}

function editFunction() {
    $("#resendOTP").val("false");
    $("#edit").val("true");
    $("#otp-verification-form").submit();
}

// function resendOTPFun() {
//     $("#resendOTP").val("true");
//     $("#edit").val("false");
//     $("#otp-verification-form").submit();
// }

$('#otp-verification-form').submit(function(e){
    $("#nextBtn").attr('disabled', 'disabled');
    $("#nextBtn-mobile").attr('disabled', 'disabled');
});

$(document).ready(function () {
    $(document).keypress(function (event) {
        if (event.which === 13 && isCaptured) {
            $("#doSubmit").click();
        }
    });
});