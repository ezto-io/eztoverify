<#macro component>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <#assign transitionWithOutScaling="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform' x-transition:enter-end='opacity-100 transform' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform' x-transition:leave-end='opacity-0 transform'">
    <template x-if="dialog">
        <div class="popup-backdrop popup-bg z-10">
            <div class="popup p-10 min-w-[418px] gap-5">
                <div>
                    <p class="font-medium text-xl text-center" x-show="currentTab === 'most_recent'">
                        ${msg('your-recent-will-removed')}
                    </p>
                    <p class="font-medium text-xl text-center" x-show="currentTab === 'favorites'">
                        ${msg('your-favorite-will-removed')}
                    </p>
                    <p class="font-medium text-xl text-center"
                        x-show="currentTab === 'groups' && !showDeleteDialog && !showAppDeleteDialog">
                        ${msg('your-groups-will-removed')}
                    </p>
                    <p class="font-medium text-xl text-center" x-show="currentTab === 'groups' && showDeleteDialog">
                        ${msg('your-apps-will-removed')}
                    </p>
                    <p class="font-medium text-xl text-center" x-show="currentTab === 'groups' && showAppDeleteDialog">
                        ${msg('your-app-will-removed')}
                    </p>
                </div>
                <div>
                    <p class="text-gray-400 text-center mb-1">
                        ${msg('are-you-sure')}
                    </p>
                    <p class="text-gray-400 text-center m-0" x-show="currentTab === 'groups' && !showAppDeleteDialog && !showDeleteDialog">
                        ${msg('remove-all-groups-dialog')}
                    </p>
                    <p class="text-gray-400 text-center m-0" x-show="currentTab === 'most_recent'">
                        ${msg('remove-recent-groups')}
                    </p>
                    <p class="text-gray-400 text-center m-0" x-show="currentTab === 'favorites'">
                        ${msg('remove-favorites-groups')}
                    </p>
                    <p class="text-gray-400 text-center m-0" x-show="currentTab === 'groups' && showDeleteDialog">
                        ${msg('remove-all-group-apps')}
                    </p>
                    <p class="text-gray-400 text-center m-0" x-show="currentTab === 'groups' && showAppDeleteDialog">
                        ${msg('remove-all-app-in-group')}
                    </p>
                </div>
                <div class="popup-btn-holder justify-center gap-6">
                    <button @click="disableDialog()"
                        class="popup-btn popup-btn-secondary hover:popup-btn-secondary-hover">
                        ${msg('cancel')}
                    </button>
                    <button class="popup-btn popup-btn-danger popup-btn-danger-hover" @click="removeAll()">
                        ${msg('remove')}
                    </button>
                </div>
            </div>
        </div>
    </template>
</#macro>