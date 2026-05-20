# floating-helper

Android floating/snippet helper frontend module.

The current implementation is a normal Activity-based helper, not an overlay. It
loads persisted `DroidJaxState` through `:android-common` and provides:

- snippet search
- all/favorites/recents modes
- category filters
- favorite toggles
- active delimiter profile switching
- internal TeX composer
- next-placeholder navigation for the last inserted snippet
- copy and clear actions

Overlay, accessibility, and app-specific behavior should remain outside `:core`.
