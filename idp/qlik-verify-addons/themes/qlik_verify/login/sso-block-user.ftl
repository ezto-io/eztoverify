<#-- Block User FTL -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <div class="col-12">
            <div class="title_sign">
                <h2>${msg("sso-block-user-title")}</h2>
            </div>
            <p>${msg("sso-block-user-msg")}</p>
        </div>
    </#if>
</@layout.registrationLayout>