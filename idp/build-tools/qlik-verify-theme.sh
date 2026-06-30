#!/bin/bash
set -e

echo "Copy QlikVerify Theme"
pushd ../qlik-verify-addons/themes/

# # Build steps for custom curl_converter
# cd ./qlik_verify/admin/
# npm i
# npm run build
# mv ./dist/curl_converter.js ./dist/tree-sitter.wasm ./resources/themes/js/
# mv ./dist/tree-sitter-bash.wasm ../../../services/qlik-verify-backoffice/src/main/resources/META-INF/resources
# rm -rf dist
# cd ../../

# Build RTL css files
# cd ./qlik_verify/admin/
# npm i
# npm run build
# ./node_modules/rtlcss/bin/rtlcss.js ./resources/themes/css/main.css ./resources/themes/css/main-rtl.css
# ./node_modules/rtlcss/bin/rtlcss.js ../welcome/resources/css/dist.css ../welcome/resources/css/dist-rtl.css
# ./node_modules/rtlcss/bin/rtlcss.js ./resources/themes/css/roles-group-tree.css ./resources/themes/css/roles-group-tree-rtl.css
# ./node_modules/rtlcss/bin/rtlcss.js ../account/resources/css/account.css ../account/resources/css/account-rtl.css
# ./node_modules/rtlcss/bin/rtlcss.js ../login/resources/css/main.dev.css ../login/resources/css/main-rtl.dev.css
# ./node_modules/rtlcss/bin/rtlcss.js ../sso/resources/css/dist.css ../sso/resources/css/dist-rtl.css
# ./node_modules/rtlcss/bin/rtlcss.js ../account/resources/css/account.css ../account/resources/css/account-rtl.css

# cd ../../
# echo -e "RTL files Created\n"


cp -a qlik_verify ../../build_*/keycloak/keycloak-*/themes/
echo -e "QlikVerify Theme Copied\n"

