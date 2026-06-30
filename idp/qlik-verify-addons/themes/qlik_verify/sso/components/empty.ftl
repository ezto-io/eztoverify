<#macro component>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <template x-if="isEmpty()">
                <div>
                    <template x-if="currentTab === 'all'">
                        <div class="flex  h-[40rem] w-full justify-center items-center">
                            <p class="text-base text-gray-500">No SSO apps found</p>
                        </div>
                    </template>
                    <template x-if="currentTab === 'groups'">
                        <div class="flex flex-col h-[40rem] justify-center items-center gap-4">
                            <button @click="showCreateGroup()"
                                class="w-[10rem] h-[10rem] rounded-lg bg-primary-100 flex items-center justify-center">
                                <img src="${url.resourcesPath}/img/add-icon.svg" />
                            </button>
                            <p>${msg('create-new-group')}</p>
                        </div>
                    </template>
                    <template x-if="currentTab === 'favorites'">
                        <div class="flex  h-[40rem] w-full justify-center items-center">
                            <p class="text-base text-gray-500">${msg('no-favorites-to-all')}</p>
                        </div>
                    </template>
                    <template x-if="currentTab === 'most_recent'">
                        <div class="flex  h-[40rem] w-full justify-center items-center">
                            <p class="text-base text-gray-500">${msg('no-opened-apps')}</p>
                        </div>
                    </template>
                </div>
            </template>
</#macro>