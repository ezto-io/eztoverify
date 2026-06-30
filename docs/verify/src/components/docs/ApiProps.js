import React, { useState } from "react";
import "./ApiProps.css";
import Link from "@docusaurus/Link";

const processText = (text) => {
  return text.replaceAll("**", " ");
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

const ApiProps = ({ title, json, version }) => {
  const checkVersion = (key)=>{
    return version === 1 && (key === "app_id" || key === "app_secret")
  }
  return (
    <div className={`${title.trim().length == 0 && 'margin-0'}`}>
      <p className={`prop-title ${title.trim().length == 0 && 'padding-0'}`}>{title}</p>
      {Object.keys(json).map((key) => (
        checkVersion(key) ? (<></>) :
        (<Props
          name={key}
          desc={json[key].desc}
          child={json[key]?.child}
          required={json[key]?.required}
          type={json[key]?.type}
          parent={[]}
          isChild={false}
        />)
      ))}
    </div>
  );
};

const Props = ({ name, desc, child, type, required, parent, isChild }) => {
  const [showChildren, setShowChildren] = useState(false);
  const [isPropOpened, setPropOpened] = useState(isChild);

  return (
    <div className="prop propsHolder">
      <p className="prop-name" onClick={() => setPropOpened(!isPropOpened)}>
        {isPropOpened ?
          <svg width="10" height="6" viewBox="0 0 10 6" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 1L5 5L1 1" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg> : 
          <svg width="6" height="10" viewBox="0 0 6 10" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M1 1L5 5L1 9" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>
        }
        <code>
          {parent.length !== 0 && <span>{parent.join(".")}.</span>}
          {name}
        </code>
        <span className="prop-type">{type}</span>
        {required && <span className="prop-required">Required</span>}
      </p>
      {
        isPropOpened &&      
          <div>
            <p className="propDescription">
              {splitText(desc).map((text) => {
                return isBold(text) ? (
                  <span className="prop-name-bold">{processText(text)}</span>
                ) : isLink(text) ? (
                  <Link to={text.split("(")[1].replace(")", "")}>
                    {text.split("(")[0].replace("[", "").replace("]", " ")}
                  </Link>
                ) : (
                  `${text} `
                );
              })}
            </p>
            {child && (
              <div className="prop-child">
                <button
                  className="prop-child-btn"
                  onClick={() => setShowChildren((prev) => !prev)}
                >
                  <svg className={`prop-child-btn-icon ${showChildren && "tilt-prop-child-btn-icon" }`}  xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" id="plus"><path fill="currentColor" d="M19,11H13V5a1,1,0,0,0-2,0v6H5a1,1,0,0,0,0,2h6v6a1,1,0,0,0,2,0V13h6a1,1,0,0,0,0-2Z"></path></svg>
                  <span>
                    {showChildren ? "Hide child attributes" : "Show child attributes"}
                  </span>
                </button>
                {showChildren &&
                  Object.keys(child).map((key) => (
                    <Props
                      name={key}
                      desc={child[key].desc}
                      child={child[key]?.child}
                      required={child[key]?.required}
                      type={child[key]?.type}
                      parent={[...parent, name]}
                      isChild={true}
                    />
                  ))}
              </div>
            )}
          </div>
      }
    </div>
  );
};
export default ApiProps;
