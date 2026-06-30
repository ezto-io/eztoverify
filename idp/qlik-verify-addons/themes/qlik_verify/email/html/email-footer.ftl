<#macro email_footer>
    <table role="presentation" bgcolor="${footerColor}" width="100%" align="center" cellpadding="0" cellspacing="0" border="0" style="margin:0px auto;width:100%!important;min-width:100%!important">
        <tbody> 
            <tr> 
                <td style="padding-top:24px;padding-bottom:0px;padding-left:60px;padding-right:60px;font-family: 'Inter', sans-serif !important;font-size:10px;line-height:16px;color:#bdc4cf;text-align:center;font-weight:normal"> 
                    <div> 
                        <span style="text-decoration:none;color:#fff"><a href="${privacyPolicyLink}" style="text-decoration:none;color:#fff" target="_blank">${msg("policy")}</a>&nbsp;</span>&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                        <span style="text-decoration:none;color:#fff"><a href="${termsAndConditionLink}" style="text-decoration:none;color:#fff" target="_blank">${msg("terms-and-condition")}</a></span>
                        <#if docsLink?has_content>&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                        <a href="${docsLink}" style="text-decoration:none;color:#fff" target="_blank"><span style="text-decoration:none;color:#fff">${msg("docs")}</span></a></#if>
                    </div>
                </td> 
            </tr> 
        </tbody> 
    </table>
    <table role="presentation" bgcolor="${footerColor}" width="100%" align="center" cellpadding="0" cellspacing="0" border="0" style="margin:0px auto;width:100%!important;min-width:100%!important">
        <tbody> 
            <tr> 
                <td style="padding-top:24px;padding-bottom:0px;padding-left:60px;padding-right:60px;font-family: 'Inter', sans-serif !important;font-size:10px;line-height:16px;color:#fff;text-align:center;font-weight:normal"> 
                    <div>
                        ${msg("email-footer-info", variant)}
                        <span style="text-decoration:none;color:#fff"> ${kcSanitize(msg("visit-support", websiteContactLink))?no_esc}</span>.
                    </div>
                </td> 
            </tr> 
        </tbody> 
    </table>
    <table role="presentation" bgcolor="${footerColor}" width="100%" align="center" cellpadding="0" cellspacing="0" border="0" class="m_-604671424965511557tableCollapse" id="m_-604671424965511557FooterModule04" style="margin:0px auto;width:100%!important;min-width:100%!important">
        <tbody> 
            <tr> 
                <td style="padding-top:24px;padding-bottom:30px;padding-left:60px;padding-right:60px;font-family: 'Inter', sans-serif !important;font-size:10px;line-height:16px;color:#fff;text-align:center;font-weight:normal"> 
                <p class="text-center">&copy; <#if year??>${year}<#else>2021</#if> <a style="text-decoration-color: #fff;color:#fff;text-decoration:none" href="${websiteLink}"><strong>${variant}</strong>. </a> ${msg("rights-reserved")}</p>
                </td>
            </tr> 
        </tbody> 
    </table>
</#macro>
