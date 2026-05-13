# AtlasCodex_byNoName

Atlas Library is a local-first Java desktop application for worldbuilding.

## Current Target

- Version: `v0.0.1ab`
- Focus: Phase 1 interactive baseline with editable nodes, layer/object controls, and project persistence.

## Implemented in this build

- Windowed startup at ~70% of usable screen space (not fullscreen by default)
- Infinite-style whiteboard canvas with grid background, pan (middle/secondary drag), zoom (scroll), node selection and drag-move
- Add Text (editable via inspector: content, opacity, font size)
- Add Image (file-browser import from local disk into canvas)
- Layer list + object list basics
- Codex raw editor + CommonMark preview
- `.atlaslib` save/open with `manifest.json` + `db/project.sqlite`
- SQLite persistence for layers/nodes/articles/anchors

## Run locally

```bash
mvn clean compile
mvn javafx:run
```

## IntelliJ run target

Run `com.atlaslibrary.app.Launcher` as the main class.

## JDK 25 warning note

When running JavaFX 21 on JDK/JBR 25, warning lines about restricted/native access and `sun.misc.Unsafe` may appear.
They are runtime warnings from JavaFX internals; for cleaner logs in IDE, add VM option:

```text
--enable-native-access=ALL-UNNAMED
```
