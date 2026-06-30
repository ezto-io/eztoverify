<#macro component url>
    <#assign transition="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform scale-90' x-transition:enter-end='opacity-100 transform scale-100' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform scale-100' x-transition:leave-end='opacity-0 transform scale-90'">
    <#assign transitionWithOutScaling="x-transition:enter='transition ease-out duration-300' x-transition:enter-start='opacity-0 transform' x-transition:enter-end='opacity-100 transform' x-transition:leave='transition ease-in duration-300' x-transition:leave-start='opacity-100 transform' x-transition:leave-end='opacity-0 transform'">
    <div x-cloak x-show="showPopup.show" class="popup-backdrop" :class="{'popup-bg': (showPopup.subTab && !showMenu)}" @click="disablePopup()"
        <#noautoesc>
            ${transitionWithOutScaling}
        </#noautoesc>
        >
        <div x-show="showPopup.subTab == 'downloadSso'"
            class="bg-white flex flex-col w-[700px] md:w-full p-10 rounded gap-11 shadow">
            <div class="flex justify-between">
                <h3 class="font-medium text-xl">${msg('install-sso')}</h3>
                <button @click="showPopup.show = false;"><img class="w-6 h-6"
                        src="${url.resourcesPath}/img/close.svg" /></button>
            </div>
            <div class="flex justify-between">
                <div class="flex gap-3 items-center flex-wrap"><img class="w-10 h-10"
                        src="${url.resourcesPath}/img/downloads/apple.svg" alt="sso logo" />
                    <h4>${msg('mac')}</h4>
                </div>
                <a href="/auth/realms/${realm.getName()}/sso/downloads/download/sso-mac" target="_blank" class="sso-download-btn">${msg('mac-store')}</a>
            </div>
            <div class="flex justify-between">
                <div class="flex gap-3 items-center  flex-wrap"><img class="w-8 h-8"
                        src="${url.resourcesPath}/img/downloads/windows.svg" alt="sso logo" />
                    <h4>${msg('windows')}</h4>
                </div>
                <a href="/auth/realms/${realm.getName()}/sso/downloads/download/sso-win"  target="_blank"  class="sso-download-btn">${msg('mircrosoft-store')}</a>
            </div>
            <div class="flex justify-between">
                <div class="flex gap-3 items-center  flex-wrap"><img class="w-8 h-8"
                        src="${url.resourcesPath}/img/downloads/app-store.svg" alt="sso logo" />
                    <h4>${msg('ios')}</h4>
                </div>
                <a href="/auth/realms/${realm.getName()}/sso/downloads/download/sso-ios"  target="_blank"  class="sso-download-btn">${msg('app-store')}</a>
            </div>
            <div class="flex justify-between">
                <div class="flex gap-3 items-center flex-wrap"><img class="w-8 h-8"
                        src="${url.resourcesPath}/img/downloads/play-store.svg" alt="sso logo" />
                    <h4>${msg('android')}</h4>
                </div>
                <a href="/auth/realms/${realm.getName()}/sso/downloads/download/sso-android"  target="_blank"  class="sso-download-btn">${msg('play-store')}</a>
            </div>
            <div class="flex justify-between  flex-wrap">
                <button @click="showPopup.subTab = 'qr'"
                    class="sso-download-btn ml-auto bg-primary-950 text-white">${msg('scan-qr')}</button>
            </div>
        </div>
        <div x-show="showPopup.subTab == 'qr' "
            class="bg-white flex flex-col w-[500px] md:w-full p-10 rounded gap-11 shadow-lg">
            <div class="flex gap-4 items-center">
                <button @click="showPopup.subTab = 'downloadSso'">
                    <img class="w-4 h-4 <#if user_preferred_lang == 'ar'>rotate-180</#if>"  src="${url.resourcesPath}/img/back.svg" />
                </button>
                <h3 class="font-medium text-xl">${msg('to-install-sso-app')}</h3>
                <button @click="showPopup.show = false" class="ml-auto"><img class="w-6 h-6"
                        src="${url.resourcesPath}/img/close.svg" /></button>
            </div>
            <div class="w-full flex flex-col items-center gap-4">
                <img class="w-44"
                    src="data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAgAAAAIAAQAAAADcA+lXAAADjUlEQVR4Xu2aMXbbQAxEoZdCpY/Ao+ho1NF4FB6BpQs/bXZmsCRlJUWSQsh7M4W5wgJfzQ4Mrh3tHxXfA38qAwyADDAAMsAAyAADIAMMgAwwACoD2GJobo+4IfQV83ZpS4Y/Mv9rT5wUMMAAqA7gMvbnzrp99n2kMhrxo/98jOichVM+9TDAAKgEYMH5xv4DJ52nvhsA0Q64g5UA+OZqgAHlAfJCW/vHDxqADpEXhlkMMOA/APRPC/aRymhbuxc6i1UGGFAeQGXVlUtMKGDFPqHIFiqc8qmHAQZABQAxlIDdCy/LIQMMqAh4Efc3TdgUl9+zmgHQr2KQAW8ApBfGUe+nPvJiT70cuoGlYYVRAwyoCUBwzVmk/8RSXtCwreypjbl7yAADGNLj/YD8ACG1n/rpOVXfQId07BJMYE3W6oMBBuDD2wFnA3TdcyxZUbfb4psMMACqBFhwbxfDC0i9hwzQ5xYMK5FzC7yAV0gDDCgIQNfuj+Oo8wpvQ4FYH7quRhQjjMzCRD0MMAAqA8AbYo4la9AWrIrzKyS90EOTAQbUBISOOsYSVOX+cjT7UbXtIwxkgAFQGUCTAeAFjNVNBpBDsHwSvWCAASUB8ELwqN+xDxaiTKWyw08IP/QrQDLAAKgOgAbgAILHZ6jDBx2yHhMKpnE45JNfZoABRQGswrCNrq0OvwiQXthfLF8mFAMMKAMYqaiKnLDZwPfbPEXn41eAAQaUA3SxKr2gV0g08CGxJHX4lAEGQMUA0KwOf/ICJZa+oXGEmbRhgAFQJcAeZBV7uVicW9TsNcIcJdg9lgYYAL0f0GQApKKth14WB4sXe0jAMsZtiAGQAYUAG486uzZKrw1LqEeRiigB8AKxQwYYABUD4NTHGECerkDQ4fkNwYTlN14wwIAKgKuWEF4Lk9U4oUBMEJYJkgEGcFePGoC+NWE/xl9TsoHHpLZ+1vFlBhhQCnDSaZbeeOrHeyOWR3SkG2AAVAaAKml/Q0QVWUq4MHvKi5FkIZT7BhiA/fcDuI8BZG77LH3XWC1W9v3MffWCAQZkEHorgPt51Btv86Bbph5mOUcNMKAyINTW93sPRCO9EKeoAQYUB7Brs0pXICMB2ROaPf81yQADqgKo9AK6tliL/vLChCesAQbUBPB4QwBA3Od/2UlarsG/h+8JBkAGFAL8vQwwADLAAMgAAyADDIAMMAAywADIgNZ+AiYknf31ml0QAAAAAElFTkSuQmCC"
                    alt="qr">
                <p class="text-slate-700">${msg('scan-the-qr-to-login')}</p>
                <p class="text-slate-700">${msg('or')}</p>
                <p class="text-slate-700">${msg('enter-given-details')}</p>
            </div>
            <div class="flex flex-col gap-3 border border-gray-300 p-4 rounded ">
                <p class="text-sm text-slate-700">${msg('workspace-name')}</p>
                <h3 class="text-primary-950 font-semibold tracking-wider">${realm.getName()}</h3>
                <p class="text-sm text-slate-700">${msg('workspace-url')}</p>
                <a class="text-primary-950 break-all font-semibold tracking-wider  <#if user_preferred_lang == 'ar'>text-ltr</#if>"
                    href="${baseURI}">${baseURI}</a>
            </div>
        </div>
        <div class="popup" x-show="showPopup.subTab== 'addTo'">
            <h4 class="font-semibold text-lg">${msg('select-the-group')}</h4>
            <template x-if="showPopup.subTab== 'addTo'">
                <select id="groupName" x-ref="groupName" x-model="addToGroup"
                    class="border rounded-lg border-gray-200 p-3 w-[25rem]">
                    <option value="" disabled>${msg('select-the-group')}</option>
                    <template x-for="group in groups" :key="group.groupName">
                        <option x-bind:value="group.groupName" x-text="group.groupName"></option>
                    </template>
                </select>
            </template>
            <div class="popup-btn-holder" x-show="showPopup.subTab== 'addTo'">
                <button @click="setShowPopup(false)"
                    class="popup-btn popup-btn-danger hover:popup-btn-danger-hover">
                    ${msg('cancel')}
                </button>
                <button x-bind:disabled="!addToGroup" @click="addTo()" class="popup-btn popup-btn-success">
                    ${msg('add')}
                </button>
            </div>
        </div>
        <div class="popup" x-show="showPopup.subTab == 'createGroup' ">
            <h4 class="font-semibold text-lg">${msg('create-group-name')}</h4>
            <input x-model="createGroupName" x-on:input="createError=false" type="text" placeholder="Enter the Group Name"
                class="border rounded-lg border-gray-200 w-[30rem] p-3 focus:outline-primary-950"  onkeypress="preventSpecialChars(event)" @keyup.enter="createGroupName !== '' && createGroup()"/>
            <span x-show="createError" x-text=createError class="text-red-500 text-sm"></span>
            <div class="popup-btn-holder">
                <button @click="setShowPopup(false);changeTab('all');createGroupName=''"
                    class="popup-btn popup-btn-danger hover:popup-btn-danger-hover">
                    ${msg('cancel')}
                </button>
                <button x-bind:disabled="!createGroupName" @click="createGroup()" class="popup-btn popup-btn-success">
                    ${msg('create')}
                </button>
            </div>
        </div>
    </div>
</#macro>    