#!/usr/bin/env bash
# Small bootstrap launcher for this source ZIP. Downloads Gradle 8.13 on first run.
set -euo pipefail
GRADLE_VERSION="8.13"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"
DIST_DIR="${GRADLE_USER_HOME}/wrapper/dists/gradle-${GRADLE_VERSION}-bin/zasnake"
GRADLE_HOME="${DIST_DIR}/gradle-${GRADLE_VERSION}"
GRADLE_BIN="${GRADLE_HOME}/bin/gradle"

if [[ ! -x "${GRADLE_BIN}" ]]; then
  mkdir -p "${DIST_DIR}"
  ZIP_FILE="${DIST_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
  echo "Downloading Gradle ${GRADLE_VERSION} (first run only)..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 2 "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "${ZIP_FILE}"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "${ZIP_FILE}" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  else
    echo "Install curl or wget, or open this project using Android Studio." >&2
    exit 1
  fi
  command -v unzip >/dev/null 2>&1 || { echo "Install unzip to extract Gradle." >&2; exit 1; }
  unzip -q -o "${ZIP_FILE}" -d "${DIST_DIR}"
fi
exec "${GRADLE_BIN}" "$@"
