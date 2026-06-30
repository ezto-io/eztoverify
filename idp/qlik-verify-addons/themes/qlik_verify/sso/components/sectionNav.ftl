<#macro component url>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <#assign transitionWithOutScaling="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform' x-transition:enter-end='opacity-100 transform' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform' x-transition:leave-end='opacity-0 transform'">
    <div class="flex sm:flex-col lg:flex-row item-center flex-wrap gap-2 w-full justify-between py-2 border-b border-gray-200 py-5">
        <div class="flex gap-2">
            <button class="sso-btn" @click="changeTab('all')"
                :class="{'popup-btn-success' : currentTab==='all' ||  currentTab==='createGroup'}">
                ${msg('all')}
            </button>
            <button class="sso-btn" @click="changeTab('most_recent'); "
                :class="{'popup-btn-success' : currentTab==='most_recent'}">
                 ${msg('most-recent')}
            </button>
            <button class="sso-btn" @click="changeTab('favorites')"
                :class="{'popup-btn-success' : currentTab==='favorites'}">
                 ${msg('favorites')}
                <span x-cloak ${transitionWithOutScaling} x-show="favDot" class="fav-dots"></span>
            </button>
            <button class="sso-btn" @click="changeTab('groups')"
                :class="{'popup-btn-success' : currentTab==='groups' ||  currentTab==='addNewApp'}">
                 ${msg('groups')}
            </button>
        </div>
        <div class="flex gap-2">
            <div x-cloak
                class="flex items-center justify-start gap-2 bg-white p-3 border rounded-md border-gray-200 h-[43px] transition-width duration-1000 ease-in-out" :class="{'w-[20rem]' : currentTab!=='groups', 'w-[25rem]' : currentTab ==='groups' }">
                <img src="${url.resourcesPath}/img/search-icon.svg" alt="search" />
                <input class="w-full outline-none" type="text" x-model="search" x-on:input="handleSearch"
                    placeholder="${msg('enter-key-search')}" />
                <select
                    x-cloak x-show="currentTab==='groups'" 
                    class="text-gray-600 outline-none" x-model="searchFilter"
                    <#noautoesc>
                        ${transition}
                    </#noautoesc> 
                >
                <option value="apps"> ${msg('apps')}</option>
                <option value="groups"> ${msg('groups')}</option>
                </select>
                <button x-cloak x-show="search" @click="search=''">
                    <img class="w-4 h-4" src="${url.resourcesPath}/img/close.svg" />
                </button>
            </div>
            <button class="bg-white px-4 border border-gray-200 rounded-md relative" :class="{'z-10' : showOptions}"
                @click="showOptions = !showOptions; showPopup.show = !showPopup.show; showPopup.subTab = '' ">
                <img src="${url.resourcesPath}/img/three-dots-icon.svg" alt="three dots" />
                <template x-if="showOptions && currentTab==='all'">
                    <div class="app-menu top-14 right-0 left-initial">
                        <button class="menu-link" @click.debounce="showCreateGroup()">
                            <img src="${url.resourcesPath}/img/add-folder.svg" /> 
                            ${msg('create-new-group')}
                        </button>
                    </div>
                </template>
                <template x-if="showOptions && currentTab==='most_recent'">
                    <div class="app-menu top-14 right-0 left-initial">
                        <button class="menu-link border-b" @click.debounce="showCreateGroup()">
                            <img src="${url.resourcesPath}/img/add-small-icon.svg" />
                             ${msg('create-new-group')}
                        </button>
                        <button class="menu-link" @click.debounce="dialog=true;">
                            <img src="${url.resourcesPath}/img/remove-small-icon.svg" />
                             ${msg('remove-all-recent-apps')}
                        </button>
                    </div>
                </template>
                <template x-if="showOptions && currentTab==='favorites'">
                    <div class="app-menu top-14 right-0 left-initial">
                        <button class="menu-link border-b" @click.debounce="showCreateGroup()">
                            <img src="${url.resourcesPath}/img/add-small-icon.svg" />
                             ${msg('create-new-group')}
                        </button>
                        <button class="menu-link" @click="dialog=true;">
                            <img src="${url.resourcesPath}/img/remove-small-icon.svg" />
                            ${msg('remove-all-favorites')}
                        </button>
                    </div>
                </template>
                <template x-if="showOptions && currentTab==='groups'">
                    <div class="app-menu top-14 right-0 left-initial">
                        <button class="menu-link" @click="dialog=true;">
                            <img src="${url.resourcesPath}/img/remove-small-icon.svg" />
                             ${msg('remove-all-groups')}
                        </button>
                    </div>
                </template>
            </button>
        </div>
    </div>
</#macro>