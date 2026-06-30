const json = {
    request_id: {
      desc: "It is a unique id generated for each transaction when it gets triggered.",
      type: "string",
    },
    email_verified: {
      desc: "It is a template specific value i.e if verify email template is used the status of the template will be shown whether it is verified or not.",
      type: "string",
    },
    user_id: {
      desc: "It is a unique id of a user.",
      type: "string",
    },
    workflow: {
      desc: "It describes whether the flow is onboarding or verification.",
      type: "string",
    },
    email_verified_at:{
      desc:"It is a template specific value i.e if verify email template is used the timestamp at which the flow gets verified is shown here.",
      type:"string",
    },
    email:{
      desc:"It is a template specific value i.e if verify email template is used the value of the email entered by the user will be shown here.",
      type:"string"
    }
  };
  
  export default json;