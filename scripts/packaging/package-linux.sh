#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_NAME="AstraLab"
APP_VERSION="1.0.0"
MAIN_JAR="astralab-${APP_VERSION}.jar"
MAIN_CLASS="com.airavat.astralab.ui.AstraLabLauncher"
ARVT_ASSOC="scripts/packaging/file-associations/arvt.properties"
APROJ_ASSOC="scripts/packaging/file-associations/aproj.properties"

cd "$ROOT_DIR"
mvn -DskipTests package

rm -rf target/jpackage-input target/installer
mkdir -p target/jpackage-input/lib target/installer
cp "target/${MAIN_JAR}" target/jpackage-input/
cp target/lib/*.jar target/jpackage-input/lib/

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "Airavat Aerospace" \
  --input target/jpackage-input \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --dest target/installer \
  --file-associations "$ARVT_ASSOC" \
  --file-associations "$APROJ_ASSOC" \
  --java-options "-Xmx1g"

mkdir -p target/installer/linux-metadata
cp scripts/packaging/linux/astralab.desktop target/installer/linux-metadata/
cp scripts/packaging/linux/astralab-mime.xml target/installer/linux-metadata/

if command -v appimagetool >/dev/null 2>&1; then
  appimagetool "target/installer/${APP_NAME}" "target/installer/${APP_NAME}-${APP_VERSION}.AppImage"
  echo "Linux AppImage written to target/installer/${APP_NAME}-${APP_VERSION}.AppImage"
else
  echo "appimagetool not found. jpackage app image written to target/installer/${APP_NAME}."
fi
