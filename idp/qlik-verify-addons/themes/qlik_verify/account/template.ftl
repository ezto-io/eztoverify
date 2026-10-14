<#macro mainLayout active bodyClass>
<!DOCTYPE html>
<html lang="${user_preferred_lang}" <#if user_preferred_lang == 'ar'> dir="rtl" </#if>  >
    <head>
        <meta charset="utf-8">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="robots" content="noindex, nofollow">

        <title>${msg('userProfileTitle',realm.getDisplayName()?cap_first!)}</title>
        <#if realm.getMode()?? && realm.getMode() == 'bo'>
        <link rel="icon" href="${url.resourcesPath}/img/eztovrfy-favicon.ico">
        <#elseif realm.getMode()?? && (realm.getMode() == 'wiam' || realm.getMode() == 'ciam')>
        <link rel="icon" href="${url.resourcesPath}/img/eztoauth-favicon.ico">
        </#if>

        <link rel="stylesheet" href="${url.resourcesPath}/css/bootstrap.min.css">
        <script type="text/javascript" src="${url.resourcesPath}/js/jquery.min.js"></script>
        <!-- Phone Number Library CSS -->
        <link rel="stylesheet" href="${url.resourcesPath}/js/tele-input/tele-input.min.css" type="text/css"/>
        <#if user_preferred_lang == 'ar'> 
            <link href="${url.resourcesPath}/css/account-rtl.css" rel="stylesheet" />
        <#else>
            <link href="${url.resourcesPath}/css/account.css" rel="stylesheet" />
        </#if>a
        <#--  <!-- Phone Number Library Scripts  -->
        <script src="${url.resourcesPath}/js/tele-input/tele-input.min.js" type="text/javascript"></script>
        <script src="${url.resourcesPath}/js/tele-input/utils.js" type="text/javascript"></script>
        <script src="https://cdn.jsdelivr.net/npm/iso-639-1@3.1.2/build/index.min.js"></script>
        <#if properties.stylesCommon?has_content>
            <#list properties.stylesCommon?split(' ') as style>
                <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
            </#list>
        </#if>
        <#if properties.styles?has_content>
            <#list properties.styles?split(' ') as style>
                <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
            </#list>
        </#if>
        <#if properties.scripts?has_content>
            <#list properties.scripts?split(' ') as script>
                <script type="text/javascript" src="${url.resourcesPath}/${script}"></script>
            </#list>
        <style>
        </#if>
        <#if realm.getMode()?? && realm.getMode() == 'wiam'>
            .fs-name{
              color: #4CA355;
            }
            .top-bar .logged-user-w .logged-user-menu {
              background: #4CA355;
            }
            .avatar-w .profile-icon {
              background-color: #4CA355;
            }
            .logged-user-menu .profile-icon {
              background-color: #fff;
            }
            .menu-w.sub-menu-style-over ul.main-menu > li.active > a {
              background-color: #4CA355;
            }
            .menu-w.sub-menu-style-over ul.main-menu > li.active {
               border-right-color: #4CA355;
               border-bottom-color: #4CA355;
              }
            .form-control:focus {
               border-color: #4CA355;
               box-shadow: 0 0 0 0.2rem rgba(76, 163, 85, 0.1);
            }
            .btn-primary.disabled, .all-wrapper .disabled.fc-button.fc-state-active,
            .btn-primary:disabled, .all-wrapper .fc-button.fc-state-active:disabled,
            .btn-primary.disabled, .all-wrapper .disabled.fc-button.fc-state-active,
            .btn-primary:disabled, .all-wrapper .fc-button.fc-state-active:disabled,
            .btn-primary:not(:disabled):not(.disabled):active, .all-wrapper .fc-button.fc-state-active:not(:disabled):not(.disabled):active,
            .btn-primary:not(:disabled):not(.disabled).active, .all-wrapper .fc-button.fc-state-active:not(:disabled):not(.disabled).active,
            .show > .btn-primary.dropdown-toggle, .all-wrapper .show > .dropdown-toggle.fc-button.fc-state-active,
            .btn-primary:hover, .all-wrapper .fc-button.fc-state-active:hover,
            .btn-primary, .all-wrapper .fc-button.fc-state-active{
               border-color: #4CA355;
               background-color: #4CA355;
            }
            .btn-primary:focus, .all-wrapper .fc-button.fc-state-active:focus,
            .btn-primary.focus, .all-wrapper .focus.fc-button.fc-state-active,
            .btn-primary:not(:disabled):not(.disabled):active:focus,
              .all-wrapper .fc-button.fc-state-active:not(:disabled):not(.disabled):active:focus,
            .btn-primary:not(:disabled):not(.disabled).active:focus,
              .all-wrapper .fc-button.fc-state-active:not(:disabled):not(.disabled).active:focus,
            .show > .btn-primary.dropdown-toggle:focus, .all-wrapper .show > .dropdown-toggle.fc-button.fc-state-active:focus
            {
               box-shadow: 0 0 0 0.2rem rgba(76, 163, 85, 0.1);
            }
            .element-wrapper .element-header:after {
             background-color: #4CA355;
            }
        <#elseif realm.getMode()?? && realm.getMode() == 'ciam'>
            .fs-name{
                color: #323232;
            }
            .top-bar .logged-user-w .logged-user-menu {
                background: #323232;
            }
            .avatar-w .profile-icon {
                background-color: #323232;
            }
            .logged-user-menu .profile-icon {
                background-color: #fff;
            }
            .menu-w.sub-menu-style-over ul.main-menu > li.active > a {
                background-color: #323232;
            }
            .menu-w.sub-menu-style-over ul.main-menu > li.active {
                border-right-color: #323232;
                border-bottom-color: #323232;
            }
            .form-control:focus {
                border-color: #323232;
                box-shadow: 0 0 0 0.2rem rgba(76, 163, 85, 0.1);
            }
            .btn-primary.disabled, .all-wrapper .disabled.fc-button.fc-state-active,
            .btn-primary:disabled, .all-wrapper .fc-button.fc-state-active:disabled,
            .btn-primary.disabled, .all-wrapper .disabled.fc-button.fc-state-active,
            .btn-primary:disabled, .all-wrapper .fc-button.fc-state-active:disabled,
            .btn-primary:not(:disabled):not(.disabled):active, .all-wrapper .fc-button.fc-state-active:not(:disabled):not(.disabled):active,
            .btn-primary:not(:disabled):not(.disabled).active, .all-wrapper .fc-button.fc-state-active:not(:disabled):not(.disabled).active,
            .show > .btn-primary.dropdown-toggle, .all-wrapper .show > .dropdown-toggle.fc-button.fc-state-active,
            .btn-primary:hover, .all-wrapper .fc-button.fc-state-active:hover,
            .btn-primary, .all-wrapper .fc-button.fc-state-active{
                 border-color: #323232;
                 background-color: #323232;
            }
            .btn-primary:focus, .all-wrapper .fc-button.fc-state-active:focus,
            .btn-primary.focus, .all-wrapper .focus.fc-button.fc-state-active,
            .btn-primary:not(:disabled):not(.disabled):active:focus,
                .all-wrapper .fc-button.fc-state-active:not(:disabled):not(.disabled):active:focus,
            .btn-primary:not(:disabled):not(.disabled).active:focus,
                .all-wrapper .fc-button.fc-state-active:not(:disabled):not(.disabled).active:focus,
            .show > .btn-primary.dropdown-toggle:focus, .all-wrapper .show > .dropdown-toggle.fc-button.fc-state-active:focus
            {
                 box-shadow: 0 0 0 0.2rem rgba(76, 163, 85, 0.1);
            }
            .element-wrapper .element-header:after {
                  background-color: #323232;
            }
        </#if>
        </style>
    </head>
    <body class="menu-position-side menu-side-left full-screen with-content-panel" id="body-container"> 
        <div class="qv-nav-bar top-bar color-scheme-transparent bg-white">
            <div class="logo-w bg-white">
                <a class="logo custom-values-given d-flex align-items-center realm-logo-container">
                    <div class="realm-logo-box"> 
                        <#if realm.getRealmLogo()?? && realm.getRealmLogo()!='' && (realm.getRealmLogo()?starts_with('data:image') || realm.getRealmLogo()?starts_with('http'))>
                                <img src="${realm.getRealmLogo()}" alt="tenant logo image"/>
                        <#elseif realm.getRealmLogo()?? && realm.getRealmLogo()!=''>
                                <img src="data:image/png;base64,${realm.getRealmLogo()}" alt="tenant logo image"/>
                        <#else>
                        <#if realm.getMode()?? && realm.getMode() == 'bo'>
                                <img src="${url.resourcesPath}/img/eztovrfy-logo.svg"  alt="tenant logo image"/>
                                <#elseif realm.getMode()?? && (realm.getMode() == 'wiam' || realm.getMode() == 'ciam')>
                                <img src="${url.resourcesPath}/img/eztoauth-logo.svg"  alt="tenant logo image"/>
                                </#if>
                        </#if> 
                    </div>
                    <span class="text-capitalize fs-name"> ${realm.getDisplayName()} </span>
                </a>
            </div>
            <div class="top-menu-controls pr-1 logged-user-w">  
                <#if showLanguageSelector?? && showLanguageSelector=="true">
                     <select onchange="handleLangChange()" class="px-2 form-control mr-3" id="locale-select"></select> 
                </#if>
                <div id="profile-dropdown" class="logged-user-i">
                    <div class="avatar-w" onclick="toggleDropdown()">
                         <#if account.getUserPicture()?? && account.getUserPicture() != ''>
                            <img height="30px" width="30px" class="profile" src="${account.getUserPicture()}" alt="user_img"/>
                        <#else>
                            <div class="profile-icon d-flex align-center justify-center"> ${account.getUsername()[0]?upper_case} </div>
                        </#if>
                    </div>
                    <div class="logged-user-menu color-style-bright border-top-radius-none">
                        <div class="logged-user-avatar-info d-flex align-items-center justify-content-start cursor-auto">
                            <div class="avatar-w <#if user_preferred_lang != 'ar'>mr-3</#if> dropdown-avatar-w bg-white">
                            <#if account.getUserPicture()?? && account.getUserPicture() != ''>
                                <img height="30px" width="30px" class="profile" src="${account.getUserPicture()}" alt="user_img"/>
                            <#else>
                                <div class="profile-icon d-flex align-center justify-center"> ${account.getUsername()[0]?upper_case} </div>
                            </#if>
                            </div> 
                            <div class="logged-user-info-w">
                                <div class="logged-user-name user-info-overflow overflow-ellipsis">
                                    ${account.getUsername()} 
                                </div>
                            </div>
                        </div>
                        <ul>
                            <li>
                                <a href="${url.logoutUrl}">
                                    <i class="os-icon os-icon-signs-11 <#if user_preferred_lang == 'ar'>rotate-180</#if>"></i>
                                    <span>${msg('doSignOut')}</span>
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div> 

        <!-- Main Layout  -->
        <div class="all-wrapper with-side-panel solid-bg-all">
            <div class="layout-w"> 
                <!-- SideBar goes here --> 
                <div class="kc-menu">
                    <div class="menu-w sub-menu-style-over">
                        <ul class="main-menu pt-0 user-select-none">
                            <li class="<#if active=='account'>active</#if>">
                                <a href="${url.accountUrl}">
                                    <div class="icon-w">
                                        <div class="os-icon os-icon-user"></div>
                                    </div>
                                    <span> ${msg("userProfile")}</span>
                                </a>
                                <div class="side-menu-title">
                                    <p>${msg("userProfile")}</p>
                                </div>
                            </li>
                            <#if features.passwordUpdateSupported>
                                <li class="<#if active=='password'>active</#if>">
                                    <a href="${url.passwordUrl}">
                                        <div class="icon-w">
                                            <div class="os-icon os-icon-eye"></div>
                                        </div>
                                        <span> ${msg("password")}</span>
                                    </a>
                                    <div class="side-menu-title">
                                        <p>${msg("password")}</p>
                                    </div>
                                </li>
                            </#if>
                            <li class="<#if active=='sessions'>active</#if>">
                                <a href="${url.sessionsUrl}">
                                    <div class="icon-w">
                                        <div class="os-icon os-icon-clock"></div>
                                    </div>
                                    <span>${msg("sessions")}</span>
                                </a>
                                <div class="side-menu-title">
                                    <p>${msg("sessions")}</p>
                                </div>
                            </li>
                            <li class="<#if active=='applications'>active</#if>">
                                <a href="${url.applicationsUrl}">
                                    <div class="icon-w">
                                        <div class="os-icon os-icon-package"></div>
                                    </div>
                                    <span>${msg("applications")}</span>
                                </a>
                                <div class="side-menu-title">
                                    <p>${msg("applications")}</p>
                                </div>
                            </li> 
                            <li class="<#if active=='mfa'>active</#if>">
                                <a href="${url.mfaUrl}">
                                    <div class="icon-w">
                                        <div class="os-icon os-icon-fingerprint"></div>
                                    </div>
                                    <span>${msg("mfa")}</span>
                                </a>
                                <div class="side-menu-title">
                                    <p>${msg("mfa")}</p>
                                </div>
                            </li> 
                             <#if features.log>
                                <li class="<#if active=='log'>active</#if>">
                                    <a href="${url.logUrl}">
                                        <div class="icon-w">
                                            <div class="os-icon os-icon-check-circle"></div>
                                        </div>
                                        <span> ${msg("log")}</span>
                                    </a>
                                    <div class="side-menu-title">
                                        <p>${msg("log")}</p>
                                    </div>
                                </li>
                            </#if>
                        </ul>
                    </div>
                </div>

                <!-- Main Content goes here -->
                <div class="content-w">
                    <div class="content-i">
                        <div class="content-box">
                            <div class="row">
                                <div class="col-sm-12"> 
                                    <#nested "content"> 
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>   
        </div> 
        <#if message?has_content>
            <div class="feedback-aligner fadeInclass" id="notificaton-parent">
                <div class="alert alert-${message.type} alert-dismissable pr-4">
                    <button type="button" class="close" onClick="hideElement('notificaton-parent')" id="notification-close">
                        <span class="notification-close-icon os-icon os-icon-x"/>
                    </button>
                    <div class="icon-notifications">
                        <#if message.type = "success"> <span class="os-icon os-icon-check-circle"></span> </#if>
                        <#if message.type = "info"> <span class="os-icon os-icon-alert-circle"></span> </#if>
                        <#if message.type = "warning"> <span class="os-icon os-icon-alert-triangle"></span> </#if>
                        <#if message.type = "error"> <span class="notification-icon picons-thin-icon-thin-0153_delete_exit_remove_close"></span> </#if>
                    </div>
                    <strong>${kcSanitize(msg('${message.type}'))?no_esc}</strong>
                    ${kcSanitize(message.summary)?no_esc}
                </div>
            </div>
        </#if>
    </body>
    <script>
    const selectElement = document.getElementById("locale-select");
    const createLocaleSelect = () => {
        // The selector is only rendered when showLanguageSelector is on.
        if (!selectElement) return;
        let option;
        <#list locales as locale>
            option = document.createElement('option');
            option.value = '${locale}';
            if ('${user_preferred_lang}' === '${locale}')
                option.selected = true;
            option.innerText = ISO6391.getNativeName('${locale}');
            selectElement.appendChild(option);
        </#list> 
    }
    </script>
</html>
</#macro>