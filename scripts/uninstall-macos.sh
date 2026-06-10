#!/usr/bin/env bash
set -euo pipefail

APP_BUNDLE="/Applications/AstraLab.app"
USER_DATA="${HOME}/Library/Application Support/AstraLab"

echo "AstraLab macOS uninstall"

if [[ -d "$APP_BUNDLE" ]]; then
  echo "Removing $APP_BUNDLE"
  rm -rf "$APP_BUNDLE"
else
  echo "Application bundle not found at $APP_BUNDLE"
fi

if [[ -d "$USER_DATA" ]]; then
  read -r -p "Delete AstraLab user data at '$USER_DATA'? This does not delete projects elsewhere. [y/N] " answer
  case "$answer" in
    y|Y|yes|YES)
      rm -rf "$USER_DATA"
      echo "User data removed."
      ;;
    *)
      echo "User data preserved."
      ;;
  esac
fi

echo "AstraLab uninstall complete."
