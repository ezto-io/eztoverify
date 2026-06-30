---
id: ErrorCodes
title: Error Codes
sidebar_label: Error Codes
--- 

import DropBox from "../../../src/components/docs/DropBox"

This page provides error codes encountered in certain workflow modules. The **Generic Codes** section includes errors that apply to all the workflows mentioned on this page, typically related to system issues, request failures, and access problems. Below that, **Workflow Module Specific Error Codes** detail errors unique to individual verification processes.

## Generic Codes

These errors can occur in any of the workflows mentioned on this page. They typically relate to system issues, request failures, or access restrictions that may disrupt the process.

| Error Code | API Response             | Message                                                                           |
|------------|--------------------------|-----------------------------------------------------------------------------------|
| **EV0001** | 401 Unauthorized         | Something went wrong. Please try again, or contact support if the issue persists. |
| **EV0002** | 403 Forbidden            | Something went wrong. Please try again, or contact support if the issue persists. |
| **EV0003** | 404 Not Found            | Something went wrong. Please try again later or contact support for assistance.   |
| **EV0004** | 408 Request Timeout      | Something went wrong. Please try again later or contact support for assistance.   |
| **EV0005** | 409 Conflict             | Something went wrong. Please try again later or contact support for assistance.   |
| **EV0006** | 429 Too Many Requests    | Too many requests. Please try again later or contact support for assistance.      |
| **EV0007** | 500 Internal Server Error| Something went wrong. Please try again later or contact support for assistance.   |
| **EV0008** |           —              | User cancelled the flow.                                                          |  

## Workflow Module Specific Error Codes

These errors are tied to specific verification workflows. They indicate issues unique to that particular process, such as failed verification attempts or invalid inputs.

:::note

When a [Validator](/docs/EV/WorkflowModules/Validator) is used with any of the workflow modules listed below as the preceding one, you can access these error codes through the **Validator.** To retrieve an error code, use the following claim format in your Validator: **${workflow_module_name_error_codes}.**

:::

<DropBox title="API Request">

| Error Code | Message |  
|------------|---------|  
| **EVAPI0001** | Please check the target API service. |  
| **EVAPI0002** | Unauthorized. |  
| **EVAPI0003** | Forbidden. |  
| **EVAPI0004** | Too many requests. |  
| **EVAPI0005** | Not found. |  
| **EVAPI0006** | Request could not be completed due to a conflict. |  
| **EVAPI0007** | Something went wrong. | 

</DropBox>

<DropBox title="DigiLocker">

| Error Code | Message |  
|------------|---------|  
| **EVDIGI0001** | Consent denied. |  
| **EVDIGI0002** | User cancelled pull document confirmation request. |  
| **EVDIGI0003** | Unable to get pull documentation confirmation from the user. |  
| **EVDIGI0004** | User cancelled select state request. |  
| **EVDIGI0005** | Access not provided for the required document. |  
| **EVDIGI0006** | User cancelled pull document. |  
| **EVDIGI0007** | DigiLocker access code not found. | 
| **EVDIGI0008** | Full Name did not match with PAN Data| 
| **EVDIGI0009** | Document pull failed. | 
| **EVDIGI0010** | PAN number is invalid.|
| **EVDIGI0011** | Chasis number entered by you did not match. |
| **EVDIGI0012** | Requested document not found. |
| **EVDIGI0013** | Doc owner name did not match. |

</DropBox>

<DropBox title="Geo Tagging">

| Error Code | Message |  
|------------|---------|  
| **EVGEO0001** | Location access denied. |  

</DropBox>

<DropBox title="Name Similarity Check">

| Error Code | Message |  
|------------|---------|  
| **EVNS0001** | Comparison failed because one of the values is empty. |  
| **EVNS0002** | Comparison value contains characters other than alphabets. |

</DropBox>

<DropBox title="PAN Aadhaar Link Status">

| Error Code | Message |  
|------------|---------|  
| **EVPAL0001** | The PAN is linked to a different Aadhaar. |  
| **EVPAL0002** | The PAN does not exist. |  
| **EVPAL0003** | Enter a valid PAN. Aadhaar-PAN linking is only for individual taxpayers. |  
| **EVPAL0004** | The PAN is not linked to Aadhaar. |  
| **EVPAL0005** | Enter a valid 12-digit Aadhaar number. | 
| **EVPAL0006** | You have exceeded the limit to invoke the service on this network. Please, try after 24 hours.|

</DropBox>

<DropBox title="Selfie Registration">

| Error Code | Message |  
|------------|---------|  
| **EVSR0001** | Spoof detected. |  
| **EVSR0002** | File download failed. | 
| **EVSR0003** | Location access denied.|

</DropBox>

<DropBox title="Selfie Verification">

| Error Code | Message |  
|------------|---------|  
| **EVSV0001** | Low confidence. |  
| **EVSV0002** | Face did not match. | 
| **EVSV0003** | Location access denied. |

</DropBox>

<DropBox title="Verify Aadhaar with OTP">

| Error Code | Message |  
|------------|---------|  
| **EVAOTP0001** | We are unable to establish a connection with Aadhaar services. Please, try again later. |  
| **EVAOTP0002** | UIDAI responded with an unexpected response. Please try again. |  
| **EVAOTP0003** | Please try again by resending the OTP. |  

</DropBox>

<DropBox title="Verify Aadhaar with OCR">

| Error Code | Message |  
|------------|---------|  
| **EVOCRAD0001** | The provided Aadhaar image is invalid. |
| **EVOCRAD0002** | Invalid response for the uploaded Aadhaar front image. |
| **EVOCRAD0003** | OCR Aadhaar API limit exhausted. Try after some time. |
| **EVOCRAD0004** | The uploaded Aadhaar is invalid. |
| **EVOCRAD0005** | Invalid input. Please upload a valid Aadhaar image. |
| **EVOCRAD0006** | Aadhaar OCR processing limit exceeded. Please, try again later. |
| **EVOCRAD0007** | Something went wrong. Please, try again later. |
| **EVOCRAD0008** | Invalid response for the uploaded Aadhaar back image. |
| **EVOCRAD0009** | The Aadhaar number is masked in the provided image. |
| **EVOCRAD0010** | The Aadhaar number in front and back image does not match. |

</DropBox>

<DropBox title="Verify Bank">

| Error Code | Message |  
|------------|---------|  
| **EVBANK0001** | We are unable to establish a connection with Bank services. Please, try again later. |  
| **EVBANK0002** | Bank account does not exist. |  
| **EVBANK0003** | Invalid IFSC code. |  
| **EVBANK0004** | This account number is invalid. Please check and try again. |  
| **EVBANK0005** | Invalid bank details. |  
| **EVBANK0006** | We are unable to establish a connection with Bank Penny Drop services. Please, try again later. |

</DropBox>

<DropBox title="Verify Driving License with OCR">

| Error Code | Message |  
|------------|---------|  
| **EVOCRDL0001** | The provided Driving License image is invalid. |
| **EVOCRDL0002** | Invalid response for the uploaded Driving License. |
| **EVOCRDL0003** | OCR Driving License API limit exhausted. Try after some time. |
| **EVOCRDL0004** | The uploaded Driving License is invalid. |
| **EVOCRDL0005** | Invalid input. Please upload a valid Driving License image. |
| **EVOCRDL0006** | Driving License OCR processing limit exceeded. Please, try again later. |
| **EVOCRDL0007** | Something went wrong. Please, try again later. |

</DropBox>

<DropBox title="Verify PAN">

| Error Code | Message |  
|------------|---------|  
| **EVPAN001** | Unable to establish a connection with PAN services. |  
| **EVPAN002** | No records found for the given ID. |  
| **EVPAN003** | Unable to verify PAN. |  
| **EVPAN004** | An error occurred during PAN verification. | 

</DropBox>