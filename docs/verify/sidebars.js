// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  documentationSidebar: [
    "EV/Overview",
    {
      type: "category",
      label: "GET STARTED",
      className: "getting-started",
      collapsible: false,
      items: [
        "EV/GetStarted/RegisterWorkspace",
        {
          type: "category",
          label: "Workspace Settings",
          items: [
            {
              type: "category",
              label: "Personalization",
              items: [
                "EV/GetStarted/Branding/WorkspaceLogo",
                "EV/GetStarted/Branding/Branding",
                "EV/GetStarted/Branding/CustomSupport",
                {
                  type: "category",
                  label: "Workspace Attributes",
                  items: [
                    "EV/GetStarted/Branding/WorkspaceAttributes/FIDO",
                    "EV/GetStarted/Branding/WorkspaceAttributes/MobileApps",
                    "EV/GetStarted/Branding/WorkspaceAttributes/DesktopApps",
                    "EV/GetStarted/Branding/WorkspaceAttributes/TwilioSID",
                  ],
                },
              ],
            },
            "EV/GetStarted/SandboxLive",
          ],
        },
        {
          type: "category",
          label: "Workspace Admins",
          items: [
            "EV/GetStarted/WorkspaceAdmins/InviteCoadmin",
            "EV/GetStarted/WorkspaceAdmins/RemoveCoadmin",
            "EV/GetStarted/WorkspaceAdmins/ChangeWorkspaceOwner",
          ],
        },
        {
          type: "category",
          label: "Configurations",
          items: [
            {
              type: "category",
              label: "Email",
              items: [
                "EV/GetStarted/Configurations/Email/Overview",
                "EV/GetStarted/Configurations/Email/AmazonSES",
                "EV/GetStarted/Configurations/Email/Mandrill",
                "EV/GetStarted/Configurations/Email/MailGun",
                "EV/GetStarted/Configurations/Email/SendGrid",
                "EV/GetStarted/Configurations/Email/SMTP",
                "EV/GetStarted/Configurations/Email/SparkPost",
              ],
            },
            "EV/GetStarted/Configurations/SMS",
            "EV/GetStarted/Configurations/WhatsApp",
          ],
        },
        "EV/GetStarted/DNS",
      ],
    },
    {
      type: "category",
      label: "START AN INTEGRATION",
      collapsible: false,
      items: [
        {
          type: "category",
          label: "Workflow",
          items: [
            "EV/StartAnIntegration/SetupWorkflow",
            "EV/StartAnIntegration/SetupSubWorkflow",
            "EV/StartAnIntegration/CreateApplication",
            {
              type: "category",
              label: "Setup Verification",
              items: [
                "EV/StartAnIntegration/SetupVerification/Web",
                "EV/StartAnIntegration/SetupVerification/Mobile",
                "EV/StartAnIntegration/SetupVerification/IVR",
                "EV/StartAnIntegration/SetupVerification/Desktop",
              ],
            },
            {
              type: "category",
              label: "Setup Result Notification",
              items: [
                "EV/StartAnIntegration/SetupResultNotification/Hook",
                "EV/StartAnIntegration/SetupResultNotification/Queue",
              ],
            },
            {
              type: "category",
              label: "Setup Dead Letter",
              items: ["EV/StartAnIntegration/SetupDeadLetter/Hook"],
            },
            {
              type: "category",
              label: "How to Integrate",
              items: [
                "EV/StartAnIntegration/HowToIntegrate/Web",
                "EV/StartAnIntegration/HowToIntegrate/Mobile",
              ],
            },
          ],
        },
      ],
    },
    {
      type: "category",
      label: "CONTROL INTEGRATION",
      collapsible: false,
      items: [
        {
          type: "category",
          label: "Rules",
          items: [
            "EV/ControlIntegration/Rules/WhatareRules",
            "EV/ControlIntegration/Rules/CreateRule",
            "EV/ControlIntegration/Rules/RemoveRule",
          ],
        },

        "EV/ControlIntegration/ApplicationLevel",
        "EV/ControlIntegration/WorkflowsLevel",
      ],
    },
    {
      type: "category",
      label: "ENCRYPTION AND DECRYPTION",
      collapsible: false,
      items: [
        "EV/EncryptionAndDecryption/Overview",
        "EV/EncryptionAndDecryption/EncryptRequest",
        "EV/EncryptionAndDecryption/DecryptResponse",
      ],
    },
    {
      type: "category",
      label: "AUDIT",
      collapsible: false,
      items: [
        "EV/Audit/Logs",
        "EV/Audit/AdminLogs",
        "EV/Audit/History",
        "EV/Audit/GenerateAuditReport",
      ],
    },
    {
      type: "category",
      label: "WORKFLOW MODULES",
      collapsible: false,
      items: [
        "EV/WorkflowModules/EnvironmentVariables",
        "EV/WorkflowModules/HelperFunctions",
        "EV/WorkflowModules/ErrorCodes",
        "EV/WorkflowModules/SubWorkflow",
        "EV/WorkflowModules/Validator",
        "EV/WorkflowModules/APIRequest",
        {
          type: "category",
          label: "Device",
          items: [
            "EV/WorkflowModules/Device/Biometric",
            "EV/WorkflowModules/Device/DeviceRegister",
            "EV/WorkflowModules/Device/DeviceVerify",
            "EV/WorkflowModules/Device/NumberPrompt",
            {
              type: "doc",
              className: "sidebar-beta",
              label: "Proximity Verification",
              customProps: {
                badges: ["new", "green"],
                featured: true,
              },
              id: "EV/WorkflowModules/Device/ProximityVerification",
            },
            "EV/WorkflowModules/Device/SecureMessage",
            "EV/WorkflowModules/Device/WebtoDevice",
          ],
        },
        {
          type: "category",
          label: "FIDO/Passkeys",
          items: [
            "EV/WorkflowModules/Fido-passkeys/Register",
            "EV/WorkflowModules/Fido-passkeys/Verify",
          ],
        },
        "EV/WorkflowModules/GeoTagging",
        {
          type: "category",
          label: "Liveness",
          items: [
            "EV/WorkflowModules/Liveness/LivenessVerification",
            "EV/WorkflowModules/Liveness/SelfieRegistration",
            "EV/WorkflowModules/Liveness/SelfieVerification",
          ],
        },
        {
          type: "category",
          label: "Email",
          items: [
            "EV/WorkflowModules/Email/OTP",
          ],
        },
        {
          type: "category",
          label: "Mobile Number",
          items: [
            {
              type: "category",
              label: "Verification",
              items: ["EV/WorkflowModules/Mobile Number/Verification/OTP"],
            },
          ],
        },
        "EV/WorkflowModules/Whatsapp",
        "EV/WorkflowModules/IdDocuments/NameSimilarityCheck",
        "EV/WorkflowModules/DynamicForm",
        "EV/WorkflowModules/TermsandConditions",
        "EV/WorkflowModules/CustomMessage",
        "EV/WorkflowModules/CustomResponse",
      ],
    },
    {
      type: "category",
      label: "ADVANCE SETTINGS",
      collapsible: false,
      items: [
        "EV/AdvancedSettings/WorkspaceActivity",
        "EV/AdvancedSettings/CacheClearing",
        "EV/AdvancedSettings/Keys",
      ],
    },
  ],
  developerTools: [
    "DeveloperTools/Overview",
    "DeveloperTools/API/APIRequest",
    {
      type: "category",
      label: "Workflow API's",
      collapsible: false,
      items: [
        {
          type: "category",
          label: "Transaction",
          items: [
            {
              type: "category",
              label: "Trigger Request",
              items: [
                "DeveloperTools/API/Transaction/TriggerRequest/v0",
                "DeveloperTools/API/Transaction/TriggerRequest/v1",
              ],
            },
            {
              type: "category",
              label: "Get Result",
              items: [
                "DeveloperTools/API/Transaction/GetResult/v0",
                "DeveloperTools/API/Transaction/GetResult/v1",
              ],
            },
          ],
        },
        {
          type: "category",
          label: "Build your Integration",
          items: [
            {
              type: "category",
              label: "Trigger Request",
              items: [
                "DeveloperTools/API/BuildyourIntegration/TriggerRequest/v0",
                "DeveloperTools/API/BuildyourIntegration/TriggerRequest/v1",
              ],
            },
            {
              type: "category",
              label: "Get Result",
              items: [
                "DeveloperTools/API/BuildyourIntegration/GetResult/v0",
                "DeveloperTools/API/BuildyourIntegration/GetResult/v1",
              ],
            },
          ],
        },
        {
          type: "category",
          label: "Management",
          items: [
            "DeveloperTools/API/AccessKeys",
            {
              type: "category",
              label: "Devices",
              items: [
                "DeveloperTools/API/Management/Devices/ListAllDevices",
                "DeveloperTools/API/Management/Devices/DeleteaDevice",
                "DeveloperTools/API/Management/Devices/EnableaDevice",
                "DeveloperTools/API/Management/Devices/DisableaDevice",
                "DeveloperTools/API/Management/Devices/MarkasLastUsed",
              ],
            },
          ],
        },
        "DeveloperTools/API/TroubleShooting",
      ],
    },
    {
      type: "category",
      label: "MOBILE SDK's",
      collapsible: false,
      items: [
        {
          type: "category",
          label: "Flutter",
          items: [
            {
              type: "category",
              label: "Mobile",
              items: [
                "DeveloperTools/MobileSDK/Flutter/SetupAndroid",
                "DeveloperTools/MobileSDK/Flutter/SetupIOS",
                "DeveloperTools/MobileSDK/Flutter/Integration",
              ],
            },
            "DeveloperTools/MobileSDK/Flutter/Web",
          ],
        },
        {
          type: "category",
          label: "React Native",
          items: [
            "DeveloperTools/MobileSDK/ReactNative/SetupAndroid",
            "DeveloperTools/MobileSDK/ReactNative/SetupIOS",
            "DeveloperTools/MobileSDK/ReactNative/Integration",
          ],
        },
        {
          type: "category",
          label: "Mobile Native",
          items: [
            "DeveloperTools/MobileSDK/MobileNative/Android",
            "DeveloperTools/MobileSDK/MobileNative/IOS",
          ],
        },
      ],
    },
    {
      type: "category",
      label: "WEB SDK'S",
      collapsible: false,
      items: [
        "DeveloperTools/WebSDKs/Setup",
        {
          type: "category",
          label: "Integration",
          items: [
            "DeveloperTools/WebSDKs/Integration/VanillaJS",
            "DeveloperTools/WebSDKs/Integration/ReactJS",
            "DeveloperTools/WebSDKs/Integration/Angular",
            "DeveloperTools/WebSDKs/Integration/NextJS",
            "DeveloperTools/WebSDKs/Integration/VueJS",
            "DeveloperTools/WebSDKs/Integration/Svelte",
          ],
        },
      ],
    },
    {
      type: "category",
      label: "Liveness SDK",
      collapsible: false,
      items: [
        "DeveloperTools/LivenessSDK/setup",
        {
          type: "category",
          label: "Integration",
          items: [
            "DeveloperTools/LivenessSDK/Integration/ReactJS",
            "DeveloperTools/LivenessSDK/Integration/Angular",
            "DeveloperTools/LivenessSDK/Integration/NextJS",
            "DeveloperTools/LivenessSDK/Integration/VueJS",
            "DeveloperTools/LivenessSDK/Integration/Svelte",
          ],
        },
        "DeveloperTools/LivenessSDK/LivenessAPI",
      ],
    },
  ],
  changeLogSidebar: [
    {
      type: "category",
      label: "Changelogs",
      collapsible: false,
      items: [
        "changelogs/9.3.0",
        "changelogs/9.2.0",
        "changelogs/9.1.0",
        "changelogs/9.0.2",
        "changelogs/9.0.1",
        "changelogs/9.0.0",
        "changelogs/8.19.0",
        "changelogs/8.18.0",
        "changelogs/8.17.1",
        "changelogs/8.17.0",
        "changelogs/8.16.1",
        "changelogs/8.16.0",
        "changelogs/8.15.0",
        "changelogs/8.14.0",
        "changelogs/8.13.0",
        "changelogs/8.12.0",
        "changelogs/8.11.0",
        "changelogs/8.10.0",
        "changelogs/8.9.0",
        "changelogs/8.8.1",
        "changelogs/8.8.0",
        "changelogs/8.7.1",
        "changelogs/8.7.0",
        "changelogs/8.6.4",
        "changelogs/8.6.3",
        "changelogs/8.6.2",
        "changelogs/8.6.1",
        "changelogs/8.6.0",
        "changelogs/8.5.1",
        "changelogs/8.5.0",
        "changelogs/8.4.0",
        "changelogs/8.3.1",
        "changelogs/8.3.0",
        "changelogs/8.2.0",
        "changelogs/8.1.0",
        "changelogs/8.0.0",
        "changelogs/7.2.2",
        "changelogs/7.2.1",
        "changelogs/7.2.0",
        "changelogs/7.1.0",
        "changelogs/7.0.0",
        "changelogs/6.1.0",
        "changelogs/6.0.1",
        "changelogs/6.0.0",
        "changelogs/5.1.0",
        "changelogs/5.0.1",
        "changelogs/5.0.0",
        "changelogs/4.1.1",
        "changelogs/4.0.0",
      ],
    },
  ],

  // But you can create a sidebar manually
  /*
    tutorialSidebar: [
      'intro',
      'hello',
      {
        type: 'category',
        label: 'Tutorial',
        items: ['tutorial-basics/create-a-document'],
      },
    ],
     */
};

module.exports = sidebars;
