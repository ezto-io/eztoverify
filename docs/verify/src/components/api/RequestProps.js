import React, { useState } from 'react'
import "./requestProps.css";

const RequestProps = ({index, setData, metaData, setUserOptions}) => {

    const deleteInput = () => {
      let localData = [...metaData];
      const deletedData = localData.splice(index, 1);
      if (deletedData[0]?.name === "phone") {
        localData = localData.filter( data => data.name !== "phoneRegion");
      }
      if (setUserOptions && deletedData[0]) {
        setUserOptions(prev => [...prev, deletedData[0]?.name])
      }
      setData(localData)
    }

    const handleChange = (value, name) => {
      setData(prev => {
        const temp = [...prev];
        temp[index][name] = value;
        return temp;
      })
    }

  return (
    <div className='request-props'>
      <input type="text" readOnly={metaData[index].required} placeholder='Enter key' value={metaData[index]["name"]} onChange={e => handleChange(e.target.value, "name")}/>
      <input type="text" placeholder='Enter value' value={metaData[index]["value"]} onChange={e => handleChange(e.target.value, "value")}/>
      <button disabled={metaData[index].disableDelete} className='input-delete-btn' onClick={deleteInput}>
       <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" id="remove"><path fill={metaData[index].disableDelete ? "#F39CA8" : "#f80929"} d="M12 0C5.383 0 0 5.383 0 12s5.383 12 12 12 12-5.383 12-12S18.617 0 12 0z" class="colore83628 svgShape"></path><path fill="#ffffff" d="M12 .25c6.575 0 11.926 5.316 11.994 11.875 0-.042.006-.083.006-.125 0-6.617-5.383-12-12-12S0 5.383 0 12c0 .042.006.083.006.125C.074 5.566 5.426.25 12 .25z" opacity=".2" class="colorffffff svgShape"></path><path d="M12 23.75C5.425 23.75.074 18.434.006 11.875c0 .042-.006.083-.006.125 0 6.617 5.383 12 12 12s12-5.383 12-12c0-.042-.006-.083-.006-.125C23.926 18.434 18.574 23.75 12 23.75z" opacity=".1" fill="#000000" class="color000000 svgShape"></path><path fill="#ffffff" d="M5 11h14v2H5z" class="colorffffff svgShape"></path><linearGradient id="a" x1="9.606" x2="18.694" y1="9.606" y2="18.694" gradientUnits="userSpaceOnUse"><stop offset="0" stop-opacity=".1"></stop><stop offset="1" stop-opacity="0"></stop></linearGradient><path fill="url(#a)" d="M19 11v2H5l10.481 10.481a12.047 12.047 0 0 0 8-8L19 11z"></path><linearGradient id="b" x1="1.125" x2="22.875" y1="6.929" y2="17.071" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#ffffff" stop-opacity=".2" class="stopColorffffff svgShape"></stop><stop offset="1" stop-color="#ffffff" stop-opacity="0" class="stopColorffffff svgShape"></stop></linearGradient><path fill="url(#b)" d="M12 0C5.383 0 0 5.383 0 12s5.383 12 12 12 12-5.383 12-12S18.617 0 12 0z"></path></svg>
      </button>
    </div>
  )
}

export default RequestProps
