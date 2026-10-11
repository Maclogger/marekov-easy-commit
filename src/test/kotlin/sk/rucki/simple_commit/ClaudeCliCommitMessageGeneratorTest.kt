package sk.rucki.simple_commit

import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import sk.rucki.simple_commit.generators.ClaudeCliCommitMessageGenerator
import sk.rucki.simple_commit.generators.ClaudeCliException
import sk.rucki.simple_commit.settings.ClaudeModel
import sk.rucki.simple_commit.settings.SimpleCommitSettings
import java.io.File
import kotlin.io.path.createTempDirectory

/** Runs the generator against a shell script pretending to be `claude`, so the real Claude is never called. */
class ClaudeCliCommitMessageGeneratorTest : BasePlatformTestCase() {

    private lateinit var dir: File

    override fun setUp() {
        super.setUp()
        dir = createTempDirectory("fake-claude").toFile()
    }

    override fun tearDown() {
        try {
            dir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    override fun shouldRunTest(): Boolean = !SystemInfo.isWindows && super.shouldRunTest()

    fun `test sends the diff on stdin and returns the message`() {
        fakeClaude("""printf '%s\n' "$@" > "${'$'}DIR/args"; cat > "${'$'}DIR/stdin"; echo "  feat: change a  """")

        assertEquals("feat: change a", generate("+val a = 2\n"))
        assertEquals("+val a = 2\n", File(dir, "stdin").readText())

        val args = File(dir, "args").readLines()
        assertEquals("-p", args[0])
        assertEquals("", args[args.indexOf("--tools") + 1])
        assertEquals("", args[args.indexOf("--setting-sources") + 1])
    }

    fun `test uses the model, prompt and diff limit from the settings`() {
        fakeClaude("""printf '%s\n' "$@" > "${'$'}DIR/args"; cat > "${'$'}DIR/stdin"; echo msg""")
        val settings = SimpleCommitSettings.State(systemPrompt = "Be brief.", model = ClaudeModel.SONNET, maxDiffChars = 5)

        runBlocking { generator(settings).generate("1234567890") }

        val args = File(dir, "args").readLines()
        assertEquals("sonnet", args[args.indexOf("--model") + 1])
        assertEquals("Be brief.", args[args.indexOf("--system-prompt") + 1])
        assertEquals("12345", File(dir, "stdin").readText())
    }

    fun `test failure reports what claude printed`() {
        fakeClaude("""cat > /dev/null; echo "Not logged in"; exit 1""")

        assertEquals("Claude failed: Not logged in", assertFails().message)
    }

    fun `test missing claude is reported`() {
        val error = runBlocking {
            runCatching { ClaudeCliCommitMessageGenerator(findClaude = { null }).generate("diff") }.exceptionOrNull()
        }

        assertInstanceOf(error, ClaudeCliException::class.java)
    }

    fun `test timeout is reported`() {
        fakeClaude("exec sleep 60")

        assertTrue(assertFails().message!!.contains("did not answer"))
    }

    fun `test cancelling kills claude`() = runBlocking {
        fakeClaude("""echo ${'$'}${'$'} > "${'$'}DIR/pid"; exec sleep 60""")

        val generation = async(Dispatchers.Default) { generator().generate("diff") }
        waitUntil { File(dir, "pid").exists() }
        generation.cancel()
        generation.join()

        val pid = File(dir, "pid").readText().trim().toLong()
        waitUntil { ProcessHandle.of(pid).map { !it.isAlive }.orElse(true) }
    }

    // --- helpers ---

    /** Writes the fake `claude` script; `$DIR` in it points to the test directory. */
    private fun fakeClaude(body: String) {
        File(dir, "claude").apply {
            writeText("#!/bin/sh\nDIR='${dir.path}'\n$body\n")
            setExecutable(true)
        }
    }

    private fun generator(settings: SimpleCommitSettings.State = SimpleCommitSettings.State(timeoutSeconds = 2)) =
        ClaudeCliCommitMessageGenerator(findClaude = { File(dir, "claude").path }, loadSettings = { settings })

    private fun generate(diff: String): String = runBlocking { generator().generate(diff) }

    private fun assertFails(): ClaudeCliException = runBlocking {
        runCatching { generator().generate("diff") }.exceptionOrNull() as? ClaudeCliException
            ?: throw AssertionError("Expected ClaudeCliException")
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) fail("Condition was not met within 10 seconds")
            Thread.sleep(20)
        }
    }
}
