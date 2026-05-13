# AtlasCodex_byNoName

Atlas Library is a local-first Java desktop application for worldbuilding.

## Current Target

- Version: `v0.0.1aa`
- Focus: Phase 1 functional baseline (map + codex + persistence).

## Implemented in this build

- JavaFX desktop shell with stylized panels and fullscreen toggle
- Infinite-style canvas interactions: pan, zoom, click selection
- Node basics: add text nodes and image placeholder nodes
- Layer/object panels
- Minimal codex raw editor + CommonMark text preview
- Anchor baseline: internal anchor creation to article IDs
- `.atlaslib` bundle save/open with `manifest.json` + `db/project.sqlite`
- SQLite persistence for layers, nodes, articles, anchors

## Run locally

```bash
mvn clean compile
mvn javafx:run
```

## IntelliJ run target

Run `com.atlaslibrary.app.Launcher` as the main class.
