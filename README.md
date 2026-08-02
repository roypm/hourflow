# HourFlow

![Android](https://img.shields.io/badge/Android-Kotlin-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![CI](https://github.com/roypm/hourflow/actions/workflows/android.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

HourFlow is a local-first Android app for tracking personal work hours, monthly rates, extra income, and shareable monthly summaries.

The project is built with Kotlin, Jetpack Compose, Material 3, Room, DataStore Preferences, Navigation Compose, and kotlinx.serialization. It is intentionally single-user and offline-first: all work data stays on the device unless the user exports it.

## Download and update

Download the latest APK from [GitHub Releases](https://github.com/roypm/hourflow/releases/latest).

HourFlow does not install updates automatically. Before installing an APK manually, export a JSON backup from **Settings > Backup**. If Android does not accept the new APK as an update because its signing key is different, uninstall the old version, install the new APK, and import the backup again. The backup includes jobs, shifts, rates, extra money, weekly templates, and monthly templates.

The app includes a direct link to the latest GitHub Releases page in **Settings > Updates**.

## Status

Personal Android project focused on local-first work-hour tracking. The app is functional and distributed through GitHub Releases, with downloadable APKs attached to each release.

## Screenshots

Screenshots use sample data.

<table>
  <tr>
    <th>Jobs</th>
    <th>Month</th>
    <th>Shift</th>
  </tr>
  <tr>
    <td><img src="docs/screenshots/01-jobs-overview.png" width="220" alt="Jobs overview"></td>
    <td><img src="docs/screenshots/02-month-calendar.png" width="220" alt="Month overview"></td>
    <td><img src="docs/screenshots/03-shift-editor.png" width="220" alt="Shift editor"></td>
  </tr>
</table>

<table>
  <tr>
    <th>Summary</th>
    <th>Settings</th>
    <th>Light Mode</th>
  </tr>
  <tr>
    <td><img src="docs/screenshots/04-summary-export.png" width="220" alt="Summary export"></td>
    <td><img src="docs/screenshots/05-settings-backup.png" width="220" alt="Settings and backup"></td>
    <td><img src="docs/screenshots/06-light-mode.png" width="220" alt="Light mode"></td>
  </tr>
</table>

<table>
  <tr>
    <th>Exported JPEG</th>
  </tr>
  <tr>
    <td><img src="docs/screenshots/07-exported-jpeg.jpg" width="420" alt="Exported JPEG summary"></td>
  </tr>
</table>

## Features

- Track multiple independent jobs.
- Log one or more shifts per day from a monthly calendar view.
- Support night shifts that cross midnight.
- Store monthly hourly rates without recalculating previous months.
- Add positive or negative extra money entries per month.
- Reuse weekly and monthly work templates.
- View monthly and yearly summaries.
- Export monthly summaries as PDF or JPEG.
- Export and restore full JSON backups.
- Switch between system, light, and dark themes.
- Use Spanish, English, or Catalan UI strings.

## Engineering Highlights

- Local-first architecture with Room as the single source of truth.
- Repository + ViewModel + StateFlow separation.
- JSON backup/restore designed for round-trip data portability.
- Historical monthly rates so future rate changes do not recalculate past work.
- Night-shift handling for work sessions that cross midnight.
- CI workflow with unit tests, validation APK artifacts, tagged release APKs, checksums, and automatic GitHub Releases.

## Privacy

HourFlow stores data locally on the device. Core tracking and export features do not require accounts, servers, analytics, or network access. Network access is only needed if the user chooses to open the GitHub Releases page for an update.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room / SQLite
- DataStore Preferences
- Navigation Compose
- kotlinx.serialization
- JUnit
- Gradle Wrapper

## Build

Requirements:

- JDK 17
- Android SDK with API 34

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

Build a release APK:

```bash
./gradlew :app:assembleRelease
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

APK outputs:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

Release builds use `keystore.properties` when present. If that file is not present, the project falls back to debug signing so anyone can clone and build the app locally without private signing keys.

## GitHub Actions

The Android workflow runs tests on pushes to `main` and pull requests. It builds a validation debug APK and uploads it as a workflow artifact.

A GitHub Release is created only when a version tag such as `v1.1.0` is pushed. The workflow validates that the tag matches `VERSION_NAME`, builds one versioned release APK, creates a SHA-256 checksum, attaches both files to the Release, and generates release notes with the backup instructions.

Version-specific release notes are stored in `docs/releases/`, for example `docs/releases/v1.1.0.md`, and are included automatically when the matching tag is published.

The release build uses `keystore.properties` when present. Without that file, it falls back to debug signing so the project can still be built without private signing material. Because the CI runner may use a different debug key, users should always export a JSON backup before installing a release APK.

Version bumps are automated with Gradle tasks:

```bash
./gradlew bumpPatch
./gradlew bumpMinor
./gradlew bumpMajor
```

Each bump increments `VERSION_CODE` and updates `VERSION_NAME` in `gradle.properties`.

## Release flow

The complete release checklist, including automatic and manual publication, is documented in [RELEASING.md](RELEASING.md).

The short automatic flow is:

```bash
./gradlew bumpMinor
git add gradle.properties
git commit -m "Release 1.1.0"
git push origin main
git tag v1.1.0
git push origin v1.1.0
```

Use `bumpPatch` for fixes, `bumpMinor` for features, and `bumpMajor` for breaking changes. Always replace the example version with the value written to `gradle.properties`.

## Tests

Run unit tests:

```bash
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

## Architecture

HourFlow follows a simple Repository + ViewModel + StateFlow architecture:

```text
UI Layer (Compose Screens)
    <-> StateFlow
ViewModel Layer
    <-> suspend / Flow
Repository Layer
    <-> DAO
Room Database (SQLite)
```

The app keeps Room as the single source of truth. ViewModels expose screen state with `StateFlow`, repositories hide persistence details, and export/import logic lives in the data layer.

More detail: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

Development practices: [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)

## License

MIT. See [LICENSE](LICENSE).
