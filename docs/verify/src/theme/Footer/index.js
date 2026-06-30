import React, { useEffect } from "react";
import "./index.css";
import config from "./config";
import SocialIcons from "./components/SocialIcons";
import PolicyLinks from "./components/PolicyLinks";
import LinkHolder from "./components/LinkHolder";
import { useLocation } from '@docusaurus/router';

export default function FooterWrapper() {
  const { links, socialLinks, policyLinks } = config;
  const location = useLocation();

  useEffect(() => {
    const images = document.getElementsByTagName('img');
    for (let i = 0; i < images.length; i++) {
      if (images[i].closest('p') == images[i].parentNode) {
        images[i].classList.add("zoom-out")
        images[i].addEventListener("click", function (event) {
          const div = document.createElement("div");
          div.classList.add("img-holder");
          div.onclick = () => {
            document
            .getElementById("__docusaurus")
            .removeChild(div);
          };
          const img = document.createElement("img");
          img.src = event.target.getAttribute("src");
          img.classList.add("popup-img");
          div.appendChild(img);
          document
            .getElementById("__docusaurus")
            .appendChild(div);
        });
      }
    }
  }, [location]);

  useEffect(() => {
    const statusCodeSummaryTable = document.getElementById('http-status-code-summary') ? document.getElementById('http-status-code-summary') : null;
    const rightSideSectionElement = document.getElementsByClassName("col--5") ? document.getElementsByClassName("col--5")[0] : null;
    if(rightSideSectionElement && statusCodeSummaryTable && location.pathname.includes('DeveloperTools/API')){
      setTimeout(() => rightSideSectionElement.lastChild.appendChild(statusCodeSummaryTable), 1 ); 
    }
  }, [location]);

  return (
    <footer className="custom-footer">
      <div className="custom-footer-wrapper">
        <div className="custom-footer-head">
          <div>
            <a href="https://ezto.io/">
              <img
                className="custom-footer-logo"
                src="/verify/img/white-logo.svg"
                alt="ezto logo"
              />
            </a>
          </div>
          <div className="custom-footer-links">
            {links.map((link, index) => (
              <LinkHolder key={index} data={link} />
            ))}
          </div>
        </div>
        <div className="custom-footer-bottom">
          <div className="footer-bottom-link-holder">
            <PolicyLinks data={policyLinks} />
            <SocialIcons data={socialLinks} />
          </div>
          <p className="custom-footer-copyright">
            ©{new Date().getFullYear()} Copyright ezto, Trademark Registered.
            All Rights Reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
