#!/usr/bin/env bash
# Copyright 2026 The Zinc Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
#
# Regenerates overlay/ and patches/ from a Chromium checkout where Zinc was
# applied with scripts/apply_patches.sh and then edited.
#
# Workflow: edit files in src/, commit with `git commit --fixup` / `git rebase
# -i` so that each Zinc commit stays one logical patch, then run this script.
#   - Files that came from overlay/ are copied back from src/ (edit them in
#     either place, but this script makes src/ win).
#   - Every commit after the "Zinc: Add overlay files" commit becomes a patch.
#
# Usage: scripts/refresh_patches.sh

source "$(dirname "$0")/common.sh"

cd "${ZINC_SRC_DIR}"

overlay_commit="$(git log --format=%H --grep="^${ZINC_OVERLAY_COMMIT_SUBJECT}\$" -n 1)"
[[ -n "${overlay_commit}" ]] || die "No '${ZINC_OVERLAY_COMMIT_SUBJECT}' commit in src/."
git diff --quiet && git diff --cached --quiet ||
  die "src/ has uncommitted changes. Commit them first."

log "Syncing overlay/ from src/"
(cd "${ZINC_ROOT}/overlay" && find . -type f) | while IFS= read -r file; do
  if [[ -f "${file}" ]]; then
    cp "${file}" "${ZINC_ROOT}/overlay/${file}"
  else
    log "warning: ${file} no longer exists in src/; remove it from overlay/ if intended"
  fi
done

log "Regenerating patches/"
rm -f "${ZINC_ROOT}"/patches/*.patch
git format-patch -q --zero-commit --no-signature --no-stat \
  -o "${ZINC_ROOT}/patches" "${overlay_commit}..HEAD"
(cd "${ZINC_ROOT}/patches" && ls -1 ./*.patch | sed 's#^\./##' > series)

log "Wrote $(wc -l < "${ZINC_ROOT}/patches/series" | tr -d ' ') patches. Review with git diff in ${ZINC_ROOT}."
