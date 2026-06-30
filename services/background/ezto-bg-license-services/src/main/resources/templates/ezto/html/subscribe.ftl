<#include "general-index-license.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       Thank you for Subscription
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
                <div style="color: #636363; font-size: 14px;">
                    <div style="border-top: 1px solid rgba(0,0,0,0.05);">
                        <div style="color: #636363; font-size: 14px;"><p> Dear, <br><br> By subscribing you get exclusive news
                            on product improvements, new features etc. We promise you we dont spam or send any unwanted message.
                        </p></div>
                    </div>
                </div>
    </div>
    <#import "email-footer-license.ftl" as s>
    <@s.email_footer />
</#macro>
