$(document).on('ready', function () {
  /*-----------------------------
    Fixed Navigation
  -----------------------------*/
  if ($('header').offset().top > 50) {
    $('body').addClass('fixed-header');
  } else {
    $('body').removeClass('fixed-header');
  }
  /* Scroll Function */
  $(window).on('scroll', function () {
    /* Fixed Navigation */
    if ($('header').offset().top > 50) {
      $('body').addClass('fixed-header');
    } else {
      $('body').removeClass('fixed-header');
    }
  });

  if($("#phoneNumber").val()) {
    checkPhoneNumberValid($("#phoneNumber")[0]);
  }

  /*-----------------------------
    hover Button (letter Effect)
  -----------------------------*/

  document.querySelectorAll('.effect-letter').forEach(button => {

    let div = document.createElement('div'),
      letters = button.textContent.trim().split('');

    function elements(letter, index, array) {

      let element = document.createElement('span'),
        part = (index >= array.length / 2) ? -1 : 1,
        position = (index >= array.length / 2) ? array.length / 2 - index + (array.length / 2 - 1) : index,
        move = position / (array.length / 2),
        rotate = 1 - move;

      element.innerHTML = !letter.trim() ? '&nbsp;' : letter;
      element.style.setProperty('--move', move);
      element.style.setProperty('--rotate', rotate);
      element.style.setProperty('--part', part);

      div.appendChild(element);

    }

    letters.forEach(elements);

    button.innerHTML = div.outerHTML;

    button.addEventListener('mouseenter', e => {
      if (!button.classList.contains('out')) {
        button.classList.add('in');
      }
    });

    button.addEventListener('mouseleave', e => {
      if (button.classList.contains('in')) {
        button.classList.add('out');
        setTimeout(() => button.classList.remove('in', 'out'), 950);
      }
    });

  });

  /*-----------------------------
    Effect on Menu for Mobile
  -----------------------------*/
  document.querySelectorAll('.menu').forEach(btn => {
    btn.addEventListener('click', e => {
      btn.classList.toggle('active');
    });
  });

  /*-----------------------------
    Smooth Scrollspy to (For Navbar)
  -----------------------------*/

  // Add scrollspy to <body>
  // $('body').scrollspy({ target: ".navbar", offset: 50 });

  // Add smooth scrolling on all links inside the navbar
  $("#myNavbar a").on('click', function (event) {
    // Make sure this.hash has a value before overriding default behavior
    if (this.hash !== "") {
      // Prevent default anchor click behavior
      event.preventDefault();

      // Store hash
      var hash = this.hash;

      // Using jQuery's animate() method to add smooth page scroll
      // The optional number (800) specifies the number of milliseconds it takes to scroll to the specified area
      $('html, body').animate({
        scrollTop: $(hash).offset().top
      }, 800, function () {

        // Add hash (#) to URL when done scrolling (default click behavior)
        window.location.hash = hash;
      });
    }  // End if
  });


  /*-----------------------------
    Collapse - icon in Collapse
  -----------------------------*/
  $('.collapse').on('show.bs.collapse', function () {
    $(this).siblings('.card-header').addClass('active');
  });

  $('.collapse').on('hide.bs.collapse', function () {
    $(this).siblings('.card-header').removeClass('active');
  });

  /*-----------------------------
    Dropdown Select for Language
  -----------------------------*/
  $('select[data-menu]').each(function () {

    let select = $(this),
      options = select.find('option'),
      menu = $('<div />').addClass('select-menu'),
      button = $('<div />').addClass('button'),
      list = $('<ul />'),
      arrow = $('<em />').prependTo(button);

    options.each(function (i) {
      let option = $(this);
      list.append($('<li />').text(option.text()));
    });

    menu.css('--t', select.find(':selected').index() * -41 + 'px');

    select.wrap(menu);

    button.append(list).insertAfter(select);

    list.clone().insertAfter(button);

  });

  $(document).on('click', '.select-menu', function (e) {

    let menu = $(this);

    if (!menu.hasClass('open')) {
      menu.addClass('open');
    }

  });

  $(document).on('click', '.select-menu > ul > li', function (e) {

    let li = $(this),
      menu = li.parent().parent(),
      select = menu.children('select'),
      selected = select.find('option:selected'),
      index = li.index();

    menu.css('--t', index * -41 + 'px');
    selected.attr('selected', false);
    select.find('option').eq(index).attr('selected', true);

    menu.addClass(index > selected.index() ? 'tilt-down' : 'tilt-up');

    setTimeout(() => {
      menu.removeClass('open tilt-up tilt-down');
    }, 500);

  });

  $(document).on('click', e => {
    e.stopPropagation();
    if ($('.select-menu').has(e.target).length === 0) {
      $('.select-menu').removeClass('open');
    }
  });

  /*-----------------------------
    Hide and Show Password
  -----------------------------*/
  $("#show_hide_password").on('click', 'a', function (event) {
    event.preventDefault();
    if ($('#show_hide_password input').attr("type") == "text") {
      $('#show_hide_password input').attr('type', 'password');
      $('#show_hide_password .hide_show span').addClass("hidden_outlined");
      $('#show_hide_password .hide_show span').removeClass("visible_outlined");
    } else if ($('#show_hide_password input').attr("type") == "password") {
      $('#show_hide_password input').attr('type', 'text');
      $('#show_hide_password .hide_show span').removeClass("hidden_outlined");
      $('#show_hide_password .hide_show span').addClass("visible_outlined");
    }
  });

  $("#show_hide_password2").on('click', 'a', function (event) {
    event.preventDefault();
    if ($('#show_hide_password2 input').attr("type") == "text") {
      $('#show_hide_password2 input').attr('type', 'password');
      $('#show_hide_password2 .hide_show span').addClass("hidden_outlined");
      $('#show_hide_password2 .hide_show span').removeClass("visible_outlined");
    } else if ($('#show_hide_password2 input').attr("type") == "password") {
      $('#show_hide_password2 input').attr('type', 'text');
      $('#show_hide_password2 .hide_show span').removeClass("hidden_outlined");
      $('#show_hide_password2 .hide_show span').addClass("visible_outlined");
    }
  });


  /*-----------------------------
    loading before open page
  -----------------------------*/
  // Fakes the loading setting a timeout
  setTimeout(function () {
    $('body').addClass('loaded_page');
  }, 3000);

  /*-----------------------------
    Back to top with progress indicator
  -----------------------------*/
  var progressPath = document.querySelector('.prgoress_indicator path');
  var pathLength = progressPath.getTotalLength();
  progressPath.style.transition = progressPath.style.WebkitTransition = 'none';
  progressPath.style.strokeDasharray = pathLength + ' ' + pathLength;
  progressPath.style.strokeDashoffset = pathLength;
  progressPath.getBoundingClientRect();
  progressPath.style.transition = progressPath.style.WebkitTransition = 'stroke-dashoffset 10ms linear';
  var updateProgress = function () {
    var scroll = $(window).scrollTop();
    var height = $(document).height() - $(window).height();
    var progress = pathLength - (scroll * pathLength / height);
    progressPath.style.strokeDashoffset = progress;
  }
  updateProgress();
  $(window).on('scroll', updateProgress);
  var offset = 250;
  var duration = 550;
  $(window).on('scroll', function () {
    if ($(this).scrollTop() > offset) {
      $('.prgoress_indicator').addClass('active-progress');
    } else {
      $('.prgoress_indicator').removeClass('active-progress');
    }
  });
  $('.prgoress_indicator').on('click', function (event) {
    event.preventDefault();
    $('html, body').animate({ scrollTop: 0 }, duration);
    return false;
  });

  /*-----------------------------
    progress-bar
  -----------------------------*/
  var delay = 400;
  $(".progress-bar").each(function (i) {
    $(this).delay(delay * i).animate({
      width: $(this).attr('aria-valuenow') + '%'
    }, delay);
  });


  /*-----------------------------
    Mousehover for dropdown
  -----------------------------*/
  $('.dropdown.dropdown-hover').hover(
    function () {
      $(this).addClass('show')
    },
    function () {
      $(this).removeClass('show')
    });

  $('.dropdown-submenu.dropdown-hover').hover(
    function () {
      $(this).addClass('show')
    },
    function () {
      $(this).removeClass('show')
    });

  /*-----------------------------
  Dropdown menu for mobile
-----------------------------*/
  var coll = document.getElementsByClassName("dropdown_menu");
  var i;

  for (i = 0; i < coll.length; i++) {
    coll[i].addEventListener("click", function () {
      this.classList.toggle("active");
      var content = this.nextElementSibling;
      if (content.style.height) {
        content.style.height = null;
      } else {
        content.style.height = content.scrollHeight + "px";
      }
    });
  };

  // End.

});


/*-----------------------------
  Video Modal
-----------------------------*/
// Gets the video src from the data-src on each button
var $videoSrc;
$('.btn_video').on('click', function () {
  $videoSrc = $(this).data("src");
});

// when the modal is opened autoplay it
$('#mdllVideo').on('shown.bs.modal', function (e) {

  // set the video src to autoplay and not to show related video. Youtube related video is like a box of chocolates... you never know what you're gonna get
  $("#video").attr('src', $videoSrc + "?autoplay=1&amp;modestbranding=1&amp;showinfo=0");
})

// stop playing the youtube video when I close the modal
$('#mdllVideo').on('hide.bs.modal', function (e) {
  // a poor man's stop video
  $("#video").attr('src', $videoSrc);
});

/*-----------------------------
  Checkbox Select
-----------------------------*/
$('.checkbox-item .item-select').on('click', function () {
  $(this).parent().find('.item-select.active').removeClass('active');
  $(this).addClass('active');
});

/*-----------------------------
  fixSide_scroll
-----------------------------*/
// var sticky = new Sticky('.fixSide_scroll');


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