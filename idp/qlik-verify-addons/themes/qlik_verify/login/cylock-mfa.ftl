<!-- Cylock Push MFA Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="cylock-registration-form" action="${url.loginAction}" method="post">
            <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
            <#if mfaProviderId??>
                <input id="mfaProviderId" name="mfaProviderId" type="hidden" value="${mfaProviderId}">
            </#if>
            <input name="hookdata" type="hidden" id="cylockhook" />
            <div class="title_sign">
                <h2>${msg("loginTotpHeader")}</h2>
                <div>
                    <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
                </div>
                <label class="mt-2">${msg("cylock-authentication")}</label>
            </div>
            <hr/>
            <div class="form-group">
                <#if expiry??>
                    <div class="form-group mb-0">
                        <label class="mb-0" id="otplive">
                            ${msg("session_expires_in_msg")} <span class="font-w-500 c-primary" id="expirytime"></span>
                        </label>
                        <label class="mb-0 input-alert-text-error font-w-400" id="otpExpired" >
                            ${msg("session_expired_msg")}
                        </label>
                    </div>
                </#if>
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
            </div>
            <button id="cancelBtn" name="cancel" value="cancel" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red">
              ${msg("doCancel")}
            </button>
            <button name="retry" value="retry" id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account btn-orange-border cursor-unset" disabled="disabled" >
                   ${msg("doRetry")}
            </button>
             <#if showTroubleShoot?? && showTroubleShoot>
                <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                    <a onclick="troubleShootMFAFunc()" class="action-link-clr cursor-pointer">
                        ${msg("troubleshoot_mfa")}
                    </a>
                </p>
            </#if>
            <hr/>
            <img class="col-12" src="${url.resourcesPath}/img/cylock_powered.png" alt="cylock-powered">
        </form>
        <script defer src="${url.resourcesPath}/js/socket.min.js" type="text/javascript"></script>
        <script> 
            $('#otpExpired').hide();
            $("#nextBtn").hide();
            $("#cancelBtn").show();
            var _done = false;
            $(document).ready(function () {
                if (_done === true) {
                    return;
                }
                console.log("document ready called");
                _done = true;

                sessionExpiryCounter(${expiry});
                startPolling();
            });

            function troubleShootMFAFunc(){
                $("#troubleShootMFA").val("true");
                $("#cylock-registration-form").submit();
            }

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
                    $("#cylockhook").val(JSON.stringify(event));
                    $("#cylock-registration-form").submit();
                });
                socket.on('connect',function(){
                    console.log("Connection established, to look for messages");
                });
            }
        </script>
    </#if>
</@layout.registrationLayout>