<#import "template.ftl" as layout>
<@layout.mainLayout active='sso' bodyClass='user'; section>
<#if section = "content">
      <div class="row">
        <div class="col-md-12">
            <h2>Welcome, <b>${ssouser.displayName}</b> </h2>
        </div>
      </div>
    </#if>
</@layout.mainLayout>