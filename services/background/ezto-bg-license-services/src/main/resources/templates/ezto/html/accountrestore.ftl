<#include "general-index-license.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       Patch up is always good ❤️!! Pinky promise we dont break your heart
    </h1>
</#macro>
<#macro page_body>
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
                <div style="color: #636363; font-size: 14px;">
                            <div style="border-top: 1px solid rgba(0,0,0,0.05);">
                                <div style="color: #636363; font-size: 14px;"><p> Dear, <br><br> Thanks for restoring your <b>${tenantName}</b>
                                    account. Your account has been completely restored and it fully functional. </p></div>
                                <div style="border: 2px solid #4B72FA; padding: 10px; margin: 10px 0px;text-align: center;"><a
                                        href="${link}"
                                        style="padding: 8px 20px; background-color: #4B72FA; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">Take
                                    me to Dashboard</a></div>
                            </div>
                        </div>
    </div>
    <#import "email-footer-license.ftl" as s>
    <@s.email_footer />
</#macro>
