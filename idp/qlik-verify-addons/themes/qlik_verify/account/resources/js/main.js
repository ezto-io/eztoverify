window.addEventListener("load", (event) => {
    createLocaleSelect();
    var profileDropdown = document.querySelector("#profile-dropdown");
    let enable = false;
    document.addEventListener('click', (event) => {
        const isClickInsideProfile = profileDropdown?.contains(event.target);
        if (profileDropdown && !isClickInsideProfile) {
            profileDropdown.classList.remove("active");
        }
    });
    const timerId = setTimeout(() => {
        hideElement('notificaton-parent');
        clearTimeout(timerId);
    }, 5000)
    var form = document.querySelector('form');
    if(!form) return;
    const required = form.querySelectorAll("[required]");
    form.addEventListener('input', async function () {
        if (required) {
            for (let element of required) {
                if (!element.value) {
                    enable = false;
                    break;
                }
                if(element.id == 'mobileNumber'){
                    element.value = element.value.replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1');
                    if(!iti.isValidNumber()) {
                        enable = false;
                        break;
                    }
                }
                enable = true;
            }
            if (enable) {
                $("button[value='Save']").not("#subscription-submit-button").removeAttr('disabled')
            } else {
                $("button[value='Save']").not('#subscription-submit-button').attr('disabled', true);
            }
        }
        $("button[value='Cancel']").removeAttr('disabled').removeClass('disabled');
    });
});

const handleLangChange = () => {
    updateLangCookie(selectElement.value);
    window.location.reload();
}

const updateLangCookie = (lang) => {
    document.cookie = "KEYCLOAK_LOCALE=" + lang + "; path=/";
}


// Pass ID of the element to hide
const hideElement = (id) => {
    $(`#${id}`).addClass('d-none');
}

const refreshPage = (e) => {
    e.preventDefault();
    window.location.reload();
}

function toggleDropdown() {
    document.querySelector("#profile-dropdown").classList.toggle("active");
}