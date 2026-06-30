import React from "react";
import "./Universal.css";

export const Universal = () => {
  return (
    <div className="universal">
      <h3 className="universal-title">ONE Universal API</h3>
      <p className="universal-desc large-content">
        ezto verify combines all required verification{" "}
      </p>
      <p className="universal-desc large-content">
        modules in one universal API call.
      </p>
      <p className="universal-desc small-content">
        ezto verify combines all required verification{" "}
      </p>
      <p className="universal-desc small-content">
        modules in one universal API call.
      </p>
      <p className="universal-desc small-content">ensure the accuracy.</p>
      <div className="universal-anim-holder">
        <div className="universal-anim-logo">
          <img src="img/universal-anim.svg" />
          <h4 className="universal-anim-text">One API Solution</h4>
        </div>
        <div className="universal-anim-send"></div>
        <div className="universal-anim-get"></div>
        <div className="universal-anim-btn-holder">
          <a
            href="/verify/docs/api/universal-apis/Send%20Request/"
            className="universal-anim-send-btn"
          >
            Send Request
          </a>
          <a
            href="/verify/docs/api/universal-apis/Get%20Result/"
            className="universal-anim-get-btn"
          >
            Get Result
          </a>
        </div>
      </div>
    </div>
  );
};
