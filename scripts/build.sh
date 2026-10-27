#!/usr/bin/env bash
# Copyright 2026 The Zinc Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
#
# Builds the Zinc APK.
#
# Usage: scripts/build.sh [args-file] [ninja target]
#   args-file    defaults to args/zinc_release_arm64.gn
#   ninja target defaults to chrome_public_apk
# The output directory is out/<args-file name> inside src/, and the APK is
# copied to dist/ in this repository.

source "$(dirname "$0")/common.sh"

args_file="${1:-${ZINC_ROOT}/args/zinc_release_arm64.gn}"
target="${2:-chrome_public_apk}"
[[ -f "${args_file}" ]] || die "No such args file: ${args_file}"
require_command gn "Add depot_tools to PATH."
require_command autoninja "Add depot_tools to PATH."

config="$(basename "${args_file}" .gn)"
out_dir="out/${config}"

cd "${ZINC_SRC_DIR}"
mkdir -p "${out_dir}"
cp "${args_file}" "${out_dir}/args.gn"

log "gn gen ${out_dir}"
gn gen "${out_dir}"

log "Building ${target} (${config})"
autoninja -C "${out_dir}" "${target}"

if [[ "${target}" == "chrome_public_apk" ]]; then
  mkdir -p "${ZINC_ROOT}/dist"
  apk="${ZINC_ROOT}/dist/Zinc-${CHROMIUM_VERSION}-${config}.apk"
  cp "${out_dir}/apks/ChromePublic.apk" "${apk}"
  log "APK: ${apk}"
  log "Install with: adb install -r \"${apk}\""
fi
