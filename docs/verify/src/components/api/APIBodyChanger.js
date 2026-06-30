import React, { useEffect } from "react";
import { useTypedDispatch } from "@theme/ApiItem/hooks";
import { setStringFormBody } from "@theme/ApiDemoPanel/Body/slice";

const APIBodyChanger = ({version, type}) => {
  const dispatch = useTypedDispatch();
  
  useEffect(() => {
    if (version === 0) {
      dispatch(
        setStringFormBody({
          key: "app_id",
          value: "{{app_id}}",
        })
      );

      dispatch(
        setStringFormBody({
          key: "app_secret",
          value: "{{app_secret}}",
        })
      );
    }

    if (type === "request") {
        dispatch(
            setStringFormBody({
              key: "request",
              value: "{{request}}",
            })
        );
    } else {
        dispatch(
            setStringFormBody({
              key: "auth_req_id",
              value: "{{auth_req_id}}",
            })
        );
    }

  }, [dispatch, setStringFormBody]);

  return <></>;
};

export default APIBodyChanger;
