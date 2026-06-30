<!-- Username Entering Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="username-form" action="${url.loginAction}" method="post">
            <input type="hidden" name="userNameForm" value="true">
                <div class="title_sign">
                    <h2>${msg("doLogIn")}</h2>
                </div>
                <div class="form-group">
                    <label>${msg("username_form_input")} <span class="text-danger"> *</span></label>
                    <#if oldUserName??>
                        <input id="inputUserName" name="username" type="text" class="form-control"
                               value="${oldUserName}" required placeholder="${msg("username-placeholder")}" oninput="disableButton()" autofocus>
                    <#else>
                        <input id="inputUserName" name="username" type="text" class="form-control" required
                               placeholder="${msg("username-placeholder")}" oninput="disableButton()" autofocus>
                    </#if>
                    <#import "error_msg.ftl" as e>
                    <@e.error_msg />
                </div>
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red   cursor-unset" disabled="disabled">
                    ${msg("username_form_submit_btn")}
                </button>
            <#if realm.password && registrationEnabled && !registrationDisabled??>
                <#import "create-account-link.ftl" as c>
                <@c.create_account_link />
           </#if>
           <#import "social-login.ftl" as s>
           <@s.social_login />
        </form>
    </#if>
    <script>
        function disableButton(){
            $("#be-error").remove();
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