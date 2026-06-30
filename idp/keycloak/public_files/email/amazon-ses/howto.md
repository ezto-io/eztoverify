##### How to setup Amazon SES 

###### Prerequisites 
* You must have an [Amazon AWS](https://aws.amazon.com/ses/) account.
* Determine the Credentials as API

###### Configure Amazon SES with API credentials

1. Log in to your [Amazon AWS](https://aws.amazon.com/ses/) account. 

2. Verify your domain.Check [here](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/verify-domains.html) on how to do it.

3. Verify email addresses.

4. Request production access.

5. Get your AWS access keys.Check [here](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/get-aws-keys.html) on how to get it.

6. Attach a policy with the `ses:SendRawEmail` and `ses:SendEmail` permissions, as in this example:
    ```
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
Click [here](https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_manage.html) to know more about permissions.
7. Now Navigate to QlikVerify and enter your `From` address , AWS `Access Key Id` & `Secret Access Key` and select your `Region`.

###### Note

>**The value of the `Access Key Id` and `Secret Access Key` field can refer a value from an external vault. It will be not stored in our database**