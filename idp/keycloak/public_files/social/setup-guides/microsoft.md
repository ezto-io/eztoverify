###### Microsoft  Configuration Instructions
Users will log in to your app using their Microsoft profile using the Microsoft social connection. 

###### How to setup Connection

###### Prerequisites 
* Create a Microsoft account before you begin. 

###### Create a Microsoft project for your app 
Please follow the below steps to complete the setup 

1. To enable Microsoft login, you must first complete a series of tasks. 

2. You need to get a `Client ID` and `Client Secret` from Microsoft first. The `Redirect URI` is one of the pieces of information you'll need from this page.  

3. You'll need to copy this URI to your clipboard when you register QlikVerify as a client on Microsoft.  

4. You must first register an OAuth application with Microsoft to allow login using your Microsoft account. To register for a Microsoft application, go to the Microsoft Application Registration website. 

5. Click Create application after entering the application's name. This will take you to your new application's application settings page. 

6. The `Redirect URI` from the QlikVerify must be copied and pasted into the Microsoft application page's Redirect URIs field.  

7. Make sure you save your changes by clicking the Add Url button. 

8. Finally, you must copy the Application ID and secret from this page in order to re-enter them on the QlikVerify Add identity provider page. Return to QlikVerify and indicate the things you want. 
