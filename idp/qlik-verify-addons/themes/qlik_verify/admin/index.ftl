<!DOCTYPE html>
<html>
    <head>
        <title></title>

        <meta charset="utf-8">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="robots" content="noindex, nofollow">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <#if variant?? && variant == 'ezto verify'>
        <link rel="shortcut icon" href="${resourceUrl}/themes/img/eztovrfy-favicon.ico?version=${serverVersion}">
        <#elseif variant?? && variant == 'ezto auth'>
        <link rel="shortcut icon" href="${resourceUrl}/themes/img/favicon.ico?version=${serverVersion}">
        </#if>
        <link href="${resourceUrl}/themes/fonts/inter/Inter-Light.ttf" rel="ttf">
        <link href="${resourceUrl}/themes/fonts/inter/Inter-Medium.ttf" rel="ttf">
        <link href="${resourceUrl}/themes/fonts/inter/Inter-Regular.ttf" rel="ttf">
        <link href="${resourceUrl}/themes/css/vendors.css?version=${serverVersion}" rel="stylesheet">
        <link href="${resourceUrl}/themes/css/roles-group-tree.css?version=${serverVersion}" rel="stylesheet" id="tree-view-style-sheet">
        <link href="${resourceUrl}/themes/css/main.css?version=${serverVersion}" rel="stylesheet" id="dashboard-style-sheet">
        <link href="${resourceUrl}/themes/css/icon-fonts.css?version=${serverVersion}" rel="stylesheet">
        <link rel="stylesheet" href="${resourceUrl}/themes/vendor/flatpickr-4.6.13/flatpickr.min.css?version=${serverVersion}">
        <script src="${resourceUrl}/themes/vendor/flatpickr-4.6.13/flatpickr.min.js?version=${serverVersion}"></script>

        <script src="https://cdn.jsdelivr.net/npm/iso-639-1@3.1.2/build/index.min.js"></script>
        <script type="text/javascript">
            var authServerUrl = '${authServerUrl}';
            var authUrl = '${authUrl}';
            var consoleBaseUrl = '${consoleBaseUrl}';
            var resourceUrl = '${resourceUrl}';
            var masterRealm = '${masterRealm}';
            var resourceVersion = '${resourceVersion}';
            var serverVersion = '${serverVersion}';
            var licenseUrl = '${licenseUrl!''}';
            var docsUrl = '${docsUrl}';
            var dashboardUrl = '${dashboardUrl}';
            const variant = '${variant}';
        </script>
        <script type="text/javascript" src="${resourceUrl}/themes/js/main_vendors.js?version=${serverVersion}"></script>
        <script defer src="${authUrl}/js/keycloak.js?version=${resourceVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/app.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/controllers/realm.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/controllers/clients.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/controllers/users.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/controllers/groups.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/controllers/roles.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/loaders.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/services.js?version=${serverVersion}" type="text/javascript"></script>
        <!-- Authorization -->
        <script defer src="${resourceUrl}/js/authz/authz-app.js?version=${serverVersion}" type="text/javascriptprea"></script>
        <script defer src="${resourceUrl}/js/authz/authz-controller.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/js/authz/authz-services.js?version=${serverVersion}" type="text/javascript"></script>
        <link href="${resourceUrl}/themes/css/quill.snow.css" rel="stylesheet" />
        <script defer src="${resourceUrl}/themes/js/quill.js"></script>
    </head>
    <body
        class="menu-position-side menu-side-left full-screen with-content-panel" id="body-container" ng-init="init()" data-ng-controller="GlobalCtrl" data-ng-cloak data-ng-show="auth.user">
        <!-- Navabar Support -->
        <div data-ng-include data-src="resourceUrl + '/partials/menu.html'">
        </div>

         <!-- Main Layout  -->
        <div class="all-wrapper with-side-panel solid-bg-all">
            <div class="layout-w" data-ng-view id="view" autoscroll="true"></div>
            <div class="display-type"></div>
        </div>

         <!-- Notifications   -->
        <div class="feedback-aligner fadeInclass" data-ng-show="notification.display">
            <div class="alert alert-{{notification.type}} alert-dismissable pr-4">
                <button type="button" class="close" data-ng-click="notification.remove()" id="notification-close">
                    <span class="notification-close-icon os-icon os-icon-x"/>
                </button>
                <div class="icon-notifications">
                    <span class="os-icon os-icon-check-circle" ng-show="notification.type == 'success'"></span>
                    <span class="os-icon os-icon-alert-circle" ng-show="notification.type == 'info'"></span>
                    <span class="os-icon os-icon-alert-triangle" ng-show="notification.type == 'warning'"></span>
                    <span class="notification-icon picons-thin-icon-thin-0153_delete_exit_remove_close" ng-show="notification.type == 'danger'"></span>
                </div>
                <strong>{{notification.header}}</strong>
                <span ng-bind-html="notification.message"></span>
                
            </div>
        </div>
        
        <!-- Support URLs - Freshworks/Freshdesk support widget removed for OSS (proprietary SaaS, leaks to freshworks.com) -->

        <script defer src="${resourceUrl}/themes/js/vendors.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceCommonUrl}/node_modules/select2/select2.js?version=${serverVersion}" type="text/javascript"></script>
        <script defer src="${resourceUrl}/themes/js/main.js?version=${serverVersion}"></script>
        <script defer src="${resourceUrl}/themes/js/region-helper.js?version=${serverVersion}"></script>
        <script defer src="https://cdn.jsdelivr.net/npm/uuid-js@0.7.5/lib/uuid.min.js"></script>
        <#if variant?? && variant == 'ezto verify'>
            <script defer src="${resourceUrl}/themes/js/curl_converter.js?version=${serverVersion}" type="text/javascript"></script>
        </#if>
</body>  
</html>
