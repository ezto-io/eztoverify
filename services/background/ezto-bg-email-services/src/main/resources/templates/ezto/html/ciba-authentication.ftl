<!DOCTYPE html>
<html>
  <body dir="${dir}" style="padding: 20px; font-size: 14px; line-height: 1.43; font-family: 'Inter', sans-serif !important;">
    <div style="max-width: 600px; margin: 0px auto; background-color: #fff; box-shadow: 0px 20px 50px rgba(0,0,0,0.05); font-family: 'Inter', sans-serif !important;">
      <div style="padding: 60px 70px; border-top: 1px solid rgba(0,0,0,0.05);">
        <div style="color: #636363; font-size: 14px;">
                <div style="color: #636363; font-size: 14px;">
                    <p><#if emTitle??>${emTitle}</#if>  </p>
                </div>
                <div style="border: 2px solid ${bodyColor}; padding: 40px; margin: 40px 0px;">
                    <h4 style="margin-bottom: 20px; margin-top: 0px; font-size: 18px; display: inline-block;">
                    <#if emBody??>${emBody}</#if> 
                    </h4>
                    <div style="text-align: center">
                        <a href="<#if link??>${link}<#else>{{link}}</#if>" style="padding: 8px 20px; background-color: ${bodyColor}; color: #fff; font-weight: bolder; font-size: 16px; display: inline-block; margin: 10px 0px; text-decoration: none;">${msg("verify-transaction")}</a>
                    </div>
                     <table style="width: 100%;">
                        <tr>
                            <td style="font-size: 14px">
                                ${msg("link-expires-in")} <b><#if expirationTime??>${expirationTime} ${msg("${expirationUnit}")}<#else>{{expirationTime}} {{expirationUnit}}</#if>.</b>
                            </td>
                        </tr>
                    </table>
                </div>
                <#if variant == "ezto auth">
                  <p>${msg("happy-authenticating")}</p>
              <#elseif variant == "ezto verify">
                  <p>${msg("happy-verifying")}</p>
              </#if>
                <#import "email-footer.ftl" as s>
                <@s.email_footer />
        </div>
      </div>
    </div>
  </body>
</html>