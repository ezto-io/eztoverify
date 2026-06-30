import React from "react";
import { useEffect, useState } from "react";
import getAuthConfig from "./keycloakConfig";
import Loader from "./Loader";
import { useLocation } from '@docusaurus/router';

const ProfileNavbarItem = () => {
  const [authenticated, setAuthenticated] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [authConfig, setAuthConfig] = useState(null);
  const [isHomePage, setIsHomePage] = useState(true);
  const location = useLocation();

  // useEffect(() => {
  //   setIsHomePage(location.pathname === "/verify/");
  //   document.addEventListener("click", (event) => {
  //     const profile = document.getElementById("profile");
  //     const isClickInsideProfile = profile?.contains(event.target);
  //     if (profile && !isClickInsideProfile) {
  //       setShowProfile(false);
  //     }
  //   });
  // }, []);

  // useEffect(() => {
  //   const login = (authConfig) => {
  //     setAuthConfig(authConfig);
  //     if (authConfig && !authConfig.didInitialize && !isHomePage) {
  //       authConfig
  //         .init({
  //           pkceMethod: "S256",
  //           flow: "implicit",
  //           checkLoginIframe: true,
  //           promiseType: "native",
  //         })
  //         .then((loginState) => {
  //           if (!loginState) {
  //             authConfig.login();
  //           }
  //           if (loginState) {
  //             setAuthenticated(true);
  //           }
  //         })
  //         .catch((error) => {
  //           authConfig.login();
  //         });
  //     }
  //   };

  //   if (!authenticated) {
  //     login(getAuthConfig());
  //   }
  // }, [isHomePage]);

  return (
    <>
      {authConfig?.authenticated ? (
        <div className="profile" id="profile">
          <img
            className="profile-icon"
            src="/verify/img/icons/user.svg"
            onClick={() => {
              setShowProfile((prev) => !prev);
            }}
          />
          <ul className={`${showProfile ? "profile-active" : ""}`}>
            <li className="profile-username">
              <img src="/verify/img/icons/user.svg" />
              <span>{authConfig.tokenParsed?.preferred_username}</span>
            </li>
            <li>
              <a target="_self" href={authConfig?.createLogoutUrl({ redirectUri: `${window.location.origin}/verify/` })}>
                <img src="/verify/img/icons/logout.svg" />
                Logout
              </a>
            </li>
          </ul>
        </div>
      ) : location.pathname === "/verify/" ? (
        <></>
      ) : (
        <Loader />
      )}
    </>
  );
};

export default ProfileNavbarItem;
