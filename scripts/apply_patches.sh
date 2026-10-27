#!/usr/bin/env bash
# Copyright 2026 The Zinc Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
#
# Turns a pristine Chromium checkout into Zinc:
#   1. resets src/ to the pinned Chromium tag (with --reset),
#   2. copies overlay/ (new Zinc files) into src/ and commits them,
#   3. applies patches/series on top, one commit per patch (git am).
#
# Usage: scripts/apply_patches.sh [--reset]

source "$(dirname "$0")/common.sh"

[[ -d "${ZINC_SRC_DIR}/.git" ]] ||
  die "No Chromium checkout at ${ZINC_SRC_DIR}. Run scripts/fetch_chromium.sh first or set ZINC_CHROMIUM_DIR."

cd "${ZINC_SRC_DIR}"

if [[ "${1:-}" == "--reset" ]]; then
  log "Resetting src/ to ${CHROMIUM_VERSION}"
  git am --abort >/dev/null 2>&1 || true
  git checkout --force --detach "${CHROMIUM_VERSION}"
  # Remove overlay files left over from a previous run (untracked at the tag).
  (cd "${ZINC_ROOT}/overlay" && find . -type f) | while IFS= read -r file; do
    git ls-files --error-unmatch "${file}" >/dev/null 2>&1 || rm -f "${file}"
  done
elif git log -1 --format=%s HEAD~"$(zinc_patches | wc -l)" 2>/dev/null |
     grep -qx "${ZINC_OVERLAY_COMMIT_SUBJECT}"; then
  die "Zinc is already applied. Use --reset to re-apply from scratch."
fi

[[ "$(git rev-parse HEAD)" == "$(git rev-parse "${CHROMIUM_VERSION}^{commit}")" ]] ||
  die "src/ is not at ${CHROMIUM_VERSION}. Use --reset (discards local changes in src/)."

git config user.name >/dev/null || git config user.name "Zinc Builder"
git config user.email >/dev/null || git config user.email "zinc@localhost"

log "Copying overlay files"
copy_overlay "${ZINC_SRC_DIR}"
git add -A -- $(cd "${ZINC_ROOT}/overlay" && find . -type f | sed 's#^\./##')
git commit -q --no-verify -m "${ZINC_OVERLAY_COMMIT_SUBJECT}"

log "Applying $(zinc_patches | wc -l | tr -d ' ') patches"
mapfile -t patches < <(zinc_patches)
git am --3way --keep-non-patch --committer-date-is-author-date "${patches[@]}" ||
  die "A patch did not apply. Resolve it, run 'git am --continue', then scripts/refresh_patches.sh."

log "Zinc applied on top of Chromium ${CHROMIUM_VERSION}. Next: scripts/build.sh"
