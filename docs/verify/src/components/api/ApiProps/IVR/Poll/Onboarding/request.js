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
    desc: "This type consists of all the required data to be validated.",
    type: "json",
    child: {
      user: {
        desc: "This json will contain all the user details.",
        type: "json",
        child: {
          phone: {
            desc: "The user's phone number is mandatory to be mentioned.",
            type: "string",
          },
          phoneRegion: {
            desc: "It is necessary to mention the phone region of the given phone number.",
            type: "string",
          },
          metadata: {
            desc: "This type will consist of data's that is specific to the user attributes.",
            type: "json",
            child: {
              key: {
                desc: "The key, you can set here is validated during IVR Verification.",
                type: "string",
              },
            },
          },
        },
      },
      message: {
        desc: "The message field contains data, which is sent to the user.",
        type: "json",
        child: {
          metadata: {
            desc: "This type will consist of data's that is specific to the IVR verification process.",
            type: "json",
            child: {
              ivr_sender: {
                desc: "This is mandatory when the **Authentication** **Way** is configured as **IVR** under the application. It represents from where the transaction is triggered. From whom the transaction is triggered.",
                type: "string",
              },
              ivr_purpose_message: {
                desc: "If the **IVR** call is triggered, the message in this field will act as the transaction message.",
                type: "string",
              },
              ivr_validation_message: {
                desc: "This field will contain the message to be conveyed for the transaction validation.",
                type: "string",
              }
            },
          },
        },
      },
    },
  },
};

export default json;