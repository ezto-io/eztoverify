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

# main.css / main-rtl.css are the OSS theme's own stylesheets (written directly, no main.dev.css to purge from)


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




























