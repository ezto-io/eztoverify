<!-- PWD Entering Page -->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
    <div id="kc-form" class="consent-form-wrapper">
        <div id="kc-form-wrapper">
            <form id="login-password-form" action="${url.loginAction}" class="d-flex flex-column consent-form justify-content-between mt-3" method="post">
            <input id="confirm_reject" name="confirm_reject" type="hidden" value="false">
            <#if consent_rejected??>
                <div class="px-1">
                    <div class="title_sign mb-3">
                        <h2 class="ciba-content-title">${msg('consent-rejection-title')}</h2>
                    </div>
                    <div id="options">
                    <#if consent_rejected_reason?? >
                        <#list consent_rejected_reason as reason >
                            <input type="radio" id="${reason}" name="rejected_reason" value="${reason}">
                            <label for="${reason}">${msg(reason)}</label><br>
                        </#list>
                    </#if>
                    </div>
                    <p id="errorElement" class="ciba-error-description my-1"></p>
                </div>    
            <#else>
                <div>
                    <div class="px-1">
                        <div class="title_sign mb-3">
                        <h2 class="ciba-content-title">${messageTitle}</h2>
                        </div>
                    </div>
                    <#if connectionDetails??>
                        <div class="d-flex flex-wrap align-items-center">
                        <#list connectionDetails?keys as key>
                            <div class="mx-2 my-1 d-flex rounded p-1 ciba-content-tags ">
                                <div style="color:${buttonBgColor}">
                                    <#if key == "location">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                                        <path d="M9.66666 5.99999C9.66666 6.92046 8.92046 7.66666 7.99999 7.66666C7.07953 7.66666 6.33333 6.92046 6.33333 5.99999C6.33333 5.07952 7.07953 4.33333 7.99999 4.33333C8.92046 4.33333 9.66666 5.07952 9.66666 5.99999Z" stroke="currentColor"/>
                                        <path d="M8.83826 11.6624C8.61339 11.8789 8.31286 12 8.00013 12C7.68733 12 7.38679 11.8789 7.16193 11.6624C5.10286 9.6672 2.34345 7.43833 3.68913 4.20248C4.41673 2.45288 6.16329 1.33333 8.00013 1.33333C9.83693 1.33333 11.5835 2.45288 12.3111 4.20248C13.6551 7.43426 10.9024 9.67406 8.83826 11.6624Z" stroke="currentColor"/>
                                        <path d="M12 13.3333C12 14.0697 10.2091 14.6667 8 14.6667C5.79086 14.6667 4 14.0697 4 13.3333" stroke="currentColor" stroke-linecap="round"/>
                                        </svg>
                                    <#elseif key == "dateAndTime">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                                        <path d="M7.33203 8.66797H10.6654M5.33203 8.66797H5.33802M8.66536 11.3346H5.33203M10.6654 11.3346H10.6594" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"/>
                                        <path d="M12 1.33203V2.66536M4 1.33203V2.66536" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"/>
                                        <path d="M1.66797 8.16213C1.66797 5.25729 1.66797 3.80485 2.50272 2.90243C3.33746 2 4.68096 2 7.36797 2H8.63464C11.3216 2 12.6652 2 13.4999 2.90243C14.3346 3.80485 14.3346 5.25729 14.3346 8.16213V8.50453C14.3346 11.4094 14.3346 12.8618 13.4999 13.7643C12.6652 14.6667 11.3216 14.6667 8.63464 14.6667H7.36797C4.68096 14.6667 3.33746 14.6667 2.50272 13.7643C1.66797 12.8618 1.66797 11.4094 1.66797 8.50453V8.16213Z" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"/>
                                        <path d="M2 5.33203H14" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"/>
                                        </svg>
                                    <#elseif key == "device">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                                        <path d="M3.33333 5.99999C3.33333 3.80011 3.33333 2.70016 4.01675 2.01675C4.70017 1.33333 5.80011 1.33333 8 1.33333C10.1999 1.33333 11.2998 1.33333 11.9833 2.01675C12.6667 2.70016 12.6667 3.80011 12.6667 5.99999V9.99999C12.6667 12.1999 12.6667 13.2998 11.9833 13.9833C11.2998 14.6667 10.1999 14.6667 8 14.6667C5.80011 14.6667 4.70017 14.6667 4.01675 13.9833C3.33333 13.2998 3.33333 12.1999 3.33333 9.99999V5.99999Z" stroke="currentColor" stroke-linecap="round"/>
                                        <path d="M7.33333 12.6667H8.66667" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"/>
                                        <path d="M6 1.33333L6.05933 1.68934C6.18792 2.46085 6.25221 2.84661 6.51679 3.08135C6.7928 3.32622 7.18407 3.33333 8 3.33333C8.81593 3.33333 9.2072 3.32622 9.4832 3.08135C9.7478 2.84661 9.81207 2.46085 9.94067 1.68934L10 1.33333" stroke="currentColor" stroke-linejoin="round"/>
                                        </svg>
                                    <#elseif key == "ip">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                                        <path d="M8.5 2C9.78558 2 11.0423 2.38122 12.1112 3.09545C13.1801 3.80968 14.0132 4.82484 14.5052 6.01256C14.9972 7.20028 15.1259 8.50721 14.8751 9.76809C14.6243 11.029 14.0052 12.1872 13.0962 13.0962C12.1872 14.0052 11.029 14.6243 9.76809 14.8751C8.50721 15.1259 7.20028 14.9972 6.01256 14.5052C4.82484 14.0132 3.80968 13.1801 3.09545 12.1112C2.38122 11.0423 2 9.78558 2 8.5C2 6.77609 2.68482 5.12279 3.90381 3.90381C5.12279 2.68482 6.77609 2 8.5 2ZM8.5 3.08333C7.42868 3.08333 6.38143 3.40101 5.49066 3.99621C4.59989 4.5914 3.90563 5.43736 3.49565 6.42713C3.08568 7.4169 2.97841 8.50601 3.18741 9.55674C3.39642 10.6075 3.9123 11.5726 4.66984 12.3302C5.42737 13.0877 6.39253 13.6036 7.44326 13.8126C8.49399 14.0216 9.5831 13.9143 10.5729 13.5043C11.5626 13.0944 12.4086 12.4001 13.0038 11.5093C13.599 10.6186 13.9167 9.57131 13.9167 8.5C13.9167 7.06341 13.346 5.68566 12.3302 4.66984C11.3143 3.65402 9.93659 3.08333 8.5 3.08333Z" fill="currentColor"/>
                                        <path d="M6 6H6.88867V11.4124H6V6ZM10.1937 6C11.4971 6 12.1572 6.55436 12.1572 7.65885C12.1572 8.76335 11.4971 9.35156 10.1768 9.35156H8.83529V11.4294H7.95085V6H10.1937ZM10.126 6.75749H8.83529V8.57715H10.126C10.4231 8.59507 10.7186 8.52121 10.9723 8.36556C11.07 8.27727 11.1457 8.16742 11.1934 8.04474C11.2412 7.92206 11.2597 7.78993 11.2474 7.65885C11.2586 7.52959 11.2388 7.39952 11.1894 7.27951C11.1401 7.15951 11.0628 7.05306 10.9639 6.96907C10.7143 6.81091 10.4207 6.73676 10.126 6.75749Z" fill="currentColor"/>
                                        </svg>
                                    </#if>
                                </div>
                                <span class="mx-2"><small>${connectionDetails[key]}</small></span>
                            </div>
                        </#list>
                        </div>
                    </#if>
                    <#if messageType?? && messageType == "string">
                        <div class="px-1 ciba-content-container mt-3 p-3">
                            <div class="form-group">
                                <div style="position: relative">
                                    <p>${attrMessage}</p>
                                </div>
                            </div>
                        </div>
                    </#if>
                    <#if messageType?? && messageType == "json">
                        <div class="ciba-content-container mt-3 p-3">
                            <div>
                                <#assign keys = attrMessage?keys>
                                <#list keys as key>
                                    <div class="form-group">
                                        <label><b>${key}</b></label>
                                        <div style="position: relative">
                                            <p>${attrMessage[key]}</p>
                                        </div>
                                    </div>
                                    <hr/>
                                </#list>
                            </div>
                        </div>
                    </#if>
                </div>
            </#if>
            <div class="form-group mt-4 mb-0">
                <#if consent_rejected??>
                    <#--  Template Buttons  -->
                    <div class="row">
                        <#--  Mobile View  -->
                        <div class="col-12 justify-content-around align-items-end display-mobile-only" id="template-button-mobile">
                            <button name="action" id="backBtn-mobile" value="back" type="submit" class="btn btn-outline-dark btn-block m-1 py-2">${msg("doBack")}</button>
                            <div id="submitBtn-mobile" value="true" class="btn  border-red  btn-outline-danger btn-block m-1 py-2" onclick="validateForm()">${msg("submit")}</div>
                        </div>
                        <#--  Web View  -->
                        <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                            <div id="empty-skip-fallback"></div>
                            <div class="d-flex justify-content-end align-items-center">
                                <button name="action" id="backBtn" value="back" type="submit" class="btn btn-outline-dark btn-block m-1 py-2">${msg("doBack")}</button>
                                <div id="submitBtn" value="true" class="btn  border-red  btn-outline-danger btn-block m-1 py-2" onclick="validateForm()">${msg("submit")}</div>
                            </div>
                        </div>
                    </div>
                <#else>
                    <div class="row">
                        <#--  Mobile View  -->
                        <div class="col-12 justify-content-around align-items-end display-mobile-only" id="template-button-mobile">
                            <button name="action" id="rejectBtn-mobile" value="rejected" type="submit" class="btn border-red btn-outline-danger btn-block m-1 py-2">${msg("reject")}</button>
                            <button name="action" id="acceptBtn-mobile" value="accepted" type="submit" class="btn border-1 ciba-btn btn-block m-1 py-2">${msg("accept")}</button>
                        </div>
                        <#--  Web View  -->
                        <div class="justify-content-between align-items-center mt-3 display-web-only w-100 px-3" id="template-button-web">
                            <div id="empty-skip-fallback"></div>
                            <div class="d-flex justify-content-end align-items-center">
                                <button name="action" id="rejectBtn" value="rejected" type="submit" class="btn border-red btn-outline-danger btn-block m-1 py-2">${msg("reject")}</button>
                                <button name="action" id="acceptBtn" value="accepted" type="submit" class="btn border-1 ciba-btn btn-block m-1 py-2">${msg("accept")}</button>
                            </div>
                        </div>
                    </div>
                </#if>
            </div>
        </form>
        </div>
    </div>
    </#if>
     <script>
        function validateForm() {
                var options = document.getElementsByName("rejected_reason");
                var error = document.getElementsByName("errorElement");
                var optionSelected = false;

                for (var i = 0; i < options.length; i++) {
                    if (options[i].checked) {
                        optionSelected = true;
                        $("#confirm_reject").val("true");
                         $('form').submit();
                       return;
                    }
                }
                if (!optionSelected) {
                     errorElement.innerHTML ="Please select any one of the option.";
                }

        }

         const radioButtons = document.querySelectorAll('input[name="rejected_reason"]');
            // Add event listener to each radio button
            radioButtons.forEach(radioButton => {
                radioButton.addEventListener('change', function() {
                    // Update the value of the hidden input to the selected radio button's value
                    rejected_reason.value = this.value;
                });
            });
        $(document).keyup(function(event) {
            // Check if the Enter key is pressed (key code 13)
            if (event.which === 13) {
                // Trigger the click event of the button
                $("#acceptBtn").click();
            }
        });

        <#if error??>
            $('#template-error').css('display', 'block');
        </#if>
    </script>
</@layout.registrationLayout>
