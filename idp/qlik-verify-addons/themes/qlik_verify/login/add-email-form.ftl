<!-- Email Form -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="email-form" action="${url.loginAction}" method="post">
            <input type="hidden" name="addEmail" value="true" />
            <div class="title_sign">
             <h2>${msg("add-email-form-title")}</h2>
            </div>
            <div class="form-group">
              <label>${msg("add-email-form-input-label")}</label>
                <#if oldEmail??>
                    <input oninput="disableButton()" id="inputEmail" name="email" type="text" class="form-control" value="${oldEmail}" required placeholder="${msg('email-placeholder')}" autofocus>
                <#else>
                  <input oninput="disableButton()" id="inputEmail" name="email" type="text" class="form-control" required placeholder="${msg('email-placeholder')}" autofocus>
                </#if> 
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
            </div>
          
              <button id="nxtbtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  " disabled="disabled">
                  ${msg("add-email-form-submit-btn")}
              </button>
        </form>
    </#if>
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
          function disableButton(){
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
</@layout.registrationLayout>