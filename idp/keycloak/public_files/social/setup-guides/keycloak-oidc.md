###### Keycloak OpenID Connect Configuration Instructions
Users will log in to your app using their Keycloak profile using the Keycloak social connection. 

###### How to setup Connection

###### Prerequisites 
* Create a Keycloak Developer account before you begin. 

###### Create a Keycloak OpenID Connect project for your app 
Please follow the below steps to complete the setup 

1. To enable Keycloak OpenID Connect login, you must first complete a series of tasks. 

2. To use Keycloak OpenID Connect for login, you need to create an `Client` in Keycloak.

3. To create a new client, click the `create` button. Copy the Redirect URI from the QlikVerify and paste it into the `Valid Redirect URI` of a Keycloak Client.  

4. You need to get a `Client ID` and `Client Secret` from Keycloak.  

5. To get the `Client Secret` from Keycloak the `Access Type` of the client should be updated to `confidential`. 

6. The `Client Secret` could be found on the `Credentials` tab. 

7. The `Authorization URL` and `Token URL` should be copied from the Keycloak `Realm Settings` Endpoints. 

8. `Client Authentication` - If the OIDC provider is built with the Keycloak GUI, this is a necessary attribute. The Client secret sent as post (Client secret submitted as post), client secret sent as basic auth (Client secret provided as basic auth), client secret as jwt, and private key jwt are the values it allows (JTW signed with private key).

9. You will be sent to the QlikVerify application's management page once you have finished this step.  

10. Enter the `Client ID` and `Client Secret` into the QlikVerify page, that you got it from Keycloak. 

11. Specify the configuration's as per your requirement. For additional information, please see the official Keycloak documentation.
