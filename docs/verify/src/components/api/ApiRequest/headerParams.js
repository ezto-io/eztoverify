const json = {
  Content_Type: {
    desc: "",
    type: "string",
    required: false,
    example:"application/json"
  },
  "api-version": {
    desc: "",
    type: "string",
    required: true,
    example: "1",
  },
  authorization: {
    desc: "",
    type: "string",
    required: true,
    example: "Basic { app_id : app_secret }",
  },
};

export default json;
