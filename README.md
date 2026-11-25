# Syndicate

A native Android RSS reader built with Kotlin, Jetpack Compose, Material 3, Room 3, Hilt and Navigation 3.

## Features

- Add feeds by URL (RSS 2.0 and Atom), with duplicate detection and fetch preview
- Group feeds with multiple tags per feed (many-to-many)
- Unread badges scoped to the current focus: all articles, individual feeds, and tags
- Mark a single article read, or mark all read for the current focus
- Manual refresh (feed list action) and optional refresh on startup
- Sort articles (newest, oldest, title, feed), feeds (name, unread, last updated) and tags (name, unread)
- Light, dark and system themes with Material You dynamic color on supported devices
- In-app article reader with an action to open the original page in a browser

## Architecture

Multi-module, feature-first:

- `app` — navigation, DI graph, startup refresh
- `feature:feeds`, `feature:articles`, `feature:reader`, `feature:settings` — UI and ViewModels
- `core:domain` — models and repository interfaces (pure Kotlin)
- `core:data`, `core:network`, `core:database`, `core:datastore` — offline-first data layer
- `core:ui`, `core:common`, `core:testing` — shared UI, utilities and test doubles
- `build-logic` — convention plugins

Room is the single source of truth. Feed XML is fetched with OkHttp and parsed with the platform DOM parser; parsed articles are upserted into Room without overwriting read state. Unread counts are derived from SQL queries and exposed as `Flow`, so badges update automatically.

## Build

Requires JDK 17 and Android SDK platform 37.

```bash
./gradlew help
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest
```

Install the debug APK from `app/build/outputs/apk/debug/`.

## Permissions

`INTERNET` for feed fetching and `ACCESS_NETWORK_STATE` to skip refresh while offline.
