const json = {
  app_id: {
    desc: "It is a unique identifier that comes with every application created under ezto verify.",
    type: "string",
    required: true,
  },
  app_secret: {
    desc: "Every application comes with an application secret upon being created, that can be rotated through the application.",
    type: "string",
    required: true,
  },
  request: {
    type:"json",
    desc: "This type consists of all the required data to be validated.",
    child: {
      user: {
        desc: "This json will contain all the user details.",
        type: "json",
        child: {
          email: {
            desc: "The user's email address to be mentioned.",
            type: "string",
          },
          metadata: {
            desc: "This type will consist of data's that is specific to the user attributes.",
            type: "json",
            child: {
              push_type:{
                desc: "Push type that needs to be triggered. Accepted values fcm, hms, desktop.",
                type: "string",
              },
              push_token: {
                desc: "Push token is not required when our authenticator is used, it's only required when your own app is used.",
                type: "string",
              },
            },
          },
        },
      },
    },
  },
};

export default json;