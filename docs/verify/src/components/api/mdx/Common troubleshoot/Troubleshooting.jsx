import React, { useState } from 'react'

function Troubleshooting({ title, children }) {
    const [isContentVisible, setIsContentVisible] = useState(false)
    return (
        <div className="troubleshoot-dropbox"> 
            <div className='troubleshooting'>
                <p class="troubleshooting-header" onClick={() => setIsContentVisible((prev) => !prev)}>
                    <img alt="down arrow" className='border-none' height="20px" src={`/verify/img/icons/${isContentVisible ? 'down-arrow.svg' : 'right-arrow.svg'}`} />
                    {title}kjk
                </p>
            </div>
            <div>
                {
                    isContentVisible &&
                    <div>
                        {children}
                    </div>
                }
            </div>
        </div>
    )
}

export default Troubleshooting