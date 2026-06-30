'use strict';

var module = angular.module('keycloak.loaders', [ 'keycloak.services', 'ngResource' ]);

module.factory('Loader', function($q) {
	var loader = {};
	loader.get = function(service, id) {
		return function() {
			var i = id && id();
            var realmLoaderFlag = false;
            if(i["realmLoader"] && i["realmLoader"]) {
                delete i["realmLoader"];
                realmLoaderFlag = true;
            }
			var delay = $q.defer();
			service.get(i, function(entry) {
				delay.resolve(entry);
                if(realmLoaderFlag) {
                    realmObject = entry;
                }
			}, function() {
				delay.reject('Unable to fetch ' + i);
			});
			return delay.promise;
		};
	};
	loader.query = function(service, id) {
		return function() {
			var i = id && id();
			var delay = $q.defer();
			service.query(i, function(entry) {
				delay.resolve(entry);
			}, function() {
				delay.reject('Unable to fetch ' + i);
			});
			return delay.promise;
		};
	};
	return loader;
});

module.factory('RealmListLoader', function(Loader, Realm, $q) {
	return Loader.get(Realm);
});

module.factory('ServerInfoLoader', function(Loader, ServerInfo) {
    return function() {
        return ServerInfo.promise;
    };
});

module.factory('RealmLoader', function(Loader, Realm, $route, $q) {
	return Loader.get(Realm, function() {
		return {
			id : $route.current.params.realm,
            realmLoader : true
		}
	});
});

module.factory('SubRealmLoader', function(Loader, Realm, $route) {
	return Loader.get(Realm, function() {
		return {
			id : $route.current.params.subRealm
		}
	});
});

module.factory('RealmWorkspaceAttributeLoader', function(Loader, Realm, $route, $q) {
	return Loader.get(Realm, function() {
		return {
			id : $route.current.params.realm,
		}
	});
});

module.factory('RealmKeysLoader', function(Loader, RealmKeys, $route, $q) {
    return Loader.get(RealmKeys, function() {
        return {
            id : $route.current.params.realm
        }
    });
});

module.factory('EmailTemplatesLoader',function(Loader,EmailTemplates,$route,$q){
    return Loader.query(EmailTemplates,function(){
        return {
            realm: $route.current.params.realm
        }
    })
})

module.factory('PolicyListLoader',function(Loader, Policies, $route, $q){
    return Loader.query(Policies, function(){
        return {
            realm : $route.current.params.realm,
            first: 0,
            max: 20
        }
    })

});

module.factory('UserSyncAvailableLoader', function(Loader, UserSyncAvailable, $route, $q){
    return Loader.query(UserSyncAvailable, function(){
        return {
            realm: $route.current.params.realm
        }
    })
});

module.factory('DirectorySyncListLoader', function(Loader, DirectorySyncList, $route, $q){
    return Loader.get(DirectorySyncList, function(){
        return {
            realm: $route.current.params.realm,
            providerId: $route.current.params.provider_id
        }
    })
});

module.factory('DirectorySyncConnListLoader', function(Loader, DirectorySyncConnList, $route, $q){
    return Loader.get(DirectorySyncConnList, function(){
        return {
            realm: $route.current.params.realm
        }
    })
});

module.factory('DirectorySyncHistoryLoader', function(Loader,DirectorySyncHistory, $route, $q){
    return Loader.get(DirectorySyncHistory, function(){
        return {
            realm: $route.current.params.realm,
            providerId: $route.current.params.provider_id,
            connectionId: $route.current.params.connection_id
        }
    })
})

module.factory('DirectorySyncInfoLoader', function(Loader,DirectorySyncInfo, $route, $q){
    return Loader.get(DirectorySyncInfo, function(){
        return {
            realm: $route.current.params.realm,
            providerId: $route.current.params.provider_id,
            connectionId: $route.current.params.connection_id
        }
    })
})

module.factory('PolicyLoader',function(Loader, Policies, $route, $q){
    return Loader.get(Policies, function(){
        return {
            realm : $route.current.params.realm,
            policy: $route.current.params.policy
        }
    })

});

module.factory('BrandingAllLoader',function(Loader, BrandingAll, $route, $q){
    return Loader.query(BrandingAll, function(){
        return {
            realm : $route.current.params.realm
        }
    })

});

module.factory('BrandingLoader',function(Loader, Brandings, $route, $q){
    return Loader.get(Brandings, function(){
        return {
            realm : $route.current.params.realm,
            branding: $route.current.params.branding
        }
    })

});

module.factory('ActionListLoader',function(Loader, Actions, $route, $q){
    return Loader.query(Actions, function(){
        return {
            realm : $route.current.params.realm,
            action: $route.current.params.action
        }
    })
});

module.factory('ActionLoader',function(Loader, Actions, $route, $q){
    return Loader.get(Actions, function(){
        return {
            realm : $route.current.params.realm,
            action: $route.current.params.action
        }
    })
});

module.factory('ActivityLoader',function(Loader, Activity, $route, $q){
    return Loader.get(Activity, function(){
        return {
            realm : $route.current.params.realm
        }
    })
});

module.factory('BoActivityLoader',function(Loader, BoActivity, $route, $q){
    return Loader.get(BoActivity, function(){
        return {
            realm : $route.current.params.realm
        }
    })
});

module.factory('CibaPolicyListLoader', function(Loader, CibaPolicies, $route, $q) {
    return Loader.query(CibaPolicies, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('CibaTutorialLoader', function(Loader,CibaTutorial,$route, $q){
    return Loader.query(CibaTutorial, function() {
        return {
            realm : $route.current.params.realm,
            client: $route.current.params.client
        }
    });
});

module.factory('ActionPolicyListLoader', function(Loader, ActionPolicies, $route, $q) {
    return Loader.query(ActionPolicies, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('PasswordPolicyListLoader', function(Loader, PasswordPolicies, $route, $q) {
    return Loader.query(PasswordPolicies, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('RealmEventsConfigLoader', function(Loader, RealmEventsConfig, $route, $q) {
    return Loader.get(RealmEventsConfig, function() {
        return {
            id : $route.current.params.realm
        }
    });
});

module.factory('RealmAPILogsLoader', function(Loader, RealmApiLogsConfig, $route, $q) {
    return Loader.get(RealmApiLogsConfig, function() {
        return {
            id : $route.current.params.realm
        }
    });
});

module.factory('RealmMFAConfigLoader', function(Loader, RealmMFAConfig, $route, $q) {
    return Loader.get(RealmMFAConfig, function() {
        return {
            id : $route.current.params.realm
        }
    });
});

module.factory('RealmEmailConfigLoader', function(Loader, RealmEmailConfig, $route, $q) {
    return Loader.get(RealmEmailConfig, function() {
        return {
            id : $route.current.params.realm
        }
    });
});

module.factory('RealmSmsConfigLoader', function(Loader, RealmSmsConfig, $route, $q) {
    return Loader.get(RealmSmsConfig, function() {
        return {
            id : $route.current.params.realm
        }
    });
});

module.factory('EventsConfigLoader',function(Loader, EventsConfig, $route, $q) {
    return Loader.get(EventsConfig, function() {
        return {
            realm : $route.current.params.realm,
            provider: $route.current.params.provider
        }
    });
})

module.factory('UserListLoader', function(Loader, User, $route, $q) {
    return Loader.query(User, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('RequiredActionsListLoader', function(Loader, RequiredActions, $route, $q) {
    return Loader.query(RequiredActions, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('UnregisteredRequiredActionsListLoader', function(Loader, UnregisteredRequiredActions, $route, $q) {
    return Loader.query(UnregisteredRequiredActions, function() {
        return {
            realm : $route.current.params.realm,
            action: $route.current.params.action
        }
    });
});

module.factory('RealmSessionStatsLoader', function(Loader, RealmSessionStats, $route, $q) {
    return Loader.get(RealmSessionStats, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('RealmClientSessionStatsLoader', function(Loader, RealmClientSessionStats, $route, $q) {
    return Loader.query(RealmClientSessionStats, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ClientProtocolMapperLoader', function(Loader, ClientProtocolMapper, $route, $q) {
    return Loader.get(ClientProtocolMapper, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client,
            id: $route.current.params.id
        }
    });
});

module.factory('ClientScopeProtocolMapperLoader', function(Loader, ClientScopeProtocolMapper, $route, $q) {
    return Loader.get(ClientScopeProtocolMapper, function() {
        return {
            realm : $route.current.params.realm,
            clientScope : $route.current.params.clientScope,
            id: $route.current.params.id
        }
    });
});

module.factory('UserLoader', function(Loader, User, $route, $q) {
    return Loader.get(User, function() {
        return {
            realm : $route.current.params.realm,
            userId : $route.current.params.user
        }
    });
});

module.factory('ComponentLoader', function(Loader, Components, $route, $q) {
    return Loader.get(Components, function() {
        return {
            realm : $route.current.params.realm,
            componentId: $route.current.params.componentId
        }
    });
});

module.factory('LDAPMapperLoader', function(Loader, Components, $route, $q) {
    return Loader.get(Components, function() {
        return {
            realm : $route.current.params.realm,
            componentId: $route.current.params.mapperId
        }
    });
});

module.factory('ComponentsLoader', function(Loader, Components, $route, $q) {
    var componentsLoader = {};

    componentsLoader.loadComponents = function(parent, componentType) {
        return Loader.query(Components, function() {
            return {
                realm : $route.current.params.realm,
                parent : parent,
                type: componentType
            }
        })();
    };

    return componentsLoader;
});

module.factory('SubComponentTypesLoader', function(Loader, SubComponentTypes, $route, $q) {
    var componentsLoader = {};

    componentsLoader.loadComponents = function(parent, componentType) {
        return Loader.query(SubComponentTypes, function() {
            return {
                realm : $route.current.params.realm,
                componentId : parent,
                type: componentType
            }
        })();
    };

    return componentsLoader;
});

module.factory('UserSessionStatsLoader', function(Loader, UserSessionStats, $route, $q) {
    return Loader.get(UserSessionStats, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user
        }
    });
});

module.factory('UserSessionsLoader', function(Loader, UserSessions, $route, $q) {
    return Loader.query(UserSessions, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user
        }
    });
});

module.factory('UserOfflineSessionsLoader', function(Loader, UserOfflineSessions, $route, $q) {
    return Loader.query(UserOfflineSessions, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user,
            client : $route.current.params.client
        }
    });
});

module.factory('UserFederatedIdentityLoader', function(Loader, UserFederatedIdentities, $route, $q) {
    return Loader.query(UserFederatedIdentities, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user
        }
    });
});

module.factory('UserConsentsLoader', function(Loader, UserConsents, $route, $q) {
    return Loader.query(UserConsents, function() {
        return {
            realm : $route.current.params.realm,
            user : $route.current.params.user
        }
    });
});



module.factory('RoleLoader', function(Loader, RoleById, $route, $q) {
    return Loader.get(RoleById, function() {
        return {
            realm : $route.current.params.realm,
            role : $route.current.params.role
        }
    });
});

module.factory('RoleListLoader', function(Loader, Role, $route, $q) {
    return Loader.query(Role, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ClientRoleLoader', function(Loader, RoleById, $route, $q) {
    return Loader.get(RoleById, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client,
            role : $route.current.params.role
        }
    });
});

module.factory('ClientSessionStatsLoader', function(Loader, ClientSessionStats, $route, $q) {
    return Loader.get(ClientSessionStats, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientSessionCountLoader', function(Loader, ClientSessionCount, $route, $q) {
    return Loader.get(ClientSessionCount, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientOfflineSessionCountLoader', function(Loader, ClientOfflineSessionCount, $route, $q) {
    return Loader.get(ClientOfflineSessionCount, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientDefaultClientScopesLoader', function(Loader, ClientDefaultClientScopes, $route, $q) {
    return Loader.query(ClientDefaultClientScopes, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientOptionalClientScopesLoader', function(Loader, ClientOptionalClientScopes, $route, $q) {
    return Loader.query(ClientOptionalClientScopes, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientLoader', function(Loader, Client, $route, $q) {
    return Loader.get(Client, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});

module.factory('ClientSSOMetadataLoader', function(Loader, ClientSSOMetadata, $route, $q) {
    return Loader.get(ClientSSOMetadata, function() {
        return {
            realm : $route.current.params.realm,
            client: $route.current.params.client
        }
    });
});

module.factory('ClientListLoader', function(Loader, Client, $route, $q) {
    return Loader.query(Client, function() {
        return {
            realm : $route.current.params.realm,
            first: 0,
            max: 20
        }
    });
});

module.factory('ClientScopeLoader', function(Loader, ClientScope, $route, $q) {
    return Loader.get(ClientScope, function() {
        return {
            realm : $route.current.params.realm,
            clientScope : $route.current.params.clientScope
        }
    });
});

module.factory('ClientScopeListLoader', function(Loader, ClientScope, $route, $q) {
    return Loader.query(ClientScope, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('RealmDefaultClientScopesLoader', function(Loader, RealmDefaultClientScopes, $route, $q) {
    return Loader.query(RealmDefaultClientScopes, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('RealmOptionalClientScopesLoader', function(Loader, RealmOptionalClientScopes, $route, $q) {
    return Loader.query(RealmOptionalClientScopes, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('ClientServiceAccountUserLoader', function(Loader, ClientServiceAccountUser, $route, $q) {
    return Loader.get(ClientServiceAccountUser, function() {
        return {
            realm : $route.current.params.realm,
            client : $route.current.params.client
        }
    });
});


module.factory('RoleMappingLoader', function(Loader, RoleMapping, $route, $q) {
	var realm = $route.current.params.realm || $route.current.params.client;

	return Loader.query(RoleMapping, function() {
		return {
			realm : realm,
			role : $route.current.params.role
		}
	});
});

module.factory('EmailProviderLoader',function(Loader,EmailProvider,$route,$q){
    return Loader.query(EmailProvider, function() {
        return {
            realm: $route.current.params.realm 
        }
    })
});

module.factory('IdentityProviderLoader', function(Loader, IdentityProvider, $route, $q) {
    return Loader.get(IdentityProvider, function () {
        return {
            realm: $route.current.params.realm,
            alias: $route.current.params.alias
        }
    });
});

module.factory('IdentityProviderFactoryLoader', function(Loader, IdentityProviderFactory, $route, $q) {
    return Loader.get(IdentityProviderFactory, function () {
        return {
            realm: $route.current.params.realm,
            provider_id: $route.current.params.provider_id
        }
    });
});

module.factory('IdentityProviderMapperTypesLoader', function(Loader, IdentityProviderMapperTypes, $route, $q) {
    return Loader.get(IdentityProviderMapperTypes, function () {
        return {
            realm: $route.current.params.realm,
            alias: $route.current.params.alias
        }
    });
});

module.factory('IdentityProviderMappersLoader', function(Loader, IdentityProviderMappers, $route, $q) {
    return Loader.query(IdentityProviderMappers, function () {
        return {
            realm: $route.current.params.realm,
            alias: $route.current.params.alias
        }
    });
});

module.factory('IdentityProviderMapperLoader', function(Loader, IdentityProviderMapper, $route, $q) {
    return Loader.get(IdentityProviderMapper, function () {
        return {
            realm: $route.current.params.realm,
            alias: $route.current.params.alias,
            mapperId: $route.current.params.mapperId
        }
    });
});

module.factory('AuthenticationFlowsLoader', function(Loader, AuthenticationFlows, $route, $q) {
    return Loader.query(AuthenticationFlows, function() {
        return {
            realm : $route.current.params.realm,
            flow: ''
        }
    });
});

module.factory('AuthenticationFlowLoader', function(Loader, AuthenticationFlows, $route, $q) {
    return Loader.query(AuthenticationFlows, function() {
        return {
            realm : $route.current.params.realm,
            flow: $route.current.params.flow
        }
    });
});

module.factory('AuthenticationFormProvidersLoader', function(Loader, AuthenticationFormProviders, $route, $q) {
    return Loader.query(AuthenticationFormProviders, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticationFormActionProvidersLoader', function(Loader, AuthenticationFormActionProviders, $route, $q) {
    return Loader.query(AuthenticationFormActionProviders, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticatorProvidersLoader', function(Loader, AuthenticatorProviders, $route, $q) {
    return Loader.query(AuthenticatorProviders, function() {
        return {
            realm : $route.current.params.realm,
            flowtype: $route.current.params.flowtype
        }
    });
});

module.factory('ClientAuthenticatorProvidersLoader', function(Loader, ClientAuthenticatorProviders, $route, $q) {
    return Loader.query(ClientAuthenticatorProviders, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticationFlowLoader', function(Loader, AuthenticationFlows, $route, $q) {
    return Loader.get(AuthenticationFlows, function() {
        return {
            realm : $route.current.params.realm,
            flow: $route.current.params.flow
        }
    });
});

module.factory('MarketplaceCategoryLoader', function(Loader, MarketplaceCategories, $route, $q) {
    return Loader.query(MarketplaceCategories, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('MarketplaceTemplateLoader', function(Loader, MarketplaceTemplate, $route, $q) {
    return Loader.get(MarketplaceTemplate, function() {
        return {
            realm : $route.current.params.realm,
            category: $route.current.params.category,
            templateid: $route.current.params.templateid
        }
    });
});

module.factory('AuthenticationInstalledFlowsLoader', function(Loader, AuthenticationInstalledFlows, $route, $q) {
    return Loader.query(AuthenticationInstalledFlows, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticationInstalledOnboardingFlowsLoader', function(Loader, AuthenticationInstalledOnboardingFlows, $route, $q) {
    return Loader.query(AuthenticationInstalledOnboardingFlows, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticationInstalledRecoveryFlowsLoader', function(Loader, AuthenticationInstalledRecoveryFlows, $route, $q) {
    return Loader.query(AuthenticationInstalledRecoveryFlows, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticationInstalledLoginFlowsLoader', function(Loader, AuthenticationInstalledLoginFlows, $route, $q) {
    return Loader.query(AuthenticationInstalledLoginFlows, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('AuthenticationInstalledDirectGrantFlowsLoader', function(Loader, AuthenticationInstalledDirectGrantFlows, $route, $q) {
    return Loader.query(AuthenticationInstalledDirectGrantFlows, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('MarketplaceTemplatesLoader', function(Loader, MarketplaceTemplates, $route, $q) {
    return Loader.query(MarketplaceTemplates, function() {
        return {
            realm : $route.current.params.realm,
            category: $route.current.params.category
        }
    });
});

module.factory('AuthenticationConfigDescriptionLoader', function(Loader, AuthenticationConfigDescription, $route, $q) {
    return Loader.get(AuthenticationConfigDescription, function () {
        return {
            realm: $route.current.params.realm,
            provider: $route.current.params.provider
        }
    });
});

module.factory('EventConfigDescriptionLoader', function(Loader, EventsConfigDescription, $route, $q) {
    return Loader.get(EventsConfigDescription, function () {
        return {
            realm: $route.current.params.realm,
            provider: $route.current.params.provider
        }
    });
});

module.factory('PerClientAuthenticationConfigDescriptionLoader', function(Loader, PerClientAuthenticationConfigDescription, $route, $q) {
    return Loader.get(PerClientAuthenticationConfigDescription, function () {
        return {
            realm: $route.current.params.realm
        }
    });
});

module.factory('ExecutionIdLoader', function($route) {
    return function() { return $route.current.params.executionId; };
});

module.factory('AuthenticationConfigLoader', function(Loader, AuthenticationConfig, $route, $q) {
    return Loader.get(AuthenticationConfig, function () {
        return {
            realm: $route.current.params.realm,
            config: $route.current.params.config
        }
    });
});

module.factory('GroupListLoader', function(Loader, Groups, $route, $q) {
    return Loader.query(Groups, function() {
        return {
            realm : $route.current.params.realm
        }
    });
});

module.factory('GroupCountLoader', function(Loader, GroupsCount, $route, $q) {
    return Loader.query(GroupsCount, function() {
        return {
            realm : $route.current.params.realm,
            top : true
        }
    });
});

module.factory('GroupLoader', function(Loader, Group, $route, $q) {
    return Loader.get(Group, function() {
        return {
            realm : $route.current.params.realm,
            groupId : $route.current.params.group
        }
    });
});

module.factory('ClientInitialAccessLoader', function(Loader, ClientInitialAccess, $route) {
    return Loader.query(ClientInitialAccess, function() {
        return {
            realm: $route.current.params.realm
        }
    });
});

module.factory('ClientRegistrationPolicyProvidersLoader', function(Loader, ClientRegistrationPolicyProviders, $route) {
    return Loader.query(ClientRegistrationPolicyProviders, function() {
        return {
            realm: $route.current.params.realm
        }
    });
});

module.factory('LicenseCheckLoader', function(Loader,LicenseChecker, $route) {
    return Loader.get(LicenseChecker, function() {
        return {
            realm: $route.current.params.realm
        }
    });
});

module.factory('RealmSpecialAttributesAvailableLoader', function(Loader, RealmSpecialAttributesAvailable, $route) {
    return Loader.get(RealmSpecialAttributesAvailable, function() {
        return {
            realm: $route.current.params.realm
        }
    })
})

module.factory('RealmSpecialAttributesLoader', function(Loader, RealmSpecialAttributes, $route){
    return Loader.get(RealmSpecialAttributes, function(){
        return {
            realm: $route.current.params.realm
        }
    })
})

module.factory('AccessKeyLoader', function(Loader, AccessKey, $route) {
	return Loader.get(AccessKey, function() {
		return {
            realm: $route.current.params.realm,
			accessKey : $route.current.params.accessKey
		}
	});
});
