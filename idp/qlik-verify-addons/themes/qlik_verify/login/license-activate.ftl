<#-- License Entering Page  -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form action="${url.loginAction}" class="row" method="post">
            <input name="from" type="hidden" value="license-activation">
            <div class="col-12">
                <div class="title_sign">
                    <h2>${msg("license-activation-title")}</h2>
                </div>
                <p>${msg("license-activation-desc")}</p>
            </div>
            <div class="col-12">
                <div class="form-group">
                    <label>${msg("license-activation-label")}</label>
                    <input id="license-key" name="license-key" type="text" class="form-control" placeholder="${msg("enter-license-key")}"
                           oninput="disableSubmit()" required>
                    <#import "error_msg.ftl" as e>
                    <@e.error_msg />
                </div>
            </div>

            <div class="col-12">
                <button id="nextBtn" type="submit" class="btn w-100 margin-t-3 btn_account bg-orange-red  " disabled="disabled" >
                    ${msg("license-activation-btn")}
                </button>
            </div>
            <div class="col-12 social-login-seperator margin-t-2">
                <p class="text-center font-s-13 c-gray">${msg("or")}</p>
            </div>
            <div class="text-center margin-t-1 col-12">
            <p>
                <span class="help-text">${msg("help-msg")}</span>  
                <a href="${msg("website-contact-link")}" target="_blank"> ${msg("contact-sales")} </a>  
            </p>
            </div> 
        </form>
    </#if>
    <script>
     function disableSubmit(){
        $('#be-error').hide();
        let licenseKey = $("#license-key").val();
        if( licenseKey ){
            $('#nextBtn').removeClass('disabled');
            $('#nextBtn').removeAttr('disabled');
        }
        else{
            $('#nextBtn').addClass('disabled');
            $('#nextBtn').attr('disabled', 'disabled');
        }
    }
    </script>
</@layout.registrationLayout>