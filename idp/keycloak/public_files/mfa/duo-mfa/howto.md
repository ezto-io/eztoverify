##### How to setup Duo MFA

###### Prerequisites 
* You must have an [Duo MFA](https://duo.com/) account.

###### Configure Duo MFA 

1. Log in to your [Duo MFA](https://duo.com//) account. 

2. Install `Web SDK` Application

3. Once installed, Create a new Application from `Add New` Option.

4. After Created you application, Get `Client ID`,`Client Secret`, and `API host` from  dashboard.

5. Copy the configurations and paste it in the QlikVerify Dashboard.

###### Note

>**Please note that you need to add the Duo MFA `iframe URL` in the Content Security Policy of the dashboard**

1. Go to `Security Defenses` tab.
2. Click on the `Headers` Section.
3. Add the URL in the `Content-Security-Policy` field after `frame-src 'self'`.