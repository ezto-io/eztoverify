import React from "react";

const SocialIcons = ({ data }) => (
  <ul className="footer-social-icons">
    {data.map((icon, index) => (
      <li key={index}>
        <a href={icon.url}>
          <img src={icon.imgURL} alt={icon.alt} />
        </a>
      </li>
    ))}
  </ul>
);

export default SocialIcons;
