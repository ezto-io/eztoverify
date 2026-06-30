<#--  MFA Reg Options Page  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="otp-login-form" action="${url.loginAction}" class="row" method="post"> 
            <#if mfaProviderId??>
                <input id="mfaProviderId" name="mfaProviderId" type="hidden" value="${mfaProviderId}">
            </#if>
            <div class="col-12">
                <div class="title_sign">
                    <h4>${msg("mfa-recovery-primary-header")}</h4>
                </div>
                <p>${msg("mfa-recovery-desc")}</p>
            </div>
            <div class="col-12">
            <i id="copyIcon" class="btn os-icon os-icon-copy snippet-copy-icon position-absolute cursor-pointer d-flex align-items-center justify-content-center" onclick="copyToClipboard('recoveryCode')"></i>
            <ol class="mb-0 py-3 recovery-code-box" id="recoveryCode">
            <img id="successGif" class="position-absolute mfa-success-gif d-none" alt="success-gif" src="${url.resourcesPath}/img/success.gif" width="250"/>
                <#list recoveryCode as rc>
                    <li>${rc}</li>
                </#list>
            </ol>
            </div>

            <div class="col-12">
                <input id="actionInput" type="hidden" />
                <button id="nextBtn" type="button" onclick="actionClicked(this)" name="rc_copied" value="copied" class="btn w-100 margin-t-3 btn_account bg-orange-red cursor-unset">
                    ${msg("copied")}
                </button>
                <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
                    ${msg("skip-link")}
                    <a onclick="actionClicked(this)" name="rc_skip" value="skip" class="action-link-clr cursor-pointer">
                    ${msg("doClickHere")}
                    </a>
                </p>
            </div>
        </form>
        <script defer type="text/javascript">
            function actionClicked(evt){
               if(evt.name){
                   $("#actionInput").attr("name" ,evt.name);
               }
               $("#otp-login-form").submit();
            }
            function copyToClipboard(element){
                $("#successGif").addClass('d-block');
                const hideSuccessGifTimeout = setTimeout(function () {
                    $("#successGif").removeClass('d-block');
                    hideSuccessGifTimeout ? clearTimeout(hideSuccessGifTimeout) : '';
                }, 1200);
                var $temp = $("<input>");
                $("body").append($temp);
                $temp.val($('#'+element).text().trim()).select()
                document.execCommand("copy");
                $temp.remove();
            }
        </script>
    </#if>
</@layout.registrationLayout>