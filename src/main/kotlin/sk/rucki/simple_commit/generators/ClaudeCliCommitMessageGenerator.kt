package sk.rucki.simple_commit.generators

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.EnvironmentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ClaudeCliException(message: String) : Exception(message)

private const val MAX_DIFF_CHARS = 100_000

private const val SYSTEM_PROMPT = """You write git commit messages.
- First line: a concise summary in the imperative mood, at most 72 characters.
- For larger changes add a blank line and a short body as "- " bullet points.
- Output ONLY the commit message. No explanations, no quotes, no Markdown code blocks."""

/**
 * Generates the commit message with `claude -p`. The diff goes in on stdin, the message comes back on stdout.
 */
class ClaudeCliCommitMessageGenerator(
    private val findClaude: () -> String? = ::findClaudeOnPath,
    private val model: String = "haiku",
    private val timeout: Duration = 30.seconds,
) : ICommitMessageGenerator {

    override suspend fun generate(diff: String): String = withContext(Dispatchers.IO) {
        val claude = findClaude() ?: throw ClaudeCliException("'claude' was not found on PATH. Install Claude Code and log in.")

        val diffFile = FileUtil.createTempFile("simple-commit", ".diff", true)
        diffFile.writeText(diff.take(MAX_DIFF_CHARS))

        val commandLine = GeneralCommandLine(
            claude, "-p", "Write the commit message for the diff on stdin.",
            "--system-prompt", SYSTEM_PROMPT,
            "--model", model,
            "--tools", "",                    // Claude only reads the diff, it must not run anything
            "--no-session-persistence",       // do not save every generation in the user's history
            "--setting-sources", "",          // do not load the user's settings, so their hooks do not fire
        )
            .withInput(diffFile)
            .withWorkDirectory(FileUtil.getTempDirectory())   // so no project's CLAUDE.md is loaded
            .withCharset(Charsets.UTF_8)

        val handler = CapturingProcessHandler(commandLine)
        try {
            // Interruptible, so cancelling the coroutine stops the waiting
            val output = runInterruptible { handler.runProcess(timeout.inWholeMilliseconds.toInt()) }
            when {
                output.isTimeout -> throw ClaudeCliException("Claude did not answer within ${timeout.inWholeSeconds} seconds.")
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
