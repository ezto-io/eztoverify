###### Setup Guide

**Note:** The User whom needs to be authenticated in QlikVerify should be available in CyLock.

CyLock Server - If you are using test application for authentication use the `CyLock server` as `UAT`(User Acceptance Testing) orelse go ahead with `LIVE`. Default Will be `UAT`.

CyLock Verify Way - The method through which the user's identity will be validated. The available methods are listed below. PUSH -> Alerts that will appear in a heads-up format on a device. PTOP -> In a Plain Text protocol User will be receving the OTP via Email or Phone Number which can be directly used for authentication. CROTP -> A challenge response protocol is a type of password authentication in which the challenge demands the password and the valid return is the actual password. In an instance, the OTP received at first by the users to their Email or Phone Number has to be entered in the CyLock application and generate a new OTP which should be used for authentication.

API Key - Application(CyLock) provides you with an API key (shared secret) on creation.

Customer ID - It is an unique identification code that you get from your application(CyLock) on creation.

User Mapping - Mapping a user to a recipient server(CyLock) to access an external data resource. 

Waiting Duration to Receive Response - Waiting duration to receive response from the external server(CyLock) .Set the duration like,for days 2d, for hours 2h, for minutes 2m, for secs 2s.