import React from "react";

const ColapsableCodeBlocks = () => {
  return (
    <div class="collapsible-code">
      <button class="collapsible-button">Toggle Code</button>
      <pre>
        <code class="language-javascript">
          {`{
    "auth_req_id": "eyJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0..YM--W8m0wxvoy75PkMPzuw.",
    "result_delivery_mode": "hook@poll",
    "requestId": "47645e38-bb59-4378-b22d-66978175f653",
    "success": true,
        (The response may differ based on the authentication way and notification way sent in your application.)
    "userId": "b35b1dac-a985-4f18-baf2-96a190682114"
      }`}
        </code>
      </pre>
    </div>
  );
};

export default ColapsableCodeBlocks;
