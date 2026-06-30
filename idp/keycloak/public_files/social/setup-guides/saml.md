###### SAML Configuration Instructions
Users will log in to your app using their SAML profile. 

###### How to setup Connection

###### Prerequisites 
* Create a SAML Developer account before you begin. 

>Note: The Applications (Example: Salesforce) which supports `SAML` must be configured.
###### Create a SAML project for your App 
Please follow the below steps to complete the setup.

1. To enable SAML login, you must complete a series of tasks. 

2. Create an application in QlikVerify with `Application Protocol` as `SAML`.

3. Register with one of our `SSO integrations` (Example: Salesforce).

4. Click on the `Setup` button and create your own account. 

5. Go to `Settings` section and click on `Identity`.

6. Click on `Single Sign-On Settings`.

7. Create a new `SAML Single Sign-On Settings` by clicking on the new button.

8. Enter all the required details and click on the `Save` button.

9. On successful completion, the `Login` and `Logout` URLs will be generated and published under `Endpoints` section.

10. Copy the `Login` and `Logout` URLs from salesforce application and paste it under `SAML` social connection in QlikVerify.

11. Specify the rest configuration's as per your requirement. For additional information, please see the official SAML documentation.

12. Click on the `Save` button.

> [!Note]
Redirect URL from QlikVerify needs to be added in the SAML application whereas the same ClientID and Client Secret (Login and Logout URL) should be added to QlikVerify.
> 
