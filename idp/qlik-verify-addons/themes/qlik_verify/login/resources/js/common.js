/* Disable Button */
function disableButton(element) {
    $(element).addClass("disabled").attr("disabled", true);
}

/* Enable Button */
function enableButton(element) {
    $(element).removeClass("disabled").removeAttr("disabled");
}

/* OCR Submit Button Enable Disable Check */
function ocrSubmitButtonEnableDisableCheck(element) {
    if (element.Front && element.Back) {
        enableButton("#doSubmit");
        disableButton("#doSubmit-mobile");
    } else {
        disableButton("#doSubmit");
    }
}

/* Set value to Input */
function setValue(element) {
    $(element).val("true");
}

$("form").on("submit", function() {
    disableButton("#doSubmit");
    disableButton("#doSubmit-mobile");
    disableButton("#cancelBtn");
});

var isTransactionFlowSkipped = false;
function transactionSkip() {
    if(!isTransactionFlowSkipped){
        isTransactionFlowSkipped = true;
        $("#skip-flow").val("true");
        $('form').submit();
    }
}

