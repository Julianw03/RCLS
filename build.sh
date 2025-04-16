#!/bin/bash
set -euxo pipefail
VOLUME_DIR="./volume";
GITHUB_WORKSPACE="/github/workspace"
FRONTEND_OUTPUT="./rcls-frontend/dist/."
BACKEND_STATIC_RESSOURCES="./rcls-backend/src/main/resources/static"
BACKEND_OUTPUT="./rcls-backend/build/libs/."
cd ./rcls-frontend
if [ -d "./dist" ]; then
  rm -r ./dist
fi
npm ci
npm run build
cd ../
# Back in original dir
if [ -d $BACKEND_STATIC_RESSOURCES ]; then
  rm -r $BACKEND_STATIC_RESSOURCES
fi
mkdir $BACKEND_STATIC_RESSOURCES
cp -r $FRONTEND_OUTPUT $BACKEND_STATIC_RESSOURCES

cd ./rcls-backend
./gradlew bootJar
cd ../
cp -r $BACKEND_OUTPUT $VOLUME_DIR
if [ -d $GITHUB_WORKSPACE ]; then
  cp -r $BACKEND_OUTPUT $GITHUB_WORKSPACE
fi

echo "Build finished"

