const json = {
  request_id: {
    desc: "It is a unique id generated for each transaction when it gets triggered.",
    type: "string",
  },
  ivr_verified: {
    desc: "The status of the template will be shown whether it is verified or not.",
    type: "string",
  },
  key: {
    desc: "The key, you had set for verification during IVR Verification.",
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
  ivr_verified_at:{
    desc:"The timestamp at which the flow gets verified is shown here.",
    type:"string",
  },
};

export default json;