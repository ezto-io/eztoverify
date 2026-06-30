---
id: EncryptRequest
title: Encrypt Request
sidebar_label: Encrypt Request
--- 

import DropBox from "../../../src/components/docs/DropBox"

# Encrypt Request

Encrypting a request ensures the security and confidentiality of the data being transmitted. This process involves converting the request body into an unreadable format using encryption algorithms, such as **AES (Advanced Encryption Standard).**

Only authorized parties with the correct decryption key can convert the data back to its original form, ensuring that sensitive information remains protected from unauthorized access during transmission.

To help visualize this process, the following flow diagram provides an overview of how encryption works.

![Flow](https://images.tango.us/workflows/72e78532-1ee4-4a3d-b67f-cf7e2cb15485/steps/386550cf-5976-4410-938b-78400114cdd3/615f32f8-6e1a-4f31-ada9-e55c3b63bbbf.png)

### Access Application

*1* Navigate to **Workspace**, and click on **Application**.

*2* This will take you to the list of all applications available in your dashboard.

*3* Click on the application you wish to view enable encryption. This will open the application **General** page.

![Application](https://images.tango.us/workflows/72e78532-1ee4-4a3d-b67f-cf7e2cb15485/steps/7b6ac068-e4e1-4448-a414-eb2497fba9ca/29f75271-8f39-4966-9a5a-5d04dcb0f24e.png)

*4* Within the application's General page, click on the **Developers** tab.

![Developers](https://images.tango.us/workflows/72e78532-1ee4-4a3d-b67f-cf7e2cb15485/steps/b725ab87-7a8a-4702-bdbf-a8a22212021d/cda85a1f-76b5-428c-bcf3-78254788fded.png)

### Enable Encryption

*1* Toggle the enable  for **Encryption** and select the **AES** key which will be available in the dropdown. To know how to create a **Key,** see [here.](/verify/docs/EV/AdvancedSettings/Keys/)

> Make sure your required workflow is set before starting with the execution process in order to get the expected result.

![Secret Key](https://images.tango.us/workflows/72e78532-1ee4-4a3d-b67f-cf7e2cb15485/steps/25cc1e7d-db71-4c1d-8280-f7f4427bcf71/641cedc0-1075-4fc3-8abb-7aca6e14ba45.png?mark-x=1126&mark-y=1005&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz00MDImaD00NSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)

*2* After choosing the **AES** key, the **Secret Key** will be displayed. Copy this key to your clipboard.

*3* Based on the requirements, build your API request body.

*4* Now, using the AES **Secret key,** you can encrypt the request payload with any of the available libraries. Ensure that the same **key size** is used as the selected **AES** key. (use padding if required)

<DropBox title="Here are some AES libraries">

For additional information, please refer to their official documentation.
<table>
   <thead>
      <tr>
         <th width="190">Language</th>
         <th>Libraries</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td><b>Java</b></td>
         <td>Use javax.crypto</td>
      </tr>
      <tr>
         <td><b>JavaScript</b></td>
         <td>
         <ul>
                <li>Web Crypto API (built into modern browsers)</li>
                <li>Node.js crypto module (for server-side JavaScript)</li>
        </ul>
        </td>
      </tr>
      <tr>
         <td><b>C#</b></td>
         <td>.NET Cryptography classes (System.Security.Cryptography namespace)</td>
      </tr>
      <tr>
         <td><b>Ruby</b></td>
         <td>OpenSSL module (built into Ruby standard library)
        </td>
      </tr>
      <tr>
         <td><b>Go</b></td>
         <td>crypto package (part of the Go standard library)</td>
      </tr>
      <tr>
         <td><b>Rust</b></td>
         <td>RustCrypto (a collection of cryptographic algorithms implemented in pure Rust)</td>
      </tr>
      <tr>
         <td><b>PHP</b></td>
         <td>OpenSSL extension (commonly used for cryptographic operations in PHP)</td>
      </tr>
      <tr>
         <td><b>Swift</b></td>
         <td>CryptoKit framework (for Apple platforms)</td>
      </tr>
      <tr>
         <td><b>Kotlin</b></td>
         <td>kotlin-crypto library</td>
      </tr>
      <tr>
         <td><b>TypeScript</b></td>
         <td>TypeScript wrappers for Web Crypto API</td>
      </tr> 
      <tr>
         <td><b>Scala</b></td>
         <td>Scala Crypto library</td>
      </tr>
   </tbody>
</table>

</DropBox>

*5* Once the encryption is done, copy the encrypted data.

**Here is an example of how the encrypted data will appear:**

```json

"T3vY1#rF9VGhpcyBpcyBhbiBleGFtcGxlIG9mIGEgdmVyeSBsZW5ndGh5IGVuY3J5cHRlZCBzdHJpbmcgdXNpbmcgYmFzZTY0IGVuY29kaW5nLiBCYXNlNjQgaXMgdXNlZnVsIGZvciBlbmNvZGluZyBiaW5hcnkgZGF0YSBpbiBhbiBlZmZpY2llbnQgd2F5LCBhbmQgaXQgdGFrZXMgYWR2YW50YWdlIGG5j$9vZb@WqL5oJ8cR2bZ4dP9fE7qK1yM8sU3tV6nW9jX2mQ5rA4nF1vC6hL3kP7oD2D0a7@N3&sY4!uG5j$9vZb@WqL5D0a7@N3&sY4!uG5j$9vZb@WqL5oJ8cR2bZ4dP9fE7qK1oJ8cR2bZ4dP9fE7qK1"

```

*6* Return to your API tool, paste the generated value in `request` value field and initiate the [Trigger Request](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/) API.

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations.

### To view result,

*1* After completing the verification process, view the exact **response** with [Get Result](/verify/docs/DeveloperTools/API/Transaction/GetResult/v1/), and initiate the API call with the copied `auth_req_id` from the **API** request made prior to this.

*2* Below is the sample response:

```json
{
  "request_id": "c504a110-c984-4fad-84d7-40dd2c9b082c",
  "email_verified": "true",
  "user_id": "mark@gmail.com",
   "workflow": "verification",
  "email_verified_at": "1708757480",
  "email": "mark@gmail.com"
}
``` 