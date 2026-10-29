package sk.rucki.simple_commit.generators

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.util.io.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import sk.rucki.simple_commit.settings.SimpleCommitSettings

class ClaudeCliException(message: String) : Exception(message)

/**
 * Generates the commit message with `claude -p`. The diff goes in on stdin, the message comes back on stdout.
 */
class ClaudeCliCommitMessageGenerator(
    private val findClaude: () -> String? = ::findClaude,
    // Read on every generation, so changes in the settings apply right away
    private val loadSettings: () -> SimpleCommitSettings.State = { SimpleCommitSettings.getInstance().state.copy() },
) : ICommitMessageGenerator {

    override suspend fun generate(diff: String): String = withContext(Dispatchers.IO) {
        val settings = loadSettings()
        val claude = findClaude() ?: throw ClaudeCliException("'claude' was not found on PATH or in the usual install locations. Install Claude Code and log in.")

        val diffFile = FileUtil.createTempFile("simple-commit", ".diff", true)
        diffFile.writeText(diff.take(settings.maxDiffChars))

        // The prompt and the JSON go in files: on Windows, npm's claude.cmd runs through cmd.exe,
        // which cuts arguments at line breaks and mangles quotes
        val promptFile = FileUtil.createTempFile("simple-commit", ".txt", true)
        promptFile.writeText(settings.systemPrompt)
        // Speed: thinking first took ~3 s of the ~5 s API time. A commit message does not need it.
        val settingsFile = FileUtil.createTempFile("simple-commit", ".json", true)
        settingsFile.writeText("""{"alwaysThinkingEnabled":false}""")

        val commandLine = GeneralCommandLine(
            claude, "-p", "Write the commit message for the diff on stdin.",
            "--system-prompt-file", promptFile.path,
            "--model", settings.model.cliName,
            "--tools", "",                    // Claude only reads the diff, it must not run anything
            "--no-session-persistence",       // do not save every generation in the user's history
            "--setting-sources", "",          // do not load the user's settings, so their hooks do not fire
            "--settings", settingsFile.path,
            "--strict-mcp-config",            // speed: connecting to the user's MCP servers added ~2 s on every call
        )
            .withInput(diffFile)
            .withWorkDirectory(FileUtil.getTempDirectory())   // so no project's CLAUDE.md is loaded
            .withCharset(Charsets.UTF_8)

        val handler = CapturingProcessHandler(commandLine)
        try {
            // Interruptible, so cancelling the coroutine stops the waiting
            val output = runInterruptible { handler.runProcess(settings.timeoutSeconds * 1_000) }
            when {
                output.isTimeout -> throw ClaudeCliException("Claude did not answer within ${settings.timeoutSeconds} seconds.")
                output.exitCode != 0 -> throw ClaudeCliException("Claude failed: " + output.stdout.ifBlank { output.stderr }.trim())
                else -> output.stdout.trim()
            }
        } finally {
            handler.destroyProcess()   // kills claude when cancelled; does nothing when it has already finished
            diffFile.delete()
            promptFile.delete()
            settingsFile.delete()
        }
    }
}
