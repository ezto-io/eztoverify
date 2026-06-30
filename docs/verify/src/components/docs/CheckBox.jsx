import React, { useEffect, useState } from "react";
import "./CheckBox.css"
import ExecutionEnvironment from '@docusaurus/ExecutionEnvironment';


const CheckBox = ({ name }) => {
  let value = false;

  if (ExecutionEnvironment.canUseDOM) {
    value = localStorage.getItem(name) === 'true' ? true : false
  }

  const [checked, setChecked] = useState(value);

  useEffect(() => {
    localStorage.setItem(name, checked);
  }, [checked]);
  return <input className="checkbox" type="checkbox" onChange={()=>setChecked(prev => !prev)} name={name} checked={checked}/>;
};

export default CheckBox;
