<#import "./components/all.ftl" as all>
<#import "./components/mostRecent.ftl" as mostRecent>
<#import "./components/favorites.ftl" as favorites>
<#import "./components/createGroup.ftl" as createGroup>
<#import "./components/groups.ftl" as groups>
<#import "./components/empty.ftl" as empty>
<#import "./components/header.ftl" as header>
<#import "./components/dialog.ftl" as dialog>
<#import "./components/popup.ftl" as popup>
<#import "./components/addNewApp.ftl" as addNewApp>
<#import "./components/sectionNav.ftl" as sectionNav>
<!DOCTYPE html>
<html lang="${user_preferred_lang}" <#if user_preferred_lang == 'ar'> dir="rtl" </#if>  >
<head>
    <title>${msg('ssoManagementTitle',realm.getDisplayName()?cap_first!)}</title>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <#if realm.getMode()?? && (realm.getMode() == 'wiam' || realm.getMode == 'ciam')>
    <link rel="icon" href="${url.resourcesPath}/img/eztoauth-favicon.ico">
    <#elseif realm.getMode()?? && (realm.getMode() == 'bo')>
    <link rel="icon" href="${url.resourcesPath}/img/eztovrfy-favicon.ico">
    </#if>
    <#if user_preferred_lang == 'ar'> 
        <link href="${url.resourcesPath}/css/dist-rtl.css" rel="stylesheet" />
    <#else>
        <link href="${url.resourcesPath}/css/dist.css" rel="stylesheet" />
    </#if>
    <script src="https://cdn.jsdelivr.net/npm/iso-639-1@3.1.2/build/index.min.js"></script>
</head>

<#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
<#assign transitionWithOutScaling="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform' x-transition:enter-end='opacity-100 transform' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform' x-transition:leave-end='opacity-0 transform'">
<body class="bg-background flex flex-col items-center justify-center w-full" x-data="sso"> 
    <input type="hidden" id="isExtensionInstalled" value="false" />
    <@header.component url=url realm=realm/>
    <main class="px-10 max-w-[1440px] w-full pt-20" @click="showProfile=false">
        <div class="py-6 text-[#666666] text-2xl border-b border-gray-200 w-fit pr-10">
            <span>${msg('welcome')}</span>
            <span class="text-black font-semi-bold">${ssouser.displayName!''}</span>
        </div>
        <div id="browser-url" class="bg-yellow-100 hidden py-3 text-sm my-4 border-l-8 rounded px-3 border-amber-400 text-yellow-900">
            <p class="flex items-center gap-1">
                <img class="w-4 h-4 mr-1" src="${url.resourcesPath}/img/warning.svg" />
                ${msg("browser-extension-not-installed-iws")}
                <a id="extensionUrl" class="underline" href="">${msg('click-here')}</a>
            </p>
        </div>
        <@dialog.component />
        <@sectionNav.component url=url/>
        <section class="mt-4">
            <@all.component />
            <@createGroup.component/>
            <@addNewApp.component/>
            <@mostRecent.component/>
            <@favorites.component/>
            <@groups.component/>
            <@empty.component/>
        </section>
         <@popup.component url=url/>
    </main>
    <div x-cloak class="feedback-aligner fadeInclass" x-show="notification.show" ${transitionWithOutScaling}>
        <div class="alert alert-dismissable pr-4 alert-success">
            <img class="w-6 h-6" src="${url.resourcesPath}/img/success.svg" alt="close-icon"/>
            <strong>${msg('success')}!</strong>
            <span x-text="notification.message"></span>
            <button class="alert-close" id="notification-close" @click="closeNotify()">
                <img class="w-4 h-4" src="${url.resourcesPath}/img/close.svg" alt="close-icon"/>
            </button>
        </div>
    </div>
</body>
<script>

    const url = "${baseURI}realms/${realm.getName()}/sso_dashboard/${realm.getName()}/user/${ssouser.getUsername()}";
    const CreateAppUrl = "${baseURI}admin/${realm.getName()}/console/#/realms/${realm.getName()}/marketplace/templates/sso-integrations";
    let selectedApp = "";
    var groupsData = {groups:[],favorites: []};
    var oldCopy = {};

    document.addEventListener("DOMContentLoaded", function () {
        const loaderDiv = document.getElementById("loader-div");
        Array(19).fill(undefined).map(()=>{
            loaderDiv.appendChild(document.querySelector(".loader-containter").cloneNode(true));
        });
        async function getApps() {
            await fetch(
                url
            ).then(response => response.json())
	        .then(data => {
                if (data.status == 401) {
                    location.reload();
                }
                if (data.groups) { 
                    groupsData.groups = data.groups;
                }
                if(data.favorites) {
                    groupsData.favorites = data.favorites;
                }
                oldCopy = JSON.parse(JSON.stringify(groupsData));
                loadAlpine();
            }).catch(error => {});
        }
        getApps();
        createLocaleSelect();
    });

    function loadAlpine() {
        var script = document.createElement('script');
        script.defer = true;
        script.src = '${url.resourcesPath}/js/alpine.min.js';
        document.head.appendChild(script);
    }

    document.addEventListener("alpine:init", () => {
        <#noautoesc>
            let ssoApps =  JSON.parse(`${ssoApps}`);
        </#noautoesc>
        ssoApps = ssoApps.map(app=>{
            if(!app.alias && app.appType !== 'iws') {
                return {...app, id: app.appType + app.displayName}
            }
            return {...app, id: app.alias}
        })
        Alpine.data("sso", () => ({
            currentTab: "all",
            popup: false,
            dialog: false,
            createError: false,
            showOptions: false,
            apps: ssoApps,
            showProfile: false,
            search: "",
            showDeleteDialog: false,
            showAppDeleteDialog: false,
            favDot: false,
            selectAll:false,
            unSelectAll:false,
            deleteAppId:'',
            searchFilter:"apps",
            profileUrl: '',
            searchResults: [],
            groups: groupsData?.groups,
            groupSearchResults:[],
            currentGroup: groupsData?.groups[0],
            favorites: groupsData?.favorites,
            filteredApps: ssoApps.filter(app => app.displayName !== 'profile' && app.displayName !== 'dashboard' && app.alias !== 'winlogon-app'),
            mostRecent: [],
            mostRecentApps: [],
            selectedGroup: [],
            showMenu: "",
            groupName: "",
            createGroupName: "",
            showPopup: {
                show: false,
                subTab: "",
            },
            notification:{
                show: false,
                message: ""
            },
            addToGroup: "",
            init() {
                this.updateFavorites();
                this.refreshMostRecent();
                if (this.groups.length !== 0) {
                    this.setSelectedGroup();
                    this.updateCurrentGroup(this.currentGroup);
                }
                document.addEventListener("visibilitychange", (event) => {
                    if (document.visibilityState !== "visible") {
                        this.save();
                    }
                });
                document.getElementById("loader-div").style.display = "none";
            },
            getSSOApps() {
                return this.apps;
            },
            addTo() {
                let isAlreadyAdded = false;
                this.groups = this.groups.map((group) => {
                    if (group.groupName === this.addToGroup) {
                        if (group.apps.includes(selectedApp)) {
                            isAlreadyAdded = true;
                        }
                        return {
                            ...group,
                            apps: [...new Set([...group.apps, selectedApp])],
                        };
                    } else {
                        return group;
                    }
                });
                this.setShowPopup(false);
                this.refreshGroup();
                if (isAlreadyAdded) {
                    this.showNotify("${msg('app-exist')}");
                    return;
                }
                this.showNotify("${msg('app-added-success')}");
            },
            setMostRecent() {
                if (this.mostRecent)
                    localStorage.setItem(
                        "most-recent",
                        JSON.stringify(this.mostRecent)
                    );
            },
            refreshMostRecent() {
                const item = localStorage.getItem("most-recent");
                if (item) {
                    this.mostRecent = JSON.parse(item);
                }
            },
            updateMostRecent(id) {
                this.mostRecent = this.mostRecent?.filter((m) => m !== id);
                this.mostRecent?.unshift(id);
                this.setMostRecent();
            },
            sortMostRecent() {
                this.mostRecentApps = this.apps.map((app) => ({
                    ...app,
                    order: this.mostRecent.indexOf(app.id),
                })).sort((a, b) => a.order - b.order)
                    .filter((app) => app.order !== -1 && app.order < 12);
            },
            removeRecent(id) {
                this.mostRecent = this.mostRecent.filter(recent => recent !== id)
                this.setMostRecent();
                this.sortMostRecent()
                this.showPopup.show = false;
            },
            handleDislike(id) {
                this.favorites.push(id);
                this.updateFavorites();
                this.showFavDot();
            },
            handleLike(id) {
                this.favorites = this.favorites?.filter((fav) => fav !== id);
                this.updateFavorites();
            },
            handleFav(app) {
                if (app.liked) {
                    this.handleLike(app.id);
                    return;
                }
                 this.handleDislike(app.id);
            },
            changeTab(tab) {
                this.search = "";
                this.currentTab = tab;
                this.showOptions = false;
                this.showPopup = { show: false, subTab: "" };
                this.save();
                this.selectAll = false;
                this.unCheckAll();
            },
            handleSearch() {
                if (this.search !== "") {
                    if (this.currentTab === "groups" && this.searchFilter === 'apps') {
                        this.searchResults = this.selectedGroup.filter((app) =>
                            app.displayName.toLowerCase().includes(this.search.toLowerCase())
                        );
                    } else if (this.currentTab === "groups" && this.searchFilter === 'groups') {
                        this.groupSearchResults = this.groups.filter((group) =>
                            group.groupName.toLowerCase().includes(this.search.toLowerCase())
                        );
                        if (this.groupSearchResults.length > 0) {
                            this.currentGroup = this.groupSearchResults[0];
                            this.setSelectedGroup();
                            return;
                        } 
                        this.currentGroup = {};
                        this.setSelectedGroup();
                    } else if (this.currentTab === "most_recent") {
                        this.searchResults = this.mostRecentApps.filter((app) =>
                            app.displayName.toLowerCase().includes(this.search.toLowerCase())
                        );
                    } else if (this.currentTab === "addNewApp" || this.currentTab === "createGroup") {
                        this.searchResults = this.filteredApps.filter((app) =>
                            app.displayName.toLowerCase().includes(this.search.toLowerCase())
                        );
                    } else {
                        this.searchResults = this.apps.filter((app) =>
                            app.displayName.toLowerCase().includes(this.search.toLowerCase())
                        );
                    }
                } else if (this.search == "" && this.currentTab === "groups" && this.searchFilter === 'groups') {
                    if (this.groups.length > 0) {
                        this.updateCurrentGroup(this.groups[0])
                    }
                }
            },
            updateCurrentGroup(group) {
                this.currentGroup = group;
                this.setSelectedGroup();
            },
            setSelectedGroup() {
                if (this.currentGroup?.groupName) {
                    this.selectedGroup = this.apps.filter((app) =>
                        this.currentGroup.apps.includes(app.id));
                        return;
                }
                this.selectedGroup = [];
            },
            refreshGroup() {
                this.groups.map((group) => {
                    if (this.currentGroup.groupName === group.groupName)
                        this.currentGroup = group;
                });
                this.setSelectedGroup();
            },
            removeApp() {
                this.groups = this.groups.map((group) => {
                    return {
                        groupName: group.groupName,
                        apps:
                            group.groupName === this.currentGroup.groupName
                                ? group.apps.filter((id) => this.deleteAppId !== id)
                                : group.apps,
                    };
                });
                this.deleteAppId = "";
                this.refreshGroup();
            },
            removeAll() {
                switch (this.currentTab) {
                    case "most_recent":
                        this.mostRecent = [];
                        this.setMostRecent();
                        break;
                    case "groups":
                        if (this.showDeleteDialog) {
                            this.groups = this.groups.filter(group => group.groupName !== this.currentGroup.groupName);
                            this.currentGroup = this.groups[0];
                        } else if (this.showAppDeleteDialog) {
                            this.removeApp();
                        } else {
                            this.groups = [];
                        }
                        this.save();
                        break;
                    case "favorites":
                        this.favorites = [];
                        this.save();
                        break;
                }
                this.disableDialog()
                this.showNotify("${msg('removed-success')}")
            },
            setShowMenu(id, state) {
                this.unCheckAll();
                const app = this.apps.filter((app)=>app.id === id)
                if (id && app[0]?.appType === 'iws' && !isPluginInstalled()) return;
                this.showMenu = id;
                this.showPopup = {};
                this.showPopup.show = state;
                this.showPopup.subTab = id
                if (id) selectedApp = id;
            },
            unCheckAll() {
                this.apps = this.apps.map((app) => ({ ...app, checked: false }));
                this.filteredApps = this.filteredApps.map((app) => ({ ...app, checked: false }));
            },
            checkAll() {
                this.filteredApps = this.filteredApps.map((app) => ({ ...app, checked: true }));
            },
            createGroup() {
                if (this.createGroupName.length > 15) {
                    this.createError = "${msg('group-name-chars')}";
                    return;
                }
                if ((this.groups.map(group=>group.groupName).includes(this.createGroupName))) {
                    this.createError = "${msg('group-name-already-exist')}";
                    return;
                }
                const ids = this.filteredApps
                    .filter((app) => app.checked)
                    .map((app) => app.id);
                const newGroup = { groupName: this.createGroupName, apps: ids };
                this.groups = [...this.groups, newGroup];
                this.setShowPopup(false);
                this.updateCurrentGroup(newGroup);
                this.unCheckAll();
                this.changeTab("groups");
                this.save();
                this.createGroupName = ""
                this.showNotify("${msg('group-created-sucess')}")
            },
            addApp() {
                const ids = this.filteredApps
                    .filter((app) => app.checked)
                    .map((app) => app.id);
                this.groups = this.groups.map(group => {
                    if (group.groupName === this.currentGroup.groupName) {
                        group.apps = [...group.apps, ...ids];
                        return group;
                    }
                    return group
                })
                this.updateCurrentGroup(this.currentGroup);
                this.unCheckAll();
                this.changeTab("groups");
                this.save();
                this.showNotify("${msg('app-added-success')}");
            },
            setShowPopup(subTab) {
                this.showPopup = { subTab, show: !subTab ? false : true };
            },
            showCreateGroup() {
                if (this.filteredApps.length === 0 ) {
                    this.createError = "${msg('no-avail-app')}";
                    this.setShowPopup('createGroup')
                } else {
                    this.currentTab = 'createGroup';
                    this.setShowPopup()
                }
            },
            updateFavorites() {
                this.apps = this.apps.map((app) => {
                    if (app.displayName === 'profile'){
                        this.profileUrl = app.redirectUrl;
                    }
                    return {
                        ...app,
                        liked: this.favorites.includes(app.id)
                    }
                });
            },
            disablePopup() {
                if (this.showMenu) {
                    this.setShowMenu('', false);
                }
                if (this.showOptions)  {
                    this.showOptions = false; 
                    this.showPopup.show = false; 
                    this.showPopup.subTab = ''
                }
            },
            disableDialog() {
                this.showPopup.show = false; 
                this.showPopup.subTab = ''
                this.showAppDeleteDialog = false;
                this.showDeleteDialog = false;
                this.dialog = false;
            },
            handleSSOClick(id) {
                const app = this.apps.filter((app)=>app.id === id)
                if (app[0].appType === 'iws' && !isPluginInstalled()) return;
                if (app[0].id !== 'dashboard') this.updateMostRecent(id);
                window.open(app[0].redirectUrl, "_blank");

            },
            handleSelect() {
                if (this.selectAll) {
                    this.checkAll()
                } else {
                    this.unCheckAll()
                }
            },
            isEmpty() {
                let isEmpty = false;
                switch (this.currentTab) {
                    case "all":
                        isEmpty = ssoApps.filter(app => app.displayName !== 'profile' && app.alias !== 'winlogon-app').length === 0;
                        break;
                    case "most_recent":
                        this.sortMostRecent()
                        isEmpty = this.mostRecentApps.length === 0;
                        break;
                    case "groups":
                        isEmpty = this.groups.length === 0;
                        break;
                    case "favorites":
                        isEmpty = this.favorites.length === 0;
                        break;
                }
                return isEmpty;
            },
            showNotify(message) {
                this.notification = { show: true, message };
                setTimeout(()=>{
                    this.closeNotify();
                }, 5000);
            },
            showFavDot() {
                this.favDot = true;
                setTimeout(()=>{
                    this.favDot = false;
                }, 3000);
            },
            closeNotify() {
                this.notification.show = false;
            },
            createNewApp() {
                window.open(CreateAppUrl,'_blank');
            },
            async save() {
                const payload = JSON.stringify({favorites:this.favorites,groups:this.groups})
                if (JSON.stringify(oldCopy) !== payload) {
                    await fetch( url, {
                        method: 'POST',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json'
                        },
                        body: payload
                        }).then((data) => {
                            if (data.status == 401) {
                                location.reload();
                            }
                            oldCopy = JSON.parse(payload);
                    }).catch(error => {
                        location.reload();
                    });
                }
            },
            async logout() {
                await this.save()
                 <#noautoesc>
                    location.replace("${url.logoutUrl}");
                </#noautoesc>
            }
        }));
    });


    let extensionInstalled = false;
    const params = new URLSearchParams(window.location.search);
    params.get('fromPluginUninstall') && delete_cookie('qv_extension_downloaded');

    async function delete_cookie(name) {
        document.cookie = 'qv_extension_downloaded=false';
    }

    function fnBrowserDetect() {
        let userAgent = navigator.userAgent;
        const chromeUrl = "${chromePluginURL!''}";
        const firefoxUrl = "${firefoxPluginURL!''}";
        const appleUrl = "${applePluginURL!''}";
        let extensionUrl;


        if (userAgent.match(/chrome|chromium|crios/i)) {
            extensionUrl = chromeUrl;
        } else if (userAgent.match(/firefox|fxios/i)) {
            extensionUrl = firefoxUrl;
        } else if (userAgent.match(/safari/i)) {
            extensionUrl = appleUrl;
        } else {
            extensionUrl = chromeUrl;
        }
        if(extensionUrl && !isPluginInstalled()) {
            document.getElementById('extensionUrl').setAttribute('href', extensionUrl);
            document.getElementById('browser-url').classList.remove('hidden');
        }
    }

    async function enableIwsWarning(){
        fnBrowserDetect()
        if (isPluginInstalled()) {
            document.querySelectorAll(".iws").forEach( element => {
                element.classList.remove('iws')
            })
        }
    }

    function isPluginInstalled() {
        return document.getElementById('isExtensionInstalled').value === 'true';
    }
    setTimeout(enableIwsWarning, 800);

    const preventSpecialChars = (event) => {
        if (!(/^[a-zA-Z0-9 ]+$/.test(String.fromCharCode(event.which)))) {
            event.preventDefault();
        }
    }

    const selectElement = document.getElementById("locale-select");
    const createLocaleSelect = () => {
        let option;
        const preferedLan = '${user_preferred_lang}' ? '${user_preferred_lang}' : 'en'
        <#list locales as locale>
            option = document.createElement('option');
            option.value = '${locale}';
            if (preferedLan === '${locale}')
                option.selected = true;
            option.innerText = ISO6391.getNativeName('${locale}');
            selectElement.appendChild(option);
        </#list> 
    }

    const handleLangChange = () => {
        updateLangCookie(selectElement.value);
        window.location.reload();
    }

    const updateLangCookie = (lang) => {
        document.cookie = "KEYCLOAK_LOCALE=" + lang + "; path=/";
    }
</script>
</html>