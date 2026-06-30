import React, { useEffect, useState } from "react";
import MDXContent from "@theme/MDXContent";

const CodeHighlighter = ({ path }) => {
  const [Code, setCode] = useState(null);
  useEffect(() => {
    import(`${path}`)
      .then((res) => {
        setCode(() => res.default);
      })
      .catch((err) => console.log(err));
  }, []);
  return (
    <div>
      <MDXContent>{Code && <Code />}</MDXContent>
    </div>
  );
};

export default CodeHighlighter;
