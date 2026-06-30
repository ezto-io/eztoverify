<!--  Info Page   -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
<form action="${url.loginAction}" class="row" method="post">
    <input name="iws_cred_type" type="hidden" value="${iws_cred_type}">
    <div id="kc-info-message" class="text-center">
            <h4>${msg("info")}</h4>
            <p class="instruction text-center">${msg("enable-credentials")} ${app_name}</p>
            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red" >
                    ${msg("ok")}
                </button>
            </div>
        </div>
        </form>
</@layout.registrationLayout>