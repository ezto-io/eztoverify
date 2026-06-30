<#import "template.ftl" as layout>
<@layout.mainLayout active='subscription' bodyClass='subscription'; section>
    <#setting number_format=",##0.00">
    <div class="element-wrapper custom-invoice">
        <div class="invoice-w mx-auto p-5">
                  <div class="invoice-heading mt-0">
                    <h1 class="element-header mb-0"> ${msg("preview_invoice")} </h1>
                  </div>
                  <div class="infos my-5">
                    <div class="info-1 pt-0 text-left">
                      <div class="mb-3">
                        <div class="invoice-sub-heading">
                            ${msg("account_name")}
                        </div>
                        <div class="invoice-sub-head">
                            ${realm.getName()}
                        </div>
                      </div>
                      <div>
                        <div class="invoice-sub-heading">
                             ${msg("billed_to")}
                        </div>
                        <div>
                            ${customer_email!}
                        </div>
                      </div>
                    </div>
                    <div class="info-2 pt-0 text-left">
                      <div class="mb-3">
                        <div class="invoice-sub-heading">
                             ${msg("due_date")}
                        </div>
                        <div> 
                            ${(due_date?number * 1000)?number_to_date}
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="invoice-body">
                    <div class="invoice invoice-table w-100 mb-4">
                      <table class="table">
                        <thead>
                          <tr>
                            <th class="invoice-table-header table-description-cell-prop">
                              ${msg("description")}
                            </th>
                            <th class="text-right invoice-table-header table-unit-price-prop">
                              ${msg("unit_price")}
                            </th>
                            <th class="text-right price-width invoice-table-header invoice-amount-prop">
                              ${msg("amount")}
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                            <#import "group-list.ftl" as g>
                            <@g.group_list invoiceItems.data ; groupName, endDate, groupItems>
                              <table class="table">
                                 <tr>
                                    <span class="font-size-14 period-prop"> ${((groupName)?replace("," , "")?number * 1000)?number_to_date} - ${((endDate)?replace("," , "")?number * 1000)?number_to_date} </span>
                                  </tr>
                                <#list groupItems as groupItem>
                                  <tr>
                                      <td class="table-description-cell-prop"> <span class="font-size-14 invoice-desc-prop" > ${groupItem.description} </span> </td>
                                      <td class="text-right table-unit-price-prop"> <span class="font-size-14 invoice-desc-prop" > $${(groupItem.price.unit_amount!"000")?replace("," , "")?number / 100} </span> </td>
                                      <td class="text-right font-size-14 invoice-desc-prop">
                                        <#if 0 <= groupItem.amount >
                                          $${(groupItem.amount)?replace("," , "")?number / 100}
                                        <#else>
                                          -$${(groupItem.amount * -1)?replace("," , "")?number / 100}
                                        </#if>
                                      </td>
                                  </tr>
                                </#list>
                              </table>
                            </@g.group_list>
                        </tbody>
                      </table>
                      <div class="d-flex" >
                        <div class="w-50 font-size-14 invoice-desc-prop" >
                          ${msg("total")}
                        </div>
                        <div class="w-50 text-right font-size-14 total-cost-prop">
                          $${total?number / 100}
                        </div>
                      </div>
                    </div>
                  </div>
                   <form id="upgrade-subscription-form" action="${url.subscriptionUrl}" method="post">
                        <input type="hidden" id="updateSubscription" name="updateSubscription" value="updateSubscription">
                        <div class="d-flex justify-content-between">
                          <div>
                            <a href="${url.subscriptionUrl}" class="ng-binding" id="backToSubscription" >
                            <i class="os-icon os-icon-arrow-left6 breadcrumb-back-icon"></i>${msg("back_to_subscription")}
                            </a>
                          </div> 
                            <button id="submit-button" class="btn btn-primary w-50 text-uppercase py-1" name="submitAction" value="Save" onclick="subscribe()">
                              <span class="btn-loading-container">
                                <span class="spinner-border spinner-border-sm"></span>
                                ${msg("updating_subscription")}
                              </span>
                              <span class="btn-text-container">
                                ${msg("confirm_subscription")}
                              </span>
                            </button>
                        </div>   
                    </form>   
                  <div class="invoice-footer">
                    <div class="invoice-logo">
                      <img alt="" src="img/logo.png"><span>${account_name!}</span>
                    </div>
                    <div class="invoice-info">
                      <span>support@qlikverify.com</span>
                    </div>
                  </div>
                
        </div>          
    </div>
    <script>
      function subscribe(){
        document.querySelector("#submit-button").classList.add("btn-submitted");
        document.querySelector("#upgrade-subscription-form").submit();
      }
    </script>

</@layout.mainLayout>