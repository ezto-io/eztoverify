import React from 'react'
import CodeBlock from '@theme/CodeBlock';

const CodeHighlight = ({language, code, title}) => {
  return (
    <div>
      <CodeBlock language={language} title={title}>
        {code}
      </CodeBlock>
    </div>
  )
}

export default CodeHighlight
