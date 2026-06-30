<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="nect-verify-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input name="status" type="hidden" id="status" />
            <input name="caseId" type="hidden" id="caseId" value="${caseId !}"/>
            <div class="instructionText">${("scan-qr-follow-instructions")}</div>
            <div class="alignCenter"><img src="${nectJumpQr !}" alt="qr"></img></div>
            <div class="instructionText"><strong>${("Note:")}</strong> ${("donot-close-window")}</div>
            <div class="poweredByText">
                <p>${msg("powered-by")}</p>
                <img src="https://play-lh.googleusercontent.com/p6ukTe1_NJLff6AKK8aNh8oakPZjdlU4uhtdwcYv1Q3pfgPyXYWYHqs8AuS6h_AWASM" alt="Google-logo">
            </div>
            <div class="nect-loading">
                <div class="nect-ld"><div></div><div></div></div>
                <p id="created">${msg("waiting-msg-for-qr")}</p>
                <p id="pending">${msg("document-getting-processed")}</p>
            </div>
        </form>
    </#if>
    <script>
        let nectCaseUrl = "${nectCaseUrl !}";
        let header = "${nectHeader !}";
        let timer = "${pollingFrequency !}";

        $(document).ready(function() {
              $("#created").show();
              $("#pending").hide();
        });

        function getStatus() {
            var myHeaders = new Headers();
            myHeaders.append("Authorization", header);

            var requestOptions = {
              method: 'GET',
              headers: myHeaders,
              redirect: 'follow'
            };

            fetch(nectCaseUrl, requestOptions)
              .then(response => response.text())
              .then(result => {
                console.log("Success", result);
                result = JSON.parse(result);
                if(result && result.state && result.state === "complete") {
                    clearInterval(apiCaller);
                    $("#status").val("success");
                    $("#nect-verify-form").submit();
                } else if(result && result.state && result.state === "pending") {
                    $("#created").hide();
                    $("#pending").show();
                } else {
                    $("#created").show();
                    $("#pending").hide();
                }
              })
              .catch(error => console.log('error', error));
        }

        if(isNaN(parseInt(timer))) {
            timer = 30000;
        } else {
            timer = parseInt(timer) * 1000;
        }
        let apiCaller = setInterval(getStatus, timer);
    </script>

    <style>
        #kc-username {
            display: none;
        }

        .instructionText {
            font-size: 18px;
        }

        .alignCenter {
            text-align: center
        }
        .login-pf-page .card-pf {
            max-width: 900px;
            position: relative;
            min-height: 360px;
            border-width: 5px;
        }
        #nect-verify-form .instructionText {
            font-size: 30px;
            width: 60%;
            font-weight: 700;
            line-height: 36px;
        }
        #nect-verify-form .alignCenter {
            position: absolute;
            right: 50px;
            top: 50px;
            border: solid 10px #1566cc;
            padding: 12px;
            border-radius: 25px;
            width: 230px;
        }
        #nect-verify-form .alignCenter + .instructionText {
            font-size: 18px;
            font-weight: 400;
            margin-top: 20px;
            line-height: 25px;
        }
        nect-verify-form .poweredByText {
            margin-top: 50px;
        }
        #nect-verify-form .poweredByText p {
            font-size: 15px;
        }
        .poweredByText {
            margin-top: 30px;
        }
        .poweredByText img {
            max-width: 60px;
        }

        .nect-loading {
            width: 200px;
            text-align: center;
            margin: auto;
            box-shadow: 0 0 2px 1px #e2e2e2;
            padding: 20px 30px 10px;
            background: #ffffff;
            border-radius: 5px;
            position: absolute;
            bottom: -80px;
            left: 0;
            right: 0;
        }
        .nect-loading p {
            line-height: 18px;
            margin-bottom: 0;
            font-weight: 600;
        }
        .nect-ld {
            display: inline-block;
            position: relative;
            width: 80px;
            height: 80px;
        }
        .nect-ld div {
            position: absolute;
            border: 4px solid #1566cc;
            opacity: 1;
            border-radius: 50%;
            animation: nect-ld 1s cubic-bezier(0, 0.2, 0.8, 1) infinite;
        }
        .nect-ld div:nth-child(2) {
            animation-delay: -0.5s;
        }
        @keyframes nect-ld {
            0% {
                top: 36px;
                left: 36px;
                width: 0;
                height: 0;
                opacity: 1;
            }
            100% {
                top: 0px;
                left: 0px;
                width: 72px;
                height: 72px;
                opacity: 0;
            }
        }
    </style>
</@layout.registrationLayout>
