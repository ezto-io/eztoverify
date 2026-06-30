<!-- SL Link Email Sent FTL -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <div class="title_sign text-center">
          <h2>${msg("emailVerifyTitle")}</h2>
        </div>
        <p id="instruction3" class="instruction text-center">
            ${msg("email-verification-msg")}
        </p>
    </#if>
</@layout.registrationLayout>