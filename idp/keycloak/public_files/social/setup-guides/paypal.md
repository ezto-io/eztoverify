###### PayPal  Configuration Instructions
Users will log in to your app using their PayPal profile using the PayPal social connection. 

###### How to setup Connection

###### Prerequisites 
* Create a PayPal account before you begin. 

###### Create a PayPal project for your app 
Please follow the below steps to complete the setup 

1. To enable PayPal login, you must first complete a series of tasks. 

2. You need to get a `Client ID` and `Client Secret` from PayPal first. The `Redirect URI` is one of the pieces of information you'll need from this page.  

3. You'll need to copy this URI to your clipboard when you register QlikVerify as a client on PayPal.  

4. To use PayPal for login, you must first create an application project in PayPal Developer Apps. 

5. To make an app, click the Create App button. 

6. You'll be sent to the app's settings page now. 

7. Make the following amendments: 
    
  *  Choose between Sandbox and Live (Live if the Target Sandbox switch on the Add identity     provider page hasn't been activated). 
  * `Client ID` and `Secret` should be copied and pasted into the QlikVerify. 
  *  Scroll down to the App Settings section. 
  *  In the Return URL field, paste the Redirect URI from QlikVerify. 
  *  Check the box that says "Log in with PayPal." 
  *  Under the personal information area, choose the Full name tick. 
  *  Under the address information column, choose the Email address tick. 
  *  Add a privacy and a user agreement URL to your domain, referring to the appropriate pages. 
