<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header" || section = "show-username">
        <script type="text/javascript">
            function fillAndSubmit(authExecId) {
                document.getElementById('authexec-hidden-input').value = authExecId;
                document.getElementById('kc-select-credential-form').submit();
            }
        </script>
        <#if section = "header">
            ${msg("loginChooseAuthenticator")}
        </#if>
    <#elseif section = "form">
        <div class="title_sign">
          <h2> ${msg("loginChooseAuthenticator")}</h2>
        </div>
        <form id="kc-select-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" id="authexec-hidden-input" name="authenticationExecution" />
            <div class="col-12 p-0">
            <div class="form-group mb-0">
                <div>
                <#assign checkedVal = 0 >
                <#list auth.authenticationSelections as authenticationSelection>
                    <div class="mt-3 radio-wrap radio-wrap d-flex flex-row  align-items-center">
                        <input id="${authenticationSelection.authExecId}" type="radio" checked = " <#if checkedVal == authenticationSelection?index > true <#else> false </#if> " name="chosenType" value="${authenticationSelection.authExecId}">
                        <label for="${authenticationSelection.authExecId}"></label>
                        <span>${msg('${authenticationSelection.displayName}')} </span>
                    </div>
                 </#list>
                </div>
                <button id="Submit" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
                    ${msg("mul_opt_proceed_btn")}
                </button>
            </div>
          </div>
        </form>

    </#if>
    <script type="text/javascript">
        const btn = document.querySelector('#Submit');
         btn.onclick = function () {
            const inputs = document.querySelectorAll('input[name="chosenType"]');
            let selectedValue;
            for (const input of inputs) {
                if (input.checked) {
                    document.getElementById('authexec-hidden-input').value = input.value;
                    break;
                }
            }
        };
        </script>
</@layout.registrationLayout>

