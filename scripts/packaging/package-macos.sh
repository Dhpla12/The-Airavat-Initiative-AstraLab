#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_NAME="AstraLab"
APP_VERSION="1.0.0"
MAIN_JAR="astralab-${APP_VERSION}.jar"
MAIN_CLASS="com.airavat.astralab.ui.AstraLabLauncher"
PACKAGE_ID="com.airavat.astralab"
ARVT_ASSOC="scripts/packaging/file-associations/arvt.properties"
APROJ_ASSOC="scripts/packaging/file-associations/aproj.properties"
JAVAFX_VERSION="21.0.6"
JAVAFX_MODULES="javafx.base,javafx.graphics,javafx.controls,javafx.swing"
RUNTIME_MODULES="${JAVAFX_MODULES},java.desktop,java.logging,java.xml,jdk.charsets,jdk.jfr,jdk.unsupported,jdk.unsupported.desktop"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "This script must be run on macOS."
  exit 1
fi

case "$(uname -m)" in
  arm64|aarch64)
    JAVAFX_CLASSIFIER="mac-aarch64"
    ;;
  x86_64)
    JAVAFX_CLASSIFIER="mac"
    ;;
  *)
    echo "Unsupported macOS architecture: $(uname -m)"
    exit 1
    ;;
esac

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)"
fi

if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/jlink" || ! -x "${JAVA_HOME}/bin/jpackage" ]]; then
  echo "A full JDK 21+ with jlink and jpackage is required. Set JAVA_HOME to that JDK."
  exit 1
fi

patch_document_types() {
  local app_image="$1"
  local plist="${app_image}/Contents/Info.plist"

  if [[ ! -f "$plist" ]]; then
    echo "Missing Info.plist: $plist"
    exit 1
  fi

  /usr/libexec/PlistBuddy -c "Delete :CFBundleDocumentTypes:0:CFBundleTypeExtensions" "$plist" 2>/dev/null || true
  /usr/libexec/PlistBuddy -c "Add :CFBundleDocumentTypes:0:CFBundleTypeExtensions array" "$plist"
  /usr/libexec/PlistBuddy -c "Add :CFBundleDocumentTypes:0:CFBundleTypeExtensions:0 string arvt" "$plist"
  /usr/libexec/PlistBuddy -c "Delete :CFBundleDocumentTypes:0:CFBundleTypeMIMETypes" "$plist" 2>/dev/null || true
  /usr/libexec/PlistBuddy -c "Add :CFBundleDocumentTypes:0:CFBundleTypeMIMETypes array" "$plist"
  /usr/libexec/PlistBuddy -c "Add :CFBundleDocumentTypes:0:CFBundleTypeMIMETypes:0 string application/x-astralab-arvt" "$plist"

  /usr/libexec/PlistBuddy -c "Delete :CFBundleDocumentTypes:1:CFBundleTypeExtensions" "$plist" 2>/dev/null || true
  /usr/libexec/PlistBuddy -c "Add :CFBundleDocumentTypes:1:CFBundleTypeExtensions array" "$plist"
  /usr/libexec/PlistBuddy -c "Add :CFBundleDocumentTypes:1:CFBundleTypeExtensions:0 string aproj" "$plist"
  /usr/libexec/PlistBuddy -c "Delete :CFBundleDocumentTypes:1:CFBundleTypeMIMETypes" "$plist" 2>/dev/null || true
  /usr/libexec/PlistBuddy -c "Add :CFBundleDocumentTypes:1:CFBundleTypeMIMETypes array" "$plist"
  /usr/libexec/PlistBuddy -c "Add :CFBundleDocumentTypes:1:CFBundleTypeMIMETypes:0 string application/x-astralab-project" "$plist"
}

cd "$ROOT_DIR"
mvn -DskipTests clean package

rm -rf target/jpackage-input target/runtime target/installer
mkdir -p target/jpackage-input/lib target/runtime target/installer
cp "target/${MAIN_JAR}" target/jpackage-input/

# JavaFX is placed in the custom runtime image by jlink. Only non-JavaFX
# libraries stay on the app classpath.
find target/lib -name '*.jar' ! -name 'javafx-*.jar' -exec cp {} target/jpackage-input/lib/ \;

JAVAFX_MODULE_PATH=""
for module in base graphics controls swing; do
  jar="target/lib/javafx-${module}-${JAVAFX_VERSION}-${JAVAFX_CLASSIFIER}.jar"
  if [[ ! -f "$jar" ]]; then
    echo "Missing JavaFX runtime jar: $jar"
    echo "Run Maven on this macOS architecture so JavaFX platform artifacts are resolved."
    exit 1
  fi
  if [[ -z "$JAVAFX_MODULE_PATH" ]]; then
    JAVAFX_MODULE_PATH="$jar"
  else
    JAVAFX_MODULE_PATH="${JAVAFX_MODULE_PATH}:$jar"
  fi
done

"${JAVA_HOME}/bin/jlink" \
  --module-path "${JAVA_HOME}/jmods:${JAVAFX_MODULE_PATH}" \
  --add-modules "$RUNTIME_MODULES" \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --output "target/runtime/${APP_NAME}Runtime"

"${JAVA_HOME}/bin/jpackage" \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "Airavat Aerospace" \
  --mac-package-identifier "$PACKAGE_ID" \
  --runtime-image "target/runtime/${APP_NAME}Runtime" \
  --input target/jpackage-input \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --dest target/installer \
  --file-associations "$ARVT_ASSOC" \
  --file-associations "$APROJ_ASSOC" \
  --java-options "--add-modules=${JAVAFX_MODULES}" \
  --java-options "-Xmx1g"

patch_document_types "target/installer/${APP_NAME}.app"

"${JAVA_HOME}/bin/jpackage" \
  --type dmg \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "Airavat Aerospace" \
  --mac-package-identifier "$PACKAGE_ID" \
  --app-image "target/installer/${APP_NAME}.app" \
  --dest target/installer

echo "macOS packages written to target/installer"
