# Development Notes

This document summarizes the engineering practices used in HourFlow and the standards expected for future changes.

## Principles

- Keep the app local-first. User data should stay on the device unless the user explicitly exports it.
- Keep Room as the single source of truth for persisted data.
- Keep UI state in ViewModels and expose it with `StateFlow`.
- Keep domain rules outside Compose screens when they can be tested independently.
- Prefer small, focused changes over broad refactors.
- Avoid introducing frameworks unless the app size and complexity justify them.

## Architecture Practices

- Compose screens should render state and send user events upward.
- ViewModels should coordinate repositories and expose screen-ready state.
- Repositories should hide DAO details from the rest of the app.
- Domain calculators should remain pure Kotlin when possible.
- Persistence models, domain models, and export DTOs should stay separate.

## Data And Migration Practices

- Add explicit Room migrations for schema changes.
- Keep exported Room schemas versioned in `app/schemas`.
- Preserve backup compatibility with older JSON versions whenever possible.
- Do not recalculate historical months when current rates change.
- Treat money as integer cents instead of floating-point values.

## Testing Practices

- Add unit tests for domain calculations and import/export behavior.
- Keep backup/restore tests focused on round-trip safety and backward compatibility.
- Run the full local validation before release:

```bash
./gradlew test
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

On Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

## Release Practices

- Normal commits should not change the app version.
- Release commits should use one of the version bump tasks:

```bash
./gradlew bumpPatch
./gradlew bumpMinor
./gradlew bumpMajor
```

- `VERSION_CODE` must always increase for a release.
- `VERSION_NAME` uses `MAJOR.MINOR.PATCH`.
- GitHub Actions creates the release automatically when a new `VERSION_NAME` reaches `main`.

## Files That Must Stay Private

Do not commit local signing or machine-specific files:

- `local.properties`
- `keystore.properties`
- `*.jks`
- `*.keystore`
- generated APKs/AABs
- Gradle or Android Studio build directories
