const json = {
  status: {
    desc:"The status code of the API request made.",
    type:"integer",
  },
  body: {
    desc: "The response body of result API.",
    type:"json",
    child:{
      type:{
        desc:"The type of way that device is registered",
        type:"string"
      },
      createdDate:{
        desc:"The timestamp value when device is registered",
        type:"integer"
      },
      deviceid:{
        desc:"The unique id for device which is registered",
        type:"string"
      },
      deviceOSVersion:{
        desc:"The OS Version of registered device",
        type:"string"
      },
      devicePlatform:{
        desc:"The platform of registered device",
        type:"string"
      },
      deviceModel:{
        desc:"The model name of registered device",
        type:"string"
      },
      enabled:{
        desc:"The boolean value indicating whether device is enabled or not",
        type:"boolean"
      },
    }
  }
};

export default json;