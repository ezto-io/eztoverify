<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','confirmpassword'); section>
    <#if section = "header">
        ${msg("updatePasswordTitle")}

    <#elseif section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" class="row ${properties.kcFormClass!}" method="post">
            <input type="text" id="username" name="username" value="${username}" autocomplete="username"

                   readonly="readonly" style="display:none;"/>
            <h4 class="col-12">${msg("upd_pwd_title")}</h4> <br>
            <#if reqFromWebsite?? && reqFromWebsite>
                <div class="warning-container">
                    <p class="warning-text">
                        <i class="tio warning-icon">${msg("warning-outlined")}</i>
                        ${msg("upd_pwd_warning_msg")}

                    </p>
                </div>
            </#if>
            <div class="col-12 form-group --password" id="show_hide_password">
                <label>${msg("passwordNew")}</label> <span class="text-danger"> *</span>
                <div class="input-group">
                <input id="password" name="password" type="password" class="form-control" placeholder="${msg("enter-password")}" oninput="disableButton()" onkeyup="updatePolicy()"/>
                    <div class="input-group-prepend hide_show cursor-pointer" onclick="togglePasswordView()">
                        <span class="input-group-text tio hidden_outlined"></span>
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
                    <div class="input-group-prepend hide_show cursor-pointer" onclick="toggleConfirmPasswordView()">
                        <span class="input-group-text tio hidden_outlined"></span>
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
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <button id="submitBtn" class="btn w-100 margin-t-3 btn_account bg-orange-red   disabled" disabled="disabled" type="submit">${msg("doSubmit")}</button>
                        <button class="btn w-100 margin-t-3 btn_account bg-orange-red  " type="submit" name="cancel-aia" value="true" >${msg("doCancel")}</button>
                    <#else>
                        <button id="submitBtn" class="btn w-100 margin-t-3 btn_account bg-orange-red   disabled" disabled="disabled" type="submit">${msg("doSubmit")}</button>
                    </#if>
                </div>
            </div>
        </form>
    </#if>
    <script>
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

                $('#submitBtn').addClass('disabled cursor-unset');

                $('#submitBtn').attr('disabled', 'disabled');

            } else {

                $('#submitBtn').removeClass('disabled cursor-unset');

                $('#submitBtn').removeAttr('disabled');

            }

        }

    </script>
</@layout.registrationLayout>