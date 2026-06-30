<#import "template.ftl" as layout>
<@layout.mainLayout active='sessions' bodyClass='sessions'; section>

    <div class="element-wrapper">
        <h1 class="element-header mb-0">${msg("sessionsHtmlTitle")}</h1>
    </div> 

    <div class="element-box">
        <form class="mb-4 pull-right" action="${url.sessionsUrl}" method="post">
            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
            <button id="logout-all-sessions" class="btn btn-secondary">${msg("doLogOutAllSessions")}</button>
        </form>
        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <td>${msg("ip")}</td>
                <td>${msg("started")}</td>
                <td>${msg("lastAccess")}</td>
                <td>${msg("expires")}</td>
                <td>${msg("clients")}</td>
            </tr>
            </thead>

            <tbody>
            <#list sessions.sessions as session>
                <tr>
                    <td>${session.ipAddress}</td>
                    <td>${session.started?datetime}</td>
                    <td>${session.lastAccess?datetime}</td>
                    <td>${session.expires?datetime}</td>
                    <td>
                        <#list session.clients as client>
                            ${client}<br/>
                        </#list>
                    </td>
                </tr>
            </#list>
            </tbody>

        </table>
        
    </div>

</@layout.mainLayout>
