var isPaginationPresent = false;
var isTransactionFlowSkipped = false;
var submitText;
var nextText;
var invalidInputFormat;
var currentPage = 0;
var totalPage = 0;
var page = 1;
let dynamicCancelBtn,
  form,
  requiredInputs,
  requiredMultiInputLabel,
  dateInputs,
  fileInputs,
  hiddenFields,
  allFieldsFilled = true,
  validInputs = [],
  fileObjects = {},
  facingMode = {},
  isCameraOpened = {},
  stream = {};
customButtons = [];
iti = {};

const fields = [];

const initFields = (obj) => {
  fields.push(obj);
};

const setCurrentPage = (page) => {
  currentPage = page;
  hideOtherPage();
  showPage();
};

const showPage = () => {
  const elements = document.querySelectorAll(`[data-page='${currentPage}']`);
  const submitBtn = $("#submitBtn");
  const submitBtnMobile = $("#submitBtn-mobile");
  if (totalPage > currentPage) {
    submitBtn.html(nextText);
    submitBtnMobile.html(nextText);
  } else {
    submitBtn.html(submitText);
    submitBtnMobile.html(submitText);
  }
  elements.forEach((element) => {
    element.classList.remove("d-none");
  });
};

const hideOtherPage = () => {
  const elements = document.querySelectorAll(`[data-page]`);

  elements.forEach((element) => {
    element.classList.add("d-none");
  });
};

const nextPage = () => {
  if (totalPage > currentPage) {
    setCurrentPage(currentPage + 1);
  } else {
    submit();
  }
};

//Actions that should be done on document ready
$(document).ready(function () {
  setCurrentPage(isPaginationPresent ? 1 : 0);

  //Input Fields
  dynamicCancelBtn = document.getElementById("cancel-input");
  form = document.getElementById("json-based-form");
  requiredInputs = form.querySelectorAll("input[required], select[required]");
  requiredMultiInputLabel = document.querySelectorAll(".required-checkbox");
  dateInputs = document.querySelectorAll('input[type="date"]');
  fileInputs = document.querySelectorAll('input[type="file"][accept]');
  hiddenFields = document.querySelectorAll("div[hidden]");
  customButtons = document.querySelectorAll("button[customButton]");

  // FlatPickr library configs
  const dateConfig = {
    allowInput: true,
    dateFormat: "d/m/Y",
    disableMobile: "true",
  };
  dateInputs.forEach((input) => {
    if (!input.readOnly) {
      flatpickr("#" + input.id, {
        ...dateConfig,
        defaultDate: input.defaultValue,
      });
    }
  });

  enableHiddenFields();
  checkRequiredFields();

  // Form event Listener for input types
  form.addEventListener("input", function () {
    enableHiddenFields();
    checkRequiredFields();
  });
});

// Function to remove file on Clicking remove
function removeFile(index, inputFieldID) {
  const fileInput = document.getElementById(inputFieldID);
  const dataTransfer = new DataTransfer();
  Array.from(fileInput.files).forEach((file, i) => {
    if (i !== index) {
      dataTransfer.items.add(file);
    }
  });
  fileInput.files = dataTransfer.files;
  fileInput.dispatchEvent(new Event("change"));
  form.dispatchEvent(new Event("input"));
}

// Function to check regex for fields if regex is present
function checkForRegexValidation() {
  const textInputs = Array.from(form.querySelectorAll("input[pattern]"));
  validInputs = [];

  textInputs.forEach((x) => {
    const pattern = x.getAttribute("pattern");
    const value = x.value;
    const regex = new RegExp(pattern);
    let errorTag = document.getElementById(x.id + "Error");

    if (!errorTag) {
      errorTag = document.createElement("p");
      errorTag.id = x.id + "Error";
      errorTag.className = "input-alert-text-error mb-0";
      x.parentNode.appendChild(errorTag);
    }

    if (value.length > 0 && !regex.test(value)) {
      validInputs.push(false);
      if (errorTag) {
        if (x?.attributes?.regexerror?.value) {
          errorTag.textContent = x?.attributes?.regexerror.value;
        } else {
          errorTag.textContent = "Enter a valid input format.";
        }
      }
    } else {
      validInputs.push(true);
      if (errorTag) {
        errorTag.textContent = ``;
      }
    }
  });
  return validInputs.every((status) => status);
}

// Function to enable hidden fields based on value
function enableHiddenFields() {
  const checkConditions = (field) => {
    const dependantOn = JSON.parse(field.getAttribute("dependOn"));
    const validationType = field.getAttribute("validationTypeForDependsOn");
    let enableHiddenField = false;

    const checkDependsOn = (x) => {
      const dependantField = $(`[name="${x}"]`);
      const dependantFieldType = dependantField.attr("type");
      let dependantFieldValue = "";

      if (dependantFieldType === "radio" || dependantFieldType === "checkbox") {
        dependantFieldValue = $(`input[name="${x}"]:checked`).val();
      } else {
        dependantFieldValue = dependantField.val();
      }

      if (
        dependantFieldValue &&
        dependantOn[x] != "" &&
        dependantFieldValue.length > 0
      ) {
        let dependsOnValue = dependantOn[x].split(",");
        if (dependsOnValue.includes(dependantFieldValue)) {
          return true;
        } else {
          return false;
        }
      } else if (
        dependantFieldValue &&
        dependantOn[x] != "" &&
        Array.isArray(dependantFieldValue) &&
        dependantOn[x] == dependantFieldValue.join()
      ) {
        return true;
      } else if (
        dependantOn[x] == "" &&
        dependantFieldType == "tel" &&
        !phoneNumValidCheck(x)
      ) {
        return false;
      } else if (
        dependantFieldValue &&
        dependantOn[x] == "" &&
        dependantFieldValue.trim().length > 0
      ) {
        return true;
      } else {
        return false;
      }
    };

    if (validationType === "all_true") {
      enableHiddenField = Object.keys(dependantOn).every(checkDependsOn);
    } else {
      enableHiddenField = Object.keys(dependantOn).some(checkDependsOn);
    }
    return enableHiddenField;
  };

  customButtons.forEach((button) => {
    const enableField = checkConditions(button);

    if (enableField) {
      button.removeAttribute("disabled");
    } else {
      button.setAttribute("disabled", "true");
    }
  });

  hiddenFields.forEach((hiddenField) => {
    const enableField = checkConditions(hiddenField);

    if (enableField) {
      hiddenField.removeAttribute("hidden");
    } else {
      hiddenField.setAttribute("hidden", "");
    }
  });
}

// Function for button Action
function buttonAction(fieldName) {
  let validInputField = true;
  fields.forEach((field) => {
    if (field.type == "phonenumber") {
      const dialCode = iti[field.name]?.getSelectedCountryData().dialCode;
      if (dialCode) {
        $(`#${field.name}_dialcode`).val(`+${dialCode}`);
      }
      if (!phoneNumValidCheck(field.name)) {
        $(`#${field.name}-error-txt`).css("display", "flex");
        validInputField = false;
      }
    }
  });
  if (validInputField) {
    fields.forEach((field) => {
      if (field.type == "button") {
        $(`#${field.name}-btn`).attr("disabled", true);
        $(`#${field.name}-btn-mobile`).attr("disabled", true);
      }
    });
    $(`#${fieldName}`).val(true);
    $("form").submit();
  }
}

// Function for cancel button
function cancelForm() {
  dynamicCancelBtn.value = true;
  $("form").submit();
}

// Function for submit button
function submit() {
  let validInputField = true;
  fields.forEach((field) => {
    if (field.type == "phonenumber") {
      const dialCode = iti[field.name]?.getSelectedCountryData().dialCode;
      if (dialCode) {
        $(`#${field.name}_dialcode`).val(`+${dialCode}`);
      }
      if (!phoneNumValidCheck(field.name)) {
        $(`#${field.name}-error-txt`).css("display", "flex");
        validInputField = false;
      }
    }
  });
  if (validInputField) {
    $("#submitBtn").attr("disabled", true);
    $("#submitBtn-mobile").attr("disabled", true);
    $("form").submit();
  }
}

// Function to prevent the value change
function preventChange(event) {
  event.preventDefault();
}

// Function for listen file input
function listenFileInput(fieldId, acceptableFileType, fileFormateError) {
  document.getElementById(`${fieldId}`).addEventListener("change", (event) => {
    document.getElementById(`${fieldId}-selected-files`).innerHTML = "";
    document.getElementById(`${fieldId}-error-txt`).classList.add("d-none");
    let totalFileSize = 0;
    var unAcceptedType = false;
    Array.from(event.target.files).forEach((file, index) => {
      const fileExtension = file.name.split(".").pop().toLowerCase();
      if (
        !acceptableFileType.includes(fileExtension) &&
        fileExtension &&
        !unAcceptedType
      ) {
        unAcceptedType = true;
        return;
      }
      let listItem = document.createElement("li");
      let spanItem = document.createElement("span");
      spanItem.classList.add("json-form-label");
      spanItem.textContent = file.name;
      listItem.appendChild(spanItem);
      let removeButton = document.createElement("span");
      removeButton.textContent = "Remove";
      removeButton.classList.add("remove-btn");
      removeButton.addEventListener("click", () => {
        removeFile(index, `${fieldId}`);
      });
      listItem.appendChild(removeButton);
      document
        .getElementById(`${fieldId}-selected-files`)
        .appendChild(listItem);
      totalFileSize = totalFileSize + file.size;
    });
    var totalSizeInMB = (totalFileSize / (1024 * 1024)).toFixed(2);
    if (totalSizeInMB > 20 || unAcceptedType) {
      if (unAcceptedType) {
        document.getElementById(`${fieldId}-error-txt`).textContent =
          fileFormateError;
      }
      document.getElementById(`${fieldId}-selected-files`).innerHTML = "";
      document.getElementById(`${fieldId}`).value = "";
      document
        .getElementById(`${fieldId}-error-txt`)
        .classList.remove("d-none");
      if (document.getElementById(`${fieldId}`).required) {
        $("#submitBtn").attr("disabled", true);
        $("#submitBtn-mobile").attr("disabled", true);
      }
    }
  });
}

// Function to load camera/capture elements
function loadCaptureElements(
  fieldName,
  isMobile,
  includeMetadata,
  cameraFacingMode
) {
  isCameraOpened[fieldName] = false;
  stream[fieldName] = null;
  facingMode[fieldName] = cameraFacingMode == "front" ? "user" : "environment";

  if (isMobile && facingMode[fieldName] == "environment") {
    $(`#${fieldName}-video`).css("transform", "scaleX(1)");
    $(`#${fieldName}-imagePreview`).css("transform", "scaleX(1)");
  }

  $(`#${fieldName}-video`).on("loadedmetadata", function () {
    const video = this;
    const canvas = $(`#${fieldName}-canvas`)[0];
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
  });

  $(`#${fieldName}-switch-camera-btn`).on("click", function () {
    if (facingMode[fieldName] == "environment") facingMode[fieldName] = "user";
    else facingMode[fieldName] = "environment";

    if (isMobile && facingMode[fieldName] == "environment") {
      $(`#${fieldName}-video`).css("transform", "scaleX(1)");
      $(`#${fieldName}-imagePreview`).css("transform", "scaleX(1)");
    } else {
      $(`#${fieldName}-video`).css("transform", "scaleX(-1)");
      $(`#${fieldName}-imagePreview`).css("transform", "scaleX(-1)");
    }
    isCameraOpened[fieldName] = false;
    if (stream[fieldName] != null) {
      stopTracksAndClearStream(stream, fieldName)
        .then((message) => {
          captureImage(fieldName, includeMetadata);
        })
        .catch((error) => {
          console.error("Error stopping tracks:", error);
        });
    }
  });
}

// Stop camera stream
function stopTracksAndClearStream(stream, fieldName) {
  return new Promise((resolve, reject) => {
    try {
      if (stream[fieldName]) {
        stream[fieldName].getTracks().forEach((track) => {
          track.stop();
        });
        stream[fieldName] = null;
      }
      resolve("Tracks stopped and stream cleared.");
    } catch (error) {
      reject(error);
    }
  });
}

// Function to capture the image
function captureImage(fieldName, includeMetadata) {
  if (!isCameraOpened[fieldName]) {
    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: facingMode[fieldName] } })
      .then((mediaStream) => {
        stream[fieldName] = mediaStream;
        stream[fieldName].getVideoTracks().forEach((track) => {
          track.addEventListener("ended", () => {
            stream[fieldName].getTracks().forEach((track) => track.stop());
            isCameraOpened[fieldName] = false;
            showRetry(fieldName);
          });
        });
        const $video = $(`#${fieldName}-video`);
        $video[0].srcObject = stream[fieldName];
        $video[0].play();
        $video.show();
        $(`#${fieldName}-placeholder-image`).hide();
        $(`#${fieldName}-imagePreview`).hide();
        $(`#${fieldName}-retake-selfie-wrapper`).hide();
        $(`#${fieldName}-camera-error`).hide();
        $(`#${fieldName}-capture-text`).hide();
        $(`#${fieldName}-capture-image`).show();
        isCameraOpened[fieldName] = true;
        $(`#${fieldName}-switch-camera-wrapper`).show();
        $(`#${fieldName}`).val("");
        enableHiddenFields();
        checkRequiredFields();
      })
      .catch((err) => {
        showRetry(fieldName);
      });
  } else {
    const canvas = $(`#${fieldName}-canvas`)[0];
    const context = canvas.getContext("2d");
    const $video = $(`#${fieldName}-video`);
    context.drawImage($video[0], 0, 0);
    let base64Image = canvas.toDataURL("image/jpeg", 1);
    let val;
    if (!includeMetadata) {
      val = base64Image.replace(/^data:image\/jpeg;base64,/i, "");
    } else {
      val = base64Image;
    }
    $(`#${fieldName}`).val(val);
    const $imagePreview = $(`#${fieldName}-imagePreview`);
    $imagePreview.attr("src", base64Image);
    $imagePreview.show();
    $(`#${fieldName}-retake-selfie-wrapper`).show();
    $(`#${fieldName}-capture-image`).hide();
    $video.hide();
    isCameraOpened[fieldName] = false;
    stream[fieldName].getTracks().forEach((track) => track.stop());
    $(`#${fieldName}-switch-camera-wrapper`).hide();
    enableHiddenFields();
    checkRequiredFields();
  }
}

const checkRequiredFields = () => {
  checkForRegexValidation();
  let allFieldsFilled = true;

  fields
    .filter((input) => input.page == currentPage)
    .map((input) => {
      const type = input.type;
      if (input.isRequired) {
        var value = "";
        switch (type) {
          case "text":
            value = $(`input[name=${input.name}]`).val().trim();
            break;
          case "option":
            value = $(`select[name=${input.name}]`).val();
            break;
          case "date":
          case "capture":
            value = $(`input[name=${input.name}]`).val();
            break;
          case "checkbox":
          case "radio":
            value = $(`input[name="${input.name}"]:checked`).val();
            break;
          case "file":
            value =
              document.querySelector(`input[name=${input.name}]`).files.length >
              0;
            break;
          case "textarea":
            value = $(`textarea[name=${input.name}]`).val();
            break;
          case "phonenumber":
            value = $(`input[name=${input.name}]`).val().trim();
            if (!phoneNumValidCheck(input.name) || !value) {
              allFieldsFilled = false;
            }
            break;
        }
        if (
          allFieldsFilled &&
          (value ||
            document
              .querySelector(`[data-name=${input.name}]`)
              .hasAttribute("hidden"))
        ) {
          allFieldsFilled = true;
        } else {
          allFieldsFilled = false;
        }
      }
    });

  $("#submitBtn").attr(
    "disabled",
    !allFieldsFilled || !checkForRegexValidation()
  );
  $("#submitBtn-mobile").attr(
    "disabled",
    !allFieldsFilled || !checkForRegexValidation()
  );
};

const showRetry = (fieldName) => {
  $(`#${fieldName}-camera-error`).show();
  $(`#${fieldName}-capture-text`).hide();
  $(`#${fieldName}-capture-image`).hide();
  $(`#${fieldName}-placeholder-image`).hide();
  $(`#${fieldName}-retake-selfie-wrapper`).hide();
  $(`#${fieldName}-imagePreview`).hide();
  $(`#${fieldName}-video`).hide();
  $(`#${fieldName}-switch-camera-wrapper`).hide();
};

// Initialize the telephone input field with intlTelInput
function initTelInput(fieldName, resourcesUrl) {
  var input = document.querySelector(`#${fieldName}`);
  iti[fieldName] = window.intlTelInput(input, {
    initialCountry: "in",
    utilsScript: `${resourcesUrl}/js/tele-input/utils.js`,
    separateDialCode: false,
  });
  input.addEventListener("countrychange", function () {
    enableHiddenFields();
    checkRequiredFields();
  });
}

// Validate phone number
function phoneNumValidCheck(fieldName) {
  if (
    iti[fieldName] &&
    iti?.phone?.getNumber() &&
    !iti[fieldName].isValidNumber()
  ) {
    return false;
  }
  return true;
}

// Allow only numeric characters in the input field
function allowOnlyNumbers(fieldName) {
  $(`#${fieldName}-error-txt`).css("display", "none");
  const inputValue = $(`#${fieldName}`)
    .val()
    .replace(/[^0-9]/g, "");
  $(`#${fieldName}`).val(inputValue);
}

const transactionSkip = () => {
  if (!isTransactionFlowSkipped) {
    isTransactionFlowSkipped = true;
    $("#skip-flow").val("true");
    $("form").submit();
  }
};
