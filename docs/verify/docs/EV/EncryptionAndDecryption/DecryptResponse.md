---
id: DecryptResponse
title: Decrypt Response
sidebar_label: Decrypt Response
--- 

import DropBox from "../../../src/components/docs/DropBox"

# Decrypt Response

Decrypting a response ensures that the data received in an encrypted format can be converted back into its original, readable form. Only authorized parties with the correct decryption key can decrypt the data, ensuring that it remains protected from unauthorized access.

To help visualize this process, the following flow diagram provides an overview of how decryption works.

![Flow](https://images.tango.us/workflows/3de8327a-d3cb-447a-9318-87fa12c3230b/steps/b80bd888-22cf-492b-b1e8-b7482eef6eb2/5a724464-7f56-4af1-87d1-b00f992f6b8c.png)

### Access Application

*1* Navigate to **Workspace**, and click on **Application**.

*2* This will take you to the list of all applications available in your dashboard.

*3* Click on the application you wish to view enable encryption. This will open the application **General** page.

![Application](https://images.tango.us/workflows/3de8327a-d3cb-447a-9318-87fa12c3230b/steps/f169adf4-5124-4e68-967e-4740ede8ec39/153b9939-3bd3-457b-9154-0f7eb801a898.png)

*4* Within the application's General page, click on the **Configurations** tab.

*5* Choose **Hook** for **Result Notification Way,** and select the response type as `Encrypted`.

![Encrypted](https://images.tango.us/workflows/3de8327a-d3cb-447a-9318-87fa12c3230b/steps/a64a9f16-15b0-44fa-9b4b-a4efbb98dcb5/f583defb-21ca-4b85-a7e8-a1c76a2aaf17.png)

<br/> 

### Decrypt Response {#decryptResponse}

*1* Choose  `Encrypted` and select the **AES** key which will be available in the dropdown. To know how to create a **Key,** see [here.](/verify/docs/EV/AdvancedSettings/Keys/)

![Secret Key](https://images.tango.us/workflows/3de8327a-d3cb-447a-9318-87fa12c3230b/steps/fa777666-cf7a-4845-8d9d-8b2afa09081e/17ab8080-155b-4fb8-9e42-52891593e769.png?mark-x=1283&mark-y=939&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz02MTgmaD00NiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)

> Make sure your required workflow is set before starting with the execution process in order to get the expected result.

*2* After choosing the **AES** key, the **Secret Key** will be displayed. Copy this key to your clipboard.

*3* To initiate the execution process, launch **Postman** or your preferred API tool and trigger the [Send Request](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/) API by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

<br/>

### To view result,

*1* After completing the verification process, view the exact **response** with [Get Result](/verify/docs/DeveloperTools/API/Transaction/GetResult/v1/), and input the copied `auth_req_id` (from the **API** request made prior to this) in the **request** value. 

*2* Proceed by initiating the API call, once the request is made, you'll receive the response in an  **encrypted** data. 

**Here is an example of how the encrypted data will appear:** 

```json
{
    "data": "T3vY1#rF9p%hK6m^L4n2wQ*X$8D0a7@N3&sY4!uG5j$9vZb@WqL5oJ8cR2bZ4dP9fE7qK1yM8sU3tV6nW9jX2mQ5rA4nF1vC6hL3kP7oD2D0a7@N3&sY4!uG5j$9vZb@WqL5D0a7@N3&sY4!uG5j$9vZb@WqL5oJ8cR2bZ4dP9fE7qK1oJ8cR2bZ4dP9fE7qK1"
}
```

*3* Now, with the available libraries, use the [above copied Secret Key](#decryptResponse) to decrypt the **data**. Ensure that the same **key size** is used as the selected **AES** key. (use the same padding as used in encryption)

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

*4* Upon performing the decryption of the data, the final response will be displayed.

*5* Below is a sample decrypted response:

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