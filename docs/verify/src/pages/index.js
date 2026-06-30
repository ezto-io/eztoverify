import React, { useEffect } from "react";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";
import "./index.css";
import { useHistory, useLocation } from '@docusaurus/router';

import styles from "./index.module.css";
import { Banner } from "../components/home/Banner";
import { About } from "../components/home/About";
import { Explore } from "../components/home/Explore";
import { Universal } from "../components/home/Universal";
import PageWrapper from "../components/home/PageWrapper";

export default function Home() {
  const { siteConfig } = useDocusaurusContext();
  const history = useHistory();
  const location = useLocation();

  useEffect(() => {
    if(location.pathname === "/verify/"){
      history.push('/verify/docs/EV/Overview/');
    }
  }, [])

  if(location.pathname === "/verify/") return null;
  return (
    <Layout
      title={`Documentation and resources for ezto verify`}
      description="Description will go into a meta tag in <head />"
    >
      <main className="main">
        <PageWrapper>
          <Banner />
          <About />
          <Universal />
        </PageWrapper>
          <Explore />
      </main>
    </Layout>
  );
}
