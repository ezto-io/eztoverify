
import Keycloak from "keycloak-js";
import siteConfig from "@generated/docusaurus.config";

let auth = null;

const getAuthConfig = () => {
    if (auth === null) {
        auth = new Keycloak({
            ...siteConfig.customFields.auth,
          });
    }
    return auth
}

export default getAuthConfig;
