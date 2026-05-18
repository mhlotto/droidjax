# keyboard-ime

Android IME frontend module.

The current implementation is a minimal proof of concept `InputMethodService`
with a few snippet buttons. It loads persisted `DroidJaxState` through
`:android-common`, consumes `:core` `InsertOperation` values, applies them
through `InputConnectionInsertAdapter`, and records recent snippet usage after
successful commits.
