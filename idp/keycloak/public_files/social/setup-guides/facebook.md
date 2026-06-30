###### Facebook Configuration Instructions
Users will log in to your app using their Facebook profile using the Facebook social connection. 

###### How to setup Connection

###### Prerequisites 
* Create a Facebook account before you begin. 

###### Create a Facebook project for your app 
Please follow the below steps to complete the setup 

1. To enable Facebook login, you must first complete a series of tasks. 

2. You need to get a `Client ID` and `Client Secret` from Facebook first. The `Redirect URI`is one of the pieces of information you'll need from this page.  

3. You'll need to copy this URI to your clipboard when you register QlikVerify as a client on Facebook.  

4. You must first establish a project and a client in the Facebook Developer Console to allow Facebook login.  

5. Once you’ve logged into the console there is a pull down menu in the top right corner of the screen that says My Apps. Select the menu option Add a New App.  

6. Select the logo for the website. Select Skip and Create App ID from the drop-down menu. 

7. Both the email address and the app category must be filled out. After you've completed that, you'll be sent to the application's dashboard. Select Settings from the left-hand menu.  

8. At the bottom of this page, pick the Website icon from the Add Platform menu. Copy and paste the Redirect URI from the QlikVerify into the Facebook Website settings block's Site URL field. 

9. Following that, the Facebook app must be made public. Switch the "Yes" button on the App Review left menu item. 

10. This page will also provide you with the `App ID` and `App Secret`, which you will need to input into the QlikVerify Add identity provider page.  

11. To get this, go to the Dashboard left menu item and choose Show under App Secret from the drop-down menu. Return to QlikVerify and fill in those details before saving your Facebook Identity Provider. 

12. The Default Scopes box on the Add identity provider page for Facebook is an important config option to remember. You may manually set the scopes that users must authorise when authenticating with this provider in this field. Please check this [link](https://developers.facebook.com/docs/graph-api} for a full list of scopes. QlikVerify employs the following scopes by default: email. 

13. Another thing to keep in mind is that by default, QlikVerify sends a profile request to graph.facebook.com/me?fields=id,name,email,first name,last name, and the answer only includes the fields requested. 

14. If you wish to get more fields from a Facebook profile (for example, birthdate), create a matching scope as stated in the previous paragraph and provide the field name in the Additional user's profile fields configuration option field. 

15. The Facebook GraphQL API Explorer can help you figure out what field names are accessible and what scopes they belong to. 
