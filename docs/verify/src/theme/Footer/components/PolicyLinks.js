import React from "react";

const PolicyLinks = ({ data }) => (
  <ul className="footer-link-holder">
    {data.map((link, index) => (
      <li key={index}>
        <a href={link.url}>{link.name}</a>
      </li>
    ))}
  </ul>
);

export default PolicyLinks;
