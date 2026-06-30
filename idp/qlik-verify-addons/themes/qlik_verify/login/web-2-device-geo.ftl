<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
<#if section = "head">
    <script defer type="text/javascript" src="/auth/assets/js/jquery.min.js?version=${serverVersion}"></script>
</#if>
<#if section = "title">
        title
<#elseif section = "header">
    <span class="${properties.kcWebAuthnKeyIcon}"></span>
    ${kcSanitize(msg("ashield-registration-title"))?no_esc}
<#elseif section = "form">
            <div class="badge badge-success text-white h-25 ml-1 mt-2_4"
            style="float: right; width: 50px; padding: 1px; line-height: 20px;">${msg('beta')}</div>
            <h2 class="ciba-template-title mt-2">${msg("device-verification")}</h2>
            <div class="my-auto">
                <p class="text-center">${msg("allow-location")}</p>
                <#if error??>
                    <span class="text-danger text-center">${error}</span>
                </#if>
            </div>
        <button class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} btn w-100 mt-2 btn_account bg-orange-red" id="cancelBtn" onclick="onCancel()">Cancel</button>
    <form id="submit" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
        <div class="${properties.kcFormGroupClass!}">
            <input type="hidden" id="latitude" name="latitude"/>
            <input type="hidden" id="longitude" name="longitude"/>
            <input type="hidden" id="accuracy" name="accuracy"/>
            <input type="hidden" id="error" name="error"/>
            <input type="hidden" id="from" name="from" value="web_2_device_geo"/>
            <input type="hidden" id="retry" name="retry" value="false"/>
            <input type="hidden" id="cancel" name="cancel" value="false"/>
            <#if promptNumber??>
                <input type="hidden" id="promptNumber" name="promptNumber" value="${promptNumber}"/>
            </#if>
        </div>
    </form>
    <script defer type="text/javascript">
        $(document).ready(function () {
            <#if !error??>
                const options = {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 0,
                };
                if (navigator.geolocation) {
                    navigator.geolocation.getCurrentPosition(success,error,options);
                } else {
                    error({
                        "code":404,
                        "message":"Not supported"
                    })
                }
            </#if>

            function onRetry(){
                $("#retry").val("true");
                $("#submit").submit();
            }

            function onCancel(){
                        $("#cancel").val("true");
                        $("#submit").submit();
                    }

            function success(pos){
                const cords = pos.coords;
                $("#latitude").val(cords.latitude);
                $("#longitude").val(cords.longitude);
                $("#accuracy").val(cords.accuracy);
                $("#submit").submit();
            }

            function error(err){
                $("#error").val(err.message);
                $("#submit").submit();
            }
        });
    </script>
</#if>
</@layout.registrationLayout>