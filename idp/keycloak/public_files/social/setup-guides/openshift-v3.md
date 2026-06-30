###### Openshift v3  Configuration Instructions
Users will log in to your app using their Openshift3 profile. 

###### How to setup Connection

###### Prerequisites 
* Create a Openshift v3 account before you begin. 

###### Create a Openshift v3  project for your app 
Please follow the below steps to complete the setup 

1. To enable Openshift v3 login, you must first complete a series of tasks. 

2. To begin, go to the Identity Providers menu item on the left side of the screen and choose OpenShift v3 from the list.  

3. This takes you to the page where you may add a new identity provider. 

4. You will use the `OC command line tool` to register your client.
`
$ oc create -f <(echo ' 
kind: OAuthClient 
apiVersion: v1 
metadata: 
 name: QV-client  
secret: "..."  
redirectURIs: 
 - "http://www.example.com/"  
grantMethod: prompt  
') 
`
5. The name of the OAuth client you're using. When making requests to `<openshift_master>/oauth/authorize `and `<openshift_master>/oauth/token`, this is passed as the client id request parameter. 

6. The `client secret` parameter is required in the request. 

7. One of the URIs in redirectURIs must be identical to (or preceded by) the redirect uri parameter in requests to `<openshift_master>/oauth/authorize` and `<openshift_master>/oauth/token`. 

8. When this client requests tokens but has not yet been given access by the user, the grant Method is utilised to determine what action to take. 

9. Return to the QlikVerify and input the client ID and secret defined by the OC create command. Return to QlikVerify and indicate the things you want. 

10. For additional information, please see the official OpenShift documentation. 
