# Marekov Easy Commit

IntelliJ Platform plugin that generates a commit message with a single click, using Claude through the
[Claude Code](https://claude.com/claude-code) CLI and your own Claude subscription.

The plugin adds a button to the commit window toolbar. Clicking it builds a diff of the changes checked for commit,
sends it to `claude -p` and writes the generated message into the commit message field. Clicking it again while
generating cancels the generation. The model, prompt, timeout and maximum diff size are configurable in
**Settings | Tools | Marekov Easy Commit**.

Requires Claude Code installed (`claude` on the PATH) and logged in.

## Development

Build with JDK 21 (`JAVA_HOME` must point to a full JDK, not just a Java runtime).

- **Run IDE with Plugin** (`.run/runIde.run.xml`) – starts a sandbox IDE with the plugin installed.
- `./gradlew test` – runs the tests.
- `./gradlew verifyPlugin` – checks compatibility with the IntelliJ Plugin Verifier.
- `./gradlew buildPlugin` – builds `build/distributions/marekov-easy-commit-<version>.zip`.

## Releasing

1. Raise `version` in `gradle.properties` and describe the changes in `CHANGELOG.md` under a new `## [x.y.z]` heading.
2. `./gradlew test verifyPlugin buildPlugin`
3. `PUBLISH_TOKEN=<token> ./gradlew publishPlugin` (the very first version must be uploaded by hand on the Marketplace website).
