module.exports = {
  content: ["./*.{html,js,ftl}"],
  theme: {
    extend: {
      colors: {
        primary: "#1c64eb",
        textPrimary: "#3a4549",
        whisperGray: "#EDEDED",
        solitudeGray: "#F2F4F8",
        platinumGray: "#797979",
        seperatorGray: "#E6E8EB",
        concreteGray: "#F2F2F2",
        lavenderBlue: "#D3D9E6",
        raisinBlack: "#202020",
        graniteGray: "#666666",


      },
      width: {
        "43prct": "43%",
        "75prct": "75%",
        1.125 : "1.125rem",
      },
      height: {
        "90vh" : "90vh",
        "30prct" : "30%",
      },
      boxShadow: {
        "modalShadow": "rgba(100, 100, 111, 0.2) 0px 7px 29px 0px;"
      },
      borderRadius: {
        1.375 : "1.375rem",
        1.125 : "1.125rem",
      },
      padding: {
        1.125: "1.125rem",
      },
    },
    fontFamily: {
      "sans": ["Inter", "sans-serif"]
    },
    screens: {
      "2xl": { "max": "1535px" },
      // => @media (max-width: 1535px) { ... }

      "xl": { "max": "1400px" },
      // => @media (max-width: 1400px) { ... }

      "lg": { "max": "1023px" },
      // => @media (max-width: 1023px) { ... }

      "md": { "max": "767px" },
      // => @media (max-width: 767px) { ... }

      "sm": { "max": "639px" }
      // => @media (max-width: 639px) { ... }
    }
  },
  plugins: [
    require("tailwind-scrollbar-hide"),
    require('autoprefixer')
  ]
};
