<p align="center">
  <img src="./assets/readme/hero.png" width="100%" alt="Syndicate app icon and article list — an offline-first RSS and Atom reader for Android">
</p>

Syndicate is a native Android reader for RSS and Atom feeds. Articles are stored locally, read state is kept per article, and feeds can be grouped into folders so you can follow what matters without depending on a network connection.

## Features

- Add feeds by URL (RSS 2.0 and Atom), with duplicate detection and a fetch preview
- Group feeds with multiple tags per feed
- Unread badges scoped to the current focus: all articles, one feed, or a tag
- Mark a single article read, or mark all articles in the current focus read
- Heart articles in the reader and find them again in the Favourites category
- Manual refresh, plus optional refresh on startup
- Sort articles, feeds, and tags by name, date, or unread count
- In-app reader with font and size options, and a link to the original page
- Light, dark, and system themes with Material You dynamic color on supported devices
- Export and import subscriptions as OPML

## Install

Requires JDK 17 and Android SDK platform 37.

```bash
git clone https://github.com/russelldickenson/syndicate.git
cd syndicate
./gradlew :app:assembleDebug
```

Install the debug APK on a connected device or emulator:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests with `./gradlew testDebugUnitTest`.

## Authors

- [Russell Dickenson](https://github.com/russelldickenson)

Agentic coding tools were used for coding.

## License

Syndicate is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).
