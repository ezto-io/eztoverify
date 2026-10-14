var isValidLength = {};
var isValidRegex = {};
var fields = [];
var isShowingHelp = false;
var iti;
var countryCode;
var resourcesUrl;
var templateTitle;

function setFields(inputFields, country, url, title) {
    fields = inputFields;
    countryCode = country;
    resourcesUrl = url;
    templateTitle = title;
    init();
}

function init() {
    if (fields.length) {
        fields.map((field) => {
            if (field.isRequired) {
                isValidLength[field.fieldName] = field.validation.fieldLength ? false : true;
                isValidRegex[field.fieldName] = field.validation.regex ? false : true;
            } else {
                isValidLength[field.fieldName] = true;
                isValidRegex[field.fieldName] = true;
            }
            if (field.isError) {
                $('#' + field.fieldName).addClass('ciba-template-input-error');
            }
            if (field.fieldType == 'tel') {
                initTelInput(field.fieldName);
            }
        });
    }
}

function checkValidation(fieldName) {
    if (fields.length) {
        fields.map((field) => {
            if (fieldName === field.fieldName) {
                let fieldValue = $('#' + fieldName).val();
                let fieldLength = fieldValue.length;
                if (field.validation.fieldLength) {
                    isValidLength[fieldName] = field.isRequired ? checkFieldLength(field.validation.fieldLength, fieldName) : fieldLength ? checkFieldLength(field.validation.fieldLength, fieldName) : true;
                }
                if (field.validation.regex) {
                    isValidRegex[fieldName] = field.isRequired ? checkRegex(field.validation.regex, fieldValue) : fieldLength ? checkRegex(field.validation.regex, fieldValue) : true;
                }
                if (field.validation.characterType) {
                    checkCharacterType(field.validation.characterType, fieldName);
                }
                if (field.validation.characterCase) {
                    checkCharacterCase(field.validation.characterCase, fieldName);
                }
                if (field.fieldType == 'text' && fieldValue[0] == ' ') {
                    $('#' + fieldName).val($('#' + fieldName).val().trim());
                }
            }
        });
    }
    enableButton();
}

function checkFieldLength(fieldValue, fieldName) {
    const characterCountValidation = fieldValue.split('-');
    const fieldLength = $('#' + fieldName).val().length;
    if (characterCountValidation[0] === 'eq' && fieldLength === parseInt(characterCountValidation[1])) {
        return true;
    } else if (characterCountValidation[0] === 'ge' && fieldLength >= parseInt(characterCountValidation[1])) {
        return true;
    } else if (characterCountValidation[0] === 'gt' && fieldLength > parseInt(characterCountValidation[1])) {
        return true;
    } else if (characterCountValidation[0] === 'le' && fieldLength <= parseInt(characterCountValidation[1])) {
        return true;
    } else if (characterCountValidation[0] === 'lt' && fieldLength < parseInt(characterCountValidation[1])) {
        return true;
    } else {
        return false;
    }
}

function checkRegex(regex, fieldValue) {
    const regexExp = new RegExp(regex);
    if (fieldValue && regexExp.test(fieldValue)) {
        return true;
    } else {
        return false;
    }
}

function checkCharacterType(characterType, fieldName) {
    if (characterType == 'numeric') {
        $('#' + fieldName).val($('#' + fieldName).val().replace(/[^0-9]/g, '').replace(/(\..*)\./g, '$1'));
    } else if (characterType == 'alphanumeric') {
        $('#' + fieldName).val($('#' + fieldName).val().replace(/[^0-9a-zA-Z]/g, '').replace(/(\..*)\./g, '$1'));
    } else if (characterType == 'alphabeta') {
        $('#' + fieldName).val($('#' + fieldName).val().replace(/[^a-zA-Z]/g, '').replace(/(\..*)\./g, '$1'));
    }
}

function checkCharacterCase(characterCase, fieldName) {
    if (characterCase == 'uppercase') {
        $('#' + fieldName).val($('#' + fieldName).val().toUpperCase());
    } else if (characterCase == 'lowercase') {
        $('#' + fieldName).val($('#' + fieldName).val().toLowerCase());
    }
}

function enableButton() {
    $('#template-error').css('display', 'none');
    var isValidPhoneNumber = true;
    if (fields.length) {
        for (let i = 0; i < fields.length; i++) {
            let fieldName = fields[i].fieldName;
            let fieldType = fields[i].fieldType;
            if (fieldType == 'tel') {
                isValidPhoneNumber = phoneNumValidCheck()
            }
            if (!isValidLength[fieldName] || !isValidRegex[fieldName] || !isValidPhoneNumber) {
                $("#nextBtn").attr('disabled', 'disabled');
                break;
            } else {
                if (fields[i].isError) {
                    $('#' + fieldName).removeClass('ciba-template-input-error');
                }
                $("#nextBtn").removeAttr('disabled');
            }
        }
    }
}

function hideHelperToBottom() {
    $('#tenant-logo').css('display', 'none');
    $('#template-helper').css('top', '+150%'); 
}

function showHelperToView() {
    $('#tenant-logo').css('display', 'block');
    $('#template-helper').css('top', '0'); 
}

function showHelper() {
    if (window.innerWidth <= 768 && window.innerHeight <= 1024) {
        if (!isShowingHelp) {
            showHelperToView()
            isShowingHelp = true;
        } else {
            hideHelperToBottom()
            isShowingHelp = false;
        } 
    }else{
        if (!isShowingHelp) {
            $('#template-error').css('display', 'none');
            $('#template-helper').css('display', 'block');
            $('#template-header').css('display', 'none').removeClass('d-flex');
            $('#template-form-fields').css('display', 'none');
            $('#template-button').css('display', 'none').removeClass('d-flex');
            isShowingHelp = true;
        } else {
            $('#template-helper').css('display', 'none');
            $('#template-header').css('display', 'flex').addClass('d-flex');
            $('#template-form-fields').css('display', 'block');
            $('#template-button').css('display', 'flex').addClass('d-flex');
            isShowingHelp = false;
        }
    }
}

function transactionCancel() {
    $("#cancel-flow").val("true");
    $('form').submit();
}

$(document).ready(function () {
    $(document).keypress(function (event) {
        if (event.which === 13) {
            $('#nextBtn').attr('disabled', 'disabled');
            $("#nextBtn").click();
        }
    });
})

function initTelInput(fieldName) {
    const phoneNumberForm = document.querySelector(`#${templateTitle}-form`)
    phoneNumberForm.addEventListener("submit", function (event) {
        $("#phone").val(iti.getNumber());
        $("#countryCode").val("+" + iti.getSelectedCountryData().dialCode)
        $("#phoneNumberRegion").val(iti.getSelectedCountryData().iso2)
        $("#nextBtn").attr('disabled', 'disabled');
        $(`#${templateTitle}-form`).submit();
    });
    var input = document.querySelector("#"+fieldName);
    iti = window.intlTelInput(input, {
        initialCountry: countryCode,
        utilsScript: `${resourcesUrl}/js/tele-input/utils.js`,
        separateDialCode: false
    });
    input.addEventListener('countrychange',function(){
        if(!phoneNumValidCheck()){
            $('#nextBtn').attr('disabled', 'disabled');
        }else{
            enableButton();
        }
    });
    input.focus();
}

function phoneNumValidCheck() {
    if (!iti.isValidNumber()) {
        return false;
    } else {
        return true;
    }
}