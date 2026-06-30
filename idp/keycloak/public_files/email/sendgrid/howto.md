##### How to setup Sendgrid

###### Prerequisites 
* You must have an [Sendgrid](https://sendgrid.com/) account.

###### Configure Sendgrid 

1. Log in to your [Sendgrid](https://sendgrid.com/) account. 

2. Next, you can set up your domain so that it can send emails from SendGrid via your domain.To add your domain, go to **Settings** -> **Sender Authentication** -> **Domain Authentication** and add a whitelabel domain. 

3. Create a new API in the dashboard by going to **Settings** -> **API Keys**.

4. Enable Email Messaging.

5. Now, Copy the `API Key` from Sendgrid dashboard and paste it in the settings.

###### Note

>**The value of the `API Key` field can refer a value from an external vault. It will be not stored in our database**