###### OpenID Connect Configuration Instructions
Users will log in to your app using their OpenID profile using the OpenID social connection. 

###### How to setup Connection

###### Prerequisites 
* Create a OpenID Connect Developer account before you begin. 

###### Create a OpenID Connect project for your app 
Please follow the below steps to complete the setup. 

1. To enable OpenID Connect login, you must first complete a series of tasks. 

2. To use OpenID Connect for login, you need to create a `Client` in OpenID Connect Provider.

3. Copy the Redirect URI from the QlikVerify and paste it into the `Valid Redirect URI` of an OpenID Connect Provider.  

4. You need to get a `Client ID` and `Client Secret` from OpenID Connect Provider.  

5. The `Authorization URL` and `Token URL` should be copied from the OpenID Connect Provider Endpoints. 

6. `Client Authentication` - If the OIDC provider is built with the OpenID Connect GUI, this is a necessary attribute. The Client secret sent as post (Client secret submitted as post), client secret sent as basic auth (Client secret provided as basic auth), client secret as jwt, and private key jwt are the values it allows (JTW signed with private key).

7. You will be sent to the QlikVerify application's management page once you have finished this step.  

8. Enter the `Client ID` and `Client Secret` into the QlikVerify page, that you got it from OpenID Connect Provider. 

9. Specify the configuration's as per your requirement. For additional information, please see the official OpenID Connect Provider documentation.
