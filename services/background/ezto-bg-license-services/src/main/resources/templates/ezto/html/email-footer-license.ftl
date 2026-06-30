<#macro email_footer>
    <table role="presentation" bgcolor="#05173f" width="100%" align="center" cellpadding="0" cellspacing="0" border="0" style="margin:0px auto;width:100%!important;min-width:100%!important">
        <tbody>
            <tr>
                <td style="padding-top:24px;padding-bottom:0px;padding-left:60px;padding-right:60px;font-family: 'Inter', sans-serif !important;font-size:10px;line-height:16px;color:#bdc4cf;text-align:center;font-weight:normal">
                <#if privacyPolicyLink?? && termsAndConditionLink?? && docsLink??>
                    <div>
                        <span style="text-decoration:none;color:#fff"><a href="${privacyPolicyLink}" style="text-decoration:none;color:#fff" target="_blank">${msg("policy")}</a>&nbsp;</span>&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                        <span style="text-decoration:none;color:#fff"><a href="${termsAndConditionLink}" style="text-decoration:none;color:#fff" target="_blank">${msg("terms-and-condition")}</a></span>&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                        <a href="${docsLink}" style="text-decoration:none;color:#fff" target="_blank"><span style="text-decoration:none;color:#fff">${msg("docs")}</span></a>
                    </div>
                <#else>
                    <div>
                        <span style="text-decoration:none;color:#fff"><a href="${msg('privacy-policy-link')}" style="text-decoration:none;color:#fff" target="_blank">${msg("policy")}</a>&nbsp;</span>&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                        <span style="text-decoration:none;color:#fff"><a href="${msg('terms-and-condition-link')}" style="text-decoration:none;color:#fff" target="_blank">${msg("terms-and-condition")}</a></span>&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                        <a href="${msg('docs-link')}" style="text-decoration:none;color:#fff" target="_blank"><span style="text-decoration:none;color:#fff">${msg("docs")}</span></a>
                    </div>
                </#if>
                </td>
            </tr>
        </tbody>
    </table>
    <table role="presentation" bgcolor="#05173f" width="100%" align="center" cellpadding="0" cellspacing="0" border="0" style="margin:0px auto;width:100%!important;min-width:100%!important">
        <tbody>
            <tr>
                <td style="padding-top:24px;padding-bottom:0px;padding-left:60px;padding-right:60px;font-family: 'Inter', sans-serif !important;font-size:10px;line-height:16px;color:#fff;text-align:center;font-weight:normal">
                    <div>
                        ${msg("email-footer-info", variant! 'ezto verify')}
                        <#noautoesc>
                        <#if websiteContactLink??>
                            <span style="text-decoration:none;color:#fff"> ${kcSanitize(msg("visit-support", websiteContactLink))?no_esc}</span>.
                        <#else>
                            <span style="text-decoration:none;color:#fff"> ${kcSanitize(msg("visit-support",msg('website-contact-link')))}</span>.
                        </#if>
                         </#noautoesc>
                    </div>
                </td>
            </tr>
        </tbody>
    </table>
    <table role="presentation" bgcolor="#05173f" width="100%" align="center" cellpadding="0" cellspacing="0" border="0" class="m_-604671424965511557tableCollapse" id="m_-604671424965511557FooterModule04" style="margin:0px auto;width:100%!important;min-width:100%!important">
        <tbody>
            <tr>
                <td style="padding-top:24px;padding-bottom:30px;padding-left:60px;padding-right:60px;font-family: 'Inter', sans-serif !important;font-size:10px;line-height:16px;color:#fff;text-align:center;font-weight:normal">
                <#if variant?? && websiteLink??>
                    <p class="text-center">&copy; <#if year??>${year}<#else>2024</#if> <a style="text-decoration-color: #fff;color:#fff;text-decoration:none" href="${websiteLink}"><strong>${variant}</strong>. </a> ${msg("rights-reserved")}</p>
                <#else>
                    <p class="text-center">&copy; <#if year??>${year}<#else>2024</#if> <a style="text-decoration-color: #fff;color:#fff;text-decoration:none" href="${msg('website-link')}"><strong>${msg("product")}</strong>. </a> ${msg("rights-reserved")}</p>
                </#if>
                </td>
            </tr>
        </tbody>
    </table>
</#macro>
