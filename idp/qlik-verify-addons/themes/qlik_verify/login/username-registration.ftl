<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="user-name-form" action="${url.loginAction}" method="post">
          <input type="hidden" name="userNamePage" value="true">
            <div class="title_sign">
              <h2>${msg("username-registration")}</h2>
            </div>
            <div class="form-group">
              <label>${msg("username")} <span class="text-danger"> *</span></label>
              <#if userName?? >
                <input oninput="disableButton()" id="inputUserName" name="username" type="text" class="form-control" value="${userName}" placeholder="${msg("username-example")}">
              <#else>
                <input oninput="disableButton()" id="inputUserName" name="username" type="text" class="form-control" placeholder="${msg("username-example")}">
              </#if>
              <#if error??>
                <p id="be-error" class="error-text input-alert-text-error">${msg('${error}')}</p>
              </#if>
            </div>
            <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red   cursor-unset" disabled="disabled">
              Next
            </button>
            <#import "login-account-link.ftl" as l>
            <@l.login_account_link />
        </form>
    </#if>

    <script>
        <#if error??>
        $("#inputUserName").addClass('input-border-red');
        </#if>
        function disableButton(){
            $("#be-error").remove();
            $("#inputUserName").removeClass('input-border-red');
            let conf = $("#inputUserName").val();
            if (!conf) {
                $('#nextBtn').addClass('disabled cursor-unset');
                $('#nextBtn').attr('disabled', 'disabled');
            } else {
                $('#nextBtn').removeClass('disabled cursor-unset');
                $('#nextBtn').removeAttr('disabled');
            }
        }
    </script>
</@layout.registrationLayout>