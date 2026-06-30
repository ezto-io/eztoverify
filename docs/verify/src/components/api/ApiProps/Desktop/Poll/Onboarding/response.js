const json = {
    auth_req_id: {
      desc: "The authentication request identifier.",
      type: "string",
    },
    result_delivery_mode: {
      desc: "The mode for delivering authentication results.",
      type: "string",
    },
    user_id: {
      desc: "It is a unique identifier of a user.",
      type: "string",
    },
    success: {
      desc: "The status of the API request made.",
      type: "boolean",
    },
    transaction_link:{
      desc:"The URL link to execute transaction.",
      type:"string",
    },
    token_access_details:{
      desc:"This field specifies how to access result.",
      type:"json"
    },
    interval:{
      desc:"This field is received when hook notification set as poll.",
      type:"integer"
    },
    request_id: {
      desc: "It is a unique identifier for the request made.",
      type: "string",
    },
    expires_in: {
      desc: "The expiry time for transaction in milliseconds.",
      type: "integer"
    }
  };
  
  export default json;
  