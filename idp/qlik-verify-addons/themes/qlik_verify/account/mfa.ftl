<#import "template.ftl" as layout>
<@layout.mainLayout active='mfa' bodyClass='mfa'; section>
    <div class="element-wrapper">
        <h1 class="element-header mb-0">${msg("mfaHtmlTitle")}</h1>
    </div>
            <#if mf.mfas?size gt 0>
            <div class="element-box">
            <table class="table table-bordered table-striped">
                <thead>
                    <tr>
                        <td>${msg("factors")}</td>
                        <td>${msg("actions")}</td>
                    </tr>
                </thead>
                <tbody>
                <#list mf.mfas as mfa>
                    <tr>
                        <td class="provider">${msg("${mfa.displayName!}")}</td>
                        <td class="action">
                            <form action="${url.mfaUrl}" method="post" class="form-inline">
                                <input type="hidden" id="submitAction" name="submitAction" value="Delete">
                                <input type="hidden" id="credId" name="credentialId" value="${mfa.credentialId}">
                                <input type="hidden" id="providerId" name="providerId" value="${mfa.providerId}">
                                <button id="remove-mobile" class="btn btn-danger">
                                    ${msg("remove")}
                                </button>
                            </form>
                        </td>
                    </tr>
                </#list>
                </tbody>
            </table>
            </div>
            <#else>
                <div class="element-box mt-4 ng-scope">
                            <div class="no-policy-img">
                                <i class="os-icon os-icon-fingerprint opacity-75 no-entry-display mb-3" style="font-size: 50px;color: #4CA355; display: inline-block; vertical-align: middle;"></i>
                                <p class="text-center no-entry-display ng-binding">${msg("no-mfa-configured")}
                                </p>
                            </div>
                        </div>
            </#if>

</@layout.mainLayout>
