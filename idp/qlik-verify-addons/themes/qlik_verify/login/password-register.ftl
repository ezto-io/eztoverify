<!-- Password Reg Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="password-registration-form" action="${url.loginAction}" class="row" method="post">
            <div class="col-12">
            <div class="title_sign">
                <h2>${msg("password_reg_title")}</h2>
            </div>
            </div>
            <div class="col-12 form-group --password" id="show_hide_password">
                <label>${msg("password_new_input")}</label> <span class="text-danger"> *</span>
                <div class="input-group">
                <input id="password" name="password" type="password" class="form-control" placeholder="${msg("enter-password")}" oninput="disableButton()" onkeyup="updatePolicy()"/>
                    <div class="input-group-prepend hide_show">
                        <a href=""><span class="input-group-text tio hidden_outlined"></span></a>
                    </div>
                </div>
            </div>
            <#if policy?? && policy?size gt 0>
                <div class="form-group">
                    <div class="col-12 text-left">
                        <p class="policy-list-header">${msg("password_restriction_title")}</p>
                        <div class="mb-0 d-flex flex-wrap flex-row">
                        <#list policy as p>
                            <#assign info = p.message>
                            <div class="policy-item" id="${p.policyName}">${replaceValue(info)}</div>
                        </#list>
                        </div>
                    </div>
                </div>
            </#if>
            <#function replaceValue info>
                <#if info?contains("Requires")>
                <#local info = info?replace("Requires", "") />
                </#if>
                    <#if info?contains("atleast")>
                    <#local info = info?replace("atleast", "") />
                </#if>
                <#if info?contains("specialChars")>
                    <#local infoMessage = msg("special-characters") />
                    <#local info = info?replace("specialChars", infoMessage) />
                </#if>
                <#if info?contains("minimum")>
                    <#local infoMessage = msg("minimum") />
                    <#local info = info?replace("minimum", infoMessage) />
                </#if>
                <#if info?contains("upperCase")>
                    <#local infoMessage = msg("uppercase") />
                    <#local info = info?replace("upperCase", infoMessage) />
                </#if>
                <#if info?contains("lowerCase")>
                    <#local infoMessage = msg("lowercase") />
                    <#local info = info?replace("lowerCase", infoMessage) />
                </#if>
                <#if info?contains("length")>
                    <#local infoMessage = msg("length") />
                    <#local info = info?replace("length", infoMessage) />
                </#if>
                <#if info?contains("of")>
                    <#local infoMessage = msg("of") />
                    <#local info = info?replace("of", infoMessage) />
                </#if>
                <#if info?contains("digits")>
                    <#if info?contains("1")>
                        <#local infoMessage = msg("digit") />
                        <#local info = info?replace("digits", infoMessage) />
                    </#if>
                    <#local infoMessage = msg("digits") />
                    <#local info = info?replace("digits", infoMessage) />
                </#if>
                <#return info />
            </#function>
            <div class="col-12 form-group --password" id="show_hide_password2">
                <label>${msg("password_confirm_input")}</label> <span class="text-danger"> *</span>
                <div class="input-group">
                    <input id="confirmpassword" name="confirmPassword" type="password" class="form-control" placeholder="${msg("passwordConfirm")}" oninput="disableButton()"/>
                    <div class="input-group-prepend hide_show">
                        <a href=""><span class="input-group-text tio hidden_outlined"></span></a>
                    </div>
                </div>
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
                <#if error??>
                    <div>
                        <p id="errorMsg">${error}</p>
                        <style>
                            #password, #confirmpassword{
                                border-color: red;
                            }
                        </style>
                    </div>
                <#elseif policyError??>
                    <div>
                        <#if policyErrorParams??>
                        <p id="errorMsg">${msg("${policyError}",policyErrorParams)}</p>
                        <#else>
                        <p id="errorMsg">${msg("${policyError}")}</p>
                        </#if>
                        <style>
                            #password, #confirmpassword{
                                border-color: red;
                            }
                        </style>
                    </div>
                </#if>
            </div>

          <div class="col-12">
            <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red   cursor-unset" disabled="disabled" >
              ${msg("password_reg_submit_btn")}
            </button>
          </div>
          <#import "login-account-link.ftl" as l>
          <@l.login_account_link />
        </form>
    </#if>
    <script>
        function showPass() {
            if ($('#inputPassword input').attr("type") == "text") {
                $('#inputPassword input').attr('type', 'password');
                $('#inputPassword .hide_show span').addClass("hidden_outlined");
                $('#inputPassword .hide_show span').removeClass("visible_outlined");
            } else if ($('#inputPassword input').attr("type") == "password") {
                $('#inputPassword input').attr('type', 'text');
                $('#inputPassword .hide_show span').removeClass("hidden_outlined");
                $('#inputPassword .hide_show span').addClass("visible_outlined");
            }
        }
        function showNewPass() {
            if ($('#inputConfirmPassword input').attr("type") == "text") {
                $('#inputConfirmPassword input').attr('type', 'password');
                $('#inputConfirmPassword .hide_show span').addClass("hidden_outlined");
                $('#inputConfirmPassword .hide_show span').removeClass("visible_outlined");
            } else if ($('#inputConfirmPassword input').attr("type") == "password") {
                $('#inputConfirmPassword input').attr('type', 'text');
                $('#inputConfirmPassword .hide_show span').removeClass("hidden_outlined");
                $('#inputConfirmPassword .hide_show span').addClass("visible_outlined");
            }
        }
        function updatePolicy(){
            var password = $("#password").val();
            <#list policy as p>
                validate_${p.policyName}(password);
            </#list>
        }
        
        <#list policy as p>
            <#if p.policyName = 'upperCase'>
                function validate_upperCase(password)
                    {
                        var count = password.replace(/[^A-Z]/g,"");
                        if(count !== null && count.length >= ${p.count})
                        {
                            $("#${p.policyName}").addClass("success");
                            $("#${p.policyName}").removeClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                        else{
                            $("#${p.policyName}").removeClass("success");
                            $("#${p.policyName}").removeClass("info");
                            $("#${p.policyName}").addClass("fail");
                        }
                    }

            </#if>
            <#if p.policyName = 'lowerCase'>
                function validate_lowerCase(password)
                    {
                        var count = password.replace(/[^a-z]/g,"");
                        if(count !== null && count.length >= ${p.count})
                        {
                            $("#${p.policyName}").addClass("success");
                            $("#${p.policyName}").removeClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                        else{
                            $("#${p.policyName}").removeClass("success");
                            $("#${p.policyName}").addClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                    }
            </#if>

            <#if p.policyName = 'specialChars'>
                function validate_specialChars(password)
                    {
                        var count = password.match(/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/gi);
                        if(count !== null && count.length >= ${p.count})
                        {
                            $("#${p.policyName}").addClass("success");
                            $("#${p.policyName}").removeClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                        else{
                            $("#${p.policyName}").removeClass("success");
                            $("#${p.policyName}").addClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                    }
            </#if>

            <#if p.policyName = 'minimum'>
                function validate_minimum(password)
                    {
                        var count = password.length;
                        if(count !== null && count >= ${p.count})
                        {
                            $("#${p.policyName}").addClass("success");
                            $("#${p.policyName}").removeClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                        else{
                            $("#${p.policyName}").removeClass("success");
                            $("#${p.policyName}").addClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                    }
            </#if>

            <#if p.policyName = 'digits'>
                function validate_digits(password)
                    {
                        var count = password.replace(/[^0-9]/g,"");
                        if(count !== null && count.length >= ${p.count})
                        {
                            $("#${p.policyName}").addClass("success");
                            $("#${p.policyName}").removeClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                        else{
                            $("#${p.policyName}").removeClass("success");
                            $("#${p.policyName}").addClass("fail");
                            $("#${p.policyName}").removeClass("info");
                        }
                    }
            </#if>
        </#list>

        
        
        <#global funcs="" />
        <#list policy as p>
            <#if funcs?length gt 0>
                <#global funcs="${funcs} && hasSuccessClassin${p.policyName}()"/>
            <#else>
                <#global funcs="hasSuccessClassin${p.policyName}()"/>
            </#if>
            function hasSuccessClassin${p.policyName}(){
                    if ($("#${p.policyName}").hasClass("success")) {
                            return true;
                    } else {
                        return false;
                    }
            }
        </#list>
        function matchPolicy(){
            <#if funcs?length gt 0>
                if (${funcs?no_esc}){
                    return true;
                } else { 
                    return false;
                }
                <#else>
                    return true;
            </#if>
        }
        function isBlank(str) {
            return (!str || /^\s*$/.test(str));
        }
        function disableButton(){
            var empty = false;
            if (isBlank($("#password").val()) || isBlank($("#confirmpassword").val())) {
                empty = true;
            }
            let Value = $("#password").val();
            let conf = $("#confirmpassword").val();
            updatePolicy();
            if (empty || Value !== conf || !matchPolicy()) {
                $('#nextBtn').addClass('disabled cursor-unset');
                $('#nextBtn').attr('disabled', 'disabled');
            } else {
                $('#nextBtn').removeClass('disabled cursor-unset');
                $('#nextBtn').removeAttr('disabled');
            }
        }
    </script>

</@layout.registrationLayout>