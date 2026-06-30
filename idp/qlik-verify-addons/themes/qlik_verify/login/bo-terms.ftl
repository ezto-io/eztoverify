<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "head">
        <#--  Quil JS  -->
        <link href="/auth/assets/css/quill.snow.min.css?version=${serverVersion}" rel="stylesheet" />
        <script defer src="/auth/assets/js/quill.min.js?version=${serverVersion}"></script>
        <#--  PDF JS  -->
        <script src="https://cdn.jsdelivr.net/npm/pdfjs-dist@2.0.385/build/pdf.min.js"></script>
    </#if>
    <#if section = "header">
        ${msg("termsTitle")}
    <#elseif section = "form">
        <div class="d-flex flex-column justify-content-between ciba-template-height">
            <#--  Template Header  -->
            <div class="col-12 mt-3 px-0 d-flex justify-content-end" id="template-header">
                <h2 class="ciba-template-title mr-auto">${msg("terms-and-conditions")}</h2>
                <#if type == "Link" >
                    <a class="text-center tc-link align-items-center text-dark display-web-only" href="${tcContent}" target="_blank"><i class="os-icon-external-link os-icon"></i></a>
                <#elseif type == "PDF" || type == "Image" >
                        <#if isWeb?? && isWeb>
                            <a class="mr-3 display-web-only" href="${tcContent}" id="pdf-link" target="_blank"><img alt="pdf-icon" height="28" src="/auth/assets/img/pdf.svg?version=${serverVersion}"/></a>
                        <#else>
                            <a class="display-web-only" target="_blank" href="${tcContent}" download="termsandcondtions-${tcVer}.pdf"><img alt="download-icon" height="28" src="/auth/assets/img/download.svg?version=${serverVersion}"/></a>
                        </#if>
                </#if>
                <#if skippable?? && skippable>
                    <div id="skipBtn-mobile" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer display-mobile-only">${msg("skip")}</div>
                </#if>
            </div>
            <#--  Template Content  -->
            <#if type == "PDF" || type == "Image" >
                <div class="pdf-holder mt-2" id="pdf-holder"></div>
                <span class="text-danger" id="error"></span>
            <#elseif type == "Text">
                <div class="text-holder rounded mt-2" id="editor"></div>
            <#elseif type == "Link">
                <iframe class="text-holder rounded mt-2 iframe-holder" src="${tcContent}" title="terms and condtions"></iframe>
            </#if>

            <#if type == "Link" >
                <a class="text-center align-items-center text-primary display-mobile-only font-size-14px my-2" href="${tcContent}" target="_blank"><span class="bg-primary-lite p-2"><i class="os-icon-external-link text-primary os-icon pr-1"></i> ${msg("open-site")}</span></a>
            <#elseif type == "PDF" || type == "Image" >
                <#if isWeb?? && isWeb>
                    <a class="text-primary display-mobile-only font-size-14px my-2" href="${tcContent}" id="pdf-link" target="_blank"><span class="bg-primary-lite p-2"><img alt="pdf-icon" height="20" class="pr-1" src="/auth/assets/img/pdf-blue.svg?version=${serverVersion}"/> ${msg("open-pdf")}</span></a>
                <#else>
                    <a class="text-primary display-mobile-only font-size-14px my-2" target="_blank" href="${tcContent}" download="termsandcondtions-${tcVer}.pdf"><span class="bg-primary-lite p-2"><img class="pr-1" height=20 alt="download-icon" src="/auth/assets/img/download-blue.svg?version=${serverVersion}"/> ${msg("download")}</span></a>
                </#if>
            </#if>

            <#--  Template Buttons  -->
            <div>
                <form class="form-actions d-flex flex-column justify-content-around mt-3 tc-action" action="${url.loginAction}" method="POST">
                    <input name="skipped" type="hidden" id="skip-flow" value="false" />
                    <div class="d-flex">
                        <input id="tc-checkbox" type="checkbox" class="mr-3">
                        <label class="tc-text" for="tc-checkbox">${msg("terms-and-condition-confirm-helper-text")}<br/>(${msg("version")} : ${tcVer})</label>
                    </div>

                    <#--  Template Buttons  -->
                    <div class="row mt-3">
                        <#--  Mobile View  -->
                        <div class="col-12 form-actions justify-content-around mt-3 display-mobile-only tc-action" id="template-button-mobile">
                            <input id="kc-decline" name="cancel" type="submit"  value="${msg('doDecline')}" class="w-100 py-3 mr-2 ciba-template-cancelbtn text-center" />
                            <input id="kc-accept-mobile" name="accept" type="submit" value="${msg('doAccept')}" class="w-100 py-3 ml-2 ciba-template-submitbtn text-center" disabled/>
                        </div>
                        <#--  Web View  -->
                        <div class="col-12 justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                            <#if skippable?? && skippable>
                                <div id="skipBtn-web" onclick="transactionSkip()" class="ciba-template-help-text cursor-pointer">${msg("skip")}</div>
                            <#else>
                                <div id="empty-skip-fallback"></div>
                            </#if>
                            <div class="d-flex justify-content-end align-items-center">
                                <input id="kc-decline" name="cancel" type="submit"  value="${msg('doDecline')}" class="w-100 px-3 py-2 mr-2 ciba-template-cancelbtn text-center" />
                                <input id="kc-accept" name="accept" type="submit" value="${msg('doAccept')}" class="w-100 px-3 py-2 ml-2 ciba-template-submitbtn text-center" disabled/>
                            </div>
                        </div>
                    </div>
                </form>
                <div class="clearfix"></div>
            </div>
        </div>
        <script>
            var isTransactionFlowSkipped = false;
            $(document).keyup(function(event) {
                if (event.which === 13) {
                    $("#kc-accept").click();
                }
            });

            $(document).ready( function(){
                <#if type == "Text">
                    const quill = new Quill('#editor', {
                        theme: 'snow',
                        readOnly: true,
                        modules: {
                            toolbar: false,
                        },
                        placeholder: 'Enter the terms and conditions...',
                    });
                    <#noautoesc>
                        quill.setContents(${tcContent});
                    </#noautoesc>
                </#if>
            });

            $("#tc-checkbox").change(function(event) {
                if ($(this).is(':checked')) {
                    $("#kc-accept").prop("disabled", false);
                    $("#kc-accept-mobile").prop("disabled", false);
                } else {
                    $("#kc-accept").prop("disabled", true);
                    $("#kc-accept-mobile").prop("disabled", true);
                }
            });
            function transactionSkip() {
                if(!isTransactionFlowSkipped){
                    isTransactionFlowSkipped = true;
                    $("#skip-flow").val("true");
                    $('form').submit();
                }
            }

            <#if type == "PDF" || type == "Image" >
                function renderPDF(url, canvasContainer, options) {
                    options = options || { scale: 1 };

                    function renderPage(page) {
                        var viewport = page.getViewport(options.scale);
                        var wrapper = document.createElement("div");
                        wrapper.className = "canvas-wrapper";
                        var canvas = document.createElement("canvas");
                        var ctx = canvas.getContext("2d");
                        var renderContext = {
                        canvasContext: ctx,
                        viewport: viewport,
                        };

                        canvas.height = viewport.height;
                        canvas.width = viewport.width;
                        wrapper.appendChild(canvas);
                        canvasContainer.appendChild(wrapper);
                        page.render(renderContext);
                    }

                    function renderPages(pdfDoc) {
                        for (var num = 1; num <= pdfDoc.numPages; num++)
                        pdfDoc.getPage(num).then(renderPage);
                    }

                    PDFJS.disableWorker = true;
                    PDFJS.getDocument(url).then(renderPages);
                }


                <#noautoesc>
                    fetch("${tcContent}")
                    .then((response) => {
                        return response.blob();
                    })
                    .then((blob) => {
                        const bufferReader = new FileReader();

                        bufferReader.onload = function (event) {
                            const typedArray = new Uint8Array(event.target.result);
                            renderPDF({ data: typedArray }, document.getElementById("pdf-holder"));
                        };
                        bufferReader.readAsArrayBuffer(blob);
                    })
                    .catch((error) => {
                        $("error").text = "${msg('pdf-issue')}";
                    });
                </#noautoesc>
            </#if>
        </script>
    </#if>
</@layout.registrationLayout>
