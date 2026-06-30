<#macro component>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <#assign transitionWithOutScaling="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform' x-transition:enter-end='opacity-100 transform' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform' x-transition:leave-end='opacity-0 transform'">
    <template x-if="currentTab==='groups' && !isEmpty()">
        <div class="groups-holder">
            <div class="your-groups">
                <h3 class="mb-4 text-lg text-center">${msg('your-groups')}</h3>
                <div class="sidebar-btn-wrapper"  @click="showCreateGroup()">
                        <div class="sidebar-btn !gap-1 justify-start">
                            <img class="w-3 h-3" src="${url.resourcesPath}/img/add-small-icon.svg"> 
                            <span>
                                ${msg('add-new-group')}
                            </span>
                        </div>
                </div>
                <div class="overflow-scroll h-[30rem] max-h-[35rem] px-4 pb-4">
                    <template x-for="group in (search !== '' && searchFilter === 'groups' ? groupSearchResults : groups) ">
                        <div class="sidebar-btn-wrapper">
                            <div class="sidebar-btn"
                                :class="{'sidebar-btn-active' : group.groupName === currentGroup.groupName}"
                                @click.debounce="updateCurrentGroup(group)">
                                <span x-text="group.groupName">
                                </span>
                                <button @click="showDeleteDialog=true;dialog=true"
                                    x-show="group.groupName === currentGroup.groupName"
                                    class="bg-white p-1 rounded-md">
                                    <img class="w-4 h-4" src="${url.resourcesPath}/img/delete-btn.svg">
                                </button>
                            </div>
                        </div>
                    </template>
                </div>
            </div>
            <div class="flex flex-1 app-holder p-3">
                <div x-show="currentGroup?.groupName" class="apps-wrapper box-border bg-primary-100 items-center !justify-center !gap-1 border-2 border-dashed border-primary-950 scale-up"  @click="unCheckAll();currentTab='addNewApp';selectAll=false">
                   <img class="w-5 h-5" src="${url.resourcesPath}/img/add-small-icon-green.svg"> <span class="text-primary-950 text-base">${msg('add-new-app')}</span>
                </div>
                <template x-for="app in (search !== '' && searchFilter === 'apps' ? searchResults : selectedGroup)">
                    <div class="apps-wrapper" :class="{'iws' : app.appType === 'iws' && !isPluginInstalled(), 'scale-up' : showMenu !== app.id}"  x-show="app.displayName !== 'profile' && app.displayName !== 'dashboard' && app.alias !== 'winlogon-app'">
                        <div class="app-wrapper-link" @click="handleSSOClick(app.id)">
                            <div class="w-14 h-14 flex items-center justify-center border rounded-xl">
                                <img class="w-10 h-10" :src="app.displayName === 'dashboard' ? '${url.resourcesPath}/img/high_secure.svg'  : app.logoUrl" />
                            </div>
                            <div>
                                <h4 class="app-name" x-text="app.displayName"></h4>
                                <span class="sub-app"x-show="app.appType && app.appType !== 'iws' && app.appType !== 'high_secure'" x-text="app.appType"></span>
                            </div>
                        </div>
                        <div class="flex gap-2 items-center relative ml-auto pr-4">
                            <button @click="deleteAppId = app.id;showAppDeleteDialog = true; dialog=true">
                                <img alt="delete-btn" src="${url.resourcesPath}/img/delete-btn.svg" />
                            </button>
                            <button class="p-3" @click="setShowMenu(app.id, true)">
                                <img alt="three-dots-btn" src="${url.resourcesPath}/img/three-dots-btn.svg" />
                            </button>
                            <template x-if="showMenu === app.id">
                                <div class="app-menu right-1 top-12">
                                    <button class="menu-link"
                                        @click="showCreateGroup()">
                                        <img src="${url.resourcesPath}/img/add-folder.svg" />
                                        ${msg('create-new-group')}
                                    </button>
                                </div>
                            </template>
                        </div>
                    </div>
                </template>
                <div class="group-empty-text" x-show="search !== '' && searchFilter === 'groups' && (groupSearchResults.length === 0 && selectedGroup.length === 0)">
                    <p class="gray-text">${msg('no-groups-found')}</p>
                </div>
            </div>
        </div>
    </template>
</#macro>