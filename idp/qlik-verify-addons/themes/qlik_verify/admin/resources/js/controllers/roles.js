module.controller('RoleMembersCtrl', function ($scope, realm, role, Session) {
    $scope.variant = variant;
    $scope.realm = realm;
    $scope.role = role;
    $scope.view = Session.getItem("roleUsersView") || 'listView';
    $scope.isWorkflow = role.attributes['connectedApp'] ? true : false;
    

    $scope.changeView = (view) => {
        $scope.view = view
        Session.setItem("roleUsersView", view)
    }
});

module.controller('KcRoleMembersCtrl', function ($scope, $http, $translate, $route, RoleMembership, Dialog, Notifications, $location, RealmRoleRemover, Session, AddUsersToGroup, AssignUsersToRole) {
    const realm = $scope.$parent.realm;
    const role = $scope.$parent.role;
    $scope.view =  $scope.$parent.view;
    $scope.changeView =  $scope.$parent.changeView;
    $scope.realm = realm;
    $scope.role = role;
    $scope.loaderTypeName = 'tableListLoader';
    $scope.page = 0;
    $scope.editTxt = "Edit";

    $scope.query = {
        realm: realm.realm,
        role: role.name,
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
    $scope.remove = function () {
        RealmRoleRemover.remove($scope.role, realm, Dialog, $location, Notifications);
    };
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

        RoleMembership.query($scope.lengthQuery, function (res) {
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
        $scope.users = RoleMembership.query($scope.query, function () {
            $scope.searchLoaded = true;
        });
    };
    function InitialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        $scope.users = RoleMembership.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
    $scope.searchQuery = function () {
        Session.setItem("pageFilterCount", $scope.query.max);
        $scope.searchLoaded = false;
        oldQueryUpdate();
        $scope.users = RoleMembership.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
    function InitialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        $scope.users = RoleMembership.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
    $scope.deleteRealmRole = function (user) {
        Dialog.confirm(
            $translate.instant('user.roles.remove.title'),
            $translate.instant('user.roles.remove.confirm', { roleName: role.name, user: user.username }),
            function () {
                $scope.rolesToRemove = JSON.parse('[' + new Array(JSON.stringify(role)) + ']');
                $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/realm',
                    { data: $scope.rolesToRemove, headers: { "content-type": "application/json" } }).then(function () {
                        $scope.rolesToRemove = [];
                    });
                Notifications.success($translate.instant('user.roles.remove.success'));
                $route.reload();
            },
            function () { }
        );
    };
    $scope.searchQuery();

    function assignUsersToRole(selectedUserIds) {
        AssignUsersToRole.save({
            realm: realm.realm,
            roleId: $route.current.params.role
        }, selectedUserIds, function () {
            $route.reload();
            Notifications.success($translate.instant('selected-users-added-to-group'));
        }, function (err) {
            Notifications.error(err.data.error);
        });
    }

    $scope.addMembers = function () {
        AddUsersToGroup.open('', '', '', assignUsersToRole, $scope.users, role);
    };
});

module.controller('KcRolesTreeCtrl', function ($scope, $http, $route, Notifications, RoleMembership, AddUsersToGroup, Dialog, $translate, AddUsersToGroup, AssignUsersToRole) {
    const realm = $scope.$parent.realm;
    const role = $scope.$parent.role;
    const root = document.querySelector(':root');
    $scope.realm = realm;
    $scope.totalUsers = [];
    $scope.users = [];
    $scope.isUserLoaded = false;
    $scope.query = {
        search: null,
        realm: realm.realm,
        role: role.name,
    }

    function initialQueryUpdate() {
            getUsers();
    }

    const setCssCountValue = () => {
        const rolesLen = $scope.users.length;
        const roleMappingLen = $scope.totalUsers.length
        root.style.setProperty('--groups-count',  `${((rolesLen +  (rolesLen === roleMappingLen ? 1 : 2) )  * 49.5) - 48.16}px`);
    }

    const getUsers = () => {
        RoleMembership.query($scope.query, function(response) {
            $scope.totalUsers = response;
            $scope.users = $scope.totalUsers.slice(0, 5);
            $scope.isUserLoaded = true;
            setCssCountValue()
        });
    }

    $scope.showMoreUsers = () => {
        $scope.users = $scope.totalUsers.slice(0, $scope.users.length + 5) 
        setCssCountValue()
    }

    function assignUsersToRole(selectedUserIds) {
        AssignUsersToRole.save({
            realm: realm.realm,
            roleId: $route.current.params.role
        }, selectedUserIds, function () {
            $route.reload();
            Notifications.success($translate.instant('selected-users-added-to-group'));
        }, function (err) {
            Notifications.error(err.data.error);
        });
    }

    $scope.addUsers = function () {
        AddUsersToGroup.open('', '', '', assignUsersToRole, $scope.users, role);
    };

    $scope.deleteRealmRole = function (user) {
        Dialog.confirm(
            $translate.instant('user.roles.remove.title'),
            $translate.instant('user.roles.remove.confirm', { roleName: role.name, user: user.username }),
            function () {
                $scope.rolesToRemove = JSON.parse('[' + new Array(JSON.stringify(role)) + ']');
                $http.delete(authUrl + '/admin/realms/' + realm.realm + '/users/' + user.id + '/role-mappings/realm',
                    { data: $scope.rolesToRemove, headers: { "content-type": "application/json" } }).then(function () {
                        $scope.rolesToRemove = [];
                    });
                Notifications.success($translate.instant('user.roles.remove.success'));
                $route.reload();
            },
            function () { }
        );
    };

    initialQueryUpdate();
});

module.controller('AdminRoleMembersCtrl', function ($scope, realm, $route, RoleMembership, RoleMembershipSearch, Dialog, Notifications, $location, RealmRoleRemover, Session) {
    $scope.realm = realm;
    $scope.page = 0;
    $scope.role = { "name": "admin" };

    $scope.query = {
        realm: realm.realm,
        role: "admin",
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
    $scope.editTxt = "Edit";
    $scope.impersonateTxt = "Impersonate";
    $scope.removeTxt = "Remove";
    $scope.remove = function () {
        RealmRoleRemover.remove($scope.role, realm, Dialog, $location, Notifications);
    };
    $scope.$watch('query.max', function () {
        $scope.query.first = 0;
    }, true);

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
        if ($scope.query.search) {
            RoleMembershipSearch.query($scope.lengthQuery, function (res) {
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
        else {
            RoleMembership.query($scope.lengthQuery, function (res) {
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
    $scope.QueryUpdate = function () {
        for (var i in $scope.query) {
            if ($scope.query[i] === "") {
                delete $scope.query[i];
            }
        }
        if ($scope.query.search) {
            $scope.users = RoleMembershipSearch.query($scope.query, function () {
                $scope.searchLoaded = true;
            });
        }
        else {
            $scope.users = RoleMembership.query($scope.query, function () {
                $scope.searchLoaded = true;
            });
        }
    };
    function InitialQueryUpdate() {
        $scope.searchLoaded = false;
        oldQueryUpdate();
        $scope.users = RoleMembership.query($scope.query, function () {
            $scope.searchLoaded = true;
            $scope.lastSearch = $scope.query.search;
        });
    };
    $scope.searchQuery = function () {
        if ($scope.lastSearch !== $scope.query.search) {
            $scope.searchLoaded = false;
            oldQueryUpdate();
            if ($scope.query.search) {
                $scope.searchQueryLoaded = true;
                searchQueryWithLoaded();
                $scope.query.first = 0;
                $scope.startCount = 1;
                $scope.userSearchquery = {
                    realm: realm.realm,
                    max: Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5,
                    first: 0,
                    search: $scope.query.search
                };
                $scope.users = RoleMembershipSearch.query($scope.userSearchquery, function () {
                    $scope.searchLoaded = true;
                    $scope.lastSearch = $scope.query.search;
                });
            }
            else {
                $scope.searchQueryLoaded = false;
                searchQueryWithLoaded();
                $scope.query.first = 0;
                $scope.startCount = 1;
                $scope.users = RoleMembership.query($scope.query, function () {
                    $scope.searchLoaded = true;
                    $scope.lastSearch = $scope.query.search;
                });
            }
        }
    };

    $scope.selectQuery = function () {
        Session.setItem("pageFilterCount", $scope.query.max);
        $scope.searchLoaded = false;
        oldQueryUpdate();
        if ($scope.query.search) {
            $scope.searchQueryLoaded = true;
            searchQueryWithLoaded();
            $scope.userSearchquery = {
                realm: realm.realm,
                max: 5,
                first: 0,
                search: $scope.query.search
            };
            $scope.users = RoleMembershipSearch.query($scope.userSearchquery, function () {
                $scope.searchLoaded = true;
            });
        }
        else {
            $scope.searchQueryLoaded = false;
            searchQueryWithLoaded();
            $scope.users = RoleMembership.query($scope.query, function () {
                $scope.searchLoaded = true;
                $scope.lastSearch = $scope.query.search;
            });
        }
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
    $scope.reset = function () {
        $scope.query.search = "";
        $route.reload();
    };

    $scope.isAdminsEmpty = function () {
        return $scope.users.length == 0;
    };

    $scope.searchQuery();

});
