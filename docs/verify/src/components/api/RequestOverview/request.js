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
      type:'json',
      desc: "This type consists of all the required data to be validated.",
      child: {
        user: {
          desc: "The user details will be retrieved.",
          type: "json",
          child: {
            metadata: {
              desc: "This type will consist of data's that is specific to the user attributes.",
              type: "json",
              child: {
                workflow: {
                  desc: "It describes the workflow to be executed, such as onboarding or verification.",
                  type: "string",
                },
              },
            },
          },
        },
        message: {
          desc: "If **Show** **Consent** is enabled for the application, then the message will take effect.",
          type: "json",
          child: {
            consent: {
              desc: "This type will work only when the **Show** **Consent** is enabled, this is an onboarding request to determine whether the triggered flow should be accepted or rejected.",
              type: "json",
              child: {
                type: {
                  desc: "If the consent type is **json**. The provided content will be in json format, else it will be displayed as plain text.",
                  type: "string",
                },
                title: {
                  desc: "The data given in this field will be displayed in the consent screen.",
                  type: "string",
                },
                message: {
                  desc: "This is the content that will be displayed in the consent screen. It can be a **json** or a **string**.",
                  type: "string / json",
                },
              },
            },
          },
        },
      },
    },
  };

  export default json;