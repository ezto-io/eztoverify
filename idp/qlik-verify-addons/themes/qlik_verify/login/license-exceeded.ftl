<#-- License Exceeded Info -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section="form">
        <form id="add-money-form" action="${url.loginAction}" class="row" method="post" onkeydown="return preventEnterKey(event);">
            <input id="skip_add_money" name="skipAddMoney" value="false" type="hidden">
            <input id="pay_with_wallet" name="payWithWallet" value="false" type="hidden">
            <#if mode?? && mode == "bo">
                <div class="col-12">
                    <div class="title_sign my-2">
                        <h5>
                            <#if tenantBlockedTitle??>
                                ${msg('${tenantBlockedTitle}')}
                            <#else>
                                ${msg("action-required-title")}
                            </#if>
                        </h5>
                    </div>
                    <#if actionMsg??>
                        <p>${msg("${actionMsg}")}</p>
                    </#if>
                </div>
                <div class="col-12 mb-4" id="license-form-body">
                    <#if accConsoleUrl?? && accConsoleUrl.url??>
                        <div class="social-login-seperator my-1">
                            <p class="text-center font-s-13 c-gray">
                                ${msg("continue-with")}
                            </p>
                        </div>
                        <#if enablePayWithWallet?? && enablePayWithWallet>
                            <div class="my-1">
                                <p>
                                    ${msg("to-pay-with-wallet")} <a class="action-link-clr cursor-pointer" href="#" onclick="$('#pay_with_wallet').val('true');$('#add-money-form').submit();">
                                        ${msg("doClickHere")}
                                    </a></p>
                            </div>
                        </#if>
                        <div class="my-1">
                            <p>
                                ${msg("to-check-invoices")} 
                                <a class="action-link-clr cursor-pointer" href="${accConsoleUrl.url}" target="_blank">
                                    ${msg("doClickHere")}
                                </a>
                            </p>
                        </div>
                    </#if>
                    <#if paymentLink??>
                        <div class="my-1">
                            <div class="row d-none mb-3" id="add-money-input-field">
                                    <div class="col-9">
                                        <input type="number" autofocus="true" class="form-control py-1" id="moneyInput" oninput="handleInput()" value="500" placeholder="${msg('enter-amount-placeholder')}" />
                                    </div>
                                    <div class="col-3">
                                        <button type="button" id="addMoney" class="w-100 p-2 ciba-template-submitbtn text-center" onclick="addMoneyFunction()">${msg('add')}</button>
                                    </div>
                                    <small class="helper-text col-12">${msg('license-exceed-minimum-amount-error')}</small>
                            </div>
                            <p id="add-money-helper-link">${msg("stripe-payment-redirect")} <a class="action-link-clr cursor-pointer" onclick="proceedAddMoney()" target="_blank"> ${msg("doClickHere")}</a></p>
                        </div>
                    </#if>
                    <#if balance?? && balance=="zero_balance">
                        <div class="my-1">
                            ${msg("skip-to-dashboard")}
                            <a onclick="skipAddMoneyFunc()" class="action-link-clr cursor-pointer">
                                ${msg("doClickHere")}
                            </a>
                        </div>
                    </#if>
                </div>
                <div id="refresh-page-helper-txt" class="col-12 my-5 d-none justify-content-center align-items-center">
                    ${msg("refresh-after-money-added")}
                </div>
                <div class="text-center margin-t-1 col-12 title_sign mb-0">
                    <p> 
                        <span class="help-text">${msg("help-msg")}</span>  
                        <a href="${websiteContactLink}" target="_blank"> ${msg("contact-support")} </a>
                    </p>
                    <#if websiteContactEmail??>
                        <p> or 
                            <a href="${websiteContactEmail}" target="_blank"> ${msg("contact-email")} </a>
                        </p>
                    </#if>
                </div> 
            <#else>
                <div class="col-12">
                    <#if tenantBlockedTitle??>
                        <div class="title_sign">
                            <h2>
                                ${msg('${tenantBlockedTitle}')}
                            </h2>
                        </div>
                        <#else>
                            <div class="title_sign">
                                <h2>
                                    ${msg("action-required-title")}
                                </h2>
                            </div>
                    </#if>
                    <#if actionMsg??>
                        <p>
                            ${msg("${actionMsg}")}
                        </p>
                    </#if>
                </div>
                <#if accConsoleUrl?? && accConsoleUrl.url??>
                    <div class="col-12 social-login-seperator margin-t-2">
                        <p class="text-center font-s-13 c-gray">
                            ${msg("continue-with")}
                        </p>
                    </div>
                    <#if enablePayWithWallet?? && enablePayWithWallet>
                        <div class="col-12 margin-t-2 text-center">
                            <p>
                                ${msg("to-pay-with-wallet")} <a class="action-link-clr cursor-pointer" href="#" onclick="$('#pay_with_wallet').val('true');$('#add-money-form').submit();">
                                    ${msg("doClickHere")}
                                </a></p>
                        </div>
                    </#if>
                    <div class="col-12 margin-t-2 text-center">
                        <p>
                            ${msg("to-check-invoices")} <a class="action-link-clr cursor-pointer" href="${accConsoleUrl.url}" target="_blank">
                                ${msg("doClickHere")}
                            </a></p>
                    </div>
                </#if>
                <#if paymentLink??>
                    <div class="col-12 text-center">
                        <p>${msg("stripe-payment-redirect")} <a class="action-link-clr cursor-pointer" href="${paymentLink}" target="_blank"> ${msg("doClickHere")}</a></p>
                    </div>
                </#if>
                <#if balance?? && balance=="zero_balance">
                    <div class="col-12 text-center">
                        ${msg("skip-to-dashboard")}
                        <a onclick="skipAddMoneyFunc()" class="action-link-clr cursor-pointer">
                            ${msg("doClickHere")}
                        </a>
                    </div>
                </#if>
                <div class="text-center margin-t-1 col-12 title_sign mb-0">
                    <p> 
                      <span class="help-text">${msg("help-msg")}</span>  
                      <a href="${websiteContactLink}" target="_blank"> ${msg("contact-support")} </a>
                    </p>
                    <#if websiteContactEmail??>
                        <p> or 
                            <a href="${websiteContactEmail}" target="_blank"> ${msg("contact-email")} </a>
                        </p>
                    </#if>
                </div> 
            </#if>
        </form>
        <script>
            function skipAddMoneyFunc() {
                $("#skip_add_money").val("true");
                $("#add-money-form").submit();
            }
            function preventEnterKey(event) {
                if (event.key === "Enter") return false; 
                return true;
            }
            <#if mode?? && mode == "bo">
                function proceedAddMoney() {
                    $("#add-money-helper-link").addClass("d-none");
                    $("#add-money-input-field").removeClass("d-none");
                }

                function addMoneyFunction() {
                    const url = '${paymentLink}';  
                    const token = '${paymentToken}'; 

                    fetch(url + $("#moneyInput").val(), {
                        method: 'POST',
                        headers: {
                            'Authorization': "Bearer " + token
                        }
                    })
                    .then(response => {
                        return response.json();
                    })
                    .then(data => {
                        $("#license-form-body").addClass("d-none");
                        $("#refresh-page-helper-txt").removeClass("d-none");
                        $("#refresh-page-helper-txt").addClass("d-flex");
                        window.open(data.body.url, "_blank");
                    })
                    .catch(error => {
                        console.log(error)
                    });
                }

                function handleInput() {
                    if ($("#moneyInput").val() < 500) {
                       $("#addMoney").prop("disabled", true)
                    } else {
                        $("#addMoney").removeAttr("disabled")
                    }
                }
            </#if>
        </script>
    </#if>
</@layout.registrationLayout>