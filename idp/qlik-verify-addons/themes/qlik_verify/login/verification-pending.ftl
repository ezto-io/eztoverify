<!--  Verification Pending Intimation Page   -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form id="ver-pending-form" action="${url.loginAction}" method="post">
            <#if email??>
                <input name="email" id="email" type="hidden" value="${email}" />
            </#if>
            <#if mob_num??>
                <input name="mobileNum" id="mobileNum" type="hidden" value="${mob_num}" />
            </#if>
            <input name="ver-triggered" id="ver-triggered" type="hidden" value="false" />
            <div class="col-12 text-center">
                <img class="mb-2 error-img" alt="info-img.png" src="${url.resourcesPath}/img/info-img.png" />
                <#if email_ver??>
                    <p> ${msg("email_ver_not_done_msg")} </p>
                    <button id="nextBtn" type="button" onclick="submitForm()" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
                        ${msg("email_ver_btn")}
                    </button>
                <#else>
                    <p> ${msg("mob_ver_not_done_msg")} </p>
                    <button id="nextBtn" type="button" onclick="submitForm()" class="btn w-100 margin-t-3 btn_account bg-orange-red  ">
                        ${msg("mob_ver_btn")}
                    </button>
                </#if>            
            </div> 
        </form>
        <script type="text/javascript">
        function submitForm(){
            $("#ver-triggered").val("true");
            $("#ver-pending-form").submit();
        }
        </script>
    </#if>
</@layout.registrationLayout>