import React from "react";
import "./About.css";
import Lottie from "lottie-react";
import lottieJson from "../../../static/json/verification.json";

export const About = () => {
  return (
    <div className="about">
      <h3 className="about-title small-view">
        <span>About </span>
        <span className="highlight">ezto verify</span>
      </h3>
      <div className="about-image">
        <Lottie className="lottie" animationData={lottieJson} />
      </div>
      <div className="about-content">
        <h3 className="about-title">
          <span>About </span>
          <span className="highlight">ezto verify</span>
        </h3>
        <p className="about-desc">
          ezto verify is a leading Identity Verification provider, employing
          cutting-edge techniques like Biometrics, FIDO, and Liveness check. The
          platform excels in the verification of user's Identity Documents and
          delivers results efficiently through one API.
        </p>
        <a href="https://ezto.io/verify" className="about-btn" role="button">
          Know More <img src="img/icons/right-arrow.svg" alt="right-icon" />
        </a>
      </div>
    </div>
  );
};
