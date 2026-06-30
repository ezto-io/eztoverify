<#macro component>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <#assign transitionWithOutScaling="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform' x-transition:enter-end='opacity-100 transform' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform' x-transition:leave-end='opacity-0 transform'">
    <template x-if="currentTab==='favorites' && !isEmpty()">
        <div class="app-holder">
            <template x-for="app in (search === '' ? apps : searchResults)">
                <template x-if="app.liked">
                    <div class="apps-wrapper" :class="{'iws' : app.appType === 'iws' && !isPluginInstalled(), 'scale-up' : showMenu !== app.id}"  x-show="app.displayName !== 'profile'" >
                        <div class="app-wrapper-link" @click="handleSSOClick(app.id)">
                            <div class="w-14 h-14 flex items-center justify-center border rounded-xl">
                                <img class="w-10 h-10" :src="app.displayName === 'dashboard' ? '${url.resourcesPath}/img/high_secure.svg'  : app.logoUrl" />
                            </div>
                            <div>
                                <h4 class="app-name" x-text="app.displayName"></h4>
                                <span class="sub-app"x-show="app.appType && app.appType !== 'iws' && app.appType !== 'high_secure'" x-text="app.appType"></span>
                            </div>
                        </div>
                        <div class="flex items-center relative ml-auto mr-4">
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
                            <template x-if="showMenu === app.id">
                                <div class="app-menu right-1 top-12">
                                    <button class="menu-link"
                                        @click="currentTab='createGroup'; setShowMenu('',false)">
                                        <img src="${url.resourcesPath}/img/add-folder.svg" />
                                        ${msg('create-new-group')}
                                    </button>
                                </div>
                            </template>
                        </div>
                    </div>
                </template>
            </template>
        </div>
    </template>
</#macro>