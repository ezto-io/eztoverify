import React from "react";
import "./VideoPlayer.css";
import ivr from '@site/static/video/ivr.mp4';

const VideoPlayer = ({name}) => {
  // Only the IVR walkthrough video ships with the OSS docs. Other product
  // walkthrough videos were removed; add an import + a branch here to restore one.
  var video = ivr;

  if (name === "IVR") {
    video = ivr;
  }

  return (
    <div className="video-player">
      <video controls width="250" loop autoPlay>
        <source src={video} type="video/mp4"></source>
      </video>
    </div>
  );
};

export default VideoPlayer;
