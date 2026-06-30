<#import "template.ftl" as layout>
<@layout.registrationLayout; section>  
  <#if section = "form">
    <form id="preferred-login-locale-form" action="${url.loginAction}" method="POST">
      <input name="cancel" type="text" class="d-none" id="cancel" value="false" />
      <input name="selectedLocale" type="text" class="d-none" id="selectedLocale" value="en" />
      <div class="title_sign">
        <h2>${msg("selectPreferredLoginLocale")}</h2>
      </div>
      <div class="form-group">
        <p>${msg("selectPreferredLoginLocale-desc")}<p>
        <select id="mySelect" class="form-control"></select>
      </div>
      <div class="row mt-3">
          <#--  Mobile View  -->
          <div class="col-12 mt-3 display-mobile-only" id="template-button-mobile">
              <div id="cancelBtn-mobile" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</div>
              <button id="nextBtn-mobile" class="w-100 py-3 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
          </div>
          <#--  Web View  -->
          <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
              <div id="empty-skip-fallback"></div>
              <div class="d-flex justify-content-end align-items-center">
                  <button id="cancelBtn" type="button" class="w-100 px-3 py-2 mr-2 ciba-template-cancelbtn text-center cursor-pointer">${msg("doCancel")}</button>
                  <button id="nextBtn" class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn">${msg("submit")}</button>
              </div>
          </div>
      </div>
    </form>
    <script>
    $(document).ready(() => {
      var mapOptions = {};
      window.onload = preloadFunc();
      function preloadFunc() {
        <#list supportedLocales as sl>
          mapOptions['${sl}'] = ISO6391.getNativeName('${sl}')
        </#list>

        populateSelect(mapOptions);
      }

      function populateSelect(options) {
        var select = document.getElementById("mySelect");
        for (let key in mapOptions) {
          var optionElement = document.createElement("option");
          optionElement.text = mapOptions[key];
          optionElement.value = key;

          if(key === 'en') {
            optionElement.selected = true;
          }

          select.appendChild(optionElement);
        }
      }


      $("#mySelect").on("change", ()=> {
        setCookie(document.getElementById('mySelect').value);
        $('#selectedLocale').val(document.getElementById("mySelect").value);
      })

      function isSafari() {
          return navigator.userAgent.indexOf('Safari') !== -1 && navigator.userAgent.indexOf('Chrome') === -1;
      }

      function setCookie(language) {
        if(isSafari()) {
          document.cookie = "KEYCLOAK_LOCALE=" + language + "; path=/; SameSite=None;"
        } else {
          document.cookie = "KEYCLOAK_LOCALE=" + language + "; path=/; SameSite=None; Secure=true"
        }
      }

      $("#cancelBtn").on("click", ()=> {
        setCookie('en');
        $('#cancel').val("true");
        $('form').submit();
      })
      $("#cancelBtn-mobile").on("click", ()=> {
        setCookie('en');
        $('#cancel').val("true");
        $('form').submit();
      })
    });
  </script>
  </#if>
  
</@layout.registrationLayout>
