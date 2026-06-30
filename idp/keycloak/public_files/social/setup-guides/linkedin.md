###### LinkedIn Configuration Instructions
Users will log in to your app using their LinkedIn profile using the LinkedIn social connection. 

###### How to setup Connection

###### Prerequisites 
* Create a LinkedIn account before you begin. 

###### Create a LinkedIn  project for your app 
Please follow the below steps to complete the setup 

1. To enable LinkedIn login, you must first complete a series of tasks. 

2. You need to get a `Client ID` and `Client Secret` from LinkedIn first. The `Redirect URI` is one of the pieces of information you'll need from this page.  

3. You'll need to copy this URI to your clipboard when you register QlikVerify as a client on LinkedIn.  

4. To use LinkedIn for login, you must first build an application on the `LinkedIn Developer Network`. 

5. Select the Create Application option from the drop-down menu. This will take you to the page where you will create a new application. 

6. Fill in the necessary values in the form, then click the Submit button. This will take you to the settings page for the new app. 

7. In the Default Application Permissions section, choose r basicprofile and r emailaddress. You'll need to copy the `Redirect URI` from the QlikVerify and paste it into the LinkedIn app settings page's OAuth 2.0 Authorized Redirect URLs box.  

8. Don't forget to hit the Update button after you're finished! 

9. The `client ID and secret` may then be obtained from this page and entered into the QlikVerify. Return to QlikVerify and indicate the things you want. 
