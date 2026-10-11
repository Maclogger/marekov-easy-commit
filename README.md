# Simple_commit

IntelliJ Platform plugin that generates a commit message with a single click using the Claude subscription.

The plugin adds a button to the commit window toolbar. Clicking it builds a diff of the changes checked for commit,
sends it to Claude and writes the generated message into the commit message field. Clicking it again while
generating cancels the generation.

## Development

- **Run IDE with Plugin** (`.run/runIde.run.xml`) – starts a sandbox IDE with the plugin installed.
- **Run Tests** / **Run Verifications** – run `check` and `verifyPlugin`.

The animated toolbar icons in `src/main/resources/icons/` are SVG frames played by `AnimatedIcon`
(see `SimpleCommitIcons.kt`).
