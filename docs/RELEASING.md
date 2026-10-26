# My X: Signed APK releases

A successful push to `master` runs unit tests, builds a signed release APK, publishes it as a GitHub Release, and only then removes older Releases and their associated tags. PRs and non-master pushes only run unit tests and publish a downloadable debug build artifact.

## Configure GitHub Actions secrets

In **Settings > Secrets and variables > Actions > Repository secrets**, set the following four secrets before triggering a master build:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of the **existing** Android JKS/keystore, e.g. `base64 < my-x-release.jks | tr -d '\n'` |
| `KEYSTORE_PASSWORD` | Password for that keystore |
| `KEY_ALIAS` | Alias of the signing key |
| `KEY_PASSWORD` | Password for that key |

Keep the original keystore and passwords in a secure backup. Every subsequent release must use the **same signing key** to install as an update over the previous signed release. Never commit a keystore or passwords.

The workflow fails if any secret is missing or if the generated APK does not pass `apksigner verify`. It does **not** fall back to a debug-signed or unsigned Release APK.

## Versioning and retention

The signed APK is attached as `my-x.apk`. Its Android `versionCode` is the Actions workflow run number, and `versionName` / Git tag are `1.0.<run_number>` and `v1.0.<run_number>`. A rerun of the same workflow uploads the APK to the same tag.

**Only after publishing successfully**, the workflow deletes older GitHub Releases and their corresponding tags. Standalone tags not associated with a Release are left unchanged. Failed builds do not delete the current downloadable Release. A stale master run is prevented from overwriting the newest commit's Release.
