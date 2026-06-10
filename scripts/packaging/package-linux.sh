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

APPDIR="target/installer/${APP_NAME}.AppDir"
rm -rf "$APPDIR"
mkdir -p \
  "$APPDIR/opt/${APP_NAME}" \
  "$APPDIR/usr/share/applications" \
  "$APPDIR/usr/share/mime/packages"

cp -R "target/installer/${APP_NAME}/." "$APPDIR/opt/${APP_NAME}/"

cat > "$APPDIR/AppRun" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/opt/AstraLab/bin/AstraLab" "$@"
EOF
chmod +x "$APPDIR/AppRun"

cat > "$APPDIR/astralab.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=AstraLab
Comment=Aerospace simulation environment for ARVT files
Exec=AppRun %f
Icon=astralab
Terminal=false
Categories=Science;Education;Engineering;
MimeType=application/x-astralab-arvt;application/x-astralab-project;
EOF
cp "$APPDIR/astralab.desktop" "$APPDIR/usr/share/applications/astralab.desktop"
cp scripts/packaging/linux/astralab-mime.xml "$APPDIR/usr/share/mime/packages/astralab-mime.xml"

cat > "$APPDIR/astralab.svg" <<'EOF'
<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">
  <rect width="256" height="256" rx="48" fill="#101827"/>
  <path d="M128 32L196 216H158L145 176H84L70 216H32L101 32H128Z" fill="#f8fafc"/>
  <path d="M95 144H134L115 86L95 144Z" fill="#006c67"/>
  <path d="M153 89L198 43L178 122L153 89Z" fill="#b46b18"/>
</svg>
EOF

if command -v appimagetool >/dev/null 2>&1; then
  appimagetool "$APPDIR" "target/installer/${APP_NAME}-${APP_VERSION}.AppImage"
  echo "Linux AppImage written to target/installer/${APP_NAME}-${APP_VERSION}.AppImage"
else
  echo "appimagetool not found. AppDir written to ${APPDIR}; jpackage app image written to target/installer/${APP_NAME}."
fi
