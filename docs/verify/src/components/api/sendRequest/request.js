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
      child: {
        user: {
          desc: "This json will contain all the user details.",
          type: "json",
          child: {
            userId: {
              desc: "This is the ID of an user generated while validation or previously created.",
              type: "string",
            },
            email: {
              desc: "When the application **Notification** **Type** under **Web** is configured as **Email**, the user's email address is mandatory to be mentioned. To know more in detail, see [here.](/docs/EV/applications/applicationdetails/Configurations/overview)",
              type: "string",
            },
            phone: {
              desc: " When the application **Notification** **Type** under **Web** is configured as **SMS** or **Whatsapp**, the user's phone number is mandatory to be mentioned. To know more in detail, see [here.](/docs/EV/applications/applicationdetails/Configurations/overview)",
              type: "string",
            },
            phoneRegion: {
              desc: "When the application **Notification** **Type** under **Web** is configured as **SMS** or **Whatsapp**, then it is necessary to mention the phone region of the given phone number. To know more in detail, see [here.](/docs/EV/applications/applicationdetails/Configurations/overview)",
              type: "string",
            },
            metadata: {
              desc: "This type will consist of data's that is specific to the user attributes.",
              type: "json",
              child: {
                push_token: {
                  desc: "This key is mandatory if the **Authentication** **Way** is configured as **Mobile** under the application. To know more about the workflow, see [here.](/docs/EV/applications/applicationexecution/Mobile). Get the private key [here.](https://firebase.google.com/docs/cloud-messaging/auth-server)",
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
            metadata: {
              desc: "Under [Templates](/docs/EV/templates/Overview) the **Rules** are set up to trigger modules based on the details provided by the user. This does not appear in the user's view. To know more in detail, see [here.](/docs/EV/rules/Overview",
              type: "json",
              child: {
                ivr_sender: {
                  desc: "This is mandatory when the **Authentication** **Way** is configured as **IVR** under the application. It represents from where the transaction is triggered.",
                  type: "string",
                },
                ivr_purpose_message: {
                  desc: "If the **IVR** call is triggered, the message in this field will act as the transaction message.",
                  type: "string",
                },
                ivr_validation_message: {
                  desc: "This field will contain the message to be conveyed for the transaction validation.",
                  type: "string",
                },
                pin: {
                  desc: "It is the transaction validation pin that needs to be provided during the IVR transaction call. To know more, see [here.](/docs/EV/applications/applicationexecution/IVR)",
                  type: "string",
                },
              },
            },
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