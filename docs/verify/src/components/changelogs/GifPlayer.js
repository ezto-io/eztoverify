import React from 'react'
import { useState } from 'react';
import "./GifPlayer.css"

const GifPlayer = ({name}) => {
    const [image] = useState(
        require(`../../../static/gifs/${name}.gif`)
    );
  return (
    <div className='changelog-gif'>
      <img src={image.default} alt={name}/>
    </div>
  )
}

export default GifPlayer


