import React from "react";

const LinkHolder = ({ data }) => (
  <div>
    <h4 className="custom-footer-link-head">{data.title}</h4>
    <ul className="footer-link-holder">
      {data.links.map((link, index) => (
        <li key={index}>
          <a href={link.url}>{link.name}</a>
        </li>
      ))}
    </ul>
  </div>
);

export default LinkHolder;
