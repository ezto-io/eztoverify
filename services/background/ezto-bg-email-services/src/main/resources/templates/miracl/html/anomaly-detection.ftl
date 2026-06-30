<#include "general-index.ftl">
<#macro page_title>
    <h1 style="margin-top: 0px;">
        ${msg('anamoly-login')}
    </h1>
</#macro>
<#macro page_body> 
    <div style="padding: 10px 10px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
            <p>
                ${msg("dear-customer")} 
            </p>
            <p>
                <#if login_message??>${login_message}<#else>{{login_message}}</#if>
            </p>
        </div>
        <div style="border: 2px solid #005581; padding: 40px; margin: 40px 0px;">
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg('browser')} : <#if browser??>${browser}<#else>{{browser}}</#if>
            </h4>
            <br>
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg('operating-system')} : <#if os??>${os}<#else>{{os}}</#if>
            </h4>
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg("date-and-time")} : <#if date_and_time??>${date_and_time}<#else>{{date_and_time}}</#if>
            </h4>
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg('ip-address')} : <#if ip??>${ip}<#else>{{ip}}</#if>
            </h4>
            <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
             ${msg('ip-location')} (${msg('approximate')}) : <#if ip_loc??>${ip_loc}<#else>{{ip_loc}}</#if>
            </h4>
            <br/>
            <p>
                ${msg("ignore-msg")}
            </p>
            <p>
                ${msg("reset-password-desc")}
            </p>
            <div style="text-align: center">
                <a href="<#if reset_password??>${reset_password}<#else>{{reset_password}}</#if>" style="padding: 8px 20px; background-color: #005581; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("reset-password")} </a> 
            </div>
        </div>
        <#import "email-footer.ftl" as s>
        <@s.email_footer />
    </div>
</#macro>