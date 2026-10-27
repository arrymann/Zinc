#!/usr/bin/env bash
# Copyright 2026 The Zinc Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
#
# Checks out the Chromium version Zinc is pinned to (see CHROMIUM_VERSION)
# together with its Android dependencies.
#
# Requirements: Linux x86-64, depot_tools on PATH, ~150 GB free disk space.
# Usage: scripts/fetch_chromium.sh [--no-install-deps]

source "$(dirname "$0")/common.sh"

install_deps=1
[[ "${1:-}" == "--no-install-deps" ]] && install_deps=0

require_command gclient "Install depot_tools: https://chromium.googlesource.com/chromium/tools/depot_tools.git"
require_command fetch "Install depot_tools: https://chromium.googlesource.com/chromium/tools/depot_tools.git"

mkdir -p "${ZINC_CHROMIUM_DIR}"
cd "${ZINC_CHROMIUM_DIR}"

if [[ ! -f .gclient ]]; then
  log "Fetching Chromium for Android into ${ZINC_CHROMIUM_DIR} (this takes a while)"
  fetch --nohooks --no-history android
fi

grep -q "target_os.*android" .gclient ||
  die ".gclient in ${ZINC_CHROMIUM_DIR} does not target Android"

log "Syncing to Chromium ${CHROMIUM_VERSION}"
git -C src fetch --depth=1 origin "refs/tags/${CHROMIUM_VERSION}:refs/tags/${CHROMIUM_VERSION}"
git -C src checkout --force --detach "${CHROMIUM_VERSION}"
gclient sync --nohooks --no-history -D --force --reset --with_tags \
  --revision "src@refs/tags/${CHROMIUM_VERSION}"

if [[ "${install_deps}" == 1 ]]; then
  log "Installing build dependencies (needs sudo)"
  ./src/build/install-build-deps.sh --android --no-prompt
fi

log "Running hooks"
gclient runhooks

log "Done. Next: scripts/apply_patches.sh"
