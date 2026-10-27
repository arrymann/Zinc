#!/usr/bin/env python3
# Copyright 2026 The Zinc Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
"""Sanity checks for a Chromium tree with the Zinc overlay and patches applied.

These catch the mistakes that otherwise only show up deep into a multi-hour
Chromium build:
  * every file a patch adds to a GN/GNI source list exists;
  * every R.string.zinc_* / @string/zinc_* used by Zinc is defined in the
    patched android_chrome_strings.grd;
  * every other zinc_* resource (drawable, layout, xml, dimen, id) used by
    Zinc is defined by an overlay resource.

Usage: check_zinc_tree.py CHROMIUM_SRC PATCH...
"""

import os
import re
import sys

_ZINC_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_OVERLAY = os.path.join(_ZINC_ROOT, 'overlay')
_GRD = 'chrome/browser/ui/android/strings/android_chrome_strings.grd'

_DIFF_FILE_RE = re.compile(r'^diff --git a/(\S+) b/\S+$')
_GN_STRING_RE = re.compile(r'"([^"$]+\.(?:java|xml|png|gn|gni|cc|h))"')
_RES_REF_RE = re.compile(
    r'(?:R\.(string|drawable|layout|xml|dimen|id)\.|@(string|drawable|layout|xml|dimen|id)/)'
    r'(zinc_[a-z0-9_]+)')


def _added_gn_paths(patch_paths):
    """Yields (gn_file, referenced_path) for every path added to a GN file."""
    for patch_path in patch_paths:
        current = None
        with open(patch_path, encoding='utf-8') as f:
            for line in f:
                match = _DIFF_FILE_RE.match(line.rstrip('\n'))
                if match:
                    current = match.group(1)
                    continue
                if not current or not current.endswith(('.gn', '.gni')):
                    continue
                if line.startswith('+') and not line.startswith('+++'):
                    for ref in _GN_STRING_RE.findall(line):
                        yield current, ref


def _resolve(src, gn_file, ref):
    if ref.startswith('//'):
        return os.path.join(src, ref[2:])
    return os.path.join(src, os.path.dirname(gn_file), ref)


def _overlay_files(*suffixes):
    for root, _, files in os.walk(_OVERLAY):
        for name in files:
            if name.endswith(suffixes):
                yield os.path.join(root, name)


def _defined_resources():
    """Returns {type: set(names)} for resources defined by overlay files."""
    defined = {t: set() for t in ('drawable', 'layout', 'xml', 'dimen', 'id')}
    for path in _overlay_files('.xml'):
        parts = path.split(os.sep)
        # Only Android resources: <...>/res*/<type>[-qualifiers]/<name>.xml
        if len(parts) < 3 or not parts[-3].startswith('res'):
            continue
        res_dir = parts[-2]
        name = os.path.splitext(parts[-1])[0]
        if res_dir.startswith('drawable'):
            defined['drawable'].add(name)
        elif res_dir.startswith('layout'):
            defined['layout'].add(name)
        elif res_dir.startswith('xml'):
            defined['xml'].add(name)
        with open(path, encoding='utf-8') as f:
            content = f.read()
        defined['dimen'].update(re.findall(r'<dimen name="([a-z0-9_]+)"', content))
        defined['id'].update(re.findall(r'<item type="id" name="([a-z0-9_]+)"', content))
        defined['id'].update(re.findall(r'@\+id/([a-z0-9_]+)', content))
    return defined


def _used_resources(src, patch_paths):
    used = set()
    sources = list(_overlay_files('.java', '.xml'))
    for patch_path in patch_paths:
        with open(patch_path, encoding='utf-8') as f:
            added = [l for l in f if l.startswith('+') and not l.startswith('+++')]
        for line in added:
            for res_type_a, res_type_b, name in _RES_REF_RE.findall(line):
                used.add((res_type_a or res_type_b, name))
    for path in sources:
        with open(path, encoding='utf-8') as f:
            for res_type_a, res_type_b, name in _RES_REF_RE.findall(f.read()):
                used.add((res_type_a or res_type_b, name))
    return used


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    src, patch_paths = argv[1], argv[2:]
    errors = []

    for gn_file, ref in _added_gn_paths(patch_paths):
        if not os.path.exists(_resolve(src, gn_file, ref)):
            errors.append(f'{gn_file} references missing file {ref}')

    # Ids declared with @+id in patched layouts count as defined too.
    defined = _defined_resources()
    for patch_path in patch_paths:
        with open(patch_path, encoding='utf-8') as f:
            defined['id'].update(re.findall(r'@\+id/(zinc_[a-z0-9_]+)', f.read()))

    with open(os.path.join(src, _GRD), encoding='utf-8') as f:
        strings = {name.lower() for name in re.findall(r'name="IDS_(ZINC_[A-Z0-9_]+)"', f.read())}

    for res_type, name in sorted(_used_resources(src, patch_paths)):
        known = strings if res_type == 'string' else defined[res_type]
        if name not in known:
            errors.append(f'{res_type}/{name} is used but not defined')

    for error in errors:
        print(f'ERROR: {error}', file=sys.stderr)
    if errors:
        return 1
    print(f'Checked GN sources and {len(strings)} Zinc strings: OK')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
