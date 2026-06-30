import React from "react";
import "./docSection.css";
import Link from '@docusaurus/Link';

const DocsSection = ({ imgUrl, title, content, imgPosition, icon }) => {
  const processText = (text) => {
    return text.replaceAll("**", "");
  };

  const splitText = (text) => {
    return text.split(" ");
  };

  const isBold = (text) => {
    return text.startsWith("**");
  };

  const isLink = (text) => {
    return text.startsWith("[");
  };

  const textProcessing = (desc) => {
    return splitText(desc).map((text) => {
      return isBold(text) ? (
        <span className="doc-section-highlight">{processText(text)}</span>
      ) : isLink(text) ? (
        <Link to={text.split("(")[1]?.replace(")", "")}>
          {text.split("(")[0]?.replace("[", "")?.replace("]", " ")}
        </Link>
      ) : (
        `${text} `
      );
    });
  };

  return (
    <div
      className={`doc-section ${imgPosition === "left" && "doc-section-left"}`}
    >
      <div className="doc-section-content-holder">
        <div className="doc-section-header">
          <img className="doc-section-icon" src={icon} />
          <h2 className="doc-section-title">{title}</h2>
        </div>
        <p className="doc-section-content">{textProcessing(content)}</p>
      </div>
      <div className="doc-section-img-holder">
        <img src={imgUrl} alt={title} className="doc-section-img" />
      </div>
    </div>
  );
};

export default DocsSection;
