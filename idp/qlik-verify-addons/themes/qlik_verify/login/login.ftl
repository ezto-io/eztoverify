<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && registrationEnabled && !registrationDisabled??; section>
    <#if section = "form">
    <div>
        <div class="title_sign">
          <h2>${msg("doLogIn")}</h2>
        </div>
    </div>
    <div id="kc-form">
      <div id="kc-form-wrapper">
            <#if realm.password>
                <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                    <div class="form-group">
                        <label for="username"><#if !realm.loginWithEmailAllowed>${msg("username")} <span class="text-danger"> *</span> <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")} <span class="text-danger"> *</span> <#else>${msg("email")} <span class="text-danger"> *</span> </#if></label>
                        <#if usernameEditDisabled??>
                            <input oninput="disableSubmit()" tabindex="1" id="username" class="form-control" placeholder="<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>" name="username" value="${(login.username!'')}" type="text" disabled  />
                        <#else>
                            <input oninput="disableSubmit()" tabindex="1"  id="username" class="form-control" placeholder="<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>" name="username" value="${(login.username!'')}" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off"
                                aria-invalid="<#if messagesPerField.existsError('username','password')>${msg("true")}</#if>"
                            />
                        </#if>
                    </div>

                    <div class="form-group --password" id="show_hide_password">
                        <label for="password">${msg("password")} <span class="text-danger"> *</span> </label>
                        <div class="input-group">
                            <input oninput="disableSubmit()" tabindex="2" id="password" class="form-control" data-toggle="password" placeholder="${msg("password")}"
                                required="" name="password" type="password" autocomplete="new-password"
                                    aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
                            
                            <div class="input-group-prepend hide_show cursor-pointer" onclick="togglePasswordView()">
                                <span class="input-group-text tio hidden_outlined"></span>
                            </div>
                        </div>
                        <#if messagesPerField.existsError('username','password')>
                                <div class="d-flex align-items-center">
                                    <img src="/auth/assets/img/error-icon.svg?version=${serverVersion}" alt="i" class="my-2" />
                                    <div class="mx-2 text-danger font-s-14">
                                        ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                                    </div>
                                </div>
                            </#if>
                    </div>

                    <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!} mb-0">
                        <div id="kc-form-options" class="d-flex">
                            <#if rememberMeEnabled && !usernameEditDisabled??>
                                <div>
                                    <label>
                                        <#if login.rememberMe??>
                                            <input tabindex="3" class="remember-checkbox" id="rememberMe" name="rememberMe" type="checkbox" checked><span>${msg("rememberMe")}</span>
                                        <#else>
                                            <input tabindex="3" class="remember-checkbox" id="rememberMe" name="rememberMe" type="checkbox"><span>${msg("rememberMe")}</span>
                                        </#if>
                                    </label>
                                </div>
                            </#if>
                            
                            <div class="${properties.kcFormOptionsWrapperClass!} ml-auto">
                                <#if resetPasswordEnabled>
                                    <span><a tabindex="5" class="font-s-13 action-link-clr" id="forgotPassword" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                                </#if>
                            </div>
                        </div>
            
                        <div id="kc-form-buttons">
                            <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                            <button tabindex="4" class="btn w-100 margin-t-3 btn_account bg-orange-red  " name="login" id="kc-login" type="submit" value="${msg("doLogIn")}" disabled="disabled">
                            ${msg("doLogIn")}
                            </button>
                        </div>

                        <#if realm.password && registrationEnabled && !registrationDisabled??>
                            <#import "create-account-link.ftl" as c>
                            <@c.create_account_link />
                        </#if>

                        <#if realm.password && social.providers??>
                            <div id="kc-social-providers" class="col-12 text-center p-0">
                                <div class="social-login-seperator margin-my-2">
                                    <p class="text-center font-s-13 c-gray">${msg("or-continue-with")}</p>
                                </div>
                                <div class="google_sign margin-t-2 social-connections-container">
                                    <#list social.providers as p>
                                        <#if p.alias == msg("facebook") >
                                        <a href="${p.loginUrl}" id="facebook" class="btn-social color-facebook rounded-16">
                                            <i class="tio facebook"></i>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("microsoft") >
                                        <a href="${p.loginUrl}" id="microsoft" class="btn-social color-microsoft rounded-16">
                                            <svg width="20" height="20" xmlns="http://www.w3.org/2000/svg"  viewBox="0 0 48 48" width="48px" height="48px"><path fill="#ff5722" d="M6 6H22V22H6z" transform="rotate(-180 14 14)"/><path fill="#4caf50" d="M26 6H42V22H26z" transform="rotate(-180 34 14)"/><path fill="#ffc107" d="M26 26H42V42H26z" transform="rotate(-180 34 34)"/><path fill="#03a9f4" d="M6 26H22V42H6z" transform="rotate(-180 14 34)"/></svg>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("linkedin") >
                                        <a href="${p.loginUrl}" id="linkedin" class="btn-social color-linkedin rounded-16">
                                            <i class="tio linkedin"></i>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("twitter") >
                                        <a href="${p.loginUrl}" id="twitter" class="btn-social color-twitter rounded-16">
                                            <i class="tio twitter"></i>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("github") >
                                        <a href="${p.loginUrl}" id="github" class="btn-social color-github rounded-16">
                                            <i class="tio github"></i>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("stackoverflow") >
                                        <a href="${p.loginUrl}" id="stackoverflow" class="btn-social color-stack-overflow rounded-16">
                                            <svg width="20" height="20" xmlns="http://www.w3.org/2000/svg"  viewBox="0 0 48 48" width="48px" height="48px"><path fill="#607D8B" d="M9 28H12V42H9z"/><path fill="#607D8B" d="M9 39H35V42H9z"/><path fill="#607D8B" d="M32 28H35V42H32zM15 34H29V37H15z"/><path fill="#A68A6E" d="M14.88 29H28.880000000000003V32H14.88z" transform="rotate(6.142 21.88 30.5)"/><path fill="#EF6C00" d="M29.452 11.646H43.451V14.647H29.452z" transform="rotate(81.234 36.453 13.148)"/><path fill="#FF9800" d="M23.576 13.578H37.574V16.579H23.576z" transform="rotate(60.79 30.576 15.079)"/><path fill="#D38B28" d="M18.395 18.275H32.393V21.276H18.395z" transform="rotate(34.765 25.396 19.777)"/><path fill="#C09553" d="M15.977 23.499H29.976V26.5H15.977z" transform="rotate(19.785 22.978 25.003)"/></svg>
                                        </a>
                                        </#if>
                                         <#if p.alias == msg("paypal") >
                                        <a href="${p.loginUrl}" id="paypal" class="btn-social color-paypal rounded-16">
                                            <i class="tio paypal"></i>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("gitlab") >
                                        <a href="${p.loginUrl}" id="gitlab" class="btn-social color-gitlab rounded-16">
                                            <svg width="20" height="20" xmlns="http://www.w3.org/2000/svg"  viewBox="0 0 48 48" width="48px" height="48px"><path fill="#e53935" d="M24 43L16 20 32 20z"/><path fill="#ff7043" d="M24 43L42 20 32 20z"/><path fill="#e53935" d="M37 5L42 20 32 20z"/><path fill="#ffa726" d="M24 43L42 20 45 28z"/><path fill="#ff7043" d="M24 43L6 20 16 20z"/><path fill="#e53935" d="M11 5L6 20 16 20z"/><path fill="#ffa726" d="M24 43L6 20 3 28z"/></svg>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("instagram") >
                                        <a href="${p.loginUrl}" id="instagram" class="btn-social color-instagram rounded-16">
                                            <i class="tio instagram"></i>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("bitbucket") >
                                        <a href="${p.loginUrl}" id="bitbucket" class="btn-social color-bitbucket rounded-16">
                                            <svg width="20" height="20" version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="8.4 14.39 2481.29 2231.21" style="enable-background:new 8.4 14.39 2481.29 2231.21" xml:space="preserve"><style type="text/css">
                                            <![CDATA[.st00{fill:#2684FF;}
                                            .st11{fill:url(#SVGID_1_);}
                                            .st22{fill:none;}]]>
                                            </style><g><path class="bb-poly" d="M989.97,1493.09h518.05l125.04-730.04H852.22L989.97,1493.09z"/><path class="st00" d="M88.92,14.4C45.02,13.83,8.97,48.96,8.41,92.86c-0.06,4.61,0.28,9.22,1.02,13.77l337.48,2048.72 c8.68,51.75,53.26,89.8,105.74,90.24h1619.03c39.38,0.5,73.19-27.9,79.49-66.78l337.49-2071.78c7.03-43.34-22.41-84.17-65.75-91.2 c-4.55-0.74-9.15-1.08-13.76-1.02L88.92,14.4z M1509.99,1495.09H993.24l-139.92-731h781.89L1509.99,1495.09z"/><linearGradient id="SVGID_1_" gradientUnits="userSpaceOnUse" x1="945.1094" y1="1524.8389" x2="944.4923" y2="1524.1893" gradientTransform="matrix(1996.6343 0 0 -1480.3047 -1884485.625 2258195)"><stop offset="0.18" style="stop-color:#0052CC"/><stop offset="1" style="stop-color:#2684FF"/></linearGradient><path class="st11" d="M2379.27,763.06h-745.5l-125.12,730.42H992.31l-609.67,723.67c19.32,16.71,43.96,26,69.5,26.21h1618.13 c39.35,0.51,73.14-27.88,79.44-66.72L2379.27,763.06z"/></g></svg>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("google") >
                                        <a href="${p.loginUrl}" id="google" class="btn-social color-google rounded-16">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 25 25">
                                            <g id="Group_6390" data-name="Group 6390" transform="translate(-595 -504)">
                                                <circle id="Ellipse_227" data-name="Ellipse 227" cx="12.5" cy="12.5" r="12.5" transform="translate(595 504)" fill="#fff"></circle>
                                                <g id="brands-and-logotypes" transform="translate(598.096 507.096)">
                                                    <path id="Path_1609" data-name="Path 1609" d="M4.344,105.854a4.893,4.893,0,0,1,.693-2.515v-3.124H1.912a9.272,9.272,0,0,0,0,11.279H5.036v-3.124A4.893,4.893,0,0,1,4.344,105.854Z" transform="translate(0 -96.587)" fill="#fbbd00"></path>
                                                    <path id="Path_1610" data-name="Path 1610" d="M198.172,373.65,196,375.822l2.172,2.172a9.2,9.2,0,0,0,5.639-1.912v-3.121H200.69A4.922,4.922,0,0,1,198.172,373.65Z" transform="translate(-188.905 -359.46)" fill="#0f9d58"></path>
                                                    <path id="Path_1611" data-name="Path 1611" d="M55.947,325.477,52.823,328.6a9.415,9.415,0,0,0,.8.913,9.207,9.207,0,0,0,6.553,2.714v-4.344A4.925,4.925,0,0,1,55.947,325.477Z" transform="translate(-50.911 -313.695)" fill="#31aa52"></path>
                                                    <path id="Path_1612" data-name="Path 1612" d="M265.267,199.448a9.351,9.351,0,0,0-.152-1.679l-.082-.445H256v4.344h4.4a4.9,4.9,0,0,1-1.878,2.014l3.121,3.121a9.417,9.417,0,0,0,.913-.8A9.206,9.206,0,0,0,265.267,199.448Z" transform="translate(-246.733 -190.181)" fill="#3c79e6"></path>
                                                    <path id="Path_1613" data-name="Path 1613" d="M201.653,5.786l.384.384L205.109,3.1l-.384-.384A9.206,9.206,0,0,0,198.172,0L196,2.172l2.172,2.172A4.891,4.891,0,0,1,201.653,5.786Z" transform="translate(-188.905 0)" fill="#cf2d48"></path>
                                                    <path id="Path_1614" data-name="Path 1614" d="M60.177,4.344V0a9.207,9.207,0,0,0-6.553,2.714,9.406,9.406,0,0,0-.8.913l3.124,3.124A4.925,4.925,0,0,1,60.177,4.344Z" transform="translate(-50.91 0)" fill="#eb4132"></path>
                                                </g>
                                            </g>
                                            </svg>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("openshift-v4") >
                                        <a href="${p.loginUrl}" id="openshift-v4" class="btn-social color-openshift rounded-16">
                                        <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="48" height="48" version="1.1" id="layer" viewBox="86 120 50 400">
                                            <style type="text/css">
                                                .st0{fill:#AA1017;}
                                                .st1{fill:#CE0E19;}
                                                .st2{fill:#8B070F;}
                                                .st3{fill:#050000;}
                                            </style>
                                            <path class="st0" d="M69.4,313.9l-27,9.8c0.4,4.3,1.1,8.6,2.1,12.8l25.6-9.3C69.3,322.8,69.1,318.4,69.4,313.9"/>
                                            <path class="st0" d="M188.6,284.1c-1.9-3.9-4.1-7.7-6.6-11.2l-27,9.8c3.1,3.2,5.8,6.8,7.9,10.7L188.6,284.1z"/>
                                            <path class="st1" d="M129.3,276.4c5.4,2.6,10.4,6.1,14.6,10.4l27-9.8c-24.7-34.7-72.9-42.7-107.6-18c-22.1,15.7-34.2,41.9-32.1,68.9  l26.9-9.9c2-27.5,26-48.2,53.6-46.2C117.8,272.3,123.7,273.9,129.3,276.4"/>
                                            <path class="st1" d="M46.2,335.9l-25.6,9.3c2.4,9.4,6.5,18.2,12,26.1l26.9-9.8C52.7,354.5,48.1,345.6,46.2,335.9"/>
                                            <path class="st1" d="M158.1,325.8c-0.4,6-2,11.9-4.5,17.3c-11.7,25.1-41.5,35.9-66.6,24.3c-5.5-2.6-10.4-6.1-14.6-10.4l-27,9.9  c7.7,10.8,18,19.4,30,25c38.6,18,84.5,1.3,102.6-37.3c5.6-12,8-25.3,6.9-38.5L158.1,325.8z"/>
                                            <path class="st1" d="M164.7,292.8l-25.6,9.3c4.8,8.6,7,18.4,6.3,28.2l26.9-9.8C171.5,310.9,169,301.5,164.7,292.8"/>
                                            <path class="st2" d="M69.3,319.4c-0.1-1.8,0-3.6,0.1-5.4l-27,9.8c0.1,1.7,0.4,3.4,0.6,5.1L69.3,319.4z"/>
                                            <path class="st2" d="M184.7,277c-0.9-1.4-1.7-2.7-2.7-4.1l-27,9.8c1.2,1.2,2.3,2.5,3.3,3.8L184.7,277z"/>
                                            <path class="st0" d="M45.4,366.8c2.1,2.9,4.4,5.7,6.9,8.3l29.3-10.7c-3.4-2.1-6.5-4.6-9.3-7.4L45.4,366.8z M184.9,316l-26.9,9.8  c-0.3,3.9-1.1,7.8-2.3,11.5l29.3-10.7C185.2,323,185.2,319.4,184.9,316"/>
                                        </svg>

                                        </a>
                                        </#if>
                                        <#if p.alias == msg("openshift-v3") >
                                        <a href="${p.loginUrl}" id="openshift-v3" class="btn-social color-openshift rounded-16">
                                        <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="48" height="48" version="1.1" id="layer" viewBox="86 120 50 400">
                                            <style type="text/css">
                                                .st0{fill:#AA1017;}
                                                .st1{fill:#CE0E19;}
                                                .st2{fill:#8B070F;}
                                                .st3{fill:#050000;}
                                            </style>
                                            <path class="st0" d="M69.4,313.9l-27,9.8c0.4,4.3,1.1,8.6,2.1,12.8l25.6-9.3C69.3,322.8,69.1,318.4,69.4,313.9"/>
                                            <path class="st0" d="M188.6,284.1c-1.9-3.9-4.1-7.7-6.6-11.2l-27,9.8c3.1,3.2,5.8,6.8,7.9,10.7L188.6,284.1z"/>
                                            <path class="st1" d="M129.3,276.4c5.4,2.6,10.4,6.1,14.6,10.4l27-9.8c-24.7-34.7-72.9-42.7-107.6-18c-22.1,15.7-34.2,41.9-32.1,68.9  l26.9-9.9c2-27.5,26-48.2,53.6-46.2C117.8,272.3,123.7,273.9,129.3,276.4"/>
                                            <path class="st1" d="M46.2,335.9l-25.6,9.3c2.4,9.4,6.5,18.2,12,26.1l26.9-9.8C52.7,354.5,48.1,345.6,46.2,335.9"/>
                                            <path class="st1" d="M158.1,325.8c-0.4,6-2,11.9-4.5,17.3c-11.7,25.1-41.5,35.9-66.6,24.3c-5.5-2.6-10.4-6.1-14.6-10.4l-27,9.9  c7.7,10.8,18,19.4,30,25c38.6,18,84.5,1.3,102.6-37.3c5.6-12,8-25.3,6.9-38.5L158.1,325.8z"/>
                                            <path class="st1" d="M164.7,292.8l-25.6,9.3c4.8,8.6,7,18.4,6.3,28.2l26.9-9.8C171.5,310.9,169,301.5,164.7,292.8"/>
                                            <path class="st2" d="M69.3,319.4c-0.1-1.8,0-3.6,0.1-5.4l-27,9.8c0.1,1.7,0.4,3.4,0.6,5.1L69.3,319.4z"/>
                                            <path class="st2" d="M184.7,277c-0.9-1.4-1.7-2.7-2.7-4.1l-27,9.8c1.2,1.2,2.3,2.5,3.3,3.8L184.7,277z"/>
                                            <path class="st0" d="M45.4,366.8c2.1,2.9,4.4,5.7,6.9,8.3l29.3-10.7c-3.4-2.1-6.5-4.6-9.3-7.4L45.4,366.8z M184.9,316l-26.9,9.8  c-0.3,3.9-1.1,7.8-2.3,11.5l29.3-10.7C185.2,323,185.2,319.4,184.9,316"/>
                                        </svg>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("duo-mfa") >
                                        <a href="${p.loginUrl}" id="duo-mfa" class="btn-social color-duo-mfa rounded-16">
                                                <svg id="layer" x="0px" y="0px" viewBox="0 0 652 652" style="enable-background:new 0 0 652 652;">
                                                <style type="text/css">
	                                                .st0{opacity:0.85;}
	                                                .sty1{fill:#6DBE50;}
                                                </style>
                                                <g transform="matrix(0.788663, 0, 0, 1.022424, 70.196426, 4.669339)">
                                                    <g class="st0">
                                                        <g>
                                                        <path class="sty1" d="M214.3,325.7H20.9v99.2h96.8C169.5,424.9,211.8,381,214.3,325.7"/>
                                                        </g>
                                                    </g>
                                                    <path class="sty1" d="M117.7,216.1H20.9v99.2h193.5C211.8,260,169.5,216.1,117.7,216.1"/>
                                                    <path class="sty1" d="M524.2,216.1c-51.8,0-94.2,44-96.7,99.2h193.3C618.4,260,576,216.1,524.2,216.1"/>
                                                    <g class="st0">
                                                        <g>
                                                        <path class="sty1" d="M620.9,325.7H427.5c2.5,55.3,44.8,99.2,96.7,99.2C576,424.9,618.4,381,620.9,325.7"/>
                                                        </g>
                                                    </g>
                                                    <path class="sty1" d="M224.1,216.1v104.4c0,55.9,40.7,101.6,92,104.3V216.1H224.1z"/>
                                                        <g class="st0">
                                                            <g>
                                                                <polygon class="sty1" points="417.7,216.1 325.8,216.1 325.8,424.9 417.7,424.9 417.7,320.5 &#9;&#9;&#9;"/>
                                                            </g>
                                                        </g>
                                                        </g>
                                                </svg>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("oidc")>
                                         <a href="${p.loginUrl}" id="oidc" class="btn-social color-openID rounded-16">
                                            <svg width="48px" height="48px" viewBox="0 0 500 500">
                                            <g transform="matrix(4.288723, 0, 0, 4.74453, -380.204773, -305.512573)" id="g2189">
                                                <g transform="matrix(1.031807e-2,0,0,1.031807e-2,143.3621,-90.91537)" id="g2202">
                                                <path d="M 2199.919 19580.113 C 1774.436 19344.611 1180.364 19198.313 530.095 19198.313 C -778.47 19198.313 -1838.165 19779.934 -1838.165 20497.149 C -1838.165 21153.706 -955.085 21692.507 188.905 21785.283 L 188.905 21407.05 C -581.783 21321.412 -1159.8 20946.746 -1159.8 20497.149 C -1159.8 19986.894 -405.167 19569.408 530.095 19569.408 C 995.721 19569.408 1417.19 19672.887 1722.254 19840.597 L 1288.742 20079.667 L 2641.462 20079.667 L 2641.462 19337.474 L 2199.919 19580.113 Z" style="fill: rgb(204, 204, 204); fill-opacity: 1;" id="path2204"/>
                                                <path d="M 188.905 18556.03 L 188.905 21407.05 L 188.905 21785.283 L 867.271 21407.05 L 867.271 18167.093 L 188.905 18556.03 Z" style="fill: rgb(255, 98, 0); fill-opacity: 1;" id="path2206"/>
                                                </g>
                                                <g transform="matrix(1.377521e-2,0,0,1.377521e-2,142.3208,-135.7131)" id="g2208"/>
                                            </g>
                                            </svg>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("saml") >
                                        <a href="${p.loginUrl}" id="saml" class="btn-social color-saml rounded-16">
                                            <svg width="48px" height="48px" viewBox="0 0 500 500" >
                                                <defs>
                                                    <bx:grid x="-2.753" y="-4.96" width="100" height="100"/>
                                                </defs>
                                                <path d="M 344.401 191.554 L 329.243 191.554 L 307.269 297.511 L 286.103 191.554 L 260.543 191.554 L 260.543 323.849 L 275.715 323.849 L 275.715 220.395 L 297.688 323.849 L 316.852 323.849 L 338.826 219.769 L 338.826 323.849 L 353.982 323.849 L 353.982 191.554 Z M 233.8 323.849 L 252.172 323.849 L 214.622 191.554 L 197.073 191.554 L 161.14 323.849 L 180.302 323.849 L 188.285 291.875 L 225.43 291.875 Z M 194.278 268.043 L 206.672 217.89 L 219.438 268.043 Z M 148.359 247.985 C 145.982 246.106 144.368 246.106 142.363 246.106 L 107.641 246.106 C 103.246 245.48 102.454 244.211 102.454 244.211 C 102.454 244.211 99.254 241.706 99.644 229.164 C 100.047 216.638 107.641 214.743 107.641 214.743 L 156.744 214.743 L 156.744 190.927 L 97.656 190.927 C 97.656 190.927 79.688 202.201 79.688 234.191 C 79.688 261.78 92.066 268.043 95.264 269.295 L 130.391 269.295 C 138.373 269.295 138.762 271.175 138.762 271.175 C 138.762 271.175 141.96 273.696 141.573 286.222 C 141.573 298.764 133.575 300.659 133.575 300.659 L 82.887 300.659 L 82.887 325.101 L 141.96 325.101 C 141.96 325.101 159.945 313.812 159.945 281.838 C 160.736 259.258 153.157 251.743 148.359 247.985 Z M 393.505 301.895 C 393.505 301.895 382.322 301.895 382.322 275.574 L 382.322 191.554 L 367.152 191.554 L 367.152 275.574 C 367.152 275.574 363.966 323.849 391.517 325.727 L 425.447 325.727 L 425.447 301.895 Z" fill="#c32527" />
                                            </svg>
                                        </a>
                                        </#if>
                                        <#if p.alias == msg("keycloak-oidc") >
                                        <a href="${p.loginUrl}" id="keycloak-oidc" class="btn-social color-openID rounded-16">
                                        <svg width="48px" height="48px" id="Guides" viewBox="0 0 64 64">
                                        <defs>
                                            <style>.cls-1{fill:none;}.cls-2{clip-path:url(#clip-path);}.cls-3{fill:#4d4d4d;}.cls-4{fill:#e1e1e1;}.cls-5{fill:#c8c8c8;}.cls-6{fill:#c2c2c2;}.cls-7{fill:#c7c7c7;}.cls-8{fill:#cecece;}.cls-9{fill:#d3d3d3;}.cls-10{fill:#c6c6c6;}.cls-11{fill:#d5d5d5;}.cls-12{fill:#d0d0d0;}.cls-13{fill:#bfbfbf;}.cls-14{fill:#d9d9d9;}.cls-15{fill:#d4d4d4;}.cls-16{fill:#d8d8d8;}.cls-17{fill:#e2e2e2;}.cls-18{fill:#e4e4e4;}.cls-19{fill:#dedede;}.cls-20{fill:#c5c5c5;}.cls-21{fill:#d1d1d1;}.cls-22{fill:#ddd;}.cls-23{fill:#e3e3e3;}.cls-24{fill:#00b8e3;}.cls-25{fill:#33c6e9;}.cls-26{fill:#008aaa;}</style>
                                            <clipPath id="clip-path">
                                            <rect class="cls-1" x="0.01" width="63.99" height="63.99"/>
                                            </clipPath>
                                        </defs>
                                        <g class="cls-2" style="" transform="matrix(0.602583, 0, 0, 0.697307, 10.846472, 10.955397)">
                                            <path class="cls-3" d="M54.81,19a0.47,0.47,0,0,1-.41-0.24L47.18,6.24A0.49,0.49,0,0,0,46.76,6H17.3a0.47,0.47,0,0,0-.41.24l-7.5,13h0L2.15,31.76a0.48,0.48,0,0,0,0,.48L9.38,44.75l7.5,13a0.47,0.47,0,0,0,.41.24H46.76a0.49,0.49,0,0,0,.42-0.24L54.4,45.23A0.47,0.47,0,0,1,54.82,45h9a0.54,0.54,0,0,0,.54-0.54V19.54A0.54,0.54,0,0,0,63.81,19h-9Z"/>
                                            <path class="cls-1" d="M63.81,19H9.8a0.49,0.49,0,0,0-.41.23h0l-0.27.46-3.3,5.73L2.16,31.76a0.48,0.48,0,0,0,0,.48l0.79,1.38L9.38,44.75A0.48,0.48,0,0,0,9.8,45h54a0.53,0.53,0,0,0,.53-0.53V19.54A0.54,0.54,0,0,0,63.81,19Z"/>
                                            <path class="cls-4" d="M11,30.68L2.95,33.61,2.16,32.24a0.48,0.48,0,0,1,0-.48l3.65-6.33Z"/>
                                            <polygon class="cls-5" points="59.03 33.02 64.35 32.26 64.35 38.43 59.03 33.02"/>
                                            <path class="cls-6" d="M59,33l5.32,5.41v6a0.53,0.53,0,0,1-.53.53H57.63Z"/>
                                            <polygon class="cls-7" points="59.03 33.02 57.63 44.99 50.19 44.99 47.93 39.56 59.03 33.02"/>
                                            <polygon class="cls-8" points="59.03 33.02 64.35 26.55 64.35 32.26 59.03 33.02"/>
                                            <path class="cls-9" d="M64.35,19.54v7L59,33,55,19h8.79A0.54,0.54,0,0,1,64.35,19.54Z"/>
                                            <polygon class="cls-10" points="50.19 44.99 46.26 44.99 45.24 43.53 47.93 39.56 50.19 44.99"/>
                                            <polygon class="cls-11" points="59.03 33.02 44.3 26.8 52.05 19 55.02 19 59.03 33.02"/>
                                            <path class="cls-12" d="M44.31,26.8l3.62,12.76L59,33Z"/>
                                            <polygon class="cls-13" points="46.26 44.99 45.19 44.99 45.24 43.53 46.26 44.99"/>
                                            <polygon class="cls-14" points="52.05 19 44.3 26.8 42.91 19.82 45.9 19 52.05 19"/>
                                            <path class="cls-15" d="M44.31,26.8L26.44,30.92l18.8,12.61Z"/>
                                            <path class="cls-12" d="M44.31,26.8l0.94,16.74,2.69-4Z"/>
                                            <path class="cls-14" d="M42.91,19.82L26.44,30.92,44.31,26.8Z"/>
                                            <polygon class="cls-16" points="26.44 30.92 18.69 44.99 14.28 44.99 11.02 30.68 26.44 30.92"/>
                                            <path class="cls-17" d="M18.5,19L11,30.68l-1.91-11,0.27-.46h0A0.49,0.49,0,0,1,9.8,19h8.7Z"/>
                                            <path class="cls-16" d="M14.28,45H9.8a0.48,0.48,0,0,1-.42-0.24L2.95,33.61,11,30.68Z"/>
                                            <polygon class="cls-18" points="11.02 30.68 5.81 25.43 9.11 19.7 11.02 30.68"/>
                                            <polygon class="cls-19" points="34.6 19 29.45 19 21.55 19 26.44 30.92 42.91 19.82 37.94 19 34.6 19"/>
                                            <polygon class="cls-19" points="19.52 19 18.5 19 11.02 30.68 26.44 30.92 21.55 19 19.52 19"/>
                                            <polygon class="cls-20" points="41.66 44.99 44.53 44.99 45.19 44.99 45.24 43.53 41.66 44.99"/>
                                            <polygon class="cls-12" points="45.24 43.53 26.44 30.92 29.83 44.99 34.6 44.99 41.66 44.99 45.24 43.53"/>
                                            <polygon class="cls-21" points="18.69 44.99 19.52 44.99 29.45 44.99 29.83 44.99 26.44 30.92 18.69 44.99"/>
                                            <polygon class="cls-22" points="42.96 19 42.91 19.82 45.9 19 44.53 19 42.96 19"/>
                                            <polygon class="cls-23" points="37.94 19 42.91 19.82 42.45 19 37.94 19"/>
                                            <polygon class="cls-17" points="42.45 19 42.91 19.82 42.96 19 42.45 19"/>
                                            <path class="cls-24" d="M29.4,19.22L22.13,31.8a0.42,0.42,0,0,0-.05.21H17L27,14.74a0.39,0.39,0,0,1,.15.14h0l2.26,3.93A0.44,0.44,0,0,1,29.4,19.22Z"/>
                                            <path class="cls-25" d="M29.39,45.23l-2.26,3.91a0.44,0.44,0,0,1-.15.14L17,32h5.07a0.39,0.39,0,0,0,0,.2v0L29.39,44.8A0.43,0.43,0,0,1,29.39,45.23Z"/>
                                            <path class="cls-26" d="M27,14.74L17,32h0l-2.5,4.33-2.39-4.14a0.39,0.39,0,0,1,0-.2,0.42,0.42,0,0,1,.05-0.21l2.42-4.19L21.89,14.9a0.42,0.42,0,0,1,.37-0.22h4.51A0.45,0.45,0,0,1,27,14.74Z"/>
                                            <path class="cls-24" d="M27,49.28a0.45,0.45,0,0,1-.21.06H22.26a0.42,0.42,0,0,1-.37-0.22L15.17,37.5l-0.66-1.15L17,32Z"/>
                                            <path class="cls-26" d="M47,32l-10,17.27a0.47,0.47,0,0,1-.15-0.14h0l-2.26-3.92a0.44,0.44,0,0,1,0-.4l7.26-12.58A0.42,0.42,0,0,0,42,32H47Z"/>
                                            <path class="cls-24" d="M52,32a0.42,0.42,0,0,1-.06.22L42.14,49.14a0.43,0.43,0,0,1-.36.2h-4.5a0.46,0.46,0,0,1-.22-0.06L47,32l2.5-4.33,2.37,4.11A0.42,0.42,0,0,1,52,32Z"/>
                                            <path class="cls-24" d="M47,32H42a0.42,0.42,0,0,0-.06-0.21L34.64,19.23a0.43,0.43,0,0,1,0-.43l2.26-3.91a0.47,0.47,0,0,1,.15-0.14Z"/>
                                            <path class="cls-25" d="M49.53,27.68h0L47,32l-10-17.27a0.46,0.46,0,0,1,.22-0.06h4.5a0.43,0.43,0,0,1,.36.2Z"/>
                                        </g>
                                        </svg>
                                        </a>
                                        </#if>
                                    </#list>
                                </div>
                            </div>
                        </#if>
                    </div> 
                </form>
            </#if>
        </div>
    </div>
    <#elseif section = "info" >
        <#if realm.password && registrationEnabled && !registrationDisabled??>
            <div id="kc-registration-container">
                <div id="kc-registration">
                    <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
                </div>
            </div>
        </#if>
    </#if>
    <script defer> 
        function disableSubmit(){
            $('#input-error').hide();
            let userName = $("#username").val();
            let password = $("#password").val();
            if( userName && password ){
                $('#kc-login').removeClass('disabled');
                $('#kc-login').removeAttr('disabled');
            }
            else{
                $('#kc-login').addClass('disabled');
                $('#kc-login').attr('disabled', 'disabled');
            }
        }
        function togglePasswordView(){
            if ($('#show_hide_password input').attr("type") == "text") {
                $('#show_hide_password input').attr('type', 'password');
                $('#show_hide_password .hide_show span').addClass("hidden_outlined");
                $('#show_hide_password .hide_show span').removeClass("visible_outlined");
            } else if ($('#show_hide_password input').attr("type") == "password") {
                $('#show_hide_password input').attr('type', 'text');
                $('#show_hide_password .hide_show span').removeClass("hidden_outlined");
                $('#show_hide_password .hide_show span').addClass("visible_outlined");
            }
            $('#inputPassword').focus();
        }
    </script>

</@layout.registrationLayout>
