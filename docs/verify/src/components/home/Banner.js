import React from "react";
import "./Banner.css";

export const Banner = () => {
  return (
    <div className="banner">
      <div className="banner-background">
        <div className="line line1"></div>
        <div className="line line2"></div>
        <div className="line line3"></div>
        <div className="line line4"></div>
        <img src="img/banner-icons/icon1.svg" className="banner-icon1 floating" alt="" />
        <img src="img/banner-icons/icon2.svg" className="banner-icon2 floating"  alt="" />
        <img src="img/banner-icons/icon3.svg" className="banner-icon3 floating"  alt="" />
        <img src="img/banner-icons/icon4.svg" className="banner-icon4 floating"  alt="" />
        <div className="scroll-down">
          <div className="scroll-down-icon"></div>
          <label>Scroll Down</label>
        </div>
      </div>
      <div className="banner-content">
        <h3 className="banner-brand-name">
          <span>ezto verify</span>
        </h3>
        <h1 className="banner-title">Documentation</h1>
        <p className="banner-description">All information you need is one click away.</p>
        <a role="button" className="banner-btn" href="/verify/docs/EV/Overview/">
          Browse Docs <img src="img/icons/right-arrow.svg" alt="right-icon" />
        </a>
      </div>
    </div>
  );
};
