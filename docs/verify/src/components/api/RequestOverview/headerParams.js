const json = {v0:{
  Content_Type: {
    desc: "",
    type: "string",
    required: false,
    example:"application/x-www-form-urlencoded"
  }},
  v1:{
    Content_Type: {
      desc: "",
      type: "string",
      required: false,
      example:"application/x-www-form-urlencoded"
    },
    "api-version": {
      desc: "",
      type: "string",
      required: true,
      example:"1"
    }, 
    "api-version": {
      desc: "",
      type: "string",
      required: true,
      example:"1"
    },
    authorization:{
      desc:"",
      type: "string",
      required: true,
      example: "Basic { app_id : app_secret }"
    }
  }
};

export default json;