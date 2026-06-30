## QLIKVERIFY

### TEXT FORMAT

Backend Text Format:

   Enter any string needed only in the format of keywords in the code. The value for the entered key should be present 
   in respective properties file present in themes folder. Set the values for the keys in required language using 
   properties file in themes folder. 
       
         Ex: In Java class,
                private static final String PROVIDER_NAME = "whatsapp-login-title";

             properties file,
                whatsapp-login-title = Whatsapp Validation
                whatsapp-login-title =Whatsapp 驗證

Frontend Text Format:
   
   1. In ftl files:  Strings that will be rendered through ftl files should be in the format of keywords only.The value
      for the keywords should be given in respective properties file.

          Ex: In ftl files,
                 ${msg("firstName")}

              properties file,
                 firstName = First name

   2. In html files: Strings that will be rendered from html files should be in the format of keywords. The value
   for the keywords should be given in respective properties file.

          Ex: In html files,
                 {{:: 'authentication-mfa' | translate}} --> for static string
                 {{:: template.category | translate}} --> for dynamic string

              properties file,
                 authentication-mfa=MFA

   3. In js files: Some strings will be rendered through js files and such string should be given in the format of
   keywords only. The value for the keywords should be present in respective properties file.

          Ex: In js files,
                 $translate.instant(d.name)
                 




         
             

             





