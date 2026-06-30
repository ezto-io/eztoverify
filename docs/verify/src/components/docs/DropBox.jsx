import React, { useState } from 'react'

function DropBox({ title, children, optional }) {
    const [isContentVisible, setIsContentVisible] = useState(false)
    return (
        <div className="dropbox-container">
            <div className='dropbox'  onClick={() => setIsContentVisible((prev) => !prev)}>
                <div className='dropbox-icon'>
                    {isContentVisible
                    ? <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 16 16" preserveAspectRatio="xMidYMid meet" class="G0 qc" style={{verticalAlign:"center"}}><path fill="currentColor" fill-rule="evenodd" d="M12.424 5.576a.6.6 0 0 1 0 .848l-4 4a.6.6 0 0 1-.848 0l-4-4a.6.6 0 0 1 .848-.848L8 9.15l3.576-3.575a.6.6 0 0 1 .848 0Z" clip-rule="evenodd"></path></svg>
                    : <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 16 16" preserveAspectRatio="xMidYMid meet" class="G0 qc" style={{verticalAlign:"center"}}><path fill="currentColor" fill-rule="evenodd" d="M5.576 3.576a.6.6 0 0 1 .848 0l4 4a.6.6 0 0 1 0 .848l-4 4a.6.6 0 0 1-.848-.848L9.15 8 5.576 4.424a.6.6 0 0 1 0-.848Z" clip-rule="evenodd"></path></svg>
                    }
                 </div>
                <p className="dropbox-header">
                    <span>{optional && "Optional" }</span>  {title}
                </p>
            </div>
            {
                isContentVisible &&
                <div className="dropbox-body">
                    {children}
                </div>
            }
        </div>
    )
}

export default DropBox