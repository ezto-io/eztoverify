<!DOCTYPE html PUBLIC "-//W3C//DTDHTML4.01Transitional//EN">

<html lang="${locale}" <#if locale == 'ar'> dir="rtl" </#if> >

<head>
     <#if mode?? && mode == 'wiam'>
        <title>${msg("welcome-to-ea")}</title>
    <#else>
         <title>${msg("welcome-to-ev")}</title>
    </#if>
    <meta charset="UTF-8" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="X-UA-Compatible" content="ie=edge" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="robots" content="noindex, nofollow">
    <#if variant?? && variant == 'ezto verify'>
    <link rel="icon" href="${resourcesPath}/img/eztovrfy-favicon.ico">
    <#elseif variant?? && variant == 'ezto auth'>
    <link rel="icon" href="${resourcesPath}/img/eztoauth-favicon.ico">
    </#if>
    <link rel="stylesheet" href="${resourcesPath}/css/dist<#if locale == 'ar'>-rtl</#if>.css" type="text/css" />
    <script defer type="text/javascript" src="${resourcesPath}/js/alpine.min.js"></script>
    <!-- Phone Validation CSS & JS -->
    <link rel="stylesheet" href="${resourcesPath}/js/tele-input/tele-input.min.css" type="text/css" />
    <script type="text/javascript" src="${resourcesPath}/js/tele-input/tele-input.min.js"></script>
    <script type="text/javascript" src="${resourcesPath}/js/tele-input/utils.min.js"></script>
    <!-- For Multiselect -->
     <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <!-- Select2 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css" rel="stylesheet" />
    <!-- Select2 JS -->
    <script src="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js"></script>
    <script nomodule type="text/javascript" src="${resourcesPath}/js/alpine.ie11.min.js" defer></script>
    <script src="https://cdn.jsdelivr.net/npm/iso-639-1@3.1.2/build/index.min.js"></script>
    <style>
    <#if mode?? && mode == 'wiam'>
        <#assign hover_100 = "hover:bg-[#4CA355]">
        <#assign hover_500 = "hover:bg-[#4CA355]">
        <#assign focus_500 = "focus:outline-[#4CA355]">
        <#assign peerChecked_600 = "peer-checked:bg-[#4CA355]">
        :root {
            --primary: #4CA355;
        }
    <#elseif mode ?? && mode == 'ciam'>
        <#assign hover = "hover:bg-[#323232]">
        <#assign hover_500 = "hover:bg-[#323232]">
        <#assign focus_500 = "focus:outline-[#323232]">
        <#assign peerChecked_600 = "peer-checked:bg-[#323232]">
        :root {
            --primary: #323232;
        }
    <#elseif mode?? && mode =='bo'>
        :root {
            --primary: #1c64eb;
        }
        <#assign hover_100 = "hover:bg-blue-100">
        <#assign hover_500 = "hover:bg-blue-500">
        <#assign focus_500 = "focus:outline-blue-500">
        <#assign peerChecked_600 = "peer-checked:bg-blue-600">
    </#if>
    .bg-primary{
        background-color: var(--primary);
    }
    .border-primary {
        border-color: var(--primary);
    }
    .text-primary {
        color: var(--primary);
    }
    .after-bg-primary:after {
        background-color: var(--primary);
    }
    .outline-primary {
        outline: var(--primary)
    }
    </style>
</head>
<body class="font-sans text-sm text-textPrimary" x-data="{showError: true}">
    <#if localUser?? && localUser>
        <#if onboardingCompleted?? && onboardingCompleted>
                <div class="min-h-screen w-full bg-[#f2f4f8]">
                    <#if stateChecker??>
                        <input id="stateChecker" name="stateChecker" type="hidden" value="${stateChecker}" />
                    </#if>
                    <section class="min-h-full min-w-full relative pt-10 pl-10 sm:pt-5 sm:pl-5"
                    >
                        <div class="max-w-full xl:h-auto">
                            <div class="flex gap-2 items-center mb-10">
                            <#if variant?? && variant == 'ezto verify'>
                                <img class="w-[215px]" src="${resourcesPath}/img/eztovrfy-logo.png" alt="ezto verify"/>
                                <#elseif variant?? && variant == 'ezto auth'>
                                <img class="w-[215px]" src="${resourcesPath}/img/eztoauth-logo.png" alt="ezto verify"/>
                            </#if>
                            </div>
                        </div>
                    </section>
                    <section>
                        <div
                        class="overflow-y-auto w-auto md:inset-0 md:h-full"
                        >
                            <div class="relative p-10 sm:p-5 w-full max-w-5xl h-full mx-auto">
                                <div
                                class="relative bg-white rounded-lg shadow-modalShadow mt-14 max-w-[1024px] flex flex-col justify-between  xl:w-full xl:h-auto"
                                >
                                    <div class="w-[500px] mx-auto pt-16 overflow-y-scroll mb-8 pb-8 xl:w-full xl:px-8 scrollbar-hide">
                                        <div class="mb-10 text-center">
                                            <div class="flex justify-center mb-4">
                                                <img src="${resourcesPath}/img/success.svg" alt="success logo" class="w-14 h-14 sm:w-14 sm:h-14" />
                                            </div>
                                            <h3 class="text-2xl font-semibold tracking-tight mb-4 sm:text-xl">
                                                ${msg("you-are-successfully-onboarded-txt")}
                                            </h3>
                                            <a id="qv_onboarding_dashboard_link" class="mt-1 text-white bg-primary ${hover_100} focus:outline-none font-medium rounded-lg text-sm px-5 py-2.5 text-center inline-block" href="${adminUrl}"
                                            >
                                                ${msg("take-me-to-dashboard-txt")}
                                            </a>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>
                </div>
        <#elseif obSteps?? && obSteps.steps?? && obSteps.currentStep?? && 0 <= obSteps.currentStep>
                <#assign currentStep = obSteps.steps[obSteps.currentStep]>
                <#assign steps = obSteps.steps>
                <#assign currentIndex = obSteps.currentStep>
                <#assign totalSteps = obSteps.steps?size>
                <#if 0 < obSteps.currentStep>
                    <#assign prevIndex = (obSteps.currentStep - 1)>
                </#if>
                <form @submit.prevent="handleSubmit(()=>$el.submit())" id="welcomeForm" x-ref="welcomeForm" class="grid grid-cols-12 min-h-screen w-full <#if !showPreview>xl:block</#if> xl:bg-[#f2f4f8]" action="${resourceUrl}" method="post" @input.debounce.100ms="handleForm();" x-init="$nextTick(() =>{
                    handleForm(<#if totalSteps-1 == currentIndex>'finalStep'<#elseif currentStep.skippable??>'skip'<#elseif errorString?? || errorMessage??>'error'<#else>false</#if>);
                    })" x-data="{isSwitch: false, switchStep: '${currentIndex}'}">
                    <#if stateChecker??>
                        <input id="stateChecker" name="stateChecker" type="hidden" value="${stateChecker}" />
                    </#if>
                    <input x-ref="isSwitch" id="isSwitch" name="isSwitch" type="hidden" x-model="isSwitch"/>
                    <input x-ref="switchStep" id="switchStep" name="switchStep" type="hidden" x-model="switchStep"/>
                    <input id="stepProviderId" name="stepProviderId" type="hidden" value="${currentStep.stepProviderId}"/>
                    <#if !showPreview>
                        <section
                            class="bg-white min-h-full min-w-full col-span-3 relative flex flex-col items-center pt-10 xl:col-span-full xl:bg-[#f2f4f8] xl:p-8 xl:pl-8 xl:items-start justify-between">
                            <div class="h-[650px] max-w-full xl:h-auto">
                                <div class="flex gap-2 items-center mb-10">
                                <#if variant?? && variant == 'ezto verify'>
                                    <img class="w-[215px]" src="${resourcesPath}/img/eztovrfy-logo.png" alt="ezto verify"/>
                                    <#elseif variant?? && variant == 'ezto auth'>
                                    <img class="w-[215px]" src="${resourcesPath}/img/eztoauth-logo.png" alt="ezto verify"/>
                                </#if>
                                </div>

                                <#if steps??>
                                    <ul class="space-y-5 xl:space-y-0 snap-y overflow-y-scroll max-h-full scroll-smooth relative xl:flex xl:flex-nowrap scrollbar-hide" x-ref="listContainer">
                                        <#list steps as step>
                                            <li class="flex snap-start xl:min-w-[250px] gap-4 items-center justify-start <#if step?index == currentIndex>p-2 rounded-lg text-white bg-primary<#else>pl-2</#if>">
                                                <div class="col-span-2">
                                                    <div
                                                        class="w-10 h-10 <#if step.getStepCompleted()?? && step.getStepCompleted()>bg-green-600<#elseif step?index == currentIndex>border-2 border-white<#else>border-2 border-primary</#if> rounded-full flex items-center justify-center relative"
                                                    >
                                                        <#if  step.getStepCompleted()?? && step.getStepCompleted()>
                                                                <img src="${resourcesPath}/img/tick.png" alt="" class="w-8 h-8" />
                                                            <#else>

                                                                <span class="font-semibold">${step?index + 1}</span>
                                                        </#if>
                                                    </div>
                                                </div>
                                                <div class="col-span-10 pr-2">
                                                    <h4 class="font-semibold tracking-tight mb-1 uppercase">${msg("${step.title}")}</h4>
                                                </div>
                                            </li>
                                        </#list>
                                    </ul>
                                </#if>
                            </div>
                            <div class="flex gap-1 items-center gap-3 mb-5 relative top-3 right-2 z-10 xl:absolute">
                                <label class="block mb-2 text-sm font-medium">${msg('language')}</label>
                                <select onchange="handleLangChange()" class="border p-2 rounded border-2 text-gray-950 outline-primary" id="locale-select"></select>
                            </div>
                        </section>
                    </#if>
                    <section class="bg-[#f2f4f8] <#if showPreview>col-span-7<#else>col-span-9 xl:col-span-full</#if>">
                        <div
                        class="overflow-y-auto w-auto md:inset-0 md:h-full scrollbar-hide"
                        >
                            <div class="relative p-8 w-full max-w-5xl h-full xl:h-auto xl:w-full xl:mx-auto pt-10">
                                <section class="w-full xl:mb-40 h-[90vh] max-h-[90vh] overflow-hidden flex flex-col justify-between pb-3 px-3 xl:h-auto xl:max-h-max">
                                    <div class="w-4/5 mx-auto mb-8 pb-8 xl:w-full xl:overflow-auto max-h-[80vh] overflow-scroll scrollbar-hide xl:max-h-max xl:h-auto">
                                        <div class="mb-10 px-2">
                                            <div class="flex justify-between w-full">
                                                <h3 class="text-2xl sm:text-xl">
                                                    <#if currentStep.title??>${msg("${currentStep.title}")}</#if>
                                                </h3>
                                                <#if showPreview>
                                                    <select onchange="handleLangChange()" class="border p-2 rounded border-2 text-gray-950 outline-primary" id="locale-select"></select>
                                                </#if>
                                            </div>
                                            <#if steps??>
                                                <#list steps as step>
                                                    <#if currentIndex==step?index >
                                                        <p class="border-b pb-2 text-sm text-[##727272] mt-3 relative after:content-[' '] after:block after:absolute after:w-8 after:bottom-[-3px] after:h-1 after-bg-primary ">${msg("${step.description}")}</p>
                                                    </#if>
                                                </#list>
                                            </#if>
                                        </div>
                                        <#if currentStep.properties?? && currentStep.properties??>
                                        <div class="space-y-6 w-full h-auto overflow-scroll xl:max-h-max scrollbar-hide mt-2 px-2" x-data="{
                                                                        <#list currentStep.properties as formField>
                                                                            <#if formField.dependsOn??>
                                                                                <#list formField.dependsOn as dependency, value>
                                                                                    ${dependency}: <#if value?is_string>'${value}'<#else>'${value?c}'</#if>,
                                                                                </#list>
                                                                            </#if>
                                                                        </#list>
                                                                    }">

                                            <#list currentStep.properties as formField>
                                                <#if formField.defaultValue?? && formField.defaultValue?is_hash && formField.defaultValue['${formField.name}']?? && formField.defaultValue.config??>
                                                    <#assign providerName = formField.defaultValue['${formField.name}']>
                                                    <#assign currentConfig = formField.defaultValue.config>
                                                </#if>
                                                <#if !formField.type??>
                                                         <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} === true ? 'block':'hidden'</#list><#else>'block'</#if>" class="mb-6" x-data="{value: '<#if formField.defaultValue??>${formField.defaultValue}</#if>'}">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <input id='${formField.name}' type="hidden" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <input name="${formField.name}" id="qv_onboarding_${formField.name}" type="text" value="<#if formField.defaultValue??>${formField.defaultValue}</#if>" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} block w-full p-2.5" placeholder="${msg("${formField.label}")}" @input="($el)=>{value=$el.target.value}"/>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'time'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{value: <#if formField.defaultValue??>${formField.defaultValue}</#if>,oldValue:'',timeObj: new TimeUnit(<#if formField.defaultValue??>${formField.defaultValue}<#else>120</#if>)}" x-init="$watch('timeObj', timeObjValue => {value = timeObj.toSeconds();})">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                               ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <input name="${formField.name}" type="hidden" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <div class="grid grid-cols-12 gap-4">
                                                                <input id="${formField.name}" min="1" type="number" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block col-span-6 p-2.5" x-model="timeObj.time"/>
                                                                <select id="qv_onboarding_${formField.name}" class="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg ${focus_500} focus:border-primary block col-span-6 p-2.5 custom-select" x-model="timeObj.unit">
                                                                        <option value="Minutes" :selected="timeObj.unit === 'Minutes'?true:false">${msg("minutes")}</option>
                                                                        <option value="Hours" :selected="timeObj.unit === 'Hours'?true:false">${msg("hours")}</option>
                                                                        <option value="Days" :selected="timeObj.unit === 'Days'?true:false">${msg("days")}</option>
                                                                </select>
                                                            </div>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'number'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{value: '<#if formField.defaultValue??>${formField.defaultValue}</#if>'}">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <input id='${formField.name}' type="hidden" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <input name="${formField.name}" id="qv_onboarding_${formField.name}" type="number" value="<#if formField.defaultValue??>${formField.defaultValue}</#if>" min="1" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5" @input="($el)=>{value=$el.target.value}"/>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'phoneregion'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{
                                                                                        error:'',
                                                                                        phoneObj: null,
                                                                                        value: '<#if formField.defaultValue??>${formField.defaultValue}</#if>',
                                                                                    }">
                                                                        <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                            ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                                        </label>
                                                                        <input id="${formField.name}" type="tel" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5 region-selector-input" maxlength="20" x-init="$nextTick(() => {
                                                                                const input = document.querySelector('#${formField.name}');
                                                                                phoneObj = window.intlTelInput(input, {
                                                                                    initialCountry: '<#if formField.defaultValue??>${formField.defaultValue}<#else>in</#if>',
                                                                                    allowDropdown : true,
                                                                                    utilsScript: '${resourcesPath}/js/tele-input/utils.js',
                                                                                    separateDialCode: false,
                                                                                    customPlaceholder: function(selectedCountryPlaceholder, selectedCountryData) {
                                                                                        value = selectedCountryData.iso2;
                                                                                        return selectedCountryData?.name;
                                                                                    },
                                                                                });
                                                                            })" >
                                                                        <input name="${formField.name}" type="hidden" id="${formField.name}" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                                        <#if formField.helpText??>
                                                                            <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                                        </#if>
                                                                        <span x-cloak x-show="error.length > 0" class="text-red-700 mt-3 text-sm <#if formField.required?? && formField.required?string == 'true'>required</#if>" x-text='error'></span>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'info'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6 flex rounded-[3px] bg-[#DBF4FC] border-l-8 border-solid border-[#68c5e1] gap-2 p-2 text-xs text-[#097093] items-center">
                                                            <img src="${resourcesPath}/img/info-icon.svg" class="w-5 h-5"/>
                                                            <p>${msg("${formField.label}")}</p>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'caution'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6 flex rounded-[3px] bg-[#fff6cb] border-l-8 border-solid border-[#ebca40] gap-2 p-2 text-xs text-[#786600] items-center">
                                                            <img src="${resourcesPath}/img/warning-icon.svg" class="w-5 h-5"/>
                                                            <p>${msg("${formField.label}")}</p>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'multiselect' || formField.type?lower_case == 'multivaluedList'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6 h-[350px]">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <select class="hidden" id="${formField.name}">
                                                                <#list formField.options as option>
                                                                    <option value="${option}" <#if formField.defaultValue?? && formField.defaultValue?seq_contains(option)>selected='true'</#if> >${option}</option>
                                                                </#list>
                                                            </select>
                                                            <div x-data="dropdown('${formField.name}')" x-init="loadOptions()" class="w-full md:w-1/2 flex flex-col items-center max-h-64 mx-auto">
                                                                <input name="${formField.name}" type="hidden" x-bind:value="selectedValues()"/>
                                                                <div class="inline-block relative w-full">
                                                                    <div class="flex flex-col items-center relative">
                                                                        <div x-on:click="open" class="w-full  svelte-1l8159u">
                                                                            <div class="my-2 p-1 flex border border-gray-200 bg-white rounded svelte-1l8159u">
                                                                                <div class="flex flex-auto flex-wrap">
                                                                                    <template x-for="(option,index) in selected" :key="options[option].value">
                                                                                        <div
                                                                                            class="flex justify-center items-center m-1 font-medium py-1 px-2 bg-blue-500/10 rounded-full text-blue-600 border border-primary">
                                                                                            <div class="text-xs font-normal leading-none max-w-full flex-initial" x-model="options[option]" x-text="options[option].text"></div>
                                                                                            <div class="flex flex-auto flex-row-reverse">
                                                                                                <div x-on:click="remove(index,option)">
                                                                                                    <svg class="fill-current h-6 w-6 " role="button" viewBox="0 0 20 20">
                                                                                                        <path d="M14.348,14.849c-0.469,0.469-1.229,0.469-1.697,0L10,11.819l-2.651,3.029c-0.469,0.469-1.229,0.469-1.697,0
                                                                                                c-0.469-0.469-0.469-1.229,0-1.697l2.758-3.15L5.651,6.849c-0.469-0.469-0.469-1.228,0-1.697s1.228-0.469,1.697,0L10,8.183
                                                                                                l2.651-3.031c0.469-0.469,1.228-0.469,1.697,0s0.469,1.229,0,1.697l-2.758,3.152l2.758,3.15
                                                                                                C14.817,13.62,14.817,14.38,14.348,14.849z" />
                                                                                                    </svg>

                                                                                                </div>
                                                                                            </div>
                                                                                        </div>
                                                                                    </template>
                                                                                    <div x-show="selected.length == 0" class="flex-1">
                                                                                        <input placeholder=${msg("select-option-txt")} id="qv_onboarding_${formField.name}"
                                                                                            class="bg-transparent p-1 px-2 appearance-none outline-none h-full w-full text-gray-800"
                                                                                            x-bind:value="selectedValues()"
                                                                                        />
                                                                                    </div>
                                                                                </div>
                                                                                <div
                                                                                    class="text-gray-300 w-8 py-1 pl-2 pr-1 border-l flex items-center border-gray-200 svelte-1l8159u">

                                                                                    <button id="qv_onboarding_open" type="button" x-show="isOpen() === true" x-on:click="open"
                                                                                        class="cursor-pointer w-6 h-6 text-gray-600 outline-none focus:outline-none">
                                                                                        <svg version="1.1" class="fill-current h-4 w-4" viewBox="0 0 20 20">
                                                                                            <path d="M17.418,6.109c0.272-0.268,0.709-0.268,0.979,0s0.271,0.701,0,0.969l-7.908,7.83
                                                            c-0.27,0.268-0.707,0.268-0.979,0l-7.908-7.83c-0.27-0.268-0.27-0.701,0-0.969c0.271-0.268,0.709-0.268,0.979,0L10,13.25
                                                            L17.418,6.109z" />
                                                                                        </svg>

                                                                                    </button>
                                                                                    <button id="qv_onboarding_close" type="button" x-show="isOpen() === false" @click="close"
                                                                                        class="cursor-pointer w-6 h-6 text-gray-600 outline-none focus:outline-none">
                                                                                        <svg class="fill-current h-4 w-4" viewBox="0 0 20 20">
                                                                                            <path d="M2.582,13.891c-0.272,0.268-0.709,0.268-0.979,0s-0.271-0.701,0-0.969l7.908-7.83
                                                            c0.27-0.268,0.707-0.268,0.979,0l7.908,7.83c0.27,0.268,0.27,0.701,0,0.969c-0.271,0.268-0.709,0.268-0.978,0L10,6.75L2.582,13.891z
                                                            " />
                                                                                        </svg>

                                                                                    </button>
                                                                                </div>
                                                                            </div>
                                                                        </div>
                                                                        <div class="w-full px-4">
                                                                            <div x-show.transition.origin.top="isOpen()"
                                                                                class="absolute shadow top-100 bg-white z-40 w-full lef-0 rounded max-h-select overflow-y-auto svelte-5uyqqj"
                                                                                x-on:click.away="close">
                                                                                <div class="flex flex-col w-full max-h-[200px]">
                                                                                    <template x-for="(option, index) in options" :key="index">
                                                                                        <div x-bind:class="option.selected ? 'hidden' : 'border-transparent'">
                                                                                            <div class="cursor-pointer w-full border-gray-100 rounded-t border-b ${hover_100}"
                                                                                                @click="select(index,$event)">
                                                                                                <div x-bind:class="option.selected ? 'border-blue-600' : 'border-transparent'"
                                                                                                    class="flex w-full items-center p-2 pl-2 border-l-2 relative">
                                                                                                    <div class="w-full items-center flex">
                                                                                                        <div class="mx-2 leading-6" x-model="option" x-text="option.text"></div>
                                                                                                    </div>
                                                                                                </div>
                                                                                            </div>
                                                                                        </div>
                                                                                    </template>
                                                                                </div>
                                                                            </div>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'email_table'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="my-8" x-data="{
                                                            value: '',
                                                            invitedEmails: [<#if formField.defaultValue?? && formField.defaultValue.invitedUsers??><#list formField.defaultValue.invitedUsers as invitedEmail>'${invitedEmail}',</#list></#if>],
                                                            emails :[<#if formField.defaultValue?? && formField.defaultValue.invitedUsers??><#list formField.defaultValue.invitedUsers as invitedEmail>{value:'${invitedEmail}',invited: true,id:${invitedEmail?index}},</#list></#if>{value:'', id: <#if formField.defaultValue?? && formField.defaultValue.invitedUsers??>${formField.defaultValue.invitedUsers?size}<#else>0</#if>}],
                                                            adminEmail : '<#if formField.defaultValue?? && formField.defaultValue.realmAdmin??>${formField.defaultValue.realmAdmin}</#if>',
                                                            changeValue(value, id){
                                                                let invitedArr = [...this.emails.map(email=>email.value)];
                                                                if(invitedArr.length>0 && invitedArr[invitedArr.length-1]===value){
                                                                    invitedArr.pop();
                                                                }
                                                                if(this.adminEmail){
                                                                    invitedArr.push(this.adminEmail);
                                                                }
                                                                if(invitedArr.includes(value.trim())){
                                                                    this.emails[id]['err'] = 'Please add a email which is not invited and not admin email.';
                                                                }else if(validateEmail(value)) {
                                                                    this.emails[id].value = value;
                                                                    this.emails[id]['err'] = '';
                                                                } else {
                                                                    this.emails[id]['err'] = 'Enter valid email id.';
                                                                }
                                                            },
                                                            updateValue(updateType, id){
                                                                if(updateType === 'delete') {
                                                                    let result = this.emails.splice(id, 1);
                                                                }else if(updateType === 'update') {
                                                                    this.emails[id] = {value: document.getElementById('email-input-'+(id+1)).value, id: id + 1}
                                                                } else{
                                                                    this.emails.push({value: '', id: id + 1})
                                                                }
                                                                let notInvitedArr = this.emails.filter(email=>(!email?.invited));
                                                                this.value = notInvitedArr.map(email=>email.value).join();
                                                            }
                                                        }">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                   ${msg("${formField.label}")}
                                                            </label>
                                                            <div class="overflow-x-auto relative shadow-md sm:rounded-lg">
                                                                <table class="w-full text-sm text-left text-gray-500">
                                                                    <thead class="text-xs text-gray-700 uppercase bg-gray-50">
                                                                        <tr>
                                                                            <th scope="col" class="py-3 px-6 text-left">
                                                                                ${msg("email-txt")}
                                                                            </th>
                                                                            <th scope="col" class="py-3 px-6 text-center">
                                                                                ${msg("action-txt")}
                                                                            </th>
                                                                        </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                        <template x-for="(email, index) in emails" :key="index">
                                                                            <tr class="border-b hover:bg-gray-50">
                                                                                <th scope="row" class="py-4 px-6 font-medium text-gray-900 whitespace-nowrap">
                                                                                    <template x-if="index === emails.length - 1 && !(email?.invited)">
                                                                                        <input :id="$id('email-input')" type="text" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} block w-full p-2.5" @input="($el)=>{changeValue($el.target.value, index)}" x-model="email.value"/>
                                                                                        <span x-cloak x-show="email?.err?.length > 0" class="text-red-700 mt-3 text-sm block" x-text='email.err'></span>
                                                                                    </template>
                                                                                    <template x-if="email?.invited === true || (index < emails.length - 1 && !(email?.invited))">
                                                                                        <input :id="$id('email-input')" type="text" class="bg-white border border-gray-300 text-sm rounded-lg block w-full p-2.5 cursor-auto" readonly x-model="email.value"/>
                                                                                    </template>
                                                                                </th>
                                                                                <template x-if="index === emails.length - 1 && !(email?.invited)">
                                                                                    <td class="py-4 px-6 text-center">
                                                                                        <button id="qv_onboarding_addBtn" x-bind:class="( email?.err || !email.value ) ? 'opacity-50 pointer-events-none' : ''" type="button" class="text-white bg-primary ${hover_500} font-medium rounded-lg text-sm px-5 py-2.5 mr-2 mb-2 focus:outline-none" @click="updateValue('add', index)">${msg("add-txt")}</button>
                                                                                    </td>
                                                                                </template>
                                                                                <template x-if="index < emails.length - 1 && !(email?.invited)">
                                                                                    <td class="py-4 px-6 text-center">
                                                                                        <button id="qv_onboarding_deleteBtn" type="button" class="focus:outline-none text-white bg-red-600 hover:bg-red-800 font-medium rounded-lg text-sm px-5 py-2.5 mr-2 mb-2" @click="updateValue('delete', index)">${msg("delete-txt")}</button>
                                                                                    </td>
                                                                                </template>
                                                                                <template x-if="email?.invited === true">
                                                                                    <td class="py-4 px-6 text-center">
                                                                                        <button id="qv_onboarding_inviteBtn" type="button" class="text-white bg-green-500 opacity-50 pointer-events-none font-medium rounded-lg text-sm px-5 py-2.5 mr-2 mb-2 focus:outline-none">${msg("invited")}</button>
                                                                                    </td>
                                                                                </template
                                                                            </tr>
                                                                        </template>
                                                                    </tbody>
                                                                </table>
                                                            </div>
                                                            <input name="${formField.name}" id='${formField.name}' type="hidden" x-model="value"/>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-3">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'image_upload'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{
                                                                                    value: '',
                                                                                    previousValue: '',
                                                                                    openLogoSelector(){
                                                                                        let imgTag = document.querySelector('#${formField.name}');
                                                                                        imgTag.click();
                                                                                    },
                                                                                    getBase64: async function(file) {
                                                                                        return new Promise((resolve, reject) => {
                                                                                            const reader = new FileReader();
                                                                                            const extension = file.name.split('.').pop();
                                                                                            reader.readAsDataURL(file);
                                                                                            reader.onload = () => resolve(
                                                                                               file.type + ','+ reader.result.split(',')[1]
                                                                                            );
                                                                                            reader.onerror = error => reject(error);
                                                                                        });
                                                                                    },
                                                                                    updateLogo: async function(file){
                                                                                        try{
                                                                                            let data = await this.getBase64(file);
                                                                                            this.value = data;
                                                                                        }catch(err){
                                                                                            console.log(err);
                                                                                        }
                                                                                    },
                                                                                    changeLogo(uploader){
                                                                                        let uploaderFile = uploader.files[0].name.split('.');
                                                                                        let uploaderFileExt = uploaderFile[uploaderFile.length - 1];
                                                                                        if (uploader.files && uploader.files[0]) {
                                                                                            let logo = document.querySelector('#${formField.name}_img');
                                                                                            let previewLogo = document.querySelector('#previewLogo');
                                                                                            logo.removeAttribute('src');
                                                                                            logo.setAttribute('src', window.URL.createObjectURL(uploader.files[0]));
                                                                                            previewLogo.removeAttribute('src');
                                                                                            previewLogo.setAttribute('src', window.URL.createObjectURL(uploader.files[0]));
                                                                                        }
                                                                                        this.updateLogo(uploader.files[0]);
                                                                                    }
                                                                                }"
                                                            >
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                            ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <div class="h-[120px] bg-white border border-b-0 border-gray-300  w-full rounded-t-lg flex items-center justify-center">
                                                                <template x-if="value">
                                                                    <input name="${formField.name}" type="hidden" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                                </template>
                                                                <div>
                                                                <#if variant?? && variant == 'ezto verify'>
                                                                    <img id="${formField.name}_img" src="<#if formField.defaultValue??>${formField.defaultValue}<#else>${resourcesPath}/img/eztovrfy-logo-icon.png</#if>" class="h-[48px] w-auto max-w-full object-contain block"/>
                                                                    <#elseif variant?? && variant == 'ezto auth'>
                                                                    <img id="${formField.name}_img" src="<#if formField.defaultValue??>${formField.defaultValue}<#else>${resourcesPath}/img/eztoauth-logo-icon.png</#if>" class="h-[48px] w-auto max-w-full object-contain block"/>
                                                                </#if>

                                                                </div>
                                                            </div>

                                                            <input type="file" id="${formField.name}" class="hidden" accept="image/png, image/jpeg, image/jpg, image/svg+xml" @change="($el)=>{changeLogo($el.target)}"/>
                                                            <a class="bg-white border border-gray-300 text-sm rounded-b-lg flex items-center justify-center w-full p-2.5 cursor-pointer" id="qv_onboarding_${formField.name}" @click="($el)=>{openLogoSelector();}">${msg("change-logo-txt")}</a>
                                                            <#if formField.helpText??>
                                                                <#--  <p class="text-xs text-[#636c72] mt-1">${msg("upload-png-images-txt")}<span class="text-red-500">*</span></p>  -->
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}", variant)}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'mobile_number'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{
                                                                            error:'',
                                                                            phoneObj: null,
                                                                            validatePhone(value){
                                                                                if(this.phoneObj.isValidNumber()){
                                                                                    this.value = this.phoneObj.getNumber();
                                                                                    this.error = '';
                                                                                }else{
                                                                                    this.value = this.phoneObj.getNumber();
                                                                                    this.error = '${msg("enter-valid-phn-num-error")}'
                                                                                }
                                                                            },
                                                                            value:'',
                                                                        }">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <input id="${formField.name}" name="${formField.name}" type="tel" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5"  @input="function($el){validatePhone($el);}" maxlength="20" x-init="$nextTick(() => {
                                                                    const input = document.querySelector('#${formField.name}');
                                                                    phoneObj = window.intlTelInput(input, {
                                                                        initialCountry: '<#if formField.defaultValue??>${formField.defaultValue}<#else>in</#if>',
                                                                        utilsScript: '${resourcesPath}/js/tele-input/utils.js',
                                                                        separateDialCode: false
                                                                    });
                                                                    let flagValue = sessionStorage.getItem('flagName');
                                                                    if(!flagValue) {
                                                                        flagValue = '<#if formField.defaultValue??>${formField.defaultValue}<#else>in</#if>';
                                                                    }
                                                                    phoneObj.setCountry(flagValue);
                                                                })" autocomplete="off"/>
                                                            <input name="${formField.name}" type="hidden" id="${formField.name}_phone" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                            <span x-cloak x-show="error.length > 0" class="text-red-700 mt-3 text-sm <#if formField.required?? && formField.required?string == 'true'>required</#if>" x-text='error'></span>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'email'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{
                                                                                error:'',
                                                                                validateField(value){
                                                                                    if(!validateEmail(value)){
                                                                                        this.error='Enter a valid email!';
                                                                                    }else{
                                                                                        this.error=''
                                                                                    }
                                                                                },
                                                                                value: '<#if providerName?? && providerName ==  currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField?? && configField.defaultValue??>${configField.defaultValue}</#if>',
                                                                        }">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <input id="${formField.name}" type="hidden" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <input name="${formField.name}" id="qv_onboarding_${formField.name}" value="<#if providerName?? && providerName == config.name && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField?? && configField.defaultValue??>${configField.defaultValue}</#if>" type="email" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5" placeholder="${msg("${formField.label}")}" @input="($el)=>{value=$el.target.value}"/>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                            <span x-cloak x-show="error.length > 0" class="text-red-700 mt-3 text-sm <#if formField.required?? && formField.required?string == 'true'>required</#if>" x-text='error'></span>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'password'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{value: '<#if formField.defaultValue??>${formField.defaultValue}</#if>', show: false}">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <div class="relative">
                                                                <input id="${formField.name}" type="hidden" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                                <input :class="show || !value ? 'text':'key'" id="qv_onboarding_${formField.name}" name="${formField.name}" type="text" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} block w-full p-2.5 pr-8" placeholder="${msg("${formField.label}")}" @input="($el)=>{value=$el.target.value;}"/>
                                                                <span class="absolute right-2 top-1/2 transform -translate-y-1/2 h-5 w-5 z-10" x-cloak x-show>
                                                                    <img src="${resourcesPath}/img/eye-closed.png" @click="show = !show" x-cloak x-show="!show" class="w-5 h-5 cursor-pointer"/>
                                                                    <img src="${resourcesPath}/img/eye-open.png" @click="show = !show" x-cloak x-show="show" class="w-5 h-5 cursor-pointer"/>
                                                                </span>
                                                            </div>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'list'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{error:'', selected:'<#if formField.defaultValue??>${formField.defaultValue}</#if>'}" x-init="$watch('selected', updatedValue => (${formField.name}=updatedValue));$nextTick(() => {${formField.name}=selected;});">
                                                            <input id="${formField.name}" type="hidden" x-model="selected" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <select name="${formField.name}" id="qv_onboarding_${formField.name}" class="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5 custom-select" x-model="selected">
                                                                <option value=''>${msg("nothing-selected")}</option>
                                                                <#list formField.options as option>
                                                                    <option value="${option}">${option}</option>
                                                                </#list>
                                                            </select>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'boolean'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{selected: <#if formField.defaultValue??>${formField.defaultValue}<#else>false</#if>}" x-init="
                                                            function(){
                                                                if(selected){
                                                                    let checkElement = document.getElementById('${formField.name}_toggle');
                                                                    if(checkElement){
                                                                        checkElement.checked = true;
                                                                        ${formField.name} = 'true';
                                                                    }
                                                                }else{
                                                                    ${formField.name} = 'false';
                                                                }
                                                            }
                                                        ">
                                                            <input id="${formField.name}" name="${formField.name}" type="hidden" x-model='selected' class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <div class="inline-flex relative items-center space-x-3">
                                                                <span class="text-sm font-medium">${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if></span>
                                                                <label for="${formField.name}_toggle" class="cursor-pointer">
                                                                    <input type="checkbox" id="${formField.name}_toggle" class="sr-only peer" @click="selected=!selected;${formField.name} = String(selected);"/>
                                                                    <div id="qv_onboarding_${formField.name}_toggle" class="w-11 h-6 bg-gray-200 peer-focus:outline-none  rounded-full peer peer-checked:after:translate-x-0 peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px]  <#if locale != 'ar'> after:right-[2px] </#if> after:-translate-x-full after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all ${peerChecked_600}"></div>
                                                                </label>
                                                            </div>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                    <#elseif formField.type?lower_case == 'config'>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{error:'', selected:'<#if providerName??>${providerName}<#elseif submittedData??><#list submittedData as checkKey,checkVal><#if checkKey == formField.name>${checkVal}</#if></#list></#if>',lastSelected: '<#if providerName??>${providerName}<#elseif submittedData??><#list submittedData as checkKey,checkVal><#if checkKey == formField.name>${checkVal}</#if></#list></#if>',openModal: false}" data-buttonEnable="<#if providerName??>true<#else>false</#if>">
                                                            <input id="${formField.name}" name="${formField.name}" type="hidden" x-model="selected" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <div class="grid grid-cols-3 gap-5 md:grid-cols-2 sm:grid-cols-1">
                                                                <#list formField.configProperties as configProperty>
                                                                    <div id="qv_onboarding_${configProperty.name}" class="block p-6 max-w-sm sm:max-w-full bg-white rounded-lg border border-gray-200 shadow-md hover:bg-white/5 cursor-pointer relative" value="${configProperty.name}" @click="selected='${configProperty.name}';openModal=true;">
                                                                        <div
                                                                            class="w-6 h-6 bg-green-600 rounded-full flex items-center justify-center absolute left-3 top-3" x-cloak x-show="lastSelected == '${configProperty.name}'"
                                                                        >
                                                                            <img src="${resourcesPath}/img/tick.png" alt="" class="w-5 h-5" />
                                                                        </div>
                                                                        <#if configProperty.logo??>
                                                                            <img src="${resourcesPath}/img/${configProperty.logo}" alt="provider logo" class="w-10 h-10 mx-auto object-contain"/>
                                                                            <#else>
                                                                                <img src="${resourcesPath}/img/logo-icon.png" alt="provider logo" class="w-10 h-10 mx-auto object-contain"/>
                                                                        </#if>
                                                                        <h5 class="text-lg sm:text-sm text-center pt-4">${msg("${configProperty.label}")}</h5>
                                                                    </div>
                                                                </#list>
                                                            </div>
                                                            <#list formField.configProperties as config>
                                                                <div class="overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 md:bottom-auto z-50 w-full inset-0 h-modal h-full justify-center items-center flex bg-black/50" x-cloak x-show="selected == '${config.name}' && openModal"
                                                                    x-init="$watch('openModal', ()=> handleForm())"
                                                                    x-data="{
                                                                        <#list config.configProperties as configField>
                                                                            <#if configField.dependsOn??>
                                                                                <#list configField.dependsOn as dependency, value>
                                                                                    ${dependency}: false,
                                                                                </#list>
                                                                            </#if>
                                                                        </#list>
                                                                    }">
                                                                    <div class="relative p-4 w-full max-w-md md:h-full h-auto">
                                                                        <div class="relative bg-white rounded-lg shadow">
                                                                            <button id="qv_onboarding_close_modal" type="button" class="absolute top-3 right-2.5 text-gray-400 bg-transparent hover:bg-gray-200 hover:text-gray-900 rounded-lg text-sm p-1.5 ml-auto inline-flex items-center" @click="openModal=false;selected=lastSelected;">
                                                                                <svg aria-hidden="true" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path></svg>
                                                                                <span class="sr-only">${msg("close-modal")}</span>
                                                                            </button>
                                                                            <div class="py-9 px-3">
                                                                                <div class="conditional-containers max-h-[360px] overflow-scroll mb-4 px-3 scrollbar-hide" x-cloak x-show="selected == '${config.name}'">
                                                                                    <#list config.configProperties as configField>
                                                                                        <#if configField.type?lower_case == 'mobile_number'>
                                                                                            <div :class="<#if configField.dependsOn??><#list configField.dependsOn as dependency, value>${dependency} === true ? 'block':'hidden'</#list><#else>'block'</#if>" data-mobileNumber="true"  class="mb-6" x-data="{
                                                                                                                error:'',
                                                                                                                phoneObj: null,
                                                                                                                validatePhone(value){
                                                                                                                    if(this.phoneObj.isValidNumber()){
                                                                                                                        this.value = this.phoneObj.getNumber();
                                                                                                                        this.error = '';
                                                                                                                    }else{
                                                                                                                        this.value = this.phoneObj.getNumber();
                                                                                                                        this.error = '${msg("enter-valid-phn-num-error")}'
                                                                                                                    }
                                                                                                                },
                                                                                                                value:'',
                                                                                                            }">
                                                                                                <label for="${configField.name}" class="block mb-2 text-sm font-medium">
                                                                                                    ${msg("${configField.label}")} <#if configField.required?? && configField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                                                                </label>
                                                                                                <input id="${config.name}_${configField.name}" oninput="this.value = this.value.replace(/[^0-9]/g, '')" name="${config.name}_${configField.name}" type="tel" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5"  @input="function($el){validatePhone($el);}" maxlength="20" x-init="$nextTick(() => {
                                                                                                        const input = document.querySelector('#${config.name}_${configField.name}');
                                                                                                        phoneObj = window.intlTelInput(input, {
                                                                                                            initialCountry: '<#if configField.defaultValue?? && configField.defaultValue?has_content>${configField.defaultValue}<#else>in</#if>',
                                                                                                            utilsScript: '${resourcesPath}/js/tele-input/utils.js',
                                                                                                            separateDialCode: false
                                                                                                        });
                                                                                                        let flagValue = sessionStorage.getItem('flagName');
                                                                                                        if(!flagValue) {
                                                                                                            flagValue = '<#if configField.defaultValue?? && configField.defaultValue?has_content>${configField.defaultValue}<#else>in</#if>';
                                                                                                        }
                                                                                                        phoneObj.setCountry(flagValue);
                                                                                                    })" >
                                                                                                <input name="${configField.name}" type="hidden" id="${config.name}_${configField.name}_phone" x-model="value" :class="
                                                                                                    <#if configField.dependsOn??>
                                                                                                        <#list configField.dependsOn as dependency, value>
                                                                                                            ${dependency} === true && selected == '${config.name}' ? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                        </#list>
                                                                                                        <#else>
                                                                                                            selected == '${config.name}'? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                    </#if>
                                                                                                "/>
                                                                                                <#if configField.helpText??>
                                                                                                    <p class="text-xs text-[#636c72] mt-1">${msg("${configField.helpText}")}</p>
                                                                                                </#if>
                                                                                                <span :class="
                                                                                                    <#if configField.dependsOn??>
                                                                                                        <#list configField.dependsOn as dependency, value>
                                                                                                            ${dependency} === true && selected == '${config.name}' ? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                        </#list>
                                                                                                        <#else>
                                                                                                            selected == '${config.name}'? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                    </#if>
                                                                                                " x-cloak x-show="error.length > 0" class="text-red-700 mt-3 text-sm" x-text='error'></span>
                                                                                            </div>
                                                                                        <#elseif configField.type?lower_case == 'multiselect' || configField.type?lower_case == 'multivaluedlist'>
                                                                                            <div :class="<#if configField.dependsOn??><#list configField.dependsOn as dependency, value>${dependency} === true ? 'block':'hidden'</#list><#else>'block'</#if>" class="mb-6">
                                                                                                <label for="${configField.name}" class="block mb-2 text-sm font-medium">
                                                                                                    ${msg("${configField.label}")} <#if configField.required?? && configField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                                                                </label>
                                                                                                <#if providerName?? && providerName == config.name  && currentConfig?? && currentConfig['${configField.name}']??>
                                                                                                    <#assign selectedDefaultValue = currentConfig['${configField.name}']>
                                                                                                <#elseif configField.defaultValue??>
                                                                                                    <#assign selectedDefaultValue = configField.defaultValue>
                                                                                                </#if>
                                                                                                <select class="hidden" id="${config.name}_${configField.name}">
                                                                                                    <#list configField.options as option>
                                                                                                        <option value="${option}" <#if selectedDefaultValue?? && selectedDefaultValue?contains(option)>selected='true'</#if> >${option}</option>
                                                                                                    </#list>
                                                                                                </select>
                                                                                                <div x-data="dropdown('${config.name}_${configField.name}')" x-init="loadOptions()" class="w-full md:w-1/2 flex flex-col items-center max-h-64 mx-auto">
                                                                                                    <input name="${configField.name}" type="hidden" x-bind:value="selectedValues()"/>
                                                                                                    <div class="inline-block relative w-full">
                                                                                                        <div class="flex flex-col items-center relative">
                                                                                                            <div x-on:click="open" class="w-full  svelte-1l8159u">
                                                                                                                <div class="my-2 p-1 flex border border-gray-200 bg-white rounded svelte-1l8159u">
                                                                                                                    <div class="flex flex-auto flex-wrap">
                                                                                                                        <template x-for="(option,index) in selected" :key="options[option].value">
                                                                                                                            <div
                                                                                                                                class="flex justify-center items-center m-1 font-medium py-1 px-2 bg-blue-500/10 rounded-full text-blue-600 border border-primary">
                                                                                                                                <div class="text-xs font-normal leading-none max-w-full flex-initial" x-model="options[option]" x-text="options[option].text"></div>
                                                                                                                                <div class="flex flex-auto flex-row-reverse">
                                                                                                                                    <div x-on:click="remove(index,option)">
                                                                                                                                        <svg class="fill-current h-6 w-6 " role="button" viewBox="0 0 20 20">
                                                                                                                                            <path d="M14.348,14.849c-0.469,0.469-1.229,0.469-1.697,0L10,11.819l-2.651,3.029c-0.469,0.469-1.229,0.469-1.697,0
                                                                                                                                    c-0.469-0.469-0.469-1.229,0-1.697l2.758-3.15L5.651,6.849c-0.469-0.469-0.469-1.228,0-1.697s1.228-0.469,1.697,0L10,8.183
                                                                                                                                    l2.651-3.031c0.469-0.469,1.228-0.469,1.697,0s0.469,1.229,0,1.697l-2.758,3.152l2.758,3.15
                                                                                                                                    C14.817,13.62,14.817,14.38,14.348,14.849z" />
                                                                                                                                        </svg>

                                                                                                                                    </div>
                                                                                                                                </div>
                                                                                                                            </div>
                                                                                                                        </template>
                                                                                                                        <div x-show="selected.length == 0" class="flex-1">
                                                                                                                            <input placeholder=${msg("select-option-txt")} id="qv_onboarding_${configField.name}"
                                                                                                                                class="bg-transparent p-1 px-2 appearance-none outline-none h-full w-full text-gray-800"
                                                                                                                                x-bind:value="selectedValues()"
                                                                                                                            />
                                                                                                                        </div>
                                                                                                                    </div>
                                                                                                                    <div
                                                                                                                        class="text-gray-300 w-8 py-1 pl-2 pr-1 border-l flex items-center border-gray-200 svelte-1l8159u">

                                                                                                                        <button id="qv_onboarding_openBtn" type="button" x-show="isOpen() === true" x-on:click="open"
                                                                                                                            class="cursor-pointer w-6 h-6 text-gray-600 outline-none focus:outline-none">
                                                                                                                            <svg version="1.1" class="fill-current h-4 w-4" viewBox="0 0 20 20">
                                                                                                                                <path d="M17.418,6.109c0.272-0.268,0.709-0.268,0.979,0s0.271,0.701,0,0.969l-7.908,7.83
                                                                                                c-0.27,0.268-0.707,0.268-0.979,0l-7.908-7.83c-0.27-0.268-0.27-0.701,0-0.969c0.271-0.268,0.709-0.268,0.979,0L10,13.25
                                                                                                L17.418,6.109z" />
                                                                                                                            </svg>

                                                                                                                        </button>
                                                                                                                        <button id="qv_onboarding_closeBtn" type="button" x-show="isOpen() === false" @click="close"
                                                                                                                            class="cursor-pointer w-6 h-6 text-gray-600 outline-none focus:outline-none">
                                                                                                                            <svg class="fill-current h-4 w-4" viewBox="0 0 20 20">
                                                                                                                                <path d="M2.582,13.891c-0.272,0.268-0.709,0.268-0.979,0s-0.271-0.701,0-0.969l7.908-7.83
                                                                                                c0.27-0.268,0.707-0.268,0.979,0l7.908,7.83c0.27,0.268,0.27,0.701,0,0.969c-0.271,0.268-0.709,0.268-0.978,0L10,6.75L2.582,13.891z
                                                                                                " />
                                                                                                                            </svg>

                                                                                                                        </button>
                                                                                                                    </div>
                                                                                                                </div>
                                                                                                            </div>
                                                                                                            <div class="w-full px-4">
                                                                                                                <div x-show.transition.origin.top="isOpen()"
                                                                                                                    class="absolute shadow-lg bg-white z-40 w-full left-0 rounded max-h-select overflow-y-auto bottom-full"
                                                                                                                    x-on:click.away="close">
                                                                                                                    <div class="flex flex-col w-full max-h-[200px]">
                                                                                                                        <template x-for="(option, index) in options" :key="index">
                                                                                                                            <div x-bind:class="option.selected ? 'hidden' : 'border-transparent'">
                                                                                                                                <div class="cursor-pointer w-full border-gray-100 rounded-t border-b ${hover_100}"
                                                                                                                                    @click="select(index,$event)">
                                                                                                                                    <div x-bind:class="option.selected ? 'border-blue-600' : 'border-transparent'"
                                                                                                                                        class="flex w-full items-center p-2 pl-2 border-l-2 relative">
                                                                                                                                        <div class="w-full items-center flex">
                                                                                                                                            <div class="mx-2 leading-6" x-model="option" x-text="option.text"></div>
                                                                                                                                        </div>
                                                                                                                                    </div>
                                                                                                                                </div>
                                                                                                                            </div>
                                                                                                                        </template>
                                                                                                                    </div>
                                                                                                                </div>
                                                                                                            </div>
                                                                                                        </div>
                                                                                                    </div>
                                                                                                </div>
                                                                                                <#if configField.helpText??>
                                                                                                    <p class="text-xs text-[#636c72] mt-1">${msg("${configField.helpText}")}</p>
                                                                                                </#if>
                                                                                            </div>
                                                                                        <#elseif configField.type?lower_case == 'boolean'>
                                                                                            <div :class="<#if configField.dependsOn??><#list configField.dependsOn as dependency, value>${dependency} === true ? 'block':'hidden'</#list><#else>'block'</#if>" class="mb-6" x-data="{choosed: <#if providerName?? && providerName == config.name  && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField.defaultValue??>${configField.defaultValue}<#else>false</#if>}" x-init="
                                                                                                function(){
                                                                                                    if(choosed){
                                                                                                        let checkElement = document.getElementById('${config.name}_${configField.name}_toggle');
                                                                                                        if(checkElement){
                                                                                                            checkElement.checked = true;
                                                                                                            ${configField.name} = true;
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                           ">
                                                                                                <input id="${configField.name}" name="${configField.name}" type="hidden" x-model='choosed' :class="
                                                                                                    <#if configField.dependsOn??>
                                                                                                        <#list configField.dependsOn as dependency, value>
                                                                                                            ${dependency} === true && selected == '${config.name}' ? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                        </#list>
                                                                                                        <#else>
                                                                                                            selected == '${config.name}'? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                    </#if>
                                                                                                "/>
                                                                                                <div class="inline-flex relative items-center space-x-3">
                                                                                                    <span class="text-sm font-medium">${msg("${configField.label}")} <#if configField.required?? && configField.required?string == 'true'><span class="text-red-500">*</span></#if></span>
                                                                                                    <label for="${config.name}_${configField.name}_toggle" class="cursor-pointer">
                                                                                                        <input type="checkbox" id="${config.name}_${configField.name}_toggle" class="sr-only peer" @click="
                                                                                                            choosed=!choosed;
                                                                                                            if(typeof ${configField.name}  === 'boolean'){
                                                                                                                ${configField.name}=choosed;
                                                                                                            }
                                                                                                        "/>
                                                                                                        <div id="qv_onboarding_${config.name}_${configField.name}_toggle" class="w-11 h-6 bg-gray-200 peer-focus:outline-none  rounded-full peer peer-checked:after:translate-x-0 peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px]<#if locale != 'ar'> after:right-[2px] </#if> after:-translate-x-full after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all ${peerChecked_600}"></div>
                                                                                                    </label>
                                                                                                </div>
                                                                                                <#if configField.helpText??>
                                                                                                    <p class="text-xs text-[#636c72] mt-1">${msg("${configField.helpText}")}</p>
                                                                                                </#if>
                                                                                            </div>
                                                                                        <#elseif configField.type?lower_case == 'email'>
                                                                                            <div :class="<#if configField.dependsOn??><#list configField.dependsOn as dependency, value>${dependency} === true ? 'block':'hidden'</#list><#else>'block'</#if>" class="mb-6" x-data="{
                                                                                                                    error:'',
                                                                                                                    validateField(value){
                                                                                                                        if(!validateEmail(value)){
                                                                                                                            this.error='Enter a valid email!';
                                                                                                                        }else{
                                                                                                                            this.error='';
                                                                                                                        }
                                                                                                                    },
                                                                                                                    value:'<#if providerName?? && providerName == config.name  && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField?? && configField.defaultValue??>${configField.defaultValue}</#if>',
                                                                                                            }">
                                                                                                <label for="${configField.name}" class="block mb-2 text-sm font-medium">
                                                                                                    ${msg("${configField.label}")} <#if configField.required?? && configField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                                                                </label>
                                                                                                <input id="${config.name}_${configField.name}" type="hidden" x-model="value" :class="
                                                                                                    <#if configField.dependsOn??>
                                                                                                        <#list configField.dependsOn as dependency, value>
                                                                                                            ${dependency} === true && selected == '${config.name}' ? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                        </#list>
                                                                                                        <#else>
                                                                                                            selected == '${config.name}'? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                    </#if>
                                                                                                "/>
                                                                                                <input name="${configField.name}" id="qv_onboarding_${config.name}_${configField.name}" value="<#if providerName?? && providerName == config.name && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField?? && configField.defaultValue??>${configField.defaultValue}</#if>" type="email" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5" placeholder="${msg("${configField.label}")}" @input="($el)=>{value=$el.target.value;}"/>
                                                                                                <#if configField.helpText??>
                                                                                                    <p class="text-xs text-[#636c72] mt-1">${msg("${configField.helpText}")}</p>
                                                                                                </#if>
                                                                                                <span :class="
                                                                                                    <#if configField.dependsOn??>
                                                                                                        <#list configField.dependsOn as dependency, value>
                                                                                                            ${dependency} === true && selected == '${config.name}' ? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                        </#list>
                                                                                                        <#else>
                                                                                                            selected == '${config.name}'? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                    </#if>
                                                                                                " x-cloak x-show="error.length > 0" class="text-red-700 mt-3 text-sm" x-text='error'></span>
                                                                                            </div>
                                                                                        <#elseif configField.type?lower_case == 'password'>
                                                                                            <div :class="<#if configField.dependsOn??><#list configField.dependsOn as dependency, value>${dependency} === true ? 'block':'hidden'</#list><#else>'block'</#if>" class="mb-6" x-data="{value: '<#if providerName?? && providerName == config.name  && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField.defaultValue??>${configField.defaultValue}</#if>', show: false}"
                                                                                            >
                                                                                                <label for="${configField.name}" class="block mb-2 text-sm font-medium">
                                                                                                    ${msg("${configField.label}")} <#if configField.required?? && configField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                                                                </label>
                                                                                                <div class="relative">
                                                                                                    <input id="${config.name}_${configField.name}" type="hidden" x-model="value" :class="
                                                                                                    <#if configField.dependsOn??>
                                                                                                        <#list configField.dependsOn as dependency, value>
                                                                                                            ${dependency} === true && selected == '${config.name}' ? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                        </#list>
                                                                                                        <#else>
                                                                                                            selected == '${config.name}'? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                    </#if>
                                                                                                "/>
                                                                                                    <input :class="show || !value ? 'text':'key'" id="qv_onboarding_${config.name}_${configField.name}" value="<#if providerName?? && providerName == config.name  && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField.defaultValue??>${configField.defaultValue}</#if>" name="${configField.name}" type="text" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5 pr-8" placeholder="${msg("${configField.label}")}" @input="($el)=>{value=$el.target.value;}"/>
                                                                                                    <span class="absolute right-2 top-1/2 transform -translate-y-1/2 h-5 w-5 z-10" x-cloak x-show="value.length > 0">
                                                                                                        <img src="${resourcesPath}/img/eye-closed.png" @click="show = !show" x-cloak x-show="!show" class="w-5 h-5 cursor-pointer"/>
                                                                                                        <img src="${resourcesPath}/img/eye-open.png" @click="show = !show" x-cloak x-show="show" class="w-5 h-5 cursor-pointer"/>
                                                                                                    </span>
                                                                                                </div>
                                                                                                <#if configField.helpText??>
                                                                                                    <p class="text-xs text-[#636c72] mt-1">${msg("${configField.helpText}")}</p>
                                                                                                </#if>
                                                                                            </div>
                                                                                        <#elseif configField.type?lower_case == 'list'>
                                                                                            <div :class="<#if configField.dependsOn??><#list configField.dependsOn as dependency, value>${dependency} === true ? 'block':'hidden'</#list><#else>'block'</#if>" class="mb-6" x-data="{error:'', choosed:'<#if providerName?? && providerName == config.name && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField.defaultValue??>${configField.defaultValue}</#if>'}">
                                                                                                <input id="${config.name}_${configField.name}" type="hidden" x-model="choosed" :class="
                                                                                                    <#if configField.dependsOn??>
                                                                                                        <#list configField.dependsOn as dependency, value>
                                                                                                            ${dependency} === true && selected == '${config.name}' ? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                        </#list>
                                                                                                        <#else>
                                                                                                            selected == '${config.name}'? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                    </#if>
                                                                                                "/>
                                                                                                <label for="${configField.name}" class="block mb-2 text-sm font-medium">
                                                                                                    ${msg("${configField.label}")} <#if configField.required?? && configField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                                                                </label>
                                                                                                <select name="${configField.name}" id="qv_onboarding_${config.name}_${configField.name}" class="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5 custom-select" x-model="choosed">
                                                                                                    <option value=''>${msg("nothing-selected")}</option>
                                                                                                    <#list configField.options as option>
                                                                                                        <option value="${option}" @click="choosed=${option}">${option}</option>
                                                                                                    </#list>
                                                                                                </select>
                                                                                                <#if configField.helpText??>
                                                                                                    <p class="text-xs text-[#636c72] mt-1">${msg("${configField.helpText}")}</p>
                                                                                                </#if>
                                                                                            </div>
                                                                                        <#elseif configField.type?lower_case == 'notification_warn'>

                                                                                        <#else>
                                                                                            <div :class="<#if configField.dependsOn??><#list configField.dependsOn as dependency, value>${dependency} === true ? 'block':'hidden'</#list><#else>'block'</#if>" class="mb-6" x-data="{value: '<#if providerName?? && providerName == config.name  && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField.defaultValue??>${configField.defaultValue}</#if>'}">
                                                                                                <label for="${configField.name}" class="block mb-2 text-sm font-medium">
                                                                                                    ${msg("${configField.label}")} <#if configField.required?? && configField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                                                                </label>
                                                                                                <input id='${config.name}_${configField.name}' type="hidden" x-model="value" :class="
                                                                                                    <#if configField.dependsOn??>
                                                                                                        <#list configField.dependsOn as dependency, value>
                                                                                                            ${dependency} === true && selected == '${config.name}' ? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                        </#list>
                                                                                                        <#else>
                                                                                                            selected == '${config.name}'? <#if configField.required?? && configField.required?string == 'true'>'required'<#else>'dummyFiller'</#if>:'duymmyFiller'
                                                                                                    </#if>
                                                                                                "/>
                                                                                                <input name="${configField.name}" id='qv_onboarding_${config.name}_${configField.name}' value="<#if providerName?? && providerName == config.name  && currentConfig?? && currentConfig['${configField.name}']??>${currentConfig['${configField.name}']}<#elseif configField.defaultValue??>${configField.defaultValue}</#if>" type="text" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5" placeholder="${msg("${configField.label}")}" @input="($el)=>{value=$el.target.value}"/>
                                                                                                <#if configField.helpText??>
                                                                                                    <p class="text-xs text-[#636c72] mt-1">${msg("${configField.helpText}")}</p>
                                                                                                </#if>
                                                                                            </div>
                                                                                        </#if>
                                                                                    </#list>
                                                                                </div>
                                                                                <div class="flex justify-between items-center space-x-2 rounded-b">
                                                                                    <button type="button" id="cancelBtn" class="text-gray-500 bg-white hover:bg-white/5 focus:ring-4 focus:outline-none focus:ring-gray-200 rounded-lg border border-gray-200 text-sm font-medium px-5 py-2.5 hover:text-gray-900 focus:z-10" @click="openModal=false;selected=lastSelected;">${msg("cancel-txt")}</button>
                                                                                    <template x-if="selected == '${config.name}'">
                                                                                        <button id="saveButton" type="button" class="text-white bg-primary ${hover_500} focus:outline-none font-medium rounded-lg text-sm px-5 py-2.5 text-center disabled:pointer-events-none disabled:opacity-30" disabled='true' @click="openModal=false;lastSelected=selected;">${msg("save-txt")}</button>
                                                                                    </template>
                                                                                </div>
                                                                            </div>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            </#list>
                                                        </div>
                                                    <#else>
                                                        <div :class="<#if formField.dependsOn??><#list formField.dependsOn as dependency, value>${dependency} == <#if value?is_string>'${value}'<#else>'${value?c}'</#if> && </#list> 'true' ? 'block':'hidden'<#else>'block'</#if>" class="mb-6" x-data="{value: '<#if formField.defaultValue??>${formField.defaultValue}</#if>'}">
                                                            <label for="${formField.name}" class="block mb-2 text-sm font-medium">
                                                                ${msg("${formField.label}")} <#if formField.required?? && formField.required?string == 'true'><span class="text-red-500">*</span></#if>
                                                            </label>
                                                            <input id='${formField.name}' type="hidden" x-model="value" class="<#if formField.required?? && formField.required?string == 'true'>required</#if>"/>
                                                            <input name="${formField.name}" id='qv_onboarding_${formField.name}' type="text" value="<#if formField.defaultValue??>${formField.defaultValue}</#if>" class="bg-white border border-gray-300 text-sm rounded-lg ${focus_500} focus:border-primary block w-full p-2.5" placeholder="${msg("${formField.label}")}" @input="($el)=>{value=$el.target.value}"/>
                                                            <#if formField.helpText??>
                                                                <p class="text-xs text-[#636c72] mt-1">${msg("${formField.helpText}")}</p>
                                                            </#if>
                                                        </div>
                                                </#if>
                                            </#list>
                                            <#if showMultiLanSelect>
                                                <div class="mb-6 lang-selector">
                                                    <label class="block mb-2 text-sm font-medium">
                                                        ${msg("available-language")}
                                                    </label>
                                                    <input name="available-lang" type="hidden" id="availabl-lang-input"/>
                                                    <select id="available-lang" class="bg-white border border-gray-300 text-sm rounded-lg focus:border-primary block w-full p-2.5" data-minimum-results-for-search="Infinity">
                                                    </select>
                                                </div>
                                            </#if>
                                            <#if !showMultiLanSelect>
                                                <input name="available-lang" type="hidden" id="availabl-lang-input" value="en"/>
                                            </#if>
                                            <#if currentStep.title == 'workspace-setup'>
                                                <div class="mb-6">
                                                    <label class="block mb-2 text-sm font-medium">
                                                        ${msg("preferred-categories")}
                                                    </label>
                                                    <input name="recommendedMarketplaceCategories" type="hidden" id="recommendedMarketplaceCategories-input"/>
                                                    <select id="recommendedMarketplaceCategories-select" class="bg-white border border-gray-300 text-sm rounded-lg focus:border-primary block w-full p-2.5" data-minimum-results-for-search="Infinity" value="">
                                                    </select>
                                                </div>
                                            </#if>

                                        </div>
                                        </#if>

                                        <#if errorString??>
                                                <div x-cloak x-show="showWrapper" class="overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 md:bottom-auto z-50 w-full inset-0 h-modal h-full justify-start items-center flex pt-32 bg-black/50 flex-col" x-data="{showWrapper: true }">
                                                    <div x-cloak x-show="showError" x-data="{showError: true}" class="flex items-center p-4 mb-4 w-full max-w-lg text-gray-500 bg-white rounded-lg shadow" role="alert">
                                                        <div class="inline-flex flex-shrink-0 justify-center items-center w-8 h-8 text-red-500 bg-red-100 rounded-full">
                                                            <svg aria-hidden="true" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path></svg>
                                                            <span class="sr-only">${msg("error-icon")}</span>
                                                        </div>
                                                        <div class="ml-3 text-sm font-normal break-all">${msg("${errorString}")}</div>
                                                        <button type="button" id="closeBtn" class="ml-auto -mx-1.5 -my-1.5 bg-white text-gray-400 hover:text-gray-900 rounded-lg focus:ring-2 focus:ring-gray-300 p-1.5 hover:bg-gray-100 inline-flex h-8 w-8" aria-label="Close" @click="showError = false;showWrapper = false;">
                                                            <span class="sr-only">${msg("close")}</span>
                                                            <svg aria-hidden="true" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path></svg>
                                                        </button>
                                                    </div>
                                                </div>
                                                <#elseif errorMessage??>
                                                    <div x-cloak x-show="showWrapper" class="overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 md:bottom-auto z-50 w-full inset-0 h-modal h-full justify-start items-center flex pt-32 bg-black/50 flex-col" x-data="{showWrapper: true }">
                                                        <div x-cloak x-show="showError" x-data="{showError: true}" class="flex p-4 mb-4 w-full max-w-lg text-gray-500 bg-white rounded-lg shadow" role="alert">
                                                            <div class="inline-flex flex-shrink-0 justify-center items-center w-8 h-8 text-red-500 bg-red-100 rounded-full">
                                                                <svg aria-hidden="true" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path></svg>
                                                                <span class="sr-only">${msg("error-icon")}</span>
                                                            </div>
                                                            <div class="ml-3 text-sm font-normal break-all">
                                                                <ul class="space-y-1 list-disc list-inside">
                                                                    <#list errorMessage.getErrorMessage()?split(",") as error>
                                                                        <li class="break-all">${error}</li>
                                                                    </#list>
                                                                </ul>
                                                            </div>
                                                            <button type="button" id="closeBtn" class="ml-auto -mx-1.5 -my-1.5 bg-white text-gray-400 hover:text-gray-900 rounded-lg focus:ring-2 focus:ring-gray-300 p-1.5 hover:bg-gray-100 inline-flex h-8 w-8" aria-label="Close" @click="showError = false;showWrapper=false;">
                                                                <span class="sr-only">${msg("close")}</span>
                                                                <svg aria-hidden="true" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path></svg>
                                                            </button>
                                                        </div>
                                                    </div>
                                        </#if>
                                    </div>
                                    <div
                                        class="w-4/5 flex items-center space-x-2 <#if prevIndex??>justify-between<#else>justify-end</#if> mx-auto xl:w-full xl:overflow-auto p-2"
                                    >
                                        <#if prevIndex??>
                                            <button
                                            id="backBtn"
                                            type="button"
                                            class="text-gray-500 bg-white hover:bg-white/5 focus:ring-4 focus:outline-none focus:ring-blue-300 rounded-lg border border-gray-200 text-sm font-medium px-5 py-2.5 hover:text-gray-900 focus:z-10 <#if prevIndex??><#else>hidden</#if>"
                                            @click="($el)=>{
                                                $el.preventDefault();
                                                isSwitch='true';
                                                switchStep='${prevIndex}';
                                                disableAllButtons();
                                                setTimeout(function(){
                                                    handleSubmit((welcomeForm = $refs.welcomeForm)=>welcomeForm.submit())
                                                },100)
                                            }">
                                                ${msg("back-txt")}
                                            </button>
                                        </#if>
                                        <div class="inline-block">
                                            <#if currentStep?? && currentStep.skippable>
                                                <button
                                                id="skipBtn"
                                                class="hover:text-white border border-primary ${hover_500} text-primary focus:outline-none font-medium rounded-lg text-sm px-5 py-2.5 text-center disabled:pointer-events-none disabled:opacity-30 mr-3"
                                                @click="($el)=>{
                                                    $el.preventDefault();
                                                    isSwitch='true';
                                                    switchStep='${currentIndex + 1}';
                                                    disableAllButtons();
                                                    setTimeout(function(){
                                                        handleSubmit((welcomeForm = $refs.welcomeForm)=>welcomeForm.submit())
                                                    },100)}
                                                ">
                                                    ${msg("skip-step-txt")}
                                                </button>
                                            </#if>
                                            <button
                                            id="submitBtn"
                                            class="text-white bg-primary ${hover_500} focus:outline-none font-medium rounded-lg text-sm px-5 py-2.5 text-center disabled:pointer-events-none disabled:opacity-30"
                                            type="button"
                                            @click="($el)=>{
                                                $el.preventDefault();
                                                disableAllButtons();
                                                <#if showMultiLanSelect>
                                                     $('#availabl-lang-input').val($('#available-lang').val().join(','));
                                                </#if>
                                                <#if currentStep.title == 'workspace-setup'>
                                                    $('#recommendedMarketplaceCategories-input').val(($('#recommendedMarketplaceCategories-select').val() || []).join(','));
                                                </#if>
                                                setTimeout(function(){
                                                        handleSubmit((welcomeForm = $refs.welcomeForm)=>welcomeForm.submit())
                                                },100);
                                            }"
                                            disabled="true"
                                            >
                                                <#if totalSteps-1 == currentIndex>
                                                    ${msg("done-txt")}
                                                    <#else>
                                                        ${msg("next-step")}
                                                </#if>
                                            </button>
                                        </div>
                                    </div>
                                </section>
                            </div>
                        </div>
                    </section>
                    <#if showPreview>
                        <section class="col-span-5 bg-[#f2f4f8]">
                        <div class="h-[90%] rounded-tl-[20px] rounded-bl-[20px] bg-[#797979] mt-12 pt-12 pl-12">
                            <div class="h-[90%] rounded-tl-[20px] rounded-bl-[20px] bg-white overflow-hidden mt-4">
                                <div class="flex p-2 shadow-lg">
                                    <#if variant?? && variant == 'ezto verify'>
                                        <img class="w-10 mr-3" src="${resourcesPath}/img/eztovrfy-logo-icon.png" alt="ezto verify" id="previewLogo"/>
                                    <#else>
                                        <img class="w-10 mr-3" src="${resourcesPath}/img/eztoauth-logo-icon.png" alt="ezto auth" id="previewLogo"/>
                                    </#if>
                                    <div class="border-l px-3">
                                        <h3 class="text-primary font-bold text-[0.8rem]" id="workspaceName"></h3>
                                        <span class="text-[0.7rem]">${msg("workspace")}</span>
                                    </div>
                                </div>
                                <div class="flex h-[80%]">
                                    <div class="w-4/12 bg-white">
                                        <ul>
                                            <li class="bg-primary p-3 text-white flex gap-2">
                                                <img src="${resourcesPath}/img/gs-menu.svg">
                                                <span>${msg("getting-started")}</span>
                                            </li>
                                            <li class="p-3 border-b flex gap-2">
                                                <img src="${resourcesPath}/img/activity-menu<#if mode?? && mode =='bo'>-bo</#if>.svg">
                                                <span>${msg("activity")}</span>
                                            </li>
                                            <#assign x=8>
                                            <#list 1..x as i>
                                                <li class="p-3 border-b">
                                                    <div class="flex gap-3">
                                                        <div class="bg-[#EDEDED] w-5 h-5 rounded-sm"></div>
                                                        <div class="bg-[#EDEDED] w-9/12 h-5 rounded-sm"></div>
                                                    </div>
                                                </li>
                                            </#list>
                                        </ul>
                                    </div>
                                    <div class="bg-[#f2f4f8] w-8/12 h-full p-4">
                                        <h1 class="text-xl border-b py-1 relative after:content-[' '] after:block after:absolute after:w-8 after:top-9 after:h-1 after-bg-primary">
                                            ${msg('getting-started')}
                                        </h1>
                                        <div class="bg-white m-1 mt-4 p-3 rounded shadow flex flex-col gap-2 pb-8">
                                            <h3 class="text-primary">
                                                ${msg('workspace-details')}
                                            </h3>
                                            <p class="font-semibold"  id="workspaceNameBold"></p>
                                            <p id="workspaceUrl" class="<#if locale == 'ar'>text-direction-ltr</#if>">${baseUri}</p>
                                        </div>
                                        <#assign x=3>
                                        <#list 1..x as i>
                                            <div class="bg-white m-1 mt-2 p-3 rounded shadow flex gap-4">
                                                <div class="bg-[#EDEDED] w-8 h-8 rounded"></div>
                                                <div class="flex flex-col gap-2">
                                                    <div class="bg-[#EDEDED] w-28 h-4 rounded-sm"></div>
                                                    <div class="bg-[#EDEDED] w-48 h-3 rounded-sm"></div>
                                                    <div class="bg-[#EDEDED] w-20 h-2 rounded-sm"></div>
                                                </div>
                                            </div>
                                        </#list>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>
                    </#if>

                </form>
            <#else>
                <p>${msg("no-steps-found-txt")}</p>
        </#if>
        <#else>
            <div class="min-h-screen w-full bg-[#f2f4f8]">
                    <#if stateChecker??>
                        <input id="stateChecker" name="stateChecker" type="hidden" value="${stateChecker}" />
                    </#if>
                    <section class="min-h-full min-w-full relative pt-10 pl-10 sm:pt-5 sm:pl-5"
                    >
                        <div class="max-w-full xl:h-auto">
                            <div class="flex gap-2 items-center mb-10">
                            <#if variant?? && variant == 'ezto verify'>
                                <img class="w-[215px]" src="${resourcesPath}/img/eztovrfy-logo.png" alt="ezto verify"/>
                                <#elseif variant?? && variant == 'ezto auth'>
                                <img class="w-[215px]" src="${resourcesPath}/img/eztoauth-logo.png" alt="ezto verify"/>
                            </#if>
                            </div>
                        </div>
                    </section>
                    <section>
                        <div
                        class="overflow-y-auto w-auto md:inset-0 md:h-full"
                        >
                            <div class="relative p-10 sm:p-5 w-full max-w-5xl h-full mx-auto">
                                <div
                                class="relative bg-white rounded-lg shadow-modalShadow mt-14 max-w-[1024px] flex flex-col justify-between  xl:w-full xl:h-auto"
                                >
                                    <div class="w-[500px] mx-auto pt-16 overflow-y-scroll mb-8 pb-8 xl:w-full xl:px-8 scrollbar-hide">
                                        <div class="mb-10 text-center">
                                            <div class="flex justify-center mb-4">
                                                <img src="${resourcesPath}/img/error.png" alt="error" class="w-14 h-14 sm:w-14 sm:h-14" />
                                            </div>
                                            <h3 class="text-2xl font-semibold tracking-tight mb-4 sm:text-xl">
                                                ${msg("oops-something-went-wrong-error")}
                                            </h3>
                                            <p class="text-base sm:text-sm">
                                                ${msg("dont-have-access-to-create-admin-user-error")}
                                            </p>
                                            <p class="text-base sm:text-sm">
                                                ${msg("pls-contact-support-team-error")}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>
            </div>
    </#if>
    <script>
    if ( window.history.replaceState ) {
        window.history.replaceState( null, null, window.location.href );
    }

        // Prevent enter actions for required input
        document.addEventListener('keydown', (e) => {
            if (e.key === "Enter" || e.which == '13') {
                e.preventDefault();
            }
        });

        function disableAllButtons(){
            let buttonIdsToDisable = ['backBtn','skipBtn','submitBtn'];
            buttonIdsToDisable.forEach(buttonId=>{
                let currentButton = document.querySelector('#'+buttonId);
                if(currentButton){
                    currentButton.disabled = true;
                }
            })
        }

        // validations
        function dropdown(id) {
            return {
                options: [],
                selected: [],
                show: false,
                open() { this.show = true },
                close() { this.show = false },
                isOpen() { return this.show === true },
                select(index, event) {
                    if (!this.options[index].selected) {
                        this.options[index].selected = true;
                        this.selected.push(index);
                    } else {
                        this.selected.splice(this.selected.lastIndexOf(index), 1);
                        this.options[index].selected = false
                    }
                },
                remove(index, option) {
                    this.options[option].selected = false;
                    this.selected.splice(index, 1);
                },
                loadOptions() {
                    const options = document.getElementById(id).options;
                    for (let i = 0; i < options.length; i++) {
                        this.options.push({
                            value: options[i].value,
                            text: options[i].innerText,
                            selected: options[i].getAttribute('selected') != null ? true : false
                        });
                        if(this.options[i].selected){
                            this.selected.push(i);
                        }
                    }
                },
                selectedValues(){
                    return this.selected.map((option)=>{
                        return this.options[option].value;
                    })
                }
            }
        }

        class TimeUnit{
            constructor(time){
                let unit = 'Minutes';

                if (time) {
                    if (time == -1) {
                        time = -1;
                    } else {
                        if (time < 60) {
                            time = 60;
                        }

                        if (time % 60 == 0) {
                            unit = 'Minutes';
                            time = time / 60;
                        }
                        if (time % 60 == 0) {
                            unit = 'Hours';
                            time = time / 60;
                        }
                        if (time % 24 == 0) {
                            unit = 'Days'
                            time = time / 24;
                        }
                    }
                }

                this.unit = unit;
                this.time = time;
            }

            toSeconds(){
                switch(this.unit){
                    case 'Minutes':
                        return this.time * 60;
                    case 'Hours':
                        return this.time * 3600;
                    case 'Days':
                        return this.time * 86400;
                }
            }
        }

        const validateEmail = (email) => {
            return String(email).toLowerCase().match(/^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/);
        };

        const handleSubmit = (callback) => {
            const emptyContainers = document.querySelectorAll(".conditional-containers");
            if(emptyContainers?.length > 0){
                emptyContainers.forEach(container=>{
                    if(container.style.display == 'none'){
                        container.innerHTML = '';
                    }
                })
            }
            callback();
        }

        const handleForm = (isInitialCall)=>{
            let showSubmit = true;
            const elements = document.querySelectorAll('input.required');
            const button = document.querySelector("button[id='submitBtn']");
            const saveButton = document.querySelector("#saveButton");
            const errorElements = document.querySelectorAll("span[x-text='error']");
            const enabledButton = document.querySelector("[data-buttonEnable='true']");
            const mobileId = document.querySelector("[data-mobileNumber]");
            <#if currentStep??>
                const currentStep = '${currentStep.title!""}';
            <#else>
                const currentStep = "";
            </#if>


            /* Check if any required fields has known error */
            if(errorElements){
                for(let itr=0; itr< errorElements.length; itr++){
                    if(errorElements[itr]._x_isShown && errorElements[itr].classList.contains('required')){
                        showSubmit = false;
                        break;
                    }
                }
            }
            /* Check if all required input elements are filled */
            if(showSubmit && elements && elements.length>0){
                for(let itr=0; itr< elements.length; itr++){
                    if(elements[itr].value === 'false' || elements[itr].value ==='' || !elements[itr].value){
                        showSubmit = false;
                        break;
                    }
                }
            }
            function isVisible( elem ) {
                return !!( elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length );
            };
            /* Check if all non required input elements are filled else disable next*/
            if(showSubmit && elements.length === 0){
                const notRequiredElements = document.querySelectorAll('input:not(.required)');
                showSubmit = false;
                for(let itr=0; itr< notRequiredElements.length; itr++){
                    if(currentStep !== 'streamers-setup' && isVisible(notRequiredElements[itr]) && notRequiredElements[itr].value){
                        showSubmit = true;
                        break;

                    }
                }
            }

            /* Disable or Show submit button based on above */
            if(showSubmit){
                button.disabled = false;
                if(saveButton){
                    saveButton.disabled = false;
                }
            }else{
                button.disabled = true;
                if(saveButton){
                    saveButton.disabled = true;
                }
            }

            /* If initial call disable all the inputs */
            if(isInitialCall === 'error' || (isInitialCall === 'skip' && !showSubmit)){
                button.disabled = true;
                if(saveButton){
                saveButton.disabled = true;
                }
            }

            if(isInitialCall === 'finalStep' || (enabledButton && !mobileId)){
                if(enabledButton){
                    enabledButton.removeAttribute('data-buttonEnable');
                }
                button.disabled = false;
                if(saveButton){
                    saveButton.disabled = false;
                }
            }
        }

        // conversions
        function conversionHandlers() {
            function sortGroups(prop, arr) {
                // sort current elements
                arr.sort(function (a, b) {
                if (a[prop] < b[prop]) {
                    return -1;
                }
                if (a[prop] > b[prop]) {
                    return 1;
                }
                return 0;
                });
                // check sub groups
                arr.forEach(function (item, index) {
                if (!!item.subGroups) {
                    sortGroups(prop, item.subGroups);
                }
                });
                return arr;
            }

            var utils = {};

            utils.sortGroups = sortGroups;

            utils.findIndexById = function (array, id) {
                for (var i = 0; i < array.length; i++) {
                if (array[i].id === id) return i;
                }
                return -1;
            };

            utils.convertAllMultivaluedStringValuesToList = function (
                properties,
                config
            ) {
                if (!properties) {
                return;
                }
                for (var i = 0; i < properties.length; i++) {
                var prop = properties[i];
                if (prop.type === "MultivaluedString") {
                    var configProperty = config[prop.name];

                    if (configProperty == null) {
                    configProperty = [];
                    config[prop.name] = configProperty;
                    }

                    if (typeof configProperty === "string") {
                    configProperty = configProperty.split("##");
                    config[prop.name] = configProperty.filter((e) => e);
                    }
                }
                }
            };
            utils.removeEmptyStringInMultivaluedString = function (properties, config) {
                if (properties && properties.length > 0) {
                for (const prop of properties) {
                    if (prop.type === "MultivaluedString") {
                    config[prop.name] = config[prop.name].filter((e) => e);
                    }
                }
                }
            };

            utils.convertNumberTypeStringValueToInt = function (properties, config) {
                if (!properties) {
                return;
                }
                for (var i = 0; i < properties.length; i++) {
                var prop = properties[i];
                if (prop.type === "Number") {
                    config[prop.name] = Number(config[prop.name]);
                }
                }
            };
            utils.checkNumberTypeMaxValueValidity = function (properties, config) {
                if (!properties) {
                return;
                }
                for (var i = 0; i < properties.length; i++) {
                var prop = properties[i];
                if (prop.type === "Number") {
                    if (prop.maxVal) {
                    if (
                        checkConfigValid(config, prop) &&
                        (config[prop.name] > prop.maxVal || !config[prop.name])
                    ) {
                        return false;
                    }
                    }
                }
                }
                return true;
            };
            function checkConfigValid(config, property) {
                if (property.dependsOn) {
                var conditions = [];
                for (const dependsKey of Object.keys(property.dependsOn)) {
                    if (Array.isArray(property.dependsOn[dependsKey])) {
                    conditions.push(
                        property.dependsOn[dependsKey].indexOf(config[dependsKey]) >= 0
                    );
                    } else {
                    conditions.push(
                        config[dependsKey] === String(property.dependsOn[dependsKey])
                    );
                    }
                }
                if (conditions.length > 0) {
                    if (property.validationType == "anyone_true") {
                    return conditions.some((c) => c);
                    }
                    return conditions.every((c) => c);
                }
                return false;
                } else {
                return true;
                }
            }
            utils.convertAllListValuesToMultivaluedString = function (
                properties,
                config
            ) {
                if (!properties) {
                return;
                }
                for (var i = 0; i < properties.length; i++) {
                var prop = properties[i];
                if (
                    prop.type === "MultivaluedString" ||
                    prop.type === "MultivaluedList"
                ) {
                    var configVal = config[prop.name];

                    if (configVal != null) {
                    if (configVal.length > 0) {
                        var lastVal = configVal[configVal.length - 1];
                        if (lastVal === "") {
                        configVal.splice(configVal.length - 1, 1);
                        }
                    }

                    var attrVals = configVal.join("##");
                    config[prop.name] = attrVals;
                    }
                }
                }
            };

            utils.addLastEmptyValueToMultivaluedLists = function (properties, config) {
                if (!properties) {
                return;
                }

                for (var i = 0; i < properties.length; i++) {
                var prop = properties[i];
                if (prop.type === "MultivaluedString") {
                    var configProperty = config[prop.name];

                    if (configProperty == null) {
                    configProperty = [];
                    config[prop.name] = configProperty;
                    }

                    if (
                    configProperty.length == 0 ||
                    configProperty[configProperty.length - 1].length > 0
                    ) {
                    configProperty.push("");
                    }
                }
                }
            };

            utils.convertBackToMultivaluedList = function (properties, config) {
                if (!properties) {
                return;
                }
                for (var i = 0; i < properties.length; i++) {
                var prop = properties[i];
                if (prop.type === "MultivaluedList") {
                    var configProperty = config[prop.name];

                    if (configProperty == null) {
                    configProperty = [];
                    config[prop.name] = configProperty;
                    }

                    if (typeof configProperty === "string") {
                    configProperty = configProperty.split("##");
                    config[prop.name] = configProperty;
                    }
                }
                }
            };

            utils.checkExpiryTimeValid = function (properties, config) {
                if (!properties) {
                return;
                }
                //regex to check the expiry time (eg. 2s,20m,2h,2d are only allowed)
                const ExpiryTimeRegex = "^([1-9][0-9]*)+(s|m|h|d)$";
                for (const prop of properties) {
                if (prop.type === "ExpiryTime") {
                    var configVal = config[prop.name];
                    if (configVal && configVal.match(ExpiryTimeRegex)) {
                    return true;
                    } else {
                    return false;
                    }
                }
                }
            };

            utils.removeLastEmptyValue = function (componentConfig) {
                for (var configPropertyName in componentConfig) {
                var configVal = componentConfig[configPropertyName];
                if (configVal && configVal.length > 0) {
                    var lastVal = configVal[configVal.length - 1];
                    if (lastVal === "") {
                    console.log(
                        "Remove empty value from config property: " + configPropertyName
                    );
                    configVal.splice(configVal.length - 1, 1);
                    }
                }
                }
            };

            // Allows you to use ui-select2 with <input> tag.
            // In HTML you will then use property.mvOptions like this:
            // <input ui-select2="prop.mvOptions" ng-model="...
            utils.addMvOptionsToMultivaluedLists = function (properties) {
                if (!properties) return;

                for (var i = 0; i < properties.length; i++) {
                var prop = properties[i];
                if (prop.type !== "MultivaluedList") continue;

                prop.mvOptions = {
                    "multiple": true,
                    "simple_tags": true,
                    "tags": angular.copy(prop.options)
                };
                }
            };

            return utils;
        }

        const selectElement = document.getElementById("locale-select");
         function getCookie(name) {
            const cookies = document.cookie.split(';');
            for (let i = 0; i < cookies.length; i++) {
                const ck = cookies[i].trim();
                if (ck.startsWith(name + "=")) {
                return ck.substring(name.length + 1);
                }
            }

            return null;
        }

        const createLocaleSelect = () => {
            let option;
            let langOption
            <#list locales as lang>
                option = document.createElement('option');
                langOption = document.createElement('option');
                option.value = '${lang}';
                langOption.value = '${lang}';
                if ('${locale}' === '${lang}') {
                     option.selected = true;
                     langOption.selected = true;
                }
                option.innerText = ISO6391.getNativeName('${lang}');
                langOption.innerText = ISO6391.getNativeName('${lang}');
                selectElement.appendChild(option);
                <#if showMultiLanSelect>
                    document.getElementById("available-lang").appendChild(langOption);
                </#if>
            </#list>
        }

        const createCategorySelect = () => {
            let categoryOption
            <#list availableCategories as category>
                categoryOption = document.createElement('option');
                categoryOption.value = '${category}';
                categoryOption.innerText = '${category}';
                document.getElementById("recommendedMarketplaceCategories-select").appendChild(categoryOption);
            </#list>
        }

        const handleLangChange = () => {
            updateLangCookie(selectElement.value);
            window.location.reload();
        }

        const updateLangCookie = (lang) => {
            document.cookie = "KEYCLOAK_LOCALE=" + lang + "; path=/";
        }

        document.addEventListener("DOMContentLoaded", function () {
            createLocaleSelect();
            createCategorySelect();
            <#if showMultiLanSelect>
                $("#available-lang").select2({
                    multiple: true,
                    search: false,
                    minimumResultsForSearch: -1,
                    placeholder:"${msg('select-avail-lang')}",
                });
                $('#available-lang').on('select2:unselecting', function (e) {
                    var remainingOptions = $(this).val().length - 1;
                    if (remainingOptions === 0) {
                        e.preventDefault();
                    }
                });
            </#if>
            $("#recommendedMarketplaceCategories-select").select2({
                multiple: true,
                search: false,
                minimumResultsForSearch: -1,
                placeholder: '${msg("select-category")}',
            });
            $('#recommendedMarketplaceCategories-select').val(null).trigger('change');
            <#if showPreview>
                document.getElementById("workspaceName").innerText = document.getElementById("qv_onboarding_display_name").value
                document.getElementById("workspaceNameBold").innerText = document.getElementById("qv_onboarding_display_name").value
            </#if>
        });

        <#if showPreview>
            document.getElementById("qv_onboarding_display_name").addEventListener("keyup", function(event){
                document.getElementById("workspaceName").innerText = event.target.value
                document.getElementById("workspaceNameBold").innerText = event.target.value
            })
        </#if>
    </script>
</body>
</html>