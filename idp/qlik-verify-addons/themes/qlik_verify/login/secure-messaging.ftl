<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "head">
        <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
    </#if>
    <#if section = "header">
        ${msg("secure-messaging-title")}
    <#elseif section = "form">
        <#if ismobile?? && ismobile>
            <div class="d-flex flex-column justify-content-between ciba-template-height">
                <div class="row">
                    <#--  Template Header  -->
                    <div class="col-12 mt-2" id="template-header">
                        <h2 class="secure-message-title mb-0">${msg("secure-messaging-title")}</h2>
                    </div>

                    <#--  Template Body  -->
                    <div class="col-12 mt-3" id="template-body">
                        <p id="message-time" class="secure-message-time mb-1">${time!""}</p>
                        <div class="secure-message-body p-2">
                            <#if sm_type == 'otp'>
                                <div class="d-flex justify-content-between align-items-center secure-message-body-border-bottom">
                                    <div>
                                        <p class="primary-color my-0">${msg("code")}</p>
                                        <p class="secure-message-otp secure-message text-dark mb-1 mr-1">${kcSanitize(sm_otp)?no_esc}</p>
                                    </div>
                                    <div class="secure-message-otp-copy-btn cursor-pointer px-3 py-1" id="copy-btn" onclick="copyOTP()">${msg("copy")}</div>
                                </div>
                            </#if>
                            <div class="my-1 secure-message">${kcSanitize(sm)?no_esc}</div>
                        </div>
                    </div>
                </div>

                <div class="row mt-3">
                    <form action="${url.loginAction}" method="POST" class="col-12 d-flex mt-3" id="template-button">
                        <#--  Template Hidden Fields  -->
                        <input type="hidden" name="from" value="secure-messaging"/>
                        <#--  Template Buttons  -->
                        <input name="accept" id="kc-accept" type="submit" class="w-100 py-3 ciba-template-submitbtn" value="${msg('ok')}"/>
                    </form>
                </div>
            </div>
        <#else>
            <div class="col-12 mt-2" id="template-header">
                <h2 class="secure-message-title mb-0">${msg("secure-messaging-title")}</h2>
            </div>
            <div class="col-12 text-center">
                <svg class="mb-2 error-img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><g fill="var(--form-accept-btn-clr)" data-name="26 - error"><path d="M18.87,3.77a3.21,3.21,0,0,0-5.74,0L2.34,25.35A3.21,3.21,0,0,0,5.21,30H26.79a3.21,3.21,0,0,0,2.87-4.65Zm9,23.66a1.2,1.2,0,0,1-1,.57H5.21a1.2,1.2,0,0,1-1.08-1.75L14.92,4.67a1.21,1.21,0,0,1,2.16,0L27.87,26.25A1.21,1.21,0,0,1,27.82,27.43Z"/><path d="M16 11a1 1 0 0 0-1 1v8a1 1 0 0 0 2 0V12A1 1 0 0 0 16 11zM16 23a1 1 0 1 0 1 1A1 1 0 0 0 16 23zm0 1z"/></g></svg>
            </div>
            <div>
                <p class="text-center">${msg("secure-messaging-not-supported")}</p>
            </div>
        </#if>
        <script> 
            function copyOTP(){
                copyToClipboard("${sm_otp}");
                $('#copy-btn').text("copied");
                setTimeout(() =>  $('#copy-btn').text("${msg('copy')}"), 1000);
            }

            $(document).ready(function () {
                if($('#message-time').text() != ''){
                    const dateMillisecond = parseInt($('#message-time').text(), 10);
                    $('#message-time').text(formatDate(dateMillisecond));
                }

                function formatDate(milliseconds) {
                    var date = new Date(milliseconds);
                    var day = date.getDate();
                    var month = date.getMonth() + 1;
                    var year = date.getFullYear();
                    var hours = date.getHours();
                    var minutes = date.getMinutes();
                    day = (day < 10) ? '0' + day : day;
                    month = (month < 10) ? '0' + month : month;
                    hours = (hours < 10) ? '0' + hours : hours;
                    minutes = (minutes < 10) ? '0' + minutes : minutes;
                    var ampm = (hours >= 12) ? 'PM' : 'AM';
                    hours = hours % 12;
                    hours = (hours == 0) ? 12 : hours;
                    var formattedDateTime = day + '-' + month + '-' + year + ' ' + hours + ':' + minutes + ' ' + ampm;
                    return formattedDateTime;
                }
            });
        </script>
    </#if>
</@layout.registrationLayout>
