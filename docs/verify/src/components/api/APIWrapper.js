import React, { useEffect, useState } from "react";
import requestApiStructure from "./requestApiStructure";
import resultApiStructure from "./resultApiStructure";
import "./APIWrapper.css";
import ApiProps from "@site/src/components/docs/ApiProps";
import CodeHighlighter from "@site/src/components/api/CodeHighlighter";
import SelectBox from "./SelectBox";
import Admonition from "@theme/Admonition";
import RequestForm from "@site/src/components/api/RequestForm";
import CodeHighlight from "./CodeHighlight";
import { useTypedDispatch } from "@theme/ApiItem/hooks";
import { setAuthData } from "@theme/ApiDemoPanel/Authorization/slice";
import { setStringFormBody } from "@theme/ApiDemoPanel/Body/slice";

const APIWrapper = ({ apiType, version }) => {
  const [selected, setSelected] = useState({});
  const [authWay, setAuthWay] = useState("");
  const [option, setOption] = useState("");
  const [flows, setFlows] = useState([]);
  const [selectedOption, setSelectedOption] = useState("");
  const [selectedFlow, setSelectedFlow] = useState("");
  const [subOption, setSubOption] = useState("");
  const [readyToFetchResult, setReadyToFetchResult] = useState(false);
  const [enableGetDetailBtn, setEnableGetDetailBtn] = useState(false);
  const [request, setRequest] = useState("");
  const [response, setResponse] = useState("");
  const [requestBody, setRequestBody] = useState({
    app_id: "",
    app_secret: "",
  });

  const versionPath = version === 1 ? "v1" : "v0";

  const dispatch = useTypedDispatch();

  useEffect(() => {
    var temp;
    if (apiType == "request") {
      temp = requestApiStructure.filter((way) => way.name === authWay);
    } else if (apiType == "result") {
      temp = resultApiStructure.filter((way) => way.name === authWay);
    }
    setOption("");
    setFlows([]);
    setSelectedOption("");
    setSelectedFlow("");
    setSubOption("");
    setRequest("");
    setResponse("");
    setSelected(temp[0]);
    if (!temp[0]?.hasSubOptions) {
      setOption(temp[0]?.options);
    }
  }, [authWay]);

  useEffect(() => {
    setSelectedOption("");
    setSelectedFlow("");
    setRequest("");
    setResponse("");
    if (selected?.hasSubOptions) {
      const temp = selected.subOptions.filter(
        (option) => option.name === subOption
      );
      setOption(temp[0]?.options);
      return;
    }
  }, [subOption]);

  useEffect(() => {
    setSelectedFlow("");
    setRequest("");
    setResponse("");
    if (selectedOption) {
      option.map((opt) => {
        if (opt.name === selectedOption) {
          setFlows([]);
          const flow = [];
          if (opt.onboarding) flow.push({ name: "Onboarding" });
          if (opt.verification) flow.push({ name: "Verification" });
          setFlows(flow);
        }
      });
    }
  }, [selectedOption]);

  useEffect(() => {
    setRequest("");
    setResponse("");
    if (selectedFlow.length > 0) {
      setReadyToFetchResult(true);
    } else {
      setReadyToFetchResult(false);
    }
  }, [selectedFlow]);

  useEffect(() => {
    if (readyToFetchResult) {
      var isFilled = true;

      if (apiType === "request") {
        Object.keys(requestBody.request.user).map((key) => {
          if (!key || !requestBody.request.user[key]) {
            isFilled = false;
          }
        });
  
        Object.keys(requestBody.request.user.metadata).map((key) => {
          if (!key || !requestBody.request.user.metadata[key]) {
            isFilled = false;
          }
        });

        if (authWay === "IVR") {
          Object.keys(requestBody.request.message.metadata).map((key) => {
            if (!key || !requestBody.request.message.metadata[key]) {
              isFilled = false;
            }
          });
        }
      } else {
          if (!requestBody.auth_req_id) {
            isFilled = false;
          }
      }

      setEnableGetDetailBtn(isFilled);

      if (version === 1) {
        dispatch(
          setAuthData({
            scheme: "ApiKeyAuth",
            key: "apiKey",
            value: "1",
          })
        );
        dispatch(
          setAuthData({
            scheme: "basicAuth",
            key: "username",
            value: requestBody.app_id,
          })
        );
        dispatch(
          setAuthData({
            scheme: "basicAuth",
            key: "password",
            value: requestBody.app_secret,
          })
        );
      }

      if (isFilled)
        updateVersionBasedBody();
    }
  }, [requestBody]);

  const handleSearch = () => {
    importFile(setRequest, createAPIJSPath("request"));
    importFile(
      setResponse,
      apiType == "request"
        ? createAPIJSPath("response")
        : createAPIJSPath("result")
    );
  };

  const getVersionBasedBody = () => {
    if (version === 1) {
      return apiType === "result"
        ? { auth_req_id: requestBody.auth_req_id }
        : { ...requestBody.request };
    }
    return apiType === "result"? { auth_req_id: requestBody.auth_req_id } : requestBody.request;
  };

  const updateVersionBasedBody = () => {
    if (version === 1) {
      switch (apiType) {
        case "request":
          dispatch(
            setStringFormBody({
              key: "request",
              value: JSON.stringify(requestBody.request, null, 2),
            })
          );
          break;
        case "result":
          dispatch(
            setStringFormBody({
              key: "auth_req_id",
              value: requestBody.auth_req_id,
            })
          );
          break;
      }
    } else {
      switch (apiType) {
        case "request":
          dispatch(
            setStringFormBody({ key: "app_id", value: requestBody.app_id })
          );
          dispatch(
            setStringFormBody({
              key: "app_secret",
              value: requestBody.app_secret,
            })
          );
          dispatch(
            setStringFormBody({
              key: "request",
              value: JSON.stringify(requestBody.request, null, 2),
            })
          );
          break;
        case "result":
          dispatch(
            setStringFormBody({ key: "app_id", value: requestBody.app_id })
          );
          dispatch(
            setStringFormBody({
              key: "app_secret",
              value: requestBody.app_secret,
            })
          );
          dispatch(
            setStringFormBody({
              key: "auth_req_id",
              value: requestBody.auth_req_id,
            })
          );
          break;
      }
    }
  };

  const createAPIJSPath = (type) => {
    if (type == "request" && apiType === "result") {
      return "./ApiProps/Result Request Body/request";
    } else if (selected?.hasSubOptions) {
      return `./ApiProps/${authWay}/${subOption}/${selectedOption}/${selectedFlow}/${type}`;
    } else {
      return `./ApiProps/${authWay}/${selectedOption}/${selectedFlow}/${type}`;
    }
  };

  const importFile = (setFile, path) => {
    import(`${path}`)
      .then((res) => {
        setFile(res.default);
      })
      .catch((err) => console.log(err));
  };

  const createMDXPath = (type) => {
    if (apiType == "result" && type == "resultRequest") {
      return `./mdx/Result API Request Body/${versionPath}.mdx`;
    } else if (selected?.hasSubOptions) {
      return `./mdx/${versionPath}/${authWay}/${subOption}/${selectedFlow}/${selectedOption}/verification_${type}_response.mdx`;
    } else {
      return `./mdx/${versionPath}/${authWay}/${selectedFlow}/${selectedOption}/verification_${type}_response.mdx`;
    }
  };

  const handleAppDetailsChange = (type, value) => {
    if (type === "app_id") {
      setRequestBody((prev) => ({ ...prev, app_id: value }));
    } else if (type === "app_secret") {
      setRequestBody((prev) => ({ ...prev, app_secret: value }));
    } else if (type === "auth_req_id") {
      setRequestBody((prev) => ({ ...prev, auth_req_id: value }));
    }
  };

  return (
    <div className="col-12">
      {apiType == "result" && (
        <Admonition type="note">
          <p>
            The Result may vary depend up on different verification flow. The
            response from API Builder is based on Email Verification Flow.
          </p>
        </Admonition>
      )}
      <div className="controller-card">
        <p className="controller-card-header">API Builder</p>
        <p className="controller-card-description">
          Build the API with needed configurations.
        </p>
        <hr className="controller-card-divider" />
        <div className="select-box-wrapper">
          <SelectBox
            value={authWay}
            setValue={setAuthWay}
            options={
              apiType == "request" ? requestApiStructure : resultApiStructure
            }
            label="Verification way"
            placeholder="Select Verify Way"
          />
          {selected?.hasSubOptions && authWay && (
            <SelectBox
              value={subOption}
              setValue={setSubOption}
              options={selected.subOptions}
              label="Type"
              placeholder="Select Sub Option"
            />
          )}
          {option && option?.length !== 0 && (
            <SelectBox
              value={selectedOption}
              setValue={setSelectedOption}
              options={option}
              label="Notification Type"
              placeholder="Select Notify Type"
            />
          )}
          {selectedOption && (
            <SelectBox
              value={selectedFlow}
              setValue={setSelectedFlow}
              options={flows}
              label="Flow"
              placeholder="Select Flow Type"
            />
          )}
        </div>
        {readyToFetchResult && (
          <>
            <div className="app-details">
              <label>App Details:</label>
              <input
                type="text"
                placeholder="Enter App id"
                value={requestBody.app_id}
                onChange={(e) =>
                  handleAppDetailsChange("app_id", e.target.value)
                }
              />
              <input
                type="text"
                placeholder="Enter App secret"
                value={requestBody.app_secret}
                onChange={(e) =>
                  handleAppDetailsChange("app_secret", e.target.value)
                }
              />
            </div>
            {apiType == "result" && (
              <div className="app-details">
                <label>Auth Request id:</label>
                <input
                  className="w-75"
                  type="text"
                  placeholder="Enter Auth Req id"
                  value={requestBody.auth_req_id}
                  onChange={(e) =>
                    handleAppDetailsChange("auth_req_id", e.target.value)
                  }
                />
              </div>
            )}
            {apiType == "request" && (
              <RequestForm way={authWay} flow={selectedFlow} setRequestBody={setRequestBody} />
            )}
          </>
        )}
        <div className="btn-wrapper">
          <button
            className="api-btn"
            disabled={!enableGetDetailBtn}
            onClick={() => handleSearch()}
          >
            Get Details
          </button>
        </div>
      </div>
      <div>
        {request && response && (
          <div>
            <h3 className="api-builder-page-header">Request Body</h3>
            <p className="api-builder-page-description">
              Below is the request body structure for the above config.
            </p>
            <div className="api-builder-body-wrapper padding-bottom-1rem">
              <ApiProps
                className="holder"
                title=""
                json={request}
                version={version}
              />
              <CodeHighlight
                code={JSON.stringify(getVersionBasedBody(), null, 2)}
                language="json"
                title="Request Body"
              />
            </div>
            <h3 className="api-builder-page-header">Response Body</h3>
            <p className="api-builder-page-description">
              Here is the response body structure for the form the API Builder
            </p>
            <div className="api-builder-body-wrapper padding-bottom-1rem">
              <ApiProps
                className="holder"
                title=""
                json={response}
                version={version}
              />
              <CodeHighlighter
                path={
                  apiType == "request"
                    ? createMDXPath("request")
                    : createMDXPath("result")
                }
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default APIWrapper;
