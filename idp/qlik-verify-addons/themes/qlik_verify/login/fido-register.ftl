<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
<#if section = "title">
        title
<#elseif section = "header">
    <span class="${properties.kcWebAuthnKeyIcon}"></span>
    ${kcSanitize(msg("webauthn-registration-title"))?no_esc}
<#elseif section = "form">
    <#if pageType??>
        <div>
            <h2 class="ciba-template-title mt-2">${msg("loginTotpHeader")}</h2>
            <div>
                <p class="text-ellipsis">${msg("verification-for",client.name)}</p>
            </div>
            <#else>
                <h2 class="ciba-template-title mt-2">${msg("fido-setup")}</h2>
            </#if>
            <p class="ciba-template-subtitle text-dark my-2 mt-4 text-center" > 
                ${msg("link-your")} <b>${msg("fido-biometric")}</b> ${msg("for")} <b>${msg("faster-registration")}. </b> 
            </p> 
            <div class="col-12 text-center my-auto">
                <img class="error-img" alt="mail" src="/auth/assets/img/youbikey.svg?version=${serverVersion}" >
                <p id="info-label" class="ciba-template-subtitle text-dark text-center" style="display:none">${msg('click-start-to-continue')}</p>
            </div> 
            <form id="register" class="${properties.kcFormClass!} mt-auto" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
                    <input type="hidden" id="attestationObject" name="attestationObject"/>
                    <input type="hidden" id="publicKeyCredentialId" name="publicKeyCredentialId"/>
                    <input type="hidden" id="authenticatorLabel" name="authenticatorLabel"/>
                    <input type="hidden" id="error" name="error"/>
                    <input type="hidden" id="platform" name="platform"/>
                    <input type="hidden" id="device" name="device"/>
                    <input type="hidden" id="eventOrigin" name="eventOrigin" value=""/>
                    <#if mfaProviderId??>
                        <input type="hidden" id="mfaProviderId" name="mfaProviderId" value="${mfaProviderId}" />
                    </#if>
                </div>
                <button id="btn" onclick="startFido()" type="button" style="display:none" class="btn w-100 margin-t-3 btn_account bg-orange-red">${msg('start')}</button>
            </form>
        </div>
            <script type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
            <script type="text/javascript" src="/auth/assets/js/base64url.min.js?version=${serverVersion}"></script>
            <script type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
            <script type="text/javascript">

                function showStartButton() {
                    const infoLabel = document.querySelector('#info-label');
                    infoLabel.style.display="block";
                    const button = document.querySelector('#btn');
                    button.style.display="block";
                }

                function startFido() {
                    var button = document.getElementById("btn");
                    if (button) {
                        button.disabled = true;
                        document.getElementById("btn").style.display = "block";
                    }
                    registerSecurityKey();
                }

                function init() {
                    if (${isDesktop?c} || isSafari()) {
                        showStartButton();
                    } else {
                        registerSecurityKey();
                    }
                }

                function handleNavigatorCredentials(key){
                    window.result = key;
                    let clientDataJSON = result.response.clientDataJSON;
                    let attestationObject = result.response.attestationObject;
                    let publicKeyCredentialId = result.rawId;

                    $("#clientDataJSON").val(base64url.encode(new Uint8Array(clientDataJSON), {pad: false}));
                    $("#attestationObject").val(base64url.encode(new Uint8Array(attestationObject), {pad: false}));
                    $("#publicKeyCredentialId").val(base64url.encode(new Uint8Array(publicKeyCredentialId), {pad: false}));

                    $("#register").submit();
                }

                function base64urlToBuffer(base64url) {
                    // Convert base64url to base64 by replacing '-' with '+', '_' with '/', and adding padding '='
                    let base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
                    // Pad base64 string to make its length a multiple of 4
                    while (base64.length % 4) {
                        base64 += '=';
                    }
                    // Decode base64 string to a binary string
                    let binaryString = atob(base64);
                    // Convert binary string to a Uint8Array
                    let len = binaryString.length;
                    let bytes = new Uint8Array(len);
                    for (let i = 0; i < len; i++) {
                        bytes[i] = binaryString.charCodeAt(i);
                    }
                    return bytes.buffer; // Return ArrayBuffer
                }


                function isSafari() {
                    return navigator.userAgent.indexOf('Safari') !== -1 && navigator.userAgent.indexOf('Chrome') === -1;
                }

                function getParentOrigin(allowedOrigins) {
                    return new Promise((resolve, reject) => {
                        const message = { type: "requestOrigin", action: "getParentOrigin" };
                        window.parent.postMessage(message, "*");

                        window.addEventListener("message", function listener(event) {
                            if (event.data.type === "getOrigin") {
                                if (allowedOrigins.includes(event.data.parentOrigin)) {
                                    document.getElementById("eventOrigin").value = event.data.parentOrigin;
                                    window.removeEventListener("message", listener);
                                    resolve(event.data.parentOrigin);
                                }
                            }
                        });
                    });
                }


                 async function registerSecurityKey() {
                    // mandatory parameters
                    let challenge = "${challenge}";
                    let userid = "${userid}";
                    let username = "${username}";
                    const webRegisteredOrigin = "${webRegisteredOrigin!''}";
                    const allowedOrigins = webRegisteredOrigin.split("##").filter(origin => origin.trim() !== '');
                    let signatureAlgorithms = "${signatureAlgorithms}";
                    let pubKeyCredParams = getPubKeyCredParams(signatureAlgorithms);
                    let rpEntityName = "${rpEntityName}";
                    let rp = {name: rpEntityName};
                    let parentOrigin = ${isWebSDK?c} && !${openInNewTab?c} ? await getParentOrigin(allowedOrigins) : rpEntityName;

                    if(${isWebSDK?c} && !${openInNewTab?c}) {
                        rpEntityName = parentOrigin;
                        rp = {name: rpEntityName};
                    }

                    let publicKey = {
                        challenge: base64url.decode(challenge, {loose: true}),
                        rp: rp,
                        user: {
                            id: base64url.decode(userid, {loose: true}),
                            name: username,
                            displayName: username
                        },
                        pubKeyCredParams: pubKeyCredParams,
                    };

                    if(!${isWebSDK?c}) {
                       // optional parameters
                       let rpId = "${rpId}";
                       publicKey.rp.id = rpId;
                    }

                    let attestationConveyancePreference = "${attestationConveyancePreference}";
                    if (attestationConveyancePreference !== 'not specified') publicKey.attestation = attestationConveyancePreference;

                    let authenticatorSelection = {};
                    let isAuthenticatorSelectionSpecified = false;

                    let authenticatorAttachment = "${authenticatorAttachment}";
                    if (authenticatorAttachment !== 'not specified') {
                        authenticatorSelection.authenticatorAttachment = authenticatorAttachment;
                        isAuthenticatorSelectionSpecified = true;
                    }

                    let requireResidentKey = "${requireResidentKey}";
                    if (requireResidentKey !== 'not specified') {
                        if (requireResidentKey === 'Yes')
                            authenticatorSelection.requireResidentKey = true;
                        else
                            authenticatorSelection.requireResidentKey = false;
                        isAuthenticatorSelectionSpecified = true;
                    }

                    let userVerificationRequirement = "${userVerificationRequirement}";
                    if (userVerificationRequirement !== 'not specified') {
                        authenticatorSelection.userVerification = userVerificationRequirement;
                        isAuthenticatorSelectionSpecified = true;
                    }

                    if (isAuthenticatorSelectionSpecified) publicKey.authenticatorSelection = authenticatorSelection;

                    let createTimeout = ${createTimeout};
                    if (createTimeout != 0) publicKey.timeout = createTimeout * 1000;

                    let excludeCredentialIds = "${excludeCredentialIds}";
                    let excludeCredentials = getExcludeCredentials(excludeCredentialIds);
                    if (excludeCredentials.length > 0) publicKey.excludeCredentials = excludeCredentials;

                    if (${isMobile?c} || ${isDesktop?c}) {
                        isJsFidoSupported().then(res => {
                            if(res.isSupported){
                                navigator.credentials.create({publicKey})
                                .then(function (result) {
                                        $("#device").val(JSON.stringify(res.device));

                                handleNavigatorCredentials(result);

                                })
                                .catch(function (err) {
                                    $("#error").val(err);
                                    $("#register").submit();

                                });
                            } else {
                                registerFido({publicKey})
                                    .then(function (result) {
                                        if(result.success)
                                        {
                                            window.result = result.data;
                                            let clientDataJSON = result.data.response.clientDataJSON;
                                            let attestationObject = result.data.response.attestationObject;
                                            let publicKeyCredentialId = result.data.rawId;
                                            let platform = result.device.platform;
                                            $("#clientDataJSON").val(clientDataJSON);
                                            $("#attestationObject").val(attestationObject);
                                            $("#publicKeyCredentialId").val(publicKeyCredentialId);
                                            $("#platform").val(platform);
                                            $("#device").val(JSON.stringify(result.device));

                                            $("#register").submit();
                                        }
                                        else{
                                            $("#error").val(result.message);
                                            $("#register").submit();
                                        }
                                    }).catch(function (err) {
                                        $("#error").val(err);
                                        $("#register").submit();

                                    });
                                }
                        });
                    } else {
                        if(${isWebSDK?c} && !${openInNewTab?c})
                        {
                            window.addEventListener('message', function(event) {
                                if (allowedOrigins.includes(event.origin)) {
                                    document.getElementById("eventOrigin").value = event.origin;
                                    if(event.data.success)
                                    {
                                        const serializeable = {
                                            authenticatorAttachment: event.data.result.authenticatorAttachment,
                                            id: event.data.result.id,
                                            rawId: base64urlToBuffer(event.data.result.rawId),
                                            response: {
                                                attestationObject: base64urlToBuffer(event.data.result.response.attestationObject),
                                                clientDataJSON: base64urlToBuffer(event.data.result.response.clientDataJSON)
                                            },
                                            type: event.data.result.type
                                        };
                                        handleNavigatorCredentials(serializeable);
                                    }
                                    else{
                                        if(event.data.err === "webauthn-unsupported-browser-text")
                                        {
                                            $("#error").val("${msg("webauthn-unsupported-browser-text")?no_esc}");
                                            $("#register").submit();
                                        }
                                        else{
                                            $("#error").val(event.data.err);
                                            $("#register").submit();
                                        }
                                    }

                                }
                            }, false);

                              window.parent.postMessage({"type":"create","action":"fido",publicKey}, parentOrigin);
                        }
                        else{
                        // Check if WebAuthn is supported by this browser
                        if (!window.PublicKeyCredential) {
                            $("#error").val("${msg("webauthn-unsupported-browser-text")?no_esc}");
                            $("#register").submit();
                            return;
                        }

                        console.log("publicKey", JSON.stringify(publicKey));

                        navigator.credentials.create({publicKey})
                            .then(function (result) {
                            handleNavigatorCredentials(result);
                            })
                            .catch(function (err) {
                                $("#error").val(err);
                                $("#register").submit();
                            });
                    }
                    }
                }


                function getPubKeyCredParams(signatureAlgorithms) {
                    let pubKeyCredParams = [];
                    if (signatureAlgorithms === "") {
                        pubKeyCredParams.push({type: "public-key", alg: -7});
                        return pubKeyCredParams;
                    }
                    let signatureAlgorithmsList = signatureAlgorithms.split(',');

                    for (let i = 0; i < signatureAlgorithmsList.length; i++) {
                        pubKeyCredParams.push({
                            type: "public-key",
                            alg: signatureAlgorithmsList[i]
                        });
                    }
                    return pubKeyCredParams;
                }

                function getExcludeCredentials(excludeCredentialIds) {
                    let excludeCredentials = [];
                    if (excludeCredentialIds === "") return excludeCredentials;

                    let excludeCredentialIdsList = excludeCredentialIds.split(',');

                    for (let i = 0; i < excludeCredentialIdsList.length; i++) {
                        excludeCredentials.push({
                            type: "public-key",
                            id: base64url.decode(excludeCredentialIdsList[i],
                            {loose: true})
                        });
                    }
                    return excludeCredentials;
                }
            </script>

            <#if !isSetRetry?has_content && isAppInitiatedAction?has_content>
                <input type="submit"
                    class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                    id="registerWebAuthnAIA" value="${msg("doRegister")}" onclick="registerSecurityKey()"
                />
                <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-webauthn-settings-form"
                    method="post">
                    <button type="submit"
                            class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                            id="cancelWebAuthnAIA" name="cancel-aia" value="true"/>${msg("doCancel")}
                    </button>
                </form>
            <#else>
                <script defer>
                    init();
                </script>
            </#if>
    </#if>
    </@layout.registrationLayout>
