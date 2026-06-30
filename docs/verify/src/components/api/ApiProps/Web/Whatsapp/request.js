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
      type: "json",
      desc: "This type consists of all the required data to be validated.",
      child: {
        user: {
          desc: "This json will contain all the user details.",
          type: "json",
          child: {
            phone: {
              desc: " When the application **Notification** **Type** under **Web** is configured as **SMS** or **Whatsapp**, the user's phone number is mandatory to be mentioned. To know more in detail, see [here.](/docs/EV/applications/applicationdetails/Configurations/overview)",
              type: "string",
            },
            phoneRegion: {
              desc: "When the application **Notification** **Type** under **Web** is configured as **SMS** or **Whatsapp**, then it is necessary to mention the phone region of the given phone number. To know more in detail, see [here.](/docs/EV/applications/applicationdetails/Configurations/overview)",
              type: "string",
            },
          },
        },
      },
    },
  };

  export default json;