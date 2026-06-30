<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
<#if section = "head">
    <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
    <script defer src="/auth/assets/js/socket.min.js?version=${serverVersion}" type="text/javascript"></script>
</#if>
<#if section = "title">
        title
<#elseif section = "header">
    <span class="${properties.kcWebAuthnKeyIcon}"></span>
    ${kcSanitize(msg("ashield-registration-title"))?no_esc}
<#elseif section = "form">
            <h2 class="ciba-template-title mt-2">${msg("device-verification")}</h2>
            <#if showDevices?? && showDevices=="true">
            <div class="my-auto">
                <label class="ml-3">${msg("select-device")}</label>
                <div class="device-container my-2 mb-4">
                    <ul class="list-unstyled">
                        <#list devices as device>
                            <li class="device-list" onclick="onDeviceSelect('${device.deviceId}')">
                                <div class="d-flex align-items-center devices">
                                    <img class="device-icon"
                                        <#if device.devicePlatform == "Android">
                                            src="/auth/assets/img/android-device.svg?version=${serverVersion}" alt="device-icon"
                                        <#elseif device.devicePlatform == "iOS">
                                            src="/auth/assets/img/ios-device.svg?version=${serverVersion}" alt="device-icon"
                                        <#else>
                                            src="/auth/assets/img/mobile-device.svg?version=${serverVersion}" alt="device-icon"
                                        </#if>
                                    />
                                    <div class="ml-1">
                                        <p class="device-name">${device.deviceModel}</p>
                                        <p class="device-details"><span>${device.devicePlatform}</span> - <span>${device.deviceOSVersion}</span></p>
                                    </div>
                                </div>
                            </li>
                        </#list>
                    </ul>
                </div>
                <div id="refresh-warning" class="my-2 text-center" hidden>
                    <p>${msg("refresh-warning")}</p>
                </div>
            </div>
            <#else>
                <#if isPush?? && isPush == "true">
                    <p id="push-status" class="ciba-template-subtitle text-dark text-center mt-4">${msg('transaction-triggered-to-device')}</p>
                    <div class="my-auto">
                        <#if promptNumber??>
                            <div class="text-center mb-0 position-relative" style="min-height: 200px;">
                                <img src="${url.resourcesPath}/img/loading.gif" id="push-device-loader"/>
                                <div id="prompt" class="prompt-number-wrapper-block rounded-circle">
                                    <p class="mb-0">${promptNumber}</p>
                                </div>
                            </div>
                        <#else>
                            <p class="text-center mb-0">
                                <img src="/auth/assets/img/loading.gif?version=${serverVersion}" alt="loading-gif" id="push-device-loader"/>
                            <p>
                        </#if>
                        <div id="refresh-warning"class="my-2 text-center" hidden>
                            <p>${msg("refresh-warning")}</p>
                        </div>
                    </div>
                <#else>
                 <ol class="col-xs-12 col-sm-12 col-md-12 col-lg-12 pr-1 ml-0 my-0" id="kc-totp-settings">
                 <li>
                     <p class="mb-0 mt-2">${msg("loginTotpStep1",applicationName)}</p>
                         <div class=" d-flex justify-content-center mb-0 ml-n-md">
                             <#if appStore??>
                                 <a href="${appStore}" target="_blank"> <img class="mb-0 error-img" width="145px" alt="apple-store" src="/auth/assets/img/apple-store.svg?version=${serverVersion}"> </a>
                             </#if>
                             <#if playStore??>
                                 <a href="${playStore}" target="_blank"><img class="mb-0 error-img ml-3" width="145px" alt="play-store"  src="/auth/assets/img/play-store.svg?version=${serverVersion}"> </a>
                             </#if>
                         </div>
                 </li>
                 <li>
                    <p id="qr-status" class="col-xs-12 col-sm-12 col-md-12 col-lg-12 pr-1 pl-0 ml-0 mb-0">${msg('scan-qr-description')}</p>
                    <div class="qr-container mb-0 m-auto text-center">
                        <img id="qr-code" src="${qr}" alt="Figure: Barcode" style="display:none;">
                        <div class="spinner-border text-primary" id="qr-loading-spinner" role="status" style="margin: 78px 0px;">
                            <span class="sr-only">Loading...</span>
                        </div>
                        <#if promptNumber??>
                            <div class="text-center mb-0 position-relative">
                                <img src="${url.resourcesPath}/img/loading.gif" id="device-loader" style="display:none"/>
                                <div id="prompt" class="prompt-number-wrapper-block rounded-circle" hidden>
                                    <p class="mb-0">${promptNumber}</p>
                                </div>
                            </div>
                        <#else>
                            <p class="text-center mb-0">
                                <img src="/auth/assets/img/loading.gif?version=${serverVersion}" alt="loading-gif" id="device-loader" style="display:none"/>
                            </p>
                        </#if>
                    </div>
                    <div id="refresh-warning"class="my-2 text-center" hidden>
                            <p>${msg("refresh-warning")}</p>
                    </div>
                    </li>
                    </ol>
                    <#if deep_link??>
                        <p>
                            <span>Having trouble scanning the QR code?</span><a class="ml-1" href="${deep_link}" target="_blank">${msg("click-here")}</a>
                        </p>
                    </#if>
                </#if>
            </#if>
        <button class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} btn w-100 mt-2 btn_account bg-orange-red" id="retryBtn" onclick="onRetry()">${msg("retry")}</button>
    <form id="submit" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <#if trxId??>
                <input type="hidden" id="trxId" name="trxId" value="${trxId}"/>
            </#if>   
            <input type="hidden" id="authReqId" name="authReqId"/>
            <input type="hidden" id="retry" name="retry" value="false"/>
            <input type="hidden" id="showDevices" name="showDevices" value="${showDevices}"/>
            <input type="hidden" id="selectedDevice" name="selectedDevice"/>
            <input type="hidden" id="cancel" name="cancel" value="false"/>
        </div>
    </form>
    <script defer>

        var isDeviceSelected = false;
        function onDeviceSelect(id){
            if(!isDeviceSelected){
                isDeviceSelected = true;
                $("#selectedDevice").val(id);
                $("#submit").submit();
            }
        }

        function onRetry(){
            $("#retry").val("true");
            $("#submit").submit();
        }

        function statusUpdate(msg, img){
            if(img){
                $("#device-loader").attr("src",img);
            }
            <#if isPush?? && isPush == "true">
                if(msg){
                    $("#push-status").text(msg);
                }
                $("#push-device-loader").attr("src",img);
               // $("#push-device-loader").show();
            <#else>
                if(msg){
                    $("#qr-status").text(msg);
                    $("#qr-code").hide();
                }
                $("#device-loader").show();
            </#if>

            <#if isPrompt?? && isPrompt == "true">
                $("#prompt").removeAttr('hidden');
            </#if>


            $("#refresh-warning").removeAttr('hidden');
        }

        $( document ).ready(function() {
            <#if pollUrl??>
                startPolling();
            </#if>

            function startPolling() {
                var chatcode = '${trxId}';
                var socket = io('${pollUrl}', { transports: ['websocket'] });
                socket.auth = {chatcode};
                socket.connect();
                socket.on(chatcode, function(event){
                    if(event.auth_req_id){
                        $("#authReqId").val(event.auth_req_id);
                        $("#submit").submit();
                    }
                    if(event.status){
                        if(event.status == 201){
                            $("#retryBtn").attr("disabled", true);
                            $("#retryBtn").hide();
                            statusUpdate("${msg('web2device-verification-in-progress')}","/auth/assets/img/loading.gif?version=${serverVersion}");
                        } else if(event.status == 200){
                            $("#retryBtn").attr("disabled", true);
                            $("#retryBtn").hide();
                            statusUpdate("${msg("verification_success")}","/auth/assets/img/success-img.png?version=${serverVersion}");
                        } else {
                            if(event.status == 403){
                                $("#cancel").val("true");
                                $("#submit").submit();
                            } else {
                                $("#retryBtn").attr("disabled", false);
                                $("#retryBtn").show();
                            }
                        }
                    }
                });

                socket.on('connect',function(){
                    if ($("#qr-loading-spinner").length && $("#qr-code").length) {
                        $("#qr-loading-spinner").hide();
                        $("#qr-code").show();
                    }
                });
            }
        });
    </script>
</#if>
</@layout.registrationLayout>

