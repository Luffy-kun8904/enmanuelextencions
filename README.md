# Aniyomi Manga Extensions — Community Repository

A small, maintainable source repository for **Aniyomi manga extensions**.

> This repository is an independent project and is not affiliated with Aniyomi or the
> content providers used by its extensions.

## Included source

### MangaDex Safe (English + Spanish)

The included source uses the public MangaDex API and deliberately requests only
`safe` content ratings and the `en` / `es` translated-language variants.

The source implements:

- Popular manga
- Latest updates
- Text search
- Manga details
- Chapter listing
- Reader page resolution through MangaDex's At-Home service
- English and Spanish chapters
- Safe-content filtering
- Pagination
- Basic HTTP headers and error handling

## Repository layout

```text
src/
└── all/
    └── mangadexsafe/
        ├── build.gradle
        ├── AndroidManifest.xml
        ├── res/
        └── src/
            └── eu/kanade/tachiyomi/extension/all/mangadexsafe/
                └── MangaDexSafe.kt
```

## Building locally

Requirements:

- JDK 17
- Android SDK / build tools
- Git

Then:

```bash
chmod +x gradlew
./gradlew spotlessCheck
./gradlew -p src :src:all:mangadexsafe:assembleRelease
```

The GitHub Actions workflow is configured to perform the same checks and build the
release APK automatically.

## Publishing

The repository contains a publishing workflow and a repository-generation script.
After configuring the signing secrets, a push to `master` builds the APK and creates
the `repo` branch contents.

The resulting repository URL is:

```text
https://raw.githubusercontent.com/YOUR-USER/YOUR-REPO/refs/heads/repo/index.min.json
```

Replace `YOUR-USER/YOUR-REPO` with your GitHub repository.

## Important compatibility note

The uploaded base repository was an **anime-extension** project and was archived
upstream. It did not contain manga sources. This project therefore keeps its useful
Gradle/build infrastructure but converts the application metadata and source module
to a manga extension.

The current Aniyomi codebase is actively evolving its extension API, so the included
build targets the stable extension-library line represented by the supplied base
rather than guessing at an unreleased API contract.

## Verification

See `docs/VERIFICATION.md` for the checks performed while preparing this package.
