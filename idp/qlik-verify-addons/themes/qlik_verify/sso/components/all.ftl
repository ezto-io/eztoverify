<#macro component>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <div class="app-holder" id="loader-div">
        <div class="apps-wrapper scale-up loader-containter">
            <div class="app-wrapper-link">
                <div class="w-14 h-14 flex items-center justify-center border rounded-xl overflow-hidden">
                    <div class="loader"></div>
                </div>
                <div class="flex flex-col">
                    <div class="w-[160px] h-5 rounded overflow-hidden">
                    <div class="loader"></div>
                    </div>
                    <div class="w-[100px] h-5 rounded overflow-hidden mt-1">
                        <div class="loader"></div>
                    </div>
                </div>
            </div> 
        </div>
    </div>
    <template x-if="currentTab==='all'">
        <div class="app-holder" x-show="!isEmpty()">
            <div x-show="apps[0].displayName === 'dashboard'" class="apps-wrapper box-border bg-primary-100 items-center !justify-center !gap-1 border-2 border-dashed border-primary-950 scale-up"  @click="createNewApp()">
                <img class="w-5 h-5" src="${url.resourcesPath}/img/add-small-icon-green.svg"> <span class="text-primary-950 text-base">${msg('install-new-app')}</span>
            </div>
            <div class="apps-wrapper scale-up" x-show="apps[0].displayName === 'dashboard'">
                <div class="app-wrapper-link" @click="handleSSOClick(apps[0].id)">
                    <div class="w-14 h-14 flex items-center justify-center border rounded-xl">
                        <img class="w-10 h-10" src="${url.resourcesPath}/img/high_secure.svg" />
                    </div>
                    <div>
                        <p class="text-base w-[130px] capitalize font-semibold mb-2 whitespace-nowrap text-ellipsis overflow-hidden" x-text="apps[0].displayName"></p>
                    </div>
                </div> 
            </div>
            <template x-for="app in (search === '' ? getSSOApps('get') : searchResults)">
                <div class="apps-wrapper" :class="{'iws' : app.appType === 'iws' && !isPluginInstalled(), 'scale-up' : showMenu !== app.id}" x-show="app.displayName !== 'profile' && app.displayName !== 'dashboard' && app.alias !== 'winlogon-app'">
                    <div class="app-wrapper-link" @click="handleSSOClick(app.id)">
                        <div class="w-14 h-14 flex items-center justify-center border rounded-xl">
                            <img class="w-10 h-10" :src="app.displayName === 'dashboard' ? '${url.resourcesPath}/img/high_secure.svg'  : app.logoUrl" />
                        </div>
                        <div>
                            <p class="app-name"
                                x-text="app.displayName"></p>
                            <span class="sub-app" x-show="app.appType && app.appType !== 'iws' && app.appType !== 'high_secure'" x-text="app.appType"></span>
                        </div>
                    </div> 
                    <div class="flex items-center relative ml-auto pr-3" x-show="(app.appType !== 'iws' && app.id !== 'dashboard') || (app.appType === 'iws' && isPluginInstalled())">
                        <button  @click="handleFav(app)" >
                            <template x-if="app.liked"> 
                                <img class="w-6 h-6" alt="delete-btn"
                                    src="${url.resourcesPath}/img/star-edge.svg" />
                            </template>
                            <template x-if="!app.liked">
                                <img class="w-6 h-6" alt="three-dots-btn" 
                                    src="${url.resourcesPath}/img/star-stroke.svg" />
                            </template>
                        </button>
                        <button class="p-3 ml-3" @click="setShowMenu(app.id, true)">
                            <img alt="three-dots-btn" src="${url.resourcesPath}/img/three-dots-btn.svg" />
                        </button>
                        <div class="app-menu right-1 top-12"
                            x-show="showMenu === app.id"          
                            <#noautoesc>
                                ${transition}
                            </#noautoesc> 
                        >
                            <button class="menu-link border-b"
                                @click="currentTab='createGroup'; setShowMenu('',false)">
                                <img src="${url.resourcesPath}/img/add-folder.svg" />
                                ${msg('create-new-group')}
                            </button>
                            <button class="menu-link" @click="setShowMenu('',true);setShowPopup('addTo');">
                                <img src="${url.resourcesPath}/img/add-group.svg" />
                                ${msg('add-to-group')}
                            </button>
                        </div>
                    </div>
                </div>
            </template>
        </div>
    </template>
</#macro>