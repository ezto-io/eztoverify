import React, { useState } from "react";
import "./application.css";
import Link from "@docusaurus/Link";

const Application = () => {
  const [tab, setTab] = useState(1);
  return (
    <div className="application-tabs">
      <p>
        The application's details page is organized into multiple tabs such as{" "}
        <Link
          to={
            "/docs/EV/applications/applicationdetails/General"
          }
        >
          General,
        </Link>{" "}
        <Link
          to={
            "/docs/EV/applications/applicationdetails/Configurations/overview"
          }
        >
          Configuration,
        </Link>{" "}
        <Link
          to={
            "/docs/EV/applications/applicationdetails/Templates"
          }
        >
          Templates,
        </Link>{" "}
        <Link
          to={
            "/docs/EV/applications/applicationdetails/Developers"
          }
        >
          Developers,
        </Link>{" "}
        <Link
          to={
            "/docs/EV/applications/applicationdetails/How%20to%20use"
          }
        >
          How to use?,
        </Link>{" "}
        with each tab serving a distinct purpose and offering unique
        functionality for an application.
      </p>
      <div className="tabs">
        <button onClick={() => setTab(1)} className={tab === 1 && "active"}>
          General
        </button>
        <button onClick={() => setTab(2)} className={tab === 2 && "active"}>
          Gatepass
        </button>
        <button onClick={() => setTab(3)} className={tab === 3 && "active"}>
          Configuration
        </button>
        <button onClick={() => setTab(4)} className={tab === 4 && "active"}>
          Templates
        </button>
        <button onClick={() => setTab(5)} className={tab === 5 && "active"}>
          Developers
        </button>
        <button onClick={() => setTab(6)} className={tab === 6 && "active"}>
          How to use?
        </button>
      </div>

      {tab === 1 && <img src="/verify/img/tabimg/general.png" />}
      {tab === 2 && <img src="/verify/img/tabimg/gatepass.png" />}
      {tab === 3 && <img src="/verify/img/tabimg/config.png" />}
      {tab === 4 && <img src="/verify/img/tabimg/templates.png" />}
      {tab === 5 && <img src="/verify/img/tabimg/devlopers.png" />}
      {tab === 6 && <img src="/verify/img/tabimg/howto.png" />}
    </div>
  );
};

export default Application;
