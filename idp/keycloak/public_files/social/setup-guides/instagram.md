###### Instagram Configuration Instructions
Users will log in to your app using their Instagram profile using the Instagram social connection. 

###### How to setup Connection

###### Prerequisites 
* Create an Instagram account before you begin. 

###### Create a Instagram  project for your app 
Please follow the below steps to complete the setup 

1. To enable Instagram login, you must first complete a series of tasks. 

2. You need to get a `Client ID` and `Client Secret` from Instagram first. The `Redirect URI` is one of the pieces of information you'll need from this page.  

3. You'll need to copy this URI to your clipboard when you register QlikVerify as a client on Instagram.  

4. To use Instagram for login, you must first create a project and a client. The `Facebook Developer Console` is used to administer the `Instagram API`. 

5. Once you've signed into the console, look for the My Apps option in the top right corner of the screen. Select the menu option Add a New App. 

6. Choose Everything Else. 

7. Complete all of the necessary fields. After you've completed that, you'll be sent to the application's dashboard. Select Basic under Settings from the menu in the left navigation panel. 

8. At the bottom, click Add Platform, then [Website] with the globe icon. Enter the URL of your website. 

9. Click Set Up in the Instagram box after selecting Dashboard from the left option. Select Basic Display from the left menu, then Create New App under Instagram. 

10. Give Display Name. 

11. Copy and paste the `Redirect URI` from the QlikVerify into the Instagram Client OAuth Settings block's Valid OAuth Redirect URIs field. 

12. This URL may also be used for Deauthorize Callback and Data Deletion Request. Although none of these are presently supported by QlikVerify, the Facebook Developer Console needs both of them to be completed. 

13. This page will also give you with the App ID and App Secret, which you will need to input into the QlikVerify. To get this, go to App Secret and choose Show. Return to QlikVerify and fill in those details before saving your Instagram Identity Provider. 

14. Following that, the Instagram app must be made public. Select App Review from the left menu, then Requests. After that, follow the on-screen directions. 