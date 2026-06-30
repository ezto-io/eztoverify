import React, { useEffect, useState } from "react";
const SelectBox = ({ setValue, value, options, label, placeholder }) => (
    <div className="select-box">
      <label className="select-box-label">{label}</label>
      <select onChange={(event) => setValue(event.target.value)} value={value} className="select-box-option">
        <option value="" selected disabled>
          {placeholder}
        </option>
        {options.map((option) => (
          <option key={option.name} value={option.name}>
            {option.name}
          </option>
        ))}
      </select>
    </div>
  );

  export default SelectBox