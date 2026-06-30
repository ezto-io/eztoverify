import React, { useEffect, useState } from "react";
const DropBox = ({ name, dropBoxStatus ,dropBoxToggle, children, }) => (
    <div className="drop-box">
      <div className="drop-box-header" onClick={ () => dropBoxToggle((prev) =>!prev)} >
        <h4>{name}</h4>
        <button>
          <img alt="down arrow" src={`/verify/img/icons/${dropBoxStatus ? 'up-arrow.svg' : 'down-arrow.svg'}`} />
        </button>
      </div>
      <div className="drop-box-child">
      {dropBoxStatus && children}</div>
    </div>
  );

  export default DropBox;