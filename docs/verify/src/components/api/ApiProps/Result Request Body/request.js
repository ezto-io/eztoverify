const json = {
    auth_req_id: {
      desc: "The request data received after you hit the **Send** **Request** **API** call should be provided.",
      required:true,
      type: "string",
    },
    app_id: {
      desc: "It is an unique identifier that comes with every application created under **ezto** **verify**.",
      type: "string",
      required:true,
    },
    app_secret: {
      desc: "Every application comes with an application secret upon being created, that can be rotated through the application settings.",
      type: "string",
      required:true,
    }
  };
  
  export default json;
  