const structure = [
  {
    name: "Web",
    hasSubOptions: true,
    subOptions: [
      {
        name: "Link",
        options: [
          { name: "Ping", onboarding: true, verification: true },
          { name: "Poll", onboarding: true, verification: true },
        ],
      },
      {
        name: "Email",
        options: [
          { name: "Ping", onboarding: true, verification: true },
          { name: "Poll", onboarding: true, verification: true },
        ],
      },
      {
        name: "SMS",
        options: [
          { name: "Ping", onboarding: true, verification: true },
          { name: "Poll", onboarding: true, verification: true },
        ],
      },
      {
        name: "Whatsapp",
        options: [
          { name: "Ping", onboarding: true, verification: true },
          { name: "Poll", onboarding: true, verification: true },
        ],
      },
    ],
  },
  {
    name: "Desktop",
    hasSubOptions: false,
    options: [
      { name: "Desktop App", onboarding: true, verification: true },
      { name: "Ping", onboarding: true, verification: true },
      { name: "Poll", onboarding: true, verification: true },
    ],
  },
  {
    name: "IVR",
    hasSubOptions: false,
    options: [
      { name: "Ping", onboarding: true, verification: true },
      { name: "Poll", onboarding: true, verification: true },
    ],
  },
  {
    name: "Mobile",
    hasSubOptions: true,
    subOptions: [
      {
        name: "Push",
        options: [
          { name: "Mobile SDK", onboarding: true, verification: true },
          { name: "Ping", onboarding: true, verification: true },
          { name: "Poll", onboarding: true, verification: true },
        ],
      },
      {
        name: "QR",
        options: [
          { name: "Mobile SDK", onboarding: true, verification: true },
          { name: "Ping", onboarding: true, verification: true },
          { name: "Poll", onboarding: true, verification: true },
        ],
      },
    ],
  },
];

export default structure;
