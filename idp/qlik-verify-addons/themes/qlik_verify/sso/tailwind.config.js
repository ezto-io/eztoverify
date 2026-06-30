/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./*.{html,js,ftl}", "./components/*.{html,js,ftl}"],
  theme: {
    extend: {
      colors: { background: "#FAFAFA", primary:{950:"#4CA355", 100:"#E9F4EA"}},
    },
    screens: {
      '2xl': { max: "1535px" },
      // => @media (max-width: 1535px) { ... }

      xl: { max: "1435px" },
      // => @media (max-width: 1400px) { ... }

      lg: { max: "1023px" },
      // => @media (max-width: 1023px) { ... }

      md: { max: "767px" },
      // => @media (max-width: 767px) { ... }

      sm: { max: "639px" },
      // => @media (max-width: 639px) { ... }
    },
  },
  fontFamily: {
    sans: ["Inter", "sans-serif"],
  },
  plugins: [require("autoprefixer")],
};
