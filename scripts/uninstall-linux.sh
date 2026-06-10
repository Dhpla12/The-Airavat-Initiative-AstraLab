#!/usr/bin/env bash
set -euo pipefail

APP_NAME="AstraLab"
USER_DATA="${HOME}/.local/share/AstraLab"
PROJECTS="${HOME}/AstraLabProjects"

echo "AstraLab Linux uninstall"

remove_path() {
  local path="$1"
  if [[ -e "$path" ]]; then
    echo "Removing $path"
    rm -rf "$path"
  fi
}

remove_path "${HOME}/.local/opt/${APP_NAME}"
remove_path "${HOME}/.local/share/applications/astralab.desktop"
remove_path "${HOME}/.local/share/mime/packages/astralab-mime.xml"
remove_path "${HOME}/.local/bin/${APP_NAME}"

if [[ -w "/opt" ]]; then
  remove_path "/opt/${APP_NAME}"
elif [[ -d "/opt/${APP_NAME}" ]]; then
  echo "Found /opt/${APP_NAME}; rerun with sudo to remove it."
fi

if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database "${HOME}/.local/share/applications" >/dev/null 2>&1 || true
fi

if command -v update-mime-database >/dev/null 2>&1; then
  update-mime-database "${HOME}/.local/share/mime" >/dev/null 2>&1 || true
fi

if [[ -d "$USER_DATA" ]]; then
  read -r -p "Delete AstraLab user data at '$USER_DATA'? [y/N] " answer
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

if [[ -d "$PROJECTS" ]]; then
  read -r -p "Delete projects at '$PROJECTS'? [y/N] " answer
  case "$answer" in
    y|Y|yes|YES)
      rm -rf "$PROJECTS"
      echo "Projects removed."
      ;;
    *)
      echo "Projects preserved."
      ;;
  esac
fi

echo "AstraLab uninstall complete."
