---
id: APIRequest
title: API
sidebar_label: API
hide_title: true
hide_table_of_contents: true
api:
  {
    "tags": ["Result > Overview"],
    "security": [
      {
        "ApiKeyAuth":[],
        "basicAuth": []
      }
    ],
    "securitySchemes": {
      basicAuth:{
        "type": "http",  
        "scheme": "basic",
        "value":"1"
      },
      "ApiKeyAuth": {
        "type": "apiKey",
        "in": "header",      
        "name": "api-version", 
        "value":"1"
      }
    },
    "requestBody": {
        "content":
          {
            "application/json":
              {
                "schema":
                  {
                    "type":"object",
                    "properties":
                      {
                        "client_id": { "type": "string" },
                        "requestedVerification": { "type": "string" },
                      },
                    "example":
                      {
                        "client_id": { "type": "string" },
                        "requestedVerification": { "type": "string" },
                      }
                    }
                  },
              },
        },
    "parameters":
      [
        {
          "name": "workspaceName",
          "in": "path",
          "schema": { "type": "string" },
          "required": true,
        },
      ],
    "description": "Get Result URL",
    "method": "post",
    "path": "/auth/realms/{workspaceName}/protocol/openid-connect/transaction",
    "servers": [{ "url": "https://{{base_url}}" }],
    "jsonRequestBodyExample":
      {
        "client_id": "{{client_id}}",
        "requestedVerification": "{{requestedVerification}}",
      },
    "postman":
      {
        "name": "Authentication Url",
        "description": { "type": "applicat" },
        "url":
          {
            "path":
              [
                "auth",
                "realms",
                "{{workspaceName}}",
                "protocol",
                "openid-connect",
                "transaction",
              ],
            "host": ["{{baseUrl}}"],
          },
       "body":
           {
            "mode": "json",
          },
        "method": "post",
      },
  }
sidebar_class_name: "get api-method"
---

import ParamsItem from "@theme/ParamsItem";
import TabItem from "@theme/TabItem";
import MimeTabs from "@theme/MimeTabs";
import ApiProps from "@site/src/components/docs/ApiProps";
import response from "@site/src/components/api/ApiRequest/response";
import ApiParams from "@site/src/components/docs/ApiParams";
import baseUrl from "@site/src/components/api/ApiRequest/baseUrl";
import headerParams from "@site/src/components/api/ApiRequest/headerParams";
import pathParams from "@site/src/components/api/ApiRequest/pathParams";
import CodeHighlighter from "@site/src/components/api/CodeHighlighter";

### API

The **ezto verify API** enables businesses to automate and initiate **user verifications** directly from their systems through secure, programmatic means.

<div className="api-builder-body-wrapper">
  <ApiParams className="holder" title="Base URL" json={baseUrl} />

  <ApiParams className="holder" title="Path Params" json={pathParams} />
  
  <ApiParams className="holder" title="Headers" json={headerParams} />
</div>

<h3 className="api-builder-page-header">Response Body</h3>

Here is the response body and response sample for the `"requestedVerification":"pan"`

<div className="api-builder-body-wrapper padding-bottom-1rem">
<ApiProps className="holder" title="" json={response}/>

<CodeHighlighter path="./mdx/API/API Request/response.mdx" />
</div>

<div id="http-status-code-summary">
<table>
   <thead>
      <tr>
         <th align="left" colSpan="3">HTTP STATUS CODE SUMMARY</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td align="left">200</td>
         <td>OK</td>
         <td align="left">Successfully done.</td>
      </tr>
      <tr>
         <td align="left">201</td>
         <td>OK</td>
         <td align="left">Successfully created.</td>
      </tr>
      <tr>
         <td align="left">400</td>
         <td>Bad Request</td>
         <td align="left">The request was unacceptable, often due to missing a required parameter.</td>
      </tr>
      <tr>
         <td align="left">401</td>
         <td>Unauthorized</td>
         <td align="left">Unauthorized access try with correct access.</td>
      </tr>
      <tr>
         <td align="left">402</td>
         <td>Request Failed</td>
         <td align="left">The parameters were valid but the request failed.</td>
      </tr>
      <tr>
         <td align="left">403</td>
         <td>Forbidden</td>
         <td align="left">The Application doesn't have permissions to perform the request.</td>
      </tr>
      <tr>
         <td align="left">404</td>
         <td>Not Found</td>
         <td align="left">The requested resource doesn't exist.</td>
      </tr>
      <tr>
         <td align="left">500</td>
         <td>Server Errors</td>
         <td align="left">Something went wrong on ezto verify's end. (These are rare.)</td>
      </tr>
      <tr>
         <td align="left">503</td>
         <td>Server Errors</td>
         <td align="left">Service temporarily unavailable.</td>
      </tr>
   </tbody>
</table>

</div>
