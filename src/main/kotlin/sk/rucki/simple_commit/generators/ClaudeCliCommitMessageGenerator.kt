package sk.rucki.simple_commit.generators

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.EnvironmentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import sk.rucki.simple_commit.settings.SimpleCommitSettings
import java.io.File

class ClaudeCliException(message: String) : Exception(message)

/**
 * Generates the commit message with `claude -p`. The diff goes in on stdin, the message comes back on stdout.
 */
class ClaudeCliCommitMessageGenerator(
    private val findClaude: () -> String? = ::findClaudeOnPath,
    // Read on every generation, so changes in the settings apply right away
    private val loadSettings: () -> SimpleCommitSettings.State = { SimpleCommitSettings.getInstance().state.copy() },
) : ICommitMessageGenerator {

    override suspend fun generate(diff: String): String = withContext(Dispatchers.IO) {
        val settings = loadSettings()
        val claude = findClaude() ?: throw ClaudeCliException("'claude' was not found on PATH. Install Claude Code and log in.")

        val diffFile = FileUtil.createTempFile("simple-commit", ".diff", true)
        diffFile.writeText(diff.take(settings.maxDiffChars))

        val commandLine = GeneralCommandLine(
            claude, "-p", "Write the commit message for the diff on stdin.",
            "--system-prompt", settings.systemPrompt,
            "--model", settings.model.cliName,
            "--tools", "",                    // Claude only reads the diff, it must not run anything
            "--no-session-persistence",       // do not save every generation in the user's history
            "--setting-sources", "",          // do not load the user's settings, so their hooks do not fire
            // Speed: thinking first took ~3 s of the ~5 s API time, and connecting to the user's
            // MCP servers added ~2 s on every call. A commit message needs neither.
            "--settings", """{"alwaysThinkingEnabled":false}""",
            "--strict-mcp-config",
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
        }
    }
}

/** Looks for `claude` on the PATH of the user's shell (an IDE started from the desktop may have a different PATH). */
fun findClaudeOnPath(): String? {
    val name = if (SystemInfo.isWindows) "claude.exe" else "claude"
    val path = EnvironmentUtil.getValue("PATH") ?: System.getenv("PATH") ?: return null
    return path.split(File.pathSeparator).map { File(it, name) }.firstOrNull { it.canExecute() }?.path
}
