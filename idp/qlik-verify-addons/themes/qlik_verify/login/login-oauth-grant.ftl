<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="oauth"; section>
    <#if section = "header">
        <#if client.name?has_content>
            ${msg("oauthGrantTitle",advancedMsg(client.name))}
        <#else>
            ${msg("oauthGrantTitle",client.clientId)}
        </#if>
    <#elseif section = "form">
        <div id="kc-oauth" class="content-area">
            

            <form class="form-actions" action="${url.oauthAction}" method="POST">
                <div class="title_sign mb-2">
                    <h2 class="mb-0" >${msg("oauthGrantRequest")} to ${oauth.clientName} ?</h2>
                </div>
                <#if oauth.clientRootScope??>
                    <p class="helper-text mb-3" >${oauth.clientRootScope.consentScreenText}</p>
                </#if>
                <div class="scope-overflow-control" >
                    <ul>
                        <#if oauth.clientScopesRequested?? && 0 < oauth.clientScopesRequested?size>
                            <h6 class="consent-madatory-title" >${msg("mandatory-desc")}</h6>
                            <#list oauth.clientScopesRequested as clientScope>
                                <li class="mb-3">
                                    <h6>${advancedMsg(clientScope.consentScreenText)}</h6>
                                        <#if clientScope.description??>
                                            <div>
                                                <small class="helper-text">${msg(clientScope.description)}</small>
                                            </div>
                                        </#if>
                                    <#if clientScope.isDefault = 'true'>
                                    </#if>
                                </li>
                            </#list>
                        </#if>
                    </ul>
                    <ul>
                        <#if oauth.optionalClientScopes?? && 0 < oauth.optionalClientScopes?size>
                            <h6 class="consent-madatory-title">${msg("option-desc")}</h6>
                            <#list oauth.optionalClientScopes as clientScope>
                                <li class="mb-3">
                                    <div class="checkbox">
                                        <input type="checkbox" name="${clientScope.id}" id="${clientScope.id}">
                                        <label for="${clientScope.id}"><h6 class="mb-0 scope-label">${advancedMsg(clientScope.consentScreenText)}</h6></label>
                                    </div>
                                    <#if clientScope.description??>
                                        <small class="helper-text">${msg(clientScope.description)}</small>
                                    </#if>
                                </li>
                            </#list>
                        </#if>
                    </ul>
                </div>
                <input type="hidden" name="code" value="${oauth.code}">
                <div class="${properties.kcFormGroupClass!}">
                    <div id="kc-form-options">
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                        </div>
                    </div>

                    <div id="kc-form-buttons" class="form-actions d-flex margin-t-3 tc-action flex-column">
                        <button class="btn w-30 btn_account bg-orange-red   w-100" name="accept" id="kc-login" type="submit">${msg("doAccept")}</button>
                        <button class="btn w-30 btn_account  btn-orange-border mt-3 w-100" name="cancel" id="kc-cancel" type="submit">${msg("doDecline")}</button>
                    </div>
                </div>
            </form>
            <div class="clearfix"></div>
        </div>
    </#if>
</@layout.registrationLayout>
