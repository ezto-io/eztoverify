import React from "react";
import "./Overview.css";

const Overview = ({img, title, firstLine, secondLine, url}) => {
  return (
    <a href={url} className="overview-card">
    <div className="overview-container">
      <img src={`/verify/img/icons/${img}.svg`} alt={title}/>
      <p className="overview-title">{title}</p>
      <div className="overview-desc">
        <p>{firstLine}</p>
        <p>{secondLine}</p>
      </div>
    </div>
    </a>
  );
};

export default Overview;
