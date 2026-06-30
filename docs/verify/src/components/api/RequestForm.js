import React, { useEffect, useState } from 'react'
import "./RequestForm.css"
import RequestProps from './RequestProps';

const RequestForm = ({way, flow, setRequestBody}) => {
  const emptyValue = { name:"", value:""};
  const [userData, setUserData] = useState([]);
  const [userMetadata, setUserMetadata] = useState([{...emptyValue}]);
  const [messageMetadata, setMessageMetadata] = useState([{...emptyValue}]);
  const [userOptions, setUserOptions] = useState(["userId","email","phone"]);
  const [selectedUserOption, setSelectedUserOptions] = useState("");

  useEffect(()=>{
    setRequestBody(prev => ({...prev, request: generateBody()}));
  },[userData, userMetadata, messageMetadata]);
  
  const addUserInput = () => {
    setUserData(prev => [...prev, {name: selectedUserOption, value:"", required: true}]);
    if (selectedUserOption === "phone") {
      setUserData(prev => [...prev, {name: "phoneRegion", value:"", required: true, disableDelete: true}]);
    }
    setUserOptions(prev => prev.filter(p => p!==selectedUserOption));
    setSelectedUserOptions("");
  }

  const addInput = (setData) => {
    setData(prev => [...prev, emptyValue]);
  }

  const generateBody = () => {
    const data = {
      user : {metadata: {...getObject(userMetadata), workflow: flow.toLowerCase()}, ...getObject(userData)}
    }
    if (way === "IVR") {
      data["message"] = {
        metadata: getObject(messageMetadata)
      }
    }
    return data;
  }

  const getObject = (array) => {
    const object = array.reduce(
      (obj, item) => Object.assign(obj, { [item.name]: item.name === 'phoneRegion' ? item.value.toLowerCase() : item.value }), {});
    return object;
  }

  return (
    <div className='request-form'>
      <p>
        <span>User Details:</span>
        {userOptions.length > 0 && 
          <>
            <select value={selectedUserOption} onChange={(e) => setSelectedUserOptions(e.target.value)}>
              <option>Select the key</option>
                {userOptions.map( key => (
                  <option>{key}</option>
                ))}
            </select>
            {selectedUserOption && 
            <button className='add-input-btn' onClick={addUserInput}>
              <svg className="prop-child-btn-icon"  xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" id="plus"><path fill="currentColor" d="M19,11H13V5a1,1,0,0,0-2,0v6H5a1,1,0,0,0,0,2h6v6a1,1,0,0,0,2,0V13h6a1,1,0,0,0,0-2Z"></path></svg>
            </button>
            }
          </>
        }
      </p>
      {
        userData.length > 0 &&
        (
          <div className='request-form-input-holder'>
            {
              userData.map((data, index)=>(
                <RequestProps setUserOptions={setUserOptions} key={index} index={index} metaData={userData} setData={setUserData}/>
              ))
            }
          </div>
        )
      }
      <p className={userData.length > 0 && `border-top`}>User Metadata</p>
      <div className='request-form-input-holder'>
        {
          userMetadata.map((data, index)=>(
            <RequestProps type="text" key={index} index={index} metaData={userMetadata} setData={setUserMetadata}/>
          ))
        }
        <button className='add-input-btn' onClick={()=>addInput(setUserMetadata)}>
          <svg className="prop-child-btn-icon"  xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" id="plus"><path fill="currentColor" d="M19,11H13V5a1,1,0,0,0-2,0v6H5a1,1,0,0,0,0,2h6v6a1,1,0,0,0,2,0V13h6a1,1,0,0,0,0-2Z"></path></svg>
        </button>
      </div>
      { way === "IVR" && (
        <>
          <p className='border-top'>Message Metadata</p>
          <div className='request-form-input-holder'>
            {
              messageMetadata.map((data, index)=>(
                <RequestProps type="text"  key={index} index={index} metaData={messageMetadata} setData={setMessageMetadata}/>
              ))
            }
            <button className='add-input-btn' onClick={()=>addInput(setMessageMetadata)}>
              <svg className="prop-child-btn-icon"  xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" id="plus"><path fill="currentColor" d="M19,11H13V5a1,1,0,0,0-2,0v6H5a1,1,0,0,0,0,2h6v6a1,1,0,0,0,2,0V13h6a1,1,0,0,0,0-2Z"></path></svg>
            </button>
          </div>
      </>)
      }
    </div>
  )
}

export default RequestForm
