import React, { useState } from "react";
import "./ExpandableBox.css";

const ExpandableBox = ({ children }) => {
  const [expand, setExpand] = useState(false);
  
  return (
    <div className="expandable">
      <div className={expand ? "expand-more" : "expand-less"}>{children}</div>
      {expand ? (
        <button className="expandable-btn show-less" onClick={() => setExpand(false)}>show less <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 16 16" preserveAspectRatio="xMidYMid meet" class="G0 qc" style={{verticalAlign:"center"}}><path fill="currentColor" fill-rule="evenodd" d="M12.424 5.576a.6.6 0 0 1 0 .848l-4 4a.6.6 0 0 1-.848 0l-4-4a.6.6 0 0 1 .848-.848L8 9.15l3.576-3.575a.6.6 0 0 1 .848 0Z" clip-rule="evenodd"></path></svg></button>
      ) : (
        <button className="expandable-btn"  onClick={() => setExpand(true)}>show more <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 16 16" preserveAspectRatio="xMidYMid meet" class="G0 qc" style={{verticalAlign:"center"}}><path fill="currentColor" fill-rule="evenodd" d="M12.424 5.576a.6.6 0 0 1 0 .848l-4 4a.6.6 0 0 1-.848 0l-4-4a.6.6 0 0 1 .848-.848L8 9.15l3.576-3.575a.6.6 0 0 1 .848 0Z" clip-rule="evenodd"></path></svg></button>
      )}
    </div>
  );
};

export default ExpandableBox;
