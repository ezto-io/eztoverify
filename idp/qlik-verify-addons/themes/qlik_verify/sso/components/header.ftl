<#macro component url realm>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <#assign transitionWithOutScaling="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform' x-transition:enter-end='opacity-100 transform' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform' x-transition:leave-end='opacity-0 transform'">
    <header class="bg-white w-full flex justify-center py-2 md:px-6 px-0 fixed top-0 z-10 shadow-sm">
        <div class="flex items-center justify-between w-full max-w-[1360px]">
            <div class="flex items-center gap-6">
                <#if realm.getRealmLogo()?? && realm.getRealmLogo()!='' && (realm.getRealmLogo()?starts_with('data:image') || 
                    realm.getRealmLogo()?starts_with('http')) >
                    <img class="h-10 w-10" src="${realm.getRealmLogo()}" alt="tenant logo image"/>
                <#elseif realm.getRealmLogo()?? && realm.getRealmLogo()!=''>
                    <img class="h-10 w-10" src="data:image/png;base64,${realm.getRealmLogo()}" alt="tenant logo image"/>
                <#else>
                    <img class="h-10 w-10" src="${url.resourcesPath}/img/eztoauth-logo.svg"  alt="tenant logo image"/>
                </#if> 
                <h1 class="border-l pl-7 py-1 text-primary-950 text-lg font-medium">
                    ${realm.getDisplayName()}
                </h1>
            </div>
            <div class="flex items-center gap-5 relative">
                <#if showLanguageSelector?? && showLanguageSelector=="true">
                    <select onchange="handleLangChange()" class="border p-2 rounded border-2 text-gray-950" id="locale-select">
                    </select>
                </#if>
                <button @click="setShowPopup('downloadSso')"
                    class="popup-btn py-2 w-36 rounded popup-btn-success text-sm font-medium hover:bg-primary-950">
                    ${msg('install-sso-app')}
                </button>
                <button class="rounded-full w-10 h-10 overflow-hidden" @click="showProfile=!showProfile">
                    <#if ssouser.getUserPicture()?? && ssouser.getUserPicture() != ''>
                        <img class="w-full h-full" src="${ssouser.getUserPicture()}" alt="user_img">
                    <#else>
                        <div class="flex justify-center items-center font-semi-bold w-full h-full bg-primary-950 text-white">${ssouser.getUsername()[0]?upper_case}</div>
                    </#if>
                </button>
                <template x-if="showProfile">
                    <div id="profile-dropdown" class="bg-primary-950 text-white rounded flex flex-col absolute top-16 right-0 px-4 w-52">
                         <a :href="profileUrl" target="_blank" class="flex py-4 gap-3 border-b items-center text-sm">
                            <#if ssouser.getUserPicture()?? && ssouser.getUserPicture() != ''>
                                <img class="rounded-full w-6 h-6" src="${ssouser.getUserPicture()}" alt="user_img">
                            <#else>
                                <div class="rounded-full flex justify-center items-center font-semi-bold w-6 h-6 text-primary-950 bg-white">${ssouser.getUsername()[0]?upper_case}</div>
                            </#if>
                            ${msg('profile')}
                        </a>
                        <button @click="logout()" class="flex py-4 gap-3 items-center text-sm">
                            <img class="w-6 h-6 <#if user_preferred_lang == 'ar'>rotate-180</#if>" src="${url.resourcesPath}/img/logout.svg" />
                            ${msg('sign-out')}
                        </button>
                    </div>
                </template>
            </div>
        </div>
    </header> 
</#macro>