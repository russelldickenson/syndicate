# Contributing to Syndicate

Thanks for your interest in Syndicate, a native Android reader for RSS and Atom feeds. Bug reports, fixes, features, and documentation improvements are all welcome. This guide covers what you need to get the app running and how changes are usually made here.

## Before you start

For a non-trivial change, open an issue first and describe what you want to do. That gives maintainers a chance to agree on the approach before you spend time on code. For a small fix or a documentation edit, a pull request is fine on its own.

When reporting a bug, include the steps to reproduce, expected and actual behavior, your Android version, and the app version.

## Getting set up

### Prerequisites

- JDK 17
- Android SDK platform 37 (compile and target SDK; the app supports API 24 and above)
- Android Studio, or a terminal with the Android SDK installed

The repository includes the Gradle wrapper (9.5.1), so a separate Gradle installation is not needed. `local.properties` stores your local SDK path and is gitignored.

### Build and run

```bash
git clone https://github.com/russelldickenson/syndicate.git
cd syndicate
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

You can also open the root project in Android Studio and run the `app` configuration.

## How the project is organized

Syndicate is a multi-module, feature-first Kotlin project:

- `app` — `SyndicateApp`, `MainActivity`, the Hilt graph, navigation wiring, and app resources
- `feature:feeds`, `feature:articles`, `feature:reader`, `feature:settings` — Compose screens, ViewModels, and Navigation 3 destinations
- `core:domain` — pure Kotlin models and repository interfaces, with no Android or Compose dependencies
- `core:data` — repository implementations, mappers, and OPML import/export
- `core:network` — feed fetching with OkHttp and XML parsing
- `core:database` — Room entities, DAOs, and migrations
- `core:datastore` — persisted user settings
- `core:ui` — shared Compose components, theme, and formatting utilities
- `core:common` — coroutine dispatchers; `core:testing` — shared unit-test dependencies
- `build-logic/convention` — Gradle convention plugins shared by modules

Room is the single source of truth. Feeds are fetched and parsed in `core:network`, then upserted without overwriting read state. Unread counts come from SQL queries exposed as `Flow`, so badges update automatically.

## Making a change

- Keep one concern per pull request and respect module boundaries: UI goes in `feature:*` or `core:ui`, business rules in `core:domain` and `core:data`.
- Add dependencies to `gradle/libs.versions.toml` and reference them through the version catalog instead of hardcoding versions in module scripts.
- Put shared build configuration in `build-logic/convention`, not in individual module files.
- Add or update tests for changed behavior. Unit tests live in `src/test/kotlin` inside the relevant module.
- Follow the existing package root, `com.zigzag.syndicate.*`.

## Before you open a pull request

Run these from the repository root:

```bash
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest
./gradlew detekt
./gradlew :app:lint
```

Fix new warnings rather than suppressing them. If you change the UI, include a screenshot or short recording in the pull request description.

## Commit messages

The project uses [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/): a lowercase type, an optional scope, and a short imperative description under 72 characters with no trailing period.

```text
feat(feeds): add feed search
fix(settings): collapse duplicate font options
style(reader): align back arrow with article text
docs: expand contributor guide
```

Mark breaking changes with `!` after the type or scope and add a `BREAKING CHANGE:` footer explaining the impact.

## Pull requests

Open pull requests against `main`. Explain what changed and why, link any related issue, and confirm that the checks above pass. Smaller, focused pull requests are reviewed more quickly than large mixed ones.

## License

Syndicate is released under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html). By contributing, you agree that your contributions are licensed under the same terms.
