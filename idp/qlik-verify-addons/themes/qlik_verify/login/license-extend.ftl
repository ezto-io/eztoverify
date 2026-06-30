<#-- License Extend Req  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form action="${url.loginAction}" class="row" method="post">
            <input name="from" type="hidden" value="license-extend">
            <div class="col-12">
                <div class="title_sign">
                    <h2>${msg("license-expired-title")}</h2>
                </div>
                <p>${msg("license-expired-desc")}</p>
            </div>

            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  " >
                    ${msg("license-expired-extend-btn")}
                </button>
                <div class="col-12 social-login-seperator margin-t-2">
                    <p class="text-center font-s-13 c-gray">${msg("or")}</p>
                </div>
                <div class="text-center margin-t-1 col-12">
                    <p>
                        <span class="help-text">${msg("help-msg")}</span>  
                        <a href="${websiteContactLink}" target="_blank" > ${msg("contact-sales")} </a>  
                    </p>
                </div> 
            </div>
        </form>
    </#if>
</@layout.registrationLayout>