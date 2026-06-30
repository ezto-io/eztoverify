<#import "template.ftl" as layout>
<@layout.mainLayout active='subscription' bodyClass='subscription'; section>
    <div class="element-wrapper custom-invoice">
        <div class="invoice-w mx-auto p-5 d-flex justify-content-center align-items-center flex-column text-center">
            <#if purchaseStatus == "success">
                <img src="${url.resourcesPath}/img/success.svg" class="status-image-prop" />
            <#else>
                <img src="${url.resourcesPath}/img/failure.svg" class="status-image-prop" />
            </#if>
            <div class="status-primary-header-prop font-weight-normal font-size-17 mt-4" >
                ${msg( purchaseStatus + "_primary_header")}
            </div>
            <div class="status-secondary-header-prop font-weight-normal mt-3" >
                ${msg( purchaseStatus + "_secondary_header")}
            </div>
            <div class="mt-4">
                <button class="btn btn-primary text-uppercase" onclick="redirectToSubscription()" >
                    ${msg( "back_to_subscription" )}
                </button>
            </div>
            <div class="mt-3" >
                <#if purchaseStatus == "success">
                    <span class="status-redirect-txt-prop">  ${msg( "redirect_text" )} <span id="countdown">5</span> ${msg("seconds")} .  </span>
                <#else>
                    <div class="status-redirect-txt-prop">
                        ${msg("get-support")} <a href="${msg("website-contact-link")}" target="_blank" > ${msg("click-here")} </a>.
                    </div>
                </#if>
            </div>                
        </div>          
    </div>
    <script>
        <#if purchaseStatus == "success">
            var timeleft = 5;
            var downloadTimer = setInterval(function(){
            if(timeleft <= 0){
                clearInterval(downloadTimer);
                redirectToSubscription()
            } else {
                document.getElementById("countdown").innerHTML = timeleft;
            }
            timeleft -= 1;
            }, 1000);
        </#if>
        function redirectToSubscription(){
           window.location.href="${url.subscriptionUrl}"
        }
    </script>
</@layout.mainLayout>