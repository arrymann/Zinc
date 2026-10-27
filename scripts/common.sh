#!/usr/bin/env bash
# Copyright 2026 The Zinc Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
#
# Shared settings for the Zinc scripts. Source this file; do not run it.

set -euo pipefail

ZINC_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHROMIUM_VERSION="$(tr -d '[:space:]' < "${ZINC_ROOT}/CHROMIUM_VERSION")"

# Where the Chromium checkout lives (the directory that contains .gclient).
ZINC_CHROMIUM_DIR="${ZINC_CHROMIUM_DIR:-${ZINC_ROOT}/../chromium}"
ZINC_SRC_DIR="${ZINC_CHROMIUM_DIR}/src"

# Marker used as the subject of the commit that adds the overlay files, so
# the patch tooling can find where the Zinc changes start.
ZINC_OVERLAY_COMMIT_SUBJECT="Zinc: Add overlay files"

log() { printf '\033[1;34m[zinc]\033[0m %s\n' "$*" >&2; }
die() { printf '\033[1;31m[zinc] error:\033[0m %s\n' "$*" >&2; exit 1; }

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' not found. $2"
}

# Prints the patches listed in patches/series, one absolute path per line.
zinc_patches() {
  local name
  while IFS= read -r name || [[ -n "${name}" ]]; do
    name="${name%%#*}"
    name="$(echo "${name}" | tr -d '[:space:]')"
    [[ -z "${name}" ]] && continue
    [[ -f "${ZINC_ROOT}/patches/${name}" ]] || die "patches/series lists missing ${name}"
    echo "${ZINC_ROOT}/patches/${name}"
  done < "${ZINC_ROOT}/patches/series"
}

# Copies every file under overlay/ into the Chromium tree given as $1.
copy_overlay() {
  local dest="$1"
  (cd "${ZINC_ROOT}/overlay" && find . -type f -print0) |
    while IFS= read -r -d '' file; do
      mkdir -p "${dest}/$(dirname "${file}")"
      cp "${ZINC_ROOT}/overlay/${file}" "${dest}/${file}"
    done
}
