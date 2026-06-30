<#include "general-index-license.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       Wallet Balance is Low
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
                <div style="color: #636363; font-size: 14px;">
                     <div style="border-top: 1px solid rgba(0,0,0,0.05);">
                                                    <div style="color: #636363; font-size: 14px;"><p> Dear, <br><br> Your wallet balance for <b>${tenantName}</b>
                                                        is gone below the threashold. Consider this email as a reminder to recharge your wallet for interrupted service </p></div>
                                                        <div style="color: #636363; font-size: 14px;"><p> Any queries or clarifications, please contact support</p></div>
                                                        <div style="color: #636363; font-size: 14px;">Your current account balance is : <h3>${balance} ${currency}</h3></div>
                                                </div>
                </div>
    </div>
    <#import "email-footer-license.ftl" as s>
    <@s.email_footer />
</#macro>
