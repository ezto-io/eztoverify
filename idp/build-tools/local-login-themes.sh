set -e

echo "Building login themes...."
pushd ../qlik-verify-addons/themes/qlik_verify/admin

asset_folder="assets"

# Check if the folder already exists
if [ -d "$asset_folder" ]; then
  echo "Folder '$asset_folder' already exists."
else
  # Create the folder
  mkdir -p ../"$asset_folder"
  echo "Folder '$asset_folder' created successfully."
fi

npx purgecss --css ../login/resources/css/main.dev.css --content ../../../**/*.ftl  --output ../login/resources/css/main.css
npx purgecss --css ../login/resources/css/main-rtl.dev.css --content  ../../../**/*.ftl  --output ../login/resources/css/main-rtl.css
#npx purgecss --css ../login/resources/css/bootstrap.min.css --content ../login/*.ftl --output ../login/resources/css/bootstrap.css
#npx purgecss --css ../login/resources/css/bootstrap-rtl.min.css --content ../login/*.ftl --output ../login/resources/css/bootstrap-rtl.css


npx uglifyjs-folder ../login/resources/js -eo ../assets/js

cp -r ../login/resources/js/tele-input/* ../"$asset_folder"/js/tele-input/

mkdir -p ../"$asset_folder"/css

npx css-minify -d  ../login/resources/css/  -o ../assets/css

img_directory="img"
fonts_directory="fonts"

mkdir -p ../"$asset_folder"/"$img_directory"
mkdir -p ../"$asset_folder"/"$fonts_directory"

cp -r ../login/resources/"$img_directory"/* ../"$asset_folder"/"$img_directory"
cp -r ../login/resources/"$fonts_directory"/* ../"$asset_folder"/"$fonts_directory"

rm -rf ../../../services/qlik-verify-services/src/main/resources/META-INF/resources/assets
mv ../"$asset_folder" ../../../services/qlik-verify-services/src/main/resources/META-INF/resources/assets

echo "Copied font and images..."




























