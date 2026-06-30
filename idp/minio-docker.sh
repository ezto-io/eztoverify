#! /bin/sh
set -e

# Step 1 : Build the Keycloak 
# Step 2: Run the minio-start-server.sh file to start the MINIO Server in Docker
docker run -d -p 9000:9000\
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=Sri@1234" \
  -e "MINIO_ROOT_PASSWORD=Sri@1234"\
  minio/minio server /data --console-address ":9001"
