<!-- Email Form -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="email-form" action="${url.loginAction}" method="post">
            <div class="title_sign">
             <h2>${msg("doLogIn")}</h2>
            </div>
            <div class="form-group">
              <label>${msg("email_form_input")}</label> <span class="text-danger"> *</span>
                <#if oldEmail??>
                    <input oninput="disableformBtn()" id="inputEmail" name="email" type="text" class="form-control" value="${oldEmail}" required placeholder="${msg('email-placeholder')}" autofocus>
                <#else>
                    <input oninput="disableformBtn()" id="inputEmail" name="email" type="text" class="form-control" required placeholder="${msg('email-placeholder')}" autofocus>
                </#if> 
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
            </div>
          
              <button id="nxtbtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  " disabled="disabled">
                  ${msg("email_form_submit_btn")}
              </button>
          <#if realm.password && registrationEnabled && !registrationDisabled??>
          <#import "create-account-link.ftl" as c>
          <@c.create_account_link />
           </#if>
           <#import "social-login.ftl" as s>
           <@s.social_login />
        </form>
        <script>
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
          function disableformBtn(){
            let email = $("#inputEmail").val();
            if( email && validateEmail(email.trim()) ){
              $('#nxtbtn').removeClass('disabled');
              $('#nxtbtn').removeAttr('disabled');
            }
            else{
              $('#nxtbtn').addClass('disabled');
              $('#nxtbtn').attr('disabled', 'disabled');
            }
          }    
    </script>
  </#if>
</@layout.registrationLayout>