---
id: LivenessAPI
title: Liveness API
sidebar_label: Liveness API
hide_title: true
hide_table_of_contents: true
api: {
  "tags": [
    "Result > Overview"
  ],
  "security": [
    {
      "ApiKeyAuth": [],
      "basicAuth": []
    }
  ],
  "securitySchemes": {
    "basicAuth": {
      "type": "http",
      "scheme": "basic",
      "value": "1"
    },
    "ApiKeyAuth": {
      "type": "apiKey",
      "in": "header",
      "name": "api-version",
      "value": "1"
    }
  },
  "requestBody": {
    "content": {
      "multipart/form-data": {
        "schema": {
          "type": "object",
          "required": [
            "metadata",
            "video_file",
            "expected",
            "hook_url"
          ],
          "properties": {
            "metadata": {
              "type": "string",
              "description": "The metadata object obtained from the liveness SDK's onCompleted callback (as JSON string)."
            },
            "video_file": {
              "type": "string",
              "format": "binary",
              "description": "The video file provided by the liveness SDK's onFileProcessed callback."
            },
            "expected": {
              "type": "string",
              "description": "The config object used as input for the liveness SDK (as JSON string)."
            },
            "hook_url": {
              "type": "string",
              "format": "uri",
              "description": "Your custom URL endpoint that will receive the liveness result."
            },
            "auth_token": {
              "type": "string",
              "description": "An authentication token for your custom hook URL, if required.",
              "required": false
            }
          }
        }
      }
    }
  },
  "parameters": [
    {
      "name": "workspaceName",
      "in": "path",
      "schema": {
        "type": "string"
      },
      "required": true
    }
  ],
  "description": "Get Result URL",
  "method": "post",
  "path": "/auth/realms/{workspaceName}/protocol/openid-connect/transaction",
  "servers": [
    {
      "url": "https://{{base_url}}"
    }
  ],
  "postman": {
    "name": "Authentication Url",
    "description": {
      "type": "applicat"
    },
    "url": {
      "path": [
        "auth",
        "realms",
        "{{workspaceName}}",
        "protocol",
        "openid-connect",
        "transaction"
      ],
      "host": [
        "{{baseUrl}}"
      ]
    },
    "body": {
      "mode": "formdata",
      "formdata": [
        {
          "key": "metadata",
          "value": "{{metadata_json}}",
          "type": "text"
        },
        {
          "key": "video_file",
          "type": "file",
          "src": "{{video_file_path}}"
        },
        {
          "key": "expected",
          "value": "{{expected_config_json}}",
          "type": "text"
        },
        {
          "key": "hook_url",
          "value": "{{hook_url}}",
          "type": "text"
        },
        {
          "key": "auth_token",
          "value": "{{auth_token}}",
          "type": "text"
        }
      ]
    },
    "method": "post"
  }
}
sidebar_class_name: "get api-method"
---

import ParamsItem from "@theme/ParamsItem";
import TabItem from "@theme/TabItem";
import MimeTabs from "@theme/MimeTabs";
import ApiProps from "@site/src/components/docs/ApiProps";
import response from "@site/src/components/api/liveness/response";
import ApiParams from "@site/src/components/docs/ApiParams";
import baseUrl from "@site/src/components/api/liveness/baseUrl";
import headerParams from "@site/src/components/api/liveness/headerParams";
import request from "@site/src/components/api/liveness/request";
import pathParams from "@site/src/components/api/liveness/pathParams";
import CodeHighlighter from "@site/src/components/api/CodeHighlighter";

### Liveness API

The **ezto verify liveness** API enables to automate **liveness** detection and verification processes directly from their systems through secure, developer-friendly interfaces

<div className="api-builder-body-wrapper">
  <ApiParams className="holder" title="Base URL" json={baseUrl} />

  <ApiParams className="holder" title="Path Params" json={pathParams} />
  
  <ApiParams className="holder" title="Headers" json={headerParams} />
</div>

<h3 className="api-builder-page-header">Request Body</h3>

Below is the request body structure for the Liveness endpoint

<div className="api-builder-body-wrapper padding-bottom-1rem">

<ApiProps className="holder" title="" json={request} version={1}/>

```json title="Sample Request Body"
{
    "client_id": "{{client_id}}",
    "requestedVerification": "liveness",
    "video_file" : "{{video file}}",
    "metadata": {
        "blink": {
        "video": {
            "start": 2.93218,
            "end": 3.43198
        }
        },
        "movement": {
        "video": {
            "start": 7.5299700000000005,
            "end": 9.52958
        }
        },
        "fingerCount": {
        "video": {
            "start": 2.39886,
            "end": 3.39886
        }
        },
        "speech": {
        "video": {
            "start": 3.4653,
            "end": 8.49665
        }
        }
    },
    "expected": {
        "fingerCount": {
        "enabled": true,
        "expected": 5
        },
        "blink": {
        "enabled": true,
        "expected": 2
        },
        "speech": {
        "enabled": true,
        "expected": "Hat"
        },
        "movement": {
        "enabled": true
        }
    },
    "hook_url": "https://webhook.site"
}

```
</div>

<h3 className="api-builder-page-header">Response Body</h3>

Here is the response body and response sample for the `"requestedVerification":"liveness"`

<div className="api-builder-body-wrapper padding-bottom-1rem">
<ApiProps className="holder" title="" json={response}/>

<CodeHighlighter path="./mdx/API/Liveness/response.mdx" />
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
