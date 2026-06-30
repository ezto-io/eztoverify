---
id: Keys
title: Keys
sidebar_label: Keys
--- 

import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';
import DropBox from "../../../src/components/docs/DropBox"

# Keys

Keys serve as the secure communication and information security, functioning as essential digital elements. Their critical role involves encrypting and decrypting data, confirming identities, and upholding the security triad of confidentiality, integrity, and authenticity for information.

There are different kinds of key, widely used to for encryption algorithm, for increasing the security efficiency of protecting data.

The **Keys** that are available in **ezto verify** are,

![Key](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/58837e7c-cbda-46b2-bc78-6d5e21f9dc84/48c15043-1feb-4b2c-af74-16f268eb22fe.png?mark-x=1010&mark-y=152&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz0xODQmaD0zMTEmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


<Tabs>

<TabItem value="aes-generated" label="aes-generated" default>


* **The Advanced Encryption Standard (AES)** is a highly symmetric encryption algorithm renowned for its security and efficient data protection capabilities.
* When data or content is designated as "AES-generated," it indicates that AES encryption or encryption-related methods have been employed to safeguard it.

![aes-generated](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/02d6c528-a4bc-4642-bed8-0f4029213f68/4832d72c-3c59-4c4e-bf84-c9c867e69ab6.png)

### Manage \`aes-generated\`

<table>
   <thead>
      <tr>
         <th width="219">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Console Display Name</td>
         <td>This field represents the display name of the provider when linked in the admin console.</td>
      </tr>
      <tr>
         <td>Priority</td>
         <td>Priority level assigned to the provider.</td>
      </tr>
      <tr>
         <td>Enable</td>
         <td>Indicates whether the keys are enabled or not.</td>
      </tr>
      <tr>
         <td>Active</td>
         <td>Indicates whether the keys can be used for signing.</td>
      </tr>
      <tr>
         <td>AES Key Size</td>
         <td>This field specifies the size in bytes for the generated AES Key. A size of 16 corresponds to AES-128, a size of 24 corresponds to AES-192, and a size of 32 corresponds to AES-256. <strong>Note</strong>: Some JDK implementations may not allow keys larger than 128 bits; caution is advised.</td>
      </tr>
   </tbody>
</table>

* After providing the above configurations, click on the `SAVE` button.


</TabItem>
<TabItem value="ecdsa-generated" label="ecdsa-generated">

* It commonly denotes something that has been produced or formed through the utilization of the **Elliptic Curve Digital Signature Algorithm (ECDSA)**. It is  widely adopted cryptographic technique employed for generating digital signatures and confirming their legitimacy.

![ecdsa-generated](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/5dcfff23-3a72-4a1d-b9a9-f1307ea69aa7/1ea2013a-398d-428a-8763-5aa89240ffa6.png)

### Manage \`**ecdsa-generated**\`

<table><thead><tr><th width="219">Name</th><th>Description</th></tr></thead><tbody><tr><td>Console Display Name</td><td>This field represents the display name of the provider when linked in the admin console.</td></tr><tr><td>Priority</td><td>Priority level assigned to the provider.</td></tr><tr><td>Enable</td><td>Indicates whether the keys are enabled or not.</td></tr><tr><td>Active</td><td>Indicates whether the keys can be used for signing.</td></tr><tr><td>Elliptic Curve</td><td>Mention the <strong>Elliptic Curve</strong> employed in ECDSA.</td></tr></tbody></table>

* After providing the above configurations, click on the `SAVE` button.


</TabItem>
<TabItem value="hmac-generated" label="hmac-generated">

It generally denotes an entity or data that has been generated or formed using the **Hash-based Message Authentication Code (HMAC)** algorithm.

![hmac-generated](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/95977b65-ee63-4a04-90cb-1c8c89e538dd/9bbc17ec-b98d-4511-ab6f-3af632932eac.png)

### Manage \`hmac**-generated**\`

<table>
   <thead>
      <tr>
         <th width="219">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Console Display Name</td>
         <td>This field represents the display name of the provider when linked in the admin console.</td>
      </tr>
      <tr>
         <td>Priority</td>
         <td>Priority level assigned to the provider.</td>
      </tr>
      <tr>
         <td>Enable</td>
         <td>Indicates whether the keys are enabled or not.</td>
      </tr>
      <tr>
         <td>Active</td>
         <td>Indicates whether the keys can be used for signing.</td>
      </tr>
      <tr>
         <td>Secret Size</td>
         <td>The size in bytes for the generated secret.</td>
      </tr>
      <tr>
         <td>Algorithm</td>
         <td>The intended algorithm for the key.</td>
      </tr>
   </tbody>
</table>

* After providing the above configurations, click on the `SAVE` button.

</TabItem>
<TabItem value="java-keystore" label="java-keystore">

* A **java keyStore**, commonly known as "JKS," serves as a storage facility for cryptographic keys, certificates, and other confidential security-related information.
*   It forms an integral part of Java's security framework, finding extensive applications in securing communication, authenticating users and systems, and maintaining data integrity.

![java-keystore](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/a850e108-785b-483c-9a26-de047d31e22a/1437243f-387d-424c-a164-4b6afd7cde0e.png)

### Manage \`java-keystore\`

<table>
   <thead>
      <tr>
         <th width="226">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Console Display Name</td>
         <td>This field represents the display name of the provider when linked in the admin console.</td>
      </tr>
      <tr>
         <td>Priority</td>
         <td>Priority level assigned to the provider.</td>
      </tr>
      <tr>
         <td>Enable</td>
         <td>Indicates whether the keys are enabled or not.</td>
      </tr>
      <tr>
         <td>Active</td>
         <td>Indicates whether the keys can be used for signing.</td>
      </tr>
      <tr>
         <td>Algorithm</td>
         <td>The intended algorithm for the key.</td>
      </tr>
      <tr>
         <td>Keystore</td>
         <td>Path to the keys file.</td>
      </tr>
      <tr>
         <td>Keystore Password</td>
         <td>Password for accessing the keys.</td>
      </tr>
      <tr>
         <td>Key Alias</td>
         <td>Alias for the private key.</td>
      </tr>
      <tr>
         <td>Key Password</td>
         <td>Password for the private key.</td>
      </tr>
      <tr>
         <td>Key Use</td>
         <td>Specifies whether the key should be used for signing or encryption.</td>
      </tr>
   </tbody>
</table>

* After providing the above configurations, click on the `SAVE` button.


</TabItem>
<TabItem value="rsa" label="rsa">

* **RSA**, stands as a prominent public-key encryption and digital signature algorithm within the realm of cryptography.
* **RSA** persists as an integral cornerstone of contemporary secure communication and information security solutions.

![rsa](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/f87fe676-f51c-4434-b277-81fa32e5c73d/73ee5246-1c0f-49b3-a2c5-6f75581d7948.png)

### Manage \`rsa\`

<table>
   <thead>
      <tr>
         <th width="226">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Console Display Name</td>
         <td>This field represents the display name of the provider when linked in the admin console.</td>
      </tr>
      <tr>
         <td>Priority</td>
         <td>Priority level assigned to the provider.</td>
      </tr>
      <tr>
         <td>Enable</td>
         <td>Indicates whether the keys are enabled or not.</td>
      </tr>
      <tr>
         <td>Active</td>
         <td>Indicates whether the keys can be used for signing.</td>
      </tr>
      <tr>
         <td>Private RSA Key</td>
         <td>The Private RSA Key encoded in PEM format.</td>
      </tr>
      <tr>
         <td>X509 Certificate</td>
         <td>The X509 Certificate encoded in PEM format.</td>
      </tr>
      <tr>
         <td>Algorithm</td>
         <td>The intended algorithm for the key.</td>
      </tr>
   </tbody>
</table>

* After providing the above configurations, click on the `SAVE` button.

</TabItem>
<TabItem value="rsa-enc" label="rsa-enc">

* It commonly denotes RSA encryption, a cryptographic technique that employs the RSA algorithm to safeguard data by transforming it into an indecipherable format, only accessible with the corresponding private key.

![rsa-enc](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/d595f5c5-aff1-42a5-95b9-ac0821c613d7/2ca024da-00d5-4404-bdb7-489afa536179.png)

### Manage  rsa-enc

<table>
   <thead>
      <tr>
         <th width="237">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Console Display Name</td>
         <td>This field represents the display name of the provider when linked in the admin console.</td>
      </tr>
      <tr>
         <td>Priority</td>
         <td>Indicates the priority level assigned to the provider.</td>
      </tr>
      <tr>
         <td>Enable</td>
         <td>Specify whether the keys are enabled or not.</td>
      </tr>
      <tr>
         <td>Active</td>
         <td>Specify whether the keys can be used for signing.</td>
      </tr>
      <tr>
         <td>Private RSA Key</td>
         <td>The <strong>Private RSA Key</strong> encoded in PEM format.</td>
      </tr>
      <tr>
         <td>X509 Certificate</td>
         <td>The X509 Certificate encoded in PEM format.</td>
      </tr>
      <tr>
         <td>Algorithm</td>
         <td>Denotes the intended algorithm for the key.</td>
      </tr>
   </tbody>
</table>

* After providing the above configurations, click on the `SAVE` button.

</TabItem>
<TabItem value="rsa-enc-generated" label="rsa-enc-generated">

* It generally describes an entity or data that has been generated or produced using RSA encryption.
* RSA encryption relies on the RSA algorithm to protect data by transforming it into an unreadable format, that can only be deciphered using the matching private key.

![rsa-enc-generated](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/44a38507-f55a-4e01-86d3-bd6e87819f53/46c787eb-4983-4e05-af19-989bf86e1f3d.png)

<table>
   <thead>
      <tr>
         <th width="245">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Console Display Name</td>
         <td>This field represents the display name of the provider when linked in the admin console.</td>
      </tr>
      <tr>
         <td>Priority</td>
         <td>Indicates the priority level assigned to the provider.</td>
      </tr>
      <tr>
         <td>Enable</td>
         <td>Specify whether the keys are enabled or not.</td>
      </tr>
      <tr>
         <td>Active</td>
         <td>Specify whether the keys can be used for signing.</td>
      </tr>
      <tr>
         <td>Key Size</td>
         <td>The size in bytes for the generated secret.</td>
      </tr>
      <tr>
         <td>Algorithm</td>
         <td>Denotes the intended algorithm for the key.</td>
      </tr>
   </tbody>
</table>

* After providing the above configurations, click on the `SAVE` button.

</TabItem>
<TabItem value="rsa-generated" label="rsa-generated">

* It generally refers to something that has been produced or created using the RSA encryption algorithm.
* RSA encryption employs mathematical techniques to secure data by converting it into an encrypted format that can only be deciphered with the associated private key.

![rsa-generated](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/984c51bf-1d6f-4ba9-8b6e-596de51a54ce/2a89ce34-b565-4b74-bd00-ff5b974dd99c.png)

<table>
   <thead>
      <tr>
         <th width="217">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Console Display Name</td>
         <td>This field specifies the display name of the provider when linked in the admin console.</td>
      </tr>
      <tr>
         <td>Priority</td>
         <td>Indicates the priority assigned to the provider.</td>
      </tr>
      <tr>
         <td>Enable</td>
         <td>Set this option to determine if the keys are enabled.</td>
      </tr>
      <tr>
         <td>Active</td>
         <td>Set this option to determine if the keys can be used for signing.</td>
      </tr>
      <tr>
         <td>Key Size</td>
         <td>Indicates the size in bytes of the generated secret.</td>
      </tr>
      <tr>
         <td>Algorithm</td>
         <td>Denotes the intended algorithm for the key.</td>
      </tr>
   </tbody>
</table>

* After providing the above configurations, click on the `SAVE` button.

</TabItem>
</Tabs>

<br/>

#### For further assistance or related topics, you may explore the following resources:


<DropBox title="Create Key">

**Keys** serve as the secure communication and information security elements, functioning as essential digital elements.

*1* Navigate to **Workspace**, and click on **Security Defenses.**

*2* Click on the **Keys** tab.

*3* To add a new key, click on the `+ ADD KEYSTORE` button.

![Add Key](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/4f4a0e08-6499-4a6b-920c-41bd94acad75/a4c1431e-f291-41cd-a0e1-fd99697a0f1e.png)

*4* There are different kinds of key, widely used to for encryption algorithm, for increasing the security efficiency of protecting data. The **Keys** that are available in **ezto verify** can be referred \[above.]link

*5* Choose the **Key** type you wish to generate. For instance, **aes-generated** is chosen here.

*6* You will be moved into the specific key's configuration page.

![Configurations](https://images.tango.us/workflows/4cf304b0-db6a-4bd6-acba-41198da33869/steps/658819bc-b980-4023-bd0c-2e86c33294ae/cee7f976-71f1-47c0-a202-4b51795ac5bc.png)

*7* Provide the necessary configs, and click on the  `SAVE` button**.**

</DropBox>