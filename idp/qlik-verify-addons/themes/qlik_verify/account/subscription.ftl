<#import "template.ftl" as layout>
<@layout.mainLayout active='subscription' bodyClass='subscription'; section>
    <div class="element-wrapper">
        <#if cpDetails?? && cpDetails.url??>
            <div class="element-actions d-flex align-items-center">
                <div>
                    <a class="text-decoration-none btn btn-secondary mr-2" id="invoice-history-btn" target="_blank" href="${cpDetails.url}">
                        ${msg("invoice-history")}
                    </a>
                </div>
                <button id="subscription-submit-button" class="btn btn-primary text-uppercase d-none" name="submitAction" value="Save" onclick="submitForm()">
                    <span class="btn-loading-container">
                        <span class="spinner-border spinner-border-sm"></span>
                        ${msg("subscription_submitting")}
                    </span>
                    <span class="btn-text-container">
                        ${msg("subscription_preview_changes")}
                    </span>
                </button>
            </div>
        </#if>

        <h1 class="element-header mb-0">${msg("change-plan")}</h1>
    </div>
    <#if subs??  && !subs.isSubscriptionActive() && subs.message??>
        <div class="element-box mt-0">
            <div class="no-policy-img">
                <img src="${url.resourcesPath}/img/no-policy.svg"  alt="plan expired image" class="opacity-40 no-entry-display"/>
                <p class="no-results-msg no-entry-display">${msg(subs.message)}</p>
                <p class="no-results-msg no-entry-display">${kcSanitize(msg("subscription_update_msg"))?no_esc}</p>
            </div>
        </div>
    <#else>
        <#assign subs = subs.subs>
        <form id="upgrade-subscription-form" action="${url.subscriptionUrl}" method="post">
            <input type="hidden" id="previewInvoice" name="previewInvoice" value="previewInvoice">
            <#if subs ?? >
                <div id="products-list">
                    <#list subs as sub>
                        <input type="hidden" id="${sub.prodId}" name="${sub.prodId}" value="${sub.isSubscribed}">
                    </#list>
                </div>
            </#if>
            <div>
                <#--  subscribed cards list   -->
                <div class="products-top">
                    <div class="subscription-container">
                        <#if subs ?? >
                            <#list subs as sub>                    
                                <#if sub.consider?? && sub.consider == 'always'>
                                    <div class="subscription-plan-card ${sub.isSubscribed}">
                                        <#if sub.isSubscribed == 'true'>
                                            <strong class="badge badge-white position-absolute">
                                            ${msg("subscription_subscribed")}</strong>
                                        </#if>
                                        <div class="d-flex justify-content-between align-items-center">
                                            <div class="d-flex align-items-center">
                                                <img src="${sub.logo}" class="subscription-logo mr-2"/>
                                                <span class="subscription-title">
                                                ${sub.title!}
                                                </span>
                                            </div>
                                            <div class="d-flex justify-content-end">
                                                <div class="align-items-center justify-content-center d-flex subscription-card-locked">
                                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                                                    </svg>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="subscription-desc mt-3">
                                        ${sub.description}
                                        </div>
                                    </div>
                                    <#elseif sub.alternative??>
                                        <#if sub.prodId?starts_with("own")>
                                            <div id="card_${sub.prodId}" class="subscription-plan-card ${sub.isSubscribed}">
                                                <#if sub.isSubscribed == 'true'>
                                                    <strong class="badge badge-white position-absolute">${msg("subscription_subscribed")}</strong>
                                                </#if>
                                                <div class="d-flex justify-content-between align-items-center">
                                                    <div class="d-flex align-items-center">
                                                        <img src="${sub.logo}" class="subscription-logo mr-2"/>
                                                        <span class="subscription-title">
                                                        ${sub.title!}
                                                        </span>
                                                    </div>
                                                    <div class="d-flex justify-content-end">
                                                        <div class="pricing-tab d-flex align-items-center slide">
                                                            <div class="pricing-tab-trigger mr-2" id="${sub.title?lower_case}-default-provider" onclick="switchAlternative('${sub.prodId}','${sub.alternative}')">${msg("default-provider")}</div>
                                                            <div class="pricing-tab-trigger active pointer-none pr-4"  id="${sub.title?lower_case}-custom-provider" >${msg("own-provider")}</div>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="subscription-desc mt-3">
                                                    ${sub.description}
                                                </div>
                                            </div>
                                        <#else>
                                                <div id="card_${sub.prodId}" class="subscription-plan-card ${sub.isSubscribed}">
                                                    <#if sub.isSubscribed == 'true'>
                                                        <strong class="badge badge-white position-absolute">${msg("subscription_subscribed")}</strong>
                                                    </#if>
                                                    <div class="d-flex justify-content-between align-items-center">
                                                        <div class="d-flex align-items-center">
                                                            <img src="${sub.logo}" class="subscription-logo mr-2"/>
                                                            <span class="subscription-title">
                                                            ${sub.title!}
                                                            </span>
                                                        </div>
                                                        <#if sub.isAllowedToUpdate == 'true'>
                                                            <div class="d-flex justify-content-end">
                                                                <div class="pricing-tab d-flex align-items-center">
                                                                        <div class="pricing-tab-trigger active pl-3 pointer-none" id="${sub.title?lower_case}-default-provider" >${msg("default-provider")}</div>
                                                                        <div class="pricing-tab-trigger" onclick="switchAlternative('${sub.prodId}','${sub.alternative}')" id="${sub.title?lower_case}-custom-provider" >${msg("custom-provider")}</div>
                                                                </div>
                                                            </div>
                                                        <#else>
                                                        <div class="d-flex justify-content-end">
                                                            <div class="align-items-center justify-content-center d-flex subscription-card-locked">
                                                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                                                                </svg>
                                                            </div>
                                                        </div>
                                                        </#if> 
                                                    </div>
                                                    <#if sub.isAllowedToUpdate == 'true'>
                                                        <div class="subscription-desc mt-3">
                                                            ${sub.description}
                                                        </div>
                                                    <#else>
                                                         <div class="subscription-desc mt-3">
                                                            ${msg("free-quota-limit-exceeded")} <a href="https://support.ezto.io" id="qv-upport" target="_blank">${msg("support")}</a> ${msg("for-any-queries")}
                                                        </div>
                                                    </#if>
                                                </div>  
                                        </#if>
                                <#else>
                                        <div class="subscription-plan-card ${sub.isSubscribed}">
                                            <#if sub.isSubscribed == 'true'>
                                                <strong class="badge badge-white position-absolute">${msg("subscription_subscribed")}</strong>
                                            </#if>
                                            <div class="d-flex justify-content-between align-items-center">
                                                <div class="d-flex align-items-center">
                                                    <img src="${sub.logo}" class="subscription-logo mr-2"/>
                                                    <span class="subscription-title">
                                                    ${sub.title}
                                                    </span>
                                                </div>
                                                <div class="d-flex justify-content-end">
                                                    <div>
                                                        <label class="switch mb-0">
                                                            <#if sub.isAllowedToUpdate == 'false'>
                                                                <div class="d-flex justify-content-end">
                                                                    <div class="align-items-center justify-content-center d-flex subscription-card-locked">
                                                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                                                                        </svg>
                                                                    </div>
                                                                </div>
                                                            <#else>
                                                                <#if sub.isSubscribed?? && sub.isSubscribed == 'true'>
                                                                    <input id="switch_${sub.prodId}" id="${sub.title?lower_case}-subscribed" type="checkbox" checked>
                                                                <#else>
                                                                    <input id="switch_${sub.prodId}" id="${sub.title?lower_case}-unsubscribed" type="checkbox">
                                                                </#if>
                                                                <span class="slider round" id="${sub.title?lower_case}" onclick="switchBox('${sub.prodId}')"></span>
                                                            </#if>
                                                        </label>
                                                    </div>
                                                </div>
                                            </div>
                                            <#if sub.isAllowedToUpdate == 'false'>
                                                <div class="subscription-desc mt-3">
                                                    ${msg("currently-using-custom-domain")}
                                                </div>
                                            <#else>
                                                <div class="subscription-desc mt-3">
                                                    ${sub.description}
                                                </div>
                                            </#if>
                                        </div>
                                </#if>
                            </#list> 
                        <#else>
                        </#if>
                        <#if subs ?? >
                            <#list subs as sub>                    
                                <#if sub.consider?? && sub.consider == 'always'>
                                <#elseif sub.alternative??>
                                <#else>
                                        <div class="subscription-plan-card unsubscribed-card ${sub.isSubscribed}">
                                            <div class="d-flex justify-content-between align-items-center">
                                                <div class="d-flex align-items-center">
                                                    <img src="${sub.logo}" class="subscription-logo mr-2"/>
                                                    <span class="subscription-title">
                                                    ${sub.title}
                                                    </span>
                                                </div>
                                                <div class="d-flex justify-content-end">
                                                    <div>
                                                        <label class="switch mb-0">
                                                            <#if sub.isSubscribed?? && sub.isSubscribed == 'true'>
                                                                <input id="switch_${sub.prodId}" type="checkbox" checked>
                                                            <#else>
                                                                <input id="switch_${sub.prodId}" type="checkbox">
                                                            </#if>
                                                            <span class="slider round" id="${sub.title?lower_case}" onclick="switchBox('${sub.prodId}')"></span>
                                                        </label>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="subscription-desc mt-3">
                                                ${sub.description}
                                            </div>
                                        </div>
                                </#if>
                            </#list> 
                        <#else>
                        </#if>
                    </div>
                </div>
            </div>
        </form>
    </#if>
    <script defer type="text/javascript">
        let listOfProducts = document.querySelectorAll("#products-list input");
        let initialProductObject = {}
        listOfProducts.forEach((hiddenInput)=>{
            initialProductObject[hiddenInput.id] = hiddenInput.value;
        })
        function checkUpdate(){
            let updatedProducts = document.querySelectorAll("#products-list input");
            let changed = false;
            for(let updatedInput of updatedProducts){
                if(updatedInput.value !== initialProductObject[updatedInput.id]){
                    changed = true;
                    break;
                }
            }
            let submitButton = document.querySelector("#subscription-submit-button");
            if(changed && submitButton.classList.contains("d-none")){
                submitButton.classList.remove("d-none");
            }
            else if(!changed && !submitButton.classList.contains("d-none")){
                submitButton.classList.add("d-none");
            }
        }
        function switchBox(id){
            let element = document.querySelector("#"+id);
            if(element && element.value==="false"){
                element.value = "true"
            }else if(element && element.value==="true"){
                 element.value = "false"
            }
            checkUpdate();
        }
        function switchAlternative(currentId, alternativeId){
            let currentCard = document.querySelector("#card_"+currentId);
            let alternativeCard = document.querySelector("#card_"+alternativeId);
            let currentInput = document.querySelector("#"+currentId);
            let alternativeInput = document.querySelector("#"+alternativeId);
            currentInput.value = "false";
            currentCard.classList.toggle("false");
            alternativeInput.value = "true";
            alternativeCard.classList.toggle("false");
            checkUpdate();
        }
        function submitForm(){
            document.querySelector("#subscription-submit-button").classList.add("btn-submitted");
            document.querySelector('#upgrade-subscription-form').submit();
        }
    </script>
</@layout.mainLayout>