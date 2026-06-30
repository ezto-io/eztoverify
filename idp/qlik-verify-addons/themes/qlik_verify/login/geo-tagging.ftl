<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
<#if section = "head">
<#--  <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>  -->
    <#if ismobile?? && ismobile>
            <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
            <script defer type="text/javascript" src="/auth/assets/js/base64url.min.js?version=${serverVersion}"></script>
            <script defer type="text/javascript" src="/auth/assets/js/ezto.min.js?version=${serverVersion}"></script>
        </#if>
</#if>
<#if section = "title">
        title
<#elseif section = "header">
    <span class="${properties.kcWebAuthnKeyIcon}"></span>
    ${kcSanitize(msg("ashield-registration-title"))?no_esc}
<#elseif section = "form">
            <h2 class="ciba-template-title mt-2">${msg("geo_tagging")}</h2>
            <div class="my-auto">
                <p id="location-general-instructions" class="text-center">${msg("allow-location")}</p>
                <div id="location-instructions" class="mt-2 text-center" style="display: none;"></div>
            </div>

            <div class="col-12 d-none justify-content-center flex-column align-items-center submit-form-loader-height py-5" id="spinner-submit">

                <div class="d-flex justify-content-center align-items-center py-5">
                    <div class="spinner-border branding-primary-color" role="status">
                        <span class="sr-only">Loading...</span>
                    </div>
                </div>

            </div>

            <button class="btn w-100 mt-2 btn_account bg-orange-red" id="retry" style="display: none; "onclick="onRetry()">${msg("retry")}</button>
        <button class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} btn w-100 mt-2 btn_account bg-orange-red" id="cancelBtn" onclick="onCancel()">${msg("doCancel")}</button>
    <form id="submit" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <input type="hidden" id="latitude" name="latitude"/>
            <input type="hidden" id="longitude" name="longitude"/>
            <input type="hidden" id="accuracy" name="accuracy"/>
            <input type="hidden" id="errorMsg" name="errorMsg" value="${errorMsg}"/>
            <input type="hidden" id="geoTaggingVerified" name="geoTaggingVerified" value=""/>
            <input type="hidden" id="instructions" name="instructions"/>
            <input type="hidden" id="from" name="from" value="geo_tagging"/>
            <input type="hidden" id="isRetry" name="isRetry" value="${isRetry}"/>
            <input type="hidden" id="retryCount" name="retryCount" value="${retryCount}"/>
            <input type="hidden" id="retriedCount" name="retriedCount" value="${retriedCount}"/>
            <input type="hidden" id="cancel" name="cancel" value="false"/>
            <input type="hidden" id="ismobile" name="ismobile" value="{isMobile?c}"/>

            <input type="hidden" id="location" name="location"/>
            <input type="hidden" id="success" name="success" value="true"/>
            <input type="hidden" id="time" name="time"/>
            <#if promptNumber??>
                <input type="hidden" id="promptNumber" name="promptNumber" value="${promptNumber}"/>
            </#if>
        </div>
    </form>
    <script defer type="text/javascript">
            $(document).ready(function () {
            var ismobile = <#if ismobile?? && ismobile>true<#else>false</#if>;

            $("#ismobile").val(ismobile);
                getGeoLocation();
            });

            function getGeoLocation() {
                <#if ismobile?? && ismobile>
                    getMobileGeoLocation();
                <#else>
                    getWebGeoLocation();
                </#if>
            }

            function getMobileGeoLocation() {
                readDeviceDetails().then(function(result){
                    $("#errorMsg").val(result.message);
                    $("#success").val(result.success);
                    $("#location").val(JSON.stringify(result.location));
                    $("#time").val(JSON.stringify(result.time));
                    if(result.message) {
                        if(parseInt(retryCount.value) == parseInt(retriedCount.value)) {
                            $("#retry").prop("disabled", true);
                            $("#submit").submit();
                        }
                        $("#retry").show();
                        $("#retry").prop("disabled", false);
                    } else {
                        $("#geoTaggingVerified").val('true');
                        $("#submit").submit();
                    }
                });
            }

            function getWebGeoLocation() {
                const options = {
                    enableHighAccuracy: true,
                    timeout: 10000,
                    maximumAge: 0,
                };
                if (navigator.geolocation) {
                    $("#location-instructions").hide();
                    $("#location-general-instructions").show();
                    navigator.geolocation.getCurrentPosition(success, error, options);
                    $("#spinner-submit").removeClass("d-none").addClass("d-flex");
                    $("#retry").hide();
                } else {
                    error({"code": 404, "message": "Not supported"});
                }
            }

            function success(pos){
                const coords = pos.coords;
                $("#latitude").val(coords.latitude);
                $("#longitude").val(coords.longitude);
                $("#accuracy").val(coords.accuracy);
                $("#errorMsg").val("");
                $("#geoTaggingVerified").val('true');
                $("#submit").submit();
            }

            function error(err) {
                $("#spinner-submit").removeClass("d-flex").addClass("d-none");
                $("#retry").show();
                $("#retry").prop("disabled", false);
                $("#errorMsg").val(err.message);
                $("#geoTaggingVerified").val('false');

                if(parseInt(retryCount.value) == parseInt(retriedCount.value)) {
                    $("#retry").prop("disabled", true);
                    $("#submit").submit();
                }

                if (err.code === 1) {
                     let instructions = getLocationInstructions();
                     $('#instructions').val(instructions);
                     $("#location-instructions").html("<p>" + instructions + "</p>").show();
                     $("#location-general-instructions").hide();
                    return;
                }
               $("#submit").submit();
            }

            function onRetry(){
                $("#retriedCount").val(parseInt(retriedCount.value) + 1);
                $("#isRetry").val("true");
                $("#retry").prop("disabled", true);
                if(parseInt(retryCount.value) >= parseInt(retriedCount.value)) {
                     if(ismobile.value === 'false') {
                        $("#location-instructions").hide();
                        $("#location-general-instructions").show();
                        $("#spinner-submit").removeClass("d-none").addClass("d-flex");
                        setTimeout(() => {getGeoLocation(); }, 1000);
                    } else {
                        getGeoLocation();
                    }
                }
            }

            function onCancel(){
                $("#cancel").val("true");
                $("#submit").submit();
            }

            function getLocationInstructions() {
                let userAgent = navigator.userAgent.toLowerCase();
                let instructions = `<div style="text-align: center;"><strong>${msg("locationGeneralInstruction")?no_esc}</strong></div><br>`;
                let steps="";

                if (userAgent.includes("chrome")) {
                    steps += "${msg("chromeLocationEnableSteps")?no_esc}";
                } else if (userAgent.includes("firefox")) {
                    steps += "${msg("firefoxLocationEnableSteps")?no_esc}";
                } else if (userAgent.includes("safari")) {
                    steps += "${msg("safariLocationEnableSteps")?no_esc}";
                } else if (userAgent.includes("edge")) {
                    steps += "${msg("edgeLocationEnableSteps")?no_esc}";
                } else {
                    steps += "${msg("defaultLocationMessage")?no_esc}";
                }

                instructions += `<div style="text-align: left; margin: 0 auto; max-width: 400px;">` + steps + `</div>`;
                return instructions;
            }

        </script>
</#if>
</@layout.registrationLayout>