---
id: AmazonSES
title: Amazon SES
sidebar_label: Amazon SES
--- 

**Amazon SES,** known as Amazon Simple Email Service, stands as a popular cloud-based email delivery platform that furnishes businesses with a scalable and economical approach to sending transactional, promotional, and marketing emails.

### Prerequisites {#amazon}

* You need to update own email provider in your subscription.
* You must have an active [Amazon AWS](https://aws.amazon.com/ses/) account.

### Access your Amazon AWS

* Log in to your [Amazon AWS](https://aws.amazon.com/ses/) account.
* Begin by verifying your domain as the initial step. If you need guidance on how to accomplish this, see [here.](https://docs.aws.amazon.com/ses/latest/dg/creating-identities.html#just-verify-domain-proc) (If you have already done with your setup, skip to the last point.)
* Attach a policy with the `ses:SendRawEmail` and `ses:SendEmail` permissions, as in this example below:
* Initiate a request for production access, and subsequently obtain your AWS access keys. To know how to do that, see [here.](https://docs.aws.amazon.com/ses/latest/dg/setting-up.html)

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ses:SendRawEmail",
                "ses:SendEmail"
            ],
            "Resource": "*"
        }
    ]
}
```

### Configure Amazon SES

*1* Navigate to **Workspace,** click on **Notification,** and choose **Email.**

*2* Click on `+ADD PROVIDER`

*3* You will be directed to the **New Email Provider** page, where all available providers will be displayed.

![Amazon SES](https://images.tango.us/workflows/b4432921-71a2-4ada-a78b-5665d0091330/steps/13daa641-93e4-4978-b8c5-f6aa5fbd77b6/71a1e680-ecc9-4bc9-ab0c-10c5f2740046.png)

*4* Choose the **Amazon SES.**

*5* You will be taken to the **Amazon SES** provider configs page. To obtain the necessary configs, log into Amazon AWS. Refer to the [prerequisites](#amazon)  on how to do it.

![Configurations](https://images.tango.us/workflows/b4432921-71a2-4ada-a78b-5665d0091330/steps/79e59b10-0ff1-4949-9665-3b13f9d83013/8955ec50-8de7-417c-b0a8-3943c9fed1c3.png)

*6* Navigate back to your **Workspace** and input the **From address,** **AWS Access Key Id,** and **Secret Access Key.** Additionally, select your preferred **Region.**

> The values for **Access Key Id** and **Secret Access Key** can be sourced from an external vault. They won't be stored in our database.

*7* After providing the necessary configurations.

*8* Click on `TEST CONFIGURATION` to ensure the configuration looks good.

![Test Configuration](https://images.tango.us/workflows/b4432921-71a2-4ada-a78b-5665d0091330/steps/09839868-22f4-4ddc-8017-22d617f76181/19e54b78-0f49-4244-a4bc-99e334504ece.png)

*9* Provide email address and click on **SUBMIT.**

*10* If everything is set up correctly, a test success email will be sent to the email address you provided earlier. Click on the **SAVE** button**.**