# Releasing HourFlow

This document describes how to publish HourFlow through GitHub Releases. The repository supports an automatic GitHub Actions flow and a manual fallback flow.

## Release policy

- Releases use semantic version names such as `1.1.0`.
- Git tags use the `v` prefix: `v1.1.0`.
- `VERSION_NAME` is the user-facing version.
- `VERSION_CODE` is the Android update number and must increase on every release.
- A feature release normally uses `bumpMinor`; a bug-fix release uses `bumpPatch`.
- Releases contain one versioned APK and a `SHA256SUMS.txt` file.
- The app does not install updates automatically. Users are instructed to export a JSON backup before installing manually.

The current project intentionally does not require a private release keystore. If `keystore.properties` is absent, Gradle falls back to debug signing. This keeps the project buildable, but a CI runner can generate a different debug key. Therefore, a user may need to uninstall the previous APK and restore the JSON backup after installing a new release.

## Before publishing

1. Finish the feature work and review the UI manually.
2. Test the backup flow with jobs, shifts, rates, extra money, weekly templates, and monthly templates.
3. Verify PDF and JPEG exports, including long documents and logos.
4. Run the local checks:

   ```bash
   ./gradlew test
   ./gradlew :app:assembleDebug
   ```

5. Check that the working tree contains only intended changes:

   ```bash
   git diff --check
   git status
   ```

6. Create the version-specific release notes file before creating the tag:

   ```text
   docs/releases/v1.1.0.md
   ```

   The tagged workflow fails if this file is missing. GitHub Actions combines it with the installation instructions and GitHub's generated change history.

## Automatic publication with GitHub Actions

This is the normal release path.

### 1. Bump the version

From the repository root, choose the appropriate task:

```bash
./gradlew bumpPatch   # 1.1.0 -> 1.1.1
./gradlew bumpMinor   # 1.1.0 -> 1.2.0
./gradlew bumpMajor   # 1.1.0 -> 2.0.0
```

On Windows PowerShell:

```powershell
.\gradlew.bat bumpMinor
```

Read `gradle.properties` and note the exact new `VERSION_NAME` and `VERSION_CODE`.

### 2. Commit the version

```bash
git add gradle.properties docs/releases/v1.1.0.md
git commit -m "Release 1.1.0"
git push origin main
```

Replace `1.1.0` with the actual version.

The push to `main` runs tests and creates a validation debug APK artifact. It does not publish a Release.

### 3. Create and push the matching tag

```bash
git tag v1.1.0
git push origin v1.1.0
```

The tag must exactly match `VERSION_NAME`. For example, `VERSION_NAME=1.1.0` requires the tag `v1.1.0`. The workflow fails before publishing if they differ.

### 4. Verify the workflow

In GitHub, open **Actions > Android** and wait for the tagged run to finish. It will:

1. Run the JVM tests.
2. Build `app-release.apk`.
3. Rename it to `HourFlow-1.1.0.apk`.
4. Generate `SHA256SUMS.txt`.
5. Create the GitHub Release for `v1.1.0`.
6. Attach the APK and checksum.
7. Add the backup/install instructions and generated release notes.

### 5. Check the published Release

Open:

```text
https://github.com/roypm/hourflow/releases/latest
```

Confirm that the Release contains:

- `HourFlow-1.1.0.apk`
- `SHA256SUMS.txt`
- The correct version and changelog
- The JSON backup warning

## Manual publication fallback

Use this process if GitHub Actions is unavailable or if you want to inspect the APK before uploading it.

### 1. Update and verify the version

Run the version bump task locally, then confirm `VERSION_NAME` and `VERSION_CODE` in `gradle.properties`.

### 2. Build the APK

```bash
./gradlew test
./gradlew :app:assembleRelease
```

On Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat :app:assembleRelease
```

Copy and rename the output using the exact version:

```text
app/build/outputs/apk/release/app-release.apk
→ HourFlow-1.1.0.apk
```

### 3. Generate the checksum

On Linux/macOS/Git Bash:

```bash
sha256sum HourFlow-1.1.0.apk > SHA256SUMS.txt
```

On Windows PowerShell:

```powershell
(Get-FileHash .\HourFlow-1.1.0.apk -Algorithm SHA256).Hash + "  HourFlow-1.1.0.apk" | Set-Content SHA256SUMS.txt
```

### 4. Create the GitHub Release

1. Push the version commit to `main`.
2. Open the repository on GitHub and choose **Releases > Draft a new release**.
3. Create or select the tag `v1.1.0` on the correct commit.
4. Set the title to `HourFlow 1.1.0`.
5. Attach `HourFlow-1.1.0.apk` and `SHA256SUMS.txt`.
6. Add the contents of `docs/releases/v1.1.0.md` to the release notes, together with the installation instructions:

   ```markdown
   ## Installation

   Before installing this APK, export a JSON backup from Settings.

   If Android does not allow the previous version to be updated because the APK signing key is different:

   1. Export the JSON backup.
   2. Uninstall HourFlow.
   3. Install the downloaded APK.
   4. Open Settings and import the JSON backup.

   ## Changes

   - Describe new features here.
   - Describe fixes here.
   ```

7. Mark it as a pre-release only if it is a beta or test build.
8. Publish the Release.

## Post-release verification

1. Download the APK from the published Release, not from the Actions artifact.
2. Verify the checksum if needed.
3. Install it on a test device.
4. Open the app and check the version.
5. Test Settings > Updates and the GitHub Releases link.
6. Test backup export and import.
7. Confirm that the Release page is the one linked by the app.

## Rollback

Do not reuse a published tag for a different APK. If a release is broken:

1. Mark the broken Release as a pre-release or add a warning.
2. Fix the problem.
3. Increase both `VERSION_NAME` and `VERSION_CODE`.
4. Publish a new patch release, for example `v1.1.1`.

Android uses the increasing `VERSION_CODE` to distinguish newer installations, so a corrected release must not reuse the previous code.
