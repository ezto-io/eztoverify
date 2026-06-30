<#import "template.ftl" as layout>
<@layout.mainLayout active='password' bodyClass='password'; section>
    <div class="element-wrapper">
        <h1 class="element-header mb-0">${msg("changePasswordHtmlTitle")}</h1>
    </div> 

    <div class="element-box"> 
        <form action="${url.passwordUrl}" class="form-horizontal" method="post">
            <input type="text" id="username" name="username" value="${(account.username!'')}" autocomplete="username" readonly="readonly" style="display:none;">

            <#if password.passwordSet>
                <div class="form-group">
                    <div class="col-sm-2 col-md-2">
                        <label for="password" class="control-label">${msg("password")}</label> <span class="required">*</span>
                    </div>

                    <div class="col-sm-10 col-md-10">
                        <div class="input-group" id="password_show_hide_password">
                            <input class="form-control br-top-4 br-bottom-4 social-password" type="password" id="password" name="password" autofocus autocomplete="current-password" required/>
                            <div class="input-group-prepend hide_show">
                                <span onclick="hideShowPassword('password')"><i class="input-group-text os-icon os-icon-eye-off my-1 px-0 mr-2 r-0 cursor-pointer"></i></span>
                            </div>
                        </div>
                    </div>
                </div>
            </#if>

            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

            <div class="form-group">
                <div class="col-sm-2 col-md-2">
                    <label for="password-new" class="control-label">${msg("passwordNew")}</label> <span class="required">*</span>
                </div>

                <div class="col-sm-10 col-md-10">
                    <div class="input-group" id="passwordNew_show_hide_password">
                        <input class="form-control br-top-4 br-bottom-4 social-password" type="password" id="password-new" name="password-new" autocomplete="new-password" required/>
                        <div class="input-group-prepend hide_show">
                            <span onclick="hideShowPassword('passwordNew')"><i class="input-group-text os-icon os-icon-eye-off my-1 px-0 mr-2 r-0 cursor-pointer"></i></span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <div class="col-sm-2 col-md-2">
                    <label for="password-confirm" class="control-label" class="two-lines">${msg("passwordConfirm")}</label> <span class="required">*</span>
                </div>

                <div class="col-sm-10 col-md-10">
                    <div class="input-group" id="passwordConfirm_show_hide_password">
                        <input class="form-control br-top-4 br-bottom-4 social-password" type="password" id="password-confirm" name="password-confirm" autocomplete="new-password" required/>
                        <div class="input-group-prepend hide_show">
                            <span onclick="hideShowPassword('passwordConfirm')"><i class="input-group-text os-icon os-icon-eye-off my-1 px-0 mr-2 r-0 cursor-pointer"></i></span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                    <div class="">
                        <button type="submit" class="btn btn-primary" name="submitAction" value="Save" disabled>${msg("changePwd")}</button>
                    </div>
                </div>
            </div>
        </form>
        <script defer type="text/javascript">
            function hideShowPassword(option) {
                if ($("#"+option+"_show_hide_password input").attr("type") === "text") {
                    $("#"+option+"_show_hide_password input").attr('type', 'password');
                    $("#"+option+"_show_hide_password .hide_show i").addClass("os-icon-eye-off");
                    $("#"+option+"_show_hide_password .hide_show i").removeClass("os-icon-eye");
                } else if ($("#"+option+"_show_hide_password input").attr("type") === "password") {
                    $("#"+option+"_show_hide_password input").attr('type', 'text');
                    $("#"+option+"_show_hide_password .hide_show i").removeClass("os-icon-eye-off");
                    $("#"+option+"_show_hide_password .hide_show i").addClass("os-icon-eye");
                }
            } 
        </script>
    </div>

</@layout.mainLayout>
