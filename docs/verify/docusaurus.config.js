// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require("@kiwicopple/prism-react-renderer/themes/nightOwlLight");
const darkCodeTheme = require("@kiwicopple/prism-react-renderer/themes/palenight");
const simplePlantUML = require("@akebifiky/remark-simple-plantuml");
require('dotenv').config()

const config = {
  title: "Verify",
  tagline: "Secure by design and Flexible by Choice",
  url: "https://ezto.io",
  baseUrl: "/verify/",
  onBrokenLinks: "warn",
  onBrokenMarkdownLinks: "warn",
  favicon: "/img/rel-icon.png",
  customFields: {
    fonts: {
      myFont: ["Inter", "Serif"],
    },
    auth:{
      url:process.env.DOCS_AUTH_URL,
      realm:process.env.DOCS_AUTH_REALM,
      clientId:process.env.DOCS_AUTH_APP_ID
    }
  },
  scripts: [],

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: "verify", // Usually your GitHub org/user name.
  projectName: "verify", // Usually your repo name.

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: "en",
    locales: ["en"],
  },
  plugins: [],
  trailingSlash: true,
  presets: [
    [
      "classic",
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
          sitemap: {
            changefreq: 'weekly',
            priority: 0.5,
            ignorePatterns: ['/tags/**'],
            filename: 'sitemap.xml',
          },
        docs: {
          include: ['**/*.md', '**/*.mdx'],
          sidebarPath: require.resolve("./sidebars.js"),
          remarkPlugins: [simplePlantUML],
          rehypePlugins: [],
          docLayoutComponent: "@theme/DocPage",
          docItemComponent: "@theme/ApiItem",
        },
        blog: false,
        theme: {
          customCss: require.resolve("./src/css/custom.css"),
        },
      }),
    ],
  ],
  themes: [
    "docusaurus-theme-openapi-docs",
    [
      require.resolve("@easyops-cn/docusaurus-search-local"),
      {
        hashed: true,
        indexDocs: true,
        indexPages: true,
        docsRouteBasePath: ["/docs/changelogs", "/docs/EV/AdvancedSettings", "verify/docs/EV/AdvancedSettings", "verify/docs/EV/Audit", "verify/docs/EV/ControlIntegration", "verify/docs/EV/EncryptionAndDecryption", "/docs/DevelopersTools", "verify/docs/EV/GetStarted", "verify/docs/EV/StartAnIntegration", "verify/docs/EV/WorkflowModules"],
      },
    ],
  ],

  themeConfig: {
    docs: {
      sidebar: {
        autoCollapseCategories: true,
      },
    },
    navbar: {
      title: "",
      logo: {
        alt: "Verify Logo",
        src: "img/brand-name.svg",
        srcDark: "img/brand-name-dark.svg",
      },
      items: [
        {
          type: "doc",
          docId: "EV/Overview",
          position: "left",
          label: "Docs",
        },
        {
          label: "Developer Tools",
          to: "docs/DeveloperTools/Overview",
          activeBaseRegex: "docs/DeveloperTools",
        },
        {
          docId: "changelogs",
          position: "left",
          label: "Changelogs",
          to: "docs/changelogs/9.3.0",
        },
        // {
        //   type: "custom-profileNavbarItem",
        //   position: "right"
        // },
      ],
    },
    footer: {
      style: "dark",
      logo: {
        src: "/img/logo.svg",
        alt: "Verify Logo",
        href: "https://ezto.io",
        width: 160,
        height: 33,
      },
      links: [
        {
          title: "Docs",
          items: [
            {
              label: "Documentation",
              type: "doc",
              to: "/docs/EV/GetStarted/Branding/Themes",
            },
            {
              label: "Release Notes",
              type: "doc",
              to: "docs/changelogs/6.1.0",
            },
            {
              label: "API",
              type: "doc",
              to: "docs/api/universal-apis/Send%20Request",
            },
          ],
        },
        {
          title: "Community",
          items: [
            {
              label: "LinkedIn",
              href: "https://www.linkedin.com/company/ezto/",
            },
          ],
        },
        {
          title: "More",
          items: [
            {
              label: "GitHub",
              href: "https://github.com/ezto-io",
            },
            {
              label: "Blogs",
              href: "https://blogs.ezto.io/",
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Verify. All Rights Reserved.`,
    },
    prism: {
      theme: lightCodeTheme,
      darkTheme: darkCodeTheme,
      plugins: ["line-numbers", "show-language"],
      additionalLanguages: ["java", "python", "php", "csharp", "ruby"],
    },
    languageTabs: [
      {
        highlight: "bash",
        language: "curl",
        logoClass: "bash",
      },
      {
        highlight: "python",
        language: "python",
        logoClass: "python",
      },
      {
        highlight: "go",
        language: "go",
        logoClass: "go",
      },
      {
        highlight: "javascript",
        language: "nodejs",
        logoClass: "nodejs",
      },
      {
        highlight: "ruby",
        language: "ruby",
        logoClass: "ruby",
      },
      {
        highlight: "csharp",
        language: "csharp",
        logoClass: "csharp",
      },
      {
        highlight: "php",
        language: "php",
        logoClass: "php",
      },
      {
        highlight: "java",
        language: "java",
        logoClass: "java",
      },
    ],
  },
};

module.exports = config;
