##### How to setup Mailgun

###### Prerequisites 
* You must have an [Mailgun](https://www.mailgun.com/) account.

###### Configure Mandrill 

1. Log in to your [Mailgun](https://www.mailgun.com/) account. 

2. Get your `Domain`, `API key` and `From` address from Mailgun dashboard.

3. After Logged in, Create a new API in the dashboard.

    * To get `domain` details, Expand **Sending** option from side menu, and then surf to **Domain** section.Get the `sandbox url` from that section
    *  To get `API key`, Expand **Settings** option from side menu, and then go to **API Keys** menu
    Get the `Private API Key` from the **API Security** tab
    * To Get `From` email address,
        * If it's a premium account, use the registered `from` email address.
        * If it's a free sandbox account, go to **Sending** section -> **Domain setting** ,
          and get `From` email address under SMTP Credentials section



###### Note  

>**The value of the `API Key` field can refer a value from an external vault. It will be not stored in our database.**  

>**If your using Mailgun Free Sandbox account, then you need to register your mails id's with mailbox   sandbox account to receive emails from mailgun.**
>###### Steps:
> 1. Navigate **Sendings** -> **Domains**.
> 2. Click your domain name.
> 3. Register your preferred emails id's in this section