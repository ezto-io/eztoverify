<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <form action="${url.loginAction}" method="post">
            <input name="actionType" type="hidden" value="${action.type}"/>
            <div class="title_sign">
              <h2>${action.title}</h2>
            </div>
            <div class="form-group mt-4">
                <p class="text-center">${msg("${actionMessage}")}</p>
            </div>
            <#list action.actions as act>
                <button id="${act.name}" name="${act.name}" value="${act.name}" type="submit" class="btn w-100 mt-2 btn_account bg-orange-red">
                    ${act.text}
                </button>
            </#list>
        </form>
    </#if>
</@layout.registrationLayout>