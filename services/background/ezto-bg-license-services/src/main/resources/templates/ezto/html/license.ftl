<#include "general-index-license.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       Thanks for evaluating ${product}!
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
                <div style="color: #636363; font-size: 14px;">
                    <div style="border-top: 1px solid rgba(0,0,0,0.05);">
                        <div style="color: #636363; font-size: 14px;"><p> Hi ${adminEmail}, Please find the license activation key below to
                            activate your ${tenantName} workspace. </p></div>
                        <div style="border: 2px solid #4B72FA; padding: 10px; margin: 10px 0px;text-align: center;"><h3
                                style="font-weight: bold;color: #4B72FA;">${licenseKey}</h3></div>
                    </div>
                </div>
    </div>
    <#import "email-footer-license.ftl" as s>
    <@s.email_footer />
</#macro>
