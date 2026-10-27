#!/usr/bin/env bash
# Copyright 2026 The Zinc Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
#
# Checks that overlay/ and patches/ apply cleanly to the pinned Chromium
# version, without needing depot_tools or a full checkout: it does a
# blob-less, sparse clone of the GitHub mirror containing only the files the
# patches touch, applies everything, and checks that every Zinc file the
# patched GN files reference exists. Used by CI.
#
# Usage: scripts/verify_patches.sh [work-dir]

source "$(dirname "$0")/common.sh"

work_dir="${1:-$(mktemp -d)}"
repo="${work_dir}/chromium"
mirror="${ZINC_CHROMIUM_MIRROR:-https://github.com/chromium/chromium}"

if [[ ! -d "${repo}/.git" ]]; then
  log "Sparse-cloning ${mirror} at ${CHROMIUM_VERSION}"
  git clone --quiet --filter=blob:none --no-checkout --depth 1 \
    --branch "${CHROMIUM_VERSION}" "${mirror}" "${repo}"
fi

cd "${repo}"
git config gc.auto 0
git sparse-checkout init --no-cone
mapfile -t patches < <(zinc_patches)
grep -h '^diff --git a/' "${patches[@]}" | sed -E 's#^diff --git a/([^ ]+) b/.*#/\1#' |
  sort -u > .git/info/sparse-checkout
git checkout --quiet --force "${CHROMIUM_VERSION}"
git clean -fdq

log "Copying overlay files"
copy_overlay "${repo}"

for patch in "${patches[@]}"; do
  log "Applying $(basename "${patch}")"
  git apply --check "${patch}" ||
    die "$(basename "${patch}") does not apply to ${CHROMIUM_VERSION}"
  git apply "${patch}"
done

log "Checking GN sources and resources referenced by Zinc"
python3 -I "${ZINC_ROOT}/tools/check_zinc_tree.py" "${repo}" "${patches[@]}"

log "OK: overlay and ${#patches[@]} patches apply cleanly to Chromium ${CHROMIUM_VERSION}"
