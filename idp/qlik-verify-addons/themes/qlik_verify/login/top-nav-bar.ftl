<#macro top_nav_bar>
    <header class="c-header nav-bar-prop" >
      <div class="container">
        <nav class="c-navbar">
            <a class="c-navbar-brand" href="${headerLogoLink}" target="_blank">    
                  <#if tenantLogo?contains("http")>
                    <img class="navbar-logo" src="${tenantLogo}" alt="nav-logo" />
                  <#else> 
                    <img class="navbar-logo" src="data:image/*;base64,${tenantLogo}" alt="nav-logo" />
                  </#if>
                    <div class="qlik-logo-label">${realm.displayName}</div>
            </a>
            <#if headerMenuItems?? && headerMenuItems != "">
              <div class="c-nav-account tab-desktop-view">
                  <#list headerMenuItems?split("##") as menuItem>
                    <#assign menuData = menuItem?split("|")>
                    <#assign menuUrl = menuData[0]>
                    <#assign menuText = menuData[1]>
                    <div class="ml-4">
                      <a class="home-link" href="${menuUrl}" target="_blank"> ${menuText} </a>
                    </div>
                  </#list>
              </div>
              <div class="mobile-view" id="nav-bar">
                <div id="toggle-drop-down" >
                  <svg id="toggleDown" focusable="false" data-prefix="fas" data-icon="bars" class="svg-inline--fa fa-bars fa-w-14 fa-lg cursor-pointer" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512"><path fill="currentColor" d="M16 132h416c8.837 0 16-7.163 16-16V76c0-8.837-7.163-16-16-16H16C7.163 60 0 67.163 0 76v40c0 8.837 7.163 16 16 16zm0 160h416c8.837 0 16-7.163 16-16v-40c0-8.837-7.163-16-16-16H16c-8.837 0-16 7.163-16 16v40c0 8.837 7.163 16 16 16zm0 160h416c8.837 0 16-7.163 16-16v-40c0-8.837-7.163-16-16-16H16c-8.837 0-16 7.163-16 16v40c0 8.837 7.163 16 16 16z"></path></svg>
                  <svg id="toggleUp" focusable="false" data-prefix="fas" data-icon="times" class="svg-inline--fa fa-times fa-w-11 fa-lg cursor-pointer" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 352 512"><path fill="currentColor" d="M242.72 256l100.07-100.07c12.28-12.28 12.28-32.19 0-44.48l-22.24-22.24c-12.28-12.28-32.19-12.28-44.48 0L176 189.28 75.93 89.21c-12.28-12.28-32.19-12.28-44.48 0L9.21 111.45c-12.28 12.28-12.28 32.19 0 44.48L109.28 256 9.21 356.07c-12.28 12.28-12.28 32.19 0 44.48l22.24 22.24c12.28 12.28 32.2 12.28 44.48 0L176 322.72l100.07 100.07c12.28 12.28 32.2 12.28 44.48 0l22.24-22.24c12.28-12.28 12.28-32.19 0-44.48L242.72 256z"></path></svg>
                </div>
              </div>
              <div class="mobile-view c-navbar-collapse scrollbar-none" style="display: none" id="toggle-parent">
                <ul class="c-navbar-nav mx-auto">
                <#list headerMenuItems?split("##") as menuItem>
                  <#assign menuData = menuItem?split("|")>
                    <#assign menuUrl = menuData[0]>
                    <#assign menuText = menuData[1]>
                  <li class="c-nav-item">
                    <a class="tagmanager-top-nav-pricing c-nav-link ml-2" href="${menuUrl}" target="_blank"> ${menuText}
                    </a>
                  </li>
                  </#list>
                </ul>
              <div>
            </#if>
        </nav>
      </div>
    </header>
    <script>
      <#if !tenantLogo?contains("http")>
        $('.navbar-logo').removeAttr("src");
        $('.navbar-logo').attr("src",`${tenantLogo}`.startsWith("data:image")?`${tenantLogo}`:`data:image/png;base64,${tenantLogo}`);
      </#if>
      var barIcon = document.getElementById("toggleDown");
      var crossIcon = document.getElementById("toggleUp");
      $("#toggle-drop-down").on('click', function (event) {
        $('#toggle-parent').toggle('toggle-open');
        document.getElementById("nav-bar").classList.toggle("toggle-open");
    });
    </script>
</#macro>