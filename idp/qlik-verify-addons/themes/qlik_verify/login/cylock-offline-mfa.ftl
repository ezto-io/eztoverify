<!-- Cylock OTP Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="cylock-registration-form" action="${url.loginAction}" method="post">
            <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
            <#if mfaProviderId??>
                <input id="mfaProviderId" name="mfaProviderId" type="hidden" value="${mfaProviderId}">
            </#if>
            <div class="title_sign">
                 <h2>${msg('${title}')}</h2>
                 <#if isCROTP??>
                    <label class="mt-2">${msg("cylock-mfa-otp-generation")}</label>
                 </#if>
                 <#if isPOTP??>
                    <label class="mt-2">${msg("cylock-mfa-otp-verified")}</label>
                 </#if>
            </div>
            <div class="form-group">
                <label>${msg("cylock-otp")}</label>
                <input id="inputOTP" name="otp" type="text" class="form-control" placeholder="${msg('enter-your-otp')}" required>
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
            </div>
            <button id="cancelBtn" value="cancel" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red">
              ${msg("otp_ver_btn")}
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
        <script defer type="text/javascript">
            $('#otpExpired').hide();
            var _done = false;
            $(document).ready(function () { 
                if (_done === true) {
                    return;
                }
                console.log("document ready called");
                _done = true;
                //sessionExpiryCounter(${expiry});
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
                    }
                }, 1000);
            }
        </script>
    </#if>
</@layout.registrationLayout>