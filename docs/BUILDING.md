# Building Zinc

Zinc is built like Chromium for Android, with Zinc's files and patches applied
first. The scripts in `scripts/` wrap every step.

## Requirements

- Linux x86-64 (Ubuntu 22.04 or newer works best; Chromium does not support
  building Android on macOS or Windows).
- About 150 GB of free disk space for the checkout and one build directory.
- 16 GB of RAM or more (32 GB recommended) and as many CPU cores as you can
  get. A first release build takes several hours on an 8-core machine.
- [depot_tools](https://commondatastorage.googleapis.com/chrome-infra-docs/flat/depot_tools/docs/html/depot_tools_tutorial.html#_setting_up)
  cloned somewhere and added to `PATH`.

By default the Chromium checkout goes next to this repository
(`../chromium`). Set `ZINC_CHROMIUM_DIR` to put it elsewhere:

```sh
export ZINC_CHROMIUM_DIR=/data/chromium
```

## 1. Fetch Chromium

```sh
scripts/fetch_chromium.sh
```

This runs `fetch android` (the first time only), checks out the version in
`CHROMIUM_VERSION`, syncs its dependencies, installs the build dependencies
with `install-build-deps.sh --android` (pass `--no-install-deps` to skip; it
needs `sudo`), and runs the hooks.

## 2. Apply Zinc

```sh
scripts/apply_patches.sh
```

This copies `overlay/` into `src/` as one commit ("Zinc: Add overlay files")
and then applies each patch in `patches/series` as its own commit with
`git am`, so `git log` in `src/` shows exactly what Zinc changes. To start
over from a clean Chromium (for example after pulling new Zinc changes):

```sh
scripts/apply_patches.sh --reset
```

`--reset` discards any uncommitted changes and Zinc commits in `src/`.

## 3. Build

```sh
scripts/build.sh                              # release, arm64 phones
scripts/build.sh args/zinc_release_x64.gn     # release, emulator / x86-64
scripts/build.sh args/zinc_debug_arm64.gn     # developer build
```

The APK is copied to `dist/`. Install it with `adb install -r dist/<apk>`.

| Args file | Use |
| --- | --- |
| `args/zinc_release_arm64.gn` | Phones. Official, optimized build. Package `app.zinc.browser`. |
| `args/zinc_release_x64.gn` | Android emulator, ChromeOS, x86-64 devices. |
| `args/zinc_debug_arm64.gn` | Development: component build, DCHECKs, lint and warnings-as-errors on. Package `app.zinc.browser.dev`, so it installs next to a release build. |

Any other target works too, e.g. `scripts/build.sh args/zinc_debug_arm64.gn
chrome_junit_tests` builds the unit tests (run them with
`out/zinc_debug_arm64/bin/run_chrome_junit_tests`).

### Signing

`chrome_public_apk` is signed with Chromium's debug key, which is fine for
testing. To distribute Zinc, sign the APK with your own key so that updates
install over each other:

```sh
apksigner sign --ks zinc-release.jks --out Zinc-signed.apk dist/Zinc-*.apk
```

## Changing Zinc

- **New files** go in `overlay/`, mirroring their path in the Chromium tree,
  and must be listed in the relevant `BUILD.gn` (through a patch).
- **Changes to Chromium files** are patches. Make them in `src/` after
  `scripts/apply_patches.sh`, commit them (amend or `git commit --fixup` the
  matching Zinc commit to keep one logical change per patch), then run:

  ```sh
  scripts/refresh_patches.sh
  ```

  This regenerates `patches/` from the commits after "Zinc: Add overlay
  files" and copies overlay files you edited in `src/` back to `overlay/`.
- **Check your work** without a Chromium checkout:

  ```sh
  gradle -p standalone test        # toolbar model unit tests
  scripts/verify_patches.sh        # patches apply, references resolve
  ```

  CI runs both on every push.

## Moving to a newer Chromium

1. Put the new version in `CHROMIUM_VERSION` (a tag such as `158.0.8130.40`;
   stable releases are listed at <https://chromiumdash.appspot.com/releases?platform=Android>).
2. `scripts/verify_patches.sh` shows which patches no longer apply.
3. `scripts/fetch_chromium.sh`, then `scripts/apply_patches.sh --reset`.
   When `git am` stops on a patch, fix the conflict, `git am --continue`, and
   finish with `scripts/refresh_patches.sh`.
4. Build, test on a device, and commit the updated `patches/` and
   `CHROMIUM_VERSION`.

## Building in CI

`.github/workflows/ci.yml` can build the APK on a self-hosted runner. Hosted
GitHub runners are far too small for Chromium. To set one up:

1. Prepare a Linux x86-64 machine as in *Requirements*, with depot_tools on
   the runner's `PATH` and `src/build/install-build-deps.sh --android` run
   once.
2. Register it as a self-hosted runner with the label `zinc-builder`.
3. Optionally set the repository variable `ZINC_CHROMIUM_DIR` (default
   `/opt/zinc/chromium`).
4. Run the CI workflow manually with *Build the APK* checked. The APK is
   uploaded as the `zinc-apk` artifact.
