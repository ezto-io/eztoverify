import React from "react";
import "./Card.css";

export const Card = ({details}) => {
  return (
    <div className="card">
      <img className="card-img" src={details.imgUrl} alt="" />
      <div className="card-content">
        <h4 className="card-title">{details.title}</h4>
        {details.desc.map((text)=>(<p className="card-desc">{text}</p>))}
        <a className="card-link" href={details.url}>Learn More</a>
      </div>
    </div>
  );
};