<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang='en'>
<head>

  <title>${msg("loginTitle",(realm.getDisplayName()?cap_first!))}</title>

  <!-- meta data -->
  <meta charset="utf-8">
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="robots" content="noindex, nofollow">
  <meta http-equiv="X-UA-Compatible" content="ie=edge" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <#if properties.meta?has_content>
    <#list properties.meta?split(' ') as meta>
        <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
    </#list>
  </#if>

  <!-- fonts -->
  <link rel="preload stylesheet" as="style" href="/auth/assets/css/normalize.min.css?version=${serverVersion}"/>
  <link rel="preload" href="/auth/assets/fonts/text-font/inter/Inter-ExtraBold.ttf?version=${serverVersion}" as="font" type="font/ttf">
  <link rel="preload" href="/auth/assets/fonts/text-font/inter/Inter-Bold.ttf?version=${serverVersion}" as="font" type="font/ttf">
  
  <!--  Custom Font  -->
  <#if fontPreference?? && fontPreference == "custom">
    <#if customFontPreference?? && customFontPreference == "link">
        <link rel="stylesheet" href="${customFontLink}" >
    </#if>
  </#if>

  <style rel="stylesheet" type="text/css">
    <#if fontPreference?? && fontPreference == "custom">
        <#if customFontPreference?? && customFontPreference == "file">
            @font-face {
              font-family: 'MyCustomFont';
              src: url(${customFontFile});
              font-display: block;
            }
        </#if>
    </#if>

   :root{
       --nav-bar-bg-clr : ${headerBgColor};
       --nav-bar-logo : url( ${tenantLogo} );
       --nav-bar-tenant-name-clr: #3e4b5b;
       --nav-bar-tenant-font-family: Avenir Next W01,Proxima Nova W01,Rubik,-apple-system,system-ui,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;
       --nav-bar-menu-clr: #0b2238;
       --nav-bar-menu-clr-hover: #165df5;
       --nav-bar-btn-border-clr: #5a5f66;
       --nav-bar-btn-txt-clr: #212529;
       --nav-bar-btn-bg-clr-hover: #0b2238;
       --nav-bar-btn-txt-clr-hover: #fff;
       --navbar-logo-height : 50px;

       <#if bodyBgType?? && bodyBgType == "Color">
       --body-bg : ${bodyBgColor};
       --body-bg-sm: ${bodyBgColor};
       </#if>

       <#if bodyBgType?? && bodyBgType == "Image">
        <#if bodyBgImage?? && bodyBgImage != "">
          --body-bg-img : url(${bodyBgImage});
          --body-bg-sm:  url(${bodyBgImage});
          --body-bg-image-opacity : ${bodyBgImageOpacity};
          --body-bg : var(--darkred);
        <#else>
          --body-bg-img : url( '../img/oss/backdrop.svg' );
          --body-bg-sm:  url( '../img/oss/backdrop.svg' );
          --body-bg-image-opacity : ${bodyBgImageOpacity};
          --body-bg : var(--darkred);
        </#if>
      </#if>

      <#if themeLayoutBgType?? && themeLayoutBgType == "Image" >
        --section-bg-image : url(${themeLayoutBgImage});
      </#if>

       --font-family : ${bodyFont}, sans-serif;
       --body-tenant-name-clr : #fff;
       --body-one-liner : #fff;
        <#if fontPreference?? && fontPreference == "custom">
          <#if customFontPreference?? && customFontPreference == "file">
              --font-family :'MyCustomFont', sans-serif;
          </#if>
       </#if>
       <#if themeLayoutTextColor??>
       --body-feature-color : ${themeLayoutTextColor};
       </#if>
       <#if showLogoInForm?? >
        <#assign logoPositionVal = logoPositionValue?split("|")>
          --form-logo-from-top: ${logoPositionVal[0]+logoPositionValueType};
          --form-logo-from-right: ${logoPositionVal[1]+logoPositionValueType};
          --form-logo-from-bottom: ${logoPositionVal[2]+logoPositionValueType};
          --form-logo-from-left: ${logoPositionVal[3]+logoPositionValueType};
        </#if>
       <#if themeLayoutBgColor??>
       --body-img-bg-clr: ${themeLayoutBgColor};
       </#if>

       --form-bg-clr : ${formBgColor};
       --form-border-clr: ${formBorderColor!"#FFFFFF"};
       --form-border-width: ${formBorderWidth!0}px;
       --form-border-radius: ${formBorderRadius!0}px;
       --form-input-border-clr : ${textBorderColor};
       --form-input-active-clr : ${textActiveColor};
       --form-accept-btn-clr : ${buttonBgColor};
       --form-radio-background-clr : ${buttonBgColor+'3b'};
       --form-btn-text-font-clr: ${buttonTextColor};
       --form-btn-border-radius: ${buttonBorderRadius}px;
       --form-input-bg-clr: ${textBoxBgColor!"#ffffff"};
       --form-input-text-clr: ${textBoxTextColor!"#0b2238"};
       --form-text-clr: ${formTextColor!"#0b2238"};
       <#if logoHeight??>
       --form-logo-height : ${logoHeight}px;
       <#else>
       --form-logo-height : 52px;
       </#if>

       --footer-txt-clr : ${footerTextColor};
       --footer-link-txt-clr : #fff;
    }
  </style>

  <!-- favicon -->
  <#if mode?? && mode == 'bo'>
    <link rel="shortcut icon" href="${url.resourcesPath}/img/eztovrfy-favicon.ico?version=${serverVersion}" type="image/x-icon" />
  <#elseif mode?? && (mode == 'wiam' || mode == 'ciam')>
    <link rel="shortcut icon" href="${url.resourcesPath}/img/eztoauth-favicon.ico?version=${serverVersion}" type="image/x-icon" />
  </#if>

  <!-- Include Custom CSS Stylesheet -->
  <#if isRtl?? && isRtl && properties.stylesRtl?has_content>
    <#list properties.stylesRtl?split(' ') as style>
      <link href="/auth/assets/css/${style}?version=${serverVersion}" rel="preload stylesheet" as="style" type="text/css"/>
    </#list>
  <#elseif properties.styles?has_content>
    <#list properties.styles?split(' ') as style>
      <link href="/auth/assets/css/${style}?version=${serverVersion}" rel="preload stylesheet" as="style" type="text/css"/>
    </#list>
  </#if>

  <!-- Include Liveness CSS Stylesheet -->
  <#if isLiveness?? && isLiveness>
    <#if isRtl?? && !isRtl>
      <link rel="preload stylesheet" as="style" href="/auth/assets/css/liveness.min.css?version=${serverVersion}" type="text/css" />
    <#else>
      <link rel="preload stylesheet" as="style" href="/auth/assets/css/liveness-rtl.min.css?version=${serverVersion}" type="text/css" />
    </#if>
  </#if>

  <!-- Scripts -->
  <script src="https://code.jquery.com/jquery-3.7.1.slim.min.js" integrity="sha256-kmHvs0B+OpCW5GVHUNjv9rOmY0IvSIRcf7zGUDTDQM8=" crossorigin="anonymous"></script>
  
  <#if properties.scripts?has_content>
    <#list properties.scripts?split(' ') as script>
      <script defer src="/auth/assets/js/${script}?version=${serverVersion}" type="text/javascript"></script>
    </#list>
  </#if>

  <#if scripts??>
    <#list scripts as script>
        <script defer src="/auth/assets/js/${script}?version=${serverVersion}" type="text/javascript"></script>
    </#list>
  </#if>

  <!-- Headers form templates -->
  <#nested "head">

</head>

<#if themeLayoutBgType?? && (themeLayoutBgType == "Image" || themeLayoutBgType == "Color") && themeLayout != "Center">
   <body dir="${isRtl?string("rtl","ltr")}" class="signup_full body-grid-container show-image z-index-0 ${themeLayout?lower_case} ev-body">
<#else>
  <body dir="${isRtl?string("rtl","ltr")}" class="signup_full body-grid-container ev-body">
</#if>
    <div class="feedback-aligner fadeInclass d-none" id="toast-container">
    </div>
    <section class="form_signup_one signup_two section-prop">
      <div class="container px-0">
      <#--  layout control  -->
        <div class="${themeLayout?lower_case} render-direction">
          <#if themeLayoutBgType?? && themeLayoutBgType == "Text" && themeLayout != "Center">
            <section class="feature-section d-none d-lg-flex" >
              <div class="col-lg-10 d-none d-md-block content-display-control">
                <div class="item_brand margin-b-3">
                  <a href="#">
                    <div class="header-text feature-prop">${realm.displayName}</div>
                  </a>
                </div>
                  <#if themeLayoutOneLine??>
                    <div id="signUp" class="title_sections margin-b-5">
                      <h4 class="one-liner mb-4 feature-prop">${themeLayoutOneLine}</h4>
                    </div>
                  </#if>
                  <#if themeLayoutFeatures??>
                    <div class="list__point">
                    <#list themeLayoutFeatures?split("##") as feature>
                        <div class="item media">
                          <div class="media-body my-auto">
                            <p class="feature-prop" >${feature}</p>
                          </div>
                        </div>
                      </#list>
                    </div>
                  </#if>
              </div>
            </section>
          </#if>
          <#if themeLayoutBgType?? && themeLayoutBgType == "Image" && themeLayout != "Center">
            <#if themeLayoutBgImage??>
              <section class="image-section top-0 d-none d-lg-block" >
                <div class="section-img-src z-index-1 d-none d-lg-flex">
                </div>
               <img src="${themeLayoutBgImage}" class="section-image" alt="bg-image"/>
              </section>
            </#if>
          </#if>
          <#if themeLayoutBgType?? && themeLayoutBgType == "Color" && themeLayout != "Center">
           <section class="image-section top-0 d-none d-lg-block" >
            <div class="d-none d-lg-block image-control z-index-1" >
              <div class="color-content d-flex"> </div>
            </div>
           </section>
          </#if>
          <section class="form-section">
            <div class="form-margin-prop">
              <div>
                <div class="item_group form-prop ev-body">
                      <div class="mb-2 ev-logo">
                          <#if tenantLogo?contains("default-verify-logo")>
                              <div class="d-flex justify-content-between ${logoPosition}"> <img alt="logo" width="50" height="30"  class="form-logo form-logo-parent" src="${url.resourcesPath}/img/eztovrfy-logo.svg?version=${serverVersion}"" /> </div>
                          <#elseif tenantLogo?contains("http")>
                              <div class="d-flex justify-content-between ${logoPosition}"> <img alt="logo" width="50" height="30" class="form-logo form-logo-parent"  src=" ${tenantLogo} " /> </div>
                          <#else>
                            <div class="d-flex justify-content-between ${logoPosition}"> <img alt="logo"  width="50" height="30"  class="form-logo form-logo-parent" src="${url.resourcesPath}/img/logo.svg?version=${serverVersion}"" /> </div>
                          </#if>
                          <#if showLanguageSelector?? && showLanguageSelector=="true">
                              <div class="position-relative">
                                  <select id="localeSelect" class="form-control locale-selector-position  <#if logoPosition=='logo-right'>left-0<#else>right-0</#if>" onchange="onLanguageChange(this)"></select>
                              </div>
                          </#if>
                      </div>
                  <#nested "form">
                  <#if auth?has_content && auth.showTryAnotherWayLink() && showAnotherWayIfPresent>
                    <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                    <div class="social-login-seperator margin-b-2 margin-t-2"> <p class="text-center font-s-13 c-gray">Or</p></div>
                        <div class="${properties.kcFormGroupClass!} text-center mb-0">
                            <input type="hidden" name="tryAnotherWay" value="on"/>
                            <span>Can't go with this method ? </span>
                            <a href="#" id="try-another-way" class="action-link-clr"
                              onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">${msg("doTryAnotherWay")}</a>
                        </div>
                    </form>
                  </#if>
                </div>
              </div>
              <#if showFooter?? && showFooter=="true" && themeLayout != "Center" && (themeLayoutBgType == "Image" || themeLayoutBgType == "Color") && !isLiveness??>
                <div class="item_footer pb-0 form-footer">
                  <div class="container">
                    <div class="row">
                      <#if footerText?? && footerText != "">
                        <p class="text-center footer z-index-1"> ${footerText} </p>
                      <#else> 
                        <p class="text-center footer z-index-1">© <span id="year"></span> <a href="${msg("website-link")}" target="_blank">${msg("qlik-verify-caps")} </a>${msg("made-with")} ♥ by <a href="https://www.grootan.com" target="_blank">${msg("grootan-technologies")}.</a> ${msg("rights-reserved")}</p>
                      </#if>
                    </div>
                  </div>
                </div>
                </#if>
            </div>
          </section>
        </div>
      </div>
    </section>

  <!-- Start item_footer -->
  <#if showFooter?? && showFooter=="true" && !isLiveness??>
  <div class="item_footer pb-0 footer-control">
    <div class="container">
      <div class="row">
        <div class="col-lg-12">
          <#if footerText?? && footerText != "">
            <p class="text-center footer m-0"> ${footerText} </p>
          <#else> 
            <p class="text-center footer m-0">© <span id="year"></span> <a href="${msg("website-link")}" target="_blank">${msg("qlik-verify-caps")} </a>${msg("made-with")} ♥ by <a href="https://www.grootan.com" target="_blank">${msg("grootan-technologies")}.</a> ${msg("rights-reserved")}</p>
          </#if>
        </div>
      </div>
    </div>
  </div>
  </#if>
  <!-- End. item_footer -->


  <!-- Back to top with progress indicator-->
  <div class="prgoress_indicator">
    <svg class="progress-circle svg-content" width="100%" height="100%" viewBox="-1 -1 102 102">
      <path d="M50,1 a49,49 0 0,1 0,98 a49,49 0 0,1 0,-98" />
    </svg>
  </div>
  <script  defer type="text/javascript">
    var mapOptions = {};
    let selectedOption = "";
    $(document).ready(()=>{
      preloadFunc();
    })
    function preloadFunc() {
      <#list supportedLocales as sl>
        mapOptions['${sl}'] = ISO6391.getNativeName('${sl}')
      </#list>

      populateSelect(mapOptions);
    }

    function populateSelect(options) {
      var select = document.getElementById("localeSelect");
      for (let key in mapOptions) {
        var optionElement = document.createElement("option");
        optionElement.text = mapOptions[key];
        optionElement.value = key;
        let localeCookie = getCookie('KEYCLOAK_LOCALE')
        selectedOption = (localeCookie === undefined || localeCookie === null  || localeCookie.length === 0) ? 'en' : localeCookie;
        switchLanguage(mapOptions);
        if(key === selectedOption) {
          optionElement.selected = true;
        }
        if(select !== null) {
            select.appendChild(optionElement);
        }
        setCookie(selectedOption);
      }
    }

      function switchLanguage(mapOptions) {
        if(Object.keys(mapOptions).length === 1) {
            let langShouldLoad = Object.keys(mapOptions)[0];
            setCookie(langShouldLoad);
            if(selectedOption !== langShouldLoad) {
                selectedOption = langShouldLoad;
                window.location.reload();
            }
        }
    }

    function onLanguageChange(element) {
      var lang = element.value;
      setCookie(lang);
      window.location.reload();
    }

    function getCookie(name) {
      const cookies = document.cookie.split(';');
      for (let i = 0; i < cookies.length; i++) {
        const ck = cookies[i].trim();
        if (ck.startsWith(name + "=")) {
          return ck.substring(name.length + 1);
        }
      }
      return null;
    }

    function setCookie(language) {
      if(isSafari()) {
        document.cookie = "KEYCLOAK_LOCALE=" + language + "; path=/; SameSite=None;"
      } else {
        document.cookie = "KEYCLOAK_LOCALE=" + language + "; path=/; SameSite=None; Secure=true"
      }
    }

    function isSafari() {
        return navigator.userAgent.indexOf('Safari') !== -1 && navigator.userAgent.indexOf('Chrome') === -1;
    }

    <#if fontPreference?? && fontPreference == "custom">
      <#if customFontPreference?? && customFontPreference == "link">
        var customFontFileUrl = new URL("${customFontLink}");
        var fontFamilyParam = customFontFileUrl.searchParams.get('family');
        if (fontFamilyParam) {
            var fontFamily = fontFamilyParam.split(':')[0];
            document.documentElement.style.setProperty('--font-family', fontFamily.replace(/\+/g, ' ') + ", sans-serif");
        }
      </#if>
    </#if>

    var selectElement = document.querySelector("#localeSelect");
    if (selectElement) {
      selectElement.addEventListener("change", function () {
        var dataToSend = {
              selectedValue: selectElement.value,
            };
        window.parent.postMessage(dataToSend, "*");
      });
    }

      $( ".show-image .render-direction" ).addClass( "d-block" );
      $( ".left .content-display-control" ).addClass( "mx-auto" );
      $( ".right .content-display-control" ).addClass( "mr-auto" );
      $( ".left.render-direction" ).addClass( "flex-row-reverse" );
      $( ".render-direction" ).addClass( "d-flex" );
      $( ".feature-section" ).addClass( "w-50" );

  </script>

</body>
</html>
</#macro>