<#macro component>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <template x-if="currentTab==='addNewApp'">
        <div class="flex gap-5 flex-col max-h-[40rem]">
            <div class="flex items-center">
                <button class="flex items-center gap-1 hover:text-primary-950" @click="changeTab('groups')">
                    <img class="w-6 h-6 <#if user_preferred_lang == 'ar'> rotate-180  </#if>" src="${url.resourcesPath}/img/left-arrow.svg" alt="right-arrow"/>
                    <p class="text-gray-700"> ${msg('go-back-to')} </p>
                    <p class="text-gray-700" x-text="currentGroup.groupName"></p>
                </button>
                <div class="ml-auto flex items-center gap-6">
                    <div class="flex items-center gap-1">
                        <input id="checkAll" type=checkbox x-model="selectAll" :change.debounce="handleSelect()" :disabled="selectedGroup.length === filteredApps.length"/>
                        <label for="checkAll" class="accent-primary-950">
                           ${msg('select-all')}
                        </label>
                    </div>
                    <button @click="addApp()" class="p-3 sso-btn popup-btn-success ml-auto py-0" :disabled="selectedGroup.length === filteredApps.length || filteredApps.findIndex(a => a.checked) < 0">
                        ${msg('add')}
                    </button>
                </div>
            </div>
            <div class="app-holder">
                <template x-for="app in (search === '' ? filteredApps : searchResults)">
                    <div class="apps-wrapper" x-show="app.displayName !== 'profile' && app.displayName !== 'dashboard' && app.alias !== 'winlogon-app' && selectedGroup?.findIndex(a => a.id === app.id) < 0" @click="app.checked = !app.checked">
                        <div class="app-wrapper-link">
                            <div class="w-14 h-14 flex items-center justify-center border rounded-xl">
                                <img class="w-10 h-10" :src="app.displayName === 'dashboard' ? '${url.resourcesPath}/img/high_secure.svg'  : app.logoUrl" />
                            </div>
                            <div>
                                <h4 class="app-name" x-text="app.displayName"></h4>
                                <span class="sub-app"x-show="app.appType && app.appType !== 'iws' && app.appType !== 'high_secure'" x-text="app.appType"></span>
                            </div>
                        </div>
                        <div class="flex items-center mr-4">
                            <input type="checkbox"
                                class="border accent-primary-950 rounded h-4 w-4 border-gray-400"
                                x-model="app.checked" />
                        </div>
                    </div>
                </template>
                <div class="group-empty-text" x-show="selectedGroup.length === filteredApps.length">
                    <p class="gray-text">${msg('no-avilable-apps-to-add')}</p>
                </div>
                <div>
            </div>
        </div>
    </template>
</#macro>