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

./gradlew build
if [ -d $VOLUME_DIR ]; then
  cp -r $BACKEND_OUTPUT $VOLUME_DIR
fi
if [ -d $GITHUB_WORKSPACE ]; then
  mkdir -p "$GITHUB_WORKSPACE/artifacts"
  cp -r $BACKEND_OUTPUT $GITHUB_WORKSPACE/artifacts/
fi

echo "Build finished"

