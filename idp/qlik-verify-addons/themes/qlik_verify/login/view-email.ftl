<!-- ML Sent Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
        ${msg("loginTitleHtml",(realm.displayNameHtml!''))?no_esc}
    <#elseif section = "form">
        <#if realm.password>
        <div class="col-12 text-center mt-4">
        <svg class="mb-2" height="96px" fill="var(--form-accept-btn-clr)" xmlns="http://www.w3.org/2000/svg" data-name="Layer 1" viewBox="0 0 512 512"><path d="M377.17,88.35H137.52a34.06,34.06,0,0,0-34,34V252.25L43.28,209.67,83.17,176A13,13,0,1,0,66.4,156.11L13.46,200.82a13,13,0,0,0-4.61,9.93V465a47.09,47.09,0,0,0,47,47H456.11a47.09,47.09,0,0,0,47-47V210.75a13,13,0,0,0-4.61-9.93l-50.25-42.44a13,13,0,1,0-16.78,19.86l38.43,32.46L411.2,252.25V122.37A34.06,34.06,0,0,0,377.17,88.35Zm-247.68,34a8,8,0,0,1,8-8H377.17a8,8,0,0,1,8,8V270.64L257.35,361.07,129.5,270.64ZM456.11,486H55.89a21.06,21.06,0,0,1-21-21V235.55L189.49,344.92,80.12,428.66a13,13,0,1,0,15.81,20.64l115.79-88.66,38.12,27a13,13,0,0,0,15,0l38.12-27,115.8,88.66a13,13,0,0,0,15.81-20.64L325.21,344.92,477.15,237.45V465A21.06,21.06,0,0,1,456.11,486Z"/><path d="M185.34 176.44h144a13 13 0 000-26h-144a13 13 0 000 26zM185.34 230.62h144a13 13 0 000-26h-144a13 13 0 000 26zM296.93 271.8a13 13 0 00-13-13H185.34a13 13 0 000 26h98.59A13 13 0 00296.93 271.8zM318.42 48.7l-54-45.63a13 13 0 00-16.78 0l-54 45.63a13 13 0 0016.78 19.86L256 30l45.64 38.55A13 13 0 00318.42 48.7z"/></svg>
            <div class="title_sign">
                <h2>${msg("magiclink_success_header")}</h2>
        </div> 
       <p> ${msg("magiclink_success_body")} </p>
        <#if expiry??>
            <div class="form-group mb-0">
                <p class="mb-0" id="otplive">
                    ${msg("magiclink_expiring_at_msg")}  <span class="font-w-500 text-success" id="expirytime"></span>
                </p>
                <p class="mb-0 input-alert-text-error font-w-400" id="otpExpired" >
                    ${msg("magiclink_expired_msg")}
                </p>
            </div>
        </#if>        
        </#if>
        <script>
            $( document ).ready(function() {
                <#if expiry??>
                    $('#otpExpired').hide();
                    getRemainingTime(${expiry});
                </#if>
            })
        </script>
    </#if>
</@layout.registrationLayout>