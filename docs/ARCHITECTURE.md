# HourFlow Architecture

This document records the main architectural and product decisions behind HourFlow, a personal Android app for tracking work hours.

## Project Scope

HourFlow is a single-user, local-first Android application. It is not a general finance app, a multi-user platform, or a cross-platform product.

## Core Stack

| Layer | Choice | Reason |
| --- | --- | --- |
| Language | Kotlin | Modern Android-first language with null safety |
| UI | Jetpack Compose + Material 3 | Declarative UI and current Android design system |
| Persistence | Room / SQLite | Type-safe local database and single source of truth |
| Navigation | Navigation Compose | Compose-native screen navigation |
| Preferences | DataStore Preferences | Coroutine-friendly persisted settings |
| Serialization | kotlinx.serialization | Kotlin-native JSON backup and restore |
| Architecture | Repository + ViewModel + StateFlow | Clear flow from data to UI state |
| Dependency Injection | Manual | Small app; no framework overhead needed |

## Data Model

### Trabajo

Top-level job entity.

- Flat hierarchy; no nested projects or categories.
- Jobs are independent from each other.
- Stores name, user name, currency symbol, and optional photo URI.

### EntradaHoras

Work-hour entry for a single shift.

- One entry represents one shift with start and end time.
- Multiple shifts can exist on the same day.
- Night shifts use an explicit `esDiaSiguiente` flag.
- Shifts that cross month boundaries belong to the month where they start.
- Entries can include notes and an optional custom hourly rate.

### TarifaMensual

Monthly hourly rate for a job.

- Stored per job and month.
- A month can inherit the previous configured rate.
- Past months are not recalculated when future rates change.

### DineroExtra

Additional money entry for a job and month.

- Stores amount, description, and date.
- Amounts may be positive or negative.

## Export Strategy

| Format | Purpose | Reimportable |
| --- | --- | --- |
| JSON | Full backup and restore | Yes |
| PDF | Formal printable monthly summary | No |
| JPEG | Fast sharing preview | No |

Exports are per job. The app does not combine data from different jobs.

## UX Principles

- Monthly grid first: users tap a day and log hours directly.
- Additional shifts are only shown when needed.
- Notes are hidden by default.
- Weekly totals appear at week boundaries.
- Partial weeks at month boundaries are shown without duplicating data.
- Year navigation uses a 12-month grid.

## Theme And Locale

- Theme modes: system, light, and dark.
- Theme preference is persisted with DataStore.
- UI strings are available in Spanish, English, and Catalan.

## Build

The project uses the Gradle Wrapper:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew test
```

Release signing uses a local `keystore.properties` file when present. The file is intentionally ignored and not part of the public repository.

## CI And Releases

GitHub Actions validates pushes and pull requests by running unit tests and building debug and release APKs.

Each run uploads the generated APKs as workflow artifacts. Pushes to `main` read `VERSION_NAME` from `gradle.properties` and publish a GitHub Release automatically when that version tag does not already exist. Version tags that start with `v`, such as `v1.3.0`, can also publish a release manually.

Release version changes are handled through Gradle tasks:

```bash
./gradlew bumpPatch
./gradlew bumpMinor
./gradlew bumpMajor
```

Each task increments `VERSION_CODE` and updates `VERSION_NAME` in `gradle.properties`.
