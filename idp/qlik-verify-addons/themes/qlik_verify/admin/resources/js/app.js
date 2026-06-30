'use strict';
var auth = {};
var resourceBundle;
var locale = 'en';
var newTcVersion = "";
var fileName = ""
var updatedAt = "";
var translateProvider = null;
var isCustomDropDownVisible = false;

var module = angular.module('keycloak', ['keycloak.services', 'keycloak.loaders', 'ui.bootstrap', 'ui.select2', 'angularFileUpload', 'angularTreeview', 'pascalprecht.translate', 'ngCookies', 'ngSanitize', 'ui.ace', 'angular-slideout-panel']);
var resourceRequests = 0;
var loadingTimer = -1;

var loadResourceBundle = (url, success, error) => {
    var req = new XMLHttpRequest();
    req.open('GET', url, true);
    req.setRequestHeader('Accept', 'application/json');

    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var data = JSON.parse(req.responseText);
                success && success(data);
            } else {
                error && error();
            }
        }
    };

    req.send();
}

angular.element(document).ready(function () {
    var keycloakAuth = new Keycloak(consoleBaseUrl + 'config');

    function whoAmI(success, error) {
        let realmName = document?.URL?.split('#')[1]?.split('/')[2];
        var req = new XMLHttpRequest();
        let finalStr = realmName ? '?realm=' + realmName : '';
        req.open('GET', consoleBaseUrl + 'whoami' + finalStr, true);
        req.setRequestHeader('Accept', 'application/json');
        req.setRequestHeader('Authorization', 'bearer ' + keycloakAuth.token);

        req.onreadystatechange = function () {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    var data = JSON.parse(req.responseText);
                    success(data);
                } else {
                    error();
                }
            }
        };

        req.send();
    }

    function hasAnyAccess(user) {
        return user && user['realm_access'];
    }

    keycloakAuth.onAuthLogout = function () {
        location.reload();
    };

    keycloakAuth.init({ onLoad: 'login-required', pkceMethod: 'S256' }).success(function () {
        auth.authz = keycloakAuth;

        if (auth.authz.idTokenParsed.locale) {
            locale = auth.authz.idTokenParsed.locale;
        }

        auth.refreshPermissions = function (success, error) {
            whoAmI(function (data) {
                auth.user = data;
                auth.loggedIn = true;
                auth.hasAnyAccess = hasAnyAccess(data);

                success();
            }, function () {
                error();
            });
        };

        auth.refreshPermissions(function () {
            module.factory('Auth', function ($translate) {
                locale = auth.user?.locale;
                loadResourceBundle(consoleBaseUrl + 'messages.json?lang='+ auth.user?.locale,function (data) {
                    resourceBundle = data;
                    const style = document.createElement("link");
                    style.rel = "stylesheet"
                    style.id = 'dashboard-rtl-style-sheet'
                    const ltrStyle = document.getElementById("dashboard-style-sheet")
                    if(locale == 'ar'){
                        $('#language-switching-loader').css("display", "flex");
                        fetch(`${resourceUrl}/themes/css/main-rtl.css?version=${serverVersion}`)
                            .then(response => response.text())
                            .then(css => {
                                style.href = 'data:text/css;charset=utf-8,' + encodeURIComponent(css);
                                style.addEventListener('load', () => {
                                    ltrStyle.remove();
                                    document.body.dir="rtl";
                                    $('#language-switching-loader').css("display", "none");
                                });
                                document.head.appendChild(style);
                            })
                            .catch(error => {
                                $('#language-switching-loader').css("display", "none");
                                console.error('Error fetching CSS:', error)
                            });
                        document.getElementById('tree-view-style-sheet').href = `${resourceUrl}/themes/css/roles-group-tree-rtl.css?version=${serverVersion}`;
                        $('#launcher-frame').css('left', '22px');
                        $('#launcher-frame').css('right', 'initial');
                    }
                    translateProvider.preferredLanguage(locale);
                    translateProvider.translations(locale, resourceBundle);
                    $translate.use(locale);
                });
                return auth;
            });
            var injector = angular.bootstrap(document, ["keycloak"]);
        });
    }).error(function () {
        window.location.reload();
    });
});

module.constant('contentConfig', {
    high_secure: 'high_secure',
    browser_based: 'browser_based',
    mappers: 'mappers',
    connections: 'connections',
    flow_override: 'flow-override'
});


module.factory('authInterceptor', function ($q, Auth) {
    return {
        request: function (config) {
            if (!config.url.match(/.html$/)) {
                var deferred = $q.defer();
                if (Auth.authz.token) {
                    Auth.authz.updateToken(5).success(function () {
                        config.headers = config.headers || {};
                        config.headers.Authorization = 'Bearer ' + Auth.authz.token;
                        config.headers.isApiRequest = 'true';
                        deferred.resolve(config);
                    }).error(function () {
                        location.reload();
                    });
                }
                return deferred.promise;
            } else {
                return config;
            }
        }
    };
});

module.config(['$translateProvider', function ($translateProvider) {
    translateProvider = $translateProvider;
    $translateProvider.useSanitizeValueStrategy('sanitizeParameters');
    $translateProvider.preferredLanguage(locale);
    $translateProvider.translations(locale, resourceBundle);
}]);

// Change for upgrade to AngularJS 1.6
// See https://github.com/angular/angular.js/commit/aa077e81129c740041438688dff2e8d20c3d7b52
module.config(['$locationProvider', function ($locationProvider) {
    $locationProvider.hashPrefix('');
}]);

module.config(['$routeProvider', function ($routeProvider) {
    $routeProvider
        .when('/realms/:realm', {
            templateUrl: resourceUrl + `/partials/${variant === "ezto verify" ? "bo-" : ""}getting-started.html`,
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GettingStartCtrl'
        })
        .when('/realms/:realm/insights', {
            templateUrl: resourceUrl + '/partials/realm-activity.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                activity: function (ActivityLoader) {
                    return ActivityLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmActivityCtrl'
        })
        .when('/realms/:realm/bo/insights', {
            templateUrl: resourceUrl + '/partials/realm-bo-activity.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmBoActivityCtrl'
        })
        .when('/realms/:realm/realm-settings', {
            templateUrl: resourceUrl + '/partials/realm-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmDetailCtrl'
        })
        .when('/realms/:realm/management/:name/api-providers', {
            templateUrl: resourceUrl + '/partials/api-providers-verifiers.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ApiProviders'
        })
        .when('/realms/:realm/management/:subRealm/products', {
            templateUrl: resourceUrl + '/partials/realm-products.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                subRealm: function (SubRealmLoader) {
                    return SubRealmLoader();
                }
            },
            controller: 'RealmProducts'
        })
        .when('/realms/:realm/realm-access-keys', {
            templateUrl: resourceUrl + '/partials/realm-access-keys.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'RealmAccessKeysCtrl'
        })
        .when('/realms/:realm/realm-access-keys/:accessKey', {
            templateUrl: resourceUrl + '/partials/realm-access-key.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                accessKey: function (AccessKeyLoader) {
                    return AccessKeyLoader();
                }
            },
            controller: 'RealmAccessKeyCtrl'
        })
        .when('/realms/:realm/products', {
            templateUrl: resourceUrl + '/partials/realm-products.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'RealmProducts'
        })
        .when('/realms/:realm/product/create', {
            templateUrl: resourceUrl + '/partials/realm-product-create.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'RealmCreateProduct'
        })
        .when('/realms/:realm/products/:subRealm/:productId', {
            templateUrl: resourceUrl + '/partials/realm-product-pricing.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                subRealm: function (SubRealmLoader) {   
                    return SubRealmLoader();
                },
        },
        controller: 'RealmProductPricing'
        })
        .when('/realms/:realm/products/:subRealm/:productId/price/:priceId', {
            templateUrl: resourceUrl + '/partials/realm-pricing.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                subRealm: function (SubRealmLoader) {   
                    return SubRealmLoader();
                }
            },
        controller: 'RealmPricing'
        })
        .when('/realms/:realm/management', {
            templateUrl: resourceUrl + '/partials/realm-management.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'RealmListCtrl'
        })
         .when('/realms/:realm/management/:name', {
            templateUrl: resourceUrl + '/partials/realm-management-name.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmManagementName'
        })
        .when('/realms/:realm/realm-attributes', {
            templateUrl: resourceUrl + '/partials/realm-special-attributes.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmSpecialAttributesCtrl'
        })
        .when('/realms/:realm/realm-attributes/:realmWorkspaceAttribute', {
            templateUrl: resourceUrl + '/partials/realm-special-attributes.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                realmWorkspaceAttribute: function (RealmWorkspaceAttributeLoader) {
                    return RealmWorkspaceAttributeLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmSpecialAttributesCtrl'
        })
        .when('/realms/:realm/realm-subscription', {
            templateUrl: resourceUrl + '/partials/realm-subscription.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmSubscriptionCtrl'
        })
        .when('/realms/:realm/realm-subscription/update-plan', {
            templateUrl: resourceUrl + '/partials/update-realm-subscription.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmUpdatePlanCtrl'
        })
        .when('/realms/:realm/login-settings', {
            templateUrl: resourceUrl + '/partials/realm-login-settings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmLoginSettingsCtrl'
        })
        .when('/realms/:realm/onboarding-settings', {
            templateUrl: resourceUrl + '/partials/realm-onboarding-settings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmOnboardingSettingsCtrl'
        })
        .when('/realms', {
            templateUrl: resourceUrl + '/partials/realm-list.html',
            resolve: {
                realm: function () {
                    return "";
                }
            },
            controller: 'RealmListCtrl'
        })
        .when('/realms/:realm/token-settings', {
            templateUrl: resourceUrl + '/partials/realm-tokens.html',
            resolve: {
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'RealmTokenDetailCtrl'
        })
        .when('/realms/:realm/dashboard-admins', {
            templateUrl: resourceUrl + '/partials/dashboard-admins.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'AdminListCtrl'
        })
        .when('/realms/:realm/security-settings', {
            templateUrl: resourceUrl + '/partials/security-settings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'TestModeCtrl'
        })
        .when('/realms/:realm/authentication-settings', {
            templateUrl: resourceUrl + '/partials/authentication-settings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                onboardingFlows: function (AuthenticationInstalledOnboardingFlowsLoader) {
                    return AuthenticationInstalledOnboardingFlowsLoader();
                },
                loginFlows: function (AuthenticationInstalledLoginFlowsLoader) {
                    return AuthenticationInstalledLoginFlowsLoader();
                },
                recoveryFlows: function (AuthenticationInstalledRecoveryFlowsLoader) {
                    return AuthenticationInstalledRecoveryFlowsLoader();
                },
                actions: function (ActionListLoader) {
                    return ActionListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                actionPolicies: function (ActionPolicyListLoader) {
                    return ActionPolicyListLoader();
                },
                cibaPolicies: function (CibaPolicyListLoader) {
                    return CibaPolicyListLoader();
                },
                directGrantFlows: function (AuthenticationInstalledDirectGrantFlowsLoader) {
                    return AuthenticationInstalledDirectGrantFlowsLoader();
                },
            },
            controller: 'RealmAuthenticationSettings'
        })
        // .when('/realms/:realm/client-registration/client-initial-access/create', {
        //     templateUrl : resourceUrl + '/partials/client-initial-access-create.html',
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         }
        //     },
        //     controller : 'ClientInitialAccessCreateCtrl'
        // })
        // .when('/realms/:realm/client-registration/client-reg-policies/create/:componentType/:providerId', {
        //     templateUrl : resourceUrl + '/partials/client-reg-policy-detail.html',
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         },
        //         instance : function($route) {
        //             return {
        //                 providerType: 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy',
        //                 subType: $route.current.params.componentType,
        //                 providerId: $route.current.params.providerId
        //             };
        //         },
        //         clientRegistrationPolicyProviders : function(ClientRegistrationPolicyProvidersLoader) {
        //             return ClientRegistrationPolicyProvidersLoader();
        //         }
        //     },
        //     controller : 'ClientRegPolicyDetailCtrl'
        // })
        // .when('/realms/:realm/client-registration/client-reg-policies/:provider/:componentId', {
        //     templateUrl : resourceUrl + '/partials/client-reg-policy-detail.html',
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         },
        //         instance : function(ComponentLoader) {
        //             return ComponentLoader();
        //         },
        //         clientRegistrationPolicyProviders : function(ClientRegistrationPolicyProvidersLoader) {
        //             return ClientRegistrationPolicyProvidersLoader();
        //         }
        //     },
        //     controller : 'ClientRegPolicyDetailCtrl'
        // })
        .when('/create/keys/:realm/providers/:provider', {
            templateUrl: resourceUrl + '/partials/realm-keys-generic.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function () {
                    return {
                    };
                },
                providerId: function ($route) {
                    return $route.current.params.provider;
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GenericKeystoreCtrl'
        })
        .when('/realms/:realm/keys/providers/:provider/:componentId', {
            templateUrl: resourceUrl + '/partials/realm-keys-generic.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function (ComponentLoader) {
                    return ComponentLoader();
                },
                providerId: function ($route) {
                    return $route.current.params.provider;
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GenericKeystoreCtrl'
        })
        .when('/create/identity-provider/:realm/:provider_id', {
            templateUrl: function (params) { return resourceUrl + '/partials/realm-identity-provider-' + params.provider_id + '.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance: function (IdentityProviderLoader) {
                    return {};
                },
                providerFactory: function (IdentityProviderFactoryLoader) {
                    return new IdentityProviderFactoryLoader();
                },
                authFlows: function (AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                }
            },
            controller: 'RealmIdentityProviderCtrl'
        })
        .when('/realms/:realm/identity-provider-settings/provider/:provider_id/:alias', {
            templateUrl: function (params) { return resourceUrl + '/partials/realm-identity-provider-' + params.provider_id + '.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance: function (IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                providerFactory: function (IdentityProviderFactoryLoader) {
                    return IdentityProviderFactoryLoader();
                },
                authFlows: function (AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                }
            },
            controller: 'RealmIdentityProviderCtrl'
        })
        .when('/realms/:realm/identity-provider-settings/provider/:provider_id/:alias/export', {
            templateUrl: resourceUrl + '/partials/realm-identity-provider-export.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                identityProvider: function (IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                providerFactory: function (IdentityProviderFactoryLoader) {
                    return IdentityProviderFactoryLoader();
                }
            },
            controller: 'RealmIdentityProviderExportCtrl'
        })
        .when('/realms/:realm/identity-provider-mappers/:alias/mappers', {
            templateUrl: function (params) { return resourceUrl + '/partials/identity-provider-mappers.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                identityProvider: function (IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                mapperTypes: function (IdentityProviderMapperTypesLoader) {
                    return IdentityProviderMapperTypesLoader();
                },
                mappers: function (IdentityProviderMappersLoader) {
                    return IdentityProviderMappersLoader();
                }
            },
            controller: 'IdentityProviderMapperListCtrl'
        })
        .when('/realms/:realm/identity-provider-mappers/:alias/mappers/:mapperId', {
            templateUrl: function (params) { return resourceUrl + '/partials/identity-provider-mapper-detail.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                identityProvider: function (IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                mapperTypes: function (IdentityProviderMapperTypesLoader) {
                    return IdentityProviderMapperTypesLoader();
                },
                mapper: function (IdentityProviderMapperLoader) {
                    return IdentityProviderMapperLoader();
                }
            },
            controller: 'IdentityProviderMapperCtrl'
        })
        .when('/create/identity-provider-mappers/:realm/:alias', {
            templateUrl: function (params) { return resourceUrl + '/partials/identity-provider-mapper-detail.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                identityProvider: function (IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                mapperTypes: function (IdentityProviderMapperTypesLoader) {
                    return IdentityProviderMapperTypesLoader();
                }
            },
            controller: 'IdentityProviderMapperCreateCtrl'
        })

        // .when('/realms/:realm/default-roles', {
        //     templateUrl : resourceUrl + '/partials/realm-default-roles.html',
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         },
        //         roles : function(RoleListLoader) {
        //             return RoleListLoader();
        //         }
        //     },
        //     controller : 'RealmDefaultRolesCtrl'
        // })
        .when('/realms/:realm/sms/providers', {
            templateUrl: resourceUrl + '/partials/realm-sms.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                smsConfig: function (RealmSmsConfigLoader) {
                    return RealmSmsConfigLoader();
                }
            },
            controller: 'RealmSmsProvidersCtrl'
        })
        .when('/realms/:realm/sms/providers/create', {
            templateUrl: resourceUrl + '/partials/create-sms-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                smsConfig: function (RealmSmsConfigLoader) {
                    return RealmSmsConfigLoader();
                }
            },
            controller: 'CreateSmsProvidersCtrl'
        })
        .when('/realms/:realm/sms/providers/create/:provider_id', {
            templateUrl: resourceUrl + '/partials/realm-sms-provider.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                smsConfig: function (RealmSmsConfigLoader) {
                    return RealmSmsConfigLoader();
                },
            },
            controller: 'SmsProviderCtrl'
        })
        .when('/realms/:realm/sms/providers/settings/:provider_id', {
            templateUrl: resourceUrl + '/partials/realm-sms-provider.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                smsConfig: function (RealmSmsConfigLoader) {
                    return RealmSmsConfigLoader();
                },
            },
            controller: 'SmsProviderCtrl'
        })
        .when('/realms/:realm/email/providers', {
            templateUrl: resourceUrl + '/partials/realm-smtp.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                emailConfig: function (RealmEmailConfigLoader) {
                    return RealmEmailConfigLoader();
                }
            },
            controller: 'RealmEmailProvidersCtrl'
        })
        .when('/realms/:realm/email/providers/create', {
            templateUrl: resourceUrl + '/partials/create-email-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                emailConfig: function (RealmEmailConfigLoader) {
                    return RealmEmailConfigLoader();
                }
            },
            controller: 'CreateEmailProvidersCtrl'
        })
        .when('/realms/:realm/email/providers/create/:provider_id', {
            templateUrl: resourceUrl + '/partials/realm-email-provider.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                emailConfig: function (RealmEmailConfigLoader) {
                    return RealmEmailConfigLoader();
                },
            },
            controller: 'EmailProviderCtrl'
        })
        .when('/realms/:realm/email/providers/settings/:provider_id', {
            templateUrl: resourceUrl + '/partials/realm-email-provider.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                emailConfig: function (RealmEmailConfigLoader) {
                    return RealmEmailConfigLoader();
                },
            },
            controller: 'EmailProviderCtrl'
        })
        .when('/realms/:realm/email/templates', {
            templateUrl: resourceUrl + '/partials/email-templates.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                emailTemplates: function (EmailTemplatesLoader) {
                    return EmailTemplatesLoader();
                }
            },
            controller: 'RealmEmailTemplatesCtrl'
        })
        .when('/realms/:realm/reports', {
            templateUrl: resourceUrl + '/partials/realm-event-reports.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                eventsConfig: function (RealmEventsConfigLoader) {
                    return RealmEventsConfigLoader();
                },
            },
            controller: 'RealmEventReportsCtrl'
        })
        .when('/realms/:realm/api-logs', {
            templateUrl: resourceUrl + '/partials/realm-api-logs.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                eventsConfig: function (RealmEventsConfigLoader) {
                    return RealmEventsConfigLoader();
                }
            },
            controller: 'RealmApiLogsCtrl',
            pricing: false
        })
        .when('/realms/:realm/pricing-logs', {
                    templateUrl: resourceUrl + '/partials/realm-api-logs.html',
                    resolve: {
                        realm: function (RealmLoader) {
                            return RealmLoader();
                        },
                        serverInfo: function (ServerInfoLoader) {
                            return ServerInfoLoader();
                        },
                        eventsConfig: function (RealmEventsConfigLoader) {
                            return RealmEventsConfigLoader();
                        }
                    },
                    controller: 'RealmApiLogsCtrl',
                    pricing: true
                })
        .when('/realms/:realm/logs', {
            templateUrl: resourceUrl + '/partials/realm-events-config.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                eventsConfig: function (RealmEventsConfigLoader) {
                    return RealmEventsConfigLoader();
                }
            },
            controller: 'RealmEventsConfigCtrl'
        })
        .when('/realms/:realm/admin-logs', {
            templateUrl: resourceUrl + '/partials/realm-events-admin.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                eventsConfig: function (RealmEventsConfigLoader) {
                    return RealmEventsConfigLoader();
                }
            },
            controller: 'RealmAdminEventsCtrl'
        })
        .when('/realms/:realm/history', {
            templateUrl: resourceUrl + '/partials/realm-events-history.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                eventsConfig: function (RealmEventsConfigLoader) {
                    return RealmEventsConfigLoader();
                },
            },
            controller: 'RealmEventHistoryCtrl'
        })
        .when('/realms/:realm/audit-listeners', {
            templateUrl: resourceUrl + '/partials/realm-events.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                eventsConfig: function (RealmEventsConfigLoader) {
                    return RealmEventsConfigLoader();
                },
            },
            controller: 'RealmEventListenersCtrl'
        })
        .when('/create/admin/:realm', {
            templateUrl: resourceUrl + '/partials/admin-user-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                user: function () {
                    return {};
                }
            },
            controller: 'AdminDetailCtrl'
        })
        .when('/realms/:realm/admin/users/:user', {
            templateUrl: resourceUrl + '/partials/admin-user-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'AdminDetailCtrl'
        })
        .when('/realms/:realm/admin/users/:user/user-attributes', {
            templateUrl: resourceUrl + '/partials/admin-user-attributes.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'AdminDetailCtrl'
        })
        .when('/realms/:realm/admin/users/:user/role-mappings', {
            templateUrl: resourceUrl + '/partials/admin-user-role-mappings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function () {
                    return {};
                }
            },
            controller: 'UserRoleMappingCtrl'
        })
        .when('/realms/:realm/admin/users/:user/authorized-connections', {
            templateUrl: resourceUrl + '/partials/admin-user-authorized-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                federatedIdentities: function (UserFederatedIdentityLoader) {
                    return UserFederatedIdentityLoader();
                }
            },
            controller: 'UserFederatedIdentityCtrl'
        })
        // .when('/realms/:realm/users/:user/authorized-connections', {
        //     templateUrl : resourceUrl + '/partials/user-authorized-connections.html',
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         },
        //         user : function(UserLoader) {
        //             return UserLoader();
        //         },
        //         federatedIdentities : function(UserFederatedIdentityLoader) {
        //             return UserFederatedIdentityLoader();
        //         }
        //     },
        //     controller : 'UserFederatedIdentityCtrl'
        // })
        .when('/realms/:realm/admin/users/:user/sessions', {
            templateUrl: resourceUrl + '/partials/admin-user-sessions.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                sessions: function (UserSessionsLoader) {
                    return UserSessionsLoader();
                }
            },
            controller: 'UserSessionsCtrl'
        })
        .when('/create/user/:realm', {
            templateUrl: resourceUrl + '/partials/user-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function () {
                    return {};
                }
            },
            controller: 'UserDetailCtrl'
        })
        .when('/realms/:realm/users/:user', {
            templateUrl: resourceUrl + '/partials/user-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'UserDetailCtrl'
        })
        .when('/realms/:realm/users/:user/user-attributes', {
            templateUrl: resourceUrl + '/partials/user-attributes.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'UserDetailCtrl'
        })
        .when('/realms/:realm/users/:user/user-transactions', {
            templateUrl: resourceUrl + '/partials/user-transactions.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'UserTransactionsCtrl'
        })
        .when('/realms/:realm/users/:user/user-credentials', {
            templateUrl: resourceUrl + '/partials/user-credentials.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'UserCredentialsCtrl'
        })
        .when('/realms/:realm/admin/users/:user/admin-user-credentials', {
            templateUrl: resourceUrl + '/partials/admin-user-credentials.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'UserCredentialsCtrl'
        })
        .when('/realms/:realm/admin/users/:user/admin-user-transactions', {
            templateUrl: resourceUrl + '/partials/admin-user-transactions.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'UserTransactionsCtrl'
        })
        .when('/realms/:realm/users/:user/role-mappings', {
            templateUrl: resourceUrl + '/partials/role-mappings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function () {
                    return {};
                }
            },
            controller: 'UserRoleMappingCtrl'
        })
        .when('/realms/:realm/users/:user/sessions', {
            templateUrl: resourceUrl + '/partials/user-sessions.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                sessions: function (UserSessionsLoader) {
                    return UserSessionsLoader();
                }
            },
            controller: 'UserSessionsCtrl'
        })
        .when('/realms/:realm/users/:user/authorized-connections', {
            templateUrl: resourceUrl + '/partials/user-authorized-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                federatedIdentities: function (UserFederatedIdentityLoader) {
                    return UserFederatedIdentityLoader();
                }
            },
            controller: 'UserFederatedIdentityCtrl'
        })
        .when('/create/federated-identity/:realm/:user', {
            templateUrl: resourceUrl + '/partials/user-federated-identity-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                federatedIdentities: function (UserFederatedIdentityLoader) {
                    return UserFederatedIdentityLoader();
                }
            },
            controller: 'UserFederatedIdentityAddCtrl'
        })
        .when('/realms/:realm/users/:user/offline-sessions/:client', {
            templateUrl: resourceUrl + '/partials/user-offline-sessions.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                offlineSessions: function (UserOfflineSessionsLoader) {
                    return UserOfflineSessionsLoader();
                }
            },
            controller: 'UserOfflineSessionsCtrl'
        })
        .when('/realms/:realm/users', {
            templateUrl: resourceUrl + '/partials/user-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'UserListCtrl'
        })
        .when('/realms/:realm/users/directory-sync/connections', {
            templateUrl: resourceUrl + `/partials/directory-sync-connections.html`,
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                directorySyncList: function (DirectorySyncConnListLoader) {
                    return DirectorySyncConnListLoader();
                }
            },
            controller: 'DirectorySyncConnectionsCtrl'
        })
        // .when('/realms/:realm/users/directory-sync/connections/:provider_id',{
        //     templateUrl: function(params){ return resourceUrl + `/partials/directory-sync-${params.provider_id}.html` },
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         },
        //         serverInfo : function(ServerInfoLoader) {
        //             return ServerInfoLoader();
        //         },
        //         directorySyncList : function(DirectorySyncListLoader){
        //             return DirectorySyncListLoader();
        //         }
        //     },
        //     controller: 'DirectorySyncConnectionsCtrl'
        // })
        .when('/realms/:realm/users/directory-sync/connections/:provider_id/:connection_id', {
            templateUrl: function (params) { return resourceUrl + `/partials/directory-sync-${params.provider_id}-conn.html`; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                availableUserSyncs : function(UserSyncAvailableLoader){
                    return UserSyncAvailableLoader();
                },
                directorySyncInfo: function (DirectorySyncInfoLoader) {
                    return DirectorySyncInfoLoader();
                },
                directorySyncHistoryList: function (DirectorySyncHistoryLoader) {
                    return DirectorySyncHistoryLoader();
                }
            },
            controller: 'DirectorySyncConnectionsManageCtrl'
        })
        .when('/realms/:realm/users/directory-sync/create', {
            templateUrl: resourceUrl + '/partials/directory-sync-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                availableUserSyncs : function(UserSyncAvailableLoader){
                    return UserSyncAvailableLoader();
                }
            },
            controller: 'DirectorySyncListCtrl'
        })
        .when('/create/role/:realm', {
            templateUrl: resourceUrl + '/partials/role-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                role: function () {
                    return {};
                },
                roles: function (RoleListLoader) {
                    return RoleListLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'RoleDetailCtrl'
        })
        .when('/realms/:realm/roles/:role', {
            templateUrl: resourceUrl + '/partials/role-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                role: function (RoleLoader) {
                    return RoleLoader();
                },
                roles: function (RoleListLoader) {
                    return RoleListLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'RoleDetailCtrl'
        })
        .when('/realms/:realm/roles/:role/role-attributes', {
            templateUrl: resourceUrl + '/partials/role-attributes.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                role: function (RoleLoader) {
                    return RoleLoader();
                },
                roles: function (RoleListLoader) {
                    return RoleListLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'RoleDetailCtrl'
        })
        .when('/realms/:realm/roles/:role/users', {
            templateUrl: resourceUrl + '/partials/realm-role-users.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                role: function (RoleLoader) {
                    return RoleLoader();
                }
            },
            controller: 'RoleMembersCtrl'
        })
        .when('/realms/:realm/roles', {
            templateUrl: resourceUrl + '/partials/role-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                role: function () {
                    return {};
                },
            },
            controller: 'RoleListCtrl'
        })
        .when('/realms/:realm/users/:user/roles-groups', {
            templateUrl: resourceUrl + '/partials/roles-groups.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function () {
                    return {};
                }
            },
            controller: 'RolesGroupsCtrl'
        })
        .when('/create/group/:realm/parent/:parentId', {
            templateUrl: resourceUrl + '/partials/create-group.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                parentId: function ($route) {
                    return $route.current.params.parentId;
                }
            },
            controller: 'GroupCreateCtrl'
        })
        .when('/create/role/:realm/clients/:client', {
            templateUrl: resourceUrl + '/partials/client-role-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                role: function () {
                    return {};
                },
                roles: function (RoleListLoader) {
                    return RoleListLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'ClientRoleDetailCtrl'
        })
        .when('/realms/:realm/clients/:client/roles/:role', {
            templateUrl: resourceUrl + '/partials/client-role-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                role: function (ClientRoleLoader) {
                    return ClientRoleLoader();
                },
                roles: function (RoleListLoader) {
                    return RoleListLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'ClientRoleDetailCtrl'
        })
        .when('/realms/:realm/clients/:client/roles/:role/role-attributes', {
            templateUrl: resourceUrl + '/partials/client-role-attributes.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                role: function (ClientRoleLoader) {
                    return ClientRoleLoader();
                },
                roles: function (RoleListLoader) {
                    return RoleListLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'ClientRoleDetailCtrl'
        })
        .when('/realms/:realm/clients/:client/roles/:role/users', {
            templateUrl: resourceUrl + '/partials/client-role-users.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                role: function (ClientRoleLoader) {
                    return ClientRoleLoader();
                }
            },
            controller: 'ClientRoleMembersCtrl'
        })
        .when('/realms/:realm/sso/:client/mappers', {
            templateUrl: resourceUrl + '/partials/client-sso-mappers.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientProtocolMapperListCtrl'
        })
        .when('/realms/:realm/sso/:client/mappers/:id', {
            templateUrl: resourceUrl + '/partials/client-protocol-mapper-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mapper: function (ClientProtocolMapperLoader) {
                    return ClientProtocolMapperLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }

            },
            controller: 'ClientProtocolMapperCtrl'
        })
        .when('/realms/:realm/clients/:client/mappers', {
            templateUrl: resourceUrl + '/partials/client-mappers.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientProtocolMapperListCtrl'
        })
        .when('/realms/:realm/sso/:client/add-mappers', {
            templateUrl: resourceUrl + '/partials/client-sso-mappers-add.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'AddBuiltinProtocolMapperCtrl'
        })
        .when('/realms/:realm/clients/:client/add-mappers', {
            templateUrl: resourceUrl + '/partials/client-mappers-add.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'AddBuiltinProtocolMapperCtrl'
        })
        .when('/realms/:realm/clients/:client/mappers/:id', {
            templateUrl: resourceUrl + '/partials/client-protocol-mapper-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mapper: function (ClientProtocolMapperLoader) {
                    return ClientProtocolMapperLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }

            },
            controller: 'ClientProtocolMapperCtrl'
        })
        .when('/create/sso/:realm/:client/mappers', {
            templateUrl: resourceUrl + '/partials/client-sso-protocol-mapper-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'ClientProtocolMapperCreateCtrl'
        })
        .when('/create/clients/:realm/:client/mappers', {
            templateUrl: resourceUrl + '/partials/client-create-protocol-mapper-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'ClientProtocolMapperCreateCtrl'
        })
        .when('/realms/:realm/sso/:client/client-scopes/setup-scopes', {
            templateUrl: resourceUrl + '/partials/client-sso-scopes-setup.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                clientScopes: function (ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                clientDefaultClientScopes: function (ClientDefaultClientScopesLoader) {
                    return ClientDefaultClientScopesLoader();
                },
                clientOptionalClientScopes: function (ClientOptionalClientScopesLoader) {
                    return ClientOptionalClientScopesLoader();
                }
            },
            controller: 'ClientClientScopesSetupCtrl'
        })
        .when('/realms/:realm/clients/:client/client-scopes/setup-scopes', {
            templateUrl: resourceUrl + '/partials/client-scopes-setup.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                clientScopes: function (ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                clientDefaultClientScopes: function (ClientDefaultClientScopesLoader) {
                    return ClientDefaultClientScopesLoader();
                },
                clientOptionalClientScopes: function (ClientOptionalClientScopesLoader) {
                    return ClientOptionalClientScopesLoader();
                }
            },
            controller: 'ClientClientScopesSetupCtrl'
        })
        .when('/realms/:realm/client-scopes', {
            templateUrl: resourceUrl + '/partials/client-scope-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clientScopes: function (ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller: 'ClientScopeListCtrl'
        })
        .when('/realms/:realm/default-client-scopes', {
            templateUrl: resourceUrl + '/partials/client-scopes-realm-default.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clientScopes: function (ClientScopeListLoader) {
                    return ClientScopeListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                realmDefaultClientScopes: function (RealmDefaultClientScopesLoader) {
                    return RealmDefaultClientScopesLoader();
                },
                realmOptionalClientScopes: function (RealmOptionalClientScopesLoader) {
                    return RealmOptionalClientScopesLoader();
                }
            },
            controller: 'ClientScopesRealmDefaultCtrl'
        })
        .when('/create/client-scope/:realm', {
            templateUrl: resourceUrl + '/partials/client-scope-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clientScope: function () {
                    return {};
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientScopeDetailCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope/mappers', {
            templateUrl: resourceUrl + '/partials/client-scope-mappers.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clientScope: function (ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientScopeProtocolMapperListCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope/add-mappers', {
            templateUrl: resourceUrl + '/partials/client-scope-mappers-add.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clientScope: function (ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientScopeAddBuiltinProtocolMapperCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope/mappers/:id', {
            templateUrl: resourceUrl + '/partials/client-scope-protocol-mapper-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clientScope: function (ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mapper: function (ClientScopeProtocolMapperLoader) {
                    return ClientScopeProtocolMapperLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }

            },
            controller: 'ClientScopeProtocolMapperCtrl'
        })
        .when('/create/client-scope/:realm/:clientScope/mappers', {
            templateUrl: resourceUrl + '/partials/client-scope-protocol-mapper-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                clientScope: function (ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'ClientScopeProtocolMapperCreateCtrl'
        })
        .when('/realms/:realm/clients/:client/credentials/client-jwt/:keyType/import/:attribute', {
            templateUrl: resourceUrl + '/partials/client-credentials-jwt-key-import.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                callingContext: function () {
                    return "jwt-credentials";
                }
            },
            controller: 'ClientCertificateImportCtrl'
        })
        .when('/realms/:realm/clients/:client/credentials/client-jwt/:keyType/export/:attribute', {
            templateUrl: resourceUrl + '/partials/client-credentials-jwt-key-export.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                callingContext: function () {
                    return "jwt-credentials";
                }
            },
            controller: 'ClientCertificateExportCtrl'
        })
        .when('/realms/:realm/clients/:client/identity-provider', {
            templateUrl: resourceUrl + '/partials/client-identity-provider.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                }
            },
            controller: 'ClientIdentityProviderCtrl'
        })
        .when('/register-node/realms/:realm/clients/:client/clustering', {
            templateUrl: resourceUrl + '/partials/client-clustering-node.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                }
            },
            controller: 'ClientClusteringNodeCtrl'
        })
        .when('/realms/:realm/clients/:client/clustering/:node', {
            templateUrl: resourceUrl + '/partials/client-clustering-node.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                }
            },
            controller: 'ClientClusteringNodeCtrl'
        })
        .when('/realms/:realm/clients/:client/saml/keys', {
            templateUrl: resourceUrl + '/partials/client-saml-keys.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                }
            },
            controller: 'ClientSamlKeyCtrl'
        })
        .when('/realms/:realm/clients/:client/saml/:keyType/import/:attribute', {
            templateUrl: resourceUrl + '/partials/client-saml-key-import.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                callingContext: function () {
                    return "saml";
                }
            },
            controller: 'ClientCertificateImportCtrl'
        })
        .when('/realms/:realm/clients/:client/saml/:keyType/export/:attribute', {
            templateUrl: resourceUrl + '/partials/client-saml-key-export.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                callingContext: function () {
                    return "saml";
                }
            },
            controller: 'ClientCertificateExportCtrl'
        })
        .when('/realms/:realm/clients/:client/service-account-roles', {
            templateUrl: resourceUrl + '/partials/client-service-account-roles.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (ClientServiceAccountUserLoader) {
                    return ClientServiceAccountUserLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                }
            },
            controller: 'UserRoleMappingCtrl'
        })
        .when('/create/client/:realm', {
            templateUrl: resourceUrl + '/partials/create-client.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function () {
                    return {};
                },
                flows: function (AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'CreateClientCtrl'
        })
        .when('/create/ciba-client/type/:type/:realm', {
            templateUrl: resourceUrl + '/partials/create-ciba-client.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function () {
                    return {};
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                brandings: function (BrandingAllLoader) {
                    return BrandingAllLoader();
                },
                actions: function (ActionListLoader) {
                    return ActionListLoader();
                },
                actionPolicies: function (ActionPolicyListLoader) {
                    return ActionPolicyListLoader();
                },
                cibaPolicies: function (CibaPolicyListLoader) {
                    return CibaPolicyListLoader();
                }
            },
            controller: 'CreateCibaClientCtrl'
        })
        .when('/create/client/type/:type/:realm', {
            templateUrl: resourceUrl + '/partials/create-client.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function () {
                    return {};
                },
                flows: function (AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'CreateClientCtrl'
        })
        .when('/realms/:realm/sso/:client', {
            templateUrl: resourceUrl + '/partials/client-sso-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                flows: function (AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                brandings: function (BrandingAllLoader) {
                    return BrandingAllLoader();
                }
            },
            controller: 'ClientSSODetailCtrl'
        })
        .when('/realms/:realm/clients/:client', {
            templateUrl: resourceUrl + '/partials/client-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                flows: function (AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                brandings: function (BrandingAllLoader) {
                    return BrandingAllLoader();
                }
            },
            controller: 'ClientDetailCtrl'
        })
        .when('/realms/:realm/clients/:client/developer', {
            templateUrl: resourceUrl + '/partials/client-developers.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                keys: function (RealmKeysLoader) {
                    return RealmKeysLoader();
                }
            },
            controller: 'ClientDeveloperCtrl'
        })
        .when('/realms/:realm/sso/:client/tutorial', {
            templateUrl: resourceUrl + '/partials/client-sso-tutorial.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
            },
            controller: 'ClientSSOTutorialCtrl'
        })
        .when('/realms/:realm/sso/:client/metadata', {
            templateUrl: resourceUrl + '/partials/client-sso-metadata.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                clientMetadata: function (ClientSSOMetadataLoader) {
                    return ClientSSOMetadataLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
            },
            controller: 'ClientSSOMetadataCtrl'
        })
        .when('/realms/:realm/clients/:client/metadata', {
            templateUrl: resourceUrl + '/partials/client-sso-metadata.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                clientMetadata: function (ClientSSOMetadataLoader) {
                    return ClientSSOMetadataLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
            },
            controller: 'ClientSSOMetadataCtrl'
        })
        .when('/realms/:realm/sso/:client/flow-override', {
            templateUrl: resourceUrl + '/partials/client-sso-flow-override.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                flows: function (AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                onboardingFlows: function (AuthenticationInstalledOnboardingFlowsLoader) {
                    return AuthenticationInstalledOnboardingFlowsLoader();
                },
                loginFlows: function (AuthenticationInstalledLoginFlowsLoader) {
                    return AuthenticationInstalledLoginFlowsLoader();
                },
                recoveryFlows: function (AuthenticationInstalledRecoveryFlowsLoader) {
                    return AuthenticationInstalledRecoveryFlowsLoader();
                },
                directGrantFlows: function (AuthenticationInstalledDirectGrantFlowsLoader) {
                    return AuthenticationInstalledDirectGrantFlowsLoader();
                },
                actions: function (ActionListLoader) {
                    return ActionListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                actionPolicies: function (ActionPolicyListLoader) {
                    return ActionPolicyListLoader();
                },
                cibaPolicies: function (CibaPolicyListLoader) {
                    return CibaPolicyListLoader();
                }
            },
            controller: 'ClientFlowOverrideCtrl'
        })
        .when('/realms/:realm/clients/:client/flow-override', {
            templateUrl: resourceUrl + '/partials/client-flow-override.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                flows: function (AuthenticationFlowsLoader) {
                    return AuthenticationFlowsLoader();
                },
                onboardingFlows: function (AuthenticationInstalledOnboardingFlowsLoader) {
                    return AuthenticationInstalledOnboardingFlowsLoader();
                },
                loginFlows: function (AuthenticationInstalledLoginFlowsLoader) {
                    return AuthenticationInstalledLoginFlowsLoader();
                },
                recoveryFlows: function (AuthenticationInstalledRecoveryFlowsLoader) {
                    return AuthenticationInstalledRecoveryFlowsLoader();
                },
                directGrantFlows: function (AuthenticationInstalledDirectGrantFlowsLoader) {
                    return AuthenticationInstalledDirectGrantFlowsLoader();
                },
                actions: function (ActionListLoader) {
                    return ActionListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                actionPolicies: function (ActionPolicyListLoader) {
                    return ActionPolicyListLoader();
                },
                cibaPolicies: function (CibaPolicyListLoader) {
                    return CibaPolicyListLoader();
                }
            },
            controller: 'ClientFlowOverrideCtrl'
        })
        .when('/realms/:realm/clients/:client/transaction/flow-override', {
            templateUrl: resourceUrl + '/partials/transaction-client-flow-override.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                actions: function (ActionListLoader) {
                    return ActionListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'TransactionClientFlowOverrideCtrl'
        })
        .when('/realms/:realm/clients/:client/transaction/gatepass', {
            templateUrl: resourceUrl + '/partials/transaction-client-gatepass.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                cibaPolicies: function (CibaPolicyListLoader) {
                    return CibaPolicyListLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'TransactionClientGatepassCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope', {
            templateUrl: resourceUrl + '/partials/client-scope-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clientScope: function (ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientScopeDetailCtrl'
        })
        .when('/realms/:realm/client-scopes/:clientScope/scope-mappings', {
            templateUrl: resourceUrl + '/partials/client-scope-scope-mappings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                clientScope: function (ClientScopeLoader) {
                    return ClientScopeLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'ClientScopeScopeMappingCtrl'
        })
        .when('/realms/:realm/clients', {
            templateUrl: resourceUrl + '/partials/client-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller: 'ClientListCtrl'
        })
        .when('/realms/:realm/clients/:client/transaction', {
            templateUrl: resourceUrl + '/partials/ciba-settings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientCibaCtrl'
        })
        .when('/realms/:realm/clients/:client/transaction/tutorial', {
            templateUrl: resourceUrl + '/partials/ciba-tutorial.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                client: function (ClientLoader) {
                    return ClientLoader();
                },
            },
            controller: 'ClientCibaTutorialCtrl'
        })
        .when('/realms/:realm/clients/type/:type', {
            templateUrl: resourceUrl + '/partials/client-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller: 'ClientListCtrl'
        })
        .when('/realms/:realm/clients/type/:type/:mode', {
            templateUrl: resourceUrl + '/partials/client-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller: 'ClientListCtrl'
        })
        .when('/realms/:realm/sso', {
            templateUrl: resourceUrl + '/partials/client-sso-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller: 'ClientSSOListCtrl'
        })
        .when('/import/client/:realm', {
            templateUrl: resourceUrl + '/partials/client-import.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientImportCtrl'
        })
        .when('/realms/:realm/client-stores', {
            templateUrl: resourceUrl + '/partials/client-storage-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ClientStoresCtrl'
        })
        .when('/realms/:realm/client-storage/providers/:provider/:componentId', {
            templateUrl: resourceUrl + '/partials/client-storage-generic.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function (ComponentLoader) {
                    return ComponentLoader();
                },
                providerId: function ($route) {
                    return $route.current.params.provider;
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GenericClientStorageCtrl'
        })
        .when('/create/client-storage/:realm/providers/:provider', {
            templateUrl: resourceUrl + '/partials/client-storage-generic.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function () {
                    return {

                    };
                },
                providerId: function ($route) {
                    return $route.current.params.provider;
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GenericClientStorageCtrl'
        })
        .when('/', {
            templateUrl: resourceUrl + '/partials/home.html',
            resolve: {
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'HomeCtrl'
        })
        .when('/mocks/:realm', {
            templateUrl: resourceUrl + '/partials/realm-detail_mock.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmDetailCtrl'
        })
        .when('/create/user-storage/:realm/providers/ldap', {
            templateUrl: resourceUrl + '/partials/user-storage-ldap.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function () {
                    return {

                    };
                },
                providerId: function ($route) {
                    return $route.current.params.provider;
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'LDAPUserStorageCtrl'
        })
        .when('/create/user-storage/:realm/providers/kerberos', {
            templateUrl: resourceUrl + '/partials/user-storage-kerberos.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function () {
                    return {

                    };
                },
                providerId: function ($route) {
                    return "kerberos";
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GenericUserStorageCtrl'
        })
        .when('/create/user-storage/:realm/providers/:provider', {
            templateUrl: resourceUrl + '/partials/user-storage-generic.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function () {
                    return {

                    };
                },
                providerId: function ($route) {
                    return $route.current.params.provider;
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GenericUserStorageCtrl'
        })
        .when('/realms/:realm/user-storage/providers/ldap/:componentId', {
            templateUrl: resourceUrl + '/partials/user-storage-ldap.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function (ComponentLoader) {
                    return ComponentLoader();
                },
                providerId: function ($route) {
                    return $route.current.params.provider;
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'LDAPUserStorageCtrl'
        })
        .when('/realms/:realm/user-storage/providers/kerberos/:componentId', {
            templateUrl: resourceUrl + '/partials/user-storage-kerberos.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function (ComponentLoader) {
                    return ComponentLoader();
                },
                providerId: function ($route) {
                    return "kerberos";
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GenericUserStorageCtrl'
        })
        .when('/realms/:realm/user-storage/providers/:provider/:componentId', {
            templateUrl: resourceUrl + '/partials/user-storage-generic.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                instance: function (ComponentLoader) {
                    return ComponentLoader();
                },
                providerId: function ($route) {
                    return $route.current.params.provider;
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'GenericUserStorageCtrl'
        })
        .when('/realms/:realm/ldap-mappers/:componentId', {
            templateUrl: function (params) { return resourceUrl + '/partials/user-storage-ldap-mappers.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                provider: function (ComponentLoader) {
                    return ComponentLoader();
                },
                mappers: function (ComponentsLoader, $route) {
                    return ComponentsLoader.loadComponents($route.current.params.componentId, 'org.keycloak.storage.ldap.mappers.LDAPStorageMapper');
                }
            },
            controller: 'LDAPMapperListCtrl'
        })
        .when('/create/ldap-mappers/:realm/:componentId', {
            templateUrl: function (params) { return resourceUrl + '/partials/user-storage-ldap-mapper-detail.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                provider: function (ComponentLoader) {
                    return ComponentLoader();
                },
                mapperTypes: function (SubComponentTypesLoader, $route) {
                    return SubComponentTypesLoader.loadComponents($route.current.params.componentId, 'org.keycloak.storage.ldap.mappers.LDAPStorageMapper');
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'LDAPMapperCreateCtrl'
        })
        .when('/realms/:realm/ldap-mappers/:componentId/mappers/:mapperId', {
            templateUrl: function (params) { return resourceUrl + '/partials/user-storage-ldap-mapper-detail.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                provider: function (ComponentLoader) {
                    return ComponentLoader();
                },
                mapperTypes: function (SubComponentTypesLoader, $route) {
                    return SubComponentTypesLoader.loadComponents($route.current.params.componentId, 'org.keycloak.storage.ldap.mappers.LDAPStorageMapper');
                },
                mapper: function (LDAPMapperLoader) {
                    return LDAPMapperLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'LDAPMapperCtrl'
        })
        .when('/realms/:realm/connections/enterprise/create', {
            templateUrl: resourceUrl + '/partials/create-enterprise-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'UserFederationCtrl'
        })
        .when('/realms/:realm/connections/enterprise', {
            templateUrl: resourceUrl + '/partials/realm-enterprise-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'UserFederationCtrl'
        })
        .when('/realms/:realm/defense/headers', {
            templateUrl: resourceUrl + '/partials/defense-headers.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller: 'DefenseHeadersCtrl'
        })
        .when('/realms/:realm/defense/brute-force', {
            templateUrl: resourceUrl + '/partials/brute-force.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'RealmBruteForceCtrl'
        })
        .when('/realms/:realm/defense/anomaly', {
            templateUrl: resourceUrl + '/partials/anomaly.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                eventsConfig: function (RealmEventsConfigLoader) {
                    return RealmEventsConfigLoader();
                }
            },
            controller: 'RealmAnomalyCtrl'
        })
        .when('/realms/:realm/defense/black-list', {
            templateUrl: resourceUrl + '/partials/black-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
            },
            controller: 'BlackListCtrl'
        })
        .when('/realms/:realm/defense/keys', {
            templateUrl: resourceUrl + '/partials/realm-keys.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                keys: function (RealmKeysLoader) {
                    return RealmKeysLoader();
                }
            },
            controller: 'RealmKeysCtrl'
        })
        .when('/realms/:realm/protocols', {
            templateUrl: resourceUrl + '/partials/protocol-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }

            },
            controller: 'ProtocolListCtrl'
        })
        .when('/realms/:realm/authentication/installed/onboarding', {
            templateUrl: resourceUrl + '/partials/authentication-installed-onboarding.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                selectedFlow: function () {
                    return null;
                }
            },
            controller: 'AuthenticationInstalledOnboardingCtrl'
        })
        .when('/realms/:realm/authentication/installed/login', {
            templateUrl: resourceUrl + '/partials/authentication-installed-login.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                selectedFlow: function () {
                    return null;
                }
            },
            controller: 'AuthenticationInstalledLoginCtrl'
        })
        .when('/realms/:realm/authentication/installed/recovery', {
            templateUrl: resourceUrl + '/partials/authentication-installed-recovery.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                selectedFlow: function () {
                    return null;
                }
            },
            controller: 'AuthenticationInstalledRecoveryCtrl'
        })
        .when('/realms/:realm/authentication/flows/:flow/create/execution/:topFlow', {
            templateUrl: resourceUrl + '/partials/create-execution.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                topFlow: function ($route) {
                    return $route.current.params.topFlow;
                },
                parentFlow: function (AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                formActionProviders: function (AuthenticationFormActionProvidersLoader) {
                    return AuthenticationFormActionProvidersLoader();
                },
                authenticatorProviders: function (AuthenticatorProvidersLoader) {
                    return AuthenticatorProvidersLoader();
                },
                clientAuthenticatorProviders: function (ClientAuthenticatorProvidersLoader) {
                    return ClientAuthenticatorProvidersLoader();
                }
            },
            controller: 'CreateExecutionCtrl'
        })
        .when('/realms/:realm/authentication/flows/:flow/create/flow/execution/:topFlow', {
            templateUrl: resourceUrl + '/partials/create-flow-execution.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                topFlow: function ($route) {
                    return $route.current.params.topFlow;
                },
                parentFlow: function (AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                formProviders: function (AuthenticationFormProvidersLoader) {
                    return AuthenticationFormProvidersLoader();
                }
            },
            controller: 'CreateExecutionFlowCtrl'
        })
        // .when('/realms/:realm/authentication/choose/flow', {
        //     templateUrl : resourceUrl + '/partials/choose-flow.html',
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         }
        //     },
        //     controller : 'ChooseFlowCtrl'
        // })
        .when('/realms/:realm/authentication/flows/:flow/:flowtype/flow', {
            templateUrl: resourceUrl + '/partials/authentication-drawflow.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                flows: function (AuthenticationInstalledFlowsLoader) {
                    return AuthenticationInstalledFlowsLoader();
                },
                flow: function (AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                selectedFlow: function ($route) {
                    return $route.current.params.flow;
                },
                authenticatorProviders: function (AuthenticatorProvidersLoader) {
                    return AuthenticatorProvidersLoader();
                },
                passwordPolicies: function (PasswordPolicyListLoader){
                    return PasswordPolicyListLoader();
                },
            },
            controller: 'AuthenticationDrawFlowsCtrl'
        })
        .when('/realms/:realm/connections/mfa', {
            templateUrl: resourceUrl + '/partials/realm-mfa-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mfaConfig: function (RealmMFAConfigLoader) {
                    return RealmMFAConfigLoader();
                }
            },
            controller: 'RealmMFAConnectionsCtrl'
        })
        .when('/realms/:realm/connections/mfa/create', {
            templateUrl: resourceUrl + '/partials/create-mfa-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mfaConfig: function (RealmMFAConfigLoader) {
                    return RealmMFAConfigLoader();
                }
            },
            controller: 'CreateMfaConnectionsCtrl'
        })
        .when('/realms/:realm/connections/mfa/create-conn/:provider_id', {
            templateUrl: function (params) { return resourceUrl + '/partials/realm-mfa-provider.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mfaConfig: function (RealmMFAConfigLoader) {
                    return RealmMFAConfigLoader();
                },
            },
            controller: 'RealmMfaConnectionsProviderCtrl'
        })
        .when('/realms/:realm/connections/mfa/settings/:provider_id', {
            templateUrl: function (params) { return resourceUrl + '/partials/realm-mfa-provider.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                mfaConfig: function (RealmMFAConfigLoader) {
                    return RealmMFAConfigLoader();
                },
            },
            controller: 'RealmMfaConnectionsProviderCtrl'
        })
        .when('/realms/:realm/connections/social', {
            templateUrl: resourceUrl + '/partials/realm-social-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance: function (IdentityProviderLoader) {
                    return {};
                },
                providerFactory: function (IdentityProviderFactoryLoader) {
                    return {};
                },
                emailSenders: function (EmailProviderLoader) {
                    return {};
                },
                authFlows: function (AuthenticationFlowsLoader) {
                    return {};
                }
            },
            controller: 'RealmSocialConnectionsCtrl'
        })
        .when('/realms/:realm/connections/social/create', {
            templateUrl: resourceUrl + '/partials/create-social-connections.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance: function (IdentityProviderLoader) {
                    return {};
                },
                providerFactory: function (IdentityProviderFactoryLoader) {
                    return {};
                },
                authFlows: function (AuthenticationFlowsLoader) {
                    return {};
                }
            },
            controller: 'CreateSocialConnectionsCtrl'
        })
        .when('/realms/:realm/connections/social/create/custom', {
            templateUrl: resourceUrl + '/partials/create-custom-social-conn.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance: function (IdentityProviderLoader) {
                    return {};
                },
                providerFactory: function (IdentityProviderFactoryLoader) {
                    return {};
                },
                authFlows: function (AuthenticationFlowsLoader) {
                    return {};
                }
            },
            controller: 'CreateCustomSocialConnectionsCtrl'
        })
        .when('/realms/:realm/connections/social/create-conn/:provider_id', {
            templateUrl: function (params) { return resourceUrl + '/partials/realm-identity-provider-' + params.provider_id + '.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance: function (IdentityProviderLoader) {
                    return {};
                },
                providerFactory: function (IdentityProviderFactoryLoader) {
                    return new IdentityProviderFactoryLoader();
                },
                emailSenders: function (EmailProviderLoader) {
                    return EmailProviderLoader();
                },
                authFlows: function (AuthenticationInstalledFlowsLoader) {
                    return AuthenticationInstalledFlowsLoader();
                },
            },
            controller: 'RealmSocialConnectionsCtrl'
        })
        .when('/realms/:realm/connections/social/settings/:provider_id/:alias', {
            templateUrl: function (params) { return resourceUrl + '/partials/realm-identity-provider-' + params.provider_id + '.html'; },
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                instance: function (IdentityProviderLoader) {
                    return IdentityProviderLoader();
                },
                emailSenders: function (EmailProviderLoader) {
                    return EmailProviderLoader();
                },
                providerFactory: function (IdentityProviderFactoryLoader) {
                    return IdentityProviderFactoryLoader();
                },
                authFlows: function (AuthenticationInstalledFlowsLoader) {
                    return AuthenticationInstalledFlowsLoader();
                },
            },
            controller: 'RealmSocialConnectionsCtrl'
        })
        .when('/realms/:realm/authentication/create/flow', {
            templateUrl: resourceUrl + '/partials/create-flow.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'CreateFlowCtrl'
        })
        .when('/realms/:realm/authentication/templates', {
            templateUrl: resourceUrl + '/partials/templates.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'TemplatesCtrl'
        })
        .when('/realms/:realm/authentication/sub-workflows', {
                    templateUrl: resourceUrl + '/partials/sub-workflows.html',
                    resolve: {
                        realm: function (RealmLoader) {
                            return RealmLoader();
                        }
                    },
                    controller: 'SubWorkflowsCtrl'
                })
        .when('/realms/:realm/authentication/templates/:action', {
            templateUrl: resourceUrl + '/partials/add-templates.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                unregisteredRequiredActions: function (UnregisteredRequiredActionsListLoader) {
                    return UnregisteredRequiredActionsListLoader();
                },
                actionrep: function (ActionLoader) {
                    return ActionLoader();
                },
                actionPolicies: function (ActionPolicyListLoader) {
                    return ActionPolicyListLoader();
                },
                cibaPolicies: function (CibaPolicyListLoader) {
                    return CibaPolicyListLoader();
                }
            },
            controller: 'CreateTemplatesCtrl'
        })
        .when('/realms/:realm/authentication/sub-workflows/:action', {
            templateUrl: resourceUrl + '/partials/add-templates.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                unregisteredRequiredActions: function (UnregisteredRequiredActionsListLoader) {
                    return UnregisteredRequiredActionsListLoader();
                },
                actionrep: function (ActionLoader) {
                    return ActionLoader();
                },
                actionPolicies: function (ActionPolicyListLoader) {
                    return ActionPolicyListLoader();
                },
                cibaPolicies: function (CibaPolicyListLoader) {
                    return CibaPolicyListLoader();
                }
            },
            controller: 'CreateTemplatesCtrl'
        })

        .when('/realms/:realm/authentication/actions', {
            templateUrl: resourceUrl + '/partials/actions.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'ActionsCtrl'
        })
        .when('/realms/:realm/authentication/actions/:action', {

            templateUrl: resourceUrl + '/partials/add-actions.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                unregisteredRequiredActions: function (UnregisteredRequiredActionsListLoader) {
                    return UnregisteredRequiredActionsListLoader();
                },
                actionrep: function (ActionLoader) {
                    return ActionLoader();
                },
                actionPolicies: function (ActionPolicyListLoader) {
                    return ActionPolicyListLoader();
                },
                passwordPolicies: function (PasswordPolicyListLoader){
                    return PasswordPolicyListLoader();
                },
                cibaPolicies: function (CibaPolicyListLoader) {
                    return CibaPolicyListLoader();
                }
            },
            controller: 'CreateActionsCtrl'
        })
        .when('/realms/:realm/brandings', {
            templateUrl: resourceUrl + '/partials/realm-brandings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmBrandingCtrl'
        })
        .when('/realms/:realm/brandings/:branding', {
            templateUrl: resourceUrl + '/partials/branding-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                branding: function (BrandingLoader) {
                    return BrandingLoader();
                }
            },
            controller: 'RealmBrandingDetailCtrl'
        })
        .when('/realms/:realm/workflows', {
            templateUrl: resourceUrl + '/partials/workflows.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'WorkflowsCtrl'
        })
        .when('/realms/:realm/policies', {
            templateUrl: resourceUrl + '/partials/realm-policies.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'RealmPolicyCtrl'
        })
        .when('/realms/:realm/policies/webauthn/create', {
            templateUrl: resourceUrl + '/partials/webauthn-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyListLoader) {
                    return PolicyListLoader();
                }
            },
            controller: 'RealmWebAuthnPolicyCtrl'
        })

        .when('/realms/:realm/policies/webauthn/:policy', {
            templateUrl: resourceUrl + '/partials/webauthn-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyLoader) {
                    return PolicyLoader();
                }
            },
            controller: 'RealmWebAuthnPolicyCtrl'
        })
        .when('/realms/:realm/policies/password/create', {
            templateUrl: resourceUrl + '/partials/password-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyListLoader) {
                    return PolicyListLoader();
                }
            },
            controller: 'RealmPasswordPolicyCtrl'
        })

        .when('/realms/:realm/policies/password/:policy', {
            templateUrl: resourceUrl + '/partials/password-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyLoader) {
                    return PolicyLoader();
                }
            },
            controller: 'RealmPasswordPolicyCtrl'
        })
        .when('/realms/:realm/policies/username/create', {
            templateUrl: resourceUrl + '/partials/username-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyListLoader) {
                    return PolicyListLoader();
                }
            },
            controller: 'RealmUsernamePolicyCtrl'
        })
        .when('/realms/:realm/policies/username/:policy', {
            templateUrl: resourceUrl + '/partials/username-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyLoader) {
                    return PolicyLoader();
                }
            },
            controller: 'RealmUsernamePolicyCtrl'
        })
        .when('/realms/:realm/policies/action/create', {
            templateUrl: resourceUrl + '/partials/action-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyListLoader) {
                    return PolicyListLoader();
                }
            },
            controller: 'RealmActionPolicyCtrl'
        })
        .when('/realms/:realm/policies/action/:policy', {
            templateUrl: resourceUrl + '/partials/action-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyLoader) {
                    return PolicyLoader();
                }
            },
            controller: 'RealmActionPolicyCtrl'
        })
        .when('/realms/:realm/policies/ciba/create', {
            templateUrl: resourceUrl + '/partials/ciba-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyListLoader) {
                    return PolicyListLoader();
                }
            },
            controller: 'RealmCibaPolicyCtrl'
        })
        .when('/realms/:realm/policies/ciba/:policy', {
            templateUrl: resourceUrl + '/partials/ciba-policy.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                },
                policies: function (PolicyLoader) {
                    return PolicyLoader();
                }
            },
            controller: 'RealmCibaPolicyCtrl'
        })
        .when('/realms/:realm/authentication/flows/:flow/config/:provider/:config', {
            templateUrl: resourceUrl + '/partials/authenticator-config.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                flow: function (AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                configType: function (AuthenticationConfigDescriptionLoader) {
                    return AuthenticationConfigDescriptionLoader();
                },
                config: function (AuthenticationConfigLoader) {
                    return AuthenticationConfigLoader();
                }
            },
            controller: 'AuthenticationConfigCtrl'
        })
        .when('/create/authentication/:realm/flows/:flow/execution/:executionId/provider/:provider', {
            templateUrl: resourceUrl + '/partials/authenticator-config.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                flow: function (AuthenticationFlowLoader) {
                    return AuthenticationFlowLoader();
                },
                configType: function (AuthenticationConfigDescriptionLoader) {
                    return AuthenticationConfigDescriptionLoader();
                },
                execution: function (ExecutionIdLoader) {
                    return ExecutionIdLoader();
                }
            },
            controller: 'AuthenticationConfigCreateCtrl'
        })
        .when('/server-info', {
            templateUrl: resourceUrl + '/partials/server-info.html',
            resolve: {
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ServerInfoCtrl'
        })
        .when('/server-info/providers', {
            templateUrl: resourceUrl + '/partials/server-info-providers.html',
            resolve: {
                serverInfo: function (ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller: 'ServerInfoCtrl'
        })
        .when('/logout', {
            templateUrl: resourceUrl + '/partials/home.html',
            controller: 'LogoutCtrl'
        })
        .when('/notfound', {
            templateUrl: resourceUrl + '/partials/notfound.html'
        })
        .when('/forbidden', {
            templateUrl: resourceUrl + '/partials/forbidden.html'
        })
        // .when('/create/realm', {
        //     templateUrl : resourceUrl + '/partials/realm-create.html',
        //     resolve : {

        //     },
        //     controller : 'RealmCreateCtrl'
        // })
        .when('/realms/:realm/groups', {
            templateUrl: resourceUrl + '/partials/group-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'GroupListCtrl'
        })
        .when('/realms/:realm/default-groups', {
            templateUrl: resourceUrl + '/partials/default-groups.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                }
            },
            controller: 'DefaultGroupsCtrl'
        })
        .when('/realms/:realm/groups/:group', {
            templateUrl: resourceUrl + '/partials/group-detail.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                group: function (GroupLoader) {
                    return GroupLoader();
                }
            },
            controller: 'GroupDetailCtrl'
        })
        .when('/realms/:realm/groups/:group/roles-users', {
            templateUrl: resourceUrl + '/partials/roles-users.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                group: function (GroupLoader) {
                    return GroupLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                }
            },
            controller: 'RolesUsersCtrl'
        })
        .when('/realms/:realm/groups/:group/attributes', {
            templateUrl: resourceUrl + '/partials/group-attributes.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                group: function (GroupLoader) {
                    return GroupLoader();
                }
            },
            controller: 'GroupDetailCtrl'
        })
        .when('/realms/:realm/groups/:group/members', {
            templateUrl: resourceUrl + '/partials/group-members.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                group: function (GroupLoader) {
                    return GroupLoader();
                }
            },
            controller: 'GroupMembersCtrl'
        })
        .when('/realms/:realm/groups/:group/role-mappings', {
            templateUrl: resourceUrl + '/partials/group-role-mappings.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                group: function (GroupLoader) {
                    return GroupLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function () {
                    return {};
                }
            },
            controller: 'GroupRoleMappingCtrl'
        })
        .when('/realms/:realm/users/:user/groups', {
            templateUrl: resourceUrl + '/partials/user-group-membership.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'KcUserGroupMembershipCtrl'
        })
        .when('/realms/:realm/admin/users/:user/groups', {
            templateUrl: resourceUrl + '/partials/admin-user-group-membership.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                }
            },
            controller: 'KcUserGroupMembershipCtrl'
        })
        .when('/realms/:realm/admin/users/:user/roles-groups', {
            templateUrl: resourceUrl + '/partials/roles-groups.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                user: function (UserLoader) {
                    return UserLoader();
                },
                clients: function (ClientListLoader) {
                    return ClientListLoader();
                },
                client: function () {
                    return {};
                }
            },
            controller: 'RolesGroupsCtrl'
        })
        // .when('/create/client-scope/:realm', {
        //     templateUrl : resourceUrl + '/partials/client-scope-detail.html',
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         },
        //         clientScope : function() {
        //             return {};
        //         },
        //         serverInfo : function(ServerInfoLoader) {
        //             return ServerInfoLoader();
        //         }
        //     },
        //     controller : 'ClientScopeDetailCtrl'
        // })
        // .when('/realms/:realm/authentication/create/template/:templatetype', {
        //     templateUrl : resourceUrl + '/partials/create-template.html',
        //     resolve : {
        //         realm : function(RealmLoader) {
        //             return RealmLoader();
        //         },
        //         templatetype: function($route) {
        //             return $route.current.params.templatetype;
        //         },
        //         authenticatorProviders : function(AuthenticatorProvidersLoader) {
        //             return AuthenticatorProvidersLoader();
        //         }
        //     },
        //     controller : 'CreateFlowTemplateCtrl'
        // })
        .otherwise({
            templateUrl: resourceUrl + '/partials/pagenotfound.html'
        });
}]);

module.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

module.config(function ($httpProvider) {
    $httpProvider.interceptors.push('errorInterceptor');

    var spinnerFunction = function (data, headersGetter) {
        if (resourceRequests == 0) {
            loadingTimer = window.setTimeout(function () {
                $('#loading').show();
                loadingTimer = -1;
            }, 500);
        }
        resourceRequests++;
        return data;
    };
    $httpProvider.defaults.transformRequest.push(spinnerFunction);

    $httpProvider.interceptors.push('spinnerInterceptor');
    $httpProvider.interceptors.push('authInterceptor');

});

module.factory('spinnerInterceptor', function ($q, $window, $rootScope, $location) {
    return {
        response: function (response) {
            resourceRequests--;
            if (resourceRequests == 0) {
                if (loadingTimer != -1) {
                    window.clearTimeout(loadingTimer);
                    loadingTimer = -1;
                }
                $('#loading').hide();
            }
            return response;
        },
        responseError: function (response) {
            resourceRequests--;
            if (resourceRequests == 0) {
                if (loadingTimer != -1) {
                    window.clearTimeout(loadingTimer);
                    loadingTimer = -1;
                }
                $('#loading').hide();
            }

            return $q.reject(response);
        }
    };
});

module.factory('errorInterceptor', function ($q, $window, $rootScope, $location, Notifications, Auth, $translate) {
    return {
        response: function (response) {
            return response;
        },
        responseError: function (response) {
            if (response.status == 401) {
                Auth.authz.logout();
            } else if (response.status == 403) {
                $location.path('/forbidden');
            } else if (response.status == 404) {
                $location.path('/notfound');
            } else if (response.status) {
                if (response.data && response.data.errorMessage) {
                    Notifications.error($translate.instant(response.data.errorMessage));
                } else if (response.data && response.data.error_description) {
                    Notifications.error($translate.instant(response.data.error_description));
                } else if (response.data && response.data.error) {
                    Notifications.error($translate.instant(response.data.error));
                } else {
                    Notifications.error($translate.instant("unexpected-server-error"));
                }
            } else {
                Notifications.error($translate.instant("no-response-from-server-error"));
            }
            return $q.reject(response);
        }
    };
});

// collapsable form fieldsets
module.directive('collapsable', function () {
    return function (scope, element, attrs) {
        element.click(function () {
            $(this).toggleClass('collapsed');
            $(this).find('.toggle-icons').toggleClass('kc-icon-collapse').toggleClass('kc-icon-expand');
            $(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    };
});

// collapsable form fieldsets
module.directive('uncollapsed', function () {
    return function (scope, element, attrs) {

        element.prepend(`<i class="toggle-class os-icon os-icon-arrow-down2" id=${attrs.id} ></i>`);
        element.click(function () {
            $(this).find('.toggle-class').toggleClass('os-icon os-icon-arrow-right').toggleClass('os-icon os-icon-arrow-down2');
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    };
});

// collapsable form fieldsets
module.directive('collapsed', function () {
    return function (scope, element, attrs) {
        element.prepend(`<i class="toggle-class os-icon os-icon-arrow-right" id=${attrs.id}></i> `);
        element.parent().find('.form-group').toggleClass('hidden');
        element.click(function () {
            $(this).find('.toggle-class').toggleClass('os-icon os-icon-arrow-down2').toggleClass('os-icon os-icon-arrow-right');
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    };
});


/**
 * Directive for presenting an ON-OFF switch for checkbox.
 * Usage: <input ng-model="mmm" name="nnn" id="iii" onoffswitch [on-text="ooo" off-text="fff"] />
 */
module.directive('onoffswitch', function () {
    return {
        restrict: "EA",
        replace: true,
        scope: {
            name: '@',
            id: '@',
            ngModel: '=',
            ngDisabled: '=',
            ngChange: '&',
            kcOnText: '@onText',
            kcOffText: '@offText'
        },
        // TODO - The same code acts differently when put into the templateURL. Find why and move the code there.
        //templateUrl: "templates/kc-switch.html",
        template: "<span><div class='onoffswitch' tabindex='0'><input type='checkbox' ng-model='ngModel' ng-disabled='ngDisabled' ng-change='ngChange()' class='onoffswitch-checkbox' name='{{name}}' id='{{id}}'></div></span>",
        compile: function (element, attrs) {
            /*
            We don't want to propagate basic attributes to the root element of directive. Id should be passed to the
            input element only to achieve proper label binding (and validity).
            */
            element.removeAttr('name');
            element.removeAttr('id');

            if (!attrs.onText) { attrs.onText = "ON"; }
            if (!attrs.offText) { attrs.offText = "OFF"; }

            element.bind('keydown', function (e) {
                var code = e.keyCode || e.which;
                if (code === 32 || code === 13) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    $(e.target).find('input').click();
                }
            });
        }
    };
});

/**
 * Directive for presenting an ON-OFF switch for checkbox. The directive expects the value to be string 'true' or 'false', not boolean true/false
 * This directive provides some additional capabilities to the default onoffswitch such as:
 *
 * - Dynamic values for id and name attributes. Useful if you need to use this directive inside a ng-repeat
 * - Specific scope to specify the value. Instead of just true or false.
 *
 * Usage: <input ng-model="mmm" name="nnn" id="iii" kc-onoffswitch-model [on-text="ooo" off-text="fff"] />
 */
module.directive('onoffswitchstring', function () {
    return {
        restrict: "EA",
        replace: true,
        scope: {
            name: '=',
            id: '=',
            value: '=',
            ngModel: '=',
            ngDisabled: '=',
            kcOnText: '@onText',
            kcOffText: '@offText'
        },
        // TODO - The same code acts differently when put into the templateURL. Find why and move the code there.
        //templateUrl: "templates/kc-switch.html",
        template: '<span><div class="onoffswitch" tabindex="0"><input type="checkbox" ng-true-value="\'true\'" ng-false-value="\'false\'" ng-model="ngModel" ng-disabled="ngDisabled" class="onoffswitch-checkbox" name="kc{{name}}" id="kc{{id}}"></div></span>',
        compile: function (element, attrs) {

            if (!attrs.onText) { attrs.onText = "ON"; }
            if (!attrs.offText) { attrs.offText = "OFF"; }

            element.bind('keydown click', function (e) {
                var code = e.keyCode || e.which;
                if (code === 32 || code === 13) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    $(e.target).find('input').click();
                }
            });
        }
    };
});
/**
 * Directive for presenting an ON-OFF switch for checkbox. The directive expects the value to be booleam true or false, not string 'true'/'false'
 * This directive provides some additional capabilities to the default onoffswitch such as:
 *
 * - Dynamic values for id and name attributes. Useful if you need to use this directive inside a ng-repeat
 * - Specific scope to specify the value. Instead of just true or false.
 *
 * Usage: <input ng-model="mmm" name="nnn" id="iii" kc-onoffswitch-model [on-text="ooo" off-text="fff"] />
 */
module.directive('onoffswitchboolean', function () {
    return {
        restrict: "EA",
        replace: true,
        scope: {
            name: '=',
            id: '=',
            value: '=',
            ngModel: '=',
            ngDisabled: '=',
            kcOnText: '@onText',
            kcOffText: '@offText'
        },
        template: '<span><div class="onoffswitch" tabindex="0"><input type="checkbox" ng-true-value="true" ng-false-value="false" ng-model="ngModel" ng-disabled="ngDisabled" class="onoffswitch-checkbox" name="kc{{name}}" id="kc{{id}}"></div></span>',
        compile: function (element, attrs) {

            if (!attrs.onText) { attrs.onText = "ON"; }
            if (!attrs.offText) { attrs.offText = "OFF"; }

            element.bind('keydown click', function (e) {
                var code = e.keyCode || e.which;
                if (code === 32 || code === 13) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    $(e.target).find('input').click();
                }
            });
        }
    };
});

/**
 * Directive for presenting an ON-OFF switch for checkbox. The directive expects the true-value or false-value to be string like 'true' or 'false', not boolean true/false.
 * This directive provides some additional capabilities to the default onoffswitch such as:
 *
 * - Specific scope to specify the value. Instead of just 'true' or 'false' you can use any other values. For example: true-value="'foo'" false-value="'bar'" .
 * But 'true'/'false' are defaults if true-value and false-value are not specified
 *
 * Usage: <input ng-model="mmm" name="nnn" id="iii" onoffswitchvalue [ true-value="'true'" false-value="'false'" on-text="ooo" off-text="fff"] />
 */
module.directive('onoffswitchvalue', function () {
    return {
        restrict: "EA",
        replace: true,
        scope: {
            name: '@',
            id: '@',
            trueValue: '@',
            falseValue: '@',
            ngModel: '=',
            ngDisabled: '=',
            kcOnText: '@onText',
            kcOffText: '@offText'
        },
        // TODO - The same code acts differently when put into the templateURL. Find why and move the code there.
        //templateUrl: "templates/kc-switch.html",
        template: "<span><div class='onoffswitch' tabindex='0'><input type='checkbox' ng-true-value='{{trueValue}}' ng-false-value='{{falseValue}}' ng-model='ngModel' ng-disabled='ngDisabled' class='onoffswitch-checkbox' name='{{name}}' id='{{id}}'><label for='{{id}}' class='onoffswitch-label'><span class='onoffswitch-inner'><span class='onoffswitch-active'>{{kcOnText}}</span><span class='onoffswitch-inactive'>{{kcOffText}}</span></span><span class='onoffswitch-switch'></span></label></div></span>",
        compile: function (element, attrs) {
            /*
             We don't want to propagate basic attributes to the root element of directive. Id should be passed to the
             input element only to achieve proper label binding (and validity).
             */
            element.removeAttr('name');
            element.removeAttr('id');

            if (!attrs.trueValue) { attrs.trueValue = "'true'"; }
            if (!attrs.falseValue) { attrs.falseValue = "'false'"; }

            if (!attrs.onText) { attrs.onText = "ON"; }
            if (!attrs.offText) { attrs.offText = "OFF"; }

            element.bind('keydown', function (e) {
                var code = e.keyCode || e.which;
                if (code === 32 || code === 13) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    $(e.target).find('input').click();
                }
            });
        }
    };
});

module.directive('kcInput', function () {
    var d = {
        scope: true,
        replace: false,
        link: function (scope, element, attrs) {
            var form = element.children('form');
            var label = element.children('label');
            var input = element.children('input');

            var id = form.attr('name') + '.' + input.attr('name');

            element.attr('class', 'control-group');

            label.attr('class', 'control-label');
            label.attr('for', id);

            input.wrap('<div class="controls"/>');
            input.attr('id', id);

            if (!input.attr('placeHolder')) {
                input.attr('placeHolder', label.text());
            }

            if (input.attr('required')) {
                label.append(' <span class="required">*</span>');
            }
        }
    };
    return d;
});

module.directive('kcEnter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if (event.which === 13) {
                scope.$apply(function () {
                    scope.$eval(attrs.kcEnter);
                });

                event.preventDefault();
            }
        });
    };
});

var getInitials = function (name) {
    var parts = name.split(' ');
    var partsLen = parts.length;
    if (partsLen > 2) {
        partsLen = 2;
    }
    var initials = '';
    for (var i = 0; i < partsLen; i++) {
        if (parts[i].length > 0 && parts[i] !== '') {
            initials += parts[i][0];
        }
    }
    return initials.toLowerCase();
};
function generateAvatar(text) {
    var colours = ["#1abc9c", "#2ecc71", "#3498db", "#9b59b6", "#34495e", "#16a085", "#27ae60", "#2980b9", "#8e44ad", "#2c3e50", "#f1c40f", "#e67e22", "#e74c3c", "#95a5a6", "#f39c12", "#d35400", "#c0392b", "#bdc3c7", "#7f8c8d"];
    var initials = getInitials(text);
    var charIndex = initials.charCodeAt(0) - 65,
        colourIndex = charIndex % 19;

    const canvas = document.createElement("canvas");
    const context = canvas.getContext("2d");

    canvas.width = 200;
    canvas.height = 200;

    // Draw background
    context.fillStyle = colours[colourIndex];
    context.fillRect(0, 0, canvas.width, canvas.height);

    // Draw text
    context.font = "300 100px Verdana";
    context.fillStyle = "white";
    context.textAlign = "center";
    context.textBaseline = "middle";
    context.fillText(initials, canvas.width / 2, canvas.height / 2);

    return canvas.toDataURL("image/png");
}

module.directive('kcUserSave', function ($compile, $timeout, Notifications, $translate) {
    var clickDelay = 500; // 500 ms

    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-primary");
            elem.attr("type", "submit");

            var disabled = false;
            elem.on('click', function (evt) {
                if ($scope.hasOwnProperty("changed") && !$scope.changed) return;

                // KEYCLOAK-4121: Prevent double form submission
                if (disabled) {
                    evt.preventDefault();
                    evt.stopImmediatePropagation();
                    return;
                } else {
                    disabled = true;
                    $timeout(function () { disabled = false; }, clickDelay, false);
                }

                $scope.$apply(function () {
                    var form = elem.closest('form');

                    if (form && form.attr('name')) {
                        var ngValid = form.find('.ng-valid');
                        if ($scope[form.attr('name')].$valid) {
                            if ($scope.user.mobileNumber && $scope.user.mobileNumber !== '') {
                                if (iti.isValidNumber()) {
                                    $scope.user.mobileNumberRegion = iti.getSelectedCountryData().iso2;
                                    ngValid.parent().removeClass('has-error');
                                    $scope['save']();
                                } else {
                                    Notifications.error($translate.instant("invalid-mobile-number-error"));
                                }
                            } else {
                                ngValid.parent().removeClass('has-error');
                                $scope['save']();
                            }
                        } else {
                            Notifications.error($translate.instant("input-missing-check-red-fields"))
                            //ngValid.removeClass('error');
                            ngValid.parent().removeClass('has-error');

                            var ngInvalid = form.find('.ng-invalid');
                            //ngInvalid.addClass('error');
                            ngInvalid.parent().addClass('has-error');
                        }
                    }
                });
            });
        }
    };
});
module.directive('kcAdminSave', function ($compile, $timeout, Notifications, $translate) {
    var clickDelay = 500; // 500 ms

    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-primary");
            elem.attr("type", "submit");

            var disabled = false;
            elem.on('click', function (evt) {

                // KEYCLOAK-4121: Prevent double form submission
                if (disabled) {
                    evt.preventDefault();
                    evt.stopImmediatePropagation();
                    return;
                } else {
                    disabled = true;
                    $timeout(function () { disabled = false; }, clickDelay, false);
                }
                $scope.$apply(function () {
                    var form = elem.closest('form');
                    if (form && form.attr('name')) {
                        var ngValid = form.find('.ng-valid');
                        if ($scope[form.attr('name')].$valid) {
                            if ($scope.user.mobileNumber && $scope.user.mobileNumber !== '') {
                                if (iti.isValidNumber()) {
                                    $scope.user.mobileNumberRegion = iti.getSelectedCountryData().iso2;
                                    ngValid.parent().removeClass('has-error');
                                    $scope['save']();
                                } else {
                                    Notifications.error($translate.instant("invalid-mobile-number-error"));
                                }

                            } else {
                                ngValid.parent().removeClass('has-error');
                                $scope['save']();
                            }
                        } else {
                            Notifications.error($translate.instant("input-missing-check-red-fields"))
                            //ngValid.removeClass('error');
                            ngValid.parent().removeClass('has-error');

                            var ngInvalid = form.find('.ng-invalid');
                            //ngInvalid.addClass('error');
                            ngInvalid.parent().addClass('has-error');
                        }
                    }
                });
            });
        }
    };
});
module.directive('kcSave', function ($compile, $timeout, Notifications, $translate) {
    var clickDelay = 500; // 500 ms

    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-primary");
            elem.attr("type", "submit");

            var disabled = false;
            elem.on('click', function (evt) {
                if ($scope.hasOwnProperty("changed") && !$scope.changed) return;

                // KEYCLOAK-4121: Prevent double form submission
                if (disabled) {
                    evt.preventDefault();
                    evt.stopImmediatePropagation();
                    return;
                } else {
                    disabled = true;
                    $timeout(function () { disabled = false; }, clickDelay, false);
                }

                $scope.$apply(function () {
                    var form = elem.closest('form');
                    if (form && form.attr('name')) {
                        var ngValid = form.find('.ng-valid');
                        if ($scope[form.attr('name')].$valid) {
                            //ngValid.removeClass('error');
                            ngValid.parent().removeClass('has-error');
                            $scope['save']();
                        } else {
                            Notifications.error($translate.instant("input-missing-check-red-fields"))
                            //ngValid.removeClass('error');
                            ngValid.parent().removeClass('has-error');

                            var ngInvalid = form.find('.ng-invalid');
                            //ngInvalid.addClass('error');
                            ngInvalid.parent().addClass('has-error');
                        }
                    }
                });
            });
        }
    };
});

module.directive('kcCopy', function ($translate) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {

            const copyElementId= elem[0].dataset['copyElementId']
            $scope.tooltipTxt = $translate.instant("copy");

            elem.on('click', function (evt) {

                $scope.$apply(function () {
                    const text = $(`#${copyElementId}`).val() || $(`#${copyElementId}`).text()
                    navigator.clipboard.writeText(text);
                    $scope.tooltipTxt = $translate.instant("copied");

                    elem.mouseleave(function () {
                        $scope.tooltipTxt = $translate.instant("copy");
                    });

                })

            });
        }    
    }
})   

module.directive('kcDrawFlow',function($translate){
    return{
        restrict: "E",
        scope: {
            kcTemplateFlow: '='
        },
        link: function ($scope, elem, attr, ctrl) {
            if ($scope.kcTemplateFlow) {
                // ************** Generate the tree diagram	 *****************
                var margin = { top: 60, right: 120, bottom: 20, left: 50 },
                    width = 960 - margin.right - margin.left,
                    height = 600 - margin.top - margin.bottom;

                var circle = { r: 35 };
                var rect = { h: 75, w: 250 };
                var circleTextPosition = 85;
                var depth = 120;

                var i = 0;
                var nodes, links;

                var tree, diagonal, svg, root;
                var init = function () {

                    d3.select("svg").remove();

                    tree = d3.layout.tree()
                        .size([height, width]);

                    diagonal = d3.svg.diagonal()
                        .projection(function (d) { return [d.x, d.y]; });

                    svg = d3.select("#drawflow").append("svg")
                        .attr("width", "100%")
                        .attr("height", "400%")
                        .append("g")
                        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                    var sortedItems = list_to_tree($scope.kcTemplateFlow.blocks);

                    root = sortedItems[0];
                    root.x0 = height / 2;
                    root.y0 = 0;

                    render();

                    d3.selectAll(".link")
                        .each(animate);
                };

                function isObject(obj) {
                    return obj != null && obj.constructor.name === "Object";
                }

                var list_to_tree = function (list) {
                    var map = {}, node, roots = [], i;

                    for (i = 0; i < list.length; i += 1) {
                        map[list[i].name] = i; // initialize the map
                        list[i].children = []; // initialize the children
                    }

                    for (i = 0; i < list.length; i += 1) {
                        node = list[i];
                        node.parent = isObject(node.parent) ? node.parent.name : node.parent;
                        if (node.parent !== "null") {
                            // if you have dangling branches check that map[node.parentId] exists
                            list[map[node.parent]].children.push(node);
                        } else {
                            roots.push(node);
                        }
                    }
                    return roots;
                };

                var animate = function () {
                    d3.select(this)
                        .transition()
                        .ease('linear')
                        .duration(1000)
                        .styleTween("stroke-dashoffset", function () {
                            return d3.interpolate(14, 0);
                        })
                        .each("end", animate);
                };

                var render = function () {

                    var levelWidth = [1];
                    var childCount = function (level, n) {

                        if (n.children && n.children.length > 0) {
                            if (levelWidth.length <= level + 1) levelWidth.push(0);

                            levelWidth[level + 1] += n.children.length;
                            n.children.forEach(function (d) {
                                childCount(level + 1, d);
                            });
                        }
                    };
                    childCount(0, root);
                    var newWidth = d3.max(levelWidth) * 25; // 25 pixels per line
                    tree = tree.size([height, newWidth]);

                    // Compute the new tree layout.
                    nodes = tree.nodes(root).reverse();
                    links = tree.links(nodes);

                    // Normalize for fixed-depth.
                    nodes.forEach(function (d) { d.y = d.depth * depth; });

                    // Declare the nodes…
                    var node = svg.selectAll("g.node")
                        .data(nodes, function (d) { return d.id || (d.id = ++i); });

                    // Enter the nodes.
                    var nodeEnter = node.enter().append("g")
                        .attr("class", "node")
                        .attr("transform", function (d) {
                            if (d.default) {
                                return "translate(" + d.x + "," + d.y + ")";
                            }
                            else {
                                return "translate(" + (d.x - (rect.w / 2)) + "," + (d.y - (rect.h / 2)) + ")";
                            }
                        });

                    // Draw Start and End Circle
                    nodeEnter.filter(function (d, i) {
                        return d.default;
                    }).append("circle")
                        .attr("r", circle.r)
                        .style("fill", "#000");

                    // Draw Provider Rectangle
                    nodeEnter.filter(function (d, i) {
                        return !d.default && !d.placeHolder;
                    }).append("rect")
                        .attr('width', rect.w)
                        .attr('height', rect.h)
                        .attr("rx", 5)
                        .style("fill", "#000");

                    // Draw Provider Rectangle
                    nodeEnter.filter(function (d, i) {
                        return !d.default && d.placeHolder;
                    }).append("rect")
                        .attr('width', rect.w)
                        .attr('height', rect.h)
                        .attr("rx", 5)
                        .style("fill", "#fff")
                        .style("stroke", "#000")
                        .style("stroke-width", 1)
                        .style("stroke-dasharray", ("10,3"));

                    // Draw Icon inside Start Circle
                    nodeEnter.filter(function (d, i) {
                        return d.default && d.parent == "null";
                    }).append('svg:foreignObject')
                        .attr("width", 30)
                        .attr("height", 30)
                        .attr("x", -15)
                        .attr("y", -15)
                        // .append("xhtml:body")
                        .append("xhtml:div")
                        .attr("class", "drawflow_content_node d-flex align-items-center justify-content-center h-100")
                        .append("xhtml:div")
                        .append("xhtml:div")
                        .attr("class", "d-flex justify-content-center text-center")
                        .html('<i class="os-icon os-icon-user" style="color:#fff"></i>');

                    // Draw Icon inside End Circle
                    nodeEnter.filter(function (d, i) {
                        return d.default && d.parent != "null";
                    }).append('svg:foreignObject')
                        .attr("width", 30)
                        .attr("height", 30)
                        .attr("x", -15)
                        .attr("y", -15)
                        .append("xhtml:div")
                        .attr("class", "drawflow_content_node d-flex align-items-center justify-content-center h-100")
                        .append("xhtml:div")
                        .append("xhtml:div")
                        .attr("class", "d-flex justify-content-center text-center")
                        .html('<i class="os-icon os-icon-check" style="color:#fff"></i>');


                    // Draw Text Next to Start and End Circle
                    nodeEnter.filter(function (d, i) {
                        return d.default;
                    }).append("text")
                        .attr("x", function (d) {
                            return 55;
                        })
                        .attr("dy", ".35em")
                        .attr("text-anchor", "middle")
                        .text(function (d) { return $translate.instant(d.name); })
                        .style("font-size", "14px")
                        .style("font-weight", 400)
                        .style("font-family", "Inter");

                    // Draw Text Inside Provider Rectangle
                    nodeEnter.filter(function (d, i) {
                        return !d.default && !d.placeHolder;
                    }).append("svg:foreignObject")
                        .attr("width", rect.w)
                        .attr("height", rect.h)
                        .attr("x", 0)
                        .attr("y", 0)
                        .html(function (d) {
                            return "<div class='drawflow_content_node d-flex align-items-center justify-content-center h-100'><div class='draw-activity-box'><div class='draw-activity-info d-flex align-items-center top-0 justify-content-center text-center'><span class='text-white'>" + $translate.instant(d.name) + "</span> </div></div></div>";
                        })

                    nodeEnter.filter(function (d, i) {
                        return !d.default && d.placeHolder;
                    }).append("svg:foreignObject")
                        .attr("width", rect.w)
                        .attr("height", rect.h)
                        .attr("x", 0)
                        .attr("y", 0)
                        .html(function (d) {
                            return "<div class='drawflow_content_node d-flex align-items-center justify-content-center h-100'><div class='draw-activity-box'><div class='draw-activity-info d-flex align-items-center top-0 justify-content-center text-center'><span class='c-light-gray'>" + $translate.instant(d.name) + "</span> </div></div></div>";
                        })

                    // Declare the links…
                    var link = svg.selectAll("path.link")
                        .data(links, function (d) { return d.target.id; });

                    // Enter the links.
                    link.enter().insert("path", "g")
                        .attr("class", "link")
                        .attr({
                            "d": diagonal
                        });

                    nodes.forEach(function (d) {
                        d.x0 = d.x;
                        d.y0 = d.y;
                    });
                };

                init();
            }

        },
        templateUrl: resourceUrl + "/templates/kc-draw-flow.html"
    };
});

module.directive('kcReset', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-secondary");
            elem.attr("type", "submit");
            elem.bind('click', function () {
                $scope.$apply(function () {
                    var form = elem.closest('form');
                    if (form && form.attr('name')) {
                        form.find('.ng-valid').removeClass('error');
                        form.find('.ng-invalid').removeClass('error');
                        $scope['reset']();
                    }
                });
            });
        }
    };
});

module.directive('kcCancel', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-secondary");
            elem.attr("type", "submit");
        }
    };
});

module.directive('kcDelete', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-danger");
            elem.attr("type", "submit");
        }
    };
});


module.directive('kcDropdown', function ($compile, Notifications) {
    return {
        scope: {
            kcOptions: '=',
            kcModel: '=',
            id: "=",
            kcPlaceholder: '@'
        },
        restrict: 'EA',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-select.html',
        link: function (scope, element, attr) {
            scope.updateModel = function (item) {
                scope.kcModel = item;
            };
        }
    };
});

module.directive('kcReadOnly', function () {
    var disabled = {};

    var d = {
        replace: false,
        link: function (scope, element, attrs) {
            var disable = function (i, e) {
                if (!e.disabled) {
                    disabled[e.tagName + i] = true;
                    e.disabled = true;
                }
            };

            var enable = function (i, e) {
                if (disabled[e.tagName + i]) {
                    e.disabled = false;
                    delete disabled[i];
                }
            };

            var filterIgnored = function (i, e) {
                return !e.attributes['kc-read-only-ignore'];
            };

            scope.$watch(attrs.kcReadOnly, function (readOnly) {
                if (readOnly) {
                    element.find('input').filter(filterIgnored).each(disable);
                    element.find('button').filter(filterIgnored).each(disable);
                    element.find('select').filter(filterIgnored).each(disable);
                    element.find('textarea').filter(filterIgnored).each(disable);
                } else {
                    element.find('input').filter(filterIgnored).each(enable);
                    element.find('input').filter(filterIgnored).each(enable);
                    element.find('button').filter(filterIgnored).each(enable);
                    element.find('select').filter(filterIgnored).each(enable);
                    element.find('textarea').filter(filterIgnored).each(enable);
                }
            });
        }
    };
    return d;
});

module.directive('kcMenu', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-menu.html',
        link: link
    };
    $scope.helpTxt = "Help";
    function link(scope, element) {
        var menu_timer;
        $('.menu-activated-on-hover').on('mouseenter', 'ul.main-menu > li.has-sub-menu', function () {
            var $elem = $(this);
            clearTimeout(menu_timer);
            $elem.closest('ul').addClass('has-active').find('> li').removeClass('active');
            $elem.addClass('active');
        });

        $('.menu-activated-on-hover').on('mouseleave', 'ul.main-menu > li.has-sub-menu', function () {
            var $elem = $(this);
            menu_timer = setTimeout(function () {
                $elem.removeClass('active').closest('ul').removeClass('has-active');
            }, 30);
        });
        $('.mobile-menu-trigger').on('click', function () {
            $('.menu-mobile .menu-and-user').slideToggle(200, 'swing');
            return false;
        });

        // INIT MENU TO ACTIVATE ON CLICK
        $('.menu-activated-on-click').on('click', 'li.has-sub-menu > a', function (event) {
            var $elem = $(this).closest('li');
            if ($elem.hasClass('active')) {
                $elem.removeClass('active');
            } else {
                $elem.closest('ul').find('li.active').removeClass('active');
                $elem.addClass('active');
            }
            return false;
        });
    }
});

module.directive('kcAuthSubMenu', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-auth-sub-menu.html'
    };
});

module.directive('kcSettingsMenu', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-settings-menu.html',
        resolve: {
            serverInfo: function (ServerInfoLoader) {
                return ServerInfoLoader();
            }
        }
    };
});

module.directive('kcMarketplaceSubMenu', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-marketplace-sub-menu.html'
    };
});

module.directive('kcTabsRealm', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-realm.html'
    };
});
module.directive('kcTabsBoMarketplace', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-bo-marketplace.html'
    };
});
module.directive('kcTabsMarketPlace', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-market-place.html'
    };
});
module.directive('kcTabsApplications', function() {
    return {
        scope: true,
        restrict: 'E',
        replalce: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-applications.html'
    }
})
module.directive('kcApplicationTabBo', function() {
    return {
        scope: true,
        restrict: 'E',
        replalce: true,
        templateUrl: resourceUrl + '/templates/kc-application-tab-bo.html'
    }
})
module.directive('kcSubtabsCustomApplications', function() {
    return {
        scope: true,
        restrict: 'E',
        replalce: true,
        templateUrl: resourceUrl + '/templates/kc-subtabs-custom-applications.html'
    }
})
module.directive('kcTabsEmailProviders', function() {
    return {
        scope: true,
        restrict: 'E',
        replalce: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-email-providers.html'
    }
})
module.directive('kcTabsSmsProviders', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-sms-providers.html'
    };
});
module.directive('kcTabsConnection', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-connection.html'
    };
});

module.directive('kcTabsAuthentication', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-authentication.html'
    };
});

module.directive('kcTabsMarketplace', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-marketplace.html'
    };
});
module.directive('kcTabsMarketPlaceFlow', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-market-place-flow.html'
    };
});

module.directive('kcTabsRole', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-role.html'
    };
});

module.directive('kcTabsClientRole', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-client-role.html'
    };
});
module.directive('brandingHeaderPreview', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/branding-header-preview.html'
    };
});

module.directive('kcTabsUser', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-user.html'
    };
});

module.directive('kcTabsUsers', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-users.html'
    };
});

module.directive('kcTabsAdminUser', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-admin-user.html'
    };
});

module.directive('kcTabsAdminUsers', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-admin-users.html'
    };
});

module.directive('kcTabsClients', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-clients.html'
    };
});
module.directive('kcTabsManagement', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-management.html'
    };
});
module.directive('kcTabsApp', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-app.html'
    };
});
module.directive('kcCreateTemplate', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-create-template.html'
    };
});
module.directive('kcMarketplaceWorkflowsList', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-marketplace-workflows-list.html'
    };
});
module.directive('kcMarketplaceDetails', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-marketplace-details.html'
    };
});
module.directive('kcMarketplaceFlow', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-marketplace-flow.html'
    };
});
module.directive('kcCibaPolicy', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-ciba-policy.html'
    };
});
module.directive('kcRoleMappings', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-role-mappings.html'
    };
});
module.directive('kcUserGroupMembership', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-user-group-membership.html'
    };
});
module.directive('kcRolesGroupsTree', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-roles-groups-tree.html'
    };
});
module.directive('kcRealmRoleUsersList', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-realm-role-users-list.html'
    };
});
module.directive('kcRolesUsersTree', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-roles-users-tree.html'
    };
});

module.directive('kcRolesTree', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-roles-tree.html'
    };
});

module.directive('kcGroupRole', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-group-role.html'
    };
});

module.directive('kcGroupMembers', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-group-members.html'
    };
});

module.directive('kcCibaTutorial', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-ciba-tutorial.html'
    };
});

module.directive('kcTabsSsoClient', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-sso-client.html'
    };
});

module.directive('kcTabsSsoApp', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-sso-app.html'
    };
});

module.directive('kcPolicyHeader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-policy-header.html'
    };
});

module.directive('kcBrandingHeader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-branding-header.html'
    };
});

module.directive('kcTabsGroup', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-group.html'
    };
});

module.directive('kcTabsGroupList', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-group-list.html'
    };
});

module.directive('kcTabsClient', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-client.html'
    };
});

module.directive('kcTabsClientScope', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-client-scope.html'
    };
});

module.directive('kcNavigationUser', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-navigation-user.html'
    };
});

module.directive('kcTabsIdentityProvider', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-identity-provider.html'
    };
});

module.directive('kcTabsUserFederation', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-user-federation.html'
    };
});

module.directive('kcTabsLdap', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-ldap.html'
    };
});

module.directive('kcTabsEvents', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-events.html'
    };
});

module.directive('kcTabsKeys', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-keys.html'
    };
});

module.directive('kcTabsRoles', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-roles.html'
    };
});

module.directive('kcTabsSecuritydefenses', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-securitydefenses.html'
    };
});
module.directive('kcFileUpload', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-file-upload.html',
        link:function(scope,elem,attrs){
            scope.name = attrs.name;
        }
    };
});

module.directive('kcApiProviders', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-api-providers.html'
    };
});

module.directive('kcApiVerifiers', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-api-verifiers.html'
    };
});

module.directive("mdPreview", function () {
    return {
        template: "<div class='MdPreview-html' ng-bind-html='htmlSafe' /></div>",
        restrict: 'C',
        replace: true,
        controller: 'MarkdownCtrl',
        scope: {},
        link: function (scope, element, attrs) {
            if (attrs.url) {
                scope.initFromUrl(attrs.url);
            }
        }
    };
});
module.controller('MarkdownCtrl', function ($scope, $window, $http, $sce) {
    $scope.md2Html = function () {
        $scope.html = $window.marked.parse($scope.markdown);
        $scope.htmlSafe = $sce.trustAsHtml($scope.html);
        const mdContainer = document.querySelector(".mdTrigger");
        if (mdContainer) {
            mdContainer.click();
        }
    };

    $scope.notFound = (url, action) => {
        const noFileId = url.includes("overview") ? "mdFileNotFoundOverview" : url.includes("installation") ? "mdFileNotFoundInstallation" : "mdFileNotFound";
        const noFileElement = document.getElementById(noFileId);
        if (noFileId && noFileElement && action === "add") {
            noFileElement.classList.add("d-none");
        } else if (noFileId && noFileElement) {
            noFileElement.classList.remove("d-none");
        }
    };

    $scope.initFromUrl = function (url) {
        $.ajax({
            "url": url,
            "method": "GET",
            "timeout": 0,
        })
            .done(function (response) {
                $scope.markdown = response;
                $scope.md2Html();
                $scope.notFound(url, "add");
            }).fail(function () {
                $scope.notFound(url, "remove");
            });
    };
});



module.controller('RoleSelectorModalCtrl', function ($scope, realm, config, configName, RealmRoles, Client, ClientRole, $modalInstance) {
    $scope.selectedRealmRole = {
        role: undefined
    };
    $scope.selectedClientRole = {
        role: undefined
    };
    $scope.client = {
        selected: undefined
    };

    $scope.selectRealmRole = function () {
        config[configName] = $scope.selectedRealmRole.role.name;
        $modalInstance.close();
    };

    $scope.selectClientRole = function () {
        config[configName] = $scope.selectedClient.clientId + "." + $scope.selectedClientRole.role.name;
        $modalInstance.close();
    };

    $scope.cancel = function () {
        $modalInstance.dismiss();
    };

    $scope.$on('$routeChangeStart', function () {
        $modalInstance.close();
    });

    clientSelectControl($scope, realm.realm, Client);

    $scope.selectedClient = null;

    $scope.changeClient = function (client) {
        $scope.selectedClient = client;
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }
        if ($scope.selectedClient) {
            ClientRole.query({ realm: realm.realm, client: $scope.selectedClient.id }, function (data) {
                $scope.clientRoles = data;
            });
        } else {
            console.log('selected client was null');
            $scope.clientRoles = null;
        }

        $scope.selectedClient = client;
    };

    RealmRoles.query({ realm: realm.realm }, function (data) {
        $scope.realmRoles = data;
    });
});

module.controller('loaderCtrl', function ($scope) {
});

module.controller("actionButtonLoadingCtrl", function ($scope, $translate) {
    $scope.finalLoadingName = $scope.loadername
        ? $scope.loadername
        : $translate.instant('loading');;
});

module.controller("SetupGuideCtrl", function ($scope, $translate, SetupGuides, $rootScope, $timeout, $sce) {
    $scope.init = function () {
        $scope.displayGuide = false;
        $scope.loaderTypeName = "setupGuideLoader";
        $scope.closeGuide = SetupGuides.close;
        $scope.setupGuideUrl = function () {
            if ($rootScope.setupguide.guideUrl === 'invalidUrl') {
                $('#setup-guide-support').remove();
                $('#setup-guide-support-nocookies').remove();
            } else {
                if (isThirdPartyCookieAllowed) {
                    $('#setup-guide-support-nodata').remove();
                    $('#setup-guide-support-nocookies').remove();
                } else {
                    $('#setup-guide-support').remove();
                    $('#setup-guide-support-nodata').remove();
                }
            }
            return $rootScope.setupguide.guideUrl;
        };
        $scope.editedUrl = function (updateUrl) {
            let finalUrl = $sce.valueOf(updateUrl).replace('/embed', '/workflow');
            return finalUrl;
        };
        $scope.duplicateLoaderTimer = $timeout(function () {
            $scope.$apply(
                function () {
                    $scope.displayGuide = true;
                }
            );
        }, 3000);
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
        if ($scope.duplicateLoaderTimer) {
            $timeout.cancel($scope.duplicateLoaderTimer);
        }
        $scope.closeGuide();
    });
});

module.controller("SetupGuideBoxCtrl", function ($scope, $translate, $rootScope, $timeout, SetupGuideReq, $sce) {
    $scope.init = function () {
        $scope.resourceUrl = resourceUrl;
        $scope.finalMapperGuides = SetupGuideReq.getData();
        $scope.displayGuide = false;
        $scope.loaderTypeName = "setupGuideLoader";
        $scope.duplicateLoaderTimer = $timeout(function () {
            $scope.$apply(
                function () {
                    $scope.displayGuide = true;
                }
            );
        }, 3000);
    };

    let checkCookies = function () {
        if (isThirdPartyCookieAllowed) {
            $('#setup-guide-nocookies').remove();
        } else {
            $('#setup-guide-iframe').remove();
        }
    };

    $scope.findMappersMatch = function () {
        let { urlId, urlGroup, urlType } = $rootScope.setupGuideDetails;
        let commonGroups = ['social', 'audit'];
        if (urlGroup === "marketplace" && $scope.finalMapperGuides?.marketPlace?.[urlId]?.[urlType]) {
            $('#setup-guide-nodata').remove();
            checkCookies();
            return $sce.trustAsResourceUrl($scope.finalMapperGuides?.marketPlace?.[urlId]?.[urlType]);
        } else if (urlId && urlGroup && commonGroups.includes(urlGroup) && $scope.finalMapperGuides?.setupGuides?.[urlGroup]?.[urlId]) {
            $('#setup-guide-nodata').remove();
            checkCookies();
            return $sce.trustAsResourceUrl($scope.finalMapperGuides?.setupGuides?.[urlGroup]?.[urlId]);
        } else {
            $('#setup-guide-nocookies').remove();
            $('#setup-guide-iframe').remove();
        }
    };

    $scope.editedUrl = function () {
        let { urlId, urlGroup, urlType } = $rootScope.setupGuideDetails;
        let commonGroups = ['social', 'audit'];
        let finalUrl = '';
        if (urlGroup === "marketplace" && $scope.finalMapperGuides?.marketPlace?.[urlId]?.[urlType]) {
            finalUrl = $scope.finalMapperGuides?.marketPlace?.[urlId]?.[urlType];
        } else if (urlId && urlGroup && commonGroups.includes(urlGroup) && $scope.finalMapperGuides?.setupGuides?.[urlGroup]?.[urlId]) {
            finalUrl = $scope.finalMapperGuides?.setupGuides?.[urlGroup]?.[urlId];
        }
        ;
        return finalUrl.replace('/embed', '/workflow');
    };

    $scope.$on("$routeChangeStart", function () {
        if ($scope.duplicateLoaderTimer) {
            $timeout.cancel($scope.duplicateLoaderTimer);
        }
    });
})
module.controller('ProviderConfigCtrl', function ($modal, $timeout, $document, $scope, $route, ComponentUtils, Notifications, Client, Policies, $http, TextEditor, $rootScope, ShowSecret, $location, Dialog, $translate, ProcessInput, CreateVersion, TermsConditionsVersion) {
    $scope.resourceUrl = resourceUrl;
    $scope.versionsCopy = angular.copy($scope.versions)
    $scope.showDeleteVersion = false;
    $scope.showVersion = true;
    $scope.path = $location.path().substring(1).split("/");
    $scope.preferredGroupOptions = {};
    $scope.multiGroupedOptions = {};
    $scope.multiGroupedOptionsCopy = {};
    $scope.showingGroupOption = {};
    $scope.selectedGroupIndex = {};
    $scope.preferredMultivaluedObject = {};
    $scope.MultivaluedObjectOptions = {};
    $scope.showingObjectListOptions = {};

    $timeout(function(){
        if($scope.properties && $scope.properties.length > 0){
            for (const prop of $scope.properties) {
                if(prop.type === "MultivaluedList" && (prop.name && (prop.name.includes("-claims") || prop.name.includes("digilocker-list-of-docs")))) {
                    $(`#qv-input__${prop.name.toLowerCase()}`).on('select2-removing', function (e) {
                        if(prop.defaultValue && prop.defaultValue.length > 0 && prop.defaultValue.includes(e.val)) {
                            e.preventDefault();
                            $scope.$apply(function(){
                            if(prop.name && prop.name.includes("digilocker-list-of-docs")) {
                                Notifications.error($translate.instant("default-document-value-cannot-be-removed"));
                            } else {
                                Notifications.error($translate.instant("claim-default-value-cannot-be-removed"));
                                }
                            })
                        }
                    });
                } else if(prop.type === "MultiValuedGroupSelect") {
                    try {
                        if(typeof $scope.configs[prop.name] === "string" && $scope.configs[prop.name].length == 0){
                            $scope.preferredGroupOptions[prop.name] = [];
                        } else if (typeof $scope.configs[prop.name] === "string" && $scope.configs[prop.name].length > 0) {
                            $scope.preferredGroupOptions[prop.name] = JSON.parse($scope.configs[prop.name]);
                        } else {
                            $scope.preferredGroupOptions[prop.name] = $scope.configs[prop.name];
                        }
                    } catch (error) {
                        $scope.preferredGroupOptions[prop.name] = [];
                    }
                    $scope.multiGroupedOptions[prop.name] = angular.copy(prop.groupOptions);
                    $scope.multiGroupedOptionsCopy[prop.name] = angular.copy($scope.multiGroupedOptions[prop.name]);
                    if($scope.preferredGroupOptions[prop.name].length == 0){
                        $scope.selectedGroupIndex[prop.name] = [-1];   
                    }else{
                        $scope.selectedGroupIndex[prop.name] = [];   
                        $scope.preferredGroupOptions[prop.name].forEach(option => {
                            $scope.multiGroupedOptionsCopy[prop.name].forEach((subArray, i) => {
                                if (subArray.some(item => item.name === option.name)) {
                                    $scope.multiGroupedOptions[prop.name][i] =  $scope.multiGroupedOptions[prop.name][i].filter(op => op.name != option.name);
                                    if (
                                        !$scope.selectedGroupIndex[prop.name].includes(i) &&
                                        $scope.preferredGroupOptions[prop.name].every(preferredItem =>
                                            subArray.some(item => item.name === preferredItem.name)
                                        )
                                    ) {
                                        $scope.selectedGroupIndex[prop.name].push(i);
                                    }
                                }
                            })
                        })
                    }
                    $scope.showingGroupOption[prop.name] = false;
                } else if (prop.type ==='MultivaluedObjectList') {
                    try {
                        if(typeof $scope.configs[prop.name] === "string" && $scope.configs[prop.name].length == 0){
                            $scope.preferredMultivaluedObject[prop.name] = [];
                        } else if (typeof $scope.configs[prop.name] === "string" && $scope.configs[prop.name].length > 0) {
                            $scope.preferredMultivaluedObject[prop.name] = JSON.parse($scope.configs[prop.name]);
                        } else {
                            $scope.preferredMultivaluedObject[prop.name] = $scope.configs[prop.name];
                        }
                    } catch (error) {
                        $scope.preferredMultivaluedObject[prop.name] = [];
                    }
  
                    if($scope.preferredMultivaluedObject[prop.name].length){
                        $scope.MultivaluedObjectOptions[prop.name] = prop.mapOptions.filter(obj => !$scope.preferredMultivaluedObject[prop.name].some(o => o.name === obj.name));
                    }else{
                        $scope.MultivaluedObjectOptions[prop.name] = prop.mapOptions;
                    }
                    $scope.showingObjectListOptions[prop.name] = false;
                }
            } 
        }
    }, 0)

    $scope.setShowVersion = () => {
        $scope.showVersion = !$scope.showVersion;
    }

    if ($scope.$parent.action?.id) {
        $scope.actionId = $scope.$parent.action?.id
    }

    $scope.init = function (config, properties, oldconfigvalue) {
        $scope.oldConfigs = angular.copy(config);
        $scope.configs = config;
        $scope.properties = properties;
        $scope.oldconfigvalue = oldconfigvalue;
        $scope.url = $location.absUrl();
    };

    $scope.openTextEditor = (name) => {
        const contentJson = $scope.config[name] ? JSON.parse($scope.config[name]) : {}
        TextEditor.open(contentJson, (content)=>{
            $scope.$parent.config[name] = JSON.stringify(content);
        }, $scope.config.isReadOnly);
    }

    $scope.checkNewVersion = () => {
        return  $scope.versions.includes(newTcVersion);
    }

    $scope.updateVersion = (version) => {
        $scope.$parent.config.isReadOnly = !(version === newTcVersion);
        $scope.$parent.config['terms_and_condition_ver'] = version;
        $scope.setShowVersion();
        if (version && newTcVersion != version)
            TermsConditionsVersion.query({realm: $route.current.params.realm , id: $scope.actionId, version: version}, function(res){
                {
                    const type = res.termsAndConditions[0].type;
                    const content = res.termsAndConditions[0].content;
                    fileName = res.fileName;
                    updatedAt = res.createdAt;
                    $scope.$parent.config['terms_and_condition_type'] = type;
                    switch(type) {
                        case "Link":
                            $scope.$parent.config['terms_and_condition_link'] = content;
                            $scope.$parent.oldconfigvalue['terms_and_condition_link'] = content;
                        break;
                        case "PDF":
                            $scope.$parent.config['terms_and_condition_pdf'] = content;
                            $scope.$parent.oldconfigvalue['terms_and_condition_pdf'] = content;
                            break;
                        case "Image":
                            $scope.$parent.config['terms_and_condition_image'] = content;
                            $scope.$parent.oldconfigvalue['terms_and_condition_image'] = content;
                        break;
                        case "Text":
                            $scope.$parent.config['terms_and_condition_text'] = content;
                            $scope.$parent.oldconfigvalue['terms_and_condition_text'] = content;
                        break;
                    }
                }
            })
    }

    $scope.deleteVersion = (version) => {
        if($scope.versions.length == 1){
            Notifications.error($translate.instant("atleast-one-version-required"));
            return;
        }
        $http.delete(`${authUrl}/admin/realms/${$route.current.params.realm}/authentication/required-action/${$scope.actionId}/${version}`).then(() => {
            const index = $scope.versions.indexOf(version)
            $scope.versions.splice(index, 1);
            var currentVersion = $scope.$parent.config['terms_and_condition_ver'] === version ? $scope.versions[0] : $scope.$parent.config['terms_and_condition_ver'];
            if(currentVersion) {
                $scope.updateVersion(currentVersion);
            } else {
                $scope.updateVersion("")
            }
        })
    }

    $scope.openCreateVersion = () => {
        CreateVersion.open((version)=>{
            var versionFormatRegExp = /^\d+\.\d+\.\d+$/;
            if (!versionFormatRegExp.test(version)) {
                Notifications.error($translate.instant("incorrect-version-format"));
                return ;
            }
            if($scope.versions.includes(version.trim())){
                Notifications.error($translate.instant("version-exist"));
                return ;
            }
            $scope.versions.push(version)
            Object.keys($scope.config).map(key => {
                if (key.startsWith('terms')) {
                    $scope.$parent.config[key] = "";
                }
            })
            $scope.$parent.config["terms_and_condition_ver"] = version
            newTcVersion = version;
            $scope.updateVersion(version)
        })
    }

    $scope.checkInputValid = function (configVal, prop) {
        if(configVal[prop.name] && ( prop.maxVal || prop.maxVal == 0 ) && (prop.minVal || prop.minVal == 0)){
            if(Number(configVal[prop.name]) > prop.maxVal){
                configVal[prop.name] = prop.maxVal.toString();
            }else if(Number(configVal[prop.name]) < prop.minVal){
                configVal[prop.name] = prop.minVal.toString();
            }
        }
    }

    $scope.navigateToLink = function (link, id) {
        let url = link.replace("{{id}}", id);
        $location.path(url);
    }

    $scope.validateExpiryTime = function (configKey) {
        let value = $scope.config[configKey];
        if (value !== undefined && value.trim() === '') {
            $scope.configs.isvalidExpiryTime = true;
            return;
        }
        const ExpiryTimeRegex = new RegExp(/^\d+[smhd]$/g); 
        if (ExpiryTimeRegex.test(value)) {
            $scope.configs.isvalidExpiryTime = true;
        } else {
            $scope.configs.isvalidExpiryTime = false;
        }
    }

    $scope.translate = function(text) {
        let translated = $translate.instant(text)
        translated = translated + (translated.includes("{{") ? '}}' : '');
        return translated;
    }

    clientSelectControl($scope, $route.current.params.realm, Client);
    $scope.fileNames = {};
    $scope.newMapEntries = {};
    var cachedMaps = {};
    var cachedParsedMaps = {};
    var focusMapValueId = null;
    $scope.policies = [];
    $scope.newValue = "";
    // imageMaxSize in kb
    $scope.imageMaxSize = 2048;
    // KEYCLOAK-4463
    $scope.initEditor = function (editor) {
        editor.$blockScrolling = Infinity; // suppress warning message
    };

    $scope.convertStringToNumber = function (val) {
        return Number(val);
    };
    $scope.showAddInput = function (option, config) {
        if (option && option.maxVal && config.length === option.maxVal) {
            return false;
        }
        return true;
    };

    $scope.initPolicyStreams = function () {
        Policies.query({ realm: $route.current.params.realm }, function (response) {
            for (const policy of response) {
                if (policy.policyType.toLowerCase() === "password") {
                    $scope.policies.push(policy.policyName);
                }
            }
        });
    };
    $scope.openImageUploader = function (id) {
        $timeout(() => {
            const input = document.getElementById(id);
            try {
                input.click();
            } catch (e) {
                console.error('Illegal click attempt:', e);
            }
        });
    };

    $scope.openFontFileUploader = function (id) {
        $timeout(() => {
            const input = document.getElementById(id);
            try {
                input.click();
            } catch (e) {
                console.error('Illegal click attempt:', e);
            }
    });
    };
    $(document).ready(function () {
        $("#bodyBgImage").change(function () {
            const imgFile = this.files[0];
            if (imgFile) {
                $scope.setImageToPreview(imgFile, 'bodyBgImageFile', '--body-bg-img', 'isBodyBgImageValid', 'bodyBgImage');
            }
        });
    });

    $(document).ready(function () {
        if($scope.config && $scope.config.hasOwnProperty("customFontFile") && $scope.config["customFontFile"] && $scope.config["customFontFile"].length){
            $("#customFontFile-fileName").val($scope.config["customFontFile"].split('/').pop());
        }
        const fontFileFormat = [".ttf", ".otf", ".woff", ".woff2"];
        $("#customFontFile").change(function () {
            const fontFile = this.files[0];
            var containsExtension = fontFileFormat.some(extension => fontFile.name.toLowerCase().replaceAll(" ", "-").includes(extension));
            if(!containsExtension){
                Notifications.error($translate.instant("invalid-font-file-format"));
                $scope.$apply();
                return ;
            }
            convertToBase64(fontFile)
                .then(base64String => {
                    $scope.config["customFontFile"] = `name=${fontFile.name.toLowerCase().replaceAll(" ", "-")};${base64String}`;
                    $("#customFontFile-fileName").val(fontFile.name.toLowerCase().replaceAll(" ", "-"));
                })
                .catch(error => {
                    console.error("Error converting file to Base64:", error);
                });

            const reader = new FileReader();
            reader.onload = function (e) {
                const fontBlob = new Blob([e.target.result], { type: fontFile.type });
                const fontUrl = URL.createObjectURL(fontBlob);
                const existingStyle = document.querySelector('style[data-font="MyCustomFontFile"]');
                if (existingStyle) {
                    existingStyle.remove();
                }

                const style = document.createElement('style');
                style.setAttribute('data-font', 'MyCustomFontFile');
                style.innerHTML = `
                    @font-face {
                        font-family: 'MyCustomFont';
                        src: url(${fontUrl});
                        font-display: block;
                    }
                `;
                document.head.appendChild(style);
                $scope.configs.bodyFont = 'MyCustomFont';
                $scope.$apply();
            };
            reader.readAsArrayBuffer(fontFile);
        });
    });

    function convertToBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                const base64String = reader.result;
                resolve(base64String);
            };
            reader.onerror = (error) => {
                reject(error);
            };
            reader.readAsDataURL(file);
        });
    }

    $(document).ready(function () {
        $("#themeLayoutBgImage").change(function () {
            const imgFile = this.files[0];
            if (imgFile) {
                $scope.setImageToPreview(imgFile, 'themeLayoutBgImageFile', '--section-bg-image', 'isThemeBgImageValid', 'themeLayoutBgImage');
            }
        });
    });
    $scope.setImageToPreview = function (imgFile, propFileName, cssProperty, propValidName, modelName) {
        $scope.config[propFileName] = imgFile;
        document.documentElement.style.setProperty(cssProperty, 'url(' + window.URL.createObjectURL(imgFile) + ')');
        checkImageSize($scope.config, propValidName, imgFile, $scope.imageMaxSize);
        $rootScope.$emit("imageUpdated", {name : imgFile.name , isChanged : true, modelName});
    };
    $scope.initSelectedClient = function (configName, config) {
        if (config[configName]) {
            $scope.selectedClient = null;
            Client.query({ realm: $route.current.params.realm, search: false, clientId: config[configName], max: 1 }, function (data) {
                if (data.length > 0) {
                    $scope.selectedClient = angular.copy(data[0]);
                    $scope.selectedClient.text = $scope.selectedClient.clientId;
                }
            });
        }
    };
    $scope.openRoleSelector = function (configName, config) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/role-selector.html',
            controller: 'RoleSelectorModalCtrl',
            backdrop: 'static',
            resolve: {
                realm: function () {
                    return $scope.realm;
                },
                config: function () {
                    return config;
                },
                configName: function () {
                    return configName;
                }
            }
        });
    };

    $scope.changeClient = function (configName, config, client) {
        if (!client || !client.id) {
            config[configName] = null;
            $scope.selectedClient = null;
            return;
        }
        $scope.selectedClient = client;
        config[configName] = client.clientId;
    };

    $scope.disablePaste = function (e) {
        e.preventDefault();
        return false;
    };

    $scope.secretValue = function (configKey) {
        if($scope.configs[configKey] === "**********" ){
            $scope.configs[configKey] = '';
        }
    }

    $scope.emptyValue = function (config, option) {
        $scope.properties.forEach(function(data) {
            if (data.defaultValue && option.name !== data.name) {
                $scope.config[data.name] = data.defaultValue;
            } else if (option.name !== data.name) {
                $scope.config[data.name] = null;
            }
        });
    }

    ComponentUtils.convertAllMultivaluedStringValuesToList($scope.properties, $scope.config);
    ComponentUtils.convertBackToMultivaluedList($scope.properties, $scope.config);
    ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.properties, $scope.config);
    $scope.addValueToMultivalued = function (option, val) {
        let inputEle = $("#qv-newInput__" + option.name.toLowerCase());
        if (inputEle.val() === '' || inputEle.val() === undefined || inputEle.val() === null) {
            return;
        }
        if ($scope.config[option.name] === undefined ){
            $scope.config[option.name] = [];
        }
        var configProperty = $scope.config[option.name];
        configProperty.push(val);
        inputEle.val('');
    };

    $scope.deleteValueFromMultivalued = function (optionName, index) {
        $scope.config[optionName].splice(index, 1);
    };

    $scope.uploadFile = function ($files, optionName, config, skipConversion) {
        var reader = new FileReader();
        reader.onload = function (e) {
            $scope.$apply(function () {
                if (skipConversion) {
                    config[optionName][0] = e.target.result;
                } else {
                    getBase64($files[0]).then(
                        data => {
                            config[optionName] = data.split("base64,")[1];
                        });
                }
            });
        };
        reader.readAsText($files[0]);
        $scope.fileNames[optionName] = $files[0].name;
    };
    function getBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.fileName = file.name
            reader.readAsDataURL(file);
            reader.onload = () => resolve(reader.result);
            reader.onerror = error => reject(error);
        });
    }

    $scope.addMapEntry = function (optionName) {
        $scope.removeMapEntry(optionName, $scope.newMapEntries[optionName].key);

        $scope.config[optionName].push($scope.newMapEntries[optionName]);

        delete $scope.newMapEntries[optionName];
    };

    $scope.checkConfigValid = function (config, property) {
        if (property.dependsOn) {
            var conditions = [];
            for (const dependsKey of Object.keys(property.dependsOn)) {
                if (Array.isArray(property.dependsOn[dependsKey])) {
                    conditions.push(property.dependsOn[dependsKey].indexOf(config[dependsKey]) >= 0);
                }
                else {
                    conditions.push(config[dependsKey] === String(property.dependsOn[dependsKey]));
                }
            }
            if (conditions.length > 0) {
                if (property.validationType == "anyone_true") {
                    return conditions.some(c => c);
                }
                return conditions.every(c => c);
            }
            return false;
        } else {
            if ($scope?.configType === 'mapper' && (config[property.name] === null || config[property.name] === undefined)) {
                config[property.name] = '';
            }
            return true;
        }
    };

    $scope.checkIfOptionAvailable = function (config, option) {
        if (config[option.name] !== '' && option?.options.length > 0 && !option.options.includes(config[option.name])) {
            config[option.name] = "";
        }
    };

    $scope.checkIfMapOptionAvailable = function (config, option) {

    let count = option.mapOptions.filter(
      (x) => x.value === config[option.name]
    ).length;
    if (
      config[option.name] !== "" &&
      option?.mapOptions.length > 0 &&
      count == 0
    ) {
      config[option.name] = "";
    }
    };

    $scope.removeMapEntry = function (optionName, key) {
        for (var i = $scope.config[optionName].length - 1; i >= 0; i--) {
            if ($scope.config[optionName][i]['key'] === key) {
                $scope.config[optionName].splice(i, 1);
            }
        }
    };

    // $scope.updateMapEntry = function (optionName, key, value) {
    //     var parsedMap = JSON.parse($scope.config[optionName]);

    //     for (var i = parsedMap.length - 1; i >= 0; i--) {
    //         if (parsedMap[i]['key'] === key) {
    //             parsedMap[i]['value'] = value;
    //         }
    //     }
    //     $scope.config[optionName] = JSON.stringify(parsedMap);

    //     // focusMapValueId = "mapValue-" + optionName + "-" + key;
    // };

    $scope.hideShowPassword = function (option) {
        if ($(`#${option.name}_show_hide_password input`).attr("type") === "text") {
            $(`#${option.name}_show_hide_password input`).attr('type', 'password');
            $(`#${option.name}_show_hide_password .hide_show span`).addClass("hidden_outlined");
            $(`#${option.name}_show_hide_password .hide_show span`).removeClass("visible_outlined");
        } else if ($(`#${option.name}_show_hide_password input`).attr("type") === "password") {
            $(`#${option.name}_show_hide_password input`).attr('type', 'text');
            $(`#${option.name}_show_hide_password .hide_show span`).removeClass("hidden_outlined");
            $(`#${option.name}_show_hide_password .hide_show span`).addClass("visible_outlined");
        }
    };
    $scope.showSecret = function (option) {
        Dialog.confirm($translate.instant('show-secret-txt'),$translate.instant('reveal-secret-txt'), function() {
            $scope.reqBody = {
                providerFactory: $scope.completeconfig.providerClass,
                providerId: $scope.completeprop.providerId
            };
            ShowSecret.show({
                realm: $route.current.params.realm,
            }, $scope.reqBody, function (secretData) {
                if (secretData[option.name]) {
                    $scope.$emit('showSecret', true);
                    $scope.oldConfigs[option.name] === $scope.config[option.name] ? $scope.config[option.name] = secretData[option.name] : '';
                    option.secret = false;
                }
                $scope.hideShowPassword(option);
            });
        });
    };

    $scope.jsonParseMap = function (optionName) {
        if (cachedParsedMaps[optionName] === undefined) {
            cachedMaps[optionName] = "[]";
            cachedParsedMaps[optionName] = [];
            if (!$scope.config.hasOwnProperty(optionName) || !$scope.config[optionName]) {
                $scope.config[optionName] = cachedMaps[optionName];
            } else {
                cachedMaps[optionName] = $scope.config[optionName];
                cachedParsedMaps[optionName] = JSON.parse(cachedMaps[optionName]);
            }
        }

        var mapChanged = $scope.config[optionName] !== cachedMaps[optionName];

        if (mapChanged) {
            cachedMaps[optionName] = $scope.config[optionName];
            cachedParsedMaps[optionName] = JSON.parse(cachedMaps[optionName]);
        }

        // if (!mapChanged && focusMapValueId !== null) {
        //     document.getElementById(focusMapValueId).focus();
        //     focusMapValueId = null;
        // }

        return cachedParsedMaps[optionName];
    };
    $scope.initPicker = function (config, option) {
        const pickr = Pickr.create({
            el: '.' + option.name,
            theme: 'nano', // or 'monolith', or 'nano',
            default: config[option.name],
            useAsButton: true,
            components: {

                // Main components
                preview: true,
                opacity: true,
                hue: true,

                // Input / output Options
                interaction: {
                    hex: true,
                    rgba: true,
                    hsla: true,
                    input: true,
                    save: true
                }
            }
        });
        pickr.on('init', instance => {
            pickr.show();
        });
        pickr.on('change', (color, source, instance) => {
            config[option.name] = color.toHEXA().toString();
            $scope.objVal = {
                name: option.name,
                color: color.toHEXA().toString()
            };
            $rootScope.$emit("updatedValue", $scope.objVal);
        });
        pickr.on('save', (color, instance) => {
            config[option.name] = color.toHEXA().toString();
            pickr.destroyAndRemove();
        });
    };

    $scope.tooltipTxt = $translate.instant("copy");
    $scope.copyToClipboard = function (element, elementVal) {
        var $temp = $("<input>");
        $("body").append($temp);
        $temp.val(elementVal).select();
        document.execCommand("copy");
        $scope.tooltipTxt = $translate.instant("copied");
        $temp.remove();
        //on MouseLeave the $scope.tooltipTxt will be changed to back.
        $(`#qv-input__${element}-copy`).mouseleave(function () {
            $scope.tooltipTxt = $translate.instant("copy");
        });
    };

    // For processing the number input with min, max value check and string value check.
    $scope.processNumberInput = ProcessInput.validateRange;

    // Type: MultiValuedGroupSelect
    $scope.showGroupOptions = (event, fieldName) => {
        if(!isCustomDropDownVisible){
            isCustomDropDownVisible = true;
            event.stopPropagation();
        }else{
            isCustomDropDownVisible = false;
        }
        $scope.showingGroupOption[fieldName] = !$scope.showingGroupOption[fieldName];
    };
    $scope.removeGroupOption = (fieldName, option) => {
        $scope.preferredGroupOptions[fieldName] = $scope.preferredGroupOptions[fieldName].filter(op => op.name !== option.name);
        $scope.selectedGroupIndex[fieldName] = [];
        $scope.multiGroupedOptionsCopy[fieldName].forEach((subArray, i) => {
            if(subArray.some(item => item.name === option.name) && !$scope.multiGroupedOptions[fieldName][i].some(item => item.name === option.name)){
                $scope.multiGroupedOptions[fieldName][i].push(option);
            }
            if($scope.preferredGroupOptions[fieldName].every(preferredItem =>subArray.some(item => item.name === preferredItem.name)) && !$scope.selectedGroupIndex[fieldName].includes(i)){
                $scope.selectedGroupIndex[fieldName].push(i);
            }
        })
        if($scope.preferredGroupOptions[fieldName].length == 0 || $scope.selectedGroupIndex[fieldName].length == 0){
            $scope.selectedGroupIndex[fieldName] = [-1];
        }
        $scope.updateMultiValuedGroupSelectValue(fieldName);
    }

    $scope.addGroupOption = (fieldName, option, index) => {
        let tempGroupOptions = angular.copy($scope.multiGroupedOptions[fieldName]);
        if ($scope.selectedGroupIndex[fieldName].includes(-1)) {
            $scope.selectedGroupIndex[fieldName] = [];
            $scope.multiGroupedOptions[fieldName].forEach((subArray, i) => {
                if (subArray.some(item => item.name === option.name)) {
                    $scope.selectedGroupIndex[fieldName].push(i);
                    tempGroupOptions[i] = tempGroupOptions[i].filter(item => item.name !== option.name);
                }
            });
        }
        else if ($scope.selectedGroupIndex[fieldName].includes(index)) {
            $scope.multiGroupedOptions[fieldName].forEach((subArray, i) => {
                if (!subArray.some(item => item.name === option.name)) {
                    $scope.selectedGroupIndex[fieldName] = $scope.selectedGroupIndex[fieldName].filter(item => item !== i);
                } else {
                    tempGroupOptions[i] = tempGroupOptions[i].filter(item => item.name !== option.name);
                }
            });
        }
        else {
            Notifications.error($translate.instant("claim-group-mismatch-error"));
            return;
        }
        $scope.preferredGroupOptions[fieldName].push(option);
        $scope.multiGroupedOptions[fieldName] = tempGroupOptions;
        $scope.updateMultiValuedGroupSelectValue(fieldName);
    }
    

    $scope.updateMultiValuedGroupSelectValue = (fieldName) => {
        $scope.preferredGroupOptions[fieldName].forEach(item => {delete item.$$hashKey});
        $scope.configs[fieldName] = JSON.stringify($scope.preferredGroupOptions[fieldName], null, '');
    }

    // Type: MultivaluedObjectList
    $scope.showMultivaluedObjectOptions = (event, fieldName) => {
        if(!isCustomDropDownVisible){
            isCustomDropDownVisible = true;
            event.stopPropagation();
        }else{
            isCustomDropDownVisible = false;
        }
        $scope.showingObjectListOptions[fieldName] = !$scope.showingObjectListOptions[fieldName];
    };

    $scope.addObjectToMultivaluedList = (fieldName, option) => {
        $scope.preferredMultivaluedObject[fieldName].push(option);
        $scope.MultivaluedObjectOptions[fieldName] = $scope.MultivaluedObjectOptions[fieldName].filter(o => o.name !== option.name);
        $scope.preferredMultivaluedObject[fieldName].forEach(item => {delete item.$$hashKey});
        $scope.configs[fieldName] = JSON.stringify($scope.preferredMultivaluedObject[fieldName], null, '');
    }

    $scope.removeObjectFromMultivaluedList = (field, option) => {
        if(field.defaultValue && field.defaultValue.length > 0 && field.defaultValue.some(opt => opt.name === option.name)) {
            if(field.name && field.name.includes("digilocker-list-of-docs")) {
                Notifications.error($translate.instant("default-document-value-cannot-be-removed"));
            } else {
                Notifications.error($translate.instant("claim-default-value-cannot-be-removed"));
                }
            return;
        }
        $scope.MultivaluedObjectOptions[field.name].push(option);
        $scope.preferredMultivaluedObject[field.name] = $scope.preferredMultivaluedObject[field.name].filter(o => o.name !== option.name);
        $scope.preferredMultivaluedObject[field.name].forEach(item => {delete item.$$hashKey});
        $scope.configs[field.name] = JSON.stringify($scope.preferredMultivaluedObject[field.name], null, '');
    }

    $document.on('click', function() {
        $scope.$apply(function() {
            Object.keys($scope.showingGroupOption).map(option => $scope.showingGroupOption[option] = false);
            Object.keys($scope.showingObjectListOptions).map(option => $scope.showingObjectListOptions[option] = false);
            isCustomDropDownVisible = false;
        });
    });

//    document.querySelector(".angular-panel-content").addEventListener("click", function (params) {
//        $scope.$apply(function() {
//            Object.keys($scope.showingGroupOption).map(option => $scope.showingGroupOption[option] = false);
//            Object.keys($scope.showingObjectListOptions).map(option => $scope.showingObjectListOptions[option] = false);
//            isCustomDropDownVisible = false;
//        });
//    })
 });

 module.controller('ValidatorConfigCtrl', function ($scope, $translate, $timeout) {
     $scope.validatorConfig = [];
     $scope.ignoreValueConditions = [];
     $scope.validatorConfigCondition = 'or';
     $scope.validatorCondition = {};
     $scope.newConfig = {
         "name": "",
         "validation": "",
         "value": "",
         "type":"string",
         "showClaims": false
        };
    $scope.defaultConfig = angular.copy($scope.newConfig);
    $scope.oldName = angular.copy($scope.action.name);
    $scope.isValidConfig = false;
    $scope.availableClaims = $scope.$parent.validatorCliams;
    $scope.claimsLength = Object.keys($scope.availableClaims).length;
    $scope.searchList = [];
    $scope.showDropDown = []
    $scope.dropIndex = null;
    $scope.dropIsnew = false;
    $scope.claimType = "string";

    const outsideBodyClick = (event) => {
        if(event.target.tagName === "INPUT" || $scope.dropIndex === null) {
            return;
        }
        $scope.showList($scope.dropIndex, $scope.dropIsnew, false, false);
        $scope.$apply();
    }

    $scope.selectClaim = (index, isNew, state, clear) => {
        if($scope.validatorConfig.length){
            $scope.validatorConfig.map((config, i) => {
                if(config.showClaims){
                    $scope.showList(i, false, false, false);
                }
            })
        }
        if($scope.newConfig.showClaims){
            $scope.showList(null, true, false, false);
        }
        $scope.showList(index, isNew, state, clear);
    }

    $( document ).ready(function() {
        $(".angular-panel-bg").click((event)=> {
            outsideBodyClick(event)
        })
        $(".angular-panel-dialog").click((event)=> {
            outsideBodyClick(event)
        })
    });

    $scope.showList = (index, isNew, state, clear) => {
        if (isNew) {
            $scope.newConfig.showClaims = state;
        } else {
            if (clear)
                $scope.clearClaimsOldValueByIndex(index);
            $scope.validatorConfig[index].showClaims = state;
        }
        if (state)
            $scope.searchList = Object.keys($scope.availableClaims);
        $scope.dropIndex = index;
        $scope.dropIsnew = isNew
    }

    $scope.closeDropDown = () => {
        $scope.searchList = [];
    }

    $scope.searchInList = (name) => {
        if (!name) {
            return ;
        }

        $scope.searchList =  Object.keys($scope.availableClaims).filter(val => {
            return val.includes(name);
        });
    }

    $scope.handleConfigChange = (index, claim) => {
        $scope.validatorConfig[index]['name'] = claim;
        $scope.showList(index, false, true, false);
        $scope.clearClaimsOldValueByIndex(index);
        $scope.searchInList(claim);
        $scope.validatorConfig[index]['type'] = $scope.availableClaims[claim] ? $scope.availableClaims[claim] : 'string';
    }

    $scope.handleChange = (index, claim)=> {
        $scope.newConfig['name'] = claim;
        $scope.showList(0, true, false, true)
        $scope.clearClaimsOldValueByIndex(index);
        $scope.searchInList(claim);
        $scope.newConfig['type'] = $scope.availableClaims[claim] ? $scope.availableClaims[claim] : 'string';
    }

    function checkIsValidConfig(config) {
        return config &&
               config.hasOwnProperty("name") && config.name && config.name.trim().length > 0 &&
               config.hasOwnProperty("validation") && config.validation && config.validation.trim().length > 0 &&
               config.hasOwnProperty("value") &&
               (
                    !$scope.ignoreValueConditions.includes(config['validation']) ?
                    (config.type == 'dt' ?
                        (config.value ? true : false)
                        : (config.value && config.value.trim().length > 0) ) :
                    true
               )
    }

    function makeSaveButtonDisabled() {
        $timeout(function(){
            $('#saveConfigBtn').prop('disabled', true);
        }, 0)
    }

    function epochToDatetime(epochTime) {
        const date = new Date(epochTime);
        const year = date.getFullYear();
        const month = ('0' + (date.getMonth() + 1)).slice(-2);
        const day = ('0' + date.getDate()).slice(-2);
        const hours = ('0' + date.getHours()).slice(-2);
        const minutes = ('0' + date.getMinutes()).slice(-2);
        const seconds = ('0' + date.getSeconds()).slice(-2);
        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
    }

    $scope.$watch('validatorConfigCondition', function() {
        $scope.validatorConditionDisplayTxt = $translate.instant($scope.validatorConfigCondition);
        if($scope.validatorConfig.length === 0 && $scope.action.name.length > 0 && checkIsValidConfig($scope.newConfig)){
            $scope.convertToClaimValidatorString();
        }else if($scope.validatorConfig.length !== 0 && $scope.action.name.length > 0 && $scope.validatorConfig.every(checkIsValidConfig)){
            $scope.convertToClaimValidatorString();
        }
    }, true)

    $scope.$watch('validatorConfig', function() {
        if ($scope.validatorConfig.length === 0 && checkIsValidConfig($scope.newConfig)) {
            $scope.isValidConfig = true;
        } else {
            $scope.isValidConfig = $scope.validatorConfig.every(checkIsValidConfig);
        }
        if($scope.isValidConfig && $scope.action.name.length > 0){
            $scope.convertToClaimValidatorString();
        }else{
            makeSaveButtonDisabled();
            $scope.config['validation-condition-prompt'] = "";
        }
    }, true);

    $scope.$watch('newConfig', function() {
        if($scope.validatorConfig.length == 0 && checkIsValidConfig($scope.newConfig)){
            $scope.isValidConfig = true;
        }else if($scope.validatorConfig.length == 0){
            $scope.isValidConfig = false;
        }
        if ($scope.isValidConfig) {
            $scope.convertToClaimValidatorString();
        } else {
            if($scope.validatorConfig.length == 0) {
                $scope.config['validation-condition-prompt'] = "";
            }
            makeSaveButtonDisabled();
        }
    }, true);

    $scope.addClaimValidator = () => {
        $scope.validatorConfig.push(angular.copy($scope.newConfig));
        $scope.newConfig = angular.copy($scope.defaultConfig);
        if($scope.validatorConfig.length == 1) {
            $scope.validatorConfig.push(angular.copy($scope.newConfig));
        }
    }

    $scope.removeClaimValidator = (index) => {
        $scope.validatorConfig.splice(index, 1);
    }

    $scope.convertToClaimValidatorString = () => {
        let claimValidatorString = "";
        let claimType = "";
        $scope.validatorConfig.forEach(config => {
            let claimString = "claimValidator(";

            if (config.name && config.name.length) {
                claimString += `(${config.name})`;
                claimString += ` (${config.type})`;
            }
            if (config.validation && config.validation.length) {
                claimString += ` (${config.validation})`;
            }
            if (!$scope.ignoreValueConditions.includes(config.validation) && config.type == 'dt'){
                var datetimeInput = angular.copy(config.value);
                var datetime = new Date(Math.floor(datetimeInput / 1000) * 1000);
                claimString += ` (${datetime.getTime()})`;
            }
            if (!$scope.ignoreValueConditions.includes($scope.newConfig.validation) && config.type != 'dt' && config.value && config.value.length) {
                claimString += ` (${config.value})`;
            }

            claimString += ") ";

            if (claimValidatorString.length) {
                claimValidatorString += "and ";
            }

            claimValidatorString += claimString;
        });
        if($scope.validatorConfig.length == 0){
            let claimString = "claimValidator(";
            let claimType = "";
            if ($scope.newConfig.name && $scope.newConfig.name.length) {
                claimString += `(${$scope.newConfig.name})`;
                claimType = $scope.newConfig.type;
                claimString += ` (${claimType})`;
            }
            if ($scope.newConfig.validation && $scope.newConfig.validation.length) {
                claimString += ` (${$scope.newConfig.validation})`;
            }
            if (!$scope.ignoreValueConditions.includes($scope.newConfig.validation) && claimType == 'dt'){
                var datetimeInput = angular.copy($scope.newConfig.value);
                var datetime = new Date(Math.floor(datetimeInput / 1000) * 1000);
                claimString += ` (${datetime.getTime()})`;
            }
            if (!$scope.ignoreValueConditions.includes($scope.newConfig.validation) && claimType != 'dt' && $scope.newConfig.value && $scope.newConfig.value.length) {
                claimString += ` (${$scope.newConfig.value})`;
            }

            claimString += ") ";

            if (claimValidatorString.length) {
                claimValidatorString += "and ";
            }

            claimValidatorString += claimString;
        }
        const decision = $scope.validatorConfigCondition === 'and' ? "unanimous" : "affirmative";
        $scope.config['validation-condition-prompt'] = `${claimValidatorString}and decision(${decision})`;
    }

    $scope.convertToClaimValidatorArray = () => {
        const parsedArray = [];
        let decisionContent = null;
        const conditions = $scope.config.hasOwnProperty('validation-condition-prompt') ? $scope.config['validation-condition-prompt'].split(' and ') : [];
        conditions.forEach(condition => {
            if (condition.startsWith('claimValidator(')) {
                const content = condition.slice(15, -1);
                const parts = [];
                let bracketDepth = 0;
                let part = '';
                for (let char of content) {
                    if (char === '(') bracketDepth++;
                    if (char === ')') bracketDepth--;

                    if (char === ' ' && bracketDepth === 0) {
                        parts.push(part.trim());
                        part = '';
                    } else {
                        part += char;
                    }
                }
                if (part) parts.push(part.trim());
                const parsedParts = parts.map(part => part.slice(1, -1));
                parsedArray.push({
                    name: parsedParts[0],
                    validation: parsedParts[2],
                    value:  parsedParts[1] == 'dt'
                        ? new Date(epochToDatetime(Number(parsedParts[3])))
                        : parsedParts[3],
                    showClaims:false,
                    type: parsedParts[1]
                });
            }
            else if (condition.startsWith('decision(')) {
                decisionContent = condition.slice(9, -1);
            }
        });
        $scope.validatorConfigCondition = decisionContent == "affirmative" ? 'or' : 'and';
        $scope.validatorConfig = parsedArray;
    }

    $scope.clearClaimsOldValueByIndex = (index) => {
        if ($scope.validatorConfig.length <= 0 || !$scope.validatorConfig[index]) {
            return;
        }
        $scope.validatorConfig[index].validation = "";
        $scope.validatorConfig[index].value = "";
    }

    $scope.clearClaimsOldValue = () => {
        $scope.newConfig.validation = "";
        $scope.newConfig.value = "";
    }

    $scope.generateConfig = (properties) => {
        properties.map(prop => {
            if (prop.name === "validator-supported-types") {
                $scope.types = prop.options;
            } else if (prop.name === "validator-value-non-supported-condition") {
                $scope.ignoreValueConditions = prop.options;
            }
        })

        properties.map( prop => {
            const typeName = prop.name.split("-")[1];
            if ($scope.types.includes(typeName)) {
                $scope.validatorCondition[typeName] = prop.options
            }
        })

        $scope.convertToClaimValidatorArray();
    }

    $scope.init = (config, properties) => {
        $scope.generateConfig(properties)
    }


 });


module.directive('kcProviderConfig', function ($modal) {
    return {
        scope: {
            config: '=',
            oldconfigvalue: '=',
            properties: '=',
            realm: '=',
            clients: '=',
            configName: '=',
            brandname: '=',
            completeconfig: '=',
            completeprop: '=',
            configType: '=',
            versions: '='
        },
        restrict: 'E',
        replace: true,
        resolve: {
            serverInfo: function (ServerInfoLoader) {
                return ServerInfoLoader();
            }
        },
        controller: 'ProviderConfigCtrl',
        templateUrl: resourceUrl + '/templates/kc-provider-config.html'
    };
});

module.directive('kcValidatorConfig', function ($modal) {
    return {
        scope: {
            config: '=',
            properties: '=',
            action:'='
        },
        restrict: 'E',
        replace: true,
        resolve: {
            serverInfo: function (ServerInfoLoader) {
                return ServerInfoLoader();
            }
        },
        controller: 'ValidatorConfigCtrl',
        templateUrl: resourceUrl + '/templates/kc-validator-config.html'
    };
});

module.controller('ComponentRoleSelectorModalCtrl', function ($scope, realm, config, configName, RealmRoles, Client, ClientRole, $modalInstance) {
    $scope.selectedRealmRole = {
        role: undefined
    };
    $scope.selectedClientRole = {
        role: undefined
    };
    $scope.client = {
        selected: undefined
    };

    $scope.selectRealmRole = function () {
        config[configName][0] = $scope.selectedRealmRole.role.name;
        $modalInstance.close();
    };

    $scope.selectClientRole = function () {
        config[configName][0] = $scope.client.selected.clientId + "." + $scope.selectedClientRole.role.name;
        $modalInstance.close();
    };

    $scope.cancel = function () {
        $modalInstance.dismiss();
    };

    $scope.$on('$routeChangeStart', function () {
        $modalInstance.close();
    });

    $scope.changeClient = function () {
        if ($scope.client.selected) {
            ClientRole.query({ realm: realm.realm, client: $scope.client.selected.id }, function (data) {
                $scope.clientRoles = data;
            });
        } else {
            console.log('selected client was null');
            $scope.clientRoles = null;
        }

    };
    RealmRoles.query({ realm: realm.realm }, function (data) {
        $scope.realmRoles = data;
    });
    Client.query({ realm: realm.realm }, function (data) {
        $scope.clients = data;
        if (data.length > 0) {
            $scope.client.selected = data[0];
            $scope.changeClient();
        }
    });
});

module.controller('ComponentConfigCtrl', function ($modal, $scope, $route, Client, Policies) {
    $scope.policies = [];
    $scope.initSelectedClient = function (configName, config) {
        if (config[configName]) {
            $scope.selectedClient = null;
            Client.query({ realm: $route.current.params.realm, search: false, clientId: config[configName], max: 1 }, function (data) {
                if (data.length > 0) {
                    $scope.selectedClient = angular.copy(data[0]);
                    $scope.selectedClient.text = $scope.selectedClient.clientId;
                }
            });
        }
    };

    $scope.changeClient = function (configName, config, client) {
        if (!client || !client.id) {
            config[configName] = null;
            $scope.selectedClient = null;
            return;
        }
        $scope.selectedClient = client;
        config[configName] = client.clientId;
    };

    $scope.initPolicyStreams = function () {
        Policies.query({ realm: $route.current.params.realm }, function (response) {
            for (const policy of response) {
                if (policy.policyType.toLowerCase() === "password") {
                    $scope.policies.push(policy.policyName);
                }
            }
        });
    };


    $scope.openRoleSelector = function (configName, config) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/component-role-selector.html',
            controller: 'ComponentRoleSelectorModalCtrl',
            backdrop: 'static',
            resolve: {
                realm: function () {
                    return $scope.realm;
                },
                config: function () {
                    return config;
                },
                configName: function () {
                    return configName;
                }
            }
        });
    };
});
module.directive('kcComponentConfig', function ($modal) {
    return {
        scope: {
            config: '=',
            properties: '=',
            realm: '=',
            clients: '=',
            configName: '='
        },
        restrict: 'E',
        replace: true,
        controller: 'ComponentConfigCtrl',
        templateUrl: resourceUrl + '/templates/kc-component-config.html'
    };
});

module.directive('kcComponentCustomConfig', function ($modal) {
    return {
        scope: {
            config: '=',
            properties: '=',
            realm: '=',
            clients: '=',
            configName: '='
        },
        restrict: 'E',
        replace: true,
        controller: 'ComponentConfigCtrl',
        templateUrl: resourceUrl + '/templates/kc-component-custom-config.html'
    };
});

/*
*  Used to select the element (invoke $(elem).select()) on specified action list.
*  Usages kc-select-action="click mouseover"
*  When used in the textarea element, this will select/highlight the textarea content on specified action (i.e. click).
*/
module.directive('kcSelectAction', function ($compile, Notifications) {
    return {
        restrict: 'A',
        compile: function (elem, attrs) {

            var events = attrs.kcSelectAction.split(" ");

            for (var i = 0; i < events.length; i++) {

                elem.bind(events[i], function () {
                    elem.select();
                });
            }
        }
    };
});

module.filter('remove', function () {
    return function (input, remove, attribute) {
        if (!input || !remove) {
            return input;
        }

        var out = [];
        for (var i = 0; i < input.length; i++) {
            var e = input[i];

            if (Array.isArray(remove)) {
                for (var j = 0; j < remove.length; j++) {
                    if (attribute) {
                        if (remove[j][attribute] == e[attribute]) {
                            e = null;
                            break;
                        }
                    } else {
                        if (remove[j] == e) {
                            e = null;
                            break;
                        }
                    }
                }
            } else {
                if (attribute) {
                    if (remove[attribute] == e[attribute]) {
                        e = null;
                    }
                } else {
                    if (remove == e) {
                        e = null;
                    }
                }
            }

            if (e != null) {
                out.push(e);
            }
        }

        return out;
    };
});

module.filter('capitalize', function () {
    return function (input) {
        if (!input) {
            return;
        }
        var splittedWords = input.split(/\s+/);
        for (var i = 0; i < splittedWords.length; i++) {
            splittedWords[i] = splittedWords[i].charAt(0).toUpperCase() + splittedWords[i].slice(1);
        };
        return splittedWords.join(" ");
    };
});

//used to remove underscore if present and add space for that character
module.filter('replaceUnderScoreWithSpace', function () {
    return function (input) {
        input = input.replace(/_/g, " ");
        return input;
    };
});
module.filter('replaceHypenWithSpace', function () {
    return function (input) {
        input = input.replace(/-/g, " ");
        return input;
    };
});

module.filter('replaceSpaceWithHyphen', function() {
    return function(input) {
        return input.replace(/\s+/g, '-');
    };
});

module.filter('replaceWithVariant', function () {
    return function (input, newText) {
        let oldText = '{variant}';
        if (input && newText) {
            return input.replace(new RegExp(oldText, 'g'), newText);
        } else {
            return input;
        }
    };
});

module.filter('translateOrReplaceWithSpace', function ($translate) {
    return function (input) {
        input = $translate.instant(input).replace(/_/g, " ");
        return input;
    };
});


//used to remove underscore if present
module.filter('removeUnderScore', function () {
    return function (input) {
        input = input.replace(/_/g, "");
        return input;
    };
});

module.filter('numToWords', function () {
    var a = ['', 'one ', 'two ', 'three ', 'four ', 'five ', 'six ', 'seven ', 'eight ', 'nine ', 'ten ', 'eleven ', 'twelve ', 'thirteen ', 'fourteen ', 'fifteen ', 'sixteen ', 'seventeen ', 'eighteen ', 'nineteen '];
    var b = ['', '', 'twenty', 'thirty', 'forty', 'fifty', 'sixty', 'seventy', 'eighty', 'ninety'];

    return function (num) {
        if ((num = num.toString()).length > 9) return 'overflow';
        var n = ('000000000' + num).substr(-9).match(/^(\d{2})(\d{2})(\d{2})(\d{1})(\d{2})$/);
        if (!n) return; var str = '';
        str += (n[1] != 0) ? (a[Number(n[1])] || b[n[1][0]] + ' ' + a[n[1][1]]) + 'crore ' : '';
        str += (n[2] != 0) ? (a[Number(n[2])] || b[n[2][0]] + ' ' + a[n[2][1]]) + 'lakh ' : '';
        str += (n[3] != 0) ? (a[Number(n[3])] || b[n[3][0]] + ' ' + a[n[3][1]]) + 'thousand ' : '';
        str += (n[4] != 0) ? (a[Number(n[4])] || b[n[4][0]] + ' ' + a[n[4][1]]) + 'hundred ' : '';
        str += (n[5] != 0) ? ((str != '') ? 'and ' : '') + (a[Number(n[5])] || b[n[5][0]] + ' ' + a[n[5][1]]) : '';
        return str;
    };
});

/*
 * Guarantees a deterministic property iteration order.
 * See: http://www.2ality.com/2015/10/property-traversal-order-es6.html
 */
module.filter('toOrderedMapSortedByKey', function () {
    return function (input) {

        if (!input) {
            return input;
        }

        var keys = Object.keys(input);

        if (keys.length <= 1) {
            return input;
        }

        keys.sort();

        var result = {};
        for (var i = 0; i < keys.length; i++) {
            result[keys[i]] = input[keys[i]];
        }

        return result;
    };
});

module.directive('kcSidebarResize', function ($window) {
    return function (scope, element) {
        function resize() {
            var navBar = angular.element(document.getElementsByClassName('navbar-pf')).height();
            var container = angular.element(document.getElementById("view").getElementsByTagName("div")[0]).height();
            var height = Math.max(container, window.innerHeight - navBar - 3);

            element[0].style['min-height'] = height + 'px';
        }

        resize();

        var w = angular.element($window);
        scope.$watch(function () {
            return {
                'h': window.innerHeight,
                'w': window.innerWidth
            };
        }, function () {
            resize();
        }, true);
        w.bind('resize', function () {
            scope.$apply();
        });
    };
});



module.directive('kcTooltip', function ($compile) {
    return {
        restrict: 'E',
        replace: false,
        terminal: true,
        priority: 1000,
        link: function link(scope, element) {
            let angularElement = angular.element(element[0]), tooltip = angularElement.text();
            angularElement.text('');
            element.addClass("hidden");
            let tempParent = angular.element(element.parent()), pTag = $compile(angular.element(`<p class="helper-text w-100">${tooltip}</p>`))(scope);
            if (tempParent.find('.form-control').length > 1 || tempParent.find('.os-toggler-w').length > 0) {
                if (tempParent.find('.os-toggler-w').length) {
                    tempParent.find('.os-toggler-w').parent().parent().append(pTag);
                } else {
                    tempParent.find('.form-control').parent().hasClass('col-md-6') ?
                        tempParent.find('.form-control').parent().parent().append(pTag) : tempParent.find('.form-control').parent().append(pTag);
                }
                if (tempParent.find('.form-control').length === 2 && tempParent.find('.form-control').parent().hasClass('col-md-6')) {
                    pTag.addClass("pl-625");
                }
            } else if (tempParent.find('.form-control').length === 1) {
                tempParent.find('.form-control').parent().append(pTag);
            } else if (tempParent[0].tagName.toLowerCase() === 'legend') {
                tempParent.find('kc-tooltip').eq(0).replaceWith(pTag);
            } else {
                tempParent.append(pTag);
            }
        }
    };
});

module.directive('kcOpen', function ($location) {
    return function (scope, element, attrs) {
        var path;

        attrs.$observe('kcOpen', function (val) {
            path = val;
        });

        element.bind('click', function () {
            scope.$apply(function () {
                $location.path(path);
            });
        });
    };
});

module.directive('kcOnReadFile', function ($parse) {
    console.debug('kcOnReadFile');
    return {
        restrict: 'A',
        scope: false,
        link: function (scope, element, attrs) {
            var fn = $parse(attrs.kcOnReadFile);

            element.on('change', function (onChangeEvent) {
                var reader = new FileReader();

                reader.onload = function (onLoadEvent) {
                    scope.$apply(function () {
                        fn(scope, { $fileContent: onLoadEvent.target.result });
                    });
                };

                reader.readAsText((onChangeEvent.srcElement || onChangeEvent.target).files[0]);
            });
        }
    };
});

module.controller('PagingCtrl', function ($scope) {
    $scope.currentPageInput = 1;

    $scope.firstPage = function () {
        if (!$scope.hasPrevious()) return;
        $scope.currentPage = 1;
        $scope.currentPageInput = 1;
    };

    $scope.lastPage = function () {
        if (!$scope.hasNext()) return;
        $scope.currentPage = $scope.numberOfPages;
        $scope.currentPageInput = $scope.numberOfPages;
    };

    $scope.previousPage = function () {
        if (!$scope.hasPrevious()) return;
        $scope.currentPage--;
        $scope.currentPageInput = $scope.currentPage;
    };

    $scope.nextPage = function () {
        if (!$scope.hasNext()) return;
        $scope.currentPage++;
        $scope.currentPageInput = $scope.currentPage;
    };

    $scope.hasNext = function () {
        return $scope.currentPage < $scope.numberOfPages;
    };

    $scope.hasPrevious = function () {
        return $scope.currentPage > 1;
    };
});

module.controller('GettingStartCtrl', function ($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, ClientTryUrl, LicenseChecker, $translate) {
    $scope.flag = 1;
    $scope.tooltipMessage = 'Copy!';
    $scope.init = function () {
        if ($scope.flag) {
            $scope.loaderTypeName = 'gettingStartedLoader';
            $scope.realm = angular.copy(realm);
            $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === 'true' ? true : false;
            $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === 'true' ? true  : false;
            genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications);
            if (serverInfo.systemInfo.offering === "CLOUD") {
                LicenseChecker.get({ realm: realm.realm }, function (data) {
                    $scope.licenseInfo = data;
                    $scope.updateLicenseInfo();
                    $scope.dataLoaded = true;
                }, function () {
                    $scope.dataLoaded = true;
                });
            } else {
                $scope.dataLoaded = true;
            }
            $scope.flag = 0;
            getWorkspaceSetupStatus()
        }
    };

    function getWorkspaceSetupStatus() {
        $http.get(`${consoleBaseUrl}workspace-setup-status?realm=${realm.realm}`)
            .then((response) => {
                $scope.workspaceSetupStatus = response.data
            })
            .catch((error) => Notifications.error($translate.instant('workspace-status-error')))
    }

    $scope.copytoClipboard = function(value) {
        navigator.clipboard.writeText(value);
        $scope.tooltipMessage = $translate.instant("copied");
        $('#copy-icon').mouseleave(function () {
            $scope.tooltipMessage = $translate.instant("copy");
        });
        navigator.clipboard.writeText(value).then(function() {
            $scope.tooltipMessage = $translate.instant("copied");
        })
        .catch(function(error) {
            console.error("Unable to copy to clipboard:", error);
        });
    }

    $scope.openTab = function () {
        ClientTryUrl.get(
          { realm: $scope.realm.realm, client: "sso" },
          function (data) {
            redirectInsideAsync(data.oidcurl);
          },
          function () {
            Notifications.error(
              $translate.instant("application-tryurl-fetch-failed")
            );
          });
    };

    $scope.updateLicenseInfo = function () {
        $scope.invoice = $scope.licenseInfo.body.invoice;
        if ($scope.licenseInfo.status === 200 && $scope.licenseInfo.body.days !== -1) {
            $scope.licenseMessage = $translate.instant('trial-ends-at') + $scope.getLicenseExpireDate($scope.licenseInfo.body.days) + ' ( ' + $scope.licenseInfo.body.days + ' ' + $translate.instant('days-to-go') + ' )';
        }
        else {
            if($scope.licenseInfo.body.stage) {
                $scope.licenseMessage = $translate.instant('license-message', { stage : $scope.licenseInfo.body.stage });
            } else {
                $scope.licenseMessage = $scope.licenseInfo.body.message;
            }

        }
    };

    $scope.getLicenseExpireDate = function (days) {
        const ExpireDate = new Date();
        ExpireDate.setDate(ExpireDate.getDate() + days);
        var month = ExpireDate.toLocaleString("en-US", { month: "short" });
        var date = ('0' + ExpireDate.getDate()).slice(-2);
        var year = ExpireDate.getFullYear();
        var shortDate = date + 'th ' + month + ', ' + year;
        return shortDate;
    };
});

// Provides a component for injection with utility methods for manipulating strings
module.factory('KcStrings', function () {
    var instance = {};

    // some IE versions do not support string.endsWith method, this method should be used as an alternative for cross-browser compatibility
    instance.endsWith = function (source, suffix) {
        return source.indexOf(suffix, source.length - suffix.length) !== -1;
    };

    return instance;
});

module.directive('kcPaging', function () {
    return {
        scope: {
            currentPage: '=',
            currentPageInput: '=',
            numberOfPages: '='
        },
        restrict: 'E',
        replace: true,
        controller: 'PagingCtrl',
        templateUrl: resourceUrl + '/templates/kc-paging.html'
    };
});

// Tests the page number input from currentPageInput to see
// if it represents a valid page.  If so, the current page is changed.
module.directive('kcValidPage', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, ctrl) {
            ctrl.$validators.inRange = function (modelValue, viewValue) {
                if (viewValue >= 1 && viewValue <= scope.numberOfPages) {
                    scope.currentPage = viewValue;
                }

                return true;
            };
        }
    };
});

// Directive to parse/format strings into numbers
module.directive('stringToNumber', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, ngModel) {
            ngModel.$parsers.push(function (value) {
                return (typeof value === 'undefined' || value === null) ? '' : '' + value;
            });
            ngModel.$formatters.push(function (value) {
                return parseFloat(value);
            });
        }
    };
});

// filter used for paged tables
module.filter('startFrom', function () {
    return function (input, start) {
        if (input) {
            start = +start;
            return input.slice(start);
        }
        return [];
    };
});

// filter used for filtering default themes
module.filter('defaultThemeFilter', function () {
    return function (input) {
        const defaultThemes = ["base", "keycloak", "keycloak.v2"];
        if (input) {
            return input.filter(theme => !defaultThemes.includes(theme.name));
        }
    };
    return [];
});


module.directive('kcPassword', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            function toggleMask(evt) {
                if (elem.hasClass('password-conceal')) {
                    view();
                } else {
                    conceal();
                }
            }

            function view() {
                elem.removeClass('password-conceal');

                var t = elem.next().children().first();
                t.addClass('fa-eye-slash');
                t.removeClass('fa-eye');
            }

            function conceal() {
                elem.addClass('password-conceal');

                var t = elem.next().children().first();
                t.removeClass('fa-eye-slash');
                t.addClass('fa-eye');
            }

            elem.addClass("password-conceal");
            elem.attr("type", "text");
            elem.attr("autocomplete", "off");

            var p = elem.parent();

            var inputGroup = $('<div class="input-group"></div>');
            var eye = $('<span class="input-group-addon btn btn-secondary"><span class="fa fa-eye"></span></span>')
                .on('click', toggleMask);

            $scope.$watch(attr.ngModel, function (v) {
                if (v && v == '**********') {
                    elem.next().addClass('disabled');
                } else if (v && v.indexOf('${v') == 0) {
                    elem.next().addClass('disabled');
                    view();
                } else {
                    elem.next().removeClass('disabled');
                }
            });

            elem.detach().appendTo(inputGroup);
            inputGroup.append(eye);
            p.append(inputGroup);
        }
    };
});


module.filter('resolveClientRootUrl', function () {
    return function (input) {
        if (!input) {
            return;
        }
        return input.replace("${authBaseUrl}", authServerUrl).replace("${authAdminUrl}", authUrl);
    };
});

module.filter("timeago", function ($translate) {
    //time: the time
    //local: compared to what time? default: now
    //raw: wheter you want in a format of "5 minutes ago", or "5 minutes"
    return function (time, local, raw) {
        if (!time) return  $translate.instant('never');

        if (!local) {
            (local = Date.now());
        }

        if (angular.isDate(time)) {
            time = time.getTime();
        } else if (typeof time === "string") {
            time = new Date(time).getTime();
        }

        if (angular.isDate(local)) {
            local = local.getTime();
        } else if (typeof local === "string") {
            local = new Date(local).getTime();
        }

        if (typeof time !== 'number' || typeof local !== 'number') {
            return;
        }

        var
            offset = Math.abs((local - time) / 1000),
            span = [],
            MINUTE = 60,
            HOUR = 3600,
            DAY = 86400,
            WEEK = 604800,
            MONTH = 2629744,
            YEAR = 31556926,
            DECADE = 315569260;

        if (offset <= MINUTE) span = ['', raw ? $translate.instant('now') : $translate.instant('less-than-min')];
        else if (offset < (MINUTE * 60)) span = [Math.round(Math.abs(offset / MINUTE)),  $translate.instant('min')];
        else if (offset < (HOUR * 24)) span = [Math.round(Math.abs(offset / HOUR)),  $translate.instant('hr')];
        else if (offset < (DAY * 7)) span = [Math.round(Math.abs(offset / DAY)),  $translate.instant('day')];
        else if (offset < (WEEK * 52)) span = [Math.round(Math.abs(offset / WEEK)),  $translate.instant('week')];
        else if (offset < (YEAR * 10)) span = [Math.round(Math.abs(offset / YEAR)),  $translate.instant('year')];
        else if (offset < (DECADE * 100)) span = [Math.round(Math.abs(offset / DECADE)),  $translate.instant('decade')];
        else span = ['', 'a long time'];

        span[1] += (span[0] === 0 || span[0] > 1) ? 's' : '';
        span = span.join(' ');

        if (raw === true) {
            return span;
        }
        var result = (time <= local) ? span + ' ' + $translate.instant('ago') : $translate.instant('in') + ' ' + span;
        return result;
    };
});

module.filter("timestampToDate", function () {
    return function (time) {
        if (!time) return "No Data";
        let options = { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric' };
        if (typeof time === "string") {
            time = new Date(Number(time)).toLocaleDateString("en-US", options);
        }

        return time;
    };
});

module.filter('roundTo', function() {
  return function(input, decimals) {
    if (isNaN(input)) return input; // handle non-numeric
    var factor = Math.pow(10, decimals || 0);
    return Math.round(input * factor) / factor;
  };
});

module.directive('kcLoader', function () {
    return {
        scope: {
            loadertype: '='
        },
        restrict: 'E',
        replace: true,
        controller: 'loaderCtrl',
        templateUrl: resourceUrl + '/templates/kc-loader.html'
    };
});
module.directive('kcUserListLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-user-list-loader.html'
    };
});
module.directive('kcSubscriptionLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-subscription-loader.html'
    };
});

module.directive('kcUserTabLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-user-tab-loader.html'
    };
});

module.directive('kcRoleListLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-role-list-loader.html'
    };
});
module.directive('kcTableListLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-table-list-loader.html'
    };
});
module.directive('kcClientListLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-client-list-loader.html'
    };
});
module.directive('kcCardListLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-card-list-loader.html'
    };
});
module.directive('kcFormPreviewLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-form-preview-loader.html'
    };
});
module.directive('kcThemePreviewLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-theme-preview-loader.html'
    };
});
module.directive('kcGettingStartedLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-getting-started-loader.html'
    };
});
module.directive('kcMarketTemplateLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-market-template-loader.html'
    };
});
module.directive('kcRealmListLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-realm-list-loader.html'
    };
});
module.directive('kcAuthCardLoader', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-auth-card-loader.html'
    };
});
module.directive('kcActionButtonLoading', function () {
    return {
        scope: {
            loadername: '=',
        },
        restrict: 'E',
        replace: true,
        controller: 'actionButtonLoadingCtrl',
        templateUrl: resourceUrl + '/templates/kc-action-button-loading.html'
    };
});
module.directive('kcSetupGuide', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        controller: 'SetupGuideCtrl',
        templateUrl: resourceUrl + '/templates/kc-setup-guide.html'
    };
});
module.directive('kcSetupGuideBox', function () {
    return {
        scope: {
            urlId: '=',
            urlGroup: '=',
            urlType: '='
        },
        restrict: 'E',
        replace: true,
        controller: 'SetupGuideBoxCtrl',
        templateUrl: resourceUrl + '/templates/kc-setup-guide-box.html'
    };
});
