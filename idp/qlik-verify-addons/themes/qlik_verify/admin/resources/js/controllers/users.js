module.controller('KcRolesGroupsTree', function ($scope, $http, $route, Client, Notifications, RealmRoleMapping, GroupRealmRoleMapping, UserGroupMembership, AddGroupsToUser, AddRolesToUser, Dialog, $translate, UserCredentials,Auth,AccountCreator,InputDialog) {
    const realm = $scope.$parent.realm;
    const user = $scope.$parent.user;
    const client = $scope.$parent.client;
    const root = document.querySelector(':root');
    $scope.realm = realm;
    $scope.user = user;
    $scope.client = client;
    $scope.groupMemberships = [];
    $scope.realmMappings = [];
    $scope.roles = [];
    $scope.groups = [];
    $scope.selectedClient = null;
    $scope.groupRoles = [];
    $scope.groupRoleLen = 0;
    $scope.isRoleLoaded = false;
    $scope.isGroupLoaded = false;
    $scope.isGroupRolesLoaded = false;
    $scope.currentGroup = {};
    $scope.currentGroupRolesLen = 0;
    $scope.query = {
        search: null,
        realm: realm.realm,
        userId: user.id
    }

    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";
    

    if (!user.profPic) {
        const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
        $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
    }

    function initialQueryUpdate() {
            getRoles();
            getGroups();
    }

    const getRoles = () => {
        RealmRoleMapping.query($scope.query, function(response) {
            $scope.realmMappings = response
            $scope.realmMappings = $scope.realmMappings.filter(role => !role.attributes.hasOwnProperty('isGroupRole'))
            $scope.roles = $scope.realmMappings.slice(0, 5) 
            $scope.isRoleLoaded = true;
            setCssCountValue("roles")
        });
    }

    $scope.showMoreRoles = () => {
        $scope.roles =  $scope.realmMappings.slice(0, $scope.roles.length + 5) 
        console.log($scope.realmMappings, $scope.roles)
        setCssCountValue("roles")
    }

    const setCssCountValue = (type) => {
        switch(type) {
            case "groups":
                const groupsLen = $scope.groups.length;
                const groupMappingLen = $scope.groupMemberships.length
                root.style.setProperty('--groups-count', `${((groupsLen +  (groupsLen === groupMappingLen ? 1 : 2))  * 49.5) - 51.2}px`);
                break;
            case "roles":
                const rolesLen = $scope.roles.length;
                const roleMappingLen = $scope.realmMappings.length
                root.style.setProperty('--roles-count',  `${((rolesLen +  (rolesLen === roleMappingLen ? 1 : 2) )  * 49.5) - 48.16}px`);
                break;
            default: 
                root.style.setProperty('--group-roles-count',  `${(( $scope.currentGroupRolesLen)  * 49.5) - 51.2}px`);
                let length = $scope.currentGroup.name.length >= 22 ? 20 : $scope.currentGroup.name.length;
                root.style.setProperty('--roles-label-width',  `${(23 - length) * 10}px`);
                break;
        }
    }

    const getGroups = () => {
        UserGroupMembership.query($scope.query, function(response) {
            $scope.groupMemberships = response;
            $scope.groupMemberships = $scope.groupMemberships.map(group => {
                if (group.rolesCount > 0) {
                    getGroupRoles(group.id)
                }
                return group
            })
            $scope.groups = $scope.groupMemberships.slice(0, 5);
            $scope.isGroupLoaded = true;
            setCssCountValue("groups")
        });
    }

    $scope.showMoreGroups = () => {
        $scope.groups = $scope.groupMemberships.slice(0, $scope.groups.length + 5) 
        setCssCountValue("groups")
    }

    const getGroupRoles = (groupId) => {
        const query = {
            ...$scope.query,
            groupId
        }
        GroupRealmRoleMapping.query(query, function(response) {
            $scope.groupRoles.push({groupId, roles:response});
        });
        $scope.groupRoleLen++;
    }

    $scope.$watch('groupRoles', function () {
       if ($scope.groupRoleLen === $scope.groupRoles.length) {
        $scope.isGroupRolesLoaded = true;
       }
       else {
       $scope.isGroupRolesLoaded = false;
       }
    }, true);

    $scope.setCurrentGroup = (group) => {
        if (group.id !== $scope.currentGroup?.id) {
            $scope.currentGroup = group;
            return;
        }
        $scope.currentGroup = {}
    }

    $scope.scrollToView = () => {
        const groupRolesElement = document.getElementById("holder");
        groupRolesElement.scrollLeft = 500;
    }

    $scope.getGroupRoles = (group) => {
        const roles =  $scope.groupRoles.filter(role => role.groupId === group.id)[0].roles;
        $scope.currentGroupRolesLen = roles.length
        setCssCountValue()
        return roles;
    }

    $scope.addRolesToUser = function () {
        AddRolesToUser.open(realm.realm, user.id, "Check", user, realm);
    };

    $scope.deleteRealmRole = function (role) {
        Dialog.confirm($translate.instant('delete-role-title'), $translate.instant('sure-to-delete-role-from-user'), function () {
            $scope.selectedRealmMappingsToRemove = [{ ...role }];
            $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/realm', { 
                data: $scope.selectedRealmMappingsToRemove,
                headers: { "content-type": "application/json" }
            }).then(function () {
                Notifications.success($translate.instant('user.roles.remove.success'));
                $route.reload();
            });
        });
    };


    $scope.addGroupsToUser = () => AddGroupsToUser.open(realm.realm, user.id, 'Check');

    $scope.deleteUserGroup = function(group) {
        Dialog.confirm($translate.instant('delete-group-title'), $translate.instant('are-you-sure-to-remove-group-for-user'), function() {
            $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + $scope.user.id + '/groups/' + group.id).then(function() {
                initialQueryUpdate();
                Notifications.success($translate.instant("user.groups.leave.success"));
            }, function(err){
                Notifications.error($translate.instant(err.description));

            });
        });
    };


    initialQueryUpdate();
});

module.controller('KcUserRoleMappingCtrl', function ($scope, $http, $route, Client, Notifications, RealmRoleMapping, RealmRoleMappingCount, AddRolesToUser, Dialog, $translate, UserCredentials,Auth,AccountCreator,InputDialog, Session) {
    $scope.loaderTypeName = 'roleListLoader';
    const realm = $scope.$parent.realm;
    const user = $scope.$parent.user;
    const client = $scope.$parent.client;
    $scope.realm = realm;
    $scope.user = user;
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.client = client;
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];
    $scope.selectedRealmMappingsToRemove = [];
    $scope.selectedClient = null;
    $scope.entries = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.searchLoaded = false;
    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";
    $scope.query = {
        search: null,
        realm: realm.realm,
        max: Session.getItem("userRolesPageFilterCount") || 5,
        first: 0,
        userId: user.id
    }
    $scope.isWorkflowRole = (role) => {
        return role.attributes['connectedApp'] ? true : false;
    }

    $scope.showChangeAccountCreatorOption = Auth.user.isAccountCreator && Auth.user.userId != user.id  && user.attributes && (!user.attributes.qv_is_account_creator || user.attributes.qv_is_account_creator == 'false')
    $scope.changeAccountCreator = function () {
        AccountCreator.sendOtp({ realm: realm.realm, userId: user.id }, function () {
            InputDialog.changeAccountCreator(realm.realm,user, function () {
            });
        })
    }
    initialQueryUpdate();

    function initialQueryUpdate() {
        $scope.realmMappings = RealmRoleMapping.query($scope.query, function() {
            oldQueryUpdate();
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    }

    $scope.resetPassword = function(){
        Dialog.confirm($translate.instant('reset-password'), $translate.instant('sure-to-reset-password'), function() {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.id,client_id : 'dashboard'}, { type : "password", value : null, temporary: true }, function() {
                Notifications.success($translate.instant("password-has-been-reset"));
                $scope.openRedirect = true;
                $route.reload();
            });
        });
    };

    $scope.deleteRealmRole = function (role) {
        Dialog.confirm($translate.instant('delete-role-title'), $translate.instant('sure-to-delete-role-from-user'), function () {
            $scope.selectedRealmMappingsToRemove = [{ ...role }];
            $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/realm', { 
                data: $scope.selectedRealmMappingsToRemove,
                headers: { "content-type": "application/json" }
            }).then(function () {
                Notifications.success($translate.instant('user.roles.remove.success'));
                $route.reload();
            });
        });
    };

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
        } else if (pageCount === $scope.entries) {
            let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
            $scope.pageCount = pageCount - backFlow;
        } else {
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

    $scope.selectQuery = function () {
        Session.setItem("userRolesPageFilterCount", $scope.query.max)
        $scope.query.first = 0;
        $scope.searchLoaded = false;
        initialQueryUpdate();
        oldQueryUpdate();
    };

    $scope.reset = function () {
        $scope.query.search = "";
        $route.reload();
    };

    $scope.QueryUpdate = function () {
        $scope.searchLoaded = false;
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        $scope.realmMappings = RealmRoleMapping.query($scope.query, function () {
            $scope.searchLoaded = true;
        });
    };
    

    $scope.isRoleListEmpty = function () {
        return $scope.realmRoles.length == 0;
    };

    function searchQueryWithLoaded() {
        $scope.searchLoaded = false;
        if ($scope.query.search && $scope.searchQueryLoaded) {
            $scope.searchQueryLoaded = true;
        } else {
            $scope.query.search = "";
            $scope.searchQueryLoaded = false;
        }
    }

    function oldQueryUpdate() {
        RealmRoleMappingCount.query({realm: realm.realm, userId: user.id, search: $scope.query.search}, function(response) {
            $scope.entries = response.count;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                $scope.pageCount = $scope.entries;
                $scope.startCount = 1;
            } else {
                $scope.pageCount = $scope.query.max;
                $scope.startCount = 1;
            }
        });
    }

    if (!user.profPic) {
        const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
        $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
    }

    $scope.addRolesToUser = function () {
        AddRolesToUser.open(realm.realm, user.id, "Check", user, realm);
    };

    clientSelectControl($scope, $route.current.params.realm, Client);
});

module.service("AddRolesToUser", function ($modal, $translate) {
    var dialog = {};
    dialog.open = function (realmName, userId, title, user, realm) {
        var controller = function ($scope, $http, $modalInstance, $route, Notifications, AvailableRealmRoleMapping, AvailableRealmRoleMappingCount,User) {
            $scope.isRTL = () => locale == 'ar';
            $scope.checkedRoles = {};
            $scope.loaderTypeName = 'roleListLoader';
            $scope.realmName = realmName;
            $scope.resourceUrl = resourceUrl;
            $scope.userId = userId;
            $scope.query = {
                search: null,
                realm: realmName,
                max: 5,
                first: 0,
                userId: userId
            }
            $scope.entries = 0;
            $scope.pageCount = 5;
            $scope.startCount = 1;
            $scope.maxValueOfPage = 0;
            $scope.searchQueryLoaded = false;

            $scope.currentRole = {};
            $scope.userOrganizations = null;
            $scope.userProjects = null;
            $scope.workflows = []
            $scope.userMetadata = {};
            $scope.availableOrg =  []
            $scope.availableProj = []
            $scope.organizations = [];
            $scope.projects = [];
            $scope.configuredWorkflowRoles = {};
            $scope.isConfigLoaded = false;
            $scope.enableDone = false

            const getMetaData = () => {
                $http.get(authUrl + `/realms/${realm.realm}/workflows/connectedAppMetadata?type=user`).then(function (result) {
                    $scope.workflows = result.data;
                    $scope.availableOrg = $scope.workflows[0].metaData.organization
                    $scope.availableProj = $scope.workflows[0].metaData.projects
                    $scope.organizations = $scope.availableOrg.map(org => org.name);
                    $scope.projects = $scope.availableProj.map(prj => prj.name);
                    $scope.isConfigLoaded = true;
                }).catch(function (error) {
                    $scope.isConfigLoaded = true;
                });
            }
            getMetaData();

            $scope.addUserAttribute = () => {
                Object.keys($scope.configuredWorkflowRoles).map( key => {
                    const organizations = $scope.configuredWorkflowRoles[key].organizations;
                    const projects = $scope.configuredWorkflowRoles[key].projects
                    $scope.userMetadata[`${key}_organizations`] = [JSON.stringify($scope.availableOrg.filter(org => organizations.includes(org.name)))];
                    $scope.userMetadata[`${key}_projects`] = [JSON.stringify($scope.availableProj.filter(pro => projects.includes(pro.name)))];
                })  
            }

            $scope.addUserAttr = () => {
                const metaData = {};
                Object.keys($scope.checkedRoles).map(roleKey => {
                    Object.keys($scope.userMetadata).map(key => {
                        if(key.split('_')[0] === $scope.checkedRoles[roleKey].name) {
                            metaData[key] = $scope.userMetadata[key];
                        }
                    })
                })
                user.attributes = {...user.attributes, ...metaData}
                User.update({
                    realm: realm.realm,
                    userId: user.id
                }, user, function (data, headers) {
                    $scope.addRealmRole()
                }, function (error) {
                    Notifications.error($translate.instant(error.data.errorMessage));                 
                });
            }

            $scope.isRoleConfigured = (role) => {
                return Object.keys($scope.configuredWorkflowRoles).includes(role.name);
            }

            $scope.isWorkflowRole = (role) => {
                return role.attributes['connectedApp'] ? true : false;
            } 
 
            $scope.setCurrentRole = (role) => {
                $scope.currentRole = role;
                if (Object.keys($scope.configuredWorkflowRoles).includes(role.name)) {
                    const organizations = $scope.configuredWorkflowRoles[role.name].organizations;
                    const projects = $scope.configuredWorkflowRoles[role.name].projects;
                    $scope.currentRole['organizations'] = organizations.length > 0 ? organizations : null;
                    $scope.currentRole['projects'] = projects.length > 0 ? projects : null;
                    return;
                }
                $scope.currentRole['organizations'] =  null;
                $scope.currentRole['projects'] = null;
            }

            $scope.configureWorkflowRole = ()=> {
                $scope.configuredWorkflowRoles[$scope.currentRole.name] = {
                    organizations:$scope.currentRole.organizations, 
                    projects:$scope.currentRole.projects
                }
                $scope.addUserAttribute();
                $scope.realmRoles = $scope.realmRoles.map(role => {
                    if (role.id === $scope.currentRole.id) {
                        return {...role, isChecked : true}
                    } else {
                        return role;
                    } 
                })
                $scope.currentRole = {};
            }

            if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
                $scope.query.search = "";
            }

            $scope.cancelWorkflow = () => {
                $scope.currentRole = {};
            } 

            $scope.$watch('currentRole', function() {
                if (($scope.currentRole.hasOwnProperty('projects') && $scope.currentRole.projects.length > 0) 
                    || ($scope.currentRole.hasOwnProperty('organizations') && $scope.currentRole.organizations.length)) {
                    $scope.enableDone = true;
                } else {
                    $scope.enableDone = false;
                }
            },true)

            initialQueryUpdate();
            function initialQueryUpdate() {
                $scope.realmRoles = AvailableRealmRoleMapping.query($scope.query, function() {
                    oldQueryUpdate();
                    $scope.searchLoaded = true;
                    $scope.lastSearch = $scope.query.search;
                });
            }
            
            $scope.$watch(function () {
                for (var i = 0; i < $scope.realmRoles?.length; i++) {
                    if ($scope.realmRoles[i].isChecked == true) {
                        $scope.checkedRoles[$scope.realmRoles[i].id] = $scope.realmRoles[i];
                    } else {
                        $scope.checkedRoles[$scope.realmRoles[i].id] && delete $scope.checkedRoles[$scope.realmRoles[i].id];
                    }
                }
            }, true);

            $scope.$watch(function () {
                $scope.enableBtn = false;
                Object.keys($scope.checkedRoles).forEach((key) => {
                    if ($scope.checkedRoles[key].isChecked == true) {
                        $scope.enableBtn = true;
                    }
                });
            }, true);

            $scope.addRealmRole = function () {
                $scope.enableBtn = false
                const selectedRoles = [];
                const finalRoles = JSON.parse(JSON.stringify($scope.checkedRoles));
                Object.keys(finalRoles).forEach((key) => {
                    delete finalRoles[key].isChecked;
                    delete finalRoles[key].organizations;
                    delete finalRoles[key].projects;
                    selectedRoles.push(finalRoles[key])
                });
                if (!selectedRoles) return;
                $scope.selectedRealmRoles = [];
                $http.post(authUrl + '/admin/realms/' + $scope.realmName + '/users/' + $scope.userId + '/role-mappings/realm',
                    selectedRoles)
                    .then(function (res) {
                        $modalInstance.dismiss('cancel');
                        $route.reload();
                        Notifications.success($translate.instant('user.roles.add.success'));
                    }, function (err) {
                        Notifications.error(err.data.error);
                    });
            };

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };

            function oldQueryUpdate() {
                AvailableRealmRoleMappingCount.query({realm: realmName, userId: userId, search: $scope.query.search}, function(response) {
                    $scope.entries = response.count;
                    if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                        $scope.pageCount = $scope.entries;
                        $scope.startCount = 1;
                    } else {
                        $scope.pageCount = $scope.query.max;
                        $scope.startCount = 1;
                    }
                });
            }

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
                } else if (pageCount === $scope.entries) {
                    let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
                    $scope.pageCount = pageCount - backFlow;
                } else {
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
        
            $scope.QueryUpdate = function () {
                $scope.searchLoaded = false;
                for (var i in $scope.query) {
                    if ($scope.query[i] === "") {
                        delete $scope.query[i];
                    }
                }
                AvailableRealmRoleMapping.query($scope.query, function (data) {
                    updateCheckedRoles(data);
                    $scope.searchLoaded = true;
                });
            };
            
            $scope.searchQuery = function () {
                if ($scope.lastSearch !== $scope.query.search) {
                    $scope.searchLoaded = false;
                    if ($scope.query.search) {
                        $scope.searchQueryLoaded = true;
                    } else {
                        $scope.searchQueryLoaded = false;
                    }
                    oldQueryUpdate();
                    searchQueryWithLoaded();
                    $scope.query.first = 0;
                    $scope.startCount = 1;
                    AvailableRealmRoleMapping.query($scope.query, function (data) {
                        updateCheckedRoles(data);
                        $scope.searchLoaded = true;
                        $scope.lastSearch = $scope.query.search;
                    });
                }
            };
            function searchQueryWithLoaded() {
                $scope.searchLoaded = false;
                if ($scope.query.search && $scope.searchQueryLoaded) {
                    $scope.searchQueryLoaded = true;
                } else {
                    $scope.query.search = "";
                    $scope.searchQueryLoaded = false;
                }
            }

            $scope.isRoleListEmpty = function () {
                return $scope.realmRoles.length == 0;
            };

            $scope.unCheckRoles = function(roleId) {
                for (var i = 0; i < $scope.realmRoles?.length; i++) {
                    if ($scope.realmRoles[i].id == roleId) {
                        $scope.realmRoles[i].isChecked = false;
                    }
                }
                delete $scope.checkedRoles[roleId];
            }

            function updateCheckedRoles(fetchedRoles) {
                fetchedRoles
                    .map((role) => {
                        if ($scope.checkedRoles[role.id]) {
                            role.isChecked = true;
                        }
                        return role;
                    });
                $scope.realmRoles = fetchedRoles;
            }
        };

        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/add-roles-modal.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            },
            size: 'lg'
        });
    };
    return dialog;
});

module.controller('UserSessionsCtrl', function ($scope, realm, user, sessions, UserSessions, UserLogout, UserSessionLogout, Notifications, Dialog, UserCredentials, $translate,Auth,AccountCreator,InputDialog) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.sessions = sessions;
    $scope.signout = $translate.instant('sign-out');
    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";

    $scope.showChangeAccountCreatorOption = Auth.user.isAccountCreator && Auth.user.userId != user.id && user.attributes && (!user.attributes.qv_is_account_creator || user.attributes.qv_is_account_creator == 'false')
    $scope.changeAccountCreator = function () {
        AccountCreator.sendOtp({ realm: realm.realm, userId: user.id }, function () {
            InputDialog.changeAccountCreator(realm.realm,user, function () {
            });
        })
    }
    $scope.logoutAll = function () {
        UserLogout.save({ realm: realm.realm, user: user.id }, function () {
            Notifications.success($translate.instant('user.logout.all.success'));
            UserSessions.query({ realm: realm.realm, user: user.id }, function (updated) {
                $scope.sessions = updated;
            });
        });
    };

    $scope.resetPassword = function(){
        Dialog.confirm($translate.instant('reset-password'), $translate.instant('sure-to-reset-password'), function() {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.id,client_id : 'dashboard'}, { type : "password", value : null, temporary: true }, function() {
                Notifications.success($translate.instant("password-has-been-reset"));
                $scope.openRedirect = true;
                $route.reload();
            });
        });
    };

    $scope.logoutSession = function (sessionId) {
        console.log('here in logoutSession');
        UserSessionLogout.delete({ realm: realm.realm, session: sessionId }, function () {
            UserSessions.query({ realm: realm.realm, user: user.id }, function (updated) {
                $scope.sessions = updated;
                Notifications.success($translate.instant('user.logout.session.success'));
            });
        });
    };

    if (!user.profPic) {
        const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
        $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
    }

    $scope.isUserSessionEmpty = function () {
        return $scope.sessions.length == 0;
    };
});

module.controller('UserFederatedIdentityCtrl', function ($scope, $location, UserCredentials, realm, user, federatedIdentities, UserFederatedIdentity, Notifications, Dialog, $translate,Auth,AccountCreator,InputDialog) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.federatedIdentities = federatedIdentities;
    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";
    if (!user.profPic) {
        const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
        $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
    }

    $scope.hasAnyProvidersToCreate = function () {
        return realm.identityProviders.length - $scope.federatedIdentities.length > 0;
    };

    $scope.removeProviderLink = function (providerLink) {
        Dialog.confirmRemove(providerLink.identityProvider, 'Connection Link', function () {
            UserFederatedIdentity.remove({ realm: realm.realm, user: user.id, provider: providerLink.identityProvider }, function () {
                Notifications.success($translate.instant("user.fedid.link.remove.success"));
                var indexToRemove = $scope.federatedIdentities.indexOf(providerLink);
                $scope.federatedIdentities.splice(indexToRemove, 1);
            });
        });
    };

    $scope.showChangeAccountCreatorOption = Auth.user.isAccountCreator && Auth.user.userId != user.id && user.attributes && (!user.attributes.qv_is_account_creator || user.attributes.qv_is_account_creator == 'false')
    $scope.changeAccountCreator = function () {
        AccountCreator.sendOtp({ realm: realm.realm, userId: user.id }, function () {
            InputDialog.changeAccountCreator(realm.realm,user, function () {
            });
        })
    }

    $scope.resetPassword = function(){
        Dialog.confirm($translate.instant('reset-password'), $translate.instant('sure-to-reset-password'), function() {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.id,client_id : 'dashboard'}, { type : "password", value : null, temporary: true }, function() {
                Notifications.success($translate.instant("password-has-been-reset"));
                $scope.openRedirect = true;
                $route.reload();
            });
        });
    };
});

module.controller('UserFederatedIdentityAddCtrl', function ($scope, $location, realm, user, federatedIdentities, UserFederatedIdentity, Notifications, $translate) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.federatedIdentity = {};

    if (!user.profPic) {
        const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
        $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
    }

    var getAvailableProvidersToCreate = function () {
        var realmProviders = [];
        for (var i = 0; i < realm.identityProviders.length; i++) {
            var providerAlias = realm.identityProviders[i].alias;
            realmProviders.push(providerAlias);
        };

        for (var i = 0; i < federatedIdentities.length; i++) {
            var providerAlias = federatedIdentities[i].identityProvider;
            var index = realmProviders.indexOf(providerAlias);
            realmProviders.splice(index, 1);
        }

        return realmProviders;
    };
    $scope.availableProvidersToCreate = getAvailableProvidersToCreate();

    $scope.save = function () {
        UserFederatedIdentity.save({
            realm: realm.realm,
            user: user.id,
            provider: $scope.federatedIdentity.identityProvider
        }, $scope.federatedIdentity, function (data, headers) {
            $location.url("/realms/" + realm.realm + '/users/' + $scope.user.id + '/federated-identity');
            Notifications.success($translate.instant("user.fedid.link.add.success"));
        });
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + '/users/' + $scope.user.id + '/federated-identity');
    };

});

module.controller('UserConsentsCtrl', function ($scope, realm, user, userConsents, UserConsents, Notifications, $translate) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.userConsents = userConsents;

    $scope.revokeConsent = function (clientId) {
        UserConsents.delete({ realm: realm.realm, user: user.id, client: clientId }, function () {
            UserConsents.query({ realm: realm.realm, user: user.id }, function (updated) {
                $scope.userConsents = updated;
            });
            Notifications.success($translate.instant('user.consent.revoke.success'));
        }, function () {
            Notifications.error("Grant couldn't be revoked");
        });
        console.log("Revoke consent " + clientId);
    };
});

module.controller('UserOfflineSessionsCtrl', function ($scope, $location, realm, user, client, offlineSessions) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.client = client;
    $scope.offlineSessions = offlineSessions;

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + '/users/' + user.id + '/consents');
    };
});


module.controller('UserListCtrl', function ($scope, realm, User, UserSearchState, UserImpersonation, UsersCount, BruteForce, Notifications, $route, Dialog, Session, serverInfo, $translate, UserCredentials) {
 
    $scope.realm = realm;
    $scope.loaderTypeName = 'userListLoader';
    UserSearchState.query.realm = realm.realm;
    $scope.serverInfo = serverInfo;
    $scope.query = UserSearchState.query;
    $scope.query.briefRepresentation = 'true';
    $scope.query.max = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
    $scope.query.first = 0;
    $scope.query.orderBy = realm.mode !== 'bo' ? 'last-logged-in' : 'asc';
    $scope.orderBy = $translate.instant($scope.query.orderBy);
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.searchQueryLoaded = false;
    $scope.navigationLoaded = false;
    if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
        $scope.query.search = "";
    }
    $scope.copyElement = angular.copy($scope.query.max);
    initialQueryUpdate();

    $scope.setOrderBy = (value) => {
        $scope.query.orderBy = value;
        $scope.orderBy = $translate.instant($scope.query.orderBy)
        $scope.showFilter = false;
        $scope.searchLoaded = false;
        $scope.QueryUpdate();
    }


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
        UsersCount.query({"realm": realm.realm, "search": $scope.query.search}, function(data) {
            $scope.entries = data.count ? parseInt(data.count) : 0;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                $scope.pageCount = $scope.entries;
                $scope.startCount = 1;
            } else {
                $scope.pageCount = $scope.query.max;
                $scope.startCount = 1;
            }
        })
    }
    $scope.$watch('query.max', function () {
        $scope.query.first = 0;
    }, true);

    $scope.editTxt = $translate.instant('edit');
    $scope.impersonateTxt = "Impersonate";
    $scope.deleteTxt = "Remove";

    $scope.impersonate = function (userId) {
        UserImpersonation.save({ realm: realm.realm, user: userId }, function (data) {
            data.redirect = data.redirect.replace('applications', '');
            if (data.sameRealm) {
                window.location = data.redirect;
            } else {
                window.open(data.redirect, "_blank");
            }
        });
    };

    $scope.unlockUsers = function () {
        BruteForce.delete({ realm: realm.realm }, function (data) {
            Notifications.success($translate.instant("user.unlock.success"));
        });
    };

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
        $scope.users = User.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            ClientListSearchState.isFirstSearch = false;
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
        if ($scope.query.search && $scope.searchQueryLoaded) {
            $scope.searchQueryLoaded = true;
        } else {
            $scope.searchQueryLoaded = false;
        }
    }
    $scope.reset = function () {
        $scope.query.search = "";
        $route.reload();
    };

    $scope.clearSearch = function () {
        $scope.query.search = ""
        $scope.searchQuery();
    };

    $scope.QueryUpdate = function () {
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        $scope.users = User.query($scope.query, function () {
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
            $scope.users = User.query($scope.query, function () {
                $scope.lastSearch = $scope.query.search;
                UserSearchState.isFirstSearch = false;
                $scope.searchLoaded = true;
            });
        }
    };

    $scope.isUserListEmpty = function () {
        return $scope.users.length == 0;
    };

    $scope.getProfilePicture = function (username, email) {
        const profPicBase64 = generateAvatar(username ? username : email);
        $scope.users.profPic = profPicBase64.split("base64,")[1];
        return $scope.users.profPic;

    };

    $scope.addAdminRole = function(user){
        Dialog.confirm($translate.instant('promote-user'), $translate.instant('are-you-sure-to-promote-user'), function() {            
            UserCredentials.addAdminRole({ realm: realm.realm, userId: user.id,client_id : 'dashboard'}, function() {
                Notifications.success($translate.instant("users-role-updated"));
                $scope.openRedirect = true;
                $route.reload();
            });
        });
    };

    $scope.removeUser = function (user) {
        Dialog.confirmDelete($scope.realm.mode === 'bo' ? user.username : user.username, 'user', function () {
            user.$remove({
                realm: realm.realm,
                userId: user.id
            }, function () {
                $route.reload();

                if ($scope.users.length === 1 && $scope.query.first > 0) {
                    $scope.previousPage();
                }

                Notifications.success($translate.instant("user.remove.success"));
            }, function () {
                Notifications.error("User couldn't be deleted");
            });
        });
    };

    function initialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        searchQueryWithLoaded();
        $scope.users = User.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            ClientListSearchState.isFirstSearch = false;
        });
    };
});

module.controller('AdminListCtrl', function ($scope, $http, realm, Admin, AdminUsersCount, User, AdminSearchState, UserImpersonation, BruteForce, Notifications, $route, Dialog, Session, $translate, UserCredentials) {
    $scope.realm = realm;
    AdminSearchState.query.realm = realm.realm;
    $scope.isAPIEnabled = $scope.realm.attributes['isApiWorkspace'] === "true" ? true : false;
    $scope.isWorkflowEnabled = $scope.realm.attributes['isPlatformWorkspace'] === "true" ? true : false;
    $scope.query = AdminSearchState.query;
    $scope.loaderTypeName = 'userListLoader';
    $scope.query.briefRepresentation = 'true';
    $scope.query.max = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
    $scope.query.first = 0;
    $scope.query.orderBy = 'last-logged-in'
    $scope.orderBy = $translate.instant($scope.query.orderBy);
    $scope.showFilter = false;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.searchQueryLoaded = false;
    $scope.navigationLoaded = false;
    if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
        $scope.query.search = "";
    }
    $scope.auth = auth
    $scope.copyElement = angular.copy($scope.query.max);
    initialQueryUpdate();
    $scope.makeSideMenuClosed();
    $scope.setOrderBy = (value) => {
        $scope.query.orderBy = value;
        $scope.orderBy = $translate.instant($scope.query.orderBy)
        $scope.showFilter = false;
        $scope.searchLoaded = false;
        $scope.QueryUpdate();
    }
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
        //Updating Admin Users count
        AdminUsersCount.query({"realm": realm.realm, "search": $scope.query.search}, function(data) {
            $scope.entries = data.count ? parseInt(data.count) : 0;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                $scope.pageCount = $scope.entries;
                $scope.startCount = 1;
            } else {
                $scope.pageCount = $scope.query.max;
                $scope.startCount = 1;
            }
        })

    }
    $scope.$watch('query.max', function () {
        $scope.query.first = 0;
    }, true);

    $scope.editTxt = $translate.instant('edit');
    $scope.impersonateTxt = "Impersonate";
    $scope.deleteTxt = "Remove";

    $scope.impersonate = function (userId) {
        UserImpersonation.save({ realm: realm.realm, user: userId }, function (data) {
            data.redirect = data.redirect.replace('applications', '');
            if (data.sameRealm) {
                window.location = data.redirect;
            } else {
                window.open(data.redirect, "_blank");
            }
        });
    };

    $scope.unlockUsers = function () {
        BruteForce.delete({ realm: realm.realm }, function (data) {
            Notifications.success($translate.instant("user.unlock.success"));
        });
    };

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
        $scope.users = Admin.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            ClientListSearchState.isFirstSearch = false;
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
        if ($scope.query.search && $scope.searchQueryLoaded) {
            $scope.searchQueryLoaded = true;
        } else {
            $scope.query.search = "";
            $scope.searchQueryLoaded = false;
        }
    }
    $scope.reset = function () {
        $scope.query.search = "";
        $route.reload();
    };

    $scope.clearSearch = function () {
        $scope.query.search = "";
        $scope.searchQuery();
    };

    $scope.QueryUpdate = function () {
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        $scope.users = Admin.query($scope.query, function () {
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
            $scope.users = Admin.query($scope.query, function () {
                $scope.lastSearch = $scope.query.search;
                AdminSearchState.isFirstSearch = false;
                $scope.searchLoaded = true;
            });
        }
    };

    $scope.isAdminsEmpty = function () {
        return $scope.users.length == 0;
    };

    $scope.removeAdminRole = function(user){
        Dialog.confirm($translate.instant('demote-user'), $translate.instant('are-you-sure-to-demote-user'), function() {
            UserCredentials.removeAdminRole({ realm: realm.realm, userId: user.id,client_id : 'dashboard', user_id: user.id}, function() {
                Notifications.success($translate.instant("users-role-updated"));
                $scope.openRedirect = true;
                $route.reload();
            });
        });
    };

    $scope.removeUser = function (user) {
        Dialog.confirmDelete(user.username, 'user', function () {
            User.remove({
                realm: realm.realm,
                userId: user.id
            }, function () {
                $route.reload();

                if ($scope.users.length === 1 && $scope.query.first > 0) {
                    $scope.previousPage();
                }

                Notifications.success($translate.instant("user.remove.success"));
            }, function () {
                Notifications.error("User couldn't be deleted");
            });
        });
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

    function initialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        searchQueryWithLoaded();
        $scope.users = Admin.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
            ClientListSearchState.isFirstSearch = false;
        });
    };
    $scope.getProfilePicture = function (username, email) {
        const profPicBase64 = generateAvatar(username ? username : email);
        $scope.users.profPic = profPicBase64.split("base64,")[1];
        return $scope.users.profPic;

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



module.controller('UserTabCtrl', function ($scope, User, UserCredentials, ConvertPictureToUrl, UserImpersonation, $location, $route, InputDialog, Dialog, PasswordRegDialog, Notifications, Current, $translate) {
    if ($scope.user.profPic) {
        $scope.user.profPic = getUpdatedImgData($scope.user.profPic);
    }
    $scope.userhasPassword = false;
    $scope.temporaryPassword = false;
    $scope.userId = $scope.user.id ? $scope.user.id : $route.current.params.user;
    $scope.realmId = Current.realm && Object.keys(Current.realm).length > 0 ? Current.realm.realm : $route.current.params.realm;
    $scope.create = !$scope.userId;
    $scope.dashboardUrl = dashboardUrl;

    $scope.uploadPro = () => {
        $("#imageUpload").click();
    };


    $(document).ready(function(){
        $("#imageUpload").change(function () {
            fasterPreview(this);
        });
    });

    function fasterPreview(uploader) {
        var userCopy = angular.copy($scope.user);
        $scope.userPicture = uploader.files[0];
        let imgVersion;
        if (userCopy.profPic) {
            const oldVersion = userCopy.profPic.split('v=')[1];
            imgVersion = !isNaN(oldVersion) ? Number(oldVersion) + 1 : 1;
        } else {
            imgVersion = 1;
        }
        let formData = new FormData();
        const userId = $scope.user.id;
        formData.append(userId, $scope.userPicture);
        ConvertPictureToUrl.create({
            realm: $scope.realm.realm,
            userId: $scope.user.id
        }, formData, function(res) {
            if(res.url){
                $scope.user.profPic = res.url + '?v=' +imgVersion;
            } 
            User.update({
                realm: Current.realm.realm,
                userId: $scope.user.id
            }, $scope.user, function () {        
                user = angular.copy($scope.user);
                $scope.user.profPic = $scope.dashboardUrl + "/" + $scope.user.profPic;
                if(user.id === $scope.auth.user.userId){
                    $scope.auth.user.profPic = $scope.dashboardUrl + "/" + user.profPic;
                }
                if (uploader.files && uploader.files[0]) {
                    $('#profileImage').attr('src',
                        window.URL.createObjectURL(uploader.files[0]));
                }
                Notifications.success($translate.instant("profile-pic-updated"));
            });
        },function(err){
            Notifications.err(err);
            $route.reload();
            console.log(err)
        });
    }

    $scope.impersonate = function() {
        UserImpersonation.save({realm : $scope.realmId, user: $scope.userId}, function (data) {
           if (data.sameRealm) {
                   window.location = data.redirect;
                } else {
                  window.open(data.redirect, "_blank");
               }
        });
    };

    $scope.addPassword = function(){ 
        PasswordRegDialog.open('Add Password' , '' , function(newPasswd,confirmNewPasswd){
            //trigger API req
            console.log(newPasswd + confirmNewPasswd);
            UserCredentials.resetPassword({ realm: $scope.realmId, userId: $scope.userId }, { type : "password", value : newPasswd, temporary: $scope.temporaryPassword }, function() {
                Notifications.success($translate.instant("password-updated"));
                $route.reload();
            });
        })
        
    }

    $scope.changePassword = function(){
        PasswordRegDialog.open('Change Password' , '' , function(newPasswd,confirmNewPasswd){
            //trigger API req
            UserCredentials.resetPassword({ realm: $scope.realmId, userId: $scope.userId}, { type : "password", value : newPasswd, temporary: $scope.temporaryPassword }, function() {
                Notifications.success($translate.instant("password-updated"));
                $route.reload();
            });
        })    
    }

    $scope.resetPassword = function(){
        InputDialog.asksForClientToResetPassword($translate.instant('reset-password'), $translate.instant('sure-to-reset-password'), function(clientId){
            UserCredentials.resetPassword({ realm: $scope.realmId, userId: $scope.userId,client_id: clientId }, { type : "password", value : null, temporary: true }, function() {
                Notifications.success($translate.instant("password-has-been-reset"));
                $route.reload();
            });
        });
    }

    $scope.isClickableAction = true;
    $scope.removeUser = function() {
        if($scope.isClickableAction){
            $scope.isClickableAction = false;
                        Dialog.confirmDelete( $scope.realm.mode === 'bo' ? $scope.user.username : $scope.user.username, 'user', function() {
                $scope.user.$remove({
                    realm : Current.realm.realm,
                    userId : $scope.user.id
                }, function() {
                    $location.url("/realms/" + Current.realm.realm + "/users");
                    Notifications.success($translate.instant("user.remove.success"));
                }, function() {
                    Notifications.error("User couldn't be deleted");
                });
                $scope.isClickableAction = true;
            }, function () {
                $scope.isClickableAction = true;
            });
            document.addEventListener('keydown', function(event) {
                if (event.key === 'Escape' || event.keyCode === 27) {
                  $scope.isClickableAction = true;
                }
            });
        }
    };   
    $scope.tooltipTxt = $translate.instant("copy");

    $scope.initCopyICon = () => {
        $(".copy-paste-icon").click(function(){
            $scope.tooltipTxt = $translate.instant("copied");
            $(".copy-paste-icon").mouseleave(function(){
                $scope.tooltipTxt = $translate.instant("copy");
            });
        });
    }
    
    $scope.copyToClipboard = function(element){
        /* Creates the empty input element and set the value of coming input field */
        var $temp = $("<input>");
        $("body").append($temp);
         /* Select the Value in Input field */
        $temp.val($(element).text().trim()).select();
        /* Copy the text inside the text field */
        document.execCommand("copy");
        $temp.remove();
    }
});

module.controller('AdminTabCtrl', function ($scope, User, UserCredentials, ConvertPictureToUrl, UserImpersonation, $location, $route, Dialog, PasswordRegDialog, Notifications, Current, Auth, $translate) {
    $scope.auth = Auth;
    if ($scope.user.profPic) {
        $scope.user.profPic = getUpdatedImgData($scope.user.profPic);
    }
    $scope.mode = Current.realm.mode;
    $scope.userhasPassword = false;
    $scope.temporaryPassword = false;
    $scope.userId = $scope.user.id ? $scope.user.id : $route.current.params.user;
    $scope.realmId = Current.realm && Object.keys(Current.realm).length > 0 ? Current.realm.realm : $route.current.params.realm;
    $scope.create = !$scope.userId;
    $scope.dashboardUrl = dashboardUrl;
    $("#profileImage").click(function () {
        $("#imageUpload").click();
    });

    $("#imageUpload").change(function (e) {
        if (this.files[0]) {
            fasterPreview(this);
        }
    });

    function fasterPreview(uploader) {
        var userCopy = angular.copy($scope.user);
        $scope.userPicture = uploader.files[0];
        let imgVersion;
        if (userCopy.profPic) {
            const oldVersion = userCopy.profPic.split('v=')[1];
            imgVersion = !isNaN(oldVersion) ? Number(oldVersion) + 1 : 1;
        } else {
            imgVersion = 1;
        }
        let formData = new FormData();
        const userId = $scope.user.id;
        formData.append(userId, $scope.userPicture);
        ConvertPictureToUrl.create({
            realm: $scope.realm.realm,
            userId: $scope.user.id
        }, formData, function (res) {
            if (res.url) {
                $scope.user.profPic = res.url + '?v=' + imgVersion;
            }
            User.update({
                realm: Current.realm.realm,
                userId: $scope.user.id
            }, $scope.user, function () {
                user = angular.copy($scope.user);
                $scope.user.profPic = $scope.dashboardUrl + "/" + $scope.user.profPic;
                if (user.id === $scope.auth.user.userId) {
                    $scope.auth.user.profPic = $scope.dashboardUrl + "/" + user.profPic;
                }
                if (uploader.files && uploader.files[0]) {
                    $('#profileImage').attr('src', window.URL.createObjectURL(uploader.files[0]));
                }
                Notifications.success($translate.instant("profile-pic-updated"));
            });
        }, function (err) {
            Notifications.err(err);
            $route.reload();
            console.log(err);
        });
    }

    $scope.impersonate = function () {
        UserImpersonation.save({ realm: $scope.realmId, user: $scope.userId }, function (data) {
            if (data.sameRealm) {
                window.location = data.redirect;
            } else {
                window.open(data.redirect, "_blank");
            }
        });
    };

    $scope.addPassword = function () {
        console.log("ADD PWD");
        PasswordRegDialog.open('Add Password', '', function (newPasswd, confirmNewPasswd) {
            //trigger API req
            console.log(newPasswd + confirmNewPasswd);
            UserCredentials.resetPassword({ realm: $scope.realmId, userId: $scope.userId }, { type: "password", value: newPasswd, temporary: $scope.temporaryPassword }, function () {
                Notifications.success($translate.instant("password-updated"));
                $route.reload();
            });
        });

    };

    $scope.changePassword = function () {
        PasswordRegDialog.open('Change Password', '', function (newPasswd, confirmNewPasswd) {
            console.log(newPasswd + confirmNewPasswd);
            //trigger API req
            UserCredentials.resetPassword({ realm: $scope.realmId, userId: $scope.userId }, { type: "password", value: newPasswd, temporary: $scope.temporaryPassword }, function () {
                Notifications.success($translate.instant("password-updated"));
                $route.reload();
            });
        });
    };



    $scope.removeUser = function () {
        Dialog.confirmDelete($scope.user.username, 'user', function () {
            $scope.user.$remove({
                realm: Current.realm.realm,
                userId: $scope.user.id
            }, function () {
                $location.url("/realms/" + Current.realm.realm + "/dashboard-admins");
                Notifications.success($translate.instant("user.remove.success"));
            }, function () {
                Notifications.error("User couldn't be deleted");
            });
        });
    };   
    $scope.tooltipTxt = $translate.instant("copy");

    $scope.initCopyICon = () => {
        $(".copy-paste-icon").click(function(){
            $scope.tooltipTxt = $translate.instant("copied");
            $(".copy-paste-icon").mouseleave(function(){
                $scope.tooltipTxt = $translate.instant("copy");
            });
        });
    }
    $scope.copyToClipboard = function (element) {
        /* Creates the empty input element and set the value of coming input field */
        var $temp = $("<input>");
        $("body").append($temp);
        /* Select the Value in Input field */
        $temp.val($(element).text().trim()).select();
        /* Copy the text inside the text field */
        document.execCommand("copy");
        $temp.remove();
    };
});

function loadUserStorageLink(realm, user, console, Components, UserStorageOperations, $scope, $location) {
    if (user.federationLink) {
        console.log("federationLink is not null. It is " + user.federationLink);

        if ($scope.access.viewRealm) {
            Components.get({ realm: realm.realm, componentId: user.federationLink }, function (link) {
                $scope.federationLinkName = link.name;
                $scope.federationLink = "#/realms/" + realm.realm + "/user-storage/providers/" + link.providerId + "/" + link.id;
            });
        } else {
            // KEYCLOAK-4328
            UserStorageOperations.simpleName.get({ realm: realm.realm, componentId: user.federationLink }, function (link) {
                $scope.federationLinkName = link.name;
                $scope.federationLink = $location.absUrl();
            });
        }

    }

    if (user.origin) {
        if ($scope.access.viewRealm) {
            Components.get({ realm: realm.realm, componentId: user.origin }, function (link) {
                $scope.originName = link.name;
                $scope.originLink = "#/realms/" + realm.realm + "/user-storage/providers/" + link.providerId + "/" + link.id;
            });
        }
        else {
            // KEYCLOAK-4328
            UserStorageOperations.simpleName.get({ realm: realm.realm, componentId: user.origin }, function (link) {
                $scope.originName = link.name;
                $scope.originLink = $location.absUrl();
            });
        }
    }
};

module.controller('UserDetailCtrl', function ($scope, realm, user, BruteForceUser, User, Components, UserImpersonation, UserStorageOperations, UserInviteResend, $location, Dialog, Notifications, ValidationUtils, ConfirmExitDialog, AdditonalClaimsDialog, $route, Current, UserCredentials, UserAdditonalClaims, $translate, $window) {
    $scope.realm = realm;
    $scope.loaderTypeName = 'userTabLoader';
    $scope.stage = 1;
    $scope.create = !user.id;
    $scope.searchLoaded = true;
    $scope.editUsername = $scope.create || $scope.realm.editUsernameAllowed;
    $scope.credentials = [];
    $scope.ClickableAction = true;
    $scope.userAttributes = angular.copy(user.attributes);
    $scope.noWorkflowMetaData = 0

    if ($location.url().endsWith('user-attributes')) {
        Object.keys($scope.userAttributes).map(key =>{
            if (key.endsWith("_organizations") || key.endsWith("_projects")) {
                delete $scope.userAttributes[key];
                $scope.noWorkflowMetaData++;
            }
        })
    }

    $scope.newAttribute = {
        key : '',
        value : ''
    }

    if ($scope.create) {
        $scope.user = { enabled: true, attributes: {} };
    } else {
        if (!user.attributes) {
            user.attributes = {};
        }
        $scope.searchLoaded = true;
        convertAttributeValuesToString(user);
        $scope.user = angular.copy(user);
        $scope.userId = $scope.user.id ? $scope.user.id : $route.current.params.user;
        $scope.realmId = Current.realm && Object.keys(Current.realm).length > 0 ? Current.realm.realm : $route.current.params.realm;
        if (!user.profPic) {
            const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
            $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
        }
        $scope.dataTime = timeAgo((user.createdTimestamp / 1000));
        $scope.impersonate = function () {
            UserImpersonation.save({ realm: realm.realm, user: $scope.user.id }, function (data) {
                data.redirect = data.redirect.replace('applications', '');

                if (data.sameRealm) {
                    window.location = data.redirect;
                } else {
                    window.open(data.redirect, "_blank");
                }
            });
        };

        loadUserStorageLink(realm, user, console, Components, UserStorageOperations, $scope, $location);

        $scope.temporarilyDisabled = false;
        var isDisabled = function () {
            BruteForceUser.get({ realm: realm.realm, userId: user.id }, function (data) {
                console.log('here in isDisabled ' + data.disabled);
                $scope.temporarilyDisabled = data.disabled;
            });
        };

        isDisabled();

        $scope.unlockUser = function () {
            BruteForceUser.delete({ realm: realm.realm, userId: user.id }, function (data) {
                isDisabled();
            });
        };
    }

    function timeAgo(timestamp, DWMY_timeAgo = true) { // DWMY_timeAgo = [Days,Weeks,Months,Years] ago
        var momentDate = moment.unix(timestamp), // Getting date and time with unix timestamp
            dateTime = {
                seconds: moment().diff(momentDate, 'seconds'),
                minutes: moment().diff(momentDate, 'minutes'),
                hours: moment().diff(momentDate, 'hours'),
                days: moment().diff(momentDate, 'days'),
                weeks: moment().diff(momentDate, 'weeks'),
                months: moment().diff(momentDate, 'months'),
                years: moment().diff(momentDate, 'years')
            },
            datetime = dateTime.date + ' at ' + dateTime.time;
        outputTime = '';

        if (dateTime.seconds > 0) {
            outputTime = '1 second ago';
        }
        if (dateTime.seconds > 1) {
            outputTime = `${dateTime.seconds}  seconds ago`;
        }

        if (dateTime.minutes === 1) {
            outputTime = '1 minute ago';
        }
        if (dateTime.minutes > 1) {

            outputTime = `${dateTime.minutes} minutes ago`;
        }

        if (dateTime.hours === 1) {
            outputTime = '1 hour ago';
        }
        if (dateTime.hours > 1) {
            outputTime = `${dateTime.hours} hours ago`;
        }

        if (dateTime.days === 1) {
            if (DWMY_timeAgo) {
                outputTime = '1 day ago';
            } else {
                outputTime = datetime;
            }
        }
        if (dateTime.days > 1) {
            if (DWMY_timeAgo) {
                outputTime = `${dateTime.days} days ago`;
            } else {
                outputTime = datetime;
            }
        }

        //weeks
        if (dateTime.weeks === 1) {
            if (DWMY_timeAgo) {
                outputTime = `${dateTime.weeks} Week`;
            } else {
                outputTime = datetime;
            }
        }
        if (dateTime.weeks > 1) {
            if (DWMY_timeAgo) {
                outputTime = `${dateTime.weeks} Weeks`;
            } else {
                outputTime = datetime;
            }
        }

        if (dateTime.months == 1) {
            if (DWMY_timeAgo) {
                outputTime = '1 month ago';
            } else {
                outputTime = datetime;
            }
        }
        if (dateTime.months > 1) {
            if (DWMY_timeAgo) {
                outputTime = dateTime.months + ' months ago';
            } else {
                outputTime = datetime;
            }
        }

        if (dateTime.years == 1) {
            if (DWMY_timeAgo) {
                outputTime = '1 year ago';
            } else {
                outputTime = datetime;
            }
        }
        if (dateTime.years > 1) {
            if (DWMY_timeAgo) {
                outputTime = dateTime.years + ' years ago';
            } else {
                outputTime = datetime;
            }
        }
        return outputTime;
    }

    $scope.changed = false; // $scope.create;

    $scope.$watch('user.profPic', function () {
        if ($scope.user.profPic !== user.profPic) {
            user.profPic = $scope.user.profPic;
        }
    }, true);
    $scope.$watch('user', function () {
        if ($scope.realm.mode === 'bo') {
            Object.keys($scope.user).forEach((userProperty)=>{
                if($scope.user[userProperty] === '' || $scope.user[userProperty] === null || $scope.user[userProperty] === undefined ) {
                    delete $scope.user[userProperty]
                }
                if(user[userProperty] === '' || user[userProperty] === null || user[userProperty] === undefined ) {
                    delete user[userProperty]
                }
            })
        }
        if (!angular.equals($scope.user, user) && $scope.user.username && $scope.user.email && ValidationUtils.validateEmail($scope.user.email)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);
    $scope.$watch('user', function () {
        if (!angular.equals($scope.user, user)) {
            $scope.enableCancelBtn = true;
        } else {
            $scope.enableCancelBtn = false;
        }
    }, true);
    $scope.$watch('newAttribute.key', function () {
        if ($scope.newAttribute.key in $scope.user.attributes ) {
            $scope.isKeyExist=true;
         }else{
            $scope.isKeyExist=false;
         }
    }, true);

    if(iti){
        if ($scope.user.mobileNumberRegion != null) {
            iti.setCountry($scope.user.mobileNumberRegion);
        } else if ($scope?.realm?.defaultRegion) {
            iti.setCountry($scope.realm.defaultRegion);
        }
    }

    //Reset Password 
    $scope.resetPassword = function(){
        Dialog.confirm($translate.instant('reset-password'), $translate.instant('sure-to-reset-password'), function() {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.id}, { type : "password", value : null, temporary: true }, function() {
                Notifications.success($translate.instant("password-has-been-reset"));
                $scope.openRedirect = true;
                $route.reload();
            });
        });
    };


    $scope.resendInvite = function () {
        $scope.stage = 2;
        UserInviteResend.save({ realm: realm.realm, userId: user.id },
            function (successRes) {
                Notifications.success($translate.instant("invite-resent-success"));
                $scope.stage = 1;
            }, function (errRes) {
                Notifications.error(errRes);
                $scope.stage = 1;
            });
    };

    var userCopy=angular.copy($scope.user);

    $scope.save = function () {
        //check for saving mobilenumber and mobilenumber region
        if ($scope.user.mobileNumber?.trim().length > 0 && iti) {
            $scope.user.mobileNumberRegion = iti.getSelectedCountryData().iso2;
        } else {
            delete $scope.user.mobileNumber;
            delete $scope.user.mobileNumberRegion;
        }
        $scope.stage = 2;
        $('#cancelBtn').attr('disabled', 'disabled');
        convertAttributeValuesToLists();
        if ($scope.create) {
            User.save({
                realm: realm.realm
            }, $scope.user, function (data, headers) {
                $scope.stage = 1;
                $('#cancelBtn').removeAttr('disabled', 'disabled');
                $scope.changed = false;
                convertAttributeValuesToString($scope.user);
                user = angular.copy($scope.user);
                var l = headers().location;
                $scope.searchLoaded = false;
                var id = l.substring(l.lastIndexOf("/") + 1);
                Notifications.success($translate.instant("user.create.success"));
                $location.url("/realms/" + realm.realm + "/users/" + id);
            }, function (error) {

                $scope.stage = 1;
                Notifications.error($translate.instant(error.data.errorMessage));
                $('#cancelBtn').removeAttr('disabled', 'disabled');
            });
        } else {
            $scope.user.profPic = user.profPic;
            var requestPayload = $scope.isUserAttributesLoaded ? $scope.user : userCopy ;
            User.update({
                realm: realm.realm,
                userId: $scope.user.id
            }, requestPayload, function () {
                $scope.stage = 1;
                $('#cancelBtn').removeAttr('disabled', 'disabled');
                $scope.changed = false;
                $scope.allowAddAttribute = false;
                convertAttributeValuesToString(requestPayload);
                user = angular.copy(requestPayload);
                $scope.isAddUserAttributeLoading = false;
                $scope.isUserAttributesLoaded=true;
                Notifications.success($translate.instant("user.edit.success"));
                $route.reload();
            }, function (error) {
                $scope.stage = 1;
                $scope.isAddUserAttributeLoading = false;
                $scope.isUserAttributesLoaded=true;
                Notifications.error($translate.instant(error.data.errorMessage));
                $('#cancelBtn').removeAttr('disabled', 'disabled');
            });
        }
    };

    function convertAttributeValuesToLists() {
        var requestPayload = $scope.isUserAttributesLoaded ? $scope.user : userCopy ;
        var attrs = requestPayload.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "string") {
                var attrVals = attrs[attribute].split("##");
                attrs[attribute] = attrVals;
            }
        }
    }
    
    function convertAttributeValuesToString(user) {
        var attrs = user.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "object") {
                var attrVals = attrs[attribute].join("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    $scope.openRedirect = false;
    $scope.reset = function () {
        $scope.user = angular.copy(user);
        $scope.changed = false;
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/users");
    };

    $scope.isUserAttributesLoaded=true;
    $scope.addAttribute = function () {
        $scope.isUserAttributesLoaded=false;
        $scope.isAddUserAttributeLoading=true;
        userCopy.attributes[$scope.newAttribute.key] = $scope.newAttribute.value;
        delete $scope.newAttribute;
        $scope.save();
    };

    $scope.removeAttribute = function(key){
        if($scope.ClickableAction){
            $scope.ClickableAction = false;
            Dialog.confirmDelete(key, 'attribute', function () {
                $scope.selectedUserAttribute=key;
                $scope.isUserAttributesLoaded=false;
                delete userCopy.attributes[key];
                $scope.changed = true;
                $scope.save();
                $scope.ClickableAction = true;
            },
            function () {
                $scope.ClickableAction = true;
            });
            document.addEventListener('keydown', function(event) {
                if (event.key === 'Escape' || event.keyCode === 27) {
                  $scope.ClickableAction = true;
                }
            });
        }
    }

    $scope.additionalClaims;
    $scope.showAdditionalClaims = function(verificationId){
        if($scope.ClickableAction){
            $scope.ClickableAction = false;
            verificationId = "bo-" + verificationId;
            UserAdditonalClaims.query({realm: realm.realm, verificationId, user:user.id},function(data){
                $scope.additionalClaims = data
                AdditonalClaimsDialog.open(verificationId, $scope.additionalClaims, function(setModalClickable){
                    $scope.ClickableAction = setModalClickable;
                });
            },function(err) {
                $scope.ClickableAction = true;
            });
        }
    }

    $scope.attributeLength = function () {
        return Object.keys($scope.userAttributes).length;
    };

    $scope.allowAddAttribute = false;

    $scope.addAttributeButton = function () {
        $scope.allowAddAttribute = true;
    };

    $scope.noAttributesSaved = function () {
        $scope.tempUser = angular.copy(user);
        return Object.keys($scope.tempUser.attributes).length - $scope.noWorkflowMetaData;
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});
module.controller('AdminDetailCtrl', function ($scope, realm, user, BruteForceUser, User, AdminUser,
    Components,
    UserImpersonation, RequiredActions,
    UserStorageOperations, UserInviteResend, Notifications,Auth,
    $location, $http, Dialog, Notifications, UserCredentials,InputDialog,AccountCreator, $route, ValidationUtils, ConfirmExitDialog, Current, $translate, $window, serverInfo) {
    $scope.realm = realm;
    $scope.loaderTypeName = 'userTabLoader';
    $scope.create = !user.id;
    $scope.stage = 1;
    $scope.editUsername = $scope.create || $scope.realm.editUsernameAllowed;
    $scope.openRedirect = false;
    $scope.Auth = Auth;
    $scope.credentials = [];
    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";
    $scope.isRootTenant = serverInfo.rootAccountName === realm.realm;

    $scope.newAttribute = {
        key : '',
        value : ''
    }
    
    if ($scope.create) {
        $scope.user = { enabled: true, attributes: {} };
    } else {
        if (!user.attributes) {
            user.attributes = {};
        }
        convertAttributeValuesToString(user);
        $scope.dataTime = moment(user.createdTimestamp).startOf("second").fromNow();
        $scope.user = angular.copy(user);
        $scope.userId = $scope.user.id ? $scope.user.id : $route.current.params.user;
        $scope.realmId = Current.realm && Object.keys(Current.realm).length > 0 ? Current.realm.realm : $route.current.params.realm;
        if (!user.profPic) {
            const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
            $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
        }
        $scope.oldMfaEnabledValue = angular.copy($scope.user.mfaEnabled);
        
        $scope.impersonate = function () {
            UserImpersonation.save({ realm: realm.realm, user: $scope.user.id }, function (data) {
                data.redirect = data.redirect.replace('applications', '');
                $scope.openRedirect = true;
                if (data.sameRealm) {
                    window.location = data.redirect;
                } else {
                    window.open(data.redirect, "_blank");
                }
            });
        };

        loadUserStorageLink(realm, user, console, Components, UserStorageOperations, $scope, $location);

        $scope.temporarilyDisabled = false;
        var isDisabled = function () {
            BruteForceUser.get({ realm: realm.realm, userId: user.id }, function (data) {
                $scope.temporarilyDisabled = data.disabled;
            });
        };

        isDisabled();

        $scope.unlockUser = function () {
            BruteForceUser.delete({ realm: realm.realm, userId: user.id }, function (data) {
                isDisabled();
            });
        };
    }

    $scope.changed = false; // $scope.create;
    
    $scope.resetPassword = function(){
        Dialog.confirm($translate.instant('reset-password'), $translate.instant('sure-to-reset-password'), function() {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.id,client_id : 'dashboard'}, { type : "password", value : null, temporary: true }, function() {
                Notifications.success($translate.instant("password-has-been-reset"));
                $scope.openRedirect = true;
                $route.reload();
            });
        });
    };

    $scope.showChangeAccountCreatorOption = Auth.user.isAccountCreator && Auth.user.userId != user.id  && user.attributes && (!user.attributes.qv_is_account_creator || user.attributes.qv_is_account_creator == 'false')
    $scope.changeAccountCreator = function () {
        AccountCreator.sendOtp({ realm: realm.realm, userId: user.id }, function () {
            InputDialog.changeAccountCreator(Current.realm.realm,user, function () {
            });
        })
    }

    $scope.resendInvite = function () {
        $scope.stage = 2;
        UserInviteResend.save({ realm: realm.realm, userId: user.id },
            function (successRes) {
                Notifications.success($translate.instant("invite-resent-success"));
                $scope.stage = 1;
            }, function (errRes) {
                Notifications.error(errRes);
                $scope.stage = 1;
            });
    };

    $scope.$watch('user.profPic', function () {
        if ($scope.user.profPic !== user.profPic) {
            user.profPic = $scope.user.profPic;
        }
    }, true);
    $scope.$watch('user', function () {
        if (!angular.equals($scope.user, user) && $scope.user.username && $scope.user.email && ValidationUtils.validateEmail($scope.user.email)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);
    $scope.$watch('user', function () {
        if ($scope.user.email && ValidationUtils.validateEmail($scope.user.email)) {
            $scope.createBtnchanged = true;
        } else {
            $scope.createBtnchanged = false;
        }
    }, true);
    $scope.$watch('user', function () {
        if (!angular.equals($scope.user, user)) {
            $scope.enableCancelBtn = true;
        } else {
            $scope.enableCancelBtn = false;
        }
    }, true);
    $scope.$watch('newAttribute.key', function () {
        if ($scope.newAttribute.key in $scope.user.attributes ) {
            $scope.isKeyExist=true;
         }else{
            $scope.isKeyExist=false;
         }
    }, true);

    if(iti){
        if ($scope.user.mobileNumberRegion != null) {
            iti.setCountry($scope.user.mobileNumberRegion);
        } else if ($scope?.realm?.defaultRegion) {
            iti.setCountry($scope.realm.defaultRegion);
        }
    }
    $scope.showNoAttributeCard = function () {
        return $scope.checkAttributeEmpty($scope.user);
    };

    var userCopy=angular.copy($scope.user);
    $scope.isAddUserAttributeLoading = false;

    $scope.save = function () {
        //check for saving mobilenumber and mobilenumber region
        if ($scope.user.mobileNumber?.trim().length > 0 && iti) {
            $scope.user.mobileNumberRegion = iti.getSelectedCountryData().iso2;
        } else {
            delete $scope.user.mobileNumber;
            delete $scope.user.mobileNumberRegion;
        }
        $scope.stage = 2;
        convertAttributeValuesToLists();
        if ($scope.create) {
            AdminUser.save({
                realm: realm.realm
            }, $scope.user, function (data, headers) {
                $scope.stage = 1;
                $scope.changed = false;
                convertAttributeValuesToString($scope.user);
                user = angular.copy($scope.user);
                var l = headers().location;

                console.debug("Location == " + l);

                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.realm + "/admin/users/" + id);
                Notifications.success($translate.instant("admin-created"));
            }, function () {
                $scope.stage = 1;
                Notifications.error($translate.instant("user-exists-with-same-username"));
            });
        } else {
            var requestPayload = $scope.isUserAttributesLoaded ? $scope.user : userCopy ;
            $scope.user.profPic = user.profPic;
            var requestPayload = $scope.isUserAttributesLoaded ? $scope.user : userCopy ;
            User.update({
                realm: realm.realm,
                userId: $scope.user.id
            }, requestPayload, function () {
                $scope.changed = false;
                $scope.allowAddAttribute = false;
                convertAttributeValuesToString(requestPayload);
                user = angular.copy(requestPayload);
                $scope.oldMfaEnabledValue = angular.copy($scope.user.mfaEnabled);
                $scope.stage = 1;
                $scope.isAddUserAttributeLoading = false;
                $scope.isUserAttributesLoaded=true;
                Notifications.success($translate.instant("user.edit.success"));
                $route.reload();
            }, function () {
                $scope.stage = 1;
            });
        }
    };

    function convertAttributeValuesToLists() {
        var requestPayload = $scope.isUserAttributesLoaded ? $scope.user : userCopy ;
        var attrs = requestPayload.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "string") {
                var attrVals = attrs[attribute].split("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    function convertAttributeValuesToString(user) {
        var attrs = user.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "object") {
                var attrVals = attrs[attribute].join("##");
                attrs[attribute] = attrVals;
            }
        }
    }

    $scope.reset = function () {
        $scope.user = angular.copy(user);
        $scope.changed = false;
    };

    $scope.cancel = function () {
        $scope.openRedirect = true;
        $location.url("/realms/" + realm.realm + "/dashboard-admins");
    };

    $scope.isUserAttributesLoaded=true;

    $scope.addAttribute = function () {
        $scope.isUserAttributesLoaded=false;
        $scope.isAddUserAttributeLoading = true;
        userCopy.attributes[$scope.newAttribute.key] = $scope.newAttribute.value;
        delete $scope.newAttribute;
        $scope.save();
    };

    $scope.removeAttribute = function (key) {
        $scope.selectedUserAttribute=key;
        $scope.isUserAttributesLoaded=false;
        delete userCopy.attributes[key];
        $scope.changed = true;
        $scope.save();
    };

    $scope.attributeLength = function () {
        return Object.keys($scope.user.attributes).length;
    };

    $scope.allowAddAttribute = false;

    $scope.addAttributeButton = function () {
        $scope.allowAddAttribute = true;
    };

    $scope.noAttributesSaved = function () {
        return $scope.checkAttributeEmpty(user);
    };
    $scope.checkAttributeEmpty = function (userData) {
        if ((Object.keys(userData.attributes).length === 1 && userData.attributes.hasOwnProperty('qv_is_account_creator')) || !Object.keys(userData.attributes).length) {
            return true;
        }
        return false;
    };
    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});
module.controller('UserCredentialsCtrl', function ($scope, realm, user, $route, $location, RequiredActions, User, UserExecuteActionsEmail,
    UserCredentials, Notifications, Dialog, TimeUnit2, Components, UserStorageOperations, $translate,$modal) {
    $scope.loaderTypeName = 'tableListLoader';
    $scope.hasPassword = false;
    $scope.searchLoaded;
    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";
    $scope.supportedDeviceTypes = ['DEVICE_TOKEN', 'ASHIELD', 'BIOMETRIC', 'webauthn-passwordless'];

    if (!user.profPic) {
        const profPicBase64 = generateAvatar(user.username ? user.username : user.email);
        $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
    }

    $scope.parseData = function(credentialData) {
        return JSON.parse(credentialData);
    }

    $scope.showData = function(credentialData) {
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/user-credential-data.html',
            controller: 'UserCredentialsDataModalCtrl',
            resolve: {
                credentialData: function () {
                    return credentialData;
                }
            }
        })
    }

    loadCredentials();

    //loadUserStorageLink(realm, user, console, Components, UserStorageOperations, $scope, $location);

    $scope.getUserStorageProviderName = function () {
        return user.federationLink ? $scope.federationLinkName : $scope.originName;
    };

    $scope.getUserStorageProviderLink = function () {
        return user.federationLink ? $scope.federationLink : $scope.originLink;
    };

    $scope.keys = function (object) {
        return object ? Object.keys(object) : [];
    };

    $scope.updateCredentialLabel = function (credential) {
        UserCredentials.updateCredentialLabel({ realm: realm.realm, userId: user.id, credentialId: credential.id }, {
            'id': credential.id,
            'userLabel': credential.userLabel ? credential.userLabel : "",
            // We JSONify the credential data
            'credentialData': JSON.stringify(credential.credentialData)
        }, function () {
            Notifications.success($translate.instant("user.credential.update.success"));
        }, function (err) {
            Notifications.error("Error while updating the credential. See console for more information.");
            console.log(err);
        });
    };

    $scope.deleteCredential = function (credential) {
        Dialog.confirm($translate.instant('user.credential.remove.confirm.title'), $translate.instant('user.credential.remove.confirm.message'), function () {
            UserCredentials.deleteCredential({ realm: realm.realm, userId: user.id, credentialId: credential.id }, null, function () {
                Notifications.success($translate.instant("user.credential.remove.success"));
                $route.reload();
            }, function (err) {
                Notifications.error($translate.instant('user.credential.remove.error'));
                console.log(err);
            });
        });
    };

    $scope.enableCredential = function (credential) {
        Dialog.confirm($translate.instant('enable-device'), $translate.instant('enable-device-txt'), function () {
            UserCredentials.enableCredential({ realm: realm.realm, userId: user.id, credentialId: credential.id }, null, function () {
                Notifications.success($translate.instant("user.credential.enable.success"));
                $route.reload();
            }, function (err) {
                Notifications.error($translate.instant('error-enable-device'));
                console.log(err);
            });
        });
    };

    $scope.disableCredential = function (credential) {
        Dialog.confirm($translate.instant('user.credential.disable.confirm.title'), $translate.instant('user.credential.disable.confirm.message'), function () {
            UserCredentials.disableCredential({ realm: realm.realm, userId: user.id, credentialId: credential.id }, null, function () {
                Notifications.success($translate.instant("user.credential.disable.success"));
                $route.reload();
            }, function (err) {
                Notifications.error($translate.instant('error-disable-device'));
                console.log(err);
            });
        });
    };

    $scope.markAsLastUsedDevice = function (credential) {
        Dialog.confirm($translate.instant('update-device'), $translate.instant('update-device-txt'), function () {
            UserCredentials.markTokenCredentialAsLastUsed({ realm: realm.realm, userId: user.id, credentialId: credential.id }, null, function () {
                Notifications.success($translate.instant("user.credential.device.latest.success"));
                $route.reload();
            }, function (err) {
                Notifications.error($translate.instant('error-update-device'));
                console.log(err);
            });
        });
    }

    $scope.moveUp = function (credentials, index) {
        // Safety first
        if (index == 0) {
            return;
        } else if (index == 1) {
            UserCredentials.moveToFirst(
                {
                    realm: realm.realm,
                    userId: user.id,
                    credentialId: credentials[index].id
                },
                function () {
                    $route.reload();
                },
                function (err) {
                    Notifications.error("Error while moving the credential to top. See console for more information.");
                    console.log(err);
                });

        } else {
            UserCredentials.moveCredentialAfter(
                {
                    realm: realm.realm,
                    userId: user.id,
                    credentialId: credentials[index].id,
                    newPreviousCredentialId: credentials[index - 2].id
                },
                function () {
                    $route.reload();
                },
                function (err) {
                    Notifications.error("Error while moving the credential up. See console for more information.");
                    console.log(err);
                });
        }
    };

    $scope.moveDown = function (credentials, index) {
        // Safety first
        if (index == credentials.length - 1) {
            return;
        }
        UserCredentials.moveCredentialAfter(
            {
                realm: realm.realm,
                userId: user.id,
                credentialId: credentials[index].id,
                newPreviousCredentialId: credentials[index + 1].id
            },
            function () {
                $route.reload();
            },
            function (err) {
                Notifications.error("Error while moving the credential down. See console for more information.");
                console.log(err);
            });
    };

    $scope.realm = realm;
    $scope.user = angular.copy(user);
    $scope.temporaryPassword = true;

    $scope.isTotp = false;
    if (!!user.totp) {
        $scope.isTotp = user.totp;
    }
    // ID - Name map for required actions. IDs are enum names.
    // RequiredActions.query({ realm: realm.realm }, function (data) {
    //     $scope.userReqActionList = [];
    //     for (var i = 0; i < data.length; i++) {
    //         console.log("listed required action: " + data[i].name);
    //         if (data[i].enabled) {
    //             var item = data[i];
    //             $scope.userReqActionList.push(item);
    //         }
    //     }

    // });

    function loadCredentials() {
        UserCredentials.getCredentials({ realm: realm.realm, userId: user.id }, null, function (credentials) {
            $scope.searchLoaded = false
            let creds = credentials.map(function (c) {
                // We de-JSONify the credential data
                if (c.credentialData) {
                    c.credentialData = JSON.parse(c.credentialData);
                }
                if (c.type == 'password') {
                    $scope.hasPassword = true;
                }
                // checking for empty objects
                if (angular.equals({}, c)) return null;
                return c;
            });

            // filtering null elements and supported types.
            $scope.credentials = creds.filter((cred) => cred && $scope.supportedDeviceTypes.includes(cred.type))

            let mobileDevices = [];
            let desktopDevices = [];

            creds.map((cred) => {
                if (cred && cred.type.toLowerCase() !== 'password' && cred.type.toLowerCase() !== 'mfa_recovery' &&  cred.type.toLowerCase() != 'otp' && cred.type.toLowerCase() !== 'password-history') {
                    if (cred.devicePlatform != undefined && (cred.devicePlatform.toLowerCase() === "ios" || cred.devicePlatform.toLowerCase() === "android")) {
                        mobileDevices.push(cred)
                    } else {
                        desktopDevices.push(cred);
                    }
                }
                $scope.searchLoaded = true;
            });

            markLastUsedToken(mobileDevices, "mobile");
            markLastUsedToken(desktopDevices, "desktop");
            $scope.credentials = {
                "mobileDevices": mobileDevices,
                "desktopDevices": desktopDevices
            }
            
            $scope.credentialsLength = Object.keys($scope.credentials).length
            $scope.isCredentialsCardVisible = new Array($scope.credentialsLength).fill(true);

            $scope.setCredentialsCardVisible = function (index) {
                $scope.isCredentialsCardVisible[index] = !$scope.isCredentialsCardVisible[index];
            }

            sanitizeCredentials($scope.credentials);
            $scope.searchLoaded = true;
        }, function (err) {
            Notifications.error("Error while loading user credentials. See console for more information.");
            console.log(err);
        });

        UserCredentials.getConfiguredUserStorageCredentialTypes({ realm: realm.realm, userId: user.id }, null, function (userStorageCredentialTypes) {
            $scope.userStorageCredentialTypes = userStorageCredentialTypes;
            $scope.hasPassword = $scope.hasPassword || userStorageCredentialTypes.lastIndexOf("password") > -1;
        }, function (err) {
            Notifications.error("Error while loading user storage credentials. See console for more information.");
            console.log(err);
        });
    }

    function markLastUsedToken(creds, deviceType) {
        let tokensBasedOnDeviceType = creds.filter((cred) => cred 
            && cred.enabled 
            && cred.type === 'DEVICE_TOKEN' 
            && (
                deviceType === 'mobile' ? 
                cred.devicePlatform.toLowerCase() === 'android' || cred.devicePlatform.toLowerCase() === 'ios' :
                cred.devicePlatform.toLowerCase() !== 'android' && cred.devicePlatform.toLowerCase() !== 'ios'
            ) && cred.lastUsed);


        if (tokensBasedOnDeviceType.length > 0) {
            let credentialId = tokensBasedOnDeviceType.reduce((minCred, currentCred) => {
                let lastUsed1 = minCred.lastUsed !== null ? minCred.lastUsed : minCred.createdDate;
                let lastUsed2 = currentCred.lastUsed !== null ? currentCred.lastUsed : currentCred.createdDate;
                
                return lastUsed2 > lastUsed1 ? currentCred : minCred;
            }).id;

            $scope.credentials.forEach(cred => {
                if (cred && cred.id == credentialId && cred.type === 'DEVICE_TOKEN' ) {
                    cred.isLastUsed = true;
                    if (tokensBasedOnDeviceType.length > 1) {
                        cred.showLastUsedTag = true;
                    }
                }

            });
        }
    }

    function sanitizeCredentials(credentials) {
        for (var key in credentials) {
            if (credentials.hasOwnProperty(key) && credentials[key].length === 0) {
                delete credentials[key];
            }
        }
    }

    $scope.resetPassword = function () {
        // hit enter without entering both fields - ignore
        if (!$scope.passwordAndConfirmPasswordEntered()) return;

        if ($scope.pwdChange) {
            if ($scope.password != $scope.confirmPassword) {
                Notifications.error("Password and confirmation does not match.");
                return;
            }
        }

        var msgTitle = ($scope.hasPassword ? 'Reset' : 'Set') + ' password';
        var msg = 'Are you sure you want to ' + ($scope.hasPassword ? 'reset' : 'set') + ' a password for the user?';
        var msgSuccess = 'The password has been ' + ($scope.hasPassword ? 'reset.' : 'set.');

        Dialog.confirm(msgTitle, msg, function () {
            UserCredentials.resetPassword({ realm: realm.realm, userId: user.id }, { type: "password", value: $scope.password, temporary: $scope.temporaryPassword }, function () {
                Notifications.success(msgSuccess);
                $scope.password = null;
                $scope.confirmPassword = null;
                $route.reload();
            });
        }, function () {
            $scope.password = null;
            $scope.confirmPassword = null;
        });
    };

    $scope.passwordAndConfirmPasswordEntered = function () {
        return $scope.password && $scope.confirmPassword;
    };

    $scope.disableCredentialTypes = function () {
        Dialog.confirm('Disable credential', 'Are you sure you want to disable these user credential?', function () {
            UserCredentials.disableCredentialTypes({ realm: realm.realm, userId: user.id }, $scope.disableableCredentialTypes, function () {
                $route.reload();
                Notifications.success($translate.instant("user.credential.disable.confirm.success"));
            }, function () {
                Notifications.error("Failed to disable credential");
            });
        });
    };

    $scope.emailActions = [];
    $scope.emailActionsTimeout = TimeUnit2.asUnit(realm.actionTokenGeneratedByAdminLifespan);
    $scope.disableableCredentialTypes = [];

    $scope.sendExecuteActionsEmail = function () {
        if ($scope.changed) {
            Dialog.message("Cannot send email", "You must save your current changes before you can send an email");
            return;
        }
        Dialog.confirm('Send Email', 'Are you sure you want to send email to user?', function () {
            UserExecuteActionsEmail.update({ realm: realm.realm, userId: user.id, lifespan: $scope.emailActionsTimeout.toSeconds() }, $scope.emailActions, function () {
                Notifications.success($translate.instant("user.actions-email.send.confirm.success"));
            }, function () {
                Notifications.error("Failed to send email to user");
            });
        });
    };



    $scope.$watch('user', function () {
        if (!angular.equals($scope.user, user)) {
            $scope.userChange = true;
        } else {
            $scope.userChange = false;
        }
    }, true);

    $scope.$watch('password', function () {
        if (!!$scope.password) {
            $scope.pwdChange = true;
        } else {
            $scope.pwdChange = false;
        }
    }, true);

    $scope.reset = function () {
        $scope.password = "";
        $scope.confirmPassword = "";

        $scope.user = angular.copy(user);

        $scope.isTotp = false;
        if (!!user.totp) {
            $scope.isTotp = user.totp;
        }

        $scope.pwdChange = false;
        $scope.userChange = false;
    };
});


module.controller('UserTransactionsCtrl', function($scope, realm, user, $route, User, Notifications, Dialog, TimeUnit2, Components, $translate,$modal, $http, UserSearchState) {

    $scope.realm = realm;
    $scope.user = angular.copy(user);
    $scope.searchLoaded = false;
    $scope.loaderTypeName = 'tableListLoader';
    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";
    $scope.requestId = '';
    $scope.status = '';
    $scope.totalUserTransactions = '';
    $scope.itemsPerPage = 5;
    $scope.transactionStatusOptions = ['SUCCESS', 'FAILURE', 'ACCESS DENIED', 'PENDING', 'EXPIRED', 'USER CANCELLED']; // Options for transaction status dropdown

    $scope.currentPage = 1;
    $scope.pagination = {
        firstResult: 0,
        maxResults: $scope.itemsPerPage
    };


    $scope.changePage = function(pageNumber) {
        $scope.currentPage = pageNumber;
        $scope.pagination.firstResult = pageNumber === 1 ? 0 : (pageNumber - 1) * $scope.itemsPerPage;
        $scope.pagination.maxResults = $scope.itemsPerPage;
        $scope.getUserTransactions();
    };

    if (!user.profPic) {
            const profPicBase64 = generateAvatar(user.username ? user.username : user.email);
            $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
    }

    $scope.getUserTransactions = function() {
            $scope.status = $scope.status === null ? '' : $scope.status;
            $scope.requestId = $scope.requestId === null ? '': $scope.requestId;
            var url = authUrl + '/admin/realms/' + realm.realm + '/users/' + $scope.user.id + '/transactions';
                              url += '?first=' + $scope.pagination.firstResult + '&max=' + $scope.pagination.maxResults + '&request=' + $scope.requestId + '&status=' + $scope.status;
            $scope.searchLoaded = false;
            $scope.transactionDetails = [];
            $http.get(url)
                .then(function(response) {
                    $scope.transactionDetails = response.data.result;
                    $scope.totalUserTransactions = response.data.count;
                    $scope.searchLoaded = true;
                })
                .catch(function(error) {
                    $scope.searchLoaded = true;
                    console.log('Error while fetching user transactions:', error);
                });
        };

            $scope.filter = function() {
                $scope.getUserTransactions();
            };

            $scope.reset = function() {
                $scope.requestId = '';
                $scope.status = '';
                $scope.pagination.firstResult = 0;
                $scope.pagination.maxResults = $scope.itemsPerPage;
                $scope.getUserTransactions();
            };

            $scope.pageCountChanged = function () {
                  $scope.changePage(1);
            };

        $scope.getUserTransactions();
});


module.controller('UserCredentialsDataModalCtrl', function($scope, credentialData, $modalInstance) {
    $scope.credentialData = credentialData;

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };

    $scope.keys = function(object) {
        return object ? Object.keys(object) : [];
    }
});

module.controller('UserFederationCtrl', function ($scope, $location, $route, realm, serverInfo, Components, Notifications, Dialog) {
    if(realm.mode != "wiam")
    {
        $location.path('/notfound');
        return;
    }
    
    document.querySelector("body").classList.add("scrollbar-none");

    $scope.realm = realm;
    $scope.providers = serverInfo.componentTypes['org.keycloak.storage.UserStorageProvider'];
    $scope.instancesLoaded = false;

    if (!$scope.providers) $scope.providers = [];

    $scope.addProvider = function (provider) {
        $location.url("/create/user-storage/" + realm.realm + "/providers/" + provider.id);
    };

    $scope.getInstanceLink = function (instance) {
        return "/realms/" + realm.realm + "/user-storage/providers/" + instance.providerId + "/" + instance.id;
    };

    $scope.getInstanceName = function (instance) {
        return instance.name;
    };
    $scope.getInstanceProvider = function (instance) {
        return instance.providerId;
    };

    $scope.isProviderEnabled = function (instance) {
        return !instance.config['enabled'] || instance.config['enabled'][0] == 'true';
    };

    $scope.getInstancePriority = function (instance) {
        if (!instance.config['priority']) {
            console.log('getInstancePriority is undefined');
        }
        return instance.config['priority'][0];
    };

    Components.query({
        realm: realm.realm,
        parent: realm.id,
        type: 'org.keycloak.storage.UserStorageProvider'
    }, function (data) {
        $scope.instances = data;
        $scope.instancesLoaded = true;
    });

    $scope.removeInstance = function (instance) {
        Dialog.confirmDelete(instance.name, 'user storage provider', function () {
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

module.controller('GenericUserStorageCtrl', function ($scope, $location, Notifications, $route, Dialog, realm,
    serverInfo, instance, providerId, Components, UserStorageOperations, $translate) {
    console.log('GenericUserStorageCtrl');
    console.log('providerId: ' + providerId);
    $scope.create = !instance.providerId;
    console.log('create: ' + $scope.create);
    var providers = serverInfo.componentTypes['org.keycloak.storage.UserStorageProvider'];
    console.log('providers length ' + providers.length);
    var providerFactory = null;
    for (var i = 0; i < providers.length; i++) {
        var p = providers[i];
        console.log('provider: ' + p.id);
        if (p.id == providerId) {
            $scope.providerFactory = p;
            providerFactory = p;
            break;
        }

    }
    $scope.showSync = false;
    $scope.changed = false;

    console.log("providerFactory: " + providerFactory.id);

    function initUserStorageSettings() {
        if ($scope.create) {
            $scope.changed = true;
            instance.name = providerFactory.id;
            instance.providerId = providerFactory.id;
            instance.providerType = 'org.keycloak.storage.UserStorageProvider';
            instance.parentId = realm.id;
            instance.config = {

            };
            instance.config['priority'] = ["0"];
            instance.config['enabled'] = ["true"];

            $scope.fullSyncEnabled = false;
            $scope.changedSyncEnabled = false;
            if (providerFactory.metadata.synchronizable) {
                instance.config['fullSyncPeriod'] = ['-1'];
                instance.config['changedSyncPeriod'] = ['-1'];

            }
            instance.config['cachePolicy'] = ['DEFAULT'];
            instance.config['evictionDay'] = [''];
            instance.config['evictionHour'] = [''];
            instance.config['evictionMinute'] = [''];
            instance.config['maxLifespan'] = [''];
            if (providerFactory.properties) {

                for (var i = 0; i < providerFactory.properties.length; i++) {
                    var configProperty = providerFactory.properties[i];
                    if (configProperty.defaultValue) {
                        instance.config[configProperty.name] = [configProperty.defaultValue];
                    } else {
                        instance.config[configProperty.name] = [''];
                    }

                }
            }

        } else {
            $scope.changed = false;
            $scope.fullSyncEnabled = (instance.config['fullSyncPeriod'] && instance.config['fullSyncPeriod'][0] > 0);
            $scope.changedSyncEnabled = (instance.config['changedSyncPeriod'] && instance.config['changedSyncPeriod'][0] > 0);
            if (providerFactory.metadata.synchronizable) {
                if (!instance.config['fullSyncPeriod']) {
                    console.log('setting to -1');
                    instance.config['fullSyncPeriod'] = ['-1'];

                }
                if (!instance.config['changedSyncPeriod']) {
                    console.log('setting to -1');
                    instance.config['changedSyncPeriod'] = ['-1'];

                }
            }
            if (!instance.config['enabled']) {
                instance.config['enabled'] = ['true'];
            }
            if (!instance.config['cachePolicy']) {
                instance.config['cachePolicy'] = ['DEFAULT'];

            }
            if (!instance.config['evictionDay']) {
                instance.config['evictionDay'] = [''];

            }
            if (!instance.config['evictionHour']) {
                instance.config['evictionHour'] = [''];

            }
            if (!instance.config['evictionMinute']) {
                instance.config['evictionMinute'] = [''];

            }
            if (!instance.config['maxLifespan']) {
                instance.config['maxLifespan'] = [''];

            }
            if (!instance.config['priority']) {
                instance.config['priority'] = ['0'];
            }

            if (providerFactory.properties) {
                for (var i = 0; i < providerFactory.properties.length; i++) {
                    var configProperty = providerFactory.properties[i];
                    if (!instance.config[configProperty.name]) {
                        instance.config[configProperty.name] = [''];
                    }
                }
            }

        }
        if (providerFactory.metadata.synchronizable) {
            if (instance.config && instance.config['importEnabled']) {
                $scope.showSync = instance.config['importEnabled'][0] == 'true';
            } else {
                $scope.showSync = true;
            }
        }

    }

    initUserStorageSettings();
    $scope.instance = angular.copy(instance);
    $scope.realm = realm;

    $scope.$watch('instance', function () {
        if (!angular.equals($scope.instance, instance)) {
            $scope.changed = true;
        }

    }, true);

    $scope.$watch('fullSyncEnabled', function (newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.config['fullSyncPeriod'][0] = $scope.fullSyncEnabled ? "604800" : "-1";
        $scope.changed = true;
    });

    $scope.$watch('changedSyncEnabled', function (newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.config['changedSyncPeriod'][0] = $scope.changedSyncEnabled ? "86400" : "-1";
        $scope.changed = true;
    });


    $scope.save = function () {
        console.log('save provider');
        $scope.changed = false;
        if ($scope.create) {
            console.log('saving new provider');
            Components.save({ realm: realm.realm }, $scope.instance, function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/user-storage/providers/" + $scope.instance.providerId + "/" + id);
                Notifications.success($translate.instant("user.storage.create.success"));
            });
        } else {
            console.log('update existing provider');
            Components.update({
                realm: realm.realm,
                componentId: instance.id
            },
                $scope.instance, function () {
                    $route.reload();
                    Notifications.success($translate.instant("user.storage.edit.success"));
                });
        }
    };

    $scope.reset = function () {
        //initUserStorageSettings();
        //$scope.instance = angular.copy(instance);
        $route.reload();
    };

    $scope.cancel = function () {
        console.log('cancel');
        if ($scope.create) {
            $location.url("/realms/" + realm.realm + "/user-federation");
        } else {
            $route.reload();
        }
    };

    $scope.triggerFullSync = function () {
        console.log('GenericCtrl: triggerFullSync');
        triggerSync('triggerFullSync');
    };

    $scope.triggerChangedUsersSync = function () {
        console.log('GenericCtrl: triggerChangedUsersSync');
        triggerSync('triggerChangedUsersSync');
    };

    function triggerSync(action) {
        UserStorageOperations.sync.save({ action: action, realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function (syncResult) {
            $route.reload();
            Notifications.success("Sync of users finished successfully. " + syncResult.status);
        }, function () {
            $route.reload();
            Notifications.error("Error during sync of users");
        });
    }
    $scope.removeImportedUsers = function () {
        UserStorageOperations.removeImportedUsers.save({ realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function (syncResult) {
            $route.reload();
            Notifications.success($translate.instant("removed-imported-users"));
        }, function () {
            $route.reload();
            Notifications.error("Error during remove");
        });
    };
    $scope.unlinkUsers = function () {
        Dialog.confirmUnlink('Unlink Users', "Please note that action is permanent and you will not be able to revert it.Once it's unlinked,you have to delete the users manually", function () {
            UserStorageOperations.unlinkUsers.save({ realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function (syncResult) {
                $route.reload();
                Notifications.success($translate.instant("unlinked-users-successfully"));
            }, function () {
                $route.reload();
                Notifications.error("Error during unlink");
            });
        });
    };

});


function removeGroupMember(groups, member) {
    for (var j = 0; j < groups.length; j++) {
        //console.log('checking: ' + groups[j].path);
        if (member.path == groups[j].path) {
            groups.splice(j, 1);
            break;
        }
        if (groups[j].subGroups && groups[j].subGroups.length > 0) {
            //console.log('going into subgroups');
            removeGroupMember(groups[j].subGroups, member);
        }
    }
}

module.controller('KcUserGroupMembershipCtrl', function ($scope, UserGroupMembership, UserGroupMembershipCount, AddGroupsToUser, Notifications, Dialog, $translate, $http,Auth,AccountCreator,InputDialog, Session) {
    $scope.loaderTypeName = 'roleListLoader';
    const realm = $scope.$parent.realm;
    const user = $scope.$parent.user;
    $scope.groupMemberships = [];
    $scope.selectedClient = null;
    $scope.entries = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.searchLoaded = false;
    $scope.query = {
        search: null,
        realm: realm.realm,
        max:  Session.getItem("userGroupsPageFilterCount") || 5,
        first: 0,
        userId: user.id
    }
    $scope.emailEnable = user && user.attributes && user.attributes.hasOwnProperty('qv_is_account_creator') && user.attributes.qv_is_account_creator[0] === "true";

    const queryParams = {
        realm: realm.realm,
        userId: user.id
    };

    function initialQueryUpdate() {
        $scope.searchLoaded = false;
        UserGroupMembership.query($scope.query, function(response) {
            $scope.groupMemberships = response
            oldQueryUpdate();
        });
    }

    $scope.firstPage = function () {
        $scope.pageCount = $scope.query.max;
        $scope.query.first = 0;
        $scope.startCount = 1;
        $scope.QueryUpdate();
    };

    $scope.previousPage = function () {
        $scope.startCount = $scope.startCount - $scope.query.max;
        let pageCount = $scope.pageCount;
        let queryMax = parseInt($scope.query.max);
        if (pageCount === 0) {
            $scope.pageCount = queryMax - 5;
        } else if (pageCount === $scope.entries) {
            let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
            $scope.pageCount = pageCount - backFlow;
        } else {
            $scope.pageCount -= queryMax;
        }

        $scope.query.first -= parseInt($scope.query.max);
        if ($scope.query.first < 0) {
            $scope.query.first = 0;
        }
        $scope.QueryUpdate();
    };

    $scope.selectQuery = function () {
        Session.setItem("userGroupsPageFilterCount", $scope.query.max);
        $scope.query.first = 0;
        $scope.searchLoaded = false;
        initialQueryUpdate()
    };

    $scope.nextPage = function () {
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

    $scope.QueryUpdate = function () {
        $scope.searchLoaded = false;
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        UserGroupMembership.query($scope.query, function (response) {
            $scope.groupMemberships = response
            $scope.searchLoaded = true;
        });
    };

    $scope.isGroupListEmpty = function () {
        return groupMemberships.length == 0;
    };

    function oldQueryUpdate() {
        UserGroupMembershipCount.query(queryParams, function(response) {
            $scope.entries = response.count;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                $scope.pageCount = $scope.entries;
                $scope.startCount = 1;
            } else {
                $scope.pageCount = $scope.query.max;
                $scope.startCount = 1;
            }
            $scope.searchLoaded = true;
        });
    }

    $scope.deleteUserGroup = function(group) {
        Dialog.confirm($translate.instant('delete-group-title'), $translate.instant('are-you-sure-to-remove-group-for-user'), function() {
            $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + $scope.user.id + '/groups/' + group.id).then(function() {
                initialQueryUpdate();
                Notifications.success($translate.instant("user.groups.leave.success"));
            }, function(err){
                Notifications.error($translate.instant(err.description));

            });
        });
    };

    $scope.showChangeAccountCreatorOption = Auth.user.isAccountCreator && Auth.user.userId != user.id  && user.attributes && (!user.attributes.qv_is_account_creator || user.attributes.qv_is_account_creator == 'false')
    $scope.changeAccountCreator = function () {
        AccountCreator.sendOtp({ realm: realm.realm, userId: user.id }, function () {
            InputDialog.changeAccountCreator(realm.realm,user, function () {});
        })
    }

    $scope.addGroupsToUser = () => AddGroupsToUser.open(realm.realm, user.id, 'Check');

     if (!user.profPic) {
        const profPicBase64 = generateAvatar($scope.user.username ? $scope.user.username : $scope.user.email);
        $scope.generatedProfilePic = profPicBase64.split("base64,")[1];
     }

    initialQueryUpdate();
});

module.controller('LDAPUserStorageCtrl', function ($scope, $location, Notifications, $route, Dialog, realm,
    serverInfo, instance, Components, UserStorageOperations, RealmLDAPConnectionTester, ConfirmExitDialog, $translate) {
    if (realm.mode != "wiam") {
        $location.path("/notfound");
        return;
    }
    var providerId = 'ldap';
    $scope.create = !instance.providerId;
    var providers = serverInfo.componentTypes['org.keycloak.storage.UserStorageProvider'];
    var providerFactory = null;
    for (var i = 0; i < providers.length; i++) {
        var p = providers[i];
        if (p.id == providerId) {
            $scope.providerFactory = p;
            providerFactory = p;
            break;
        }

    }

    $scope.provider = instance;
    $scope.showSync = false;

    if (serverInfo.profileInfo.name == 'community') {
        $scope.ldapVendors = [
            { "id": "ad", "name": "Active Directory" },
            { "id": "rhds", "name": "Red Hat Directory Server" },
            { "id": "tivoli", "name": "Tivoli" },
            { "id": "edirectory", "name": "Novell eDirectory" },
            { "id": "other", "name": "Other" }
        ];
    } else {
        $scope.ldapVendors = [
            { "id": "ad", "name": "Active Directory" },
            { "id": "rhds", "name": "Red Hat Directory Server" }
        ];
    }

    $scope.authTypes = [
        { "id": "none", "name": "none" },
        { "id": "simple", "name": "simple" }
    ];

    $scope.searchScopes = [
        { "id": "1", "name": "One Level" },
        { "id": "2", "name": "Subtree" }
    ];

    $scope.useTruststoreOptions = [
        { "id": "always", "name": "Always" },
        { "id": "ldapsOnly", "name": "Only for ldaps" },
        { "id": "never", "name": "Never" }
    ];

    var DEFAULT_BATCH_SIZE = "1000";


    console.log("providerFactory: " + providerFactory.id);

    $scope.changed = false;
    $scope.openRedirect = false;
    $scope.editModeOptions = ['READ_ONLY','WRITABLE','UNSYNCED'];

    function initUserStorageSettings() {
        if ($scope.create) {
            $scope.changed = true;
            instance.name = 'ldap';
            instance.providerId = 'ldap';
            instance.providerType = 'org.keycloak.storage.UserStorageProvider';
            instance.parentId = realm.id;
            instance.config = {

            };
            instance.config['enabled'] = ["true"];
            instance.config['priority'] = ["0"];

            $scope.fullSyncEnabled = false;
            $scope.changedSyncEnabled = false;
            instance.config['fullSyncPeriod'] = ['-1'];
            instance.config['changedSyncPeriod'] = ['-1'];
            instance.config['cachePolicy'] = ['DEFAULT'];
            instance.config['evictionDay'] = [''];
            instance.config['evictionHour'] = [''];
            instance.config['evictionMinute'] = [''];
            instance.config['maxLifespan'] = [''];
            instance.config['batchSizeForSync'] = [DEFAULT_BATCH_SIZE];
            //instance.config['importEnabled'] = ['true'];

            if (providerFactory.properties) {

                for (var i = 0; i < providerFactory.properties.length; i++) {
                    var configProperty = providerFactory.properties[i];
                    if (configProperty.defaultValue) {
                        instance.config[configProperty.name] = [configProperty.defaultValue];
                    } else {
                        instance.config[configProperty.name] = [''];
                    }

                }
            }
            instance.config['editMode'] = [$scope.editModeOptions[0]]
        } else {
            $scope.changed = false;
            $scope.fullSyncEnabled = (instance.config['fullSyncPeriod'] && instance.config['fullSyncPeriod'][0] > 0);
            $scope.changedSyncEnabled = (instance.config['changedSyncPeriod'] && instance.config['changedSyncPeriod'][0] > 0);
            if (!instance.config['fullSyncPeriod']) {
                console.log('setting to -1');
                instance.config['fullSyncPeriod'] = ['-1'];

            }
            if (!instance.config['enabled']) {
                instance.config['enabled'] = ['true'];
            }
            if (!instance.config['changedSyncPeriod']) {
                console.log('setting to -1');
                instance.config['changedSyncPeriod'] = ['-1'];

            }
            if (!instance.config['cachePolicy']) {
                instance.config['cachePolicy'] = ['DEFAULT'];

            }
            if (!instance.config['evictionDay']) {
                instance.config['evictionDay'] = [''];

            }
            if (!instance.config['evictionHour']) {
                instance.config['evictionHour'] = [''];

            }
            if (!instance.config['evictionMinute']) {
                instance.config['evictionMinute'] = [''];

            }
            if (!instance.config['maxLifespan']) {
                instance.config['maxLifespan'] = [''];

            }
            if (!instance.config['priority']) {
                instance.config['priority'] = ['0'];
            }
            if (!instance.config['importEnabled']) {
                instance.config['importEnabled'] = ['true'];
            }

            if (providerFactory.properties) {

                for (var i = 0; i < providerFactory.properties.length; i++) {
                    var configProperty = providerFactory.properties[i];
                    if (!instance.config[configProperty.name]) {
                        if (configProperty.defaultValue) {
                            instance.config[configProperty.name] = [configProperty.defaultValue];
                        } else {
                            instance.config[configProperty.name] = [''];
                        }
                    }

                }
            }

            for (var i = 0; i < $scope.ldapVendors.length; i++) {
                if ($scope.ldapVendors[i].id === instance.config['vendor'][0]) {
                    $scope.vendorName = $scope.ldapVendors[i].name;
                }
            };



        }
        if (instance.config && instance.config['importEnabled']) {
            $scope.showSync = instance.config['importEnabled'][0] == 'true';
        } else {
            $scope.showSync = true;
        }

        $scope.lastVendor = instance.config['vendor'][0];
    }

    initUserStorageSettings();
    $scope.instance = angular.copy(instance);
    $scope.realm = realm;

    $scope.$watch('instance', function () {
        if (!angular.equals($scope.instance, instance)) {
            $scope.changed = true;
        }

        if (!angular.equals($scope.instance.config['vendor'][0], $scope.lastVendor)) {
            console.log("LDAP vendor changed. Previous=" + $scope.lastVendor + " New=" + $scope.instance.config['vendor'][0]);
            $scope.lastVendor = $scope.instance.config['vendor'][0];

            if ($scope.lastVendor === "ad") {
                $scope.instance.config['usernameLDAPAttribute'][0] = "cn";
                $scope.instance.config['userObjectClasses'][0] = "person, organizationalPerson, user";
            } else {
                $scope.instance.config['usernameLDAPAttribute'][0] = "uid";
                $scope.instance.config['userObjectClasses'][0] = "inetOrgPerson, organizationalPerson";
            }

            $scope.instance.config['rdnLDAPAttribute'][0] = $scope.instance.config['usernameLDAPAttribute'][0];

            var vendorToUUID = {
                rhds: "nsuniqueid",
                tivoli: "uniqueidentifier",
                edirectory: "guid",
                ad: "objectGUID",
                other: "entryUUID"
            };
            $scope.instance.config['uuidLDAPAttribute'][0] = vendorToUUID[$scope.lastVendor];
        }


    }, true);

    $scope.$watch('fullSyncEnabled', function (newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.config['fullSyncPeriod'][0] = $scope.fullSyncEnabled ? "604800" : "-1";
        $scope.changed = true;
    });

    $scope.$watch('changedSyncEnabled', function (newVal, oldVal) {
        if (oldVal == newVal) {
            return;
        }

        $scope.instance.config['changedSyncPeriod'][0] = $scope.changedSyncEnabled ? "86400" : "-1";
        $scope.changed = true;
    });


    $scope.save = function () {
        $scope.changed = false;
        $scope.instance.config['enabled'][0] = $scope.instance.config['enabled'][0].toString();
        if (!$scope.instance.config['batchSizeForSync'] || !parseInt($scope.instance.config['batchSizeForSync'][0])) {
            $scope.instance.config['batchSizeForSync'] = [DEFAULT_BATCH_SIZE];
        } else {
            $scope.instance.config['batchSizeForSync'][0] = parseInt($scope.instance.config.batchSizeForSync).toString();
        }

        if ($scope.create) {
            Components.save({ realm: realm.realm }, $scope.instance, function (data, headers) {
                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + "/user-storage/providers/" + $scope.instance.providerId + "/" + id);
                Notifications.success($translate.instant("user.storage.create.success"));
            });
        } else {
            Components.update({
                realm: realm.realm,
                componentId: instance.id
            },
                $scope.instance, function () {
                    $scope.openRedirect = true;
                    $route.reload();
                    Notifications.success($translate.instant("user.storage.edit.success"));
                });
        }
    };

    $scope.reset = function () {
        $scope.openRedirect = true;
        $route.reload();
    };

    $scope.cancel = function () {
        if ($scope.create) {
            $location.url("/realms/" + realm.realm + "/user-federation");
        } else {
            $scope.openRedirect = true;
            $route.reload();
        }
    };

    $scope.removeImportedUsers = function () {
        UserStorageOperations.removeImportedUsers.save({ realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function (syncResult) {
            $scope.openRedirect = true;
            $route.reload();
            Notifications.success($translate.instant("removed-imported-users"));
        }, function () {
            $scope.openRedirect = true;
            $route.reload();
            Notifications.error("Error during remove");
        });
    };
    $scope.unlinkUsers = function () {
        Dialog.confirmUnlink('Unlink Users', "Please note that action is permanent and you will not be able to revert it.Once it's unlinked,you have to delete the users manually", function () {
            UserStorageOperations.unlinkUsers.save({ realm: $scope.realm.realm, componentId: $scope.instance.id }, {}, function (syncResult) {
                $scope.openRedirect = true;
                $route.reload();
                Notifications.success($translate.instant("unlinked-users-successfully"));
            }, function () {
                $scope.openRedirect = true;
                $route.reload();
                Notifications.error("Error during unlink");
            });
        });

    };
    var initConnectionTest = function (testAction, ldapConfig) {
        return {
            action: testAction,
            realm: $scope.realm.realm,
            connectionUrl: ldapConfig.connectionUrl,
            bindDn: ldapConfig.bindDn,
            bindCredential: ldapConfig.bindCredential,
            useTruststoreSpi: ldapConfig.useTruststoreSpi,
            connectionTimeout: ldapConfig.connectionTimeout,
            startTls: ldapConfig.startTls,
            componentId: instance.id
        };
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

    $scope.testConnection = function () {
        console.log('LDAPCtrl: testConnection');
        RealmLDAPConnectionTester.save(initConnectionTest("testConnection", $scope.instance.config), function () {
            Notifications.success($translate.instant("ldap-connection-successful"));
        }, function () {
            Notifications.error("Error when trying to connect to LDAP. See server.log for details.");
        });
    };

    $scope.testAuthentication = function () {
        console.log('LDAPCtrl: testAuthentication');
        RealmLDAPConnectionTester.save(initConnectionTest("testAuthentication", $scope.instance.config), function () {
            Notifications.success($translate.instant("ldap-authentication-successful"));
        }, function () {
            Notifications.error("LDAP authentication failed. See server.log for details");
        });
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });

});

module.controller('LDAPTabCtrl', function (Dialog, $scope, Current, Notifications, $location, UserStorageOperations, $route, $translate) {
    $scope.removeUserFederation = function () {
        Dialog.confirmDelete($scope.instance.name, 'ldap provider', function () {
            $scope.instance.$remove({
                realm: Current.realm.realm,
                componentId: $scope.instance.id
            }, function () {
                $location.url("/realms/" + Current.realm.realm + "/connections/enterprise");
                Notifications.success($translate.instant("user.storage.remove.success"));
            });
        });
    };
    $scope.triggerChangedUsersSync = function () {
        triggerSync('triggerChangedUsersSync');
    };

    $scope.triggerFullSync = function () {
        triggerSync('triggerFullSync');
    };

    function triggerSync(action) {
        UserStorageOperations.sync.save({ action: action, realm: Current.realm.realm, componentId: $scope.instance.id }, {}, function (syncResult) {
            $route.reload();
            Notifications.success("Sync of users finished successfully. " + syncResult.status);
        }, function () {
            $route.reload();
            Notifications.error("Error during sync of users");
        });
    }
});


module.controller('LDAPMapperListCtrl', function ($scope, $location, Notifications, $route, Dialog, realm, provider, mappers) {

    $scope.realm = realm;
    $scope.provider = provider;
    $scope.instance = provider;

    $scope.mappers = mappers;

    $scope.changeSearch = function () {
        $scope.search = $scope.searchValue;
    };
    $scope.clearSearch = function () {
        $scope.search = "";
        $scope.searchValue = "";
    };

});

module.controller('LDAPMapperCtrl', function ($scope, $route, realm, provider, mapperTypes, mapper, clients, Components, LDAPMapperSync, Notifications, Dialog, $location, ConfirmExitDialog, $translate) {
    $scope.realm = realm;
    $scope.provider = provider;
    $scope.clients = clients;
    $scope.create = false;
    $scope.changed = false;
    $scope.openRedirect = false;

    for (var i = 0; i < mapperTypes.length; i++) {
        console.log('mapper.providerId: ' + mapper.providerId);
        console.log('mapperTypes[i].id ' + mapperTypes[i].id);
        if (mapperTypes[i].id == mapper.providerId) {
            $scope.mapperType = mapperTypes[i];
            break;
        }
    }

    if ($scope.mapperType.properties) {

        for (var i = 0; i < $scope.mapperType.properties.length; i++) {
            var configProperty = $scope.mapperType.properties[i];
            if (!mapper.config[configProperty.name]) {
                if (configProperty.defaultValue) {
                    mapper.config[configProperty.name] = [configProperty.defaultValue];
                } else {
                    mapper.config[configProperty.name] = [''];
                }
            }

        }
    }
    $scope.mapper = angular.copy(mapper);


    $scope.$watch('mapper', function () {
        if (!angular.equals($scope.mapper, mapper)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function () {
        Components.update({
            realm: realm.realm,
            componentId: mapper.id
        },
            $scope.mapper, function () {
                $scope.openRedirect = true;
                $route.reload();
                Notifications.success($translate.instant("mapper-updated"));
            });
    };

    $scope.reset = function () {
        $scope.mapper = angular.copy(mapper);
        $scope.changed = false;
    };

    $scope.remove = function () {
        Dialog.confirmDelete($scope.mapper.name, 'ldap mapper', function () {
            Components.remove({
                realm: realm.realm,
                componentId: mapper.id
            }, function () {
                $scope.openRedirect = true;
                $location.url("/realms/" + realm.realm + '/ldap-mappers/' + provider.id);
                Notifications.success($translate.instant("user.storage.remove.success"));
            });
        });
    };

    $scope.triggerFedToKeycloakSync = function () {
        triggerMapperSync("fedToKeycloak");
    };

    $scope.triggerKeycloakToFedSync = function () {
        triggerMapperSync("keycloakToFed");
    };

    function triggerMapperSync(direction) {
        LDAPMapperSync.save({ direction: direction, realm: realm.realm, parentId: provider.id, mapperId: $scope.mapper.id }, {}, function (syncResult) {
            Notifications.success("Data synced successfully. " + syncResult.status);
        }, function (error) {
            Notifications.error(error.data.errorMessage);
        });
    }

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});

module.controller('LDAPMapperCreateCtrl', function ($scope, realm, provider, mapperTypes, clients, Components, Notifications, Dialog, $location, ConfirmExitDialog, $translate) {
    $scope.realm = realm;
    $scope.provider = provider;
    $scope.clients = clients;
    $scope.create = true;
    $scope.mapper = { config: {} };
    $scope.mapperTypes = mapperTypes;
    $scope.mapperType = null;
    $scope.changed = true;
    $scope.openRedirect = false;

    $scope.$watch('mapperType', function () {
        if ($scope.mapperType != null) {
            $scope.mapper.config = {};
            if ($scope.mapperType.properties) {

                for (var i = 0; i < $scope.mapperType.properties.length; i++) {
                    var configProperty = $scope.mapperType.properties[i];
                    if (!$scope.mapper.config[configProperty.name]) {
                        if (configProperty.defaultValue) {
                            $scope.mapper.config[configProperty.name] = [configProperty.defaultValue];
                        } else {
                            $scope.mapper.config[configProperty.name] = [''];
                        }
                    }

                }
            }
        }
    }, true);

    $scope.save = function () {
        if ($scope.mapperType == null) {
            Notifications.error("You need to select mapper type!");
            return;
        }

        $scope.mapper.providerId = $scope.mapperType.id;
        $scope.mapper.providerType = 'org.keycloak.storage.ldap.mappers.LDAPStorageMapper';
        $scope.mapper.parentId = provider.id;

        if ($scope.mapper.config && $scope.mapper.config["role"] && !Array.isArray($scope.mapper.config["role"])) {
            $scope.mapper.config["role"] = [$scope.mapper.config["role"]];
        }

        Components.save({ realm: realm.realm }, $scope.mapper, function (data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $scope.openRedirect = true;
            $location.url("/realms/" + realm.realm + "/ldap-mappers/" + $scope.mapper.parentId + "/mappers/" + id);
            Notifications.success($translate.instant("mapper-created"));
        });
    };

    $scope.reset = function () {
        $location.url("/realms/" + realm.realm + '/ldap-mappers/' + provider.id);
    };

    $scope.$on('$routeChangeStart', function (event, newUrl) {
        if (!$scope.openRedirect && $scope.changed) {
            showConfirmExitDialog(event, ConfirmExitDialog, $scope, $location, newUrl);
        }
    });
});
