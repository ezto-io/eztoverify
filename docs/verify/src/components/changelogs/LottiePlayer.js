import React, { useState } from "react";
import "./LottiePlayer.css";
import Lottie from "lottie-react";

const LottiePlayer = ({ lottie }) => {
  const [lottieJson, setLottieJson] = useState(
    require(`../../../static/json/${lottie}.json`)
  );

  const defaultOptions = {
    loop: true,
    autoplay: true,
    animationData: lottieJson,
    rendererSettings: { preserveAspectRatio: "xMidYMid slice" },
  };

  return (
    <div className="changelog-lottie">
      <Lottie animationData={lottieJson}/>
    </div>
  );
};

export default LottiePlayer;
