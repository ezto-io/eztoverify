const json = {
    result_delivery_mode: {
      desc: "The mode for delivering authentication results.",
      type: "string",
    },
    user_id: {
      desc: "It is a unique identifier of a user.",
      type: "string",
    },
    transaction_link:{
      desc: "The URL link to execute transaction.",
      type:"string",
    },
    success: {
      desc: "The status of the API request made.",
      type: "boolean",
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