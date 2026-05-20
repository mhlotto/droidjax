# keyboard-ime

Android IME frontend module.

The current implementation is a compact `InputMethodService` keyboard surface.
It loads persisted `DroidJaxState` through `:android-common` and provides:

- all/favorites/recents modes
- category chips
- snippet keys from the active catalog
- active delimiter profile toggle
- space, enter, and backspace keys
- long-press favorite toggles
- recent usage recording after successful commits

Snippet keys consume `:core` `InsertOperation` values and apply them through
`InputConnectionInsertAdapter`.
