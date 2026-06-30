import React from "react";
import Link from "@docusaurus/Link";

const APIVersion = ({ version, link1, link2, title }) => {
  return (
    <div className="version-selectbox">
      <h3>{title}</h3>
      <div className="version-slider">
        <Link
          className={version === 0 ? "version-slider-active" : ""}
          to={version === 1 && link1}
        >
          V0
        </Link>
        <Link
          className={version === 1 ? "version-slider-active" : ""}
          to={version === 0 && link2}
        >
          V1
        </Link>
      </div>
    </div>
  );
};

export default APIVersion;
