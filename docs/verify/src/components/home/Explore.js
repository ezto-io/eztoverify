import React from "react";
import "./Explore.css";
import { Card } from "./Card";

export const Explore = () => {
  const cardDetails = [
    {
      title: "Resources",
      imgUrl: "img/explore-icons/rocket.svg",
      desc: ["Explore setup guides, changelogs, and", "API docs."],
      url: "docs/EV/Overview",
    },
    {
      title: "Universal API's",
      imgUrl: "img/explore-icons/link.svg",
      desc: ["Make your transaction process, safe, ", "secure and seamless."],
      url: "docs/api/universal-apis/Send%20Request/",
    },
    {
      title: "Release notes",
      imgUrl: "img/explore-icons/release.svg",
      desc: [
        "Stay up to date on the latest features and  ",
        "enhancements in ezto verify.",
      ],
      url: "docs/changelogs/7.2.1/",
    },
    {
      title: "Applications",
      imgUrl: "img/explore-icons/app.svg",
      desc: [
        "Discover guides to create, configure and",
        "perform execution of an application.",
      ],
      url: "docs/EV/applications/Overview/",
    },
  ];

  return (
    <div className="explore-wrapper">
      <div className="explore">
        <h3 className="explore-title">Explore more</h3>
        <p className="explore-desc large-content">
          Discover the full potential of ezto verify to maximize platform
        </p>
        <p className="explore-desc large-content">
          advantages and facilitate effortless identity verification.
        </p>
        <p className="explore-desc small-content">
          Discover the full potential of ezto verify to maximize platform
        </p>
        <p className="explore-desc small-content">
          advantages and facilitate effortless identity verification.
        </p>
        <p className="explore-desc small-content">ensure the accuracy.</p>
        <div className="card-details">
          {cardDetails.map((card) => (
            <Card details={card} />
          ))}
        </div>
      </div>
    </div>  
  );
};
