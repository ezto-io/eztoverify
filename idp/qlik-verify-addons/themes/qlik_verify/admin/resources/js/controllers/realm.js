 //color codes to show in FlowChart
const configurableNodeColor = "#FCFAD9";
const configurableNodeBorderColor = "#D8CC2C";
const enabledNodeColor = "#DBEDDD";
const enabledNodeBorderColor = "#4CA355";
const disabledNodeColor = "#EBEBEB";
const disabledNodeBorderColor = "#BBBBBB";
const notConfiguredNodeColor = "#FFF2E5";
const notConfiguredNodeBorderColor = "#F04141";
const validatorNodeColor = "#E8EFFD";
const validatorNodeBorderColor = "#90B3F5";

//sideMenuBar to shrink and expand
const sideMenu = document.querySelector("#body-container");

function getAccess(Auth, Current, role) {
    if (!Current.realm) return false;
    var realmAccess = Auth.user && Auth.user['realm_access'];
    if (realmAccess) {
        realmAccess = realmAccess[Current.realm.realm];
        if (realmAccess) {
            return realmAccess.indexOf(role) >= 0;
        }
    }
    return false;
}

function getAccessObject(Auth, Current) {
    return {
        get createRealm() {
            return Auth.user && Auth.user.createRealm;
        },

        get queryUsers() {
            return getAccess(Auth, Current, 'query-users') || this.viewUsers;
        },

        get queryGroups() {
            return getAccess(Auth, Current, 'query-groups') || this.viewUsers;
        },

        get queryClients() {
            return getAccess(Auth, Current, 'query-clients') || this.viewClients;
        },

        get viewRealm() {
            return getAccess(Auth, Current, 'view-workspace') || getAccess(Auth, Current, 'manage-workspace') || this.manageRealm;
        },

        get viewClients() {
            return getAccess(Auth, Current, 'view-clients') || getAccess(Auth, Current, 'manage-clients') || this.manageClients;
        },

        get viewUsers() {
            return getAccess(Auth, Current, 'view-users') || getAccess(Auth, Current, 'manage-users') || this.manageClients;
        },

        get viewEvents() {
            return getAccess(Auth, Current, 'view-events') || getAccess(Auth, Current, 'manage-events') || this.manageClients;
        },

        get viewIdentityProviders() {
            return getAccess(Auth, Current, 'view-identity-providers') || getAccess(Auth, Current, 'manage-identity-providers') || this.manageIdentityProviders;
        },

        get viewAuthorization() {
            return getAccess(Auth, Current, 'view-authorization') || this.manageAuthorization;
        },

        get manageRealm() {
            return getAccess(Auth, Current, 'manage-workspace');
        },

        get manageClients() {
            return getAccess(Auth, Current, 'manage-workspace');
        },

        get manageUsers() {
            return getAccess(Auth, Current, 'manage-users');
        },

        get manageEvents() {
            return getAccess(Auth, Current, 'manage-events');
        },

        get manageIdentityProviders() {
            return getAccess(Auth, Current, 'manage-identity-providers');
        },

        get manageAuthorization() {
            return getAccess(Auth, Current, 'manage-authorization');
        },

        get impersonation() {
            return getAccess(Auth, Current, 'impersonation');
        }
    };
}

module.controller('GlobalCtrl', function ($scope, $http, $timeout, Auth, Current, $location, Notifications, ServerInfo, IntroJs, $timeout, $rootScope, IntroManagement, $interval, IntroJsDetails, $route, SetupGuides, $sce, WhoAmI) {
    let splittedPath = $location.path().substring(1).split("/");
    if (splittedPath.length > 0) {
        let oldBrandingAttributes = JSON.parse(sessionStorage.getItem(`${splittedPath[1]}branding`));
        if (oldBrandingAttributes) {
            for (const [key, value] of Object.entries(oldBrandingAttributes)) {
                if (key === 'primary') {
                    $(':root').css('--primaryOpaque', hexToRgbA(value));
                }
                $(':root').css(`--${key}`, value);
            }
        }
    }
    $scope.authUrl = authUrl;
    $scope.resourceUrl = resourceUrl;
    $scope.serverVersion = serverVersion;
    $scope.auth = Auth;
    $scope.introDetails = null;
    $scope.modalDetails = null;
    $scope.originalPath = null;
    $scope.introMappersList = null;
    $scope.modalId = null;
    $scope.introExecuted = false;
    $scope.showSwitch = 'true';
    $scope.serverInfo = ServerInfo.get(Current.realm, introCallback);
    $scope.access = getAccessObject(Auth, Current);
    $scope.rootAccount = false;
    $scope.rootAccountName = "";
    $rootScope.finalMapperGuides = {};
    $scope.mapperGuidesLoaded = false;
    $scope.currentGuideUrl = '';
    $scope.isRootAccountCreator = false;
    $scope.isRTL = () => locale == 'ar';
    $scope.getImage = function (imageData) {
        return getUpdatedImgData(imageData);
    };
    $scope.currentRealm = Current;
    $rootScope.showBranding = true;
    $rootScope.isCibaEnabled = true;
    function introCallback(response) {
        if (!response?.data?.systemInfo?.multiTenant) {
            $scope.showSwitch = 'false';
        }
        if (response?.data?.brandInfo?.brandingEnabled) {
            $rootScope.showBranding = true;
        } else {
            $rootScope.showBranding = false;
        }
        $rootScope.isCibaEnabled = response?.data?.cibaFeatureEnabled;
        $scope.rootAccountName = response?.data?.rootAccountName;
        if (response?.data?.systemInfo?.offering == 'CLOUD' && Auth.user.realm == $scope.rootAccountName ) {
            $http.get(authUrl + '/admin/realms/' + Auth.user.realm + "/users/" + Auth.user.userId).then(function (res) {
                let data = res.data;
                $scope.isRootAccountCreator = data?.attributes?.qv_is_account_creator[0] === "true";
            });
        }
        if (Current?.realm?.displayName) {
            document.title = `${Current.realm.displayName.slice(0, 1).toUpperCase() + Current.realm.displayName.slice(1)} Admin`;
        } else if (Current?.realm?.realm) {
            document.title = `${Current.realm.realm.slice(0, 1).toUpperCase() + Current.realm.realm.slice(1)} Admin`;
        } else if ($scope?.rootAccountName) {
            document.title = `${($scope.rootAccountName).toUpperCase()} Admin`;
        }
        if ((response?.data?.rootAccountName === Current?.realm?.realm) || (response?.data?.rootAccountName === Auth?.user?.realm)) {
            $scope.rootAccount = true;
        }

        if (response?.data?.brandInfo?.attributes) {
            const brandingAttributes = response?.data?.brandInfo?.attributes;
            if (splittedPath.length > 0) {
                sessionStorage.setItem(`${splittedPath[1]}branding`, JSON.stringify(brandingAttributes));
            }
            for (const [key, value] of Object.entries(brandingAttributes)) {
                if (key === 'primary') {
                    $(':root').css('--primaryOpaque', hexToRgbA(value));
                }
                $(':root').css(`--${key}`, value);
            }
        }
        if (response?.data?.brandInfo?.brandingEnabled) {
            $scope.$on("$routeChangeSuccess", function (event, newUrl) {
                if (
                    newUrl.$$route.originalPath !==
                    "/realms/:realm/marketplace/templates/:category" &&
                    newUrl.$$route.originalPath !== "/realms/:realm/brandings/:branding"
                ) {
                    sideMenu.classList.remove("custom-menu-true");
                }
            });
        }
    };

    $scope.getPanelDirection = function () {
        return locale == 'ar' ? 'left' : 'right';
    }

    $scope.makeSideMenuClosed = function () {
        $timeout(function() {
            document.getElementById("body-container").classList.add("custom-menu-true");
        }, 0);
    }

    function hexToRgbA(hex) {
        var c;
        if (/^#([A-Fa-f0-9]{3}){1,2}$/.test(hex)) {
            c = hex.substring(1).split('');
            if (c.length == 3) {
                c = [c[0], c[0], c[1], c[1], c[2], c[2]];
            }
            c = '0x' + c.join('');
            return 'rgba(' + [(c >> 16) & 255, (c >> 8) & 255, c & 255].join(',') + ',0.25)';
        }
        throw new Error('Bad Hex');
    }

    // to make the support element container iFrame width equal to the content.
    let retryLimitToFindSupport = 10;
    $scope.supportInterval = $interval(() => {
        let supportContainer = document.querySelector("#launcher-frame");
        const iWindow = supportContainer?.contentWindow;
        const iDocument = iWindow?.document;
        let launcherButton = iDocument?.querySelector(".launcher-button");
        let documentBody = iDocument?.querySelector("body");
        if (supportContainer && launcherButton) {
            documentBody.style.margin = "0px";
            let launcherButtonWidth = Number(
                launcherButton.getBoundingClientRect().width
            );
            let launcherButtonHeight = launcherButton.clientHeight;
            launcherButton.style.boxShadow = "none";
            launcherButton.style.transform = "scale(1)";
            launcherButton.style.animation = "none";
            supportContainer.style.borderRadius = "30px 8px 30px 30px";
            supportContainer.style.maxWidth = `${launcherButtonWidth}px`;
            supportContainer.style.maxHeight = `${launcherButtonHeight}px`;
            $interval.cancel($scope.supportInterval);
        }
        if (!retryLimitToFindSupport) {
            $interval.cancel($scope.supportInterval);
        }
        --retryLimitToFindSupport;
    }, 1000);

    // updating title when the current realm changes
    $scope.$watch(
        "currentRealm",
        function () {
            if ($scope.currentRealm?.realm?.realm && !($scope.auth?.user?.realm_access?.[$scope.currentRealm.realm.realm])) {
                WhoAmI.get($scope.currentRealm.realm.realm, function (res) {
                    $scope.auth.user = res.data;
                });
            }
            if ($scope.currentRealm?.realm?.displayName) {
                document.title = `${$scope.currentRealm.realm.displayName.slice(0, 1).toUpperCase() + $scope.currentRealm.realm.displayName.slice(1)} Admin`;
            } else if ($scope.currentRealm?.realm?.realm) {
                document.title = `${$scope.currentRealm.realm.realm.slice(0, 1).toUpperCase() + $scope.currentRealm.realm.realm.slice(1)} Admin`;
            }
        },
        true
    );

    // function to split paths.
    $scope.$watch(
        function () {
            return $location.path();
        },
        function () {
            $scope.fragment = $location.path();
            $scope.path = $location.path().substring(1).split("/");
        }
    );

    $rootScope.initializeSetupGuide = function (urlId, urlGroup, urlType) {
        $rootScope.setupGuideDetails = {
            urlId,
            urlGroup,
            urlType
        };
    };

    $scope.openSupportWidget = function (urlId, urlGroup, urlType) {
    };

}
);

module.controller('HomeCtrl', function (Realm, Auth, Current, $location, serverInfo) {
    if (Auth.user.realm) {
        // if(Auth.user.realm === serverInfo?.rootAccountName){
        //     $location.url('/realms');
        // }else{
        Realm.get({ id: Auth.user.realm }, function (realm) {
            Current.realm = realm;
            var access = getAccessObject(Auth, Current);
            if (access.viewRealm || access.manageRealm) {
                $location.url('/realms/' + Current.realm.realm);
            } else {
                $location.url('/realms');
            }
        });
        // }
    } else {
        $location.url('/realms');
    }
});

module.controller('AuthTabCtrl', function ($scope, $route, AuthCounter) {
    $scope.counter = {};
    $scope.isRTL = () => locale == 'ar';
    AuthCounter.get({ realm: $route.current.params.realm }, function (res) {
        $scope.counter = res;
    });
});

module.controller('RealmTabCtrl', function (InputDialog, $scope, $rootScope, $route, Current, Realm, Notifications, $location, $translate, RealmSpecialAttributes, Auth) {
    $scope.removeTenantDesc = $translate.instant('remove-tenant-content', {variant:variant});
    $scope.isWorkspaceAttributeTabOpen = $scope.path[2] !== 'realm-attributes' ? false : true;
    $scope.isOwner = Auth.user.isAccountCreator;
    $scope.isRTL = locale == 'ar';
    if (!Current.realm?.realm) {
        return;
    }
    $scope.isCustomPricedTenant = (Current.realm.attributes && Current.realm.attributes.hasOwnProperty('tenant_pricing_type') && Current.realm.attributes['tenant_pricing_type'] == 'custom') ? true : false;
    $scope.isRootAccount = Current.realm.realm === $scope.rootAccountName;
    $scope.isAdminRealmUser = $scope.rootAccountName === Auth.user.realm
    $scope.isAPIEnabled = Current.realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = Current.realm.attributes['isPlatformWorkspace'] === "true" ? true : false;

    $scope.toggleWorkspaceAttribute = function () {
        $scope.isWorkspaceAttributeTabOpen = !$scope.isWorkspaceAttributeTabOpen;
    }

    $scope.$watch("isWorkspaceAttributeTabOpen", function () {
            if($scope.path[2] !== 'realm-attributes' && Current.realm.realm){
                RealmSpecialAttributes.query({ realm: Current.realm.realm }, function (data) {
                    $scope.loading = false;
                    $scope.specialAttributes = angular.copy(data);
                    if ($scope.specialAttributes) {
                        $scope.specialAttributes.forEach(function (data) {
                        data.hideTab = !data.attributes || data.attributes.every(attr => !attr.showInSubTenant);
                        });
                    }
                }, function (data) {
                    $scope.loading = false;
                });
            }
    }, true);

    $scope.removeRealm = function () {
        InputDialog.confirmTenantRemove(Current.realm.realm, 'Workspace', function () {
            Realm.remove({ id: Current.realm.realm }, function () {
                //Current.realms = Realm.query();
                Notifications.success($translate.instant("workspace-removed-successfully"));
                location.reload(true);
            });
        });
    };

    $scope.deleteRealm = function () {
        InputDialog.confirmTenantDelete(Current.realm.realm, $translate.instant('workspace_txt'), function () {
            Realm.remove({ id: Current.realm.realm }, function () {
                //Current.realms = Realm.query();
                Notifications.success($translate.instant("workspace-deleted-successfully"));
                $rootScope.isTenantDelReq = true;
                $location.url("/realms");
            });
        });
    };

    $scope.restoreRealm = function () {
        Realm.remove({ id: Current.realm.realm }, function () {
            //Current.realms = Realm.query();
            Notifications.success($translate.instant("workspace-restored-successfully"));
            location.reload(true);
        });
    };

    $scope.selectAttribute = function (attribute) {
        $scope.isWorkspaceAttributeTabOpen = $scope.path[2] !== 'realm-attributes' ? false : true;
        $location.url("/realms/" + Current.realm.realm + "/realm-attributes/" + attribute);
    }

});

module.controller('ServerInfoCtrl', function ($scope, ServerInfo, Current, serverInfo) {
    ServerInfo.reload();
    $scope.serverInfo = serverInfo;

    $scope.$watch($scope.serverInfo, function () {
        $scope.providers = [];
        for (var spi in $scope.serverInfo.providers) {
            var p = angular.copy($scope.serverInfo.providers[spi]);
            p.name = spi;
            $scope.providers.push(p);
        }
    });

    $scope.serverInfoReload = function () {
        ServerInfo.reload();
    };
});

module.controller('RealmListCtrl', function ($scope,realm, $http, GetAllRealms, Session, Current, ServerInfo, User) {
    realmObject = {};
    $scope.serverInfo = null;
    $scope.current = Current;
    Current.realm = null;
    $scope.isLoaded = false;
    if (realm) {
        $scope.realm = realm;
    }
    $scope.variant = variant;
    $scope.mode =  'live';
    $scope.isBo = variant === 'ezto verify';
    $scope.setMode = (mode) => {
        $scope.mode = mode;
        $scope.init()
    }
    $scope.init = function () {
        $scope.loaderTypeName = realm ? 'roleListLoader' : 'realmListLoader';
        $scope.query = {
            first: 0
        };
        if ($scope.isBo && !realm) {
            $scope.query.sandboxMode = $scope.mode === "sandbox"
        }
        $scope.maxValue = $scope.query.max = Session.getItem("realmFilterCount") ? Session.getItem("realmFilterCount") : 10;
        $scope.presentIndex = 0;
        $scope.searchData = "";
        ServerInfo.get(Current.realm, function (res) {
            $scope.showMode = false;
            $scope.serverInfo = res.data;
            if (res?.data?.brandInfo?.modeEnabled) {
                $scope.showMode = true;
            }
            $scope.getRealmCount($scope.updateRealmList);
        });
    };
    $scope.getRealmCount = function (callback) {
        let requestParams = $scope.query.realmName ? `?realmName=${$scope.query.realmName}&search=true` : '';
        $scope.isLoaded = false;
        const url = authUrl + '/admin/realms/count' + requestParams + (realm ? "" : `?sandboxMode=${$scope.query.sandboxMode}`)
        console.log("url", url)
        $http.get(url).then(function (res) {
            $scope.realmcount = res.data;
            if (callback) {
                callback();
            } else {
                $scope.isLoaded = true;
            }
        });
    };
    $scope.updateRealmList = function () {
        $scope.isLoaded = false;
        !$scope.query.realmName ? delete $scope.query.realmName : '';
        GetAllRealms.query($scope.query, function (res) {
            $scope.realms = res;
            $scope.isLoaded = true;
        });
    };
    $scope.selectQuery = function (maxValue) {
        $scope.maxValue = $scope.query.max = maxValue;
        $scope.presentIndex = $scope.query.first = 0;
        Session.setItem("realmFilterCount", $scope.query.max);
        $scope.updateRealmList();
    };
    $scope.searchRealm = function (searchKey) {
        $scope.query.realmName = searchKey;
        $scope.presentIndex = $scope.query.first = 0;
        if ($scope.query.realmName) {
            $scope.query.search = true;
            $scope.query.max = $scope.maxValue;
        } else {
            delete $scope.query.search;
            delete $scope.query.realmName;
            $scope.query.max = $scope.maxValue;
        }
        $scope.getRealmCount($scope.updateRealmList);
    };
    $scope.previousPage = function () {
        $scope.presentIndex -= $scope.query.max;
        ($scope.presentIndex > 0) ? $scope.presentIndex : 0;
        $scope.query.first = $scope.presentIndex;
        $scope.updateRealmList();
    };
    $scope.reset = function () {
        delete $scope.query.search;
        delete $scope.query.realmName;
        $scope.query.max = $scope.maxValue;
        $("#realmSearchKey").val("");
        $scope.updateRealmList();
    };
    $scope.firstPage = function () {
        $scope.presentIndex = $scope.query.first = 0;
        $scope.updateRealmList();
    };
    $scope.nextPage = function () {
        $scope.presentIndex += $scope.query.max;
        $scope.query.first = $scope.presentIndex;
        $scope.updateRealmList();
    };
    $scope.clearSearch = function () {
        $("#realmSearchKey").val("");
        $scope.searchKey = "";
        $scope.searchRealm($scope.searchKey);
    };
});

module.controller('RealmDropdownCtrl', function ($scope, Realm, Current, Auth,  $location, Notifications, RealmTenantCreateDialog, $route, ServerInfo, $translate) {
    const sideMenuSlider = document.querySelector("#body-container");
    $scope.isOwner = Auth.user.isAccountCreator && Auth.user.realm === $scope.realm.realm;
    $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === 'true' ? true : false;
    $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === 'true' ? true  : false;

    // Realm.get({id: $route.current.params.realm}, function(realm) {
    //     $scope.realm = realm;
    // })

    $scope.isRTL = locale == 'ar'
    Current.realm = angular.copy($scope.realm);
    $scope.current = Current;
    const toggleOnTabSize = function (screenSize) {
        if (screenSize && screenSize <= 1007 && !sideMenuSlider.classList.contains("custom-menu-true")) {
            sideMenuSlider.classList.add("custom-menu-true");
        }
    };
    toggleOnTabSize(window.screen.width);
    $scope.toggleSideMenu = function () {
        sideMenuSlider.classList.toggle("custom-menu-true");
    };
    $scope.changeRealm = function (selectedRealm) {
        $location.url("/realms/" + selectedRealm);
    };
    $scope.createNewRealm = function () {
        ServerInfo.get(Current.realm, function (res) {
            let showMode = false;
            if (res?.data?.brandInfo?.modeEnabled) {
                showMode = true;
            }
            RealmTenantCreateDialog.open($translate.instant('create-tenant'), showMode, res?.data?.systemInfo.mode, '', function (realm, modalScope) {
                var realmCopy = angular.copy(realm);
                Realm.create(realmCopy, function () {
                    Notifications.success($translate.instant("workspace-created-successfully"));
                    Auth.refreshPermissions(function () {
                        $scope.$apply(function () {
                            $location.url("/realms/" + realmCopy.realm);
                        });
                        //$route.reload();
                    });
                }, function () {
                    modalScope.stage = 1;
                });
            });
        });
    };
    // submenu handler
    $scope.submenuControllerEnter = (event) => {
        const eventController = event.currentTarget;
        eventController.classList.add("submenu-active");
    };
    $scope.submenuControllerOnLeave = (event) => {
        const eventController = event.currentTarget;
        eventController.classList.remove("submenu-active");
    };
    $scope.showSideMenu = () => {
        document.querySelector(".menu-w-wrapper").style.zIndex = 5;
        document.querySelector(".menu-w-wrapper").style.width = "100%";
    };
    $scope.hideSideMenu = () => {
        document.querySelector(".menu-w-wrapper").style.zIndex = 0;
        document.querySelector(".menu-w-wrapper").style.width = "auto";
    };
    // auto toggle on tablets
    window.addEventListener("resize", () => {
        toggleOnTabSize(window.screen.width);
    });
});

module.controller('RealmTopNavCtrl', function ($scope, Realm, User, Current, Auth, ServerInfo,ComponentUtils,$location, Notifications, RealmTenantCreateDialog, $route, OnboardingToursDialog, $rootScope, $translate) {
    $scope.isToursActive = false;
    $scope.isRealmAvailable = false
    $scope.current = '';
    $scope.docsUrl = '';
    $scope.auth = '';
    $scope.loadingText = '';


    ServerInfo.get("", function (res){
        $scope.serverInfo = res.data;
        if ($scope.serverInfo?.supportedLocales) {
            $scope.supportedLocalesEntity = ISO6391.getLanguages($scope.serverInfo?.supportedLocales);
        }
    } );

    $scope.getMessageJson = function (changeDir) {
        loadResourceBundle(consoleBaseUrl + 'messages.json?lang=' + encodeURIComponent(toServerLocale(locale)), function (data) {
            $scope.preferredLanguage = locale;
            isLocaleChange = true;
            resourceBundle = data;
            translateProvider.translations(locale, resourceBundle);
            translateProvider.preferredLanguage(locale);
            $translate.use(locale);
            $scope.loadingText = $translate.instant('loading');
            if (changeDir) {
                if (locale === 'ar') {
                    ComponentUtils.changeViewToRtl();
                } else {
                    ComponentUtils.changeViewToLtr();
                }
            }
            $scope.current = Current;
            $scope.docsUrl = docsUrl;
            $scope.auth = Auth;

            window.document.cookie = "KEYCLOAK_LOCALE=" + locale + "; path=/"

            if (($scope.path[0] === 'realms' && $scope.path[1] !== undefined) || $scope.path[0] !== 'realms') {
                $route.reload();
            }
        });
    }

    $scope.getMessageJson(false);

    $scope.$watch('preferredLanguage', function(oldValue, newValue){
        if(oldValue !== undefined && newValue !== undefined && (oldValue !== newValue)){
            locale=$scope.preferredLanguage;
            if($scope.user?.id && $scope.current.realm?.realm ){
                $scope.user.attributes.preferred_locale = locale;
                User.update({
                    realm: $scope.current.realm?.realm,
                    userId: $scope.user.id
                }, $scope.user, function (data) {
                }, function (error) {
                    Notifications.error($translate.instant(error.data.errorMessage));
                });
            }
            $('#language-switching-loader').css("display", "flex");
            $scope.current = '';
            $scope.docsUrl = '';
            $scope.auth = '';
            $scope.getMessageJson(true);
        }
    },true);

    $scope.updateUserImgFormat = function () {
        if ($scope.auth.user?.profPic) {
            $scope.auth.user.profPic = getUpdatedImgData($scope.auth.user.profPic);
        }
    };
    $scope.navToRealmList = function (rootAccountName) {
        if ($scope.showSwitch == 'true') {
            if (($scope.access && $scope.access.createRealm) || $scope.current?.realm?.realm == rootAccountName) {
                $location.url("/realms");
            }
        }
    };

    $scope.initProfileElement = () => {
        $scope.profileDropdown = document.querySelector("#profile-dropdown");
    }


    $scope.updateUserImgFormat();
    $scope.$watch('current.realm', function () {
        if(!$scope.isRealmAvailable && $scope.current?.realm?.realm){
            User.get({
                realm: $scope.current.realm?.realm,
                userId: $scope.auth.user.userId
            }, function(data){
                $scope.user = data;
                if($scope.user.attributes.preferred_locale[0] && $scope.preferredLanguage !== $scope.user.attributes.preferred_locale[0]){
                    $scope.preferredLanguage = $scope.user.attributes.preferred_locale[0] ;
                }
            }, function(err){
                console.log("Can't able to get user details")
            })
            $scope.isRealmAvailable = true;
        }
        if ($scope.current.realm?.realmLogo) {
            $scope.current.realm.realmLogo = getUpdatedImgData($scope.current.realm.realmLogo);
        }
        $scope.updateUserImgFormat();
    });

    $scope.$watch('auth', function () {
        $scope.updateUserImgFormat();
    });

    $scope.toggleDropdown = function () {
        $scope.profileDropdown.classList.toggle("active");
    };

    document.addEventListener('click', (event) => {
        const isClickInsideProfile = $scope.profileDropdown?.contains(event.target);
        if ($scope.profileDropdown && !isClickInsideProfile) {
            $scope.profileDropdown.classList.remove("active");
        }
    });

    $scope.createNewRealm = function () {
        ServerInfo.get(Current.realm, function (res) {
            let showMode = false;
            if (res?.data?.brandInfo?.modeEnabled) {
                showMode = true;
            }
            RealmTenantCreateDialog.open($translate.instant('create-tenant'), showMode, res?.data?.systemInfo.mode, '', function (realm, modalScope) {
                var realmCopy = angular.copy(realm);
                Realm.create(realmCopy, function () {
                    Notifications.success($translate.instant("workspace-created-successfully"));
                    Auth.refreshPermissions(function () {
                        $scope.$apply(function () {
                            $location.url("/realms/" + realmCopy.realm);
                        });
                    });
                }, function () {
                    modalScope.stage = 1;
                });
            });
        });

    };
});


module.controller('RealmCreateCtrl', function ($scope, Current, Realm, $upload, $http, $location, $route, Dialog, Notifications, Auth, $modal, $translate) {
    Current.realm = null;

    $scope.realm = {
        enabled: true
    };

    $scope.changed = false;
    $scope.files = [];

    var oldCopy = angular.copy($scope.realm);

    $scope.importFile = function ($fileContent) {
        $scope.realm = angular.copy(JSON.parse($fileContent));
        $scope.importing = true;
    };

    $scope.viewImportDetails = function () {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-object.html',
            controller: 'ObjectModalCtrl',
            backdrop: 'static',
            resolve: {
                object: function () {
                    return $scope.realm;
                }
            }
        });
    };

    $scope.$watch('realm', function () {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.$watch('realm.realm', function () {
        $scope.realm.id = $scope.realm.realm;
    }, true);

    var get_realm_name_elem = angular.element(document.getElementById("name"));
    var error_msg_elem = angular.element(document.getElementById("error_msg"));

    $(get_realm_name_elem).on("click", function () {
        $("#error_msg").text("");
    });

    function processLogo(uploader) {
        if (uploader.files && uploader.files[0]) {
            $('#profileImage').attr('src',
                window.URL.createObjectURL(uploader.files[0]));
        }
        getBase64(uploader.files[0]).then(
            data => $scope.realm.realmLogo = data.split("base64,")[1]
        );
    }

    $("#logoUpload").change(function () {
        processLogo(this);
    });

    $scope.save = function () {
        var realmName = $scope.realm.realm;
        let validationResult = validateTenantName(realmName);
        if (!validationResult.valid) {
            error_msg_elem.text(validationResult.err);
        } else {
            var realmCopy = angular.copy($scope.realm);
            Realm.create(realmCopy, function () {
                Notifications.success($translate.instant("workspace-created-successfully"));

                Auth.refreshPermissions(function () {
                    $scope.$apply(function () {
                        $location.url("/realms/" + realmCopy.realm);
                    });
                });
            });
        }

    };

    $scope.cancel = function () {
        $location.url("/");
    };

    $scope.reset = function () {
        $route.reload();
    };
});



function getBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
    });
}

function isBase64(str) {
    const base64Regex = /^(?:[A-Za-z0-9+\/]{4})*(?:[A-Za-z0-9+\/]{2}==|[A-Za-z0-9+\/]{3}=)?$/;
    if (!base64Regex.test(str)) {
        return false;
    }
    try {
        atob(str);
        return true;
    } catch (err) {
        return false;
    }
}

function isValidURL(data) {
    try {
        new URL(data);
        return true;
    } catch (_) {
        return false;
    }
}

module.controller('ObjectModalCtrl', function ($scope, object) {
    $scope.object = object;
});

module.controller('fileChangeCtrl', function ($scope) {
    $scope.$parent.changed = false;
    $scope.fileNameChanged = function () {
        $scope.$parent.changed = $scope.$parent.create;
        $scope.$parent.changed = true;
    };
});
module.controller('RealmSubscriptionCtrl', function ($scope, $modal, $route, realm, LicenseCreator, LicenseExtender, serverInfo, $http, ClientTryUrl, $location, $window, Dialog, RealmClearUserCache, RealmClearRealmCache, RealmClearKeysCache, Notifications, Auth, $translate, LicenseChecker, $translate, UpdateRealmPricingType) {
    let ctr = 0;
    $scope.init = function () {
        if (ctr === 0) {
            ctr = 1;
            $scope.stage = 1;
            $scope.loaderTypeName = 'subscriptionLoader';
            $scope.dataLoaded = false;
            $scope.serverInfo = serverInfo;
            $scope.realmName = realm.realm;
            $scope.realm = angular.copy(realm);
            $scope.licenseConfig = [];
            $scope.userCanUpdate = false;
            $scope.isAddMoneyLoading = false;

            if ($scope.serverInfo.systemInfo.offering == 'CLOUD') {
                $http.get(authUrl + '/admin/realms/' + realm.realm + "/users/" + Auth.user.userId).then(function (res) {
                    let data = res.data;
                    $scope.userCanUpdate = data?.attributes?.qv_is_account_creator[0] === "true";
                });
            }

            $scope.updateSubscription = function () {
                ClientTryUrl.get({ realm: $scope.realm.realm, client: "profile", redirectUrl: authUrl + "/realms/" + realm.realm + "/account/subscription/" }, function (data) {
                    redirectInsideAsync(data.oidcurl);
                }, function () {
                    Notifications.error($translate.instant('application-tryurl-fetch-failed'));
                });
            };

            LicenseChecker.get({ realm: realm.realm }, function (data) {
                $scope.licenseInfo = data;
                $scope.updateLicenseInfo();
                $scope.dataLoaded = true;
            }, function () {
                $scope.dataLoaded = true;
            });
        }
    };

    $scope.makeSideMenuClosed();

    $scope.showCreditPricingMetadata = function (credit, currency) {
        var controller = function ($scope, $modalInstance, $translate) {
            $scope.credit = credit;
            $scope.currency = currency;
            $scope.pricing = [];
            $scope.config = [];
            $scope.dashboardLocale = getCookie('KEYCLOAK_LOCALE');
            $scope.propertyName = 'desc_' + $scope.dashboardLocale;
            $scope.description = $scope.credit[$scope.propertyName];

            if ($scope.credit.pricing) {
                Object.keys($scope.credit.pricing.metadata).forEach((key) => {
                    const value = $scope.credit.pricing.metadata[key];
                    if (isNaN(value) || key === 'volume') {
                        $scope.config.push({ key, value });
                    } else {
                        $scope.pricing.push({ key, value });
                    }
                });
            }

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-credit-pricing-metadata.html',
            controller,
            backdrop: 'static',
        });
    };

    $scope.showContactSupport = function (title) {
        var controller = function ($scope, $modalInstance, $translate, $sce) {
            $scope.title = $sce.trustAsHtml($translate.instant(title));
            $scope.body = $sce.trustAsHtml($translate.instant('contact-support-info', { link: $translate.instant('support-link'), title: $translate.instant(title).toLowerCase() }));

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
                templateUrl: resourceUrl + '/partials/modal/generic-info.html',
                controller,
                backdrop: 'static',
            });
    };

    $scope.showAddMoneyModal = function () {
        var controller = function ($scope, $modalInstance, $translate, $http, Notifications) {
            $scope.title = $translate.instant('add-money');

            $scope.addMoney = () => {
                let amount = $('#subscription-amount').val();
                let cleanedValue = amount.replace(/[^0-9.]/g, '');
                if(amount !== cleanedValue){
                    Notifications.error($translate.instant('add-money-invalid-input-formate-error'));
                    $('#subscription-amount').val(cleanedValue);
                    return;
                }
                if(cleanedValue.length){
                    $http.post(authUrl + '/admin/realms/' + realm.realm + '/license/payment-link/' + cleanedValue )
                    .then((response) => {
                        $scope.isAddMoneyLoading = false;
                        $scope.cancel();
                        window.open(response.data.body.url, "_blank");
                    })
                    .catch((error) => {
                        $scope.isAddMoneyLoading = false;
                        Notifications.error($translate.instant('wallet-add-error'));
                    });
                }
            }

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $(document).ready(function () {
                setTimeout(function () {
                    $("#subscription-amoun").focus();
                }, 100);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/add-money-modal.html',
            controller: controller,
            backdrop: 'static'
        });
    };

    $scope.showChangeToLiveMode = function(title) {
    var controller = function ($scope, $modalInstance, $translate, $sce) {
                $scope.title = $sce.trustAsHtml($translate.instant(title));
                $scope.body = $sce.trustAsHtml($translate.instant('sandbox-contact-support', { link: $translate.instant('support-link'), title: $translate.instant("change-to-live-mode").toLowerCase() }));

                $scope.cancel = function () {
                    $modalInstance.dismiss('cancel');
                };
                $scope.$on('$routeChangeStart', function () {
                    $modalInstance.close();
                });
            };
            $modal.open({
                    templateUrl: resourceUrl + '/partials/modal/generic-info.html',
                    controller,
                    backdrop: 'static',
                });
    };

    $scope.handleUpdatePlan = function(event) {
        if (realm.sandboxMode) {
            event.preventDefault();
            $scope.showChangeToLiveMode('update-plan');
        } else {
        }
    };

    function getCookie(name) {
          const cookies = document.cookie.split(';');
          for (let i = 0; i < cookies.length; i++) {
            const ck = cookies[i].trim();
            if (ck.startsWith(name + "=")) {
              return ck.substring(name.length + 1);
            }
          }
          return null;
        }

    $scope.isMobileWView = false;
    $scope.toggleSideBarShow = function () {
       var sidebarMenu = document.getElementById('setting-sidebar');
       var sidebarIcon = document.getElementById('setting-sidebar-icon');
       if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
           sidebarMenu.classList.remove("bo-setting-sidemenu");
           sidebarMenu.classList.add("bo-setting-sidemenu-show");
           sidebarIcon.classList.add('d-none');
           sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
           $scope.isMobileWView = true;
       }else{
           sidebarMenu.classList.add("bo-setting-sidemenu");
           sidebarMenu.classList.remove("bo-setting-sidemenu-show");
           sidebarIcon.classList.remove('d-none');
           sidebarIcon.classList.add('bo-setting-sidemenu-icon');
           $scope.isMobileWView = false;
       }
   }

    function drawPercentageCircle(cardData) {
        let progressBar = document.querySelector(`.circular-progress-${cardData.prodId}`);
        let valueContainer = document.querySelector(`.value-container-${cardData.prodId}`);
        let progressValue = 0;
        let progressEndValue = (cardData.consumedCount / cardData.maxProdCount) * 100;
        let speed = 25;
        let progress = setInterval(() => {
            let filledColor = "#4ECC48";
            let unFilledColor = "#B2E4B5";
            valueContainer.textContent = `${progressValue}%`;
            if (progressValue >= 75) {
                filledColor = "#EA4F14";
                unFilledColor = "#EEA5A8";
            }
            progressBar.style.background = `conic-gradient(
            ${filledColor} ${progressValue * 3.6}deg,
          ${unFilledColor} ${progressValue * 3.6}deg
      )`;
            if (progressValue >= progressEndValue || progressValue >= 100) {
                clearInterval(progress);
            }
            if (progressValue < 100) {
                progressValue++;
            }
        }, speed);
    };

    $scope.addMoney = function () {
        $scope.isAddMoneyLoading = true;
        $http.get(`${authUrl}/admin/realms/${$scope.realm.realm}/wallet/add`)
            .then((response) => {
                $scope.isAddMoneyLoading = false;
                window.open(response.data.body.url, "_self");
            })
            .catch((error) => {
                $scope.isAddMoneyLoading = false;
                Notifications.error($translate.instant('wallet-add-error'));
            });
    };

    $scope.updateLicenseInfo = function () {
        $scope.invoice = angular.copy($scope.licenseInfo.body.invoice);
        if ($scope.licenseInfo.status === 200 && $scope.licenseInfo.body.days !== -1) {
            $scope.licenseMessage = $translate.instant('trial-ends-at') + $scope.getLicenseExpireDate($scope.licenseInfo.body.days) + ' ( ' + $scope.licenseInfo.body.days + ' ' + $translate.instant('days-to-go') + ' )';
            $scope.licenseConfig = $scope.licenseInfo.body;
            $scope.licenseEndsAt = $scope.getLicenseExpireDate($scope.licenseInfo.body.days);
        }
        if ($scope.licenseInfo.status === 200 && $scope.licenseInfo.body.days === -1) {
            $scope.licenseMessage = $scope.licenseInfo.body.message;
            if( $scope.licenseInfo.body.stage) {
                $scope.licenseMessage = $translate.instant('license-message', { stage : $scope.licenseInfo.body.stage });
            }
            if ($scope.licenseInfo.body) {
                $scope.licenseConfig = $scope.licenseInfo.body;
            }
            $scope.showAction = false;
        }
        if ($scope.licenseInfo.status === 205) {
            $scope.licenseMessage = $scope.licenseInfo.body.message;
            if( $scope.licenseInfo.body.stage) {
               $scope.licenseMessage = $translate.instant('license-message', { stage : $scope.licenseInfo.body.stage });
            }
            $scope.licenseConfig = $scope.licenseInfo.body;
            $scope.licenseEndsAt = $scope.getLicenseExpireDate($scope.licenseInfo.body.days);
        }
        if ($scope.licenseInfo && $scope.licenseInfo.status === 206) {
            $scope.licenseMessage = $scope.licenseInfo.body.message;
            if( $scope.licenseInfo.body.stage) {
               $scope.licenseMessage = $translate.instant('license-message', { stage : $scope.licenseInfo.body.stage });
            }
        }
        if ($scope.licenseInfo && $scope.licenseInfo.status == 404) {
            $scope.licenseMessage = $translate.instant('unlicensed-account');
            $scope.showAction = true;
            $scope.licenseBtnTxt = $translate.instant('register');
            $scope.licenseActionType = "register";
        }
        else if ($scope.licenseInfo && $scope.licenseInfo.status == 403) {
            $scope.showAction = true;
            $scope.licenseMessage = $scope.licenseInfo.body.message;
            $scope.licenseBtnTxt = $translate.instant('extend-license');
            $scope.licenseActionType = "extend";
        }
        else if ($scope.licenseInfo && $scope.licenseInfo.status == 200 && $scope.licenseInfo.body.days !== -1) {
            if (!$scope.licenseInfo.body.activated) {
                $scope.showAction = true;
                $scope.licenseBtnTxt = $translate.instant('activate-license');
                $scope.licenseActionType = "activate";
            }
            else {

                $scope.showAction = $scope.licenseInfo.body.days <= 5;
                $scope.licenseBtnTxt = $translate.instant('extend-license');
                $scope.licenseActionType = "extend";
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

    $scope.applyStyles = function (used, total) {
        return used * (100 / total);
    };
    $scope.licenseAction = function () {
        $scope.stage = 2;
        if ($scope.licenseActionType == "register") {
            LicenseCreator.save({ realm: $scope.realmName }, function (res) {
                if (res.status == 200) {
                    Notifications.success($translate.instant(res.body.message));
                    $route.reload();
                }
                else {
                    Notifications.error(res.body.message);
                }
                $scope.stage = 1;
            });
        }
        else if ($scope.licenseActionType == "extend") {
            LicenseExtender.save({ realm: $scope.realmName }, function (res) {
                if (res.status == 200) {
                    Notifications.success($translate.instant(res.body.message));
                    $route.reload();
                }
                else {
                    Notifications.error(res.body.message);
                }
                $scope.stage = 1;
            });
        }
        // else if ($scope.licenseActionType == "activate") {
        //     $scope.stage = 1;
        //     var controller = function ($scope, $modalInstance, $location) {
        //         $scope.license = {};
        //         $scope.$watch('license', function () {
        //             $scope.changed = $scope.license.key && $scope.license.key.length > 0;
        //         }, true);
        //         $scope.activateLicense = function () {
        //             $modalInstance.close();
        //             LicenseValidator.save({ realm: realm.realm }, $scope.license.key, function (res) {
        //                 if (res.status == 200) {
        //                     Notifications.success($translate.instant("license-activated"));
        //                     $route.reload();
        //                 }
        //                 else {
        //                     Notifications.error(res.body.message);
        //                 }
        //             });
        //         };
        //         $scope.cancel = function () {
        //             $modalInstance.dismiss('cancel');
        //         };
        //         $scope.$on('$routeChangeStart', function () {
        //             $modalInstance.close();
        //         });
        //         $(document).ready(function () {
        //             setTimeout(function () {
        //                 $("#name").focus();
        //             }, 100);
        //         });
        //     };
        //     $modal.open({
        //         templateUrl: resourceUrl + '/partials/modal/activate-license.html',
        //         controller: controller,
        //         backdrop: 'static',
        //         resolve: {
        //         }
        //     });
        // }
    };
});



module.controller('RealmUpdatePlanCtrl', function ($scope, $route, $translate, realm, $http, LicenseChecker, $modal, Notifications) {
    $scope.realm = realm;
    $scope.isLoading = false;
    $scope.loaderTypeName = 'subscriptionLoader';
    $scope.subscribedAuthentication = realm.attributes.subscriptionNeedsAuthenticationProduct ? JSON.parse(realm.attributes.subscriptionNeedsAuthenticationProduct) : false;
    $scope.isUpdateEnabled = false;
    $scope.dashboardLocale = getCookie('KEYCLOAK_LOCALE');
    $scope.propertyName = 'desc_' + $scope.dashboardLocale;

    $scope.makeSideMenuClosed();

    $scope.transactionId = "bo_tran";

    $scope.oneTimeSubscriptions = {};
    $scope.subscriptions = {
        creditSubscriptions: {},
        recurringSubscriptions: {}
    };
    $scope.allAvailableProduct = {};
    $scope.isRedirected = $route.current.params.isRedirected;

    $scope.isUpdateOnRedirectLoading = false;

    function getCookie(name) {
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
          const ck = cookies[i].trim();
          if (ck.startsWith(name + "=")) {
            return ck.substring(name.length + 1);
          }
        }
        return null;
      }

    $scope.init = function () {
        $scope.isLoading = true;
        LicenseChecker.get({ realm: realm.realm }, function (data) {
            $scope.licenseInfo = data.body;
            data.body.credits.forEach((credit) => {
                //Group credits based on pricingType
                if (credit.pricingType === 'one_time') {
                    $scope.oneTimeSubscriptions[credit.externalId] = { ...credit, savedPricingId: credit.pricing.priceId, currentPricingId: credit.pricing.priceId };
                } else if (credit.pricingType === 'recurring') {
                    $scope.subscriptions.recurringSubscriptions[credit.externalId] = { ...credit, isEnabled: true, savedIsEnabled: true };
                } else {
                    $scope.subscriptions.creditSubscriptions[credit.externalId] = { ...credit, isDefaultProvider: credit.pricingType !== 'free', savedIsDefaultProvider: credit.pricingType !== 'free' };
                }
            });
            //Update subscription if redirected from stripe after transaction payment
            if ($scope.isRedirected) {
                const payload = parseSubscriptionsParam();
                //Append transaction info - not included in redirect query since transaction payment confirmation is required by calling /check API.
                payload[$scope.transactionId] = {
                    productId: $scope.oneTimeSubscriptions[$scope.transactionId].prodId,
                    priceId: $scope.oneTimeSubscriptions[$scope.transactionId].currentPricingId
                };
                $scope.isLoading = false;
                return updateSubscriptions(payload);
            }
            loadProducts();
        }, function () {
            $scope.isLoading = false;
            Notifications.error($translate.instant("check-license-error"));
        });
    };

    function loadProducts() {
        $http.get(`${authUrl}/admin/realms/${realm.realm}/products`).then((response) => {
            const allCredits = response.data.body.products;
            allCredits.forEach((credit) => {
                $scope.allAvailableProduct[credit.externalId] = { ...credit };
                if (credit.pricingType === 'one_time' && $scope.oneTimeSubscriptions[credit.externalId]) {
                    $scope.oneTimeSubscriptions[credit.externalId] = { ...$scope.oneTimeSubscriptions[credit.externalId], pricing: credit.pricing };
                }
                //credit/free is skipped since it will take the default settings of master tenant.
                if (credit.pricingType === 'recurring' && !$scope.subscriptions.recurringSubscriptions[credit.externalId] && !$scope.subscriptions.recurringSubscriptions[credit.alternative]) {
                    $scope.subscriptions.recurringSubscriptions[credit.externalId] = { ...credit, isEnabled: false, savedIsEnabled: false };
                }
                if (credit.pricingType === 'recurring' && ($scope.subscriptions.recurringSubscriptions[credit.externalId] || $scope.subscriptions.recurringSubscriptions[credit.alternative])) {
                    $scope.subscriptions.recurringSubscriptions[credit.externalId] = {
                        ...$scope.subscriptions.recurringSubscriptions[credit.externalId],
                        pricing: credit.pricing
                    };
                }
            });
            $scope.isLoading = false;
        }).catch(() => {
            $scope.isLoading = false;
            Notifications.error($translate.instant("get-all-subscriptions-error"));
        });
    }

    $scope.$watch('subscriptions', function () {
        $scope.isUpdateEnabled = false;
        $scope.isSubscriptionsUpdateRequired = false;
        Object.values($scope.subscriptions['creditSubscriptions']).forEach((subscription) => {
            if (subscription.isDefaultProvider !== subscription.savedIsDefaultProvider) {
                $scope.isUpdateEnabled = true;
                $scope.isSubscriptionsUpdateRequired = true;
                return;
            }
        });
        if (!$scope.isUpdateEnabled) {
            Object.values($scope.subscriptions['recurringSubscriptions']).forEach((subscription) => {
                if (subscription.isEnabled !== subscription.savedIsEnabled) {
                    $scope.isUpdateEnabled = true;
                    $scope.isSubscriptionsUpdateRequired = true;
                    return;
                }
            });
        }
    }, true);

    $scope.$watch('oneTimeSubscriptions', function () {
        $scope.isUpdateEnabled = false;
        $scope.isTransactionUpdateRequired = false;
        Object.values($scope.oneTimeSubscriptions).forEach((subscription) => {
            if (subscription.currentPricingId !== subscription.savedPricingId) {
                $scope.isUpdateEnabled = true;
                $scope.isTransactionUpdateRequired = true;
                return;
            }
        });
    }, true);

    function getMaxPricing(pricingList, walletCurrency) {
        let maxPricing = { amount: -1 };
        pricingList?.forEach((pricing) => {
            if (pricing.currencyOptions[0].currency.toLowerCase() === walletCurrency.toLowerCase() && pricing.currencyOptions[0].amount > maxPricing.amount) {
                maxPricing = {
                    pricingId: pricing.priceId,
                    amount: pricing.currencyOptions[0].amount
                };
            }
        });
        return maxPricing;
    }

    function generateUpdateSubscriptionPayload() {
        const payload = {};
        Object.keys($scope.subscriptions).forEach((subscriptionType) => {
            Object.values($scope.subscriptions[subscriptionType]).forEach((subscription) => {
                if (subscriptionType === 'creditSubscriptions' && subscription.isDefaultProvider) {
                    if (subscription.pricingType === 'credit') {
                        payload[subscription.externalId] = {
                            productId: subscription.prodId,
                            priceId: subscription.pricing ? subscription.pricing.priceId : ""
                        };
                    } else {
                        payload[subscription.alternative] = {
                            productId: $scope.allAvailableProduct[subscription.alternative].productId,
                            priceId: getMaxPricing($scope.allAvailableProduct[subscription.alternative].pricing, $scope.licenseInfo.walletCurrency).pricingId ?? ""
                        };
                    }
                } else if (subscriptionType === 'creditSubscriptions' && !subscription.isDefaultProvider) {
                    if (subscription.pricingType === 'credit') {
                        payload[subscription.alternative] = {
                            productId: $scope.allAvailableProduct[subscription.alternative].productId,
                            priceId: getMaxPricing($scope.allAvailableProduct[subscription.alternative].pricing, $scope.licenseInfo.walletCurrency).pricingId ?? ""
                        };
                    } else {
                        payload[subscription.externalId] = {
                            productId: subscription.prodId,
                            priceId: subscription.pricing ? subscription.pricing.priceId : ""
                        };
                    }
                } else if (subscriptionType === 'recurringSubscriptions' && subscription.isEnabled) {
                    payload[subscription.externalId] = {
                        productId: subscription.productId ? subscription.productId : subscription.prodId,
                        priceId: getMaxPricing(subscription.pricing, $scope.licenseInfo.walletCurrency).pricingId
                    };
                }
            });
        });
        if (!$scope.isTransactionUpdateRequired) {
            Object.values($scope.oneTimeSubscriptions).forEach((subscription) => {
                payload[subscription.externalId] = {
                    productId: subscription.prodId,
                    priceId: subscription.currentPricingId
                };
            });
        }
        return payload;
    }

    function flattenPayload() {
        const flattenedPayload = {};
        const payload = generateUpdateSubscriptionPayload();
        Object.keys(payload).forEach((externalId) => {
            Object.keys(payload[externalId]).forEach((key) => {
                flattenedPayload[`${externalId}-${key}`] = payload[externalId][key];
            });
        });
        return flattenedPayload;
    }

    function parseSubscriptionsParam() {
        const subscriptions = {};
        const params = $route.current.params;
        Object.keys(params).forEach((key) => {
            if (key !== 'isRedirected' && key !== 'realm') {
                const keySplit = key.split("-");
                if (keySplit) {
                    subscriptions[keySplit[0]] = { ...subscriptions[keySplit[0]], [keySplit[1]]: params[key] ?? '' };
                }
            }
        });
        return subscriptions;
    }

    function generateRedirectUi() {
        if ($scope.isSubscriptionsUpdateRequired) {
            return `${window.location.href}?isRedirected=true&${new URLSearchParams(flattenPayload(generateUpdateSubscriptionPayload())).toString()}`;
        }
        return window.location.href;
    }

    function updateTransactionStage() {
        $http.get(`${authUrl}/admin/realms/${realm.realm}/products/${$scope.oneTimeSubscriptions[$scope.transactionId].currentPricingId}/update?redirectUrl=${encodeURIComponent(generateRedirectUi())}`)
            .then((response) => {
                if(response.data.hasOwnProperty('url') && response.data.url){
                    window.open(response.data.url, "_blank");
                }
                $scope.isUpdateLoading = false;
                $scope.isUpdateEnabled = false;
                $scope.isTransactionUpdateRequired = false
                $route.reload();
                Notifications.success($translate.instant("update-license-success"));
            })
            .catch((error) => {
                Notifications.error($translate.instant("update-transaction-error"));
                $scope.isUpdateLoading = false;
            });
    }

    function updateSubscriptions(payload) {
        if ($scope.isRedirected) {
            $scope.isUpdateOnRedirectLoading = true;
        }
        $http.put(`${authUrl}/admin/realms/${realm.realm}/subscription/update-subscription`, payload).then((response) => {
            $scope.isUpdateLoading = false;
            $scope.isUpdateEnabled = false;
            if ($scope.isRedirected) {
                return window.location.href = window.location.href.split("?")[0];
            }
            Notifications.success($translate.instant("update-license-success"));
            $route.reload();
        }).catch(() => {
            $scope.isUpdateLoading = false;
            $scope.isUpdateEnabled = false;
            Notifications.error($translate.instant("update-license-error"));
            if ($scope.isRedirected) {
                window.location.href = window.location.href.split("?")[0];
            }
        });
    }

    $scope.update = function () {
        $scope.isUpdateLoading = true;
        if ($scope.isTransactionUpdateRequired) {
            return updateTransactionStage();
        }
        updateSubscriptions(generateUpdateSubscriptionPayload());
    };

    $scope.filterStagesByCurrency = function (stage) {
        return stage.currencyOptions[0].currency === $scope.licenseInfo.walletCurrency.toLowerCase();
    };

    $scope.orderStageByAmount = function (stage) {
        return stage.currencyOptions[0].amount;
    };

    $scope.toggleCreditProvider = function (product, type) {
        $scope.subscriptions.creditSubscriptions[product.externalId].isDefaultProvider = type !== 'own';
    };

    $scope.showPricingMetadata = function (id, pricingId) {
        const type = $scope.oneTimeSubscriptions[id].name;
        const pricing = $scope.oneTimeSubscriptions[id].pricing.filter((pricingItem) => pricingItem.priceId === pricingId)[0];

        var controller = function ($scope, $modalInstance) {
            $scope.type = type;
            $scope.pricing = pricing;
            $scope.pricingMetadata = [];
            $scope.planStage = pricing.metadata['stage'];

            Object.keys(pricing.metadata).forEach((key) => {
                const value = pricing.metadata[key];
                if (!isNaN(value) && key !== 'volume') {
                    $scope.pricingMetadata.push({ key, value });
                }
            });

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-pricing-metadata.html',
            controller,
            backdrop: 'static',
        });
    };
});

module.controller('RealmActivityCtrl', function ($scope, $rootScope, $route, Current, Realm, Activity, realm, activity, serverInfo, $translate) {
    $scope.serverInfo = serverInfo;
    $scope.activityData = activity;
    $scope.isRootTenant = $scope.serverInfo.rootAccountName === realm.realm;
    $scope.isRootAccount = $scope.serverInfo.rootAccountName === realm.realm && ($scope.activityData).hasOwnProperty('tenantsCount');
    $scope.realm = angular.copy(realm);
    $scope.filter = {
        selectedFilter: ""
    };
    $scope.filtersForRootTenant = ["all-workspace-txt", "this-workspace-txt"];
    $scope.filter.selectedFilter = $scope.filtersForRootTenant[0];
    $scope.rootTenantFilter = {
        options: [
            { name: "7-days", value: '7' },
            { name: "14-days", value: '14' },
            { name: "30-days", value: '30' }
        ],
        selected: "7"
    };
    $scope.currentTenantFilter = {
        options: [
            { name: "7-days", value: '7' },
            { name: "14-days", value: '14' },
            { name: "30-days", value: '30' }
        ],
        selected: "7"
    };
    $scope.dataLoading = false;
    $scope.loaderTypeName = 'activityLoader';

    const splitGraphData = function (dates, currentOption) {
        let resultObj = {};
        let stepCount = Math.floor(currentOption / 7);
        let options = { month: 'short', day: 'numeric' };
        let startCtr = 30 - currentOption;
        for (let str = startCtr; str < 30; str = str + stepCount) {
            let singleData = {
                startDate: new Date(Number(Object.keys(dates)[str]) * 1000).toLocaleDateString("en-US", options),
                endDate: new Date(Number(Object.keys(dates)[str + stepCount - 1]) * 1000).toLocaleDateString("en-US", options),
                count: 0
            };
            for (let curr = str; curr < str + stepCount; curr++) {
                singleData.count = Number(singleData.count) + Number(Object.values(dates)[curr]);
            }
            let propName = singleData.startDate === singleData.endDate ? singleData.startDate : `${singleData.startDate} - ${singleData.endDate}`;
            resultObj[propName] = singleData.count;
        }
        return resultObj;
    };

    $scope.updateFilter = function () {
        let toPassParams = { realm: realm.realm };
        $scope.dataLoading = true;
        if ($scope.filter.selectedFilter.includes("this-workspace-txt")) {
            toPassParams.getSpecificTenant = true;
        }
        Activity.get(toPassParams, function (data) {
            $scope.activityData = data;
            $scope.isRootAccount = $scope.serverInfo.rootAccountName === realm.realm && ($scope.activityData).hasOwnProperty('tenantsCount');
            $scope.updateActivity();
            $scope.dataLoading = false;
        }, function (err) {
            $scope.dataLoading = false;
            console.log(err);
        });
    };

    $scope.updateActivity = function () {
        $scope.activityHistory = $scope.activityData.history[0];
        let labelData = [];
        let dataPoints = [];
        let finalDays = 7;
        if ($scope.isRootAccount && $scope.filter.selectedFilter.trim() === "All Workspaces") {
            finalDays = $scope.rootTenantFilter.selected;
        } else {
            finalDays = $scope.currentTenantFilter.selected;
        }
        if ($scope.activityHistory) {
            const sorted = Object.keys($scope.activityHistory).slice(-Number(finalDays)).slice(-Number(finalDays)).sort()
                .reduce((accumulator, key) => {
                    accumulator[key] = $scope.activityHistory[key];
                    return accumulator;
                }, {});
            labelData = Object.keys(sorted);
            dataPoints = Object.values(sorted);
        }
        $scope.uniqueVisitors = dataPoints.reduce((a, b) => parseInt(a) + parseInt(b), 0);
        $scope.uniqueVisitorsText = $scope.isRootAccount ? $translate.instant('workspace-created-txt') : $translate.instant('unique-logins-txt');
        let options = { month: 'short', day: 'numeric', year: 'numeric' };
        let finalLabelData = labelData.map(data => new Date(Number(data) * 1000).toLocaleDateString("en-US", options));

        if ($("#lineChart").length) {

            $("#lineChart").remove();
            var canvas = document.createElement('canvas');
            canvas.id = "lineChart";
            canvas.width = 600;
            canvas.height = 170;
            $('#chart-container').append(canvas);
            var lineChart = $("#lineChart");
            let cssStyles = getComputedStyle(document.body);
            // line chart data
            var lineData = {
                labels: finalLabelData,
                datasets: [{
                    label: $scope.isRootAccount ? $translate.instant('workspace-created-txt') : $translate.instant('unique-logins-txt'),
                    fill: false,
                    lineTension: 0.3,
                    backgroundColor: "#fff",
                    borderColor: cssStyles.getPropertyValue('--primary'),
                    borderCapStyle: 'butt',
                    borderDash: [],
                    borderDashOffset: 0.0,
                    borderJoinStyle: 'miter',
                    pointBorderColor: cssStyles.getPropertyValue('--primary'),
                    pointBackgroundColor: cssStyles.getPropertyValue('--primary'),
                    pointBorderWidth: 0,
                    pointHoverRadius: 5,
                    pointHoverBackgroundColor: cssStyles.getPropertyValue('--btnPrimaryHover'),
                    pointHoverBorderColor: cssStyles.getPropertyValue('--btnPrimaryHover'),
                    pointHoverBorderWidth: 3,
                    pointRadius: 2,
                    pointHitRadius: 10,
                    data: dataPoints,
                    spanGaps: false
                }]
            };
            // line chart init
            var myLineChart = new Chart(lineChart, {
                type: 'line',
                data: lineData,
                options: {
                    plugins: {
                        legend: {
                            display: false,
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true
                        },
                        x: {
                            display: true,
                            ticks: {
                                callback: function (dataLabel, index) {
                                    let stepCount = Math.floor(finalDays / 7);
                                    let finalLabel = '';
                                    let options = { month: 'short', day: 'numeric' };
                                    if (index === 0 || index % stepCount === 0) {
                                        finalLabel = new Date(Number(labelData[dataLabel]) * 1000).toLocaleDateString("en-US", options);;
                                    }
                                    return finalLabel;
                                }
                            }
                        }
                    }
                }
            });
        }
    };
    $scope.addData = function (chart, label, data) {
        chart.data.labels.push(label);
        chart.data.datasets.forEach((dataset) => {
            dataset.data.push(data);
        });
        chart.update();
    };
    setTimeout(function () {
        $scope.updateActivity();
    }, 100);
});

module.controller('RealmBoActivityCtrl', function ($scope, $rootScope, $route, Current, Realm, BoActivity, realm, serverInfo, $translate) {
    $scope.serverInfo = serverInfo;
    $scope.isRootTenant = $scope.serverInfo.rootAccountName === realm.realm;
    $scope.isAPIEnabled = realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = realm.attributes['isPlatformWorkspace'] === "true" ? true : false;
    $scope.realm = angular.copy(realm);
    $scope.dataLoading = true;
    $scope.loaderTypeName = 'boActivityLoader';
    $scope.filter = {
        selectedFilter: ""
    };
    $scope.filtersForRootTenant = ["all-workspace-txt", "this-workspace-txt"];
    $scope.filter.selectedFilter = $scope.filtersForRootTenant[0];
    $scope.rootTenantFilter = {
        options: [
            { name: "7-days", value: '7' },
            { name: "14-days", value: '14' },
            { name: "30-days", value: '30' }
        ],
        selected: "7"
    };
    $scope.currentTenantFilter = {
        options: [
            { name: "7-days", value: '7' },
            { name: "14-days", value: '14' },
            { name: "30-days", value: '30' }
        ],
        selected: "7"
    };

    $scope.updateFilter = function () {
        let toPassParams = { realm: realm.realm };
        $scope.dataLoading = true;
        if ($scope.filter.selectedFilter.includes("this-workspace-txt")) {
            toPassParams.getSpecificTenant = true;
        }
        BoActivity.get(toPassParams, function (data) {
            $scope.activityData = data;
            $scope.dataLoading = false;
        }, function (err) {
            $scope.dataLoading = false;
            console.log(err);
        });
    };

    $scope.activityCardList = [
        {label:'apps', icon:'activity-application', title:'total-apps', value:'', theme:'bo-activity-app-theme'},
        {label:'uniqueUsers', icon:'activity-user', title:'total-unique-users', value:'', theme:'bo-activity-user-theme'},
        {label:'transactions', icon:'activity-transaction', title:'total-transactions', value:'', theme:'bo-activity-transaction-theme'},
        {label:'completed', icon:'activity-transaction-completed', title:'total-completed', value:'', theme:'bo-activity-transaction-completed-theme'},
        {label:'rejected', icon:'activity-transaction-rejected', title:'total-rejected', value:'', theme:'bo-activity-transaction-rejected-theme'},
        {label:'pending', icon:'activity-transaction-pending', title:'total-pending', value:'', theme:'bo-activity-transaction-pending-theme'}
    ]

    $scope.formatNumber = function (number) {
        const suffixes = ["", "K+", "M+", "B+", "T+"];
        const suffixNum = Math.floor(("" + number).length / 3);

        let shortNumber = parseFloat((suffixNum !== 0 ? number / Math.pow(1000, suffixNum) : number));
        if (shortNumber % 1 !== 0) {
            // Getting 1 digit fixed number with ceiling.
            const power = Math.pow(10, 1 || 0);
            shortNumber = Math.floor(shortNumber * power) / power;
        }

        return shortNumber + suffixes[suffixNum];
    }

    $scope.$watch("activityData", function () {
        if($scope.activityData){
            Object.keys($scope.activityData).map((activity)=>{
                $scope.activityCardList.forEach((card)=>{
                    if(activity === card.label){
                        if($scope.activityData[activity] > 999){
                            card.value = $scope.formatNumber($scope.activityData[activity]);
                        }else{
                            card.value = $scope.activityData[activity];
                        }
                    }
                })
            })
        }
    },true)

    $scope.updateFilter();
});

module.controller('ApiProviders', function($scope, realm, ApiProvidersSync, $translate, Notifications, $route, GetRealmDetails){
    $scope.realm = angular.copy(realm);
    $scope.currentTab = "providers";
    $scope.showSync = false;
    $scope.currentRealm;
    $scope.loading = true;

    $scope.realmDetails = GetRealmDetails.query({
        realm: $scope.realm.realm,
        name: $route.current.params.name
    }).$promise.then(function (data) {
        $scope.currentRealm = angular.copy(data);
        $scope.showSync = $scope.serverInfo.rootAccountName === $scope.currentRealm.workspaceName ? true : false;
    });

    $scope.setSetShowSync = (state) => {
        $scope.showSync = state;
    }
    $scope.changeCurrentTab = (tab) => {
        $scope.currentTab = tab;
    }
    $scope.syncTenant = () => {
        $scope.syncLoading = true;
        ApiProvidersSync.query({realm: $scope.currentRealm.workspaceId},(response)=> {
            Notifications.success($translate.instant("synced-successfully"));
            $scope.syncLoading = false;
            $route.reload();
        }, (error) => {
            Notifications.error($translate.instant("synced-failed"));
            $scope.syncLoading = false;
        })
    }
});

module.controller('KcApiProvidersCtrl', function($scope, ApiProviders, $modal, Session){
    $scope.query = {};
    $scope.syncLoading = false;
    $scope.loaderTypeName ='roleListLoader';
    $scope.loading = true;
    $scope.isInitialRequest = false;
    $scope.providers={}
    $scope.query.max = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
    $scope.syncTenant = $scope.$parent.syncTenant;
    $scope.makeRequest = () => {
        $scope.loading = true;
        $scope.providers = ApiProviders.query({realm: $scope.realm.id, ...$scope.query}, function(data) {
            $scope.loading = false;
            if (data?.providers.length > 0) {
                $scope.$parent.setSetShowSync(true);
                $scope.isInitialRequest = true;
            }
        }, function(data) {
            $scope.loading = false;
        });
    }

    $scope.oldState = () => {
        $scope.query.first = 0;
        $scope.query.last = $scope.query.max;
    }

    $scope.initQuery = () => {
        $scope.oldState()
        $scope.query.search = "";
        $scope.makeRequest()
    }

    $scope.previous = () => {
        $scope.query.first -= $scope.query.max;
        $scope.query.last -= $scope.query.max;
        $scope.makeRequest();
    }

    $scope.next = () => {
        $scope.query.first += $scope.query.max;
        $scope.query.last += $scope.query.max;
        $scope.makeRequest();
    }

    $scope.changeCurrentTab = (tab) => {
        $scope.currentTab = tab;
    }

    $scope.changePageCount = () => {
        $scope.oldState();
        $scope.makeRequest();
    }

    $scope.makeSearch = (clear) => {
        $scope.oldState()
        if (clear) {
            $scope.initQuery();
        } else {
            $scope.makeRequest();
        }
    }

    $scope.initQuery();

    $scope.editProviders = function () {
        var controller = function ($scope, $modalInstance, providers, realm, UpdateApiProviders, $translate, Notifications) {
            $scope.providers = providers;
            $scope.isProvider = true;
            $scope.realm = realm
            $scope.save = function () {
                const removedList = $scope.providers.filter(p => !p.live).map(p => p.id);
                UpdateApiProviders.update({realm: realm.id}, removedList, function(data){
                    Notifications.success($translate.instant("providers-updated"));
                    $scope.cancel();
                    $modalInstance.close();
                }, function(data) {
                    $modalInstance.close();
                    Notifications.error($translate.instant("something-went-wrong"));
                })
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/update-api-providers.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                providers : () => {return $scope.providers.providers},
                realm: () => {return $scope.realm},
            }
        });
    };
});

module.controller('KcApiVerifiersCtrl', function($scope, $translate, Session, ApiVerifiers, $modal, UpdateApiVerifiers, Notifications, GetRealmDetails, $route){
    $scope.query = {};
    $scope.verifiers = {}
    $scope.query.sort = "ASC";
    $scope.query.enabled = "false";
    $scope.loaderTypeName ='roleListLoader';
    $scope.$parent.loading = true;
    $scope.currentRealm= {logo:""};
    $scope.query.max = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;

    $scope.makeRequest = () => {
        $scope.$parent.loading = true;
        $scope.verifiers = ApiVerifiers.query(
            { realm: $scope.currentRealm.workspaceId, ...$scope.query },
            function () {
                $scope.$parent.loading = false;
            },
            function (err) {
                $scope.$parent.loading = false;
            }
        );
    };

    $scope.getRealmDetails = () => {
        $scope.$parent.loading = true;
        GetRealmDetails.query({
            realm: $scope.realm.realm,
            name: $route.current.params.name
        }).$promise.then(function (data) {
            $scope.currentRealm = angular.copy(data);
            $scope.makeRequest();
        });
    };


    $scope.oldState = () => {
        $scope.query.first = 0;
        $scope.query.last = $scope.query.max;
    }

    $scope.initQuery = () => {
        $scope.oldState()
        $scope.query.search = "";
        $scope.getRealmDetails()
    }

    $scope.previous = () => {
        $scope.query.first -= $scope.query.max;
        $scope.query.last -= $scope.query.max;
        $scope.makeRequest();
    }

    $scope.next = () => {
        $scope.query.first += $scope.query.max;
        $scope.query.last += $scope.query.max;
        $scope.makeRequest();
    }

    $scope.changeCurrentTab = (tab) => {
        $scope.currentTab = tab;
    }

    $scope.changePageCount = () => {
        $scope.oldState();
        $scope.makeRequest();
    }

    $scope.makeSearch = (clear) => {
        $scope.oldState()
        if (clear) {
            $scope.initQuery();
        } else {
            $scope.makeRequest();
        }
    }

    $scope.editVerifiers = function (verifier, verifiers) {
        var controller = function ($scope, $modalInstance, realm,subRealm, UpdateApiVerifiers, $translate, Notifications) {
            $scope.verifier = verifier;
            $scope.verifiers = verifiers;
            $scope.realm = realm;
            $scope.selectedProvider = verifier.selectedProvider;
            $scope.isEnabled = verifier.isEnabled;

            $scope.save = function () {
                const selectedValue = document.querySelector('input[name="apiProvider"]:checked')?.value;
                const requestBody = {
                    verifierName: verifier.verifierName,
                    providerName: selectedValue
                }
                UpdateApiVerifiers.update({realm: subRealm.workspaceId}, requestBody, function(data){
                    $scope.verifiers.find(v => v.verifierName === $scope.verifier.verifierName ).selectedProvider = selectedValue;
                    Notifications.success($translate.instant("verifiers-updated"));
                    $scope.cancel();
                },function(data){
                  Notifications.error($translate.instant("verifiers-update-failed"));
                  $scope.cancel();
              }
                )
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/update-api-providers.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                realm: () => {return $scope.realm},
                subRealm: () => {return $scope.currentRealm},
            }
        });
    };

    $scope.updateVerifier = function(verifier) {
        const verifierCopy = angular.copy(verifier)
        // Optionally call backend to persist this change
        UpdateApiVerifiers.update(
            { realm: $scope.currentRealm.workspaceId },
            { verifierName: verifierCopy.verifierName, isEnabled: !verifierCopy.isEnabled },
            function() {
                Notifications.success($translate.instant("verifier-status-updated"));
            },
            function() {
                Notifications.error($translate.instant("verifier-status-update-failed"));
                verifier.isEnabled = !verifier.isEnabled; // Rollback if update fails
            }
        );
    };

    $scope.initQuery();
    $scope.openedVerifiers = {};

    Object.keys($scope.verifiers).map(v => {
        $scope.openedVerifiers[v] = false;
    })

    $scope.toggleVerifiers = (name) => {
        $scope.openedVerifiers[name] = !$scope.openedVerifiers[name];
    }
});

module.directive('draggableList', function(UpdateApiProvidersPriority,$translate, Notifications, Dialog) {
    return {
      restrict: 'A',
      link: function(scope, element) {
        var dragIndex = -1;

        element.on('dragstart', 'li', function(e) {
          dragIndex = $(this).index();
          e.originalEvent.dataTransfer.effectAllowed = 'move';
        });

        element.on('dragover', 'li', function(e) {
          e.preventDefault();
          $(this).addClass('drag-over');
        });

        element.on('dragleave', 'li', function() {
          $(this).removeClass('drag-over');
        });

        element.on('drop', 'li', function(e) {
          e.preventDefault();
          var dropIndex = $(this).index();
          const selected = scope.providers.providers[dragIndex-1];
          const over = scope.providers.providers[dropIndex-1];
          Dialog.confirmPriorityChange(
          () => UpdateApiProvidersPriority.update({realm: scope.realm.id, providerId: selected.id}, {priority: over.providerPriority}, function(data) {
            Notifications.success($translate.instant("priority-updated-successfully"));
            scope.initQuery()
          }))
      });
    }
    }
  });

  module.directive('draggableListVerifiers', function(UpdateApiVerifierPriority,$translate, Notifications, Dialog) {
    return {
      restrict: 'A',
      link: function(scope, element) {
        var dragIndex = -1;
        var draggingElement

        element.on('dragstart', 'li', function(e) {
          dragIndex = $(this).index();
          draggingElement = $(this);
          e.originalEvent.dataTransfer.effectAllowed = 'move';
        });

        element.on('dragover', 'li', function(e) {
          e.preventDefault();
          $(this).addClass('drag-over');
        });

        element.on('dragleave', 'li', function() {
          $(this).removeClass('drag-over');
        });

        element.on('drop', 'li', function(e) {
          e.preventDefault();
          let isElementUnderSameGroup = $(this)?.attr("id")?.startsWith(`verifier-${draggingElement?.attr("data-name")?.replaceAll(' ','-').toLowerCase()}`);
          let isElementItself = $(this)?.attr("id") === draggingElement?.attr("id");
          if(isElementUnderSameGroup && !isElementItself) {
              var dropIndex = $(this).index();
              const selected = scope.verifiers[dragIndex-1];
              const over = scope.verifiers[dropIndex-1];
              Dialog.confirmPriorityChange(() =>
              UpdateApiVerifierPriority.update({realm: scope.realm.id, verifierId: selected.id}, {priority: over.priority}, function(data) {
                Notifications.success($translate.instant("verifier-updated-successfully"));
                scope.initQuery()
              }))
          }
      });
    }
    }
  });

module.controller('RealmDetailCtrl', function ($scope, $modal, $timeout, $document, $rootScope, $route, Current, Realm, realm, serverInfo, $http, $location, $window, Dialog, RealmClearUserCache, RealmClearRealmCache, RealmClearKeysCache, RealmClearReportQueue, Notifications, Auth, $translate, ConfirmExitDialog, ConvertLogoToUrl, angularSlideOutPanel, CustomDomainCreate, CustomDomainVerify, ARecordCreate, ARecordVerify, IngressCreate, IngressRemove, $translate, CustomDomainList, GetAllDomains, GetAllBOMarketplaceCategory) {
    $scope.createRealm = !realm.realm;
    $scope.stage = 1;
    $scope.serverInfo = serverInfo;
    $scope.showAction = false;
    $scope.validLogo = true;
    $scope.isLogoUpdated = false;
    $scope.logoFile;
    $scope.realmName = realm.realm;
    $scope.disableRename = true; //realm.realm == masterRealm;
    $scope.authServerUrl = authServerUrl;
    $scope.tenantLogoHelptxt = $translate.instant('tenant-logo-helptxt');
    $rootScope.isTenantDelReq = false;
    $scope.realm = angular.copy(realm);
    $scope.oldCopy = angular.copy(realm);
    $scope.changed = $scope.create;
    $scope.openRedirect = false;
    $scope.showDropdownIndex = -1;
    $scope.domains = [];
    let isCustomDomainOpened = false;
    let isCustomDomainSettingsOpened = false;
    $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;

    GetAllDomains.get({ realm: realm.realm}, function (data) {
        $scope.domains = data.body.customDomains ? data.body.customDomains : [];
    })

    $scope.makeSideMenuClosed();


    $scope.openCustomDomain = () => {
        if(!isCustomDomainOpened) {
            isCustomDomainOpened = true;
            CustomDomainList.open($scope.realm, () => {
                isCustomDomainOpened = false;
            })
        }
    }

    let ctr = 0;
    let input = document.querySelector("#defRegion");
    let iti = window.intlTelInput(input, {
        initialCountry: $scope.realm.defaultRegion,
        allowDropdown: true,
        separateDialCode: false,
        customPlaceholder: function (selectedCountryPlaceholder, selectedCountryData) {
            if (ctr !== 0) {
                $('#realmRegion').val(selectedCountryData.iso2).trigger('change');
            }
            ctr++;
            return selectedCountryData?.name;
        },
    });

    $('#realmRegion').change(function () {
        $scope.$apply(function () {
            $scope.realm.defaultRegion = $('#realmRegion').val();
        });
    });

    $scope.disablePaste = function (e) {
        e.preventDefault();
        return false;
    };

    var supportedLocalesEntity = ISO6391.getLanguages($scope.serverInfo?.supportedLocales)
    $scope.supportedLocalesName = supportedLocalesEntity.map(lang => lang['nativeName'] );
    $scope.marketplaceCategories = [];
    $scope.preferredCategories = [];
    if($scope.realm.attributes.hasOwnProperty("recommendedMarketplaceCategories") && $scope.realm.attributes.recommendedMarketplaceCategories.trim().length > 0) {
        $scope.preferredCategories = $scope.realm.attributes.recommendedMarketplaceCategories.split(",");
    }
    $scope.preferredCategoriesCopy = angular.copy($scope.preferredCategories);

    $scope.workflowSupportedLocaleOptions = {
        'multiple': true,
        'simple_tags': true,
        'maximumSelectionLength': 3,
        'tags': $scope.supportedLocalesName,
    };

    $scope.showingCategories = false;
    $scope.isMarketplaceCategoriesLoaded = false;
    $scope.showCategories = (event) => {
        $scope.searchCategory = "";
        $scope.showingCategories = !$scope.showingCategories;
        event.stopPropagation();
    };
    $scope.removeCategory = (category) => {
        $scope.marketplaceCategories.push(category);
        $scope.preferredCategories = $scope.preferredCategories.filter(c => c !== category);
    }
    $scope.addCategory = (category) => {
        $scope.preferredCategories.push(category);
        $scope.marketplaceCategories = $scope.marketplaceCategories.filter(c => c !== category);
    }

    $document.on('click', function() {
        $scope.$apply(function() {
            $scope.showingCategories = false;
        });
    });

    var selectedLocalesEntity = ISO6391.getLanguages(realm.preferredLoginLocales);
    $scope.preferredLoginLocales = selectedLocalesEntity.map(lang => lang['nativeName'] );
    $scope.preferredLoginLocalesCopy = angular.copy($scope.preferredLoginLocales);
    $scope.$watch('preferredLoginLocales', function (oldvalue, newvalue) {
        if((Array.isArray($scope.preferredLoginLocales) && $scope.preferredLoginLocales.length > 3) || ( typeof $scope.preferredLoginLocales === 'string' && $scope.preferredLoginLocales.split(',').length > 3)){
            $scope.preferredLoginLocales = angular.copy($scope.preferredLoginLocalesCopy);
            Notifications.error($translate.instant('loginlocle-maximum-limit-error'))
            return;
        }else if((Array.isArray($scope.preferredLoginLocales) && $scope.preferredLoginLocales.length == 0) || ( typeof $scope.preferredLoginLocales === 'string' && $scope.preferredLoginLocales.split(',').length == 0)){
            $scope.preferredLoginLocales = selectedLocalesEntity.map(lang => lang['nativeName'] );
            Notifications.error($translate.instant('loginlocle-minimum-limit-error'))
            return
        }else{
            $scope.preferredLoginLocalesCopy = angular.copy($scope.preferredLoginLocales);
        }
        if(JSON.stringify($scope.preferredLoginLocales) !== JSON.stringify(selectedLocalesEntity.map(lang => lang['nativeName'] ))){
            $scope.changed = true;
        }else{
            $scope.changed = false;
        }
        if (!$scope.realm?.displayName?.trim() || !$scope.realm?.displayName?.trim().match(/^[\p{L}\p{M}\p{Z}\p{N}\-]+$/u) || $scope.realm?.displayName?.trim().length > 55) {
            $scope.changed = false;
        }
    })

    $scope.isMobileWView = false;
    $scope.toggleSideBarShow = function () {
       var sidebarMenu = document.getElementById('setting-sidebar');
       var sidebarIcon = document.getElementById('setting-sidebar-icon');
       if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
           sidebarMenu.classList.remove("bo-setting-sidemenu");
           sidebarMenu.classList.add("bo-setting-sidemenu-show");
           sidebarIcon.classList.add('d-none');
           sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
           $scope.isMobileWView = true;
       }else{
           sidebarMenu.classList.add("bo-setting-sidemenu");
           sidebarMenu.classList.remove("bo-setting-sidemenu-show");
           sidebarIcon.classList.remove('d-none');
           sidebarIcon.classList.add('bo-setting-sidemenu-icon');
           $scope.isMobileWView = false;
       }
   }

    $scope.$watch('realm', function () {
        if ($scope.realm?.realmLogo) {
            $scope.realm.realmLogo = getUpdatedImgData($scope.realm.realmLogo);
        }
        if (!angular.equals($scope.realm, $scope.oldCopy)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
        if (!$scope.realm?.displayName?.trim() || !$scope.realm?.displayName?.trim().match(/^[\p{L}\p{M}\p{Z}\p{N}\-]+$/u) || $scope.realm?.displayName?.trim().length > 55) {
            $scope.changed = false;
        }
    }, true);
    $scope.$watch('realmName', function () {
        if (!angular.equals($scope.realmName, $scope.oldCopy.realm)) {
            $scope.changed = true;
        }
    }, true);

    $scope.copyWorkspace = function (element) {
        /* Creates the empty input element and set the value of coming input field */
        var $temp = $("<input>");
        $("body").append($temp);
        /* Select the Value in Input field */
        $temp.val($('#' + element).text()).select();
        /* Copy the text inside the text field */
        document.execCommand("copy");
        $temp.remove();
    };

    function processLogo(uploader) {
        if (uploader.files && uploader.files[0]) {
            $('#realmLogo').removeAttr('src');
            $('#realmLogo').attr('src',
                window.URL.createObjectURL(uploader.files[0]));
        }
        $scope.logoFile = uploader.files[0];
    }
    $scope.tooltipTxt = $translate.instant("copy");
    $(".copy-icon").click(function () {
        $scope.tooltipTxt = $translate.instant("copied");
        $(".copy-icon").mouseleave(function () {
            $scope.tooltipTxt = $translate.instant("copy");
        });
    });

    $scope.initWorkspaceNameCopy = () => {
        $("#workspace-name-copy-icon").click(function () {
            $scope.tooltipTxt = $translate.instant("copied");
            $("#workspace-name-copy-icon").mouseleave(function () {
                $scope.tooltipTxt = $translate.instant("copy");
            });
        });
    }

    $scope.copyToClipboard = (element) => {
        var $temp = $("<input>");
        $("body").append($temp);
        if (!$(element).val()) {
            $temp.val($(element)[0].innerText).select();
        } else {
            $temp.val($(element).val()).select();
        }
        document.execCommand("copy");
        $temp.remove();
    };

    $("#updateLogo").change(function () {
        processLogo(this);
        $scope.validLogo = true;
        $scope.isLogoUpdated = true;
        checkDimension($scope, "validLogo", this);
    });
    $('#changeLogo').click(function () {
        $("#updateLogo").val("");
        $('#updateLogo').trigger('click');
    });

    $scope.save = function () {
        $scope.stage = 2;
        $('#cancelBtn').attr('disabled', 'disabled');
        var realmCopy = angular.copy($scope.realm);
        realmCopy.realm = $scope.realmName;
        realmCopy.preferredLoginLocales = $scope.preferredLoginLocales.map(lang => ISO6391.getCode(lang) );
        realmCopy.attributes.recommendedMarketplaceCategories = $scope.preferredCategories.join();
        $scope.changed = false;
        $scope.nameChanged = !angular.equals($scope.realmName, $scope.oldCopy.realm);
        $scope.oldName = $scope.oldCopy.realm;

        if (!$scope.validLogo) {
            Notifications.error($translate.instant('logo-error-msg'));
            $scope.reset();
            $scope.stage = 1;
            $('#cancelBtn').removeAttr('disabled', 'disabled');
            return;
        }

        if ($scope.isLogoUpdated) {
            let imgVersion;
            if (realmCopy.realmLogo) {
                const oldVersion = realmCopy.realmLogo.split('v=')[1];
                imgVersion = !isNaN(oldVersion) ? Number(oldVersion) + 1 : 1;
            } else {
                imgVersion = 1;
            }
            let formData = new FormData();
            formData.append("realmLogo", $scope.logoFile);
            ConvertLogoToUrl.create({
                realm: $scope.realm.realm
            }, formData, function (res) {
                if (res.url) {
                    // realmCopy.realmLogo = getValidDomainForMinio(location.origin) + "/" + res.url + '?v=' + imgVersion;
                    realmCopy.realmLogo = res.url + '?v=' + imgVersion;
                }
                $scope.updateRealm(realmCopy);
            }, function (err) {
                console.log(err);
                $route.reload();
            });
        } else {
            $scope.updateRealm(realmCopy);
        }
    };

    $scope.updateRealm = function (realmCopy) {
        realmCopy.defaultRegion = iti.getSelectedCountryData().iso2;
        localStorage.setItem('ezto-dashboard-preferred-locale', realmCopy.attributes.dashboardLocale);
        Realm.update({ id: $scope.oldCopy.realm }, realmCopy, function () {
            $scope.stage = 1;
            $scope.changed = false;
            // var data = Realm.query(function () {
            //     Current.realms = data;
            //     for (var i = 0; i < Current.realms.length; i++) {
            //         if (Current.realms[i].realm == realmCopy.realm) {
            //             Current.realm = Current.realms[i];
            //             $scope.oldCopy = angular.copy($scope.realm);
            //         }
            //     }
            // });
            $scope.oldCopy = angular.copy(realmCopy);
            Current.realm = angular.copy(realmCopy);
            $scope.realm.realmLogo = realmCopy.realmLogo;
            if ($scope.nameChanged) {
                console.debug(Auth);
                console.debug(Auth.authz.tokenParsed.iss);

                if (Auth.authz.tokenParsed.iss.endsWith(masterRealm)) {
                    Auth.refreshPermissions(function () {
                        Auth.refreshPermissions(function () {
                            Notifications.success($translate.instant("changes-saved-to-realm"));
                            $scope.$apply(function () {
                                $scope.openRedirect = true;
                                $location.url("/realms/" + realmCopy.realm + "/realm-settings");
                            });
                        });
                    });
                } else {
                    delete Auth.authz.token;
                    delete Auth.authz.refreshToken;

                    var newLocation = $window.location.href.replace('/' + $scope.oldName + '/', '/' + realmCopy.realm + '/')
                        .replace('/realms/' + $scope.oldName, '/realms/' + realmCopy.realm);
                    window.location.replace(newLocation);
                }
            } else {
                $scope.openRedirect = true;
                $location.url("/realms/" + realmCopy.realm + "/realm-settings");
            }
            $route.reload();
        }, function () {
            $scope.stage = 1;
            $('#cancelBtn').removeAttr('disabled', 'disabled');
        });
    };

    $scope.getAllMarketplaceCategory = function () {
        GetAllBOMarketplaceCategory.query({ realm: $scope.realm.realm, first: -1, max: -1}, function (res) {
            var resFilteredName = res.map(obj => obj.category);
            $scope.marketplaceCategories = resFilteredName.filter(element => !$scope.preferredCategories.includes(element));
            $scope.isMarketplaceCategoriesLoaded = true;
            $scope.$watch('preferredCategories', function () {
                if (!angular.equals($scope.preferredCategoriesCopy, $scope.preferredCategories) && $scope.realm?.displayName) {
                    $scope.changed = true;
                } else {
                    $scope.changed = false;
                }
            }, true);
        });
    };

    $scope.getAllMarketplaceCategory();

    $scope.reset = function () {
        if ($scope?.oldCopy?.defaultRegion) {
            ctr = 0;
            iti.setCountry($scope.oldCopy.defaultRegion);
        } else {
            ctr = 0;
            iti.setCountry("us");
        }
        $scope.validLogo = true;
        if ($scope.oldCopy.realmLogo) {
            $('#realmLogo').attr('src', `${getUpdatedImgData($scope.oldCopy.realmLogo)}`);
            $scope.realm.realmLogo = angular.copy($scope.oldCopy.realmLogo);
        } else {
            if(realm.mode === 'wiam' || realm.mode === 'ciam'){
            $('#realmLogo').attr('src', `${resourceUrl}/themes/img/logo.svg?version=${serverVersion}`);
            } else {
            $('#realmLogo').attr('src', `${resourceUrl}/themes/img/eztovrfy-logo.svg?version=${serverVersion}`);
            }

            $scope.realm.realmLogo = "";
        }
        $scope.preferredLoginLocales = selectedLocalesEntity.map(lang => lang['nativeName'] );
        $scope.realm = angular.copy($scope.oldCopy);
        if($scope.realm.attributes.hasOwnProperty("recommendedMarketplaceCategories") && $scope.realm.attributes.recommendedMarketplaceCategories.trim().length > 0) {
            $scope.preferredCategories = $scope.realm.attributes.recommendedMarketplaceCategories.split(",");
        } else {
            $scope.preferredCategories = [];
        }
        $scope.changed = false;
    };

    $scope.cancel = function () {
        window.history.back();
    };

    //Cache Clear Controls
    $scope.clearUserCache = function () {
        Dialog.confirmClearCache('User', function () {
            RealmClearUserCache.save({ realm: realm.realm }, function () {
                Notifications.success($translate.instant("user-cache-cleared"));
            }, function () {
                Notifications.success($translate.instant("unexpected-error-occured"));
            });
        });

    };

    $scope.clearRealmCache = function () {
        Dialog.confirmClearCache('workspace', function () {
            RealmClearRealmCache.save({ realm: realm.realm }, function () {
                Notifications.success($translate.instant("realm-cache-cleared"));
            }, function () {
                Notifications.success($translate.instant("unexpected-error-occured"));
            });
        });
    };

    $scope.clearKeysCache = function () {
        Dialog.confirmClearCache('Keys', function () {
            RealmClearKeysCache.save({ realm: realm.realm }, function () {
                Notifications.success($translate.instant("public-keys-cache-cleared"));
            }, function () {
                Notifications.success($translate.instant("unexpected-error-occured"));
            });
        });
    };

    $scope.clearKeysCache = function () {
        Dialog.confirmClearCache('Keys', function () {
            RealmClearKeysCache.save({ realm: realm.realm }, function () {
                Notifications.success($translate.instant("public-keys-cache-cleared"));
            }, function () {
                Notifications.success($translate.instant("unexpected-error-occured"));
            });
        });
    };

    $scope.clearReportQueue = function () {
        Dialog.confirmClearQueue('Report', function () {
            RealmClearReportQueue.save({ realm: realm.realm }, function () {
                Notifications.success($translate.instant("report-queue-cleared"));
            }, function () {
                Notifications.success($translate.instant("unexpected-error-occured"));
            });
        });
    };



    $scope.openCustomDomainSettings = function () {
        if(isCustomDomainSettingsOpened){
            return;
        }
        isCustomDomainSettingsOpened = true;
        if (!$scope.realm.dnsenabled) {
            Dialog.message($translate.instant('license-restriction'),  $translate.instant('license-restriction-desc'));
            isCustomDomainSettingsOpened = false;
            return;
        } else if ($scope.realm.accessTokenLifespan <= 600) {
            Dialog.message($translate.instant('license-invalid-expiry'), $translate.instant('license-invalid-expiry-desc'));
            isCustomDomainSettingsOpened = false;
        } else {
            var controller = function ($scope, $location) {
                $scope.resourceUrl = resourceUrl;
                $scope.realm = realm;
                $scope.init = function () {
                    $scope.domain = {
                        name: "",
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.domainRecord = {
                        recordData: null,
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.aRecord = {
                        recordData: null,
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.isCert = {
                        enabled: false
                    };

                    $scope.cert = {
                        name: null,
                        recordData: null,
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.certKey = {
                        name: null,
                        recordData: null,
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.removeDomain = {
                        isLoading: false
                    };

                    $scope.stage = 1;
                };

                $scope.reset = function () {
                    $scope.domain = {
                        name: "",
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.domainRecord = {
                        recordData: null,
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.aRecord = {
                        recordData: null,
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.isCert = {
                        enabled: false
                    };

                    $scope.cert = {
                        name: null,
                        recordData: null,
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.certKey = {
                        name: null,
                        recordData: null,
                        stepCompleted: false,
                        isLoading: false,
                        err: ""
                    };

                    $scope.removeDomain = {
                        isLoading: false
                    };

                    $scope.stage = 1;
                };

                $scope.validateDomain = function () {
                    $scope.domain.name = $scope.domain.name.toLowerCase();
                    var val = $scope.domain.name;
                    if (val === '' || /^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9]{2,63}$/i.test(val)) {
                        $scope.domain.err = "";
                    } else {
                        $scope.domain.err = $translate.instant('valid-domain-name');
                    }
                };

                $scope.validateCert = function (val) {
                    if (val.trim().startsWith('-----BEGIN CERTIFICATE-----') && val.trim().endsWith('-----END CERTIFICATE-----')) {
                        return true;
                    }
                    return false;
                };
                $scope.validateCertKey = function (val) {
                    if (val.trim().startsWith('-----BEGIN PRIVATE KEY-----') && val.trim().endsWith('-----END PRIVATE KEY-----')) {
                        return true;
                    }
                    return false;
                };

                function getBase64(file) {
                    return new Promise((resolve, reject) => {
                        const reader = new FileReader();
                        reader.readAsDataURL(file);
                        reader.onload = () => resolve(reader.result);
                        reader.onerror = error => reject(error);
                    });
                }

                $scope.uploadFile = function ($files, fileType) {
                    var reader = new FileReader();
                    reader.readAsText($files[0]);
                    reader.onload = function (e) {
                        if (fileType === 'certificate') {
                            $scope.$apply(function () {
                                $scope.cert.isLoading = true;
                            });
                            if ($scope.validateCert(e.target.result)) {
                                getBase64($files[0]).then(function (data) {
                                    $scope.$apply(
                                        function () {
                                            $scope.cert.err = "";
                                            $scope.cert.name = $files[0].name;
                                            $scope.cert.stepCompleted = true;
                                            $scope.cert.recordData = data.split("base64,")[1];
                                            $scope.cert.isLoading = false;
                                        }
                                    );
                                });
                            } else {
                                $scope.$apply(function () {
                                    $scope.cert.err = "The certificate is not valid.";
                                    $scope.cert.name = "";
                                    $scope.cert.stepCompleted = false;
                                    $scope.cert.recordData = "";
                                    $scope.cert.isLoading = false;
                                });
                            }
                        } else {
                            $scope.$apply(function () {
                                $scope.certKey.isLoading = true;
                            });
                            if ($scope.validateCertKey(e.target.result)) {
                                getBase64($files[0]).then(function (data) {
                                    $scope.$apply(
                                        function () {
                                            $scope.certKey.err = "";
                                            $scope.certKey.name = $files[0].name;
                                            $scope.certKey.stepCompleted = true;
                                            $scope.certKey.recordData = data.split("base64,")[1];
                                            $scope.certKey.isLoading = false;
                                        }
                                    );
                                });
                            } else {
                                $scope.$apply(function () {
                                    $scope.certKey.err =
                                        "The certificate key is not valid";
                                    $scope.certKey.name = "";
                                    $scope.certKey.stepCompleted = false;
                                    $scope.certKey.recordData = "";
                                    $scope.certKey.isLoading = false;
                                });
                            }
                        }
                    };
                };

                $scope.createDomain = function () {
                    $scope.domain.isLoading = true;
                    CustomDomainCreate.create(
                        { realm: realm.realm},
                        {domainName: $scope.domain.name },
                        function (data) {
                            if (data.status === 200) {
                                $scope.domain.stepCompleted = true;
                                $scope.domainRecord.recordData = data.body;
                                $scope.domain.err = '';
                                $scope.domain.isLoading = false;
                            } else {
                                $scope.domain.err = data?.body?.message ? data?.body?.message : data?.body;
                                $scope.domain.isLoading = false;
                            }
                        },
                        function (data) {
                            $scope.domain.err = data?.body?.message ? data?.body?.message : data?.body;
                            $scope.domain.isLoading = false;
                        }
                    );
                };

                $scope.verifyDomain = function () {
                    $scope.domainRecord.isLoading = true;
                    CustomDomainVerify.verify(
                        { realm: realm.realm },
                        { domainName: $scope.domain.name},
                        function (data) {
                            if (data.status === 200) {
                                $scope.domainRecord.err = "";
                                $scope.domainRecord.stepCompleted = true;

                                // To create a record once the domain record is created.
                                ARecordCreate.create(
                                    { realm: realm.realm },
                                    { domainName: $scope.domain.name},
                                    function (data) {
                                        if (data.status === 200) {
                                            $scope.aRecord.recordData = data.body;
                                            $scope.aRecord.err = "";
                                            $scope.domainRecord.isLoading = false;
                                        } else {
                                            $scope.domainRecord.err = data?.body?.message ? data?.body?.message : data?.body;
                                            $scope.domainRecord.isLoading = false;
                                        }
                                    },
                                    function (data) {
                                        $scope.domainRecord.err = data?.body?.message ? data?.body?.message : data?.body;
                                        $scope.domainRecord.isLoading = false;
                                    }
                                );
                            } else {
                                $scope.domainRecord.err = data?.body?.message ? data?.body?.message : data?.body;
                                $scope.domainRecord.stepCompleted = false;
                                $scope.domainRecord.isLoading = false;
                            }
                        },
                        function (data) {
                            $scope.domainRecord.err = data?.body?.message ? data?.body?.message : data?.body;
                            $scope.domainRecord.stepCompleted = false;
                            $scope.domainRecord.isLoading = false;
                        }
                    );
                };

                $scope.verifyARecord = function () {
                    $scope.aRecord.isLoading = true;
                    ARecordVerify.verify(
                        { realm: realm.realm},
                        { domainName: $scope.domain.name},
                        function (data) {
                            if (data.status === 200) {
                                $scope.aRecord.err = "";
                                $scope.aRecord.stepCompleted = true;
                                $scope.aRecord.isLoading = false;
                            } else {
                                $scope.aRecord.err = data?.body?.message ? data?.body?.message : data?.body;
                                $scope.aRecord.stepCompleted = false;
                                $scope.aRecord.isLoading = false;
                            }
                        },
                        function (data) {
                            $scope.aRecord.err = data?.body?.message ? data?.body?.message : data?.body;
                            $scope.aRecord.stepCompleted = false;
                            $scope.aRecord.isLoading = false;
                        }
                    );
                };

                $scope.save = function (callback) {
                    $scope.stage = 2;
                    if (
                        $scope.domain.stepCompleted &&
                        $scope.domainRecord.stepCompleted &&
                        $scope.aRecord.stepCompleted && (!$scope.isCert.enabled || ($scope.isCert.enabled && $scope.cert.stepCompleted && $scope.certKey.stepCompleted))
                    ) {
                        const resBody = {};
                        if ($scope.isCert.enabled) {
                            resBody['customCert'] = true;
                            resBody['crt'] = $scope.cert.recordData;
                            resBody['key'] = $scope.certKey.recordData;
                        }
                        IngressCreate.create(
                            { realm: realm.realm, domain:  $scope.domain.name},
                            { ...resBody },
                            function (data) {
                                if (data.status === 200) {
                                    Notifications.success(data?.body?.message ? data?.body?.message : data?.body);
                                    // let workspaceURL = new URL($scope.realm.workspace);
                                    // $scope.stage = 1;
                                    // if (workspaceURL.origin === window.location.origin) {
                                    //     window.location.hostname = $scope.domain.name;
                                    // } else {
                                    //     $route.reload();
                                    // }
                                    $scope.stage = 1;
                                    $scope.$panelInstance.dismiss();
                                } else {
                                    $scope.stage = 1;
                                    Notifications.error(data?.body?.message ? data?.body?.message : data?.body);
                                }
                                if (callback) {
                                    callback();
                                }
                            },
                            function (data) {
                                if (callback) {
                                    callback();
                                }
                                $scope.stage = 1;
                                Notifications.error(data?.body?.message ? data?.body?.message : data?.body);
                            }
                        );
                    } else {
                        if (callback) {
                            callback();
                        }
                        $scope.stage = 1;
                    }
                };

                $scope.removeCustomDomain = function () {
                    Dialog.confirmDelete($scope.realm.workspace, $translate.instant('custom-domain'), function () {
                        $scope.removeDomain.isLoading = true;
                        IngressRemove.remove(
                            { realm: realm.realm },
                            {},
                            function (data) {
                                if (data.status === 200) {
                                    $scope.removeDomain.isLoading = false;
                                    Notifications.success(data?.body?.message ? data?.body?.message : data?.body);
                                    let newWorkspaceURL = new URL(data.body.workspaceUrl);
                                    let workspaceURL = new URL($scope.realm.workspace);
                                    if (workspaceURL.origin === window.location.origin) {
                                        window.location.href = data.body.workspaceUrl;
                                    } else {
                                        $route.reload();
                                    }
                                } else {
                                    $scope.removeDomain.isLoading = false;
                                    Notifications.error(data?.body?.message ? $translate.instant(data?.body?.message) : data?.body);
                                    $route.reload();
                                }
                            },
                            function (data) {
                                $scope.removeDomain.isLoading = false;
                                Notifications.error(data?.body?.message ? $translate.instant(data?.body?.message) : data?.body);
                            }
                        );
                    });
                };

                $scope.dismissPanel = function () {
                    $scope.$panelInstance.dismiss("this is from the controller!!");
                };

                $scope.$on("$routeChangeStart", function () {
                    $scope.$panelInstance.dismiss();
                });
            };

            $http
                .get(resourceUrl + "/partials/panels/domain-settings-panel.html")
                .then(function (result) {
                    var panelInstance2 = angularSlideOutPanel.open({
                        template: result.data,
                        openOn:  $scope.getPanelDirection(),
                        backdrop: "static",
                        controller: ["$scope", "$location", controller]
                    });
                    panelInstance2.result
                        .then(function (result) {
                            console.log("panel was closed with result : ", result);
                            isCustomDomainSettingsOpened = false;
                        })
                        .catch(function (error) {
                            console.log("panel was rejected with error : ", error);
                            isCustomDomainSettingsOpened = false;
                            $scope.dismissPanel();
                        });
                });
        }
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed && !$rootScope.isTenantDelReq) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('RealmAccessKeysCtrl', function($scope, $modal, $translate, realm, Session, Notifications, GetAccessKeys, CreateAccessKey, AccessKey, InputDialog){
    $scope.realm=realm;
    $scope.loading = true;
    $scope.loaderTypeName = 'realmAttributesLoader';
    $scope.accessKeys = { content:[], totalElements: 0};
    $scope.currentPage = 1;
    $scope.itemsPerPage = Session.getItem("EZTOVerifyAccessKeyPageCount") ? Session.getItem("EZTOVerifyAccessKeyPageCount") : 5;
    $scope.pagination = {
        firstResult: 0,
        maxResults: $scope.itemsPerPage,
        searchValue: ''
    };
    $scope.isMobileWView = false;

    $scope.toggleSideBarShow = function () {
       var sidebarMenu = document.getElementById('setting-sidebar');
       var sidebarIcon = document.getElementById('setting-sidebar-icon');
       if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
           sidebarMenu.classList.remove("bo-setting-sidemenu");
           sidebarMenu.classList.add("bo-setting-sidemenu-show");
           sidebarIcon.classList.add('d-none');
           sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
           $scope.isMobileWView = true;
       }else{
           sidebarMenu.classList.add("bo-setting-sidemenu");
           sidebarMenu.classList.remove("bo-setting-sidemenu-show");
           sidebarIcon.classList.remove('d-none');
           sidebarIcon.classList.add('bo-setting-sidemenu-icon');
           $scope.isMobileWView = false;
       }
   }

    $scope.makeSideMenuClosed();

    $scope.pageCountChanged = function () {
        Session.setItem("EZTOVerifyAccessKeyPageCount", $scope.itemsPerPage);
        $scope.changePage(1);
    };

    $scope.clearSearch = function () {
        $scope.pagination.searchValue = '';
        $scope.changePage(1);
    }

    $scope.changePage = function(pageNumber) {
        $scope.currentPage = pageNumber;
        $scope.pagination.firstResult = pageNumber === 1 ? 0 : (pageNumber - 1) * $scope.itemsPerPage;
        $scope.pagination.maxResults = $scope.itemsPerPage;
        $scope.getAccessKeys();
    };

    $scope.getAccessKeys = function () {
        $scope.loading = true;
        GetAccessKeys.get({ realm: $scope.realm.realm, first: $scope.pagination.firstResult, max: $scope.pagination.maxResults, search: $scope.pagination.searchValue }, function (data) {
            $scope.accessKeys = data;
            $scope.loading = false;
        }, function (data) {
            Notifications.error($translate.instant("access-keys-fetch-error"));
            $scope.loading = false;
        })
    }

    $scope.createNewAccessKey = function () {
        var controller = function ($scope, $modalInstance, $location, $translate, Notifications) {
            $scope.accessKeyName = '';
            $scope.create = function () {
                $scope.accessKeyName = $('#name').val();
                if($scope.accessKeyName.length){
                    CreateAccessKey.save({ realm: realm.realm }, { name : $('#name').val().trim() }, function (response) {
                        Notifications.success($translate.instant("access-key-created-success"));
                        $location.url("/realms/" + realm.realm + '/realm-access-keys/' + response.id);
                    });
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $(document).ready(function () {
                setTimeout(function () {
                    $("#name").focus();
                }, 100);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-access-key.html',
            controller: controller,
            backdrop: 'static',
        });
    }

    $scope.deleteAccessKey = function (key) {
        InputDialog.confirmTenantDelete(key.name, 'access-key', function() {
            AccessKey.remove({realm: $scope.realm.realm, accessKey: key.id}, function (data){
                Notifications.success($translate.instant("access-key-deleted-success"));
                $scope.getAccessKeys();
            }, function (err) {
                Notifications.error($translate.instant("access-key-deleted-error"));
            })
        });
    }

    $scope.getAccessKeys();
})

module.controller('RealmProducts', function($scope, $location, $translate, realm, subRealm, LicenseProducts){
    $scope.realm=realm;
    $scope.subRealm = subRealm;
    $scope.realm = angular.copy($scope.realm);
    $scope.isCustomPricedTenant = ($scope.subRealm.attributes && $scope.subRealm.attributes.hasOwnProperty('tenant_pricing_type') && $scope.subRealm.attributes['tenant_pricing_type'] == 'custom') ? true : false;
    $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;

    if(!(!$scope.isRootAccount ? Current.realm.adminRealm && Current.realm.adminRealm === true && $scope.isCustomPricedTenant : true)){
        $location.path("/notfound");
    }
    $scope.makeSideMenuClosed();
    $scope.loading = false;
    $scope.loaderTypeName = 'realmAttributesLoader';
    $scope.tooltipTxt = $translate.instant("copy");
    $scope.products = [];
    $scope.currentPage = 1;
    $scope.searchValue = '';
    $scope.isMobileWView = false;

    $scope.toggleSideBarShow = function () {
       var sidebarMenu = document.getElementById('setting-sidebar');
       var sidebarIcon = document.getElementById('setting-sidebar-icon');
       if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
           sidebarMenu.classList.remove("bo-setting-sidemenu");
           sidebarMenu.classList.add("bo-setting-sidemenu-show");
           sidebarIcon.classList.add('d-none');
           sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
           $scope.isMobileWView = true;
       }else{
           sidebarMenu.classList.add("bo-setting-sidemenu");
           sidebarMenu.classList.remove("bo-setting-sidemenu-show");
           sidebarIcon.classList.remove('d-none');
           sidebarIcon.classList.add('bo-setting-sidemenu-icon');
           $scope.isMobileWView = false;
       }
   }


    $scope.copytoClipboard = function(value) {
        var $temp = $("<input>");
        $("body").append($temp);
        $temp.val(value.trim()).select();
        $scope.tooltipTxt = $translate.instant("copied");
        document.execCommand("copy");
        setInterval(() => $scope.tooltipTxt = $translate.instant('copy'), 3000);
        $temp.remove();
    }

    $scope.getProducts = function () {
        $scope.loading = true;
        LicenseProducts.query({ realm: $scope.subRealm.realm, }, function (data) {
            $scope.products = data;
            $scope.loading = false;
        }, function (data) {
            Notifications.error($translate.instant("license-products-fetch-error"));
            $scope.loading = false;
        })
    }

    $scope.createNewProduct = function () {
        $location.url("/realms/" + realm.realm + '/product/create');
    }

    $scope.clearSearch = function () {
        $scope.searchValue = '';
    }

    $scope.getProducts();
})

module.controller('RealmCreateProduct', function($scope, $translate, $location, realm, Notifications, CreateLicenseProduct){
    $scope.realm = realm;
    $scope.isCustomPricedTenant = ($scope.realm.attributes && $scope.realm.attributes.hasOwnProperty('tenant_pricing_type') && $scope.realm.attributes['tenant_pricing_type'] == 'custom') ? true : false;
    $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;
    if(!(!$scope.isRootAccount ? $scope.realm.adminRealm && $scope.realm.adminRealm === true && $scope.isCustomPricedTenant : true)){
        $location.path("/notfound");
    }
    $scope.makeSideMenuClosed();
    $scope.product = {};
    $scope.newDescription = {
        languageCode: '',
        description: ''
    };
    $scope.logoUploadTxt = $translate.instant('upload-logo');
    $scope.changed = false;
    $scope.saveInProgress = false;
    $scope.isMobileWView = false;

    $scope.toggleSideBarShow = function () {
       var sidebarMenu = document.getElementById('setting-sidebar');
       var sidebarIcon = document.getElementById('setting-sidebar-icon');
       if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
           sidebarMenu.classList.remove("bo-setting-sidemenu");
           sidebarMenu.classList.add("bo-setting-sidemenu-show");
           sidebarIcon.classList.add('d-none');
           sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
           $scope.isMobileWView = true;
       }else{
           sidebarMenu.classList.add("bo-setting-sidemenu");
           sidebarMenu.classList.remove("bo-setting-sidemenu-show");
           sidebarIcon.classList.remove('d-none');
           sidebarIcon.classList.add('bo-setting-sidemenu-icon');
           $scope.isMobileWView = false;
       }
   }

    $scope.$watch('product', function(){
        if($scope.product.name && $scope.product.name.trim().length &&
            $scope.product.externalId && $scope.product.externalId.trim().length &&
            $scope.product.image && $scope.product.image.trim().length &&
            $scope.product.productType && $scope.product.productType.trim().length &&
            $scope.product.description && Object.keys($scope.product.description).length ){
                $scope.changed = true;
        }else{
            $scope.changed = false;
        }
    }, true);

    $("#updateLogo").change(function () {
        processLogo(this);
    });

    function processLogo(uploader) {
        if (uploader.files && uploader.files[0]) {
            $scope.$apply(function(){
                $('#productLogo').attr('src', window.URL.createObjectURL(uploader.files[0]));
                $scope.logoUploadTxt = $translate.instant('change-tenant-logo');
            })
            const reader = new FileReader();
            reader.onload = function(e) {
                const binaryString = e.target.result;
                const base64String = btoa(binaryString);
                $scope.product.image = base64String;
            };
            reader.onerror = function(error) {
                console.error('Error reading file:', error);
            };
            reader.readAsBinaryString(uploader.files[0]);
        }
    }

    $scope.uploadImage = () => {
        $('#updateLogo').trigger('click');
    };

    $scope.addDescription = () => {
        let code = $scope.newDescription.languageCode.trim();
        let description = $scope.newDescription.description.trim();
        if(code.length && description.length){
            if(!$scope.product.description){
                $scope.product.description = {};
            }
            $scope.product.description[code] = description;
            $scope.newDescription.languageCode = '';
            $scope.newDescription.description = '';
        }
    };

    $scope.removeDescription = (code) => {
        delete $scope.product.description[code];
    };

    $scope.saveRealmProduct = () => {
        // $scope.saveInProgress = true;
        $scope.changed = false;
        CreateLicenseProduct.create({ realm: $scope.realm.realm }, $scope.product ,function (data) {
            // $scope.saveInProgress = false;
            Notifications.success($translate.instant("license-product-save-success"));
            $location.path("/realms/"+$scope.realm.realm+"/products")
        }, function (error) {
            Notifications.error($translate.instant(error.errorMessage));
            // $scope.saveInProgress = false;
            $scope.changed = true;
        })
    }
})

module.controller('RealmProductPricing', function($scope, $modal, realm, subRealm, GetProductDetail, LicenseProducts, UpdateLicensePricing, $location, InputDialog, Notifications, $translate){
    $scope.subRealm = subRealm;
    $scope.realm = realm;
    $scope.isCustomPricedTenant = ($scope.subRealm.attributes && $scope.subRealm.attributes.hasOwnProperty('tenant_pricing_type') && $scope.subRealm.attributes['tenant_pricing_type'] == 'custom') ? true : false;
    $scope.isRootAccount = $scope.realm.id === $scope.rootAccountName;
    // if(!(!$scope.isRootAccount ? $scope.realm.adminRealm && $scope.realm.adminRealm === true && $scope.isCustomPricedTenant : true)){
    //     $location.path("/notfound");
    // }
    $scope.makeSideMenuClosed();
    $scope.product ={};
    $scope.productCopy = {};
    $scope.isProductEditable = false;
    $scope.newDescription = {
        languageCode: '',
        description: ''
    };
    $scope.changed = false;
    $scope.saveInProgress = false;
    $scope.searchValue = '';
    $scope.currencyFilterValue = 'inr';
    $scope.isMobileWView = false;

    $scope.toggleSideBarShow = function () {
       var sidebarMenu = document.getElementById('setting-sidebar');
       var sidebarIcon = document.getElementById('setting-sidebar-icon');
       if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
           sidebarMenu.classList.remove("bo-setting-sidemenu");
           sidebarMenu.classList.add("bo-setting-sidemenu-show");
           sidebarIcon.classList.add('d-none');
           sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
           $scope.isMobileWView = true;
       }else{
           sidebarMenu.classList.add("bo-setting-sidemenu");
           sidebarMenu.classList.remove("bo-setting-sidemenu-show");
           sidebarIcon.classList.remove('d-none');
           sidebarIcon.classList.add('bo-setting-sidemenu-icon');
           $scope.isMobileWView = false;
       }
   }


    $scope.$watch('product', function(){
        if($scope.product.name && $scope.product.name.trim().length &&
            $scope.product.externalId && $scope.product.externalId.trim().length &&
            $scope.product.image && $scope.product.image.trim().length &&
            $scope.product.productType && $scope.product.productType.trim().length &&
            $scope.product.description && Object.keys($scope.product.description).length &&
            !angular.equals($scope.product, $scope.productCopy) ){
                $scope.changed = true;
        }else{
            $scope.changed = false;
        }
    }, true);

    $scope.getPriceName = function (price) {
        return price.metadata.filter((meta) => meta.key == 'stage' )[0]?.value;
    }

    $scope.filterByCurrency = function(price) {
        const matchesCurrency = price.currencyOptions[0].currency === $scope.currencyFilterValue;
        var matchesSearch;
        if($scope.product.name == "Transactions") {
            let searchKey = (price.name && price.name.length > 0) ? price.name : 'default';
            matchesSearch = !$scope.searchValue || searchKey.toLowerCase().includes($scope.searchValue.toLowerCase());
        } else {
            matchesSearch = !$scope.searchValue || price.priceId.toLowerCase().includes($scope.searchValue.toLowerCase());
        }
        return matchesCurrency && matchesSearch;
      };

    $scope.makeEditable = (boolean) => {
        $scope.isProductEditable = boolean;
        if(!boolean){
            $scope.product = angular.copy($scope.productCopy);
        }
    }

    $scope.addDescription = () => {
        let code = $scope.newDescription.languageCode.trim();
        let description = $scope.newDescription.description.trim();
        if(code.length && description.length){
            if(!$scope.product.description){
                $scope.product.description = {};
            }
            $scope.product.description[code] = description;
            $scope.newDescription.languageCode = '';
            $scope.newDescription.description = '';
        }
    };

    $scope.removeDescription = (code) => {
        delete $scope.product.description[code];
    };

    $scope.setProductDetail = (data) => {
        $scope.product = data;
        $scope.productCopy = angular.copy($scope.product);
    }

    $scope.addPricing = (productId, realm, setProductDetail, isRootAccount, productName) => {
        var controller = function ($scope, $modalInstance, $translate, Notifications, CreateLicensePricing, productName) {
            $scope.price = {
                name: '',
                currency: '',
                amount: '',
                consider: false,
                addToCustomSubtenant: false
            };
            $scope.productName = productName;

            $scope.isRootAccount = isRootAccount;

            $scope.convertToNumberFormate = () => {
                $scope.price.amount = angular.copy($scope.price.amount.replace(/[^0-9.]/g, ''));
            }

            $scope.create = function () {
                let addToCustomSubtenant = $scope.price.addToCustomSubtenant;
                let pricingData = {
                    productId : productId,
                    tenantId : realm.id,
                    pricingType : realm.attributes.hasOwnProperty('tenant_pricing_type') ? realm.attributes['tenant_pricing_type'] : 'default',
                    metadata : [],
                    consider: $scope.price.consider == true ? "always" : null,
                    currencyOptions : [ {name: $scope.price.name, currency: $scope.price.currency, amount:$scope.price.amount}]
                }
                CreateLicensePricing.create({ realm: realm.realm, addToCustomSubtenant: addToCustomSubtenant }, pricingData ,function (data) {
                    setProductDetail(data);
                    Notifications.success($translate.instant("license-pricing-add-success"));
                    $scope.cancel();
                }, function () {
                    Notifications.error($translate.instant("license-pricing-add-error"));
                })
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/realm-price-create.html',
            controller: controller,
            backdrop: 'static',
            size: 'md',
            resolve: {
                productName: function () {
                    return productName;
                }
            }
        });
    }

    $scope.deletePricing = (price) => {
        InputDialog.confirmTenantDelete((price.name && price.name.length) ? price.name : 'default', 'pricing', function() {
            $scope.productDuplicate = angular.copy($scope.product);
            $scope.productDuplicate.pricing = angular.copy( $scope.product.pricing.filter(p => p.priceId !== price.priceId));
            let currency = $scope.product.currency;
            delete $scope.productDuplicate['currency'];
            LicenseProducts.update({ realm: $scope.subRealm.realm }, $scope.productDuplicate, function (data) {
                $scope.saveInProgress = false;
                $scope.makeEditable(false);
                let productData = data.body;
                productData['currency'] = currency;
                $scope.setProductDetail(productData);
                Notifications.success($translate.instant("license-pricing-delete-success"));
            }, function () {
                Notifications.error($translate.instant("license-pricing-delete-error"));
                $scope.saveInProgress = false;
            })
        });
    };

    $scope.makeAsDefault = (price) => {
        let updatedPrice = angular.copy(price);
        updatedPrice['productId'] = $scope.path[4];
        updatedPrice['tenantId'] = $scope.subRealm.id;
        updatedPrice['consider'] = "always";
        updatedPrice['pricingType'] = $scope.subRealm.attributes.hasOwnProperty('tenant_pricing_type') ? $scope.subRealm.attributes['tenant_pricing_type'] : 'default';
        UpdateLicensePricing.update({ realm: $scope.subRealm.realm, pricingId: price.priceId }, updatedPrice , function (data) {
            $scope.getProductData();
        }, function (error) {
            Notifications.error($translate.instant("license-pricing-make-default-error"));
        })
    }

    $scope.getProductData = () => {
        GetProductDetail.get({ realm: $scope.subRealm.realm, productId: $scope.path[4] }, function (data) {
            $scope.product = data;
            $scope.productCopy = angular.copy($scope.product);
            $scope.currencyFilterValue = $scope.product.currency.toLowerCase();
        }, function () {
            Notifications.error($translate.instant("license-product-fetch-error"));
        })
    }


    $scope.updateProductData = () => {
        // $scope.saveInProgress = true;
        $scope.changed = false;
        LicenseProducts.update({ realm: $scope.subRealm.realm }, $scope.product, function (data) {
            // $scope.saveInProgress = false;
            $scope.makeEditable(false);
            $scope.setProductDetail(data.body);
            $scope.changed = false;
        }, function (error) {
            Notifications.error($translate.instant("license-product-update-error"));
            // $scope.saveInProgress = false;
            $scope.changed = true;
        })
    }

    $scope.getProductData();

})

module.controller('RealmPricing', function($scope, $timeout, realm, subRealm, UpdateLicensePricing, GetLicensePricing, $location, Notifications, $translate, ApiProviders, GetProductDetail){
    $scope.subRealm = subRealm;
    $scope.realm = realm;
    $scope.isCustomPricedTenant = ($scope.subRealm.attributes && $scope.subRealm.attributes.hasOwnProperty('tenant_pricing_type') && $scope.subRealm.attributes['tenant_pricing_type'] == 'custom') ? true : false;
    $scope.isRootAccount = $scope.realm.id === $scope.rootAccountName;
    // if(!(!$scope.isRootAccount ? $scope.realm.adminRealm && $scope.realm.adminRealm === true && $scope.isCustomPricedTenant : true)){
    //     $location.path("/notfound");
    // }
    $scope.makeSideMenuClosed();
    $scope.price = {};
    $scope.priceCopy = {};
    $scope.isPriceEditable = false;
    $scope.changed = false;
    $scope.saveInProgress = false;
    $scope.searchValue = "";
    let newMetaData = {
        key:"",
        value: ""
    }
    $scope.makePriceWatch = false;
    $scope.currencySymbol;
    $scope.isMobileWView = false;
    $scope.selectedProvider = '';
    $scope.providers = [];
    $scope.product;
    $scope.loading = true;
    $scope.loaderTypeName = 'realmAttributesLoader';

    $scope.toggleSideBarShow = function () {
       var sidebarMenu = document.getElementById('setting-sidebar');
       var sidebarIcon = document.getElementById('setting-sidebar-icon');
       if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
           sidebarMenu.classList.remove("bo-setting-sidemenu");
           sidebarMenu.classList.add("bo-setting-sidemenu-show");
           sidebarIcon.classList.add('d-none');
           sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
           $scope.isMobileWView = true;
       }else{
           sidebarMenu.classList.add("bo-setting-sidemenu");
           sidebarMenu.classList.remove("bo-setting-sidemenu-show");
           sidebarIcon.classList.remove('d-none');
           sidebarIcon.classList.add('bo-setting-sidemenu-icon');
           $scope.isMobileWView = false;
       }
   }

    $scope.$watch('price', function(){
        if($scope.makePriceWatch){
            $scope.price.currencyOptions[0].amount = angular.copy($scope.price.currencyOptions[0].amount.replace(/[^0-9.]/g, ''));
            $scope.currencySymbol = getCurrencySymbol('en',  $scope.price.currencyOptions[0].currency);
            if(!angular.equals($scope.price, $scope.priceCopy) && !isObjectInArray($scope.price.metadata, newMetaData) && $scope.price.currencyOptions[0].amount.trim().length > 0){
                $scope.changed = true;
            }else{
                $scope.changed = false;
            }
        }else{
            $scope.makePriceWatch = true;
        }
    }, true);

    $scope.getPriceName = function (price) {
        priceNameArray = price.metadata && price.metadata.length > 0 ? price.metadata.filter((meta) => meta.key == 'stage' ) : '' ;
        if(priceNameArray.length > 0){
            return price.metadata.filter((meta) => meta.key == 'stage' )[0].value;
        } else {
            return price.priceId;
        }
    }

    const isObjectInArray = (arr, obj) => {
        return arr.some(item => item.key.trim() === obj.key || item.value.trim() === obj.value);
    };

    $scope.convertToNumberFormate = (index) => {
        $scope.price.metadata[index].value = angular.copy($scope.price.metadata[index].value.replace(/[^0-9.]/g, ''));
    }

    $scope.makeEditable = (boolean) => {
        $scope.isPriceEditable = boolean;
        if(!boolean){
            $scope.price = angular.copy($scope.priceCopy);
            $scope.changed = false;
        }
    }

    $scope.addMetaData = () => {
        if(!isObjectInArray($scope.price.metadata, newMetaData)){
            $scope.price.metadata.push(angular.copy(newMetaData));
        }
        $timeout(function() {
            $('#metadata-wrapper').scrollTop($('#metadata-wrapper').prop("scrollHeight"));
        }, 0);
    }

    $scope.removeMetaData = (index) => {
        $scope.price.metadata.splice(index, 1);
    }

    $scope.updatePricing = () => {
        $scope.saveInProgress = true;
        $scope.makePriceWatch = false;
        $scope.price['productId'] = $scope.path[4];
        $scope.price['tenantId'] = $scope.subRealm.id;
        $scope.price['provider'] = $scope.selectedProvider;
        $scope.price['pricingType'] = $scope.subRealm.attributes.hasOwnProperty('tenant_pricing_type') ? $scope.subRealm.attributes['tenant_pricing_type'] : 'default';
        $scope.price.currencyOptions[0].amount = +$scope.price.currencyOptions[0].amount.trim();
        UpdateLicensePricing.update({ realm: $scope.subRealm.realm, pricingId: $scope.price.priceId }, $scope.price, function (data) {
            $scope.saveInProgress = false;
            $scope.isPriceEditable = false;
            $scope.changed = false;
            $scope.price = data.body;
            $scope.price.currencyOptions[0].amount = $scope.price.currencyOptions[0].amount.toString();
            $scope.priceCopy = angular.copy($scope.price);
            Notifications.success($translate.instant("license-pricing-update-success"));
        }, function (error) {
            Notifications.error($translate.instant("license-pricing-update-error"));
            $scope.saveInProgress = false;
        })
    }

    $scope.getPricingDetail = () => {
        $scope.loading = true;
        GetLicensePricing.get({ realm: $scope.subRealm.realm, pricingId: $scope.path[6], priceType: $scope.subRealm.attributes.hasOwnProperty('tenant_pricing_type') ? $scope.subRealm.attributes['tenant_pricing_type'] : 'default', provider: $scope.selectedProvider }, function (data) {
            $scope.price = data;
            $scope.price.currencyOptions[0].amount = $scope.price.currencyOptions[0].amount.toString();
            $scope.priceCopy = angular.copy($scope.price);
            $scope.currencySymbol = getCurrencySymbol('en',  $scope.price.currencyOptions[0].currency);
            $scope.loading = false;
        }, function () {
            Notifications.error($translate.instant("license-pricing-get-error"));
            $scope.loading = false;
        })
    }

    $scope.$watch('selectedProvider', function(){
        if(!$scope.loading){
            $scope.getPricingDetail();
        }
    }, true);

    $scope.getProductData = () => {
        GetProductDetail.get({ realm: $scope.subRealm.realm, productId: $scope.path[4] }, function (productdata) {
            $scope.product = productdata;
            if(productdata.externalId == 'bo_tran'){
                ApiProviders.query({realm: $scope.subRealm.id},function(data){
                  if(data.length > 0){
                      $scope.selectedProvider=data[0];
                  }
                  $scope.providers = data;
                  $scope.getPricingDetail();
                }, function(data){

              });
            }else{
                $scope.getPricingDetail();
            }
        })
    }

    $scope.getProductData();
})

module.controller('RealmAccessKeyCtrl', function($scope, $route, $translate, realm, accessKey, Dialog, Notifications, AccessKey, RotateAccessKey){
    $scope.realm = realm;
    $scope.accessKey = accessKey;
    $scope.accessKeyCopy = angular.copy($scope.accessKey);
    $scope.expiryTime = new Date(Math.floor($scope.accessKey.expiryDate / 1000) * 1000);
    $scope.copyTooltipTxt = $translate.instant('copy');
    $scope.currentDateObject = new Date();
    $scope.currentDateObject.setDate($scope.currentDateObject.getDate() + 1)
    $scope.currentDateTime = $scope.currentDateObject.toISOString().slice(0, 16);
    $scope.changed = false;
    $scope.isSecretVisible = false;
    $scope.showSecretHelperTxt = $translate.instant('show-secret');

    $scope.isMobileWView = false;
    $scope.toggleSideBarShow = function () {
       var sidebarMenu = document.getElementById('setting-sidebar');
       var sidebarIcon = document.getElementById('setting-sidebar-icon');
       if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
           sidebarMenu.classList.remove("bo-setting-sidemenu");
           sidebarMenu.classList.add("bo-setting-sidemenu-show");
           sidebarIcon.classList.add('d-none');
           sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
           $scope.isMobileWView = true;
       }else{
           sidebarMenu.classList.add("bo-setting-sidemenu");
           sidebarMenu.classList.remove("bo-setting-sidemenu-show");
           sidebarIcon.classList.remove('d-none');
           sidebarIcon.classList.add('bo-setting-sidemenu-icon');
           $scope.isMobileWView = false;
       }
   }

    $scope.makeSideMenuClosed();

    $scope.getAccessKey = function () {
        AccessKey.get({ realm: $scope.realm.realm, accessKey: $route.current.params.accessKey }, function (data) {
            $scope.accessKey = data;
            $scope.expiryTime = new Date(Math.floor($scope.accessKey.expiryDate / 1000) * 1000);
        }, function (data) {
            Notifications.error($translate.instant("access-key-fetch-error"));
        })
    }

    $scope.$watch('accessKey', function () {
        if (!angular.equals($scope.accessKey, $scope.accessKeyCopy) && $scope.accessKey.name.trim().length > 0) {
            $scope.changed = true;
        }else{
            $scope.changed = false;
        }
    }, true)

    $scope.$watch('expiryTime', function () {
        if(!$scope.expiryTime) {
            Notifications.error($translate.instant("invalid-expiry-time"));
            $scope.expiryTime = new Date(Math.floor($scope.accessKeyCopy.expiryDate / 1000) * 1000);
        }else {
            var datetimeInput = $scope.expiryTime;
            var datetime = new Date(Math.floor(datetimeInput / 1000) * 1000);
            $scope.accessKey.expiryDate = datetime.getTime();
        }
    }, true)

    $scope.save = function () {
        let accessKeyUpdateValue = {
            "name": $scope.accessKey.name,
            "enabled": $scope.accessKey.enabled,
            "expiryDate": $scope.accessKey.expiryDate,
            "permissions": $scope.accessKey.permissions,
            "sandboxMode": $scope.accessKey.sandboxMode,
            "sandboxSecret": $scope.accessKey.sandboxSecret,
        }
        AccessKey.update({ realm: $scope.realm.realm, accessKey: $route.current.params.accessKey }, accessKeyUpdateValue, function (data) {
            Notifications.success($translate.instant("access-key-update-success"));
            $scope.accessKey = data;
            $route.reload();
        }, function (data) {
            Notifications.error($translate.instant("access-key-update-error"));
        })
    }

    $scope.rotateSecret = function () {
        Dialog.confirm($translate.instant("rotate-secret-title"), $translate.instant("are-you-sure-to-rotate-secret"), function() {
            RotateAccessKey.get({ realm: $scope.realm.realm, accessKey: $route.current.params.accessKey }, function(data) {
                Notifications.success($translate.instant("access-key-rotate-success"));
                $scope.accessKey = data;
            }, function (data) {
                Notifications.error($translate.instant("access-key-rotate-error"));
            })
        })
    }

    $scope.copyToClipboard = function (value) {
        $scope.copyTooltipTxt = $translate.instant('copied');
        var $temp = $("<input>");
        $("body").append($temp);
        $temp.val(value).select();
        document.execCommand($translate.instant('copy'));
        setInterval(() => $scope.copyTooltipTxt = $translate.instant('copy'), 3000);
        $temp.remove();
    };

    $scope.showHide = function () {
        const input = $('#accessKeySecret');
        if(!$scope.isSecretVisible){
            Dialog.confirm($translate.instant("show-access-key-secret-title"), $translate.instant("are-you-sure-to-show-secret"), function() {
                input.prop("type", "text");
                $scope.isSecretVisible = true;
                $scope.showSecretHelperTxt = $translate.instant('hide-secret');
            });
        }else{
            input.prop("type", "password");
            $scope.isSecretVisible = false;
            $scope.showSecretHelperTxt = $translate.instant('show-secret');
        }
    }

    $scope.reset = function () {
        $scope.accessKey = angular.copy($scope.accessKeyCopy);
        $scope.expiryTime = new Date(Math.floor($scope.accessKeyCopy.expiryDate / 1000) * 1000);
    }

})

module.controller('RealmSpecialAttributesCtrl', function ($scope, $rootScope, realm, Dialog, Notifications, Session, RealmSpecialAttributesRotate,RealmSpecialAttributesGenerate, RealmSpecialAttributesUpdate, RealmSpecialAttributes, DeviceApplicationDialog, GetDeviceApplications, $location, $route, $translate) {
    $scope.realm = angular.copy(realm);
    $scope.specialTabName = '';
    $scope.workspaceSelectedAttributes = {};
    $scope.specialAttributes = [];
    $scope.specialAttributesCopy = [];
    $scope.tooltipTxt = $translate.instant('copy');
    $scope.loaderTypeName = 'realmAttributesLoader';
    $scope.loading = true;
    $scope.changed = false;
    $scope.isAccountCreator = auth.user.isAccountCreator;
    $scope.stage = 1;
    $scope.useCustomSupport = false;
    $scope.resourceUrl = resourceUrl;
    $scope.deviceApplication = {};
    $scope.appType = '';
    $scope.appName = '';
    $scope.platform = '';
    $scope.itemsPerPage = Session.getItem("boDeviceAppCount") ? Session.getItem("boDeviceAppCount") : 5;
    $scope.currentPage = 1;
    $scope.pagination = {
        firstResult: 0,
        maxResults: $scope.itemsPerPage
    };

    $scope.makeSideMenuClosed();

    $rootScope.$on('$routeChangeStart', function () {
        $scope.loading = true;
      });

    $scope.changePage = function(pageNumber) {
        $scope.currentPage = pageNumber;
        $scope.pagination.firstResult = pageNumber === 1 ? 0 : (pageNumber - 1) * $scope.itemsPerPage;
        $scope.pagination.maxResults = $scope.itemsPerPage;
        $scope.getDeviceList();
    };

    $scope.resetFilter = function() {
        $scope.appName = '';
        $scope.platform = '';
        $scope.currentPage = 1;
        $scope.pagination.firstResult = 0;
        $scope.pagination.maxResults = $scope.itemsPerPage;
        $scope.getDeviceList();
    };


    $scope.pageCountChanged = function () {
        Session.setItem("boDeviceAppCount", $scope.itemsPerPage);
        $scope.changePage(1);
    };

    if(realm.realm){
        RealmSpecialAttributes.query({ realm: realm.realm }, function (data) {
            $scope.loading = false;
            $scope.specialAttributes = angular.copy(data);
            if ($scope.specialAttributes) {
                let order = 1;
                $scope.specialAttributes.forEach(function (data) {
                    data.attributes.forEach(function (attr) {
                        attr.order = order++;
                        if (attr.json && attr.attributeValue) {
                            var obj = JSON.parse(attr.attributeValue);
                            var prettyValue = JSON.stringify(obj ,undefined, 4);
                            attr.attributeValue= prettyValue ? prettyValue : '';
                        } else if (attr.toggle) {
                            attr.order = 0;
                            attr.attributeValue = attr.attributeValue == 'true' ? true : false;
                        } else {
                            attr.attributeValue = attr.attributeValue ? attr.attributeValue : '';
                        }
                    })
                  data.hideTab = !data.attributes || data.attributes.every(attr => !attr.showInSubTenant);
                });
            }
            $scope.specialAttributesCopy = angular.copy($scope.specialAttributes);
        }, function (data) {
            $scope.loading = false;
        });
    }

    $scope.checkValidInput = function () {
        for ( j =0 ; j<$scope.specialAttributes.length; j++ ) {
            if($scope.specialAttributes[j].tabName.replaceAll(' ','-').toLowerCase() == $scope.path[3]){
                for ( i =0 ; i<$scope.specialAttributes[j].attributes.length; i++ ) {
                    if($scope.specialAttributes[j].attributes[i].json && ($scope.specialAttributes[j].attributes[i].attributeValue && $scope.specialAttributesCopy[j].attributes[i].attributeValue && $scope.specialAttributes[j].attributes[i].attributeValue.trim())){
                        try{
                            var obj = JSON.parse($scope.specialAttributes[j].attributes[i].attributeValue);
                            var prettyValue = JSON.stringify(obj);
                            var objCopy = JSON.parse($scope.specialAttributesCopy[j].attributes[i].attributeValue);
                            var prettyValueCopy = JSON.stringify(objCopy);
                            if(prettyValue !== prettyValueCopy){
                                return true;
                            }
                        }catch{
                            return false;
                        }
                    }
                    else if($scope.specialAttributes[j].attributes[i].toggle) {
                        return true;
                    }
                    else if($scope.specialAttributes[j].attributes[i].attributeValue.trim() !==  $scope.specialAttributesCopy[j].attributes[i].attributeValue){
                        return true;
                    }
                }
            }
        }
    }

    $scope.getDeviceList = function () {
        $scope.appType = $scope.path[3] == 'desktop-apps' ? 'desktop' : 'mobile' ;
        $scope.platformOption = $scope.appType == "mobile" ? ['ios', 'android'] : ['windows', 'macos'];
        $scope.isListLoading = true;
        GetDeviceApplications.get({ realmId: $scope.realm.id, appType: $scope.appType, first: $scope.pagination.firstResult, max: $scope.pagination.maxResults, appName: $scope.appName, platform: $scope.platform }, function (data) {
            $scope.deviceApplication = data;
            $scope.isListLoading = false;
        }, function (data) {
            Notifications.error($translate.instant("device-app-fetch-error"))
            $scope.isListLoading = false;
        })
    }

    if($scope.path[3] == 'desktop-apps' || $scope.path[3] == 'mobile-apps' ) {
        $scope.getDeviceList();
    }

    $scope.createDeviceApp = function () {
        DeviceApplicationDialog.open($scope.realm, 'create', {}, $scope.appType, $scope.itemsPerPage, function (app) {
            $scope.deviceApplication = app;
            $scope.resetFilter();
        })
    }

    $scope.viewDeviceApp = function (app) {
        DeviceApplicationDialog.open($scope.realm, 'edit', app, $scope.appType,  $scope.itemsPerPage, function (app) {
            $scope.deviceApplication = app;
            $scope.resetFilter();
        })
    }

    $scope.$watch('specialAttributes', function () {
        if(!angular.equals($scope.specialAttributes, $scope.specialAttributesCopy) && $scope.checkValidInput()){
            $scope.changed = true;
        }else{
            $scope.changed = false;
        }
    }, true);

    $scope.handleToggle = (state, providerId)=>{
        if(providerId === "support_custom_toggle") {
            $scope.useCustomSupport = state;
            // if (state) $scope.setSupport('url');
            // else $scope.setSupport('')
        }
    }

    $scope.update = function (tab) {
        $scope.stage = 2;
        $scope.specialAttributes.forEach(function (data) {
            if($scope.path[3] === data.tabName.replaceAll(' ','-').toLowerCase()){
                data.attributes.forEach(function (attr) {
                    if(!attr.readOnly && attr.canBeUpdated){
                        if(attr.json && attr.attributeValue){
                            try{
                                var obj = JSON.parse(attr.attributeValue);
                                var prettyValue = JSON.stringify(obj);
                                $scope.workspaceSelectedAttributes[attr.attributeName] = prettyValue ?? '';
                            }catch{
                                Notifications.error('Please enter the valid data');
                                $scope.changed = false;
                                $scope.stage = 1;
                                return;
                            }
                        }else{
                            $scope.workspaceSelectedAttributes[attr.attributeName] = attr.attributeValue ?? '';
                        }
                    }
                })
            }
        });
        if($scope.changed){
            Dialog.confirm(
                $translate.instant('update'),
                $translate.instant('realm-attributes-update'),
                function () {
                    RealmSpecialAttributesUpdate.send(
                        {
                            realm: realm.realm,
                            tabName: tab
                        },
                        $scope.workspaceSelectedAttributes
                        ,
                        function (data) {
                            if (data.status === 400) {
                                Notifications.error($translate.instant(data.body.message));
                            } else {
                                Notifications.success($translate.instant(data.body.message));
                            }
                            $route.reload();
                            $scope.stage = 1;
                        },
                        function (data) {
                            Notifications.error(data.body.message);
                            $scope.stage = 1;
                        }
                    );
                },
                function(){
                    $scope.stage = 1;
                }
            );
        }
    };

    $scope.reset = function () {
        $scope.specialAttributes = angular.copy($scope.specialAttributesCopy);
    }

    $scope.isMobileWView = false;
     $scope.toggleSideBarShow = function () {
        var sidebarMenu = document.getElementById('setting-sidebar');
        var sidebarIcon = document.getElementById('setting-sidebar-icon');
        if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
            sidebarMenu.classList.remove("bo-setting-sidemenu");
            sidebarMenu.classList.add("bo-setting-sidemenu-show");
            sidebarIcon.classList.add('d-none');
            sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
            $scope.isMobileWView = true;
        }else{
            sidebarMenu.classList.add("bo-setting-sidemenu");
            sidebarMenu.classList.remove("bo-setting-sidemenu-show");
            sidebarIcon.classList.remove('d-none');
            sidebarIcon.classList.add('bo-setting-sidemenu-icon');
            $scope.isMobileWView = false;
        }
    }

    $scope.rotateSecret = function (providerId) {
        Dialog.confirm(
            $translate.instant("rotate-secret-title"),
            $translate.instant("are-you-sure-to-rotate-secret"),
            function () {
                RealmSpecialAttributesRotate.send(
                    {
                        realm: realm.realm,
                        providerId: providerId
                    },
                    function (data) {
                        $route.reload();
                        Notifications.success(
                          $translation.instant(data.body.message)
                        );
                    },
                    function (data) {
                        Notifications.error(data.body.message);
                    }
                );
            }
        );
    };

    $scope.generateSecret = function (providerId) {
        Dialog.confirm(
            $translate.instant("generate-secret-title"),
            $translate.instant("are-you-sure-to-generate-secret"),
            function () {
                RealmSpecialAttributesGenerate.send(
                    {
                        realm: realm.realm,
                        providerId: providerId
                    },
                    function (data) {
                        $route.reload();
                        Notifications.success($translation.instant(data.body.message));
                    },
                    function (data) {
                        Notifications.error(data.body.message);
                    }
                );
            }
        );
    };

    $scope.copyToClipboard = function (inputElement) {
        $scope.tooltipTxt = $translate.instant('copied');
        var $temp = $("<input>");
        $("body").append($temp);
        $temp.val($('#' + inputElement).val()).select();
        document.execCommand("copy");
        setInterval(() => $scope.tooltipTxt = $translate.instant('copy'), 3000);
        $temp.remove();
    };

    $scope.showHide = function (inputElement) {
        const input = $('#' + inputElement);
        if (input.prop("type") === 'password')
            input.prop("type", "text");
        else
            input.prop("type", "password");
    };

    $scope.isValidCopy = function (id) {
        const value = $("#" + id).val();
        return value && value.trim();
    };

    $scope.enableSaveButton = function (id) {
        const value = $("#" + id).val();
        const emptySpaceLen = value.replace(/\s/g, '').length;
        const saveIcon = $("#" + id + "-saveIcon");

        if (value && !emptySpaceLen) {
            saveIcon.attr("disabled", true);
        } else {
            saveIcon.removeAttr("disabled");
        }
    };

    $scope.prettyShow = function (specialAttribute, attributeId, isJson) {
        if (isJson) {
            $scope.specialAttributes.map((specialAttr) => {
                if(specialAttr.tabName === specialAttribute){
                    specialAttr.attributes.map((attribute) => {
                        if(attribute.providerId === attributeId && attribute.attributeValue.trim()){
                            try{
                                var obj = JSON.parse(attribute.attributeValue);
                                var pretty = JSON.stringify(obj, undefined, 4);
                                attribute.attributeValue = pretty;
                            }catch{
                                Notifications.error('Please enter the valid data');
                                $scope.changed = false;
                            }
                        }
                    })
                }
            })
        }
    }

    $scope.resetCopy = function () {
        $scope.tooltipTxt = $translate.instant('copy');
    };

});

function genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, url, ConfirmExitDialog, $location, TimeUnit2, $translate, onboardingFlows = [], loginFlows = [], recoveryFlows = [], actions = [], cibaPolicies = [], actionPolicies = [], directGrantFlows = []) {
    $scope.realm = angular.copy(realm);
    $scope.serverInfo = serverInfo;
    $scope.actionTokenProviders = $scope.serverInfo.providers.actionTokenHandler.providers;
    $scope.realm.attributes = realm.attributes;
    $scope.onboardingFlows = onboardingFlows;
    $scope.loginFlows = loginFlows;
    $scope.recoveryFlows = recoveryFlows;
    $scope.actions = actions;
    $scope.mfaProviders = realm.mfaProviders;
    $scope.cibaPolicies = cibaPolicies;
    $scope.actionPolicies = actionPolicies;
    $scope.allPolicies = actionPolicies.concat(cibaPolicies);
    $scope.configuredProviders = angular.copy(realm.identityProviders);
    if ($translate) {
        $scope.createSocialConnDesc = $translate.instant(
            "no-social-connection-sec-desc",
            { link: "#/realms/" + realm.realm + "/connections/social/create" }
        );
        $scope.createMFADesc = $translate.instant("no-mfa-sec-desc ", {
            link: "#/realms/" + realm.realm + "/connections/mfa/create ",
        });
    }
    $scope.directGrantFlows = directGrantFlows;

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;
    $scope.openRedirect = false;

    $scope.$watch('realm', function () {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    function updateActionTokenAttribute() {
        let userInviteLinkExpiry = $scope.realm.attributes.hasOwnProperty("actionTokenGeneratedByUserLifespan.user-invite-link") ?
            $scope.realm.attributes["actionTokenGeneratedByUserLifespan.user-invite-link"]
            : $scope.realm.actionTokenGeneratedByUserLifespan;
        if (TimeUnit2) {
            $scope.actionTokenAttribute =
                TimeUnit2.asUnit(userInviteLinkExpiry);
            if (
                $scope.realm.userInviteWay === "link" &&
                !$scope.realm.attributes.hasOwnProperty(
                    "actionTokenGeneratedByUserLifespan.user-invite-link"
                )
            ) {
                $scope.realm.attributes[
                    "actionTokenGeneratedByUserLifespan.user-invite-link"
                ] = $scope.actionTokenAttribute.toSeconds().toString();
                oldCopy.attributes[
                    "actionTokenGeneratedByUserLifespan.user-invite-link"
                ] = $scope.actionTokenAttribute.toSeconds().toString();
            } else if (
                $scope.realm.userInviteWay === "otp" &&
                !oldCopy.attributes.hasOwnProperty(
                    "actionTokenGeneratedByUserLifespan.user-invite-link"
                ) &&
                $scope.realm.attributes.hasOwnProperty(
                    "actionTokenGeneratedByUserLifespan.user-invite-link"
                )
            ) {
                delete $scope.realm.attributes[
                    "actionTokenGeneratedByUserLifespan.user-invite-link"
                ];
            }
        }
    }

    $scope.$watch('realm.userInviteWay', function () {
        updateActionTokenAttribute();
    }, true);

    $scope.$watch('actionTokenAttribute', function () {
        if ($scope.realm.userInviteWay === 'link' && $scope.actionTokenAttribute) {
            $scope.realm.attributes['actionTokenGeneratedByUserLifespan.user-invite-link'] = $scope.actionTokenAttribute.toSeconds().toString();
        }
    }, true);

    $scope.resetToDefaultToken = function (actionTokenId) {
        $scope.realm.attributes['actionTokenGeneratedByUserLifespan.user-invite-link'] = oldCopy.attributes['actionTokenGeneratedByUserLifespan.user-invite-link'];
        $scope.actionTokenAttribute = TimeUnit2.asUnit($scope.realm.attributes['actionTokenGeneratedByUserLifespan.user-invite-link']);
    };

    $scope.save = function () {
        if ($scope.actionTokenAttribute && !$scope.actionTokenAttribute.time) {
            Notifications.error($translate.instant("link-expiration-value-error"));
        } else {
            var realmCopy = angular.copy($scope.realm);
            $scope.changed = false;
            Realm.update({ id: oldCopy.realm }, realmCopy, function () {
                $scope.openRedirect = true;
                $route.reload();
                Notifications.success($translate.instant("changes-saved-to-realm"));
            });
        }
    };

    $scope.reset = function () {
        $scope.realm = angular.copy(oldCopy);
        if ($scope.realm.userInviteWay === 'link') {
            updateActionTokenAttribute();
        }
        $scope.changed = false;
    };

    $scope.cancel = function () {
        $scope.openRedirect = true;
        $route.reload();
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (ConfirmExitDialog && $location) {
            if (!$scope.openRedirect && $scope.changed) {
                showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
            }
        }
    });
}

module.controller('DefenseHeadersCtrl', function ($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, ConfirmExitDialog, $location) {
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/defense/headers", ConfirmExitDialog, $location);
});

module.controller('RealmOnboardingSettingsCtrl', function ($scope, Current, Client, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, ConfirmExitDialog, $location, TimeUnit2, $translate) {
    $scope.serverInfo = serverInfo;
    $scope.clients = [];
    $scope.initClientStreams = function () {
        Client.query({ realm: $route.current.params.realm }, function (data) {
            $scope.clients = data.filter(x => x.overrideAuthentication && (x.attributes.inviteOnly === "true" || x.attributes.inviteOnly === true));
        });
    };
    $scope.toggleSideBarShow = function () {
        var sidebarMenu = document.getElementById('setting-sidebar');
        var sidebarIcon = document.getElementById('setting-sidebar-icon');
        if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
            sidebarMenu.classList.remove("bo-setting-sidemenu");
            sidebarMenu.classList.add("bo-setting-sidemenu-show");
            sidebarIcon.classList.add('d-none');
            sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
            $scope.isMobileWView = true;
        }else{
            sidebarMenu.classList.add("bo-setting-sidemenu");
            sidebarMenu.classList.remove("bo-setting-sidemenu-show");
            sidebarIcon.classList.remove('d-none');
            sidebarIcon.classList.add('bo-setting-sidemenu-icon');
            $scope.isMobileWView = false;
        }
    }
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/onboarding-settings", ConfirmExitDialog, $location, TimeUnit2, $translate);
});


module.controller('RealmLoginSettingsCtrl', function ($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, ConfirmExitDialog, $location, TimeUnit2, $translate) {
    // KEYCLOAK-5474: Make sure duplicateEmailsAllowed is disabled if loginWithEmailAllowed
    $scope.serverInfo = serverInfo;
    $scope.$watch('realm.loginWithEmailAllowed', function () {
        if ($scope.realm.loginWithEmailAllowed) {
            $scope.realm.duplicateEmailsAllowed = false;
        }
    });

    $scope.toggleSideBarShow = function () {
        var sidebarMenu = document.getElementById('setting-sidebar');
        var sidebarIcon = document.getElementById('setting-sidebar-icon');
        if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
            sidebarMenu.classList.remove("bo-setting-sidemenu");
            sidebarMenu.classList.add("bo-setting-sidemenu-show");
            sidebarIcon.classList.add('d-none');
            sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
            $scope.isMobileWView = true;
        }else{
            sidebarMenu.classList.add("bo-setting-sidemenu");
            sidebarMenu.classList.remove("bo-setting-sidemenu-show");
            sidebarIcon.classList.remove('d-none');
            sidebarIcon.classList.add('bo-setting-sidemenu-icon');
            $scope.isMobileWView = false;
        }
    }

    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/login-settings", ConfirmExitDialog, $location, TimeUnit2, $translate);
});

module.controller('RealmOtpPolicyCtrl', function ($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications) {
    $scope.optionsDigits = [6, 8];

    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/authentication/otp-policy");
});



module.controller('RealmThemeCtrl', function ($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications) {
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/theme-settings");

    $scope.supportedLocalesOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': []
    };

    updateSupported();

    function localeForTheme(type, name) {
        name = name || 'base';
        for (var i = 0; i < serverInfo.themes[type].length; i++) {
            if (serverInfo.themes[type][i].name == name) {
                return serverInfo.themes[type][i].locales || [];
            }
        }
        return [];
    }

    function updateSupported() {
        if ($scope.realm.internationalizationEnabled) {
            var accountLocales = localeForTheme('account', $scope.realm.accountTheme);
            var loginLocales = localeForTheme('login', $scope.realm.loginTheme);
            var emailLocales = localeForTheme('email', $scope.realm.emailTheme);

            var supportedLocales = [];
            for (var i = 0; i < accountLocales.length; i++) {
                var l = accountLocales[i];
                if (loginLocales.indexOf(l) >= 0 && emailLocales.indexOf(l) >= 0) {
                    supportedLocales.push(l);
                }
            }

            $scope.supportedLocalesOptions.tags = supportedLocales;

            if (!$scope.realm.supportedLocales) {
                $scope.realm.supportedLocales = supportedLocales;
            } else {
                for (var i = 0; i < $scope.realm.supportedLocales.length; i++) {
                    if (supportedLocales.indexOf($scope.realm.supportedLocales[i]) == -1) {
                        $scope.realm.supportedLocales = supportedLocales;
                    }
                }
            }

            if (!$scope.realm.defaultLocale || supportedLocales.indexOf($scope.realm.defaultLocale) == -1) {
                $scope.realm.defaultLocale = 'en';
            }
        }
    }

    $scope.$watch('realm.loginTheme', updateSupported);
    $scope.$watch('realm.accountTheme', updateSupported);
    $scope.$watch('realm.emailTheme', updateSupported);
    $scope.$watch('realm.internationalizationEnabled', updateSupported);
});

module.controller('RealmCacheCtrl', function ($scope, realm, RealmClearUserCache, RealmClearRealmCache, RealmClearKeysCache, RealmClearReportQueue, Notifications, $translate) {
    $scope.realm = angular.copy(realm);

    $scope.clearUserCache = function () {
        RealmClearUserCache.save({ realm: realm.realm }, function () {
            Notifications.success($translate.instant("user-cache-cleared"));
        });
    };

    $scope.clearRealmCache = function () {
        RealmClearRealmCache.save({ realm: realm.realm }, function () {
            Notifications.success($translate.instant("realm-cache-cleared"));
        });
    };

    $scope.clearKeysCache = function () {
        RealmClearKeysCache.save({ realm: realm.realm }, function () {
            Notifications.success($translate.instant("public-keys-cache-cleared"));
        });
    };

    $scope.clearReportQueue = function () {
        RealmClearReportQueue.save({ realm: realm.realm }, function () {
            Notifications.success($translate.instant("report-queue-cleared"));
        });
    };

    $scope.clearReportQueue = function () {
        RealmClearReportQueue.save({ realm: realm.realm }, function () {
            Notifications.success($translate.instant("report-queue-cleared"));
        });
    };


});

module.controller('RealmBrandingDetailCtrl', function ($scope, Current, Realm, realm, $location, serverInfo, $http, $route, Dialog, Notifications, Brandings, BrandingConfig, branding, ComponentUtils, ConvertImageToUrl, $translate, $rootScope, InputDialog, BrandingDefault, $timeout) {
    if (!serverInfo?.brandInfo?.brandingEnabled) {
        $location.path("/notfound");
    }
    $scope.isLoaded = false;
    $scope.loaderTypeName = 'formPreviewLoader';
    $scope.previewLoaderTypeName = 'themePreviewLoader';
    const sideMenuSlider = document.querySelector("#body-container");
    sideMenuSlider.classList.add("custom-menu-true");
    $scope.realm = realm;
    $scope.branding = angular.copy(branding);
    $scope.serverInfo = serverInfo;
    $scope.changed = false;
    $scope.config = {};
    $scope.header = [];
    $scope.layout = [];
    $scope.section = [];
    $scope.form = [];
    $scope.footer = [];
    $scope.currentSection = "Layout";
    $scope.isThemeBgImageValid = true;
    $scope.isBodyBgImageValid = true;
    $scope.toggleOpenCloseIcon = function () {
        $('#toggle-parent').toggle('toggle-open');
        document.getElementById("nav-bar").classList.toggle("toggle-open");
    };
    $scope.stage = 1;
    BrandingConfig.query({ realm: realm.realm }, function (response) {
        $scope.config = response;
        ComponentUtils.convertAllMultivaluedStringValuesToList($scope.config, $scope.branding.attributes);
        ComponentUtils.convertNumberTypeStringValueToInt($scope.config, $scope.branding.attributes);
        $scope.updateMenuPreview();
        $scope.groupProp();
        $scope.oldVal = angular.copy($scope.branding.attributes);
        $scope.isLoaded = true;
    });
    $scope.groupProp = function () {
        for (const prop of $scope.config) {
            if (prop.groupBy === 'layout') {
                $scope.layout.push(prop);
                continue;
            }
            if (prop.groupBy === 'section') {
                $scope.section.push(prop);
                continue;
            }
            if (prop.groupBy === 'form') {
                $scope.form.push(prop);
                continue;
            }
            if (prop.groupBy === 'footer') {
                $scope.footer.push(prop);
                continue;
            }
        }
    };
    if ($scope.branding.attributes.bodyBgType !== 'Image') {
        document.documentElement.style.setProperty('--body-bg-img', '');
    }
    if ($scope.branding.attributes.themeLayoutBgType !== 'Image') {
        document.documentElement.style.setProperty('--section-bg-image', '');
    }

    $rootScope.$on("imageUpdated", function (event, data, modelName) {
        if (data.isChanged) {
            $scope.branding.attributes[data.modelName] = data.name;
            $rootScope.$apply(function () {
                $scope.changed = true;
            });
        }
    });

    const setBgImage = (url) => {
        $scope.bodyBackgroundColor = "#000000";
        document.documentElement.style.setProperty('--body-bg-img', 'url(' + url + ')');
    };

    const clearBgImageState = () => {
        $scope.bodyBackgroundImage = "";
        $scope.branding.attributes.bodyBgImage = "";
        $scope.branding.attributes.bodyBgImageFile = "";
        document.documentElement.style.setProperty('--body-bg-img', '');
    };

    const setThemeLayoutImage = (url) => {
        document.documentElement.style.setProperty('--section-bg-image', 'url(' + url + ')');
    };

    // Branding BG IMAGE State
    $scope.$watch("branding.attributes.bodyBgImage", function () {
        updateBgImage();
    }, true);

    // Branding BG COLOR State
    $scope.$watch("branding.attributes.bodyBgColor", function () {
        updateBgColor();
    }, true);

    // Branding BG Type State
    $scope.$watch("branding.attributes.bodyBgType", function () {
        if ($scope.branding.attributes.bodyBgType === 'Image') {
            updateBgImage();
        } else {
            updateBgColor();
        }
    }, true);

    // Branding Section BG IMAGE State
    $scope.$watch("branding.attributes.themeLayoutBgImage", function () {
        updateSectionBgImage();
    }, true);

    $scope.$watch("branding.attributes", function () {
        if($scope.branding.attributes.fontPreference == "custom" && $scope.branding.attributes.customFontPreference == "file" ){
            var customFontUrl = $scope.branding.attributes.hasOwnProperty('customFontFile') ? angular.copy($scope.branding.attributes.customFontFile) : '';
            $scope.branding.attributes.customFontLink = '';
            if(customFontUrl.trim().length == 0){
                $scope.branding.attributes.bodyFont = 'sans-serif';
                return;
            }
            if(customFontUrl.startsWith('http')){
                const existingStyle = document.querySelector('style[data-font="MyCustomFontFile"]');
                if (existingStyle) {
                    existingStyle.remove();
                }
                const style = document.createElement('style');
                style.setAttribute('data-font', 'MyCustomFontFile');
                style.innerHTML = `
                    @font-face {
                        font-family: 'MyCustomFont';
                        src: url(${customFontUrl});
                        font-display: block;
                    }
                `;
                $scope.branding.attributes.bodyFont = 'MyCustomFont';
                document.head.appendChild(style);
                $("#customFontFile-fileName").val(customFontUrl.split('/').pop());
            }
        } else if ($scope.branding.attributes.fontPreference == "custom" && $scope.branding.attributes.customFontPreference == "link"){
            const customFontLink = $scope.branding.attributes.hasOwnProperty('customFontLink') ? angular.copy($scope.branding.attributes.customFontLink) : '';
            $scope.branding.attributes.customFontFile = '';
            if(customFontLink.trim().length == 0){
                $scope.branding.attributes.bodyFont = 'sans-serif';
                return;
            }
            const existingLink = document.querySelector('link[data-font="MyCustomFontLink"]');
            if (existingLink) {
                existingLink.remove();
            }
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = customFontLink;
            link.setAttribute('data-font', 'MyCustomFontLink');
            document.head.appendChild(link);
            var customFontFileUrl = new URL(customFontLink);
            var fontFamilyParam = customFontFileUrl.searchParams.get('family');
            if (fontFamilyParam) {
                var fontFamily = fontFamilyParam.split(':')[0];
                $scope.branding.attributes.bodyFont = fontFamily.replace(/\+/g, ' ');
            }
        } else if($scope.branding.attributes.fontPreference == "default"){
            var availableDefaultFonts = ['Avenir Next W01', 'Proxima Nova W01', 'Rubik', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'Inter'];
            if(!availableDefaultFonts.includes($scope.branding.attributes.bodyFont)){
                $scope.branding.attributes.bodyFont = '';
            }
            $scope.branding.attributes.customFontLink = '';
            $scope.branding.attributes.customFontFile = '';
        }
    }, true);


    const updateBgImage = () => {
        if ($scope.branding.attributes.bodyBgType === 'Image' && $scope.branding.attributes.bodyBgImage &&
            $scope.branding.attributes.bodyBgImageFile?.name !== $scope.branding.attributes.bodyBgImage &&
            $scope.branding.attributes.bodyBgImage != " ") {
            $("#bodyBgImage").val(null);
            $scope.branding.attributes.bodyBgImageFile = "";
            $scope.bodyBackgroundImage = $scope.branding.attributes.bodyBgImage;
            setBgImage($scope.branding.attributes.bodyBgImage);
        }
        else if ($scope.branding.attributes.bodyBgType === 'Image' && $scope.branding.attributes.bodyBgImageFile) {
            setBgImage(window.URL.createObjectURL($scope.branding.attributes.bodyBgImageFile));
            $scope.bodyBackgroundImage = "";
        }
        else if ($scope.branding.attributes.bodyBgType === 'Image' && !$scope.branding.attributes.bodyBgImage) {
            if ($scope.branding.attributes.bodyBgImage != " ") {
                $scope.branding.attributes.bodyBgImage = " ";
            }
            const defaultImage = resourceUrl + '/themes/img/bg-about.png';
            $scope.bodyBackgroundImage = defaultImage;
            setBgImage(defaultImage);
        }
        $scope.updateMenuPreview();
    };

    const updateSectionBgImage = () => {
        if ($scope.branding.attributes.themeLayoutBgType === 'Image' && $scope.branding.attributes.themeLayoutBgImage &&
            $scope.branding.attributes.themeLayoutBgImageFile?.name !== $scope.branding.attributes.themeLayoutBgImage &&
            $scope.branding.attributes.themeLayoutBgImage != " ") {
            $scope.branding.attributes.themeLayoutBgImageFile = "";
            $scope.themeLayoutBgImage = $scope.branding.attributes.themeLayoutBgImage;
            setThemeLayoutImage($scope.branding.attributes.themeLayoutBgImage);
            $("#themeLayoutBgImage").val(null);
        }
        else if ($scope.branding.attributes.themeLayoutBgType === 'Image' && $scope.branding.attributes.themeLayoutBgImageFile) {
            setThemeLayoutImage(window.URL.createObjectURL($scope.branding.attributes.themeLayoutBgImageFile));
        }
        else if ($scope.branding.attributes.themeLayoutBgType === 'Image' && !$scope.branding.attributes.themeLayoutBgImage) {
            if ($scope.branding.attributes.themeLayoutBgImage != " ") {
                $scope.branding.attributes.themeLayoutBgImage = " ";
            }
        }
        $scope.updateMenuPreview();
    };

    const updateBgColor = () => {
        if ($scope.branding.attributes.bodyBgType === 'Color' && $scope.branding.attributes.bodyBgColor) {
            $scope.bodyBackgroundColor = $scope.branding.attributes.bodyBgColor;
            clearBgImageState();
        } else if ($scope.branding.attributes.bodyBgType === 'Color') {
            $scope.bodyBackgroundColor = "";
            clearBgImageState();
        }
        $scope.updateMenuPreview();
    };

    $rootScope.$on("updatedValue", function (event, val) {
        $scope.$apply(
            function () {
                $scope.branding.attributes[val.name] = angular.copy(val.color);
            }
        );
    });

    $scope.$watch('branding', function () {
        setTimeout(() => $scope.inputPanelSplitUp("customize-pane-parent", "col-md-3", "col-md-9"), 0);
        if ($scope.branding.attributes.showLogoInForm) {
            if (typeof ($scope.branding.attributes.logoPositionValue) == 'string') {
                $scope.branding.attributes.logoPositionValue = $scope.branding.attributes.logoPositionValue.split('|').map(Number);
            }
            const logoPositionBox = document.getElementById('logo-position-box');
            if (logoPositionBox) {
                logoPositionBox.parentElement.parentElement.classList.add('parent-logo-position-box');
            }
            const logoPositionSelect = document.getElementById('logo-position-select-box');
            if (logoPositionSelect) {
                logoPositionSelect.parentElement.parentElement.classList.add('parent-logo-position-select');
            }
        }

        $scope.updateMenuPreview();
        ComponentUtils.removeEmptyStringInMultivaluedString($scope.config, $scope.branding.attributes);
        if ((!angular.equals($scope.branding.attributes, $scope.oldVal) && $scope.oldVal) || $scope.branding.attributes.themeLayoutBgImageFile || $scope.branding.attributes.bodyBgImageFile) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    $scope.save = function () {
        InputDialog.brandingUpdate("publish", $translate.instant('branding-publish-title'), $translate.instant('branding-publish-desc'), $scope, function ($scope, closeModal) {
            $scope.stage = 2;
            const defaultImage = resourceUrl + '/themes/img/bg-about.png';
            if ($scope.branding.attributes.themeLayoutBgImageFile && !$scope.branding.attributes.bodyBgImageFile && $scope.branding.attributes.themeLayoutBgType === 'Image') {
                if ($scope.branding.attributes.bodyBgImage === ' ') {
                    $scope.branding.attributes.bodyBgImage = defaultImage.replace("/", '');
                }
                if ($scope.branding.attributes.isThemeBgImageValid) {
                    $scope.convertFileToUrlAndSave($scope.branding.attributes.themeLayoutBgImageFile, 'themeLayoutBgImage', 'sectionBgImg', closeModal);
                } else {
                    $scope.stage = 1;
                    closeModal();
                    Notifications.error($translate.instant('section-bg-image-error'));
                }
            }
            else if ($scope.branding.attributes.bodyBgImageFile && !$scope.branding.attributes.themeLayoutBgImageFile && $scope.branding.attributes.bodyBgType === 'Image') {
                if ($scope.branding.attributes.isBodyBgImageValid) {
                    $scope.convertFileToUrlAndSave($scope.branding.attributes.bodyBgImageFile, 'bodyBgImage', 'bodyBgImg', closeModal);
                } else {
                    $scope.stage = 1;
                    closeModal();
                    Notifications.error($translate.instant('body-bg-image-error'));
                }
            }
            else if ($scope.branding.attributes.themeLayoutBgImageFile && $scope.branding.attributes.bodyBgImageFile) {
                if ($scope.branding.attributes.isBodyBgImageValid || $scope.branding.attributes.isThemeBgImageValid) {
                    $scope.convertFileToUrl($scope.branding.attributes.themeLayoutBgImageFile, 'sectionBgImg', closeModal);
                } else {
                    $scope.stage = 1;
                    closeModal();
                    Notifications.error($translate.instant('upload-image-error'));
                }
            }
            else if ($scope.branding.attributes.bodyBgType === 'Image' && $scope.branding.attributes.bodyBgImage === " ") {
                $scope.branding.attributes.bodyBgImage = defaultImage.replace("/", '');
                $scope.saveConfig(closeModal);
            }
            else {
                $scope.branding.attributes.themeLayoutBgImageFile ? delete $scope.branding.attributes.themeLayoutBgImageFile : "";
                $scope.branding.attributes.isThemeBgImageValid ? delete $scope.branding.attributes.isThemeBgImageValid : "";
                $scope.branding.attributes.bodyBgImageFile ? delete $scope.branding.attributes.bodyBgImageFile : "";
                $scope.branding.attributes.isBodyBgImageValid ? delete $scope.branding.attributes.isBodyBgImageValid : "";
                $scope.saveConfig(closeModal);
            }
        }
        );
    };
    $scope.saveConfig = function (closeModal) {
        ComponentUtils.removeEmptyStringInMultivaluedString($scope.config, $scope.branding.attributes);
        $scope.branding.attributes.logoPositionValue = $scope.branding.attributes.logoPositionValue.join('|');
        var brandingCopy = angular.copy($scope.branding);
        ComponentUtils.convertAllMultivaluedStringValuesToList($scope.config, brandingCopy.attributes);
        ComponentUtils.convertAllListValuesToMultivaluedString($scope.config, brandingCopy.attributes);
        if (ComponentUtils.checkNumberTypeMaxValueValidity($scope.config, brandingCopy.attributes)) {
            Brandings.update({ realm: $scope.realm.realm }, brandingCopy, function () {
                Notifications.success($translate.instant('branding-successfully-updated'));
                $scope.stage = 1;
                $scope.changed = false;
                $scope.oldVal = angular.copy(brandingCopy.attributes);
                closeModal && closeModal();
                $route.reload();
            }, function () {
                $scope.stage = 1;
            });
        } else {
            Notifications.error($translate.instant('value-contrain-error'));
            $scope.stage = 1;
        }
    };
    $scope.convertFileToUrlAndSave = function (file, type, formName, closeModal) {
        let imgVersion;
        if ($scope.branding.attributes[type]) {
            const oldVersion = $scope.branding.attributes[type].split('v=')[1];
            imgVersion = !isNaN(oldVersion) ? Number(oldVersion) + 1 : 1;
        } else {
            imgVersion = 1;
        }
        let formData = new FormData();
        formData.append("brandName", $scope.branding.name);
        formData.append(formName, file);
        ConvertImageToUrl.create({
            realm: $scope.realm.realm
        }, formData, function (res) {
            // $scope.branding.attributes[type] = getValidDomainForMinio(location.origin) + "/" + res.url + '?v=' + imgVersion;
            $scope.branding.attributes[type] = res.url;
            if (type === 'themeLayoutBgImage') {
                $scope.branding.attributes.themeLayoutBgImageFile ? delete $scope.branding.attributes.themeLayoutBgImageFile : "";
                $scope.branding.attributes.isThemeBgImageValid ? delete $scope.branding.attributes.isThemeBgImageValid : "";
            }
            if (type === 'bodyBgImage') {
                $scope.branding.attributes.bodyBgImageFile ? delete $scope.branding.attributes.bodyBgImageFile : "";
                $scope.branding.attributes.isBodyBgImageValid ? delete $scope.branding.attributes.isBodyBgImageValid : "";
            }
            $scope.saveConfig(closeModal);
        }, function () {
            $scope.stage = 1;
        });
    };
    $scope.convertFileToUrl = function (file, formName, closeModal) {
        let formData = new FormData();
        formData.append("brandName", $scope.branding.name);
        formData.append(formName, file);
        ConvertImageToUrl.create({
            realm: $scope.realm.realm
        }, formData, function (res) {
            // $scope.branding.attributes.themeLayoutBgImage = getValidDomainForMinio(location.origin) + "/" + res.url;
            $scope.branding.attributes.themeLayoutBgImage = res.url;
            $scope.branding.attributes.themeLayoutBgImageFile ? delete $scope.branding.attributes.themeLayoutBgImageFile : "";
            $scope.branding.attributes.isThemeBgImageValid ? delete $scope.branding.attributes.isThemeBgImageValid : "";
            $scope.convertFileToUrlAndSave($scope.branding.attributes.bodyBgImageFile, 'bodyBgImage', 'bodyBgImg', closeModal);
        });
    };

    const removeEmptyStringInMultivaluedList = function (properties, config) {
        if (!properties) {
            return;
        }
        for (prop of properties) {
            if (prop.type === 'MultivaluedList') {
                var configProperty = config[prop.name];
                if (configProperty && configProperty[0] === "") {
                    config[prop.name] = [];
                }
            }
        }
    };

    $scope.delete = function () {
        Dialog.confirmDelete(branding.name, 'branding', function () {
            Brandings.remove({
                realm: $scope.realm.realm,
                branding: $scope.branding.id
            }, function () {
                $location.url("/realms/" + realm.realm + "/brandings");
                Notifications.success($translate.instant('branding-successfully-removed'));
            });
        });
    };
    $scope.openSection = function (id) {
        if ($scope.currentSection && $scope.currentSection === id) {
            $scope.currentSection = "";
        } else {
            $scope.currentSection = id;
        }
    };
    $scope.editorBackBtn = function () {
        $scope.currentSection = "";
    };
    $scope.deleteValueFromMultivalued = function (optionName, index) {
        optionName.splice(index, 1);
        $scope.updateMenuPreview();
    };
    $scope.openImageUploader = function (id) {
        $('#' + id).click();
    };
    $scope.updateMenuPreview = function () {
        $scope.menuList = [];
        for (const menu of $scope.branding.attributes.headerMenuItems) {
            const menuData = menu.split("|");
            if (menuData && menuData.length) {
                $scope.menuList.push(menuData);
            }
        }
        setTimeout(() => $scope.inputPanelSplitUp("customize-pane-parent", "col-md-3", "col-md-9"), 0);
    };
    $scope.inputPanelSplitUp = function (parentClassName, ...classNames) {
        if (classNames && classNames.length > 0) {
            classNames.forEach(function (className) {
                document.querySelectorAll(`.${parentClassName} .${className}`).forEach(function (e) {
                    e.classList.add("col-12");
                    e.classList.remove(className);
                });
            });
        }
    };

    $scope.$on('$viewContentLoaded', function () {
        setTimeout(() => $scope.inputPanelSplitUp("customize-pane-parent", "col-md-3", "col-md-9"), 200);
    });

    // setTimeout(() => $scope.inputPanelSplitUp("customize-pane-parent", "col-md-3", "col-md-9"), 300)

    $scope.cancel = function () {
        InputDialog.brandingUpdate("cancel", $translate.instant('branding-cancel-title'), $translate.instant('branding-cancel-desc'), $scope, function ($scope) {
            $route.reload();
            if ($scope.branding.attributes.bodyBgType === 'Image' && $scope.branding.attributes.bodyBgImage) {
                document.documentElement.style.setProperty('--body-bg-img', 'url(' + $scope.branding.attributes.bodyBgImage + ')');
            }
            if ($scope.branding.attributes.themeLayoutBgType === 'Image' && $scope.branding.attributes.themeLayoutBgImage) {
                document.documentElement.style.setProperty('--section-bg-image', 'url(' + $scope.branding.attributes.themeLayoutBgImage + ')');
            }
        });
    };
    $scope.reset = function () {
        InputDialog.brandingUpdate('reset', $translate.instant('branding-reset-title'), $translate.instant('branding-reset-desc'), $scope, function ($scope) {
            $scope.stage = 2;
            const defaultImage = resourceUrl + '/themes/img/bg-about.png';
            BrandingDefault.get(
                {
                    realm: $scope.realm.realm
                },
                function (data) {

                    $scope.branding.attributes.bodyBgImage = defaultImage.replace("/", '');
                    $scope.branding.attributes.themeLayoutBgImage = "";
                    document.documentElement.style.setProperty('--body-bg-img', '');
                    document.documentElement.style.setProperty('--section-bg-image', '');
                    let brandingCopy = angular.copy($scope.branding);
                    data.attributes.bodyBgImage = defaultImage.replace("/", '');
                    data.attributes.themeLayoutBgImage = "";
                    if (data.attributes) {
                        brandingCopy.attributes = data.attributes;
                    }
                    Brandings.update({ realm: $scope.realm.realm }, brandingCopy,
                        function () {
                            $scope.stage = 1;
                            $route.reload();
                            sideMenu.classList.add("custom-menu-true");
                        },
                        function () {
                            $scope.stage = 1;
                        }
                    );
                }
            );
        });
    };
});

module.controller('RealmBrandingCtrl', function ($scope, realm, $location, $route, Dialog, Notifications, serverInfo, BrandingAll, Brandings, RealmBrandingCreateDialog, Session, $translate) {

    if (!serverInfo?.brandInfo?.brandingEnabled) {
        $location.path("/notfound");
    }

    $scope.result = [];
    $scope.first = 1;
    $scope.max = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
    $scope.last = $scope.max;
    $scope.total = 0;
    $scope.brandings = [];
    $scope.brandingCopy = [];
    $scope.search = "";
    $scope.searchLoaded = false;
    $scope.realm = realm;
    $scope.loaderTypeName = 'cardListLoader';

    $scope.init = () => {
        $scope.searchLoaded = false;
        $scope.brandings = BrandingAll.query({realm: realm.realm}, function (data) {
            $scope.searchLoaded = true;
            $scope.brandings = data;
            $scope.brandingCopy = angular.copy($scope.brandings);
            $scope.total = data.length;
            $scope.last = $scope.last > $scope.total ? $scope.total : $scope.last;
            $scope.updateResult()
        });
    }

    $scope.initialState = () => {
        $scope.first = 1;
        $scope.last = $scope.max >= $scope.total ? $scope.total : $scope.max;
    }

    $scope.updateResult = () => {
        $scope.result = $scope.brandings.slice(($scope.first - 1), $scope.last);
    }

    $scope.firstPage = () => {
        $scope.initialState();
        $scope.updateResult();
    }

    $scope.previousPage = () => {
        $scope.first = $scope.first - $scope.max;
        $scope.last = $scope.first + ($scope.max - 1);
        $scope.updateResult();
    }

    $scope.nextPage = () => {
        $scope.first = $scope.first + $scope.max;
        $scope.last = $scope.last + $scope.max;
        $scope.last = $scope.last >= $scope.total ? $scope.total : $scope.last;
        $scope.updateResult();
    }

    $scope.handleMaxChange = () => {
        Session.setItem("pageFilterCount", $scope.max);
        $scope.firstPage();
    }

    $scope.handleSearch = () => {
        $scope.brandings = angular.copy($scope.brandingCopy);
        if ($scope.search) {
            $scope.brandings = $scope.brandings.filter( brand => brand.name.toLowerCase().includes($scope.search.toLowerCase()));
        }
        $scope.total = $scope.brandings.length;
        $scope.firstPage()
    }

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.handleSearch();
    };

    $scope.delete = function (branding) {
        Dialog.confirmDelete(branding.name, 'branding', function () {
            Brandings.remove({
                realm: $scope.realm.realm,
                branding: branding.id
            }, function () {
                $location.url("/realms/" + realm.realm + "/brandings");
                $route.reload();
                Notifications.success($translate.instant("branding-successfully-removed"));
            });
        });
    };

    $scope.create = function () {
        RealmBrandingCreateDialog.open($translate.instant('create-new-branding'), '', function (brandName) {
            $scope.brandingRep = {
                name: brandName,
            };
            Brandings.update({ realm: $scope.realm.realm }, $scope.brandingRep, function (res) {
                $location.url("/realms/" + realm.realm + "/brandings/" + res.id);
                Notifications.success($translate.instant("branding-created-successfully"));
            });
        });
    };
});
module.controller('WorkflowsCtrl', function ($scope, realm, serverInfo, $route, $http, $translate, Notifications, Auth) {
    $scope.realm = realm;
    $scope.variant = variant;
    $scope.serverInfo = serverInfo;
    $scope.currentApp = ""
    $scope.configuredWorkflowsProviders = [];
    $scope.connectedApps = [];
    $scope.isConfigLoaded = false;
    $scope.updatingFlowId = ""
    $scope.isOpening = false
    $scope.showReload = false;
    $scope.isOwner = Auth.user.isAccountCreator;

    if (!$scope.isOwner) {
        $location.path('/notfound');
        return;
    }

    const getStreams = () => {
        $http.get(authUrl + `/admin/realms/${realm.realm}/workflows/getAllConfiguredWorkflows`).then(function (result) {
            $scope.configuredWorkflowsProviders = result.data.configuredWorkflowsProviders;
            $scope.connectedApps = [...new Map($scope.configuredWorkflowsProviders.map(item =>
                [item[ "connectedApp"], item])).values()];

            $scope.isConfigLoaded = true;
        }).catch(function (error) {
             $scope.isConfigLoaded = true;
        });
     }

     getStreams();


     $scope.openStreams = () => {
        $scope.isOpening = true;
        $http.get(authUrl + `/admin/realms/${realm.realm}/workflows`).then(function (result) {
            $scope.isOpening = false;
            if (result.data.url) {
                $scope.showReload = true
                window.open( result.data.url, '_blank','noopener');
            } else {
                Notifications.error($translate.instant('failed-to-open'));
            }
         }).catch(function (error) {
              Notifications.error($translate.instant('failed-to-open'));
              $scope.isOpening = false;
         });
     }

     $scope.updateStreams = (flowId, enabled) => {
        $scope.updatingFlowId = flowId;
        const payload = {
            flowId,enabled
        }

        $http.put(authUrl + `/admin/realms/${realm.realm}/workflows/updateWorkflowConfig`, payload).then(function (result) {
               Notifications.success($translate.instant('updated-successfully'));
               $scope.updatingFlowId = "";
               getStreams();
          }).catch(function (error) {
               Notifications.error($translate.instant('failed-to-update'));
               $scope.updatingFlowId = "";
          });
      }

    $scope.setCurrentApp = (name) =>{
        $scope.currentApp = $scope.currentApp !== name ? name : '';
    }

    $scope.reload = () => {
        $route.reload();
        $scope.showReload = false;
    }

    // $scope.searchKey = "";

    // $scope.findConfigured = function (searchValue) {
    //     if (searchValue && searchValue.trim()) {
    //         for (const providers of $scope.configuredWorkflowsProviders) {
    //             if (providers.flowName.toLowerCase().includes(searchValue.toLowerCase())) {
    //                 return true;
    //             }
    //         }
    //         return false;
    //     }
    //     return true;
    // };

    // $scope.changeSearchKey = function (val) {
    //     $scope.searchKey = val;
    // };

    // $scope.clearSearch = function () {
    //     $scope.search = "";
    //     $scope.changeSearchKey($scope.search);
    // };
});

module.controller('RealmPolicyCtrl', function ($scope, Realm, realm, $http, $location, $route, Dialog, Notifications, serverInfo, Policies, PoliciesSearchState, RealmPolicyCreateDialog, Session, $translate) {


    $scope.init = function () {
        $scope.realm = realm;
        $scope.variant = variant;
        $scope.loaderTypeName = 'cardListLoader';
        $scope.serverInfo = serverInfo;
        PoliciesSearchState.query.realm = realm.realm;
        $scope.query = PoliciesSearchState.query;
        $scope.query.max = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
        $scope.query.first = 0;
        $scope.entries = 0;
        $scope.pageCount = 5;
        $scope.startCount = 1;
        $scope.maxValueOfPage = 0;
        $scope.searchQueryLoaded = false;
        $scope.navigationLoaded = false;
        if ($scope.query && $scope.query.first === 0 && $scope.query.policyName) {
            $scope.query.policyName = "";
        }
        $scope.copyElement = angular.copy($scope.query.max);
        initialQueryUpdate();
    };

    function oldQueryUpdate() {
        $scope.lengthQuery = {};
        if ($scope.copyElement && ($scope.query.max !== $scope.copyElement)) {
            $scope.query.first = 0;
        }
        let querySelectorValue = {};
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
            else if (i !== "max" && i !== "first") {
                querySelectorValue = {
                    [i]: $scope.query[i]
                };
                Object.assign($scope.lengthQuery, querySelectorValue);
            }
        }
        Policies.query({
            search: true,
            realm: $scope.realm.realm,
            policyName: $scope.query.policyName
        }, function(response) {
            $scope.entries = response.length;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                $scope.pageCount = $scope.entries;
                $scope.startCount = 1;
            } else {
                $scope.pageCount = $scope.query.max;
                $scope.startCount = 1;
            }
        });
    }

    $scope.$watch('maxValue', function () {
        $scope.query.first = 0;
    }, true);

    $scope.firstPage = function () {
        searchQueryWithLoaded();
        $scope.pageCount = parseInt($scope.query.max);
        $scope.query.first = 0;
        $scope.startCount = 1;
        $scope.QueryUpdate();
    };

    $scope.selectQuery = function () {
        Session.setItem("pageFilterCount", $scope.query.max);
        $scope.searchLoaded = false;
        searchQueryWithLoaded();
        oldQueryUpdate();
        $scope.policies = Policies.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.policyName;
            PoliciesSearchState.isFirstSearch = false;
        });
    };

    $scope.previousPage = function () {
        searchQueryWithLoaded();
        $scope.startCount = $scope.startCount - $scope.query.max;
        let pageCount = $scope.pageCount;
        let queryMax = parseInt($scope.query.max);
        if (pageCount === 0) {
            $scope.pageCount = queryMax - 5;
        }
        else if (pageCount === $scope.entries) {
            let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
            $scope.pageCount = pageCount - backFlow;
        }
        else {
            $scope.pageCount -= queryMax;
        }
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.QueryUpdate();
    };

    $scope.nextPage = function () {
        searchQueryWithLoaded();
        $scope.startCount = $scope.pageCount + 1;
        let fullLenthOfPages = parseInt($scope.query.max) + $scope.pageCount;
        $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
        $scope.pageCount = ($scope.entries < fullLenthOfPages) ? $scope.entries : fullLenthOfPages;
        $scope.query.first += parseInt($scope.query.max);
        $scope.QueryUpdate();
    };

    function searchQueryWithLoaded() {
        $scope.searchLoaded = false;
        if ($scope.query.policyName && $scope.searchQueryLoaded) {
            $scope.searchQueryLoaded = true;
            $scope.query.search = true;
        }
        else {
            $scope.query.policyName = "";
            $scope.searchQueryLoaded = false;
        }
    }

    $scope.reset = function () {
        $scope.query.policyName = "";
        $route.reload();
    };

    $scope.clearSearch = function () {
        $scope.query.policyName = "";
        $scope.searchQuery();
    };

    $scope.QueryUpdate = function () {
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        $scope.policies = Policies.query($scope.query, function () {
            $scope.searchLoaded = true;
        });
    };

    $scope.searchQuery = function () {
        if ($scope.lastSearch !== $scope.query.policyName) {
            $scope.searchLoaded = false;
            if ($scope.query.policyName) {
                $scope.searchQueryLoaded = true;
            }
            else {
                $scope.searchQueryLoaded = false;
            }
            searchQueryWithLoaded();
            oldQueryUpdate();
            $scope.query.first = 0;
            $scope.startCount = 1;
            $scope.policies = Policies.query($scope.query, function () {
                $scope.lastSearch = $scope.query.policyName;
                PoliciesSearchState.isFirstSearch = false;
                $scope.searchLoaded = true;
            });
        }
    };

    $scope.isPoliciesListEmpty = function () {
        return $scope.policies.length == 0;
    };

    function initialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        searchQueryWithLoaded();
        $scope.policies = Policies.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.policyName;
            PoliciesSearchState.isFirstSearch = false;
        });
    };

    $scope.delete = function (policy) {
        Dialog.confirmDelete(policy.policyName, $scope.realm.mode === 'bo' ? 'rule' : 'policy', function () {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: policy.id
            }, function () {
                $location.url("/realms/" + realm.realm + "/policies");
                $route.reload();
                Notifications.success($translate.instant($scope.realm.mode === 'bo' ? 'rule-name-removed-successfully' : "policy-name-removed-successfully"));
            });
        });

    };

    $scope.create = function () {
        RealmPolicyCreateDialog.open($translate.instant($scope.realm.mode === 'bo' ? 'create-new-rule' : 'create-new-policy'), '', function (policyName, policyType) {
            $scope.policyRep = {
                policyName: policyName,
                policyType: policyType,
                policyValue: "",
                realmPolicy: false,
            };
            Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function (res) {
                $location.url("/realms/" + realm.realm + "/policies/" + policyType + "/" + res.id);
                Notifications.success($translate.instant($scope.realm.mode === 'bo' ? 'rule-created-successfully' : "policy-created-successfully"));
            });
        });
    };
});

module.controller('RealmPasswordPolicyCtrl', function ($scope, Realm, realm, $http, $location, $route, Dialog, Notifications, serverInfo, Policies, policies, ConfirmExitDialog, $translate) {

    const { dropdownToggle } = { ...getDropdown() };
    var parse = function (policyString) {
        var policies = [];
        if (!policyString || policyString.length === 0) {
            return policies;
        }

        var policyArray = policyString.split(" and ");

        for (var i = 0; i < policyArray.length; i++) {
            var policyToken = policyArray[i];
            var id;
            var value;
            if (policyToken.indexOf('(') == -1) {
                id = policyToken.trim();
                value = null;
            } else {
                id = policyToken.substring(0, policyToken.indexOf('('));
                value = policyToken.substring(policyToken.indexOf('(') + 1, policyToken.lastIndexOf(')')).trim();
            }

            for (var j = 0; j < serverInfo.passwordPolicies.length; j++) {
                if (serverInfo.passwordPolicies[j].id == id) {
                    // clone
                    var p = JSON.parse(JSON.stringify(serverInfo.passwordPolicies[j]));

                    p.value = value && value || p.defaultValue;
                    policies.push(p);
                }
            }
        }
        return policies;
    };

    var toString = function (policies) {
        if (!policies || policies.length == 0) {
            return "";
        }
        var policyString = "";
        for (var i = 0; i < policies.length; i++) {
            policyString += policies[i].id + '(' + policies[i].value + ')';
            if (i != policies.length - 1) {
                policyString += ' and ';
            }
        }
        return policyString;
    };
    $scope.variant = variant;
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.policyRep = {
        policyName: $route.current.params.policyName,
        policyType: "password",
        policyValue: "",
        realmPolicy: false,
    };

    if (policies.id) {
        $scope.policyRep.id = policies.id;
    }

    if (policies.policyValue) {
        $scope.policyRep.policyValue = policies.policyValue;
    }

    if (policies.policyName) {
        $scope.policyRep.policyName = policies.policyName;
    }

    if (policies.realmPolicy) {
        $scope.policyRep.realmPolicy = policies.realmPolicy;
    }

    $scope.changed = false;
    $scope.openRedirect = false;
    $scope.policy = parse(policies.policyValue);
    var oldCopy = angular.copy($scope.policy);

    $scope.$watch('policy', function () {
        if (!angular.equals($scope.policy, oldCopy)) {
            let changed = true;
            for (const policy of $scope.policy) {
                if (policy.id !== "notEmail" && policy.id !== "notUsername") {
                    if (!policy.value) {
                        changed = false;
                        break;
                    }
                }
            }
            $scope.changed = changed;
        }
    }, true);

    $scope.addPolicy = function (event, policy) {
        event.stopPropagation();
        policy.value = policy.defaultValue;
        if (!$scope.policy) {
            $scope.policy = [];
        }
        if (policy.multipleSupported) {
            $scope.policy.push({ ...policy });
        } else {
            $scope.policy.push(policy);
        }
    };

    $scope.openPolicy = dropdownToggle;

    $scope.removePolicy = function (index) {
        $scope.policy.splice(index, 1);
    };

    $scope.delete = function () {
        Dialog.confirmDelete(policies.policyName, 'policy', function () {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: $scope.policyRep.id
            }, function () {
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/policies");
                Notifications.success($translate.instant("policy-name-removed-successfully"));
            });
        });
    };

    $scope.save = function () {
        $scope.policyRep.policyValue = toString($scope.policy);
        Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function () {
            $scope.openRedirect = true;
            $route.reload();
            Notifications.success($translate.instant("policy-created-successfully"));
        });
    };

    $scope.reset = function () {
        $location.url("/realms/" + realm.realm + "/policies");
    };

    $scope.isPolicyListEmpty = function () {
        return parse($scope.policyRep.policyValue).length == 0;
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('RealmUsernamePolicyCtrl', function ($scope, Realm, realm, $http, $location, $route, Dialog, Notifications, serverInfo, Policies, policies, ConfirmExitDialog, $translate) {

    const { dropdownToggle } = { ...getDropdown() };
    var parse = function (policyString) {
        var policies = [];
        if (!policyString || policyString.length === 0) {
            return policies;
        }

        var policyArray = policyString.split(" and ");

        for (var i = 0; i < policyArray.length; i++) {
            var policyToken = policyArray[i];
            var id;
            var value;
            if (policyToken.indexOf('(') == -1) {
                id = policyToken.trim();
                value = null;
            } else {
                id = policyToken.substring(0, policyToken.indexOf('('));
                value = policyToken.substring(policyToken.indexOf('(') + 1, policyToken.lastIndexOf(')')).trim();
            }

            for (var j = 0; j < serverInfo.usernamePolicies.length; j++) {
                if (serverInfo.usernamePolicies[j].id == id) {
                    // clone
                    var p = JSON.parse(JSON.stringify(serverInfo.usernamePolicies[j]));

                    p.value = value && value || p.defaultValue;
                    policies.push(p);
                }
            }
        }
        return policies;
    };

    var toString = function (policies) {
        if (!policies || policies.length == 0) {
            return "";
        }
        var policyString = "";
        for (var i = 0; i < policies.length; i++) {
            policyString += policies[i].id + '(' + policies[i].value + ')';
            if (i != policies.length - 1) {
                policyString += ' and ';
            }
        }
        return policyString;
    };
    $scope.variant = variant;
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.policyRep = {
        policyName: $route.current.params.policyName,
        policyType: "username",
        policyValue: "",
        realmPolicy: false,
    };

    if (policies.id) {
        $scope.policyRep.id = policies.id;
    }

    if (policies.policyValue) {
        $scope.policyRep.policyValue = policies.policyValue;
    }

    if (policies.policyName) {
        $scope.policyRep.policyName = policies.policyName;
    }

    $scope.changed = false;
    $scope.openRedirect = false;
    $scope.policy = parse(policies.policyValue);
    var oldCopy = angular.copy($scope.policy);

    $scope.$watch('policy', function () {
        $scope.changed = !angular.equals($scope.policy, oldCopy);
    }, true);

    $scope.addPolicy = function (event, policy) {
        event.stopPropagation();
        policy.value = policy.defaultValue;
        if (!$scope.policy) {
            $scope.policy = [];
        }
        if (policy.multipleSupported) {
            $scope.policy.push({ ...policy });
        } else {
            $scope.policy.push(policy);
        }
    };

    $scope.openPolicy = dropdownToggle;

    $scope.removePolicy = function (index) {
        $scope.policy.splice(index, 1);
    };

    $scope.delete = function () {
        Dialog.confirmDelete(policies.policyName, 'policy', function () {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: $scope.policyRep.id
            }, function () {
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/policies");
                Notifications.success($translate.instant("policy-name-removed-successfully"));
            });
        });
    };

    $scope.save = function () {
        $scope.policyRep.policyValue = toString($scope.policy);

        Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function () {
            $scope.openRedirect = true;
            $route.reload();
            Notifications.success($translate.instant("policy-created-successfully"));
        });
    };

    $scope.reset = function () {
        $location.url("/realms/" + realm.realm + "/policies");
    };

    $scope.isPolicyListEmpty = function () {
        return parse($scope.policyRep.policyValue).length == 0;
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('RealmCibaPolicyCtrl', function ($scope, Realm, realm, $http, $location, $route, $modal, Dialog, Notifications, serverInfo, Policies, policies, ConfirmExitDialog, $translate) {
    const { dropdownToggle } = { ...getDropdown() };
    $scope.policy = [];
    $scope.metadataRuleRepresentation = [];
    $scope.actionRuleRepresentaion = [];
    function init() {
        var value = policies.policyValue;
        var policyArray = value.split(" and ");
        policyArray.map(function (policy) {
            if (policy.trim() !== "" && policy.startsWith("metadata") && !policy.includes("decision")) {
                getMetaDataRuleRepresentation(policy);
            } else if (policy.trim() !== "" && !policy.includes("decision")) {
                getActionRuleRepresentation(policy);
            } else {
                // decision
                var id;
                var value;
                if (policy.indexOf('(') == -1) {
                    id = policy.trim();
                    value = null;
                } else {
                    var policydetails = policy.replaceAll("(", " ").replaceAll(")", " ").split(" ");
                    id = policydetails[0];
                    value = policydetails[1];
                }
                if (id === "decision") {
                    if (value) {
                        $scope.decision = value;
                        $scope.oldStrategyDecision = angular.copy($scope.decision);
                        $(`#${$scope.decision}`).attr('checked', 'checked');
                    } else {
                        $scope.decision = "unanimous";
                        $scope.oldStrategyDecision = angular.copy($scope.decision);
                        $('#unanimous').attr('checked', 'checked');
                    }
                }
            }
        });
    };

    init();

    $scope.oldMetadataRuleRepresentation = angular.copy($scope.metadataRuleRepresentation);
    $scope.oldActionRuleRepresentation = angular.copy($scope.actionRuleRepresentaion);


    function parseSubString(polSubString) {
        if (polSubString.indexOf('(') == 0)
            return polSubString.substring(1, polSubString.length);

        if (polSubString.indexOf(')') >= 0)
            return polSubString.substring(0, polSubString.length - 1);

        return polSubString;
    };

    function getMetaDataRuleRepresentation(policy) {
        if (policy == undefined || policy.trim() == "") return;
        if (policy.includes("metadata(")) {
            policy = policy.replace("metadata(", "");
            policy = removeLastCharIfMatches(policy, ")");
        }
        var policyToken = policy.split(') (');
        var prKey = parseSubString(policyToken[0]);
        var prop = {
            id: $scope.metadataRuleRepresentation.length + 1,
            propKey: prKey,
            propType: parseSubString(policyToken[1]),
            propComparator: parseSubString(policyToken[2]),
            propValue: parseSubString(policyToken[3])
        };
        $scope.metadataRuleRepresentation.push(prop);
        $scope.policy.push(prop);
    }

    function removeLastCharIfMatches(str, charToRemove) {
        if (str.charAt(str.length - 1) === charToRemove) {
            return str.slice(0, -1);
        }
        return str;
    }

    function getActionRuleRepresentation(policy) {

        var policyToken = policy;
        if (policyToken.trim() == "") return;
        var id;
        var value;
        if (policyToken.indexOf('(') == -1) {
            id = policyToken.trim();
            value = null;
        } else {
            id = policyToken.substring(0, policyToken.indexOf('('));
            value = policyToken.substring(policyToken.indexOf('(') + 1, policyToken.lastIndexOf(')')).trim();
        }
        for (var j = 0; j < serverInfo.actionPolicies.length; j++) {
            if (serverInfo.actionPolicies[j].id == id) {
                // clone
                var p = JSON.parse(JSON.stringify(serverInfo.actionPolicies[j]));

                p.value = value && value || p.defaultValue;

                $scope.actionRuleRepresentaion.push(p);
                $scope.policy.push(p);
            }
        }
    }

    var getPolicyString = () => {
        var policyString = "";
        for (var i = 0; i < $scope.metadataRuleRepresentation.length; i++) {
            var metadata = "";
            metadata += '(' + $scope.metadataRuleRepresentation[i].propKey + ') ';
            metadata += '(' + $scope.metadataRuleRepresentation[i].propType + ') ';
            metadata += '(' + $scope.metadataRuleRepresentation[i].propComparator + ') ';
            metadata += '(' + $scope.metadataRuleRepresentation[i].propValue + ')';
            policyString += 'metadata(' + metadata + ')';
            if (i != $scope.metadataRuleRepresentation.length - 1) {
                policyString += ' and ';
            }
        }

        if (policyString !== undefined && policyString !== "" && policyString.trim() !== "" && $scope.actionRuleRepresentaion.length > 0) {
            policyString += ' and ';
        }
        for (var i = 0; i < $scope.actionRuleRepresentaion.length; i++) {
            if (!$scope.actionRuleRepresentaion[i].value) {
                policyString += $scope.actionRuleRepresentaion[i].id + '(' + '' + ')';
            } else {
                policyString += $scope.actionRuleRepresentaion[i].id + '(' + $scope.actionRuleRepresentaion[i].value + ')';
            }
            if (i != $scope.actionRuleRepresentaion.length - 1) {
                policyString += ' and ';
            }
        }

        // decision
        var count = $scope.metadataRuleRepresentation.length + $scope.actionRuleRepresentaion.length;
        var decision = $scope.decision || 'unanimous';
        if (count <= 1) {
            decision = 'unanimous';
        }
        if (policyString !== undefined && policyString !== "" && policyString.trim() !== "") {
            policyString += ' and decision(' + decision + ')';
        } else {
            policyString = "";
        }

        return policyString;
    };

    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.policyRep = {
        policyName: $route.current.params.policyName,
        policyType: "ciba",
        policyValue: "",
        realmPolicy: false,
    };

    if (policies.id) {
        $scope.policyRep.id = policies.id;
    }

    if (policies.policyValue) {
        $scope.policyRep.policyValue = policies.policyValue;
    }

    if (policies.policyName) {
        $scope.policyRep.policyName = policies.policyName;
    }

    $scope.changed = false;
    $scope.openRedirect = false;
    var oldCopy = angular.copy($scope.policy);

    $scope.$watch('policy', function () {
        if (!angular.equals($scope.policy, oldCopy) || !angular.equals($scope.oldStrategyDecision, $scope.decision)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    $scope.addPolicy = function (event, policy) {
        event.stopPropagation();
        if (policy.id == 'metadata') {
            var prop = {
                id: $scope.metadataRuleRepresentation.length + 1,
                propKey: "",
                propType: "",
                propComparator: "",
                propValue: "",
                elementId: "new-condition"
            };
            if (policy.multipleSupported) {
                $scope.metadataRuleRepresentation.push({ ...prop });
                $scope.policy.push({ ...prop });
            } else {
                $scope.metadataRuleRepresentation.push(prop);
                $scope.policy.push(prop);
            }
        } else {
            if (!policy.defaultValue) {
                policy.value = "";
            } else {
                policy.value = policy.defaultValue;
            }
            if (!$scope.policy) {
                $scope.policy = [];
            }
            if (policy.multipleSupported) {
                $scope.actionRule.push({ ...policy });
                $scope.policy.push({ ...policy });
            } else {
                $scope.actionRuleRepresentaion.push(policy);
                $scope.policy.push(policy);
            }
        }
        $scope.openPolicy(event.target.parentElement.parentElement);
    };

    $scope.openPolicy = dropdownToggle;

    $scope.removeMetadataRule = function (index, policyId) {
        $scope.metadataRuleRepresentation.splice(index, 1);
        $scope.policy = $scope.policy.filter(e => e.id != policyId);
    };

    $scope.removeActionRule = function (index, policyId) {
        $scope.actionRuleRepresentaion.splice(index, 1);
        $scope.policy = $scope.policy.filter(e => e.id != policyId);
    };

    $scope.$watch("metadataRuleRepresentation", function () {
        if ($scope.metadataRuleRepresentation != $scope.oldMetadataRuleRepresentation) $scope.changed = true;
    }, true);

    $scope.$watch("actionRuleRepresentaion", function () {
        if ($scope.actionRuleRepresentaion != $scope.oldActionRuleRepresentation) $scope.changed = true;
    }, true);

    $scope.updateDecision = function (value) {
        $scope.decision = value;
    };
    $scope.delete = function () {
        Dialog.confirmDelete(policies.policyName, $scope.realm.mode === 'bo' ? 'rule' : 'policy', function () {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: $scope.policyRep.id
            }, function () {
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/policies");
                Notifications.success($translate.instant("rule-name-removed-successfully"));
            });
        });
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };

    $scope.save = function () {
        $scope.policyCopy = angular.copy($scope.policy);
        $scope.policyCopy.push({ propKey: "decision", propValue: $scope.decision == undefined ? "unanimous" : $scope.decision });
        $scope.policyRep.policyValue = getPolicyString();

        Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function () {
            $scope.openRedirect = true;
            $route.reload();
            Notifications.success($translate.instant("rule-updated-successfully"));
        });
    };
    $scope.$watch('decision', function () {
        if (!angular.equals($scope.policy, oldCopy) || !angular.equals($scope.oldStrategyDecision, $scope.decision)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);
    $scope.reset = function () {
        $location.url("/realms/" + realm.realm + "/policies");
    };

    $scope.isPolicyListEmpty = function () {
        return parse($scope.policyRep.policyValue).length == 0;
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('CibaRuleModalCtrl', function ($scope, $location, $route, Dialog, Notifications, Policies, ConfirmExitDialog, $translate) {
    const { dropdownToggle } = { ...getDropdown() };
    $scope.policy = [];
    $scope.metadataRuleRepresentation = [];
    $scope.resourceUrl = resourceUrl;
    $scope.actionRuleRepresentaion = [];
    $scope.policies = $scope.$parent.policies;
    $scope.serverInfo = $scope.$parent.serverInfo;
    $scope.realm = $scope.$parent.realm;
    $scope.closeRuleCreationModal = $scope.$parent.cancel;
    function init() {
        var value = $scope.policies.policyValue;
        var policyArray = value.split(" and ");
        policyArray.map(function (policy) {
            if (policy.trim() !== "" && policy.startsWith("metadata") && !policy.includes("decision")) {
                getMetaDataRuleRepresentation(policy);
            } else if (policy.trim() !== "" && !policy.includes("decision")) {
                getActionRuleRepresentation(policy);
            } else {
                // decision
                var id;
                var value;
                if (policy.indexOf('(') == -1) {
                    id = policy.trim();
                    value = null;
                } else {
                    var policydetails = policy.replaceAll("(", " ").replaceAll(")", " ").split(" ");
                    id = policydetails[0];
                    value = policydetails[1];
                }
                if (id === "decision") {
                    if (value) {
                        $scope.decision = value;
                        $scope.oldStrategyDecision = angular.copy($scope.decision);
                        $(`#${$scope.decision}`).attr('checked', 'checked');
                    } else {
                        $scope.decision = "unanimous";
                        $scope.oldStrategyDecision = angular.copy($scope.decision);
                        $('#unanimous').attr('checked', 'checked');
                    }
                }
            }
        });
    };

    init();

    $scope.oldMetadataRuleRepresentation = angular.copy($scope.metadataRuleRepresentation);
    $scope.oldActionRuleRepresentation = angular.copy($scope.actionRuleRepresentaion);


    function parseSubString(polSubString) {
        if (polSubString.indexOf('(') == 0)
            return polSubString.substring(1, polSubString.length);

        if (polSubString.indexOf(')') >= 0)
            return polSubString.substring(0, polSubString.length - 1);

        return polSubString;
    };

    function getMetaDataRuleRepresentation(policy) {
        if (policy == undefined || policy.trim() == "") return;
        if (policy.includes("metadata(")) {
            policy = policy.replace("metadata(", "");
            policy = removeLastCharIfMatches(policy, ")");
        }
        var policyToken = policy.split(') (');
        var prKey = parseSubString(policyToken[0]);
        var prop = {
            id: $scope.metadataRuleRepresentation.length + 1,
            propKey: prKey,
            propType: parseSubString(policyToken[1]),
            propComparator: parseSubString(policyToken[2]),
            propValue: parseSubString(policyToken[3])
        };
        $scope.metadataRuleRepresentation.push(prop);
        $scope.policy.push(prop);
    }

    function removeLastCharIfMatches(str, charToRemove) {
        if (str.charAt(str.length - 1) === charToRemove) {
            return str.slice(0, -1);
        }
        return str;
    }

    function getActionRuleRepresentation(policy) {

        var policyToken = policy;
        if (policyToken.trim() == "") return;
        var id;
        var value;
        if (policyToken.indexOf('(') == -1) {
            id = policyToken.trim();
            value = null;
        } else {
            id = policyToken.substring(0, policyToken.indexOf('('));
            value = policyToken.substring(policyToken.indexOf('(') + 1, policyToken.lastIndexOf(')')).trim();
        }
        for (var j = 0; j < $scope.serverInfo.actionPolicies.length; j++) {
            if ($scope.serverInfo.actionPolicies[j].id == id) {
                // clone
                var p = JSON.parse(JSON.stringify($scope.serverInfo.actionPolicies[j]));

                p.value = value && value || p.defaultValue;

                $scope.actionRuleRepresentaion.push(p);
                $scope.policy.push(p);
            }
        }
    }

    var getPolicyString = () => {
        var policyString = "";
        for (var i = 0; i < $scope.metadataRuleRepresentation.length; i++) {
            var metadata = "";
            metadata += '(' + $scope.metadataRuleRepresentation[i].propKey + ') ';
            metadata += '(' + $scope.metadataRuleRepresentation[i].propType + ') ';
            metadata += '(' + $scope.metadataRuleRepresentation[i].propComparator + ') ';
            metadata += '(' + $scope.metadataRuleRepresentation[i].propValue + ')';
            policyString += 'metadata(' + metadata + ')';
            if (i != $scope.metadataRuleRepresentation.length - 1) {
                policyString += ' and ';
            }
        }

        if (policyString !== undefined && policyString !== "" && policyString.trim() !== "" && $scope.actionRuleRepresentaion.length > 0) {
            policyString += ' and ';
        }
        for (var i = 0; i < $scope.actionRuleRepresentaion.length; i++) {
            if (!$scope.actionRuleRepresentaion[i].value) {
                policyString += $scope.actionRuleRepresentaion[i].id + '(' + '' + ')';
            } else {
                policyString += $scope.actionRuleRepresentaion[i].id + '(' + $scope.actionRuleRepresentaion[i].value + ')';
            }
            if (i != $scope.actionRuleRepresentaion.length - 1) {
                policyString += ' and ';
            }
        }

        // decision
        var count = $scope.metadataRuleRepresentation.length + $scope.actionRuleRepresentaion.length;
        var decision = $scope.decision || 'unanimous';
        if (count <= 1) {
            decision = 'unanimous';
        }
        if (policyString !== undefined && policyString !== "" && policyString.trim() !== "") {
            policyString += ' and decision(' + decision + ')';
        } else {
            policyString = "";
        }

        return policyString;
    };

    $scope.policyRep = {
        policyName: $route.current.params.policyName,
        policyType: "ciba",
        policyValue: "",
        realmPolicy: false,
    };

    if ($scope.policies.id) {
        $scope.policyRep.id = $scope.policies.id;
    }

    if ($scope.policies.policyValue) {
        $scope.policyRep.policyValue = $scope.policies.policyValue;
    }

    if ($scope.policies.policyName) {
        $scope.policyRep.policyName = $scope.policies.policyName;
    }

    $scope.changed = false;
    $scope.openRedirect = false;
    var oldCopy = angular.copy($scope.policy);

    $scope.$watch('policy', function () {
        if (!angular.equals($scope.policy, oldCopy) || !angular.equals($scope.oldStrategyDecision, $scope.decision)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    $scope.addPolicy = function (event, policy) {
        event.stopPropagation();
        if (policy.id == 'metadata') {
            var prop = {
                id: $scope.metadataRuleRepresentation.length + 1,
                propKey: "",
                propType: "",
                propComparator: "",
                propValue: ""
            };
            if (policy.multipleSupported) {
                $scope.metadataRuleRepresentation.push({ ...prop });
                $scope.policy.push({ ...prop });
            } else {
                $scope.metadataRuleRepresentation.push(prop);
                $scope.policy.push(prop);
            }
        } else {
            if (!policy.defaultValue) {
                policy.value = "";
            } else {
                policy.value = policy.defaultValue;
            }
            if (!$scope.policy) {
                $scope.policy = [];
            }
            if (policy.multipleSupported) {
                $scope.actionRule.push({ ...policy });
                $scope.policy.push({ ...policy });
            } else {
                $scope.actionRuleRepresentaion.push(policy);
                $scope.policy.push(policy);
            }
        }
        $scope.openPolicy(event.target.parentElement.parentElement);
    };

    $scope.openPolicy = dropdownToggle;

    $scope.removeMetadataRule = function (index, policyId) {
        $scope.metadataRuleRepresentation.splice(index, 1);
        $scope.policy = $scope.policy.filter(e => e.id != policyId);
    };

    $scope.removeActionRule = function (index, policyId) {
        $scope.actionRuleRepresentaion.splice(index, 1);
        $scope.policy = $scope.policy.filter(e => e.id != policyId);
    };

    $scope.$watch("metadataRuleRepresentation", function () {
        if ($scope.metadataRuleRepresentation != $scope.oldMetadataRuleRepresentation) $scope.changed = true;
    }, true);

    $scope.$watch("actionRuleRepresentaion", function () {
        if ($scope.actionRuleRepresentaion != $scope.oldActionRuleRepresentation) $scope.changed = true;
    }, true);

    $scope.updateDecision = function (value) {
        $scope.decision = value;
    };
    $scope.delete = function () {
        Dialog.confirmDelete($scope.policies.policyName, $scope.realm.mode === 'bo' ? 'rule' : 'policy', function () {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: $scope.policyRep.id
            }, function () {
                $scope.openRedirect = true;
                $modalInstance.dismiss('cancel');
                Notifications.success($translate.instant("rule-name-removed-successfully"));
            });
        });
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };

    $scope.save = function () {
        $scope.policyCopy = angular.copy($scope.policy);
        $scope.policyCopy.push({ propKey: "decision", propValue: $scope.decision == undefined ? "unanimous" : $scope.decision });
        $scope.policyRep.policyValue = getPolicyString();

        Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function () {
            $scope.openRedirect = true;
            $scope.changed = false;
            oldCopy = angular.copy($scope.policy);
            $scope.oldStrategyDecision = angular.copy($scope.decision);
            Notifications.success($translate.instant("rule-updated-successfully"));
            $scope.closeRuleCreationModal();
        });
    };
    $scope.$watch('decision', function () {
        if (!angular.equals($scope.policy, oldCopy) || !angular.equals($scope.oldStrategyDecision, $scope.decision)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);
    $scope.reset = function () {
        $location.url("/realms/" + $scope.realm.realm + "/policies");
    };

    $scope.isPolicyListEmpty = function () {
        return parse($scope.policyRep.policyValue).length == 0;
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('RealmActionPolicyCtrl', function ($scope, Realm, realm, $http, $location, $route, $modal, Dialog, Notifications, serverInfo, Policies, policies, ConfirmExitDialog, $translate) {

    const { dropdownToggle } = { ...getDropdown() };
    var parse = function (policyString) {
        var policies = [];
        if (!policyString || policyString.length === 0) {
            return policies;
        }

        var policyArray = policyString.split(" and ");

        for (var i = 0; i < policyArray.length; i++) {
            var policyToken = policyArray[i];
            var id;
            var value;
            if (policyToken.indexOf('(') == -1) {
                id = policyToken.trim();
                value = null;
            } else {
                id = policyToken.substring(0, policyToken.indexOf('('));
                value = policyToken.substring(policyToken.indexOf('(') + 1, policyToken.lastIndexOf(')')).trim();
            }
            if (id === "decision") {
                if (value) {
                    $scope.decision = value;
                    $scope.oldStrategyDecision = angular.copy($scope.decision);
                    $(`#${$scope.decision}`).attr('checked', 'checked');
                } else {
                    $scope.decision = "unanimous";
                    $scope.oldStrategyDecision = angular.copy($scope.decision);
                    $('#unanimous').attr('checked', 'checked');
                }
            }
            for (var j = 0; j < serverInfo.actionPolicies.length; j++) {
                if (serverInfo.actionPolicies[j].id == id) {
                    // clone
                    var p = JSON.parse(JSON.stringify(serverInfo.actionPolicies[j]));

                    p.value = value && value || p.defaultValue;
                    policies.push(p);
                }
            }
        }
        return policies;
    };

    var toString = function (policies) {
        if (!policies || policies.length == 0) {
            return "";
        }
        var policyString = "";
        for (var i = 0; i < policies.length; i++) {
            if (!policies[i].value) {
                policyString += policies[i].id + '(' + '' + ')';
            } else {
                policyString += policies[i].id + '(' + policies[i].value + ')';
            }
            if (i != policies.length - 1) {
                policyString += ' and ';
            }
        }
        return policyString;
    };
    $scope.variant = variant;
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.policyRep = {
        policyName: $route.current.params.policyName,
        policyType: "action",
        policyValue: "",
        realmPolicy: false,
    };

    if (policies.id) {
        $scope.policyRep.id = policies.id;
    }

    if (policies.policyValue) {
        $scope.policyRep.policyValue = policies.policyValue;
    }

    if (policies.policyName) {
        $scope.policyRep.policyName = policies.policyName;
    }

    $scope.changed = false;
    $scope.openRedirect = false;
    $scope.policy = parse(policies.policyValue);
    var oldCopy = angular.copy($scope.policy);

    $scope.$watch('policy', function () {
        if (!angular.equals($scope.policy, oldCopy) || !angular.equals($scope.oldStrategyDecision, $scope.decision)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    $scope.addPolicy = function (event, policy) {
        event.stopPropagation();
        if (!policy.defaultValue) {
            policy.value = "";
        } else {
            policy.value = policy.defaultValue;
        }
        if (!$scope.policy) {
            $scope.policy = [];
        }
        if (policy.multipleSupported) {
            $scope.policy.push({ ...policy });
        } else {
            $scope.policy.push(policy);
        }
    };

    $scope.openPolicy = dropdownToggle;

    $scope.removePolicy = function (index) {
        $scope.policy.splice(index, 1);
        if ($scope.policy && $scope.policy.length < 2) {
            $scope.updateDecision('unanimous');
        }
    };

    $scope.updateDecision = function (value) {
        $scope.decision = value;
    };
    $scope.delete = function () {
        Dialog.confirmDelete(policies.policyName, 'policy', function () {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: $scope.policyRep.id
            }, function () {
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/policies");
                Notifications.success($translate.instant("policy-name-removed-successfully"));
            });
        });
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };

    $scope.save = function () {
        $scope.policyCopy = angular.copy($scope.policy);
        $scope.policyCopy.push({ id: "decision", value: $scope.decision });
        $scope.policyRep.policyValue = toString($scope.policyCopy);

        Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function () {
            $scope.openRedirect = true;
            $route.reload();
            Notifications.success($translate.instant("policy-created-successfully"));
        });
    };
    $scope.$watch('decision', function () {
        if (!angular.equals($scope.policy, oldCopy) || !angular.equals($scope.oldStrategyDecision, $scope.decision)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);
    $scope.reset = function () {
        $location.url("/realms/" + realm.realm + "/policies");
    };

    $scope.isPolicyListEmpty = function () {
        return parse($scope.policyRep.policyValue).length == 0;
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('RealmWebAuthnPolicyCtrl', function ($scope, Current, Realm, realm, $location, serverInfo, $http, $route, Dialog, Notifications, Policies, policies, ConfirmExitDialog, $translate) {

    var parse = function (policyString) {
        var policies = {};
        if (!policyString || policyString.length === 0) {
            return policies;
        }

        try { policies = JSON.parse(policyString); }
        catch (e) {

        }
        return policies;
    };
    $scope.variant = variant;
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.policyRep = {
        policyName: $route.current.params.policyName,
        policyType: "webauthn",
        policyValue: "",
        realmPolicy: false,
    };

    $scope.policySignatureAlgorithms = ["ES384", "ES512", "RS256", "RS384", "RS512", "RS1"];

    $scope.webauthnpolicy = {};

    if (policies.id) {
        $scope.policyRep.id = policies.id;
    }

    if (policies.policyValue) {
        $scope.policyRep.policyValue = policies.policyValue;
    }

    if (policies.policyName) {
        $scope.policyRep.policyName = policies.policyName;
    }

    $scope.changed = false;
    $scope.openRedirect = false;
    $scope.webauthnpolicy = parse(policies.policyValue);
    var oldCopy = angular.copy($scope.webauthnpolicy);

    $scope.$watch('webauthnpolicy', function () {
        if (!angular.equals($scope.webauthnpolicy, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.deleteAcceptableAaguid = function (index) {
        $scope.webauthnpolicy.webAuthnPolicyAcceptableAaguids.splice(index, 1);
    };

    $scope.addAcceptableAaguid = function () {
        if (!$scope.webauthnpolicy.webAuthnPolicyAcceptableAaguids) {
            $scope.webauthnpolicy["webAuthnPolicyAcceptableAaguids"] = [];
        }
        $scope.webauthnpolicy.webAuthnPolicyAcceptableAaguids.push($scope.newAcceptableAaguid);
        $scope.newAcceptableAaguid = "";
    };

    $scope.save = function () {
        $scope.policyRep.policyValue = JSON.stringify($scope.webauthnpolicy);
        Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function () {
            $scope.openRedirect = true;
            $route.reload();
            Notifications.success($translate.instant("policy-created-successfully"));
        });
    };

    $scope.delete = function () {
        Dialog.confirmDelete(policies.policyName, 'policy', function () {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: $scope.policyRep.id
            }, function () {
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/policies");
                Notifications.success($translate.instant("policy-name-removed-successfully"));
            });
        });
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('RealmWebAuthnPasswordlessPolicyCtrl', function ($scope, Current, Realm, realm, $location, serverInfo, $http, $route, Dialog, Notifications, Policies, policies, $translate) {
    var parse = function (policyString) {
        var policies = {};
        if (!policyString || policyString.length === 0) {
            return policies;
        }

        try { policies = JSON.parse(policyString); }
        catch (e) {

        }
        return policies;
    };
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.policyRep = {
        policyName: $route.current.params.policyName,
        policyType: "webauthn-passwordless",
        policyValue: "",
        realmPolicy: false,
    };

    $scope.webauthnpolicy = {};

    $scope.policySignatureAlgorithms = ["ES384", "ES512", "RS256", "RS384", "RS512", "RS1"];

    if (policies.id) {
        $scope.policyRep.id = policies.id;
    }

    if (policies.policyValue) {
        $scope.policyRep.policyValue = policies.policyValue;
    }

    if (policies.policyName) {
        $scope.policyRep.policyName = policies.policyName;
    }

    $scope.changed = false;
    $scope.webauthnpolicy = parse(policies.policyValue);
    var oldCopy = angular.copy($scope.webauthnpolicy);

    $scope.$watch('webauthnpolicy', function () {
        if (!angular.equals($scope.webauthnpolicy, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.deleteAcceptableAaguid = function (index) {
        $scope.webauthnpolicy.webAuthnPolicyAcceptableAaguids.splice(index, 1);
    };

    $scope.addAcceptableAaguid = function () {
        if (!$scope.webauthnpolicy.webAuthnPolicyAcceptableAaguids) {
            $scope.webauthnpolicy["webAuthnPolicyAcceptableAaguids"] = [];
        }
        $scope.webauthnpolicy.webAuthnPolicyAcceptableAaguids.push($scope.newAcceptableAaguid);
        $scope.newAcceptableAaguid = "";
    };

    $scope.save = function () {
        $scope.policyRep.policyValue = JSON.stringify($scope.webauthnpolicy);

        Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function () {
            $route.reload();
            Notifications.success($translate.instant("policy-created-successfully"));
        });
    };

    $scope.delete = function () {
        Dialog.confirmDelete(policies.policyName, 'policy', function () {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: $scope.policyRep.id
            }, function () {
                $location.url("/realms/" + realm.realm + "/policies");
                Notifications.success($translate.instant("policy-name-removed-successfully"));
            });
        });
    };
});

module.controller('RealmDefaultRolesCtrl', function ($scope, $route, Realm, realm, roles, Notifications, ClientRole, Client, $translate) {

    $scope.realm = realm;

    $scope.availableRealmRoles = [];
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmDefRoles = [];

    $scope.availableClientRoles = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientDefRoles = [];

    if (!$scope.realm.hasOwnProperty('defaultRoles') || $scope.realm.defaultRoles === null) {
        $scope.realm.defaultRoles = [];
    }

    // Populate available roles. Available roles are neither already assigned
    for (var i = 0; i < roles.length; i++) {
        var item = roles[i].name;

        if ($scope.realm.defaultRoles.indexOf(item) < 0) {
            $scope.availableRealmRoles.push(item);
        }
    }

    $scope.addRealmDefaultRole = function () {

        // Remove selected roles from the Available roles and add them to realm default roles (move from left to right).
        for (var i = 0; i < $scope.selectedRealmRoles.length; i++) {
            var selectedRole = $scope.selectedRealmRoles[i];

            $scope.realm.defaultRoles.push(selectedRole);

            var index = $scope.availableRealmRoles.indexOf(selectedRole);
            if (index > -1) {
                $scope.availableRealmRoles.splice(index, 1);
            }
        }

        $scope.selectedRealmRoles = [];

        // Update/save the realm with new default roles.
        Realm.update($scope.realm, function () {
            Notifications.success($translate.instant("realm-roles-updated"));
        });
    };

    $scope.deleteRealmDefaultRole = function () {

        // Remove selected roles from the realm default roles and add them to available roles (move from right to left).
        for (var i = 0; i < $scope.selectedRealmDefRoles.length; i++) {
            $scope.availableRealmRoles.push($scope.selectedRealmDefRoles[i]);

            var index = $scope.realm.defaultRoles.indexOf($scope.selectedRealmDefRoles[i]);
            if (index > -1) {
                $scope.realm.defaultRoles.splice(index, 1);
            }
        }

        $scope.selectedRealmDefRoles = [];

        // Update/save the realm with new default roles.
        //var realmCopy = angular.copy($scope.realm);
        Realm.update($scope.realm, function () {
            Notifications.success($translate.instant("realm-roles-updated"));
        });
    };

    $scope.changeClient = function (client) {
        $scope.selectedClient = client;
        $scope.selectedClientRoles = [];
        $scope.selectedClientDefRoles = [];
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }

        // Populate available roles for selected client
        if ($scope.selectedClient) {
            ClientRole.query({ realm: $scope.realm.realm, client: $scope.selectedClient.id }, function (appDefaultRoles) {
                if (!$scope.selectedClient.hasOwnProperty('defaultRoles') || $scope.selectedClient.defaultRoles === null) {
                    $scope.selectedClient.defaultRoles = [];
                }

                $scope.availableClientRoles = [];
                for (var i = 0; i < appDefaultRoles.length; i++) {

                    var roleName = appDefaultRoles[i].name;
                    if ($scope.selectedClient.defaultRoles.indexOf(roleName) < 0) {
                        $scope.availableClientRoles.push(roleName);
                    }
                }
            });
        } else {
            $scope.availableClientRoles = null;
        }
    };

    $scope.addClientDefaultRole = function () {

        // Remove selected roles from the app available roles and add them to app default roles (move from left to right).
        for (var i = 0; i < $scope.selectedClientRoles.length; i++) {
            var role = $scope.selectedClientRoles[i];

            var idx = $scope.selectedClient.defaultRoles.indexOf(role);
            if (idx < 0) {
                $scope.selectedClient.defaultRoles.push(role);
            }

            idx = $scope.availableClientRoles.indexOf(role);

            if (idx != -1) {
                $scope.availableClientRoles.splice(idx, 1);
            }
        }

        $scope.selectedClientRoles = [];

        // Update/save the selected client with new default roles.
        delete $scope.selectedClient.text;
        Client.update({
            realm: $scope.realm.realm,
            client: $scope.selectedClient.id
        }, $scope.selectedClient, function () {
            Notifications.success($translate.instant("changes-saved-to-application"));
            Client.get({ realm: realm.realm, client: $scope.selectedClient.id }, function (response) {
                response.text = response.clientId;
                $scope.changeClient(response);
            });
        });
    };

    $scope.rmClientDefaultRole = function () {

        // Remove selected roles from the app default roles and add them to app available roles (move from right to left).
        for (var i = 0; i < $scope.selectedClientDefRoles.length; i++) {
            var role = $scope.selectedClientDefRoles[i];
            var idx = $scope.selectedClient.defaultRoles.indexOf(role);
            if (idx != -1) {
                $scope.selectedClient.defaultRoles.splice(idx, 1);
            }
            idx = $scope.availableClientRoles.indexOf(role);
            if (idx < 0) {
                $scope.availableClientRoles.push(role);
            }
        }

        $scope.selectedClientDefRoles = [];

        // Update/save the selected client with new default roles.
        delete $scope.selectedClient.text;
        Client.update({
            realm: $scope.realm.realm,
            client: $scope.selectedClient.id
        }, $scope.selectedClient, function () {
            Notifications.success($translate.instant("changes-saved-to-application"));
            Client.get({ realm: realm.realm, client: $scope.selectedClient.id }, function (response) {
                response.text = response.clientId;
                $scope.changeClient(response);
            });
        });
    };

    clientSelectControl($scope, $route.current.params.realm, Client);
});



module.controller('IdentityProviderTabCtrl', function (Dialog, $scope, Current, Notifications, $location, $translate) {
    $scope.removeIdentityProvider = function () {
        Dialog.confirmDelete($scope.identityProvider.alias, 'provider', function () {
            $scope.identityProvider.$remove({
                realm: Current.realm.realm,
                alias: $scope.identityProvider.alias
            }, function () {
                $location.url("/realms/" + Current.realm.realm + "/connections/social");
                Notifications.success($translate.instant("identity-provider-deleted"));
            });
        });
    };
});

module.controller('RealmSocialConnectionsCtrl', function ($scope, $filter, $upload, $http, $route, realm, instance, providerFactory, IdentityProvider, serverInfo, emailSenders, authFlows, $location, angularSlideOutPanel, Notifications, Dialog, $translate, Session, ConfirmExitDialog, ShowSecret) {

    if (realm.mode == "bo") {
        $location.path('/notfound');
        return;
    }
    $scope.init = function () {
        $scope.realm = angular.copy(realm);
        $scope.configuredProviders = angular.copy(realm.identityProviders);
        $scope.first = 0;
        $scope.limit = Session.getItem("socialConnectionFilterCount") ? Session.getItem("socialConnectionFilterCount") : 5;
        $scope.last = $scope.first + $scope.limit;
        $scope.perPage = 5;
        $scope.updateSliceArray();
        $scope.providerFactory = providerFactory;
    };
    $scope.showSecret = function (id) {
        Dialog.confirm($translate.instant('show-secret'), $translate.instant('reveal-secret-txt'), function () {
            $scope.reqBody = {
                providerFactory: providerFactory.providerClass,
                providerId: providerFactory.id
            };
            ShowSecret.show({
                realm: $route.current.params.realm,
            }, $scope.reqBody, function (secretData) {
                if (secretData[id]) {
                    instance.config[id] === $scope.identityProvider.config[id] ? $scope.identityProvider.config[id] = secretData[id] : '';
                    if (id === 'clientSecret') {
                        $scope.hideClientSecret = true;
                    }
                }
                $scope.hideShowPassword(id);
            });
        });
    };
    $scope.noEmailSender = $translate.instant('no-email-sender', { emailProviderLink: "#/realms/" + realm.realm + "/email/providers/create" });
    $scope.initSamlProvider = function () {
        $scope.nameIdFormats = [
            /*
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:transient",
                name: "Transient"
            },
            */
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
                name: "Persistent"

            },
            {
                format: "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                name: "Email"

            },
            {
                format: "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos",
                name: "Kerberos"

            },
            {
                format: "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName",
                name: "X.509 Subject Name"

            },
            {
                format: "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName",
                name: "Windows Domain Qualified Name"

            },
            {
                format: "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified",
                name: "Unspecified"

            }
        ];
        $scope.signatureAlgorithms = [
            "RSA_SHA1",
            "RSA_SHA256",
            "RSA_SHA512",
            "DSA_SHA1"
        ];
        $scope.xmlKeyNameTranformers = [
            "NONE",
            "KEY_ID",
            "CERT_SUBJECT"
        ];
        $scope.principalTypes = [
            {
                type: "SUBJECT",
                name: "Subject NameID"

            },
            {
                type: "ATTRIBUTE",
                name: "Attribute [Name]"

            },
            {
                type: "FRIENDLY_ATTRIBUTE",
                name: "Attribute [Friendly Name]"

            }
        ];
        if (instance && instance.alias) {

        } else {
            $scope.identityProvider.config.nameIDPolicyFormat = $scope.nameIdFormats[0].format;
            $scope.identityProvider.config.principalType = $scope.principalTypes[0].type;
            $scope.identityProvider.config.signatureAlgorithm = $scope.signatureAlgorithms[1];
            $scope.identityProvider.config.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[1];
        }
    };

    $scope.hidePassword = true;
    $scope.fromUrl = {
        data: ''
    };

    if (instance && instance.alias) {
        $scope.identityProvider = angular.copy(instance);
        $scope.identityProvider.setupGuideLink = providerFactory.setupGuideLink;
        $scope.hideClientSecret = $scope.newIdentityProvider = false;
        for (var i in serverInfo.identityProviders) {
            var provider = serverInfo.identityProviders[i];

            if (provider.id == instance.providerId) {
                $scope.provider = provider;
            }
        }
    } else {
        $scope.identityProvider = {};
        $scope.identityProvider.config = {};
        $scope.identityProvider.alias = providerFactory.id;
        $scope.identityProvider.providerId = providerFactory.id;
        $scope.identityProvider.setupGuideLink = providerFactory.setupGuideLink;

        $scope.identityProvider.enabled = true;
        $scope.identityProvider.authenticateByDefault = false;
        $scope.identityProvider.firstBrokerLoginFlowAlias = 'first broker login';
        $scope.identityProvider.config.useJwksUrl = true;
        $scope.hideClientSecret = $scope.newIdentityProvider = true;
    }

    let oldCopy = angular.copy($scope.identityProvider);

    $scope.$watch('identityProvider', function () {
        if (!angular.equals($scope.identityProvider, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.serverInfo = serverInfo;

    $scope.allProviders = angular.copy(serverInfo.identityProviders);



    $scope.hideShowPassword = function (id) {
        if ($(`#${id}_show_hide_password input`).attr("type") === "text") {
            $(`#${id}_show_hide_password input`).attr('type', 'password');
            $(`#${id}_show_hide_password .hide_show span`).addClass("hidden_outlined");
            $(`#${id}_show_hide_password .hide_show span`).removeClass("visible_outlined");
        } else if ($(`#${id}_show_hide_password input`).attr("type") === "password") {
            $(`#${id}_show_hide_password input`).attr('type', 'text');
            $(`#${id}_show_hide_password .hide_show span`).removeClass("hidden_outlined");
            $(`#${id}_show_hide_password .hide_show span`).addClass("visible_outlined");
        }
    };

    $scope.emailProviders = [];
    if (!angular.equals({}, emailSenders)) {
        for (const emailProvider of emailSenders) {
            $scope.emailProviders.push(emailProvider);
        }
    }

    $scope.authFlows = [];
    for (var i = 0; i < authFlows.length; i++) {
        if (authFlows[i].providerId == 'basic-flow') {
            $scope.authFlows.push(authFlows[i]);
        }
    }

    $scope.postBrokerAuthFlows = [];
    for (var i = 0; i < $scope.authFlows.length; i++) {
        $scope.postBrokerAuthFlows.push($scope.authFlows[i]);
    }

    $scope.$watch(function () {
        return $location.path();
    }, function () {
        $scope.path = $location.path().substring(1).split("/");
    });


    $scope.files = [];
    $scope.importFile = false;
    $scope.importUrl = false;

    $scope.onFileSelect = function ($files) {
        $scope.importFile = true;
        $scope.files = $files;
    };

    $scope.clearFileSelect = function () {
        $scope.importUrl = false;
        $scope.importFile = false;
        $scope.files = null;
    };

    var setConfig = function (data) {
        for (var key in data) {
            $scope.identityProvider.config[key] = data[key];
        }
    };

    $scope.uploadFile = function () {
        if (!$scope.identityProvider.alias) {
            Notifications.error($translate.instant("specify-alias-error"));
            return;
        }
        var input = {
            providerId: providerFactory.id
        };
        //$files: an array of files selected, each file has name, size, and type.
        for (var i = 0; i < $scope.files.length; i++) {
            var $file = $scope.files[i];
            $scope.upload = $upload.upload({
                url: authUrl + '/admin/realms/' + realm.realm + '/identity-provider/import-config',
                // method: POST or PUT,
                // headers: {'headerKey': 'headerValue'}, withCredential: true,
                data: input,
                file: $file
                /* set file formData name for 'Content-Desposition' header. Default: 'file' */
                //fileFormDataName: myFile,
                /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
                //formDataAppender: function(formData, key, val){}
            }).progress(function (evt) {
                console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
            }).then(function (response) {
                setConfig(response.data);
                $scope.clearFileSelect();
                Notifications.success($translate.instant("idp-metadata-loaded"));
            }).catch(function () {
                Notifications.error($translate.instant("localization-file.upload.error"));
            });
        }
    };

    $scope.importFrom = function () {
        if (!$scope.identityProvider.alias) {
            Notifications.error($translate.instant("specify-alias-error"));
            return;
        }
        var input = {
            fromUrl: $scope.fromUrl.data,
            providerId: providerFactory.id
        };
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/identity-provider/import-config', input)
            .then(function (response) {
                setConfig(response.data);
                $scope.fromUrl.data = '';
                $scope.importUrl = false;
                Notifications.success($translate.instant("imported-config-url"));
            }).catch(function () {
                Notifications.error($translate.instant("config-not-imported-error"));
            });
    };
    $scope.$watch('fromUrl.data', function (newVal, oldVal) {
        if ($scope.fromUrl.data && $scope.fromUrl.data.length > 0) {
            $scope.importUrl = true;
        } else {
            $scope.importUrl = false;
        }
    });

    $scope.$watch('configuredProviders', function (configuredProviders) {
        if (configuredProviders) {
            $scope.configuredProviders = angular.copy(configuredProviders);

            for (var j = 0; j < configuredProviders.length; j++) {
                var configProvidedId = configuredProviders[j].providerId;

                for (var i in $scope.allProviders) {
                    var provider = $scope.allProviders[i];
                    if (provider.id == configProvidedId) {
                        configuredProviders[j].provider = provider;
                    }
                }
            }
            $scope.configuredProviders = angular.copy(configuredProviders);
        }
    }, true);

    $scope.callbackUrl = realm.workspace + "/auth/realms/" + realm.realm + "/broker/";

    $scope.addProvider = function (provider) {
        $location.url("/create/identity-provider/" + realm.realm + "/" + provider.id);
    };
    $scope.realm = angular.copy(realm);
    $scope.configuredProviders = angular.copy(realm.identityProviders);
    $scope.openRedirect = false;
    $scope.save = function () {
        if ($scope.newIdentityProvider) {
            if (!$scope.identityProvider.alias) {
                Notifications.error($translate.instant("specify-alias-error"));
                return;
            }
            IdentityProvider.save({
                realm: $scope.realm.realm, alias: ''
            }, $scope.identityProvider, function () {
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/connections/social/settings/" + $scope.identityProvider.providerId + "/" + $scope.identityProvider.alias);
                Notifications.success($translate.instant('social-provider-created', { name: $scope.identityProvider.alias }));

            });
        } else {
            IdentityProvider.update({
                realm: $scope.realm.realm,
                alias: $scope.identityProvider.alias
            }, $scope.identityProvider, function () {
                $scope.openRedirect = true;
                $route.reload();
                Notifications.success($translate.instant($translate.instant('social-provider-updated'), { name: $scope.identityProvider.alias }));
            });
        }
    };

    $scope.cancel = function () {
        if ($scope.newIdentityProvider) {
            $location.url("/realms/" + realm.realm + "/connections/social");
        } else {
            $scope.openRedirect = true;
            $route.reload();
        }
    };


    $scope.reset = function () {
        $scope.identityProvider = {};
        $scope.configuredProviders = angular.copy($scope.realm.identityProviders);
    };

    $scope.showPassword = function (flag) {
        $scope.hidePassword = flag;
    };

    $scope.removeIdentityProvider = function (identityProvider) {
        Dialog.confirmDelete(identityProvider.alias, 'provider', function () {
            IdentityProvider.remove({
                realm: realm.realm,
                alias: identityProvider.alias
            }, function () {
                $scope.openRedirect = true;
                $route.reload();
                Notifications.success($translate.instant("identity-provider-deleted"));
            });
        });
    };

    // KEYCLOAK-5932: remove social providers that have already been defined
    function removeUsedSocial() {
        var i = $scope.allProviders.length;
        while (i--) {
            if ($scope.allProviders[i].groupName !== 'Social') continue;
            if ($scope.configuredProviders != null) {
                for (var j = 0; j < $scope.configuredProviders.length; j++) {
                    if ($scope.configuredProviders[j].providerId === $scope.allProviders[i].id) {
                        $scope.allProviders.splice(i, 1);
                        break;
                    }
                }
            }
        }
    };

    //Search
    $scope.searchKey = "";

    $scope.findConfigured = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const configured of $scope.configuredProviders) {
                if (configured.alias.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
    //social-connection pagination
    $scope.selectQuery = function (limit) {
        $scope.limit = limit;
        Session.setItem("socialConnectionFilterCount", $scope.limit);
        $scope.first = 0;
        $scope.last = $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.firstPage = function () {
        $scope.first = 0;
        $scope.last = $scope.first + $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.next = function () {
        $scope.first += $scope.limit;
        $scope.last += $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.previous = function () {
        $scope.first -= $scope.limit;
        $scope.last -= $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.updateSliceArray = function () {
        $scope.slicedSocialConnection = $scope.configuredProviders.slice($scope.first, $scope.last);
    };

    $scope.$on('$routeChangeStart', function (event, newUrl, oldUrl) {
        let oldUrlPath = oldUrl.$$route.originalPath.substring(1).split("/");
        if ((oldUrlPath[4] === "settings" || oldUrlPath[4] === "create-conn") && !$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
    //Open SetupGuide Panel
    $scope.openSetupGuide = function (providerId, setupGuideLink) {
        var controller = function ($scope, $location, $route) {
            $scope.realm = realm;
            $scope.providerId = providerId;
            $scope.resourceUrl = resourceUrl;
            $scope.howToDo = setupGuideLink; //getValidDomainForMinio(location.origin)+"/"+
            $scope.ishowTo = true;
            $scope.count = 0;

            $scope.dismissPanel = function () {
                $scope.$panelInstance.dismiss();
            };

            $scope.mdTrigger = function () {
                return null;
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });
        };

        $http.get(resourceUrl + '/partials/panels/sc-setup-guide-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$route',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                    console.log('panel was closed with result : ', result);
                }).catch(function (error) {
                    console.log('panel was rejected with error : ', error);
                });
        });
    };

});

module.controller('DirectorySyncConnectionsCtrl', function ($scope, $route, realm, $location, directorySyncList, serverInfo, DirectorySync, RealmDirectorySyncCreateDialog, DirectorySyncRedirect, $location, DirectorySyncDelete, Dialog, $route, Notifications, $translate) {
    $scope.init = function () {
        $scope.realm = angular.copy(realm);
        $scope.configuredProviders = angular.copy(directorySyncList.body);
    };

    $scope.searchKey = false;
    $scope.realm = angular.copy(realm);

    $scope.deleteConnection = function (connection) {
        if (connection?.id) {
            Dialog.confirmDelete(connection.name, $translate.instant('user-sync'), function () {
                DirectorySyncDelete.remove(
                    {
                        realm: $scope.realm.realm,
                        connectionId: connection.id
                    },
                    function (data) {
                        if (data.status !== 200) {
                            Notifications.error($translate.instant(data?.body?.message));
                        } else {
                            Notifications.success($translate.instant("user-sync-deleted"));
                            $route.reload();
                        }
                    },
                    function (err) {
                        Notifications.error($translate.instant(err?.message));
                    }
                );
            });
        }
    };
});

module.controller('DirectorySyncConnectionsManageCtrl', function ($scope, $route, realm, $location, directorySyncInfo, availableUserSyncs, CancelDirectorySync, serverInfo, DirectorySyncUserData, directorySyncHistoryList, DirectorySyncRedirect, DirectorySyncStatus, $route, Notifications, DirectorySyncDelete, Dialog, DirectorySyncHistory, RealmDirectorySyncCreateDialog, DirectorySyncUpdate, $translate) {
    $scope.directoryProviders = availableUserSyncs;

    $scope.init = function () {
        $scope.realm = angular.copy(realm);
        $scope.connectionsHistory = directorySyncHistoryList.body;
        $scope.currentProvider = $scope.directoryProviders.find(
            (data) => data === $route.current.params.provider_id
        );
        $scope.currentConnection = directorySyncInfo.body;
        $scope.isDirectoryLoggedIn = $scope.currentConnection.tokenPresents;
        $scope.directorySyncProgress = {
            syncing: false,
            logging: false,
            checking: false,
            deleting: false,
            triggerId: '',
            message: ''
        };
        if ($scope.isDirectoryLoggedIn) {
            $scope.checkSyncStatus();
        }
    };

    $scope.searchKey = false;
    $scope.realm = angular.copy(realm);

    $scope.sync = function () {
        $scope.directorySyncProgress.syncing = true;
        DirectorySyncUserData.sync(
            {
                realm: $scope.realm.realm,
                providerId: $scope.currentProvider,
                connectionId: $route.current.params.connection_id
            },
            function (data) {
                if (data.status === 200 || data.status === 201) {
                    setTimeout(function () {
                        $scope.directorySyncProgress.triggerId = data.body.triggererId;
                        $scope.checkSyncStatus(function () {
                            $scope.directorySyncProgress.syncing = false;
                            Notifications.success($translate.instant("directory-sync-in-progress"));
                        });
                    }, 2000);
                } else {
                    Notifications.error($translate.instant(data.body.message));
                    $scope.directorySyncProgress.syncing = false;
                }
            },
            function (err) {
                $scope.directorySyncProgress.syncing = false;
                Notifications.error($translate.instant(err?.message));
            }
        );
    };

    $scope.checkSyncStatus = function (callback) {
        let reqBody = {
            realm: $scope.realm.realm,
            providerId: $scope.currentProvider,
            connectionId: $route.current.params.connection_id
        };
        if ($scope.directorySyncProgress.triggerId) {
            reqBody = { ...reqBody, trigger_id: $scope.directorySyncProgress.triggerId };
        }
        $scope.directorySyncProgress.checking = true;
        DirectorySyncStatus.get(
            reqBody,
            function (data) {
                if (data.status === 200) {
                    if (data?.body?.message) {
                        $scope.directorySyncProgress.message = data.body.message;
                    } else {
                        $route.reload();
                    }
                } else {
                    $scope.updateHistory();
                    $scope.directorySyncProgress.message = '';
                }
                $scope.directorySyncProgress.checking = false;
                if (callback) {
                    callback();
                }
            },
            function (err) {
                $scope.directorySyncProgress.checking = false;
                if (callback) {
                    callback();
                }
            }
        );
    };

    let resetAfterCancel = function () {
        $scope.directorySyncProgress.triggerId = "";
        $scope.directorySyncProgress.message = '';
        $scope.directorySyncProgress.cancelling = false;
    };

    $scope.cancelSync = function (callback) {
        let reqBody = {
            realm: $scope.realm.realm,
            providerId: $scope.currentProvider,
            connectionId: $route.current.params.connection_id
        };
        $scope.directorySyncProgress.cancelling = true;
        CancelDirectorySync.get(reqBody, function (data) {
            if (data.status === 200) {
                Notifications.success($translate.instant(data?.body?.message));
            } else {
                Notifications.error($translate.instant(data?.body?.message));
            }
            $route.reload();
        }, function (err) {
            Notifications.error($translate.instant(err.errorMessage));
            $route.reload();
        });
    };
    $scope.deleteConnection = function () {
        Dialog.confirmDelete(
            $scope.currentConnection.name,
            "user-sync",
            function () {
                $scope.directorySyncProgress.deleting = true;
                DirectorySyncDelete.remove(
                    {
                        realm: $scope.realm.realm,
                        providerId: $scope.currentProvider,
                        connectionId: $route.current.params.connection_id
                    },
                    function (data) {
                        if (data.status !== 200) {
                            $scope.directorySyncProgress.deleting = false;
                            Notifications.error($translate.instant(data?.body?.message));
                        } else {
                            $scope.directorySyncProgress.deleting = false;
                            Notifications.success($translate.instant("user-sync-deleted"));
                            $location.path(
                                `/realms/${$scope.realm.realm}/users/directory-sync/connections`
                            );
                        }
                    },
                    function (err) {
                        $scope.directorySyncProgress.deleting = false;
                        Notifications.error($translate.instant(err?.message));
                    }
                );
            }
        );
    };

    $scope.loginDirectory = function () {
        $scope.directorySyncProgress.logging = true;
        DirectorySyncRedirect.get({ realm: $scope.realm.realm, providerId: $scope.currentProvider, connectionId: $scope.currentConnection.id }, function (data) {
            if (data.status !== 200) {
                $scope.directorySyncProgress.logging = false;
                Notifications.error($translate.instant(data?.body?.message));
            } else {
                window.location.href = data.body;
                $scope.directorySyncProgress.logging = false;
            }
        }, function (err) {
            $scope.directorySyncProgress.logging = false;
            Notifications.error($translate.instant(err?.message));
        });
    };

    $scope.edit = function () {
        RealmDirectorySyncCreateDialog.open(
            $translate.instant('update-connection', { providerId: $translate.instant($scope.currentConnection.providerId + "_name") }),
            { 'providerId': $scope.currentConnection.providerId },
            function (connectionDetails) {
                let createConnPayload = {
                    name: connectionDetails.connectionName,
                    enrollmentOption: connectionDetails.connectionType,
                    syncConfig: connectionDetails.syncConfig
                };
                DirectorySyncUpdate.update(
                    {
                        realm: $scope.realm.realm,
                        providerId: $scope.currentProvider,
                        connectionId: $route.current.params.connection_id
                    },
                    createConnPayload,
                    function (data) {
                        if (data.status !== 200) {
                            Notifications.error($translate.instant(data?.body?.message));
                        } else {
                            Notifications.success($translate.instant("user-sync-updated-successfully"));
                            $route.reload();
                        }
                    },
                    function (err) {
                        Notifications.error($translate.instant(err?.message ? err.message : err));
                    }
                );
            },
            $scope.currentConnection
        );
    };

    $scope.updateHistory = function () {
        DirectorySyncHistory.get({ realm: $scope.realm.realm, providerId: $scope.currentProvider, connectionId: $scope.currentConnection.id }, function (data) {
            if ($scope.connectionsHistory.length !== data.body.length) {
                $scope.connectionsHistory = data.body;
            }
        });
    };
});

module.controller('CreateSocialConnectionsCtrl', function ($scope, $filter, $upload, $http, $route, realm, instance, providerFactory, IdentityProvider, serverInfo, authFlows, $location, Notifications, Dialog) {
    $scope.realm = angular.copy(realm);

    if (realm.mode == 'bo' ){
        $location.url("/notfound");
    }

    $scope.searchKey = "";

    $scope.serverInfo = serverInfo;

    $scope.allProviders = angular.copy(serverInfo.identityProviders);
    $scope.socialProviders = $scope.allProviders;

    $scope.configuredProviders = angular.copy(realm.identityProviders);

    removeUsedSocial();

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };
    $scope.findConnections = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const conn of $scope.socialProviders) {
                if (conn.name.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };
    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };

    $scope.hideShowPassword = function () {
        if ($('#show_hide_password input').attr("type") === "text") {
            $('#show_hide_password input').attr('type', 'password');
            $('#show_hide_password .hide_show span').addClass("hidden_outlined");
            $('#show_hide_password .hide_show span').removeClass("visible_outlined");
        } else if ($('#show_hide_password input').attr("type") === "password") {
            $('#show_hide_password input').attr('type', 'text');
            $('#show_hide_password .hide_show span').removeClass("hidden_outlined");
            $('#show_hide_password .hide_show span').addClass("visible_outlined");
        }
    };

    //used to remove the configured connections
    function removeUsedSocial() {
        var i = $scope.socialProviders.length;
        while (i--) {
            if ($scope.configuredProviders != null) {
                for (var j = 0; j < $scope.configuredProviders.length; j++) {
                    if ($scope.configuredProviders[j].providerId === $scope.socialProviders[i].id) {
                        $scope.socialProviders.splice(i, 1);
                        break;
                    }
                }
            }
        }
    };

});
module.controller('DirectorySyncListCtrl', function ($scope, realm, availableUserSyncs, RealmDirectorySyncCreateDialog, DirectorySync, $location, Notifications, $translate) {
    $scope.realm = angular.copy(realm);

    $scope.searchKey = "";
    $scope.directoryProviders = availableUserSyncs;

    // $scope.changeSearchKey = function (val) {
    //     $scope.searchKey = val;
    // };

    // $scope.findConnections = function (searchValue) {
    //     if (searchValue && searchValue.trim()) {
    //         for (const conn of $scope.directoryProviders) {
    //             if (conn.name.toLowerCase().includes(searchValue.toLowerCase())) {
    //                 return true;
    //             }
    //         }
    //         return false;
    //     }
    //     return true;
    // };

    // $scope.clearSearch = function () {
    //     $scope.search = "";
    //     $scope.changeSearchKey($scope.search);
    // };s

    $scope.save = function (providerId) {
        RealmDirectorySyncCreateDialog.open(
            $translate.instant('add-new-user-sync', { providerId: $translate.instant(providerId + "_name") }),
            { 'providerId': providerId },
            function (connectionDetails) {
                let createConnPayload = {
                    name: connectionDetails.connectionName,
                    enrollmentOption: connectionDetails.connectionType,
                    providerId: providerId,
                    syncConfig: connectionDetails.syncConfig
                };
                DirectorySync.create({ realm: $scope.realm.realm }, createConnPayload, function (data) {
                    if (data.status !== 200) {
                        Notifications.error($translate.instant(data?.body?.message));
                    } else {
                        Notifications.success($translate.instant("user-sync-created-successfully"));
                        $location.path(`/realms/${$scope.realm.realm}/users/directory-sync/connections/${providerId}/${data.body.id}`);
                    }
                }, function (err) {
                    Notifications.error($translate.instant(err?.message ? err.message : err));
                });
            }
        );

    };
    // $scope.save = function () {
    //     RealmDirectorySyncCreateDialog.open($translate.instant('add-new-user-sync'), function (connectionDetails) {
    //         let createConnPayload = {
    //             name: connectionDetails.connectionName,
    //             enrollmentOption: connectionDetails.connectionType,
    //             syncConfig: {
    //                 "mapperType": connectionDetails.mapperType
    //             }
    //         };
    //         DirectorySync.create({ realm: $scope.realm.realm, providerId: "azureadsync" }, createConnPayload, function (data) {
    //             if (data.status !== 200) {
    //                 Notifications.error(data?.body?.message);
    //             } else {
    //                 Notifications.success($translate.instant("user-sync-created-successfully"));
    //                 $location.path(`/realms/${$scope.realm.realm}/users/directory-sync/connections/azureadsync/${data.body.id}`);
    //             }
    //         }, function (err) {
    //             Notifications.error(err?.message ? err.message : err);
    //         });
    //     });
    // };
});
module.controller('CreateCustomSocialConnectionsCtrl', function ($scope, $filter, $upload, $http, $route, realm, instance, providerFactory, IdentityProvider, serverInfo, authFlows, $location, Notifications, Dialog) {
    $scope.realm = angular.copy(realm);

    $scope.serverInfo = serverInfo;

    $scope.allProviders = angular.copy(serverInfo.identityProviders);
    $scope.customProviders = $scope.allProviders.filter((prov) => prov.groupName === "User-defined");
    $scope.configuredProviders = angular.copy(realm.identityProviders);

    $scope.hideShowPassword = function () {
        if ($('#show_hide_password input').attr("type") === "text") {
            $('#show_hide_password input').attr('type', 'password');
            $('#show_hide_password .hide_show span').addClass("hidden_outlined");
            $('#show_hide_password .hide_show span').removeClass("visible_outlined");
        } else if ($('#show_hide_password input').attr("type") === "password") {
            $('#show_hide_password input').attr('type', 'text');
            $('#show_hide_password .hide_show span').removeClass("hidden_outlined");
            $('#show_hide_password .hide_show span').addClass("visible_outlined");
        }
    };

});
module.controller('RealmIdentityProviderExportCtrl', function (realm, identityProvider, $scope, $http, IdentityProviderExport) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.download = null;
    $scope.exported = "";
    $scope.exportedType = "";

    var url = IdentityProviderExport.url({ realm: realm.realm, alias: identityProvider.alias });
    $http.get(url).then(function (response) {
        $scope.exportedType = response.headers('Content-Type');
        $scope.exported = response.data;
    });

    $scope.download = function () {
        var suffix = "txt";
        if ($scope.exportedType == 'application/xml') {
            suffix = 'xml';
        } else if ($scope.exportedType == 'application/json') {
            suffix = 'json';
        }
        saveAs(new Blob([$scope.exported], { type: $scope.exportedType }), 'keycloak.' + suffix);
    };
});

module.controller('RealmTokenDetailCtrl', function ($scope, Realm, realm, $http, $location, $route, Dialog, Notifications, TimeUnit, TimeUnit2, serverInfo, ConfirmExitDialog, $translate) {
    $scope.realm = realm;
    $scope.stage = 1;
    $scope.serverInfo = serverInfo;
    $scope.actionTokenProviders = $scope.serverInfo.providers.actionTokenHandler.providers;
    $scope.subscribedAuthentication = realm.attributes.subscriptionNeedsAuthenticationProduct ? JSON.parse(realm.attributes.subscriptionNeedsAuthenticationProduct) : false;

    $scope.realm.accessTokenLifespan = TimeUnit2.asUnit(realm.accessTokenLifespan);
    $scope.realm.accessTokenLifespanForImplicitFlow = TimeUnit2.asUnit(realm.accessTokenLifespanForImplicitFlow);
    $scope.realm.ssoSessionIdleTimeout = TimeUnit2.asUnit(realm.ssoSessionIdleTimeout);
    $scope.realm.ssoSessionMaxLifespan = TimeUnit2.asUnit(realm.ssoSessionMaxLifespan);
    $scope.realm.ssoSessionIdleTimeoutRememberMe = TimeUnit2.asUnit(realm.ssoSessionIdleTimeoutRememberMe);
    $scope.realm.ssoSessionMaxLifespanRememberMe = TimeUnit2.asUnit(realm.ssoSessionMaxLifespanRememberMe);
    $scope.realm.offlineSessionIdleTimeout = TimeUnit2.asUnit(realm.offlineSessionIdleTimeout);
    // KEYCLOAK-7688 Offline Session Max for Offline Token
    $scope.realm.offlineSessionMaxLifespan = TimeUnit2.asUnit(realm.offlineSessionMaxLifespan);
    $scope.realm.accessCodeLifespan = TimeUnit2.asUnit(realm.accessCodeLifespan);
    $scope.realm.accessCodeLifespanLogin = TimeUnit2.asUnit(realm.accessCodeLifespanLogin);
    $scope.realm.accessCodeLifespanUserAction = TimeUnit2.asUnit(realm.accessCodeLifespanUserAction);
    $scope.realm.actionTokenGeneratedByAdminLifespan = TimeUnit2.asUnit(realm.actionTokenGeneratedByAdminLifespan);
    $scope.realm.actionTokenGeneratedByUserLifespan = TimeUnit2.asUnit(realm.actionTokenGeneratedByUserLifespan);
    $scope.realm.attributes = realm.attributes;

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;
    $scope.openRedirect = false;

    $scope.toggleSideBarShow = function () {
        var sidebarMenu = document.getElementById('setting-sidebar');
        var sidebarIcon = document.getElementById('setting-sidebar-icon');
        if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
            sidebarMenu.classList.remove("bo-setting-sidemenu");
            sidebarMenu.classList.add("bo-setting-sidemenu-show");
            sidebarIcon.classList.add('d-none');
            sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
            $scope.isMobileWView = true;
        }else{
            sidebarMenu.classList.add("bo-setting-sidemenu");
            sidebarMenu.classList.remove("bo-setting-sidemenu-show");
            sidebarIcon.classList.remove('d-none');
            sidebarIcon.classList.add('bo-setting-sidemenu-icon');
            $scope.isMobileWView = false;
        }
    }

    $scope.$watch('realm', function () {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.$watch('actionLifespanId', function () {
        // changedActionLifespanId signals other watchers that we were merely
        // changing the dropdown and we should not enable 'save' button
        if ($scope.actionTokenAttribute && $scope.actionTokenAttribute.hasOwnProperty('time') && $scope.actionTokenAttribute.time) {
            $scope.changedActionLifespanId = true;
        }
        $scope.actionTokenAttribute = TimeUnit2.asUnit($scope.realm.attributes['actionTokenGeneratedByUserLifespan.' + $scope.actionLifespanId]);
    }, true);

    $scope.$watch('actionTokenAttribute', function () {
        if ($scope.actionLifespanId === null) return;

        if ($scope.changedActionLifespanId) {
            $scope.changedActionLifespanId = false;
            return;
        } else {
            $scope.changed = true;
        }

        if ($scope.actionTokenAttribute !== null) {
            $scope.realm.attributes['actionTokenGeneratedByUserLifespan.' + $scope.actionLifespanId] = $scope.actionTokenAttribute.toSeconds();
        }
    }, true);

    $scope.changeRevokeRefreshToken = function () {

    };

    $scope.save = function () {
        $scope.stage = 2;
        $('#cancelBtn').attr('disabled', 'disabled');
        $scope.realmCopy = angular.copy($scope.realm);
        var realmCopy = {
            ...$scope.realmCopy, accessTokenLifespan: $scope.realm.accessTokenLifespan.toSeconds(),
            accessTokenLifespanForImplicitFlow: $scope.realm.accessTokenLifespanForImplicitFlow.toSeconds(),
            ssoSessionIdleTimeout: $scope.realm.ssoSessionIdleTimeout.toSeconds(),
            ssoSessionMaxLifespan: $scope.realm.ssoSessionMaxLifespan.toSeconds(),
            ssoSessionIdleTimeoutRememberMe: $scope.realm.ssoSessionIdleTimeoutRememberMe.toSeconds(),
            ssoSessionMaxLifespanRememberMe: $scope.realm.ssoSessionMaxLifespanRememberMe.toSeconds(),
            offlineSessionIdleTimeout: $scope.realm.offlineSessionIdleTimeout.toSeconds(),
            offlineSessionMaxLifespan: $scope.realm.offlineSessionMaxLifespan.toSeconds(),
            accessCodeLifespan: $scope.realm.accessCodeLifespan.toSeconds(),
            accessCodeLifespanUserAction: $scope.realm.accessCodeLifespanUserAction.toSeconds(),
            accessCodeLifespanLogin: $scope.realm.accessCodeLifespanLogin.toSeconds(),
            actionTokenGeneratedByAdminLifespan: $scope.realm.actionTokenGeneratedByAdminLifespan.toSeconds(),
            actionTokenGeneratedByUserLifespan: $scope.realm.actionTokenGeneratedByUserLifespan.toSeconds()
        };
        delete realmCopy.$promise;
        delete realmCopy.$resolved;
        Realm.update(realmCopy, function () {
            $scope.stage = 1;
            $('#cancelBtn').removeAttr('disabled', 'disabled');
            $scope.openRedirect = true;
            $route.reload();
            Notifications.success($translate.instant('changes-saved'));
        }, function () {
            $scope.stage = 1;
            $('#cancelBtn').removeAttr('disabled', 'disabled');
        });
    };

    $scope.resetToDefaultToken = function (actionTokenId) {
        $scope.actionTokenAttribute = {};
        delete $scope.realm.attributes['actionTokenGeneratedByUserLifespan.' + $scope.actionLifespanId];
        //Only for UI effects, resets to the original state
        $scope.actionTokenAttribute.unit = 'Minutes';
    };

    $scope.reset = function () {
        $scope.openRedirect = true;
        $route.reload();
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('ViewKeyCtrl', function ($scope, key, $modalInstance, $translate) {
    $scope.key = key;
    $scope.tooltipTxt = $translate.instant("copy");
    $scope.init = function () {
        $(".key-copy-icon").click(function () {
            $scope.tooltipTxt = $translate.instant("copied");
            $(".key-copy-icon").mouseleave(function () {
                $scope.tooltipTxt = $translate.instant("copy");
            });
        });
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
    $scope.$on('$routeChangeStart', function () {
        $modalInstance.close();
    });

    $scope.copyToClipboard = function (element) {
        var $temp = $("<input>");
        $("body").append($temp);
        $temp.val($(element).text().trim()).select();
        document.execCommand("copy");
        $temp.remove();
    };
});

module.controller('RealmKeysCtrl', function ($scope, Realm, realm, $http, $route, $location, Dialog, Notifications, serverInfo, keys, Components, $modal, $translate) {
    $scope.realm = angular.copy(realm);
    $scope.loaderTypeName = 'tableListLoader';
    $scope.keys = keys.keys;
    $scope.active = {};
    $scope.valueChanged = false;
    $scope.publickey = $translate.instant("public-key-hover");
    $scope.certificateTxt = $translate.instant("certificate-hover");
    $scope.providers = serverInfo.componentTypes['org.keycloak.keys.KeyProvider'];
    $scope.queryFinished = false;

    QuerySearch();
    $scope.addProvider = function (provider) {
        $location.url("/create/keys/" + realm.realm + "/providers/" + provider.id);
    };
    function QuerySearch() {
        Components.query({
            realm: realm.realm,
            parent: realm.id,
            type: 'org.keycloak.keys.KeyProvider'
        }, function (data) {
            $scope.queryFinished = true;
            for (var i = 0; i < keys.keys.length; i++) {
                for (var j = 0; j < data.length; j++) {
                    if (keys.keys[i].providerId == data[j].id) {
                        keys.keys[i].provider = data[j];
                    }
                }
            }

            for (var t in keys.active) {
                for (var i = 0; i < keys.keys.length; i++) {
                    if (keys.active[t] == keys.keys[i].kid) {
                        $scope.active[t] = keys.keys[i];
                    }
                }
            }
        });
    }
    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.searchValue = "";
        $scope.queryFinished = false;
        QuerySearch();
    };
    $scope.changeSearch = function () {
        $scope.search = $("#search").val();
    };
    $scope.viewKey = function (key) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-key.html',
            controller: 'ViewKeyCtrl',
            backdrop: 'static',
            resolve: {
                key: function () {
                    return key;
                }
            }
        });
    };
});

module.controller('BlackListCtrl', function ($scope, realm, $route, Notifications, $translate, RealmBlackList, BlackListingModal, ConfirmExitDialog, $location) {
    $scope.realm = angular.copy(realm);
    $scope.blackList = [];
    $scope.blackListArray = [];
    $scope.changed = false;
    $scope.isEnabled = {};
    $scope.loaded = false;
    $scope.canRedirected = true;
    $scope.selectedInput = "";
    $scope.iti= "";

    RealmBlackList.query({ realm: realm.realm }, function(data){
        $scope.blackList = data;
        $scope.isEnabled = $scope.blackList[0];
        $scope.isEnabled.defaultValue = $scope.isEnabled.defaultValue == "true" ? true : false;
        $scope.initialIsEnabled = angular.copy($scope.isEnabled);
        $scope.initialValue = $scope.blackList.map(list => {
            if (list.type === 'String') {
                const value = list.defaultValue && list?.defaultValue !== "" ? list.defaultValue.split("##") : null;
                return ({...list, defaultValue: value})
            } else {
                return list;
            }
        });
        $scope.initialValue = $scope.initialValue.
            map(value => ({...value, defaultValue: value.defaultValue ? value.defaultValue : []}));
        $scope.blackListArray =  angular.copy($scope.initialValue);
        $scope.loaded = true;
    })

    $scope.openModal = (value, index) => { BlackListingModal.open(value, index, $scope.removeValue)}

    $scope.setSelectedInput = (name) => {
        $scope.selectedInput = name;
    }

    $scope.handleEnter = (event, index) => {
        if (event.key === "Enter") {
            $scope.addValue(index);
        }
    }

    $scope.handleKeyPress = (event, name) => {
        if(!preventSpecialChars(String.fromCharCode(event.which), name, event.target.value.length)) {
            event.preventDefault();
        }
    }

    const preventSpecialChars = (value, name, valueLen)=> {
        let isValid = true;
        let pattern = "";
        let length = 0;
        switch (name) {
            case "ip-address":
                pattern = /^[a-zA-Z0-9:.]+$/;
                length = 38;
                break;
            case "phone":
                pattern = /^[0-9]+$/;
                length = 14;
                break;
            case "email":
                pattern = "";
                length = 99;
                break;
            case "username":
                pattern = "";
                length = 99;
                break;
            case "bl-location":
                pattern = /^[0-9:-]+$/;
                length = 18;
                break;
            default:
                pattern = /^[a-zA-Z]+$/;
                length = 19;
                break;
        }

        if ((pattern && !pattern.test(value)) || length < valueLen) {
            isValid = false;
        }

        return isValid;
    }
    $scope.addValue = (index) => {
        if ($scope.blackList[index].name === 'phone') {
            if (!$scope.iti.isValidNumber()) {
                Notifications.error($translate.instant("invalid-phone-number"));
                return;
            }
            $scope.blackList[index].newValue = $scope.iti.getSelectedCountryData().dialCode + $scope.blackList[index].newValue;
        }
        if(!preventSpecialChars($scope.blackList[index].newValue, $scope.blackList[index].name, `${$scope.blackList[index].newValue}`.length - 1)) {
            Notifications.error($translate.instant("invalid_value"));
            return;
        }
        if ($scope.blackListArray[index].defaultValue) {
            if (!$scope.blackListArray[index].defaultValue.includes($scope.blackList[index].newValue))
                $scope.blackListArray[index].defaultValue.unshift($scope.blackList[index].newValue);
        } else {
            $scope.blackListArray[index].defaultValue = []
            $scope.blackListArray[index].defaultValue.unshift($scope.blackList[index].newValue);
        }
        $scope.blackList[index].newValue = '';
    }

    $scope.removeValue = (index, key) => {
        $scope.blackListArray[index].defaultValue = $scope.blackListArray[index].defaultValue.filter((value) => value !== key)
    }

    $scope.$watch('blackListArray', function () {
        if (JSON.stringify(angular.copy($scope.initialValue)) !== JSON.stringify(angular.copy($scope.blackListArray))) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }

    }, true);

    $scope.$watch('isEnabled', function () {
        if ($scope.isEnabled.defaultValue !== $scope.initialIsEnabled?.defaultValue) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);
    $scope.telInit = () =>
    $(document).ready(function(){
        var input = document.querySelector("#mobileNum");
        $scope.iti = window.intlTelInput(input, {
            initialCountry: "in",
            separateDialCode: false
        });
    });

    $scope.checkValueLen = (list) => {
        let maxIndex = 0;
        let len = 1;
        let showMore = false;
        if (!list) {
            return {
                maxIndex,
                showMore
            }
        }
        list.map((value, index) => {
            if (len > 20) {
                showMore = true;
                return
            }
            maxIndex = index;
            len += value.length
        })
        return {
            maxIndex,
            showMore,
        }
    }

    $scope.save = () => {
        $scope.blackList = $scope.blackList.map( (list, i) => {
            if (list.type === 'boolean') {
                return $scope.isEnabled;
            } else {
                const temp = list;
                if ($scope.blackListArray[i].defaultValue && $scope.blackListArray[i].defaultValue.length !== 0 && $scope.blackListArray[i].defaultValue.length !== "") {
                    temp.defaultValue = $scope.blackListArray[i].defaultValue.join("##");
                } else {
                    temp.defaultValue = null;
                }
                delete temp.newValue;
                return temp;
            }
        })

        RealmBlackList.update({
                realm: realm.realm,
            },
            $scope.blackList,
            function () {
                $scope.reset()
            }, function () {
                Notifications.success($translate.instant("blacklist-success-save"));
                $scope.reset();
            }
        )
    }

    $scope.reset = function () {
        $scope.canRedirected = false;
        $route.reload();
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if ($scope.changed && $scope.canRedirected) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
            $scope.canRedirected = false;
        }
    })

});

module.controller('RealmKeysProvidersCtrl', function ($scope, Realm, realm, $http, $route, $location, Dialog, Notifications, serverInfo, Components, $modal, $translate) {
    $scope.realm = angular.copy(realm);
    $scope.enableUpload = false;

    $scope.providers = serverInfo.componentTypes['org.keycloak.keys.KeyProvider'];

    Components.query({
        realm: realm.realm,
        parent: realm.id,
        type: 'org.keycloak.keys.KeyProvider'
    }, function (data) {
        $scope.instances = data;

        for (var i = 0; i < $scope.instances.length; i++) {
            for (var j = 0; j < $scope.providers.length; j++) {
                if ($scope.providers[j].id === $scope.instances[i].providerId) {
                    $scope.instances[i].provider = $scope.providers[j];
                }
            }
        }
    });

    $scope.addProvider = function (provider) {
        $location.url("/create/keys/" + realm.realm + "/providers/" + provider.id);
    };

    $scope.removeInstance = function (instance) {
        Dialog.confirmDelete(instance.name, $translate.instant('key-provider'), function () {
            Components.remove({
                realm: realm.realm,
                componentId: instance.id
            }, function () {
                $route.reload();
                Notifications.success($translate.instant("user.storage.remove.success"));
            });
        });
    };
});

module.controller('GenericKeystoreCtrl', function ($scope, $location, Notifications, $route, Dialog, realm, serverInfo, instance, providerId, Components, ConfirmExitDialog, $translate) {
    $scope.create = !instance.providerId;
    $scope.stage = 1;
    $scope.realm = realm;
    $scope.changedFirstSave = false;
    $scope.oldValue = {};
    $scope.keyName = instance.name;

    var providers = serverInfo.componentTypes['org.keycloak.keys.KeyProvider'];
    var providerFactory = null;
    var fileAcceptType = ".cer, .der,.key, .pem, .spc, .p7a, .p7b, p7c, .p8, .pfx, .p12";
    for (var i = 0; i < providers.length; i++) {
        var p = providers[i];
        if (p.id == providerId) {
            $scope.providerFactory = p;
            providerFactory = p;
            break;
        }
    }
    $scope.openRedirect = false;

    if ($scope.create) {
        $scope.instance = {
            // name: providerFactory.id,
            providerId: providerFactory.id,
            providerType: 'org.keycloak.keys.KeyProvider',
            parentId: realm.id,
            config: {
                'priority': ["0"]
            }
        };
    } else {
        $scope.instance = angular.copy(instance);
    }

    if (providerFactory.properties) {
        for (var i of providerFactory.properties) {
            if (i.name === "privateKey" && i.type === "File") {
                var accept = { accept: fileAcceptType };
                Object.assign(i, accept);
            }
            if (i.name === "certificate" && i.type === "File") {
                var accept = { accept: fileAcceptType };
                Object.assign(i, accept);
            }

            if (!$scope.instance.config[i.name]) {
                if (i.name && i.name === "enabled") {
                    i.label = "Enable";
                }
                if (i.defaultValue) {
                    $scope.instance.config[i.name] = [i.defaultValue];
                    if (!$scope.create) {
                        instance.config[i.name] = [i.defaultValue];
                    }
                } else {
                    $scope.instance.config[i.name] = [''];
                    if (!$scope.create) {
                        instance.config[i.name] = [i.defaultValue];
                    }
                }
            }
        }
    }
    $scope.oldValue = angular.copy($scope.instance);
    $scope.$watch('instance', function () {
        if (!angular.equals($scope.instance, $scope.oldValue)) {
            $scope.changedFirstSave = true;
        }
    }, true);

    $scope.save = function () {
        $scope.stage = 2;
        $('#cancelBtn').attr('disabled', 'disabled');
        if ($scope.create) {
            Components.save({ realm: realm.realm }, $scope.instance, function (data, headers) {
                $scope.stage = 1;
                $('#cancelBtn').removeAttr('disabled', 'disabled');
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/keys/providers/" + $scope.instance.providerId + "/" + id);
                Notifications.success($translate.instant("user.storage.create.success"));
            }, function () {
                $scope.stage = 1;
                $('#cancelBtn').removeAttr('disabled', 'disabled');
            });
        } else {
            Components.update({
                realm: realm.realm,
                componentId: instance.id
            },
                $scope.instance, function () {
                    $scope.stage = 1;
                    $('#cancelBtn').removeAttr('disabled', 'disabled');
                    $scope.openRedirect = true;
                    $route.reload();
                    Notifications.success($translate.instant("user.storage.edit.success"));
                }, function () {
                    $scope.stage = 1;
                    $route.reload();
                    $('#cancelBtn').removeAttr('disabled', 'disabled');
                });
        }
        $scope.changedFirstSave = false;
    };

    $scope.reset = function () {
        $scope.openRedirect = true;
        $route.reload();
    };

    $scope.cancel = function () {
        if ($scope.create) {
            $location.url("/realms/" + realm.realm + "/defense/keys");
        } else {
            $scope.openRedirect = true;
            $route.reload();
        }
    };

    $scope.removeInstance = function (instance) {
        Dialog.confirmDelete(instance.name, $translate.instant('key-provider'), function () {
            Components.remove({
                realm: realm.realm,
                componentId: instance.id
            }, function () {
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/defense/keys");
                Notifications.success($translate.instant("user.storage.remove.success"));
            });
        });
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changedFirstSave) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('RealmSessionStatsCtrl', function ($scope, realm, stats, RealmClientSessionStats, RealmLogoutAll, Notifications) {
    $scope.realm = realm;
    $scope.stats = stats;

    $scope.logoutAll = function () {
        RealmLogoutAll.save({ realm: realm.realm }, function (globalReqResult) {
            var successCount = globalReqResult.successRequests ? globalReqResult.successRequests.length : 0;
            var failedCount = globalReqResult.failedRequests ? globalReqResult.failedRequests.length : 0;

            if (failedCount > 0) {
                var msgStart = successCount > 0 ? 'Successfully logout all users under: ' + globalReqResult.successRequests + ' . ' : '';
                Notifications.error(msgStart + 'Failed to logout users under: ' + globalReqResult.failedRequests + '. Verify availability of failed hosts and try again');
            } else {
                window.location.reload();
            }
        });
    };
});


module.controller('RealmRevocationCtrl', function ($scope, Realm, RealmPushRevocation, realm, $http, $location, Dialog, Notifications, $translate) {
    $scope.realm = angular.copy(realm);

    var setNotBefore = function () {
        if ($scope.realm.notBefore == 0) {
            $scope.notBefore = "None";
        } else {
            $scope.notBefore = new Date($scope.realm.notBefore * 1000);
        }
    };

    setNotBefore();

    var reset = function () {
        Realm.get({ id: realm.realm }, function (updated) {
            $scope.realm = updated;
            setNotBefore();
        });

    };

    $scope.clear = function () {
        Realm.update({ realm: realm.realm, notBefore: 0 }, function () {
            $scope.notBefore = "None";
            Notifications.success($translate.instant('not-before-cleared-for-realm'));
            reset();
        });
    };
    $scope.setNotBeforeNow = function () {
        Realm.update({ realm: realm.realm, notBefore: new Date().getTime() / 1000 }, function () {
            Notifications.success($translate.instant('not-before-set-for-realm'));
            reset();
        });
    };
    $scope.pushRevocation = function () {
        RealmPushRevocation.save({ realm: realm.realm }, function (globalReqResult) {
            var successCount = globalReqResult.successRequests ? globalReqResult.successRequests.length : 0;
            var failedCount = globalReqResult.failedRequests ? globalReqResult.failedRequests.length : 0;

            if (successCount == 0 && failedCount == 0) {
                Notifications.warn($translate.instant('push-not-sent'));
            } else if (failedCount > 0) {
                var msgStart = successCount > 0 ? 'Successfully push notBefore to: ' + globalReqResult.successRequests + ' . ' : '';
                Notifications.error(msgStart + 'Failed to push notBefore to: ' + globalReqResult.failedRequests + '. Verify availability of failed hosts and try again');
            } else {
                Notifications.success($translate.instant('push-notbefore-to-all-clients'));
            }
        });
    };

});


module.controller('RoleTabCtrl', function (Dialog, $scope, Current, Notifications, $location, $translate) {
    $scope.removeRole = function () {
        Dialog.confirmDelete($scope.role.name, $translate.instant('role'), function () {
            RoleById.remove({
                realm: realm.realm,
                role: $scope.role.id
            }, function () {
                $route.reload();
                Notifications.success($translate.instant("role-has-been-deleted"));
            });
        });
    };
});


module.controller('RoleListCtrl', function ($scope, $route, $http, Role, role, Dialog, Notifications, realm, RoleList, RoleById, filterFilter, angularSlideOutPanel, Session, $translate) {
    $scope.realm = realm;
    $scope.roles = [];
    $scope.loaderTypeName = 'roleListLoader';
    $scope.query = {
        realm: realm.realm,
        search: null,
        max: 5,
        first: 0
    };

    $scope.query.max = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
    $scope.query.first = 0;
    $scope.entries = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.searchQueryLoaded = false;
    if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
        $scope.query.search = "";
    }
    initialQueryUpdate();
    $scope.copyElement = angular.copy($scope.query.max);
    $scope.editTxt = "Edit";
    $scope.deleteTxt = "Remove";
    $scope.isWorkflow = (role)=> {
        if (role.attributes['flowName']) {
            return true;
        }
        return false;
    }



    function oldQueryUpdate() {
        $scope.lengthQuery = {};
        if ($scope.query.max !== $scope.copyElement) {
            $scope.query.first = 0;
        }
        let querySelectorValue = {};
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
            else if (i !== "max" && i !== "first") {
                querySelectorValue = {
                    [i]: $scope.query[i]
                };
                Object.assign($scope.lengthQuery, querySelectorValue);
            }
        }

        RoleList.query($scope.lengthQuery, function (res) {
            $scope.entries = res.length;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                $scope.pageCount = $scope.entries;
                $scope.startCount = 1;
            }

            else {
                $scope.pageCount = $scope.query.max;
                $scope.startCount = 1;
            }
        });
    }
    $scope.$watch('query.max', function () {
        $scope.query.first = 0;
    }, true);

    $scope.firstPage = function () {
        searchQueryWithLoaded();
        $scope.pageCount = parseInt($scope.query.max);
        $scope.query.first = 0;
        $scope.startCount = 1;
        $scope.QueryUpdate();
    };

    $scope.previousPage = function () {
        searchQueryWithLoaded();
        $scope.startCount = $scope.startCount - $scope.query.max;
        let pageCount = $scope.pageCount;
        let queryMax = parseInt($scope.query.max);
        if (pageCount === 0) {
            $scope.pageCount = queryMax - 5;
        }
        else if (pageCount === $scope.entries) {
            let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
            $scope.pageCount = pageCount - backFlow;
        }
        else {
            $scope.pageCount -= queryMax;
        }
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.QueryUpdate();
    };

    $scope.nextPage = function () {
        searchQueryWithLoaded();
        $scope.startCount = $scope.pageCount + 1;
        let fullLenthOfPages = parseInt($scope.query.max) + $scope.pageCount;
        $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
        $scope.pageCount = ($scope.entries < fullLenthOfPages) ? $scope.entries : fullLenthOfPages;
        $scope.query.first += parseInt($scope.query.max);
        $scope.QueryUpdate();
    };

    $scope.reset = function () {
        $scope.query.search = "";
        $route.reload();
    };

    $scope.clearSearch = function () {
        $scope.query.search = "";
        $scope.searchQuery();
    };

    $scope.QueryUpdate = function () {
        $scope.searchLoaded = false;
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        $scope.roles = RoleList.query($scope.query, function () {
            $scope.searchLoaded = true;
        });
    };

    $scope.searchQuery = function () {
        if ($scope.lastSearch !== $scope.query.search) {
            $scope.searchLoaded = false;
            if ($scope.query.search) {
                $scope.searchQueryLoaded = true;
            }
            else {
                $scope.searchQueryLoaded = false;
            }
            oldQueryUpdate();
            searchQueryWithLoaded();
            $scope.query.first = 0;
            $scope.startCount = 1;
            $scope.roles = RoleList.query($scope.query, function () {
                $scope.searchLoaded = true;
                $scope.lastSearch = $scope.query.search;
            });
        }
    };
    function initialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        searchQueryWithLoaded();
        $scope.roles = RoleList.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
    $scope.selectQuery = function () {
        Session.setItem("pageFilterCount", $scope.query.max);
        $scope.searchLoaded = false;
        searchQueryWithLoaded();
        oldQueryUpdate();
        $scope.roles = RoleList.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    function searchQueryWithLoaded() {
        $scope.searchLoaded = false;
        if ($scope.query.search && $scope.searchQueryLoaded) {
            $scope.searchQueryLoaded = true;
        }
        else {
            $scope.query.search = "";
            $scope.searchQueryLoaded = false;
        }
    }


    $scope.removeRole = function (role) {
        Dialog.confirmDelete(role.name, 'role', function () {
            RoleById.remove({
                realm: realm.realm,
                role: role.id
            }, function () {
                $route.reload();
                Notifications.success($translate.instant("role-has-been-deleted"));
            });
        });
    };
    $scope.isRoleListEmpty = function () {
        return $scope.roles.length == 0;
    };

    $scope.addNewRolePanel = function () {
        var controller = function ($scope, $location) {
            $scope.stage = 1;
            $scope.access = $scope.$$prevSibling.access || $route.current.locals.$scope.$parent.access;
            $scope.realm = realm;
            $scope.role = angular.copy(role);
            $scope.changed = false;
            $scope.oldRoleCopy = angular.copy($scope.role);
            $scope.isWorkflow = false;
            $scope.isWorkflowAvailable = false;
            $scope.workflows = []
            $scope.flow = {}

            const getMetaData = () => {
                $http.get(authUrl + `/realms/${realm.realm}/workflows/connectedAppMetadata?type=role`).then(function (result) {
                    $scope.workflows = result.data;
                    $scope.isWorkflowAvailable = $scope.workflows.length > 0 ? true : false;
                    $scope.isConfigLoaded = true;
                }).catch(function (error) {
                    $scope.isConfigLoaded = true;
                });
            }

            getMetaData();

            $scope.connectedApp = "";
            $scope.flowName = "";
            $scope.permissions = [];
            $scope.AvailableFlowNames = [];
            $scope.RoleMetaData = {};

            $scope.setPermission = () => {
                $scope.workflows.map( flow => {
                    if (flow.flowName === $scope.flow.flowName) {
                        $scope.permissions = flow.metaData.permissions;
                    }
                })
            }

            $scope.setFlowName = () => {
                $scope.AvailableFlowNames = [];
                $scope.workflows.map( flow => {
                    if (flow.connectedApp === $scope.flow.connectedApp) {
                        $scope.AvailableFlowNames.push(flow.flowName)
                    }
                })
            }

            $scope.$watch('role', function() {
                checkValue();
            }, true);


            $scope.$watch('isWorkflow', function() {
                checkValue();
            })

            $scope.$watch('flow', function () {
                checkValue();
            },true)

            $scope.$watch('permissions', function () {
                checkValue();
            },true)

            const checkValue = () => {
                let canSave = false;
                if ($scope.isWorkflow) {
                    canSave = $scope.flow.connectedApp && $scope.flow.flowName && $scope.permissions.some(p => p.checked) && $scope.role.name;
                }
                if (!$scope.isWorkflow) {
                    canSave = $scope.role.name ? true : false;
                }
                $scope.changed = canSave;
            }

            const createRoleMetaData = () => {
                const RoleMetaData = {}
                RoleMetaData['connectedApp'] = [$scope.flow.connectedApp];
                RoleMetaData['flowName'] = [$scope.flow.flowName];
                RoleMetaData['permissions'] = []
                $scope.permissions.map(permission => {
                    if (permission.checked) {
                        RoleMetaData['permissions'].push({id: permission.id, name: permission.name})
                    }
                })
                RoleMetaData['permissions'] = [JSON.stringify(RoleMetaData['permissions'])];
                return RoleMetaData;
            }

            $scope.save = function () {
                $scope.stage = 2;
                $('#cancelBtn').attr('disabled', 'disabled');
                convertAttributeValuesToLists();
                if ($scope.isWorkflow) {
                    $scope.role['attributes'] =  createRoleMetaData();
                    $scope.role['workflowRole'] = true;
                }
                Role.save({
                    realm: realm.realm
                }, $scope.role, function (data, headers) {
                    $scope.stage = 1;
                    $('#cancelBtn').removeAttr('disabled', 'disabled');
                    $scope.changed = false;
                    convertAttributeValuesToString($scope.role);
                    role = angular.copy($scope.role);
                    Role.get({ realm: realm.realm, role: role.name }, function (role) {
                        var id = role.id;
                        $location.url("/realms/" + realm.realm + "/roles/" + id);
                        $scope.$panelInstance.dismiss('this is from the controller!!');
                        Notifications.success($translate.instant('role-created'));
                    });
                }, function () {
                    $scope.stage = 1;
                    $('#cancelBtn').removeAttr('disabled', 'disabled');
                });
            };

            $scope.cancel = function () {
                $scope.$panelInstance.dismiss('this is from the controller!!');
            };

            function convertAttributeValuesToLists() {
                var attrs = $scope.role.attributes;
                for (var attribute in attrs) {
                    if (typeof attrs[attribute] === "string") {
                        var attrVals = attrs[attribute].split("##");
                        attrs[attribute] = attrVals;
                    }
                }
            }

            function convertAttributeValuesToString(role) {
                var attrs = role.attributes;
                for (var attribute in attrs) {
                    if (typeof attrs[attribute] === "object") {
                        var attrVals = attrs[attribute].join("##");
                        attrs[attribute] = attrVals;
                    }
                }
            }

            $scope.dismissPanel = function () {
                $scope.$panelInstance.dismiss('this is from the controller!!');
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });
        };

        $http.get(resourceUrl + '/partials/panels/add-role-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                    console.log('panel was closed with result : ', result);
                }).catch(function (error) {
                    console.log('panel was rejected with error : ', error);
                });
        });

    };

});

module.controller('RolesGroupsCtrl', function ($scope, user, client, realm, AddRolesToUser, AddGroupsToUser, Session) {
    $scope.variant = variant;
    $scope.realm = realm;
    $scope.user = user;
    $scope.client = client;
    $scope.subTab =  Session.getItem("rolesGroupsSubTab") || 'roles';
    $scope.view = Session.getItem("rolesGroupsView") || 'listView';
    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";

    if (!user.profPic) {
        const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
        $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
    }

    $scope.addRolesToUser = () => AddRolesToUser.open(realm.realm, user.id, "Check");

    $scope.addGroupsToUser = () => AddGroupsToUser.open(realm.realm, user.id, 'Check');

    $scope.changeSubTab = (tab) => {
        $scope.subTab = tab;
        Session.setItem("rolesGroupsSubTab", tab)
    }

    $scope.changeView = (view) => {
        $scope.view = view
        Session.setItem("rolesGroupsView", view)
        $scope.subTab = "roles";
    }
});

module.controller('RolesUsersCtrl', function ($scope, group, clients, realm, Session) {
    $scope.variant = variant;
    $scope.realm = realm;
    $scope.clients = clients;
    $scope.group = group;
    $scope.subTab =  Session.getItem("rolesUsersSubTab") || "roles";
    $scope.view = Session.getItem("rolesUsersView") || 'listView';

    $scope.changeSubTab = (tab) => {
        $scope.subTab = tab;
        Session.setItem("rolesUsersSubTab", tab)
    }

    $scope.changeView = (view) => {
        $scope.view = view
        Session.setItem("rolesUsersView", view)
        $scope.subTab = "roles";
    }
});



module.controller('RoleDetailCtrl', function ($scope, realm, role, roles, Client, $route,
    Role, ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
    $http, $location, Dialog, Notifications, RealmRoleRemover, ComponentUtils, ConfirmExitDialog, $translate) {
    $scope.realm = realm;
    $scope.stage = 1;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;
    $scope.deleteTxt = "Remove";
    $scope.changed = $scope.create;
    $scope.oldRoleCopy = angular.copy($scope.role);
    $scope.roleAttributes = angular.copy($scope.role.attributes);
    $scope.isConfigLoaded = false;
    $scope.isWorkflow = role.attributes['connectedApp'] ? true : false;
    let checkedPermissions = [];
    if ($scope.isWorkflow) {
        $scope.workflows = []
        $scope.permissions = [];
        checkedPermissions = JSON.parse(role.attributes['permissions'])
        $scope.oldPermissions = [];
    }

    if ($location.url().endsWith('role-attributes') && $scope.isWorkflow) {
        delete $scope.roleAttributes['permissions'];
        delete $scope.roleAttributes['connectedApp'];
        delete $scope.roleAttributes['flowName'];
        delete $scope.roleAttributes['isWorkflowRole'];
        $scope.noWorkflowMetaData = 4;
        Object.keys($scope.roleAttributes).map(key =>{
            if (key.endsWith("_organizations") || key.endsWith("_projects")) {
                delete $scope.roleAttributes[key];
                $scope.noWorkflowMetaData++;
            }
        })
    }

    const updatePermission = () => {
        $scope.workflows.map( flow => {
            if (flow.flowName === role.attributes['flowName'][0]) {
                $scope.permissions = flow.metaData.permissions;
            }
        })
        $scope.permissions = $scope.permissions.map(permission => {
            if (checkedPermissions.find(p => p.id === permission.id)) {
                return ({...permission, checked: true});
            } else  { return ({...permission, checked: false});}
        })
        $scope.oldPermissions = angular.copy($scope.permissions);
    }

    const getMetaData = () => {
        $http.get(authUrl + `/realms/${realm.realm}/workflows/connectedAppMetadata?type=role`).then(function (result) {
            $scope.workflows = result.data;
            $scope.isWorkflowAvailable = $scope.workflows.length > 0 ? true : false;
            updatePermission();
            $scope.isConfigLoaded = true;
        }).catch(function (error) {
            $scope.isConfigLoaded = true;
        });
    }
    if ($scope.isWorkflow) {
        getMetaData();
    }

    $scope.$watch('permissions', function () {
        if ($scope.isConfigLoaded && $scope.isWorkflow && !angular.equals($scope.oldPermissions, $scope.permissions)) {
            checkValue();
        } else {
            $scope.changed = false;
        }
    },true)

    const checkValue = () => {
        $scope.changed = $scope.permissions.some(p => p.checked);
    }

    $scope.$watch('role', function () {
        if (!angular.equals($scope.role, $scope.oldRoleCopy)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    const createRoleMetaData = () => {
        let permissions = [];
        $scope.permissions.map(permission => {
            if (permission.checked) {
                permissions.push({id: permission.id, name: permission.name})
            }
        })
        permissions = [JSON.stringify(permissions)];
        return permissions;
    }

    $scope.newAttribute = {
        key: '',
        value: ''
    };

    // convertAttributeValuesToString($scope.role);

    var roleCopy = angular.copy($scope.role);

    $scope.$watch('newAttribute.key', function () {
        if ($scope.newAttribute.key in $scope.role.attributes) {
            $scope.isKeyExist = true;
        } else {
            $scope.isKeyExist = false;
        }
    }, true);



    $scope.save = function () {
        $scope.stage = 2;
        $('#cancelBtn').attr('disabled', 'disabled');
        convertAttributeValuesToLists();
        if ($scope.create) {
            Role.save({
                realm: realm.realm
            }, $scope.role, function (data, headers) {
                $scope.stage = 1;
                $('#cancelBtn').removeAttr('disabled', 'disabled');
                $scope.changed = false;
                convertAttributeValuesToString($scope.role);
                role = angular.copy($scope.role);

                Role.get({ realm: realm.realm, role: role.name }, function (role) {
                    var id = role.id;
                    $location.url("/realms/" + realm.realm + "/roles/" + id);
                    Notifications.success($translate.instant("role-created"));
                    $route.reload();
                });
            }, function () {
                $scope.stage = 1;
                $('#cancelBtn').removeAttr('disabled', 'disabled');
            });
        } else {
            if ($scope.isWorkflow) {
                $scope.role.attributes['permissions'] = createRoleMetaData()
            }
            var requestPayload = $scope.isRoleAttributesLoaded ? $scope.role : roleCopy;
            RoleById.update({
                realm: realm.realm,
                role: role.id
            }, requestPayload, function () {
                $scope.stage = 1;
                $('#cancelBtn').removeAttr('disabled', 'disabled');
                $scope.changed = false;
                $scope.allowAddAttribute = false;
                role = angular.copy(requestPayload);
                convertAttributeValuesToString(requestPayload);
                $scope.isAddRoleAttributeLoading = false;
                $scope.isRoleAttributesLoaded = true;
                Notifications.success($translate.instant("changes-saved-to-role"));
                $route.reload();
            }, function () {
                $scope.stage = 1;
                $('#cancelBtn').removeAttr('disabled', 'disabled');
            });
        }
    };

    $scope.remove = function () {
        RealmRoleRemover.remove($scope.role, realm);
    };

    $scope.cancel = function () {
       $scope.permissions = angular.copy($scope.oldPermissions);
       $scope.changed = false;
    };

    $scope.isRoleAttributesLoaded = true;

    $scope.addAttribute = function () {
        $scope.isRoleAttributesLoaded = false;
        $scope.isAddRoleAttributeLoading = true;
        roleCopy.attributes[$scope.newAttribute.key] = $scope.newAttribute.value;
        delete $scope.newAttribute;
        $scope.save();
    };

    $scope.removeAttribute = function (key) {
        $scope.selectedRoleAttribute = key;
        $scope.isRoleAttributesLoaded = false;
        delete roleCopy.attributes[key];
        $scope.changed = true;
        $scope.save();

    };


    function convertAttributeValuesToLists() {
        var requestPayload = $scope.isRoleAttributesLoaded ? $scope.role : roleCopy;
        var attrs = requestPayload.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "string") {
                var attrVals = attrs[attribute].split("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    function convertAttributeValuesToString(role) {
        var attrs = role.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "object") {
                var attrVals = attrs[attribute].join("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    $scope.$watch('realm', function () {
        if (!angular.equals($scope.role, $scope.oldRoleCopy)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    $scope.attributeLength = function () {
        if ($scope.roleAttributes !== undefined)
            return Object.keys($scope.roleAttributes).length;
        return 0;
    };

    $scope.allowAddAttribute = false;

    $scope.addAttributeButton = function () {
        $scope.allowAddAttribute = true;
    };

    $scope.noAttributesSaved = function () {
        $scope.tempRole = angular.copy(role);
        if ($scope.tempRole.attributes !== undefined) {
            let len = Object.keys($scope.tempRole.attributes).length;
            return $scope.isWorkflow ? len - $scope.noWorkflowMetaData : len;
        }
        return 0;
    };

    $scope.openRedirect = false;
    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });

    roleControl($scope, $route, realm, role, roles, Client,
        ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
        $http, $location, Notifications, Dialog, ComponentUtils);
});

module.controller('RealmMFAConnectionsCtrl', function ($scope, MFAConfig, mfaConfig, ComponentUtils, MFAConfigDescription, RealmMFAConfig, Current, Realm, $route, realm, $http, Dialog, Notifications, InputDialog, angularSlideOutPanel, serverInfo, $location, $translate) {
    if (realm.mode == "bo") {
        $location.path('/notfound');
        return;
    }
    $scope.realm = angular.copy(realm);
    $scope.mfaProviders = serverInfo.mfaProviders;
    $scope.mfaConfig = mfaConfig;
    $scope.configuredMfaProviders = mfaConfig.mfaProviders ? $scope.mfaProviders.filter(providerData => mfaConfig.mfaProviders.includes(providerData.id)) : [];
    $scope.isEnabled = function (id) {
        return mfaConfig.mfaProviders.indexOf(id) >= 0;
    };
    $scope.remove = function (providerId) {
        Dialog.confirmDelete("", providerId, function () {
            MFAConfig.remove({
                realm: realm.realm,
                provider: providerId
            }, function (res) {
                var copy = angular.copy($scope.mfaConfig);
                if (copy.mfaProviders.indexOf(providerId) >= 0) {
                    copy.mfaProviders.splice(copy.mfaProviders.indexOf(providerId), 1);
                }
                RealmMFAConfig.update({
                    id: realm.realm
                }, copy, function () {
                    mfaConfig = copy;
                    $route.reload();
                    Notifications.success($translate.instant('mfaProviderDeleted'));
                });
            });
        });
    };
    $scope.searchKey = "";

    $scope.findConfigured = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const configured of $scope.configuredMfaProviders) {
                if (configured.displayName.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('RealmMfaConnectionsProviderCtrl', function ($scope, MFAConfig, mfaConfig, ComponentUtils, MFAConfigDescription, RealmMFAConfig, MarketplaceConfig, Current, Realm, $route, realm, $http, Dialog, Notifications, InputDialog, angularSlideOutPanel, serverInfo, $location, $translate, ComponentUtils, ConfirmExitDialog, ServerInfo, ProcessInput) {

    if (realm.mode == "bo") {
        $location.path('/notfound');
        return;
    }
    $scope.realm = realm;
    $scope.mfaProviders = serverInfo.mfaProviders;
    $scope.configuredMfaProviders = mfaConfig.mfaProviders;
    $scope.urlPaths = $location.url().split("/");
    $scope.providerId = $scope.urlPaths[$scope.urlPaths.length - 1];
    for (provider of $scope.mfaProviders) {
        if ($scope.providerId === provider.id) {
            $scope.howToDo = provider.howToDo;
            $scope.name = provider.displayName;
            $scope.logo = provider.logo;
            $scope.isDefault = provider.default;
            $scope.uninstallInfo = $translate.instant('uninstall-mfa-conn-desc', { name: provider.displayName });
        }
    }
    $scope.newMFAProvider = $scope.urlPaths[$scope.urlPaths.length - 2] === "create-conn";
    $scope.resourceUrl = resourceUrl;
    $scope.mfaConfig = mfaConfig;
    $scope.hasConfig = true;
    $scope.changed = false;
    $scope.ishowTo = true;
    $scope.isConfig = false;
    $scope.toggleChanged = false;
    let oldConfig = null;
    $scope.stage = 1;
    $scope.tabSelect = function (tab) {
        if (tab === "howTo") {
            $scope.ishowTo = true;
            $scope.isConfig = false;
        }
        else if (tab === "Config") {
            $scope.ishowTo = false;
            $scope.isConfig = true;
        }
    };

    if ($scope.hasConfig) {
        MFAConfig.get({ realm: realm.realm, provider: $scope.providerId }, function (conresponse) {
            MFAConfigDescription.get({ realm: realm.realm, provider: $scope.providerId }, function (response) {
                $scope.configType = response;
                if (conresponse != null && Object.keys(conresponse.config).length > 0) {
                    oldConfig = angular.copy(conresponse);
                    $scope.config = angular.copy(conresponse);
                }
                else {
                    var defaultConfig = {};
                    if ($scope.configType && Array.isArray($scope.configType.properties) && $scope.newMFAProvider) {
                        for (property of $scope.configType.properties) {
                            if (property && property.name) {
                                defaultConfig[property.name] = property.defaultValue;
                            }
                        }
                    }
                    oldConfig = angular.copy({ config: defaultConfig });
                    $scope.config = { config: defaultConfig };
                }
                $scope.disableEnableBtnCtrl();
            });
        });
    }
    $scope.disableEnableBtnCtrl = function () {
        for (const prop of $scope.configType.properties) {
            if ($scope.newMFAProvider && prop.required) {
                return;
            }
        }
        if ($scope.newMFAProvider) {
            $('#mfaSaveBtn').removeAttr("disabled");
            $('#mfaCancelBtn').removeAttr("disabled");
        }
    };

    const removeEmptyStringInMultivaluedList = function (properties, config) {
        if (!properties) {
            return;
        }
        for (prop of properties) {
            if (prop.type === 'MultivaluedList') {
                var configProperty = config[prop.name];
                if (configProperty && configProperty[0] === "") {
                    config[prop.name] = [];
                }
            }
        }
    };

    $scope.$watch('config', function () {
        if ($scope.config && $scope.config.config) {
            ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, oldConfig.config);
            ComponentUtils.convertBackToMultivaluedList($scope.configType.properties, oldConfig.config);
            ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.configType.properties, oldConfig.config);
            removeEmptyStringInMultivaluedList($scope.configType.properties, $scope.config.config);
            if (!angular.equals($scope.config.config, oldConfig.config) && checkConfigNull($scope.config.config, $scope.configType.properties)) {
                $scope.toggleChanged = true;
            } else {
                $scope.toggleChanged = false;
            }
        }
    }, true);
    const sendConfig = function (copy) {
        $scope.stage = 2;
        var configCopy = angular.copy($scope.config);
        ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
        ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
        if (!$scope.newMFAProvider) {
            MFAConfig.update({
                realm: realm.realm,
                provider: $scope.providerId
            }, configCopy, function (res) {
                RealmMFAConfig.update({
                    id: realm.realm
                }, copy, function () {
                    $scope.stage = 1;
                    mfaConfig = copy;
                    $scope.toggleChanged = false;
                    Notifications.success($translate.instant('changesSavedSuccess'));
                    oldConfig = angular.copy($scope.config);
                }, function () {
                    $scope.stage = 1;
                });
            }, function () {
                $scope.stage = 1;
            });
        }
        else {
            MFAConfig.save({
                realm: realm.realm,
                provider: $scope.providerId
            }, configCopy, function (res) {
                RealmMFAConfig.update({
                    id: realm.realm
                }, copy, function () {
                    $scope.stage = 1;
                    mfaConfig = copy;
                    $scope.toggleChanged = false;
                    $location.url("/realms/" + realm.realm + "/connections/mfa");
                    Notifications.success($translate.instant('changesSavedSuccess'));
                    oldConfig = angular.copy($scope.config);
                }, function () {
                    $scope.stage = 1;
                });
            }, function (err) {
                Notifications.error($translate.instant(err.data.errorMessage));
                $scope.stage = 1;
            });
        }

    };


    $scope.save = function () {
        $scope.changed = false;
        var copy = angular.copy($scope.mfaConfig);
        if (copy.mfaProviders.indexOf($scope.providerId) < 0) {
            copy.mfaProviders.push($scope.providerId);
        }
        sendConfig(copy);
    };

    $scope.remove = function () {
        Dialog.confirmDelete("", $translate.instant($scope.providerId), function () {
            MFAConfig.remove({
                realm: realm.realm,
                provider: $scope.providerId
            }, function (res) {
                var copy = angular.copy($scope.mfaConfig);
                if (copy.mfaProviders.indexOf($scope.providerId) >= 0) {
                    copy.mfaProviders.splice(copy.mfaProviders.indexOf($scope.providerId), 1);
                }
                RealmMFAConfig.update({
                    id: realm.realm
                }, copy, function () {
                    mfaConfig = copy;
                    $location.url("/realms/" + Current.realm.realm + "/connections/mfa");
                    Notifications.success($translate.instant('mfaProviderDeleted'));
                });
            }, function (error) {
                if (error.status === 409) {
                    Notifications.error($translate.instant(error.data.error));
                }
            });
        });
    };

    $scope.removeToMarketplace = function () {
        Dialog.confirmUnInstall($scope.realm.realm, $scope.providerId, function () {
            MarketplaceConfig.remove({
                realm: realm.realm,
                provider: $scope.providerId
            }, function (res) {
                ServerInfo.reload(realm.realm);
                $location.url("/realms/" + Current.realm.realm + "/connections/mfa");
                Notifications.success($translate.instant('mfaProviderUninstalled'));
            });
        });
    };

    $scope.isEnabled = function (id) {
        return mfaConfig.mfaProviders.indexOf(id) >= 0;
    };

    $scope.cancel = function () {
        $scope.openRedirect = true;
        if ($scope.newMFAProvider) {
            $location.url("/realms/" + realm.realm + "/connections/mfa");
        } else {
            $route.reload();
        }
    };

    $scope.openRedirect = false;
    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.toggleChanged) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });

    $scope.openSetupGuide = function (name, logo, setupGuideLink) {
        var controller = function ($scope, $location, $route) {
            $scope.realm = realm;
            $scope.name = name;
            $scope.logo = logo;
            $scope.resourceUrl = resourceUrl;
            $scope.howToDo = setupGuideLink; //getValidDomainForMinio(location.origin)+"/"+
            $scope.ishowTo = true;
            $scope.count = 0;

            $scope.dismissPanel = function () {
                $scope.$panelInstance.dismiss();
            };

            $scope.mdTrigger = function () {
                return null;
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });
        };

        $http.get(resourceUrl + '/partials/panels/setup-guide-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$route',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                }).catch(function (error) {
                    console.log('panel was rejected with error : ', error);
                });
        });
    };
});

module.controller('CreateMfaConnectionsCtrl', function ($scope, $location, MFAConfig, mfaConfig, ComponentUtils, MFAConfigDescription, RealmMFAConfig, Current, Realm, $route, realm, $http, Dialog, Notifications, InputDialog, angularSlideOutPanel, ServerInfo, serverInfo) {

    if (realm.mode == "bo") {
        $location.url("/notfound");
    }
    $scope.realm = angular.copy(realm);
    $scope.mfaProviders = serverInfo.mfaProviders;
    $scope.configuredMfaProviders = angular.copy(mfaConfig.mfaProviders);
    $scope.nonConfiguredMfaProviders = $scope.mfaProviders ? $scope.mfaProviders.filter(providerData => !mfaConfig.mfaProviders || !mfaConfig.mfaProviders.includes(providerData.id)) : [];
    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.findConnections = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const conn of $scope.nonConfiguredMfaProviders) {
                if (conn.displayName.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('RealmSmsProvidersCtrl', function ($scope, SmsConfig, ClientTryUrl, smsConfig, RealmSmsConfig, $route, Realm, realm, Dialog, Notifications, serverInfo, $translate, LicenseChecker, Auth, $http, $window) {
    $scope.loaderTypeName = 'cardListLoader';
    $scope.searchLoaded = false;
    $scope.realm = realm;
    let setOwnType = () => {
        $scope.smsProviderType = "own";
        $scope.whatsappProviderType = "own";
        $scope.searchLoaded = true;
    };
    $scope.offering = serverInfo.systemInfo.offering;
    $scope.userCanUpdate = false;

    if ($scope.offering == 'CLOUD') {
        $http.get(authUrl + '/admin/realms/' + realm.realm + "/users/" + Auth.user.userId).then(function (res) {
            let data = res.data;
            $scope.userCanUpdate = data?.attributes?.qv_is_account_creator[0] === "true";
        });
    }

    $scope.updateSubscription = function () {
        ClientTryUrl.get({ realm: $scope.realm.realm, client: "profile", redirectUrl: authUrl + "/realms/" + realm.realm + "/account/subscription/" }, function (data) {
            redirectInsideAsync(data.oidcurl);
        }, function () {
            Notifications.error($translate.instant('application-tryurl-fetch-failed'));
        });
    };
    if ($scope.offering.toLowerCase() === "cloud") {
        LicenseChecker.get({ realm: realm.realm }, function (data) {
            if (data?.status === 200) {
                if(data?.body.message === "lifetime-license-validity")
                {
                    setOwnType();
                    return;
                }
                if (data?.body) {
                    $scope.smsProviderType = $scope.getProviderCategory(data?.body?.credits, "sms");
                    $scope.whatsappProviderType = $scope.getProviderCategory(data?.body?.credits, "whatsapp");
                    $scope.searchLoaded = true;
                }
                else {
                    setOwnType();
                }
            }
            else {
                $scope.unlicensedProduct = true;
                $scope.licenseMessage = data?.body?.message;
                $scope.searchLoaded = true;
            }
        });
    } else {
        setOwnType();
    }
    $scope.getProviderCategory = function (data, type) {
        for (const provider of data) {
            if (provider.externalId === type) {
                return "default";
            }
        }
        return "own";
    };
    $scope.smsConfig = smsConfig;
    $scope.smsProviders = [...serverInfo.smsProviders.normal, ...serverInfo.smsProviders.premium];
    $scope.configuredSmsProviders = smsConfig.smsSenders && $scope.smsProviders ? $scope.smsProviders.filter(providerData => smsConfig.smsSenders.includes(providerData.id)) : [];

    $scope.isEnabled = function (id) {
        return smsConfig.smsSenders.indexOf(id) >= 0;
    };
    $scope.remove = function (providerId) {
        let realmCopy = angular.copy($scope.realm);
        Dialog.confirmDelete("", providerId, function () {
            if ($scope.configuredSmsProviders.length == 1) {
                //On deleting last SMS Provider
                realmCopy.enabledOwnSmsProvider = false;
                Realm.update({ id: realm.realm },
                    realmCopy, function () {
                    });
            }
            SmsConfig.remove({
                realm: realm.realm,
                provider: providerId
            }, function (res) {
                var copy = angular.copy($scope.smsConfig);
                if (copy.smsSenders.indexOf(providerId) >= 0) {
                    copy.smsSenders.splice(copy.smsSenders.indexOf(providerId), 1);
                }
                RealmSmsConfig.update({
                    id: realm.realm
                }, copy, function () {
                    smsConfig = copy;
                    $route.reload();
                    Notifications.success($translate.instant('smsProviderDeleted'));
                });
            });
        });
    };

    $scope.updateSmsProvider = function () {
        let realmCopy = angular.copy($scope.realm);
        realmCopy.enabledOwnSmsProvider = !$scope.realm.enabledOwnSmsProvider;
        Realm.update({ id: realm.realm },
            realmCopy, function () {
                Notifications.success($translate.instant("changes-saved-successfully"));
            }, function (err) {
                Notifications.error($translate.instant("something-went-wrong-notify-error"));
            });
    };

    $scope.searchKey = "";

    $scope.findConfigured = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const configured of $scope.configuredSmsProviders) {
                if (configured.displayName.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('SmsProviderCtrl', function ($scope, SmsConfig, smsConfig, ComponentUtils, SmsConfigDescription, Realm, RealmSmsConfig, Current, $route, realm, $http, $location, Dialog, Notifications, InputDialog, angularSlideOutPanel, serverInfo, $translate, ConfirmExitDialog, FieldValidationUtils) {
    $scope.realm = realm;
    $scope.smsProviders = [...serverInfo.smsProviders.normal, ...serverInfo.smsProviders.premium];
    $scope.configuredSmsProviders = smsConfig.smsSenders && $scope.smsProviders ? $scope.smsProviders.filter(providerData => smsConfig.smsSenders.includes(providerData.id)) : [];
    if (realm.smtpServer.host != undefined)
        $scope.smtpEnabled.value = true;

    $("#smtp_list").load("li", function (ev) {
        var liCount = $("#smtp_list li").length;
    });

    $scope.isEnabled = function (id) {
        return smsConfig.smsSenders.indexOf(id) >= 0;
    };

    $scope.urlPaths = $location.url().split("/");
    $scope.providerId = $scope.urlPaths[$scope.urlPaths.length - 1];
    for (provider of $scope.smsProviders) {
        if ($scope.providerId === provider.id) {
            $scope.howToDo = provider.howToDo;
            $scope.name = provider.displayName;
            $scope.logo = provider.logo;
        }
    }
    $scope.newsmsProvider = $scope.urlPaths[$scope.urlPaths.length - 2] === "create";
    $scope.resourceUrl = resourceUrl;
    $scope.smsConfig = smsConfig;
    $scope.hasConfig = true;
    $scope.changed = false;
    $scope.ishowTo = true;
    $scope.isConfig = false;
    $scope.toggleChanged = false;
    $scope.openRedirect = false;
    let oldConfig = null;
    $scope.stage = 1;
    $scope.tabSelect = function (tab) {
        if (tab === "howTo") {
            $scope.ishowTo = true;
            $scope.isConfig = false;
        }
        else if (tab === "Config") {
            $scope.ishowTo = false;
            $scope.isConfig = true;
        }
    };
    if ($scope.hasConfig) {
        SmsConfig.get({ realm: realm.realm, provider: $scope.providerId }, function (conresponse) {
            SmsConfigDescription.get({ realm: realm.realm, provider: $scope.providerId }, function (response) {
                $scope.configType = response;
                if (conresponse != null && Object.keys(conresponse.config).length > 0) {
                    oldConfig = angular.copy(conresponse);
                    $scope.config = angular.copy(conresponse);
                }
                else {
                    var defaultConfig = {};
                    if ($scope.configType && Array.isArray($scope.configType.properties)) {
                        for (property of $scope.configType.properties) {
                            if (property && property.name) {
                                defaultConfig[property.name] = property.defaultValue;
                            }
                        }
                    }
                    oldConfig = angular.copy({ config: defaultConfig });
                    $scope.config = { config: defaultConfig };
                }
            });
        });
    }

    const removeEmptyStringInMultivaluedList = function (properties, config) {
        if (!properties) {
            return;
        }
        for (prop of properties) {
            if (prop.type === 'MultivaluedList') {
                var configProperty = config[prop.name];
                if (configProperty && configProperty[0] === "") {
                    config[prop.name] = [];
                }
            }
        }
    };

    $scope.$watch('config', function () {
        if ($scope.config && $scope.config.config) {
            ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, oldConfig.config);
            ComponentUtils.convertBackToMultivaluedList($scope.configType.properties, oldConfig.config);
            ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.configType.properties, oldConfig.config);
            removeEmptyStringInMultivaluedList($scope.configType.properties, $scope.config.config);
            if (!angular.equals($scope.config.config, oldConfig.config) && checkConfigWithMultipleDependsOn($scope.config.config, $scope.configType.properties)) {
                $scope.toggleChanged = true;
            } else {
                $scope.toggleChanged = false;
            }
        }
    }, true);

    $scope.test = function () {
        if (FieldValidationUtils.validateType($scope)) {
            InputDialog.asksForPhoneNumbertoTest($scope.config, $scope.configType, $scope.providerId, $translate.instant('enter-sms'), $translate.instant('sms'), function (mobNum) { }, $scope.realm);
        }
    };

    const sendConfig = function (copy) {
        $scope.stage = 2;
        var configCopy = angular.copy($scope.config);
        ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
        ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
        if (configCopy.id) {
            SmsConfig.update({
                realm: realm.realm,
                provider: $scope.providerId
            }, configCopy, function (res) {
                RealmSmsConfig.update({
                    id: realm.realm
                }, copy, function () {
                    $scope.stage = 1;
                    smsConfig = copy;
                    $scope.toggleChanged = false;
                    oldConfig.config = angular.copy(configCopy.config);
                    Notifications.success($translate.instant('changesSavedSuccess'));
                }, function () {
                    $scope.stage = 1;
                });
            }, function () {
                $scope.stage = 1;
            });
        }
        else {
            SmsConfig.save({
                realm: realm.realm,
                provider: $scope.providerId
            }, configCopy, function (res) {
                RealmSmsConfig.update({
                    id: realm.realm
                }, copy, function () {
                    $scope.stage = 1;
                    smsConfig = copy;
                    $scope.toggleChanged = false;
                    $location.url("/realms/" + realm.realm + "/sms/providers/settings/" + $scope.providerId);
                    Notifications.success($translate.instant('changesSavedSuccess'));
                }, function () {
                    $scope.stage = 1;
                });
            }, function () {
                $scope.stage = 1;
            });
        }
    };

    var realmCopy = angular.copy($scope.realm);

    $scope.save = function () {
        if (FieldValidationUtils.validateType($scope)) {
            $scope.changed = false;
            if ($scope.configuredSmsProviders.length == 0) {
                //On Creating first sms Provider
                realmCopy.enabledOwnSmsProvider = true;
                Realm.update({ id: realm.realm },
                    realmCopy, function () {
                    });
            }
            var copy = angular.copy($scope.smsConfig);
            if (copy.smsSenders.indexOf($scope.providerId) < 0) {
                copy.smsSenders.push($scope.providerId);
            }
            sendConfig(copy);
        }
    };

    $scope.remove = function () {
        Dialog.confirmDelete("", $scope.providerId, function () {
            if ($scope.configuredSmsProviders.length == 1) {
                //On deleting last SMS Provider
                realmCopy.enabledOwnSmsProvider = false;
                Realm.update({ id: realm.realm },
                    realmCopy, function () {
                    });
            }
            SmsConfig.remove({
                realm: realm.realm,
                provider: $scope.providerId
            }, function (res) {
                var copy = angular.copy($scope.smsConfig);
                if (copy.smsSenders.indexOf($scope.providerId) >= 0) {
                    copy.smsSenders.splice(copy.smsSenders.indexOf($scope.providerId), 1);
                }
                RealmSmsConfig.update({
                    id: realm.realm
                }, copy, function () {
                    smsConfig = copy;
                    $scope.openRedirect = true;
                    $location.url("/realms/" + Current.realm.realm + "/sms/providers");
                    Notifications.success($translate.instant('smsProviderDeleted'));
                });
            });
        });
    };

    $scope.cancel = function () {
        if ($scope.newSmsProvider) {
            $location.url("/realms/" + realm.realm + "/sms/providers");
        } else {
            $scope.openRedirect = true;
            $route.reload();
        }
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.toggleChanged) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });

    $scope.openSetupGuide = function (name, logo, setupGuideLink) {
        var controller = function ($scope, $location, $route) {
            $scope.realm = realm;
            $scope.name = name;
            $scope.logo = logo;
            $scope.resourceUrl = resourceUrl;
            $scope.howToDo = setupGuideLink; //getValidDomainForMinio(location.origin)+"/"+
            $scope.ishowTo = true;
            $scope.count = 0;

            $scope.dismissPanel = function () {
                $scope.$panelInstance.dismiss();
            };

            $scope.mdTrigger = function () {
                return null;
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });
        };

        $http.get(resourceUrl + '/partials/panels/setup-guide-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$route',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                }).catch(function (error) {
                    console.log('panel was rejected with error : ', error);
                });
        });
    };
});

module.controller('CreateSmsProvidersCtrl', function ($scope, smsConfig, ClientTryUrl, realm, serverInfo, Session, LicenseChecker, Auth, $window, $http) {
    $scope.realm = angular.copy(realm);
    $scope.initialCurrentTab = Session.getItem("smsProviderType") || "sms";
    //including both the normal and premium sms providers
    $scope.smsProviders = [...serverInfo.smsProviders.normal, ...serverInfo.smsProviders.premium];
    $scope.smsProvider = [];
    $scope.whatsappProvider = [];
    let setOwnType = () => {
        $scope.smsProviderType = "own";
        $scope.whatsappProviderType = "own";
        $scope.resultLoaded = true;
    };
    $scope.offering = serverInfo.systemInfo.offering;
    $scope.resultLoaded = false;
    $scope.userCanUpdate = false;

    if ($scope.offering == 'CLOUD') {
        $http.get(authUrl + '/admin/realms/' + realm.realm + "/users/" + Auth.user.userId).then(function (res) {
            let data = res.data;
            $scope.userCanUpdate = data?.attributes?.qv_is_account_creator[0] === "true";
        });
    }

    $scope.updateSubscription = function () {
        ClientTryUrl.get({ realm: $scope.realm.realm, client: "profile", redirectUrl: authUrl + "/realms/" + realm.realm + "/account/subscription/" }, function (data) {
            redirectInsideAsync(data.oidcurl);
        }, function () {
            Notifications.error($translate.instant('application-tryurl-fetch-failed'));
        });
    };

    //get details of the current license
    if ($scope.offering.toLowerCase() === 'cloud') {
        LicenseChecker.get({ realm: realm.realm }, function (data) {
            if (data?.status === 200 && data?.body) {
                $scope.smsProviderType = $scope.getProviderCategory(data?.body?.credits, "sms");
                $scope.whatsappProviderType = $scope.getProviderCategory(data?.body?.credits, "whatsapp");
                $scope.resultLoaded = true;
            } else {
                setOwnType();
            }
             $scope.searchLoaded = true;
        });
    } else {
        setOwnType();
    }

    //function to return the prodId if the type matches.
    $scope.getProviderCategory = function (data, type) {
        if (data) {
             for (const provider of data) {
                if (provider.externalId === type) {
                    return "default";
                }
            }
        }
        return "own";
    };
    //filtering the non configured providers using smsProviders and smsConfig
    $scope.nonConfiguredSmsProviders = $scope.smsProviders ? $scope.smsProviders.filter(providerData => !smsConfig.smsSenders || !smsConfig.smsSenders.includes(providerData.id)) : [];
    //to check if all the providers are configured.
    $scope.isEveryProvidersConfigured = function (type) {
        if (type === "sms") {
            if ($scope.smsProvider.length || $scope.isProviderDefault(type)) {
                return false;
            } else {
                return true;
            }
        } else {
            if ($scope.whatsappProvider.length || $scope.isProviderDefault(type)) {
                return false;
            } else {
                return true;
            }
        }
    };
    //to check if the sms or whatsapp are default.
    $scope.isProviderDefault = function (type) {
        if (type === "sms" && $scope.smsProviderType === "default") {
            return true;
        }
        if (type === "whatsapp" && $scope.whatsappProviderType === "default") {
            return true;
        }
        return false;
    };
    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };
    $scope.nonConfiguredSmsProviders.filter(function (provider, index) {
        if (provider.id !== "whatsapp") {
            $scope.smsProvider.push(provider);
        } else {
            $scope.whatsappProvider.push(provider);
        }
    });

    $scope.updateProviderData = function (tabName) {
        if (tabName === 'sms') {
            $scope.renderProvider = $scope.smsProvider;
        } else {
            $scope.renderProvider = $scope.whatsappProvider;
        }
    };
    $scope.updateProviderData($scope.initialCurrentTab);
    $scope.initialTab = function (tabName) {
        $scope.searchKey = $scope.search = "";
        $scope.initialCurrentTab = tabName;
        Session.setItem("smsProviderType", tabName);
        $scope.updateProviderData(tabName);
    };

    $scope.findConnections = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const conn of $scope.renderProvider) {
                if (conn.displayName.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('RealmEmailProvidersCtrl', function ($scope, ClientTryUrl, EmailConfig, emailConfig, Realm, $route, realm, $http, Dialog, Notifications, smtpEnabled, serverInfo, $translate, LicenseChecker, Auth) {
    $scope.loaderTypeName = 'cardListLoader';
    $scope.searchLoaded = false;
    $scope.realm = realm;
    let setOwnType = () => {
        $scope.emailProviderType = "own";
    };
    $scope.offering = serverInfo.systemInfo.offering;
    $scope.userCanUpdate = false;

    if ($scope.offering.toLowerCase() === "cloud") {
        $http.get(authUrl + '/admin/realms/' + realm.realm + "/users/" + Auth.user.userId).then(function (res) {
            let data = res.data;
            $scope.userCanUpdate = data?.attributes?.qv_is_account_creator[0] === "true";
        });
    }

    $scope.updateSubscription = function () {
        ClientTryUrl.get({ realm: $scope.realm.realm, client: "profile", redirectUrl: authUrl + "/realms/" + realm.realm + "/account/subscription/" }, function (data) {
            redirectInsideAsync(data.oidcurl);
        }, function () {
            Notifications.error($translate.instant('application-tryurl-fetch-failed'));
        });
    };
    if ($scope.offering.toLowerCase() === "cloud") {
        LicenseChecker.get({ realm: realm.realm }, function (data) {
            if (data?.status === 200) {
                if (data?.body?.credits) {
                    $scope.licenseInfo = data;
                    $scope.getEmailProviderCategory(data?.body?.credits);
                }
                else {
                    setOwnType();
                }
                $scope.searchLoaded = true;
            }
            else {
                $scope.unlicensedProduct = true;
                $scope.licenseMessage = data?.body?.message;
                $scope.searchLoaded = true;
            }
        });
    } else {
        setOwnType();
        $scope.searchLoaded = true;
    }
    $scope.getEmailProviderCategory = function (data) {

        for (const provider of data) {
            if (provider.externalId === 'email') {
                $scope.emailProviderType = "default";
            } else if(provider.externalId === 'own_email_provider') {
                setOwnType();
            }
        }
    };
    $scope.smtpEnabled = smtpEnabled;
    $scope.emailConfig = emailConfig;
    $scope.smtpEnabled.value = false;
    $scope.emailProviders = serverInfo.emailProviders;
    $scope.configuredEmailProviders = emailConfig.emailSenders && $scope.emailProviders ? $scope.emailProviders.filter(providerData => emailConfig.emailSenders.includes(providerData.id)) : [];
    if (realm.smtpServer.host != undefined)
        $scope.smtpEnabled.value = true;

    $("#smtp_list").load("li", function (ev) {
        var liCount = $("#smtp_list li").length;
    });
    $scope.isEnabled = function (id) {
        return emailConfig.emailSenders.indexOf(id) >= 0;
    };
    $scope.remove = function (providerId) {
        Dialog.confirmDelete("", providerId, function () {
            let realmCopy = angular.copy($scope.realm);
            if ($scope.configuredEmailProviders.length == 1) {
                //On deleting last email Provider
                realmCopy.enabledOwnEmailProvider = false;
            }
            EmailConfig.remove({
                realm: realm.realm,
                provider: providerId
            }, function (res) {
                $scope.configuredEmailProviders = $scope.configuredEmailProviders.filter(x => x.id !== providerId);
                $route.reload();
                Notifications.success($translate.instant('emailProviderDeleted'));
            });
        });
    };

    $scope.updateEmailProvider = function () {
        let realmCopy = angular.copy($scope.realm);
        realmCopy.enabledOwnEmailProvider = !$scope.realm.enabledOwnEmailProvider;
        Realm.update({ id: realm.realm },
            realmCopy, function () {
                Notifications.success($translate.instant("changes-saved-successfully"));
            }, function (err) {
                Notifications.error($translate.instant("something-went-wrong-notify-error"));
            });
    };
    $scope.searchKey = "";

    $scope.findConfigured = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const configured of $scope.configuredEmailProviders) {
                if (configured.displayName.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('EmailProviderCtrl', function ($scope, TestEmailConfig, EmailConfig, emailConfig, ComponentUtils, EmailConfigDescription, RealmEmailConfig, Current, Realm, $route, realm, $http, $location, Dialog, Notifications, InputDialog, RealmSMTPConnectionTester, angularSlideOutPanel, smtpEnabled, serverInfo, $translate, ConfirmExitDialog, FieldValidationUtils) {
    $scope.realm = realm;
    $scope.smtpEnabled = smtpEnabled;
    $scope.smtpEnabled.value = false;
    $scope.emailProviders = serverInfo.emailProviders;
    $scope.configuredEmailProviders = emailConfig.emailSenders && $scope.emailProviders ? $scope.emailProviders.filter(providerData => emailConfig.emailSenders.includes(providerData.id)) : [];
    $scope.isTestOk = false;
    $scope.isSecretShown = false;
    if (realm.smtpServer.host != undefined)
        $scope.smtpEnabled.value = true;

    $("#smtp_list").load("li", function (ev) {
        var liCount = $("#smtp_list li").length;
    });

    $scope.isEnabled = function (id) {
        return emailConfig.emailSenders.indexOf(id) >= 0;
    };

    $scope.urlPaths = $location.url().split("/");
    $scope.providerId = $scope.urlPaths[$scope.urlPaths.length - 1];
    for (provider of $scope.emailProviders) {
        if ($scope.providerId === provider.id) {
            $scope.howToDo = provider.howToDo;
            $scope.name = provider.displayName;
            $scope.logo = provider.logo;
        }
    }
    $scope.newEmailProvider = $scope.urlPaths[$scope.urlPaths.length - 2] === "create";
    $scope.resourceUrl = resourceUrl;
    $scope.emailConfig = emailConfig;
    $scope.hasConfig = true;
    $scope.changed = false;
    $scope.ishowTo = true;
    $scope.isConfig = false;
    $scope.toggleChanged = false;
    $scope.openRedirect = false;
    let oldConfig = null;
    $scope.stage = 1;
    $scope.tabSelect = function (tab) {
        if (tab === "howTo") {
            $scope.ishowTo = true;
            $scope.isConfig = false;
        }
        else if (tab === "Config") {
            $scope.ishowTo = false;
            $scope.isConfig = true;
        }
    };
    if ($scope.hasConfig) {
        EmailConfig.get({ realm: realm.realm, provider: $scope.providerId }, function (conresponse) {
            EmailConfigDescription.get({ realm: realm.realm, provider: $scope.providerId }, function (response) {
                $scope.configType = response;
                if (conresponse != null && Object.keys(conresponse.config).length > 0) {
                    oldConfig = angular.copy(conresponse);
                    $scope.config = angular.copy(conresponse);
                }
                else {
                    var defaultConfig = {};
                    if ($scope.configType && Array.isArray($scope.configType.properties)) {
                        for (property of $scope.configType.properties) {
                            if (property && property.name) {
                                defaultConfig[property.name] = property.defaultValue;
                            }
                        }
                    }
                    oldConfig = angular.copy({ config: defaultConfig });
                    $scope.config = { config: defaultConfig };
                }
            });
        });
    }

    const removeEmptyStringInMultivaluedList = function (properties, config) {
        if (!properties) {
            return;
        }
        for (prop of properties) {
            if (prop.type === 'MultivaluedList') {
                var configProperty = config[prop.name];
                if (configProperty && configProperty[0] === "") {
                    config[prop.name] = [];
                }
            }
        }
    };

    const handleTestResult = function (state) {
        $scope.isTestOk = state;
    }

    $scope.$broadcast('showSecret');
    $scope.$on('showSecret', function (event, state) {
        $scope.isSecretShown = state;
    });


    $scope.$watch('config', function () {
        if ($scope.config && $scope.config.config) {
            ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, oldConfig.config);
            ComponentUtils.convertBackToMultivaluedList($scope.configType.properties, oldConfig.config);
            ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.configType.properties, oldConfig.config);
            removeEmptyStringInMultivaluedList($scope.configType.properties, $scope.config.config);
            if (!angular.equals($scope.config.config, oldConfig.config) && checkConfigWithMultipleDependsOn($scope.config.config, $scope.configType.properties) && !$scope.isSecretShown) {
                $scope.toggleChanged = true;
                $scope.isTestOk = false;
            } else {
                $scope.isSecretShown = false;
                $scope.toggleChanged = false;
            }
        }
    }, true);

    $scope.test = function () {
        if (FieldValidationUtils.validateType($scope)) {
            testConfiguration();
        } else {
            Notifications.error($translate.instant("invalid-email-address"));
        }
    };

    function testConfiguration() {
        InputDialog.asksForEmailtoTest($scope.config, $scope.configType, $scope.providerId, $translate.instant('enter-email'), $translate.instant('email'), handleTestResult, $scope.realm);
    }

    const sendConfig = function (copy) {
        $scope.stage = 2;
        var configCopy = angular.copy($scope.config);
        ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
        ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
        if (configCopy.id) {
            EmailConfig.update({
                realm: realm.realm,
                provider: $scope.providerId
            }, configCopy, function (res) {
                $scope.stage = 1;
                emailConfig = copy;
                $scope.toggleChanged = false;
                oldConfig.config = angular.copy(configCopy.config);
                Notifications.success($translate.instant('changesSavedSuccess'));
            }, function () {
                $scope.stage = 1;
            });
        }
        else {
            EmailConfig.save({
                realm: realm.realm,
                provider: $scope.providerId
            }, configCopy, function (res) {
                $scope.stage = 1;
                emailConfig = copy;
                $scope.toggleChanged = false;
                $location.url("/realms/" + realm.realm + "/email/providers/settings/" + $scope.providerId);
                Notifications.success($translate.instant('changesSavedSuccess'));
            }, function () {
                $scope.stage = 1;
            });
        }
    };

    var realmCopy = angular.copy($scope.realm);

    $scope.save = function () {
        if (FieldValidationUtils.validateType($scope)) {
            saveConfiguration();
        } else {
            Notifications.error($translate.instant("invalid-email-address"));
        }
    };

    function saveConfiguration() {
        $scope.changed = false;
        if ($scope.configuredEmailProviders.length == 0) {
            //On Creating first email Provider
            realmCopy.enabledOwnEmailProvider = true;
            Realm.update({ id: realm.realm },
                realmCopy, function () {
                });
        }
        var copy = angular.copy($scope.emailConfig);
        if (copy.emailSenders.indexOf($scope.providerId) < 0) {
            copy.emailSenders.push($scope.providerId);
        }
        sendConfig(copy);
    }

    $scope.remove = function () {

        Dialog.confirmDelete("", $scope.providerId, function () {
            if ($scope.configuredEmailProviders.length == 1) {
                realmCopy.enabledOwnEmailProvider = false;
                //On deleting last email Provider

            }

            EmailConfig.remove({
                realm: realm.realm,
                provider: $scope.providerId
            }, function (res) {
                var copy = angular.copy($scope.emailConfig);
                if (copy.emailSenders.indexOf($scope.providerId) >= 0) {
                    copy.emailSenders.splice(copy.emailSenders.indexOf($scope.providerId), 1);
                }
                emailConfig = copy;
                $scope.openRedirect = true;
                $location.url("/realms/" + Current.realm.realm + "/email/providers");
                Notifications.success($translate.instant('emailProviderDeleted'));
            });
        });
    };

    $scope.cancel = function () {
        if ($scope.newEmailProvider) {
            $location.url("/realms/" + realm.realm + "/email/providers");
        } else {
            $scope.openRedirect = true;
            $route.reload();
        }
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.toggleChanged) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });

    $scope.openSetupGuide = function (name, logo, setupGuideLink) {
        var controller = function ($scope, $location, $route) {
            $scope.realm = realm;
            $scope.name = name;
            $scope.logo = logo;
            $scope.resourceUrl = resourceUrl;
            $scope.howToDo = setupGuideLink; //getValidDomainForMinio(location.origin)+"/"+
            $scope.ishowTo = true;
            $scope.count = 0;

            $scope.dismissPanel = function () {
                $scope.$panelInstance.dismiss();
            };

            $scope.mdTrigger = function () {
                return null;
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });
        };

        $http.get(resourceUrl + '/partials/panels/setup-guide-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$route',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                }).catch(function (error) {
                    console.log('panel was rejected with error : ', error);
                });
        });
    };
});

module.controller('CreateEmailProvidersCtrl', function ($scope, TestEmailConfig, EmailConfig, emailConfig, ComponentUtils, EmailConfigDescription, RealmEmailConfig, Current, Realm, $route, realm, $http, $location, Dialog, Notifications, InputDialog, RealmSMTPConnectionTester, angularSlideOutPanel, smtpEnabled, serverInfo) {
    $scope.realm = angular.copy(realm);
    $scope.emailProviders = serverInfo.emailProviders;
    $scope.nonConfiguredEmailProviders = $scope.emailProviders ? $scope.emailProviders.filter(providerData => !emailConfig.emailSenders || !emailConfig.emailSenders.includes(providerData.id)) : [];
    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.findConnections = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const conn of $scope.nonConfiguredEmailProviders) {
                if (conn.displayName.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('RealmEmailTemplatesCtrl', function ($scope, $route, realm, emailTemplates, SaveEmailTemplate, TestEmailTemplate, ResetEmailTemplate, InputDialog, $http, $location, Dialog, Notifications, Session, $translate) {
if (realm.mode == 'bo') {
        $location.url("/notfound");
    }

    $scope.realm = realm;

    $scope.emailTemplates = emailTemplates;
    $scope.stage = 1;
    let templateChoosen = Session.getItem("emailTemplateChoosen") ? Session.getItem("emailTemplateChoosen") : $scope.emailTemplates[0].templateName;
    for (const emailTemplate of emailTemplates) {
        if (emailTemplate.templateName.toLowerCase().includes(templateChoosen.toLowerCase())) {
            $scope.email = {
                template: emailTemplate
            };
            break;
        }
    }
    var editor;
    $scope.initializeCode = function () {
        editor = CodeMirror(document.querySelector('#code'), {
            lineNumbers: true,
            tabSize: 2,
            mode: "htmlmixed",
            htmlMode: true,
            matchBrackets: true,
            theme: "material",
            autoCloseTags: true,
            value: $scope.email.template.htmlBody,
            enableCodeFolding: true,
        });
    };

    const hasScripts = function (html) {
        const scriptRegex = /<\s?\/?script\s*?>/;
        return scriptRegex.test(html);
    };

    $scope.initializeCode();
    $scope.save = function () {
        $scope.stage = 2;
        const toSaveHtml = editor.getValue();
        var emailTempRep = {
            templateName: $scope.email.template.templateName,
            templateEnabled: true,
            htmlBody: toSaveHtml,
            textBody: toSaveHtml
        };

        if (!hasScripts(toSaveHtml)) {
            SaveEmailTemplate.save({ realm: realm.realm }, emailTempRep, function (res) {
                $route.reload();
                Notifications.success($translate.instant('emailTemplateSaved'));
                $scope.stage = 1;
            }, function (err) {
                Notifications.error(err.data.errorMessage);
                $scope.stage = 1;
            });
        } else {
            Notifications.error($translate.instant('emailTemplateErr'));
            $scope.stage = 1;
        }
    };
    editor.on("change", function () {
        $('#submit').removeAttr('disabled', 'disabled');
        $('#undo').removeAttr('disabled', 'disabled');
    });
    $scope.reset = function () {
        ResetEmailTemplate.save({ realm: realm.realm, templatename: $scope.email.template.templateName }, function (res) {
            $route.reload();
            Notifications.success($translate.instant('emailTemplateReset'));
        }, function (err) {
            Notifications.error(err.data.errorMessage);
        });
    };

    $scope.testTemplate = function () {
        InputDialog.asksForEmailtoTestTemplate($translate.instant('enter-email'), $translate.instant('email'), realm.realm, $scope.email.template.templateName, function (email) {
        }, $scope.realm);
    };

    $scope.changeTemplate = function (emailTemp) {
        Session.setItem("emailTemplateChoosen", emailTemp.templateName);
        editor.doc.setValue(emailTemp.htmlBody);
        $('#submit').attr('disabled', 'disabled');
        $('#undo').attr('disabled', 'disabled');
    };
    $scope.cancel = function () {
        editor.doc.setValue($scope.email.template.htmlBody);
    };

});

module.controller('RealmEventReportsCtrl', function ($scope, RealmEventsFetcher, eventsConfig, realm, Session) {

    $scope.realm = realm;
    $scope.eventsConfig = eventsConfig;
    $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === "true" ? true : false;
    $scope.loaderTypeName = 'tableListLoader';
    $scope.navigationLoaded = false;
    $scope.authUrl = authUrl;
    $scope.query = {
        id: realm.realm,
        max: Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5,
        // first: ,
    };

    $scope.refresh = function () {
        init();
    };


    function init() {

        $scope.navigationLoaded = true;
        RealmEventsFetcher.query($scope.query, function (res) {
            $scope.navigationLoaded = false;
            $scope.reports = res;
        });
    }

    init();
});

module.controller('RealmApiLogsCtrl', function ($scope, realm, Session, RealmApiLogsConfig, eventsConfig, $timeout, $route, $location, RealmAPIEventsGenerator, Notifications, $translate, ApiVerifiers, ApiVerifiersName) {

    $scope.realm = realm;
    $scope.loaderTypeName = 'tableListLoader';
    $scope.apiLogs = [];
    $scope.queryFinished = false;
    $scope.eventsConfig = eventsConfig;
    $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === "true" ? true : false;
    $scope.verifiersName = [];

    $scope.entries = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.page = 0;
    $scope.navigationLoaded = false;
    $scope.isPricingLogs = $route.current.pricing;
    let filterByTimestamp = null;
    let filterByVerifierName = null;

    $scope.loadVerifiersName = function (term, callback) {
        ApiVerifiersName.query(
            { realm: realm.realm, verifierName: term.trim() },
            function (response) {
                const results = response
                    .filter(v => v)
                    .map(v => ({ id: v, text: v }));
                callback(results);
            },
            function (error) {
                callback([]);
            }
        );
    };

    $scope.verifierNameOptions = {
        minimumInputLength: 0,
        delay: 500,
        allowClear: true,
        query: function (query) {
            $scope.loadVerifiersName(query.term, function (results) {
                query.callback({ results: results });
            });
        },
        formatResult: function (object) {
            return object.text.toUpperCase();;
        },
        formatSelection: function (object) {
            return object.text.toUpperCase();;
        }
    };

    $scope.computeTimestampFromLast = function(last) {
        if (!last) return null;
        const days = parseInt(last.replace('d',''));
        const now = Date.now();
        const from = new Date();
        from.setDate(from.getDate() - days);
        return {
            from: from.getTime(),
            to: now
        };
    };

    $timeout(function() {
        flatpickr("#dateRange", {
            mode: "range",
            maxDate: "today",
            dateFormat: "Y-m-d",
            onChange: function(selectedDates) {
                if (selectedDates.length === 2 || selectedDates.length === 1) {
                    const start = new Date(selectedDates[0]);
                    start.setHours(0, 0, 0, 0);
                    const end = new Date(selectedDates[1]);
                    end.setHours(23, 59, 59, 999); // include entire end date
                    $scope.query.dateFrom = start.getTime();
                    $scope.query.dateTo   = end.getTime();
                } else {
                    $scope.query.dateFrom = null;
                    $scope.query.dateTo = null;
                }
                $scope.$apply();
            }
        });
    }, 0);


    $scope.loadAPILogs = function () {
        $scope.loading = true;
        filterByVerifierName = $scope.query.filterByVerifierName?.id || '';

        if ($scope.query.dateFrom && $scope.query.dateTo) {
            filterByTimestamp = {
                from: $scope.query.dateFrom,
                to: $scope.query.dateTo
            };
        } else if ($scope.query.last) {
            filterByTimestamp = $scope.computeTimestampFromLast($scope.query.last);
        }

        RealmApiLogsConfig.get({
            realm: $scope.realm.id,
            filterByTimestamp: filterByTimestamp || null,
            filterByVerifierName: filterByVerifierName || '',
            requestId: $scope.query.requestId || null,
            isPricingLogs: $route.current.pricing || false,
            eventType: $scope.query.type,
            page: $scope.query.page,
            size: $scope.query.size
        }).$promise.then(function (data) {
            $scope.apiLogs = data.content;
            $scope.entries = data.totalElements;
            $scope.totalPages = data.totalPages;
            $scope.currentPage = data.number;

            $scope.startCount = ($scope.currentPage * $scope.query.size) + 1;
            $scope.pageCount = ($scope.currentPage * $scope.query.size) + $scope.apiLogs.length;

            $scope.loading = false;
            $scope.queryFinished = true;
        }).catch(function (err) {
            console.error("Error fetching API logs:", err);
            $scope.loading = false;
        });
    };

     $scope.eventSelectOptions = {
             'multiple': true,
             'simple_tags': true,
             'tags': ['VERIFICATION', 'VERIFICATION_FAILED']
         };


            $scope.refresh = function () {
                    $route.reload();
                };

            $scope.query = {
                realmId: realm.id,
                filterByTimestamp: '',
                filterByVerifierName: '',
                requestId: '',
                last: '7d',
                isPricingLogs: $route.current.pricing,
                page: 0,
                size: 5
            };


            $scope.disablePaste = function (e) {
                e.preventDefault();
                return false;
            };

            $scope.$watch('query.size', function () {
                $scope.query.first = 0;
                $scope.update();
            }, true);


            $scope.update = function () {
                $scope.query.page = 0;

                for (var key in $scope.query) {
                    if ($scope.query[key] === '' || $scope.query[key] === null) {
                        delete $scope.query[key];
                    }
                }

                $scope.loadAPILogs();
            };

            $scope.reset = function () {
                    $scope.query.size = 5;
                    $scope.query.type = '';
                    $scope.query.filterByTimestamp= '';
                    $scope.query.filterByVerifierName= '';
                    $scope.query.dateRange='';
                    $scope.query.requestId='';
                    $scope.query.dateFrom = '';
                    $scope.query.dateTo = '';
                    $scope.query.last = '7d';
                    $scope.update();
                };


        $scope.loadAPILogs();

        // Pagination controls
    $scope.firstPage = function () {
        if ($scope.currentPage > 0) {
            $scope.query.page = 0;
            $scope.loadAPILogs();
        }
    };

    $scope.previousPage = function () {
        if ($scope.currentPage > 0) {
            $scope.query.page--;
            $scope.loadAPILogs();
        }
    };

    $scope.nextPage = function () {
        if ($scope.currentPage < $scope.totalPages - 1) {
            $scope.query.page++;
            $scope.loadAPILogs();
        }
    };

    $scope.lastPage = function () {
    if ($scope.currentPage < $scope.totalPages - 1) {
        $scope.query.page = $scope.totalPages - 1;
        $scope.loadAPILogs();
    }
    };

    $scope.generateReport = function (isPricingLogs) {
        //Remove first key and set max equals totalEntries while report generation to include all logs
        const _query = { ...$scope.query };
        delete _query.first;
        delete _query.last;
        _query.size = $scope.entries;
        delete _query.max;
        delete _query.realmId;
        _query.id = realm.realm;
        _query.isPricingLogs = isPricingLogs;
        _query.filterByTimestamp = $scope.query.filterByTimestamp || filterByTimestamp;
        _query.filterByVerifierName = $scope.query.filterByVerifierName ? $scope.query.filterByVerifierName.id : '';

        RealmAPIEventsGenerator.get(_query, function (res) {
            if (res.status == 204)
                Notifications.success($translate.instant('report-triggered-msg'));
            else
                Notifications.error($translate.instant('report-queued-msg'));
        });
    };

    });


module.controller('RealmEventsConfigCtrl', function ($scope, $route, $rootScope, $http, eventsConfig, Client, AllUsers, RealmEventsConfig, RealmEvents, RealmEventsGenerator, realm, serverInfo, $location, Notifications, TimeUnit, Dialog, $modal, angularSlideOutPanel, eventsLength, logsEnabled, Session, $translate) {

    $scope.realm = realm;
    $scope.loaderTypeName = 'tableListLoader';
    $scope.eventsConfig = eventsConfig;
    $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === "true" ? true : false;
    $scope.logsEnabled = logsEnabled;
    $scope.logsEnabled.eventsEnabled = false;
    if ($scope.eventsConfig.eventsEnabled != undefined && $scope.eventsConfig.eventsEnabled) {
        $scope.logsEnabled.eventsEnabled = true;
    }

    $scope.eventsConfig.expirationUnit = TimeUnit.autoUnit(eventsConfig.eventsExpiration);
    $scope.eventsConfig.eventsExpiration = TimeUnit.toUnit(eventsConfig.eventsExpiration, $scope.eventsConfig.expirationUnit);

    $scope.eventListeners = Object.keys(serverInfo.providers.eventsListener.providers);
    $scope.authDetailTxt = $translate.instant("auth-details-txt");
    $scope.entries = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.navigationLoaded = false;
    $scope.eventsConfigSelectOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': $scope.eventListeners
    };

    $scope.eventSelectOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': serverInfo.enums['eventType']
    };
    $scope.eventsLength = eventsLength;
    $scope.eventsLength.logs = 0;

    $scope.clientSelectOptions = {
        minimumInputLength: 0,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = { results: [] };
            Client.query({ realm: realm.realm, search: true, clientName: query.term.trim(), max: 20 }, function (response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function (object, container, query) {
            object.text = object.name;
            return object.name;
        }
    };

    $scope.userSelectOptions = {
        minimumInputLength: 0,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = { results: [] };
            AllUsers.query({ realm: realm.realm, search: query.term.trim(), max: 20 }, function (response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function (object, ncontainer, query) {
            object.text = object.username;
            return object.username;
        }
    };

    $scope.gotoSettings = function () {
        var controller = function ($scope, $location) {
            $scope.logsEnabled = logsEnabled;
            $scope.eventsLength = eventsLength;
            $scope.eventsLength.logs = eventsLength.logs;
            $scope.eventsConfig = eventsConfig;
            $scope.realm = realm;
            $scope.eventSelectOptions = {
                'multiple': true,
                'simple_tags': true,
                'tags': serverInfo.enums['eventType']
            };

            var oldCopy = angular.copy($scope.eventsConfig);
            $scope.changed = false;

            $scope.$watch('eventsConfig', function () {
                if (!angular.equals($scope.eventsConfig, oldCopy)) {
                    $scope.changed = true;
                    if ($scope.eventsConfig.eventsEnabled && !$scope.eventsConfig.expirationUnit) {
                        $scope.changed = false;
                    }
                }
                if (angular.equals($scope.eventsConfig, oldCopy)) {
                    $scope.changed = false;
                }
            }, true);

            $scope.save = function () {
                $scope.changed = false;

                var copy = angular.copy($scope.eventsConfig);
                delete copy['expirationUnit'];

                copy.eventsExpiration = TimeUnit.toSeconds($scope.eventsConfig.eventsExpiration, $scope.eventsConfig.expirationUnit);

                RealmEventsConfig.update({
                    id: realm.realm
                }, copy, function () {
                    $route.reload();
                    $scope.$panelInstance.dismiss('this is from the controller!!');
                    Notifications.success($translate.instant("changes-saved-to-realm"));
                    if ($scope.eventsConfig.eventsEnabled != undefined && $scope.eventsConfig.eventsEnabled) {
                        $scope.logsEnabled.eventsEnabled = true;
                    }
                    else {
                        $scope.logsEnabled.eventsEnabled = false;
                    }
                });
            };

            $scope.clearEvents = function () {
                Dialog.confirmDelete($scope.realm.realm, 'events', function () {
                    RealmEvents.remove({ id: $scope.realm.realm }, function () {
                        Notifications.success($translate.instant("logs-has-been-cleared"));
                        $route.reload();
                        $scope.$panelInstance.dismiss('this is from the controller!!');
                    });
                });
            };

            $scope.dismissPanel = function () {
                $scope.eventsConfig = angular.copy(oldCopy);
                $scope.$panelInstance.dismiss('this is from the controller!!');
                $route.reload();
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });
        };

        $http.get(resourceUrl + '/partials/panels/logs-settings-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                    console.log('panel was closed with result : ', result);
                }).catch(function (error) {
                    $scope.dismissPanel();
                    console.log('panel was rejected with error : ', error);
                });
        });

    };

    $scope.refresh = function () {
        $route.reload();
    };

    $scope.reset = function () {
        $scope.eventsConfig = angular.copy(oldCopy);
        $scope.changed = false;
    };



    $scope.page = 0;

    let eventTypes = serverInfo.enums['eventType'].toString();
    if (realm.mode == "bo") {
        eventTypes = eventTypes.toString().split(",").filter((data) => (data.startsWith("EZ_") || data.startsWith("NOTIFICATION")) && (!data.startsWith("EZ_ASHIELD")) && (!data.startsWith("EZ_LIVELINESS")) && (!data.startsWith("EZ_JSON_BASED_FORM"))) ;
    }
    else {
        eventTypes = eventTypes.toString().split(",").filter((data) => !data.startsWith("EZ_"));
    }

    $scope.eventSelectOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': eventTypes
    };

    $scope.query = {
        id: realm.realm,
        max: Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5,
        first: 0,
        last: '7d'
    };

    $scope.disablePaste = function (e) {
        e.preventDefault();
        return false;
    };

    $scope.$watch('query.max', function () {
        $scope.query.first = 0;
        $scope.update();
    }, true);

    $scope.update = function () {
        if ($scope.query.client && $scope.query.client.name) {
            $scope.query.client = $scope.query.client.sandboxMode ? $scope.query.client.sandboxClientId : $scope.query.client.clientId;
        }
        if ($scope.query.user && $scope.query.user.id) {
            $scope.query.user = $scope.query.user.id;
        }
        Session.setItem("pageFilterCount", $scope.query.max);
        oldQueryUpdate();
        $scope.queryFinished = false;
        $scope.query.first = 0;
        for (var i in $scope.query) {
            if ($scope.query[i] === '' || $scope.query[i].length === 0) {
                delete $scope.query[i];
            }
        }
    };

    function oldQueryUpdate() {
        $scope.lengthQuery = {};
        let querySelectorValue = {};
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
            }
            else if (i !== "max" && i !== "first") {
                querySelectorValue = {
                    [i]: $scope.query[i]
                };
                Object.assign($scope.lengthQuery, querySelectorValue);
            }
        }
        RealmEvents.get($scope.query, function (res) {
            $scope.entries = res.total;
            $scope.events = res.representation;
            $scope.queryFinished = true;

            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                $scope.pageCount = $scope.entries;
                $scope.startCount = 1;
            }

            else {
                $scope.pageCount = $scope.query.max;
                $scope.startCount = 1;
            }
        });
    }
    $scope.reset = function () {
        $scope.query.first = 0;
        $scope.query.max = 5;
        $scope.query.type = '';
        $scope.query.client = '';
        $scope.query.user = '';
        $scope.query.request='';
        $scope.query.dateFrom = '';
        $scope.query.dateTo = '';
        $scope.query.last = '7d';
        $scope.update();
    };

    $scope.generateReport = function () {
        //Remove first key and set max equals totalEntries while report generation to include all logs
        const _query = { ...$scope.query };
        delete _query.first;
        _query.max = $scope.entries;

        RealmEventsGenerator.get(_query, function (res) {
            if (res.status == 204)
                Notifications.success($translate.instant('report-triggered-msg'));
            else
                Notifications.error($translate.instant('report-queued-msg'));
        });
    };

    $scope.queryUpdate = function () {
        if ($scope.pageCount === $scope.entries) {
            $scope.queryFinished = true;
        }
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
            }
        }
        RealmEvents.get($scope.query, function (res) {
            $scope.events = res.representation;
            $scope.navigationLoaded = false;
            if (typeof $scope.events !== 'undefined' && $scope.events)
                $scope.eventsLength.logs = $scope.events.length;
            else
                $scope.eventsLength.logs = 0;
        });
    };

    $scope.firstPage = function () {
        $scope.navigationLoaded = true;
        $scope.pageCount = parseInt($scope.query.max);
        $scope.query.first = 0;
        $scope.startCount = 1;
        $scope.queryUpdate();
    };

    $scope.previousPage = function () {
        $scope.navigationLoaded = true;
        $scope.startCount = $scope.startCount - $scope.query.max;
        let pageCount = $scope.pageCount;
        let queryMax = parseInt($scope.query.max);
        if (pageCount === 0) {
            $scope.pageCount = queryMax - 5;
        }
        else if (pageCount === $scope.entries) {
            let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
            $scope.pageCount = pageCount - backFlow;
        }
        else {
            $scope.pageCount -= queryMax;
        }
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.queryUpdate();
    };

    $scope.nextPage = function () {
        $scope.navigationLoaded = true;
        $scope.startCount = $scope.pageCount + 1;
        let fullLenthOfPages = parseInt($scope.query.max) + $scope.pageCount;
        $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
        $scope.pageCount = ($scope.entries < fullLenthOfPages) ? $scope.entries : fullLenthOfPages;
        $scope.query.first += parseInt($scope.query.max);
        $scope.queryUpdate();
    };

    $scope.viewDetails = function (event) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/realm-events-details.html',
            controller: 'RealmAdminEventsModalCtrl',
            backdrop: 'static',
            resolve: {
                event: function () {
                    return event;
                }
            }
        });
    };
});

module.controller('RealmEventListenersCtrl', function ($scope, $http, realm, serverInfo, RealmEventsConfig, eventsConfig, EventsConfigDescription, EventsConfig, TestEventsConfig, angularSlideOutPanel, ComponentUtils, Notifications, $translate) {
    $scope.realm = realm;
    $scope.eventListeners = Object.keys(serverInfo.providers.eventsListener.providers);
    $scope.eventsConfig = eventsConfig;
    $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === "true" ? true : false;

    $scope.showDetails = function (id, hasConfig, howToDo, name, logo) {
        var controller = function ($scope, $location, $route) {
            $scope.realm = realm;
            $scope.resourceUrl = resourceUrl;
            $scope.providerId = id;
            $scope.eventsConfig = eventsConfig;
            $scope.hasConfig = hasConfig;
            $scope.name = name;
            $scope.logo = logo;
            $scope.howToDo = howToDo; //getValidDomainForMinio(location.origin)+"/"+
            $scope.changed = false;
            $scope.ishowTo = true;
            $scope.isConfig = false;
            $scope.oldStatus = $scope.eventsConfig.eventsListeners.indexOf(id) >= 0;
            $scope.toggleChanged = false;
            let oldConfig = null;
            $scope.stage = 1;
            $scope.subStage = 1;
            $scope.tabSelect = function (tab) {
                if (tab === "howTo") {
                    $scope.ishowTo = true;
                    $scope.isConfig = false;
                }
                else if (tab === "Config") {
                    $scope.ishowTo = false;
                    $scope.isConfig = true;
                }
            };
            $scope.mdTrigger = function () {
                return null;
            };
            if (hasConfig) {
                EventsConfig.get({ realm: realm.realm, provider: id }, function (conresponse) {
                    EventsConfigDescription.get({ realm: realm.realm, provider: id }, function (response) {
                        $scope.configType = response;
                        if (conresponse != null && Object.keys(conresponse.config).length > 0) {
                            oldConfig = angular.copy(conresponse);
                            $scope.config = angular.copy(conresponse);
                        }
                        else {
                            var defaultConfig = {};
                            if ($scope.configType && Array.isArray($scope.configType.properties)) {
                                for (var i = 0; i < $scope.configType.properties.length; i++) {
                                    var property = $scope.configType.properties[i];
                                    if (property && property.name) {
                                        defaultConfig[property.name] = property.defaultValue;
                                    }
                                }
                            }
                            oldConfig = angular.copy({ config: defaultConfig });
                            $scope.config = { config: defaultConfig };
                        }
                    });
                });
            }

            $scope.dismissPanel = function () {
                $scope.$panelInstance.dismiss();
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });

            $scope.isEnabled = $scope.eventsConfig.eventsListeners.indexOf(id) >= 0;

            const removeEmptyStringInMultivaluedList = function (properties, config) {
                if (!properties) {
                    return;
                }
                for (var i = 0; i < properties.length; i++) {
                    var prop = properties[i];
                    if (prop.type === 'MultivaluedList') {
                        var configProperty = config[prop.name];
                        if (configProperty && configProperty[0] === "") {
                            config[prop.name] = [];
                        }
                    }
                }
            };

            $scope.isTestConfigEnabled = function (config, properties) {
                return $scope.isEnabled && $scope.config?.config &&
                    configCheckNullAuthentication(
                        $scope.config.config,
                        $scope.configType.properties
                    );
            };

            const isConfigValid = function () {
                if ($scope.config && $scope.config.config) {
                    ComponentUtils.convertAllMultivaluedStringValuesToList(
                        $scope.configType.properties,
                        oldConfig.config
                    );
                    ComponentUtils.convertBackToMultivaluedList(
                        $scope.configType.properties,
                        oldConfig.config
                    );
                    ComponentUtils.addLastEmptyValueToMultivaluedLists(
                        $scope.configType.properties,
                        oldConfig.config
                    );
                    removeEmptyStringInMultivaluedList(
                        $scope.configType.properties,
                        $scope.config.config
                    );
                }
                if (!$scope.isEnabled && $scope.oldStatus !== $scope.isEnabled) {
                    $scope.toggleChanged = true;
                } else if (
                    $scope.isEnabled && $scope.config?.config &&
                    configCheckNullAuthentication(
                        $scope.config.config,
                        $scope.configType.properties
                    )
                ) {
                    if ($scope.config && $scope.config.config) {
                        if (
                            !angular.equals($scope.config.config, oldConfig.config)
                        ) {
                            $scope.toggleChanged = true;
                        } else if (
                            !angular.equals($scope.oldStatus, $scope.isEnabled)
                        ) {
                            $scope.toggleChanged = true;
                        } else {
                            $scope.toggleChanged = false;
                        }
                    }
                } else {
                    $scope.toggleChanged = false;
                }
            };

            $scope.$watch('isEnabled', function () {
                isConfigValid();
            }, true);

            $scope.$watch('config', function () {
                isConfigValid();

            }, true);

            $scope.test = function () {
                $scope.stage = 2;
                if ($scope.toggleChanged) {
                    $('#submit').attr('disabled', 'disabled');
                }
                var configCopy = angular.copy($scope.config);
                ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
                ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
                TestEventsConfig.save({
                    realm: realm.realm,
                    provider: id
                }, configCopy.config, function (confRes) {
                    $scope.stage = 1;
                    if ($scope.toggleChanged) {
                        $('#submit').removeAttr('disabled', 'disabled');
                    }
                    if (confRes.success) {
                        Notifications.success($translate.instant("config-test-succeeded"));
                    }
                    else {
                        Notifications.error($translate.instant(confRes.message));
                    }
                });
            };

            const sendConfig = function (copy) {
                RealmEventsConfig.update({
                    id: realm.realm
                }, copy, function () {
                    if ($scope.isEnabled) {
                        eventsConfig = copy;
                        var configCopy = angular.copy($scope.config);
                        ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
                        ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
                        if (configCopy.id) {
                            EventsConfig.update({
                                realm: realm.realm,
                                provider: id
                            }, configCopy, function (res) {
                                $scope.$panelInstance.dismiss();
                                Notifications.success($translate.instant("changesSavedSuccess"));
                            });
                        }
                        else {
                            EventsConfig.save({
                                realm: realm.realm,
                                provider: id
                            }, configCopy, function (res) {
                                $scope.$panelInstance.dismiss();
                                Notifications.success($translate.instant("changesSavedSuccess"));
                            });
                        }
                    }
                    else {
                        eventsConfig = copy;
                        $scope.$panelInstance.dismiss();
                        Notifications.success($translate.instant("changesSavedSuccess"));
                    }
                });
            };


            $scope.save = function () {
                $scope.changed = false;
                var copy = angular.copy($scope.eventsConfig);
                if ($scope.isEnabled) {
                    if (copy.eventsListeners.indexOf(id) < 0) {
                        copy.eventsListeners.push(id);
                    }
                }
                else {
                    if (copy.eventsListeners.indexOf(id) >= 0) {
                        copy.eventsListeners.splice(copy.eventsListeners.indexOf(id), 1);
                    }
                }

                if ($scope.isEnabled) {
                    $scope.subStage = 2;
                    $('#testBtn').attr('disabled', 'disabled');
                    var configCopy = angular.copy($scope.config);
                    if ($scope.configType) {
                        ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
                        ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
                    }
                    if (configCopy) {
                        TestEventsConfig.save({
                            realm: realm.realm,
                            provider: id
                        }, configCopy.config, function (confRes) {
                            $scope.subStage = 1;
                            $('#testBtn').removeAttr('disabled', 'disabled');
                            if (confRes.success) {
                                sendConfig(copy);
                            }
                            else {
                                Notifications.error($translate.instant(confRes.message));
                            }
                        });
                    } else {
                        $scope.subStage = 1;
                        $('#testBtn').removeAttr('disabled', 'disabled');
                        sendConfig(copy);
                    }
                }
                else {
                    $scope.subStage = 1;
                    $('#testBtn').removeAttr('disabled', 'disabled');
                    sendConfig(copy);
                }
            };

        };

        $http.get(resourceUrl + '/partials/panels/log-listener-config-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$route',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                    console.log('panel was closed with result : ', result);
                }).catch(function (error) {
                    console.log('panel was rejected with error : ', error);
                });
        });

    };

    $scope.isEnabled = function (id) {
        return eventsConfig.eventsListeners.indexOf(id) >= 0;
    };
});

module.controller('RealmAdminEventsCtrl', function ($scope, RealmAdminEvents, $http, realm, eventsConfig, serverInfo, $modal, $filter, RealmEventsConfig, RealmAdminEvents, RealmAdminEventsGenerator, angularSlideOutPanel, Notifications, Dialog, $route, eventsLength, adminLogsEnabled, Session, $translate, Client, Admin) {
    $scope.realm = realm;
    $scope.page = 0;
    $scope.loaderTypeName = 'tableListLoader';
    $scope.eventsConfig = eventsConfig;
    $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === "true" ? true : false;
    $scope.adminLogsEnabled = adminLogsEnabled;
    $scope.adminLogsEnabled.adminEventsEnabled = false;
    if ($scope.eventsConfig.adminEventsEnabled != undefined && $scope.eventsConfig.adminEventsEnabled) {
        $scope.adminLogsEnabled.adminEventsEnabled = true;
    }
    $scope.eventsLength = eventsLength;
    $scope.eventsLength.adminLogs = 0;
    $scope.entries = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.navigationLoaded = false;
    $scope.maxValueOfPage = 0;
    $scope.query = {
        id: realm.realm,
        max: Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5,
        first: 0,
        last: '7d'
    };
    $scope.moreInfo = $translate.instant('more-details');

    $scope.adminEnabledEventOperationsOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': serverInfo.enums['operationType']
    };

    $scope.adminEnabledEventResourceTypesOptions = {
        'multiple': true,
        'simple_tags': true,
        'tags': serverInfo.enums['resourceType']
    };

    $scope.clientSelectOptions = {
        minimumInputLength: 0,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = { results: [] };
            Client.query({ realm: realm.realm, search: true, clientName: query.term.trim(), max: 20 }, function (response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function (object, container, query) {
            object.text = object.name;
            return object.name;
        }
    };

    $scope.userSelectOptions = {
        minimumInputLength: 0,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = { results: [] };
            Admin.query({ realm: realm.realm, search: query.term.trim(), max: 20 }, function (response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function (object, ncontainer, query) {
            object.text = object.username;
            return object.username;
        }
    };


    $scope.disablePaste = function (e) {
        e.preventDefault();
        return false;
    };

    $scope.$watch('query.max', function () {
        $scope.query.first = 0;
        $scope.update();
    }, true);

    $scope.update = function () {
        if ($scope.query.authClient && $scope.query.authClient.name) {
            $scope.query.authClient = $scope.query.authClient.clientId;
        }
        if ($scope.query.authUser && $scope.query.authUser.id) {
            $scope.query.authUser = $scope.query.authUser.id;
        }

        Session.setItem("pageFilterCount", $scope.query.max);
        oldQueryUpdate();
        $scope.queryFinished = false;
        $scope.query.first = 0;
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
            }
        }
    };
    function oldQueryUpdate() {
        $scope.lengthQuery = {};
        let querySelectorValue = {};
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
            }
            else if (i !== "max" && i !== "first") {
                querySelectorValue = {
                    [i]: $scope.query[i]
                };
                Object.assign($scope.lengthQuery, querySelectorValue);
            }
        }
        RealmAdminEvents.get($scope.query, function (res) {
            $scope.entries = res.total;
            $scope.events = res.representation;
            $scope.queryFinished = true;
            if (typeof $scope.events !== 'undefined' && $scope.events)
                $scope.eventsLength.adminLogs = $scope.events.length;
            else
                $scope.eventsLength.adminLogs = 0;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                $scope.pageCount = $scope.entries;
                $scope.startCount = 1;
            }

            else {
                $scope.pageCount = $scope.query.max;
                $scope.startCount = 1;
            }
        });
    }
    $scope.reset = function () {
        $scope.query.first = 0;
        $scope.query.max = 5;
        $scope.query.operationTypes = '';
        $scope.query.resourceTypes = '';
        $scope.query.resourcePath = '';
        $scope.query.authRealm = '';
        $scope.query.authClient = '';
        $scope.query.authUser = '';
        $scope.query.authIpAddress = '';
        $scope.query.dateFrom = '';
        $scope.query.dateTo = '';
        $scope.query.last = '7d';
        $scope.update();
    };

    $scope.generateReport = function () {
        //Remove first key and set max equals totalEntries while report generation to include all logs
        const _query = { ...$scope.query };
        delete _query.first;
        _query.max = $scope.entries;
        RealmAdminEventsGenerator.get(_query, function (res) {
            if (res.status == 204)
                Notifications.success($translate.instant('report-triggered-msg'));
            else
                Notifications.error($translate.instant('report-queued-msg'));
        });
    };

    $scope.queryUpdate = function () {
        if ($scope.pageCount === $scope.entries) {
            $scope.queryFinished = true;
        }
        for (var i in $scope.query) {
            if ($scope.query[i] === '') {
                delete $scope.query[i];
            }
        }

        RealmAdminEvents.get($scope.query, function (res) {
            $scope.navigationLoaded = false;
            $scope.entries = res.total;
            $scope.events = res.representation;
            $scope.queryFinished = true;
        });

        if (typeof $scope.events !== 'undefined' && $scope.events) {
            $scope.eventsLength.adminLogs = $scope.events.length;
        }
        else {
            $scope.eventsLength.adminLogs = 0;
        }
    };

    $scope.firstPage = function () {
        $scope.navigationLoaded = true;
        $scope.pageCount = parseInt($scope.query.max);
        $scope.query.first = 0;
        $scope.startCount = 1;
        $scope.queryUpdate();
    };

    $scope.previousPage = function () {
        $scope.navigationLoaded = true;
        $scope.startCount = $scope.startCount - $scope.query.max;
        let pageCount = $scope.pageCount;
        let queryMax = parseInt($scope.query.max);
        if (pageCount === 0) {
            $scope.pageCount = queryMax - 5;
        }
        else if (pageCount === $scope.entries) {
            let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
            $scope.pageCount = pageCount - backFlow;
        }
        else {
            $scope.pageCount -= queryMax;
        }
        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.queryUpdate();
    };

    $scope.nextPage = function () {
        $scope.navigationLoaded = true;
        $scope.startCount = $scope.pageCount + 1;
        let fullLenthOfPages = parseInt($scope.query.max) + $scope.pageCount;
        $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
        $scope.pageCount = ($scope.entries < fullLenthOfPages) ? $scope.entries : fullLenthOfPages;
        $scope.query.first += parseInt($scope.query.max);
        $scope.queryUpdate();
    };


    $scope.refresh = function () {
        $route.reload();
    };

    $scope.gotoSettings = function () {
        var controller = function ($scope, $location) {
            $scope.adminLogsEnabled = adminLogsEnabled;
            $scope.eventsConfig = eventsConfig;
            $scope.eventsLength = eventsLength;
            $scope.realm = realm;

            var oldCopy = angular.copy($scope.eventsConfig);
            $scope.changed = false;

            $scope.$watch('eventsConfig', function () {
                if (!angular.equals($scope.eventsConfig, oldCopy)) {
                    $scope.changed = true;
                }
                if (angular.equals($scope.eventsConfig, oldCopy)) {
                    $scope.changed = false;
                }
            }, true);

            $scope.save = function () {
                $scope.changed = false;

                var copy = angular.copy($scope.eventsConfig);

                copy.adminEventsDetailsEnabled = copy.adminEventsEnabled;

                RealmEventsConfig.update({
                    id: realm.realm
                }, copy, function () {
                    $scope.$panelInstance.dismiss('this is from the controller!!');
                    $route.reload();
                    Notifications.success($translate.instant("changes-saved-to-realm"));
                    if ($scope.eventsConfig.adminEventsEnabled != undefined && $scope.eventsConfig.adminEventsEnabled) {
                        $scope.adminLogsEnabled.adminEventsEnabled = true;
                    }
                    else {
                        $scope.adminLogsEnabled.adminEventsEnabled = false;
                    }
                });
            };

            $scope.clearEvents = function () {
                Dialog.confirmDelete($scope.realm.realm, 'events', function () {
                    RealmAdminEvents.remove({ id: $scope.realm.realm }, function () {
                        $scope.$panelInstance.dismiss('this is from the controller!!');
                        $route.reload();
                        Notifications.success($translate.instant("admin-logs-cleared"));
                    });
                });
            };

            $scope.dismissPanel = function () {
                $route.reload();
                $scope.$panelInstance.dismiss('this is from the controller!!');
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });
        };

        $http.get(resourceUrl + '/partials/panels/admin-logs-settings-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                    console.log('panel was closed with result : ', result);
                }).catch(function (error) {
                    console.log('panel was rejected with error : ', error);
                    $route.reload();
                });
        });

    };

    $scope.viewRepresentation = function (event) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/realm-events-admin-representation.html',
            controller: 'RealmAdminEventsModalCtrl',
            backdrop: 'static',
            resolve: {
                event: function () {
                    return event;
                }
            }
        });
    };
});


module.controller('RealmEventHistoryCtrl', function ($scope, $http, AuditHistory, realm, eventsConfig, $modal,adminLogsEnabled, Session, $translate) {
    $scope.realm = realm;
    $scope.page = 0;
    $scope.loading = true;
    $scope.loaderTypeName = 'tableListLoader';
    $scope.eventsConfig = eventsConfig;
    $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === "true" ? true : false;
    $scope.adminLogsEnabled = adminLogsEnabled;
    $scope.adminLogsEnabled.adminEventsEnabled = false;
    $scope.histories = [];
    $scope.entries = 0;
    $scope.query = {
        max: 5,
        first: 0
    };

    if ($scope.eventsConfig.adminEventsEnabled != undefined && $scope.eventsConfig.adminEventsEnabled) {
        $scope.adminLogsEnabled.adminEventsEnabled = true;
    }

    $scope.getHistories = () => {
        AuditHistory.query({ realm: $scope.realm.realm, ...$scope.query}, function (res) {
            $scope.histories = res.transactions;
            $scope.entries = res.totalCount;
            $scope.loading = false;
        });
    }

     $scope.oldState = () => {
        $scope.query.first = 0;
    }

    $scope.initQuery = () => {
        $scope.oldState()
        $scope.query.search = "";
        $scope.getHistories()
    }

    $scope.previousPage = () => {
        $scope.query.first -= $scope.query.max;
        $scope.query.last -= $scope.query.max;
        $scope.getHistories()
    }

    $scope.nextPage = () => {
        $scope.query.first += $scope.query.max;
        $scope.query.last += $scope.query.max;
        $scope.getHistories()
    }

    $scope.changeCurrentTab = (tab) => {
        $scope.currentTab = tab;
    }

    $scope.changePageCount = () => {
        $scope.oldState();
        $scope.getHistories()
    }

    $scope.makeSearch = () => {
        $scope.oldState()
        $scope.getHistories()
    }

    $scope.initQuery();

    $scope.refresh = function () {
        $scope.initQuery();
    };

    $scope.showHistory = (requestId) => {
        var controller = function ($scope, $modalInstance, requestId, AuditHistoryById, realm) {
            $scope.requestId = requestId;
            $scope.workflowHistory = [];
            $scope.loading = true;

            $scope.sanitizeName = (name) => {
                name = name.replace("EZ_", "").replaceAll("_", " ");
                name = name.includes("VALIDATOR") ? "VALIDATOR" : name;
                return name;
            }
            
            $scope.getMargin = (text, name) => {
                if ($scope.sanitizeName(name) === "VALIDATOR") {
                    return;
                }
                text = $translate.instant(text);
                return Math.ceil(text.length/59);
            }

            AuditHistoryById.query({ realm: realm.realm, requestId: requestId }, function (res) {
                $scope.workflowHistory = res;
                $scope.loading = false;
            });

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/workflow-history.html',
            controller: controller,
            backdrop: 'static',
            size: 'md',
            resolve: {
                requestId : () => requestId,
                realm: () => $scope.realm,
            }
        });
    }
});

module.controller('RealmAdminEventsModalCtrl', function ($scope, $filter, event, $modalInstance) {
    $scope.event = event;
    $scope.cancel = function () {
        $modalInstance.dismiss();
    };
    $scope.$on('$routeChangeStart', function () {
        $modalInstance.close();
    });
});

module.controller('RealmBruteForceCtrl', function ($scope, Realm, realm, $http, $location, Dialog, Notifications, TimeUnit, $route, ConfirmExitDialog, $translate) {

    $scope.realm = realm;
    $scope.stage = 1;

    $scope.realm.waitIncrementUnit = TimeUnit.autoUnit(realm.waitIncrementSeconds);
    $scope.realm.waitIncrement = TimeUnit.toUnit(realm.waitIncrementSeconds, $scope.realm.waitIncrementUnit);

    $scope.realm.minimumQuickLoginWaitUnit = TimeUnit.autoUnit(realm.minimumQuickLoginWaitSeconds);
    $scope.realm.minimumQuickLoginWait = TimeUnit.toUnit(realm.minimumQuickLoginWaitSeconds, $scope.realm.minimumQuickLoginWaitUnit);

    $scope.realm.maxFailureWaitUnit = TimeUnit.autoUnit(realm.maxFailureWaitSeconds);
    $scope.realm.maxFailureWait = TimeUnit.toUnit(realm.maxFailureWaitSeconds, $scope.realm.maxFailureWaitUnit);

    $scope.realm.maxDeltaTimeUnit = TimeUnit.autoUnit(realm.maxDeltaTimeSeconds);
    $scope.realm.maxDeltaTime = TimeUnit.toUnit(realm.maxDeltaTimeSeconds, $scope.realm.maxDeltaTimeUnit);

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;
    $scope.openRedirect = false;

    $scope.$watch('realm', function () {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function () {
        $scope.stage = 2;
        $('#cancelBtn').attr('disabled', 'disabled');
        var realmCopy = angular.copy($scope.realm);
        delete realmCopy["waitIncrementUnit"];
        delete realmCopy["waitIncrement"];
        delete realmCopy["minimumQuickLoginWaitUnit"];
        delete realmCopy["minimumQuickLoginWait"];
        delete realmCopy["maxFailureWaitUnit"];
        delete realmCopy["maxFailureWait"];
        delete realmCopy["maxDeltaTimeUnit"];
        delete realmCopy["maxDeltaTime"];

        realmCopy.waitIncrementSeconds = TimeUnit.toSeconds($scope.realm.waitIncrement, $scope.realm.waitIncrementUnit);
        realmCopy.minimumQuickLoginWaitSeconds = TimeUnit.toSeconds($scope.realm.minimumQuickLoginWait, $scope.realm.minimumQuickLoginWaitUnit);
        realmCopy.maxFailureWaitSeconds = TimeUnit.toSeconds($scope.realm.maxFailureWait, $scope.realm.maxFailureWaitUnit);
        realmCopy.maxDeltaTimeSeconds = TimeUnit.toSeconds($scope.realm.maxDeltaTime, $scope.realm.maxDeltaTimeUnit);

        $scope.changed = false;
        Realm.update(realmCopy, function () {
            $scope.stage = 1;
            $('#cancelBtn').removeAttr('disabled', 'disabled');
            oldCopy = angular.copy($scope.realm);
            $scope.openRedirect = true;
            $location.url("/realms/" + realm.realm + "/defense/brute-force");
            Notifications.success($translate.instant("changes-saved-to-realm"));
        }, function () {
            $scope.stage = 1;
            $('#cancelBtn').removeAttr('disabled', 'disabled');
        });
    };

    $scope.reset = function () {
        $scope.openRedirect = true;
        $route.reload();
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('RealmAnomalyCtrl', function ($scope, TestEventsConfig, RealmEventsConfig, eventsConfig, Realm, realm, $http, $location, Dialog, Notifications, TimeUnit, $route, EventsConfig, EventsConfigDescription, ComponentUtils, $translate) {
    var id = "qv-anamoly-detection";
    $scope.realm = realm;
    $scope.oldStatus = eventsConfig.eventsListeners.indexOf(id) >= 0;
    $scope.toggleChanged = false;
    let oldConfig = null;
    $scope.stage = 1;
    $scope.subStage = 1;
    $scope.isEnabled = { value: eventsConfig.eventsListeners.indexOf(id) >= 0 };
    EventsConfig.get({ realm: realm.realm, provider: id }, function (conresponse) {
        EventsConfigDescription.get({ realm: realm.realm, provider: id }, function (response) {
            $scope.configType = response;
            if (conresponse != null && Object.keys(conresponse.config).length > 0) {
                oldConfig = angular.copy(conresponse);
                $scope.config = angular.copy(conresponse);
            }
            else {
                var defaultConfig = {};
                if ($scope.configType && Array.isArray($scope.configType.properties)) {
                    for (var i = 0; i < $scope.configType.properties.length; i++) {
                        var property = $scope.configType.properties[i];
                        if (property && property.name) {
                            defaultConfig[property.name] = property.defaultValue;
                        }
                    }
                }
                oldConfig = angular.copy({ config: defaultConfig });
                $scope.config = { config: defaultConfig };
            }
        });
    });
    const removeEmptyStringInMultivaluedList = function (properties, config) {
        if (!properties) {
            return;
        }
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
            if (prop.type === 'MultivaluedList') {
                var configProperty = config[prop.name];
                if (configProperty && configProperty[0] === "") {
                    config[prop.name] = [];
                }
            }
        }
    };
    $scope.$watch('isEnabled.value', function () {
        if (!angular.equals($scope.oldStatus, $scope.isEnabled.value) && $scope.isEnabled.value && checkConfigNull($scope.config.config, $scope.configType.properties)) {
            $scope.toggleChanged = true;
        } else if (!angular.equals($scope.oldStatus, $scope.isEnabled.value) && !$scope.isEnabled.value) {
            $scope.toggleChanged = true;
        } else {
            $scope.toggleChanged = false;
        }
    }, true);

    $scope.$watch('config', function () {
        if ($scope.config && $scope.config.config) {
            ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, oldConfig.config);
            ComponentUtils.convertBackToMultivaluedList($scope.configType.properties, oldConfig.config);
            ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.configType.properties, oldConfig.config);
            removeEmptyStringInMultivaluedList($scope.configType.properties, $scope.config.config);
            if (!angular.equals($scope.config.config, oldConfig.config) && $scope.isEnabled.value && checkConfigNull($scope.config.config, $scope.configType.properties)) {
                $scope.toggleChanged = true;
            } else if (!angular.equals($scope.oldStatus, $scope.isEnabled.value) && !$scope.isEnabled.value) {
                $scope.toggleChanged = true;
            } else {
                $scope.toggleChanged = false;
            }
        }
    }, true);

    $scope.test = function () {
        $scope.stage = 2;
        $('#submit').attr('disabled', 'disabled');
        var configCopy = angular.copy($scope.config);
        ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
        ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
        TestEventsConfig.save({
            realm: realm.realm,
            provider: id
        }, configCopy.config, function (confRes) {
            $scope.stage = 1;
            $('#submit').removeAttr('disabled', 'disabled');
            if (confRes.success) {
                Notifications.success($translate.instant("config-test-succeeded"));
            }
            else {
                Notifications.error(confRes.message);
            }
        });
    };

    const sendConfig = function (copy) {
        RealmEventsConfig.update({
            id: realm.realm
        }, copy, function () {
            if ($scope.isEnabled.value) {
                eventsConfig = copy;
                var configCopy = angular.copy($scope.config);
                ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
                ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
                if (configCopy.id) {
                    EventsConfig.update({
                        realm: realm.realm,
                        provider: id
                    }, configCopy, function (res) {
                        Notifications.success($translate.instant("changesSavedSuccess"));
                    });
                }
                else {
                    EventsConfig.save({
                        realm: realm.realm,
                        provider: id
                    }, configCopy, function (res) {
                        Notifications.success($translate.instant("changesSavedSuccess"));
                    });
                }
            }
            else {
                eventsConfig = copy;
                $scope.oldStatus = $scope.isEnabled.value;
                Notifications.success($translate.instant('changesSavedSuccess'));
            }
            $location.url("/realms/" + realm.realm + "/defense/anomaly/");
        });
    };

    $scope.save = function () {
        $scope.changed = false;
        var copy = angular.copy(eventsConfig);
        $scope.toggleChanged = false;
        if ($scope.isEnabled.value) {
            if (copy.eventsListeners.indexOf(id) < 0) {
                copy.eventsListeners.push(id);
            }
        }
        else {
            if (copy.eventsListeners.indexOf(id) >= 0) {
                copy.eventsListeners.splice(copy.eventsListeners.indexOf(id), 1);
            }
        }

        if ($scope.isEnabled.value) {
            $scope.subStage = 2;
            $('#testBtn').attr('disabled', 'disabled');
            var configCopy = angular.copy($scope.config);
            if ($scope.configType) {
                ComponentUtils.convertAllMultivaluedStringValuesToList($scope.configType.properties, configCopy.config);
                ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType.properties, configCopy.config);
            }
            if (configCopy && $scope.configType.hasTestConfig) {
                TestEventsConfig.save({
                    realm: realm.realm,
                    provider: id
                }, configCopy.config, function (confRes) {
                    $scope.subStage = 1;
                    $('#testBtn').removeAttr('disabled', 'disabled');
                    if (confRes.success) {
                        oldConfig.config = angular.copy(configCopy.config);
                        $scope.oldStatus = $scope.isEnabled.value;
                        sendConfig(copy);
                    }
                    else {
                        Notifications.error($translate.instant(confRes.message));
                        $scope.reset();
                    }
                });
            } else {
                $scope.subStage = 1;
                $('#testBtn').removeAttr('disabled', 'disabled');
                sendConfig(copy);
            }
        }
        else {
            $scope.subStage = 1;
            $('#testBtn').removeAttr('disabled', 'disabled');
            sendConfig(copy);
        }
    };
    $scope.reset = function () {
        $route.reload();
    };
});


module.controller('IdentityProviderMapperListCtrl', function ($scope, realm, identityProvider, mapperTypes, mappers) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.mapperTypes = mapperTypes;
    $scope.mappers = mappers;
});

module.controller('IdentityProviderMapperCtrl', function ($scope, realm, identityProvider, mapperTypes, mapper, IdentityProviderMapper, Notifications, Dialog, $location, $translate) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.create = false;
    $scope.mapper = angular.copy(mapper);
    $scope.changed = false;
    $scope.mapperType = mapperTypes[mapper.identityProviderMapper];
    $scope.$watch(function () {
        return $location.path();
    }, function () {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('mapper', function () {
        if (!angular.equals($scope.mapper, mapper)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function () {
        IdentityProviderMapper.update({
            realm: realm.realm,
            alias: identityProvider.alias,
            mapperId: mapper.id
        }, $scope.mapper, function () {
            $scope.changed = false;
            mapper = angular.copy($scope.mapper);
            $location.url("/realms/" + realm.realm + '/identity-provider-mappers/' + identityProvider.alias + "/mappers/" + mapper.id);
            Notifications.success($translate.instant("changesSavedSuccess"));
        });
    };

    $scope.reset = function () {
        $scope.mapper = angular.copy(mapper);
        $scope.changed = false;
    };

    $scope.cancel = function () {
        //$location.url("/realms");
        window.history.back();
    };

    $scope.remove = function () {
        Dialog.confirmDelete($scope.mapper.name, 'mapper', function () {
            IdentityProviderMapper.remove({ realm: realm.realm, alias: mapper.identityProviderAlias, mapperId: $scope.mapper.id }, function () {
                Notifications.success($translate.instant("mapper-deleted"));
                $location.url("/realms/" + realm.realm + '/identity-provider-mappers/' + identityProvider.alias + "/mappers");
            });
        });
    };

});

module.controller('IdentityProviderMapperCreateCtrl', function ($scope, realm, identityProvider, mapperTypes, IdentityProviderMapper, Notifications, Dialog, $location, $translate) {
    $scope.realm = realm;
    $scope.identityProvider = identityProvider;
    $scope.create = true;
    $scope.mapper = { identityProviderAlias: identityProvider.alias, config: {} };
    $scope.mapperTypes = mapperTypes;

    // make first type the default
    $scope.mapperType = mapperTypes[Object.keys(mapperTypes)[0]];

    $scope.$watch(function () {
        return $location.path();
    }, function () {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function () {
        $scope.mapper.identityProviderMapper = $scope.mapperType.id;
        IdentityProviderMapper.save({
            realm: realm.realm, alias: identityProvider.alias
        }, $scope.mapper, function (data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + '/identity-provider-mappers/' + identityProvider.alias + "/mappers/" + id);
            Notifications.success($translate.instant("mapper-created"));
        });
    };

    $scope.cancel = function () {
        //$location.url("/realms");
        window.history.back();
    };


});

module.controller('RealmFlowBindingCtrl', function ($scope, flows, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications) {
    $scope.flows = [];
    $scope.clientFlows = [];
    for (var i = 0; i < flows.length; i++) {
        if (flows[i].providerId == 'client-flow') {
            $scope.clientFlows.push(flows[i]);
        } else {
            $scope.flows.push(flows[i]);
        }
    }

    $scope.profileInfo = serverInfo.profileInfo;

    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/authentication/flow-bindings");
});


module.controller('CreateFlowCtrl', function ($scope, realm,
    AuthenticationFlows,
    Notifications, $location, $translate) {
    console.debug('CreateFlowCtrl');
    $scope.realm = realm;
    $scope.flow = {
        alias: "",
        providerId: "basic-flow",
        description: "",
        topLevel: true,
        builtIn: false
    };

    $scope.save = function () {
        AuthenticationFlows.save({ realm: realm.realm, flow: "" }, $scope.flow, function (res) {
            $location.url("/realms/" + realm.realm + "/authentication/flows/" + $scope.flow.alias);
            Notifications.success($translate.instant("flow-created"));
        });
    };
    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/authentication/flows");
    };
});


module.controller('ChooseFlowCtrl', function ($scope, realm, $filter,
    SaveCustomAuthenticationFlows,
    Notifications, $location, CreateCustomTemplate, IntroJs) {
    $scope.realm = realm;

    $scope.chooseFlows = ['onboarding', 'login', 'recovery'];

    $scope.onChooseFlow = function (flowChosen) {
        CreateCustomTemplate.open('Create ' + flowChosen.charAt(0).toUpperCase() + flowChosen.slice(1) + ' Flow', flowChosen, '', function (name, flowType) {
            $scope.choosenFlow = flowChosen;
            if (flowType != null && flowType === 'direct_grant') {
                flowChosen = 'direct_grant';
            }
            var templateRep = {
                alias: name,
                providerId: "basic-flow",
                description: $translate.instant('auth-desc-custom') + $translate.instant(flowChosen) + ' ' + $translate.instant('auth-flow-desc') + ' ' + name,
                topLevel: true,
                builtIn: false,
                templateAlias: $translate.instant('login-template-alias', { flowChosen, name }),
                installed: true,
                templateType: flowChosen,
            };
            SaveCustomAuthenticationFlows.save({ realm: realm.realm, templatetype: flowChosen }, templateRep, function (res) {
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + res.id + '/' + $filter('removeUnderScore')(flowChosen) + '/flow');
                Notifications.success(name + " is saved successfully.");
            }, function (err) {
                err && err.data && err.data.errorMessage ? Notifications.error($translate.instant(err.data.errorMessage)) : (err.data.error ? Notifications.error($translate.instant(err.data.error)) : Notifications.error($translate.instant('could-not-create') + $translate.instant($scope.choosenFlow)));
            });
        });

    };
});

module.controller('AuthenticationDrawFlowsCtrl', function ($scope, $q, $http, $translate, $route, $location, flow, realm, authenticatorProviders, Notifications, angularSlideOutPanel, Dialog, AuthenticationFlows, AuthenticationConfig, ComponentUtils, AuthenticationExecutionConfig, AuthenticationExecution, CreateExecution, AuthenticationFlowExecutions, AuthenticationFlowPriority, passwordPolicies) {
    $scope.realm = realm;
    const whitelistedAuthenticators = ["allow-access-authenticator", "deny-access-authenticator", "auth-cookie", "auth-spnego", "docker-http-basic-authenticator", "http-basic-authenticator", "qv-idp-param-forwarder", "offline-authenticator"];
    const filteredAuthenticatorProviders = authenticatorProviders.filter(element => !whitelistedAuthenticators.includes(element.id));
    $scope.providers = filteredAuthenticatorProviders;
    $scope.currentFlow = flow;
    $scope.displayFlow = angular.copy(flow);
    setTree();
    $scope.deleteFlowContent = $translate.instant('delete-flow-content', { type: $scope.currentFlow.templateType });
    $scope.authExecutions = [];
    $scope.nodeClicked = false;
    $scope.isMultipleLoginAuthenticatorFullyConfigured = false;

    $scope.findProviders = function (providersList, searchValue) {
        if (searchValue && searchValue.trim()) {
            for (var itr = 0; itr < providersList.length; itr++) {
                if (providersList[itr].displayName.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };


    // ************** Generate the tree diagram	 *****************
    var leftMargin =  $scope.isRTL() ? 320 : 120;
    var margin = { top: 60, right: 120, bottom: 20, left: leftMargin },
        width = 960 - margin.right - margin.left,
        height = 600 - margin.top - margin.bottom;

    var circle = { r: 35 };
    var rect = { h: 75, w: 250 };
    var circleTextPosition = 55;
    var depth = 120;
    var dragNodeName, dragNodeId;

    var i = 0;

    var dragStarted = false;
    var nodes, nodesExit, links, domNode, panTimer, selectedNode = null, draggingNode, relCoords;
    var panSpeed = 200;
    var panBoundary = 10;

    var tree, diagonal, svg, root, orginalRoot = [];

    // define the zoomListener which calls the zoom function on the "zoom" event constrained within the scaleExtents
    var zoomListener = d3.behavior.zoom().scaleExtent([0.1, 3]).on("zoom", zoom);

    var init = function () {

        d3.select("svg").remove();

        tree = d3.layout.tree()
            .size([height, width]);

        diagonal = d3.svg.diagonal()
            .projection(function (d) { return [d.x, d.y]; });

        svg = d3.select("#drawflow").append("svg")
            .attr("width", width + margin.right + margin.left)
            .attr("height", "400%")
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        root = $scope.treeData[0];
        orginalRoot = $scope.treeData[0];
        root.x0 = height / 2;
        root.y0 = 0;

        render();

        d3.selectAll(".link")
            .each(animate);
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

    var zoom = function () {
        svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    };

    var reiterateHierarchy = function () {
        if (selectedNode != null && draggingNode != null) {
            let flowUpdated = false;
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
            childCount(0, orginalRoot);
            var tempNodes = d3.layout.tree().nodes(orginalRoot);
            if (selectedNode.y0 < draggingNode.y0) {
                var selectedIndex = tempNodes.findIndex(x => x.name == selectedNode.name);
                var draggingIndex = tempNodes.findIndex(x => x.name == draggingNode.name);
                tempNodes.splice(selectedIndex + 1, 0, tempNodes[draggingIndex]);
                if (draggingIndex < selectedIndex) {
                    tempNodes.splice(draggingIndex, 1);
                }
                else {
                    tempNodes.splice(draggingIndex + 1, 1);
                }
                flowUpdated = true;
            }

            var sortedItems = [];

            tempNodes.forEach((element, index) => {
                var item = {
                    "name": element.name,
                    "isDefault": element.isDefault,
                    "isPlaceHolder": element.isPlaceHolder,
                    "nodeId": element.nodeId,
                    "providerId": element.providerId,
                    "isConfigurable": element.isConfigurable,
                    "hasConfig": element.hasConfig,
                    "requirement": element.requirement,
                    "priority": element.priority,
                    "parent": index == 0 ? "null" : tempNodes[index - 1].name,
                    "requirementChoices": element.requirementChoices
                };

                if (index != (tempNodes.length - 1)) {
                    item["children"] = null;
                }
                sortedItems.push(item);
            });
            var reps = [];
            var index = 0;
            sortedItems.forEach(element => {
                if (!element.isDefault && !element.isPlaceHolder) {
                    var item = {
                        "executionId": element.nodeId,
                        "priority": index
                    };
                    reps.push(item);
                    index = index + 1;
                }
            });

            $scope.$evalAsync(function () {
                if (flowUpdated) {
                    AuthenticationFlowPriority.save({ realm: realm.realm }, reps, function () {
                        Notifications.success($translate.instant("priority-of-execution-updated"));
                    });
                }
            });

            list_to_tree(sortedItems);

            return sortedItems;
        }
    };

    var list_to_tree = function (list) {
        var map = {}, node, roots = [], i;

        for (i = 0; i < list.length; i += 1) {
            map[list[i].name] = i; // initialize the map
            list[i].children = []; // initialize the children
        }

        for (i = 0; i < list.length; i += 1) {
            node = list[i];
            if (node.parent !== "null") {
                // if you have dangling branches check that map[node.parentId] exists
                list[map[node.parent]].children.push(node);
            } else {
                roots.push(node);
            }
        }
        return roots;
    };

    var overNode = function (d) {
        selectedNode = d;
        updateTempConnector();
    };

    var outNode = function (d) {
        selectedNode = null;
        updateTempConnector();
    };

    var updateTempConnector = function () {
        var data = [];
        if (draggingNode !== null && selectedNode !== null) {
            // have to flip the source coordinates since we did this for the existing connectors on the original tree
            data = [{
                source: {
                    x: (draggingNode.x0 + (100 / 2)),
                    y: (draggingNode.y0)
                },
                target: {
                    x: selectedNode.x0,
                    y: selectedNode.y0
                }
            }];
        }
        var link = svg.selectAll(".templink").data(data);

        link.enter().append("path")
            .attr("class", "templink")
            .attr("d", d3.svg.diagonal())
            .attr('pointer-events', 'none');

        link.attr("d", d3.svg.diagonal());

        link.exit().remove();
    };

    var pan = function (domNode, direction) {
        var speed = panSpeed;
        if (panTimer) {
            clearTimeout(panTimer);
            var translateCoords = d3.transform(svg.attr("transform"));
            if (direction == 'left' || direction == 'right') {
                translateX = translateCoords.translate[0];
                translateY = direction == 'left' ? translateCoords.translate[1] + speed : translateCoords.translate[1] - speed;
            } else if (direction == 'up' || direction == 'down') {
                translateX = direction == 'up' ? translateCoords.translate[1] - speed : translateCoords.translate[1] + speed;
                translateY = translateCoords.translate[0];
            }
            d3.select(domNode).select('g.node').attr("transform", "translate(" + translateX + "," + translateY + ")");
            zoomListener.scale(zoomListener.scale());
            zoomListener.translate([translateX, translateY]);
            panTimer = setTimeout(function () {
                pan(domNode, speed, direction);
            }, 50);
        }
    };

    var initiateDrag = function (d, domNode) {
        draggingNode = d;
        d3.select(domNode).select('.ghostRect').attr('pointer-events', 'none');
        d3.selectAll('.ghostRect').attr('class', 'ghostRect show');

        d3.select(domNode).select('.ghostCircle').attr('pointer-events', 'none');
        d3.selectAll('.ghostCircle').attr('class', 'ghostCircle show');
        d3.select(domNode).attr('class', 'node activeDrag');

        svg.selectAll("g.node").sort(function (a, b) { // select the parent and sort the path's
            if (a.id != draggingNode.id) return 1; // a is not the hovered element, send "a" to the back
            else return -1; // a is the hovered element, bring "a" to the front
        });

        dragStarted = null;
    };

    var endDrag = function () {
        selectedNode = null;
        d3.selectAll('.ghostCircle').attr('class', 'ghostCircle');
        d3.selectAll('.ghostRect').attr('class', 'ghostRect');
        d3.select(domNode).attr('class', 'node');
        // now restore the mouseover event or we won't be able to drag a 2nd time
        d3.select(domNode).select('.ghostCircle').attr('pointer-events', '');
        d3.select(domNode).select('.ghostRect').attr('pointer-events', '');
        updateTempConnector();
        if (draggingNode !== null) {
            init();
            draggingNode = null;
        }
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


        var onNodeClick = function (event, d) {
            if (event.defaultPrevented || event.isDefault || event.isPlaceHolder || $scope.nodeClicked) return;
            $scope.nodeClicked = true;

            var selectedExec = $scope.authExecutions.find(x => x.id === event.nodeId);
            $scope.$evalAsync(function () {
                //need to resolve two promises,
                // One is for getting config Properties & Another is for Already set Configs for the authenticator.
                const promises = [];
                var providerId = selectedExec ? selectedExec.providerId : d.nodeId;
                if (selectedExec && selectedExec.authenticationConfig != null) {
                    let promiseOne = $http.get(authUrl + '/admin/realms/' + realm.realm + '/authentication/config/' + selectedExec.authenticationConfig);
                    promises.push(promiseOne);
                }
                let promiseTwo = $http.get(authUrl + '/admin/realms/' + realm.realm + '/authentication/config-description/' + providerId);
                promises.push(promiseTwo);
                $q.all(promises)
                    .then(data => {
                        var configProps = [];
                        var settenConfigs = {};
                        if (data.length > 1) {
                            settenConfigs = data[0].data;
                            configProps = data[1].data.properties;
                        } else {
                            configProps = data[0].data.properties;
                        }
                        $scope.gotoSettings(d, selectedExec, configProps, settenConfigs);
                    })
                    .catch(err => {
                        $scope.nodeClicked = false;
                    });
            });
        };

        // Enter the nodes.
        var nodeEnter = node.enter().append("g")
            .attr("id", function (d) { return `node-${d.providerId ?? d.name.replaceAll(" ", "-")}`; })
            .call(d3.behavior.drag()
                .on("dragstart", function (d) {
                    if (d.isDefault || d.isPlaceHolder) {
                        return;
                    }
                    dragStarted = true;
                    nodes = tree.nodes(d);
                    d3.event.sourceEvent.stopPropagation();
                })
                .on("drag", function (d) {

                    if (d.isDefault || d.isPlaceHolder) {
                        return;
                    }

                    if (dragStarted) {
                        var domNode = this;
                        initiateDrag(d, domNode);
                    }

                    relCoords = d3.mouse($('svg').get(0));
                    if (relCoords[0] < panBoundary) {
                        panTimer = true;
                        pan(this, 'left');
                    } else if (relCoords[0] > ($('svg').width() - panBoundary)) {
                        panTimer = true;
                        pan(this, 'right');
                    } else if (relCoords[1] < panBoundary) {
                        panTimer = true;
                        pan(this, 'up');
                    } else if (relCoords[1] > ($('svg').height() - panBoundary)) {
                        panTimer = true;
                        pan(this, 'down');
                    } else {
                        try {
                            clearTimeout(panTimer);
                        } catch (e) {

                        }
                    }
                    d.x0 += d3.event.dx;
                    d.y0 += d3.event.dy;
                    var node = d3.select(this);
                    node.attr("transform", "translate(" + d.x0 + "," + d.y0 + ")");
                    $(this).addClass('dragged');
                    updateTempConnector();

                })
                .on("dragend", function (d) {
                    if (d.isDefault || d.isPlaceHolder) {
                        return;
                    }
                    domNode = this;
                    var isDragged = $(this).hasClass('dragged');
                    if (selectedNode) {
                        if (selectedNode.name != draggingNode.parent.name) {
                            var hierarchy = reiterateHierarchy();
                            if (hierarchy != null) {
                                $scope.treeData = [];
                                $scope.treeData.push(hierarchy[0]);
                            }
                        }
                        endDrag();
                    } else {
                        endDrag();
                    }
                    if (!isDragged) {
                        onNodeClick(d, d.id - 1);
                    }

                }))
            .attr("class", "node")
            .attr("id", function (d) {
                return createUUID();
            })
            .attr("transform", function (d) {
                if (d.isDefault) {
                    return "translate(" + d.x + "," + d.y + ")";
                }
                else {
                    return "translate(" + (d.x - (rect.w / 2)) + "," + (d.y - (rect.h / 2)) + ")";
                }
            });

        // Draw Start and End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault;
        }).append("circle")
            .attr("r", circle.r)
            .style("fill", "#000");

        // Draw Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && !d.isPlaceHolder;
        }).append("rect")
            .attr('width', rect.w)
            .attr('height', rect.h)
            .attr("rx", 5)
            .style("fill", function (d) {
                if (d.requirement === "REQUIRED") {
                    if ((d?.requirementChoices.length > 0 && d.isConfigurable && !d.hasConfig) ||
                        (d.providerId === "multiple-login-options-authenticator" && !$scope.isMultipleLoginAuthenticatorFullyConfigured)) {
                        return configurableNodeColor;
                    }
                    return enabledNodeColor;
                } else {
                    return disabledNodeColor;
                }
            });

        // Draw Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && d.isPlaceHolder;
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
            return d.isDefault && d.parent == "null";
        }).append('svg:foreignObject')
            .attr("width", 30)
            .attr("height", 30)
            .attr("x", -15)
            .attr("y", -15)
            // .append("xhtml:body")
            .append("xhtml:div")
            .attr("class", "drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer")
            .append("xhtml:div")
            .append("xhtml:div")
            .attr("class", "d-flex justify-content-center text-center")
            .html('<i class="os-icon os-icon-user" style="color:#fff"></i>');

        // Draw Icon inside End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault && d.parent != "null";
        }).append('svg:foreignObject')
            .attr("width", 30)
            .attr("height", 30)
            .attr("x", -15)
            .attr("y", -15)
            .append("xhtml:div")
            .attr("class", "drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer")
            .append("xhtml:div")
            .append("xhtml:div")
            .attr("class", "d-flex justify-content-center text-center")
            .html('<i class="os-icon os-icon-check" style="color:#fff"></i>');


        // Draw Text Next to Start and End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault;
        }).append("text")
            .attr("x", function (d) {
                return circleTextPosition;
            })
            .attr("dy", ".35em")
            .attr("text-anchor", "middle")
            .text(function (d) { return $translate.instant(d.name); })
            .style("font-size", "14px")
            .style("font-weight", 400)
            .style("font-family", "Inter");

        // Draw Text Inside Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && !d.isPlaceHolder;
        }).append("svg:foreignObject")
            .attr("width", rect.w)
            .attr("height", rect.h)
            .attr("x", 0)
            .attr("y", 0)
            .html(function (d) {
                return "<div class='drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer'><div class='draw-activity-box'><div class='draw-activity-info d-flex align-items-center top-0 justify-content-center text-center'><span class='text-white'>" + $translate.instant(d.name) + "</span> </div></div></div>";
            });

        nodeEnter.filter(function (d, i) {
            return !d.isDefault && d.isPlaceHolder;
        }).append("svg:foreignObject")
            .attr("width", rect.w)
            .attr("height", rect.h)
            .attr("x", 0)
            .attr("y", 0)
            .html(function (d) {
                return "<div class='drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer'><div class='draw-activity-box'><div class='draw-activity-info d-flex align-items-center top-0 justify-content-center text-center'><span class='c-light-gray'>" + $translate.instant(d.name) + "</span> </div></div></div>";
            });

        // Add temp Selection Ghost around Rectangle
        nodeEnter.append("rect")
            .filter(function (d, i) {
                return !d.isDefault && !d.isPlaceHolder;
            })
            .attr('class', 'ghostRect')
            .attr('width', rect.w * 8)
            .attr('height', rect.h * 3)
            .attr('x', -(rect.w * 4))
            .attr('y', -(rect.h))
            .attr("opacity", 0) // change this to zero to hide the target area
            .style("fill", "#047bf8")
            .attr('pointer-events', 'mouseover')
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

        nodeEnter.append("rect")
            .filter(function (d, i) {
                return !d.isDefault && d.isPlaceHolder;
            })
            .attr('class', 'dragRect')
            .attr('width', rect.w)
            .attr('height', rect.h)
            .attr("opacity", 0.2) // change this to zero to hide the target area
            .style("fill", "red")
            .attr('pointer-events', 'mouseover')
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

        // Add temp Selection Ghost around Circle
        nodeEnter.append("circle")
            .filter(function (d, i) {
                return d.isDefault && d.parent === "null";
            })
            .attr('class', 'ghostCircle')
            .attr('r', circle.r * 2)
            .attr("opacity", 0) // change this to zero to hide the target area
            .style("fill", "#047bf8")
            .attr('pointer-events', 'mouseover')
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

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

    $scope.dragComp = function (evt) {
        dragNodeName = evt.target.getAttribute("data-node");
        dragNodeId = evt.target.getAttribute("data-id");
        d3.select(domNode).select('.dragRect').attr('pointer-events', 'none');
        d3.selectAll('.dragRect').attr('class', 'dragRect show');
    };

    $scope.dropComp = function (evt) {
        evt.preventDefault();

        d3.select(domNode).select('.dragRect').attr('pointer-events', '');
        d3.selectAll('.dragRect').attr('class', 'dragRect');

        $scope.$evalAsync(function () {
            var execution = {
                provider: dragNodeId,
                requirement: "REQUIRED"
            };
            CreateExecution.save({ realm: realm.realm, alias: $scope.currentFlow.alias }, execution, function () {
                Notifications.success($translate.instant("execution-created"));
                dragNodeName = null;
                dragNodeId = null;
                $route.reload();
            });
        });
    };

    $scope.dragEnd = function (evt) {
        evt.preventDefault();
        d3.select(domNode).select('.dragRect').attr('pointer-events', '');
        d3.selectAll('.dragRect').attr('class', 'dragRect');
    };

    $scope.allowDropComp = function (evt) {
        evt.preventDefault();
    };

    function setTree() {
        $scope.treeData = [
            {
                "name": $translate.instant('start-flow'),
                "parent": "null",
                "isPlaceHolder": false,
                "isDefault": true,
                "children": [
                    {
                        "name": $translate.instant("drag-and-drop-txt") + " " + $translate.instant($scope.currentFlow.templateType) + " " + $translate.instant("executors"),
                        "parent": $translate.instant('start-flow'),
                        "isPlaceHolder": true,
                        "isDefault": false,
                        "children": [{
                            "name": $translate.instant('end-flow'),
                            "isPlaceHolder": false,
                            "parent": $translate.instant("drag-and-drop-txt") + " " + $translate.instant($scope.currentFlow.templateType) + " " + $translate.instant("executors"),
                            "isDefault": true,
                        }]
                    }
                ]
            }
        ];
    }

    function queryFlows() {
        AuthenticationFlowExecutions.query({ realm: realm.realm, alias: $scope.currentFlow.alias }, function (data) {
            $scope.authExecutions = [];
            data.forEach(element => {
                if (element.multipleLoginAuthenticatorEnabled) {
                    $scope.isMultipleLoginAuthenticatorFullyConfigured = true;
                }
                if (!element.hidden) {
                    $scope.authExecutions.push(element);
                    $scope.providers.splice($scope.providers.findIndex(item => item.id === element.providerId), 1);
                }
                else if (element.authenticationFlow && $scope.currentFlow.templateType.toLowerCase() === "login") {
                    $scope.currentFlow.alias = element.displayName;
                }
            });
            if ($scope.authExecutions.length > 0) {
                setTree();
                orginalRoot = $scope.treeData[0];
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
                childCount(0, orginalRoot);
                var tempNodes = d3.layout.tree().nodes(orginalRoot);
                var sortedItems = [];

                $scope.requestArr = $scope.authExecutions.map(data => $http.get(authUrl + '/admin/realms/' + realm.realm + '/authentication/config-description/' + data.providerId));
                $q.all($scope.requestArr).then(function (values) {
                    let configRequirementDetailArr = values.map((currentValue, index) => {
                        let authConfigured = true;
                        let configData = currentValue.data;
                        let authConfig = $scope.authExecutions[index]?.config;
                        if (configData.properties) { // checking if both properties && config is preset for given authenticator
                            for (let itr = 0; itr < configData.properties.length; itr++) {
                                // checking if the given property is required
                                if (configData.properties[itr].required) {
                                    // cheking if the given property has any dependent value to be checked.
                                    if (configData.properties[itr]?.dependsOn) {
                                        for (let dependsOnName of Object.keys(configData.properties[itr]?.dependsOn)) {// since the given depends on is an object getting the name of the key which the given property is dependent on.
                                            let dependsOnValues = configData.properties[itr]?.dependsOn[`${dependsOnName}`]; // getting all the dependent values of the given key.
                                            if (authConfig?.[`${dependsOnName}`]) { // checking if given dependency is being checked in the configuration and if so proceed
                                                if (typeof dependsOnValues !== "object") { // if type is string excecute this
                                                    if (authConfig?.[`${dependsOnName}`] === "true" && (configData.properties[itr].type.toLowerCase() === 'list' && !configData.properties[itr].options.includes(authConfig?.[`${configData.properties[itr].name}`]))) {
                                                        return false;
                                                    }
                                                    else if (authConfig?.[`${dependsOnName}`] === dependsOnValues && (!authConfig?.[`${configData.properties[itr].name}`] || (configData.properties[itr].type.toLowerCase() === 'list' && !configData.properties[itr].options.includes(authConfig?.[`${configData.properties[itr].name}`])))) { // check if theres dependent value matches and if the values of current property is empty then return false.
                                                        return false;
                                                    }
                                                } else { // iterate and find if any of the given dependencies are assigned to the config.
                                                    if (dependsOnValues.indexOf(authConfig?.[`${dependsOnName}`]) >= 0 && (!authConfig?.[`${configData.properties[itr].name}`] || (configData.properties[itr].type.toLowerCase() === 'list' && !configData.properties[itr].options.includes(authConfig?.[`${configData.properties[itr].name}`])))) {
                                                        return false;
                                                    }
                                                }
                                            }
                                        }
                                        // let dependsOnName = Object.keys(configData.properties[itr]?.dependsOn)[0];
                                    } else if (!authConfig?.[`${configData.properties[itr].name}`] || (configData.properties[itr].type.toLowerCase() === 'list' && !configData.properties[itr].options.includes(authConfig?.[`${configData.properties[itr].name}`]))) {
                                        return false;
                                    }
                                }
                            }
                        }

                        return authConfigured;
                    });
                    $scope.authExecutions.forEach((element, index) => {
                        var newNode = {
                            "name": element.displayName,
                            "isDefault": false,
                            "isPlaceHolder": false,
                            "providerId": element.providerId,
                            "nodeId": element.id,
                            "isConfigurable": element.configurable,
                            "hasConfig": configRequirementDetailArr[index],
                            "requirement": element.requirement,
                            "priority": element.priority,
                            "requirementChoices": element.requirementChoices
                        };

                        tempNodes.splice(tempNodes.length - 2, 0, newNode);
                    });

                    tempNodes.forEach((element, index) => {
                        var item = {
                            "name": element.name,
                            "isDefault": element.isDefault,
                            "isPlaceHolder": element.isPlaceHolder,
                            "nodeId": element.nodeId,
                            "providerId": element.providerId,
                            "isConfigurable": element.isConfigurable,
                            "hasConfig": element.hasConfig,
                            "requirement": element.requirement,
                            "priority": element.priority,
                            "parent": index == 0 ? "null" : tempNodes[index - 1].name,
                            "requirementChoices": element.requirementChoices
                        };

                        if (index != (tempNodes.length - 1)) {
                            item["children"] = null;
                        }
                        sortedItems.push(item);
                    });

                    list_to_tree(sortedItems);

                    $scope.treeData = [];
                    $scope.treeData.push(sortedItems[0]);
                    init();
                });

            }
            else {
                init();
            }
        }, function (err) {
            Notifications.error(err.data.errorMessage);
        });
    }

    $scope.gotoSettings = function (node, selectedExec, configProps, settenConfigs) {
        var controller = function ($scope, $location) {
            $scope.realm = realm;
            $scope.stage = 1;
            $scope.currentFlow = flow;
            $scope.alreadySet = true;
            $scope.selectedNode = node;
            $scope.selectedExecution = selectedExec;
            $scope.execCheck = {
                isEnabled: selectedExec ? selectedExec.requirement.toLowerCase() === "required" : false
            };
            $scope.configType = configProps;
            $scope.resourceUrl = resourceUrl;
            $scope.changed = false;
            $scope.configChanged = false;
            $scope.oldvalue = {};
            if (settenConfigs.config !== undefined) {
                $scope.oldvalue = angular.copy(settenConfigs.config);
            }
            if(settenConfigs.config !== undefined && settenConfigs.config.passwordPolicy !== null) {
                const isPasswordPolicyPresent = passwordPolicies.filter(x => x.policyName === settenConfigs.config.passwordPolicy);
                if(isPasswordPolicyPresent.length === 0) {
                    settenConfigs.config.passwordPolicy = 'default_password_policy';
                }
            }

            var defaultConfig = {};
            if (configProps && Array.isArray(configProps)) {
                for (var i = 0; i < configProps.length; i++) {
                    var property = configProps[i];
                    if (property && property.name) {
                        defaultConfig[property.name] = property.defaultValue;
                    }
                }
            }
            if (!angular.equals(settenConfigs, {})) {
                $scope.config = { config: settenConfigs.config };
                $scope.config.alias = settenConfigs.alias;
            } else {
                $scope.alreadySet = false;
                $scope.config = { config: defaultConfig };
            }
            $scope.$watch('config', function () {
                var oldValue = selectedExec ? selectedExec.requirement.toLowerCase() === "required" : false;
                if (!angular.equals($scope.oldvalue, $scope.config.config) && configCheckNullAuthentication($scope.config.config, $scope.configType)) {
                    $scope.changed = true;
                    $scope.configChanged = true;
                    if (!$scope.execCheck.isEnabled && oldValue === $scope.execCheck.isEnabled) {
                        $scope.changed = false;
                    }
                } else {
                    $scope.changed = false;
                    $scope.configChanged = false;
                    if ((!$scope.execCheck.isEnabled && oldValue !== $scope.execCheck.isEnabled)) {
                        $scope.changed = true;
                    }
                }
            }, true);

            $scope.$watch('execCheck.isEnabled', function () {
                var oldValue = selectedExec ? selectedExec.requirement.toLowerCase() === "required" : false;
                if ((oldValue !== $scope.execCheck.isEnabled) && (!$scope.execCheck.isEnabled || configCheckNullAuthentication($scope.config.config, $scope.configType))) {
                    $scope.changed = true;
                }
                else if ((oldValue === $scope.execCheck.isEnabled) && (!$scope.configChanged || !configCheckNullAuthentication($scope.config.config, $scope.configType))) {
                    $scope.changed = false;
                } else if (!$scope.execCheck.isEnabled && oldValue === $scope.execCheck.isEnabled) {
                    $scope.changed = false;
                }
            });

            $scope.isUpdated = function () {
                return !angular.equals(settenConfigs, {}) && $scope.alreadySet;
            };

            $scope.save = function () {
                $scope.stage = 2;
                $('#cancelBtn').attr('disabled', 'disabled');
                var configCopy = angular.copy($scope.config);
                ComponentUtils.convertAllListValuesToMultivaluedString($scope.configType, configCopy.config);
                if (configCopy.config.hasOwnProperty('verify-expiry-time') && configCopy.config['verify-expiry-time']) {
                    const isValidExpiryTime = ComponentUtils.checkExpiryTimeValid($scope.configType, configCopy.config);
                    if (!isValidExpiryTime) {
                        $scope.stage = 1;
                        $('#cancelBtn').removeAttr('disabled', 'disabled');
                        Notifications.error($translate.instant("invalid-expiry-time-error"));
                        return;
                    }
                }
                if (!angular.equals(settenConfigs, {}) && $scope.alreadySet) {
                    if (!$scope.execCheck.isEnabled) {
                        configCopy = {};
                    }
                    AuthenticationConfig.update({
                        realm: realm.realm,
                        config: settenConfigs.id
                    }, configCopy, function () {
                        $scope.stage = 1;
                        $('#cancelBtn').removeAttr('disabled', 'disabled');
                        selectedExec.requirement = $scope.execCheck.isEnabled ? "REQUIRED" : "DISABLED";
                        AuthenticationFlowExecutions.update({ realm: realm.realm, alias: $scope.currentFlow.alias }, selectedExec, function () {
                            Notifications.success($translate.instant("changesSavedSuccess"));
                            $route.reload();
                            $scope.$panelInstance.dismiss('this is from the controller!!');
                        });
                    }, function (err) {
                        $scope.stage = 1;
                        $('#cancelBtn').removeAttr('disabled', 'disabled');
                        Notifications.error(err.data.errorMessage);
                    });
                } else {
                    if (!$scope.execCheck.isEnabled) {
                        configCopy = {};
                    }
                    AuthenticationExecutionConfig.save({
                        realm: realm.realm,
                        execution: selectedExec.id
                    }, configCopy, function (data, headers) {
                        selectedExec.requirement = $scope.execCheck.isEnabled ? "REQUIRED" : "DISABLED";
                        AuthenticationFlowExecutions.update({ realm: realm.realm, alias: $scope.currentFlow.alias }, selectedExec, function () {
                            Notifications.success($translate.instant("config-created"));
                            $scope.stage = 1;
                            $('#cancelBtn').removeAttr('disabled', 'disabled');
                            $route.reload();
                            $scope.$panelInstance.dismiss('this is from the controller!!');
                        });
                    }, function (err) {
                        $scope.stage = 1;
                        $('#cancelBtn').removeAttr('disabled', 'disabled');
                        Notifications.error(err.data.errorMessage);
                    });
                }
            };

            $scope.removeExecution = function (execution) {
                $scope.$panelInstance.dismiss('this is from the controller!!');
                Dialog.confirmDelete(execution.displayName, 'execution', function () {
                    $scope.$evalAsync(function () {
                        AuthenticationExecution.remove({ realm: realm.realm, execution: execution.id }, function () {
                            Notifications.success($translate.instant("execution-removed"));
                            $route.reload();
                        }, function (err) {
                            console.log(err.data.errorMessage);
                        });
                    });

                }, function () {
                    $route.reload();
                });
            };

            $scope.dismissPanel = function () {
                $route.reload();
                $scope.$panelInstance.dismiss('this is from the controller!!');
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });

            $scope.reset = function () {
                $scope.config.config = angular.copy($scope.oldvalue);
                $scope.$panelInstance.dismiss('this is from the controller!!');
                $route.reload();
            };
        };

        $http.get(resourceUrl + '/partials/panels/auth-config-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                    //Deselects the Selected Node in the Flow
                    editor.deselectNode();
                    ev.stopPropagation();
                    ev.preventDefault();
                    console.log('panel was closed with result : ', result);
                }).catch(function (error) {
                    if (error && (error === "backdrop click" || error === "escape key press")) {
                        $route.reload();
                    }
                    editor.deselectNode();
                    ev.stopPropagation();
                    ev.preventDefault();
                    console.log('panel was rejected with error : ', error);
                });
        });
    };

    $scope.deleteFlow = function () {
        Dialog.confirmDelete($scope.currentFlow.alias, 'flow', function () {
            $scope.removeFlow();
        });
    };

    $scope.removeFlow = function () {
        if (realm.browserFlow == $scope.currentFlow.alias) {
            Notifications.error($translate.instant("flow-not-removed-used-in-browser-flow-error"));

        } else if (realm.registrationFlow == $scope.currentFlow.alias) {
            Notifications.error($translate.instant("flow-not-removed-used-in-reg-flow-error"));

        } else if (realm.directGrantFlow == $scope.currentFlow.alias) {
            Notifications.error($translate.instant("flow-not-removed-used-in-dir-grant-flow-error"));

        } else if (realm.resetCredentialsFlow == $scope.currentFlow.alias) {
            Notifications.error($translate.instant("flow-not-removed-used-in-cred-flow-error"));

        } else if (realm.clientAuthenticationFlow == $scope.currentFlow.alias) {
            Notifications.error($translate.instant("flow-not-removed-used-in-auth-flow-error"));

        } else if (realm.dockerAuthenticationFlow == $scope.currentFlow.alias) {
            Notifications.error($translate.instant("flow-not-removed-used-in-doc-auth-flow-error"));

        } else {
            let templateType = $scope.currentFlow.templateType.toLowerCase();
            templateType = (templateType === "direct_grant") ? "login" : templateType;
            AuthenticationFlows.remove({ realm: realm.realm, flow: $scope.currentFlow.id }, function () {
                $location.url("/realms/" + realm.realm + '/authentication/installed/' + templateType);
                Notifications.success($translate.instant('remove-flow-success'));
            }, function (err) {
                Notifications.error($translate.instant(err.data.errorMessage));
            });
        }

    };


    queryFlows();

});

module.controller('CreateExecutionFlowCtrl', function ($scope, realm, parentFlow, formProviders,
    CreateExecutionFlow,
    Notifications, $location) {
    $scope.realm = realm;
    $scope.formProviders = formProviders;

    var defaultFlowType = parentFlow.providerId == 'client-flow' ? 'client-flow' : 'basic-flow';
    $scope.flow = {
        alias: "",
        type: defaultFlowType,
        description: ""
    };
    $scope.provider = {};
    if (formProviders.length > 0) {
        $scope.provider = formProviders[0];
    }

    $scope.save = function () {
        $scope.flow.provider = $scope.provider.id;
        CreateExecutionFlow.save({ realm: realm.realm, alias: parentFlow.alias }, $scope.flow, function () {
            $location.url("/realms/" + realm.realm + "/authentication/flows");
            Notifications.success($translate.instant("flow-created"));
        });
    };
    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/authentication/flows");
    };
});

module.controller('CreateExecutionCtrl', function ($scope, realm, parentFlow, formActionProviders, authenticatorProviders, clientAuthenticatorProviders,
    CreateExecution,
    Notifications, $location, $translate) {
    $scope.realm = realm;
    $scope.parentFlow = parentFlow;

    if (parentFlow.providerId == 'form-flow') {
        $scope.providers = formActionProviders;
    } else if (parentFlow.providerId == 'client-flow') {
        $scope.providers = clientAuthenticatorProviders;
    } else {
        $scope.providers = authenticatorProviders;
    }

    $scope.provider = {};
    if ($scope.providers.length > 0) {
        $scope.provider = $scope.providers[0];
    }

    $scope.save = function () {
        var execution = {
            provider: $scope.provider.id
        };
        CreateExecution.save({ realm: realm.realm, alias: parentFlow.alias }, execution, function () {
            $location.url("/realms/" + realm.realm + "/authentication/flows");
            Notifications.success($translate.instant("execution-created"));
        });
    };
    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/authentication/flows");
    };
});

module.controller("MarketplaceDetailsCtrl", function($scope, realm, serverInfo) {
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.templateId =  $scope.path[4];
});

module.controller("KcMarketplaceDetailsCtrl", function($scope, $translate, $location, Notifications, Dialog, BOMarketplaceTemplate, ImportWorkflow, CreateInputModal) {
    $scope.realm = $scope.$parent.realm;
    $scope.serverInfo = $scope.$parent.serverInfo;
    $scope.templateId =   $scope.$parent.templateId;
    $scope.isModalOpened = false;
    $scope.template = {};
    $scope.loading = false;
    $scope.templateCountry = [];
    $scope.availableCountries = [...intlTelInputGlobals.getCountryData()];
    $scope.isPublished = false;
    $scope.isInstalled = false;
    $scope.isModalOpened = false;
    $scope.installingTemplateName = "";
    $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;
    $scope.currentTab = "overview"
    $scope.detailsUrl = `#/realms/${$scope.realm.realm}/marketplace/details/${$scope.templateId}`;
    $scope.flowUrl = `#/realms/${$scope.realm.realm}/marketplace/flow/${$scope.templateId}`;

    $scope.getMarketplaceTemplate = function () {
        BOMarketplaceTemplate.get({realm: $scope.realm.realm, templateId: $scope.templateId }, function (res) {
            $scope.template = res;
            updatedCountry();
            document.getElementById('markdown-content').innerHTML = marked.parse($scope.template.workflowDescription);
        })
    }

    $scope.deleteMarketplaceTemplate = function () {
        if(!$scope.isModalOpened) {
            $scope.isModalOpened = true;
            Dialog.confirmDelete($scope.template.workflowName, 'template', function () {
                $scope.isModalOpened = false;
                BOMarketplaceTemplate.remove({realm: $scope.realm.realm, templateId: $scope.templateId }, function(res) {
                    Notifications.success($translate.instant('marketplace-template-deletion-success'));
                    $location.path(`/realms/${$scope.realm.realm}/marketplace/workflows`);
                }, function (err) {
                    if(err.data.errorMessage) {
                        Notifications.error($translate.instant(err.data.errorMessage));
                    }else {
                        Notifications.error($translate.instant('marketplace-template-deletion-error'));
                    }
                })
            }, function () {
                $scope.isModalOpened = false;
            });
        }
    }

    $scope.publishMarketplaceTemplate = function () {
        $scope.loading = true;
        $scope.isPublished = true;
        $scope.template.published = angular.copy(!$scope.template.published);
        BOMarketplaceTemplate.update({realm: $scope.realm.realm, templateId: $scope.template.id}, $scope.template, function (res) {
            $scope.template = res;
            $scope.loading = false;
            Notifications.success($translate.instant($scope.template.published ? 'marketplace-template-published' : 'marketplace-template-unpublished'));
            $scope.isPublished = false;
        }, function () {
            Notifications.error($translate.instant('marketplace-template-updation-error'));
            $scope.loading = false;
            $scope.isPublished = false;
        })
    }

    $scope.installMarketplace = function () {
        if(!$scope.isModalOpened) {
            $scope.isModalOpened = true;
            CreateInputModal.open($translate.instant('install-template'), '48', function (installTemplateName) {
                $scope.loading = true;
                $scope.isInstalled = true;
                ImportWorkflow.create({realm : $scope.realm.realm, workflowName: installTemplateName }, JSON.parse($scope.template.workflowJson), function (response) {
                    Notifications.success($translate.instant("template-created"));
                    $location.url("/realms/" + $scope.realm.realm + '/authentication/templates/' + response.body);
                    $scope.loading = false;
                    $scope.isInstalled = false;
                }, function (err) {
                    $scope.loading = false;
                    $scope.isInstalled = false;
                    if(err.data.errorMessage) {
                        Notifications.error($translate.instant(err.data.errorMessage));
                    }else {
                        Notifications.error($translate.instant('marketplace-template-installation-error'));
                    }
                });
            }, function () {
                $scope.isModalOpened = false;
            });
        }
    }

    const updatedCountry = () => {
        const countries = [];
        if (!$scope.template.country) {
            $scope.templateCountry = countries;
            return;
        }
        $scope.templateCountry = $scope.template.country.split(",");
        $scope.templateCountry.map((country) => {
            countries.push(...$scope.availableCountries.filter(c => c.iso2 === country))
            $scope.availableCountries = $scope.availableCountries.filter(c => c.iso2 !== country)

        })
        $scope.templateCountry = countries;
    }

    $scope.getMarketplaceTemplate();
});

module.controller("MarketplaceFlowCtrl", function($scope, realm, serverInfo, actionPolicies, cibaPolicies) {
    $scope.realm =  realm;
    $scope.serverInfo = serverInfo;
    $scope.actionPolicies = actionPolicies;
    $scope.cibaPolicies = cibaPolicies;
    $scope.templateId = $scope.path[4];
});

module.controller("KcMarketplaceFlowCtrl", function($scope, $translate, $location, UnregisteredRequiredActions, Notifications, BOMarketplaceTemplate, ActionVariablesDialog, CreateInputModal, ImportWorkflow) {
    $scope.realm = $scope.$parent.realm;
    $scope.serverInfo = $scope.$parent.serverInfo;
    $scope.actionPolicies = $scope.$parent.actionPolicies;
    $scope.cibaPolicies = $scope.$parent.cibaPolicies;
    $scope.templateId = $scope.$parent.templateId;
    $scope.resourceUrl = resourceUrl;
    $scope.unregisteredRequiredActions = [];
    $scope.template = {};
    $scope.loading = false;
    $scope.isPublished = false;
    $scope.actionId = "";
    $scope.actionrep = {};
    $scope.action = {};
    $scope.isTemplateReady = false;
    $scope.showTopBar = false;
    $scope.isInstalled = false;
    $scope.isModalOpened = false;
    $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;
    $scope.currentTab = "flow"
    $scope.flowUrl = `#/realms/${$scope.realm.realm}/marketplace/flow/${$scope.templateId}}`;
    $scope.detailsUrl = `#/realms/${$scope.realm.realm}/marketplace/details/${$scope.templateId}`;

    $scope.getMarketplaceTemplate = function () {
        BOMarketplaceTemplate.get({realm: $scope.realm.realm, templateId: $scope.templateId}, function (res) {
            $scope.template = res;
            $scope.actionId = res.workflowId;
            $scope.actionrep = { "alias" : res.workflowName, "id": res.workflowId, "actionType": "verification", "actionId": res.workflowId, "actionVariables": JSON.parse(res.workflowJson).actionVariables.actionVariables };
            $scope.action = angular.copy($scope.actionrep);
            $scope.fetchActions();
            document.getElementById('markdown-content').innerHTML = marked.parse($scope.template.workflowDescription)
        })
    }

    $scope.fetchActions = function () {
        UnregisteredRequiredActions.query(
            { realm: $scope.realm.realm, action: $scope.actionId },
            function (data) {
                $scope.unregisteredRequiredActions = data;
                $scope.isTemplateReady = true;
            },
            function (data) {
                Notifications.error($translate.instant("ciba-failed-to-fetch-required-action"));
            },
        );
    }

    $scope.publishMarketplaceTemplate = function () {
        $scope.loading = true;
        $scope.isPublished = true;
        $scope.template.published = angular.copy(!$scope.template.published);
        BOMarketplaceTemplate.update({realm: $scope.realm.realm, templateId: $scope.template.id}, $scope.template, function (res) {
            $scope.template = res;
            $scope.loading = false;
            $scope.isPublished = false;
            Notifications.success($translate.instant($scope.template.published ? 'marketplace-template-published' : 'marketplace-template-unpublished'));
        }, function () {
            Notifications.error($translate.instant('marketplace-template-updation-error'));
            $scope.loading = false;
            $scope.isPublished = false;
        })
    }

    $scope.installMarketplace = function () {
        if(!$scope.isModalOpened) {
            $scope.isModalOpened = true;
            CreateInputModal.open($translate.instant('install-template'), '48', function (installTemplateName) {
                $scope.loading = true;
                $scope.isInstalled = true;
                ImportWorkflow.create({realm : $scope.realm.realm, workflowName: installTemplateName }, JSON.parse($scope.template.workflowJson), function (response) {
                    Notifications.success($translate.instant("template-created"));
                    $location.url("/realms/" + $scope.realm.realm + '/authentication/templates/' + response.body);
                    $scope.loading = false;
                    $scope.isInstalled = false;
                }, function (err) {
                    $scope.loading = false;
                    $scope.isInstalled = false;
                    if(err.data.errorMessage) {
                        Notifications.error($translate.instant(err.data.errorMessage));
                    }else {
                        Notifications.error($translate.instant('marketplace-template-installation-error'));
                    }
                });
            }, function () {
                $scope.isModalOpened = false;
            });
        }
    }

    $scope.openActionVariableModal = function () {
        if(!$scope.isModalOpened) {
            $scope.isModalOpened = true;
            ActionVariablesDialog.open($translate.instant("action-variables"), $scope.realm.realm, $scope.actionrep, function (data) {
                $scope.actionrep = data;
            }, function() {
                $scope.isModalOpened = false;
            });
        }
    }

    $scope.getMarketplaceTemplate();
});

module.controller("MarketplaceEditCtrl", function($scope, realm, $document, serverInfo, Notifications, $translate, BOMarketplaceTemplate, GetAllBOMarketplaceCategory, CreateInputModal, CreateBOMarketplaceCategory, $route) {
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.showingCountires = false;
    $scope.isModalOpened = false;
    $scope.availableCountries = [...intlTelInputGlobals.getCountryData()].map((c, i) => ({...c, index:i}));
    $scope.currentTab = "overview"
    $scope.detailsUrl = `#/realms/${$scope.path[1]}/marketplace/edit/${$scope.path[4]}`;
    $scope.flowUrl = `#/realms/${$scope.path[1]}/marketplace/flow/${$scope.path[4]}`;
    $scope.oldCopy = {};
    $scope.template = {};
    $scope.isLoading = true;
    $scope.isChanged = false;
    $scope.searchCountry = {};
    const regex = /^[a-zA-Z0-9-_ ]*$/;

    $scope.cancel = () => {
        $route.reload();
    }

    $scope.$watch("template", function () {
        if (!Array.isArray($scope.template.country)) {
            return;
        }
        const newCopy = angular.copy($scope.template);
        newCopy?.country?.sort((a, b) => a.iso2.localeCompare(b.iso2));
        $scope.oldCopy.country?.sort((a, b) => a.iso2.localeCompare(b.iso2));
        if (JSON.stringify($scope.oldCopy) !== JSON.stringify(newCopy) && newCopy.workflowName?.trim()) {
            $scope.isChanged = true;
        } else {
            $scope.isChanged = false;
        }
    }, true);

    BOMarketplaceTemplate.get({realm: $scope.realm.realm, templateId: $scope.path[4]}, function (res) {
        $scope.template = res;
        $scope.oldCopy = angular.copy($scope.template);
        $scope.templateTags = [];
        updatedCountry();
        if ($scope.template.templateTag.length === 0) {
            $scope.template.templateTag = null;
            $scope.oldCopy.templateTag = []

        }
        $scope.isLoading = false;
    })

    $scope.createMarketplaceCategory = function () {
        if ( !$scope.isModalOpened ) {
            $scope.isModalOpened = true;
            CreateInputModal.open($translate.instant('create-category'), '48', function (categoryName) {
                if (!regex.test(categoryName)) {
                    Notifications.error($translate.instant('category-name-chars-not-allowed'));
                    return;
                }
                CreateBOMarketplaceCategory.create({realm: $scope.realm.realm}, {name:categoryName}, function (res) {
                    $scope.categories = res.categories;
                    $scope.template.workflowCategory = res.selectedCategory.name;
                    Notifications.success($translate.instant('marketplace-category-creation-success'));
                }, function (err) {
                    if(err.data.errorMessage) {
                        Notifications.error($translate.instant(err.data.errorMessage));
                    }else {
                        Notifications.error($translate.instant('marketplace-category-creation-error'));
                    }
                } )
            }, function () {
                $scope.isModalOpened = false;
            });
        }
    }

    const updatedCountry = () => {
        const countries = [];
        if (!$scope.template.country) {
            $scope.template.country = countries;
            $scope.oldCopy = angular.copy($scope.template);
            return;
        }
        $scope.template.country = $scope.template.country.split(",");
        $scope.template.country.map((country) => {
            countries.push(...$scope.availableCountries.filter(c => c.iso2 === country))
            $scope.availableCountries = $scope.availableCountries.filter(c => c.iso2 !== country)

        })
        $scope.template.country = countries;
        $scope.oldCopy = angular.copy($scope.template);
    }

    $scope.showTags = () => {
        $scope.showingTags = !$scope.showingTags;
    };
    $scope.updateTemplate = () => {
        if (!regex.test($scope.template.workflowName)) {
            Notifications.error($translate.instant('template-name-chars-not-allowed'));
            return;
        }
        if (!$scope.template.workflowName) {
            Notifications.error($translate.instant('template-name-required'));
            return;
        }
        $scope.loading = true;
        const body = angular.copy($scope.template);
        body.country = body.country.map(c => c.iso2).join();
        BOMarketplaceTemplate.update({realm: $scope.realm.realm, templateId: $scope.path[4]}, body, function (data) {
            $scope.loading = false;
            $scope.isChanged = false;
            Notifications.success($translate.instant('marketplace-template-updated'));
         },
         function (e){
            $scope.loading = false;
            $scope.isChanged = false;
            Notifications.error($translate.instant(e.data.errorMessage));
         })
    }

    GetAllBOMarketplaceCategory.query({realm: $scope.realm.realm, first: -1, max: -1}, function (res) {
        $scope.categories = res;
    })

    $scope.showCountries = (event) => {
        $scope.searchCountry.name = "";
        $scope.showingCountries = !$scope.showingCountries;
        event.stopPropagation();
    };

    $scope.addCountry = (index) => {
        var selectedCountryIndex = $scope.availableCountries.findIndex((country) => country.index === index);
        $scope.template.country.push($scope.availableCountries[selectedCountryIndex]);
        $scope.availableCountries.splice(selectedCountryIndex, 1);
    }

    $scope.removeCountry = (index) => {
        $scope.availableCountries.push($scope.template.country[index]);
        $scope.template.country.splice(index, 1);
        $scope.availableCountries.sort((a, b) => a.name.localeCompare(b.name));
    }


    $( document ).ready(function() {
        $("#outside-content").click((event)=> {
            if(event.target.tagName === "INPUT") {
                return;
            }
            $scope.showingCountries = false;
            $scope.$apply();
        })
    });

    // Logo uploading

    $(document).ready(function () {
        $("#updateLogo").change(function () {
            processLogo(this);
        });
    });


    const processLogo = function (uploader) {
        const supportedImgType = ["image/png", "image/jpeg", "image/jpg", "image/svg+xml", "image/webp"]
        if (uploader.files && uploader.files[0]) {
            if (!supportedImgType.includes(uploader.files[0].type)) {
                Notifications.error($translate.instant("invalid-file-type"));
                $scope.$apply()
                return;
            }
            if (uploader.files[0].size > 3000000) {
                Notifications.error($translate.instant("terms-and-condition-image-process-error-desc"));
                $scope.$apply()
                return;
            }
            $('#templateLogo').attr('src', window.URL.createObjectURL(uploader.files[0]));
            $scope.logoUploadTxt = $translate.instant('change-tenant-logo');
            const reader = new FileReader();
            reader.onload = function(e) {
                const binaryString = e.target.result;
                const base64String = btoa(binaryString);
                $scope.template.imageURL = base64String;
                $scope.$apply();
            };
            reader.onerror = function(error) {
                console.error('Error reading file:', error);
            };
            reader.readAsBinaryString(uploader.files[0]);
        }
    }

    $scope.uploadImage = function () {
        $('#updateLogo').trigger('click');
    };
})

module.controller('MarketplaceWorkflowCategoryListCtrl', function ($scope, $translate, realm, serverInfo, Session, Notifications, Dialog, CreateInputModal, CreateBOMarketplaceCategory, BOMarketplaceCategory, GetAllBOMarketplaceCategory) {
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.searchLoaded = false;
    $scope.categories = [];
    $scope.categoriesCopy = [];
    $scope.searchKey = "";
    $scope.isModalOpened = false;
    $scope.loaderTypeName = 'authCardLoader';
    const regex = /^[a-zA-Z0-9-_ ]*$/;

    $scope.init = function () {
        $scope.first = 0;
        $scope.limit = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
        $scope.last = $scope.first + $scope.limit;
        $scope.perPage = 5;
        $scope.query = {
            realm: realm.realm,
        };
        updateTotal();
        $scope.searchQuery();
    };

    const updateTotal = () => {
        GetAllBOMarketplaceCategory.query({...$scope.query, first:-1, last: -1, max: -1}, function (res) {
            $scope.total = res.length;
        })
    }

    $scope.searchQuery = function () {
        $scope.searchLoaded = false;
        $scope.query = {...$scope.query, first: $scope.first, last: $scope.limit, max :$scope.limit}
        GetAllBOMarketplaceCategory.query($scope.query, function (res) {
            $scope.categories = res;
            $scope.categoriesCopy = res;
            $scope.searchLoaded = true;
        })
    };

    $scope.selectQuery = function (limit) {
        $scope.limit = limit;
        Session.setItem("pageFilterCount", $scope.limit);
        $scope.first = 0;
        $scope.last = $scope.limit;
        $scope.searchQuery()
    };

    $scope.firstPage = function () {
        $scope.first = 0;
        $scope.last = $scope.first + $scope.limit;
        $scope.searchQuery();
    };

    $scope.next = function () {
        $scope.first += $scope.limit;
        $scope.last += $scope.limit;
        $scope.searchQuery();
    };

    $scope.previous = function () {
        $scope.first -= $scope.limit;
        $scope.last -= $scope.limit;
        $scope.searchQuery();
    };

    $scope.changeSearchKey = function () {
        $scope.searchKey = angular.copy($("#templates-search-input").val().trim());
        $scope.query.search = $scope.searchKey;
        $scope.first = 0
        $scope.last = $scope.limit;
        updateTotal();
        $scope.searchQuery();
    }

    $scope.clearSearch = function () {
        $scope.searchKey = "";
        $scope.searchValue = "";
        $scope.init();
    }

    $scope.createCategory = function () {
        if(!$scope.isModalOpened) {
            $scope.isModalOpened = true;
            CreateInputModal.open($translate.instant('create-category'), '48', function (categoryName) {
                if (!regex.test(categoryName)) {
                    Notifications.error($translate.instant('category-name-chars-not-allowed'));
                    return;
                }
                CreateBOMarketplaceCategory.create({realm: $scope.realm.realm}, {name:categoryName}, function (res) {
                    $scope.init()
                    Notifications.success($translate.instant('marketplace-category-creation-success'));
                }, function (err) {
                    if(err.data.errorMessage) {
                        Notifications.error($translate.instant(err.data.errorMessage));
                        $scope.init()
                    }else {
                        Notifications.error($translate.instant('marketplace-category-creation-error'));
                    }
                } )
            }, function () {
                $scope.isModalOpened = false;
            });

        }
    }

    $scope.deleteCategory = function (category) {
        if(!$scope.isModalOpened) {
            $scope.isModalOpened = true;
            Dialog.confirmDelete(category.category, 'category', function () {
                $scope.isModalOpened = false;
                BOMarketplaceCategory.delete({realm: $scope.realm.realm, categoryName: category.category}, function (res) {
                    Notifications.success($translate.instant('marketplace-category-deletion-success'));
                    $scope.init()
                }, function(err) {
                    if(err.data.errorMessage) {
                        Notifications.error($translate.instant(err.data.errorMessage));
                    }else {
                        Notifications.error($translate.instant('marketplace-category-deletion-error'));
                    }
                })
            }, function () {
                $scope.isModalOpened = false;
            });
        }
    }

    $scope.init();
});

module.controller('MarketplaceWorkflowsCtrl', function ($scope, realm, serverInfo, GetAllBOMarketplaceTemplate) {
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.isSmallScreen = false;
    $scope.selectedTab = "published";
    $scope.loaderTypeName = "boMasterMarketplaceCardLoader";
    $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;
    $scope.templates = [];
    $scope.searchLoaded = false;
    $scope.searchInputValue = "";
    $scope.category = "";
    $scope.order = "";
    $scope.categories = [];
    $scope.isRTL = () => locale == 'ar';

    // $scope.getMarketplaceTemplates = function () {
    //     GetAllBOMarketplaceTemplate.query({realm: $scope.realm.realm, searchValue: $scope.searchInputValue, category: $scope.category, order: $scope.order, isPublished: $scope.selectedTab == "published" ? true : false }, function(res) {
    //         $scope.templates = res;
    //         $scope.searchLoaded = true;
    //     }, function (err) {
    //         if(err.errorMessage) {
    //             Notifications.error($translate.instant(err.errorMessage));
    //         }else {
    //             Notifications.error($translate.instant('marketplace-template-fetch-error'));
    //         }
    //     })
    // }

    // $scope.setSelectedTab = function (tab) {
    //     $scope.selectedTab = tab;
    //     $scope.getMarketplaceTemplates();
    // }


    // $scope.clearSearch = function () {
    //     $scope.searchInputValue = "";
    //     $scope.getMarketplaceTemplates();
    // }

    // $scope.getMarketplaceTemplates();
});

module.controller('kcMarketplaceWorkflowsListCtrl', function ($scope, $translate, GetAllBOMarketplaceTemplate, GetAllBOMarketplaceCategory, Notifications) {
    $scope.realm = $scope.$parent.realm;
    $scope.serverInfo = $scope.$parent.serverInfo;
    $scope.isSmallScreen = $scope.$parent.isSmallScreen;
    $scope.templateId = $scope.$parent.templateId ;
    $scope.selectedTab = "published";
    $scope.loaderTypeName = "boSubtenantMarketplaceCardLoader";
    $scope.isFilterOptionVisible = false;
    $scope.filterOrder = 'asc';
    $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;
    if(!$scope.isRootAccount){
        $scope.selectedCategory = 'recommended';
    }else {
        $scope.selectedCategory = '';
    }
    $scope.searchInputValue = '';
    $scope.templates = [];
    $scope.templateRecommended = [];
    $scope.searchLoaded = false;
    $scope.isSearchResultAvailable = false;
    $scope.isRTL = () => locale == 'ar';
    var isFilterOrderChanged = false;
    var isCategoryChanged = false;
    var selectedCategoryCopy = angular.copy($scope.selectedCategory);

    $scope.toggleFilterVisibility = function () {
        $scope.isFilterOptionVisible = !angular.copy($scope.isFilterOptionVisible);
    }

    $scope.setFilterOrder = function (option) {
        $scope.filterOrder = option;
        isFilterOrderChanged = true;
        selectedCategoryCopy = angular.copy($scope.selectedCategory);
        if($scope.selectedCategory == 'recommended'){
            $scope.getMarketplaceRecommendedTemplates();
        }else {
            $scope.getMarketplaceTemplates();
        }
    }

    $scope.setSelectedTab = function (tab) {
        $scope.selectedTab = tab;
        $scope.getMarketplaceTemplates();
    }

    $scope.setCategory = function (category) {
        $scope.selectedCategory = category;
        if($scope.selectedCategory == 'recommended' && selectedCategoryCopy != 'recommended'){
            $scope.getMarketplaceRecommendedTemplates();
        }else if($scope.selectedCategory != 'recommended' && selectedCategoryCopy == 'recommended') {
            $scope.getMarketplaceTemplates();
        }
        selectedCategoryCopy = angular.copy($scope.selectedCategory);
        isCategoryChanged = true;
    }

    $scope.getTemplatesByValue = function(){
        if(!$scope.isRootAccount){
            $scope.getMarketplaceRecommendedTemplates();
        }
        $scope.getMarketplaceTemplates();
    }

    $scope.clearSearchInputValue = function(){
        $scope.searchInputValue = "";
        $scope.getTemplatesByValue();
    }

    $scope.getMarketplaceTemplates = function () {
        $scope.searchLoaded = false;
        GetAllBOMarketplaceTemplate.query({realm: $scope.realm.realm, searchValue: $scope.searchInputValue, category: '', order: $scope.filterOrder, isPublished: $scope.selectedTab == "published" ? true : false }, function(res) {
            $scope.templates = res;
            if(!isFilterOrderChanged){
                $scope.categories = [];
                $scope.templates.map((template) => {
                    if(!$scope.categories.includes(template.workflowCategory)){
                        $scope.categories.push(template.workflowCategory)
                    }
                })
            }
            if(selectedCategoryCopy == 'recommended'){
                $scope.selectedCategory = 'recommended';
            }else if((isFilterOrderChanged || isCategoryChanged) && $scope.categories.includes(selectedCategoryCopy)){
                $scope.selectedCategory = selectedCategoryCopy;
            }else if($scope.categories.length > 0){
                $scope.selectedCategory = $scope.categories[0];
            }else{
                $scope.selectedCategory = '';
            }
            isFilterOrderChanged = false;
            isCategoryChanged = false;
            $scope.searchLoaded = true;
        }, function (err) {
            if(err.errorMessage) {
                Notifications.error($translate.instant(err.errorMessage));
            }else {
                Notifications.error($translate.instant('marketplace-template-fetch-error'));
            }
        })
    }

    $scope.getMarketplaceRecommendedTemplates = function () {
        $scope.searchLoaded = false;
        GetAllBOMarketplaceTemplate.query({realm: $scope.realm.realm, searchValue: $scope.searchInputValue, category: 'recommended', order: $scope.filterOrder, isPublished: $scope.selectedTab == "published" ? true : false }, function(res) {
            $scope.templateRecommended = res;
            $scope.selectedCategory = 'recommended'
            $scope.searchLoaded = true;
            if ($scope.searchInputValue.trim().length > 0) {
                $scope.isSearchResultAvailable = true;
            } else {
                $scope.isSearchResultAvailable = false;
            }
        }, function (err) {
            if(err.errorMessage) {
                Notifications.error($translate.instant(err.errorMessage));
            }else {
                Notifications.error($translate.instant('marketplace-template-fetch-error'));
            }
        })
    }

    if(!$scope.isRootAccount){
        $scope.getMarketplaceRecommendedTemplates();
    }
    $scope.getMarketplaceTemplates();
});

module.controller('MarketplaceWorkflowCreateCtrl', function ($scope, realm, $document, serverInfo, $translate, $location, Notifications, actionPolicies, cibaPolicies, CreateInputModal, CreateBOMarketplaceTemplate, BOMarketplaceTemplate, CreateBOMarketplaceCategory, GetAllBOMarketplaceCategory, UnregisteredRequiredActions) {
    $scope.realm = realm;
    $scope.serverInfo = serverInfo;
    $scope.isValidTemplateDetail = false;
    $scope.isValidWorkflow = true;
    $scope.isModalOpened = false;
    $scope.loading = false;
    $scope.actionPolicies = actionPolicies;
    $scope.cibaPolicies = cibaPolicies;
    $scope.actionrep = {};
    $scope.action = {};
    $scope.unregisteredRequiredActions = [];
    $scope.actionId = '';
    $scope.showTopBar = false;
    $scope.availableCountries =  $scope.availableCountries = [...intlTelInputGlobals.getCountryData()].map((c, i) => ({...c, index:i}));
    $scope.pages = [
        { index: 1, name: $translate.instant("marketplace-category-create-title"), description: $translate.instant("marketplace-category-create-description"), completed: false },
        { index: 2, name: $translate.instant("marketplace-workflow-create-title"), description: $translate.instant("marketplace-workflow-create-description"), completed: false }
    ];
    $scope.currentPage = $scope.pages[0];
    $scope.logoUploadTxt = $translate.instant('upload-logo');
    $scope.template = {};
    $scope.template["marketplace"] = true;
    $scope.template.country = [];
    $scope.templateTags = [];
    $scope.templateCategories = [];
    $scope.showingCountries = false;
    $scope.searchCountry = {};
    const regex = /^[a-zA-Z0-9-_ ]*$/;


    $scope.showCountries = (event) => {
        $scope.searchCountry.name = "";
        $scope.showingCountries = !$scope.showingCountries;
        event.stopPropagation();
    };

    $scope.addCountry = (index) => {
        var selectedCountryIndex = $scope.availableCountries.findIndex((country) => country.index === index);
        $scope.template.country.push($scope.availableCountries[selectedCountryIndex]);
        $scope.availableCountries.splice(selectedCountryIndex, 1);
    }

    $scope.removeCountry = (index) => {
        $scope.availableCountries.push( $scope.template.country[index]);
        $scope.template.country.splice(index, 1);
        $scope.availableCountries.sort((a, b) => a.name.localeCompare(b.name));
    }

    $( document ).ready(function() {
        $("#outside-content").click((event)=> {
            if(event.target.tagName === "INPUT") {
                return;
            }
            $scope.showingCountries = false;
            $scope.$apply();
        })
    });

    $scope.restrictSpecialChars = (e) => {
        console.log("target", e);
    }

    // Pagination circular progress

    $scope.$watch("currentPage", function () {
        $scope.setProgress(($scope.currentPage.index*100)/$scope.pages.length);
    });

    $scope.$watch("template", function () {
        if ($scope.template.workflowName && $scope.template.workflowName.length && $scope.template.workflowCategory && $scope.template.workflowCategory.length) {
            $scope.isValidTemplateDetail = true;
        } else {
            $scope.isValidTemplateDetail = false;
        }
    }, true);

    $scope.setProgress = function (percent) {
        const circumference = 2 * Math.PI * 40;
        const dashOffset = circumference - (percent / 100) * circumference;
        const progressBar = document.querySelector('.progress-bar');
        const progressLabel = document.querySelector('.progress-label');
        progressBar.style.strokeDasharray = `${circumference}, ${circumference}`;
        progressBar.style.strokeDashoffset = dashOffset;
        progressLabel.textContent = `${$scope.currentPage.index} ${$translate.instant('of')} ${$scope.pages.length}`;
    }

    $scope.setCurrentPage = function (page) {
        $scope.currentPage = page;
    }

    $scope.createMarketplaceCategory = function () {
        if ( !$scope.isModalOpened ) {
            $scope.isModalOpened = true;
            CreateInputModal.open($translate.instant('create-category'), '48', function (categoryName) {
                if (!regex.test(categoryName)) {
                    Notifications.error($translate.instant('category-name-chars-not-allowed'));
                    return;
                }
                CreateBOMarketplaceCategory.create({realm: $scope.realm.realm}, {name:categoryName}, function (res) {
                    $scope.template.workflowCategory = res.selectedCategory.name;
                    $scope.templateCategories = res.categories;
                    Notifications.success($translate.instant('marketplace-category-creation-success'));
                }, function (err) {
                    if(err.data.errorMessage) {
                        Notifications.error($translate.instant(err.data.errorMessage));
                    }else {
                        Notifications.error($translate.instant('marketplace-category-creation-error'));
                    }
                } )
            }, function () {
                $scope.isModalOpened = false;
            });
        }
    }

    $scope.fetchActions = function () {
        UnregisteredRequiredActions.query(
            { realm: $scope.realm.realm, action: $scope.actionId },
            function (data) {
              $scope.unregisteredRequiredActions = data;
              $scope.setCurrentPage($scope.pages[1]);
            },
            function (data) {
              Notifications.error($translate.instant("ciba-failed-to-fetch-required-action"));
            },
        );
    }

    $scope.getAllMarketplaceCategory = function () {
        GetAllBOMarketplaceCategory.query({realm: $scope.realm.realm, first: -1, max: -1}, function (res) {
            $scope.templateCategories = res;
        })
    }

    // Logo uploading

    $(document).ready(function () {
        $("#updateLogo").change(function () {
            processLogo(this);
        });
    });


    const processLogo = function (uploader) {
        if (uploader.files && uploader.files[0]) {
            const supportedImgType = ["image/png", "image/jpeg", "image/jpg", "image/svg+xml", "image/webp"]
            if (!supportedImgType.includes(uploader.files[0].type)) {
                Notifications.error($translate.instant("invalid-file-type"));
                $scope.$apply()
                return;
            }
            if (uploader.files[0].size > 3000000) {
                Notifications.error($translate.instant("terms-and-condition-image-process-error-desc"));
                $scope.$apply()
                return;
            }

            $scope.$apply(function(){
                $('#templateLogo').attr('src', window.URL.createObjectURL(uploader.files[0]));
                $scope.logoUploadTxt = $translate.instant('change-tenant-logo');
            })
            const reader = new FileReader();
            reader.onload = function(e) {
                const binaryString = e.target.result;
                const base64String = btoa(binaryString);
                $scope.template.imageURL = base64String;
            };
            reader.onerror = function(error) {
                console.error('Error reading file:', error);
            };
            reader.readAsBinaryString(uploader.files[0]);
        }
    }

    $scope.uploadImage = function () {
        $('#updateLogo').trigger('click');
    };

    $scope.createMarketplaceTemplate = function (isPublish) {
        if (!regex.test($scope.template.workflowName)) {
            Notifications.error($translate.instant('template-name-chars-not-allowed'));
            return;
        }
        $scope.loading = true;
        $scope.isValidTemplateDetail = false;
        if($scope.template.hasOwnProperty("id")) {
            if (isPublish) {
                $scope.template.published = true;
            }
            const body = angular.copy($scope.template)
            body.country = $scope.template.country ? $scope.template.country.map(c => c.iso2).join() : '';
            BOMarketplaceTemplate.update({realm: $scope.realm.realm, templateId: $scope.template.id}, body, function (res) {
                if (isPublish) {
                    $location.path(`/realms/${$scope.realm.realm}/marketplace/workflows`);
                }
                res.country = $scope.template.country;
                res.templateTag = $scope.template.templateTag;
                $scope.template = res;
                $scope.actionId = res.workflowId;
                $scope.actionrep = { "alias" : res.workflowName, "id": res.workflowId, "actionType": "verification" };
                $scope.action = angular.copy($scope.actionrep);
                $scope.loading = false;
                $scope.fetchActions();
                $scope.isValidTemplateDetail = true;
            }, function () {
                Notifications.error($translate.instant('marketplace-template-updation-error'));
                $scope.loading = false;
                $scope.isValidTemplateDetail = true;
            })
        } else {
            const body = angular.copy($scope.template)
            body.country = $scope.template.country ? $scope.template.country.map(c => c.iso2).join() : '';
            CreateBOMarketplaceTemplate.create({realm: $scope.realm.realm}, body, function (res) {
                res.country = $scope.template.country;
                res.templateTag = $scope.template.templateTag;
                $scope.template = res;
                $scope.actionId = res.workflowId;
                $scope.actionrep = { "alias" : res.workflowName, "id": res.workflowId, "actionType": "verification" };
                $scope.action = angular.copy($scope.actionrep);
                $scope.loading = false;
                $scope.fetchActions();
                $scope.isValidTemplateDetail = true;
            }, function () {
                Notifications.error($translate.instant('marketplace-template-creation-error'));
                $scope.loading = false;
                $scope.isValidTemplateDetail = true;
            })
        }
    }

    $scope.getAllMarketplaceCategory();
});

module.controller('MarketplaceTemplatesCtrl', function ($scope, $route, realm, MarketplaceTemplateSearch, MarketplaceTemplates, Session, serverInfo, categories) {
    let ctr = 0;
    $scope.init = function () {
        if (ctr === 0) {
            ctr = 1;
            sideMenu.classList.add("custom-menu-true");
            $scope.serverInfo = serverInfo;
            $scope.isIwsFeatureEnabled = $scope.serverInfo.iwsenabled;
            if (!$scope.isIwsFeatureEnabled) {
                $scope.categories = categories.filter((category) => category !== 'IWS');
            } else {
                $scope.categories = categories;
            }
            $scope.category = $route.current.params.category;
            $scope.categoryDescName = $scope.category.toLowerCase().split(" ").join("-");
            $scope.realm = realm;
            $scope.loaderTypeName = 'marketTemplateLoader';
            $scope.recomLoaderTypeName = 'recomTemplateLoader';
            $scope.searchLoaded = false;
            $scope.searchKey = "";
            $scope.first = 0;
            $scope.last = $scope.limit = Session.getItem("templateFilterCount") ? Session.getItem("templateFilterCount") : 6;
            $scope.query = {
                realm: realm.realm,
                category: $route.current.params.category || "recommended"
            };
            $scope.searchQuery();
            $scope.getRecommendedTemplates();
        }
    };

    $scope.variant = variant ;

    const sideMenuSlider = document.querySelector("#body-container");
    sideMenuSlider.classList.add("custom-menu-true");
    $scope.searchQuery = function () {
        $scope.searchLoaded = false;
        $scope.templates = MarketplaceTemplates.query($scope.query, function () {
            $scope.searchLoaded = true;
            // for (const template of $scope.templates) {
            //     template.logo = getValidDomainForMinio(location.origin) + "/" + template.logo;
            // }
            $scope.updateSliceArray();
        });
    };
    $scope.getRecommendedTemplates = function () {
        $scope.recommendedQuery = {
            realm: realm.realm,
            category: "recommended"
        };
        MarketplaceTemplates.query($scope.recommendedQuery, function (marketplaceTemplates) {
            $scope.recommendedTemplate = marketplaceTemplates;
            $scope.recommendedLength = marketplaceTemplates.length;
            $scope.sliderMaxIndexAvailable = marketplaceTemplates.length - 3;
            // for (const marketplaceTemplate of marketplaceTemplates) {
            //     marketplaceTemplate.logo = getValidDomainForMinio(location.origin) + "/" + marketplaceTemplate.logo;
            // }
        });
    };

    $scope.searchFunction = function () {
        MarketplaceTemplateSearch.query({ realm: realm.realm, pattern: $scope.pattern }, function (templateSearchResult) {
            $scope.slicedTemplate = templateSearchResult;
            // for (const template of $scope.slicedTemplate) {
            //     template.logo = getValidDomainForMinio(location.origin) + "/" + template.logo;
            // }
            $scope.searchLoaded = true;
        }, function () {
            $scope.searchLoaded = true;
        });
    };

    $scope.remainingKeywords = function (templateParam) {
        $scope.count = 0;
        if (templateParam.keywords != undefined || templateParam.keywords != null)
            $scope.count = templateParam.keywords.length - 2;
        $scope.remainingValue = '';
        if ($scope.count > 2) {
            $scope.remainingValue = '+' + $scope.count;
        }
        return $scope.remainingValue;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
        $('#search-template').val("");
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchLoaded = false;
        $scope.pattern = val;
        $scope.first = 0;
        if (val) {
            $scope.limit = $scope.templates.length;
            $scope.searchFunction();
        } else {
            $scope.limit = $scope.last;
            $scope.updateSliceArray();
            $scope.searchLoaded = true;
        }
    };

    $scope.filterTemplate = function (key) {
        $scope.slicedTemplate = $scope.templates.filter(template => {
            return template.title.toLowerCase().includes(key.toLowerCase());
        });
    };
    $scope.selectQuery = function (limit) {
        $scope.last = $scope.limit = limit;
        $scope.first = 0;
        Session.setItem("templateFilterCount", $scope.limit);
        $scope.updateSliceArray();
    };
    $scope.firstPage = function () {
        $scope.first = 0;
        $scope.last = $scope.first + $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.nextPage = function () {
        $scope.first += $scope.limit;
        $scope.last += $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.previousPage = function () {
        $scope.first -= $scope.limit;
        $scope.last -= $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.updateSliceArray = function () {
        $scope.slicedTemplate = $scope.templates;
    };
});

module.controller('MarketplaceTemplateCtrl', function ($scope, $route, realm, template, MarketplaceTemplate, MarketplaceTrialTemplate, MarketplaceInstallTemplate, ShowCurlSnippetDialog, $location, Notifications, ServerInfo, $translate, Dialog, serverInfo) {
    $scope.realm = realm;
    $scope.template = template;
    $scope.variant = variant;
    $scope.stage = 1;
    $scope.takeTrialBtnStage = 1;
    $scope.enableInstallBtn = false;
    $scope.initialCurrentTab = "tab_overview";
    $scope.template.logo = template.logo;
    $scope.currentTab = template.onboarding ? "tab_onboarding" : template.login ? "tab_login" : template.recovery ? "tab_recovery" : template.redirect ? "tab_redirect" : "tab_onboarding";
    $scope.installDoc = template.installationMdUrl;
    $scope.overviewDoc = template.overviewMdUrl;
    $scope.howItWorksDoc = template.howItWorksMdUrl || "";
    $scope.uninstallInfo = $translate.instant('uninstall-mfa-conn-desc', { name: template.author });
    sideMenu.classList.remove("custom-menu-true");

    $scope.removeToMarketplace = function () {
        Dialog.confirmUnInstall(template.title, $scope.providerId, function () {
            MarketplaceTemplate.remove({
                realm: realm.realm,
                category: template.category,
                templateid: template.id
            }, function (res) {
                Notifications.success($translate.instant('partnerisUninstalled'));
                location.reload(true);
            }, function (err) {
                err && err.data && err.data.errorMessage ? Notifications.error($translate.instant(err.data.errorMessage)) : (err.data.error ? Notifications.error($translate.instant(err.data.error)) : Notifications.error(''));
            });
        });
    };
    if (!$scope.template.constraints?.success) {
        if (!$scope.template.constraints?.data[0]?.redirectUrl) {
            Notifications.error($translate.instant($scope.template.constraints.data[0].message));
        }
    }
    var timeOutId = setTimeout(function () {
        $("#overview").click();
        $('.mdPreview.MdPreview-html').addClass("hide-scroll-bar");
        $('.mdPreview.MdPreview-html pre').addClass("hide-scroll-bar");
        timeOutId ? clearTimeout(timeOutId) : '';
    }, 300);
    $scope.isImplemented = ($scope.template.isImplemented || $scope.template.isImplemented === false) ? $scope.template.isImplemented : 'true';
    $scope.switchTab = function (tabName) {
        $scope.currentTab = tabName;
    };
    $scope.initialTab = function (tabName) {
        $scope.initialCurrentTab = tabName;
        if (tabName === "tab_flow") {
            $scope.currentTab = template.onboarding ? "tab_onboarding" : template.login ? "tab_login" : template.recovery ? "tab_recovery" : template.redirect ? "tab_redirect" : "tab_onboarding";
        }
    };

    $scope.install = function () {
        $scope.enableInstallBtn = true;
        if (template.installType == "Multiple") {
            $scope.enableInstallBtn = false;
            MarketplaceInstallTemplate.open($translate.instant('install-template'), template.config, realm, function (rep) {
                $scope.stage = 2;
                $scope.enableInstallBtn = true;
                var templateRep = {
                    config: rep
                };
                MarketplaceTemplate.save({ realm: realm.realm, category: template.category, templateid: template.id }, templateRep, function (res) {
                    if (res.success) {
                        if (res.redirectLink) {
                            $location.url(res.redirectLink);
                            Notifications.success($translate.instant(template.title) + " " + $translate.instant("template-installed-successfully"));
                        }
                        else {
                            Notifications.success($translate.instant(template.title) + " " + $translate.instant("template-installed-successfully"));
                            ServerInfo.reload(realm.realm);
                            $route.reload();
                        }
                    }
                    else {
                        Notifications.error(res.message);
                        if (res.redirectLink) {
                            $location.url(res.redirectLink);
                        }
                        else {
                            $scope.install();
                            $scope.enableInstallBtn = false;
                        }
                    }
                    $scope.stage = 1;
                },
                    function (err) {
                        if (err.status == "400") {
                            Notifications.error($translate.instant("alias-name-already-exists-error"));
                        }
                        else {
                            Notifications.error(
                                err?.data?.message
                                    ? err.data.message
                                    : err?.data?.errorMessage
                                        ? err.data.errorMessage
                                        : ""
                            );
                        }
                        $scope.enableInstallBtn = false;
                        $scope.stage = 1;
                    });
            });
        }
        else {
            $scope.enableInstallBtn = false;
            if (template?.config?.length > 0) {
                MarketplaceInstallTemplate.open($translate.instant('install-template'), template.config, realm, function (rep) {
                    saveMarketplace(rep);
                });
            }
            else {
                saveMarketplace({});
            }
        }

        function saveMarketplace(rep) {
            $scope.stage = 2;
            if (rep["supported_apps"] && rep["supported_apps"].length > 0) {
                rep["supported_apps"] = rep["supported_apps"].join(',');
            }
            var templateRep = {
                config: rep
            };
            MarketplaceTemplate.save({ realm: realm.realm, category: template.category, templateid: template.id }, templateRep, function (res) {
                if (res.success) {
                    if (res.redirectLink) {
                        Notifications.success($translate.instant(template.title) + " " + $translate.instant("template-installed-successfully"));
                        ServerInfo.reload(realm.realm, () => {
                            $location.url(res.redirectLink);
                            $scope.stage = 1;
                        });
                    }
                    else {
                        Notifications.success($translate.instant(template.title) + " " + $translate.instant("template-installed-successfully"));
                        ServerInfo.reload(realm.realm, () => {
                            $route.reload();
                            $scope.stage = 1;
                        });
                    }
                }
                else {
                    Notifications.error(res.message);
                    if (res.redirectLink) {
                        $location.url(res.redirectLink);
                    }
                    else {
                        $scope.install();
                        $scope.enableInstallBtn = false;
                    }
                    $scope.stage = 1;
                }
            },
                function (err) {
                    if (err.status == "400") {
                        Notifications.error($translate.instant(err.data.errorMessage));
                    }
                    else {
                        Notifications.error($translate.instant(err.data.errorMessage));
                    }
                    $scope.stage = 1;
                    $scope.enableInstallBtn = false;
                });
        }
    };

    $scope.takeTrial = function (isUnderTrial) {

        if (isUnderTrial) {
            $scope.install();
        }
        else {
            $scope.enableTrialBtn = true;
            doTrial();

            function doTrial() {
                $scope.takeTrialBtnStage = 2;
                MarketplaceTrialTemplate.save({ realm: realm.realm, category: template.category, templateid: template.id }, {}, function (res) {
                    if (res.success) {
                        Notifications.success($translate.instant(res.message));
                        ServerInfo.reload(realm.realm, () => {
                            $route.reload();
                            $scope.takeTrialBtnStage = 1;
                        });
                    }
                    else {
                        Notifications.error(res.message);
                        if (res.redirectLink) {
                            $location.url(res.redirectLink);
                        }
                        else {
                            $scope.enableTrialBtn = false;
                        }
                        $scope.takeTrialBtnStage = 1;
                    }
                },
                    function (err) {
                        $scope.takeTrialBtnStage = 1;
                        $scope.enableTrialBtn = false;
                        Notifications.error(err.data.errorMessage ? err.data.errorMessage : err.data.message);
                    });
            }
        }
    };

    $scope.openConstraintDetail = function (constraints) {
        ShowCurlSnippetDialog.showConstraints(constraints, function () { });
    };

});

module.controller('AuthenticationInstalledOnboardingCtrl', function ($scope, $translate, $route, realm, AuthenticationInstalledFlows, Dialog, AuthenticationFlows, $location, SaveCustomAuthenticationFlows,
    Notifications, CreateCustomTemplate, $location, $filter) {

    $scope.init = function () {
        $scope.loaderTypeName = 'authCardLoader';
        $scope.searchLoaded = false;
        $scope.searchKey = "";
        $scope.onBoardingCount = 0;
        $scope.realm = realm;
        $scope.query = {
            realm: realm.realm,
        };
        $scope.searchQuery();
        if (realm.mode == 'bo') {
            $location.url("/notfound");
        }
    };


    $scope.marketPlaceLinkMsg = $translate.instant('marketplace-template-link-in-flows', { mpLink: "#/realms/" + realm.realm + "/marketplace/templates/authentication", variant: variant});
    $scope.searchQuery = function () {
        $scope.searchLoaded = false;
        $scope.flows = AuthenticationInstalledFlows.query($scope.query, function () {
            $scope.updateCount();
            if ($scope.flows) {
                $scope.loginFlows = Object.values(Object.fromEntries(Object.entries($scope.flows).filter(([key, value]) => value.templateType === "onboarding" || value.templateType === "ONBOARDING")));
            }
            $scope.searchLoaded = true;
        });
    };
    $scope.updateCount = function () {
        for (const flow of $scope.flows) {
            switch (flow.templateType.toLowerCase()) {
                case "onboarding": $scope.onBoardingCount++;
                    break;
            }
        }
    };

    $scope.openFlow = function (flowId, flowType) {
        let url = `/realms/${$scope.realm.realm}/authentication/flows/${flowId}/${flowType}/flow`;
        $location.url(url);
    };

    $scope.removeFlow = function (currentFlow) {
        if ($scope.realm.browserFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "browser flow" }));
        } else if ($scope.realm.registrationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "registration flow" }));

        } else if ($scope.realm.directGrantFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "direct grant flow" }));

        } else if ($scope.realm.resetCredentialsFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "reset credentials flow" }));
        } else if ($scope.realm.clientAuthenticationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "client authentication flow" }));
        } else if ($scope.realm.dockerAuthenticationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "docker authentication flow" }));
        } else {
            AuthenticationFlows.remove({ realm: $scope.realm.realm, flow: currentFlow.id }, function () {
                $route.reload();
                Notifications.success($translate.instant('remove-flow-success'));
            }, function (err) {
                Notifications.error($translate.instant(err.data.errorMessage));
            });
        }

    };

    $scope.deleteFlow = function (event, currentFlow) {
        event.stopPropagation();
        Dialog.confirmDelete(currentFlow.alias, 'flow', function () {
            $scope.removeFlow(currentFlow);
        });
    };

    $scope.onChooseFlow = function () {
        var flowChosen = "onboarding";
        CreateCustomTemplate.open($translate.instant('create-onboarding-flow'), flowChosen, '', function (name, flowType) {
            $scope.choosenFlow = flowChosen;
            if (flowType != null && flowType === 'direct_grant') {
                flowChosen = 'direct_grant';
            }
            var templateRep = {
                alias: name,
                providerId: "basic-flow",
                description: $translate.instant('auth-desc-custom') + $translate.instant(flowChosen) + ' ' + $translate.instant('auth-flow-desc') + ' ' + name,
                topLevel: true,
                builtIn: false,
                templateAlias: $translate.instant('login-template-alias', { flowChosen, name }),
                installed: true,
                templateType: flowChosen,
            };
            SaveCustomAuthenticationFlows.save({ realm: realm.realm, templatetype: flowChosen }, templateRep, function (res) {
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + res.id + '/' + $filter('removeUnderScore')(flowChosen) + '/flow');
                Notifications.success($translate.instant('create-flow-success', { flowName: name }));
            }, function (err) {
                err && err.data && err.data.errorMessage ? Notifications.error($translate.instant(err.data.errorMessage)) : (err.data.error ? Notifications.error($translate.instant(err.data.error)) : Notifications.error($translate.instant('could-not-create') + $translate.instant($scope.choosenFlow)));
            });
        });

    };


    $scope.findConfigured = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const configured of $scope.loginFlows) {
                if (configured.alias.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('AuthenticationInstalledLoginCtrl', function ($scope, $translate, $route, realm, AuthenticationInstalledFlows, Dialog, AuthenticationFlows, $location, SaveCustomAuthenticationFlows,
    Notifications, CreateCustomTemplate, $location, $filter) {

    if (realm.mode == 'bo') {
        $location.url("/notfound");
    }
    $scope.onChooseFlow = function () {
        var flowChosen = "login";
        CreateCustomTemplate.open($translate.instant('create-login-flow'), flowChosen, '', function (name, flowType) {
            $scope.choosenFlow = flowChosen;
            if (flowType != null && flowType === 'direct_grant') {
                flowChosen = 'direct_grant';
            }
            var templateRep = {
                alias: name,
                providerId: "basic-flow",
                description: $translate.instant('auth-desc-custom') + $translate.instant(flowChosen) + ' ' + $translate.instant('auth-flow-desc') + ' ' + name,
                topLevel: true,
                builtIn: false,
                templateAlias: $translate.instant('login-template-alias', { flowChosen, name }),
                installed: true,
                templateType: flowChosen,
            };
            SaveCustomAuthenticationFlows.save({ realm: realm.realm, templatetype: flowChosen }, templateRep, function (res) {
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + res.id + '/' + $filter('removeUnderScore')(flowChosen) + '/flow');
                Notifications.success($translate.instant('create-flow-success', { flowName: name }));
            }, function (err) {
                err && err.data && err.data.errorMessage ? Notifications.error($translate.instant(err.data.errorMessage)) : (err.data.error ? Notifications.error($translate.instant(err.data.error)) : Notifications.error($translate.instant('could-not-create') + $translate.instant($scope.choosenFlow)));
            });
        });

    };

    $scope.init = function () {
        $scope.loaderTypeName = 'authCardLoader';
        $scope.onBoardingCount = 0;
        $scope.searchLoaded = false;
        $scope.searchKey = "";
        $scope.loginCount = 0;
        $scope.realm = realm;
        $scope.query = {
            realm: realm.realm,
        };
        $scope.searchQuery();
    };

    $scope.marketPlaceLinkMsg = $translate.instant('marketplace-template-link-in-flows', { mpLink: "#/realms/" + realm.realm + "/marketplace/templates/authentication", variant: variant});

    $scope.searchQuery = function () {
        $scope.searchLoaded = false;
        $scope.flows = AuthenticationInstalledFlows.query($scope.query, function () {
            $scope.updateCount();
            if ($scope.flows) {
                $scope.loginFlows = Object.values(Object.fromEntries(Object.entries($scope.flows).filter(([key, value]) => value.templateType === "login" || value.templateType === "LOGIN" || value.templateType === "direct_grant" || value.templateType === "DIRECTGRANT")));
            }
            $scope.searchLoaded = true;
        });
    };

    $scope.updateCount = function () {
        for (const flow of $scope.flows) {
            switch (flow.templateType.toLowerCase()) {
                case "login": $scope.loginCount++;
                    break;
                case "direct_grant": $scope.loginCount++;
                    break;
            }
        }
    };

    $scope.openFlow = function (flowId, flowType) {
        let url = `/realms/${$scope.realm.realm}/authentication/flows/${flowId}/${flowType}/flow`;
        $location.url(url);
    };

    $scope.removeFlow = function (currentFlow) {
        if ($scope.realm.browserFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "browser flow" }));
        } else if ($scope.realm.registrationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "registration flow" }));

        } else if ($scope.realm.directGrantFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "direct grant flow" }));

        } else if ($scope.realm.resetCredentialsFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "reset credentials flow" }));
        } else if ($scope.realm.clientAuthenticationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "client authentication flow" }));
        } else if ($scope.realm.dockerAuthenticationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "docker authentication flow" }));
        } else {
            AuthenticationFlows.remove({ realm: $scope.realm.realm, flow: currentFlow.id }, function () {
                $route.reload();
                Notifications.success($translate.instant('remove-flow-success'));
            }, function (err) {
                Notifications.error($translate.instant(err.data.errorMessage));
            });
        }

    };

    $scope.deleteFlow = function (event, currentFlow) {
        event.stopPropagation();
        Dialog.confirmDelete(currentFlow.alias, 'flow', function () {
            $scope.removeFlow(currentFlow);
        });
    };

    $scope.findConfigured = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const configured of $scope.loginFlows) {
                if (configured.alias.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };


});

module.controller('AuthenticationInstalledRecoveryCtrl', function ($scope, $translate, $route, realm, AuthenticationInstalledFlows, Dialog, AuthenticationFlows, $location, SaveCustomAuthenticationFlows,
    Notifications, CreateCustomTemplate, $location, $filter) {
    if (realm.mode == "bo") {
        $location.path('/notfound');
        return;
    }

    $scope.onChooseFlow = function () {
        var flowChosen = "recovery";
        CreateCustomTemplate.open($translate.instant('create-recovery-flow'), flowChosen, '', function (name, flowType) {
            $scope.choosenFlow = flowChosen;
            if (flowType != null && flowType === 'direct_grant') {
                flowChosen = 'direct_grant';
            }
            var templateRep = {
                alias: name,
                providerId: "basic-flow",
                description: $translate.instant('auth-desc-custom') + $translate.instant(flowChosen) + ' ' + $translate.instant('auth-flow-desc') + ' ' + name,
                topLevel: true,
                builtIn: false,
                templateAlias: $translate.instant('login-template-alias', { flowChosen, name }),
                installed: true,
                templateType: flowChosen,
            };
            SaveCustomAuthenticationFlows.save({ realm: realm.realm, templatetype: flowChosen }, templateRep, function (res) {
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + res.id + '/' + $filter('removeUnderScore')(flowChosen) + '/flow');
                Notifications.success($translate.instant('create-flow-success', { flowName: name }));
            }, function (err) {
                err && err.data && err.data.errorMessage ? Notifications.error($translate.instant(err.data.errorMessage)) : (err.data.error ? Notifications.error($translate.instant(err.data.error)) : Notifications.error($translate.instant('could-not-create') + $translate.instant($scope.choosenFlow)));
            });
        });

    };

    $scope.init = function () {
        $scope.loaderTypeName = 'authCardLoader';
        $scope.searchLoaded = false;
        $scope.searchKey = "";
        $scope.recoveryCount = 0;
        $scope.realm = realm;
        $scope.query = {
            realm: realm.realm,
        };
        $scope.searchQuery();
    };
    $scope.marketPlaceLinkMsg = $translate.instant('marketplace-template-link-in-flows', { mpLink: "#/realms/" + realm.realm + "/marketplace/templates/authentication", variant: variant});

    $scope.searchQuery = function () {
        $scope.searchLoaded = false;
        $scope.flows = AuthenticationInstalledFlows.query($scope.query, function () {
            $scope.updateCount();
            if ($scope.flows) {
                $scope.loginFlows = Object.values(Object.fromEntries(Object.entries($scope.flows).filter(([key, value]) => value.templateType === "recovery" || value.templateType === "RECOVERY")));
            }
            $scope.searchLoaded = true;
        });
    };

    $scope.updateCount = function () {
        for (const flow of $scope.flows) {
            switch (flow.templateType.toLowerCase()) {
                case "recovery": $scope.recoveryCount++;
                    break;
            }
        }
    };

    $scope.openFlow = function (flowId, flowType) {
        let url = `/realms/${$scope.realm.realm}/authentication/flows/${flowId}/${flowType}/flow`;
        $location.url(url);
    };

    $scope.removeFlow = function (currentFlow) {
        if ($scope.realm.browserFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "browser flow" }));
        } else if ($scope.realm.registrationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "registration flow" }));

        } else if ($scope.realm.directGrantFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "direct grant flow" }));

        } else if ($scope.realm.resetCredentialsFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "reset credentials flow" }));
        } else if ($scope.realm.clientAuthenticationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "client authentication flow" }));
        } else if ($scope.realm.dockerAuthenticationFlow === currentFlow.alias) {
            Notifications.error($translate.instant('remove-flow-error', { flowName: "docker authentication flow" }));
        } else {
            AuthenticationFlows.remove({ realm: $scope.realm.realm, flow: currentFlow.id }, function () {
                $route.reload();
                Notifications.success($translate.instant('remove-flow-success'));
            }, function (err) {
                Notifications.error(err.data.errorMessage);
            });
        }

    };

    $scope.deleteFlow = function (event, currentFlow) {
        event.stopPropagation();
        Dialog.confirmDelete(currentFlow.alias, 'flow', function () {
            $scope.removeFlow(currentFlow);
        });
    };

    $scope.findConfigured = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const configured of $scope.loginFlows) {
                if (configured.alias.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('CreateTemplatesCtrl', function ($scope, realm, actionrep, unregisteredRequiredActions, $route, actionPolicies, cibaPolicies) {
    $scope.actionPolicies = actionPolicies;
    $scope.cibaPolicies = cibaPolicies;
    $scope.realm = realm;
    $scope.actionrep = actionrep;
    $scope.actionId = "";
    $scope.unregisteredRequiredActions = unregisteredRequiredActions;
    $scope.actionId = $route.current.params.action;
    $scope.showTopBar = true;
    $scope.template = actionrep;

    $(document).ready(function () {
        const sideMenuSlider = document.querySelector("#body-container");
        sideMenuSlider.classList.add("custom-menu-true");
    });

});

module.controller('KcCreateTemplatesCtrl', function ($scope, $http,
    $modal, $route, Actions, Dialog, InputDialog, $location, ComponentUtils, WorkflowPreviewDialog, WorkflowVisualizeGif, ActionVariablesDialog, GetWorkflowEstimatedCost, AddTemplateToWorkflow,
    RegisterRequiredAction, RegisterRequiredActions, UpdateRequiredActions, RequiredActions,GetValidationConfigs, UnregisteredRequiredActions, RequiredActionsQuery, TermsConditionsVersions, angularSlideOutPanel, ActionsPriority, Notifications, $translate, $q, Policies, ProcessInput) {
    const realm = $scope.$parent.realm;
    $scope.realm = realm;
    $scope.actionPolicies = $scope.$parent.actionPolicies;
    $scope.cibaPolicies = $scope.$parent.cibaPolicies;
    $scope.actionrep = $scope.$parent.actionrep;
    $scope.unregisteredRequiredActions = $scope.$parent.unregisteredRequiredActions;
    $scope.actionId = $scope.$parent.actionId;
    $scope.selectedAction = undefined;
    $scope.oldValue = [];
    $scope.isMarketplaceSaveEnabled = false;
    $scope.requiredActions = [];
    $scope.setRequiredActions = (data) => {
        $scope.requiredActions = data;
    }
    $scope.payloadActions = [];
    $scope.showTopBar = $scope.$parent.showTopBar;
    $scope.isSaving = false;
    $scope.isRTL = locale == 'ar'
    $scope.dragOverParent = "none";
    $scope.validatorStepId = "none"
    $scope.dragOverDirection = "none";
    $scope.isFlowLoading = false
    $scope.validatorCliams = [];
    $scope.autoSave = $scope.$parent.template && $scope.$parent.template.marketplace;
    $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;
    if ($scope.autoSave && !$scope.isRootAccount) {
        $scope.isReadOnly = true;
    } else {
        $scope.isReadOnly = false;
    }
    var unregisteredRequiredActionsCopy = angular.copy($scope.unregisteredRequiredActions);
    $scope.currencySymbol;
    var isNewMarketPlace = true;

    $scope.$watch('requiredActions', function (){
        if (!angular.equals($scope.requiredActions, $scope.oldValue)) {
            $scope.isMarketplaceSaveEnabled = true;
        } else {
            $scope.isMarketplaceSaveEnabled = false;
        }
    },true)


    setTree();

    $scope.findActions = function (actionsList, searchValue) {
        if (searchValue && searchValue.trim()) {
            for (var itr = 0; itr < actionsList.length; itr++) {
                if (actionsList[itr].name.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };

    const updateEstimation = () => {
        GetWorkflowEstimatedCost.get({realmId: realm.id, actionId:$scope.actionId }, function(data){
            $scope.costEstimation = data;
            $scope.currencySymbol = getCurrencySymbol('en', $scope.costEstimation.walletCurrency);
            if(Array.isArray($scope.costEstimation.estimatedCost)) {
                $scope.costEstimation.estimatedCost = $scope.costEstimation.estimatedCost.join(` - ${$scope.currencySymbol} `);
            }
            Object.keys($scope.costEstimation.costForEachVerificationModule).map(key => {
                if(Array.isArray($scope.costEstimation.costForEachVerificationModule[key])) {
                    $scope.costEstimation.costForEachVerificationModule[key] = $scope.costEstimation.costForEachVerificationModule[key].join(` - ${$scope.currencySymbol} `);
                }
            })
        }, function(err){
            console.log(err);
        })
    }

    $scope.searchFilter = '';
    $scope.$watch('search', function () {
        $scope.searchFilter = $scope.search ? $scope.search.trim() : "";
    }, true);


    // Custom filter for the template.
    $scope.templateFilter = function (action) {
        // OSS: hide private-connector workflow templates (deferred connector split)
        var hiddenTemplates = ['bo-video-kyc'];
        if (hiddenTemplates.indexOf(action.name) !== -1) {
            return false;
        }
        return $scope.searchFilter
               ? $translate.instant(action.name).toLowerCase().includes($scope.searchFilter.toLowerCase())
               : true;
    }

    const getValidatorId = (list, selected, parentId) => {
        let index;
        if (list.length <= 0 || !selected) {
            return "none";
        }

        const node = list.find((element, i) => {
            index = i;
            return element.id === parentId;
        })

        if (node && node.validator) {
            return node.id;
        } else {
            const newList = [...list]
            newList.splice(index,1);
            return getValidatorId(newList, node, node?.parentId);
        }
    }

    const isDragaRestricted = (d) => {
        let isRestricted = d.isDefault || d.isPlaceHolder || d.providerId === "bo-custom-message" || $scope.isReadOnly;
        if (d.validator && d.direction === "none") {
            isRestricted = true;
        } else if(!d.vaidator && $scope.requiredActions.find(action => action.id === d.parentId)?.validator && $scope.requiredActions.find(action => action.parentId === d.nodeId)?.validator) {
            isRestricted = true;
        }
        return isRestricted
    }

    const getValidatorChilds = (startId) => {
        let resultList = [];
        let toProcess = [startId];
        let processed = new Set();

        while (toProcess.length > 0) {
            let currentId = toProcess.pop();

            if (processed.has(currentId)) continue;
            processed.add(currentId);

            $scope.requiredActions.forEach(action => {
                if (action?.validatorStepId === currentId) {
                    resultList.push(action.id);
                    if (action.validator) {
                        toProcess.push(action.id);
                    }
                }
            });
        }

        return resultList;
    }


    const checkConnectionIsAvailable = () => {
        let isAvailable = true;
        if (!selectedNode && !draggingNode) {
            isAvailable = false;
        } else if (selectedNode && (selectedNode.validator || selectedNode.providerId === "bo-custom-message")) {
            isAvailable = false;
        } else if (selectedNode && draggingNode && draggingNode.validator && getValidatorChilds(draggingNode.nodeId).includes(selectedNode.nodeId)) {
            isAvailable = false;
        } else if (selectedNode && draggingNode && draggingNode.validator && $scope.requiredActions.find( action => action.parentId === selectedNode.nodeId)) {
            isAvailable = false;
        }
        return isAvailable;
    }


    // ************** Generate the tree diagram	 *****************
    var margin = { top: 60, right: 120, bottom: 20, left: 1000 },
        width = $('#drawflow').width(),
        height = 250 - margin.top - margin.bottom;

    var circle = { r: 35 };
    var rect = { h: 60, w: 200 };
    var circleTextPosition = 85;
    var depth = 120;
    var dragNodeId;

    var i = 0;

    var dragStarted = false;
    var nodes, links, domNode, panTimer, selectedNode = null, draggingNode, relCoords;
    var panSpeed = 300;
    var panBoundary = 30;

    var tree, diagonal, svg, root, orginalRoot = [];

    // define the zoomListener which calls the zoom function on the "zoom" event constrained within the scaleExtents
    var zoomListener;
    var treeTransformValue = "translate(" + width/2 + "," + height/2 + ")";
    $scope.workflowIndex = {};

    var init = function () {

        d3.selectAll("svg:not(#application-creation-progress):not(#api-icon)").remove();

        tree = d3.layout.tree()
            .nodeSize([200, 60])
            .separation(function(a, b) { return a.parentId === b.parentId ? 1 : 1.2; })
            .children(function(d) { return d.children; })
            .size([height, width]);

        var lines = d3.svg.diagonal()
            .projection(function (d) {d.depth = 10; return [d.x, d.y]; });


        diagonal =  d => lines(d).replace('C', 'L');

        zoomListener = d3.behavior.zoom().scaleExtent([0.1, 1]).on("zoom", zoom);

        svg = d3.select("#drawflow").append("svg")
            .attr("width", "100%")
            .attr("height", "100%")
            .call(zoomListener)
            .append("g")
            .attr("transform", treeTransformValue)
            .attr("width", "100%")

        zoomListener.translate(getTranslateValues(treeTransformValue));
        zoomListener.scale(getScaleValue(treeTransformValue));

        root = $scope.treeData[0];
        orginalRoot = $scope.treeData[0];
        root.x0 = height / 2;
        root.y0 = 0;

        render();

        d3.selectAll(".link").each(animate);

        d3.select("#zoomIn").on("click", () => zoomBy(1.5));
        d3.select("#zoomOut").on("click", () => zoomBy(0.5));
        d3.select("#centerLocator").on("click", () => alignToCenter());

        $scope.isSaving = false;
    };

    function getTranslateValues(transformString) {
        const translateRegex = /translate\(([^)]+)\)/;
        const translateMatch = transformString.match(translateRegex);

        if (translateMatch) {
          return translateMatch[1].split(',').map(Number);
        }

        return [margin.left, margin.top];
    }

    function getScaleValue(transformString) {
        const scaleRegex = /scale\(([^)]+)\)/;
        const scaleMatch = transformString.match(scaleRegex);

        if (scaleMatch) {
          return parseFloat(scaleMatch[1]);
        }

        return 1;
      }

    function alignToCenter(){
        zoomListener.scale(0.8);
        zoomListener.translate([width/2, height/2]);
        treeTransformValue = "translate(" + width/2 + "," + height/2 + ")scale(" + 0.8 + ")";
        svg.transition().duration(500).attr("transform", treeTransformValue);
    }


    function zoomBy(factor) {
        var scale = zoomListener.scale() * factor;
        var translate = zoomListener.translate();
        zoomListener.scale(scale);
        zoomListener.translate(translate);
        if(scale <= 1){
            treeTransformValue = "translate(" + translate + ")scale(" + scale + ")";
        }else {
            treeTransformValue = "translate(" + translate + ")scale(1)"
        }
        svg.transition().duration(500).attr("transform", treeTransformValue);
    }

    var zoom = function () {
        if(d3.event.scale <= 1){
            treeTransformValue = "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")";
        }else{
            treeTransformValue = "translate(" + d3.event.translate + ")scale(1)";
        }
        svg.attr("transform", treeTransformValue);
    }

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

    const emptyValidatorConfig = (validator, callback) => {
        validator = $scope.requiredActions.find( action => (action.id === validator?.nodeId || action.id === validator?.id));
        if (validator.config['validation-policy-id']) {
            Policies.remove({
                realm: $scope.realm.realm,
                policy: validator.config['validation-policy-id']
            }, function () {
                validator.config = {};
                RequiredActions.update({
                    realm: realm.realm,
                    alias: validator.id
                }, validator, function () {
                    callback();
                });
            });
        } else {
            callback();
        }
    }

    var reiterateHierarchy = function () {
        if (selectedNode != null && draggingNode != null) {

            var tempNodes = d3.layout.tree().nodes(orginalRoot);
            const nodeList = [];
            let draggingNodeChildCount , draggingNodeChilds = [];

            tempNodes.forEach((element) => { // Getting node from the tree.
                var item = {
                    "name": element.name,
                    "nodeId": element.nodeId,
                    "priority": element.priority,
                    "validatorStepId": element.validatorStepId,
                    "direction": element.direction,
                    "validator": element.validator,
                    "parentId": element.parentId,
                    "isDefault": element.isDefault,
                    "isPlaceHolder": element.isPlaceHolder,
                    "providerId": element.providerId
                };
                nodeList.push({...item});
            });

            if (draggingNode?.validator) {
                draggingNodeChilds =  getValidatorChilds(draggingNode.nodeId)
                draggingNodeChildCount = draggingNodeChilds.length;
            }

            const priorityPayload = [];
            const actionId = "actionId";
            const priority = "priority";
            const validatorStepId = "validatorStepId";
            const direction = "direction";
            const parentId = "parentId";
            const template = {actionId , priority, validatorStepId, direction, parentId}; // Requesty payload template for the prority request.
            const draggingNodeLocal = draggingNode;
            const selectedNodeLocal = selectedNode;

            const getParentAndChild = ( node ) => {
                let parent = null;
                let child = null;
                nodeList.map( n => {
                    if (n.parentId === node.nodeId) {
                        child = n;
                    } else if (node.parentId === n.nodeId) {
                        parent = n;
                    }
                })
                return [parent, child];
            }

            const [draggingParent, draggingChild] = getParentAndChild(draggingNode);
            const [selectedParent, selectedChild] = getParentAndChild(selectedNode);

            const mapParent = () => {
                nodeList.map( node => {
                    if (node.nodeId === draggingChild?.nodeId) {
                        node.parentId =  draggingParent.nodeId;
                    } else if (node.nodeId === draggingNode.nodeId) {
                        node.parentId = selectedNode.nodeId;
                    } else if (selectedChild?.nodeId === node.nodeId) {
                        node.parentId = draggingNode.nodeId;
                    }
                    return node;
                })
            }

            if (draggingNode.validatorStepId === "none" && !selectedNode.validatorStepId === "none" // If the node is not under the validator.
                || (draggingNode?.validatorStepId === selectedNode?.validatorStepId // If the child on the same direction and same validator
                    &&  draggingNode.direction === selectedNode.direction)) {

                mapParent();

                nodeList.map( node => {
                    if (node.isDefault || node.isPlaceHolder) return;

                    template.actionId = node.nodeId;
                    template.validatorStepId = node.validatorStepId;
                    template.direction = node.direction;
                    template.parentId = node.parentId;
                    var selectNodePriority = selectedNode?.priority >= 0 ? selectedNode.priority : -1;
                    if (node.nodeId === draggingNode.nodeId) {
                        template.priority =  selectNodePriority + 1;
                    } else if ( node.priority > selectNodePriority ) {
                        template.priority = node.priority + 1;
                    } else {
                        template.priority = node.priority;
                    }
                    priorityPayload.push({...template});
                });

            } else if (draggingNode.validator) {  // if the dragging is validator.

                nodeList.map( node => {
                    if (node.isDefault || node.isPlaceHolder) return;

                    template.actionId = node.nodeId;
                    template.priority = node.priority;
                    template.validatorStepId = node.validatorStepId;
                    template.direction = node.direction;
                    template.parentId = node.parentId;

                    if (node.nodeId === draggingNode.nodeId) {
                        template.priority = selectedNode.priority + 1;
                        template.validatorStepId = selectedNode.validatorStepId;
                        template.direction = selectedNode.direction;
                        template.parentId = selectedNode.nodeId;
                    } else if (draggingNodeChildCount > 0 && draggingNodeChilds.includes(node.nodeId)) {
                        template.priority = selectedNode.priority + 1 + template.priority;
                    }

                    priorityPayload.push({...template});
                });

                priorityPayload.sort((a, b) => a.priority - b.priority)
            } else if (draggingNode.validatorStepId || selectedNode.validatorStepId ) {  // if the dragging node and selected node in the different direction or under the different validator.

                nodeList.map( node => {
                    if (node.isDefault || node.isPlaceHolder) return;

                    template.actionId = node.nodeId;
                    template.priority = node.priority;
                    template.validatorStepId = node.validatorStepId;
                    template.direction = node.direction;
                    template.parentId = node.parentId;

                    if (node.nodeId === draggingNode.nodeId) {
                        template.priority = selectedNode.priority + 1;
                        template.validatorStepId = selectedNode.validatorStepId;
                        template.direction = selectedNode.direction;
                        template.parentId = selectedNode.nodeId;
                    } else if (draggingChild && draggingChild.nodeId === node.nodeId) {
                        template.parentId = draggingParent.nodeId;
                    } else if ((selectedChild && selectedChild.nodeId === node.nodeId)) {
                        template.parentId = draggingNode.nodeId;
                        template.priority = template.priority + 1;
                    } else if (node.priority > selectedNode) {
                        template.priority = template.priority + 1;
                    }

                    priorityPayload.push({...template});
                });

                priorityPayload.sort((a, b) => a.priority - b.priority)
            }

            $scope.$evalAsync(function () {
                const callback = () => {
                    InputDialog.confirmWorkflowChange(selectedNodeLocal, draggingNodeLocal, function () {
                        if($scope.autoSave) {
                            priorityPayload.forEach((element) => {
                                $scope.requiredActions = $scope.requiredActions.map((action) => {
                                    let tempRequiredAction = action;
                                    if(tempRequiredAction.id === element.actionId) {
                                        tempRequiredAction.priority = element.priority;
                                        tempRequiredAction.direction = element.direction;
                                        tempRequiredAction.validatorStepId = element.validatorStepId;
                                        tempRequiredAction.parentId = element.parentId;
                                    }
                                    return tempRequiredAction;
                                })
                            })
                            $scope.requiredActions.sort((a, b) => a.priority - b.priority);
                            RequiredActionsQueryResultAction($scope.requiredActions);
                            Notifications.success($translate.instant("priority-of-execution-updated"));
                        } else {
                            ActionsPriority.save({ realm: realm.realm }, priorityPayload, function () {
                                const childValidator = $scope.requiredActions.find( action => action.validatorStepId === draggingNodeLocal.validatorStepId && action.validator && action.direction === draggingNodeLocal.direction)
                                Notifications.success($translate.instant("priority-of-execution-updated"));
                                queryFlows();
                            });
                        }
                    })
                }
                // const childValidator = $scope.requiredActions.find( action => action.validatorStepId === draggingNodeLocal.validatorStepId && action.validator && action.direction === draggingNodeLocal.direction)
                // if (draggingNodeLocal.validator) {
                //     emptyValidatorConfig(draggingNodeLocal, callback);
                // } else if (!draggingNodeLocal.validator && childValidator) {
                //     emptyValidatorConfig(childValidator, callback);
                // } else {
                //     callback()
                // }
                callback();
            });
        }
    };

    var list_to_tree = function (list) {
        var map = {}, node, roots = [], i;
        // initialize the children
        for (i = 0; i < list.length; i += 1) {
            map[list[i].nodeId] = i;
            if (list[i].validator) {
                list[i].children = [undefined, undefined];
            } else {
                list[i].children = [];
            }
        }


        const getLastNode = (direction, parentId) => {
            const tempNode = {
                "name": $translate.instant('drag-and-drop-templates-for-'),
                "parent": parentId,
                "isPlaceHolder": true,
                "direction":"left",
                "nodeId":"dragId",
                "isDefault": false,
            }
            const newNodeId = tempNode.name + createUUID();
            tempNode.nodeId =  newNodeId;
            tempNode.name = `${tempNode.name}${direction}`;
            tempNode.direction = direction
            return {...tempNode};
        }

        let validatorId = null;
        let lastParent = null;


        const getParentDetails = (id) => {
            return list.find( n => n.nodeId == id)
        };


        for (i = 0; i < list.length; i += 1) {
            node = list[i];
            if (node.parent !== "null") {
                const parentDetails = getParentDetails(node.parentId)
                // if you have dangling branches check that map[node.parentId] exists
                if (node.validator) {
                    validatorId = node.nodeId;
                    $scope.validatorStepId = validatorId;
                }
                if (parentDetails?.validator && node.direction === 'left') {
                    list[map[node.parentId]].children[0] = node;
                    lastLeftNode = node.nodeId;
                } else if ((parentDetails?.validator && node.direction === 'right')) {
                    list[map[node.parentId]].children[1] = node;
                    lastRightNode = node.nodeId;
                } else if (node.direction === 'left') {
                    list[map[node.parentId]].children.push(node);
                    lastLeftNode = node.nodeId;
                } else if (node.direction === 'right'){
                    list[map[node.parentId]].children.push(node);
                    lastRightNode = node.nodeId;
                } else {
                    if (validatorId && (node.name === $translate.instant('drag-and-drop-templates')))  {
                        continue;
                    }
                    // Resolve the parent index; tolerate a dangling parentId (e.g. an imported
                    // flow whose root keeps the source realm's id) by falling back to lastParent.
                    var parentIdx = (node.parentId && node.parentId !== "none")
                        ? map[node.parentId]
                        : map[lastParent?.nodeId];
                    if (parentIdx === undefined || !list[parentIdx]) {
                        parentIdx = map[lastParent?.nodeId];
                    }
                    if (parentIdx !== undefined && list[parentIdx]) {
                        list[parentIdx].children.push(node);
                    }
                }
            }
            lastParent = node;
        }

        list = list.map((item) => {
            if (item.children.length <= 0 && !item.validtor) {
                if (item.direction === "left") {
                    item.children[0] = getLastNode("left", item.nodeId);
                } else if (item.direction === "right") {
                    item.children[0] = getLastNode("right", item.nodeId);
                }
            } else if (item.validator) {
                if (!item.children[0]) {
                    item.children[0] = getLastNode("left", item.nodeId);
                }
                if (!item.children[1]) {
                    item.children[1] = getLastNode("right", item.nodeId);
                }
            }
            return item;
        })


        roots.push(list[i]);
        return roots;
    };

    var overNode = function (d) {
        selectedNode = d;
        if (checkConnectionIsAvailable())
            updateTempConnector();
    };

    var outNode = function (d) {
        selectedNode = null;
        if (checkConnectionIsAvailable())
            updateTempConnector();
    };

    var updateTempConnector = function () {
        var data = [];
        if (draggingNode && selectedNode) {
            // have to flip the source coordinates since we did this for the existing connectors on the original tree
            data = [{
                source: {
                    x: (draggingNode.x0 + (100 / 2)),
                    y: (draggingNode.y0)
                },
                target: {
                    x: selectedNode.x0,
                    y: selectedNode.y0
                }
            }];
        }

        var link = svg.selectAll(".templink").data(data);

        link.enter().append("path")
            .attr("class", "templink")
            .attr("d", d3.svg.diagonal())
            .attr('pointer-events', 'none');

        link.attr("d", d3.svg.diagonal());

        link.exit().remove();
    };

    var pan = function (domNode, direction) {
        var speed = panSpeed;
        if (panTimer) {
            clearTimeout(panTimer);
            var translateCoords = d3.transform(svg.attr("transform"));
            if (direction == 'left' || direction == 'right') {
                translateX = translateCoords.translate[0];
                translateY = direction == 'left' ? translateCoords.translate[1] + speed : translateCoords.translate[1] - speed;
            } else if (direction == 'up' || direction == 'down') {
                translateX = direction == 'up' ? translateCoords.translate[1] - speed : translateCoords.translate[1] + speed;
                translateY = translateCoords.translate[0];
            }
            d3.select(domNode).select('g.node').attr("transform", "translate(" + translateX + "," + translateY + ")");
            zoomListener.scale(zoomListener.scale());
            zoomListener.translate([translateX, translateY]);
            panTimer = setTimeout(function () {
                pan(domNode, speed, direction);
            }, 50);
        }
    };

    var initiateDrag = function (d, domNode) {
        draggingNode = d;
        d3.select(domNode).select('.ghostRect').attr('pointer-events', 'none');
        d3.selectAll('.ghostRect').attr('class', 'ghostRect show');

        d3.select(domNode).select('.ghostCircle').attr('pointer-events', 'none');
        d3.selectAll('.ghostCircle').attr('class', 'ghostCircle show');
        d3.select(domNode).attr('class', 'node activeDrag');

        svg.selectAll("g.node").sort(function (a, b) { // select the parent and sort the path's
            if (a.id != draggingNode.id) return 1; // a is not the hovered element, send "a" to the back
            else return -1; // a is the hovered element, bring "a" to the front
        });

        dragStarted = null;
    };

    var endDrag = function () {
        selectedNode = null;
        d3.selectAll('.ghostCircle').attr('class', 'ghostCircle');
        d3.selectAll('.ghostRect').attr('class', 'ghostRect');
        d3.select(domNode).attr('class', 'node');
        // now restore the mouseover event or we won't be able to drag a 2nd time
        d3.select(domNode).select('.ghostCircle').attr('pointer-events', '');
        d3.select(domNode).select('.ghostRect').attr('pointer-events', '');
        if (checkConnectionIsAvailable())
            updateTempConnector();
        if (draggingNode !== null) {
            init();
            draggingNode = null;
        }
    };

    var render = function () {
        const templateIds = {};
        var levelWidth = [1];

        const generateTemplateId = (name) => {
            if (templateIds[name]) {
                templateIds[name] = ++templateIds[name];
            } else {
                templateIds[name] = 1;
            }
            return`${name}-${templateIds[name]}`;
        }

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
        tree = tree.size([height, newWidth]).nodeSize([200, 60]);;

        // Compute the new tree layout.
        nodes = tree.nodes(root).reverse();
        links = tree.links(nodes);

        // Normalize for fixed-depth.
        nodes.forEach(function (d) {
             if (d && d.direction != "none") {
                d.x = (d.x * 5) / 3;
                d.y = (d.depth * depth);
                return;
             }
             d.y = d.depth * depth;
            });

        // Declare the nodes…
        var node = svg.selectAll("g.node")
            .data(nodes, function (d) { return d.id || (d.id = ++i); });

        $scope.clickableNode = true;
        $scope.setClickableNode = function (value) {
            $scope.clickableNode = value;
        }

        const getParents = (actionsList, validator, list) => {
            const removeElement = (id) => {
                actionsList = actionsList.filter(action => action.id != id);
            }
            actionsList.forEach(action => {
                if (validator.validatorStepId === action.validatorStepId && validator.direction === action.direction && !action.isValidator) {
                    list.push(action);
                    removeElement(action.id);
                } else if (action.id === validator.validatorStepId) {
                    getParents(actionsList, action, list);
                } else if (action.isValidator && (action.direction === 'none' || !action.direction)) {
                    list.push(action);
                    removeElement(action.id);
                }
            })
        }

        const getValidatorClaims = async (validator) => {
            let parentActions = [];
            let configClaims = {}
            let providerIds = [];
            getParents($scope.requiredActions, validator, parentActions)

            parentActions.forEach(action => {
                if (!action.isValidator && !action.isDelete) {
                    providerIds.push({id:action.providerId, config: action.config})
                }
            })
            const res = await GetValidationConfigs.create({realm:realm.realm},[...providerIds]).$promise;
            Object.entries(res).forEach(([key, value]) => configClaims[key.replace("sp@","")] = value);
            var { $promise, $resolved, ...cleanedResponse } = configClaims;
            return cleanedResponse;
        }

        var onNodeClick = function (event) {
            if ($scope.isReadOnly) {
                return;
            }
            if (event.defaultPrevented || event.isDefault || event.isPlaceHolder) return;
            var selectedExec = $scope.requiredActions.find(x => x.id === event.nodeId);
            var parentValidatorExec = $scope.requiredActions.find(x => x.id === selectedExec.validatorStepId);
            if($scope.clickableNode){
                $scope.clickableNode = false;
                if (event.validator) {
                    if($scope.autoSave){
                        getValidatorClaims(selectedExec).then((res) => {
                            $scope.gotoSettings(selectedExec, $scope.actionId, parentValidatorExec ? parentValidatorExec.enabled : true, $scope.actionPolicies, emptyValidatorConfig, $scope.cibaPolicies, $scope.setClickableNode, queryFlows, res, event, $scope.setRequiredActions, $scope.$parent.template);
                        })
                    } else {
                        $http.get(authUrl + `/admin/realms/${realm.realm}/authentication/validator-action-config/${$scope.actionId}/provider/${event.nodeId}`)
                        .then(function (res) {
                            $scope.gotoSettings(selectedExec, $scope.actionId, parentValidatorExec ? parentValidatorExec.enabled : true, $scope.actionPolicies, emptyValidatorConfig, $scope.cibaPolicies, $scope.setClickableNode, queryFlows, res.data.properties[0].dependsOn, event, $scope.setRequiredActions);
                        })
                        .catch(err => console.log(err));
                    }
                } else {
                    $scope.gotoSettings(selectedExec, $scope.actionId, parentValidatorExec ? parentValidatorExec.enabled : true, $scope.actionPolicies, emptyValidatorConfig, $scope.cibaPolicies, $scope.setClickableNode, queryFlows, [], event, $scope.setRequiredActions, $scope.$parent.template);
                }
            }
        };
        // Enter the nodes.
        var nodeEnter = node.enter().append("g")
            .attr("id", function (d) { return `node-${d.providerId ?? d.name.replaceAll(" ", "-")}`; })
            .call(d3.behavior.drag()
                .on("dragstart", function (d) {
                    if (d.isDefault || d.isPlaceHolder) {
                        return;
                    }
                    dragStarted = true;
                    nodes = tree.nodes(d);
                    d3.event.sourceEvent.stopPropagation();
                })
                .on("drag", function (d) {

                    if (isDragaRestricted(d)) {
                        return;
                    }

                    if (dragStarted) {
                        var domNode = this;
                        initiateDrag(d, domNode);
                    }

                    relCoords = d3.mouse($('svg').get(0));
                    if (relCoords[0] < panBoundary) {
                        panTimer = true;
                        pan(this, 'left');
                    } else if (relCoords[0] > ($('svg').width() - panBoundary)) {
                        panTimer = true;
                        pan(this, 'right');
                    } else if (relCoords[1] < panBoundary) {
                        panTimer = true;
                        pan(this, 'up');
                    } else if (relCoords[1] > ($('svg').height() - panBoundary)) {
                        panTimer = true;
                        pan(this, 'down');
                    } else {
                        try {
                            clearTimeout(panTimer);
                        } catch (e) {

                        }
                    }
                    d.x0 += d3.event.dx;
                    d.y0 += d3.event.dy;
                    var node = d3.select(this);
                    $(this).addClass('dragged');
                    node.attr("transform", "translate(" + d.x0 + "," + d.y0 + ")");
                    if (checkConnectionIsAvailable())
                    updateTempConnector();

                })
                .on("dragend", function (d) {
                    if (d.isDefault || d.isPlaceHolder) {
                        return;
                    }
                    domNode = this;
                    if (selectedNode && checkConnectionIsAvailable()) {
                        if (selectedNode.nodeId != draggingNode.parent.nodeId) {
                            var hierarchy = reiterateHierarchy();
                            if (hierarchy != null) {
                                $scope.treeData = [];
                                $scope.treeData.push(hierarchy[0]);
                            }
                        }
                        endDrag();
                    } else if (draggingNode) {
                        endDrag();
                    }
                }))
            .attr("class", "node")
            .attr("id", function (d) {
                    return createUUID();
            })
            .attr("transform", function (d) {
                if (d.isDefault) {
                    return "translate(" + d.x + "," + d.y + ")";
                } else if ( d.direction === "left") {
                    const temp = ((d.x - (rect.w / 2)));
                    return "translate(" + temp + "," + (d.y - (rect.h / 2)) + ")";
                } else if (d.direction === "right") {
                    return "translate(" + (d.x - (rect.w / 2)) + "," + (d.y - (rect.h / 2)) + ")";
                } else {
                    return "translate(" + (d.x - (rect.w / 2)) + "," + (d.y - (rect.h / 2)) + ")";
                }
            });

        // Draw Start and End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault;
        }).append("circle")
            .attr("r", circle.r)
            .style("fill", "#000");
            const centerX = 100;
            const centerY = 90;
            const size = 70;

            // Define the points of the diamond
            const points = [
                { x: centerX, y: centerY - size }, // top point
                { x: centerX + size, y: centerY }, // right point
                { x: centerX, y: centerY + size }, // bottom point
                { x: centerX - size, y: centerY }, // left point
            ];

            // Create a string from the points array
            const pointsString = points.map(p => `${p.x},${p.y}`).join(" ");

            nodeEnter.filter(function (d, i) {
                return !d.isDefault && !d.isPlaceHolder && d.validator;
            }).append("polygon")
                .attr("points", pointsString)
                .attr("rx", 4)
                .attr("stroke-width", 1)
                .style("fill", function (d) {
                    if (d.isConfigurable && !d.hasConfig) {
                        if (d.requirement) {
                            return configurableNodeColor;
                        } else {
                            return disabledNodeColor;
                        }
                    }
                    if (d.isConfigurable && d.hasConfig) {
                        if (d.requirement) {
                            return validatorNodeColor;
                        } else {
                            return disabledNodeColor;
                        }
                    }

                    if (!d.isConfigurable) {
                        if (d.requirement) {
                            return validatorNodeColor;
                        } else {
                            return disabledNodeColor;
                        }
                    }
                }).attr("stroke", function (d) {
                    if (d.isConfigurable && !d.hasConfig) {
                        if (d.requirement) {
                            return configurableNodeBorderColor;
                        } else {
                            return disabledNodeBorderColor;
                        }
                    }
                    if (d.isConfigurable && d.hasConfig) {
                        if (d.requirement) {
                            return validatorNodeBorderColor;
                        } else {
                            return disabledNodeBorderColor;
                        }
                    }

                    if (!d.isConfigurable) {
                        if (d.requirement) {
                            return validatorNodeBorderColor;
                        } else {
                            return disabledNodeBorderColor;
                        }
                    }
                });


        // Draw Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && !d.isPlaceHolder && !d.validator;
        }).append("rect")
            .attr('width', rect.w)
            .attr('height', rect.h)
            .attr("rx", 4)
            .attr("id", (d) => d.nodeId)
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            })
            .style("fill", function (d) {
                if (d.isConfigurable && !d.hasConfig) {
                    if (d.requirement) {
                        return configurableNodeColor;
                    } else {
                        return disabledNodeColor;
                    }
                }
                if (d.isConfigurable && d.hasConfig) {
                    if (d.requirement) {
                        return enabledNodeColor;
                    } else {
                        return disabledNodeColor;
                    }
                }

                if (!d.isConfigurable) {
                    if (d.requirement) {
                        return enabledNodeColor;
                    } else {
                        return disabledNodeColor;
                    }
                }
            }).attr("stroke", function (d) {
                if (d.isConfigurable && !d.hasConfig) {
                    if (d.requirement) {
                        return configurableNodeBorderColor;
                    } else {
                        return disabledNodeBorderColor;
                    }
                }
                if (d.isConfigurable && d.hasConfig) {
                    if (d.requirement) {
                        return enabledNodeBorderColor;
                    } else {
                        return disabledNodeBorderColor;
                    }
                }

                if (!d.isConfigurable) {
                    if (d.requirement) {
                        return enabledNodeBorderColor;
                    } else {
                        return disabledNodeBorderColor;
                    }
                }
            });

        // Draw Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && d.isPlaceHolder;
        }).append("rect")
            .attr('width', rect.w )
            .attr('height', rect.h + 100)
            .attr("rx", 5)
            .style("fill", "transparent")

        // Draw Icon inside Start Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault && d.parent == "null";
        }).append('svg:foreignObject')
            .attr("width", 30)
            .attr("height", 30)
            .attr("x", -15)
            .attr("y", -15)
            // .append("xhtml:body")
            .append("xhtml:div")
            .attr("class", "drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer")
            .append("xhtml:div")
            .append("xhtml:div")
            .attr("class", "d-flex justify-content-center text-center")
            .html('<i class="os-icon os-icon-user" style="color:#fff"></i>');

        // Draw Icon inside End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault && d.parent != "null";
        }).append('svg:foreignObject')
            .attr("width", 30)
            .attr("height", 30)
            .attr("x", -15)
            .attr("y", -15)
            .append("xhtml:div")
            .attr("class", "drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer")
            .append("xhtml:div")
            .append("xhtml:div")
            .attr("class", "d-flex justify-content-center text-center")
            .html('<i class="os-icon os-icon-check" style="color:#fff"></i>');


        // Draw Text Next to Start and End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault;
        }).append("text")
            .attr("x", function (d) {
                if (d.parent == "null") {
                    return $scope.realm.mode === 'bo' ? 125 : circleTextPosition;
                }
                else {
                    return 55;
                }
            })
            .attr("dy", ".35em")
            .attr("text-anchor", "middle")
            .text(function (d) { return $translate.instant(d.name); })
            .style("font-size", "14px")
            .style("font-weight", 400)
            .style("font-family", "Rubik");

        // Draw Text Inside Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && !d.isPlaceHolder && !d.validator;
        }).append("svg:foreignObject")
            .attr("width", rect.w)
            .attr("height", 100)
            .attr("x", 0)
            .attr("y", 0)
            .attr("class", function (d) {
                if (d.isConfigurable && !d.hasConfig) {
                    if (d.requirement) {
                        return 'workflow-configurable-node-background-color';
                    } else {
                        return 'workflow-disabled-node-background-color';
                    }
                }
                if (d.isConfigurable && d.hasConfig) {
                    if (d.requirement) {
                        return 'workflow-enabled-node-background-color';
                    } else {
                        return 'workflow-disabled-node-background-color';
                    }
                }

                if (!d.isConfigurable) {
                    if (d.requirement) {
                        return 'workflow-enabled-node-background-color';
                    } else {
                        return 'workflow-disabled-node-background-color';
                    }
                }
            })
            .html(function (d) {
                const workflowName = d.providerId;
                if(!$scope.workflowIndex[workflowName]) {
                    $scope.workflowIndex[workflowName] = 0;
                }
                $scope.workflowIndex[workflowName] +=1;
                const addTemplateId = `${workflowName + $scope.workflowIndex[workflowName]}-addTemplate`;
                const addValidatorId = `${workflowName + $scope.workflowIndex[workflowName]}-addValidator`;
                const templateId = generateTemplateId(d.providerId);
                const hasChild = $scope.requiredActions.find( action => action.parentId === d.nodeId);
                var isWorkflowModification = false;

                $(document).ready(function() {
                    if (hasChild) {
                        $(`#${addTemplateId}`).click(() => {
                            if(!isWorkflowModification){
                                isWorkflowModification = true;
                                AddTemplateToWorkflow.open(angular.copy($scope.unregisteredRequiredActions), addTemplate, d, true, () => isWorkflowModification = false);
                            }
                        })
                    }
                    $(`#${templateId}`).click(() => {
                        onNodeClick(d);
                    });
                    let isBtnclicked = false;
                    document.getElementById(addValidatorId) && document.getElementById(addValidatorId).addEventListener("click", () => {
                        if (!isBtnclicked && !isWorkflowModification) {
                            isWorkflowModification = true;
                            isBtnclicked = true;
                            addTemplate(d, angular.copy($scope.unregisteredRequiredActions.find(action => action.isValidator === "true")));
                        }
                    });
                });

                const addTemplatesDiv =
                `<div class='drawflow_content_node d-flex align-items-center justify-content-center cursor-pointer flex-column'>
                    <div class='draw-activity-box rounded' id="${templateId}">
                        <div class='draw-activity-info d-flex align-items-center justify-content-center text-center' title="${$translate.instant(d.name)}">
                            <span class='text-dark text-capitalize'>${$translate.instant(d.name)}</span>
                        </div>
                    </div>
                    ${
                        hasChild && !$scope.isReadOnly ?
                        `<div class='createflow-component-tooltip d-flex createflow-component-tooltip-middle btn'>
                            <div class="d-flex">
                                <button class="add-btn-validator-btn" id="${addTemplateId}">
                                    <img class="add-btn-validator" style="width:40px" src='${$scope.resourceUrl}/themes/img/add-template.svg'/>
                                </button>
                                <button class="add-btn-validator-btn" id="${addValidatorId}">
                                    <img class="add-btn-validator" src='${$scope.resourceUrl}/themes/img/add-validator-icon.svg'/>
                                </button>
                            </div>
                        </div>` : ``
                    }
                </div>`;

                return addTemplatesDiv;
            });

        // Draw Text for validator Inside Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && !d.isPlaceHolder && d.validator;
        }).append("svg:foreignObject")
            .attr("width", 100)
            .attr("height", 100 )
            .attr("x", 35)
            .attr("y", 62)
            .attr("class", function (d) {
                if (d.isConfigurable && !d.hasConfig) {
                    if (d.requirement) {
                        return 'workflow-configurable-node-background-color';
                    } else {
                        return 'workflow-disabled-node-background-color';
                    }
                }
                if (d.isConfigurable && d.hasConfig) {
                    if (d.requirement) {
                        return 'workflow-validator-node-background-color';
                    } else {
                        return 'workflow-disabled-node-background-color';
                    }
                }

                if (!d.isConfigurable) {
                    if (d.requirement) {
                        return 'workflow-validator-node-background-color';
                    } else {
                        return 'workflow-disabled-node-background-color';
                    }
                }
            })
            .attr("transform", "rotate(45, 120, 120)")
            .html(function (d) {
                const templateId = generateTemplateId(d.providerId);
                $(document).ready(function() {
                    $(`#${templateId}`).click(() => {
                        onNodeClick(d);
                    })
                 });
                var textColor = '';
                if (d.isConfigurable && !d.hasConfig) {
                    if (d.requirement) {
                        textColor = "text-dark";
                    } else {
                        textColor = "text-dark";
                    }
                }
                if (d.isConfigurable && d.hasConfig) {
                    if (d.requirement) {
                        textColor = "text-primary";
                    } else {
                        textColor = "text-dark";
                    }
                }

                if (!d.isConfigurable) {
                    if (d.requirement) {
                        textColor = "text-primary";
                    } else {
                        textColor = "text-dark";
                    }
                }
                return `<div id='${templateId}' class='drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer'>
                            <div class='draw-activity-box rounded'>
                                <div class='draw-activity-validator-info d-flex align-items-center top-0 justify-content-center text-center'>
                                    <svg width="100%" height="100%" viewBox="0 0 300 100">
                                        <foreignObject x="0" y="${d.name.length > 16 ? '0' : '20'}" width="100%" height="100%" transform="rotate(-45, 150, 50)">
                                            <div class="h-100 ${textColor} text-ellipsis-2-lines" style="font-size: 38px;" title="${d.name}">
                                                <span style="font-size: 38px;">${d.name}</span>
                                            </div>
                                        </foreignObject>
                                    </svg>
                                </div>
                            </div>
                        </div>`;
            }) ;

            function toTitleCase(str) {
                return str
                    .toLowerCase()
                    .split(' ')
                    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
                    .join(' ');
            }

            const addTemplate = (child, node) => {
                let childId = $scope.requiredActions.find( action => action.parentId === child.nodeId);
                let numericValues = [];
                let verifierNumbericPresence = {};
                let validatorCount = $scope.requiredActions.filter( action => {
                    if (action.name.startsWith("Validator ")) {
                        const value = action.name.slice(10);
                        const numericValue = parseInt(value, 10);
                        if (!isNaN(numericValue)) {
                          numericValues.push(numericValue);
                        }
                    }
                    return action.validator;
                }).length;
                node.direction = child.direction;
                node.validatorStepId = getValidatorId($scope.requiredActions, child, child.parent.nodeId);
                node.childId = childId?.id;
                node.parentId = child.parent.nodeId;
                let parentValidator = $scope.requiredActions.find( action => action.id === child.parent.validatorStepId);
                node.enabled = (parentValidator && parentValidator.hasOwnProperty('enabled')) ? parentValidator.enabled : true;
                node.name = $translate.instant(node.providerId);

                const makeAddActionRequest = () => {
                    RegisterRequiredAction.save({ realm: realm.realm, action: $scope.actionId }, node, function () {
                        Notifications.success($translate.instant("execution-created"));
                    queryFlows();
                    });
                }
                if(!verifierNumbericPresence.hasOwnProperty(node.providerId)){
                    verifierNumbericPresence[node.providerId] = [];
                }

                let verifierCount = $scope.requiredActions.filter( action => {
                    if(!verifierNumbericPresence.hasOwnProperty(action.providerId)){
                        verifierNumbericPresence[action.providerId] = [];
                    }
                    if (action.name.startsWith($translate.instant(node.providerId))) {
                        const value = action.name.slice($translate.instant(node.providerId).length + 1);
                        const numericValue = parseInt(value, 10);
                        if (!isNaN(numericValue)) {
                          verifierNumbericPresence[node.providerId].push(numericValue);
                        }
                    }
                    return action.name.startsWith($translate.instant(node.providerId));
                }).length;

                if(verifierCount)
                while (numericValues.includes(validatorCount + 1)){
                    validatorCount++ ;
                }
                while (verifierNumbericPresence[node.providerId].includes(verifierCount)){
                    verifierCount++ ;
                }
                if (node.isValidator === 'true') {
                    node.name = toTitleCase(node.providerId) + " " + (validatorCount + 1);
                }else{
                    node.name = $translate.instant(node.providerId) + (verifierCount ? " " + verifierCount : '');
                }

                if (childId) {
                    node.parentId = child.nodeId;
                    node.priority = childId.priority;
                    node.childId = childId.id;
                    node.childPriority = childId.priority;
                    if($scope.autoSave){
                        createDummyWorkflow(node)
                    } else {
                        $http.put(authUrl + `/admin/realms/${realm.realm}/authentication/register-required-actions/validator/raise-priority/${childId.id}`).then(function (result) {
                            makeAddActionRequest()
                        }).catch(function (error) {
                                Notifications.error($translate.instant('failed-to-update'));
                        });
                    }
                } else {
                    if($scope.autoSave){
                        createDummyWorkflow(node)
                    } else {
                        makeAddActionRequest();
                    }
                }
            }


        // Draw Text for validator Inside Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && d.isPlaceHolder;
        }).append("svg:foreignObject")
            .attr("width", rect.w)
            .attr("height", rect.h + 200)
            .attr("x", 0)
            .attr("y", 0)
            .html(function (d) {
                if (d.parent.providerId === "bo-custom-message" || $scope.isReadOnly) {
                    const endFlow = `
                    <div class='createflow-component-tooltip  createflow-component-tooltip-middle pt-3'>
                        <div class="endflow d-flex align-items-center justify-content-center">
                            ${$translate.instant("end")}
                        </div>
                    </div>
                    `;
                    return endFlow;
                }
                const workflowName = d.providerId ?? d.name.replace(/ /g,"_");
                if(!$scope.workflowIndex[workflowName]) {
                    $scope.workflowIndex[workflowName] = 0;
                }
                $scope.workflowIndex[workflowName] +=1;
                const addTemplateId = `${workflowName+$scope.workflowIndex[workflowName]}-addTemplate`;
                const addValidatorId = `${workflowName+$scope.workflowIndex[workflowName]}-addValidator`;
                const isParentValidator = d.parent.validator;
                const chooseOptionId = `${d.nodeId}-choose-option`;
                const chooseOptionBlockId = `${d.nodeId}-choose-option-block`;
                var isWorkflowModification = false;

                $(document).ready(function() {
                    $(`#${addTemplateId}`).off("click").on("click", () => {
                        if(!isWorkflowModification){
                            isWorkflowModification = true;
                            AddTemplateToWorkflow.open(angular.copy($scope.unregisteredRequiredActions), addTemplate, d, false, () => isWorkflowModification = false);
                        }
                   })

                //    $(`#${chooseOptionId}`).click( () => {
                //         $(`#${chooseOptionBlockId}`).css('display',"block");
                //     });
                    let isBtnclicked = false;
                    document.getElementById(addValidatorId).addEventListener("click", () => {
                        if (!isBtnclicked && !isWorkflowModification) {
                            isBtnclicked = true;
                            isWorkflowModification = true;
                            addTemplate(d, angular.copy($scope.unregisteredRequiredActions.find(action => action.isValidator === "true")));
                            setTimeout(() => {
                                isBtnclicked = false;
                                isWorkflowModification = false;
                            }, 1000);
                        }
                    })
                });

                const isDirection = d.hasOwnProperty('direction') && d.direction;
                const addTemplatesDiv =
                `<div class='createflow-component-tooltip createflow-component-tooltip-middle pt-3' id="${chooseOptionId}">
                    <img src='${$scope.resourceUrl}/themes/img/workflow-module-add-icon.svg'>
                    <div class='createflow-component-tooltiptext rounded' id="${chooseOptionBlockId}">
                        <div class='bg-white text-center p-2'>
                            <p class='my-1'>${$translate.instant("choose-the-option")}</p>
                            <div id="${addTemplateId}" class='workflow-createTemplate-card rounded cursor-pointer py-2 mt-3 mb-2'><img class="pr-1" src="${$scope.resourceUrl}/themes/img/workflow-template-rectangle.svg">${$translate.instant("workflow")} ${isDirection ? `=<span class='pl-1 text-${ d.direction === 'left' ? 'success' : 'danger'}'> ${ d.direction === 'left' ? 'TRUE' : 'FALSE' }</span>` : ""}</div>
                            <div id="${addValidatorId}"  class='workflow-createValidator-card rounded cursor-pointer py-2'><img class="pr-1" src="${$scope.resourceUrl}/themes/img/workflow-validator-diamond.svg">${$translate.instant("validator")}</div>
                        </div>
                    </div>
                </div>`;
                return addTemplatesDiv;
            });

        // Add temp Selection Ghost around Rectangle
            nodeEnter.append("rect")
            .filter(function (d, i) {
                return !d.isDefault && !d.isPlaceHolder;
            })
            .attr('class', 'ghostRect')
            .attr('width', rect.w)
            .attr('height', rect.h)
            .attr("opacity", 0) // change this to zero to hide the target area
            .style("fill", "#047bf8")
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

        const nodeElements = document.querySelectorAll('.ghostRect');
        nodeElements.forEach(element => {
            element.addEventListener('dragover', (event) => {
                const id = event.target['__data__'].nodeId;
                if (id) {
                    const dropElement = document.getElementById(id)
                    if (dropElement)
                        dropElement.classList.add("d3element");
                }
            });

            element.addEventListener("dragleave", (event) => {
                const removeD3Element = document.querySelector(".d3element");
                if (removeD3Element) {
                    removeD3Element.classList.remove("d3element")
                }
            })
        });

        nodeEnter.append("rect")
            .filter(function (d, i) {
                return !d.isDefault && d.isPlaceHolder;
            })
            .attr('class', 'dragRect')
            .attr("id", (d)=> d.nodeId)
            .attr('width', rect.w)
            .attr('height', rect.h)
            .attr("opacity", 0.2) // change this to zero to hide the target area
            .style("fill", "red")
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

        // Add temp Selection Ghost around Circle
        nodeEnter.append("circle")
            .filter(function (d, i) {
                return d.isDefault && d.parent === "null";
            })
            .attr('class', 'ghostCircle')
            .attr('r', circle.r * 2)
            .attr("opacity", 0) // change this to zero to hide the target area
            .style("fill", "#047bf8")
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

        // Declare the links…
        var link = svg.selectAll("path.link")
            .data(links, function (d) { return d.target.id; }).attr("shape-rendering", "crispEdges");

        svg.selectAll("path.link").filter((d) => {
            return true;
        })

        link.enter().insert("path", "g").filter((d)=>{
            return !d.source.direction || (d.source.direction && d.source.direction === "none");
        })
        .attr("class", "link")
        .attr("stroke", "silver")
        .attr({
            "d": diagonal
        });


        // Enter the links.
        link.enter().insert("path", "g").filter((d)=>{
            return (!d.source.validator && d.source.direction && d.source.direction !== "none" && d.source.direction === "left")
            || (d.source.validator && d.target.direction === "left") ;
        })
        .attr("class", "link")
        .attr("stroke", function (d) {
            if (d.target.isConfigurable && !d.target.hasConfig) {
                if (d.target.requirement) {
                    return "#198511";
                } else {
                    return disabledNodeBorderColor;
                }
            }
            if (d.target.isConfigurable && d.target.hasConfig) {
                if (d.target.requirement) {
                    return "#198511";
                } else {
                    return disabledNodeBorderColor;
                }
            }

            if (!d.target.isConfigurable) {
                if (d.target.requirement) {
                    return "#198511";
                } else {
                    return disabledNodeBorderColor;
                }
            }
        })
        .attr({
            "d": diagonal
        });


        link.enter().insert("path", "g").filter((d)=>{
            return (!d.source.validator && d.source.direction && d.source.direction !== "none" && d.source.direction === "right")
            || (d.source.validator && d.target.direction === "right");
        })
        .attr("class", "link")
        .attr("stroke", function (d) {
            if (d.target.isConfigurable && !d.target.hasConfig) {
                if (d.target.requirement) {
                    return "red";
                } else {
                    return disabledNodeBorderColor;
                }
            }
            if (d.target.isConfigurable && d.target.hasConfig) {
                if (d.target.requirement) {
                    return "red";
                } else {
                    return disabledNodeBorderColor;
                }
            }

            if (!d.target.isConfigurable) {
                if (d.target.requirement) {
                    return "red";
                } else {
                    return disabledNodeBorderColor;
                }
            }
        })
        .attr({
            "d": diagonal
        });

        nodes.forEach(function (d) {
            d.x0 = d.x;
            d.y0 = d.y;
        });
        $scope.isFlowLoading = false;
    };

    $scope.dragEnd = function (evt) {
        evt.preventDefault();
        d3.select(domNode).select('.dragRect').attr('pointer-events', '');
        d3.selectAll('.dragRect').attr('class', 'dragRect');
    };

    function setTree() {
        $scope.treeData = [
            {
                "name": $translate.instant('after-user-confirmation'),
                "parent": "null",
                "isPlaceHolder": false,
                "nodeId": "After User Confirmation",
                "isDefault": true,
                "direction":"none",
                "validatorStepId":"none",
                "parentId":"none",
                "children": [
                    {
                        "name": $translate.instant('drag-and-drop-templates'),
                        "parent": $translate.instant('after-user-confirmation'),
                        "nodeId": "drag-and-drop-template",
                        "isPlaceHolder": true,
                        "isDefault": false,
                        "children": null,
                    },
                ]
            }
        ];
    }

    $scope.delete = function () {
        if($scope.clickableNode){
            $scope.clickableNode = false;
            Dialog.confirmDelete($scope.actionrep.alias, $scope.actionrep.subWorkflow ? 'sub-workflow': "workflow", function () {
                Actions.remove({ realm: realm.realm, action: $scope.actionrep.actionId }, function () {
                    Notifications.success($translate.instant("template-deleted"));
                    $location.url("/realms/" + realm.realm + '/authentication/templates');
                });
                $scope.clickableNode = true;
            }, function () {
                $scope.clickableNode = true;
            });
            document.addEventListener('keydown', function(event) {
                if (event.key === 'Escape' || event.keyCode === 27) {
                  $scope.clickableNode = true;
                }
            });
        }
    };
     $scope.isTemplatePreviewVisible = true;
     $scope.showTemplatePreview = function () {
        if($scope.isTemplatePreviewVisible){
        $scope.isTemplatePreviewVisible = false;
        WorkflowVisualizeGif.query({ realm: realm.realm, action: $scope.actionId}, function (response){
            var selectedTemplate =  $scope.actionId;
            WorkflowPreviewDialog.open(response, selectedTemplate[0], $scope.actionrep.subWorkflow)
            $scope.isTemplatePreviewVisible = true;
            }, function () {
            $scope.isTemplatePreviewVisible = true;
            })
        }
    }

    $scope.isActionVariablesVisible = true;
    $scope.showActionVariables = function () {
      if ($scope.isActionVariablesVisible) {
        $scope.isActionVariablesVisible = false;
        ActionVariablesDialog.open(
          $translate.instant("action-variables"),
          $scope.realm.realm,
          $scope.actionrep,
          function (data) {
            $scope.actionrep = data;
          },
          function () {
            $scope.isActionVariablesVisible = true;
          }
        );
      }
    };

    $scope.saveMarketPlace = () => {
        if (isNewMarketPlace) {
            RegisterRequiredActions.save({ realm: realm.realm, action: $scope.actionId }, $scope.requiredActions, function () {
                Notifications.success($translate.instant("execution-updated"));
                queryFlows();
            });
        } else {
            UpdateRequiredActions.update({ realm: realm.realm, action: $scope.actionId }, $scope.requiredActions, function () {
                Notifications.success($translate.instant("execution-updated"));
                queryFlows();
            });
        }
    }

    $scope.exportWorkflowJson = () => {
        var controller = function ($scope, $modalInstance, action, requiredActions) {
            $scope.exportFileName = '';
            $scope.isSubWorkflow = action.subWorkflow;
            $scope.ok = function () {
                let workflowJson = {};
                workflowJson.actionVariables = angular.copy(action);
                workflowJson.data = angular.copy(requiredActions);
                const jsonData = JSON.stringify(workflowJson, null, 2);
                const blob = new Blob([jsonData], { type: "application/json" });
                const link = document.createElement("a");
                link.href = URL.createObjectURL(blob);
                link.download = $("#name").val()+".json";
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                $modalInstance.close();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $(document).ready(function () {
                setTimeout(function () {
                    $("#name").focus();
                }, 100);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/export-workflow.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                action : () => $scope.actionrep,
                requiredActions : () => $scope.requiredActions
            }
        });
    }

    $scope.checkHasPolicy = function (policyId) {
        for (const policy of $scope.actionPolicies.concat($scope.cibaPolicies)) {
            if (policyId === policy.id) {
                return true;
            }
        }
        return false;
    };

    var RequiredActionsQueryResultAction = function (data) {
        $scope.requiredActions = [];
        $scope.unregisteredRequiredActions = unregisteredRequiredActionsCopy;
        for (var i = 0; i < data.length; i++) {
            $scope.requiredActions.push(data[i]);
        }
        if ($scope.requiredActions.length > 0) {
            setTree();
            orginalRoot = $scope.treeData[0];
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
            childCount(0, orginalRoot);
            var tempNodes = d3.layout.tree().nodes(orginalRoot);
            var sortedItems = [];

            $scope.requestArr = $scope.requiredActions.map(data => $http.get(authUrl + '/admin/realms/' + realm.realm + '/authentication/action-config-description/' + data.providerId));

            // fetching the config data of all the providers at once.

            const checkIfMapOptionAvailable = (config, option) => {
                if (option.hasOwnProperty('mapOptions')) {
                    let count = option.mapOptions.filter(
                    (x) => x.value ? x.value === config[option.name] : true
                    ).length;
                    if (count === 0 || option.type==="Notification_Warn") {
                        return true;
                    }
                }
                return false;
            };

            $q.all($scope.requestArr).then(function (values) {

                // this array contains the result that if a provider is configured or not.
                let configRequirementDetailArr = values.map((currentValue, index) => {
                    let actionConfigured = true;
                    let actionData = currentValue.data

                    if (actionData.configurable && actionData.properties) {
                        for (let itr = 0; itr < actionData.properties.length; itr++) {
                            if (checkIfMapOptionAvailable($scope.requiredActions[index].config,actionData.properties[itr]) || actionData.properties[itr].required && ((!$scope.requiredActions[index]?.config?.[actionData.properties[itr].name]) || (actionData.properties[itr].type.toLowerCase() === 'list' && !actionData.properties[itr].options.includes($scope.requiredActions[index]?.config?.[actionData.properties[itr].name])))) {
                                return false;
                            }
                        }
                    }
                    return actionConfigured;
                });
                tempNodes = tempNodes.filter(node => !node?.isDelete)
                let lastParent;
                $scope.parentIds = {}
                $scope.requiredActions.forEach((element, index) => {
                    if (!element.isDelete) {
                        var newNode = {
                            "name": element.name,
                            "isDefault": false,
                            "isPlaceHolder": false,
                            "providerId": element.providerId,
                            "nodeId": element.id,
                            "isConfigurable": element.configurable,
                            "hasConfig": configRequirementDetailArr[index],
                            "requirement": element.enabled,
                            "priority": element.priority,
                            "parentId": element.parentId,
                            "direction": element.direction,
                            "validatorStepId" : element.validatorStepId,
                            "validator":element.validator,
                            "hasPolicy": element.hasOwnProperty('policyId') && $scope.checkHasPolicy(element.policyId),
                            "isEnabled": element.enabled,
                            "providerId": element.providerId,
                        }

                        if ($scope.parentIds[element.parentId]) {
                            newNode.parentId = $scope.parentIds[element.parentId];
                        }

                        tempNodes.splice(tempNodes.length - 1, 0, newNode);
                    } else {
                        $scope.parentIds[element.id] = element.parentId;
                        lastParent = element;
                    }
                });

                tempNodes.forEach((element, index) => {
                    if (!element.isDelete) {
                        var item = {
                            "name": element.name,
                            "isDefault": element.isDefault,
                            "isPlaceHolder": element.isPlaceHolder,
                            "nodeId": element.nodeId,
                            "providerId": element.providerId,
                            "isConfigurable": element.isConfigurable,
                            "hasConfig": element.hasConfig,
                            "requirement": element.requirement,
                            "priority": element.priority,
                            "direction": element.direction,
                            "validatorStepId" : element.validatorStepId,
                            "validator": element.validator,
                            "parentId": element.parentId,
                            "parent": index == 0 ? "null" : tempNodes[index - 1].name,
                            "hasPolicy": element.hasPolicy,
                            "isEnabled": element.isEnabled,
                            "providerId": element.providerId
                        };

                        if (index != (tempNodes.length - 1)) {
                            item["children"] = null;
                        }
                        sortedItems.push(item);
                    }
                });

                list_to_tree(sortedItems);

                $scope.treeData = [];
                $scope.treeData.push(sortedItems[0]);
                init();
            }).catch(function (err) {
                // Never leave the editor stuck on the spinner: surface the failure instead.
                $scope.isFlowLoading = false;
                console.error('Workflow render failed:', err);
                Notifications.error($translate.instant('workflow-render-failed'));
            });
        }
        else {
            $scope.unregisteredRequiredActions = unregisteredRequiredActionsCopy;
            setTree();
            init();
        }
    }

    const createDummyWorkflow = (node) => {
        const temp = {
            "name": node.name,
            "providerId": node.providerId,
            "enabled": true,
            "skippable": false,
            "validator": node.isValidator === "true",
            "isValidator": node.isValidator === "true",
            "defaultAction": false,
            "priority": getMaxPriority() + 1,
            "policyType": "ciba",
            "parentId": node.parentId,
            "direction": node.direction ? node.direction : 'none',
            "validatorStepId": node.validatorStepId,
            "id": Math.random().toString(36).substring(2),
            "config": {},
            "hasPolicy": true,
            "configurable": true
        };
        if (node.childId) {
            temp.priority = node.childPriority;
            let nextValidator = {id:"", priority: getMaxPriority()};
            $scope.requiredActions.forEach( action => {
                if (node.priority <= action.priority && action.validator && !nextValidator.id) {
                    nextValidator = action;
                }
            })

            $scope.requiredActions = $scope.requiredActions.map( action => {
                a = action;
                if (node.isValidator === "true" && action.priority >= node.priority && action.priority <= nextValidator.priority) {
                    a.direction = "left";
                    a.validatorStepId = temp.id;
                }
                if (action.priority >= node.childPriority) {
                    a.priority = a.priority + 1;
                }
                if (action.id === node.childId) {
                    a.parentId = temp.id;
                }
                return a;
            })
        }

        $scope.requiredActions = [...$scope.requiredActions, temp]
        $scope.requiredActions = $scope.requiredActions.sort(function(a, b){return a.priority - b.priority});
        RequiredActionsQueryResultAction($scope.requiredActions);
        Notifications.success($translate.instant("execution-created"));
    }

    const getMaxPriority = () => {
        return $scope.requiredActions.reduce((max, action) => {
            return Math.max(max, action.priority);
        }, 0);
    }


    var queryFlows = function () {
        $scope.isFlowLoading = true;
//        updateEstimation();
        if($scope.isReadOnly){
            var data = JSON.parse($scope.$parent.template.workflowJson);
            if(data.hasOwnProperty('data')){
                isNewMarketPlace = false;
                RequiredActionsQueryResultAction(data.data);
                $scope.oldValue = data.data;
            } else {
                RequiredActionsQueryResultAction([]);
            }
        } else {
            RequiredActionsQuery.query({ realm: realm.realm, action: $scope.actionId }, function (data) {
                $scope.oldValue = data
                RequiredActionsQueryResultAction(data);
                if (data.length > 0) {
                    isNewMarketPlace = false;
                }
            });
        }
    };

    $scope.queryFlows = queryFlows;

    $scope.removePolicy = function (action) {
        action.policyId = null;
        RequiredActions.update({ realm: realm.realm, alias: action.alias }, action, function () {
            Notifications.success("Required action updated");
            setupRequiredActionsForm();
        });
    };

    $scope.gotoSettings = function (action, actionId, isParentValidatorEnabled, actionPolicies, emptyValidatorConfig, cibaPolicies, setClickableNode, queryFlows, validatorCliams, node, setRequiredActions, template) {
        var controller = function ($scope, requiredActions, autoSave, $location) {
            $scope.realm = realm;
            $scope.actionId = actionId;
            $scope.isParentValidatorEnabled = isParentValidatorEnabled;
            $scope.requiredActions = requiredActions;
            $scope.autoSave = autoSave;
            $scope.action = angular.copy(action);
            $scope.action.name = $translate.instant($scope.action.name);
            $scope.resourceUrl = resourceUrl;
            $scope.hasConfig = true;
            $scope.changed = false;
            $scope.genSaveEnabled = false;
            $scope.policySaveEnabled = false;
            $scope.attrSaveEnabled = false;
            $scope.configProps = {};
            $scope.isActionConfigurable = false;
            $scope.actionPolicies = actionPolicies;
            $scope.validatorClaimsLoaded = false;
            $scope.validatorCliams = validatorCliams;
            $scope.cibaPolicies = cibaPolicies;
            $scope.resourceUrl = resourceUrl;
            $scope.isInfo = true;
            $scope.newConfig = {};
            $scope.isConfig = false;
            $scope.isConfigLoader = false;
            let oldConfig = null;
            $scope.infoProps = {};
            $scope.showSkipOption = false;
            $scope.versions = [];

            if ($scope.action.providerId === "terms-and-conditions") {
                TermsConditionsVersions.query({ realm: realm.realm, id: $scope.action.id }, function (data) {
                    if (data) {
                        $scope.versions = data;
                    }
                });
            }

            const processConfig = (conresponse, isConfig) => {
                //Set policyId in UI if hasPolicy
                if (conresponse.hasPolicy) {
                    $scope.action.selectedPolicy = conresponse.policyId;
                    $scope.oldCopy = angular.copy($scope.action);
                }
                if(!checkPolicyInCibaPolicies(conresponse.policyId)) {
                    delete $scope.action.selectedPolicy;
                }

                const compareConfigs = (a, b) => {
                    if (Array.isArray(a)) {
                        return a.includes(b?.toString())
                    }
                    return a?.toString() === b?.toString();
                }

                var defaultConfig = {};
                if ($scope.configProps && Array.isArray($scope.configProps)) {
                    for (var i = 0; i < $scope.configProps.length; i++) {
                        var property = $scope.configProps[i];
                        const value =  conresponse.config[property.name] ?  conresponse.config[property.name] : property.defaultValue ? property.defaultValue : property.type == 'List' ? null : property.type == 'MultivaluedString' ? [''] : property.type == 'boolean' ? 'false' : '';
                        if (property && property.name) {
                            if (property.dependsOn) {
                                Object.keys(property.dependsOn).forEach( key => {
                                    const dependsOnField = $scope.configProps.find(prop => prop.name === key);
                                    if ((!$scope.config?.config[dependsOnField.name] && compareConfigs(property.dependsOn[dependsOnField.name], $scope.oldCopy.config[dependsOnField.name])) ||
                                    (compareConfigs(property.dependsOn[dependsOnField.name], $scope.config?.config[dependsOnField.name])) ||
                                    (!$scope.config?.config[dependsOnField.name] && !$scope.oldCopy.config[dependsOnField.name] && compareConfigs(property.dependsOn[dependsOnField.name], dependsOnField.defaultValue))) {
                                        if (typeof isConfig === 'boolean') {
                                            defaultConfig[property.name] = property.defaultValue;
                                        } else
                                        defaultConfig[property.name] = value;
                                    }
                                });
                            } else {
                                defaultConfig[property.name] = value;
                            }
                            if( property.type == 'boolean' ) {
                                defaultConfig[property.name] = `${defaultConfig[property.name]}`;
                            }

                            if (property.type.toLowerCase() == 'list' && property.name.toLowerCase() === 'verification-type') {
                                // TODO: CHANGE ME PATCH FIX
                                if (conresponse.config[property.name] != undefined && !property.options.includes(defaultConfig[property.name])) {
                                    defaultConfig[property.name] = property.defaultValue;
                                }
                            }
                            if (property.type.toLowerCase() == 'map') {
                                if(!defaultConfig[property.name] || defaultConfig[property.name].length == 0){
                                    defaultConfig[property.name] = [];
                                }else {
                                    defaultConfig[property.name] = JSON.parse(defaultConfig[property.name]);
                                }
                            }
                        }
                    }
                }
                if (typeof isConfig !== 'boolean') {
                    $scope.oldCopy.config = defaultConfig;
                    $scope.oldCopy.config.isvalidExpiryTime = true;
                    ComponentUtils.convertBackToMultivaluedList($scope.configProps, $scope.oldCopy.config);
                    $scope.config = JSON.parse(JSON.stringify($scope.oldCopy));
                    ComponentUtils.removeEmptyStringInMultivaluedString($scope.configProps, $scope.oldCopy.config);
                    $scope.isConfigLoader = true;
                } else {
                    $scope.config.config = defaultConfig;
                    $scope.config.config.isvalidExpiryTime = true;
                }
            }

            function getSavedConfigValues() {
                if($scope.autoSave) {
                    processConfig($scope.action)
                } else {
                    RequiredActions.get(
                        {
                            realm: realm.realm,
                            alias: $scope.action.id,
                        },
                        processConfig,
                        function (error) {
                            Notifications.error(error.data.errorMessage);
                        }
                    );
                }
            }

            function checkPolicyInCibaPolicies(policyId) {
            const policyExists = $scope.cibaPolicies.filter(policy => policy.id === policyId);
                if(!policyExists.length > 0) {
                    return false;
                }
                return true;
            }

            $scope.$evalAsync(function () {
                $http.get(authUrl + '/admin/realms/' + realm.realm + '/authentication/action-config-description/' + $scope.action.providerId)
                    .then(function (res) {
                        if (res.data.configurable) {
                            $scope.isActionConfigurable = true;
                            $scope.configProps = res.data.properties;
                        }
                        $scope.infoProps = res.data.info;
                        $scope.showSkipOption = (res.data.shouldShowSkipOption == undefined || res.data.shouldShowSkipOption == '')
                                                ? false :
                                                res.data.shouldShowSkipOption;
                        getSavedConfigValues();
                    })
                    .catch(err => console.log(err));
            });

            if ($scope.realm.mode == 'bo') {
                $scope.action.selectedPolicyType = "ciba";
            }
            else {
                $scope.action.selectedPolicyType = "action";
            }

            if ($scope.action.policyId != undefined) {
                if ($scope.action.selectedPolicy == "action") {
                    for (var i = 0; i < $scope.actionPolicies.length; i++) {
                        var id = $scope.actionPolicies[i].id;
                        if (id == $scope.action.policyId) {
                            $scope.action.selectedPolicy = $scope.actionPolicies[i].id;
                            $scope.action.selectedPolicyType = "action";
                            break;
                        }
                    }
                }
                if ($scope.action.selectedPolicy == "ciba") {
                    for (var i = 0; i < $scope.cibaPolicies.length; i++) {
                        var id = $scope.cibaPolicies[i].id;
                        if (id == $scope.action.policyId) {
                            $scope.action.selectedPolicy = $scope.cibaPolicies[i].id;
                            $scope.action.selectedPolicyType = "ciba";
                            break;
                        }
                    }
                }
            }
            $scope.$watch('action', function () {
                if ($scope.action && !$scope.action.selectedPolicy) {
                    delete $scope.action.selectedPolicy;
                }
                if ($scope.oldCopy && $scope.action.name && (!angular.equals($scope.action.enabled, $scope.oldCopy.enabled) || !angular.equals($scope.action.selectedPolicy, $scope.oldCopy.selectedPolicy) || !angular.equals($scope.action.description, $scope.oldCopy.description) || !angular.equals($scope.action.name, $scope.oldCopy.name) || !angular.equals($scope.action.skippable, $scope.oldCopy.skippable))) {
                    $scope.genSaveEnabled = true;
                } else {
                    $scope.genSaveEnabled = false;
                }
                if ($scope.oldCopy && $scope.action.name && !angular.equals($scope.action.enabled, $scope.oldCopy.enabled) && !angular.equals($scope.action.name, $scope.oldCopy.name) && !$scope.action.selectedPolicy && !angular.equals($scope.action.skippable, $scope.oldCopy.skippable)) {
                    $scope.genSaveEnabled = true;
                }
            }, true);


            const checkDependOnValue = (old, newValue)=> {
                let state = false;
                if (!old) return state;
                Object.entries(old).map(([key, value]) => {
                    if (value !== newValue[key]) {
                        const dependOnKey = $scope.configProps.filter(prop => prop?.dependsOn && prop.dependsOn[key]);
                        if (dependOnKey.length > 0) {
                            state = true;
                        }
                    }
                })
                return state
            }

            //watch the Config Object in Action Configs
            $scope.$watch('config', function (newValue, oldValue) {
                if (!$scope.config) return;
                if($scope.config.config.hasOwnProperty("isReadOnly")){
                    $scope.oldCopy.config["isReadOnly"] = $scope.config.config["isReadOnly"];
                }
                ComponentUtils.removeEmptyStringInMultivaluedString($scope.configProps, $scope.config.config);
                if (!angular.equals($scope.oldCopy.config, $scope.config.config) && checkConfigWithMultipleDependsOn($scope.config.config, $scope.configProps)) {
                    $scope.attrSaveEnabled = true;
                    // $('#saveConfigBtn').prop('disabled', false);
                } else {
                    $scope.attrSaveEnabled = false;
                }
                $scope.configProps.map(prop => {
                    if (prop.type === "Number" && !ProcessInput.validateRange($scope.config.config, prop)) {
                        $scope.attrSaveEnabled = false;
                    }
                 })
                if($scope.config.config.hasOwnProperty("isReadOnly")){
                    delete $scope.oldCopy.config["isReadOnly"];
                }
                if (checkDependOnValue(oldValue?.config, newValue?.config))
                    processConfig($scope.config, true);
            }, true);


            $scope.createPolicy = function () {
                $scope.dismissPanel();
                $location.url("/realms/" + realm.realm + '/policies');
            };

            const getValidatorChilds = (actionsList, validator, list) => {
                actionsList.forEach( action => {
                    if (validator.id === action.validatorStepId && action.validator) {
                        list.push(action.id);
                        actionsList = actionsList.filter(a => action.id !== a.id)
                        getValidatorChilds(actionsList, action, list);
                    } else if (validator.id === action.validatorStepId && !action.validator) {
                        list.push(action.id);
                        actionsList = actionsList.filter(a => action.id !== a.id)
                    }
                })
            }

            var deleteWorkflowModule = function () {
                if ($scope.autoSave) {
                    const list = [];
                    if (action.validator) {
                        list.push(action.id);
                        getValidatorChilds(requiredActions, action, list);
                    }
                    RequiredActionsQueryResultAction(requiredActions.map(a => {
                        if (a.id === action.id || list.includes(a.id)) {
                            return {...a, isDelete: true};
                        }
                        return a;
                    }));
                    Notifications.success($translate.instant("action-deleted"));
                    $scope.$panelInstance.dismiss('this is from the controller!!');
                    return;
                }
                RequiredActions.remove({ realm: realm.realm, alias: action.id }, function () {
                    $scope.$panelInstance.dismiss('this is from the controller!!');
                    UnregisteredRequiredActions.query({realm: realm.realm, action: $scope.actionId}, function (data) {
                        unregisteredRequiredActionsCopy = data
                        queryFlows();
                    }, function(){
                        Notifications.error($translate.instant("ciba-failed-to-fetch-required-action"));
                    })
                    Notifications.success($translate.instant("action-deleted"));
                });
            }

            $scope.removeRequiredAction = function () {
                Dialog.confirmDelete($scope.oldCopy.name, 'action', function () {
                    var targetChildValidator = $scope.requiredActions.find(action => action.validatorStepId === $scope.action.validatorStepId && action.validator && action.direction === $scope.action.direction );
                    // if(targetChildValidator){
                    //     emptyValidatorConfig(targetChildValidator, () => {
                    //         deleteWorkflowModule();
                    //     })
                    // }else{
                    //     deleteWorkflowModule();
                    // }
                    deleteWorkflowModule();
                });
            };

            const createNewPolicy = (policyName, policyType, policyValue, id, callback, payload) => {
                $scope.policyRep = {
                    policyName : createUUID(),
                    policyType,
                    policyValue,
                    realmPolicy: false,
                };

                if (id) $scope.policyRep.id = id;

                Policies.update({ realm: $scope.realm.realm }, $scope.policyRep, function (res) {
                    payload.config['validation-policy-id'] = id ? id : res.id
                    callback(payload);
                });
            }

            const saveConfig = (payload) => {
                if(template && template.marketplace) {
                    var payloadCopy = angular.copy(payload);
                    var existingVerifiersWithSameNameCount = 0;
                    var verifierNames = requiredActions.map(a => {
                        if(a.name?.toLowerCase() == payloadCopy.name?.toLowerCase() && payloadCopy.id != a.id){
                            existingVerifiersWithSameNameCount++;
                        }
                        return a.name;
                    });
                    if (existingVerifiersWithSameNameCount > 0) {
                        let suffix = existingVerifiersWithSameNameCount;
                        let newName = `${payloadCopy.name} ${suffix}`;

                        while (verifierNames.includes(newName)) {
                            suffix++;
                            newName = `${payloadCopy.name} ${suffix}`;
                        }

                        payloadCopy.name = newName;
                    }
                    RequiredActionsQueryResultAction(requiredActions.map(a => {
                        if (a.id === payloadCopy.id) {
                            return payloadCopy;
                        }
                        return a;
                    }))
                    $scope.$panelInstance.dismiss('this is from the controller!!');
                    Notifications.success($translate.instant("changesSavedSuccess"));
                    setClickableNode(true);
                    newTcVersion = "";
                } else {
                    RequiredActions.update({
                        realm: realm.realm,
                        alias: action.id
                    }, payload, function () {
                        queryFlows();
                        $scope.$panelInstance.dismiss('this is from the controller!!');
                        Notifications.success($translate.instant("changesSavedSuccess"));
                        setClickableNode(true);
                        newTcVersion = "";
                    }, function(){
                        $scope.attrSaveEnabled = true;
                    });
                }
            }

            $scope.showTemplateDelete = () => {
                return (!$scope.action.validator && $scope.requiredActions.find(action => action.id === $scope.action.parentId)?.validator && $scope.requiredActions.find(action => action.parentId === $scope.action.id)?.validator);
            }

            $scope.save = function () {
                var copy = angular.copy($scope.action);
                var configCopy = angular.copy($scope.config);
                if (!$scope.config.config.isvalidExpiryTime) {
                    delete configCopy.config.isvalidExpiryTime;
                    Notifications.error($translate.instant('invalid-expiry-time'));
                    return ;
                }
                Object.keys(configCopy.config).forEach(key => {
                    if (configCopy?.config[key] == null) {
                        delete configCopy.config[key];
                    }
                })
                $scope.attrSaveEnabled = false;
                ComponentUtils.convertAllListValuesToMultivaluedString($scope.configProps, configCopy.config);
                copy.config = configCopy.config;
                copy.policyId = $scope.action.selectedPolicy;
                copy.policyType = $scope.action.selectedPolicyType;
                if (node && node.validator && !$scope.autoSave) {
                    createNewPolicy("newPolicy", "validatorPolicy", copy.config['validation-condition-prompt'], copy.config['validation-policy-id'] , saveConfig, copy)
                } else {
                    saveConfig(copy)
                }
                delete copy.selectedPolicy;
                delete copy.selectedPolicyType;
                delete copy.config.isvalidExpiryTime;
            };

            $scope.closePanel = function () {
                $scope.$panelInstance.close('this is from the controller!!');
                setClickableNode(true);
            };

            $scope.dismissPanel = function () {
                $scope.$panelInstance.dismiss('this is from the controller!!');
                setClickableNode(true);
            };

            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
                setClickableNode(true);
            });

            $scope.reset = function () {
                $scope.config.config = angular.copy($scope.oldCopy.config);
                ComponentUtils.convertBackToMultivaluedList($scope.configProps, $scope.config.config);
                $scope.dismissPanel();
            };

            $scope.configLength = function () {
                if ($scope.action.config !== undefined)
                    return Object.keys($scope.action.config).length;
                return 0;
            };

            $scope.tabSelect = function (tab) {
                if (tab == "General") {
                    $scope.isGeneral = true;
                    $scope.isConfig = false;
                    $scope.isInfo = false;
                }
                else if (tab == "Policy") {
                    $scope.isGeneral = false;
                    $scope.isConfig = false;
                    $scope.isInfo = false;
                }
                else if (tab == "Config") {
                    $scope.isGeneral = false;
                    $scope.isConfig = true;
                    $scope.isInfo = false;
                }
                else if(tab == "Info"){
                    $scope.isGeneral = false;
                    $scope.isConfig = false;
                    $scope.isInfo = true;
                }
                if($scope.oldCopy){
                    $scope.action = angular.copy($scope.oldCopy);
                    $scope.config = angular.copy($scope.oldCopy);
                }
            };
        };

        $scope.getPanelDirection = function () {
            return locale == 'ar' ? 'left' : 'right';
        }

        $http.get(resourceUrl + '/partials/panels/action-settings-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                resolve: {
                    requiredActions: function() {
                        return $scope.requiredActions;
                    },
                    autoSave: function() {
                        return $scope.autoSave;
                    }
                },
                controller: [
                    '$scope',
                    'requiredActions',
                    'autoSave',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                    console.log('panel was closed with result : 1', result);
                    setClickableNode(true);
                }).catch(function (error) {
                    // queryFlows(); 
                    console.log('panel was rejected with error : 2', error);
                    setClickableNode(true);
                });
        });

    };

    queryFlows();
});

module.controller('TemplatesCtrl', function ($scope, realm, $modal, Actions, $route, Notifications, Dialog, $location, Session, $translate) {
    $scope.searchKey = "";
    $scope.init = function () {
        $scope.loaderTypeName = 'authCardLoader';
        $scope.realm = realm;
        $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;
        $scope.first = 0;
        $scope.limit = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
        $scope.last = $scope.first + $scope.limit;
        $scope.perPage = 5;
        $scope.query = {
            realm: realm.realm,
        };
        $scope.searchQuery();
    };
    $scope.searchQuery = function () {
        $scope.searchLoaded = false;
        $scope.actions = Actions.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.updateSliceArray();
        });
    };
    $scope.selectQuery = function (limit) {
        $scope.limit = limit;
        Session.setItem("pageFilterCount", $scope.limit);
        $scope.first = 0;
        $scope.last = $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.firstPage = function () {
        $scope.first = 0;
        $scope.last = $scope.first + $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.next = function () {
        $scope.first += $scope.limit;
        $scope.last += $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.previous = function () {
        $scope.first -= $scope.limit;
        $scope.last -= $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.updateSliceArray = function () {
        $scope.slicedAction = $scope.actions.slice($scope.first, $scope.last);
    };

    $scope.chooseMarketplaceTemplate = function (realm) {
        var controller = function ($scope, $modalInstance, $location, Notifications, CreateInputModal, ImportWorkflow) {
            $scope.realm = realm;
            $scope.isSmallScreen = true;
            $scope.currentTab = 'templates';
            $scope.templateId = '';
            $scope.templateWorkflowName = '';
            $scope.templateImageURL = '';
            $scope.isRTL = () => locale == 'ar';
            $scope.isRootAccount = $scope.realm.realm === $scope.rootAccountName;
            $scope.isModalOpened = false;
            $scope.loading = false;
            $scope.isInstalled = false;
            $scope.template = {};

            $scope.init = function (template) {
                $scope.template = template;
            }

            $scope.viewTemplate = function (id, title, imageUrl) {
                $scope.templateId = id;
                $scope.currentTab = 'template-detail';
                $scope.templateWorkflowName = title;
                $scope.templateImageURL = imageUrl;
            }

            $scope.setCurrentTab = function (tab, title, imageUrl) {
                if(tab == 'templates'){
                    $scope.templateId = '';
                }
                $scope.currentTab = tab;
                $scope.templateWorkflowName = title;
                $scope.templateImageURL = imageUrl;
            }


            $scope.installMarketplace = function () {
                if(!$scope.isModalOpened) {
                    $scope.isModalOpened = true;
                    CreateInputModal.open($translate.instant('install-template'), '48', function (installTemplateName) {
                        $scope.loading = true;
                        $scope.isInstalled = true;
                        ImportWorkflow.create({realm : $scope.realm.realm, workflowName: installTemplateName }, JSON.parse($scope.template.workflowJson), function (response) {
                            Notifications.success($translate.instant("template-created"));
                            $location.url("/realms/" + $scope.realm.realm + '/authentication/templates/' + response.body);
                            $scope.loading = false;
                            $scope.isInstalled = false;
                        }, function (err) {
                            $scope.loading = false;
                            $scope.isInstalled = false;
                            if(err.data.errorMessage) {
                                Notifications.error($translate.instant(err.data.errorMessage));
                            }else {
                                Notifications.error($translate.instant('marketplace-template-installation-error'));
                            }
                        });
                    }, function () {
                        $scope.isModalOpened = false;
                    });
                }
            }

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        }
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-templates-basedOn-marketplace.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg',
            resolve: {
            }
        });
    }

    $scope.create = function () {
        var controller = function ($scope, $modalInstance, $location, Notifications, ImportWorkflow) {
            $scope.action = {
                actionType: "verification"
            };
            $scope.action.isSubWorkflow = false;
            $scope.isJsonFileSelected = false;
            $scope.isWorkflowJsonInputFilled = false;
            $scope.selectedJsonFile;
            $scope.JsonRawData;
            $scope.resourceUrl = resourceUrl;
            $scope.importWorkflowEnabled = false;
            var base64WorkflowFile;
            $scope.$watch('action', function () {
                $scope.changed = $scope.action.alias && $scope.action.alias.length > 0;
            }, true);
            function convertFileToBase64(file) {
                return new Promise((resolve, reject) => {
                  const reader = new FileReader();
                  reader.onload = () => resolve(reader.result);
                  reader.onerror = (error) => reject(error);
                  reader.readAsDataURL(file);
                });
            }
            function isValidJSON(str) {
                try {
                    JSON.parse(str);
                    return true;
                } catch (e) {
                    return false;
                }
            }
            $scope.ok = function () {
                if(document.getElementById("importWorkflowToggle").checked){
                    let workflowJson;
                    if($scope.isJsonFileSelected) {
                        workflowJson = { file : base64WorkflowFile };
                    }else if($scope.isWorkflowJsonInputFilled){
                        workflowJson = document.getElementById("jsonRawData").value;
                        if(!isValidJSON(workflowJson)){
                            Notifications.error($translate.instant("invalid-json-format"));
                            return;
                        }
                    }else {
                        Notifications.error($translate.instant("json-content-missing"));
                        return;
                    }
                    ImportWorkflow.create({realm : realm.realm, workflowName: $scope.action.alias }, workflowJson, function (response) {
                        Notifications.success($translate.instant("template-created"));
                        $location.url("/realms/" + realm.realm + '/authentication/templates/' + response.body);
                    });
                    $modalInstance.close();
                }else{
                    Actions.save({ realm: realm.realm }, $scope.action, function (response) {
                        Notifications.success($translate.instant("template-created"));
                        $location.url("/realms/" + realm.realm + '/authentication/templates/' + response.id);
                    });
                    $modalInstance.close();
                }
            };
            $scope.uploadWorkflowJsonFile = () => {
                const fileInputElement = document.getElementById('JsonFileInput');
                fileInputElement.click();
                if (!fileInputElement.uploadListenerAdded) {
                    fileInputElement.addEventListener('input', async (event) => {
                        $scope.selectedJsonFile = event.target.files[0];

                        if (!$scope.selectedJsonFile || ($scope.selectedJsonFile.type !== "application/json" && !$scope.selectedJsonFile.name.endsWith('.json'))) {
                            $scope.$apply(() => {
                                Notifications.error($translate.instant('invalid-file-format'));
                                $scope.selectedJsonFile = "";
                                fileInputElement.value = "";
                                $scope.isJsonFileSelected = false;
                            });
                            return;
                        }
                        try {
                            base64WorkflowFile = await convertFileToBase64($scope.selectedJsonFile);
                        } catch (error) {
                            $scope.$apply(() => {
                                Notifications.error($translate.instant('error-reading-file'));
                            });
                            return;
                        }
                        $scope.$apply(() => {
                            $scope.isJsonFileSelected = true;
                            $scope.isWorkflowJsonInputFilled = false;
                            $scope.JsonRawData = "";
                            document.getElementById('jsonRawData').value = "";
                        });
                    });
                    fileInputElement.uploadListenerAdded = true;
                }
            };

            $scope.pasteWorkflowJson = () => {
                $scope.isJsonFileSelected = false;
                $scope.selectedJsonFile = '';
                document.getElementById('JsonFileInput').value = "";
                $scope.isWorkflowJsonInputFilled = true;
            }
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $(document).ready(function () {
                setTimeout(function () {
                    $("#name").focus();
                }, 100);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-templates.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
            }
        });
    };
    $scope.delete = function (action) {
        Dialog.confirmDelete(action.alias, 'workflow', function () {
            Actions.remove({ realm: realm.realm, action: action.actionId }, function () {
                $route.reload();
                $location.url("/realms/" + realm.realm + '/authentication/templates');
                Notifications.success($translate.instant("template-deleted"));
            }, function (err) {
                Notifications.error(err.data.errorMessage);
            });
        });
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.showMore = function () {
        $scope.limit += $scope.perPage;
        setTimeout(() => {
            const lastInd = $scope.limit >= actions.length ? actions.length - 1 : $scope.limit - 1;
            const lastCard = document.querySelector(`#action-card-${lastInd}`);
            lastCard.scrollIntoView({ behavior: "smooth" });
        }, 100);
    };

    $scope.showLess = function () {
        $scope.limit -= $scope.perPage;
    };

    $scope.findActionsTab = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const action of $scope.actions) {
                if (action.alias.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };
    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});


module.controller('SubWorkflowsCtrl', function ($scope, realm, $modal, Actions, SubWorkflows, $route, Notifications, Dialog, $location, Session, $translate) {
    $scope.searchKey = "";
    $scope.init = function () {
        $scope.loaderTypeName = 'authCardLoader';
        $scope.realm = realm;
        $scope.first = 0;
        $scope.limit = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
        $scope.last = $scope.first + $scope.limit;
        $scope.perPage = 5;
        $scope.query = {
            realm: realm.realm,
        };
        $scope.searchQuery();
    };
    $scope.searchQuery = function () {
        $scope.searchLoaded = false;
        $scope.actions = SubWorkflows.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.updateSliceArray();
        });
    };
    $scope.selectQuery = function (limit) {
        $scope.limit = limit;
        Session.setItem("pageFilterCount", $scope.limit);
        $scope.first = 0;
        $scope.last = $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.firstPage = function () {
        $scope.first = 0;
        $scope.last = $scope.first + $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.next = function () {
        $scope.first += $scope.limit;
        $scope.last += $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.previous = function () {
        $scope.first -= $scope.limit;
        $scope.last -= $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.updateSliceArray = function () {
        $scope.slicedAction = $scope.actions.slice($scope.first, $scope.last);
    };



    $scope.create = function () {
        var controller = function ($scope, $modalInstance, $location, Notifications, ImportWorkflow, ImportSubWorkflow) {
            $scope.action = {
                actionType: "verification"
            };
            $scope.action.isSubWorkflow = true;
            $scope.isJsonFileSelected = false;
            $scope.isWorkflowJsonInputFilled = false;
            $scope.selectedJsonFile;
            $scope.JsonRawData;
            $scope.resourceUrl = resourceUrl;
            $scope.importWorkflowEnabled = false;
            var base64WorkflowFile;
            $scope.$watch('action', function () {
                $scope.changed = $scope.action.alias && $scope.action.alias.length > 0;
            }, true);
            function convertFileToBase64(file) {
                return new Promise((resolve, reject) => {
                  const reader = new FileReader();
                  reader.onload = () => resolve(reader.result);
                  reader.onerror = (error) => reject(error);
                  reader.readAsDataURL(file);
                });
            }
            function isValidJSON(str) {
                try {
                    JSON.parse(str);
                    return true;
                } catch (e) {
                    return false;
                }
            }
            $scope.ok = function () {
                if(document.getElementById("importWorkflowToggle").checked){
                    let workflowJson;
                    if($scope.isJsonFileSelected) {
                        workflowJson = { file : base64WorkflowFile };
                    }else if($scope.isWorkflowJsonInputFilled){
                        workflowJson = document.getElementById("jsonRawData").value;
                        if(!isValidJSON(workflowJson)){
                            Notifications.error($translate.instant("invalid-json-format"));
                            return;
                        }
                    }else {
                        Notifications.error($translate.instant("json-content-missing"));
                        return;
                    }
                    ImportSubWorkflow.create({realm : realm.realm, workflowName: $scope.action.alias, subWorkflow:'true' }, workflowJson, function (response) {
                        Notifications.success($translate.instant("sub-workflow-created"));
                        $location.url("/realms/" + realm.realm + '/authentication/sub-workflows/' + response.body);
                    });
                    $modalInstance.close();
                }else{
                    Actions.save({ realm: realm.realm }, $scope.action, function (response) {
                        Notifications.success($translate.instant("sub-workflow-created"));
                        $location.url("/realms/" + realm.realm + '/authentication/sub-workflows/' + response.id);
                    });
                    $modalInstance.close();
                }
            };
            $scope.uploadWorkflowJsonFile = () => {
                const fileInputElement = document.getElementById('JsonFileInput');
                fileInputElement.click();
                if (!fileInputElement.uploadListenerAdded) {
                    fileInputElement.addEventListener('input', async (event) => {
                        $scope.selectedJsonFile = event.target.files[0];

                        if (!$scope.selectedJsonFile || ($scope.selectedJsonFile.type !== "application/json" && !$scope.selectedJsonFile.name.endsWith('.json'))) {
                            $scope.$apply(() => {
                                Notifications.error($translate.instant('invalid-file-format'));
                                $scope.selectedJsonFile = "";
                                fileInputElement.value = "";
                                $scope.isJsonFileSelected = false;
                            });
                            return;
                        }
                        try {
                            base64WorkflowFile = await convertFileToBase64($scope.selectedJsonFile);
                        } catch (error) {
                            $scope.$apply(() => {
                                Notifications.error($translate.instant('error-reading-file'));
                            });
                            return;
                        }
                        $scope.$apply(() => {
                            $scope.isJsonFileSelected = true;
                            $scope.isWorkflowJsonInputFilled = false;
                            $scope.JsonRawData = "";
                            document.getElementById('jsonRawData').value = "";
                        });
                    });
                    fileInputElement.uploadListenerAdded = true;
                }
            };

            $scope.pasteWorkflowJson = () => {
                $scope.isJsonFileSelected = false;
                $scope.selectedJsonFile = '';
                document.getElementById('JsonFileInput').value = "";
                $scope.isWorkflowJsonInputFilled = true;
            }
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $(document).ready(function () {
                setTimeout(function () {
                    $("#name").focus();
                }, 100);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-sub-workflows.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
            }
        });
    };
    $scope.delete = function (action) {
        Dialog.confirmDelete(action.alias, 'sub-workflow', function () {
            Actions.remove({ realm: realm.realm, action: action.actionId }, function () {
                $route.reload();
                $location.url("/realms/" + realm.realm + '/authentication/sub-workflows');
                Notifications.success($translate.instant("sub-workflow-deleted"));
            }, function (err) {
                Notifications.error(err.data.errorMessage);
            });
        });
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.showMore = function () {
        $scope.limit += $scope.perPage;
        setTimeout(() => {
            const lastInd = $scope.limit >= actions.length ? actions.length - 1 : $scope.limit - 1;
            const lastCard = document.querySelector(`#action-card-${lastInd}`);
            lastCard.scrollIntoView({ behavior: "smooth" });
        }, 100);
    };

    $scope.showLess = function () {
        $scope.limit -= $scope.perPage;
    };

    $scope.findActionsTab = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const action of $scope.actions) {
                if (action.alias.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };
    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('CreateActionsCtrl', function ($scope, $http, realm, actionrep, unregisteredRequiredActions,
    $modal, $route, Actions, Dialog, $location, actionPolicies, cibaPolicies, ComponentUtils,
    RegisterRequiredAction, RequiredActions, RequiredActionsQuery, angularSlideOutPanel, ActionsPriority, Notifications, $translate, $q, passwordPolicies) {
    if (realm.mode == "bo") {
        $location.path('/notfound');
        return;
    }
    $scope.actionPolicies = actionPolicies;
    $scope.cibaPolicies = cibaPolicies;
    setTree();
    $scope.realm = realm;
    $scope.actionrep = actionrep;
    $scope.unregisteredRequiredActions = unregisteredRequiredActions;
    $scope.selectedAction = undefined;
    $scope.requiredActions = [];
    $scope.nodeClicked = false;
    $scope.passwordPolicies = passwordPolicies;

    $scope.findActions = function (actionsList, searchValue) {
        if (searchValue && searchValue.trim()) {
            for (var itr = 0; itr < actionsList.length; itr++) {
                if (actionsList[itr].name.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };


    // ************** Generate the tree diagram	 *****************
    var leftMargin =  $scope.isRTL() ? 320 : 120;
    var margin = { top: 60, right: 120, bottom: 20, left: leftMargin },
        width = 960 - margin.right - margin.left,
        height = 600 - margin.top - margin.bottom;

    var circle = { r: 35 };
    var rect = { h: 75, w: 250 };
    var circleTextPosition = 85;
    var depth = 120;
    var dragNodeName, dragNodeId;

    var i = 0;

    var dragStarted = false;
    var nodes, nodesExit, links, domNode, panTimer, selectedNode = null, draggingNode, relCoords;
    var panSpeed = 200;
    var panBoundary = 10;

    var tree, diagonal, svg, root, orginalRoot = [];

    // define the zoomListener which calls the zoom function on the "zoom" event constrained within the scaleExtents
    var zoomListener = d3.behavior.zoom().scaleExtent([0.1, 3]).on("zoom", zoom);

    var init = function () {

        d3.select("svg").remove();

        tree = d3.layout.tree()
            .size([height, width]);

        diagonal = d3.svg.diagonal()
            .projection(function (d) { return [d.x, d.y]; });

        svg = d3.select("#drawflow").append("svg")
            .attr("width", width + margin.right + margin.left)
            .attr("height", "400%")
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        root = $scope.treeData[0];
        orginalRoot = $scope.treeData[0];
        root.x0 = height / 2;
        root.y0 = 0;

        render();

        d3.selectAll(".link")
            .each(animate);
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

    var zoom = function () {
        svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    };

    var reiterateHierarchy = function () {
        if (selectedNode != null && draggingNode != null) {
            let flowUpdated = false;
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
            childCount(0, orginalRoot);
            var tempNodes = d3.layout.tree().nodes(orginalRoot);
            if (selectedNode.y0 < draggingNode.y0) {
                var selectedIndex = tempNodes.findIndex(x => x.name == selectedNode.name);
                var draggingIndex = tempNodes.findIndex(x => x.name == draggingNode.name);
                tempNodes.splice(selectedIndex + 1, 0, tempNodes[draggingIndex]);
                if (draggingIndex < selectedIndex) {
                    tempNodes.splice(draggingIndex, 1);
                }
                else {
                    tempNodes.splice(draggingIndex + 1, 1);
                }
                flowUpdated = true;
            }
            var sortedItems = [];

            tempNodes.forEach((element, index) => {
                var item = {
                    "name": element.name,
                    "isDefault": element.isDefault,
                    "isPlaceHolder": element.isPlaceHolder,
                    "nodeId": element.nodeId,
                    "providerId": element.providerId,
                    "isConfigurable": element.isConfigurable,
                    "hasConfig": element.hasConfig,
                    "requirement": element.requirement,
                    "priority": element.priority,
                    "parent": index == 0 ? "null" : tempNodes[index - 1].name,
                    "hasPolicy": element.hasPolicy,
                };

                if (index != (tempNodes.length - 1)) {
                    item["children"] = null;
                }
                sortedItems.push(item);
            });
            var reps = [];
            var index = 0;
            sortedItems.forEach(element => {
                if (!element.isDefault && !element.isPlaceHolder) {
                    var item = {
                        "actionId": element.nodeId,
                        "priority": index
                    };

                    reps.push(item);
                    index = index + 1;
                }
            });

            $scope.$evalAsync(function () {
                if (flowUpdated) {
                    ActionsPriority.save({ realm: realm.realm }, reps, function () {
                        Notifications.success($translate.instant("priority-of-execution-updated"));
                    });
                }
            });
            list_to_tree(sortedItems);

            return sortedItems;
        }
    };

    var list_to_tree = function (list) {
        var map = {}, node, roots = [], i;

        for (i = 0; i < list.length; i += 1) {
            map[list[i].name] = i; // initialize the map
            list[i].children = []; // initialize the children
        }

        for (i = 0; i < list.length; i += 1) {
            node = list[i];
            if (node.parent !== "null") {
                // if you have dangling branches check that map[node.parentId] exists
                list[map[node.parent]].children.push(node);
            } else {
                roots.push(node);
            }
        }
        return roots;
    };

    var overNode = function (d) {
        selectedNode = d;
        updateTempConnector();
    };

    var outNode = function (d) {
        selectedNode = null;
        updateTempConnector();
    };

    var updateTempConnector = function () {
        var data = [];
        if (draggingNode !== null && selectedNode !== null) {
            // have to flip the source coordinates since we did this for the existing connectors on the original tree
            data = [{
                source: {
                    x: (draggingNode.x0 + (100 / 2)),
                    y: (draggingNode.y0)
                },
                target: {
                    x: selectedNode.x0,
                    y: selectedNode.y0
                }
            }];
        }
        var link = svg.selectAll(".templink").data(data);

        link.enter().append("path")
            .attr("class", "templink")
            .attr("d", d3.svg.diagonal())
            .attr('pointer-events', 'none');

        link.attr("d", d3.svg.diagonal());

        link.exit().remove();
    };

    var pan = function (domNode, direction) {
        var speed = panSpeed;
        if (panTimer) {
            clearTimeout(panTimer);
            var translateCoords = d3.transform(svg.attr("transform"));
            if (direction == 'left' || direction == 'right') {
                translateX = translateCoords.translate[0];
                translateY = direction == 'left' ? translateCoords.translate[1] + speed : translateCoords.translate[1] - speed;
            } else if (direction == 'up' || direction == 'down') {
                translateX = direction == 'up' ? translateCoords.translate[1] - speed : translateCoords.translate[1] + speed;
                translateY = translateCoords.translate[0];
            }
            d3.select(domNode).select('g.node').attr("transform", "translate(" + translateX + "," + translateY + ")");
            zoomListener.scale(zoomListener.scale());
            zoomListener.translate([translateX, translateY]);
            panTimer = setTimeout(function () {
                pan(domNode, speed, direction);
            }, 50);
        }
    };

    var initiateDrag = function (d, domNode) {
        draggingNode = d;
        d3.select(domNode).select('.ghostRect').attr('pointer-events', 'none');
        d3.selectAll('.ghostRect').attr('class', 'ghostRect show');

        d3.select(domNode).select('.ghostCircle').attr('pointer-events', 'none');
        d3.selectAll('.ghostCircle').attr('class', 'ghostCircle show');
        d3.select(domNode).attr('class', 'node activeDrag');

        svg.selectAll("g.node").sort(function (a, b) { // select the parent and sort the path's
            if (a.id != draggingNode.id) return 1; // a is not the hovered element, send "a" to the back
            else return -1; // a is the hovered element, bring "a" to the front
        });

        dragStarted = null;
    };

    var endDrag = function () {
        selectedNode = null;
        d3.selectAll('.ghostCircle').attr('class', 'ghostCircle');
        d3.selectAll('.ghostRect').attr('class', 'ghostRect');
        d3.select(domNode).attr('class', 'node');
        // now restore the mouseover event or we won't be able to drag a 2nd time
        d3.select(domNode).select('.ghostCircle').attr('pointer-events', '');
        d3.select(domNode).select('.ghostRect').attr('pointer-events', '');
        updateTempConnector();
        if (draggingNode !== null) {
            init();
            draggingNode = null;
        }
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
        tree = tree.size([height, newWidth]).nodeSize([200, 60]);

        // Compute the new tree layout.
        nodes = tree.nodes(root).reverse();
        links = tree.links(nodes);

        // Normalize for fixed-depth.
        nodes.forEach(function (d) { d.y = d.depth * depth; });

        // Declare the nodes…
        var node = svg.selectAll("g.node")
            .data(nodes, function (d) { return d.id || (d.id = ++i); });

        var onNodeClick = function (event, d) {
            if (event.defaultPrevented || event.isDefault || event.isPlaceHolder || $scope.nodeClicked) return;
            $scope.nodeClicked = true;
            var selectedExec = $scope.requiredActions.find(x => x.id === event.nodeId);
            $scope.gotoSettings(selectedExec);
        };
        // Enter the nodes.
        var nodeEnter = node.enter().append("g")
            .attr("id", function (d) { return `node-${d.providerId ?? d.name.replaceAll(" ", "-")}`; })
            .call(d3.behavior.drag()
                .on("dragstart", function (d) {
                    if (d.isDefault || d.isPlaceHolder) {
                        return;
                    }
                    dragStarted = true;
                    nodes = tree.nodes(d);
                    d3.event.sourceEvent.stopPropagation();
                })
                .on("drag", function (d) {

                    if (d.isDefault || d.isPlaceHolder) {
                        return;
                    }

                    if (dragStarted) {
                        var domNode = this;
                        initiateDrag(d, domNode);
                    }

                    relCoords = d3.mouse($('svg').get(0));
                    if (relCoords[0] < panBoundary) {
                        panTimer = true;
                        pan(this, 'left');
                    } else if (relCoords[0] > ($('svg').width() - panBoundary)) {
                        panTimer = true;
                        pan(this, 'right');
                    } else if (relCoords[1] < panBoundary) {
                        panTimer = true;
                        pan(this, 'up');
                    } else if (relCoords[1] > ($('svg').height() - panBoundary)) {
                        panTimer = true;
                        pan(this, 'down');
                    } else {
                        try {
                            clearTimeout(panTimer);
                        } catch (e) {

                        }
                    }
                    d.x0 += d3.event.dx;
                    d.y0 += d3.event.dy;
                    var node = d3.select(this);
                    $(this).addClass('dragged');
                    node.attr("transform", "translate(" + d.x0 + "," + d.y0 + ")");
                    updateTempConnector();

                })
                .on("dragend", function (d) {
                    if (d.isDefault || d.isPlaceHolder) {
                        return;
                    }
                    domNode = this;
                    var isDragged = $(this).hasClass('dragged');
                    if (selectedNode) {
                        if (selectedNode.name != draggingNode.parent.name) {
                            var hierarchy = reiterateHierarchy();
                            if (hierarchy != null) {
                                $scope.treeData = [];
                                $scope.treeData.push(hierarchy[0]);
                            }
                        }
                        endDrag();
                    } else {
                        endDrag();
                    }
                    if (!isDragged) {
                        // onNodeClick(d, d.id - 1);
                    }
                }))
            .attr("class", "node")
            .attr("transform", function (d) {
                if (d.isDefault) {
                    return "translate(" + d.x + "," + d.y + ")";
                }
                else {
                    return "translate(" + (d.x - (rect.w / 2)) + "," + (d.y - (rect.h / 2)) + ")";
                }
            });

        // Draw Start and End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault;
        }).append("circle")
            .attr("r", circle.r)
            .style("fill", "#000");

        // Draw Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && !d.isPlaceHolder;
        }).append("rect")
            .attr('width', rect.w)
            .attr('height', rect.h)
            .attr("rx", 5)
            .style("fill", function (d) {
                if (!d.hasPolicy) {
                    if (d?.isEnabled) {
                        return notConfiguredNodeColor;
                    } else {
                        return disabledNodeColor;
                    }
                }
                if (d.isConfigurable && !d.hasConfig) {
                    if (d.requirement) {
                        return configurableNodeColor;
                    } else {
                        return disabledNodeColor;
                    }
                }
                if (d.isConfigurable && d.hasConfig) {
                    if (d.requirement) {
                        return enabledNodeColor;
                    } else {
                        return disabledNodeColor;
                    }
                }

                if (!d.isConfigurable) {
                    if (d.requirement) {
                        return enabledNodeColor;
                    } else {
                        return disabledNodeColor;
                    }
                }
            });

        // Draw Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && d.isPlaceHolder;
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
            return d.isDefault && d.parent == "null";
        }).append('svg:foreignObject')
            .attr("width", 30)
            .attr("height", 30)
            .attr("x", -15)
            .attr("y", -15)
            // .append("xhtml:body")
            .append("xhtml:div")
            .attr("class", "drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer")
            .append("xhtml:div")
            .append("xhtml:div")
            .attr("class", "d-flex justify-content-center text-center")
            .html('<i class="os-icon os-icon-user" style="color:#fff"></i>');

        // Draw Icon inside End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault && d.parent != "null";
        }).append('svg:foreignObject')
            .attr("width", 30)
            .attr("height", 30)
            .attr("x", -15)
            .attr("y", -15)
            .append("xhtml:div")
            .attr("class", "drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer")
            .append("xhtml:div")
            .append("xhtml:div")
            .attr("class", "d-flex justify-content-center text-center")
            .html('<i class="os-icon os-icon-check" style="color:#fff"></i>');


        // Draw Text Next to Start and End Circle
        nodeEnter.filter(function (d, i) {
            return d.isDefault;
        }).append("text")
            .attr("x", function (d) {
                if (d.parent == "null") {
                    return circleTextPosition;
                }
                else {
                    return 55;
                }
            })
            .attr("dy", ".35em")
            .attr("text-anchor", "middle")
            .text(function (d) { return $translate.instant(d.name); })
            .style("font-size", "14px")
            .style("font-weight", 400)
            .style("font-family", "Rubik");

        // Draw Text Inside Provider Rectangle
        nodeEnter.filter(function (d, i) {
            return !d.isDefault && !d.isPlaceHolder;
        }).append("svg:foreignObject")
            .attr("width", rect.w)
            .attr("height", rect.h)
            .attr("x", 0)
            .attr("y", 0)
            .html(function (d) {
                return "<div class='drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer'><div class='draw-activity-box'><div class='draw-activity-info d-flex align-items-center top-0 justify-content-center text-center'><span class='text-white'>" + $translate.instant(d.name) + "</span> </div></div></div>";
            });

        nodeEnter.filter(function (d, i) {
            return !d.isDefault && d.isPlaceHolder;
        }).append("svg:foreignObject")
            .attr("width", rect.w)
            .attr("height", rect.h)
            .attr("x", 0)
            .attr("y", 0)
            .html(function (d) {
                return "<div class='drawflow_content_node d-flex align-items-center justify-content-center h-100 cursor-pointer'><div class='draw-activity-box'><div class='draw-activity-info d-flex align-items-center top-0 justify-content-center text-center'><span class='c-light-gray'>" + $translate.instant(d.name) + "</span> </div></div></div>";
            });

        // Add temp Selection Ghost around Rectangle
        nodeEnter.append("rect")
            .filter(function (d, i) {
                return !d.isDefault && !d.isPlaceHolder;
            })
            .attr('class', 'ghostRect')
            .attr('width', rect.w * 2)
            .attr('height', rect.h * 3)
            .attr('x', -(rect.w * 4))
            .attr('y', -(rect.h))
            .attr("opacity", 0) // change this to zero to hide the target area
            .style("fill", "#047bf8")
            .attr('pointer-events', 'mouseover')
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

        nodeEnter.append("rect")
            .filter(function (d, i) {
                return !d.isDefault && d.isPlaceHolder;
            })
            .attr('class', 'dragRect')
            .attr('width', rect.w)
            .attr('height', rect.h)
            .attr("opacity", 0.2) // change this to zero to hide the target area
            .style("fill", "red")
            .attr('pointer-events', 'mouseover')
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

        // Add temp Selection Ghost around Circle
        nodeEnter.append("circle")
            .filter(function (d, i) {
                return d.isDefault && d.parent === "null";
            })
            .attr('class', 'ghostCircle')
            .attr('r', circle.r * 2)
            .attr("opacity", 0) // change this to zero to hide the target area
            .style("fill", "#047bf8")
            .attr('pointer-events', 'mouseover')
            .on("mouseover", function (node) {
                overNode(node);
            })
            .on("mouseout", function (node) {
                outNode(node);
            });

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

    $scope.dragComp = function (evt) {
        dragNodeName = evt.target.getAttribute("data-node");
        dragNodeId = evt.target.getAttribute("data-id");
        d3.select(domNode).select('.dragRect').attr('pointer-events', 'none');
        d3.selectAll('.dragRect').attr('class', 'dragRect show');
    };

    $scope.dropComp = function (evt) {
        evt.preventDefault();

        d3.select(domNode).select('.dragRect').attr('pointer-events', '');
        d3.selectAll('.dragRect').attr('class', 'dragRect');

        $scope.$evalAsync(function () {
            var copy = angular.copy($scope.unregisteredRequiredActions);
            $scope.unregisteredRequiredActions = copy.filter(function (el) {
                return el.providerId != dragNodeId;
            });
            var selected = copy.find(function (el) {
                return el.providerId === dragNodeId;
            });
            RegisterRequiredAction.save({ realm: realm.realm, action: $route.current.params.action }, selected, function () {
                Notifications.success($translate.instant("execution-created"));
                dragNodeName = null;
                dragNodeId = null;
                $route.reload();
            });
        });
    };

    $scope.dragEnd = function (evt) {
        evt.preventDefault();
        d3.select(domNode).select('.dragRect').attr('pointer-events', '');
        d3.selectAll('.dragRect').attr('class', 'dragRect');
    };

    $scope.allowDropComp = function (evt) {
        evt.preventDefault();
    };

    function setTree() {
        $scope.treeData = [
            {
                "name": $translate.instant('after-login'),
                "parent": "null",
                "isPlaceHolder": false,
                "isDefault": true,
                "children": [
                    {
                        "name": $translate.instant('drag-and-drop-actions'),
                        "parent": $translate.instant('after-login'),
                        "isPlaceHolder": true,
                        "isDefault": false,
                        "children": [{
                            "name": $translate.instant('end-flow'),
                            "isPlaceHolder": false,
                            "parent": $translate.instant('drag-and-drop-actions'),
                            "isDefault": true,
                        }]
                    }
                ]
            }
        ];
    }

    $scope.delete = function () {
        Dialog.confirmDelete($scope.actionrep.alias, 'action', function () {
            Actions.remove({ realm: realm.realm, action: $scope.actionrep.actionId }, function () {
                Notifications.success($translate.instant("action-deleted"));
                $location.url("/realms/" + realm.realm + '/authentication/actions');
            });
        });
    };
    $scope.checkHasPolicy = function (policyId) {
        for (const policy of $scope.actionPolicies.concat($scope.cibaPolicies)) {
            if (policyId === policy.id) {
                return true;
            }
        }
        return false;
    };

    var queryFlows = function () {
        RequiredActionsQuery.query({ realm: realm.realm, action: $route.current.params.action }, function (data) {
            $scope.requiredActions = [];
            for (var i = 0; i < data.length; i++) {
                $scope.requiredActions.push(data[i]);
            }
            if ($scope.requiredActions.length > 0) {
                setTree();
                orginalRoot = $scope.treeData[0];
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
                childCount(0, orginalRoot);
                var tempNodes = d3.layout.tree().nodes(orginalRoot);
                var sortedItems = [];

                $scope.requestArr = $scope.requiredActions.map(data => $http.get(authUrl + '/admin/realms/' + realm.realm + '/authentication/action-config-description/' + data.providerId));

                // fetching the config data of all the providers at once.
                $q.all($scope.requestArr).then(function (values) {

                    // this array contains the result that if a provider is configured or not.
                    let configRequirementDetailArr = values.map((currentValue, index) => {
                        let actionConfigured = true;
                        let actionData = currentValue.data;

                        if (actionData.configurable && actionData.properties) {
                            for (let itr = 0; itr < actionData.properties.length; itr++) {
                                if (actionData.properties[itr].required && ((!$scope.requiredActions[index]?.config?.[actionData.properties[itr].name]) || (actionData.properties[itr].type.toLowerCase() === 'list' && !actionData.properties[itr].options.includes($scope.requiredActions[index]?.config?.[actionData.properties[itr].name])))) {
                                    return false;
                                }
                            }
                        }
                        return actionConfigured;
                    });
                    $scope.requiredActions.forEach((element, index) => {
                        var newNode = {
                            "name": element.name,
                            "isDefault": false,
                            "isPlaceHolder": false,
                            "providerId": element.providerId,
                            "nodeId": element.id,
                            "isConfigurable": element.configurable,
                            "hasConfig": configRequirementDetailArr[index],
                            "requirement": element.enabled,
                            "priority": element.priority,
                            "hasPolicy": element.hasOwnProperty('policyId') && $scope.checkHasPolicy(element.policyId),
                            "isEnabled": element.enabled
                        };

                        tempNodes.splice(tempNodes.length - 2, 0, newNode);
                    });

                    tempNodes.forEach((element, index) => {
                        var item = {
                            "name": element.name,
                            "isDefault": element.isDefault,
                            "isPlaceHolder": element.isPlaceHolder,
                            "nodeId": element.nodeId,
                            "providerId": element.providerId,
                            "isConfigurable": element.isConfigurable,
                            "hasConfig": element.hasConfig,
                            "requirement": element.requirement,
                            "priority": element.priority,
                            "parent": index == 0 ? "null" : tempNodes[index - 1].name,
                            "hasPolicy": element.hasPolicy,
                            "isEnabled": element.isEnabled
                        };

                        if (index != (tempNodes.length - 1)) {
                            item["children"] = null;
                        }
                        sortedItems.push(item);
                    });

                    list_to_tree(sortedItems);

                    $scope.treeData = [];
                    $scope.treeData.push(sortedItems[0]);
                    init();
                });
            }
            else {
                init();
            }
        });
    };

    $scope.removePolicy = function (action) {
        action.policyId = null;
        RequiredActions.update({ realm: realm.realm, alias: action.alias }, action, function () {
            Notifications.success("Required action updated");
            setupRequiredActionsForm();
        });
    };

    $scope.gotoSettings = function (action) {
        //this controller loads action-settings-panel.html
        var controller = function ($scope, $location) {
            $scope.realm = realm;
            $scope.action = angular.copy(action);
            $scope.resourceUrl = resourceUrl;

            $scope.changed = false;
            $scope.genSaveEnabled = false;
            $scope.policySaveEnabled = false;
            $scope.attrSaveEnabled = false;
            $scope.configProps = {};
            $scope.isActionConfigurable = false;
            $scope.isConfigLoader = false;
            $scope.actionPolicies = actionPolicies;
            $scope.passwordPolicies = passwordPolicies;
            $scope.cibaPolicies = cibaPolicies;
            $scope.resourceUrl = resourceUrl;
            $scope.isGeneral = true;

            //Fetch required action config values & set the saved values insted of default values
            function getSavedConfigValues() {
                RequiredActions.get(
                    {
                      realm: realm.realm,
                      alias: $scope.action.id,
                    },
                    function (conresponse) {
                        //Set policyId in UI if hasPolicy
                        if(conresponse.hasPolicy){
                            $scope.action.selectedPolicy = conresponse.policyId;
                            $scope.oldCopy = angular.copy($scope.action);

                        }
                        if (conresponse != null && Object.keys(conresponse.config).length > 0) {
                            oldConfig = angular.copy(conresponse);
                            $scope.config = angular.copy(conresponse);
                        }
                        $scope.isConfigLoader = true;
                    },
                    function(error){
                        Notifications.error(error.data.errorMessage)
                    }
                  );
            }

            //Fetch required action config values & set the saved values insted of default values
            function getSavedConfigValues() {
                RequiredActions.get(
                    {
                        realm: realm.realm,
                        alias: $scope.action.id,
                    },
                    function (conresponse) {
                        //Set policyId in UI if hasPolicy
                        if (conresponse.hasPolicy) {
                            $scope.action.selectedPolicy = conresponse.policyId;
                            $scope.oldCopy = angular.copy($scope.action);
                        }
                        var defaultConfig = {};
                        if ($scope.configProps && Array.isArray($scope.configProps)) {
                            for (var i = 0; i < $scope.configProps.length; i++) {
                                var property = $scope.configProps[i];
                                if (property && property.name) {
                                    defaultConfig[property.name] = conresponse.config[property.name] ?? (property.type == 'List' ? null : property.type == 'boolean' ? 'false' : '');
                                }
                            }
                        }
                        $scope.oldCopy.config = defaultConfig;
                        $scope.config = JSON.parse(JSON.stringify($scope.oldCopy));
                        $scope.isConfigLoader = true;
                    },
                    function (error) {
                        Notifications.error(error.data.errorMessage);
                    }
                );
            }

            $scope.$watch('config', () => {
                const config = $scope.config.config;
                const oldConfig = $scope.oldCopy.config;
                if (!angular.equals($scope.config.config, $scope.oldCopy.config)) {
                    $scope.configProps.forEach((prop) => {
                        if (prop.dependsOn && (config[prop.name] !== oldConfig[prop.name])) {
                            let dependentCondionStatus = true;
                            Object.keys(prop.dependsOn).forEach((conditionKey) => {
                                if (config[conditionKey] !== prop.dependsOn[conditionKey].toString()) {
                                    dependentCondionStatus = false;
                                    return;
                                }
                            });
                            if (!dependentCondionStatus) {
                                config[prop.name] = oldConfig[prop.name];
                            }
                        }
                    });
                }
            }, true);

            //This API is used to fetch the config properties of the specified Required Action($scope.action.providerId)
            $scope.$evalAsync(function () {
                $http.get(authUrl + '/admin/realms/' + realm.realm + '/authentication/action-config-description/' + $scope.action.providerId)
                    .then(function (res) {
                        if (res.data.configurable) {
                            $scope.isActionConfigurable = true;
                            $scope.configProps = res.data.properties;
                        }
                        getSavedConfigValues();
                    })
                    .catch(err => console.log(err));
            });

            getSavedConfigValues()

            if ($scope.realm.mode == 'bo') {
                $scope.action.selectedPolicyType = "ciba";
            }
            else {
                $scope.action.selectedPolicyType = "action";
            }

             const defaultPasswordPolicy = passwordPolicies.find(policy => policy.policyName === 'default_password_policy');
             if(!$scope.action.passwordPolicy) {
                 $scope.action.passwordPolicy = defaultPasswordPolicy.id;
             } else {
                  const isPasswordPolicyPresent = passwordPolicies.filter(x => x.id === $scope.action.passwordPolicy);
                  if(isPasswordPolicyPresent.length === 0) {
                       $scope.action.passwordPolicy = defaultPasswordPolicy.id;
                  }
             }

            if ($scope.action.policyId != undefined) {
                if ($scope.action.selectedPolicy == "action") {
                    for (var i = 0; i < $scope.actionPolicies.length; i++) {
                        var id = $scope.actionPolicies[i].id;
                        if (id == $scope.action.policyId) {
                            $scope.action.selectedPolicy = $scope.actionPolicies[i].id;
                            $scope.action.selectedPolicyType = "action";
                            break;
                        }
                    }
                    for (var i = 0; i < $scope.passwordPolicies.length; i++) {
                        var id = $scope.actionPolicies[i].id;
                        if (id == $scope.action.policyId) {
                            $scope.action.selectedPolicy = $scope.passwordPolicies[i].id;
                            $scope.action.selectedPolicyType = "password";
                            break;
                        }
                    }
                }
                if ($scope.action.selectedPolicy == "ciba") {
                    for (var i = 0; i < $scope.cibaPolicies.length; i++) {
                        var id = $scope.cibaPolicies[i].id;
                        if (id == $scope.action.policyId) {
                            $scope.action.selectedPolicy = $scope.cibaPolicies[i].id;
                            $scope.action.selectedPolicyType = "ciba";
                            break;
                        }
                    }
                }
            }

            $scope.$watch('action', function () {
             if($scope.action.providerId == 'UPDATE_PASSWORD') {
                if ((!angular.equals($scope.action.enabled, $scope.oldCopy.enabled) || !angular.equals($scope.action.selectedPolicy, $scope.oldCopy.selectedPolicy) || !angular.equals($scope.action.passwordPolicy, $scope.oldCopy.passwordPolicy)) && $scope.action.selectedPolicy) {
                    $scope.genSaveEnabled = true;
                } else {
                    $scope.genSaveEnabled = false;
                }
             } else {
                if ((!angular.equals($scope.action.enabled, $scope.oldCopy.enabled) || !angular.equals($scope.action.selectedPolicy, $scope.oldCopy.selectedPolicy)) && $scope.action.selectedPolicy) {
                    $scope.genSaveEnabled = true;
                } else {
                    $scope.genSaveEnabled = false;
                }
             }
            }, true);

            //watch the Config Object in Action Configs
            $scope.$watch('config', function () {
                //check for changes in config properties
                if (!angular.equals($scope.oldCopy.config, $scope.config.config)) {
                    let configAttr = $scope.configProps;
                    for (let i = 0; i < configAttr.length; i++) {
                        // check for false condition
                        if (configAttr[i].required && ($scope.config.config[configAttr[i].name] === null || $scope.config.config[configAttr[i].name] === 'false' || $scope.config.config[configAttr[i].name].length === 0)) {
                            // check for property has dependsOn key
                            if (configAttr[i].dependsOn) {
                                let dependentCondionStatus = true;
                                Object.keys(configAttr[i].dependsOn).forEach((conditionKey) => {
                                    // check for dependent conditons and set condtion variable to false while any condition not met critiria
                                    if ($scope.config.config[conditionKey] !== configAttr[i].dependsOn[conditionKey].toString()) {
                                        dependentCondionStatus = false;
                                        return;
                                    }
                                });
                                if (dependentCondionStatus) {
                                    $scope.attrSaveEnabled = false;
                                    break;
                                } else {
                                    $scope.attrSaveEnabled = true;
                                }

                            } else {
                                $scope.attrSaveEnabled = false;
                                break;
                            }
                        } else {
                            $scope.attrSaveEnabled = true;
                        }
                    }
                } else {
                    $scope.attrSaveEnabled = false;
                }
            }, true);

            $scope.createPolicy = function () {
                $scope.dismissPanel();
                $location.url("/realms/" + realm.realm + '/policies');
            };

            $scope.removeRequiredAction = function () {
                Dialog.confirmDelete($scope.action.alias, 'action', function () {
                    RequiredActions.remove({ realm: realm.realm, alias: action.id }, function () {
                        $scope.$panelInstance.dismiss('this is from the controller!!');
                        Notifications.success($translate.instant("action-deleted"));
                        queryFlows();
                    });
                });
            };

            $scope.save = function () {
                var copy = angular.copy($scope.action);
                var configCopy = angular.copy($scope.config);
                ComponentUtils.convertAllListValuesToMultivaluedString($scope.configProps, configCopy.config);
                copy.config = configCopy.config;
                copy.policyId = $scope.action.selectedPolicy;
                copy.policyType = $scope.action.selectedPolicyType;
                delete copy.selectedPolicy;
                delete copy.selectedPolicyType;
                RequiredActions.update({
                    realm: realm.realm,
                    alias: action.id
                }, copy, function () {
                    $scope.$panelInstance.dismiss('this is from the controller!!');
                    Notifications.success($translate.instant("changesSavedSuccess"));
                });
            };

            $scope.dismissPanel = function () {
                $scope.$panelInstance.dismiss('this is from the controller!!');
            };
            $scope.$on('$routeChangeStart', function () {
                $scope.$panelInstance.dismiss();
            });

            $scope.reset = function () {
                $scope.config.config = angular.copy($scope.oldCopy.config);
                $scope.dismissPanel();
            };

            $scope.configLength = function () {
                if ($scope.action.config !== undefined)
                    return Object.keys($scope.action.config).length;
                return 0;
            };

            $scope.tabSelect = function (tab) {
                if (tab == "General") {
                    $scope.isGeneral = true;
                    $scope.isConfig = false;
                }
                else if (tab == "Policy") {
                    $scope.isGeneral = false;
                    $scope.isConfig = false;
                }
                else if (tab == "Config") {
                    $scope.isGeneral = false;
                    $scope.isConfig = true;
                }
            };
        };

        $http.get(resourceUrl + '/partials/panels/action-settings-panel.html').then(function (result) {
            var panelInstance2 = angularSlideOutPanel.open({
                template: result.data,
                openOn:  $scope.getPanelDirection(),
                backdrop: 'static',
                controller: [
                    '$scope',
                    '$location',
                    controller
                ]
            });
            panelInstance2.result
                .then(function (result) {
                    console.log('panel was closed with result : 3', result);
                }).catch(function (error) {
                    $route.reload();
                    console.log('panel was rejected with error : 4', error);
                });
        });

    };

    queryFlows();
});

module.controller('ActionsCtrl', function ($scope, realm, $modal, Actions, $route, Notifications, Dialog, $location, Session, $translate) {
    if (realm.mode == "bo") {
        $location.path('/notfound');
        return;
    }
    $scope.searchKey = "";

    $scope.init = function () {
        $scope.loaderTypeName = 'authCardLoader';
        $scope.realm = realm;
        $scope.first = 0;
        $scope.limit = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
        $scope.last = $scope.first + $scope.limit;
        $scope.perPage = 5;
        $scope.query = {
            realm: realm.realm,
        };
        $scope.searchQuery();
    };
    $scope.searchQuery = function () {
        $scope.searchLoaded = false;
        $scope.actions = Actions.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.updateSliceArray();
        });
    };
    $scope.selectQuery = function (limit) {
        $scope.limit = limit;
        Session.setItem("pageFilterCount", $scope.limit);
        $scope.first = 0;
        $scope.last = $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.firstPage = function () {
        $scope.first = 0;
        $scope.last = $scope.first + $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.next = function () {
        $scope.first += $scope.limit;
        $scope.last += $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.previous = function () {
        $scope.first -= $scope.limit;
        $scope.last -= $scope.limit;
        $scope.updateSliceArray();
    };
    $scope.updateSliceArray = function () {
        $scope.slicedAction = $scope.actions.slice($scope.first, $scope.last);
    };



    $scope.create = function () {
        var controller = function ($scope, $modalInstance, $location) {
            $scope.action = {};
            $scope.$watch('action', function () {
                $scope.changed = $scope.action.alias && $scope.action.alias.length > 0;
            }, true);
            $scope.ok = function () {
                $modalInstance.close();
                Actions.save({ realm: realm.realm }, $scope.action, function () {
                    Notifications.success($translate.instant("action-created"));
                    $route.reload();
                });
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $(document).ready(function () {
                setTimeout(function () {
                    $("#name").focus();
                }, 100);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-action.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
            }
        });
    };
    $scope.delete = function (action) {
        Dialog.confirmDelete(action.alias, 'action', function () {
            Actions.remove({ realm: realm.realm, action: action.actionId }, function () {
                $route.reload();
                $location.url("/realms/" + realm.realm + '/authentication/actions');
                Notifications.success($translate.instant("action-deleted"));
            }, function (err) {
                Notifications.error(err.data.errorMessage);
            });
        });
    };

    $scope.changeSearchKey = function (val) {
        $scope.searchKey = val;
    };

    $scope.showMore = function () {
        $scope.limit += $scope.perPage;
        setTimeout(() => {
            const lastInd = $scope.limit >= actions.length ? actions.length - 1 : $scope.limit - 1;
            const lastCard = document.querySelector(`#action-card-${lastInd}`);
            lastCard.scrollIntoView({ behavior: "smooth" });
        }, 100);
    };

    $scope.showLess = function () {
        $scope.limit -= $scope.perPage;
    };

    $scope.findActionsTab = function (searchValue) {
        if (searchValue && searchValue.trim()) {
            for (const action of $scope.actions) {
                if (action.alias.toLowerCase().includes(searchValue.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    };
    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.changeSearchKey($scope.search);
    };
});

module.controller('AuthenticationConfigCtrl', function ($scope, realm, flow, configType, config, AuthenticationConfig, Notifications, $translate,
    Dialog, $location, ComponentUtils) {
    $scope.realm = realm;
    $scope.flow = flow;
    $scope.configType = configType;
    $scope.create = false;
    $scope.config = angular.copy(config);
    $scope.changed = false;

    $scope.$watch(function () {
        return $location.path();
    }, function () {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('config', function () {
        if (!angular.equals($scope.config, config)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function () {
        var configCopy = angular.copy($scope.config);
        ComponentUtils.convertAllListValuesToMultivaluedString(configType.properties, configCopy.config);

        AuthenticationConfig.update({
            realm: realm.realm,
            config: config.id
        }, configCopy, function () {
            $scope.changed = false;
            config = angular.copy($scope.config);
            $location.url("/realms/" + realm.realm + '/authentication/flows/' + flow.id + '/config/' + configType.providerId + "/" + config.id);
            Notifications.success($translate.instant("changesSavedSuccess"));
        });
    };

    $scope.reset = function () {
        $scope.config = angular.copy(config);
        $scope.changed = false;
    };

    $scope.cancel = function () {
        //$location.url("/realms");
        window.history.back();
    };

    $scope.remove = function () {
        Dialog.confirmDelete($scope.config.alias, 'config', function () {
            AuthenticationConfig.remove({ realm: realm.realm, config: $scope.config.id }, function () {
                Notifications.success($translate.instant("config-deleted"));
                $location.url("/realms/" + realm.realm + '/authentication/flows/' + flow.id);
            });
        });
    };

});

module.controller('AuthenticationConfigCreateCtrl', function ($scope, realm, flow, configType, execution, AuthenticationExecutionConfig, $translate,
    Notifications, Dialog, $location, ComponentUtils) {
    $scope.realm = realm;
    $scope.flow = flow;
    $scope.create = true;
    $scope.configType = configType;

    var defaultConfig = {};
    if (configType && Array.isArray(configType.properties)) {
        for (var i = 0; i < configType.properties.length; i++) {
            var property = configType.properties[i];
            if (property && property.name) {
                defaultConfig[property.name] = property.defaultValue;
            }
        }
    }

    $scope.config = { config: defaultConfig };

    $scope.$watch(function () {
        return $location.path();
    }, function () {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.save = function () {
        var configCopy = angular.copy($scope.config);
        ComponentUtils.convertAllListValuesToMultivaluedString(configType.properties, configCopy.config);

        AuthenticationExecutionConfig.save({
            realm: realm.realm,
            execution: execution
        }, configCopy, function (data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            var url = "/realms/" + realm.realm + '/authentication/flows/' + flow.id + '/config/' + configType.providerId + "/" + id;
            $location.url(url);
            Notifications.success($translate.instant("config-created"));
        });
    };

    $scope.cancel = function () {
        //$location.url("/realms");
        window.history.back();
    };
});

module.controller('ClientInitialAccessCtrl', function ($scope, realm, clientInitialAccess, ClientInitialAccess, Dialog, Notifications, $route, $location, $translate) {
    $scope.realm = realm;
    $scope.clientInitialAccess = clientInitialAccess;

    $scope.remove = function (id) {
        Dialog.confirmDelete(id, 'initial access token', function () {
            ClientInitialAccess.remove({ realm: realm.realm, id: id }, function () {
                Notifications.success($translate.instant("initial-access-token-deleted"));
                $route.reload();
            });
        });
    };
});

module.controller('ClientInitialAccessCreateCtrl', function ($scope, realm, ClientInitialAccess, TimeUnit, Dialog, $location, $translate) {
    $scope.expirationUnit = 'Days';
    $scope.expiration = TimeUnit.toUnit(0, $scope.expirationUnit);
    $scope.count = 1;
    $scope.realm = realm;

    $scope.save = function () {
        var expiration = TimeUnit.toSeconds($scope.expiration, $scope.expirationUnit);
        ClientInitialAccess.save({
            realm: realm.realm
        }, { expiration: expiration, count: $scope.count }, function (data) {
            console.debug(data);
            $scope.id = data.id;
            $scope.token = data.token;
        });
    };

    $scope.cancel = function () {
        $location.url('/realms/' + realm.realm + '/client-registration/client-initial-access');
    };

    $scope.done = function () {
        var btns = {
            ok: {
                label: $translate.instant('continue'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        var title = $translate.instant('initial-access-token.confirm.title');
        var message = $translate.instant('initial-access-token.confirm.text');
        Dialog.open(title, message, btns, function () {
            $location.url('/realms/' + realm.realm + '/client-registration/client-initial-access');
        });
    };
});

module.controller('ClientRegPoliciesCtrl', function ($scope, realm, clientRegistrationPolicyProviders, policies, Dialog, Notifications, Components, $route, $location, $translate) {
    $scope.realm = realm;
    $scope.providers = clientRegistrationPolicyProviders;
    $scope.anonPolicies = [];
    $scope.authPolicies = [];
    for (var i = 0; i < policies.length; i++) {
        var policy = policies[i];
        if (policy.subType === 'anonymous') {
            $scope.anonPolicies.push(policy);
        } else if (policy.subType === 'authenticated') {
            $scope.authPolicies.push(policy);
        } else {
            throw 'subType is required for clientRegistration policy component!';
        }
    }

    $scope.addProvider = function (authType, provider) {
        $location.url("/realms/" + realm.realm + "/client-registration/client-reg-policies/create/" + authType + '/' + provider.id);
    };

    $scope.getInstanceLink = function (instance) {
        return "/realms/" + realm.realm + "/client-registration/client-reg-policies/" + instance.providerId + "/" + instance.id;
    };

    $scope.removeInstance = function (instance) {
        Dialog.confirmDelete(instance.name, 'client registration policy', function () {
            Components.remove({
                realm: realm.realm,
                componentId: instance.id
            }, function () {
                $route.reload();
                Notifications.success($translate.instant("policy-deleted"));
            });
        });
    };

});

module.controller('ClientRegPolicyDetailCtrl', function ($scope, realm, clientRegistrationPolicyProviders, instance, Dialog, Notifications, Components, ComponentUtils, $route, $location, $translate) {
    $scope.realm = realm;
    $scope.instance = instance;
    $scope.providerTypes = clientRegistrationPolicyProviders;

    for (var i = 0; i < $scope.providerTypes.length; i++) {
        var providerType = $scope.providerTypes[i];
        if (providerType.id === instance.providerId) {
            $scope.providerType = providerType;
            break;
        }
    }

    $scope.create = !$scope.instance.name;

    function toDefaultValue(configProperty) {
        if (configProperty.type === 'MultivaluedString' || configProperty.type === 'MultivaluedList') {
            if (configProperty.defaultValue) {
                return configProperty.defaultValue;
            } else {
                return [];
            }
        }

        if (configProperty.defaultValue) {
            return [configProperty.defaultValue];
        } else {
            return [''];
        }
    }

    if ($scope.create) {
        $scope.instance.name = $scope.instance.providerId;
        $scope.instance.parentId = realm.id;
        $scope.instance.config = {};

        if ($scope.providerType.properties) {

            for (var i = 0; i < $scope.providerType.properties.length; i++) {
                var configProperty = $scope.providerType.properties[i];
                $scope.instance.config[configProperty.name] = toDefaultValue(configProperty);
            }
        }
    }

    if ($scope.providerType.properties) {
        ComponentUtils.addLastEmptyValueToMultivaluedLists($scope.providerType.properties, $scope.instance.config);
        ComponentUtils.addMvOptionsToMultivaluedLists($scope.providerType.properties);
    }

    var oldCopy = angular.copy($scope.instance);
    $scope.changed = false;

    $scope.$watch('instance', function () {
        if (!angular.equals($scope.instance, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.reset = function () {
        $route.reload();
    };

    $scope.save = function () {
        $scope.changed = false;
        if ($scope.create) {
            Components.save({ realm: realm.realm }, $scope.instance, function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/client-registration/client-reg-policies/" + $scope.instance.providerId + "/" + id);
                Notifications.success($translate.instant("policy-created"));
            });
        } else {
            Components.update({
                realm: realm.realm,
                componentId: instance.id
            },
                $scope.instance, function () {
                    $route.reload();
                    Notifications.success($translate.instant("policy-updated"));
                });
        }
    };

});

module.controller('RealmImportCtrl', function ($scope, realm, $route,
    Notifications, $modal, $resource) {
    $scope.rawContent = {};
    $scope.fileContent = {
        enabled: true
    };
    $scope.changed = false;
    $scope.files = [];
    $scope.realm = realm;
    $scope.overwrite = false;
    $scope.skip = false;
    $scope.importUsers = false;
    $scope.importGroups = false;
    $scope.importClients = false;
    $scope.importIdentityProviders = false;
    $scope.importRealmRoles = false;
    $scope.importClientRoles = false;
    $scope.ifResourceExists = 'FAIL';
    $scope.isMultiRealm = false;
    $scope.results = {};
    $scope.currentPage = 0;
    var pageSize = 15;

    var oldCopy = angular.copy($scope.fileContent);

    $scope.importFile = function ($fileContent) {
        var parsed;
        try {
            parsed = JSON.parse($fileContent);
        } catch (e) {
            Notifications.error($translate.instant('json-not-parsed-error'));
            return;
        }

        $scope.rawContent = angular.copy(parsed);
        if (($scope.rawContent instanceof Array) && ($scope.rawContent.length > 0)) {
            if ($scope.rawContent.length > 1) $scope.isMultiRealm = true;
            $scope.fileContent = $scope.rawContent[0];
        } else {
            $scope.fileContent = $scope.rawContent;
        }

        $scope.importing = true;
        setOnOffSwitchDefaults();
        $scope.results = {};
        if (!$scope.hasResources()) {
            $scope.nothingToImport();
        }
    };

    $scope.hasResults = function () {
        return (Object.keys($scope.results).length > 0) &&
            ($scope.results.results !== undefined) &&
            ($scope.results.results.length > 0);
    };

    $scope.resultsPage = function () {
        if (!$scope.hasResults()) return {};
        return $scope.results.results.slice(startIndex(), endIndex());
    };

    function startIndex() {
        return pageSize * $scope.currentPage;
    }

    function endIndex() {
        var length = $scope.results.results.length;
        var endIndex = startIndex() + pageSize;
        if (endIndex > length) endIndex = length;
        return endIndex;
    }

    function setOnOffSwitchDefaults() {
        $scope.importUsers = $scope.hasArray('users');
        $scope.importGroups = $scope.hasArray('groups');
        $scope.importClients = $scope.hasArray('clients');
        $scope.importIdentityProviders = $scope.hasArray('identityProviders');
        $scope.importRealmRoles = $scope.hasRealmRoles();
        $scope.importClientRoles = $scope.hasClientRoles();
    }

    $scope.setFirstPage = function () {
        $scope.currentPage = 0;
    };

    $scope.setNextPage = function () {
        $scope.currentPage++;
    };

    $scope.setPreviousPage = function () {
        $scope.currentPage--;
    };

    $scope.hasNext = function () {
        if (!$scope.hasResults()) return false;
        var length = $scope.results.results.length;
        var endIndex = startIndex() + pageSize;
        return length > endIndex;
    };

    $scope.hasPrevious = function () {
        if (!$scope.hasResults()) return false;
        return $scope.currentPage > 0;
    };

    $scope.viewImportDetails = function () {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/view-object.html',
            controller: 'ObjectModalCtrl',
            backdrop: 'static',
            resolve: {
                object: function () {
                    return $scope.fileContent;
                }
            }
        });
    };

    $scope.hasArray = function (section) {
        return ($scope.fileContent !== 'undefined') &&
            ($scope.fileContent.hasOwnProperty(section)) &&
            ($scope.fileContent[section] instanceof Array) &&
            ($scope.fileContent[section].length > 0);
    };

    $scope.hasRealmRoles = function () {
        return $scope.hasRoles() &&
            ($scope.fileContent.roles.hasOwnProperty('realm')) &&
            ($scope.fileContent.roles.realm instanceof Array) &&
            ($scope.fileContent.roles.realm.length > 0);
    };

    $scope.hasRoles = function () {
        return ($scope.fileContent !== 'undefined') &&
            ($scope.fileContent.hasOwnProperty('roles')) &&
            ($scope.fileContent.roles !== 'undefined');
    };

    $scope.hasClientRoles = function () {
        return $scope.hasRoles() &&
            ($scope.fileContent.roles.hasOwnProperty('client')) &&
            (Object.keys($scope.fileContent.roles.client).length > 0);
    };

    $scope.itemCount = function (section) {
        if (!$scope.importing) return 0;
        if ($scope.hasRealmRoles() && (section === 'roles.realm')) return $scope.fileContent.roles.realm.length;
        if ($scope.hasClientRoles() && (section === 'roles.client')) return clientRolesCount($scope.fileContent.roles.client);

        if (!$scope.fileContent.hasOwnProperty(section)) return 0;

        return $scope.fileContent[section].length;
    };

    clientRolesCount = function (clientRoles) {
        var total = 0;
        for (var clientName in clientRoles) {
            total += clientRoles[clientName].length;
        }
        return total;
    };

    $scope.hasResources = function () {
        return ($scope.importUsers && $scope.hasArray('users')) ||
            ($scope.importGroups && $scope.hasArray('groups')) ||
            ($scope.importClients && $scope.hasArray('clients')) ||
            ($scope.importIdentityProviders && $scope.hasArray('identityProviders')) ||
            ($scope.importRealmRoles && $scope.hasRealmRoles()) ||
            ($scope.importClientRoles && $scope.hasClientRoles());
    };

    $scope.nothingToImport = function () {
        Notifications.error($translate.instant('no-resources-spec-import-error'));
    };

    $scope.$watch('fileContent', function () {
        if (!angular.equals($scope.fileContent, oldCopy)) {
            $scope.changed = true;
        }
        setOnOffSwitchDefaults();
    }, true);

    $scope.successMessage = function () {
        var message = $scope.results.added + ' records added. ';
        if ($scope.ifResourceExists === 'SKIP') {
            message += $scope.results.skipped + ' records skipped.';
        }
        if ($scope.ifResourceExists === 'OVERWRITE') {
            message += $scope.results.overwritten + ' records overwritten.';
        }
        return message;
    };

    $scope.save = function () {
        var json = angular.copy($scope.fileContent);
        json.ifResourceExists = $scope.ifResourceExists;
        if (!$scope.importUsers) delete json.users;
        if (!$scope.importGroups) delete json.groups;
        if (!$scope.importIdentityProviders) delete json.identityProviders;
        if (!$scope.importClients) delete json.clients;

        if (json.hasOwnProperty('roles')) {
            if (!$scope.importRealmRoles) delete json.roles.realm;
            if (!$scope.importClientRoles) delete json.roles.client;
        }

        var importFile = $resource(authUrl + '/admin/realms/' + realm.realm + '/partialImport');
        $scope.results = importFile.save(json, function () {
            Notifications.success($scope.successMessage());
        }, function (error) {
            if (error.data.errorMessage) {
                Notifications.error(error.data.errorMessage);
            } else {
                Notifications.error($translate.instant('unexpected-during-import-error'));
            }
        });
    };

    $scope.reset = function () {
        $route.reload();
    };

});

module.controller('RealmExportCtrl', function ($scope, realm, $http,
    $httpParamSerializer, Notifications, Dialog) {
    $scope.realm = realm;
    $scope.exportGroupsAndRoles = false;
    $scope.exportClients = false;

    $scope.export = function () {
        if ($scope.exportGroupsAndRoles || $scope.exportClients) {
            Dialog.confirm('Export', 'This operation may make server unresponsive for a while.\n\nAre you sure you want to proceed?', download);
        } else {
            download();
        }
    };

    function download() {
        var exportUrl = authUrl + '/admin/realms/' + realm.realm + '/partial-export';
        var params = {};
        if ($scope.exportGroupsAndRoles) {
            params['exportGroupsAndRoles'] = true;
        }
        if ($scope.exportClients) {
            params['exportClients'] = true;
        }
        if (Object.keys(params).length > 0) {
            exportUrl += '?' + $httpParamSerializer(params);
        }
        $http.post(exportUrl)
            .then(function (response) {
                var download = angular.fromJson(response.data);
                download = angular.toJson(download, true);
                saveAs(new Blob([download], { type: 'application/json' }), 'realm-export.json');
            }).catch(function () {
                Notifications.error($translate.instant("sorry-something-went-wrong-error"));
            });
    }
});

module.controller('RealmAuthenticationSettings', function ($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, ConfirmExitDialog, $location, TimeUnit2, $translate, onboardingFlows, loginFlows, recoveryFlows, actions, actionPolicies,
    cibaPolicies, directGrantFlows, actions) {
    $scope.serverInfo = serverInfo;
    $scope.$watch('realm.loginWithEmailAllowed', function () {
        if ($scope.realm.loginWithEmailAllowed) {
            $scope.realm.duplicateEmailsAllowed = false;
        }
    });

    $scope.toTemplate = (type, id, alias) => {
        var link = "";
        switch (type) {
            case "mfa":
                link = `/realms/${$scope.realm.realm}/connections/mfa/settings/${id}`;
                break;
            case "social":
                link = `/realms/${$scope.realm.realm}/connections/social/settings/${id}/${alias}`;
                break;
            case "action":
                link = `/realms/${$scope.realm.realm}/authentication/actions/${realm.authenticationFlowBindingOverrides[type]}`;
                break;
            case "policy":
                link = `/realms/${$scope.realm.realm}/policies/action/${id}`;
                break;
            default:
                link = `/realms/${$scope.realm.realm}/authentication/flows/${realm.authenticationFlowBindingOverrides[type]}/${type}/flow`;
                break;
        }
        $location.path(link);
    }

    $scope.toggleSideBarShow = function () {
        var sidebarMenu = document.getElementById('setting-sidebar');
        var sidebarIcon = document.getElementById('setting-sidebar-icon');
        if(sidebarMenu.classList.contains('bo-setting-sidemenu')){
            sidebarMenu.classList.remove("bo-setting-sidemenu");
            sidebarMenu.classList.add("bo-setting-sidemenu-show");
            sidebarIcon.classList.add('d-none');
            sidebarIcon.classList.remove('bo-setting-sidemenu-icon');
            $scope.isMobileWView = true;
        }else{
            sidebarMenu.classList.add("bo-setting-sidemenu");
            sidebarMenu.classList.remove("bo-setting-sidemenu-show");
            sidebarIcon.classList.remove('d-none');
            sidebarIcon.classList.add('bo-setting-sidemenu-icon');
            $scope.isMobileWView = false;
        }
    }
    genericRealmUpdate($scope, Current, Realm, realm, serverInfo, $http, $route, Dialog, Notifications, "/realms/" + realm.realm + "/authentication-settings", ConfirmExitDialog, $location, TimeUnit2, $translate, onboardingFlows, loginFlows, recoveryFlows, actions, cibaPolicies, actionPolicies, directGrantFlows);
});

module.controller('TestModeCtrl', function ($scope, realm, Realm, serverInfo, $route, Notifications, $translate) {
    $scope.realm = angular.copy(realm);
    $scope.disableMfa = $scope.realm.attributes?.disableMfa === 'true' ? true : false;
    const oldCopy = $scope.disableMfa;
    $scope.serverInfo = serverInfo;
    $scope.isLoading = false;
    $scope.isChanged = false;

    $scope.makeSideMenuClosed();

    $scope.reset = function () {
        $scope.disableMfa = oldCopy;
        $scope.isChanged = false;
    }

    $scope.$watch('disableMfa', function () {
        if (!angular.equals($scope.disableMfa, oldCopy)) {
            $scope.isChanged = true;
        } else {
            $scope.isChanged = false;
        }
    }, true);

    $scope.update = function () {
        $scope.isLoading = true;
        $scope.realm.attributes.disableMfa = $scope.disableMfa;
        Realm.update($scope.realm, function () {
            $route.reload();
            Notifications.success($translate.instant("changes-saved-to-realm"));
        }, function () {
            $route.reload();
            Notifications.error($translate.instant("something-went-wrong-error"));
        });
    }

});


module.controller('kcFileUploadCtrl', function ($scope, Notifications, $translate) {

    $scope.file = null;
    $scope.files = [];
    $scope.type = "application/pdf";
    $scope.uploadType = "single";
    $scope.isNewFile = false;
    $scope.isImage = false;


    $scope.clickFileInput = () => {
        $('#fileUpload').click();
    }

    $scope.getFileDetails = () => ({fileName, updatedAt})

    const checkType = (file) => {
        return ($scope.uploadType === 'multi' || ($scope.option.type === "ImageFile" && $scope.uploadType === 'single')) && $scope.type.split('/')[0] !== file.type.split('/')[0]
    }


    const handleInputUpload = (uploader) => {
        if (uploader.files && uploader.files[0]) {
            if (checkType(uploader.files[0])) {
                $scope.$apply(function () {
                    Notifications.error($translate.instant("invalid-file-type"));
                })
                return;
            }
            if ($scope.uploadType === 'multi') {
                $scope.setFiles(uploader.files[0])
                return
            }
            $scope.setFile(uploader.files[0])
        }
    }

    $scope.$watch("name", function() {
        const name = JSON.parse($scope.name)
        if (name.type === 'ImageFile' && $scope.option.name !== "custom-message-logo") {
            if($scope.option.name === "terms_and_condition_image") {
                $scope.type = "image/png, image/jpg, image/jpeg"
            } else {
                $scope.type = "image/*"
            }
            $scope.uploadType = 'multi';
        } else if (name.type === 'PDFFile'){
            $scope.type = ".pdf"
            $scope.uploadType = 'single';
            $scope.isImage = false;
        } else {
            $scope.type = "image/*"
            $scope.uploadType = 'single';
            $scope.isImage = true;
        }
        if ($scope.files.length === 0) {
            $scope.file = $scope.$parent.config[name.name];
            if(!isBase64($scope.file) && !isValidURL($scope.file)){
                fileName = "";
            }
        }
    }, true)

    $scope.setFile = async (file)=>{
        if($scope.isFileSizeExceed(file.size, 5)){
            $scope.$apply(function () {
                Notifications.error($translate.instant("terms-and-condtion-file-limit-exceed"));
            })
            return ;
        }else{
            $scope.file = await getBase64(file);
            fileName = file.name;
            const today = new Date()
            updatedAt = today.getDate()+'-'+(today.getMonth()+1)+'-'+today.getFullYear();
            $scope.$parent.config[JSON.parse($scope.name).name] = file.name + ':' + $scope.file.split("base64,")[1];
            $scope.isNewFile = true;
            $scope.$apply();
        }
    }

    $scope.setFiles = (file) => {
        if ($scope.files.length >= 5) {
            $scope.$apply(function () {
                Notifications.error($translate.instant("only-five-files-allowed"));
            })
            return;
        }
      
        let totalFilesize = $scope.files.reduce((total, f) => total + f.size, 0);
        if ($scope.isFileSizeExceed(totalFilesize + file.size, 5)) {
            $scope.$apply(function () {
                Notifications.error($translate.instant("terms-and-condtion-file-limit-exceed"));
            })
            return;
        }

        $scope.files.push(file);
        updateFile(true);
    }

    $scope.deleteFile = (index) => {
        $scope.files.splice(index, 1);
        updateFile(false);
    }

    const updateFile = (apply) => {
        $scope.$parent.config[JSON.parse($scope.name).name] = "";
        $scope.files.map(async f => {
            const base64 = await getBase64(f)
            if ($scope.$parent.config[JSON.parse($scope.name).name].length > 0) $scope.$parent.config[JSON.parse($scope.name).name] += ',' + base64.split("base64,")[1];
            else $scope.$parent.config[JSON.parse($scope.name).name] = base64.split("base64,")[1];
        })
        if (apply)
        $scope.$apply();
    }

    $scope.removeFile = () => {
        $scope.file = null;
    }

    $scope.initFileUpload = () => {
        const dropZone = document.querySelector("#dropZone");
        dropZone.addEventListener("dragover", (e) => {
            e.preventDefault();
        });

        dropZone.addEventListener("drop", (e) => {
           $scope.dropHandler(e);
        });

        $("#fileUpload").change(function () {
            handleInputUpload(this)
        });
    };

    // maxLimit in MegaBytes
    $scope.isFileSizeExceed = (fileSize, maxLimit) => {
        let sizeRoundOf = Math.ceil(fileSize / (1024 * 1204));
        if(sizeRoundOf > maxLimit){
            return true;
        }else{
            return false;
        }
    }

    $scope.dropHandler = (ev) => {
        ev.preventDefault();
        if (ev.dataTransfer.items) {
        if (ev.dataTransfer.items.length > 1 && $scope.uploadType === "single") {
            $scope.$apply(function () {
                Notifications.error($translate.instant("multiple-files-not-allowed"));
            });
            return;
        };
        [...ev.dataTransfer.items].forEach((item, i) => {
            if (item.kind === "file") {
                const file = item.getAsFile();
                if (checkType(file)) {
                    $scope.$apply(function () {
                        Notifications.error($translate.instant("invalid-file-type"));
                    });
                } else {
                    if ($scope.uploadType === 'multi') {
                        $scope.setFiles(file)
                    } else {
                        $scope.setFile(file)
                    }
                }
            }
        });
        }
    }
});

module.controller('APIListCtrl', function ($scope, ApiVerifiers, ApiCategories, $translate, realm, Client, APICurlTest, ApiConfig, FetchClientSecret, $modal, $route) {
    $scope.realm = angular.copy(realm);
    $scope.isLoading = true;
    $scope.apiArray = [];
    $scope.openedTag = ""; 
    $scope.apiCategories = ["all"]
    $scope.supportedLanguages=[]
    $scope.apps = [];
    $scope.clientQuery = {};
    $scope.query = {};
    $scope.query.sort = "ASC";
    $scope.query.enabled = "false";
    $scope.query.first = 0;
    $scope.query.last = 9;
    $scope.query.max = 9;
    $scope.query.category = null;
    $scope.apiSearchName = ""
    $scope.showViewMore = true;
    $scope.apiCount = 0;
    $scope.maxResults = 0
    $scope.apiRequestArray = [];
    $scope.basicAuth = "";
    $scope.selectedApp = null;
    $scope.tooltipTxt = $translate.instant("copy");
    $scope.loaderType = "apiCardLoader";
    $scope.isLoading = true;

    function getKeyByValue(value) {
        return Object.keys(resourceBundle).find(key => resourceBundle[key]?.toLowerCase() === value?.toLowerCase());
    }

    function fetchApis() {
        $scope.isLoading = true
        ApiVerifiers.query({realm: $scope.realm.id, ...$scope.query}, function(data) {
            $scope.isLoading = false;
            if( Array.isArray(data?.verifiers)) {
                $scope.apiArray = [...data?.verifiers];
            } else {
                $scope.apiArray = []
            }
            $scope.apiCount = data.count;
            $scope.maxResults = data.maxResultSize;
        }, function(err) {
            $scope.loading = false;
        });
    }


    function fetchCategories () {
        ApiCategories.query({realm: $scope.realm.id}, function(data) {
            data.forEach((value => {
                if (value)
                    $scope.apiCategories.push(value)
            }))
        }, function(err) {
            $scope.loading = false;
        });
    }

    function fetchRequestDetails() {
        ApiConfig.query({realm: $scope.realm.id, ...{}}, function(data) {
            $scope.isLoading = false;
            $scope.apiRequestArray = data;
        }, function(err) {
            $scope.isLoading = false;
        });
    }

    function fetchClientSecret () {
        if (!$scope.selectedApp) return;
        FetchClientSecret.query({ realm: $scope.realm.realm, client: $scope.selectedApp }, async function(data) {
            const value = data.value;
            $scope.basicAuth = btoa(`${$scope.selectedApp}:${value}`);
        }, function (err) {
            console.err(err.message);
        })
    }

    function fetchApps() {
        $scope.clientQuery.sort = "ASC";
        $scope.clientQuery.realm = $scope.realm.realm;
        $scope.clientQuery.sandboxMode = false;
        $scope.clientQuery.isApiClient = true;
        $scope.clientQuery.orderBy = 'last-updated'
        $scope.orderBy = $translate.instant($scope.clientQuery.orderBy);
        $scope.appCount = 0;
        Client.query($scope.clientQuery, function (data) {
            $scope.apps = data;
            $scope.isLoading = false;
            $scope.selectedApp = data[0].clientId;
            fetchClientSecret();
        });
    }

    fetchApis();
    fetchApps();
    fetchRequestDetails();
    fetchCategories()

    $scope.searchApi = () => {
        $scope.query.search = getKeyByValue($scope.apiSearchName) ? getKeyByValue($scope.apiSearchName) : $scope.apiSearchName.replace(/\s+/g, '_').toLowerCase();
        $scope.query.max = 9;
        $scope.query.last = 9;
        fetchApis();
    }

    $scope.$watch("selectedApp", function() {
        fetchClientSecret();
    })

    $scope.openCurl = (name)=> {
        const requestDetails = $scope.apiRequestArray.find(config => config.verifierName === name)
        $scope.modalData = {
            selectedLanguage: "curl"
        }
        APICurlTest.open($scope.realm, requestDetails, $scope.basicAuth, $scope.selectedApp)
    }

    $scope.viewMore = () => {
        $scope.query.max += 18;
        $scope.query.last += 18;
        fetchApis();
        $scope.showViewMore = false
    }

    function isInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= document.getElementById("card-holder").getBoundingClientRect().bottom
        );
    }

    // Check on scroll
    document.getElementById("card-holder").addEventListener('scroll', () => {
        const element = document.querySelector(`[data-element="element-${$scope.maxResults - 1}"]`);
        if (!element) return;
        if (isInViewport(element) && $scope.apiCount != $scope.apiArray.length) {
            $scope.showViewMore = true;
        } else {
            $scope.showViewMore = false;
         }
        $scope.$apply();
    });

    $scope.selectedCategory = $scope.apiCategories[0];
    $scope.selectedApi = null;

    $scope.handleCategoryChange = (category) => {
        $scope.selectedCategory = category
        $scope.query.max = 9;
        $scope.query.last = 9;
        if (category === "all") {
            $scope.query.category = null;
        } else {
            $scope.query.category = category
        }
        fetchApis()
        $scope.showViewMore = true;
    }

    $scope.createApp = function () {
        var controller = function ($scope, $modalInstance, Client, Notifications) {
          $scope.client = {
            "attributes": {
              "login_theme": "default",
              "consent_rejected_reason": "Incorrect Information##Suspicious Activity##Transaction not triggered by me##Inappropriate Transaction"
            },
            "authenticationFlowBindingOverrides": {
              "ciba_auth": "api-authorizer"
            },
            "enabled": true,
            "redirectUris": [],
            "protocol": "openid-connect",
            "appType": "ciba"
          };

          $scope.ok = function () {
            Client.save(
              {
                realm: realm.realm,
                client: "",
              },
              { ...$scope.client},
              function (data, headers) {
                Notifications.success($translate.instant("api-app-created"))
                $route.reload();
              },
              function (err) {
                Notifications.error($translate.instant(err.data.errorMessage));
              },
            );
          };

          $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
          };
          $scope.$on('$routeChangeStart', function () {
            $modalInstance.close();
          });
          $(document).ready(function () {
              setTimeout(function () {
                  $("#name").focus();
              }, 100);
          });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-api-app.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
            }
        });
    };
});

module.controller(
  "ManagementTabCtrl",
  function (
    $scope,
    Current,
    $route,
    GetRealmDetails
  ) {
    $scope.currentRealm;
    $scope.showPricing = false;
    $scope.master = Current.realm;

    GetRealmDetails.query({ realm: Current.realm.realm, name:$route.current.params.name || $route.current.params.subRealm}, function(data) {
        $scope.currentRealm = angular.copy(data);
        $scope.currentRealmName = $scope.currentRealm.workspaceName;
        $scope.isApiProviderEnabled = $scope.currentRealm.isApiWorkspace;
        $scope.showSync = $scope.currentRealm.isLive ? true : false;
        $scope.showPricing = $scope.serverInfo.rootAccountName == $scope.currentRealmName ? true : ( $scope.currentRealm.attributes.hasOwnProperty('tenant_pricing_type') && $scope.currentRealm.attributes['tenant_pricing_type'] == 'custom') ? true : false;
        $scope.loading = false;
    })
  },
);

module.controller('RealmManagementName', function ($scope, $http, $translate, Notifications, realm, GetRealmDetails, $route, UpdateRealmPricingType, serverInfo) {
    $scope.realm = angular.copy(realm);
    $scope.currentRealm = null
    $scope.loaderTypeName = "cardListLoader";
    $scope.loading = true;
    $scope.syncLoading = false;
    $scope.customPricing = false;
    $scope.showTab = false;
    $scope.isAPIEnabled = false;
    $scope.priceLoading = false;
    $scope.serverInfo = serverInfo;
    $scope.loadertype = "detailsLoader"

    $scope.updatePricing =  (type) => {
        $scope.priceLoading = true
        UpdateRealmPricingType.update({realm: $scope.currentRealm.workspaceName, realmId: $scope.currentRealm.workspaceId, pricingType: type }, {}, function(){
            $scope.currentRealm.attributes['tenant_pricing_type'] = $scope.customPricing ? 'custom' : 'default';
            Notifications.success($translate.instant("realm-updated-successfully"));
            $route.reload()
            $scope.priceLoading = false;
        }, function(){
            Notifications.error($translate.instant('update-pricing-type-update-error'));
            $scope.priceLoading = false;
        })
    };
    
    GetRealmDetails.query({ realm: realm.realm, name:$route.current.params.name}, function(data) {
        $scope.currentRealm = angular.copy(data);
        $scope.loading = false;
        $scope.showPricingToggle = $scope.currentRealm.isLive ? true : false;
        $scope.isAPIEnabled = $scope.currentRealm.attributes['isApiWorkspace'] === "true" ? true : false;
        $scope.customPricing = ($scope.currentRealm.attributes.hasOwnProperty('tenant_pricing_type') && $scope.currentRealm.attributes['tenant_pricing_type'] == 'custom') ? true : false;
    })

    $scope.syncTenant = () => {
        $scope.syncLoading = true;
        $http.get(`${authUrl}/realms/api/qv_subscription/subscription/${$scope.currentRealm.workspaceId}`)
        .then(function (response) {
            $scope.syncLoading = false;
            Notifications.success($translate.instant("workspace-sync-success"));
        }).catch(function () {
            $scope.syncLoading = false;
            Notifications.error($translate.instant("workspace-sync-failed"));
        });
    }


    $scope.updateValue = (key) => {
        $http.post(`${authUrl}/admin/realms/${realm.realm}/workspace-management/${$scope.currentRealm.workspaceName}/${key}?value=${!$scope.currentRealm[key]}`)
        .then(function (response) {
            console.log("respons", response)
            if (response.data.message === "Cant update master workspace details") {
                $route.reload()
                Notifications.error($translate.instant("can-not-update-master"));
            } else {
                Notifications.success($translate.instant("realm-updated-successfully"));
                $route.reload()
            }
        }).catch(function (message) {
            $route.reload()
            Notifications.error($translate.instant("failed-to-update-realm"));
        });
    }
});
