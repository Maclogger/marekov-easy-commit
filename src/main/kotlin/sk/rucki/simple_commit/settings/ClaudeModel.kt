package sk.rucki.simple_commit.settings

/**
 * Models offered in the settings. [cliName] is passed to `claude --model`;
 * the aliases always point to the newest version of each model.
 */
enum class ClaudeModel(val cliName: String, val displayName: String) {
    HAIKU("haiku", "Haiku - fastest"),
    SONNET("sonnet", "Sonnet"),
    OPUS("opus", "Opus - best, slowest"),
}
