<#include "general-index-license.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
       Its hard than a breakup 💔!!
    </h1>
</#macro>
<#macro page_body>
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
                <div style="border-top: 1px solid rgba(0,0,0,0.05);">
                                <div style="color: #636363; font-size: 14px;"><p> Dear, <br><br> This is a confirmation that your <b>${tenantName}</b>
                                    account has been removed at your request. </p>
                                    <p> The good news is complete account will be deleted from system by <b>${deleteTime}</b>, still
                                        you have time to restore your account . Contact <a href="https://support.qlikverify.com"
                                                                                           style="text-decoration: underline; color: #4B72FA;">support</a>
                                        to restore. </p>
                                    <p> Did you expect something different or was it missing something you needed? </p>
                                    <p> Your feedback really helps us improve would be great to hear from you. </p></div>
                                <div style="border: 2px solid #4B72FA; padding: 10px; margin: 10px 0px;text-align: center;"><a
                                        href="${googleLink}"
                                        style="padding: 8px 20px; background-color: #4B72FA; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">Feedback</a>
                                </div>
                            </div>
    </div>
    <#import "email-footer-license.ftl" as s>
    <@s.email_footer />
</#macro>
