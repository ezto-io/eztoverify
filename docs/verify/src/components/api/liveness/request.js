const json = {
  requestedVerification: {
    desc: "This value will be liveness",
    required: true,
    type: "string",
  },
  client_id: {
    desc: "Client id for your application.",
    required: true,
    type: "string",
  },
   metadata: {
    desc: "The metadata object obtained from the liveness SDK's onCompleted callback.",
    required: true,
    type: "string",
  },
  video_file: {
    desc: "The video file provided by the liveness SDK's onFileProcessed callback.",
    type: "file",
    required: true,
  },
  expected: {
    desc: "The config object used as input for the liveness SDK (the same config you pass when initializing the SDK).",
    type: "string",
    required: true,
  },
  hook_url: {
    desc: "Your custom URL endpoint that will receive the liveness result.",
    type: "string",
    required: true,
  },
  auth_token: {
    desc: "An authentication token for your custom hook URL, if required.",
    type: "string",
    required: false,
  },
};

export default json;
