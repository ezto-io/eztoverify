<!--  Error Page  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
    <div class="col-12 text-center">
     <svg class="mb-2 error-img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><g fill="var(--form-accept-btn-clr)" data-name="26 - error"><path d="M18.87,3.77a3.21,3.21,0,0,0-5.74,0L2.34,25.35A3.21,3.21,0,0,0,5.21,30H26.79a3.21,3.21,0,0,0,2.87-4.65Zm9,23.66a1.2,1.2,0,0,1-1,.57H5.21a1.2,1.2,0,0,1-1.08-1.75L14.92,4.67a1.21,1.21,0,0,1,2.16,0L27.87,26.25A1.21,1.21,0,0,1,27.82,27.43Z"/><path d="M16 11a1 1 0 0 0-1 1v8a1 1 0 0 0 2 0V12A1 1 0 0 0 16 11zM16 23a1 1 0 1 0 1 1A1 1 0 0 0 16 23zm0 1z"/></g></svg>
    </div>
    <div>
        <#if errorCode?? && errorCode?has_content>
                    <#if errorMessage??>
                         <p class="text-center">${errorCode} : ${msg("${errorMessage}")}</p>
                    <#elseif message??>
                         <p class="text-center">${message.summary?no_esc}</p>
                    </#if>
                <#else>
                     <#if errorMessage??>
                           <p class="text-center">${msg("${errorMessage}")}</p>
                     <#elseif message??>
                           <p class="text-center">${message.summary?no_esc}</p>
                     </#if>
                </#if>
    </div>
    <#if tryAgain!true>
        <#if url.loginRestartFlowUrl??>
            <button id="nextBtn" type="button" onclick="location.href='${url.loginRestartFlowUrl}';" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
                ${msg("doTryAgain")}
            </button>
        </#if>
        <div class="col-12 social-login-seperator margin-t-2">
        <p class="text-center font-s-13 c-gray">${msg("or")}</p>
    </div>
    </#if>
    <div class="text-center margin-t-1 col-12 title_sign mb-0">
        <p> 
            <span class="help-text">${msg("help-msg")}</span>  
            <a href="${websiteContactLink}" target="_blank"> ${msg("contact-support")} </a>  
        </p>
    </div>
    </#if>
</@layout.registrationLayout>