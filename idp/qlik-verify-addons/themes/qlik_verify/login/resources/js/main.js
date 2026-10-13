/* EztoVerify login theme: scripts shared by the pages (loaded on every page, theme.properties scripts). */

/*
 * Password fields with an eye icon: a click shows the typed password, the next click hides it again. `scope` holds
 * the password input(s) and the icon (.hide_show span: class hidden_outlined while hidden, visible_outlined while
 * shown). Only fields that are currently of type password or text are switched. `focusSelector`, when given, is
 * focused afterwards.
 */
function switchPasswordVisibility(scope, focusSelector) {
  var fields = $(scope + ' input');
  var current = fields.attr('type');
  if (current === 'password' || current === 'text') {
    var reveal = current === 'password';
    fields.attr('type', reveal ? 'text' : 'password');
    $(scope + ' .hide_show span').toggleClass('visible_outlined', reveal).toggleClass('hidden_outlined', !reveal);
  }
  if (focusSelector) {
    $(focusSelector).focus();
  }
}

// the names the pages call: onclick="togglePasswordView()" / onclick="toggleConfirmPasswordView()"
function togglePasswordView() {
  switchPasswordVisibility('#show_hide_password', '#inputPassword');
}

function toggleConfirmPasswordView() {
  switchPasswordVisibility('#show_hide_password2', '#inputPassword');
}

//getting full year 
$( document ).ready(function() {
  $("#year").text(new Date().getFullYear());
});

//starts the timer using expiry time
function otpExpiryCounter(expiry){
  var countDownDate = expiry;
  var x = setInterval(function() {
    var count = "";
    var now = new Date().getTime();
    var distance = countDownDate - now;
    var days = Math.floor(distance / (1000 * 60 * 60 * 24));
    var hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    var minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
    var seconds = Math.floor((distance % (1000 * 60)) / 1000);
    
    if( days ){
        count += days + "d "; 
    }
    if( hours ){
        count += hours + "h "; 
    }
    if( minutes ){
        count += minutes + "m "; 
    }
    if( seconds ){
        count += seconds + "s "; 
    }
    document.getElementById("expirytime").innerHTML = count;

    if (distance < 1) {
    clearInterval(x);
    $('#be-error').hide();
    $('#otplive').hide();
    $('#otpExpired').show();
    $('#template-error').show();
    $('#nextBtn').addClass('disabled cursor-unset');
    $('#nextBtn').attr('disabled', 'disabled');
    }
}, 1000);
}

function getRemainingTime(expiry){
    var time = new Date(expiry).toLocaleTimeString("en-US", {hour: '2-digit', minute:'2-digit'});
    document.getElementById("expirytime").innerHTML = time;
}

function disableSubmitBtn(element1, element2){
  $("#be-error").remove();
  let elementValue1 = $(element1).val();
  let elementValue2 = $(element2).val();
  if (!elementValue1 || !elementValue2) {
      $('#nextBtn').addClass('disabled cursor-unset');
      $('#nextBtn').attr('disabled', 'disabled');
  } else {
      $('#nextBtn').removeClass('disabled cursor-unset');
      $('#nextBtn').removeAttr('disabled');
  }
}

function disableBtn(){
  $("#be-error").remove();
  let conf = $("#inputOTP").val();
  if (!conf) {
      $('#nextBtn').addClass('disabled cursor-unset');
      $('#nextBtn').attr('disabled', 'disabled');
  } else {
      $('#nextBtn').removeClass('disabled cursor-unset');
      $('#nextBtn').removeAttr('disabled');
  }
}

function disableOTPSubmitBtn(expiry){
  var countDownDate = expiry;
  var now = new Date().getTime();
  var distance = countDownDate - now;
  $("#be-error").remove();
  let confOTP = $("#inputOTP").val();
  let emailOTP = $("#inputEmailOTP").val();
  let mobileOTP = $("#inputMobileOTP").val();
  if ((!(emailOTP && mobileOTP) && !confOTP) || (distance < 0) ) {
      $("#nextBtn-mobile").addClass('disabled cursor-unset');
      $("#nextBtn-mobile").attr('disabled', 'disabled');
      $('#nextBtn').addClass('disabled cursor-unset');
      $('#nextBtn').attr('disabled', 'disabled');
  } else {
      $('#nextBtn').removeClass('disabled cursor-unset');
      $('#nextBtn').removeAttr('disabled');
      $("#nextBtn-mobile").removeClass('disabled cursor-unset');
      $("#nextBtn-mobile").removeAttr('disabled');
  }
}

function disableOTPSubmitBtnIncludingOtpLength(expiry, otpLength){
  var countDownDate = expiry;
  var now = new Date().getTime();
  var distance = countDownDate - now;
  $("#be-error").remove();
  let confOTPLength = $("#inputOTP").val().length;
  if ((confOTPLength != otpLength) || (distance < 0) ) {
      $("#nextBtn-mobile").addClass('disabled cursor-unset');
      $("#nextBtn-mobile").attr('disabled', 'disabled');
      $('#nextBtn').addClass('disabled cursor-unset');
      $('#nextBtn').attr('disabled', 'disabled');
  } else {
      $('#nextBtn').removeClass('disabled cursor-unset');
      $('#nextBtn').removeAttr('disabled');
      $("#nextBtn-mobile").removeClass('disabled cursor-unset');
      $("#nextBtn-mobile").removeAttr('disabled');
  }
}

// INFO: Input value are trimmed for the below types in the variable 'elementTypes'
$( document ).ready(function() { 
  const formElement = document.querySelector("form");
  formElement.addEventListener('submit', function(event) {
    truncateSpace(event,formElement);
  });
});
function truncateSpace(event,formElement) {
  event.preventDefault();
  let elementList = event.target.querySelectorAll('input');
  elementList.forEach( (element, index) => {
    elementTypes = ["text","password","number","email","search","tel","url"];
    if(elementTypes.includes(element.type)) {
      element.value = element.value.trim();
    }
  })
  new FormData(formElement);
  formElement.addEventListener('formdata', (e) => {
    event.submitter.attributes.name ? e.formData.append( event.submitter.attributes.name.value, event.submitter.attributes.value.value ) : '';
  });
  event.target.submit();
}
function checkPhoneNumberValid(inputvalue) {
  inputvalue.value = inputvalue.value.replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1');
  if(!iti.isValidNumber()) {
    $('#nextBtn').attr('disabled', 'disabled');
    $('#nextBtn').addClass('disabled');
    $('#nextBtn-mobile').attr('disabled', 'disabled');
    $('#nextBtn-mobile').addClass('disabled');
  }else {
    $('#nextBtn').removeClass('disabled');
    $('#nextBtn').removeAttr('disabled');
    $('#nextBtn-mobile').removeClass('disabled');
    $('#nextBtn-mobile').removeAttr('disabled');
  }
}

// Callback function to execute when mutations are observed
const callback = (mutationList, observer) => {
  for (const mutation of mutationList) {
    if (mutation.type === 'attributes') {
      if(mutation.target.classList.contains("iti__selected-flag")) {
        removeErrorByElementName()
      }
    }
  }
};

// Make input accepts only digits.
const preventText = (input)=>{
  input.value = input.value.replace(/[^0-9]/g, '');
}

// Observer to watch the change in html attr or childList...
const observer = new MutationObserver(callback);

//  document onload 
$(document).ready(function() {
  // auto-focusing input field
  $(".initial-focus").focus();
  $(document).keypress(function(event) {
      // Check if the Enter key is pressed (key code 13)
      if (event.which === 13) {
          // blur all input field before form submittion
          $('input').each(function() {
            $(this).blur();
          });
          // Trigger the click event of the button
          $("#nextBtn").click();
      }
  });
});