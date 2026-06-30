<#import "template.ftl" as layout>
<@layout.registrationLayout showAnotherWayIfPresent=false; section>
<#if section = "title">
     title
<#elseif section = "header">
    ${kcSanitize(msg("webauthn-login-title"))?no_esc}
<#elseif section = "form">
    <h2 class="ciba-template-title mt-2">${msg("fido-auth")}</h2>
    <p class="ciba-template-subtitle text-dark my-2 mt-4 text-center" > 
        ${msg("use-your")} <b>${msg("fido-biometric")}</b> ${msg("for")} <b>${msg("faster-login")}. </b>
    </p> 
    <div class="col-12 text-center mt-auto">
        <img class="error-img" alt="mail" src="/auth/assets/img/youbikey.svg?version=${serverVersion}" >
        <p id="info-label" class="ciba-template-subtitle text-dark text-center" style="display:none">${msg('click-start-to-continue')}</p>
    </div> 
    <form id="webauth" class="${properties.kcFormClass!} mt-auto" action="${url.loginAction}" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <input type="hidden" id="troubleShootMFA" name="troubleShootMFA" value="false" />
            <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
            <input type="hidden" id="authenticatorData" name="authenticatorData"/>
            <input type="hidden" id="signature" name="signature"/>
            <input type="hidden" id="credentialId" name="credentialId"/>
            <input type="hidden" id="userHandle" name="userHandle"/>
            <input type="hidden" id="error" name="error"/>
            <input type="hidden" id="platform" name="platform"/>
            <input type="hidden" id="eventOrigin" name="eventOrigin" value=""/>
        </div>
        <button id="btn" type="button" style="display:none" class="btn w-100 margin-t-3 btn_account bg-orange-red" onclick="startVerification()">Start</button>
    </form>
    <#if authenticators??>
        <form id="authn_select" class="${properties.kcFormClass!}">
            <#list authenticators.authenticators as authenticator>
                <input type="hidden" name="authn_use_chk" value="${authenticator.credentialId}"/>
            </#list>
        </form>
    </#if>

    <#if showTroubleShoot?? && showTroubleShoot>
       <p class="col-12 font-s-13 mb-0 margin-t-3 text-center">
            <a onclick="troubleShootMFAFunc()" class="action-link-clr cursor-pointer">${msg("troubleshoot_mfa")}</a>
        </p>
    </#if>

    <script type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
    <script type="text/javascript" src="/auth/assets/js/base64url.min.js?version=${serverVersion}"></script>
    <script type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
    <script type="text/javascript">
        function init() {
            if (${isDesktop?c} || isSafari()) {
                showStartButton();
            } else {
                startVerification();
            }
        }

        function showStartButton() {
            const infoLabel = document.querySelector('#info-label');
            infoLabel.style.display="block";
            const button = document.querySelector('#btn');
            button.style.display="block";
        }

        function handleNavigatorCredentials(key){
            window.result = key;
            let clientDataJSON = result.response.clientDataJSON;
            let authenticatorData = result.response.authenticatorData;
            let signature = result.response.signature;

            $("#clientDataJSON").val(base64url.encode(new Uint8Array(clientDataJSON), { pad: false }));
            $("#authenticatorData").val(base64url.encode(new Uint8Array(authenticatorData), { pad: false }));
            $("#signature").val(base64url.encode(new Uint8Array(signature), { pad: false }));
            $("#credentialId").val(result.id);
            if(result.response.userHandle) {
                $("#userHandle").val(base64url.encode(new Uint8Array(result.response.userHandle), { pad: false }));
            }
            $("#webauth").submit();
        }

        function isSafari() {
            return navigator.userAgent.indexOf('Safari') !== -1 && navigator.userAgent.indexOf('Chrome') === -1;
        }
            function troubleShootMFAFunc(){
                $("#troubleShootMFA").val("true");
                $("#webauth").submit();
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



            const startVerification = () => {
                var button = document.getElementById("btn");
                if (button) {
                    button.disabled = true;
                    document.getElementById("btn").style.display = "block";
                }
                let isUserIdentified = ${isUserIdentified};
                if (!isUserIdentified) {
                    doAuthenticate([]);
                    return;
                }
                checkAllowCredentials();
            };

        function checkAllowCredentials() {
            let allowCredentials = [];
            let authn_use = document.forms['authn_select'].authn_use_chk;

            if (authn_use !== undefined) {
                if (authn_use.length === undefined) {
                    allowCredentials.push({
                        id: base64url.decode(authn_use.value, {loose: true}),
                        type: 'public-key',
                    });
                } else {
                    for (let i = 0; i < authn_use.length; i++) {
                        allowCredentials.push({
                            id: base64url.decode(authn_use[i].value, {loose: true}),
                            type: 'public-key',
                        });
                    }
                }
            }
            doAuthenticate(allowCredentials);
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

        async function doAuthenticate(allowCredentials) {
            let challenge = "${challenge}";
            let userVerification = "${userVerification}";
            let rpId = "${rpId}";
            const webRegisteredOrigin = "${webRegisteredOrigin!''}";
            const allowedOrigins = webRegisteredOrigin.split("##").filter(origin => origin.trim() !== '');
            let parentOrigin = ${isWebSDK?c} && !${openInNewTab?c} ? await getParentOrigin(allowedOrigins) : rpId;
            let publicKey = '';

             if(${isWebSDK?c}) {
                rpId = {name: parentOrigin};
                publicKey = {
                    rp: rpId,
                    challenge: base64url.decode(challenge, { loose: true })
                };
             } else {
                 publicKey = {
                     rpId: rpId,
                     challenge: base64url.decode(challenge, { loose: true })
                 };
             }


            if (allowCredentials.length) {
                publicKey.allowCredentials = allowCredentials;
            }

            if (userVerification !== 'not specified') publicKey.userVerification = userVerification;

            if (${isMobile?c} || ${isDesktop?c}) {
                isJsFidoSupported().then(res => {
                    if(res.isSupported){
                        navigator.credentials.get({publicKey})
                        .then((result) => {
                            handleNavigatorCredentials(result);
                        })
                        .catch((err) => {
                            $("#error").val(err);
                            $("#webauth").submit();
                        });
                    } else {
                        signFido({publicKey})
                            .then(function (result) {
                                if(result.success)
                                {
                                    window.result = result.data;

                                    let clientDataJSON = result.data.response.clientDataJSON;
                                    let authenticatorData = result.data.response.authenticatorData;
                                    let signature = result.data.response.signature;
                                    let platform = result.device.platform;

                                    $("#clientDataJSON").val(clientDataJSON);
                                    $("#authenticatorData").val(authenticatorData);
                                    $("#signature").val(signature);
                                    $("#credentialId").val(result.data.id)
                                    $("#platform").val(platform);
                                    if(result.data.response.userHandle) {
                                        $("#userHandle").val(result.data.response.userHandle);
                                    }
                                    $("#webauth").submit();
                                }
                                else{
                                    $("#error").val(result.message);
                                    $("#webauth").submit();
                                }
                            }).catch(function (err) {
                                $("#error").val(err);
                                $("#webauth").submit();
                            });
                        }
                });
            } else {
                if(${isWebSDK?c} && !${openInNewTab?c})
                    {
                        window.addEventListener('message', function(event) {
                                if (allowedOrigins.includes(event.origin)) {
                                document.getElementById("eventOrigin").value = event.origin;
                                console.log(event.data);
                                if(event.data.success)
                                {
                                    const serializeable = {
                                        authenticatorAttachment: event.data.result.authenticatorAttachment,
                                        id: event.data.result.id,
                                        rawId: base64urlToBuffer(event.data.result.rawId),
                                        response: {
                                            authenticatorData: base64urlToBuffer(event.data.result.response.authenticatorData),
                                            clientDataJSON: base64urlToBuffer(event.data.result.response.clientDataJSON),
                                            signature: base64urlToBuffer(event.data.result.response.signature)
                                        },
                                        type: event.data.result.type
                                    };
                                    handleNavigatorCredentials(serializeable);
                                }
                                else{
                                    if(event.data.err === "webauthn-unsupported-browser-text")
                                    {
                                        $("#error").val("${msg("webauthn-unsupported-browser-text")?no_esc}");
                                        $("#webauth").submit();
                                    }
                                    else{
                                        $("#error").val(event.data.err);
                                        $("#webauth").submit();
                                    }
                                }
                            } 
                        }, {once: true});
                        window.parent.postMessage({"type":"get","action":"fido",publicKey}, parentOrigin);
                    }
                else{
                    // Check if WebAuthn is supported by this browser
                    if (!window.PublicKeyCredential) {
                        $("#error").val("${msg("webauthn-unsupported-browser-text")?no_esc}");
                        $("#webauth").submit();
                        return;
                    }

                    console.log("publiKey", JSON.stringify(publicKey))
                    navigator.credentials.get({publicKey})
                        .then((result) => {
                            handleNavigatorCredentials(result);
                        })
                        .catch((err) => {
                            console.log("err", err);
                            $("#error").val(err);
                            $("#webauth").submit();
                        });
                }   
            }
        }
            
            init();
    </script>
    <#elseif section = "info">

    </#if>
    </@layout.registrationLayout>
