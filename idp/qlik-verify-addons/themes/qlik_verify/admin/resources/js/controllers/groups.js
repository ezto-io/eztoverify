module.controller('GroupListCtrl', function ($scope, $route, $translate, realm, Groups, Group, GroupChildren, Notifications, AddUsersToGroup, $location, Dialog, Session) {

    $scope.realm = realm;
    $scope.groupList = [];
    $scope.loaderTypeName = 'roleListLoader';
    $scope.query = {
        realm: realm.realm,
        search: null,
        max: 5,
        first: 0
    };

    $scope.openUserInGroups = () => {
        Session.setItem("rolesUsersSubTab", 'users')
    }

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

        Group.query($scope.lengthQuery, function (res) {
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
        $scope.query.search = ""
        $scope.searchQuery();
    };

    $scope.QueryUpdate = function () {
        $scope.searchLoaded = false;
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        $scope.groupList = Groups.query($scope.query, function () {
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
            $scope.groupList = Groups.query($scope.query, function () {
                $scope.searchLoaded = true;
                $scope.lastSearch = $scope.query.search;
            });
        }
    };
    function initialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        searchQueryWithLoaded();
        $scope.groupList = Groups.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    $scope.selectQuery = function () {
        Session.setItem("pageFilterCount", $scope.query.max);
        $scope.searchLoaded = false;
        searchQueryWithLoaded();
        oldQueryUpdate();
        $scope.groupList = Groups.query($scope.query, function () {
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

    $scope.edit = function (selected) {
        if (selected.id === 'realm') return;
        $location.url("/realms/" + realm.realm + "/groups/" + selected.id);
    };

    $scope.cut = function (selected) {
        $scope.cutNode = selected;
    };

    $scope.isDisabled = function () {
        if (!$scope.tree.currentNode) return true;
        return $scope.tree.currentNode.id === 'realm';
    };

    $scope.paste = function (selected) {
        if (selected === null) return;
        if ($scope.cutNode === null) return;
        if (selected.id === $scope.cutNode.id) return;
        if (selected.id === 'realm') {
            Groups.save({ realm: realm.realm }, { id: $scope.cutNode.id }, function () {
                $route.reload();
                Notifications.success($translate.instant("group-moved"));

            });

        } else {
            GroupChildren.save({ realm: realm.realm, groupId: selected.id }, { id: $scope.cutNode.id }, function () {
                $route.reload();
                Notifications.success($translate.instant("group-moved"));

            });

        }

    };

    $scope.remove = function (selected) {
        if (selected === null) return;
        Dialog.confirmDelete(selected.name, 'group', function () {
            Group.remove({ realm: realm.realm, groupId: selected.id }, function () {
                $route.reload();
                Notifications.success($translate.instant("group-deleted"));
            });
        });

    };

    $scope.createGroup = function (selected) {
        var parent = 'realm';
        if (selected) {
            parent = selected.id;
        }
        $location.url("/create/group/" + realm.realm + '/parent/' + parent);

    };
    $scope.createNewGroup = function () {

    };
    var isLeaf = function (node) {
        return node.id !== "realm" && (!node.subGroups || node.subGroups.length === 0);
    };

    $scope.getGroupClass = function (node) {
        if (node.id === "realm") {
            return 'pficon pficon-users';
        }
        if (isLeaf(node)) {
            return 'normal';
        }
        if (node.subGroups.length && node.collapsed) return 'collapsed';
        if (node.subGroups.length && !node.collapsed) return 'expanded';
        return 'collapsed';

    };

    $scope.getSelectedClass = function (node) {
        if (node.selected) {
            return 'selected';
        } else if ($scope.cutNode && $scope.cutNode.id === node.id) {
            return 'cut';
        }
        return undefined;
    };

    $scope.addUsersToGroup = function (node) {
        console.log('Add Group called');
        AddUsersToGroup.open('testdata', 'data', 'data', 'data');
    };

});

module.controller('GroupCreateCtrl', function ($scope, $route, realm, parentId, Groups, Group, GroupChildren, Notifications, $location, $translate) {
    $scope.realm = realm;
    $scope.group = {};
    $scope.save = function () {
        console.log('save!!!');
        if (parentId === 'realm') {
            console.log('realm');
            Groups.save({ realm: realm.realm }, $scope.group, function (data, headers) {
                var l = headers().location;


                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/groups/" + id);
                Notifications.success($translate.instant("group.create.success"));
            });

        } else {
            GroupChildren.save({ realm: realm.realm, groupId: parentId }, $scope.group, function (data, headers) {
                var l = headers().location;


                var id = l.substring(l.lastIndexOf("/") + 1);

                $location.url("/realms/" + realm.realm + "/groups/" + id);
                Notifications.success($translate.instant("group.create.success"));
            });

        }

    };
    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/groups");
    };
});

module.controller('GroupTabCtrl', function (Dialog, $scope, Current, Group, GroupInvite, Notifications, $location, $route, $translate) {
    $scope.inviteStatus = {
        enable: true
    };
    $scope.groupInviteStatus = {
        invited: false
    };
    if ($scope.group?.attributes?.hidden_ga_grp_invite_started) {
        $scope.groupInviteStatus.invited = true;
    }
    $scope.removeGroup = function () {
        Dialog.confirmDelete($scope.group.name, 'group', function () {
            Group.remove({
                realm: Current.realm.realm,
                groupId: $scope.group.id
            }, function () {
                $location.url("/realms/" + Current.realm.realm + "/groups");
                Notifications.success($translate.instant("group-deleted"));
            });
        });
    };

    $scope.inviteGroup = function () {
        Dialog.confirmInvite($scope.group.name, $translate.instant('group'), function () {
            GroupInvite.send({
                realm: Current.realm.realm,
                groupId: $scope.group.id
            }, function (data) {
                $scope.inviteStatus.enable = false;
                if (data.status === 400) {
                    Notifications.error($translate.instant(data.body.message));
                } else {
                    Notifications.success($translate.instant(data.body.message));
                    $route.reload();
                }
                $scope.inviteStatus.enable = true;
            }, function (err) {
                Notifications.error(err.message ? err.message : '');
                $scope.inviteStatus.enable = true;
            });
        });
    };

});


module.controller('GroupDetailCtrl', function (Dialog, $scope, $route, realm, group, Group, Notifications, AddUsersToGroup, $location, $translate) {
    $scope.realm = realm;

    if (!group.attributes) {
        group.attributes = {};
    }

    $scope.newAttribute = {
        key : '',
        value : ''
    }
    convertAttributeValuesToString(group);


    $scope.group = angular.copy(group);
    $scope.groupOld = angular.copy(group);
    $scope.groupAttributes = angular.copy(group.attributes)
    $scope.noWorkflowMetaData = 0;

    if ($location.url().endsWith('attributes')) {
        Object.keys($scope.groupAttributes).map(key =>{
            if (key.endsWith("_organizations") || key.endsWith("_projects")) {
                delete $scope.groupAttributes[key];
                $scope.noWorkflowMetaData++;
            }
        })
    }

    $scope.changed = false; // $scope.create;
    $scope.$watch('group', function () {
        if (!angular.equals($scope.group, group)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    $scope.$watch('newAttribute.key', function () {
        if ($scope.newAttribute.key in $scope.group.attributes ) {
            $scope.isKeyExist=true;
         }else{
            $scope.isKeyExist=false;
         }
    }, true);


    var groupCopy=angular.copy($scope.group);

    $scope.save = function () {
        convertAttributeValuesToLists();
        var requestPayload = $scope.isGroupAttributesLoaded ? $scope.group : groupCopy ;

        var requestPayload = $scope.isGroupAttributesLoaded ? $scope.group : groupCopy ;

        var requestPayload = $scope.isGroupAttributesLoaded ? $scope.group : groupCopy ;

        Group.update({
            realm: realm.realm,
            groupId: $scope.group.id
        }, requestPayload, function () {
            $scope.changed = false;
            convertAttributeValuesToString(requestPayload);
            group = angular.copy(requestPayload);
            $scope.isAddGroupAttributeLoading = false;
            $scope.isGroupAttributesLoaded=true;
            Notifications.success($translate.instant("group.edit.success"));
            $route.reload();
        });
    };

    function convertAttributeValuesToLists() {
        var requestPayload = $scope.isGroupAttributesLoaded ? $scope.group : groupCopy ;
        var attrs = requestPayload.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "string") {
                attrs[attribute] = attrs[attribute].split("##");
            }
        }
    }

    function convertAttributeValuesToString(group) {
        var attrs = group.attributes;
        for (var attribute in attrs) {
            if (typeof attrs[attribute] === "object") {
                attrs[attribute] = attrs[attribute].join("##");
            }
        }
    }

    $scope.allowAddAttribute = false;

    $scope.addAttributeButton = function () {
        $scope.allowAddAttribute = true;
    };

    $scope.attributeLength = function () {
        return Object.keys($scope.groupAttributes).length;
    };

    $scope.noAttributesSaved = function () {
        $scope.tempGroup = angular.copy(group);
        return Object.keys($scope.tempGroup.attributes).length - $scope.noWorkflowMetaData;
    };

    $scope.reset = function () {
        $scope.group = angular.copy(group);
        $scope.changed = false;
    };

    $scope.cancel = function () {
        $location.url("/realms/" + realm.realm + "/groups");
    };

    $scope.newAttribute = {
        key: "",
        value: ""
    };

    $scope.isGroupAttributesLoaded=true;

    $scope.addAttribute = function () {
        $scope.isGroupAttributesLoaded=false;
        $scope.isAddGroupAttributeLoading=true;
        groupCopy.attributes[$scope.newAttribute.key] = $scope.newAttribute.value;
        delete $scope.newAttribute;
        $scope.save();
    };

    $scope.removeAttribute = function (key) {
        $scope.selectedGroupAttribute=key;
        $scope.isGroupAttributesLoaded=false;
        delete groupCopy.attributes[key];
        $scope.changed = true;
        $scope.save();
    };
});


module.controller('KcRolesUsersTree', function ($scope, $http, $route, Notifications, GroupRealmRoleMapping, GroupMembership, AddUsersToGroup,  AddRolesToGroup, Dialog, $translate, AssignUsersToGroup, UserGroupMapping) {
    const realm = $scope.$parent.realm;
    const user = $scope.$parent.user;
    const client = $scope.$parent.client;
    const group = $scope.$parent.group;
    const root = document.querySelector(':root');
    $scope.group = group;
    $scope.realm = realm;
    $scope.user = user;
    $scope.client = client;
    $scope.groupMemberships = [];
    $scope.groupRoles = [];
    $scope.roles = [];
    $scope.users = [];
    $scope.selectedClient = null;
    $scope.isRoleLoaded = false;
    $scope.isUserLoaded = false;
    $scope.currentGroup = {};
    $scope.query = {
        search: null,
        realm: realm.realm,
        groupId: group.id
    }

    function initialQueryUpdate() {
            getRoles();
            getUsers();
    }

    const getRoles = () => {
        GroupRealmRoleMapping.query($scope.query, function(response) {
            $scope.groupRoles = response
            $scope.roles = $scope.groupRoles.slice(0, 5) 
            $scope.isRoleLoaded = true;
            setCssCountValue("roles")
        });
    }

    $scope.showMoreRoles = () => {
        $scope.roles =  $scope.groupRoles.slice(0, $scope.roles.length + 5) 
        setCssCountValue("roles")
    }

    const setCssCountValue = (type) => {
        switch(type) {
            case "groups":
                const groupsLen = $scope.users.length;
                const groupMappingLen = $scope.groupMemberships.length
                root.style.setProperty('--groups-count', `${((groupsLen +  (groupsLen === groupMappingLen ? 1 : 2))  * 49.5) - 51.2}px`);
                break;
            case "roles":
                const rolesLen = $scope.roles.length;
                const roleMappingLen = $scope.groupRoles.length
                root.style.setProperty('--roles-count',  `${((rolesLen +  (rolesLen === roleMappingLen ? 1 : 2) )  * 49.5) - 48.16}px`);
                break;
        }
    }

    const getUsers = () => {
        GroupMembership.query($scope.query, function(response) {
            $scope.groupMemberships = response;
            $scope.groupMemberships = $scope.groupMemberships.map(group => {
                if (group.rolesCount > 0) {
                    getGroupRoles(group.id)
                }
                return group
            })
            $scope.users = $scope.groupMemberships.slice(0, 5);
            $scope.isGroupLoaded = true;
            setCssCountValue("groups")
        });
    }

    $scope.showMoreUsers = () => {
        $scope.users = $scope.groupMemberships.slice(0, $scope.users.length + 5) 
        setCssCountValue("groups")
    }


    $scope.scrollToView = () => {
        const groupRolesElement = document.getElementById("holder");
        groupRolesElement.scrollLeft = 500;
    }


    $scope.addRolesToGroup = function () {
        AddRolesToGroup.open(realm.realm, group.id, group, realm);
    };

    $scope.deleteRealmRole = function (role) {
        Dialog.confirm($translate.instant('delete-role-title'), $translate.instant('sure-to-delete-role-from-group'), function () {
            $scope.selectedRealmMappingsToRemove = [{ ...role }];
            $http.delete(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/realm', { data: $scope.selectedRealmMappingsToRemove, headers: { "content-type": "application/json" } })
            .then(function () {
                Notifications.success($translate.instant("group.roles.remove.success"));
                $route.reload();
            });
    });
    }

    function assignUsersToGroup(selectedUserIds) {
        AssignUsersToGroup.save({
            realm: realm.realm,
            groupId: $route.current.params.group
        }, selectedUserIds, function () {
            $route.reload();
            Notifications.success($translate.instant('selected-users-added-to-group'));
        }, function (err) {
            Notifications.error(err.data.error);
        });
    }

    $scope.addUser = function () {
        GroupMembership.query({
            realm: realm.realm,
            groupId: $route.current.params.group
        }, function (data) {
            AddUsersToGroup.open('', '', '', assignUsersToGroup, data);
        });
    };

    $scope.leaveUserFromGroup = function (userId) {
        Dialog.confirm($translate.instant('remove-user-text'), $translate.instant('sure-to-remove-user-from-group'), function () {
            UserGroupMapping.remove({ realm: $scope.query.realm, userId: userId, groupId: $scope.query.groupId }, function () {
                Notifications.success($translate.instant('user-successfully-removed-from-group'));
                $route.reload();
            }, function (err) {
                Notifications.error(err.data.error);
            });
        });
    };

    initialQueryUpdate();
});

module.controller('KcGroupRoleMappingsCtrl', function ($scope, $http, $route, Dialog, Client, Notifications, AddRolesToGroup, GroupRealmRoleMapping, GroupRealmRoleMappingCount, $translate, Session) {
    $scope.loaderTypeName = 'roleListLoader';
    const realm = $scope.$parent.realm;
    const client = $scope.$parent.client;
    const group = $scope.$parent.group;
    $scope.subTab = $scope.$parent.subTab;
    $scope.view = $scope.$parent.view;
    $scope.changeSubTab = $scope.$parent.changeSubTab;
    $scope.changeView = $scope.$parent.changeView;
    
    $scope.realm = realm;
    $scope.group = group;
    $scope.client = client;
    $scope.groupOld = angular.copy(group);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.selectedRealmMappingsToRemove = [];
    $scope.clientMappings = [];
    $scope.dummymodel = [];
    $scope.entries = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.searchLoaded = false;
    $scope.query = {
        search: null,
        realm: realm.realm,
        max: Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5,
        first: 0,
        groupId: group.id
    }
    initialQueryUpdate();

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
        let fullLengthOfPages = parseInt($scope.query.max) + $scope.pageCount;
        $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
        $scope.pageCount = ($scope.entries < fullLengthOfPages) ? $scope.entries : fullLengthOfPages;
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
        $scope.realmMappings = GroupRealmRoleMapping.query($scope.query, function () {
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

    $scope.searchQuery = function () {
        Session.setItem("pageFilterCount", $scope.query.max);
        $scope.searchLoaded = false;
        oldQueryUpdate();
        $scope.query.first = 0;
        $scope.realmMappings = GroupRealmRoleMapping.query($scope.query, function () {
            $scope.searchLoaded = true;
        });
    };

    function oldQueryUpdate() {
        GroupRealmRoleMappingCount.query({realm: realm.realm, groupId: group.id, search: $scope.query.search}, function(response) {
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

    function initialQueryUpdate() {
        $scope.realmMappings = GroupRealmRoleMapping.query($scope.query, function() {
            oldQueryUpdate();
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    }

    $scope.addRealmRole = function () {
        $scope.selectedRealmRolesToAdd = JSON.parse('[' + $scope.selectedRealmRoles + ']');
        $scope.selectedRealmRoles = [];
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/realm',
            $scope.selectedRealmRolesToAdd).then(function () {
                $scope.realmMappings = GroupRealmRoleMapping.query({ realm: realm.realm, groupId: group.id });
                $scope.selectedRealmMappings = [];
                $scope.selectRealmRoles = [];
                if ($scope.selectedClient) {
                    $scope.selectedClientRoles = [];
                    $scope.selectedClientMappings = [];
                }
                $scope.selectedRealmRolesToAdd = [];
                Notifications.success($translate.instant("group.roles.add.success"));

            });
    };

    $scope.deleteRealmRole = function (role) {
        Dialog.confirm($translate.instant('delete-role-title'), $translate.instant('sure-to-delete-role-from-group'), function () {
            $scope.selectedRealmMappingsToRemove = [{ ...role }];
            $http.delete(authUrl + '/admin/realms/' + realm.realm + '/groups/' + group.id + '/role-mappings/realm', { data: $scope.selectedRealmMappingsToRemove, headers: { "content-type": "application/json" } })
            .then(function () {
                Notifications.success($translate.instant("group.roles.remove.success"));
                $route.reload();
            });
        });
    };
    $scope.addRolesToGroup = function () {
        AddRolesToGroup.open(realm.realm, group.id, group, realm);
    };
    clientSelectControl($scope, $route.current.params.realm, Client);
});

module.service("AddRolesToGroup", function ($modal) {
    var dialog = {};
    dialog.open = function (realmName, groupId, group, realm) {
        var controller = function ($scope, $http, $q, Group, $modalInstance, $route, Notifications, $translate, GroupAvailableRealmRoleMapping, GroupAvailableRealmRoleMappingCount) {
            $scope.isRTL = () => locale == 'ar';
            $scope.checkedRoles = {};
            $scope.realmName = realmName;
            $scope.resourceUrl = resourceUrl;
            $scope.loaderTypeName = 'roleListLoader';
            $scope.groupId = groupId;
            $scope.query = {
                search: null,
                realm: realmName,
                max: 5,
                first: 0,
                groupId: $scope.groupId
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
            
                group.attributes = {...group.attributes, ...metaData}
                Group.update({
                    realm: realm.realm,
                    groupId: group.id
                }, group, function (data, headers) {
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
                if ($scope.currentRole.projects.length > 0 || $scope.currentRole.organizations.length) {
                    $scope.enableDone = true;
                } else {
                    $scope.enableDone = false;
                }
            },true)

            initialQueryUpdate();
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
                $http.post(authUrl + '/admin/realms/' + $scope.realmName + '/groups/' + $scope.groupId + '/role-mappings/realm', selectedRoles)
                .then(function () {
                    $modalInstance.dismiss('cancel');
                    $scope.checkedRoles = {};
                    $route.reload();
                    Notifications.success($translate.instant("group.roles.add.success"));
                }, function (err) {
                    Notifications.error(err.data.error);
                });
            };

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };

            function oldQueryUpdate() {
                GroupAvailableRealmRoleMappingCount.query({realm: realmName, groupId: groupId, search: $scope.query.search}, function(response) {
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
                let fullLengthOfPages = parseInt($scope.query.max) + $scope.pageCount;
                $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
                $scope.pageCount = ($scope.entries < fullLengthOfPages) ? $scope.entries : fullLengthOfPages;
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
                GroupAvailableRealmRoleMapping.query($scope.query, function (data) {
                    updateCheckedRoles(data)
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
                    GroupAvailableRealmRoleMapping.query($scope.query, function (data) {
                        updateCheckedRoles(data)
                        $scope.searchLoaded = true;
                        $scope.lastSearch = $scope.query.search;
                    });
                }
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

            function initialQueryUpdate() {
                $scope.searchLoaded = false;
                oldQueryUpdate();
                searchQueryWithLoaded();
                $scope.realmRoles = GroupAvailableRealmRoleMapping.query($scope.query, function () {
                    $scope.searchLoaded = true;
                    $scope.lastSearch = $scope.query.search;
                });
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
        };

        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/add-roles-modal.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg'
        });
    };
    return dialog;
});

module.controller('KcGroupMembersCtrl', function ($scope, $route, GroupMembership, AddUsersToGroup, AssignUsersToGroup, UserGroupMapping, Notifications, Dialog, Session, $translate) {

    const realm = $scope.$parent.realm;
    const group = $scope.$parent.group;
    $scope.realm = realm;
    $scope.loaderTypeName = 'tableListLoader';
    $scope.page = 0;
    $scope.group = group;
    $scope.groupOld = angular.copy(group);
    $scope.editTxt = "Edit";

    $scope.query = {
        realm: realm.realm,
        groupId: group.id,
        max: Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5,
        first: 0
    };
    $scope.entries = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.searchQueryLoaded = false;
    $scope.navigationLoaded = false;
    if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
        $scope.query.search = "";
    }
    InitialQueryUpdate();
    $scope.copyElement = angular.copy($scope.query.max);
    $scope.$watch('query.max', function () {
        $scope.query.first = 0;
    }, true);

    function oldQueryUpdate() {
        $scope.lengthQuery = {};
        if ($scope.copyElement && ($scope.query.max !== $scope.copyElement)) {
            $scope.query.first = 0;
            $scope.navigationLoaded = true;
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

        GroupMembership.query($scope.lengthQuery, function (res) {
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
    $scope.firstPage = function () {
        $scope.searchLoaded = false;
        if ($scope.entries !== 0) {
            $scope.navigationLoaded = true;
        }
        $scope.query.search = "";
        $scope.pageCount = parseInt($scope.query.max);
        $scope.query.first = 0;
        $scope.startCount = 1;
        $scope.QueryUpdate();
    };

    $scope.previousPage = function () {
        $scope.query.search = "";
        $scope.searchLoaded = false;
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
        $scope.QueryUpdate();
    };

    $scope.nextPage = function () {
        $scope.query.search = "";
        $scope.searchLoaded = false;
        $scope.navigationLoaded = true;
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
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        if ($scope.query.search) {
            $scope.searchQueryLoaded = true;
            $scope.searchLoaded = false;
            $scope.navigationLoaded = true;
        }
        else {
            $scope.searchQueryLoaded = false;
            $scope.searchLoaded = false;
        }
        $scope.users = GroupMembership.query($scope.query, function () {
            $scope.searchLoaded = true;
        });
    };
    function InitialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        $scope.users = GroupMembership.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };

    $scope.searchQuery = function () {
        Session.setItem("pageFilterCount", $scope.query.max);
        $scope.searchLoaded = false;
        oldQueryUpdate();
        $scope.users = GroupMembership.query($scope.query, function () {
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

    function InitialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        $scope.users = GroupMembership.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };


    function assignUsersToGroup(selectedUserIds) {
        AssignUsersToGroup.save({
            realm: realm.realm,
            groupId: $route.current.params.group
        }, selectedUserIds, function () {
            $route.reload();
            Notifications.success($translate.instant('selected-users-added-to-group'));
        }, function (err) {
            Notifications.error(err.data.error);
        });
    }

    $scope.addUser = function () {
        GroupMembership.query({
            realm: realm.realm,
            groupId: $route.current.params.group
        }, function (data) {
            AddUsersToGroup.open('', '', '', assignUsersToGroup, data);
        });
    };

    $scope.leaveUserFromGroup = function (userId) {
        Dialog.confirm($translate.instant('remove-user-text'), $translate.instant('sure-to-remove-user-from-group'), function () {
            UserGroupMapping.remove({ realm: $scope.query.realm, userId: userId, groupId: $scope.query.groupId }, function () {
                Notifications.success($translate.instant('user-successfully-removed-from-group'));
                $route.reload();
            }, function (err) {
                Notifications.error(err.data.error);
            });
        });
    };
});

module.controller('DefaultGroupsCtrl', function ($scope, $q, realm, Groups, GroupsCount, DefaultGroups, Notifications, $translate) {
    $scope.realm = realm;
    $scope.groupList = [];
    $scope.selectedGroup = null;
    $scope.tree = [];
    $scope.defaultTree = [];

    $scope.searchCriteria = '';
    $scope.currentPage = 1;
    $scope.currentPageInput = $scope.currentPage;
    $scope.pageSize = 20;
    $scope.numberOfPages = 1;

    var refreshDefaultGroups = function () {
        DefaultGroups.query({ realm: realm.realm }, function (data) {
            if ($scope.tree?.currentNode?.selected) {
                $scope.tree.currentNode.selected = null;
            } if ($scope.defaultTree?.currentNode?.selected) {
                $scope.defaultTree.currentNode.selected = null;
            }
            $scope.tree.currentNode = '';
            $scope.defaultTree.currentNode = '';
            $scope.defaultGroups = data;
        });
    };

    var refreshAvailableGroups = function (search) {
        var first = ($scope.currentPage * $scope.pageSize) - $scope.pageSize;
        $scope.currentPageInput = $scope.currentPage;
        var queryParams = {
            realm: realm.realm,
            first: first
        };
        var countParams = {
            realm: realm.realm,
            top: 'true'
        };

        if (angular.isDefined(search) && search !== '') {
            queryParams.search = search;
            countParams.search = search;
        }

        var promiseGetGroups = $q.defer();
        Groups.query(queryParams, function (entry) {
            promiseGetGroups.resolve(entry);
        }, function () {
            promiseGetGroups.reject('Unable to fetch ' + queryParams);
        });
        promiseGetGroups.promise.then(function (groups) {
            $scope.groupList = groups;
        }, function (failed) {
            Notifications.success(failed);
        });

        var promiseCount = $q.defer();
        GroupsCount.query(countParams, function (entry) {
            promiseCount.resolve(entry);
        }, function () {
            promiseCount.reject('Unable to fetch ' + countParams);
        });
        promiseCount.promise.then(function (entry) {
            if (angular.isDefined(entry.count) && entry.count > $scope.pageSize) {
                $scope.numberOfPages = Math.ceil(entry.count / $scope.pageSize);
            }
        }, function (failed) {
            Notifications.success(failed);
        });
    };

    refreshAvailableGroups();

    $scope.$watch('currentPage', function (newValue, oldValue) {
        if (parseInt(newValue, 10) !== parseInt(oldValue, 10)) {
            refreshAvailableGroups($scope.searchCriteria);
        }
    });

    $scope.clearSearch = function () {
        $scope.searchCriteria = '';
        if (parseInt($scope.currentPage, 10) === 1) {
            refreshAvailableGroups();
        } else {
            $scope.currentPage = 1;
        }
    };

    $scope.searchGroup = function () {
        if (parseInt($scope.currentPage, 10) === 1) {
            refreshAvailableGroups($scope.searchCriteria);
        } else {
            $scope.currentPage = 1;
        }
    };

    refreshDefaultGroups();

    $scope.addDefaultGroup = function () {
        if (!$scope.tree?.currentNode) {
            Notifications.error('Please select a group to add');
            return;
        }
        let existingNode;
        if ($scope.defaultGroups && $scope.tree?.currentNode?.id) {
            existingNode = $scope.defaultGroups.find(data => data.id === $scope.tree.currentNode.id);
        }
        if (existingNode) {
            Notifications.error("Group already exists");
        } else {
            DefaultGroups.update({ realm: realm.realm, groupId: $scope.tree.currentNode.id }, function () {
                refreshDefaultGroups();
                Notifications.success($translate.instant('group.default.add.success'));
            });
        }
    };

    $scope.removeDefaultGroup = function () {
        let existingNode;
        if ($scope.defaultGroups && $scope.defaultTree?.currentNode?.id) {
            existingNode = $scope.defaultGroups.find(data => data.id === $scope.defaultTree?.currentNode?.id);
        }
        if (existingNode) {
            DefaultGroups.remove({ realm: realm.realm, groupId: $scope.defaultTree.currentNode.id }, function () {
                refreshDefaultGroups();
                Notifications.success($translate.instant('group.default.remove.success'));
            });
        } else {
            Notifications.error('Please select a group to remove');
        }
    };

    var isLeaf = function (node) {
        return node.id !== "realm" && (!node.subGroups || node.subGroups.length === 0);
    };

    $scope.getGroupClass = function (node) {
        if (node.id === "realm") {
            return 'pficon pficon-users';
        }
        if (isLeaf(node)) {
            return 'normal';
        }
        if (node.subGroups.length && node.collapsed) return 'collapsed';
        if (node.subGroups.length && !node.collapsed) return 'expanded';
        return 'collapsed';

    };

    $scope.getSelectedClass = function (node) {
        if (node.selected) {
            return 'selected';
        } else if ($scope.cutNode && $scope.cutNode.id === node.id) {
            return 'cut';
        }
        return undefined;
    };

});
