<#import "template.ftl" as layout>
<@layout.mainLayout active='applications' bodyClass='applications'; section>
    <div class="element-wrapper">
        <h1 class="element-header mb-0">${msg("applicationsHtmlTitle")}</h1>
    </div>  

    <div class="element-box">
        <form action="${url.applicationsUrl}" method="post">
            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
            <input type="hidden" id="referrer" name="referrer" value="${stateChecker}">

            <table class="table table-striped table-bordered">
                <thead>
                    <tr>
                        <td>${msg("application")}</td>
                        <td>${msg("availableRoles")}</td>
                        <td>${msg("grantedPermissions")}</td> 
                        <td>${msg("appType")}</td> 
                        <td>${msg("action")}</td> 
                    </tr>
                </thead>

                <tbody>
                    <#list applications.applications as application>
                        <tr>
                            <td>
                                <#if application.effectiveUrl?has_content><a href="${application.effectiveUrl}"></#if>
                                    <#if application.client.name?has_content>${advancedMsg(application.client.name)}<#else>${application.client.clientId}</#if>
                                <#if application.effectiveUrl?has_content></a></#if>
                            </td>

                            <td>
                                <#list application.realmRolesAvailable as role>
                                    <#if role.name??>${advancedMsg(role.name)}</#if>
                                    <#if role_has_next>, </#if>
                                </#list>
                                <#list application.resourceRolesAvailable?keys as resource>
                                    <#if application.realmRolesAvailable?has_content>, </#if>
                                    <#list application.resourceRolesAvailable[resource] as clientRole>
                                        <#if clientRole.roleDescription??>${advancedMsg(clientRole.roleDescription)}<#else>${advancedMsg(clientRole.roleName)}</#if>
                                        ${msg("inResource")} <strong><#if clientRole.clientName??>${advancedMsg(clientRole.clientName)}<#else>${clientRole.clientId}</#if></strong>
                                        <#if clientRole_has_next>, </#if>
                                    </#list>
                                </#list>
                            </td>

                            <td>
                                <#if application.client.consentRequired>
                                    <#list application.clientScopesGranted as claim>
                                        ${advancedMsg(claim)}<#if claim_has_next>, </#if>
                                    </#list>
                                <#else>
                                    <strong>${msg("fullAccess")}</strong>
                                </#if>
                            </td>
                            <td>
                                <strong>${application.clientAppType}</strong>
                            </td>
                            <td>
                                <#if (application.client.consentRequired && application.clientScopesGranted?has_content) || application.additionalGrants?has_content>
                                    <button type='submit' class='${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!}' id='revoke-${application.client.clientId}' name='clientId' value="${application.client.id}">${msg("revoke")}</button>
                                </#if>
                                <#if application.clientAppType == "iws" && application.client.attributes['iws.credtype'] == "iws_user_set_pass">
                                    <#if application.iwsAppEnabled?? && application.iwsAppEnabled == "true">
                                        <div class="d-flex flex-column">
                                            <p class="w-200">${msg("change-password")}</p>
                                            <div>
                                                <label class="switch mb-0">
                                                        <input type="checkbox" onchange="submitAction('${application.client.id}@revoke')">
                                                    <span class="slider round primary-slider"></span>
                                                </label>
                                            </div>
                                        </div>
                                    <#else>
                                        <div class="d-flex flex-column">
                                            <p class="w-200">${msg("change-password-onlogin")}</p>
                                            <div>
                                                <label class="switch mb-0">
                                                        <input type="checkbox" onchange="submitAction('${application.client.id}@change')">
                                                    <span class="slider round primary-slider"></span>
                                                </label>
                                            </div>
                                        </div>
                                    </#if>
                                </#if>
                            </td>
                        </tr>
                    </#list>
                </tbody>
            </table>
        </form>
    </div>
    <script>
        function submitAction(clientId){
            setTimeout(function(){
                $('form').append('<input type="hidden" name="clientId" id="clientId"/>');
                $('#clientId').val(clientId);
                $('form').submit();
            },100)
        }
    </script>

</@layout.mainLayout>