# floating-helper

Android floating/snippet helper frontend module.

The current implementation is a normal Activity-based prototype, not an overlay.
It loads persisted `DroidJaxState` through `:android-common`, renders grouped
snippets from `:core`, supports search, persists delimiter profile changes,
records recent snippet usage, inserts snippets into an internal composer, moves
through placeholder positions for the last inserted snippet, and copies composed
TeX to the clipboard.

Overlay, accessibility, and app-specific behavior should remain outside `:core`.
