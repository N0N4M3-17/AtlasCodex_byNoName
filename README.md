# AtlasCodex_byNoName

Atlas Library is a local-first Java 21 + JavaFX desktop application for worldbuilding. It combines:

- Infinite visual map workspace
- Rich linked codex articles
- Internal/external anchor navigation
- Calendar and timeline systems

## Current implementation target

- **Version:** `v0.0.1aa`
- **Phase:** Foundation MVP (Phase 1 baseline)
- **Primary outcome:** a user can build a small world map with linked articles and reopen it reliably.

## Versioning policy (draft)

- Core semantic pattern: `MAJOR.MINOR.PATCH`.
- Required push identifier suffix: two lowercase letters (`aa` to `zz`) appended to every build/release marker.
  - Example: `v0.0.1aa`, `v0.0.1ab`, ..., `v0.0.1az`, `v0.0.1ba`.
- Automatic upward movement:
  - Patch and suffix progression should advance every **5 to 10 pushes** (configurable threshold per branch policy).
  - The two-letter suffix remains mandatory and acts as a unique push-readable identifier.

See `docs/v0.0.1aa-foundation-spec.md` for the detailed scope.
