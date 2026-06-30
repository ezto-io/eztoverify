<#-- MFA troubleshoot -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="otp-verification-form" action="${url.loginAction}" class="row" method="post">
            <#if mfaProviderId??>
                <input id="mfaProviderId" name="mfaProviderId" type="hidden" value="${mfaProviderId}">
            </#if>
            <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="true" />
            <div class="col-12">
                <div class="title_sign">
                    <h2>${msg("mfa-troubleshoot")}</h2>
                </div>
            </div>
            <div class="col-12">
                <div class="form-group">
                    <label>
                        ${msg("recovery_code")}
                        <span class="text-danger"> *</span>
                    </label>
                    <input oninput="disableButton()" id="inputOTP" name="recovery_code" type="text" class="form-control" placeholder="${msg("enter-recovery-code")}"/>
                    <#import "error_msg.ftl" as e>
                    <@e.error_msg />
                </div>
            </div>

            <div class="col-12">
                <input id="actionInput" type="hidden" />
                <button id="nextBtn" type="submit" onclick="actionClicked(this)" name="verify_troubleshoot" value="true" class="btn w-100 margin-t-3 btn_account bg-orange-red" disabled="disabled">
                    ${msg("otp_ver_btn")}
                </button>
                <button type="submit" onclick="actionClicked(this)" name="cancel_troubleshoot" value="true" class="btn w-100 margin-t-3 btn_account btn-orange-border">
                    ${msg("doCancel")}
                </button>
            </div>
        </form>
        <script defer type="text/javascript">
            function actionClicked(evt){
               if(evt.name){
                   $("#actionInput").attr("name" ,evt.name);
                   $("#actionInput").val(evt.value);
               }
               $("#otp-verification-form").submit();
            }
             function disableButton(){
                $('#be-error').hide();
                const inputValue = $("#inputOTP").val().trim();
                if( inputValue ){
                    $('#nextBtn').removeClass('disabled cursor-unset');
                    $('#nextBtn').removeAttr('disabled');
                }
                else{
                    $('#nextBtn').addClass('disabled cursor-unset');
                    $('#nextBtn').attr('disabled', 'disabled');
                }
          }
        </script>
    </#if>
</@layout.registrationLayout>