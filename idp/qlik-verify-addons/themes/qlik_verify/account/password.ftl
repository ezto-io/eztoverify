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
            // Eye icon next to a password field: a click shows the typed password, the next click hides it again.
            // `option` names the field's wrapper (#<option>_show_hide_password); its icon carries os-icon-eye while the
            // password is shown and os-icon-eye-off while it is hidden. Fields of any other type are left as they are.
            function hideShowPassword(option) {
                var wrapper = '#' + option + '_show_hide_password';
                var fields = $(wrapper + ' input');
                var current = fields.attr('type');
                if (current !== 'password' && current !== 'text') {
                    return;
                }
                var reveal = current === 'password';
                fields.attr('type', reveal ? 'text' : 'password');
                $(wrapper + ' .hide_show i').toggleClass('os-icon-eye', reveal).toggleClass('os-icon-eye-off', !reveal);
            }
        </script>
    </div>

</@layout.mainLayout>
