<!-- Email Reg Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="email-registration-form" action="${url.loginAction}" method="post">
          <input name="isEmailRegistration" value="true" type="hidden">
            <div class="title_sign">
              <h2 class="font-big" >${msg("email_reg_title")}</h2>
            </div>
            <div class="form-group">
              <label>${msg("email_input")}</label> <span class="text-danger"> *</span>
              <#if oldEmail??>
                <input oninput="disableButton()" id="inputEmail" name="email" type="email" class="form-control" value=${oldEmail} placeholder="${msg("email-placeholder")}" autofocus>
              <#else>
                <input oninput="disableButton()" id="inputEmail" name="email" type="email" class="form-control" placeholder="${msg("email-placeholder")}" autofocus>
              </#if>
              <#import "error_msg.ftl" as e>
              <@e.error_msg />
            </div>

            <button id="nxtbtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red   cursor-unset" disabled="disabled">
              ${msg("email_reg_submit_btn")}
            </button>
            <#import "login-account-link.ftl" as l>
            <@l.login_account_link />
        </form>
    </#if>
    <script>
        <#if error_msg??>
           $("#inputEmail").addClass('input-border-red');
        </#if>

          const Configuration = {
            EmailRegex: /^(([^<>!%&*#$()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,63}))$/,
            Spacing: /\s/,
          };


          function validateEmail (email){
            if( email && email.match(Configuration.EmailRegex)  ){
              return true;
            } else {
              return false;
            }
          }

          function disableButton(){
            $('#be-error').hide();
            $("#inputEmail").removeClass('input-border-red');
            let email = $("#inputEmail").val();
            if( email && validateEmail(email.trim()) ){
              $('#nxtbtn').removeClass('disabled cursor-unset');
              $('#nxtbtn').removeAttr('disabled');
            }
            else{
              $('#nxtbtn').addClass('disabled cursor-unset');
              $('#nxtbtn').attr('disabled', 'disabled');
            }
          }    
    </script>
</@layout.registrationLayout>