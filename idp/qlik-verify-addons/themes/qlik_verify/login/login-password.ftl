<!-- PWD Entering Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="login-password-form" action="${url.loginAction}" class="row" method="post">
          <input name="isLoginPasswordForm" value="true" type="hidden">
          <input name="chosenType" value="passwordLoginOption" type="hidden">
          <input id="edit" name="edit" value="false" type="hidden">
          <div class="col-12">
            <div class="title_sign">
              <h2>${msg("pwd_form_title")}</h2>
            </div>
          </div>
           <#if email??>
               <div class="col-12">
                   <div class="form-group">
                       <label>${msg("pwd_form_email")}</label>
                       <div style="position: relative">
                           <input name="email" type="email" class="form-control" value="${email}" disabled>
                           <a href="${url.loginRestartFlowUrl}">
                              <i class="tio editBtn cursor-pointer">edit</i>
                           </a>
                       </div>
                   </div>
               </div>
          </#if>
          <#if phoneNumber??>
              <div class="col-12">
                  <div class="form-group">
                      <label>${msg("pwd_form_mob_num")}</label>
                      <div style="position: relative">
                          <input name="phoneNumber" type="tel" class="form-control" value="${phoneNumber}" disabled>
                          <a href="${url.loginRestartFlowUrl}">
                              <i class="tio editBtn cursor-pointer">edit</i>
                          </a>
                      </div>
                  </div>
              </div>
          </#if>
            <#if username??>
                <div class="col-12">
                    <div class="form-group">
                        <label>${msg("pwd_form_username")}</label>
                        <div style="position: relative">
                            <input name="username" type="text" class="form-control" value="${username}" disabled>
                            <a href="${url.loginRestartFlowUrl}">
                               <i class="tio editBtn cursor-pointer">edit</i>
                            </a>
                        </div>
                    </div>
                </div>
            </#if>
            <div class="col-12 form-group --password" id="show_hide_password">
                <label for="password">${msg("pwd_form_pwd")}</label> <span class="text-danger"> *</span>
                <div class="input-group">
                    <input oninput="disableButton()" tabindex="2" id="password" class="form-control" data-toggle="password" placeholder="${msg("enter-password")}"
                           required="" name="password" type="password" autocomplete="off"
                           aria-invalid="<#if error??>true</#if>"/>

                    <div class="input-group-prepend hide_show">
                        <a href=""><span class="input-group-text tio hidden_outlined"></span></a>
                    </div>

                </div>
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
            </div>
            <div class="col-12 form-group">
                <div id="kc-form-options" class="d-flex">
                    <div class="${properties.kcFormOptionsWrapperClass!} ml-auto">
                        <#if resetPasswordEnabled>
                            <span><a tabindex="5" class="font-s-13 action-link-clr" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                        </#if>
                    </div>
                </div>
                <div class="kc-form-buttons">
                    <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  " disabled="disabled" >
                       ${msg("pwd_form_submit_btn")}
                    </button>
                </div>
            </div>

        </form>
        <script>
            function disableButton(){
                $("#be-error").remove();
                let conf = $("#password").val();
                if (!conf) {
                    $('#nextBtn').addClass('disabled cursor-unset');
                    $('#nextBtn').attr('disabled', 'disabled');
                } else {
                    $('#nextBtn').removeClass('disabled cursor-unset');
                    $('#nextBtn').removeAttr('disabled');
                }
            }
        </script>
    </#if>
</@layout.registrationLayout>