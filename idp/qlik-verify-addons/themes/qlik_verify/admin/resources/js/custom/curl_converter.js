import * as _ from 'curlconverter';

const curlParser = require('curl-parser');

const {
  parse_curl
} = require('curl-parser');

export const supportedLanguges = [
  "csharp",
  "dart",
  "go",
  "java",
  "javascript",
  "kotlin",
  "php",
  "python",
];

export function isSupported(language) {
  return supportedLanguges.includes(language);
}

export function isValidCurl(curl) {
  try {
    parse_curl(curl);
    return true;
  } catch (e) {
    return false;
  }
}

export function convertCurlToSnippets(curl, language) {

  try {
    parse_curl(curl);
  } catch (e) {
    return "Invalid curl";
  }

  switch (language) {
    case "ansible":
      return _.toAnsible(curl);

    case "cfml":
      return _.toCFML(curl);

    case "clojure":
      return _.toClojure(curl);

    case "csharp":
      return _.toCSharp(curl);

    case "dart":
      return _.toDart(curl);

    case "elixir":
      return _.toElixir(curl);

    case "go":
      return _.toGo(curl);

    case "har":
      return _.toHarString(curl);

    case "http":
      return _.toHTTP(curl);

    case "httpie":
      return _.toHttpie(curl);

    case "java":
      return _.toJava(curl);

    case "java-httpurlconnection":
      return _.toJavaHttpUrlConnection(curl);

    case "java-jsoup":
      return _.toJavaJsoup(curl);

    case "java-okhttp":
      return _.toJavaOkHttp(curl);

    case "javascript":
      return _.toJavaScript(curl);

    case "javascript-jquery":
      return _.toJavaScriptJquery(curl);

    case "javascript-xhr":
      return _.toJavaScriptXHR(curl);

    case "json":
      return _.toJsonString(curl);

    case "julia":
      return _.toJulia(curl);

    case "kotlin":
      return _.toKotlin(curl);

    case "lua":
      return _.toLua(curl);

    case "matlab":
      return _.toMATLAB(curl);

    case "node":
      return _.toNode(curl);

    case "node-http":
      return _.toNodeHttp(curl);

    case "node-axios":
      return _.toNodeAxios(curl);

    case "node-got":
      return _.toNodeGot(curl);

    case "node-ky":
      return _.toNodeKy(curl);

    case "node-request":
      return _.toNodeRequest(curl);

    case "node-superagent":
      return _.toNodeSuperAgent(curl);

    case "objc":
      return _.toObjectiveC(curl);

    case "ocaml":
      return _.toOCaml(curl);

    case "perl":
      return _.toPerl(curl);

    case "php":
      return _.toPhp(curl);

    case "php-guzzle":
      return _.toPhpGuzzle(curl);

    case "php-requests":
      return _.toPhpRequests(curl);

    case "powershell":
      return _.toPowershellRestMethod(curl);

    case "powershell-webrequest":
      return _.toPowershellWebRequest(curl);

    case "python":
      return _.toPython(curl);

    case "python-http":
      return _.toPythonHttp(curl);

    case "r":
      return _.toR(curl);

    case "ruby":
      return _.toRuby(curl);

    case "rust":
      return _.toRust(curl);

    case "swift":
      return _.toSwift(curl);

    case "wget":
      return _.toWget(curl);

    default:
      return "Invalid language selection";
  }

}


