###### Google Configuration Instructions
Allow your users to login with their Google Account 

###### How to setup Connection

###### Prerequisites 
* Create a Google account before you begin.  

###### Create a Google project for your app
Please follow the below steps to complete the setup 

1. To enable Google login, you must first complete a series of tasks. 

2. You need to get a `Client ID` and `Client Secret` from Google first. The `Redirect URI` is one of the pieces of information you'll need from this page.  

3. You'll need to copy this URI to your clipboard when you register QlikVerify as a client on Google.  

4. You must first establish a project and a client in the `Google Developer Console` to enable Google login. The `client ID` and `client Secret` must then be copied into the QlikVerify Admin Console. 

5. First, let's look at how to make a Google project. Go to the `Google Developer Console` and sign in. 

6. To start a new project, click the Create button.  

7. Click the Create button after entering any value for Project name and Project ID. Await the completion of the project (this may take a while).  

8. You'll be sent to the project dashboard once you've finished creating it. 

9. After that, go to the Google Developer Console's APIs & Services area. Go to the Credentials administration section of that screen. 

10. When users connect into Google using QlikVerify, they will be presented with a consent screen from Google, asking if QlikVerify is permitted to see information about their user profile. 

11. As a result, before developing any secrets for the product, Google wants some basic information about it. You must first configure the `OAuth consent screen` for a new project. 

12. Filling in the Application name is sufficient for the most basic setup. Additional information, such as scopes for Google APIs, can be specified on this page. 

13. After that, you'll need to generate an OAuth `client ID` and a `client secret`. Return to Credentials administration and choose OAuth `client ID` under the Create credentials button under the Credentials tab. 

14. After that, you'll be sent to the Create OAuth client ID page. As the application type, select Web application. Choose a name for your customer. 

15. In the Authorized redirect URIs section, paste the Redirect URI from the QlikVerify. then press the Create button. 

16. You'll be sent to the Credentials page after clicking Create. To examine the settings of your new Google Client, click on your new OAuth 2.0 Client ID. 

17. To use QlikVerify, you'll need to get the client ID and secret from this page. Return to QlikVerify and add the items to your list. 

18. The Default Scopes box on the Add identity provider page for Google is one config option worth noting. You may manually set the scopes that users must authorise when authenticating with this provider using this field. 

19. Please check this [link](https://developers.google.com/oauthplayground/) for a comprehensive list of scopes. QlikVerify employs the following scopes by default: Email address for the openid profile. 

20. If your company utilises `G-Suite` and you want to limit access to just members of your company, you'll need to activate it by entering the `G-Suite` domain into the Hosted Domain area. 
