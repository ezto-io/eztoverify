<!-- Dynamic Forms Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="firstname-lastname-form" action="${url.loginAction}" class="row" method="post">
            <input type="hidden" name="firstNameLastNamePage" value="true">
            <div class="col-12">
                <div class="title_sign">
                    <h2>${msg("firstname_lastname_reg_title")}</h2>
                </div>
            </div>
            <div class="col-12">
                <div class="form-group">
                    <label>${msg("firstname")}</label> <span class="text-danger"> *</span>
                    <#if firstName??>
                        <input oninput="disableSubmitBtn('#inputFirstName', '#inputLastName')" id="inputFirstName" name="firstName" type="text" class="form-control"
                               value="${firstName}" placeholder="First Name eg: Mary">
                    <#else>
                        <input oninput="disableSubmitBtn('#inputFirstName', '#inputLastName')" id="inputFirstName" name="firstName" type="text" class="form-control"
                               placeholder="First Name eg: Mary" autofocus>
                    </#if>
                </div>
            </div>
            <div class="col-12">
                <div class="form-group">
                    <label>${msg("lastname")}</label> <span class="text-danger"> *</span>
                    <#if lastName??>
                        <input oninput="disableSubmitBtn('#inputFirstName', '#inputLastName')" id="inputLastName" name="lastName" type="text" class="form-control"
                               value="${lastName}" placeholder="Last Name eg: Elizabeth">
                    <#else>
                        <input oninput="disableSubmitBtn('#inputFirstName', '#inputLastName')" id="inputLastName" name="lastName" type="text" class="form-control"
                               placeholder="Last Name eg: Elizabeth">
                    </#if>
                </div>
                <#import "error_msg.ftl" as e>
                <@e.error_msg />
            </div>

            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  " disabled="disabled" >
                    ${msg("firstname_lastname_submit_btn")}
                </button>
            </div>
            <#import "login-account-link.ftl" as l>
            <@l.login_account_link />
        </form>
    </#if>
</@layout.registrationLayout>
