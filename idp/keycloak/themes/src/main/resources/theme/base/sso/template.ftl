<#macro mainLayout active bodyClass>
<!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">

    <title>${msg("ssoManagementTitle")}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script type="text/javascript" src="${url.resourcesPath}/${script}"></script>
        </#list>
    </#if>
</head>
<body class="admin-console user ${bodyClass}">
        
    <header class="navbar navbar-default navbar-pf navbar-main header">
        <nav class="navbar" role="navigation">
            <div class="navbar-header">
                <div class="container">
                    <h1 class="navbar-title">QlikVerify</h1>
                </div>
            </div>
            <div class="navbar-collapse navbar-collapse-1">
                <div class="container">
                    <ul class="nav navbar-nav navbar-utility">
                        <#if referrer?has_content && referrer.url?has_content><li><a href="${referrer.url}" id="referrer">${msg("backTo",referrer.name)}</a></li></#if>
                        <li><a href="${url.logoutUrl}">${msg("doSignOut")}</a></li>
                    </ul>
                </div>
            </div>
        </nav>
    </header>

    <div class="container">
        <div class="search-content-area">

            <#nested "content">
        </div>
    </div>
    <script>
            <#if message?has_content>
                <#if message.type ='success'>toastr.success("${kcSanitize(message.summary)?no_esc}");</#if>
                <#if message.type ='error'>toastr.error("${kcSanitize(message.summary)?no_esc}");</#if>
            </#if>
           </script>
</body>
</html>
</#macro>