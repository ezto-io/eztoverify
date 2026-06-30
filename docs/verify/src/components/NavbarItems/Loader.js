import React from "react";
import Lottie from "lottie-react";
import lottieJson from "../../../static/json/Loading.json";

const Loader = () => {
  return (
    <div className="loader">
      <div className="loader-logo"></div>
      {/* <img src="/verify/img/brand-name.svg" /> */}
      <Lottie
        width={100}
        animationData={lottieJson}
        className="loader-lottie"
      />
    </div>
  );
};

export default Loader;
