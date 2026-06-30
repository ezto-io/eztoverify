<#-- License Entering Page  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form action="${url.loginAction}" class="row" method="post">
            <input name="iws_cred_type" type="hidden" value="${iws_cred_type}">
            <div class="col-12">
                <div class="title_sign">
                    <h2>${msg("sign-in-to")} ${app_name}</h2>
                </div>
                <p>${msg("provide-username-password")} ${app_name}. ${msg("one-time-process")} ${app_name} ${msg("app")}</p>
            </div>
            <div class="col-12">
                <div class="form-group">
                    <label>${msg("username")}</label>
                    <input id="iws_username" name="iws_username" type="text" class="form-control" placeholder="${msg("application-username")}"
                           oninput="disableSubmit()" required>
                </div>
                <div class="form-group">
                    <label>${msg("password")}</label>
                    <input id="iws_password" name="iws_password" type="password" class="form-control" placeholder="${msg("application-password")}"
                           oninput="disableSubmit()" required>
                    
                </div>
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
            </div>

            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red" disabled="disabled" >
                    ${msg("sign-in-to")} ${app_name}
                </button>
            </div>
        </form>
    </#if>
    <script>
     function disableSubmit(){
        $('#be-error').hide();
        let user = $("#iws_username").val();
        let pass = $("#iws_password").val();
        if( user && pass ){
            $('#nextBtn').removeClass('disabled');
            $('#nextBtn').removeAttr('disabled');
        }
        else{
            $('#nextBtn').addClass('disabled');
            $('#nextBtn').attr('disabled', 'disabled');
        }
    }
    </script>
</@layout.registrationLayout>