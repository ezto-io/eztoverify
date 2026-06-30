<#macro registrationLayout active bodyClass>
<!DOCTYPE html>
<html>
    <head>
        <title>${msg('ssoManagementTitle',realm.getDisplayName()?cap_first!)}</title>

        <meta charset="utf-8">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="robots" content="noindex, nofollow">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <#if realm.getMode()?? && (realm.getMode() == 'wiam' || realm.getMode == 'ciam')>
        <link rel="icon" href="${url.resourcesPath}/img/eztoauth-favicon.ico">
        <#elseif realm.getMode()?? && (realm.getMode() == 'bo')>
        <link rel="icon" href="${url.resourcesPath}/img/eztovrfy-favicon.ico">
        </#if>

        <link rel="stylesheet" href="${url.resourcesPath}/css/bootstrap.min.css">
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
        </#if>
        <style>
         <#if realm.getMode() ?? && realm.getMode() == 'wiam'>
             .avatar-w .profile-icon {
                 background-color: #4CA355;
             }
             .logged-user-menu .profile-icon{
                 background-color: #fff;
             }
             .info-card:hover .email-title{
                 color: #4CA355;
             }
             .fs-name{
                 color: #4CA355;
             }
             .top-bar .logged-user-w .logged-user-menu {
                 background: #4CA355;
             }
         <#elseif realm.getMode() ?? && realm.getMode() == 'ciam'>
            .avatar-w .profile-icon {
                 background-color: #323232;
            }
            .logged-user-menu .profile-icon{
                 background-color: #fff;
            }
            .info-card:hover .email-title{
                 color: #323232;
            }
            .fs-name{
                 color: #323232;
            }
            .top-bar .logged-user-w .logged-user-menu {
                 background: #323232;
            }
         </#if>
        </style>
        <script type="text/javascript" src='${url.resourcesPath}/js/jquery.min.js'></script>
    </head>
    <body
        class="menu-position-side menu-side-left full-screen with-content-panel" id="body-container">
         <input type="hidden" id="isExtensionInstalled" value="false" />
          <div class="qv-nav-bar top-bar color-scheme-transparent bg-white pr-0 d-block">
            <div class="d-flex top-bar-container mx-auto top-bar">
                <div class="logo-w bg-white d-flex">
                    <a class="logo custom-values-given d-flex align-items-center realm-logo-container">
                        <div class="realm-logo-box">

                            <#if realm.getRealmLogo()?? && realm.getRealmLogo()!='' && (realm.getRealmLogo()?starts_with('data:image') || 
                             realm.getRealmLogo()?starts_with('http')) >
                                    <img src="${realm.getRealmLogo()}" alt="tenant logo image"/>
                            <#elseif realm.getRealmLogo()?? && realm.getRealmLogo()!=''>
                                    <img src="data:image/png;base64,${realm.getRealmLogo()}" alt="tenant logo image"/>
                            <#else>
                            <#if realm.getMode()?? && (realm.getMode() == 'bo')>
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
                    <#if ssoproviders.desktopAppInstalled>
                        <div class="top-menu-controls pr-4">   
                            <button id="install_sso_app" onclick="showModal()" class="btn btn-primary install-sso-app-btn btn-sm p-2">${msg("install-sso-apps")}</button>
                        </div>
                    </#if>   
                    <div id="profile-dropdown" class="logged-user-i">
                        <div class="avatar-w" onclick="toggleDropdown()" id="qv-avatar-icon">
                            <#if ssouser.getUserPicture()?? && ssouser.getUserPicture() != ''>
                                <img height="30px" width="30px" class="profile" src="${ssouser.getUserPicture()}" alt="user_img"/>
                            <#else>
                                <div class="profile-icon d-flex align-center justify-center">${ssouser.getUsername()[0]?upper_case} </div>
                            </#if>
                        </div>
                        <div class="logged-user-menu color-style-bright border-top-radius-none">
                            <a class="border-0 bg-none" id="user-profile-login" style="text-decoration:none !important;" id="qv-profile-option" onclick="window.open(profileUrl, '_blank')">
                                <div class="logged-user-avatar-info d-flex align-items-center justify-content-start cursor-pointer">
                                    <div class="avatar-w mr-3 dropdown-avatar-w bg-white">
                                    <#if ssouser.getUserPicture()?? && ssouser.getUserPicture() != ''>
                                        <img height="30px" width="30px" class="profile" src="${ssouser.getUserPicture()}" alt="user_img"/>
                                    <#else>
                                        <div class="profile-icon d-flex align-center justify-center"> ${ssouser.getUsername()[0]?upper_case} </div>
                                    </#if>
                                    </div>
                                        <div class="logged-user-info-w">
                                            <div class="logged-user-name user-info-overflow overflow-ellipsis">
                                                Profile
                                            </div>
                                        </div>
                                </div>
                            </a>
                            <ul>
                                <li>
                                    <a href="${url.logoutUrl}" id="qv-signOutBtn">
                                        <i class="os-icon os-icon-signs-11"></i>
                                        <span>${msg('doSignOut')}</span>
                                    </a>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div> 
        <div class="all-wrapper with-side-panel solid-bg-all">
           
            <div class="layout-w">
                 <div class="content-w ml-0 mx-auto">
                    <div class="content-i h-100">
                        <div class="content-box h-100">
                            <div class="row h-100">
                                <div class="col-sm-12 h-100">
                                    <#nested "content"> 
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <#if ssoproviders.desktopAppInstalled>
            <div class="modal-backdrop fade d-none z-sm" modal-backdrop=""></div>
            <div tabindex="-1" role="dialog" class="modal fade ng-isolate-scope in z-lg" modal-window="" index="0" animate="animate">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content" ng-transclude=""><div class="modal-header ">
                    <h5 class="modal-title d-inline-flex">
                        <span id="prev-button">
                            <i class="os-icon os-icon-arrow-left6 font-weight-normal cursor-pointer"></i> ${msg("to-txt")}
                        </span> ${msg("install-sso-apps")}
                    </h5>
                    <button type="button" class="close" id="modal-closeBtn" onclick="hideModal()">
                        <span class="os-icon os-icon-close"></span>
                    </button>
                </div>
                <div class="modal-body ">
                    <div id="hidden-text" class="d-none">${ssoproviders.appDetails.workspace}</div>
                    <div id='application-download'>
                        <div class="row py-4">
                            <div class="col-xl-1 col-auto">
                                <img src="${url.resourcesPath}/img/downloads/apple.svg" class="download-section-img">
                            </div>
                            <div class="col-lg-6 col-xl-8 col-auto d-flex align-items-center">
                                <div class="d-flex align-items-center">
                                    <h6 class="download-section-title ng-binding mb-0" data-ng-if="apps.title">${msg("mac")}</h6>
                                </div>
                            </div>
                            <div class="col-lg-4 col-xl-3 col-12 ml-lg-0 mt-lg-0 d-flex align-items-center justify-content-end justify-content-lg-start mt-2">
                                <a id="qv-macStore-downloadBtn" class="btn btn-outline-success download-btn ng-binding" href='${ssoproviders.appDetails.desktop_app_link_mac}' target="_blank">
                                    ${msg("mac-store")}
                                </a>
                            </div>
                        </div>
                        <div class="row py-4">
                            <div class="col-xl-1 col-auto">
                                <img  class="download-section-img" src="${url.resourcesPath}/img/downloads/windows.svg">
                            </div>
                            <div class="col-lg-6 col-xl-8 col-auto d-flex align-items-center">
                                <div class="d-flex align-items-center">
                                    <h6 class="download-section-title ng-binding mb-0" data-ng-if="apps.title">${msg("windows")}</h6>
                                </div>
                            </div>
                            <div class="col-lg-4 col-xl-3 col-12 ml-lg-0 mt-lg-0 d-flex align-items-center justify-content-end justify-content-lg-start mt-2">
                                <a id="qv-microsoftStore-downloadBtn" class="btn btn-outline-success download-btn" href='${ssoproviders.appDetails.desktop_app_link_win}' target="_blank">
                                    ${msg("microsoft-store")}
                                </a>
                            </div>
                        </div>
                        <div class="row py-4">
                            <div class="col-xl-1 col-auto">
                                <img  class="download-section-img" src="${url.resourcesPath}/img/downloads/app-store.svg">
                            </div>
                            <div class="col-lg-6 col-xl-8 col-auto d-flex align-items-center">
                                <div class="d-flex align-items-center">
                                    <h6 class="download-section-title ng-binding mb-0" data-ng-if="apps.title">${msg("ios")}</h6>
                                </div>
                            </div>
                            <div class="col-lg-4 col-xl-3 col-12 ml-lg-0 mt-lg-0 d-flex align-items-center justify-content-end justify-content-lg-start mt-2">
                                <a id="qv-appStore-downloadBtn" class="btn btn-outline-success download-btn" href='${ssoproviders.appDetails.app_store_link}' target="_blank">
                                    ${msg("appstore")}
                                </a>
                            </div>
                        </div>
                        <div class="row py-4">
                            <div class="col-xl-1 col-auto">
                                <img  class="download-section-img" src="${url.resourcesPath}/img/downloads/play-store.svg">
                            </div>
                            <div class="col-lg-6 col-xl-8 col-auto d-flex align-items-center">
                                <div class="d-flex align-items-center">
                                    <h6 class="download-section-title ng-binding mb-0" data-ng-if="apps.title">${msg("android")}</h6>
                                </div>
                            </div>
                            <div class="col-lg-4 col-xl-3 col-12 ml-lg-0 mt-lg-0 d-flex align-items-center justify-content-end justify-content-lg-start mt-2">
                                <a id="qv-playStore-downloadBtn" class="btn btn-outline-success download-btn" href='${ssoproviders.appDetails.play_store_link}' target="_blank">
                                    ${msg("playstore")}
                                </a>
                            </div>
                        </div>
                    </div>
                    <div id="scan-qr">
                        <div class="qr-container mb-2 mx-auto">
                            <img alt="qr-code" src="data:image/png;base64, ${ssoproviders.appDetails.qr}">
                        </div>
                        <p class="text-center mb-1">${msg("scan-qr-to-login")}</p>
                        <p class="text-center mb-1">${msg("or")}</p>
                        <p class="text-center mb-2">${msg("enter-given-details")}</p>
                        <div class="d-flex flex-column align-items-center">
                            <div class="d-flex flex-column gap-sm workspace-detail">
                                <div>
                                    <span class="text-small">${msg("workspace-name")}</span>
                                    <p class="mb-0 font-weight-bold text-success text-lg" id="workspace-name"></p>
                                </div>
                                <div>
                                    <span class="text-small">${msg("workspace-url")}</span>
                                    <p class="mb-0 font-weight-bold text-success text-lg" id="workspace-url"></p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button id="scan-qr-button" type="button" class="btn btn-primary install-sso-app-btn" onclick="scanQR()">${msg("scan-qr")}</button>
                </div>
                </div> 
                </div>
            </div>
            <script>
                
                function showModal(){
                    $('#body-container').addClass('modal-open');
                    $('.modal-backdrop').removeClass('d-none').addClass('in');
                    $('.modal').addClass('d-block')
                }
                function hideModal(){
                    $('.modal').removeClass('d-block');
                    $('.modal-backdrop').addClass('d-none').removeClass('in');
                    $('#body-container').removeClass('modal-open');
                }
                function clickPrevious(){
                    $('#prev-button').hide();
                    $('#scan-qr-button').show();
                    $('#scan-qr').hide();
                    $('.modal-dialog').addClass('modal-lg');
                    $('#application-download').show();
                }
                function scanQR(){
                    $('#application-download').hide();
                    $('.modal-dialog').removeClass('modal-lg');
                    $('#scan-qr').show();
                    $('#scan-qr-button').hide();
                    $('#prev-button').show();
                }
                $(document).ready(function(){
                    $('#scan-qr').hide();
                    $('#prev-button').hide();
                    if($('#prev-button')){
                        $('#prev-button i').click(clickPrevious)
                    }
                    let workspaceDetails = JSON.parse($('#hidden-text').text());
                    $('#workspace-name').text(workspaceDetails.workspaceName);
                    $('#workspace-url').text(workspaceDetails.workspaceUrl);
                })
            </script>
        </#if>
</body>  
</html>
</#macro>
