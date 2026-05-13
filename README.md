# AtlasCodex_byNoName

Atlas Library is a local-first Java desktop application for worldbuilding.

## Current Target

- Version: `v0.0.1aa`
- Focus: Foundation Phase 1 baseline and core UI scaffolding.

## Included in this baseline

- Stylized JavaFX desktop shell (top command bar, left layers/object panel, center viewport, right inspector/codex, bottom status bar)
- Fullscreen-ready main window
- Canvas/interaction guidance scaffold for pan/zoom and selection model
- Codex raw/rendered editor placeholders
- Version-tracked application label (`v0.0.1aa`)

## Run locally

```bash
mvn clean compile
mvn javafx:run
```

## Next implementation slices

- Project bundle create/open/save (`.atlaslib`)
- SQLite schema + migrations
- Image/text node placement and transforms
- Anchor creation and navigation


## IntelliJ run target

Run `com.atlaslibrary.app.Launcher` as the main class. This avoids the classpath JavaFX launcher error (`JavaFX runtime components are missing`) that can happen when running `Application` subclasses directly in some IDE configurations.
