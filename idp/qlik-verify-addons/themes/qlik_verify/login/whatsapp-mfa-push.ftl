<!-- Whatsapp MFA Push Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="whatsapp-registration-form" action="${url.loginAction}" method="post">
            <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
            <#if mfaProviderId??>
                <input id="mfaProviderId" name="mfaProviderId" type="hidden" value="${mfaProviderId}">
            </#if>
            <input name="hookdata" type="hidden" id="whatsapphook" />
            <div class="title_sign mb-2">
                <#if pageType??>
                    <h2>${msg("loginTotpHeader")}</h2>
                <#else>
                    <h2>${msg('${title}')}</h2>
                </#if>
                <div>
                    <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
                </div>
              <label class="mt-2">${msg("wp_mfa_push_notification_desc")}</label>
            </div>
            <div class="form-group mt-3">
                <#if expiry??>
                    <label id="otplive">
                            ${msg("session_expires_push_in_msg")} <span class="font-w-500 c-blue" id="expirytime"></span>
                    </label>
                    <label class="input-alert-text-error font-w-400" id="otpExpired" >
                            ${msg("session_expired_msg")}
                    </label>
                </#if>
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
            </div>

            <button id="cancelBtn" name="cancel" value="cancel" type="submit" class="btn w-100 mt-2 btn_account bg-orange-red">
              ${msg("doCancel")}
            </button>
            <button name="retry" value="retry" id="nextBtn" type="submit" class="btn w-100 mt-2 btn_account bg-orange-red">
                ${msg("doRetry")}
            </button>
            <#if showTroubleShoot?? && showTroubleShoot>
                <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                    <a onclick="troubleShootMFAFunc()" class="action-link-clr cursor-pointer">
                        ${msg("troubleshoot_mfa")}
                    </a>
                </p>
            </#if>
            <div class="text-center mt-3">
                <img src="${url.resourcesPath}/img/twilio_powered.png" alt="twilio-powered" class="form-provider-img">
            </div>
            <#if isFromReg??>
                <hr/>
                <#if realm.password && registrationEnabled && !registrationDisabled?? && !isFromReg>
                <#import "create-account-link.ftl" as c>
                <@c.create_account_link />
                <#import "social-login.ftl" as s>
                <@s.social_login />
                </#if >
                <#if isFromReg>
                    <#import "login-account-link.ftl" as l>
                    <@l.login_account_link />
                </#if>
            </#if>
        </form>
        <script defer src="${url.resourcesPath}/js/socket.min.js" type="text/javascript"></script>
        <script>
            function troubleShootMFAFunc(){
                $("#troubleShootMFA").val("true");
                $("#whatsapp-registration-form").submit();
            }
            $('#otpExpired').hide();
            $("#nextBtn").hide();
            $("#cancelBtn").show();
            var _done = false;
            let isExpired = false;
            $(document).ready(function () {
                if (_done === true) {
                    return;
                }
                console.log("document ready called");
                _done = true;
                <#if expiry??>
                    sessionExpiryCounter(${expiry});
                <#else>
                    sessionExpiryCounter(0);
                </#if>
                startPolling();
            });
            function sessionExpiryCounter(expiry) {
                console.log("executing");
                var countDownDate = expiry;
                var x = setInterval(function () {
                    var count = "";
                    var now = new Date().getTime();
                    var distance = countDownDate - now;
                    var days = Math.floor(distance / (1000 * 60 * 60 * 24));
                    var hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                    var minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
                    var seconds = Math.floor((distance % (1000 * 60)) / 1000);

                    if (days) {
                        count += days + "d ";
                    }
                    if (hours) {
                        count += hours + "h ";
                    }
                    if (minutes) {
                        count += minutes + "m ";
                    }
                    if (seconds) {
                        count += seconds + "s ";
                    }
                    document.getElementById("expirytime").innerHTML = count;

                    if (distance < 1) {
                        isExpired = true;
                        clearInterval(x);
                        $('#otplive').hide();
                        $('#otpExpired').show();
                        $("#cancelBtn").hide();
                        $("#nextBtn").show();
                        $('#nextBtn').removeClass('disabled cursor-unset');
                        $('#nextBtn').removeAttr('disabled', 'disabled');
                    }
                }, 1000);
            }

            function startPolling() {
                var chatcode = '${chatcode}';
                var socket = io('${socketUrl}', { transports: ['websocket'] });
                socket.auth = {chatcode};
                socket.connect();
                socket.on(chatcode, function(event){
                    console.log(event);
                    if(!isExpired){
                        $("#whatsapphook").val(JSON.stringify(event));
                        $("#whatsapp-registration-form").submit();
                    }
                });
                socket.on('connect',function(){
                    console.log("Connection established, to look for messages");
                });
            }
        </script>
    </#if>
</@layout.registrationLayout>